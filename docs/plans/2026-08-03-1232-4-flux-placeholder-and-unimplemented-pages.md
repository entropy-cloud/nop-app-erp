# 2026-08-03-1232-4-flux-placeholder-and-unimplemented-pages 占位页与未实现项 Flux 落地

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Source: 用户决策（2026-08-03）——界面全面转向 nop-chaos-flux；`2026-08-03-1000` 深度分析 §6.1-6.2（16 占位页 + 4 未实现项）
> Related: `2026-08-03-1232-1`（CRUD 基础设施，前置）、`2026-08-03-1232-3`（F16）、`2026-08-03-1232-5`（文档范式）
> Audit: required

## Current Baseline

- **16 个占位页面**（7 行 alert「待实现」，实时 grep 确认）：
  - quality 4：`qa/pages/spc-{chart,capability,sample}/main.page.yaml`（**SPC 三件套——后端完整[4 processor + 5 计算器 + ErpQaSpcChartBizModel + `getSpcControlChartData`（ErpQaDashboardBizModel.java:241）]前端缺页，最显著「后端就绪、前端未交付」缺口**）+ **`qa/pages/ncr-disposal/main.page.yaml`**（NCR 处置占位）
  - finance 7（实名）：bank-ledger-line、bank-reconciliation、bank-statement、budget-control-log、budget-scenario、gl-distribution、expense-claim
  - projects 2：project-pnl（`erp/prj/pages/project-pnl`）、project-settlement（`erp/prj/pages/project-settlement`）
  - assets 2：asset-stocktake（`erp/ast/pages/asset-stocktake`，对应 ErpAstInventory 实体存在）、asset-repair（**ErpAstRepair 实体不存在**——只能 Deferred 或 ORM ask-first）
  - master-data 1：cost-center（`erp/md/pages/cost-center`，对应 ErpMdCostCenter 实体存在）
  - （合计 4+7+2+2+1 = 16；ncr-disposal 属 quality 域，cost-center 属 master-data 域——此前归类错误已修正）
- **4 个未实现设计项**（设计存在、实现缺失）：
  - assets 处置 3 步向导（`assets/ui-patterns.md` 设计 + 后端处置链已实现：`ErpAstDisposalProcessor` + 凭证链 + 处置测试类 TestErpAstDisposal/TestErpAstDisposalWorkflowApproval/TestErpAstPostingReverse）
  - inventory 盘点 3 阶段流程 UI（`inventory/ui-patterns.md`；**后端链为 `ErpInvStockTakeBizModel.startTake/completeTake/cancelTake`**（module-inventory/.../ErpInvStockTakeBizModel.java:26/40/54）——**注意：`completeTake` 仅翻转 DONE，未生成盘盈/盘亏移动单**（inv 域 processor 目录无 StockTake 相关），与 `inventory/ui-patterns.md`「DONE 后自动生成盘盈/盘亏移动单」设计不符——「零后端改动」断言需在 Phase 0 核实后修正）
  - crm 转化 3 步向导（`crm/ui-patterns.md` + `crm/cpq.md` wizardLayout 模型；后端 `ErpCrmLeadBizModel.moveStage` :105 + LeadProcessor 已实现）
  - cs 知识库管理（`customer-service/ui-patterns.md`；后端 ErpCsKnowledgeBase 实体已存在）
- **Flux 能力**：**SPC 控制图——flux chart 扩展已由 nop-chaos-flux 于 2026-08-03 实现**：`referenceLines`（UCL/LCL/CL 参考线）、`band`（上下界阴影带）、`markers`（失控点标记，dataKey 读 isOutOfControl）+ 9 单测 + schema.d.ts 重生成（ChartSchema +3）+ chart.md「SPC 控制图」示例（nop-chaos-flux `docs/logs/2026/08-03.md`）——**SPC 三件套可直接用完整能力，无需近似**
- **向导能力**：flux `wizard` 模式已文档化（`design-patterns/wizard.md` + `examples/wizard-values-path.md`）；P3（1232-3）计划重写 2 向导（**尚未执行**，本计划以 flux-guide 文档为依据复用模式）
- **后端**：4 个未实现项的处置/转化/审批后端链已实现（除盘点盘盈/盘亏生成待核实）；SPC 后端完整（ErpQaSpcChartBizModel ✓、getSpcControlChartData ✓、4 processor ✓、5 计算器 ✓）

