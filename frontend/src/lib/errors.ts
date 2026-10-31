import axios from 'axios';

export interface AppError {
  status?: number;
  code: string;
  message: string;
  details?: unknown;
  requestId?: string;
  timestamp?: string;
  isNetworkError?: boolean;
}

interface BackendErrorBody {
  code?: unknown;
  message?: unknown;
  details?: unknown;
  requestId?: unknown;
  timestamp?: unknown;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : null;
}

function extractBackendError(payload: unknown): BackendErrorBody | null {
  const body = asRecord(payload);
  if (!body) return null;

  const nested = asRecord(body.error);
  return nested ? nested : body;
}

function fallbackForStatus(status: number | undefined): { code: string; message: string } {
  if (!status) {
    return {
      code: 'NETWORK_ERROR',
      message: 'Unable to reach the server. Check your connection and try again.',
    };
  }
  if (status === 400) {
    return { code: 'BAD_REQUEST', message: 'The request could not be processed. Check the values and try again.' };
  }
  if (status === 401) {
    return { code: 'UNAUTHENTICATED', message: 'Your session is no longer valid. Please sign in again.' };
  }
  if (status === 403) {
    return {
      code: 'FORBIDDEN',
      message: 'You do not have permission to perform this action. Contact your AICTE administrator if you believe this is an error.',
    };
  }
  if (status === 404) {
    return { code: 'NOT_FOUND', message: 'The requested record could not be found.' };
  }
  if (status === 409) {
    return { code: 'CONFLICT', message: 'The request conflicts with the current state of the record.' };
  }
  if (status === 413) {
    return { code: 'PAYLOAD_TOO_LARGE', message: 'The uploaded file is larger than the server allows.' };
  }
  if (status === 422) {
    return { code: 'VALIDATION_FAILED', message: 'Some of the submitted values are not valid.' };
  }
  if (status === 429) {
    return { code: 'RATE_LIMITED', message: 'Too many requests were sent. Wait a moment and try again.' };
  }
  if (status >= 500) {
    return {
      code: 'SERVER_ERROR',
      message: 'The service is temporarily unavailable. Please try again shortly.',
    };
  }
  return { code: 'REQUEST_FAILED', message: 'The request failed. Please try again.' };
}

/**
 * Converts anything thrown by axios/fetch/components into a single predictable
 * shape so every screen can render the same error presentation.
 *
 * Backend contract:
 * { "error": { "code", "message", "details", "requestId", "timestamp" } }
 */
export function normalizeError(error: unknown): AppError {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const backend = extractBackendError(error.response?.data);
    const fallback = fallbackForStatus(status);

    const message =
      typeof backend?.message === 'string' && backend.message.trim()
        ? backend.message
        : fallback.message;
    const code =
      typeof backend?.code === 'string' && backend.code.trim() ? backend.code : fallback.code;

    return {
      status,
      code,
      message,
      details: backend?.details,
      requestId: typeof backend?.requestId === 'string' ? backend.requestId : undefined,
      timestamp: typeof backend?.timestamp === 'string' ? backend.timestamp : undefined,
      isNetworkError: !error.response,
    };
  }

  if (error instanceof Error) {
    return {
      code: 'UNEXPECTED_ERROR',
      message: error.message || 'An unexpected error occurred.',
    };
  }

  return {
    code: 'UNKNOWN_ERROR',
    message: 'An unexpected error occurred. Please try again.',
  };
}

/** Field-level messages are only used when the backend supplies structured details. */
export function getFieldErrors(error: AppError): Record<string, string> {
  const details = asRecord(error.details);
  const fieldErrors = details ? asRecord(details.fieldErrors) : null;
  if (!fieldErrors) return {};

  return Object.entries(fieldErrors).reduce<Record<string, string>>((acc, [key, value]) => {
    if (typeof value === 'string') acc[key] = value;
    return acc;
  }, {});
}
