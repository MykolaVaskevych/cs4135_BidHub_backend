# BidHub Backend

[![CI](https://github.com/MykolaVaskevych/cs4135_week2_backend/actions/workflows/ci.yml/badge.svg)](https://github.com/MykolaVaskevych/cs4135_week2_backend/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

Multi-module Spring Boot microservice backend for BidHub.

## Architecture

| Module | Port | Description | Status |
|--------|------|-------------|--------|
| `eureka-server` | 8761 | Service discovery (Spring Cloud Netflix) | Done |
| `config-server` | 8888 | Centralised configuration (Spring Cloud Config) | Done |
| `api-gateway` | 8080 | Edge routing + JWT validation (Spring Cloud Gateway) | Done |
| `account-service` | 8081 | User registration, login, profile, addresses, admin moderation | Done |
| `catalog-service` | 8082 | Catalogue read projection (search, categories) | Stub |
| `auction-service` | 8083 | Listings, auctions, bids, watchlist | Done |
| `order-service` | 8084 | Order lifecycle | Stub |
| `payment-service` | 8085 | Wallet, escrow, transactions | Stub |
| `notification-service` | 8086 | In-app notifications | Stub |
| `admin-service` | 8087 | Category management, reports, moderation, dashboard | Done |

**Stub** = Maven module exists with Application class, H2 config, and health endpoint only. Ready for implementation.

## Prerequisites

- Java 21
- Docker (for PostgreSQL)

## Quick Start

### Full stack (recommended)

```bash
docker compose up --build
```

Starts all 10 services + Postgres. First build takes ~5 minutes (Maven multi-stage). Subsequent builds are cached.

| Endpoint | URL |
|----------|-----|
| API gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Swagger UI (auction) | http://localhost:8083/swagger-ui/index.html |
| Swagger UI (admin) | http://localhost:8087/swagger-ui/index.html |

### Individual services (faster for development)

```bash
# Start infra first
./mvnw -pl eureka-server spring-boot:run &
./mvnw -pl config-server spring-boot:run &

# Then any service (account-service uses Postgres via docker compose)
docker compose up -d postgres
./mvnw -pl account-service spring-boot:run
```

### Run tests

```bash
./mvnw test                          # all modules
./mvnw -pl auction-service,admin-service test   # just Mykola's services
```

## Database

| Service | Dev DB | Notes |
|---------|--------|-------|
| `account-service` | PostgreSQL via docker-compose | `bidhub_accounts` on localhost:5432 |
| Stub services | H2 in-memory | Will migrate to Postgres when implemented |

Credentials default to `bidhub` / `bidhub_dev_password`, overridable via `DB_USERNAME` / `DB_PASSWORD` env vars.

## API Tests

API tests are in the `bruno/` directory using [Bruno](https://www.usebruno.com/).

```bash
npm install -g @usebruno/cli
cd bruno && bru run --env ci
```

## Deployment (Railway)

All 10 services are deployed on Railway. See [`docs/DEPLOY.md`](docs/DEPLOY.md) for the full workflow including env vars and re-deploy instructions.

To redeploy a service after pushing to `main`:

```bash
railway up --service <service-name> --detach --path-as-root backend/
```

Run from the root of the monorepo (`cs4135_BidHub/`).

## Pre-commit Hooks

Lefthook + Spotless for code formatting.

```bash
lefthook install
./mvnw spotless:apply
```
