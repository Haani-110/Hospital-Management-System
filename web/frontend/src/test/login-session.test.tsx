import { describe, expect, it } from 'vitest';
import { StrictMode } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { App } from '@/App';
import { AuthProvider } from '@/providers/auth-provider';
import { ThemeProvider } from '@/providers/theme-provider';

declare global {
  /**
   * Vitest runs in Node, so `process.env` is available for the opt-in live
   * flags without pulling Node's type definitions into browser code.
   */
  var process: { env: Record<string, string | undefined> };
}

const account = {
  username: process.env.LIVE_ADMIN_USERNAME ?? '',
  password: process.env.LIVE_ADMIN_PASSWORD ?? '',
};

const LIVE = process.env.RUN_LIVE_TESTS === 'true' && Boolean(account.username && account.password);
const describeLive = LIVE ? describe : describe.skip;

interface Call {
  method: string;
  path: string;
  status: number;
  sentAuth: boolean;
}

/** Records every request the app makes so the session flow can be asserted. */
function recordRequests(calls: Call[]) {
  const original = globalThis.fetch;
  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    const headers = (init?.headers ?? {}) as Record<string, string>;
    const response = await original(input, init);
    calls.push({
      method: init?.method ?? 'GET',
      path: url.replace(/^https?:\/\/[^/]+/, ''),
      status: response.status,
      sentAuth: Boolean(headers.Authorization),
    });
    return response;
  };
  return () => {
    globalThis.fetch = original;
  };
}

function renderApp() {
  return render(
    <StrictMode>
      <MemoryRouter initialEntries={['/']}>
        <ThemeProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    </StrictMode>,
  );
}

async function signIn() {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/username/i), account.username);
  await user.type(await screen.findByLabelText(/^password/i), account.password);
  await user.click(screen.getByRole('button', { name: /sign in/i }));
  return user;
}

async function settle(milliseconds = 2_500) {
  // Let any late response (a stray 401 redirecting to /login) happen before the
  // assertions, without tripping React's act() warning.
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, milliseconds));
  });
}

describeLive(`login session stays valid (${process.env.VITE_API_BASE_URL ?? 'http://localhost:4000/api/v1'})`, () => {
  it('a successful login must not immediately log the user out', async () => {
    window.sessionStorage.clear();
    const calls: Call[] = [];
    const restore = recordRequests(calls);

    try {
      renderApp();
      await signIn();

      await expect(screen.findByRole('heading', { name: /welcome back/i })).resolves.toBeTruthy();
      await settle();

      const token = window.sessionStorage.getItem('hms.auth.token');
      const login = calls.find((call) => call.path.endsWith('/auth/login'));
      const me = calls.find((call) => call.path.endsWith('/auth/me'));
      const dashboard = calls.find((call) => call.path.endsWith('/reports/dashboard-summary'));

      // The full documented sequence: login, the token confirmed by the API, the
      // dashboard served with that same token — and no 401 anywhere in between.
      expect(login).toMatchObject({ status: 200 });
      expect(token).toBeTruthy();
      expect(me).toMatchObject({ status: 200, sentAuth: true });
      expect(dashboard).toMatchObject({ status: 200, sentAuth: true });
      expect(calls.filter((call) => call.status === 401)).toEqual([]);

      // Still signed in after everything settled: no late redirect to /login.
      expect(screen.queryByRole('heading', { name: /welcome back/i })).not.toBeNull();
      expect(screen.queryByRole('button', { name: /sign in/i })).toBeNull();
    } finally {
      restore();
    }
  }, 40_000);

  it('a page load with the stored token restores the session without signing in again', async () => {
    // A reload starts the app from scratch with the token the previous page kept
    // in sessionStorage: it has to confirm the session through GET /auth/me and
    // land on the dashboard without asking for credentials.
    window.sessionStorage.clear();
    const loginCalls: Call[] = [];
    const restore = recordRequests(loginCalls);

    try {
      const first = renderApp();
      await signIn();
      await screen.findByRole('heading', { name: /welcome back/i });
      await settle();

      expect(window.sessionStorage.getItem('hms.auth.token')).toBeTruthy();

      first.unmount();
      loginCalls.length = 0;

      const reloadCalls: Call[] = [];
      const restoreReload = recordRequests(reloadCalls);
      try {
        renderApp();

        await expect(screen.findByRole('heading', { name: /welcome back/i })).resolves.toBeTruthy();
        await settle();

        expect(reloadCalls.some((call) => call.path.endsWith('/auth/login'))).toBe(false);
        expect(reloadCalls.find((call) => call.path.endsWith('/auth/me'))).toMatchObject({
          status: 200,
          sentAuth: true,
        });
        expect(reloadCalls.filter((call) => call.status === 401)).toEqual([]);
        expect(screen.queryByRole('button', { name: /sign in/i })).toBeNull();
      } finally {
        restoreReload();
      }
    } finally {
      restore();
    }
  }, 40_000);
});
