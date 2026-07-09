import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpCsTicket',
  route: '/ErpCsTicket-main',
  expectedCount: 2,
  expectedTokens: ['TKT-2026-001', 'HIGH'],
  fields: 'id code priority',
});
