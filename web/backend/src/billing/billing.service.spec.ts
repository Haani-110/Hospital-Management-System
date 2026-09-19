import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { NotFoundException } from '@nestjs/common';
import { BillingService } from './billing.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { BillStatus } from '../generated/prisma/client.js';

class DecimalLike {
  constructor(private readonly value: string) {}
  toString(): string {
    return this.value;
  }
}

describe('BillingService', () => {
  let findMany: jest.Mock<(args: unknown) => Promise<unknown[]>>;
  let count: jest.Mock<(args: unknown) => Promise<number>>;
  let findUnique: jest.Mock<(args: unknown) => Promise<unknown | null>>;
  let transaction: jest.Mock<(operations: unknown) => Promise<[number, unknown[]]>>;
  let service: BillingService;

  const billRow = {
    id: 'b1',
    billNumber: 'BILL-000001',
    patientId: 'p1',
    appointmentId: null,
    billDate: new Date('2026-03-15'),
    status: BillStatus.PARTIALLY_PAID,
    subtotal: new DecimalLike('2500.00'),
    discount: new DecimalLike('500.00'),
    totalAmount: new DecimalLike('2000.00'),
    notes: null,
    createdAt: new Date(),
    updatedAt: new Date(),
    patient: { id: 'p1', patientCode: 'PAT-000001', fullName: 'Ada Lovelace' },
    items: [
      {
        id: 'i1',
        description: 'Consultation',
        quantity: 1,
        unitPrice: new DecimalLike('2500.00'),
        amount: new DecimalLike('2500.00'),
      },
    ],
  };

  beforeEach(() => {
    findMany = jest.fn<(args: unknown) => Promise<unknown[]>>().mockResolvedValue([]);
    count = jest.fn<(args: unknown) => Promise<number>>().mockResolvedValue(0);
    findUnique = jest.fn<(args: unknown) => Promise<unknown | null>>().mockResolvedValue(null);
    transaction = jest
      .fn<(operations: unknown) => Promise<[number, unknown[]]>>()
      .mockResolvedValue([0, []]);

    service = new BillingService({
      bill: { findMany, count, findUnique },
      $transaction: transaction,
    } as unknown as PrismaService);
  });

  it('returns monetary values as decimal strings, including line items', async () => {
    transaction.mockResolvedValue([1, [billRow]]);

    const result = await service.findAll({ page: 1, limit: 20, skip: 0 });
    const bill = result.data[0];

    expect(bill?.subtotal).toBe('2500.00');
    expect(bill?.discount).toBe('500.00');
    expect(bill?.totalAmount).toBe('2000.00');
    expect(bill?.items[0]?.unitPrice).toBe('2500.00');
    expect(bill?.items[0]?.amount).toBe('2500.00');
  });

  it('filters by patient, appointment, status and date range', async () => {
    transaction.mockResolvedValue([0, []]);
    const from = new Date('2026-03-01');
    const to = new Date('2026-04-01');

    await service.findAll({
      page: 1,
      limit: 10,
      skip: 0,
      patientId: 'p1',
      appointmentId: 'a1',
      status: BillStatus.UNPAID,
      from,
      to,
    });

    const args = count.mock.calls[0]?.[0] as { where: Record<string, unknown> };
    expect(args.where).toMatchObject({
      patientId: 'p1',
      appointmentId: 'a1',
      status: BillStatus.UNPAID,
      billDate: { gte: from, lt: to },
    });
  });

  it('returns a single bill with decimal strings', async () => {
    findUnique.mockResolvedValue(billRow);

    const bill = await service.findOne('b1');

    expect(bill.totalAmount).toBe('2000.00');
  });

  it('throws NotFound for an unknown bill id', async () => {
    findUnique.mockResolvedValue(null);

    await expect(service.findOne('missing')).rejects.toThrow(NotFoundException);
  });
});
