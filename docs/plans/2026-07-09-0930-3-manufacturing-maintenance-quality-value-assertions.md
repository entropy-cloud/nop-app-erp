# 2026-07-09-0930-3-manufacturing-maintenance-quality-value-assertions 制造/维护/质量域看板+报表数据驱动数值断言

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-08-2210-2-operational-domain-value-assertions.md` Deferred「其他扩展域看板/报表数值断言（manufacturing/quality/maintenance/...）」（Successor Required: yes，触发条件「当对应扩展域交易种子 seed（N=1 Deferred 后续批次）后，按域逐批补数值断言」——**已满足**：N=1 `2026-07-09-0930-1` 制造域种子 + N=2 `2026-07-09-0930-2` 维护/质量域种子固化三域交易种子）；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-2210-2-operational-domain-value-assertions.md`（completed，数值断言范式 + helper，本计划复用）、`docs/plans/2026-07-09-0930-1-manufacturing-transaction-seeds.md`（同批 N=1，本计划制造域数据层前置）、`docs/plans/2026-07-09-0930-2-maintenance-quality-transaction-seeds.md`（同批 N=2，本计划维护/质量域数据层前置）
> Audit: required

## Current Baseline

实时仓库逐项核实（`ls`/`rg`/`read`，非采信旧记忆）：

- **数值断言范式已建立（1445-2 / 2210-2 交付）**：`tests/e2e/dashboards/_helper.ts:40` `assertDashboardKpiValues(cfg: DashboardKpiAssertion)`——直接 `page.request.post('/graphql')` 取 `getDashboardKpi` 原始 Map，逐字段 `expect(actual === expected)` 断言（非 AMIS DOM 文本解析，规避千分位/币种符号抖动）；`tests/e2e/reports/_helper.ts:58` `assertReportRenderedWithValue(cfg: ReportValueAssertion)`——`renderHtml` 取 HTML，剥离千分位后断言含期望数值 token。两 helper 接口就绪：
  - `DashboardKpiAssertion { domain, route, query, variables, responseKey, expected: Record<string, number> }`
  - `ReportValueAssertion { reportLabel, route, query, variables, responseKey, expectedTokens: string[] }`
