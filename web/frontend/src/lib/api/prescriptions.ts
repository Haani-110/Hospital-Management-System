import { api } from './client';
import type { Paginated, Prescription, PrescriptionsQuery } from './types';

/** `GET /prescriptions` — includes prescription items. */
export function listPrescriptions(query: PrescriptionsQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<Prescription>>('/prescriptions', { query, signal });
}

/** `GET /prescriptions/:id` — includes prescription items. */
export function getPrescription(id: string, signal?: AbortSignal) {
  return api.get<Prescription>(`/prescriptions/${id}`, { signal });
}
