import { apiClient } from '@/lib/http';
import type {
  ApaarVerificationResult,
  FormatValidationResult,
  PublicVerification,
} from '@/api/types';

export async function formatValidate(
  studentId: string,
  apaarId: string,
): Promise<FormatValidationResult> {
  const { data } = await apiClient.post<FormatValidationResult>(
    '/apaar-verifications/format-validate',
    { studentId, apaarId },
  );
  return data;
}

export async function verifyApaar(studentId: string): Promise<ApaarVerificationResult> {
  const { data } = await apiClient.post<ApaarVerificationResult>(
    `/apaar-verifications/${encodeURIComponent(studentId)}/verify`,
    { mode: 'SIMULATED' },
  );
  return data;
}

/** Public endpoint: intentionally never sends a Bearer token. */
export async function getPublicVerification(publicRef: string): Promise<PublicVerification> {
  const { data } = await apiClient.get<PublicVerification>(
    `/verifications/${encodeURIComponent(publicRef)}`,
    { skipAuth: true },
  );
  return data;
}
