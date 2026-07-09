import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpQaInspection',
  route: '/ErpQaInspection-main',
  expectedCount: 3,
  expectedTokens: ['INS-2026-001', 'ACCEPTED', 'REJECTED'],
  fields: 'id code result',
});
