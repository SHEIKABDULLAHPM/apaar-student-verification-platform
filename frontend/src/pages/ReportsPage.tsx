import { useState } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { DownloadIcon } from '@/components/icons';
import { downloadEnrolmentsCsv } from '@/api/reports';

export function ReportsPage() {
  const [downloading, setDownloading] = useState(false);

  async function handleDownloadEnrolments() {
    setDownloading(true);
    try {
      const { blob, filename } = await downloadEnrolmentsCsv({});
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download report', err);
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Regulatory Reports & Exports"
        description="Filterable, server-calculated exports for AICTE compliance audits, intake reconciliations, and institution records."
        eyebrow="Data Governance"
      />

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <Card className="p-6 space-y-4">
          <div>
            <h3 className="text-lg font-semibold text-ink-900">Enrolment & Intake Roster</h3>
            <p className="text-sm text-ink-600 mt-1">
              Download the complete national or institution student enrolment breakdown, including quota categories and APAAR status.
            </p>
          </div>
          <Button
            variant="primary"
            onClick={handleDownloadEnrolments}
            disabled={downloading}
          >
            <DownloadIcon className="h-4 w-4 mr-2" />
            {downloading ? 'Generating Report...' : 'Download Enrolments CSV'}
          </Button>
        </Card>

        <Card className="p-6 space-y-4">
          <div>
            <h3 className="text-lg font-semibold text-ink-900">APAAR Compliance Summary</h3>
            <p className="text-sm text-ink-600 mt-1">
              Comprehensive report on verified, unverified, and mismatched student identifiers across academic programs.
            </p>
          </div>
          <Button variant="secondary" disabled>
            <DownloadIcon className="h-4 w-4 mr-2" />
            Compliance Summary (PDF / CSV)
          </Button>
        </Card>
      </div>
    </div>
  );
}

