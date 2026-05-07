# Skill: Backend Developer

Architectural guidelines for the `trainer-be` Spring Boot backend.

---

## 1. Architectural Pattern — Boundary-Control-Entity (BCE)

All backend code must follow the BCE pattern. Every feature/domain is organized into three distinct layers:

### Boundary
- REST controllers (`@RestController`)
- Request/Response DTOs — use **Java 21 Records** for immutable data carriers
- Exception handlers (`@RestControllerAdvice`)
- External interface adapters (e.g., outbound HTTP clients, messaging producers)

```
com.example.backend.<domain>.boundary/
  ├── <Domain>Controller.java
  ├── <Domain>Request.java      // record
  └── <Domain>Response.java     // record
```

### Control
- Business logic and service orchestration (`@Service`)
- No persistence calls directly — delegates to Entity layer via repositories
- Publishes and listens to Application Events for cross-boundary communication
- Must remain free of HTTP/web concerns

```
com.example.backend.<domain>.control/
  ├── <Domain>Service.java
  └── <Domain>EventHandler.java  // @EventListener methods
```

### Entity
- JPA entities (`@Entity`) and Spring Data repositories (`@Repository`)
- Domain value objects — prefer **Java 21 Records** for value types
- No business logic; pure data representation and persistence

```
com.example.backend.<domain>.entity/
  ├── <Domain>.java              // @Entity
  ├── <Domain>Repository.java    // extends JpaRepository
  └── <Domain>Id.java            // record (if composite key or value object)
```

---

## 2. Decoupling & Event-Driven Design

### Self-Contained Boundaries
- **No direct Spring bean injection across domain boundaries.** A service in `user.control` must never inject a bean from `workout.control` or any other domain.
- Each domain boundary owns its own data. Cross-domain reads must go through published events or a dedicated query/anti-corruption layer.

### Inter-Boundary Communication via Application Events
- Use Spring's `ApplicationEventPublisher` to publish events from the Control layer.
- Use `@EventListener` (or `@TransactionalEventListener` when the listener must run after commit) to consume events in other boundaries.
- Event classes live in a shared `events` package and are **Java 21 Records**:

```
com.example.backend.events/
  └── <SomethingHappened>Event.java   // record
```

Example:
```java
// Publishing (inside a Control service)
publisher.publishEvent(new UserRegisteredEvent(user.id(), user.email()));

// Consuming (in another boundary's EventHandler)
@EventListener
public void onUserRegistered(UserRegisteredEvent event) { ... }
```

---

## 3. Build System Constraints

> **⚠️ STRICTLY FORBIDDEN: Do not modify `pom.xml` without explicit prior approval.**

- Never add, remove, or change a dependency, plugin, or property in `pom.xml` on your own initiative.
- If a feature requires a new dependency, **stop and ask the user for permission first**, explaining what dependency is needed and why.
- This rule has no exceptions, even for test-scoped or optional dependencies.

---

## 4. Technology Standards

| Concern | Standard |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x best practices |
| Immutable DTOs / Value Objects | Java 21 **Records** |
| Conditional logic on types | **Pattern Matching** (`instanceof`, `switch`) |
| Closed type hierarchies | **Sealed Classes / Interfaces** |
| Password encoding | `BCryptPasswordEncoder` |
| Schema management | Flyway only — Hibernate DDL is `validate` |
| Session strategy | Stateless — no `HttpSession` |

### Java 21 Feature Usage Guidelines
- **Records** — DTOs, events, value objects, response/request types
- **Pattern matching for switch** — replace chains of `instanceof` checks in Control logic
- **Sealed classes** — model domain result types (e.g., `sealed interface AuthResult permits Success, Failure`)
- **Text blocks** — multi-line SQL or JSON strings in tests

---

## 5. Package Layout Summary

```
com.example.backend/
├── <domain>/
│   ├── boundary/      # Controllers, DTOs (records), external adapters
│   ├── control/       # Services, event handlers
│   └── entity/        # JPA entities, repositories, value objects
├── events/            # Shared application event records
└── config/            # Spring configuration classes
```

---

## Checklist Before Implementing Any Feature

- [ ] Does the code fit cleanly into Boundary, Control, or Entity?
- [ ] Are DTOs and events defined as Java Records?
- [ ] Does the feature cross a domain boundary? If yes, use an Application Event.
- [ ] Does the feature require a new dependency? If yes, **ask for pom.xml approval first**.
- [ ] Does the new migration follow `V{n}__{description}.sql` naming?