- **既有数值断言 spec（12 个，仅 finance/sales/purchase/inventory/assets/projects）**：`tests/e2e/dashboards/{finance,sales,purchase,inventory,assets,projects}.value.spec.ts` + `tests/e2e/reports/fin-{income-statement,balance-sheet,ar-ap-aging}.value.spec.ts` + `tests/e2e/reports/{ast-depreciation,inv-inventory-trace,prj-cost-summary}.value.spec.ts`。期望值表落盘 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md` + `docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`。日期漂移防护范式：销售/采购/库存/维护/质量看板 spec 显式传 startDate/endDate 覆盖种子区间。
- **三域看板后端 @BizQuery 已就绪（数值层无阻塞，缺数据）**：
  - `ErpMfgDashboardBizModel.getDashboardKpi(@Optional startDate,@Optional endDate,IServiceContext)`（`module-manufacturing/erp-mfg-service/.../dashboard/ErpMfgDashboardBizModel.java:52`）
  - `ErpMntDashboardBizModel.getDashboardKpi(@Optional startDate,@Optional endDate,IServiceContext)`（`.../ErpMntDashboardBizModel.java:58`）
  - `ErpQaDashboardBizModel.getDashboardKpi(@Optional startDate,@Optional endDate,IServiceContext)`（`.../ErpQaDashboardBizModel.java:65`）
- **三域报表后端 renderHtml 已就绪**：`ErpMfgReport__renderHtml`（crp-load/production-variance/forecast-variance；**crp-load 因 N=1 Deferred crp_load seed 仍空，本计划仅断言 production-variance/forecast-variance**）/ `ErpMntReport__renderHtml`（maintenance-history/downtime-summary）/ `ErpQaReport__renderHtml`（inspection-summary/ncr-capa-summary），各域冒烟 spec 已绿。
- **三域种子为 N=1/N=2 硬前置（本计划依赖）**：当前 57 CSV 下三域看板/报表数值为 0/空（2210-2 Deferred 明示「扩展域交易种子未 seed，数值仍空，断言无意义」）。N=1 制造域 + N=2 维护/质量域固化种子后，三域看板 KPI 转非空，断言方有意义。**本计划不可在 N=1/N=2 落地前执行**。
- **保护区域**：纯测试 spec + helper 复用 + 文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 2210-2）。属 `plan-first`（跨域断言 + 期望值派生 + >5 文件）。

剩余差距：(1) 三域看板缺数值断言 spec；(2) 三域关键报表（mfg crp-load/production-variance/forecast-variance、mnt maintenance-history、qa inspection-summary）缺数值断言 spec；(3) 期望值表（派生公式 + N=1/N=2 seed 行依据）未派生。

## Goals

- 在 N=1/N=2 固化的三域种子基线上，为 manufacturing/maintenance/quality 三域看板叠加**数据驱动 KPI 数值断言**（`*.value.spec.ts`，经 `assertDashboardKpiValues` 取 `getDashboardKpi` 原始值断言）。
- 为三域关键报表叠加**渲染数值断言**（`*.value.spec.ts`，经 `assertReportRenderedWithValue` 断言 HTML 含期望数值 token），至少含 mfg production-variance/forecast-variance 两报表（crp-load 因 N=1 Deferred crp_load seed 归本计划 Deferred）+ mnt maintenance-history + qa inspection-summary。
- 落盘期望值表分析文档（每 KPI/报表 token 标注期望值 + 派生公式 + N=1/N=2 seed 行依据 + variables 选择），确立 seed 漂移同步机制。
- 解除 2210-2 Deferred「其他扩展域看板/报表数值断言（manufacturing/quality/maintenance 子集）」。

## Non-Goals

- **不**断言其他扩展域（CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）——对应域交易种子未 seed（N=1/N=2 Non-Goal），数值仍空，断言无意义。按域逐批 successor（触发条件：对应域交易种子 seed 后）。
- **不**做像素级视觉回归 / 报表下载产物 diff / 跨浏览器矩阵——0637-1 既定 Deferred（触发条件未变：跨环境渲染稳定性 + 产品像素验收 / 非 Chromium 支持）。
- **不**做 CRUD 写操作 / 全实体覆盖——1234-2 Deferred（独立结果表面）。
- **不**改后端 `@BizQuery`/报表模板/ORM——纯测试层断言（镜像 2210-2）。若 N=1/N=2 种子致某 KPI 字段名/口径与断言不符，回 N=1/N=2 调整种子或回本计划调期望值，**不改后端**。
- **不**断言 GL 凭证串联数值（三域 posted=false，N=1/N=2 Non-Goal）——仅断言域表聚合 KPI。

## Task Route

- Type: `verification or audit work`（纯 E2E 数值断言 spec + 期望值派生，零生产代码变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构 + 数值断言层 + seed 漂移同步机制）、`docs/design/dashboards.md` §7/§8/§9（制造/维护/质量看板 KPI 口径，派生期望值依据）、`docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`（期望值表范式）、`docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md` + `docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`（N=1/N=2 seed 行依据，本计划执行时已存在）
- Skill Selection Basis: `nop-frontend-dev`（Playwright spec + 既有 helper 复用，断言层非后端逻辑）。注：`nop-testing` 技能范围为 Java 侧 `JunitAutoTestCase`/`IGraphQLEngine`/快照，**不**覆盖 Playwright 浏览器 E2E；本计划纯浏览器断言层，故选 `nop-frontend-dev`（镜像 2210-2 口径）。

## Infrastructure And Config Prereqs

- N=1 制造域 + N=2 维护/质量域种子已落地（fresh-DB seed 加载 0 冲突，三域看板 KPI 非空）——本计划硬前置。
- 既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB seed + `auth.ts`/`fixtures.ts`/`dashboards/_helper.ts`/`reports/_helper.ts`）就绪，无需改。
- 回滚策略：纯新增 spec + 分析文档，移除即回滚。

## Execution Plan

### Phase 1 - 期望值派生 + 断言范式确认（Proof + Decision）

Status: completed
Targets: N=1/N=2 seed 行、`docs/design/dashboards.md` §7/§8/§9、`docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`
Skill: `nop-frontend-dev`

- Item Types: `Proof | Decision`
- Prereqs: N=1 制造域 + N=2 维护/质量域种子落地（fresh-DB 三域 KPI 非空）

- [x] `Proof`：逐 KPI 派生期望值——读 N=1/N=2 seed 行（work_order/equipment/visit/inspection/non_conformance 等），按 `dashboards.md` 口径手算每域 getDashboardKpi 各字段期望值（如制造在制数=count(work_order IN [IN_PROCESS,STOCK_RESERVED])、维护设备总数=count(equipment 非 DECOMMISSIONED)、质量检验合格率=count(inspection ACCEPTED)/total）。逐报表 token 派生（如 mfg production-variance 报表含某差异金额数值；qa inspection-summary 含检验数 token）。产出期望值表分析文档（每项标注期望值 + 派生公式 + N=1/N=2 seed 行依据）。
      - Skill: `nop-frontend-dev`
- [x] `Decision`：日期漂移防护裁决——制造/维护/质量看板本期 KPI 依赖业务日期区间，spec 显式传 startDate/endDate 覆盖 N=1/N=2 种子区间；制造看板 getDashboardKpi(startDate,endDate,context) 传区间；维护/质量同理。SPC 预警（getSpcOutOfControlWarning）config-gated 默认开，若 N=2 未 seed SPC 则该预警返回 outOfControlChartCount=0——本计划对该方法断言确定性 0（非跳过），以覆盖该 `@BizQuery` 的可观测性。本项为约束记录（镜像 2210-2 范式），记录每 spec 的 variables 选择依据。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 期望值表分析文档落盘（`docs/analysis/2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md`），每 KPI/token 标注期望值 + 派生公式 + seed 行依据 + variables 选择。

### Phase 2 - 三域看板 KPI 数值断言 spec（Add + Proof）

Status: completed
Targets: `tests/e2e/dashboards/{manufacturing,maintenance,quality}.value.spec.ts`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 期望值表

- [x] `Add`：新增 `manufacturing.value.spec.ts` / `maintenance.value.spec.ts` / `quality.value.spec.ts`，各调 `assertDashboardKpiValues` 传入该域 `getDashboardKpi` query + variables（Phase 1 裁决）+ expected（Phase 1 派生）。responseKey 对齐真实方法名（`ErpMfgDashboard__getDashboardKpi` / `ErpMntDashboard__getDashboardKpi` / `ErpQaDashboard__getDashboardKpi`）。
      - Skill: `nop-frontend-dev`
- [x] `Proof`：`npx playwright test tests/e2e/dashboards/{manufacturing,maintenance,quality}.value.spec.ts --workers=1` 全绿（KPI 原始值 = 期望值）。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 三域看板数值断言 spec 全绿（KPI 原始值精确匹配期望值，非「DOM 存在」冒烟级）。

### Phase 3 - 三域报表渲染数值断言 spec（Add + Proof）

Status: completed
Targets: `tests/e2e/reports/{mfg-production-variance,mfg-forecast-variance,mnt-maintenance-history,qa-inspection-summary}.value.spec.ts`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 期望值表

- [x] `Add`：新增至少 4 三域报表数值断言 spec（mfg production-variance / mfg forecast-variance / mnt maintenance-history / qa inspection-summary），各调 `assertReportRenderedWithValue` 传入 `Erp{Mfg,Mnt,Qa}Report__renderHtml` query + variables + expectedTokens（Phase 1 派生的 seed 数值 token）。（mfg crp-load 归 Deferred——N=1 未 seed crp_load。）
      - Skill: `nop-frontend-dev`
- [x] `Proof`：`npx playwright test tests/e2e/reports/{mfg-production-variance,mfg-forecast-variance,mnt-maintenance-history,qa-inspection-summary}.value.spec.ts --workers=1` 全绿（渲染 HTML 含期望数值 token）。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 三域报表数值断言 spec 全绿（HTML 含 seed 派生数值 token，剥离千分位后匹配）。

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 + Phase 3 全绿

- [x] `Add`：`docs/testing/e2e-runbook.md` 数值断言层段补三域（manufacturing/maintenance/quality 看板 + 报表）+ 套件总数更新 + 文件结构补新 spec；`docs/testing/known-good-baselines.md` 增三域数值断言基线行；2210-2 Deferred「其他扩展域看板/报表数值断言（manufacturing/quality/maintenance 子集）」登记解除（本计划 Closure 段登记）。
      - Skill: none

Exit Criteria:

- [x] e2e-runbook 数值断言层含三域 + 套件总数/文件结构同步；known-good-baselines 含三域数值断言基线行；2210-2 Deferred mfg/mnt/qa 子集登记解除（本计划 Closure 段登记）。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0bd79aa1bffemLqYtR5osvXkK6`，独立 general 子代理，新会话冷重播无执行者上下文) — 2210-2 模式镜像保真度高（helper 签名/specs/BizModel/模板/前置门控/skill 理由均 live 验真），rules 1-14 合规扎实。无 BLOCKER。1 MAJOR：dashboards.md 章节号引用错误（§4/§7/§8，§4=财务与制造无关；正确为 §7 制造/§8 维护/§9 质量）→ **已修复**（Task Route + Phase 1 Targets 两处）。2 MINOR：forward-ref 分析文档未存在但正确门控（N=1/N=2 draft，本计划执行时已存在）；Phase 1 SPC 决策「断言为 0 或跳过」模糊 → **已采纳**（commit 为确定性断言 0，覆盖该 @BizQuery 可观测性）。附加修订：N=1 草案审查后 Deferred crp_load + crp-load 报表，本计划同步将 mfg crp-load 断言移出范围至 Deferred（Phase 3 改 4 报表 + 报表数 5→4），与 N=1 收窄后范围一致。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划纯测试 spec + 文档新增，零生产代码变更；验证门控以全套件 fresh-DB seed E2E 全绿为主。

