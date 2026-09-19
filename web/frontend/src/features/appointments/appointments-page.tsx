import { useCallback, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CalendarDays, CalendarX2, Info } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { buttonStyles } from '@/components/ui/button-styles';
import { Button,  } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { ListView } from '@/components/data/list-view';
import { PatientPicker } from '@/components/data/record-picker';
import { DoctorPicker } from '@/components/data/entity-pickers';
import { AppointmentStatusBadge } from '@/components/data/status-badge';
import { Alert } from '@/components/ui/alert';
import { appointmentsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { dayBounds, formatDate, formatTime, isToday } from '@/lib/utils';
import { AppointmentStatus } from '@/lib/api/types';

const APPOINTMENT_FILTERS = {
  search: '',
  patientId: '',
  doctorId: '',
  status: '',
  date: '',
  page: '',
  limit: '20',
};

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: AppointmentStatus.SCHEDULED, label: 'Scheduled' },
  { value: AppointmentStatus.COMPLETED, label: 'Completed' },
  { value: AppointmentStatus.CANCELLED, label: 'Cancelled' },
];

/**
 * Appointment schedule backed by `GET /appointments`.
 *
 * Filters mirror the backend query DTO exactly: patient, doctor, status and a
 * single-day date filter sent as the `from`/`to` range the API expects. The
 * endpoint accepts no free-text search, so none is offered.
 */
