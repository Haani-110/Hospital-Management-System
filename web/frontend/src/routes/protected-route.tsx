import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { Logo } from '@/components/layout/logo';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuth } from '@/providers/auth-context';

/** Full-screen placeholder while the stored session is verified. */
function SessionLoading() {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-bg">
      <Logo />
      <div className="w-56 space-y-2" role="status" aria-label="Checking your session">
        <Skeleton className="mx-auto h-3 w-40" />
        <Skeleton className="mx-auto h-3 w-28" />
      </div>
    </div>
  );
}

/**
 * Gate for authenticated areas. Unauthenticated visitors are redirected to the
 * login screen with the intended destination preserved.
 */
export function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <SessionLoading />;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  return <Outlet />;
}
