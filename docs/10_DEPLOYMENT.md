# 10. Deployment

## 1. Local run with Docker Compose

`docker/docker-compose.yml` starts PostgreSQL 16, the five Spring Boot services, and the frontend (nginx) under Compose project `onboardhub`. On a small machine, start infrastructure first, then business services, then the gateway and frontend; wait for health between stages.

| Service | Published port | Notes |
| :--- | :--- | :--- |
| frontend | 3001 | public |
| api-gateway | 8090 | public |
| discovery-server | host loopback, port 8771 | Eureka dashboard |
| auth, employee, asset services | host loopback, ports 8091, 8092, 8093 | Swagger UI from the host only |
| postgres | not published | internal network |

A 4 GB Docker memory limit is the local target; `JAVA_OPTS` can be set to `-Xmx256m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError`. On a small laptop, run only one project stack at a time.

## 2. Environment variables

| Variable | Used by | Purpose |
| :--- | :--- | :--- |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | postgres, auth, employee, asset | Database credentials. |
| `JWT_SECRET` (32+ characters), `JWT_EXPIRATION_MS` | auth, employee, asset | Shared JWT signing and validation settings. |
| `CORS_ALLOWED_ORIGINS` | gateway | Exact browser origin(s), with no trailing slash; production values may be comma-separated. |
| `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | auth | First administrator, created only when no admin exists. |
| `EUREKA_URL`, `EUREKA_INSTANCE_PREFER_IP_ADDRESS` | Eureka clients | Service discovery and registration. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | auth, employee, asset | Database connection settings. |
| `JAVA_OPTS` | Java services | JVM options. |
| `VITE_API_BASE_URL` | frontend build | Gateway URL used by the browser. |
| `DOCKER_USERNAME`, `IMAGE_TAG`, `API_DOMAIN` | production Compose | Docker Hub namespace, image tag, and API hostname. |

`docker/.env` is git-ignored. Local development uses `docker/.env.example`; production uses `docker/.env.prod.example`, copied to `docker/.env` on the server. The production template contains placeholders only.

## 3. CI/CD (`.github/workflows/ci-cd.yml`)

The workflow runs on pushes and pull requests targeting `main` or `develop`, and on manual dispatch. It grants `contents: read` and cancels older runs on the same ref.

1. `backend-tests`: matrix over the five backend modules using Temurin JDK 17 and `mvn -B test`; Surefire reports are uploaded on failure.
2. `frontend`: Node 20, `npm ci`, `npm test`, and `npm run build`.
3. `docker-build`: builds six images (the five backend services and frontend) with Buildx cache. Tags are `latest` and the commit SHA. It pushes only on a push to `main`, using repository secrets `DOCKER_USERNAME` and `DOCKER_PASSWORD`.
4. `compose-smoke`: runs after image builds on every workflow event. It creates a temporary `docker/.env`, starts the full local Compose stack, waits for every container healthcheck and the four Eureka registrations, runs `scripts/smoke-test.sh`, then runs the Postman collection with Newman. It prints Compose status, uploads Compose logs on failure, and removes the stack and volumes afterward.

The smoke script exercises gateway health, unauthenticated access, bootstrap admin login, employee registration/login, role restrictions, HR and IT account creation, asset creation and duplicate serial handling, employee checklist creation, assignment permissions and type matching, task/asset/history state, employee ownership, dashboard and asset summary, and asset return. Newman runs `postman/OnboardHub.postman_collection.json` using `postman/OnboardHub.local.postman_environment.json`.

## 4. Production: AWS EC2 + Vercel

### Architecture

```text
Vercel (React SPA)
  └─ HTTPS to https://<API_DOMAIN>
       └─ Caddy (EC2, ports 80 and 443)
            └─ api-gateway :8090
                 ├─ auth-team-service :8091 ─ auth_db
                 ├─ employee-onboarding-service :8092 ─ employee_db
                 ├─ asset-inventory-service :8093 ─ asset_db
                 └─ discovery-server :8771
