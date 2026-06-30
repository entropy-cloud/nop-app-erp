# VMI / 寄售 / 受托代销（Consignment）

## 目的

设计库存所有权（ownership）维度建模与所有权转移，支持供应商寄售（VMI）、受托代销、客供料等"物权与库位分离"场景。补齐 `ErpInvStockBalance` 无 `ownerId` 维度的 P1 缺口。

## 边界

- 本模块负责：库存 owner 维度建模（StockBalance/StockLedger 加 ownerId）、所有权转移单（物权变更，物理位置不变）、所有权转移触发应付凭证。
- 本模块不负责：物理库存移动（`state-machine.md` 移动单，物权转移物理位置不变）；供应商主数据（master-data）。
- **owner 是库存正交维度**，与 product/lot/location 并列——`ErpInvStockBalance`/`StockLedger` 加 `ownerId` 是**合理的库存维度扩展**（owner 是库存正交轴），**不属于"污染核心实体"**（见计划 Design Rationale 范式 A 澄清，对照 l10n 凭证指针反查）。
- 实体为**建议命名，待 ORM 计划落地**（`model/app-erp-inventory.orm.xml` 是 ask-first 保护区域，本文件不复述 schema）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.3。

### 核心设计点与证据精确化

🟢 Odoo 源码回查（`stock_quant`/`res_config_settings`/`stock_rule`）：

1. **owner 作为库存维度独立轴**——🟢 `stock_quant.py:74` `owner_id` 字段，`:155` 唯一键含 owner。
2. **owner 是 feature group 开关**（默认关）——🟢 `res_config_settings.py:21` Consignment 是 feature group，避免非 VMI 用户被字段干扰。
3. **owner 不参与 stock.rule**——🟢 `grep stock_rule.py owner_id` 空命中。

**关键结论（诚实标注）**：🟢 **Odoo 无 VMI 自动结算闭环**，owner 只做库存维度隔离，结算靠 EDI/发票层人工处理。本项目"所有权转移自动触发 AP_INVOICE"是**自建能力，非开源借鉴**，仅借鉴 owner 维度建模。文档不得声称"借鉴 Odoo VMI 自动结算"。

## 实体清单

> 表前缀 `erp_inv_`、类名 `ErpInv*`、字典 `erp-inv/*`。以下为建议命名，待 ORM 计划落地。

### 现有实体变更（加 owner 维度）

| 实体 | 现状 | 建议变更 |
|---|---|---|
| `ErpInvStockBalance`（orm.xml:229） | 按 物料×仓库×库位×批次 汇总 | 加 `ownerId`（往来单位，→ErpMdPartner）+ `ownershipType` |
| `ErpInvStockLedger`（orm.xml:184） | 不可变流水 | 加 `ownerId` + `ownershipType`（流水继承移动单的 owner） |

`ownershipType` dict `erp-inv/ownership-type`：

| 值 | 含义 |
|---|---|
| OWNED | 自有库存（默认） |
| VMI_SUPPLIER | 供应商寄售（货在己方仓，所有权属供应商，消耗时才转移） |
| CONSIGNMENT_OUT | 寄售出去（货在客户/代销方仓，所有权属己方） |
| CUSTOMER_PROVIDED | 客供料（客户提供，用于为其加工） |

### ErpInvOwnershipTransfer（所有权转移单，表 `erp_inv_ownership_transfer`）

**关键设计决策：物理移动 vs 所有权转移单据分离**——所有权转移是法权变更，物理位置可能不变，须专用单据，不可复用普通移动单。

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| transferType | dict `erp-inv/ownership-transfer-type`：VMI_CONSUME（VMI 消耗，供应商→自有）/CONSIGNMENT_RETURN（寄售回收）/OWNERSHIP_TO_CUSTOMER（所有权转客户） |
| partnerId | 所有权对方（供应商/客户，→ErpMdPartner） |
| sourceLocId/destLocId | **源/目的库位相同**（物理位置不变，仅法权变更，记录留痕） |
| fromOwnershipType/toOwnershipType | 转移前后所有权类型 |
| businessDate | 转移日期 |
| docStatus | dict `erp-inv/ownership-transfer-status`：DRAFT/CONFIRMED/DONE/CANCELLED |
| posted/postedBy/postedAt | 业财三件套 |
| 标准审计字段 | |

**状态机**：`DRAFT → CONFIRMED → DONE`（触发所有权变更 + 过账）；`CONFIRMED → CANCELLED`。DONE 时更新 StockBalance 的 ownershipType/ownerId（同库位内调账），不复用移动单的物理移动流程。

