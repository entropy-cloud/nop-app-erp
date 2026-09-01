# sales 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（测试共享夹具见 `app-erp-test-data`，边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.1a（plan `docs/plans/2026-09-01-0838-1-m11a-md-sal-seed-expansion.md`，2026-09-01）。此前 8 张 sales 表 seed（order/order_line/delivery/delivery_line/invoice/invoice_line/receipt/receipt_line，P2P+O2C 端到端链）见 `docs/architecture/seed-data.md` 历史批次段。

## 种子数据范围（M1.1a 批次 8 表）

sales 域 16 实体中 8 表已由历史批次 seed；本批补齐其余 8 表（报价/合同/退货/价格 4 组，含 3 组主子表），达成**域内全量 seed 覆盖（16/16）**。

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpSalQuotation（销售报价单头） | `erp_sal_quotation.csv` | 3 | P+N-TERM | CUSTOMER_ID→ErpMdPartner〔跨域:md·已seed〕、CURRENCY_ID→ErpMdCurrency〔跨域:md·已seed〕 |
| ErpSalQuotationLine（报价单行） | `erp_sal_quotation_line.csv` | 3（1/头） | P | QUOTATION_ID→ErpSalQuotation〔本批〕、MATERIAL_ID→ErpMdMaterial〔跨域:md·已seed〕、UOM_ID→ErpMdUoM〔跨域:md·已seed〕 |
| ErpSalContract（销售合同） | `erp_sal_contract.csv` | 3 | P+N-TERM | CUSTOMER_ID→ErpMdPartner〔跨域:md·已seed〕、CURRENCY_ID→ErpMdCurrency〔跨域:md·已seed〕 |
| ErpSalPriceList（销售价格清单头） | `erp_sal_price_list.csv` | 2 | P+N-DIS | —（无必填 FK；CURRENCY_ID/PARTNER_ID 可选指向〔已seed〕） |
| ErpSalPriceListLine（价格清单行） | `erp_sal_price_list_line.csv` | 2（1/头） | P | PRICE_LIST_ID→ErpSalPriceList〔本批〕 |
| ErpSalPricingRule（销售促销规则） | `erp_sal_pricing_rule.csv` | 2 | P+N-DIS | —（无必填 FK；MATERIAL_CATEGORY_ID 可选指向〔已seed〕） |
| ErpSalReturn（销售退货单头） | `erp_sal_return.csv` | 3 | P+N-TERM | CUSTOMER_ID→ErpMdPartner〔跨域:md·已seed〕、WAREHOUSE_ID→ErpMdWarehouse〔跨域:md·已seed〕、CURRENCY_ID→ErpMdCurrency〔跨域:md·已seed〕 |
| ErpSalReturnLine（退货单行） | `erp_sal_return_line.csv` | 3（1/头） | P | RETURN_ID→ErpSalReturn〔本批〕、MATERIAL_ID→ErpMdMaterial〔跨域:md·已seed〕、UOM_ID→ErpMdUoM〔跨域:md·已seed〕 |

## FK 闭环图

```
[跨域:md·已seed] erp_md_partner(1..2 客户) ──CUSTOMER_ID──▶ erp_sal_quotation / contract / return
[跨域:md·已seed] erp_md_currency(1 CNY)    ──CURRENCY_ID───▶ erp_sal_quotation / contract / return / price_list
[跨域:md·已seed] erp_md_warehouse(1..2)    ──WAREHOUSE_ID──▶ erp_sal_return
[跨域:md·已seed] erp_md_material(1..4)     ──MATERIAL_ID───▶ erp_sal_quotation_line / return_line / price_list_line
[跨域:md·已seed] erp_md_uom(1..4)          ──UOM_ID────────▶ erp_sal_quotation_line / return_line / price_list_line
[本批] erp_sal_quotation(1..3) ──QUOTATION_ID──▶ erp_sal_quotation_line
[本批] erp_sal_price_list(1..2) ─PRICE_LIST_ID─▶ erp_sal_price_list_line
[本批] erp_sal_return(1..3) ────RETURN_ID─────▶ erp_sal_return_line
[已seed] erp_sal_delivery(1) ─DELIVERY_ID（可选）▶ erp_sal_return
[已seed] erp_sal_delivery_line(1) ─DELIVERY_LINE_ID（可选）▶ erp_sal_return_line
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空（`TestErpSeedDataIntegrity` 引用完整性门禁背书）。

## 用例指示编码与 negative 行语义

编码定义见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」通用约定 5：

- **P（最小正例行）**：全部 8 表均有；单据头金额与行金额自洽（含税 = 不含税 × 1.13，税率 13%）。
- **N-TERM（终态行）**：`erp_sal_quotation` / `erp_sal_contract` / `erp_sal_return` 各第 3 行 `DOC_STATUS=CANCELLED`（`erp/doc-status` 三值 DRAFT/ACTIVE/CANCELLED 的终态，与 `ErpSal*DocumentStateMachine` terminal 集一致，见 `docs/design/sales/state-machine.md`），供单据状态非法迁移守卫负路径（`ERR_*_ILLEGAL_DOC_STATUS_TRANSITION` 族）消费。
- **N-DIS（禁用行）**：`erp_sal_price_list` 第 2 行与 `erp_sal_pricing_rule` 第 2 行 `IS_ACTIVE=false`（两实体携带 isActive 列），供启用前置守卫负路径消费。

## 状态与过账语义

- 正例单据头 `DOC_STATUS=ACTIVE + APPROVE_STATUS=APPROVED`（对齐历史批次既有 sal seed 口径；`docStatus` 生命周期注记见 `docs/design/sales/state-machine.md`——ACTIVE 为 dict 死状态仅存于 seed 静态行，新单据实际生命周期 DRAFT→CANCELLED）。
- `erp_sal_return` 三行 `POSTED=false`：退货过账产物（红字凭证 + 反向辅助账/库存入库）未 seed，与 P2P+O2C 批「posted 当且仅当有对应凭证」裁决一致。
- 报价单 `IS_ACCEPTED`：行 1 true（已接受正例）/ 行 2、3 false。

## 约定对齐

- 列头 = DB 列名大写下划线；省略审计列；ISO 日期；小写布尔；ID < 100000（`zz-sequence-advance.sql` 序列推进值域约束）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议）。
