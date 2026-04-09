import { Injectable, inject } from '@angular/core';
import { DefaultService } from '../../api';
import { PathData } from '../../api/model/pathData';
import { Observable, map } from 'rxjs';
import { TreeNode } from 'primeng/api';

@Injectable({
  providedIn: 'root'
})
export class DomainService {
  private defaultService = inject(DefaultService);

  /**
   * Fetches tree-like data structures from the domain endpoint.
   * Calls /domain?search=tree
   */
  getTreeData(): Observable<PathData[]> {
    return this.defaultService.domainGet({ search: 'tree' }).pipe(
      map(data => data.rows)
    );
  }

  transformToTreeNodeItems(rows: PathData[]): TreeNode[] {
    // Sort by path string as requested
    const sortedRows = [...rows].sort((a, b) => a.path.localeCompare(b.path));

    const root: TreeNode[] = [];
    const pathMap = new Map<string, TreeNode>();

    sortedRows.forEach((row) => {
      let parts = row.path.split('/').filter((p) => !!p);
      if (parts.length > 0 && parts[0] === 'domain') {
        parts.shift();
      }
      if (parts.length === 0) return;

      let currentPath = '';

      parts.forEach((part, index) => {
        const parentPath = currentPath;
        currentPath = currentPath ? `${currentPath}/${part}` : part;

        if (!pathMap.has(currentPath)) {
          const isLeaf = index === parts.length - 1;
          const newItem = {
            label: part,
            expanded: false,
            theicon: this.getIconForType(isLeaf ? row.type : undefined),
            routerLink: null,
            children: isLeaf ? undefined : [],
          } as TreeNode;

          if (isLeaf && row.uuid) {
            newItem.data = { uuid: row.uuid };
          }

          pathMap.set(currentPath, newItem);

          if (parentPath === '') {
            root.push(newItem);
          } else {
            const parentItem = pathMap.get(parentPath);
            if (parentItem) {
              if (!parentItem.children) parentItem.children = [];
              parentItem.children!.push(newItem);
            }
          }
        }
      });
    });

    return root;
  }

  private getIconForType(type?: PathData.TypeEnum): string {
    switch (type) {
      case 'domain':
        return 'pi pi-folder';
      case 'item':
        return 'pi pi-file';
      case 'agent':
        return 'pi pi-user';
      case 'role':
        return 'pi pi-shield';
      default:
        return 'pi pi-folder';
    }
  }
}
