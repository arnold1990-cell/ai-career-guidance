# MVP Delivery Order

## A. Final folder structure

```text
.
├── backend/
│   ├── pom.xml
│   ├── src/main/java/com/edurite/...
│   ├── src/main/resources/
│   └── src/test/
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
├── database/
│   ├── migrations/
│   └── seed/
├── docs/
│   ├── architecture.md
│   ├── modules.md
│   ├── data-model-summary.md
│   ├── mvp-delivery-order.md
│   └── adr/
├── docker/
│   ├── docker-compose.yml
│   ├── backend.Dockerfile
│   └── frontend.Dockerfile
├── infra/
│   └── README.md
├── .env.example
├── docker-compose.yml
└── README.md
```

## B. Backend package-by-feature structure

See: `docs/modules.md`.

## C. Entity relationship summary

See: `docs/data-model-summary.md`.

## D. MVP delivery order (phased)

### Phase 0 — Foundation (current baseline)

1. Repository scaffolding + modular package structure.
2. Spring profiles/config baselines.
3. Docker dependencies (PostgreSQL, Redis).
4. Flyway bootstrap migrations.
5. Shared API/error envelope baseline.

### Phase 1 — Core MVP

1. Auth module (register/login/refresh/logout/reset).
2. JWT security and RBAC guards.
3. Student and company profile basics.
4. Read/search APIs for careers, courses, institutions, bursaries.
5. Admin basic auth + user overview endpoint.

### Phase 2 — Applications & Operations

1. Upload module + S3 integration and metadata storage.
2. Student application submission/tracking.
3. Company bursary posting and management APIs.
4. Admin review workflows (verification + bursary approval).
5. Notification abstraction + local mock providers.
6. Audit logging of sensitive actions.

### Phase 3 — Recommendations & Commercial

1. Rule-based recommendations with rationale/score outputs.
2. Saved items and dashboard aggregation endpoints.
3. Subscription tiers + payment abstraction/stub.
4. Analytics aggregation foundation.

### Phase 4 — Hardening

1. Test expansion (unit, controller, repository, integration, security).
2. Performance tuning (indexes, query refinement, caching).
3. Security hardening (token policies, limits, headers, monitoring).
4. CI/CD workflows and deployment documentation.
