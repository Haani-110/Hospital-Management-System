import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from './button';
import type { PaginationMeta } from '@/lib/api/types';

interface PaginationProps {
  meta: PaginationMeta;
  onPageChange: (page: number) => void;
  /** Noun used in the summary line, e.g. `patients`. */
  itemLabel?: string;
  className?: string;
}

/** Page controls plus a summary of the current slice. */
export function Pagination({
  meta,
  onPageChange,
  itemLabel = 'records',
  className,
}: PaginationProps) {
  const { page, limit, total, totalPages } = meta;
  const first = total === 0 ? 0 : (page - 1) * limit + 1;
  const last = Math.min(page * limit, total);
  const canGoBack = page > 1;
  const canGoForward = page < totalPages;

  return (
    <nav
      aria-label="Pagination"
      className={cn(
        'flex flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3',
        className,
      )}
    >
      <p className="text-[13px] text-muted tabular" aria-live="polite">
        {total === 0 ? (
          <>No {itemLabel}</>
        ) : (
          <>
            <span className="text-fg">{first.toLocaleString()}</span>–
            <span className="text-fg">{last.toLocaleString()}</span> of{' '}
            <span className="text-fg">{total.toLocaleString()}</span> {itemLabel}
          </>
        )}
      </p>

      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={!canGoBack}
        >
          <ChevronLeft aria-hidden className="size-3.5" />
          Previous
        </Button>
        <span className="px-1 text-[13px] text-muted tabular">
          Page {page} of {Math.max(totalPages, 1)}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={!canGoForward}
        >
          Next
          <ChevronRight aria-hidden className="size-3.5" />
        </Button>
      </div>
    </nav>
  );
}
