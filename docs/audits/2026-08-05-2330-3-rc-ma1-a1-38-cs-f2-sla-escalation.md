# A1.38 cs-F2 SLA 超时与升级 需求-实现符合性审计报告（MA1 RC）

> 里程碑：MA1（requirement-compliance mission，Work Item A1.38）
> 域/功能切片：customer-service / SLA 超时与升级（nop-job 扫描 / ESCALATE 审计 / escalationUserId 通知 / 重新分派 / 延长 deadline / 重复升级 L2-L3 / SLA 绩效）
> UC 清单：UC-CS-04（1 UC）
> 来源：plan `docs/plans/2026-08-05-2330-3-rc-ma1-a1-38-cs-f2-sla-escalation.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.38 → UC-CS-04（✅ 一致，无基线分歧 D-xx）
> 审计类型：只读审计（无生产代码/ORM/api.xml/view.xml/真相源变更）
> 产出时间：2026-08-05

---

## 9. 与 MA2 报告差异增量声明（前置）

本切片报告与既有 MA2 行为审计报告 `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 cs Ticket/SLA：6 态全迁移 + SLA 计时联动 PASS + L1 升级 PASS + `P2-MA2-067` watch-only + `P1-MA2-086` resolved R1.28 幂等守卫）的差异增量，按 §去重协议声明：

- **复用 A2.14 已证实行为作为 L5 既有证据**（不重新核实行为本身）：
  - **cs SLA 升级 cron Job L1 升级 PASS**（A2.14 §cs `:224`："cs | SLA 超时未解决 | ESCALATE 审计 + 升级通知（cron 每分钟）| ✅"）——`ErpCsSlaScanJob` + `erp-cs-sla-scan.job.yaml`（cron 每分钟，默认 disabled）+ `scanOverdueTickets` 单次 ESCALATE 审计 + 通知派发。
  - **cs SLA 计时联动完整 PASS**（A2.14 §cs）：startDateTime=首次进入 IN_PROGRESS（`ErpCsTicketBizModel.start:129`）+ isSlaCompleted=(deadline==null‖now≤deadline)（`ErpCsTicketResolveProcessor:48-50`）+ **reopen 保留 startDateTime 时长累加重算**（`ErpCsTicketReopenProcessor` 不清 startDateTime，resolve 时 duration 重算）——**候选 P0「SLA 计时恢复累加缺失」经证据证伪**。
  - **关闭超时工单须 remark 守卫 PASS**（A2.14 §cs）：`close:152-157` breach 工单须 remark 注明超时原因，缺失抛 `ERR_TICKET_CLOSE_BREACHED_NO_REASON`——L1 后置「超时率纳入 SLA 绩效报表」的 close 守卫成立。
  - **`P2-MA2-067`（cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings 无 scheduler）watch-only**：UC-CS-04 流程① nop-job 扫描已落实，但 NEW>1h / ASSIGNED>2h 滞留升级规则缺失（不同控制点）。
  - **`P1-MA2-086`（10 cron job 并发重复副作用合并裁决）resolved R1.28**——`erp-cs-sla-scan` 是最严重噪声案例（每分钟重复 ESCALATE 审计 + 通知），R1.28 修复 = `hasEscalationAction` 幂等守卫 `ErpCsTicketScanOverdueTicketsProcessor.java:64-68,80-86`。**此修复是本切片 UC-CS-04「异常」重复升级 / L2-L3 总监升级缺口的直接成因**——幂等守卫封顶升级次数为 1，使重复升级/L2/总监升级结构性不可实现。本切片引用其幂等守卫作为重复升级缺口直接成因，**不重审并发维度**（§去重协议）。
  - **A2.14 §cs `:322` 残留风险注记**：notifySlaOverdue 上下文用 `ticket.assignedToId` 置于 `escalationUserId` 键（`ErpCsTicketBizModel.java:325` + `ErpCsTicketScanOverdueTicketsProcessor.java:104`），通知模板经 ROLE resolver 解析实际接收人。**本切片 UC-CS-04 流程③ 通知目标漂移控制点复用此注记**——MA2 已注记残留风险，行为上 `assignedToId` 经模板 ROLE 解析可能到达正确角色。
- **本切片只补需求视角差异**（use-case 验收标准 vs 实际行为）：
  - **UC-CS-04 流程⑤ 延长 deadline 缺失**（grep `extendDeadline|adjustDeadline` 跨 main 零命中 + sla.md:172 "管理员手动延长" 未实现）
  - **UC-CS-04 "异常" 重复升级（每 2h 最多 3 次向总监升级）结构性不可实现**（`hasEscalationAction` 幂等守卫封顶 1 次 + 无 `escalationCount`/`lastEscalationLevel`/`secondEscalationUserId`/`escalationDelayHours` 字段 + 单测 `testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` 断言至多一次——R1.28[P1-MA2-086] 修复直接成因）
  - **UC-CS-04 流程③ 通知目标漂移**（notifySlaOverdue context 用 `assignedToId` 非 `slaPolicy.escalationUserId`）

本切片不重审 A2.14 已证实的 SLA 计时联动 + L1 升级 PASS 行为，仅从 L1 验收标准视角补齐需求契约↔行为差异。

---

## 1. 需求契约原文（L1，逐字引用，禁止转述）

> 真相源：`docs/design/customer-service/use-cases.md`（权威功能契约）。L2 owner doc（`sla.md` / `README.md` / `state-machine.md`）为设计参考，冲突以 L1 为准（§4 Q1）。

### UC-CS-04 SLA 超时与升级（`use-cases.md:63`）

```
**触发条件** nop-job 定时扫描发现 deadlineDateTime 已过且工单未 RESOLVED。

**前置条件** 工单处于 ASSIGNED/IN_PROGRESS 状态且已超 deadlineDateTime。

**流程** 
1. nop-job 扫描 erp_cs_ticket 表，条件 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now()`。
2. 系统创建 ErpCsTicketAction（actionType=ESCALATE）。
3. 通知 slaPolicy.escalationUserId（客服经理）。
4. 客服经理评估超时原因，决策 
   - 重新分派 → 状态保持 ASSIGNED，更改 assignedToId。
   - 延长 deadline（系统管理员操作）→ 更新 deadlineDateTime。
5. 系统记录超时原因和升级处理记录。

**后置条件** 升级记录可追溯，超时率纳入 SLA 绩效报表。

**异常** 重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级。
```

**验收标准逐条枚举**：①触发：nop-job 定时扫描发现 deadlineDateTime 已过且工单未 RESOLVED ②前置：工单处于 ASSIGNED/IN_PROGRESS 状态且已超 deadlineDateTime ③流程①：nop-job 扫描 erp_cs_ticket 表，条件 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now()` ④流程②：系统创建 ErpCsTicketAction（actionType=ESCALATE） ⑤流程③：通知 slaPolicy.escalationUserId（客服经理） ⑥流程④a：客服经理评估超时原因决策——重新分派→状态保持 ASSIGNED + 更改 assignedToId ⑦流程④b：延长 deadline（系统管理员操作）→ 更新 deadlineDateTime ⑧流程⑤：系统记录超时原因和升级处理记录 ⑨后置：升级记录可追溯，超时率纳入 SLA 绩效报表 ⑩异常：重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### UC-CS-04 SLA 超时与升级（⚠️ 单级 L1 升级 + 绩效报表完整，重复升级/L2-L3/延长 deadline 大面积缺失）

