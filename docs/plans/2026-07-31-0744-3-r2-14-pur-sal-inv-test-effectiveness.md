# 2026-07-31-0744-3-r2-14-pur-sal-inv-test-effectiveness R2.14 pur+sal+inv 过账/核销/成本链路测试有效性（残差补强）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.14（P1-MA4-021 残差）
> Related: `docs/audits/arm-index.md`（P1-MA4-021/020）、R1.8（三单匹配，已落地 settle recheck 测试）、R1.9（多币种，已落地 pur/sal 多币种凭证行级测试）、R1.12（成本方法，已落地 STANDARD 红冲不变量+SPECIFIC 历史成本测试）、R1.16（业财悬挂，已落地到岸成本 reverse 告警代码+InvPosting 悬挂测试）、R1.17（reverseApprove，已落地 PurReversalListener.rollbackReceive 测试）、R2.10/R2.13（同族测试有效性残差范式）
> Audit: required（独立草案审查 + 独立 closure audit）

## Current Baseline

P1-MA4-021（finding 写于 R1.8/R1.9/R1.12/R1.16/R1.17 之前）的子项 (a)(c)(e)(g) **已由 R1.x 落地的测试大量闭合**。独立草案审查（fresh-session 实测）确认下列既有测试已覆盖原 finding 列为「零覆盖」的多项，本计划仅补**残差缺口**。逐项实测基线：

**已闭合子项（不在本计划范围，避免重复）**：
- 多币种凭证行级 amountSource/amountFunctional（P1-MA2-002/009）：`TestErpPurMultiCurrencyPosting`（testMultiCurrencyApInvoiceLineAmounts :67-109 RATE=7.0 断言 amountSource=100≠amountFunctional=700 + 借贷方折算；testMultiCurrencyPaymentLineAmounts :112-133，R1.9）+ `TestErpSalMultiCurrencyReconFx`（testMultiCurrencyReconciliationFxGain :79-155 RATE_INV=7.0/RATE_RECV=7.1 断言 assertLineMultiCurrency :158-169 per-line amountSource≠amountFunctional + FX 损益，R1.9）—— 闭合 (a)；inventory 无独立多币种过账路径（库存账本单/本位币 by design）
- STANDARD 红冲成本不变量（P1-MA2-024）：`TestErpInvStandardCosting.testReverseRestoresCostInvariantAcrossRevaluation`（:200-238 incoming 20@actual12 std10→outgoing 8@old-std-10→publish FIRMED rollup=15→reverse 断言 reverse-incoming unitCost=10 非 15 + balance.totalCost 恢复，R1.12）—— 闭合 (c)
- SPECIFIC 历史成本守卫（P1-MA2-023 实体）：`TestErpInvSpecificCosting.testOutgoingIgnoresFutureDatedSameBatchLayer`（:123-152 findSpecificLayers businessDate 过滤，R1.12）—— 闭合 P1-MA2-023 主体
- PurReversalListener.rollbackReceive 不对称（P1-MA2-051）：`TestPurReversalListenerReceiveRollback`（testRollbackReceiveAlignsToRejectedLikeOthers :41-66 posted=false+REJECTED；testRollbackReceiveNoOpWhenNotPosted :68-89，R1.17）—— 闭合 (e)
- settle 三单匹配二次门禁（P1-MA2-003）：`TestErpPurSettleThreeWayMatchRecheck`（testSettleRejectsPriceMismatchWhenRecheckEnabled :77-90；testSettleRejectsQtyMismatchWhenRecheckEnabled :96-106；testSettlePassesWhenMatchWithinTolerance :112-124，R1.8 config-gated）—— 闭合 (g)

