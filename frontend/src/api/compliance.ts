import { apiClient } from '@/lib/http';
import type { ComplianceViolation, Paged } from '@/api/types';

export interface ComplianceQuery {
  page: number;
  size: number;
  status?: string;
}

export async function listComplianceViolations({
  page,
  size,
  status,
}: ComplianceQuery): Promise<Paged<ComplianceViolation>> {
  const { data } = await apiClient.get<Paged<ComplianceViolation>>(
    '/compliance/violations',
    { params: { page, size, status } },
  );
  return data;
}

export interface ComplianceEvaluationInput {
  institutionId: string;
  academicYearId: string;
}

export async function evaluateCompliance(
  input: ComplianceEvaluationInput,
): Promise<Record<string, unknown>> {
  const { data } = await apiClient.post<Record<string, unknown>>(
    '/compliance/evaluations',
    input,
  );
  return data;
}
