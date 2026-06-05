import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SearchTextService {
  private _searchText = signal<string>('');

  readonly searchText = this._searchText.asReadonly();

  setSearchText(text: string) {
    this._searchText.set(text);
  }

  clear() {
    this._searchText.set('');
  }
}
