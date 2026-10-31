import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import {
  AlertTriangleIcon,
  CheckCircleIcon,
  InfoIcon,
  XCircleIcon,
} from '@/components/icons';

export type AlertVariant = 'info' | 'success' | 'warning' | 'danger';

export interface AlertProps {
  variant?: AlertVariant;
  title?: string;
  children: ReactNode;
  className?: string;
}

const variantClasses: Record<AlertVariant, string> = {
  info: 'border-info/25 bg-info-soft/60 text-ink-800',
  success: 'border-success/25 bg-success-soft/60 text-ink-800',
  warning: 'border-warning/25 bg-warning-soft/60 text-ink-800',
  danger: 'border-danger/25 bg-danger-soft/60 text-ink-800',
};

const iconClasses: Record<AlertVariant, string> = {
  info: 'text-info',
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-danger',
};

const icons: Record<AlertVariant, ReactNode> = {
  info: <InfoIcon className="h-5 w-5" />,
  success: <CheckCircleIcon className="h-5 w-5" />,
  warning: <AlertTriangleIcon className="h-5 w-5" />,
  danger: <XCircleIcon className="h-5 w-5" />,
};

export function Alert({ variant = 'info', title, children, className }: AlertProps) {
  return (
    <div
      role={variant === 'danger' ? 'alert' : 'status'}
      className={cn(
        'flex items-start gap-3 rounded-lg border px-4 py-3 text-sm',
        variantClasses[variant],
        className,
      )}
    >
      <span aria-hidden="true" className={cn('mt-0.5 shrink-0', iconClasses[variant])}>
        {icons[variant]}
      </span>
      <div className="min-w-0">
        {title && <p className="font-semibold">{title}</p>}
        <div className={cn(title && 'mt-0.5')}>{children}</div>
      </div>
    </div>
  );
}
