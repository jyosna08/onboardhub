# 11. Prompt Log

## How prompts were used

The work followed an architect-style plan based on the project requirements and contracts. Prompts were scoped to one service or feature at a time, began with an `ASSUMPTIONS` list, and included a verification loop. A human reviewed the changes and made commits manually. This log records prompt scope and branch names without reproducing prompt text.

## Prompt history

| Number | Scope | Branch | Outcome |
| :--- | :--- | :--- | :--- |
| 1A | Discovery server and API gateway | `feature/discovery-gateway` | Eureka registry and reactive gateway implemented and verified. |
| 1B | Auth and team service | `feature/auth-team-service` | Authentication, roles, user management, and bootstrap admin implemented and verified. |
| 2 | Asset inventory service | `feature/asset-service` | Inventory, atomic assignment, return, history, and summary implemented and verified. |
| 3A | Employee service core | `feature/employee-service-core` | Employee records and generated onboarding checklist implemented and verified. |
| 3B | Assign-asset flow | `feature/employee-assign-asset` | Cross-service equipment assignment flow implemented and verified. |
| 4 | Frontend foundation | `feature/frontend-foundation` | Frontend foundation and authentication implemented. |
| 5 | Frontend dashboard and employees | `feature/frontend-employees` | Dashboard, employee list, details, and onboarding views implemented. |
| 6 | Frontend assets, team, and my onboarding | `feature/frontend-assets-team` | Asset, team, and employee onboarding views implemented. |
| 7 | Docker Compose | `feature/docker-compose` | Container images and local Compose stack implemented. |
| 8 | CI/CD and smoke test | `feature/ci-cd` | Automated pipeline and API smoke script implemented. |
| 9 | Postman, test report, and README | `feature/postman-docs` | Postman collection, test report, and project README prepared. |

## Lessons learned

- Read and update the contracts and architecture documents before code.
- Keep each prompt focused on one module or feature.
- Depend on service interfaces so tests can use mocks across JDK versions.
- Use atomic updates for state that may be changed concurrently.
- Never hold a database transaction open across a remote service call.
- Use `127.0.0.1` for healthchecks inside containers.
- Smoke tests should retry while service discovery propagates.
- Never put secrets in git.

## Prompt template

```text
You are working in the repository at <path> on branch <branch>.

SESSION RULES:
1. Read the project context, requirements, architecture, API contract, coding standards, testing, and deployment documents relevant to this task.
2. Print a short ASSUMPTIONS list (no more than eight bullets), then continue without waiting.
3. Modify only the explicitly allowed files and folders.
4. Do not run git commands. Do not start Docker unless the task explicitly requires it and authorizes it.
5. Run the requested verification loop, record only observed results, and stop after the final report.

GOAL
<Describe one service or feature, its interface, behavior, constraints, and acceptance criteria.>

VERIFICATION
<Give exact commands and required outcomes.>

FINAL REPORT
<Keep it short; include changed files, observed test results, and deviations.>
```
