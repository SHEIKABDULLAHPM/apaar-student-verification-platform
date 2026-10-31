import { useNavigate } from 'react-router-dom';
import { LockIcon } from '@/components/icons';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';

export function ForbiddenPage() {
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader
        title="Access denied"
        eyebrow="403 · Authorisation"
        description="Your signed-in account does not have a role that permits this area."
      />
      <Card className="flex flex-col items-start gap-5 p-6 sm:flex-row sm:items-start">
        <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-danger-soft text-danger">
          <LockIcon className="h-6 w-6" />
        </span>
        <div className="min-w-0">
          <h2 className="text-base font-semibold text-ink-900">This action is not available to you</h2>
          <p className="mt-1.5 max-w-2xl text-sm text-ink-600">
            Navigation is filtered by role for clarity, but the API enforces authorisation for every
            request. If your responsibilities have changed, ask an AICTE administrator to update your
            role assignment.
          </p>
          <Button className="mt-5" variant="secondary" onClick={() => navigate('/dashboard')}>
            Return to dashboard
          </Button>
        </div>
      </Card>
    </div>
  );
}
