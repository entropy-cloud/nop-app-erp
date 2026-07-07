# 2026-07-07-1430-3-crm-sales-sequence-funnel-analysis CRM 销售序列 + 漏斗分析（UC-CRM-08/09）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` Non-Goal scope boundary（UC-CRM-08 序列管理 / UC-CRM-09 漏斗分析，归后继工作项）+ `docs/design/crm/sales-sequence.md` + `docs/design/crm/lead-waterfall.md`
> Related: `2026-07-04-0700-1-crm-event-reminder-lead-scoring-forecast.md`（CRM 活动提醒 + Lead 状态机已完成，`ErpCrmEvent` 状态机 + `ErpCrmLeadConvLog` 流转日志已存在）；`2026-07-07-1100-1-crm-territory-quota.md`（区域管理 draft，`ErpCrmTerritory` 维度漏斗可引用 territoryId）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **十实体已物化且 BizModel 为空壳**（design `sales-sequence.md`/`lead-waterfall.md` 与 ORM 精确匹配）：
  - **序列族**：`ErpCrmSequence`（模板头，`templateType` NEW_LEAD/QUALIFICATION/NEGOTIATION/RE_ENGAGEMENT、`isActive`、`isDefault`、`expectedDuration`）、`ErpCrmSequenceStep`（`stepOrder`、`dueDays`、`activityType` CALL/EMAIL/MEETING/TASK、`completionCondition`、`isMandatory`、`autoCreateEvent`）、`ErpCrmSequenceAssignment`（`priority`、`conditionType` LEAD_SOURCE/TERRITORY/PRODUCT_LINE/CUSTOM_FIELD、`conditionValue` JSON、`isDefault`）、`ErpCrmLeadSequenceProgress`（`leadId`/`sequenceId`/`currentStepIndex`/`status` IN_PROGRESS/COMPLETED/SKIPPED/`startedAt`/`completedAt`）。四 BizModel（`module-crm/erp-crm-service/.../entity/`，15 行）均为空壳。
  - **漏斗族**：`ErpCrmLeadFunnel`（物化聚合头，`periodStart`/`periodEnd`/`territoryId`/`teamId`/`sourceId`/`totalLeadsAtTop`/`totalOpportunities`/`totalWon`/`totalLost`/`totalRevenue`/`lostRevenue`/`weightedRevenue`/`avgDealSize`/`avgSalesCycleDays`/`calculatedAt`）、`ErpCrmFunnelStageMetrics`（`funnelId`/`stageId`/`stageOrder`/`stageName` 快照/`leadCountIn`/`leadCountOut`/`leadCountRemaining`/`conversionRate`/`dropOffRate`/`avgDaysInStage`/`lostCount`/`lostAmount`/`lostReasonTop` JSON）。两 BizModel 为空壳。
