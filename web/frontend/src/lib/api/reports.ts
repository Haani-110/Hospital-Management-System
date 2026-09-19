import { api } from './client';
import type { DashboardSummary, HealthStatus } from './types';

/** `GET /reports/dashboard-summary` — real counters computed in PostgreSQL. */
export function getDashboardSummary(signal?: AbortSignal) {
  return api.get<DashboardSummary>('/reports/dashboard-summary', { signal });
}

/** `GET /health` — public liveness probe. */
export function getHealth(signal?: AbortSignal) {
  return api.get<HealthStatus>('/health', { authenticated: false, signal });
}
