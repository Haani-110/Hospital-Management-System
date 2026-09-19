import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, buildQuery, request, setUnauthorizedHandler } from './client';
import { getToken, setToken } from './token-store';

/** Minimal fetch stand-in returning a JSON response. */
function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('buildQuery', () => {
  it('omits empty values and serialises the rest', () => {
    expect(
      buildQuery({ page: 1, limit: 20, search: '', patientId: null, active: false }),
    ).toBe('?page=1&limit=20&active=false');
  });

  it('returns an empty string without parameters', () => {
    expect(buildQuery()).toBe('');
    expect(buildQuery({})).toBe('');
  });
});

describe('request', () => {
  afterEach(() => {
    setUnauthorizedHandler(null);
  });

  it('sends the bearer token and parses JSON', async () => {
    setToken('test-token');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 'p1' }));
    vi.stubGlobal('fetch', fetchMock);

    const result = await request<{ id: string }>('/patients/p1');

    expect(result).toEqual({ id: 'p1' });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/patients/p1');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer test-token');
  });

  it('skips the token on public endpoints', async () => {
    setToken('test-token');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ accessToken: 'a' }));
    vi.stubGlobal('fetch', fetchMock);

    await request('/auth/login', { method: 'POST', body: {}, authenticated: false });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it('maps validation messages from the error envelope', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            statusCode: 400,
            error: 'Bad Request',
            message: ['page must not be less than 1', 'limit is too large'],
            path: '/api/v1/patients',
            timestamp: '2026-09-19T00:00:00.000Z',
          },
          400,
        ),
      ),
    );

    const error = await request('/patients').catch((cause: unknown) => cause);

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.messages).toEqual(['page must not be less than 1', 'limit is too large']);
    expect(apiError.isValidationError).toBe(true);
  });

  it('clears the token and notifies the app on 401', async () => {
    setToken('expired-token');
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          { statusCode: 401, error: 'Unauthorized', message: 'Invalid or expired access token' },
          401,
        ),
      ),
    );

    const error = (await request('/auth/me').catch((cause: unknown) => cause)) as ApiError;

    expect(error.isUnauthorized).toBe(true);
    expect(error.messages).toEqual(['Invalid or expired access token']);
    expect(getToken()).toBeNull();
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('reports 403 without dropping the session', async () => {
    setToken('doctor-token');
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            statusCode: 403,
            error: 'Forbidden',
            message: 'Your role is not allowed to perform this action',
          },
          403,
        ),
      ),
    );

    const error = (await request('/bills').catch((cause: unknown) => cause)) as ApiError;

    expect(error.isForbidden).toBe(true);
    expect(getToken()).toBe('doctor-token');
  });

  it('turns a transport failure into a network error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    const error = (await request('/patients').catch((cause: unknown) => cause)) as ApiError;

    expect(error.isNetworkError).toBe(true);
    expect(error.messages[0]).toMatch(/unable to reach the server/i);
  });
});
