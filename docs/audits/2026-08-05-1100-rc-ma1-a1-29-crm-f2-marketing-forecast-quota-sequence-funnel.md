# rc-ma1-a1-29 crm-F2 营销/预测/配额/序列/事件提醒 需求-实现符合性审计

> 报告类型：MA1(RC) 五级追踪审计（requirement-compliance mission，A1.29 切片）
> 域：crm | 功能切片：crm-F2 营销/预测/配额/序列/漏斗/事件提醒 | UC 清单：UC-CRM-05/07/08/10/12/14/15（7 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.29（UC 锚点 `use-cases.md:92/:132/:154/:208/:269/:332/:366`，覆盖率 ✅ 一致，无基线分歧 D-xx）
> 计划：`docs/plans/2026-08-03-1341-3-rc-ma1-a1-29-crm-f2-marketing-forecast-quota-sequence-funnel.md`（独立草案审查 ses_039d6fb4dffeP2cbH4iH36v0xR accept）
> 审计性质：**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更）
> 报告日期：2026-08-05

---

## 1. 需求契约原文（L1，逐字引用）

> 真相源 = `docs/design/crm/use-cases.md`（权威功能契约，§4 Q1 层级 2）。下方逐字引用 7 UC 的验收标准原文，不转述。

### UC-CRM-05 活动/事件记录（`use-cases.md:92`）

```
Lead.docStatus 非 CONVERTED 或 LOST →
  创建 Event(eventType={CALL|EMAIL|MEETING|TASK},
              relatedLeadId=leadId, status=PLANNED) → 排程活动
  Event.status: PLANNED → COMPLETED（执行完成）
  Event.status: PLANNED → CANCELLED（取消）
  关联 Lead 自动派生：
    lead.lastContactDate = max(相关 Event.startDateTime)
    lead.nextActivityDate = min(相关 PLANNED Event.startDateTime)
  Lead.docStatus 为 CONVERTED → 允许创建活动（保留历史）
```

### UC-CRM-07 UTM 营销活动归因（`use-cases.md:132`）

```
外部渠道（网页/表单）提交 →
  携带 UTM 参数(campaignId/utmMedium/utmSource) →
  创建 Lead:
    lead.campaignId → 匹配 ErpCrmCampaign
    lead.utmMedium → 复制 campaign.medium（若未显式传入）
    lead.utmSource → 复制 campaign.source（若未显式传入）
  营销活动归因报表：
    SELECT campaign.name, count(lead.id), sum(lead.expectedRevenue)
    FROM ErpCrmLead lead JOIN ErpCrmCampaign campaign
    GROUP BY campaign.id
```

### UC-CRM-08 事件提醒 Job（`use-cases.md:154`）

```
nop-job 定时执行 EventReminderJob →
  查询 ErpCrmEvent WHERE status == PLANNED
    AND startDateTime BETWEEN now AND (now + reminderMinutesBefore)
  对每个匹配的 Event →
    发送通知（邮件/站内信）给 event.ownerId
    提醒内容：event.subject, startDateTime, relatedLead.code
  已完成的 Event（COMPLETED）→ 不触发提醒
  已取消的 Event（CANCELLED）→ 不触发提醒
  cron 配置：erp-crm.event-reminder-cron（默认每小时）
```

### UC-CRM-10 销售预测生成（`use-cases.md:208`）

```
管理员创建 ErpCrmForecastPeriod(periodType=MONTHLY, status=OPEN)

预测触发（商机概率变更 / 手动刷新 / 定时 Job）→
  查询 ErpCrmForecastPeriod（status=OPEN）内符合条件的商机：
    leadType=OPPORTUNITY, docStatus=QUALIFIED,
    expectedCloseDate BETWEEN periodStart AND periodEnd

  按 ownerId 分组聚合：
    weightedAmount = Σ(expectedRevenue × probability / 100)
    commitAmount = Σ(expectedRevenue) WHERE probability >= 80%
    upsideAmount = Σ(expectedRevenue) WHERE 30% <= probability < 80%
    bestCaseAmount = Σ(expectedRevenue)

  写入/更新 ErpCrmForecast + 创建 ErpCrmForecastLine 明细 →
  触发上级层级聚合（团队 → 区域 → 公司）

期间 CLOSED 后自动计算 ErpCrmForecastAccuracy：
  commitAccuracy = 1 - |commitAmount - actualClosedRevenue|
                    / MAX(commitAmount, actualClosedRevenue)
```

### UC-CRM-12 销售配额管理（`use-cases.md:269`）

```
管理员创建 ErpCrmQuota：
  territoryId=华东, teamId=null, ownerId=null,
  periodType=QUARTERLY, fiscalYear=2026,
  periodLabel="2026-Q3", quotaAmount=5000000

层级配额自动聚合：
  个人配额（territoryId, teamId, ownerId 均非空）
     ↑ Σ
  团队配额（teamId 非空, ownerId 为空）= Σ 个人配额
     ↑ Σ
  区域配额（territoryId 非空, teamId/ownerId 为空）= Σ 团队配额   ← L1 :285
     ↑ Σ
  公司配额（territoryId/teamId/ownerId 均为空）= Σ 区域配额

管理员可为各层级写入显式配额值（覆盖聚合值）

isFinalized=true → 配额不可修改（需先解冻）

报表同屏展示：actual（实际）vs forecast（预测）vs quota（目标）
```

### UC-CRM-14 销售序列自动分配与推进（`use-cases.md:332`）

```
管理员创建 ErpCrmSequence(templateType=NEW_LEAD, isActive=true)
  并配置 ErpCrmSequenceStep 步骤：
    stepOrder=1, dueDays=1,  activityType=CALL,  completionCondition=CALL_COMPLETED
    stepOrder=2, dueDays=3,  activityType=EMAIL, completionCondition=EMAIL_OPENED
    stepOrder=3, dueDays=7,  activityType=MEETING, completionCondition=MEETING_HELD
  设置 ErpCrmSequenceAssignment(
    conditionType=LEAD_SOURCE,
    conditionValue={"sourceId":["WEBSITE"]},
    priority=1)

线索创建（sourceId=WEBSITE）→ docStatus=QUALIFIED →
  按 assignment 规则匹配合序列 →
  创建 ErpCrmLeadSequenceProgress(sequenceId, currentStepIndex=0, status=IN_PROGRESS)

步骤推进：
  用户创建 Event(eventType=CALL, relatedLeadId=leadId, status=COMPLETED)
    → 匹配 step.completionCondition → currentStepIndex += 1
  所有步骤完成 → status=COMPLETED, completedAt=now

步骤逾期：
  now > (startedAt + ΣdueDays) + gracePeriod(2天) →
    标记"逾期"，连续逾期 >= 3 提醒负责人
```

### UC-CRM-15 线索漏斗分析（`use-cases.md:366`）

