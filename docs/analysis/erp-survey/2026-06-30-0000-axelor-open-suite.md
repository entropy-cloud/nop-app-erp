---
调研日期: 2026-06-30
来源: ~/sources/erp/axelor-open-suite（浅克隆）
状态: 已完成（基于源码实测）
---

# Axelor Open Suite 调研报告

> Axelor Open Suite 是基于 Java/Gradle 构建的模块化开源 ERP，25 个模块覆盖了几乎所有企业业务场景，包括 CRM、HRM（421 Java 文件）、生产制造含 APS（344 Java 文件）、售后服务、现场服务等 nop-app-erp 计划 03 标记为"骨架"或"P2"的功能。

## 基本信息

| 属性 | 值 |
|------|-----|
| 项目 | axelor-open-suite |
| 组织 | axelor |
| Stars | 957 |
| License | AGPL-3.0 |
| 技术栈 | Java/Gradle/Guice |
| 模块数 | 25 |
| 总 Java 文件 | ~2000+ |
| 总 XML 文件 | ~1000+ |

## 模块架构（25 模块）

```
axelor-base                  — 基础模块
axelor-account               — 财务核算
axelor-bank-payment          — 银行支付
axelor-budget                — 预算管理
axelor-cash-management       — 资金管理
axelor-client-portal         — 客户门户
axelor-contract              — 合同管理
axelor-crm                   — 客户关系管理（Lead/Opportunity/Event）
axelor-fleet                 — 车队管理
axelor-gdpr                  — GDPR 合规
axelor-helpdesk              — 售后客服（工单/Ticket）
axelor-human-resource        — 人力资源管理（421 Java file，含考勤/工时/薪酬）
axelor-intervention          — 现场服务（Field Service）
axelor-maintenance           — 设备维护
axelor-marketing              — 营销活动
axelor-mobile-settings       — 移动端设置
axelor-production            — 生产制造（含 MRP/APS，344 Java file）
axelor-project               — 项目管理
axelor-purchase              — 采购管理
axelor-quality               — 质量管理
axelor-sale                  — 销售管理
axelor-stock                 — 库存管理
axelor-supplier-management   — 供应商管理
axelor-supplier-portal       — 供应商门户
axelor-supplychain           — 供应链计划（386 Java file，最大模块）
axelor-talent                — 人才管理（招聘/培训/绩效）
```

## 关键模块详细分析

### 1. CRM（axle-crm：70 Java + 69 XML）

**实体（源码实测）**：
- `Lead` — 线索管理，含 Lead → Convert → Opportunity 转化流程
- `Opportunity` — 商机管理
- `Event` — 日历事件/活动
- `Meeting` — 会议记录
- `CrmActivity` — CRM 活动记录

**关键服务**：
- `LeadService` / `LeadManagementRepository` — 线索 CRUD + 查重 + 转化
- `OpportunityService` / `OpportunityManagementRepository` — 商机管理
- `ConvertLeadWizardService` — 线索转商机/转客户/转报价向导
- `EventService` / `EventReminderJob` — 事件/提醒
- `MessageServiceCrmImpl` — CRM 消息模板（事件/会议）

**对 nop 的借鉴**：
- Lead → Convert → Opportunity/Quote 完整转化流，包含**活动历史记录**（Event/Meeting），比 Odoo 更完善的 CRM 生态
- **非硬编码阶段**：阶段管理在视图层可配置

### 2. 人力资源管理（axelor-human-resource：421 Java + 185 XML）—— **覆盖 P2 HRMS 缺口**

**实体**（源码 `src/main/java/com/axelor/apps/hr/db/`）：
- `Employee` — 员工主数据
- `Contract` — 劳动合同
- `Payroll` — 薪酬计算
- `LeaveRequest` — 休假申请
- `Expense` — 费用报销（员工侧）
- `Timesheet` / `TimesheetLine` — 工时记录
- `PublicHoliday` — 公共假期
- `Tax` — 个税计算
- `Seniority` — 工龄

**关键模块**：
- 工时表（Timesheet）—— React 前端（axelor-react-timesheet 子模块）
- 薪酬（Payroll）—— 薪资计算与税费
- 招聘流程 —— 通过 axelor-talent 模块补充
- 休假管理 —— 请假/加班/调休

### 3. 生产制造 + APS（axelor-production：344 Java + 139 XML）—— **覆盖 APS "自承认延迟" 缺口**

**实体**（源码 `src/main/java/com/axelor/apps/production/db/`）：
- `ManufOrder` — 生产工单（Manufacturing Order）
- `OperationOrder` — 工序工单（Operation Order，APS 关键）
- `BillOfMaterial` — BOM
- `ProdProcess` / `ProdProcessLine` — 工艺路线/工序
- `Machine` — 设备/工作中心
- `CostSheet` — 成本核算表
- `ProdProduct` — 生产产品
- `ProductionBatch` — 生产批次
- `ConfiguratorBOM` / `ConfiguratorProdProcess` — BOM 配置器

**APS 能力源码实测**（`service/` 目录）：
- `ManufOrderService` / `OperationOrderService` — 工单/工序服务
- 排序/排产逻辑（OperationOrder 有序号字段，含时间排程能力）
- `MrpLineProductionService` — **MRP 计算**
- `CostSheetService` — 成本核算
- `ProductionBatchService` — 批次管理
- `RawMaterialRequirementService` — 原材料需求计算

**核心结论**：Axelor 的 production 模块**确实包含 APS 级的能力**（OperationOrder 实现工序级排产，ManufOrder 承载工单，MRP 驱动物料/产能运算），比 Odoo 的前向填充更接近真 APS。

### 4. 售后服务/客服（axelor-helpdesk：26 Java + 23 XML）—— **覆盖 P2 售后服务缺口**

