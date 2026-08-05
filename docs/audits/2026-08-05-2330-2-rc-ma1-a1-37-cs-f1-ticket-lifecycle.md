# A1.37 cs-F1 工单生命周期 需求-实现符合性审计报告（MA1 RC）

> 里程碑：MA1（requirement-compliance mission，Work Item A1.37）
> 域/功能切片：customer-service / 工单生命周期（创建/分派接受/解决确认/计时录入）
> UC 清单：UC-CS-01 / UC-CS-02 / UC-CS-03 / UC-CS-11（4 UC）
> 来源：plan `docs/plans/2026-08-05-2330-2-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.37 → UC-CS-01/02/03/11（✅ 一致）
> 审计类型：只读审计（无生产代码/ORM/api.xml/view.xml/真相源变更）
> 产出时间：2026-08-05

---

## 9. 与 MA2 报告差异增量声明（前置）

本切片报告与既有 MA2 行为审计报告 `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 五域：cs Ticket 6 态全迁移 + SLA 计时联动 + `P2-MA2-067` watch-only）的差异增量，按 §去重协议声明：

- **复用 A2.14 已证实行为作为 L5 既有证据**（不重新核实行为本身）：
  - **cs Ticket 6 态状态机全迁移 PASS**（A2.14 §cs）：assign(NEW→ASSIGNED)/start(ASSIGNED→IN_PROGRESS)/resolve(IN_PROGRESS→RESOLVED)/close(RESOLVED→CLOSED)/reopen(RESOLVED→IN_PROGRESS)/cancel(any non-terminal→CANCELLED) 全迁移守卫齐全，非法迁移全抛 `ERR_INVALID_TICKET_STATUS_TRANSITION`/`ERR_TICKET_ALREADY_TERMINAL`，每迁移写 `ErpCsTicketAction` 审计行。
  - **SLA 计时联动完整 PASS**（A2.14 §cs）：startDateTime=首次进入 IN_PROGRESS（`ErpCsTicketBizModel.start:129`）+ isSlaCompleted=(deadline==null||now≤deadline)（`ErpCsTicketResolveProcessor:48-50`）+ **reopen 保留 startDateTime 时长累加重算**（`ErpCsTicketReopenProcessor` 不清 startDateTime，resolve 时 duration 重算）——**候选 P0「SLA 计时恢复累加缺失」经证据证伪**。
  - **关闭超时工单须 remark 守卫 PASS**（A2.14 §cs）：`close:152-157` breach 工单（isSlaCompleted=false）须 remark 注明超时原因，缺失抛 `ERR_TICKET_CLOSE_BREACHED_NO_REASON`。
  - **`P2-MA2-067`（cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings 无 scheduler）watch-only**：状态机维度登记的软 UX 升级规则缺失，归 owner doc Deferred。
  - **cs 域 zero P1（候选 P0 证伪）**（A2.14 §cs）：SLA 核心完整，无 P1/P0 finding。
- **本切片只补需求视角差异**（use-case 验收标准 vs 实际行为）：
  - **UC-CS-01 创建自动富化大面积缺失**（建议匹配 ticketType/slaPolicy + 自动 deadline + 自动分配 + TK 编号 + 确认通知 + NEW→ASSIGNED 6 项全缺，仅裸 CRUD）
  - **UC-CS-02 拒绝路径缺失**（拒绝回 NEW + 拒绝原因零实现）+ **2h 不响应升级**（与 P2-MA2-067 同控制点，复用注记）
  - **UC-CS-03 客户确认门控缺失**（close 操作员驱动无客户门户门控）+ **7 天自动关闭缺失**（无 scheduler）
  - **UC-CS-11 计时器 session 完全未实现**（仅 ORM 实体 + 裸 CRUD 壳，start/pause/resume/stop/12h/单计时器/聚合全缺 + config flag 未声明）

本切片不重审 A2.14 已证实的 6 态状态机 + SLA 计时联动行为，仅从 L1 验收标准视角补齐需求契约↔行为差异。

---

## 1. 需求契约原文（L1，逐字引用，禁止转述）

> 真相源：`docs/design/customer-service/use-cases.md`（权威功能契约）。L2 owner doc（`README.md` / `sla.md` / `state-machine.md` / `time-tracking.md`）为设计参考，冲突以 L1 为准（§4 Q1）。

### UC-CS-01 客户创建工单（`use-cases.md:3`）

```
**流程** 
1. 客户提交工单信息（subject、description、customerId、priority）。
2. 系统读取客户信息，建议匹配的 ticketType、slaPolicy。
3. 系统自动计算 SLA 截止时间（deadlineDateTime = now + slaPolicy.resolveHours）。
4. 系统根据 ticketType + team 匹配规则自动分配处理人（轮转/最少未结工单）。
5. 工单状态 → NEW → ASSIGNED。
6. 系统向客户发送工单确认通知（含工单编号 TK{YYYYMM}{SEQ4}）。

**后置条件** 工单进入 ASSIGNED 状态，处理人待办列表出现新工单。
**异常** 自动分派无匹配处理人 → 留 NEW 状态，升级通知客服主管人工分派。
```

**验收标准逐条枚举**：①客户提交工单信息(subject/description/customerId/priority) ②系统读取客户信息建议匹配 ticketType/slaPolicy ③自动计算 deadlineDateTime=now+slaPolicy.resolveHours ④自动分配处理人(轮转/最少未结工单) ⑤NEW→ASSIGNED ⑥向客户发送确认通知(含工单编号 TK{YYYYMM}{SEQ4}) ⑦后置：工单进入 ASSIGNED 处理人待办出现 ⑧异常：自动分派无匹配→留 NEW 升级通知主管。

### UC-CS-02 工单分派与接受（`use-cases.md:23`）

```
**流程** 
3. 系统更新 assignedToId，工单状态 → ASSIGNED。
4. 处理人收到通知，点击"接受"→ IN_PROGRESS，startDateTime 记录。
5. 如处理人拒绝，填写拒绝原因，工单回到 NEW 待重新分派。

**后置条件** startDateTime 开始计时，SLA 调用。
**异常** 处理人长时间（>2h）不响应 → 自动升级通知主管。
```

**验收标准逐条枚举**：①主管/系统更新 assignedToId 状态→ASSIGNED ②处理人收到通知点击"接受"→IN_PROGRESS + startDateTime 记录 ③处理人拒绝填拒绝原因工单回 NEW ④后置：startDateTime 开始计时 SLA 调用 ⑤异常：处理人>2h 不响应→自动升级通知主管。

### UC-CS-03 工单解决与客户确认（`use-cases.md:42`）

```
**流程** 
3. 系统校验必填项，工单状态 → RESOLVED。
4. SLA 停止计时，计算实际耗时（duration = now - startDateTime）。
5. 系统判断是否超时（now > deadlineDateTime → isSlaCompleted=false）。
6. 通知客户确认 通过门户/邮件查看解决方案。
7. 客户确认 → CLOSED（endDateTime=now）；客户驳回 → IN_PROGRESS。

**后置条件** RESOLVED 状态保持到客户确认，超时未确认（>7天）自动 CLOSED。
**异常** 处理人超时未解决 → 触发升级流程（参见用例 4）。
```

