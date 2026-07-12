# 2026-07-12-0900-2-transaction-line-warehouse-name-resolution 交易明细行仓库外键名称解析（5 行实体 warehouseName）

> Plan Status: draft
> Last Reviewed: 2026-07-12
> Source: `docs/plans/2026-07-12-0800-2-transaction-line-fk-name-resolution-batch3.md` Deferred「行实体 materialId 以外 FK 名称解析（uomId / warehouseId / skuId / currencyId 等）」（Successor Required: yes，触发条件=低频 FK 显示需求累积时，**本计划覆盖其中最高价值的 warehouseId**——交易明细行的收发货仓库是核心业务实体，用户首要识别字段之一，与已落地的 materialName 并列）；`docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」
> Related: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Phase 3（批次 1：4 头实体机制 D 落地）；`docs/plans/2026-07-12-0600-1-transaction-list-fk-name-resolution-batch2.md`（批次 2：10 头实体 + ErpInvStockMove 双仓库 sourceWarehouseName/destWarehouseName）；`docs/plans/2026-07-12-0800-2-transaction-line-fk-name-resolution-batch3.md`（批次 3：13 行实体 materialName）
> Audit: required

## Current Baseline

外键名称解析经三个前置计划已覆盖**头实体** 14 个（批次 1/2，含 ErpInvStockMove 双仓库 sourceWarehouseName/destWarehouseName）与**明细行实体** 13 个的 `materialName`（批次 3，计划 0800-2），经机制 D 三层落地（xmeta `*Name` 派生 prop + Line BizModel `@BizLoader(forType=...)` 批量加载防 N+1 + view.xml grid `bounded-merge` 列替换原始 `*Id`）。

**剩余缺口**：批次 3 覆盖的 13 行实体中，**5 个含仓库外键的行实体**列表页 grid 仍显示原始数字 `warehouseId`（或 `sourceWarehouseId`/`destWarehouseId`），零层 warehouseName 落地。经实时仓库核实 ORM `_app.orm.xml`（权威源）：

| # | 实体 | 域 | 仓库列 | warehouse to-one 关系（ORM 核实） |
|---|------|----|--------|--------------------------------|
| 1 | ErpPurOrderLine | purchase | `warehouseId`（"收货仓库"） | `warehouse` → ErpMdWarehouse |
| 2 | ErpPurReceiveLine | purchase | `warehouseId`（"入库库位"） | `warehouse` → ErpMdWarehouse |
| 3 | ErpSalOrderLine | sales | `warehouseId`（"发货仓库"） | `warehouse` → ErpMdWarehouse |
| 4 | ErpSalDeliveryLine | sales | `warehouseId`（"出库库位"） | `warehouse` → ErpMdWarehouse |
| 5 | ErpMfgWorkOrderLine | manufacturing | `sourceWarehouseId` + `destWarehouseId` | `sourceWarehouse` + `destWarehouse` → ErpMdWarehouse（双仓库，镜像 ErpInvStockMove 头实体范式） |

**机制 D 可达性**：全部 5 行实体的 ORM 均声明仓库 `<to-one>` 关系（经 `_app.orm.xml` 核实：purchase model、sales model、manufacturing model），故 `getWarehouse().getName()`（及 ErpMfgWorkOrderLine 的 `getSourceWarehouse().getName()` / `getDestWarehouse().getName()`）可达，`orm().batchLoadProps(lines, Collections.singleton("warehouse"))` 批量加载防 N+1 可行。机制 D 与头实体批次 + 行实体 materialName 批次完全同范式。全部 5 行实体均已有定制 `{Entity}LineBizModel.java`（经独立子代理核实，绝对路径见审查记录），且批次 3 已落地 `materialName` 的 `@BizLoader(forType=...)` 方法——本计划在其同方法内追加 warehouseName 加载（同一 `batchLoadProps` 调用可一次批量加载多关系，或追加独立 `batchLoadProps`）。

**行实体列表页可达性**：5 行实体均有独立 `_gen/_<Entity>.view.xml`（含 `<grid id="list">` + `<cols>`）。批次 3 已为这些行实体的定制层 `<Entity>.view.xml` 增 `<cols x:override="bounded-merge">`（含 `materialName` 列）——本计划在其既有 `<cols>` 内追加 `warehouseName` 列（已存在 bounded-merge override，仅追加列，非新增 override）。行实体亦在父单据 drawer 子表中以 grid 渲染——`warehouseName` 派生 prop 经 xmeta 暴露后，drawer 子表经同一 xmeta 也可显示名称（xmeta 为实体级，grid/drawer 共享）。

**范本范式**（批次 1/2/3 已验证）：
- xmeta：`module-purchase/erp-pur-meta/.../ErpPurOrder/ErpPurOrder.xmeta` — `warehouseName` prop（`queryable="false" sortable="false"` + `<schema type="java.lang.String"/>`）
- BizLoader：`ErpPurOrderBizModel.java:107-145` — `@BizLoader(forType=...)` + `orm().batchLoadProps(...)` + 逐行 `getWarehouse().getName()`
- 双仓库命名（头实体先例）：批次 2 ErpInvStockMove 落地 `sourceWarehouseName` / `destWarehouseName`
- view grid：`ErpPurOrder.view.xml:6-20` — `<cols x:override="bounded-merge">` 含 `warehouseName` 列

**定制机制**：定制层 `<Entity>.view.xml` 用 `x:extends="_gen/_<Entity>.view.xml"` + `<cols x:override="bounded-merge">`；项目不用 `_delta/` 目录（同批次 1/2/3）。

## Goals

- 5 交易行实体列表页 grid 列显示 `warehouseName`（仓库名称）而非原始数字 `warehouseId`（ErpMfgWorkOrderLine 显示 `sourceWarehouseName` / `destWarehouseName`），经机制 D 三层落地（xmeta `*Name` 派生 prop + Line BizModel `@BizLoader(forType=...)` 批量加载防 N+1 + view.xml grid `bounded-merge` 列替换）。
- 行实体 `warehouseName` 经 xmeta 实体级暴露后，父单据 drawer 子表同样显示仓库名称（xmeta 共享，无需额外改动）。

## Non-Goals

- **uomId / skuId / currencyId / projectId / costCenterId 等其他行实体 FK 名称解析**——仓库（warehouseName）为行实体 materialName 之后第二高价值 FK（收发货仓库为核心业务实体）。计量单位识别度低且可经物料带出；SKU/币种/项目/成本中心为低频维度。其余 FK 归后续 successor（触发条件：低频 FK 显示需求累积时）。
- **全量 ~60 行实体覆盖**——本计划仅覆盖 13 批次 3 行实体中含仓库外键的 5 个；其余行实体（invoice/return/quotation 等无仓库列的行实体、财务/CRM/HR 等域行实体）归后续 successor。
- **头实体仓库 FK 名称解析**——头实体 14 个（含 ErpInvStockMove 双仓库）已由批次 1/2 覆盖；剩余低频头实体归 codegen 模板层 successor。
- **drawer / edit 表单内仓库选择器改造**——仅 grid 列展示（同批次 1/2/3 Non-Goals）。
- **ErpInvStockMoveLine 仓库名称解析**——经 ORM 核实，StockMoveLine 在行级**无仓库列**（仅 sourceLocationId/destLocationId 库位列，仓库在父头 ErpInvStockMove 上）。库位（location）粒度过细且非仓库语义，按批次 2/3 Non-Goals 排除。

## Task Route

- Type: `app-layer design change`（改用户可见列表页 + drawer 子表数据展示行为，跨 3 域多表面）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（FK 显示 D 机制权威）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`（加 BizLoader）
- Skill Selection Basis: 涉 xmeta 派生字段 + BizModel `@BizLoader` + view.xml grid delta → 匹配 `nop-frontend-dev`（XView 三层 / bounded-merge / grid 定制）+ `nop-backend-dev`（`@BizLoader` / `batchLoadProps` / 跨实体 I*Biz）。Java 测试 → `nop-testing`。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。FK 名称解析经既有 to-one 关系只读聚合（无新外部服务/端口/密钥）。

