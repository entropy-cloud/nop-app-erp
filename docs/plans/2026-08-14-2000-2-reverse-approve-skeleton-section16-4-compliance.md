# 2026-08-14-2000-2-reverse-approve-skeleton-section16-4-compliance 反审核共享骨架 §16.4 合规化修复（跨域 confirmed live defect）

> Plan Status: active
> Last Reviewed: 2026-08-14
> Source: 5+ 状态机迁移计划 Deferred successor——`2026-08-13-0945-1`（M3 采购审批）、`2026-08-13-1950-1`（M4 采购审批）、`2026-08-13-1950-2`（M4 销售审批）、`2026-08-14-0930-1`（M4 制造）、`2026-08-14-0930-2`（M4 质量）均登记 `AbstractReverseApproveProcessor.doReverseApprove` 返回 SUBMITTED 违反 `domain-design-guidelines.md §16.4`（应 REJECTED），Classification = `confirmed live defect moved to explicit successor ownership`，触发条件 = 本计划开启。本计划即 5+ 计划的共有 successor。
> Related: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约）、`docs/design/domain-design-guidelines.md §16.4`（反审核目标态权威规则）、`docs/architecture/processor-extension-pattern.md`（共享骨架范式）
> Mission: entity-state-machine
> Work Item: reverseApprove-§16.4-compliance-fix
> Audit: required
>
> **治理声明**：本计划修复 `module-common-service` 共享骨架的确认缺陷。缺陷在源码层**真实存在**（`doReverseApprove:39` 写 SUBMITTED 违反 §16.4），但在运行时**惰性**（inert）——全部 25 个域子类覆写该路径为 REJECTED 或抛不可逆异常，无生产调用路径可达骨架默认 body。修复触及跨域共享模块（`module-common-service`），须盘点全部使用者、确认零行为回归。按规则 13，已确认的实时缺陷不可降级为非阻塞 Follow-up——本计划为其独立 Fix plan。

## Current Baseline

> 零信任实仓核实（独立子代理 `ses_002fd1dcdffedaeQVyhkKp8fkk` 全量盘点）。骨架 `AbstractReverseApproveProcessor` 位于 `module-common-service`，63 行，plan `2026-07-24-2200-1` Phase 1 落地。

- **缺陷骨架**：`AbstractReverseApproveProcessor`（`module-common-service/src/main/java/app/erp/common/service/AbstractReverseApproveProcessor.java`）：
  - `doReverseApprove`（`:38-42`）：`setApproveStatus(entity, submittedStatus())`——**写 SUBMITTED**（违反 §16.4 应写 REJECTED）。
  - javadoc（`:14`）：显式记录非合规意图"目标状态：回到 SUBMITTED"。
  - `submittedStatus()`（`:62`）：abstract，各子类返回 entity-specific SUBMITTED 常量。
  - `validateTransitionForReverseApprove`（`:31-36`）：守卫源态 = APPROVED。
  - 编排 `reverseApprove`（`:18-29`）：`requireEntity → isRejected short-circuit → validateTransitionForReverseApprove → beforeStateChange → doReverseApprove → afterStateChange → save`。
  - **无 `rejectedStatus()` 抽象方法**——仅 `approvedStatus()`/`submittedStatus()`。

- **§16.4 权威规则**（`docs/design/domain-design-guidelines.md:580-586`）：
  > 反审核（已审核单据撤销审核）的目标态是 `REJECTED`（可重新提交），**不是 `UNSUBMITTED`**（初始态）。语义理由：反审核的单据已发生过业务，不应回退为"未提交"；`REJECTED` 保留"曾审核过"的历史语义；从 `REJECTED` 可重新 `SUBMITTED`。API 契约表 `:635`：`reverseApprove | APPROVED → REJECTED（见 §16.4）`。

