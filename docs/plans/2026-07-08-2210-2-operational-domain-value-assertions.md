# 2026-07-08-2210-2-operational-domain-value-assertions 运营域（库存/资产/项目）看板/报表数据驱动数值断言

> Plan Status: completed
> Mission: erp
> Work Item: 运营域（inventory/assets/projects）看板 KPI + 报表渲染数据驱动精确数值浏览器 E2E 断言
> Last Reviewed: 2026-07-08
> Source: deferred 项承接 `docs/plans/2026-07-08-1445-2-data-driven-e2e-value-assertions.md` Deferred「扩展域看板/报表数值断言（assets/projects/manufacturing/.../inventory）」（Successor Required: yes，触发条件「当对应扩展域交易种子 seed（1445-1 Deferred 后续批次）后，按域逐批补数值断言」——**已满足**：N=1 `2026-07-08-2210-1-operational-domain-transaction-seeds.md` 固化运营域交易种子）；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-1445-2-data-driven-e2e-value-assertions.md`（completed，数值断言范式 + `assertDashboardKpiValues`/`assertReportRenderedWithValue` helper，本计划复用）、`docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md`（同批 N=1，本计划的数据层前置）、`docs/plans/2026-07-07-1100-3-dashboard-deferred-indicators.md`（completed，项目毛利率 `getProjectGrossMargin` 后端已就绪，本计划接数值断言）
> Audit: required

## Current Baseline

实时仓库逐项核实（`ls`/`rg`/`read`，非采信旧记忆）：

- **数值断言范式已建立（1445-2 交付）**：`tests/e2e/dashboards/_helper.ts:40` `assertDashboardKpiValues(cfg: DashboardKpiAssertion)`——直接 `page.request.post('/graphql')` 取 `getDashboardKpi` 原始 Map，逐字段 `expect(actual === expected)` 断言（非 AMIS DOM 文本解析，规避千分位/币种符号抖动）；`tests/e2e/reports/_helper.ts:58` `assertReportRenderedWithValue(cfg: ReportValueAssertion)`——`renderHtml` 取 HTML，剥离千分位后断言含期望数值 token。两 helper 接口就绪：
  - `DashboardKpiAssertion { domain, route, query, variables, responseKey, expected: Record<string, number> }`
  - `ReportValueAssertion { reportLabel, route, query, variables, responseKey, expectedTokens: string[] }`
- **既有数值断言 spec（仅 finance/sales/purchase 三域，59 spec 中 6 个）**：`tests/e2e/dashboards/{finance,sales,purchase}.value.spec.ts` + `tests/e2e/reports/fin-{income-statement,balance-sheet,ar-ap-aging}.value.spec.ts`。期望值表派生范式落盘 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`（每 KPI 标注期望值 + 派生公式 + seed 行依据）。日期漂移防护范式：销售/采购看板 spec 显式传 startDate/endDate 覆盖种子区间；财务传 periodId。
- **运营域看板后端 @BizQuery 已就绪（数值层无阻塞，缺数据）**：
  - `ErpInvDashboardBizModel.getDashboardKpi(@Optional startDate, @Optional endDate, ...)`（`module-inventory/erp-inv-service/.../dashboard/ErpInvDashboardBizModel.java:62`）
  - `ErpAstDashboardBizModel.getDashboardKpi(@Optional periodId: String, ...)`（`.../ErpAstDashboardBizModel.java:51`）
  - `ErpPrjDashboardBizModel.getDashboardKpi(IServiceContext)`（`.../ErpPrjDashboardBizModel.java:58`）+ `getProjectGrossMargin(@Optional projectId)`（:164，1100-3 已落地）
