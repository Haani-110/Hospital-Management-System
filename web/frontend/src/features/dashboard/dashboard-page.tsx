import { useCallback } from 'react';
import {
  Activity,
  Banknote,
  CalendarCheck,
  CalendarClock,
  CalendarDays,
  CircleDollarSign,
  Receipt,
  Stethoscope,
  Users,
} from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { StatCard, StatCardSkeleton } from '@/components/data/stat-card';
import { EmptyState } from '@/components/ui/empty-state';
import { Alert } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { DonutChart } from '@/components/charts/donut-chart';
import { ErrorState } from '@/components/data/data-state';
import { reportsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { formatCurrency, formatNumber } from '@/lib/utils';
import { useAuth } from '@/providers/auth-context';

/**
 * Dashboard.
 *
 * Every number on this screen comes from `GET /reports/dashboard-summary`,
 * which computes its counters in PostgreSQL. Nothing is estimated, sampled or
 * hardcoded — if the database is empty, the screen shows zeros and says so.
 */
export function DashboardPage() {
  const { user } = useAuth();
  const { data, error, isInitialLoading, refetch } = useApi(
    useCallback((signal) => reportsApi.getDashboardSummary(signal), []),
  );

  const totalTracked =
    (data?.pendingAppointments ?? 0) +
    (data?.completedAppointments ?? 0);
  const hasAnyData = data
    ? data.totalPatients +
        data.activeDoctors +
        data.todayAppointments +
        data.unpaidBills +
        data.partiallyPaidBills >
      0
    : false;

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Overview"
        title={user ? `Welcome back, ${user.username}` : 'Dashboard'}
        description="Live totals from the hospital database — patients, today's clinics and outstanding billing."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isInitialLoading}>
            Refresh
          </Button>
        }
      />

      {isInitialLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, index) => (
            <StatCardSkeleton key={index} />
          ))}
        </div>
      ) : error ? (
        <ErrorState error={error} onRetry={refetch} />
      ) : data ? (
        <>
          {!hasAnyData ? (
            <Alert tone="info" title="The database is empty">
              No patients, doctors or bills have been recorded yet. Totals will appear here as soon
              as the first records are created through the API or seed script.
            </Alert>
          ) : null}

          <section aria-label="Key totals" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              emphasis
              label="Today's revenue"
              value={formatCurrency(data.todayRevenue)}
              hint="Billed today across all payment states"
              icon={CircleDollarSign}
            />
            <StatCard
              label="Total patients"
              value={formatNumber(data.totalPatients)}
              hint="Registered in the system"
              icon={Users}
            />
            <StatCard
              label="Active doctors"
              value={formatNumber(data.activeDoctors)}
              hint="Available for appointments"
              icon={Stethoscope}
            />
            <StatCard
              label="Today's appointments"
              value={formatNumber(data.todayAppointments)}
              hint="Scheduled for today"
              icon={CalendarDays}
            />
            <StatCard
              label="Pending appointments"
              value={formatNumber(data.pendingAppointments)}
              hint="Still scheduled, not yet completed"
              icon={CalendarClock}
            />
            <StatCard
              label="Completed appointments"
              value={formatNumber(data.completedAppointments)}
              hint="All time"
              icon={CalendarCheck}
            />
            <StatCard
              label="Unpaid bills"
              value={formatNumber(data.unpaidBills)}
              hint="No payment recorded"
              icon={Receipt}
            />
            <StatCard
              label="Partially paid bills"
              value={formatNumber(data.partiallyPaidBills)}
              hint="Balance still outstanding"
              icon={Banknote}
            />
          </section>

          <section className="grid gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <div>
                  <CardTitle>Appointment status</CardTitle>
                  <CardDescription>
                    Scheduled versus completed visits recorded in the system.
                  </CardDescription>
                </div>
              </CardHeader>
              <CardContent>
                {totalTracked === 0 ? (
                  <EmptyState
                    icon={Activity}
                    title="No appointments recorded"
                    description="Once appointments are created, their status breakdown appears here."
                  />
                ) : (
                  <DonutChart
                    centerLabel="appointments"
                    slices={[
                      {
                        label: 'Scheduled',
                        value: data.pendingAppointments,
                        color: 'var(--info)',
                      },
                      {
                        label: 'Completed',
                        value: data.completedAppointments,
                        color: 'var(--success)',
                      },
                    ]}
                  />
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <div>
                  <CardTitle>Billing position</CardTitle>
                  <CardDescription>Outstanding work by payment state.</CardDescription>
                </div>
              </CardHeader>
              <CardContent className="space-y-5">
                <dl className="grid grid-cols-2 gap-4">
                  <div className="rounded-md border border-border bg-elevated/50 p-4">
                    <dt className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                      Unpaid bills
                    </dt>
                    <dd className="mt-1.5 text-xl font-semibold text-fg tabular">
                      {formatNumber(data.unpaidBills)}
                    </dd>
                  </div>
                  <div className="rounded-md border border-border bg-elevated/50 p-4">
                    <dt className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                      Partially paid
                    </dt>
                    <dd className="mt-1.5 text-xl font-semibold text-fg tabular">
                      {formatNumber(data.partiallyPaidBills)}
                    </dd>
                  </div>
                </dl>
                <div className="border-t border-border pt-4">
                  <p className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                    Today&rsquo;s revenue
                  </p>
                  <p className="mt-1.5 text-2xl font-semibold text-fg tabular">
                    {formatCurrency(data.todayRevenue)}
                  </p>
                  <p className="mt-1 text-xs text-subtle">
                    Sum of bills dated today. Nothing is projected or estimated.
                  </p>
                </div>
              </CardContent>
            </Card>
          </section>
        </>
      ) : (
        <Skeleton className="h-40 w-full" />
      )}
    </div>
  );
}