PostgreSQL 16 and all services stay on the Compose network.
GitHub Actions publishes service images to Docker Hub on pushes to main; EC2 pulls them.
```

Only Caddy publishes host ports 80 and 443. The Compose project is named `onboardhub`; other application ports stay inside its network. The gateway handles CORS; Caddy does not add CORS headers.

### Prerequisites

- AWS account with a budget alert configured.
- DuckDNS subdomain whose DNS points to the EC2 Elastic IP.
- Vercel account and Docker Hub account.
- GitHub repository secrets `DOCKER_USERNAME` and `DOCKER_PASSWORD` configured so CI can publish images on a push to `main`.
- A successful CI image-publishing run before the server deploys those images.

### EC2 settings

Use Ubuntu 24.04 or 26.04. The `m7i-flex.large` has 8 GiB RAM and is currently listed by AWS as Free Plan eligible for eligible accounts; verify the current account’s offer and region in the launch wizard. The Free Plan has account eligibility and credit limits, so do not treat the label as a promise of zero cost. See [AWS EC2 Free Tier usage](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-free-tier-usage.html). Use a 30 GB gp3 volume, associate an Elastic IP, and configure the security group for SSH from your current IP only and HTTP/HTTPS from anywhere. Do not open service or database ports.

### Clone, bootstrap, and deploy

On the new Ubuntu server, connect as a sudo-capable user. Install Git if it is not present, clone the repository, and run the bootstrap once on that server:

```bash
sudo apt-get update
sudo apt-get install -y git
git clone <repository-url> onboardhub
cd onboardhub
sudo bash scripts/ec2-bootstrap.sh
```

The bootstrap supports Ubuntu 24.04 and 26.04. It installs Docker Engine, Compose, Git, curl, and jq; enables Docker; adds the login user to the `docker` group; creates a 2 GB swapfile if no swap is active; and sets `vm.swappiness=10`. Log out and back in for the group change to apply.

Create and edit the production environment file. Replace every `replace_with_` placeholder, use a JWT secret of at least 32 characters, set the DuckDNS hostname without a scheme in `API_DOMAIN`, and set `CORS_ALLOWED_ORIGINS` to the exact Vercel HTTPS origin(s), without trailing slashes. The template instructs that passwords use letters and digits only.

```bash
cp docker/.env.prod.example docker/.env
nano docker/.env
bash scripts/deploy.sh
```

The deployment script pulls images unless `--no-pull` is supplied, then starts PostgreSQL and discovery, the business services, the gateway, and Caddy in stages. It waits up to five minutes per service healthcheck. For rollback to a commit image tag, set `IMAGE_TAG` to that SHA:

```bash
IMAGE_TAG=<commit-sha> bash scripts/deploy.sh
```

After changing only environment configuration, use the existing local images without pulling:

```bash
bash scripts/deploy.sh --no-pull
```

### Vercel frontend

Import the repository into Vercel and configure:

- Root directory: `frontend`
- Framework preset: Vite
- Environment variable: `VITE_API_BASE_URL=https://<API_DOMAIN>`

Deploy the frontend and copy its final HTTPS origin. On the EC2 server, set `CORS_ALLOWED_ORIGINS` in `docker/.env` to that exact origin (or comma-separated exact origins) and recreate services using the already-pulled images:

```bash
bash scripts/deploy.sh --no-pull
```

`frontend/vercel.json` rewrites SPA routes to `/index.html`, so refreshing dashboard, employee, asset, team, and onboarding routes returns the React app.

### Verification checklist

- [ ] DuckDNS resolves to the EC2 Elastic IP.
- [ ] Only SSH from your IP, HTTP, and HTTPS are allowed by the security group.
- [ ] `docker compose -f docker/docker-compose.prod.yml --env-file docker/.env ps` shows all services healthy.
- [ ] The deployment script’s internal gateway health check succeeds.
- [ ] `https://<API_DOMAIN>/actuator/health` returns the gateway health response.
- [ ] Eureka contains the gateway and three business services.
- [ ] Vercel loads and refreshes `/dashboard`, `/employees`, `/assets`, `/admin/team`, and `/my-onboarding` without a 404.
- [ ] Browser API calls succeed with the deployed Vercel origin in `CORS_ALLOWED_ORIGINS`.

### Operations runbook

