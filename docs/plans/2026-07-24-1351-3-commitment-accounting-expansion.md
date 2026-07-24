# 2026-07-24-1351-3 Commitment Accounting Expansion

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/backlog/deepening-roadmap.md` §8.4 A2 落地证据 Deferred successor「承付款业务场景全集（销售订单/付款单等其他场景）」（line 227）；`docs/plans/2026-07-21-1206-2-finance-budget-multi-year-carryforward.md` §Deferred But Adjudicated
> Related: `docs/plans/2026-07-21-1206-2-finance-budget-multi-year-carryforward.md`（A2 — commitment 基础设施 + 采购订单试点）；`docs/design/finance/budget.md`（A2 EXPAND owner doc，承付段 L238-252 当前仅采购）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-24，对 finance commitment 链 + sales Processor + finance Processor 目录 + budget.md 扫描）：

### A2 已落地的基础设施（本计划复用）

- **SPI**：`IErpFinBudgetCommitmentBiz`（`erp-fin-dao/.../biz/IErpFinBudgetCommitmentBiz.java`）— `commit(sourceBillType, sourceBillCode, subjectId, costCenterId, periodId, amount, context)` + `release(sourceBillType, sourceBillCode, context)`。泛型，按 sourceBillType + sourceBillCode 索引。
- **实现**：`ErpFinBudgetCommitmentBizModel`（`erp-fin-service/.../budget/ErpFinBudgetCommitmentBizModel.java`）— config-gate（`isCommitmentEnabled()` L116）+ 委托 `CommitmentVoucherGenerator`。
- **凭证生成器**：`CommitmentVoucherGenerator`（`erp-fin-service/.../budget/CommitmentVoucherGenerator.java`）— `generateCommitment` 写 postingType=COMMITMENT 凭证 + 1 行 Dr/Cr + ErpFinVoucherBillR（billType = `"PURCHASE_ORDER_COMMITMENT"` ErpFinConstants:434）。该 billType 常量在 7 处使用：`generateCommitment`（L150/157/159）+ `reverseCommitment`（L222/230/232）+ `findCommitmentVouchers`（L242/265）按 billCode + billType 查找。新增场景的 billType 必须同时贯穿 commit/reverse/find 三路径以保持占用/释放对称性。
- **dormant Provider**：`CommitmentAcctDocProvider`（getSupportedBusinessTypes 返回空集，文档化 stub）。
- **config-gate**：`erp-fin.budget-commitment-enabled`（`ErpFinConstants.java:412`）默认 false。配套必配 `erp-fin.budget-commitment-subject-code`（L414）。
- **既有采购钩子（3 接入点）**：
  - `ErpPurOrderProcessor.approve`（L75→L86 `runCommitmentCommitHook`）→ commit `"PURCHASE_ORDER"`
  - `ErpPurOrderProcessor.reverseApprove`（L98→L106）+ `cancel`（L111→L116）→ `runCommitmentReleaseHook` → release
  - `ErpPurInvoiceProcessor.approve`（L78→L94 `runCommitmentReleaseOnInvoiceApproveHook`）→ release（经 invoiceLine→receiveLine→receive→order.code 反查）

### 销售域扩展目标现状（grep 实测零 budget 接线）

| Processor | approve() | reverseApprove() | budget check？ | commitment？ |
|-----------|-----------|------------------|----------------|--------------|
| `ErpSalOrderProcessor` | L69 | L90 | **否**（仅 creditLimitChecker AR 信用 L164） | **否** |
| `ErpSalInvoiceProcessor` | L71 | L94 | **否** | **否** |

**关键发现 1**：sales Processor 全无 budget 接线（既无 check 也无 commitment）。`CommitmentVoucherGenerator.findCommitmentVouchers`（L242/265）仅按 `billCode + billType` 查找，billType 当前硬编码 `"PURCHASE_ORDER_COMMITMENT"`。扩展至 sales 需新 sourceBillType/billType 常量以避免 lookup 碰撞，且须贯穿 reverse 路径。

### 资金单据域现状（grep 实测 — 关键边界发现）

`module-finance/.../processor/` 目录实际 Processor 为：`ErpFinAccountingPeriodProcessor`、`ErpFinNotesReceivableProcessor`、`ErpFinNotesPayableProcessor`、`ErpFinExpenseClaimProcessor`、`ErpFinBadDebtProcessor`、`ErpFinEmployeeAdvanceProcessor`。**`ErpFinPaymentProcessor` / `ErpFinReceiptProcessor` 不存在**，全仓 grep `ErpFinPayment`/`ErpFinReceipt` 实体/BizModel/ORM 定义均为空。

**关键发现 2**：A2 Deferred 原文「承付款业务场景全集（销售订单/付款单等其他场景）」中的「付款单」在本仓**无对应领域对象**。资金承诺场景（付款单/收款单）的前提是这些领域对象存在，故资金承诺归 Deferred successor（触发条件见 §Deferred But Adjudicated），本计划不纳入。

### owner doc 现状

`budget.md` 承付段（L238-252）业务规则 3（L82）仅描述采购承诺；sales 承付为**未文档化方向**（既非 in-scope 也非显式 deferred）。

### 保护区域提示

本工作触及财务保护区域（预算承付凭证）。按 `AGENTS.md`，owner doc（`budget.md` EXPAND）须先描述 sales 承付语义，且 **sales 承付的业务合理性须经 Phase 1 Decision 裁决**（commitment/encumbrance 传统是支出面概念；收入面承付虽在预算会计中成立但非主流，Phase 1 须明确采纳/否决）。

## Goals

- **裁决** sales 承付（销售订单 approve 生成 COMMITMENT 凭证）的业务合理性，并在 owner doc 落盘结论（Phase 1 Decision 门控后续实施）。
- **若 Phase 1 裁决 sales 承付适用**：将 commitment 凭证生成从采购订单扩展至销售订单（ErpSalOrder）approve/reverseApprove + 销售发票 approve 时 release-on-invoice，使收入预算预留可经承付凭证表达。
- 泛化 `CommitmentVoucherGenerator` 的 billType/sourceBillType 常量（贯穿 commit/reverse/find 三路径），避免跨场景 lookup 碰撞。
- EXPAND `docs/design/finance/budget.md` 承付段补充 sales 承付语义裁决结论 + 接入点表（若适用）或将 sales 承付显式登记为 Deferred（若裁决否决）。

## Non-Goals

- **不改 ORM 实体**（A2 已落地 ErpFinBudgetScenario/Line/ControlLog + RollforwardLog/CarryForwardLog，本计划仅消费）。
- **不为 sales 新增 budget check（IErpFinBudgetControlBiz.check）**（sales 当前无 budget check；check 扩展是独立的预算控制面，本计划聚焦承付凭证 postingType=COMMITMENT；sales budget check 归 successor）。
- **不做资金承诺（付款单/收款单）**（ErpFinPayment/ErpFinReceipt 领域对象不存在；归 Deferred successor，触发条件=资金单据域对象构建后）。
- **不做 commitment 跨年度结转**（A2 Deferred「commitment 一并结转」）。
- **不做 GL Mapping 接入 commitment**（CommitmentAcctDocProvider dormant，科目经 config 解析；多维规则接入归 GL Mapping rollout successor）。
- **不改变既有采购 commitment 3 接入点行为**（仅扩展新场景，零回归）。

## Task Route

- Type: `architecture change`（Generator 常量泛化 + 跨域 Processor 钩子）+ `implementation-only change`
- Owner Docs: `docs/design/finance/budget.md`（A2 EXPAND 承付段 L238-252）、`docs/design/finance/posting.md`（承付段 EXPAND）
- Skill Selection Basis: `nop-backend-dev`（Processor 钩子 + 跨实体 SPI + 事务边界 + config-gate + 保护区域决策门）；需阅读 `nop-entropy/docs-for-ai/02-core-guides/` 跨实体访问 + transaction-boundary 文档

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- config-gate `erp-fin.budget-commitment-enabled` 保持默认 false；测试中开启。Phase 1 Decision 将裁定是否新增 `erp-fin.budget-commitment-sales-subject-code` 配套科目配置（与既有 `erp-fin.budget-commitment-subject-code` 并列，使 sales 承付科目独立配置）。

## Execution Plan

### Phase 1 - Sales 承付合理性裁决 + Generator 泛化 + Owner Doc 设计

Status: completed
Targets: `CommitmentVoucherGenerator`、`ErpFinConstants`、`docs/design/finance/budget.md`、`docs/design/finance/posting.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: A2 基础设施已落地（plan 2026-07-21-1206-2 completed）

