import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'inventory',
  route: '/inv-dashboard-main',
  query: 'query($startDate:String,$endDate:String){ ErpInvDashboard__getDashboardKpi(startDate:$startDate,endDate:$endDate) }',
  variables: { startDate: '2026-07-01', endDate: '2026-07-31' },
  responseKey: 'ErpInvDashboard__getDashboardKpi',
  expected: { totalValue: 10450, incomingQty: 100, outgoingQty: 0, turnoverRate: 0 },
});
