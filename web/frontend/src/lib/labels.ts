import type { Role } from './api/types';

const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrator',
  DOCTOR: 'Doctor',
  RECEPTIONIST: 'Receptionist',
};

/** Human label for a role value, e.g. `ADMIN` → `Administrator`. */
export function roleLabel(role: Role): string {
  return roleLabels[role];
}
