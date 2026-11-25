# 05. API Contract

All endpoints go through the gateway (`http://localhost:8090`). Protected endpoints need `Authorization: Bearer <JWT>`.
Dates are `yyyy-MM-dd`. Timestamps are UTC without a zone suffix (for example `2026-12-01T09:30:00`); clients convert them to local time.

## 0. Standard error payload (all services)

```json
{
  "timestamp": "2026-12-01T09:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Asset LAP-0007 is not available",
  "path": "/api/assets/7/assign"
}
```

Status codes: `400` validation, `401` missing or invalid token, `403` wrong role, `404` not found, `409` rule conflict, `503` dependency unavailable, `500` unexpected.

---

## 1. Auth & Team Service (`/api/auth`, `/api/users`)

### POST `/api/auth/register`

- **Auth:** Public. Creates `EMPLOYEE` accounts only.
- **Body:** `{"fullName": "Asha Rao", "email": "asha@example.com", "password": "Password@123"}` (`role` is optional; if sent it must be `EMPLOYEE`).
- **Validation:** `fullName` (NotBlank, max 100), `email` (valid, max 150, stored lowercase, unique), `password` (8 to 100 chars).
- **201:** `{"id": 5, "fullName": "Asha Rao", "email": "asha@example.com", "role": "EMPLOYEE"}`
- **Errors:** `400`, `403` (another role requested), `409` (email exists).

### POST `/api/auth/login`

- **Auth:** Public. **Body:** `{"email": "...", "password": "..."}`
- **200:** `{"token": "eyJ...", "userId": 5, "fullName": "Asha Rao", "role": "EMPLOYEE"}`
- **Errors:** `400`, `401` ("Invalid email or password" for unknown email and wrong password alike).

### POST `/api/users`

- **Auth:** `ADMIN` only.
- **Body:** `{"fullName": "Ravi HR", "email": "ravi@example.com", "password": "Password@123", "role": "HR"}`; `role` is required (`ADMIN`, `HR`, `IT`, `EMPLOYEE`).
- **201:** `UserResponse`. **Errors:** `400`, `401`, `403`, `409`.

### GET `/api/users`

- **Auth:** `ADMIN` only. Optional `?role=HR`.
- **200:** array of `UserResponse` (`id`, `fullName`, `email`, `role`), newest first.

### GET `/api/users/me`

- **Auth:** any authenticated user. **200:** `UserResponse` of the caller.

### First administrator (bootstrap)

On startup `auth-team-service` creates one `ADMIN` from `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`, only if email and password are set (password at least 8 characters) and no `ADMIN` exists. It never changes an existing user and never fails startup.

---

## 2. Employee Onboarding Service (`/api/employees`, `/api/onboarding`)

### Shared shapes

`Task`:

```json
{
  "id": 41,
  "title": "Assign LAPTOP",
  "category": "EQUIPMENT",
  "ownerRole": "IT",
  "status": "TODO",
  "dueDate": "2026-12-01",
  "overdue": false,
  "assetType": "LAPTOP",
  "assetId": null,
  "assetTag": null,
  "assetLabel": null,
  "completedAt": null,
  "completedByName": null
}
```

`EmployeeDetail`:

```json
{
  "id": 12,
  "employeeCode": "EMP-0012",
  "fullName": "Asha Rao",
  "email": "asha@example.com",
  "department": "ENGINEERING",
  "jobTitle": "Backend Engineer",
  "joiningDate": "2026-12-02",
  "status": "ONBOARDING",
  "onboardingCompletedAt": null,
  "createdAt": "2026-11-28T08:00:00",
  "tasks": []
}
```

`EmployeeSummary` is the same without `tasks` and with `tasksDone` and `tasksTotal`.

### POST `/api/employees`

- **Auth:** `HR`, `ADMIN`.
- **Body:**

