import type { AuthTokens } from '@/api/types';

/**
 * Session storage strategy
 * -----------------------
 * Both the access token and the rotating refresh token live in
 * `sessionStorage`, never `localStorage`. That means:
 *
 * 1. Tokens disappear when the tab/browser window closes, reducing the blast
 *    radius on shared institutional machines.
 * 2. Tokens are scoped to a single tab, so two users on the same browser do not
 *    overwrite each other's session.
 *
 * sessionStorage is still readable by JavaScript, so it does not eliminate XSS
 * risk. The mitigation is defence in depth: strict CSP at the nginx layer, no
 * `dangerouslySetInnerHTML`, dependency review, and server-side authorization
 * for every request. A future iteration can move refresh tokens to an
 * HttpOnly, Secure, SameSite=Strict cookie if the backend exposes CSRF
 * protection for that flow.
 */

export const SESSION_STORAGE_KEY = 'apaar.session.v1';
export const SESSION_EXPIRED_EVENT = 'apaar:session-expired';

export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  expiresAt: number | null;
}

let cached: StoredSession | null | undefined;

function storage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  } catch {
    // Some privacy modes throw on access.
    return null;
  }
}

function isStoredSession(value: unknown): value is StoredSession {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.accessToken === 'string' &&
    typeof candidate.refreshToken === 'string' &&
    (typeof candidate.expiresAt === 'number' || candidate.expiresAt === null)
  );
}

export function getSession(): StoredSession | null {
  if (cached !== undefined) return cached;

  const store = storage();
  if (!store) {
    cached = null;
    return cached;
  }

  try {
    const raw = store.getItem(SESSION_STORAGE_KEY);
    if (!raw) {
      cached = null;
      return cached;
    }
    const parsed: unknown = JSON.parse(raw);
    if (!isStoredSession(parsed)) {
      store.removeItem(SESSION_STORAGE_KEY);
      cached = null;
      return cached;
    }
    cached = parsed;
    return cached;
  } catch {
    store.removeItem(SESSION_STORAGE_KEY);
    cached = null;
    return cached;
  }
}

export function saveSession(
  tokens: Pick<AuthTokens, 'accessToken' | 'refreshToken' | 'expiresIn'>,
): StoredSession {
  const expiresAt =
    typeof tokens.expiresIn === 'number' && Number.isFinite(tokens.expiresIn)
      ? Date.now() + tokens.expiresIn * 1000
      : null;

  const session: StoredSession = {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    expiresAt,
  };

  cached = session;
  try {
    storage()?.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  } catch {
    // Storage can be unavailable (private mode/quota). The in-memory cache keeps
    // this tab usable for the current page lifetime.
  }
  return session;
}

export function clearSession(): void {
  cached = null;
  try {
    storage()?.removeItem(SESSION_STORAGE_KEY);
  } catch {
    // Ignore storage failures; the in-memory session is already cleared.
  }
}

export function hasSession(): boolean {
  return getSession() !== null;
}

export function notifySessionExpired(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  }
}

/** Test helper / debugging helper. */
export function resetSessionCache(): void {
  cached = undefined;
}
