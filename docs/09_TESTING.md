# 09. Testing

## 1. Strategy

| Level | Tools | Scope |
| :--- | :--- | :--- |
| Unit | JUnit 5 + Mockito | Service rules: checklist generation, status derivation, task transitions, and assignment rules. |
| Integration | `@SpringBootTest` + MockMvc + H2; Feign clients mocked | Controllers, security, validation, and error payloads. |
| Frontend | Vitest + Testing Library with mocked APIs | Components, hooks, route guards, and forms. |
| API | Postman collection + Newman | Full stack through the gateway. |
| Manual | Browser and Docker Compose | End-to-end UI walkthrough. |

Backend tests use H2 and mocked remote calls; they do not require Postgres, a running Eureka server, or network access.

## 2. Test cases and coverage map

| ID | Service | Scenario | Expected | Status |
| :--- | :--- | :--- | :--- | :--- |
| TC-01 | auth-team-service | Public registration; attempted `HR` registration | 201 `EMPLOYEE`; 403 and no privileged account | Covered by automated tests |
| TC-02 | auth-team-service | Admin creates HR and IT; non-admin tries | 201; 403 | Covered by automated tests |
| TC-03 | employee-onboarding-service | HR creates an employee with LAPTOP and MONITOR | 201; seven tasks with owners and due dates; `EMP-xxxx` code | Covered by automated tests |
| TC-04 | employee-onboarding-service | Duplicate employee email | 409 | Covered by automated tests |
| TC-05 | asset-inventory-service | IT adds an asset; duplicate serial | 201 with generated tag; 409 | Covered by automated tests |
| TC-06 | employee-onboarding-service + asset-inventory-service | IT assigns matching available laptop to its task | Task `DONE`; asset `ASSIGNED`; history entry | Covered by automated tests |
| TC-07 | asset-inventory-service | Assign an unavailable asset | 409; state remains unchanged | Covered by automated tests |
| TC-08 | asset-inventory-service | Two simultaneous assignments of one asset | Exactly one succeeds | Covered by automated tests |
| TC-09 | employee-onboarding-service | Assign monitor to laptop task | 409 type mismatch | Covered by automated tests |
| TC-10 | employee-onboarding-service | Manually complete an equipment task | 409 | Covered by automated tests |
| TC-11 | employee-onboarding-service | Wrong role updates a task | 403 | Covered by automated tests |
| TC-12 | employee-onboarding-service | Employee reads another employee’s record | 404 | Covered by automated tests |
| TC-13 | employee-onboarding-service | Task changes derive employee status | `ONBOARDING` while work is open; `ACTIVE` when all tasks are done | Covered by automated tests |
| TC-14 | asset-inventory-service | Return assigned asset; return it again | `AVAILABLE`, history closed; second return 409 | Covered by automated tests |
| TC-15 | employee-onboarding-service | Asset service unavailable during assignment | 503; task remains `TODO` | Covered by automated tests |
| TC-16 | frontend | Role guards, login redirects, checklist actions, assign modal, and duplicate-submit guard | As described in [06_UI_FLOW.md](06_UI_FLOW.md) | Pending manual UI check |

### Coverage references

`Postman` folders are in [postman/OnboardHub.postman_collection.json](../postman/OnboardHub.postman_collection.json). CI smoke references are grouped steps in the workflow’s `scripts/smoke-test.sh` stage; the exact 16 groups are listed below. A dash means no direct coverage in that layer.