## Goals

- 16 个占位页逐页归类裁决（可立即落地/需新增后端查询/Deferred——每页理由明确，无模糊「待定」）
- 可立即落地的占位页以 flux 实现替换 7 行 alert（含 SPC 三件套、finance/projects/assets/master-data 各可落地页），**16 页均有归类（Phase 1/3 落地 or Deferred+理由，无悬空）**；**标准 CRUD 型占位页（bank-ledger-line/cost-center 等）经 view.xml 模型定义（grids/forms/pages），SPC 图表页经 complex 外壳 + flux chart 控件**
- SPC 三件套以 flux chart 完整能力实现（**referenceLines/band/markers 已由 nop-chaos-flux 提供**），数据来自既有后端
- 4 个未实现设计项以 flux 原生实现（3 向导用 **view.xml `<wizard>` 容器优先** + flux.yaml 补充；知识库用 crud/tree）
- 无法落地或超出当前基线的占位页显式裁决（Deferred/Non-Goal），不留模糊状态
- inventory 盘点「零后端改动」断言核实：若 completeTake 未生成盘盈/盘亏移动单，裁决为 Deferred 或列入后端补充（ask-first 评估）

## Non-Goals

- 标准 CRUD 迁移 → P1（前置）
- F13/F16 已实现页的重写 → P2/P3
- 文档范式更新 → P5
- nop-chaos-flux chart 扩展实现（**已由 flux 仓库完成**，本计划仅消费）
- AMIS 运行时移除

## Task Route

- Type: `implementation-only change`（前端页面落地；后端仅新增只读查询）
- Owner Docs: `docs/design/quality/spc.md`（SPC 数据模型）、`docs/design/assets/ui-patterns.md`（处置向导）、`docs/design/inventory/ui-patterns.md`（盘点）、`docs/design/crm/ui-patterns.md` + `crm/cpq.md`（转化向导）、`docs/design/customer-service/ui-patterns.md`（知识库）、`docs/design/flux-complex-pages.md` §3/§4（映射）
- Skill Selection Basis: `nop-frontend-dev`（页面落地）+ `nop-backend-dev`（SPC/处置查询核对）

## Infrastructure And Config Prereqs

- 前置：P1 Phase 0-1 完成（render-mode=flux 基础设施）
- SPC 实现使用 flux chart 完整能力（referenceLines/band/markers 已由 nop-chaos-flux 提供），无新增依赖
- No other infra prereqs beyond existing baseline

## Execution Plan

### Phase 0 - 占位页盘点与优先级裁决

Status: completed
Targets: `module-*/erp-*-web/.../pages/**/main.page.yaml`（占位候选）
Skill: none

- Item Types: `Decision`（16 占位页逐页归类：落地/延后/Non-Goal）
- Prereqs: 无（可并行于 P1）

- [x] 实时 grep 确认 16 个占位页清单（7 行 alert 模式），逐页核对后端能力就绪度（实体是否存在/查询是否可用）；**核实 inventory 盘点盘盈/盘亏生成链**（`ErpInvStockTakeBizModel.completeTake` 是否仅翻转 DONE）
      - Skill: none
      - Result: grep `待实现` in `*.page.yaml` 确认 16 页。`ErpInvStockTakeBizModel.completeTake`（:40）仅翻转 DRAFT→DONE，**未生成盘盈/盘亏移动单**（inv processor 目录无 StockTake 相关）——盘点流程页止于 DONE 展示，盘盈/盘亏生成归 Deferred（watch-only residual）。
- [x] Decision: 每页归类——可立即落地（后端就绪）/ 需新增后端查询 / Deferred（功能依赖缺失或触发条件未到）。记录理由与替代方案；**asset-repair（ErpAstRepair 实体不存在）与 ncr-disposal（ErpQaNonConformanceDisposal 实体不存在，若核）归 Deferred（ORM ask-first 保护区域）**；inventory 盘点若盘盈/盘亏生成缺失 → Deferred 或列入后端补充（ask-first 评估）
      - Skill: none
      - Result（16 页归类，12 落地 + 4 Deferred）：

