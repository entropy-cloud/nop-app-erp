// C1 Party Search Picker visual smoke (plan 2026-07-21-0827-2 Phase 2).
//
// Validates the backend wiring contract that the hand-written
// `party-search/main.picker.page.yaml` depends on:
//   1. GraphQL `ErpParty__findParties` action is registered (BizModel loaded,
//      @BizQuery wired, no "field not found" or "action not registered").
//   2. `ErpParty__getParty` returns null gracefully for non-existent ID.
//   3. `ErpParty__findReferences` returns empty Map for Employee/Organization
//      (SPI port exists but downstream impl not registered).
//
// Why GraphQL-level (not DOM-rendered): the picker is embedded by parent pages
// (no top-level route, plan §5.3 "picker 不需独立菜单项，picker 经父页面引用即可").
// Visual DOM rendering of the AMIS picker would require a parent page that
// includes it; per Non-Goals of C1, no parent page is modified. This smoke
// therefore validates the backend contract that the AMIS schema would invoke,
// equivalent to a "wired & loadable" check.
//
// Same strategy as gl-mapping-rule.visual.spec.ts (proves @BizMutation wired
// through). Run with: npx playwright test tests/e2e/visual/party-search-picker.visual.spec.ts

import { test, expect, loginAndNavigate } from '../fixtures';

test.describe('C1 Party Search Picker backend wiring', () => {
  test('ErpParty__findParties action is registered and returns array', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const response = await page.evaluate(async () => {
      const res = await fetch('/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `query {
            ErpParty__findParties(keyword: "ABCDEF-NOT-MATCHED", partyTypes: ["PARTNER", "EMPLOYEE", "ORGANIZATION"], limit: 50) {
              partyType partyId code name phone email status displayName
            }
          }`,
        }),
      });
      return res.json();
    });

    // Action must be registered (no errors field with "not found")
    expect(response.errors, `GraphQL errors: ${JSON.stringify(response.errors)}`).toBeUndefined();
    // findParties result must be an array (empty for non-matching keyword)
    const items = response.data?.ErpParty__findParties;
    expect(Array.isArray(items), 'ErpParty__findParties must return an array').toBe(true);
  });

  test('ErpParty__getParty returns null for non-existent ID', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const response = await page.evaluate(async () => {
      const res = await fetch('/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `query {
            ErpParty__getParty(partyType: "PARTNER", partyId: 99999999) {
              partyType partyId code name
            }
          }`,
        }),
      });
      return res.json();
    });

    expect(response.errors, `GraphQL errors: ${JSON.stringify(response.errors)}`).toBeUndefined();
    expect(response.data?.ErpParty__getParty, 'getParty must return null for non-existent ID').toBeNull();
  });

  test('ErpParty__findReferences returns empty map for unregistered SPI', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const response = await page.evaluate(async () => {
      const res = await fetch('/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: `query {
            ErpParty__findReferences(partyType: "EMPLOYEE", partyId: 1) { k v }
          }`,
        }),
      });
      return res.json();
    });

    // Employee SPI port exists but no downstream impl registered → empty Map (no exception)
    expect(response.errors, `GraphQL errors: ${JSON.stringify(response.errors)}`).toBeUndefined();
    const refs = response.data?.ErpParty__findReferences;
    expect(Array.isArray(refs), 'findReferences must return a Map (serialized as [{k,v}] in GraphQL)').toBe(true);
    expect(refs.length, 'Employee findReferences must be empty Map (SPI unregistered)').toBe(0);
  });
});
