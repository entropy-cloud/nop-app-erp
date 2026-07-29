# 2026-07-30-0143-2-r1-13-finance-voucher-period-budget-state-machine finance 凭证/期间/预算状态机修复

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.13（P1-MA2-031 + P1-MA2-033 + P1-MA2-034，源自 A2.5a/A2.5b finance 状态机审查）
> Related: `docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`、`docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`、`docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（P1-MA3-024 CLOSED 语义三源冲突）
> Audit: required

## Current Baseline

**P1-MA2-034（carryForward 缺源年度全 CLOSED 前置）— 确认，活跃契约漂移：**
- `ErpFinBudgetScenarioProcessor.validateCarryForwardPreconditions:305-325` 仅校验 source APPROVED + target DRAFT + 同 org/schema/currency，**未校验源年度期间 CLOSED**。
- owner doc 硬前置明确：`docs/design/finance/budget.md:209` + `docs/design/finance/period-close.md:361-365`（「源 Scenario 所在年度的所有会计期间必须 CLOSED（`ErpFinAccountingPeriodStatus.glStatus = CLOSED`）——年度已结账是结转的硬前置」；L364 指明抛 `ERP_FIN_BUDGET_CARRY_FORWARD_RULE_INVALID`）。
- 可复用 ErrorCode：`ErpFinErrors.ERR_BUDGET_CARRY_FORWARD_RULE_INVALID`（`ErpFinErrors.java:419-421`，key=`erp.err.fin.budget.carry-forward-rule-invalid`，已带 rule 参数槽）。
- 无现成 `findPeriodsByFiscalYear` helper；可复用查询模式见 `ErpFinAccountingPeriodProcessor.java:233-235 / 206-209`（按 year query）。`ErpFinAccountingPeriodStatus` 实体（`app-erp-finance.orm.xml:693`）持 `glStatus`。
- 现有测试 `TestErpFinBudgetCarryForward.java:174` `seedOpenPeriod` **总是设 `status=OPEN`**、未设 glStatus——加前置后须改 seed 为 CLOSED/glStatus=CLOSED 否则测试失败（审计 :513 已预告）。

**P1-MA2-033（NEVER_OPENED→OPEN 迁移路径缺失）— 确认：**
- `ErpFinAccountingPeriodProcessor.generateNextYearPeriods:267-269` 次年 2-12 月置 `PERIOD_STATUS_NEVER_OPENED`；全仓库 grep **无 `openPeriod` BizMutation**。
- `IErpFinPeriodCloseBiz.java:47-58` javadoc 承诺「其余月份设为 NEVER_OPENED（待自然月到达时由运营开启）」，但接口仅声明 preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods，**无 openPeriod**——契约承诺无实现。
- period-status dict（`erp-fin-meta/.../dict/erp-fin/period-status.dict.yaml`）5 值含 NEVER_OPENED；`ErpFinConstants.java:139-143` 5 常量齐备。

**P1-MA2-031（凭证 DRAFT→CANCELLED 不可达 + 红字凭证终态归属未定义）— 确认：**
- `ErpFinVoucherBizModel.java:87-116` 仅有 postVoucher（DRAFT→POSTED）+ reverseVoucher（POSTED + `setIsReversed(true):113`，无状态迁移），**无 cancelVoucher**。
- voucher-status dict（`erp-fin/voucher-status.dict.yaml`）3 值 DRAFT/POSTED/**CANCELLED**；`ErpFinConstants.java:302-303` `VOUCHER_STATUS_CANCELLED` 定义但零写入。
- `app-erp-finance.orm.xml:426` 仅有 `isReversed` 布尔列，**无 `reversedVoucherId` 反向外键**；`reverseVoucher` 是原凭证上的 flag-flip（保留 POSTED + isReversed=true）。
- owner doc `state-machine.md:20/28/35/39` 声明 DRAFT→CANCELLED 迁移 + CANCELLED 终态；`:41` 红字凭证承诺「关联原凭证（双向回链）」——与代码单边 isReversed 不符。

**保护区域：** P1-MA2-034（会计硬前置）+ P1-MA2-033（期间控制）触及会计保护区域；P1-MA2-031 为契约/文档一致性。owner doc 存在。需 plan-audit + closure-audit。

## Goals

- carryForward 在源年度存在未结账期间时拒绝结转（落实 owner doc 硬前置）。
- 运营可经系统 action 将 NEVER_OPENED 期间开启为 OPEN（解除可达性死锁，兑现 `IErpFinPeriodCloseBiz` 契约）。
- 凭证 CANCELLED 死状态与红字凭证终态归属在 owner doc 中与代码实际行为对齐，消除契约漂移。

## Non-Goals

- 不实现凭证 `cancelVoucher` 新 mutation（草稿废弃经 logical delete 承载，见 Decision）。
- 不为红字凭证增加 `reversedVoucherId` 双向回链列（ORM 变更，owner doc 对齐到现状即可）。
- 不解决 P1-MA3-024 CLOSED 语义三源冲突的全部文档重写（归 MR2 R2.3）；本计划仅对齐 031/033/034 三 finding 直接相关段落。
- 不改 reverseClose kill-switch（P1-MA2-020，归 R1.11 已 done）。

## Task Route

- Type: `implementation-only change`（P1-MA2-033/034 代码）+ `app-layer design change`（P1-MA2-031 文档对齐）
- Owner Docs: `docs/design/finance/state-machine.md`、`docs/design/finance/period-close.md`、`docs/design/finance/budget.md`
- Skill Selection Basis: 新增/修改 BizMutation（openPeriod）+ Processor 校验（carryForward）→ `nop-backend-dev`（IBiz 声明顺序、requireEntity、ErrorCode、QueryBean）；测试 → `nop-testing`。会计保护区域 → plan-audit + closure-audit。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。openPeriod 不 gate（无条件可用动作）。

## Execution Plan

### Phase 1 - carryForward 源年度 CLOSED 前置（P1-MA2-034）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetScenarioProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: none

- [x] 在 `validateCarryForwardPreconditions` 增源年度 CLOSED 校验：按 `source.getFiscalYear()` 查询全部 `ErpFinAccountingPeriodStatus`（或期间 + glStatus），任一 `glStatus != CLOSED` 抛 `NopException(ERR_BUDGET_CARRY_FORWARD_RULE_INVALID).param(rule,"source fiscalYear periods not all CLOSED").param(fiscalYear,...)`。查询走 `daoProvider().daoFor(ErpFinAccountingPeriodStatus.class)`（同模块只读聚合，加注释说明原因）或复用 ErpFinAccountingPeriodProcessor 既有按 year 查询模式。
      - Skill: `nop-backend-dev`
- [x] Proof（单元）：`TestErpFinBudgetCarryForward` 现有 4 测试 seed 期间改为 glStatus=CLOSED（保持 happy path 绿）；新增负向测试——源年度留一个期间 glStatus=OPEN/CLOSING，断言 carryForward 抛 `ERR_BUDGET_CARRY_FORWARD_RULE_INVALID`。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 源年度存在未 CLOSED 期间时 carryForward 抛指定 ErrorCode（负向测试通过）；既有 4 happy path 在 CLOSED seed 下仍绿。

### Phase 2 - openPeriod 期间开启 action（P1-MA2-033）

Status: completed
Targets: `module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinPeriodCloseBiz.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinPeriodCloseBizModel.java`（或对应 Processor）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 无依赖。

- [x] **Decision**：方案 A（实现 `openPeriod`）vs 方案 B（owner doc 标注「NEVER_OPENED 仅标记，运营经 DB 直改/重新生成」为已知简化）。
      - 选择 A（推荐）：审计推荐；是低风险加法状态迁移（开启一个从未开启的期间，不结账/不反结账）；兑现 `IErpFinPeriodCloseBiz:52` 契约，解除可达性死锁。
      - 选择 B：零代码，但留下「次年 2-12 月静默不可用」运营痛点，且未兑现接口 javadoc 契约。
      - 残留风险：A 需守卫 + 权限 + 测试；B 留运营缺口。采纳 A，理由记入计划。
      - Skill: `none`
- [x] 按强制顺序：先 `IErpFinPeriodCloseBiz` 声明 `@BizMutation openPeriod(@Name("periodId") Long periodId, IServiceContext context)`；再 BizModel/Processor 实现——`requireEntity` 取期间，守卫 `status==NEVER_OPENED`（否则抛 `ERR_FIN_PERIOD_ILLEGAL_TRANSITION` 或既有期间非法迁移码），置 `PERIOD_STATUS_OPEN` + `saveEntity`；`assertPeriodNotLocked` 复用。
      - Skill: `nop-backend-dev`
- [x] Proof（单元）：测试 NEVER_OPENED→OPEN 成功 + 非 NEVER_OPENED 状态调用被守卫拒绝（抛异常）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] `openPeriod` 将 NEVER_OPENED 期间迁移至 OPEN（正向测试）；非法源状态被守卫拒绝（负向测试）；IBiz 接口先于实现声明（自检 P2）。

### Phase 3 - 凭证 CANCELLED 死状态 + 红字凭证终态 owner doc 对齐（P1-MA2-031）

Status: completed
Targets: `docs/design/finance/state-machine.md`
Skill: `none`

- Item Types: `Decision | Fix`
- Prereqs: none（纯文档；P1-MA2-031 为已确认 owner-doc 漂移，故对齐动作为 `Fix` 而非 `Add`）。

- [x] **Decision**：凭证 DRAFT→CANCELLED 死状态处置。
      - 选择 A（推荐）：owner doc 对齐现状——标注「草稿凭证废弃经 logical delete（useLogicalDelete）承载，不经 DRAFT→CANCELLED 状态迁移；CANCELLED dict 项保留为预留语义入口（未来显式作废工作流 successor）」。
      - 选择 B：实现 `cancelVoucher`（DRAFT→CANCELLED）mutation + 守卫 + 测试。
      - 选择 C：从 ORM 删除 CANCELLED dict 项 + 常量（ORM 变更 + regen）。
      - 理由：现有 reverseVoucher 已覆盖 POSTED 凭证红冲；草稿废弃 logical delete 已工作；无 PM 需求驱动新增会计 mutation。采纳 A（最低风险、消除契约漂移）。
      - Skill: `none`
- [x] 按 Decision 更新 `state-machine.md`：§1/§2/§3 标注 CANCELLED 为「预留（logical delete 承载草稿废弃）」；§3 红字凭证段对齐为「单边 `isReversed=true` 标记（原凭证保留 POSTED），不建立 reversedVoucherId 双向回链——已知简化」。
      - Skill: `none`

Exit Criteria:

- [x] `state-machine.md` 凭证段反映 CANCELLED 预留语义 + 红字凭证实际 isReversed 行为，与代码一致。

## Draft Review Record

- Independent draft review iteration 1: accept (review 2026-07-30-0143-2)。格式合规（必需 front matter/section 齐备，Phase 结构有效）；三 finding 同属 finance 状态机 owner-doc（state-machine/period-close/budget），按指南规则 14 合并为单计划合理；范围边界清晰（Non-Goals 显式排除 cancelVoucher/reversedVoucherId/P1-MA3-024 全量重写）；Exit Criteria 可测；Closure Gates 与结束证据槽就位。修订两处：Phase 3 Item Types `Decision | Add`→`Decision | Fix`（P1-MA2-031 为已确认 owner-doc 漂移，指南规则 7 要求 `Fix`）；聚焦验证命令补 `-am`（Phase 2 改 erp-fin-dao IBiz，需重建依赖）。无 Blocker，可进入实施。

## Closure Gates

- [x] 范围内行为完成（carryForward CLOSED 前置 + openPeriod 迁移 + 凭证状态机文档对齐）
- [x] 相关文档对齐（state-machine.md / period-close.md / budget.md）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am`（聚焦 finance，`-am` 含 Phase 2 改动的 erp-fin-dao）+ Closure 时 `mvn clean install -DskipTests` 全绿
- [x] 无范围内项目降级为 deferred/follow-up（Phase 3 Decision A 的「预留 successor」是裁决后明确移出范围的语义入口，非范围内缺陷降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 凭证 cancelVoucher 显式作废工作流（Decision A 选择 B/C 的 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 草稿废弃经 logical delete 已工作；红字凭证经 reverseVoucher 已工作；CANCELLED 作为预留 dict 项已文档化。无活跃数据破坏。
- Successor Required: `yes`（当 PM 要求「保留审计轨迹的显式作废动作」时实现 cancelVoucher mutation + reversedVoucherId 双向回链）