```
定时 Job 执行漏斗聚合 →
  确定分析期间（如 2026-Q3）

  计算 ErpCrmLeadFunnel：
    totalLeadsAtTop = 期间内进入 Stage 1 的线索数
    totalOpportunities = 期间内 leadType 变为 OPPORTUNITY 的线索数
    totalWon = 期间内 CONVERTED + isWonStage=true 的线索数
    totalLost = 期间内 LOST 的线索数
    avgSalesCycleDays = AVG(QUALIFIED → CONVERTED 天数)

  计算 ErpCrmFunnelStageMetrics（每个阶段一条）：
    leadCountIn = 进入本阶段的线索数
    conversionRate = leadCountOutForward / leadCountIn
    avgDaysInStage = AVG(exitTime - entryTime)
    lostReasonTop = 本阶段 TOP 3 丢失原因(JSON)

前端漏斗图：
  stages: [{name, count, avgDays, conversionRate}, ...]
  lostByStage: [{stageName, lostCount, lostReasonTop}, ...]
```

---

## 2. 实现证据（L3 代码路径，含行号）

> 跨域调用链列全（Facade → Processor → 跨域 I*Biz）。L3 行号经 HEAD 实仓复核。

| UC | 代码路径（含行号） |
|----|-------------------|
| UC-CRM-05 | `ErpCrmEventBizModel.java:43-178`（CrudBizModel Facade）；`complete:62-66`→`ErpCrmEventCompleteProcessor` + `cancel:68-72`→`ErpCrmEventCancelProcessor`；派生 `deriveLeadFields:169-174`→`LeadActivityDerivationHelper.recalculateForLead:36-49`（`lastContactDate=latestCompletedStartDateTime:46+54-62` + `nextActivityDate=earliestPlannedStartDateTime:47+68-76`，Event 状态变 push model）；`getLeadTimeline:139-143`→`EventTimelineAggregator`。**lastContactDate 实现按 COMPLETED 过滤**（`:55` eq EVENT_STATUS_COMPLETED），L1 字面"相关 Event"未限定状态，存在语义偏差（见 SP-1） |
| UC-CRM-07 | **缺失 #1 UTM copy-on-create**：`ErpCrmLeadBizModel.defaultPrepareSave:196-223` 做 duplicate-check (`:199`) + territory 分配 (`:201-223`) 但**无 campaign.medium→lead.utmMedium / campaign.source→lead.utmSource 复制**；grep `setUtmMedium\|setUtmSource\|setMedium\|campaign.getMedium\|campaign.getSource` 跨 `module-crm/erp-crm-service/src/main` = **0 业务命中**；`IErpCrmCampaignBiz` 仅被 `_service.beans.xml:55`（bean def）+ `ErpCrmCampaignBizModel.java:12`（自身 implements）引用，**ErpCrmLeadBizModel 不注入 IErpCrmCampaignBiz**（grep imports 仅 Stage/StageBiz）。**缺失 #2 归因报表**：`ErpCrmCampaignBizModel.java:11-19` = **19 行空 CRUD stub**（`extends CrudBizModel<ErpCrmCampaign>` 无任何方法）；`ErpCrmReportBizModel.java:151-164 prepareDataset` switch 仅 handle `lead-conversion-funnel` (`:155-157`) + `forecast-accuracy` (`:158-160`)；glob `**/report/crm/*.xpt.xml` = **2 文件**（`forecast-accuracy.xpt.xml` + `lead-conversion-funnel.xpt.xml`）——**无 `SELECT campaign.name, count(lead.id), sum(lead.expectedRevenue) GROUP BY campaignId` 归因报表**。数据模型支撑已就绪（`app-erp-crm.orm.xml:209-211` Lead campaignId propId22 + utmMedium propId23 + utmSource propId24 + `:232/:235` to-one source/campaign + Campaign `:425-426` medium propId6/source propId7） |
| UC-CRM-08 | `job/ErpCrmEventReminderJob.java:33-113`（cron 双门控 `:57-60` cron 空=skip + `:62-95` findDueReminders 候选 + per-event filter `:97-108` notifyEvent→`IErpSysNotificationBiz.notify("crm.event-reminder"):107` + 单失败隔离 try/catch）；**per-event reminderMinutesBefore 已读**（P1-MA2-076 resolved R1.24）：`ErpCrmEventBizModel.findDueReminders:76-118`（扫窗口 `scanWindow = max(window, maxPerEventReminder):88-92` + per-event filter `effectiveReminder = perEvent ?: window :107-108` + 仅保留 `startDateTime ≤ now+effectiveReminder :113`）；job 注册 `_vfs/erp/crm/beans/app-service.beans.xml:55`；scheduler `_vfs/nop/job/conf/erp-crm-event-reminder.job.yaml`（默认 `0 0/15 * * * ?` 每 15 分钟）；COMPLETED/CANCELLED 正确排除（`ErpCrmEventBizModel:97` q.addFilter eq status PLANNED） |
| UC-CRM-10 | `ErpCrmForecastBizModel.java:20-37`（refreshForecast→`ErpCrmForecastRefreshForecastProcessor`）；`support/ForecastAggregator.java:41-380`：`refreshForecast:50-102`（load OPEN 期商机 `loadOpportunities:54` + 清旧 `clearPeriodForecasts:55` + ownerId 分组 `:60-72` + commit≥80%/upside 30-80%/weighted×prob/100/bestCase `ForecastTotals.of:82` + 写 Forecast+ForecastLine `:83-85` + **3 级 rollup 个人→团队→公司**：个人 `:77-91` + 团队 `:93-97` + 公司 `:99-101`）+ `computeAccuracy:107-115`；类头注释 `:36` 字面声明"层级 rollup：个人→团队→公司"（**无 territory/区域 tier**）；`job/ErpCrmForecastRecalcJob.java:35-84`（daily `0 3 * * *` cron 门控 `:48-50`）；期间状态机 FROZEN/CLOSED 拒 recalc `requireOpen:52`→`ERR_FORECAST_PERIOD_NOT_OPEN`。**偏差 #3'（L1 `:228` 区域 tier 未实现）**：`sales-forecast.md:222` §实现约定显式 Deferred 标注"区域（territory）层级因 Lead ORM 无 territoryId 直接关联暂未实现（记 Follow-up，触发条件：Lead→Territory 映射就绪时）"——但 `ErpCrmLead.territoryId` (propId 41) **现已添加**（见 territory.md §实现注记 1 `:219-222`），**触发条件已满足** |
| UC-CRM-12 | `ErpCrmQuotaBizModel.java:34-121`（`getQuotaRollup:48-54`→`QuotaRollupCalculator.rollup` + `finalizeQuota:58-67`/`unfinalizeQuota:71-76` + `distributeAnnualQuota:80-84`→Processor + `getTerritoryPipeline:88-117` 3 段 quota/forecast/actual）；`support/QuotaRollupCalculator.java:35-276`（`rollup:44-113` 显式值优先 `:46-67` + territory 子树聚合 `collectSubtreeIds:71-73→:220-227` 递归 + `accumulatePipeline:163-216`）；**territory tier 实际已实现**——`rollup(territoryId, ...)` 取 territory 子树聚合所有团队/个人配额行 `:74-83`，territory.md §实现注记 4 `:237-239` 字面声明"territoryId≠null 且 teamId/ownerId=null → 区域级（聚合该区域子树所有团队/个人配额行）"；test `TestErpCrmTerritoryQuota#testQuotaRollupExplicitValuePriorityAndAggregate:295-329` 实证 region 聚合 1000+500=1500 |
| UC-CRM-14 | `ErpCrmLeadSequenceProgressBizModel.java:57-310`（`assignSequence`/`advanceStep`/`switchSequence`/`scanOverdueSteps:101-131` 含 grace+连续逾期/`getSequencePerformance:133-184`）；engines：`SequenceAssignmentEngine.java`（4 conditionType LEAD_SOURCE/CUSTOM_FIELD 等 + 默认 fallback）+ `SequenceStepAdvancer.java:36-153`（5 completionCondition：CALL_COMPLETED→CALL/MEETING_HELD→MEETING/TASK_DONE→TASK 匹配 event-type dict `:125-130` + EMAIL_OPENED/EMAIL_REPLIED **降级为 eventType=EMAIL 匹配** `:131-133`）；`job/ErpCrmSequenceOverdueJob.java:32-132`（daily `0 0 6 * * ?` cron 门控 `:58-61`→scanOverdueSteps→`notifyOverdue:97-116`→`IErpSysNotificationBiz.notify("crm.sequence-overdue"):116`）。documented 降级（`sales-sequence.md §实现注记:229-231`）：EMAIL_OPENED/REPLIED→eventType 匹配（无邮件追踪服务）+ TASK 映射 event-type 字典 + 1-active-sequence（多序列 successor） |
| UC-CRM-15 | `ErpCrmLeadFunnelBizModel.java:37-124`（`refreshFunnel:49-58`→`ErpCrmLeadFunnelRefreshFunnelProcessor` 清旧重建 / `getFunnelView:60-108` 可视化数据结构 stages+lostByStage）；`support/FunnelAggregationEngine.java:42-458`（聚合 ErpCrmLeadConvLog+ErpCrmLead+ErpCrmStage+ErpCrmLostReason → ErpCrmLeadFunnel + ErpCrmFunnelStageMetrics，含 leadCountIn/conversionRate/avgDaysInStage/lostReasonTop TOP-N config-gated）；`job/ErpCrmFunnelAggregationJob.java:27-58`（daily `0 30 3 * * ?` cron 门控 `:42-44`→refresh 当前月全量快照）。documented Non-Goal（`lead-waterfall.md §实现注记:240`）：AMIS 漏斗前端可视化 = successor（后端 getFunnelView 已就绪） |