#### ErpInvOwnershipTransferLine（转移明细，表 `erp_inv_ownership_transfer_line`）

| 字段 | 含义 |
|---|---|
| id/transferId/lineNo | 标准 |
| materialId/batchId | 物料/批次 |
| quantity | 数量 |
| sourceBillType/sourceBillCode | 来源（可选，关联消耗的销售出库等） |
| 标准审计字段 | |

## 业财过账（businessType）

复用 `IErpFinAcctDocProvider`（见 `posting.md`），新增：

| businessType | 触发 | 借贷方向（典型 VMI 消耗） |
|---|---|---|
| OWNERSHIP_TRANSFER | 所有权转移单 DONE | 借：存货(自有) / 贷：应付-供应商（生成 AP，待采购发票核销） |

> VMI 消耗（VMI_SUPPLIER → OWNED）产生应付，是本项目**自建的自动结算能力**（Odoo 不做此闭环）。生成的应付通过 `ErpFinReconciliation` 与后续采购发票核销。

## 业务规则

1. **物理移动 vs 所有权转移严格分离**：`ErpInvOwnershipTransfer` 的 `sourceLocId=destLocId`（物理位置不变），不可复用普通移动单（普通移动单 `sourceLocId≠destLocId`，改变物理位置）。
2. **出库按 owner 匹配**：销售出库时，自有库存（OWNED）优先消耗，VMI 库存（VMI_SUPPLIER）不直接发给普通订单（防止把供应商寄售库存发给普通订单，🟢 `stock_quant.py:144-145` 范式）。
3. **owner 不塞进物料主数据**：同物料可能部分 OWNED 部分 VMI 并存，owner 必须挂在库存余额/流水层，不在 `ErpMdMaterial` 加 ownerId。
4. **所有权转移 → AP 联动**：VMI 消耗转移 DONE 时，自动生成应付（AP_INVOICE 或 AP 凭证），待供应商开票后核销。
5. **owner 维度可选开启**：`erp-inv.ownership-tracking-enabled=false`（默认关）时，所有库存按 OWNED 处理，ownerId 留空。

## 跨域协作

| 对端 | 协作内容 |
|---|---|
| master-data（ErpMdPartner） | owner 关联供应商/客户 |
| finance/posting | 所有权转移过账 OWNERSHIP_TRANSFER 生成应付 |
| finance/reconciliation | VMI 消耗生成的应付与采购发票核销 |
| sales | 销售出库按 ownershipType 匹配库存 |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-inv.ownership-tracking-enabled` | false | 是否启用 owner 维度（对应 🟢 Odoo feature group，默认关，非 VMI 用户无感知） |
| `erp-inv.vmi-auto-generate-ap` | true | VMI 消耗转移 DONE 时是否自动生成应付 |

## 反模式警示

- ⛔ **声称"借鉴 Odoo VMI 自动结算"**——🟢 源码确认 Odoo 无结算闭环（owner 不参与 stock.rule），本项目自动触发 AP 是自建能力，仅借鉴 owner 维度建模。
- ⛔ **所有权转移复用普通移动单**（sourceLocId≠destLocId）——物权变更物理位置可能不变，必须用专用转移单（sourceLocId=destLocId）。
- ⛔ **owner 塞进物料主数据**——同物料可能 OWNED/VMI 并存，owner 必须挂库存层。

## 菜单归属

inventory 域「所有权管理」分组：所有权转移单（owner 维度在库存查询页按 ownerId/ownershipType 筛选）。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| owner 作为库存维度 | 🟢 | Odoo `stock_quant.py:74,155` 源码实测 |
| owner 是 feature group 开关 | 🟢 | Odoo `res_config_settings.py:21` 源码实测 |
| Odoo 无 VMI 自动结算闭环 | 🟢 | `grep stock_rule.py owner_id` 空命中，源码实测 |
| 出库按 owner 匹配 | 🟢 | Odoo `stock_quant.py:144-145` 源码实测 |
| 所有权转移自动触发 AP | ⚪ | 本项目自建（Odoo 不做），领域常识 |
| 本项目 ErpInvStockBalance/StockLedger | 🟢 | `module-inventory/...orm.xml:229,184` 实测（当前无 ownerId） |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §2.3（设计依据）
- `docs/design/inventory/state-machine.md`（三层模型：移动单/流水/余额）
- `docs/design/finance/posting.md`（IErpFinAcctDocProvider 过账机制）
- `docs/design/l10n/cn-golden-tax.md:109`（凭证指针反查范式，核心实体零污染对照）
