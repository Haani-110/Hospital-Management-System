import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/providers/auth-context';
import type { Role } from '@/lib/api/types';

/**
 * Restricts a route to the given roles.
 *
 * This is a navigation convenience only — every request is still authorised by
 * the backend, which remains the source of truth.
 */
export function RoleRoute({ allow }: { allow: Role[] }) {
  const { user } = useAuth();

  if (!user || !allow.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
