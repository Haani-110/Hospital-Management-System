import { useEffect, useId, useRef } from 'react';
import { LogOut, Moon, Sun, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/providers/auth-context';
import { useTheme } from '@/providers/theme-context';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Logo } from './logo';
import { SidebarNav } from './sidebar-nav';
import { roleLabel } from '@/lib/labels';

/** Signed-in user block shown at the bottom of the sidebar. */
export function UserPanel({ className }: { className?: string }) {
  const { user, logout } = useAuth();
  if (!user) return null;

  return (
    <div className={cn('border-t border-border p-3', className)}>
      <div className="flex items-center gap-2.5 rounded-md px-1.5 py-1.5">
        <Avatar name={user.username} size="sm" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-[13px] font-medium text-fg">{user.username}</p>
          <p className="truncate text-[11px] text-subtle">{roleLabel(user.role)}</p>
        </div>
      </div>
      <Button
        variant="ghost"
        size="sm"
        className="mt-1 w-full justify-start"
        onClick={logout}
      >
        <LogOut aria-hidden className="size-3.5" />
        Sign out
      </Button>
    </div>
  );
}

/** Theme switch used in the sidebar and topbar. */
export function ThemeToggle({ className }: { className?: string }) {
  const { theme, toggleTheme } = useTheme();
  const label = theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme';

  return (
    <Button
      variant="ghost"
      size="sm"
      onClick={toggleTheme}
      aria-label={label}
      title={label}
      className={className}
    >
      {theme === 'dark' ? (
        <Sun aria-hidden className="size-4" />
      ) : (
        <Moon aria-hidden className="size-4" />
      )}
      <span className="lg:hidden">Theme</span>
    </Button>
  );
}

interface SidebarProps {
  /** Mobile drawer state; ignored on desktop where the sidebar is persistent. */
  open: boolean;
  onClose: () => void;
}

/**
 * Persistent sidebar from the `lg` breakpoint up, and an accessible modal
 * drawer below it. The drawer closes on Escape, on backdrop click, and after
 * navigating.
 */
export function Sidebar({ open, onClose }: SidebarProps) {
  const { user } = useAuth();
  const panelRef = useRef<HTMLDivElement>(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    // Prevent the page behind the drawer from scrolling.
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    panelRef.current?.focus();
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, onClose]);

  return (
    <>
      {/* Desktop: persistent column */}
      <aside className="hidden w-64 shrink-0 border-r border-border bg-surface lg:flex lg:flex-col">
        <div className="flex h-16 items-center border-b border-border px-4">
          <Logo />
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <SidebarNav role={user?.role} />
        </div>
        <UserPanel />
      </aside>

      {/* Mobile: modal drawer */}
      {open ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div
            className="animate-overlay-in absolute inset-0 bg-overlay"
            onClick={onClose}
            aria-hidden
          />
          <div
            ref={panelRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            tabIndex={-1}
            className="animate-fade-in absolute inset-y-0 left-0 flex w-[17rem] max-w-[85vw] flex-col border-r border-border bg-surface shadow-lg focus-visible:outline-none"
          >
            <div className="flex h-16 items-center justify-between border-b border-border pr-2 pl-4">
              <Logo />
              <Button variant="ghost" size="icon" onClick={onClose} aria-label="Close navigation">
                <X aria-hidden className="size-4" />
              </Button>
            </div>
            <h2 id={titleId} className="sr-only">
              Main navigation
            </h2>
            <div className="min-h-0 flex-1 overflow-y-auto">
              <SidebarNav role={user?.role} onNavigate={onClose} />
            </div>
            <UserPanel />
          </div>
        </div>
      ) : null}
    </>
  );
}