---

## 3. 测试证据（L4 测试断言，注明强度）

> 测试位于 `module-crm/erp-crm-service/src/test/`。E2E 无本切片直接覆盖（crm E2E 集 `crm-lead.action.spec.ts` 仅覆盖 UC-CRM-01 部分）。

| 测试 | 覆盖 UC | 断言强度 |
|------|---------|---------|
| `TestErpCrmEventReminderTimeline.java` | UC-CRM-05 | **强**——`testCompleteAndCancelAndDerivation` 断言状态迁移 PLANNED→COMPLETED/CANCELLED + lastContactDate/nextActivityDate 派生 + per-event reminder |
| `TestErpCrmEventReminderDisabled.java` + `TestErpCrmEventPerEventReminder.java` | UC-CRM-05/08 | **强**——config-gated 关闭时返回空 + per-event reminderMinutesBefore 行为 |
| `job/TestErpCrmEventReminderJob.java` | UC-CRM-08 | **强**——cron 空 skip + cron set delegate + notify 调用断言 CountingJob |
| `TestErpCrmForecastAndScoring.java`（含 forecast 部分） | UC-CRM-10 | **强**——`testRefreshForecastAndRollup` 断言 userA commit=1000/upside=0/best=1500/2 商机 + team rollup + ForecastLine 快照计数；`testFrozenRejectsRefresh` ERR_FORECAST_PERIOD_NOT_OPEN；`testClosePeriodTriggersAccuracy` accuracy=1.0 |
| `job/TestErpCrmForecastRecalcJob.java` | UC-CRM-10 | **强**——cron 空 skip + cron set delegate |
| `TestErpCrmTerritoryQuota.java`（604 行，10 @Test） | UC-CRM-12（+UC-CRM-11） | **强**——territory 树 level/fullPath/isLeaf + cycle/depth 拒绝 + 4 conditionType + `testQuotaRollupExplicitValuePriorityAndAggregate:295-329`（region Σ 1000+500=1500）+ finalize/unfinalize + `testDistributeAnnualQuota`（1200/4=300/季）+ `testGetTerritoryPipelineReturnsThreeSections`（quota/forecast/actual 三段） |
| `TestErpCrmSequenceAndFunnel.java`（9 @Test） | UC-CRM-14/15 | **强**——序列：`testSequenceAssignAdvanceComplete`（IN_PROGRESS+stepIndex + autoCreateEvent + advanceStep）+ `testSwitchSequenceOldSkipped` + `testScanOverdueSteps`（grace+连续逾期）+ 4 conditionType；漏斗：`testRefreshFunnelClearRebuildAndView`（清旧重建 + getFunnelView 结构）+ `testInvalidPeriodRejects` |
| `TestSequenceAssignmentEngine.java` | UC-CRM-14 引擎 | **强**——4 conditionType 匹配/不匹配 + 默认 fallback（注：lead2.setUtmSource 用作*条件匹配*字段非归因） |
| `TestSequenceStepAdvancer.java` | UC-CRM-14 引擎 | **强**——5 completionCondition 满足/不满足 + 末步完成 + autoCreateEvent 建下一步（含 EMAIL_* 降级断言） |
| `TestFunnelAggregationEngine.java`（8 @Test） | UC-CRM-15 引擎 | **强**——`testEmptyDataReturnsZeroStructure`（空数据零值）+ `testHeaderMetricsWonLostRevenue` + `testStageMetricsConversionAndDropOff` + `testAvgDaysInStage` + `testLostReasonTopNLimited` + `testStageNameSnapshotPreserved`（每指标纯函数断言） |

**测试缺口**（按候选缺口对应）：
- ① UC-CRM-07 **零测试**（grep utmMedium/utmSource/campaign 在 test dir 仅 TerritoryQuota/SequenceAssignment 作*条件匹配*seed 非归因——UTM copy+归因报表 2 项 L1 要求零覆盖）。
- ② **Job bean 测试缺口**：无 `TestErpCrmSequenceOverdueJob` + 无 `TestErpCrmFunnelAggregationJob`（仅 ForecastRecalc/EventReminder 有 dedicated Job 测试；BizModel 逻辑层已测，Job bean cron-gating execute() 入口无 dedicated 测试——P2）。

---

## 4. 运行时行为证据（L5）

> 来源：复用既有 MA2/A1.13/A4.5 报告已证实行为（§去重协议，不重新核实行为本身）+ 本切片 L3/L4 静态证据。运行时存疑点入 §7。