**残差缺口（本计划范围）**：
- **G1（P1-MA4-021(b)）dispatcher tryPost 失败悬挂 posted=false 可观测**：pur/sal 6 dispatcher + inv 3 dispatcher 的 tryPost catch 吞咽路径无测试触发——pur `PurInvoicePostingDispatcher.tryPost`（:39-48）/`PurPaymentPostingDispatcher.tryPost`（:39-48）/`PurReturnPostingDispatcher.tryPost`（:44-54）+ sal `SalInvoicePostingDispatcher.tryPost`（:39-48）/`SalReceiptPostingDispatcher.tryPost`（:39-48）/`SalReturnPostingDispatcher.tryPost`（:51-61）全为纯 catch(LOG.warn)→posted=false 无告警；inv `LandedCostPostingDispatcher`/`CostAdjustmentPostingDispatcher`/`OwnershipTransferPostingDispatcher` tryPost 失败悬挂未测（**注**：inv `InvPosting` 已由 `TestErpInvPosting.testPostingFailureLeavesMoveDonePostedFalse` :107-115 覆盖，不在本计划范围）。pur/sal 测试树零 Mockito（grep 无命中），现有测试仅在正常 reverse 路径断言 posted=false。残差 = 确定性诱导 post 失败（seed 无会计期间/清空科目映射，复用 InvPosting 无 mock 范式）→断言 posted=false 持续 + 业务单据终态不受影响。
- **G2（P1-MA4-021(f)）SalReversalListener 3/4 rollback 路径**：`SalReversalListener`（posting/，:50-65 dispatch）4 rollback 方法仅 `rollbackInvoice`（AR_INVOICE）由 `TestErpSalFinanceReversalWriteback.testFinanceReverseRollsBackSalesInvoicePostedAndApproveStatus`（:63-94）覆盖；`rollbackReceipt`（RECEIPT）/`rollbackReturn`（SALES_RETURN）/`rollbackDelivery`（SALES_OUTPUT）零覆盖（无 TestSalReversalListener* 文件）。残差 = 补 3 路径对称测试（注：rollbackDelivery 为 R1.17 P2-MA2-057 watch-only deferred，但 Receipt/Return 未 deferred 仍须补）。
- **G3（P1-MA4-021(h)）到岸成本反向悬挂**：R1.16 已落地代码 `ErpInvLandedCostProcessor.dispatchReverseFailureAlert`（:511，reverse catch :199 调用），但**零测试**驱动 reverse-throws→posted=false+告警路径。`TestErpInvLandedCostReversal`（:88-143 正常 reverse + :148-169 not-posted guard）未覆盖 reverse 失败。grep `dispatchReverseFailureAlert` 跨 inv 测试树零命中。残差 = 确定性诱导 reverse 失败→断言 posted=false + 告警触发。
- **G4（P1-MA4-021(d) 残差裁决）CostAdjustmentService SPECIFIC 分支**：注——P1-MA2-023 主体（SpecificCostingStrategy.findSpecificLayers 历史成本守卫）已由 R1.12 闭合（见上）。sub-item (d) 文本"CostAdjustmentService 无 SPECIFIC 分支"指向**独立未跟踪项**：`CostAdjustmentService.applyLine`（:108-112）仅 FIFO 分支 vs applyAverageLike else，无 SPECIFIC 分支（SPECIFIC 物料成本调整落入 applyAverageLike 设 avgCost=newUnitCost，对 SPECIFIC per-batch 层模型错误）；`TestErpInvCostAdjust` 无 SPECIFIC 用例；R1.12 Non-Goals（:44）显式排除 CostAdjustmentService 重构。**本计划裁决**：(d) 的 tracked P1-MA2-023 测试有效性已闭合（findSpecificLayers 测试落地）；CostAdjustmentService SPECIFIC 分支为独立代码质量项非 P1-MA4-021 tracked 范围，登记为 watch-only successor（见 Deferred），不在本测试计划补测试（须先代码修复加 SPECIFIC 分支，触及成本保护区域须 owner doc+人工批准，非测试有效性范围）。

剩余差距：G1 + G2 + G3 三个残差（G4 裁决为 successor）。本计划为**纯测试新增**（无生产 Java/ORM/view.xml 变更），不触及采购/销售/库存/成本保护区域运行时行为——仅补测试使业财悬挂状态、销售 reversal rollback、到岸成本 reverse 悬挂对测试可观测。

## Goals

- G1：dispatcher tryPost 失败悬挂测试——确定性诱导 pur 3 + sal 3 + inv 3（LandedCost/CostAdjust/OwnershipTransfer）dispatcher post 失败，断言 posted=false 持续 + 业务单据终态不受影响（闭合 P1-MA4-021(b) + P1-MA2-032 family 测试可见性）
- G2：SalReversalListener rollback 对称测试——补 rollbackReceipt/rollbackReturn/rollbackDelivery 3 路径，断言 posted=false+APPROVE_STATUS_REJECTED 对齐 rollbackInvoice 行为（rollbackDelivery 标注 P2-MA2-057 watch-only 但仍补以闭合对称性）
- G3：到岸成本反向悬挂测试——确定性诱导 ErpInvLandedCostProcessor reverse 失败，断言 posted=false + dispatchReverseFailureAlert 触发（闭合 P1-MA4-020 测试可见性）

