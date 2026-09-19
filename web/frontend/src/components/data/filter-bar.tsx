import { cn } from '@/lib/utils';

/** Horizontal container for search + filter controls above a table. */
export function FilterBar({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'flex flex-col gap-3 border-b border-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between',
        className,
      )}
      {...props}
    />
  );
}

/** Grouped filter controls that wrap on narrow screens. */
export function FilterGroup({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('flex flex-wrap items-center gap-2', className)} {...props} />;
}
