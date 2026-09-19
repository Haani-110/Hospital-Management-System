import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { ReportsService, getTodayRange } from './reports.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { AppointmentStatus, BillStatus } from '../generated/prisma/client.js';

type AggregateResult = { _sum: { totalAmount: { toString(): string } | null } };

class DecimalLike {
  constructor(private readonly value: string) {}
  toString(): string {
    return this.value;
  }
}

describe('getTodayRange', () => {
  it('covers the whole local day with an exclusive upper bound', () => {
    const now = new Date('2026-03-15T14:30:00');
    const { startOfDay, startOfNextDay } = getTodayRange(now);

    expect(startOfDay.getHours()).toBe(0);
    expect(startOfDay.getMinutes()).toBe(0);
    expect(startOfDay.getSeconds()).toBe(0);
    expect(startOfDay.getMilliseconds()).toBe(0);
    expect(startOfNextDay.getTime() - startOfDay.getTime()).toBe(24 * 60 * 60 * 1000);
    expect(startOfDay.getDate()).toBe(15);
    expect(startOfNextDay.getDate()).toBe(16);
  });
});

describe('ReportsService', () => {
  let transaction: jest.Mock<(operations: unknown) => Promise<unknown[]>>;
  let count: jest.Mock<() => Promise<number>>;
  let aggregate: jest.Mock<() => Promise<AggregateResult>>;
  let service: ReportsService;

  function mockResults(
    overrides: Partial<{
      totalPatients: number;
      activeDoctors: number;
      todayAppointments: number;
      pending: number;
      completed: number;
      unpaid: number;
      partial: number;
      revenue: AggregateResult;
    }> = {},
  ) {
    const values = {
      totalPatients: 0,
      activeDoctors: 0,
      todayAppointments: 0,
      pending: 0,
      completed: 0,
      unpaid: 0,
      partial: 0,
      revenue: { _sum: { totalAmount: null } } as AggregateResult,
      ...overrides,
    };
    aggregate.mockResolvedValue(values.revenue);
    transaction.mockResolvedValue([
      values.totalPatients,
      values.activeDoctors,
      values.todayAppointments,
      values.pending,
      values.completed,
      values.unpaid,
      values.partial,
      values.revenue,
    ]);
    return values;
  }

  beforeEach(() => {
    transaction = jest.fn<(operations: unknown) => Promise<unknown[]>>();
    count = jest.fn<() => Promise<number>>().mockResolvedValue(0);
    aggregate = jest
      .fn<() => Promise<AggregateResult>>()
      .mockResolvedValue({ _sum: { totalAmount: null } });

    // The service builds each query eagerly and then batch-executes them,
    // so every model used in the dashboard needs a stub.
    service = new ReportsService({
      patient: { count },
      doctor: { count },
      appointment: { count },
      bill: { count, aggregate },
      $transaction: transaction,
    } as unknown as PrismaService);
  });

  it('returns every dashboard counter', async () => {
    mockResults({
      totalPatients: 12,
      activeDoctors: 4,
      todayAppointments: 3,
      pending: 5,
      completed: 20,
      unpaid: 2,
      partial: 1,
    });

    const summary = await service.getDashboardSummary();

    expect(summary).toEqual({
      totalPatients: 12,
      activeDoctors: 4,
      todayAppointments: 3,
      pendingAppointments: 5,
      completedAppointments: 20,
      unpaidBills: 2,
      partiallyPaidBills: 1,
      todayRevenue: '0',
    });
  });

  it('returns today revenue as a decimal string with two decimals intact', async () => {
    mockResults({ revenue: { _sum: { totalAmount: new DecimalLike('4820.75') } } });

    const summary = await service.getDashboardSummary();

    expect(summary.todayRevenue).toBe('4820.75');
    expect(typeof summary.todayRevenue).toBe('string');
  });

  it('reports zero revenue when no paid bills exist today', async () => {
    mockResults({ revenue: { _sum: { totalAmount: null } } });

    await expect(service.getDashboardSummary()).resolves.toMatchObject({ todayRevenue: '0' });
  });

  it('batches every counter into a single transaction', async () => {
    mockResults();

    await service.getDashboardSummary();

    const operations = transaction.mock.calls[0]?.[0] as unknown[];
    expect(operations).toHaveLength(8);
    expect(count).toHaveBeenCalledTimes(7); // patients, doctors + 5 status counters
    expect(aggregate).toHaveBeenCalledTimes(1); // today's paid revenue
    expect(AppointmentStatus.SCHEDULED).toBe('SCHEDULED');
    expect(BillStatus.PARTIALLY_PAID).toBe('PARTIALLY_PAID');
  });
});
