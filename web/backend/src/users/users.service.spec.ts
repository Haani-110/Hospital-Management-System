import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { NotFoundException } from '@nestjs/common';
import { UsersService } from './users.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { Role } from '../generated/prisma/client.js';

describe('UsersService', () => {
  let findMany: jest.Mock<(args: unknown) => Promise<unknown[]>>;
  let count: jest.Mock<(args: unknown) => Promise<number>>;
  let findUnique: jest.Mock<(args: unknown) => Promise<unknown | null>>;
  let transaction: jest.Mock<(operations: unknown) => Promise<[number, unknown[]]>>;
  let service: UsersService;

  beforeEach(() => {
    findMany = jest.fn<(args: unknown) => Promise<unknown[]>>().mockResolvedValue([]);
    count = jest.fn<(args: unknown) => Promise<number>>().mockResolvedValue(0);
    findUnique = jest.fn<(args: unknown) => Promise<unknown | null>>().mockResolvedValue(null);
    transaction = jest
      .fn<(operations: unknown) => Promise<[number, unknown[]]>>()
      .mockResolvedValue([0, []]);

    service = new UsersService({
      user: { findMany, count, findUnique },
      $transaction: transaction,
    } as unknown as PrismaService);
  });

  it('omits the password hash at the query level', async () => {
    transaction.mockResolvedValue([0, []]);

    await service.findAll({ page: 1, limit: 20, skip: 0 });

    const args = findMany.mock.calls[0]?.[0] as { omit: Record<string, boolean> };
    expect(args.omit).toEqual({ passwordHash: true });
  });

  it('omits the password hash when fetching a single user', async () => {
    findUnique.mockResolvedValue({ id: 'u1', username: 'admin' });

    await service.findOne('u1');

    const args = findUnique.mock.calls[0]?.[0] as { omit: Record<string, boolean> };
    expect(args.omit).toEqual({ passwordHash: true });
  });

  it('filters by role, active flag and search term', async () => {
    transaction.mockResolvedValue([0, []]);

    await service.findAll({
      page: 1,
      limit: 10,
      skip: 0,
      role: Role.DOCTOR,
      active: true,
      search: 'grace',
    });

    const args = count.mock.calls[0]?.[0] as { where: Record<string, unknown> };
    expect(args.where).toMatchObject({ role: Role.DOCTOR, active: true });
    expect(args.where.OR).toEqual([
      { username: { contains: 'grace', mode: 'insensitive' } },
      { email: { contains: 'grace', mode: 'insensitive' } },
    ]);
  });

  it('sorts users by username and applies pagination', async () => {
    transaction.mockResolvedValue([0, []]);

    await service.findAll({ page: 2, limit: 25, skip: 25 });

    expect(findMany).toHaveBeenCalledWith(
      expect.objectContaining({ orderBy: { username: 'asc' }, skip: 25, take: 25 }),
    );
  });

  it('throws NotFound for an unknown user id', async () => {
    findUnique.mockResolvedValue(null);

    await expect(service.findOne('missing')).rejects.toThrow(NotFoundException);
  });
});
