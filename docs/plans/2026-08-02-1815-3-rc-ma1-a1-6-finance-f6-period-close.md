# 2026-08-02-1815-3 rc-ma1-a1-6-finance-f6-period-close finance-F6 期间与结账需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.6（MA1 需求追踪矩阵审计 — finance-F6 期间与结账：期末结账前置门禁 + 反结账）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.6
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.6 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，同 finance 审计范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.6 给出 UC 清单 = `UC-FIN-06/07`（2 UC），含 `use-cases.md:110` / `:129` 锚点。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md`：
  - UC-FIN-06 期末结账前置门禁（`:110`）：期间结账（→CLOSING）前置检查——若存在 `posted=false` 单据→拒绝（列出）；若存在未审核凭证→拒绝；若存在未核销应收应付（强制核销模式）→拒绝；若资产未折旧→拒绝；若成本未算→拒绝；全部通过→进入结账步骤（§结账8步）。
  - UC-FIN-07 反结账（`:129`）：反结账需高权限 + 审批；`CLOSED_FINAL → OPEN`；冲销结转凭证/折旧凭证/成本凭证；解锁期间内单据（可修改）；重新结账→CLOSED_FINAL；全程审计（记录反结账操作人/原因）。

- **L2 owner doc 设计参考**：`docs/design/finance/period-close.md`（§结账前置检查 `:25-58`、§期末结账步骤 8 步 `:60-111`、§期间控制状态机 `:147-168`、§反结账流程 `:170-228`、§反结账约束 `:223-228`、§已知简化—reverse-close approval `:321-325`、§期末结账向导 `:400-432`）。**注意**：owner doc 对 UC-FIN-06 的 AR/AP 处理为"提示"非"拒绝"（`:42-43`），与 L1 字面"强制核销模式→拒绝"存在偏离——按 §4 Q1，冲突以 L1 为准，L2 推定已向实现妥协。

- **L3 代码实现现状（实测，subagent 探查）**——功能**已实现**（Facade + R6.1 per-mutation Processor 拆分）：
  - Facade：`ErpFinAccountingPeriodBizModel.java`（preCheck:52 / closePeriod:58 / finalizePeriod:64 / reverseClose:70 / openPeriod:76 / generateNextYearPeriods:82）。共享编排 helper `ErpFinAccountingPeriodProcessor.java`（726 行）+ 6 per-mutation Processor（PreCheck/ClosePeriod/FinalizePeriod/ReverseClose/OpenPeriod/GenerateNextYearPeriods）。Pre-check report DTO `PeriodPreCheckReport.java`（hasIssues:93 / hasReminders:102 / hasAllowanceShortfall:110）。
  - UC-FIN-06 前置检查：`findUnpostedVoucherCodes:432-440`（未过账凭证，**hard block**）；跨域 posted=false 悬挂经 `findUnresolvedPostingExceptionKeys:471-477` + `findUnresolvedDepreciationSchedules:506-527`（assets）+ `findUnresolvedLandedCosts:530-549`（inv 到岸成本，**hard block**）；`findUnsettledArApCodes:442-462`（未核销 AR/AP）→ `PeriodPreCheckReport.hasReminders:102-105`（**实现为 reminder，非 hard block**——经 P1-MA2-017 resolved 的有意设计）；坏账准备缺口 `populateAllowanceCheck:99-118`（**hard block**，`ClosePeriodProcessor:52-56`）；资产未折旧/成本未算**间接**经 auto-execute 步骤（`runDepreciation:179-200` / `recloseInvCosts:208-232`，config-gated）+ 悬挂阻断，无显式 preCheck report 字段。**关键偏离**：UC-FIN-06 字面 5 条件全部"拒绝"，实现仅未过账凭证+过账异常+坏账缺口为 hard block，AR/AP 为 reminder——偏离 L1 但 owner-doc 已记录（§4 Q1 以 L1 为准，此为分歧须报告）。
  - 状态机：OPEN→CLOSING→CLOSED（`ClosePeriodProcessor:81-82`，CLOSING 瞬态不刷出，P2-MA2-025 watch-only）；CLOSED→CLOSED_FINAL（`FinalizePeriodProcessor:20-21`）；CLOSED_FINAL→OPEN（`ReverseCloseProcessor:39`，直接一步，kill-switch 门控）；NEVER_OPENED→OPEN（`OpenPeriodProcessor:20-21`，P1-MA2-033 resolved）；状态守卫 `assertPeriodStatus:574-581`；CLOSED_FINAL 凭证锁 `ErpFinVoucherBizModel.assertPeriodNotLocked:177-195`（P1-MA2-021 resolved）。
  - 结账8步（owner doc §60-111 概念，实现坍缩为同步 `closePeriod`）：过账检查✓/成本计算（config-gated，BATCH/LIFO/STANDARD 为 inventory successor）/折旧（config-gated，G3 分级）/费用摊销**缺失（Non-Goal 模块未落地）**/P&L 结转（`closeGlModule:163-171`→`profitLossClosingService.close`，FX 损益已含 P0-MA2-016 resolved）/结账凭证+试算平衡（试算快照 `populateTrialBalanceForAllSchemas:331-383`，无独立结账汇总凭证）/标记 CLOSED+锁+开下期✓/结账报表（nop-report Non-Goal）。模块结账顺序 AR→AP→INV→AST→GL（`ClosePeriodProcessor:67-71`，乱序抛 `ERR_MODULE_OUT_OF_ORDER`）。
  - UC-FIN-07 反结账：`ReverseCloseProcessor`——高权限=**仅 kill-switch**（`isReverseCloseApprovalRequired()` 默认 true 抛 `ERR_REVERSE_CLOSE_APPROVAL_REQUIRED` `:26-29`，**无 @BizAuth/@RolesAllowed**，全仓 grep 0 命中）；审批=**缺失（documented simplification，P1-MA2-020/P1-MA3-036 resolved 为 kill-switch successor）**；CLOSED_FINAL→OPEN `:39`；冲销结转凭证（`reverseCloseVoucher:42-43`，前缀 `PERIOD-CLOSE-` + PERIOD_CLOSE）/冲销 FX 重估（`:44-45`，`FX-REVAL-` + EXCHANGE_GAIN_LOSS）/冲销年末结转（`:46-49`，isYearEnd，`ANNUAL-CLOSE-` + PROFIT_TO_RETAINED_EARNINGS）/冲销折旧（`:50-52` + `reverseDepreciation:238-263`，config-gated）；冲销成本凭证=**缺失（Non-Goal，INV costing 无 finance 侧期间凭证可冲）**；解锁单据（`reopenModules:278-284` 重置 per-module 状态为 OPEN，凭证锁随期间 OPEN 解除，无显式单据解锁）；重新结账→CLOSED_FINAL（幂等，`TestErpFinReverseClose:49-50`）；年末反结账阻断（`:32-36` 抛 `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）。**关键缺口（疑似未登记 finding）**：UC-FIN-07"全程审计（记录反结账操作人/原因）"——`reverseClose(periodId, context)` **无 reason 参数**，无 `ReverseCloseLog` 实体，`ErpFinAccountingPeriod` ORM（`:655-694`）**无 reversedBy/reverseCloseReason 列**（仅 `closedBy/closedAt:670-671`），全仓 grep 0 命中——**该需求未实现且 arm-index 无追踪 finding**。高权限无运行时角色强制（P1-MA3-046，经 MA6 A6.1/A6.2 确认全域敏感动作缺运行时权限保护）。

