import { cn } from '@/lib/utils';

export interface BarDatum {
  label: string;
  value: number;
  color?: string;
}

interface BarChartProps {
  data: BarDatum[];
  className?: string;
  /** Formats the value shown next to each bar. */
  formatValue?: (value: number) => string;
}

/**
 * Horizontal bar chart for real counts — used where a donut would be harder to
 * compare at small sizes. Bars scale against the largest value.
 */
export function BarChart({ data, className, formatValue }: BarChartProps) {
  const max = Math.max(...data.map((item) => item.value), 1);

  return (
    <ul className={cn('space-y-4', className)}>
      {data.map((item, index) => {
        const percent = Math.round((item.value / max) * 100);
        return (
          <li key={item.label}>
            <div className="flex items-baseline justify-between gap-4 text-[13px]">
              <span className="text-muted">{item.label}</span>
              <span className="font-medium text-fg tabular">
                {formatValue ? formatValue(item.value) : item.value.toLocaleString()}
              </span>
            </div>
            <div
              className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-elevated"
              role="img"
              aria-label={`${item.label}: ${item.value}`}
            >
              <div
                className="h-full rounded-full transition-[width] duration-700 ease-out"
                style={{
                  width: `${percent}%`,
                  backgroundColor: item.color ?? 'var(--accent)',
                  transitionDelay: `${index * 40}ms`,
                }}
              />
            </div>
          </li>
        );
      })}
    </ul>
  );
}
