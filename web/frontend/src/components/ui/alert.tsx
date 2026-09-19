import { AlertTriangle, CheckCircle2, Info, ShieldAlert } from 'lucide-react';
import { cn } from '@/lib/utils';

export type AlertTone = 'info' | 'success' | 'warning' | 'danger';

const tones: Record<AlertTone, { wrap: string; icon: React.ElementType }> = {
  info: { wrap: 'border-info/30 bg-info-soft text-info', icon: Info },
  success: { wrap: 'border-success/30 bg-success-soft text-success', icon: CheckCircle2 },
  warning: { wrap: 'border-warning/30 bg-warning-soft text-warning', icon: AlertTriangle },
  danger: { wrap: 'border-danger/30 bg-danger-soft text-danger', icon: ShieldAlert },
};

export interface AlertProps {
  tone?: AlertTone;
  title?: string;
  children?: React.ReactNode;
  className?: string;
  /** `assertive` interrupts the screen reader — reserve it for errors. */
  live?: 'polite' | 'assertive' | 'off';
}

export function Alert({ tone = 'info', title, children, className, live = 'polite' }: AlertProps) {
  const { wrap, icon: Icon } = tones[tone];

  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      aria-live={live}
      className={cn('flex gap-3 rounded-md border px-4 py-3', wrap, className)}
    >
      <Icon aria-hidden className="mt-0.5 size-4 shrink-0" />
      <div className="min-w-0 text-[13px]">
        {title ? <p className="font-semibold text-fg">{title}</p> : null}
        {children ? <div className={cn('text-muted', title && 'mt-0.5')}>{children}</div> : null}
      </div>
    </div>
  );
}
