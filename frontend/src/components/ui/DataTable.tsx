import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { EmptyState } from '@/components/ui/EmptyState';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

export interface DataTableColumn<T> {
  key: string;
  header: ReactNode;
  render: (row: T) => ReactNode;
  align?: 'left' | 'center' | 'right';
  className?: string;
  headerClassName?: string;
}

export interface DataTableProps<T> {
  columns: DataTableColumn<T>[];
  rows: T[];
  getRowId: (row: T) => string;
  caption: string;
  loading?: boolean;
  emptyTitle?: string;
  emptyDescription?: ReactNode;
  emptyAction?: ReactNode;
  rowClassName?: (row: T) => string;
  className?: string;
}

const alignClasses = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
};

export function DataTable<T>({
  columns,
  rows,
  getRowId,
  caption,
  loading = false,
  emptyTitle = 'No records found',
  emptyDescription,
  emptyAction,
  rowClassName,
  className,
}: DataTableProps<T>) {
  const isEmpty = !loading && rows.length === 0;

  return (
    <div className={cn('table-shell', className)}>
      <table className="table-base" aria-busy={loading || undefined}>
        <caption className="sr-only">{caption}</caption>
        <thead className="table-head">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={cn(alignClasses[column.align ?? 'left'], column.headerClassName)}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading && (
            <tr>
              <td colSpan={columns.length} className="px-4 py-14 text-center">
                <LoadingSpinner label="Loading records" showLabel />
              </td>
            </tr>
          )}

          {isEmpty && (
            <tr>
              <td colSpan={columns.length} className="p-0">
                <EmptyState
                  title={emptyTitle}
                  description={emptyDescription}
                  action={emptyAction}
                />
              </td>
            </tr>
          )}

          {!loading &&
            rows.map((row) => (
              <tr key={getRowId(row)} className={cn('table-row', rowClassName?.(row))}>
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={cn(alignClasses[column.align ?? 'left'], column.className)}
                  >
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}
