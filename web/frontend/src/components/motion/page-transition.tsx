import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';

/**
 * Fades and lifts page content on each route change (~240ms).
 *
 * Implemented with CSS so there is no animation dependency; the global
 * reduced-motion rule in index.css disables it when the user asks for less.
 */
export function PageTransition({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const { pathname } = useLocation();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    // Restart the entrance animation on navigation.
    node.classList.remove('animate-fade-up');
    void node.offsetWidth;
    node.classList.add('animate-fade-up');
    // Scroll the content area back to the top for a fresh screen.
    node.scrollTo?.({ top: 0 });
  }, [pathname]);

  return (
    <div ref={ref} className={cn('animate-fade-up', className)}>
      {children}
    </div>
  );
}
