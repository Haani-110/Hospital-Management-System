import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { buttonStyles } from '@/components/ui/button-styles';
import {  } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';

export function NotFoundPage() {
  return (
    <div className="py-10">
      <EmptyState
        size="page"
        icon={Compass}
        title="Page not found"
        description="The page you were looking for doesn’t exist or has moved."
        action={
          <Link to="/" className={buttonStyles({ variant: 'outline', size: 'sm' })}>
            Back to dashboard
          </Link>
        }
      />
    </div>
  );
}
