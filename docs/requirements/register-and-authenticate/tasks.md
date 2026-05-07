# Implementation Plan: user-auth

## Overview

Implement end-to-end user authentication across the Spring Boot backend and Angular frontend. The backend exposes stateless JWT-based REST endpoints under `/api/auth/**`; the frontend provides login/register pages, an `AuthService`, a DI-based HTTP interceptor, and a route guard. The existing `trainer.users` table is the sole persistence target — no schema migrations are needed.

## Tasks

- [ ] 1. Add backend dependencies and JWT configuration
  - Add `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JJWT 0.12.x) and `jqwik` (1.9.3) to `pom.xml`
  - Add `app.jwt.secret` and `app.jwt.expiration-ms` (86400000) properties to `application.yml`
  - _Requirements: 2.5_

- [ ] 2. Implement the `User` entity and `UserRepository`
  - [ ] 2.1 Create `User` JPA entity in `com.trainer.auth`
    - Map to `trainer.users` table; implement `UserDetails`; fields: `id`, `username`, `email`, `password`, `enabled`, `createdAt`, `updatedAt`
    - _Requirements: 1.1, 1.8, 2.6_
  - [ ] 2.2 Create `UserRepository` in `com.trainer.auth`
    - Extend `JpaRepository<User, Long>`; add `findByUsername`, `existsByUsername`, `existsByEmail`
    - _Requirements: 1.2, 1.3, 2.1_

- [ ] 3. Implement `JwtUtil`
  - [ ] 3.1 Create `JwtUtil` bean in `com.trainer.auth`
    - Implement `generateToken(String username)`, `extractUsername(String token)`, `isTokenValid(String token, UserDetails userDetails)`
    - Read secret and expiry from `application.yml` via `@Value`
    - _Requirements: 2.5, 3.1, 3.3, 3.4_
  - [ ]* 3.2 Write property tests for `JwtUtil` (`JwtUtilPropertyTest`)
    - **Property 5: Login issues a correctly-formed JWT** — for any valid username string, `generateToken` produces a JWT whose `sub` equals the username and `exp` equals `iat + 86400`
    - **Validates: Requirements 2.5**
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid username arbitraries_

- [ ] 4. Implement `AuthService` bean and request/response DTOs
  - [ ] 4.1 Create DTOs: `RegisterRequest`, `LoginRequest`, `RegisterResponse`, `LoginResponse`, `ErrorResponse` in `com.trainer.auth`
    - Apply Bean Validation constraints as specified in the design
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 2.4_
  - [ ] 4.2 Create `AuthService` bean in `com.trainer.auth`
    - Implement `register(RegisterRequest)`: check uniqueness, BCrypt-hash password, persist, return `RegisterResponse`
    - Implement `login(LoginRequest)`: load user, verify BCrypt match, check `enabled`, issue JWT via `JwtUtil`
    - Implement `UserDetailsService` to load users by username for Spring Security
    - _Requirements: 1.1, 1.2, 1.3, 1.8, 2.1, 2.2, 2.3, 2.6_
  - [ ]* 4.3 Write property tests for `AuthService` bean (`AuthServicePropertyTest`)
    - **Property 1: Valid registration round-trip** — for any valid username/email/password, `register()` returns a response with the submitted username and the persisted user has `enabled = true`
    - **Validates: Requirements 1.1, 1.8**
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid field arbitraries; mock `UserRepository` and `PasswordEncoder`_

- [ ] 5. Implement `GlobalExceptionHandler`
  - Create `@RestControllerAdvice` in `com.trainer.auth` handling `MethodArgumentNotValidException` (→ 400), `DataIntegrityViolationException` (→ 409), `BadCredentialsException` / `DisabledException` (→ 401), and generic `Exception` (→ 500)
  - Ensure no stack traces, class names, or SQL details are exposed in responses
  - _Requirements: 1.2, 1.3, 1.4, 2.2, 2.3_

- [ ] 6. Implement `JwtAuthFilter` and update `SecurityConfig`
  - [ ] 6.1 Create `JwtAuthFilter` in `com.trainer.auth`
    - Extend `OncePerRequestFilter`; read `Authorization` header; delegate to `JwtUtil`; load `UserDetails`; set `SecurityContextHolder`
    - Catch all JWT exceptions and write a 401 response directly (no stack trace)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6_
  - [ ] 6.2 Update `SecurityConfig` in `com.trainer.config`
    - Inject `JwtAuthFilter`; add it before `UsernamePasswordAuthenticationFilter`
    - Expose `AuthenticationManager` bean; wire `UserDetailsService`
    - _Requirements: 3.1, 3.5_

- [ ] 7. Implement `AuthController`
  - Create `AuthController` in `com.trainer.auth` with `POST /api/auth/register` (→ 201) and `POST /api/auth/login` (→ 200)
  - Use `@Valid` on request bodies; delegate to `AuthService`
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4_

- [ ] 8. Write backend controller property tests (`AuthControllerPropertyTest`)
  - [ ]* 8.1 Write property test for invalid username rejection
    - **Property 2: Invalid username is rejected** — for any username that is too short, too long, or contains disallowed characters, `POST /api/auth/register` returns HTTP 400
    - **Validates: Requirements 1.4, 1.5**
    - _Uses jqwik `@Property(tries = 100)` with `MockMvc`; arbitraries for bad usernames_
  - [ ]* 8.2 Write property test for invalid password rejection
    - **Property 3: Invalid password is rejected** — for any password outside [8, 255] characters, `POST /api/auth/register` returns HTTP 400
    - **Validates: Requirements 1.4, 1.6**
  - [ ]* 8.3 Write property test for invalid email rejection
    - **Property 4: Invalid email is rejected** — for any malformed email string, `POST /api/auth/register` returns HTTP 400
    - **Validates: Requirements 1.4, 1.7**
  - [ ]* 8.4 Write property test for blank login fields rejection
    - **Property 6: Blank login fields are rejected** — for any blank/whitespace username or password, `POST /api/auth/login` returns HTTP 400
    - **Validates: Requirements 2.4**

- [ ] 9. Write backend integration tests
  - [ ]* 9.1 Write `JwtAuthFilterPropertyTest`
    - **Property 7: Valid token grants access to protected endpoints** — for any registered enabled user with a valid JWT, a request to a protected endpoint returns non-401
    - **Validates: Requirements 3.1**
    - _Uses jqwik + `MockMvc`; generate users and tokens via `JwtUtil`; mock `UserDetailsService`_
  - [ ]* 9.2 Write Spring Boot integration tests with Testcontainers
    - Full register → login → access-protected-endpoint cycle against a real PostgreSQL container
    - JWT filter scenarios: valid token, expired token, wrong-key token, missing header
    - Public endpoint accessibility (`/api/auth/**`, `/actuator/health`)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 10. Backend checkpoint — Ensure all tests pass
  - Ensure all backend tests pass, ask the user if questions arise.

- [ ] 11. Implement Angular `AuthService`
  - [ ] 11.1 Create `AuthService` in `src/app/core/auth/`
    - `BehaviorSubject<DecodedToken | null>` for `currentUser$`; derive `isAuthenticated$`
    - Implement `login()`, `register()`, `logout()`, `getToken()`, `initFromStorage()`
    - Decode JWT payload via base64 (middle segment); check `exp` against `Date.now()`
    - Wire `APP_INITIALIZER` to call `initFromStorage()` at startup
    - _Requirements: 4.1, 4.2, 4.3, 4.5_
  - [ ]* 11.2 Write property tests for `AuthService` (`auth.service.spec.ts`)
    - **Property 8: Successful login stores the token** — for any JWT string returned by a successful login response, the token is present in `localStorage` after `login()` resolves
    - **Validates: Requirements 4.1**
    - **Property 9: App initialisation restores authenticated state from a valid token** — for any non-expired well-formed JWT in `localStorage`, `initFromStorage()` sets `isAuthenticated$` to `true` and emits a `DecodedToken` with matching `sub`
    - **Validates: Requirements 4.2**
    - **Property 10: App initialisation clears state for absent or expired tokens** — for any expired JWT or absent token, `initFromStorage()` sets `isAuthenticated$` to `false` and emits `null`
    - **Validates: Requirements 4.3**
    - **Property 12: Logout clears all authentication state** — for any authenticated state, `logout()` removes the token from `localStorage` and `isAuthenticated$` emits `false`
    - **Validates: Requirements 4.5**
    - _Uses fast-check 3.22.0; install as devDependency_

- [ ] 12. Implement `AuthInterceptor`
  - [ ] 12.1 Create `AuthInterceptor` (`HttpInterceptorFn`) in `src/app/core/auth/`
    - Read token from `AuthService.getToken()`; clone request with `Authorization: Bearer <token>` if present
    - On HTTP 401 response, call `AuthService.logout()`
    - Register in `app.config.ts` via `withInterceptors([authInterceptor])`
    - _Requirements: 4.4, 4.6_
  - [ ]* 12.2 Write property tests for `AuthInterceptor` (`auth.interceptor.spec.ts`)
    - **Property 11: HTTP interceptor attaches Bearer token to every request** — for any token string in `localStorage`, every outgoing request processed by `AuthInterceptor` carries `Authorization: Bearer <token>`
    - **Validates: Requirements 4.4**
    - _Uses fast-check `fc.string()` for token values_

- [ ] 13. Implement `AuthGuard`
  - [ ] 13.1 Create `AuthGuard` (`CanActivateFn`) in `src/app/core/auth/`
    - Unauthenticated on protected route → redirect to `/auth/login?redirectTo=<url>`
    - Authenticated on `/auth/login` or `/auth/register` → redirect to `/`
    - Missing or expired token → clear token, treat as unauthenticated
    - _Requirements: 5.1, 5.2, 5.3_
  - [ ]* 13.2 Write property tests for `AuthGuard` (`auth.guard.spec.ts`)
    - **Property 13: Auth guard redirects unauthenticated users with original URL preserved** — for any protected route path, when no valid token is present, `AuthGuard` redirects to `/auth/login` with the original path as `redirectTo`
    - **Validates: Requirements 5.1**
    - _Uses fast-check `fc.webPath()` for route paths_

- [ ] 14. Implement `LoginPageComponent`
  - [ ] 14.1 Create `LoginPageComponent` in `src/app/features/auth/login/`
    - Standalone component; reactive form with `username` and `password` controls
    - Inline validation errors for blank fields (no API call); distinct 401 vs other-error messages
    - Disable submit and show spinner while request is in-flight; clear errors on resubmit
    - Navigate to `/` on success; provide link to register page
    - Redirect to `/` if already authenticated (via `AuthGuard`)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_
  - [ ]* 14.2 Write property tests for `LoginPageComponent` (`login-page.component.spec.ts`)
    - **Property 14: Login form blank-field validation prevents API calls** — for any combination of blank username and/or password, submitting the form shows inline errors and dispatches no HTTP request
    - **Validates: Requirements 6.2**
    - _Uses fast-check `fc.constantFrom('', ' ', '   ')` for blank inputs_

- [ ] 15. Implement `RegisterPageComponent`
  - [ ] 15.1 Create `RegisterPageComponent` in `src/app/features/auth/register/`
    - Standalone component; reactive form with `username`, `email`, `password`, `confirmPassword` controls
    - Cross-field validator for password match; inline errors for blank fields (no API call)
    - Map 409 response body to field-level errors; display 400 `message` from API
    - Disable submit and show spinner while request is in-flight; clear errors on resubmit
    - Navigate to `/auth/login` on success (HTTP 201); provide link to login page
    - Redirect to `/` if already authenticated (via `AuthGuard`)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_
  - [ ]* 15.2 Write property tests for `RegisterPageComponent` (`register-page.component.spec.ts`)
    - **Property 15: Registration form blank-field validation prevents API calls** — for any combination of blank required fields, submitting the form shows inline errors and dispatches no HTTP request
    - **Validates: Requirements 7.2**
    - **Property 16: Password mismatch validation prevents API calls** — for any two distinct non-empty strings in password and confirm-password, submitting the form shows a cross-field error and dispatches no HTTP request
    - **Validates: Requirements 7.3**
    - _Uses fast-check `fc.constantFrom('', ' ', '   ')` and `fc.tuple(fc.string(), fc.string()).filter(([a,b]) => a !== b)`_

- [ ] 16. Wire routes and app configuration
  - Update `app.routes.ts` with `/auth/login`, `/auth/register` (lazy-loaded), and a protected root route using `AuthGuard`
  - Update `app.config.ts` to register `AuthInterceptor` via `withInterceptors([authInterceptor])` and add `APP_INITIALIZER` for `AuthService.initFromStorage()`
  - _Requirements: 4.2, 5.1, 5.2_

- [ ] 17. Frontend checkpoint — Ensure all tests pass
  - Ensure all frontend tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Backend PBT uses jqwik 1.9.3 (add to `pom.xml` test scope); frontend PBT uses fast-check 3.22.0 (add to `package.json` devDependencies)
- Checkpoints ensure incremental validation at the backend/frontend boundary
- No Flyway migration is needed — `trainer.users` already exists

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1", "2.2", "4.1"] },
    { "id": 1, "tasks": ["3.1", "4.2"] },
    { "id": 2, "tasks": ["3.2", "4.3", "6.1"] },
    { "id": 3, "tasks": ["5", "6.2", "8.1", "8.2", "8.3", "8.4"] },
    { "id": 4, "tasks": ["7", "9.1", "9.2"] },
    { "id": 5, "tasks": ["11.1"] },
    { "id": 6, "tasks": ["11.2", "12.1"] },
    { "id": 7, "tasks": ["12.2", "13.1"] },
    { "id": 8, "tasks": ["13.2", "14.1"] },
    { "id": 9, "tasks": ["14.2", "15.1"] },
    { "id": 10, "tasks": ["15.2", "16"] }
  ]
}
```
