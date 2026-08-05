# 2026-08-05-2330-3 rc-ma1-a1-38-cs-f2-sla-escalation 客服域 cs-F2 SLA 超时与升级需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.38（MA1 需求追踪矩阵审计 — cs-F2 SLA 超时与升级：nop-job 扫描 / ESCALATE 审计 / 通知 escalationUserId / 重复升级 L2-L3 / SLA 绩效）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.38
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.38 的 0.2 依赖）、`2026-08-05-2330-2-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`（cs-F1 工单生命周期同批 N=2，SLA 计时依赖工单状态迁移与 startDateTime 记录，F1 先于 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.38 给出 UC 清单 = `UC-CS-04`（1 UC），含 `use-cases.md:63` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 cs 域第二个 RC 切片（依赖 A1.37 cs-F1 工单生命周期前置——SLA 计时基于工单 startDateTime 与 deadlineDateTime）。

- **L1 需求契约（权威真相源）**：`docs/design/customer-service/use-cases.md`：
  - **UC-CS-04 SLA 超时与升级（SLA Breach & Escalation）**（`:63`）：触发=nop-job 定时扫描发现 deadlineDateTime 已过且工单未 RESOLVED；前置=工单处于 ASSIGNED/IN_PROGRESS 且已超 deadlineDateTime。流程：① **nop-job 扫描 erp_cs_ticket 表，条件 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now()`**；② **系统创建 ErpCsTicketAction(actionType=ESCALATE)**；③ **通知 slaPolicy.escalationUserId（客服经理）**；④ 客服经理评估决策——重新分派（状态保持 ASSIGNED，更改 assignedToId）/ **延长 deadline（系统管理员操作）→ 更新 deadlineDateTime**；⑤ 系统记录超时原因和升级处理记录。后置：升级记录可追溯，超时率纳入 SLA 绩效报表。
  - **UC-CS-04 异常（关键验收标准）**：**重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级。**
  - **L1 关键不变量**：① nop-job 扫描超时工单；② 创建 ESCALATE 审计 + 通知 escalationUserId；③ 重新分派 / **延长 deadline**；④ **重复升级（每 2h，最多 3 次，向总监升级）**；⑤ 超时率入 SLA 绩效报表。

- **L3 代码实现现状（实测）**——**单次 L1 升级 + 绩效报表完整，但重复升级/L2-L3/延长 deadline 大面积缺失（R1.28 幂等守卫结构性阻断）**：
  - **SLA 策略实体 `ErpCsSlaPolicy`（✅ L1 字段就绪，⚠️ L2/L3 字段缺失）**：`module-cs/model/app-erp-cs.orm.xml:258-298`——`resolveHours`(propId7 :268) / `escalationUserId`(propId10 :271, **仅 L1，BIGINT[long] 非 stdDomain=userId**) / `isWorkingDays`(propId9 :270)。**缺失字段**（sla.md §1.1 :24-26 声明但 ORM 无）：`secondEscalationUserId`(L2 通知目标) / `escalationDelayHours`(L1→L2 延迟) / `workingHourStart`/`workingHourEnd` / `isActive`。
  - **SLA 计算（deadline + 计时，✅ 完整）**：deadline 计算 `SlaDeadlineCalculator.calculate:35-63`（calendar-hours `:49-55` / working-days skip-weekend `:57-62`）；deadline 写入工单 `ErpCsTicketMatchAndAttachSlaProcessor.java:62-63`（+ 权益覆盖 `:101-108`）；SLA 计时 START `ErpCsTicketBizModel.java:129`（start() 首次 IN_PROGRESS 设 startDateTime——刻意不在创建时设，sla.md:344 注记）；SLA 计时 STOP `ErpCsTicketResolveProcessor.java:43-50`（duration 经 SlaDeadlineCalculator.minutesBetween + isSlaCompleted=(deadline==null||now≤deadline)）；**reopen 保留 startDateTime**（`ErpCsTicketReopenProcessor.java:42-44`，注释"恢复计时：保留原 startDateTime"——A2.14 证伪 P0 候选的行为）。
  - **UC-CS-04 流程① nop-job 扫描（✅ 单级实现，默认禁用）**：
    - Job bean `ErpCsSlaScanJob.java:36-49`（execute + in-bean cron 门 `resolveCronConfig():56-58` 读 `erp-cs.sla-scan-cron` 空=跳过）；bean 注册 `app-service.beans.xml:14`。
    - **Job 调度（.job.yaml，不在 module-cs）**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-sla-scan.job.yaml:1-10`——`cronExpr: "@cfg:nop.job.erp-cs-sla-scan.cron-expr|0 * * * * ?"`（每分钟）+ `enabled: "@cfg:nop.job.erp-cs-sla-scan.enabled|false"`（**默认禁用**）。
    - 查询过滤 `ErpCsTicketScanOverdueTicketsProcessor.java:54-58`：`status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now AND isSlaCompleted=false` ✅。
  - **UC-CS-04 流程② ESCALATE 审计（✅ 单次）**：`ErpCsTicketScanOverdueTicketsProcessor.java:70-71` `writeAction(ticket, ACTION_TYPE_ESCALATE, ...)`（`ErpCsConstants.java:37`）；查询过滤 `:54-58` ✅。**无独立 escalate() @BizMutation**——仅经 scanOverdueTickets（`ErpCsTicketBizModel.java:216-220`）可达。
  - **UC-CS-04 流程③ 通知（⚠️ 通知目标漂移）**：`ErpCsTicketScanOverdueTicketsProcessor.java:95-111` `notifySlaOverdue()`（config-gated `ErpCsConfigs.isSlaNotifyEnabled()`）——**context 置 `escalationUserId` = `ticket.getAssignedToId()`（`:104`），非 `slaPolicy.escalationUserId`**（MA2 报告 `2026-07-28-1020-...md:322` 残留风险注记）。
  - **UC-CS-04 流程④ 重新分派 / 延长 deadline（⚠️ 重新分派仅手工 / 延长 deadline 全缺）**：重新分派仅经手工 `assign()`（NEW 态，`ErpCsTicketBizModel.java:101-117`），非升级产物 + 无超时自动重新分派；**延长 deadline ❌ 全缺**——grep `extendDeadline`/`adjustDeadline`/`setDeadlineDateTime` 在 erp-cs-service 仅 matchAndAttach 写入命中，**无 extendDeadline 方法**（sla.md:172"延长 deadline（管理员手动延长）"未实现）。
  - **UC-CS-04 异常 重复升级/L2-L3（❌ 结构性不可实现——R1.28 幂等守卫阻断）**：**幂等守卫使重复升级不可能**——`ErpCsTicketScanOverdueTicketsProcessor.java:64-68,80-86` `hasEscalationAction(ticketId)` 查询任意 ESCALATE 审计行，存在则 `continue`。单测 `testScanOverdueTicketsIdempotentNoDuplicateEscalation`（`TestErpCsTicketSlaCsat.java:162-175`）**断言**第二次扫描仍仅 1 ESCALATE 行。**无 escalation 级别计数器**（grep `lastEscalationLevel`/`escalationCount` 零）；**无 L2/L3 通知目标**（`secondEscalationUserId` ORM 缺失）；**无 escalationDelayHours 定时器**（config `erp-cs.escalation-l1-to-l2-hours` sla.md:286 文档化但 `ErpCsConfigs.java` 无 reader）。
  - **UC-CS-04 后置 SLA 绩效报表（✅ 已实现）**：`ErpCsReportBizModel.buildTicketSlaCsatSummaryDataset():184-243`（聚合 slaCompletedCount `:196-197` + slaBreachedCount `:198-200` per ticketTypeId，行暴露两计数 `:234-235`；返回原始计数非百分比率）+ `ErpCsQualityDashboardBizModel.getDashboardKpi():66-122`（slaCompletionRate `:100-102,117` + slaBreachedCount `:115-116` over CLOSED tickets）。
  - **跨域 daoFor**：module-cs 生产代码零跨域 daoFor（同 A1.37 基线）。P1-MA1-022 不涉及 cs。

