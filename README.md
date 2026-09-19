# Hospital Management System

Monorepo for the Hospital Management System project. It contains two clearly separated
applications:

| Area | Version | Location | Status |
|---|---|---|---|
| Desktop application | **v1.0** | [`desktop/`](desktop/) | Complete and stable — JavaFX + SQLite |
| Web API | **v2.0** | [`web/backend/`](web/backend/) | Implemented — NestJS + Prisma + PostgreSQL (read APIs, JWT auth, roles) |
| Web interface | **v2.0** | [`web/frontend/`](web/frontend/) | Implemented — React 19 + TypeScript + Vite, consuming the live API |

The v1.0 desktop application was developed as a standalone Maven project at the
repository root. It has been moved into [`desktop/`](desktop/) so the v2.0 web
application can be developed alongside it without mixing toolchains or build files.

> **Educational / portfolio project:** this is not production hospital or clinical
> software. It makes no real-world regulatory or clinical-compliance claim. Use synthetic
> demonstration data, not real patient information.

## Repository layout

```text
Hospital-Management-System/
├── desktop/                    # v1.0 — JavaFX + SQLite desktop application (stable)
│   ├── pom.xml                 # Maven build for the desktop app (Java 21, JavaFX 21)
│   ├── src/
│   │   ├── main/               # Java sources + JavaFX/CSS resources
│   │   └── test/               # JUnit 5 test suite
│   ├── data/
│   │   ├── .gitkeep            # Preserves the directory; runtime DB is ignored
│   │   └── hospital.db         # Local SQLite database (created at runtime, not committed)
│   └── README.md               # Full desktop documentation
├── web/                        # v2.0 — web application
│   ├── backend/                # NestJS + Prisma REST API (auth, read endpoints, Swagger)
│   └── frontend/               # React + TypeScript + Vite single-page interface
├── docs/
│   └── phase-9-ui-verification.md
├── .gitignore
└── README.md                   # This file
```

## Desktop application — v1.0 (stable)

A local, offline JavaFX desktop application for hospital administration: staff accounts,
departments, doctors, patients, appointments, medical records, prescriptions, billing,
and reports, backed by SQLite.

- **Technology:** Java 21, JavaFX 21, SQLite (JDBC), Maven, JUnit 5.
- **Location:** everything for this application lives under `desktop/` — its own
  `pom.xml`, sources, tests, resources, and local `data/` directory.
- **Documentation:** see [`desktop/README.md`](desktop/README.md) for full feature,
  architecture, role/permission, testing, and database documentation.

### Quick start

```bash
git clone https://github.com/Haani-110/Hospital-Management-System.git
cd Hospital-Management-System/desktop
```

Then build and launch from inside `desktop/`:

```bash
mvn clean test
mvn javafx:run
```

Requirements are **JDK 21** (not just a JRE), **Maven**, and a graphical desktop session.
Run Maven from `desktop/` so the relative database location stays consistent — the app
resolves SQLite at `data/hospital.db` relative to the working directory, which means
`desktop/data/hospital.db`.

### Development/demo accounts

Public development credentials, not secure production credentials:

| Role | Username | Password |
|---|---|---|
| `ADMIN` | `admin` | `admin123` |
| `DOCTOR` | `doctor` | `doctor123` |
| `RECEPTIONIST` | `receptionist` | `receptionist123` |

## Web application — v2.0

Two applications, both under [`web/`](web/):

| Part | Technology | Location | Documentation |
|---|---|---|---|
| API | NestJS 12, Prisma 7, PostgreSQL, JWT | [`web/backend/`](web/backend/) | [`web/backend/README.md`](web/backend/README.md) |
| Interface | React 19, TypeScript, Vite 8, React Router 7, Tailwind CSS 4 | [`web/frontend/`](web/frontend/) | [`web/frontend/README.md`](web/frontend/README.md) |

The interface is a real client: every screen reads live records from the API and database.
There is no mock data, no fake CRUD and no invented statistics. Modules whose endpoints do not
exist yet (Laboratory, Pharmacy, Admissions) are deliberate, clearly-labelled foundations that
show an honest empty state instead of placeholder rows.

### Quick start

```bash
# 1. API — needs a local PostgreSQL 14+, Node.js 20.19+
cd web/backend
npm install
cp .env.example .env          # set DATABASE_URL and JWT_SECRET
npm run prisma:generate
npm run prisma:deploy         # apply the committed migration
npm run db:seed               # creates the admin account, prints credentials
npm run start:dev             # http://localhost:4000  (Swagger at /api/docs)

# 2. Interface — in a second terminal
cd web/frontend
npm install
cp .env.example .env          # VITE_API_BASE_URL=http://localhost:4000/api/v1
npm run dev                   # http://localhost:3000
```

Sign in with the credentials printed by the seed script. Roles: `ADMIN`, `DOCTOR`,
`RECEPTIONIST` — the API enforces what each one may read, and the interface mirrors it
(billing requires ADMIN/RECEPTIONIST, user accounts require ADMIN).

Everything runs locally and for free: no cloud service, no container runtime, no paid API.

## Documentation

- [`desktop/README.md`](desktop/README.md) — v1.0 desktop application: features, roles and
  permissions, architecture, project structure, installation, testing, and local database.
- [`docs/phase-9-ui-verification.md`](docs/phase-9-ui-verification.md) — Phase 9 UI polish
  verification notes and manual acceptance checklists (historical QA context for the
  desktop app).
- [`web/backend/README.md`](web/backend/README.md) — API: endpoints, authentication,
  database setup, migrations, seeding, testing.
- [`web/frontend/README.md`](web/frontend/README.md) — interface: screens and the endpoints
  they call, environment variables, architecture, testing, troubleshooting.

## Repository conventions

- `desktop/` owns the v1.0 JavaFX application and its Maven build; `web/backend/` owns the
  v2.0 REST API and `web/frontend/` owns the v2.0 web interface, each with its own toolchain.
- Each application owns its own build files and tooling; the repository root stays
  technology-neutral (no root-level Maven project, no root-level application code).
- Generated build output, compiled classes, IDE settings, logs, local SQLite databases and
  their sidecar files, and environment files are excluded by [`.gitignore`](.gitignore).
  Databases must only ever contain synthetic demonstration data.
