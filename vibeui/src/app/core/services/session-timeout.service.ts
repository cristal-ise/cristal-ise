import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { fromEvent, merge, Subscription, timer } from 'rxjs';
import { filter, switchMap, throttleTime } from 'rxjs/operators';
import { MessageService } from 'primeng/api';
import { NavigationEnd, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard } from '../guards/auth.guard';
import { environment } from '../../../environments/environment';
import { TranslocoService } from '@jsverse/transloco';

@Injectable({
  providedIn: 'root'
})
export class SessionTimeoutService implements OnDestroy {
  private ngZone = inject(NgZone);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private translocoService = inject(TranslocoService);
  private router = inject(Router);
  private timeoutSub?: Subscription;
  private routerSub?: Subscription;
  private isWarningShown = false;

  constructor() {
    // Listen to route changes to enable/disable timeout monitoring
    this.routerSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.checkRouteGuards());

    // Check initial route
    this.checkRouteGuards();
  }

  private checkRouteGuards() {
    const isGuarded = this.hasAuthGuard(this.router.routerState.snapshot.root);
    
    if (isGuarded) {
      this.initTimeout();
    } else {
      this.stopTimeout();
    }
  }

  private hasAuthGuard(snapshot: ActivatedRouteSnapshot): boolean {
    let current: ActivatedRouteSnapshot | null = snapshot;
    while (current) {
      if (current.routeConfig?.canActivate?.includes(authGuard)) {
        return true;
      }
      current = current.firstChild;
    }
    return false;
  }

  private initTimeout() {
    if (this.timeoutSub) {
      return; // Already running
    }

    const { idleTimeoutMinutes, idleWarningMinutes } = environment.auth;

    // Disabled if zero or negative
    if (!idleTimeoutMinutes || idleTimeoutMinutes <= 0) {
      return;
    }

    const maxIdleSeconds = idleTimeoutMinutes * 60;
    const warningThresholdSeconds = (idleTimeoutMinutes - idleWarningMinutes) * 60;

    // Activity events to monitor
    const activityEvents$ = merge(
      fromEvent(window, 'mousemove'),
      fromEvent(window, 'keydown'),
      fromEvent(window, 'click'),
      fromEvent(window, 'scroll')
    ).pipe(throttleTime(1000));

    // We run the timer outside Angular to avoid triggering change detection every second
    this.ngZone.runOutsideAngular(() => {
      // Create a stream that resets the timer every time an activity event fires
      this.timeoutSub = merge(timer(0), activityEvents$)
        .pipe(
          switchMap(() => {
            // Activity occurred -> clear any existing warning
            if (this.isWarningShown) {
              this.ngZone.run(() => {
                this.messageService.clear('system');
                this.isWarningShown = false;
              });
            }
            // Start a new timer counting seconds of inactivity
            return timer(0, 1000);
          })
        )
        .subscribe((idleSeconds) => {
          // Only enforce timeout if the user is authenticated
          if (!this.authService.isAuthenticated()) {
            return;
          }

          if (idleSeconds === warningThresholdSeconds) {
            this.ngZone.run(() => {
              this.isWarningShown = true;
              this.messageService.add({
                key: 'system',
                severity: 'warn',
                summary: this.translocoService.translate('layout.session_expiring'),
                detail: this.translocoService.translate('layout.session_expiring_detail', { minutes: idleWarningMinutes }),
                sticky: true
              });
            });
          } else if (idleSeconds >= maxIdleSeconds) {
            this.ngZone.run(() => {
              this.messageService.clear('system');
              this.isWarningShown = false;
              this.authService.logout('timeout').subscribe();
            });
          }
        });
    });
  }

  private stopTimeout() {
    if (this.timeoutSub) {
      this.timeoutSub.unsubscribe();
      this.timeoutSub = undefined;
    }
    
    // Clear warning if it was shown
    if (this.isWarningShown) {
      this.ngZone.run(() => {
        this.messageService.clear('system');
        this.isWarningShown = false;
      });
    }
  }

  ngOnDestroy() {
    if (this.timeoutSub) {
      this.timeoutSub.unsubscribe();
    }
    if (this.routerSub) {
      this.routerSub.unsubscribe();
    }
  }
}
