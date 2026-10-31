import { apiClient } from '@/lib/http';
import type { Paged, CaseDetail } from '@/api/types';

export interface FormatValidationResponse {
  valid: boolean;
  apaarId: string;
  reasonCode: string;
  message: string;
  status: string;
  validatedAt: string;
}

export interface VerificationRecord {
  id: string;
  studentId: string;
  studentName: string;
  apaarId: string;
  status: string;
  verificationMode: string;
  adapterName: string;
  providerReference?: string | null;
  reasonCode?: string | null;
  simulated: boolean;
  authoritative: boolean;
  message?: string | null;
  verifiedAt?: string | null;
  requestedAt: string;
}

export interface MatchingRunRequest {
  academicYearId?: string | null;
  institutionId?: string | null;
  runType?: string;
}

export interface MatchingRun {
  id: string;
  runType: string;
  status: string;
  recordsScanned: number;
  matchesFound: number;
  conflictsGenerated: number;
  conflictsSuppressed: number;
  runBy?: string | null;
  startedAt: string;
  completedAt?: string | null;
  summary: Record<string, unknown>;
}

export interface IdentityMatch {
  id: string;
  runId: string;
  matchType: string;
  matchingRule: string;
  primaryStudentId: string;
  primaryStudentName?: string | null;
  primaryEnrolmentId?: string | null;
  primaryInstitutionName?: string | null;
  secondaryStudentId?: string | null;
  secondaryStudentName?: string | null;
  secondaryEnrolmentId?: string | null;
  secondaryInstitutionName?: string | null;
  confidenceScore: number;
  matchClassification: string;
  reviewRequired: boolean;
  matchingFields: string[];
  conflictingFields: string[];
  details: Record<string, unknown>;
  conflictId?: string | null;
  createdAt: string;
}

export interface ConflictSummary {
  id: string;
  conflictType: string;
  status: string;
  severity: string;
  reasonCode: string;
  detectionSource: string;
  primaryStudentId?: string | null;
  primaryStudentName?: string | null;
  secondaryStudentId?: string | null;
  secondaryStudentName?: string | null;
  primaryEnrolmentId?: string | null;
  secondaryEnrolmentId?: string | null;
  assignedTo?: string | null;
  details: Record<string, unknown>;
  createdAt: string;
  resolvedAt?: string | null;
}

export interface ConflictEvidence {
  conflictId: string;
  conflictType: string;
  matchingRule: string;
  confidenceScore: number;
  primaryRecord: Record<string, unknown>;
  secondaryRecord: Record<string, unknown>;
  matchingFields: string[];
  conflictingFields: string[];
  verificationHistory: VerificationRecord[];
  explanation: string;
}

export interface IdentityDashboardMetrics {
  totalIdentitiesAssessed: number;
  formatValidCount: number;
  missingOrMalformedCount: number;
  verificationPendingCount: number;
  authoritativelyVerifiedCount: number;
  simulatedVerifiedCount: number;
  potentialDuplicatesCount: number;
  openConflictsCount: number;
  casesAwaitingReviewCount: number;
  resolvedCasesCount: number;
}

// Format validation
export async function validateApaarFormat(apaarId: string, studentId?: string): Promise<FormatValidationResponse> {
  const { data } = await apiClient.post<FormatValidationResponse>('/identity/apaar/validate', {
    apaarId,
    studentId: studentId || null,
  });
  return data;
}

// Verification
export async function requestApaarVerification(studentId: string, mode: 'SIMULATED' | 'AUTHORIZED' = 'SIMULATED'): Promise<VerificationRecord> {
  const { data } = await apiClient.post<VerificationRecord>('/identity/apaar/verification-requests', {
    studentId,
    mode,
  });
  return data;
}

export async function getStudentVerificationHistory(studentId: string): Promise<VerificationRecord[]> {
  const { data } = await apiClient.get<VerificationRecord[]>(`/identity/students/${encodeURIComponent(studentId)}/verification-history`);
  return data;
}

// Matching Runs
export async function triggerMatchingRun(request: MatchingRunRequest = {}): Promise<MatchingRun> {
  const { data } = await apiClient.post<MatchingRun>('/identity/matching-runs', request);
  return data;
}

export async function listMatchingRuns(page = 0, size = 10): Promise<Paged<MatchingRun>> {
  const { data } = await apiClient.get<Paged<MatchingRun>>('/identity/matching-runs', {
    params: { page, size },
  });
  return data;
}

export async function listIdentityMatches(classification?: string, page = 0, size = 20): Promise<Paged<IdentityMatch>> {
  const { data } = await apiClient.get<Paged<IdentityMatch>>('/identity/matches', {
    params: { classification, page, size },
  });
  return data;
}

// Conflicts
export async function listIdentityConflicts(status?: string, page = 0, size = 20): Promise<Paged<ConflictSummary>> {
  const { data } = await apiClient.get<Paged<ConflictSummary>>('/identity/conflicts', {
    params: { status, page, size },
  });
  return data;
}

export async function getConflictEvidence(conflictId: string): Promise<ConflictEvidence> {
  const { data } = await apiClient.get<ConflictEvidence>(`/identity/conflicts/${encodeURIComponent(conflictId)}/evidence`);
  return data;
}

export async function updateConflictStatus(conflictId: string, status: string, resolutionNotes?: string): Promise<ConflictSummary> {
  const { data } = await apiClient.patch<ConflictSummary>(`/identity/conflicts/${encodeURIComponent(conflictId)}`, {
    status,
    resolutionNotes,
  });
  return data;
}

export async function createCaseFromConflict(conflictId: string): Promise<CaseDetail> {
  const { data } = await apiClient.post<CaseDetail>(`/identity/conflicts/${encodeURIComponent(conflictId)}/investigations`);
  return data;
}

// Dashboard
export async function getIdentityDashboard(): Promise<IdentityDashboardMetrics> {
  const { data } = await apiClient.get<IdentityDashboardMetrics>('/identity/dashboard');
  return data;
}

