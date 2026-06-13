export interface OutcomeWithViewpointResultItem {
  SCHEMA_NAME: string;
  SCHEMA_VERSION: number;
  EVENT_ID: number;
  TIMESTAMP: string;
  AGENT_UUID: string;
  VIEWPOINT: string;
  TotalCount: number;
}

export interface OutcomeWithViewpointResult {
  OutcomeWithViewpoint: {
    Record: OutcomeWithViewpointResultItem[];
  };
}
