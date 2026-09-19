import { forwardRef } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export const selectStyles =
  'h-9.5 w-full appearance-none rounded-md border border-border bg-sunken pl-3 pr-9 text-sm text-fg ' +
  'transition-colors hover:border-border-strong focus:border-accent focus:outline-none ' +
  'disabled:cursor-not-allowed disabled:opacity-60';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  options: SelectOption[];
  /** Rendered as a disabled first option. */
  placeholder?: string;
  hasError?: boolean;
}

/**
 * Styled wrapper around a native `<select>`: full keyboard support, correct
 * announcements and mobile OS pickers come for free.
 */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, options, placeholder, hasError, ...props },
  ref,
) {
  return (
    <div className="relative">
      <select
        ref={ref}
        aria-invalid={hasError || undefined}
        className={cn(selectStyles, hasError && 'border-danger/60', className)}
        {...props}
      >
        {placeholder ? (
          <option value="" disabled>
            {placeholder}
          </option>
        ) : null}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <ChevronDown
        aria-hidden
        className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-subtle"
      />
    </div>
  );
});
