# 售后服务/客服工单域（customer-service）

## 目的

设计售后服务与客户支持模块：客户工单（Ticket）登记 → SLA 管理 → 分派处理 → 解决关闭的全流程。与质量 NCR、设备维护、CRM 衔接。

## 边界

- 本模块负责：客服工单（Ticket）管理、工单类型/优先级配置、SLA 策略与计时、团队分派、知识库/FAQ。
- **与 quality 的边界**：质量 NCR 是"内部不合格事件"，客服工单是"客户发起的服务请求"。NCR 可能升级为批量退货或召回；客服工单可能触发现场服务。两者独立但不互斥。
- **与 maintenance 的边界**：维护工单（Request）是设备报修；客服工单是客户服务请求。设备报修从客服工单可触发维护流程。
- **与 CRM 的边界**：CRM 管售前（线索→商机→报价），客服管售后（工单→解决）。客户信息统一从 ErpMdPartner 引用。
- 本模块不负责：设备维护执行（maintenance 域）；质量不合格处理（quality 域）；现场服务执行（intervention 域的现场派工）。

## 设计依据

> 参考 **Axelor Helpdesk**（26 Java + 23 XML）：Ticket + SLA 策略 + 工单类型 + 状态配置 + 团队分派 + 计时器。`AppHelpdesk` 配置：isSla/manageTimer/resolvedTicketStatus/closedTicketStatus/inProgressTicketStatus/defaultTicketStatus。
>
> 参考 **ERPNext Support** 顶层域。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §售后服务。

## 实体清单

> 表前缀 `erp_cs_`（Customer Service）、类名 `ErpCs*`、字典 `erp-cs/*`。

### ErpCsTicket（客服工单）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准（code 为工单编号，格式 TK{YYYYMM}{SEQ4}） | 🟢 Axelor Ticket.ticketSeq |
| subject | 工单主题 | 🟢 Axelor Ticket.subject |
| description | 问题描述（large=true） | 🟢 Axelor Ticket.description |
| customerId | 客户（→ErpMdPartner） | 🟢 Axelor Ticket.customerPartner |
| contactId | 联系人（→ErpMdPartner） | 🟢 Axelor Ticket.contactPartner |
| ticketTypeId | 工单类型（→ErpCsTicketType） | 🟢 Axelor Ticket.ticketType |
| priority | dict `erp-cs/ticket-priority`：LOW/NORMAL/HIGH/URGENT | 🟢 Axelor Ticket.prioritySelect |
| source | dict `erp-cs/ticket-source`：PHONE/EMAIL/PORTAL/CHAT/SOCIAL | — |
| assignedToId | 分配处理人（→User） | 🟢 Axelor Ticket.assignedToUser |
| responsibleId | 负责人（→User） | 🟢 Axelor Ticket.responsibleUser |
| teamId | 处理团队（→ErpCsTeam） | 🟢 Axelor Sla.team |
| slaPolicyId | SLA 策略（→ErpCsSlaPolicy） | 🟢 Axelor Ticket.slaPolicy |
| deadlineDateTime | SLA 截止时间（创建时按策略计算） | 🟢 Axelor Ticket.deadlineDateT |
| isSlaCompleted | SLA 是否完成 | 🟢 Axelor Ticket.isSlaCompleted |
| startDateTime | 开始处理时间 | 🟢 Axelor Ticket.startDateT |
| endDateTime | 关闭时间 | 🟢 Axelor Ticket.endDateT |
| duration | 处理时长（分钟） | 🟢 Axelor Ticket.duration |
| progress | 进度百分比（0-100） | 🟢 Axelor Ticket.progressSelect |
| relatedBillType/relatedBillCode | 关联业务单（销售订单/出库单等，弱指针） | — |
| status | dict `erp-cs/ticket-status`：NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED | 🟢 Axelor TicketStatus |
| 标准审计字段 | | |

**状态机**：

```
NEW（新建工单）
  ├─ 分派 → ASSIGNED（已分配处理人）
  │           └─ 开始处理 → IN_PROGRESS（处理中，SLA 开始计时）
  │                            ├─ 解决 → RESOLVED（已给出解决方案，等待客户确认）
  │                            │           └─ 客户确认 → CLOSED（终态）
  │                            └─ → CANCELLED（取消，终态）
  └─ → CANCELLED（取消，终态）
```

