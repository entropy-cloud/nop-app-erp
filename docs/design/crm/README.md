# CRM 域（crm）— 设计骨架

## 目的

设计客户关系管理（CRM）模块：线索（Lead）→ 商机（Opportunity）→ 转化为销售报价单的漏斗管理。补齐 P1 独立扩展模块缺口。

## 模块定位（Decision：独立扩展工程）

> **裁决**：CRM 定位为**可选独立扩展工程 `module-crm`**，**不纳入 product-scope 的 10 域基线**（`product-scope.md:49-52` 明确外部集成/垂直行业为延迟范围），作为可选模块按需组装。

- 工程范式参考 `docs/design/l10n/cn-golden-tax.md`（独立 Maven 工程 + 独立 appName + 凭证指针反查核心域、不污染核心实体）。
- 命名：实体 `ErpCrm*`，表名 `erp_crm_*`，字典 `erp-crm/*`，appName `app-erp-crm`。
- 在 `app-erp-all/pom.xml` 作为**可选依赖**引入（按需启用）。
- **考虑的替代方案**：纳入核心域子模块（拒绝，因 product-scope 明确 CRM 为延迟范围）。
- **残留风险**：骨架文档与未来深化可能脱节，通过 Follow-up 触发条件（客户需求确认）缓解。

**实施级设计延迟声明**：本文为**设计骨架**（模块定位 + 最小实体清单 + 衔接契约），深化到实施级（完整字段/状态机/UI/用例）延迟到**客户行业需求确认**时触发。

## 边界

- 本模块负责：线索/商机登记、漏斗阶段管理、营销活动归因（UTM）、商机活动记录、商机转化为销售报价单。
- **与 sales 的边界**：CRM 管线索到商机，**商机转化结果交接给 sales 域的报价单 `ErpSalQuotation`**（`module-sales/...orm.xml:97` 实测存在）。sales 域从报价单起（`sales/README.md:9`），与 ERPNext `selling/doctype/Quotation` vs `crm/doctype/opportunity` 的边界划分一致（🟢 ERPNext 边界确认）。
- 本模块不负责：报价单/订单/出库/开票（sales 域）；客户主数据（master-data 域 `ErpMdPartner`）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.1。

### 核心设计点

1. **单实体 + type 判别**：建议 `ErpCrmLead`，`leadType ∈ {LEAD, OPPORTUNITY}`，比 Lead/Opportunity 两表更精简（🟢 Odoo `crm_lead.py:84-125` `crm.lead` type 判别）。
2. **漏斗 = 可配置阶段表**：`ErpCrmStage`（有序记录 + 团队作用域 + 概率默认值，**非硬编码 enum**）（🟢 `crm_stage.py:14-33`）。
3. **营销活动归因**：UTM mixin（campaign/medium/source）（🟢 `crm_lead.py:24-28`）。
4. **核心零污染**：转化结果用弱指针 `relatedBillType=SALES_QUOTATION` 反查 sales 报价单，**不在 sales 实体加 opportunityId**（反 🟢 Odoo `sale_crm/models/crm_lead.py:13` `sale.order.opportunity_id` 外键污染销售核心反例）。

## 实体清单（最小骨架，标注延迟）

> 表前缀 `erp_crm_`、类名 `ErpCrm*`、字典 `erp-crm/*`。以下为建议命名，待客户需求触发后落地 ORM。

### ErpCrmLead（线索/商机，单实体 + type 判别，表 `erp_crm_lead`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| leadType | dict `erp-crm/lead-type`：LEAD（线索）/OPPORTUNITY（商机） |
| partnerId | 客户（→ErpMdPartner，线索阶段可空） |
| contactName/contactPhone/contactEmail | 联系人信息（线索阶段） |
| stageId | 当前漏斗阶段（→ErpCrmStage） |
| expectedRevenue | 预期收入（一次性） |
| recurringRevenue/recurringPlan | 周期性收入/MRR（🟢 `crm_lead.py:141-150`） |
| probability | 成交概率（默认取阶段概率，可覆盖） |
| campaignId/medium/source | UTM 归因（→ErpCrmCampaign） |
| ownerId | 负责销售员 |
| relatedBillType/relatedBillCode | 转化结果弱指针（SALES_QUOTATION + 报价单号） |
| docStatus | dict `erp-crm/lead-status`：NEW/QUALIFIED/CONVERTED/LOST |
| 标准审计字段 | |

**状态机（简化骨架）**：`NEW(线索) → QUALIFIED(商机，进入漏斗) → CONVERTED(已转报价单，终态)` 或 `→ LOST(流失，终态)`。详细阶段流转由 `ErpCrmStage` 配置驱动。

