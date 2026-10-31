import { NavLink } from 'react-router-dom';
import type { NavItem } from '@/nav';
import { NAV_SECTIONS } from '@/nav';
import { useAuth } from '@/auth/AuthContext';
import { CloseIcon, ShieldCheckIcon } from '@/components/icons';
import { cn } from '@/lib/cn';

function SidebarBrand({ className }: { className?: string }) {
  return (
    <div className={cn('flex items-center gap-3 border-b border-white/10 px-5 py-5', className)}>
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white/10 text-accent-300">
        <ShieldCheckIcon className="h-6 w-6" />
      </span>
      <div className="min-w-0">
        <p className="text-sm font-semibold tracking-wide text-white">APAAR</p>
        <p className="truncate text-xs text-brand-100/70">Student verification platform</p>
      </div>
    </div>
  );
}

function SidebarNav({
  onNavigate,
  id,
}: {
  onNavigate?: () => void;
  id?: string;
}) {
  const { hasAnyRole } = useAuth();

  const sections = NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => hasAnyRole(item.roles)),
  })).filter((section) => section.items.length > 0);

  function linkClass({ isActive }: { isActive: boolean }): string {
    return cn(
      'group flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
      isActive
        ? 'bg-white/15 text-white shadow-sm'
        : 'text-brand-100/75 hover:bg-white/10 hover:text-white',
    );
  }

  return (
    <nav id={id} aria-label="Primary" className="flex-1 space-y-6 overflow-y-auto px-3 py-5">
      {sections.map((section) => (
        <div key={section.label}>
          <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-brand-200/60">
            {section.label}
          </p>
          <ul className="space-y-1">
            {section.items.map((item: NavItem) => {
              const Icon = item.icon;
              return (
                <li key={item.to}>
                  <NavLink to={item.to} className={linkClass} onClick={onNavigate}>
                    <Icon className="h-5 w-5 shrink-0 opacity-90" />
                    <span className="truncate">{item.label}</span>
                  </NavLink>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );
}

function SidebarFooter() {
  return (
    <div className="border-t border-white/10 px-5 py-4">
      <p className="text-[11px] leading-relaxed text-brand-100/60">
        Authorisation is enforced by the server. Navigation visibility is a convenience only.
      </p>
      <p className="mt-2 text-[11px] font-medium text-brand-100/80">AICTE · Government of India</p>
    </div>
  );
}

export interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
}

export function Sidebar({ mobileOpen, onClose }: SidebarProps) {
  return (
    <>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 flex-col bg-brand-950 lg:flex">
        <SidebarBrand />
        <SidebarNav />
        <SidebarFooter />
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-ink-900/60"
            aria-label="Close navigation"
            onClick={onClose}
            tabIndex={-1}
          />
          <aside
            role="dialog"
            aria-modal="true"
            aria-label="Primary navigation"
            className="relative flex h-full w-80 max-w-[85vw] animate-slide-in flex-col bg-brand-950 shadow-raised"
          >
            <div className="flex items-center justify-between border-b border-white/10 pr-3">
              <SidebarBrand className="min-w-0 border-b-0" />
              <button
                type="button"
                onClick={onClose}
                className="rounded-md p-2 text-brand-100 hover:bg-white/10 hover:text-white"
                aria-label="Close navigation"
              >
                <CloseIcon className="h-5 w-5" />
              </button>
            </div>
            <SidebarNav id="mobile-primary-navigation" onNavigate={onClose} />
            <SidebarFooter />
          </aside>
        </div>
      )}
    </>
  );
}