```json
{
  "fullName": "Asha Rao",
  "email": "asha@example.com",
  "department": "ENGINEERING",
  "jobTitle": "Backend Engineer",
  "joiningDate": "2026-12-02",
  "requiredAssetTypes": ["LAPTOP", "MONITOR"]
}
```

- **Validation:** `fullName` (NotBlank, max 100), `email` (valid, max 150, lowercase, unique), `department` (enum), `jobTitle` (NotBlank, max 100), `joiningDate` (NotNull), `requiredAssetTypes` (optional, default `["LAPTOP"]`, up to 6 distinct valid types).
- **201:** `EmployeeDetail` with the generated checklist.
- **Checklist template** (due dates relative to `joiningDate`):

| #    | Title                                   | Category      | Owner | Due              |
| :--- | :-------------------------------------- | :------------ | :---- | :--------------- |
| 1    | Collect identity and joining documents  | DOCUMENTATION | HR    | joining - 2 days |
| 2    | Sign offer letter and policies          | DOCUMENTATION | HR    | joining - 1 day  |
| 3    | Create company email account            | ACCOUNT_SETUP | IT    | joining - 1 day  |
| 4    | Grant system access                     | ACCOUNT_SETUP | IT    | joining          |
| 5..  | Assign `<TYPE>` (one per required type) | EQUIPMENT     | IT    | joining - 1 day  |
| last | Orientation session                     | ORIENTATION   | HR    | joining + 1 day  |

- **Errors:** `400`, `401`, `403`, `409` (email exists).

### GET `/api/employees?status=&department=&search=`

- **Auth:** `HR`, `IT`, `ADMIN`. All parameters optional and combinable; `search` matches name, email or code (case-insensitive). Invalid enum value gives `400`.
- **200:** array of `EmployeeSummary`, newest first.

### GET `/api/employees/{id}`

- **Auth:** `HR`, `IT`, `ADMIN`; `EMPLOYEE` only for their own record (matched by email), otherwise `404`.
- **200:** `EmployeeDetail`. **Errors:** `401`, `404`.

### GET `/api/employees/me`

- **Auth:** `EMPLOYEE`. **200:** `EmployeeDetail` of the caller (matched by email). **404** if no employee record exists for that email.

### PATCH `/api/employees/{id}`

- **Auth:** `HR`, `ADMIN`. **Body (all optional):** `fullName`, `department`, `jobTitle`, `joiningDate`. Email cannot change. Changing `joiningDate` does not move existing task due dates.
- **200:** `EmployeeDetail`. **Errors:** `400`, `403`, `404`.

### PATCH `/api/onboarding/tasks/{taskId}/status`

- **Auth:** the task's owner role (`HR` tasks: `HR`/`ADMIN`; `IT` tasks: `IT`/`ADMIN`); any other role gets `403`.
- **Body:** `{"status": "IN_PROGRESS"}`
- **Transitions:** `TODO` to `IN_PROGRESS`, `IN_PROGRESS` to `TODO`, `TODO` or `IN_PROGRESS` to `DONE`. `DONE` is final.
- **200:** the updated `Task` (and the employee status is recomputed).
- **Errors:** `400`, `403`, `404`, `409` (invalid transition, task already `DONE`, or an `EQUIPMENT` task being set to `DONE` manually).

### POST `/api/onboarding/tasks/{taskId}/assign-asset`

- **Auth:** `IT`, `ADMIN`.
- **Body:** `{"assetId": 7}`
- **Behaviour:** see `03_ARCHITECTURE.md` section 5.2. On success the task is `DONE` with `assetId`, `assetTag` and `assetLabel` (for example `Dell Latitude 5440`) and `completedByName` set.
- **200:** the updated `Task`.
- **Errors:** `400`, `403`, `404` (task or asset), `409` (not an equipment task, task already `DONE`, asset not `AVAILABLE`, asset type does not match), `503` (asset service unavailable).

### GET `/api/onboarding/dashboard`

- **Auth:** `HR`, `IT`, `ADMIN`.
- **200:** `{"pendingEmployees": 3, "onboardingEmployees": 5, "activeEmployees": 20, "openTasks": 31, "overdueTasks": 4}`

