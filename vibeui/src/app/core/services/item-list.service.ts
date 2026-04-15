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
   * Fetches the result of the QueryBasicItemList by calling POST /query/queryResult.
   * Parameters: name=QueryBasicItemList, version=0
   * Body: {"domainPath":"/domain/desc/DomainContext","offset":0,"limit":10}
   */
  getBasicItemList(): Observable<BasicItemListResult> {
    const body = JSON.stringify({
      domainPath: '/domain/desc/DomainContext',
      offset: 0,
      limit: 10,
    });

    return this.defaultService.queryQueryResultPost(
      { name: 'QueryBasicItemList', version: 0, body },
      'body',
      false,
      { httpHeaderAccept: 'application/json' },
    ).pipe(map(result => result as BasicItemListResult));
  }
}
