import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'purchase',
  route: '/pur-dashboard-main',
  query: 'query($startDate:String,$endDate:String){ ErpPurDashboard__getDashboardKpi(startDate:$startDate,endDate:$endDate) }',
  variables: { startDate: '2026-07-01', endDate: '2026-07-31' },
  responseKey: 'ErpPurDashboard__getDashboardKpi',
  expected: { purchaseAmount: 850, orderCount: 1 },
});
