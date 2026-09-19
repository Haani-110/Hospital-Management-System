import { useCallback } from 'react';
import { CheckCircle2, KeyRound, Monitor, Plug, ShieldCheck, TriangleAlert, XCircle } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { DescriptionList, OptionalValue } from '@/components/data/description-list';
import { RoleBadge } from '@/components/data/status-badge';
import { ApiError } from '@/lib/api/client';
import { reportsApi, API_BASE_URL } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { formatDateTime } from '@/lib/utils';
import { Role } from '@/lib/api/types';
import { useAuth } from '@/providers/auth-context';
import { useTheme } from '@/providers/theme-context';

/**
 * Settings.
 *
 * Only what the system can actually do: show the current session, switch the
 * theme, and probe the API. Account management (password change, profile
 * editing, hospital configuration) has no backend support yet and is listed as
 * unavailable rather than faked.
 */
export function SettingsPage() {
  const { user, logout } = useAuth();
  const { theme, setTheme } = useTheme();

  const health = useApi(useCallback((signal) => reportsApi.getHealth(signal), []));

  const roleAccess =
    user?.role === Role.ADMIN
      ? ['Patients, doctors, appointments, records and prescriptions', 'Billing and invoices', 'User accounts and system settings']
      : user?.role === Role.RECEPTIONIST
        ? ['Patients, doctors, appointments, records and prescriptions', 'Billing and invoices', 'Not: user accounts']
        : ['Patients, doctors, appointments, records and prescriptions', 'Not: billing or user accounts'];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="System"
        title="Settings"
        description="Your session, interface preferences and the connection to the hospital API."
      />

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Session</CardTitle>
              <CardDescription>The account currently signed in on this device.</CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            {user ? (
              <>
                <DescriptionList
                  items={[
                    { label: 'Username', value: user.username },
                    { label: 'Role', value: <RoleBadge role={user.role} /> },
                    { label: 'Email', value: <OptionalValue value={user.email} /> },
                    {
                      label: 'Account status',
                      value: user.active ? 'Active' : 'Inactive',
                    },
                    {
                      label: 'Last sign-in',
                      value: user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Not recorded',
                    },
                    { label: 'Member since', value: formatDateTime(user.createdAt) },
                  ]}
                />
                <Separator className="my-5" />
                <Button variant="outline" size="sm" onClick={logout}>
                  <KeyRound aria-hidden className="size-3.5" />
                  Sign out of this device
                </Button>
              </>
            ) : (
              <Skeleton className="h-24 w-full" />
            )}
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <div>
                <CardTitle>Appearance</CardTitle>
                <CardDescription>
                  The interface is designed dark-first; the light theme uses the same tokens.
                </CardDescription>
              </div>
            </CardHeader>
            <CardContent>
              <fieldset>
                <legend className="sr-only">Colour theme</legend>
                <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Colour theme">
                  {(['dark', 'light'] as const).map((option) => (
                    <button
                      key={option}
                      type="button"
                      role="radio"
                      aria-checked={theme === option}
                      onClick={() => setTheme(option)}
                      className={`press flex items-center gap-2 rounded-md border px-3.5 py-2 text-[13px] font-medium transition-colors ${
                        theme === option
                          ? 'border-accent/40 bg-accent-soft text-accent'
                          : 'border-border bg-sunken text-muted hover:text-fg'
                      }`}
                    >
                      <Monitor aria-hidden className="size-3.5" />
                      {option === 'dark' ? 'Dark' : 'Light'}
                      {theme === option ? <CheckCircle2 aria-hidden className="size-3.5" /> : null}
                    </button>
                  ))}
                </div>
              </fieldset>
              <p className="mt-4 text-xs text-subtle">
                The preference is stored in this browser only, and applied before the first paint.
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div>
                <CardTitle>API connection</CardTitle>
                <CardDescription>The backend this interface is calling.</CardDescription>
              </div>
            </CardHeader>
            <CardContent>
              <DescriptionList
                columns={1}
                items={[
                  {
                    label: 'Base URL',
                    value: <span className="font-mono text-[13px] break-all">{API_BASE_URL}</span>,
                  },
                  { label: 'Configured by', value: 'VITE_API_BASE_URL' },
                ]}
              />

              <div className="mt-5 flex flex-wrap items-center gap-3">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={health.refetch}
                  disabled={health.isLoading}
                  isLoading={health.isLoading}
                >
                  <Plug aria-hidden className="size-3.5" />
                  Test connection
                </Button>

                {!health.isLoading && health.data ? (
                  <Badge tone="success" withDot>
                    Reachable · {health.data.status}
                  </Badge>
                ) : null}
                {!health.isLoading && health.error ? (
                  <Badge tone="danger" withDot>
                    {health.error instanceof ApiError && health.error.isNetworkError
                      ? 'Unreachable'
                      : 'Error'}
                  </Badge>
                ) : null}
              </div>

              {health.data ? (
                <p role="status" className="mt-3 text-xs text-subtle">
                  Responded {formatDateTime(health.data.timestamp)} from <code>/health</code>.
                </p>
              ) : null}
              {health.error ? (
                <p role="alert" className="mt-3 text-xs text-danger">
                  {health.error.messages.join(' ')}
                </p>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>What your role can access</CardTitle>
              <CardDescription>
                Navigation mirrors these rules; the API enforces them on every request.
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2.5">
              {roleAccess.map((entry) => (
                <li key={entry} className="flex items-start gap-2.5 text-[13px] text-muted">
                  {entry.startsWith('Not:') ? (
                    <XCircle aria-hidden className="mt-0.5 size-4 shrink-0 text-subtle" />
                  ) : (
                    <ShieldCheck aria-hidden className="mt-0.5 size-4 shrink-0 text-success" />
                  )}
                  <span>{entry}</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Not available yet</CardTitle>
              <CardDescription>Functions the backend does not expose in this build.</CardDescription>
            </div>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2.5">
              {[
                'Changing your own password',
                'Editing your profile details',
                'Hospital-wide configuration (wards, departments, tariffs)',
                'Creating or amending clinical, billing or staff records',
              ].map((item) => (
                <li key={item} className="flex items-start gap-2.5 text-[13px] text-muted">
                  <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0 text-warning" />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
            <p className="mt-4 text-xs text-subtle">
              These appear here rather than as disabled buttons that would mislead.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
