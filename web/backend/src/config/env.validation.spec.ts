import { describe, expect, it } from '@jest/globals';
import { NodeEnvironment, validateEnvironment } from './env.validation.js';

const validConfig = {
  DATABASE_URL: 'postgresql://user:pass@localhost:5432/hms_dev?schema=public',
  JWT_SECRET: 'a-sufficiently-long-development-secret-value',
};

describe('validateEnvironment', () => {
  it('accepts a valid configuration and applies defaults', () => {
    const env = validateEnvironment(validConfig);

    expect(env.PORT).toBe(4000);
    expect(env.API_PREFIX).toBe('api/v1');
    expect(env.JWT_EXPIRES_IN).toBe('15m');
    expect(env.BCRYPT_ROUNDS).toBe(12);
    expect(env.NODE_ENV).toBe(NodeEnvironment.Development);
  });

  it('coerces numeric values supplied as strings', () => {
    const env = validateEnvironment({ ...validConfig, PORT: '4100', BCRYPT_ROUNDS: '10' });

    expect(env.PORT).toBe(4100);
    expect(env.BCRYPT_ROUNDS).toBe(10);
  });

  it('refuses to start without a DATABASE_URL', () => {
    expect(() => validateEnvironment({ JWT_SECRET: validConfig.JWT_SECRET })).toThrow(
      /DATABASE_URL/,
    );
  });

  it('refuses to start without a JWT_SECRET', () => {
    expect(() => validateEnvironment({ DATABASE_URL: validConfig.DATABASE_URL })).toThrow(
      /JWT_SECRET/,
    );
  });

  it('rejects a JWT secret that is too short to be safe', () => {
    expect(() => validateEnvironment({ ...validConfig, JWT_SECRET: 'short-secret' })).toThrow(
      /at least 32 characters/,
    );
  });

  it('rejects an out-of-range port', () => {
    expect(() => validateEnvironment({ ...validConfig, PORT: '70000' })).toThrow(/PORT/);
  });

  it('rejects an unsafe bcrypt cost', () => {
    expect(() => validateEnvironment({ ...validConfig, BCRYPT_ROUNDS: '2' })).toThrow(
      /BCRYPT_ROUNDS/,
    );
  });

  it('splits CORS origins from the environment default', () => {
    const env = validateEnvironment(validConfig);

    expect(env.CORS_ORIGINS).toContain('http://localhost:3000');
  });
});
