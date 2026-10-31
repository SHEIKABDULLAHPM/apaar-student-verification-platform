import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listEnrolments } from '@/api/enrolments';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import type { Enrolment } from '@/api/types';

export function EnrolmentsPage() {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listEnrolments({ page, size }),
    [page, size]
  );

  const columns: DataTableColumn<Enrolment>[] = [
    {
      key: 'enrolmentNumber',
      header: 'Enrolment #',
      render: (row) => <span className="font-mono font-medium">{row.enrolmentNumber}</span>,
    },
    {
      key: 'student',
      header: 'Student',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{row.studentName ?? 'Student Record'}</p>
          <p className="text-xs text-ink-500">ID: {row.studentId ? row.studentId.slice(0, 8) + '...' : '-'}</p>
        </div>
      ),
    },
    {
      key: 'programme',
      header: 'Programme & Year',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{row.programmeName ?? 'General'}</p>
          <p className="text-xs text-ink-500">AY: {row.academicYear ?? '2026-27'}</p>
        </div>
      ),
    },
    {
      key: 'category',
      header: 'Quota Category',
      render: (row) => <span className="font-medium">{row.admissionCategory ?? 'GENERAL'}</span>,
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
        title="Institutional Enrolments"
        description="Active and historical student enrolments mapped to technical institutions and sanctioned seats."
        eyebrow="Admissions Roster"
      />

      {error ? (
        <ErrorState title="Failed to load enrolments" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => row.id}
            caption="List of student enrolments"
            loading={isLoading}
            emptyTitle="No enrolments found"
            emptyDescription="Enrolments will show up here after bulk import or individual admission submission."
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