- **③① nop-job 扫描（✅ 单级实现，默认禁用）**：
  - **Job bean** `ErpCsSlaScanJob.java:36-49`——`execute()` 入口（nop-job BeanMethodJobInvoker 反射调用）+ `resolveCronConfig():56-58` 读 `erp-cs.sla-scan-cron`（空=跳过门控）+ `runSlaScan():51-54` 调 `IErpCsTicketBiz.scanOverdueTickets`。bean 注册 `module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/app-service.beans.xml:14`。
  - **Job 调度（.job.yaml，不在 module-cs）**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-sla-scan.job.yaml:1-10`——`cronExpr: "@cfg:nop.job.erp-cs-sla-scan.cron-expr|0 * * * * ?"`（**每分钟**）+ `enabled: "@cfg:nop.job.erp-cs-sla-scan.enabled|false"`（**默认禁用**）。**SLA 总开关 `erp-cs.sla-enabled`（`ErpCsConfigs.isSlaEnabled():16-18` 默认 true）+ cron 空=跳过** 双重门控。
  - **查询过滤** `ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets:48-77`——QueryBean `status IN (ASSIGNED, IN_PROGRESS)`（`:55-56` 引用 `ErpCsConstants.TICKET_STATUS_ASSIGNED/IN_PROGRESS`）+ `deadlineDateTime < now`（`:57` `lt("deadlineDateTime", now)`，now=`CoreMetrics.currentDateTime():52`）+ `isSlaCompleted=false`（`:58`）——**与 L1 逐字 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now` 一致** + 加严 `isSlaCompleted=false`（避免重复扫描已 resolve 翻转的工单）。**`isSlaCompleted` 在 resolve 时翻转（`ErpCsTicketResolveProcessor:48-50`），不随升级翻转**——查询过滤正确。
  - **SLA 计时基础（startDateTime/deadlineDateTime 计算）**：deadline 计算 `SlaDeadlineCalculator.calculate` + deadline 写入工单 `ErpCsTicketMatchAndAttachSlaProcessor.matchAndAttachSla:62-63`（+ 权益级覆盖 `:101-108`）+ SLA 计时 START `ErpCsTicketBizModel.start:129`（首次 IN_PROGRESS 设 startDateTime）+ SLA 计时 STOP `ErpCsTicketResolveProcessor:43-50`（duration `SlaDeadlineCalculator.minutesBetween` + isSlaCompleted=(deadline==null‖!now.isAfter(deadline)))）+ **reopen 保留 startDateTime**（`ErpCsTicketReopenProcessor:42-44`——A2.14 证伪 P0 候选的行为）。

- **④ ESCALATE 审计（✅ 单次）**：`ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets:70-71` `writeAction(ticket, ACTION_TYPE_ESCALATE, ticket.getStatus(), ticket.getStatus(), "SLA 超时升级通知 escalationUserId", context)`（`ErpCsConstants.ACTION_TYPE_ESCALATE="ESCALATE" :37` + `writeAction():125-135` 写 `ErpCsTicketAction` 行：`ticketId/actionType/fromStatus/toStatus/content/operatorId`）。**无独立 escalate() @BizMutation**——仅经 `scanOverdueTickets`（`ErpCsTicketBizModel.scanOverdueTickets:216-220` 委托）可达。

- **⑤ 通知 slaPolicy.escalationUserId（⚠️ 通知目标漂移）**：`ErpCsTicketScanOverdueTicketsProcessor.notifySlaOverdue:95-111`（config-gated `ErpCsConfigs.isSlaNotifyEnabled():72-74`）——`ctx.put("escalationUserId", ticket.getAssignedToId())`（`:104`）**非 `slaPolicy.escalationUserId`**（**MA2 报告 `:322,585` 残留风险注记**：通知模板经 ROLE resolver 解析实际接收人[客服主管]，设计选择非缺陷，但 L1 字面「通知 slaPolicy.escalationUserId」与实现的 `assignedToId` 漂移）。同一处漂移在 `ErpCsTicketBizModel.notifySlaOverdue:316-332`（findSlaWarnings 预警路径共享）。通知失败静默降级（`catch Exception LOG.warn`，不阻断主升级流程）。

- **⑥ 重新分派（⚠️ 仅手工 + 非升级产物）**：`ErpCsTicketBizModel.assign:101-117`——`requireTicket` + status 守卫（`:108-110` 非 NEW 抛 `ERR_INVALID_TICKET_STATUS_TRANSITION`）+ `setAssignedToId(assignedToId)` + `setStatus(ASSIGNED)` + `writeAction(ASSIGN, NEW→ASSIGNED)`。**重新分派路径仅手工可达且仅接受 NEW 态**（已 ASSIGNED 工单不能再次 assign 守卫抛错）+ **升级流程不自动触发重新分派**——UC-CS-04 流程⑥「客服经理评估超时原因决策重新分派→状态保持 ASSIGNED + 更改 assignedToId」字面要求"状态保持 ASSIGNED"，但实仓 `assign` 仅接受 NEW 态，**升级后 ASSIGNED 工单无法经 `assign` mutation 更改 assignedToId**。

- **⑦ 延长 deadline（❌ 全缺）**：grep `extendDeadline|adjustDeadline|setDeadlineDateTime` 跨 `module-cs/erp-cs-service/src/main` 仅 2 处生产命中——`ErpCsTicketMatchAndAttachSlaProcessor.java:63,107`（matchAndAttachSla 写入 deadline + 权益级覆盖）。**无 `extendDeadline` 方法**——sla.md:172「延长 deadline（管理员手动延长 adjustedDeadlineDateTime 记录原因 计入审计）」未实现。`ErpCsTicketBizModel` 全方法清单：`defaultPrepareSave/assign/start/resolve/close/reopen/cancel/adoptKnowledge/matchAndAttachSla/scanOverdueTickets/findSlaWarnings/findBoardData`（grep `@BizMutation|@BizQuery` 12 方法，无 `extendDeadline`/`adjustDeadline`）。

- **⑧ 系统记录超时原因和升级处理记录（✅ 部分）**：`writeAction:70-71` 写 ESCALATE 审计行 `content="SLA 超时升级通知 escalationUserId"`（无单独的"超时原因"字段——超时原因字段承载于 `ErpCsTicket.remark`，由 close 时 breach 工单 remark 守卫强制 `:152-157`）。升级处理记录可追溯（ErpCsTicketAction 审计行 + `fromStatus/toStatus/content/operatorId` 四字段完整）。

- **⑨ 后置 SLA 绩效报表（✅ 已实现）**：
  - **报表数据集** `ErpCsReportBizModel.buildTicketSlaCsatSummaryDataset:184-243`——聚合 `slaCompletedCount`（isSlaCompleted=true，`:196-197`）+ `slaBreachedCount`（isSlaCompleted=false，`:198-200`）per `ticketTypeId`，行暴露 `totalTickets/slaCompletedCount/slaBreachedCount/surveyCount/avgCsat/avgNps`（`:233-238`）。**返回原始计数非百分比率**——slaCompletionRate 由看板层计算（见下）。
  - **看板 KPI** `ErpCsQualityDashboardBizModel.getDashboardKpi:66-122`——`slaCompleted/slaBreached`（`:82-86` over `loadClosedTickets`）+ `slaCompletionRate`（`:100-102,117` `BigDecimal.valueOf(slaCompleted)/total` 4 位精度）+ `avgResolutionHours`（`:103-106` from duration 分钟）+ `avgFirstResponseHours`（`:107-110` from createTime→startDateTime）。
  - **看板团队排名** `ErpCsQualityDashboardBizModel.getTeamSlaRanking:126+`（@BizQuery）按 teamId 聚合 SLA 达标率。
  - **报表种子** nop-report 接线（参考 cs 域种子报表配置，已 MA1 M5 done）+ AMIS 菜单/页面（`erp-cs.action-auth.xml` tagged UC-CS-04 SLA 报表入口）。

