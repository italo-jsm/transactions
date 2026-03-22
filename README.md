# Transactions

## Repository Note

If you are reviewing this project in GitHub, it is worth opening the Actions tab and checking the workflow run for this branch. The pipeline is intentionally split into `test`, `build`, and `push` jobs so you can quickly verify how quality gates and image delivery are organized.

## Image Versioning and Rollback

Published container images keep multiple tags in GHCR:

- the short commit hash for immutable traceability
- the branch tag, such as `main`
- `latest` for the most recent image published from `main`

This policy keeps deployments easy to track and makes rollback straightforward. If a new release causes problems, it is possible to redeploy a previous image directly by its commit hash without guessing which code produced it.

## Running with Docker Compose

The current `docker compose` setup starts the API and PostgreSQL together:

```bash
docker compose up -d --build
```

Wait until both containers are healthy before running the API curls:

```bash
docker compose ps
```

The expected status is:

- `transactions-postgres` as `healthy`
- `transactions-app` as `healthy`

To stop the containers and remove the database volume:

```bash
docker compose down -v
```

Services exposed locally:

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

The database starts with these default credentials:

- database: `mydb`
- username: `myuser`
- password: `mypassword`
- port: `5432`

## Running the Application Locally

If you prefer to run only the database with Docker and start the API with Gradle:

```bash
docker compose up -d postgres
```

With the database running, start the API:

```bash
./gradlew bootRun
```

If you want to override the connection using environment variables:

```bash
DB_URL=jdbc:postgresql://localhost:5432/mydb \
DB_USERNAME=myuser \
DB_PASSWORD=mypassword \
./gradlew bootRun
```

## Testing Actuator

After starting the application, keep these health checks handy:

Check overall health:

```bash
curl http://localhost:8080/actuator/health
```

Check liveness:

```bash
curl http://localhost:8080/actuator/health/liveness
```

Check readiness:

```bash
curl http://localhost:8080/actuator/health/readiness
```

Check the info endpoint:

```bash
curl http://localhost:8080/actuator/info
```

## Testing Use Cases

Create a purchase transaction:

```bash
curl -i -X POST http://localhost:8080/purchase-transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Notebook",
    "transactionDate": "2026-03-20",
    "amount": 3500.00
  }'
```

The response should return `201 Created` and a `purchaseId`. Use that identifier to fetch the transaction converted to the target country:

```bash
curl -i "http://localhost:8080/purchase-transactions/<purchaseId>?country=Brazil"
```

Another lookup example, now for a different country:

```bash
curl -i "http://localhost:8080/purchase-transactions/<purchaseId>?country=Canada"
```

Some useful scenarios to validate API rules:

Invalid payload with a non-existent date:

```bash
curl -i -X POST http://localhost:8080/purchase-transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Item",
    "transactionDate": "2026-02-30",
    "amount": 10.00
  }'
```

Invalid payload with an amount below the minimum:

```bash
curl -i -X POST http://localhost:8080/purchase-transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Cafe",
    "transactionDate": "2026-03-20",
    "amount": 0.00
  }'
```

Invalid lookup without the `country` parameter:

```bash
curl -i "http://localhost:8080/purchase-transactions/<purchaseId>?country="
```

## Swagger

With the application running, open the Swagger UI at:

```bash
http://localhost:8080/swagger-ui/index.html
```
