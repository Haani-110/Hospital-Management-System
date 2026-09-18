# Phase 9 — UI polish verification

## Scope

This is a JavaFX/CSS presentation pass. There are no FXML files or external UI libraries.

- `styles.css` owns the neutral/teal palette, typography, surfaces, buttons, focus states, tables, statuses and dialogs.
- `UiStyles` contains presentation helpers only. It must not contain role checks, service calls, validation or navigation decisions.
- Controllers retain their existing controls, callbacks, role checks, data sources and edit handlers. The billing Notes input is displayed once, beside its label in the form, instead of being reparented below the totals.
- All eight dashboard metrics still use the same getters. “Pending Appointments” is the display label for the existing pending count; the query is unchanged.
- Table column resize policies, sorting, selection, cell values and editable cell factories are retained. Minimum column widths and horizontal viewports keep text readable on smaller windows.
- Workspaces switch between side-by-side and stacked list/editor panes at 1,000 pixels of available content width. Both panes can be resized using the divider and scrolled. Filters and action buttons wrap independently of table width.

## Automated verification — local execution required

On a desktop with JDK 21 and Maven:

```bash
mvn clean test
mvn javafx:run
```

The expected existing suite remains **339 tests, 0 failures, 0 errors, 0 skipped**.
This is an expectation, **not a Phase 9 execution result**.

In the editing sandbox, both commands failed with `mvn: command not found`.
Compilation, JavaFX CSS parsing, visual inspection and interactive testing remain unverified here.
No build dependencies or test files were changed to work around the environment.

Static comparisons checked:

- Non-layout controller methods unchanged except calls that style dialogs.
- All action callbacks, edit-commit handlers and property listeners unchanged.
- All role checks and disabled/visible/managed conditions unchanged.
- Sidebar destinations, dashboard quick actions and eight metric getters retained.
- Every existing alert receives the shared stylesheet and button styling.
- No changes to Main navigation wiring, SceneManager, services, DAOs, models, database configuration/schema, dependencies or tests.

## Desktop smoke checklist

Use a disposable local database or test records for any mutation checks.
These checks are pending; do not interpret this list as completed testing.

### Sign-in and shell

- [ ] Empty/incorrect credentials show the original error text inside the login card.
- [ ] Tab and Shift+Tab reach username, password and Sign in in order; Enter still signs in.
- [ ] Valid login succeeds for each role; the header identifies the current user/role.
- [ ] Sidebar links and Back to Dashboard reach their original screens.
- [ ] Hidden navigation items have no gaps and cannot receive keyboard focus.
- [ ] Logout returns to sign-in; signing in under another role refreshes navigation correctly.

### Exact navigation matrix

| Module | Admin | Doctor | Receptionist |
|---|:---:|:---:|:---:|
| Dashboard | Yes | Yes | Yes |
| Departments | Yes | No | No |
| Users / User Management | Yes | No | No |
| Doctors | Yes | No | Yes |
| Patients | Yes | Yes | Yes |
| Appointments | Yes | Yes | Yes |
| Medical Records | Yes | Yes | Yes |
| Prescriptions | Yes | Yes | Yes |
| Billing | Yes | No | Yes |
| Reports | Yes | Yes | Yes |

- [ ] Check this matrix on every reachable screen, not just Dashboard.
- [ ] Quick actions retain the same role visibility and destinations as before Phase 9.

### Dashboard and tables

- [ ] All eight metrics are visible with both zero and nonzero data; large numbers remain readable.
- [ ] Quick actions wrap and the dashboard scrolls without covering the header.
- [ ] Visit all nine module screens; verify empty tables, populated tables, selection, sorting and refresh.
- [ ] Status text remains unchanged, with subtle styling for active/inactive, appointment and bill statuses.
- [ ] Scroll/reuse status cells and switch report types: old status colors must not leak into unrelated rows.
- [ ] Currency formats and numeric values are unchanged; money/count columns are right aligned.

### Forms and dialogs

- [ ] Select, edit, clear and create records using the original workflow in each module.
- [ ] Existing required fields, validation errors and role-based read-only controls still behave as before.
- [ ] All form fields, messages and action buttons are reachable by scrolling and keyboard navigation.
- [ ] Billing Notes appears once with its label; editing and saving notes still works.
- [ ] Prescription/billing item cells still support double-click editing, Enter, add and remove.
- [ ] Billing totals, discounts and status actions behave exactly as before.
- [ ] Delete, deactivate, cancel and completion confirmations retain their original results and cancel behavior.
- [ ] Password dialog retains its enable/disable and validation behavior.
- [ ] Patient, medical record, prescription and billing detail dialogs show all original text, including long notes/items, with scrolling.

### Reports, resizing and accessibility

- [ ] Each report type returns its original data; search, Apply and Clear behave as before.
- [ ] Date/status/active/department filters enable and disable as before; summary totals/counts match the results.
- [ ] Try 800×600, 1366×768 and a larger desktop window; also check OS display scaling.
- [ ] Resize across the workspace breakpoint; current selections and entered form values remain intact.
- [ ] Tables scroll horizontally rather than compressing columns to unreadable widths.
- [ ] Split-pane dividers work; no action button or form field becomes permanently clipped.
- [ ] Tab focus is visible on buttons, fields, selectors, tables and scroll areas.
- [ ] Inspect console output for JavaFX CSS warnings during all checks.
