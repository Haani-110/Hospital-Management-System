import { describe, it, expect } from '@jest/globals';
import {
  DEFAULT_BCRYPT_ROUNDS,
  generateRandomPassword,
  getDummyPasswordHash,
  hashPassword,
  verifyPassword,
} from './password.util.js';

const ROUNDS = 4; // low cost keeps the unit suite fast

describe('password utilities', () => {
  it('hashes a password with a bcrypt prefix and never returns the plain text', async () => {
    const hash = await hashPassword('correct horse battery staple', ROUNDS);

    expect(hash).not.toContain('correct horse battery staple');
    expect(hash).toMatch(/^\$2[aby]\$/);
    expect(hash).toHaveLength(60);
  });

  it('produces a different hash for the same password (random per-password salt)', async () => {
    const [first, second] = await Promise.all([
      hashPassword('repeatable-password', ROUNDS),
      hashPassword('repeatable-password', ROUNDS),
    ]);

    expect(first).not.toEqual(second);
    await expect(verifyPassword('repeatable-password', first)).resolves.toBe(true);
    await expect(verifyPassword('repeatable-password', second)).resolves.toBe(true);
  });

  it('verifies the correct password and rejects an incorrect one', async () => {
    const hash = await hashPassword('s3cret-password', ROUNDS);

    await expect(verifyPassword('s3cret-password', hash)).resolves.toBe(true);
    await expect(verifyPassword('S3cret-password', hash)).resolves.toBe(false);
    await expect(verifyPassword('', hash)).resolves.toBe(false);
  });

  it('treats a malformed stored hash as a failed login instead of throwing', async () => {
    await expect(verifyPassword('anything', 'not-a-bcrypt-hash')).resolves.toBe(false);
    await expect(verifyPassword('anything', '')).resolves.toBe(false);
  });

  it('exposes a stable dummy hash for timing equalisation', async () => {
    const first = await getDummyPasswordHash(ROUNDS);
    const second = await getDummyPasswordHash(ROUNDS);

    expect(first).toEqual(second);
    await expect(verifyPassword('some-guess', first)).resolves.toBe(false);
  });

  it('generates random passwords that do not repeat', () => {
    const first = generateRandomPassword();
    const second = generateRandomPassword();

    expect(first).not.toEqual(second);
    expect(first.length).toBeGreaterThanOrEqual(20);
  });

  it('defaults to a sane bcrypt cost', () => {
    expect(DEFAULT_BCRYPT_ROUNDS).toBeGreaterThanOrEqual(10);
  });
});