- **数据源实体已就绪**：`ErpCrmLeadConvLog`（阶段流转日志 `fromStageId`/`toStageId`/`changedAt`/`leadId`）、`ErpCrmStage`（阶段定义 `sequence`/`stageName`）、`ErpCrmLostReason`、`ErpCrmLead`（当前 `docStatus`/`stageId`/`lostReasonId`/`expectedRevenue`）均存在（0700-1 Lead 状态机产物）。
- **关键缺口 1 — 序列进度存储方案已定型（关联表），无须 Lead 加列**：`ErpCrmLead`（ORM 40 列）**无任何序列字段**（无 `currentStepIndex`/`currentSequenceId`/`sequenceStartedAt`）。design `sales-sequence.md`:102-113 提供两套方案，**实现已选择方案 (b)**：序列进度全部由已物化的 `ErpCrmLeadSequenceProgress` 关联表承载（`currentStepIndex`/`status` IN_PROGRESS/COMPLETED/SKIPPED/`startedAt`/`completedAt`），grep 全仓 `currentStepIndex` 仅命中该关联表（ORM:1393），`currentSequenceId`/`sequenceStartedAt` 全仓 0 命中。本期沿用关联表方案，**不加 Lead 列**（避免 ORM ask-first 保护区域 + codegen 重生成），Phase 1 记录此既定方案。
- **关键缺口 2 — 序列引擎缺失**：无序列自动分配（按 `SequenceAssignment` 规则匹配）、无步骤推进（`ErpCrmEvent.status=COMPLETED` 匹配 `completionCondition` → `currentStepIndex+1`）、无 `autoCreateEvent` 步骤建 Event。
- **关键缺口 3 — 漏斗聚合引擎缺失**：无定时 Job 从 `ErpCrmLeadConvLog`+`ErpCrmLead` 预聚合到 `ErpCrmLeadFunnel`/`ErpCrmFunnelStageMetrics`（design `lead-waterfall.md` §聚合计算流程）；无转化率/停留时间/丢失原因阶段归因计算。
- **字典口径注意**：`ErpCrmSequenceStep.activityType` 设计列 CALL/EMAIL/MEETING/TASK，但 ORM 字典 `erp-crm/activity-type` 仅含 NOTE/CALL/EMAIL/MEETING（TASK 仅在 `erp-crm/event-type`）；`SequenceStepAdvancer` 的 eventType 匹配须在实现时裁定（Phase 1 Decision：TASK 步骤映射到 event-type 或 activity-type 字典补 TASK，记入实现注记）。
- **平台范式已就绪**：CRM 域三件套经 0700-1 验证；nop-job 已接线（0306-1）支持漏斗聚合定时 Job + 序列逾期检查；通知派发（0504-1）支持序列逾期提醒。
- **菜单已生成**：`erp-crm.action-auth.xml` 已含 `crm-sequence` 与 `crm-funnel` 菜单组（CRUD 页面已生成，本期补业务行为）。
- **纯 CRM 域内部**：仅 `ErpCrmLead`/`ErpCrmEvent`/`ErpCrmStage` 同域实体引用，无跨域 I*Biz 依赖，风险低。
- **剩余差距**：序列分配/推进引擎、漏斗聚合引擎、两定时 Job、序列性能分析查询、漏斗可视化数据查询均缺失。

## Goals

- **序列分配引擎**：`SequenceAssignmentEngine`（纯函数式 + 注入加载函数便于单测）—— Lead 创建/进入 QUALIFIED 时按 `priority` 遍历 `isActive` 规则，`conditionType`（LEAD_SOURCE/TERRITORY/PRODUCT_LINE/CUSTOM_FIELD）经 `conditionValue` JSON 匹配，首个命中分配 `ErpCrmSequence`，无命中走 `isDefault`，仍无则不分配；建 `ErpCrmLeadSequenceProgress`(status=IN_PROGRESS, currentStepIndex=0)。
- **步骤推进引擎**：`SequenceStepAdvancer`（纯函数式）—— `ErpCrmEvent.status=COMPLETED` 且 `eventType` 匹配 `step.activityType` + `completionCondition` 满足 → `currentStepIndex+1`，末步完成则 status=COMPLETED + `completedAt`（`ErpCrmLeadSequenceProgress.completedAt` 列）；`autoCreateEvent=true` 步骤在分配/推进时建 `ErpCrmEvent`（排程活动，0700-1 范式）。
- **序列逾期 + 性能分析**：`IErpCrmLeadSequenceProgressBiz.scanOverdueSteps(ctx)` + nop-job 逾期检查（连续逾期 ≥ `max-overdue-steps` 提醒）；`@BizQuery getSequencePerformance(templateType, ctx)`（完成率/平均完成天数/步骤流失率，对齐 sales-sequence.md §5）。
- **漏斗聚合引擎**：`FunnelAggregationEngine`（纯函数式 + 注入加载函数便于单测）—— 从 `ErpCrmLeadConvLog`+`ErpCrmLead` 按 `periodStart`~`periodEnd` + 维度（territoryId/teamId/sourceId）聚合：LeadFunnel 头（总量/商机/赢单/丢单/收入/周期）+ FunnelStageMetrics 明细（进入/流出/剩余/转化率/流失率/停留天数/丢失原因 TOP N JSON）。`stageName` 快照防阶段定义变更致历史错误。
- **漏斗定时 Job + 查询**：`IErpCrmLeadFunnelBiz.refreshFunnel(periodStart, periodEnd, dimensions, ctx)` @BizMutation（清旧重建快照范式，对齐 0700-1 forecast）+ nop-job 每日聚合（`aggregation-cron`）+ `@BizQuery getFunnelView(funnelId, ctx)`（漏斗可视化数据结构，对齐 lead-waterfall.md §4）。
- **owner doc 收口 + 测试**：行为测试覆盖序列分配各 conditionType、步骤推进/逾期、漏斗聚合各度量、丢失原因阶段归因。

