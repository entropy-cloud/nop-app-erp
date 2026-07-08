import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'hr-payroll-simulation-comparison',
  route: '/payroll-simulation-comparison',
  query: 'query($reportName:String!,$simulationId:BigDecimal){ ErpHrReport__renderHtml(reportName:$reportName,data:{simulationId:$simulationId}) }',
  variables: { reportName: 'payroll-simulation-comparison', simulationId: '1' },
  responseKey: 'ErpHrReport__renderHtml',
  expectedTokens: [
    '薪酬模拟对比报表',
    '赵明',
    '钱华',
    '部门小计',
    'BASE_SALARY',
    '10000.00',
    '11000.00',
  ],
});
