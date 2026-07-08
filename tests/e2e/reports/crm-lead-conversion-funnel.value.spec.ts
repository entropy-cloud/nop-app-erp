import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'crm-lead-conversion-funnel',
  route: '/lead-conversion-funnel',
  query: 'query($reportName:String!){ ErpCrmReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'lead-conversion-funnel' },
  responseKey: 'ErpCrmReport__renderHtml',
  expectedTokens: ['线索转化漏斗表', '验证', '报价', '50000.00', '80000.00'],
});
