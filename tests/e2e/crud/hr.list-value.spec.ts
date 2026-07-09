import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpHrEmployee',
  route: '/ErpHrEmployee-main',
  expectedCount: 2,
  expectedTokens: ['HR-EMP-001', 'HR-EMP-002'],
  fields: 'id code fullName',
});
