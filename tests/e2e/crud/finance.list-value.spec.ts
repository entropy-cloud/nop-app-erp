import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpFinVoucher',
  route: '/ErpFinVoucher-main',
  expectedCount: 4,
  expectedTokens: ['PZ-2026-001', 'TRANSFER'],
  fields: 'id code voucherType',
});
