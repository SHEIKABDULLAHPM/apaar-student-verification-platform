import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { formatNumber } from '@/lib/format';

export type StatTone = 'default' | 'success' | 'warning' | 'danger' | 'info';

export interface StatCardProps {
  label: string;
  value: number | string | null | undefined;
  hint?: ReactNode;
  icon?: ReactNode;
  tone?: StatTone;
  loading?: boolean;
  className?: string;
}

const toneClasses: Record<StatTone, string> = {
  default: 'bg-brand-50 text-brand-700',
  success: 'bg-success-soft text-success',
  warning: 'bg-warning-soft text-warning',
  danger: 'bg-danger-soft text-danger',
  info: 'bg-info-soft text-info',
};

export function StatCard({
  label,
  value,
  hint,
  icon,
  tone = 'default',
  loading = false,
  className,
}: StatCardProps) {
  const displayValue =
    typeof value === 'number' ? formatNumber(value) : value === null || value === undefined ? '—' : value;

  return (
    <div
      className={cn('surface-card flex flex-col gap-3 p-5', className)}
      aria-busy={loading || undefined}
    >
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-ink-600">{label}</p>
        {icon && (
          <span
            aria-hidden="true"
            className={cn('flex h-9 w-9 shrink-0 items-center justify-center rounded-lg', toneClasses[tone])}
          >
            {icon}
          </span>
        )}
      </div>
      {loading ? (
        <>
          <div className="h-8 w-24 animate-pulse rounded bg-line/80" />
          <div className="h-3 w-32 animate-pulse rounded bg-line/60" />
        </>
      ) : (
        <>
          <p className="text-2xl font-semibold tabular-nums tracking-tight text-ink-900">
            {displayValue}
          </p>
          {hint && <p className="text-xs text-ink-500">{hint}</p>}
        </>
      )}
    </div>
  );
}
