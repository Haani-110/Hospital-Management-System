import { useCallback, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardList } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { ListView } from '@/components/data/list-view';
import { PatientPicker } from '@/components/data/record-picker';
import { DoctorPicker } from '@/components/data/entity-pickers';
import { DateRangeFilter } from '@/components/data/date-range-filter';
import { OptionalValue } from '@/components/data/description-list';
import { prescriptionsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { toApiRange } from '@/lib/date-range';
import { formatDate } from '@/lib/utils';
import type { Prescription } from '@/lib/api/types';

const PRESCRIPTION_FILTERS = {
  search: '',
  patientId: '',
  doctorId: '',
  from: '',
  to: '',
  page: '',
  limit: '20',
};

/**
 * Prescriptions backed by `GET /prescriptions`, which nests the prescribed
 * items. The API exposes no free-text search here, so filtering is by patient,
 * prescriber and date range only.
 */
export function PrescriptionsPage() {
  const { values, page, limit, setPage, setFilter, setSearch } = useListQuery(PRESCRIPTION_FILTERS);
  const [openPrescription, setOpenPrescription] = useState<Prescription | null>(null);

  const range = useMemo(() => ({ from: values.from, to: values.to }), [values.from, values.to]);

  const query = useMemo(() => {
    const bounds = toApiRange(range);
    return {
      page,
      limit,
      patientId: values.patientId || undefined,
      doctorId: values.doctorId || undefined,
      from: bounds.from,
      to: bounds.to,
    };
  }, [page, limit, values.patientId, values.doctorId, range]);

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback((signal) => prescriptionsApi.listPrescriptions(query, signal), [query]),
  );

  const hasFilters = Boolean(values.patientId || values.doctorId || values.from || values.to);

  function resetFilters() {
    setSearch('');
    setFilter('patientId', '');
    setFilter('doctorId', '');
    setFilter('from', '');
    setFilter('to', '');
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Clinical"
        title="Prescriptions"
        description="Medication issued after consultations, with dosage and duration."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Card className="overflow-hidden">
        <FilterBar>
          <PatientPicker
            label="Filter by patient"
            className="w-full sm:w-64"
            value={values.patientId}
            onChange={(value) => setFilter('patientId', value)}
          />
          <FilterGroup>
            <DoctorPicker
              label="Filter by prescriber"
              className="w-full sm:w-60"
              value={values.doctorId}
              onChange={(value) => setFilter('doctorId', value)}
            />
            <DateRangeFilter
              idPrefix="prescription"
              value={range}
              onChange={(next) => {
                setFilter('from', next.from);
                setFilter('to', next.to);
              }}
            />
            {hasFilters ? (
              <Button variant="ghost" size="sm" onClick={resetFilters}>
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
          skeletonColumns={4}
          emptyIcon={ClipboardList}
          emptyTitle={hasFilters ? 'No prescriptions match these filters' : 'No prescriptions yet'}
          emptyDescription={
            hasFilters
              ? 'Try a different patient, prescriber or date range.'
              : 'Prescriptions are recorded with their medicine items when issued. This build reads them from the API; creating a prescription is not exposed by the backend yet.'
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
                Prescriptions, page {page}. Newest first.
              </caption>
              <THead>
                <TR>
                  <TH>Date</TH>
                  <TH>Patient</TH>
                  <TH>Medicines</TH>
                  <TH className="hidden lg:table-cell">Prescriber</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((prescription) => (
                  <TR key={prescription.id}>
                    <TD className="whitespace-nowrap text-muted">
                      {formatDate(prescription.prescriptionDate)}
                    </TD>
                    <TD>
                      {prescription.patient ? (
                        <Link
                          to={`/patients/${prescription.patient.id}`}
                          className="font-medium text-fg hover:text-accent"
                        >
                          {prescription.patient.fullName}
                          <span className="block font-mono text-xs font-normal text-subtle">
                            {prescription.patient.patientCode}
                          </span>
                        </Link>
                      ) : (
                        <span className="text-subtle">—</span>
                      )}
                    </TD>
                    <TD>
                      {prescription.items.length === 0 ? (
                        <span className="text-subtle">No items recorded</span>
                      ) : (
                        <>
                          <span className="text-fg">{prescription.items[0]?.medicineName}</span>
                          {prescription.items.length > 1 ? (
                            <Badge tone="neutral" className="ml-2">
                              +{prescription.items.length - 1} more
                            </Badge>
                          ) : null}
                        </>
                      )}
                    </TD>
                    <TD className="hidden lg:table-cell text-muted">
                      {prescription.doctor?.fullName ?? '—'}
                    </TD>
                    <TD className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setOpenPrescription(prescription)}>
                        Details
                        <span className="sr-only">
                          {' '}
                          for prescription dated {formatDate(prescription.prescriptionDate)}
                        </span>
                      </Button>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? (
            <Pagination meta={data.meta} onPageChange={setPage} itemLabel="prescriptions" />
          ) : null}
        </ListView>
      </Card>

      <Dialog
        open={openPrescription !== null}
        onOpenChange={(open) => {
          if (!open) setOpenPrescription(null);
        }}
        title="Prescription"
        description={
          openPrescription
            ? `${openPrescription.patient?.fullName ?? 'Patient'} · issued ${formatDate(
                openPrescription.prescriptionDate,
              )}`
            : undefined
        }
      >
        {openPrescription ? (
          <div className="space-y-5">
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <dt className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                  Prescriber
                </dt>
                <dd className="mt-1.5 text-sm text-fg">
                  {openPrescription.doctor?.fullName ?? <OptionalValue value={null} />}
                </dd>
              </div>
              <div>
                <dt className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                  Prescription date
                </dt>
                <dd className="mt-1.5 text-sm text-fg">
                  {formatDate(openPrescription.prescriptionDate)}
                </dd>
              </div>
            </dl>

            <Separator />

            <div>
              <h3 className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                Medicines
              </h3>
              {openPrescription.items.length === 0 ? (
                <p className="mt-2 text-[13px] text-muted">No medicine items recorded.</p>
              ) : (
                <ul className="mt-3 space-y-3">
                  {openPrescription.items.map((item) => (
                    <li key={item.id} className="rounded-md border border-border bg-elevated/40 p-3.5">
                      <p className="text-sm font-medium text-fg">{item.medicineName}</p>
                      <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1.5 text-[13px] sm:grid-cols-3">
                        <div>
                          <dt className="text-subtle">Dosage</dt>
                          <dd className="text-fg">{item.dosage ?? '—'}</dd>
                        </div>
                        <div>
                          <dt className="text-subtle">Frequency</dt>
                          <dd className="text-fg">{item.frequency ?? '—'}</dd>
                        </div>
                        <div>
                          <dt className="text-subtle">Duration</dt>
                          <dd className="text-fg">{item.duration ?? '—'}</dd>
                        </div>
                      </dl>
                      {item.instructions ? (
                        <p className="mt-2 text-[13px] text-muted">{item.instructions}</p>
                      ) : null}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {openPrescription.notes ? (
              <>
                <Separator />
                <div>
                  <h3 className="text-[11px] font-semibold tracking-wide text-subtle uppercase">
                    Notes
                  </h3>
                  <p className="mt-2 text-[13px] text-muted">{openPrescription.notes}</p>
                </div>
              </>
            ) : null}
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}
