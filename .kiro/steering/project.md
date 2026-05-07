# Project: AI Trainer

A full-stack personal AI trainer application with an Angular frontend and a Spring Boot backend.

## Structure

```
ai-trainer/
├── trainer-be/   # Spring Boot backend (Java 21)
└── trainer-fe/   # Angular 21 frontend (TypeScript)
```

## Backend (trainer-be)

- **Framework:** Spring Boot 3.5.4, Java 21
- **Database:** PostgreSQL, schema `trainer`
- **ORM:** Spring Data JPA + Hibernate (DDL mode: `validate` — Flyway owns the schema)
- **Migrations:** Flyway — add new scripts to `src/main/resources/db/migration/` following the `V{n}__{description}.sql` naming convention
- **Security:** Spring Security, stateless (no sessions), BCrypt password encoding
- **Validation:** Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Server port:** 8080
- **Base package:** `com.example.backend`

### Key conventions
- Public endpoints are under `/api/auth/**`; everything else requires authentication
- DB credentials come from env vars `DB_USERNAME` / `DB_PASSWORD` (defaults: `postgres`/`postgres`)
- Use `@EnableMethodSecurity` for method-level access control
- Logging: root=INFO, `com.example.backend`=DEBUG

### Build & run
```bash
cd trainer-be
./mvnw spring-boot:run          # dev
./mvnw test                     # tests
./mvnw package -DskipTests      # build jar
```

## Frontend (trainer-fe)

- **Framework:** Angular 21 (standalone components)
- **Language:** TypeScript 5.6
- **Styles:** SCSS
- **HTTP:** `provideHttpClient(withInterceptorsFromDi())` — use DI-based interceptors
- **Animations:** `provideAnimationsAsync()`
- **Testing:** Karma + Jasmine

### Key conventions
- All components should be standalone (`standalone: true`)
- Routes are defined in `src/app/app.routes.ts` using lazy-loaded feature modules where possible
- Place feature code in `src/app/features/<feature-name>/`
- Place shared services in `src/app/core/` and reusable UI in `src/app/shared/`

### Build & run
```bash
cd trainer-fe
npm start           # dev server (port 4200)
npm run build       # production build
npm test            # Karma tests
```

## Database Schema

Current tables (schema: `trainer`):

| Table   | Key columns |
|---------|-------------|
| `users` | `id`, `username`, `email`, `password`, `enabled`, `created_at`, `updated_at` |
