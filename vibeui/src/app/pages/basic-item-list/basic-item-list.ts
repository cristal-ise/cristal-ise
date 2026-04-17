import { ChangeDetectionStrategy, Component, inject, input, signal, computed, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { MultiSelectModule } from 'primeng/multiselect';
import { ItemListService } from '../../core/services/item-list.service';
import { BasicItemListResultItem } from '../../core/models/basic-item-list-result';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { switchMap, map, combineLatest } from 'rxjs';

@Component({
  selector: 'app-basic-item-list',
  standalone: true,
  imports: [CommonModule, TableModule, MultiSelectModule, FormsModule],
  templateUrl: './basic-item-list.html',
  styleUrl: './basic-item-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemList {
  domainPath = input.required<string>();

  private itemListService = inject(ItemListService);

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
      untracked(() => {
        this.offset.set(0);
      });
    });
  }

  items = toSignal(
    combineLatest([
      toObservable(this.domainPath),
      toObservable(this.offset),
      toObservable(this.limit),
    ]).pipe(
      switchMap(([path, first, rows]) =>
        this.itemListService.getBasicItemList(path, '', first, rows),
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
