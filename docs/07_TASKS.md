# 07. Task Breakdown

Each task = one Codex prompt (one module at a time). Estimates are Codex working time.

| ID    | Task                                                               | Priority | Est.   | Depends on   | Deliverables                                                                                             | Definition of Done                                            |
| :---- | :----------------------------------------------------------------- | :------- | :----- | :----------- | :------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------ |
| T-01  | Docs 01 to 10 reviewed and committed                               | P0       | 1 h    | none         | `docs/01..10`                                                                                            | Files committed on `develop`                                  |
| T-101 | `discovery-server` (8771)                                          | P0       | 15 min | T-01         | Eureka module, tests                                                                                     | `mvn clean test` green, jar builds                            |
| T-102 | `api-gateway` (8090)                                               | P0       | 20 min | T-101        | Routes, CORS, tests                                                                                      | Routes and CORS asserted by tests                             |
| T-103 | `auth-team-service` (8091)                                         | P0       | 45 min | T-101        | Register, login, JWT, `POST/GET /api/users`, `/me`, bootstrap admin, tests                               | Tests green; 403 for non-employee registration; Swagger works |
| T-201 | `asset-inventory-service` (8093)                                   | P0       | 45 min | T-103        | Assets CRUD, atomic assign and return, history, summary, tests                                           | Concurrency-safe assign test; role matrix tests green         |
| T-202 | `employee-onboarding-service` (8092)                               | P0       | 60 min | T-201        | Employees, checklist generation, task rules, assign-asset Feign flow with compensation, dashboard, tests | All business rules covered; Feign mocked; tests green         |
| T-301 | Frontend foundation and auth                                       | P0       | 40 min | T-103        | Vite, Tailwind, API layer, auth context, guards, shared components, login and register                   | `npm test` and build green                                    |
| T-302 | Frontend: dashboard, employees, detail page, assign asset modal    | P0       | 60 min | T-202, T-301 | Pages and components                                                                                     | Tests green; checklist and assign flow work against the API   |
| T-303 | Frontend: assets, team, my onboarding                              | P0       | 45 min | T-302        | Pages and components                                                                                     | Tests green                                                   |
| T-401 | Dockerfiles, Compose, `.env.example`, init SQL                     | P0       | 30 min | T-202, T-303 | `docker/`, Dockerfiles                                                                                   | Stack becomes healthy locally (staged start)                  |
| T-402 | GitHub Actions CI/CD                                               | P0       | 20 min | T-401        | `ci-cd.yml`                                                                                              | Green on pull requests and on `main` (smoke test included)    |
| T-501 | Postman collection, test report, README                            | P1       | 30 min | T-401        | `postman/`, `docs/09_TESTING.md`, `README.md`                                                            | Newman run green; real test counts recorded                   |
| T-502 | Production files (Compose prod, Caddy, EC2 scripts, Vercel config) | P1       | 30 min | T-402        | `docker-compose.prod.yml`, `Caddyfile`, scripts                                                          | Compose config validates                                      |
| T-503 | Deploy to AWS and Vercel, final docs and prompt log                | P1       | 2 h    | T-502        | Live URLs, screenshots, `docs/11_PROMPTS.md`                                                             | Full flow works on the live URLs                              |

## Prompt plan

| Prompt | Scope | Status |
| :--- | :--- | :--- |
| 1A | `discovery-server` and `api-gateway` | Done |
| 1B | `auth-team-service` | Done |
| 2 | `asset-inventory-service` | Done |
| 3A | Employee core | Done |
| 3B | Assign-asset flow | Done |
| 4 | Frontend foundation | Done |
| 5 | Frontend dashboard and employees | Done |
| 6 | Frontend assets, team, and my onboarding | Done |
| 7 | Docker | Done |
| 8 | CI/CD and smoke test | Done |
| 9 | Postman, test report, and README | In progress |
| 10 | Production files | Planned |
| 11 | Final docs after deployment | Planned |
| Manual | Deployment | Pending |