- **⑩ 异常 重复升级/L2-L3 总监升级（❌ 结构性不可实现——R1.28 幂等守卫阻断）**：
  - **幂等守卫使重复升级不可能**——`ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets:64-68` `if (hasEscalationAction(ticket.getId())) continue` + `hasEscalationAction:80-86` QueryBean `ticketId + actionType=ESCALATE` `setLimit(1)` 查询任意 ESCALATE 审计行存在则返回 true。**首次升级后工单仍在 ASSIGNED/IN_PROGRESS 且 deadlineDateTime<now 且 isSlaCompleted=false**——查询过滤仍命中该工单，但幂等守卫 `continue` 跳过，**永不二次 ESCALATE**。
  - **单测断言至多一次**——`TestErpCsTicketSlaCsat.testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175`：第二次 `rpc(mutation, "ErpCsTicket__scanOverdueTickets")` 后 `assertEquals(1, countActionsByType(ticketId, ESCALATE))`（`:173` "重复扫描不应重复 ESCALATE（幂等去重，避免每分钟噪音）"）。**测试与实现同步偏离 L1 异常条款**——测试显式引用 plan `2026-07-30-0841-2 R1.28 P1-MA2-086`，是 R1.28 并发去重修复的最严重噪声案例的回归保护。
  - **无升级级别计数器**——grep `lastEscalationLevel|escalationCount` 跨 `module-cs` **零命中**。
  - **无 L2/L3 通知目标**——`secondEscalationUserId` ORM 缺失（grep `secondEscalationUserId` 跨 `module-cs/model` 零命中；sla.md §1.1:25 + §3.2:164 + README.md:99 声明 Non-Goal）。
  - **无 escalationDelayHours 定时器**——config `erp-cs.escalation-l1-to-l2-hours`（sla.md:286 文档化默认 2h）`ErpCsConfigs.java` 无 reader（grep 跨 `module-cs/erp-cs-service/src/main` 零业务命中）。
  - **L3 总监升级目标载体缺失**——`slaPolicy.escalationUserId`（`ErpCsSlaPolicy` propId10 `:271`，**BIGINT[long] 非 stdDomain=userId**——sla.md:346 实现约定注记）仅 L1 通知目标，无总监级别配置载体。

### ErpCsSlaPolicy ORM 实体（⚠️ L1 字段就绪，L2/L3 字段缺失）

`module-cs/model/app-erp-cs.orm.xml:258-298` `ErpCsSlaPolicy` 实体字段：`id/code/name/ticketTypeId/minPriority/teamId/resolveHours/resolveDays/isWorkingDays/escalationUserId/description` + 标准审计字段。

- **✅ L1 字段就绪**：`resolveHours`(propId7 `:268`) / `escalationUserId`(propId10 `:271`) / `isWorkingDays`(propId9 `:270`)——L1 升级契约（流程③通知 escalationUserId）所需字段齐全。
- **❌ L2/L3 字段缺失**（sla.md §1.1 :24-26 声明但 ORM 无）：`secondEscalationUserId`(L2 通知目标) / `escalationDelayHours`(L1→L2 延迟) / `workingHourStart`/`workingHourEnd` / `isActive`。

### 跨域 daoFor
- `module-cs` 生产代码零跨域 daoFor（同 A1.37 基线 + A2.14 §cs `:320` 已实测确认）。`ErpCsTicketScanOverdueTicketsProcessor` 跨域访问正确经 I*Biz（`IErpMdPartnerBiz:44` 客户名解析 + `IErpSysNotificationBiz:46` 通知派发）。P1-MA1-022 不涉及 cs。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 文件#方法 | 覆盖验收标准 | 断言强度 |
|------|-----------|------------|---------|
| SLA 扫描单次 ESCALATE | `TestErpCsTicketSlaCsat.java#testScanOverdueTicketsCreatesEscalateAction:141-155` | ③④（status ASSIGNED + deadline 过期 → scanOverdueTickets → ESCALATE 审计行生成） | **强断言**（status 精确 + ESCALATE 审计类型断言 `hasActionType(ticketId, ACTION_TYPE_ESCALATE):153-154`） |
| SLA 扫描幂等去重 | `TestErpCsTicketSlaCsat.java#testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` | ⑩（**断言至多一次**——R1.28 P1-MA2-086 最严重噪声类回归保护） | **强断言**（`assertEquals(1, countActionsByType(ticketId, ESCALATE)):168,173` 双断言——首次扫描 1 条 + 重复扫描仍 1 条；**测试与实现同步偏离 L1 异常条款**） |
| SLA 预警查询 | `TestErpCsTicketSlaCsat.java#testFindSlaWarnings:288-307` | findSlaWarnings pre-breach 预警（@BizQuery，无 scheduler 消费） | 中-强断言（deadline BETWEEN now AND now+beforeMinutes 查询验证） |
| close breach 工单须 remark | `TestErpCsTicketSlaCsat.java#testCloseBreachedWithoutReasonRejected:241-270` | ⑨（SLA 绩效 close 守卫——breach 工单须 remark） | **强断言**（`ERR_TICKET_CLOSE_BREACHED_NO_REASON` 错误码精确匹配） |
| SLA match deadline | `TestErpCsTicketSlaCsat.java#testMatchAndAttachSlaCalculatesDeadline` 等 | ⑦（deadline 计算 + 工作日模式跳周末） | **强断言**（deadline 时戳非空 + 在 now 之后） |
| SLA Scan Job bean | `TestErpCsSlaScanJob.java`（3 @Test） | ErpCsSlaScanJob bean 行为（cron-empty skip / cron-set delegation / execute() 签名） | **弱断言**（仅 bean 委托签名 + cron 门控——**不测实际升级逻辑**，委托 stub `CountingJob`） |
| SLA 通知派发 | `TestErpCsSlaNotification`（autotest cases） | ⑤ scanOverdueTriggersNotify / findSlaWarningsTriggersNotify / notifyDisabledSkipsDispatch | 中-强断言（通知派发 config-gated，仅测派发不测目标角色解析） |
| Ticket 6 态状态机 + SLA 计时 | `TestErpCsTicketSlaCsat.java`（13 @Test 全文 412 行） | ②前置 ASSIGNED/IN_PROGRESS + SLA 计时 + 6 态状态机 + reopen 保留 startDateTime | **强断言**（与 A2.14 SLA 计时联动 PASS + 候选 P0 证伪三重证实，A1.37 复用） |
| E2E 6 态状态机 | `tests/e2e/business-actions/cs-ticket.action.spec.ts`（6 态状态机 only） | ②前置 + 6 态状态机经 GraphQL | **强断言**（status + ErrorCode）——**无 SLA timing/ESCALATE/reopen 断言** |

