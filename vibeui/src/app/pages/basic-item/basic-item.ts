import { ChangeDetectionStrategy, Component, inject, input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { BasicItemDetails } from './basic-item-details/basic-item-details';
import { BasicItemHistory } from './basic-item-history/basic-item-history';
import { DefaultService } from '../../api';
import { ItemSummary } from '../../api';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from '../../core/services/api-error.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-basic-item',
  imports: [CommonModule, TabsModule, ButtonModule, TranslocoPipe, BasicItemDetails, BasicItemHistory],
  templateUrl: './basic-item.html',
  styleUrl: './basic-item.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItem {
  uuid = input.required<string>();
  activeTab = signal<any>('details');

  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);
  private router = inject(Router);

  itemSummary$ = toObservable(this.uuid).pipe(
    switchMap(uuid => this.defaultService.itemUuidGet({ uuid }).pipe(
      catchError((error: HttpErrorResponse) => {
        this.apiErrorService.handleError(error);
        return of(null);
      })
    ))
  );

  item = toSignal(this.itemSummary$, { initialValue: null as ItemSummary | null });

  // Header format: {itemType} : {ItemName}
  headerText = computed(() => {
    const summary = this.item();
    if (!summary) return '';
    return `${summary.type || ''} : ${summary.name || ''}`;
  });

  // dynamic tabs from collections
  collections = computed(() => {
    return this.item()?.collections || [];
  });
}
