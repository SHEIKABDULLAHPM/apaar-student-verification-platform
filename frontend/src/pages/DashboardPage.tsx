import { useAuth } from '@/auth/AuthContext';
import { useApiQuery } from '@/hooks/useApiQuery';
import { getAicteDashboard, getInstitutionDashboard } from '@/api/dashboards';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatCard } from '@/components/ui/StatCard';
import { ChartCard } from '@/components/charts/ChartCard';
import {
  IntakeEnrolmentChart,
  EnrolmentCategoryChart,
  VerificationActivityChart,
} from '@/components/charts/DashboardCharts';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import {
  ShieldCheckIcon,
  UsersIcon,
  AlertTriangleIcon,
  ScaleIcon,
  BuildingIcon,
} from '@/components/icons';

export function DashboardPage() {
  const { user, hasAnyRole } = useAuth();
  const isPlatformUser = hasAnyRole(['PLATFORM_ADMIN', 'AICTE_OFFICER']);

  const {
    data: dashboard,
    isLoading,
    error,
    reload,
  } = useApiQuery(
    () => (isPlatformUser ? getAicteDashboard() : getInstitutionDashboard()),
    [isPlatformUser]
  );

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <LoadingSpinner label="Loading regulatory dashboard..." size="lg" />
      </div>
    );
  }

  if (error || !dashboard) {
    return (
      <ErrorState
        title="Unable to load dashboard"
        error={error}
        onRetry={reload}
      />
    );
  }

  const verifiedPercent =
    dashboard.studentCount > 0
      ? Math.round((dashboard.apaarVerifiedCount / dashboard.studentCount) * 100)
      : 0;

  return (
    <div className="space-y-8">
      <PageHeader
        title={isPlatformUser ? 'AICTE Regulatory Intelligence Dashboard' : 'Institutional Governance Dashboard'}
        description={
          isPlatformUser
            ? 'National overview of AICTE-approved institutions, student enrolments, APAAR identity compliance, and AI-flagged anomalies.'
            : `Institutional compliance and student verification overview for ${user?.institutionName ?? 'Registered Institution'}.`
        }
        eyebrow="Real-Time Regulatory Oversight"
      />

      {/* Primary KPI Metrics */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isPlatformUser && (
          <StatCard
            label="Approved Institutions"
            value={dashboard.institutionCount ?? 0}
            icon={<BuildingIcon className="h-6 w-6 text-brand-600" />}
            hint="Active AICTE-approved colleges"
            tone="default"
          />
        )}
        <StatCard
          label="Total Students"
          value={dashboard.studentCount}
          icon={<UsersIcon className="h-6 w-6 text-brand-600" />}
          hint={`${dashboard.enrolmentCount} active programme enrolments`}
          tone="default"
        />
        <StatCard
          label="APAAR Verified Rate"
          value={`${verifiedPercent}%`}
          icon={<ShieldCheckIcon className="h-6 w-6 text-success" />}
          hint={`${dashboard.apaarVerifiedCount} of ${dashboard.studentCount} identities verified`}
          tone={verifiedPercent > 80 ? 'success' : 'warning'}
        />
        <StatCard
          label="AI Anomaly Alerts"
          value={dashboard.anomalyAlertCount}
          icon={<AlertTriangleIcon className="h-6 w-6 text-danger" />}
          hint="Statistical & ML anomalies flagged for review"
          tone={dashboard.anomalyAlertCount > 0 ? 'danger' : 'success'}
        />
        <StatCard
          label="Open Investigation Cases"
          value={dashboard.openCaseCount}
          icon={<ScaleIcon className="h-6 w-6 text-warning" />}
          hint={`${dashboard.openConflictCount} identity conflicts unresolved`}
          tone={dashboard.openCaseCount > 0 ? 'warning' : 'default'}
        />
      </div>

      {/* Visual Analytics */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <ChartCard
          title="Approved Intake vs Actual Enrolment"
          description="Monitoring compliance with sanctioned institution capacity across programmes"
        >
          <IntakeEnrolmentChart data={dashboard.intakeVsEnrolment} />
        </ChartCard>

        <ChartCard
          title="Enrolment Distribution by Category"
          description="Category-wise seat distribution (General, OBC, SC, ST, EWS, Supernumerary)"
        >
          <EnrolmentCategoryChart data={dashboard.enrolmentsByCategory} />
        </ChartCard>
      </div>

      <div className="grid grid-cols-1 gap-6">
        <ChartCard
          title="Academic Verification Activity"
          description="Trend of authorized verification requests and QR-based integrity checks"
        >
          <VerificationActivityChart data={dashboard.verificationActivity} />
        </ChartCard>
      </div>
    </div>
  );
}