- [x] 范围内行为完成（三域看板 + 报表数值断言 spec + 期望值表）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines）
- [x] 已运行验证：`npx playwright test`（全套件 fresh-DB seed：既有 + 新增三域数值断言全绿 0 回归）+ `mvn clean install -DskipTests`（154 模块，确认无后端污染——纯 tests/ 新增）
- [x] 无范围内项目降级为 deferred/follow-up（其他扩展域断言/像素回归/CRUD 写操作/GL 串联数值/SPC 可选均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 其他扩展域看板/报表数值断言（CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 对应扩展域交易种子未 seed（N=1/N=2 Non-Goal），数值仍空，断言无意义。本计划确立的范式（helper + 期望值表）可供扩展域 successor 复用。
- Successor Required: `yes`
- Trigger Condition: 当对应扩展域交易种子 seed（N=1/N=2 Deferred 后续批次）后，按域逐批补数值断言。

### 像素级视觉回归 + 报表下载产物 diff + 跨浏览器矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 0637-1 既定 Deferred，触发条件未变。本计划数值断言属不同层（数据正确性 vs. 视觉/格式）。
- Successor Required: `yes`
- Trigger Condition: 同 0637-1 Deferred。

### 三域 GL 凭证串联数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三域 posted=false（N=1/N=2 Non-Goal，无 GL 凭证 seed）。本计划仅断言域表聚合 KPI；GL 串联数值断言需三域 GL seed 先行。
- Successor Required: `yes`
- Trigger Condition: 当三域 GL 凭证 seed（N=1/N=2 Deferred）落地后，补 GL 串联数值断言。

