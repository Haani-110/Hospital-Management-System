import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * Small wrapper around `useSearchParams` so list screens keep their filters,
 * search text and page number in the URL (shareable and back-button friendly).
 */
export function useQueryParams<T extends Record<string, string>>(defaults: T) {
  const [searchParams, setSearchParams] = useSearchParams();

  const values = useMemo(() => {
    const result = { ...defaults };
    for (const key of Object.keys(defaults)) {
      const current = searchParams.get(key);
      if (current !== null && current !== '') {
        result[key as keyof T] = current as T[keyof T];
      }
    }
    return result;
  }, [searchParams, defaults]);

  const setValues = useCallback(
    (updates: Partial<Record<keyof T, string>>, options: { replace?: boolean } = {}) => {
      setSearchParams(
        (previous) => {
          const next = new URLSearchParams(previous);
          for (const [key, value] of Object.entries(updates)) {
            if (!value || value === defaults[key as keyof T]) {
              next.delete(key);
            } else {
              next.set(key, value);
            }
          }
          return next;
        },
        { replace: options.replace ?? false },
      );
    },
    [setSearchParams, defaults],
  );

  const reset = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true });
  }, [setSearchParams]);

  return { values, setValues, reset, searchParams };
}
