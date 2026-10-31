import { useState, type FormEvent } from 'react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ShieldCheckIcon } from '@/components/icons';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { apiClient } from '@/lib/http';
import { normalizeError } from '@/lib/errors';
import type { PublicVerification } from '@/api/types';

export function PublicVerifyPage() {
  const [reference, setReference] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<PublicVerification | null>(null);
  const [error, setError] = useState<string | null>(null);

  useDocumentTitle('Public Academic Verification');

  async function handleVerify(e: FormEvent) {
    e.preventDefault();
    if (!reference.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const { data } = await apiClient.get<PublicVerification>(
        `/verifications/public/${encodeURIComponent(reference.trim())}`
      );
      setResult(data);
    } catch (err) {
      setError(
        normalizeError(err).message ||
          'Verification record not found or reference has expired/been revoked.'
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-surface px-4 py-12 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-xl space-y-8">
        <div className="text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-brand-100 text-brand-700">
            <ShieldCheckIcon className="h-8 w-8" />
          </div>
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-ink-900">
            AICTE Academic Credential Verification
          </h1>
          <p className="mt-2 text-sm text-ink-600">
            Verify academic records, student ID/APAAR association, and degree credentials via authorized verification references.
          </p>
        </div>

        <Card className="p-6">
          <form onSubmit={handleVerify} className="space-y-4">
            <FormField
              id="verification-reference-input"
              label="Verification Reference Code"
              hint="Enter the reference code from the document or scanned QR token."
            >
              <Input
                id="verification-reference-input"
                placeholder="e.g. demo-verify-2026-synthetic-0001"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                required
              />
            </FormField>

            <Button type="submit" variant="primary" className="w-full" disabled={loading}>
              {loading ? 'Verifying...' : 'Verify Credential'}
            </Button>
          </form>
        </Card>

        {error && (
          <div className="rounded-md bg-red-50 p-4 border border-red-200">
            <p className="text-sm text-red-700 font-medium">{error}</p>
          </div>
        )}

        {result && (
          <Card className="p-6 space-y-4 border-2 border-emerald-500">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-ink-900">Verification Result</h2>
              <StatusBadge
                status={result.status}
                label={result.status}
                tone={result.status === 'VERIFIED' ? 'success' : 'neutral'}
              />
            </div>

            <div className="space-y-2 text-sm">
              <p>
                <span className="font-medium text-ink-500">Student Name:</span>{' '}
                <span className="font-semibold text-ink-900">{result.studentNameMasked}</span>
              </p>
              <p>
                <span className="font-medium text-ink-500">Institution:</span>{' '}
                <span className="text-ink-900">{result.institutionName}</span>
              </p>
              <p>
                <span className="font-medium text-ink-500">Programme:</span>{' '}
                <span className="text-ink-900">{result.programmeName}</span>
              </p>
              <p>
                <span className="font-medium text-ink-500">Academic Year:</span>{' '}
                <span className="text-ink-900">{result.academicYear}</span>
              </p>
            </div>

            <p className="text-xs text-ink-500 border-t border-line pt-3">
              Verified server-side via AICTE APAAR Governance Platform. Access event recorded for audit.
            </p>
          </Card>
        )}
      </div>
    </div>
  );
}
