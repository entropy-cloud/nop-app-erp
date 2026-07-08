import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'mfg-forecast-variance',
  route: '/forecast-variance-report',
  query: 'query($reportName:String!){ ErpMfgReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'forecast-variance-report' },
  responseKey: 'ErpMfgReport__renderHtml',
  expectedTokens: ['需求预测差异分析表', '200.00', '180.00', '-20.00'],
});
