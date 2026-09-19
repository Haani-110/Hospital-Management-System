import { api } from './client';
import type { Paginated, Patient, PatientsQuery } from './types';

/** `GET /patients` — paginated, searchable list. */
export function listPatients(query: PatientsQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<Patient>>('/patients', { query, signal });
}

/** `GET /patients/:id`. */
export function getPatient(id: string, signal?: AbortSignal) {
  return api.get<Patient>(`/patients/${id}`, { signal });
}
