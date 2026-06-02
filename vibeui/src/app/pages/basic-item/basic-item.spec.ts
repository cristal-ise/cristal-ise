import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { BasicItem } from './basic-item';
import { DefaultService } from '../../api';
import { ApiErrorService } from '../../core/services/api-error.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';

describe('BasicItem', () => {
  let component: BasicItem;
  let fixture: ComponentFixture<BasicItem>;
  let defaultServiceMock: any;
  let apiErrorServiceMock: any;
  let routerMock: any;

  const mockItemSummary = {
    uuid: 'test-uuid-123',
    name: 'TestItemName',
    type: 'TestItemType',
    domainPaths: ['/domain/test/path'],
    hasMasterOutcome: true,
    isAgent: false,
    workflow: 'http://workflow-uri',
    history: 'http://history-uri',
    outcome: 'http://outcome-uri',
    attachment: 'http://attachment-uri',
    job: 'http://job-uri',
    collections: [
      { name: 'CollectionA', url: 'http://col-a' },
      { name: 'CollectionB', url: 'http://col-b' }
    ],
    properties: [
      { name: 'prop1', value: 'val1' },
      { name: 'prop2', value: 'val2' }
    ]
  };

  beforeEach(async () => {
    vi.stubGlobal('ResizeObserver', class {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    });

    defaultServiceMock = {
      itemUuidGet: vi.fn().mockReturnValue(of(mockItemSummary))
    };

    apiErrorServiceMock = {
      handleError: vi.fn()
    };

    routerMock = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        BasicItem,
        TranslocoTestingModule.forRoot({
          langs: {},
          translocoConfig: {
            defaultLang: 'en',
            fallbackLang: 'en',
          },
        }),
      ],
      providers: [
        { provide: DefaultService, useValue: defaultServiceMock },
        { provide: ApiErrorService, useValue: apiErrorServiceMock },
        { provide: Router, useValue: routerMock },
        provideHttpClient(),
        provideNoopAnimations(),
        providePrimeNG({
          theme: {
            preset: Aura
          }
        }),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BasicItem);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('uuid', 'test-uuid-123');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call defaultService.itemUuidGet with uuid', () => {
    expect(defaultServiceMock.itemUuidGet).toHaveBeenCalledWith({ uuid: 'test-uuid-123' });
  });

  it('should retrieve collections dynamic properties', () => {
    expect(component.collections().length).toBe(2);
    expect(component.collections()[0].name).toBe('CollectionA');
  });

  it('should initialize with the Details tab selected by default', () => {
    const tabsElement = fixture.debugElement.query(By.css('p-tabs'));
    expect(tabsElement).toBeTruthy();
    expect(tabsElement.componentInstance.value()).toBe('details');
  });

  it('should handle error using ApiErrorService when API fails', async () => {
    const errorResponse = new HttpErrorResponse({ status: 500, statusText: 'Server Error' });
    defaultServiceMock.itemUuidGet.mockReturnValue(throwError(() => errorResponse));

    fixture = TestBed.createComponent(BasicItem);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('uuid', 'error-uuid');

    fixture.detectChanges();
    await fixture.whenStable();

    expect(apiErrorServiceMock.handleError).toHaveBeenCalledWith(errorResponse);
    expect(component.item()).toBeNull();
  });
});
