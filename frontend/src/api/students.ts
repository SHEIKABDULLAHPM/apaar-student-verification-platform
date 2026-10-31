import { apiClient } from '@/lib/http';
import type { Paged, StudentDetail, StudentSummary } from '@/api/types';

export interface StudentQuery {
  page: number;
  size: number;
  search?: string;
}

export async function listStudents({
  page,
  size,
  search,
}: StudentQuery): Promise<Paged<StudentSummary>> {
  const { data } = await apiClient.get<Paged<StudentSummary>>('/students', {
    params: { page, size, search },
  });
  return data;
}

export async function getStudent(id: string): Promise<StudentDetail> {
  const { data } = await apiClient.get<StudentDetail>(`/students/${encodeURIComponent(id)}`);
  return data;
}