- **L4 测试证据现状**（`module-cs/erp-cs-service/src/test/java/`）：
  - `TestErpCsTicketSlaCsat.java`：`testScanOverdueTicketsCreatesEscalateAction:141-155`（单次 ESCALATE 断言）+ `testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175`（**断言至多一次**，显式引用 plan `2026-07-30-0841-2 R1.28 P1-MA2-086`）+ `testFindSlaWarnings:288-307` + `testCloseBreachedWithoutReasonRejected:241-270` + 全生命周期 `:64-101`。
  - `TestErpCsSlaScanJob.java`（3 @Test：cron-empty skip / cron-set delegation / execute() 签名）——**不测实际升级逻辑**（委托 stub CountingJob）。
  - `TestErpCsSlaNotification`（autotest cases：scanOverdueTriggersNotify / findSlaWarningsTriggersNotify / notifyDisabledSkipsDispatch）——仅通知派发。
  - E2E `tests/e2e/business-actions/cs-ticket.action.spec.ts`——6 态状态机 only，**无 SLA timing/ESCALATE/reopen 断言**。报告 E2E `tests/e2e/reports/cs-ticket-sla-csat.value.spec.ts`（token 断言）+ `cs-ticket-sla.smoke.spec.ts`（渲染冒烟）。
  - **⚠️ 测试缺口**：① 重复升级（每 2h/最多 3 次/总监升级）**零测试**（功能不存在）；② 延长 deadline **零测试**；③ 通知目标正确性（escalationUserId vs assignedToId）无负向断言。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：**cs 域 verdict `✅(A2.14✅)` zero P1 zero P0**——SLA 计时联动完整（候选 P0「计时恢复累加缺失」经证据证伪：reopen 保留 startDateTime）；cs SLA 升级 Job 评估为 PASS for L1 contract（`:224`："cs | SLA 超时未解决 | ESCALATE 审计 + 升级通知（cron 每分钟）| ✅"）；残留风险（`:115,584` 非结论）：reopen 不延长 deadline 故 RESOLVED 等待窗口计入下次 duration（更严解释归残留风险）。
  - **cs 相关既有 finding**：`P2-MA2-067`（cs NEW>1h/ASSIGNED>2h 滞留升级未实现 + findSlaWarnings 无 scheduler，watch-only 归 owner doc Deferred，`:352,539` + `arm-index.md:627`）；**`P1-MA2-086`**（10 cron job 并发重复副作用合并裁决，**resolved R1.28**——`erp-cs-sla-scan` 是最严重噪声案例，**R1.28 修复 = `hasEscalationAction` 幂等守卫** `ErpCsTicketScanOverdueTicketsProcessor.java:66-68`，**此修复是 cs-F2 缺口的直接成因**：去重 ESCALATE 行使重复/L2/总监升级结构性不可能）。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：复用 A2.14 SLA 计时联动 + L1 升级 PASS 已证实行为，只补需求视角差异（UC-CS-04"异常"重复升级条款 vs 单次幂等实现的契约矛盾 / 通知目标漂移 / 延长 deadline 缺失）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P2-MA2-067`（cs 滞留升级 watch-only）、`P1-MA2-086`（cron 并发幂等 resolved R1.28——**幂等守卫直接成因**）。cs 域**无既有 RC finding**（与 A1.37 同批为 cs 域首批 RC 切片）。本切片新 finding 续全仓 RC 序列（A1.37 同批 N=2 续编 P1-RC-054/055 + P2-RC-051，本切片续编 P1-RC-056+ / P2-RC-052+）。本切片须 grep arm-index cs sla/escalat/escalation/scan/overdue/deadline/extend/repeat/director 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10。本切片候选偏差（重复升级/延长 deadline/通知目标）属**代码逻辑**类（预授权——ScanOverdueTicketsProcessor 升级逻辑 + extendDeadline mutation + notify context 修正）。若修复触及 ORM（`secondEscalationUserId`/`escalationDelayHours`/`lastEscalationLevel` 字段）须 ask-first。

- **剩余差距**：A1.38 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.38 报告并登记 finding，解除 cs 域 SLA 切片证据缺口。

## Goals

- 产出 A1.38 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-38-cs-f2-sla-escalation.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-CS-04 逐条核验**每条验收标准**（完整枚举，§3）：nop-job 扫描 / ESCALATE 审计 / 通知 escalationUserId / 重新分派 / 延长 deadline / **重复升级（每 2h 最多 3 次向总监升级）** / SLA 绩效 全链逐条。
- 对候选缺口给出分级结论：①UC-CS-04 **"异常"重复升级/L2-L3 总监升级结构性不可实现**（`hasEscalationAction` 幂等守卫封顶 1 次 + 无 escalationCount/secondEscalationUserId/escalationDelayHours——R1.28[P1-MA2-086] 修复直接成因 + owner doc README.md:98/sla.md:346 标 Non-Goal 但**未调和 use-cases.md:80 文本**）倾向 **P1**（**§4 三判据关键裁决**——owner doc Non-Goal 标注是否经人工批准[i]plan-audit/[ii]AI 自标 ≠ 人工批准[methodology §4 line 168]/[iii]product-scope 裁剪；L1 异常条款是明确验收标准，owner doc 静默降级与 use-cases 直接矛盾）；②UC-CS-04 **延长 deadline 缺失**（grep extendDeadline 零 + sla.md:172"管理员手动延长"未实现）倾向 **P1/P2**；③UC-CS-04 **通知目标漂移**（notify context 用 assignedToId 非 slaPolicy.escalationUserId，MA2 已注记残留风险）倾向 **P2**；④UC-CS-04 nop-job 扫描 + ESCALATE 审计 + SLA 绩效 → 倾向**接受**（单级 L1 完整 + 强测）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编 P1-RC-056+ / P2-RC-052+）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/sla.md/README.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.37 cs-F1 工单生命周期 / A1.39 cs-F3 知识库 / A1.40 cs-F4 调查权益 独立 plan；A1.38 只覆盖 UC-CS-04）。
- **不复审工单创建/分派/解决/计时/知识库/调查/权益/目录**（UC-CS-01/02/03/11 属 A1.37 / UC-CS-05/06/07 属 A1.39 / UC-CS-08/09/10/12 属 A1.40；本切片仅核 SLA 超时与升级）。
- **不重审 P2-MA2-067 cs SLA 计时行为 / P1-MA2-086 cron 幂等**（§去重协议：P2-MA2-067 watch-only 复用；P1-MA2-086 resolved R1.28——本切片引用其幂等守卫作为重复升级缺口直接成因，不重审并发维度）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.38 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.38 UC 锚点）+ `docs/design/customer-service/use-cases.md`（L1 真相源）+ `docs/design/customer-service/sla.md` + `README.md`（L2 设计参考，非真相源——**sla.md:346/README.md:98 Non-Goal 标注是 §4 三判据复核对象**）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.14 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-cs/erp-cs-service -Dtest=TestErpCsTicketSlaCsat,TestErpCsSlaScanJob`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-38-cs-f2-sla-escalation.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-CS-04 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:63` 验收标准原文（**含"异常"重复升级条款逐字引用**）；L2 引用 `sla.md §1.1/§升级/§4` + `README.md`（标注"设计参考，冲突以 L1 为准"——sla.md:346/README.md:98 Non-Goal 标注须 §4 三判据复核）；L3 引用 `ErpCsSlaScanJob`/`ErpCsTicketScanOverdueTicketsProcessor`/`ErpCsTicketBizModel`/`ErpCsReportBizModel`/`ErpCsQualityDashboardBizModel`/`ErpCsConfigs`/`ErpCsConstants`/`ErpCsSlaPolicy` ORM（含行号）；L4 引用 `TestErpCsTicketSlaCsat`#method + `TestErpCsSlaScanJob` + `TestErpCsSlaNotification`（注明断言强度）；L5 复用 A2.14（SLA 计时联动 PASS + L1 升级 PASS + P2-MA2-067 watch-only + P1-MA2-086 resolved R1.28 幂等守卫）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-CS-04 ①**nop-job 扫描**（`ErpCsSlaScanJob` + `erp-cs-sla-scan.job.yaml` cron 每分钟默认禁用 + 查询过滤 `status IN (ASSIGNED,IN_PROGRESS) AND deadlineDateTime<now AND isSlaCompleted=false` ✅）②**ESCALATE 审计**（`ScanOverdueTicketsProcessor:70-71` 单次 ✅）③**通知 escalationUserId**（`notifySlaOverdue():95-111` context 用 `ticket.assignedToId` 非 `slaPolicy.escalationUserId` → **⚠️ 目标漂移**）④**重新分派**（仅手工 `assign()` NEW 态非升级产物 + 无超时自动重新分派 → **⚠️**）⑤**延长 deadline**（grep `extendDeadline`/`adjustDeadline` 零 + sla.md:172 未实现 → **❌**）⑥**异常：重复升级（每 2h 最多 3 次向总监）**（`hasEscalationAction:64-68,80-86` 幂等守卫封顶 1 次 + 无 escalationCount/secondEscalationUserId/escalationDelayHours + 单测 `:162-175` 断言至多一次 → **❌ 结构性不可实现**）⑦**SLA 绩效报表**（`buildTicketSlaCsatSummaryDataset():184-243` slaCompletedCount/slaBreachedCount + `getDashboardKpi():66-122` slaCompletionRate/slaBreachedCount ✅）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对 UC-CS-04 给出符合性结论（取最高）：UC-CS-04 → nop-job 扫描 + ESCALATE 审计 + SLA 绩效 **接受**（单级 L1 完整 + 强测）+ 重复升级/L2-L3 缺失倾向 **P1**（**§4 三判据关键裁决**：UC-CS-04"异常"`use-cases.md:80` 逐字要求"重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级"——owner doc README.md:98"仅 L1 升级…多级升级链归 Non-Goal" + sla.md:346"L2/L3 多级升级链归 Non-Goal（ORM 无 secondEscalationUserId/escalationDelayHours）"是**静默降级**：判据[i]plan-audit——此 Non-Goal 标注无独立 plan-audit 通过记录；判据[ii]owner doc 显式 documented simplification 但**经人工批准痕迹**——git log 全 AI commits，AI 自标 Non-Goal ≠ 人工批准（methodology §4 line 168）；判据[iii]product-scope 裁剪——product-scope 未将多级升级列入"不在范围"。三判据均不成立 → Q4=(a) 强制实现。R1.28[P1-MA2-086] 幂等守卫是并发去重修复，其副作用封顶升级次数属设计张力需更深的升级级别计数器方案[如 lastEscalationLevel/escalationCount + escalationDelayHours 定时器 + secondEscalationUserId ORM]，**非退缩到方案 B**）+ 延长 deadline 缺失倾向 **P1/P2**（sla.md:172"管理员手动延长"未实现 + UC-CS-04 流程④明确要求）+ 通知目标漂移倾向 **P2**（MA2 已注记残留风险，行为上 assignedToId 经模板 ROLE 解析可能到达正确角色，watch-only）。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（**P1 项核 sla.md/README.md Non-Goal 标注的人工批准痕迹**——判据[ii]关键：grep git log commit author 确认是否人工批准）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-CS-04 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用（含异常条款）、L3 含行号、L4 注明断言强度、L5 标注复用 A2.14 来源
- [ ] UC-CS-04 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-③ 有明确分级；重复升级/延长 deadline/通知漂移各有 §4 三判据复核路径；**重复升级 P1 裁决须含 sla.md/README.md Non-Goal 标注的人工批准痕迹核查结论**；单级 L1 升级+绩效接受结论成立

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-38-cs-f2-sla-escalation.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` cs sla/escalat/escalation/scan/overdue/deadline/extend/repeat/director/second-escalation 同域同控制点后裁决——重复升级/L2-L3 缺失为**新根因**（既有 arm-index 无 RC finding 涉及 cs SLA 重复升级/总监升级需求契约维度——`P2-MA2-067` 是滞留升级 watch-only 状态机维度，`P1-MA2-086` 是 cron 并发幂等维度 resolved，两者不同控制点）→ 新建 `P1-RC-056`（UC-CS-04 重复升级/L2-L3 结构性不可实现 + R1.28 幂等守卫成因注记）；延长 deadline 缺失为**新根因** → 新建 `P2-RC-052`（与 sla.md:172 未实现衔接）；通知目标漂移**复用** A2.14 `:322` 残留风险注记（不新建，在既有注记追加 RC 交叉引用）→ 追加注记非新编号。与既有 RC 系列协调，续 A1.37 P1-RC-055/P2-RC-051。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）+ **R1.28[P1-MA2-086] 幂等守卫与重复升级缺口的设计张力注记**（修复须协调：升级级别计数器方案[lastEscalationLevel/escalationCount + escalationDelayHours 定时器]而非去除幂等守卫，避免重回 P1-MA2-086 并发噪声）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 notify context assignedToId 经模板 ROLE 解析是否实际到达 escalationUserId 角色 / SP-2 erp-cs-sla-scan enabled=true 时单实例每分钟扫描的实际升级频率与噪声 / SP-3 slaPolicy.escalationUserId 为 null 时 notifySlaOverdue 的降级行为 / SP-4 reopen 不延长 deadline 致 RESOLVED 等待窗口计入下次 duration 的实际违约率影响[复用 A2.14 残留风险]；每存疑点一行）。**P0 即时通道未触发**（本切片无 P0——重复升级缺失致告警不足但不破坏活跃数据/会计正确性 + 单级 L1 升级可达 + SLA 绩效可观测）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 cs SLA 计时联动 PASS + L1 升级 PASS + reopen 保留 startDateTime 证伪 P0 候选 + P2-MA2-067 watch-only + P1-MA2-086 resolved R1.28 幂等守卫），列明只补的需求视角差异（UC-CS-04 异常重复升级条款 vs 单次幂等实现契约矛盾 / 通知目标漂移 / 延长 deadline 缺失）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P1-RC-056` + `P2-RC-052` 入 RC 发现追踪分区；audit reports 表新增 A1.38 行；通知目标漂移在 A2.14 既有注记追加 RC 交叉引用。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding（P1-RC-056 + P2-RC-052）已写入 `arm-index.md`；静态存疑点清单已登记（SP-1~SP-4 供 A4.1/A4.2 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02fb10002ffetnyRjMvXdFPzrS，fresh session，未起草本计划）。17 项 load-bearing 引用经实仓复核 CONFIRMED TRUE：①A1.38 UC-CS-04 锚点 ✅ 一致；②`use-cases.md:80` 异常条款逐字"重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级"精确匹配；③`hasEscalationAction` 守卫 `:66` continue + `:80-86` 封顶 1 次；④单测 `testScanOverdueTicketsIdempotentNoDuplicateEscalation:163` assertEquals(1) 双断言至多一次；⑤sla.md:346 + README.md:98 Non-Goal 标注 verbatim 确认；⑥methodology §4 判据[ii]"AI 自标 ≠ 人工批准"(:162-168) 引用正确；⑦P1-MA2-086 resolved R1.28 = hasEscalationAction guard（arm-index:412 + plan 2026-07-30-0841-2 确认），cs-F2 缺口直接成因成立；⑧extendDeadline/adjustDeadline grep 零；⑨ErpCsSlaPolicy escalationUserId(propId10 BIGINT) 有 + secondEscalationUserId/escalationDelayHours/lastEscalationLevel/escalationCount grep 零；⑩erp-cs-sla-scan.job.yaml 默认 enabled=false；⑪SLA 绩效报表（buildTicketSlaCsatSummaryDataset + getDashboardKpi）存在；⑫A2.14 cs ✅ zero P1；⑬RC 编号严格顺序无冲突；⑭§4 判据[iii] product-scope 未列多级升级出范围；⑮A2.14:322 残留风险注记 + notify context 用 assignedToId(:104) 确认。scope（UC-CS-04 only，无 A1.37 creep）、anti-slack、§6 9-section + §4 三判据（Non-Goal 人工批准痕迹核查路径正确）+ §7 reuse（P2-MA2-067 watch-only / P1-MA2-086 resolved / A2.14:322 reuse 不新建）+ §去重协议全对齐。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.38 报告 9 段齐全 + UC-CS-04 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.38 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（重复升级/延长 deadline/通知目标）属**代码逻辑**类（预授权——ScanOverdueTicketsProcessor 升级级别逻辑 + extendDeadline mutation + notify context 修正）；重复升级修复若触及 ORM（secondEscalationUserId/escalationDelayHours/lastEscalationLevel）须 ask-first + 独立 plan-audit（须协调 R1.28 幂等守卫，避免重回 P1-MA2-086 并发噪声）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；重复升级修复须与 A1.37 cs-F1 拒绝路径/2h 升级同域协同[UC-CS-02 异常"2h 不响应升级"与本切片重复升级机制互补]）

## Closure

Status Note: <待执行完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理>
- Evidence: <待报告落盘后填写>
