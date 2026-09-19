import { cn } from '@/lib/utils';
import { Skeleton } from '@/components/ui/skeleton';

export interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  icon?: React.ElementType;
  /** Optional secondary line, e.g. a percentage or footnote. */
  footer?: React.ReactNode;
  className?: string;
  /** Emphasised variant for the headline metric. */
  emphasis?: boolean;
}

/** Single KPI tile. Values come from the API — never hardcoded. */
export function StatCard({
  label,
  value,
  hint,
  icon: Icon,
  footer,
  className,
  emphasis = false,
}: StatCardProps) {
  return (
    <div
      className={cn(
        'group relative overflow-hidden rounded-lg border border-border bg-surface p-5',
        'transition-colors duration-150 hover:border-border-strong',
        emphasis && 'bg-elevated',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <p className="text-[11px] font-semibold tracking-[0.06em] text-subtle uppercase">{label}</p>
        {Icon ? (
          <Icon
            aria-hidden
            className="size-4 shrink-0 text-subtle transition-colors group-hover:text-accent"
          />
        ) : null}
      </div>

      <p
        className={cn(
          'mt-3 font-semibold text-fg tabular',
          emphasis ? 'text-3xl tracking-tight' : 'text-2xl tracking-tight',
        )}
      >
        {value}
      </p>

      {hint ? <p className="mt-1.5 text-[13px] text-muted">{hint}</p> : null}
      {footer ? <div className="mt-3">{footer}</div> : null}
    </div>
  );
}

/** Loading placeholder matching `StatCard` height. */
export function StatCardSkeleton() {
  return (
    <div className="rounded-lg border border-border bg-surface p-5">
      <Skeleton className="h-3 w-24" />
      <Skeleton className="mt-4 h-7 w-20" />
      <Skeleton className="mt-2.5 h-3 w-28" />
    </div>
  );
}
