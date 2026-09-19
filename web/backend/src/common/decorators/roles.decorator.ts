import { SetMetadata } from '@nestjs/common';
import { Role } from '../../generated/prisma/client.js';

export const ROLES_KEY = 'roles';

/**
 * Declares which roles may call a route. Enforced by RolesGuard from the
 * verified JWT payload — never from a client-supplied role field.
 */
export const Roles = (...roles: Role[]) => SetMetadata(ROLES_KEY, roles);
