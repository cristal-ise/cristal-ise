import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { FieldsetModule } from 'primeng/fieldset';

@Component({
  selector: 'app-outcome-table',
  imports: [CommonModule, TableModule, FieldsetModule],
  templateUrl: './outcome-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OutcomeTable {
  label = input.required<string>();
  value = input.required<any[]>();
  collapsed = input<boolean>(false);

  getColumns(val: any[]): string[] {
    if (!val || val.length === 0) return [];
    return Object.keys(val[0]);
  }
}
