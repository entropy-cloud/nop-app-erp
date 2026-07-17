# 2026-07-17-1430-2-test-hardening-anchor-assertions 核心测试三层验证加固

> Plan Status: active
> Last Reviewed: 2026-07-17
> Source: 技术债识别 — 录制回放测试在重新录制时可能掩码业务回归；锚点断言审计确认断言充分但缺 output 快照覆盖
> Related: `2026-07-15-1022-1-orm-tagset-all-domains.md`（BUSINESS_DATE/VOUCHER_DATE 加 tagSet="clock" 修复）
> Audit: required

## Current Baseline

### 断言覆盖现状

1. 抽样审计 8 个核心业务测试类共 54 个测试方法（finance posting/bad-debt/period-close/budget, purchase order-approval/three-way-match, sales order-approval, inventory stock-move-bookkeeping）—— **100% 已有显式锚点断言**，均通过 `daoProvider.daoFor()` 获取实体状态做 `assertEquals`/`assertNull`/`assertThrows` 断言。第一层（核心业务字段断言）充分。
2. 但这些测试**不使用 `output()` 录制回放**——第二层（响应快照）和第三层（数据库快照）完全缺失。`output()` 的用法仅存在于 `*CrudSmoke` 冒烟测试类。
3. 深入审计 13 个跨域/高复杂度测试类的副作用覆盖发现：断言只检查了开发者明确想到的表和字段，副作用覆盖有显著缺口。

### 副作用覆盖缺口（Top 5）

| 优先级 | 测试类 | 当前断言覆盖表 | 未覆盖的副作用表 | 风险 |
|--------|--------|--------------|----------------|------|
| **P0** | `TestErpPurProcureToPayEnd` | ErpPurReceive, ErpPurInvoice, ErpPurPayment, ErpFinVoucherBillR (count) | ErpInvStockMove, ErpInvStockBalance, ErpInvStockLedger, ErpFinVoucher, ErpFinVoucherLine, ErpFinArApItem | 跨域 P2P 全链，断言只覆盖 ~4/12+ 张表 |
| **P0** | `TestErpFinPeriodCloseEndToEnd` | ErpFinAccountingPeriod (status), ErpFinAccountingPeriodStatus, ErpFinVoucherBillR (count) | ErpFinVoucherLine (科目金额), ErpFinGlBalance (期末余额), ErpFinArApItem (重估影响) | 期末结账是合规关口，当前只检查"存在"不检查"正确" |
| **P1** | `TestErpFinPostingService` | ErpFinVoucher, ErpFinVoucherLine, ErpFinVoucherBillR | ErpFinArApItem（冲销测试未验证应收应付项被取消） | 过账引擎是跨域集线器，AR/AP 项是已知盲区 |
| **P1** | `TestErpAstDepreciation` | ErpAstDepreciationSchedule, ErpAstAsset (NBV), ErpFinVoucherBillR (count) | ErpFinVoucherLine（60+ 张凭证的科目/金额从未检查） | 高数量 × 低断言密度，单笔金额错误不可见 |
| **P1** | `TestErpMfgWorkOrderEndToEnd` | ErpInvStockBalance (产成品), ErpMfgWorkOrder (成本汇总) | ErpInvStockBalance (原材料 M1 扣减从未检查), ErpInvStockLedger, ErpMfgMaterialIssue | 成本流转盲区，材料消耗静默失败不会被发现 |

## Goals

- 为高风险跨域测试补充 `output()` 快照，实现三层验证（显式断言 + 响应快照 + 数据库快照）
- 消除现有测试中的副作用覆盖盲区
- 将审计方法和自检清单固化为可复用模式

## Non-Goals

- 不改动已通过审计的锚点断言
- 不涉及 `*CrudSmoke` 冒烟测试（无业务语义）
- 不涉及 ORM 模型或业务逻辑代码变更
- 不为纯状态机/校验型测试（如 approval-only 测试）加 output

