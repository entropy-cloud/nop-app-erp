import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'md-material-price-list',
  route: '/material-price-list',
  query: 'query($reportName:String!){ ErpMdReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'material-price-list' },
  responseKey: 'ErpMdReport__renderHtml',
  expectedTokens: [
    '物料价格清单',
    'MAT-001',
    'MAT-002',
    'FINISHED_PRODUCT',
    'RAW_MATERIAL',
    '120.00',
    '200.00',
    '280.00',
    '300.00',
    '500.00',
    '680.00',
    '8.50',
  ],
});
