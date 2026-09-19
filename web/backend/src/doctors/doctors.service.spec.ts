import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { NotFoundException } from '@nestjs/common';
import { DoctorsService } from './doctors.service.js';
import { PrismaService } from '../prisma/prisma.service.js';

/** Mimics Prisma's Decimal: JSON-serialisable but not a JS number. */
class DecimalLike {
  constructor(private readonly value: string) {}
  toString(): string {
    return this.value;
  }
}

describe('DoctorsService', () => {
  let findMany: jest.Mock<(args: unknown) => Promise<unknown[]>>;
  let count: jest.Mock<(args: unknown) => Promise<number>>;
  let findUnique: jest.Mock<(args: unknown) => Promise<unknown | null>>;
  let transaction: jest.Mock<(operations: unknown) => Promise<[number, unknown[]]>>;
  let service: DoctorsService;

  const doctorRow = {
    id: 'd1',
    userId: null,
    departmentId: 'dep1',
    fullName: 'Dr. Grace Hopper',
    specialization: 'Cardiology',
    phone: null,
    email: null,
    consultationFee: new DecimalLike('1500.50'),
    active: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    department: { id: 'dep1', name: 'Cardiology' },
  };

  beforeEach(() => {
    findMany = jest.fn<(args: unknown) => Promise<unknown[]>>().mockResolvedValue([]);
    count = jest.fn<(args: unknown) => Promise<number>>().mockResolvedValue(0);
    findUnique = jest.fn<(args: unknown) => Promise<unknown | null>>().mockResolvedValue(null);
    transaction = jest
      .fn<(operations: unknown) => Promise<[number, unknown[]]>>()
      .mockResolvedValue([0, []]);

    service = new DoctorsService({
      doctor: { findMany, count, findUnique },
      $transaction: transaction,
    } as unknown as PrismaService);
  });

  it('serialises the consultation fee as a decimal string', async () => {
    transaction.mockResolvedValue([1, [doctorRow]]);

    const result = await service.findAll({ page: 1, limit: 20, skip: 0 });

    expect(result.data[0]?.consultationFee).toBe('1500.50');
    expect(typeof result.data[0]?.consultationFee).toBe('string');
  });

  it('includes the department summary and pagination metadata', async () => {
    transaction.mockResolvedValue([1, [doctorRow]]);

    const result = await service.findAll({ page: 2, limit: 5, skip: 5 });

    expect(result.data[0]?.department).toEqual({ id: 'dep1', name: 'Cardiology' });
    expect(result.meta).toEqual({ page: 2, limit: 5, total: 1, totalPages: 1 });
  });

  it('filters by department, active flag and search term', async () => {
    transaction.mockResolvedValue([0, []]);

    await service.findAll({
      page: 1,
      limit: 10,
      skip: 0,
      departmentId: 'dep1',
      active: false,
      search: 'cardio',
    });

    const args = count.mock.calls[0]?.[0] as { where: Record<string, unknown> };
    expect(args.where).toMatchObject({ departmentId: 'dep1', active: false });
    expect(args.where.OR).toEqual([
      { fullName: { contains: 'cardio', mode: 'insensitive' } },
      { specialization: { contains: 'cardio', mode: 'insensitive' } },
    ]);
  });

  it('returns a doctor by id with the decimal fee as a string', async () => {
    findUnique.mockResolvedValue(doctorRow);

    const doctor = await service.findOne('d1');

    expect(doctor.consultationFee).toBe('1500.50');
  });

  it('throws NotFound for an unknown doctor id', async () => {
    findUnique.mockResolvedValue(null);

    await expect(service.findOne('missing')).rejects.toThrow(NotFoundException);
  });
});
