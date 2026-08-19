import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';

// RC-R1.78 / UC-MAIN-10 OEE data-plane value assertions (plan 2026-08-20-0518-1
// Phase 3). Seed fact: none of the 3 equipment rows carries WORKCENTER_ID, so
// fleet OEE is deterministically uncomputable -> computedCount=0 + "—" display
// (D4 null semantics: no-data != zero-efficiency). Per-equipment computeOee on
// EQ-1 returns null components without error.
//
// Browser-render layer (visual token/pixel specs) for dashboards is pending the
// in-progress flux runtime re-baseline (see docs/testing/e2e-runbook.md and
// product-scope next-step scope); this spec pins the GraphQL data plane.

test.describe('maintenance dashboard OEE values', () => {
  test('getDashboardOeeKpi returns deterministic null-OEE semantics for seed', async ({ page }) => {
    await loginAndNavigate(page, '/mnt-dashboard-main');

    const json: any = await new GraphQLClient(page).raw(
      'query($startDate:String,$endDate:String){ ErpMntDashboard__getDashboardOeeKpi(startDate:$startDate,endDate:$endDate) }',
      { startDate: '2026-07-01', endDate: '2026-07-31' },
    );
    const kpi = json?.data?.ErpMntDashboard__getDashboardOeeKpi;
    expect(kpi, 'OEE KPI map should be present').toBeTruthy();
    expect(Number(kpi.equipmentTotal)).toBe(3);
    expect(Number(kpi.computedCount)).toBe(0);
    expect(kpi.oeeDisplay).toBe('—');
    expect(kpi.oeeAvg).toBe(null);
  });

  test('computeOee returns null components for equipment without workcenter (no error)', async ({ page }) => {
    await loginAndNavigate(page, '/mnt-dashboard-main');

    const json: any = await new GraphQLClient(page).raw(
      'query($equipmentId:Long,$dateFrom:String,$dateTo:String){ ErpMntDashboard__computeOee(equipmentId:$equipmentId,dateFrom:$dateFrom,dateTo:$dateTo) }',
      { equipmentId: 1, dateFrom: '2026-07-01', dateTo: '2026-07-31' },
    );
    const row = json?.data?.ErpMntDashboard__computeOee;
    expect(row, 'computeOee map should be present').toBeTruthy();
    expect(Number(row.equipmentId)).toBe(1);
    expect(row.availability).toBe(null);
    expect(row.performance).toBe(null);
    expect(row.quality).toBe(null);
    expect(row.oee).toBe(null);
  });
});
