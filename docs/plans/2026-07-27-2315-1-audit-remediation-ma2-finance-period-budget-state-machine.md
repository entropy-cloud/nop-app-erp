# 2026-07-27-2315-1-audit-remediation-ma2-finance-period-budget-state-machine MA2 finance 状态机审查 — 预算与期间（A2.5b）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.5b finance 状态机审查 — 预算与期间（S 级拆分 2/3）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.5b）
> Related: `docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`（A2.5a 凭证状态机，done；其 §Deferred But Adjudicated 显式将"期间状态机系统性审查"交接 A2.5b；P1-MA2-021 CLOSED_FINAL 凭证锁定经 A2.5a 凭证侧复核维持 P1，本审计复核**期间侧守卫**）；`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（A2.3 期末结账端到端，已落地期间状态机 OPEN→CLOSING→CLOSED→CLOSED_FINAL + 反结账 + 年度结转；P1-MA2-017/018/019/020/021/022 已登记供本审计从状态机角度复核）；`docs/plans/2026-07-27-2211-1-audit-remediation-ma2-inventory-costing-consistency.md`（A2.4 库存核算，invStatus 模块关账交互）；`docs/plans/2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`（P2-MA1-019 fromCode 异常 + LocalDate.now 待 MR1）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/finance/state-machine.md §对象二`+`period-close.md`+`budget.md`（owner doc）
> Audit: required

## Current Baseline

期间状态机与预算方案状态机是 ERP 财务核算时间维度的两大控制枢纽。**会计期间状态机**（`ErpFinAccountingPeriod.status` dict `erp-fin/period-status`：OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL）经 `closePeriod`（月度结账：AR→AP→INV→AST→GL 模块按序关账 + 损益结转 + 汇兑重估）→ `finalizePeriod`（CLOSED→CLOSED_FINAL 最终锁定）→ `reverseClose`（反结账回 OPEN + 红冲结转/汇兑/年度凭证）三 mutation 驱动；年度结账 12 月追加辅助账对账门控 + 本年利润→未分配利润 + 次年期间 populate。**预算方案状态机**（`ErpFinBudgetScenario.docStatus` dict `erp-fin/budget-status`：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED）经 submit→approve 生成 BUDGET 影子凭证参与预算控制，A2 扩展 rollForward（滚动预算复制）/carryForward（结转规则引擎，源 Scenario 终态 CLOSED）/承付会计（COMMITMENT 凭证 commit/release）。owner doc `state-machine.md §对象二`（130-228 行）定义期间状态机 4 态 + 反结账路径；`budget.md` 定义预算方案状态机 + 承付/结转/滚动扩展；`period-close.md` 定义期末结账 8 步概念模型 + 反结账 + 年度结转。A2.5a 仅覆盖**会计凭证状态机**；本审计 A2.5b 覆盖**会计期间状态机 + 预算方案状态机**。

实时仓库已落地的期间与预算实现（逐项核实）：

