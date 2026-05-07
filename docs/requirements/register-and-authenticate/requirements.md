# Requirements Document

## Introduction

This feature adds user authentication to the AI Trainer application. It covers three flows: registration (new users create an account), login (existing users authenticate and receive a JWT), and logout (users end their session on the client). The Angular frontend guards protected routes and redirects unauthenticated users to the login page. The Spring Boot backend exposes stateless REST endpoints under `/api/auth/**` and validates credentials using BCrypt-hashed passwords stored in the existing `trainer.users` table.

## Glossary

- **Auth_API**: The Spring Boot REST controller handling authentication endpoints under `/api/auth/**`.
- **Auth_Service**: The Angular service responsible for calling the Auth_API and managing the authentication state on the client.
- **Auth_Guard**: The Angular route guard that protects routes requiring authentication.
- **JWT**: JSON Web Token — a signed, stateless token issued by the Auth_API upon successful login and used to authenticate subsequent requests.
- **Token_Store**: The client-side storage mechanism (localStorage) used by the Auth_Service to persist the JWT between page reloads.
- **HTTP_Interceptor**: The Angular DI-based HTTP interceptor that attaches the JWT as a Bearer token to outgoing requests.
- **Login_Page**: The Angular standalone component presenting the login form.
- **Register_Page**: The Angular standalone component presenting the registration form.
- **User**: A person with a record in the `trainer.users` table.

---

## Requirements

### Requirement 1: User Registration

**User Story:** As a visitor, I want to create an account with a username, email, and password, so that I can access the AI Trainer application.

#### Acceptance Criteria

1. WHEN a POST request is sent to `/api/auth/register` with a valid username, email, and password, THE Auth_API SHALL create a new User record and return HTTP 201 with a JSON body containing the created user's `id` and `username`.
2. WHEN a POST request is sent to `/api/auth/register` with a username that already exists in the database, THE Auth_API SHALL return HTTP 409 with an error message indicating the username is taken.
3. WHEN a POST request is sent to `/api/auth/register` with an email that already exists in the database, THE Auth_API SHALL return HTTP 409 with an error message indicating the email is taken.
4. WHEN a POST request is sent to `/api/auth/register` with a missing or blank username, email, or password field, THE Auth_API SHALL return HTTP 400 with a validation error message identifying the invalid field.
5. WHEN a POST request is sent to `/api/auth/register` with a username shorter than 3 characters or longer than 100 characters, or containing characters other than letters, digits, underscores, or hyphens, THE Auth_API SHALL return HTTP 400 with a validation error message.
6. WHEN a POST request is sent to `/api/auth/register` with a password shorter than 8 characters or longer than 255 characters, THE Auth_API SHALL return HTTP 400 with a validation error message.
7. WHEN a POST request is sent to `/api/auth/register` with an email that does not contain exactly one `@` character with a non-empty local part and a domain containing at least one `.`, THE Auth_API SHALL return HTTP 400 with a validation error message.
8. THE Auth_API SHALL set the `enabled` field to `true` for all newly registered Users.

---

### Requirement 2: User Login

**User Story:** As a registered user, I want to log in with my username and password, so that I can access protected features of the AI Trainer application.

#### Acceptance Criteria

1. WHEN a POST request is sent to `/api/auth/login` with a valid username and matching password, THE Auth_API SHALL return HTTP 200 with a JSON body containing a signed JWT.
2. WHEN a POST request is sent to `/api/auth/login` with a valid username and an incorrect password, THE Auth_API SHALL return HTTP 401 with an error message.
3. WHEN a POST request is sent to `/api/auth/login` with a username that does not exist in the database, THE Auth_API SHALL return HTTP 401 with an error message.
4. WHEN a POST request is sent to `/api/auth/login` with a missing, blank, or whitespace-only username or password field, THE Auth_API SHALL return HTTP 400 with a validation error message.
5. THE Auth_API SHALL issue a JWT whose `sub` claim contains the authenticated user's username and whose `exp` claim is set to exactly 24 hours after the time of issuance.
6. WHILE a User's `enabled` field is `false`, THE Auth_API SHALL return HTTP 401 with an error message when a login request is received for that User.

---

### Requirement 3: JWT Authentication for Protected Endpoints

**User Story:** As a logged-in user, I want my requests to be authenticated automatically, so that I can access protected API resources without re-entering my credentials.

#### Acceptance Criteria

1. WHEN an HTTP request arrives at a protected endpoint with an `Authorization: Bearer <token>` header where the token has a valid signature, has not expired, and whose `sub` claim matches an enabled User, THE Auth_API SHALL allow the request to proceed.
2. WHEN an HTTP request arrives at a protected endpoint without an `Authorization` header, or with an `Authorization` header that is not parseable as `Bearer <token>`, THE Auth_API SHALL return HTTP 401.
3. WHEN an HTTP request arrives at a protected endpoint with an expired JWT, THE Auth_API SHALL return HTTP 401.
4. WHEN an HTTP request arrives at a protected endpoint with a JWT signed by an unknown key, THE Auth_API SHALL return HTTP 401.
5. THE Auth_API SHALL permit requests to `/api/auth/**` and `/actuator/health` without a JWT.
6. WHEN an HTTP request arrives at a protected endpoint with a JWT whose `sub` claim does not correspond to an enabled User in the database, THE Auth_API SHALL return HTTP 401.

