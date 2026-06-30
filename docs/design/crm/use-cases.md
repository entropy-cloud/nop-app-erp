# CRM 域用例规格（CRM Use Cases）

> 从使用场景出发组织 CRM 域可验证用例。机制细节引用不重复（指向 state-machine / README）。
> CRM 核心是线索获取 → 跟进 → 转化为商机 → 漏斗阶段管理 → 转化为报价单的全流程，附带活动时间线、UTM 归因、事件提醒。

## 状态轴速查（详见 state-machine.md）

```
Lead/docStatus(5态): NEW / QUALIFIED / CONVERTED / LOST / CANCELLED
Event/status(3态):   PLANNED / COMPLETED / CANCELLED
```

---

## UC-CRM-01 线索创建与验证

**场景**：销售员创建新线索，跟进后验证为有效商机。

**可验证断言**（见 README.md §ErpCrmLead、state-machine.md §Lead）：
```
Lead 创建：leadType=LEAD, docStatus=NEW, contactName/companyName 必填
Lead 跟进验证 →
  docStatus: NEW → QUALIFIED
  stageId 可设（取 ErpCrmStage.sequence 的第一个阶段）
  probability 取 stage.defaultProbability（用户可覆盖）
  lastContactDate 自动更新为当前时间
```

**涉及机制**：state-machine.md §Lead、README.md §ErpCrmLead

---

## UC-CRM-02 线索 → 商机转化

**场景**：已验证线索转化为商机（升格为 OPPORTUNITY 类型）。

**可验证断言**（见 README.md §业务规则 1、§跨域协作）：
```
Lead.docStatus == QUALIFIED 且 leadType == LEAD →
  执行转化操作：
    if convertToCustomer:
      创建 ErpMdPartner（派生自 contactName/companyName/contactPhone/contactEmail）
      创建 ErpCrmLead(leadType=OPPORTUNITY, partnerId=新建客户)
      原 lead.docStatus → CONVERTED
    if 不创建客户:
      lead.leadType → OPPORTUNITY（直接升格）
  LEAD 类型不可直接转报价单（系统拦截）
```

**涉及机制**：README.md §业务规则 1、state-machine.md §Lead

---

## UC-CRM-03 商机 → 报价单转化

**场景**：商机到达赢单阶段后转化为销售报价单。

**可验证断言**（见 README.md §跨域协作 §衔接契约）：
```
Lead.leadType == OPPORTUNITY 且 lead.docStatus == QUALIFIED
 且 stage.isWonStage == true →
  convertToQuotation(leadId, quotationData) →
   调用 IErpSalQuotationBiz 创建 ErpSalQuotation（跨域 I*Biz）
   回写 lead.relatedBillType = 'SALES_QUOTATION'
   回写 lead.relatedBillCode = 报价单号
   lead.docStatus → CONVERTED
 转化后：ErpSalQuotation 无 CRM 外键（核心零污染）
```

**涉及机制**：README.md §跨域协作 §衔接契约

---

## UC-CRM-04 丢单原因记录

**场景**：商机跟进失败，标记丢失并录入原因。

**可验证断言**（见 state-machine.md §Lead、README.md §业务规则 5）：
```
Lead.docStatus 为 NEW 或 QUALIFIED →
  标记丢失 →
    lostReasonId 必填（不允许空）
    lostReasonDesc 可选（补充说明）
    lead.docStatus → LOST（终态）
  若 lostReasonId 为空 → 拒绝迁移，返回校验错误
```

**涉及机制**：state-machine.md §Lead §4 异常路径、README.md §业务规则 5

---

## UC-CRM-05 活动/事件记录

**场景**：在线索/商机上记录通话、邮件或会议活动。

**可验证断言**（见 README.md §ErpCrmEvent、state-machine.md §Event）：
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

**涉及机制**：README.md §ErpCrmEvent、§业务规则 2、state-machine.md §Event

---

## UC-CRM-06 漏斗阶段推进

**场景**：商机按漏斗阶段逐步前移（如"需求分析 → 方案演示 → 谈判中 → 赢单"）。

**可验证断言**（见 README.md §ErpCrmStage、state-machine.md §Lead §2）：
```
Lead.docStatus == QUALIFIED 且 leadType == OPPORTUNITY →
  阶段前移：stageId 只能沿 ErpCrmStage.sequence 递增
    if newStage.sequence > currentStage.sequence → 允许前移
    if newStage.sequence <= currentStage.sequence → 拒绝（不可跳级回退）
   记录 ErpCrmLeadConvLog(fromStageId, toStageId, changedAt, changedBy)
  isWonStage == true → 允许触发 UC-CRM-03（转化）
  stageId 变更时不修改 docStatus（docStatus 仍为 QUALIFIED）
```