- **25 个域子类全量盘点——无生产调用路径可达骨架默认 body**：
  - **Category A（4 entity，骨架编排 + 覆写 `doReverseApprove` → Bean REJECTED）**：`ErpSalOrder`（`:102-106`）、`ErpSalQuotation`（`:95-99`）、`ErpPurOrder`（`:102-106`）、`ErpPurRequisition`（`:94-98`）。覆写 `doReverseApprove` 委托 `stateMachine.reverseApproveTargetStatus()` = REJECTED。
  - **Category B（19 entity，Pattern B 覆写公共 `reverseApprove` → REJECTED）**：`ErpInvCostAdjust`、`ErpInvLandedCost`、`ErpMfgSubcontractOrder`、`ErpMfgWorkOrder`、`ErpSalDelivery`、`ErpSalReceipt`、`ErpSalInvoice`、`ErpSalReturn`、`ErpAstValueAdjustment`、`ErpAstAssetCapitalization`、`ErpAstDisposal`、`ErpQaRecall`、`ErpFinExpenseClaim`、`ErpFinBadDebt`、`ErpFinEmployeeAdvance`、`ErpPurPayment`、`ErpPurReturn`、`ErpPurReceive`、`ErpPurInvoice`。全部覆写公共方法绕过骨架 `doReverseApprove`，直接/经 facade/经 Bean 写 REJECTED。
  - **Category C（2 entity，不可逆——覆写抛异常）**：`ErpAstSplit`（`:22-26` 抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`）、`ErpAstMerge`（`:22-26` 抛 `ERR_AST_MERGE_REVERSE_NOT_SUPPORTED`）。不可逆设计（owner doc `split-merge.md §关键业务规则 5`）。
  - **INLINE xbiz 非骨架（4 entity，非 extends 骨架）**：`ErpPurQuotation`、`ErpPurRfq`、`ErpSalContract`、`ErpAstMovement`——经 xbiz `prepareReverseApprove` + BizModel/Bean 写 REJECTED，不受本修复影响。

- **16 个 ApprovalStateMachine Bean 全部编码 REJECTED**：每个 Bean 的 `reverseApproveTargetStatus()` 返回 REJECTED（purchase 8 + sales 6 + quality 1 + assets 1）。`transitions()` 元数据边均为 APPROVED→REJECTED。

- **测试基线**：无测试断言 SUBMITTED 为 reverseApprove 目标态。全部断言 REJECTED。关键守卫测试含显式注释"reverseApprove 目标态应为 REJECTED（非 SUBMITTED）"——`TestErpPurQuotationRfqReverseApprove:64,98`、`TestErpSalContractReverseApprove:63`、`TestErpAstMovementReverseApprove:63`、`TestErpFinBadDebtReversal:107,170` 等。

- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。

## Goals

- 修复 `AbstractReverseApproveProcessor.doReverseApprove` 使其写 REJECTED 而非 SUBMITTED，实现 §16.4 源码层合规。
- 确保全部 25 个域子类在骨架修复后零行为回归（它们均已覆写该路径为 REJECTED 或抛异常）。
- 更新骨架 javadoc 以反映 §16.4 合规目标态。
- 全量回归验证：跨域集成测试 + 矩阵测试零行为变化。

## Non-Goals

- 不改变任何子类的覆写行为（零行为回归——它们已覆写为 REJECTED）。
- 不改变 reverseApprove 的编排顺序（`requireEntity → isRejected short-circuit → validate → before/do/after → save`）。
- 不改变错误码、权限、审计字段清空逻辑（`approvedBy`/`approvedAt` 清空保留）。
- 不改变 `validateTransitionForReverseApprove` 的 APPROVED 源态守卫。
- 不修改 INLINE xbiz reverseApprove（4 entity 不 extends 骨架）。
- 不修改任何 `model/*.orm.xml` / 字典 yaml。
- 不引入新的 reverseApprove 行为或状态边。

## Task Route

- Type: `bug investigation`（confirmed live defect Fix——骨架源码层违反 §16.4，虽运行时惰性但须修复以确保源码正确性 + 防止未来子类继承错误行为）
- Owner Docs: `docs/design/domain-design-guidelines.md §16.4`（反审核目标态权威规则）、`docs/architecture/processor-extension-pattern.md`（共享骨架范式）、`docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + Bean 编码一致性）
- Skill Selection Basis: 跨域共享骨架修复须匹配 `nop-backend-dev`（`@Inject` 非 private、骨架 abstract 方法约定、产品化可定制性自检）+ `nop-testing`（跨域回归）。`nop-debugging` 匹配 confirmed defect 修复（根因已定位，零信任验证路径）。`state-machine-business-review-prompt.md` 匹配子类矩阵全量盘点验证。

## Infrastructure And Config Prereqs

- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。
- 修复不触及业财过账/数据删除/模型保护区——纯共享骨架源码层合规化（运行时惰性，零行为变化）。

## Execution Plan

### Phase 1 - 骨架修复（AbstractReverseApproveProcessor.doReverseApprove §16.4 合规化）

Status: planned
Targets: `module-common-service/src/main/java/app/erp/common/service/AbstractReverseApproveProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（独立缺陷修复，不依赖任何迁移计划）

- [ ] `Decision`（骨架修复方案裁决）：骨架需将 `doReverseApprove:39` 从 `setApproveStatus(entity, submittedStatus())` 改为写 REJECTED。**方案选择**：(A) 新增 `protected abstract String rejectedStatus()` 并在 25 子类实现——对齐现有 `submittedStatus()`/`approvedStatus()` abstract 模式，编译时强制，但 25 子类 churn 大（每子类一行 return `*_REJECTED`）；(B) 新增 `protected String rejectedStatus()` 带 default 实现 `return "REJECTED"`——因 `wf/approve-status` 是平台标准 dict（REJECTED 值全局一致），default 可覆盖全部子类，仅非标准实体需覆写，churn 最小。**裁定：方案 (B)**——理由：全部 25 子类使用 `wf/approve-status` 标准 dict（REJECTED="REJECTED"），default 覆盖无遗漏；运行时惰性（无子类依赖骨架默认 body）；churn 最小化降低跨域回归风险。Category A（4 entity 覆写 `doReverseApprove` → Bean）不受影响（其覆写不调 `rejectedStatus()`）；Category B（19 entity 覆写公共方法）不受影响；Category C（2 entity 抛异常）不受影响。若未来有实体使用非标准 REJECTED 值，覆写 default 即可。考虑的替代方案：方案 A（全 abstract）——拒绝理由：25 子类 churn 不成比例于惰性方法修复收益。
  - Skill: `nop-backend-dev`
- [ ] `Fix`：`AbstractReverseApproveProcessor`——(a) `doReverseApprove:39` 改为 `setApproveStatus(entity, rejectedStatus())`；(b) 新增 `protected String rejectedStatus() { return "REJECTED"; }`（default 实现，方案 B）；(c) javadoc `:14` 更新为"目标状态：REJECTED（§16.4 反审核目标态），清空 approvedBy/approvedAt 审计字段"。`approvedBy`/`approvedAt` 清空逻辑（`:40-41`）保留不变。`validateTransitionForReverseApprove`（`:31-36`）APPROVED 源态守卫保留不变。编排 `reverseApprove`（`:18-29`）顺序保留不变。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `AbstractReverseApproveProcessor.doReverseApprove` 写 REJECTED；`rejectedStatus()` default 方法存在；javadoc 更新。`module-common-service` 本地 `mvn compile -pl module-common-service -am` 通过。

### Phase 2 - 跨域子类全量盘点验证 + 回归

Status: planned
Targets: `module-common-service`（骨架）+ 全部 25 子类所在模块（purchase/sales/inventory/manufacturing/finance/assets/quality）
Skill: `nop-testing` + `nop-debugging`

- Item Types: `Proof`
- Prereqs: Phase 1（骨架修复已落地）

- [ ] `Proof`（子类覆写完整性验证）：确认全部 25 子类在骨架修复后零行为变化——(a) Category A 4 entity 覆写 `doReverseApprove` → Bean REJECTED（不受影响）；(b) Category B 19 entity 覆写公共 `reverseApprove`（不受影响）；(c) Category C 2 entity 抛异常（不受影响）。逐类验证：grep 确认 `submittedStatus()` 在骨架内仅被 `doReverseApprove:39` 引用（修复后变为 dead code），保留为 abstract 以最小化跨域 churn（避免移除 25 子类的既有实现）；本次仅改 `doReverseApprove` 调用点。编译全部 7 个域模块通过。
  - Skill: `nop-debugging`
- [ ] `Proof`（跨域集成测试回归）：运行 reverseApprove 相关测试全绿——(a) purchase `TestErpPurQuotationRfqReverseApprove`（显式 REJECTED 守卫注释 `:64,98`）+ `TestErpPurReturnRefundEndToEnd` + 8 个 Bean 矩阵测试；(b) sales `TestErpSalContractReverseApprove` + `TestErpSalReturnRefundEndToEnd` + 6 个 Bean 矩阵测试；(c) assets `TestErpAstMovementReverseApprove` + `TestErpAstPostingReverse` + 1 Bean 矩阵测试；(d) finance `TestErpFinBadDebtReversal` + `TestErpFinExpenseClaimApproval`；(e) quality `TestErpQaRecallApprovalStateMachineMatrix`。本地 `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-assets/erp-ast-service,module-finance/erp-fin-service,module-quality/erp-qa-service -am` 全绿。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 全部 25 子类零行为变化（覆写完整性确认）；跨域集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_002f74c93ffeaI96Il8nKMxIk9`) — 零信任实仓核实全部 11 项 baseline 声明全 pass（骨架 defect :39 写 submittedStatus + javadoc :14 + §16.4 :580-586 + Category A/B/C 全量 25 子类覆写验证 + 16 Bean 编码 REJECTED + 测试断言 REJECTED + Decision (B) soundness：全部 9 域 `*DocStatus` 接口 `APPROVE_STATUS_REJECTED="REJECTED"` 无非标准值 + 5+ 计划 Deferred successor 引用确认）。1 MINOR 已修正：`submittedStatus()` 在骨架内仅被 `doReverseApprove:39` 引用，修复后为 dead code——Phase 2 Proof 措辞从"其他方法可能引用"订正为"dead code，保留 abstract 以最小化跨域 churn"。无 BLOCKER / MAJOR。草案审查收敛。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [ ] 范围内行为完成（骨架 §16.4 合规化 + 25 子类零行为回归验证）
- [ ] 相关文档对齐（§16.4 骨架合规确认；5+ successor 计划的 Deferred 项可关闭引用）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + 跨域 `mvn test` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Category A 子类覆写 `doReverseApprove` 委托 Bean（非骨架 default）

- Classification: `watch-only residual (intentional legacy Pattern A)`
- Why Not Blocking Closure: 4 entity（SalOrder/SalQuotation/PurOrder/PurRequisition）覆写 `doReverseApprove` 委托 Bean——骨架 `rejectedStatus()` default 对其无影响（不调用）。统一为骨架 default 须移除覆写 + 改用骨架编排，属架构收敛非缺陷修复。
- Successor Required: no（仅当全部审批 Bean 统一经骨架编排时重评）

### 5+ 迁移计划的 Deferred successor 引用关闭

- Classification: `documentation cleanup (successor closeout)`
- Why Not Blocking Closure: 5+ 计划（`2026-08-13-0945-1`、`2026-08-13-1950-1`、`2026-08-13-1950-2`、`2026-08-14-0930-1`、`2026-08-14-0930-2`）的 Deferred But Adjudicated 均登记"reverseApprove 共享骨架 §16.4 合规化 → Successor Required: yes（触发条件 = 独立 plan）"。本计划即该 successor——完成后各计划该 Deferred 项的 successor 引用可标注为已由本计划关闭（不在各已完成计划中追溯修改文本，仅在本计划 Closure 中记录覆盖关系）。
- Successor Required: no（本计划关闭即完成）

## Closure

Status Note: _待执行后填写_

Closure Audit Evidence:

- Auditor / Agent: _待执行后填写_
- Evidence: _待执行后填写_

Follow-up:

- <待执行后填写；Deferred 项均为既定 successor>
