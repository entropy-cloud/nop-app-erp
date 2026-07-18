# 2026-07-18-2251-2-finance-bad-debt-provision-reversal finance 坏账准备计提反向入口

> Plan Status: active
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: finance-bad-debt-provision-reversal
> Source: 跨域过账红冲缺口系统性审计 Phase 2（承接 `docs/plans/2026-07-18-1745-3` Deferred「坏账计提（provision）红冲」——触发条件「provision 凭证化落地时」经实时仓库核实**已满足**：`BadDebtProvisionService.runBadDebtProvision` 实测经 `CloseVoucherWriter.writeVoucher` 已生成 BAD_DEBT_RESERVE/BAD_DEBT_RELEASE 凭证 + ErpFinVoucherBillR 业财回链；原 1745-3 Deferred 措辞「`runBadDebtProvision` 返回结构非实体凭证」系采信 0413-2 旧记忆的过时陈述——实时仓库已证伪。本计划补齐反向入口，闭合 finance 域红冲闭环 Phase 2）。
> Related: `2026-07-18-1745-1`/`1745-2`/`1745-3`（同型红冲闭环 Phase 1 已完成）、`2026-07-12-0413-2`（坏账业务动作 E2E，已 completed）、`2026-07-18-1745-3`（坏账 reverseApprove + EmployeeAdvance reverseCashRepay，已 completed）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18），finance 域坏账准备期末计提**正向链路已完整落地且已生成凭证**，**反向红冲入口完全缺失**：

### 正向链路（已落地，且已凭证化——证伪 1745-3 Deferred 措辞）

- `ErpFinBadDebtBizModel.runBadDebtProvision(@Name("periodId") Long, IServiceContext)` `@BizMutation`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBadDebtBizModel.java:79`）委派 `BadDebtProvisionService.runBadDebtProvision`。
- `BadDebtProvisionService.runBadDebtProvisionForSchema`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/baddebt/BadDebtProvisionService.java:83`）按账套循环：
  - `calculateRequiredProvision(period)` 计算必需准备（账龄分桶法）
  - `getAllowanceBalance()`（`:141`）查 GL Allowance 累计余额（经 `ErpFinVoucherLine` 聚合，过滤 `isReversed=false` + 非 BUDGET 凭证）
  - **必需 > 账面 → 经 `CloseVoucherWriter.writeVoucher(..., "BDR", ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.code, ErpFinBusinessType.BAD_DEBT_RESERVE.name(), ...)`（`:98-102`）写 BAD_DEBT_RESERVE 凭证**（Dr 信用减值损失 / Cr 坏账准备）
  - **必需 < 账面 → 经 `CloseVoucherWriter.writeVoucher(..., "BDL", ErpFinConstants.BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.code, ErpFinBusinessType.BAD_DEBT_RELEASE.name(), ...)`（`:114-118`）写 BAD_DEBT_RELEASE 凭证**（Dr 坏账准备 / Cr 信用减值损失）
  - 相等 → `action=NONE` 无凭证
