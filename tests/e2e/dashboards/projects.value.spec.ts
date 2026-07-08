import { assertDashboardKpiValues } from './_helper';

assertDashboardKpiValues({
  domain: 'projects',
  route: '/prj-dashboard-main',
  query: 'query{ ErpPrjDashboard__getDashboardKpi }',
  variables: {},
  responseKey: 'ErpPrjDashboard__getDashboardKpi',
  expected: { openProjectCount: 1, totalBudget: 50000, incurredCost: 30000, executionRate: 0.6 },
});

assertDashboardKpiValues({
  domain: 'projects-gross-margin',
  route: '/prj-dashboard-main',
  query: 'query{ ErpPrjDashboard__getProjectGrossMargin }',
  variables: {},
  responseKey: 'ErpPrjDashboard__getProjectGrossMargin',
  expected: {
    projectCount: 1,
    totalRevenue: 50000,
    totalCost: 30000,
    totalGrossProfit: 20000,
    grossMarginPct: 0.4,
  },
});
