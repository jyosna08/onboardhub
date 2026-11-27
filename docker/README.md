# OnboardHub Docker Compose

## Prerequisites and first run

Install Docker Desktop with Compose v2 and allow at least 4 GB of memory. From the repository root, create the local environment file and replace every placeholder with suitable values:

```powershell
Copy-Item docker/.env.example docker/.env
```

Keep `docker/.env` local; never commit real credentials.

## Commands

Run these from the repository root:

```powershell
docker compose -f docker/docker-compose.yml --env-file docker/.env up --build -d
docker compose -f docker/docker-compose.yml --env-file docker/.env ps
docker compose -f docker/docker-compose.yml --env-file docker/.env logs -f <service>
docker compose -f docker/docker-compose.yml --env-file docker/.env stop
docker compose -f docker/docker-compose.yml --env-file docker/.env down
docker compose -f docker/docker-compose.yml --env-file docker/.env down -v
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --build <service>
```

`down -v` removes the PostgreSQL volume and resets all three databases. To rebuild one service, replace `<service>` with its Compose name, such as `api-gateway`.

## URLs

| Service | URL |
| :--- | :--- |
| Frontend | http://localhost:3001 |
| API gateway | http://localhost:8090 |
| Eureka dashboard | http://localhost:8771 |
| Auth/team Swagger UI | http://localhost:8091/swagger-ui.html |
| Employee onboarding Swagger UI | http://localhost:8092/swagger-ui.html |
| Asset inventory Swagger UI | http://localhost:8093/swagger-ui.html |

PostgreSQL is internal to the Compose network and is not published to the host. Discovery and service Swagger ports bind to loopback only.

## Staged start for low memory

The Compose project name gives containers names such as `onboardhub-postgres-1`. These PowerShell commands start the database and registry, wait for both healthchecks, then start the business services and wait for their healthchecks before the gateway and frontend:

```powershell
$compose = @('-f', 'docker/docker-compose.yml', '--env-file', 'docker/.env')
function Wait-OnboardHubHealthy([string[]]$Services) {
  $deadline = (Get-Date).AddMinutes(5)
  do {
    $states = @($Services | ForEach-Object {
      docker inspect --format='{{.State.Health.Status}}' "onboardhub-$_-1" 2>$null
    })
    if ($states.Count -eq $Services.Count -and @($states | Where-Object { $_ -ne 'healthy' }).Count -eq 0) { return }
    if ((Get-Date) -ge $deadline) { throw "Timed out waiting for healthy services: $($Services -join ', ')" }
    Start-Sleep -Seconds 5
  } while ($true)
}

docker compose @compose up -d postgres discovery-server
Wait-OnboardHubHealthy @('postgres', 'discovery-server')

docker compose @compose up -d auth-team-service asset-inventory-service employee-onboarding-service
Wait-OnboardHubHealthy @('auth-team-service', 'asset-inventory-service', 'employee-onboarding-service')

docker compose @compose up -d api-gateway frontend
```

## Troubleshooting

- If a service is unhealthy, inspect it with `docker compose -f docker/docker-compose.yml --env-file docker/.env logs <service>`.
- `VITE_API_BASE_URL` is a frontend build argument. Rebuild the frontend after changing it.
- The database initialization SQL runs only when PostgreSQL creates an empty data volume. Use `down -v` to recreate the databases.
- `CORS_ALLOWED_ORIGINS` must exactly match the browser origin, including scheme and port, with no trailing slash.