- `CloseVoucherWriter.writeVoucher`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/close/CloseVoucherWriter.java:61`）：写 `ErpFinVoucher`（docStatus=POSTED, isReversed=false）+ `ErpFinVoucherLine` 行 + `ErpFinVoucherBillR`（billType/BILL_CODE/businessType 三元组）。**关键缺陷**：`writeVoucher` 实测**无幂等检查**——同 billHeadCode 二次调用创建第二张凭证 + 第二条 billR（voucher.code 含 UUID 后缀不冲突，但 billR.billHeadCode 重复）。
- `BadDebtProvisionResult`（`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/BadDebtProvisionResult.java`）含 `voucherId` 字段（`:104` 后 `result.setVoucherId(voucherId)`）——**结构已含凭证 ID，证伪 1745-3「返回结构非实体凭证」措辞**。

### 反向链路（完全缺失——本计划范围）

- `ErpFinBadDebtBizModel` 仅有 `runBadDebtProvision`（`:79`），**无 `reverseBadDebtProvision` / `reverseProvision` 入口**——一旦期末计提执行（误选期间 / 误算 / 业务变化需重提），无反向回滚入口。
- `BadDebtProvisionService` 无 reverse 方法。
- **多凭证风险**：`CloseVoucherWriter` 无幂等检查，多次 `runBadDebtProvision` 同期间产生多张 BAD_DEBT_RESERVE 或 BAD_DEBT_RELEASE 凭证（billHeadCode 相同但 voucher.code 不同）；`getAllowanceBalance`（`:141`）查余额聚合所有非红冲凭证 → 第二次调用余额已被首张凭证影响 → 计算偏差。

### 既有红冲基础设施（可复用，无 API 缺口）

- `FinPostingExecutor.reverse(String billHeadCode, ErpFinBusinessType businessType)`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/FinPostingExecutor.java:31`）已存在，委派 `voucherBiz.reverse(billHeadCode, businessType, context)`。
- `IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)` platform 内置幂等守护（同 billHeadCode+businessType 无凭证时安全 no-op）。
- 范式参照：`ErpFinBadDebtProcessor.reverseApprove`（1745-3 落地，调 `FinPostingExecutor.reverse(badDebt.code, BAD_DEBT_WRITE_OFF|RECOVERY)`）；`ErpFinEmployeeAdvanceBizModel.reverseCashRepay`（1745-3 落地，经 `ErpFinVoucherBillR` 反查多凭证 → reverse 最新一笔）。
- `ErpFinConstants.BAD_DEBT_RESERVE_BILL_CODE_PREFIX` + `BAD_DEBT_RELEASE_BILL_CODE_PREFIX` 常量已存在（正向 `writeVoucher` 使用）。

### owner doc 状态

- `docs/design/finance/bad-debt.md §步骤2`（l.52-60）声明计提设计 + `§步骤5` 释放 + `§步骤6` 反审核红冲（已由 1745-3 落地于坏账核销路径）。
- **owner doc 未声明计提反向红冲路径**——属实现层缺口 + 1745-3 Deferred 已声明 successor（触发条件已满足）。本计划补 `§步骤2b 反向红冲` 章节，闭合 owner doc 红冲义务。

### 剩余差距

1. `ErpFinBadDebtBizModel` 缺 `reverseBadDebtProvision(periodId)` `@BizMutation` 反向入口。
2. `BadDebtProvisionService` 缺 reverse 方法（按 periodId 反查 BAD_DEBT_RESERVE/RELEASE 凭证 + 调 FinPostingExecutor.reverse）。
3. `IErpFinBadDebtBiz` 缺接口声明。
4. owner doc `bad-debt.md` 缺 §步骤2b 反向红冲章节。

## Goals

- `BadDebtProvisionService` 新增 `reverseBadDebtProvision(Long periodId, IServiceContext context) → BadDebtProvisionResult` 方法：经 `ErpFinVoucherBillR` 反查该期间全部 BAD_DEBT_RESERVE + BAD_DEBT_RELEASE 凭证（`billCode = BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.code` 完全匹配 或 `billCode = BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.code` 完全匹配——`CloseVoucherWriter.writeVoucher` 实测 billCode 确定性派生无 UUID 后缀，**用 `=` 完全匹配非 LIKE**）+ `isReversed=false` 过滤 → 逐张调 `FinPostingExecutor.reverse(billCode, BAD_DEBT_RESERVE|RELEASE)` 红冲 → 返回结果含红冲凭证数量 + 反向金额合计。
- `ErpFinBadDebtBizModel.reverseBadDebtProvision(@Name("periodId") Long, IServiceContext)` `@BizMutation` 委派 service；接口声明加入 `IErpFinBadDebtBiz`。
- 守卫：(1) 未找到任何 BAD_DEBT_RESERVE/RELEASE 凭证抛 `ERR_BAD_DEBT_PROVISION_NOT_FOUND`（新增 ErrorCode）；(2) period.status=CLOSED_FINAL 抛 `ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED`（新增 ErrorCode）——服务层显式守卫（不依赖 `ErpFinVoucher` 写入时的期间状态校验，提供清晰错误 token + GraphQL 友好错误消息）。
- 浏览器层 E2E 断言红冲产物（凭证行同向取负 + 原凭证 `isReversed=true` + Allowance 余额回退）。
- 兑现 1745-3 Deferred successor（owner-doc 缺口补齐——bad-debt.md §步骤2b 反向红冲章节）。

## Non-Goals

