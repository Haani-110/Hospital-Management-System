# Hospital Management System

Monorepo for the Hospital Management System project. It contains two clearly separated
applications:

| Area | Version | Location | Status |
|---|---|---|---|
| Desktop application | **v1.0** | [`desktop/`](desktop/) | Complete and stable — JavaFX + SQLite |
| Web application | **v2.0** | [`web/`](web/) | Structure reserved only — no implementation yet |

The v1.0 desktop application was developed as a standalone Maven project at the
repository root. It has been moved into [`desktop/`](desktop/) so the future v2.0 web
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
├── web/                        # v2.0 — web application (placeholder structure only)
│   ├── frontend/               # Reserved for the future web frontend
│   └── backend/                # Reserved for the future web backend
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

## Web application — v2.0 (not implemented yet)

`web/frontend/` and `web/backend/` are currently **empty placeholder directories**
(kept in Git with `.gitkeep` files). They exist so the v2.0 web application has a defined
home in the monorepo.

Nothing has been scaffolded or decided yet:

- No frontend framework or application code (no React/Next.js, no pages, no assets).
- No backend framework or application code (no NestJS, no API, no modules).
- No PostgreSQL, Prisma, Docker, Supabase, Firebase, or any other cloud service.
- No shared packages, build tooling, or CI for the web application.

Web source code will be added in a later phase of the v2.0 work, not in this
reorganization.

## Documentation

- [`desktop/README.md`](desktop/README.md) — v1.0 desktop application: features, roles and
  permissions, architecture, project structure, installation, testing, and local database.
- [`docs/phase-9-ui-verification.md`](docs/phase-9-ui-verification.md) — Phase 9 UI polish
  verification notes and manual acceptance checklists (historical QA context for the
  desktop app).

## Repository conventions

- `desktop/` owns the v1.0 JavaFX application and its Maven build; `web/frontend/` and
  `web/backend/` are reserved for the v2.0 web application.
- Each application owns its own build files and tooling; the repository root stays
  technology-neutral (no root-level Maven project, no root-level application code).
- Generated build output, compiled classes, IDE settings, logs, local SQLite databases and
  their sidecar files, and environment files are excluded by [`.gitignore`](.gitignore).
  Databases must only ever contain synthetic demonstration data.
