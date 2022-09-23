import { TestBed } from '@angular/core/testing';

import { ItemTableService } from './item-table.service';

describe('ItemTableService', () => {
  let service: ItemTableService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ItemTableService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