**涉及机制**：README.md §ErpCrmStage、state-machine.md §Lead §2、§4

---

## UC-CRM-07 UTM 营销活动归因

**场景**：通过 UTM 参数创建线索，归因到对应营销活动。

**可验证断言**（见 README.md §ErpCrmCampaign §ErpCrmLead）：
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

**涉及机制**：README.md §ErpCrmCampaign §ErpCrmLead

---

## UC-CRM-08 事件提醒 Job

**场景**：定时任务检查即将开始的计划事件并发送提醒。

**可验证断言**（见 README.md §业务规则 4、state-machine.md §Event §7）：
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

**涉及机制**：README.md §业务规则 4、state-machine.md §Event §7

---

## UC-CRM-09 线索自动评分

**场景**：系统根据可配置的评分准则对线索自动评分，高分线索自动转商机。

**可验证断言**（见 `lead-scoring.md` §业务规则 §评分计算流程）：
```
管理员创建 ErpCrmLeadScoreConfig(isActive=true) 并配置 configLines
（如 SOURCE_WEIGHT=LOOKUP, ENGAGEMENT_SCORE=FORMULA）

评分触发（MANUAL / LEAD_UPDATE / SCHEDULED）→
  加载当前生效的评分规则 →
  按 configLines 逐条评分（LOOKUP/FORMULA/BOOLEAN）→
  计算 totalScore（归一化 0-100）→
  创建 ErpCrmLeadScore + ErpCrmLeadScoreLine 记录 →
  回写 lead.score = totalScore

if totalScore >= autoQualifyThreshold
  且 lead.leadType == LEAD 且 lead.docStatus == NEW →
    自动执行：
      lead.docStatus → QUALIFIED
      lead.score = totalScore
      ErpCrmLeadScore.triggeredAction = AUTO_QUALIFY

if autoQualifyThreshold > totalScore >= minScoreForFollowUp →
  NOTIFY_OWNER（通知销售优先跟进不改变 docStatus）

评分历史只追加：每次评分创建新记录，lead 当前分数取最新一条
```

**涉及机制**：`lead-scoring.md` §业务规则 §评分计算流程

---

## UC-CRM-10 销售预测生成

**场景**：系统基于期间内商机的阶段概率生成加权管道预测，按销售员/团队/区域分层汇总。

**可验证断言**（见 `sales-forecast.md` §业务规则 §预测重新计算流程）：
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

**涉及机制**：`sales-forecast.md` §业务规则 §预测重新计算流程

---

## UC-CRM-11 线索区域自动分配

**场景**：创建线索时系统根据分配规则自动匹配销售区域和负责人。

**可验证断言**（见 `territory.md` §业务规则 §分配执行流程）：
```
管理员配置 ErpCrmTerritory 树形结构（REGION → AREA → BRANCH → TEAM）
  并创建 ErpCrmTerritoryAssignmentRule(
    priority=1, conditionType=GEOGRAPHY,
    conditionValue={"province":["上海","浙江"]},
    assignmentMethod=ROUND_ROBIN)

线索创建（未指派 owner/team）→
  按优先级遍历 isActive=true 的规则 →
    匹配线索字段（province/industry/companySize/sourceId）→
    找到首个匹配规则 →
    按 assignmentMethod 分配：
      ROUND_ROBIN → 轮流分给团队内成员
      LOAD_BALANCED → 分给线索最少的成员
      MANUAL → 标记待分配
    回写 lead.territoryId / lead.teamId / lead.ownerId

无规则匹配 → 使用 isDefault=true 的规则
仍无匹配 → territoryId 留空，标记"未分配"
```

**涉及机制**：`territory.md` §业务规则 §分配执行流程

---

## UC-CRM-12 销售配额管理

**场景**：按区域/团队/个人设置销售目标配额，支持多级汇总和定稿锁定。

**可验证断言**（见 `territory.md` §ErpCrmQuota §配额层级汇总）：
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
  区域配额（territoryId 非空, teamId/ownerId 为空）= Σ 团队配额
     ↑ Σ
  公司配额（territoryId/teamId/ownerId 均为空）= Σ 区域配额

管理员可为各层级写入显式配额值（覆盖聚合值）

isFinalized=true → 配额不可修改（需先解冻）

