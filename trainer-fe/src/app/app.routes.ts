import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login-page.component'),
    canActivate: [guestGuard],
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./features/auth/register/register-page.component'),
    canActivate: [guestGuard],
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
