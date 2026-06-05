import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TranslocoPipe } from '@jsverse/transloco';
import { ItemDataDirective } from '../../../core/directives/item-data.directive';
import { DefaultService, EventData } from '../../../api';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'basic-item-history',
  imports: [CommonModule, TableModule, TranslocoPipe, ItemDataDirective, ButtonModule],
  templateUrl: './basic-item-history.html',
  styleUrl: './basic-item-history.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemHistory {
  uuid = input.required<string>();

  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);
  private router = inject(Router);

  historyItems = toSignal(
    toObservable(this.uuid).pipe(
      switchMap((uuid) =>
        this.defaultService.itemUuidHistoryGet({ uuid }).pipe(
          catchError((error: HttpErrorResponse) => {
            this.apiErrorService.handleError(error);
            return of([] as EventData[]);
          }),
        ),
      ),
    ),
    { initialValue: [] as EventData[] },
  );

  columns = [
    { field: 'timestamp', header: 'history.timestamp' },
    { field: 'agent', header: 'history.agent' },
    { field: 'role', header: 'history.role' },
    { field: 'activity', header: 'history.activity' },
    { field: 'transition', header: 'history.transition' },
    { field: 'schema', header: 'history.schema' },
    { field: 'details', header: 'history.details' },
  ];

  getTransitionName(transition: any): string {
    if (typeof transition === 'string') {
      return transition;
    }
    return transition?.name || 'history.na';
  }

  navigateToOutcomeView(event: EventData) {
    this.router.navigate(['/admin/items', this.uuid(), 'history', event.id, 'data']);
  }
}