- **L4 测试证据现状**：`TestErpFinPeriodPreCheck`（3，列 issues+clean+blocking close 拒绝）、`TestErpFinPeriodStateMachine`（5，正反向+非法迁移拒绝+非阻塞 close+NEVER_OPENED→OPEN）、`TestErpFinPeriodCloseEndToEnd`（1，全链 preCheck→close→FX+PL 凭证→finalize→reverse→re-close）、`TestErpFinReverseClose`（1，余额还原+GL/AST 模块重开+PL 红冲凭证存在+re-close）、`TestErpFinVoucherPeriodLock`（4，CLOSED/CLOSED_FINAL 过账/冲销阻断）。**注意**：反结账审批 kill-switch 默认阻断路径**无测试**（测试用 `period-close-end-to-end-test.yaml` 关闭审批）；反结账审计轨迹（操作人/原因）**无测试**（功能缺失）；年末反结账阻断（MA2 报告 §3.12 称未覆盖，`TestErpFinAnnualClose` 待核存在性）；AR/AP reminder（非阻断）路径无显式测试；坏账缺口 hard block 无测试；跨域悬挂阻断无测试；折旧/成本失败传播（G3 rethrow）无测试（P1-MA4-004 resolved，测试 follow-up P1-MA4-005 MR2）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`（217 行）= THE period-close 审计**：多维 E2E。发现 **1 P0**（P0-MA2-016 FX 损益未结转，**resolved**）+ **6 P1**（P1-MA2-017/018/019/020/021/022 **全 resolved**）+ 3 P2 watch-only（P2-MA2-023/024/025）。建立 7 控制点；模块结账顺序+P&L 平衡+逐凭证 FX 平衡 PASS。
  - **`docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（529+ 行，A2.5b）**：状态机审查。发现 **2 P1**（P1-MA2-033 NEVER_OPENED→OPEN 缺失 resolved / P1-MA2-034 carryForward 年初前置，budget 侧）+ 2 P2。确认 @BizMutation 事务回滚一致性。
  - `docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b，含 `ErpFinAccountingPeriodProcessor` + `ProfitLossClosingService` + `AnnualCloseService`；P1-MA4-004 跨域异常吞噬 resolved R1.16 / P1-MA4-005 测试有效性 resolved R2.10 / P2-MA4-003）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实的结账/状态机行为，只补"需求契约↔行为"差异（反结账审计轨迹缺失 / 高权限无角色强制 / AR/AP reminder 偏离 L1 / resolved finding HEAD 复核等）。注意 MA2 报告行号为 pre-R6.1 单体 Processor 时代，当前代码已拆分为 per-mutation Processor（R6.1），行号需重新核验。

- **arm-index 既有 finding 衔接**：period-close 相关——`P0-MA2-016`（FX 损益结转，done）、`P1-MA2-017`（auto-post-on-close 分级，resolved）、`P1-MA2-018`（年初余额非累积，resolved documented simplification，GL 余额引擎 successor）、`P1-MA2-019`（辅助账对账范围，resolved）、`P1-MA2-020`（反结账审批 kill-switch，resolved documented simplification，审批流 successor 仍 open）、`P1-MA2-021`（CLOSED_FINAL 凭证锁，resolved）、`P1-MA2-022`（FX 重估跨期，resolved documented simplification）、`P1-MA2-033`（NEVER_OPENED→OPEN，resolved）、`P1-MA3-036`（反结账审批 doc↔code，done R2.5）、`P1-MA4-004/005`（resolved）、`P2-MA2-023/024/025`（watch-only）。**本切片新发现的缺口**（反结账审计轨迹操作人/原因缺失且无 finding、高权限无角色强制）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及会计过账逻辑（P&L 结转/FX 重估/折旧凭证冲销）或 ORM 结构（反结账审计列/ReverseCloseLog 实体）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.6 切片的五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.6 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.6 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-6-finance-f6-period-close.md`，含方法论 §6 **9 段全部内容**：①UC-FIN-06/07 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 per-mutation Processor 调用链）③测试证据（注明断言强度）④运行时行为证据（复用 MA2/E2E，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-FIN-06 五前置条件 + UC-FIN-07 反结账要求（高权限/审批/状态迁移/冲销三类凭证/解锁/重新结账/审计轨迹），各一矩阵行。
- 对候选缺口/偏离给出分级结论：反结账审计轨迹（操作人/原因）缺失、高权限无运行时角色强制、AR/AP reminder 偏离 L1 字面"拒绝"、费用摊销步骤缺失、成本凭证冲销缺失、反结账审批流 successor——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / `period-close.md` 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.1-A1.5 done/进行中；A1.7-A1.51 各自独立 plan；A1.6 只覆盖 UC-FIN-06/07）。
- **不重跑既有 MA2 行为审计**（§去重协议：A2.3/A2.5b 已证实行为直接引用，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.6 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.6 UC 锚点）+ `docs/design/finance/use-cases.md`（L1 真相源）+ `docs/design/finance/period-close.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用既有 A2.3/A2.5b 审计 + E2E recordings（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinPeriod*,TestErpFinReverseClose,TestErpFinVoucherPeriodLock`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-6-finance-f6-period-close.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-FIN-06/07 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:110/:129` 验收标准原文（禁止转述）；L2 引用 `period-close.md` 对应 section（标注"设计参考，冲突以 L1 为准"，注意 AR/AP"提示 vs 拒绝"偏离 `:42-43`、反结账审批 kill-switch successor `:321-325`）；L3 引用 `module-finance/erp-fin-service/.../processor/<file>:line`（含 per-mutation Processor + `ErpFinAccountingPeriodProcessor` helper + `ErpFinVoucherBizModel.assertPeriodNotLocked` 调用链列全，注意 R6.1 行号偏移）；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.3/A2.5b 已证实行为 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-FIN-06——①未过账凭证 hard block（✓）；②未审核凭证（归①）；③未核销 AR/AP 实现为 reminder 非"拒绝"（偏离 L1，P1-MA2-017 resolved 有意设计）；④资产未折旧（间接 auto-execute，无显式 preCheck 字段）；⑤成本未算（间接，无显式字段）；UC-FIN-07——⑥高权限（仅 kill-switch，无 @BizAuth 角色强制）；⑦审批（缺失，documented simplification successor）；⑧状态迁移 CLOSED_FINAL→OPEN（✓）；⑨冲销结转/折旧/FX 凭证（✓）；冲销成本凭证（缺失 Non-Goal）；⑩解锁单据（间接）；⑪重新结账（✓ 幂等）；⑫**全程审计（操作人/原因）——缺失且无 finding，ORM 无 reversedBy/reverseCloseReason 列，reverseClose 无 reason 参数**。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**：对 arm-index 标记 resolved 的 period-close finding（P0-MA2-016 / P1-MA2-017/018/019/020/021/022/033 / P1-MA3-036 / P1-MA4-004/005）在当前 HEAD 代码实际落地（注意 R6.1 per-mutation 拆分导致行号偏移，按逻辑而非行号核验），逐条记录复核结论（已落地 / 回退 / 部分落地 / documented simplification 仍 open successor）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：反结账审计轨迹缺失（⑫）若为"安全审计要求未实现"按 §2 定级（P1 候选，会计/数据安全类）；高权限无角色强制（⑥）若为"安全门控缺失"（P1 候选）；AR/AP reminder 偏离（③）为 owner-doc 裁决，记录分歧但倾向接受（L2 已记录有意设计）；其余按实测定级。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-FIN-06/07 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号（R6.1 后）、L4 注明断言强度、L5 标注复用 A2.3/A2.5b 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-⑫有明确分级（非悬空"待查"）；resolved finding HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-6-finance-f6-period-close.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` period-close 同域同控制点（如 P1-MA2-020 反结账审批 successor、P1-MA3-046 全域权限保护、P1-MA2-017 AR/AP 分级等行）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：反结账审计轨迹缺失若 arm-index 无同控制点 finding 则新建；高权限角色强制若 P1-MA3-046 已覆盖则复用并追加 RC 交叉引用。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 `2026-07-27-1949-arm-ma2-period-close-e2e.md`（A2.3）/ `...-period-budget-state-machine.md`（A2.5b）等已证实行为，列明本切片只补的需求视角差异（反结账审计轨迹缺失 / 高权限无角色强制 / AR/AP reminder 偏离 L1 / resolved finding HEAD 复核结论 / R6.1 行号偏移说明等）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 ses_03e0411bcffe8WS0Q55AAOKKkn，fresh session，未起草本计划）。10 项检查 A-J 全 PASS：格式完整、Deps 正确（A1.6 Deps=0.2 done）、单结果表面、Baseline 准确（逐项实测命中：UC-FIN-06/07 锚点 + period-close.md 各 section / R6.1 per-mutation Processor 拆分（6 + 共享 helper）/ 反结账审计轨迹缺失真实且未登记（reverseCloseReason/reversedBy/ReverseCloseLog 全仓 0 命中，仅 plan 文件自身命中）/ ORM 仅 closedBy/closedAt 无反结账审计列 / @BizAuth/@RolesAllowed 0 命中 / AR/AP hasReminders 提示非阻断 / A2.3+A2.5b+A4.1b 报告存在 / arm-index 12 个 resolved finding 状态 / R6.1 行号偏移说明正确），UC 覆盖 UC-FIN-06（5 前置条件）+ UC-FIN-07（7 反结账要求含审计轨迹）全枚举，方法论 §1-§10 + §去重对齐，§7 grep-before-create 显式考虑 P1-MA3-046 复用 + 反结账审计轨迹新 finding grep，L1-vs-L2 AR/AP 分歧按 Q1 以 L1 为准正确框定，resolved finding HEAD 复核合理，反松弛合规，Closure Gates audit-only 有据，无范围蔓延，item typing 合规，Skill 就绪。无阻塞。Non-blocking（已吸收 + 已评估）：①P1-MA3-046 标注（"MA6 P1-MA3-046"措辞不精确——P1-MA3-046 源自 MA3 经 MA6 确认）→**已修订**为"P1-MA3-046，经 MA6 A6.1/A6.2 确认"；②Facade 行号无 R6.1 caveat（执行时复验，Phase 1 已立"按逻辑核验"纪律）；③TestErpFinAnnualClose 存在性"待核"（baseline 恰当谦逊，L4 列执行时裁决）；④AR/AP"倾向接受"预判（可辩护，reporter 执行时须显式论证 Q4 是否重开）。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + resolved finding HEAD 复核 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。（§8 含 checker 实测记录，但 checker 是 reporter 非门控；门控真值在 CI workflow。）