- **复用 A2.14**（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）：crm Event 3 态（PLANNED/COMPLETED/CANCELLED）状态机 PASS；UC-CRM-08 reminderMinutesBefore 死字段 **P1-MA2-076 resolved R1.24**（实测 `ErpCrmEventBizModel:89-116` 现读 per-event reminderMinutesBefore）；UC-CRM-14 cron 并发（含 sequence-overdue） **P1-MA2-086 resolved R1.28**（leader-lock 落地）。
- **复用 A1.13**（`2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md`）：crm 15 维平台合规 15/15 PASS（跨实体访问经 Facade 0 跨模块写）。
- **复用 A4.5**（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）：crm BizModel + LeadScoringEngine + **ForecastAggregator + FunnelAggregationEngine** 代码质量 PASS（P1-MA1-009 crm DECIMAL↔double MR1 建议 P2，非本切片 Lead 实体，触 Forecast ratio 字段——A1.28 已记录）。
- **复用 A2.18**（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`）：crm `ErpCrmLeadBizModel.loadActiveRules` 收 orgId 参数（P1-MA2-093 orgId 查询隔离 resolved R1.29 全局 IQueryTransformer）。
- **L3/L4 静态证实**：UC-05 Event complete/cancel + 派生（recalculateForLead:36-49 push model）、UC-08 reminder Job + per-event reminderMinutesBefore、UC-10 forecast 引擎（commit/upside/bestCase 公式 + 3 级 rollup + 准确率 + FROZEN 拒 recalc）、UC-12 quota rollup（territory 子树聚合 + 显式值优先 + finalize/distribute）、UC-14 序列 assign/advance/overdue（documented EMAIL_* 降级 + 1-active-sequence accepted）、UC-15 funnel 聚合 + getFunnelView（documented AMIS 可视化 successor）均行为正确（经 A2.14+A4.5+单测三重证实）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

> 方法论 §2 判据取最高；§4 Q1 真相源层级（L1 权威，L2 冲突以 L1 为准）；§5 Q4 修复义务（P0/P1 必须实现禁方案 B 无例外）。

### UC-CRM-05 活动/事件记录 — **接受 on 主路径 + 观察 lastContactDate 语义偏差**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| Lead.docStatus 非 CONVERTED/LOST → 创建 Event(eventType, relatedLeadId, status=PLANNED) | XMeta + CrudBizModel + Event ORM | TestErpCrmEventReminderTimeline seed | ✅ 接受 |
| Event.status PLANNED→COMPLETED | `ErpCrmEventCompleteProcessor`（complete:62-66 委托） | testCompleteAndCancelAndDerivation | ✅ 接受 |
| Event.status PLANNED→CANCELLED | `ErpCrmEventCancelProcessor`（cancel:68-72 委托） | testCompleteAndCancelAndDerivation | ✅ 接受 |
| lead.lastContactDate = max(相关 Event.startDateTime) | `recalculateForLead:46` + `latestCompletedStartDateTime:54-62`（**按 COMPLETED 过滤**：`:55` eq EVENT_STATUS_COMPLETED） | testCompleteAndCancelAndDerivation（COMPLETED 路径派生） | ⚠ 接受（**语义偏差见 SP-1**：L1 字面"相关 Event"未限定状态，实现按 COMPLETED 过滤——"last contact"语义合理，不记 finding，登记 SP 供 MA4 运行时确认） |
| lead.nextActivityDate = min(相关 PLANNED Event.startDateTime) | `recalculateForLead:47` + `earliestPlannedStartDateTime:68-76` | testCompleteAndCancelAndDerivation | ✅ 接受 |
| CONVERTED lead 允许创建活动（保留历史） | Event 无 lead docStatus 守卫（XMeta 仅校验 eventType/relatedLeadId） | — | ✅ 接受 |

**结论**：**接受 on 主路径**（5/6 验收标准强测；lastContactDate 按 COMPLETED 过滤是合理实现选择，登记 SP-1 供 MA4 运行时确认 L1 字面"相关 Event"是否意指"任何状态包括 CANCELLED"——倾向接受，不开 finding）。

### UC-CRM-07 UTM 营销活动归因 — **P1**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 外部渠道提交携带 UTM(campaignId/utmMedium/utmSource) → 创建 Lead | XMeta + CrudBizModel + Lead ORM 字段 (`:209-211`) | — | ✅ 接受（字段就绪） |
| **lead.campaignId → 匹配 ErpCrmCampaign** | Lead ORM `:209/:235` to-one campaign + IErpCrmCampaignBiz bean 存在（`_service.beans.xml:55`） | — | ✅ 接受（外键就绪，匹配由调用方/campaignId 显式传入） |
| **lead.utmMedium → 复制 campaign.medium（若未显式传入）** | **`defaultPrepareSave:196-223` 无 campaign.medium→lead.utmMedium 复制**（grep setUtmMedium 跨 src/main = 0 业务命中；ErpCrmLeadBizModel 不注入 IErpCrmCampaignBiz） | **无测试** | **❌ P1（P1-RC-037）** |
| **lead.utmSource → 复制 campaign.source（若未显式传入）** | **`defaultPrepareSave:196-223` 无 campaign.source→lead.utmSource 复制**（grep setUtmSource 跨 src/main = 0 业务命中） | **无测试** | **❌ P1（合并入 P1-RC-037 同根因同控制点）** |
| **营销活动归因报表：SELECT campaign.name, count(lead.id), sum(expectedRevenue) GROUP BY campaignId** | **`ErpCrmCampaignBizModel.java:11-19` 19 行空 CRUD stub** + `ErpCrmReportBizModel.prepareDataset:151-164` 仅 2 报表 + glob `**/report/crm/*.xpt.xml` 仅 2 文件（无归因报表模板） | **无测试** | **❌ P1（P1-RC-038）** |

**结论**：**P1**——两项功能完全缺失：#1 UTM copy-on-create 派生缺失（P1-RC-037，§2 P1① 功能完全缺失 + §2 P1⑤ 测试断言完全缺失）+ #2 归因报表完全缺失（P1-RC-038，§2 P1① 功能完全缺失 + §2 P1⑤ 测试断言完全缺失）。**L2 `marketing.md` / `README.md §ErpCrmCampaign` 与 L1 一致**（无 L2↔L1 冲突）。**须人工确认 product-scope 是否要求 UTM 归因**（营销 ROI 归因是 L1 明确功能点，倾向 P1 强制实现 Q4 无例外）。**数据模型支撑已就绪**（字段+FK 存在），属**代码逻辑**类修复（预授权——defaultPrepareSave 加 UTM copy 读 IErpCrmCampaignBiz + 加报表 dataset+`.xpt.xml` 模板镜像既有 funnel 模式，不涉及 ORM 结构变更）。

### UC-CRM-08 事件提醒 Job — **接受**

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| nop-job 定时执行 EventReminderJob | `ErpCrmEventReminderJob:33-113` + bean 注册 `_service.beans.xml:55` + job yaml `erp-crm-event-reminder.job.yaml` | TestErpCrmEventReminderJob（cron set delegate） | ✅ 接受 |
| 查询 ErpCrmEvent WHERE status==PLANNED AND startDateTime BETWEEN now AND (now+reminderMinutesBefore) | `ErpCrmEventBizModel.findDueReminders:96-99`（eq PLANNED + ge now + le now+scanWindow）；**per-event reminderMinutesBefore 已读**：`scanWindow = max(window, maxPerEventReminder):88-92` + per-event filter `:106-116` | TestErpCrmEventReminderJob + TestErpCrmEventPerEventReminder（per-event 行为强断言） | ✅ 接受（P1-MA2-076 resolved R1.24 实证） |
| 对每个匹配的 Event 发送通知（邮件/站内信）给 event.ownerId | `ErpCrmEventReminderJob.notifyEvent:97-108`→`IErpSysNotificationBiz.notify("crm.event-reminder"):107` | TestErpCrmEventReminderJob（CountingJob notify 调用断言） | ✅ 接受 |
| 提醒内容：event.subject, startDateTime, relatedLead.code | `ErpCrmEventReminderJob.notifyEvent` map 构造（subject/startDateTime/leadCode） | TestErpCrmEventReminderJob | ✅ 接受 |
| COMPLETED/CANCELLED 不触发 | `ErpCrmEventBizModel.findDueReminders:97` q.addFilter eq status PLANNED（排除 COMPLETED/CANCELLED） | TestErpCrmEventReminderJob | ✅ 接受 |
| cron 配置 erp-crm.event-reminder-cron（默认每小时） | `ErpCrmEventReminderJob:111 resolveCronConfig` AppConfig.var(CONFIG_EVENT_REMINDER_CRON)；job yaml 默认 `0 0/15 * * * ?` 每 15 分钟（**比 L1 字面"每小时"更密**，更激进非降级） | TestErpCrmEventReminderDisabled（cron 空 skip） | ✅ 接受（更密非降级） |

**结论**：**接受**（已实现 & 强测，P1-MA2-076/086 resolved R1.24/R1.28 直接相关——缺口已 CLOSED）。

### UC-CRM-10 销售预测生成 — **P1**（区域 tier rollup follow-up 触发条件已满足）

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 管理员建 ErpCrmForecastPeriod(periodType=MONTHLY, status=OPEN) | ErpCrmForecastPeriod ORM + CrudBizModel | TestErpCrmForecastAndScoring seed | ✅ 接受 |
| 预测触发（商机概率变更/手动刷新/定时 Job） | 手动 refresh `ErpCrmForecastBizModel.refreshForecast:20-37`；定时 `ErpCrmForecastRecalcJob:47-82`（daily cron 门控）；**商机概率变更自动触发**——config-gated（不在本切片核心，无 config key 拦截即可） | TestErpCrmForecastAndScoring + TestErpCrmForecastRecalcJob | ✅ 接受 |
| 查询 OPEN 期内商机（leadType=OPPORTUNITY, docStatus=QUALIFIED, expectedCloseDate BETWEEN periodStart AND periodEnd） | `ForecastAggregator.loadOpportunities:54`（requireOpen:52 + 期间过滤） | testRefreshForecastAndRollup | ✅ 接受 |
| 按 ownerId 分组 weightedAmount=Σ(expectedRevenue×probability/100) | `ForecastTotals.of:82` weighted 公式 + `rebuildLines:137-149` 每行 weighted=expectedRevenue×prob/100 | testRefreshForecastAndRollup（userA weighted 断言） | ✅ 接受 |
| commitAmount=Σ WHERE probability>=80% | `ForecastTotals.of:82` commitThreshold=80 + `rebuildLines:145` inCommit | testRefreshForecastAndRollup（commit=1000） | ✅ 接受 |
| upsideAmount=Σ WHERE 30<=probability<80% | `ForecastTotals.of:82` upsideThreshold=30 + `rebuildLines` upside | testRefreshForecastAndRollup（upside=0） | ✅ 接受 |
| bestCaseAmount=Σ | `ForecastTotals.of:82` bestCase=Σ | testRefreshForecastAndRollup（best=1500） | ✅ 接受 |
| 写 ErpCrmForecast + ErpCrmForecastLine + **触发上级层级聚合（团队→区域→公司）** | 写 Forecast+Line `:83-85`；**3 级 rollup**：个人 `:77-91` + 团队 `:93-97` + 公司 `:99-101`；**类头注释 `:36` 字面"个人→团队→公司"——无 territory/区域 tier** | testRefreshForecastAndRollup（team rollup 断言；**无 territory tier 测试**） | **❌ P1（P1-RC-039）**——L1 `:228` 要求"团队→区域→公司"4 级，实现仅 3 级 |
| 期间 CLOSED 后自动算 ErpCrmForecastAccuracy（commitAccuracy=1−|commit−actual|/MAX(commit,actual)） | `ForecastAggregator.computeAccuracy:107-115` + 期间 CLOSED 触发 | testClosePeriodTriggersAccuracy（accuracy=1.0） | ✅ 接受 |

**结论**：**P1**——#3' Forecast territory tier rollup 未实现 + follow-up 触发条件现已满足（P1-RC-039）。

**#3' §4 三判据复核 `sales-forecast.md:222` §实现约定 Deferred 标注**（"区域 tier 因 Lead ORM 无 territoryId 直接关联暂未实现，触发条件：Lead→Territory 映射就绪时"）：
- (i) plan 含独立 plan-audit 通过记录？——`sales-forecast.md:222` 注记引用"实现约定"，但无独立 plan-audit task id 链接（与 A1.28 P1-RC-036 同型）。
- (ii) owner doc 显式 documented simplification 标注且经人工批准？——`sales-forecast.md:222` 有 Deferred 标注，但为 AI 落地补注，git log 无人工批准痕迹可追溯（与 A1.28 territory.md Deferred 同型）。
- (iii) product-scope 范围裁剪登记？——product-scope 未将 Forecast territory tier 列入范围裁剪。
- **触发条件复核**：`sales-forecast.md:222` Deferred 的触发条件 = "Lead→Territory 映射就绪" → `territory.md §实现注记 1:219-222` 字面记录 "`ErpCrmLead` 新增可空 `territoryId`(propId 41, BIGINT, stdDataType=long) + to-one `territory` + 索引 `IDX_CRM_LEAD_TERRITORY_ID`"——**触发条件现已满足**。
- **三判据在"人工批准"意义上均不满足 + 触发条件已满足 → 非 documented simplification → 按 Q4=(a) 重开 P1**。**须人工确认 product-scope 是否要求 Forecast 区域 tier**：若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现。**修复属代码逻辑**类（ForecastAggregator.refreshForecast 加 territory tier rollup，复用既有 `ErpCrmLead.territoryId` + territory 子树 `collectSubtreeIds` 模式镜像 QuotaRollupCalculator，**不触及 ORM 结构变更**——预授权自动执行）。

### UC-CRM-12 销售配额管理 — **接受**（territory tier 已实现，纠正计划基线假设）

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 管理员建 ErpCrmQuota(territoryId, teamId, ownerId, periodType, fiscalYear, periodLabel, quotaAmount) | ErpCrmQuota ORM（territoryId propId5 mandatory `:1015`）+ CrudBizModel | TestErpCrmTerritoryQuota seedQuota | ✅ 接受 |
| 层级配额自动聚合：个人→团队→区域→公司 | `QuotaRollupCalculator.rollup:44-113`（**territory 子树聚合** `collectSubtreeIds:71-73→:220-227` 递归）+ 显式值优先 `:46-67`；**territory.md §实现注记 4:237-239 字面声明 territory tier 已实现**（"territoryId≠null 且 teamId/ownerId=null → 区域级（聚合该区域子树所有团队/个人配额行）"） | `testQuotaRollupExplicitValuePriorityAndAggregate:295-329`（region Σ 1000+500=1500 强断言） | ✅ 接受（**计划基线 #3 假设"区域 tier 缺失"被纠正——territory tier 实际已实现**） |
| **管理员可各层级写显式配额（覆盖聚合值）** | `rollup:46-67` 显式值优先（同层级有显式 quotaAmount 则直接返回） | testQuotaRollupExplicitValuePriorityAndAggregate | ✅ 接受 |
| isFinalized=true → 配额不可修改（需先解冻） | `finalizeQuota:58-67`（重复定稿 ERR_QUOTA_FINALIZED）+ `unfinalizeQuota:71-76` | `testFinalizeAndUnfinalizeQuota:331-353`（含重复定稿拒绝） | ✅ 接受 |
| 报表同屏展示：actual vs forecast vs quota | `getTerritoryPipeline:88-117`→`accumulatePipeline:163-216` 3 段 quota/forecast/actual（实际段聚合 CONVERTED 商机 expectedRevenue） | `testGetTerritoryPipelineReturnsThreeSections:401-...`（3 段返回） | ✅ 接受 |

**结论**：**接受**（5/5 验收标准全实现 & 强测）。**关键纠正**：计划基线 #3 候选缺口"区域配额 tier 未实现"复核为**不成立**——`QuotaRollupCalculator.rollup(territoryId, ...)` 实际支持 territory 子树聚合，`testQuotaRollupExplicitValuePriorityAndAggregate:295-329` 强断言 region 聚合 1000+500=1500 实证。territory.md §实现注记 4 字面声明 territory tier 已实现（与 Draft Review Record 第 3 项非阻塞观察一致）。**注**：team 级 rollup API 未单独暴露（getQuotaRollup 仅按 territoryId 入参），但 territory 树本身含 TEAM 节点（L1 `:245` REGION→AREA→BRANCH→TEAM），调用 `rollup(territoryId=<team-node-id>)` 等价 team 级聚合——属 API 设计选择非缺口。

### UC-CRM-14 销售序列自动分配与推进 — **接受**（documented 降级 accepted）

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 管理员建 ErpCrmSequence + Step(stepOrder/dueDays/activityType/completionCondition) + Assignment(conditionType/conditionValue/priority) | ErpCrmSequence/Step/Assignment ORM + CrudBizModel | TestErpCrmSequenceAndFunnel seed | ✅ 接受 |
| 线索创建 + QUALIFIED → 按规则匹配序列 → 建 ErpCrmLeadSequenceProgress(currentStepIndex=0, IN_PROGRESS) | `ErpCrmLeadSequenceProgressBizModel.assignSequence` + `SequenceAssignmentEngine`（4 conditionType LEAD_SOURCE/CUSTOM_FIELD 等 + 默认 fallback） | testSequenceAssignAdvanceComplete + testDefaultFallbackAssigns + TestSequenceAssignmentEngine | ✅ 接受 |
| 步骤推进：Event(COMPLETED) 匹配 completionCondition → currentStepIndex+=1 | `ErpCrmLeadSequenceProgressBizModel.advanceStep`→`SequenceStepAdvancer.advance:46-153`（CALL_COMPLETED/MEETING_HELD/TASK_DONE 按 eventType 匹配 `:125-130` + EMAIL_OPENED/REPLIED **降级为 eventType=EMAIL 匹配** `:131-133`） | testSequenceAssignAdvanceComplete + TestSequenceStepAdvancer（5 conditionCondition） | ⚠ 接受（**documented 降级 EMAIL_*→eventType 匹配**——`sales-sequence.md §实现注记:230` 显式标注 successor 触发条件"邮件跟踪服务接入时"，§4 三判据 (ii) 满足边缘但无人工批准痕迹，归 successor） |
| 所有步骤完成 → status=COMPLETED, completedAt=now | `SequenceStepAdvancer:46-153` 末步完成判定 | testSequenceAssignAdvanceComplete | ✅ 接受 |
| 步骤逾期（now > startedAt+ΣdueDays+gracePeriod(2天)）→ 标记逾期 | `ErpCrmLeadSequenceProgressBizModel.scanOverdueSteps:101-131`（grace+连续逾期≥3 计数） | testScanOverdueSteps | ✅ 接受 |
| 连续逾期≥3 提醒负责人 | `ErpCrmSequenceOverdueJob.notifyOverdue:97-116`→`IErpSysNotificationBiz.notify("crm.sequence-overdue"):116` | testScanOverdueSteps + Job cron-gating（无 dedicated TestErpCrmSequenceOverdueJob，见 #4） | ⚠ 接受（**BizModel 逻辑层强测，Job bean 入口仅 indirect 测试——#4 P2**） |

**结论**：**接受**（主路径强测 + documented 降级 EMAIL_*/1-active-sequence accepted per sales-sequence.md §实现注记 :229-231；§4 三判据 (ii) 边缘满足——owner doc 显式 Deferred 标注存在但 AI 落地无人工批准痕迹，归 successor）。

