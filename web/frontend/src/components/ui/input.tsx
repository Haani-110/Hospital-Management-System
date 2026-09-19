import { forwardRef, useId, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { cn } from '@/lib/utils';

export const inputStyles =
  'h-9.5 w-full rounded-md border border-border bg-sunken px-3 text-sm text-fg ' +
  'placeholder:text-subtle transition-colors ' +
  'hover:border-border-strong focus:border-accent focus:outline-none ' +
  'focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-60';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  /** Renders a red border and wires `aria-invalid`. */
  hasError?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, hasError, ...props },
  ref,
) {
  return (
    <input
      ref={ref}
      aria-invalid={hasError || undefined}
      className={cn(inputStyles, hasError && 'border-danger/60 focus:border-danger', className)}
      {...props}
    />
  );
});

export type PasswordInputProps = Omit<InputProps, 'type'>;

/** Password field with an accessible show/hide toggle. */
export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  function PasswordInput({ className, hasError, ...props }, ref) {
    const [visible, setVisible] = useState(false);
    const id = useId();

    return (
      <div className="relative">
        <input
          ref={ref}
          type={visible ? 'text' : 'password'}
          aria-invalid={props['aria-invalid'] ?? (hasError || undefined)}
          className={cn(
            inputStyles,
            'pr-11',
            hasError && 'border-danger/60 focus:border-danger',
            className,
          )}
          {...props}
        />
        <button
          type="button"
          id={id}
          onClick={() => setVisible((value) => !value)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          aria-pressed={visible}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-md text-subtle transition-colors hover:text-fg"
        >
          {visible ? (
            <EyeOff aria-hidden className="size-4" />
          ) : (
            <Eye aria-hidden className="size-4" />
          )}
        </button>
      </div>
    );
  },
);
