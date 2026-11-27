# 09. Testing

## 1. Strategy

| Level       | Tools                                                  | Scope                                                                                       |
| :---------- | :----------------------------------------------------- | :------------------------------------------------------------------------------------------ |
| Unit        | JUnit 5 + Mockito                                      | Service rules (checklist generation, status derivation, task transitions, assignment rules) |
| Integration | `@SpringBootTest` + MockMvc + H2, Feign clients mocked | Controllers, security, validation, error payloads                                           |
| Frontend    | Vitest + Testing Library (API mocked)                  | Components, hooks, route guards, forms                                                      |
| API         | Postman collection + newman                            | Whole stack running in Docker                                                               |
| Manual      | Browser, Docker Compose stack                          | End-to-end flows                                                                            |

Backend tests need no Postgres, Eureka or network.

## 2. Test Cases

| ID    | Service                                               | Scenario                                                                          | Expected                                                                      | Status  |
| :---- | :---------------------------------------------------- | :-------------------------------------------------------------------------------- | :---------------------------------------------------------------------------- | :------ |
| TC-01 | auth-team-service                                     | Public register; register with role `HR`                                          | 201 `EMPLOYEE`; 403 and nothing created                                       | Pending |
| TC-02 | auth-team-service                                     | Admin creates `HR` and `IT`; non-admin tries                                      | 201; 403                                                                      | Pending |
| TC-03 | employee-onboarding-service                           | HR creates an employee with LAPTOP and MONITOR                                    | 201; checklist has 7 tasks with correct owners and due dates; code `EMP-xxxx` | Pending |
| TC-04 | employee-onboarding-service                           | Duplicate employee email                                                          | 409                                                                           | Pending |
| TC-05 | asset-inventory-service                               | IT adds an asset; duplicate serial                                                | 201 with tag; 409                                                             | Pending |
| TC-06 | employee-onboarding-service + asset-inventory-service | IT assigns an available laptop to the laptop task                                 | Task `DONE` with tag; asset `ASSIGNED`; history row                           | Pending |
| TC-07 | asset-inventory-service                               | Assign an already assigned asset (allocation error)                               | 409, nothing changes                                                          | Pending |
| TC-08 | asset-inventory-service                               | Two simultaneous assignments of one asset                                         | Exactly one succeeds                                                          | Pending |
| TC-09 | employee-onboarding-service                           | Assign a monitor to a laptop task                                                 | 409 type mismatch                                                             | Pending |
| TC-10 | employee-onboarding-service                           | Set an equipment task to `DONE` manually                                          | 409                                                                           | Pending |
| TC-11 | employee-onboarding-service                           | HR updates an IT task; IT updates an HR task                                      | 403                                                                           | Pending |
| TC-12 | employee-onboarding-service                           | Employee opens another employee's record                                          | 404                                                                           | Pending |
| TC-13 | employee-onboarding-service                           | Complete all tasks                                                                | Employee becomes `ACTIVE`; first task start gives `ONBOARDING`                | Pending |
| TC-14 | asset-inventory-service                               | Return an assigned asset; return an available asset                               | `AVAILABLE`, history closed; 409                                              | Pending |
| TC-15 | employee-onboarding-service                           | Asset service down during assignment                                              | 503, task unchanged                                                           | Pending |
| TC-16 | frontend                                              | Role guards, login redirects, checklist actions, assign modal, double-click guard | As specified in `06_UI_FLOW.md`                                               | Pending |

## 3. Deliverables Checklist

- [ ] All backend test suites green (record counts)
- [ ] Frontend tests and build green (record counts)
- [ ] Postman collection exported and run with newman
- [ ] Manual verification list completed on the Docker stack
- [ ] CI pipeline green on pull requests and `main`

## 4. Test Report (filled in at the end with REAL numbers)

| Module                      | Tests run | Failures | Errors | Skipped |
| :-------------------------- | :-------- | :------- | :----- | :------ |
| discovery-server            |           |          |        |         |
| api-gateway                 |           |          |        |         |
| auth-team-service           |           |          |        |         |
| asset-inventory-service     |           |          |        |         |
| employee-onboarding-service |           |          |        |         |
| frontend                    |           |          |        |         |

Sections added at the end: manually verified flows (with date), pending items, known limitations, bug list.
