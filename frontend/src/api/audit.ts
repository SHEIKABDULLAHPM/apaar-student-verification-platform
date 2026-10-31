import { apiClient } from '@/lib/http';
import type { AuditEntry, Paged } from '@/api/types';

export async function listAuditEntries({
  page,
  size,
}: {
  page: number;
  size: number;
}): Promise<Paged<AuditEntry>> {
  const { data } = await apiClient.get<Paged<AuditEntry>>('/audit', {
    params: { page, size },
  });
  return data;
}
