import type { ComponentType } from 'react';
import type { Role } from '@/api/types';
import { ALL_ROLES, PAGE_ACCESS } from '@/auth/roles';
import {
  AlertTriangleIcon,
  BookIcon,
  BuildingIcon,
  ClipboardIcon,
  DashboardIcon,
  DownloadIcon,
  ScaleIcon,
  ScrollIcon,
  ShieldCheckIcon,
  UploadIcon,
  UsersIcon,
  type IconProps,
} from '@/components/icons';

export interface NavItem {
  to: string;
  label: string;
  icon: ComponentType<IconProps>;
  roles: readonly Role[];
  description?: string;
}

export interface NavSection {
  label: string;
  items: NavItem[];
}

export const NAV_SECTIONS: NavSection[] = [
  {
    label: 'Overview',
    items: [
      {
        to: '/dashboard',
        label: 'Dashboard',
        icon: DashboardIcon,
        roles: ALL_ROLES,
        description: 'National or institution-level summary',
      },
    ],
  },
  {
    label: 'Registry',
    items: [
      {
        to: '/institutions',
        label: 'Institutions',
        icon: BuildingIcon,
        roles: PAGE_ACCESS.institutions,
      },
      {
        to: '/programmes',
        label: 'Programmes',
        icon: BookIcon,
        roles: PAGE_ACCESS.programmes,
      },
      {
        to: '/students',
        label: 'Students',
        icon: UsersIcon,
        roles: PAGE_ACCESS.students,
      },
      {
        to: '/enrolments',
        label: 'Enrolments',
        icon: ClipboardIcon,
        roles: PAGE_ACCESS.enrolments,
      },
    ],
  },
  {
    label: 'Operations',
    items: [
      {
        to: '/imports',
        label: 'Imports',
        icon: UploadIcon,
        roles: PAGE_ACCESS.imports,
      },
      {
        to: '/compliance',
        label: 'Compliance',
        icon: ShieldCheckIcon,
        roles: PAGE_ACCESS.compliance,
      },
      {
        to: '/anomalies',
        label: 'Anomalies',
        icon: AlertTriangleIcon,
        roles: PAGE_ACCESS.anomalies,
      },
      {
        to: '/cases',
        label: 'Identity & Cases',
        icon: ScaleIcon,
        roles: PAGE_ACCESS.cases,
      },
    ],
  },
  {
    label: 'Governance',
    items: [
      {
        to: '/audit',
        label: 'Audit trail',
        icon: ScrollIcon,
        roles: PAGE_ACCESS.audit,
      },
      {
        to: '/reports',
        label: 'Reports',
        icon: DownloadIcon,
        roles: PAGE_ACCESS.reports,
      },
    ],
  },
];
