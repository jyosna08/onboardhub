# 06. UI Flow

React 18 + Vite 5 + Tailwind CSS 3.4. Dev server on port 3001. API base URL from `VITE_API_BASE_URL` (default `http://localhost:8090`).

## 1. Routes and Access

| Route                 | Access                                            | Screen                                                                    |
| :-------------------- | :------------------------------------------------ | :------------------------------------------------------------------------ |
| `/login`, `/register` | Public only (logged-in users are redirected home) | Auth screens (register creates an employee account)                       |
| `/dashboard`          | `HR`, `IT`, `ADMIN`                               | KPI cards and work queues                                                 |
| `/employees`          | `HR`, `IT`, `ADMIN`                               | Employee table with filters                                               |
| `/employees/:id`      | `HR`, `IT`, `ADMIN`                               | Employee detail and checklist                                             |
| `/assets`             | `HR`, `IT`, `ADMIN` (only `IT`/`ADMIN` can edit)  | Asset inventory                                                           |
| `/admin/team`         | `ADMIN`                                           | Create and list team accounts                                             |
| `/my-onboarding`      | `EMPLOYEE`                                        | Own onboarding progress and equipment                                     |
| `/`                   | any                                               | Redirect by role (`EMPLOYEE` to `/my-onboarding`, others to `/dashboard`) |

## 2. Screens

**Login / Register:** email, password (and name for register), inline validation, API error banner, loading state. Register states that HR and IT accounts are created by an administrator.

**Dashboard:** five KPI cards (Pending, Onboarding, Active employees, Open tasks, Overdue tasks; overdue highlighted) from `/api/onboarding/dashboard`, an asset stock strip from `/api/assets/summary` (per type: available / assigned), and an "Onboarding in progress" list (employees that are not yet ACTIVE, soonest joining date first, with their task progress).

**Employees:** search box (debounced), status and department filters, table (code, name, department, title, joining date, status chip, progress bar `done/total`), "Add employee" button (`HR`, `ADMIN`). Row click opens the detail page.

**Add employee modal:** name, email, department, job title, joining date, required equipment (multi-select chips for the six asset types, default Laptop). Submit creates the employee and opens its detail page.

**Employee detail:** header (name, code, email, department, title, joining date, status chip), progress bar, tasks grouped as "HR tasks", "IT tasks" and "Equipment". Each task row shows title, owner badge, due date (red when overdue), status chip and actions allowed for the current role: Start, Mark done, and for equipment tasks **Assign asset**. Completed equipment tasks show the asset tag and label.

**Assign asset modal:** lists `AVAILABLE` assets of the task's type (tag, make, model, serial), choose one and confirm. A 409 shows the backend message ("not available") and refreshes the list.

**Assets:** filters (type, status, search), table (tag, type, make and model, serial, status chip, assigned to), "Add asset" (`IT`, `ADMIN`). Row click opens a drawer with details, status actions (Mark in repair, Retire, Make available), **Return** for assigned assets and the assignment history.

**Team (admin):** form (name, email, password, role HR / IT / ADMIN) and a table of existing users with role chips.

**My onboarding (employee):** welcome header, progress bar, the checklist (read-only) and an "Equipment issued" card built from completed equipment tasks. If no record exists for the email it shows a friendly empty state ("HR has not created your onboarding record yet").

## 3. Folder Structure (`frontend/src`)

```
api/          axiosClient, authApi, userApi, employeeApi, taskApi, assetApi
context/      AuthContext
hooks/        useAuth, useDebounce, usePolling, useSubmitGuard, useEmployees, useAssets, ...
routes/       AppRoutes, ProtectedRoute, PublicOnlyRoute
pages/        LoginPage, RegisterPage, DashboardPage, EmployeesPage, EmployeeDetailPage,
              AssetsPage, TeamPage, MyOnboardingPage, NotFoundPage
components/
  layout/     AppLayout, Navbar, SideNav, UserMenu
  common/     Button, Spinner, Skeleton, ErrorBanner, NoticeBanner, EmptyState, Badge, Modal,
              SlideOver, FormField, Logo
  auth/       AuthLayout, LoginForm, RegisterForm
  dashboard/  KpiCard, KpiGrid, AssetStockStrip, AttentionList
  employees/  EmployeeTable, EmployeeFilters, AddEmployeeModal, StatusChip, ProgressBar
  onboarding/ TaskList, TaskRow, AssignAssetModal
  assets/     AssetTable, AssetFilters, AddAssetModal, AssetDrawer, AssetHistory
  admin/      CreateUserForm, UserTable
utils/        constants, validators, authStorage, jwt, errors, roleRoutes, dateTime, formatters
```

Rules: one component per file; pages only compose components; components never call `axios` directly; no raw role or status strings in components (use `utils/constants.js`).

## 4. Design Tokens

- Layout: left side navigation (collapses on mobile) with a top bar; light gray page background, white cards with a subtle border and rounded corners.
- Brand: indigo (`brand-600` for primary buttons and active nav). Success: emerald. Warning: amber. Danger: rose.
- Status chips: employee `PENDING` gray, `ONBOARDING` amber, `ACTIVE` emerald; task `TODO` gray, `IN_PROGRESS` blue, `DONE` emerald; asset `AVAILABLE` emerald, `ASSIGNED` indigo, `IN_REPAIR` amber, `RETIRED` gray. Owner badges: HR violet, IT sky.
- System font stack, no external font downloads.

## 5. Behaviour Decisions

- Session in `localStorage` key `onboardhub.session` (only through `utils/authStorage.js`); expired token clears the session; any `401` (except login and register) logs out.
- Lists and dashboards refresh every 30 seconds and after every action; a failed background refresh keeps the old data on screen.
- Dates (`yyyy-MM-dd`) are shown as local dates without time-zone shifts; timestamps are parsed as UTC through `utils/dateTime.js`.
- Mutating actions use the duplicate-submit guard (`useSubmitGuard`) and disabled buttons while pending.
- Every page has loading skeletons, an error banner with retry and an empty state.

## 6. Visual Design v2

- **Shell:** dark slate side navigation with a gradient logo mark, light content area, sticky top bar; the sidebar collapses behind a menu button on small screens.
- **Dashboard:** gradient welcome banner (greeting, role, today's date), KPI cards with coloured icon chips and a subtle hover lift, asset stock tiles with one icon per asset type and an available/assigned bar.
- **Employees:** initials avatars (colour derived from the name), status chips with a coloured dot, gradient progress bars, tables turn into stacked cards on small screens.
- **Employee detail:** header card with avatar; the checklist is a vertical timeline with check circles, owner badges and overdue dates in the danger colour.
- **Feedback:** skeleton loaders, 150 ms transitions, visible focus rings; animations are disabled with `prefers-reduced-motion`.
- **Constraint:** only Tailwind and lucide-react icons (no extra UI libraries).
