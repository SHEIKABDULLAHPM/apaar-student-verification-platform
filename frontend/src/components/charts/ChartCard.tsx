import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Card, CardHeader } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ChartIcon } from '@/components/icons';

export interface ChartCardProps {
  title: string;
  description?: string;
  children: ReactNode;
  loading?: boolean;
  empty?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
  className?: string;
  actions?: ReactNode;
}

export function ChartCard({
  title,
  description,
  children,
  loading = false,
  empty = false,
  emptyTitle = 'No data available',
  emptyDescription = 'Data will appear here once the platform has records for the selected scope.',
  className,
  actions,
}: ChartCardProps) {
  return (
    <Card className={cn('overflow-hidden', className)}>
      <CardHeader title={title} description={description} actions={actions} />
      <div className="p-5">
        {loading ? (
          <div className="h-72 animate-pulse rounded-lg bg-surface-muted" aria-busy="true">
            <span className="sr-only">Loading chart</span>
          </div>
        ) : empty ? (
          <EmptyState
            title={emptyTitle}
            description={emptyDescription}
            icon={<ChartIcon className="h-6 w-6" />}
            className="py-10"
          />
        ) : (
          children
        )}
      </div>
    </Card>
  );
}
