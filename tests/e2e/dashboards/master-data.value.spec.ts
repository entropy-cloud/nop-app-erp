import { test, expect, loginAndNavigate } from '../fixtures';
import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'master-data',
  route: '/md-dashboard-main',
  query: '{ ErpMdDashboard__getDashboardKpi }',
  variables: {},
  responseKey: 'ErpMdDashboard__getDashboardKpi',
  expected: {
    materialCount: 4,
    customerCount: 2,
    vendorCount: 2,
    inactiveMaterialCount: 0,
    inactivePartnerCount: 0,
  },
});

test.describe('master-data dashboard alerts empty-set', () => {
  test('findMaterialWithoutSkuAlert returns empty list (seed: all materials have SKU)', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const resp = await page.request.post('/graphql', {
      data: { query: '{ ErpMdDashboard__findMaterialWithoutSkuAlert }', variables: {} },
    });
    expect(resp.status(), 'GraphQL should return 200').toBe(200);

    const json = await resp.json();
    const list = json?.data?.ErpMdDashboard__findMaterialWithoutSkuAlert;
    expect(Array.isArray(list), 'alert should be a list').toBe(true);
    expect(list.length, 'no material without SKU in seed baseline').toBe(0);
  });

  test('findSkuWithoutPriceAlert returns empty list (seed: all SKUs have purchase price)', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const resp = await page.request.post('/graphql', {
      data: { query: '{ ErpMdDashboard__findSkuWithoutPriceAlert }', variables: {} },
    });
    expect(resp.status(), 'GraphQL should return 200').toBe(200);

    const json = await resp.json();
    const list = json?.data?.ErpMdDashboard__findSkuWithoutPriceAlert;
    expect(Array.isArray(list), 'alert should be a list').toBe(true);
    expect(list.length, 'no SKU without price in seed baseline').toBe(0);
  });
});
