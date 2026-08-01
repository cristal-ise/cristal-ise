import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OutcomeTabs } from './outcome-tabs';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import DynamicOutcomeViewConfig from '../dynamic-outcome-view-config';

describe('OutcomeTabs', () => {
  let component: OutcomeTabs;
  let fixture: ComponentFixture<OutcomeTabs>;

  beforeEach(async () => {
    global.ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    } as any;

    await TestBed.configureTestingModule({
      imports: [OutcomeTabs],
      providers: [
        providePrimeNG({
          theme: {
            preset: Aura
          }
        })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OutcomeTabs);
    component = fixture.componentInstance;
  });

  it('should return name if present (lowercase)', () => {
    const item = { name: 'Test Name' };
    fixture.componentRef.setInput('label', 'Items');
    fixture.componentRef.setInput('value', [item]);
    fixture.componentRef.setInput('catalog', new DynamicOutcomeViewConfig());
    fixture.componentRef.setInput('path', 'root');
    fixture.detectChanges();

    expect(component.getTabLabel(item, 0)).toBe('Test Name');
  });

  it('should return Name if present (capitalized)', () => {
    const item = { Name: 'Capital Name' };
    fixture.componentRef.setInput('label', 'Items');
    fixture.componentRef.setInput('value', [item]);
    fixture.componentRef.setInput('catalog', new DynamicOutcomeViewConfig());
    fixture.componentRef.setInput('path', 'root');
    fixture.detectChanges();

    // Current implementation will fail this if it only looks for 'name'
    expect(component.getTabLabel(item, 0)).toBe('Capital Name');
  });

  it('should return id if present (lowercase)', () => {
    const item = { id: 'test-id' };
    fixture.componentRef.setInput('label', 'Items');
    fixture.componentRef.setInput('value', [item]);
    fixture.componentRef.setInput('catalog', new DynamicOutcomeViewConfig());
    fixture.componentRef.setInput('path', 'root');
    fixture.detectChanges();

    expect(component.getTabLabel(item, 0)).toBe('id:test-id');
  });

  it('should return ID if present (uppercase)', () => {
    const item = { ID: 'UPPER-ID' };
    fixture.componentRef.setInput('label', 'Items');
    fixture.componentRef.setInput('value', [item]);
    fixture.componentRef.setInput('catalog', new DynamicOutcomeViewConfig());
    fixture.componentRef.setInput('path', 'root');
    fixture.detectChanges();

    expect(component.getTabLabel(item, 0)).toBe('ID:UPPER-ID');
  });

  it('should return label if present', () => {
    const item = { label: 'Test Label' };
    fixture.componentRef.setInput('label', 'Items');
    fixture.componentRef.setInput('value', [item]);
    fixture.componentRef.setInput('catalog', new DynamicOutcomeViewConfig());
    fixture.componentRef.setInput('path', 'root');
    fixture.detectChanges();

    expect(component.getTabLabel(item, 0)).toBe('Test Label');
  });

  it('should return default label using component label() when no specific keys found', () => {
    const item = { other: 'value' };
    fixture.componentRef.setInput('label', 'MyCategory');
    fixture.componentRef.setInput('value', [item]);
    fixture.componentRef.setInput('catalog', new DynamicOutcomeViewConfig());
    fixture.componentRef.setInput('path', 'root');
    fixture.detectChanges();

    // Should be "MyCategory 1" instead of "Item 1"
    expect(component.getTabLabel(item, 0)).toBe('MyCategory #1');
  });
});