| TC | Automated test classes and methods | Postman folder(s) | CI smoke step(s) |
| :--- | :--- | :--- | :--- |
| TC-01 | `AuthControllerIntegrationTest.publicRegistrationAcceptsEmployeeAndRejectsOtherRolesWithoutCreatingUsers`; `registrationReportsDuplicateAndInvalidBody` | `00 Setup`, `01 Auth and users` | 4–5 |
| TC-02 | `AuthControllerIntegrationTest.adminCreatesHrAndItUsersWhoCanLogIn`; `userCreationRejectsUnauthenticatedNonAdminDuplicateAndInvalidRequests`; `adminListsNewestFirstAndCanFilterByRole` | `00 Setup`, `01 Auth and users` | 6 |
| TC-03 | `EmployeeControllerIntegrationTest.hrCreatesEmployeeChecklistAndValidationAndRolesAreEnforced`; `ChecklistFactoryTest.createsSevenTasksWithExpectedOrderOwnersCategoriesAndDates` | `03 Employees` | 8 (smoke uses one required asset type; seven-task LAPTOP+MONITOR checklist is covered by automated tests and Postman) |
| TC-04 | `EmployeeServiceImplTest.duplicateEmailIsRejected`; `EmployeeControllerIntegrationTest.hrCreatesEmployeeChecklistAndValidationAndRolesAreEnforced` | `03 Employees` | 8 |
| TC-05 | `AssetControllerIntegrationTest.itCreatesAssetsAndGetsValidationAndDuplicateErrors`; `AssetServiceImplTest.generatesTagForEachAssetType`; `duplicateSerialIsRejectedCaseInsensitivelyAfterTrimming` | `02 Assets` | 7 |
| TC-06 | `AssignAssetServiceImplTest.assignsRemotelyThenPersists`; `EmployeeControllerIntegrationTest.assignmentEndpointCompletesEquipmentTaskAndEnforcesPermissionsAndErrors`; `AssetControllerIntegrationTest.assignmentConflictReturnAndHistoryWork` | `04 Onboarding tasks`, `05 Asset lifecycle` | 10, 12 |
| TC-07 | `AssetServiceImplTest.unavailableAssetIsConflict`; `AssetControllerIntegrationTest.assignmentConflictReturnAndHistoryWork` | `02 Assets` (the collection’s repeated task assignment is in `04 Onboarding tasks`; it does not call the asset assignment endpoint directly) | — (step 11 checks a completed task conflict, not direct asset re-assignment) |
| TC-08 | `AssignConcurrencyTest.onlyOneConcurrentAssignmentSucceeds` | `02 Assets` (no concurrent request in the collection) | — |
| TC-09 | `AssignAssetServiceImplTest.preservesConflictAndNotFoundErrors`; `EmployeeControllerIntegrationTest.assignmentEndpointCompletesEquipmentTaskAndEnforcesPermissionsAndErrors` | `04 Onboarding tasks` | — |
| TC-10 | `EmployeeServiceImplTest.equipmentCannotBeMarkedDoneManually`; `EmployeeControllerIntegrationTest.taskOwnershipStatusTransitionsEquipmentRuleAndDashboardWork` | `04 Onboarding tasks` | — |
| TC-11 | `EmployeeServiceImplTest.onlyTaskOwnerOrAdminCanUpdateTask`; `EmployeeControllerIntegrationTest.taskOwnershipStatusTransitionsEquipmentRuleAndDashboardWork` | `04 Onboarding tasks` | — |
| TC-12 | `EmployeeServiceImplTest.employeeCanReadOwnRecordButOtherEmployeeRecordIsHiddenAsNotFound`; `EmployeeControllerIntegrationTest.employeeOwnRecordMeAndPatchRulesWork` | `03 Employees` | 14 |
| TC-13 | `EmployeeStatusCalculatorTest.anyStartedOrCompletedTaskMeansOnboardingUntilAllAreDone`; `allTasksDoneMeansActive`; `EmployeeServiceImplTest.taskTransitionsRecomputeEmployeeStatus` | `04 Onboarding tasks` | 13 (smoke verifies `ONBOARDING`; the all-done `ACTIVE` transition is covered by automated tests) |
| TC-14 | `AssetServiceImplTest.returnReleasesAssetAndClosesHistory`; `returningNonAssignedAssetIsConflict`; `AssetControllerIntegrationTest.assignmentConflictReturnAndHistoryWork` | `05 Asset lifecycle` | 16 |
| TC-15 | `AssignAssetServiceImplTest.mapsFeignConnectionFailureToUnavailable`; `EmployeeControllerIntegrationTest.remoteAssignmentErrorsLeaveTaskTodo` | `04 Onboarding tasks` (the collection does not stop the asset service) | — |
| TC-16 | `ProtectedRoute.test.jsx` (`redirects a logged-out visitor to login`, `redirects a wrong role to its own home`); `AppRoutes.access.test.jsx` (`allows only ADMIN to open the team route`); `LoginForm.test.jsx` (`redirects to the signed-in role home after success`); `AssignAssetModal.test.jsx` (`lists the matching asset and closes and refreshes after assigning`); `useSubmitGuard.test.jsx` (`runs one submission for same-tick calls and clears the guard in finally`); `assetTeamOnboarding.integration.test.jsx` (`shows onboarding progress with a read-only checklist`) | — | — |

