import { useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Receipt } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { ListView } from '@/components/data/list-view';
import { PatientPicker } from '@/components/data/record-picker';
import { AppointmentPicker } from '@/components/data/entity-pickers';
import { DateRangeFilter } from '@/components/data/date-range-filter';
import { BillStatusBadge } from '@/components/data/status-badge';
import { billsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { toApiRange } from '@/lib/date-range';
import { formatCurrency, formatDate } from '@/lib/utils';
import { BillStatus } from '@/lib/api/types';
import { useAuth } from '@/providers/auth-context';

const BILL_FILTERS = {
  search: '',
  patientId: '',
  appointmentId: '',
  status: '',
  from: '',
  to: '',
  page: '',
  limit: '20',
};

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: BillStatus.UNPAID, label: 'Unpaid' },
  { value: BillStatus.PARTIALLY_PAID, label: 'Partially paid' },
  { value: BillStatus.PAID, label: 'Paid' },
  { value: BillStatus.CANCELLED, label: 'Cancelled' },
];

/**
 * Billing ledger backed by `GET /bills`.
 *
 * The endpoint is restricted to ADMIN and RECEPTIONIST: clinical accounts
 * receive 403, which is surfaced here as an explanation rather than a crash, and
 * the sidebar hides the link for those roles.
 */
export function BillingPage() {
  const { user } = useAuth();
  const { values, page, limit, setPage, setFilter, setSearch } = useListQuery(BILL_FILTERS);

  const range = useMemo(() => ({ from: values.from, to: values.to }), [values.from, values.to]);

  const query = useMemo(() => {
    const bounds = toApiRange(range);
    return {
      page,
      limit,
      patientId: values.patientId || undefined,
      appointmentId: values.appointmentId || undefined,
      status: (values.status || undefined) as BillStatus | undefined,
      from: bounds.from,
      to: bounds.to,
    };
  }, [page, limit, values.patientId, values.appointmentId, values.status, range]);

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback((signal) => billsApi.listBills(query, signal), [query]),
  );

  const hasFilters = Boolean(
    values.patientId || values.appointmentId || values.status || values.from || values.to,
  );

  function resetFilters() {
    setSearch('');
    setFilter('patientId', '');
    setFilter('appointmentId', '');
    setFilter('status', '');
    setFilter('from', '');
    setFilter('to', '');
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Operations"
        title="Billing"
        description="Invoices raised for consultations, procedures and services, with their payment status."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Card className="overflow-hidden">
        <FilterBar>
          <FilterGroup className="w-full sm:w-auto">
            <PatientPicker
              label="Filter by patient"
              className="w-full sm:w-64"
              value={values.patientId}
              onChange={(value) => setFilter('patientId', value)}
            />
            <AppointmentPicker
              label="Filter by appointment"
              className="w-full sm:w-60"
              value={values.appointmentId}
              onChange={(value) => setFilter('appointmentId', value)}
            />
          </FilterGroup>
        </FilterBar>

        <FilterBar className="border-t-0 bg-elevated/30">
          <FilterGroup>
            <label htmlFor="bill-status" className="sr-only">
              Filter by payment status
            </label>
            <Select
              id="bill-status"
              className="w-full sm:w-44"
              value={values.status}
              onChange={(event) => setFilter('status', event.target.value)}
              options={STATUS_OPTIONS}
            />
            <DateRangeFilter
              idPrefix="bill"
              value={range}
              onChange={(next) => {
                setFilter('from', next.from);
                setFilter('to', next.to);
              }}
            />
          </FilterGroup>
          {hasFilters ? (
            <FilterGroup>
              <Button variant="ghost" size="sm" onClick={resetFilters}>
                Reset filters
              </Button>
            </FilterGroup>
          ) : null}
        </FilterBar>

        <ListView
          isInitialLoading={isInitialLoading}
          error={error}
          isEmpty={data?.data.length === 0}
          onRetry={refetch}
          isRefreshing={isLoading && !isInitialLoading}
          skeletonColumns={5}
          emptyIcon={Receipt}
          emptyTitle={hasFilters ? 'No bills match these filters' : 'No bills yet'}
          emptyDescription={
            hasFilters
              ? 'Adjust the patient, appointment, status or date filters.'
              : user?.role
                ? 'Invoices appear here once issued through the API. Raising a bill is not exposed by the backend in this build, so no create form is shown.'
                : undefined
          }
          emptyAction={
            hasFilters ? (
              <Button variant="outline" size="sm" onClick={resetFilters}>
                Reset filters
              </Button>
            ) : undefined
          }
        >
          <TableWrap>
            <Table>
              <caption className="sr-only">
                Bills, page {page}. Newest first; totals are decimal strings from the API.
              </caption>
              <THead>
                <TR>
                  <TH>Bill</TH>
                  <TH>Patient</TH>
                  <TH className="hidden lg:table-cell">Date</TH>
                  <TH>Status</TH>
                  <TH className="text-right">Subtotal</TH>
                  <TH className="text-right">Discount</TH>
                  <TH className="text-right">Total</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((bill) => (
                  <TR key={bill.id}>
                    <TD className="font-mono text-[13px] text-muted">{bill.billNumber}</TD>
                    <TD>
                      {bill.patient ? (
                        <Link
                          to={`/patients/${bill.patient.id}`}
                          className="font-medium text-fg hover:text-accent"
                        >
                          {bill.patient.fullName}
                          <span className="block font-mono text-xs font-normal text-subtle">
                            {bill.patient.patientCode}
                          </span>
                        </Link>
                      ) : (
                        <span className="text-subtle">—</span>
                      )}
                    </TD>
                    <TD className="hidden lg:table-cell whitespace-nowrap text-muted">
                      {formatDate(bill.billDate)}
                    </TD>
                    <TD>
                      <BillStatusBadge status={bill.status} />
                    </TD>
                    <TD className="text-right text-muted tabular">
                      {formatCurrency(bill.subtotal)}
                    </TD>
                    <TD className="text-right text-muted tabular">
                      {formatCurrency(bill.discount)}
                    </TD>
                    <TD className="text-right font-medium tabular">
                      {formatCurrency(bill.totalAmount)}
                    </TD>
                    <TD className="text-right">
                      <Link
                        to={`/billing/${bill.id}`}
                        className="text-[13px] font-medium text-accent hover:underline"
                      >
                        Open
                        <span className="sr-only"> bill {bill.billNumber}</span>
                      </Link>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? <Pagination meta={data.meta} onPageChange={setPage} itemLabel="bills" /> : null}
        </ListView>
      </Card>
    </div>
  );
}
