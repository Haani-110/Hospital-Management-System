import { forwardRef } from 'react';
import { cn } from '@/lib/utils';

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  hasError?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, hasError, ...props },
  ref,
) {
  return (
    <textarea
      ref={ref}
      aria-invalid={hasError || undefined}
      className={cn(
        'min-h-24 w-full rounded-md border border-border bg-sunken px-3 py-2 text-sm text-fg',
        'placeholder:text-subtle transition-colors hover:border-border-strong',
        'focus:border-accent focus:outline-none disabled:cursor-not-allowed disabled:opacity-60',
        hasError && 'border-danger/60',
        className,
      )}
      {...props}
    />
  );
});
