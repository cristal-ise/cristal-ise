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
  itemData = input.required<ItemSummary>();

  coreMetadata = computed(() => [
    { label: 'Name', value: this.itemData().name },
    { label: 'UUID', value: this.itemData().uuid },
    { label: 'Type', value: this.itemData().type || 'N/A' },
    { label: 'Agent', value: this.itemData().isAgent, isAgent: true },
    { label: 'Domain Paths', value: this.itemData().domainPaths.join(', ') },
  ]);

  relatedReferences = computed(() => [
    { label: 'Workflow', value: this.itemData().workflow, isLink: true },
    { label: 'History', value: this.itemData().history, isLink: true },
    { label: 'Outcome', value: this.itemData().outcome, isLink: true },
    { label: 'Attachment', value: this.itemData().attachment, isLink: true },
    { label: 'Job', value: this.itemData().job, isLink: true },
  ]);
}
