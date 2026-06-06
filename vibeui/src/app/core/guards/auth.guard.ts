import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Functional guard to protect routes that require authentication.
 * Redirects to the login page if the user is not authenticated,
 * and passes the current URL as a 'returnUrl' query parameter.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Store the intended destination internally to keep the URL clean
  authService.redirectUrl = state.url;

  // Redirect to login
  return router.createUrlTree(['/login']);
};
