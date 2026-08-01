import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { FieldsetModule } from 'primeng/fieldset';
import { OutcomeFieldSet } from '../outcome-field-set/outcome-field-set';
import DynamicOutcomeViewConfig from '../dynamic-outcome-view-config';

@Component({
  selector: 'app-outcome-tabs',
  imports: [CommonModule, TabsModule, FieldsetModule, forwardRef(() => OutcomeFieldSet)],
  templateUrl: './outcome-tabs.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OutcomeTabs {
  private fieldsForLabel: string[] = ['name', 'id', 'key', 'label',];

  label = input.required<string>();
  value = input.required<any[]>();
  catalog = input.required<DynamicOutcomeViewConfig>();
  path = input.required<string>();
  collapsed = input<boolean>(false);

  getTabLabel(item: any, index: number) {
    const keys = Object.keys(item);

    for (let labelField of this.fieldsForLabel) {
      const keyFound = keys.find((k) => (k.toLowerCase() === labelField && item[k]));

      if (keyFound) {
        const value = item[keyFound] as string;

        if (labelField === 'id') return `${keyFound}:${value}`;
        else return value;
      }
    }

    return `${this.label()} #${index + 1}`;
  }

  objectEntries(item: any) {
    return Object.entries(item).map(([key, value]) => ({ key, value }));
  }
}