SLA 从 NEW 创建时开始计时，到 RESOLVED 时停止。deadlineDateTime 作为超时监控依据。

### ErpCsTicketType（工单类型）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| defaultPriority | 默认优先级 |
| defaultSlaPolicyId | 默认 SLA 策略 |
| sequence | 排序 |

### ErpCsSlaPolicy（SLA 策略）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/name/orgId | 标准 | 🟢 Axelor Sla |
| ticketTypeId | 适用工单类型 | 🟢 Axelor Sla.ticketType |
| minPriority | 最低触发优先级 | 🟢 Axelor Sla.prioritySelect |
| teamId | 适用团队 | 🟢 Axelor Sla.team |
| resolveHours | 解决时限（小时） | 🟢 Axelor Sla.hours |
| resolveDays | 解决时限（天） | 🟢 Axelor Sla.days |
| isWorkingDays | 是否仅计算工作日 | 🟢 Axelor Sla.isWorkingDays |
| escalationUserId | 升级通知人（超时后通知） | — |
| description | 说明 | 🟢 Axelor Sla.description |

### ErpCsTeam（客服团队）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| teamLeaderId | 团队负责人 |
| memberIds | 团队成员（many-to-many → User） |

### ErpCsTicketAction（工单操作日志）

| 字段 | 含义 |
|------|------|
| id/ticketId/orgId | 标准 |
| actionType | dict：ASSIGN（分派）/ NOTE（备注）/ ATTACH（附件）/ ESCALATE（升级）/ CLOSE（关闭） |
| fromStatus/toStatus | 状态变更 |
| operatorId | 操作人 |
| content | 操作内容 |
| 标准审计字段 | |

### ErpCsKnowledgeBase（知识库/FAQ）

已落地能力：`searchKnowledge(keyword, categoryId?, limit?)` / `suggestForTicket(subject, limit?)` `@BizQuery`（LIKE 关键词匹配，对已发布文章按 title+content 命中返回 Top N；详见 UC-CS-05）。

| 字段 | 含义 |
|------|------|
| id/code | 标准 |
| title | 标题 |
| content | 正文 |
| categoryId | 分类（→ErpCsCannedCategory） |
| isPublished | 是否发布 |
| remark | 备注 |
| 标准审计字段 | |

## 业务规则

1. **SLA 自动计时**：工单创建时按 `ErpCsSlaPolicy` 计算 `deadlineDateTime`。IN_PROGRESS → RESOLVED 时停止计时并标记 `isSlaCompleted`。超时触发升级通知。
2. **分派规则**：NEW 时按 ticketType + team 自动匹配处理人（轮转/最少未结工单），也支持手动分派。
3. **工单与业务单关联**：通过 `relatedBillType/relatedBillCode` 弱指针关联销售订单/出库单等（核心零污染）。
4. **知识库建议**：创建工单时按 subject 关键词搜索知识库，向客户推荐可能解决方案。
5. **工单关闭前检查**：CLOSED 前必须确保 `isSlaCompleted`（超时工单需注明原因）。

## 业财过账

