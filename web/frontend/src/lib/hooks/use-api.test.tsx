import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useApi } from './use-api';
import { ApiError } from '../api/client';

/** Minimal harness exposing the hook's state as text. */
function Harness({ request }: { request: (signal: AbortSignal) => Promise<string> }) {
  const { data, error, isLoading, isInitialLoading, refetch } = useApi(request);
  return (
    <div>
      <p data-testid="data">{data ?? 'none'}</p>
      <p data-testid="error">{error ? error.messages.join(',') : 'none'}</p>
      <p data-testid="state">
        {isInitialLoading ? 'initial' : isLoading ? 'loading' : 'settled'}
      </p>
      <button type="button" onClick={refetch}>
        refetch
      </button>
    </div>
  );
}

describe('useApi', () => {
  it('reports initial loading, then the resolved data', async () => {
    const request = vi.fn().mockResolvedValue('patients:1');

    render(<Harness request={request} />);

    expect(screen.getByTestId('state')).toHaveTextContent('initial');
    await waitFor(() => expect(screen.getByTestId('data')).toHaveTextContent('patients:1'));
    expect(screen.getByTestId('state')).toHaveTextContent('settled');
  });

  it('refetches when the request identity changes', async () => {
    render(
      <Harness
        request={(signal) =>
          Promise.resolve(`${signal.aborted ? 'aborted' : 'first'}`)
        }
      />,
    );

    await waitFor(() => expect(screen.getByTestId('data')).toHaveTextContent('first'));

    // Changing the request prop simulates a filter change in a list screen.
    const second = () => Promise.resolve('second');
    render(<Harness request={second} />);

    await waitFor(() => expect(screen.getAllByTestId('data')[1]).toHaveTextContent('second'));
  });

  it('surfaces API errors and keeps the previous state out of the way', async () => {
    const request = vi.fn().mockRejectedValue(new ApiError(403, ['Forbidden'], 'Forbidden'));

    render(<Harness request={request} />);

    await waitFor(() => expect(screen.getByTestId('error')).toHaveTextContent('Forbidden'));
    expect(screen.getByTestId('data')).toHaveTextContent('none');
    expect(screen.getByTestId('state')).toHaveTextContent('settled');
  });

  it('marks a manual refetch as loading again', async () => {
    let resolveSecond: ((value: string) => void) | undefined;
    const request = vi
      .fn()
      .mockResolvedValueOnce('first')
      .mockImplementationOnce(() => new Promise<string>((resolve) => (resolveSecond = resolve)));

    const user = userEvent.setup();
    render(<Harness request={request} />);
    await waitFor(() => expect(screen.getByTestId('data')).toHaveTextContent('first'));

    await user.click(screen.getByRole('button', { name: 'refetch' }));

    // Still showing the old value would be fine, but the state must say loading.
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('loading'));

    resolveSecond?.('second');
    await waitFor(() => expect(screen.getByTestId('data')).toHaveTextContent('second'));
  });

  it('aborts the in-flight request when the component unmounts', async () => {
    let capturedSignal: AbortSignal | undefined;
    const request = (signal: AbortSignal) => {
      capturedSignal = signal;
      return new Promise<string>(() => undefined);
    };

    const { unmount } = render(<Harness request={request} />);
    unmount();

    await waitFor(() => expect(capturedSignal?.aborted).toBe(true));
  });
});
