# 业务交易单据种子（P2P+O2C）— 表清单、列映射、加载拓扑序与范围裁决

> Owner: `docs/plans/2026-07-08-1445-1-p2p-o2c-transaction-seed-data.md` Phase 1 Exit Criteria
> 权威源: `module-{purchase,sales,finance}/model/app-erp-*.orm.xml`（逐表逐列核实，非采信旧记忆）
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（21 张主数据 CSV 已 seed）

## 0. 约定（与 1234-1 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（1234-1 经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`（与 1234-1 `IS_FUNCTIONAL=true` 等一致）。
- 日期列值 `YYYY-MM-DD`；datetime 列本批不 seed（postedAt/approvedAt 等非 mandatory 审计列一律省略，由应用层或后续补充）。
- **关键陷阱（采购 UoM 列名）**：采购三张行表（order_line/receive_line/invoice_line）计量单位列 `code="UO_M_ID"`（驼峰 prop `uoMId`），而销售三张行表（order_line/delivery_line/invoice_line）为 `code="UOM_ID"`（prop `uoMId`）。CSV 必须按各自 `code` 区分，列名错配会在启动期抛 NopException。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

跨域依赖序（seed 设计依赖序，确保 FK 上游先于下游）：

```
会计期间(accounting_period) → 期间状态(accounting_period_status)
  → [P2P 源单据] pur_order → pur_order_line → pur_receive → pur_receive_line
    → pur_invoice → pur_invoice_line → pur_payment → pur_payment_line
  → [O2C 源单据] sal_order → sal_order_line → sal_delivery → sal_delivery_line
    → sal_invoice → sal_invoice_line → sal_receipt → sal_receipt_line
  → [下游财务产物] fin_voucher → fin_voucher_line → fin_voucher_bill_r
    → fin_ar_ap_item → fin_gl_balance
```

> 期间必须最先：凭证（PERIOD_ID）与辅助账（PERIOD_ID）均引用期间；期间状态引用期间+账套。
> 凭证必须在凭证行/回链之前；辅助账、GL 余额依赖期间与科目（科目在 1234-1 主数据已 seed）。
> 核销单（reconciliation）按 §3 Decision (B) 移出本批范围。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 期间与期间状态

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_fin_accounting_period | ID; CODE(M); NAME(M); ORG_ID(FK org=2); YEAR(M); MONTH(M); START_DATE(M); END_DATE(M); QUARTER(opt); IS_ADJUSTMENT(opt=false); STATUS(M=OPEN) | 1 |
| erp_fin_accounting_period_status | ID; PERIOD_ID(FK period=1,M); ACCT_SCHEMA_ID(FK=1,M); TOTAL_VOUCHERS(opt); POSTED_VOUCHERS(opt); UNPOSTED_VOUCHERS(opt); AR_STATUS(M); AP_STATUS(M); INV_STATUS(M); GL_STATUS(M); ASSET_STATUS(M) | 1 |

### 2.2 P2P 源单据（采购）

| 表 | mandatory 业务列 | FK 列（引用上游 ID） | seed 行 |
|----|-----------------|---------------------|--------|
| erp_pur_order | CODE,SUPPLIER_ID,BUSINESS_DATE,CURRENCY_ID,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, SUPPLIER_ID→3, WAREHOUSE_ID→2, CURRENCY_ID→1, SETTLEMENT_METHOD_ID→2 | 1 |
| erp_pur_order_line | ORDER_ID,LINE_NO,MATERIAL_ID,UO_M_ID,QUANTITY,UNIT_PRICE,AMOUNT | ORDER_ID→1, MATERIAL_ID→3, SKU_ID→3, UO_M_ID→2, TAX_RATE_ID→1, WAREHOUSE_ID→2 | 1 |
| erp_pur_receive | CODE,SUPPLIER_ID,WAREHOUSE_ID,BUSINESS_DATE,CURRENCY_ID,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, ORDER_ID→1, SUPPLIER_ID→3, WAREHOUSE_ID→2, CURRENCY_ID→1 | 1 |
| erp_pur_receive_line | RECEIVE_ID,LINE_NO,MATERIAL_ID,UO_M_ID,QUANTITY | RECEIVE_ID→1, ORDER_LINE_ID→1, MATERIAL_ID→3, SKU_ID→3, UO_M_ID→2, WAREHOUSE_ID→2 | 1 |
| erp_pur_invoice | CODE,SUPPLIER_ID,BUSINESS_DATE,CURRENCY_ID,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, SUPPLIER_ID→3, CURRENCY_ID→1 | 1 |
| erp_pur_invoice_line | INVOICE_ID,LINE_NO,MATERIAL_ID,UO_M_ID,QUANTITY | INVOICE_ID→1, RECEIVE_LINE_ID→1, MATERIAL_ID→3, UO_M_ID→2 | 1 |
| erp_pur_payment | CODE,SUPPLIER_ID,BUSINESS_DATE,CURRENCY_ID,AMOUNT_SOURCE,AMOUNT_FUNCTIONAL,TOTAL_AMOUNT,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, SUPPLIER_ID→3, CURRENCY_ID→1, SETTLEMENT_METHOD_ID→2, PARTNER_BANK_ACCOUNT_ID→2 | 1 |
| erp_pur_payment_line | PAYMENT_ID,INVOICE_ID,AMOUNT | PAYMENT_ID→1, INVOICE_ID→1 | 1 |

