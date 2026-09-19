import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { CalendarDays, ClipboardList, FileText, Receipt } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { Pagination } from '@/components/ui/pagination';
import { buttonStyles } from '@/components/ui/button-styles';
import {  } from '@/components/ui/button';
import { ListView } from '@/components/data/list-view';
import { AppointmentStatusBadge, BillStatusBadge } from '@/components/data/status-badge';
import {
  appointmentsApi,
  billsApi,
  medicalRecordsApi,
  prescriptionsApi,
} from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { useListQuery } from '@/lib/hooks/use-list-query';
import { formatCurrency, formatDate, formatDateTime } from '@/lib/utils';
import type { Appointment, Bill, MedicalRecord, PaginationMeta, Prescription } from '@/lib/api/types';

export type HistoryKind = 'appointments' | 'medical-records' | 'prescriptions' | 'bills';

const HISTORY_DEFAULTS = { search: '', page: '', limit: '10' };

/** Row count for the current result regardless of which model it holds. */
function readRows(result: HistoryResult | null): unknown[] {
  return result?.rows ?? [];
}

const emptyCopy: Record<HistoryKind, { title: string; description: string; icon: React.ElementType }> = {
  appointments: {
    title: 'No appointments for this patient',
    description: 'Visits scheduled for this patient will be listed here.',
    icon: CalendarDays,
  },
  'medical-records': {
    title: 'No medical records for this patient',
    description: 'Consultation notes, diagnoses and treatments will appear here once recorded.',
    icon: FileText,
  },
  prescriptions: {
    title: 'No prescriptions for this patient',
    description: 'Medication prescribed during consultations will be listed here.',
    icon: ClipboardList,
  },
  bills: {
    title: 'No bills for this patient',
    description: 'Invoices raised for this patient will be listed here.',
    icon: Receipt,
  },
};

interface PatientHistoryProps {
  patientId: string;
  kind: HistoryKind;
  canViewBilling: boolean;
}

/**
 * Discriminated result so each table below is typed against the model it
 * actually renders, even though one hook drives all four tabs.
 */
type HistoryResult =
  | { kind: 'appointments'; rows: Appointment[]; meta: PaginationMeta }
  | { kind: 'medical-records'; rows: MedicalRecord[]; meta: PaginationMeta }
  | { kind: 'prescriptions'; rows: Prescription[]; meta: PaginationMeta }
  | { kind: 'bills'; rows: Bill[]; meta: PaginationMeta };

