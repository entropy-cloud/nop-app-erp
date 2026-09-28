#!/usr/bin/env node
// enum-unbounded-queries.mjs — GATE-04 replayable enumeration of unbounded query sites (A9 family).
//
// Owner doc : docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md §2.2 A9
// Source    : docs/backlog/perf-ux-debt-consolidation-roadmap.md GATE-04
// Plan      : docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md
// Paradigm  : docs/lessons/20 (full scan / anti-fake-green self-test / fail-loud / one-way ledger)
//
// Scan universe: src/main *DashboardBizModel.java / *ReportBizModel.java (excluding _gen/, /target/,
// "_"-prefixed basenames) — the A9 governance surface; 1418-4's missed 4 sites all live here.
//
// A call site is UNBOUNDED (Decision-1, two-level, conservative-toward-false-positive) unless:
//   (a) its own statement contains setLimit( or .limit(, OR
//   (b) the query variable flowing into the call receives <var>.setLimit( / <var>.limit(
//       anywhere within its enclosing method.
// Known accepted false-positive: builder helpers (setLimit inside a helper method) — 1 site at
// freeze, ledgered as 1418-4-cap.
//
// Modes:
//   (default)  diff: live unbounded sites vs LEDGER; new site => exit 1; vanished => IMPROVEMENTS
//              (report only — never auto-rewrites the ledger)
//   --freeze   print an annotated-draft ledger to stdout (empty LEDGER skips ledger validation)
//   --self-test in-memory fault injection (exit 0/1)
//
// LEDGER mechanical validation (lesson 20 element 4, fail-loud): source must be in the closed
// enum, adjudication pointer non-empty, key format legal — violations exit 2. The ledger carries
// human-annotated columns (source/shape/pointer), so it is NOT a pure function of the repo tree
// and cannot be wholesale regenerated like cjk-baseline: the git-diff crosscheck against
// adjudication pointers is the only tamper防线 (Closure Gates item).
//
// Exit codes: 0 = clean/pass, 1 = new unbounded sites or self-test fail, 2 = operational failure.
//
// Diff object = sites judged unbounded by Decision-1 (bounded sites are filtered before diff).

import { readFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import { execFile } from 'child_process';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

// ---------------------------------------------------------------------------
// LEDGER (frozen 2026-09-28 at plan 2026-09-28-1745-1 Phase 1; annotate-only growth)
// key: file :: methodName :: normalizedExcerpt
// cols: source (closed enum) | shape | pointer (文件#锚点)
// source enum: batch2-p1-4-agg | phase5-agg | phase5-exempt | 1418-4-exempt | 1418-4-cap |
//              positive-template | D01-deferred | freeze-unadjudicated
// ---------------------------------------------------------------------------
const SOURCE_ENUM = ['batch2-p1-4-agg', 'phase5-agg', 'phase5-exempt', '1418-4-exempt', '1418-4-cap', 'positive-template', 'D01-deferred', 'freeze-unadjudicated'];

// Per-file source defaults derived from the plan's F-1-verified composition; per-site overrides
// follow. `pointer` cites the adjudication anchor; freeze-unadjudicated points at this plan's
// freeze record.
const FILE_SOURCE_DEFAULTS = {
  'module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java': ['batch2-p1-4-agg', '分组投影/SUM 聚合', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3'],
  'module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java': ['batch2-p1-4-agg', '分组投影/SUM 聚合', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3'],
  'module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java': ['batch2-p1-4-agg', '分组投影/SUM 聚合', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3'],
  'module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java': ['batch2-p1-4-agg', '分组投影/SUM 聚合', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3'],
  'module-assets/erp-ast-service/src/main/java/app/erp/ast/service/dashboard/ErpAstDashboardBizModel.java': ['phase5-agg', 'GROUP BY 投影聚合', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区①'],
  'module-assets/erp-ast-service/src/main/java/app/erp/ast/service/report/ErpAstReportBizModel.java': ['phase5-agg', '投影聚合/cap', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区'],
  'module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/report/ErpInvReportBizModel.java': ['phase5-agg', 'in() 有界跟随/批载', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区①'],
  'module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/report/ErpMfgReportBizModel.java': ['phase5-agg', 'cap/in() 有界', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区②'],
  'module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java': ['phase5-exempt', 'in(id) 有界跟随', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③'],
  'module-cs/erp-cs-service/src/main/java/app/erp/cs/service/report/ErpCsReportBizModel.java': ['phase5-exempt', 'in(id) 有界跟随', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③'],
  'module-crm/erp-crm-service/src/main/java/app/erp/crm/service/report/ErpCrmReportBizModel.java': ['phase5-exempt', 'in(id) 有界/配置小表', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③'],
  'module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java': ['phase5-exempt', 'eq(simulationId)/in(id) 有界', 'docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③'],
  'module-projects/erp-prj-service/src/main/java/app/erp/prj/service/report/ErpPrjReportBizModel.java': ['1418-4-exempt', '主数据小表/分组投影', 'docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#Non-Goals与F-2'],
  'module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/report/ErpMntReportBizModel.java': ['1418-4-exempt', 'in(visitId) 有界跟随(cap 父集)', 'docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-3'],
  'module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java': ['1418-4-cap', 'chartId 单列投影+内存去重', 'docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-4'],
  'module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java': ['1418-4-cap', 'cap 父集有界跟随/buildClosedTicketQuery', 'docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-1'],
  'module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java': ['freeze-unadjudicated', '期间状态小表/期末结账数据集计数——冻结时未裁决', 'docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录'],
};

// Per-site source overrides within the files above (methodName-substring match).
const SITE_OVERRIDES = [
  { file: 'module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java', method: /openItemsQuery|loadPostedVoucherLineSet|Aging|aging/i, source: 'D01-deferred', shape: '现金流/账龄全量物化（D-01 触发驱动）', pointer: 'docs/backlog/perf-ux-debt-consolidation-roadmap.md#D-01' },
  { file: 'module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java', method: /^\(no-method\)$/, excerpt: /openItemsQuery/, source: 'D01-deferred', shape: '账龄 openItemsQuery 物化（D-01 触发驱动）', pointer: 'docs/backlog/perf-ux-debt-consolidation-roadmap.md#D-01' },
  { file: 'module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java', method: /loadGlBalances/, source: 'positive-template', shape: 'SQL 聚合正面范式（分析报告 §2.4 明记不需要动）', pointer: 'docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md#2.4' },
];

// NOTE(2026-09-28 复审计 r-1): 下方 LEDGER 为冻结终态；FILE_SOURCE_DEFAULTS/SITE_OVERRIDES
// 种子表仅服务 --freeze 草案生成、相对冻结台账已过期——未来重生成草案须以 LEDGER 为准人工重对。
const LEDGER = [
  '- module-assets/erp-ast-service/src/main/java/app/erp/ast/service/dashboard/ErpAstDashboardBizModel.java :: loadCategoryNames :: for (ErpAstAssetCategory c : dao.findAllByQuery(q)) { | source=phase5-agg | shape=GROUP BY 投影聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区①',
  '- module-assets/erp-ast-service/src/main/java/app/erp/ast/service/report/ErpAstReportBizModel.java :: (no-method) :: List<ErpAstDepreciationSchedule> schedules = daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q); | source=phase5-agg | shape=投影聚合/cap | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区',
  '- module-assets/erp-ast-service/src/main/java/app/erp/ast/service/report/ErpAstReportBizModel.java :: resolveCategoryNames :: List<ErpAstAssetCategory> cats = daoProvider.daoFor(ErpAstAssetCategory.class).findAllByQuery(q); | source=phase5-agg | shape=投影聚合/cap | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区',
  '- module-crm/erp-crm-service/src/main/java/app/erp/crm/service/report/ErpCrmReportBizModel.java :: resolveCampaignNames :: List<ErpCrmCampaign> campaigns = daoProvider.daoFor(ErpCrmCampaign.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界/配置小表 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-crm/erp-crm-service/src/main/java/app/erp/crm/service/report/ErpCrmReportBizModel.java :: loadForecasts :: return daoProvider.daoFor(ErpCrmForecast.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界/配置小表 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-crm/erp-crm-service/src/main/java/app/erp/crm/service/report/ErpCrmReportBizModel.java :: aggregateForecastLines :: List<ErpCrmForecastLine> lines = daoProvider.daoFor(ErpCrmForecastLine.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界/配置小表 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-crm/erp-crm-service/src/main/java/app/erp/crm/service/report/ErpCrmReportBizModel.java :: resolveStageNames :: List<ErpCrmStage> stages = daoProvider.daoFor(ErpCrmStage.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界/配置小表 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java :: loadClosedTickets :: return daoProvider.daoFor(ErpCsTicket.class).findAllByQuery(q); | source=1418-4-cap | shape=cap 父集有界跟随/buildClosedTicketQuery | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-1',
  '- module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java :: loadSlaPolicyTeamMap :: List<ErpCsSlaPolicy> policies = daoProvider.daoFor(ErpCsSlaPolicy.class).findAllByQuery(q); | source=1418-4-cap | shape=cap 父集有界跟随/buildClosedTicketQuery | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-1',
  '- module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java :: loadTeamNames :: List<ErpCsTeam> teams = daoProvider.daoFor(ErpCsTeam.class).findAllByQuery(q); | source=1418-4-cap | shape=cap 父集有界跟随/buildClosedTicketQuery | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-1',
  '- module-cs/erp-cs-service/src/main/java/app/erp/cs/service/dashboard/ErpCsQualityDashboardBizModel.java :: loadSurveyByTicket :: List<ErpCsSurvey> surveys = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q); | source=1418-4-cap | shape=cap 父集有界跟随/buildClosedTicketQuery | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-1',
  '- module-cs/erp-cs-service/src/main/java/app/erp/cs/service/report/ErpCsReportBizModel.java :: aggregateSurveys :: List<ErpCsSurvey> surveys = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界跟随 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-cs/erp-cs-service/src/main/java/app/erp/cs/service/report/ErpCsReportBizModel.java :: resolveTicketTypeNames :: daoProvider.daoFor(ErpCsTicketType.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界跟随 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java :: loadGlBalances :: return dao.findAllByQuery(q); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java :: loadGlBalances :: return dao.findAllByQuery(q); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java :: loadGlBalancesInRange :: List<ErpFinAccountingPeriod> periods = pDao.findAllByQuery(pq); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java :: loadGlBalancesInRange :: return dao.findAllByQuery(q); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java :: aggPnlActivityBySubjectPeriod :: List<ErpFinAccountingPeriod> periods = pDao.findAllByQuery(pq); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java :: loadSubjects :: for (ErpMdSubject s : dao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: (no-method) :: List<ErpFinArApItem> items = dao.findAllByQuery(openItemsQuery()); | source=D01-deferred | shape=账龄 openItemsQuery 物化（D-01 触发驱动） | pointer=docs/backlog/perf-ux-debt-consolidation-roadmap.md#D-01',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: loadGlBalances :: list = dao.findAllByQuery(q); | source=positive-template | shape=SQL 聚合正面范式（分析报告 §2.4 明记不需要动） | pointer=docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md#2.4',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: loadGlBalances :: list = dao.findAllByQuery(q); | source=positive-template | shape=SQL 聚合正面范式（分析报告 §2.4 明记不需要动） | pointer=docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md#2.4',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: loadPostedVoucherLineSet :: List<ErpFinVoucher> vouchers = vDao.findAllByQuery(vq); | source=D01-deferred | shape=现金流/账龄全量物化（D-01 触发驱动） | pointer=docs/backlog/perf-ux-debt-consolidation-roadmap.md#D-01',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: loadPostedVoucherLineSet :: List<ErpFinVoucherLine> lines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq); | source=D01-deferred | shape=现金流/账龄全量物化（D-01 触发驱动） | pointer=docs/backlog/perf-ux-debt-consolidation-roadmap.md#D-01',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: loadPeriodStatus :: daoProvider.daoFor(ErpFinAccountingPeriodStatus.class).findAllByQuery(q); | source=freeze-unadjudicated | shape=期间状态小表/期末结账数据集计数——冻结时未裁决 | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: countBillR :: List<ErpFinVoucher> vouchers = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq); | source=freeze-unadjudicated | shape=期间状态小表/期末结账数据集计数——冻结时未裁决 | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java :: countBillR :: return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(bq).size(); | source=freeze-unadjudicated | shape=期间状态小表/期末结账数据集计数——冻结时未裁决 | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java :: resolvePartnerNames :: for (ErpMdPartner p : dao.findAllByQuery(q)) { | source=phase5-exempt | shape=eq(simulationId)/in(id) 有界 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java :: loadAdjustments :: return daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class).findAllByQuery(q); | source=phase5-exempt | shape=eq(simulationId)/in(id) 有界 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java :: loadEmployees :: List<ErpHrEmployee> employees = dao.findAllByQuery(q); | source=phase5-exempt | shape=eq(simulationId)/in(id) 有界 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java :: findBatchExpiryAlert :: List<ErpInvBatch> batches = dao.findAllByQuery(q); | source=batch2-p1-4-agg | shape=expiryDate 窗口批次全行物化（预警列表） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java :: loadDoneMovesInRange :: return dao.findAllByQuery(q); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java :: loadSafetyStock :: for (ErpMdMaterial m : dao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java :: loadMaterialNames :: for (ErpMdMaterial m : dao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java :: loadWarehouseNames :: for (ErpMdWarehouse w : dao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/dashboard/ErpInvDashboardBizModel.java :: loadLastOutgoingDates :: List<ErpInvStockMove> moves = mDao.findAllByQuery(mq); | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/report/ErpInvReportBizModel.java :: findCandidateMoves :: for (ErpInvStockMoveLine line : daoProvider.daoFor(ErpInvStockMoveLine.class).findAllByQuery(lineQ)) { | source=phase5-agg | shape=in() 有界跟随/批载 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区①',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/report/ErpInvReportBizModel.java :: findCandidateMoves :: return daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q); | source=phase5-agg | shape=in() 有界跟随/批载 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区①',
  '- module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/report/ErpInvReportBizModel.java :: toTraceRows :: for (ErpInvStockMoveLine line : lineDao.findAllByQuery(batchQ)) { | source=phase5-agg | shape=in() 有界跟随/批载 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区①',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java :: loadEquipmentsNotDecommissioned :: return dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=ne(status) 过滤设备主数据列表（量级=在册设备数） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java :: loadEquipmentsByStatus :: return dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=eq(status) 过滤设备主数据列表（量级=在册设备数） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java :: loadEquipmentIdsWithOngoingDowntime :: for (ErpMntDowntimeEntry d : dao.findAllByQuery(q)) { | source=freeze-unadjudicated | shape=eq(isActive) 进行中停机记录（增长面但现量级小——watch-only） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/dashboard/ErpMntDashboardBizModel.java :: loadActiveSchedules :: return dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=eq(isActive) 活动排程列表（量级=活动排程数） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/report/ErpMntReportBizModel.java :: countTasksByVisit :: List<ErpMntVisitTask> tasks = daoProvider.daoFor(ErpMntVisitTask.class).findAllByQuery(q); | source=1418-4-exempt | shape=in(visitId) 有界跟随(cap 父集) | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-3',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/report/ErpMntReportBizModel.java :: countUsagesByVisit :: List<ErpMntSparePartUsage> usages = daoProvider.daoFor(ErpMntSparePartUsage.class).findAllByQuery(q); | source=1418-4-exempt | shape=in(visitId) 有界跟随(cap 父集) | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-3',
  '- module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/report/ErpMntReportBizModel.java :: resolveEquipmentNamesByIds :: List<ErpMntEquipment> list = daoProvider.daoFor(ErpMntEquipment.class).findAllByQuery(q); | source=1418-4-exempt | shape=in(visitId) 有界跟随(cap 父集) | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#F-3',
  '- module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java :: findDelayedWorkOrderAlert :: List<ErpMfgWorkOrder> orders = dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=预警列表无 cap（同形 findCapaOverdueAlert 于 1418-4 拿 cap——cap 候选形） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java :: sumCompletedQtyInRange :: for (ErpMfgWorkOrder o : dao.findAllByQuery(q)) { | source=freeze-unadjudicated | shape=日期可选范围全量内存求和（cap/聚合候选形） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java :: computeOnTimeRate :: List<ErpMfgWorkOrder> completed = dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=**cap 候选**：全部历史完工工单无日期界无 cap——与 0325-2 已裁决 cap+D-12 的 pur onTimeRate 同形同源 | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java :: loadCompletedInRange :: return dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=日期可选的完工工单明细列表（cap 候选形） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/report/ErpMfgReportBizModel.java :: aggregateForecastQty :: List<ErpMfgForecastLine> lines = daoProvider.daoFor(ErpMfgForecastLine.class).findAllByQuery(q); | source=phase5-agg | shape=cap/in() 有界 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区②',
  '- module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/report/ErpMfgReportBizModel.java :: loadApprovedForecasts :: return daoProvider.daoFor(ErpMfgForecast.class).findAllByQuery(q); | source=phase5-agg | shape=cap/in() 有界 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区②',
  '- module-master-data/erp-md-service/src/main/java/app/erp/md/service/report/ErpMdReportBizModel.java :: (no-method) :: List<ErpMdPartner> partners = daoProvider.daoFor(ErpMdPartner.class).findAllByQuery(q); | source=freeze-unadjudicated | shape=**cap 候选**：增长主数据表 partner 明细报表无 cap（同形 ast loadAssets 为 ②类 cap） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-master-data/erp-md-service/src/main/java/app/erp/md/service/report/ErpMdReportBizModel.java :: loadMaterials :: return daoProvider.daoFor(ErpMdMaterial.class).findAllByQuery(q); | source=freeze-unadjudicated | shape=物料主数据全列表（增长主数据表——cap 候选形） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-master-data/erp-md-service/src/main/java/app/erp/md/service/report/ErpMdReportBizModel.java :: loadDefaultSkus :: List<ErpMdMaterialSku> skus = daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q); | source=freeze-unadjudicated | shape=默认 SKU 主数据列表（量级=物料数） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java :: findDelayedProjectAlert :: List<ErpPrjProject> projects = dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=**cap 候选**：预警列表无 cap（同形 findCapaOverdueAlert 于 1418-4 拿 cap） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java :: (no-method) :: List<ErpPrjProjectPnl> rows = dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=**cap 候选**：PnL 快照表「每日一快照」增长面全量物化+内存去重（代码注释自认增长面） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java :: loadOpenProjects :: return dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=eq/open 状态过滤项目主表列表（量级=在用项目数） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java :: sumBudgetForProjects :: for (ErpPrjBudget b : dao.findAllByQuery(q)) { | source=freeze-unadjudicated | shape=in(projectIds) 有界跟随（开放项目集） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java :: sumCostForProjects :: for (ErpPrjCostCollection c : dao.findAllByQuery(q)) { | source=freeze-unadjudicated | shape=in(projectIds) 有界跟随（开放项目集） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-projects/erp-prj-service/src/main/java/app/erp/prj/service/report/ErpPrjReportBizModel.java :: loadProjects :: return daoProvider.daoFor(ErpPrjProject.class).findAllByQuery(q); | source=1418-4-exempt | shape=主数据小表/分组投影 | pointer=docs/plans/2026-09-28-1418-4-a9-addendum-unbounded-queries.md#Non-Goals与F-2',
  '- module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java :: findThreeWayMatchDiffAlert :: for (ErpPurReceiveLine rl : rlDao.findAllByQuery(rlq)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java :: findThreeWayMatchDiffAlert :: for (ErpPurOrderLine ol : olDao.findAllByQuery(olq)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java :: loadPartnerNames :: for (ErpMdPartner p : partnerDao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java :: loadInvoiceLinesByInvoice :: for (ErpPurInvoiceLine il : ilDao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
  '- module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/dashboard/ErpPurDashboardBizModel.java :: computeOnTimeRate :: List<ErpPurReceive> receives = rDao.findAllByQuery(rq); | source=batch2-p1-4-agg | shape=receive 全量枚举内存比对（0325-2 Decision 保留内存比对+cap 于 order 侧；聚合重写 Deferred=D-12） | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Decision-D-12',
  '- module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java :: loadSpcSamples :: List<ErpQaSpcSample> raw = dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=eq(chartId) 单图全行样本清单（1418-4 F-4 仅裁决投影计数三站，本站未经裁决） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-quality/erp-qa-service/src/main/java/app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java :: loadInspectionsInRange :: return dao.findAllByQuery(q); | source=freeze-unadjudicated | shape=日期窗全量检验单列表（1418-4 F-4 仅裁决投影计数三站，本站未经裁决） | pointer=docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录',
  '- module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java :: countActionsByNcr :: List<ErpQaAction> actions = daoProvider.daoFor(ErpQaAction.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界跟随 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-quality/erp-qa-service/src/main/java/app/erp/qa/service/report/ErpQaReportBizModel.java :: resolveMaterialNames :: List<ErpMdMaterial> materials = daoProvider.daoFor(ErpMdMaterial.class).findAllByQuery(q); | source=phase5-exempt | shape=in(id) 有界跟随 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase5留痕区③',
  '- module-sales/erp-sal-service/src/main/java/app/erp/sal/service/dashboard/ErpSalDashboardBizModel.java :: loadPartnerNames :: for (ErpMdPartner p : partnerDao.findAllByQuery(q)) { | source=batch2-p1-4-agg | shape=分组投影/SUM 聚合 | pointer=docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md#Phase1-3',
];
// Populate LEDGER from defaults + overrides at scan time (see buildLedgerEntry).

// ---------------------------------------------------------------------------
// parsing helpers
// ---------------------------------------------------------------------------

const CALL_RE = /findAllByQuery\(|\.findList\(|\.findAll\(/;

function normalizeExcerpt(text) {
  return text.replace(/\s+/g, ' ').trim();
}

function methodRanges(lines) {
  const ranges = [];
  let cur = null, depth = 0;
  for (let i = 0; i < lines.length; i++) {
    const l = lines[i];
    if (!cur && /^\s+(public|private|protected)\s+[\w<>,\[\]\s]+\s+\w+\s*\(/.test(l) && !l.trim().endsWith(';')) {
      cur = { name: (l.match(/(\w+)\s*\(/) || [])[1] || '?', start: i, depth: (l.match(/\{/g) || []).length - (l.match(/\}/g) || []).length };
      if (cur.depth <= 0) { ranges.push({ ...cur, end: i }); cur = null; }
    } else if (cur) {
      cur.depth += (l.match(/\{/g) || []).length - (l.match(/\}/g) || []).length;
      if (cur.depth <= 0) { ranges.push({ ...cur, end: i }); cur = null; }
    }
  }
  return ranges;
}

function statementOf(lines, callIdx) {
  // walk back to the nearest line ending a statement ({, }, ; at depth-0 approximated by trim)
  let start = callIdx;
  for (let i = callIdx; i >= 0 && i > callIdx - 40; i--) {
    const t = lines[i].trim();
    start = i;
    if (i !== callIdx && /[;{}]$/.test(t)) break;
  }
  return lines.slice(start, callIdx + 1).join('\n');
}

function varsInCall(callText) {
  const vars = new Set();
  // receiver before .findList( / .findAll(
  const recv = callText.match(/(\w+)\s*\.\s*(?:findList|findAll)\s*\(/);
  if (recv) vars.add(recv[1]);
  // arguments of findAllByQuery( ... ) — take trailing identifier args
  const args = callText.match(/findAllByQuery\s*\(([^)]*)\)/);
  if (args) {
    for (const m of args[1].matchAll(/\b([a-z]\w*)\b/g)) vars.add(m[1]);
  }
  vars.delete('dao');
  vars.delete('orm');
  return [...vars];
}

/** Decision-1 two-level boundedness check for one call site. */
function isBounded(lines, ranges, callIdx) {
  const stmt = statementOf(lines, callIdx);
  if (/setLimit\s*\(|\.limit\s*\(/.test(stmt)) return true; // (a) statement-level
  const range = ranges.find(r => callIdx >= r.start && callIdx <= r.end);
  if (!range) return false; // top-level (no method context) => unbounded
  const body = lines.slice(range.start, range.end + 1).join('\n');
  const callText = lines[callIdx];
  for (const v of varsInCall(stmt + '\n' + callText)) {
    const re = new RegExp(`\\b${v}\\s*\\.\\s*(?:setLimit|limit)\\s*\\(`);
    if (re.test(body)) return true; // (b) variable-level within method
  }
  return false;
}

async function listUniverseFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '-co', '--exclude-standard'], {
    cwd: rootDir,
    maxBuffer: 64 * 1024 * 1024,
  });
  return stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
    .filter(f => f.includes('/src/main/java/') && !f.includes('_gen') && !f.includes('/target/'))
    .filter(f => /(?:DashboardBizModel|ReportBizModel)\.java$/.test(f))
    .filter(f => !path.basename(f).startsWith('_'))
    .sort();
}

async function scanUniverse() {
  const files = await listUniverseFiles();
  const sites = [];
  for (const f of files) {
    const content = await readFile(path.join(rootDir, f), 'utf8');
    const lines = content.split('\n');
    const ranges = methodRanges(lines);
    for (let i = 0; i < lines.length; i++) {
      if (!CALL_RE.test(lines[i])) continue;
      if (isBounded(lines, ranges, i)) continue;
      const range = ranges.find(r => i >= r.start && i <= r.end);
      sites.push({
        file: f,
        line: i + 1,
        method: range ? range.name : '(no-method)',
        excerpt: normalizeExcerpt(lines[i]).slice(0, 120),
      });
    }
  }
  return { files, sites };
}

function ledgerKey(site) {
  return `${site.file} :: ${site.method} :: ${site.excerpt}`;
}

function resolveAnnotation(site) {
  let ann = null;
  for (const o of SITE_OVERRIDES) {
    if (o.file !== site.file) continue;
    if (o.method && !o.method.test(site.method)) continue;
    if (o.excerpt && !o.excerpt.test(site.excerpt)) continue;
    ann = { source: o.source, shape: o.shape, pointer: o.pointer };
    break;
  }
  if (!ann) {
    const def = FILE_SOURCE_DEFAULTS[site.file];
    if (def) ann = { source: def[0], shape: def[1], pointer: def[2] };
  }
  if (!ann) ann = { source: 'freeze-unadjudicated', shape: '（未映射文件——冻结时人工归档）', pointer: 'docs/plans/2026-09-28-1745-1-gate04-enum-unbounded-queries.md#冻结记录' };
  return ann;
}

// ---------------------------------------------------------------------------
// LEDGER loading + mechanical validation
// ---------------------------------------------------------------------------

function parseLedgerText(text) {
  // entries: "- file :: method :: excerpt | source=<src> | shape=<shape> | pointer=<ptr>"
  // Malformed ledger-looking lines are RETURNED, never silently dropped (lesson 20 fail-loud).
  const entries = [];
  const malformed = [];
  for (const raw of text.split('\n')) {
    if (!/^\s*-\s+/.test(raw)) continue;
    const m = raw.match(/^\s*-\s+(.+?)\s*\|\s*source=(\S+)\s*\|\s*shape=(.*?)\s*\|\s*pointer=(\S.*)\s*$/);
    if (!m) { malformed.push(raw.trim().slice(0, 90)); continue; }
    entries.push({ key: m[1].trim(), source: m[2], shape: m[3].trim(), pointer: m[4].trim() });
  }
  return { entries, malformed };
}

function validateLedger(parsed) {
  const errors = parsed.malformed.map(l => `malformed ledger line (dropped entries are silent holes): ${l}`);
  const entries = parsed.entries;
  for (const e of entries) {
    if (!e.key.includes(' :: ')) errors.push(`illegal key format: ${e.key.slice(0, 80)}`);
    if (!SOURCE_ENUM.includes(e.source)) errors.push(`source "${e.source}" not in closed enum: ${e.key.slice(0, 60)}`);
    if (!e.shape) errors.push(`empty shape: ${e.key.slice(0, 60)}`);
    if (!e.pointer || !e.pointer.includes('#')) errors.push(`adjudication pointer must be 文件#锚点: ${e.key.slice(0, 60)}`);
  }
  return errors;
}

function currentLedger() {
  return parseLedgerText(LEDGER.join('\n'));
}

// ---------------------------------------------------------------------------
// self-test
// ---------------------------------------------------------------------------

export function runSelfTest() {
  let pass = true;
  const expect = (name, cond) => { if (!cond) { console.log(`  FAIL  ${name}`); pass = false; } else console.log(`  PASS  ${name}`); };
  const mk = (lines) => {
    const ranges = methodRanges(lines);
    const out = [];
    lines.forEach((l, i) => { if (CALL_RE.test(l) && !isBounded(lines, ranges, i)) out.push(i + 1); });
    return out;
  };

  expect('statement setLimit => bounded',
    mk(['class A {', '  void m() {', '    QueryBean q = QueryBean.of(X.class);', '    q.setLimit(5);', '    List<X> l = dao().findAllByQuery(q);', '  }', '}']).length === 0);
  expect('chained .limit( => bounded',
    mk(['class A {', '  void m() {', '    List<X> l = dao().findAllByQuery(QueryBean.of(X.class).limit(5));', '  }', '}']).length === 0);
  expect('variable-level setLimit in method => bounded',
    mk(['class A {', '  void m(boolean all) {', '    QueryBean q = QueryBean.of(X.class);', '    if (all) q.setLimit(CAP);', '    List<X> l = dao().findAllByQuery(q);', '  }', '}']).length === 0);
  expect('plain unbounded call => flagged',
    mk(['class A {', '  void m() {', '    QueryBean q = QueryBean.of(X.class);', '    List<X> l = dao().findAllByQuery(q);', '  }', '}']).length === 1);
  expect('different second query variable without setLimit => flagged (no method-level blind spot)',
    mk(['class A {', '  void m() {', '    QueryBean q = QueryBean.of(X.class);', '    q.setLimit(5);', '    List<X> a = dao().findAllByQuery(q);', '    QueryBean q2 = QueryBean.of(Y.class);', '    List<Y> b = dao().findAllByQuery(q2);', '  }', '}']).length === 1);
  expect('top-level call without method context => flagged',
    mk(['List<X> l = dao().findAllByQuery(QueryBean.of(X.class));']).length === 1);
  expect('builder helper form (setLimit inside helper) => conservatively flagged (Decision-1 known false-positive)',
    mk(['class A {', '  QueryBean buildQ() {', '    QueryBean q = QueryBean.of(X.class);', '    q.setLimit(CAP);', '    return q;', '  }', '  void m() {', '    QueryBean q = buildQ();', '    List<X> l = dao().findAllByQuery(q);', '  }', '}']).length === 1);

  // ledger validation (fail-loud: malformed lines are errors, never silently dropped)
  expect('ledger: source outside closed enum detected',
    validateLedger(parseLedgerText('- a :: m :: x | source=bogus | shape=s | pointer=f#p')).length === 1);
  expect('ledger: missing pointer detected as malformed line',
    validateLedger(parseLedgerText('- a :: m :: x | source=phase5-agg | shape=s | pointer=')).length === 1);
  expect('ledger: well-formed entry passes',
    validateLedger(parseLedgerText('- a :: m :: x | source=phase5-agg | shape=s | pointer=doc#sec')).length === 0);
  expect('ledger: shape column empty detected',
    validateLedger(parseLedgerText('- a :: m :: x | source=phase5-agg | shape= | pointer=f#p')).length === 1);

  console.log(pass ? '[enum-unbounded-queries] RESULT: PASS (self-test green)' : '[enum-unbounded-queries] RESULT: FAIL (self-test detected fake-green risk)');
  return pass ? 0 : 1;
}

// ---------------------------------------------------------------------------
// main
// ---------------------------------------------------------------------------

async function main() {
  const args = process.argv.slice(2);
  if (args.includes('--self-test')) process.exit(runSelfTest());

  let scan;
  try {
    scan = await scanUniverse();
  } catch (error) {
    console.error('[enum-unbounded-queries] scan failed:', error.message);
    process.exit(2);
  }

  const ledgerParsed = currentLedger();
  const ledger = ledgerParsed.entries;
  {
    const errs = validateLedger(ledgerParsed);
    if (errs.length) {
      console.error('[enum-unbounded-queries] LEDGER mechanical validation failed (fail-loud, lesson 20 element 4):');
      for (const e of errs.slice(0, 20)) console.error('  ' + e);
      process.exit(2);
    }
  }

  console.log('='.repeat(72));
  console.log('enum-unbounded-queries  (GATE-04 — A9 universe: *DashboardBizModel/*ReportBizModel)');
  console.log('='.repeat(72));
  console.log(`scanned universe files : ${scan.files.length}`);
  console.log(`unbounded sites (live) : ${scan.sites.length}`);

  if (args.includes('--freeze')) {
    console.log('\n--- FREEZE DRAFT (annotate source/shape/pointer then paste back into LEDGER) ---');
    for (const site of scan.sites) {
      const ann = resolveAnnotation(site);
      console.log(`- ${ledgerKey(site)} | source=${ann.source} | shape=${ann.shape} | pointer=${ann.pointer}`);
    }
    process.exit(0);
  }

  const ledgerKeys = new Set(ledger.map(e => e.key));
  const newSites = scan.sites.filter(s => !ledgerKeys.has(ledgerKey(s)));
  const liveKeys = new Set(scan.sites.map(ledgerKey));
  const vanished = ledger.filter(e => !liveKeys.has(e.key));
  void ledgerParsed;

  if (newSites.length) {
    console.log('-'.repeat(72));
    console.log(`RESULT: FAIL (${newSites.length} NEW unbounded site(s) not in ledger — A9-style enumeration gap)`);
    for (const s of newSites.slice(0, 50)) {
      console.log(`  NEW ${s.file}:${s.line} :: ${s.method} :: ${s.excerpt}`);
    }
    if (newSites.length > 50) console.log(`  ... (${newSites.length - 50} more)`);
    process.exit(1);
  }
  if (vanished.length) {
    console.log(`IMPROVEMENTS (ledger sites no longer present — tighten via annotated ledger edit, never auto-rewritten): ${vanished.length}`);
    for (const e of vanished.slice(0, 20)) console.log(`  IMPROVED ${e.key}`);
  }
  console.log(`RESULT: PASS (all ${scan.sites.length} live unbounded sites are in the ledger; ledger entries=${ledger.length})`);
  process.exit(0);
}

main().catch((error) => {
  console.error('[enum-unbounded-queries] Error:', error);
  process.exit(2);
});
