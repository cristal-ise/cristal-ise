import { TestBed } from '@angular/core/testing';
import { SessionTimeoutService } from './session-timeout.service';
import { AuthService } from './auth.service';
import { MessageService } from 'primeng/api';
import { NgZone, provideZonelessChangeDetection, EventEmitter, signal } from '@angular/core';
import { of, Subject } from 'rxjs';
import { intervalProvider } from 'rxjs/internal/scheduler/intervalProvider';
import { timeoutProvider } from 'rxjs/internal/scheduler/timeoutProvider';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Router, NavigationEnd } from '@angular/router';
import { authGuard } from '../guards/auth.guard';

describe('SessionTimeoutService', () => {
  let service: SessionTimeoutService;
  let mockAuthService: any;
  let mockMessageService: any;
  let mockRouter: any;
  let routerEventsSubject: Subject<any>;

  beforeEach(() => {
    vi.useFakeTimers();

    // Bridge RxJS's asyncScheduler to Vitest's fake timers.
    intervalProvider.delegate = {
      setInterval: (fn: any, delay?: any, ...args: any[]) =>
        (globalThis.setInterval as any)(fn, delay, ...args),
      clearInterval: (id: any) => (globalThis.clearInterval as any)(id),
    };
    timeoutProvider.delegate = {
      setTimeout: (fn: any, delay?: any, ...args: any[]) =>
        (globalThis.setTimeout as any)(fn, delay, ...args),
      clearTimeout: (id: any) => (globalThis.clearTimeout as any)(id),
    };

    mockAuthService = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      logout: vi.fn().mockReturnValue(of(null as any))
    };
    mockMessageService = {
      add: vi.fn(),
      clear: vi.fn()
    };

    routerEventsSubject = new Subject();
    mockRouter = {
      events: routerEventsSubject.asObservable(),
      routerState: {
        snapshot: {
          root: {
            routeConfig: {
              canActivate: {
                includes: () => true
              }
            },
            firstChild: null
          }
        }
      }
    };

    const mockNgZone: Pick<NgZone, 'run' | 'runOutsideAngular' | 'onStable' | 'onUnstable' | 'onMicrotaskEmpty' | 'onError' | 'isStable' | 'hasPendingMacrotasks' | 'hasPendingMicrotasks'> = {
      run: (fn: () => any) => fn(),
      runOutsideAngular: (fn: () => any) => fn(),
      onStable: new EventEmitter(),
      onUnstable: new EventEmitter(),
      onMicrotaskEmpty: new EventEmitter(),
      onError: new EventEmitter(),
      isStable: true,
      hasPendingMacrotasks: false,
      hasPendingMicrotasks: false,
    };

    TestBed.configureTestingModule({
      imports: [
        TranslocoTestingModule.forRoot({
          langs: {},
          translocoConfig: {
            defaultLang: 'en',
            fallbackLang: 'en',
          },
        }),
      ],
      providers: [
        provideZonelessChangeDetection(),
        SessionTimeoutService,
        { provide: AuthService, useValue: mockAuthService },
        { provide: MessageService, useValue: mockMessageService },
        { provide: NgZone, useValue: mockNgZone },
        { provide: Router, useValue: mockRouter }
      ]
    });
  });

  afterEach(() => {
    if (service) {
      service.ngOnDestroy();
    }
    intervalProvider.delegate = undefined;
    timeoutProvider.delegate = undefined;
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('should be created', () => {
    service = TestBed.inject(SessionTimeoutService);
    expect(service).toBeTruthy();
  });

  it('should show warning when warning threshold is reached', () => {
    service = TestBed.inject(SessionTimeoutService);
    (service as any).stopTimeout();
    (service as any).initTimeout();

    vi.advanceTimersByTime(841000);

    expect(mockMessageService.add).toHaveBeenCalled();
    const callArgs = mockMessageService.add.mock.calls[0][0];
    expect(callArgs.severity).toBe('warn');
    expect(callArgs.summary).toContain('layout.session_expiring');
  });

  it('should clear warning and reset timer on user activity', () => {
    service = TestBed.inject(SessionTimeoutService);
    (service as any).stopTimeout();
    (service as any).initTimeout();

    vi.advanceTimersByTime(841000); // Reaches warning
    expect(mockMessageService.add).toHaveBeenCalledTimes(1);

    // Simulate user activity
    window.dispatchEvent(new KeyboardEvent('keydown'));
    vi.advanceTimersByTime(1001); // Advance past throttleTime(1000) so activity event passes through

    expect(mockMessageService.clear).toHaveBeenCalledWith('system');

    // Another 14m should trigger warning again
    vi.advanceTimersByTime(841000);
    expect(mockMessageService.add).toHaveBeenCalledTimes(2);
  });

  it('should logout when max idle time is reached', () => {
    service = TestBed.inject(SessionTimeoutService);
    (service as any).stopTimeout();
    (service as any).initTimeout();

    vi.advanceTimersByTime(841000); // Reaches warning
    vi.advanceTimersByTime(60000);  // Reaches max idle time (total 901s)

    expect(mockAuthService.logout).toHaveBeenCalledWith('timeout');
    expect(mockMessageService.clear).toHaveBeenCalledWith('system');
  });

  it('should do nothing if user is not authenticated', () => {
    mockAuthService.isAuthenticated.mockReturnValue(false);
    service = TestBed.inject(SessionTimeoutService);
    (service as any).stopTimeout();
    (service as any).initTimeout();

    vi.advanceTimersByTime(901000); // Reaches max idle time

    expect(mockMessageService.add).not.toHaveBeenCalled();
    expect(mockAuthService.logout).not.toHaveBeenCalled();
  });
});
