# OnboardHub frontend

OnboardHub is a role-based employee onboarding and asset inventory interface built with React, Vite, Tailwind CSS, and lucide-react.

## Prerequisites

- Node.js 18 or newer
- npm

## Install and run

From this directory:

```sh
npm install
npm run dev
```

The Vite development server runs at `http://localhost:3001`.

## Test and build

```sh
npm test
npm run build
```

Set `VITE_API_BASE_URL` to the API gateway URL. It defaults to `http://localhost:8090`.

## Source layout

```text
api/          axiosClient, authApi, userApi, employeeApi, taskApi, assetApi
context/      AuthContext
hooks/        useAuth, useDebounce, usePolling, useSubmitGuard, useEmployees, useAssets, ...
routes/       AppRoutes, ProtectedRoute, PublicOnlyRoute
pages/        LoginPage, RegisterPage, DashboardPage, EmployeesPage, EmployeeDetailPage,
              AssetsPage, TeamPage, MyOnboardingPage, NotFoundPage
components/
  layout/     AppLayout, Navbar, SideNav, UserMenu
  common/     Button, Spinner, Skeleton, ErrorBanner, NoticeBanner, EmptyState, Badge, Modal,
              SlideOver, FormField, Logo, Avatar
  auth/       AuthLayout, LoginForm, RegisterForm
  dashboard/  KpiCard, KpiGrid, AssetStockStrip, AttentionList, WelcomeBanner
  employees/  EmployeeTable, EmployeeFilters, AddEmployeeModal, StatusChip, ProgressBar
  onboarding/ TaskList, TaskRow, AssignAssetModal, EquipmentIssuedCard
  assets/     AssetTable, AssetFilters, AddAssetModal, AssetDrawer, AssetHistory, AssetStatusChip
  admin/      CreateUserForm, UserTable
utils/        constants, validators, authStorage, jwt, errors, roleRoutes, dateTime, formatters
```

## Roles and landing pages

| Role | Landing page |
| :--- | :----------- |
| `ADMIN` | `/dashboard` |
| `HR` | `/dashboard` |
| `IT` | `/dashboard` |
| `EMPLOYEE` | `/my-onboarding` |

## Preview without a backend

The frontend only **decodes** the JWT to render role-specific screens; the backend validates the token. In the browser DevTools console on the app, set `role` to `ADMIN`, `HR`, `IT`, or `EMPLOYEE` and run this snippet to create an unsigned preview session:

```js
const role = 'ADMIN'; // ADMIN, HR, IT, or EMPLOYEE
const fullName = 'Preview User';
const email = 'preview@example.test';
const base64url = (value) => btoa(unescape(encodeURIComponent(JSON.stringify(value))))
  .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
const header = base64url({ alg: 'none', typ: 'JWT' });
const payload = base64url({ sub: 'preview-user', email, role, fullName, exp: Math.floor(Date.now() / 1000) + 3600 });
const token = `${header}.${payload}.x`;
localStorage.setItem('onboardhub.session', JSON.stringify({ token, userId: 'preview-user', fullName, role }));
location.assign(role === 'EMPLOYEE' ? '/my-onboarding' : '/dashboard');
```

Data requests will fail without a backend, so pages show their error and retry states. Real data flows are verified with the full stack.
