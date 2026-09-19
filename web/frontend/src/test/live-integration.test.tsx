/**
 * Live integration test — renders the real application against a running API.
 *
 * Opt-in, exactly like the backend's database e2e suite:
 *
 *   1. start the backend on http://localhost:4000 (see web/backend/README.md)
 *   2. seed it (`npm run db:seed`) and note the usernames/passwords it prints
 *   3. run the suite with those credentials in the environment:
 *
 *        RUN_LIVE_TESTS=true \
 *        LIVE_ADMIN_USERNAME=admin LIVE_ADMIN_PASSWORD=... \
 *        LIVE_DOCTOR_USERNAME=doctor LIVE_DOCTOR_PASSWORD=... \
 *        npm test
 *
 * Nothing here is mocked: the components call the NestJS API, which queries
 * PostgreSQL. No password is stored in this repository — they are read from the
 * environment, and the suite is skipped when they are absent.
 */
import { beforeAll, describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from '@/App';
import { AuthProvider } from '@/providers/auth-provider';
import { ThemeProvider } from '@/providers/theme-provider';
import { authApi, reportsApi } from '@/lib/api';
import { setToken } from '@/lib/api/token-store';
import type { LoginResponse } from '@/lib/api/types';

const apiBaseUrl = process.env.VITE_API_BASE_URL ?? 'http://localhost:4000/api/v1';

/** Seed usernames are public defaults; passwords must come from the environment. */
const accounts = {
  admin: {
    username: process.env.LIVE_ADMIN_USERNAME ?? '',
    password: process.env.LIVE_ADMIN_PASSWORD ?? '',
  },
  doctor: {
    username: process.env.LIVE_DOCTOR_USERNAME ?? '',
    password: process.env.LIVE_DOCTOR_PASSWORD ?? '',
  },
};

const hasCredentials =
  Boolean(accounts.admin.username) &&
  Boolean(accounts.admin.password) &&
  Boolean(accounts.doctor.username) &&
  Boolean(accounts.doctor.password);

const LIVE = process.env.RUN_LIVE_TESTS === 'true' && hasCredentials;

async function signIn(account: { username: string; password: string }): Promise<LoginResponse> {
  const response = await authApi.login(account);
  setToken(response.accessToken);
  return response;
}

function renderApp(route: string) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

const describeLive = LIVE ? describe : describe.skip;

describeLive(`live API integration (${apiBaseUrl})`, () => {
  beforeAll(async () => {
    // Fail fast with a clear message when the API is not running.
    const health = await reportsApi.getHealth().catch(() => null);
    if (!health) {
      throw new Error(
        `The API at ${apiBaseUrl} is not reachable. Start the backend before running live tests.`,
      );
    }
  });

  it('signs in as an administrator and shows real dashboard totals', async () => {
    const session = await signIn(accounts.admin);
    const summary = await reportsApi.getDashboardSummary();

    renderApp('/');

    expect(
      await screen.findByRole('heading', { name: `Welcome back, ${session.user.username}` }),
    ).toBeInTheDocument();

    // The rendered patient total is the value the API just returned.
    await waitFor(() => {
      expect(
        screen.getAllByText(new RegExp(`^${summary.totalPatients.toLocaleString()}$`)).length,
      ).toBeGreaterThan(0);
    });

    // Today's revenue tile shows the decimal string the API sent, formatted.
    const revenueDigits = Number(summary.todayRevenue).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    await waitFor(() => {
      expect(screen.getAllByText(new RegExp(revenueDigits.replace('.', '\\.'))).length).toBeGreaterThan(0);
    });
  });

  it('renders the seeded patient from the database', async () => {
    await signIn(accounts.admin);
    renderApp('/patients');

    expect(await screen.findByRole('heading', { name: 'Patients' })).toBeInTheDocument();
    expect(await screen.findByText(/PAT-\d{6}/)).toBeInTheDocument();
  });

  it('explains the 403 the API returns for a doctor opening billing', async () => {
    const session = await signIn(accounts.doctor);
    expect(session.user.role).toBe('DOCTOR');

    renderApp('/billing');

    expect(
      await screen.findByText(/you do not have permission to view this/i),
    ).toBeInTheDocument();

    // The sidebar hides the restricted link for this role.
    expect(screen.queryByRole('link', { name: 'Billing' })).not.toBeInTheDocument();
  });

  it('shows an honest foundation screen for a module without endpoints', async () => {
    await signIn(accounts.admin);
    renderApp('/laboratory');

    expect(await screen.findByRole('heading', { name: 'Laboratory' })).toBeInTheDocument();
    expect(screen.getByText(/no backend api yet/i)).toBeInTheDocument();
  });
});