## Execution Plan

### Phase 1 - 采购+销售交易行实体 warehouseName 解析（4 实体，单仓库）

Status: planned
Targets: ErpPurOrderLine / ErpPurReceiveLine / ErpSalOrderLine / ErpSalDeliveryLine 的 xmeta + Line BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（warehouse to-one 关系经 ORM 已存在；批次 3 已落地 materialName 范式）

- [ ] `Decision | Explore`: 裁决 warehouseName 命名约定与纳入准则——(a) 命名约定：单关系实体沿用 `{relation}Name`（`warehouseName`，对齐头实体批次 1/2 范式）。(b) 纳入准则：仅解析含 `warehouse` to-one 关系且 `getName()` 有业务语义的行实体（5 个：4 单仓库 + 1 双仓库）；ErpInvStockMoveLine 行级无仓库列（仅库位列）按 Non-Goals 排除。(c) 替代方案：同时解析 uomName（否决——uom 识别度低且可经物料带出，先覆盖第二高价值 warehouseName）。(d) Explore：逐实体核实 ORM `_app.orm.xml` 中 `<to-one name="warehouse">` 关系名与 `ErpMdWarehouse.getName()` 可达性（已初步确认 4 单仓库实体均可达，Phase 1 落地前复核）。全部 4 行实体均已有定制 Line BizModel（批次 3 materialName `@BizLoader` 已就绪），无需创建新 BizModel。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 4 行实体 xmeta 各增 `warehouseName` 派生 prop（`queryable="false" sortable="false"` + `<schema type="java.lang.String"/>`），镜像 `ErpPurOrder.xmeta` 范式。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 4 行实体已有定制 Line BizModel 在既有 `materialName` `@BizLoader(forType=...)` 方法内追加 warehouse 批量加载（`orm().batchLoadProps(lines, Collections.singleton("warehouse"))` + 逐行 `getWarehouse().getName()`），镜像 `ErpPurOrderBizModel.java:107-145` 范式。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 4 行实体 view.xml grid 既有 `<cols x:override="bounded-merge">`（批次 3 已建，含 materialName 列）内追加 `warehouseName` 列替换原始 `warehouseId` 列（保留 `id`/`lineNo`/`materialName`/`quantity`/`unitPrice` 等业务列）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 4 行实体 grid 列本地化验证（xmeta `warehouseName` prop 存在 + view.xml grid 含 `warehouseName` 列 + `_gen` 原始 `warehouseId` 列被 bounded-merge 覆盖）+ 涉及模块编译通过 + 代表行实体（purchase + sales 各 ≥1）BizLoader 批量加载测试（在批次 3 既有测试类 `TestErpPurFkNameLoader` / `TestErpSalFkNameLoader` 新增 warehouseName 解析测试方法，经 `IGraphQLEngine` findList 触发 `@BizLoader`，验证 warehouseName 对齐 master-data）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 行实体 xmeta + Line BizModel + view grid 三层 `warehouseName` 落地
- [ ] 代表行实体 BizLoader 批量加载测试全绿（purchase + sales 各 ≥1 行实体，warehouseName 对齐 master-data）
- [ ] 涉及模块编译通过（`mvn clean install -DskipTests` 归 Closure Gates）

