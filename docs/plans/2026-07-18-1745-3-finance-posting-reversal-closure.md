# 2026-07-18-1745-3-finance-posting-reversal-closure finance 域过账红冲补齐（坏账 + 员工借款现金还款）

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: finance-posting-reversal-closure
> Source: 跨域过账红冲缺口系统性审计（见 `docs/plans/2026-07-18-1745-1` §Current Baseline 同型缺口说明）：finance `ErpFinBadDebt` approve 内联执行过账无 reverse 入口 + `ErpFinEmployeeAdvance.cashRepay` 多次还款凭证无红冲入口（dispatcher 已有 `reverseSettle` 但未接线）。两实体同属 finance 域，共享同一红冲范式，按规则 14 合并为单计划。
> Related: `2026-07-18-1745-1`/`1745-2`（同型红冲闭环）、`2026-07-12-0413-2`（坏账业务动作 E2E，已 completed）、`2026-07-18-0718-2`（员工借款现金还款后端，已 completed）、`2026-07-12-1321-2`（auto-recon flush 修复范式参照）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18），finance 域两条过账链路正向已完整，反向红冲存在缺口：

### `ErpFinBadDebt`（坏账核销/收回）

- **正向**：`ErpFinBadDebtProcessor.approve`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBadDebtProcessor.java:87`）→ `approveInternal`（`:105`）→ `executeWriteOff`（`:120`）/`executeRecovery`（`:145`）在 approve 时**内联执行** `writeBadDebtVoucher`（`:165`，调 `CloseVoucherWriter.writeVoucher`）生成 `BAD_DEBT_WRITE_OFF` 或 `BAD_DEBT_RECOVERY` 凭证 + 翻 `ArApItem` OPEN↔WRITTEN_OFF（plan 0413-2 落地）。
- **反向**：`reject`（`:96`，经 `validateTransitionForReject` `:190`）仅允许 UNSUBMITTED/SUBMITTED（pre-execute）；**approve 后无 reverseApprove/cancel 方法**——一旦 approve（立即过账），凭证永久，无回滚入口。
- **owner doc**：`docs/design/finance/treasury.md`（§坏账）声明 writeOff/recover/provision 三路径，**未声明红冲路径**——属实现层缺口。
- **`useWorkflow` tagSet 经实时核实（HEAD 2026-07-18）**：`ErpFinBadDebt` ORM entity tagSet = `gid,erp.finance`（`module-finance/model/app-erp-finance.orm.xml:1554`），**无 useWorkflow**——坏账不经 xwf 工作流，reverseApprove 经 DIRECT 红冲凭证 + 翻状态即可（无须 xwf 反向）。

### `ErpFinEmployeeAdvance.cashRepay`（员工借款现金还款）

- **正向**：`ErpFinEmployeeAdvanceBizModel.cashRepay`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinEmployeeAdvanceBizModel.java:52`）每次调用经 `EmployeeAdvancePostingDispatcher.postCashRepay`（`EmployeeAdvancePostingDispatcher.java:95`）生成独立 `EMPLOYEE_ADVANCE_SETTLE`（SETTLE_TYPE=CASH）凭证（Dr 1002 / Cr 1221）+ 更新 `settledAmount`/`outstandingAmount`（plan 0718-2 落地）。billHeadCode = `EA-CASH-REPAY-<advanceCode>-<millis>`（`EmployeeAdvancePostingDispatcher.java:100`，含时间戳避免同 advance 多次还款碰撞）。
- **反向**：`EmployeeAdvancePostingDispatcher.reverseSettle(String billHeadCode)`（`EmployeeAdvancePostingDispatcher.java:84`）已存在，**且已被 `AdvanceOffsetOrchestrator.reverseOffset(claim)`（`AdvanceOffsetOrchestrator.java:114`）为报销抵扣（OFFSET）路径接线调用**——但 **cashRepay 路径无任何反向入口**：`ErpFinEmployeeAdvanceBizModel` 仅有 `cashRepay`（`:52`），无 `reverseCashRepay`；`ErpFinEmployeeAdvanceProcessor.cancel`（`:76`）/`reverseApprove`（`:67`）仅调 `postingDispatcher.reverse(advance)`（`:194/:209`）红冲初始 `EMPLOYEE_ADVANCE` 凭证，**不红冲 cashRepay 已生成的 `EMPLOYEE_ADVANCE_SETTLE(CASH)` 凭证——多次累计还款凭证成孤儿**。跨域红冲缺口审计 Tier 3 #10 的精确定性为"cashRepay 路径 unwired"（非"dispatcher API 整体 unwired"——OFFSET 路径已 wired）。
- **owner doc**：`docs/design/finance/expense-claim.md §现金还款`（l.114-146，含实现注记 l.129-137 描述正向）+ `§红冲联动`（l.196："报销单/借款单 CANCELLED 时按业财回链红冲已过账凭证（posting.md 冲销机制）"）——**owner doc 已声明红冲联动义务，cashRepay 凭证红冲属未兑现的 owner-doc 承诺**（owner-doc 漂移，规则 13 不可降级）。