**验收标准逐条枚举**：①工单状态→RESOLVED ②SLA 停止计时算 duration=now-startDateTime ③判断超时 now>deadlineDateTime→isSlaCompleted=false ④通知客户确认(门户/邮件) ⑤客户确认→CLOSED(endDateTime=now) ⑥客户驳回→IN_PROGRESS ⑦后置：RESOLVED 保持到客户确认，超时未确认(>7天)自动 CLOSED。

### UC-CS-11 工单计时录入（`use-cases.md:203`）

```
**前置条件** erp-cs.time-tracking-enabled=true。
**流程**
1. 客服点击"开始计时"→ 系统创建计时器 session。
2. 客服可暂停/恢复计时（暂停原因可选）。
3. 客服点击"停止计时"→ 生成 ErpCsTimeEntry（startTime、endTime、duration 自动计算）。
4. 客服补充 description、isBillable 标识 → 提交。
5. 可计费条目自动进入审批（或超阈值触发审批）。
6. 审批通过 → 工单总工时聚合（totalTimeSpent、totalBillableTime）。

**后置条件** TimeEntry 进入 PENDING/APPROVED 状态，工单聚合工时可查。
**异常** 单次计时超 12h → 自动停止；同一客服同一时刻只能启动一个计时器。
```

**验收标准逐条枚举**：①前置：erp-cs.time-tracking-enabled=true ②点击"开始计时"→系统创建计时器 session ③暂停/恢复计时(暂停原因可选) ④点击"停止计时"→生成 ErpCsTimeEntry(startTime/endTime/duration 自动计算) ⑤补充 description/isBillable 提交 ⑥可计费条目自动进入审批(或超阈值触发审批) ⑦审批通过→工单总工时聚合(totalTimeSpent/totalBillableTime) ⑧异常：单次计时超 12h→自动停止 ⑨异常：同一客服同一时刻只能启动一个计时器。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### UC-CS-01 客户创建工单（⚠️ 仅裸 CRUD 持久化，6 项自动富化全缺）
- `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java`：
  - `defaultPrepareSave:78-85` 仅 `super.defaultPrepareSave` + 填 `businessDate`（`:82-84` `if (entity.getBusinessDate() == null) entity.setBusinessDate(CoreMetrics.today())`），**无其他富化逻辑**。
  - `ErpCsTicket.xbiz`（`module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/ErpCsTicket.xbiz.xml`）仅审批流 mutation（无 save 时富化 actions）。
- **建议匹配 ticketType/slaPolicy ❌ 缺失**：`ErpCsTicketType.defaultSlaPolicyId`（`module-cs/model/app-erp-cs.orm.xml:231` + `:252` to-one）schema 存在，但 save 路径从不读取（grep `getDefaultSlaPolicyId|defaultSlaPolicyId` 跨 `erp-cs-service/src/main` 零业务调用方，仅 `_gen` entity getter + orm.xml 自身）。
- **自动计算 deadlineDateTime ❌ 缺失（save 路径）**：`ErpCsTicketMatchAndAttachSlaProcessor.matchAndAttachSla:62-63` 计算 `deadline = SlaDeadlineCalculator.calculate(now, policy)` + `setDeadlineDateTime`，但**仅经独立 `matchAndAttachSla` mutation 可达**（`ErpCsTicketBizModel.matchAndAttachSla:212-214` 委托），**非 save 自动触发**。测试/E2E 均显式先 save 再调 matchAndAttachSla。
- **自动分配处理人(轮转/最少未结) ❌ 缺失**：`ErpCsTicketBizModel.assign:101-117` 从调用方取 `assignedToId` 入参（`:103-104` `@Optional @Name("assignedToId") String assignedToId`）+ `:111` `ticket.setAssignedToId(assignedToId)`，**无分配算法**（无 team 成员查询/轮转计数/最少未结比较）。config flag `erp-cs.auto-assign-on-create`（`ErpCsConstants.java:55` `CONFIG_AUTO_ASSIGN_ON_CREATE` + `ErpCsConfigs.java:26-28` `isAutoAssignOnCreate()` 默认 true）**已声明但 main 源码零调用方——死 flag**（grep 全 `module-cs/erp-cs-service/src/main` 仅 3 命中：Constants 自声明 + Configs 自读 + 二者交叉，无 BizModel/Processor 消费）。
- **NEW→ASSIGNED at create ❌ 缺失**：`defaultPrepareSave:78-85` 不设 status；ORM `status` 列（`app-erp-cs.orm.xml:165` mandatory=true，ext:dict=`erp-cs/ticket-status`）**无默认值**。测试/E2E 均显式传 `status='NEW'`。
- **TK{YYYYMM}{SEQ4} 编号 ❌ 缺失**：`code` 列（`app-erp-cs.orm.xml:148`）`tagSet="var"`（用户/调用方自填，非 `seq` 或 `gen`）；grep `TK\{YYYYMM\}|generateCode|nextTicketCode` 跨 `module-cs/erp-cs-service/src/main` **零命中**。仅 catalog 驱动创建 `ErpCsServiceCatalogItemCreateFromCatalogProcessor.java:132` 合成 `data.put("code", "TK-" + CoreMetrics.currentTimeMillis())`（毫秒时间戳格式，非 UC-CS-01 的 `TK{YYYYMM}{SEQ4}` 月份+序号格式，且属 UC-CS-10 服务目录请求而非 UC-CS-01 客户创建工单）。
- **客户确认通知 ❌ 缺失**：save 路径无 `notificationBiz.notify` 调用。仅 SLA-overdue（`ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE` `:71`）/CSAT-reminder（`NOTIFY_EVENT_CSAT_REMINDER` `:77`）/entitlement-expiry（`NOTIFY_EVENT_ENTITLEMENT_EXPIRY` `:114`）三类通知事件存在，**无 ticket-created-confirmation 事件**。

### UC-CS-02 工单分派与接受（✅ 分派+接受记录；⚠️ 拒绝路径 + 2h 升级缺失）
- **assign ✅**：`ErpCsTicketBizModel.assign:101-117`——`requireTicket` + `:108-110` status 守卫（非 NEW 抛 `ERR_INVALID_TICKET_STATUS_TRANSITION`）+ `:111 setAssignedToId` + `:112 setStatus(ASSIGNED)` + `:113 updateEntity` + `:114-115 writeAction(ASSIGN, NEW→ASSIGNED)`。
- **start（"接受"）✅**：`ErpCsTicketBizModel.start:119-134`——`requireTicket` + `:124-126` status 守卫（非 ASSIGNED 抛错）+ `:127 setStatus(IN_PROGRESS)` + `:128-129 setStartDateTime(CoreMetrics.currentTimestamp())`（**startDateTime=首次 IN_PROGRESS**，注释 `:128` 明示）+ `:130 updateEntity` + `:131-132 writeAction(NOTE, ASSIGNED→IN_PROGRESS)`。"接受"隐式 = 调 start，**无独立 `acceptTicket` mutation**（grep `acceptTicket` 跨 `module-cs` 零命中）。
- **拒绝路径(回 NEW + 拒绝原因) ❌ 缺失**：grep `rejectTicket|rejectAssignment|declineAssignment` 跨 `module-cs/erp-cs-service/src/main` **零命中**。ASSIGNED 后唯一前进路径是 `start`，**无"回 NEW"路径**。
- **2h 不响应自动升级 ❌ 缺失（与 P2-MA2-067 同控制点）**：无 scheduler 检查"已分派未开始>2h"；仅 SLA-overdue 扫描（`ErpCsTicketScanOverdueTicketsProcessor`，基于 `deadlineDateTime<now`）+ `findSlaWarnings:224-240`（pre-breach，无 scheduler 消费）。grep `2h|不响应|no-response|ack-timeout` 跨 main 零业务命中。

