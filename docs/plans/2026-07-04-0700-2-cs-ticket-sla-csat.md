# 2026-07-04-0700-2-cs-ticket-sla-csat 客服工单状态机 + SLA 计时 + 满意度调查

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` 工作项 3.5 / 3.6（M3 客服域）；`docs/design/customer-service/README.md`、`docs/design/customer-service/state-machine.md`、`docs/design/customer-service/sla.md`、`docs/design/customer-service/csat.md`
> Related: `2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`（同批 N=1，CRM 续；本计划 N=2，两域无相互依赖，CRM 先行仅为路线图自然顺序）
> Audit: required

## Current Baseline

- 客服域 CRUD 全 done（`crud-roadmap.md` M3）。`ErpCsTicket`（`module-cs/model/app-erp-cs.orm.xml:154`）已建模——`status`(NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED)、`priority`、`ticketTypeId`、`assignedToId`、`slaPolicyId`、`deadlineDateTime`、`isSlaCompleted`、`startDateTime`、`endDateTime`、`duration`、`progress`、`customerId`/`contactId`、`source` 齐全；三轴分离（status/docStatus/approveStatus）。`ErpCsTicketBizModel` 为 15 行 codegen 空壳，`IErpCsTicketBiz` 接口已生成。**剩余差距**：无工单状态机动作（assign/start/resolve/close/cancel/reopen）、无 SLA 策略匹配、无 deadline 计算、无 isSlaCompleted 标记、无超时升级。
- `ErpCsSlaPolicy`（orm.xml:227）已建模——`ticketTypeId`/`minPriority`/`teamId`/`resolveHours`/`resolveDays`/`isWorkingDays`/`escalationUserId`。**ORM 与设计差距**：(1) 缺 `workingHourStart`/`workingHourEnd`（工作时段窗口）；(2) 缺 `secondEscalationUserId`/`escalationDelayHours`（L2 升级）；(3) **缺 `isActive` 列**（sla.md §1.1 列出但未建模）——SLA 策略匹配不按 active 过滤，按精确度排序取首条匹配（见 Phase 1 Decision）。另：`escalationUserId` 类型为 `BIGINT`(long)，非 `stdDomain=userId` 的 VARCHAR(36)（与 assignedToId/operatorId 不同源），通知逻辑需按 long ID 解析用户。本计划按 ORM 现状交付，工作时段窗口/L2 升级/isActive 归 Non-Goal（不扩 ORM）。
- `ErpCsTicketAction`（orm.xml:277）已建模——`actionType`(ASSIGN/NOTE/ATTACH/ESCALATE/CLOSE 等)、`fromStatus`/`toStatus`、`operatorId`、`content`，作状态迁移审计 + 升级记录载体。**无独立 `ErpCsTicketSlaPause` 实体**（设计 §2.2 的 SLA 暂停机制）——暂停归 Non-Goal。
- `ErpCsTicketType`（含 `defaultSlaPolicyId`/`defaultPriority`）、`ErpCsTeam` 已建模。字典 `erp-cs/ticket-status`/`ticket-priority`/`ticket-source`/`ticket-type`/`action-type` 已就绪。
- **3.6 CSAT 基线**：`ErpCsSurvey`（orm.xml:388）已建模——`ticketId`、`surveyToken`、`csatScore`、`npsScore`、`cesScore`、`comment`、`respondedAt`、`surveySentAt`、`surveyChannel`。**ORM 与设计差距**：无独立 `status` 列（PENDING/SENT/COMPLETED/FAILED）——状态由 `surveySentAt`/`respondedAt` 时间戳派生（PENDING=sentAt 空 / SENT=sentAt 有 & respondedAt 空 / COMPLETED=respondedAt 有）。`ErpCsSurveyBizModel` 为 15 行空壳。**剩余差距**：无调查创建（RESOLVED 触发）、无 token 提交、无 NPS/CES 分类、无提醒/过期。
- 跨域：客服工单不产生会计凭证（`customer-service/README.md §业财过账`）；质量问题升级 quality NCR、设备报修触发 maintenance 属独立联动（弱指针/可选钩子）。无 ORM 模型变更、无 codegen 重生成。

## Goals

- **3.5 工单状态机 + SLA 计时**：`ErpCsTicketBizModel` 状态机动作——`assign`(NEW→ASSIGNED)、`start`(ASSIGNED→IN_PROGRESS，设 `startDateTime`)、`resolve`(IN_PROGRESS→RESOLVED，停 SLA 计时算 `duration`、标记 `isSlaCompleted = resolvedAt ≤ deadlineDateTime`)、`close`(RESOLVED→CLOSED，设 `endDateTime`，关闭前校验 isSlaCompleted/超时原因)、`reopen`(RESOLVED→IN_PROGRESS，恢复计时累加)、`cancel`(*→CANCELLED)；每迁移写 `ErpCsTicketAction` 审计。
- **SLA 策略匹配 + deadline 计算**：工单创建/优先级变更时按 ticketType + minPriority + team 匹配 `ErpCsSlaPolicy`（精确优先→兜底），写 `slaPolicyId` + 算 `deadlineDateTime`（日历小时模式主路径：`now + resolveHours`；工作日模式 config-gated：`isWorkingDays=true` 时按 Mon-Fri 跳周末，节假日日历可选——见 Decision）。
- **SLA 超时升级 + 预警**：`scanOverdueTickets()` 扫描 `adjustedDeadline < now` 且未完成的工单，创建 `actionType=ESCALATE` 的 TicketAction + 通知 `escalationUserId`（L1，config-gated）；`findSlaWarnings(beforeMinutes)` 预警查询（供 nop-job 调用）。
- **3.6 CSAT 调查生命周期**：`createSurvey(ticketId)`——工单 RESOLVED 时触发（config-gated `survey-trigger-status` + `survey-send-delay`），生成 `surveyToken`(UUID)，状态 PENDING（派生）；`submitSurvey(token, csatScore, npsScore?, cesScore?, comment?)`——无鉴权 token 提交，设 `respondedAt`，状态→COMPLETED，NPS 分类（推荐者/被动者/贬损者）；提醒/过期查询方法。
- 两子系统 config-gated、不产生凭证、零核心实体污染（quality/maintenance 零字段新增）。

## Non-Goals

- **SLA 暂停/恢复机制（Pending）**（设计 §2.2，需独立 `ErpCsTicketSlaPause` 实体 + adjustedDeadlineDateTime——ORM 现状不支持，扩 ORM 属独立面）。
- **工作时段窗口（workingHourStart/End）+ 节假日日历精确计算**（SlaPolicy ORM 无 workingHour 字段；工作日模式首版仅 Mon-Fri 周末跳过；节假日日历与精确工时累计归 Non-Goal）。
- **SLA L2/L3 多级升级链**（SlaPolicy ORM 无 secondEscalationUserId/escalationDelayHours——仅交付 L1 escalationUserId）。
- **调查独立 status 列 + FAILED/EXPIRED 持久状态机**（Survey ORM 无 status 列；状态由时间戳派生，FAILED/EXPIRED 仅查询期判定）。
- **调查实际邮件/门户发送通道集成**（本计划交付 createSurvey/submitSurvey 业务逻辑 + token；实际邮件发送/门户渲染属 nop-notification 独立面）。
- **客服绩效（CSAT→HR KPI）/ 知识库关键词推荐 / 工单自动分派轮转算法**（独立结果面）。
- **nop-job cron 实际注册/调度**（本计划交付可经 GraphQL/测试调用的 BizModel 方法；cron 注册为 config-gated 接线点，实际调度器注册为 Follow-up）。
- **quality NCR / maintenance 联动实际触发**（弱指针/可选钩子，独立面）。

## Task Route

- Type: `implementation-only change`（无 ORM 模型变更——实体/字典/IBiz 接口全就绪，仅 BizModel 自定义动作 + SLA 计算 + config-gated 门控 + 同域跨实体调用）
- Owner Docs: `docs/design/customer-service/README.md`（§业务规则 SLA 计时/分派/关闭前检查）、`docs/design/customer-service/state-machine.md`（工单状态机权威设计）、`docs/design/customer-service/sla.md`（SLA 策略匹配/deadline 计算/升级）、`docs/design/customer-service/csat.md`（CSAT 调查生命周期/评分映射）
- Skill Selection Basis: BizModel 自定义动作 + 工单状态机 + SLA 计时算法 + config 门控 + 跨实体同域（Ticket→SlaPolicy/TicketAction/Survey）→ 加载 `nop-backend-dev`；状态机可达性自检由该技能路由

## Infrastructure And Config Prereqs

- 配置项（均 config-gated）：
  - `erp-cs.sla-enabled`（默认 true）
  - `erp-cs.sla-scan-interval`（默认 1 分钟，扫描方法调用频率提示）
  - `erp-cs.sla-warning-before`（默认 60 分钟，预警提前量）
  - `erp-cs.auto-assign-on-create`（默认 true，新建工单是否自动分派；详见 `customer-service/README.md` 配置点）
  - `erp-cs.survey-enabled`（默认 true）
  - `erp-cs.survey-trigger-status`（默认 RESOLVED）
  - `erp-cs.survey-send-delay`（默认 0 小时）
  - `erp-cs.survey-csat-enabled`/`survey-nps-enabled`(默认 false)/`survey-ces-enabled`(默认 false)
- 无外部服务/端口/密钥依赖；无数据迁移；无 codegen 重生成。

## Execution Plan

### Phase 1 - 工单状态机 + SLA 策略匹配 + deadline 计算 + 超时升级（工作项 3.5）

Status: completed
Targets: `ErpCsTicketBizModel.java`、`ErpCsSlaPolicyBizModel.java`；新增 support 类（如 `SlaPolicyMatcher`、`SlaDeadlineCalculator`）；`ErpCsErrors`/`ErpCsConstants`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] Add: `ErpCsTicketBizModel` 状态机动作——`assign`(NEW→ASSIGNED，设 assignedToId，可选 config-gated 自动分派)、`start`(ASSIGNED→IN_PROGRESS，设 `startDateTime`=now)、`resolve`(IN_PROGRESS→RESOLVED，停计时算 `duration`=(now-startDateTime)分钟，标 `isSlaCompleted`=(now ≤ deadlineDateTime))、`close`(RESOLVED→CLOSED，设 endDateTime，关闭前校验：超时工单需超时原因 remark)、`reopen`(RESOLVED→IN_PROGRESS，恢复计时，duration 累加)、`cancel`(非终态→CANCELLED)；非法迁移抛 ErrorCode；每迁移写 `ErpCsTicketAction`(fromStatus/toStatus/operatorId)。范式对照既有三轴审批 BizModel。
  - Skill: `nop-backend-dev`
- [x] Add: SLA 策略匹配 `SlaPolicyMatcher`——工单创建/优先级变更时查 `ErpCsSlaPolicy`(ticketTypeId 匹配 + minPriority ≤ 工单 priority + (teamId=工单 teamId OR teamId 空)；ORM 无 isActive 列故不做 active 过滤)，按精确度排序（type+priority+team > type+team > type+priority > type 兜底）取首条，写 `ticket.slaPolicyId`；无匹配则不挂策略（deadlineDateTime 空）。
  - Skill: `nop-backend-dev`
- [x] Add: deadline 计算 `SlaDeadlineCalculator`——挂策略时算 `deadlineDateTime`：日历小时模式(`isWorkingDays=false`)：`now + resolveHours`；工作日模式(`isWorkingDays=true`)：按 resolveDays/Hours 跳周末（Mon-Fri），config-gated；写回 ticket.deadlineDateTime。priority 变更时重算（保留原 startDateTime）。
  - Skill: `nop-backend-dev`
- [x] Add: 超时升级 + 预警——`scanOverdueTickets()`（status IN ASSIGNED/IN_PROGRESS AND deadlineDateTime < now AND isSlaCompleted=false → 创建 `actionType=ESCALATE` TicketAction + 通知 escalationUserId，config-gated `sla-enabled`）；`findSlaWarnings(beforeMinutes)`（deadlineDateTime BETWEEN now AND now+beforeMinutes 且未完成，供 nop-job 预警）。
  - Skill: `nop-backend-dev`
- [x] Decision: 工作日模式节假日——首版仅 Mon-Fri 周末跳过，不依赖节假日日历主数据（设计 §5.2 的 ErpHolidayCalendar 未确认存在）。精确节假日 + 工作时段窗口累计归 Non-Goal（ORM 无 workingHour 字段）。残留风险：含法定节假日的准确 SLA 截止需后续接入日历主数据——记 Follow-up。
  - Skill: `nop-backend-dev`
- [x] Decision: SLA 计时起止——选 `startDateTime = 首次 IN_PROGRESS 时间`（与设计 §2.1 一致，而非 NEW 创建时），`duration = resolve 时 now - startDateTime`。理由：NEW/ASSIGNED 阶段未实际处理，计实际处理时长更公平。
  - Skill: `nop-backend-dev`
- [x] Decision: SlaPolicy 无 isActive 列——匹配器不做 active 过滤，按精确度排序取首条匹配策略（设计 §1.2 匹配优先级即"取第一条"，已隐含唯一选择）。替代方案：扩 ORM 加 isActive——被否（破坏 implementation-only 定性；管理员可通过删除/作废策略控制可用集）。残留风险：多策略同精确度时取序——记 Follow-up（触发条件：需显式启用/禁用策略切换时，加 isActive 列）。
  - Skill: `nop-backend-dev`
- [x] Decision: TicketAction 审计 actionType 取值——`erp-cs/action-type` 字典仅 ASSIGN/NOTE/ATTACH/ESCALATE/CLOSE/CANCEL（无 START/RESOLVE/REOPEN）。选"不扩字典，迁移语义由 fromStatus/toStatus 承载"（assign→ASSIGN、close→CLOSE、cancel→CANCEL、超时→ESCALATE；start/resolve/reopen 复用 NOTE 或最接近码，fromStatus/toStatus 记录精确迁移）。替代方案：扩字典加 START/RESOLVE/REOPEN——被否（字典扩展属 ORM 变更 + codegen 重生成，破坏 implementation-only）。残留风险：按 actionType 单维统计 start/resolve 需走 fromStatus/toStatus——记 Follow-up（触发条件：报表需按动作类型聚合时扩字典）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 交付工单状态机 + SLA 匹配/计算/升级。完整仓库验证归 Closure Gates。

- [x] 六状态迁移（assign/start/resolve/close/reopen/cancel）合法且非法迁移抛正确 ErrorCode + TicketAction 审计写入
- [x] SLA 策略匹配 + deadline 计算（日历/工作日）+ resolve 标记 isSlaCompleted + scanOverdueTickets 升级可验证

### Phase 2 - CSAT 调查生命周期 + 端到端 + 文档/日志（工作项 3.6）

Status: completed
Targets: `ErpCsSurveyBizModel.java`；新增 support 类（如 `SurveyTokenGenerator`/`NpsClassifier`）；`ErpCsErrors`/`ErpCsConstants`；`docs/design/customer-service/README.md`/`sla.md`/`csat.md`/`state-machine.md` 实现偏离补注；`docs/logs/`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（调查在工单 RESOLVED 时触发，依赖 resolve 动作）

- [x] Add: `createSurvey(ticketId)`——工单达到 config `survey-trigger-status`(默认 RESOLVED) 时触发（config-gated `survey-enabled` + `survey-send-delay`，delay>0 时 PENDING 延迟发送），生成 `surveyToken`(UUID，无鉴权访问)，设 `surveySentAt`(delay=0 时=now)，状态派生 PENDING→SENT；唯一约束一工单一调查（重复创建抛 `ERR_SURVEY_ALREADY_EXISTS`）。
  - Skill: `nop-backend-dev`
- [x] Add: `submitSurvey(surveyToken, csatScore, npsScore?, cesScore?, comment?)`——按 token 定位（token 无效抛 `ERR_SURVEY_TOKEN_INVALID`），校验评分区间（csat 1-5/nps 0-10/ces 1-7，config-gated 各评分启用），设 `respondedAt`=now，状态→COMPLETED；NPS 分类（9-10 推荐者/7-8 被动者/0-6 贬损者）记入 scoreBreakdown 或派生查询。
  - Skill: `nop-backend-dev`
- [x] Add: 调查触发接线——Phase 1 `resolve` 动作成功后（config-gated）调 `createSurvey`；reopen 时若调查未响应则取消（删除/标记 SENT→作废，避免误发）。
  - Skill: `nop-backend-dev`
- [x] Add: 提醒/过期查询——`findSurveyReminders(reminderHours)`（SENT 且 respondedAt 空 且 sentAt + reminderHours < now，供 nop-job 提醒）；`findExpiredSurveys(expireDays)`（超期未响应，查询期标记）。
  - Skill: `nop-backend-dev`
- [x] Decision: 调查状态持久化——选"时间戳派生状态"（surveySentAt/respondedAt null 判定 PENDING/SENT/COMPLETED）而非新增 status 列（ORM 现状无 status 列，扩 ORM 归 Non-Goal）。FAILED/EXPIRED 仅查询期判定不持久。
  - Skill: `nop-backend-dev`
- [x] Proof: 端到端测试 `TestErpCsTicketSlaCsat`（JunitAutoTestCase + GraphQL）——覆盖：工单 assign→start→resolve(标 isSlaCompleted)→close 全链路 + 非法迁移拒绝；SLA 策略匹配 + deadline 计算（日历 + 工作日跳周末）；scanOverdueTickets 升级；createSurvey(RESOLVED 触发) + submitSurvey(token, csat) + NPS 分类 + 重复创建拒绝 + token 无效拒绝。指定验证命令：`mvn test -pl module-cs -am`。
  - Skill: `nop-testing`
- [x] Add: 文档对齐——`customer-service/README.md`/`sla.md`/`csat.md`/`state-machine.md` 补实现偏离补注（SLA 工作日模式仅跳周末/无工作时段窗口、仅 L1 升级、无 SlaPause 实体、Survey 状态时间戳派生、各 config 默认值）；更新 `docs/logs/{year}/{month}-{day}.md`。
  - Skill: none

Exit Criteria:

> 交付 CSAT 调查生命周期 + 端到端。完整仓库验证归 Closure Gates。

- [x] createSurvey(RESOLVED 触发) + submitSurvey(token) 评分区间校验 + NPS 分类 + 状态时间戳派生可验证
- [x] 重复调查/无效 token/超期提醒路径正确

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0d5bff3feffeSHpESnfN5Ba0WP) because `SlaPolicyMatcher` 过滤引用 `ErpCsSlaPolicy.isActive`（ORM 该实体无此列，基线未列此差距）。非阻塞建议：action-type 字典无 START/RESOLVE/REOPEN 码、escalationUserId 类型为 BIGINT、缺 auto-assign-on-create 配置。
- Independent draft review iteration 2: accept (ses_0d5bca2d6ffe9YV5XW5ygxyp5R) after 修订——基线补第三 ORM 差距（isActive 缺失 + escalationUserId BIGINT 类型注记）；matcher 去除 isActive 过滤 + Decision 记录选择/替代/触发；新增 action-type 字典 Decision（fromStatus/toStatus 承载语义，不扩字典）+ Deferred；Infrastructure 补 auto-assign-on-create 配置。所有规则 PASS，无新阻塞问题。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选后关闭。完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（3.5 工单状态机 + SLA 计时/升级 + 3.6 CSAT 调查生命周期）
- [x] 相关文档对齐（customer-service/README、sla、csat、state-machine 偏离补注）
- [x] 已运行验证：`mvn clean install -DskipTests`（全量编译）+ `mvn test -pl module-cs -am`（CS 模块测试全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### SLA 暂停/恢复机制（Pending，需 ErpCsTicketSlaPause 实体 + adjustedDeadlineDateTime）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计 §2.2 标注暂停为可选；ORM 无 SlaPause 实体与 adjustedDeadline 字段，扩 ORM 属独立面。核心 SLA 计时（deadline + isSlaCompleted + 升级）已交付。
- Successor Required: `yes`（触发条件：等待客户/第三方场景需精确暂停计时时，加 SlaPause 实体 + adjustedDeadline 列）

### 工作时段窗口 + 节假日日历精确 SLA 截止

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SlaPolicy ORM 无 workingHourStart/End 字段；首版工作日模式仅跳周末满足多数场景。
- Successor Required: `yes`（触发条件：需精确工时累计（含午休/班次）或法定节假日准确截止时，接入 master-data 节假日日历 + 扩 workingHour 字段）

### SLA L2/L3 多级升级链

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SlaPolicy ORM 无 secondEscalationUserId/escalationDelayHours；仅交付 L1 escalationUserId。
- Successor Required: `yes`（触发条件：多级升级通知/SMS 全通道需求时，扩 SlaPolicy 字段）

### SlaPolicy.isActive 启用/禁用切换

- Classification: `optimization candidate`
- Why Not Blocking Closure: ORM 无 isActive 列；首版按精确度排序取首条匹配策略，功能完整。
- Successor Required: `yes`（触发条件：需显式启用/禁用策略切换时，扩 isActive 列）

### action-type 字典扩展（START/RESOLVE/REOPEN 动作码）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 迁移语义由 fromStatus/toStatus 完整承载，审计不丢；仅按 actionType 单维统计时受影响。
- Successor Required: `yes`（触发条件：报表需按动作类型聚合时扩字典）

### nop-job cron 实际注册/调度（SLA 扫描 / 预警 / 调查提醒）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划交付可经 GraphQL/测试调用的 BizModel 方法（scanOverdueTickets/findSlaWarnings/findSurveyReminders），核心逻辑可验证；cron 注册属平台调度接线。
- Successor Required: `yes`（触发条件：生产部署需定时自动触发时，注册 nop-job 调度）

## Closure

Status Note: 已完成。3.5 工单六态状态机（assign/start/resolve/close/reopen/cancel）+ SLA 策略匹配/deadline 计算（日历 + 工作日跳周末）/超时升级 ESCALATE/预警查询 + 3.6 CSAT 调查生命周期（RESOLVED 触发创建 + token 提交 + NPS 派生分类 + 提醒/过期查询 + reopen 取消未响应调查）全部交付。implementation-only，零 ORM 变更，零核心实体污染。验证全绿：`mvn clean install -DskipTests`（全量编译）+ `mvn test -pl module-cs -am`（17 tests 全通过，含新增 `TestErpCsTicketSlaCsat` 12 tests）。文档对齐：README/sla/csat/state-machine 均补「实现偏离补注」。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_0d58416b6ffeJkqcOun7OK0oyF），非执行者
- Verdict: APPROVED_FOR_CLOSURE（8/8 closure gates PASS）
- Evidence: 
  - 验证命令实跑：`mvn test -pl module-cs/erp-cs-service -am` → 17 tests, 0 failures, BUILD SUCCESS；`mvn compile -am` → BUILD SUCCESS
  - 反模式抽检：@Inject 非 private ✓、NopException+ErrorCode ✓、IServiceContext 末参 ✓、无 @BizMutation @Transactional 组合 ✓、主实体 requireEntity ✓
  - 非阻塞观察：BizModel 内 `dao().updateEntity()` 直调（与既有 `ErpQaNonConformanceBizModel` 同范式，状态机 BizModel 显式校验场景通用，不阻断关闭）
  - 详细审计记录见本会话任务 ses_0d58416b6ffeJkqcOun7OK0oyF

Follow-up:

- SLA 暂停机制（见 Deferred，触发条件：精确暂停计时需求）
- 工作时段/节假日精确截止（见 Deferred，触发条件：精确工时/节假日需求）
- L2/L3 多级升级（见 Deferred，触发条件：多级通知需求）
- nop-job cron 注册（见 Deferred，触发条件：生产定时调度需求）