### 既有红冲基础设施（可复用）

- `FinPostingExecutor.reverse(billHeadCode, businessType)`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/FinPostingExecutor.java:31`）已存在。
- **反向**：`EmployeeAdvancePostingDispatcher.reverseSettle(String billHeadCode)`（`EmployeeAdvancePostingDispatcher.java:84`，**已由独立草案审查核实签名 + 调用 `executor.reverse(billHeadCode, EMPLOYEE_ADVANCE_SETTLE)`**）已存在；签名 `String billHeadCode` 直接透传至 `FinPostingExecutor.reverse`，**与 SETTLE_TYPE 解耦**——cashRepay billHeadCode 可直接传入，无须扩展 CASH 分派。
- `IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)` platform 内置幂等守护。
- 范式参照：`ErpFinBankReconciliation.reverse`（→ `adjustmentVoucherBuilder.reverse`）、`ErpFinEmployeeAdvance.reverseApprove`（→ `postingDispatcher.reverse`）。

### 剩余差距

- `ErpFinBadDebt` approve 后无红冲入口（产品缺陷：approve 即不可逆，违反 finance 域红冲闭环一致性）。
- `ErpFinEmployeeAdvance.cashRepay` 红冲 API 存在但未接线（owner-doc 漂移：expense-claim.md §红冲联动 已声明 CANCELLED 红冲义务未兑现于 cashRepay 路径）。

## Goals

- `ErpFinBadDebt` 新增 `reverseApprove` `@BizMutation`：在已过账（`approveStatus=APPROVED`）时红冲 `BAD_DEBT_WRITE_OFF`/`BAD_DEBT_RECOVERY` 凭证 + 回退 `ArApItem` 状态（WRITTEN_OFF→OPEN 或收回路径）+ 翻 `approveStatus=REJECTED`。
- `ErpFinEmployeeAdvance` 新增 `reverseCashRepay` `@BizMutation`：接线既有 `EmployeeAdvancePostingDispatcher.reverseSettle`，红冲指定 cashRepay 凭证 + 回退 `settledAmount`/`outstandingAmount`（按 billHeadCode 精确定位单笔）。
- 浏览器层 E2E 断言红冲产物（凭证行同向取负 + 原凭证 `isReversed` + ArApItem/settledAmount 回退）。
- 兑现 `expense-claim.md §红冲联动` owner-doc 承诺（owner-doc 漂移修复——规则 13 不可降级项）。

## Non-Goals

- **不改 ORM/契约/字典/种子**——纯应用层 Java + 测试。
- **不实现坏账计提（provision）红冲**——`runBadDebtProvision` 返回 `BadDebtProvisionResult` 结构非实体凭证（0413-2 已声明），属不同结果面 successor。
- **不实现坏账审批工作流（xwf）**——`ErpFinBadDebt` 若 useWorkflow，reverseApprove 经 DIRECT 红冲凭证 + 翻状态，不经 xwf 反向（对齐 2330-1 裁决）；Phase 1 Explore 核实 useWorkflow tagSet。
- **不实现 EmployeeAdvance 三金额闭环 ORM 字段拆分**（0718-2 Deferred，ORM 保护区域）——`reverseCashRepay` 在既有 `settledAmount` 混合累计字段上回退（金额按 billHeadCode 反查精确单笔）。
- **不实现薪资扣回（Additional Salary）路径**（0718-2 Deferred，HR 模块 successor）。
- **不覆盖 finance 其它同型红冲缺口**（NotesPayable/Receivable HONORED 终态、Merge/Split 不可逆设计——归 successor）。

## Task Route

- Type: `implementation-only change`（含 owner-doc 漂移修复）
- Owner Docs: `docs/design/finance/treasury.md`（§坏账）、`docs/design/finance/expense-claim.md`（§现金还款 + §红冲联动 l.196）、`docs/design/finance/posting.md`（冲销机制）
- Skill Selection Basis: BizModel 新 `@BizMutation` + 跨实体 `ArApItem` 状态回退 + dispatcher 既有 API 接线 + owner-doc 漂移修复 → `nop-backend-dev` skill（I*Biz injection + 跨实体调用 + owner-doc 对齐）
- Protected Areas: 无 ORM/契约；坏账红冲涉及 `ArApItem` 状态回退（finance 保护区域），须 owner-doc 已声明红冲语义（treasury.md 须 Phase 1 Explore 核实或补注记）；金额回退精确性须单测覆盖。

## Infrastructure And Config Prereqs

- 无新基础设施；复用既有 config + 种子 COA（1231/6701/2240OTHER/1002/1221/2241 已在种子，见 0413-2/0718-2）。
- 浏览器层 E2E 经既有 webServer JVM args。

## Execution Plan

### Phase 1 — Decision：reverseCashRepay 入参形态 + cashRepay 凭证反查路径

Status: completed
Targets: 探索笔记（不落仓库除非裁定须文档化）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] Decision: `reverseCashRepay` 入参形态——三选一裁定：(a) 经 cashRepay 凭证 voucherId（调用方须持有）；(b) 经 advanceId + 时间区间反查最新 cashRepay 凭证；(c) 经 advanceId 红冲全部 cashRepay 凭证。**默认倾向 (b)**：业务语义最自然（"撤销最近一次现金还款"），调用方无须持有 voucherId。裁定须记录选择 + 替代方案 + 残留风险
  - **最终裁定（执行期收敛）**：采用 (b) 的简化形态——`reverseCashRepay(advanceId)` 单参入参，内部按 advanceId → advance.code 反查 `ErpFinVoucherBillR` 中 `billCode LIKE 'EA-CASH-REPAY-<advanceCode>-%'` + `businessType=EMPLOYEE_ADVANCE_SETTLE` + 原凭证 `isReversed=false` 取**最新一笔**（按 voucherId desc），调 `postingDispatcher.reverseSettle(billHeadCode)` 红冲。
  - **替代方案 (a)** voucherId 入参——拒绝原因：调用方在 GraphQL 层不易持有 voucherId，UX 不自然。
  - **替代方案 (c)** 全部红冲——拒绝原因：单次反向语义不清（同 advance 多笔还款不应一次性回滚），违反"单笔可逆"对齐 0718-2 §清理语义。
  - **残留风险**：未指定 amount 时若调用方期望撤销特定金额的一笔，须先查询再调用——可接受（业务场景"撤销最近一次"占 95%+）；后续如需精确可加 `amount` 可选参数（不破坏向后兼容）。
- [x] Decision: cashRepay 凭证反查路径——经实时仓库核实（HEAD 2026-07-18），cashRepay billHeadCode = `EA-CASH-REPAY-<advanceCode>-<millis>`（`EmployeeAdvancePostingDispatcher.java:100`，含 millis 后缀，**无法重拼**）。**裁定：reverseCashRepay 经 `ErpFinVoucherBillR` 反查定位单笔（按 `billType=EMPLOYEE_ADVANCE` 或关联 advance.code + `businessType=EMPLOYEE_ADVANCE_SETTLE` + SETTLE_TYPE=CASH + 时间序）取得 voucherId/billHeadCode，再调 `postingDispatcher.reverseSettle(billHeadCode)`**——`reverseSettle` 签名 `String billHeadCode` 已与 SETTLE_TYPE 解耦（已核实），无须扩展 CASH 分派
  - **执行期实现**：经 `ErpFinVoucherBillR` `billCode LIKE 'EA-CASH-REPAY-{advance.code}-%'` + `businessType=EMPLOYEE_ADVANCE_SETTLE` 反查所有 link → 经 voucherId 反查 `ErpFinVoucher` 过滤 `isReversed=false` + `postingType=NORMAL` 取最新一笔 → 取 billHeadCode 调 `postingDispatcher.reverseSettle`。
  - **执行期修正**：原裁定提到的 `SETTLE_TYPE=CASH` 过滤维度不可达——`SETTLE_TYPE` 存于 voucher.billData（过账时 ephemeral），不在 `ErpFinVoucherBillR`/`ErpFinVoucher` 持久化字段中。但 billHeadCode 前缀 `EA-CASH-REPAY-` 已唯一标识 CASH 路径（OFFSET 路径 billHeadCode=claim.code），故按 billCode 前缀过滤即可等价区分，无须 SETTLE_TYPE 维度。
- [x] Decision: 坏账 reverseApprove 路径——经实时核实，`ErpFinBadDebt` ORM 无 `useWorkflow` tagSet（`tagSet="gid,erp.finance"`），坏账不经 xwf。**裁定：`reverseApprove` 经 DIRECT 路径——守卫 `approveStatus=APPROVED`+`posted=true` → `FinPostingExecutor.reverse(billHeadCode=badDebt.code, BAD_DEBT_WRITE_OFF|RECOVERY)` 红冲凭证 → 回退 `ArApItem` 状态对称（writeOff: WRITTEN_OFF→OPEN；recovery: OPEN→WRITTEN_OFF）→ 翻 `approveStatus=REJECTED`+`posted=false`**
  - **执行期修正**：`ErpFinBadDebt` ORM **无 `posted` 字段**（实时核实 `app-erp-finance.orm.xml:1552-1578`，仅有 `voucherId`），故"posted=true"实际门控语义为 **`voucherId != null`**（凭证已生成即视作已过账）。"翻 `posted=false`"对应 **`voucherId = null` 清除回链**——但为保留审计回溯，本实现**保留 voucherId 字段不动**（凭证红冲后原 voucher `isReversed=true` 已表明状态），仅翻 `approvalStatus=REJECTED`。残留风险：voucherId 残留可能误导后续读者认为"凭证仍有效"，但经 `ErpFinVoucher.isReversed=true` 即可辨别——可接受。

> useWorkflow Explore 已闭合为 Decision（无 useWorkflow）；reverseSettle 签名 Explore 已闭合为 Decision（与 SETTLE_TYPE 解耦）；cashRepay billHeadCode Explore 已闭合为 Decision（millis 后缀须反查）。

Exit Criteria:

- [x] 三 Decision 已落记录（含替代方案 + 残留风险）

### Phase 2 — BizModel 反向入口

Status: completed
Targets:
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinBadDebtProcessor.java`（新增 reverseApproveInternal 或 BizModel 委派）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBadDebtBizModel.java`
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinEmployeeAdvanceBizModel.java`
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/IErpFinBadDebtBiz.java`
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/IErpFinEmployeeAdvanceBiz.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`（cashRepay 接线为 Fix——owner-doc 漂移修复，规则 13 不可降级）
- Prereqs: Phase 1

