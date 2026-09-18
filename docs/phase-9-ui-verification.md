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

## Third pass — stable shell and visibly wired motion

This section supersedes the second-pass sizing and motion descriptions above.
Baseline for this pass: `5f2780a` (`UI: add premium interactions and visual polish`).

### Diagnosis and shell ownership

The old `SceneManager.show()` installed a different Scene on the Stage for every
route. Controllers supplied conflicting dimensions: Dashboard 1200×750,
Departments 1050×680, Patients 1280×760, Appointments 1300×760, Billing/Reports
1400×800, and others. Scene replacement therefore reintroduced each screen's
preferred window size. Fixed screen dimensions were not a responsive-shell policy.

The old motion hook depended on window-show events, not module navigation. There
was no central content transition; entrance movement was only 6 px and dashboard
stagger delays were capped at 50 ms. Registering effects was not enough to ensure
they played visibly after the destination had been mounted and laid out.

Changes:

- `SceneManager` now installs **one Scene once**, with a persistent BorderPane
  shell. Temporary controller Scenes only transport their view roots; they never
  reach `Stage.setScene`. All eleven controller Scene constructors are unsized.
- Login uses 1100×760, minimum 900×640. Entry into the authenticated portal uses
  **1280×800**, minimum **1000×700**. Entry sizes/minima are clamped to the current
  monitor's visual bounds. Main no longer supplies a conflicting minimum.
- Only login/portal boundaries invoke sizing. Ordinary module navigation does
  not set Stage size, position, maximized/full-screen state or call `sizeToScene`.
  Manual resizing and maximize/restore are left to the existing Stage.
- The 236 px rail, brand, navigation slots, scroll position and session/footer stay
  mounted. Only active/inactive navigation nodes are exchanged where necessary,
  using original controller nodes and callbacks. The active Doctors label maps
  to the existing Doctors slot despite its original “Doctor Management” text.
- Slot visibility/management follows the original role-filtered nodes. Shared
  helpers do not consult Session/Role or introduce a second permission system.
  Dashboard's original Logout button/handler remains mounted throughout the
  portal; logout discards the entire rail before a new user's login.
- Incoming page roots lose only their rail. All other regions—including headers
  and any footer—are retained in the shell's main content area.

### Responsive content review

All major views were inspected at source level. These are **conceptual layout
checks, not screenshots or measured JavaFX runtime results**. The table below
allows approximately 16 px for window decoration; actual OS metrics vary.

| Outer size | Approx. content width after rail | CRUD table/form layout | Dashboard featured/supporting columns | Report filter sections |
|---|---:|---|---|---|
| 1000×700 minimum | 748 px | Stacked, independently scrollable | 2 / 3 | 1 column |
| 1280×800 baseline | 1028 px | Side by side | 2 / 3 | 2 columns |
| 1440×900 | 1188 px | Side by side | 2 / 3 | 2 columns |
| 1920×1080 | 1668 px | Side by side | 2 / 3 | 2 columns |

- Patients, Doctors, Appointments, Medical Records, Prescriptions, Billing,
  Departments and Users all use the shared growing/scrollable workspace. Its
  width breakpoint is 1000 px of **content**, not outer-window width. The split
  divider now keeps at least 340 px for the list and 300 px for the editor.
- `UiStyles.apply()` now actually adapts every application FlowPane. Fixed
  preferred search/filter widths become upper bounds constrained to their row;
  action buttons can wrap and are capped to available row width. This also
  reaches Dashboard quick actions and Reports' date/filter/action rows.
- Forms retain growing single-column fields, wrapping text areas and vertical
  scrolling. Tables retain readable column minima with local horizontal
  scrolling, rather than widening the window or pushing filters offscreen.
- Dashboard retains its featured/supporting metric grids and scrollable content.
  Reports retains its distinct filter groups and results/summary area, with
  wrapping toolbars and vertically scrollable content. Login retains its capped,
  centered card in a fit-to-width/height viewport.
- Header identity columns can shrink/wrap while the existing action remains
  accessible. The sidebar's appropriate fixed width is not applied to content.

### Motion actually connected to screens

Entrance requests are prepared before mounting and start on a **one-shot
post-layout pulse in the live Scene**. Pending pulses and owned animations are
cancelled on outgoing views, rapid navigation, dialog close and window hide.
The Stage and entire sidebar are never faded, scaled or translated.

