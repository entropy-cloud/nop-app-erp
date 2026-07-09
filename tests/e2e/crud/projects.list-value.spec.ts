import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpPrjProject',
  route: '/ErpPrjProject-main',
  expectedCount: 1,
  expectedTokens: ['PRJ-2026-001'],
  fields: 'id code',
});
