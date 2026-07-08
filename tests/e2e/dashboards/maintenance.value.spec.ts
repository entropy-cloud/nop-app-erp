import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'maintenance',
  route: '/mnt-dashboard-main',
  query: 'query($startDate:String,$endDate:String){ ErpMntDashboard__getDashboardKpi(startDate:$startDate,endDate:$endDate) }',
  variables: { startDate: '2026-07-01', endDate: '2026-07-31' },
  responseKey: 'ErpMntDashboard__getDashboardKpi',
  expected: { equipmentTotal: 3, runningCount: 2, openRequestCount: 1, periodVisitCount: 1 },
});
