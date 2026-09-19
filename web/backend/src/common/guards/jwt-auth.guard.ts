import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Request } from 'express';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator.js';
import type { AuthenticatedUser, JwtPayload } from '../types/authenticated-user.type.js';

type RequestWithUser = Request & { user?: AuthenticatedUser };

function extractBearerToken(request: RequestWithUser): string | null {
  const header = request.headers?.authorization;
  if (!header || Array.isArray(header)) {
    return null;
  }
  const [scheme, token] = header.split(' ');
  if (scheme?.toLowerCase() !== 'bearer' || !token) {
    return null;
  }
  return token;
}

/**
 * Verifies the bearer access token and attaches the resulting principal to the
 * request. Applied globally; individual routes opt out with `@Public()`.
 *
 * The role attached here comes from the signed token payload, so a client can
 * never grant itself a role by sending a role field in the request body.
 */
@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(
    private readonly jwtService: JwtService,
    private readonly reflector: Reflector,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) {
      return true;
    }

    const request = context.switchToHttp().getRequest<RequestWithUser>();
    const token = extractBearerToken(request);
    if (!token) {
      throw new UnauthorizedException('Missing bearer token');
    }

    let payload: JwtPayload;
    try {
      payload = await this.jwtService.verifyAsync<JwtPayload>(token);
    } catch {
      throw new UnauthorizedException('Invalid or expired access token');
    }

    if (!payload?.sub || !payload.username || !payload.role) {
      throw new UnauthorizedException('Malformed access token payload');
    }

    request.user = { id: payload.sub, username: payload.username, role: payload.role };
    return true;
  }
}
