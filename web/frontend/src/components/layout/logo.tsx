import { cn } from '@/lib/utils';

/** Wordmark used in the sidebar, topbar and login screen. */
export function Logo({
  className,
  showText = true,
}: {
  className?: string;
  showText?: boolean;
}) {
  return (
    <span className={cn('flex items-center gap-2.5', className)}>
      <span
        aria-hidden
        className="flex size-8 shrink-0 items-center justify-center rounded-md border border-border bg-elevated"
      >
        <svg viewBox="0 0 24 24" className="size-4" fill="none">
          <path
            d="M12 5v14M5 12h14"
            stroke="var(--accent)"
            strokeWidth="2.4"
            strokeLinecap="round"
          />
        </svg>
      </span>
      {showText ? (
        <span className="min-w-0">
          <span className="block truncate text-sm font-semibold tracking-tight text-fg">
            Hospital Management
          </span>
          <span className="block text-[11px] font-medium tracking-wide text-subtle">
            v2.0 · Web
          </span>
        </span>
      ) : null}
    </span>
  );
}
