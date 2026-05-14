# Requirements Document

## Introduction

This feature adds a persistent side navigation menu to the AI Trainer application. The menu appears on the left side of the viewport for authenticated users and provides access to the user profile, displays the application version and logged-in username, includes a placeholder "Plans" menu item, and offers a logout action. The profile page remains the default landing page after login.

## Glossary

- **Navigation_Menu**: The Angular standalone component that renders the left-side navigation panel for authenticated users.
- **Auth_Service**: The Angular service responsible for managing the authentication state on the client (already exists in `src/app/core/auth/`).
- **Profile_Page**: The Angular standalone component displaying the athlete profile (already exists in `src/app/features/athlete-profile/`).
- **Login_Page**: The Angular standalone component presenting the login form (already exists in `src/app/features/auth/login/`).
- **App_Layout**: The Angular standalone component that wraps authenticated routes with the Navigation_Menu alongside the main content area.
- **App_Version**: The version string defined in the frontend `package.json` (`version` field).
- **Logged_In_User**: The username extracted from the JWT `sub` claim of the currently authenticated user.

---

## Requirements

### Requirement 1: Navigation Menu Visibility

**User Story:** As a logged-in user, I want to see a navigation menu on the left side of the screen, so that I can access different sections of the application.

#### Acceptance Criteria

1. WHILE a user is authenticated, THE App_Layout SHALL display the Navigation_Menu on the left side of the viewport alongside the main content area.
2. WHILE a user is unauthenticated, THE App_Layout SHALL hide the Navigation_Menu and render only the main content area.
3. WHILE the user navigates between authenticated routes, THE Navigation_Menu SHALL remain rendered without unmounting or re-initializing, maintaining its scroll position and visual state.
4. WHEN the Auth_Service authentication state changes from authenticated to unauthenticated, THE App_Layout SHALL immediately hide the Navigation_Menu and render only the main content area.

---

### Requirement 2: Display Application Version and Username

**User Story:** As a logged-in user, I want to see the app version and my username in the navigation menu, so that I know which version I am using and confirm I am logged in with the correct account.

#### Acceptance Criteria

1. THE Navigation_Menu SHALL display the App_Version as visible text that is not truncated.
2. THE Navigation_Menu SHALL display the Logged_In_User username as visible text, truncated with an ellipsis if the username exceeds the available width of the Navigation_Menu.
3. WHEN the authenticated user changes (e.g. after a new login), THE Navigation_Menu SHALL update the displayed Logged_In_User to reflect the current user's username.
4. IF the Logged_In_User username cannot be extracted from the JWT, THEN THE Navigation_Menu SHALL display an empty string in place of the username.

---

### Requirement 3: User Profile Navigation

**User Story:** As a logged-in user, I want to click a profile icon in the navigation menu, so that I can view my athlete profile page.

#### Acceptance Criteria

1. THE Navigation_Menu SHALL display a clickable user profile icon (PrimeNG `pi-user`) as a menu item with the label "Profile".
2. WHEN the user clicks the profile icon, THE Navigation_Menu SHALL navigate the user to the `/profile` route.
3. WHILE the user is on the `/profile` route, THE Navigation_Menu SHALL visually indicate that the profile menu item is active by applying the active styling class.
4. WHEN an authenticated user logs in and no specific route is requested, THE App_Layout SHALL redirect to the `/profile` route, displaying the Profile_Page as the default view.

---

### Requirement 4: Plans Placeholder Menu Item

**User Story:** As a logged-in user, I want to see a "Plans" entry in the navigation menu, so that I am aware of upcoming functionality.

#### Acceptance Criteria

1. THE Navigation_Menu SHALL display a "Plans" menu item with a PrimeNG icon (e.g. `pi-calendar` or `pi-list`) and the label text "Plans".
2. THE Navigation_Menu SHALL render the "Plans" menu item using the same layout structure, font size, icon size, and spacing as other menu items in the Navigation_Menu.
3. THE Navigation_Menu SHALL display the "Plans" menu item in a disabled state with reduced opacity to indicate it is not yet functional.
4. THE Navigation_Menu SHALL render the "Plans" menu item as non-interactive so that clicking or activating it performs no navigation, route change, or visible response.
5. THE Navigation_Menu SHALL position the "Plans" menu item after the profile menu item and before the logout icon.

---

### Requirement 5: Logout Action

**User Story:** As a logged-in user, I want to click a logout icon at the bottom of the navigation menu, so that I can end my session and return to the login page.

#### Acceptance Criteria

1. THE Navigation_Menu SHALL display a logout icon (PrimeNG `pi-sign-out`) positioned at the bottom of the menu, visually separated from the other menu items by a spacer or divider.
2. WHEN the user clicks the logout icon, THE Navigation_Menu SHALL invoke the Auth_Service logout method without displaying a confirmation dialog.
3. WHEN the Auth_Service logout method is invoked, THE Auth_Service SHALL remove the JWT from local storage, clear the authenticated state, and navigate the user to the Login_Page route (`/login`).
4. AFTER logout completes, IF the user attempts to navigate to an authenticated route, THE application SHALL redirect the user back to the Login_Page.

---

### Requirement 6: Responsive Layout Integration

**User Story:** As a logged-in user, I want the navigation menu and content area to coexist without overlap, so that I can use the application comfortably.

#### Acceptance Criteria

1. THE App_Layout SHALL position the Navigation_Menu as a fixed-width panel of 72 pixels on the left side and allocate the remaining viewport width to the main content area.
2. THE Navigation_Menu SHALL use a fixed width of 72 pixels, sufficient to display icons and text labels of up to 10 characters without truncation.
3. THE App_Layout SHALL ensure the main content area begins immediately to the right of the Navigation_Menu with no overlapping or underlapping of the two regions.
4. WHILE the viewport width is 576 pixels or greater, THE App_Layout SHALL display both the Navigation_Menu and the main content area side by side without horizontal scrolling.
5. WHILE the main content area content exceeds the available vertical space, THE App_Layout SHALL allow the main content area to scroll vertically independent of the Navigation_Menu.
