# 主数据域数值断言期望值派生（plan 2026-07-09-1145-1）

> 权威源：`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_*.csv`（1234-1 固化种子，唯一真相）、
> `ErpMdDashboardBizModel`（KPI/预警聚合口径）、`ErpMdReportBizModel`（报表数据集构造）、
> `material-price-list.xpt.xml` / `partner-list.xpt.xml`（NumberFormat `#,##0.00`）。
> E2E helper `tests/e2e/{dashboards,reports}/_helper.ts` 对 HTML 做 `,`→`` 千分位剥离后匹配。

## 1. 看板 KPI（`ErpMdDashboard__getDashboardKpi`，零参，全表内存聚合）

口径（`ErpMdDashboardBizModel.java:43-72`）：materialCount=Σ material；customerCount=filter partnerType=="CUSTOMER"；vendorCount=filter partnerType==`PARTNER_TYPE_VENDOR`（**本计划修复后="SUPPLIER"**）；inactive*=filter status=="INACTIVE"。

| 字段 | 期望值 | 派生公式 | seed 行依据 |
|------|--------|----------|-------------|
| materialCount | **4** | `erp_md_material` 全表 count | MAT-001/002/003/004（4 行） |
| customerCount | **2** | filter partnerType=="CUSTOMER" | erp_md_partner 行 1,2（CUST-001/CUST-002） |
| vendorCount | **2**（修复后） | filter partnerType=="SUPPLIER" | erp_md_partner 行 3,4（SUP-001/SUP-002） |
| inactiveMaterialCount | **0** | filter status=="INACTIVE" | 4 material 全 ACTIVE |
| inactivePartnerCount | **0** | filter status=="INACTIVE" | 5 partner 全 ACTIVE |

> 修复前 vendorCount=0（常量误用 "VENDOR"，字典 `erp-md/partner-type` 无此值，自定义值 CUSTOMER/SUPPLIER/BOTH/EMPLOYEE）。修复改常量为 "SUPPLIER" 对齐字典。

## 2. 预警（零参，全表内存聚合）

| 预警 | 期望行数 | 派生 | seed 行依据 |
|------|----------|------|-------------|
| findMaterialWithoutSkuAlert | **0** | material 无关联 SKU 触发 | 4 material（id 1-4）在 erp_md_material_sku 均有 materialId 1-4 关联，全覆盖 |
| findSkuWithoutPriceAlert | **0** | 四价（purchase/sale/wholesale/retail）全 ≤0 触发 | 4 SKU purchasePrice 均 >0（120/300/8.50/5.00），每行至少一档价 |

## 3. 物料价格清单报表（`ErpMdReport__renderHtml` reportName=`material-price-list`，零参）

数据集 `buildMaterialPriceListDataset(null)`：material 按 code ASC（MAT-001..004）× 默认 SKU（isDefault=true）。
- 数值单元格 styleId="num" NumberFormat `#,##0.00`；文本单元格 styleId="cell" General。
- `wholesalePrice` 列：CSV 无 WHOLESALE_PRICE 列 → entity 值 null → `nz(null)`=BigDecimal.ZERO → 渲染 "0.00"（**Non-Goal 不断言 wholesale token**）。
- `materialType` 经 `orm_propValueByName("materialType")` 取原始值 = dict code（FINISHED_PRODUCT/RAW_MATERIAL/PACKAGING），非 label。

| material (code ASC) | materialType | status | skuCode | purchase | sale | retail |
|---------------------|--------------|--------|---------|----------|------|--------|
| MAT-001 | FINISHED_PRODUCT | ACTIVE | MAT-001-PCS | 120.00 | 200.00 | 280.00 |
| MAT-002 | FINISHED_PRODUCT | ACTIVE | MAT-002-PCS | 300.00 | 500.00 | 680.00 |
| MAT-003 | RAW_MATERIAL | ACTIVE | MAT-003-KG | 8.50 | 0.00(null) | 0.00(null) |
| MAT-004 | PACKAGING | ACTIVE | MAT-004-BOX | 5.00 | 8.00 | 0.00(null) |

断言 token（全 < 1000 无千分位；helper 已剥离 `,`）：`物料价格清单`、`MAT-001`、`MAT-002`、`FINISHED_PRODUCT`、`RAW_MATERIAL`、`120.00`、`200.00`、`280.00`、`300.00`、`500.00`、`680.00`、`8.50`。

## 4. 往来单位清单报表（`ErpMdReport__renderHtml` reportName=`partner-list`，零参）

数据集 `buildPartnerListDataset(null)`：partner 按 code ASC（CUST-001, CUST-002, EMP-PTN-001, SUP-001, SUP-002）。
- `partnerType` 经 `orm_propValueByName("partnerType")` = dict code（CUSTOMER/SUPPLIER/EMPLOYEE），非 label。
- `creditLimit`/`creditPeriodDays` 数值 num `#,##0.00`：500000→"500,000.00"、300000→"300,000.00"、0→"0.00"。helper 剥离 `,` 后匹配 → 期望 token 写无逗号形式。

| partner (code ASC) | partnerType | status | creditLimit | creditPeriodDays |
|--------------------|-------------|--------|-------------|------------------|
| CUST-001 | CUSTOMER | ACTIVE | 500,000.00 | 30.00 |
| CUST-002 | CUSTOMER | ACTIVE | 300,000.00 | 45.00 |
| EMP-PTN-001 | EMPLOYEE | ACTIVE | 0.00 | 0.00 |
| SUP-001 | SUPPLIER | ACTIVE | 0.00 | 60.00 |
| SUP-002 | SUPPLIER | ACTIVE | 0.00 | 30.00 |

断言 token（`,` 剥离）：`往来单位清单`、`CUST-001`、`CUST-002`、`SUP-001`、`SUP-002`、`CUSTOMER`、`SUPPLIER`、`500000.00`、`300000.00`、`ACTIVE`。

## 5. 日期漂移防护

master-data KPI/预警/报表均零参全表聚合，确定性来自 seed 行本身（无日期参数、无 period 依赖），镜像 1045-2 CRM/HR 零参范式。
