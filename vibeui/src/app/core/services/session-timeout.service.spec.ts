import { TestBed } from '@angular/core/testing';
import { SessionTimeoutService } from './session-timeout.service';
import { AuthService } from './auth.service';
import { MessageService } from 'primeng/api';
import { NgZone, provideZonelessChangeDetection, EventEmitter } from '@angular/core';
import { of } from 'rxjs';
import { intervalProvider } from 'rxjs/internal/scheduler/intervalProvider';

describe('SessionTimeoutService', () => {
  let service: SessionTimeoutService;
  let mockAuthService: any;
  let mockMessageService: any;

  beforeEach(() => {
    vi.useFakeTimers();

    // Bridge RxJS's asyncScheduler to Vitest's fake timers.
    // In the jsdom test environment vi.useFakeTimers() patches globalThis.setInterval
    // (jsdom's window), but RxJS's CJS module falls back to the Node.js-native setInterval.
    // Setting intervalProvider.delegate forces RxJS to use globalThis.setInterval (the
    // faked one) so that vi.advanceTimersByTime() triggers RxJS timer callbacks.
    intervalProvider.delegate = {
      setInterval: (fn: any, delay?: any, ...args: any[]) =>
        (globalThis.setInterval as any)(fn, delay, ...args),
      clearInterval: (id: any) => (globalThis.clearInterval as any)(id),
    };

    mockAuthService = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      logout: vi.fn().mockReturnValue(of(null as any))
    };
    mockMessageService = {
      add: vi.fn(),
      clear: vi.fn()
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
      providers: [
        provideZonelessChangeDetection(),
        SessionTimeoutService,
        { provide: AuthService, useValue: mockAuthService },
        { provide: MessageService, useValue: mockMessageService },
        { provide: NgZone, useValue: mockNgZone }
      ]
    });
  });

  afterEach(() => {
    if (service) {
      service.ngOnDestroy();
    }
    intervalProvider.delegate = undefined;
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('should be created', () => {
    service = TestBed.inject(SessionTimeoutService);
    expect(service).toBeTruthy();
  });

  it('should show warning when warning threshold is reached', () => {
    service = TestBed.inject(SessionTimeoutService);

    vi.advanceTimersByTime(30000);

    expect(mockMessageService.add).toHaveBeenCalled();
    const callArgs = mockMessageService.add.mock.calls[0][0];
    expect(callArgs.severity).toBe('warn');
    expect(callArgs.summary).toBe('Session Expiring');
  });

  it('should clear warning and reset timer on user activity', () => {
    service = TestBed.inject(SessionTimeoutService);

    vi.advanceTimersByTime(30000); // Reaches warning
    expect(mockMessageService.add).toHaveBeenCalledTimes(1);

    // Simulate user activity
    window.dispatchEvent(new KeyboardEvent('keydown'));
    vi.advanceTimersByTime(1000); // Advance past throttleTime(1000) so activity event passes through

    expect(mockMessageService.clear).toHaveBeenCalledWith('system');

    // Another 30s should trigger warning again
    vi.advanceTimersByTime(30000);
    expect(mockMessageService.add).toHaveBeenCalledTimes(2);
  });

  it('should logout when max idle time is reached', () => {
    service = TestBed.inject(SessionTimeoutService);

    vi.advanceTimersByTime(30000); // Reaches warning
    vi.advanceTimersByTime(30000); // Reaches max idle time (total 60s)

    expect(mockAuthService.logout).toHaveBeenCalledWith('timeout');
    expect(mockMessageService.clear).toHaveBeenCalledWith('system');
  });

  it('should do nothing if user is not authenticated', () => {
    mockAuthService.isAuthenticated.mockReturnValue(false);
    service = TestBed.inject(SessionTimeoutService);

    vi.advanceTimersByTime(60000); // Reaches max idle time

    expect(mockMessageService.add).not.toHaveBeenCalled();
    expect(mockAuthService.logout).not.toHaveBeenCalled();
  });
});
