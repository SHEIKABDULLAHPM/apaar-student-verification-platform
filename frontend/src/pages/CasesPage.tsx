import { useState } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import { listCases, getCase, addCaseComment, recordCaseDecision, type RecordDecisionPayload } from '@/api/cases';
import {
  getIdentityDashboard,
  listIdentityConflicts,
  getConflictEvidence,
  createCaseFromConflict,
  triggerMatchingRun,
  listMatchingRuns,
  validateApaarFormat,
  requestApaarVerification,
  type ConflictSummary,
  type ConflictEvidence,
  type FormatValidationResponse,
  type VerificationRecord,
} from '@/api/identity';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ErrorState } from '@/components/ui/ErrorState';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Alert } from '@/components/ui/Alert';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Select } from '@/components/ui/Select';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import {
  ScaleIcon,
  ShieldCheckIcon,
  AlertTriangleIcon,
  RefreshIcon,
  CheckCircleIcon,
  XCircleIcon,
  UsersIcon,
  BookIcon,
  InfoIcon,
} from '@/components/icons';
import type { CaseDetail, CaseSummary } from '@/api/types';

export function CasesPage() {
  const [activeTab, setActiveTab] = useState<'overview' | 'conflicts' | 'cases'>('overview');

  // Cases Pagination & Filter
  const [casePage, setCasePage] = useState(0);
  const [caseStatus, setCaseStatus] = useState<string>('');
  const caseSize = 10;

  // Conflicts Pagination & Filter
  const [conflictPage, setConflictPage] = useState(0);
  const [conflictStatus, setConflictStatus] = useState<string>('OPEN');
  const conflictSize = 10;

  // Selected entities for modals
  const [selectedCaseId, setSelectedCaseId] = useState<string | null>(null);
  const [selectedCaseDetail, setSelectedCaseDetail] = useState<CaseDetail | null>(null);
  const [loadingCaseDetail, setLoadingCaseDetail] = useState(false);

  const [selectedConflictId, setSelectedConflictId] = useState<string | null>(null);
  const [conflictEvidence, setConflictEvidence] = useState<ConflictEvidence | null>(null);
  const [loadingEvidence, setLoadingEvidence] = useState(false);

  // Decision Form State
  const [decisionCode, setDecisionCode] = useState('IDENTITY_CONFIRMED');
  const [decisionRationale, setDecisionRationale] = useState('');
  const [effectiveAction, setEffectiveAction] = useState('');
  const [correctedName, setCorrectedName] = useState('');
  const [correctedDob, setCorrectedDob] = useState('');
  const [submittingDecision, setSubmittingDecision] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);

  // Matching Engine Trigger State
  const [isRunningEngine, setIsRunningEngine] = useState(false);
  const [engineNotification, setEngineNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // APAAR Format Validator Playground State
  const [validatorApaarInput, setValidatorApaarInput] = useState('');
  const [validatingApaar, setValidatingApaar] = useState(false);
  const [validationOutcome, setValidationOutcome] = useState<FormatValidationResponse | null>(null);

  // Adapter Verification Playground State
  const [verificationStudentId, setVerificationStudentId] = useState('');
  const [verificationMode, setVerificationMode] = useState<'SIMULATED' | 'AUTHORIZED'>('SIMULATED');
  const [verifyingStudent, setVerifyingStudent] = useState(false);
  const [verificationOutcome, setVerificationOutcome] = useState<VerificationRecord | null>(null);

  // Queries
  const { data: dashboard, reload: reloadDashboard } = useApiQuery(
    () => getIdentityDashboard(),
    []
  );

  const { data: matchingRuns, reload: reloadRuns } = useApiQuery(
    () => listMatchingRuns(0, 5),
    []
  );

  const { data: conflictsData, isLoading: loadingConflicts, reload: reloadConflicts } = useApiQuery(
    () => listIdentityConflicts(conflictStatus || undefined, conflictPage, conflictSize),
    [conflictStatus, conflictPage]
  );

  const { data: casesData, isLoading: loadingCases, error: casesError, reload: reloadCases } = useApiQuery(
    () => listCases({ page: casePage, size: caseSize, status: caseStatus || undefined }),
    [casePage, caseStatus]
  );

  // Handlers
  const handleRunMatchingEngine = async () => {
    setIsRunningEngine(true);
    setEngineNotification(null);
    try {
      const run = await triggerMatchingRun({ runType: 'EXACT_MATCH' });
      setEngineNotification({
        type: 'success',
        message: `Matching Run completed! Scanned ${run.recordsScanned} records, found ${run.matchesFound} matches, generated ${run.conflictsGenerated} conflicts (${run.conflictsSuppressed} duplicate conflicts suppressed).`,
      });
      reloadDashboard();
      reloadRuns();
      reloadConflicts();
      reloadCases();
    } catch (err: unknown) {
      setEngineNotification({
        type: 'error',
        message: err instanceof Error ? err.message : 'Failed to execute matching engine run',
      });
    } finally {
      setIsRunningEngine(false);
    }
  };

  const handleTestApaarFormat = async () => {
    if (!validatorApaarInput.trim()) return;
    setValidatingApaar(true);
    setValidationOutcome(null);
    try {
      const res = await validateApaarFormat(validatorApaarInput.trim());
      setValidationOutcome(res);
    } catch (err: unknown) {
      setValidationOutcome({
        valid: false,
        apaarId: validatorApaarInput,
        reasonCode: 'VALIDATION_FAILED',
        message: err instanceof Error ? err.message : 'Server-side validation failed',
        status: 'FORMAT_INVALID',
        validatedAt: new Date().toISOString(),
      });
    } finally {
      setValidatingApaar(false);
    }
  };

  const handleTestVerification = async () => {
    if (!verificationStudentId.trim()) return;
    setVerifyingStudent(true);
    setVerificationOutcome(null);
    try {
      const res = await requestApaarVerification(verificationStudentId.trim(), verificationMode);
      setVerificationOutcome(res);
      reloadDashboard();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Verification request failed');
    } finally {
      setVerifyingStudent(false);
    }
  };

  const handleInspectEvidence = async (conflictId: string) => {
    setSelectedConflictId(conflictId);
    setLoadingEvidence(true);
    try {
      const ev = await getConflictEvidence(conflictId);
      setConflictEvidence(ev);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to fetch conflict evidence');
    } finally {
      setLoadingEvidence(false);
    }
  };

  const handlePromoteToCase = async (conflictId: string) => {
    try {
      const newCase = await createCaseFromConflict(conflictId);
      alert(`Investigation Case ${newCase.caseNumber} successfully created from conflict!`);
      setSelectedConflictId(null);
      setConflictEvidence(null);
      reloadConflicts();
      reloadCases();
      reloadDashboard();
      setActiveTab('cases');
      openCaseDetail(newCase.id);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to promote conflict to investigation case');
    }
  };

  const openCaseDetail = async (id: string) => {
    setSelectedCaseId(id);
    setLoadingCaseDetail(true);
    try {
      const detail = await getCase(id);
      setSelectedCaseDetail(detail);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to fetch case dossier');
    } finally {
      setLoadingCaseDetail(false);
    }
  };

  const handleAddComment = async () => {
    if (!selectedCaseId || !newComment.trim()) return;
    setSubmittingComment(true);
    try {
      await addCaseComment(selectedCaseId, newComment.trim());
      setNewComment('');
      const updated = await getCase(selectedCaseId);
      setSelectedCaseDetail(updated);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to submit comment');
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleRecordDecision = async () => {
    if (!selectedCaseId || !decisionRationale.trim()) return;
    setSubmittingDecision(true);
    try {
      const payload: RecordDecisionPayload = {
        decisionCode,
        rationale: decisionRationale.trim(),
        effectiveAction: effectiveAction.trim() || undefined,
        correctedFullName: correctedName.trim() || undefined,
        correctedDateOfBirth: correctedDob.trim() || undefined,
      };
      const updated = await recordCaseDetailDecision(selectedCaseId, payload);
      setSelectedCaseDetail(updated);
      alert(`Formal decision [${decisionCode}] recorded. Case and linked conflicts updated.`);
      setDecisionRationale('');
      setEffectiveAction('');
      setCorrectedName('');
      setCorrectedDob('');
      reloadCases();
      reloadConflicts();
      reloadDashboard();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to record decision');
    } finally {
      setSubmittingDecision(false);
    }
  };

  async function recordCaseDetailDecision(id: string, payload: RecordDecisionPayload): Promise<CaseDetail> {
    return await recordCaseDecision(id, payload);
  }

  // Conflict Table Columns
  const conflictColumns: DataTableColumn<ConflictSummary>[] = [
    {
      key: 'conflictType',
      header: 'Conflict Type & Code',
      render: (row) => (
        <div>
          <span className="font-semibold text-ink-900">{row.conflictType.replace(/_/g, ' ')}</span>
          <p className="text-xs font-mono text-ink-500 mt-0.5">{row.reasonCode}</p>
        </div>
      ),
    },
    {
      key: 'severity',
      header: 'Severity',
      render: (row) => {
        const sev = row.severity || 'MEDIUM';
        const tone = sev === 'CRITICAL' || sev === 'HIGH' ? 'danger' : sev === 'MEDIUM' ? 'warning' : 'info';
        return <StatusBadge status={sev} label={sev} tone={tone} />;
      },
    },
    {
      key: 'status',
      header: 'Review Status',
      render: (row) => {
        const tone = row.status === 'RESOLVED' ? 'success' : row.status === 'INVESTIGATING' ? 'info' : 'warning';
        return <StatusBadge status={row.status} label={row.status} tone={tone} />;
      },
    },
    {
      key: 'students',
      header: 'Involved Students',
      render: (row) => (
        <div className="text-xs space-y-0.5">
          <p className="font-medium text-ink-800">Primary: {row.primaryStudentName || 'Record A'}</p>
          <p className="text-ink-600">Secondary: {row.secondaryStudentName || 'Record B'}</p>
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="secondary"
            onClick={() => handleInspectEvidence(row.id)}
          >
            Inspect Evidence
          </Button>
          {row.status === 'OPEN' && (
            <Button
              size="sm"
              variant="primary"
              onClick={() => handlePromoteToCase(row.id)}
            >
              Open Case
            </Button>
          )}
        </div>
      ),
    },
  ];

  // Cases Table Columns
  const caseColumns: DataTableColumn<CaseSummary>[] = [
    {
      key: 'caseNumber',
      header: 'Case #',
      render: (row) => <span className="font-mono font-semibold text-ink-900">{row.caseNumber ?? 'CASE'}</span>,
    },
    {
      key: 'title',
      header: 'Subject & Scope',
      render: (row) => (
        <div>
          <p className="font-medium text-ink-900">{row.title ?? row.subject ?? 'Case Investigation'}</p>
          <p className="text-xs text-ink-500">Institution: {row.institutionName ?? 'Platform Governance'}</p>
        </div>
      ),
    },
    {
      key: 'priority',
      header: 'Priority',
      render: (row) => {
        const priority = row.priority ?? 'MEDIUM';
        const tone = priority === 'CRITICAL' || priority === 'HIGH' ? 'danger' : 'warning';
        return <StatusBadge status={priority} label={priority} tone={tone} />;
      },
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => (
        <StatusBadge
          status={row.status}
          label={row.status}
          tone={row.status === 'RESOLVED' || row.status === 'CLOSED' ? 'success' : 'warning'}
        />
      ),
    },
    {
      key: 'actions',
      header: 'Dossier',
      render: (row) => (
        <Button size="sm" variant="secondary" onClick={() => openCaseDetail(row.id)}>
          View Dossier
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="APAAR Identity Resolution & Case Management"
        description="Verify student identities through authorized mechanisms, execute deterministic cross-institution matching, inspect side-by-side conflict evidence, and record auditable resolution decisions."
        eyebrow="Component 2 — Identity Governance"
      />

      {/* Engine Notification Alert */}
      {engineNotification && (
        <Alert
          variant={engineNotification.type === 'success' ? 'success' : 'danger'}
          title={engineNotification.type === 'success' ? 'Matching Engine Execution Complete' : 'Execution Failed'}
        >
          {engineNotification.message}
        </Alert>
      )}

      {/* Navigation Tabs */}
      <div className="flex border-b border-line bg-surface-card rounded-t-lg px-2">
        <button
          onClick={() => setActiveTab('overview')}
          className={`flex items-center gap-2 py-3 px-4 border-b-2 font-medium text-sm transition-colors ${
            activeTab === 'overview'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-ink-600 hover:text-ink-900 hover:border-ink-300'
          }`}
        >
          <ShieldCheckIcon className="w-4 h-4" />
          Overview & Resolution Dashboard
        </button>
        <button
          onClick={() => setActiveTab('conflicts')}
          className={`flex items-center gap-2 py-3 px-4 border-b-2 font-medium text-sm transition-colors ${
            activeTab === 'conflicts'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-ink-600 hover:text-ink-900 hover:border-ink-300'
          }`}
        >
          <AlertTriangleIcon className="w-4 h-4" />
          Detected Conflicts & Matches
          {dashboard?.openConflictsCount ? (
            <span className="ml-1 px-1.5 py-0.5 text-xs bg-danger-soft text-danger font-semibold rounded-full">
              {dashboard.openConflictsCount}
            </span>
          ) : null}
        </button>
        <button
          onClick={() => setActiveTab('cases')}
          className={`flex items-center gap-2 py-3 px-4 border-b-2 font-medium text-sm transition-colors ${
            activeTab === 'cases'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-ink-600 hover:text-ink-900 hover:border-ink-300'
          }`}
        >
          <ScaleIcon className="w-4 h-4" />
          Investigation Cases Dossier
          {dashboard?.casesAwaitingReviewCount ? (
            <span className="ml-1 px-1.5 py-0.5 text-xs bg-warning-soft text-warning font-semibold rounded-full">
              {dashboard.casesAwaitingReviewCount}
            </span>
          ) : null}
        </button>
      </div>

      {/* TAB 1: OVERVIEW & DASHBOARD */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Action Header Card with Run Button */}
          <Card className="p-6 bg-gradient-to-r from-brand-50/50 to-surface-card border-brand-200">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold text-ink-900">Deterministic Identity Matching Engine</h3>
                <p className="text-sm text-ink-600 mt-1 max-w-2xl">
                  Executes exact identifier matching, active cross-institution enrolment comparison, historical sequential transition
                  analysis, and duplicate conflict suppression with deterministic SHA-256 fingerprinting.
                </p>
              </div>
              <div className="flex items-center gap-3">
                <Button
                  variant="primary"
                  onClick={handleRunMatchingEngine}
                  disabled={isRunningEngine}
                  className="shadow-sm"
                >
                  {isRunningEngine ? <LoadingSpinner size="sm" className="mr-2" /> : <RefreshIcon className="w-4 h-4 mr-2" />}
                  {isRunningEngine ? 'Executing Matching Run...' : 'Run Matching Engine'}
                </Button>
              </div>
            </div>
          </Card>

          {/* Metric Cards Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Card className="p-4 border-l-4 border-l-brand-600">
              <p className="text-xs font-medium text-ink-500 uppercase tracking-wider">Total Identities Assessed</p>
              <p className="text-2xl font-bold text-ink-900 mt-2">{dashboard?.totalIdentitiesAssessed ?? 0}</p>
              <p className="text-xs text-ink-500 mt-1">From Component 1 normalized imports</p>
            </Card>

            <Card className="p-4 border-l-4 border-l-success">
              <p className="text-xs font-medium text-ink-500 uppercase tracking-wider">Format Valid (12-digit)</p>
              <p className="text-2xl font-bold text-success mt-2">{dashboard?.formatValidCount ?? 0}</p>
              <p className="text-xs text-ink-500 mt-1">Passed 12-digit format check</p>
            </Card>

            <Card className="p-4 border-l-4 border-l-info">
              <p className="text-xs font-medium text-ink-500 uppercase tracking-wider">Simulated Verified</p>
              <p className="text-2xl font-bold text-info mt-2">{dashboard?.simulatedVerifiedCount ?? 0}</p>
              <p className="text-xs text-ink-500 mt-1">Verified via demo adapter</p>
            </Card>

            <Card className="p-4 border-l-4 border-l-danger">
              <p className="text-xs font-medium text-ink-500 uppercase tracking-wider">Open Conflicts</p>
              <p className="text-2xl font-bold text-danger mt-2">{dashboard?.openConflictsCount ?? 0}</p>
              <p className="text-xs text-ink-500 mt-1">Require officer review</p>
            </Card>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Feature 1: APAAR Format Validator Playground */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-3">
                <BookIcon className="w-5 h-5 text-brand-600" />
                <h4 className="font-semibold text-ink-900">APAAR Format Validation Tool</h4>
              </div>
              <p className="text-xs text-ink-600 mb-4">
                Validates that the APAAR is exactly 12 numeric digits. Notice:
                <strong className="text-ink-900"> FORMAT_VALID</strong> confirms only syntactic validity; it does not establish identity existence or ownership.
              </p>

              <div className="space-y-4">
                <div className="flex gap-2">
                  <Input
                    placeholder="Enter 12-digit APAAR (e.g. 900000000001)"
                    value={validatorApaarInput}
                    onChange={(e) => setValidatorApaarInput(e.target.value)}
                    className="font-mono"
                    maxLength={14}
                  />
                  <Button
                    variant="secondary"
                    onClick={handleTestApaarFormat}
                    disabled={validatingApaar || !validatorApaarInput.trim()}
                  >
                    {validatingApaar ? 'Checking...' : 'Check Format'}
                  </Button>
                </div>

                {validationOutcome && (
                  <div
                    className={`p-4 rounded-md border text-sm space-y-2 ${
                      validationOutcome.valid
                        ? 'bg-success-soft/30 border-success/30 text-success'
                        : 'bg-danger-soft/30 border-danger/30 text-danger'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-semibold">
                      {validationOutcome.valid ? <CheckCircleIcon className="w-5 h-5" /> : <XCircleIcon className="w-5 h-5" />}
                      Status: {validationOutcome.status} ({validationOutcome.reasonCode})
                    </div>
                    <p className="text-xs text-ink-700">{validationOutcome.message}</p>
                    <div className="text-xs text-ink-500 pt-1 border-t border-line">
                      Validated at: {new Date(validationOutcome.validatedAt).toLocaleTimeString()}
                    </div>
                  </div>
                )}
              </div>
            </Card>

            {/* Feature 2: Authoritative vs Simulated Verification Adapter */}
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-3">
                <ShieldCheckIcon className="w-5 h-5 text-brand-600" />
                <h4 className="font-semibold text-ink-900">APAAR Verification Adapter Playground</h4>
              </div>
              <p className="text-xs text-ink-600 mb-4">
                Test verification boundary integration. When configured in simulated mode, test outputs are labelled
                <span className="font-semibold text-warning"> SIMULATED</span>. In authoritative mode without live credentials,
                it safely reports technical status without fabricating verification.
              </p>

              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    placeholder="Student UUID (from registry)"
                    value={verificationStudentId}
                    onChange={(e) => setVerificationStudentId(e.target.value)}
                    className="font-mono text-xs"
                  />
                  <Select
                    value={verificationMode}
                    onChange={(e) => setVerificationMode(e.target.value as 'SIMULATED' | 'AUTHORIZED')}
                  >
                    <option value="SIMULATED">Mode: SIMULATED (Demo Adapter)</option>
                    <option value="AUTHORIZED">Mode: AUTHORIZED (Official Gateway)</option>
                  </Select>
                </div>

                <Button
                  variant="primary"
                  onClick={handleTestVerification}
                  disabled={verifyingStudent || !verificationStudentId.trim()}
                  className="w-full"
                >
                  {verifyingStudent ? 'Submitting Verification...' : 'Execute Adapter Verification'}
                </Button>

                {verificationOutcome && (
                  <div className="p-4 rounded-md border border-line bg-surface-muted text-xs space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-ink-900">Verification Outcome: {verificationOutcome.status}</span>
                      <StatusBadge
                        status={verificationOutcome.simulated ? 'SIMULATED' : 'AUTHORITATIVE'}
                        label={verificationOutcome.simulated ? 'SIMULATED ADAPTER' : 'AUTHORITATIVE GATEWAY'}
                        tone={verificationOutcome.simulated ? 'warning' : 'info'}
                      />
                    </div>
                    <p className="text-ink-600">Adapter: {verificationOutcome.adapterName} (Ref: {verificationOutcome.providerReference})</p>
                    {verificationOutcome.simulated && (
                      <p className="text-ink-500 italic">
                        * Note: Synthetic verification generated for local testing. Official government verification requires live credentials.
                      </p>
                    )}
                  </div>
                )}
              </div>
            </Card>
          </div>

          {/* Recent Matching Runs Table */}
          <Card className="p-6">
            <h4 className="font-semibold text-ink-900 mb-4">Recent Identity Matching Runs</h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border border-line">
                <thead className="bg-surface-muted text-ink-700 uppercase font-semibold">
                  <tr>
                    <th className="py-2.5 px-3 border-b border-line">Run ID</th>
                    <th className="py-2.5 px-3 border-b border-line">Run Type</th>
                    <th className="py-2.5 px-3 border-b border-line">Status</th>
                    <th className="py-2.5 px-3 border-b border-line">Scanned</th>
                    <th className="py-2.5 px-3 border-b border-line">Matches Found</th>
                    <th className="py-2.5 px-3 border-b border-line">Conflicts Created</th>
                    <th className="py-2.5 px-3 border-b border-line">Conflicts Suppressed</th>
                    <th className="py-2.5 px-3 border-b border-line">Started At</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line font-mono">
                  {matchingRuns?.content?.map((run) => (
                    <tr key={run.id} className="hover:bg-surface-muted/50">
                      <td className="py-2.5 px-3 text-brand-600 font-semibold">{run.id.substring(0, 8)}...</td>
                      <td className="py-2.5 px-3">{run.runType}</td>
                      <td className="py-2.5 px-3">
                        <span className="px-2 py-0.5 rounded text-success font-semibold bg-success-soft">
                          {run.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3">{run.recordsScanned}</td>
                      <td className="py-2.5 px-3">{run.matchesFound}</td>
                      <td className="py-2.5 px-3 text-danger font-semibold">{run.conflictsGenerated}</td>
                      <td className="py-2.5 px-3 text-info font-semibold">{run.conflictsSuppressed}</td>
                      <td className="py-2.5 px-3 text-ink-500 font-sans">
                        {new Date(run.startedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                  {(!matchingRuns?.content || matchingRuns.content.length === 0) && (
                    <tr>
                      <td colSpan={8} className="py-4 text-center text-ink-500 font-sans">
                        No matching runs executed yet. Click &quot;Run Matching Engine&quot; above to scan student records.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      )}

      {/* TAB 2: DETECTED CONFLICTS & MATCHES */}
      {activeTab === 'conflicts' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex gap-2">
              {(['OPEN', 'INVESTIGATING', 'RESOLVED', ''] as const).map((status) => (
                <button
                  key={status}
                  onClick={() => {
                    setConflictStatus(status);
                    setConflictPage(0);
                  }}
                  className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                    conflictStatus === status
                      ? 'bg-brand-600 text-white shadow-sm'
                      : 'bg-surface-card border border-line text-ink-700 hover:bg-surface-muted'
                  }`}
                >
                  {status ? status : 'ALL CONFLICTS'}
                </button>
              ))}
            </div>

            <Button size="sm" variant="secondary" onClick={reloadConflicts}>
              <RefreshIcon className="w-3.5 h-3.5 mr-1" />
              Refresh
            </Button>
          </div>

          <DataTable
            columns={conflictColumns}
            rows={conflictsData?.content ?? []}
            getRowId={(row) => row.id}
            caption="List of detected identity conflicts"
            loading={loadingConflicts}
            emptyTitle="No conflicts found"
            emptyDescription="No identity discrepancies or active enrolment conflicts match the current filter."
          />

          {conflictsData && conflictsData.totalPages > 1 && (
            <Pagination
              page={conflictsData.page}
              totalPages={conflictsData.totalPages}
              totalElements={conflictsData.totalElements}
              size={conflictsData.size}
              onPageChange={setConflictPage}
            />
          )}
        </div>
      )}

      {/* TAB 3: INVESTIGATION CASES DOSSIER */}
      {activeTab === 'cases' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex gap-2">
              {(['', 'OPEN', 'IN_REVIEW', 'RESOLVED'] as const).map((status) => (
                <button
                  key={status}
                  onClick={() => {
                    setCaseStatus(status);
                    setCasePage(0);
                  }}
                  className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                    caseStatus === status
                      ? 'bg-brand-600 text-white shadow-sm'
                      : 'bg-surface-card border border-line text-ink-700 hover:bg-surface-muted'
                  }`}
                >
                  {status ? status : 'ALL CASES'}
                </button>
              ))}
            </div>

            <Button size="sm" variant="secondary" onClick={reloadCases}>
              <RefreshIcon className="w-3.5 h-3.5 mr-1" />
              Refresh
            </Button>
          </div>

          {casesError ? (
            <ErrorState title="Failed to load investigation cases" error={casesError} onRetry={reloadCases} />
          ) : (
            <div className="space-y-4">
              <DataTable
                columns={caseColumns}
                rows={casesData?.content ?? []}
                getRowId={(row) => row.id}
                caption="List of formal investigation cases"
                loading={loadingCases}
                emptyTitle="No investigation cases"
                emptyDescription="Discrepancies promoted to formal investigation will appear here."
              />

              {casesData && casesData.totalPages > 1 && (
                <Pagination
                  page={casesData.page}
                  totalPages={casesData.totalPages}
                  totalElements={casesData.totalElements}
                  size={casesData.size}
                  onPageChange={setCasePage}
                />
              )}
            </div>
          )}
        </div>
      )}

      {/* MODAL 1: SIDE-BY-SIDE EVIDENCE COMPARISON DOSSIER */}
      {selectedConflictId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-surface-card rounded-lg shadow-raised border border-line max-w-4xl w-full max-h-[90vh] overflow-y-auto p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-line pb-4">
              <div>
                <h3 className="text-lg font-bold text-ink-900 flex items-center gap-2">
                  <AlertTriangleIcon className="w-5 h-5 text-warning" />
                  Side-by-Side Conflict Evidence Comparison
                </h3>
                <p className="text-xs text-ink-600 mt-1">
                  Examine attribute divergence and matching evidence between institutional submissions.
                </p>
              </div>
              <button
                onClick={() => {
                  setSelectedConflictId(null);
                  setConflictEvidence(null);
                }}
                className="text-ink-400 hover:text-ink-700 p-1 rounded"
              >
                ✕
              </button>
            </div>

            {loadingEvidence || !conflictEvidence ? (
              <div className="py-12 flex justify-center">
                <LoadingSpinner size="lg" />
              </div>
            ) : (
              <div className="space-y-6">
                {/* Explanation Banner */}
                <div className="p-4 rounded-md bg-brand-50 border border-brand-200 text-sm text-brand-900 flex items-start gap-3">
                  <InfoIcon className="w-5 h-5 text-brand-600 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold">Matching Engine Finding & Scope</p>
                    <p className="text-xs text-brand-800 mt-1">{conflictEvidence.explanation}</p>
                  </div>
                </div>

                {/* Side-by-Side Cards Comparison */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Primary Record */}
                  <Card className="p-4 border-t-4 border-t-brand-600">
                    <h5 className="font-semibold text-ink-900 border-b border-line pb-2 mb-3 flex items-center gap-2">
                      <UsersIcon className="w-4 h-4 text-brand-600" />
                      Primary Submission (Record A)
                    </h5>
                    <dl className="space-y-2 text-xs">
                      <div>
                        <dt className="text-ink-500 font-medium">Institution</dt>
                        <dd className="font-semibold text-ink-900">
                          {String(conflictEvidence.primaryRecord?.institutionName || 'Unknown Institution')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">Student Full Name</dt>
                        <dd
                          className={`font-semibold px-1 py-0.5 rounded ${
                            conflictEvidence.conflictingFields.includes('fullName')
                              ? 'bg-danger-soft text-danger'
                              : 'text-ink-900'
                          }`}
                        >
                          {String(conflictEvidence.primaryRecord?.fullName || 'N/A')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">Date of Birth</dt>
                        <dd
                          className={`px-1 py-0.5 rounded ${
                            conflictEvidence.conflictingFields.includes('dateOfBirth')
                              ? 'bg-danger-soft text-danger font-semibold'
                              : 'text-ink-800'
                          }`}
                        >
                          {String(conflictEvidence.primaryRecord?.dateOfBirth || 'N/A')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">APAAR Identifier</dt>
                        <dd className="font-mono font-bold text-brand-600">
                          {String(conflictEvidence.primaryRecord?.apaarId || 'Not Supplied')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">Enrolment Number & Status</dt>
                        <dd className="font-semibold text-ink-800">
                          {String(conflictEvidence.primaryRecord?.enrolmentNumber || 'N/A')} (
                          {String(conflictEvidence.primaryRecord?.status || 'ACTIVE')})
                        </dd>
                      </div>
                    </dl>
                  </Card>

                  {/* Secondary Record */}
                  <Card className="p-4 border-t-4 border-t-accent-600">
                    <h5 className="font-semibold text-ink-900 border-b border-line pb-2 mb-3 flex items-center gap-2">
                      <UsersIcon className="w-4 h-4 text-accent-600" />
                      Secondary Submission (Record B)
                    </h5>
                    <dl className="space-y-2 text-xs">
                      <div>
                        <dt className="text-ink-500 font-medium">Institution</dt>
                        <dd className="font-semibold text-ink-900">
                          {String(conflictEvidence.secondaryRecord?.institutionName || 'Unknown Institution')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">Student Full Name</dt>
                        <dd
                          className={`font-semibold px-1 py-0.5 rounded ${
                            conflictEvidence.conflictingFields.includes('fullName')
                              ? 'bg-danger-soft text-danger'
                              : 'text-ink-900'
                          }`}
                        >
                          {String(conflictEvidence.secondaryRecord?.fullName || 'N/A')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">Date of Birth</dt>
                        <dd
                          className={`px-1 py-0.5 rounded ${
                            conflictEvidence.conflictingFields.includes('dateOfBirth')
                              ? 'bg-danger-soft text-danger font-semibold'
                              : 'text-ink-800'
                          }`}
                        >
                          {String(conflictEvidence.secondaryRecord?.dateOfBirth || 'N/A')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">APAAR Identifier</dt>
                        <dd className="font-mono font-bold text-accent-600">
                          {String(conflictEvidence.secondaryRecord?.apaarId || 'Not Supplied')}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-ink-500 font-medium">Enrolment Number & Status</dt>
                        <dd className="font-semibold text-ink-800">
                          {String(conflictEvidence.secondaryRecord?.enrolmentNumber || 'N/A')} (
                          {String(conflictEvidence.secondaryRecord?.status || 'ACTIVE')})
                        </dd>
                      </div>
                    </dl>
                  </Card>
                </div>

                {/* Verification History Log */}
                {conflictEvidence.verificationHistory && conflictEvidence.verificationHistory.length > 0 && (
                  <div className="border border-line rounded-md p-4">
                    <h6 className="font-semibold text-xs text-ink-900 mb-2">Attached Verification Attempts History</h6>
                    <div className="space-y-1.5 text-xs font-mono">
                      {conflictEvidence.verificationHistory.map((vr) => (
                        <div key={vr.id} className="flex items-center justify-between p-2 bg-surface-muted rounded">
                          <div>
                            <span className="font-semibold text-ink-800">{vr.studentName}: </span>
                            <span className="text-ink-600">{vr.status} ({vr.reasonCode})</span>
                          </div>
                          <StatusBadge
                            status={vr.simulated ? 'SIMULATED' : 'AUTHORITATIVE'}
                            label={vr.simulated ? 'SIMULATED' : 'AUTHORITATIVE'}
                            tone={vr.simulated ? 'warning' : 'success'}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Modal Action Bar */}
                <div className="flex items-center justify-end gap-3 pt-4 border-t border-line">
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setSelectedConflictId(null);
                      setConflictEvidence(null);
                    }}
                  >
                    Close
                  </Button>
                  <Button
                    variant="primary"
                    onClick={() => handlePromoteToCase(selectedConflictId)}
                  >
                    <ScaleIcon className="w-4 h-4 mr-1.5" />
                    Open Formal Investigation Case
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL 2: INVESTIGATION CASE DOSSIER & RESOLUTION DECISION */}
      {selectedCaseId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-surface-card rounded-lg shadow-raised border border-line max-w-4xl w-full max-h-[92vh] overflow-y-auto p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-line pb-4">
              <div>
                <h3 className="text-lg font-bold text-ink-900 flex items-center gap-2">
                  <ScaleIcon className="w-5 h-5 text-brand-600" />
                  Investigation Case Dossier: {selectedCaseDetail?.caseNumber ?? 'Loading...'}
                </h3>
                <p className="text-xs text-ink-600 mt-1">
                  Full evidentiary record, officer comments, and authorized resolution decisions.
                </p>
              </div>
              <button
                onClick={() => {
                  setSelectedCaseId(null);
                  setSelectedCaseDetail(null);
                }}
                className="text-ink-400 hover:text-ink-700 p-1 rounded"
              >
                ✕
              </button>
            </div>

            {loadingCaseDetail || !selectedCaseDetail ? (
              <div className="py-12 flex justify-center">
                <LoadingSpinner size="lg" />
              </div>
            ) : (
              <div className="space-y-6">
                {/* Case Header Details */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-surface-muted rounded-md text-xs">
                  <div>
                    <span className="text-ink-500 font-medium">Status</span>
                    <p className="mt-1">
                      <StatusBadge
                        status={selectedCaseDetail.status}
                        label={selectedCaseDetail.status}
                        tone={selectedCaseDetail.status === 'RESOLVED' ? 'success' : 'warning'}
                      />
                    </p>
                  </div>
                  <div>
                    <span className="text-ink-500 font-medium">Priority</span>
                    <p className="mt-1 font-semibold text-ink-900">{selectedCaseDetail.priority}</p>
                  </div>
                  <div>
                    <span className="text-ink-500 font-medium">Institution Scope</span>
                    <p className="mt-1 font-medium text-ink-800">
                      {selectedCaseDetail.institutionName ?? 'Platform Wide'}
                    </p>
                  </div>
                  <div>
                    <span className="text-ink-500 font-medium">Assigned Officer</span>
                    <p className="mt-1 font-medium text-ink-800">
                      {selectedCaseDetail.assignedTo ?? 'Unassigned'}
                    </p>
                  </div>
                </div>

                {/* Evidence Artifacts List */}
                <div className="space-y-2">
                  <h5 className="font-semibold text-sm text-ink-900">Evidentiary Artifacts ({selectedCaseDetail.evidence?.length ?? 0})</h5>
                  <div className="border border-line rounded-md divide-y divide-line">
                    {selectedCaseDetail.evidence?.map((ev) => (
                      <div key={ev.id} className="p-3 text-xs flex items-center justify-between">
                        <div>
                          <span className="font-semibold text-ink-900">{ev.type}: </span>
                          <span className="text-ink-600">{ev.description}</span>
                        </div>
                        {ev.fileRef && (
                          <span className="font-mono text-xs text-brand-600 bg-brand-50 px-2 py-0.5 rounded">
                            {ev.fileRef}
                          </span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Decisions Log */}
                {selectedCaseDetail.decisions && selectedCaseDetail.decisions.length > 0 && (
                  <div className="space-y-2">
                    <h5 className="font-semibold text-sm text-ink-900">Recorded Authorized Decisions</h5>
                    <div className="border border-line rounded-md divide-y divide-line bg-success-soft/10">
                      {selectedCaseDetail.decisions.map((dec) => (
                        <div key={dec.id} className="p-3 text-xs space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-success text-sm">{dec.decision}</span>
                            <span className="text-ink-500">By {dec.decidedBy}</span>
                          </div>
                          <p className="text-ink-800 font-medium">{dec.reason}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Record Resolution Decision Section */}
                {selectedCaseDetail.status !== 'RESOLVED' && (
                  <Card className="p-4 border-2 border-brand-200 bg-brand-50/20 space-y-4">
                    <h5 className="font-semibold text-sm text-ink-900 flex items-center gap-2">
                      <CheckCircleIcon className="w-4 h-4 text-brand-600" />
                      Record Authorized Decision & Resolution
                    </h5>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-medium text-ink-700 mb-1">Decision Code</label>
                        <Select
                          value={decisionCode}
                          onChange={(e) => setDecisionCode(e.target.value)}
                        >
                          <option value="IDENTITY_CONFIRMED">IDENTITY_CONFIRMED: Identity verified legitimate</option>
                          <option value="DUPLICATE_RECORD_CONFIRMED">DUPLICATE_RECORD_CONFIRMED: Confirmed duplicate submission</option>
                          <option value="ENROLMENT_CONFLICT_CONFIRMED">ENROLMENT_CONFLICT_CONFIRMED: Ineligible dual enrolment</option>
                          <option value="DATA_ENTRY_ERROR">DATA_ENTRY_ERROR: Typo or administrative error in student record</option>
                          <option value="LEGITIMATE_HISTORICAL_ENROLMENT">LEGITIMATE_HISTORICAL_ENROLMENT: Valid sequential transfer</option>
                          <option value="TRANSFER_OR_STATUS_CHANGE_CONFIRMED">TRANSFER_OR_STATUS_CHANGE_CONFIRMED: Formal transfer</option>
                          <option value="NO_ACTION_REQUIRED">NO_ACTION_REQUIRED: False alarm or compliant record</option>
                          <option value="INSUFFICIENT_EVIDENCE">INSUFFICIENT_EVIDENCE: Discrepancy cannot be substantiated</option>
                        </Select>
                      </div>

                      <div>
                        <label className="block text-xs font-medium text-ink-700 mb-1">Effective Action Code</label>
                        <Input
                          placeholder="e.g. RECORD_CORRECTED, MERGE_IDENTITY"
                          value={effectiveAction}
                          onChange={(e) => setEffectiveAction(e.target.value)}
                          className="text-xs"
                        />
                      </div>
                    </div>

                    {decisionCode === 'DATA_ENTRY_ERROR' && (
                      <div className="p-3 bg-surface-card border border-warning/30 rounded text-xs space-y-2">
                        <p className="font-semibold text-warning">Apply Record Correction (Increments Student Record Version):</p>
                        <div className="grid grid-cols-2 gap-3">
                          <Input
                            placeholder="Corrected Full Name"
                            value={correctedName}
                            onChange={(e) => setCorrectedName(e.target.value)}
                            className="text-xs"
                          />
                          <Input
                            placeholder="Corrected DOB (YYYY-MM-DD)"
                            value={correctedDob}
                            onChange={(e) => setCorrectedDob(e.target.value)}
                            className="text-xs"
                          />
                        </div>
                      </div>
                    )}

                    <div>
                      <label className="block text-xs font-medium text-ink-700 mb-1">
                        Rationale & Legal / Administrative Justification <span className="text-danger">*</span>
                      </label>
                      <Textarea
                        placeholder="Provide detailed justification and document citations for this authorized resolution..."
                        rows={3}
                        value={decisionRationale}
                        onChange={(e) => setDecisionRationale(e.target.value)}
                        className="text-xs"
                      />
                    </div>

                    <div className="flex justify-end">
                      <Button
                        variant="primary"
                        onClick={handleRecordDecision}
                        disabled={submittingDecision || !decisionRationale.trim()}
                      >
                        {submittingDecision ? 'Submitting Decision...' : 'Record Decision & Resolve Case'}
                      </Button>
                    </div>
                  </Card>
                )}

                {/* Case Comments Section */}
                <div className="space-y-3">
                  <h5 className="font-semibold text-sm text-ink-900">Case Dossier Notes & Comments</h5>
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {selectedCaseDetail.comments?.map((cm) => (
                      <div key={cm.id} className="p-2.5 rounded bg-surface-muted text-xs space-y-1">
                        <div className="flex items-center justify-between text-ink-500 font-medium">
                          <span>{cm.authorName} ({cm.authorEmail})</span>
                          <span>{cm.createdAt ? new Date(cm.createdAt).toLocaleString() : ''}</span>
                        </div>
                        <p className="text-ink-800">{cm.body}</p>
                      </div>
                    ))}
                  </div>

                  <div className="flex gap-2">
                    <Input
                      placeholder="Add investigation comment..."
                      value={newComment}
                      onChange={(e) => setNewComment(e.target.value)}
                      className="text-xs"
                    />
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={handleAddComment}
                      disabled={submittingComment || !newComment.trim()}
                    >
                      Add Note
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
