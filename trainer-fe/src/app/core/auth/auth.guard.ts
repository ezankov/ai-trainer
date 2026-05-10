import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { map, take } from 'rxjs';

/**
 * Guard for protected routes.
 * Redirects unauthenticated users to /auth/login with the original URL preserved as a redirectTo query parameter.
 * If the token in localStorage is expired, removes it and treats the user as unauthenticated.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Re-evaluate token validity on each navigation
  authService.initFromStorage();

  return authService.isAuthenticated$.pipe(
    take(1),
    map(isAuthenticated => {
      if (isAuthenticated) {
        return true;
      }

      return router.createUrlTree(['/auth/login'], {
        queryParams: { redirectTo: state.url }
      });
    })
  );
};

/**
 * Guard for auth routes (login, register).
 * Redirects authenticated users to the home page (/).
 */
export const guestGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Re-evaluate token validity on each navigation
  authService.initFromStorage();

  return authService.isAuthenticated$.pipe(
    take(1),
    map(isAuthenticated => {
      if (!isAuthenticated) {
        return true;
      }

      return router.createUrlTree(['/']);
    })
  );
};
