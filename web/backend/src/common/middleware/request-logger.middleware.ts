import { Injectable, Logger, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';

/**
 * Lightweight request logging: method, path, status and duration.
 * Deliberately dependency-free and silent during tests.
 */
@Injectable()
export class RequestLoggerMiddleware implements NestMiddleware {
  private readonly logger = new Logger('HTTP');

  use(request: Request, response: Response, next: NextFunction): void {
    if (process.env.NODE_ENV === 'test') {
      next();
      return;
    }

    const startedAt = Date.now();
    response.on('finish', () => {
      const duration = Date.now() - startedAt;
      this.logger.log(
        `${request.method} ${request.originalUrl} ${response.statusCode} ${duration}ms`,
      );
    });

    next();
  }
}
