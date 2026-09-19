import { createContext, useContext } from 'react';
import type { LoginPayload } from '@/lib/api/auth';
import type { Role, User } from '@/lib/api/types';

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

export interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  /** True while a login request is in flight. */
  isSigningIn: boolean;
  login: (credentials: LoginPayload) => Promise<User>;
  logout: () => void;
  /** True when the current user holds one of the given roles. */
  hasRole: (...roles: Role[]) => boolean;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

/** Access the current session. Must be used inside `AuthProvider`. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return context;
}
