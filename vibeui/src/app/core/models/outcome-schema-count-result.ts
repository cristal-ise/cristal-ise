export interface OutcomeSchemaCountDataResultItem {
  SCHEMA_NAME: string;
  count: number;
}

export interface OutcomeSchemaCountResult {
  OutcomeSchema: {
    Record: OutcomeSchemaCountDataResultItem[];
  };
}
