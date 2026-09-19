import { api } from './client';
import type { MedicalRecord, MedicalRecordsQuery, Paginated } from './types';

/** `GET /medical-records` — supports patientId, doctorId, diagnosis search and dates. */
export function listMedicalRecords(query: MedicalRecordsQuery = {}, signal?: AbortSignal) {
  return api.get<Paginated<MedicalRecord>>('/medical-records', { query, signal });
}

/** `GET /medical-records/:id`. */
export function getMedicalRecord(id: string, signal?: AbortSignal) {
  return api.get<MedicalRecord>(`/medical-records/${id}`, { signal });
}
