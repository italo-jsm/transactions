# Transactions

## Running with Docker Compose

The current `docker compose` setup starts the API and PostgreSQL together:

```bash
docker compose up -d --build
```

To check the container status:

```bash
docker compose ps
```

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