- [x] Decision: sales 承付业务合理性 — **裁决 (a) 采纳**（收入预算预留，与采购支出承诺对称；iDempiere Fact.java COMMITMENT 支持对称；config-gate 默认关隔离风险；Dr/Cr 经 subject.direction 自动取）。替代方案 (b) 否决（收入面承付非主流但 config-gate 已隔离）。残留风险：业务方接受度需验证（缓解：默认关 + 独立科目配置）。门控 Phase 2。
  - Skill: `nop-backend-dev`
- [x] Add（无条件）：泛化 `CommitmentVoucherGenerator` 的 billType/sourceBillType — 新增 `SALES_ORDER_COMMITMENT` 常量（ErpFinConstants）+ `COMMITMENT_SOURCE_BILL_SALES_ORDER`；`resolveCommitmentBillType(sourceBillType)` 按 sourceBillType 派发对应 billType；`findCommitmentVouchers` + `reverseCommitment` + `hasUnreversedCommitment` 路径同步使用派发 billType，保证占用/释放 lookup 对称（grep 核实 SALES_ORDER_COMMITMENT 在 generateCommitment + reverseCommitment + findCommitmentVouchers 三路径）。
- [x] Add（条件依赖 Decision (a)）：EXPAND `budget.md` 承付段补充「sales 承付扩展」子段（Decision 裁决结论 + Generator 泛化说明 + sales 接入点表 + 科目独立配置）+ 业务规则 3 扩展；EXPAND `posting.md` 承付段 config-gated 启用回链 sales 承付。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅写此阶段交付的可观察结果。完整仓库 build 在 Closure Gates。

