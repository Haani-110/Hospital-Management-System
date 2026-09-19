import type { ApiErrorBody } from './types';
import { clearToken, getToken } from './token-store';

/** Base URL of the backend API, e.g. `http://localhost:4000/api/v1`. */
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:4000/api/v1'
).replace(/\/+$/, '');

/**
 * Error thrown for every failed request.
 *
 * `status === 0` means the request never reached the API (offline, DNS, CORS).
 * Validation failures from the global ValidationPipe arrive as `messages`.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly messages: string[];
  readonly error: string;

  constructor(status: number, messages: string[], error?: string, cause?: unknown) {
    super(messages[0] ?? 'Request failed');
    this.name = 'ApiError';
    this.status = status;
    this.messages = messages;
    this.error = error ?? 'Error';
    if (cause !== undefined) {
      this.cause = cause;
    }
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }

  /** True when the backend rejected the payload (ValidationPipe / DTO rules). */
  get isValidationError(): boolean {
    return this.status === 400 || this.status === 422;
  }
}

/** Called whenever the API answers 401, so the app can drop the session. */
type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler;
}

export type QueryValue = string | number | boolean | undefined | null;

/** Serialises a query object, dropping empty values (`page=1&limit=20`). */
export function buildQuery(params?: Record<string, QueryValue>): string {
  if (!params) return '';
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue;
    search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : '';
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  /** Query parameters; empty strings/null/undefined are omitted. */
  query?: Record<string, QueryValue>;
  body?: unknown;
  signal?: AbortSignal;
  /** Set to false for public endpoints (login) to skip the bearer header. */
  authenticated?: boolean;
}

async function parseErrorBody(response: Response): Promise<ApiError> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    body = null;
  }

  const parsed = body as Partial<ApiErrorBody> | null;
  const rawMessage = parsed?.message;
  const messages = Array.isArray(rawMessage)
    ? rawMessage
    : typeof rawMessage === 'string'
      ? [rawMessage]
      : [response.statusText || 'Request failed'];

  return new ApiError(response.status, messages, parsed?.error);
}

/**
 * Single entry point for API calls: resolves the URL, attaches the bearer
 * token, decodes JSON, and normalises every failure into an `ApiError`.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', query, body, signal, authenticated = true } = options;
  const url = `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}${buildQuery(query)}`;

  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  if (authenticated) {
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
      credentials: 'omit',
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw new ApiError(
      0,
      ['Unable to reach the server. Check that the API is running and try again.'],
      'Network Error',
      error,
    );
  }

  if (response.status === 401) {
    // The token is missing, expired or invalid: drop it and let the app react.
    clearToken();
    onUnauthorized?.();
  }

  if (!response.ok) {
    throw await parseErrorBody(response);
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'GET' }),

  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'POST', body }),

  patch: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'PATCH', body }),

  put: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'PUT', body }),

  delete: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'DELETE' }),
};
