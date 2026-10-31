import { cn } from '@/lib/cn';

export interface LoadingSpinnerProps {
  label?: string;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  className?: string;
}

const sizeClasses = {
  sm: 'h-5 w-5 border-2',
  md: 'h-8 w-8 border-[3px]',
  lg: 'h-12 w-12 border-4',
};

export function LoadingSpinner({
  label = 'Loading',
  size = 'md',
  showLabel = false,
  className,
}: LoadingSpinnerProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn('inline-flex flex-col items-center justify-center gap-3', className)}
    >
      <span
        aria-hidden="true"
        className={cn(
          'animate-spin rounded-full border-brand-600 border-t-transparent',
          sizeClasses[size],
        )}
      />
      <span className={showLabel ? 'text-sm text-ink-600' : 'sr-only'}>{label}</span>
    </div>
  );
}
