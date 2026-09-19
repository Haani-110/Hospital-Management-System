import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { NotFoundException } from '@nestjs/common';
import { PatientsService } from './patients.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { Gender } from '../generated/prisma/client.js';

type TransactionResult = [number, unknown[]];

describe('PatientsService', () => {
  let findMany: jest.Mock<(args: unknown) => Promise<unknown[]>>;
  let count: jest.Mock<(args: unknown) => Promise<number>>;
  let findUnique: jest.Mock<(args: unknown) => Promise<unknown | null>>;
  let transaction: jest.Mock<(operations: unknown) => Promise<TransactionResult>>;
  let service: PatientsService;

  beforeEach(() => {
    findMany = jest.fn<(args: unknown) => Promise<unknown[]>>().mockResolvedValue([]);
    count = jest.fn<(args: unknown) => Promise<number>>().mockResolvedValue(0);
    findUnique = jest.fn<(args: unknown) => Promise<unknown | null>>().mockResolvedValue(null);
    transaction = jest
      .fn<(operations: unknown) => Promise<TransactionResult>>()
      .mockResolvedValue([0, []]);

    const prisma = {
      patient: { findMany, count, findUnique },
      $transaction: transaction,
    } as unknown as PrismaService;

    service = new PatientsService(prisma);
  });

  it('returns an empty page with correct metadata', async () => {
    count.mockResolvedValue(0);
    findMany.mockResolvedValue([]);
    transaction.mockResolvedValue([0, []]);

    const result = await service.findAll({ page: 1, limit: 20, skip: 0 });

    expect(result.data).toEqual([]);
    expect(result.meta).toEqual({ page: 1, limit: 20, total: 0, totalPages: 0 });
  });

  it('computes pagination metadata from the total count', async () => {
    transaction.mockResolvedValue([42, [{ id: 'p1' }]]);

    const result = await service.findAll({ page: 3, limit: 20, skip: 40 });

    expect(result.meta).toEqual({ page: 3, limit: 20, total: 42, totalPages: 3 });
    expect(findMany).toHaveBeenCalledWith(
      expect.objectContaining({ skip: 40, take: 20, orderBy: { fullName: 'asc' } }),
    );
  });

  it('translates filters into a where clause', async () => {
    transaction.mockResolvedValue([0, []]);

    await service.findAll({ page: 1, limit: 10, skip: 0, active: true, search: 'ada' });

    const whereArg = count.mock.calls[0]?.[0] as { where: Record<string, unknown> };
    expect(whereArg.where).toMatchObject({ active: true });
    expect(whereArg.where.OR).toEqual([
      { fullName: { contains: 'ada', mode: 'insensitive' } },
      { patientCode: { contains: 'ada', mode: 'insensitive' } },
    ]);
  });

  it('only selects public patient columns', async () => {
    transaction.mockResolvedValue([0, []]);

    await service.findAll({ page: 1, limit: 10, skip: 0 });

    const args = findMany.mock.calls[0]?.[0] as { select: Record<string, boolean> };
    expect(args.select).toMatchObject({
      patientCode: true,
      fullName: true,
      dateOfBirth: true,
      gender: true,
    });
    expect(args.select).not.toHaveProperty('deletedAt');
  });

  it('returns a patient by id', async () => {
    const patient = {
      id: 'p1',
      patientCode: 'PAT-000001',
      fullName: 'Ada Lovelace',
      dateOfBirth: new Date('1990-01-01'),
      gender: Gender.FEMALE,
      phone: null,
      email: null,
      address: null,
      emergencyContactName: null,
      emergencyContactPhone: null,
      bloodGroup: null,
      active: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    findUnique.mockResolvedValue(patient);

    await expect(service.findOne('p1')).resolves.toEqual(patient);
  });

  it('throws NotFound for an unknown patient id', async () => {
    findUnique.mockResolvedValue(null);

    await expect(service.findOne('missing')).rejects.toThrow(NotFoundException);
  });
});
