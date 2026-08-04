import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { CommonModule, JsonPipe } from '@angular/common';
import { FieldsetModule } from 'primeng/fieldset';
import { OutcomeTable } from '../outcome-table/outcome-table';
import { OutcomeTabs } from '../outcome-tabs/outcome-tabs';
import DynamicOutcomeViewConfig, { OutcomeComponentType } from '../dynamic-outcome-view-config';
import { PanelModule } from 'primeng/panel';

@Component({
  selector: 'app-outcome-field-set',
  imports: [CommonModule, FieldsetModule, OutcomeTable, forwardRef(() => OutcomeTabs), JsonPipe, PanelModule],
  templateUrl: './outcome-field-set.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OutcomeFieldSet {
  label = input.required<string>();
  value = input.required<any>();
  collapsed = input<boolean>(false);
  catalog = input.required<DynamicOutcomeViewConfig>();
  path = input.required<string>();

  protected readonly OutcomeComponentType = OutcomeComponentType;

  getComponentType(key?: string): OutcomeComponentType {
    const currentPath = key ? `${this.path()}.${key}` : this.path();
    return this.catalog().getComponentType(currentPath);
  }

  objectEntries(val: any) {
    if (!val) return [];
    return Object.entries(val).map(([key, value]) => ({ key, value }));
  }
}
