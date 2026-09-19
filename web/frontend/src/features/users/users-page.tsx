import { useCallback, useMemo, useState } from 'react';
import { ShieldCheck, UserCog } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { Dialog } from '@/components/ui/dialog';
import { Alert } from '@/components/ui/alert';
import { Avatar } from '@/components/ui/avatar';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { FilterBar, FilterGroup } from '@/components/data/filter-bar';
import { SearchInput } from '@/components/data/search-input';
import { ListView } from '@/components/data/list-view';
import { ActiveBadge, RoleBadge } from '@/components/data/status-badge';
import { DescriptionList, OptionalValue } from '@/components/data/description-list';
import { usersApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { formatDateTime } from '@/lib/utils';
import { Role } from '@/lib/api/types';

const USER_FILTERS = { search: '', role: '', active: '', page: '', limit: '20' };

const ROLE_OPTIONS = [
  { value: '', label: 'All roles' },
  { value: Role.ADMIN, label: 'Administrators' },
  { value: Role.DOCTOR, label: 'Doctors' },
  { value: Role.RECEPTIONIST, label: 'Receptionists' },
];

/**
 * User accounts — ADMIN only.
 *
 * The route is gated for administrators and the API enforces the same rule with
 * a 403 for everyone else, so the restriction holds even if the screen is
 * reached directly.
 */
export function UsersPage() {
  const { values, debouncedSearch, page, limit, setPage, setFilter, setSearch } =
    useListQuery(USER_FILTERS);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  const query = useMemo(
    () => ({
      page,
      limit,
      search: debouncedSearch || undefined,
      role: (values.role || undefined) as Role | undefined,
      active: values.active === '' ? undefined : values.active === 'true',
    }),
    [page, limit, debouncedSearch, values.role, values.active],
  );

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(
    useCallback((signal) => usersApi.listUsers(query, signal), [query]),
  );

  const hasFilters = Boolean(values.search || values.role || values.active);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="System"
        title="Users"
        description="Accounts that can sign in to the system, with their role and status."
        actions={
          <Button variant="outline" size="sm" onClick={refetch} disabled={isLoading}>
            Refresh
          </Button>
        }
      />

      <Alert tone="info" title="Accounts are provisioned on the backend">
        Creating users, changing roles and resetting passwords are administrative operations the API
        does not expose yet, so this screen is read-only by design.
      </Alert>

      <Card className="overflow-hidden">
        <FilterBar>
          <SearchInput
            id="user-search"
            label="Search users by username or email"
            className="w-full sm:max-w-xs"
            placeholder="Username or email…"
            value={values.search}
            onValueChange={setSearch}
          />
          <FilterGroup>
            <label htmlFor="user-role" className="sr-only">
              Filter by role
            </label>
            <Select
              id="user-role"
              className="w-full sm:w-40"
              value={values.role}
              onChange={(event) => setFilter('role', event.target.value)}
              options={ROLE_OPTIONS}
            />
            <label htmlFor="user-status" className="sr-only">
              Filter by account status
            </label>
            <Select
              id="user-status"
              className="w-full sm:w-40"
              value={values.active}
              onChange={(event) => setFilter('active', event.target.value)}
              options={[
                { value: '', label: 'All statuses' },
                { value: 'true', label: 'Active only' },
                { value: 'false', label: 'Inactive only' },
              ]}
            />
            {hasFilters ? (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch('');
                  setFilter('role', '');
                  setFilter('active', '');
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
          emptyIcon={ShieldCheck}
          emptyTitle={hasFilters ? 'No users match these filters' : 'No user accounts'}
          emptyDescription={
            hasFilters
              ? 'Try a different search term, role or status.'
              : 'User accounts are created by the backend seed script or administrator tooling.'
          }
        >
          <TableWrap>
            <Table>
              <caption className="sr-only">User accounts, page {page}</caption>
              <THead>
                <TR>
                  <TH>User</TH>
                  <TH>Role</TH>
                  <TH className="hidden md:table-cell">Email</TH>
                  <TH className="hidden lg:table-cell">Last sign-in</TH>
                  <TH>Status</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data?.data.map((user) => (
                  <TR key={user.id}>
                    <TD>
                      <div className="flex items-center gap-3">
                        <Avatar name={user.username} size="sm" />
                        <span className="font-medium text-fg">{user.username}</span>
                      </div>
                    </TD>
                    <TD>
                      <RoleBadge role={user.role} />
                    </TD>
                    <TD className="hidden md:table-cell text-muted">{user.email ?? '—'}</TD>
                    <TD className="hidden lg:table-cell text-muted">
                      {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Never'}
                    </TD>
                    <TD>
                      <ActiveBadge active={user.active} />
                    </TD>
                    <TD className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => setSelectedUserId(user.id)}>
                        Details
                        <span className="sr-only"> for {user.username}</span>
                      </Button>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>

          {data ? <Pagination meta={data.meta} onPageChange={setPage} itemLabel="users" /> : null}
        </ListView>
      </Card>

      <UserDetailDialog userId={selectedUserId} onClose={() => setSelectedUserId(null)} />
    </div>
  );
}

/** Loads a single account via `GET /users/:id` so the dialog reflects the API. */
function UserDetailDialog({
  userId,
  onClose,
}: {
  userId: string | null;
  onClose: () => void;
}) {
  const { data, error, isLoading } = useApi(
    useCallback((signal) => usersApi.getUser(userId ?? '', signal), [userId]),
    { enabled: Boolean(userId) },
  );

  return (
    <Dialog
      open={userId !== null}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      title="User account"
      description={data?.username}
    >
      {isLoading ? (
        <div className="space-y-3" role="status" aria-label="Loading account">
          <Skeleton className="h-3.5 w-40" />
          <Skeleton className="h-3.5 w-56" />
          <Skeleton className="h-3.5 w-32" />
        </div>
      ) : error ? (
        <Alert tone="danger" title="Could not load this account">
          {error.messages.join(' ')}
        </Alert>
      ) : data ? (
        <div className="space-y-5">
          <div className="flex items-center gap-3">
            <Avatar name={data.username} />
            <div>
              <p className="text-sm font-medium text-fg">{data.username}</p>
              <div className="mt-1 flex items-center gap-2">
                <RoleBadge role={data.role} />
                <ActiveBadge active={data.active} />
              </div>
            </div>
          </div>
          <DescriptionList
            columns={1}
            items={[
              { label: 'Email', value: <OptionalValue value={data.email} /> },
              {
                label: 'Last sign-in',
                value: data.lastLoginAt ? formatDateTime(data.lastLoginAt) : 'Never',
              },
              { label: 'Created', value: formatDateTime(data.createdAt) },
              { label: 'Last updated', value: formatDateTime(data.updatedAt) },
              { label: 'Account ID', value: <span className="font-mono text-[13px]">{data.id}</span> },
            ]}
          />
          <p className="flex items-center gap-2 text-xs text-subtle">
            <UserCog aria-hidden className="size-3.5" />
            Password hashes are never returned by the API, so they are never displayed here.
          </p>
        </div>
      ) : null}
    </Dialog>
  );
}
