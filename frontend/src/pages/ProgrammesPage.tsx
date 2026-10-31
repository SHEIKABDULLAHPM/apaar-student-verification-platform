import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listProgrammes } from '@/api/academics';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import type { Programme } from '@/api/types';

export function ProgrammesPage() {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listProgrammes({ page, size }),
    [page, size]
  );

  const columns: DataTableColumn<Programme>[] = [
    {
      key: 'code',
      header: 'Programme Code',
      render: (row) => <span className="font-mono font-medium">{row.code}</span>,
    },
    {
      key: 'name',
      header: 'Programme Name',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{row.name}</p>
          <p className="text-xs text-ink-500">Degree Level: {row.degreeLevel ?? 'UG'}</p>
        </div>
      ),
    },
    {
      key: 'approvedIntake',
      header: 'Approved Intake',
      render: (row) => (
        <span className="font-semibold">{row.approvedIntake ?? 'Not Configured'}</span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => (
        <StatusBadge
          status={row.status ?? 'ACTIVE'}
          label={row.status ?? 'ACTIVE'}
          tone={row.status === 'ACTIVE' ? 'success' : 'neutral'}
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Academic Programmes & Sanctioned Intake"
        description="Approved technical branches, degree levels, and annual intake limits set by regulatory approvals."
        eyebrow="Curriculum & Capacity"
      />

      {error ? (
        <ErrorState title="Failed to load programmes" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => row.id}
            caption="List of approved programmes"
            loading={isLoading}
            emptyTitle="No programmes found"
            emptyDescription="Programmes and intake quotas will be listed here once configured."
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
