import { cn } from '@/lib/utils';

export interface DescriptionItem {
  label: string;
  /** Plain value, or richer content such as a badge. */
  value: React.ReactNode;
  /** Render across both columns. */
  span?: boolean;
}

/** Key/value grid used on detail pages. */
export function DescriptionList({
  items,
  className,
  columns = 2,
}: {
  items: DescriptionItem[];
  className?: string;
  columns?: 1 | 2 | 3;
}) {
  const gridCols =
    columns === 1 ? 'sm:grid-cols-1' : columns === 3 ? 'sm:grid-cols-3' : 'sm:grid-cols-2';

  return (
    <dl className={cn('grid grid-cols-1 gap-x-8 gap-y-5', gridCols, className)}>
      {items.map((item) => (
        <div key={item.label} className={cn('min-w-0', item.span && 'sm:col-span-full')}>
          <dt className="text-[11px] font-semibold tracking-[0.06em] text-subtle uppercase">
            {item.label}
          </dt>
          <dd className="mt-1.5 text-sm break-words text-fg">{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}

/** Renders a value that may be null as muted em-dash. */
export function OptionalValue({ value }: { value: string | null | undefined }) {
  if (!value) return <span className="text-subtle">Not recorded</span>;
  return <>{value}</>;
}
