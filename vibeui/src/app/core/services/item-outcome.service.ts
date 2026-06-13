import { Injectable, inject } from '@angular/core';
import { DefaultService } from '../../api';
import { Observable, map, catchError, throwError } from 'rxjs';
import { OutcomeSchemaCountResult } from '../models/outcome-schema-count-result';
import { OutcomeWithViewpointResult } from '../models/outcome-with-viewpoint-result';
import { ApiErrorService } from './api-error.service';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class ItemOutcomeService {
  private defaultService = inject(DefaultService);
  private apiErrorService = inject(ApiErrorService);

  /**
   * Retrieves count data for each outcome schema of an item.
   *
   * @param {string} uuid The UUID of the item.
   * @param {number} offset Optional. Pagination offset. Defaults to 0.
   * @param {number} limit Optional. Pagination limit. Defaults to 20.
   * @return {Observable<OutcomeSchemaCountResult>} Observable emitting outcome schema counts.
   */
  getOutcomeSchemaCountData(uuid: string, offset: number = 0, limit: number = 20): Observable<OutcomeSchemaCountResult> {
    const queryName = 'CountOutcomeSchema';
    const version = 0;

    const body = JSON.stringify({
      uuid: uuid,
      offset: offset,
      limit: limit,
    });

    return this.defaultService
      .queryQueryResultPost({ name: queryName, version: version, body }, 'body', false, {
        httpHeaderAccept: 'application/json',
      })
      .pipe(
        map((result) => result as OutcomeSchemaCountResult),
        catchError((error: HttpErrorResponse) => {
          this.apiErrorService.handleError(error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Retrieves viewpoint-outcome join data for a specific schema of an item.
   *
   * @param {string} uuid The UUID of the item.
   * @param {string} schema The schema name to filter by.
   * @param {number} offset Optional. Pagination offset. Defaults to 0.
   * @param {number} limit Optional. Pagination limit. Defaults to 20.
   * @return {Observable<OutcomeWithViewpointResult>} Observable emitting join data.
   */
  getOutcomeSchemaViewpointData(uuid: string, schema: string, offset: number = 0, limit: number = 20): Observable<OutcomeWithViewpointResult> {
    const queryName = 'JoinOutcomeWithViewpoint';
    const version = 0;

    const body = JSON.stringify({
      uuid: uuid,
      schema: schema,
      offset: offset,
      limit: limit,
    });

    return this.defaultService
      .queryQueryResultPost({ name: queryName, version: version, body }, 'body', false, {
        httpHeaderAccept: 'application/json',
      })
      .pipe(
        map((result) => result as OutcomeWithViewpointResult),
        catchError((error: HttpErrorResponse) => {
          this.apiErrorService.handleError(error);
          return throwError(() => error);
        })
      );
  }
}
