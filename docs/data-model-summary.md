# Entity Relationship Summary (MVP)

## Core identity and access

- `users` (1) --- (N) `user_roles` (N) --- (1) `roles`
- `users` has account metadata (`account_type`, `status`, `last_login`).
- One user can be either student-facing or company-facing identity (by account type + role assignment).

## Student domain

- `students` (1) --- (N) `student_subjects`
- `students` (1) --- (N) `student_qualifications`
- `students` (1) --- (N) `student_experiences`
- `students` (1) --- (N) `student_documents`
- `students` (1) --- (N) `subscriptions`
- `students` (1) --- (N) `applications`
- `students` (1) --- (N) `recommendations`

## Learning and opportunities domain

- `institutions` (1) --- (N) `courses`
- `companies` (1) --- (N) `company_documents`
- `companies` (1) --- (N) `bursaries`
- `applications` references either:
  - `bursary_id` (nullable), or
  - `course_id` (nullable),
  with service-level validation enforcing at least one target.

## Platform support domain

- `subscriptions` (1) --- (N) `payments`
- `users` (1) --- (N) `notifications`
- `users` (1) --- (N) `audit_logs` via `actor_user_id`

## Recommendation model

`recommendations` stores polymorphic recommendations:

- `item_type` (`CAREER`, `COURSE`, `BURSARY`)
- `item_id` (UUID reference to target table)
- `score`, `rationale`, `model_version`, `created_at`

This supports phase-1 rule-based ranking while preserving compatibility with future ML-generated outputs.

## Key integrity and indexing notes

- Unique constraints: `users.email`, `companies.registration_number`.
- Search/perf indexes: `careers.title`, `courses.title`, `institutions.name`, `bursaries.application_end_date`, `applications.student_id`.
- Use enums/check constraints for statuses and tier fields.
- Use UUID PKs + FK constraints across all ownership relations.
