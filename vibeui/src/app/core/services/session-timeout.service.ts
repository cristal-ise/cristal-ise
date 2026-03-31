import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { fromEvent, merge, Subscription, timer } from 'rxjs';
import { switchMap, throttleTime } from 'rxjs/operators';
import { MessageService } from 'primeng/api';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SessionTimeoutService implements OnDestroy {
  private ngZone = inject(NgZone);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);

  private timeoutSub?: Subscription;
  private isWarningShown = false;

  constructor() {
    this.initTimeout();

    // Listen to auth changes so we only track idle time when logged in
    // This assumes authService exposes a way to know if we are authenticated.
    // If we only start tracking once login succeeds, we can just start it.
    // However, the best approach is to check if authenticated during the timer evaluation
  }

  private initTimeout() {
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
                summary: 'Session Expiring',
                detail: `You have been inactive. The session will timeout in ${idleWarningMinutes} minute(s). Move your mouse or press a key to stay logged in.`,
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

  ngOnDestroy() {
    if (this.timeoutSub) {
      this.timeoutSub.unsubscribe();
    }
  }
}
