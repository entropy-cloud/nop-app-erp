import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'inv-inventory-trace',
  route: '/inventory-trace-report',
  query: 'query($reportName:String!){ ErpInvReport__renderHtml(reportName:$reportName,data:{moveId:1}) }',
  variables: { reportName: 'inventory-trace-report' },
  responseKey: 'ErpInvReport__renderHtml',
  expectedTokens: ['库存追溯链可视化报表', 'MV-2026-001', '100.00'],
});
