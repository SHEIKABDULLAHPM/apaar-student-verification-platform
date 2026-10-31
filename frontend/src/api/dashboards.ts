import { apiClient } from '@/lib/http';
import type { DashboardSummary } from '@/api/types';

export async function getAicteDashboard(): Promise<DashboardSummary> {
  const { data } = await apiClient.get<DashboardSummary>('/dashboards/aicte');
  return data;
}

export async function getInstitutionDashboard(): Promise<DashboardSummary> {
  const { data } = await apiClient.get<DashboardSummary>('/dashboards/institution');
  return data;
}