- **运营域报表后端 renderHtml 已就绪**：`ErpInvReport__renderHtml` / `ErpAstReport__renderHtml` / `ErpPrjReport__renderHtml`（各域 `ErpXxxReportBizModel` 已落地，24 报表 spec 中含 ast-depreciation/ast-disposal/inv-inventory-trace/prj-cost-summary/prj-timesheet 冒烟 spec 已绿）。
- **运营域种子为 N=1 前置（本计划依赖）**：当前 P2P+O2C 种子库（44 CSV）下运营域看板/报表数值为 0/空（1445-2 Deferred 明示「扩展域交易种子未 seed，数值仍空，断言无意义」）。N=1 `2026-07-08-2210-1` 固化运营域种子后，三域看板 KPI 转非空，断言方有意义。**本计划不可在 N=1 落地前执行**。
- **保护区域**：纯测试 spec + helper 复用 + 文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 1445-2）。属 `plan-first`（跨域断言 + 期望值派生需严谨 + >5 文件）。

剩余差距：(1) 运营域三域看板缺数值断言 spec（仅 finance/sales/purchase 有）；(2) 运营域报表（ast-depreciation/ast-disposal/inv-inventory-trace/prj-cost-summary/prj-timesheet）缺数值断言 spec；(3) 期望值表（派生公式 + N=1 seed 行依据）未派生。

## Goals

- 在 N=1 固化的运营域种子基线上，为 inventory/assets/projects 三域看板叠加**数据驱动 KPI 数值断言**（`*.value.spec.ts`，经 `assertDashboardKpiValues` 取 `getDashboardKpi` 原始值断言）。
- 为运营域关键报表叠加**渲染数值断言**（`*.value.spec.ts`，经 `assertReportRenderedWithValue` 断言 HTML 含期望数值 token），至少含 ast-depreciation / inv-inventory-trace / prj-cost-summary。
- 落盘期望值表分析文档（每 KPI/报表 token 标注期望值 + 派生公式 + N=1 seed 行依据），确立 seed 漂移同步机制。
- 解除 1445-2 Deferred「扩展域看板/报表数值断言（assets/projects/inventory 子集）」。

## Non-Goals

- **不**断言其他扩展域（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）——对应域交易种子未 seed（N=1 Non-Goal），数值仍空，断言无意义。按域逐批 successor（触发条件：对应域交易种子 seed 后）。
- **不**做像素级视觉回归 / 报表下载产物 diff / 跨浏览器矩阵——0637-1 既定 Deferred（触发条件未变：跨环境渲染稳定性 + 产品像素验收 / 非 Chromium 支持）。
- **不**做 CRUD 写操作 / 全 343 实体覆盖——1234-2 Deferred（独立结果表面）。
- **不**改后端 `@BizQuery`/报表模板/ORM——纯测试层断言（镜像 1445-2）。若 N=1 种子致某 KPI 字段名/口径与断言不符，回 N=1 调整种子或回本计划调期望值，**不改后端**。
- **不**断言 GL 凭证串联数值（运营域 posted=false，N=1 Non-Goal）——仅断言域表聚合 KPI。

## Task Route

- Type: `verification or audit work`（纯 E2E 数值断言 spec + 期望值派生，零生产代码变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构 + 数值断言层 + seed 漂移同步机制）、`docs/design/dashboards.md` §3/§5/§6（库存/资产/项目看板 KPI 口径，派生期望值依据）、`docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`（期望值表范式）、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（N=1 Phase 1 产出，本计划执行时已存在——N=1 seed 行依据）
- Skill Selection Basis: `nop-frontend-dev`（Playwright spec + 既有 helper 复用，断言层非后端逻辑）。注：此处与 1445-2 标注的 `nop-testing` 有意偏离——`nop-testing` 技能范围是 Java 侧 `JunitAutoTestCase`/`IGraphQLEngine`/快照录制回放，**不**覆盖 Playwright 浏览器 E2E；本计划纯浏览器断言层，故选 `nop-frontend-dev`（1445-2 实际执行亦为 Playwright，该标注为历史口径偏差）。

## Infrastructure And Config Prereqs

