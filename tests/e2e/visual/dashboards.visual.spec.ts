import { assertDashboardRendered } from './_helper';

// AMIS front-end render-layer value-token assertions for all 10 dashboards.
//
// Defect A (docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md) mangled
// bare `$var` in the hand-written GraphQL query templates, so all 8
// parameterized dashboards rendered ¥0/empty (and the projects gross-margin
// service too). Fixed by escaping `$` -> `${'$'}` (plan 2026-07-09-1728-1
// Phase 1 Decision). These assertions drive the real AMIS page (through the
// template resolver) so they detect the defect: pre-fix the tokens are absent
// (¥0); post-fix they render the deterministic seed values.
//
// Expected tokens derive from the value-spec layer
// (tests/e2e/dashboards/*.value.spec.ts -> docs/analysis/2026-07-08-1445-2-...).
//
// NOTE on date filters: the AMIS `input-date` filter renders no fillable
// `<input name>` (it is a custom picker component), so date-range dashboards
// are asserted on their DEFAULT current-month load. The seed data is dated
// 2026-07, so when the server clock is in that month the default range
// (month-to-date) covers the seed and the tokens match. The deterministic
// date-locked assertions live in the value-spec layer (which posts explicit
// startDate/endDate). periodId-based dashboards (finance/assets) DO expose a
// fillable input and are locked explicitly.

assertDashboardRendered({
  domain: 'finance',
  route: '/fin-dashboard-main',
  filterValues: { periodId: '1' },
  expectedKpiTokens: ['1130'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'sales',
  route: '/sal-dashboard-main',
  expectedKpiTokens: ['1000'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'purchase',
  route: '/pur-dashboard-main',
  expectedKpiTokens: ['850'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'inventory',
  route: '/inv-dashboard-main',
  expectedKpiTokens: ['10450'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'assets',
  route: '/ast-dashboard-main',
  filterValues: { periodId: '2026-07' },
  expectedKpiTokens: ['135000'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'projects',
  route: '/prj-dashboard-main',
  // getDashboardKpi has no `$var` (token 50000 = totalBudget, already intact).
  // getProjectGrossMargin HAS `$var` (was mangled -> ¥0); token 0.4 =
  // grossMarginPct proves that service now renders through AMIS.
  expectedKpiTokens: ['50000', '0.4'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'manufacturing',
  route: '/mfg-dashboard-main',
  // Default month-to-date load (seed 2026-07) -> periodCompletedQty = 80.
  // (The value spec's 180 spans the wider 2026-06-01..07-31 range.)
  expectedKpiTokens: ['80'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'maintenance',
  route: '/mnt-dashboard-main',
  expectedKpiTokens: ['3'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'quality',
  route: '/qa-dashboard-main',
  // passRate 0.6666.. -> round:2 -> "0.67", distinctive proof of $var fix.
  expectedKpiTokens: ['0.67'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'master-data',
  route: '/md-dashboard-main',
  // Non-parameterized (query has no `$var`), always intact.
  expectedKpiTokens: ['4'],
  hasChart: false,
  alertTable: true,
});
