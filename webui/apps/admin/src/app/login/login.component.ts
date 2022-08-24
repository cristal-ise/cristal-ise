import { Component } from '@angular/core';
import { Router } from "@angular/router";

import { CookieAuthenticationService } from "@cristalise/core";

@Component({
  selector: 'cristalise-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  valCheck: string[] = ['remember'];

  public user = "";
  public password = "";

  constructor(private router: Router, private authService: CookieAuthenticationService) {}

  submit() {
    this.authService.login(this.user, this.password);
    this.router.navigateByUrl(this.authService.redirectUrl || "/");
  }
}