- N=1 运营域种子已落地（fresh-DB seed 加载 0 冲突，三域看板 KPI 非空）——本计划硬前置。
- 既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB seed + `auth.ts`/`fixtures.ts`）就绪，无需改。
- 回滚策略：纯新增 spec + 分析文档，移除即回滚。

## Execution Plan

### Phase 1 - 期望值派生 + 断言范式确认（Proof + Decision）

Status: completed
Targets: N=1 seed 行、`docs/design/dashboards.md` §3/§5/§6、`docs/analysis/2026-07-08-1445-2-kpi-expected-values.md`
Skill: `nop-frontend-dev`

- Item Types: `Proof | Decision`
- Prereqs: N=1 运营域种子落地（fresh-DB 三域 KPI 非空）

- [x] `Proof`：逐 KPI 派生期望值——读 N=1 seed 行（stock_balance/asset/depreciation_schedule/project/cost_collection/project_pnl），按 `dashboards.md` 口径手算每域 getDashboardKpi 各字段期望值（如库存总值 = Σ stock_balance.totalCost；资产原值 = Σ asset.originalValue(IN_SERVICE)；项目毛利率 = Σ project_pnl.grossProfit / Σ revenueAmount）。逐报表 token 派生（如 ast-depreciation 报表含某资产月折旧额数值）。产出期望值表分析文档（每项标注期望值 + 派生公式 + N=1 seed 行依据）。
      - Skill: `nop-frontend-dev`
- [x] `Decision`：日期漂移防护裁决——库存看板「本期出入库量」依赖业务日期区间，spec 显式传 startDate/endDate 覆盖 N=1 种子区间；资产看板传 periodId（若 KPI 依赖期间）；项目看板 getDashboardKpi(context) 无显式参数则断言聚合全量。本项为约束记录（镜像 1445-2 销售/采购范式，非新决策），记录每 spec 的 variables 选择依据。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 期望值表分析文档落盘（`docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`），每 KPI/token 标注期望值 + 派生公式 + seed 行依据 + variables 选择。

### Phase 2 - 运营域看板 KPI 数值断言 spec（Add + Proof）

Status: completed
Targets: `tests/e2e/dashboards/{inventory,assets,projects}.value.spec.ts`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 期望值表

- [x] `Add`：新增 `inventory.value.spec.ts` / `assets.value.spec.ts` / `projects.value.spec.ts`，各调 `assertDashboardKpiValues` 传入该域 `getDashboardKpi` query + variables（Phase 1 裁决）+ expected（Phase 1 派生）。responseKey 对齐真实方法名（`ErpInvDashboard__getDashboardKpi` / `ErpAstDashboard__getDashboardKpi` / `ErpPrjDashboard__getDashboardKpi`）。
      - Skill: `nop-frontend-dev`
- [x] `Proof`：`npx playwright test tests/e2e/dashboards/{inventory,assets,projects}.value.spec.ts --workers=1` 全绿（KPI 原始值 = 期望值）。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 三域看板数值断言 spec 全绿（KPI 原始值精确匹配期望值，非「DOM 存在」冒烟级）。

### Phase 3 - 运营域报表渲染数值断言 spec（Add + Proof）

Status: completed
Targets: `tests/e2e/reports/{ast-depreciation,inv-inventory-trace,prj-cost-summary}.value.spec.ts`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 期望值表

- [x] `Add`：新增至少 3 运营域报表数值断言 spec（ast-depreciation / inv-inventory-trace / prj-cost-summary），各调 `assertReportRenderedWithValue` 传入 `Erp{Ast,Inv,Prj}Report__renderHtml` query + variables + expectedTokens（Phase 1 派生的 seed 数值 token）。
      - Skill: `nop-frontend-dev`