### ErpCrmStage（漏斗阶段表，表 `erp_crm_stage`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| stageName | 阶段名（如"新线索/已联系/已演示/谈判中/赢单"） |
| sequence | 排序（漏斗顺序） |
| teamId | 团队作用域（销售团队） |
| defaultProbability | 默认成交概率% |
| isWonStage | 是否赢单阶段 |
| 标准审计字段 | |

> **非硬编码 enum**：阶段是数据库记录，支持按团队自定义漏斗阶段（🟢 `crm_stage.py:14-33`）。

### ErpCrmActivity（商机活动，表 `erp_crm_activity`）

| 字段 | 含义 |
|---|---|
| id/leadId/orgId | 标准 |
| activityType | dict：CALL/EMAIL/MEETING/TASK |
| activityDate | 活动日期 |
| summary | 内容摘要 |
| 标准审计字段 | |

### ErpCrmCampaign（营销活动，UTM 归因，表 `erp_crm_campaign`）

| 字段 | 含义 |
|---|---|
| id/code/name/orgId | 标准 |
| campaignName | 活动名 |
| medium/source | UTM medium/source |
| startDate/endDate | 活动区间 |
| budgetAmount/actualCost | 预算/实际成本 |
| 标准审计字段 | |

### ErpCrmLeadConvLog（阶段流转审计，表 `erp_crm_lead_conv_log`）

| 字段 | 含义 |
|---|---|
| id/leadId/orgId | 标准 |
| fromStageId/toStageId | 前/后阶段 |
| changedAt/changedBy | 变更时间/人 |
| 标准审计字段 | |

## 衔接契约（SPI：内部转化服务，无外部 SPI）

CRM 是业务模块，**无外部 SPI**（不像 TMS/EDI 对接外部系统），仅内部转化服务：

```
IErpCrmConversionBiz（CRM 内部转化服务）
  └─ convertToQuotation(leadId, quotationData) → ErpSalQuotation
       ├─ 校验 leadType == OPPORTUNITY
       ├─ 调用 IErpSalQuotationBiz 创建报价单（跨域通过 I*Biz 接口）
       ├─ 回写 lead.relatedBillType=SALES_QUOTATION + relatedBillCode（弱指针，核心零污染）
       └─ lead.docStatus → CONVERTED
```

> **核心零污染**：转化结果存在 CRM 侧（lead.relatedBillType/Code），**sales 实体（ErpSalQuotation）零字段新增**——反 🟢 Odoo `sale_crm/models/crm_lead.py:13` 在 `sale.order` 加 `opportunity_id` 的污染反例。

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-crm.enabled` | false | CRM 模块是否启用（可选扩展） |

## 跨域协作

| 对端 | 协作方式 |
|---|---|
| sales（ErpSalQuotation） | `IErpSalQuotationBiz` 创建报价单（弱指针反查，核心零污染） |
| master-data（ErpMdPartner） | 客户主数据 |

## 反模式警示

- ⛔ **在 sales 实体加 opportunityId**（核心污染）——🟢 Odoo `sale_crm/models/crm_lead.py:13` 反例；本项目转化结果用 CRM 侧弱指针反查。
- ⛔ **漏斗阶段硬编码 enum**——阶段是可配置记录（🟢 `crm_stage.py:14-33`），支持按团队自定义。

## 菜单归属

新增 crm 域 TOPM「客户关系」（可选，启用时显示），分组：线索/商机、漏斗阶段、营销活动。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| 单实体 + type 判别 | 🟢 | Odoo `crm_lead.py:84-125` 源码实测 |
| 漏斗阶段可配置表 | 🟢 | Odoo `crm_stage.py:14-33` 源码实测 |
| 金额三件套（一次性+周期） | 🟢 | Odoo `crm_lead.py:141-150` 源码实测 |
| UTM 归因 mixin | 🟢 | Odoo `crm_lead.py:24-28` 源码实测 |
| sale.order.opportunity_id 污染（反模式） | 🟢 | Odoo `sale_crm/models/crm_lead.py:13` 源码实测 |
| ERPNext CRM 边界（Quotation 在 selling） | 🟢 | `selling/doctype/Quotation` vs `crm/doctype/opportunity` 源码实测 |
| 本项目 ErpSalQuotation 存在 | 🟢 | `module-sales/...orm.xml:97` 实测 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.1（设计依据）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/design/sales/README.md`（与 sales 边界）
- `docs/requirements/product-scope.md:49-52`（延迟范围）
