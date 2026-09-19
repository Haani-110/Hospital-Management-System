import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ArgumentsHost, BadRequestException, NotFoundException } from '@nestjs/common';
import { AllExceptionsFilter } from './all-exceptions.filter.js';
import { Prisma } from '../../prisma/prisma.service.js';

type JsonPayload = Record<string, unknown>;

type JsonMock = jest.Mock<(payload: JsonPayload) => void>;
type StatusMock = jest.Mock<(statusCode: number) => { json: JsonMock }>;

function buildHost(): { host: ArgumentsHost; status: StatusMock; json: JsonMock } {
  const json: JsonMock = jest.fn<(payload: JsonPayload) => void>();
  const status: StatusMock = jest.fn<(statusCode: number) => { json: JsonMock }>();
  status.mockReturnValue({ json });

  const host = {
    switchToHttp: () => ({
      getResponse: () => ({ status, json }),
      getRequest: () => ({ originalUrl: '/api/v1/patients', method: 'GET' }),
    }),
  } as unknown as ArgumentsHost;

  return { host, status, json };
}

describe('AllExceptionsFilter', () => {
  let filter: AllExceptionsFilter;

  beforeEach(() => {
    filter = new AllExceptionsFilter();
  });

  it('formats an HttpException into the standard error envelope', () => {
    const { host, status, json } = buildHost();

    filter.catch(new NotFoundException('Patient not found'), host);

    expect(status).toHaveBeenCalledWith(404);
    expect(json.mock.calls[0]?.[0]).toMatchObject({
      statusCode: 404,
      message: 'Patient not found',
      path: '/api/v1/patients',
    });
    expect(typeof json.mock.calls[0]?.[0]?.timestamp).toBe('string');
  });

  it('preserves validation message arrays from the global ValidationPipe', () => {
    const { host, json } = buildHost();

    filter.catch(
      new BadRequestException({
        statusCode: 400,
        error: 'Bad Request',
        message: ['username must be longer than or equal to 3 characters'],
      }),
      host,
    );

    expect(json.mock.calls[0]?.[0]?.message).toEqual([
      'username must be longer than or equal to 3 characters',
    ]);
  });

  it('never leaks an unexpected error message to the client', () => {
    const { host, status, json } = buildHost();

    filter.catch(new Error('connection string postgresql://user:pass@host/db failed'), host);

    expect(status).toHaveBeenCalledWith(500);
    expect(json.mock.calls[0]?.[0]?.message).toBe('Internal server error');
    expect(JSON.stringify(json.mock.calls[0]?.[0])).not.toContain('postgresql://');
  });

  it('maps a Prisma unique-constraint violation to 409 with a safe message', () => {
    const { host, status, json } = buildHost();

    // Simulate the Prisma error class hierarchy without a live database.
    const prismaError = Object.assign(
      new Error('Unique constraint failed on the fields: (`username`)'),
      { code: 'P2002' },
    );
    Object.setPrototypeOf(prismaError, Prisma.PrismaClientKnownRequestError.prototype);

    filter.catch(prismaError, host);

    expect(status).toHaveBeenCalledWith(409);
    expect(json.mock.calls[0]?.[0]?.message).toBe('A record with these values already exists');
    expect(JSON.stringify(json.mock.calls[0]?.[0])).not.toContain('Unique constraint');
  });
});
