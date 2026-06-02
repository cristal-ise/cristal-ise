import { Injectable, inject } from '@angular/core';
import { DefaultService, ItemAliases } from '../../api';
import { PathData } from '../../api/model/pathData';
import { Observable, map, catchError, throwError, Subject, bufferTime, ReplaySubject, of } from 'rxjs';
import { TreeNode } from 'primeng/api';
import { ApiErrorService } from './api-error.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class DomainService {
  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);

  private uuidToNameCache = new Map<string, string>();
  private resolvedUuidsSubject = new Subject<string>();
  private pendingResolveRequests = new Map<string, ReplaySubject<string>>();

  constructor() {
    this.resolvedUuidsSubject.pipe(
      bufferTime(100)
    ).subscribe(uuids => {
      if (uuids.length === 0) return;

      const uniqueUuids = Array.from(new Set(uuids));
      this.resolveAliases(uniqueUuids).subscribe({
        next: (aliases) => {
          const aliasMap = new Map(aliases.map(a => [a.uuid, a.name]));
          uniqueUuids.forEach(uuid => {
            const name = aliasMap.get(uuid) || uuid;
            if (aliasMap.has(uuid) && aliasMap.get(uuid)) {
              this.uuidToNameCache.set(uuid, name);
            }
            const pending = this.pendingResolveRequests.get(uuid);
            if (pending) {
              pending.next(name);
              pending.complete();
              this.pendingResolveRequests.delete(uuid);
            }
          });
        },
        error: () => {
          uniqueUuids.forEach(uuid => {
            const pending = this.pendingResolveRequests.get(uuid);
            if (pending) {
              pending.next(uuid); // Fallback to UUID
              pending.complete();
              this.pendingResolveRequests.delete(uuid);
            }
          });
        }
      });
    });
  }

  /**
   * Resolves a single UUID to a name. Uses batching and caching.
   */
  resolveUuid(uuid: string): Observable<string> {
    const cachedName = this.uuidToNameCache.get(uuid);
    if (cachedName) {
      return of(cachedName);
    }

    if (!this.pendingResolveRequests.has(uuid)) {
      this.pendingResolveRequests.set(uuid, new ReplaySubject<string>(1));
      this.resolvedUuidsSubject.next(uuid);
    }

    return this.pendingResolveRequests.get(uuid)!;
  }

  /**
   * Calls the /domain/aliases endpoint to resolve multiple UUIDs.
   */
  resolveAliases(uuids: string[]): Observable<ItemAliases[]> {
    const search = JSON.stringify(uuids).replace(/"/g, "'");
    return this.defaultService.domainPathGet({ path: 'aliases', search }).pipe(
      map(data => data as ItemAliases[]),
      catchError((error: HttpErrorResponse) => {
        this.apiErrorService.handleError(error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Fetches tree-like data structures from the domain endpoint.
   * Calls /domain?search=tree
   */
  getTreeData(): Observable<PathData[]> {
    return this.defaultService.domainGet({ search: 'tree' }).pipe(
      map(data => data.rows),
      catchError((error: HttpErrorResponse) => {
        this.apiErrorService.handleError(error);
        return throwError(() => error);
      })
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
            routerLink: isLeaf ? '/admin/items' : null,
            queryParams: isLeaf ? { domainPath: row.path } : null,
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
