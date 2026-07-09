import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpMntVisit',
  route: '/ErpMntVisit-main',
  expectedCount: 2,
  expectedTokens: ['VIS-2026-001', 'PLANNED'],
  fields: 'id code visitType',
});