### 2.3 O2C 源单据（销售）

| 表 | mandatory 业务列 | FK 列（引用上游 ID） | seed 行 |
|----|-----------------|---------------------|--------|
| erp_sal_order | CODE,CUSTOMER_ID,BUSINESS_DATE,CURRENCY_ID,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, CUSTOMER_ID→1, WAREHOUSE_ID→1, CURRENCY_ID→1, SETTLEMENT_METHOD_ID→2 | 1 |
| erp_sal_order_line | ORDER_ID,LINE_NO,MATERIAL_ID,UOM_ID,QUANTITY,UNIT_PRICE,AMOUNT | ORDER_ID→1, MATERIAL_ID→1, SKU_ID→1, UOM_ID→1, TAX_RATE_ID→1, WAREHOUSE_ID→1 | 1 |
| erp_sal_delivery | CODE,CUSTOMER_ID,WAREHOUSE_ID,BUSINESS_DATE,CURRENCY_ID,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, ORDER_ID→1, CUSTOMER_ID→1, WAREHOUSE_ID→1, CURRENCY_ID→1 | 1 |
| erp_sal_delivery_line | DELIVERY_ID,LINE_NO,MATERIAL_ID,UOM_ID,QUANTITY | DELIVERY_ID→1, ORDER_LINE_ID→1, MATERIAL_ID→1, SKU_ID→1, UOM_ID→1, WAREHOUSE_ID→1 | 1 |
| erp_sal_invoice | CODE,CUSTOMER_ID,BUSINESS_DATE,CURRENCY_ID,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, CUSTOMER_ID→1, CURRENCY_ID→1 | 1 |
| erp_sal_invoice_line | INVOICE_ID,LINE_NO,MATERIAL_ID,UOM_ID,QUANTITY | INVOICE_ID→1, DELIVERY_LINE_ID→1, MATERIAL_ID→1, UOM_ID→1 | 1 |
| erp_sal_receipt | CODE,CUSTOMER_ID,BUSINESS_DATE,CURRENCY_ID,AMOUNT_SOURCE,AMOUNT_FUNCTIONAL,TOTAL_AMOUNT,DOC_STATUS,APPROVE_STATUS | ORG_ID→2, CUSTOMER_ID→1, CURRENCY_ID→1, SETTLEMENT_METHOD_ID→2, PARTNER_BANK_ACCOUNT_ID→1 | 1 |
| erp_sal_receipt_line | RECEIPT_ID,INVOICE_ID,AMOUNT | RECEIPT_ID→1, INVOICE_ID→1 | 1 |

### 2.4 下游财务产物

