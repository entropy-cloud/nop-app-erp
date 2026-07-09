import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpMdPartner',
  route: '/ErpMdPartner-main',
  expectedCount: 5,
  expectedTokens: ['CUST-001', 'SUP-001', 'CUSTOMER', 'SUPPLIER'],
  fields: 'id code partnerType status',
});
