import { Badge } from '@/components/ui/badge';
import type { BadgeTone } from '@/components/ui/badge';
import type { AppointmentStatus, BillStatus, Role } from '@/lib/api/types';
import { roleLabel } from '@/lib/labels';

const appointmentTones: Record<AppointmentStatus, BadgeTone> = {
  SCHEDULED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'neutral',
};

const appointmentLabels: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Scheduled',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

/** Appointment status — label plus tone, never colour alone. */
export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  return (
    <Badge tone={appointmentTones[status]} withDot>
      {appointmentLabels[status]}
    </Badge>
  );
}

const billTones: Record<BillStatus, BadgeTone> = {
  UNPAID: 'warning',
  PARTIALLY_PAID: 'info',
  PAID: 'success',
  CANCELLED: 'neutral',
};

const billLabels: Record<BillStatus, string> = {
  UNPAID: 'Unpaid',
  PARTIALLY_PAID: 'Partially paid',
  PAID: 'Paid',
  CANCELLED: 'Cancelled',
};

/** Bill status — label plus tone. */
export function BillStatusBadge({ status }: { status: BillStatus }) {
  return (
    <Badge tone={billTones[status]} withDot>
      {billLabels[status]}
    </Badge>
  );
}

const roleTones: Record<Role, BadgeTone> = {
  ADMIN: 'accent',
  DOCTOR: 'info',
  RECEPTIONIST: 'neutral',
};

export function RoleBadge({ role }: { role: Role }) {
  return <Badge tone={roleTones[role]}>{roleLabel(role)}</Badge>;
}

/** Active / inactive record state. */
export function ActiveBadge({ active }: { active: boolean }) {
  return (
    <Badge tone={active ? 'success' : 'neutral'} withDot>
      {active ? 'Active' : 'Inactive'}
    </Badge>
  );
}
