import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthenticationGuard } from './guards/authentication.guard';
import { CookieAuthenticationService } from './services/cookie-authentication.service';

@NgModule({
  providers: [AuthenticationGuard, CookieAuthenticationService],
  imports: [CommonModule],
})
export class CoreModule {}
