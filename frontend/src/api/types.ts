export type Role =
  | 'PLATFORM_ADMIN'
  | 'AICTE_OFFICER'
  | 'INSTITUTION_ADMIN'
  | 'INSTITUTION_OPERATOR'
  | 'VERIFICATION_OFFICER'
  | 'CASE_REVIEWER'
  | 'AUDITOR';

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
  institutionId?: string | null;
  institutionName?: string | null;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: AuthUser;
}

export interface Paged<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IntakeVsEnrolment {
  institutionName: string;
  programmeName: string;
  academicYear: string;
  approvedIntake: number;
  actualEnrolment: number;
}

export interface CountByCategory {
  category: string;
  count: number;
}

export interface VerificationActivity {
  date: string;
  count: number;
}

export interface DashboardSummary {
  generatedAt: string;
  institutionId?: string | null;
  institutionName?: string | null;
  institutionCount?: number | null;
  studentCount: number;
  enrolmentCount: number;
  apaarVerifiedCount: number;
  apaarUnverifiedCount: number;
  openConflictCount: number;
  openCaseCount: number;
  complianceFindingCount: number;
  anomalyAlertCount: number;
  intakeVsEnrolment: IntakeVsEnrolment[];
  enrolmentsByCategory: CountByCategory[];
  verificationActivity: VerificationActivity[];
}

export interface Institution {
  id: string;
  code: string;
  name: string;
  status: string;
  city?: string | null;
  state?: string | null;
}

export interface AcademicYear {
  id: string;
  code: string;
  startDate: string;
  endDate: string;
  active: boolean;
}

export interface Programme {
  id: string;
  code: string;
  name: string;
  degreeLevel?: string | null;
  approvedIntake?: number | null;
  status?: string | null;
}

export interface StudentSummary {
  id: string;
  apaarId: string;
  fullName: string;
  dateOfBirth?: string | null;
  gender?: string | null;
  category?: string | null;
  apaarFormatValid?: boolean | null;
  apaarVerificationStatus?: string | null;
  createdAt?: string | null;
}

export interface StudentEnrolment {
  id: string;
  institutionId?: string | null;
  institutionName?: string | null;
  programmeName?: string | null;
  academicYear?: string | null;
  enrolmentNumber?: string | null;
  admissionCategory?: string | null;
  status?: string | null;
}

export interface StudentDetail extends StudentSummary {
  enrolments: StudentEnrolment[];
}

export interface Enrolment {
  id: string;
  studentId?: string | null;
  studentName?: string | null;
  institutionId?: string | null;
  institutionName?: string | null;
  programmeId?: string | null;
  programmeName?: string | null;
  academicYear?: string | null;
  academicYearId?: string | null;
  enrolmentNumber?: string | null;
  admissionCategory?: string | null;
  admissionDate?: string | null;
  status?: string | null;
  createdAt?: string | null;
}

export interface ImportJob {
  id: string;
  status: string;
  totalRows?: number | null;
  validRows?: number | null;
  errorRows?: number | null;
  fileName?: string | null;
  institutionId?: string | null;
  institutionName?: string | null;
  academicYearId?: string | null;
  academicYear?: string | null;
  createdAt?: string | null;
  completedAt?: string | null;
  errors?: ImportErrorRow[] | null;
}

export interface ImportErrorRow {
  rowNumber: number;
  columnName?: string | null;
  errorCode?: string | null;
  message: string;
  rawValue?: string | null;
}

export interface FormatValidationResult {
  valid: boolean;
  reasonCode?: string | null;
  message: string;
}

export interface ApaarVerificationResult {
  status: string;
  mode: string;
  verifiedAt?: string | null;
  adapterName?: string | null;
  message?: string | null;
}

export interface PublicVerification {
  status: string;
  studentNameMasked: string;
  institutionName: string;
  programmeName: string;
  academicYear: string;
  verifiedAt?: string | null;
}

export interface ComplianceViolation {
  id: string;
  reasonCode?: string | null;
  severity?: string | null;
  status: string;
  details?: unknown;
  createdAt?: string | null;
}

export type AnomalyDecision = 'CONFIRMED' | 'DISMISSED' | 'INVESTIGATING';

export interface Anomaly {
  id: string;
  type?: string | null;
  anomalyType?: string | null;
  severity?: string | null;
  status: string;
  description?: string | null;
  institutionName?: string | null;
  studentId?: string | null;
  createdAt?: string | null;
}

export interface CaseSummary {
  id: string;
  caseNumber?: string | null;
  title?: string | null;
  subject?: string | null;
  status: string;
  priority?: string | null;
  institutionName?: string | null;
  assignedTo?: string | null;
  openedAt?: string | null;
  createdAt?: string | null;
}

export interface CaseEvidence {
  id?: string;
  type?: string | null;
  description?: string | null;
  fileRef?: string | null;
  createdAt?: string | null;
  [key: string]: unknown;
}

export interface CaseComment {
  id?: string;
  authorName?: string | null;
  authorEmail?: string | null;
  body?: string | null;
  createdAt?: string | null;
  [key: string]: unknown;
}

export interface CaseDecision {
  id?: string;
  decision?: string | null;
  reason?: string | null;
  decidedBy?: string | null;
  createdAt?: string | null;
  [key: string]: unknown;
}

export interface CaseDetail extends CaseSummary {
  description?: string | null;
  evidence?: CaseEvidence[];
  comments?: CaseComment[];
  decisions?: CaseDecision[];
  [key: string]: unknown;
}

export interface AuditEntry {
  id?: string;
  action?: string | null;
  actor?: string | null;
  actorEmail?: string | null;
  entityType?: string | null;
  entityId?: string | null;
  outcome?: string | null;
  ipAddress?: string | null;
  details?: unknown;
  createdAt?: string | null;
  occurredAt?: string | null;
  timestamp?: string | null;
  [key: string]: unknown;
}

export interface UploadResponseDto {
  importId: string;
  status: string;
  filename: string;
  fileSize: number;
  totalRowsDetected: number;
  sourceHeaders: string[];
  suggestedMappings: Record<string, string>;
  unmappedHeaders: string[];
  missingMandatoryFields: string[];
  sampleRows: Record<string, string>[];
}

export interface RowErrorDto {
  rowNumber: number;
  columnName?: string | null;
  errorCode: string;
  message: string;
  rawValue?: string | null;
}

export interface NormalizedRowDto {
  id: string;
  rowNumber: number;
  rowStatus: string;
  reconciliationAction: string;
  errorCount: number;
  payload?: Record<string, unknown> | null;
  rawData?: Record<string, string> | null;
  errors: RowErrorDto[];
}

export interface ImportPreviewDto {
  job: ImportJob;
  totalRows: number;
  validRows: number;
  errorRows: number;
  duplicateRows: number;
  reviewRows: number;
  rows: NormalizedRowDto[];
}

export interface CommitResponseDto {
  importId: string;
  status: string;
  insertedRows: number;
  updatedRows: number;
  totalCommitted: number;
  completedAt: string;
}

export interface ReconciliationItemDto {
  studentIdentifier: string;
  apaarId?: string | null;
  studentName: string;
  action: string;
  changedFields: Record<string, unknown>;
}

export interface ReconciliationSummaryDto {
  importJobId: string;
  institutionId: string;
  academicYearId: string;
  programmeId?: string | null;
  totalCurrentSubmission: number;
  newAdmissions: number;
  unchangedCount: number;
  updatedCount: number;
  missingFromSnapshotCount: number;
  items: ReconciliationItemDto[];
}