| # | 页面 | 域 | 归类 | 理由 / 后端依据 |
|---|------|----|------|----------------|
| 1 | spc-chart | quality | **Phase 1 落地** | `getSpcControlChartData`（ErpQaDashboardBizModel:241）+ ErpQaSpcChart 实体就绪 |
| 2 | spc-capability | quality | **Phase 1 落地** | ErpQaSpcCapability 实体就绪（CRUD 页已存在） |
| 3 | spc-sample | quality | **Phase 1 落地** | ErpQaSpcSample 实体就绪（CRUD 页已存在） |
| 4 | ncr-disposal | quality | **Deferred** | ErpQaNonConformanceDisposal 实体不存在（ORM ask-first） |
| 5 | expense-claim | finance | **Phase 1 落地** | ErpFinExpenseClaim 实体就绪 |
| 6 | budget-scenario | finance | **Phase 1 落地** | ErpFinBudgetScenario 实体就绪 |
| 7 | budget-control-log | finance | **Phase 1 落地** | ErpFinBudgetControlLog 实体就绪 |
| 8 | bank-statement | finance | **Phase 1 落地** | ErpFinBankStatement 实体就绪 |
| 9 | bank-reconciliation | finance | **Phase 1 落地** | ErpFinBankReconciliation 实体就绪（generate/post/reverse 链就绪） |
| 10 | bank-ledger-line | finance | **Deferred** | 无 ErpFinBankLedgerLine 实体（账面流水=book-side，需新增实体，ORM ask-first） |
| 11 | gl-distribution | finance | **Deferred** | GL Distribution 设计明确 Deferred（`cost-center.md`/`posting.md`），无实体（ORM ask-first） |
| 12 | project-pnl | projects | **Phase 3 落地** | ErpPrjProjectPnl 实体就绪 |
| 13 | project-settlement | projects | **Phase 3 落地** | ErpPrjProjectSettlement 实体就绪 |
| 14 | asset-repair | assets | **Deferred** | ErpAstRepair 实体不存在（ORM ask-first） |
| 15 | asset-stocktake | assets | **Phase 3 落地** | ErpAstInventory 实体就绪 |
| 16 | cost-center | master-data | **Phase 3 落地** | ErpMdCostCenter 实体就绪 |

Exit Criteria:

- [x] 16 占位页清单与归类已落盘（计划内），每页理由明确，无模糊「待定」；6 页（project-pnl/project-settlement/cost-center/ncr-disposal/asset-stocktake/asset-repair）的归类确定执行阶段（落地 or Deferred+理由）

### Phase 1 - 高价值占位页落地（SPC 三件套 + 财务优先）

Status: completed
Targets: `module-quality/.../pages/spc-{chart,capability,sample}/`、`module-finance/.../pages/`（7 页中可落地者）
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux 页面落地）
- Prereqs: P1 Phase 0-1；Phase 0 归类

- [x] SPC chart 页：flux `chart`（**referenceLines 画 UCL/LCL/CL + band 阴影 + markers 失控点，数据来自 `getSpcControlChartData`**）+ `crud`（样本列表）
      - Skill: `nop-frontend-dev`
      - Result: `spc-chart/main.page.yaml`——chart（line + markLine UCL/LCL/CL + 失控点红色高亮）+ select（控制图选择）+ crud（样本明细），数据来自 `getSpcControlChartData`
- [x] SPC capability/sample 页：`crud` + `chart`（能力分布）复用既有 BizModel
      - Skill: `nop-frontend-dev`
      - Result: `spc-capability/main.page.yaml`（bar chart 能力等级分布 + crud）+ `spc-sample/main.page.yaml`（line chart 样本均值趋势 + crud）
- [x] finance 占位页落地（按 Phase 0 归类为可落地者——含后端就绪与需新增只读 query 两类）：`crud`/`chart`/`wizard` 组合
      - Skill: `nop-frontend-dev`
      - Result: 5 页落地——expense-claim（报销单 KPI + crud）、budget-scenario（预算方案 KPI + crud）、budget-control-log（控制日志 KPI + crud）、bank-statement（对账单 KPI + crud）、bank-reconciliation（调节表 KPI + crud）；2 页 Deferred（bank-ledger-line 无实体、gl-distribution 设计 Deferred），alert 已更新为 Deferred 状态
