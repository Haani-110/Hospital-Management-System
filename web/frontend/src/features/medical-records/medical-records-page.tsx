import { useCallback, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { Separator } from '@/components/ui/separator';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { SearchInput } from '@/components/data/search-input';
import { ListView } from '@/components/data/list-view';
import { PatientPicker } from '@/components/data/record-picker';
import { DoctorPicker } from '@/components/data/entity-pickers';
import { DateRangeFilter } from '@/components/data/date-range-filter';
import { DescriptionList, OptionalValue } from '@/components/data/description-list';
import { medicalRecordsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { toApiRange, todayInputValue } from '@/lib/date-range';
import { formatDate } from '@/lib/utils';
import type { MedicalRecord } from '@/lib/api/types';

const RECORD_FILTERS = {
  search: '',
  patientId: '',
  doctorId: '',
  from: '',
  to: '',
  page: '',
  limit: '20',
};

/**
 * Consultation records backed by `GET /medical-records`.
 *
 * The API's `search` parameter matches the **diagnosis** field only, which is
 * what the input label states. Full clinical detail is shown in a dialog fed by
 * the row's own data — no extra request is needed because the list response
 * already carries every field.
 */
export function MedicalRecordsPage() {
  const { values, debouncedSearch, page, limit, setPage, setFilter, setSearch } =
    useListQuery(RECORD_FILTERS);
  const [openRecord, setOpenRecord] = useState<MedicalRecord | null>(null);

  const range = useMemo(
    () => ({ from: values.from, to: values.to }),
    [values.from, values.to],
  );

  const query = useMemo(() => {
    const bounds = toApiRange(range);
    return {
      page,
      limit,
      search: debouncedSearch || undefined,
      patientId: values.patientId || undefined,
      doctorId: values.doctorId || undefined,
      from: bounds.from,
      to: bounds.to,
    };
  }, [page, limit, debouncedSearch, values.patientId, values.doctorId, range]);

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback((signal) => medicalRecordsApi.listMedicalRecords(query, signal), [query]),
  );

  const hasFilters = Boolean(
    values.search || values.patientId || values.doctorId || values.from || values.to,
  );

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
        title="Medical records"
        description="Diagnoses, examinations and treatment notes recorded during consultations."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Card className="overflow-hidden">
        <FilterBar>
          <SearchInput
            id="record-search"
            label="Search medical records by diagnosis"
            className="w-full sm:max-w-xs"
            placeholder="Diagnosis…"
            value={values.search}
            onValueChange={setSearch}
          />
          <FilterGroup>
            <PatientPicker
              label="Filter by patient"
              className="w-full sm:w-64"
              value={values.patientId}
              onChange={(value) => setFilter('patientId', value)}
            />
            <DoctorPicker
              label="Filter by doctor"
              className="w-full sm:w-60"
              value={values.doctorId}
              onChange={(value) => setFilter('doctorId', value)}
            />
          </FilterGroup>
        </FilterBar>

        <FilterBar className="border-t-0 bg-elevated/30">
          <DateRangeFilter
            idPrefix="record"
            value={range}
            onChange={(next) => {
              setFilter('from', next.from);
              setFilter('to', next.to);
            }}
          />
          <FilterGroup>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setFilter('from', todayInputValue());
                setFilter('to', todayInputValue());
              }}
            >
              Today only
            </Button>
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
          emptyIcon={FileText}
          emptyTitle={hasFilters ? 'No records match these filters' : 'No medical records yet'}
          emptyDescription={
            hasFilters
              ? 'Widen the date range or clear the patient and doctor filters.'
              : 'Diagnoses are stored here when a consultation is recorded through the API. Clinical endpoints are read-only in this build, so records are added by the backend or its seed script.'
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
                Medical records, page {page}. Newest first; search matches the diagnosis field.
              </caption>
              <THead>
                <TR>
                  <TH>Date</TH>
                  <TH>Patient</TH>
                  <TH>Diagnosis</TH>
                  <TH className="hidden lg:table-cell">Doctor</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((record) => (
                  <TR key={record.id}>
                    <TD className="whitespace-nowrap text-muted">{formatDate(record.recordDate)}</TD>
                    <TD>
                      {record.patient ? (
                        <Link
                          to={`/patients/${record.patient.id}`}
                          className="font-medium text-fg hover:text-accent"
                        >
                          {record.patient.fullName}
                          <span className="block font-mono text-xs font-normal text-subtle">
                            {record.patient.patientCode}
                          </span>
                        </Link>
                      ) : (
                        <span className="text-subtle">—</span>
                      )}
                    </TD>
                    <TD className="max-w-md">
                      <span className="font-medium text-fg">{record.diagnosis}</span>
                      {record.symptoms ? (
                        <span className="mt-0.5 block truncate text-xs text-subtle">
                          {record.symptoms}
                        </span>
                      ) : null}
                    </TD>
                    <TD className="hidden lg:table-cell text-muted">{record.doctor?.fullName ?? '—'}</TD>
                    <TD className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setOpenRecord(record)}>
                        Details
                        <span className="sr-only"> for {record.diagnosis}</span>
                      </Button>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? <Pagination meta={data.meta} onPageChange={setPage} itemLabel="records" /> : null}
        </ListView>
      </Card>

      <Dialog
        open={openRecord !== null}
        onOpenChange={(open) => {
          if (!open) setOpenRecord(null);
        }}
        title="Medical record"
        description={openRecord ? formatDate(openRecord.recordDate) : undefined}
      >
        {openRecord ? (
          <div className="space-y-5">
            <DescriptionList
              columns={1}
              items={[
                {
                  label: 'Patient',
                  value: openRecord.patient ? (
                    <Link
                      to={`/patients/${openRecord.patient.id}`}
                      className="text-accent hover:underline"
                    >
                      {openRecord.patient.fullName} · {openRecord.patient.patientCode}
                    </Link>
                  ) : (
                    <OptionalValue value={null} />
                  ),
                },
                { label: 'Doctor', value: <OptionalValue value={openRecord.doctor?.fullName} /> },
                { label: 'Diagnosis', value: openRecord.diagnosis },
                { label: 'Symptoms', value: <OptionalValue value={openRecord.symptoms} /> },
                { label: 'Examination', value: <OptionalValue value={openRecord.examination} /> },
                { label: 'Treatment notes', value: <OptionalValue value={openRecord.treatmentNotes} /> },
              ]}
            />
            <Separator />
            <div className="space-y-1 text-xs text-subtle">
              <p>
                Recorded {new Date(openRecord.createdAt).toLocaleString()} · last updated{' '}
                {new Date(openRecord.updatedAt).toLocaleString()}
              </p>
              <p>
                Linked to appointment{' '}
                <span className="font-mono">{openRecord.appointmentId.slice(0, 8)}</span>
              </p>
            </div>
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}