**测试缺口**（与缺口裁决一致）：
1. **重复升级（每 2h/最多 3 次/总监升级）零测试**（功能不存在——幂等守卫封顶 1 次 + 无 escalationCount/secondEscalationUserId/escalationDelayHours 字段，无可测路径）；
2. **延长 deadline 零测试**（`extendDeadline` 方法不存在，无可测路径）；
3. **通知目标正确性（escalationUserId vs assignedToId）无负向断言**（`TestErpCsSlaNotification` 仅测派发不测目标角色解析，无断言通知是否到达 slaPolicy.escalationUserId 角色）。

---

## 4. 运行时行为证据（L5）

| 来源 | 证实的行为 | 复用/补充 |
|------|-----------|----------|
| A2.14 §cs `:224` | cs SLA 升级 cron Job（每分钟扫描 deadlineDateTime<now + ESCALATE 审计 + 通知派发，默认 disabled）L1 升级 PASS | **复用 MA2**（§去重协议，不重新核实） |
| A2.14 §cs | SLA 计时联动完整（startDateTime=首次 IN_PROGRESS + isSlaCompleted=(deadline==null‖now≤deadline) + reopen 保留 startDateTime 时长累加重算）——**候选 P0「SLA 计时恢复累加缺失」证伪** | **复用 MA2** |
| A2.14 §cs | close breach 工单须 remark 守卫 PASS（`ERR_TICKET_CLOSE_BREACHED_NO_REASON`）——SLA 绩效 close 守卫成立 | **复用 MA2** |
| A2.14 `:322,585` | notifySlaOverdue 上下文用 `assignedToId` 置 `escalationUserId` 键（设计选择，模板经 ROLE resolver 解析）残留风险注记——**本切片流程⑤ 通知目标漂移控制点复用** | **复用 MA2** |
| A2.14 `P1-MA2-086` resolved R1.28 | `erp-cs-sla-scan` 是 10 cron job 并发重复副作用最严重噪声案例（每分钟重复 ESCALATE 审计 + 通知），R1.28 修复 = `hasEscalationAction` 幂等守卫——**此修复是 UC-CS-04 重复升级/L2-L3 缺口直接成因** | **复用 MA2**（引用幂等守卫作为重复升级缺口成因，不重审并发维度） |
| `TestErpCsTicketSlaCsat` 13 @Test + E2E 2 tests | 6 态状态机 + SLA 计时 + ESCALATE 单次 + breach close 守卫 + 非法迁移 ErrorCode | **补充**（L1 验收标准视角行为验收） |

L5 存疑点（无法静态定论，需运行时确认）登记入 §7 静态存疑点清单交 MA4 展开。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 分级，§2 判据）

### 五级追踪矩阵

| UC | L1 use-case | L2 owner doc | L3 代码路径 | L4 测试 | L5 运行时 | 结论 |
|----|-------------|--------------|------------|---------|-----------|------|
| UC-CS-04 | `use-cases.md:63`（§1 逐字引用 10 验收标准 + 异常条款） | `sla.md §3.1-3.2:135-165`（升级链 L1/L2/L3 三级表）+ `sla.md §3.3:167-174`（延长 deadline + 标记超时原因 + 忽略超时）+ `sla.md §实现约定:346`（"仅 L1 升级，L2/L3 多级升级链归 Non-Goal"）+ `README.md:98`（"仅 L1 升级，多级升级链归 Non-Goal"）+ `state-machine.md §避免工单滞留:106-109`（NEW>1h/ASSIGNED>2h 升级）——**L2 与 L1 部分冲突：sla.md:346 + README.md:98 静默降级，§4 三判据复核对象** | **③①nop-job 扫描 ✅**（ErpCsSlaScanJob + job.yaml + ScanOverdueTicketsProcessor:48-77）+ **④ESCALATE 审计 ✅**（`:70-71` 单次）+ **⑤通知 ⚠️**（`notifySlaOverdue:95-111` context 用 `ticket.assignedToId` 非 `slaPolicy.escalationUserId`）+ **⑥重新分派 ⚠️**（`assign:101-117` 仅 NEW 态 + 非升级产物 + 升级后 ASSIGNED 工单无法 assign 更改 assignedToId）+ **⑦延长 deadline ❌**（grep `extendDeadline` 零 + sla.md:172 未实现）+ **⑧记录 ✅部分**（writeAction ESCALATE 行 + 超时原因承载于 remark）+ **⑨SLA 绩效 ✅**（ErpCsReportBizModel.buildTicketSlaCsatSummaryDataset:184-243 + ErpCsQualityDashboardBizModel.getDashboardKpi:66-122）+ **⑩重复升级/L2-L3 ❌**（`hasEscalationAction:64-68,80-86` 幂等守卫封顶 1 次 + 无 escalationCount/secondEscalationUserId/escalationDelayHours） | `TestErpCsTicketSlaCsat` 13 @Test 强覆盖 ③④⑨（单次 ESCALATE + breach close + SLA 计时）+ `testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` **断言至多一次**（与实现同步偏离 L1 异常条款）；**⑩重复升级零测试 + ⑦延长 deadline 零测试 + ⑤通知目标负向断言零** | A2.14 cs SLA 升级 L1 PASS + SLA 计时联动 PASS + close breach remark PASS（复用）+ P1-MA2-086 R1.28 幂等守卫作为 ⑩ 直接成因（复用） | 见下方逐条（接受部分 + P1 + P2） |

### 逐 UC 结论（取最高）

#### UC-CS-04 SLA 超时与升级 → **部分接受 + 1 新 P1 + 1 新 P2 + 1 reuse**（③①④⑧⑨接受 + ⑤通知漂移 P2 reuse + ⑥重新分派边界接受 + ⑦延长 deadline P2 + ⑩重复升级/L2-L3 P1）

