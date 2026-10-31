import type { ReactNode } from 'react';
import type { Role } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { FullPageLoader } from '@/components/ui/FullPageLoader';
import { ForbiddenPage } from '@/pages/ForbiddenPage';

export interface RoleGateProps {
  roles: readonly Role[];
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * Client-side visibility guard only. The backend independently authorises
 * every request based on the authenticated principal's roles and scope.
 */
export function RoleGate({ roles, children, fallback }: RoleGateProps) {
  const { status, hasAnyRole } = useAuth();

  if (status === 'booting') return <FullPageLoader />;
  if (!hasAnyRole(roles)) return fallback ?? <ForbiddenPage />;

  return <>{children}</>;
}