- **不改 ORM/契约/字典/种子**——纯应用层 Java + 测试。
- **不修复 `CloseVoucherWriter` 无幂等检查的根因**（属不同结果面——多凭证累积是 `writeVoucher` 通用缺陷，跨损益结转/汇兑重估/坏账计提多个 caller；本计划仅在 reverse 路径按 `ErpFinVoucherBillR` 反查**全部**同 billCode 凭证一次性红冲，覆盖既有累积；根因修复归 successor，触发条件：writeVoucher 幂等缺失在生产引发其它域数据完整性事故时）。
- **不实现 period 级累计算出（provision 累计 / 反向后自动重提）**——本计划仅交付手动反向入口；如用户须重提，反向后重新调用 `runBadDebtProvision(periodId)` 即可（`getAllowanceBalance` 重新计算 + 写新凭证）。
- **不实现 reverseBadDebtProvision 批量多期间入口**——单期间反向已覆盖核心场景；批量归运营自动化面 successor（对齐 0718-1 `accrueAllFacilities` 同型 Deferred）。
- **不实现反向审批工作流**——`ErpFinBadDebt` 无 useWorkflow tagSet（1745-3 已核实），DIRECT 路径调 reverse 即可；如业务要求双层审批属不同结果面 successor。
- **不覆盖 `ErpFinAccountingPeriodProcessor.java:114` 同型 calculateRequiredProvision 调用点**——该调用为**预览**（不写凭证），经实时仓库核实（`badDebtProvisionService.calculateRequiredProvision(period)` 仅返回 result，不调 `writeVoucher`），无反向缺口。

## Task Route

- Type: `implementation-only change`（含 owner-doc 缺口补齐）
- Owner Docs: `docs/design/finance/bad-debt.md`（§步骤2 计提 + §步骤5 释放 + §步骤6 反审核红冲）、`docs/design/finance/posting.md §冲销机制`、`docs/design/finance/treasury.md §坏账`
- Skill Selection Basis: BizModel 新 `@BizMutation` + service 反查多凭证 + 跨实体 `FinPostingExecutor.reverse` 调用 + owner-doc 缺口补齐 → 加载 `nop-backend-dev` skill（I*Biz injection + 跨实体调用 + protected step 方法 + owner-doc 对齐）
- Protected Areas: 无 ORM/契约；红冲涉及 finance 保护区域（BS / P&L 凭证），但反向语义经 owner doc §步骤6 范式已确立（1745-3 落地）；多凭证反查精确性须单测覆盖。

## Infrastructure And Config Prereqs

- 无新基础设施；复用既有 config + 种子 COA（1231 坏账准备 / 6701 信用减值损失 / 1122 应收账款 已由 0413-2 落地）。
- 浏览器层 E2E 经既有 webServer JVM args（`erp-fin.bad-debt-allowance-subject-code=1231` + `erp-fin.bad-debt-expense-subject-code=6701` + `erp-fin.bad-debt-ar-subject-code=1122`，0413-2 已配置）。

## Execution Plan

### Phase 1 — Decision：多凭证红冲策略 + reverse 入参形态裁定

Status: planned
Targets: 探索笔记（不落仓库除非裁定须文档化）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [ ] Decision: 多凭证红冲策略——经实时仓库核实 `CloseVoucherWriter.writeVoucher` 无幂等检查（`CloseVoucherWriter.java:61-139` 逐次创建新 voucher + billR），多次 `runBadDebtProvision` 同期间产生多张 BAD_DEBT_RESERVE/RELEASE 凭证。**三选一裁定**：(a) 反向全部（按 `ErpFinVoucherBillR` 反查所有 `isReversed=false` 凭证逐张红冲）；(b) 仅反向最新一张；(c) 反向全部 + 拒绝多次调用的根因修复（writeVoucher 幂等门控）。**默认倾向 (a)**：业务语义最自然（"撤销该期间全部计提"），且与根因修复 (c) 解耦（根因属不同结果面 successor）。裁定须记录选择 + 替代方案 + 残留风险
  - 替代方案 (b) 拒绝原因：单张反向留下其它累积凭证成孤儿，违反反向语义完整性。
  - 替代方案 (c) 拒绝原因：writeVoucher 跨多个 caller（损益结转/汇兑重估/坏账），加幂等门控须逐 caller 评估（部分 caller 可能依赖每次新凭证语义），属不同结果面 + 高风险。
  - 残留风险：策略 (a) 反向后用户重提须再调 `runBadDebtProvision`（`getAllowanceBalance` 重算）——多次反向/重提产生大量红字凭证链，可能影响 voucher 表性能。可接受（运营场景频次低）。
