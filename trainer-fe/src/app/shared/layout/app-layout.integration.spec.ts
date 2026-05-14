import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AppLayoutComponent } from './app-layout.component';
import { NavigationMenuComponent } from './navigation-menu.component';
import { AuthService, DecodedToken } from '../../core/auth/auth.service';
import { APP_VERSION } from '../../core/app-version.token';

@Component({ standalone: true, selector: 'test-profile', template: '<p>Profile Page</p>' })
class MockProfileComponent {}

@Component({ standalone: true, selector: 'test-login', template: '<p>Login Page</p>' })
class MockLoginComponent {}

@Component({ standalone: true, selector: 'test-other', template: '<p>Other Page</p>' })
class MockOtherComponent {}

describe('AppLayout Integration', () => {
  let isAuthenticated$: BehaviorSubject<boolean>;
  let currentUser$: BehaviorSubject<DecodedToken | null>;
  let logoutSpy: jasmine.Spy;
  let router: Router;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    isAuthenticated$ = new BehaviorSubject<boolean>(true);
    currentUser$ = new BehaviorSubject<DecodedToken | null>({
      sub: 'testuser',
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
    });
    logoutSpy = jasmine.createSpy('logout');

    const mockAuthService = {
      isAuthenticated$: isAuthenticated$.asObservable(),
      currentUser$: currentUser$.asObservable(),
      logout: logoutSpy,
      initFromStorage: jasmine.createSpy('initFromStorage'),
    };

    await TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: APP_VERSION, useValue: '1.0.0-test' },
        provideRouter([
          {
            path: '',
            component: AppLayoutComponent,
            children: [
              { path: 'profile', component: MockProfileComponent },
              { path: 'other', component: MockOtherComponent },
              { path: '', redirectTo: 'profile', pathMatch: 'full' },
            ],
          },
          { path: 'auth/login', component: MockLoginComponent },
        ]),
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    harness = await RouterTestingHarness.create();
  });

  it('should navigate to /auth/login after AuthService.logout()', async () => {
    await harness.navigateByUrl('/profile');

    logoutSpy.and.callFake(() => {
      isAuthenticated$.next(false);
      currentUser$.next(null);
      router.navigate(['/auth/login']);
    });

    const authService = TestBed.inject(AuthService);
    authService.logout();
    await harness.fixture.whenStable();

    expect(router.url).toBe('/auth/login');
  });

  it('should redirect default route / to /profile for authenticated users', async () => {
    isAuthenticated$.next(true);

    await harness.navigateByUrl('/');

    expect(router.url).toBe('/profile');
  });

  it('should persist menu across route changes without re-initializing', async () => {
    isAuthenticated$.next(true);

    const rootFixture = harness.fixture;

    await harness.navigateByUrl('/profile');
    rootFixture.detectChanges();

    const menuBefore = rootFixture.nativeElement.querySelector('app-navigation-menu');
    expect(menuBefore).toBeTruthy();

    await harness.navigateByUrl('/other');
    rootFixture.detectChanges();

    const menuAfter = rootFixture.nativeElement.querySelector('app-navigation-menu');
    expect(menuAfter).toBeTruthy();
    expect(menuAfter).toBe(menuBefore);
  });
});
