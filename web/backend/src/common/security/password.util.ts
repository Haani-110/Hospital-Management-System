import { randomBytes } from 'node:crypto';
import * as bcrypt from 'bcrypt';

/**
 * Password hashing helpers.
 *
 * bcrypt is used with a per-password random salt (bcrypt generates and stores
 * the salt inside the resulting hash). Plain-text passwords are never logged,
 * stored, or returned by the API.
 */

export const DEFAULT_BCRYPT_ROUNDS = 12;

export async function hashPassword(
  plainText: string,
  rounds = DEFAULT_BCRYPT_ROUNDS,
): Promise<string> {
  return bcrypt.hash(plainText, rounds);
}

export async function verifyPassword(plainText: string, passwordHash: string): Promise<boolean> {
  try {
    return await bcrypt.compare(plainText, passwordHash);
  } catch {
    // A malformed stored hash must be treated as a failed login, not a crash.
    return false;
  }
}

let dummyHashPromise: Promise<string> | null = null;

/**
 * A throwaway hash used to equalise the work done for unknown usernames.
 * Without it, a missing user returns noticeably faster than a wrong password,
 * which leaks whether a username exists.
 */
export function getDummyPasswordHash(rounds = DEFAULT_BCRYPT_ROUNDS): Promise<string> {
  dummyHashPromise ??= hashPassword(randomBytes(32).toString('hex'), rounds);
  return dummyHashPromise;
}

export function generateRandomPassword(bytes = 18): string {
  return randomBytes(bytes).toString('base64url');
}