- **期间聚合 BizModel Facade**（`module-finance/erp-fin-service/.../service/entity/ErpFinAccountingPeriodBizModel.java`）：`closePeriod():43-44`（@BizMutation）/ `finalizePeriod():49-50`（@BizMutation）/ `reverseClose():55-56`（@BizMutation）/ `preCheck`（@BizQuery 只读结构化报告）。委托 `ErpFinAccountingPeriodProcessor`。
- **期间状态机轴**（`ErpFinAccountingPeriod`，`module-finance/model/app-erp-finance.orm.xml`）：`status` dict `erp-fin/period-status`（**5 态**：OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL — owner doc `state-machine.md §对象二:128-133` 仅列 4 态 CLOSED/OPEN/CLOSING/CLOSED_FINAL，**未含 NEVER_OPENED**）；`closedAt`/`closedBy`；`year`/`month`/`quarter`/`isAdjustment`。
- **期间 Processor**（`ErpFinAccountingPeriodProcessor.java`）：
  - `closePeriod():130-163`：`assertPeriodStatus(OPEN)` 前置 → preCheck（config-gated `auto-post-on-close` 阻断/提示）→ `advanceModule(AR/AP)` + `closeInvModule`（reclose 存货成本）+ `closeAssetModule`（折旧）+ `closeGlModule`（汇兑重估 + 损益结转）→ 年度分支 `closeAnnual`（12 月：辅助账对账 + 本年利润→未分配利润 + 次年期间 populate）→ **`:157-158` 连续 `setStatus(CLOSING)` 再 `setStatus(CLOSED)` 无中间 flush**（**P2-MA2-025** CLOSING 永不对外可见，并发结账同期间无法靠 CLOSING 态互斥——交接 A2.17）→ `setClosedAt/closedBy` + flushSession。
  - `finalizePeriod():204-210`：`assertPeriodStatus(CLOSED)` 前置 → `setStatus(CLOSED_FINAL)` + flushSession。**owner doc `state-machine.md §对象二` 迁移图 `CLOSING → CLOSED_FINAL`（结账成功）未体现 CLOSED 中间态 + 独立 finalizePeriod 步骤**——设计与实现状态命名/迁移路径不一致（CLOSED 在 owner doc 是"结账完成待复核"，在代码是 closePeriod 已直接到达的态，需 finalizePeriod 才到 CLOSED_FINAL）。
  - `reverseClose():274-309`：`assertPeriodStatus(CLOSED_FINAL)` 前置 → **`:278-281` kill-switch**（`isReverseCloseApprovalRequired()` 默认 true 时直接 throw `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`，反结账完全不可用；置 false 无条件放行无审批流——**P1-MA2-020**）→ 年末门控（次年期间已创建阻止反结账 `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）→ **`:291` 直接 `setStatus(OPEN)`**（owner doc 间不一致：`period-close.md:186` 描述 `CLOSED_FINAL → CLOSING → OPEN` 三态迁移，而 `state-machine.md:153/182/222` 描述一步 `CLOSED_FINAL → OPEN`——代码与 state-machine.md 一致，与 period-close.md 不一致；本审计须复核两 owner doc 间裁决）→ 红冲本期 PERIOD_CLOSE/EXCHANGE_GAIN_LOSS/PROFIT_TO_RETAINED_EARNINGS 凭证 + 条件红冲折旧 → `reopenModules`。
  - `generateNextYearPeriods():219-267`：批量创建次年 1-12 月期间，**1 月置 OPEN / 2-12 月置 NEVER_OPENED**（NEVER_OPENED→OPEN 的迁移路径 owner doc 未定义——运营如何开启 2-12 月？是否有 action？或仅靠人工改 DB？）。
- **per-module 关账状态**（`ErpFinAccountingPeriodStatus`，`app-erp-finance.orm.xml:669-673`）：`arStatus`/`apStatus`/`invStatus`/`glStatus`/`assetStatus`（dict `erp-fin/module-close-status`：OPEN/CLOSING/CLOSED）按 `periodId` 关联；`advanceModule`/`reopenModules` 驱动。`closePeriod` 同步一次性执行，per-module CLOSING 中间态同样无对外可见窗口（与 P2-MA2-025 同型）。
- **预算方案状态机轴**（`ErpFinBudgetScenario`，`app-erp-finance.orm.xml:1755`）：`docStatus` dict `erp-fin/budget-status`（**6 态**：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED — A2 新增 CLOSED）；`approveStatus`（共用 `erp-fin/approve-status`）；`controlLevel`（NONE/WARN/HARD）；`scenarioType`（ANNUAL/ROLLING/ADJUSTMENT）；A2 字段 `budgetGroupCode`/`parentScenarioId`/`closedAt`。
- **预算控制 BizModel**（`ErpFinBudgetScenarioBizModel` + `IErpFinBudgetControlBiz.check()` + `IErpFinBudgetCommitmentBiz.commit/release`）：`check()` 在 purchase/sales 审核事务内同步余量校验（PASS/WARNED/BLOCKED）；`commit`/`release` SYNC 同事务生成/红冲 COMMITMENT 凭证。HARD 控制下余量<0 抛 `NopException` 阻断审核。
- **预算滚动/结转引擎**（A2，plan 2026-07-21-1206-2）：`rollForward(scenarioId, newFiscalYear, strategy)`（源 APPROVED → 目标 DRAFT，3 策略 FIXED_PERCENTAGE/ZERO_BASED/INCREMENTAL）；`carryForward(scenarioId, targetScenarioId, rule)`（源 APPROVED + 目标 DRAFT + 源年度期间全 CLOSED 前置 → 源 docStatus=CLOSED 终态，4 规则 REMAINING_FULL/REMAINING_RATIO/USED_FULL/NONE）；写 RollforwardLog/CarryForwardLog。
- **承付会计**（A2 + plan 2026-07-24-1351-3 sales 扩展）：采购/销售订单 approve 生成 COMMITMENT 凭证（commit）；订单 reverseApprove/cancel + 发票 approve 红冲 COMMITMENT 凭证（release）；config-gated `erp-fin.budget-commitment-enabled`；release 容错对称性（plan 2026-07-26-0410-2 latent defect fix）。
- **测试覆盖**：服务层 `TestErpFinPeriodStateMachine`（OPEN→CLOSED→CLOSED_FINAL→OPEN 反结账 + 非法状态守卫）/`TestErpFinPeriodPreCheck`（阻断/提示模式）/`TestErpFinReverseClose`/`TestErpFinAnnualClose`（年度结转 + 次年期间已存在阻止反结账）/`TestErpFinAuxiliaryReconGate`/`TestErpFinDepreciationIntegration`/`TestErpFinModuleCloseOrder`（模块关账顺序）/`TestErpFinProfitLossClosing`/`TestErpFinBudgetIsolation`（BUDGET 凭证与 ACTUAL 隔离）；预算域 `TestErpFinBudgetRollforward`/`TestErpFinBudgetCarryForward`/`TestErpFinBudgetCommitment`/`TestErpFinBudgetControl`；浏览器层 `fin-period-close-wizard`（preCheck→closePeriod→finalizePeriod→reverseClose 全链 + 非法状态守卫）/`fin-budget-rollforward-carryforward`/`fin-commitment-accounting`。

**已登记的直指期间/预算状态机的 MA2 finding（本审计须复核其状态机行为）**：

- `P1-MA2-017`（todo MR1）：`auto-post-on-close` doc/code 默认值 + 语义双重偏离 + AR-AP/allowance 阻断分级不一致。**期间状态机 scope**：preCheck 阻断门控与期间 OPEN→CLOSING 迁移的前置关系。
- `P1-MA2-018`（todo MR1）：年初余额 populate 非累计。**期间状态机 scope**：年度结转分支（CLOSED→次年 OPEN 期间的年初余额写入正确性）。
- `P1-MA2-019`（todo MR1）：辅助账跨年对账作用域不匹配。**期间状态机 scope**：`assertAuxiliaryReconciles` 在 CLOSED 迁移前的门控作用域。
- `P1-MA2-020`（todo MR1）：反结账 approval kill-switch 无审批流。**期间状态机 scope**：**直接是 reverseClose 状态迁移的门控**——本审计复核 CLOSED_FINAL→OPEN 迁移是否有合法审批路径，或 owner doc `§反结账约束` 「管理员+审批」契约是否落空。
- `P1-MA2-021`（todo MR1，A2.5a 已复核凭证侧维持 P1）：CLOSED_FINAL 期间凭证锁定未实现。**期间状态机 scope**：**期间侧守卫**——CLOSED_FINAL 是否在凭证/预算/核销 mutation 前校验期间状态（`resolveOpenPeriod:507` 仅要求 OPEN，CLOSED_FINAL 凭证可被修改/红冲）。
- `P1-MA2-022`（todo MR1）：FX 重估无前期 reversal + 无期间过滤。**期间状态机 scope**：`ExchangeRevaluationService.revalueArAp` 在 GL 关账段执行，期间状态机时序是否正确（重估在 CLOSED 前执行）。
- `P2-MA2-025`（watch-only，A2.17）：`closePeriod:157-158` 连续 setStatus 无 flush，CLOSING 永不对外可见。**期间状态机 scope**：**直接是期间状态机的并发互斥语义缺陷**——CLOSING 态定义存在但运行时不可观测。
- `P2-MA1-019`（watch-only）：`ErpFinVoucherTemplateBizModel:95` 用 `LocalDate.now()`（与期间状态机无直接关联，但 VoucherTemplate 经预算/期间使用，顺手复核）。

**但从未做过一次覆盖会计期间状态机 + 预算方案状态机、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：期间状态机 5 态（dict）vs owner doc 4 态（缺 NEVER_OPENED）——NEVER_OPENED 是"未到期间"还是"待运营开启"？预算 6 态（dict）vs owner doc 5 态（A2 新增 CLOSED 是否在 state-machine 文档同步？budget.md:223 已记录但 state-machine.md 未含预算状态机章节）。per-module 关账状态（module-close-status OPEN/CLOSING/CLOSED）是否为期间状态机的子状态机——与期间主状态机的协调一致性。
- **转换完整性**：每个期间状态的所有传入/传出转换——OPEN 的入边（CLOSED→OPEN 反结账 / NEVER_OPENED→OPEN 运营开启 / 次年 1 月生成时置 OPEN）/ CLOSING 的入边出边（owner doc CLOSING→CLOSED_FINAL 与 CLOSING→OPEN 失败回退，代码实际 CLOSING→CLOSED 无回退路径——结账失败如何处理？`closePeriod` 抛异常时期间状态回退吗？）；预算 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED 的所有转换。
- **终端状态与恢复**：CLOSED_FINAL（终态，反结账可恢复）/ 预算 CLOSED（A2 终态"结转后不可再调整"——是否真终态，反结账源 Scenario 是否可回退）；CANCELLED 预算凭证红冲路径。
- **异常路径**：结账失败（成本核算/折旧/损益结转失败——`closePeriod` 抛异常时期间状态是否回退到 OPEN 还是悬挂 CLOSING？owner doc `§对象二 异常路径` 声明 CLOSING→OPEN 回退，代码无显式 try-catch+setStatus 回退）；反结账失败（红冲失败）；并发结账（P2-MA2-025）；预算控制 HARD 阻断。
- **可达性**：NEVER_OPENED 是否可达终态（2-12 月次年期间生成后置 NEVER_OPENED，如何到 OPEN/结账？是否有显式开启 action？还是只能经 DB？）；预算 CLOSED 是否只能经 carryForward 到达。
- **角色与权限**：closePeriod/finalizePeriod/reverseClose 绑定角色（reverseClose owner doc 要求管理员+审批——P1-MA2-020 kill-switch）；预算 submit/approve/reject/carryForward/rollForward 绑定角色；危险操作（反结账影响已出具报表 / HARD 预算阻断 / 预算 CLOSED 终态）。
- **外部依赖**：期间状态机与凭证状态机的耦合（凭证过账需期间 OPEN——P1-MA2-021 期间侧守卫；期间结账需凭证已过账——preCheck）；预算状态机与凭证状态机的耦合（预算 APPROVED 生成 BUDGET 凭证；COMMITMENT 凭证 commit/release）；折旧/成本/汇兑外部步骤（assets/inventory 域）在期间状态机迁移中的时序。
- **TODO/任务策略**：OPEN 月末是否产生财务员结账待办；CLOSING 失败回退是否产生待办（若代码无回退则失败悬挂）；预算 HARD 阻断是否产生待办。
- **场景演练**：(a) 月末结账快乐路径（OPEN→CLOSING→CLOSED→finalizePeriod→CLOSED_FINAL）；(b) 结账失败（成本异常——状态回退或悬挂？）；(c) 反结账调整（CLOSED_FINAL→kill-switch throw or OPEN→红冲→重新结账）；(d) 年度结转（12 月 CLOSED→次年期间创建→年初余额 populate）；(e) 预算审批（DRAFT→SUBMITTED→APPROVED→BUDGET 凭证）；(f) 预算结转（源 APPROVED+目标 DRAFT+年度 CLOSED→源 CLOSED）；(g) 预算滚动（源 APPROVED→目标 DRAFT）；(h) 承付 commit/release。
- **与设计文档一致性**：`state-machine.md §对象二` 4 态 vs dict 5 态；`§对象二` 迁移图 `CLOSING→CLOSED_FINAL` 直接 vs 代码 CLOSED 中间态 + finalizePeriod；`§反结账步骤2` `CLOSED_FINAL→CLOSING→OPEN` 三态 vs 代码一步到 OPEN；`state-machine.md` 无预算状态机章节（预算状态机散落在 budget.md）；period-close.md 8 步概念模型与代码 closePeriod 单次编排的差异。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（结账失败期间悬挂 CLOSING 致死锁 / NEVER_OPENED 期间无开启路径致次年 2-12 月不可记账 / 反结账 kill-switch 致 CLOSED_FINAL 死锁无合法恢复路径——P1-MA2-020 升级评估 / 期间侧 CLOSED_FINAL 凭证锁定完全缺失——P1-MA2-021 升级评估）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对**会计期间状态机**（OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL + per-module 关账子状态机）+ **预算方案状态机**（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED + 承付/滚动/结转扩展）做系统性业务审查，产出审计报告。**严格限定 A2.5b scope = 期间状态机 + 预算状态机**；会计凭证状态机归 A2.5a（done）、AR/AP 核销状态机归 A2.5c。
- 重点核验 9 个已识别控制点：(1) 状态定义清晰性（期间 dict 5 态 vs owner doc 4 态缺 NEVER_OPENED；预算 state-machine.md 无独立章节；per-module 子状态机）；(2) 转换完整性（CLOSED 中间态 + finalizePeriod 独立步骤 vs owner doc 迁移图；CLOSING 失败回退路径；NEVER_OPENED→OPEN 迁移）；(3) 终端与恢复（CLOSED_FINAL 反结账；预算 CLOSED 终态；CLOSED→CLOSED_FINAL 是否需显式 finalize）；(4) 异常路径（结账失败状态回退或悬挂；反结账红冲失败；并发结账）；(5) 可达性（NEVER_OPENED 期间开启路径；预算 CLOSED 仅经 carryForward）；(6) 角色权限（reverseClose kill-switch P1-MA2-020；HARD 预算阻断）；(7) 外部依赖（凭证/预算凭证与期间耦合；折旧/成本/汇兑外部步骤时序）；(8) TODO/任务策略（月末结账待办；CLOSING 失败悬挂）；(9) 场景演练（月末/年度/反结账/预算审批/结转/滚动/承付）。
- 复核已登记 finding 在期间/预算状态机运行时的行为影响：P1-MA2-017（preCheck 阻断门控）/ P1-MA2-018（年初余额 populate）/ P1-MA2-019（辅助账对账作用域）/ P1-MA2-020（反结账 kill-switch 升级评估）/ P1-MA2-021（期间侧 CLOSED_FINAL 凭证锁定升级评估）/ P1-MA2-022（FX 重估时序）/ P2-MA2-025（CLOSING 并发不可见），标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.x finance/期间状态机 + 预算状态机 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.5b 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.5a 会计凭证状态机 — done；本审计只复核**期间侧**对凭证的守卫（P1-MA2-021 期间侧），不做凭证状态机本身审查。
- **不**审计 A2.5c AR/AP 核销状态机 — AR/AP 辅助账项核销路径归 A2.5c。本审计只确认期间 CLOSED_FINAL 时核销是否被阻止（期间侧守卫）。
- **不**审计 A2.3 期末结账链路编排正确性 — done；本审计只确认这些链路驱动的**期间状态机迁移**正确性（OPEN→CLOSING→CLOSED 失败回退/悬挂等）。
- **不**审计 A2.4 库存核算三方对账 — done；本审计只确认 INV 模块关账（invStatus）在期间状态机中的时序。
- **不**审计 A4.1b finance 代码质量 — 期间/预算 Processor 代码质量（异常处理/N+1/索引）系统性审查归 A4.1b；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发结账/并发反结账/并发预算审批的 lost-update 风险归 A2.17；本审计只标注观察到的并发敏感点（P2-MA2-025 CLOSING 并发不可见、预算 HARD 控制竞态）。
- **不**审计 A2.16 预算 commitment 释放路径完整性 — roadmap 单独工作项（commit/release 路径覆盖完整性）；本审计只复核 commitment 凭证在预算状态机中的 commit/release 迁移正确性，不做 release 路径覆盖完整性审计。
- **不**审计 Non-Goal 子项（owner doc 已裁定）：银行存款外币重估细节（period-close.md 已落地）、多账套/合并报表年度结转（successor）、预算物化快照表（successor）、预算编制工作流（successor）、跨币种结转汇率差异（treasury successor）、报表多年度维度实施（frontend successor）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/state-machine.md §对象二`（期间状态机 4 态 + 反结账路径 — 权威，**需复核 NEVER_OPENED 缺失 + CLOSED 中间态 + 反结账一步到 OPEN 三处漂移**）；`docs/design/finance/period-close.md`（期末结账 8 步概念 + 反结账 8 步 + 年度结转 + per-module 关账 + 配置项 + 预算结转与期间状态机协调 §316-354）；`docs/design/finance/budget.md`（预算方案状态机 + 多年度 + 滚动 + 结转 + 承付会计 + sales 承付扩展 — 预算状态机权威，**state-machine.md 无独立预算章节，散落在此**）；`docs/design/finance/posting.md`（期间与凭证耦合约束）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.5b 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：会计/财务（期间状态机/反结账/年度结转/预算控制/承付会计）与 ORM 模型（`module-finance/model/*.orm.xml` 期间/预算字段/状态字典）是 ask-first **最高级别**保护区域。P0 即时修复若触及 `ErpFinAccountingPeriodProcessor`/`ErpFinBudgetScenarioBizModel`/`AnnualCloseService`/`ExchangeRevaluationService`/期间或预算状态字典/承付 SPI，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（`project-context.md §AI 阻塞条件`）。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 期间状态机 + 预算状态机系统性业务审查