客服工单本身不产生会计凭证。触发的现场服务或售后维修通过 maintenance/intervention 域过账。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| CRM（ErpCrmLead） | 售后工单可关联售前线索（同一客户的全生命周期视图） |
| quality（NCR） | 质量问题升级到 NCR：工单中确认是产品质量问题 → 创建 ErpQaNonConformance |
| maintenance（Request） | 设备报修：工单触发维护请求 → 走 maintenance 流程 |
| master-data（ErpMdPartner） | 客户/联系人主数据 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.sla-enabled` | true | 是否启用 SLA 计时 |
| `erp-cs.auto-assign-on-create` | true | 新建工单是否自动分派 |
| `erp-cs.escalation-notify-hours` | 2 | SLA 超时后升级通知延迟（小时） |

## 菜单归属

新增 cs 域 TOPM「售后服务」，分组：
- 客服工单（列表/详情/看板视图）
- 工单类型
- SLA 策略
- 客服团队
- 知识库
- 工单看板（按状态分组，参考 Axelor Dashboard.xml）

## 反模式警示

- ⛔ **工单与 NCR 混为一谈**——工单是客户服务入口，NCR 是内部质量事件，生命周期和 SLA 不同。
- ⛔ **硬编码工单状态/优先级**——TicketStatus/TicketType/priority 都应该是可配置字典。
- ⛔ **工单直接写库存/财务**——工单只记录服务请求，涉及退换货/维修的库存变动走标准退货/出入库流程。

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| Ticket + TicketType + TicketStatus 三层 | 🟢 | Axelor `Ticket.xml`（含 SLA/分派/优先级/计时/进度） |
| SLA 策略（时效/工作日/团队/升级） | 🟢 | Axelor `Sla.xml`（days/hours/isWorkingDays/team/ticketType/prioritySelect） |
| AppHelpdesk 配置（状态映射/计时/SLA开关） | 🟢 | Axelor `AppHelpdesk.xml`（resolvedTicketStatus/closedTicketStatus/manageTimer/isSla） |
| ERPNext Support 顶层域 | 🟢 | `erpnext.md:33` support 顶层域 |
| 工单与 Partner 关联 | 🟢 | Axelor Ticket.customerPartner/contactPartner |
| 工单分派到 User/Team | 🟢 | Axelor Ticket.assignedToUser/responsibleUser |

## 实现偏离补注（2026-07-04 实现）

> 权威计划：`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md`。下列条目为 implementation-only 定性下相对设计文档的取舍，变更 ORM 字段即打破定性。

- **SLA 工作日模式仅跳周末**：`SlaPolicy.isWorkingDays=true` 时按 Mon-Fri 跳周末，不依赖节假日日历主数据（`ErpHolidayCalendar` 未确认存在）；精确工时累计（含 `workingHourStart/End` 工作时段窗口）归 Non-Goal（ORM 无 workingHour 字段）。
- **仅 L1 升级**：超时仅通知 `SlaPolicy.escalationUserId`（L1）；L2/L3 多级升级链归 Non-Goal（ORM 无 `secondEscalationUserId`/`escalationDelayHours`）。
- **无 SlaPause 实体**：SLA 暂停/恢复机制（设计 §2.2，需独立 `ErpCsTicketSlaPause` + `adjustedDeadlineDateTime`）归 Non-Goal，核心计时按 `deadlineDateTime` + `isSlaCompleted`。
- **`escalationUserId` 类型为 BIGINT**(long)：与 `assignedToId`/`operatorId`（`stdDomain=userId` VARCHAR(36)）不同源，通知逻辑按 long ID 解析用户。
- **Survey 状态时间戳派生**：`ErpCsSurvey` 无独立 `status` 列；状态由 `surveySentAt`/`respondedAt` 派生（PENDING=sentAt 空 / SENT=sentAt 有且 respondedAt 空 / COMPLETED=respondedAt 有），FAILED/EXPIRED 仅查询期判定。
- **`action-type` 字典不扩**：审计 `actionType` 仅 ASSIGN/NOTE/ATTACH/ESCALATE/CLOSE/CANCEL；start/resolve/reopen 复用 NOTE，迁移语义由 `fromStatus`/`toStatus` 承载。
- **SLA 计时起点 = 首次 IN_PROGRESS**：`startDateTime` 在 start 动作时设置（非 NEW 创建时），`duration = resolve 时 now - startDateTime`。
- **配置项（均 config-gated）默认值**：`erp-cs.sla-enabled=true`、`erp-cs.sla-warning-before=60`（分钟）、`erp-cs.auto-assign-on-create=true`、`erp-cs.survey-enabled=true`、`erp-cs.survey-trigger-status=RESOLVED`、`erp-cs.survey-send-delay=0`（小时）、`erp-cs.survey-csat-enabled=true`、`erp-cs.survey-nps-enabled=false`、`erp-cs.survey-ces-enabled=false`、`erp-cs.survey-reminder-hours=48`、`erp-cs.survey-expire-days=7`。

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §售后服务
- `docs/design/quality/state-machine.md`（NCR 升级联动）
- `docs/design/maintenance/README.md`（维护请求联动）
- `docs/design/master-data/README.md`（合作伙伴主数据）
