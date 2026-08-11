export interface BasicItemListResultItem {
  UUID: string;
  Module: string;
  Type: string;
  Name: string;
  Version: string;
  Path: string;
  TotalCount: number;
}

export interface BasicItemListResult {
  BasicItemList: {
    Item: BasicItemListResultItem[];
  };
}
