import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ApiError, setUnauthorizedHandler } from '@/lib/api/client';
import { authApi } from '@/lib/api';
import type { LoginPayload } from '@/lib/api/auth';
import type { Role, User } from '@/lib/api/types';
import { clearToken, getToken, setToken } from '@/lib/api/token-store';
import { AuthContext, type AuthContextValue, type AuthStatus } from './auth-context';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>(() => (getToken() ? 'loading' : 'unauthenticated'));
  const [user, setUser] = useState<User | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);
  const mountedRef = useRef(true);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    setStatus('unauthenticated');
  }, []);

  // A 401 anywhere in the app means the token is no longer usable.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearToken();
      setUser(null);
      setStatus('unauthenticated');
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  // Restore the session on boot: a stored token is only trusted after the API
  // confirms it by returning the profile. When no token is stored the initial
  // state is already `unauthenticated`, so nothing has to be set here.
  useEffect(() => {
    mountedRef.current = true;
    const token = getToken();
    if (!token) return;

    const controller = new AbortController();
    authApi
      .getCurrentUser(controller.signal)
      .then((profile) => {
        if (!mountedRef.current) return;
        setUser(profile);
        setStatus('authenticated');
      })
      .catch((error: unknown) => {
        if (!mountedRef.current || controller.signal.aborted) return;
        if (error instanceof ApiError && error.isNetworkError) {
          // Keep the token: the API may simply be starting up.
          setStatus('authenticated');
          return;
        }
        clearToken();
        setUser(null);
        setStatus('unauthenticated');
      });

    return () => {
      mountedRef.current = false;
      controller.abort();
    };
  }, []);

  const login = useCallback(async (credentials: LoginPayload) => {
    setIsSigningIn(true);
    try {
      const response = await authApi.login(credentials);
      setToken(response.accessToken);

      // Confirm the fresh token against the API (`GET /auth/me`) before any
      // protected screen renders. A session the API rejects has to fail here,
      // where the form can explain it, instead of showing the dashboard for a
      // moment and bouncing straight back to this screen.
      let profile = response.user;
      try {
        profile = await authApi.getCurrentUser();
      } catch (error) {
        if (!(error instanceof ApiError && error.isNetworkError)) {
          clearToken();
          setUser(null);
          setStatus('unauthenticated');
          if (error instanceof ApiError && error.isUnauthorized) {
            throw new ApiError(401, [
              'Signed in, but the API did not accept the session. Please try again.',
            ]);
          }
          throw error;
        }
        // Unreachable right now: keep the session. The login response already
        // proved the credentials and carried the profile.
      }

      setUser(profile);
      setStatus('authenticated');
      return profile;
    } finally {
      setIsSigningIn(false);
    }
  }, []);

  const hasRole = useCallback(
    (...roles: Role[]) => (user ? roles.includes(user.role) : false),
    [user],
  );

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, isSigningIn, login, logout, hasRole }),
    [status, user, isSigningIn, login, logout, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
