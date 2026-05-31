import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { Configuration } from '../../api';
import { MessageService } from 'primeng/api';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { DomainService } from '../../core/services/domain.service';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { signal } from '@angular/core';

import { AdminLayout } from './admin-layout';

describe('AdminLayout', () => {
  let component: AdminLayout;
  let fixture: ComponentFixture<AdminLayout>;

  beforeEach(async () => {
    const mockDomainService = {
      getTreeData: vi.fn().mockReturnValue(of([])),
      transformToTreeNodeItems: vi.fn().mockReturnValue([])
    };

    const mockAuthService = {
      isAuthenticated: signal(true),
      checkSession: vi.fn().mockReturnValue(of(true)),
      logout: vi.fn().mockReturnValue(of(null))
    };

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        Configuration,
        MessageService,
        { provide: DomainService, useValue: mockDomainService },
        { provide: AuthService, useValue: mockAuthService }
      ],
      imports: [
        AdminLayout,
        TranslocoTestingModule.forRoot({
          langs: {},
          translocoConfig: {
            defaultLang: 'en',
            fallbackLang: 'en',
          },
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
