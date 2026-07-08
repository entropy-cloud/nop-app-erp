import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'hr-employee-net-balance',
  route: '/employee-net-balance',
  query: 'query($reportName:String!){ ErpHrReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'employee-net-balance' },
  responseKey: 'ErpHrReport__renderHtml',
  expectedTokens: ['员工净余额报表', '张三员工往来', '员工欠公司', '1000.00', '700.00'],
});
