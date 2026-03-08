# EduRite Architecture Summary (MVP Foundation)

EduRite is implemented as a **modular monolith** on Spring Boot 3 / Java 21, with strict package-by-feature boundaries so each domain can be extracted into independent services later.

## 1) System architecture overview

- **Backend**: Spring Boot REST API (`/api/v1`) with stateless JWT security and RBAC.
- **Frontend**: React + Vite + TypeScript web apps (public + role-specific portals).
- **Primary data store**: PostgreSQL with Flyway migrations.
- **Cache/rate-limit**: Redis (token/session adjunct, caching, request throttling).
- **File storage**: S3-compatible object storage with metadata in PostgreSQL.
- **Observability**: Spring Actuator, structured logging, request correlation IDs.

## 2) Modular-monolith boundaries

Business bounded contexts:

- `auth`, `user`, `student`, `company`, `institution`, `course`, `career`, `bursary`,
  `application`, `recommendation`, `notification`, `subscription`, `payment`,
  `admin`, `analytics`, `upload`, `verification`, `audit`.

Cross-cutting/support contexts:

- `common`, `config`, `security`.

Design rule: Each module owns its entities/repositories/services/controllers/DTOs. Inter-module access should happen through clear service interfaces and events, not direct entity coupling.

## 3) Security and access model

- JWT access + refresh token model.
- Password hashing with BCrypt.
- Method-level authorization with role guards.
- Roles: `STUDENT`, `COMPANY`, `ADMIN` (optionally `REVIEWER`, `SUPPORT`).
- Account statuses: `ACTIVE`, `INACTIVE`, `PENDING_VERIFICATION`, `SUSPENDED`.
- Login/reset endpoints protected by Redis-backed rate limiting.

## 4) Data and integration strategy

- PostgreSQL first for transactional integrity and MVP speed.
- Search starts with indexed SQL queries and pageable endpoints.
- Recommendation engine starts rule-based (`RecommendationProvider` abstraction), with an adapter boundary for a future Python ML service.
- Notifications implemented via channel abstractions (`EMAIL`, `SMS`, `PUSH` placeholder).

## 5) Scalability path

- Keep modules isolated and contracts explicit.
- Introduce async events/outbox for heavy workflows (notifications, analytics, recompute jobs).
- Extract high-churn/high-scale domains first (recommendation, notification, search) into services when needed.
