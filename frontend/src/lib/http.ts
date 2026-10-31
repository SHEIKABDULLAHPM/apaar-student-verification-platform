import axios, { AxiosHeaders } from 'axios';
import { API_BASE_URL } from '@/config';
import {
  clearSession,
  getSession,
  notifySessionExpired,
  saveSession,
  type StoredSession,
} from '@/lib/session';
import type { AuthTokens } from '@/api/types';

/**
 * Shared axios client.
 *
 * - Base URL is relative by default (`/api/v1`) so dev proxy and nginx proxy
 *   work without CORS.
 * - Bearer access token is attached from sessionStorage.
 * - On the first 401 we attempt exactly one refresh, single-flight across
 *   concurrent requests. If refresh fails the local session is cleared and a
 *   session-expired event is emitted for the auth provider.
 */
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
});

function isAuthPath(url: string | undefined): boolean {
  if (!url) return false;
  return url.includes('/auth/login') || url.includes('/auth/refresh');
}

async function performRefresh(): Promise<StoredSession> {
  const current = getSession();
  if (!current?.refreshToken) {
    throw new Error('No refresh token available');
  }

  // Use the bare axios client so this call cannot recurse through the
  // interceptor. skipAuth is still set for clarity.
  const response = await axios.post<AuthTokens>(
    `${API_BASE_URL}/auth/refresh`,
    { refreshToken: current.refreshToken },
    { skipAuth: true },
  );

  return saveSession(response.data);
}

let refreshPromise: Promise<StoredSession> | null = null;

function ensureRefreshed(): Promise<StoredSession> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

apiClient.interceptors.request.use((config) => {
  if (!config.skipAuth) {
    const session = getSession();
    if (session?.accessToken) {
      config.headers = AxiosHeaders.from(config.headers);
      config.headers.set('Authorization', `Bearer ${session.accessToken}`);
    }
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) {
      throw error;
    }

    const config = error.config;
    const status = error.response?.status;
    const canAttemptRefresh =
      status === 401 &&
      Boolean(config) &&
      !config?._retry &&
      !config?.skipAuth &&
      !isAuthPath(config?.url) &&
      Boolean(getSession()?.refreshToken);

    if (!config || !canAttemptRefresh) {
      if (status === 401 && !isAuthPath(config?.url) && !config?.skipAuth) {
        clearSession();
        notifySessionExpired();
      }
      throw error;
    }

    config._retry = true;

    try {
      const refreshed = await ensureRefreshed();
      config.headers = AxiosHeaders.from(config.headers);
      config.headers.set('Authorization', `Bearer ${refreshed.accessToken}`);
      return apiClient(config);
    } catch {
      clearSession();
      notifySessionExpired();
      throw error;
    }
  },
);
