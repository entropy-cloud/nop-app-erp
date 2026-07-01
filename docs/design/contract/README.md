# 合同全生命周期管理（Contract Lifecycle Management）

## 目的

设计合同从谈判 → 签署 → 执行 → 续期/终止的全生命周期管理。覆盖采购合同、销售合同、劳动合同的统一管理。`purchase/README.md` 的抬头级合同（字段级）是采购单上的合同引用，本文是独立合同管理模块。

## 边界

- 本模块负责：合同模板库、合同版本管理（起草→审批→签署→归档）、合同变更单（Amendment）、开票计划（InvoicePlan）、到期提醒/续期、用量/消耗计费（Consumption）。
- **与 purchase/sales 的边界**：采购合同关联供应商和采购订单，销售合同关联客户和销售订单。合同是**框架协议**层，订单是执行层。
- 本模块不负责：订单级执行（purchase/sales 域）；法律条款的自动审核（属法律专家系统）。
- 实体为**建议命名，待 ORM 计划落地**。

## 设计依据

> 参考 **Axelor contract**（83 Java + 50 XML）：Contract + ContractLine + ContractVersion + InvoicePlan + ConsumptionLine 完整模型。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §合同管理。

## 实体清单

> 表前缀 `erp_pur_contract`（采购合同）/ `erp_sal_contract`（销售合同）或统一 `erp_ct_`。类名 `ErpCt*` / 字典 `erp-ct/*`。以下为统一合同模块建议。

### ErpCtContract（合同头）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/code/orgId | 标准 | 🟢 Axelor Contract |
| contractName | 合同名称 | 🟢 Axelor Contract.name |
| contractType | dict `erp-ct/contract-type`：PURCHASE（采购）/ SALES（销售）/ EMPLOYMENT（劳动）/ SERVICE（服务） | — |
| contractDirection | dict：INBOUND（进，我方付款）/ OUTBOUND（出，客户付款） | — |
| partnerId | 对方（供应商/客户/员工→ErpMdPartner） | 🟢 Axelor Contract.partner |
| currencyId | 币种 | — |
| totalAmount | 合同总额 | — |
| startDate/endDate | 合同有效期 | 🟢 Axelor Contract |
| signDate | 签署日期 | — |
| status | dict `erp-ct/contract-status`：DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED | — |
| templateId | 合同模板（→ErpCtTemplate） | — |
| parentContractId | 父合同（变更单关联原合同） | — |
| description | 描述 | — |
| attachmentId | 合同文件（PDF/scanned） | — |
| 标准审计字段 | | |

**状态机**：`DRAFT（起草） → NEGOTIATION（谈判中） → ACTIVE（执行中） → EXPIRED（到期）`；`ACTIVE → SUSPENDED（中止） → ACTIVE（恢复）`；`ACTIVE/NEGOTIATION → TERMINATED（终止）`；`DRAFT → CANCELLED`。

### ErpCtContractLine（合同行）

| 字段 | 含义 |
|------|------|
| id/contractId/lineNo | 标准 |
| materialId/productId | 物料/产品（可空，框架合同可不指定） |
| description | 行描述 |
| quantity | 数量（框架合同为预估总量） |
| unitPrice | 单价 |
| amount | 金额 |
| deliveryDate | 交货日期（可选） |

### ErpCtContractVersion（合同版本）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/contractId/versionNo | 标准 | 🟢 Axelor ContractVersion |
| versionDate | 版本日期 | — |
| content | 版本内容（条款变更说明） | 🟢 Axelor ContractVersion |
| attachmentId | 版本文件 | — |
| isCurrent | 是否当前版本 | — |
| status | dict：DRAFT/FINALIZED/SIGNED | — |
| approvedBy/approvedAt | 批准人/时间 | — |

