import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'finance',
  route: '/fin-dashboard-main',
  query: 'query($periodId:Long){ ErpFinDashboard__getDashboardKpi(periodId:$periodId) }',
  variables: { periodId: 1 },
  responseKey: 'ErpFinDashboard__getDashboardKpi',
  expected: { revenue: 1130, netProfit: 1130, expense: 0 },
});
