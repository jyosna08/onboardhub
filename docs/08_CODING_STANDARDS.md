# 08. Coding Standards

## Mandatory (backend)

- Java 17 target, Spring Boot 3.2.12, Spring Cloud 2023.0.6. **No Lombok**: records for DTOs and properties, explicit constructors, `LoggerFactory` logging.
- Layered architecture: `entity`, `enums`, `dto`, `repository`, `service` (interface + impl), `controller`, `security`, `config`, `exception`, `mapper`. Controllers are thin; entities never leave the service layer.
- Constructor injection only. Bean Validation (`@Valid`) on every request body. Enums instead of raw strings.
- Each service is a standalone Maven module with its own `pom.xml` and H2 in test scope.
- One `@RestControllerAdvice` per service returning the standard error payload; `AccessDeniedException` maps to 403 and the security entry point and access-denied handler write the same JSON for 401 and 403.
- Method security (`@PreAuthorize`) for role rules; JWT validated in a filter with the shared secret (fail fast if shorter than 32 bytes).
- Do not hold a database transaction open across a Feign call; use short transactions around persistence and compensate failures after remote calls.
- Concurrency-sensitive state changes (asset assignment) use an atomic conditional update, never read-then-write.
- Swagger annotations on controllers; meaningful INFO/WARN logs, never secrets, tokens, passwords or full personal data.
- Configuration through environment variables with local defaults (`${DB_URL:...}`); no hardcoded secrets; `TZ=UTC` in containers.
- Methods under about 30 lines; no duplicated code; no magic numbers.

## Mandatory (frontend)

- JavaScript, functional components, hooks; one component per file; Tailwind utilities only; no inline styles, no `console.log`, no `dangerouslySetInnerHTML`.
- No raw role or status strings in components; shared constants and display maps.
- `localStorage` only inside `utils/authStorage.js`; API calls only in `api/` modules; `AbortController` for GET requests that can be superseded.
- Accessible forms (labels, `aria-invalid`, `role="alert"`), keyboard-friendly modals and drawers, responsive layout.

## Infrastructure

- Healthchecks inside containers use `127.0.0.1` (not `localhost`).
- Staged start on small machines (infrastructure, services, gateway and frontend).
- `docker/.env` is never committed; only `.env.example` files with obvious placeholders.

## Git

Feature branches, meaningful commits (`feat:`, `fix:`, `docs:`, `test:`, `ci:`), pull request into `develop`, no direct pushes to `main`. Every API change updates `05_API_CONTRACT.md` first.

## AI-assisted workflow (ADF)

Each session: read the docs, print ASSUMPTIONS, modify only the allowed folders, no git commands, verification loop (tests green), short final report. Never invent test results.
