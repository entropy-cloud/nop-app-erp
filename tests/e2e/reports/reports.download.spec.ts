import { test } from '../fixtures';
import { assertReportDownload, type ReportDownloadCase } from './_helper';

// Report download runtime regression: 24 reports × {xlsx, pdf} = 48 cases.
// Verifies the full path "page → AMIS download target → Erp{Domain}Report__download
// → binary product" produces a non-empty binary with correct magic bytes
// (XLSX = PK\x03\x04 zip header, PDF = %PDF) and a weak structural signal
// (at least one report-specific token extractable from the binary).
//
// Path Decision (plan Phase 1 Explore):
// The page.yaml download buttons send their query to `/graphql`, but the
// `/graphql` endpoint JSON-serializes the WebContentBean return (it does
// not trigger the binary download pipeline). Binary download is served by
// the `/p/{operationName}` page-query endpoint, which routes through
// `buildJaxrsPageResponse` → `consumeWebContent` and writes the File
// content directly as the HTTP response body (Content-Type from the bean,
// Content-Disposition with the filename). Both endpoints invoke the same
// `Erp{Domain}Report__download` @BizQuery, so backend logic (template
// resolution, ALLOWED_RENDER_TYPES check, reportEngine.getRenderer) is
// identical — only the HTTP serialization layer differs.
//
// Token extraction:
// - XLSX: Nop report engine writes cell text as inline strings
//   (`t="inlineStr"` + `<is><t>...</t></is>`) directly in worksheet XML,
//   not as shared strings. The helper parses the zip, reads
//   `xl/worksheets/*.xml` entries, and extracts `<t>...</t>` text.
// - PDF: The font subset's ToUnicode CMap (bfrange/bfchar) maps glyph
//   codes to Unicode code points. The helper decompresses FlateDecode
//   streams, builds the glyph map, decodes Tj/TJ text operators, then
//   normalizes CJK Radical variants (U+2F00–U+2FDF, e.g. ⼯→工) to
//   standard CJK Unified Ideographs so tokens from the value-spec layer
//   match. Coverage verified for all 24 reports.

const REPORT_DOWNLOAD_CASES: ReportDownloadCase[] = [
  // === finance (5) ===
  {
    domain: 'fin', reportName: 'income-statement',
    data: { periodId: 1 },
    expectedTokens: ['利润表', '主营业务收入', '净利润'],
  },
  {
    domain: 'fin', reportName: 'balance-sheet',
    data: { periodId: 1 },
    expectedTokens: ['资产负债表', '银行存款'],
  },
  {
    domain: 'fin', reportName: 'ar-ap-aging',
    data: { asOfDate: '2026-07-08' },
    expectedTokens: ['应收应付账龄分析表', '未核销余额合计'],
  },
  {
    domain: 'fin', reportName: 'cash-flow-statement',
    data: { periodId: 1 },
    expectedTokens: ['现金流量表', '活动分类', '现金净增加'],
  },
  {
    domain: 'fin', reportName: 'period-close-report',
    data: { periodId: 1 },
    expectedTokens: ['期末结账报告', '分组', '项目'],
  },
  // === manufacturing (3) ===
  {
    domain: 'mfg', reportName: 'crp-load-report',
    data: { workcenterId: 1, startDate: '2026-07-15', endDate: '2026-07-15' },
    expectedTokens: ['CRP 工作中心负荷分析表'],
  },
  {
    domain: 'mfg', reportName: 'production-variance-report',
    data: {},
    expectedTokens: ['生产差异分析表'],
  },
  {
    domain: 'mfg', reportName: 'forecast-variance-report',
    data: {},
    expectedTokens: ['需求预测差异分析表'],
  },
  // === assets (2) ===
  {
    domain: 'ast', reportName: 'asset-depreciation-detail',
    data: {},
    expectedTokens: ['资产折旧明细表', '累计折旧'],
  },
  {
    domain: 'ast', reportName: 'asset-disposal-detail',
    data: {},
    expectedTokens: ['资产处置明细表', '处置类型', '清理损益'],
  },
  // === maintenance (2) ===
  {
    domain: 'mnt', reportName: 'maintenance-history',
    data: {},
    expectedTokens: ['维护历史表'],
  },
  {
    domain: 'mnt', reportName: 'downtime-summary',
    data: { equipmentId: 1 },
    expectedTokens: ['停机统计表', '设备名称'],
  },
  // === projects (2) ===
  {
    domain: 'prj', reportName: 'project-cost-summary',
    data: {},
    expectedTokens: ['项目成本汇总表'],
  },
  {
    domain: 'prj', reportName: 'timesheet-detail',
    data: { projectId: 1 },
    expectedTokens: ['工时明细表'],
  },
  // === quality (2) ===
  {
    domain: 'qa', reportName: 'inspection-summary',
    data: {},
    expectedTokens: ['质检合格率统计表', '产品甲'],
  },
  {
    domain: 'qa', reportName: 'ncr-capa-summary',
    data: {},
    expectedTokens: ['NCR-CAPA 统计表'],
  },
  // === master-data (2) ===
  {
    domain: 'md', reportName: 'material-price-list',
    data: {},
    expectedTokens: ['物料价格清单'],
  },
  {
    domain: 'md', reportName: 'partner-list',
    data: {},
    expectedTokens: ['往来单位清单'],
  },
  // === inventory (1) ===
  {
    domain: 'inv', reportName: 'inventory-trace-report',
    data: {},
    expectedTokens: ['库存追溯链可视化报表'],
  },
  // === cs (1) ===
  {
    domain: 'cs', reportName: 'ticket-sla-csat-summary',
    data: { ticketType: '1' },
    expectedTokens: ['工单 SLA/CSAT 综合统计表', '投诉'],
  },
  // === crm (2) ===
  {
    domain: 'crm', reportName: 'lead-conversion-funnel',
    data: {},
    expectedTokens: ['线索转化漏斗表', '验证', '报价'],
  },
  {
    domain: 'crm', reportName: 'forecast-accuracy',
    data: { forecastId: 1 },
    expectedTokens: ['销售预测准确率表'],
  },
  // === hr (2) ===
  {
    domain: 'hr', reportName: 'employee-net-balance',
    data: {},
    expectedTokens: ['员工净余额报表'],
  },
  {
    domain: 'hr', reportName: 'payroll-simulation-comparison',
    data: { simulationId: 1 },
    expectedTokens: ['薪酬模拟对比报表'],
  },
];

const RENDER_TYPES: Array<'xlsx' | 'pdf'> = ['xlsx', 'pdf'];

test.describe('report download runtime regression (24 reports × {xlsx,pdf})', () => {
  for (const c of REPORT_DOWNLOAD_CASES) {
    for (const rt of RENDER_TYPES) {
      assertReportDownload({
        domain: c.domain,
        reportName: c.reportName,
        renderType: rt,
        data: c.data,
        expectedTokens: c.expectedTokens,
      });
    }
  }
});
