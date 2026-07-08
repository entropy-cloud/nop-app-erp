import { assertReportRenderedWithValue } from './_helper';

assertReportRenderedWithValue({
  reportLabel: 'crm-forecast-accuracy',
  route: '/forecast-accuracy',
  query: 'query($reportName:String!,$forecastId:BigDecimal){ ErpCrmReport__renderHtml(reportName:$reportName,data:{forecastId:$forecastId}) }',
  variables: { reportName: 'forecast-accuracy', forecastId: '1' },
  responseKey: 'ErpCrmReport__renderHtml',
  expectedTokens: ['销售预测准确率表', '50000.00', '45000.00', '80000.00', '63000.00'],
});
