import { Logo } from './logo';
import { Skeleton } from '@/components/ui/skeleton';

/** Shown while the application resolves the stored session. */
export function FullPageLoading() {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-bg">
      <Logo />
      <div className="w-56 space-y-2" role="status" aria-label="Loading">
        <Skeleton className="mx-auto h-3 w-40" />
        <Skeleton className="mx-auto h-3 w-28" />
      </div>
    </div>
  );
}
