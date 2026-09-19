import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { ROLES_KEY } from '../decorators/roles.decorator.js';
import { Role } from '../../generated/prisma/client.js';
import type { AuthenticatedUser } from '../types/authenticated-user.type.js';

/**
 * Role-based authorization foundation.
 *
 * Routes without `@Roles(...)` only require a valid token; routes with
 * `@Roles(...)` require the authenticated role to be one of the listed roles.
 * Roles are read from the verified principal, not from request input.
 */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const requiredRoles = this.reflector.getAllAndOverride<Role[] | undefined>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    const request = context.switchToHttp().getRequest<{ user?: AuthenticatedUser }>();
    const user = request.user;

    if (!user) {
      throw new ForbiddenException('Authenticated user context is required');
    }

    if (!requiredRoles.includes(user.role)) {
      throw new ForbiddenException('Your role is not allowed to perform this action');
    }

    return true;
  }
}
