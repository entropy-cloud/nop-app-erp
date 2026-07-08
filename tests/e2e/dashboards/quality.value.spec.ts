import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'quality',
  route: '/qa-dashboard-main',
  query: 'query($startDate:String,$endDate:String){ ErpQaDashboard__getDashboardKpi(startDate:$startDate,endDate:$endDate) }',
  variables: { startDate: '2026-07-01', endDate: '2026-07-31' },
  responseKey: 'ErpQaDashboard__getDashboardKpi',
  expected: {
    inspectionCount: 3,
    passRate: 0.6666666666666666,
    rejectedCount: 1,
    openNcrCount: 3,
  },
});

assertDashboardKpiValues({
  domain: 'quality-spc-warning',
  route: '/qa-dashboard-main',
  query: 'query{ ErpQaDashboard__getSpcOutOfControlWarning }',
  variables: {},
  responseKey: 'ErpQaDashboard__getSpcOutOfControlWarning',
  expected: { outOfControlChartCount: 1, inadequateCapabilityCount: 1, openSpcNcrCount: 1 },
});