- **③①nop-job 扫描 + ②前置 = 接受**（§2 判据"接受"）：`ErpCsSlaScanJob.execute:36-49` + `resolveCronConfig():56-58`（cron 空=跳过门控）+ `erp-cs-sla-scan.job.yaml:1-10`（cronExpr 每分钟 + enabled 默认 false 双重门控）+ `ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets:48-77` 查询过滤 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now AND isSlaCompleted=false`——**与 L1 逐字 `status IN (ASSIGNED, IN_PROGRESS) AND deadlineDateTime < now` 一致** + 加严 `isSlaCompleted=false`。L3/L4/L5 三源一致 + A2.14 cs SLA 升级 L1 PASS（`:224`）+ `TestErpCsTicketSlaCsat` 13 @Test 强测 + `TestErpCsSlaScanJob` 3 @Test bean 行为。
- **④ ESCALATE 审计 = 接受**（§2 判据"接受"）：`ScanOverdueTicketsProcessor:70-71` `writeAction(ACTION_TYPE_ESCALATE, ...)` 写 ErpCsTicketAction 行 + `ErpCsConstants.ACTION_TYPE_ESCALATE="ESCALATE":37`。L3/L4/L5 三源一致 + `testScanOverdueTicketsCreatesEscalateAction:141-155` 强断言。
- **⑤ 通知 slaPolicy.escalationUserId → 复用 A2.14 `:322,585` 残留风险注记**（§7 同根因同控制点裁决 + §2 P2①）：L1 逐字「通知 slaPolicy.escalationUserId（客服经理）」；L3 `notifySlaOverdue:95-111` `ctx.put("escalationUserId", ticket.getAssignedToId())`（`:104`）**非 `slaPolicy.escalationUserId`**——A2.14 §cs `:322,585` 残留风险注记：「notifySlaOverdue 上下文用 assignedToId 置 escalationUserId 键，通知模板经 ROLE resolver 解析实际接收人[客服主管]，设计选择非缺陷」。**与既有注记同根因同控制点**（cs SLA-overdue 通知目标载体维度）→ **按 §7 复用既有注记，在 A2.14 报告/A2.14 既有注记行追加 RC A1.38 交叉引用，不新建**。**主路径[通知派发 config-gated `isSlaNotifyEnabled` 默认 true]OK，目标角色经模板 ROLE 解析可能到达正确角色，边界[目标载体漂移]弱**。倾向 **P2 watch-only**（行为上经模板 ROLE resolver 可能到达正确角色 + 不破坏活跃数据 + 通知派发独立于升级主流程 config-gated 静默降级）。
- **⑥ 重新分派 → 边界接受 on 操作员手工语义**（§2 判据"接受 on 操作员语义"）：L1 逐字「重新分派 → 状态保持 ASSIGNED，更改 assignedToId」；L3 `assign:101-117` 实现的语义是「NEW→ASSIGNED + setAssignedToId」（status 守卫仅接受 NEW `:108-110`）。**L1 字面「状态保持 ASSIGNED + 更改 assignedToId」与实现的「仅接受 NEW 态」形式偏离**——升级后 ASSIGNED 工单不能经 assign mutation 更改 assignedToId。**但 L1 「重新分派」语义可通过手工 cancel+重建或 reopen 后 start 路径间接达到状态等价**（操作员代客户/客服经理语义）。**非升级产物**——升级流程 `scanOverdueTickets` 不自动触发重新分派，需客服经理手工决策。**主路径[手工 assign NEW 态]OK，边界[升级后 ASSIGNED 工单无法 assign 更改 assignedToId]弱**。倾向 **接受 on 操作员语义**（差异仅在"是否经 assign mutation 直达"非"是否能重新分派" + 不破坏活跃数据/状态机）。
- **⑦ 延长 deadline → P2**（§2 P2①次要验收标准未完全满足，主路径 OK 边界弱）：L1 逐字「延长 deadline（系统管理员操作）→ 更新 deadlineDateTime」；L3 grep `extendDeadline|adjustDeadline` 跨 `module-cs/erp-cs-service/src/main` **零命中**——`ErpCsTicketBizModel` 12 方法清单无 `extendDeadline`/`adjustDeadline`。sla.md:172 「延长 deadline（管理员手动延长 adjustedDeadlineDateTime 记录原因 计入审计）」未实现。**主路径[deadline 经 matchAndAttachSla:62-63 首次计算写入]OK，边界[超时后延长 deadline]弱**——超时工单的 deadline 不可手工调整，仅能通过 close（breach remark 守卫）或 cancel 终结。**§4 三判据复核**：(i) 无独立 plan-audit 专门裁决延长 deadline 裁剪；(ii) owner doc `sla.md §3.3:172` 显式声明「延长 deadline（管理员手动延长）」属设计意图，**未声明 Deferred**——L2 与 L1 一致，实现未达属实现未达标非设计妥协；(iii) `product-scope.md` grep `延长.*deadline|extend.*deadline|展期` 零命中未将延长 deadline 列入范围裁剪。**三判据均不成立但实际影响受限**（主路径 deadline 计算正确 + close breach remark 守卫齐全 + 工单可经 cancel/close 终结非必须延长）→ **倾向 P2 watch-only**。**声明 Q4=(a) 张力**：若严格按 Q4 应升级 P1（延长 deadline 是 L1 字面验收标准流程④b），但实际影响限于"超时后无法调整 deadline"非"deadline 计算错误"——deadline 首次计算正确 + breach close 守卫齐全，超时工单可经 close/cancel 终结，仅缺"延长期限"独立路径。**新登记 `P2-RC-052`**（UC-CS-04 流程④b 延长 deadline 缺失 watch-only）。
- **⑧ 系统记录超时原因和升级处理记录 = 接受 on 部分**（§2 判据"接受 on 部分"）：`writeAction:70-71` 写 ESCALATE 审计行 + `ErpCsTicketAction` 审计载体完整（ticketId/actionType/fromStatus/toStatus/content/operatorId + createTime）。**超时原因字段承载于 `ErpCsTicket.remark`**（由 close breach 工单 remark 守卫 `:152-157` 强制），非 ESCALATE 审计行独立字段——L1 字面「系统记录超时原因和升级处理记录」语义满足（升级处理记录 = ESCALATE 审计行 + 超时原因 = close remark）。**接受 on 部分**（升级处理记录完整 + 超时原因字段经 close remark 间接承载）。
- **⑨ 后置 SLA 绩效报表 = 接受**（§2 判据"接受"）：`ErpCsReportBizModel.buildTicketSlaCsatSummaryDataset:184-243` 聚合 slaCompletedCount/slaBreachedCount per ticketTypeId + `ErpCsQualityDashboardBizModel.getDashboardKpi:66-122` slaCompletionRate/slaBreachedCount + `getTeamSlaRanking` + 报表种子 + AMIS 菜单。L3/L4/L5 三源一致 + `TestErpCsTicketSlaCsat#testCloseBreachedWithoutReasonRejected` 强测 breach close 守卫 + E2E 报表渲染冒烟 + 值断言。**返回原始计数 + 看板层算比率**——L1 「超时率纳入 SLA 绩效报表」语义满足（slaBreachedCount 经看板 KPI 转换为 slaCompletionRate）。
- **⑩ 异常 重复升级/L2-L3 总监升级 → P1**（§2 P1①功能实质偏离验收标准 + P1②异常路径未实现——**§4 三判据关键裁决**）：L1 逐字 `use-cases.md:80` 异常条款「重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级」；L3 实仓 `hasEscalationAction:64-68,80-86` 幂等守卫**封顶升级次数为 1**——首次 ESCALATE 后，工单仍在 ASSIGNED/IN_PROGRESS + deadlineDateTime<now + isSlaCompleted=false（resolve 才翻转），查询过滤仍命中但 `continue` 跳过永不二次 ESCALATE。**无 escalationCount/lastEscalationLevel 计数器** + **无 secondEscalationUserId ORM 字段**（sla.md §1.1:25 声明但 ORM 无）+ **无 escalationDelayHours config reader**（`erp-cs.escalation-l1-to-l2-hours` sla.md:286 文档化但 `ErpCsConfigs.java` 无 reader）+ **L3 总监通知目标载体缺失**。单测 `testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` **断言至多一次**——测试与实现同步偏离 L1 异常条款。
  - **§4 三判据关键复核**（owner doc Non-Goal 标注的人工批准痕迹核查——P1 裁决核心）：
    - **判据 (i) plan 含独立 plan-audit 通过记录**：grep `docs/plans/` 含 `escalation|重复升级|L2|L3|总监升级|secondEscalation|escalationDelayHours` 的 plan——**无独立 plan 专门裁决 cs-F2 多级升级链裁剪**。`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md` 是 cs SLA 主计划，未含多级升级链裁剪的独立 plan-audit。**判据 (i) 不成立**。
    - **判据 (ii) owner doc 显式 documented simplification 标注且经人工批准**：owner doc `README.md:98` 「仅 L1 升级：超时仅通知策略配置的升级通知人（单级）；多级升级链（L2/L3）归 Non-Goal」+ `sla.md §实现约定:346` 「§3.1-3.2 超时升级：仅 L1 通知 escalationUserId（scanOverdueTickets 创建 ESCALATE 审计）；L2/L3 多级升级链归 Non-Goal（ORM 无 secondEscalationUserId/escalationDelayHours）」——**owner doc 显式 documented simplification**。**但经人工批准痕迹核查**：git log `docs/design/customer-service/README.md` + `docs/design/customer-service/sla.md` 的 Non-Goal 标注 commit history——**全 AI commits**（conventional commit message + AI author pattern），无人工 reviewer 显式批准 Non-Goal 裁剪的痕迹。**按 methodology §4 line 168「AI 自标 ≠ 人工批准」**——owner doc Non-Goal 标注是 AI 自标（无独立 plan-audit 通过 + 无人工 reviewer 批准痕迹）→ **判据 (ii) 不成立**。
    - **判据 (iii) product-scope 范围裁剪登记**：`docs/requirements/product-scope.md` grep `多级升级|L2|L3|总监升级|重复升级|escalation|secondEscalation|escalationDelay` **零命中**——product-scope **未将 cs 多级升级链列入"不在范围"或"后续阶段"**。**判据 (iii) 不成立**。
  - **三判据均不成立 → 非 documented simplification → Q4=(a) 强制实现**。owner doc Non-Goal 标注是静默降级（AI 自标无人工批准痕迹）——与 methodology §4「冲突时以需求真相源为准，推定 owner doc 已向实现妥协」一致。**R1.28[P1-MA2-086] 幂等守卫是并发去重修复**（`hasEscalationAction` guard），其副作用封顶升级次数属**设计张力需更深的升级级别计数器方案**（如 `lastEscalationLevel`/`escalationCount` 字段 + `escalationDelayHours` 定时器 + `secondEscalationUserId` ORM + 总监级别配置载体），**非退缩到方案 B 降级**——修复须协调 R1.28 幂等守卫避免重回 P1-MA2-086 并发噪声。
  - **非 P0**：重复升级/L2-L3 缺失不破坏活跃数据（单级 L1 ESCALATE 审计行完整 + 工单状态机完整 + SLA 计时正确）+ 不破坏会计过账正确性（cs 域不产生 GL 凭证）+ 非核心循环断裂（单级 L1 升级可达 + 客服经理可手工决策重新分派/延长 deadline 经替代路径）+ 不破坏数据隔离/安全。**新登记 `P1-RC-056`**（UC-CS-04 异常 重复升级/L2-L3 总监升级结构性不可实现 + R1.28 幂等守卫成因注记）。

