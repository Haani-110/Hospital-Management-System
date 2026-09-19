import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from './data-state';
import type { ApiError } from '@/lib/api/client';

/** Loading/error handling for a single-record screen. */
export function DetailView({
  isLoading,
  error,
  onRetry,
  skeleton,
  children,
}: {
  isLoading: boolean;
  error: ApiError | null;
  onRetry?: () => void;
  /** Custom skeleton; falls back to a generic block layout. */
  skeleton?: React.ReactNode;
  children: React.ReactNode;
}) {
  if (isLoading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading record">
        {skeleton ?? (
          <>
            <div className="rounded-lg border border-border bg-surface p-6">
              <div className="flex items-center gap-4">
                <Skeleton className="size-14 rounded-full" />
                <div className="flex-1 space-y-2.5">
                  <Skeleton className="h-4 w-48" />
                  <Skeleton className="h-3 w-32" />
                </div>
              </div>
              <div className="mt-7 grid gap-5 sm:grid-cols-2">
                {Array.from({ length: 6 }).map((_, index) => (
                  <div key={index} className="space-y-2">
                    <Skeleton className="h-2.5 w-20" />
                    <Skeleton className="h-3.5 w-32" />
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    );
  }

  if (error) {
    return <ErrorState error={error} onRetry={onRetry} />;
  }

  return <div className="animate-fade-up space-y-6">{children}</div>;
}