### UC-CS-03 工单解决与客户确认（✅ resolve+SLA 停止+reopen；⚠️ 客户确认门控 + 7 天自动关闭缺失）
- **resolve ✅**：`ErpCsTicketBizModel.resolve:136-142` 委托 → `ErpCsTicketResolveProcessor.resolve:35-65`——`:38-40` status 守卫（非 IN_PROGRESS 抛错）+ `:43-46` duration 分钟 `SlaDeadlineCalculator.minutesBetween(startDateTime, now)`（startDateTime 空时留空）+ `:48-50 isSlaCompleted = (deadline==null || !now.isAfter(deadline))`（**SLA 停止计时 + 超时判定**）+ `:51 setStatus(RESOLVED)` + `:52-53 setRemark(resolution)` + `:55 dao.updateEntity` + `:56-57 writeAction(NOTE, IN_PROGRESS→RESOLVED)` + `:59-63` CSAT 触发（config-gated `isSurveyEnabled` + trigger-status=RESOLVED）。
- **close ✅（操作员驱动，无客户门户门控）**：`ErpCsTicketBizModel.close:144-164`——`:149-151` status 守卫（非 RESOLVED 抛错）+ `:152-157` breach 工单须 remark 守卫（isSlaCompleted=false 且 remark 空 → `ERR_TICKET_CLOSE_BREACHED_NO_REASON`）+ `:158 setStatus(CLOSED)` + `:159 setEndDateTime(now)` + `:160 updateEntity` + `:161-162 writeAction(CLOSE, RESOLVED→CLOSED)`。**但 close 是任意操作员驱动（context.getUserId()），无客户门户"客户确认"前置门控**——任何 operator 可关闭，无 portal-side confirmation 路径。
- **reopen ✅（语义=操作员 reopen）**：`ErpCsTicketBizModel.reopen:166-170` 委托 → `ErpCsTicketReopenProcessor.reopen:36-51`——status 守卫 + RESOLVED→IN_PROGRESS + 取消未响应 survey（`surveyBiz` 调用）。**客户驳回路径经 reopen 可达但语义是"操作员 reopen"非"客户门户驳回"**（无 customer-facing mutation）。
- **7 天自动关闭 ❌ 缺失**：grep `autoClose|AUTOCLOSE|7 days|7-day|autoCloseResolved` 跨 `module-cs/erp-cs-service/src/main` **零命中**，无定时任务扫描"RESOLVED 超 7 天"。
- **通知客户确认 ❌ 缺失**：无"ticket resolved, please confirm"通知事件（CSAT survey 触发是满意度调查 `NOTIFY_EVENT_CSAT_REMINDER`，非解决确认门控）。grep `resolved.*confirm|please.*confirm|VERIFY_RESOLUTION` 跨 main 零命中。

### UC-CS-11 工单计时录入（⚠️ 仅 ORM 实体 + 裸 CRUD 壳，计时器 session 全缺）
- **ErpCsTimeEntry ORM 实体存在**：`app-erp-cs.orm.xml` ErpCsTimeEntry 实体（含 startTime/endTime/duration/isBillable/billingRate/billableAmount/approvalStatus[PENDING/APPROVED/REJECTED]/source[MANUAL/TIMER_IMPORT]）。
- **ErpCsTimeEntryBizModel 空壳**：`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTimeEntryBizModel.java:1-19`（19 行 CrudBizModel 无任何方法重写，仅构造器 `setEntityName`）。
- **startTimer/stopTimer/pause/resume/TimerSession/activeTimer ❌ 全缺**：grep `startTimer|stopTimer|pauseTimer|resumeTimer|TimerSession|activeTimer` 跨 `module-cs/erp-cs-service/src/main` **零命中**。
- **自动 duration 计算 ❌ 缺失** + **12h 自动停止 ❌ 缺失** + **单计时器约束 ❌ 缺失**（无计时器 session 实体/逻辑可承载）+ **totalTimeSpent/totalBillableTime 聚合 ❌ 缺失**（grep `setTotalTimeSpent|setTotalBillableTime` 跨 main 零业务命中）。
- **config `erp-cs.time-tracking-enabled` ❌ 未声明**：grep `time-tracking-enabled|CONFIG_TIME_TRACKING_ENABLED|isTimeTrackingEnabled` 跨 `module-cs`（含 main + meta + resources）**零命中**——UC-CS-11 前置条件 `erp-cs.time-tracking-enabled=true` 无对应 config key。owner doc `time-tracking.md §七:250` 显式列出该配置项但实现层未声明。

### 工单状态机（✅ 核心迁移齐全 + 非法守卫）
- status 字段 + dict `erp-cs/ticket-status`（`app-erp-cs.orm.xml:30-37`）6 态 NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED；常量 `ErpCsConstants.java:18-23`（TICKET_STATUS_*）。迁移见上 4 UC 实现证据。非法守卫 ErrorCode：`ERR_INVALID_TICKET_STATUS_TRANSITION`/`ERR_TICKET_ALREADY_TERMINAL`/`ERR_TICKET_CLOSE_BREACHED_NO_REASON`/`ERR_TICKET_NOT_FOUND`（`ErpCsErrors.java:51-69`）。每迁移写 `ErpCsTicketAction` 审计行（`ErpCsTicketBizModel.writeAction:360-370`）。

### 跨域 daoFor
- `module-cs` 生产代码**零跨域 daoFor**（grep `daoFor\(` 跨 `erp-cs-service/src/main` 共 38 命中**全 ErpCs* 自域实体**：ErpCsTicket/ErpCsSurvey/ErpCsTicketType/ErpCsSlaPolicy/ErpCsTeam/ErpCsTicketAction/ErpCsEntitlement/ErpCsCatalogFulfillment/ErpCsCannedResponse/ErpCsCatalogCategory/ErpCsServiceCatalogItem）；跨域访问正确经 I*Biz（`IErpMdPartnerBiz:62` + `IErpSysNotificationBiz:64`）。P1-MA1-022（跨域只读 daoFor todo MR1）不涉及 cs（cs 无 daoFor 跨域）。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 文件#方法 | 覆盖验收标准 | 断言强度 |
|------|-----------|------------|---------|
| Ticket 全生命周期 + SLA + 非法迁移 | `TestErpCsTicketSlaCsat.java`（412 行，**13 @Test**）| UC-CS-02 ①②（assign/start + startDateTime）+ UC-CS-03 ①②③（resolve + SLA 停止 + duration + isSlaCompleted）+ 6 态状态机 + 非法迁移 ErrorCode 断言 + SLA-match deadline + scanOverdue ESCALATE 审计 + 幂等 + CSAT 生命周期 + reopen 取消 survey + close-breached 拒绝 + findSlaWarnings | **强断言**（status 精确 + ErrorCode + 审计行 + duration 数值 + deadline 计算） |
| Ticket E2E 6 态状态机 | `tests/e2e/business-actions/cs-ticket.action.spec.ts`（105 行，2 tests）| UC-CS-02 ①② + UC-CS-03 ① + 6 态状态机经 GraphQL assign/start/resolve/close + status 断言 + 非法迁移拒绝 + cancel 直达 | **强断言**（GraphQL 返回 status + 错误码拒绝路径） |

