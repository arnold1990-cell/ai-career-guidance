# EduRite Frontend

React + TypeScript + Vite frontend for the EduRite AI-powered Student Career Guidance Platform.

## Stack
- React, Vite, TypeScript
- Tailwind CSS
- React Router
- React Hook Form + Zod
- TanStack Query
- Axios
- Lucide React icons

## Setup
```bash
npm install
npm run dev
```

Default API base URL is `/api/v1`, which uses the Vite dev proxy (`/api` -> `http://localhost:8080`). Override with `VITE_API_BASE_URL` for other environments (for example, `https://api.example.com/api/v1`).

## Architecture Highlights
- `src/app`: root app composition and providers
- `src/routes`: route guards (`RequireAuth`, `RequireRole`)
- `src/components`: shared UI, layout, feedback, table, and card components
- `src/pages`: public + role-based portal pages
- `src/features/auth`: auth context/store and JWT persistence
- `src/services`: typed API service modules mapped to Spring Boot endpoints
- `src/types`: shared models for API/domain contracts

## Role-based portals
- `STUDENT`: recommendations, profile, applications, subscription
- `COMPANY`: bursary management, applicants, talent search
- `ADMIN`: users, roles, moderation, analytics, audit logs

## Notes
- Axios interceptors include access token injection and refresh flow structure (`/auth/refresh`).
- Forms use React Hook Form and Zod validation.
- Layout is fully responsive with mobile sidebar toggle.
