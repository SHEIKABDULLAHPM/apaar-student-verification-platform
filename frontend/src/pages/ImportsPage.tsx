import { useState, useEffect } from 'react';
import { useApiQuery } from '@/hooks/useApiQuery';
import {
  listImports,
  uploadImport,
  applyMappings,
  getImportPreview,
  commitImport,
  getReconciliation,
  downloadErrorReport,
} from '@/api/imports';
import { listAcademicYears, listProgrammes } from '@/api/academics';
import { normalizeError } from '@/lib/errors';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { Pagination } from '@/components/ui/Pagination';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { FormField } from '@/components/ui/FormField';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  UploadIcon,
  DownloadIcon,
  RefreshIcon,
  CheckCircleIcon,
  AlertTriangleIcon,
  ChevronRightIcon,
  CloseIcon,
} from '@/components/icons';
import type {
  UploadResponseDto,
  ImportPreviewDto,
  NormalizedRowDto,
  ReconciliationSummaryDto,
  AcademicYear,
  Programme,
} from '@/api/types';

const CANONICAL_FIELDS: { key: string; label: string; mandatory?: boolean }[] = [
  { key: 'studentIdentifier', label: 'Student Identifier / Enrolment No', mandatory: true },
  { key: 'fullName', label: 'Full Name', mandatory: true },
  { key: 'apaarId', label: 'APAAR ID (12 digits)' },
  { key: 'dateOfBirth', label: 'Date of Birth (YYYY-MM-DD or DD/MM/YYYY)' },
  { key: 'gender', label: 'Gender (Male / Female / Other)' },
  { key: 'admissionCategory', label: 'Admission Category (OPEN, OBC, SC, ST, EWS)', mandatory: true },
  { key: 'admissionDate', label: 'Admission Date' },
  { key: 'programmeCode', label: 'Programme Code' },
  { key: 'email', label: 'Student Email' },
  { key: 'phone', label: 'Mobile / Phone' },
  { key: 'guardianName', label: 'Guardian / Parent Name' },
  { key: 'addressLine', label: 'Address Line' },
  { key: 'state', label: 'State / Domicile' },
  { key: 'quotaType', label: 'Quota Type' },
  { key: 'supernumerary', label: 'Supernumerary (Yes/No)' },
  { key: 'nriClassification', label: 'NRI Status (Yes/No)' },
  { key: 'ociClassification', label: 'OCI Status (Yes/No)' },
  { key: 'foreignNationalClassification', label: 'Foreign National (Yes/No)' },
];

