import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpMfgWorkOrder',
  route: '/ErpMfgWorkOrder-main',
  expectedCount: 4,
  expectedTokens: ['WO-2026-001', 'COMPLETED', 'IN_PROCESS'],
  fields: 'id code docStatus',
});