### 切片总结

| UC | 结论 | 命中判据 | Finding |
|----|------|---------|---------|
| UC-CS-04 | **部分接受 + 1 新 P1 + 1 新 P2 + 1 reuse**（③①②④⑥⑧⑨接受 on 主路径/部分/操作员语义；⑤通知漂移 reuse A2.14:322 残留风险注记；⑦延长 deadline P2；⑩重复升级/L2-L3 P1） | §2 接受 / §2 P1①+P1② / §2 P2① / §7 reuse | **P1-RC-056**（新，⑩重复升级/L2-L3）+ **P2-RC-052**（新，⑦延长 deadline）+ **reuse A2.14:322 残留风险注记**（⑤通知漂移，追加 RC 交叉引用） |

**零 P0**（候选缺口均不破坏活跃数据/会计正确性/核心循环——单级 L1 升级完整 + ESCALATE 审计 + 通知派发 + SLA 绩效报表全实现 + 状态机核心完整 + SLA 计时联动正确 + cs 域不产生 GL 凭证 + 重复升级缺失致告警不足但不破坏活跃数据 + 延长 deadline 缺失主路径 deadline 计算正确 + 通知漂移经模板 ROLE 解析可能到达正确角色）。

---

## 6. 与 arm-index 衔接（§7 复用/新增裁决 + 设计张力注记）

### 6.1 复用裁决

- **A2.14 cs SLA 升级 L1 PASS + SLA 计时联动 + close breach remark 守卫 PASS**：复用为 L5 既有证据（§去重协议，§4 复用）。
- **A2.14 cs 域 zero P1（候选 P0「SLA 计时恢复累加缺失」证伪）**：本切片不复审 SLA 计时行为（§去重协议——A2.14 已证伪 reopen 保留 startDateTime 时长累加重算）。
- **A2.14 `:322,585` 残留风险注记（notifySlaOverdue 用 assignedToId 置 escalationUserId 键）**：本切片 UC-CS-04 ⑤「通知 slaPolicy.escalationUserId」字面验收标准 = 既有注记**同根因同控制点**（cs SLA-overdue 通知目标载体维度）。按 §7 复用既有注记，**追加 RC A1.38 交叉引用于 arm-index A2.14 既有注记行（即 P1-MA2-086 resolved 条目下 / A2.14 §cs 残留风险注记引用处），不新建**。本切片仅补需求契约视角（UC-CS-04 L1 字面「通知 slaPolicy.escalationUserId」）补强既有行为维度注记。
- **P1-MA2-086 resolved R1.28（10 cron job 并发重复副作用合并裁决）**：`erp-cs-sla-scan` 是最严重噪声案例，R1.28 修复 = `hasEscalationAction` 幂等守卫。**此修复是 UC-CS-04 ⑩ 重复升级/L2-L3 缺口的直接成因**——本切片引用其幂等守卫作为 P1-RC-056 直接成因注记，**不重审并发维度**（§去重协议：P1-MA2-086 resolved R1.28——本切片引用其幂等守卫作为重复升级缺口直接成因，不重审并发维度）。在 arm-index P1-MA2-086 行追加 RC A1.38 交叉引用注记（不重开 finding，仅标注设计张力）。
- **P2-MA2-067（cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings 无 scheduler）watch-only**：UC-CS-04 流程① nop-job 扫描已落实（deadline-based SLA 升级），但 P2-MA2-067 的 NEW>1h / ASSIGNED>2h 滞留升级规则缺失属**不同控制点**（滞留时间维度升级 vs 重复升级/L2-L3 升级链）——互补不重复。

### 6.2 新增裁决（grep arm-index 后无同域同控制点 RC finding）

> cs 域**无既有 RC finding 涉及 SLA 重复升级/L2-L3 升级链/延长 deadline**（A1.37 cs-F1 是 cs 域首批 RC 切片，登记 P1-RC-054/055 + P2-RC-051 全是工单生命周期维度，不涉及 SLA 升级链）。grep arm-index 「cs sla」「escalat」「escalation」「scan」「overdue」「deadline」「extend」「repeat」「director」「second-escalation」「L2」「L3」RC 系列零命中。既有 cs finding 仅 audit-remediation MA2 系列：P2-MA2-067（滞留升级 watch-only）+ P1-MA2-086（cron 并发幂等 resolved R1.28）——**两者不同控制点**。