/** One paginated slice of a patient's history, filtered server-side by `patientId`. */
export function PatientHistory({ patientId, kind, canViewBilling }: PatientHistoryProps) {
  const { page, limit, setPage } = useListQuery(HISTORY_DEFAULTS);

  const request = useCallback(
    async (signal: AbortSignal): Promise<HistoryResult> => {
      const params = { patientId, page, limit };
      switch (kind) {
        case 'appointments': {
          const response = await appointmentsApi.listAppointments(params, signal);
          return { kind, rows: response.data, meta: response.meta };
        }
        case 'medical-records': {
          const response = await medicalRecordsApi.listMedicalRecords(params, signal);
          return { kind, rows: response.data, meta: response.meta };
        }
        case 'prescriptions': {
          const response = await prescriptionsApi.listPrescriptions(params, signal);
          return { kind, rows: response.data, meta: response.meta };
        }
        case 'bills': {
          const response = await billsApi.listBills(params, signal);
          return { kind, rows: response.data, meta: response.meta };
        }
      }
    },
    [kind, patientId, page, limit],
  );

  const { data, error, isInitialLoading, isLoading, refetch } = useApi(request, {
    enabled: kind !== 'bills' || canViewBilling,
  });

  const copy = emptyCopy[kind];

  return (
    <Card className="overflow-hidden">
      <ListView
        isInitialLoading={isInitialLoading}
        error={error}
        isEmpty={readRows(data).length === 0}
        onRetry={refetch}
        isRefreshing={isLoading && !isInitialLoading}
        skeletonColumns={4}
        skeletonRows={5}
        emptyIcon={copy.icon}
        emptyTitle={copy.title}
        emptyDescription={copy.description}
      >
        {data?.kind === 'appointments' ? (
          <TableWrap>
            <Table>
              <caption className="sr-only">Appointments for this patient</caption>
              <THead>
                <TR>
                  <TH>Scheduled</TH>
                  <TH>Doctor</TH>
                  <TH className="hidden md:table-cell">Reason</TH>
                  <TH>Status</TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data.rows.map((appointment) => (
                  <TR key={appointment.id}>
                    <TD className="whitespace-nowrap">{formatDateTime(appointment.scheduledAt)}</TD>
                    <TD className="text-muted">{appointment.doctor?.fullName ?? '—'}</TD>
                    <TD className="hidden md:table-cell text-muted">{appointment.reason ?? '—'}</TD>
                    <TD>
                      <AppointmentStatusBadge status={appointment.status} />
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>
        ) : null}

        {data?.kind === 'medical-records' ? (
          <TableWrap>
            <Table>
              <caption className="sr-only">Medical records for this patient</caption>
              <THead>
                <TR>
                  <TH>Date</TH>
                  <TH>Diagnosis</TH>
                  <TH className="hidden md:table-cell">Doctor</TH>
                  <TH className="hidden lg:table-cell">Treatment</TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data.rows.map((record) => (
                  <TR key={record.id}>
                    <TD className="whitespace-nowrap">{formatDate(record.recordDate)}</TD>
                    <TD className="font-medium text-fg">{record.diagnosis}</TD>
                    <TD className="hidden md:table-cell text-muted">{record.doctor?.fullName ?? '—'}</TD>
                    <TD className="hidden lg:table-cell max-w-sm truncate text-muted">
                      {record.treatmentNotes ?? '—'}
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>
        ) : null}

        {data?.kind === 'prescriptions' ? (
          <TableWrap>
            <Table>
              <caption className="sr-only">Prescriptions for this patient</caption>
              <THead>
                <TR>
                  <TH>Date</TH>
                  <TH>Doctor</TH>
                  <TH>Medicines</TH>
                  <TH className="hidden lg:table-cell">Notes</TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data.rows.map((prescription) => (
                  <TR key={prescription.id}>
                    <TD className="whitespace-nowrap">{formatDate(prescription.prescriptionDate)}</TD>
                    <TD className="text-muted">{prescription.doctor?.fullName ?? '—'}</TD>
                    <TD>
                      {prescription.items.length === 0 ? (
                        <span className="text-subtle">No items</span>
                      ) : (
                        <ul className="space-y-0.5">
                          {prescription.items.map((item) => (
                            <li key={item.id} className="text-[13px] text-fg">
                              {item.medicineName}
                              {item.dosage ? (
                                <span className="text-subtle"> · {item.dosage}</span>
                              ) : null}
                            </li>
                          ))}
                        </ul>
                      )}
                    </TD>
                    <TD className="hidden lg:table-cell max-w-xs truncate text-muted">
                      {prescription.notes ?? '—'}
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </TableWrap>
        ) : null}

        {data?.kind === 'bills' ? (
          <TableWrap>
            <Table>
              <caption className="sr-only">Bills for this patient</caption>
              <THead>
                <TR>
                  <TH>Bill</TH>
                  <TH>Date</TH>
                  <TH>Status</TH>
                  <TH className="text-right">Total</TH>
                  <TH>
                    <span className="sr-only">Actions</span>
                  </TH>
                </TR>
              </THead>
              <TBody className="stagger">
                {data.rows.map((bill) => (
                  <TR key={bill.id}>
                    <TD className="font-mono text-[13px] text-muted">{bill.billNumber}</TD>
                    <TD className="whitespace-nowrap">{formatDate(bill.billDate)}</TD>
                    <TD>
                      <BillStatusBadge status={bill.status} />
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
        ) : null}

        {data ? <Pagination meta={data.meta} onPageChange={setPage} itemLabel={kind.replace('-', ' ')} /> : null}
      </ListView>

      <div className="border-t border-border px-4 py-3">
        <Link
          to={`/${kind}?patientId=${patientId}`}
          className={buttonStyles({ variant: 'ghost', size: 'sm' })}
        >
          Open in {kind.replace('-', ' ')}
          <Badge tone="neutral" className="ml-1">
            full view
          </Badge>
        </Link>
      </div>
    </Card>
  );
}
