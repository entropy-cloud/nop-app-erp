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
// NOTE on date filters: flux `input-date` renders a fillable `<input
// name>`, so every parameterized dashboard is locked to the seed window via
// explicit filterValues (mirroring the value-spec layer). This is required,
// not optional: global-setup pins the "current period" to the running month
// (E2E-AUTO-<yyyymm>), so the unfiltered month-to-date default no longer
// covers the fixed 2026-07 seed dates once the clock moves past July 2026.
// The form values only reach the page-level data-sources because the
// dashboard filterForm declares `valuesPath: filterForm` (flux forms publish
// to the parent scope only via valuesPath).

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
  // Explicit seed-month range (mirrors sales.value.spec.ts). The unfiltered
  // default is month-to-date; global-setup pins the current period to the
  // running month, which no longer covers the fixed 2026-07 seed dates.
  filterDates: { 开始日期: '2026-07-01', 结束日期: '2026-07-31' },
  expectedKpiTokens: ['1000'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'purchase',
  route: '/pur-dashboard-main',
  filterDates: { 开始日期: '2026-07-01', 结束日期: '2026-07-31' },
  expectedKpiTokens: ['850'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'inventory',
  route: '/inv-dashboard-main',
  filterDates: { 开始日期: '2026-07-01', 结束日期: '2026-07-31' },
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
  // Same range as manufacturing.value.spec.ts -> periodCompletedQty = 180.
  filterDates: { 开始日期: '2026-06-01', 结束日期: '2026-07-31' },
  expectedKpiTokens: ['180'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'maintenance',
  route: '/mnt-dashboard-main',
  filterDates: { 开始日期: '2026-07-01', 结束日期: '2026-07-31' },
  expectedKpiTokens: ['3'],
  hasChart: true,
  alertTable: true,
});

assertDashboardRendered({
  domain: 'quality',
  route: '/qa-dashboard-main',
  filterDates: { 开始日期: '2026-07-01', 结束日期: '2026-07-31' },
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
