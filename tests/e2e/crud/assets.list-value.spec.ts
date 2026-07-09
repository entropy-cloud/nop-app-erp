import { assertCrudListValues } from './_helper';

assertCrudListValues({
  entityName: 'ErpAstAsset',
  route: '/ErpAstAsset-main',
  expectedCount: 3,
  expectedTokens: ['AST-2026-001', 'AST-2026-002'],
  fields: 'id code',
});
