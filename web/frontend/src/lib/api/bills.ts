import { api } from './client';
import type { Bill, BillsQuery, Paginated } from './types';

/**
 * `GET /bills` — includes bill items.
 * Restricted to ADMIN and RECEPTIONIST by the backend (DOCTOR receives 403).
 */
export function listBills(query: BillsQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<Bill>>('/bills', { query, signal });
}

/** `GET /bills/:id` — includes bill items. */
export function getBill(id: string, signal?: AbortSignal) {
  return api.get<Bill>(`/bills/${id}`, { signal });
}
