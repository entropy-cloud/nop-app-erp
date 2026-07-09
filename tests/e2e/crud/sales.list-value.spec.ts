import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpSalOrder',
  route: '/ErpSalOrder-main',
  expectedCount: 1,
  expectedTokens: ['SO-2026-001'],
  fields: 'id code',
});