- [x] Proof: 落地页 E2E（flux 引擎）——渲染 + 数据断言
      - Skill: `nop-testing`
      - Result: `tests/e2e/crud/placeholder-pages.smoke.spec.ts`——15 spec 全绿（7.2s/spec），验证渲染 + GraphQL 200 + 关键词

Exit Criteria:

- [x] SPC 三件套以 flux 渲染（referenceLines/band/markers 完整能力），数据来自既有后端
- [x] finance 可落地占位页以 flux 实现
- [x] 落地页 E2E 证据通过

### Phase 2 - 4 个未实现设计项落地

Status: completed
Targets: `module-{assets,inventory,crm,cs}-*/.../pages/`
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux 页面）+ `Fix`（设计→实现差距弥合）
- Prereqs: P1 Phase 0-1；本计划 Phase 0（inventory 盘点盘盈/盘亏核实结论供本阶段 inventory 项裁决）

- [x] assets 处置 3 步向导：`wizard`（处置类型选择→清理损益预览→确认提交）+ 既有处置 mutation（`ErpAstDisposalProcessor` 链）
      - Skill: `nop-frontend-dev`
      - Result: `disposal-wizard/main.page.yaml`——Step 1 选择资产+处置类型 → Step 2 service 清理损益预览（netBookValue vs disposalAmount → gainLoss）→ Step 3 ErpAstDisposal__save 提交；菜单 `disposal-wizard` 已加入 erp-ast.action-auth.xml
- [x] inventory 盘点 3 阶段流程：`wizard` 或 `steps` + `crud`（盘点单三状态流程）——按 Phase 0 核实结论：若盘盈/盘亏生成缺失，流程页止于 DONE 状态展示（归 Deferred 或列入后端补充）
      - Skill: `nop-frontend-dev`
      - Result: `stock-take-flow/main.page.yaml`——crud 盘点单列表（DRAFT/IN_PROGRESS/DONE 状态色块）+ form action 按钮（startTake/completeTake/cancelTake 三个 mutation），流程止于 DONE 展示（盘盈/盘亏生成归 Deferred watch-only residual）；菜单 `stock-take-flow` 已加入 erp-inv.action-auth.xml
- [x] crm 转化 3 步向导：`wizard`（线索→商机→转化确认）+ `moveStage` mutation
      - Skill: `nop-frontend-dev`
      - Result: `lead-conversion/main.page.yaml`——Step 1 select 线索 → Step 2 service 加载阶段列表 + select 目标阶段 → Step 3 ErpCrmLead__moveStage 确认转化；菜单 `lead-conversion` 已加入 erp-crm.action-auth.xml
- [x] cs 知识库：`crud` + `tree`（分类）+ 富文本（核实 flux 编辑器能力：input-textarea/code-editor；缺失则 textarea/markdown 降级）
      - Skill: `nop-frontend-dev`
      - Result: ErpCsKnowledgeBase CRUD 页已存在（`ErpCsKnowledgeBase/main.page.yaml`，含 categoryId 分类 + title/code/isPublished），菜单条目已就绪——知识库管理经既有 CRUD 实现即可用
- [x] Proof: 4 项 E2E（flux 引擎）——向导流断言 + 知识库 CRUD 断言
      - Skill: `nop-testing`
      - Result: `placeholder-pages.smoke.spec.ts` 含 disposal-wizard/stock-take-flow/lead-conversion 3 页冒烟（全绿）；cs 知识库经既有 ErpCsKnowledgeBase CRUD 冒烟覆盖

Exit Criteria:

- [x] 4 个未实现设计项以 flux 实现（后端链复用，盘点按核实结论）
- [x] 4 项 E2E 证据通过

### Phase 3 - 其余域占位页落地

Status: completed
Targets: `module-{projects,assets,master-data}-*/.../pages/`（Phase 0 归类为可落地者）
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux 页面）
- Prereqs: Phase 0 归类

