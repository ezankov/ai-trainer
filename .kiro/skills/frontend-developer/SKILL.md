# Skill: Frontend Developer

Architectural guidelines for the `trainer-fe` Angular 21 frontend.

---

## 1. Framework & Performance

### Zoneless Change Detection
- Configure the app with `provideExperimentalZonelessChangeDetection()` — do **not** use `provideZoneChangeDetection()`.
- Never rely on `zone.js` to trigger change detection. All reactivity must flow through Signals.

### Signals Only
- Use **Angular Signals** (`signal()`, `computed()`, `effect()`) for all state management and data flow.
- **Do not use `BehaviorSubject`, `Subject`, or `ReplaySubject` for state.** Observables are only acceptable at the boundary where they are unavoidable (e.g., `HttpClient` responses) — convert immediately to Signals using `toSignal()`.
- Derived state must use `computed()`, never manually synchronized signals.

### Control Flow Syntax
- Exclusively use the built-in control flow syntax: `@if`, `@for`, `@switch`.
- **Never use** `*ngIf`, `*ngFor`, `*ngSwitch`, or `NgIf`/`NgFor`/`NgSwitch` directives.

```html
<!-- ✅ Correct -->
@if (user()) {
  <app-profile [user]="user()" />
}

@for (item of items(); track item.id) {
  <app-item [item]="item" />
}

<!-- ❌ Forbidden -->
<div *ngIf="user">...</div>
<div *ngFor="let item of items">...</div>
```

---

## 2. Architecture & Pattern

### BCE Alignment
The frontend mirrors the backend's BCE pattern:

| BCE Layer | Frontend Equivalent |
|---|---|
| **Boundary** | Components (Smart + Presentational) — UI surface |
| **Control** | Services — business logic, state, HTTP orchestration |
| **Entity** | TypeScript interfaces/types — domain models |

### Component Structure — Smart vs Presentational

**Smart Components** (feature-level, routed)
- Own Signals and interact with Control services
- Handle user events and dispatch actions
- Live in `src/app/features/<feature>/`
- Suffix: `<Feature>PageComponent` or `<Feature>ContainerComponent`

**Presentational Components** (pure UI)
- Receive data exclusively via `input()` signals
- Emit events exclusively via `output()`
- Zero service injection — no side effects
- Live in `src/app/shared/ui/` (global) or `src/app/features/<feature>/components/` (feature-scoped)
- Suffix: `<Name>Component`

```typescript
// ✅ Presentational component
@Component({ standalone: true, ... })
export class UserCardComponent {
  user = input.required<User>();
  edit = output<User>();
}

// ✅ Smart component
@Component({ standalone: true, ... })
export class ProfilePageComponent {
  private userService = inject(UserService);
  user = this.userService.currentUser; // Signal
}
```

### State Management — Signal-Based Services
- Services act as lightweight Signal Stores.
- State is held in `private` signals, exposed as `readonly` via `asReadonly()` or `computed()`.
- State mutations happen only through explicit service methods (signal-based actions).
- State must be treated as **immutable** — always replace, never mutate in place.

```typescript
@Injectable({ providedIn: 'root' })
export class UserService {
  private _user = signal<User | null>(null);
  readonly user = this._user.asReadonly();

  setUser(user: User) {
    this._user.set(user);  // replace, never mutate
  }
}
```

---

## 3. Inter-Component / Inter-Module Communication

### Mediator Pattern — Signal-Based Event Bus
- **No direct service injection across feature boundaries.** A service in `features/workout` must never inject a service from `features/auth`.
- Cross-feature communication uses a shared `EventBusService` (or dedicated mediator) that exposes named signals or a signal-based message stream.
- Feature modules react to bus events via `effect()` in their own services.

```
src/app/core/
  └── event-bus.service.ts    // Signal-based mediator
```

```typescript
// Publishing
eventBus.emit({ type: 'USER_LOGGED_IN', payload: user });

// Consuming (in another feature's service)
effect(() => {
  const event = eventBus.latest();
  if (event?.type === 'USER_LOGGED_IN') { ... }
});
```

---

## 4. Build System & Styling

### Build System Constraints

> **⚠️ STRICTLY FORBIDDEN: Do not modify `package.json` or `angular.json` without explicit prior approval.**

- Never add, remove, or change a dependency, script, or configuration in `package.json` or `angular.json` on your own initiative.
- If a feature requires a new package or build config change, **stop and ask the user for permission first**, explaining what is needed and why.
- This rule has no exceptions.

### Standalone Only
- Every component, directive, and pipe must be `standalone: true`.
- **NgModules are forbidden.** Do not create or reference any `@NgModule`.

### Styling — SCSS + CSS Variables
- All styles are written in SCSS.
- The "AI-Trainer" design system is implemented via **CSS Custom Properties** (variables) defined at the `:root` level in `src/styles.scss`.
- Components use these variables — never hardcode colors, spacing, or typography values.

```scss
// src/styles.scss — design system tokens
:root {
  --color-primary: #...;
  --color-surface: #...;
  --spacing-md: 1rem;
  --font-body: 'Inter', sans-serif;
}

// component.scss — consume tokens
.card {
  background: var(--color-surface);
  padding: var(--spacing-md);
}
```

---

## 5. Package Layout Summary

```
src/app/
├── core/                        # Singleton services, guards, interceptors, event bus
│   ├── event-bus.service.ts
│   └── auth/
├── shared/
│   ├── ui/                      # Reusable presentational components
│   └── models/                  # TypeScript interfaces (Entity layer)
├── features/
│   └── <feature>/
│       ├── <feature>.routes.ts  # Lazy-loaded route config
│       ├── <feature>-page.component.ts   # Smart component
│       └── components/          # Feature-scoped presentational components
└── app.routes.ts                # Root routes (lazy-load features)
```

---

## 6. Checklist Before Implementing Any Feature

- [ ] Is change detection zoneless? No `zone.js` triggers relied upon.
- [ ] Is all state managed via Signals? No `BehaviorSubject` for state.
- [ ] Are `@if` / `@for` / `@switch` used exclusively? No structural directives.
- [ ] Is the component Smart or Presentational? Is the boundary clear?
- [ ] Does the component cross a feature boundary? If yes, use the Event Bus.
- [ ] Is the component `standalone: true`? No NgModule involved.
- [ ] Are styles using CSS Variables from the design system? No hardcoded values.
- [ ] Does the feature need a new package or config change? If yes, **ask for approval first**.