## 3. CI smoke flow

The GitHub Actions workflow runs the smoke script before Newman. The smoke script verifies these grouped flows, in order:

1. Gateway health returns 200 and `UP`.
2. Employee list without a token returns 401.
3. Bootstrap administrator login returns 200.
4. Public employee registration returns 201 and that employee can log in with 200.
5. Public registration requesting the HR role returns 403.
6. Admin creates HR and IT users; HR is forbidden from listing users; admin can list users.
7. IT creates a laptop with a `LAP-` tag and `AVAILABLE` status; HR creation is forbidden; duplicate serial returns 409.
8. HR creates an employee checklist with six tasks; duplicate email returns 409; IT creation is forbidden.
9. HR asset assignment is forbidden.
10. IT assigns a laptop to the equipment task; the task is `DONE` with the matching asset tag.
11. Reassigning the completed task returns 409.
12. IT reads the assigned asset and its single history entry.
13. HR reads the employee and sees `ONBOARDING` status.
14. The employee user receives 404 when reading another employee’s record.
15. HR dashboard and IT asset summary return 200; summary includes an assigned asset and six asset types.
16. IT returns the asset to `AVAILABLE`; returning it again returns 409.

## 4. Test report

All suites below were run in this session. Backend commands were `mvn -B test` in each module; `mvn -q clean` was run afterward in each module. Frontend dependencies were already present, so `npm ci` was not needed; `npm test` was run, then `frontend/node_modules` was removed and `npm cache clean --force` was run. Local JDK: 25.0.4 (Maven targets Java 17).

| Module | Tests run | Failures | Errors | Skipped |
| :--- | ---: | ---: | ---: | ---: |
| discovery-server | 1 | 0 | 0 | 0 |
| api-gateway | 3 | 0 | 0 | 0 |
| auth-team-service | 33 | 0 | 0 | 0 |
| asset-inventory-service | 28 | 0 | 0 | 0 |
| employee-onboarding-service | 39 | 0 | 0 | 0 |
| frontend | 73 | 0 | 0 | 0 |
| **Total** | **177** | **0** | **0** | **0** |

## 5. Verified in CI on 2026-11-28

The user verified that GitHub Actions was green on both the `develop` and `main` branches, including the `compose-smoke` job with its end-to-end smoke test and Newman step. The pipeline includes `backend-tests`, `frontend`, `docker-build`, and `compose-smoke`.

## 6. Deliverables checklist

- [x] Backend test suites run and actual counts recorded.
- [x] Frontend test suite run and actual count recorded.
- [x] Postman collection exported and JavaScript/JSON syntax validated. Newman was not run locally because no API calls are allowed for this task.
- [x] Manual browser verification of the full UI flow (verified 2026-11-28; see Section 8).
- [x] Production deployment verification (verified 2026-11-28; see Section 8).
- [ ] Load testing.

## 7. Pending

- Run load testing.

## 8. Verified deployment and browser walkthrough (2026-11-28)

The user verified the live deployment on AWS EC2 and Vercel. The API health endpoint returned `{"status":"UP"}` over HTTPS. The browser walkthrough verified bootstrap admin login; creating HR and IT users on the Team page; employee self-registration; HR creating an employee with LAPTOP and MONITOR, generating seven tasks; IT adding assets and assigning a laptop to the equipment task through the cross-service call; task status changes restricted by owner role; the employee seeing progress and issued equipment on My onboarding; and role-based navigation.

GitHub Actions was green on `develop` and `main`, including the Compose smoke job and Newman step. The backend deployment uses Docker Hub images, with Postgres and application services reachable only on the Docker network; Caddy publishes ports 80 and 443.

## 9. Known limitations

- Hibernate uses `ddl-auto=update` instead of database migrations.
- JWTs are stored in browser `localStorage`.
- There are no email notifications or file uploads.
- Only one stack runs at a time on the demo server.

## 10. Bug list

Fixed defects recorded for the project:

- Spring `AccessDeniedException` returned 500 instead of 403; it is now mapped to 403.
- A Postman script used a reserved sandbox variable; the collection design rule now prohibits reserved globals.
- Container healthchecks use the loopback address instead of `localhost`.
- Gateway routing returned 503 until discovery propagated; the smoke helper retries 502, 503, and 504 responses.
- Date-only fields shifted with the time zone; date-only values now remain local dates without conversion shifts.
