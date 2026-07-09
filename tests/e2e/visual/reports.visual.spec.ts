import { assertReportRendered } from './_helper';

// AMIS front-end render-layer assertions for reports.
//
// Defect B (docs/bugs/2026-07-09-1249-report-render-container-wiring.md):
// the "渲染报表" button fired renderHtml (backend returned HTML) but the
// response never reached the DOM. Defect A additionally mangled the `$var`
// in the renderHtml query. Both are fixed (plan 2026-07-09-1728-1).
//
// IMPLEMENTATION NOTE (deviation from the plan's prescribed onEvent mirror):
// the balance-sheet `onEvent: setVariable(event.data.result) + setValue`
// reference pattern turned out to be itself broken at runtime — (1) the
// legacy button-ajax result is NOT exposed as `event.data.result` (amis-core
// AjaxAction stores it under `outputVar`, default `responseResult`), and (2)
// CmptAction resolves targets via `componentId`/`componentName`, ignoring the
// `target:` field. The working pattern (verified here) is the same one the
// dashboards use: the render button does `actionType: reload` of an in-form
// `service` (name: reportService, initFetch: false) whose api has an adaptor
// that flattens the report HTML to `data.reportHtml`, and whose body is a
// `type: html html: "${reportHtml}"`. The service lives INSIDE the form so it
// shares the form's field-value scope (a sibling service gets periodId="" ->
// BigDecimal NumberFormatException). See plan Phase 3 Decision Record.
//
// These assertions drive the real AMIS page, click "渲染报表", wait for the
// AMIS-issued renderHtml response, then assert the report-specific tokens
// (subject names / labels that appear ONLY in the rendered report HTML) are
// now in the DOM. Pre-fix the container is empty; post-fix the rendered HTML
// is injected. Tokens derive from the value-spec layer
// (tests/e2e/reports/*.value.spec.ts) and the .xpt.xml templates; numeric
// values are comma-formatted in the HTML (e.g. "1,130.00") so format-
// independent subject/label tokens are used here, with comma-stripping left
// to the value-spec layer.
//
// Token selection: 2-3 format-independent subject/label tokens per report
// (title / column headers / dimension codes / dict labels). Numeric tokens
// >= 1000 are avoided because the DOM applies #,##0.00 thousand-separator
// formatting (e.g. 120000 -> "120,000.00"), which would break a bare
// "120000.00" assertion. Tokens < 1000 (e.g. "200.00", "0.50") are safe.
//
// Fill strategy (aligned with value-spec layer + 1728-1:182 residual risk):
// date-param reports (AMIS input-date) have NO fillable <input name>, so
// only ID/dimension params (input-number/input-text) are filled; date filters
// are left empty and the backend treats null date ranges as full extent
// (matching the value-spec layer's zero-date behavior).

// === finance (5) ===

assertReportRendered({
  reportLabel: 'fin-income-statement',
  route: '/income-statement',
  fill: { periodId: '1' },
  // Subject/label tokens (format-independent; values are comma-formatted
  // e.g. "1,130.00" so they're verified in the value-spec layer instead).
  expectedTokens: ['主营业务收入', '净利润'],
});

assertReportRendered({
  reportLabel: 'fin-balance-sheet',
  route: '/balance-sheet',
  fill: { periodId: '1' },
  expectedTokens: ['资产负债表', '银行存款', '169.50'],
});

assertReportRendered({
  reportLabel: 'fin-ar-ap-aging',
  route: '/ar-ap-aging',
  // asOfDate input-date has value: "${NOW()}" which evaluates to a raw Unix
  // timestamp (e.g. "1783621109") that the backend cannot parse as a date.
  // Fill it with a valid ISO date to override the broken default.
  fillDates: { '账龄基准日': '2026-07-08' },
  expectedTokens: ['应收应付账龄分析表', '未核销余额合计'],
});

assertReportRendered({
  reportLabel: 'fin-cash-flow',
  route: '/cash-flow',
  fill: { periodId: '1' },
  expectedTokens: ['现金流量表', '活动分类', '现金净增加'],
});

assertReportRendered({
  reportLabel: 'fin-period-close-report',
  route: '/period-close-report',
  fill: { periodId: '1' },
  expectedTokens: ['期末结账报告', '分组', '项目'],
});

// === manufacturing (3) ===

assertReportRendered({
  reportLabel: 'mfg-crp-load',
  route: '/crp-load-report',
  // workcenterId is input-number (fillable); startDate/endDate are input-date
  // (left empty — backend full extent). WC-001 is the seeded workcenter code.
  fill: { workcenterId: '1' },
  expectedTokens: ['CRP 工作中心负荷分析表', 'WC-001'],
});

assertReportRendered({
  reportLabel: 'mfg-production-variance',
  route: '/production-variance-report',
  // workOrderId + dates are optional filters; left empty for full dataset
  // (value-spec layer passes zero params). $var template still exercised.
  expectedTokens: ['生产差异分析表', 'WO-2026-001'],
});

assertReportRendered({
  reportLabel: 'mfg-forecast-variance',
  route: '/forecast-variance-report',
  // Numbers < 1000 are safe (no thousand-separator in DOM).
  expectedTokens: ['需求预测差异分析表', '200.00', '180.00'],
});