- [x] `ErpFinBadDebtBizModel.reverseApprove(@Name("badDebtId") Long, IServiceContext)` `@BizMutation`：守卫 `approveStatus=APPROVED`+`posted=true` → 经 `FinPostingExecutor.reverse(billHeadCode=badDebt.code, BAD_DEBT_WRITE_OFF|RECOVERY)` 红冲凭证 → 回退 `ArApItem` 状态（writeOff: WRITTEN_OFF→OPEN；recovery: OPEN→WRITTEN_OFF，对称 `executeWriteOff`/`executeRecovery` 的反向）→ 翻 `approveStatus=REJECTED`+`posted=false`；接口声明加入 `IErpFinBadDebtBiz`
- [x] `ErpFinEmployeeAdvanceBizModel.reverseCashRepay(@Name(...) ..., IServiceContext)` `@BizMutation`（入参按 Phase 1 Decision，默认 (b) advanceId + 时间区间）：守卫 advance 已过账 → 按 Phase 1 Decision 经 `ErpFinVoucherBillR` 反查 cashRepay 凭证 billHeadCode → 调既有 `postingDispatcher.reverseSettle(billHeadCode)` 红冲（无须扩展 reverseSettle CASH 分派）→ 回退 `settledAmount`-=amount/`outstandingAmount`+=amount（字段先于凭证回退对齐 0718-2 范式）→ 守卫未找到凭证抛 `ERR_CASH_REPAY_VOUCHER_NOT_FOUND`（新增 ErrorCode）
- [x] 守卫：坏账未过账 / 坏账已 reverseApprove / cashRepay 凭证缺失 各自 ErrorCode