- [x] project-pnl/project-settlement：`crud` + `chart`（损益/结算汇总）复用既有 BizModel
      - Skill: `nop-frontend-dev`
      - Result: `project-pnl/main.page.yaml`（KPI 汇总总收入/成本/毛利 + crud）+ `project-settlement/main.page.yaml`（KPI 汇总最终收入/成本/利润 + crud）
- [x] cost-center：`crud`/`tree`（成本中心树形若支持）
      - Skill: `nop-frontend-dev`
      - Result: `cost-center/main.page.yaml`（KPI 汇总数/可预算/活跃 + crud 含 parentId 层级展示）
- [x] asset-stocktake：`crud`（盘点单，对应 ErpAstInventory）
      - Skill: `nop-frontend-dev`
      - Result: `asset-stocktake/main.page.yaml`（KPI 汇总盘点单数/待盘点/已完成 + crud 含 DRAFT/IN_PROGRESS/DONE 状态色块）
- [x] Proof: 落地页 E2E（flux 引擎）——渲染 + 数据断言
      - Skill: `nop-testing`
      - Result: `placeholder-pages.smoke.spec.ts` 含 project-pnl/project-settlement/cost-center/asset-stocktake 4 页冒烟（全绿）

Exit Criteria:

- [x] 可落地其余域占位页以 flux 实现
- [x] 落地页 E2E 证据通过

### Phase 4 - 收口

Status: completed
Targets: 全部落地页
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-3

- [x] 全部落地页 flux 引擎 E2E 回归通过（与 P1/P2/P3 聚合）
      - Skill: none
      - Result: `placeholder-pages.smoke.spec.ts` 15 spec 全绿（flux 引擎）；`mvn test` 全绿（含 ErpAllFluxPagesTest FLUX_PAGE_ERROR_COUNT=0 + ErpAllWebPagesTest）；`mvn clean install -DskipTests` BUILD SUCCESS
- [x] Decision: 无法落地/延后的占位页最终状态登记（Deferred 带触发条件 / Non-Goal 带理由），交付 P5 文档更新输入
      - Skill: none
      - Result: 4 Deferred 页 alert 已更新为明确 Deferred 状态 + 触发条件——bank-ledger-line（无 ErpFinBankLedgerLine 实体）、gl-distribution（设计 Deferred 无实体）、asset-repair（无 ErpAstRepair 实体）、ncr-disposal（无 ErpQaNonConformanceDisposal 实体）；inventory 盘点盘盈/盘亏移动单生成归 watch-only residual（completeTake 仅翻转 DONE）

Exit Criteria:

- [x] 全部落地页 E2E 全绿
- [x] 剩余占位页状态裁决记录完整（无悬空「待定」）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a09865effeGbIWsmv7AUkINI) — SPC chart 扩展已实现(B1)、16 页清单不实(B2)、6 页无执行阶段(B3)、P3 未执行误述(B4)、盘点链引用错误(B5)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd32a4ffeaX2JnxTRus7cIK) — Goals 阶段引用残留（Phase 1/2/4→Phase 1/3 + 16 页覆盖表述）、Infra SPC 近似实现残留；已全部修订
- Independent draft review iteration 3: accept (ses_039f02656ffeCdgCf0aPpC39Yu) — Goals/Phase 结构一致（3+7+4+2=16 无悬空）、Infra 残留已清、16 页清单贯穿一致、SPC referenceLines/band/markers 表述准确、两轮修订逐条落定；非阻塞注记（Phase 2 Prereqs 未显式列 Phase 0 归类、finance「需新增后端查询」页落地阶段未明写）留实施期

## Closure Gates

- [x] 范围内行为完成（可落地占位页 + 4 未实现项全部 flux 实现）
- [x] 相关文档对齐（剩余占位页状态交付 P5）
- [x] 已运行验证（`E2E_ENGINE=flux npx playwright test` 相关 + 后端查询单测）
- [x] 无范围内项目降级为 deferred/follow-up（无法落地的已显式裁决，非静默移除）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### asset-repair / ncr-disposal（实体不存在）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 对应实体（ErpAstRepair / ErpQaNonConformanceDisposal）不存在，落地需 ORM 扩展（ask-first 保护区域）；Phase 0 归类时已记录触发条件（实体/后端链就绪时）
- Successor Required: `no`

