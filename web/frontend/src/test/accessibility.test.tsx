/**
 * Accessibility checks.
 *
 * `axe-core` runs against the rendered markup for the screens that are always
 * reachable: the sign-in form and the application shell. Violations fail the
 * suite, so labels, roles and focus targets cannot silently regress.
 */
import { beforeAll, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import axe from 'axe-core';
import { LoginPage } from '@/features/auth/login-page';
import { AppShell } from '@/components/layout/app-shell';
import { AuthProvider } from '@/providers/auth-provider';
import { ThemeProvider } from '@/providers/theme-provider';

async function analyse(container: HTMLElement) {
  const results = await axe.run(container, {
    // Contrast cannot be evaluated in jsdom (no layout/paint), so it is
    // verified visually instead; everything else must still pass.
    rules: { 'color-contrast': { enabled: false } },
  });
  return results.violations.map((violation) => ({
    id: violation.id,
    help: violation.help,
    targets: violation.nodes.map((node) => node.target.join(' ')),
  }));
}

/** Sets a stored token so the shell treats the visitor as signed in. */
async function signInStub() {
  const { setToken } = await import('@/lib/api/token-store');
  setToken('test-token');
  const profile = {
    id: '11111111-1111-4111-8111-111111111111',
    username: 'admin',
    email: null,
    role: 'ADMIN',
    active: true,
    lastLoginAt: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  };
  globalThis.fetch = (() =>
    Promise.resolve(
      new Response(JSON.stringify(profile), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )) as typeof fetch;
}

describe('accessibility (axe-core)', () => {
  beforeAll(() => {
    // The shell renders media-query and dialog APIs that jsdom lacks.
    if (!window.matchMedia) {
      Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: (query: string) => ({
          matches: false,
          media: query,
          onchange: null,
          addEventListener: () => undefined,
          removeEventListener: () => undefined,
          dispatchEvent: () => false,
        }),
      });
    }
  });

  it('has no violations on the sign-in screen', async () => {
    const { container } = render(
      <MemoryRouter>
        <ThemeProvider>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>,
    );

    await screen.findByRole('button', { name: /sign in/i });
    expect(await analyse(container)).toEqual([]);
  });

  it('has no violations on the application shell', async () => {
    await signInStub();

    const { container } = render(
      <MemoryRouter initialEntries={['/']}>
        <ThemeProvider>
          <AuthProvider>
            <AppShell />
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>,
    );

    await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(await analyse(container)).toEqual([]);
  });
});