- [ ] Decision: `reverseBadDebtProvision` 入参形态——经实时仓库核实 `runBadDebtProvision(periodId)` 单参，**裁定 `reverseBadDebtProvision(periodId)` 单参对称**（无须 schemaId 入参——service 内部按 `schemaPropagator.resolveTargetSchemas` 循环全部 schema 对称正向）；返回类型 `BadDebtProvisionResult` 复用（设置 `action="REVERSED"` + voucherId=null + reversedCount 字段扩展，或新建 `BadDebtProvisionReversalResult` DTO——后者更清晰）。**默认倾向：新建 `BadDebtProvisionReversalResult` DTO**（避免既有 DTO 语义污染）。裁定须记录选择 + 替代方案
- [ ] Decision: period 状态门控——经实时仓库核实 `ErpFinAccountingPeriod` 有状态机（OPEN/CLOSING/CLOSED/CLOSED_FINAL）。**裁定：reverseBadDebtProvision 守卫 `period.status != CLOSED_FINAL`**（CLOSED_FINAL 期间禁止任何凭证变动，对齐 `ErpFinVoucherBizModel` 既有期间硬约束）；其它状态允许反向。裁定须记录守卫 ErrorCode + message token

> 探索项须 Phase 1 闭合为 Decision，否则 Phase 2 不能开始。

Exit Criteria:

- [ ] 三 Decision 已落记录（含替代方案 + 残留风险）

### Phase 2 — `BadDebtProvisionService.reverseBadDebtProvision` + BizModel 接线

