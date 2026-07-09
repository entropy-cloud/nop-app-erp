import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpCrmLead',
  route: '/ErpCrmLead-main',
  expectedCount: 2,
  expectedTokens: ['LEAD-2026-001', 'OPPORTUNITY'],
  fields: 'id code leadType',
});
