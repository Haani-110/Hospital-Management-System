import { useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Users } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { SearchInput } from '@/components/data/search-input';
import { ListView } from '@/components/data/list-view';
import { ActiveBadge } from '@/components/data/status-badge';
import { patientsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { calculateAge, formatDate, humanise } from '@/lib/utils';

const PATIENT_FILTERS = { search: '', active: '', page: '', limit: '20' };

/**
 * Patient registry — paginated, searchable list backed by `GET /patients`.
 *
 * There is no create form because the backend exposes no write endpoint for
 * patients yet; the empty state says so rather than offering a broken action.
 */
export function PatientsPage() {
  const { values, debouncedSearch, page, limit, setPage, setFilter, setSearch, isSearchPending } =
    useListQuery(PATIENT_FILTERS);

  const query = useMemo(
    () => ({
      page,
      limit,
      search: debouncedSearch || undefined,
      active: values.active === '' ? undefined : values.active === 'true',
    }),
    [page, limit, debouncedSearch, values.active],
  );

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback((signal) => patientsApi.listPatients(query, signal), [query]),
  );

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Clinical"
        title="Patients"
        description="Every registered patient with their demographics and contact details."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Card className="overflow-hidden">
        <FilterBar>
          <SearchInput
            id="patient-search"
            label="Search patients"
            className="w-full sm:max-w-xs"
            placeholder="Name or patient code…"
            value={values.search}
            onValueChange={setSearch}
          />
          <FilterGroup>
            <label htmlFor="patient-status" className="sr-only">
              Filter by record status
            </label>
            <Select
              id="patient-status"
              className="w-full sm:w-40"
              value={values.active}
              onChange={(event) => setFilter('active', event.target.value)}
              options={[
                { value: '', label: 'All statuses' },
                { value: 'true', label: 'Active only' },
                { value: 'false', label: 'Inactive only' },
              ]}
            />
          </FilterGroup>
        </FilterBar>

        <ListView
          isInitialLoading={isInitialLoading}
          error={error}
          isEmpty={data?.data.length === 0}
          onRetry={refetch}
          isRefreshing={isLoading && !isInitialLoading}
          skeletonColumns={5}
          emptyIcon={Users}
          emptyTitle={values.search || values.active ? 'No patients match these filters' : 'No patients yet'}
          emptyDescription={
            values.search || values.active
              ? 'Try a different search term or clear the status filter.'
              : 'Patients appear here as soon as they are registered. The API currently exposes read-only patient endpoints, so records are created directly in the backend or via its seed script.'
          }
          emptyAction={
            values.search || values.active ? (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSearch('');
                  setFilter('active', '');
                }}
              >
                Clear filters
              </Button>
            ) : undefined
          }
        >
          <TableWrap>
            <Table>
              <caption className="sr-only">
                Registered patients, page {page}. Search is case-insensitive across names and
                patient codes.
              </caption>
              <THead>
                <TR>
                  <TH>Patient</TH>
                  <TH>Code</TH>
                  <TH className="hidden md:table-cell">Age / Gender</TH>
                  <TH className="hidden lg:table-cell">Phone</TH>
                  <TH className="hidden xl:table-cell">Blood group</TH>
                  <TH>Status</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((patient) => (
                  <TR key={patient.id}>
                    <TD>
                      <Link
                        to={`/patients/${patient.id}`}
                        className="font-medium text-fg hover:text-accent focus-visible:text-accent"
                      >
                        {patient.fullName}
                      </Link>
                      <p className="mt-0.5 text-xs text-subtle">
                        Registered {formatDate(patient.createdAt)}
                      </p>
                    </TD>
                    <TD className="font-mono text-[13px] text-muted">{patient.patientCode}</TD>
                    <TD className="hidden md:table-cell text-muted">
                      {calculateAge(patient.dateOfBirth) ?? '—'} · {humanise(patient.gender)}
                    </TD>
                    <TD className="hidden lg:table-cell text-muted">{patient.phone ?? '—'}</TD>
                    <TD className="hidden xl:table-cell">
                      {patient.bloodGroup ? (
                        <Badge tone="neutral">{patient.bloodGroup}</Badge>
                      ) : (
                        <span className="text-subtle">—</span>
                      )}
                    </TD>
                    <TD>
                      <ActiveBadge active={patient.active} />
                    </TD>
                    <TD className="text-right">
                      <Link
                        to={`/patients/${patient.id}`}
                        className="text-[13px] font-medium text-accent hover:underline"
                      >
                        View
                        <span className="sr-only"> profile for {patient.fullName}</span>
                      </Link>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? <Pagination meta={data.meta} onPageChange={setPage} itemLabel="patients" /> : null}
        </ListView>

        {isSearchPending && data ? (
          <p role="status" className="sr-only">
            Updating results
          </p>
        ) : null}
      </Card>
    </div>
  );
}
