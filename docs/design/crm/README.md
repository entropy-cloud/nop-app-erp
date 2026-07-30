# CRM 域（crm）

## 目的

设计客户关系管理（CRM）模块：线索获取 → 线索跟进 → 转化为商机 → 漏斗阶段管理 → 转化为销售报价单的全流程。包含活动历史（通话/邮件/会议/任务）、日历管理、营销活动归因（UTM）、销售团队协作。

## 边界

- 本模块负责：线索（Lead）管理、商机（Opportunity）管理、漏斗阶段配置、营销活动归因（UTM）、活动/事件记录、日历/会议、线索转商机/转报价单。
- **与 sales 的边界**：CRM 管线索到商机，商机转化结果通过弱指针交接给 sales 域的报价单 `ErpSalQuotation`。sales 域从报价单起，不在 sales 实体加任何 CRM 外键（核心零污染）。
- 本模块不负责：报价单/订单/出库/开票（sales 域）；客户/合作伙伴主数据（master-data 域 `ErpMdPartner`）；售后客服工单（customer-service 域）。
- 持久化字段、字典、状态码以 `module-crm/model/app-erp-crm.orm.xml` 为准。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 设计依据

参考 Axelor CRM（Lead→Opportunity 转化流、Event/Meeting 活动时间线、查重服务与事件提醒）、IDURAR（Lead→Quote→Invoice 端到端验证）。源码分析见 `docs/analysis/erp-survey/`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-crm` |
| appName | `erp-crm`（两级） |
| 权威模型 | `module-crm/model/app-erp-crm.orm.xml` |
| 实体包 | `app.erp.crm.dao.entity` |
| 表前缀 | `erp_crm_` |
| 类名前缀 | `ErpCrm*` |
| 字典命名空间 | `erp-crm/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 线索/商机（Lead） | 单实体 + leadType 判别（LEAD 线索 / OPPORTUNITY 商机），比 Lead/Opportunity 两表更精简。承载联系人/公司/职位信息、线索来源、线索状态、漏斗阶段、预期收入（含乐观/悲观范围与周期性收入 MRR）、成交概率、预期签单日、UTM 归因、负责销售员与团队、丢单原因与描述、最后联系与下次活动日期、转化结果弱指针 |
| 线索状态字典（LeadStatus） | 自有状态字典记录（非硬编码 enum），支持排序与默认值 |
| 漏斗阶段（Stage） | 可配置的漏斗阶段记录：阶段名、漏斗顺序、团队作用域、默认成交概率、是否赢单阶段（赢单后触发转化）。阶段是数据库记录，支持按团队自定义 |
| 线索来源（Source） | 线索来源字典记录 |
| 丢单原因（LostReason） | 丢单原因字典记录 |
| 活动/事件（Event） | CRM 活动时间线核心：覆盖通话/邮件/会议/任务的完整记录，含起止时间与时长、活动类别、关联线索/商机/客户/联系人、状态与优先级、重复事件与提醒。独立于 Activity，覆盖完整日历排程 |
| 活动类别（EventCategory） | 活动分类，带日历颜色 |
| 商机活动记录（Activity） | 轻量操作日志（仅记录"谁在何时做了什么"），不涉及时长/日历/提醒，与 Event 区分用途 |
| 营销活动（Campaign） | UTM 归因的营销活动：活动名、medium/source、活动区间、预算与实际成本 |
| 销售团队（Team） | 销售团队：负责人与成员 |
| 阶段流转审计（LeadConvLog） | 线索漏斗阶段变更的前后阶段、时间与操作人审计 |
| 报价模板（QuoteTemplate，可选） | 报价内容模板，支持占位符与默认模板标记 |

## 状态机

- 线索/商机：`NEW（新建）→ QUALIFIED（已验证，进入漏斗阶段管理）→ CONVERTED（已转报价单，终态）/ LOST（录入丢单原因，终态）/ CANCELLED（无效/重复，终态）`。漏斗阶段（stageId）是独立维度，由 Stage 的顺序驱动前移。
- 活动/事件：`PLANNED → COMPLETED` 或 `PLANNED → CANCELLED`。