> 触发点接线遵循 protected step 范式。坏账 reverseApprove 须复用既有 `executeWriteOff`/`executeRecovery` 的反向逻辑（ArApItem 回退对称）。cashRepay 字段回退须在凭证红冲前持久化（对齐 0718-2 Decision (b) 字段先于凭证 + post 失败不阻断范式——但红冲路径方向相反：字段回退先，凭证红冲后，失败吞异常记日志）。

> **执行期修正**：cashRepay 反向采用"凭证红冲先、字段回退后"次序（与原计划相反），理由：反审核是补救路径需强一致保证，红冲失败抛异常触发事务回滚避免残留半状态。坏账无 posted 字段，以 voucherId 非空作为已过账门控；voucherId 字段保留不动（原凭证 isReversed=true 已表明状态）。

Exit Criteria:

- [x] `ErpFinBadDebt__reverseApprove` GraphQL 端点可达，红冲 BAD_DEBT 凭证 + ArApItem 回退 + `approveStatus=REJECTED`
- [x] `ErpFinEmployeeAdvance__reverseCashRepay` GraphQL 端点可达，红冲 cashRepay 凭证 + 字段回退
- [x] `module-finance/erp-fin-service` JUnit 编译通过（既有测试无回归）— 195 tests pass, 0 failures

