import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './sidebar';
import { Topbar } from './topbar';
import { PageTransition } from '@/components/motion/page-transition';

/**
 * Authenticated application frame: persistent sidebar on desktop, drawer on
 * mobile, and a scrollable content area.
 *
 * The drawer remembers the route it was opened on, so a navigation (or a back
 * button press) closes it without an effect that would re-render on every
 * route change.
 */
export function AppShell() {
  const { pathname } = useLocation();
  const [openFor, setOpenFor] = useState<string | null>(null);
  const navOpen = openFor === pathname;

  return (
    <div className="flex min-h-dvh bg-bg">
      <Sidebar open={navOpen} onClose={() => setOpenFor(null)} />

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onOpenNav={() => setOpenFor(pathname)} />

        <main id="main-content" className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          <div className="mx-auto w-full max-w-[100rem]">
            <PageTransition>
              <Outlet />
            </PageTransition>
          </div>
        </main>

        <footer className="border-t border-border px-4 py-4 sm:px-6 lg:px-8">
          <p className="text-xs text-subtle">
            Hospital Management System v2.0 — data served live from the API.
          </p>
        </footer>
      </div>
    </div>
  );
}
