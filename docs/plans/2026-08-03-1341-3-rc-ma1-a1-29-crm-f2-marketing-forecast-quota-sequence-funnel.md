# 2026-08-03-1341-3 rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel crm-F2 营销/预测/配额/序列/漏斗需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.29（MA1 需求追踪矩阵审计 — crm-F2 营销/预测/配额/序列/事件提醒）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.29
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.29 的 0.2 依赖）、`2026-08-03-1341-2-rc-ma1-a1-28-crm-f1-lead-lifecycle.md`（同批次 N=2，crm-F1 线索生命周期为本计划营销归因/预测/序列的依赖基础，F1 先于 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.29 给出 UC 清单 = `UC-CRM-05/07/08/10/12/14/15`（7 UC），含 `use-cases.md:92/:132/:154/:208/:269/:332/:366` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。CRM 域首个 RC 切片为 A1.28（同批次 N=2 先行），本切片为 CRM 域第二个 RC 切片。

- **L1 需求契约（权威真相源）**：`docs/design/crm/use-cases.md`（机制见 `crm/README.md`、`crm/marketing.md`、`crm/sales-forecast.md`、`crm/territory.md`、`crm/sales-sequence.md`、`crm/lead-waterfall.md`）：
  - UC-CRM-05 活动/事件记录（`:92`）：Lead.docStatus 非 CONVERTED/LOST → 创建 Event(eventType=CALL/EMAIL/MEETING/TASK, relatedLeadId, status=PLANNED)；Event.status PLANNED→COMPLETED/CANCELLED；关联 Lead 自动派生 lead.lastContactDate=max(相关 Event.startDateTime)、lead.nextActivityDate=min(相关 PLANNED Event.startDateTime)；CONVERTED lead 允许创建活动（保留历史）。
  - UC-CRM-07 UTM 营销活动归因（`:132`）：外部渠道提交携带 UTM(campaignId/utmMedium/utmSource) → 创建 Lead：lead.campaignId→匹配 ErpCrmCampaign；**lead.utmMedium→复制 campaign.medium（若未显式传入）**；**lead.utmSource→复制 campaign.source（若未显式传入）**；归因报表 `SELECT campaign.name, count(lead.id), sum(lead.expectedRevenue) FROM ErpCrmLead JOIN ErpCrmCampaign GROUP BY campaign.id`。
  - UC-CRM-08 事件提醒 Job（`:154`）：nop-job 定时 EventReminderJob → 查 ErpCrmEvent WHERE status==PLANNED AND startDateTime BETWEEN now AND (now+reminderMinutesBefore) → 对每匹配 Event 发通知（邮件/站内信）给 event.ownerId；COMPLETED/CANCELLED 不触发；cron 配置 erp-crm.event-reminder-cron（默认每小时）。
  - UC-CRM-10 销售预测生成（`:208`）：管理员建 ErpCrmForecastPeriod(periodType=MONTHLY, status=OPEN)；预测触发（商机概率变更/手动刷新/定时 Job）→ 查 OPEN 期内符合条件的商机（leadType=OPPORTUNITY, docStatus=QUALIFIED, expectedCloseDate BETWEEN periodStart AND periodEnd）→ 按 ownerId 分组聚合 weightedAmount=Σ(expectedRevenue×probability/100)、commitAmount(≥80%)、upsideAmount(30-80%)、bestCaseAmount(Σ) → 写 ErpCrmForecast + ErpCrmForecastLine + 触发上级层级聚合（**团队→区域→公司**）；期间 CLOSED 后自动算 ErpCrmForecastAccuracy（commitAccuracy=1−|commit−actual|/MAX(commit,actual)）。
  - UC-CRM-12 销售配额管理（`:269`）：管理员建 ErpCrmQuota(territoryId, teamId, ownerId, periodType, fiscalYear, periodLabel, quotaAmount)；**层级配额自动聚合：个人↑Σ团队↑Σ区域↑Σ公司**；管理员可各层级写显式配额（覆盖聚合值）；isFinalized=true 不可改（需先解冻）；报表同屏 actual vs forecast vs quota。
  - UC-CRM-14 销售序列自动分配与推进（`:332`）：管理员建 ErpCrmSequence(templateType, isActive)+ErpCrmSequenceStep(stepOrder, dueDays, activityType, completionCondition)+ErpCrmSequenceAssignment(conditionType, conditionValue, priority)；线索创建（sourceId 匹配）+ QUALIFIED → 按规则匹配序列 → 建 ErpCrmLeadSequenceProgress(currentStepIndex=0, IN_PROGRESS)；步骤推进：用户建 Event(COMPLETED)→匹配 completionCondition→currentStepIndex+=1；全完成→COMPLETED；步骤逾期（now>startedAt+ΣdueDays+gracePeriod(2天)）→标记逾期，连续逾期≥3 提醒负责人。
  - UC-CRM-15 线索漏斗分析（`:366`）：定时 Job 漏斗聚合 → 确定 analysis 期间 → 算 ErpCrmLeadFunnel（totalLeadsAtTop/totalOpportunities/totalWon/totalLost/avgSalesCycleDays）+ ErpCrmFunnelStageMetrics（每阶段 leadCountIn/conversionRate/avgDaysInStage/lostReasonTop）；前端漏斗图 stages+lostByStage。

