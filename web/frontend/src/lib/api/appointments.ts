import { api } from './client';
import type { Appointment, AppointmentsQuery, Paginated } from './types';

/** `GET /appointments` — supports patientId, doctorId, status and date range. */
export function listAppointments(query: AppointmentsQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<Appointment>>('/appointments', { query, signal });
}

/** `GET /appointments/:id`. */
export function getAppointment(id: string, signal?: AbortSignal) {
  return api.get<Appointment>(`/appointments/${id}`, { signal });
}