**测试缺口**（与缺口裁决一致）：
1. **UC-CS-01 自动创建语义零测试**（功能不存在——defaultPrepareSave:78-85 仅填 businessDate，无可测自动富化路径）；
2. **UC-CS-02 拒绝路径零测试**（rejectTicket mutation 不存在，无可测路径）；
3. **UC-CS-03 客户确认门控零测试**（无 customer-facing mutation，无可测路径）；
4. **UC-CS-03 7 天自动关闭零测试**（无 scheduler，无可测路径）；
5. **UC-CS-11 计时器行为零测试**（ErpCsTimeEntryBizModel 19 行空壳，startTimer/stopTimer 不存在，仅 ORM 实体 + 标准 CRUD 页面，无可测计时器行为）。

---

## 4. 运行时行为证据（L5）

| 来源 | 证实的行为 | 复用/补充 |
|------|-----------|----------|
| A2.14 §cs | Ticket 6 态状态机全迁移 + 非法守卫 + 每迁移写 ErpCsTicketAction | **复用 MA2**（§去重协议，不重新核实） |
| A2.14 §cs | SLA 计时联动完整（startDateTime=首次 IN_PROGRESS + isSlaCompleted=(deadline==null‖now≤deadline) + reopen 保留 startDateTime 时长累加重算）——**候选 P0「SLA 计时恢复累加缺失」证伪** | **复用 MA2** |
| A2.14 §cs | close breach 工单须 remark 守卫 PASS（`ERR_TICKET_CLOSE_BREACHED_NO_REASON`） | **复用 MA2** |
| A2.14 `P2-MA2-067` | cs NEW>1h / ASSIGNED>2h 滞留升级未实现 + findSlaWarnings 无 scheduler（watch-only） | **复用 MA2**（与本切片 UC-CS-02 "2h 不响应升级"同控制点，§6.1 裁决） |
| `TestErpCsTicketSlaCsat` 13 @Test + E2E 2 tests | 6 态状态机 + SLA 计时 + resolve/close/reopen + 非法迁移 ErrorCode | **补充**（L1 验收标准视角行为验收） |

L5 存疑点（无法静态定论，需运行时确认）登记入 §7 静态存疑点清单交 MA4 展开。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 分级，§2 判据）

### 五级追踪矩阵

| UC | L1 use-case | L2 owner doc | L3 代码路径 | L4 测试 | L5 运行时 | 结论 |
|----|-------------|--------------|------------|---------|-----------|------|
| UC-CS-01 | `use-cases.md:3`（§1 逐字引用 8 验收标准） | `README.md §关键业务规则 1-2:57-58`（设计参考，SLA 自动计时 + 分派规则 NEW 时按工单类型与团队自动匹配处理人）+ `sla.md §实现约定:349`（auto-assign-on-create=true） | `defaultPrepareSave:78-85` 仅填 businessDate（①接受 on 客户提交）；**②建议匹配 ❌**（defaultSlaPolicyId schema 存在 :231 但 save 不读）+ **③自动 deadline ❌**（仅 matchAndAttachSla mutation 可达 :62-63 非自动）+ **④自动分配 ❌**（assign:101-117 取调用方入参无算法 + auto-assign-on-create 死 flag 零消费）+ **⑤NEW→ASSIGNED at create ❌**（save 持久化调用方传入 status 无默认 :165）+ **⑥TK 编号 ❌**（code tagSet="var" :148 + grep generateCode 零）+ **⑧确认通知 ❌**（save 无 notify） | **零测试**（自动富化功能不存在） | A2.14 6 态状态机 PASS（复用，但创建自动富化路径不存在） | 见下方逐条（P1） |
| UC-CS-02 | `use-cases.md:23`（§1 逐字引用 5 验收标准） | `README.md §关键业务规则 2:58`（分派规则 NEW 时自动匹配）+ `state-machine.md §避免工单滞留:106-109`（NEW>1h/ASSIGNED>2h 升级） | **①assign ✅**:101-117 + **②start+startDateTime ✅**:119-134（"接受"=start）+ **③拒绝回 NEW ❌**（grep rejectTicket 零）+ **⑤2h 升级 ❌**（无 scheduler） | `TestErpCsTicketSlaCsat` 13 @Test 强覆盖①②（assign/start/status/startDateTime）；**③⑤零测试** | A2.14 6 态 PASS（复用）+ P2-MA2-067 ASSIGNED>2h 滞留升级 watch-only（复用） | 见下方逐条（P1 部分 + P2 部分） |
| UC-CS-03 | `use-cases.md:42`（§1 逐字引用 7 验收标准） | `README.md §关键业务规则 1/5:57,61`（SLA 计时 + 关闭前检查）+ `state-machine.md`（无客户门户门控契约） | **①②③ resolve+SLA 停止+duration+isSlaCompleted ✅** ResolveProcessor:35-65 + **⑤close ✅**（操作员驱动）:144-164 + **⑥reopen ✅**:166-170（操作员 reopen）；**④通知客户确认 ❌**（无 resolved-please-confirm 事件）+ **⑤close 无客户门户门控 ⚠️** + **⑥客户驳回经 reopen 可达但语义偏离 ⚠️** + **⑦7 天自动关闭 ❌**（grep autoClose 零） | `TestErpCsTicketSlaCsat` 13 @Test 强覆盖①②③⑤ + close-breached 守卫；**④⑦零测试 + 客户门户门控零测试** | A2.14 SLA 计时联动 + close breach remark 守卫 PASS（复用） | 见下方逐条（接受部分 + P2 部分） |
| UC-CS-11 | `use-cases.md:203`（§1 逐字引用 9 验收标准） | `time-tracking.md §一-七`（设计参考，完整计时器 session + 12h + 单计时器 + 聚合设计；§七:250 列 `erp-cs.time-tracking-enabled=true`） | **ErpCsTimeEntry ORM 实体存在**（含 approvalStatus/source 全字段）；**ErpCsTimeEntryBizModel 19 行空 CrudBizModel**；**①time-tracking-enabled config ❌未声明** + **②~⑨计时器 session 全缺**（grep startTimer/stopTimer/pause/resume/TimerSession/activeTimer 零）+ 自动 duration/12h/单计时器/聚合全缺 + config flag 未声明 | **零计时器行为测试**（功能不存在，仅 ORM 实体 + 标准 CRUD 页面可冒烟） | A2.14 未覆盖 cs 计时器（计时器子系统不存在） | 见下方逐条（P1） |

### 逐 UC 结论（取最高）

#### UC-CS-01 客户创建工单 → **P1**（6 项自动富化全缺，仅裸 CRUD）

