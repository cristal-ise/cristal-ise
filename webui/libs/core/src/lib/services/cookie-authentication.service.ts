import { Injectable } from '@angular/core';
// import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CookieAuthenticationService {

  public isLoggedIn = false;
  public redirectUrl: string | null = null;

  private loggedInUser = new Subject<string>();

  login(userName: string, password: string): void {
    if (userName && password) {
      this.loggedInUser.next(userName);
      this.isLoggedIn = true;
    }
  }

  logout(): void {
    this.loggedInUser.next("");
    this.isLoggedIn = false;
  }

  listen(): Observable<string> {
    return this.loggedInUser.asObservable();
  }
}