- [x] `Proof`：`npx playwright test tests/e2e/reports/{ast-depreciation,inv-inventory-trace,prj-cost-summary}.value.spec.ts --workers=1` 全绿（渲染 HTML 含期望数值 token）。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 运营域报表数值断言 spec 全绿（HTML 含 seed 派生数值 token，剥离千分位后匹配）。

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 + Phase 3 全绿

- [x] `Add`：`docs/testing/e2e-runbook.md` 数值断言层段补运营域（inventory/assets/projects 看板 + 报表）+ 套件总数更新（59→65）+ 文件结构补新 spec；`docs/testing/known-good-baselines.md` 增运营域数值断言基线行。
      - Skill: none

Exit Criteria:

- [x] e2e-runbook 数值断言层含运营域 + 套件总数/文件结构同步；known-good-baselines 含运营域数值断言基线行；1445-2 Deferred「扩展域看板/报表数值断言（assets/projects/inventory 子集）」登记解除（本计划 Closure 段登记）。

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0bdea2f16ffe9cwCROlADuzuqO`，独立 general 子代理，新会话冷重播无执行者上下文) — 全部基线主张经实时仓库逐项核实，零虚假/陈旧：helper 签名精确匹配（`DashboardKpiAssertion`/`ReportValueAssertion` 字段一致）/仅 6 个 `*.value.spec.ts`（finance/sales/purchase 看板 + 3 finance 报表，无运营域）/三域 `getDashboardKpi` 方法名+签名+行号一致（ErpInv/Ast/Prj Dashboard + Prj getProjectGrossMargin）/三域 `Erp{Inv,Ast,Prj}Report__renderHtml` 存在/N=1 依赖正确（N=1 为 draft 且交叉引用 N=2 successor，运营域 seed CSV 不存在 → KPI 当前空，断言有意义性依赖 N=1 成立）/59 spec 基线一致/dashboards.md §3/§5/§6 存在。N=1 硬前置正确声明（Infrastructure Prereqs + Phase 1 Prereqs + Baseline）。规则 1-14 全项合规（honest live baseline / 清晰 goals+non-goals / typed items / skill 记录 / Deferred 带触发条件 / 充分 Closure Gates / 正确前置处理 / 单结果表面镜像 1445-2 范式）。无 BLOCKER/MAJOR。3 MINOR 已采纳：skill 选用偏离 1445-2 的理由已补注（nop-testing 范围为 Java 侧测试非 Playwright，本计划纯浏览器断言层故选 nop-frontend-dev）；Phase 1 Decision 标注为约束记录（镜像 1445-2 范式，非新决策）；N=1 产出分析文档前向引用已注明「N=1 Phase 1 产出，本计划执行时已存在」。草案已收敛为可接受执行契约（gated on N=1 落地，已正确声明）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划纯测试 spec + 文档新增，零生产代码变更；验证门控以全套件 fresh-DB seed E2E 全绿为主。

- [x] 范围内行为完成（运营域三域看板 + 报表数值断言 spec + 期望值表）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines）
- [x] 已运行验证：`npx playwright test`（全套件 fresh-DB seed：既有 59 + 新增运营域数值断言全绿 0 回归）+ `mvn clean install -DskipTests`（154 模块，确认无后端污染——纯 tests/ 新增）
- [x] 无范围内项目降级为 deferred/follow-up（其他扩展域断言/像素回归/CRUD 写操作/GL 串联数值均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 其他扩展域看板/报表数值断言（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 对应扩展域交易种子未 seed（N=1 Non-Goal），数值仍空，断言无意义。本计划确立的范式（helper + 期望值表）可供扩展域 successor 复用。
- Successor Required: `yes`
- Trigger Condition: 当对应扩展域交易种子 seed（N=1 Deferred 后续批次）后，按域逐批补数值断言。

### 像素级视觉回归 + 报表下载产物 diff + 跨浏览器矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 0637-1 既定 Deferred，触发条件未变（跨环境渲染稳定性 / 产品像素验收 / 报表口径回归缺陷 / 非 Chromium 支持需求）。本计划数值断言属不同层（数据正确性 vs. 视觉/格式）。
- Successor Required: `yes`
- Trigger Condition: 同 0637-1 Deferred。

### 运营域 GL 凭证串联数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 运营域 posted=false（N=1 Non-Goal，无 GL 凭证 seed）。本计划仅断言域表聚合 KPI；GL 串联（凭证↔源单据↔辅助账）数值断言需运营域 GL seed 先行。
- Successor Required: `yes`
- Trigger Condition: 当运营域 GL 凭证 seed（N=1 Deferred）落地后，补 GL 串联数值断言。

## Closure

Status Note: 执行完成（2026-07-08，主代理执行 4 阶段全绿）。在 2210-1 固化运营域种子基线上叠加**数据驱动数值断言层**：新增 6 个 `*.value.spec.ts`（dashboards/{inventory,assets,projects} + reports/{ast-depreciation,inv-inventory-trace,prj-cost-summary}），spec 总数 59→65。看板 KPI 经 GraphQL `getDashboardKpi`/`getProjectGrossMargin` 取原始 Map 逐字段断言（inv totalValue=10450/incomingQty=100/outgoingQty=0/turnoverRate=0、ast originalValue=135000/accumulatedDepreciation=6000/netBookValue=129000/periodDepreciation=2000、prj openProjectCount=1/totalBudget=50000/incurredCost=30000/executionRate=0.6 + 毛利率 totalRevenue=50000/totalGrossProfit=20000/grossMarginPct=0.4）；报表 `renderHtml` HTML 含确定性 token（资产折旧明细 120000.00/6000.00/2000.00/114000.00、库存追溯链 MV-2026-001/100.00（moveId=1 正向链根节点）、项目成本汇总 50000.00/30000.00/60.00%）。日期漂移防护：库存传 startDate/endDate、资产传 periodId="2026-07"、项目无参聚合全量。期望值表落盘 `docs/analysis/2026-07-08-2210-2-operational-kpi-expected-values.md`（每项标注期望值 + 派生公式 + N=1 seed 行依据 + variables 选择）。验证全绿：`npx playwright test`（66 test case / 65 spec 文件，9.6m）0 回归；`mvn clean install -DskipTests`（154 模块，1:27）BUILD SUCCESS 确认纯 tests/ 新增无后端污染。文档对齐：e2e-runbook 数值断言层 + 期望值表 + 文件结构 + 套件总数（59→65）补运营域、known-good-baselines 增运营域数值断言基线行。Deferred 解除登记：1445-2 Deferred「扩展域看板/报表数值断言（assets/projects/inventory 子集）」——本计划已交付 inventory/assets/projects 子集断言，解除该子集；其余扩展域（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps/master-data）数值断言仍按域逐批（本计划 Non-Goal，触发条件：对应域交易种子 seed 后）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bd9d8446ffeCBNFk2wlMSDL2k`（general，新会话冷重播无执行者上下文）。VERDICT: **PASS**（无 BLOCKER）。逐项核实：6 新 spec + 期望值表 + 2 测试文档 + plan 文件为唯一变更（`git diff --stat` 确认零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更，保护区域洁净）/ 4 Phase 全 `Status: completed` + 全项 `[x]` / 算术抽查 3 处全对（inv totalValue=850+9600、ast periodDepreciation=schedule(period=2026-07,EXECUTED) actualAmount=2000、prj executionRate=30000/50000=0.6）/ 报表 token + variables 正确（ast num 格式 #,##0.00、prj 执行率 0.00%→60.00%、inv 传 data:{moveId:1} 因 findCandidateMoves(null,null) 空短路而 forwardTrace 必返回根节点）/ 无陈旧文档声明。1 MINOR「12 域」措辞已修订为「12 个数据驱动数值断言 spec」。无技术交付物返工。

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