- [x] 范围内行为完成：A1.6 报告 9 段齐全 + UC-FIN-06/07 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.6 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

> Closure Audit Record: 独立子代理 `ses_03d6198c9ffek420xRyqiFb4Cn`（fresh session，未起草本计划/未执行 Phase 1-2）于 2026-08-02 完成 closure audit。Verdict: **pass**。9 项核验全 PASS：plan 执行完整性、报告 9 段完整性、UC 完整枚举（UC-FIN-06 PC-1..PC-6 + UC-FIN-07 RC-1..RC-9）、L1 逐字引用、findings grep-before-create（P1-MA3-046 复用 + 3 新建均有 arm-index 比对依据）、11 项 resolved finding HEAD 复核（独立抽检 4 项准确）、7 项关键证据实测核验（reverseClose 无 reason 参数 / ORM 无反结账审计列 / @BizAuth 零命中 / testReverseCloseApprovalBlocked 测试存在 / ProfitLossClosingService 仅排除 PERIOD_CLOSE / assertPeriodNotLocked 存在 / OpenPeriodProcessor 存在）、roadmap A1.6 done、真相源冻结（git status use-cases.md/period-close.md/product-scope.md 干净）。无阻塞 issue。

## Closure

Status Note: A1.6 审计切片的两阶段（五级追踪矩阵填充 + finding 登记/完整性自检）均已完成，所有执行项 `[x]`、所有阶段退出标准 `[x]`、所有 Closure Gates `[x]`。审计报告 `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md`（544 行，9 段齐全）已落盘；arm-index 新 finding 已登记；本计划为只读审计无代码变更故无构建/测试验证义务。独立结束审计由独立子代理（fresh session）执行并 verdict=pass，执行者未自我审计。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_03d6198c9ffek420xRyqiFb4Cn`（fresh session，未起草本计划/未执行 Phase 1-2），独立结束审计于 2026-08-02 完成
- Evidence: Verdict=**pass**；9 项核验全 PASS（plan 执行完整性、报告 9 段完整性、UC 完整枚举 UC-FIN-06 PC-1..PC-6 + UC-FIN-07 RC-1..RC-9、L1 逐字引用、findings grep-before-create 含 P1-MA3-046 复用 + 3 新建均有 arm-index 比对、11 项 resolved finding HEAD 复核独立抽检 4 项准确、7 项关键证据实测核验、roadmap A1.6 done、真相源冻结 git status 干净）
- Audit Report: `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md`（544 行，方法论 §6 9 段齐全：UC 契约/实现证据/测试证据/运行时行为/五级矩阵/finding 裁决/存疑点/过程自检/MA2 差异增量）
- arm-index 登记证据: 新建 finding `P1-RC-006`（UC-FIN-07 RC-9 反结账审计轨迹操作人/原因完全缺失）/ `P2-RC-006`（UC-FIN-06 PC-3 AR/AP reminder 偏离 L1 字面"拒绝"）/ `P2-RC-007`（UC-FIN-07 RC-6 反结账成本凭证冲销缺失 Non-Goal）已写入 `docs/audits/arm-index.md` MA1 finding 区（`:98-100`）；既有 `P1-MA3-046`（UC-FIN-07 RC-1 高权限无角色强制，复用非新建）追加 RC 交叉引用注记（`:110`）
- 真相源冻结: `git status` 显示 `use-cases.md`/`period-close.md`/`product-scope.md` 干净，符合方法论 §9 冻结条款

Follow-up:

- finding 修复经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）实施，触及会计过账或 ORM 结构的修复行须 ask-first + 独立 plan-audit（详见 `## Deferred But Adjudicated`）

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（P&L 结转/FX 重估/折旧凭证冲销）或 ORM 结构（反结账审计列/ReverseCloseLog 实体）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）
