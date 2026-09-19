import { api } from './client';
import type { Doctor, DoctorsQuery, Paginated } from './types';

/** `GET /doctors` — paginated list with department, search and active filters. */
export function listDoctors(query: DoctorsQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<Doctor>>('/doctors', { query, signal });
}

/** `GET /doctors/:id`. */
export function getDoctor(id: string, signal?: AbortSignal) {
  return api.get<Doctor>(`/doctors/${id}`, { signal });
}
