import { useState } from 'react';
import { useAuth } from '@/auth/AuthContext';
import { roleLabelsFor } from '@/auth/roles';
import { LogOutIcon, MenuIcon, ShieldCheckIcon } from '@/components/icons';
import { Button } from '@/components/ui/Button';

export interface TopbarProps {
  onMenuClick: () => void;
  mobileNavOpen: boolean;
}

export function Topbar({ onMenuClick, mobileNavOpen }: TopbarProps) {
  const { user, logout } = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  const roles = roleLabelsFor(user);

  async function handleSignOut() {
    setSigningOut(true);
    try {
      await logout();
    } finally {
      setSigningOut(false);
    }
  }

  return (
    <header className="sticky top-0 z-20 border-b border-line bg-surface-card/95 backdrop-blur">
      <div className="flex h-16 items-center gap-3 px-4 sm:px-6 lg:px-8">
        <button
          type="button"
          onClick={onMenuClick}
          className="rounded-md p-2 text-ink-700 hover:bg-surface-muted lg:hidden"
          aria-label="Open navigation"
          aria-expanded={mobileNavOpen}
          aria-controls="mobile-primary-navigation"
        >
          <MenuIcon className="h-5 w-5" />
        </button>

        <div className="flex min-w-0 items-center gap-2 lg:hidden">
          <ShieldCheckIcon className="h-5 w-5 text-brand-700" />
          <span className="text-sm font-semibold text-ink-900">APAAR</span>
        </div>

        <div className="ml-auto flex min-w-0 items-center gap-3 sm:gap-4">
          <div className="hidden min-w-0 text-right sm:block">
            <p className="truncate text-sm font-semibold text-ink-900">
              {user?.fullName ?? 'Signed-in user'}
            </p>
            <p className="truncate text-xs text-ink-500">
              {roles.length > 0 ? roles.join(' · ') : 'No assigned role'}
            </p>
          </div>

          <div className="hidden h-8 w-px bg-line sm:block" aria-hidden="true" />

          <div className="hidden max-w-56 text-right md:block">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-400">Scope</p>
            <p className="truncate text-xs text-ink-600" title={user?.institutionName ?? undefined}>
              {user?.institutionName?.trim() || 'AICTE platform'}
            </p>
          </div>

          <Button
            variant="secondary"
            size="sm"
            onClick={() => void handleSignOut()}
            loading={signingOut}
            leftIcon={<LogOutIcon className="h-4 w-4" />}
          >
            Sign out
          </Button>
        </div>
      </div>
    </header>
  );
}