## Non-Goals

- **序列步骤的邮件跟踪集成（EMAIL_OPENED/EMAIL_REPLIED 完成条件）**：design `sales-sequence.md`:197 反模式警示——EMAIL_OPENED 需集成邮件跟踪服务。本期 `completionCondition`=EMAIL_OPENED/REPLIED 降级为 Event.status=COMPLETED + eventType=EMAIL 即视为满足（不集成邮件打开/回复跟踪）；完整邮件跟踪归 successor（触发条件：邮件跟踪服务接入时）。须 owner doc 实现注记。
- **漏斗增量实时更新（ConvLog 插入时实时更新度量子）**：design 标注为高阶方案 ⚪；本期用定时 Job 全量刷新 + 手动 refresh。
- **漏斗/序列 AMIS 可视化前端（漏斗图/序列 Kanban）**：本期后端聚合 + 查询就绪；可视化归前端 successor。
- **序列多模板并发（一 lead 同时跑多序列）**：本期一 lead 一活跃序列（切换时旧序列 SKIPPED）；多序列并发归 successor。
- **漏斗/序列报表渲染**：归报表 successor（nop-report 已接线 0504-2）。
- **CPQ（UC-CRM-07）**：归独立 successor 计划（`2026-07-07-1430-2`）。
- **区域维度漏斗依赖 1100-1 territory**：本期 `territoryId` 维度字段已就绪（`ErpCrmLeadFunnel.territoryId`），territoryId 来源可空（手动或 Lead.territoryId 若 1100-1 落地）；不强阻塞。

## Task Route

- Type: `implementation-only change`（十 BizModel 扩展 + 三纯函数式引擎 + 两定时 Job，ORM 无变更）+ 少量 `app-layer design change`（owner doc 实现注记：邮件跟踪降级 + activityType 字典裁定 + 序列进度既定方案记录）。
- Rule 14 归并判定：序列（owner doc `sales-sequence.md`）与漏斗（owner doc `lead-waterfall.md`）虽 owner doc 不同，但同属 CRM 组件、同 `nop-backend-dev` 技能、同 `mvn test -pl module-crm/erp-crm-service -am` 验证路径、同 lead 生命周期数据源（`ErpCrmLeadConvLog`/`ErpCrmLead`），Phase 间「并行安全」即互不阻塞——归并为单计划避免队列碎片化（规则 14 反碎片意图）。若后续发现闭环标准/owner-doc 义务实质分叉，再拆分。
- Owner Docs: `docs/design/crm/sales-sequence.md`（实体/规则/配置已完整）、`docs/design/crm/lead-waterfall.md`（聚合逻辑/配置已完整）、`docs/design/crm/use-cases.md`（UC-CRM-14/15）。
- Skill Selection Basis: 后端 BizModel/IBiz/ErrorCode/CrudBizModel 钩子 + 单步操作（序列分配/步骤推进/漏斗聚合各自单步，非多步编排，无需 Processor）+ 三纯函数式引擎便于单测 → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase` → 加载 `nop-testing`。两技能必需输入（sales-sequence.md/lead-waterfall.md 既有、十实体 ORM 既有、ConvLog/Stage 数据源既有）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移；无 ORM 变更（Lead 序列进度用既有 `ErpCrmLeadSequenceProgress` 关联表，不加 Lead 列）；无 codegen 增量。
- 新增配置键遵循 CRM 域范式（`ErpCrmConstants` 字符串键 + `ErpCrmConfigs` 默认值/reader，对齐 0700-1）：`erp-crm.sequence.auto-assign-on-qualify`(true)、`erp-crm.sequence.grace-period-days`(2)、`erp-crm.sequence.max-overdue-steps`(3)、`erp-crm.sequence.default-template`(NEW_LEAD)、`erp-crm.funnel.aggregation-cron`("0 0 3 * * ?")、`erp-crm.funnel.retention-period-months`(24)、`erp-crm.funnel.top-lost-reasons`(5)。
- 无新业务类型（无业财过账）。
- 回滚策略：全部改动为应用层 Java + 配置键 + scheduler.yaml，git 可逆；配置默认值与 design 一致。

## Execution Plan

### Phase 1 - 序列分配 + 步骤推进引擎 + 逾期/性能

Status: completed
Targets: `IErpCrmSequenceBiz`、`IErpCrmSequenceAssignmentBiz`、`IErpCrmLeadSequenceProgressBiz`、`SequenceAssignmentEngine`、`SequenceStepAdvancer`、`ErpCrmConstants`、`ErpCrmErrors`、`ErpCrmConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无（Lead 状态机、Event 状态机、ConvLog 已就绪）

