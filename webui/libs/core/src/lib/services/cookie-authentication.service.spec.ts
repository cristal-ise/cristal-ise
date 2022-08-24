import { TestBed } from '@angular/core/testing';

import { CookieAuthenticationService } from './cookie-authentication.service';

describe('CookieAuthenticationService', () => {
  let service: CookieAuthenticationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CookieAuthenticationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
