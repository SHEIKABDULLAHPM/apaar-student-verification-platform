import { apiClient } from '@/lib/http';
import type { AuthTokens, AuthUser } from '@/api/types';

/** Login is unauthenticated; skipAuth prevents attaching a stale Bearer token. */
export async function login(email: string, password: string): Promise<AuthTokens> {
  const { data } = await apiClient.post<AuthTokens>(
    '/auth/login',
    { email, password },
    { skipAuth: true },
  );
  return data;
}

export async function getCurrentUser(): Promise<AuthUser> {
  const { data } = await apiClient.get<AuthUser>('/auth/me');
  return data;
}

/** Best-effort server-side revocation; local session is cleared regardless. */
export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout');
}
