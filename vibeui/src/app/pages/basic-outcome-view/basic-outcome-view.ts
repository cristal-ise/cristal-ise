import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { CommonModule, JsonPipe } from '@angular/common';
import { DefaultService } from '../../api';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { switchMap, of, catchError, combineLatest } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from '../../core/services/api-error.service';

@Component({
  selector: 'app-basic-outcome-view',
  imports: [CommonModule, JsonPipe],
  templateUrl: './basic-outcome-view.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BasicOutcomeView {
  uuid = input.required<string>();
  eventId = input.required<string>();

  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);

  data = toSignal(
    combineLatest([toObservable(this.uuid), toObservable(this.eventId)]).pipe(
      switchMap(([uuid, eventId]) =>
        this.defaultService.itemUuidHistoryEventIdDataGet({ uuid: uuid, eventId: eventId }).pipe(
          catchError((error: HttpErrorResponse) => {
            this.apiErrorService.handleError(error);
            return of(null);
          }),
        ),
      ),
    ),
  );
}
