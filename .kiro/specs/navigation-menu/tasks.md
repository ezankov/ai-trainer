# Implementation Plan: navigation-menu

## Overview

Implement a persistent left-side navigation menu for authenticated users in the Angular 21 frontend. This introduces two new standalone components (`AppLayoutComponent` and `NavigationMenuComponent`) in `src/app/shared/layout/`, an `APP_VERSION` injection token in `src/app/core/`, and updates `AppComponent` to delegate layout responsibility to `AppLayoutComponent`. No backend changes are required.

## Tasks

- [x] 1. Create APP_VERSION token and configure provider
  - [x] 1.1 Create `APP_VERSION` injection token in `src/app/core/app-version.token.ts`
    - Define and export `APP_VERSION` as an `InjectionToken<string>`
    - Register the token in `app.config.ts` providers array using `{ provide: APP_VERSION, useValue: packageJson.version }`
    - Import `package.json` using `import packageJson from '../../package.json'` (ensure `resolveJsonModule: true` in `tsconfig.json`)
    - _Requirements: 2.1_

- [x] 2. Implement NavigationMenuComponent
  - [x] 2.1 Create `NavigationMenuComponent` in `src/app/shared/layout/`
    - Create standalone component with selector `app-navigation-menu`
    - Inject `AuthService`, `Router`, and `APP_VERSION` token
    - Derive `username` signal from `AuthService.currentUser$` mapping to `user?.sub ?? ''`
    - Store `appVersion` from injected `APP_VERSION` token
    - Implement `logout()` method that calls `AuthService.logout()`
    - Define static `menuItems` array with Profile (`pi-user`, routerLink `/profile`) and Plans (`pi-calendar`, disabled)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2_

  - [x] 2.2 Create template and styles for `NavigationMenuComponent`
    - Render profile menu item as `<a>` with `routerLink="/profile"` and `routerLinkActive` for active styling
    - Render Plans menu item as a `<div>` with disabled class (`opacity: 0.4`, `pointer-events: none`)
    - Position Plans between Profile and Logout
    - Render logout button (`pi-sign-out`) at the bottom, separated by a spacer
    - Display username (with `text-overflow: ellipsis`, `max-width: 60px`) and version (`v{{ appVersion }}`) in the footer
    - Set component width to 72px, height to 100vh, flex column layout
    - Use PrimeNG icon classes and CSS variables for theming (`--surface-card`, `--surface-border`, `--primary-color`, etc.)
    - _Requirements: 2.1, 2.2, 3.1, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 6.2_

  - [x]* 2.3 Write unit tests for `NavigationMenuComponent`
    - Test: displays app version text from `APP_VERSION` token
    - Test: displays username from `AuthService.currentUser$`
    - Test: displays empty string when `currentUser$` emits `null`
    - Test: updates username when `currentUser$` emits new value
    - Test: renders profile item with `pi-user` icon and "Profile" label
    - Test: profile item has `routerLink="/profile"`
    - Test: renders Plans item with icon, label, and disabled styling
    - Test: Plans item is non-interactive (`pointer-events: none`)
    - Test: Plans item positioned between Profile and Logout
    - Test: renders logout icon at bottom, separated from menu items
    - Test: clicking logout calls `AuthService.logout()`
    - Test: menu has fixed width of 72px
    - Mock `AuthService` with `BehaviorSubject`, provide test `APP_VERSION` value
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 6.2_

- [x] 3. Implement AppLayoutComponent
  - [x] 3.1 Create `AppLayoutComponent` in `src/app/shared/layout/`
    - Create standalone component with selector `app-layout`
    - Inject `AuthService`
    - Derive `isAuthenticated` signal using `toSignal` from `@angular/core/rxjs-interop` on `AuthService.isAuthenticated$`
    - Template: when authenticated, render flex container with `<app-navigation-menu />` and `<main>` wrapping `<router-outlet>`; when unauthenticated, render only `<router-outlet>`
    - Import `NavigationMenuComponent`, `RouterOutlet`
    - _Requirements: 1.1, 1.2, 1.4_

  - [x] 3.2 Create styles for `AppLayoutComponent`
    - `.app-layout`: `display: flex`, `height: 100vh`
    - `.app-layout__content`: `flex: 1`, `overflow-y: auto`, `min-width: 0`
    - Ensures menu is fixed 72px on left, content fills remaining width, no overlap
    - Content area scrolls independently of the navigation menu
    - _Requirements: 6.1, 6.3, 6.4, 6.5_

  - [x]* 3.3 Write unit tests for `AppLayoutComponent`
    - Test: renders `NavigationMenuComponent` when `isAuthenticated$` emits `true`
    - Test: does NOT render `NavigationMenuComponent` when `isAuthenticated$` emits `false`
    - Test: hides menu when auth state changes from `true` to `false`
    - Test: always renders `<router-outlet>` regardless of auth state
    - Test: layout uses flexbox with menu and content side by side
    - Mock `AuthService` with `BehaviorSubject` to control auth state
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.1, 6.3_

- [x] 4. Checkpoint — Ensure components compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Wire AppLayoutComponent into AppComponent
  - [x] 5.1 Update `AppComponent` to use `AppLayoutComponent`
    - Replace `RouterOutlet` import with `AppLayoutComponent` import
    - Change template from `<router-outlet />` to `<app-layout />`
    - Remove `RouterOutlet` from imports array, add `AppLayoutComponent`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.1_

  - [x]* 5.2 Write integration-style tests for navigation and layout
    - Test: after `AuthService.logout()`, user is navigated to `/auth/login`
    - Test: default route `/` redirects to `/profile` for authenticated users
    - Test: menu persists across route changes without re-initializing
    - Use `provideRouter` with test routes and `RouterTestingHarness`
    - _Requirements: 1.3, 3.4, 5.3, 5.4_

- [x] 6. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- No property-based tests — the design explicitly assessed PBT as not applicable for this UI/layout feature
- Testing uses Karma + Jasmine (project standard); mock `AuthService` with `BehaviorSubject` for reactive state control
- The existing route configuration already handles `/` → `/profile` redirect and auth guards, so no route changes are needed
- `resolveJsonModule: true` should already be set in Angular 21's default `tsconfig.json`; verify before importing `package.json`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["2.3", "3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["5.2"] }
  ]
}
```
