# 10. Deployment

## 1. Local run with Docker Compose

`docker/docker-compose.yml` starts PostgreSQL 16, the five Spring Boot services and the frontend (nginx). Compose project name `onboardhub`. Start in stages on a small machine: infrastructure (postgres, discovery-server), then the three business services, then the gateway and frontend; wait for `healthy` between stages.

| Service                        | Published port             | Notes                         |
| :----------------------------- | :------------------------- | :---------------------------- |
| frontend                       | 3001                       | public                        |
| api-gateway                    | 8090                       | public                        |
| discovery-server               | 127.0.0.1:8771             | Eureka dashboard              |
| auth, employee, asset services | 127.0.0.1:8091, 8092, 8093 | Swagger UI from the host only |
| postgres                       | not published              | internal network              |

Memory: about 4 GB Docker limit is enough with `JAVA_OPTS=-Xmx256m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError`. Only one project stack (OnboardHub or ResolveAI) should run at a time on a small laptop.

## 2. Environment Variables

| Variable                                                                    | Used by                                   | Purpose                                                    |
| :-------------------------------------------------------------------------- | :---------------------------------------- | :--------------------------------------------------------- |
| `POSTGRES_USER`, `POSTGRES_PASSWORD`                                        | postgres, auth, employee, asset           | Database credentials                                       |
| `JWT_SECRET` (32+ chars), `JWT_EXPIRATION_MS`                               | auth (issues), employee, asset (validate) | Shared signing key                                         |
| `CORS_ALLOWED_ORIGINS`                                                      | gateway                                   | Exact browser origin(s) of the frontend, no trailing slash |
| `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | auth                                      | First administrator (created only if no admin exists)      |
| `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS`                           | all Eureka clients                        | Service discovery                                          |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`                                      | auth, employee, asset                     | `jdbc:postgresql://postgres:5432/<db>`                     |
| `JAVA_OPTS`                                                                 | Java services                             | JVM options                                                |
| `VITE_API_BASE_URL`                                                         | frontend (build time)                     | URL the browser uses to reach the gateway                  |
| `DOCKER_USERNAME`, `IMAGE_TAG`, `API_DOMAIN`                                | production compose                        | Image namespace, tag, API hostname                         |

`docker/.env` is git-ignored; `docker/.env.example` contains placeholders only.

## 3. CI/CD (`.github/workflows/ci-cd.yml`)

Triggers: push and pull request to `main` and `develop`, plus manual runs; a newer run on the same branch cancels the older one.

1. `backend-tests`: matrix of the five services on JDK 17, `mvn -B test`.
2. `frontend`: Node 20, `npm ci`, `npm test`, `npm run build`.
3. `docker-build`: builds the six images (five services and frontend) with Buildx cache; pushes `<DOCKER_USERNAME>/onboardhub-<name>` with tags `latest` and the commit SHA only on a push to `main`.
4. `compose-smoke` (every pull request and push): builds and starts the full stack with Docker Compose, waits until every container is healthy and Eureka lists the four registered services, then runs `scripts/smoke-test.sh`: admin login, creation of HR and IT users, employee creation with the generated checklist, asset creation, role checks (403/401), asset assignment through the Feign call, duplicate assignment conflict (409), employee status change, ownership check (404) and asset return. Gateway answers 502/503/504 are retried because service discovery needs time to propagate.

Secrets: `DOCKER_USERNAME`, `DOCKER_PASSWORD` (Docker Hub access token, Read and Write).

## 4. Production: AWS EC2 + Vercel

- **Backend:** one EC2 Ubuntu server (24.04 or 26.04 LTS), instance type with 8 GB RAM that is eligible for the AWS Free plan (for example `m7i-flex.large`; check "Free tier eligible" in the launch wizard), 30 GB gp3, Elastic IP, security group with SSH from my IP only and HTTP/HTTPS from anywhere.
- **HTTPS:** Caddy in front of the gateway with a DuckDNS subdomain pointing to the Elastic IP; certificates are issued automatically. No CORS headers in Caddy (the gateway handles CORS).
- **Images:** pulled from Docker Hub by `scripts/deploy.sh` with a staged start; `IMAGE_TAG=<sha>` for rollbacks.
- **Frontend:** Vercel project with root directory `frontend`, framework Vite, `VITE_API_BASE_URL=https://<API_DOMAIN>`; `frontend/vercel.json` rewrites all routes to `index.html`.
- **CORS:** after the first Vercel deployment set `CORS_ALLOWED_ORIGINS` to the exact Vercel URL on the server and run `deploy.sh --no-pull`.

## 5. Operations Runbook

- **Start/stop:** start the instance in the EC2 console and wait about 5 minutes (containers restart automatically); stop it (never terminate) when not demoing. A stopped instance still costs a little (EBS volume and public IPv4); Free plan credits are consumed while it runs.
- **SSH:** add `ServerAliveInterval 30` to `~/.ssh/config`; update the SSH security group rule when the laptop IP changes.
- **Backups:** `docker compose exec postgres pg_dump -U <user> <db>` for each database.
- **Logs:** `docker compose -f docker/docker-compose.prod.yml logs --tail 100 <service>`.

## 6. Lessons Carried Over

Use `127.0.0.1` in container healthchecks (Alpine resolves `localhost` to IPv6); wait for service discovery before smoke tests; do not assume a single OS version in server scripts; send timestamps in UTC; never paste secrets twice or commit `.env`; AWS Free plan restricts instance types.

## 7. Verified Deployment

Filled in after deployment: live URLs, date, instance type and region, and the verified flows.
