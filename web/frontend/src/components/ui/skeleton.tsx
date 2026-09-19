import { cn } from '@/lib/utils';

/** Placeholder block shown while data loads. */
export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden
      className={cn('animate-shimmer rounded-sm bg-elevated', className)}
      {...props}
    />
  );
}

/** Skeleton for a table body; `columns` sets the cell count. */
export function TableSkeleton({ rows = 6, columns = 4 }: { rows?: number; columns?: number }) {
  return (
    <div role="status" aria-label="Loading data" className="flex flex-col">
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div
          key={rowIndex}
          className="flex items-center gap-4 border-b border-border px-4 py-3.5 last:border-b-0"
        >
          {Array.from({ length: columns }).map((__, columnIndex) => (
            <Skeleton
              key={columnIndex}
              className={cn('h-3.5', columnIndex === 0 ? 'w-40' : 'w-24', 'shrink-0')}
              style={{ animationDelay: `${rowIndex * 60}ms` }}
            />
          ))}
        </div>
      ))}
    </div>
  );
}

/** Skeleton grid for KPI cards. */
export function CardsSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div role="status" aria-label="Loading metrics" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="rounded-lg border border-border bg-surface p-5">
          <Skeleton className="h-3 w-24" style={{ animationDelay: `${index * 60}ms` }} />
          <Skeleton className="mt-4 h-7 w-16" style={{ animationDelay: `${index * 60}ms` }} />
        </div>
      ))}
    </div>
  );
}
