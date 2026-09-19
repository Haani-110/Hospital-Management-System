import { forwardRef } from 'react';
import { Loader2 } from 'lucide-react';
import { buttonStyles, type ButtonSize, type ButtonVariant } from './button-styles';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Shows a spinner and blocks interaction. */
  isLoading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant = 'primary', size = 'md', isLoading = false, disabled, children, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      // `aria-busy` lets screen readers announce work in progress.
      aria-busy={isLoading || undefined}
      disabled={disabled || isLoading}
      className={buttonStyles({ variant, size, className })}
      {...props}
    >
      {isLoading ? <Loader2 aria-hidden className="size-4 animate-spin" /> : null}
      {children}
    </button>
  );
});