## Non-Goals

- 不重复实现已闭合子项（多币种凭证行级 / STANDARD 红冲不变量 / SPECIFIC 历史成本守卫 / PurReversalListener.rollbackReceive / settle 三单匹配二次门禁 / InvPosting 悬挂——见 Current Baseline 既有测试清单）
- 不修改任何生产 Java 代码（BizModel/Processor/Dispatcher/Listener/Strategy/Service）——若 G1-G3 测试发现与 owner doc 不符的真实行为缺陷，按不可降级 Fix 规则升级为独立修复计划，不在本测试计划中静默修改生产代码
- 不为 pur/sal 6 dispatcher 补告警 dispatch（R1.16 未覆盖的代码侧告警）——属代码修复非测试有效性；G1 仅测 posted=false 状态可观测，告警代码侧 successor 由 G1 测试证据触发独立代码修复 plan
- 不实现 CostAdjustmentService SPECIFIC 分支（G4 裁决 successor，触及成本保护区域须 owner doc+人工批准，非测试有效性范围）
- 不补 finance/mfg/assets/hr 测试有效性（分别归 R2.10 done / R2.11 / R2.12 / R2.13 done）
- 不补 R2.15 view.xml drift

## Task Route

- Type: `implementation-only change`（纯测试新增）
- Owner Docs: `docs/design/purchase/state-machine.md`、`docs/design/sales/state-machine.md`、`docs/design/inventory/`（过账悬挂 + reversal rollback + 到岸成本 reverse owner-doc 语义）。测试断言的预期行为须与 owner doc 一致
- Skill Selection Basis: 工作方法为 Nop 服务层集成测试（`JunitAutoTestCase` + Facade Java API + seed/output/assert + 无 mock 确定性失败诱导 + listener 单元直调）→ `nop-testing`（基类选择、@NopTestConfig、seed 只追加、拒绝路径快照处理、三层验证模型）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 localDb 集成测试，无端口/外部服务；确定性失败诱导复用 inventory 既有范式——seed 无会计期间/清空科目配置触发 post 失败 + listener 单元直调复用 TestDepreciationPostingFailureAlert/PurReversalListenerReceiveRollback 既有 Proxy-stub 范式，无需 Mockito）

## Execution Plan

### Phase 1 - dispatcher 悬挂 + SalReversalListener rollback + 到岸成本 reverse 悬挂（G1+G2+G3）

Status: completed
Targets: pur/sal/inv 各域过账测试 + 新增 `TestSalReversalListenerRollback*.java`（listener 单元直调）+ `TestErpInvLandedCostReversal.java`（新增测试方法 + 对应 `_cases/` 快照/seed）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: R2.0 done（已 done）；R1.8/R1.9/R1.12 done（已闭合子项基线）；R1.16 done（到岸成本 reverse 告警代码已落地，InvPosting 悬挂测试范式）；R1.17 done（PurReversalListener.rollbackReceive 范式）

- [x] Add: G1 dispatcher tryPost 失败悬挂测试 — 确定性诱导 post 失败（subclass/Proxy 桩 executor/voucherBiz 抛 NopException，对齐 R1.16 TestDepreciationPostingFailureAlert 无 Mockito 范式；dispatcher 为 Facade 编排层，tryPost→false/null 即「posted=false 悬挂」可观测契约，happy-path posted=true 由既有集成测试覆盖），对 pur 3（Invoice/Payment/Return，`TestErpPurPostingDispatcherFailureHangs`）+ sal 3（Invoice/Receipt/Return，`TestErpSalPostingDispatcherFailureHangs`）+ inv 3（LandedCost/CostAdjust/OwnershipTransfer，`TestErpInvPostingDispatcherFailureHangs`）dispatcher 各断言悬挂返回 + 业务单据终态不受影响；闭合 P1-MA4-021(b) + P1-MA2-032 family 测试可见性
  - Skill: `nop-testing`
