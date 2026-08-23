import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'inv-inventory-trace',
  route: '/inventory-trace-report',
  query: 'query($reportName:String!,$moveId:String){ ErpInvReport__renderHtml(reportName:$reportName,data:{moveId:$moveId}) }',
  variables: { reportName: 'inventory-trace-report', moveId: '1' },
  responseKey: 'ErpInvReport__renderHtml',
  expectedTokens: ['库存追溯链可视化报表', 'MV-2026-001', '100.00'],
});
