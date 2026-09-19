import { cn } from '@/lib/utils';
import { initials } from '@/lib/utils';

const sizes = {
  sm: 'size-8 text-xs',
  md: 'size-10 text-sm',
  lg: 'size-14 text-base',
} as const;

/** Initials avatar — deterministic, no external image requests. */
export function Avatar({
  name,
  size = 'md',
  className,
}: {
  name: string | null | undefined;
  size?: keyof typeof sizes;
  className?: string;
}) {
  return (
    <span
      aria-hidden
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-full border border-border',
        'bg-accent-soft font-semibold text-accent',
        sizes[size],
        className,
      )}
    >
      {initials(name)}
    </span>
  );
}
