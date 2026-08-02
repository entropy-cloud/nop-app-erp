import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'mnt-downtime-summary',
  route: '/downtime-summary',
  query: 'query($reportName:String!,$equipmentId:BigDecimal,$startDate:String,$endDate:String){ ErpMntReport__renderHtml(reportName:$reportName,data:{equipmentId:$equipmentId,startDate:$startDate,endDate:$endDate}) }',
  variables: { reportName: 'downtime-summary' },
  responseKey: 'ErpMntReport__renderHtml',
  expectedTokens: ['停机统计表', '预防性维护停机', '240.00', '数控机床'],
});
