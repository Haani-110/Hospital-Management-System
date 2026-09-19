import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { BarChart3, CircleDollarSign } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { StatCard, StatCardSkeleton } from '@/components/data/stat-card';
import { ErrorState } from '@/components/data/data-state';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/skeleton';
import {
  appointmentsApi,
  billsApi,
  doctorsApi,
  medicalRecordsApi,
  patientsApi,
  prescriptionsApi,
  reportsApi,
} from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { formatCurrency, formatNumber } from '@/lib/utils';
import { AppointmentStatus, BillStatus, Role } from '@/lib/api/types';
import { useAuth } from '@/providers/auth-context';

interface BreakdownRow {
  label: string;
  count: number;
  href: string;
}

interface ReportsData {
  summary: Awaited<ReturnType<typeof reportsApi.getDashboardSummary>>;
  breakdown: BreakdownRow[];
}

/** Total row count for a filtered request, taken from the pagination metadata. */
async function countOf(
  request: (signal: AbortSignal) => Promise<{ meta: { total: number } }>,
  signal: AbortSignal,
): Promise<number> {
  const response = await request(signal);
  return response.meta.total;
}

/**
 * Reports and operational totals.
 *
 * Every figure is a real count: the headline tiles come from
 * `/reports/dashboard-summary`, and the breakdown table asks the list endpoints
 * for filtered page counts (`limit=1`, reading `meta.total`). No estimates,
 * sampling or cached values are used.
 */
