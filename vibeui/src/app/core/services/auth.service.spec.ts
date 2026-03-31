import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { DefaultService, Configuration } from '../../api';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('AuthService Timeout and Window Close', () => {
  let service: AuthService;
  let mockDefaultService: any;
  let mockRouter: any;
  let mockConfiguration: Configuration;

  beforeEach(() => {
    mockDefaultService = {
      logoutGet: vi.fn(),
      loginPost: vi.fn(),
      domainGet: vi.fn()
    };
    mockRouter = {
      navigate: vi.fn()
    };
    mockConfiguration = new Configuration({ basePath: 'http://localhost:8080/api' });

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: DefaultService, useValue: mockDefaultService },
        { provide: Router, useValue: mockRouter },
        { provide: Configuration, useValue: mockConfiguration }
      ]
    });

    service = TestBed.inject(AuthService);
    
    // Set initial state to authenticated to test logout state changes
    service.isAuthenticated.set(true);
  });

  describe('logout features', () => {
    it('should handle window closed feature by using fetch with keepalive', () => {
      // Mock global fetch
      const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(new Response());

      service.logout('windowClose').subscribe();

      expect(fetchSpy).toHaveBeenCalledWith(
        'http://localhost:8080/api/logout?reason=windowClose',
        { keepalive: true, credentials: 'include' }
      );
      expect(service.isAuthenticated()).toBe(false);
      expect(mockDefaultService.logoutGet).not.toHaveBeenCalled();
      expect(mockRouter.navigate).not.toHaveBeenCalled();
    });

    it('should handle timeout feature and redirect to login with correct state', () => {
      mockDefaultService.logoutGet.mockReturnValue(of(null as any));

      service.logout('timeout').subscribe();

      expect(mockDefaultService.logoutGet).toHaveBeenCalledWith({ reason: 'timeout' });
      expect(service.isAuthenticated()).toBe(false);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], { state: { loggedOutByTimeout: true } });
    });

    it('should handle standard logout and redirect to landing page', () => {
      mockDefaultService.logoutGet.mockReturnValue(of(null as any));

      service.logout().subscribe();

      expect(mockDefaultService.logoutGet).toHaveBeenCalledWith({ reason: undefined });
      expect(service.isAuthenticated()).toBe(false);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
    });
    
    it('should still logout locally if server call fails during timeout', () => {
      mockDefaultService.logoutGet.mockReturnValue(throwError(() => new Error('Network error')));

      service.logout('timeout').subscribe();

      expect(mockDefaultService.logoutGet).toHaveBeenCalledWith({ reason: 'timeout' });
      expect(service.isAuthenticated()).toBe(false);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login'], { state: { loggedOutByTimeout: true } });
    });
  });
});
