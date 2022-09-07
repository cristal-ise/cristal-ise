import { Component, OnDestroy } from '@angular/core';
import { Router } from "@angular/router";

import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Message, MessageService } from 'primeng/api';

import { CookieAuthenticationService, Logger } from "@cristalise/core";

@Component({
  selector: 'cristalise-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnDestroy {

  private readonly onDestroy$: Subject<void> = new Subject();
  private log = new Logger('@cristalise/admin', 'LoginComponent')

  valCheck: string[] = ['remember'];

  public user = "";
  public password = "";
  public messages: Message[] = [];

  constructor(
    private router: Router,
    private messageService: MessageService,
    private authService: CookieAuthenticationService) {
  }

  ngOnDestroy(): void {
    this.onDestroy$.next();
    this.onDestroy$.complete();
  }

  submit() {
    this.messages = []

    this.authService.login(this.user, this.password)
    .pipe(takeUntil(this.onDestroy$))
    .subscribe({
      next: (result) => {
        if (result) {
          this.router.navigateByUrl(this.authService.redirectUrl || "/");
        }
        else {
          const message = { severity:'error', summary:'Info', detail:'Invalid username or password' };
          this.messages = [message];
          this.log.debug('submit()', message);
        }
      },
      error: (error) => {
        const message = {severity:'error', summary:'Info', detail: 'Internal server during login'};
        this.messages = [message]
        this.log.debug('submit()', error);
      }
    })
  }
}
