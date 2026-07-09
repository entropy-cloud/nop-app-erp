import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpPurOrder',
  route: '/ErpPurOrder-main',
  expectedCount: 1,
  expectedTokens: ['PO-2026-001'],
  fields: 'id code',
});
