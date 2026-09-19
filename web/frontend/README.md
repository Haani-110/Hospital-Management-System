# Hospital Management System — v2.0 Web Frontend

React single-page application for the v2.0 web application. It talks to the
[`web/backend`](../backend/) NestJS API over HTTP and shows **live database records only** —
there is no mock data, no fake CRUD and no invented statistics anywhere in this app.

- **Stack:** React 19, TypeScript, Vite 8, React Router 7, Tailwind CSS 4, lucide-react.
- **Runs locally and for free:** no cloud service, no container runtime, no paid API.
- **Design:** dark-first healthcare interface with a light theme built from the same tokens.

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Environment variables](#environment-variables)
4. [Running the app](#running-the-app)
5. [Signing in](#signing-in)
6. [What is implemented](#what-is-implemented)
7. [Modules that are intentionally not connected yet](#modules-that-are-intentionally-not-connected-yet)
8. [Architecture](#architecture)
9. [Accessibility and responsiveness](#accessibility-and-responsiveness)
10. [Testing](#testing)
11. [Scripts](#scripts)
12. [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Version | Notes                                                    |
| ----------- | ------- | -------------------------------------------------------- |
| Node.js     | `>= 20.19` (22 LTS recommended) | Same requirement as the backend.       |
| npm         | 10+     | Ships with Node.                                          |
| The backend | v2.0 API | Running on `http://localhost:4000` with a seeded database. |

You need the API running before the interface shows any data. See
[`../backend/README.md`](../backend/README.md) for installing PostgreSQL, applying the
migration, seeding and starting the API.

## Installation

```bash
cd web/frontend
npm install
```

## Environment variables

```bash
cp .env.example .env
```

`.env` is git-ignored. **Never put a secret in a `VITE_*` variable** — Vite inlines those
values into the browser bundle, where anyone can read them. The frontend needs exactly one
value, and it is not a secret:

| Variable            | Example                       | Purpose                                  |
| ------------------- | ----------------------------- | ---------------------------------------- |
| `VITE_API_BASE_URL` | `http://localhost:4000/api/v1` | Base URL of the backend API (with version prefix). |

If `.env` is missing, the app falls back to `http://localhost:4000/api/v1`.

**Remote preview / non-localhost hosting.** When the page is opened through a preview URL
(the browser is not on the same machine as the API), pointing at `http://localhost:4000`
would call the *viewer's* computer. In that situation set:

```
VITE_API_BASE_URL=/api/v1
```

The Vite dev server proxies `/api` to `http://localhost:4000` (see
[`vite.config.ts`](vite.config.ts)), so the browser talks to the dev server and the dev
server talks to the API. The same proxy is what makes the sandboxed preview work.

## Running the app

Two terminals:

```bash
# terminal 1 — API
cd web/backend
npm run start:dev

# terminal 2 — interface
cd web/frontend
npm run dev
```

Then open:

| URL                              | What it is                          |
| -------------------------------- | ----------------------------------- |
| `http://localhost:3000`          | The web interface                   |
| `http://localhost:4000/api/v1`   | The API the interface calls         |
| `http://localhost:4000/api/docs` | Swagger UI for the same API         |

Production build:

```bash
npm run build     # type-checks, then bundles into dist/
npm run preview   # serves dist/ on http://localhost:3000
```

## Signing in

There is no public sign-up. Accounts come from the backend seed script
(`npm run db:seed` in `web/backend`), which prints the usernames it created and either uses
the passwords from `SEED_*_PASSWORD` variables or generates and prints random ones once.

With the backend's demo seed (`SEED_DEMO_DATA=true`) you get three roles to explore:

| Role           | Username       | Sees                                                   |
| -------------- | -------------- | ------------------------------------------------------ |
| `ADMIN`        | `admin`        | Everything, including user accounts                    |
| `DOCTOR`       | `doctor`       | Clinical screens; no billing, no user accounts          |
| `RECEPTIONIST` | `receptionist` | Clinical screens plus billing                          |

Sign-in calls `POST /api/v1/auth/login`, stores the returned JWT in `sessionStorage` (cleared when
the tab closes) and then confirms the brand-new session with `GET /api/v1/auth/me` before any
protected screen renders — a token the API will not accept is reported by the form itself instead
of showing up as an instant, unexplained return to the sign-in screen. On reload the stored token
is validated the same way. Expired or invalid tokens send you back to the sign-in screen; a `403`
explains that your role is not allowed and keeps you signed in.

The token lives in `sessionStorage` and is mirrored in one global slot shared by every copy of
`src/lib/api/token-store.ts`, so a duplicated module (a dev-server hot update, a re-imported
chunk) cannot leave the app signed in while its requests go out unauthenticated.

## What is implemented

Every screen below reads real records from the API.

| Screen            | Route                          | API used                                                                 |
| ----------------- | ------------------------------ | ------------------------------------------------------------------------ |
| Dashboard         | `/`                            | `GET /reports/dashboard-summary`                                         |
| Patients          | `/patients`                    | `GET /patients` (search, active filter, pagination)                       |
| Patient profile   | `/patients/:id`                | `GET /patients/:id` + `?patientId=` on appointments, medical records, prescriptions and bills |
| Doctors           | `/doctors`                     | `GET /doctors` (search, active filter, pagination)                        |
| Doctor profile    | `/doctors/:id`                 | `GET /doctors/:id`, `GET /appointments?doctorId=`                          |
| Appointments      | `/appointments`                | `GET /appointments` (patient, doctor, status, date)                       |
| Medical records   | `/medical-records`             | `GET /medical-records` (diagnosis search, patient, doctor, date range)    |
| Prescriptions     | `/prescriptions`               | `GET /prescriptions` (patient, prescriber, date range)                    |
| Billing           | `/billing`, `/billing/:id`     | `GET /bills`, `GET /bills/:id` — ADMIN and RECEPTIONIST only              |
| Reports           | `/reports`                     | dashboard summary plus filtered counts from every list endpoint           |
| Users             | `/users`                       | `GET /users`, `GET /users/:id` — ADMIN only                               |
| Settings          | `/settings`                    | `GET /auth/me` (session), `GET /health` (connection test), theme           |
| Not found         | any unknown route              | —                                                                         |

The list endpoints are **read-only in this backend version**: there are no create, update or
delete routes. The interface therefore offers no forms that would fail — empty states say
plainly that records are created through the backend or its seed script.

## Modules that are intentionally not connected yet

These screens exist so the navigation matches the full hospital workflow. They are honest
foundations: they state that no API exists, list the workflow that will live there, and link
to related records that *are* stored today. No placeholder patients, stock or beds are shown.

| Module     | Route          | Why it is empty                                                     |
| ---------- | -------------- | ------------------------------------------------------------------- |
| Laboratory | `/laboratory`  | No test catalogue, order or result endpoints in the API              |
| Pharmacy   | `/pharmacy`    | No medicine, stock or dispensing endpoints                           |
| Admissions | `/admissions`  | No ward, bed or inpatient-stay endpoints                             |

Other known gaps, shown in the UI where relevant:

- No departments endpoint, so doctors cannot be filtered by department (the department is
  shown per doctor instead).
- `GET /appointments` and `GET /bills` accept no free-text search; the appointment picker on
  the billing screen therefore filters the most recent appointments in the browser and says so.
- Bills cannot be receipted or adjusted from the UI (read-only API).
- Passwords and profile details cannot be changed from the UI (no endpoint).

## Architecture

```text
src/
├── App.tsx                     # route table
├── main.tsx                    # providers + mount
├── index.css                   # design tokens (light & dark) and utilities
├── components/
│   ├── ui/                     # primitives: button, input, card, dialog, tabs, table, …
│   ├── layout/                 # app shell, sidebar, topbar, page header, error boundary
│   ├── data/                   # list/detail states, pickers, badges, stat cards, tables
│   ├── charts/                 # dependency-free SVG donut and bar charts
│   └── motion/                 # page transition
├── features/                   # one folder per module (page components)
├── lib/
│   ├── api/                    # typed API layer — the only place `fetch` is called
│   ├── hooks/                  # useApi, useListQuery, useDebouncedValue, useMediaQuery
│   ├── labels.ts, date-range.ts, utils.ts
└── providers/                  # auth (session) and theme contexts
```

Key decisions:

- **One API layer.** `src/lib/api/` owns the base URL, the bearer token, JSON decoding and
  error normalisation. `types.ts` mirrors the backend DTOs exactly, including decimal values
  arriving as strings, and is the only place request/response shapes are declared.
- **URL is the state.** List filters, search text and page numbers live in the query string,
  so a filtered view can be reloaded or shared. Search input writes immediately; the request
  waits for a debounced value.
- **`useApi` instead of a data library.** A small hook tracks loading/error/data per request,
  which keeps the dependency list short and the behaviour explicit.
- **Role-aware, not role-enforcing.** Navigation hides what your role cannot use, but every
  request is authorised by the API — the UI never invents its own permission system.
- **No fabricated content.** Charts render only from `dashboard-summary` and list counts;
  where the database is empty the screen shows an empty state or zeros.

## Accessibility and responsiveness

- Semantic landmarks (`header`, `nav`, `main`, `footer`), one `h1` per screen, real tables
  with captions, labelled form controls, visible focus rings.
- Dialogs use the native `<dialog>` element, so focus trapping, Escape and the inert
  background come from the browser. Pickers implement the ARIA combobox pattern with
  Arrow/Enter/Escape keys. Status is never conveyed by colour alone.
- Loading, empty, error and forbidden states are announced through `role="status"` /
  `role="alert"`.
- Motion is subtle and respects `prefers-reduced-motion` (page fade-up, card stagger, button
  press states only).
- Layout works from small phones up: persistent sidebar from `lg`, a modal drawer below it,
  and tables that scroll horizontally instead of breaking the page.

## Testing

```bash
npm test           # unit, component and accessibility tests
npm run typecheck  # tsc --noEmit, project references
npm run lint       # eslint, warnings are errors
npm run build      # type-check + production bundle
```

Covered: the API client (query building, validation errors, 401 handling, 403 without
session loss, transport failures), formatters and decimal handling, the sign-in flow
(validation, invalid credentials, success, password toggle), protected-route behaviour and
`axe-core` accessibility checks for the sign-in screen and the app shell.

**Live integration suite (opt-in).** It renders the real app against a running API and
asserts real records, including walking every route. It is skipped unless you provide
credentials:

```bash
# with the backend running and seeded
RUN_LIVE_TESTS=true \
LIVE_ADMIN_USERNAME=admin LIVE_ADMIN_PASSWORD=<seeded password> \
LIVE_DOCTOR_USERNAME=doctor LIVE_DOCTOR_PASSWORD=<seeded password> \
npm test
```

No password is stored in this repository — they are read from the environment only.

## Scripts

| Script               | What it does                                   |
| -------------------- | ---------------------------------------------- |
| `npm run dev`        | Vite dev server on `http://localhost:3000`      |
| `npm run build`      | Type-check and build to `dist/`                 |
| `npm run preview`    | Serve the production build on port 3000         |
| `npm test`           | Vitest run (add `RUN_LIVE_TESTS=true` for live) |
| `npm run test:watch` | Vitest in watch mode                            |
| `npm run typecheck`  | `tsc -b --noEmit`                               |
| `npm run lint`       | ESLint with zero-warning policy                 |
| `npm run lint:fix`   | ESLint with `--fix`                             |

## Troubleshooting

| Symptom                                        | Cause and fix                                                                                             |
| ---------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| “Cannot reach the API” on every screen         | The backend is not running. Start it on port 4000 and confirm `curl http://localhost:4000/api/v1/health`.  |
| Login succeeds but requests fail with 401      | `JWT_SECRET` changed since the token was issued. Sign out and sign in again.                                |
| Signed in, then instantly back at sign-in      | The request after `POST /auth/login` reached the API without a token (the backend log shows it as `401` in ~0 ms). Reload the page — a dev-server hot update can leave one copy of the token store holding the session while another sends the requests — and confirm the request carries `Authorization: Bearer …` in the Network tab. |
| Browser console shows a CORS error             | Your origin is not in the backend's `CORS_ORIGINS` (default allows `http://localhost:3000`).                |
| The screen is empty rather than broken         | Your database has no records yet — run the backend seed script.                                            |
| Billing or Users screens say “no permission”   | Working as intended: those endpoints are ADMIN/RECEPTIONIST (bills) and ADMIN (users) only.                |
| Preview URL shows the API as unreachable       | Use a relative base: `VITE_API_BASE_URL=/api/v1` so the dev server proxies the API (see above).             |
