import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listAnomalies } from '@/api/anomalies';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import type { Anomaly } from '@/api/types';

export function AnomaliesPage() {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, error, reload } = useApiQuery(
    () => listAnomalies({ page, size }),
    [page, size]
  );

  const columns: DataTableColumn<Anomaly>[] = [
    {
      key: 'anomalyType',
      header: 'Anomaly Type',
      render: (row) => (
        <div>
          <span className="font-mono font-bold text-ink-900">
            {row.anomalyType ?? row.type ?? 'STATISTICAL_OUTLIER'}
          </span>
          <p className="text-xs text-ink-500">{row.description ?? 'Anomaly pattern flagged for investigation'}</p>
        </div>
      ),
    },
    {
      key: 'institution',
      header: 'Institution',
      render: (row) => row.institutionName ?? 'All Institutions',
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
      header: 'Review Status',
      render: (row) => (
        <StatusBadge
          status={row.status}
          label={row.status}
          tone={row.status === 'NEW' ? 'warning' : 'neutral'}
        />
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="AI-Assisted Regulatory Intelligence"
        description="Machine learning and statistical pattern anomalies, duplicate signals, and explainable risk scores."
        eyebrow="Pattern Recognition & Triage"
      />

      {error ? (
        <ErrorState title="Failed to load anomaly results" error={error} onRetry={reload} />
      ) : (
        <div className="space-y-4">
          <DataTable
            columns={columns}
            rows={data?.content ?? []}
            getRowId={(row) => row.id}
            caption="Flagged AI anomalies requiring officer review"
            loading={isLoading}
            emptyTitle="No anomalies detected"
            emptyDescription="All records fall within normal institutional baseline distributions."
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