- [x] `Decision`：序列进度存储既定方案确认 —— 序列进度由既有 `ErpCrmLeadSequenceProgress` 关联表承载（实现已选择 design 方案 b 并物化），**不**在 `ErpCrmLead` 加 `currentSequenceId`/`sequenceStartedAt` 列，因为关联表已支持多序列历史且避免 ORM ask-first 保护区域。本项为既定方案记录（非新选择），残留风险：Lead 无单字段快速查当前序列，须经关联表 latest IN_PROGRESS 查询（可加索引）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：`activityType` 字典口径 —— `ErpCrmSequenceStep.activityType` 设计列 TASK，但 ORM 字典 `erp-crm/activity-type` 仅含 NOTE/CALL/EMAIL/MEETING。**选择**在 `SequenceStepAdvancer` 将 TASK 步骤映射到 `erp-crm/event-type`（TASK 存在于该字典），不在 activity-type 字典补值（避免触动字典保护区域）；记入 `sales-sequence.md` 实现注记。**替代**：activity-type 字典补 TASK —— 触及字典保护区域，rejected。
  - Skill: `nop-backend-dev`
- [x] `Add`：`SequenceAssignmentEngine`（`module-crm/erp-crm-service/.../sequence/`）—— 纯函数式 + 注入加载函数便于单测：`assign(lead, rules, defaultRule)` 按 `priority` 遍历 isActive 规则，`ConditionMatcher` 按 conditionType 解析 conditionValue JSON 匹配 lead 字段（LEAD_SOURCE→sourceId、TERRITORY→territoryId、PRODUCT_LINE→产品线、CUSTOM_FIELD→任意字段），命中分配 Sequence + 建 LeadSequenceProgress(IN_PROGRESS, stepIndex=0)；`autoCreateEvent` 首步建 ErpCrmEvent。
  - Skill: `nop-backend-dev`