### ErpCtInvoicePlan（开票计划）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/contractLineId/orgId | 标准 | 🟢 Axelor InvoicePlan |
| planDate | 计划开票日期 | 🟢 Axelor InvoicePlan |
| amount | 开票金额 | 🟢 Axelor InvoicePlan |
| isInvoiced | 是否已开票 | — |
| invoiceBillCode | 关联发票（→AP/AR Invoice） | — |
| invoiceDate | 实际开票日期 | — |
| invoiceTerm | dict `erp-ct/invoice-term`：ADVANCE（预付款）/ MILESTONE（里程碑）/ MONTHLY（月结）/ COMPLETION（完工） | 🟢 Axelor InvoiceTerm |

### ErpCtConsumptionLine（消耗计费行——用量计费场景）

| 字段 | 含义 | 参考 |
|------|------|------|
| id/contractLineId/orgId | 标准 | 🟢 Axelor ConsumptionLine |
| consumptionDate | 消耗日期 | 🟢 Axelor ConsumptionLine |
| quantity | 消耗数量 | — |
| unitPrice | 单价（合同行价格，可选覆盖） | — |
| amount | 金额（=quantity × unitPrice） | — |
| sourceBillType/sourceBillCode | 来源业务单 | — |

### ErpCtTemplate（合同模板）

| 字段 | 含义 |
|------|------|
| id/code/name | 标准 |
| contractType | 适用合同类型 |
| contentTemplate | 模板内容（占位符 + 条款） |
| isActive | 是否启用 |

## 业务规则

1. **合同版本管理**：每次合同变更（Amendment）创建一个新版本，原版本保留。当前版本 `isCurrent=true`。审计可追溯历史版本。
2. **开票计划驱动**：InvoicePlan 按合同条款生成开票计划（预付30%/里程碑50%/完工20%），自动生成 AP/AR 发票草稿。
3. **用量计费**：ConsumptionLine 记录实际消耗量（如 SaaS 订阅的 API 调用次数/存储空间），周期结束时汇总生成发票。
4. **到期提醒**：endDate 前 30/15/7 天通过 nop-job 发送到期提醒通知。
5. **合同与订单弱关联**：合同与订单通过 `relatedBillType/relatedBillCode` 弱指针关联，订单执行回写合同已执行金额。

## 业财过账

合同本身不产生会计凭证。**开票计划触发的发票**（AP/AR invoice）走 purchase/sales 域的标准过账流程。

## 跨域协作

| 对端 | 协作方式 |
|------|---------|
| purchase（PO） | 采购合同关联采购订单（弱指针），合同已执行金额由 PO 回写 |
| sales（SO） | 销售合同关联销售订单 |
| finance（AP/AR Invoice） | 开票计划触发生成发票（走标准过账） |
| human-resource | 劳动合同关联员工主数据 |

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-ct.reminder-days-before-expiry` | 30 | 合同到期前多少天开始提醒 |
| `erp-ct.auto-create-renewal-draft` | false | 到期时是否自动创建续期草稿 |

## 菜单归属

purchase 域「合同管理」分组：合同列表、合同模板、开票计划、消耗记录。

## 反模式警示

- ⛔ **合同无版本管理**——每次修改直接覆盖原合同文件，丢失审计保留。必须用 ContractVersion。
- ⛔ **开票计划与合同行耦合**——InvoicePlan 是独立实体，支持按时间/里程碑/用量多种模式。
- ⛔ **合同与订单通过外键强耦合**——用弱指针 `relatedBillType/relatedBillCode`。

## 证据强度标注

| 证据 | 强度 | 说明 |
|------|------|------|
| Contract + ContractVersion + InvoicePlan 三层 | 🟢 | Axelor `Contract.xml` + `InvoicePlan.xml` + `ConsumptionLine.xml` |
| 合同版本管理 | 🟢 | Axelor ContractVersion（版本日期/内容/附件/是否当前/批准） |
| 开票计划/条款 | 🟢 | Axelor InvoicePlan（计划日期/金额/是否开票/条款类型） |
| 消耗计费 | 🟢 | Axelor ConsumptionLine（消耗日期/数量/单价/金额/来源单） |

## 参考

- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §合同管理
- `docs/design/purchase/README.md`（采购域）
- `docs/design/sales/README.md`（销售域）
