import { useNavigate } from 'react-router-dom';
import { FileQuestionIcon } from '@/components/icons';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { PageHeader } from '@/components/ui/PageHeader';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader
        title="Page not found"
        eyebrow="404"
        description="The page you requested does not exist, or the link is no longer valid."
      />
      <Card>
        <EmptyState
          icon={<FileQuestionIcon className="h-7 w-7" />}
          title="We could not find that page"
          description="Check the address or return to a known area of the platform."
          action={
            <div className="flex flex-wrap justify-center gap-2">
              <Button onClick={() => navigate('/dashboard')}>Go to dashboard</Button>
              <Button variant="secondary" onClick={() => navigate(-1)}>
                Go back
              </Button>
            </div>
          }
        />
      </Card>
    </div>
  );
}
