import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { tap } from 'rxjs';
import { Me, Role } from './api';

export const HOME: Record<Role, string> = { EMPLOYEE: '/host', FRONT_DESK: '/desk', ADMIN: '/admin' };

/** Holds the signed-in user for this browser tab. The server checks every request; this only drives the UI. */
@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);
  readonly user = signal<Me | null>(JSON.parse(sessionStorage.getItem('user') ?? 'null'));

  login(username: string, password: string) {
    sessionStorage.setItem('credentials', btoa(`${username}:${password}`));
    return this.http.get<Me>('/api/me').pipe(
      tap({
        next: (me) => {
          sessionStorage.setItem('user', JSON.stringify(me));
          this.user.set(me);
        },
        error: () => this.logout(),
      }),
    );
  }

  logout() {
    sessionStorage.clear();
    this.user.set(null);
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const credentials = sessionStorage.getItem('credentials');
  // X-Requested-With makes Spring Security answer 401 without a WWW-Authenticate header, so the browser
  // does not show its own sign-in popup.
  const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };
  if (credentials) {
    headers['Authorization'] = `Basic ${credentials}`;
  }
  return next(req.clone({ setHeaders: headers }));
};

export const requireRole =
  (role: Role): CanActivateFn =>
  () =>
    inject(Auth).user()?.role === role || inject(Router).parseUrl('/');