Status: planned
Targets:
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/baddebt/BadDebtProvisionService.java`
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBadDebtBizModel.java`
  - `module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinBadDebtBiz.java`
  - `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/BadDebtProvisionReversalResult.java`（新建 DTO，按 Phase 1 Decision (b)）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinErrors.java`（新增 ErrorCode）
Skill: `nop-backend-dev`

- Item Types: `Add`（owner-doc §步骤2b 新章节 + 1745-3 Deferred successor 兑现均为 Add 范畴——bad-debt.md §步骤2b 是新增章节非修复现有承诺文本；service 方法 + BizModel + DTO + ErrorCode + IBiz 接口均为新增）
- Prereqs: Phase 1

- [ ] `BadDebtProvisionService.reverseBadDebtProvision(Long periodId, IServiceContext context) → BadDebtProvisionReversalResult`：守卫 period 存在 + status != CLOSED_FINAL（按 Phase 1 Decision (c)）→ 按 `ErpFinVoucherBillR` 反查该期间全部 `billCode = BAD_DEBT_RESERVE_BILL_CODE_PREFIX + period.code` 或 `BAD_DEBT_RELEASE_BILL_CODE_PREFIX + period.code` 凭证（按 Phase 1 Decision (a) 反向全部）→ 过滤 voucher `isReversed=false` → 逐张调 `FinPostingExecutor.reverse(billCode, BAD_DEBT_RESERVE|RELEASE)` → 累计 reversedCount + reversedReserveAmount + reversedReleaseAmount → 返回 result；守卫未找到任何凭证抛 `ERR_BAD_DEBT_PROVISION_NOT_FOUND`
- [ ] `BadDebtProvisionReversalResult` DTO：含 `periodId` / `periodCode` / `reversedReserveCount` / `reversedReleaseCount` / `reversedReserveAmount` / `reversedReleaseAmount` / `totalReversedCount` 字段
- [ ] `ErpFinBadDebtBizModel.reverseBadDebtProvision(@Name("periodId") Long, IServiceContext)` `@BizMutation` 委派 service；接口声明加入 `IErpFinBadDebtBiz`
- [ ] `ErpFinErrors.ERR_BAD_DEBT_PROVISION_NOT_FOUND` + `ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED`（按 Phase 1 Decision (c)）+ ARG_PERIOD_ID 参数声明

> 接口契约：`reverseBadDebtProvision(periodId)` 为 finance 域侧 reverse 入口（与 `runBadDebtProvision(periodId)` 对称）；返回 DTO 含红冲证据。多凭证反查须精确（按 `ErpFinVoucherBillR` billType + billCode 完全匹配，非模糊匹配，避免误反查其它期间）。

> **执行期注意**：如 `FinPostingExecutor.reverse` 在某一张凭证红冲失败抛异常，须决定整体事务边界：(a) REQUIRES_NEW 逐张独立（部分红冲成功 + 部分失败留下残留）；(b) 整体事务回滚（任一失败回滚全部）。**默认倾向 (b)**（与 1745-3 reverseCashRepay 强一致范式对称——反审核是补救路径须保证无残留半状态）。@BizMutation 默认事务边界 REQUIRED 已足够（无须 @Transactional 显式声明）。

Exit Criteria:

- [ ] `ErpFinBadDebt__reverseBadDebtProvision` GraphQL 端点可达，红冲指定期间全部 BAD_DEBT_RESERVE/RELEASE 凭证
- [ ] 新代码本地编译通过（完整模块 JUnit 在 Closure Gates 验证）

> **执行期注意（防 Nop `@Inject IDaoProvider` NPE gotcha）**：BizModel 内不能用 `@Inject IDaoProvider` 字段（IoC 不注入致 NPE，1745-3 closure l.208 已记录）——`BadDebtProvisionService` 已用 `@Inject IDaoProvider daoProvider` 字段（service 层非 BizModel 层，IoC 正常注入），本计划在 service 层扩展现有方法，数据访问经 `daoProvider.daoFor(ErpFinVoucherBillR.class)` + `daoProvider.daoFor(ErpFinVoucher.class)`，**不在 BizModel 层引入 `@Inject IDaoProvider`**。

### Phase 3 — JUnit + 浏览器层 E2E + owner-doc 对齐

Status: planned
Targets:
  - `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/TestErpFinBadDebtProvisionReversal.java`（新建）
  - `tests/e2e/business-actions/fin-bad-debt-provision-reverse.action.spec.ts`（新建）
  - `docs/design/finance/bad-debt.md`（§步骤2b 反向红冲新增章节）
  - `docs/testing/e2e-runbook.md`（业务动作表 +1 行）
Skill: `nop-testing`

- Item Types: `Add | Proof`（owner-doc §步骤2b 新章节为 Add 范畴——非修复现有承诺文本）
- Prereqs: Phase 2

- [ ] `TestErpFinBadDebtProvisionReversal`：(a) 单凭证反向——建 OPEN AR 对 + runBadDebtProvision 产 1 张 BAD_DEBT_RESERVE 凭证（Dr 6701/Cr 1231）→ reverseBadDebtProvision → 原凭证 `isReversed=true` + 红字凭证行同向取负（Dr 6701=-X/Cr 1231=-X）+ `getAllowanceBalance` 回退至反向前余额 + reversedReserveCount=1；(b) 多凭证累积反向——runBadDebtProvision 两次产生 2 张 BDR 凭证 → reverseBadDebtProvision → 两张全 `isReversed=true` + 红字凭证 2 张 + reversedReserveCount=2（按 Phase 1 Decision (a) 反向全部）；(c) 未找到凭证守卫——无计提记录的期间调用抛 `ERR_BAD_DEBT_PROVISION_NOT_FOUND`；(d) CLOSED_FINAL 期间守卫——置 period.status=CLOSED_FINAL 调用抛 `ERR_BAD_DEBT_PROVISION_PERIOD_FINAL_CLOSED`；(e) 混合反向——同期间产 1 BDR + 1 BDL（修改 AR 数据触发不同方向）→ reverseBadDebtProvision → 两张全红冲
- [ ] E2E `fin-bad-debt-provision-reverse`：复用 0413-2 既有坏账 setup（建 partner+OPEN AR 对+period）→ `runBadDebtProvision` mutation → `reverseBadDebtProvision` mutation → 经 `findVoucherIdByBillCode(BDR-{period.code}, "REVERSAL")` 反查红字凭证 + `assertVoucherLines` 同向取负 + 原凭证 `isReversed=true` + GraphQL response 断言 `reversedReserveCount=1`
- [ ] owner-doc 对齐：`bad-debt.md §步骤2b 反向红冲`（新增章节，详述 reverseBadDebtProvision 反向语义 + DIRECT 路径 + 多凭证策略 + 期间状态门控 + 事务边界）；e2e-runbook 业务动作表 +1 finance 反向行 + 套件计数更新

Exit Criteria:

- [ ] JUnit 全绿（红绿反转证明：移除 reverse 调用则原凭证 `isReversed=true` 断言红）
- [ ] E2E spec 全绿，断言红字凭证行精确数值 + 原凭证 `isReversed` + reversedCount
- [ ] owner-doc §步骤2b 章节落地

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_08a44747effewp7hdMN7n7gKI）because 一处 BLOCKER：Non-Goals l.66「不实现期间已 CLOSED_FINAL 的反向禁止门控」与 Phase 1 Decision (c)/Phase 2 ErrorCode/Phase 3 test (d) 假设的 in-scope 守卫内部矛盾（规则 10/11/2）。**已修订**：移除 Non-Goals 中「不实现 CLOSED_FINAL 门控」条目，保留 Phase 1 Decision (c) in-scope 守卫（服务层显式守卫提供清晰错误 token + GraphQL 友好错误消息，不依赖 `ErpFinVoucher` 写入时的期间状态校验）。ADVISORY 已修订：(a) Goals `LIKE` → `=` 完全匹配（`CloseVoucherWriter.writeVoucher` 实测 billCode 确定性派生无 UUID 后缀）；(b) Phase 2 Exit Criteria 全模块 JUnit 移至 Closure Gates，本地化编译检查替代（执行时规则 7）；(c) Phase 2/3 Item Types `Add | Fix` → `Add`（owner-doc §步骤2b 是新增章节非 Fix 现有承诺——bad-debt.md §步骤6 仅覆盖 BAD_DEBT_WRITE_OFF/RECOVERY，provision 反向是 1745-3 Deferred successor 触发的新义务非漂移修复）；(d) Phase 2 增「执行期注意」段提醒 `@Inject IDaoProvider` BizModel NPE gotcha（1745-3 closure l.208 已踩坑）。规则 1/4/7/9/10/11/13/14 修订后均合规。当前草案 13 处 VERIFIED 基线主张经实时仓库核实准确（含 1745-3 Deferred 措辞「provision 凭证化」触发条件经核实**已满足**——`BadDebtProvisionService.runBadDebtProvisionForSchema:98-102` + `:114-118` 实测调 `CloseVoucherWriter.writeVoucher` 写真实 ErpFinVoucher + ErpFinVoucherLine + ErpFinVoucherBillR，证伪 1745-3 旧措辞）。
- Independent draft review iteration 2: 待审查

## Closure Gates

- [ ] 范围内行为完成（`reverseBadDebtProvision` 反向入口 + 多凭证策略 + 期间状态门控）
- [ ] 相关文档对齐（`bad-debt.md §步骤2b` 新增章节 + e2e-runbook + `docs/logs/2026/07-18.md` 或当日日志）
- [ ] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿 + 154 模块 `mvn clean install -DskipTests` 全绿 + 新 E2E spec 全绿
- [ ] 无范围内项目降级为 deferred/follow-up（owner-doc §步骤2b 新章节 + 多凭证策略 (a) 均纳入范围内不降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致（**含 CLOSED_FINAL 守卫——Non-Goals 已移除"不实现"措辞，与 Phase 1 Decision (c)/Phase 2 ErrorCode/Phase 3 test (d) 守卫一致**）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### `CloseVoucherWriter` 无幂等检查根因修复

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `CloseVoucherWriter.writeVoucher` 跨多个 caller（损益结转/汇兑重估/坏账计提），加幂等门控须逐 caller 评估（部分 caller 可能依赖每次新凭证语义）。本计划按 Phase 1 Decision (a) 在 reverse 路径覆盖既有累积。
- Successor Required: `yes`（触发条件：writeVoucher 幂等缺失在生产引发其它域数据完整性事故时）

### 多期间批量反向入口

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 单期间反向已覆盖核心场景；批量属运营自动化面。
- Successor Required: `yes`（触发条件：财务月结批量反向需求落地时）

### 反向审批工作流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpFinBadDebt` 无 useWorkflow tagSet（1745-3 已核实），DIRECT 路径；如业务要求双层审批属不同结果面。
- Successor Required: `yes`（触发条件：财务合规要求反向须主管审批时）

## Closure

Status Note: 待执行。

Closure Audit Evidence:

- Auditor / Agent: 待执行（独立子代理 fresh session）
- Evidence: 待执行结束后填充