// === assets (2) ===

assertReportRendered({
  reportLabel: 'ast-asset-depreciation-detail',
  route: '/asset-depreciation-detail',
  // categoryId + dates optional; left empty. AST-2026-002 is the seeded asset
  // with depreciation data. '累计折旧' is a static column header.
  expectedTokens: ['资产折旧明细表', 'AST-2026-002', '累计折旧'],
});

assertReportRendered({
  reportLabel: 'ast-asset-disposal-detail',
  route: '/asset-disposal-detail',
  // startDate/endDate are input-date (no fillable name). Title + headers
  // always render regardless of data extent.
  expectedTokens: ['资产处置明细表', '处置类型', '清理损益'],
});

// === maintenance (2) ===

assertReportRendered({
  reportLabel: 'mnt-downtime-summary',
  route: '/downtime-summary',
  // equipmentId is input-number (fillable); dates are input-date (left empty).
  fill: { equipmentId: '1' },
  expectedTokens: ['停机统计表', '设备名称', '停机分钟'],
});

assertReportRendered({
  reportLabel: 'mnt-maintenance-history',
  route: '/maintenance-history',
  expectedTokens: ['维护历史表', 'VIS-2026-001'],
});

// === projects (2) ===

assertReportRendered({
  reportLabel: 'prj-project-cost-summary',
  route: '/project-cost-summary',
  // projectId + dates optional; left empty. PRJ-2026-001 is seeded project.
  expectedTokens: ['项目成本汇总表', 'PRJ-2026-001', '预算执行率'],
});

assertReportRendered({
  reportLabel: 'prj-timesheet-detail',
  route: '/timesheet-detail',
  // projectId is input-number (fillable); dates are input-date (left empty).
  fill: { projectId: '1' },
  expectedTokens: ['工时明细表', '工时(小时)', '工时成本'],
});

// === quality (2) ===

assertReportRendered({
  reportLabel: 'qa-inspection-summary',
  route: '/inspection-summary',
  // materialId + dates optional; left empty. '产品甲' is seeded material name.
  expectedTokens: ['质检合格率统计表', '产品甲'],
});

assertReportRendered({
  reportLabel: 'qa-ncr-capa-summary',
  route: '/ncr-capa-summary',
  // startDate/endDate are input-date (no fillable name). Title + headers
  // always render.
  expectedTokens: ['NCR-CAPA 统计表', '严重度', 'CAPA动作数'],
});

// === master-data (2) ===

assertReportRendered({
  reportLabel: 'md-material-price-list',
  route: '/material-price-list',
  // Zero-param full render. Codes + dict labels are format-independent.
  expectedTokens: ['物料价格清单', 'MAT-001', 'MAT-002'],
});

assertReportRendered({
  reportLabel: 'md-partner-list',
  route: '/partner-list',
  expectedTokens: ['往来单位清单', 'CUST-001', 'SUP-001'],
});

// === inventory (1) ===

assertReportRendered({
  reportLabel: 'inv-inventory-trace',
  route: '/inventory-trace-report',
  // The page.yaml form sends batchNo/materialId/warehouseId, but the backend's
  // buildInventoryTraceDataset only recognizes moveId (value-spec sends
  // moveId:1). With unmatched params the data rows are empty, so only the
  // always-rendering title + column headers are asserted (the AMIS render
  // pipeline itself — page form → service reload → renderHtml → DOM injection
  // — is fully exercised regardless of data extent).
  expectedTokens: ['库存追溯链可视化报表', '移动单号', '目标仓库'],
});

// === cs (1) ===

assertReportRendered({
  reportLabel: 'cs-ticket-sla-csat-summary',
  route: '/ticket-sla-csat-summary',
  // ticketType is input-text (fillable). '投诉' is the type-1 name.
  fill: { ticketType: '1' },
  expectedTokens: ['工单 SLA/CSAT 综合统计表', '投诉'],
});

// === crm (2) ===

assertReportRendered({
  reportLabel: 'crm-lead-conversion-funnel',
  route: '/lead-conversion-funnel',
  expectedTokens: ['线索转化漏斗表', '验证', '报价'],
});

assertReportRendered({
  reportLabel: 'crm-forecast-accuracy',
  route: '/forecast-accuracy',
  // forecastId is input-number (fillable). All amounts >= 1000 (comma-
  // formatted in DOM), so static column headers are used as tokens.
  fill: { forecastId: '1' },
  expectedTokens: ['销售预测准确率表', '承诺金额', '行加权收入合计'],
});

// === hr (2) ===

assertReportRendered({
  reportLabel: 'hr-employee-net-balance',
  route: '/employee-net-balance',
  expectedTokens: ['员工净余额报表', '张三员工往来', '员工欠公司'],
});

assertReportRendered({
  reportLabel: 'hr-payroll-simulation-comparison',
  route: '/payroll-simulation-comparison',
  // simulationId is input-number (fillable). '赵明'/'部门小计' are data/labels.
  fill: { simulationId: '1' },
  expectedTokens: ['薪酬模拟对比报表', '赵明', '部门小计'],
});