- [x] sales 承付 Decision 落盘（选择 + 替代方案 + 残留风险，明确门控 Phase 2）；CommitmentVoucherGenerator billType 常量泛化（resolveCommitmentBillType 贯穿 commit/reverse/find 三路径）；budget.md 承付段含裁决结论

### Phase 2 - 销售订单 commitment 钩子（gated by Phase 1 Decision (a)）

Status: completed
Targets: `ErpSalOrderProcessor`、`ErpSalInvoiceProcessor`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Decision 裁决 (a)（采纳 sales 承付）

- [x] Add: `ErpSalOrderProcessor.approve`（L69）后置 `runCommitmentCommitHook`（镜像 `ErpPurOrderProcessor:207-221` 范式，sourceBillType=`"SALES_ORDER"`，subjectId/periodId/amount 经订单头/行派生）+ `reverseApprove`（L90）+ cancel `runCommitmentReleaseHook`。config-gated。
- [x] Add: `ErpSalInvoiceProcessor.approve`（L71）后置 release-on-invoice 镜像钩子（经 invoiceLine→deliveryLine→delivery→order.code 反查，对齐 `ErpPurInvoiceProcessor:306-373` 范式）。
- [x] Proof: 单元测试 — SO approve 产 COMMITMENT 凭证（billType=SALES_ORDER_COMMITMENT）+ SO reverseApprove/cancel 红冲 + invoice approve release + 重复 release 守卫 + 既有采购 commitment 测试零回归。复用 `TestErpFinBudgetCommitment` + `TestErpPurOrderCommitment` 范式扩展（新增 `TestErpSalOrderCommitment` + `TestErpFinBudgetCommitment.testSalesCommitmentDispatchesSalesBillType`）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] SO approve→COMMITMENT 凭证可观测（postingType + billType）；sales service 局部 `mvn test` 通过（125 tests）；finance commitment 测试零回归

### Phase 3 - Owner doc 回链 + roadmap 同步 + 全仓库验证

