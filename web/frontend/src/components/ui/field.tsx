import { useId } from 'react';
import { cn } from '@/lib/utils';
import { Label } from './label';

interface FieldProps {
  label: string;
  /** Rendered under the control; also linked via aria-describedby. */
  hint?: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: (props: {
    id: string;
    'aria-describedby': string | undefined;
    'aria-invalid': boolean | undefined;
    hasError: boolean;
  }) => React.ReactNode;
}

/**
 * Associates a label, hint and error message with a form control.
 * The render prop keeps the wiring explicit and screen-reader correct.
 */
export function Field({ label, hint, error, required, className, children }: FieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy = error ? errorId : hint ? hintId : undefined;

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <Label htmlFor={id} required={required}>
        {label}
      </Label>
      {children({
        id,
        'aria-describedby': describedBy,
        'aria-invalid': error ? true : undefined,
        hasError: Boolean(error),
      })}
      {hint && !error ? (
        <p id={hintId} className="text-xs text-subtle">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} role="alert" className="text-xs text-danger">
          {error}
        </p>
      ) : null}
    </div>
  );
}
