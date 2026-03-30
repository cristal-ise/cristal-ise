import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DefaultService, LoginRequest } from '../../api';
import { catchError, map, of, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private defaultService = inject(DefaultService);
  private router = inject(Router);

  /**
   * Tracks the authentication status.
   * Initialized to false, but can be updated by checkSession().
   */
  isAuthenticated = signal<boolean>(false);

  /**
   * Intended destination when redirected to login.
   * Internal state to keep the returnUrl hidden from the browser address bar.
   */
  redirectUrl: string | null = null;

  /**
   * Performs login and updates authentication status on success.
   */
  login(loginRequest: LoginRequest) {
    return this.defaultService.loginPost({ loginRequest }).pipe(
      tap(() => this.isAuthenticated.set(true))
    );
  }

  /**
   * Performs logout, updates authentication status, and redirects to landing page.
   */
  logout() {
    return this.defaultService.logoutGet({ reason: undefined }).pipe(
      catchError(() => of(null)), // Still logout locally if server call fails
      tap(() => {
        this.isAuthenticated.set(false);
        this.router.navigate(['/']);
      })
    );
  }

  /**
   * Attempts to verify if a session is already active by calling a protected endpoint.
   * Useful for restoring state after page refresh.
   */
  checkSession() {
    return this.defaultService.domainGet({ start: 0, batch: 1 }).pipe(
      map(() => {
        this.isAuthenticated.set(true);
        return true;
      }),
      catchError(() => {
        this.isAuthenticated.set(false);
        return of(false);
      })
    );
  }
}