- [x] `Add`：`SequenceStepAdvancer`（纯函数式 + 注入加载函数便于单测）：`advance(progress, completedEvent, steps)` 校验 Event.status=COMPLETED + eventType 匹配 step.activityType + completionCondition 满足（CALL_COMPLETED/EMAIL_OPENED/EMAIL_REPLIED/MEETING_HELD/TASK_DONE，EMAIL_* 本期降级为 eventType 匹配，TASK 按 activityType 字典口径 Decision 映射）→ currentStepIndex+1，末步完成则 status=COMPLETED + `completedAt`；推进时 `autoCreateEvent` 建下一步 Event。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCrmLeadSequenceProgressBiz` 扩展：`@BizMutation assignSequence(leadId, ctx)`（config-gated `auto-assign-on-qualify`，调 SequenceAssignmentEngine）；`@BizMutation advanceStep(progressId, eventId, ctx)`（调 SequenceStepAdvancer）；`@BizMutation switchSequence(leadId, newSequenceId, ctx)`（旧序列 SKIPPED 快照 + 新序列 stepIndex=0）；`@BizQuery scanOverdueSteps(ctx)` + `@BizQuery getSequencePerformance(templateType, ctx)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpCrmErrors` 扩展 ErrorCode：`ERR_SEQUENCE_NO_MATCH`、`ERR_SEQUENCE_STEP_NOT_DUE`、`ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION`、`ERR_SEQUENCE_ALREADY_ASSIGNED`、`ERR_FUNNEL_PERIOD_INVALID`（中文描述 + ARG_* 参数）。`ErpCrmConstants` 配置键 + `ErpCrmConfigs` reader。
  - Skill: `nop-backend-dev`
- [x] `Add`：nop-job `ErpCrmSequenceOverdueJob` + scheduler.yaml 注册（每日逾期检查，连续逾期 ≥ max-overdue-steps 经通知派发 0504-1 提醒），双层门控对齐 0306-1。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 序列分配（四 conditionType + default 兜底 + 建 Progress）、步骤推进（completionCondition 各值 + 末步完成写 `completedAt` + autoCreateEvent 建下一步）、序列切换、逾期扫描均可观察；序列进度关联表既定方案 + activityType 字典口径 Decision 记录入实现注记。
- [x] `mvn compile -pl module-crm/erp-crm-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 2 - 漏斗聚合引擎 + 定时 Job + 查询

Status: completed
Targets: `IErpCrmLeadFunnelBiz`、`ErpCrmLeadFunnelBizModel`、`FunnelAggregationEngine`、`IErpCrmFunnelStageMetricsBiz`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（序列推进产生 ConvLog 数据，但漏斗聚合读既有 ConvLog，不强依赖 Phase 1；并行安全）

- [x] `Add`：`FunnelAggregationEngine`（`module-crm/erp-crm-service/.../funnel/`）—— 纯函数式 + 注入加载函数便于单测：`aggregate(periodStart, periodEnd, dimensions, convLogs, leads, stages, ctx)` 计算 LeadFunnel 头（totalLeadsAtTop/totalOpportunities/totalWon/totalLost/totalRevenue/lostRevenue/weightedRevenue/avgDealSize/avgSalesCycleDays）+ FunnelStageMetrics 明细（leadCountIn/Out/Remaining、conversionRate、dropOffRate、avgDaysInStage、lostCount/lostAmount、lostReasonTop TOP N JSON 聚合）；`stageName` 快照防阶段定义变更。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpCrmLeadFunnelBiz.refreshFunnel(periodStart, periodEnd, territoryId?, teamId?, sourceId?, ctx)` @BizMutation —— 清旧重建快照范式（对齐 0700-1 forecast），调 FunnelAggregationEngine 持久化 LeadFunnel + FunnelStageMetrics（upsert by funnelId+stageId）；`@BizQuery getFunnelView(funnelId, ctx)` 返回漏斗可视化数据结构（stages 数组 + conversionRate + lostReasonTop，对齐 lead-waterfall.md §4）；`IErpCrmFunnelStageMetricsBiz.@BizQuery getStageMetrics(funnelId, ctx)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：nop-job `ErpCrmFunnelAggregationJob` + scheduler.yaml 注册（`aggregation-cron` 每日凌晨全量刷新），双层门控对齐 0306-1。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 漏斗聚合（头 + 阶段明细各度量 + 丢失原因 TOP N + stageName 快照）、清旧重建、可视化查询数据结构均可观察（非空实现，空数据返回零值结构）。
- [x] `mvn compile -pl module-crm/erp-crm-service -am` 通过；行为测试在 Phase 3 统一编写。

### Phase 3 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-crm/erp-crm-service/src/test/.../TestErpCrmSequence*.java`、`TestErpCrmFunnel*.java`、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Add`：`TestSequenceAssignmentEngine`（纯单元测试）：四 conditionType 匹配 + default 兜底 + 无匹配不分配。
  - Skill: `nop-testing`