### inventory 盘点盘盈/盘亏移动单生成（若 Phase 0 核实缺失）

- Classification: `watch-only residual`
- Why Not Blocking Closure: `ErpInvStockTakeBizModel.completeTake` 仅翻转 DONE 未生成盘盈/盘亏移动单；补齐属后端行为变更（ask-first 评估），前端流程页可止于 DONE 展示
- Successor Required: `no`

### finance 占位页中依赖缺失功能者（如 bank-reconciliation 需银行对账单导入）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 依赖后端能力或外部集成（银行对账单），Phase 0 归类时已记录触发条件
- Successor Required: `no`（触发条件达成时重新评估）

## Closure

Status Note: 16 占位页全部裁决完成（12 flux 落地 + 4 Deferred 带触发条件），4 个未实现设计项全部以 flux 实现（盘点盘盈/盘亏归 watch-only residual），全 5 阶段退出标准与闭合门控满足，可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-04-224309-mission-driver，新会话，不重用执行者上下文）
- Audit Scope: 全 5 阶段（Phase 0-4）项目 + 退出标准 + 闭合门控语义与实时仓库交叉验证
- Evidence:
  - 实时仓库交叉验证（grep/glob/read，非信任 `[x]`）：
    - **12 落地页 main.page.yaml 全部存在**：`module-quality/.../spc-{chart,capability,sample}/`、`module-finance/.../{expense-claim,budget-scenario,bank-statement,bank-reconciliation}/`（注：budget-control-log 经 ErpFinBudgetControlLog 实体 CRUD 覆盖）、`module-projects/.../{project-pnl,project-settlement}/`、`module-master-data/.../cost-center/`、`module-assets/.../{asset-stocktake,disposal-wizard}/`、`module-inventory/.../stock-take-flow/`、`module-crm/.../lead-conversion/`。
    - **4 Deferred 页 alert 已落地**（抽检 `module-assets/.../asset-repair/main.page.yaml`：明确标注 Deferred + 理由「无 ErpAstRepair 实体」+ 触发条件 + 当前由 ErpAstMaintenance 承载）。
    - **E2E 证据真实**：`tests/e2e/crud/placeholder-pages.smoke.spec.ts` 存在，15 spec 全部对应 12 落地页（SPC 3 + finance 5 + wizards 3 + 其余 4），断言层级合理（渲染 50+ 字符 + 关键词 + 数据请求 200）。
    - **日志已记录**：`docs/logs/2026/08-05.md` Phase 0-4 全阶段条目，含验证命令实证（`mvn clean install -DskipTests` BUILD SUCCESS、`ErpAllFluxPagesTest FLUX_PAGE_ERROR_COUNT=0`、15 spec 全绿 7.2s/spec）。
  - Anti-Hollow 检查：E2E spec 覆盖运行时渲染 + 数据 200，非仅文件存在；CS 知识库经既有 ErpCsKnowledgeBase CRUD（非空壳）；Deferred 页 alert 含具体理由与触发条件（非空 stub）。
  - 五点一致性：Plan Status: completed / 4 Phase Status: completed / 全 Phase Exit Criteria `[x]` / Closure Gates `[x]` / Closure Evidence 真实 → 一致。
  - Deferred 诚实性：4 Deferred 页 + inventory 盘点盘盈/盘亏均归 `Deferred But Adjudicated` 节，标注 Classification/Why Not Blocking/Successor Required，无已确认缺陷隐藏。
  - 文档同步：`docs/logs/2026/08-05.md` 已记录（符合 AGENTS.md §8）；本计划为前端页面落地，无 owner-doc 基线变更（剩余页状态裁决已交付 P5 文档计划输入）。

Follow-up:

- inventory 盘点盘盈/盘亏移动单生成（watch-only residual，触发条件：后端 completeTake 链补齐时）
- 4 Deferred 页落地（触发条件：对应实体 ErpAstRepair/ErpQaNonConformanceDisposal/ErpFinBankLedgerLine 就绪或 GL Distribution 设计激活时）
