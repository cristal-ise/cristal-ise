import { ChangeDetectionStrategy, Component, inject, input, signal, computed, effect, untracked, OnDestroy, Signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';
import { Router } from '@angular/router';
import { ItemListService } from '../../core/services/item-list.service';
import { BasicItemListResultItem } from '../../core/models/basic-item-list-result';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { switchMap, map, combineLatest, catchError, of } from 'rxjs';
import { SearchTextService } from '../../core/services/search-text.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-basic-item-list',
  standalone: true,
  imports: [CommonModule, TableModule, MultiSelectModule, FormsModule, ButtonModule],
  templateUrl: './basic-item-list.html',
  styleUrl: './basic-item-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemList implements OnDestroy {
  domainPath: Signal<string> = input.required<string>();
  displayPath: Signal<string> = computed(() => this.domainPath()?.substring(this.domainPath()?.lastIndexOf('/') + 1));

  private itemListService = inject(ItemListService);
  private searchTextService = inject(SearchTextService);
  private router = inject(Router);

  offset = signal(0);
  limit = signal(10);

  columns = [
    { field: 'Name', header: 'Name' },
    { field: 'Type', header: 'Type' },
    { field: 'Module', header: 'Module' },
    { field: 'UUID', header: 'UUID' },
    { field: 'Version', header: 'Version' },
  ];

  selectedItem = signal<BasicItemListResultItem | undefined>(undefined);
  selectedColumns = signal([this.columns[0], this.columns[1], this.columns[2], this.columns[3]]);

  constructor() {
    effect(() => {
      this.domainPath();
      this.searchTextService.searchText();
      untracked(() => {
        this.offset.set(0);
        this.selectedItem.set(undefined);
      });
    });
  }

  ngOnDestroy() {
    this.searchTextService.clear();
  }

  items: Signal<BasicItemListResultItem[]> = toSignal(
    combineLatest([
      toObservable(this.domainPath),
      toObservable(this.offset),
      toObservable(this.limit),
      toObservable(this.searchTextService.searchText),
    ]).pipe(
      switchMap(([path, first, rows, search]) => this.itemListService.getBasicItemList(path, search, first, rows)),
      map((result) => result?.BasicItemList?.Item || [] as BasicItemListResultItem[]),
      catchError((error: HttpErrorResponse) => {
        // required because signal cannot be undefined 
        return of([] as BasicItemListResultItem[]);
      }),
    ),
    { initialValue: [] as BasicItemListResultItem[] },
  );

  totalRecords: Signal<number> = computed(() => {
    const currentItems = this.items();
    return currentItems.length > 0 ? currentItems[0].TotalCount : 0;
  });

  onLazyLoad(event: TableLazyLoadEvent) {
    if (event.first != null) {
      this.offset.set(event.first);
    }
    if (event.rows != null) {
      this.limit.set(event.rows);
    }
  }

  viewDetails(uuid: string) {
    this.router.navigate(['/dashboard/items', uuid]);
  }
}
