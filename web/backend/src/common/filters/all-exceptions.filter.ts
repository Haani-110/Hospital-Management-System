import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { Prisma } from '../../generated/prisma/client.js';

interface ErrorResponseBody {
  statusCode: number;
  error: string;
  message: string | string[];
  path?: string;
  timestamp: string;
}

/** Maps known Prisma failures onto HTTP responses without leaking internals. */
function mapPrismaError(exception: unknown): { status: number; message: string } | null {
  if (exception instanceof Prisma.PrismaClientKnownRequestError) {
    switch (exception.code) {
      case 'P2002':
        return {
          status: HttpStatus.CONFLICT,
          message: 'A record with these values already exists',
        };
      case 'P2003':
        return {
          status: HttpStatus.CONFLICT,
          message: 'The request conflicts with a related record',
        };
      case 'P2025':
        return { status: HttpStatus.NOT_FOUND, message: 'The requested record was not found' };
      default:
        return { status: HttpStatus.BAD_REQUEST, message: 'The database rejected this request' };
    }
  }

  if (exception instanceof Prisma.PrismaClientValidationError) {
    return { status: HttpStatus.BAD_REQUEST, message: 'Invalid query parameters supplied' };
  }

  if (exception instanceof Prisma.PrismaClientInitializationError) {
    return {
      status: HttpStatus.SERVICE_UNAVAILABLE,
      message: 'The database is currently unavailable',
    };
  }

  return null;
}

/**
 * Single, consistent error envelope for every failure:
 * { statusCode, error, message, path, timestamp }.
 *
 * Unexpected errors are logged with their stack server-side and answered with a
 * generic message, so raw database or framework errors never reach a client.
 */
@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger(AllExceptionsFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const context = host.switchToHttp();
    const response = context.getResponse<Response>();
    const request = context.getRequest<Request>();

    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let message: string | string[] = 'Internal server error';
    let error = 'Internal Server Error';

    if (exception instanceof HttpException) {
      status = exception.getStatus();
      const body = exception.getResponse();
      if (typeof body === 'string') {
        message = body;
        error = exception.name.replace('Exception', '');
      } else if (typeof body === 'object' && body !== null) {
        const typedBody = body as { message?: string | string[]; error?: string };
        message = typedBody.message ?? exception.message;
        error = typedBody.error ?? exception.name.replace('Exception', '');
      }
    } else {
      const prismaError = mapPrismaError(exception);
      if (prismaError) {
        status = prismaError.status;
        message = prismaError.message;
        error = HttpStatus[status] ?? 'Error';
      } else {
        this.logger.error(
          `Unhandled exception on ${request?.method} ${request?.originalUrl}`,
          exception instanceof Error ? exception.stack : String(exception),
        );
      }
    }

    const payload: ErrorResponseBody = {
      statusCode: status,
      error,
      message,
      path: request?.originalUrl,
      timestamp: new Date().toISOString(),
    };

    response.status(status).json(payload);
  }
}