- **①客户提交工单信息 = 接受**（§2 判据"接受"）：`defaultPrepareSave:78-85` 持久化 subject/description/customerId/priority + 填 businessDate，标准 CRUD 路径完整。
- **②建议匹配 ticketType/slaPolicy → P1**（§2 P1①功能实质偏离验收标准）：L1 逐字「系统读取客户信息，建议匹配的 ticketType、slaPolicy」；L3 实仓 `ErpCsTicketType.defaultSlaPolicyId`（orm.xml:231）schema 存在但 save 路径从不读取（grep `defaultSlaPolicyId` 业务调用零命中）。**§4 三判据复核**：(i) 无独立 plan-audit 专门裁决创建富化裁剪；(ii) owner doc `README.md §关键业务规则 1:57`「SLA 自动计时：工单创建时按 SLA 策略计算截止时间」+ `sla.md §实现约定:349` 显式 `auto-assign-on-create=true` **设计意图含自动富化**，未声明 Deferred——L2 与 L1 一致，实现未达属实现未达标非设计妥协；(iii) `product-scope.md` 未将 cs 创建自动富化列入范围裁剪（grep `客服.*创建|ticket.*create|自动分派|auto-assign` 零命中）。**三判据均不成立 → 非 documented simplification → Q4=(a) 强制实现**。
- **③自动计算 deadlineDateTime → P1**（§2 P1①）：L1 逐字「系统自动计算 SLA 截止时间」；L3 仅 `matchAndAttachSla` mutation 可达非 save 自动触发，测试/E2E 均显式先 save 再调 matchAndAttachSla。**§4 三判据复核**：(i) 无 plan-audit；(ii) owner doc `README.md §关键业务规则 1:57`「工单创建时按 SLA 策略计算截止时间」与 L1 一致未声明 Deferred；(iii) product-scope 未裁剪。三判据均不成立 → Q4=(a) 强制实现。
- **④自动分配处理人(轮转/最少未结) → P1**（§2 P1①）：L1 逐字「系统根据 ticketType + team 匹配规则自动分配处理人（轮转/最少未结工单）」；L3 `assign:101-117` 取调用方 assignedToId 无算法 + `auto-assign-on-create` config flag（`ErpCsConstants:55` + `ErpCsConfigs:26-28`）**已声明但 main 源码零调用方——死 flag**。**§4 三判据复核同上（三判据均不成立）**。**死 flag 证明意图存在但未接线**——属"配置层声明但业务层未消费"同 finance `P1-RC-005`（BANK_RECON_AUTO_REVERSE config key 无 scheduler 消费）+ projects `P1-RC-053`（pnl-calc-cron config key 无 nop-job 消费）范式。
- **⑤NEW→ASSIGNED at create → P1**（§2 P1①）：L1 逐字「工单状态 → NEW → ASSIGNED」；L3 save 持久化调用方传入 status，ORM `status` 列（:165 mandatory=true）无默认值，测试/E2E 均显式传 `status='NEW'`。**§4 三判据复核同上（三判据均不成立）**。**非 P0**：状态机本身完整（assign mutation 实现正确），缺的是"创建时自动迁移"语义非状态机破坏；工单可手工经 assign mutation 完成 NEW→ASSIGNED。
- **⑥TK{YYYYMM}{SEQ4} 编号 → P1**（§2 P1①）：L1 逐字「含工单编号 TK{YYYYMM}{SEQ4}」；L3 `code` 列 tagSet="var"（用户自填）+ grep `TK\{YYYYMM\}|generateCode|nextTicketCode` 零命中；仅 catalog 驱动创建合成 `TK-<millis>`（`ErpCsServiceCatalogItemCreateFromCatalogProcessor:132`，毫秒时间戳格式非月份+序号格式，且属 UC-CS-10 非 UC-CS-01）。**§4 三判据复核同上（三判据均不成立）**。
- **⑦后置：工单进入 ASSIGNED 处理人待办出现 → 受 ②③④⑤⑥ 缺失连带未满足**（无自动分派 + 无 ASSIGNED at create → 处理人待办不会自动出现）。
- **⑧异常：自动分派无匹配→留 NEW 升级通知主管 → P1**（§2 P1②异常路径未实现）：L1 逐字「自动分派无匹配处理人 → 留 NEW 状态，升级通知客服主管人工分派」；L3 无自动分派算法故无"无匹配"分支，无升级通知路径。**§4 三判据复核同上（三判据均不成立）**。
- **整体 Verdict：P1（取最高，§2 P1①+P1②）**。**非 P0**：创建自动富化缺失不破坏活跃数据（工单 CRUD 主路径完整可手工操作全链）+ 不破坏会计过账正确性（cs 域不产生 GL 凭证）+ 非核心循环断裂（工单生命周期可手工 assign/start/resolve/close 走完）。**新登记 `P1-RC-054`**（UC-CS-01 创建自动富化 6 项全缺——建议匹配 + 自动 deadline + 自动分配 + NEW→ASSIGNED + TK 编号 + 确认通知 + 无匹配异常路径合并）。

#### UC-CS-02 工单分派与接受 → **部分接受 + 1 新 P2**（①②接受 + ③拒绝路径 P2 + ⑤2h 升级复用 P2-MA2-067）

- **①assign + ②start+startDateTime = 接受**（§2 判据"接受"）：`assign:101-117`（NEW→ASSIGNED + assignedToId + writeAction ASSIGN）+ `start:119-134`（ASSIGNED→IN_PROGRESS + `setStartDateTime:129`）。L3/L4/L5 三源一致 + `TestErpCsTicketSlaCsat` 13 @Test 强测 + A2.14 6 态 PASS。"接受"=start 隐式实现（无独立 acceptTicket），L1 措辞「点击'接受'→IN_PROGRESS，startDateTime 记录」与 start 行为等价。
- **③拒绝回 NEW + 拒绝原因 → P2**（§2 P2①次要验收标准未完全满足）：L1 逐字「如处理人拒绝，填写拒绝原因，工单回到 NEW 待重新分派」；L3 grep `rejectTicket|rejectAssignment|declineAssignment` 跨 main **零命中**——ASSIGNED 后唯一前进路径是 start，**无"回 NEW"路径**。**与 P2-MA2-067 不同控制点**：P2-MA2-067 = ASSIGNED>2h 滞留升级（时间维度），本缺口 = 拒绝路径（状态迁移维度）。**主路径[接受=start]OK 边界[拒绝路径]弱**。**§4 三判据复核**：(i) 无 plan-audit；(ii) owner doc `state-machine.md` 未声明拒绝路径 Deferred（仅 §避免工单滞留 提到时间维度升级）；(iii) product-scope 未裁剪。**三判据均不成立但实际影响受限**（主路径完整 + 工单可经 cancel/close 走非拒绝路径 + 不破坏活跃数据/GL/状态机）→ **倾向 P2 watch-only**。**声明 Q4=(a) 张力**：若严格按 Q4 应升级 P1，但实际影响限于"拒绝语义未实现"非"工单生命周期破坏"——工单可经 cancel（取消整单）或 reopen（解决后回退）走替代路径，且不破坏 SLA 计时/状态机守卫/审计轨迹。**新登记 `P2-RC-051`**（UC-CS-02 拒绝路径 + UC-CS-03 客户确认门控 + 7 天自动关闭 合并 watch-only）。
- **⑤2h 不响应自动升级 → 复用 `P2-MA2-067`**（§7 同根因同控制点裁决）：L1 逐字「处理人长时间（>2h）不响应 → 自动升级通知主管」；P2-MA2-067（arm-index:633）字面「cs NEW>1h / ASSIGNED>2h 滞留升级规则未实现 + findSlaWarnings 无 scheduler」——**本缺口 = P2-MA2-067 的 ASSIGNED>2h 子项**（同控制点：cs ASSIGNED 状态滞留时间维度升级）。按 §7「同根因同控制点 → 复用既有 finding ID，不新建编号」，**追加 RC A1.37 交叉引用注记于 P2-MA2-067 行，不新建**。
- **整体 Verdict：部分接受（①②接受）+ P2（③拒绝路径）+ reuse P2-MA2-067（⑤2h 升级）**。**非 P0/P1**：主路径完整 + 拒绝路径不影响活跃数据 + 2h 升级属软 UX 规则缺失非数据破坏。

