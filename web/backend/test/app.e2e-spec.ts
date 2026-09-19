import { afterAll, beforeAll, describe, expect, it } from '@jest/globals';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { AppModule } from '../src/app.module.js';
import { AllExceptionsFilter } from '../src/common/filters/all-exceptions.filter.js';

/**
 * End-to-end smoke tests.
 *
 * These exercise the real HTTP stack against a real PostgreSQL database, so
 * they only run when explicitly enabled:
 *
 *   RUN_DB_TESTS=true npm run test:e2e
 *
 * Without a database they are skipped rather than reported as passing.
 */
const describeWithDatabase = process.env.RUN_DB_TESTS === 'true' ? describe : describe.skip;

describeWithDatabase('API (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();

    app = moduleRef.createNestApplication();
    app.setGlobalPrefix(process.env.API_PREFIX ?? 'api/v1');
    app.useGlobalPipes(
      new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true }),
    );
    app.useGlobalFilters(new AllExceptionsFilter());
    await app.init();
  });

  afterAll(async () => {
    await app?.close();
  });

  it('exposes an unauthenticated health endpoint', async () => {
    await request(app.getHttpServer()).get('/api/v1/health').expect(200);
  });

  it('rejects a protected route without a token', async () => {
    await request(app.getHttpServer()).get('/api/v1/patients').expect(401);
  });

  it('rejects invalid login payloads with a 400 validation envelope', async () => {
    const response = await request(app.getHttpServer())
      .post('/api/v1/auth/login')
      .send({ username: 'a', password: 'short' })
      .expect(400);

    expect(response.body).toMatchObject({ statusCode: 400 });
    expect(Array.isArray(response.body.message)).toBe(true);
  });

  it('rejects unknown credentials with a generic 401 message', async () => {
    const response = await request(app.getHttpServer())
      .post('/api/v1/auth/login')
      .send({ username: 'no-such-user', password: 'irrelevant-password' })
      .expect(401);

    expect(response.body.message).toBe('Invalid username or password');
  });
});