- [x] `Add`：`TestSequenceStepAdvancer`（纯单元测试）：各 completionCondition 满足/不满足、末步完成、autoCreateEvent 建下一步、EMAIL_* 降级。
  - Skill: `nop-testing`
- [x] `Add`：`TestFunnelAggregationEngine`（纯单元测试）：头度量 + 阶段明细（进入/流出/转化率/流失率/停留天数）+ 丢失原因 TOP N + stageName 快照 + 空数据零值。
  - Skill: `nop-testing`
- [x] `Add`：`TestErpCrmSequenceAndFunnel`（集成测试）：序列分配→推进→完成端到端、序列切换旧序列 SKIPPED、逾期扫描、漏斗 refreshFunnel 清旧重建 + getFunnelView 可视化结构。
  - Skill: `nop-testing`
- [x] `Proof`：`mvn test -pl module-crm/erp-crm-service -am`（含本期新增 + 0700-1/1430-2 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` Non-Goal boundary 标注 UC-CRM-08/09 已承接；`sales-sequence.md`/`lead-waterfall.md` 实现注记（序列进度关联表既定方案 + activityType 字典口径 Decision + EMAIL_* 降级 + 增量实时更新归 successor）。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试全绿（单元 + 集成）；crm-service 既有测试无回归。
- [x] 当日日志条目在位；roadmap Non-Goal boundary 标注更新。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0c50ea0e3ffeZnMkCTrkDfbEEq) — 1 项阻塞：baseline 误述 `ErpCrmLead` 含 `currentStepIndex` 但缺 `currentSequenceId`/`sequenceStartedAt`（实况：Lead 无任何序列字段，关联表方案已定型并物化，`currentStepIndex` 仅存于 `ErpCrmLeadSequenceProgress`）。非阻塞：rule 14 归并无显式判定、`sequenceCompletedAt` 实体列实为 `completedAt`、`activityType` TASK 不在 activity-type 字典。
- Independent draft review iteration 2: accept (ses_0c501866dffe2oYI2OSuABH3SV) — 阻塞项 resolved（经实时仓库复核：ErpCrmLead 无序列字段、关联表既定方案、grep 证据准确）；非阻塞项全部落实（completedAt 字段名 / activityType TASK 字典 Decision / rule 14 归并判定 / Phase 1 Item Types 准确）；清理一处重复 Skill 行。草案已达可执行契约，Plan Status → active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（序列分配 + 步骤推进 + 逾期/性能 + 漏斗聚合 + 定时 Job + 可视化查询）
- [x] 相关文档对齐（sales-sequence.md/lead-waterfall.md 实现注记、roadmap Non-Goal boundary、当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-crm/erp-crm-service -am`（0 failures / 0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（邮件跟踪/增量实时更新/可视化前端/多序列并发/报表渲染均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符（独立 closure-auditor 子代理本会话完成语义验证：见 Closure 证据）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 序列步骤邮件跟踪集成（EMAIL_OPENED/EMAIL_REPLIED 完成条件）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: design 反模式警示 EMAIL_OPENED 需集成邮件跟踪服务；本期降级为 eventType 匹配。
- Successor Required: yes（触发条件：邮件跟踪服务接入时）

### 漏斗增量实时更新（ConvLog 插入时实时更新度量子）

- Classification: `optimization candidate`
- Why Not Blocking Closure: design 标注高阶方案 ⚪；本期定时 Job 全量刷新 + 手动 refresh。
- Successor Required: yes（触发条件：实时漏斗监控业务需求上线时）

### 漏斗/序列 AMIS 可视化前端（漏斗图/序列 Kanban）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归前端 successor；本期后端聚合 + 查询就绪。
- Successor Required: yes（触发条件：CRM 前端可视化套件建立时）

### 序列多模板并发（一 lead 同时跑多序列）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期一 lead 一活跃序列；多序列并发归 successor。
- Successor Required: yes（触发条件：多序列并行跟进业务上线时）

## Closure

