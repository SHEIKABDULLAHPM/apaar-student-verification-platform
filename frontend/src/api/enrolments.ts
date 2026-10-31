import { apiClient } from '@/lib/http';
import type { Enrolment, Paged } from '@/api/types';

export interface EnrolmentQuery {
  page: number;
  size: number;
  institutionId?: string;
  academicYearId?: string;
  programmeId?: string;
}

export async function listEnrolments({
  page,
  size,
  institutionId,
  academicYearId,
  programmeId,
}: EnrolmentQuery): Promise<Paged<Enrolment>> {
  const { data } = await apiClient.get<Paged<Enrolment>>('/enrolments', {
    params: { page, size, institutionId, academicYearId, programmeId },
  });
  return data;
}
