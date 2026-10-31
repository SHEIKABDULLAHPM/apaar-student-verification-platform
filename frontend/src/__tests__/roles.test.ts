import { describe, it, expect } from 'vitest';
import {
  ALL_ROLES,
  PAGE_ACCESS,
  ACTION_ACCESS,
} from '@/auth/roles';

describe('RBAC definitions', () => {
  it('contains all 7 mandated roles', () => {
    expect(ALL_ROLES).toHaveLength(7);
    expect(ALL_ROLES).toContain('PLATFORM_ADMIN');
    expect(ALL_ROLES).toContain('AICTE_OFFICER');
    expect(ALL_ROLES).toContain('INSTITUTION_ADMIN');
    expect(ALL_ROLES).toContain('INSTITUTION_OPERATOR');
    expect(ALL_ROLES).toContain('VERIFICATION_OFFICER');
    expect(ALL_ROLES).toContain('CASE_REVIEWER');
    expect(ALL_ROLES).toContain('AUDITOR');
  });

  it('restricts national institutions directory to platform roles', () => {
    expect(PAGE_ACCESS.institutions).toEqual(['PLATFORM_ADMIN', 'AICTE_OFFICER']);
  });

  it('restricts audit view to platform admin and auditor', () => {
    expect(PAGE_ACCESS.audit).toEqual(['PLATFORM_ADMIN', 'AUDITOR']);
  });

  it('allows institution roles to upload data imports', () => {
    expect(ACTION_ACCESS.uploadImports).toContain('INSTITUTION_ADMIN');
    expect(ACTION_ACCESS.uploadImports).toContain('INSTITUTION_OPERATOR');
  });
});
