import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpInvStockMove',
  route: '/ErpInvStockMove-main',
  expectedCount: 1,
  expectedTokens: ['MV-2026-001', 'INCOMING'],
  fields: 'id code moveType',
});
