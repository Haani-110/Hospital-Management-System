import { useId } from 'react';
import { cn } from '@/lib/utils';

export interface DonutSlice {
  label: string;
  value: number;
  /** Any CSS colour, normally a design token such as `var(--accent)`. */
  color: string;
}

interface DonutChartProps {
  slices: DonutSlice[];
  /** Big number in the middle; defaults to the total. */
  centerValue?: string;
  centerLabel?: string;
  className?: string;
  size?: number;
  thickness?: number;
}

/** Arc geometry for the donut: pure, so no render-time state is mutated. */
function buildArcs(slices: DonutSlice[], total: number, circumference: number) {
  return slices.reduce<
    (DonutSlice & { dashArray: string; dashOffset: number; percent: number })[]
  >((arcs, slice) => {
    const fraction = total === 0 ? 0 : slice.value / total;
    const length = fraction * circumference;
    const consumed = arcs.reduce((sum, arc) => sum + (arc.percent / 100) * circumference, 0);
    arcs.push({
      ...slice,
      dashArray: `${length} ${circumference - length}`,
      dashOffset: -consumed,
      percent: Math.round(fraction * 100),
    });
    return arcs;
  }, []);
}

/**
 * Dependency-free SVG donut for real counts only.
 * Renders nothing when every slice is zero, so an empty database shows the
 * surrounding empty state instead of a meaningless ring.
 */
export function DonutChart({
  slices,
  centerValue,
  centerLabel,
  className,
  size = 168,
  thickness = 14,
}: DonutChartProps) {
  const titleId = useId();
  const total = slices.reduce((sum, slice) => sum + slice.value, 0);
  const radius = (size - thickness) / 2;
  const circumference = 2 * Math.PI * radius;
  const populated = slices.filter((slice) => slice.value > 0);

  const arcs = buildArcs(populated, total, circumference);

  return (
    <div className={cn('flex flex-col items-center gap-5 sm:flex-row sm:gap-7', className)}>
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          role="img"
          aria-labelledby={titleId}
          className="-rotate-90"
        >
          <title id={titleId}>
            {populated.length === 0
              ? 'No data recorded yet'
              : populated.map((slice) => `${slice.label}: ${slice.value}`).join(', ')}
          </title>

          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke="var(--border)"
            strokeWidth={thickness}
          />

          {arcs.map((arc) => (
            <circle
              key={arc.label}
              cx={size / 2}
              cy={size / 2}
              r={radius}
              fill="none"
              stroke={arc.color}
              strokeWidth={thickness}
              strokeDasharray={arc.dashArray}
              strokeDashoffset={arc.dashOffset}
              strokeLinecap="butt"
              className="animate-fade-in"
            />
          ))}
        </svg>

        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-semibold text-fg tabular">
            {centerValue ?? total.toLocaleString()}
          </span>
          {centerLabel ? (
            <span className="mt-0.5 text-[11px] font-medium tracking-wide text-subtle uppercase">
              {centerLabel}
            </span>
          ) : null}
        </div>
      </div>

      <ul className="w-full space-y-2.5">
        {slices.map((slice) => (
          <li key={slice.label} className="flex items-center justify-between gap-4 text-sm">
            <span className="flex min-w-0 items-center gap-2.5">
              <span
                aria-hidden
                className="size-2.5 shrink-0 rounded-[3px]"
                style={{ backgroundColor: slice.color }}
              />
              <span className="truncate text-muted">{slice.label}</span>
            </span>
            <span className="shrink-0 font-medium text-fg tabular">
              {slice.value.toLocaleString()}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
