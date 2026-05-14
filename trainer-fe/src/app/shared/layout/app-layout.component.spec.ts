import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';

import { AppLayoutComponent } from './app-layout.component';
import { AuthService } from '../../core/auth/auth.service';
import { NavigationMenuComponent } from './navigation-menu.component';

@Component({
  selector: 'app-navigation-menu',
  standalone: true,
  template: '<div class="mock-nav-menu">Mock Nav</div>',
})
class MockNavigationMenuComponent {}

describe('AppLayoutComponent', () => {
  let fixture: ComponentFixture<AppLayoutComponent>;
  let component: AppLayoutComponent;
  let isAuthenticated$: BehaviorSubject<boolean>;

  beforeEach(async () => {
    isAuthenticated$ = new BehaviorSubject<boolean>(false);

    const mockAuthService = {
      isAuthenticated$: isAuthenticated$.asObservable(),
    };

    await TestBed.configureTestingModule({
      imports: [AppLayoutComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        provideRouter([]),
      ],
    })
      .overrideComponent(AppLayoutComponent, {
        remove: { imports: [NavigationMenuComponent] },
        add: { imports: [MockNavigationMenuComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(AppLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render NavigationMenuComponent when isAuthenticated$ emits true', () => {
    isAuthenticated$.next(true);
    fixture.detectChanges();

    const navMenu = fixture.debugElement.query(By.css('app-navigation-menu'));
    expect(navMenu).toBeTruthy();
  });

  it('should NOT render NavigationMenuComponent when isAuthenticated$ emits false', () => {
    isAuthenticated$.next(false);
    fixture.detectChanges();

    const navMenu = fixture.debugElement.query(By.css('app-navigation-menu'));
    expect(navMenu).toBeNull();
  });

  it('should hide menu when auth state changes from true to false', () => {
    isAuthenticated$.next(true);
    fixture.detectChanges();

    let navMenu = fixture.debugElement.query(By.css('app-navigation-menu'));
    expect(navMenu).toBeTruthy();

    isAuthenticated$.next(false);
    fixture.detectChanges();

    navMenu = fixture.debugElement.query(By.css('app-navigation-menu'));
    expect(navMenu).toBeNull();
  });

  it('should always render router-outlet regardless of auth state', () => {
    isAuthenticated$.next(false);
    fixture.detectChanges();

    let routerOutlet = fixture.debugElement.query(By.css('router-outlet'));
    expect(routerOutlet).toBeTruthy();

    isAuthenticated$.next(true);
    fixture.detectChanges();

    routerOutlet = fixture.debugElement.query(By.css('router-outlet'));
    expect(routerOutlet).toBeTruthy();
  });

  it('should use flexbox layout with menu and content side by side when authenticated', () => {
    isAuthenticated$.next(true);
    fixture.detectChanges();

    const layoutEl = fixture.debugElement.query(By.css('.app-layout'));
    expect(layoutEl).toBeTruthy();

    const styles = getComputedStyle(layoutEl.nativeElement);
    expect(styles.display).toBe('flex');

    const contentEl = fixture.debugElement.query(By.css('.app-layout__content'));
    expect(contentEl).toBeTruthy();
  });
});
