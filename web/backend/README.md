# Hospital Management System — v2.0 Backend

REST API foundation for the v2.0 web application: **NestJS + TypeScript + Prisma + PostgreSQL**,
with JWT authentication and role-based authorization.

This is the first backend milestone. It provides the database foundation, authentication,
authorization, the module architecture and read APIs for the core hospital domains. It is built to
be consumed by the future React/Next.js frontend in `web/frontend/` (not implemented yet).

Everything in this project runs locally and for free. No cloud service, container runtime or paid
API is required — only Node.js and a local PostgreSQL server.

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Environment setup](#environment-setup)
4. [Database setup (Prisma)](#database-setup-prisma)
5. [Seeding development data](#seeding-development-data)
6. [Running the API](#running-the-api)
7. [Testing](#testing)
8. [API reference](#api-reference)
9. [Authentication & authorization](#authentication--authorization)
10. [Project structure](#project-structure)
11. [Scripts](#scripts)
12. [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement    | Version                    | Notes                                                        |
| -------------- | -------------------------- | ------------------------------------------------------------ |
| Node.js        | `^20.19` / `^22.12` / `>=24` | Required by Prisma 7 and NestJS 12. Node 22 LTS is recommended. |
| npm            | 10+                        | Ships with Node.                                              |
| PostgreSQL     | 14 or newer                | Must run **locally** on port `5432`.                          |

No Docker is required. Install PostgreSQL the normal way for your OS
(`brew install postgresql@16`, `winget install PostgreSQL.PostgreSQL`, `apt install postgresql`,
or the official installer), then make sure the server is running:

```bash
# PostgreSQL should be listening on 5432
psql -h localhost -p 5432 -U postgres -c "SELECT version();"
```

## Installation

```bash
cd web/backend
npm install
```

The Prisma client is **generated code** and is not committed to Git, so it must be generated once
after installing (and again whenever `prisma/schema.prisma` changes):

```bash
npm run prisma:generate
```

## Environment setup

Copy the example file and fill in local values:

```bash
cp .env.example .env
```

`.env` is git-ignored and must never be committed. `.env.example` contains placeholders only.

| Variable         | Purpose                                                            |
| ---------------- | ------------------------------------------------------------------ |
| `NODE_ENV`       | `development` / `test` / `production`.                             |
| `PORT`           | HTTP port for the API (default `4000`).                            |
| `API_PREFIX`     | Global route prefix (default `api/v1`).                            |
| `DATABASE_URL`   | PostgreSQL connection string.                                      |
| `JWT_SECRET`     | Access-token signing secret, **at least 32 characters**.           |
| `JWT_EXPIRES_IN` | Token lifetime, e.g. `15m`, `1h`, `7d`.                            |
| `BCRYPT_ROUNDS`  | Password hashing cost, `10`–`14` (default `12`).                   |
| `CORS_ORIGINS`   | Comma-separated allowed browser origins (default localhost:3000).   |
| `SEED_*`         | Development seed inputs — see [Seeding](#seeding-development-data). |

The application validates this configuration at boot and refuses to start if `DATABASE_URL` is
missing, if `JWT_SECRET` is shorter than 32 characters, or if `PORT`/`BCRYPT_ROUNDS` are out of
range. Generate a secret with:

```bash
openssl rand -base64 48
```

## Database setup (Prisma)

Create the database once (any client works):

```bash
createdb -h localhost -p 5432 -U postgres hms_dev
# or: psql -U postgres -c "CREATE DATABASE hms_dev;"
```

Then apply the schema. The initial migration is committed in this repository, so applying it is a
single command:

```bash
npm run prisma:deploy
```

That runs `prisma migrate deploy`, which applies every migration in `prisma/migrations/` that has
not been applied yet and records it in the `_prisma_migrations` table. It is idempotent: running it
again reports "No pending migrations to apply".

When you change `prisma/schema.prisma` during development, create a follow-up migration:

```bash
npm run prisma:migrate -- --name add_something
```

`prisma migrate dev` diffs your schema against the migration history, writes a new
`prisma/migrations/<timestamp>_add_something/migration.sql`, applies it, and regenerates the client.
Committing the generated SQL is what keeps every environment in sync.

Migration layout:

```
prisma/migrations/
├── 20260919091226_init/
│   └── migration.sql      # initial schema: 10 tables, 4 enums, 31 indexes, 14 foreign keys
└── migration_lock.toml    # pins the provider (postgresql) for the migration history
```

Useful companions:

```bash
npm run prisma:validate   # check schema.prisma for errors
npm run prisma:generate   # regenerate the typed client
npm run prisma:studio     # browse data in a local web UI
npm run prisma:deploy     # apply committed migrations without prompting (CI/production)
```

> **Schema location.** The datasource URL is supplied by `prisma.config.ts` from `DATABASE_URL`;
> the schema itself lives in `prisma/schema.prisma`. The generated client is written to
> `src/generated/prisma` and imported from there (this is the Prisma 7 `prisma-client` generator,
> which replaced `prisma-client-js`).

### Data model

Ten models model the hospital domain, using UUID primary keys, `timestamptz` audit columns and
foreign-key constraints:

| Model              | Purpose                                            | Key relationships                                                            |
| ------------------ | -------------------------------------------------- | ---------------------------------------------------------------------------- |
| `User`             | Login accounts and their role                       | 1–1 optional with `Doctor`                                                    |
| `Department`       | Clinical department                                 | 1–N with `Doctor`                                                             |
| `Doctor`           | Clinician, optionally linked to a login account     | N–1 `Department`; 1–N `Appointment`, `MedicalRecord`, `Prescription`           |
| `Patient`          | Patient record                                      | 1–N `Appointment`, `MedicalRecord`, `Prescription`, `Bill`                     |
| `Appointment`      | Scheduled visit between a patient and a doctor      | N–1 `Patient`, `Doctor`; 1–1 optional `MedicalRecord`                          |
| `MedicalRecord`    | Clinical findings for one appointment               | N–1 `Patient`, `Doctor`; 1–1 `Appointment`; 1–1 optional `Prescription`        |
| `Prescription`     | Medication order issued from a medical record       | N–1 `MedicalRecord`, `Patient`, `Doctor`; 1–N `PrescriptionItem`               |
| `PrescriptionItem` | One medication line on a prescription               | N–1 `Prescription` (cascade delete)                                            |
| `Bill`             | Invoice for a patient visit                         | N–1 `Patient`; N–1 optional `Appointment`; 1–N `BillItem`                      |
| `BillItem`         | One charge line on a bill                           | N–1 `Bill` (cascade delete)                                                    |

Enums: `Role` (`ADMIN`, `DOCTOR`, `RECEPTIONIST`), `Gender`, `AppointmentStatus`
(`SCHEDULED`, `COMPLETED`, `CANCELLED`) and `BillStatus` (`UNPAID`, `PARTIALLY_PAID`, `PAID`,
`CANCELLED`). Monetary values use `Decimal(12, 2)` and are returned by the API as strings so no
precision is lost in JSON.

## Seeding development data

The seed script is idempotent and stores **no credentials in the repository**. It reads passwords
from the environment; if a password variable is empty, it generates a random password and prints it
to the console once.

```bash
# Development admin login (password printed once)
npm run db:seed

# Also create doctor/receptionist logins and a small demo data set
SEED_DEMO_DATA=true npm run db:seed
```

| Variable                     | Default  | Behaviour                                             |
| ---------------------------- | -------- | ----------------------------------------------------- |
| `SEED_ADMIN_USERNAME`        | `admin`  | Username of the seeded administrator.                 |
| `SEED_ADMIN_PASSWORD`        | *(empty)*| Empty → random password generated and printed.        |
| `SEED_DOCTOR_PASSWORD`       | *(empty)*| Same, for the demo `doctor` account.                  |
| `SEED_RECEPTIONIST_PASSWORD` | *(empty)*| Same, for the demo `receptionist` account.            |
| `SEED_DEMO_DATA`             | `false`  | `true` also creates departments, a doctor and a patient. |

> Seeding is **explicit** (`npm run db:seed`) — Prisma 7 no longer auto-seeds during migrations.
> Never run this seed against a production database.

## Running the API

```bash
npm run start:dev     # watch mode, reloads on change
npm run build         # compile to dist/
npm run start:prod    # run the compiled build (node dist/main)
```

The API listens on `http://localhost:4000/api/v1` (port and prefix are configurable), bound to
`0.0.0.0` so it is also reachable from a proxy or another local process. Interactive API docs are
served at **`http://localhost:4000/api/docs`**.

CORS is enabled for the origins in `CORS_ORIGINS` (`http://localhost:3000` by default) so the
future frontend can call the API directly during development.

## Testing

```bash
npm test              # unit tests (no database required)
npm run test:watch    # watch mode
npm run test:cov      # coverage report
```

Unit tests mock `PrismaService`, so they run anywhere and cover password hashing and
authentication, JWT guard behaviour, role authorization, the exception filter, environment
validation, DTO validation, and the users/patients/doctors/billing/reports services.

End-to-end tests exercise the real HTTP stack and need a real PostgreSQL database:

```bash
# DATABASE_URL must point at a reachable database with the schema applied
RUN_DB_TESTS=true npm run test:e2e
```

Without `RUN_DB_TESTS=true` the e2e suite is skipped rather than reported as passing.

Lint and format checks:

```bash
npm run lint            # ESLint, zero warnings tolerated
npm run format:check    # Prettier
npm run typecheck       # tsc --noEmit
```

## API reference

Base URL: `http://localhost:4000/api/v1`

| Method | Endpoint                       | Auth              | Description                              |
| ------ | ------------------------------ | ----------------- | ---------------------------------------- |
| `GET`  | `/health`                      | public            | Liveness check.                          |
| `POST` | `/auth/login`                  | public            | Exchange credentials for a JWT.          |
| `GET`  | `/auth/me`                     | any role          | Current authenticated user.              |
| `GET`  | `/users`                       | `ADMIN`           | List user accounts (`?role=&active=&search=&page=&limit=`). |
| `GET`  | `/users/:id`                   | `ADMIN`           | Single user.                             |
| `GET`  | `/doctors`                     | all roles         | List doctors (`?departmentId=&search=&active=&page=&limit=`). |
| `GET`  | `/doctors/:id`                 | all roles         | Single doctor.                           |
| `GET`  | `/patients`                    | all roles         | List patients (`?search=&active=&page=&limit=`). |
| `GET`  | `/patients/:id`                | all roles         | Single patient.                          |
| `GET`  | `/appointments`                | all roles         | List appointments (`?patientId=&doctorId=&status=&from=&to=&page=&limit=`). |
| `GET`  | `/appointments/:id`            | all roles         | Single appointment.                      |
| `GET`  | `/medical-records`             | all roles         | List medical records (`?patientId=&doctorId=&search=&page=&limit=`). |
| `GET`  | `/medical-records/:id`         | all roles         | Single medical record.                   |
| `GET`  | `/prescriptions`               | all roles         | List prescriptions with their items.     |
| `GET`  | `/prescriptions/:id`           | all roles         | Single prescription.                     |
| `GET`  | `/bills`                       | `ADMIN`, `RECEPTIONIST` | List bills with their items.       |
| `GET`  | `/bills/:id`                   | `ADMIN`, `RECEPTIONIST` | Single bill.                       |
| `GET`  | `/reports/dashboard-summary`   | all roles         | Counters and today's revenue.            |

Full CRUD is intentionally out of scope for this milestone.

### Response conventions

List endpoints return a pagination envelope:

```json
{
  "data": [{ "id": "…" }],
  "meta": { "page": 1, "limit": 20, "total": 42, "totalPages": 3 }
}
```

Errors use a single envelope produced by a global exception filter; raw database errors are never
forwarded to clients:

```json
{
  "statusCode": 404,
  "error": "Not Found",
  "message": "Patient not found",
  "path": "/api/v1/patients/…",
  "timestamp": "2026-01-01T00:00:00.000Z"
}
```

Validation errors return `400` with an array of messages. Request bodies and query parameters are
validated globally: unknown properties are rejected, and query values are type-checked
(so `?limit=1000` or `?from=not-a-date` fail with `400` instead of reaching the database).

## Authentication & authorization

1. `POST /api/v1/auth/login` with `{ "username": "…", "password": "…" }`.
2. The API compares the password against the stored **bcrypt** hash and returns a signed JWT:

   ```json
   {
     "accessToken": "eyJ…",
     "tokenType": "Bearer",
     "expiresIn": "15m",
     "user": { "id": "…", "username": "admin", "role": "ADMIN", "active": true }
   }
   ```

3. Send the token on subsequent requests: `Authorization: Bearer <token>`.

Behavior worth noting:

- Passwords are stored only as bcrypt hashes; hashes are never returned by any endpoint.
- Unknown usernames, wrong passwords and deactivated accounts all return the same generic
  `401 Invalid username or password`, and a dummy hash comparison keeps the response time flat.
- The JWT payload carries `sub`, `username` and `role`, signed with `JWT_SECRET` from the
  environment. Roles are read **only** from the verified token — never from request bodies,
  query parameters or headers.
- `@Public()` routes (`/health`, `/auth/login`) skip the guard; every other route requires a valid,
  unexpired token.
- `@Roles(...)` enforces the authorization matrix above; a valid token with an insufficient role
  receives `403 Forbidden`.

## Project structure

```
web/backend/
├── prisma/
│   ├── schema.prisma          # datasource, generator, models, enums
│   └── seed.ts                # idempotent development seed
├── prisma.config.ts           # Prisma CLI configuration (loads .env, datasource URL)
├── src/
│   ├── auth/                  # login, JWT issuance, /auth/me
│   ├── users/                 # user administration (ADMIN only)
│   ├── doctors/               # doctor directory
│   ├── patients/              # patient directory
│   ├── appointments/          # appointment listing and filters
│   ├── medical-records/       # clinical records
│   ├── prescriptions/         # prescriptions incl. items
│   ├── billing/               # bills incl. items
│   ├── reports/               # dashboard summary counters
│   ├── common/                # guards, decorators, filters, DTOs, password helpers
│   ├── config/                # typed configuration + environment validation
│   ├── prisma/                # PrismaService (Prisma 7 driver adapter)
│   ├── generated/prisma/      # generated client (git-ignored — run prisma:generate)
│   ├── app.module.ts
│   └── main.ts                # bootstrap: prefix, CORS, validation, Swagger
└── test/                      # e2e smoke tests + test environment setup
```

Dependencies are deliberately minimal: NestJS, Prisma (`@prisma/client`, `@prisma/adapter-pg`,
`pg`), `@nestjs/jwt`, `@nestjs/config`, `@nestjs/swagger`, `class-validator`/`class-transformer`,
`bcrypt` and `rxjs`.

## Scripts

| Script                   | What it does                                                     |
| ------------------------ | ---------------------------------------------------------------- |
| `npm run start:dev`      | Start the API in watch mode.                                      |
| `npm run build`          | Compile TypeScript to `dist/`.                                    |
| `npm run start:prod`     | Run the compiled build.                                           |
| `npm run typecheck`      | Type-check without emitting.                                      |
| `npm run lint` / `lint:fix` | ESLint (the generated Prisma client is ignored).               |
| `npm run format` / `format:check` | Prettier.                                                |
| `npm run prisma:generate` / `prisma:validate` / `prisma:migrate` / `prisma:deploy` / `prisma:studio` | Prisma workflows. |
| `npm run db:seed`        | Run the development seed.                                         |
| `npm test` / `test:watch` / `test:cov` / `test:e2e` | Jest suites.                                |

## Troubleshooting

- **`Cannot find module '…/src/generated/prisma/…'`** — the Prisma client has not been generated
  yet: run `npm run prisma:generate`.
- **`JWT_SECRET must be at least 32 characters`** — the API validates its environment at boot;
  set a longer secret in `.env`.
- **`Can't reach database server` / migration errors** — confirm PostgreSQL is running on the host
  and port in `DATABASE_URL`, and that the database exists.
- **`P3005: The database schema is not empty`** — the target database already contains tables that
  were not created by these migrations (for example an older hand-made schema). Either point
  `DATABASE_URL` at a fresh database or baseline the existing one:
  `npx prisma migrate resolve --applied 20260919091226_init`.
- **`prisma migrate` cannot download its engine / offline machine** — the CLI needs its engines the
  first time; run `npm run prisma:generate` once with network access, or point
  `PRISMA_SCHEMA_ENGINE_BINARY` at a local engine binary in air-gapped setups.
- **Port 4000 already in use** — change `PORT` in `.env`.
- **CORS errors from the frontend** — add the frontend origin to `CORS_ORIGINS`.

## Security notes

- Passwords are hashed with bcrypt (cost from `BCRYPT_ROUNDS`); only hashes are stored.
- The JWT secret comes from the environment and is never committed; `.env` is git-ignored.
- Roles are always taken from the verified token, never from client-supplied data.
- All queries run through Prisma's parameterized query builder — no raw SQL string building.
- Database errors are translated into generic HTTP responses; internal messages are logged
  server-side only.
