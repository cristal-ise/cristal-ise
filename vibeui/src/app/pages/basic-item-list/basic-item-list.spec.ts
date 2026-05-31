import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { BasicItemList } from './basic-item-list';
import { ItemListService } from '../../core/services/item-list.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';

describe('BasicItemList', () => {
  let component: BasicItemList;
  let fixture: ComponentFixture<BasicItemList>;
  let itemListServiceMock: any;
  let routerMock: any;

  const mockItems = [
    { UUID: '1', Module: 'm1', Type: 't1', Name: 'n1', Version: '1.0', TotalCount: 1 }
  ];

  beforeEach(async () => {
    itemListServiceMock = {
      getBasicItemList: vi.fn().mockReturnValue(of({
        BasicItemList: {
          Item: mockItems
        }
      }))
    };

    routerMock = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        BasicItemList,
        TranslocoTestingModule.forRoot({
          langs: {},
          translocoConfig: {
            defaultLang: 'en',
            fallbackLang: 'en',
          },
        }),
      ],
      providers: [
        { provide: ItemListService, useValue: itemListServiceMock },
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

    fixture = TestBed.createComponent(BasicItemList);
    component = fixture.componentInstance;

    // Set required input
    fixture.componentRef.setInput('domainPath', 'test/path');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call ItemListService with domainPath and default paging', () => {
    expect(itemListServiceMock.getBasicItemList).toHaveBeenCalledWith('test/path', '', 0, 10);
  });

  it('should have items from service in the table', () => {
    expect(component.items().length).toBe(1);
    expect(component.items()[0].UUID).toBe('1');
  });

  it('should update paging parameters and call service when onLazyLoad is called', async () => {
    component.onLazyLoad({ first: 20, rows: 10 });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.offset()).toBe(20);
    expect(component.limit()).toBe(10);
    expect(itemListServiceMock.getBasicItemList).toHaveBeenCalledWith('test/path', '', 20, 10);
  });

  it('should reset paging when domainPath changes', async () => {
    // Set some paging first
    component.onLazyLoad({ first: 20, rows: 10 });
    fixture.detectChanges();
    await fixture.whenStable();

    // Change domainPath
    fixture.componentRef.setInput('domainPath', 'new/path');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.offset()).toBe(0);
    expect(itemListServiceMock.getBasicItemList).toHaveBeenCalledWith('new/path', '', 0, 10);
  });

  it('should compute totalRecords from items', () => {
    expect(component.totalRecords()).toBe(1);
  });

  it('should initialize with required columns only', () => {
    const selectedFields = component.selectedColumns().map(c => c.field);
    expect(selectedFields).toContain('Type');
    expect(selectedFields).toContain('Module');
    expect(selectedFields).toContain('Name');
    expect(selectedFields).not.toContain('UUID');
    expect(selectedFields).not.toContain('Version');
    expect(selectedFields).not.toContain('TotalCount');
  });

  it('should update selected columns', () => {
    const allCols = component.columns;
    component.selectedColumns.set(allCols);
    expect(component.selectedColumns().length).toBe(allCols.length);
  });

  it('should navigate to item details when viewDetails is called', () => {
    component.viewDetails('test-uuid-123');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/admin/items', 'test-uuid-123']);
  });
});