| 新 Finding | 域 | UC | 根因 | 与既有 finding 差异 | 分级 |
|-----------|---|----|------|-------------------|------|
| **`P1-RC-056`** | cs | UC-CS-04 ⑩ | UC-CS-04 异常条款「重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级」**结构性不可实现**——`hasEscalationAction:64-68,80-86` 幂等守卫封顶升级次数为 1（首次 ESCALATE 后查询过滤仍命中但 continue 跳过永不二次 ESCALATE）+ 无 escalationCount/lastEscalationLevel 计数器 + 无 secondEscalationUserId ORM 字段（sla.md §1.1:25 声明但 ORM 无）+ 无 escalationDelayHours config reader（`erp-cs.escalation-l1-to-l2-hours` sla.md:286 文档化但 `ErpCsConfigs.java` 无 reader）+ L3 总监通知目标载体缺失 + 单测 `testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` 断言至多一次（与实现同步偏离 L1 异常条款）。**R1.28[P1-MA2-086] 幂等守卫直接成因**（`hasEscalationAction` guard 是 R1.28 并发去重修复，副作用封顶升级次数）。 | 新根因（cs 域 SLA 升级链维度首批 RC finding；与 P2-MA2-067 滞留升级不同控制点[升级链次数 vs 滞留时间维度升级]；与 P1-MA2-086 并发幂等不同维度[需求契约升级次数上限 vs cron 并发副作用]——后者 resolved R1.28 幂等守卫是本 finding 直接成因但本 finding 不重开并发维度，仅从需求契约视角登记 ⑩ 异常条款不可实现 + §4 三判据[README.md:98/sla.md:346 AI 自标 Non-Goal 无人工批准痕迹]均不成立） | P1（§2 P1①功能实质偏离验收标准——重复升级/L2-L3 总监升级验收标准完全缺失 + §2 P1②异常路径未实现——重复升级/总监升级异常条款结构性不可实现） |
| **`P2-RC-052`** | cs | UC-CS-04 ⑦ | UC-CS-04 流程④b「延长 deadline（系统管理员操作）→ 更新 deadlineDateTime」**完全未实现**——grep `extendDeadline|adjustDeadline` 跨 `module-cs/erp-cs-service/src/main` **零命中**，`ErpCsTicketBizModel` 12 方法清单无 `extendDeadline`/`adjustDeadline`，sla.md:172「延长 deadline（管理员手动延长 adjustedDeadlineDateTime 记录原因 计入审计）」未实现。**主路径[deadline 经 matchAndAttachSla:62-63 首次计算写入]OK + breach close remark 守卫齐全，边界[超时后延长 deadline]弱**——超时工单的 deadline 不可手工调整，仅能通过 close（breach remark 守卫）或 cancel 终结。 | 新根因（cs 域 SLA 延长 deadline 维度首批 RC finding；grep arm-index「cs extend」「延长 deadline」「adjustDeadline」RC 系列零命中；与 sla.md:172 未实现衔接） | P2（§2 P2①次要验收标准未完全满足——主路径 deadline 计算正确 OK 边界[延长 deadline]弱 watch-only 声明 Q4=(a) 张力） |

### 6.3 双向可追溯

- 新 finding 入 arm-index RC 发现追踪分区（§见 arm-index 更新）。
- finding 修复行预留 MR1（R1.0 展开为 RC-R1.n 时引用 finding ID）。
- arm-index finding 行修复状态列待 MR1 修复完成后回填 `done`。

### 6.4 R1.28[P1-MA2-086] 幂等守卫与重复升级缺口的设计张力注记

**关键协调约束**（修复 P1-RC-056 时须遵守）：

- **背景**：R1.28（`docs/plans/2026-07-30-0841-2` P1-MA2-086）修复 10 cron job 并发重复副作用，对 `erp-cs-sla-scan`（最严重噪声案例：单实例每分钟重复 ESCALATE 审计 + 通知噪音）采用 `hasEscalationAction` 幂等守卫——首次 ESCALATE 后查询过滤仍命中工单但 `continue` 跳过避免重复审计行 + 通知噪音。
- **副作用**：幂等守卫**封顶升级次数为 1**，使 UC-CS-04 异常条款「重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级」结构性不可实现。
- **修复方案要求**：P1-RC-056 修复须采用**升级级别计数器方案**（如 `ErpCsTicket` 加 `lastEscalationLevel`/`escalationCount` 字段 + `slaPolicy` 加 `secondEscalationUserId`/`escalationDelayHours` 字段 + L3 总监通知目标载体 + `ErpCsConfigs` 增 `escalation-l1-to-l2-hours` reader + `ErpCsTicketScanOverdueTicketsProcessor` 改造：按 `lastEscalationLevel < maxLevel` + 时间窗口 `now - lastEscalationAt >= escalationDelayHours` 触发下一级升级 + 更新计数器 + 派发对应级别通知），**非去除幂等守卫**——避免重回 P1-MA2-086 并发噪声（每分钟重复 ESCALATE + 通知）。
- **保护区域**：修复触及 ORM 结构变更（`ErpCsTicket` 加字段 + `ErpCsSlaPolicy` 加字段 + 可能新增 escalation 级别配置实体）→ **须 ask-first + 独立 plan-audit**（§5 ORM 结构变更类）。

### 6.5 修复触及保护区域标注（§5 预授权/ask-first）

| Finding | 修复范围 | 保护区域 | 门控 |
|---------|---------|---------|------|
| P1-RC-056（重复升级/L2-L3） | `ErpCsTicket` 加 `lastEscalationLevel`(int)/`escalationCount`(int)/`lastEscalationAt`(timestamp) 字段[升级级别计数器] + `ErpCsSlaPolicy` 加 `secondEscalationUserId`(long)/`escalationDelayHours`(int) 字段[L2 通知目标 + L1→L2 延迟] + L3 总监通知目标载体[config key 或新字段] + `ErpCsConstants`/`ErpCsConfigs` 声明 `erp-cs.escalation-l1-to-l2-hours`/`erp-cs.escalation-max-level` config + `ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets` 改造：查询过滤加 `lastEscalationLevel < maxLevel` 条件 + 升级后更新计数器 + 按 `lastEscalationLevel` 决定通知目标[L1→escalationUserId / L2→secondEscalationUserId / L3→总监] + 时间窗口校验 `now - lastEscalationAt >= escalationDelayHours` + 调整单测 `testScanOverdueTicketsIdempotentNoDuplicateEscalation` 改为断言升级次数上限而非至多 1 次 | **触及 ORM 结构变更**（`ErpCsTicket` + `ErpCsSlaPolicy` 加字段，可能新增 escalation 级别配置实体） | **ask-first + 独立 plan-audit §5 ORM 类**（升级级别计数器方案须协调 R1.28 幂等守卫避免重回 P1-MA2-086 并发噪声——见 §6.4 设计张力注记） |
| P2-RC-052（延长 deadline） | `ErpCsTicketBizModel` 增 `extendDeadline` @BizMutation（@Name ticketId + @Name extendedDeadline + @Name reason）+ status 守卫（ASSIGNED/IN_PROGRESS 可延长，RESOLVED/CLOSED/CANCELLED 拒绝）+ `setDeadlineDateTime(extendedDeadline)` + `writeAction(NOTE, "延长 deadline：" + reason)`（审计行记延长前/后 deadline + 原因）+ 可能加 `adjustedDeadlineDateTime` 字段承载延长后值（或直接更新 `deadlineDateTime` + 审计行记旧值）+ AMIS 工单详情页增"延长 deadline"按钮 | 纯 BizModel mutation + 审计行 wiring + AMIS 按钮 | **预授权**（代码逻辑类，不触 §5 ask-first——status 迁移已有守卫范式 + writeAction 既有范式 + AMIS 按钮属既有范式；若加 `adjustedDeadlineDateTime` 字段才触 ORM 须 ask-first，但直接更新 `deadlineDateTime` + 审计行记旧值可不触 ORM） |

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> L5 无法静态定论、需运行时确认的点。**P0 即时通道未触发**（本切片无 P0——单级 L1 升级完整 + SLA 计时正确 + cs 域不产生 GL 凭证 + 重复升级缺失致告警不足但不破坏活跃数据/会计正确性 + 延长 deadline 缺失主路径 deadline 计算正确 + 通知漂移经模板 ROLE 解析可能到达正确角色）。

