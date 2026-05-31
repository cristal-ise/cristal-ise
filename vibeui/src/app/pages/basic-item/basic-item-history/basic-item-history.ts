import { ChangeDetectionStrategy, Component, inject, input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TranslocoPipe } from '@jsverse/transloco';
import { DefaultService, EventData } from '../../../api';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from '../../../core/services/api-error.service';

@Component({
  selector: 'basic-item-history',
  imports: [CommonModule, TableModule, TranslocoPipe],
  templateUrl: './basic-item-history.html',
  styleUrl: './basic-item-history.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicItemHistory {
  uuid = input.required<string>();

  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);

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
    { field: 'activity.name', header: 'history.activity' },
    { field: 'transition', header: 'history.transition' },
    { field: 'outcome.schema', header: 'history.outcome' },
  ];

  getTransitionName(transition: any): string {
    if (typeof transition === 'string') {
      return transition;
    }
    return transition?.name || 'history.na';
  }
}
