import { api } from './client';
import type { Paginated, User, UsersQuery } from './types';

/** `GET /users` — ADMIN only; the backend answers 403 for other roles. */
export function listUsers(query: UsersQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<User>>('/users', { query, signal });
}

/** `GET /users/:id` — ADMIN only. */
export function getUser(id: string, signal?: AbortSignal) {
  return api.get<User>(`/users/${id}`, { signal });
}
