import { ChangeDetectionStrategy, Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ItemSummary } from '../../../api';

@Component({
  selector: 'basic-item-details',
  imports: [CommonModule, TableModule, CardModule],
  templateUrl: './basic-item-details.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemDetails {
  itemSummary = input.required<ItemSummary>();

  coreMetadata = computed(() => [
    { label: 'Name', value: this.itemSummary().name },
    { label: 'UUID', value: this.itemSummary().uuid },
    { label: 'Type', value: this.itemSummary().type || 'N/A' },
    { label: 'Agent', value: this.itemSummary().isAgent, isAgent: true },
    { label: 'Domain Paths', value: this.itemSummary().domainPaths.join(', ') },
  ]);

  relatedReferences = computed(() => [
    { label: 'Workflow', value: this.itemSummary().workflow, isLink: true },
    { label: 'History', value: this.itemSummary().history, isLink: true },
    { label: 'Outcome', value: this.itemSummary().outcome, isLink: true },
    { label: 'Attachment', value: this.itemSummary().attachment, isLink: true },
    { label: 'Job', value: this.itemSummary().job, isLink: true },
  ]);
}