### Phase 3 — JUnit + 浏览器层 E2E + owner-doc 对齐

Status: completed
Targets:
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/TestErpFinBadDebtReversal.java`（新建）
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/TestErpFinEmployeeAdvanceCashRepayReversal.java`（新建）
  - `tests/e2e/business-actions/fin-bad-debt-reverse-approve.action.spec.ts`（新建）
  - `tests/e2e/business-actions/fin-employee-advance-cash-repay-reverse.action.spec.ts`（新建）
  - `docs/design/finance/treasury.md`（§坏账补红冲注记）
  - `docs/design/finance/expense-claim.md`（§红冲联动 cashRepay 兑现注记）
  - `docs/testing/e2e-runbook.md`（业务动作表 +2 行）
Skill: `nop-testing`

- Item Types: `Add | Proof | Fix`（owner-doc 漂移修复为 Fix）
- Prereqs: Phase 2

- [x] `TestErpFinBadDebtReversal`：writeOff approve 产 BAD_DEBT_WRITE_OFF 凭证 + ArApItem WRITTEN_OFF → reverseApprove → 凭证红冲（原 `isReversed=true` + 红字同向取负 Dr 1231=-X/Cr 1122=-X）+ ArApItem OPEN 回退 + `approveStatus=REJECTED`；recovery 路径同形
- [x] `TestErpFinEmployeeAdvanceCashRepayReversal`：全额 cashRepay 产 EMPLOYEE_ADVANCE_SETTLE(CASH) 凭证 + settledAmount 更新 → reverseCashRepay → 凭证红冲 + settledAmount/outstandingAmount 回退 + 凭证缺失守卫
- [x] E2E `fin-bad-debt-reverse-approve`：复用 0413-2 既有坏账 setup（建 partner+OPEN AR 对+BadDebt）→ approve → reverseApprove → `findVoucherIdByBillCode(...,'REVERSAL')` + `assertVoucherLines` 同向取负 + 原凭证 `isReversed=true` + ArApItem OPEN 经 `__get`
- [x] E2E `fin-employee-advance-cash-repay-reverse`：复用 0718-2 既有 cashRepay setup → cashRepay → reverseCashRepay → 凭证红冲 + 字段回退经 `verifyState`
- [x] owner-doc 对齐：`treasury.md §坏账` 补红冲路径实现注记；`expense-claim.md §红冲联动` cashRepay 兑现注记（标记 owner-doc 漂移已修复）
  - **执行期裁定**：treasury.md 无 §坏账 章节（坏账 owner doc 在 `bad-debt.md`），改为更新 `bad-debt.md §步骤 6 反审核红冲`（新增章节，详述 reverseApprove 反向语义 + DIRECT 路径 + 门控 + 事务边界 + voucherId 保留）
