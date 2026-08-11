// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { DomainService } from '../services/domain.service';
import { ItemDataDirective } from './item-data.directive';
import { of } from 'rxjs';
import { provideZonelessChangeDetection } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
  standalone: true,
  imports: [ItemDataDirective],
  template: `<span [itemData]="value"></span>`
})
class TestWrapperComponent {
  value: string | undefined;
}

describe('ItemDataDirective', () => {
  let mockDomainService: any;
  let mockTranslocoService: any;
  let mockRouter: any;

  beforeEach(async () => {
    mockDomainService = {
      resolveUuid: vi.fn()
    };
    mockTranslocoService = {
      translate: vi.fn((key: string) => key)
    };
    mockRouter = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [TestWrapperComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: DomainService, useValue: mockDomainService },
        { provide: TranslocoService, useValue: mockTranslocoService },
        { provide: Router, useValue: mockRouter },
      ]
    }).compileComponents();
  });

  it('should resolve and display UUID as name', async () => {
    const uuid = '123e4567-e89b-12d3-a456-426614174000';
    mockDomainService.resolveUuid.mockReturnValue(of('Resolved Name'));

    const fixture = TestBed.createComponent(TestWrapperComponent);
    fixture.componentInstance.value = uuid;
    fixture.detectChanges();

    const directiveElement = fixture.debugElement.query(By.directive(ItemDataDirective));

    expect(mockDomainService.resolveUuid).toHaveBeenCalledWith(uuid);
    expect(directiveElement.nativeElement.textContent).toBe('Resolved Name');
    expect(directiveElement.nativeElement.style.cursor).toBe('pointer');
  });

  it('should display non-UUID text directly', async () => {
    const text = 'SYSTEM';

    const fixture = TestBed.createComponent(TestWrapperComponent);
    fixture.componentInstance.value = text;
    fixture.detectChanges();

    const directiveElement = fixture.debugElement.query(By.directive(ItemDataDirective));

    expect(directiveElement.nativeElement.textContent).toBe('SYSTEM');
    expect(directiveElement.nativeElement.style.cursor).toBe('default');
  });
});