| 表 | mandatory 业务列 | FK 列 | seed 行 |
|----|-----------------|-------|--------|
| erp_fin_voucher | CODE,VOUCHER_TYPE,VOUCHER_DATE,ORG_ID,ACCT_SCHEMA_ID,PERIOD_ID,DOC_STATUS | ORG_ID→2, ACCT_SCHEMA_ID→1, PERIOD_ID→1 | 4 |
| erp_fin_voucher_line | VOUCHER_ID,LINE_NO,SUBJECT_ID,SUBJECT_CODE,DC_DIRECTION,CURRENCY_ID,ACCT_SCHEMA_ID | VOUCHER_ID→1..4, SUBJECT_ID→科目, CURRENCY_ID→1, ACCT_SCHEMA_ID→1, ORG_ID→2, PARTNER_ID→往来, BUSINESS_TYPE(语义) | 8 |
| erp_fin_voucher_bill_r | VOUCHER_ID,BILL_TYPE,BILL_CODE | VOUCHER_ID→1..4 | 4 |
| erp_fin_ar_ap_item | CODE,ORG_ID,ACCT_SCHEMA_ID,DIRECTION,PARTNER_ID,SOURCE_BILL_TYPE,SOURCE_BILL_CODE,BUSINESS_DATE,CURRENCY_ID,AMOUNT_SOURCE,AMOUNT_FUNCTIONAL,OPEN_AMOUNT_SOURCE,OPEN_AMOUNT_FUNCTIONAL,STATUS | ORG_ID→2, ACCT_SCHEMA_ID→1, PARTNER_ID→往来, CURRENCY_ID→1, PERIOD_ID→1 | 4 |
| erp_fin_gl_balance | ORG_ID,ACCT_SCHEMA_ID,PERIOD_ID,SUBJECT_ID,CURRENCY_ID | ORG_ID→2, ACCT_SCHEMA_ID→1, PERIOD_ID→1, SUBJECT_ID→科目, CURRENCY_ID→1 | 5 |

