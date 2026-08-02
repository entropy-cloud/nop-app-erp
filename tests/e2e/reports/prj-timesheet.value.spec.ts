import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'prj-timesheet',
  route: '/timesheet-detail',
  query: 'query($reportName:String!,$projectId:BigDecimal,$startDate:String,$endDate:String){ ErpPrjReport__renderHtml(reportName:$reportName,data:{projectId:$projectId,startDate:$startDate,endDate:$endDate}) }',
  variables: { reportName: 'timesheet-detail' },
  responseKey: 'ErpPrjReport__renderHtml',
  expectedTokens: ['工时明细表', '800.00', '8.00'],
});
