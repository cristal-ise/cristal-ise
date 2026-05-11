import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Configuration, DefaultService, LoginRequest } from '../../api';
import { catchError, map, of, tap, throwError } from 'rxjs';
import { ApiErrorService } from './api-error.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private defaultService = inject(DefaultService);
  private configuration = inject(Configuration);
  private router = inject(Router);
  private apiErrorService = inject(ApiErrorService);

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
      tap(() => this.isAuthenticated.set(true)),
      catchError((error: HttpErrorResponse) => {
        this.apiErrorService.handleError(error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Performs logout, updates authentication status, and redirects to landing page.
   * Uses fetch with keepalive for 'windowClose' to ensure the request completes during unload.
   */
  logout(reason?: 'timeout' | 'windowClose') {
    if (reason === 'windowClose') {
      const url = `${this.configuration.basePath}/logout?reason=windowClose`;
      fetch(url, { keepalive: true, credentials: 'include' });
      this.isAuthenticated.set(false);
      return of(null);
    }
    return this.defaultService.logoutGet({ reason }).pipe(
      catchError((error: HttpErrorResponse) => {
        this.apiErrorService.handleError(error);
        return of(null);
      }), // Still logout locally if server call fails
      tap(() => {
        this.isAuthenticated.set(false);
        if (reason === 'timeout') {
          this.router.navigate(['/login'], { state: { loggedOutByTimeout: true } });
        } else {
          this.router.navigate(['/']);
        }
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
      catchError((error: HttpErrorResponse) => {
        // We don't show toast for checkSession failure as it's a silent check
        this.isAuthenticated.set(false);
        return of(false);
      })
    );
  }
}