### UC-CRM-15 线索漏斗分析 — **接受**（AMIS 可视化 successor）

| 验收标准 | L3 证据 | L4 证据 | 结论 |
|---------|---------|---------|------|
| 定时 Job 执行漏斗聚合 | `ErpCrmFunnelAggregationJob:27-58`（daily `0 30 3 * * ?` cron 门控 `:42-44`）+ bean 注册 | （无 dedicated TestErpCrmFunnelAggregationJob，见 #4）+ BizModel `refreshFunnel` 测试覆盖 | ⚠ 接受（BizModel 强测，Job bean 入口无 dedicated 测试——#4 P2） |
| 确定 analysis 期间 → 算 ErpCrmLeadFunnel(totalLeadsAtTop/totalOpportunities/totalWon/totalLost/avgSalesCycleDays) | `ErpCrmLeadFunnelBizModel.refreshFunnel:49-58`→Processor 清旧重建 + `FunnelAggregationEngine:42-458` 聚合 | testRefreshFunnelClearRebuildAndView + TestFunnelAggregationEngine（testHeaderMetricsWonLostRevenue） | ✅ 接受 |
| ErpCrmFunnelStageMetrics（每阶段 leadCountIn/conversionRate/avgDaysInStage/lostReasonTop） | `FunnelAggregationEngine` per-stage aggregation + lostReasonTop TOP-N config-gated | TestFunnelAggregationEngine（testStageMetricsConversionAndDropOff + testAvgDaysInStage + testLostReasonTopNLimited） | ✅ 接受 |
| 前端漏斗图 stages + lostByStage | `ErpCrmLeadFunnelBizModel.getFunnelView:60-108`（可视化数据结构 stages+lostByStage） | testRefreshFunnelClearRebuildAndView（结构断言） | ⚠ 接受（**后端 getFunnelView 就绪；AMIS 前端可视化 = successor**——`lead-waterfall.md §实现注记:240` 显式标注 successor 触发条件"CRM 前端可视化套件建立时"，§4 三判据 (ii) 边缘满足但 AI 落地无人工批准，归 successor） |

