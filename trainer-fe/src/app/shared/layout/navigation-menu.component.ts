import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { DividerModule } from 'primeng/divider';

import { AuthService } from '../../core/auth/auth.service';
import { APP_VERSION } from '../../core/app-version.token';

export interface MenuItem {
  label: string;
  icon: string;
  routerLink?: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-navigation-menu',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, ButtonModule, TooltipModule, DividerModule],
  template: `
    <nav class="nav-menu">
      <div class="nav-menu__items">
        <a class="nav-menu__item"
           routerLink="/profile"
           routerLinkActive="nav-menu__item--active"
           pTooltip="Profile"
           tooltipPosition="right">
          <i class="pi pi-user nav-menu__icon"></i>
          <span class="nav-menu__label">Profile</span>
        </a>

        <div class="nav-menu__item nav-menu__item--disabled"
             pTooltip="Coming soon"
             tooltipPosition="right">
          <i class="pi pi-calendar nav-menu__icon"></i>
          <span class="nav-menu__label">Plans</span>
        </div>
      </div>

      <div class="nav-menu__spacer"></div>

      <div class="nav-menu__footer">
        <div class="nav-menu__user-info">
          <span class="nav-menu__username">{{ username() }}</span>
          <span class="nav-menu__version">v{{ appVersion }}</span>
        </div>

        <p-divider />

        <button class="nav-menu__item nav-menu__logout-btn"
                (click)="logout()"
                pTooltip="Logout"
                tooltipPosition="right">
          <i class="pi pi-sign-out nav-menu__icon"></i>
          <span class="nav-menu__label">Logout</span>
        </button>
      </div>
    </nav>
  `,
  styleUrl: './navigation-menu.component.scss',
})
export class NavigationMenuComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly appVersion = inject(APP_VERSION);

  readonly username = toSignal(
    this.authService.currentUser$.pipe(
      map(user => user?.sub ?? '')
    ),
    { initialValue: '' }
  );

  readonly menuItems: MenuItem[] = [
    { label: 'Profile', icon: 'pi-user', routerLink: '/profile' },
    { label: 'Plans', icon: 'pi-calendar', disabled: true },
  ];

  logout(): void {
    this.authService.logout();
  }
}
