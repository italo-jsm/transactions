# Transactions

## Banco local com Docker

Suba o PostgreSQL para testes locais:

```bash
docker compose up -d
```

Para derrubar o container e remover o volume do banco:

```bash
docker compose down -v
```

O banco sobe com estas credenciais por padrao:

- database: `mydb`
- username: `myuser`
- password: `mypassword`
- port: `5432`

## Rodando a aplicacao

Com o banco ativo, suba a API:

```bash
./gradlew bootRun
```

Se quiser sobrescrever a conexao com variaveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/mydb \
DB_USERNAME=myuser \
DB_PASSWORD=mypassword \
./gradlew bootRun
```

## Testando o Actuator

Verifique o health geral:

```bash
curl http://localhost:8080/actuator/health
```

Verifique liveness:

```bash
curl http://localhost:8080/actuator/health/liveness
```

Verifique readiness:

```bash
curl http://localhost:8080/actuator/health/readiness
```

Verifique o endpoint de info:

```bash
curl http://localhost:8080/actuator/info
```