export function ReportsPage() {
  const { user } = useAuth();
  const canViewBilling = user?.role === Role.ADMIN || user?.role === Role.RECEPTIONIST;

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback(async (signal): Promise<ReportsData> => {
      const [
        summary,
        activePatients,
        inactivePatients,
        activeDoctors,
        inactiveDoctors,
        scheduled,
        completed,
        cancelled,
        records,
        prescriptions,
        unpaid,
        partiallyPaid,
        paid,
      ] = await Promise.all([
        reportsApi.getDashboardSummary(signal),
        countOf((s) => patientsApi.listPatients({ active: true, limit: 1, page: 1 }, s), signal),
        countOf((s) => patientsApi.listPatients({ active: false, limit: 1, page: 1 }, s), signal),
        countOf((s) => doctorsApi.listDoctors({ active: true, limit: 1, page: 1 }, s), signal),
        countOf((s) => doctorsApi.listDoctors({ active: false, limit: 1, page: 1 }, s), signal),
        countOf(
          (s) =>
            appointmentsApi.listAppointments(
              { status: AppointmentStatus.SCHEDULED, limit: 1, page: 1 },
              s,
            ),
          signal,
        ),
        countOf(
          (s) =>
            appointmentsApi.listAppointments(
              { status: AppointmentStatus.COMPLETED, limit: 1, page: 1 },
              s,
            ),
          signal,
        ),
        countOf(
          (s) =>
            appointmentsApi.listAppointments(
              { status: AppointmentStatus.CANCELLED, limit: 1, page: 1 },
              s,
            ),
          signal,
        ),
        countOf((s) => medicalRecordsApi.listMedicalRecords({ limit: 1, page: 1 }, s), signal),
        countOf((s) => prescriptionsApi.listPrescriptions({ limit: 1, page: 1 }, s), signal),
        // Bills are restricted to ADMIN and RECEPTIONIST; clinical accounts skip them.
        canViewBilling
          ? countOf((s) => billsApi.listBills({ status: BillStatus.UNPAID, limit: 1, page: 1 }, s), signal)
          : Promise.resolve<number | null>(null),
        canViewBilling
          ? countOf(
              (s) => billsApi.listBills({ status: BillStatus.PARTIALLY_PAID, limit: 1, page: 1 }, s),
              signal,
            )
          : Promise.resolve<number | null>(null),
        canViewBilling
          ? countOf((s) => billsApi.listBills({ status: BillStatus.PAID, limit: 1, page: 1 }, s), signal)
          : Promise.resolve<number | null>(null),
      ]);

      const breakdown: BreakdownRow[] = [
        { label: 'Active patients', count: activePatients, href: '/patients?active=true' },
        { label: 'Inactive patients', count: inactivePatients, href: '/patients?active=false' },
        { label: 'Active doctors', count: activeDoctors, href: '/doctors?active=true' },
        { label: 'Inactive doctors', count: inactiveDoctors, href: '/doctors?active=false' },
        {
          label: 'Appointments — scheduled',
          count: scheduled,
          href: `/appointments?status=${AppointmentStatus.SCHEDULED}`,
        },
        {
          label: 'Appointments — completed',
          count: completed,
          href: `/appointments?status=${AppointmentStatus.COMPLETED}`,
        },
        {
          label: 'Appointments — cancelled',
          count: cancelled,
          href: `/appointments?status=${AppointmentStatus.CANCELLED}`,
        },
        { label: 'Medical records', count: records, href: '/medical-records' },
        { label: 'Prescriptions', count: prescriptions, href: '/prescriptions' },
      ];

      if (unpaid !== null && partiallyPaid !== null && paid !== null) {
        breakdown.push(
          { label: 'Bills — unpaid', count: unpaid, href: `/billing?status=${BillStatus.UNPAID}` },
          {
            label: 'Bills — partially paid',
            count: partiallyPaid,
            href: `/billing?status=${BillStatus.PARTIALLY_PAID}`,
          },
          { label: 'Bills — paid', count: paid, href: `/billing?status=${BillStatus.PAID}` },
        );
      }

      return { summary, breakdown };
    }, [canViewBilling]),
  );

  const totalRecords = data ? data.breakdown.reduce((sum, row) => sum + row.count, 0) : 0;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Reports"
        title="Reports"
        description="Operational totals computed from the live database. Each figure links to the matching filtered list."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      {isInitialLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <StatCardSkeleton key={index} />
          ))}
        </div>
      ) : error ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : data ? (
        <>
          <section aria-label="Headline totals" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="Total patients"
              value={formatNumber(data.summary.totalPatients)}
              icon={BarChart3}
            />
            <StatCard
              label="Active doctors"
              value={formatNumber(data.summary.activeDoctors)}
              icon={BarChart3}
            />
            <StatCard
              label="Appointments today"
              value={formatNumber(data.summary.todayAppointments)}
              icon={BarChart3}
            />
            <StatCard
              label="Revenue today"
              value={formatCurrency(data.summary.todayRevenue)}
              icon={CircleDollarSign}
            />
          </section>

          <Card>
            <CardHeader>
              <div>
                <CardTitle>Breakdown</CardTitle>
                <CardDescription>
                  Filtered counts from the list endpoints, read from each response&rsquo;s pagination
                  metadata.
                </CardDescription>
              </div>
            </CardHeader>
            <CardContent className="px-0">
              {totalRecords === 0 ? (
                <EmptyState
                  icon={BarChart3}
                  title="Nothing recorded yet"
                  description="Counts appear here as soon as the database contains patients, staff, visits or bills."
                />
              ) : (
                <ul className="divide-y divide-border">
                  {data.breakdown.map((row) => (
                    <li key={row.label}>
                      <Link
                        to={row.href}
                        className="flex items-center justify-between gap-4 px-5 py-3 transition-colors hover:bg-elevated/60"
                      >
                        <span className="text-[13px] text-muted">{row.label}</span>
                        <span className="flex items-center gap-3">
                          <span className="font-medium text-fg tabular">
                            {formatNumber(row.count)}
                          </span>
                          <span aria-hidden className="text-subtle">
                            →
                          </span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          {!canViewBilling ? (
            <p className="text-xs text-subtle">
              Billing rows are hidden because the bills endpoint rejects clinical accounts.
            </p>
          ) : null}
        </>
      ) : (
        <Skeleton className="h-40 w-full" />
      )}
    </div>
  );
}
