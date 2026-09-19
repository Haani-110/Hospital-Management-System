import { Link } from 'react-router-dom';
import { Lock } from 'lucide-react';
import { buttonStyles } from '@/components/ui/button-styles';
import {  } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { roleLabel } from '@/lib/labels';
import { useAuth } from '@/providers/auth-context';

/** Shown when the signed-in role may not access a screen. */
export function ForbiddenPage() {
  const { user } = useAuth();

  return (
    <div className="py-10">
      <EmptyState
        size="page"
        titleAs="h1"
        icon={Lock}
        title="You don't have access to this area"
        description={
          user
            ? `Your account is signed in as ${roleLabel(user.role)}. This screen requires different permissions — the API will also reject requests from roles that are not allowed.`
            : 'This screen requires different permissions.'
        }
        action={
          <Link to="/" className={buttonStyles({ variant: 'outline', size: 'sm' })}>
            Back to dashboard
          </Link>
        }
      />
    </div>
  );
}