### 红字凭证 reversedVoucherId 双向回链

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已对齐为单边 isReversed 简化；红冲闭环功能完整（reverseVoucher 已实现）。
- Successor Required: `yes`（若采用双向回链报表需求）

## Closure

Status Note: 三 Phase 全 done（P1-MA2-034 carryForward 源年度 CLOSED 前置 + P1-MA2-033 openPeriod NEVER_OPENED→OPEN 迁移 + P1-MA2-031 凭证 CANCELLED/红字凭证 owner doc 对齐）。验证：`mvn test -pl module-finance/erp-fin-service -am` 293 测试全绿（含新增 1 carryForward 负向 + 2 openPeriod 测试）；`mvn clean install -DskipTests` 全工程绿。结束审计由独立子代理（新会话，无执行者上下文）执行通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence: 实时仓库复核确认三 Phase 落地——(1) P1-MA2-034：`ErpFinBudgetScenarioProcessor.validateCarryForwardPreconditions:326-337` 增源年度 CLOSED 硬前置 + `isSourceFiscalYearFullyClosed:346-372` helper（按 fiscalYear 查 ErpFinAccountingPeriod + ErpFinAccountingPeriodStatus，任一 glStatus≠CLOSED 抛 `ERR_BUDGET_CARRY_FORWARD_RULE_INVALID`，带 rule+fiscalYear 参数）；`TestErpFinBudgetCarryForward:164-182` 负向测试（源年度留一期间 glStatus=OPEN，断言抛指定 ErrorCode），既有 4 happy path seed 已改 glStatus=CLOSED（:192-217）。(2) P1-MA2-033：`IErpFinPeriodCloseBiz:52` 声明 `@BizMutation openPeriod` → `ErpFinAccountingPeriodBizModel:61` Facade → `ErpFinAccountingPeriodProcessor.openPeriod:323-329` 实现（requirePeriod + `assertPeriodStatus(NEVER_OPENED,"开启")` 守卫 + 置 PERIOD_STATUS_OPEN + flush）；`TestErpFinPeriodStateMachine:114-130` 正向（NEVER_OPENED→OPEN）+ 负向（非 NEVER_OPENED 被守卫拒绝）。IBiz 先于实现声明（自检 P2 通过）。(3) P1-MA2-031：`docs/design/finance/state-machine.md:20/27-29/35-36/40-43/61-62/70-71/95/249-250` CANCELLED 标注为预留 dict 项（草稿废弃经 logical delete 承载）+ 红字凭证对齐为单边 isReversed 简化，与代码 isReversed/reverseVoucher 行为一致。docs/logs/2026/07-30.md 记录完成。文本一致性：Plan Status=completed、三 Phase Status=completed、Exit Criteria 全 [x]、Closure Gates 全 [x] 一致。

Follow-up:

- _（非阻塞跟进；已确认缺陷不得出现在此处，详见上方 Deferred But Adjudicated）_