export function ImportsPage() {
  const [activeTab, setActiveTab] = useState<'wizard' | 'history'>('wizard');
  const [wizardStep, setWizardStep] = useState<1 | 2 | 3 | 4>(1);

  // Context Selection
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [programmes, setProgrammes] = useState<Programme[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<string>('');
  const [selectedProgId, setSelectedProgId] = useState<string>('');

  // Upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadData, setUploadData] = useState<UploadResponseDto | null>(null);

  // Mappings state
  const [mappings, setMappings] = useState<Record<string, string>>({});
  const [isValidating, setIsValidating] = useState(false);

  // Preview state
  const [currentJobId, setCurrentJobId] = useState<string | null>(null);
  const [previewData, setPreviewData] = useState<ImportPreviewDto | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Reconciliation state
  const [reconData, setReconData] = useState<ReconciliationSummaryDto | null>(null);
  const [isLoadingRecon, setIsLoadingRecon] = useState(false);

  // Commit state
  const [isCommitDialogOpen, setIsCommitDialogOpen] = useState(false);
  const [isCommitting, setIsCommitting] = useState(false);
  const [commitSuccess, setCommitSuccess] = useState<string | null>(null);

  const isAlreadyCommitted =
    previewData?.job.status === 'COMPLETED' ||
    previewData?.job.status === 'COMPLETED_WITH_ERRORS';

  // Resubmission state
  const [resubmitParentId, setResubmitParentId] = useState<string | null>(null);

  // History query
  const [historyPage, setHistoryPage] = useState(0);
  const historySize = 10;
  const { data: historyData, isLoading: isHistoryLoading, reload: reloadHistory } = useApiQuery(
    () => listImports({ page: historyPage, size: historySize }),
    [historyPage, historySize]
  );

  // Load academic years and programmes on mount
  useEffect(() => {
    listAcademicYears().then((years) => {
      setAcademicYears(years);
      const active = years.find((y) => y.active) ?? years[0];
      if (active) setSelectedYearId(active.id);
    });
    listProgrammes().then((res) => {
      setProgrammes(res.content);
    });
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setUploadError(null);
    }
  };

  const handleStartUpload = async () => {
    if (!selectedFile) {
      setUploadError('Please select a file to upload');
      return;
    }
    if (!selectedYearId) {
      setUploadError('Please select an Academic Year');
      return;
    }

    setIsUploading(true);
    setUploadError(null);
    try {
      const res = await uploadImport({
        file: selectedFile,
        academicYearId: selectedYearId,
        programmeId: selectedProgId || undefined,
        parentImportId: resubmitParentId || undefined,
      });
      setUploadData(res);
      setCurrentJobId(res.importId);
      setMappings(res.suggestedMappings || {});
      setWizardStep(2);
    } catch (err) {
      setUploadError(normalizeError(err).message || 'Failed to upload and parse file');
    } finally {
      setIsUploading(false);
    }
  };

  const handleValidateMappings = async () => {
    if (!currentJobId) return;
    setIsValidating(true);
    setUploadError(null);
    try {
      await applyMappings(currentJobId, mappings);
      // Fetch preview
      const preview = await getImportPreview(currentJobId, undefined, 0, 50);
      setPreviewData(preview);
      setWizardStep(3);
    } catch (err) {
      setUploadError(normalizeError(err).message || 'Validation failed. Please verify field mappings.');
    } finally {
      setIsValidating(false);
    }
  };

  const handleProceedToReconciliation = async () => {
    if (!currentJobId) return;
    setIsLoadingRecon(true);
    setUploadError(null);
    try {
      const recon = await getReconciliation(currentJobId);
      setReconData(recon);
      setWizardStep(4);
    } catch (err) {
      setUploadError(normalizeError(err).message || 'Failed to load reconciliation comparison');
    } finally {
      setIsLoadingRecon(false);
    }
  };

  const handleConfirmCommit = async () => {
    if (!currentJobId) return;
    setIsCommitting(true);
    setUploadError(null);
    try {
      const res = await commitImport(currentJobId);
      setCommitSuccess(
        `Successfully committed ${res.totalCommitted} student enrolment records (${res.insertedRows} newly enrolled, ${res.updatedRows} updated).`
      );
      if (previewData) {
        setPreviewData({
          ...previewData,
          job: {
            ...previewData.job,
            status: res.status,
          },
        });
      }
      setIsCommitDialogOpen(false);
      reloadHistory();
    } catch (err) {
      setUploadError(normalizeError(err).message || 'Failed to commit import records');
    } finally {
      setIsCommitting(false);
    }
  };

  const handleDownloadErrors = async (format: 'csv' | 'xlsx') => {
    if (!currentJobId) return;
    try {
      const blob = await downloadErrorReport(currentJobId, format);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `import_errors_${currentJobId.slice(0, 8)}.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      alert(`Failed to download error report: ${normalizeError(err).message}`);
    }
  };

  const handleInspectJob = async (jobId: string) => {
    setCurrentJobId(jobId);
    try {
      const preview = await getImportPreview(jobId, undefined, 0, 50);
      setPreviewData(preview);
      const recon = await getReconciliation(jobId);
      setReconData(recon);
      setActiveTab('wizard');
      setWizardStep(3);
    } catch (err) {
      alert(`Failed to load job details: ${normalizeError(err).message}`);
    }
  };

  const handleTriggerResubmit = (jobId: string) => {
    setResubmitParentId(jobId);
    setCommitSuccess(null);
    setSelectedFile(null);
    setUploadData(null);
    setActiveTab('wizard');
    setWizardStep(1);
  };

  // Preview Columns
  const previewColumns: DataTableColumn<NormalizedRowDto>[] = [
    {
      key: 'rowNumber',
      header: 'Row',
      render: (row) => <span className="font-mono text-xs font-semibold text-ink-600">#{row.rowNumber}</span>,
    },
    {
      key: 'identifier',
      header: 'Student ID',
      render: (row) => (
        <span className="font-mono text-xs font-medium text-ink-900">
          {(row.payload?.studentIdentifier as string) || (row.rawData?.['Student Identifier'] as string) || '—'}
        </span>
      ),
    },
    {
      key: 'fullName',
      header: 'Student Name',
      render: (row) => (
        <span className="font-medium text-ink-900">
          {(row.payload?.fullName as string) || (row.rawData?.['Full Name'] as string) || '—'}
        </span>
      ),
    },
    {
      key: 'apaar',
      header: 'APAAR ID (Format Check)',
      render: (row) => {
        const apaar = row.payload?.apaarId as string;
        const valid = row.payload?.apaarFormatValid as boolean;

        if (!apaar) {
          return <span className="text-xs text-ink-400 italic">Missing</span>;
        }
        return (
          <div className="flex items-center gap-1.5">
            <span className="font-mono text-xs">{apaar}</span>
            <span
              className={`text-[10px] px-1.5 py-0.5 rounded font-medium ${
                valid ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'
              }`}
            >
              {valid ? 'FORMAT VALID' : 'FORMAT INVALID'}
            </span>
          </div>
        );
      },
    },
    {
      key: 'category',
      header: 'Category / Quota',
      render: (row) => (
        <span className="text-xs">
          {(row.payload?.admissionCategory as string) || '—'}
          {row.payload?.quotaType ? ` (${row.payload.quotaType})` : ''}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status & Errors',
      render: (row) => {
        const tone =
          row.rowStatus === 'VALID'
            ? 'success'
            : row.rowStatus === 'DUPLICATE'
              ? 'warning'
              : row.rowStatus === 'REVIEW_REQUIRED'
                ? 'info'
                : 'danger';

        return (
          <div className="space-y-1">
            <StatusBadge status={row.rowStatus} label={row.rowStatus} tone={tone} />
            {row.errors.length > 0 && (
              <div className="space-y-0.5">
                {row.errors.slice(0, 2).map((err, i) => (
                  <p key={i} className="text-[11px] text-danger leading-tight">
                    • <strong className="font-mono">{err.errorCode}:</strong> {err.message}
                  </p>
                ))}
                {row.errors.length > 2 && (
                  <p className="text-[10px] text-ink-500 italic">+{row.errors.length - 2} more errors</p>
                )}
              </div>
            )}
          </div>
        );
      },
    },
  ];

  // Filter preview rows
  const filteredPreviewRows = previewData?.rows.filter((r) => {
    if (statusFilter === 'ALL') return true;
    return r.rowStatus === statusFilter;
  }) ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Unified Student Data Integration"
        description="Ingest, map, validate, reconcile, and commit student admissions & enrolments with APAAR format verification and formula injection protection."
        eyebrow="AICTE Student Data Governance"
        actions={
          <div className="flex gap-2">
            <Button
              variant={activeTab === 'wizard' ? 'primary' : 'secondary'}
              onClick={() => setActiveTab('wizard')}
            >
              <UploadIcon className="h-4 w-4 mr-1.5" />
              Upload Wizard
            </Button>
            <Button
              variant={activeTab === 'history' ? 'primary' : 'secondary'}
              onClick={() => setActiveTab('history')}
            >
              Import History
            </Button>
          </div>
        }
      />

      {commitSuccess && (
        <div className="p-4 bg-success/10 border border-success/30 rounded-lg flex items-center justify-between">
          <div className="flex items-center gap-3 text-success">
            <CheckCircleIcon className="h-5 w-5 flex-shrink-0" />
            <p className="font-medium text-sm">{commitSuccess}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={() => setCommitSuccess(null)}>
            <CloseIcon className="h-4 w-4" />
          </Button>
        </div>
      )}

      {uploadError && (
        <div className="p-4 bg-danger/10 border border-danger/30 rounded-lg flex items-center justify-between">
          <div className="flex items-center gap-3 text-danger">
            <AlertTriangleIcon className="h-5 w-5 flex-shrink-0" />
            <p className="font-medium text-sm">{uploadError}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={() => setUploadError(null)}>
            <CloseIcon className="h-4 w-4" />
          </Button>
        </div>
      )}

      {activeTab === 'wizard' && (
        <div className="space-y-6">
          {/* Step Progress Bar */}
          <div className="bg-surface-card border border-line rounded-xl p-4 shadow-sm flex items-center justify-between">
            {[
              { num: 1, label: 'Context & File Upload' },
              { num: 2, label: 'Column Mapping' },
              { num: 3, label: 'Validate & Preview' },
              { num: 4, label: 'Reconciliation & Commit' },
            ].map((step, idx) => {
              const active = wizardStep === step.num;
              const completed = wizardStep > step.num;
              return (
                <div key={step.num} className="flex items-center gap-2">
                  <div
                    className={`h-8 w-8 rounded-full flex items-center justify-center font-semibold text-xs transition-colors ${
                      completed
                        ? 'bg-success text-white'
                        : active
                          ? 'bg-ink-900 text-white'
                          : 'bg-surface-muted text-ink-500'
                    }`}
                  >
                    {completed ? '✓' : step.num}
                  </div>
                  <span
                    className={`text-xs font-medium hidden sm:inline ${
                      active ? 'text-ink-900 font-bold' : completed ? 'text-success' : 'text-ink-500'
                    }`}
                  >
                    {step.label}
                  </span>
                  {idx < 3 && <ChevronRightIcon className="h-4 w-4 text-ink-300 ml-2 hidden sm:inline" />}
                </div>
              );
            })}
          </div>

          {/* STEP 1: UPLOAD & CONTEXT */}
          {wizardStep === 1 && (
            <Card className="p-6 space-y-6">
              <div>
                <h3 className="text-lg font-bold text-ink-900">Step 1: Select Academic Context & File</h3>
                <p className="text-xs text-ink-500">
                  Select the target Academic Year and Programme, then upload your institution student data batch (.csv or .xlsx).
                </p>
                {resubmitParentId && (
                  <div className="mt-2 text-xs bg-amber-50 text-amber-800 p-2 rounded border border-amber-200">
                    Resubmitting corrected batch linked to Parent Import:{' '}
                    <strong className="font-mono">{resubmitParentId}</strong>
                  </div>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField id="select-academic-year" label="Academic Year" required>
                  <Select
                    value={selectedYearId}
                    onChange={(e) => setSelectedYearId(e.target.value)}
                  >
                    {academicYears.map((y) => (
                      <option key={y.id} value={y.id}>
                        {y.code} {y.active ? '(Active)' : ''}
                      </option>
                    ))}
                  </Select>
                </FormField>

                <FormField id="select-programme" label="Target Programme (Optional)">
                  <Select
                    value={selectedProgId}
                    onChange={(e) => setSelectedProgId(e.target.value)}
                  >
                    <option value="">All Programmes in File / Institution Level</option>
                    {programmes.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.code} - {p.name}
                      </option>
                    ))}
                  </Select>
                </FormField>
              </div>

              {/* Drag and Drop Zone */}
              <div className="border-2 border-dashed border-line rounded-xl p-8 text-center bg-surface-muted/50 hover:bg-surface-muted transition-colors">
                <UploadIcon className="h-10 w-10 text-ink-400 mx-auto mb-3" />
                <p className="text-sm font-medium text-ink-900 mb-1">
                  Choose a file or drag and drop your spreadsheet here
                </p>
                <p className="text-xs text-ink-500 mb-4">
                  Supports CSV (.csv) and Microsoft Excel (.xlsx, .xls) up to 50 MB
                </p>

                <input
                  type="file"
                  id="student-file-upload"
                  className="hidden"
                  accept=".csv,.xlsx,.xls"
                  onChange={handleFileChange}
                />
                <label
                  htmlFor="student-file-upload"
                  className="inline-flex items-center px-4 py-2 border border-line text-xs font-semibold rounded-lg bg-surface-card text-ink-800 hover:bg-surface-muted cursor-pointer shadow-sm transition-colors"
                >
                  Browse Computer
                </label>

                {selectedFile && (
                  <div className="mt-4 inline-flex items-center gap-2 px-3 py-1.5 bg-ink-900/5 text-ink-900 rounded-md font-mono text-xs">
                    <span>{selectedFile.name}</span>
                    <span className="text-ink-400">({(selectedFile.size / 1024).toFixed(1)} KB)</span>
                  </div>
                )}
              </div>

              <div className="flex justify-between items-center pt-2">
                <a
                  href="/database/synthetic-demo-data/initial_student_intake_valid.csv"
                  download="sample_student_template.csv"
                  className="text-xs text-brand-700 hover:underline inline-flex items-center gap-1"
                >
                  <DownloadIcon className="h-3.5 w-3.5" />
                  Download Sample CSV Template
                </a>

                <Button
                  variant="primary"
                  disabled={!selectedFile || isUploading}
                  onClick={handleStartUpload}
                >
                  {isUploading ? (
                    <>
                      <RefreshIcon className="h-4 w-4 mr-2 animate-spin" />
                      Parsing Spreadsheet...
                    </>
                  ) : (
                    <>
                      Upload & Auto-Map Columns
                      <ChevronRightIcon className="h-4 w-4 ml-1.5" />
                    </>
                  )}
                </Button>
              </div>
            </Card>
          )}

          {/* STEP 2: COLUMN MAPPING */}
          {wizardStep === 2 && uploadData && (
            <Card className="p-6 space-y-6">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="text-lg font-bold text-ink-900">Step 2: Column Mapping & Standardization</h3>
                  <p className="text-xs text-ink-500">
                    Verify how file column headers map to platform canonical attributes. Auto-detected mappings are pre-selected.
                  </p>
                </div>
                <div className="text-right">
                  <span className="font-mono text-xs font-semibold px-2 py-1 bg-surface-muted rounded">
                    {uploadData.totalRowsDetected} records detected
                  </span>
                </div>
              </div>

              {uploadData.missingMandatoryFields.length > 0 && (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800 flex items-center gap-2">
                  <AlertTriangleIcon className="h-4 w-4 text-amber-600 flex-shrink-0" />
                  <span>
                    Warning: Mandatory canonical fields not yet mapped:{' '}
                    <strong>{uploadData.missingMandatoryFields.join(', ')}</strong>. Please assign corresponding columns below.
                  </span>
                </div>
              )}

              <div className="border border-line rounded-lg overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-surface-muted text-ink-700 font-semibold uppercase tracking-wider text-[11px]">
                    <tr>
                      <th className="py-2.5 px-4">Uploaded File Header</th>
                      <th className="py-2.5 px-4">Sample Value</th>
                      <th className="py-2.5 px-4">Target Canonical Field</th>
                      <th className="py-2.5 px-4">Match Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {uploadData.sourceHeaders.map((header) => {
                      const sampleVal = uploadData.sampleRows[0]?.[header] || '—';
                      const mappedField = mappings[header] || '';
                      const isAuto = uploadData.suggestedMappings[header] === mappedField && mappedField !== '';

                      return (
                        <tr key={header} className="hover:bg-surface-muted/50">
                          <td className="py-2.5 px-4 font-mono font-medium text-ink-900">{header}</td>
                          <td className="py-2.5 px-4 text-ink-500 font-mono text-[11px] truncate max-w-xs">
                            {sampleVal}
                          </td>
                          <td className="py-2.5 px-4">
                            <select
                              value={mappedField}
                              onChange={(e) =>
                                setMappings({ ...mappings, [header]: e.target.value })
                              }
                              className="w-full text-xs py-1 px-2 border border-line rounded focus:ring-1 focus:ring-ink-900 focus:outline-none bg-surface-card"
                            >
                              <option value="">-- Do Not Import / Skip Column --</option>
                              {CANONICAL_FIELDS.map((f) => (
                                <option key={f.key} value={f.key}>
                                  {f.label} {f.mandatory ? '*' : ''}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td className="py-2.5 px-4">
                            {mappedField ? (
                              <span
                                className={`text-[10px] font-semibold px-2 py-0.5 rounded ${
                                  isAuto ? 'bg-success/10 text-success' : 'bg-brand-700/10 text-brand-700'
                                }`}
                              >
                                {isAuto ? 'Auto-Matched' : 'Custom Mapped'}
                              </span>
                            ) : (
                              <span className="text-[10px] text-ink-400 font-medium px-2 py-0.5 bg-surface-muted rounded">
                                Unmapped
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div className="flex justify-between items-center pt-2">
                <Button variant="secondary" onClick={() => setWizardStep(1)}>
                  Back
                </Button>
                <Button
                  variant="primary"
                  disabled={isValidating}
                  onClick={handleValidateMappings}
                >
                  {isValidating ? (
                    <>
                      <RefreshIcon className="h-4 w-4 mr-2 animate-spin" />
                      Validating Rows...
                    </>
                  ) : (
                    <>
                      Run Row-Level Validation
                      <ChevronRightIcon className="h-4 w-4 ml-1.5" />
                    </>
                  )}
                </Button>
              </div>
            </Card>
          )}

          {/* STEP 3: PREVIEW & VALIDATION RESULTS */}
          {wizardStep === 3 && previewData && (
            <div className="space-y-6">
              {/* Stat Summary Cards */}
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                <div className="p-3 bg-surface-card border border-line rounded-xl text-center shadow-sm">
                  <p className="text-[11px] font-semibold uppercase text-ink-500">Total Rows</p>
                  <p className="text-xl font-bold text-ink-900 font-mono mt-0.5">{previewData.totalRows}</p>
                </div>
                <div className="p-3 bg-surface-card border border-line rounded-xl text-center shadow-sm">
                  <p className="text-[11px] font-semibold uppercase text-success">Valid Rows</p>
                  <p className="text-xl font-bold text-success font-mono mt-0.5">{previewData.validRows}</p>
                </div>
                <div className="p-3 bg-surface-card border border-line rounded-xl text-center shadow-sm">
                  <p className="text-[11px] font-semibold uppercase text-danger">Invalid Rows</p>
                  <p className="text-xl font-bold text-danger font-mono mt-0.5">{previewData.errorRows}</p>
                </div>
                <div className="p-3 bg-surface-card border border-line rounded-xl text-center shadow-sm">
                  <p className="text-[11px] font-semibold uppercase text-warning">Duplicates</p>
                  <p className="text-xl font-bold text-warning font-mono mt-0.5">{previewData.duplicateRows}</p>
                </div>
                <div className="p-3 bg-surface-card border border-line rounded-xl text-center shadow-sm">
                  <p className="text-[11px] font-semibold uppercase text-ink-600">Review Required</p>
                  <p className="text-xl font-bold text-ink-700 font-mono mt-0.5">{previewData.reviewRows}</p>
                </div>
              </div>

              {/* Action Toolbar */}
              <div className="flex flex-wrap items-center justify-between gap-3 bg-surface-card p-3 border border-line rounded-xl shadow-sm">
                <div className="flex gap-1.5">
                  {['ALL', 'VALID', 'INVALID', 'DUPLICATE', 'REVIEW_REQUIRED'].map((f) => (
                    <button
                      key={f}
                      onClick={() => setStatusFilter(f)}
                      className={`text-xs px-2.5 py-1 rounded font-medium transition-colors ${
                        statusFilter === f
                          ? 'bg-ink-900 text-white'
                          : 'bg-surface-muted text-ink-600 hover:bg-line'
                      }`}
                    >
                      {f}
                    </button>
                  ))}
                </div>

                <div className="flex items-center gap-2">
                  {previewData.errorRows > 0 && (
                    <>
                      <Button variant="secondary" size="sm" onClick={() => handleDownloadErrors('csv')}>
                        <DownloadIcon className="h-3.5 w-3.5 mr-1" />
                        Download Errors (CSV)
                      </Button>
                      <Button variant="secondary" size="sm" onClick={() => handleDownloadErrors('xlsx')}>
                        <DownloadIcon className="h-3.5 w-3.5 mr-1" />
                        Download Errors (Excel)
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => currentJobId && handleTriggerResubmit(currentJobId)}
                      >
                        Resubmit Corrected Batch
                      </Button>
                    </>
                  )}
                  <Button
                    variant="primary"
                    size="sm"
                    disabled={previewData.validRows === 0 || isLoadingRecon}
                    onClick={handleProceedToReconciliation}
                  >
                    Proceed to Reconciliation & Commit
                    <ChevronRightIcon className="h-4 w-4 ml-1" />
                  </Button>
                </div>
              </div>

              {/* Preview Table */}
              <DataTable
                columns={previewColumns}
                rows={filteredPreviewRows}
                getRowId={(r) => r.id}
                caption="Normalized row preview with validation feedback"
                emptyTitle="No records match current filter"
                emptyDescription="Try selecting another filter status above."
              />
            </div>
          )}

          {/* STEP 4: RECONCILIATION & COMMIT */}
          {wizardStep === 4 && reconData && (
            <Card className="p-6 space-y-6">
              <div>
                <h3 className="text-lg font-bold text-ink-900">Step 4: Successive Submission Reconciliation</h3>
                <p className="text-xs text-ink-500">
                  Compares current valid rows against previously accepted academic enrolment records for this institution.
                </p>
              </div>

              {isAlreadyCommitted && (
                <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <CheckCircleIcon className="w-5 h-5 text-emerald-600 shrink-0" />
                    <div>
                      <h4 className="text-sm font-bold text-emerald-900">Enrolment Import Completed</h4>
                      <p className="text-xs text-emerald-700">
                        This batch was already committed to the student registry (Status: {previewData?.job.status}).
                      </p>
                    </div>
                  </div>
                  <span className="text-xs font-semibold px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-full border border-emerald-300">
                    {previewData?.job.status}
                  </span>
                </div>
              )}

              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl">
                  <p className="text-xs font-semibold text-emerald-800">New Admissions</p>
                  <p className="text-2xl font-bold text-emerald-900 font-mono mt-1">{reconData.newAdmissions}</p>
                  <p className="text-[11px] text-emerald-700 mt-1">Will be newly enrolled</p>
                </div>
                <div className="p-4 bg-sky-50 border border-sky-200 rounded-xl">
                  <p className="text-xs font-semibold text-sky-800">Updated Records</p>
                  <p className="text-2xl font-bold text-sky-900 font-mono mt-1">{reconData.updatedCount}</p>
                  <p className="text-[11px] text-sky-700 mt-1">New version snapshots recorded</p>
                </div>
                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl">
                  <p className="text-xs font-semibold text-slate-700">Unchanged Records</p>
                  <p className="text-2xl font-bold text-slate-800 font-mono mt-1">{reconData.unchangedCount}</p>
                  <p className="text-[11px] text-slate-600 mt-1">Identical to existing registry</p>
                </div>
                <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl">
                  <p className="text-xs font-semibold text-amber-800">Missing from Snapshot</p>
                  <p className="text-2xl font-bold text-amber-900 font-mono mt-1">{reconData.missingFromSnapshotCount}</p>
                  <p className="text-[11px] text-amber-700 mt-1">Preserved; not deleted</p>
                </div>
              </div>

              {/* Reconciliation Items Breakdown */}
              {reconData.items.length > 0 && (
                <div className="border border-line rounded-lg overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-surface-muted text-ink-700 font-semibold uppercase text-[11px]">
                      <tr>
                        <th className="py-2 px-3">Student Identifier</th>
                        <th className="py-2 px-3">Student Name</th>
                        <th className="py-2 px-3">APAAR ID</th>
                        <th className="py-2 px-3">Action</th>
                        <th className="py-2 px-3">Details / Diffs</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-line">
                      {reconData.items.slice(0, 15).map((item, idx) => (
                        <tr key={idx} className="hover:bg-surface-muted/50">
                          <td className="py-2 px-3 font-mono font-medium">{item.studentIdentifier}</td>
                          <td className="py-2 px-3 font-medium">{item.studentName}</td>
                          <td className="py-2 px-3 font-mono text-[11px]">{item.apaarId || '—'}</td>
                          <td className="py-2 px-3">
                            <span
                              className={`text-[10px] font-semibold px-2 py-0.5 rounded ${
                                item.action === 'NEW_ADMISSION'
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : item.action === 'DATA_UPDATE'
                                    ? 'bg-sky-100 text-sky-800'
                                    : item.action === 'MISSING_FROM_SNAPSHOT'
                                      ? 'bg-amber-100 text-amber-800'
                                      : 'bg-slate-100 text-slate-700'
                              }`}
                            >
                              {item.action}
                            </span>
                          </td>
                          <td className="py-2 px-3 font-mono text-[11px] text-ink-500">
                            {Object.keys(item.changedFields).length > 0
                              ? JSON.stringify(item.changedFields)
                              : 'No changes'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {reconData.items.length > 15 && (
                    <div className="p-2 text-center text-xs text-ink-500 bg-surface-muted">
                      Showing first 15 of {reconData.items.length} reconciliation items
                    </div>
                  )}
                </div>
              )}

              <div className="flex justify-between items-center pt-2">
                <Button variant="secondary" onClick={() => setWizardStep(3)}>
                  Back to Preview
                </Button>
                {isAlreadyCommitted ? (
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-ink-500 font-medium italic">
                      Already committed to database
                    </span>
                    <Button
                      variant="primary"
                      onClick={() => {
                        setCurrentJobId(null);
                        setSelectedFile(null);
                        setUploadData(null);
                        setPreviewData(null);
                        setReconData(null);
                        setCommitSuccess(null);
                        setUploadError(null);
                        setWizardStep(1);
                      }}
                    >
                      Start New Import
                    </Button>
                  </div>
                ) : (
                  <Button
                    variant="primary"
                    onClick={() => setIsCommitDialogOpen(true)}
                    disabled={isCommitting || (previewData?.validRows ?? 0) === 0}
                  >
                    {isCommitting ? 'Committing...' : 'Commit Valid Enrolments to Database'}
                  </Button>
                )}
              </div>
            </Card>
          )}
        </div>
      )}

      {/* TAB: HISTORY */}
      {activeTab === 'history' && (
        <div className="space-y-4">
          <DataTable
            columns={[
              {
                key: 'fileName',
                header: 'File Name',
                render: (row) => (
                  <div>
                    <p className="font-medium text-ink-900">{row.fileName ?? 'Batch Data Upload'}</p>
                    <p className="text-xs text-ink-500 font-mono">ID: {row.id.slice(0, 8)}...</p>
                  </div>
                ),
              },
              {
                key: 'institution',
                header: 'Institution',
                render: (row) => row.institutionName ?? 'Default Institution',
              },
              {
                key: 'counts',
                header: 'Rows (Total / Valid / Errors)',
                render: (row) => (
                  <span className="font-mono text-xs">
                    {row.totalRows ?? 0} /{' '}
                    <span className="text-success font-medium">{row.validRows ?? 0}</span> /{' '}
                    <span className={row.errorRows ? 'text-danger font-bold' : 'text-ink-500'}>
                      {row.errorRows ?? 0}
                    </span>
                  </span>
                ),
              },
              {
                key: 'status',
                header: 'Status',
                render: (row) => {
                  const tone =
                    row.status === 'COMPLETED'
                      ? 'success'
                      : row.status === 'VALIDATED'
                        ? 'info'
                        : row.status === 'VALIDATED_WITH_ERRORS'
                          ? 'warning'
                          : row.status === 'FAILED'
                            ? 'danger'
                            : 'neutral';
                  return <StatusBadge status={row.status} label={row.status} tone={tone} />;
                },
              },
              {
                key: 'actions',
                header: 'Actions',
                render: (row) => (
                  <div className="flex items-center gap-2">
                    <Button variant="ghost" size="sm" onClick={() => handleInspectJob(row.id)}>
                      Inspect
                    </Button>
                    {(row.errorRows ?? 0) > 0 && (
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleTriggerResubmit(row.id)}
                      >
                        Resubmit
                      </Button>
                    )}
                  </div>
                ),
              },
            ]}
            rows={historyData?.content ?? []}
            getRowId={(row) => row.id}
            caption="History of student data ingestion jobs"
            loading={isHistoryLoading}
            emptyTitle="No import jobs recorded"
            emptyDescription="Submitted batch files will appear here with verification and error breakdown."
          />
          {historyData && historyData.totalPages > 1 && (
            <Pagination
              page={historyData.page}
              totalPages={historyData.totalPages}
              totalElements={historyData.totalElements}
              size={historyData.size}
              onPageChange={setHistoryPage}
            />
          )}
        </div>
      )}

      {/* COMMIT CONFIRM DIALOG */}
      <ConfirmDialog
        open={isCommitDialogOpen}
        title="Commit Valid Student Enrolments?"
        description={`This will persist ${previewData?.validRows ?? 0} valid student records into the authoritative student registry and create active enrolments. Immutable audit versions will be preserved for any updated student attributes.`}
        confirmLabel={isCommitting ? 'Committing...' : 'Confirm & Commit'}
        tone="primary"
        loading={isCommitting}
        onConfirm={handleConfirmCommit}
        onCancel={() => setIsCommitDialogOpen(false)}
      />
    </div>
  );
}
