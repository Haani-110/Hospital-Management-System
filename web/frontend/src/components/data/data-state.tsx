import { AlertTriangle, Lock, RefreshCw, SearchX, WifiOff } from 'lucide-react';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import type { ApiError } from '@/lib/api/client';

export interface ErrorStateProps {
  error: ApiError;
  onRetry?: () => void;
  /** Message shown for 403 responses. */
  forbiddenMessage?: string;
}

/**
 * Renders a failed request as a human-readable message.
 * Raw server text is only shown when the backend produced a safe message; the
 * generic branch never leaks internals.
 */
export function ErrorState({ error, onRetry, forbiddenMessage }: ErrorStateProps) {
  if (error.isForbidden) {
    return (
      <EmptyState
        icon={Lock}
        title="You do not have permission to view this"
        description={
          forbiddenMessage ??
          'This area is restricted for your role. If you believe you should have access, contact an administrator.'
        }
      />
    );
  }

  if (error.isNotFound) {
    return (
      <EmptyState
        icon={SearchX}
        title="Not found"
        description={error.messages.join(' ')}
      />
    );
  }

  if (error.isNetworkError) {
    return (
      <EmptyState
        icon={WifiOff}
        title="Cannot reach the API"
        description={error.messages.join(' ')}
        action={
          onRetry ? (
            <Button variant="outline" size="sm" onClick={onRetry}>
              <RefreshCw aria-hidden className="size-3.5" />
              Try again
            </Button>
          ) : null
        }
      />
    );
  }

  if (error.isUnauthorized) {
    return (
      <Alert tone="warning" title="Your session has ended">
        Sign in again to continue.
      </Alert>
    );
  }

  return (
    <EmptyState
      icon={AlertTriangle}
      title="Something went wrong"
      description={error.messages.join(' ')}
      action={
        onRetry ? (
          <Button variant="outline" size="sm" onClick={onRetry}>
            <RefreshCw aria-hidden className="size-3.5" />
            Try again
          </Button>
        ) : null
      }
    />
  );
}