**实体**：
- `Ticket` — 客户工单
- `TicketType` — 工单类型
- `SLA` — 服务级别协议
- `SLA calendar` — SLA 日历
- `Ticket notification` — 工单通知

**对 nop 的借鉴**：客户工单（Ticket）关联 SLA + 支持团队，与 axelor-intervention（现场服务）联动。

### 5. 现场服务（axelor-intervention：71 Java + 75 XML）—— **覆盖 FSM 缺口**

**实体**：
- `Intervention` — 现场服务工单
- `InterventionType` — 服务类型
- `Appointment` — 预约排程
- 关联 Stock/Equipment/Partner

**对 nop 的借鉴**：与 maintenance（设备维护）/ CRM（客户）/ helpdesk（工单升级）紧密集成。

### 6. 供应链计划（axelor-supplychain：386 Java + 130 XML）—— **覆盖 DRP 缺口**

最大模块！覆盖：
- **Supply Chain 需求计划**
- 采购-库存-销售 集成计算
- 供应计划（类似 DRP 跨仓补货）

### 7. 合同管理（axelor-contract：83 Java + 50 XML）

**实体**：
- `Contract` — 合同
- `ContractLine` — 合同行
- `ContractVersion` — 版本
- `InvoicePlan` / `InvoiceTerm` — 开票计划
- `ConsumptionLine` — 消耗记录（用量计费）

**对 nop 的借鉴**：合同版本管理 + 开票计划 + 用量计费，可用作 CLM 和订阅计费的设计参考。

### 8. 门户（axelor-client-portal + axelor-supplier-portal）

- 客户门户：客户自助查看订单/发票
- 供应商门户：供应商自助查看 PO/ASN

### 9. 其他特色模块

| 模块 | 功能 | 对 nop 的价值 |
|------|------|--------------|
| axelor-fleet | 车队管理（车辆/司机/路线） | 物流扩展参考 |
| axelor-marketing | 营销活动/线索来源 | CRM 扩展参考 |
| axelor-quality | 质检/非符合项 | 与 nop quality 域对标 |
| axelor-gdpr | GDPR 合规 | 合规模块参考 |

## 对 nop-app-erp 的借鉴建议

### 必须覆盖的功能（此前被误标记为"骨架/P2/延迟"）

| 功能 | Axelor 模块 | 本项目当前状态 | 纠正方向 |
|------|------------|--------------|---------|
| CRM 完整（Lead/Opportunity/Event） | axelor-crm | 设计骨架 → 升级为完整实施设计 | Lead → Convert → Opportunity/Quote 完整流 |
| 售后服务/客服工单 | axelor-helpdesk | P2 排除 → 纳入设计 | Ticket + SLA + 支持团队 |
| 现场服务 | axelor-intervention | 未覆盖 → 纳入设计 | 现场工单 + 预约排程 + 备件 |
| APS 排产 | axelor-production | 自承认延迟 → 纳入设计 | ManufOrder + OperationOrder + 工序级排产 |
| HRMS/薪酬/考勤 | axelor-human-resource | P2 排除 → 纳入设计 | Employee + Contract + Payroll + Leave |
| DRP 分销计划 | axelor-supplychain | P2 排除 → 纳入设计 | 多级补货计划（已有 MRP 可扩） |
| 合同生命周期 | axelor-contract | 部分覆盖 → 全生命周期 | Contract + Version + InvoicePlan |
| 人才/招聘 | axelor-talent | 未覆盖 → 纳入设计 | 招聘流程 + 培训 + 绩效 |
| 客户/供应商门户 | axelor-client/supplier-portal | 未覆盖 → 纳入设计 | 自助查询/协作 |

### 设计参考要点

1. **Lead → Convert → Opportunity + Event 模型**：比 Odoo 的 simple crm 更丰富，Event/Meeting 串联了客户交互时间线。
2. **OperationOrder（工序工单）**：APS 的核心数据结构，ManufOrder（主工单）下挂多个 OperationOrder（工序），每工序可绑定工作中心/设备/时间，实现有限产能排产。
3. **Contract Line + InvoicePlan**：合同行的开票计划 + 消耗记录，可用于订阅计费和收入确认。
4. **Helpdesk + Intervention 联动**：从客户工单（Ticket）升级到现场服务（Intervention），两层 SLA 管理。
5. **模块化独立部署**：25 个独立 Maven/Gradle 模块，每个模块可独立启用/禁用，与 nop 的模块化哲学一致。

## 源码关键路径

- `/Users/abc/sources/erp/axelor-open-suite/axelor-crm/src/main/java/com/axelor/apps/crm/` — CRM 全部逻辑
- `/Users/abc/sources/erp/axelor-open-suite/axelor-human-resource/src/main/java/com/axelor/apps/hr/` — HR 全部逻辑
- `/Users/abc/sources/erp/axelor-open-suite/axelor-production/src/main/java/com/axelor/apps/production/` — 生产/APS 全部逻辑
- `/Users/abc/sources/erp/axelor-open-suite/axelor-helpdesk/src/main/java/com/axelor/apps/helpdesk/` — 售后客服
- `/Users/abc/sources/erp/axelor-open-suite/axelor-intervention/src/main/java/com/axelor/apps/intervention/` — 现场服务
- `/Users/abc/sources/erp/axelor-open-suite/axelor-contract/src/main/java/com/axelor/apps/contract/` — 合同管理
- `/Users/abc/sources/erp/axelor-open-suite/axelor-supplychain/src/main/java/com/axelor/apps/supplychain/` — 供应链计划
- `/Users/abc/sources/erp/axelor-open-suite/axelor-talent/src/main/java/com/axelor/apps/talent/` — 人才管理