### Phase 2 - 制造工单行双仓库名称解析（1 实体，双仓库）

Status: planned
Targets: ErpMfgWorkOrderLine 的 xmeta + Line BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 范式已验证

- [ ] `Decision | Explore`: 裁决双仓库命名——沿用头实体 ErpInvStockMove 批次 2 范式 `sourceWarehouseName` / `destWarehouseName`（双关系保留语义区分）。核实 ORM `_app.orm.xml` 中 ErpMfgWorkOrderLine 的 `sourceWarehouse` / `destWarehouse` to-one 关系名与 `ErpMdWarehouse.getName()` 可达性（已初步确认，Phase 2 落地前复核）。
  - Skill: `nop-backend-dev`
- [ ] `Add`: ErpMfgWorkOrderLine xmeta 增 `sourceWarehouseName` + `destWarehouseName` 派生 prop；Line BizModel 在既有 materialName `@BizLoader` 方法内追加双仓库批量加载（`orm().batchLoadProps(lines, CollectionHelper.buildSet("sourceWarehouse","destWarehouse"))` + 逐行 `getSourceWarehouse().getName()` / `getDestWarehouse().getName()`）；view.xml grid 既有 `<cols>` 内追加 `sourceWarehouseName` / `destWarehouseName` 列替换原始 `sourceWarehouseId` / `destWarehouseId` 列。
  - Skill: `nop-frontend-dev`、`nop-backend-dev`
- [ ] `Proof`: ErpMfgWorkOrderLine grid 列本地化验证（xmeta 双 prop 存在 + view grid 含双 `*Name` 列 + `_gen` 原始双 `*Id` 列被覆盖）+ 涉及模块编译通过 + manufacturing 域代表行实体 BizLoader 批量加载测试（在 `TestErpMfgFkNameLoader` 或既有测试类新增 sourceWarehouseName/destWarehouseName 解析测试方法）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] ErpMfgWorkOrderLine xmeta + Line BizModel + view grid 三层 `sourceWarehouseName` / `destWarehouseName` 落地
- [ ] 涉及模块编译通过

## Draft Review Record

- Independent draft review iteration 1: <待独立子代理审查>

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 涉及 service `mvn test` 一次。

- [ ] 范围内行为完成（5 行实体 warehouseName 解析）
- [ ] 相关文档对齐（`view-and-page-strategy.md` 行实体 FK 名称解析范式已由批次 1/2/3 确立，本批次推广至仓库 FK，无新约定）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 涉及 service `mvn test` 代表 FK name loader 测试全绿 + view.xml/xmeta `xmllint --noout` well-formed）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 行实体其他 FK 名称解析（uomId / skuId / currencyId / projectId / costCenterId 等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 仓库（warehouseName）为行实体 materialName 之后第二高价值 FK。计量单位可经物料带出；SKU/币种/项目/成本中心为低频维度，用户面价值低。
- Successor Required: `yes`（触发条件：低频 FK 显示需求累积时）

### 全量 ~60 行实体 FK 名称解析（本批之外）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖 5 含仓库外键的核心交易行实体。其余行实体（invoice/return/quotation 等无仓库列行实体、财务/CRM/HR 等域行实体）含仓库/物料语义的少，或 FK 为内部实体引用/会计辅助维度。
- Successor Required: `yes`（触发条件：低频域行实体 FK 显示需求累积时，或 codegen 模板层 FK 名称解析方案落地时）

### ErpInvStockMoveLine 仓库/库位名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: StockMoveLine 行级无仓库列（仓库在父头 ErpInvStockMove 上，已由批次 2 头实体覆盖）；行级仅有库位列（sourceLocationId/destLocationId），库位粒度过细且非仓库语义。
- Successor Required: `no`

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- 行实体其他 FK（uom/sku/currency/project/costCenter）successor（见上方 Deferred）
- 全量行实体 FK 名称解析 successor（见上方 Deferred）
