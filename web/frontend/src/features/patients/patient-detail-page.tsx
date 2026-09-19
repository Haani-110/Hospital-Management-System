import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, FileText } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { buttonStyles } from '@/components/ui/button-styles';
import { Button,  } from '@/components/ui/button';
import { Tabs, TabPanel, type TabItem } from '@/components/ui/tabs';
import { Avatar } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/ui/empty-state';
import { DetailView } from '@/components/data/detail-view';
import { DescriptionList, OptionalValue } from '@/components/data/description-list';
import { ActiveBadge } from '@/components/data/status-badge';
import { PatientHistory } from './patient-history';
import { PatientCareSummary } from './patient-care-summary';
import { patientsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { calculateAge, formatDate, formatDateTime, humanise } from '@/lib/utils';
import { useAuth } from '@/providers/auth-context';
import { Role } from '@/lib/api/types';

const TAB_ITEMS: TabItem[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'appointments', label: 'Appointments' },
  { id: 'records', label: 'Medical records' },
  { id: 'prescriptions', label: 'Prescriptions' },
  { id: 'billing', label: 'Billing' },
];

/**
 * Patient profile.
 *
 * Identity and demographics come from `GET /patients/:id`; the clinical history
 * is assembled from real per-patient queries (`?patientId=`) on the
 * appointments, medical-records, prescriptions and bills endpoints, because the
 * patient resource itself does not nest any relations.
 */
export function PatientDetailPage() {
  const { patientId = '' } = useParams();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');

  const canViewBilling = user?.role === Role.ADMIN || user?.role === Role.RECEPTIONIST;

  const { data: patient, error, isLoading, refetch } = useApi(
    useCallback((signal) => patientsApi.getPatient(patientId, signal), [patientId]),
    { enabled: Boolean(patientId) },
  );

  const tabs = canViewBilling ? TAB_ITEMS : TAB_ITEMS.filter((tab) => tab.id !== 'billing');

  return (
    <div className="space-y-6">
      <div>
        <Link
          to="/patients"
          className="inline-flex items-center gap-1.5 text-[13px] text-muted transition-colors hover:text-fg"
        >
          <ArrowLeft aria-hidden className="size-3.5" />
          All patients
        </Link>
      </div>

      <DetailView
        isLoading={isLoading}
        error={error}
        onRetry={refetch}
        skeleton={
          <Card className="p-6">
            <div className="flex items-center gap-4">
              <Skeleton className="size-14 rounded-full" />
              <div className="flex-1 space-y-2.5">
                <Skeleton className="h-5 w-56" />
                <Skeleton className="h-3 w-36" />
              </div>
            </div>
          </Card>
        }
      >
        {patient ? (
          <>
            <PageHeader
              eyebrow="Patient record"
              title={patient.fullName}
              description={`${patient.patientCode} · registered ${formatDate(patient.createdAt)}`}
              actions={
                <>
                  <ActiveBadge active={patient.active} />
                  <Button variant="outline" size="sm" onClick={refetch}>
                    Refresh
                  </Button>
                </>
              }
            />

            <Tabs idPrefix="patient" items={tabs} value={activeTab} onValueChange={setActiveTab} />

            <TabPanel idPrefix="patient" id="overview" activeId={activeTab}>
              <div className="space-y-6">
                <Card>
                  <CardHeader>
                    <div className="flex items-center gap-4">
                      <Avatar name={patient.fullName} size="lg" />
                      <div className="min-w-0">
                        <CardTitle className="text-base">{patient.fullName}</CardTitle>
                        <CardDescription>
                          {calculateAge(patient.dateOfBirth) !== null
                            ? `${calculateAge(patient.dateOfBirth)} years old · `
                            : ''}
                          {humanise(patient.gender)} · born {formatDate(patient.dateOfBirth)}
                        </CardDescription>
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-2">
                      <Badge tone="neutral">
                        <span className="sr-only">Patient code </span>
                        {patient.patientCode}
                      </Badge>
                      {patient.bloodGroup ? (
                        <Badge tone="accent">
                          <span className="sr-only">Blood group </span>
                          {patient.bloodGroup}
                        </Badge>
                      ) : null}
                    </div>
                  </CardHeader>
                  <CardContent className="pt-1">
                    <Separator className="mb-5" />
                    <DescriptionList
                      items={[
                        { label: 'Date of birth', value: formatDate(patient.dateOfBirth) },
                        { label: 'Gender', value: humanise(patient.gender) },
                        { label: 'Blood group', value: <OptionalValue value={patient.bloodGroup} /> },
                        {
                          label: 'Record status',
                          value: patient.active ? 'Active' : 'Inactive',
                        },
                        { label: 'Phone', value: <OptionalValue value={patient.phone} /> },
                        { label: 'Email', value: <OptionalValue value={patient.email} /> },
                        {
                          label: 'Address',
                          value: <OptionalValue value={patient.address} />,
                          span: true,
                        },
                        {
                          label: 'Emergency contact',
                          value: <OptionalValue value={patient.emergencyContactName} />,
                        },
                        {
                          label: 'Emergency phone',
                          value: <OptionalValue value={patient.emergencyContactPhone} />,
                        },
                        {
                          label: 'Last updated',
                          value: formatDateTime(patient.updatedAt),
                          span: true,
                        },
                      ]}
                    />
                  </CardContent>
                </Card>

                <PatientCareSummary patientId={patient.id} canViewBilling={canViewBilling} />
              </div>
            </TabPanel>

            <TabPanel idPrefix="patient" id="appointments" activeId={activeTab}>
              <PatientHistory
                patientId={patient.id}
                kind="appointments"
                canViewBilling={canViewBilling}
              />
            </TabPanel>
            <TabPanel idPrefix="patient" id="records" activeId={activeTab}>
              <PatientHistory
                patientId={patient.id}
                kind="medical-records"
                canViewBilling={canViewBilling}
              />
            </TabPanel>
            <TabPanel idPrefix="patient" id="prescriptions" activeId={activeTab}>
              <PatientHistory
                patientId={patient.id}
                kind="prescriptions"
                canViewBilling={canViewBilling}
              />
            </TabPanel>
            <TabPanel idPrefix="patient" id="billing" activeId={activeTab}>
              {canViewBilling ? (
                <PatientHistory
                  patientId={patient.id}
                  kind="bills"
                  canViewBilling={canViewBilling}
                />
              ) : null}
            </TabPanel>
          </>
        ) : (
          <EmptyState
            icon={FileText}
            title="Patient not found"
            description="This patient does not exist or has been removed."
            action={
              <Link to="/patients" className={buttonStyles({ variant: 'outline', size: 'sm' })}>
                Back to patients
              </Link>
            }
          />
        )}
      </DetailView>
    </div>
  );
}
