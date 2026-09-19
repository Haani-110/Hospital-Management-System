import { useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CalendarDays, Stethoscope } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Avatar } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
import { buttonStyles } from '@/components/ui/button-styles';
import {  } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert } from '@/components/ui/alert';
import { EmptyState } from '@/components/ui/empty-state';
import { DetailView } from '@/components/data/detail-view';
import { DescriptionList, OptionalValue } from '@/components/data/description-list';
import { ActiveBadge, AppointmentStatusBadge } from '@/components/data/status-badge';
import { appointmentsApi, doctorsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { formatCurrency, formatDate, formatDateTime } from '@/lib/utils';

function DoctorSkeleton() {
  return (
    <Card className="p-6">
      <div className="flex items-center gap-4">
        <Skeleton className="size-14 rounded-full" />
        <div className="flex-1 space-y-2.5">
          <Skeleton className="h-5 w-52" />
          <Skeleton className="h-3 w-40" />
        </div>
      </div>
    </Card>
  );
}

/** Doctor profile with their real appointment activity from `GET /appointments?doctorId=`. */
export function DoctorDetailPage() {
  const { doctorId = '' } = useParams();

  const { data: doctor, error, isLoading, refetch } = useApi(
    useCallback((signal) => doctorsApi.getDoctor(doctorId, signal), [doctorId]),
    { enabled: Boolean(doctorId) },
  );

  const appointments = useApi(
    useCallback(
      (signal) => appointmentsApi.listAppointments({ doctorId, limit: 8, page: 1 }, signal),
      [doctorId],
    ),
    { enabled: Boolean(doctorId) },
  );

  return (
    <div className="space-y-6">
      <div>
        <Link
          to="/doctors"
          className="inline-flex items-center gap-1.5 text-[13px] text-muted transition-colors hover:text-fg"
        >
          <ArrowLeft aria-hidden className="size-3.5" />
          All doctors
        </Link>
      </div>

      <DetailView
        isLoading={isLoading}
        error={error}
        onRetry={refetch}
        skeleton={<DoctorSkeleton />}
      >
        {doctor ? (
          <>
            <PageHeader
              eyebrow="Medical staff"
              title={doctor.fullName}
              description={doctor.specialization ?? 'General practice'}
              actions={
                <>
                  <ActiveBadge active={doctor.active} />
                  <Link
                    to={`/appointments?doctorId=${doctor.id}`}
                    className={buttonStyles({ variant: 'outline', size: 'sm' })}
                  >
                    View schedule
                  </Link>
                </>
              }
            />

            <div className="grid gap-4 lg:grid-cols-3">
              <Card className="lg:col-span-2">
                <CardHeader>
                  <div className="flex items-center gap-4">
                    <Avatar name={doctor.fullName} size="lg" />
                    <div>
                      <CardTitle className="text-base">{doctor.fullName}</CardTitle>
                      <CardDescription>
                        {doctor.department?.name ?? 'No department assigned'}
                      </CardDescription>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <Separator className="mb-5" />
                  <DescriptionList
                    items={[
                      { label: 'Specialization', value: <OptionalValue value={doctor.specialization} /> },
                      { label: 'Department', value: <OptionalValue value={doctor.department?.name} /> },
                      { label: 'Consultation fee', value: formatCurrency(doctor.consultationFee) },
                      { label: 'Staff status', value: doctor.active ? 'Active' : 'Inactive' },
                      { label: 'Phone', value: <OptionalValue value={doctor.phone} /> },
                      { label: 'Email', value: <OptionalValue value={doctor.email} /> },
                      { label: 'Joined', value: formatDate(doctor.createdAt) },
                      { label: 'Last updated', value: formatDateTime(doctor.updatedAt) },
                    ]}
                  />
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <div>
                    <CardTitle>Activity</CardTitle>
                    <CardDescription>Appointments recorded for this doctor.</CardDescription>
                  </div>
                </CardHeader>
                <CardContent>
                  {appointments.isInitialLoading ? (
                    <Skeleton className="h-20 rounded-md" />
                  ) : appointments.error ? (
                    <Alert tone="warning" title="Could not load appointments">
                      {appointments.error.messages.join(' ')}
                    </Alert>
                  ) : (
                    <>
                      <p className="text-3xl font-semibold text-fg tabular">
                        {appointments.data?.meta.total ?? 0}
                      </p>
                      <p className="mt-1 text-[13px] text-muted">appointments in total</p>

                      {appointments.data?.data.length ? (
                        <ul className="mt-5 space-y-3 border-t border-border pt-4">
                          {appointments.data.data.slice(0, 4).map((appointment) => (
                            <li key={appointment.id} className="flex items-start justify-between gap-3">
                              <div className="min-w-0">
                                <p className="truncate text-[13px] text-fg">
                                  {appointment.patient?.fullName ?? 'Unknown patient'}
                                </p>
                                <p className="text-xs text-subtle">
                                  {formatDateTime(appointment.scheduledAt)}
                                </p>
                              </div>
                              <AppointmentStatusBadge status={appointment.status} />
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="mt-4 border-t border-border pt-4 text-[13px] text-subtle">
                          No appointments scheduled with this doctor yet.
                        </p>
                      )}
                    </>
                  )}
                </CardContent>
              </Card>
            </div>

            {doctor.userId === null ? (
              <Alert tone="info" title="No login account linked">
                This doctor record is not connected to a user account, so they cannot sign in or
                authorise clinical actions yet.
              </Alert>
            ) : null}
          </>
        ) : error ? null : (
          <EmptyState
            icon={Stethoscope}
            title="Doctor not found"
            description="This doctor record does not exist or has been removed."
            action={
              <Link to="/doctors" className={buttonStyles({ variant: 'outline', size: 'sm' })}>
                Back to doctors
              </Link>
            }
          />
        )}
      </DetailView>

      {doctor && appointments.data?.meta.total === 0 ? (
        <p className="text-xs text-subtle">
          <CalendarDays aria-hidden className="mr-1.5 inline size-3.5" />
          Appointment totals are computed from the appointments table, not cached.
        </p>
      ) : null}
    </div>
  );
}
