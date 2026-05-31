import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { Configuration } from '../../../api';
import { MessageService } from 'primeng/api';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { DomainService } from '../../../core/services/domain.service';
import { of } from 'rxjs';

import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  let component: Sidebar;
  let fixture: ComponentFixture<Sidebar>;

  beforeEach(async () => {
    const mockDomainService = {
      getTreeData: vi.fn().mockReturnValue(of([])),
      transformToTreeNodeItems: vi.fn().mockReturnValue([])
    };

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        Configuration,
        MessageService,
        { provide: DomainService, useValue: mockDomainService }
      ],
      imports: [
        Sidebar,
        TranslocoTestingModule.forRoot({
          langs: {},
          translocoConfig: {
            defaultLang: 'en',
            fallbackLang: 'en',
          },
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Sidebar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
