import { ChangeDetectionStrategy, Component, inject, input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { ItemDataDirective } from '../../core/directives/item-data.directive';
import { BasicItemDetails } from './basic-item-details/basic-item-details';
import { BasicItemHistory } from './basic-item-history/basic-item-history';
import { BasicItemData } from './basic-item-data/basic-item-data';
import { DefaultService } from '../../api';
import { ItemSummary } from '../../api';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from '../../core/services/api-error.service';

@Component({
  selector: 'app-basic-item',
  imports: [CommonModule, TabsModule, ButtonModule, TranslocoPipe, ItemDataDirective, BasicItemDetails, BasicItemHistory, BasicItemData],
  templateUrl: './basic-item.html',
  styleUrl: './basic-item.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItem {
  uuid = input.required<string>();
  activeTab = signal<any>('details');

  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);

  itemSummary$ = toObservable(this.uuid)
    .pipe(
      switchMap(uuid => this.defaultService.itemUuidGet({ uuid })
        .pipe(catchError((error: HttpErrorResponse) => {
          this.apiErrorService.handleError(error);
          return of(null);
        })
      ))
    );

  item = toSignal(this.itemSummary$, { initialValue: null as ItemSummary | null });

  // dynamic tabs from collections
  collections = computed(() => {
    return this.item()?.collections || [];
  });
}
