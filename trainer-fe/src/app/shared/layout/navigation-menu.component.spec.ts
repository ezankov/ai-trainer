import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { provideRouter } from '@angular/router';

import { NavigationMenuComponent } from './navigation-menu.component';
import { AuthService, DecodedToken } from '../../core/auth/auth.service';
import { APP_VERSION } from '../../core/app-version.token';

describe('NavigationMenuComponent', () => {
  let component: NavigationMenuComponent;
  let fixture: ComponentFixture<NavigationMenuComponent>;
  let currentUserSubject: BehaviorSubject<DecodedToken | null>;
  let mockAuthService: jasmine.SpyObj<AuthService>;

  const TEST_VERSION = '1.2.3';

  beforeEach(async () => {
    currentUserSubject = new BehaviorSubject<DecodedToken | null>(null);

    mockAuthService = jasmine.createSpyObj('AuthService', ['logout'], {
      currentUser$: currentUserSubject.asObservable(),
    });

    await TestBed.configureTestingModule({
      imports: [NavigationMenuComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: APP_VERSION, useValue: TEST_VERSION },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NavigationMenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should display app version text from APP_VERSION token', () => {
    const versionEl = fixture.nativeElement.querySelector('.nav-menu__version');
    expect(versionEl.textContent).toContain(TEST_VERSION);
  });

  it('should display username from AuthService.currentUser$', () => {
    currentUserSubject.next({ sub: 'testuser', iat: 0, exp: 9999999999 });
    fixture.detectChanges();

    const usernameEl = fixture.nativeElement.querySelector('.nav-menu__username');
    expect(usernameEl.textContent.trim()).toBe('testuser');
  });

  it('should display empty string when currentUser$ emits null', () => {
    currentUserSubject.next(null);
    fixture.detectChanges();

    const usernameEl = fixture.nativeElement.querySelector('.nav-menu__username');
    expect(usernameEl.textContent.trim()).toBe('');
  });

  it('should update username when currentUser$ emits new value', () => {
    currentUserSubject.next({ sub: 'user1', iat: 0, exp: 9999999999 });
    fixture.detectChanges();

    const usernameEl = fixture.nativeElement.querySelector('.nav-menu__username');
    expect(usernameEl.textContent.trim()).toBe('user1');

    currentUserSubject.next({ sub: 'user2', iat: 0, exp: 9999999999 });
    fixture.detectChanges();

    expect(usernameEl.textContent.trim()).toBe('user2');
  });

  it('should render profile item with pi-user icon and "Profile" label', () => {
    const profileItem = fixture.nativeElement.querySelector('a[routerLink="/profile"]');
    expect(profileItem).toBeTruthy();

    const icon = profileItem.querySelector('i.pi.pi-user');
    expect(icon).toBeTruthy();

    const label = profileItem.querySelector('span');
    expect(label.textContent.trim()).toBe('Profile');
  });

  it('should have profile item with routerLink="/profile"', () => {
    const profileItem = fixture.nativeElement.querySelector('a[routerLink="/profile"]');
    expect(profileItem).toBeTruthy();
    expect(profileItem.getAttribute('routerLink')).toBe('/profile');
  });

  it('should render Training Plans item with icon, label, and routerLink to /plans', () => {
    const plansItem = fixture.nativeElement.querySelector('a[routerLink="/plans"]');
    expect(plansItem).toBeTruthy();

    const icon = plansItem.querySelector('i.pi.pi-calendar');
    expect(icon).toBeTruthy();

    const label = plansItem.querySelector('span');
    expect(label.textContent.trim()).toBe('Training Plans');
  });

  it('should have Training Plans item as an enabled clickable link', () => {
    const plansItem = fixture.nativeElement.querySelector('a[routerLink="/plans"]');
    expect(plansItem).toBeTruthy();
    expect(plansItem.tagName.toLowerCase()).toBe('a');
    expect(plansItem.getAttribute('routerLink')).toBe('/plans');
  });

  it('should position Training Plans item between Profile and Logout', () => {
    const items = fixture.nativeElement.querySelector('.nav-menu__items');
    const children = items.children;

    // First item should be Profile (the <a> with routerLink)
    expect(children[0].getAttribute('routerLink')).toBe('/profile');
    // Second item should be Training Plans (the <a> with routerLink /plans)
    expect(children[1].getAttribute('routerLink')).toBe('/plans');

    // Logout should be in the footer, after the spacer
    const footer = fixture.nativeElement.querySelector('.nav-menu__footer');
    expect(footer).toBeTruthy();
    const logoutBtn = footer.querySelector('.nav-menu__logout-btn');
    expect(logoutBtn).toBeTruthy();
  });

  it('should render logout icon at bottom, separated from menu items', () => {
    const footer = fixture.nativeElement.querySelector('.nav-menu__footer');
    expect(footer).toBeTruthy();

    const logoutBtn = footer.querySelector('.nav-menu__logout-btn');
    expect(logoutBtn).toBeTruthy();

    const icon = logoutBtn.querySelector('i.pi.pi-sign-out');
    expect(icon).toBeTruthy();

    // Verify spacer exists between items and footer
    const spacer = fixture.nativeElement.querySelector('.nav-menu__spacer');
    expect(spacer).toBeTruthy();
  });

  it('should call AuthService.logout() when clicking logout', () => {
    const logoutBtn = fixture.nativeElement.querySelector('.nav-menu__logout-btn');
    logoutBtn.click();

    expect(mockAuthService.logout).toHaveBeenCalled();
  });

  it('should have menu with fixed width of 72px', () => {
    const navMenu = fixture.nativeElement.querySelector('.nav-menu');
    const computedStyle = getComputedStyle(navMenu);
    expect(computedStyle.width).toBe('72px');
  });
});