---

## 3. Asset Inventory Service (`/api/assets`)

`Asset`:

```json
{
  "id": 7,
  "assetTag": "LAP-0007",
  "type": "LAPTOP",
  "make": "Dell",
  "model": "Latitude 5440",
  "serialNumber": "DL5440-8832",
  "status": "AVAILABLE",
  "purchaseDate": "2026-09-15",
  "assignedEmployeeId": null,
  "assignedEmployeeName": null,
  "assignedAt": null,
  "createdAt": "2026-11-28T08:00:00"
}
```

### POST `/api/assets`

- **Auth:** `IT`, `ADMIN`.
- **Body:** `{"type": "LAPTOP", "make": "Dell", "model": "Latitude 5440", "serialNumber": "DL5440-8832", "purchaseDate": "2026-09-15"}`
- **Validation:** `type` (enum), `make` (NotBlank, max 60), `model` (NotBlank, max 80), `serialNumber` (NotBlank, max 80, unique), `purchaseDate` (optional, not in the future).
- **201:** `Asset` with generated `assetTag` and status `AVAILABLE`. **Errors:** `400`, `403`, `409` (serial exists).

### GET `/api/assets?type=&status=&search=`

- **Auth:** `HR`, `IT`, `ADMIN`. Filters optional; `search` matches tag, make, model or serial. **200:** array of `Asset`, newest first.

### GET `/api/assets/{id}`

- **Auth:** `HR`, `IT`, `ADMIN`. **200:** `Asset`. **404** if missing.

### PATCH `/api/assets/{id}/status`

- **Auth:** `IT`, `ADMIN`. **Body:** `{"status": "IN_REPAIR"}` with `AVAILABLE`, `IN_REPAIR` or `RETIRED`.
- **Rules:** an `ASSIGNED` asset must be returned first; `ASSIGNED` cannot be set here. **200:** `Asset`. **Errors:** `400`, `403`, `404`, `409`.

### POST `/api/assets/{id}/assign`

- **Auth:** `IT`, `ADMIN` (called by the employee service with the forwarded token).
- **Body:** `{"employeeId": 12, "employeeName": "Asha Rao", "expectedType": "LAPTOP"}`
- **Rules:** atomic; the asset must be `AVAILABLE` and its type must equal `expectedType`. A history row is created with `assignedByName` taken from the token.
- **200:** `Asset` (status `ASSIGNED`). **Errors:** `400`, `403`, `404`, `409` ("Asset LAP-0007 is not available" or "Asset type does not match").

### POST `/api/assets/{id}/return`

- **Auth:** `IT`, `ADMIN`. **Rules:** the asset must be `ASSIGNED`. The open history row gets `returnedAt` and `returnedByName`. Returning does not reopen an onboarding task.
- **200:** `Asset` (status `AVAILABLE`). **Errors:** `403`, `404`, `409`.

### GET `/api/assets/{id}/history`

- **Auth:** `HR`, `IT`, `ADMIN`.
- **200:** `[{"id": 3, "employeeId": 12, "employeeName": "Asha Rao", "assignedByName": "Ravi IT", "assignedAt": "2026-11-29T10:00:00", "returnedAt": null, "returnedByName": null}]`, newest first.

### GET `/api/assets/summary`

- **Auth:** `HR`, `IT`, `ADMIN`.
- **200:**

```json
{
  "total": 40,
  "available": 22,
  "assigned": 15,
  "inRepair": 2,
  "retired": 1,
  "byType": [
    {
      "type": "LAPTOP",
      "available": 6,
      "assigned": 9,
      "inRepair": 1,
      "retired": 0
    }
  ]
}
```

---

## 4. Health and docs

`GET /actuator/health` (public) on every service; Swagger UI at `/swagger-ui.html` on `auth-team-service` (8091), `employee-onboarding-service` (8092) and `asset-inventory-service` (8093).