Status: completed
Targets: `module-finance/erp-fin-service/.../service/entity/ErpFinAccountingPeriodBizModel.java`（closePeriod/finalizePeriod/reverseClose/preCheck）；`.../service/processor/ErpFinAccountingPeriodProcessor.java`（closePeriod:130-163/finalizePeriod:204-210/reverseClose:274-309/generateNextYearPeriods:219-267/closeAnnual:174-186/advanceModule/reopenModules/isReverseCloseApprovalRequired:701）；`.../service/annualclose/AnnualCloseService.java`（executeAnnualClose/populateNextYearOpening/assertAuxiliaryReconciles）；`.../service/entity/ErpFinBudgetScenarioBizModel.java`（submit/approve/reject/cancel/rollForward/carryForward）；`.../service/budget/*BizModel` + `IErpFinBudgetControlBiz` + `IErpFinBudgetCommitmentBiz`；`.../service/reconciliation/ExchangeRevaluationService.java`（revalueArAp GL 关账段时序）；`module-finance/model/app-erp-finance.orm.xml`（ErpFinAccountingPeriod/ErpFinAccountingPeriodStatus/ErpFinBudgetScenario 字段 + period-status/budget-status/module-close-status 字典）；`docs/design/finance/state-machine.md §对象二`+`period-close.md`+`budget.md`；服务层 `TestErpFinPeriodStateMachine`+`TestErpFinPeriodPreCheck`+`TestErpFinReverseClose`+`TestErpFinAnnualClose`+`TestErpFinAuxiliaryReconGate`+`TestErpFinModuleCloseOrder`+`TestErpFinBudgetIsolation`+`TestErpFinBudgetRollforward`+`TestErpFinBudgetCarryForward`+`TestErpFinBudgetCommitment`；浏览器层 `fin-period-close-wizard`+`fin-budget-rollforward-carryforward`+`fin-commitment-accounting`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P2-MA1-019 fromCode 异常 + LocalDate.now 已登记待 MR1）；A2.1-A2.4 done（P1-MA2-017/018/019/020/022 已登记供本审计从状态机角度复核）；A2.5a done（P1-MA2-021 CLOSED_FINAL 凭证锁定经 A2.5a 凭证侧复核维持 P1，供本审计复核期间侧守卫）