### 三域其余报表数值断言（mfg crp-load / mnt downtime-summary / qa ncr-capa-summary）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划至少覆盖 4 报表（mfg production-variance/forecast-variance + mnt maintenance-history + qa inspection-summary）。mfg crp-load 因 N=1 Deferred crp_load seed（workcenter 配置链依赖）仍空，归此；mnt downtime-summary / qa ncr-capa-summary 为同域增强，范式相同，可本计划内或 successor 补。
- Successor Required: `yes`
- Trigger Condition: mfg crp-load：当 N=1 crp_load Deferred 解除（workcenter 配置链 seed 落地）后；mnt downtime-summary / qa ncr-capa-summary：当需全覆盖同域报表数值断言时（本计划 Phase 1 Decision 可纳入或留 successor）。

## Closure

Status Note: 计划已完成并通过独立结束审计（2026-07-09）。三域（manufacturing/maintenance/quality）看板 KPI + 4 报表数据驱动数值断言层落地（7 新 `*.value.spec.ts`），期望值表派生自 0930-1/0930-2 固化种子并经独立审计逐项核对 BizModel 聚合口径与 seed CSV 行算术一致。纯测试 + 文档新增，零生产代码变更（git status 11 文件，无 `*.orm.xml`/`*.java`/CSV 等）。验证全绿：`mvn clean install -DskipTests`（154 模块，1:32）BUILD SUCCESS；`npx playwright test` 全套件 fresh-DB seed 0 回归（74 测试，10.5m，含 4 看板 KPI 断言 + 4 报表渲染断言）。质量看板 `getSpcOutOfControlWarning` 确定性 0 三段断言覆盖该 `@BizQuery` 可观测性（SPC 三表 0930-2 Deferred 未 seed）。

