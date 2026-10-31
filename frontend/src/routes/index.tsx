import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppShell } from '@/components/layout/AppShell';
import { ProtectedRoute } from '@/components/routing/ProtectedRoute';
import { RoleGate } from '@/components/routing/RoleGate';
import { PAGE_ACCESS } from '@/auth/roles';

import { LoginPage } from '@/pages/LoginPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { InstitutionsPage } from '@/pages/InstitutionsPage';
import { ProgrammesPage } from '@/pages/ProgrammesPage';
import { StudentsPage } from '@/pages/StudentsPage';
import { EnrolmentsPage } from '@/pages/EnrolmentsPage';
import { ImportsPage } from '@/pages/ImportsPage';
import { CompliancePage } from '@/pages/CompliancePage';
import { AnomaliesPage } from '@/pages/AnomaliesPage';
import { CasesPage } from '@/pages/CasesPage';
import { AuditPage } from '@/pages/AuditPage';
import { ReportsPage } from '@/pages/ReportsPage';
import { PublicVerifyPage } from '@/pages/PublicVerifyPage';
import { ForbiddenPage } from '@/pages/ForbiddenPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/verify',
    element: <PublicVerifyPage />,
  },
  {
    path: '/forbidden',
    element: <ForbiddenPage />,
  },
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          {
            index: true,
            element: <Navigate to="/dashboard" replace />,
          },
          {
            path: 'dashboard',
            element: <DashboardPage />,
          },
          {
            path: 'institutions',
            element: (
              <RoleGate roles={PAGE_ACCESS.institutions}>
                <InstitutionsPage />
              </RoleGate>
            ),
          },
          {
            path: 'programmes',
            element: (
              <RoleGate roles={PAGE_ACCESS.programmes}>
                <ProgrammesPage />
              </RoleGate>
            ),
          },
          {
            path: 'students',
            element: (
              <RoleGate roles={PAGE_ACCESS.students}>
                <StudentsPage />
              </RoleGate>
            ),
          },
          {
            path: 'enrolments',
            element: (
              <RoleGate roles={PAGE_ACCESS.enrolments}>
                <EnrolmentsPage />
              </RoleGate>
            ),
          },
          {
            path: 'imports',
            element: (
              <RoleGate roles={PAGE_ACCESS.imports}>
                <ImportsPage />
              </RoleGate>
            ),
          },
          {
            path: 'compliance',
            element: (
              <RoleGate roles={PAGE_ACCESS.compliance}>
                <CompliancePage />
              </RoleGate>
            ),
          },
          {
            path: 'anomalies',
            element: (
              <RoleGate roles={PAGE_ACCESS.anomalies}>
                <AnomaliesPage />
              </RoleGate>
            ),
          },
          {
            path: 'cases',
            element: (
              <RoleGate roles={PAGE_ACCESS.cases}>
                <CasesPage />
              </RoleGate>
            ),
          },
          {
            path: 'identity',
            element: (
              <RoleGate roles={PAGE_ACCESS.cases}>
                <CasesPage />
              </RoleGate>
            ),
          },
          {
            path: 'audit',
            element: (
              <RoleGate roles={PAGE_ACCESS.audit}>
                <AuditPage />
              </RoleGate>
            ),
          },
          {
            path: 'reports',
            element: (
              <RoleGate roles={PAGE_ACCESS.reports}>
                <ReportsPage />
              </RoleGate>
            ),
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);

