import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children?: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
}

/**
 * Modal built on the native `<dialog>` element.
 *
 * Using `showModal()` delegates focus trapping, the inert background, the
 * top-layer stacking and Escape handling to the browser, which removes an
 * entire class of accessibility bugs.
 */
export function Dialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  className,
}: DialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onClose={() => onOpenChange(false)}
      onCancel={() => onOpenChange(false)}
      onClick={(event) => {
        // Clicks landing on the dialog element itself are backdrop clicks.
        if (event.target === dialogRef.current) onOpenChange(false);
      }}
      className={cn(
        'm-auto w-[min(32rem,calc(100vw-2rem))] rounded-xl border border-border bg-surface p-0 text-fg shadow-lg',
        'backdrop:bg-overlay animate-scale-in',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-6 border-b border-border px-5 py-4">
        <div className="min-w-0">
          <h2 id={titleId} className="text-sm font-semibold text-fg">
            {title}
          </h2>
          {description ? <p className="mt-1 text-[13px] text-muted">{description}</p> : null}
        </div>
        <button
          type="button"
          onClick={() => onOpenChange(false)}
          aria-label="Close dialog"
          className="press -mt-1 -mr-1 rounded-md p-1.5 text-subtle hover:bg-elevated hover:text-fg"
        >
          <X aria-hidden className="size-4" />
        </button>
      </div>

      {children ? <div className="max-h-[70vh] overflow-y-auto px-5 py-4">{children}</div> : null}

      {footer ? (
        <div className="flex flex-wrap justify-end gap-2 border-t border-border px-5 py-3.5">
          {footer}
        </div>
      ) : null}
    </dialog>
  );
}