| Surface / interaction | Wired behavior |
|---|---|
| All ten portal modules | Main content opacity 0→1 and Y 12→0, 240 ms, EASE_OUT |
| Dashboard header | Additional 240 ms fade/10 px entrance |
| All eight dashboard metrics | 240 ms fade/10 px entrance; sequential 0, 40, 80, 120, 160, 200, 240, 280 ms delays; final card finishes at about 520 ms |
| Metric hover | 170 ms shadow elevation and scale 1→1.015, without changing layout bounds |
| Sidebar hover | 150 ms background-tone feedback and small graphic-accent opacity/scale; no whole-item translation or scale |
| Active indicator | 200 ms fade and Y-scale 0.25→1 on the teal indicator; active background/text emphasis retained |
| Buttons / quick actions | 150 ms tone/elevation feedback, pressed scale 0.975, disabled desaturation/lightening and normal disabled cursor |
| Text/choice/date fields | 150 ms border-color interpolation and gentle focus glow; hover/blur/disabled return states, no field movement |
| Application-owned dialogs | 200 ms fade and scale 0.97→1; showing-property listener does not overwrite dialog lifecycle/result handlers |
| Login card / validation messages | Login card 240 ms fade/10 px entrance; visible error/success labels retain 180 ms fade |

Animated field borders are bound to their interpolated color so CSS focus/hover
recalculation cannot snap them immediately to the destination color. Fields keep
the same one-pixel border geometry throughout the animation.

Transition coverage: **Dashboard, Patients, Doctors, Appointments, Medical
Records, Prescriptions, Billing, Reports, Departments, User Management**.
All 12 existing Alert creation sites still call `UiStyles.dialog`, which now
connects to the dialog-show motion hook. Original button types, action handlers,
result handling, native close, default/cancel and keyboard behavior are unchanged.
Table hover/selection and exact-text status chips remain restrained CSS/cell
presentation; no row-factory replacement, row animation loops or status pulses.

`-Dhospital.ui.reduceMotion=true` skips entrances and applies feedback end states
immediately. There are no new dependencies, fake loaders or background tasks.

### Verification performed in Arena

- `mvn clean test`: **could not run**, exit 127, `mvn: command not found`.
- `mvn javafx:run`: **could not run**, exit 127, `mvn: command not found`.
- Java and Maven are absent; no installation was attempted in this pass.
- **No compilation, 339-test result, JavaFX CSS/rendering or manual UI execution
  is claimed.** Local runtime confirmation remains required.
- Source comparison against `5f2780a`: **187 controller methods outside
  `buildScene` unchanged**; all original action/edit/listener bodies, controller
  fields, permission/state statements, sidebar definitions, dashboard shortcuts
  and eight statistic getters preserved. Main changed only sizing ownership.
- Source-evaluated role matrix: **30 screen/role combinations** retain exactly
  Admin's ten modules, Doctor's six (no Doctors/Billing/Departments/Users), and
  Receptionist's eight (no Departments/Users). Hidden nodes remain unmanaged.
- A separate source/algorithm check verified compatible persistent-slot keys and
  active/visibility updates across the requested navigation sequence and all
  module/role combinations. This is not a JavaFX binding/rendering test.
- Source contracts checked: one installed Scene; no controller dimensions or
  Stage setters; size calls only at login/portal entry; full incoming content
  preservation; post-layout motion hookup; finite durations; all dialog hooks;
  shared responsive adaptation; no action-handler replacement or row animation.
- Balanced source delimiters, CSS declaration syntax and `git diff --check`
  passed. Backend, schema, model, DAO, services, dependency definitions and
  existing test files were not changed.

### Local acceptance checklist — still pending

- [ ] Run `mvn clean test`: target 339 tests, zero failures/errors.
- [ ] Launch normally (without reduced motion) and inspect all ten modules.
- [ ] At 1280×800, 1440×900 and 1920×1080, follow Dashboard → Patients →
      Appointments → Billing → Reports → Dashboard; record unchanged Stage bounds.
- [ ] Manually resize, maximize, navigate, restore, and navigate again. Keep the
      chosen size, stable rail/scroll position and correctly highlighted route.
- [ ] Repeat rapid navigation before 240 ms and before the dashboard's last card
      enters. No stale pulses, invisible content, stuck scale or flicker.
- [ ] Verify header/card entrances, ordered 40 ms stagger, card hover, navigation
      hover/indicator, button hover/press/disabled, and form focus/blur feedback.
- [ ] Resize across the workspace breakpoint; drag the divider; inspect wrapped
      filters/buttons, readable table scrolling and vertically accessible forms.
- [ ] Verify all three roles and role-aware quick actions. Logout from a module,
      then log in as a different role; no stale user label or navigation access.
- [ ] Open each details/confirmation/error dialog, including billing, patient,
      medical-record and prescription details. Check fade/scale, Escape, Enter,
      Cancel, native close and unchanged confirmation results.
- [ ] Recheck appointment conflicts, record/prescription eligibility, bill totals
      and statuses, report filtering, Admin user/department operations and validation.
- [ ] Repeat with reduced motion; inspect long text, empty data, display scaling,
      table selection/status chips, and JavaFX console warnings.
