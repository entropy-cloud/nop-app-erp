import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'fin-cash-flow',
  route: '/cash-flow',
  query: 'query($reportName:String!,$periodId:BigDecimal){ ErpFinReport__renderHtml(reportName:$reportName,data:{periodId:$periodId}) }',
  variables: { reportName: 'cash-flow-statement', periodId: '1' },
  responseKey: 'ErpFinReport__renderHtml',
  expectedTokens: ['现金流量表', '银行存款', '960.50', 'OUTFLOW'],
});
