import { apiClient } from '@/lib/http';
import type { CaseDetail, CaseSummary, Paged } from '@/api/types';

export interface CaseQuery {
  page: number;
  size: number;
  status?: string;
}

export interface RecordDecisionPayload {
  decisionCode: string;
  rationale: string;
  effectiveAction?: string;
  studentIdToCorrect?: string;
  correctedFullName?: string;
  correctedDateOfBirth?: string;
  correctedGender?: string;
}

export async function listCases({ page, size, status }: CaseQuery): Promise<Paged<CaseSummary>> {
  const { data } = await apiClient.get<Paged<CaseSummary>>('/cases', {
    params: { page, size, status },
  });
  return data;
}

export async function getCase(id: string): Promise<CaseDetail> {
  const { data } = await apiClient.get<CaseDetail>(`/cases/${encodeURIComponent(id)}`);
  return data;
}

export async function addCaseComment(id: string, body: string): Promise<unknown> {
  const { data } = await apiClient.post<unknown>(
    `/cases/${encodeURIComponent(id)}/comments`,
    { body },
  );
  return data;
}

export async function recordCaseDecision(id: string, payload: RecordDecisionPayload): Promise<CaseDetail> {
  const { data } = await apiClient.post<CaseDetail>(
    `/cases/${encodeURIComponent(id)}/decisions`,
    payload,
  );
  return data;
}

export async function updateCaseStatus(id: string, status: string, notes?: string): Promise<CaseDetail> {
  const { data } = await apiClient.post<CaseDetail>(
    `/cases/${encodeURIComponent(id)}/status`,
    { status, notes },
  );
  return data;
}

export async function assignCase(id: string, assignedTo: string): Promise<CaseDetail> {
  const { data } = await apiClient.post<CaseDetail>(
    `/cases/${encodeURIComponent(id)}/assign`,
    { assignedTo },
  );
  return data;
}

export async function addCaseEvidence(
  id: string,
  type: string,
  description: string,
  storageRef?: string,
): Promise<unknown> {
  const { data } = await apiClient.post<unknown>(
    `/cases/${encodeURIComponent(id)}/evidence`,
    { type, description, storageRef },
  );
  return data;
}