- [x] Add: G2 SalReversalListener rollback 对称测试 — 新建 `TestSalReversalListenerRollback` 单元直调（复用 TestPurReversalListenerReceiveRollback 范式），补 rollbackReceipt/rollbackReturn（断言 posted=false+APPROVED→REJECTED 对齐 rollbackInvoice）+ rollbackDelivery（断言 posted=false、approveStatus 不变——库存物理冲销独立于凭证红冲，该不对称为 P2-MA2-057 watch-only 当前设计行为，非新缺陷）
  - Skill: `nop-testing`
- [x] Add: G3 到岸成本反向悬挂测试 — 子类桩 `LandedCostPostingDispatcher.reverse` 抛 NopException（避开「无原始凭证 reverse 为幂等 no-op」陷阱）驱动 `ErpInvLandedCostProcessor.doReverseApprove` + Proxy 桩 `IErpSysNotificationBiz` 捕获事件类型，断言 posted=false + approveStatus=REJECTED + 告警事件类型 `inv.landed-cost-reverse-failure`（`TestErpInvLandedCostReverseFailureAlert`，含 null notificationBiz 静默跳过用例）；闭合 P1-MA4-020 测试可见性
  - Skill: `nop-testing`
- [x] Proof: Phase 1 新增测试方法 CHECKING 全绿（14 测试：pur 3 + sal 6 + inv 5）
  - `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-inventory/erp-inv-service` 全绿（purchase 135 / sales 147 / inventory 全绿，含 14 新增测试）
  - Skill: none

Exit Criteria:

> pur+sal+inv 业财悬挂状态 + 销售 reversal rollback + 到岸成本 reverse 悬挂补齐，使 3 类缺陷对测试可观测。

- [x] G1（9 dispatcher posted=false 持续+终态不受影响）+ G2（SalReversalListener 3 rollback 路径对称）+ G3（到岸成本 reverse posted=false+告警）测试在 CHECKING 模式绿
- [x] 若 G1/G3 测试发现与 owner doc 不符的真实行为缺陷，升级为独立 Fix 计划并记录（不静默改生产代码）—— G2 rollbackDelivery「仅 posted=false 不翻 approveStatus」为 P2-MA2-057 既裁决 watch-only 当前设计行为（注释已标注），非新缺陷，无须升级；G1/G3 未发现与 owner doc 不符的真实缺陷

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04a92ce1cffe9Tv2yb5psbXQHP) — fresh-session 实测复核：(a) 多币种 TestErpPurMultiCurrencyPosting:67-133 + TestErpSalMultiCurrencyReconFx:79-169 断言 amountSource≠amountFunctional + rate≠ONE / (c) STANDARD reverse-revalue StandardCosting:200-238 / (e) rollbackReceive PurReversalListenerReceiveRollback:41-89 / (g) settle recheck SettleThreeWayMatchRecheck:77-124 全部闭合声明精确；G1-G3 残差全部真实（pur3+sal3+inv3 dispatcher 零 Mockito 零 failure 测试 / SalReversalListener 4 rollback 仅 rollbackInvoice 覆盖 + 无 TestSalReversalListener* / dispatchReverseFailureAlert:511 零测试驱动）；(d) 裁决 sound（findSpecificLayers:123-152 闭合 tracked P1-MA2-023；CostAdjustmentService:108-112 无 SPECIFIC 分支已被原审计 reclassify 为 P2-MA4-010 watch-only，非 tracked P1 静默降级）；P1-MA4-021 (a)-(h) 全子项无遗漏；规则 14 一 plan 一结果表面正确（同 finding/owner-doc cluster）。G3 失败诱导澄清已采纳（无凭证 reverse 为幂等 no-op，须 CLOSED_FINAL 期间或等效）。无阻塞项。草案审查收敛，转 active。

## Closure Gates

> 纯测试新增，无生产代码/ORM/view.xml 变更。完整仓库验证在此处一次。

