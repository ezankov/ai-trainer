import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, map, Observable, tap } from 'rxjs';

export interface DecodedToken {
  sub: string;
  iat: number;
  exp: number;
}

interface LoginResponse {
  token: string;
}

interface RegisterResponse {
  id: number;
  username: string;
}

const TOKEN_KEY = 'auth_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<DecodedToken | null>(null);

  currentUser$ = this.currentUserSubject.asObservable();
  isAuthenticated$: Observable<boolean> = this.currentUser$.pipe(
    map(user => user !== null)
  );

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', credentials).pipe(
      tap(response => {
        localStorage.setItem(TOKEN_KEY, response.token);
        const decoded = this.decodeToken(response.token);
        this.currentUserSubject.next(decoded);
      })
    );
  }

  register(data: { username: string; email: string; password: string }): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', data);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  initFromStorage(): void {
    const token = this.getToken();
    if (!token) {
      this.currentUserSubject.next(null);
      return;
    }

    const decoded = this.decodeToken(token);
    if (!decoded || this.isTokenExpired(decoded)) {
      localStorage.removeItem(TOKEN_KEY);
      this.currentUserSubject.next(null);
      return;
    }

    this.currentUserSubject.next(decoded);
  }

  private decodeToken(token: string): DecodedToken | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }
      const payload = parts[1];
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded) as DecodedToken;
    } catch {
      return null;
    }
  }

  private isTokenExpired(decoded: DecodedToken): boolean {
    return decoded.exp < Date.now() / 1000;
  }
}