---

### Requirement 4: Client-Side Token Management

**User Story:** As a logged-in user, I want my session to persist across page reloads, so that I do not have to log in again after refreshing the browser.

#### Acceptance Criteria

1. WHEN the Auth_Service receives a successful login response, THE Auth_Service SHALL store the JWT in the Token_Store.
2. WHEN the application initialises and a non-expired JWT is present in the Token_Store, THE Auth_Service SHALL restore the authenticated state so that protected routes are accessible and the user's identity is available.
3. WHEN the application initialises and no JWT is present in the Token_Store, or the stored JWT is expired, THE Auth_Service SHALL set the authentication state to unauthenticated.
4. WHILE a token is present in the Token_Store, THE HTTP_Interceptor SHALL attach the JWT as an `Authorization: Bearer <token>` header to every outgoing HTTP request.
5. WHEN the Auth_Service performs a logout, THE Auth_Service SHALL remove the JWT from the Token_Store and clear the authenticated state.
6. WHEN the HTTP_Interceptor receives an HTTP 401 response from the server, THE Auth_Service SHALL remove the JWT from the Token_Store, clear the authenticated state, and redirect the user to `/auth/login`.

---

### Requirement 5: Route Protection and Navigation

**User Story:** As an unauthenticated visitor, I want to be redirected to the login page when I try to access a protected route, so that the application guides me to authenticate.

#### Acceptance Criteria

1. WHEN an unauthenticated user navigates to any route not under `/auth`, THE Auth_Guard SHALL redirect the user to `/auth/login` and preserve the original URL as a `redirectTo` query parameter.
2. WHEN an authenticated user navigates to `/auth/login` or `/auth/register`, THE Auth_Guard SHALL redirect the user to the application home page (`/`).
3. WHEN the Auth_Guard evaluates a route and finds a missing or expired token in the Token_Store, THE Auth_Guard SHALL remove the token from the Token_Store and treat the user as unauthenticated.
4. WHEN a user logs out, THE Auth_Service SHALL navigate the user to `/auth/login`.

---

### Requirement 6: Login Page UI

**User Story:** As a visitor, I want a login form with clear fields and error feedback, so that I can authenticate without confusion.

#### Acceptance Criteria

1. THE Login_Page SHALL display a username field, a password field (with input masked), and a submit button.
2. WHEN the user submits the login form with a blank username or password, THE Login_Page SHALL display an inline validation error for each blank field without sending a request to the Auth_API.
3. WHEN the Auth_API returns HTTP 401, THE Login_Page SHALL display an error message indicating the credentials are invalid, and that error message SHALL be cleared when the user resubmits the form.
4. WHEN the Auth_API returns an error other than HTTP 401, THE Login_Page SHALL display an error message that does not reveal credential or server implementation details, and that error message SHALL be cleared when the user resubmits the form.
5. WHILE a login request is in progress, THE Login_Page SHALL disable the submit button and display a loading indicator to prevent duplicate submissions.
6. THE Login_Page SHALL provide a navigation link to the Register_Page.
7. WHEN the Auth_API returns HTTP 200, THE Login_Page SHALL navigate the user to the application home page (`/`).
8. WHEN an authenticated user navigates to the Login_Page, THE Login_Page SHALL redirect the user to the application home page (`/`).

---

### Requirement 7: Registration Page UI

**User Story:** As a visitor, I want a registration form with clear fields and error feedback, so that I can create an account without confusion.

#### Acceptance Criteria

1. THE Register_Page SHALL display a username field, an email field, a password field (with input masked), a confirm-password field (with input masked), and a submit button.
2. WHEN the user submits the registration form with a blank required field, THE Register_Page SHALL display an inline validation error for each blank field without sending a request to the Auth_API.
3. WHEN the password field and confirm-password field do not match, THE Register_Page SHALL display an inline validation error on the confirm-password field without sending a request to the Auth_API.
4. WHEN the Auth_API returns HTTP 409, THE Register_Page SHALL display an error message indicating which field (username or email) is already taken.
5. WHEN the Auth_API returns HTTP 400, THE Register_Page SHALL display the validation error message returned by the Auth_API.
6. WHILE a registration request is in progress, THE Register_Page SHALL disable the submit button and display a loading indicator to prevent duplicate submissions.
7. WHEN registration succeeds (HTTP 201), THE Register_Page SHALL navigate the user to `/auth/login`.
8. THE Register_Page SHALL provide a navigation link to the Login_Page.
9. WHEN an authenticated user navigates to the Register_Page, THE Register_Page SHALL redirect the user to the application home page (`/`).
