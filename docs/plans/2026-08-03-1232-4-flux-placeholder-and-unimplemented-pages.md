# 2026-08-03-1232-4-flux-placeholder-and-unimplemented-pages 占位页与未实现项 Flux 落地

> Plan Status: draft
> Last Reviewed: 2026-08-03
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
- 可立即落地的占位页以 flux 实现替换 7 行 alert（含 SPC 三件套、finance/projects/assets/master-data 各可落地页），**16 页均有归类（Phase 1/3 落地 or Deferred+理由，无悬空）**
- SPC 三件套以 flux chart 完整能力实现（**referenceLines/band/markers 已由 nop-chaos-flux 提供**），数据来自既有后端
- 4 个未实现设计项以 flux 原生实现（3 向导用 wizard + valuesPath；知识库用 crud/tree）
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

Status: planned
Targets: `module-*/erp-*-web/.../pages/**/main.page.yaml`（占位候选）
Skill: none

- Item Types: `Decision`（16 占位页逐页归类：落地/延后/Non-Goal）
- Prereqs: 无（可并行于 P1）

- [ ] 实时 grep 确认 16 个占位页清单（7 行 alert 模式），逐页核对后端能力就绪度（实体是否存在/查询是否可用）；**核实 inventory 盘点盘盈/盘亏生成链**（`ErpInvStockTakeBizModel.completeTake` 是否仅翻转 DONE）
      - Skill: none
- [ ] Decision: 每页归类——可立即落地（后端就绪）/ 需新增后端查询 / Deferred（功能依赖缺失或触发条件未到）。记录理由与替代方案；**asset-repair（ErpAstRepair 实体不存在）与 ncr-disposal（ErpQaNonConformanceDisposal 实体不存在，若核）归 Deferred（ORM ask-first 保护区域）**；inventory 盘点若盘盈/盘亏生成缺失 → Deferred 或列入后端补充（ask-first 评估）
      - Skill: none

Exit Criteria:

- [ ] 16 占位页清单与归类已落盘（计划内），每页理由明确，无模糊「待定」；6 页（project-pnl/project-settlement/cost-center/ncr-disposal/asset-stocktake/asset-repair）的归类确定执行阶段（落地 or Deferred+理由）

### Phase 1 - 高价值占位页落地（SPC 三件套 + 财务优先）

Status: planned
Targets: `module-quality/.../pages/spc-{chart,capability,sample}/`、`module-finance/.../pages/`（7 页中可落地者）
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux 页面落地）
- Prereqs: P1 Phase 0-1；Phase 0 归类

- [ ] SPC chart 页：flux `chart`（**referenceLines 画 UCL/LCL/CL + band 阴影 + markers 失控点，数据来自 `getSpcControlChartData`**）+ `crud`（样本列表）
      - Skill: `nop-frontend-dev`
- [ ] SPC capability/sample 页：`crud` + `chart`（能力分布）复用既有 BizModel
      - Skill: `nop-frontend-dev`
- [ ] finance 占位页落地（按 Phase 0 归类中后端就绪者）：`crud`/`chart`/`wizard` 组合
      - Skill: `nop-frontend-dev`
- [ ] Proof: 落地页 E2E（flux 引擎）——渲染 + 数据断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] SPC 三件套以 flux 渲染（referenceLines/band/markers 完整能力），数据来自既有后端
- [ ] finance 可落地占位页以 flux 实现
- [ ] 落地页 E2E 证据通过

### Phase 2 - 4 个未实现设计项落地

Status: planned
Targets: `module-{assets,inventory,crm,cs}-*/.../pages/`
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux 页面）+ `Fix`（设计→实现差距弥合）
- Prereqs: P1 Phase 0-1

- [ ] assets 处置 3 步向导：`wizard`（处置类型选择→清理损益预览→确认提交）+ 既有处置 mutation（`ErpAstDisposalProcessor` 链）
      - Skill: `nop-frontend-dev`
- [ ] inventory 盘点 3 阶段流程：`wizard` 或 `steps` + `crud`（盘点单三状态流程）——按 Phase 0 核实结论：若盘盈/盘亏生成缺失，流程页止于 DONE 状态展示（归 Deferred 或列入后端补充）
      - Skill: `nop-frontend-dev`
- [ ] crm 转化 3 步向导：`wizard`（线索→商机→转化确认）+ `moveStage` mutation
      - Skill: `nop-frontend-dev`
- [ ] cs 知识库：`crud` + `tree`（分类）+ 富文本（核实 flux 编辑器能力：input-textarea/code-editor；缺失则 textarea/markdown 降级）
      - Skill: `nop-frontend-dev`
- [ ] Proof: 4 项 E2E（flux 引擎）——向导流断言 + 知识库 CRUD 断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 个未实现设计项以 flux 实现（后端链复用，盘点按核实结论）
- [ ] 4 项 E2E 证据通过

### Phase 3 - 其余域占位页落地

Status: planned
Targets: `module-{projects,assets,master-data}-*/.../pages/`（Phase 0 归类为可落地者）
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux 页面）
- Prereqs: Phase 0 归类

- [ ] project-pnl/project-settlement：`crud` + `chart`（损益/结算汇总）复用既有 BizModel
      - Skill: `nop-frontend-dev`
- [ ] cost-center：`crud`/`tree`（成本中心树形若支持）
      - Skill: `nop-frontend-dev`
- [ ] asset-stocktake：`crud`（盘点单，对应 ErpAstInventory）
      - Skill: `nop-frontend-dev`
- [ ] Proof: 落地页 E2E（flux 引擎）——渲染 + 数据断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 可落地其余域占位页以 flux 实现
- [ ] 落地页 E2E 证据通过

### Phase 4 - 收口

Status: planned
Targets: 全部落地页
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-3

- [ ] 全部落地页 flux 引擎 E2E 回归通过（与 P1/P2/P3 聚合）
      - Skill: none
- [ ] Decision: 无法落地/延后的占位页最终状态登记（Deferred 带触发条件 / Non-Goal 带理由），交付 P5 文档更新输入
      - Skill: none

Exit Criteria:

- [ ] 全部落地页 E2E 全绿
- [ ] 剩余占位页状态裁决记录完整（无悬空「待定」）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a09865effeGbIWsmv7AUkINI) — SPC chart 扩展已实现(B1)、16 页清单不实(B2)、6 页无执行阶段(B3)、P3 未执行误述(B4)、盘点链引用错误(B5)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd32a4ffeaX2JnxTRus7cIK) — Goals 阶段引用残留（Phase 1/2/4→Phase 1/3 + 16 页覆盖表述）、Infra SPC 近似实现残留；已全部修订

## Closure Gates

- [ ] 范围内行为完成（可落地占位页 + 4 未实现项全部 flux 实现）
- [ ] 相关文档对齐（剩余占位页状态交付 P5）
- [ ] 已运行验证（`E2E_ENGINE=flux npx playwright test` 相关 + 后端查询单测）
- [ ] 无范围内项目降级为 deferred/follow-up（无法落地的已显式裁决，非静默移除）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
