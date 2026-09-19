/**
 * Button variants and shared styling.
 *
 * Kept out of `button.tsx` so links can wear button styling (a `<button>`
 * wrapping a `<Link>` would be invalid HTML) without mixing non-component
 * exports into the component module.
 */
import { cn } from '@/lib/utils';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg' | 'icon';

const variantStyles: Record<ButtonVariant, string> = {
  primary:
    'bg-accent text-accent-fg border border-transparent hover:bg-accent-hover shadow-sm',
  secondary:
    'bg-elevated text-fg border border-border hover:border-border-strong hover:bg-elevated/80',
  outline:
    'bg-transparent text-fg border border-border hover:border-border-strong hover:bg-elevated',
  ghost: 'bg-transparent text-muted border border-transparent hover:bg-elevated hover:text-fg',
  danger:
    'bg-danger-soft text-danger border border-danger/30 hover:border-danger/50 hover:bg-danger-soft/70',
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-[13px] gap-1.5',
  md: 'h-9.5 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-sm gap-2',
  icon: 'h-9 w-9 justify-center',
};

/**
 * Shared button styling, also used for links that should look like buttons
 * (a `<Link>` inside a `<button>` would be invalid HTML).
 */
export function buttonStyles({
  variant = 'primary',
  size = 'md',
  className,
}: {
  variant?: ButtonVariant;
  size?: ButtonSize;
  className?: string;
} = {}): string {
  return cn(
    'press inline-flex items-center rounded-md font-medium whitespace-nowrap',
    'disabled:pointer-events-none disabled:opacity-50',
    variantStyles[variant],
    sizeStyles[size],
    className,
  );
}
