import { cn } from '@/lib/utils';

export interface LabelProps extends React.LabelHTMLAttributes<HTMLLabelElement> {
  /** Marks the field as required for assistive technology. */
  required?: boolean;
}

export function Label({ className, children, required, ...props }: LabelProps) {
  return (
    <label
      className={cn('text-[13px] font-medium text-fg select-none', className)}
      {...props}
    >
      {children}
      {required ? (
        <span aria-hidden className="ml-0.5 text-danger">
          *
        </span>
      ) : null}
    </label>
  );
}
