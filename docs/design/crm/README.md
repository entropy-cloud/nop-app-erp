# CRM 域（crm）— 完整设计

## 目的

设计客户关系管理（CRM）模块：线索获取 → 线索跟进 → 转化为商机 → 漏斗阶段管理 → 转化为销售报价单的全流程。包含活动历史（Event/Meeting）、日历管理、营销活动归因（UTM）、团队协作。

## 边界

- 本模块负责：线索（Lead）管理、商机（Opportunity）管理、漏斗阶段配置、营销活动归因（UTM）、活动/事件记录、日历/会议、线索转商机/转报价单。
- **与 sales 的边界**：CRM 管线索到商机，商机转化结果通过弱指针交接给 sales 域的报价单 `ErpSalQuotation`。sales 域从报价单起。不在 sales 实体加任何 CRM 外键（核心零污染）。
- 本模块不负责：报价单/订单/出库/开票（sales 域）；客户/合作伙伴主数据（master-data 域 `ErpMdPartner`）；售后客服工单（`customer-service` 域）。

## 设计依据

> 参考 **Axelor CRM**（70 Java 文件）：Lead→Convert→Opportunity 完整转化流 + Event/Meeting 活动时间线 + 日历服务 + 查重服务 + 事件提醒 Job。
>
> 参考 **IDURAR ERP CRM**（8504⭐）：Lead→Quote→Invoice 端到端流验证。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §CRM 章节 + `docs/analysis/erp-survey/2026-06-30-0000-idurar-erp-crm.md`。

## 实体清单

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。

### ErpCrmLead（线索/商机）

单实体 + type 判别：`leadType ∈ {LEAD, OPPORTUNITY}`，比 Lead/Opportunity 两表更精简。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | — |
| leadType | dict `erp-crm/lead-type`：LEAD（线索）/ OPPORTUNITY（商机） | 🟢 Odoo crm_lead.py:84-125 |
| partnerId | 客户（→ErpMdPartner，线索阶段可空，转化时填充） | 🟢 Axelor Lead.partner |
| contactName/contactPhone/contactEmail | 联系人信息（线索阶段） | 🟢 Axelor Lead.name/firstName/mobilePhone |
| companyName | 公司名 | 🟢 Axelor Lead.enterpriseName |
| jobTitle/department | 职位/部门 | 🟢 Axelor Lead.jobTitleFunction/department |
| sourceId | 线索来源（→ErpCrmSource） | 🟢 Axelor Lead.source |
| leadStatusId | 线索阶段/状态（→ErpCrmLeadStatus，自有状态字典） | 🟢 Axelor Lead.leadStatus |
| stageId | 漏斗阶段（→ErpCrmStage） | 🟢 Odoo crm_stage |
| expectedRevenue | 预期收入（一次性） | 🟢 Axelor Opportunity.amount |
| bestCaseAmount/worstCaseAmount | 预期范围（乐观/悲观） | 🟢 Axelor Opportunity.bestCase/worstCase |
| recurringRevenue/recurringPlan | 周期性收入/MRR | 🟢 Odoo crm_lead.py:141-150 |
| expectedCloseDate | 预期签单日 | 🟢 Axelor Opportunity.expectedCloseDate |
| probability | 成交概率（默认取阶段概率，可覆盖） | 🟢 Axelor Opportunity.probability |
| campaignId/medium/source | UTM 归因（→ErpCrmCampaign） | 🟢 Odoo crm_lead.py:24-28 |
| ownerId | 负责销售员（→User） | 🟢 Axelor Lead.user |
| teamId | 销售团队（→ErpCrmTeam） | 🟢 Axelor Lead.team |
| lostReasonId | 丢单原因（→ErpCrmLostReason） | 🟢 Axelor Lead.lostReason/Opportunity.lostReason |
| lostReasonDesc | 丢单描述 | 🟢 Axelor Opportunity.lostReasonStr |
| lastContactDate | 最后联系日期 | 🟢 Axelor Opportunity.lastEventDateT（formula） |
| nextActivityDate | 下次活动日期 | 🟢 Axelor Opportunity.nextScheduledEventDateT |
| relatedBillType/relatedBillCode | 转化结果弱指针（SALES_QUOTATION + 报价单号） | 核心零污染 |
| docStatus | dict `erp-crm/lead-doc-status`：NEW / QUALIFIED / CONVERTED / LOST / CANCELLED | — |
| 标准审计字段 | version/delVersion/createdBy/createTime/updatedBy/updateTime/remark | — |

