import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { NavigationMenuComponent } from './navigation-menu.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, NavigationMenuComponent],
  template: `
    @if (isAuthenticated()) {
      <div class="app-layout">
        <app-navigation-menu />
        <main class="app-layout__content">
          <router-outlet />
        </main>
      </div>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .app-layout {
      display: flex;
      height: 100vh;
    }

    .app-layout__content {
      flex: 1;
      overflow-y: auto;
      min-width: 0;
    }
  `],
})
export class AppLayoutComponent {
  private readonly authService = inject(AuthService);

  readonly isAuthenticated = toSignal(this.authService.isAuthenticated$, {
    initialValue: false,
  });
}