- [x] 维度「状态定义」：审查期间 5 态（OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL）语义清晰性——每个状态名是否清楚表达业务等待点；NEVER_OPENED 是"未到期间"还是"待运营开启"（owner doc `§对象二` 4 态未含 NEVER_OPENED，dict 含——漂移）；CLOSING 是否为真实等待点（代码 `:157-158` 连续 setStatus 致 CLOSING 运行时不可见，P2-MA2-025）；预算 6 态（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED）语义；per-module 关账状态（module-close-status OPEN/CLOSING/CLOSED）是否为期间主状态机的子状态机——协调一致性（主状态 CLOSED_FINAL 时 per-module 应全 CLOSED？）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：列出每个期间状态的所有传入/传出转换——OPEN 的入边（NEVER_OPENED→OPEN 运营开启 / 次年 1 月生成置 OPEN / CLOSED_FINAL→OPEN 反结账）/ CLOSING 的入出边（owner doc `CLOSING→CLOSED_FINAL` 与 `CLOSING→OPEN` 失败回退，代码实际 `CLOSING→CLOSED` 无回退路径——**结账失败如何处理**：`closePeriod` 抛异常时期间状态是否回退到 OPEN？无显式 try-catch+setStatus 回退则悬挂 CLOSING？）/ CLOSED（代码 closePeriod 直接到达 + finalizePeriod 前置要求）/ CLOSED_FINAL（finalizePeriod 直接到达）；预算 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED 所有转换；承付 commit/release 是否经预算状态机迁移还是独立凭证状态机。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：列出所有终端状态——CLOSED_FINAL（反结账可恢复 CLOSED_FINAL→OPEN，owner doc 要求管理员+审批 P1-MA2-020 kill-switch 升级评估）；预算 CLOSED（A2 终态"结转后不可再调整"，反结账源 Scenario 是否回退——budget.md:324 "已结转的源 Scenario status=CLOSED 不回退"）；CANCELLED 预算凭证红冲路径（APPROVED→CANCELLED 红冲原 BUDGET 凭证）；归档与活动期间是否可区分。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——结账失败（成本核算/折旧/损益结转/汇兑重估失败，`closePeriod` 抛异常时期间状态回退或悬挂 CLOSING——**重点核验是否有 try-catch+setStatus(OPEN) 回退**，owner doc `§对象二 异常路径` 声明 CLOSING→OPEN 回退）；反结账失败（红冲凭证失败、次年期间已存在阻止）；并发结账（P2-MA2-025 CLOSING 不可见）；预算 HARD 阻断（余量<0 抛 NopException）；预算 commitment release 重复触发（`ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 守卫）；幂等性（generateNextYearPeriods `period-generate-skip-existing` 策略）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：从初始状态每个状态是否可达——**重点 NEVER_OPENED**（次年 2-12 月生成置 NEVER_OPENED，如何到 OPEN？是否有显式开启 action `openPeriod`？还是仅靠 DB？若无可达性死锁）；CLOSED（只能经 closePeriod 到达）；CLOSED_FINAL（只能经 finalizePeriod 从 CLOSED 到达）；预算 CLOSED（只能经 carryForward 到达）；是否有死循环或不可达终态路径。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个转换绑定执行角色——closePeriod（财务员）/ finalizePeriod（财务员）/ reverseClose（**owner doc 要求管理员+审批，代码 kill-switch P1-MA2-020 升级评估**：是否破坏状态机合法恢复路径——CLOSED_FINAL 死锁无合法路径回到 OPEN）/ 预算 submit/approve（财务员/财务管理员）/ carryForward/rollForward（财务管理员）；危险操作（反结账影响已出具报表与税务申报 / HARD 预算阻断 / 预算 CLOSED 终态）；多角色冲突。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：期间状态机与凭证状态机的耦合（凭证过账需期间 OPEN——P1-MA2-021 期间侧守卫复核；期间结账需凭证已过账——preCheck）；预算状态机与凭证状态机的耦合（预算 APPROVED 生成 BUDGET 凭证；COMMITMENT 凭证 commit/release）；折旧（assets 域 `IErpAstDepreciationScheduleBiz`）/ 成本（inventory 域 `IErpInvCostingBiz`）/ 汇兑（`ExchangeRevaluationService`）外部步骤在期间状态机迁移中的时序（均在期间仍 OPEN 时执行，状态簿记 CLOSING→CLOSED 在最后）；外部步骤失败是否阻断状态迁移或悬挂。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：每个非终端状态是否产生正确类型待办——OPEN 月末是否产生财务员结账待办（`closing-reminder-days` 配置）；CLOSING 失败回退（若代码无回退则失败悬挂——是否产生待办）；预算 HARD 阻断是否产生待办（采购/销售审核被阻断的单据悬挂）；是否存在期望有人行动但不产生待办的状态（NEVER_OPENED 期间待运营开启——无待办则次年 2-12 月静默不可用）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 月末结账快乐路径（OPEN→closePeriod→CLOSING→CLOSED→finalizePeriod→CLOSED_FINAL）；(b) 结账失败（成本异常——状态回退 OPEN 或悬挂 CLOSING？）；(c) 反结账调整（CLOSED_FINAL→kill-switch throw [默认 config] 或 CLOSED_FINAL→OPEN [config 置 false]→红冲结转凭证→重新结账）；(d) 年度结转（12 月 CLOSED→辅助账对账→本年利润→未分配利润→次年期间创建→年初余额 populate）；(e) 次年期间开启（次年 2 月 NEVER_OPENED→OPEN——是否有 action？）；(f) 预算审批（DRAFT→SUBMITTED→APPROVED→BUDGET 凭证）；(g) 预算结转（源 APPROVED+目标 DRAFT+源年度全 CLOSED→carryForward→源 CLOSED+目标增补行+结转凭证）；(h) 预算滚动（源 APPROVED→rollForward→目标 DRAFT）；(i) 承付 commit/release（订单 approve→commit COMMITMENT 凭证 / 订单 cancel 或发票 approve→release 红冲）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md §对象二`/`period-close.md`/`budget.md` 是否有匹配的页面/API/权限/业务注释——**重点漂移**：(1) `§对象二` 4 态 vs dict 5 态（NEVER_OPENED 缺失）；(2) `§对象二` 迁移图 `CLOSING→CLOSED_FINAL`（结账成功）vs 代码 `CLOSING→CLOSED` + 独立 `finalizePeriod` `CLOSED→CLOSED_FINAL`；(3) `§反结账步骤2` `CLOSED_FINAL→CLOSING→OPEN` 三态 vs 代码一步到 OPEN；(4) `state-machine.md` 无预算方案状态机独立章节（散落在 budget.md：41/223/331）；(5) `period-close.md §期间控制` 4 态（OPEN/CLOSING/CLOSED/CLOSED_FINAL）vs dict 5 态；(6) budget.md:78 预算审批即过账 vs 实际实现是否经状态机迁移触发。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「期间状态机并发互斥（项目特定，P2-MA2-025 复核）」：核验 `closePeriod:157-158` 连续 setStatus(CLOSING) 再 setStatus(CLOSED) 无中间 flush——CLOSING 态运行时不可观测，并发结账同期间无法靠 CLOSING 态互斥（两事务均读到 OPEN 均进入结账）；是否有 `@Version` 乐观锁或其他互斥机制保护期间状态迁移；finalizePeriod/reverseClose 同型并发风险；per-module 关账状态并发（模块按序关账中并发同模块）；预算状态机并发（同 Scenario 并行 submit/approve）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「预算承付 commit/release 状态机（项目特定）」：核验 commitment 凭证 commit（订单 approve 后置 SYNC 同事务生成 COMMITMENT 凭证）/ release（订单 reverseApprove/cancel + 发票 approve 红冲 COMMITMENT 凭证）的迁移路径完整性——commit/release 是否经预算 Scenario 状态机迁移还是独立凭证状态机；release 容错对称性（采购 `ErpPurOrderProcessor.runCommitmentReleaseHook` 原 try-catch 缺失经 plan 2026-07-26-0410-2 fix，复核对称性是否保持）；sales 承付镜像（billType SALES_ORDER_COMMITMENT 派发）；release-on-receive-vs-invoice 业务规则（budget.md:260 在 invoice approve 释放非 receive approve）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 MA2 finding 期间/预算状态机角度：P1-MA2-017（preCheck 阻断门控与 OPEN→CLOSING 前置）/ P1-MA2-018（年度结转 CLOSED→次年 OPEN 年初余额 populate）/ P1-MA2-019（assertAuxiliaryReconciles 作用域在 CLOSED 迁移前）/ P1-MA2-020（reverseClose kill-switch **升级评估**：CLOSED_FINAL 死锁无合法恢复路径是否破坏状态机）/ P1-MA2-021（**期间侧 CLOSED_FINAL 凭证锁定升级评估**：期间状态机是否在凭证/预算/核销 mutation 前校验 CLOSED_FINAL）/ P1-MA2-022（ExchangeRevaluation 在 GL 关账段时序，期间仍 OPEN）/ P2-MA2-025（CLOSING 并发不可见交接 A2.17）。标注每项终态（仅治理缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（含：期间状态机状态图 + 预算方案状态机状态图 + per-module 子状态机图、各维度通过/失败裁决、9 控制点 PASS/FAIL、MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

