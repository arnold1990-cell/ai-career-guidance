# EduRite MVP Foundation

EduRite is an AI-powered student career guidance platform built as a **modular monolith**.

## 1) Project folder structure

- `backend/` Spring Boot 3, Java 21 API.
- `backend/src/main/java/com/edurite/*` Domain modules (auth, student, company, admin, recommendation, etc.).
- `backend/src/main/resources/db/migration/` Flyway SQL migration scripts.
- `frontend/` React + Vite + TypeScript starter shell.
- `docker-compose.yml` Local platform stack (Postgres, Redis, backend, frontend).
- `docker/docker-compose.yml` Alternate compose entry from docker folder.
- `.github/workflows/ci.yml` CI pipeline for backend build/verification.

## 2) Spring Boot project setup

The backend uses:

- Spring Web, Validation, Security, Data JPA
- PostgreSQL + Flyway
- Redis cache starter
- JWT (JJWT)
- OpenAPI (springdoc)
- Lombok + MapStruct

Base package: `com.edurite`.

Modules include:

`auth, user, student, company, career, course, institution, bursary, application, recommendation, subscription, payment, notification, admin, analytics, upload, verification, audit, common, config, security`

Each module contains `controller/service/repository/entity/dto/mapper` packages.

## 3) Database schema and Flyway migrations

`backend/src/main/resources/db/migration/V1__baseline_schema.sql` provisions PostgreSQL tables with UUID PKs for:

- users, roles, user_roles
- students, student_subjects, student_qualifications, student_experiences, student_documents
- companies, company_documents
- careers, institutions, courses
- bursaries, applications
- recommendations
- subscriptions, payments
- notifications
- audit_logs

## 4) Core entities

Implemented JPA entities for early MVP execution:

- `User`, `Role`
- `StudentProfile`
- `CompanyProfile`
- `Career`
- `Bursary`
- `ApplicationRecord`

All inherit common timestamps + UUID identity from `BaseEntity`.

## 5) Security and JWT authentication

Implemented and verified:

- `POST /api/v1/auth/register/student` (201 Created)
- `POST /api/v1/auth/register/company` (201 Created)
- `POST /api/v1/auth/login` (200 OK)
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

Security details:

- Stateless Spring Security filter chain (`SessionCreationPolicy.STATELESS`)
- BCrypt password hashing for every persisted user password
- JWT access + refresh token generation + validation
- Database-backed authentication via custom `UserDetailsService` (no in-memory users)
- Public endpoints: `/api/v1/auth/**`, docs/health endpoints
- All other `/api/**` endpoints require valid Bearer JWT

### Registration payloads

Student registration request:

```json
{
  "fullName": "Jane Student",
  "email": "jane.student@example.com",
  "password": "StrongPass@123"
}
```

Company registration request:

```json
{
  "companyName": "Acme Corp",
  "email": "hr@acme.com",
  "password": "StrongPass@123",
  "industry": "Technology"
}
```

