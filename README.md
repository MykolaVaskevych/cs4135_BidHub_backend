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
| `auction-service` | 8083 | Listings, auctions, bids, watchlist | Stub |
| `order-service` | 8084 | Order lifecycle | Stub |
| `payment-service` | 8085 | Wallet, escrow, transactions | Stub |
| `notification-service` | 8086 | In-app notifications | Stub |

**Stub** = Maven module exists with Application class, H2 config, and health endpoint only. Ready for implementation.

## Prerequisites

- Java 21
- Docker (for PostgreSQL)

## Quick Start

### 1. Start PostgreSQL (if you don't wanna use H2)

```bash
docker compose up -d
```

This starts a Postgres 16 container (`bidhub-postgres`) on port 5432 with database `bidhub_accounts`.

### 2. Start infrastructure services (in order)

```bash
cd eureka-server  && ../mvnw spring-boot:run &
cd config-server  && ../mvnw spring-boot:run &
```

Wait until both are healthy, then start the gateway and business services:

```bash
cd api-gateway      && ../mvnw spring-boot:run &
cd account-service  && ../mvnw spring-boot:run &
```

Or start any stub service the same way (e.g. `cd auction-service && ../mvnw spring-boot:run`).

### 3. Verify

- Eureka dashboard: http://localhost:8761
- Gateway health: http://localhost:8080/actuator/health
- Account health: http://localhost:8081/actuator/health

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

## Pre-commit Hooks

Lefthook + Spotless for code formatting.

```bash
lefthook install
./mvnw spotless:apply
```
