import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TreeTableModule } from 'primeng/treetable';
import { TranslocoPipe } from '@jsverse/transloco';
import { ItemDataDirective } from '../../../core/directives/item-data.directive';
import { ItemOutcomeService } from '../../../core/services/item-outcome.service';
import { TreeNode } from 'primeng/api';
import { Router } from '@angular/router';
import { ButtonModule, Button } from 'primeng/button';

@Component({
  selector: 'basic-item-data',
  imports: [CommonModule, TreeTableModule, TranslocoPipe, ItemDataDirective, Button],
  templateUrl: './basic-item-data.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemData {
  uuid = input.required<string>();


  nodes = signal<TreeNode[]>([]);
  totalRecords = signal<number>(0);
  loading = signal<boolean>(false);
  rows = signal<number>(10);

  private itemOutcomeService = inject(ItemOutcomeService);
  private router = inject(Router);

  loadNodes(event: any) {
    this.loading.set(true);
    const offset = event.first ?? 0;
    const limit = event.rows ?? 10;

    this.itemOutcomeService.getOutcomeSchemaCountData(this.uuid(), offset, limit).subscribe({
      next: (res) => {
        const records = res?.OutcomeSchema?.Record || [];
        this.nodes.set(
          records.map((record) => ({
            data: {
              name: record.SCHEMA_NAME,
              count: record.count,
              isLeaf: false,
            },
            leaf: false,
            children: [],
          }))
        );
        this.totalRecords.set(records.length);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onNodeExpand(event: any) {
    const node = event.node;
    if (node.children && node.children.length > 0) {
      return; // Already loaded children
    }
    node.loading = true;
    const schema = node.data.name;

    this.itemOutcomeService.getOutcomeSchemaViewpointData(this.uuid(), schema, 0, 100).subscribe({
      next: (res) => {
        const records = res?.OutcomeWithViewpoint?.Record || [];
        node.children = records.map((r) => ({
          data: {
            name: '',
            count: '',
            version: r.SCHEMA_VERSION,
            eventId: r.EVENT_ID,
            viewpoint: r.VIEWPOINT,
            agent: r.AGENT_UUID,
            timestamp: r.TIMESTAMP,
            isLeaf: true,
          },
          leaf: true,
        }));
        node.loading = false;
        // Trigger change detection by cloning the nodes array
        this.nodes.set([...this.nodes()]);
      },
      error: () => {
        node.loading = false;
      },
    });
  }

  navigateToOutcomeView(rowData: any) {
    this.router.navigate(['/admin/items', this.uuid(), 'history', rowData.eventId, 'data']);
  }
}
