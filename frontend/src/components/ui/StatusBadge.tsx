import { cn } from '@/lib/cn';
import { humanizeCode } from '@/lib/format';

export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export interface StatusBadgeProps {
  status: string | null | undefined;
  tone?: StatusTone;
  label?: string;
  className?: string;
}

const toneClasses: Record<StatusTone, string> = {
  success: 'bg-success-soft text-success',
  warning: 'bg-warning-soft text-warning',
  danger: 'bg-danger-soft text-danger',
  info: 'bg-info-soft text-info',
  neutral: 'bg-surface-muted text-ink-600',
};

function matchesStatus(status: string, values: readonly string[]): boolean {
  return values.some((value) => status === value || status.endsWith(`_${value}`));
}

export function inferStatusTone(status: string | null | undefined): StatusTone {
  const normalized = status?.trim().toUpperCase();
  if (!normalized) return 'neutral';

  if (
    matchesStatus(normalized, [
      'ACTIVE',
      'VERIFIED',
      'APPROVED',
      'COMPLETED',
      'RESOLVED',
      'VALID',
      'SUCCESS',
      'PASSED',
      'ACCEPTED',
      'CLOSED',
    ])
  ) {
    return 'success';
  }

  if (
    matchesStatus(normalized, [
      'FAILED',
      'REJECTED',
      'ERROR',
      'INVALID',
      'CRITICAL',
      'HIGH',
      'VIOLATION',
      'BLOCKED',
      'EXPIRED',
      'CONFIRMED',
    ])
  ) {
    return 'danger';
  }

  if (
    matchesStatus(normalized, [
      'PENDING',
      'IN_PROGRESS',
      'PROCESSING',
      'OPEN',
      'UNDER_REVIEW',
      'INVESTIGATING',
      'PARTIAL',
      'WARNING',
      'MEDIUM',
      'UNVERIFIED',
      'AWAITING',
    ])
  ) {
    return 'warning';
  }

  if (
    matchesStatus(normalized, ['NEW', 'SUBMITTED', 'INITIATED', 'SIMULATED', 'LOW', 'INFO'])
  ) {
    return 'info';
  }

  return 'neutral';
}

export function StatusBadge({ status, tone, label, className }: StatusBadgeProps) {
  const resolvedTone = tone ?? inferStatusTone(status);

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold',
        toneClasses[resolvedTone],
        className,
      )}
      title={status ?? undefined}
    >
      <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-current" />
      {label ?? humanizeCode(status)}
    </span>
  );
}
