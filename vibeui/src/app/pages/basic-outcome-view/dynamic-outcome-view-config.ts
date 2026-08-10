export enum OutcomeComponentType {
  FIELDSET = 'FIELDSET',
  TABLE = 'TABLE',
  PRIMITIVE = 'PRIMITIVE',
  PRIMITIVE_ARRAY = 'PRIMITIVE_ARRAY',
  TABS = 'TABS',
  UNIMPLEMENTED = 'UNIMPLEMENTED',
}

export type ComponentCatalogMap = Record<string, OutcomeComponentType>;

class DynamicOutcomeViewConfig {
  constructor(
    private readonly mappings: ComponentCatalogMap = {},
  ) {}

  getComponentType(path: string): OutcomeComponentType {
    return this.mappings[path] || OutcomeComponentType.PRIMITIVE;
  }

  getMappings(): ComponentCatalogMap {
    return this.mappings;
  }

  static createFromSchema(schema: any, rootName?: string): DynamicOutcomeViewConfig {
    if (!schema) return new DynamicOutcomeViewConfig({});

    const mappings: ComponentCatalogMap = {};

    // Root element
    if (rootName) {
      const rootType = this.getOutcomeComponentType(schema, schema);
      mappings[rootName] =
        rootType === OutcomeComponentType.PRIMITIVE ? OutcomeComponentType.FIELDSET : rootType;
      if (schema.properties) {
        this.processProperties(schema, schema.properties, rootName, mappings);
      } else if (schema.items) {
        const itemDef = this.getDefinition(schema, schema.items);
        if (itemDef && itemDef.properties) {
          this.processProperties(schema, itemDef.properties, rootName, mappings);
        }
      } else if (schema.$ref) {
        const def = this.getDefinition(schema, schema);
        if (def && def.properties) {
          this.processProperties(schema, def.properties, rootName, mappings);
        }
      } else if (schema.definitions && schema.definitions[rootName]) {
        const def = schema.definitions[rootName];
        if (def.properties) {
          this.processProperties(schema, def.properties, rootName, mappings);
        }
      }
    }

    return new DynamicOutcomeViewConfig(mappings);
  }

  private static processProperties(
    schema: any,
    properties: any,
    prefix: string,
    mappings: ComponentCatalogMap,
  ) {
    if (!properties) return;

    Object.entries(properties).forEach(([key, prop]: [string, any]) => {
      const path = prefix ? `${prefix}.${key}` : key;
      mappings[path] = this.getOutcomeComponentType(schema, prop);

      if (mappings[path] === OutcomeComponentType.FIELDSET) {
        const def = this.getDefinition(schema, prop);
        if (def && def.properties) {
          this.processProperties(schema, def.properties, path, mappings);
        }
      }

      if (mappings[path] === OutcomeComponentType.TABS) {
        const itemDef = this.getDefinition(schema, prop.items);
        if (itemDef && itemDef.properties) {
          this.processProperties(schema, itemDef.properties, path, mappings);
        }
      }
    });
  }

  private static getDefinition(schema: any, propOrRef: any) {
    const ref = typeof propOrRef === 'string' ? propOrRef : propOrRef?.$ref;
    if (ref) {
      const name = ref.split('/').pop();
      return schema.definitions?.[name!];
    }
    return propOrRef;
  }

  private static isObject(schema: any, prop: any): boolean {
    const effective = this.getDefinition(schema, prop);
    return effective?.type === 'object';
  }

  private static isComplexObject(schema: any, def: any): boolean {
    const effectiveDef = this.getDefinition(schema, def);

    if (effectiveDef && effectiveDef.properties) {
      return Object.values(effectiveDef.properties).some((prop: any) =>
        this.isObject(schema, prop),
      );
    }

    return false;
  }

  private static getOutcomeComponentType(schema: any, prop: any): OutcomeComponentType {
    const effectiveProp = this.getDefinition(schema, prop);
    const type = effectiveProp?.type;

    if (type === 'object') {
      if (!effectiveProp.properties && !effectiveProp.$ref) {
        return OutcomeComponentType.UNIMPLEMENTED;
      }
      return OutcomeComponentType.FIELDSET;
    } else if (type === 'array') {
      const items = effectiveProp.items;
      if (!items) return OutcomeComponentType.UNIMPLEMENTED;

      const itemDef = this.getDefinition(schema, items);

      if (itemDef && itemDef.type === 'object') {
        if (this.isComplexObject(schema, itemDef)) {
          return OutcomeComponentType.TABS;
        }
        return OutcomeComponentType.TABLE;
      }
      return OutcomeComponentType.PRIMITIVE_ARRAY;
    }

    return OutcomeComponentType.PRIMITIVE;
  }
}

export default DynamicOutcomeViewConfig;