**状态机**：

```
NEW（新线索/新商机创建）
  ├─ 跟进 → QUALIFIED（已验证，进入漏斗阶段管理）
  │            ├─ 转化 → CONVERTED（已转报价单，终态）
  │            └─ 标记丢失 → LOST（录入丢单原因，终态）
  ├─ 标记丢失 → LOST（线索阶段直接丢单）
  └─ 取消 → CANCELLED（无效/重复，终态）
```

阶段流转（stageId）是独立维度，`docStatus=QUALIFIED` 后由 `ErpCrmStage` 的 sequence 驱动前移。

### ErpCrmLeadStatus（线索状态字典）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| sequence | 排序 | 
| isDefault | 是否默认 |

参考 🟢 Axelor Lead.leadStatus（LeadStatus 实体，非硬编码 enum）。

### ErpCrmStage（漏斗阶段表）

| 字段 | 含义 |
|------|------|
| id/code/orgId | 标准 |
| stageName | 阶段名（如"新线索/已联系/需求分析/方案演示/谈判中/赢单"） |
| sequence | 排序（漏斗顺序） |
| teamId | 团队作用域（销售团队） |
| defaultProbability | 默认成交概率% |
| isWonStage | 是否赢单阶段（赢单后触发转化） |

> **非硬编码 enum**：阶段是数据库记录，支持按团队自定义（🟢 Odoo `crm_stage.py:14-33`，🟢 Axelor Opportunity.opportunityStatus）。

### ErpCrmSource（线索来源）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| sequence | 排序 |

参考 🟢 Axelor Lead.source（Source 实体）。

### ErpCrmLostReason（丢单原因）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| sequence | 排序 |

参考 🟢 Axelor Lead.lostReason / Opportunity.lostReason（LostReason 实体）。

### ErpCrmEvent（活动/事件/日历）

对应 🟢 Axelor Event（extends ICalendarEvent）。CRM 活动时间线的核心实体，覆盖通话/邮件/会议/任务的完整记录。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | — |
| eventType | dict `erp-crm/event-type`：CALL（通话）/ EMAIL（邮件）/ MEETING（会议）/ TASK（任务） | 🟢 Axelor Event.callTypeSelect |
| eventCategoryId | 活动类别（→ErpCrmEventCategory） | 🟢 Axelor Event.eventCategory |
| subject | 活动主题 | — |
| description | 活动详细描述（large=true） | — |
| startDateTime/endDateTime | 起止时间 | — |
| duration | 时长（分钟） | 🟢 Axelor Event.duration |
| relatedLeadId | 关联线索/商机（→ErpCrmLead，多态走弱指针：relatedObjectType/relatedObjectId） | 🟢 Axelor Event.eventLead/opportunity |
| relatedBillType/relatedBillCode | 关联业务单（销售订单/报价单等，可选） | — |
| partnerId | 关联客户（→ErpMdPartner） | 🟢 Axelor Event.partner |
| contactId | 联系人（→ErpMdPartner） | 🟢 Axelor Event.contactPartner |
| ownerId | 负责人（→User） | — |
| status | dict `erp-crm/event-status`：PLANNED（已计划）/ COMPLETED（已完成）/ CANCELLED（已取消） | 🟢 Axelor Event.statusSelect |
| priority | dict `erp-crm/event-priority`：LOW/NORMAL/HIGH/URGENT | 🟢 Axelor Event.prioritySelect |
| isRecurrent | 是否重复事件 | 🟢 Axelor Event.isRecurrent |
| parentEventId | 父事件（重复事件实例关联） | 🟢 Axelor Event.parentEvent |
| reminderMinutesBefore | 提醒提前分钟数 | 🟢 Axelor Event.eventReminderList |
| 标准审计字段 | | |

**状态机**：`PLANNED → COMPLETED`（正常完成）或 `PLANNED → CANCELLED`。

> 活动（Event）是 CRM 时间线的核心——与线索/商机的每一次通话、邮件、会议都是一条 Event 记录，按时间倒序展示在活动历史中。Event 独立于 Activity（活动类型更窄，仅用于记录操作类型），Event 覆盖完整的日历排程。

