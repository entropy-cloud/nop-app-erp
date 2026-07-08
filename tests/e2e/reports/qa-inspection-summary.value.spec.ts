import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'qa-inspection-summary',
  route: '/inspection-summary',
  query: 'query($reportName:String!){ ErpQaReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'inspection-summary' },
  responseKey: 'ErpQaReport__renderHtml',
  expectedTokens: ['质检合格率统计表', '产品甲', '100.00%', '50.00%'],
});