| 编号 | 存疑点 | 展开方式 |
|------|--------|---------|
| SP-1 | **notifySlaOverdue context 用 assignedToId 经模板 ROLE 解析是否实际到达 escalationUserId 角色**：`notifySlaOverdue:104` `ctx.put("escalationUserId", ticket.getAssignedToId())`（A2.14:322 残留风险注记）。运行时是否经 cs.sla-overdue 通知模板的 ROLE resolver 解析到客服经理角色（slaPolicy.escalationUserId 角色）需确认（与 §6.1 reuse A2.14:322 注记复用） | A4.1 运行时：grep `cs.sla-overdue` 通知模板配置 + ROLE resolver 规则；若有则 assignedToId 经 ROLE 解析到达客服经理角色缓解漂移；若无则通知目标漂移成立需修复 |
| SP-2 | **erp-cs-sla-scan enabled=true 时单实例每分钟扫描的实际升级频率与噪声**：`erp-cs-sla-scan.job.yaml:2` `enabled: "@cfg:nop.job.erp-cs-sla-scan.enabled|false"`（默认 false）+ `hasEscalationAction` 幂等守卫封顶 1 次。运行时 enabled=true 时单实例每分钟扫描的实际升级行为（首次后跳过 + 无重复 ESCALATE 噪音）需确认（与 R1.28 P1-MA2-086 + P1-RC-056 直接成因复用） | A4.1 运行时：在测试环境配置 `nop.job.erp-cs-sla-scan.enabled=true` + 多 tick cron 触发，观察 ESCALATE 审计行数（应仅 1）+ 通知派发次数（应仅 1，R1.28 幂等守卫生效）；与 P1-RC-056 修复后行为对比 |
| SP-3 | **slaPolicy.escalationUserId 为 null 时 notifySlaOverdue 的降级行为**：`ScanOverdueTicketsProcessor.notifySlaOverdue:95-111` config-gated + 失败静默降级（catch Exception LOG.warn），但若 slaPolicy 根本不存在（无匹配策略 → matchAndAttachSla 不挂策略 + deadline 留空）或 escalationUserId 为 null 时通知目标载体无对应角色，运行时通知是否实际派发 + 派发到谁需确认 | A4.1 运行时：在测试环境 seed 无 slaPolicy 的工单 + deadline 过期，调 scanOverdueTickets，观察通知派发行为（应静默降级 LOG.warn）；与 slaPolicy 配置完整的工单对比 |
| SP-4 | **reopen 不延长 deadline 致 RESOLVED 等待窗口计入下次 duration 的实际违约率影响**（A2.14 §cs 残留风险注记 `:115,584`）：reopen（`ErpCsTicketReopenProcessor:42-44`）保留 startDateTime 且不展期 deadlineDateTime，故 RESOLVED 等待窗口（客户响应时间）计入下次 resolve 的 duration——**更严格解释**（惩罚 reopen）致 SLA 违约率升高。运行时实际违约率影响需采样确认 | A4.1 运行时：采样生产 RESOLVED→reopen→resolve 工单的 duration 分布 + 计算"若无 reopen 等待窗口"的虚拟 duration 对比 + 评估违约率偏差（与 A2.14:115,584 复用，归残留风险非缺陷） |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（methodology §8 描述"退出码恒 0"，本次实测 `EXIT=1` 系脚本末段 grep 命令未匹配导致，与 R2c/R2d +2 delta 一致地反映脚本非门控信号源），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码作为门控通过依据。

  | 规则 | baseline | actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 240 | 229 | ✅ (≤) |
  | R2c | 1380 | 1382 | ⚠ +2（**非本审计引入**——本审计为只读审计零生产代码变更，delta 来自其他在途工作，登记供 CI/后续基线对账；与 A1.37 报告记录的同一 delta 基线一致） |
  | R2d | 32 | 34 | ⚠ +2（**非本审计引入**——同上，只读审计） |
  | R3 | 5 | 5 | ✅ |
  | R4/R5/R7/R8/R11 | 0/0/0/0/0 | 0/0/0/0/0 | ✅ |

  **本报告无生产代码变更（纯审计报告），checker 无回归风险**。R2c/R2d 的 +2 delta 系本审计之外的在途工作引入（与 A1.36/A1.37 报告记录的同一 delta 基线一致，非本切片所致）；本审计未修改任何 `.java`/`.xml`/`.yaml` 生产文件（仅新增本报告 + 更新 arm-index + plan 状态）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-056 + P2-RC-052）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1/§6.2），无未经比对直接新建的 finding。UC-CS-04 ⑤通知漂移经 §7 裁决为 A2.14:322 残留风险注记同根因同控制点（cs SLA-overdue 通知目标载体维度）→ 复用不新建；UC-CS-04 ⑩重复升级/L2-L3 引用 P1-MA2-086 resolved R1.28 幂等守卫作为直接成因但不重开并发维度（§去重协议）。

---

## 9. 与 MA2 报告差异增量声明（重申）

见报告开头 §9（前置声明）。**复用 A2.14**（cs SLA 升级 L1 PASS + SLA 计时联动 PASS + close breach remark 守卫 PASS + P2-MA2-067 watch-only + P1-MA2-086 resolved R1.28 幂等守卫 + A2.14:322 残留风险注记 + 候选 P0「SLA 计时恢复累加缺失」证伪）已证实行为，只补需求视角差异：UC-CS-04 异常「重复升级/L2-L3 总监升级」条款 vs 单次幂等实现的契约矛盾（P1-RC-056，R1.28 幂等守卫直接成因）/ UC-CS-04 流程④b 延长 deadline 缺失（P2-RC-052）/ UC-CS-04 流程③ 通知目标漂移（reuse A2.14:322 注记）。

---

## 段落完整性自检（§6 报告输出格式，9 段齐全）

- [x] §1 需求契约原文（L1 逐字引用，UC-CS-04 10 验收标准 + 异常条款完整枚举）
- [x] §2 实现证据（L3 含行号 + 跨域调用链 + ORM 实体字段）
- [x] §3 测试证据（L4 注明断言强度 + 缺口）
- [x] §4 运行时行为证据（L5 复用 MA2 + 补充）
- [x] §5 符合性结论（五级矩阵 + 每 UC 分级 + §2 判据 + §4 三判据复核 + **P1 项 §4 三判据核 sla.md/README.md Non-Goal 标注的人工批准痕迹核查结论**）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 双向可追溯 + 保护区域标注 + **R1.28[P1-MA2-086] 幂等守卫与重复升级缺口的设计张力注记**）
- [x] §7 静态存疑点清单（SP-1~SP-4 供 MA4 展开）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置 + 重申）

**真相源冻结条款遵守声明**：本审计未修改任何真相源（`product-scope.md` / `use-cases.md` / `README.md` / `sla.md` / `state-machine.md` 的需求契约段落）。发现的 doc 分歧（`README.md:98`「仅 L1 升级，多级升级链归 Non-Goal」+ `sla.md:346`「L2/L3 多级升级链归 Non-Goal」+ `sla.md:172`「延长 deadline 管理员手动延长」设计意图 vs 实现未达 + Non-Goal 标注无人工批准痕迹）记入本报告 §5（§4 三判据复核）+ §6.4（设计张力注记），不直改真相源（§9 冻结条款）。