- **L3 代码实现现状（实测）**——CRM 域并非全 stub：7 UC 中 6 UC（05/08/10/12/14/15）主路径已实现且测试强，**UC-CRM-07 UTM 归因是唯一实质性缺口**（2 项功能完全缺失）：
  - **UC-CRM-05 活动/事件记录**（✅ 已实现 & 强测）：`ErpCrmEventBizModel.java:43-178`（complete:62-66→ErpCrmEventCompleteProcessor / cancel:68-72 / getLeadTimeline:139-143→EventTimelineAggregator）；派生 `LeadActivityDerivationHelper.java:36-76 recalculateForLead`（lastContactDate=max(COMPLETED Event.startDateTime):46 + nextActivityDate=min(PLANNED Event.startDateTime):47，Event 状态变 push model）——与 L1 一致。
  - **UC-CRM-07 UTM 营销活动归因**（❌ PARTIAL——2 项功能完全缺失，最高风险缺口）：实体 `ErpCrmCampaign.java`（含 medium/source 字段）+ `ErpCrmLead` UTM 字段（utmMedium propId 23 / utmSource propId 24 / campaignId，`model/app-erp-crm.orm.xml:210-211`）存在。**缺失 #1（L1 `:142-143` UTM copy-on-create 派生完全缺失）**：`ErpCrmLeadBizModel.defaultPrepareSave:189-217` 做 duplicate-check + territory 分配但**无 campaign.medium→lead.utmMedium / campaign.source→lead.utmSource 复制**；grep `utmMedium|utmSource|setMedium|campaign` 跨 `module-crm/erp-crm-service/.../processor/` = **0 业务命中**（仅 SequenceAssignmentEngine/TerritoryAssignmentEngine 用 utmSource 作*条件匹配*字段非归因）。**缺失 #2（L1 `:144-148` 归因报表完全缺失）**：`ErpCrmCampaignBizModel.java:11-19` 为 **19 行空 CRUD**（无方法）；`ErpCrmReportBizModel.java:151-164 prepareDataset` switch 仅 handle `lead-conversion-funnel`+`forecast-accuracy`；glob `**/report/crm/*.xpt.xml` 仅 2 文件——**无 `campaign.name/count(lead)/sum(expectedRevenue) GROUP BY campaignId` 归因报表**。数据模型支撑（字段+FK 存在），仅缺派生逻辑+报表。属**代码逻辑**类修复（预授权——defaultPrepareSave 加 UTM copy 读 IErpCrmCampaignBiz + 加报表 dataset+`.xpt.xml` 模板镜像既有 funnel 模式，不涉及 ORM 结构变更）。
  - **UC-CRM-08 事件提醒 Job**（✅ 已实现 & 强测，P1-MA2-076 fixed R1.24）：`job/ErpCrmEventReminderJob.java:33-113`（cron 双门控 `erp-crm.event-reminder-cron` 空=skip + findDueReminders→per-event notify("crm.event-reminder") + 单失败隔离）；**per-event reminderMinutesBefore 已读**（P1-MA2-076 resolved）：`ErpCrmEventBizModel.findDueReminders:89-116`（扫窗口 max(window,maxPerEventReminder) 后 per-event filter effectiveReminder :107-116）；job 注册 `_vfs/erp/crm/beans/app-service.beans.xml:55`；COMPLETED/CANCELLED 正确排除（`:97` q.addFilter eq status PLANNED）；scheduler cron `app-erp-all/.../scheduler.yaml`。
  - **UC-CRM-10 销售预测生成**（✅ 已实现 & 强测）：`ErpCrmForecastBizModel.java:20-37`（refreshForecast→ErpCrmForecastRefreshForecastProcessor）；`support/ForecastAggregator.java:41-380`（load OPEN 期商机 :50-54 + 清旧 :55 + ownerId 分组 + commit≥80%/upside 30-80%/weighted/bestCase + 写 ErpCrmForecast+ForecastLine 快照 + 3 级 rollup 个人→团队→公司 + computeAccuracy()）；`job/ErpCrmForecastRecalcJob.java:35-84`（daily cron 门控）；期间状态机 FROZEN/CLOSED 拒 recalc ERR_FORECAST_PERIOD_NOT_OPEN。**偏差（documented）**：territory（区域）级 rollup tier 未实现（`sales-forecast.md:222`）——L1 `:228` 要求"团队→区域→公司"，territory tier 为 UC-12 更强要求（见下）。
  - **UC-CRM-12 销售配额管理**（⚠️ LARGELY 实现，区域 tier 缺失 + follow-up 触发条件现已满足）：`ErpCrmQuotaBizModel.java:34-121`（getQuotaRollup→QuotaRollupCalculator.rollup 显式值优先 / finalize/unfinalize / distributeAnnualQuota / getTerritoryPipeline 3 段 quota/forecast/actual）；`support/QuotaRollupCalculator.java`（company/region/team/individual 聚合 + 显式值优先，`territory.md` §实现注记 4）。**缺失 #3（L1 `:285` 区域配额 tier 未实现）**：`territory.md` §实现注记 原 Non-Goal"Lead ORM 无 territoryId 直接关联"——**但 `ErpCrmLead.territoryId`(propId 41) 后已添加**（`territory.md` §实现注记 1），**原 follow-up 触发条件现已满足** → 区域 tier rollup 缺口可能从原 accepted Non-Goal 重开为 P2（须 §4 三判据复核：territory.md 注记是否 AI 落地无人工批准 + product-scope 未裁剪区域 tier + 触发条件已满足）。
  - **UC-CRM-14 销售序列自动分配与推进**（✅ 已实现 & 强测，documented 降级 accepted）：`ErpCrmLeadSequenceProgressBizModel.java:57-310`（assignSequence/advanceStep/switchSequence/scanOverdueSteps:101-131 含 grace+连续逾期/getSequencePerformance:133-184）；engines `SequenceAssignmentEngine.java`（4 conditionType + 默认 fallback）+ `SequenceStepAdvancer.java`（5 completionCondition；EMAIL_* 降级为 eventType 匹配；TASK 映射 event-type dict）；`job/ErpCrmSequenceOverdueJob.java:32-132`（daily cron 门控→scanOverdueSteps→notify("crm.sequence-overdue")）。documented 降级：EMAIL_OPENED/REPLIED→eventType 匹配（无邮件追踪服务）；1-active-sequence（多序列 successor）——`sales-sequence.md` §实现注记 accepted。
  - **UC-CRM-15 线索漏斗分析**（✅ 已实现 & 强测，AMIS 可视化 successor）：`ErpCrmLeadFunnelBizModel.java:37-124`（refreshFunnel:49-58→ErpCrmLeadFunnelRefreshFunnelProcessor 清旧重建 / getFunnelView:60-108 可视化数据结构）；`support/FunnelAggregationEngine.java:42-458`（聚合 ErpCrmLeadConvLog+ErpCrmLead+ErpCrmStage+ErpCrmLostReason → ErpCrmLeadFunnel + ErpCrmFunnelStageMetrics）；`job/ErpCrmFunnelAggregationJob.java:27-58`（daily cron 门控→refresh 当前月全量快照）。documented Non-Goal：AMIS 漏斗前端可视化 = successor（`lead-waterfall.md:240`，后端 getFunnelView 已就绪）。