#### UC-CS-03 工单解决与客户确认 → **部分接受 + P2**（①②③接受 + ⑤⑥接受 on 操作员语义 + ④⑦+客户门户门控 P2）

- **①②③ resolve + SLA 停止 + duration + isSlaCompleted = 接受**（§2 判据"接受"）：`ResolveProcessor:35-65` 完整实现 IN_PROGRESS→RESOLVED + duration 分钟（`:43-46`）+ isSlaCompleted=(deadline==null‖!now.isAfter(deadline))（`:48-50`）。L3/L4/L5 三源一致 + 13 @Test 强测 + A2.14 SLA 计时联动 PASS + 候选 P0「SLA 计时恢复累加缺失」证伪（reopen 保留 startDateTime）。
- **⑤close → CLOSED(endDateTime=now) = 接受 on 操作员语义**（§2 判据"接受"）：`close:144-164`（RESOLVED→CLOSED + `setEndDateTime:159` + breach 须 remark 守卫 `:152-157`）。**L1 措辞「客户确认 → CLOSED」与实现的"操作员驱动 close"形式偏离**——但行为等价（CLOSED 终态 + endDateTime 记录 + breach 守卫），差异在"由谁触发"（客户门户 vs 操作员）。
- **⑥客户驳回 → IN_PROGRESS = 接受 on 操作员 reopen 语义**：`reopen:166-170`（RESOLVED→IN_PROGRESS）。**L1 措辞「客户驳回 → IN_PROGRESS」与实现的"操作员 reopen"语义偏离**——状态迁移等价（RESOLVED→IN_PROGRESS）+ reopen 取消未响应 survey，差异在"由谁触发"。
- **④通知客户确认 + ⑤close 客户门户门控 + ⑥客户驳回门户路径 + ⑦7 天自动关闭 → P2**（§2 P2①次要验收标准未完全满足，主路径 OK 边界弱）：L1 逐字「通知客户确认 通过门户/邮件」+「客户确认 → CLOSED；客户驳回 → IN_PROGRESS」+「超时未确认(>7天)自动 CLOSED」；L3 无"ticket resolved, please confirm"通知事件 + 无 customer-facing close/reject mutation + 无 7 天自动关闭 scheduler（grep `autoClose|7 days` 零）。**主路径[操作员 close/reopen]状态等价 + SLA 计时正确 + breach 守卫齐全**，**边界[客户门户门控 + 7 天自动关闭]弱**。**§4 三判据复核**：(i) 无 plan-audit；(ii) owner doc `README.md §状态机:42`「RESOLVED（已给出解决方案，等待客户确认）」+ `state-machine.md` 未显式声明客户门户门控 Deferred（仅描述状态语义非触发主体）——L2 未声明 Deferred 但 L2 触发主体语义模糊（"等待客户确认"未明确"由客户 mutation 触发"vs"由操作员代客户触发"）；(iii) product-scope 未将客户门户门控裁剪。**三判据在"人工批准"意义上不满足但 L2 触发主体语义模糊** → **倾向 P2 watch-only**（形式偏离但行为状态等价 + 不破坏活跃数据/GL/状态机）。**声明 Q4=(a) 张力**：若严格按 Q4 应升级 P1（客户门户门控是 L1 字面验收标准），但实际影响限于"触发主体"非"状态语义"——CLOSED 终态 + endDateTime + breach 守卫均正确，仅缺"由客户门户触发"的独立路径。**合并入 `P2-RC-051`**（与 UC-CS-02 ③拒绝路径合并，均为"客户/处理人门户侧主动 mutation 缺失"同型）。
- **整体 Verdict：部分接受（①②③⑤⑥接受 on 操作员语义）+ P2（④⑦+客户门户门控）**。**非 P0/P1**：状态机完整 + SLA 计时正确 + breach 守卫齐全 + 操作员路径状态等价 + 不破坏活跃数据/GL。

#### UC-CS-11 工单计时录入 → **P1**（计时器 session 完全未实现，仅 ORM 实体 + 裸 CRUD 壳）

- **ErpCsTimeEntry ORM 实体存在 = 接受 on schema**（§2 判据"接受"）：ORM 实体字段齐全（startTime/endTime/duration/isBillable/billingRate/billableAmount/approvalStatus/source）。标准 CRUD 页面 + 菜单存在（`erp-cs.action-auth.xml` tagged UC-CS-11）。
- **①time-tracking-enabled config → P1**（§2 P1①功能完全缺失）：L1 逐字「前置条件 erp-cs.time-tracking-enabled=true」；L3 grep `time-tracking-enabled|CONFIG_TIME_TRACKING_ENABLED|isTimeTrackingEnabled` 跨 `module-cs`（main + meta + resources）**零命中**——config key 未声明，UC-CS-11 前置条件无法满足。**§4 三判据复核**：(i) 无 plan-audit；(ii) owner doc `time-tracking.md §七:250` 显式 `erp-cs.time-tracking-enabled=true` 默认值——**L2 与 L1 一致未声明 Deferred**；(iii) product-scope 未裁剪（grep `计时|time.?tracking|工单计时` 零命中）。三判据均不成立 → Q4=(a) 强制实现。
- **②③④⑤⑥⑦计时器 session + pause/resume + stop/duration + 审批 + 聚合 → P1**（§2 P1①功能完全缺失 + P1⑤验收标准零断言）：L1 逐字要求 7 项行为（开始计时→session / 暂停恢复 / 停止→ErpCsTimeEntry 自动 duration / 补充 description+isBillable / 可计费自动审批 / 审批通过聚合 totalTimeSpent+totalBillableTime）；L3 grep `startTimer|stopTimer|pauseTimer|resumeTimer|TimerSession|activeTimer` 跨 main **零命中**——**计时器 session 子系统完全不存在**。`ErpCsTimeEntryBizModel.java:1-19` 19 行空 CrudBizModel（仅构造器 setEntityName，无任何方法重写）。**§4 三判据复核同上（三判据均不成立）**。**owner doc `time-tracking.md` §一-七完整设计计时器 session + 12h + 单计时器 + 聚合** —— 设计意图完整但实现层完全未落地，属"设计文档已就绪但代码层未实现"同 A1.34 UC-PRJ-03 P1-RC-049（物料归集设计文档就绪但代码缺失）范式。
- **⑧单次计时超 12h 自动停止 + ⑨同一客服同一时刻只能启动一个计时器 → P1**（§2 P1②异常路径未实现）：L1 逐字「单次计时超 12h → 自动停止；同一客服同一时刻只能启动一个计时器」；L3 无计时器 session 故无 12h 上限守卫 + 无单计时器唯一性约束。**§4 三判据复核同上**。
- **整体 Verdict：P1（取最高，§2 P1①+P1②+P1⑤）**。**非 P0**：计时器缺失不破坏活跃数据（工单生命周期主路径完整 + ErpCsTimeEntry CRUD 可手工录入条目）+ 不破坏会计过账正确性（cs 域不产生 GL 凭证，计时器条目经可计费审批走 projects/sales 跨域属另一控制点）+ 非核心循环断裂（工单可手工全链 + 计时是辅助功能非工单流转必要条件）。**新登记 `P1-RC-055`**（UC-CS-11 计时器 session 完全未实现——config 未声明 + start/pause/resume/stop/duration/12h/单计时器/聚合全缺 + BizModel 19 行空壳合并）。

