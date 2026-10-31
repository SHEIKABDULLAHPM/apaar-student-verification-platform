import { apiClient } from '@/lib/http';
import type { Anomaly, AnomalyDecision, Paged } from '@/api/types';

export interface AnomalyQuery {
  page: number;
  size: number;
  status?: string;
}

export async function listAnomalies({
  page,
  size,
  status,
}: AnomalyQuery): Promise<Paged<Anomaly>> {
  const { data } = await apiClient.get<Paged<Anomaly>>('/anomalies', {
    params: { page, size, status },
  });
  return data;
}

export interface AnomalyReviewInput {
  decision: AnomalyDecision;
  notes?: string;
}

export async function reviewAnomaly(
  id: string,
  input: AnomalyReviewInput,
): Promise<Anomaly> {
  const { data } = await apiClient.patch<Anomaly>(
    `/anomalies/${encodeURIComponent(id)}/review`,
    input,
  );
  return data;
}
