import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'fin-balance-sheet',
  route: '/balance-sheet',
  query: 'query($reportName:String!,$periodId:BigDecimal){ ErpFinReport__renderHtml(reportName:$reportName,data:{periodId:$periodId}) }',
  variables: { reportName: 'balance-sheet', periodId: '1' },
  responseKey: 'ErpFinReport__renderHtml',
  expectedTokens: ['资产负债表', '银行存款', '169.50'],
});
