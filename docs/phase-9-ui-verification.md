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

## Second visual pass — premium presentation

This pass updates only Login, Dashboard and Reports composition, the shared
presentation helpers, and CSS. Other module controllers pick up the common
header, grouped rail, form sections, status chips and dialog treatment without
changes to their callbacks or rules.

### Visual changes

- A deep-teal rail groups the **existing** navigation nodes. Group headings
  disappear when all of their links are hidden. The same user/role label is moved
  into the staff-session footer; Dashboard's existing Logout button is moved
  there too. Other pages retain Back to Dashboard; no new logout route is added.
- Dashboard retains all eight metric getters. Today's appointments and paid
  revenue are featured above six supporting metrics and the original quick actions.
- Headers gain short context descriptions. Form section labels separate related
  fields without replacing controls or changing their traversal order.
- Status cells use reusable Label chips, retaining the exact original status text
  and an accessible text equivalent. Graphics are cleared on empty/non-status cells.
- Reports separates report/search selection, date range, additional filters,
  Apply/Clear actions, and the results/summary surface.
- Empty states give context without creating records, changing filters or making
  permission decisions. They are left-aligned to remain in the visible portion
  of wide, horizontally scrollable tables.
- Dialogs use a custom header and inset surface while retaining native window
  controls, original text, button types, default/cancel behavior and result checks.

### Motion and effects

`UiMotion` is presentation-only and uses standard JavaFX APIs:

| Interaction | Treatment |
|---|---|
| Login and dashboard entrance | 180 ms fade + 200 ms / 6 px slide; card delays capped at 50 ms |
| Active navigation indicator | 180 ms scale on the small indicator, not the whole link |
| Buttons / quick actions | 150 ms tone interpolation for hover, pressed and disabled states |
| Field focus | 150 ms subtle glow |
| Statistic-card hover | 180 ms light shadow elevation; no card movement |
| Error/success message appearance | 180 ms fade |
| Dialog entrance | Fade/slide with a restrained 0.99 → 1 scale over 200 ms |

There are no indefinite animations, fake loaders, table-row animation loops,
background tasks or new dependencies. In-flight transitions are replaced on fast
state changes, one-shot entrance listeners detach after showing, and node-detach
cleanup stops owned transitions. Motion never invokes or replaces an action handler.

**Reduced motion:** start the JVM with `-Dhospital.ui.reduceMotion=true`.
For example, in a POSIX shell:

```bash
JAVA_TOOL_OPTIONS="-Dhospital.ui.reduceMotion=true" mvn javafx:run
```

This skips entrance/indicator/message animation and applies hover, disabled and
focus states immediately. Static depth and focus visibility remain available.

### Additional local checks — pending

Both `mvn clean test` and `mvn javafx:run` were attempted again in Arena and failed
with `mvn: command not found`. The 339-test target and JavaFX rendering are still
**not verified in this environment**.

- [ ] Repeat the complete role matrix and workflow checklist above.
- [ ] Verify sidebar groups have no empty headings/gaps for Doctor/Receptionist.
- [ ] Use the staff-session Logout button; confirm its original behavior.
- [ ] Navigate rapidly during entrances, including leaving Dashboard immediately.
- [ ] Hover/press/release buttons quickly, including keyboard Space/Enter and disabled actions.
- [ ] Trigger invalid login and form errors; ensure messages remain readable after animation.
- [ ] Open/close each dialog quickly; test Escape, Enter, Cancel and the native close button.
- [ ] Reuse/sort/scroll status cells and switch report types; no stale chips or duplicate text.
- [ ] Resize the feature/support metric grids and report filter groups across their breakpoints.
- [ ] Check long names, long detail text, zero/large metric values, empty lists and OS display scaling.
- [ ] Repeat with reduced motion enabled; nothing should remain transparent or scaled down.
- [ ] Inspect JavaFX console output for CSS warnings; confirm no continued motion while idle.
