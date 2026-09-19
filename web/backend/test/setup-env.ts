import 'reflect-metadata';

/**
 * Test-only environment.
 *
 * These are obvious placeholders (not credentials) so unit tests can build
 * configuration objects without a real .env file. Nothing here is used by the
 * application at runtime.
 */
process.env.NODE_ENV = 'test';
process.env.DATABASE_URL ??= 'postgresql://test:test@localhost:5432/hms_test?schema=public';
process.env.JWT_SECRET ??= 'test-only-jwt-secret-value-not-used-in-production-000';
process.env.JWT_EXPIRES_IN ??= '15m';
// Valid, low-cost value: the app validates this range at boot.
process.env.BCRYPT_ROUNDS ??= '10';