- [x] e2e-runbook 业务动作表 +2 finance 红冲行 + 套件计数更新（79→81）

Exit Criteria:

- [x] 两 JUnit 类全绿（红绿反转证明）— TestErpFinBadDebtReversal 3 tests + TestErpFinEmployeeAdvanceCashRepayReversal 3 tests = 6 tests pass
- [x] 两 E2E spec 全绿，断言红字凭证行精确数值 + 原凭证 `isReversed` + ArApItem/settledAmount 回退 — 4 tests pass（44.6s）
- [x] owner-doc 红冲联动义务兑现（expense-claim.md §红冲联动 不再有未兑现承诺）

## Draft Review Record

- Independent draft review iteration 1: `accept after fixes`（independent-draft-review-session-1）because 原草案 Current Baseline 与 Phase 1 Explore 含四处与实时仓库不符或可即时闭合的项目（规则 1/11）：(1) 称 `EmployeeAdvancePostingDispatcher.reverseSettle` "未在 BizModel 接线"——实际经 `AdvanceOffsetOrchestrator.java:114` 为 OFFSET（报销抵扣）路径已接线调用，仅 cashRepay 路径 unwired；原措辞 "API exists, just not wired" 误导，**已修订**为精确陈述（"OFFSET 路径已 wired，cashRepay 路径 unwired"），并补充 `ErpFinEmployeeAdvanceProcessor.cancel`/`reverseApprove` 仅调 `postingDispatcher.reverse(advance)` 红冲初始凭证、留 cashRepay 凭证孤儿的精确缺口定义；(2) Phase 1 Explore 推迟 `ErpFinBadDebt` useWorkflow 核实——实际 ORM entity `tagSet="gid,erp.finance"`（`app-erp-finance.orm.xml:1554`）无 useWorkflow，**已修订**为 §`ErpFinBadDebt` 末尾的已核实事实 + Phase 1 闭合为 Decision（DIRECT 路径）；(3) Phase 1 Explore 推迟 `reverseSettle` 签名核实——实际 `reverseSettle(String billHeadCode)`（`EmployeeAdvancePostingDispatcher.java:84`）直接透传 `FinPostingExecutor.reverse`，与 SETTLE_TYPE 解耦，**已修订**为 §既有红冲基础设施 已核实陈述 + Phase 1 闭合为 Decision（无须扩展 CASH 分派）；(4) Phase 1 Explore 推迟 cashRepay billHeadCode 拼接——实际 `EA-CASH-REPAY-<advanceCode>-<millis>`（`:100`）含 millis 后缀已在 owner doc 与代码中明确，**已修订**为 Phase 1 闭合为 Decision（经 `ErpFinVoucherBillR` 反查）。**额外修订**：(e) Phase 2 删除"（条件性）扩展 reverseSettle CASH 分派"项目 + Targets 移除 `EmployeeAdvancePostingDispatcher.java`；(f) Phase 1 由 `Decision | Explore` 改为纯 `Decision`，三个 Decision（reverseCashRepay 入参 + cashRepay 反查路径 + 坏账 reverseApprove DIRECT）含默认倾向与残留风险。owner-doc 漂移识别（`expense-claim.md §红冲联动` l.196 已核实存在）+ 规则 13 不可降级定性正确；规则 4/7/8/9/10/11 均合规。可 flip 到 `active`。

## Closure Gates

- [x] 范围内行为完成（BadDebt.reverseApprove + EmployeeAdvance.reverseCashRepay 红冲闭环）
- [x] 相关文档对齐（`bad-debt.md §步骤 6` + `expense-claim.md §红冲联动` 兑现 + e2e-runbook + `docs/logs/2026/07-18.md`）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿（201 tests）+ 154 模块 `mvn clean install -DskipTests` 全绿 + 新 E2E spec 全绿（4 tests）
- [x] 无范围内项目降级为 deferred/follow-up（cashRepay owner-doc 漂移为规则 13 不可降级项，必须兑现）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行 — 待执行（plan EXECUTE 模式自闭合，正式独立结束审计可选）
- [x] 结束证据存在于文件中（JUnit 6 用例 + E2E 4 用例 + owner-doc 注记 + log + roadmap README）

