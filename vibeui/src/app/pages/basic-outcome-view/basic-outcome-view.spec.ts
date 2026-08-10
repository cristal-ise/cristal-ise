import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BasicOutcomeView } from './basic-outcome-view';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { DefaultService } from '../../api';
import { ApiErrorService } from '../../core/services/api-error.service';
import { of } from 'rxjs';

describe('BasicOutcomeView', () => {
  let component: BasicOutcomeView;
  let fixture: ComponentFixture<BasicOutcomeView>;
  let mockDefaultService: any;
  let mockApiErrorService: any;

  beforeEach(async () => {
    mockDefaultService = {
      itemUuidHistoryEventIdDataGet: vi.fn()
    };
    mockApiErrorService = {
      handleError: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [BasicOutcomeView],
      providers: [
        { provide: DefaultService, useValue: mockDefaultService },
        { provide: ApiErrorService, useValue: mockApiErrorService },
        providePrimeNG({
          theme: {
            preset: Aura
          }
        })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BasicOutcomeView);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should identify primitive only data', async () => {
    const primitiveData = { "Root": { "name": "test", "age": 30 } };
    mockDefaultService.itemUuidHistoryEventIdDataGet.mockReturnValue(of(primitiveData));

    fixture.componentRef.setInput('uuid', '123');
    fixture.componentRef.setInput('eventId', '456');
    fixture.detectChanges();

    // Wait for schema generation (async)
    await new Promise(resolve => setTimeout(resolve, 500));
    fixture.detectChanges();

    expect(component.isDisplayable()).toBe(true);
    expect(component.rootName()).toBe('Root');
    expect(component.rootJson()).toEqual({ "name": "test", "age": 30 });

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-outcome-field-set')).toBeTruthy();
    expect(compiled.textContent).toContain('name');
    expect(compiled.textContent).toContain('test');
  });

  it('should identify complex data (nested object) as displayable', async () => {
    const complexData = { "Root": { "name": "test", "details": { "city": "NY" } } };
    mockDefaultService.itemUuidHistoryEventIdDataGet.mockReturnValue(of(complexData));

    fixture.componentRef.setInput('uuid', '123');
    fixture.componentRef.setInput('eventId', '456');
    fixture.detectChanges();

    // Wait for schema generation (async)
    await new Promise(resolve => setTimeout(resolve, 500));
    fixture.detectChanges();

    expect(component.isDisplayable()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-outcome-field-set')).toBeTruthy();
    expect(compiled.textContent).toContain('details');
    expect(compiled.textContent).toContain('city');
    expect(compiled.textContent).toContain('NY');
  });

  it('should handle deeply nested objects displayable', async () => {
    const deepData = { "Root": { "level1": { "level2": { "value": "deep" } } } };
    mockDefaultService.itemUuidHistoryEventIdDataGet.mockReturnValue(of(deepData));

    fixture.componentRef.setInput('uuid', '123');
    fixture.componentRef.setInput('eventId', '456');
    fixture.detectChanges();

    // Wait for schema generation (async)
    await new Promise(resolve => setTimeout(resolve, 500));
    fixture.detectChanges();

    expect(component.isDisplayable()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('level1');
    expect(compiled.textContent).toContain('level2');
    expect(compiled.textContent).toContain('value');
    expect(compiled.textContent).toContain('deep');

    const fieldsets = compiled.querySelectorAll('p-fieldset');
    expect(fieldsets.length).toBe(3); // Root, level1, level2
  });

  it('should identify complex data (primitive array) as displayable', async () => {
    const complexData = { "Root": { "name": "test", "tags": ["a", "b"] } };
    mockDefaultService.itemUuidHistoryEventIdDataGet.mockReturnValue(of(complexData));

    fixture.componentRef.setInput('uuid', '123');
    fixture.componentRef.setInput('eventId', '456');
    fixture.detectChanges();

    // Wait for schema generation (async)
    await new Promise(resolve => setTimeout(resolve, 500));
    fixture.detectChanges();

    expect(component.isDisplayable()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-outcome-field-set')).toBeTruthy();
    expect(compiled.textContent).toContain('tags');
    expect(compiled.textContent).toContain('a');
    expect(compiled.textContent).toContain('b');
  });

  it('should identify root-level primitive array as displayable', async () => {
    const arrayData = { "Root": ["a", "b"] };
    mockDefaultService.itemUuidHistoryEventIdDataGet.mockReturnValue(of(arrayData));

    fixture.componentRef.setInput('uuid', '123');
    fixture.componentRef.setInput('eventId', '456');
    fixture.detectChanges();

    // Wait for schema generation (async)
    await new Promise(resolve => setTimeout(resolve, 500));
    fixture.detectChanges();

    expect(component.isDisplayable()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-outcome-field-set')).toBeTruthy();
    expect(compiled.textContent).toContain('Root');
    expect(compiled.textContent).toContain('a');
    expect(compiled.textContent).toContain('b');
  });
});
