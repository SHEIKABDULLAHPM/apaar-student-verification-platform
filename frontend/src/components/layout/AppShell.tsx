import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from '@/components/layout/Sidebar';
import { Topbar } from '@/components/layout/Topbar';
import { APP_OWNER } from '@/config';

export function AppShell() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();

  // Close the mobile drawer whenever a route changes.
  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  // Lock background scrolling while the mobile drawer is open.
  useEffect(() => {
    if (!mobileOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [mobileOpen]);

  return (
    <div className="min-h-screen bg-surface">
      <a
        href="#main-content"
        className="sr-only z-[60] rounded-md bg-brand-800 px-4 py-2 text-sm font-semibold text-white focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
      >
        Skip to main content
      </a>

      <Sidebar mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />

      <div className="lg:pl-72">
        <Topbar onMenuClick={() => setMobileOpen(true)} mobileNavOpen={mobileOpen} />

        <main
          id="main-content"
          tabIndex={-1}
          className="mx-auto w-full max-w-[90rem] px-4 py-8 focus:outline-none sm:px-6 lg:px-8"
        >
          <Outlet />
        </main>

        <footer className="mx-auto w-full max-w-[90rem] border-t border-line px-4 py-6 text-xs text-ink-500 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <p>
              © {new Date().getFullYear()} {APP_OWNER}. APAAR student data verification platform.
            </p>
            <p>Access is logged and subject to AICTE information-security policy.</p>
          </div>
        </footer>
      </div>
    </div>
  );
}
