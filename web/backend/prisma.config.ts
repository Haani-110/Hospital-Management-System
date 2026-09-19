import 'dotenv/config';
import { defineConfig, env } from 'prisma/config';

/**
 * Prisma CLI configuration (Prisma 7+).
 *
 * The datasource URL is intentionally NOT in schema.prisma any more — Prisma 7
 * reads it from here, and the CLI no longer loads .env automatically. The
 * `dotenv/config` import above restores that behaviour for local development;
 * in CI/production DATABASE_URL is provided by the environment.
 */
export default defineConfig({
  schema: 'prisma/schema.prisma',
  migrations: {
    path: 'prisma/migrations',
    seed: 'node --loader ts-node/esm prisma/seed.ts',
  },
  datasource: {
    url: env('DATABASE_URL'),
  },
});