报表同屏展示：actual（实际）vs forecast（预测）vs quota（目标）
```

**涉及机制**：`territory.md` §ErpCrmQuota §配额层级汇总、`sales-forecast.md`

---

## UC-CRM-13 CPQ 配置-定价-报价

**场景**：销售员通过产品配置器选择特征组合，系统应用价格规则生成报价。

**可验证断言**（见 `cpq.md` §业务规则 §配置规则引擎）：
```
管理员创建 ErpCrmProductConfigurator(isActive=true, productType="SERVER")
  并配置 configLines + wizardLayout

用户在配置向导中按步骤选择特征 →
  每步选择触发 ErpCrmConfigRule 规则引擎：
    if REQUIRED → 目标特征标记必选
    if EXCLUDED → 目标特征禁用
    if RECOMMENDED → 目标特征高亮推荐
  UI 即时更新可选列表

配置完成后 →
  应用 ErpCrmPriceRule 计算价格（按 VOLUME/PROMOTIONAL/CUSTOMER_SPECIFIC 优先级）
  可选：应用 ErpCrmBundlePricing（若匹配捆绑包）
  生成配置快照(JSON)

生成报价 →
  调用 IErpSalQuotationBiz.createFromConfig(
    leadId, configSnapshot, bundlePricingId?, priceRuleIds?)
  → 创建 ErpSalQuotation
  回写 lead.relatedBillType/Code
```

**涉及机制**：`cpq.md` §业务规则 §价格规则引擎 §配置规则引擎，`README.md` §跨域协作

---

## UC-CRM-14 销售序列自动分配与推进

**场景**：线索进入 QUALIFIED 后自动分配跟进序列，步骤活动自动排程，完成条件驱动步骤推进。

**可验证断言**（见 `sales-sequence.md` §业务规则 §序列自动分配 §步骤推进）：
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

**涉及机制**：`sales-sequence.md` §业务规则，`state-machine.md` §Event，`README.md` §ErpCrmEvent

---

## UC-CRM-15 线索漏斗分析

**场景**：系统定时聚合阶段流转数据，生成漏斗各层容量、转化率、停留天数、丢失原因归因分析。

**可验证断言**（见 `lead-waterfall.md` §聚合计算流程 §业务规则）：
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

**涉及机制**：`lead-waterfall.md` §聚合计算流程 §业务规则，`README.md` §ErpCrmLeadConvLog §ErpCrmStage

---

## 用例与测试的衔接

- 线索创建验证（UC-01）→ docStatus 迁移 + 联系人必填校验
- 线索升格（UC-02）→ leadType 变更 + ErpMdPartner 创建
- 商机转报价（UC-03）→ 跨域 I*Biz 调用 + 弱指针回写
- 丢单必填（UC-04）→ lostReasonId 非空约束
- 活动时间线（UC-05）→ Event CRUD + lastContactDate 自动派生
- 阶段推进（UC-06）→ sequence 单向约束 + ConvLog 审计
- UTM 归因（UC-07）→ campaignId 关联 + 归因报表
- 事件提醒（UC-08）→ Job 定时查询 + 通知发送
- 线索评分（UC-09）→ config 驱动评分引擎 + auto-qualify 阈值触发 docStatus 迁移
- 销售预测（UC-10）→ 加权管道聚合 + commit/upside 分类 + 层级汇总 + 准确率
- 区域分配（UC-11）→ territory 树 + assignment rule 按条件分配 + ROUND_ROBIN/LOAD_BALANCED
- 配额管理（UC-12）→ quota 层级汇总 + isFinalized 定稿锁定 + 预测同屏对比
- CPQ 配置-定价-报价（UC-13）→ configurator 特征规则 + price rule 优先级 + bundle 折扣 + 配置→报价单生成
- 销售序列（UC-14）→ sequence 模板 + step 完成条件 + 自动分配 + 步骤推进 + 逾期检查
- 线索漏斗（UC-15）→ ConvLog 聚合 + stage 转化率 + time-in-stage + 丢失原因阶段归因 + 可视化数据

## 参考机制文档

- `state-machine.md` — Lead/Event 状态/异常
- `lead-scoring.md` — 评分规则配置/计算引擎/阈值驱动
- `sales-forecast.md` — 预测期间/加权管道/层级聚合/准确率
- `territory.md` — 区域树/分配规则/配额管理
- `cpq.md` — 产品配置器/配置规则/捆绑定价/价格规则/报价生成
- `sales-sequence.md` — 序列模板/步骤完成条件/分配规则/进度跟踪
- `lead-waterfall.md` — 漏斗聚合/阶段度量/转化率/丢失原因归因
- `../sales/README.md` — 报价单域边界
- `../master-data/README.md` — 合作伙伴主数据
