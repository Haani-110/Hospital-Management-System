# Hospital Management System

A local JavaFX desktop application for managing hospital administration and related care records, backed by SQLite.

## Overview

Hospital Management System brings staff accounts, departments, doctors, patients, appointments, medical records, prescriptions, billing, and reports into one role-aware desktop application. It is built with **Java 21, JavaFX 21, and SQLite**, with a layered Java codebase and automated tests.

The application runs locally/offline: no paid services, cloud infrastructure, external APIs, or database server are required. Initial setup and Maven dependency downloads require internet access; normal application use does not.

> **Educational / portfolio project:** this is not production hospital or clinical software. It makes no real-world regulatory or clinical-compliance claim. Use synthetic demonstration data, not real patient information.

## Contents

- [Features](#features)
- [Roles and permissions](#roles-and-permissions)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Installation and running](#installation-and-running)
- [Development/demo accounts](#developmentdemo-accounts)
- [Testing](#testing)
- [Local database](#local-database)
- [UI and accessibility](#ui-and-accessibility)
- [Screenshots](#screenshots)
- [Developer notes](#developer-notes)
- [Known limitations](#known-limitations)
- [Future development ideas](#future-development-ideas)

## Features

### Authentication

- Login and logout with an in-memory session.
- Password hashing using PBKDF2-HMAC-SHA256 with a random salt per password.
- Active/inactive user accounts; inactive accounts cannot log in.
- Role-based navigation and service-layer authorization.

### Departments

- Create, view, update, and delete departments.
- Required-field, length, and duplicate-name validation.
- Admin-managed department operations, subject to database relationships.

### Users

- Admin user management, role assignment, and activation/deactivation.
- Password changes with validation and confirmation; passwords are stored as hashes.
- Duplicate-username validation.
- Admin self-protection: the current administrator cannot deactivate their own account or change their own role away from `ADMIN`.

### Doctors

- Create and update doctor profiles, including department, specialization, contact details, and consultation fee.
- Admin-controlled activation/deactivation.
- Search, filter, and view doctor details; receptionist access is read-only.

### Patients

- Patient record management: create, read, and update records, with activation/deactivation instead of physical deletion.
- Automatically generated patient codes.
- Search and active/inactive filtering.
- Validation of identity, date of birth, contact, and other patient fields.

### Appointments

- Schedule patients with doctors and validate dates/times and related records.
- Prevent doctor conflicts at the same appointment date/time.
- Search and filter appointments.
- Status lifecycle: `SCHEDULED` → `COMPLETED` or `CANCELLED`.
- Edit or cancel eligible scheduled appointments; completed/cancelled appointments cannot be edited or re-transitioned.
- Completion is currently an Admin operation; see [operation restrictions](#operation-restrictions).

### Medical Records

- At most one medical record per **completed** appointment.
- Diagnosis, symptoms, examination, treatment notes, and record date.
- Patient and doctor association inherited from the appointment.
- Search/view records and enforce doctor ownership for doctor-created/edited records.

### Prescriptions

- At most one prescription per medical record, containing multiple medicine items.
- Add, edit, and remove medicine items in the prescription form.
- Validate medicine details and require at least one item when saving.
- Doctor ownership checks for prescription creation/editing.

### Billing

- Create bills with line items, quantities, unit prices, and discounts.
- Automatically calculate item amounts, subtotal, and total using `BigDecimal`.
- Associate each bill with a patient and optionally a matching appointment.
- Search/view bills and manage the `UNPAID`, `PARTIALLY_PAID`, `PAID`, and `CANCELLED` lifecycle under role restrictions.
- Validate discounts and status transitions; cancelled bills are immutable.

### Dashboard

All eight statistics are calculated from the local database:

| Statistic | Meaning |
|---|---|
| Total Patients | Count of patient records, including inactive patients |
| Active Doctors | Count of active doctor profiles |
| Today's Appointments | Appointments dated today, regardless of status |
| Pending Appointments | Appointments with `SCHEDULED` status |
| Completed Appointments | Appointments with `COMPLETED` status |
| Unpaid Bills | Bills with `UNPAID` status |
| Partially Paid Bills | Bills with `PARTIALLY_PAID` status |
| Today's Revenue | Sum of totals for `PAID` bills whose **bill date** is today |

Today's Revenue is a bill-date-based summary, not a payment-transaction ledger. The dashboard also provides role-aware shortcuts to modules.

### Reports

Read-only reports present local data in tables with search and relevant filters:

| Report | Available filters |
|---|---|
| Appointments | Search, appointment date range, status |
| Patients | Search, active/inactive status |
| Doctors | Search, department, active/inactive status |
| Medical records | Search, record date range |
| Prescriptions | Search, prescription date range |
| Billing | Search, bill date range, payment status |

Reports include result counts; billing reports also summarize subtotal, discount, and total. Date-range inputs are validated where applicable. All authenticated roles can run the existing read-only reports, including billing reports.

### UI/UX

A consistent JavaFX/CSS visual system provides a responsive desktop shell, role-specific navigation, module transitions, dashboard card entrances, hover/focus interactions, status badges, styled dialogs, and responsive tables/forms. See [UI and accessibility](#ui-and-accessibility) for reduced-motion support.

## Roles and permissions

### Navigation access

This table describes the modules visible in each role's application navigation—not unrestricted permission to perform every operation in those modules.

| Role | Visible modules | Scope |
|---|---|---|
| `ADMIN` | Dashboard, Patients, Doctors, Appointments, Medical Records, Prescriptions, Billing, Reports, Departments, Users | Full system management, subject to validation and lifecycle rules |
| `DOCTOR` | Dashboard, Patients, Appointments, Medical Records, Prescriptions, Reports | Clinical viewing and ownership-restricted record/prescription editing; **no billing management** |
| `RECEPTIONIST` | Dashboard, Patients, Doctors, Appointments, Medical Records, Prescriptions, Billing, Reports | Registration, scheduling, and permitted billing operations; no Departments or Users navigation |

### Operation restrictions

- **Departments, users, and doctor profiles:** only Admin can create/update/manage these records. Receptionists can view/search doctors but cannot change their profiles.
- **Patients:** Admin and Receptionist can create/update and activate/deactivate patients; Doctor access is read-only.
- **Appointments:** Admin and Receptionist can schedule, edit, and cancel eligible appointments. Only Admin can mark appointments completed. Doctor access is read-only.
- **Medical records and prescriptions:** Admin can create/edit. Doctors may create/edit only when their account is linked to the doctor profile associated with the appointment/record. Receptionists can view/search but cannot create/edit.
- **Billing:** Admin and Receptionist can create/edit eligible bills and mark partial payment. Only Admin can mark bills paid or cancel them. Doctors have no Billing navigation and cannot mutate bills.
- **Reports and dashboard:** available to all authenticated roles; reports are read-only, including billing reports.

**Navigation visibility and backend authorization are separate protections.** Controllers hide inappropriate routes and disable unavailable actions; services independently check sessions, roles, ownership, and lifecycle rules. A hidden route is not a blanket service-level read prohibition: for example, existing billing view methods require a logged-in user, while billing mutations require the appropriate role. The navigation table should not be interpreted as a complete data-isolation policy.

The demo Doctor account is not linked to a doctor profile, and the current UI has no profile-linking workflow. Its ownership-restricted medical-record/prescription writes are therefore rejected rather than guessing an identity.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 21 | Application language |
| JavaFX 21 | Desktop UI |
| SQLite | Local database |
| JDBC | Database access |
| Maven | Build/dependency management |
| JUnit 5 | Automated testing |
| CSS | UI styling |

The versions pinned in [`pom.xml`](pom.xml) include JavaFX **21.0.2**, SQLite JDBC **3.45.1.0**, and JUnit **5.10.2**. The application uses a local database, not a cloud service.

## Architecture

```text
JavaFX UI / Controllers
          ↓
       Services
          ↓
         DAOs
          ↓
   SQLite (via JDBC)
```

- **Controllers** build the JavaFX views and handle user interaction, presentation state, and calls to services.
- **Services** contain business rules, validation, authorization, and workflow coordination. The report service also builds report queries for the report DAO to execute.
- **DAOs** handle persistence, SQL execution, and mapping stored data to Java objects or report rows.
- **SQLite** stores application data on the local filesystem.
- **Models** represent domain data; configuration classes initialize and connect to the database; utilities provide password hashing and shared presentation helpers.

`Main.java` initializes the database, constructs DAOs/services/controllers, and wires navigation callbacks explicitly. Views are built in Java, not FXML. There is no web server, ORM, or dependency-injection framework.

## Project structure

```text
Hospital-Management-System/
├── README.md
├── pom.xml
├── .gitignore
├── data/
│   └── .gitkeep                 # Preserves the directory; runtime DB is ignored
├── docs/
│   └── phase-9-ui-verification.md
└── src/
    ├── main/
    │   ├── java/com/hospital/
    │   │   ├── Main.java        # Entry point and application wiring
    │   │   ├── config/          # DatabaseConfig, DatabaseConnection, DatabaseInitializer
    │   │   ├── controller/      # Login and the ten module controllers
    │   │   ├── service/         # Business services and Session
    │   │   ├── dao/             # DAO interfaces/implementations and ReportDao
    │   │   ├── model/           # Domain objects, Role, DashboardStats
    │   │   ├── exception/       # Authentication, authorization, validation, DB errors
    │   │   └── util/            # PasswordUtil, SceneManager, UiStyles, UiMotion
    │   └── resources/com/hospital/css/
    │       └── styles.css       # Shared JavaFX stylesheet
    └── test/java/com/hospital/
        ├── config/             # Database initialization tests and TestDatabaseFactory
        ├── dao/                # Persistence tests
        ├── service/            # Business-rule, authorization, and report tests
        └── util/               # Password hashing tests
```

## Installation and running

### Requirements

- **JDK 21** (not just a JRE).
- **Maven**.
- Git to clone the repository.
- A graphical Linux desktop session to display JavaFX windows.

On Ubuntu 24.04, install the development tools:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git
```

On other Ubuntu/Linux releases, use the distribution's equivalent JDK 21 and Maven packages. A separate JavaFX SDK or SQLite server is not needed; Maven resolves the JavaFX libraries and SQLite JDBC driver.

Verify the selected tools:

```bash
java -version
javac -version
mvn -version
```

Both Java commands should report version **21**, and Maven should also report that it is using Java 21. If multiple JDKs are installed, select JDK 21 and correct `JAVA_HOME` before continuing.

### Clone, test, and launch

```bash
git clone https://github.com/Haani-110/Hospital-Management-System.git
cd Hospital-Management-System
```

The clone initially uses the repository's default branch. The Phase 9.2 work documented here is on its review branch; in a fresh clone, select it before building:

```bash
git switch --track origin/arena/01a0b616-hospital-management-system
```

Then test and launch:

```bash
mvn clean test
mvn javafx:run
```

Run Maven from the repository root so the relative database location is consistent. The first launch initializes the SQLite schema and seeds demo accounts if the users table is empty. No manual schema import is required.

An initial build may download dependencies/plugins. The application itself needs no network connection or remote service. Launch it from a desktop session, not a headless terminal without a graphical display.

## Development/demo accounts

These are **public development/demo credentials**, not secure production credentials:

| Role | Username | Password |
|---|---|---|
| `ADMIN` | `admin` | `admin123` |
| `DOCTOR` | `doctor` | `doctor123` |
| `RECEPTIONIST` | `receptionist` | `receptionist123` |

`AuthService` seeds these accounts only when the users table is empty. Relaunching does not reset passwords or recreate missing demo accounts in a non-empty users table. The stored passwords are salted hashes, but the published defaults are intentionally known.

For a demonstration, use synthetic data. Admin can create departments and doctor profiles, register patients, schedule and complete an appointment, then create its medical record and prescription. Changing the demo passwords does not make this project suitable for clinical deployment. See the Doctor profile-linking limitation under [roles and permissions](#roles-and-permissions).

## Testing

Run the automated suite from the project root:

```bash
mvn clean test
```

The current expected suite is **339 tests, 0 failures, 0 errors**. Source inspection finds **339 `@Test` methods across 22 test classes**; this count is not proof of a successful test run.

Tests cover areas including:

- Password hashing, authentication, logout, inactive accounts, and demo seeding.
- Authorization, role restrictions, ownership checks, and Admin self-protection.
- Database initialization, DAO CRUD/persistence, and validation.
- Patient and doctor management.
- Appointment scheduling, conflicts, and status transitions.
- Medical-record eligibility and uniqueness.
- Prescription items and validation.
- Billing calculations, discounts, and status permissions.
- Dashboard aggregates, reports, filters, and date-range validation.

Database tests use isolated temporary SQLite files through `TestDatabaseFactory`, rather than the application's `data/hospital.db`. Maven writes test reports to `target/surefire-reports/` after execution.

**Phase 9.2 verification:** `mvn clean test` was attempted in the editing environment, but Maven was unavailable (`mvn: command not found`). Compilation and test results could not be verified there; no passing result or coverage percentage is claimed.

Automated tests do not replace desktop interaction checks. The existing [Phase 9 UI verification notes](docs/phase-9-ui-verification.md) contain implementation history and pending manual acceptance checklists; the third-pass section describes the current shell and motion behavior.

## Local database

- **Storage:** SQLite file at `data/hospital.db`, relative to the working directory where the application is launched.
- **Configuration:** [`DatabaseConfig.java`](src/main/java/com/hospital/config/DatabaseConfig.java) defines the default path and JDBC URL (`jdbc:sqlite:data/hospital.db` on Linux).
- **Connections:** `DatabaseConnection` opens JDBC connections and enables foreign-key enforcement.
- **Initialization:** `DatabaseInitializer` creates the schema and applies its existing startup compatibility updates. Demo users are seeded separately by `AuthService`.
- **Persistence:** records remain on the local filesystem between application runs. No cloud database, database-service account, or external API is required.

The database and its SQLite journal/WAL sidecar files are excluded from Git; only `data/.gitkeep` is tracked. Do not commit databases containing personal data. Close the application before making a filesystem backup of the database, and keep any backups outside the repository. Starting from another working directory can create a different `data/hospital.db` there.

## UI and accessibility

Phase 9 introduced shared presentation helpers and JavaFX/CSS styling:

- **Stable application shell:** one Stage-owned Scene and a persistent sidebar. Portal entry uses approximately 1280×800 with a 1000×700 minimum, clamped to available screen space. Navigation retains the chosen window size and maximized state; login uses separate entry sizing.
- **Responsive content:** table/form workspaces switch between side-by-side and stacked layouts, with wrapping filters/actions and scrollable forms. Tables retain readable column widths with horizontal scrolling when needed.
- **Module transitions:** incoming main content fades and slides into place without animating the window or whole sidebar.
- **Dashboard motion:** a header entrance, staggered entrances for all eight metric cards, and restrained hover elevation/scale.
- **Interactions:** sidebar hover/accent feedback, an active-route indicator, button hover/press/disabled feedback, and field focus/blur transitions.
- **Dialogs and statuses:** styled dialogs with fade/scale entrances and text-preserving status chips; color is not the only status cue.
- **Consistency:** shared typography, spacing, surfaces, focus treatment, and a neutral/teal palette in the JavaFX stylesheet.

### Reduced motion

The existing JVM property disables entrance animation and applies interaction end states immediately:

```text
-Dhospital.ui.reduceMotion=true
```

On Ubuntu/Linux, pass it to the Maven-launched JVM using:

```bash
JAVA_TOOL_OPTIONS="-Dhospital.ui.reduceMotion=true" mvn javafx:run
```

Focus and status styling remain available. This invocation sets the option for that command; it does not change the application's default configuration.

## Screenshots

**Screenshots have not yet been added to this repository.** No generated or placeholder images are presented as application screenshots.

When real desktop captures are available, add them under a dedicated directory in `docs/` and link the actual committed files here. Useful views include Login, Dashboard, a table/form workspace, Billing, Reports, and a styled dialog. Use synthetic data and omit any personal information. No screenshot image links are included until those files exist.

## Developer notes

- Keep presentation in controllers/shared UI utilities, business rules in services, and persistence in DAOs. `Main.java` is the starting point for understanding application wiring.
- Preserve both UI role restrictions and service authorization when extending workflows; do not rely on hidden buttons as authorization.
- Use isolated test databases for automated tests and synthetic data for manual demonstrations.
- Before submitting changes, run `mvn clean test` and `git diff --check`; exercise affected desktop workflows as well.
- Do not commit `target/`, compiled classes, IDE settings, logs, local database files, SQLite sidecars, or private environment files. `.gitignore` covers these generated/local files without ignoring application source or tests.
- The Phase 9 verification document is retained as historical QA context rather than duplicated setup instructions.

## Known limitations

- This is a local desktop application with SQLite storage, not a deployed cloud service or remote multi-user system.
- No external hospital, insurance, pharmacy, or payment-processing integrations are implemented.
- Billing tracks bill amounts and statuses; it is not a payment gateway or detailed payment-transaction ledger.
- The UI does not provide a doctor-account/profile linking workflow. Unlinked doctors, including the seeded Doctor account, cannot perform ownership-restricted clinical writes. Appointment completion is Admin-only.
- Patients and doctors are retained through activation/deactivation rather than hard deletion.
- Existing reports are read-only views, not a role-isolated clinical data-access system; authenticated roles can view all available report types.
- Desktop rendering and interaction require manual verification in a graphical environment; the automated suite is not a UI test suite.
- The project is **not intended for production clinical use** and makes no real-world regulatory, privacy-compliance, or clinical-safety certification claim.

## Future development ideas

These are possible directions, **not implemented features or delivery commitments**:

- A web version using the same core domain concepts.
- A remote/multi-user architecture with suitable authentication and data-access controls.
- More advanced reporting and export options.
- A supported doctor-account/profile linking workflow.
- Additional hospital or administrative integrations.
