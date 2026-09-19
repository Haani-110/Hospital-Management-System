import { cn } from '@/lib/utils';
import { TableSkeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/ui/empty-state';
import { ErrorState } from './data-state';
import type { ApiError } from '@/lib/api/client';

export interface ListViewProps {
  isInitialLoading: boolean;
  error: ApiError | null;
  isEmpty: boolean;
  onRetry?: () => void;
  emptyIcon?: React.ElementType;
  emptyTitle: string;
  emptyDescription?: string;
  emptyAction?: React.ReactNode;
  skeletonRows?: number;
  skeletonColumns?: number;
  /** Shown in the corner while a filter change is being applied. */
  isRefreshing?: boolean;
  className?: string;
  children: React.ReactNode;
}

/**
 * Standard rendering of a list screen's states: loading → error → empty →
 * content. Keeping this in one place means every module reports failures and
 * emptiness identically.
 */
export function ListView({
  isInitialLoading,
  error,
  isEmpty,
  onRetry,
  emptyIcon,
  emptyTitle,
  emptyDescription,
  emptyAction,
  skeletonRows = 8,
  skeletonColumns = 4,
  isRefreshing,
  className,
  children,
}: ListViewProps) {
  if (isInitialLoading) {
    return <TableSkeleton rows={skeletonRows} columns={skeletonColumns} />;
  }

  if (error) {
    return (
      <div className="px-4 py-6">
        <ErrorState error={error} onRetry={onRetry} />
      </div>
    );
  }

  if (isEmpty) {
    return (
      <EmptyState
        icon={emptyIcon}
        title={emptyTitle}
        description={emptyDescription}
        action={emptyAction}
      />
    );
  }

  return (
    <div className={cn('relative', className)} aria-busy={isRefreshing || undefined}>
      {isRefreshing ? (
        <div
          aria-hidden
          className="absolute inset-x-0 top-0 h-px animate-shimmer bg-accent/60"
        />
      ) : null}
      {children}
    </div>
  );
}