- **L4 测试证据现状**（`module-crm/erp-crm-service/src/test/`）：UC-05 `TestErpCrmEventReminderTimeline.java`（testCompleteAndCancelAndDerivation 断言状态迁移 + lastContactDate/nextActivityDate 派生 + per-event reminder——**强**）+ `TestErpCrmEventReminderDisabled`/`TestErpCrmEventPerEventReminder`；UC-08 `job/TestErpCrmEventReminderJob.java`（cron 空 skip + cron set delegate + notify 调用断言 CountingJob——**强**）；UC-10 `TestErpCrmForecastAndScoring.java`（testRefreshForecastAndRollup 断言 userA commit=1000/upside=0/best=1500/2 商机 + team rollup + ForecastLine 快照计数 + testFrozenRejectsRefresh ERR_FORECAST_PERIOD_NOT_OPEN + testClosePeriodTriggersAccuracy accuracy=1.0 + `job/TestErpCrmForecastRecalcJob`——**强**）；UC-12 `TestErpCrmTerritoryQuota.java`（10 @Test territory 树 + 4 conditionType + quota 层级 Σ + 显式值优先 + finalize + annual distribute + getTerritoryPipeline 3 段——**强**）；UC-14 `TestErpCrmSequenceAndFunnel.java`+`TestSequenceAssignmentEngine`+`TestSequenceStepAdvancer`（Progress IN_PROGRESS+stepIndex + autoCreateEvent + advanceStep + switch SKIPPED + overdue scan + 4 conditionType + 5 completionCondition——**强**）；UC-15 `TestErpCrmSequenceAndFunnel.java`(funnel section)+`TestFunnelAggregationEngine.java`（refreshFunnel 清旧重建 + getFunnelView 结构 + 每指标纯函数 + 空数据零值 + stageName 快照——**强**）。**⚠️ UC-07 零测试**（grep utmMedium/utmSource/campaign 在 test dir 仅 TerritoryQuota/SequenceAssignment 作*条件匹配*seed 非归因——UTM copy+归因报表 2 项 L1 要求零覆盖）。**⚠️ Job bean 测试缺口**：无 `TestErpCrmSequenceOverdueJob` + 无 `TestErpCrmFunnelAggregationJob`（仅 ForecastRecalc/EventReminder 有；BizModel 逻辑层已测，Job bean cron-gating execute() 入口无 dedicated 测试——P2）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：crm Event reminderMinutesBefore 死字段 **P1-MA2-076 resolved R1.24**（UC-CRM-08 直接相关——缺口已 CLOSED，实测 `ErpCrmEventBizModel:89-116` 现读 per-event reminderMinutesBefore）；P1-MA2-075 stageId 守卫（UC-06 非本切片 resolved）。
  - `docs/audits/...-arm-ma2-concurrency-optimistic-lock.md`：**P1-MA2-086**（10 cron job 含 erp-crm-event-reminder/erp-crm-sequence-overdue 在非分布式 nop-job-local 无 leader-lock 致重复 side-effects，resolved R1.28——UC-CRM-08/14 直接相关，缺口已 CLOSED）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：crm BizModel + LeadScoringEngine + ForecastAggregator + PriceRuleEngine + FunnelAggregationEngine 代码质量 PASS；P1-MA1-009（crm DECIMAL↔double，MR1 建议 P2，非本切片 Lead 实体，触 Forecast ratio 字段）。
  - `docs/audits/...-arm-ma4-crm-hr-view-xml-drift.md`（A4.8）：P2-MA4-020 crm badge 漂移 watch-only（视图层非本切片）。
  - **无既有 MA2/MA4 报告审计 UC-CRM-07 UTM 归因**——本切片新发现（无 MA2/MA4/MA5 报告标记 UTM copy 缺失或归因报表缺失）。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为（UC-CRM-08 reminder 已修 R1.24 + cron 并发已修 R1.28 + UC-10/12/14/15 引擎代码质量 PASS），只补"需求契约↔行为"差异（UC-CRM-07 UTM copy+归因报表缺失 / UC-CRM-12 区域 tier follow-up 触发条件已满足 / Job bean 测试缺口）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-075`（stageId 守卫 UC-06 非本切片 resolved R1.24）、`P1-MA2-076`（Event reminderMinutesBefore **UC-CRM-08 resolved R1.24**）、`P1-MA2-086`（cron 并发含 crm event-reminder/sequence-overdue **UC-CRM-08/14 resolved R1.28**）、`P1-MA1-009`（crm DECIMAL↔double MR1 建议 P2 触 Forecast ratio）、`P2-MA4-013`（crm Forecast stageName stub + refresh concurrency watch-only 触 UC-10/15）、`P2-MA4-020`（crm badge 漂移 watch-only 视图层）。**RC 系列对 crm 为零**（A1.29 与 A1.28 同为 CRM 域首批 RC 切片）。本切片须 grep arm-index crm activity/UTM/campaign/forecast/quota/sequence/funnel 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。UC-CRM-07 UTM copy + 归因报表修复属**代码逻辑**类（预授权——defaultPrepareSave 加 copy 读 IErpCrmCampaignBiz + 加报表 dataset+模板，不涉及 ORM 结构变更）；UC-CRM-12 区域 tier rollup 修复属**代码逻辑**类（预授权——`ErpCrmLead.territoryId` 已存在，QuotaRollupCalculator 加 territory tier，不涉及 ORM 结构变更）。须人工确认 product-scope 是否要求 UTM 归因报表与区域 tier rollup（若 L1 明确要求则 P1 强制实现）。

- **剩余差距**：A1.29 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源，且 UC-CRM-07 UTM copy+归因报表完全缺失是潜在合规风险（营销 ROI 归因无数据/无报表；UTM 字段空置致归因链断裂）。本计划产出 A1.29 报告并登记 finding，解除 CRM 域第二批 RC 切片证据缺口。

## Goals

- 产出 A1.29 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`，含方法论 §6 **9 段全部内容**。
- 对 7 UC（UC-CRM-05/07/08/10/12/14/15）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并、禁止跳号。
- 对候选缺口给出分级结论：#1 UC-CRM-07 UTM copy-on-create 派生缺失（L1 `:142-143`——倾向 **P1**）、#2 UC-CRM-07 归因报表缺失（L1 `:144-148`——倾向 **P1**）、#3 UC-CRM-12 区域 tier quota rollup 未实现 + follow-up 触发条件现已满足（L1 `:285`——倾向 **P2**，须 §4 三判据复核 territory.md Non-Goal）、#4 UC-CRM-14/15 Job bean cron-gating 测试缺失（倾向 **P2**）、UC-CRM-05/08/10/14/15 主路径已实现复核接受——按 §2 判据定级，若为 P0/P1 则新建 `P1-RC-xxx` 并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/README.md/marketing.md/sales-forecast.md/territory.md/sales-sequence.md/lead-waterfall.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.28 crm-F1 独立 plan；A1.30 crm-F3 CPQ/漏斗推进独立；A1.29 只覆盖 UC-CRM-05/07/08/10/12/14/15）。
- **不复审 UC-CRM-06 stageId 守卫**（P1-MA2-075 属 A1.30，resolved R1.24，非本切片）。
- **不复审 UC-CRM-09 线索评分**（属 A1.28，非本切片）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：UC-CRM-08 reminder/cron 由 P1-MA2-076/086 已修证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议，**§4 三判据为本切片 UC-CRM-12 复核 territory.md Non-Goal 的关键**）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.29 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.29 UC 锚点）+ `docs/design/crm/use-cases.md`（L1 真相源）+ `docs/design/crm/README.md`+`marketing.md`+`sales-forecast.md`+`territory.md`+`sales-sequence.md`+`lead-waterfall.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-crm/erp-crm-service -Dtest=TestErpCrmEventReminderTimeline,TestErpCrmForecastAndScoring,TestErpCrmTerritoryQuota,TestErpCrmSequenceAndFunnel,TestFunnelAggregationEngine`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-CRM-05/07/08/10/12/14/15 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:92/:132/:154/:208/:269/:332/:366` 验收标准原文；L2 引用 `crm/README.md`+`marketing.md`+`sales-forecast.md`+`territory.md`+`sales-sequence.md`+`lead-waterfall.md`（标注"设计参考，冲突以 L1 为准"，L2↔L1 偏差如 sales-forecast.md:222 territory rollup Non-Goal、territory.md §实现注记 4 quota rollup）；L3 引用 `ErpCrmEventBizModel.java`/`LeadActivityDerivationHelper.java`/`ErpCrmLeadBizModel.java`/`ErpCrmCampaignBizModel.java`/`ErpCrmReportBizModel.java`/`ErpCrmEventReminderJob.java`/`ForecastAggregator.java`/`ErpCrmForecastBizModel.java`/`ErpCrmQuotaBizModel.java`/`QuotaRollupCalculator.java`/`ErpCrmLeadSequenceProgressBizModel.java`/`SequenceAssignmentEngine.java`/`SequenceStepAdvancer.java`/`ErpCrmLeadFunnelBizModel.java`/`FunnelAggregationEngine.java`（含行号）；L4 引用对应 `Test*.java#method`（注明断言强度）；L5 复用 A2.14/concurrency/A4.5 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-CRM-05 Event complete/cancel + 派生（recalculateForLead:36-76 lastContactDate/nextActivityDate——已实现复核）；②UC-CRM-07 campaign/UTM 实体字段存在性（orm.xml:210-211 复核）；③#1 UC-CRM-07 UTM copy-on-create（defaultPrepareSave:189-217 无 copy，grep=0，复核缺失）；④#2 UC-CRM-07 归因报表（ErpCrmCampaignBizModel 空Crud + ErpCrmReportBizModel:151-164 仅 2 报表 + xpt.xml 仅 2 文件，复核缺失）；⑤UC-CRM-08 reminder Job + per-event reminderMinutesBefore（P1-MA2-076 resolved R1.24，复核 findDueReminders:89-116 读 per-event）；⑥UC-CRM-10 forecast 引擎 + rollup（ForecastAggregator commit≥80%/upside/bestCase + 3 级 rollup，复核 L1 借贷/聚合公式）；⑦#3 UC-CRM-12 区域 tier rollup（L1 `:285`——QuotaRollupCalculator 缺 territory tier + ErpCrmLead.territoryId 现存致 follow-up 触发条件满足，复核）；⑧UC-CRM-12 finalize/pipeline（已实现复核）；⑨UC-CRM-14 序列 assign/advance/overdue（已实现复核 + documented 降级 EMAIL_*/1-active-sequence accepted）；⑩UC-CRM-15 funnel 聚合 + getFunnelView（已实现复核 + AMIS viz successor）；⑪#4 UC-CRM-14/15 Job bean 测试缺失（grep TestErpCrmSequenceOverdueJob/TestErpCrmFunnelAggregationJob=0，复核）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#1/#2 UC-CRM-07 UTM copy+归因报表"功能完全缺失"（§2 P1①）——倾向 **P1**（须人工确认 product-scope 是否要求 UTM 归因）；#3 UC-CRM-12 区域 tier 须 §4 三判据复核 territory.md Non-Goal（触发条件 territoryId 已满足→倾向 **P2** 重开）；#4 Job bean 测试缺口倾向 **P2**；UC-CRM-05/08/10/14/15 主路径已实现则接受（含 documented 降级 accepted）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-CRM-05/07/08/10/12/14/15 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.14/concurrency/A4.5 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#4 有明确分级（非悬空"待查"）；#1/#2 UTM 归因有明确 P1 倾向 + 人工确认范围标记；#3 区域 tier 有 §4 三判据复核路径

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` crm activity/UTM/campaign/forecast/quota/sequence/funnel 同域同控制点后裁决——#1/#2 UC-CRM-07 UTM copy+归因报表为**新发现**（既有 arm-index 无 RC finding 涉及 crm UTM 归因）→ 新建 `P1-RC-xxx` 列明差异依据；#3 UC-CRM-12 区域 tier 须裁决复用 territory.md Non-Goal 注记 vs 新建（§4 三判据复核）；UC-CRM-08 已由 P1-MA2-076 resolved→复用注记（不重开）；UC-CRM-10 触 P2-MA4-013 watch-only→复用注记。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 UTM copy 缺失下 lead.utmMedium/utmSource 实际默认值、归因报表缺失下 campaignId 已填但无聚合的实际数据状态、ForecastAggregator 3 级 rollup 跨 ownerId 边界正确性、QuotaRollupCalculator 显式值优先在区域 tier 缺失下的汇总语义、FunnelAggregationJob 实际 cron 触发行为等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 P1-MA2-076 UC-CRM-08 reminder resolved R1.24）+ concurrency 报告（P1-MA2-086 cron 并发 resolved R1.28）+ A4.5（crm 引擎代码质量 PASS），列明只补的需求视角差异（UC-CRM-07 UTM copy+归因报表缺失 / UC-CRM-12 区域 tier follow-up 触发条件已满足 / Job bean 测试缺口）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_039d6fb4dffeP2cbH4iH36v0xR，fresh session，未起草本计划）。live-baseline 全量独立复核 CONFIRMED——头条缺口 UC-CRM-07（fields-exist-but-logic/report-missing）精确证实：`ErpCrmCampaignBizModel` 19 行空 CRUD + `defaultPrepareSave:189-217` 无 UTM copy（grep setUtmMedium/setUtmSource=0）+ `prepareDataset:151-164` 仅 2 报表 + glob report/crm/*.xpt.xml=2 文件 + ORM 字段（utmMedium propId23/utmSource propId24/territoryId propId41）均存在（→ P1 非 ORM 缺口）。UC-CRM-12 `ErpCrmLead.territoryId` 现存（→ territory.md Non-Goal follow-up 触发条件已满足，倾向 P2 重开）。UC-CRM-08 P1-MA2-076/086 resolved R1.24/R1.28（正确不复开）。Rule 4 单结果面（7 UC）/ anti-slack / Exit localized / Closure Gates 只读审计定制 / 方法论 §4 三判据（UC-CRM-12）+§7 reuse+§9+Q4=(a) / Deferred 诚实 全 PASS。3 项非阻塞观察（sales-forecast.md:222 引用行号 ±1 / findDueReminders 子范围引用合理 / UC-CRM-12 QuotaRollupCalculator 实际支持 territory tier 而 ForecastAggregator 不支持——计划已路由 §4 复核+P2 倾向+Phase1⑦ 复核，审计报告将澄清）已记录。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.29 报告 9 段齐全 + 7 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.29 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；UC-CRM-07 UTM copy + 归因报表 + UC-CRM-10 区域 tier rollup 修复均属**代码逻辑**类（预授权——复用既有字段/IErpCrmCampaignBiz/报表模板/ForecastAggregator territory 子树镜像 QuotaRollupCalculator，不涉及 ORM 结构变更）。#1/#2 UTM 归因 + #3' 区域 tier 须人工确认 product-scope 是否要求（若 L1 明确要求则 P1 强制实现）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；#1/#2/#3' 待人工确认 product-scope 范围）

## Closure

Status Note: 已完成（2026-08-05）。执行者（主代理 opencode）已落盘 A1.29 报告 `docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（9 段齐全 + 7 UC 五级矩阵 + 3 新 P1[P1-RC-037/038/039] + 1 新 P2[P2-RC-035]）+ 同步更新 `docs/audits/arm-index.md`（audit reports 表 + P1 详细清单 + P2 详细清单 + A1.29 RC 交叉引用注记）。**关键纠正**：计划基线 #3 候选缺口"UC-CRM-12 Quota territory tier 缺失"经实仓复核 + test 实证（testQuotaRollupExplicitValuePriorityAndAggregate:295-329 region Σ 1000+500=1500）纠正为**不成立**——`QuotaRollupCalculator.rollup:44-113` 实际支持 territory 子树聚合，territory.md §实现注记 4 字面声明 territory tier 已实现。本切片对计划假设的重要修订基于实仓代码 + test 实证，不影响其他切片结论。**新发现 #3'**：UC-CRM-10 ForecastAggregator territory tier rollup 缺失（仅 3 级，L1 `:228` 要求 4 级）+ `sales-forecast.md:222` Deferred 触发条件（Lead.territoryId）现已满足 → §4 三判据复核重开 P1（与 A1.28 P1-RC-036 同型）。**结束审计已由独立子代理（新会话，不重用执行者上下文）执行通过**——见下方 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-audit task，新会话，不重用执行者上下文 — opencode 主代理在结束审计前未参与本计划执行）
- Audit Date: 2026-08-05
- Verification Scope: 计划文件完整性 + 报告产出物存在性 + arm-index 衔接真实性 + 五点一致性 + Anti-Hollow + 只读审计合规性
- Evidence:
  - **结构检查通过**（`node .../plan-check.mjs --strict` → PASS after tick）：front matter `Plan Status: completed` + `Last Reviewed: 2026-08-05`；Phase 1/2 均 `Status: completed` 且 Exit Criteria 全 `[x]`；Closure Gates 全 `[x]`（含本次勾选的独立结束审计门控）；`## Closure` 段含具体证据非占位符。
  - **产出物存在性 CONFIRMED**：报告 `docs/audits/2026-08-05-1100-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md` 落盘（401 行，§1-§9 全部存在：§1 需求契约原文[7 UC 各一节] / §2 实现证据 / §3 测试证据 / §4 运行时行为证据 / §5 符合性结论[7 UC 结论 + 候选缺口分级汇总] / §6 arm-index 衔接[4 finding 复用/新增裁决表] / §7 静态存疑点[8 SP] / §8 过程纪律自检 / §9 与 MA2 报告差异增量）。
  - **arm-index 衔接 CONFIRMED**：`docs/audits/arm-index.md:97` 新增 audit reports 表行；`:175-178` 新增 P1-RC-037/038/039 + P2-RC-035 详细清单（含 §2 判据 / 目标 MR / 触及保护区域 / 修复路径）；`:184` A1.29 RC 交叉引用注记（含 7 UC 结论 + 关键纠正 #3 不成立 + #3' 新发现）。
  - **五点一致性 PASS**：Plan Status `completed` / Phase 1-2 `completed` / Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / Closure 证据非占位符 — 全部一致。
  - **Anti-Hollow PASS**：报告内含具体代码路径 + 行号（`ErpCrmLeadBizModel.defaultPrepareSave:196-223`、`ErpCrmReportBizModel.prepareDataset:151-164`、`ForecastAggregator.refreshForecast:50-102`、`QuotaRollupCalculator.rollup:44-113` 等）+ grep 实证结果（setUtmMedium 跨 src/main=0 业务命中 / glob `**/report/crm/*.xpt.xml`=2 文件）+ test 方法锚点（testQuotaRollupExplicitValuePriorityAndAggregate:295-329）— 非空泛描述，非占位符。
  - **只读审计合规性 PASS**：本计划 Non-Goals 明确"不修改代码/ORM/api.xml/真相源"；实仓复核 = 报告 + arm-index + 日志更新，无生产代码变更（无 `module-*/erp-*-service/src/main/` Java 文件修改、无 `*.orm.xml` / `*.api.xml` 变更）— 与计划声明一致。
  - **Deferred 诚实性 PASS**：3 P1 finding（P1-RC-037/038/039）+ 1 P2 finding（P2-RC-035）显式登记入 arm-index + 报告 §6 路由 MR1/successor，无 finding 隐藏在 Deferred But Adjudicated 段。`Deferred But Adjudicated` 段仅含"finding 修复实施"项（Classification: out-of-scope improvement，Successor Required: yes[MR0/MR1]），合规。
  - **关键纠正基于实证 PASS**：计划基线 #3 候选缺口[UC-CRM-12 territory tier]经实仓代码 + test 实证纠正为不成立 — 计划 Status Note 与报告 §5/§6/§9 一致记录此纠正，无静默范围缩小。
  - **日志同步 PASS**：`docs/logs/2026/08-05.md:5-16` 已记录 A1.29 任务、过程、产出（含 P1-RC-037/038/039 + P2-RC-035 + 关键纠正）。

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
- #1/#2 UC-CRM-07 UTM 归因 + #3' UC-CRM-10 区域 tier 须人工确认 product-scope 是否要求对应 L1 验收标准
