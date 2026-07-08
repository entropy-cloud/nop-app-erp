import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'ast-depreciation',
  route: '/asset-depreciation-detail',
  query: 'query($reportName:String!){ ErpAstReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'asset-depreciation-detail' },
  responseKey: 'ErpAstReport__renderHtml',
  expectedTokens: ['资产折旧明细表', '120000.00', '6000.00', '2000.00', '114000.00'],
});
