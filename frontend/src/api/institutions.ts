import { apiClient } from '@/lib/http';
import type { Institution, Paged } from '@/api/types';

export interface InstitutionQuery {
  page: number;
  size: number;
}

export async function listInstitutions({
  page,
  size,
}: InstitutionQuery): Promise<Paged<Institution>> {
  const { data } = await apiClient.get<Paged<Institution>>('/institutions', {
    params: { page, size },
  });
  return data;
}
