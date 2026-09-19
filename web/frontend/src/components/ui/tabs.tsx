import { useCallback, useRef } from 'react';
import { cn } from '@/lib/utils';

export interface TabItem {
  id: string;
  label: string;
  /** Optional count shown next to the label. */
  count?: number;
}

interface TabsProps {
  /** Id prefix shared with `TabPanel` so each tab links to its panel. */
  idPrefix: string;
  items: TabItem[];
  value: string;
  onValueChange: (id: string) => void;
  className?: string;
}

/**
 * Accessible tab strip (WAI-ARIA tabs pattern) with roving focus:
 * Left/Right/Home/End move between tabs, each panel is linked by id.
 * Panels are rendered by the caller using `tabs-<id>-panel` as their id.
 */
export function Tabs({ idPrefix, items, value, onValueChange, className }: TabsProps) {
  const listRef = useRef<HTMLDivElement>(null);

  const onKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLDivElement>) => {
      const index = items.findIndex((item) => item.id === value);
      if (index === -1) return;

      let nextIndex: number;
      if (event.key === 'ArrowRight') nextIndex = (index + 1) % items.length;
      else if (event.key === 'ArrowLeft') nextIndex = (index - 1 + items.length) % items.length;
      else if (event.key === 'Home') nextIndex = 0;
      else if (event.key === 'End') nextIndex = items.length - 1;
      else return;

      event.preventDefault();
      const nextItem = items[nextIndex];
      if (!nextItem) return;
      onValueChange(nextItem.id);
      listRef.current
        ?.querySelector<HTMLButtonElement>(`#${CSS.escape(`${idPrefix}-tab-${nextItem.id}`)}`)
        ?.focus();
    },
    [items, value, onValueChange, idPrefix],
  );

  return (
    <div
      ref={listRef}
      role="tablist"
      aria-label="Sections"
      onKeyDown={onKeyDown}
      className={cn(
        'flex gap-1 overflow-x-auto border-b border-border pb-px',
        '[-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden',
        className,
      )}
    >
      {items.map((item) => {
        const selected = item.id === value;
        return (
          <button
            key={item.id}
            id={`${idPrefix}-tab-${item.id}`}
            role="tab"
            type="button"
            aria-selected={selected}
            aria-controls={`${idPrefix}-panel-${item.id}`}
            tabIndex={selected ? 0 : -1}
            onClick={() => onValueChange(item.id)}
            className={cn(
              'press relative shrink-0 rounded-t-md px-3.5 py-2 text-[13px] font-medium whitespace-nowrap',
              selected
                ? 'text-fg after:absolute after:inset-x-2 after:-bottom-px after:h-0.5 after:rounded-full after:bg-accent'
                : 'text-muted hover:text-fg',
            )}
          >
            {item.label}
            {item.count !== undefined ? (
              <span className="ml-1.5 tabular text-xs text-subtle">{item.count}</span>
            ) : null}
          </button>
        );
      })}
    </div>
  );
}

/** Panel container matching `Tabs`; keeps inactive panels out of the a11y tree. */
export function TabPanel({
  idPrefix,
  id,
  activeId,
  children,
  className,
}: {
  /** Must match the `idPrefix` of the owning `Tabs` component. */
  idPrefix: string;
  id: string;
  activeId: string;
  children: React.ReactNode;
  className?: string;
}) {
  if (id !== activeId) return null;
  return (
    <div
      id={`${idPrefix}-panel-${id}`}
      role="tabpanel"
      aria-labelledby={`${idPrefix}-tab-${id}`}
      tabIndex={0}
      className={cn('animate-fade-up focus-visible:outline-none', className)}
    >
      {children}
    </div>
  );
}
