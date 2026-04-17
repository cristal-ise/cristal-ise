import { Injectable, inject } from '@angular/core';
import { DefaultService } from '../../api';
import { Observable, map } from 'rxjs';
import { BasicItemListResult } from '../models/basic-item-list-result';

@Injectable({
  providedIn: 'root',
})
export class ItemListService {
  private defaultService = inject(DefaultService);

  /**
   * Retrieves a basic list of items based on the specified parameters.
   *
   * @param {string} domainPath The domain path used for filtering the item list.
   * @param {string} searchText Optional. The text used to search items in the list. Defaults to an empty string.
   * @param {number} offset Optional. The offset for pagination, indicating the starting point of items to retrieve. Defaults to 0.
   * @param {number} limit Optional. The maximum number of items to retrieve. Defaults to 20.
   * @return {Observable<BasicItemListResult>} An observable emitting the result of the basic item list query.
   */
  getBasicItemList(domainPath: string, searchText: string='', offset: number=0, limit: number=20): Observable<BasicItemListResult> {
    const queryName = 'QueryBasicItemList';
    const version = 0;

    const body = JSON.stringify({
      domainPath: domainPath,
      searchText: searchText,
      offset: offset,
      limit: limit,
    });

    return this.defaultService
      .queryQueryResultPost({ name: queryName, version: version, body }, 'body', false, {
        httpHeaderAccept: 'application/json',
      })
      .pipe(map((result) => result as BasicItemListResult));
  }
}