Successful registration/login response shape:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>",
  "tokenType": "Bearer",
  "accessTokenExpiresIn": 3600,
  "user": {
    "id": "<uuid>",
    "email": "jane.student@example.com",
    "fullName": "Jane Student",
    "roles": ["ROLE_STUDENT"]
  }
}
```

### Admin account seeding

Admin registration endpoint is intentionally not exposed. Instead, startup seeding guarantees an admin user exists:

- default admin email: `admin@edurite.local`
- default admin password: `Admin@12345`
- default role: `ROLE_ADMIN`

Override with environment variables/properties:

- `edurite.auth.seed.admin.email`
- `edurite.auth.seed.admin.password`
- `edurite.auth.seed.admin.first-name`
- `edurite.auth.seed.admin.last-name`

Flyway migration `V2__seed_roles.sql` seeds `ROLE_STUDENT`, `ROLE_COMPANY`, and `ROLE_ADMIN`.

### Error responses

Validation/auth conflicts return structured JSON errors:

- `400` validation failure
- `409` duplicate email conflict
- `401` invalid credentials

## 6) Student module

- `GET /api/v1/students/me`
- `GET /api/v1/careers`
- `GET /api/v1/careers/{id}`
- `GET /api/v1/bursaries`
- `GET /api/v1/bursaries/{id}`
- `POST /api/v1/bursaries/{id}/applications`

Student dashboard recommendation feed:

- `GET /api/v1/recommendations/me`

## 7) Company module

- `GET /api/v1/companies/me`
- `PUT /api/v1/companies/me`
- `POST /api/v1/companies/me/documents`
- `POST /api/v1/companies/bursaries`
- `PUT /api/v1/companies/bursaries/{id}`
- `GET /api/v1/companies/bursaries`
- `GET /api/v1/companies/applicants`

## 8) Admin module

- `GET /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{id}/status`
- `GET /api/v1/admin/roles`
- `POST /api/v1/admin/roles`
- `PUT /api/v1/admin/roles/{id}`
- `GET /api/v1/admin/bursaries/pending`
- `PATCH /api/v1/admin/bursaries/{id}/review`
- `GET /api/v1/admin/analytics`

## 9) Recommendation engine

Rule-based recommendation service produces ranked recommendations for career, bursary, and course with:

- `score`
- `rationale`
- `modelVersion`

The service is structured for future integration with an external Python ML microservice.

## 10) Notification service

Notification service includes channel placeholders:

- EMAIL
- SMS
- PUSH

Events supported by workflow design:

- account created
- bursary approved/rejected
- application submitted
- deadline reminder
- subscription renewal

## 11) Docker setup

Start all services:

```sh
docker compose up -d
```

Services:

- postgres (`5432`)
- redis (`6379`)
- backend (`8080`)
- frontend (`5173`)

## 12) Major design decisions

1. **Modular monolith first**: keeps deployment simple while preserving clear module boundaries.
2. **UUID everywhere**: supports distributed integrations and safer client-side identifier exposure.
3. **Flyway baseline migration**: explicit schema evolution and reproducibility across environments.
4. **Stateless JWT auth**: scalable API sessions for web/mobile clients.
5. **Rule engine now, ML later**: immediate recommendation value with clean upgrade path.
6. **S3 abstraction via storage service**: provider-agnostic upload integration surface.

## Why startup failed with PostgreSQL 17.7

The backend failed during Flyway initialization (before JPA startup completed) because the Flyway version in the Spring Boot 3.3.2 stack did not support PostgreSQL `17.7`, producing:

- `Unsupported Database: PostgreSQL 17.7`

For local development, this repository pins Docker PostgreSQL to `16.x`, which is compatible with this stack.

## Local development requirements

- Java 21
- Maven 3.9+
- PostgreSQL **16.x** (Flyway in this stack is validated against PostgreSQL 16 for local/dev)
- Redis 7.x

## Backend local run (dev profile)

1. Start PostgreSQL 16 and Redis 7 locally (or with Docker Compose below).
2. Run the backend:

```sh
cd backend
mvn spring-boot:run
```

The default `dev` profile points to:

- PostgreSQL: `jdbc:postgresql://localhost:5432/edurite`
- Redis: `localhost:6379`

## Docker local stack

Use Docker Compose from project root:

```sh
docker compose up -d postgres redis
cd backend
mvn spring-boot:run
```

Or run the full stack:

```sh
docker compose up -d
```

The backend service in Compose sets `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/edurite` and `SPRING_DATA_REDIS_HOST=redis` so it can start correctly inside the Docker network.

Compose pins PostgreSQL to `postgres:16` for Flyway compatibility in local development.

## Flyway migrations

Flyway is enabled in the `dev` profile and migrations are loaded from:

- `classpath:db/migration`

To run migrations during startup, start the backend with the dev profile (default in this project).

## Run backend locally

```sh
cd backend
mvn spring-boot:run
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`


## Security configuration notes (JWT stateless auth)

Why the generated password disappeared:

- Spring Boot logs `Using generated security password` only when its fallback default security is active.
- EduRite now provides a full custom security stack (`SecurityFilterChain`, `DaoAuthenticationProvider`, `UserDetailsService`, JWT filter, `AuthenticationManager`), so Boot no longer auto-creates the default in-memory user.

How login works now:

1. Client calls `POST /api/v1/auth/login` with email/password.
2. Credentials are validated by Spring Security `AuthenticationManager` using the database-backed `CustomUserDetailsService`.
3. On success, EduRite returns Bearer access + refresh JWT tokens.
4. Client sends `Authorization: Bearer <access-token>` on protected endpoints.
5. `JwtAuthenticationFilter` validates the token and sets the authenticated principal in the security context.

Endpoint access policy:

- Public: `/api/v1/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/actuator/health`
- Protected: all `/api/**` endpoints (except the public auth routes above)

Required JWT properties/env placeholders:

- `security.jwt.secret` via `EDURITE_JWT_SECRET`
- `security.jwt.access-token-expiration` via `EDURITE_JWT_ACCESS_TOKEN_EXPIRATION`
- `security.jwt.refresh-token-expiration` via `EDURITE_JWT_REFRESH_TOKEN_EXPIRATION`

Never use real production secrets in source control; inject them via environment variables or secret managers.


## Run backend tests

```sh
cd backend
mvn test
```

The auth integration suite covers student/company/admin registration/login, duplicate email handling, JWT protection, and password hashing assertions.
