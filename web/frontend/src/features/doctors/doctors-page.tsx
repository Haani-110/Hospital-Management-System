import { useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Stethoscope } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { SearchInput } from '@/components/data/search-input';
import { ListView } from '@/components/data/list-view';
import { ActiveBadge } from '@/components/data/status-badge';
import { Avatar } from '@/components/ui/avatar';
import { doctorsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { formatCurrency } from '@/lib/utils';

const DOCTOR_FILTERS = { search: '', active: '', page: '', limit: '20' };

/**
 * Medical staff directory backed by `GET /doctors`.
 *
 * The list endpoint also accepts `departmentId`, but the backend exposes no
 * departments endpoint yet, so a department dropdown would have no truthful
 * options to populate. Department is therefore displayed per doctor instead.
 */
export function DoctorsPage() {
  const { values, debouncedSearch, page, limit, setPage, setFilter, setSearch } =
    useListQuery(DOCTOR_FILTERS);

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
    useCallback((signal) => doctorsApi.listDoctors(query, signal), [query]),
  );

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Clinical"
        title="Doctors"
        description="Consultants on staff, their departments and consultation fees."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Card className="overflow-hidden">
        <FilterBar>
          <SearchInput
            id="doctor-search"
            label="Search doctors"
            className="w-full sm:max-w-xs"
            placeholder="Name or specialization…"
            value={values.search}
            onValueChange={setSearch}
          />
          <FilterGroup>
            <label htmlFor="doctor-status" className="sr-only">
              Filter by staff status
            </label>
            <Select
              id="doctor-status"
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
          skeletonColumns={4}
          emptyIcon={Stethoscope}
          emptyTitle={values.search || values.active ? 'No doctors match these filters' : 'No doctors yet'}
          emptyDescription={
            values.search || values.active
              ? 'Try another search term or clear the status filter.'
              : 'Doctors appear here once their records exist. The API exposes read-only doctor endpoints today, so staff are added directly in the backend or through its seed script.'
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
              <caption className="sr-only">Medical staff directory, page {page}</caption>
              <THead>
                <TR>
                  <TH>Doctor</TH>
                  <TH>Department</TH>
                  <TH className="hidden lg:table-cell">Contact</TH>
                  <TH className="hidden md:table-cell">Consultation fee</TH>
                  <TH>Status</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((doctor) => (
                  <TR key={doctor.id}>
                    <TD>
                      <div className="flex items-center gap-3">
                        <Avatar name={doctor.fullName} size="sm" />
                        <div className="min-w-0">
                          <Link
                            to={`/doctors/${doctor.id}`}
                            className="block truncate font-medium text-fg hover:text-accent"
                          >
                            {doctor.fullName}
                          </Link>
                          <p className="truncate text-xs text-subtle">
                            {doctor.specialization ?? 'General practice'}
                          </p>
                        </div>
                      </div>
                    </TD>
                    <TD className="text-muted">{doctor.department?.name ?? '—'}</TD>
                    <TD className="hidden lg:table-cell text-muted">
                      <span className="block">{doctor.phone ?? '—'}</span>
                      <span className="block text-xs text-subtle">{doctor.email ?? ''}</span>
                    </TD>
                    <TD className="hidden md:table-cell tabular">
                      {formatCurrency(doctor.consultationFee)}
                    </TD>
                    <TD>
                      <ActiveBadge active={doctor.active} />
                    </TD>
                    <TD className="text-right">
                      <Link
                        to={`/doctors/${doctor.id}`}
                        className="text-[13px] font-medium text-accent hover:underline"
                      >
                        View
                        <span className="sr-only"> profile for {doctor.fullName}</span>
                      </Link>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? <Pagination meta={data.meta} onPageChange={setPage} itemLabel="doctors" /> : null}
        </ListView>
      </Card>
    </div>
  );
}
