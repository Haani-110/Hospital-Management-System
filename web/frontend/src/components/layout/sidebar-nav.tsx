import { NavLink } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { navigationForRole } from './nav-config';
import type { Role } from '@/lib/api/types';

interface SidebarNavProps {
  role: Role | undefined;
  /** Called after a navigation happens (used to close the mobile drawer). */
  onNavigate?: () => void;
}

/** Sectioned primary navigation with an animated active indicator. */
export function SidebarNav({ role, onNavigate }: SidebarNavProps) {
  const sections = navigationForRole(role);

  return (
    <nav aria-label="Main navigation" className="flex flex-col gap-6 px-3 py-4">
      {sections.map((section) => (
        <div key={section.title}>
          <h2 className="px-2.5 pb-2 text-[10px] font-semibold tracking-[0.1em] text-subtle uppercase">
            {section.title}
          </h2>
          <ul className="space-y-0.5">
            {section.items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === '/'}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'group relative flex items-center gap-2.5 rounded-md px-2.5 py-2 text-[13px] font-medium transition-colors duration-150',
                      isActive
                        ? 'bg-elevated text-fg'
                        : 'text-muted hover:bg-elevated/60 hover:text-fg',
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      <span
                        aria-hidden
                        className={cn(
                          'absolute left-0 h-4 w-[2px] rounded-full bg-accent transition-all duration-200',
                          isActive ? 'opacity-100' : 'opacity-0',
                        )}
                      />
                      <item.icon
                        aria-hidden
                        className={cn(
                          'size-4 shrink-0 transition-colors',
                          isActive ? 'text-accent' : 'text-subtle group-hover:text-muted',
                        )}
                      />
                      <span className="truncate">{item.label}</span>
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}
