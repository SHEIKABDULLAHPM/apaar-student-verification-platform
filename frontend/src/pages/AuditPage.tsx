import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listAuditEntries } from '@/api/audit';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import type { AuditEntry } from '@/api/types';

export function AuditPage() {
  const [page, setPage] = useState(0);
  const size = 15;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listAuditEntries({ page, size }),
    [page, size]
  );

  const columns: DataTableColumn<AuditEntry>[] = [
    {
      key: 'eventType',
      header: 'Event Type',
      render: (row) => <span className="font-mono font-medium">{String(row.eventType ?? row.action ?? 'EVENT')}</span>,
    },
    {
      key: 'actor',
      header: 'Actor & Role',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{String(row.actorRole ?? row.actorEmail ?? 'System')}</p>
          <p className="text-xs text-ink-500 font-mono">
            {row.actorId ? String(row.actorId).slice(0, 8) + '...' : String(row.actor ?? 'SYSTEM')}
          </p>
        </div>
      ),
    },
    {
      key: 'entity',
      header: 'Target Entity',
      render: (row) => (
        <div>
          <span className="font-medium text-ink-900">{String(row.entityType ?? 'SYSTEM')}</span>
          <p className="text-xs text-ink-500 font-mono">{String(row.entityId ?? '-')}</p>
        </div>
      ),
    },
    {
      key: 'action',
      header: 'Action',
      render: (row) => <span className="font-semibold">{String(row.action ?? 'MUTATION')}</span>,
    },
    {
      key: 'outcome',
      header: 'Outcome',
      render: (row) => {
        const outcome = String(row.outcome ?? 'SUCCESS');
        return (
          <StatusBadge
            status={outcome}
            label={outcome}
            tone={outcome === 'SUCCESS' ? 'success' : 'danger'}
          />
        );
      },
    },
    {
      key: 'occurredAt',
      header: 'Timestamp',
      render: (row) => <span className="text-xs text-ink-500">{String(row.occurredAt ?? row.createdAt ?? row.timestamp ?? '-')}</span>,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Append-Only Audit Trail"
        description="Immutable record of every privileged administrative, verification, and data mutation event."
        eyebrow="Compliance & Accountability"
      />

      {error ? (
        <ErrorState title="Failed to load audit trail" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => String(row.id ?? Math.random())}
            caption="Immutable log of system actions"
            loading={isLoading}
            emptyTitle="No audit records yet"
            emptyDescription="Administrative and security actions will be appended here."
          />
          {data && data.totalPages > 1 && (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={data.size}
              onPageChange={setPage}
            />
          )}
        </div>
      )}
    </div>
  );
}