Status: completed
Targets: `docs/design/finance/budget.md`、`docs/design/finance/posting.md`、`docs/backlog/deepening-roadmap.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1-2

- [x] Add: budget.md 承付段回链实际落地路径（sales 接入点表若适用 + Generator 泛化说明 + config-gate + 科目配置）；posting.md 承付段回链。deepening-roadmap §8.4 Deferred successor「承付款业务场景全集」标注 sales 已落地/已否决 + 资金承诺 successor 登记。
- [x] Add: 更新每日开发日志 `docs/logs/2026/07-24.md`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] owner doc 回链完成；roadmap Deferred successor 状态更新

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_06d4f5821ffeP2QkfCLyhN4CLd) because (1) Rule 1 违反 — ErpFinPaymentProcessor/ErpFinReceiptProcessor 不存在（全仓零命中），Goal #2 + Phase 3 建立在不存在目标上；(2) Rule 9+2 违反 — Goal #1 预先裁决了 Phase 1 Decision #1 应裁定的问题（sales 承付合理性）。已修正：资金承诺整体降级为 Deferred successor（触发=资金单据域对象构建后）+ Goal 改为 Phase 1 Decision 门控 + Generator 泛化标注贯穿 reverse 路径 + Phase 重构为 3 phase（Decision 门控→gated 实施→owner doc）+ 「可能新增」anti-slack 修正。基线 SPI/Generator/Purchase-hook 事实全部核实通过。
- Independent draft review iteration 2: acceptable as-is (ses_06d4a9b9fffecrE9zaFmi87Txt) — 4 项 iteration-1 阻塞全修复（资金承诺 descoped / Decision-gated / anti-slack / 3-path billType）+ 计划在 Decision (b)（否决 sales）下仍保持连贯（Phase 1 仍交付 Generator 泛化 + Decision 落盘 + budget.md Deferred 登记）+ 模板完整。无阻塞问题。

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + finance/sales service `mvn test` 一次。

- [x] 范围内行为完成（Phase 1 Generator 泛化贯穿 commit/reverse/find 三路径 + Decision 落盘；若裁决采纳：SO approve→COMMITMENT 凭证 + reverseApprove/cancel 红冲 + 既有采购 commitment 零回归 + config-gate 默认 false）
- [x] 相关文档对齐（budget.md 承付段 EXPAND sales 裁决结论；posting.md 承付段）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + finance/sales `mvn test` 全绿）
- [x] 无范围内项目降级为 deferred/follow-up（资金承诺降级经核实为领域对象不存在的前提性降级，非范围内项目偷降；sales 承付若 Phase 1 裁决否决则 Phase 2 明确标记为 Decision-gated 不实施）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 资金承诺（付款单/收款单 commitment）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpFinPayment`/`ErpFinReceipt` 领域对象在本仓不存在（grep 全仓零命中）；资金承诺的前提是这些领域对象先构建。A2 Deferred 原文「付款单」假设的资金单据域尚未落地。
- Successor Required: `yes`（触发条件：付款单/收款单领域对象构建后 + budget owner doc 授权资金承诺语义）

### Sales budget check（IErpFinBudgetControlBiz.check）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: sales 当前无 budget check（仅 AR 信用控制）；check 扩展是独立预算控制面，与 commitment posting 解耦
- Successor Required: `yes`（触发条件：sales 收入预算控制需求 + budget owner doc 授权）

### commitment 跨年度结转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2 Deferred — commitment 一并结转语义复杂，归 successor
- Successor Required: `yes`（触发条件：跨年度 commitment 余额处理需求）

### GL Mapping 接入 commitment 多维规则

- Classification: `optimization candidate`
- Why Not Blocking Closure: CommitmentAcctDocProvider dormant，科目经 config 解析；多维规则接入归 GL Mapping rollout successor
- Successor Required: `yes`（触发条件：commitment 科目需多维差异化 + A1 resolver 稳定）

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: independent closure audit (ses_06c15edb2ffeFB5Ehmda5TPmAD) — PASS. All 8 claims verified true against live repository: Generator generalization (resolveCommitmentBillType 贯穿 commit/reverse/find), Constants (SALES_ORDER_COMMITMENT + CONFIG_BUDGET_COMMITMENT_SALES_SUBJECT_CODE), ErpSalOrderProcessor hooks (commit/release config-gated), ErpSalInvoiceProcessor release-on-invoice hook, tests (TestErpSalOrderCommitment 3 场景 + TestErpFinBudgetCommitment sales dispatch), owner docs (budget.md §sales 承付扩展 + posting.md), roadmap sync, daily log.

Follow-up:

- 资金承诺（付款单/收款单，触发：资金单据域对象构建后）
- 承付款业务场景全集剩余项（其他业务单据类型，触发：业务方明确需求）
- commitment 跨年度结转（触发：跨年度 commitment 余额处理需求）
