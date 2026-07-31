import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'fin-period-close',
  route: '/period-close-report',
  query: 'query($reportName:String!,$periodId:BigDecimal){ ErpFinReport__renderHtml(reportName:$reportName,data:{periodId:$periodId}) }',
  variables: { reportName: 'period-close-report', periodId: '1' },
  responseKey: 'ErpFinReport__renderHtml',
  expectedTokens: ['期末结账报告', '2026-07'],
});
