import { Injectable } from '@angular/core';
import { Router,ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot, UrlTree } from '@angular/router';
// import { Observable } from 'rxjs';
import { CookieAuthenticationService } from '../services/cookie-authentication.service';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationGuard implements CanActivate {
  constructor(
    private authService: CookieAuthenticationService,
    private router: Router,
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    return this.isLoggedIn(state.url);
  }

  isLoggedIn(url: string): true | UrlTree {
    // Store the attempted URL for redirecting
    this.authService.redirectUrl = url;

    if (this.authService.isLoggedIn) {
      return true;
    } else {
      // Redirect to the login page
      return this.router.parseUrl('/login');
    }
  }
}
