import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { JwtAuthGuard } from './jwt-auth.guard.js';
import { Role } from '../../generated/prisma/client.js';
import type { AuthenticatedUser, JwtPayload } from '../types/authenticated-user.type.js';

type GuardedRequest = { headers: Record<string, string | undefined>; user?: AuthenticatedUser };

function buildContext(request: GuardedRequest): ExecutionContext {
  return {
    getHandler: () => undefined,
    getClass: () => undefined,
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext;
}

describe('JwtAuthGuard', () => {
  let verifyAsync: jest.Mock<(token: string) => Promise<JwtPayload>>;
  let getMetadata: jest.Mock<(key: string, targets: unknown[]) => boolean | undefined>;
  let guard: JwtAuthGuard;

  beforeEach(() => {
    verifyAsync = jest.fn<(token: string) => Promise<JwtPayload>>();
    getMetadata = jest.fn<(key: string, targets: unknown[]) => boolean | undefined>();
    getMetadata.mockReturnValue(undefined);

    guard = new JwtAuthGuard(
      { verifyAsync } as unknown as JwtService,
      { getAllAndOverride: getMetadata } as unknown as Reflector,
    );
  });

  it('allows a route marked as public without checking the token', async () => {
    getMetadata.mockReturnValue(true);
    const result = await guard.canActivate(buildContext({ headers: {} }));

    expect(result).toBe(true);
    expect(verifyAsync).not.toHaveBeenCalled();
  });

  it('rejects a request with no Authorization header', async () => {
    await expect(guard.canActivate(buildContext({ headers: {} }))).rejects.toThrow(
      UnauthorizedException,
    );
  });

  it('rejects non-bearer authorization schemes', async () => {
    await expect(
      guard.canActivate(buildContext({ headers: { authorization: 'Basic abc123' } })),
    ).rejects.toThrow(UnauthorizedException);
    expect(verifyAsync).not.toHaveBeenCalled();
  });

  it('attaches the principal from a valid token to the request', async () => {
    verifyAsync.mockResolvedValue({
      sub: 'user-1',
      username: 'admin',
      role: Role.ADMIN,
    });
    const request: GuardedRequest = { headers: { authorization: 'Bearer good.token' } };

    await expect(guard.canActivate(buildContext(request))).resolves.toBe(true);
    expect(verifyAsync).toHaveBeenCalledWith('good.token');
    expect(request.user).toEqual({ id: 'user-1', username: 'admin', role: Role.ADMIN });
  });

  it('rejects an invalid or expired token', async () => {
    verifyAsync.mockRejectedValue(new Error('jwt expired'));

    await expect(
      guard.canActivate(buildContext({ headers: { authorization: 'Bearer expired.token' } })),
    ).rejects.toThrow(new UnauthorizedException('Invalid or expired access token'));
  });

  it('rejects a token whose payload is missing required claims', async () => {
    verifyAsync.mockResolvedValue({ sub: '', username: '', role: Role.ADMIN });

    await expect(
      guard.canActivate(buildContext({ headers: { authorization: 'Bearer weird.token' } })),
    ).rejects.toThrow(new UnauthorizedException('Malformed access token payload'));
  });
});
