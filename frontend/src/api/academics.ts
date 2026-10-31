import { apiClient } from '@/lib/http';
import type { AcademicYear, Paged, Programme } from '@/api/types';

export async function listAcademicYears(): Promise<AcademicYear[]> {
  const { data } = await apiClient.get<AcademicYear[]>('/academic-years');
  return data;
}

export interface ProgrammeQuery {
  institutionId?: string;
  page?: number;
  size?: number;
}

export async function listProgrammes({
  institutionId,
  page = 0,
  size = 100,
}: ProgrammeQuery = {}): Promise<Paged<Programme>> {
  const { data } = await apiClient.get<Paged<Programme>>('/programmes', {
    params: { institutionId, page, size },
  });
  return data;
}
