import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { TreeModule } from 'primeng/tree';
import { TreeNode } from 'primeng/api';
import { DomainService } from '../../../core/services/domain.service';
import { Observable, map } from 'rxjs';
import { PathData } from '../../../api';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, ButtonModule, TranslocoPipe, TreeModule, AsyncPipe],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sidebar {
  private domainService = inject(DomainService);
  treeNodes$: Observable<TreeNode[]> = this.domainService
    .getTreeData()
    .pipe(
      map((rows) => {
        const domainNodes = this.domainService.transformToTreeNodeItems(rows);
        return [
          {
            label: 'layout.dashboard_label',
            expanded: false,
            theicon: 'pi pi-home',
            routerLink: '/admin',
          } as TreeNode,
          ...domainNodes,
        ];
      }),
    );
}
