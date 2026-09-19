import { useCallback } from 'react';
import { useDebouncedValue } from './use-debounced-value';
import { useQueryParams } from './use-query-params';

/** Filter state every list screen keeps in the URL. */
export type ListQueryState = {
  /** Free-text search term. */
  search: string;
  /** 1-based page number, stored as a string because URLs are text. */
  page: string;
  limit: string;
};

/**
 * URL-backed list state shared by every module.
 *
 * The query string is the single source of truth: typing in a search box writes
 * to the URL immediately (with `replace`, so the back button is not flooded),
 * while the *request* waits for the debounced value. Because there is no second
 * copy of the search text in component state, there is nothing to keep in sync.
 */
export function useListQuery<T extends ListQueryState & Record<string, string>>(defaults: T) {
  const { values, setValues } = useQueryParams(defaults);

  const defaultLimit = Number(defaults.limit) || 20;
  const debouncedSearch = useDebouncedValue(values.search, 350);

  const page = Math.max(1, Number(values.page) || 1);
  const limit = Math.min(100, Math.max(1, Number(values.limit) || defaultLimit));

  const setPage = useCallback(
    (next: number) => {
      setValues({ page: next <= 1 ? '' : String(next) } as Partial<T>);
    },
    [setValues],
  );

  const setFilter = useCallback(
    (key: keyof T & string, value: string) => {
      // Any filter change returns to the first page.
      setValues({ [key]: value, page: '' } as Partial<T>);
    },
    [setValues],
  );

  const setSearch = useCallback(
    (value: string) => {
      setValues({ search: value, page: '' } as unknown as Partial<T>, { replace: true });
    },
    [setValues],
  );

  return {
    /** Current filter values as they appear in the URL. */
    values,
    /** Search term as typed (drives the input). */
    search: values.search,
    /** Search term after debouncing (drives the request). */
    debouncedSearch,
    page,
    limit,
    setPage,
    setFilter,
    setSearch,
    /** True while the user is typing but the request has not caught up. */
    isSearchPending: values.search !== debouncedSearch,
  };
}