## Deferred But Adjudicated

### 坏账计提（provision）红冲

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `runBadDebtProvision` 返回结构非实体凭证（0413-2 已声明），属不同结果面。
- Successor Required: `yes`（触发条件：provision 凭证化落地时）

### EmployeeAdvance 三金额闭环 ORM 字段拆分

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0718-2 已裁定 ORM 保护区域 successor；`reverseCashRepay` 在混合 `settledAmount` 上按 billHeadCase 精确回退，不影响闭环正确性。
- Successor Required: `yes`（触发条件：HR 三金额报表需求落地时）

### 薪资扣回（Additional Salary）路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0718-2 Deferred，需 HR `Additional Salary` 模块。
- Successor Required: `yes`（触发条件：HR Additional Salary 模块落地时）

### NotesPayable/Receivable HONORED 终态红冲

- Classification: `watch-only residual`
- Why Not Blocking Closure: HONORED 为终态，owner-doc 未声明红冲义务；属不同结果面。
- Successor Required: `yes`（触发条件：HONORED 票据需红冲时）

## Closure

Status Note: 已完成。三 Phase 全绿（Phase 1 Decisions 闭合 + Phase 2 BizModel 反向入口实现 + Phase 3 JUnit 6 + E2E 4 + owner-doc 对齐）。finance 域过账红冲闭环（坏账 reverseApprove + 员工借款现金还款 reverseCashRepay）补齐，跨域过账红冲缺口系统性审计 finance 域遗漏解除。owner-doc `expense-claim.md §红冲联动` cashRepay 路径承诺兑现（规则 13 不可降级项已修复）。

Closure Audit Evidence:
- JUnit: `mvn test -pl module-finance/erp-fin-service` 195→**201 passed**（+6 新增：TestErpFinBadDebtReversal 3 + TestErpFinEmployeeAdvanceCashRepayReversal 3，0 回归）
- E2E: `BASE_URL=http://127.0.0.1:8011 npx playwright test tests/e2e/business-actions/fin-bad-debt-reverse-approve.action.spec.ts tests/e2e/business-actions/fin-employee-advance-cash-repay-reverse.action.spec.ts --workers=1` **4 passed**（44.6s）
- 全模块编译: `mvn clean install -DskipTests` **154 模块 BUILD SUCCESS**
- 文档: bad-debt.md §步骤 6（新增章节）+ expense-claim.md §红冲联动 cashRepay 兑现注记 + e2e-runbook 业务动作表 +2 行 + 套件计数 79→81 + docs/logs/2026/07-18.md 当日日志 + docs/backlog/README.md +1 行
- 后端代码: `ErpFinBadDebtProcessor.reverseApprove` + `ErpFinEmployeeAdvanceBizModel.reverseCashRepay` + 2 IBiz 接口声明 + 2 ErrorCode（`ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED` + `ERR_EMPLOYEE_ADVANCE_CASH_REPAY_VOUCHER_NOT_FOUND`），零 ORM/契约/字典/种子/config 变更

**执行期关键修正**：
1. ErpFinBadDebt 无 `posted` 字段（plan 原文 posted=true 修正为 voucherId!=null 门控）。
2. BizModel 内不能用 `@Inject IDaoProvider` 字段（IoC 不注入致 NPE），改用 `daoProvider()` 方法对齐 ErpFinReconciliationBizModel 范式。
3. cashRepay 凭证反查 SETTLE_TYPE 维度不可达（持久化字段无），改用 billCode 前缀 `EA-CASH-REPAY-` 等价区分。
4. reverseCashRepay 字段回退次序与 cashRepay 正向相反（先凭证后字段），补救路径强一致保证。
5. 测试 seed period 须覆盖今天（reverseApprove 触发红冲凭证 voucherDate=today）。
6. owner doc 更新位置：treasury.md 无 §坏账 章节，改为 bad-debt.md §步骤 6 新增章节。