Deferred Release:
- `docs/plans/2026-07-08-2210-2-operational-domain-value-assertions.md` Deferred「其他扩展域看板/报表数值断言（manufacturing/quality/maintenance 子集）」— **RELEASED**。触发条件已满足：0930-1 制造域 + 0930-2 维护/质量域交易种子固化（N=1/N=2）。本计划交付 3 看板 KPI 断言（mfg/mnt/qa `getDashboardKpi` + qa `getSpcOutOfControlWarning`）+ 4 报表渲染数值断言（mfg production-variance/forecast-variance + mnt maintenance-history + qa inspection-summary）。其余扩展域（CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）数值断言仍按域逐批 successor（触发条件：对应域交易种子 seed 后）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bcf33929ffeKmJ1ZvwMPbY0B4`（general，新会话冷重播无执行者上下文，2026-07-09，逐项核实 LIVE 仓库非采信执行者自述）。VERDICT: **PASS**（0 BLOCKER / 0 MAJOR 修复后 / 1 MINOR 非阻塞）。
  - Task A（7 spec 存在性 + 期望值算术，CRITICAL）：7/7 spec 存在且含 required keys；`quality.value.spec.ts` 含两 describe（getDashboardKpi + getSpcOutOfControlWarning）；逐 KPI/token 对照 seed CSV + `Erp{Mfg,Mnt,Qa}DashboardBizModel` + `Erp{Mfg,Mnt,Qa}ReportBizModel` 聚合逻辑算术核验一致（mfg inProcessCount=1/periodCompletedQty=180=100+80/onTimeRate=0.5=1÷2；mnt equipmentTotal=3/runningCount=2/periodVisitCount=1；qa inspectionCount=3/passRate=2÷3/rejectedCount=1/openNcrCount=2/SPC 全 0；4 报表 token WO-2026-001/6000.00/6300.00/300.00、200.00/180.00/-20.00、VIS-2026-001/120.00/90.00、产品甲/100.00%/50.00% 均 seed 派生确定）。
  - Task B（保护区域）：`git status --porcelain` 11 文件（7 spec + 分析文档 + plan + e2e-runbook + known-good-baselines），**零** orm.xml/xbiz.xml/page.yaml/view.xml/.java/.beans.xml/sql-lib.xml/.xpt.xml/CSV 变更（镜像 2210-2）。
  - Task C（分析文档）：`docs/analysis/2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md` 每 KPI/token 标注期望值 + 派生公式 + seed 行依据 + variables 选择，与 spec + seed 三方一致。
  - Task D（文档同步）：e2e-runbook 概览「19 数值断言 spec（共 72 spec）」/ 全套件「74 测试 10.5m」计数自洽；数值断言层段含三域；期望值表含 mfg/mnt/qa/SPC 行；文件结构列 7 新 spec；known-good-baselines 含 2026-07-09 0930-3 基线行（passing 命令 + 0 回归）。
  - Task E（计划一致性）：4/4 Phase `Status: completed`、所有项目 `[x]`、`Plan Status: completed`；Deferred 段（其他扩展域/像素回归/GL 串联/crp-load+downtime-summary+ncr-capa-summary）均附触发条件。
  - Task F（2210-2 release 登记）：本 Closure 段「Deferred Release」已登记（修复审计 MAJOR）。
  - MINOR（非阻塞，已修复）：e2e-runbook seed-drift-sync step 2 括注原漏列 0930-3 文档 → 执行者已补列，与期望值表清单对齐。

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
