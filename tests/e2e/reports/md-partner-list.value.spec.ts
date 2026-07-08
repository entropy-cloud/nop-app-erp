import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'md-partner-list',
  route: '/partner-list',
  query: 'query($reportName:String!){ ErpMdReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'partner-list' },
  responseKey: 'ErpMdReport__renderHtml',
  expectedTokens: [
    '往来单位清单',
    'CUST-001',
    'CUST-002',
    'SUP-001',
    'SUP-002',
    'CUSTOMER',
    'SUPPLIER',
    '500000.00',
    '300000.00',
    'ACTIVE',
  ],
});
