import type { AppError } from '@/lib/errors';
import { cn } from '@/lib/cn';
import { AlertTriangleIcon } from '@/components/icons';
import { Button } from '@/components/ui/Button';

export interface ErrorStateProps {
  title?: string;
  error?: AppError | null;
  message?: string;
  onRetry?: () => void;
  retryLabel?: string;
  className?: string;
}

export function ErrorState({
  title = 'Something went wrong',
  error,
  message,
  onRetry,
  retryLabel = 'Try again',
  className,
}: ErrorStateProps) {
  const displayMessage =
    message ?? error?.message ?? 'An unexpected error occurred. Please try again.';

  return (
    <div
      role="alert"
      className={cn(
        'flex flex-col items-start gap-4 rounded-xl border border-danger/25 bg-danger-soft/50 p-6 sm:flex-row sm:items-start',
        className,
      )}
    >
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-danger-soft text-danger">
        <AlertTriangleIcon className="h-5 w-5" />
      </span>
      <div className="min-w-0 flex-1">
        <h2 className="text-base font-semibold text-ink-900">{title}</h2>
        <p className="mt-1 text-sm text-ink-700">{displayMessage}</p>
        {(error?.code || error?.requestId) && (
          <p className="mt-2 font-mono text-xs text-ink-500">
            {error.code && <span>{error.code}</span>}
            {error.code && error.requestId && <span aria-hidden="true"> · </span>}
            {error.requestId && <span>Request ID: {error.requestId}</span>}
          </p>
        )}
        {onRetry && (
          <Button variant="secondary" size="sm" className="mt-4" onClick={onRetry}>
            {retryLabel}
          </Button>
        )}
      </div>
    </div>
  );
}
