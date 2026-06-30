# 销售报价单（Sales Quotation）

## 目的

设计销售报价单的生命周期：创建 → 客户确认 → 转销售订单。衔接 CRM 域（线索→商机转化）与 sales 域订单管理的前端环节。

## 边界

- 本模块负责：报价单创建/审批/发送客户/客户确认/转订单/过期管理。
- **与 CRM 的边界**：CRM 管线索→商机→转化时调用 `IErpSalQuotationBiz` 创建报价单。报价单的后续生命周期属 sales 域。
- **与 sales 订单的边界**：报价单客户确认后转为 `ErpSalOrder`，走 `sales/state-machine.md` 订单状态机。

## 设计依据

> 实体已存在：`ErpSalQuotation` / `ErpSalQuotationLine`（`module-sales/model/app-erp-sales.orm.xml`）。含 docStatus + approveStatus 三轴状态。

## 状态机

```
DRAFT（草稿创建）
  ├─ 提交审批 → SUBMITTED（已提交）
  │              ├─ 审批通过 → APPROVED（已审核，可发送客户）
  │              │               ├─ 客户确认 → ACCEPTED（客户已接受）
  │              │               │              └─ 转订单 → 生成 ErpSalOrder（走订单状态机）
  │              │               └─ 报价过期 → EXPIRED（有效期到，终态）
  │              └─ 驳回 → REJECTED（修改后重提）
  └─ → CANCELLED（作废）
```

| 状态 | 业务含义 |
|------|---------|
| DRAFT | 销售员创建报价单，编辑中 |
| SUBMITTED | 已提交审核 |
| APPROVED | 审核通过，可发送给客户（含报价单 PDF） |
| ACCEPTED | 客户已书面/电子确认接受报价 |
| EXPIRED | validTo 日期已过，报价自动失效 |
| REJECTED | 审批驳回（可修改后重提） |
| CANCELLED | 作废（不可恢复） |

## 业务规则

1. **报价有效期**：ErpSalQuotation.validTo 到期后系统自动标记 EXPIRED（通过 nop-job 每日扫描）。
2. **客户确认→转订单**：ACCEPTED 后销售员确认转订单，调用 `IErpSalOrderBiz` 创建 ErpSalOrder，报价单行转为订单行。
3. **报价版本**：每次修改创建新版本（`isCurrentVersion` 标记），历史版本保留审计。
4. **报价转 CRM 回链**：若报价单由 CRM 商机转化而来，报价单通过 `relatedBillType/relatedBillCode` 弱指针反查 CRM Lead。
5. **一次报价多次采购**：ACCEPTED 报价单可按客户分批转订单（部分交货场景），报价单状态保持 ACCEPTED 直到全部转完。

## 跨域协作

| 对端 | 协作内容 |
|------|---------|
| CRM（ErpCrmLead） | 报价单可追溯来源商机（弱指针反查） |
| sales/state-machine | ACCEPTED 转 ErpSalOrder → 走订单状态机 |
| master-data（ErpMdPartner） | 客户主数据 |
| manufacturing | 报价单中定制产品需确认交期（通过 APS ATP/CTP） |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-sal.quotation-expiry-check-cron` | 0 0 2 * * * | 每日凌晨检查过期报价单 |
| `erp-sal.quotation-auto-accept-threshold` | 0 | 低于此金额的报价单客户确认后自动转订单（不需人工确认） |
