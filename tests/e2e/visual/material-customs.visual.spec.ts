// C2 Material Customs visual smoke (plan 2026-07-21-1206-1 Phase 2).
//
// Validates the backend wiring contract that the customized
// `ErpMdMaterial/ErpMdMaterial.view.xml` (跨境贸易分组) and
// `ErpMdMaterialCustoms/ErpMdMaterialCustoms.view.xml` (per-transaction 报关记录)
// depend on:
//   1. ErpMdMaterial xmeta exposes the 9 cross-border fields (vatRate/drawbackRate/
//      customsHS/countryOfOrigin/preferenceCode/customsNameCn/customsNameEn/
//      declarationUnit/supervisionCondition) — proves the form's crossBorder group
//      will resolve without "field not found".
//   2. ErpMdMaterialCustoms entity + BizModel is registered (findPage action
//      callable, returns empty array for non-matching filter, no "unknown-operation"
//      or "field not found").
//
// Why GraphQL-level (not DOM-rendered): same rationale as party-search-picker:
// proves the backend contract the AMIS view.xml would invoke. Run with:
//   npx playwright test tests/e2e/visual/material-customs.visual.spec.ts

import { test, expect, loginAndNavigate } from '../fixtures';

test.describe('C2 Material Customs backend wiring', () => {
  test('ErpMdMaterial xmeta exposes 9 cross-border fields', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    // Query the 9 cross-border fields + a known-existing baseline field (code)
    // on a non-matching filter. If any field is not in xmeta, GraphQL returns
    // "Cannot query field" error.
    const response = await page.evaluate(async () => {
      const res = await fetch('/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `query {
            ErpMdMaterial__findPage(limit: 1) {
              total
              items {
                code
                vatRate
                drawbackRate
                customsHS
                countryOfOrigin
                preferenceCode
                customsNameCn
                customsNameEn
                declarationUnit
                supervisionCondition
              }
            }
          }`,
        }),
      });
      return res.json();
    });

    expect(response.errors, `GraphQL errors: ${JSON.stringify(response.errors)}`).toBeUndefined();
    expect(response.data?.ErpMdMaterial__findPage, 'findPage must return page object').toBeDefined();
    expect(Array.isArray(response.data?.ErpMdMaterial__findPage?.items),
      'items must be an array').toBe(true);
  });

  test('ErpMdMaterialCustoms findPage action is registered and returns page', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const response = await page.evaluate(async () => {
      const res = await fetch('/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `query {
            ErpMdMaterialCustoms__findPage(limit: 10) {
              total
              items {
                id code materialId declarationNo partnerId
                declarationDate qtyDeclared uomDeclared amountDeclared
                dutyAmount vatAmount drawbackReceiptNo
                sourceBillType sourceBillCode
              }
            }
          }`,
        }),
      });
      return res.json();
    });

    // Action must be registered (BizModel discovered via @BizModel + bean registered)
    expect(response.errors, `GraphQL errors: ${JSON.stringify(response.errors)}`).toBeUndefined();
    expect(response.data?.ErpMdMaterialCustoms__findPage,
      'ErpMdMaterialCustoms__findPage must return page object').toBeDefined();
    expect(Array.isArray(response.data?.ErpMdMaterialCustoms__findPage?.items),
      'items must be an array (empty for fresh DB)').toBe(true);
  });
});
