import { useCallback, useSyncExternalStore } from 'react';

/**
 * Subscribes to a CSS media query, e.g. `(min-width: 1024px)`.
 *
 * `useSyncExternalStore` is used instead of state plus an effect so the value
 * is always read from the browser's own media-query state (no flash of the
 * wrong layout on hydration, no cascading render).
 */
export function useMediaQuery(query: string): boolean {
  const subscribe = useCallback(
    (onChange: () => void) => {
      const mediaQuery = window.matchMedia(query);
      mediaQuery.addEventListener('change', onChange);
      return () => mediaQuery.removeEventListener('change', onChange);
    },
    [query],
  );

  const getSnapshot = useCallback(() => window.matchMedia(query).matches, [query]);

  return useSyncExternalStore(subscribe, getSnapshot, () => false);
}

/** True from the `lg` breakpoint up — the sidebar becomes persistent there. */
export function useIsDesktop(): boolean {
  return useMediaQuery('(min-width: 1024px)');
}
