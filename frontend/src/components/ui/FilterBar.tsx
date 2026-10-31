import type { FormEvent, ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/cn';

export interface FilterBarProps {
  children: ReactNode;
  onApply: () => void;
  onReset?: () => void;
  applyLabel?: string;
  resetLabel?: string;
  busy?: boolean;
  label?: string;
  className?: string;
}

export function FilterBar({
  children,
  onApply,
  onReset,
  applyLabel = 'Apply filters',
  resetLabel = 'Reset',
  busy = false,
  label = 'Filters',
  className,
}: FilterBarProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onApply();
  }

  return (
    <form
      onSubmit={handleSubmit}
      aria-label={label}
      className={cn('surface-card p-4', className)}
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{children}</div>
      <div className="mt-4 flex flex-wrap items-center justify-end gap-2">
        {onReset && (
          <Button variant="ghost" size="sm" onClick={onReset} disabled={busy}>
            {resetLabel}
          </Button>
        )}
        <Button type="submit" size="sm" loading={busy}>
          {applyLabel}
        </Button>
      </div>
    </form>
  );
}
