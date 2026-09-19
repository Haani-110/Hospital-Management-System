import { Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Logo } from './logo';
import { ThemeToggle } from './sidebar';

/**
 * Compact top bar for small screens: navigation trigger, wordmark and the
 * theme switch. The desktop layout uses the persistent sidebar instead.
 */
export function Topbar({ onOpenNav }: { onOpenNav: () => void }) {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-2 border-b border-border bg-surface/95 px-3 backdrop-blur lg:hidden">
      <Button variant="ghost" size="icon" onClick={onOpenNav} aria-label="Open navigation">
        <Menu aria-hidden className="size-5" />
      </Button>
      <Logo />
      <div className="ml-auto">
        <ThemeToggle />
      </div>
    </header>
  );
}
