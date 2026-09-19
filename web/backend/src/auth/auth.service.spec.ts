import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { AuthService } from './auth.service.js';
import { AppConfigService } from '../config/app-config.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { hashPassword } from '../common/security/password.util.js';
import { Role } from '../generated/prisma/client.js';
import type { JwtPayload } from '../common/types/authenticated-user.type.js';

const ROUNDS = 4;

type UserRow = {
  id: string;
  username: string;
  email: string | null;
  passwordHash: string;
  role: Role;
  active: boolean;
  lastLoginAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
};

type FindUniqueArgs = { where: { username?: string; id?: string } };
type UpdateArgs = { where: { id: string }; data: { lastLoginAt: Date } };

function buildUser(overrides: Partial<UserRow> = {}): UserRow {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    username: 'admin',
    email: 'admin@example.test',
    passwordHash: '',
    role: Role.ADMIN,
    active: true,
    lastLoginAt: null,
    createdAt: new Date('2026-01-01T00:00:00Z'),
    updatedAt: new Date('2026-01-01T00:00:00Z'),
    ...overrides,
  };
}

describe('AuthService', () => {
  let service: AuthService;
  let findUnique: jest.Mock<(args: FindUniqueArgs) => Promise<UserRow | null>>;
  let update: jest.Mock<(args: UpdateArgs) => Promise<UserRow>>;
  let signAsync: jest.Mock<(payload: JwtPayload) => Promise<string>>;

  beforeEach(() => {
    findUnique = jest.fn<(args: FindUniqueArgs) => Promise<UserRow | null>>();
    update = jest.fn<(args: UpdateArgs) => Promise<UserRow>>();
    signAsync = jest.fn<(payload: JwtPayload) => Promise<string>>();
    signAsync.mockResolvedValue('signed.jwt.token');

    const prisma = { user: { findUnique, update } } as unknown as PrismaService;
    const jwt = { signAsync } as unknown as JwtService;
    const config = { bcryptRounds: ROUNDS, jwtExpiresIn: '15m' } as unknown as AppConfigService;

    service = new AuthService(prisma, jwt, config);
  });

  it('issues a token and returns the profile (never the password hash) on valid credentials', async () => {
    const passwordHash = await hashPassword('admin-password', ROUNDS);
    const user = buildUser({ passwordHash });
    findUnique.mockResolvedValue(user);
    update.mockResolvedValue({ ...user, lastLoginAt: new Date() });

    const result = await service.login({ username: 'admin', password: 'admin-password' });

    expect(result.accessToken).toBe('signed.jwt.token');
    expect(result.tokenType).toBe('Bearer');
    expect(result.expiresIn).toBe('15m');
    expect(result.user).toMatchObject({ id: user.id, username: 'admin', role: Role.ADMIN });
    expect(JSON.stringify(result)).not.toContain(passwordHash);
    expect(result.user).not.toHaveProperty('passwordHash');
  });

  it('signs a payload carrying the user id, username and role', async () => {
    const passwordHash = await hashPassword('admin-password', ROUNDS);
    findUnique.mockResolvedValue(buildUser({ passwordHash }));
    update.mockResolvedValue(buildUser({ passwordHash }));

    await service.login({ username: 'admin', password: 'admin-password' });

    expect(signAsync).toHaveBeenCalledWith({
      sub: '11111111-1111-1111-1111-111111111111',
      username: 'admin',
      role: Role.ADMIN,
    });
  });

  it('records the last login time on success', async () => {
    const passwordHash = await hashPassword('admin-password', ROUNDS);
    findUnique.mockResolvedValue(buildUser({ passwordHash }));
    update.mockResolvedValue(buildUser({ passwordHash, lastLoginAt: new Date() }));

    await service.login({ username: 'admin', password: 'admin-password' });

    expect(update).toHaveBeenCalledWith(
      expect.objectContaining({ data: { lastLoginAt: expect.any(Date) } }),
    );
  });

  it('rejects a wrong password and does not issue a token', async () => {
    findUnique.mockResolvedValue(
      buildUser({ passwordHash: await hashPassword('the-real-password', ROUNDS) }),
    );

    await expect(service.login({ username: 'admin', password: 'wrong-password' })).rejects.toThrow(
      UnauthorizedException,
    );
    expect(signAsync).not.toHaveBeenCalled();
  });

  it('rejects an inactive account even with the correct password', async () => {
    findUnique.mockResolvedValue(
      buildUser({ passwordHash: await hashPassword('admin-password', ROUNDS), active: false }),
    );

    await expect(service.login({ username: 'admin', password: 'admin-password' })).rejects.toThrow(
      UnauthorizedException,
    );
    expect(signAsync).not.toHaveBeenCalled();
  });

  it('gives the same error message for unknown users and wrong passwords', async () => {
    findUnique.mockResolvedValue(null);
    const unknownUserError = await service
      .login({ username: 'ghost', password: 'whatever-password' })
      .catch((error: UnauthorizedException) => error.message);

    findUnique.mockResolvedValue(
      buildUser({ passwordHash: await hashPassword('the-real-password', ROUNDS) }),
    );
    const wrongPasswordError = await service
      .login({ username: 'admin', password: 'wrong-password' })
      .catch((error: UnauthorizedException) => error.message);

    expect(unknownUserError).toBe('Invalid username or password');
    expect(wrongPasswordError).toBe('Invalid username or password');
  });

  it('performs a comparable verification attempt for unknown usernames', async () => {
    findUnique.mockResolvedValue(null);

    await expect(
      service.login({ username: 'ghost', password: 'whatever-password' }),
    ).rejects.toThrow(UnauthorizedException);
    expect(signAsync).not.toHaveBeenCalled();
  });

  it('returns the profile for an active user id', async () => {
    findUnique.mockResolvedValue(buildUser());

    const profile = await service.getProfile('11111111-1111-1111-1111-111111111111');

    expect(profile).toMatchObject({ username: 'admin', role: Role.ADMIN, active: true });
    expect(profile).not.toHaveProperty('passwordHash');
  });

  it('refuses to return a profile for a missing or inactive account', async () => {
    findUnique.mockResolvedValue(null);
    await expect(service.getProfile('missing')).rejects.toThrow(UnauthorizedException);

    findUnique.mockResolvedValue(buildUser({ active: false }));
    await expect(service.getProfile('inactive')).rejects.toThrow(UnauthorizedException);
  });
});