- **Start and stop:** start and stop the EC2 instance in the AWS console; never terminate it when preserving the deployment. Allow about five minutes after start for containers to become healthy. A stopped instance can still incur charges for its EBS volume and public IPv4/Elastic IP resources; check current AWS pricing and billing.
- **SSH keepalive:** add `ServerAliveInterval 30` to `~/.ssh/config`. Update the security group SSH source when your laptop’s public IP changes.
- **Switch between projects:** because the server is shared and has limited resources, stop one Compose project before starting the other. Stop OnboardHub with `docker compose -f docker/docker-compose.prod.yml --env-file docker/.env down`, then use the other project’s own Compose command. Reverse the order when switching back. This does not remove database volumes.
- **Back up databases:** run one command per database and store the resulting dump securely:

  ```bash
  docker compose -f docker/docker-compose.prod.yml --env-file docker/.env exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" auth_db' > auth_db.sql
  docker compose -f docker/docker-compose.prod.yml --env-file docker/.env exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" employee_db' > employee_db.sql
  docker compose -f docker/docker-compose.prod.yml --env-file docker/.env exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" asset_db' > asset_db.sql
  ```

- **Logs:** `docker compose -f docker/docker-compose.prod.yml --env-file docker/.env logs --tail 100 <service>`.
- **Status:** `docker compose -f docker/docker-compose.prod.yml --env-file docker/.env ps`.

### Troubleshooting

| Symptom | Checks and action |
| :--- | :--- |
| Caddy certificate is not issued | Confirm `API_DOMAIN` has no scheme, DuckDNS resolves to the Elastic IP, and security group/firewall permits inbound 80 and 443. Check `docker compose -f docker/docker-compose.prod.yml --env-file docker/.env logs --tail 100 caddy`. |
| Browser reports a CORS error | Set `CORS_ALLOWED_ORIGINS` to the exact Vercel origin, with scheme and no trailing slash; for multiple origins use a comma-separated list. Run `bash scripts/deploy.sh --no-pull`. Do not add CORS headers to Caddy. |
| Caddy returns 502 or the gateway returns 503 | Check `docker compose ... ps` and the relevant service logs. Wait about a minute for Eureka propagation after startup; the gateway can return 503 while discovery updates. Confirm `api-gateway` and the three business services are healthy and registered. |
| A service needs restarting | Run `docker compose -f docker/docker-compose.prod.yml --env-file docker/.env restart <service>`, then inspect status and logs. |
| Image not found | Confirm CI pushed the image on a `main` push, `DOCKER_USERNAME` matches its Docker Hub namespace, and `IMAGE_TAG` is `latest` or the exact published commit SHA. |

## 5. Lessons carried over

Use the loopback address in container healthchecks; wait for service discovery before smoke tests; keep timestamps in UTC; do not commit `.env`; and check AWS account, region, and billing eligibility before launch.

## 6. Verified deployment

Verified by the user on 2026-11-28:

- Frontend: [https://onboardhub-flax.vercel.app](https://onboardhub-flax.vercel.app), deployed on Vercel with the project root set to `frontend`.
- API: [https://onboardhub-jyosna.duckdns.org](https://onboardhub-jyosna.duckdns.org), served over HTTPS by Caddy in front of `api-gateway`. `GET /actuator/health` returned `{"status":"UP"}`.
- Backend host: a dedicated AWS EC2 server in `ap-southeast-2`, using `m7i-flex.large`, Ubuntu 24.04, the AWS Free plan, an Elastic IP, and a DuckDNS subdomain.
- Images: six public `onboardhub-*` Docker Hub images, built and pushed by CI on pushes to `main`.
- Network exposure: Postgres and application services are reachable only inside the Docker network; only Caddy publishes ports 80 and 443.
- Browser verification: bootstrap admin login; creating HR and IT users; employee self-registration; HR creating an employee with LAPTOP and MONITOR and receiving seven generated tasks; IT adding assets and assigning a laptop through the cross-service flow; task actions restricted by owner role; employee progress and issued equipment on My onboarding; and role-based navigation.
- CI: GitHub Actions was green on `develop` and `main`, including the Compose smoke end-to-end test and Newman step.
