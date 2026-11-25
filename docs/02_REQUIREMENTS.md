# 02. Requirements

## 1. Functional Requirements

### Authentication and users

- **FR-A1:** Public registration creates `EMPLOYEE` accounts only. Requesting any other role returns `403`.
- **FR-A2:** Login returns a JWT (HS256) with claims `sub` (user id), `email`, `role`, `fullName`.
- **FR-A3:** An `ADMIN` can create users with any role (`ADMIN`, `HR`, `IT`, `EMPLOYEE`) and list users.
- **FR-A4:** The first `ADMIN` is bootstrapped from environment variables if no admin exists.

### Employees and onboarding

- **FR-E1:** `HR` or `ADMIN` creates an employee (name, email, department, job title, joining date, required asset types). The email must be unique.
- **FR-E2:** Creating an employee generates a checklist: 3 HR tasks, 2 IT tasks and one equipment task per required asset type, each with an owner role and a due date derived from the joining date.
- **FR-E3:** Task statuses are `TODO`, `IN_PROGRESS`, `DONE`. `DONE` is final. A task can only be updated by its owner role (or `ADMIN`).
- **FR-E4:** Equipment tasks are completed only by assigning an asset (FR-S4), not by changing the status manually.
- **FR-E5:** Employee status is derived: `PENDING` (no task started), `ONBOARDING` (at least one task started or done), `ACTIVE` (all tasks done).
- **FR-E6:** Employees can be searched by name, email or code and filtered by status and department.
- **FR-E7:** An `EMPLOYEE` user sees only their own record (matched by email) and checklist.
- **FR-E8:** A task is overdue when its due date is before today and it is not `DONE`.

### Assets

- **FR-S1:** `IT` or `ADMIN` adds assets (type, make, model, serial number, purchase date). Serial numbers are unique. An asset tag (for example `LAP-0007`) is generated.
- **FR-S2:** Asset statuses: `AVAILABLE`, `ASSIGNED`, `IN_REPAIR`, `RETIRED`. `ASSIGNED` is set only by assignment.
- **FR-S3:** Assets can be listed and filtered by type, status and search text.
- **FR-S4:** Assigning an asset requires status `AVAILABLE` and an asset type that matches the equipment task. The check is atomic: two simultaneous assignments of the same asset cannot both succeed.
- **FR-S5:** Returning an `ASSIGNED` asset makes it `AVAILABLE` again.
- **FR-S6:** Every assignment and return is stored in an assignment history per asset.

### Dashboards

- **FR-D1:** Onboarding dashboard: pending, onboarding and active employees, open tasks, overdue tasks.
- **FR-D2:** Asset summary: totals and a per-type breakdown by status.

### UI

- **FR-U1:** Role-based navigation and landing pages; route guards.
- **FR-U2:** Every page has loading, error and empty states, form validation and a responsive layout.
- **FR-U3:** Data refreshes every 30 seconds and after every action; forms cannot be submitted twice by a double click.

## 2. Non-Functional Requirements

| ID    | Requirement                                                                                  |
| :---- | :------------------------------------------------------------------------------------------- |
| NFR-1 | Stateless services; JWT validated by every service with a shared secret                      |
| NFR-2 | Passwords hashed with BCrypt; no secrets in git; configuration through environment variables |
| NFR-3 | All timestamps are UTC; dates are ISO `yyyy-MM-dd`                                           |
| NFR-4 | Backend tests run offline (H2, mocked Feign) and pass in CI                                  |
| NFR-5 | Standard JSON error payload for every error, including 401 and 403                           |
| NFR-6 | The stack starts in about 8 minutes on a 4 GB Docker limit (staged start)                    |
| NFR-7 | Swagger UI for each business service                                                         |

## 4. Permission Matrix

| Action                                        | ADMIN | HR  | IT  | EMPLOYEE |
| :-------------------------------------------- | :---: | :-: | :-: | :------: |
| Create `HR`/`IT`/`ADMIN` users, list users    |  Yes  | No  | No  |    No    |
| Create or update employees                    |  Yes  | Yes | No  |    No    |
| View employee list and details                |  Yes  | Yes | Yes | Own only |
| Update HR-owned tasks                         |  Yes  | Yes | No  |    No    |
| Update IT-owned tasks, assign asset to a task |  Yes  | No  | Yes |    No    |
| Add or update assets, return assets           |  Yes  | No  | Yes |    No    |
| View assets and history                       |  Yes  | Yes | Yes |    No    |
| View dashboards                               |  Yes  | Yes | Yes |    No    |

## 5. User Stories and Acceptance Criteria

| Story                                                 | Acceptance criteria                                                                                  |
| :---------------------------------------------------- | :--------------------------------------------------------------------------------------------------- |
| As HR, I create an employee so that onboarding starts | 201; checklist generated with the correct owners and due dates; duplicate email returns 409          |
| As IT, I add assets to the inventory                  | 201 with a generated asset tag; duplicate serial returns 409                                         |
| As IT, I assign a laptop to a new employee            | Task becomes `DONE` with the asset tag; the asset becomes `ASSIGNED`; assigning it again returns 409 |
| As IT, I cannot assign a monitor to a laptop task     | 409 with a clear message                                                                             |
| As HR, I cannot assign assets                         | 403                                                                                                  |
| As an employee, I see only my onboarding              | Another employee's id returns 404                                                                    |
| As HR, I see who is blocked                           | Overdue tasks are flagged on the dashboard and in the checklist                                      |
| As IT, I return an asset                              | Asset is `AVAILABLE`; the history row gets a return time                                             |

## 6. Out of Scope

AI features, notifications, file uploads, offboarding, payroll, SSO, multi-tenancy.
