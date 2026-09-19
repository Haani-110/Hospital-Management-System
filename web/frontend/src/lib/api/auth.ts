import { api } from './client';
import type { LoginResponse, User } from './types';

export interface LoginPayload {
  /** Backend rule: 3–50 characters. */
  username: string;
  /** Backend rule: 8–128 characters. */
  password: string;
}

/** `POST /auth/login` — public. */
export function login(payload: LoginPayload, signal?: AbortSignal): Promise<LoginResponse> {
  return api.post<LoginResponse>('/auth/login', payload, { authenticated: false, signal });
}

/** `GET /auth/me` — profile of the authenticated user. */
export function getCurrentUser(signal?: AbortSignal): Promise<User> {
  return api.get<User>('/auth/me', { signal });
}