**字典码值（已核实 dict.yaml）**：
- `erp-pur/doc-status`：DRAFT/ACTIVE/CANCELLED → 本批用 `ACTIVE`
- `erp-sal/doc-status`：DRAFT/ACTIVE/CANCELLED → 本批用 `ACTIVE`
- `wf/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → 本批用 `APPROVED`
- `erp-pur/receive-status`：UNRECEIVED/PARTIAL/RECEIVED → PO 用 `RECEIVED`，receive 头用 `RECEIVED`
- `erp-pur/paid-status`：UNPAID/PARTIAL/PAID → PO/Invoice 用 `PAID`，Payment `WRITTEN_OFF_STATUS` 复用此字典 → `PAID`
- `erp-sal/delivery-status`：UNDELIVERED/PARTIAL/DELIVERED → SO 用 `DELIVERED`，delivery 头用 `DELIVERED`
- `erp-sal/received-status`：UNRECEIVED/PARTIAL/RECEIVED → SO/Invoice 用 `RECEIVED`，Receipt `WRITTEN_OFF_STATUS` 复用此字典 → `RECEIVED`
- `erp-fin/voucher-type`：RECEIPT/PAYMENT/TRANSFER → 本批凭证均用 `TRANSFER`（转账凭证）
- `erp-fin/voucher-status`：DRAFT/POSTED/CANCELLED → 用 `POSTED`
- `erp-fin/dc-direction`：DEBIT/CREDIT
- `erp-fin/business-type`：AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT（+ PURCHASE_INPUT/SALES_OUTPUT）
- `erp-fin/ar-ap-direction`：RECEIVABLE/PAYABLE（**无 `DIRECTION_` 前缀**，经 dict 核实）
- `erp-fin/ar-ap-status`：OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF → 本批用 `SETTLED`
- `erp-fin/period-status`：OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL → 用 `OPEN`
- `erp-fin/module-close-status`：OPEN/CLOSING/CLOSED → 全 `OPEN`

## 3. 范围 Decision（Phase 1 item 2）

**选择**：首批 P2P+O2C 各 1 条端到端最小连通链（头 1 行 + 行 1 行），源单据全链 + 对应**已过账财务产物**（凭证/凭证行/回链/AR-AP 辅助账/GL 余额/期间 OPEN）。

**记录设计**（引用 1234-1 已 seed 主数据固定 ID）：
- 通用：orgId=2(ERP-CO)，acctSchemaId=1(ACCT-FIN-01)，currencyId=1(CNY)，exchangeRate=1，periodId=1(2026-07)。
- **P2P 链**：供应商=3(北方钢铁)，仓库=2(原料仓)，物料=3(原料 X 钢材)/sku=3/UOM=2(KG)/taxRateId=1(VAT-13)；数量 100，单价 8.50 → 金额 850、税额 110.50、价税合计 960.50。单据码：PO `PO-2026-001`、receive `PRCV-2026-001`、invoice `PINV-2026-001`、payment `PAY-2026-001`。业务日期 07-01→07-03→07-05→07-10。
- **O2C 链**：客户=1(华东科技)，仓库=1(主仓库)，物料=1(产品甲)/sku=1/UOM=1(PCS)/taxRateId=1(VAT-13)；数量 5，单价 200 → 金额 1000、税额 130、价税合计 1130。单据码：SO `SO-2026-001`、delivery `SDLV-2026-001`、invoice `SINV-2026-001`、receipt `REC-2026-001`。业务日期 07-02→07-04→07-06→07-09。

**过账产物凭证（4 张，均借贷平衡，voucherType=TRANSFER，docStatus=POSTED）**：
> **已知简化（master-data gap）**：1234-1 seed 的 `erp_md_subject` 仅 8 个 GL 科目（库存现金/银行存款/应收账款/库存商品/应付账款/主营业务收入/主营业务成本/销售费用），**未含进项税/销项税/应交税费科目**。本计划 Non-Goal 不扩展主数据。故凭证将税额并入相邻科目（AP 发票税额并入存货借方；AR 发票税额并入收入贷方），保证「凭证合计 = 发票价税合计」金额自洽且借贷平衡。精确税金科目分拆是主数据扩展 successor（触发：seed 进/销项税科目后）。

| 凭证 | businessType | 借方 | 贷方 | 金额 |
|------|-------------|------|------|------|
| 1 AP 发票过账 | AP_INVOICE | 库存商品(1405) | 应付账款(2202) | 960.50 |
| 2 付款过账 | PAYMENT | 应付账款(2202) | 银行存款(1002) | 960.50 |
| 3 AR 发票过账 | AR_INVOICE | 应收账款(1122) | 主营业务收入(5001) | 1130 |
| 4 收款过账 | RECEIPT | 银行存款(1002) | 应收账款(1122) | 1130 |

**posted 一致性裁决**：`posted=true` **当且仅当**该源单据有对应凭证（经 `voucher_bill_r` 串联）。故：
- PO/SO（订单不直接过账 GL）→ `posted=false`
- 采购入库/销售出库（其过账产物是库存移动，属 inventory 域，**Non-Goal 未 seed 库存表**）→ `posted=false`（业务完成但库存过账产物不在本批 seed 范围）
- 采购发票/付款/销售发票/收款 → `posted=true`（有凭证）

**核销单 Decision (B)**：本批**不**纳入 `erp_fin_reconciliation`(+line) 核销单文档。理由：(1) 核销单行的 `paymentItemId`/`invoiceItemId` 须引用 `erp_fin_ar_ap_item` 的成对 ID，参照完整性设计复杂度高；(2) 端到端「已付清/已收清」态已由源单据层的 `payment_line`/`receipt_line`（付款↔发票多对多）+ `ar_ap_item` 的 `settled/openAmount/status=SETTLED` 充分表达；(3) `ar_ap_item` 的 settled/open 字段是直接数据（领域事实），无须核销单文档即可表达已结算态。核销单文档归后续批次（触发：核销单 CRUD/数值端到端回归需求）。

**替代方案分析**：
- (a) 仅 seed 源单据不 seed 下游财务产物：**rejected**——过账 action 驱动，CSV 不触发凭证生成，看板/报表财务数值仍为空（未解除阻塞，与计划核心目标冲突）。
- (b) seed 全部 18 域交易单据：**rejected**——复杂度爆炸，按域逐批是 1234-1 Deferred 既定策略。
- 核销 Decision (A)（首批纳入核销单使 openAmount 归零）：**rejected**——见上 (B) 理由，复杂度收益不匹配。

**残留风险与防护**：
- 参照完整性遗漏（FK 列引用未 seed 的上游 ID）→ 启动期 DataInitInitializer 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。
- 列名错配（尤其采购 `UO_M_ID` vs 销售 `UOM_ID`）→ 同上启动期暴露。
- 非幂等（1234-1 已确认）→ fresh-DB 重置（删 `db/erp.mv.db`）是必需前置，playwright webServer 已内置。

## 4. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**（无 CSV 不便表达的初始化需求）。期间状态经 INSERT（accounting_period_status 为新行），无序列重置需求，无批量 UPDATE 需求。故不补 `NN-init-transaction-*.sql`。

## 5. seed 行数汇总

| 域 | 表数 | 行数 |
|----|------|------|
| finance（期间+产物） | 7（accounting_period, accounting_period_status, voucher, voucher_line, voucher_bill_r, ar_ap_item, gl_balance） | 1+1+4+8+4+4+5 = 27 |
| purchase（源单据） | 8（order/receive/invoice/payment × 头+line） | 8 |
| sales（源单据） | 8（order/delivery/invoice/receipt × 头+line） | 8 |
| **合计** | **23 张交易表 CSV** | **43 行** |

> 23 张新交易表 CSV 加入 `_vfs/_init-data/`，与 1234-1 的 21 张主数据 CSV 共存（总计 44 张 CSV）。
