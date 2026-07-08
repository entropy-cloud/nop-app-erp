import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'assets',
  route: '/ast-dashboard-main',
  query: 'query($periodId:String){ ErpAstDashboard__getDashboardKpi(periodId:$periodId) }',
  variables: { periodId: '2026-07' },
  responseKey: 'ErpAstDashboard__getDashboardKpi',
  expected: {
    originalValue: 135000,
    accumulatedDepreciation: 6000,
    netBookValue: 129000,
    periodDepreciation: 2000,
  },
});
