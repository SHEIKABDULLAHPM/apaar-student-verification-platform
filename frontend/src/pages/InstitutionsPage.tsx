import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listInstitutions } from '@/api/institutions';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import type { Institution } from '@/api/types';

export function InstitutionsPage() {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listInstitutions({ page, size }),
    [page, size]
  );

  const columns: DataTableColumn<Institution>[] = [
    {
      key: 'code',
      header: 'Code',
      render: (row) => <span className="font-mono font-medium">{row.code}</span>,
    },
    {
      key: 'name',
      header: 'Institution Name',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{row.name}</p>
          <p className="text-xs text-ink-500">{[row.city, row.state].filter(Boolean).join(', ')}</p>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => (
        <StatusBadge
          status={row.status}
          label={row.status}
          tone={row.status === 'ACTIVE' ? 'success' : 'neutral'}
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="AICTE-Approved Institutions"
        description="Directory of approved technical institutions, intake allocations, and data submission statuses."
        eyebrow="Institution Registry"
      />

      {error ? (
        <ErrorState title="Failed to load institutions" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => row.id}
            caption="List of approved institutions"
            loading={isLoading}
            emptyTitle="No institutions found"
            emptyDescription="Approved institutions will appear here once registered or seeded."
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
