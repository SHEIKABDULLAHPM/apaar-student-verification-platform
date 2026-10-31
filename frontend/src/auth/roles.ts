import type { AuthUser, Role } from '@/api/types';

export const ALL_ROLES: readonly Role[] = [
  'PLATFORM_ADMIN',
  'AICTE_OFFICER',
  'INSTITUTION_ADMIN',
  'INSTITUTION_OPERATOR',
  'VERIFICATION_OFFICER',
  'CASE_REVIEWER',
  'AUDITOR',
];

export const PLATFORM_ROLES: readonly Role[] = ['PLATFORM_ADMIN', 'AICTE_OFFICER'];

export const INSTITUTION_ROLES: readonly Role[] = [
  'INSTITUTION_ADMIN',
  'INSTITUTION_OPERATOR',
  'VERIFICATION_OFFICER',
  'CASE_REVIEWER',
];

export const ROLE_LABELS: Record<Role, string> = {
  PLATFORM_ADMIN: 'Platform administrator',
  AICTE_OFFICER: 'AICTE officer',
  INSTITUTION_ADMIN: 'Institution administrator',
  INSTITUTION_OPERATOR: 'Institution operator',
  VERIFICATION_OFFICER: 'Verification officer',
  CASE_REVIEWER: 'Case reviewer',
  AUDITOR: 'Auditor',
};

/**
 * Navigation/route visibility is a UX convenience only. Every API call is
 * authorised again by the backend; hiding a link is not a security control.
 */
export const PAGE_ACCESS = {
  institutions: ['PLATFORM_ADMIN', 'AICTE_OFFICER'],
  programmes: [
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
    'CASE_REVIEWER',
  ],
  students: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
    'CASE_REVIEWER',
  ],
  enrolments: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
    'CASE_REVIEWER',
  ],
  imports: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
    'CASE_REVIEWER',
  ],
  compliance: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
    'CASE_REVIEWER',
  ],
  anomalies: ['PLATFORM_ADMIN', 'AICTE_OFFICER', 'CASE_REVIEWER'],
  cases: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
    'CASE_REVIEWER',
  ],
  audit: ['PLATFORM_ADMIN', 'AUDITOR'],
  reports: ['PLATFORM_ADMIN', 'AICTE_OFFICER'],
} as const satisfies Record<string, readonly Role[]>;

export const ACTION_ACCESS = {
  uploadImports: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
  ],
  runComplianceEvaluation: ['PLATFORM_ADMIN', 'AICTE_OFFICER', 'INSTITUTION_ADMIN'],
  reviewAnomaly: ['PLATFORM_ADMIN', 'AICTE_OFFICER', 'INSTITUTION_ADMIN', 'CASE_REVIEWER'],
  commentOnCase: ['PLATFORM_ADMIN', 'AICTE_OFFICER', 'INSTITUTION_ADMIN', 'CASE_REVIEWER'],
  verifyApaar: [
    'PLATFORM_ADMIN',
    'AICTE_OFFICER',
    'INSTITUTION_ADMIN',
    'INSTITUTION_OPERATOR',
    'VERIFICATION_OFFICER',
  ],
  downloadReports: ['PLATFORM_ADMIN', 'AICTE_OFFICER'],
} as const satisfies Record<string, readonly Role[]>;

export function normalizeRole(role: string): string {
  return role.trim().toUpperCase();
}

export function hasAnyRole(
  userRoles: readonly string[] | null | undefined,
  required: readonly Role[],
): boolean {
  if (!userRoles || userRoles.length === 0) return false;
  const granted = new Set(userRoles.map(normalizeRole));
  return required.some((role) => granted.has(role));
}

export function roleLabelsFor(user: AuthUser | null): string[] {
  if (!user) return [];
  return user.roles.map((role) => {
    const normalized = normalizeRole(role) as Role;
    return ROLE_LABELS[normalized] ?? role;
  });
}

/** AICTE/platform/auditor users see the national dashboard. */
export function dashboardScopeFor(user: AuthUser | null): 'aicte' | 'institution' {
  if (!user) return 'institution';
  return hasAnyRole(user.roles, ['PLATFORM_ADMIN', 'AICTE_OFFICER', 'AUDITOR'])
    ? 'aicte'
    : 'institution';
}
