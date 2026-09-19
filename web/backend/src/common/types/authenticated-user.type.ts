import { Role } from '../../generated/prisma/client.js';

/**
 * The authenticated principal attached to the request by JwtAuthGuard.
 * It is derived exclusively from a verified JWT — never from request input.
 */
export interface AuthenticatedUser {
  id: string;
  username: string;
  role: Role;
}

export interface JwtPayload {
  /** Subject: the user id. */
  sub: string;
  username: string;
  role: Role;
  iat?: number;
  exp?: number;
}
