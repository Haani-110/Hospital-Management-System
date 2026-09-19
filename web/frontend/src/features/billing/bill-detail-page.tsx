import { useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Receipt } from 'lucide-react';
import { PageHeader } from '@/components/layout/page-header';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { buttonStyles } from '@/components/ui/button-styles';
import {  } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/ui/empty-state';
import { Table, TableWrap, TBody, TD, TH, THead, TR } from '@/components/ui/table';
import { DetailView } from '@/components/data/detail-view';
import { DescriptionList, OptionalValue } from '@/components/data/description-list';
import { BillStatusBadge } from '@/components/data/status-badge';
import { billsApi } from '@/lib/api';
import { useApi } from '@/lib/hooks/use-api';
import { formatCurrency, formatDate } from '@/lib/utils';

/** Itemised invoice view backed by `GET /bills/:id`. */
export function BillDetailPage() {
  const { billId = '' } = useParams();

  const { data: bill, error, isLoading, refetch } = useApi(
    useCallback((signal) => billsApi.getBill(billId, signal), [billId]),
    { enabled: Boolean(billId) },
  );

  const itemTotal = bill
    ? bill.items.reduce((sum, item) => sum + Number(item.amount), 0).toFixed(2)
    : null;

  return (
    <div className="space-y-6">
      <div>
        <Link
          to="/billing"
          className="inline-flex items-center gap-1.5 text-[13px] text-muted transition-colors hover:text-fg"
        >
          <ArrowLeft aria-hidden className="size-3.5" />
          All bills
        </Link>
      </div>

      <DetailView
        isLoading={isLoading}
        error={error}
        onRetry={refetch}
        skeleton={
          <Card className="p-6">
            <Skeleton className="h-5 w-40" />
            <Skeleton className="mt-3 h-3 w-64" />
            <Skeleton className="mt-8 h-24 w-full" />
          </Card>
        }
      >
        {bill ? (
          <>
            <PageHeader
              eyebrow="Invoice"
              title={bill.billNumber}
              description={`Issued ${formatDate(bill.billDate)}${
                bill.patient ? ` to ${bill.patient.fullName} (${bill.patient.patientCode})` : ''
              }`}
              actions={<BillStatusBadge status={bill.status} />}
            />

            <div className="grid gap-4 lg:grid-cols-3">
              <Card className="lg:col-span-2">
                <CardHeader>
                  <div>
                    <CardTitle>Charges</CardTitle>
                    <CardDescription>
                      {bill.items.length} line item{bill.items.length === 1 ? '' : 's'}
                    </CardDescription>
                  </div>
                </CardHeader>
                <CardContent className="px-0 pb-0">
                  {bill.items.length === 0 ? (
                    <EmptyState
                      icon={Receipt}
                      title="No line items on this bill"
                      description="This invoice has no recorded charges."
                    />
                  ) : (
                    <TableWrap>
                      <Table>
                        <caption className="sr-only">Charges on bill {bill.billNumber}</caption>
                        <THead>
                          <TR>
                            <TH>Description</TH>
                            <TH className="text-right">Qty</TH>
                            <TH className="text-right">Unit price</TH>
                            <TH className="text-right">Amount</TH>
                          </TR>
                        </THead>
                        <TBody>
                          {bill.items.map((item) => (
                            <TR key={item.id}>
                              <TD>{item.description}</TD>
                              <TD className="text-right tabular">{item.quantity}</TD>
                              <TD className="text-right tabular">
                                {formatCurrency(item.unitPrice)}
                              </TD>
                              <TD className="text-right font-medium tabular">
                                {formatCurrency(item.amount)}
                              </TD>
                            </TR>
                          ))}
                        </TBody>
                      </Table>
                    </TableWrap>
                  )}

                  <div className="border-t border-border px-5 py-4">
                    <dl className="ml-auto max-w-xs space-y-2 text-sm">
                      <div className="flex justify-between gap-6">
                        <dt className="text-muted">Items total</dt>
                        <dd className="tabular">{itemTotal ? formatCurrency(itemTotal) : '—'}</dd>
                      </div>
                      <div className="flex justify-between gap-6">
                        <dt className="text-muted">Subtotal</dt>
                        <dd className="tabular">{formatCurrency(bill.subtotal)}</dd>
                      </div>
                      <div className="flex justify-between gap-6">
                        <dt className="text-muted">Discount</dt>
                        <dd className="tabular">−{formatCurrency(bill.discount)}</dd>
                      </div>
                      <Separator />
                      <div className="flex justify-between gap-6">
                        <dt className="font-medium text-fg">Total amount</dt>
                        <dd className="text-base font-semibold tabular">
                          {formatCurrency(bill.totalAmount)}
                        </dd>
                      </div>
                    </dl>
                  </div>
                </CardContent>
              </Card>

              <div className="space-y-4">
                <Card>
                  <CardHeader>
                    <CardTitle>Invoice details</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <DescriptionList
                      columns={1}
                      items={[
                        {
                          label: 'Patient',
                          value: bill.patient ? (
                            <Link
                              to={`/patients/${bill.patient.id}`}
                              className="text-accent hover:underline"
                            >
                              {bill.patient.fullName} · {bill.patient.patientCode}
                            </Link>
                          ) : (
                            <OptionalValue value={null} />
                          ),
                        },
                        { label: 'Bill date', value: formatDate(bill.billDate) },
                        {
                          label: 'Payment status',
                          value: <BillStatusBadge status={bill.status} />,
                        },
                        {
                          label: 'Linked appointment',
                          value: bill.appointmentId ? (
                            <span className="font-mono text-[13px]">
                              {bill.appointmentId.slice(0, 8)}
                            </span>
                          ) : (
                            <span className="text-subtle">Not linked</span>
                          ),
                        },
                        { label: 'Notes', value: <OptionalValue value={bill.notes} /> },
                      ]}
                    />
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Payment</CardTitle>
                    <CardDescription>
                      Payment recording is not available in this build.
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <p className="text-[13px] leading-relaxed text-muted">
                      The API exposes bills as read-only, so receipting or adjusting a payment cannot
                      be done from the interface yet. Payments are recorded directly in the backend
                      until a write endpoint exists.
                    </p>
                    <Badge tone="neutral" className="mt-4">
                      Read-only
                    </Badge>
                  </CardContent>
                </Card>
              </div>
            </div>
          </>
        ) : error ? null : (
          <EmptyState
            icon={Receipt}
            title="Bill not found"
            description="This invoice does not exist or has been removed."
            action={
              <Link to="/billing" className={buttonStyles({ variant: 'outline', size: 'sm' })}>
                Back to billing
              </Link>
            }
          />
        )}
      </DetailView>
    </div>
  );
}
