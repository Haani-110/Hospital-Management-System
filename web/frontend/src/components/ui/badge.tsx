import { cn } from '@/lib/utils';

export type BadgeTone = 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'info';

const toneStyles: Record<BadgeTone, string> = {
  neutral: 'bg-elevated text-muted border-border',
  accent: 'bg-accent-soft text-accent border-accent/25',
  success: 'bg-success-soft text-success border-success/25',
  warning: 'bg-warning-soft text-warning border-warning/25',
  danger: 'bg-danger-soft text-danger border-danger/25',
  info: 'bg-info-soft text-info border-info/25',
};

const dotStyles: Record<BadgeTone, string> = {
  neutral: 'bg-subtle',
  accent: 'bg-accent',
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
  info: 'bg-info',
};

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone;
  /**
   * Adds a status dot. Status is always communicated by text as well as
   * colour, never by colour alone.
   */
  withDot?: boolean;
}

export function Badge({ className, tone = 'neutral', withDot, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap',
        toneStyles[tone],
        className,
      )}
      {...props}
    >
      {withDot ? <span aria-hidden className={cn('size-1.5 rounded-full', dotStyles[tone])} /> : null}
      {children}
    </span>
  );
}
