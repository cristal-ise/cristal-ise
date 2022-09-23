import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ItemTableService {
  private root = "/api";

  constructor(private http: HttpClient) {}

}