### 切片总结

| UC | 结论 | 命中判据 | Finding |
|----|------|---------|---------|
| UC-CS-01 | **P1**（①接受；②③④⑤⑥⑦⑧全缺） | §2 P1①+P1② | **P1-RC-054**（新） |
| UC-CS-02 | **部分接受 + P2**（①②接受；③拒绝路径 P2；⑤2h 升级 reuse P2-MA2-067） | §2 接受 / §2 P2① / §7 reuse | **P2-RC-051**（新，③拒绝路径）+ **P2-MA2-067 reuse**（⑤2h 升级） |
| UC-CS-03 | **部分接受 + P2**（①②③⑤⑥接受 on 操作员语义；④⑦+客户门户门控 P2） | §2 接受 / §2 P2① | **P2-RC-051**（合并，④⑦+客户门户门控） |
| UC-CS-11 | **P1**（schema 接受；①~⑨计时器 session + config 全缺） | §2 P1①+P1②+P1⑤ | **P1-RC-055**（新） |

**零 P0**（候选缺口均不破坏活跃数据/会计正确性/核心循环——UC-CS-01 创建富化缺失工单可手工 assign/start/resolve/close 走完生命周期 + UC-CS-11 计时器缺失工单流转不受影响 + UC-CS-02③/UC-CS-03④⑦主路径操作员路径状态等价 + 状态机核心完整 + SLA 计时联动正确 + cs 域不产生 GL 凭证）。

---

## 6. 与 arm-index 衔接（§7 复用/新增裁决）

### 6.1 复用裁决

- **`P2-MA2-067`（cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings 无 scheduler）watch-only**：本切片 UC-CS-02 ⑤「处理人>2h 不响应→自动升级通知主管」= P2-MA2-067 的 ASSIGNED>2h 子项**同根因同控制点**（cs ASSIGNED 状态滞留时间维度升级 + 无 scheduler 消费）。按 §7 复用既有 finding ID，**追加 RC A1.37 交叉引用注记于 arm-index P2-MA2-067 行，不新建**。本切片仅补需求契约视角（UC-CS-02 L1 字面「>2h 不响应」）补强既有状态机维度 finding。
- **A2.14 cs Ticket 6 态 + SLA 计时联动 + close breach remark 守卫 PASS**：复用为 L5 既有证据（§去重协议，§4 复用）。
- **A2.14 cs 域 zero P1（候选 P0「SLA 计时恢复累加缺失」证伪）**：本切片不复审 SLA 计时行为（§去重协议——A2.14 已证伪 reopen 保留 startDateTime 时长累加重算）。

### 6.2 新增裁决（grep arm-index 后无同域同控制点 RC finding）

> cs 域**无既有 RC finding**（本切片为 cs 域首批 RC 切片；既有 cs finding 仅 audit-remediation MA2 系列的 P2-MA2-067）。grep arm-index「cs ticket」「create」「assign」「reject」「resolve」「close」「reopen」「timer」「time-entry」「auto-assign」「TK」RC 系列零命中。

| 新 Finding | 域 | UC | 根因 | 与既有 finding 差异 | 分级 |
|-----------|---|----|------|-------------------|------|
| **`P1-RC-054`** | cs | UC-CS-01 ②③④⑤⑥⑦⑧ | 创建自动富化 6 项全缺（建议匹配 + 自动 deadline + 自动分配 + NEW→ASSIGNED + TK 编号 + 确认通知 + 无匹配异常路径）——defaultPrepareSave:78-85 仅填 businessDate | 新根因（cs 域首批 RC finding；与 P2-MA2-067 滞留升级不同控制点[创建自动富化 vs 时间维度升级]；与 finance P1-RC-005 + projects P1-RC-053 死 config flag 范式同型但不同域不同控制点） | P1（§2 P1①+P1②） |
| **`P1-RC-055`** | cs | UC-CS-11 ①~⑨ | 计时器 session 完全未实现（config 未声明 + start/pause/resume/stop/duration/12h/单计时器/聚合全缺 + BizModel 19 行空壳） | 新根因（cs 域首批 RC finding；与 projects P1-RC-049 物料归集设计文档就绪但代码层缺失范式同型但不同域不同控制点） | P1（§2 P1①+P1②+P1⑤） |
| **`P2-RC-051`** | cs | UC-CS-02 ③ + UC-CS-03 ④⑤⑥⑦ | 拒绝路径(回 NEW+拒绝原因) + 客户确认门控(通知+门户 close/reject mutation) + 7 天自动关闭 全缺（主路径操作员路径状态等价） | 新根因（cs 域首批 RC finding；与 P2-MA2-067 滞留升级不同控制点[状态迁移/门户路径 vs 时间维度升级]） | P2（§2 P2①）watch-only |

### 6.3 双向可追溯

- 新 finding 入 arm-index RC 发现追踪分区（§见 arm-index 更新）。
- finding 修复行预留 MR1（R1.0 展开为 RC-R1.n 时引用 finding ID）。
- arm-index finding 行修复状态列待 MR1 修复完成后回填 `done`。

### 6.4 修复触及保护区域标注（§5 预授权/ask-first）

| Finding | 修复范围 | 保护区域 | 门控 |
|---------|---------|---------|------|
| P1-RC-054（创建自动富化） | `ErpCsTicketBizModel.defaultPrepareSave:78-85` 增创建富化（读 ticketType.defaultSlaPolicyId 建议匹配 + 自动调 matchAndAttachSla 计算 deadline + 实现 auto-assign-on-create 接线 config flag 已存在[轮转/最少未结算法] + status 默认 NEW + save 后自动 assign 或留 NEW 异常路径 + TK{YYYYMM}{SEQ4} code 生成[code 列 tagSet 改 "seq" 或 gen 规则] + 客户确认 notify 事件 wiring） | **混合**：①纯 BizModel 富化 + notify 接线（预授权，不触 §5 ask-first）；②code 列 tagSet="var"→"seq" 触及 **ORM 结构变更**（须 ask-first + 独立 plan-audit §5 ORM 类）；③轮转/最少未结算法需 team 成员查询（ErpCsTeam 仅 teamLeaderId 无成员子实体——**触及 ORM 结构变更 须 ask-first**，或经 IErpOrgBiz/IUserBiz 跨域查询避免 ORM 改动） | **部分预授权 + 部分 ask-first**（BizModel 富化 + notify 预授权；code 序号化 + team 成员载体 须 ask-first） |
| P1-RC-055（计时器 session） | 新增 `ErpCsTicketTimerSession` 计时器实体[agentId/ticketId/startTime/pauseStartDateTime/cumulativePauseMinutes/status(RUNNING/PAUSED/STOPPED) 唯一约束(agentId, status=RUNNING) 单计时器] + `ErpCsTimeEntryBizModel` 增 startTimer/stopTimer/pause/resume mutation[12h 自动停止守卫 + duration 自动计算 + 单计时器唯一性] + `erp-cs.time-tracking-enabled` config 声明[ErpCsConstants + ErpCsConfigs] + 审批触发 + totalTimeSpent/totalBillableTime 聚合[ErpCsTicket 聚合查询或字段] | **触及 ORM 结构变更**（新增 ErpCsTicketTimerSession 实体 + 可能 ErpCsTicket 加聚合字段） | **ask-first + 独立 plan-audit §5 ORM 类**（计时器 session 实体属 ORM 结构变更；若聚合走查询非字段可降低 ORM 触及面） |
| P2-RC-051（拒绝+客户门户+7 天关闭） | `ErpCsTicketBizModel` 增 `rejectAssignment` mutation（ASSIGNED→NEW + 拒绝原因 remark）+ `customerConfirm`/`customerReject` mutation（客户门户路径，复用 close/reopen 状态迁移语义）+ 7 天自动关闭 scheduler[注册 nop-job 扫 RESOLVED 超 7 天 + 自动 close] + "ticket resolved, please confirm" notify 事件 | 纯 BizModel mutation + scheduler 接线 + notify 接线 | **预授权**（代码逻辑类，不触 §5 ask-first——status 迁移已有守卫范式 + scheduler 接线 + notify 事件 wiring 属既有范式） |

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> L5 无法静态定论、需运行时确认的点。**P0 即时通道未触发**（本切片无 P0——创建富化/计时器缺失不破坏活跃数据/会计正确性 + 状态机核心完整 + SLA 计时联动正确 + 工单可手工操作全链 + cs 域不产生 GL 凭证）。

