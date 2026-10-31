import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, invalid, ...rest },
  ref,
) {
  const ariaInvalid = invalid ?? Boolean(rest['aria-invalid']);

  return (
    <input
      ref={ref}
      className={cn('field-control', className)}
      aria-invalid={ariaInvalid || undefined}
      {...rest}
    />
  );
});