### ErpCrmEventCategory（活动类别字典）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| color | 日历颜色 | 

### ErpCrmActivity（商机活动记录，简化活动日志）

与 Event 的区别：Activity 是**轻量操作日志**（仅记录"谁在何时做了什么"），不涉及时长/日历/提醒。

| 字段 | 含义 |
|------|------|
| id/leadId/orgId | 标准 |
| activityType | dict：NOTE（备注）/ CALL（通话）/ EMAIL（邮件）/ MEETING（会议） |
| activityDate | 活动日期 |
| summary | 内容摘要 |
| ownerId | 负责人 |
| 标准审计字段 | |

参考 🟢 Axelor CrmActivityService。

### ErpCrmCampaign（营销活动，UTM 归因）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| campaignName | 活动名 |
| medium/source | UTM medium/source |
| startDate/endDate | 活动区间 |
| budgetAmount/actualCost | 预算/实际成本 |
| 标准审计字段 | |

### ErpCrmTeam（销售团队）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| teamLeaderId | 团队负责人 |
| memberIds | 团队成员（many-to-many → User） |

### ErpCrmLeadConvLog（阶段流转审计）

| 字段 | 含义 |
|------|------|
| id/leadId/orgId | 标准 |
| fromStageId/toStageId | 前/后阶段 |
| changedAt/changedBy | 变更时间/人 |
| 标准审计字段 | |

### ErpCrmQuoteTemplate（报价模板，可选项）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| templateContent | 模板内容（支持占位符） |
| isDefault | 是否默认 |

## 业务规则

1. **Lead→Convert→Opportunity→Quotation 转化流**（参考 🟢 Axelor ConvertLeadWizardService）：
   - LEA类型→转化→创建 ErpMdPartner（客户）+ 创建 ErpCrmLead(leadType=OPPORTUNITY)
   - OPPORTUNITY类型→转化→调用 IErpSalQuotationBiz 创建 ErpSalQuotation
   - 转化后 lead.docStatus=CONVERTED，relatedBillType/Code 写回

2. **活动时间线自动派生**：lead 的 lastContactDate 和 nextActivityDate 从关联的 ErpCrmEvent 自动计算（参考 🟢 Axelor Opportunity.lastEventDateT/nextScheduledEventDateT 公式字段）。

3. **线索查重**：提交 Lead 时自动检查重复（相同企业名/邮箱/电话），提示用户合并或跳过（参考 🟢 Axelor DuplicateObjectsCrmService / LeadDuplicateService）。

4. **事件提醒 Job**：ErpCrmEvent.status=PLANNED 且 startDateTime 临近时，通过 nop-job 发送通知（参考 🟢 Axelor EventReminderJob）。

5. **丢单原因必填**：docStatus→LOST 时，lostReasonId 必填。

## 业财过账

CRM 本身不直接产生会计凭证（报价单/订单的凭证在 sales 域生成）。CRM 域**无独立 businessType**。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| sales（ErpSalQuotation） | IErpSalQuotationBiz 创建报价单（弱指针反查，核心零污染） |
| master-data（ErpMdPartner） | 客户主数据（Lead 转化时创建/关联） |
| nop-sys（定时任务） | 事件提醒（EventReminderJob） |

### 衔接契约

```
IErpCrmConversionBiz（CRM 转化服务）
  └─ convertToQuotation(leadId, quotationData) → ErpSalQuotation
       ├─ 校验 leadType == OPPORTUNITY
       ├─ 调用 IErpSalQuotationBiz 创建报价单（跨域通过 I*Biz 接口）
       ├─ 回写 lead.relatedBillType=SALES_QUOTATION + relatedBillCode（弱指针）
       └─ lead.docStatus → CONVERTED

  └─ convertToCustomer(leadId) → ErpMdPartner
       ├─ 校验 leadType == LEAD
       ├─ 创建 ErpMdPartner（从 Lead 的联系人/公司名派生）
       ├─ 创建 ErpCrmLead(leadType=OPPORTUNITY, partnerId=新建客户)
       └─ lead.docStatus → CONVERTED
```

