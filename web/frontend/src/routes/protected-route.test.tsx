import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/providers/auth-provider';
import { ProtectedRoute } from './protected-route';
import { setToken } from '@/lib/api/token-store';
import type { User } from '@/lib/api/types';

const doctor: User = {
  id: '22222222-2222-4222-8222-222222222222',
  username: 'dr.house',
  email: null,
  role: 'DOCTOR',
  active: true,
  lastLoginAt: null,
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-01T00:00:00.000Z',
};

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={['/patients']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<h1>Sign in</h1>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/patients" element={<h1>Patients</h1>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  it('redirects to the login screen when no token is stored', async () => {
    vi.stubGlobal('fetch', vi.fn());
    renderProtected();
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('renders the protected screen when the stored token is valid', async () => {
    setToken('valid-token');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify(doctor), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ));

    renderProtected();
    expect(await screen.findByRole('heading', { name: 'Patients' })).toBeInTheDocument();
  });

  it('drops an expired token and returns to login', async () => {
    setToken('expired-token');
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            statusCode: 401,
            error: 'Unauthorized',
            message: 'Invalid or expired access token',
          }),
          { status: 401, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    renderProtected();
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });
});
