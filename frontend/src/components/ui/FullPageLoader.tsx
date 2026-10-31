import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { ShieldCheckIcon } from '@/components/icons';

export function FullPageLoader({ label = 'Checking your session' }: { label?: string }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-surface px-4">
      <div className="flex items-center gap-3 text-brand-800">
        <ShieldCheckIcon className="h-9 w-9" />
        <span className="text-sm font-semibold uppercase tracking-widest">APAAR</span>
      </div>
      <LoadingSpinner label={label} showLabel size="lg" />
    </div>
  );
}
