# EduRite

Phase 0 foundation for the EduRite platform.

## Project Structure

- `backend/` Spring Boot modular monolith scaffold.
- `database/` Flyway SQL migration placeholders.
- `docker/` Docker Compose for local dependencies.
- `docs/` Architecture and module documentation.
- `frontend/` Placeholder for future frontend implementation.
- `infra/` Placeholder for infrastructure-as-code.

## Run Backend (local)

1. Ensure Java 21 and Maven are installed.
2. Start database dependencies first (see Docker instructions below).
3. Start backend:

```sh
cd backend
mvn spring-boot:run
```

Backend defaults to port `8080`.

## Run Database Migrations

Flyway is configured in Spring profiles and will run at backend startup.

Manual review of migration files:

- `database/migrations/V1__init_users.sql`
- `database/migrations/V2__init_roles.sql`

## Run with Docker Compose

> Note: `docker/docker-compose.yml` is the canonical local stack definition for EduRite.

```sh
cd docker
docker compose up -d
```

This starts:

- PostgreSQL on `5432`
- Redis on `6379`
- Backend placeholder container on `8080` (TODO runtime wiring)
