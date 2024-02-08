import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { Observable } from 'rxjs';


export interface LookupData {
  name: string;
  type: string; //values : domain, item, agent, role
  path: string;
  url: string;
  uuid?: string;
  hasJoblist?: boolean;
}

export interface PagedResult {
  totalRows: number;
  rows: LookupData[];
}

@Injectable({
  providedIn: 'root'
})
export class LookupService {

  private root = "/api";

  constructor(private http: HttpClient) {}

  public getDomainTree(
    startPath: string,
  ): Observable<PagedResult> {
    const params = new HttpParams().set('search', "tree");

    return this.http.get<PagedResult>(this.root + '/domain' + startPath, {
      params: params,
      withCredentials: true,
    });
  }

  public getDomainChildren(path: string, params?: any): Observable<PagedResult> {
    return this.http.get<PagedResult>(this.root + '/domain' + path, {
      params: params,
      withCredentials: true,
    });
  }
}
