import { ChangeDetectionStrategy, Component, inject, input, signal, computed, effect, untracked, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { MultiSelectModule } from 'primeng/multiselect';
import { ItemListService } from '../../core/services/item-list.service';
import { BasicItemListResultItem } from '../../core/models/basic-item-list-result';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { switchMap, map, combineLatest } from 'rxjs';
import { SearchTextService } from '../../core/services/search-text.service';

@Component({
  selector: 'app-basic-item-list',
  standalone: true,
  imports: [CommonModule, TableModule, MultiSelectModule, FormsModule],
  templateUrl: './basic-item-list.html',
  styleUrl: './basic-item-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemList implements OnDestroy {
  domainPath = input.required<string>();
  displayPath = computed(() => this.domainPath().replace(/^\/domain/, ''));

  private itemListService = inject(ItemListService);
  private searchTextService = inject(SearchTextService);

  offset = signal(0);
  limit = signal(10);

  cols = [
    { field: 'Name', header: 'Name' },
    { field: 'Type', header: 'Type' },
    { field: 'Module', header: 'Module' },
    { field: 'UUID', header: 'UUID' },
    { field: 'Version', header: 'Version' },
  ];

  selectedColumns = signal([this.cols[0], this.cols[1], this.cols[2], this.cols[3]]);

  constructor() {
    effect(() => {
      this.domainPath();
      this.searchTextService.searchText();
      untracked(() => {
        this.offset.set(0);
      });
    });
  }

  ngOnDestroy() {
    this.searchTextService.clear();
  }

  items = toSignal(
    combineLatest([
      toObservable(this.domainPath),
      toObservable(this.offset),
      toObservable(this.limit),
      toObservable(this.searchTextService.searchText),
    ]).pipe(
      switchMap(([path, first, rows, search]) =>
        this.itemListService.getBasicItemList(path, search, first, rows),
      ),
      map((result) => result.BasicItemList.Item),
    ),
    { initialValue: [] as BasicItemListResultItem[] },
  );

  totalRecords = computed(() => {
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
}
