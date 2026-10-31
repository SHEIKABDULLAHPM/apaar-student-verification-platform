import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listComplianceViolations } from '@/api/compliance';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import type { ComplianceViolation } from '@/api/types';

export function CompliancePage() {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listComplianceViolations({ page, size }),
    [page, size]
  );

  const columns: DataTableColumn<ComplianceViolation>[] = [
    {
      key: 'reasonCode',
      header: 'Finding Code',
      render: (row) => <span className="font-mono font-semibold">{row.reasonCode ?? 'N/A'}</span>,
    },
    {
      key: 'severity',
      header: 'Severity',
      render: (row) => {
        const severity = row.severity ?? 'MEDIUM';
        const tone =
          severity === 'CRITICAL' || severity === 'HIGH'
            ? 'danger'
            : severity === 'MEDIUM'
              ? 'warning'
              : 'info';
        return <StatusBadge status={severity} label={severity} tone={tone} />;
      },
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => (
        <StatusBadge
          status={row.status}
          label={row.status}
          tone={row.status === 'RESOLVED' ? 'success' : 'neutral'}
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Admission & Regulatory Compliance"
        description="Automated intake validation, category quota compliance checks, and regulatory discrepancy findings."
        eyebrow="AICTE Norms Enforcement"
      />

      {error ? (
        <ErrorState title="Failed to load compliance violations" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => row.id}
            caption="List of compliance findings"
            loading={isLoading}
            emptyTitle="No compliance discrepancies"
            emptyDescription="Institutions meet approved intake capacity and category limits."
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
