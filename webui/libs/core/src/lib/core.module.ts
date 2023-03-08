import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthenticationGuard } from './guards/authentication.guard';
import { CookieAuthenticationService } from './services/cookie-authentication.service';
import { LookupService } from './services/lookup.service';

@NgModule({
  providers: [AuthenticationGuard, CookieAuthenticationService, LookupService],
  imports: [CommonModule],
})
export class CoreModule {}
