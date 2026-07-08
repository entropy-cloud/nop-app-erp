import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'fin-income-statement',
  route: '/income-statement',
  query: 'query($reportName:String!,$periodId:BigDecimal){ ErpFinReport__renderHtml(reportName:$reportName,data:{periodId:$periodId}) }',
  variables: { reportName: 'income-statement', periodId: '1' },
  responseKey: 'ErpFinReport__renderHtml',
  expectedTokens: ['利润表', '主营业务收入', '1130.00', '净利润'],
});