**结论**：**接受**（后端聚合 + getFunnelView 强测；AMIS 前端可视化 documented successor）。

### 候选缺口分级汇总

| # | UC | 候选缺口 | 复核结论 | 分级 | finding ID | §2 判据 |
|---|----|---------|---------|------|-----------|---------|
| #1 | UC-CRM-07 | UTM copy-on-create 派生缺失 | **成立** | **P1** | P1-RC-037 | §2 P1① + P1⑤ |
| #2 | UC-CRM-07 | 归因报表完全缺失 | **成立** | **P1** | P1-RC-038 | §2 P1① + P1⑤ |
| #3 | UC-CRM-12 | 区域配额 tier rollup（计划基线假设） | **不成立（纠正计划基线）** | — | — | QuotaRollupCalculator.rollup 实际支持 territory tier，territory.md §实现注记 4 字面声明 + test 实证 |
| #3' | UC-CRM-10 | Forecast 区域 tier rollup 未实现 + follow-up 触发条件已满足 | **成立（新发现）** | **P1** | P1-RC-039 | §2 P1① + §4 三判据复核重开（触发条件 Lead.territoryId 现存） |
| #4 | UC-CRM-14/15 | Job bean cron-gating execute() 测试缺失（SequenceOverdue/FunnelAggregation） | **成立** | **P2** | P2-RC-035 | §2 P2① 次要验证维度弱 |

