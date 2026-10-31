import { apiClient } from '@/lib/http';
import type {
  ImportJob,
  Paged,
  RowErrorDto,
  UploadResponseDto,
  ImportPreviewDto,
  CommitResponseDto,
  ReconciliationSummaryDto,
} from '@/api/types';

export interface ImportUploadInput {
  file: File;
  institutionId?: string;
  academicYearId: string;
  programmeId?: string;
  parentImportId?: string;
}

export async function uploadImport({
  file,
  institutionId,
  academicYearId,
  programmeId,
  parentImportId,
}: ImportUploadInput): Promise<UploadResponseDto> {
  const formData = new FormData();
  formData.append('file', file);
  if (institutionId) formData.append('institutionId', institutionId);
  formData.append('academicYearId', academicYearId);
  if (programmeId) formData.append('programmeId', programmeId);
  if (parentImportId) formData.append('parentImportId', parentImportId);

  const { data } = await apiClient.post<UploadResponseDto>('/imports', formData);
  return data;
}

export async function listImports({
  institutionId,
  academicYearId,
  status,
  page = 0,
  size = 20,
}: {
  institutionId?: string;
  academicYearId?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<Paged<ImportJob>> {
  const { data } = await apiClient.get<Paged<ImportJob>>('/imports', {
    params: { institutionId, academicYearId, status, page, size },
  });
  return data;
}

export async function getImport(id: string): Promise<ImportJob> {
  const { data } = await apiClient.get<ImportJob>(`/imports/${encodeURIComponent(id)}`);
  return data;
}

export async function applyMappings(
  id: string,
  mappings: Record<string, string>,
): Promise<ImportJob> {
  const { data } = await apiClient.post<ImportJob>(
    `/imports/${encodeURIComponent(id)}/mappings`,
    { mappings },
  );
  return data;
}

export async function getImportPreview(
  id: string,
  status?: string,
  page = 0,
  size = 50,
): Promise<ImportPreviewDto> {
  const { data } = await apiClient.get<ImportPreviewDto>(
    `/imports/${encodeURIComponent(id)}/preview`,
    { params: { status, page, size } },
  );
  return data;
}

export async function listImportErrors(
  id: string,
  { page = 0, size = 50 }: { page?: number; size?: number },
): Promise<Paged<RowErrorDto>> {
  const { data } = await apiClient.get<Paged<RowErrorDto>>(
    `/imports/${encodeURIComponent(id)}/errors`,
    { params: { page, size } },
  );
  return data;
}

export async function downloadErrorReport(
  id: string,
  format: 'csv' | 'xlsx' = 'csv',
): Promise<Blob> {
  const { data } = await apiClient.get<Blob>(
    `/imports/${encodeURIComponent(id)}/error-report`,
    {
      params: { format },
      responseType: 'blob',
    },
  );
  return data;
}

export async function commitImport(id: string): Promise<CommitResponseDto> {
  const { data } = await apiClient.post<CommitResponseDto>(
    `/imports/${encodeURIComponent(id)}/commit`,
  );
  return data;
}

export async function getReconciliation(id: string): Promise<ReconciliationSummaryDto> {
  const { data } = await apiClient.get<ReconciliationSummaryDto>(
    `/imports/${encodeURIComponent(id)}/reconciliation`,
  );
  return data;
}

export async function resubmitImport({
  id,
  file,
}: {
  id: string;
  file: File;
}): Promise<UploadResponseDto> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await apiClient.post<UploadResponseDto>(
    `/imports/${encodeURIComponent(id)}/resubmissions`,
    formData,
  );
  return data;
}