## Task Route

- Type: `implementation-only change`
- Owner Docs: `../nop-entropy/docs-for-ai/02-core-guides/testing.md`（已更新锚点章节）
- Skill Selection Basis: `nop-testing`（需要理解 JunitAutoTestCase 的 output() 模式和三层验证组合写法）

## Infrastructure And Config Prereqs

无。纯测试代码变更。

## Execution Plan

### Phase 1 — 核心业务测试断言覆盖审计（已完成）

Status: completed
Targets: 8 个测试类 / 54 个测试方法（finance/purchase/sales/inventory）
Skill: `nop-testing`

- Item Types: `Proof`

审计结果：

| 域 | 测试类 | 方法数 | 断言覆盖 | 审计结论 |
|---|--------|--------|---------|---------|
| finance | TestErpFinPostingService | 6 | 100% | 充足：借贷平衡/状态/冲销代数/拒绝码 |
| finance | TestErpFinBadDebt | 7 | 100% | 充足：准备计提计算/核销/恢复/释放 |
| finance | TestErpFinPeriodCloseEndToEnd | 1 | 100% | 充足：期间状态变迁全闭环 |
| finance | TestErpFinBudgetEndToEnd | 6 | 100% | 充足：预算控制模式/超预算拒绝/注销回冲 |
| purchase | TestErpPurOrderApproval | 5 | 100% | 充足：审批流状态机/拒绝码 |
| purchase | TestErpPurThreeWayMatch | 6 | 100% | 充足：三单匹配拒绝/放行边界 |
| sales | TestErpSalOrderApproval | 14 | 100% | 充足：信用额度控制多币种/特殊审批/已交付解锁 |
| inventory | TestErpInvStockMoveBookkeeping | 9 | 100% | 充足：数量/成本/可用量/负库存边界 |

结论：第一层（显式断言）充分，但第二三层（output 快照）完全缺失。

- [x] 抽样审计 8 个测试类并记录
- [x] 深入审计 13 个测试类的副作用覆盖并输出缺口分析

### Phase 2 — 为 Top 5 高价值测试补充 `output()` 快照

