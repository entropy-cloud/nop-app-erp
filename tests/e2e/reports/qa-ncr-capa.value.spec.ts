import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'qa-ncr-capa',
  route: '/ncr-capa-summary',
  query: 'query($reportName:String!,$startDate:String,$endDate:String){ ErpQaReport__renderHtml(reportName:$reportName,data:{startDate:$startDate,endDate:$endDate}) }',
  variables: { reportName: 'ncr-capa-summary' },
  responseKey: 'ErpQaReport__renderHtml',
  expectedTokens: ['NCR-CAPA 统计表', 'HIGH', 'NORMAL'],
});
