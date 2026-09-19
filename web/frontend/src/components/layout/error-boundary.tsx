import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Logo } from './logo';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

/**
 * Catches render-time crashes so a single broken screen cannot blank the app.
 *
 * The message shown to the user is deliberately generic; the technical detail
 * stays in the console for developers.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('Unhandled UI error', error, info.componentStack);
  }

  private handleReset = (): void => {
    this.setState({ error: null });
  };

  render(): ReactNode {
    if (!this.state.error) return this.props.children;

    return (
      <div className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-bg px-6 text-center">
        <Logo />
        <span
          aria-hidden
          className="flex size-11 items-center justify-center rounded-xl border border-border bg-elevated text-warning"
        >
          <AlertTriangle className="size-5" />
        </span>
        <div className="max-w-md">
          <h1 className="text-lg font-semibold text-fg">Something went wrong on this screen</h1>
          <p className="mt-2 text-sm leading-relaxed text-muted">
            The interface hit an unexpected error. Your data is safe — nothing was submitted. Reload
            the page or return to the dashboard to continue.
          </p>
        </div>
        <div className="flex gap-2">
          <Button onClick={this.handleReset} variant="outline" size="sm">
            Try again
          </Button>
          <Button
            size="sm"
            onClick={() => {
              window.location.assign('/');
            }}
          >
            Go to dashboard
          </Button>
        </div>
      </div>
    );
  }
}
