import { apiClient } from '@/lib/http';
import { filenameFromDisposition } from '@/lib/format';

export interface EnrolmentReportQuery {
  institutionId?: string;
  academicYearId?: string;
}

export interface ReportDownload {
  blob: Blob;
  filename: string;
}

export async function downloadEnrolmentsCsv(
  params: EnrolmentReportQuery,
): Promise<ReportDownload> {
  const response = await apiClient.get<Blob>('/reports/enrolments.csv', {
    params,
    responseType: 'blob',
  });

  const dispositionHeader = response.headers['content-disposition'];
  const filename = filenameFromDisposition(
    typeof dispositionHeader === 'string' ? dispositionHeader : undefined,
    'enrolments.csv',
  );

  return { blob: response.data, filename };
}
