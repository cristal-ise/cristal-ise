import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BasicItemDetails } from './basic-item-details';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { ItemSummary } from '../../../api';

describe('BasicItemDetails', () => {
  let component: BasicItemDetails;
  let fixture: ComponentFixture<BasicItemDetails>;

  const mockItemSummary: ItemSummary = {
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
    collections: [],
    properties: [
      { name: 'prop1', value: 'val1' },
      { name: 'prop2', value: 'val2' }
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BasicItemDetails],
      providers: [
        providePrimeNG({
          theme: {
            preset: Aura
          }
        })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BasicItemDetails);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('itemData', mockItemSummary);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display item metadata', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('test-uuid-123');
    expect(compiled.textContent).toContain('TestItemName');
    expect(compiled.textContent).toContain('TestItemType');
  });

  it('should display properties in the table', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('prop1');
    expect(compiled.textContent).toContain('val1');
    expect(compiled.textContent).toContain('prop2');
    expect(compiled.textContent).toContain('val2');
  });
});
