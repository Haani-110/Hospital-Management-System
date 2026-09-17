# Hospital Management System (Phase 1)

A **100% free, local** Java desktop application for managing a small hospital.
This is Phase 1 of a university Java project. The application runs **offline**
using JavaFX for the UI and SQLite for data storage. No cloud services, no
databases other than SQLite, and no paid tools are used.

---

## Project overview

The goal is a small but complete Hospital Management System built as a layered
Java desktop application. Right now (Phase 1) the foundation is in place:
Maven build, JavaFX window, SQLite database, login screen, demo users, and a
basic dashboard shell.

Future phases will add Patients, Doctors, Departments management,
Appointments, Medical Records, Prescriptions, and Billing modules.

---

## Technologies (Phase 1)

| Tool / library | Version | Purpose |
|----------------|---------|---------|
| Java / OpenJDK | **21 LTS** | Language + runtime |
| JavaFX (controls, graphics, fxml) | 21.0.2 | Desktop UI toolkit |
| SQLite JDBC (`org.xerial:sqlite-jdbc`) | 3.45.1.0 | Local database driver |
| Maven | – | Build + dependency management |
| JUnit 5 | 5.10.2 | Automated tests |
| CSS | – | UI styling |

The database is a local file: `data/hospital.db` (created automatically).

---

## Requirements

* Ubuntu Linux (tested on Ubuntu 22.04/24.04)
* **Java 21 JDK** (e.g. `openjdk-21-jdk`)
* **Maven 3.8+**
* **sqlite3** command-line shell (for inspecting the DB)

Install them on Ubuntu:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven sqlite3
```

Verify:

```bash
java -version        # should show openjdk version "21"
mvn -version         # should show Apache Maven 3.8+
sqlite3 --version
```

---

## Project structure

```text
Hospital-Management-System/
├── pom.xml
├── README.md
├── .gitignore
├── data/                      # auto-created SQLite data directory
│   └── .gitkeep
└── src/
    ├── main/
    │   ├── java/com/hospital/
    │   │   ├── Main.java
    │   │   ├── config/        # DatabaseConnection, DatabaseInitializer, DatabaseConfig
    │   │   ├── model/         # User, Department, Role enum
    │   │   ├── dao/           # UserDao, DepartmentDao (+ implementations)
    │   │   ├── service/       # AuthService, Session
    │   │   ├── controller/    # LoginController, DashboardController
    │   │   ├── util/          # PasswordUtil, SceneManager
    │   │   └── exception/     # DatabaseException, AuthenticationException
    │   └── resources/com/hospital/
    │       └── css/styles.css
    └── test/java/com/hospital/
        ├── config/            # DB initializer tests, TestDatabaseFactory
        ├── dao/UserDaoTest.java
        └── service/AuthServiceTest.java
```

The architecture follows a clean layered pattern that is easy to understand
for a university student:

```text
JavaFX UI  →  Controllers  →  Services  →  DAO  →  SQLite
```

---

## How to build

From the project root:

```bash
mvn clean compile
```

## How to run automated tests

```bash
mvn clean test
```

All tests use a **temporary** SQLite file (`/tmp/hospital-test-*.db`) so they
never touch your real `data/hospital.db`. The tests verify:

* Database connection opens successfully
* Database initializer creates the required tables
* Required tables (`users`, `departments`) exist with expected columns
* Demo users are seeded on first run
* Demo-user seeding is idempotent
* Valid credentials authenticate
* Invalid / blank credentials are rejected
* User roles are returned correctly (ADMIN, DOCTOR, RECEPTIONIST)
* Duplicate usernames raise a clear database error
* Passwords are **not** stored in plain text (PBKDF2-HMAC-SHA256 + salt)
* Logout clears the session

## How to run the application

```bash
mvn javafx:run
```

The first launch creates `data/hospital.db`, creates the tables, and seeds
the three demo accounts.

---

## Demo credentials

Passwords are **not** stored in plain text; they are hashed with PBKDF2-HMAC-SHA256
with a per-user random salt.

| Role         | Username       | Password         |
|--------------|----------------|------------------|
| Administrator| `admin`        | `admin123`       |
| Doctor       | `doctor`       | `doctor123`      |
| Receptionist | `receptionist` | `receptionist123`|

Use any of these at the login screen. After logging in you will see the basic
dashboard shell with your username, your role, a disabled sidebar placeholder
for future modules, and a working Logout button.

---

## Manual database inspection on Ubuntu

After running the app once, you can inspect the generated SQLite file from
the command line:

```bash
# 1. Open the database with the sqlite3 shell
sqlite3 data/hospital.db
```

Inside the `sqlite3` prompt you can run:

```sql
-- List all tables
.tables

-- Show the CREATE statements (verifies schema and constraints)
.schema users
.schema departments

-- See the three seeded users (password_hash column is a PBKDF2 hash)
SELECT id, username, role, created_at FROM users;

-- Departments are empty in Phase 1
SELECT * FROM departments;

-- Quit
.quit
```

You can also run one-off queries without entering the prompt:

```bash
sqlite3 data/hospital.db "SELECT id, username, role FROM users;"
sqlite3 data/hospital.db ".tables"
```

---

## Git notes

* `target/`, `*.class`, and `.env` are excluded via `.gitignore`.
* `data/hospital.db` is **not** committed. It is generated locally the first
  time the application runs, which is the clean approach for a university
  project: every developer (and the marker) starts with a fresh database,
  demo users are recreated automatically, and there is no risk of
  accidentally committing a stale or personal database.
* The empty `data/` directory is preserved via `data/.gitkeep` so it exists
  after cloning.

---

## Phase 1 features (implemented)

* Maven project configured for Java 21 + JavaFX 21 + SQLite JDBC + JUnit 5
* JavaFX application that launches with a proper window (min size 800x600)
* CSS styling loaded from the classpath
* Local SQLite database at `data/hospital.db`, auto-created on first run
* `DatabaseConnection` utility that opens JDBC connections and enables FKs
* `DatabaseInitializer` that creates `users` and `departments` tables (safe
  to run on every startup)
* Tables use primary keys, `NOT NULL`, `UNIQUE`, and `CHECK` constraints
* Password hashing utility using PBKDF2-HMAC-SHA256 with per-user salt
* Three demo users auto-seeded if the users table is empty
* Clean login screen (title, username, password, login button, error area)
* Simple in-memory `Session` for the currently logged-in user
* Basic dashboard shell with app title, username/role display, logout button,
  and disabled sidebar items for future modules
* JUnit 5 test suite covering DB connectivity, schema creation, seeding,
  authentication, role lookup, and duplicate username handling
* `mvn clean test` passes

## Future modules (NOT implemented yet)

The dashboard sidebar currently contains **placeholder** (disabled) entries
for these upcoming modules:

* Patients
* Doctors
* Departments management
* Appointments
* Medical Records
* Prescriptions
* Billing

These will be added in later phases on request.
