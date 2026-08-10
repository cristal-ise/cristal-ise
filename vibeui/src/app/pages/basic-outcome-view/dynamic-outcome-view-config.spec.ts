import { describe, it, expect } from 'vitest';
import DynamicOutcomeViewConfig, { OutcomeComponentType } from './dynamic-outcome-view-config';
import * as fs from 'fs';
import * as path from 'path';
import { BasicOutcomeView } from './basic-outcome-view';

describe('ComponentCatalog', () => {
  const dataDir = path.join(process.cwd(), 'src/test/data/OutcomeView');
  const expectedDir = path.join(dataDir, 'expected');
  const files = fs.readdirSync(dataDir).filter(f => f.endsWith('.json'));

  files.forEach(file => {
    it(`should match expected catalog for ${file}`, async () => {
      const filePath = path.join(dataDir, file);
      const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));

      const rootName = Object.keys(data)[0];
      const schema = await BasicOutcomeView.generateSchema(data);
      const catalog = DynamicOutcomeViewConfig.createFromSchema(schema, rootName);
      expect(catalog).toBeInstanceOf(DynamicOutcomeViewConfig);

      const expectedPath = path.join(expectedDir, `${file.substring(0, file.indexOf('.'))}Catalog.json`);
      if (!fs.existsSync(expectedPath)) {
        throw new Error(`Expected catalog file not found: ${expectedPath}. Run the generation script first.`);
      }

      const expected = JSON.parse(fs.readFileSync(expectedPath, 'utf8'));
      expect(catalog.getMappings()).toEqual(expected.mappings);
    });
  });

  it('should return PRIMITIVE for unknown paths', () => {
    const catalog = new DynamicOutcomeViewConfig();
    expect(catalog.getComponentType('any.path')).toBe(OutcomeComponentType.PRIMITIVE);
  });
});