- [x] 期间状态机（5 态）+ 预算方案状态机（6 态）+ per-module 关账子状态机（3 态）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 9 个已识别控制点（状态定义 / 转换完整性 / 终端与恢复 / 异常路径 / 可达性 / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度 + 2 项目特定维度（期间并发互斥 / 预算承付 commit/release）至少一句裁决（含「本维度无发现」）
- [x] MA2 finding 运行时影响复核结论已记录（含 P1-MA2-020 反结账 kill-switch 升级评估 + P1-MA2-021 期间侧 CLOSED_FINAL 锁定升级评估裁决）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 期间/预算状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.x finance/期间状态机 + 预算状态机行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（结账失败期间悬挂 CLOSING 致死锁 / NEVER_OPENED 期间无开启路径致次年 2-12 月不可记账 / 反结账 kill-switch 致 CLOSED_FINAL 死锁无合法恢复路径 [若 P1-MA2-020 升级] / 期间侧 CLOSED_FINAL 凭证锁定完全缺失 [若 P1-MA2-021 升级] / 预算状态机迁移破缺）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - **执行结论**：本审计零 P0。NEVER_OPENED→OPEN 迁移路径缺失（P1-MA2-033）经评估不构成 P0——次年期间 2-12 月仍可经手工 DB update 开启，不破坏既有业务路径（仅缺系统 action）；P1-MA2-020 反结账 kill-switch 经评估不升 P0——config=false 时合法路径开放（虽无审批流但路径存在），不构成 CLOSED_FINAL 死锁；P1-MA2-021 期间侧 CLOSED_FINAL 凭证锁定经评估不升 P0——业务路径 post/reverse 已守卫，仅直接 entity mutation 未守卫。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。注意：本审计对已登记 finding（P1-MA2-017/018/019/020/021/022 + P2-MA2-025）只复核状态机运行时影响不重复登记根因；若发现新 P1（如 NEVER_OPENED→OPEN 迁移缺失 / CLOSED 中间态 owner doc 漂移 / 结账失败无状态回退 / 预算状态机 owner doc 缺独立章节）按新 finding ID 登记。
      - Skill: none
      - **执行结论**：新登记 2 项 P1（P1-MA2-033 NEVER_OPENED→OPEN 迁移路径缺失 / P1-MA2-034 carryForward 不校验源年度全 CLOSED 前置）至 arm-index §P1 详细清单；已登记 finding（P1-MA2-017/018/019/020/021/022）经状态机运行时影响复核结论记录于审计报告 §5，无根因重复登记。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.x finance/期间状态机 + 预算状态机 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - **执行结论**：arm-index §报告清单新增本报告行（done）；arm-index §P1 汇总段补 A2.5b 段（新增 P1/P2/升级评估裁决）；arm-index §A2.5b 新增项章节已添加；arm-index §P2 汇总新增 P2-MA2-034/035 行；scope matrix §2.2 注记追加 A2.5b 段；scope matrix 表格「预算与承付」行 finance 列由 `❓S拆` 推进至 `⚠️P1`，「状态机正确性」行 finance 列维持 `⚠️P1`（A2.5a + A2.5b 合并覆盖完成）。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05bae1cc4ffey8mGC12lzq73k5`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`ErpFinAccountingPeriodProcessor` closePeriod:157-158 连续 setStatus(CLOSING)/setStatus(CLOSED) 无中间 flush ✓ / finalizePeriod:204-210 assertPeriodStatus(CLOSED)→setStatus(CLOSED_FINAL) ✓ / reverseClose:274-309 kill-switch `isReverseCloseApprovalRequired()`(默认 true):278-281 + :291 直接 setStatus(OPEN) ✓ / generateNextYearPeriods:260-261 1月=OPEN/2-12月=NEVER_OPENED ✓；dict erp-fin/period-status 5 态（含 NEVER_OPENED）✓ + erp-fin/budget-status 6 态（含 CLOSED）✓；`state-machine.md §对象二` 仅 4 态（无 NEVER_OPENED）✓ + 无预算独立章节 ✓ + 迁移图 CLOSING→CLOSED_FINAL 无 CLOSED 中间态 ✓；arm-index findings P1-MA2-017/018/019/020/021/022 + P2-MA2-025 + P2-MA1-019 全部存在 ✓；BizModel Facade 行号（closePeriod:43-44/finalizePeriod:49-50/reverseClose:55-56）✓。14 项检查清单全部 PASS（基线准确性/格式/结果表面/Item 类型/技能/反松弛/不可降级/范围——A2.5b 含期间+预算合理（commitment 跨两者+period CLOSED_FINAL 门控预算+carryForward 要求源年度全 CLOSED，roadmap S 级 2-4 片设计）/结束门控/退出标准）。**采纳的非阻塞精化**：reverseClose 三态描述从"代码偏离 owner doc"改为"period-close.md:186 三态 vs state-machine.md:153/182/222 一步态的 owner-doc 间不一致，代码与 state-machine.md 一致"（已应用至 Current Baseline）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。期间/预算状态机触及会计保护区域，P0 即时修复须额外人工确认。

- [x] 范围内行为完成（A2.5b 期间状态机 + 预算状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/period-close/budget owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.5a 会计凭证状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.5a done；本审计覆盖**期间侧**对凭证的守卫（P1-MA2-021 期间侧），凭证状态机本身（DRAFT/POSTED/CANCELLED + isReversed + postingType）归 A2.5a。
- Successor Required: `no`——A2.5a 已 done。

### A2.5c AR/AP 核销状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计只确认期间 CLOSED_FINAL 时核销是否被阻止（期间侧守卫），AR/AP 辅助账核销状态机归 A2.5c。
- Successor Required: `yes`——A2.5c 执行时复核。

### A2.16 预算 commitment 释放路径完整性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 单独工作项 A2.16 审计 commitment 释放路径完整性（release 覆盖所有触发场景）。本审计只复核 commitment 凭证在预算状态机中的 commit/release 迁移正确性。
- Successor Required: `yes`——A2.16 执行时复核。

### A4.1b finance 代码质量审计 — 预算/AR-AP/成本/期间

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做期间/预算状态机**业务正确性**审查；期间/预算 Processor 代码质量（异常处理类型/N+1/索引/辅助方法）系统性审查归 A4.1b。
- Successor Required: `yes`——A4.1b 执行时复核。

### A2.17 并发与乐观锁（并发结账/并发反结账/并发预算审批/CLOSING 不可见）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（P2-MA2-025 CLOSING 不可见、预算 HARD 控制竞态、finalizePeriod/reverseClose 并发），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### 银行存款外币重估细节 + 多账套/合并报表年度结转 + 预算物化快照 + 预算编制工作流 + 跨币种结转 + 报表多年度维度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定——银行存款外币重估已落地（period-close.md）；多账套/合并报表年度结转/预算物化快照/预算编制工作流/跨币种结转/报表多年度维度归 successor（budget.md:7 Deferred successor 清单）。
- Successor Required: `yes`——各 successor 触发条件满足时（如多公司合并报表上线/预算物化查询性能不达标/预算编制多级审批需求）。

## Closure

Status Note: A2.5b（期间状态机 + 预算方案状态机系统性业务审查）完成。审计报告 `docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` 产出（FAIL 裁决——零 P0 + 2 项新 P1 + 2 项新 P2，符合审计预期：审计报告 FAIL ≠ 计划失败）。期间状态机核心契约（5 态 + per-module 子状态机 + 反结账经事务回滚保证一致性）+ 预算状态机核心契约（6 态 + 承付 commit/release 独立凭证状态机）经证据确认。新发现 P1-MA2-033（NEVER_OPENED→OPEN 迁移路径缺失）+ P1-MA2-034（carryForward 不校验源年度全 CLOSED 前置）已登记 arm-index 待 MR1。已登记 finding 运行时影响复核无升级（P1-MA2-020/021 升级评估维持 P1 不升 P0；P1-MA2-017/018/019/022 仅治理缺陷；P2-MA2-025 交接 A2.17）。5 处并发敏感点交接 A2.17。

Closure Audit Evidence:

> **Closure Audit: PASS** (independent fresh-context audit by subagent `ses_05b9c99c0ffeN672zpCJ0YlgZK`, 2026-07-28)
>
> Verified against live repository: (1) Plan `completed` with all 31 checklist items `[x]` across Phase 1/2 + Exit Criteria + Closure Gates; (2) Audit report `docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` complete with period state machine 状态图/转换矩阵 (§1), budget state machine 状态图/转换矩阵 (§2), per-module 子状态机 (§1.3), 10 state-machine-business-review dimensions + 2 project-specific dimensions (§3.1-3.12), 9-control-point summary (§4), MA2 finding 复核表 covering P1-MA2-017/018/019/020/021/022 + P2-MA2-025 (§0.3/§5), and P1-MA2-020/021 upgrade-evaluation verdicts "维持 P1 不升 P0" (§3.3); (3) All 7 spot-checks confirmed factual accuracy — `ErpFinAccountingPeriodProcessor:157-158` consecutive setStatus no flush, `generateNextYearPeriods:260-261` 1月=OPEN/2-12月=NEVER_OPENED, `reverseClose` kill-switch:278-281 + one-step setStatus(OPEN):291, `ErpFinBudgetScenarioProcessor.validateCarryForwardPreconditions:305-325` does NOT check source-year CLOSED, `openPeriod` action genuinely absent from Java code, `app-erp-finance.orm.xml` period-status 5 states vs `state-machine.md §对象二` 4 states; (4) `arm-index.md` updated with report row + P1-MA2-033/034 + P2-MA2-034/035 + A2.5b section after A2.5a; (5) scope matrix §2.2 appended with A2.5b summary, 预算与承付 finance column `❓S拆`→`⚠️P1`; (6) Build green — `mvn test -pl module-finance/erp-fin-service -am`: BUILD SUCCESS, 286 tests, 0 failures/errors. Audit verdict FAIL (2 new P1 + 2 new P2, zero P0) is the correct audit outcome and does not block plan closure — findings are properly registered in arm-index for MR1.

Follow-up:

- P1-MA2-033 / P1-MA2-034 待 MR1 经 R1.0 展开机制转化为具体修复工作项行（不属本审计 plan scope）
- 5 处并发敏感点交接 A2.17（不属本审计 plan scope）
- P2-MA2-034 / P2-MA2-035 watch-only，MR1 顺手收敛（不属本审计 plan scope）
