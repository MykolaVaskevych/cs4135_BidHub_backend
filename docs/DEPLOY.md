# BidHub Deployment Guide

## Local Development (Docker Compose)

Run the full stack locally with:

```bash
cd backend
docker compose up --build
```

Services start in the correct dependency order. First boot takes ~3-5 minutes to build all images.

| Service             | URL                          |
|---------------------|------------------------------|
| API Gateway         | http://localhost:8080        |
| Eureka Dashboard    | http://localhost:8761        |
| Config Server       | http://localhost:8888        |
| Account Service     | http://localhost:8081        |
| Catalog Service     | http://localhost:8082        |
| Auction Service     | http://localhost:8083        |
| Order Service       | http://localhost:8084        |
| Payment Service     | http://localhost:8085        |
| Notification Service| http://localhost:8086        |
| Admin Service       | http://localhost:8087        |

### Useful commands

```bash
# Bring up only infra + one service
docker compose up postgres eureka-server config-server auction-service

# Rebuild a single service after code change
docker compose up --build auction-service

# Tear down (keeps postgres volume)
docker compose down

# Tear down and delete data
docker compose down -v
```

---

## Railway Deployment

**Railway project**: https://railway.com/project/b4e59d4d-ca8e-44a0-a7ec-d0c8f7a08a87

### Re-deploying after a code change

```bash
# From repo root (cs4135_BidHub):

# 1. Fast-forward backend/main to pick up development work
cd backend
git checkout main && git merge --ff-only origin/development && git push origin main && git checkout development

# 2. Update root/main submodule pointer
cd ..
git checkout main
git submodule update --remote --merge
BACKEND_SHA=$(git -C backend rev-parse HEAD)
git add backend
git commit -m "pin backend@${BACKEND_SHA:0:7}"
git push origin main
git checkout development

# 3. Redeploy all services (Railway reads the current source)
for svc in eureka-server config-server account-service catalog-service auction-service \
           order-service payment-service notification-service admin-service api-gateway; do
  railway up --service "$svc" --detach --path-as-root backend/
done
```

> **Why this works**: pushing to `backend/main` and `root/main` keeps them stable.
> Development work on `backend/development` never affects production until you explicitly fast-forward.

### One-time setup (project owner — Mykola)

1. Install Railway CLI: `npm install -g @railway/cli`
2. Login: `railway login`
3. Link the project: `railway link` (select the **bidhub** project)

### Add a new service to Railway

Each service in `backend/` has its own `railway.json`. To add a service to the Railway project:

```bash
cd backend
railway service create <service-name>   # e.g. eureka-server
railway up --service <service-name>
```

Railway reads `<service-name>/railway.json` automatically.

### Recommended service creation order

Create services in this order so env vars can reference each other:

1. **postgres** — use Railway's managed PostgreSQL template, NOT a Dockerfile
2. **eureka-server**
3. **config-server**
4. **account-service**
5. **catalog-service**
6. **auction-service**
7. **order-service**
8. **payment-service**
9. **notification-service**
10. **admin-service**
11. **api-gateway** (last — needs all services registered in Eureka)

### Required environment variables per service

Railway uses internal DNS: `<service-name>.railway.internal` for private networking.

#### eureka-server
_(no extra vars needed)_

#### config-server
```
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.railway.internal:8761/eureka/
```

#### account-service
```
SPRING_PROFILES_ACTIVE=dev
CONFIG_SERVER_URL=http://config-server.railway.internal:8888
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.railway.internal:8761/eureka/
DB_URL=${{Postgres.DATABASE_URL}}   # or set manually: jdbc:postgresql://<host>:<port>/bidhub_accounts
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

> **Why `CONFIG_SERVER_URL` not `SPRING_CONFIG_IMPORT`**: Spring Boot resolves `spring.config.import` from the config file before env var overrides apply. The `${CONFIG_SERVER_URL:http://localhost:8888}` placeholder in `application.yml/properties` is resolved at file-read time using env vars, which makes the override work reliably.

#### catalog-service / order-service / payment-service / notification-service
```
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.railway.internal:8761/eureka/
```

#### auction-service
```
SPRING_PROFILES_ACTIVE=dev
CONFIG_SERVER_URL=http://config-server.railway.internal:8888
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.railway.internal:8761/eureka/
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/bidhub_auction
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

#### admin-service
```
SPRING_PROFILES_ACTIVE=dev
CONFIG_SERVER_URL=http://config-server.railway.internal:8888
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.railway.internal:8761/eureka/
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/bidhub_admin
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

#### api-gateway
```
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.railway.internal:8761/eureka/
JWT_SECRET=<generate a strong random secret>
CORS_ORIGINS=https://your-frontend.railway.app
```

### Creating additional databases in Railway Postgres

Railway's managed Postgres only creates one database. Connect to it and run:

```sql
CREATE DATABASE bidhub_accounts;
CREATE DATABASE bidhub_auction;
CREATE DATABASE bidhub_admin;
```

Or use the Railway shell:
```bash
railway connect postgres
# then run the CREATE DATABASE statements
```

### Build context note

All Dockerfiles use `backend/` as the build context (they build the full Maven reactor). In Railway, set **Root Directory** to `backend/` in the service settings, then Railway will use the correct `railway.json` and Dockerfile path.

---

## CI/CD (GitHub Actions)

The pipeline at `.github/workflows/ci.yml` runs on every push/PR to `development` and `main`:

1. **tests** job — starts PostgreSQL, runs `./mvnw test` (all modules), starts API Gateway, runs Bruno API tests
2. **docker-build** job (runs after tests) — builds all 10 Docker images in parallel using GitHub Actions cache to verify Dockerfiles are healthy

Docker images are **not pushed** in CI; Railway pulls from the Git repo and builds on its own.
