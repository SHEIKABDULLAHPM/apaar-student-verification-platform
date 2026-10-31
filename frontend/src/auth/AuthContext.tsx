import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { AuthUser, Role } from '@/api/types';
import * as authApi from '@/api/auth';
import {
  clearSession,
  getSession,
  hasSession,
  saveSession,
  SESSION_EXPIRED_EVENT,
} from '@/lib/session';
import { hasAnyRole as checkRoles } from '@/auth/roles';

export type AuthStatus = 'booting' | 'authenticated' | 'anonymous';

export interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  notice: string | null;
  login: (email: string, password: string) => Promise<AuthUser>;
  logout: () => Promise<void>;
  hasRole: (role: Role) => boolean;
  hasAnyRole: (roles: readonly Role[]) => boolean;
  clearNotice: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>(() =>
    hasSession() ? 'booting' : 'anonymous',
  );
  const [notice, setNotice] = useState<string | null>(null);

  // Resolve the current user from the stored access token (or refreshed token)
  // on first load. This is also how role changes from the server are picked up.
  useEffect(() => {
    if (!hasSession()) {
      setStatus('anonymous');
      return;
    }

    let cancelled = false;

    authApi
      .getCurrentUser()
      .then((currentUser) => {
        if (cancelled) return;
        setUser(currentUser);
        setStatus('authenticated');
      })
      .catch(() => {
        if (cancelled) return;
        clearSession();
        setUser(null);
        setStatus('anonymous');
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // The axios interceptor emits this when a refresh fails. Clearing local auth
  // state lets ProtectedRoute redirect to /login without a hard reload.
  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
      setStatus('anonymous');
      setNotice('Your session expired. Please sign in again.');
    };

    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpired);
  }, []);

  const login = useCallback(async (email: string, password: string): Promise<AuthUser> => {
    const tokens = await authApi.login(email, password);
    saveSession(tokens);
    setUser(tokens.user);
    setStatus('authenticated');
    setNotice(null);
    return tokens.user;
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    try {
      if (getSession()) {
        await authApi.logout();
      }
    } catch {
      // Revocation failure must not strand the user in a signed-in shell. The
      // tokens still expire server-side; local state is cleared below.
    } finally {
      clearSession();
      setUser(null);
      setStatus('anonymous');
      setNotice(null);
    }
  }, []);

  const hasRole = useCallback(
    (role: Role) => checkRoles(user?.roles, [role]),
    [user],
  );

  const hasAnyRole = useCallback(
    (roles: readonly Role[]) => checkRoles(user?.roles, roles),
    [user],
  );

  const clearNotice = useCallback(() => setNotice(null), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAuthenticated: status === 'authenticated',
      notice,
      login,
      logout,
      hasRole,
      hasAnyRole,
      clearNotice,
    }),
    [user, status, notice, login, logout, hasRole, hasAnyRole, clearNotice],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return context;
}