Status Note: 3 Phase 全部完成（序列分配/推进/切换/逾期/性能 + 漏斗聚合/可视化/定时 Job），full-green verification。`mvn clean install -DskipTests` BUILD SUCCESS（154 reactor 模块）；`mvn test -pl module-crm/erp-crm-service` Tests run: 131, Failures: 0, Errors: 0。新增 39 cases（3 单元测试 30 cases + 1 集成测试 9 cases）。所有 Non-Goal（邮件跟踪/增量实时更新/可视化前端/多序列并发/报表渲染）附触发条件归 successor。结束审计由独立子代理执行（见下）。

Closure Audit Evidence:

- Verification 命令：`mvn clean install -DskipTests`（全绿）+ `mvn test -pl module-crm/erp-crm-service`（131 tests / 0 failures / 0 errors）
- 代码路径：`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/support/{SequenceAssignmentEngine,SequenceStepAdvancer,FunnelAggregationEngine}.java` + `entity/{ErpCrmLeadSequenceProgressBizModel,ErpCrmLeadFunnelBizModel,ErpCrmFunnelStageMetricsBizModel}.java` + `job/{ErpCrmSequenceOverdueJob,ErpCrmFunnelAggregationJob}.java`
- 测试路径：`module-crm/erp-crm-service/src/test/java/app/erp/crm/service/{TestSequenceAssignmentEngine,TestSequenceStepAdvancer,TestFunnelAggregationEngine,TestErpCrmSequenceAndFunnel}.java`
- owner doc 实现注记：`docs/design/crm/sales-sequence.md` + `docs/design/crm/lead-waterfall.md`
- roadmap 标注：`docs/backlog/extended-roadmap.md` Non-Goal boundary UC-CRM-08/09 ✅ done

- Auditor / Agent: 独立 closure-auditor 子代理（新会话，不重用执行者上下文），任务分类 `verification or audit work`
- Audit Method: 逐项核对 SCRIPT_CHECK_DETAILS → 实时仓库 `glob`/`grep`/`read` 验证（非采信计划自述）：
  - 引擎三件套实存且非空壳：`SequenceAssignmentEngine`(254 行，real `assign()` 实现 priority+conditionType 匹配) / `SequenceStepAdvancer`(210 行) / `FunnelAggregationEngine`(453 行) — anti-hollow 通过
  - BizModel 行为落地：`ErpCrmLeadSequenceProgressBizModel` 五动作 (`assignSequence`/`advanceStep`/`switchSequence`/`scanOverdueSteps`/`getSequencePerformance`) + `ErpCrmLeadFunnelBizModel` 双动作 (`refreshFunnel`/`getFunnelView`) + `ErpCrmFunnelStageMetricsBizModel.getStageMetrics` grep 命中
  - 两 Job 注册：`ErpCrmSequenceOverdueJob`(132 行) + `ErpCrmFunnelAggregationJob`(58 行) 在 `app-erp-all/.../scheduler.yaml` L99/L108 双层门控 bean 注册
  - 测试覆盖：4 测试类共 39 `@Test`（11+11+8+9，与计划自述一致）
  - 文档对齐：`docs/logs/2026/07-07.md` L25-29 本计划条目 + `extended-roadmap.md` L52 UC-CRM-08/09 ✅ done + `sales-sequence.md` §实现注记 L226-231（关联表既定方案 / activityType TASK 字典口径 / EMAIL_* 降级）+ `lead-waterfall.md` §实现注记 L234-240（增量实时更新 / AMIS 可视化归 successor）
  - 五点一致性：Plan Status=completed ↔ 三 Phase Status=completed ↔ 全 Exit Criteria `[x]` ↔ Closure Gates 全 `[x]` ↔ Closure 证据在位 — 全部一致
  - Deferred honesty：邮件跟踪 / 增量实时更新 / 可视化前端 / 多序列并发 / 报表渲染均为 Non-Goal 且附触发条件，无范围内缺陷降级

Follow-up:

- 序列步骤邮件跟踪集成（见上方 Deferred）
- 漏斗增量实时更新（见上方 Deferred）
- 漏斗/序列可视化前端（见上方 Deferred）
- 序列多模板并发（见上方 Deferred）
