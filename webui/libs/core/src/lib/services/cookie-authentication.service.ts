import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Logger } from '@cristalise/core';

@Injectable({
  providedIn: 'root'
})
export class CookieAuthenticationService {

  public isLoggedIn = false;
  public redirectUrl: string | null = null;

  private loggedInUser = new Subject<string>();

  private root = "/api";

  private log = new Logger('@cristalise/core', 'CookieAuthenticationService')

  constructor(private http: HttpClient) {
  }

  login(userName: string, password: string): Observable<boolean> {
    const result = new Subject<boolean>();

    this.callLoginPost(userName, password).subscribe({
      next: (value) => {
        this.log.debug('login()', value);
        if (value['Login'] && value['Login'].uuid) {
          this.loggedInUser.next(value['Login'].uuid);
          this.isLoggedIn = true;
          result.next(true);
        }
        else {
          result.next(false);
        }
      },
      error: (error) => {
        this.log.debug('login()', error);
        result.next(false);
      },
      complete: () => {
        this.log.debug('login()', 'complete');
      }
    });
    return result.asObservable();
  }

  callLoginPost(agent: string, pwd: string): Observable<any> {
    const url = this.root + '/login';
    // btoa encrypts username and password to base64 - UTF8 Charset
    const body = JSON.stringify({ username: btoa(agent), password: btoa(pwd) });

    return this.http.post(url, body, { withCredentials: true });
  }

  logout(): void {
    this.loggedInUser.next("");
    this.isLoggedIn = false;
  }

  listen(): Observable<string> {
    return this.loggedInUser.asObservable();
  }
}
