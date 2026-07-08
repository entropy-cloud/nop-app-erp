import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'mnt-maintenance-history',
  route: '/maintenance-history',
  query: 'query($reportName:String!){ ErpMntReport__renderHtml(reportName:$reportName) }',
  variables: { reportName: 'maintenance-history' },
  responseKey: 'ErpMntReport__renderHtml',
  expectedTokens: ['维护历史表', 'VIS-2026-001', '120.00', '90.00'],
});