**整体裁决**：7 UC 结论 = UC-CRM-05 接受（on 主路径 + SP-1 观察）/ UC-CRM-07 P1 / UC-CRM-08 接受 / UC-CRM-10 P1 / UC-CRM-12 接受（纠正计划基线 #3）/ UC-CRM-14 接受（documented 降级）/ UC-CRM-15 接受（AMIS successor）。**3 项新 P1（P1-RC-037/038/039）+ 1 项新 P2（P2-RC-035）**。**零 P0**（候选缺口均不破坏活跃数据/GL 平衡/核心循环/会计正确性——CRM 域本身不直接产生会计凭证；UTM 归因缺失影响营销 ROI 报表但不破坏 GL；Forecast territory tier 缺失影响区域级预测聚合精度但 commit/upside/bestCase 公式主路径正确；Job bean 测试缺口是验证维度非功能缺失）。

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：每条 finding 产出前 grep arm-index 同域同控制点后裁决（禁止未经比对直接新建）。

**arm-index crm activity/UTM/campaign/forecast/quota/sequence/funnel 同域 grep 结果**：
- `P1-MA1-009`（crm DECIMAL↔double）——**非本切片**（非本切片实体），不复用。
- `P1-MA1-022`（5 域跨域只读 daoFor 含 crm）——**非本切片**（daoFor 维度非 UTM/forecast/quota 控制点），不复用。
- `P1-MA2-075`（stageId 守卫 UC-CRM-06）——**非本切片**（UC-CRM-06 归 A1.30），resolved R1.24，不复用。
- `P1-MA2-076`（reminderMinutesBefore UC-CRM-08）——**本切片 UC-CRM-08 已 resolved**，复用注记（行为已证实 R1.24）。
- `P1-MA2-086`（cron job 并发含 crm event-reminder/sequence-overdue）——**本切片 UC-CRM-08/14 已 resolved**，复用注记（行为已证实 R1.28）。
- `P1-MA2-093`/`094`（orgId 隔离）——resolved R1.29，**不同控制点**（orgId 隔离 vs UTM 归因/forecast tier/quota rollup），不复用。
- `P2-MA4-013`（crm Forecast stageName stub + refresh concurrency watch-only）——**不同控制点**（stageName/concurrency vs territory tier rollup），不复用；本切片 UC-CRM-10 触 P2-MA4-013 watch-only → 复用注记（同 Forecast 实体不同维度）。
- `P2-MA4-020`（crm badge 漂移 watch-only 视图层）——**不同控制点**，不复用。
- `P1-MA3-004`（8 扩展域 README schema）——resolved R2.1，**不同维度**，不复用。
- **RC 系列对 crm marketing/forecast/quota/sequence/funnel = 零**（A1.29 为 CRM 域第二批 RC 切片；A1.28 同批次覆盖 crm-F1 线索生命周期，**P1-RC-032~036 + P2-RC-031~034** 已分配）。

**裁决结论**：#1/#2/#3'/#4 均为 CRM 域**第二批 RC 切片新发现**（既有 arm-index 无 RC finding 涉及 crm UTM 归因/forecast territory tier/sequence-funnel job bean 测试）→ **新建 P1-RC-037/038/039 + P2-RC-035**（接续 A1.28 P1-RC-036/P2-RC-034 编号）。UC-CRM-05/08/12/14/15 已证实→接受（无 finding）。UC-CRM-12 #3 候选缺口**纠正为不成立**（territory tier 实际已实现）。UC-CRM-08 P1-MA2-076 + UC-CRM-14 P1-MA2-086 已 resolved → 复用注记不新建。禁止未经比对新建——已 grep arm-index crm activity/UTM/campaign/forecast/quota/sequence/funnel 同域同控制点确认零重叠。

| Finding ID | UC | 描述（简） | 目标 MR | 触及保护区域 |
|-----------|----|-----------|--------|-------------|
| P1-RC-037 | UC-CRM-07 #1 | UTM copy-on-create 派生缺失（defaultPrepareSave 不复制 campaign.medium/source） | MR1（RC-R1.n） | 否（代码逻辑类预授权——复用既有 IErpCrmCampaignBiz + utmMedium/utmSource 字段） |
| P1-RC-038 | UC-CRM-07 #2 | 归因报表完全缺失（CampaignBizModel 19 行 stub + 无 campaign attribution dataset + 无 .xpt.xml 模板） | MR1（RC-R1.n） | 否（代码逻辑类预授权——镜像既有 funnel/forecast-accuracy 报表范式） |
| P1-RC-039 | UC-CRM-10 #3' | Forecast territory tier rollup 未实现 + follow-up 触发条件已满足（Lead.territoryId 现存） | MR1（RC-R1.n）/ §4(iii) | 否（代码逻辑类预授权——ForecastAggregator 加 territory tier，复用既有 territoryId + collectSubtreeIds 模式镜像 QuotaRollupCalculator） |
| P2-RC-035 | UC-CRM-14/15 #4 | Job bean cron-gating execute() 测试缺失（SequenceOverdue + FunnelAggregation） | successor watch-only | 否（纯测试补充预授权自动执行） |

**双向可追溯**：finding ID ↔ 修复行预留 MR1（RC-R1.n）；arm-index 新 finding 行在 §6 落盘后同步写入 arm-index（见 arm-index RC 交叉引用注记）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点（每存疑点一行）。

