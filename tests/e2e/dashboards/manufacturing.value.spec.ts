import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'manufacturing',
  route: '/mfg-dashboard-main',
  query: 'query($startDate:String,$endDate:String){ ErpMfgDashboard__getDashboardKpi(startDate:$startDate,endDate:$endDate) }',
  variables: { startDate: '2026-06-01', endDate: '2026-07-31' },
  responseKey: 'ErpMfgDashboard__getDashboardKpi',
  expected: { inProcessCount: 1, periodCompletedQty: 180, stockPartialCount: 1, onTimeRate: 0.5 },
});
