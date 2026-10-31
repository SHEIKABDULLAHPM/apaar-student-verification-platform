import { forwardRef, type TextareaHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, invalid, rows = 4, ...rest },
  ref,
) {
  const ariaInvalid = invalid ?? Boolean(rest['aria-invalid']);

  return (
    <textarea
      ref={ref}
      rows={rows}
      className={cn('field-control resize-y', className)}
      aria-invalid={ariaInvalid || undefined}
      {...rest}
    />
  );
});
