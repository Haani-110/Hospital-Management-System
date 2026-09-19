import { cn } from '@/lib/utils';

export interface EmptyStateProps {
  icon?: React.ElementType;
  title: string;
  description?: string;
  /** Optional call to action (e.g. a link or button). */
  action?: React.ReactNode;
  className?: string;
  /** `compact` fits inside a card; `page` fills a content area. */
  size?: 'compact' | 'page';
  /**
   * Element used for the title. Page-level empty states should use a heading
   * so the document outline stays meaningful for screen-reader navigation.
   */
  titleAs?: 'p' | 'h1' | 'h2';
}

/** Empty, unavailable or not-yet-connected state. Never shows fake content. */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
  size = 'compact',
  titleAs: TitleTag = 'p',
}: EmptyStateProps) {
  return (
    <div
      role="status"
      className={cn(
        'flex flex-col items-center justify-center text-center',
        size === 'page' ? 'gap-4 px-6 py-20' : 'gap-3 px-6 py-12',
        className,
      )}
    >
      {Icon ? (
        <span
          aria-hidden
          className={cn(
            'flex items-center justify-center rounded-xl border border-border bg-elevated text-subtle',
            size === 'page' ? 'size-12' : 'size-10',
          )}
        >
          <Icon className={size === 'page' ? 'size-5' : 'size-4.5'} />
        </span>
      ) : null}
      <div className="max-w-md">
        <TitleTag
          className={cn(
            'font-medium text-fg',
            size === 'page' ? 'text-base' : 'text-sm',
            TitleTag !== 'p' && 'font-semibold',
          )}
        >
          {title}
        </TitleTag>
        {description ? (
          <p className="mt-1.5 text-[13px] leading-relaxed text-muted">{description}</p>
        ) : null}
      </div>
      {action ? <div className="mt-1">{action}</div> : null}
    </div>
  );
}