| 编号 | 存疑点 | 展开方式 |
|------|--------|---------|
| SP-1 | **`auto-assign-on-create` 死 flag 在 delta/未来定制消费的运行时行为**：config flag（ErpCsConstants:55 + ErpCsConfigs:26-28 默认 true）已声明但 main 源码零调用方。delta 定制可能消费该 flag 触发自动分派——运行时是否经 xbiz 跨域 GraphQL 或 AMIS 按钮间接触发需确认（与 P1-RC-054 复用） | A4.1 运行时：grep `_vfs/erp/cs/ErpCsTicket.xbiz.xml` + AMIS view.xml 是否含 auto-assign 路径 + delta beans.xml 是否覆盖 defaultPrepareSave 触发自动分派；若有则前端可达但主代码层仍缺失 |
| SP-2 | **matchAndAttachSla 作为创建后置手动步骤的运行时可达性**：`matchAndAttachSla:212-214` mutation 存在且 `ErpCsTicketMatchAndAttachSlaProcessor:62-63` 计算 deadline，测试/E2E 均显式先 save 再调 matchAndAttachSla。运行时是否经前端 AMIS 创建工单页面自动串联（save→matchAndAttachSla 两步对用户透明）需确认（与 P1-RC-054 复用） | A4.1 运行时：grep cs 工单创建 AMIS view.xml 是否含 matchAndAttachSla 后置调用；若有则前端编排弥补 save 路径自动 deadline 缺失（但仍非 L1 字面"系统自动"语义） |
| SP-3 | **reopen 作为客户驳回替代路径的语义等价性**：`reopen:166-170`（RESOLVED→IN_PROGRESS）实现状态迁移等价 L1「客户驳回 → IN_PROGRESS」，但触发主体是操作员非客户门户。运行时是否经前端区分"客户驳回"vs"操作员 reopen"语义需确认（与 P2-RC-051 复用） | A4.1 运行时：grep AMIS view.xml + xbiz mutation 入参是否含 customerFacing 标记或独立 customerReject mutation；若无则语义等价但形式偏离 |
| SP-4 | **close 操作员驱动在无客户确认下是否产生数据完整性问题**：`close:144-164` 任意 operator 可关闭（context.getUserId()），无客户确认前置门控。运行时是否经权限角色 RBAC 限制（仅客服经理可关闭）或 SLA 报表准确性受影响需确认（与 P2-RC-051 复用） | A4.1 运行时：grep `ErpCsTicket.close` xbiz 权限注解 + AMIS close 按钮角色门控；若有 RBAC 限制则形式偏离缓解；SLA 报表受 RESOLVED→CLOSED 时长影响需运行时数据采样 |
| SP-5 | **ErpCsTimeEntry CRUD 壳在 delta/未来定制下计时器行为**：`ErpCsTimeEntryBizModel:1-19` 19 行空 CrudBizModel，标准 CRUD 页面存在。delta 定制可能扩展该 BizModel 实现计时器——运行时是否经 delta beans.xml 覆盖已有部分计时器逻辑需确认（与 P1-RC-055 复用） | A4.1 运行时：grep delta beans.xml 是否覆盖 ErpCsTimeEntryBizModel + AMIS 是否含计时器 wizard 页面；若有则定制层部分弥补但 main 层仍缺失 |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本次实测 `EXIT=0`），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码 0 作为门控通过依据。

  | 规则 | baseline | actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 240 | 229 | ✅ (≤) |
  | R2c | 1380 | 1382 | ⚠ +2（**非本审计引入**——本审计为只读审计零生产代码变更，delta 来自其他在途工作，登记供 CI/后续基线对账） |
  | R2d | 32 | 34 | ⚠ +2（**非本审计引入**——同上，只读审计） |
  | R3 | 5 | 5 | ✅ |
  | R4/R5/R7/R8/R11 | 0/0/0/0/0 | 0/0/0/0/0 | ✅ |

  **本报告无生产代码变更（纯审计报告），checker 无回归风险**。R2c/R2d 的 +2 delta 系本审计之外的在途工作引入（与 A1.36 报告记录的同一 delta 基线一致，非本切片所致）；本审计未修改任何 `.java`/`.xml`/`.yaml` 生产文件（仅新增本报告 + 更新 arm-index + plan 状态）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-054/055 + P2-RC-051）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1/§6.2），无未经比对直接新建的 finding。UC-CS-02 ⑤2h 升级经 §7 裁决为 P2-MA2-067 同根因同控制点（cs ASSIGNED>2h 滞留时间维度升级）→ 复用不新建。

---

## 9. 与 MA2 报告差异增量声明（重申）

见报告开头 §9（前置声明）。**复用 A2.14（cs Ticket 6 态状态机 + SLA 计时联动 + close breach remark 守卫 + P2-MA2-067 watch-only + 候选 P0「SLA 计时恢复累加缺失」证伪）已证实行为，只补需求视角差异**：UC-CS-01 创建自动富化 6 项全缺 / UC-CS-02 拒绝路径缺失（+2h 升级复用 P2-MA2-067）/ UC-CS-03 客户确认门控 + 7 天自动关闭缺失 / UC-CS-11 计时器 session 完全未实现。

---

## 段落完整性自检（§6 报告输出格式，9 段齐全）

- [x] §1 需求契约原文（L1 逐字引用，4 UC 验收标准完整枚举）
- [x] §2 实现证据（L3 含行号 + 跨域调用链）
- [x] §3 测试证据（L4 注明断言强度 + 缺口）
- [x] §4 运行时行为证据（L5 复用 MA2 + 补充）
- [x] §5 符合性结论（五级矩阵 + 每 UC 分级 + §2 判据 + §4 三判据复核）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 双向可追溯 + 保护区域标注）
- [x] §7 静态存疑点清单（SP-1~SP-5 供 MA4 展开）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置 + 重申）

**真相源冻结条款遵守声明**：本审计未修改任何真相源（`product-scope.md` / `use-cases.md` / `README.md` / `sla.md` / `time-tracking.md` / `state-machine.md` 的需求契约段落）。发现的 doc 分歧（`README.md §关键业务规则 1-2:57-58` 创建自动富化设计意图 + `sla.md §实现约定:349` auto-assign-on-create=true + `time-tracking.md §七:250` time-tracking-enabled=true 设计意图 vs 实现未达）记入本报告 §5，不直改真相源（§9 冻结条款）。
