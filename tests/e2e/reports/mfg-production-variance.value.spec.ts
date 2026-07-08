import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'mfg-production-variance',
  route: '/production-variance-report',
  query: 'query($reportName:String!){ ErpMfgReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'production-variance-report' },
  responseKey: 'ErpMfgReport__renderHtml',
  expectedTokens: ['生产差异分析表', 'WO-2026-001', '6000.00', '6300.00', '300.00'],
});