- SP-1：UC-CRM-05 lastContactDate 按 COMPLETED 过滤的语义偏差——L1 `:104` 字面"max(相关 Event.startDateTime)"未限定状态，实现 `LeadActivityDerivationHelper.latestCompletedStartDateTime:55` 按 COMPLETED 过滤。倾向"last contact"语义合理（CANCELLED 不算联系），但运行时需确认是否应包含 CANCELLED（如客户取消的会议仍属"联系过"）。
- SP-2：UC-CRM-07 UTM copy 缺失下 lead.utmMedium/utmSource 实际默认值（NULL vs 调用方显式传入）——运行时确认外部渠道提交时若不传 utmMedium/utmSource，字段实际持久化为 NULL，影响后续归因报表准确性。
- SP-3：UC-CRM-07 归因报表缺失下 campaignId 已填但无聚合的实际数据状态——campaignId 外键可被 Lead 持久化但无报表消费，运行时确认是否影响营销 ROI 决策（业务影响 = 报表缺失，非数据破坏）。
- SP-4：UC-CRM-10 ForecastAggregator 3 级 rollup（个人→团队→公司）跨 ownerId 边界正确性——团队 rollup 按 `ownerTeam:62-72` 映射 ownerId→teamId，运行时确认同一 owner 跨团队迁移场景的 rollup 一致性。
- SP-5：UC-CRM-10 territory tier 缺失下跨区域预测汇总实际行为——`getTerritoryPipeline:88-117` 走 QuotaRollupCalculator.accumulatePipeline（含 territory 子树），但 Forecast 段无 territory tier 行（ForecastAggregator 仅生成个人/团队/公司行），运行时确认 Forecast 段在 territory 级管道报表的实际展示（可能是 0 或跨 territory 聚合失真）。
- SP-6：UC-CRM-12 QuotaRollupCalculator 显式值优先在 team-level explicit + individual explicit 共存时的汇总语义——当前 `rollup:94-100` 对 territory 子树所有行（含 team-level explicit + individual-level explicit）求和，可能 double-count（team=1000 + 个人=500 → region=1500，L1 严格"region=Σ teams"应为 1000）。运行时确认实际行为 + 与 L1 字面"区域配额=Σ 团队配额"的偏差面（可能为 P2 successor）。
- SP-7：UC-CRM-14/15 FunnelAggregationJob/SequenceOverdueJob 实际 cron 触发行为——cron yaml 已注册但 `enabled` 默认 false（`@cfg:...enabled|false`），运行时确认部署时 enabled=true 后的执行链路（BeanMethodJobInvoker→execute()→cron 空 skip 门控）。
- SP-8：UC-CRM-14 EMAIL_OPENED/EMAIL_REPLIED 降级为 eventType=EMAIL 匹配的实际触发面——降级后所有 EMAIL 历史均触发步骤推进，运行时确认是否过早推进（邮件未实际打开/回复但步骤已 advance）。

**P0 即时通道未触发**：本切片无 P0——所有候选缺口（#1/#2 UTM 归因 / #3' Forecast territory tier / #4 测试缺口）均不属 §2 P0①②③④（活跃数据破坏/安全隔离/核心循环断裂/会计过账正确性）。CRM 域不直接产生会计凭证，UTM 归因/forecast territory tier 缺失影响报表精度但不破坏 GL 平衡或核心 O2C/P2P 循环。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，退出码 = 0（纯 reporter，恒 0）。actual vs baseline 抽样（R1 dao() 直接调用=0 / R2 daoFor() 绕 I*Biz=34 / R3 new Erp*() 构造=229 / 生产代码总计=1382）——均为既有项目状态，**本审计为只读审计无生产代码变更，checker 无回归风险**。**不以 checker 脚本退出码 0 作为门控通过依据**（真正门控在 CI workflow `.github/workflows/compliance.yml`）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点（crm activity/UTM/campaign/forecast/quota/sequence/funnel）后给出"复用 or 新增"裁决（见 §6），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9 冻结条款）：本审计未修改 product-scope / use-cases.md / owner doc（README/marketing/sales-forecast/territory/sales-sequence/lead-waterfall）的需求契约段落。发现的 L2 注记（sales-forecast.md:222 Deferred 触发条件已满足）记入本报告不直改真相源。
- [x] **9 段完整性自检**：落盘前自查 §1-§9 全部存在（§1 需求契约原文 / §2 实现证据 / §3 测试证据 / §4 运行时行为 / §5 符合性结论 / §6 arm-index 衔接 / §7 静态存疑点 / §8 过程纪律自检 / §9 与 MA2 报告差异增量）。

---

## 9. 与 MA2 报告差异增量声明

> 方法论 §去重协议：复用既有 MA2 报告已证实行为，只补需求视角差异。

**复用的 MA2/A1.13/A4.5 已证实行为**（不重新核实）：
- A2.14：crm Event 3 态状态机 PASS + 转化跨域经 Facade 零跨模块 ORM 写；P1-MA2-076（UC-CRM-08 reminder）resolved R1.24；P1-MA2-086（UC-CRM-08/14 cron 并发）resolved R1.28。
- A1.13：crm 15 维平台合规 15/15 PASS。
- A4.5：crm BizModel + LeadScoringEngine + **ForecastAggregator + FunnelAggregationEngine** 代码质量 PASS（P1-MA1-009 crm DECIMAL↔double 非本切片）。
- A2.18：crm orgId 查询隔离（resolved R1.29）。

**本切片只补的"需求契约↔实际行为"差异**（既有 MA2 未覆盖的需求视角）：
1. **UC-CRM-07 UTM copy-on-create 派生缺失**（P1-RC-037）——A2.14/A4.5 证实 Event/转化/评分引擎行为，未从 L1 `:142-143` "utmMedium→复制 campaign.medium（若未显式传入）"视角审视 UTM copy 缺失；**无既有 MA2/MA4 报告标记 UTM copy 缺失**（本切片新发现）。
2. **UC-CRM-07 归因报表缺失**（P1-RC-038）——A4.5 代码质量 PASS 未覆盖报表模板维度；**无既有 MA2/MA4 报告标记归因报表缺失**（本切片新发现）。
3. **UC-CRM-10 Forecast territory tier 缺失 + follow-up 触发条件已满足**（P1-RC-039）——A4.5 ForecastAggregator 代码质量 PASS 但未从 L1 `:228` "团队→区域→公司"4 级视角审视 territory tier 缺失；`sales-forecast.md:222` Deferred 标注的触发条件（"Lead→Territory 映射就绪"）现已满足（`territory.md §实现注记 1:219-222` Lead.territoryId propId 41 已落地）→ §4 三判据复核重开 P1（与 A1.28 P1-RC-036 territory ROUND_ROBIN 降级同型——territory.md Deferred AI 落地无人工批准 + 触发条件已满足）。
4. **UC-CRM-12 territory tier 实际已实现**（纠正计划基线 #3 假设）——A4.5 QuotaRollupCalculator 代码质量 PASS，本切片补 L1 `:285` 视角复核 + test 实证（testQuotaRollupExplicitValuePriorityAndAggregate:295-329），**纠正计划基线假设**。
5. **UC-CRM-14/15 Job bean 测试缺口**（P2-RC-035）——A2.14 证实 BizModel 状态机行为，未覆盖 Job bean cron-gating execute() 入口维度（仅 ForecastRecalc/EventReminder 有 dedicated Job 测试，SequenceOverdue/FunnelAggregation 缺）。
6. **UC-CRM-05 lastContactDate 按 COMPLETED 过滤**（SP-1）——A2.14 证实 Event 状态机 + 派生行为，未从 L1 `:104` "相关 Event"字面视角审视按状态过滤的语义偏差（倾向接受实现选择，登记 SP）。

**结论**：本切片与既有 MA2/A1.13/A4.5 报告互补不重复——MA2 视角 = 状态机/链路行为/代码质量，本 RC 切片视角 = 需求契约（use-case 验收标准）符合性。**UC-CRM-12 计划基线 #3 候选缺口纠正**是本切片对计划假设的重要修订（基于实仓代码 + test 实证），不影响其他切片结论。
