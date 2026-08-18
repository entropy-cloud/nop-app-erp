# 售后服务/客服工单域（customer-service）

## 目的

设计售后服务与客户支持模块：客户工单（Ticket）登记 → SLA 管理 → 分派处理 → 解决关闭的全流程。与质量 NCR、设备维护、CRM 衔接。

## 边界

- 本模块负责：客服工单（Ticket）管理、工单类型/优先级配置、SLA 策略与计时、团队分派、知识库/FAQ、客户满意度回访（CSAT/NPS/CES）。
- **与 quality 的边界**：质量 NCR 是"内部不合格事件"，客服工单是"客户发起的服务请求"。NCR 可能升级为批量退货或召回；客服工单可能触发现场服务。两者独立但不互斥。
- **与 maintenance 的边界**：维护工单（Request）是设备报修；客服工单是客户服务请求。设备报修可从客服工单触发维护流程。
- **与 CRM 的边界**：CRM 管售前（线索→商机→报价），客服管售后（工单→解决）。客户信息统一从 ErpMdPartner 引用。
- 本模块不负责：设备维护执行（maintenance 域）；质量不合格处理（quality 域）；现场服务执行（intervention 域的现场派工）。
- 持久化字段、字典、状态码以 `module-cs/model/app-erp-cs.orm.xml` 为准。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-cs` |
| appName | `erp-cs`（两级） |
| 权威模型 | `module-cs/model/app-erp-cs.orm.xml` |
| 实体包 | `app.erp.cs.dao.entity` |
| 表前缀 | `erp_cs_` |
| 类名前缀 | `ErpCs*` |
| 字典命名空间 | `erp-cs/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 客服工单（Ticket） | 客户发起的服务请求主记录：工单主题与描述、客户与联系人、工单类型、优先级（低/中/高/紧急）、来源渠道、处理人与负责人、处理团队、SLA 策略与截止时间、开始/关闭时间与处理时长、进度、关联业务单弱指针 |
| 工单类型（TicketType） | 工单分类，绑定默认优先级与默认 SLA 策略 |
| SLA 策略（SlaPolicy） | 服务时效规则：适用工单类型、最低触发优先级、适用团队、解决时限（小时或天）、是否仅计工作日、超时升级通知人 |
| 客服团队（Team） | 处理团队：负责人与成员 |
| 工单操作日志（TicketAction） | 工单状态变更与操作的审计日志：分派/备注/附件/升级/关闭/取消，含前后状态与操作内容 |
| 知识库（KnowledgeBase） | 可发布的知识/FAQ 文章，支持按标题与正文关键词检索，并按工单主题智能推荐候选方案（详见 use-cases.md） |

## 状态机

