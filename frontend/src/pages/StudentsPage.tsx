import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listStudents } from '@/api/students';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import { FilterBar } from '@/components/ui/FilterBar';
import { Input } from '@/components/ui/Input';
import type { StudentSummary } from '@/api/types';

export function StudentsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const size = 10;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listStudents({ page, size, search: search.trim() || undefined }),
    [page, size, search]
  );

  const columns: DataTableColumn<StudentSummary>[] = [
    {
      key: 'fullName',
      header: 'Student Name',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{row.fullName}</p>
          <p className="text-xs text-ink-500">DOB: {row.dateOfBirth ?? 'N/A'}</p>
        </div>
      ),
    },
    {
      key: 'apaarId',
      header: 'APAAR Identifier',
      render: (row) => (
        <div>
          <span className="font-mono text-sm font-semibold">{row.apaarId || 'Not Assigned'}</span>
          {row.apaarFormatValid !== undefined && row.apaarFormatValid !== null && (
            <p className="text-xs text-ink-500">
              Format: {row.apaarFormatValid ? '✓ Valid (12-digit)' : '✗ Invalid Format'}
            </p>
          )}
        </div>
      ),
    },
    {
      key: 'category',
      header: 'Category',
      render: (row) => row.category ?? 'GENERAL',
    },
    {
      key: 'verification',
      header: 'APAAR Verification Status',
      render: (row) => {
        const status = row.apaarVerificationStatus ?? 'PENDING';
        const tone =
          status === 'VERIFIED'
            ? 'success'
            : status === 'MISMATCH'
              ? 'danger'
              : status === 'NOT_VERIFIED'
                ? 'warning'
                : 'neutral';
        return <StatusBadge status={status} label={status} tone={tone} />;
      },
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Student Registry"
        description="Centralized student identities, APAAR validation status, and institutional affiliation history."
        eyebrow="Identity Resolution"
      />

      <FilterBar
        onApply={() => reload()}
        onReset={() => {
          setSearch('');
          setPage(0);
        }}
      >
        <Input
          placeholder="Search by student name or APAAR ID..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
      </FilterBar>

      {error ? (
        <ErrorState title="Failed to load students" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => row.id}
            caption="Directory of registered students"
            loading={isLoading}
            emptyTitle="No students found"
            emptyDescription="Student records will appear here after import or manual enrollment."
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
