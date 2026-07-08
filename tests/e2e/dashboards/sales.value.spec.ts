import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'sales',
  route: '/sal-dashboard-main',
  query: 'query($startDate:String,$endDate:String){ ErpSalDashboard__getDashboardKpi(startDate:$startDate,endDate:$endDate) }',
  variables: { startDate: '2026-07-01', endDate: '2026-07-31' },
  responseKey: 'ErpSalDashboard__getDashboardKpi',
  expected: { salesAmount: 1000, orderCount: 1, invoiceCount: 1 },
});
