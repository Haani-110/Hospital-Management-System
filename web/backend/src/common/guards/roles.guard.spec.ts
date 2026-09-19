import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ExecutionContext, ForbiddenException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RolesGuard } from './roles.guard.js';
import { Role } from '../../generated/prisma/client.js';
import type { AuthenticatedUser } from '../types/authenticated-user.type.js';

function buildContext(user?: AuthenticatedUser): ExecutionContext {
  return {
    getHandler: () => undefined,
    getClass: () => undefined,
    switchToHttp: () => ({ getRequest: () => ({ user }) }),
  } as unknown as ExecutionContext;
}

describe('RolesGuard', () => {
  let getMetadata: jest.Mock<(key: string, targets: unknown[]) => Role[] | undefined>;
  let guard: RolesGuard;

  beforeEach(() => {
    getMetadata = jest.fn<(key: string, targets: unknown[]) => Role[] | undefined>();
    guard = new RolesGuard({ getAllAndOverride: getMetadata } as unknown as Reflector);
  });

  it('allows routes that do not declare @Roles', () => {
    getMetadata.mockReturnValue(undefined);

    expect(guard.canActivate(buildContext())).toBe(true);
  });

  it('allows a user whose role is listed', () => {
    getMetadata.mockReturnValue([Role.ADMIN, Role.RECEPTIONIST]);

    expect(
      guard.canActivate(buildContext({ id: 'u1', username: 'rec', role: Role.RECEPTIONIST })),
    ).toBe(true);
  });

  it('denies a user whose role is not listed', () => {
    getMetadata.mockReturnValue([Role.ADMIN]);

    expect(() =>
      guard.canActivate(buildContext({ id: 'u1', username: 'doc', role: Role.DOCTOR })),
    ).toThrow(ForbiddenException);
  });

  it('denies a protected route when no principal is attached', () => {
    getMetadata.mockReturnValue([Role.ADMIN]);

    expect(() => guard.canActivate(buildContext(undefined))).toThrow(ForbiddenException);
  });
});
