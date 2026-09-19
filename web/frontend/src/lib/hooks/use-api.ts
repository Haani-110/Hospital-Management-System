import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/client';

export interface ApiState<T> {
  data: T | null;
  error: ApiError | null;
  /** True while a request is in flight (including refetches). */
  isLoading: boolean;
  /** True only for the very first load, so skeletons don't flash on refetch. */
  isInitialLoading: boolean;
  refetch: () => void;
}

type Request<T> = (signal: AbortSignal) => Promise<T>;

interface Settled<T> {
  /** The exact request function that produced this result. */
  request: Request<T>;
  /** Refresh generation, so a manual refetch reads as loading again. */
  nonce: number;
  data: T | null;
  error: ApiError | null;
}

function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (error instanceof Error) return new ApiError(0, [error.message], 'Error', error);
  return new ApiError(0, ['Something went wrong.'], 'Error', error);
}

/**
 * Runs an API call and tracks loading/error/data state.
 *
 * `request` must be stable (wrap it in `useCallback`) — its identity is what
 * tells the hook that the filters changed and a new request is needed. Loading
 * is *derived* by comparing the current request with the last settled one, so
 * the effect never sets state synchronously and nothing is kept in a ref.
 */
export function useApi<T>(request: Request<T>, options: { enabled?: boolean } = {}): ApiState<T> {
  const { enabled = true } = options;

  const [nonce, setNonce] = useState(0);
  const [settled, setSettled] = useState<Settled<T> | null>(null);

  useEffect(() => {
    if (!enabled) return;

    const controller = new AbortController();
    let active = true;

    request(controller.signal)
      .then((result) => {
        if (!active) return;
        setSettled({ request, nonce, data: result, error: null });
      })
      .catch((cause: unknown) => {
        if (!active || controller.signal.aborted) return;
        setSettled({ request, nonce, data: null, error: toApiError(cause) });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [request, enabled, nonce]);

  const fresh = settled !== null && settled.request === request && settled.nonce === nonce;
  const isLoading = enabled && !fresh;

  const refetch = useCallback(() => setNonce((value) => value + 1), []);

  return {
    data: fresh ? settled.data : null,
    error: fresh ? settled.error : null,
    isLoading,
    isInitialLoading: isLoading && settled === null,
    refetch,
  };
}