- [x] 范围内行为完成（G1 + G2 + G3 残差测试方法落地并 CHECKING 绿）
- [x] 相关文档对齐（G1-G3 未发现与 owner doc 不符的真实缺陷，无须更新；G2 rollbackDelivery 不对称为 P2-MA2-057 既裁决 watch-only，已在测试注释标注）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿（154 模块 BUILD SUCCESS） + `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-inventory/erp-inv-service` 全绿（purchase 135 / sales 147 / inventory 含 14 新增测试，0 失败 0 错误）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中（checker 经 `-type d -name test -prune` 仅扫生产代码，本计划纯测试新增无生产代码变更）
- [x] 无范围内项目降级为 deferred/follow-up（G4 CostAdjustmentService SPECIFIC 分支为计划前既裁决 successor，非本次降级；pur/sal 6 dispatcher 告警 dispatch 为 R1.16 既裁决 watch-only successor，非本次降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CostAdjustmentService SPECIFIC 分支（G4 裁决，独立未跟踪代码质量项）

- Classification: `out-of-scope improvement`（R1.12 Non-Goals :44 显式排除 CostAdjustmentService 重构）
- Why Not Blocking Closure: P1-MA4-021 (d) 的 tracked P1-MA2-023 测试有效性已由 R1.12 闭合（SpecificCostingStrategy.findSpecificLayers 历史成本守卫测试落地）；CostAdjustmentService.applyLine 无 SPECIFIC 分支为独立代码质量项非 P1-MA4-021 tracked 范围，须先代码修复加 SPECIFIC 分支（触及成本保护区域须 owner doc+人工批准）再补测试，非测试有效性范围
- Successor Required: `yes`（触发 = 人工批准成本保护区域 owner doc 后，独立 CostAdjustmentService SPECIFIC 分支实现+测试 plan）

### pur/sal 6 dispatcher 告警 dispatch（R1.16 未覆盖的代码侧告警）

- Classification: `watch-only residual`（沿用 R1.16 裁决边界——pur/sal 6 dispatcher 仍纯 LOG 吞咽无 IErpSysNotificationBiz 告警）
- Why Not Blocking Closure: 本计划为测试有效性（R2.14）；告警代码侧补齐属代码修复非测试；G1 仅测 posted=false 状态可观测（悬挂对测试可见即达测试有效性目标），告警闭环 successor 由 G1 测试证据触发独立代码修复 plan
- Successor Required: `yes`（触发 = G1 测试落地后确认 posted=false 悬挂且无告警 → 独立告警补齐 plan）

## Closure

Status Note: G1+G2+G3 三类残差闭合。新增 5 个测试类 14 个测试方法（pur 3 + sal 6 + inv 5），pur/sal/inv 9 dispatcher tryPost 失败悬挂 + SalReversalListener 3 rollback 路径对称 + 到岸成本 reverse 失败悬挂+告警对测试可观测。纯测试新增，无生产代码/ORM/view.xml 变更。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 三模块 `mvn test` 全绿（purchase 135 / sales 147 / inventory，含 14 新增测试）+ compliance checker 零新增命中。G2 rollbackDelivery「仅 posted=false 不翻 approveStatus」确认为 P2-MA2-057 既裁决 watch-only 当前设计行为，非新缺陷。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（新会话 ses_04a2fa1acffeOgnGD8vpgu615b，general agent）
- Evidence: 独立审计逐项核实——(1) 5 个测试文件均为纯测试无生产代码改动；(2) G1 9 dispatcher 故障诱导+悬挂断言正确；(3) G2 rollback 路径与 SalReversalListener.java 生产行为一致（Receipt/Return→REJECTED，Delivery 仅 posted=false 配 P2-MA2-057 注释）；(4) G3 与 ErpInvLandedCostProcessor.doReverseApprove 一致（reverse 失败→posted=false + 告警事件 `inv.landed-cost-reverse-failure`）；(5) `git status` 仅计划 .md + 5 测试文件变更，零生产 Java/ORM/view.xml 触碰；(6) G4 + pur/sal 6 dispatcher 告警 successor 维持既裁决未降级。审计初轮标记 4 处文档占位符（Plan Status / Status Note / 证据 / Follow-up），已全部补齐后收敛 PASS。

Follow-up:

- 仅 §Deferred But Adjudicated 已裁决的非阻塞 successor（非本次范围、非缺陷降级）：
  - G4 CostAdjustmentService SPECIFIC 分支（须先人工批准成本保护区域 owner doc 后独立实现+测试 plan）
  - pur/sal 6 dispatcher 告警 dispatch（R1.16 watch-only；G1 测试已证 posted=false 悬挂，告警代码侧补齐触发独立 plan）
