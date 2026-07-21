# 税务框架

## 目的

定义 nop-app-erp 的可插拔税务引擎接口，支持不同国家/地区的税务规则实现。

## 税务引擎接口

```
IErpTaxEngine
    ├─ calculateTax(billData) → TaxResult
    ├─ validateTaxCode(taxCode) → boolean
    └─ getTaxCategories() → List<TaxCategory>
```

## 中国增值税实现

| 税率 | 适用场景 |
|------|----------|
| 13% | 一般货物销售/采购 |
| 9% | 交通运输、建筑、不动产 |
| 6% | 金融、现代服务 |
| 0% | 出口退税 |
| 免税 | 免税项目 |

## 凭证集成

税务凭证与业务凭证在同一过账事件中生成：

| 业务类型 | 税务分录 |
|----------|----------|
| 采购发票 | 借：进项税额 |
| 销售发票 | 贷：销项税额 |
| 进项税转出 | 借：进项税额转出 |

## 税额计算

```
税额 = 不含税金额 × 税率
含税金额 = 不含税金额 + 税额
```

> 支持价税分离：单据行分别记录金额（不含税）和税额。税率按物料×往来单位组合匹配。

## 物料层跨境税快查（C2 跨境贸易扩展）

> Owner doc：`docs/design/master-data/cross-border-trade.md` §2 / §6。本段是该 owner doc 的回链摘要。

`ErpMdMaterial` 在 `defaultTaxRateId→ErpMdTaxRate` 详细税率配置 FK 之外，**冗余** 两个跨境场景高频查询字段：

| 字段 | 类型 | 业务语义 |
|------|------|----------|
| `vatRate` | DECIMAL(6,4) | 报关场景增值税率快查（如 0.13 表示 13%）|
| `drawbackRate` | DECIMAL(6,4) | 出口退税率快查 |

### 双轨设计（快查 vs 联查）

| 场景 | 使用字段 | 理由 |
|------|---------|------|
| 默认显示 / 一般业务单据 | `defaultTaxRate.rate` 联查值 | 详细税率配置在 `ErpMdTaxRate`，含税种/有效期/零税率/免税等元数据 |
| 报关场景（高频） | `vatRate` / `drawbackRate` 物料层快查 | 报关时海关申报需直接读取物料层税率，避免每行报关都联查 `ErpMdTaxRate` |

### 风险与缓解

- **风险**：字段冗余在物料主表，可能多场景下与 `ErpMdTaxRate` 不一致（如税率变更后物料层未同步）。
- **缓解**：默认显示联查值；`vatRate`/`drawbackRate` 字段仅在**报关场景显式覆盖**（业务约定）。

### 与 ErpMdMaterialCustoms 的关系

`ErpMdMaterialCustoms.dutyAmount` / `vatAmount` 是 **per-transaction 报关记录**上记录的实际关税/增值税金额（由 finance successor 关税计算引擎填充），与本节物料层的税率快查字段（`vatRate`/`drawbackRate`）是**字段层级不同**：前者是单据级金额，后者是主层级税率。Per-transaction 实体覆盖物料层快查——同一物料在不同报关场景可能因协定/政策适用不同税率。

> 关税过账 Provider 接入（IMPORT_DUTY / VAT_REFUND businessType）属 finance successor（触发：业务客户跨境业务量 > 100 单/月 或 财务 owner doc 显式授权），本节仅落地字段。