> **核心零污染**：转化结果存在 CRM 侧（lead.relatedBillType/Code），sales 实体（ErpSalQuotation）零字段新增。

> **实现偏离补注**（2026-07-04，plan 2026-07-04-0549-2）：转化动作（convertToCustomer/convertToQuotation）实现于 `ErpCrmLeadBizModel`（`@BizModel("ErpCrmLead")`）而非独立的 `ErpCrmConversion` BizModel——非实体 BizModel 不会被 GraphQL 自动注册为业务对象（unknown-biz-obj-name）。`IErpCrmConversionBiz` 契约接口保留为衔接 seam，由 `ErpCrmLeadBizModel` 实现，GraphQL 动作名为 `ErpCrmLead__convertToCustomer` / `ErpCrmLead__convertToQuotation`。另：`IErpCrmLeadBiz.lose` 的 `lostReasonId`/`lostReasonDesc` 标注 `@Optional`（Nop GraphQL 对 `@Name` 参数默认非空校验，须显式 `@Optional` 才能让业务校验在缺失时抛 `ERR_LOST_REASON_REQUIRED`）。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.auto-convert-duplicate-lead` | false | 发现重复线索时是否自动合并 |
| `erp-crm.event-reminder-cron` | — | 事件提醒 cron（如每小时） |
| `erp-crm.default-team-id` | — | 新线索默认团队 |

## 菜单归属

crm 域 TOPM「客户关系」，分组：
- 线索/商机（含可配置列表视图，按 leadType 筛选）
- 线索来源
- 丢单原因
- 漏斗阶段
- 活动类别
- 销售团队
- 营销活动
- 活动日历（按日/周/月视图展示 ErpCrmEvent）

## 反模式警示

- ⛔ **在 sales 实体加 opportunityId**（核心污染）——🟢 Odoo `sale_crm/models/crm_lead.py:13` 反例；本项目转化结果用 CRM 侧弱指针反查。
- ⛔ **漏斗阶段硬编码 enum**——阶段是可配置记录（🟢 Odoo `crm_stage.py:14-33`，🟢 Axelor Opportunity.opportunityStatus）。
- ⛔ **Lead/Opportunity 拆两张表**——单实体 + type 判别更精简（🟢 Axelor 也拆了 Lead 和 Opportunity 两个独立实体，但本项目用单实体 + type 判别更简洁）。
- ⛔ **活动日志与 Event 混淆**——Event（日历/时长/提醒/排程）和 Activity（轻量操作记录）用途不同，不当成同一实体。

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| Lead→Opportunity 转化流 | 🟢 | Axelor `ConvertLeadWizardService.java` + Odoo `crm_lead.py:84-125` |
| 活动/事件管理（Event+Reminder） | 🟢 | Axelor `Event.xml`（关联 Lead/Opportunity/Partner，含提醒、优先级、重复） |
| 漏斗阶段可配置表 | 🟢 | Odoo `crm_stage.py:14-33` + Axelor Opportunity.opportunityStatus |
| UTM 归因 | 🟢 | Odoo `crm_lead.py:24-28` |
| 线索查重 | 🟢 | Axelor `DuplicateObjectsCrmService.java` + `LeadDuplicateService.java` |
| 丢单原因 | 🟢 | Axelor `LostReason` 实体 |
| 销售团队分配 | 🟢 | Axelor Lead.team / Opportunity.team |
| sale.order.opportunity_id 污染（反模式） | 🟢 | Odoo `sale_crm/models/crm_lead.py:13` |
| ERPNext CRM 边界（Quotation 在 selling） | 🟢 | `selling/doctype/Quotation` vs `crm/doctype/opportunity` |
| IDURAR Lead→Quote→Invoice 端到端 | 🟢 | IDURAR 源码实测（8504⭐） |
| 本项目 ErpSalQuotation | 🟢 | `module-sales/...orm.xml:97` |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §CRM
- `docs/analysis/erp-survey/2026-06-30-0000-idurar-erp-crm.md`
- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.1
- `cpq.md` — CPQ 配置-定价-报价引擎
- `sales-sequence.md` — 销售序列/跟进流程管理
- `lead-waterfall.md` — 线索漏斗分析
- `docs/design/sales/README.md`（与 sales 边界）
- `docs/design/master-data/README.md`（合作伙伴主数据）
