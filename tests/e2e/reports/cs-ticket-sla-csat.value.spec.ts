import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'cs-ticket-sla-csat-summary',
  route: '/ticket-sla-csat-summary',
  query: 'query($reportName:String!,$ticketType:String){ ErpCsReport__renderHtml(reportName:$reportName,data:{ticketType:$ticketType}) }',
  variables: { reportName: 'ticket-sla-csat-summary', ticketType: '1' },
  responseKey: 'ErpCsReport__renderHtml',
  expectedTokens: ['工单 SLA/CSAT 综合统计表', '投诉', '5.00', '9.00'],
});