export function AppointmentsPage() {
  const { values, page, limit, setPage, setFilter } = useListQuery(APPOINTMENT_FILTERS);
  const [searchParams] = useSearchParams();

  // Deep links from other screens carry the patient filter in the URL.
  const patientFilterActive = Boolean(values.patientId || searchParams.get('patientId'));

  const bounds = useMemo(() => (values.date ? dayBounds(new Date(`${values.date}T00:00:00`)) : null), [
    values.date,
  ]);

  const query = useMemo(
    () => ({
      page,
      limit,
      patientId: values.patientId || undefined,
      doctorId: values.doctorId || undefined,
      status: (values.status || undefined) as AppointmentStatus | undefined,
      from: bounds?.from,
      to: bounds?.to,
    }),
    [page, limit, values.patientId, values.doctorId, values.status, bounds],
  );

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback((signal) => appointmentsApi.listAppointments(query, signal), [query]),
  );

  // `GET /appointments` supports no free-text search, so this screen filters by
  // the parameters the API genuinely implements.
  const hasFilters = Boolean(values.patientId || values.doctorId || values.status || values.date);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Clinical"
        title="Appointments"
        description="Scheduled, completed and cancelled visits across the hospital."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Alert tone="info" title="Appointments are scheduled by reception through the API">
        This build reads live scheduling data; creating or rescheduling a visit is not exposed by the
        backend yet, so no form is offered here.
      </Alert>

      <Card className="overflow-hidden">
        <FilterBar className="gap-3">
          <FilterGroup className="w-full sm:w-auto">
            <PatientPicker
              label="Filter by patient"
              className="w-full sm:w-64"
              value={values.patientId}
              onChange={(value) => setFilter('patientId', value)}
            />
            <DoctorPicker
              label="Filter by doctor"
              className="w-full sm:w-64"
              value={values.doctorId}
              onChange={(value) => setFilter('doctorId', value)}
            />
          </FilterGroup>
        </FilterBar>

        <FilterBar className="border-t-0 bg-elevated/30">
          <FilterGroup>
            <label htmlFor="appointment-status" className="sr-only">
              Filter by appointment status
            </label>
            <Select
              id="appointment-status"
              className="w-full sm:w-44"
              value={values.status}
              onChange={(event) => setFilter('status', event.target.value)}
              options={STATUS_OPTIONS}
            />
            <label htmlFor="appointment-date" className="sr-only">
              Show appointments on a single date
            </label>
            <Input
              id="appointment-date"
              type="date"
              className="w-full sm:w-44"
              value={values.date}
              onChange={(event) => setFilter('date', event.target.value)}
            />
            {values.date ? (
              <Button variant="ghost" size="sm" onClick={() => setFilter('date', '')}>
                Clear date
              </Button>
            ) : null}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                const today = new Date();
                const iso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(
                  today.getDate(),
                ).padStart(2, '0')}`;
                setFilter('date', iso);
              }}
            >
              Today
            </Button>
          </FilterGroup>

          <FilterGroup>
            {patientFilterActive ? (
              <Badge tone="accent">Filtered to one patient</Badge>
            ) : null}
            {hasFilters ? (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setFilter('status', '');
                  setFilter('patientId', '');
                  setFilter('doctorId', '');
                  setFilter('date', '');
                }}
              >
                Reset filters
              </Button>
            ) : null}
          </FilterGroup>
        </FilterBar>

        <ListView
          isInitialLoading={isInitialLoading}
          error={error}
          isEmpty={data?.data.length === 0}
          onRetry={refetch}
          isRefreshing={isLoading && !isInitialLoading}
          skeletonColumns={5}
          emptyIcon={hasFilters ? CalendarX2 : CalendarDays}
          emptyTitle={hasFilters ? 'No appointments match these filters' : 'No appointments yet'}
          emptyDescription={
            hasFilters
              ? 'Adjust the date range, status or people filters to widen the search.'
              : 'Appointments will appear here as soon as they are scheduled through the API.'
          }
          emptyAction={
            hasFilters ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setFilter('status', '');
                  setFilter('patientId', '');
                  setFilter('doctorId', '');
                  setFilter('date', '');
                }}
              >
                Reset filters
              </Button>
            ) : (
              <span className={buttonStyles({ variant: 'ghost', size: 'sm' })}>
                <Info aria-hidden className="size-3.5" />
                No write endpoint available
              </span>
            )
          }
        >
          <TableWrap>
            <Table>
              <caption className="sr-only">
                Appointments, page {page}. Sorted by scheduled time, most recent first.
              </caption>
              <THead>
                <TR>
                  <TH>When</TH>
                  <TH>Patient</TH>
                  <TH className="hidden md:table-cell">Doctor</TH>
                  <TH className="hidden lg:table-cell">Reason</TH>
                  <TH>Status</TH>
                  <TH className="hidden xl:table-cell">Appointment ID</TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((appointment) => (
                  <TR key={appointment.id}>
                    <TD className="whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <time dateTime={appointment.scheduledAt} className="font-medium text-fg">
                          {formatDate(appointment.scheduledAt)}
                        </time>
                        {isToday(appointment.scheduledAt) ? <Badge tone="accent">Today</Badge> : null}
                      </div>
                      <p className="mt-0.5 text-xs text-subtle">
                        {formatTime(appointment.scheduledAt)}
                      </p>
                    </TD>
                    <TD>
                      {appointment.patient ? (
                        <Link
                          to={`/patients/${appointment.patient.id}`}
                          className="font-medium text-fg hover:text-accent"
                        >
                          {appointment.patient.fullName}
                          <span className="block font-mono text-xs font-normal text-subtle">
                            {appointment.patient.patientCode}
                          </span>
                        </Link>
                      ) : (
                        <span className="text-subtle">Unknown patient</span>
                      )}
                    </TD>
                    <TD className="hidden md:table-cell">
                      {appointment.doctor ? (
                        <Link
                          to={`/doctors/${appointment.doctor.id}`}
                          className="text-muted hover:text-accent"
                        >
                          {appointment.doctor.fullName}
                          <span className="block text-xs text-subtle">
                            {appointment.doctor.specialization ?? ''}
                          </span>
                        </Link>
                      ) : (
                        <span className="text-subtle">—</span>
                      )}
                    </TD>
                    <TD className="hidden lg:table-cell max-w-xs">
                      <span className="line-clamp-2 text-muted">{appointment.reason ?? '—'}</span>
                    </TD>
                    <TD>
                      <AppointmentStatusBadge status={appointment.status} />
                    </TD>
                    <TD className="hidden xl:table-cell">
                      <span className="font-mono text-xs text-subtle">
                        {appointment.id.slice(0, 8)}
                      </span>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? (
            <Pagination meta={data.meta} onPageChange={setPage} itemLabel="appointments" />
          ) : null}
        </ListView>

      </Card>
    </div>
  );
}