工单状态：`NEW → ASSIGNED（已分配处理人）→ IN_PROGRESS（处理中，SLA 开始计时）→ RESOLVED（已给出解决方案，等待客户确认）→ CLOSED（终态）`；任一阶段可 `→ CANCELLED`。SLA 从首次进入 IN_PROGRESS 开始计时，到 RESOLVED 停止。详细规则见 [`state-machine.md`](state-machine.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 售前/售后全生命周期视图 | crm | 售后工单可关联售前线索（同一客户） |
| 质量问题升级 | quality | 工单中确认是产品质量问题 → 创建 ErpQaNonConformance |
| 设备报修 | maintenance | 工单触发维护请求 → 走 maintenance 流程 |
| 客户/联系人主数据 | master-data | 引用 ErpMdPartner |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

1. **SLA 自动计时**：工单创建时按 SLA 策略计算截止时间；首次进入 IN_PROGRESS 起算，RESOLVED 时停止并标记完成。超时触发升级通知。
   - *实现注记（RC-R1.65，plan `2026-08-17-2125-1`）*：创建路径富化 = save 后置钩子（`doSaveEntity`）自动触发 `matchAndAttachSla`（fill-when-absent：slaPolicyId 与 deadlineDateTime 均空才挂载——策略匹配 + deadline 计算 + 权益扣减 UC-CS-09 单次联动三合一）；缺省填充 status=NEW + priority←工单类型默认；`TK{YYYYMM}{SEQ4}` 编号按月序列自动生成（显式 code 不覆盖）。
2. **分派规则**：NEW 时按工单类型与团队自动匹配处理人（轮转/最少未结工单），也支持手动分派。
   - *实现注记（RC-R1.65）*：自动分配经 `erp-cs.auto-assign-on-create`（默认 true）门控 + `erp-cs.assign-method`（ROUND_ROBIN | LEAST_OPEN，默认 ROUND_ROBIN）；候选池 = SLA 策略 teamId → 客服团队 → **按 code 相等约定映射同码 crm 团队成员**（`ErpCrmTeamMember.userId`，跨域约定——无同码 crm 团队/成员时池空）；分配成功 NEW→ASSIGNED + ASSIGN 审计；无匹配留 NEW 并升级通知客服主管（`cs.ticket-assign-no-match`）；创建确认通知 `cs.ticket-created`（接收人=提单人 createdBy，IN_APP 占位语义，实际邮件/门户投递归 nop-notification successor）。
3. **工单与业务单关联**：通过弱指针（relatedBillType/relatedBillCode）关联销售订单/出库单等（核心零污染）。
4. **知识库建议**：创建工单时按主题关键词检索知识库，向客户推荐可能解决方案。
5. **工单关闭前检查**：CLOSED 前必须确保 SLA 已完成（超时工单需注明原因）。
6. **审计动作类型固定**：操作日志 actionType 仅 ASSIGN/NOTE/ATTACH/ESCALATE/CLOSE/CANCEL；start/resolve/reopen 复用 NOTE，状态迁移语义由前后状态承载。

## 业财过账

客服工单本身不产生会计凭证。触发的现场服务或售后维修通过 maintenance/intervention 域过账。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.sla-enabled` | true | 是否启用 SLA 计时 |
| `erp-cs.sla-warning-before` | 60 | SLA 即将超时预警提前量（分钟） |
| `erp-cs.auto-assign-on-create` | true | 新建工单是否自动分派 |
| `erp-cs.escalation-notify-hours` | 2 | SLA 超时后升级通知延迟（小时） |
| `erp-cs.survey-enabled` | true | 是否启用满意度回访 |
| `erp-cs.survey-trigger-status` | RESOLVED | 触发回访问卷的工单状态 |
| `erp-cs.survey-send-delay` | 0 | 问卷发送延迟（小时） |
| `erp-cs.survey-csat-enabled` | true | 是否启用 CSAT |
| `erp-cs.survey-nps-enabled` | false | 是否启用 NPS |
| `erp-cs.survey-ces-enabled` | false | 是否启用 CES |
| `erp-cs.survey-reminder-hours` | 48 | 问卷催填间隔（小时） |
| `erp-cs.survey-expire-days` | 7 | 问卷过期天数 |

## 菜单归属

cs 域 TOPM「售后服务」，分组：客服工单（列表/详情/看板）、工单类型、SLA 策略、客服团队、知识库、工单看板（按状态分组）。

## 反模式警示

- ⛔ **工单与 NCR 混为一谈**——工单是客户服务入口，NCR 是内部质量事件，生命周期和 SLA 不同。
- ⛔ **硬编码工单状态/优先级**——工单状态/类型/优先级都应是可配置字典。
- ⛔ **工单直接写库存/财务**——工单只记录服务请求，涉及退换货/维修的库存变动走标准退货/出入库流程。

## 延迟项与非目标

- **SLA 工作日模式仅跳周末**：策略标记"仅计工作日"时按周一至周五跳周末，不依赖节假日日历主数据；精确工时累计（含工作时段窗口）归 Non-Goal。
- **多级升级链已实现（RC-R1.67，plan `2026-08-17-2125-3`）**：deadline 超时 → L1 通知 `slaPolicy.escalationUserId` → 每 `escalationDelayHours`（policy 优先，默认 2h）重复通知（上限 1+`erp-cs.escalation-max-repeat`，默认重复 3 次）→ L2 通知 `secondEscalationUserId`（空则跳级）→ L3 通知 config 总监（`erp-cs.escalation-l3-user-id`）→ level=3 封顶；完整判定式见 `sla.md` §3.2 实现注记。**行为变更声明**：L1/预警通知接收人从模板 ROLE 客服主管重路由到策略指定人（policy.escalationUserId 优先，缺失回退 assignedToId，UC-CS-04 ③/⑤ 漂移修正）。
- **无 SLA 暂停/恢复实体**：SLA 暂停与恢复机制（需独立暂停记录与调整后截止时间）归 Non-Goal，核心计时按截止时间与完成标记。
- **回访问卷状态派生**：问卷状态由发送时间与响应时间派生（待发送/已发送/已响应），失败/过期仅在查询期判定。
- **升级通知人 ID 源不同**：升级通知人为数值型用户 ID，与工单分派人/操作人的用户标识不同源，通知逻辑按数值 ID 解析用户。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、工单与 SLA 模型、跨域协作 |
| `state-machine.md` | 工单状态机 |
| `sla.md` | SLA 策略与计时机制 |
| `entitlement.md` | 客户服务权益/合同衔接 |
| `service-catalog.md` | 服务目录 |
| `canned-response.md` | 快捷回复 |
| `csat.md` | 满意度回访（CSAT/NPS/CES） |
| `time-tracking.md` | 工单工时跟踪 |
| `ui-patterns.md` | 前端模式（工单看板） |
| `use-cases.md` | 用例 |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §售后服务（源码分析见 erp-survey）
- `docs/design/quality/state-machine.md`（NCR 升级联动）
- `docs/design/maintenance/README.md`（维护请求联动）
- `docs/design/master-data/README.md`（合作伙伴主数据）
