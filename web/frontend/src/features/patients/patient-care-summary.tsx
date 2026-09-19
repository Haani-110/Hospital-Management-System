import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { CalendarDays, ClipboardList, FileText, Receipt } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert } from '@/components/ui/alert';
import { buttonStyles } from '@/components/ui/button-styles';
import {  } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { AppointmentStatusBadge } from '@/components/data/status-badge';
import { appointmentsApi, billsApi, medicalRecordsApi, prescriptionsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { formatDateTime } from '@/lib/utils';

interface PatientCareSummaryProps {
  patientId: string;
  canViewBilling: boolean;
}

/**
 * Counts of this patient's clinical activity.
 *
 * The totals are the `meta.total` values returned by the paginated list
 * endpoints, requested with `limit=1` — real counts, not estimates. A recent
 * appointments timeline is shown alongside.
 */
export function PatientCareSummary({ patientId, canViewBilling }: PatientCareSummaryProps) {
  const appointments = useApi(
    useCallback(
      (signal) => appointmentsApi.listAppointments({ patientId, limit: 5, page: 1 }, signal),
      [patientId],
    ),
  );

  const records = useApi(
    useCallback(
      (signal) => medicalRecordsApi.listMedicalRecords({ patientId, limit: 1, page: 1 }, signal),
      [patientId],
    ),
  );

  const prescriptions = useApi(
    useCallback(
      (signal) => prescriptionsApi.listPrescriptions({ patientId, limit: 1, page: 1 }, signal),
      [patientId],
    ),
  );

  const bills = useApi(
    useCallback(
      (signal) => billsApi.listBills({ patientId, limit: 1, page: 1 }, signal),
      [patientId],
    ),
    { enabled: canViewBilling },
  );

  const isLoading =
    appointments.isInitialLoading ||
    records.isInitialLoading ||
    prescriptions.isInitialLoading ||
    (canViewBilling && bills.isInitialLoading);

  const metrics = [
    {
      label: 'Appointments',
      value: appointments.data?.meta.total,
      href: `/appointments?patientId=${patientId}`,
      icon: CalendarDays,
    },
    {
      label: 'Medical records',
      value: records.data?.meta.total,
      href: `/medical-records?patientId=${patientId}`,
      icon: FileText,
    },
    {
      label: 'Prescriptions',
      value: prescriptions.data?.meta.total,
      href: `/prescriptions?patientId=${patientId}`,
      icon: ClipboardList,
    },
    ...(canViewBilling
      ? [
          {
            label: 'Bills',
            value: bills.data?.meta.total,
            href: `/billing?patientId=${patientId}`,
            icon: Receipt,
          },
        ]
      : []),
  ];

  const recentAppointments = appointments.data?.data ?? [];

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Care summary</CardTitle>
            <CardDescription>What exists for this patient across the system.</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {Array.from({ length: canViewBilling ? 4 : 3 }).map((_, index) => (
                <Skeleton key={index} className="h-20 rounded-md" />
              ))}
            </div>
          ) : (
            <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {metrics.map((metric) => (
                <li key={metric.label}>
                  <Link
                    to={metric.href}
                    className="block rounded-md border border-border bg-elevated/50 p-3.5 transition-colors hover:border-border-strong focus-visible:border-border-strong"
                  >
                    <metric.icon aria-hidden className="size-4 text-subtle" />
                    <p className="mt-2.5 text-xl font-semibold text-fg tabular">
                      {metric.value ?? '—'}
                    </p>
                    <p className="mt-0.5 text-[11px] text-subtle">{metric.label}</p>
                  </Link>
                </li>
              ))}
            </ul>
          )}

          {!canViewBilling ? (
            <Alert tone="info" className="mt-4">
              Billing totals are not available to clinical accounts — the API restricts bills to
              administrators and reception staff.
            </Alert>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Recent appointments</CardTitle>
            <CardDescription>The five most recent visits for this patient.</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          {appointments.isInitialLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 3 }).map((_, index) => (
                <Skeleton key={index} className="h-12 rounded-md" />
              ))}
            </div>
          ) : appointments.error ? (
            <Alert tone="warning" title="Appointments could not be loaded">
              {appointments.error.messages.join(' ')}
            </Alert>
          ) : recentAppointments.length === 0 ? (
            <EmptyState
              icon={CalendarDays}
              title="No appointments recorded"
              description="This patient has not been scheduled for any visits yet."
            />
          ) : (
            <ol className="relative space-y-4 border-l border-border pl-5">
              {recentAppointments.map((appointment) => (
                <li key={appointment.id} className="relative">
                  <span
                    aria-hidden
                    className="absolute top-1.5 -left-[25px] size-2 rounded-full border border-border bg-elevated"
                  />
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <time
                      dateTime={appointment.scheduledAt}
                      className="text-[13px] font-medium text-fg"
                    >
                      {formatDateTime(appointment.scheduledAt)}
                    </time>
                    <AppointmentStatusBadge status={appointment.status} />
                  </div>
                  <p className="mt-0.5 text-[13px] text-muted">
                    {appointment.doctor?.fullName ?? 'Unassigned doctor'}
                    {appointment.reason ? ` · ${appointment.reason}` : ''}
                  </p>
                </li>
              ))}
            </ol>
          )}

          <div className="mt-5 border-t border-border pt-4">
            <Link
              to={`/appointments?patientId=${patientId}`}
              className={buttonStyles({ variant: 'ghost', size: 'sm' })}
            >
              View all appointments
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
