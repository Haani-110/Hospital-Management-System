import { describe, expect, it } from '@jest/globals';
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { LoginDto } from './login.dto.js';

async function validatePayload(payload: Record<string, unknown>) {
  return validate(plainToInstance(LoginDto, payload));
}

describe('LoginDto validation', () => {
  it('accepts a well-formed credential payload', async () => {
    expect(await validatePayload({ username: 'admin', password: 'admin-password' })).toHaveLength(
      0,
    );
  });

  it('rejects a missing username', async () => {
    const errors = await validatePayload({ password: 'admin-password' });
    expect(errors.some((error) => error.property === 'username')).toBe(true);
  });

  it('rejects a too-short username', async () => {
    const errors = await validatePayload({ username: 'ab', password: 'admin-password' });
    expect(errors.some((error) => error.property === 'username')).toBe(true);
  });

  it('rejects a missing or too-short password', async () => {
    expect(
      (await validatePayload({ username: 'admin' })).some((error) => error.property === 'password'),
    ).toBe(true);
    expect(
      (await validatePayload({ username: 'admin', password: 'short' })).some(
        (error) => error.property === 'password',
      ),
    ).toBe(true);
  });

  it('rejects non-string credentials (object/array payloads)', async () => {
    expect(
      (await validatePayload({ username: { $ne: null }, password: ['a', 'b'] })).length,
    ).toBeGreaterThan(0);
  });
});
