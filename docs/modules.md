# Backend Package-by-Feature Structure

```text
backend/src/main/java/com/edurite
├── EduRiteApplication.java
├── config/
│   ├── properties/
│   ├── openapi/
│   ├── web/
│   └── persistence/
├── common/
│   ├── api/                  # ApiResponse, error envelope, pagination wrappers
│   ├── exception/            # global exception model and handlers
│   ├── validation/           # custom validators/annotations
│   ├── enums/
│   └── util/
├── security/
│   ├── jwt/                  # JwtService, token parsing/validation
│   ├── filter/               # JwtAuthenticationFilter
│   ├── handler/              # auth entrypoint + access denied handlers
│   ├── ratelimit/            # Redis-backed sensitive endpoint limiting
│   └── SecurityConfig.java
├── auth/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── domain/
│   └── repository/
├── user/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── domain/
│   └── repository/
├── student/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── mapper/
│   ├── domain/
│   └── repository/
├── company/
├── institution/
├── course/
├── career/
├── bursary/
├── application/
├── recommendation/
├── notification/
├── subscription/
├── payment/
├── admin/
├── analytics/
├── upload/
├── verification/
└── audit/
```

## Module implementation convention

For each business module, use this internal structure:

```text
<module>/
├── controller/   # REST controllers (no business logic)
├── service/      # application/domain orchestration
├── domain/       # JPA entities + value objects
├── repository/   # Spring Data repositories
├── dto/          # request/response contracts
├── mapper/       # MapStruct/manual mappers
└── event/        # domain/app events (optional by phase)
```

## Dependency direction

- `controller -> service -> repository/domain`
- DTOs are API-facing; entities are persistence-facing.
- `common` and `security` can be consumed by all modules.
- Business modules should avoid direct cross-entity writes; expose service contracts instead.
