# 01. Project Context: OnboardHub

**Title:** Corporate Employee Onboarding & Asset Tracker Using Microservices

## 1. Problem Statement

Managing new employee onboarding and assigning company assets like laptops and monitors across HR and IT departments can lead to delays, allocation errors and tracking difficulties. HR and IT work from separate spreadsheets, nobody knows which onboarding steps are pending, and the same laptop can be promised to two people.

## 2. Solution

OnboardHub gives HR and IT one shared workflow:

1. **HR creates an employee** (name, email, department, job title, joining date, required equipment).
2. The system **generates an onboarding checklist** automatically: HR tasks (documents, orientation), IT tasks (email account, system access) and one equipment task per required asset type.
3. **IT keeps the asset inventory** (laptops, monitors, keyboards...) with a status for every asset.
4. **IT assigns an available asset to the employee's equipment task.** The asset service allows an assignment only if the asset is `AVAILABLE`, so the same asset can never be given to two people (allocation error prevention).
5. Every checklist step is tracked with an owner (HR or IT), a due date and a status. When all steps are `DONE` the employee becomes `ACTIVE`.
6. The **employee signs in** to see their own onboarding progress and the equipment assigned to them.
7. **Dashboards** show pending onboardings, overdue tasks and asset stock by type.

## 3. Roles

| Role       | Purpose                                                                           |
| :--------- | :-------------------------------------------------------------------------------- |
| `ADMIN`    | Creates HR, IT and admin accounts (Team page); can do everything HR and IT can do |
| `HR`       | Creates and manages employee records, completes HR-owned tasks                    |
| `IT`       | Manages the asset inventory, assigns and returns assets, completes IT-owned tasks |
| `EMPLOYEE` | Read-only view of their own onboarding and assigned equipment                     |

## 4. Scope

**In scope:** authentication with roles, employee records, auto-generated checklists, task tracking with owners and due dates, asset inventory, atomic asset assignment and return with history, dashboards, React UI, Docker, CI/CD, AWS + Vercel deployment.

**Out of scope:** AI features, email or SMS notifications, document uploads, offboarding and payroll, SSO, multi-company tenancy.

## 5. Technology Stack

| Layer    | Technology                                                                                                                                                                 |
| :------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend  | Java 17, Spring Boot 3.2.12, Spring Cloud 2023.0.6 (Eureka, Gateway, OpenFeign), Spring Security (stateless JWT, BCrypt), Springdoc OpenAPI, JUnit 5 + Mockito (no Lombok) |
| Database | PostgreSQL 16 (`auth_db`, `employee_db`, `asset_db`), H2 for tests                                                                                                         |
| Frontend | React 18, Vite 5, Tailwind CSS 3.4, React Router 6, Axios, Vitest                                                                                                          |
| DevOps   | Docker (multi-stage), Docker Compose, GitHub Actions, Docker Hub                                                                                                           |
| Hosting  | Frontend on Vercel, backend on one AWS EC2 server with Caddy HTTPS                                                                                                         |

## 6. Services and Ports

| Service                       | Port | Database      | Responsibility                                        |
| :---------------------------- | :--- | :------------ | :---------------------------------------------------- |
| `discovery-server`            | 8771 | none          | Eureka service registry                               |
| `api-gateway`                 | 8090 | none          | Single entry point, routing, CORS                     |
| `auth-team-service`           | 8091 | `auth_db`     | Login, registration, JWT, user management             |
| `employee-onboarding-service` | 8092 | `employee_db` | Employees, checklists, tasks, onboarding dashboard    |
| `asset-inventory-service`     | 8093 | `asset_db`    | Assets, assignment and return, history, stock summary |
| `frontend` (nginx)            | 3001 | none          | React app (local and compose)                         |

## 7. Folder Structure

See the README tree: `backend/<service>`, `frontend/`, `docker/`, `postman/`, `scripts/`, `.github/workflows/`, `docs/`.

## 8. Branching Strategy

`main` (production) <- `develop` <- `feature/<name>` branches. Every feature goes through a pull request into `develop`; `develop` is merged into `main` for releases. Commit style: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `ci:`.

## 9. Deployment Strategy

CI builds and tests everything; on a push to `main` it publishes Docker images to Docker Hub. The EC2 server pulls the images and runs them with Docker Compose behind Caddy (automatic HTTPS on a DuckDNS domain). The React app is deployed on Vercel. See `10_DEPLOYMENT.md`.

## 10. Conventions

Coding rules are in `08_CODING_STANDARDS.md`, tests in `09_TESTING.md`. Every API change updates `05_API_CONTRACT.md` before code.