详细规则见 [`state-machine.md`](state-machine.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 商机转报价单 | sales | 转化时调用 IErpSalQuotationBiz 创建 ErpSalQuotation（弱指针反查，核心零污染） |
| 客户主数据 | master-data | Lead 转化时创建/关联 ErpMdPartner |
| 事件提醒 | nop-sys（定时任务） | 到期事件经定时任务发送通知 |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

### 衔接契约

- **线索转客户**：LEAD 类型转化时创建客户主数据（ErpMdPartner）并生成对应商机（leadType=OPPORTUNITY，绑定新建客户），原线索 docStatus→CONVERTED。
- **商机转报价单**：OPPORTUNITY 类型转化时校验类型后调用 sales 域创建报价单（ErpSalQuotation），转化结果以弱指针（relatedBillType=SALES_QUOTATION + relatedBillCode）回写至 CRM 侧线索，sales 实体零字段新增（核心零污染）。

## 关键业务规则

1. **Lead→Convert→Opportunity→Quotation 转化流**：LEAD 转化→创建客户主数据 + 生成商机；OPPORTUNITY 转化→调用 sales 域创建报价单；转化后线索 docStatus=CONVERTED，转化结果弱指针写回。
2. **活动时间线自动派生**：线索的最后联系日期与下次活动日期从关联的活动/事件自动计算。
3. **线索查重**：提交线索时自动检查重复（相同企业名/邮箱/电话），提示用户合并或跳过。
4. **事件提醒**：活动状态为已计划且临近开始时间时，通过定时任务发送通知。
5. **丢单原因必填**：docStatus→LOST 时，丢单原因必填。

## 业财过账

CRM 本身不直接产生会计凭证（报价单/订单的凭证在 sales 域生成）。CRM 域无独立 businessType。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-crm.auto-convert-duplicate-lead` | false | 发现重复线索时是否自动合并 |
| `erp-crm.event-reminder-enabled` | true | 是否启用到期事件提醒查询 |
| `erp-crm.event-reminder-cron` | — | 事件提醒定时表达式（如每小时） |
| `erp-crm.default-team-id` | — | 新线索默认团队 |
| `erp-crm.lead-scoring.auto-qualify` | true | 评分达标是否自动转商机（复用 NEW→QUALIFIED） |
| `erp-crm.lead-scoring.recalc-on-lead-update` | true | 线索评分相关字段变更是否触发重新评分 |
| `erp-crm.forecast.commit-threshold` | 80 | 预测 commit 概率阈值（%） |
| `erp-crm.forecast.upside-threshold` | 30 | 预测 upside 概率阈值（%） |
| `erp-crm.forecast.accuracy-auto-compute` | true | 期间关闭后是否自动计算准确率 |

## 菜单归属

crm 域 TOPM「客户关系」，分组：线索/商机（可配置列表视图，按 leadType 筛选）、线索来源、丢单原因、漏斗阶段、活动类别、销售团队、营销活动、活动日历（按日/周/月视图）。

## 反模式警示

- ⛔ **在 sales 实体加 opportunityId**（核心污染）——转化结果用 CRM 侧弱指针反查，sales 实体零字段新增。
- ⛔ **漏斗阶段硬编码 enum**——阶段是可配置数据库记录，支持按团队自定义。
- ⛔ **Lead/Opportunity 拆两张表**——单实体 + type 判别更精简。
- ⛔ **活动日志与 Event 混淆**——Event（日历/时长/提醒/排程）和 Activity（轻量操作记录）用途不同，不当成同一实体。

## 实现落位提示

| 设计含义 | 默认实现落位 |
|----------|--------------|
| 转化动作（转客户/转报价单） | 归入 `ErpCrmLead` 业务对象而非独立 BizModel（非实体 BizModel 不会被 GraphQL 自动注册为业务对象）；`IErpCrmConversionBiz` 契约接口保留为衔接 seam，由 `ErpCrmLeadBizModel` 实现 |
| 丢单原因可选校验 | 丢单原因为可选参数，由业务校验在缺失时抛 `ERR_LOST_REASON_REQUIRED` |
| 活动时间线派生 | 采用推模式——活动状态变更时即时回写线索的最后联系/下次活动日期，查询零成本避免 N+1 |
| 到期提醒范围查询 | 范围查询由方法内部构造（XMeta 仅暴露等值/区间查询） |

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、线索/商机模型、转化与跨域协作 |
| `state-machine.md` | 线索/商机与活动状态机 |
| `cpq.md` | CPQ 配置-定价-报价引擎 |
| `sales-sequence.md` | 销售序列/跟进流程管理 |
| `lead-scoring.md` | 线索评分 |
| `lead-waterfall.md` | 线索漏斗分析 |
| `sales-forecast.md` | 销售预测 |
| `marketing.md` | 营销活动与 UTM 归因 |
| `territory.md` | 销售区域 |
| `ui-patterns.md` | 前端模式 |
| `use-cases.md` | 用例 |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §CRM（源码分析见 erp-survey）
- `docs/analysis/erp-survey/2026-06-30-0000-idurar-erp-crm.md`
- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.1
- `docs/design/sales/README.md`（与 sales 边界）
- `docs/design/master-data/README.md`（合作伙伴主数据）