Status: planned
Targets:
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurProcureToPayEnd.java`
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinPeriodCloseEndToEnd.java`
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingService.java`
- `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstDepreciation.java`
- `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderEndToEnd.java`

Skill: `nop-testing`

- Item Types: `Add`

实施策略：

每个测试方法在执行业务操作后，在关键分支点加一行 `output("response.json5", result)` 或 `outputText(...)`。注意：
- 如果方法当前通过 `executeRpc(...)` 调用 API，结果可直接传给 `output()`。
- 如果方法通过 `ormTemplate.runInSession(...)` 直接调用 service/processor，可能没有 ApiResponse 可传。此时在类级别加 `@EnableSnapshot` 或在方法上加 `@EnableSnapshot(saveOutput = true)`，框架自动录制提交时的数据库快照到 `output/tables/*.csv`。
- 需要确认每个方法在加 output 后能通过录制，并在 CHECKING 模式下验证不破坏现有断言。

#### `TestErpPurProcureToPayEnd` — 3 个测试方法

- `testProcureToPayPartialSettlement`：P2P 全链，在最终 verify 阶段加 `output()` 录制 DB 快照
- `testReverseScenarios`：冲销场景，冲销完成后加 `output()`
- `testFinanceReconciliationLayerPayable`：对账过账后加 `output()`

#### `TestErpFinPeriodCloseEndToEnd` — 1 个测试方法

- `testFullChain`：5 个子步骤（preCheck→close→finalize→reverseClose→re-close），在每个关键步骤后或最终状态加 `output()`

#### `TestErpFinPostingService` — 4 个测试方法

- `testPostHappyPath`：过账后在断言行之后加 `output()`
- `testReverse`：冲销完成后加 `output()`
- `testPostIdempotent`、`testPostUnbalancedRejected`、`testPostPeriodClosedRejected`：加 `output()` 录制异常路径 DB 状态

#### `TestErpAstDepreciation` — 3 个方法

- `testStraightLinePerPeriodEqualAndLastToResidual`、`testDoubleDecliningResidualConstraint`、`testBatchDepreciationProcessesAllAssets`：折旧执行后加 `output()`

#### `TestErpMfgWorkOrderEndToEnd` — 1 个方法

- `testEndToEndIssueReportCompletion`：工单全生命周期完成后加 `output()`

- [ ] 逐一修改 Top 5 测试类，为所有涉及多表副作用的方法补充 `output()` 调用
      - Skill: `nop-testing`
- [ ] `mvn clean install -DskipTests` 构建通过
- [ ] 为每个修改后的测试类在 RECORDING 模式下生成 output 快照
- [ ] 切回 CHECKING 模式，确认 `mvn test -pl` 各模块全绿

Exit Criteria（对应计划第 99-121 行列出的具体方法）：

- [ ] TestErpPurProcureToPayEnd：3 个方法（testProcureToPayPartialSettlement / testReverseScenarios / testFinanceReconciliationLayerPayable）已补 output
- [ ] TestErpFinPeriodCloseEndToEnd：testFullChain 已补 output
- [ ] TestErpFinPostingService：4 个方法（testPostHappyPath / testReverse / testPostIdempotent / testPostUnbalancedRejected / testPostPeriodClosedRejected）已补 output
- [ ] TestErpAstDepreciation：3 个方法（testStraightLine / testDoubleDeclining / testBatchDepreciation）已补 output
- [ ] TestErpMfgWorkOrderEndToEnd：testEndToEndIssueReportCompletion 已补 output
- [ ] 快照录制完成，CHECKING 模式下各模块 `mvn test` 全绿
- [ ] 没有破坏任何现有锚点断言

### Phase 3 — 扩展到其他有跨表副作用的业务测试

Status: planned
Targets: 根据副作用缺口分析，对剩余有 medium 收益的测试类补 output
Skill: `nop-testing`

- Item Types: `Add`

候选：

| 域 | 测试类 | 优先级 | 理由 |
|---|--------|--------|------|
| finance | TestErpFinBadDebt | MEDIUM | 凭证头/BillR 未检查 |
| finance | TestErpFinArApItemGeneration | MEDIUM | 凭证侧的 AR/AP 生成未检查 |
| finance | TestErpFinReversalDispatch | MEDIUM | 红字凭证的 DB 侧未检查 |
| inventory | TestErpInvStockMoveBookkeeping | MEDIUM | 已充分覆盖，但 ErpInvStockMoveLine/ErpInvReservation 未检查 |

- [ ] 逐个为候选测试类补 `output()` 并录制/验证
- [ ] `mvn test` 全绿

Exit Criteria:

- [ ] Medium 优先级测试类已审计补完，或评估决定跳过并有理由记录

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0919e8418ffewxIKEe1xSruamT`) because baseline was inaccurate — sampled core business tests already have 100% anchor assertion coverage and don't use `output()` snapshot recording.
- Independent draft review iteration 2: acceptable as-is (`ses_0919bc16fffemLNIwWSxzS5Avj`) after revision moved to audit-first approach.
- Independent draft review iteration 3: acceptable as-is (`ses_09193a741ffeTv3imTVx4VhLpX`). Baseline accuracy confirmed via file inspection; non-RPC execution mode handled by `@EnableSnapshot`; two non-blocking polish items fixed.

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs-for-ai/02-core-guides/testing.md` 的三层验证章节已更新）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 各模块全绿
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 状态机/校验型测试不补 output

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `TestErpPurOrderApproval`、`TestErpSalOrderApproval`、`TestErpPurThreeWayMatch` 等纯状态机测试无跨表副作用，output 收益低。

## Closure

（待完成时填写）
