import { forwardRef, type SelectHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, invalid, children, ...rest },
  ref,
) {
  const ariaInvalid = invalid ?? Boolean(rest['aria-invalid']);

  return (
    <select
      ref={ref}
      className={cn('field-control appearance-none bg-no-repeat pr-9', className)}
      style={{
        backgroundImage:
          "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20' fill='none' stroke='%23475569' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 8 4 4 4-4'/%3E%3C/svg%3E\")",
        backgroundPosition: 'right 0.65rem center',
        backgroundSize: '1.1rem',
      }}
      aria-invalid={ariaInvalid || undefined}
      {...rest}
    >
      {children}
    </select>
  );
});
