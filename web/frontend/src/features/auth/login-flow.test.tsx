import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/providers/auth-provider';
import { ThemeProvider } from '@/providers/theme-provider';
import { LoginPage } from './login-page';
import { getToken } from '@/lib/api/token-store';
import type { User } from '@/lib/api/types';

const adminUser: User = {
  id: '11111111-1111-4111-8111-111111111111',
  username: 'admin',
  email: 'admin@hospital.test',
  role: 'ADMIN',
  active: true,
  lastLoginAt: null,
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-01T00:00:00.000Z',
};

function requestUrl(input: RequestInfo | URL): string {
  return typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <ThemeProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<h1>Dashboard</h1>} />
          </Routes>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

describe('login', () => {
  it('validates the credentials before calling the API', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderLogin();
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Enter your username.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('shows the API message for invalid credentials', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          { statusCode: 401, error: 'Unauthorized', message: 'Invalid username or password' },
          401,
        ),
      ),
    );
    const user = userEvent.setup();

    renderLogin();
    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/^password/i), 'wrong-password');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password');
    expect(getToken()).toBeNull();
  });

  it('stores the token, confirms the session and redirects after a successful sign-in', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = requestUrl(input);
      if (url.endsWith('/auth/login')) {
        return Promise.resolve(
          jsonResponse({
            accessToken: 'jwt-token',
            tokenType: 'Bearer',
            expiresIn: '15m',
            user: adminUser,
          }),
        );
      }
      if (url.endsWith('/auth/me')) return Promise.resolve(jsonResponse(adminUser));
      return Promise.resolve(jsonResponse({ message: 'Not found' }, 404));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderLogin();
    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/^password/i), 'correct-password');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(getToken()).toBe('jwt-token');
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(fetchMock.mock.calls.map(([input]) => requestUrl(input))).toEqual([
      expect.stringContaining('/auth/login'),
      expect.stringContaining('/auth/me'),
    ]);
  });

  it('reports a session the API rejects instead of bouncing to the login screen', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = requestUrl(input);
      if (url.endsWith('/auth/login')) {
        return Promise.resolve(
          jsonResponse({
            accessToken: 'jwt-token',
            tokenType: 'Bearer',
            expiresIn: '15m',
            user: adminUser,
          }),
        );
      }
      return Promise.resolve(jsonResponse({ statusCode: 401, message: 'Unauthorized' }, 401));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderLogin();
    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/^password/i), 'correct-password');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/did not accept the session/i);
    expect(getToken()).toBeNull();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('toggles password visibility accessibly', async () => {
    const user = userEvent.setup();
    renderLogin();

    const password = screen.getByLabelText(/^password/i);
    expect(password).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: /show password/i }));

    expect(screen.getByLabelText(/^password/i)).toHaveAttribute('type', 'text');
  });
});
