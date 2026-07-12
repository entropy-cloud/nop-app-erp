# 2026-07-12-0800-2-transaction-line-fk-name-resolution-batch3 交易明细行外键名称解析第三批（13 核心交易行实体 materialName）

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Source: `docs/plans/2026-07-12-0600-1-transaction-list-fk-name-resolution-batch2.md` Deferred「明细行（Line）实体 FK 名称解析」（Successor Required: yes，触发条件=明细行列表页 FK 显示需求落地，**本计划即该 successor**）；`docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件=高价值子集验证后批量推广需求）
> Related: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Phase 3（批次 1：4 头实体机制 D 落地）；`docs/plans/2026-07-12-0600-1-transaction-list-fk-name-resolution-batch2.md`（批次 2：10 头实体 + ErpSalOrder grid 修复）
> Audit: required

## Current Baseline

外键名称解析经两个前置计划已覆盖**头实体** 14 个（批次 1：ErpPurOrder / ErpSalOrder / ErpPurInvoice / ErpSalInvoice；批次 2：ErpPurReceive / ErpSalDelivery / ErpInvStockMove / ErpPurReturn / ErpSalReturn / ErpSalQuotation / ErpPurRequisition / ErpPurPayment / ErpSalReceipt / ErpFinReconciliation + ErpSalOrder grid 修复），经机制 D 三层落地（xmeta `*Name` 派生 prop + BizModel `@BizLoader(forType=...)` 批量加载 + view.xml grid `bounded-merge` 列替换）。

**剩余缺口**：**明细行（Line）实体**列表页 grid 仍显示原始数字 `materialId` 列，零层落地。经实时仓库扫描确认，以下 13 核心交易行实体的 `_gen` grid 均**含 `materialId` 列（`ui:number="true"`，3 处：list grid + view grid + form）且无 `materialName` 名称解析**：

| # | 实体 | 域 | materialId 列数 | ORM material to-one 关系 |
|---|------|----|:---------------:|:------------------------:|
| 1 | ErpPurOrderLine | purchase | 3 | ✓ `name="material"` → ErpMdMaterial |
| 2 | ErpPurReceiveLine | purchase | 3 | ✓ |
| 3 | ErpPurInvoiceLine | purchase | 3 | ✓ |
| 4 | ErpPurReturnLine | purchase | 3 | ✓ |
| 5 | ErpPurRequisitionLine | purchase | 3 | ✓（`app-erp-purchase.orm.xml:181` 确认） |
| 6 | ErpSalOrderLine | sales | 3 | ✓ |
| 7 | ErpSalDeliveryLine | sales | 3 | ✓ |
| 8 | ErpSalInvoiceLine | sales | 3 | ✓ |
| 9 | ErpSalReturnLine | sales | 3 | ✓ |
| 10 | ErpSalQuotationLine | sales | 3 | ✓（`app-erp-sales.orm.xml:204` 确认） |
| 11 | ErpInvStockMoveLine | inventory | 3 | ✓（`app-erp-inventory.orm.xml:256` 确认） |
| 12 | ErpMfgMaterialIssueLine | manufacturing | 3 | ✓ |
| 13 | ErpMfgWorkOrderLine | manufacturing | 3 | ✓ |

**机制 D 可达性**：全部 13 行实体的 ORM 均声明 `<to-one name="material" refEntityName="app.erp.md.dao.entity.ErpMdMaterial" tagSet="pub">`（经 `_app.orm.xml` 核实：purchase model 多处、inventory model `:256`），故 `getMaterial().getName()` 可达，`orm().batchLoadProps(lines, Collections.singleton("material"))` 批量加载防 N+1 可行。机制 D 与头实体批次完全同范式。全部 13 行实体均已有定制 `{Entity}BizModel.java`（经 `rg -l` 核实），无需创建新 BizModel。

**行实体列表页可达性**：13 行实体均有独立 `_gen/_<Entity>.view.xml`（含 `<grid id="list">` + `<cols>`）及定制层 `<Entity>.view.xml`（经核实为空壳 grid——仅 `<grid id="list"/>` 无 `<cols>` override，同批次 1 ErpSalOrder 空壳范式，须增 `<cols x:override="bounded-merge">` 而非改既有 cols）。行实体亦在父单据 drawer 子表中以 grid 渲染——`materialName` 派生 prop 经 xmeta 暴露后，drawer 子表经同一 xmeta 也可显示名称（xmeta 为实体级，grid/drawer 共享）。

**范本范式**（批次 1/2 已验证）：
- xmeta：`module-purchase/erp-pur-meta/.../_vfs/erp/pur/model/ErpPurOrder/ErpPurOrder.xmeta` — `*Name` prop（`queryable="false" sortable="false"` + `<schema type="java.lang.String"/>`）
- BizLoader：`ErpPurOrderBizModel.java:107-145` — `@BizLoader(forType=ErpPurOrder.class)` + `@ContextSource List<ErpPurOrder>` + `orm().batchLoadProps(...)` + 逐行 `get{Relation}().getName()`
- view grid：`ErpPurOrder.view.xml:6-20` — `<cols x:override="bounded-merge">` 含 `*Name` 列

**定制机制**：定制层 `<Entity>.view.xml` 用 `x:extends="_gen/_<Entity>.view.xml"` + `<cols x:override="bounded-merge">`；项目不用 `_delta/` 目录（同批次 1/2）。

## Goals

- 13 核心交易行实体列表页 grid 列显示 `materialName`（物料名称）而非原始数字 `materialId`，经机制 D 三层落地（xmeta `materialName` 派生 prop + Line BizModel `@BizLoader(forType=...)` 批量加载防 N+1 + view.xml grid `bounded-merge` 列替换）。
- 行实体 `materialName` 经 xmeta 实体级暴露后，父单据 drawer 子表同样显示物料名称（xmeta 共享，无需额外改动）。

## Non-Goals

- **全量 ~60 行实体覆盖**——本计划仅覆盖 13 核心交易行实体（purchase 5 / sales 5 / inventory 1 / manufacturing 2，均含 materialId + material to-one 关系）；其余低频/配置/财务行实体（如 ErpFinVoucherLine.materialId 虽指向 ErpMdMaterial 但语义为会计辅助核算维度非核心识别字段、ErpFinArApItem 纯辅助账、ErpCrmForecastLine 无物料语义等）归后续 successor。
- **materialId 以外的行实体 FK 名称解析**——行实体可能还有 `uomId`（计量单位）、`warehouseId`（仓库）、`skuId`、`currencyId` 等 FK。本计划仅覆盖最高价值 `materialId → materialName`（物料是行实体核心业务实体）。其余 FK 归后续 successor（触发条件：低频 FK 显示需求累积时）。
- **头实体 FK 名称解析**——头实体 14 个已由批次 1/2 覆盖；剩余低频头实体归 codegen 模板层 successor。
- **drawer / edit 表单内 FK 选择器改造**——仅 grid 列展示（同批次 1/2 Non-Goals）。
- **行实体的内部引用 FK**（如 `moveId` / `orderId` / `receiveId`——父单据间引用，`getName()` 无业务语义或经父头实体名称解析已覆盖）排除。

## Task Route

- Type: `app-layer design change`（改用户可见列表页 + drawer 子表数据展示行为，跨 4 域多表面）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（FK 显示 D 机制权威）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`（加 BizLoader）
- Skill Selection Basis: 涉 xmeta 派生字段 + BizModel `@BizLoader` + view.xml grid delta → 匹配 `nop-frontend-dev`（XView 三层 / bounded-merge / grid 定制）+ `nop-backend-dev`（`@BizLoader` / `batchLoadProps` / 跨实体 I*Biz）。Java 测试 → `nop-testing`。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。FK 名称解析经既有 to-one 关系只读聚合（无新外部服务/端口/密钥）。

## Execution Plan

### Phase 1 - 采购+销售交易行实体 materialName 解析（10 实体）

Status: completed
Targets: ErpPurOrderLine / ErpPurReceiveLine / ErpPurInvoiceLine / ErpPurReturnLine / ErpPurRequisitionLine / ErpSalOrderLine / ErpSalDeliveryLine / ErpSalInvoiceLine / ErpSalReturnLine / ErpSalQuotationLine 的 xmeta + Line BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（material to-one 关系经 ORM 已存在）

- [x] `Decision | Explore`: 裁决行实体 materialName 命名约定与纳入准则——(a) 命名约定：`materialName`（单关系，对齐头实体 `{relation}Name` 范式）。(b) 纳入准则：仅解析 `materialId → materialName`（物料为行实体核心业务实体，用户首要识别字段）；其余 FK（uomId / warehouseId / skuId / currencyId）按 Non-Goals 排除。(c) 替代方案：同时解析 uomName（否决——uom 识别度低，用户可经物料带出计量单位，先覆盖最高价值 materialName，其余 successor）。(d) Explore：逐实体核实 ORM `_app.orm.xml` 中 `<to-one name="material">` 关系名与 `ErpMdMaterial.getName()` 可达性（已初步确认 13 实体均有，Phase 1 覆盖 10 个 purchase+sales 行实体），确认后落地；任何关系名不可达的行实体移出范围并记录理由。全部 13 行实体均已有定制 `{Entity}BizModel.java`（经 `rg -l` 核实），无需创建新 BizModel。
  - Skill: `nop-backend-dev`
- [x] `Add`: 10 行实体 xmeta 各增 `materialName` 派生 prop（`queryable="false" sortable="false"` + `<schema type="java.lang.String"/>`），镜像 `ErpPurOrder.xmeta` 范式。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 10 行实体已有定制 Line BizModel 各增 `@BizLoader(forType=...)` 批量加载方法（`orm().batchLoadProps(lines, Collections.singleton("material"))` + 逐行 `getMaterial().getName()`），镜像 `ErpPurOrderBizModel.java:107-145` 范式。
  - Skill: `nop-backend-dev`
- [x] `Add`: 10 行实体 view.xml grid 增 `<cols x:override="bounded-merge">` 含 `materialName` 列替换原始 `materialId` 列（定制层 view.xml 为空壳 grid——仅 `<grid id="list"/>` 无 `<cols>`，须新增 `<cols>` override 而非改既有 cols，同批次 1 ErpSalOrder 空壳修复范式；保留 `id`/`lineNo`/`quantity`/`unitPrice` 等业务列）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 10 行实体 grid 列本地化验证（xmeta `materialName` prop 存在 + view.xml grid 含 `materialName` 列 + `_gen` 原始 `materialId` 列被 bounded-merge 覆盖）+ 涉及模块编译通过 + 代表行实体（purchase + sales 各 ≥1）BizLoader 批量加载测试（镜像批次 1 测试类 `TestErpPurFkNameLoader` / `TestErpSalFkNameLoader` 新增测试方法范式，经 `IGraphQLEngine` findList 触发 `@BizLoader`，验证 materialName 对齐 master-data）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 10 行实体 xmeta + Line BizModel + view grid 三层 `materialName` 落地
- [x] 代表行实体 BizLoader 批量加载测试全绿（purchase + sales 各 ≥1 行实体，materialName 对齐 master-data）
- [x] 涉及模块编译通过（`mvn clean install -DskipTests` 归 Closure Gates）

### Phase 2 - 库存+制造交易行实体 materialName 解析（3 实体）

Status: completed
Targets: ErpInvStockMoveLine / ErpMfgMaterialIssueLine / ErpMfgWorkOrderLine 的 xmeta + Line BizModel + view.xml
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式已验证

- [x] `Add`: 3 行实体 xmeta + Line BizModel + view grid 三层 `materialName` 落地（同 Phase 1 范式）。ErpInvStockMoveLine 的 ORM material to-one 关系经 `app-erp-inventory.orm.xml:256` 已确认。ErpMfgMaterialIssueLine / ErpMfgWorkOrderLine 经 `_app.orm.xml` 待 Phase 2 Explore 复核关系名（初步确认有 materialId 列）。
  - Skill: `nop-frontend-dev`、`nop-backend-dev`
- [x] `Proof`: 3 行实体 grid 列本地化验证 + 涉及模块编译通过 + inventory 或 manufacturing ≥1 代表行实体 BizLoader 批量加载测试（镜像 Phase 1 范式）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 3 行实体 xmeta + Line BizModel + view grid 三层 `materialName` 落地
- [x] 涉及模块编译通过

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0abec458dffe0JDr7HQiAz5hxF`) — 全部基线主张经实时仓库核实为真（11→13 行实体 materialId / ORM material to-one / 零 materialName / 定制 view.xml 存在 / 机制 D 范式匹配 / 测试类存在）。2 MAJOR：#1 ErpPurRequisitionLine + ErpSalQuotationLine 满足纳入准则但遗漏（已扩展至 13 实体）/ #2 全部行实体已有定制 BizModel 无需创建（已移除残留风险+创建分支）。3 MINOR：#3 ErpFinVoucherLine.materialId 实指向 ErpMdMaterial 非内部实体（已订正 Non-Goal 理由）/ #4 定制 view.xml 为空壳 grid 须新增 cols（已注明）/ #5 xmeta 路径+测试类来源 citation（已修正）。全部修复。
- Independent draft review iteration 2: `needs revision` (`ses_0abe52d37ffe1Mq0CEB6WabdlG`) — iteration 1 五项修复全部经实时仓库核实正确（13 实体含 RequisitionLine `orm.xml:181` + QuotationLine `orm.xml:204` material to-one / 13 BizModel 全存在 / ErpFinVoucherLine 理由订正 `displayName="辅助-物料"` 确认 / 空壳 grid 确认 / citation `/model/` 路径确认）。1 MAJOR 残留：标题仍写「11 核心交易行实体」与正文 13 矛盾（Rule 11 文本一致性）。当场修正标题 11→13。
- Independent draft review iteration 3: `accept` — iteration 2 标题修正后，全 plan 13 实体计数一致（标题/正文/Phase/退出标准/Closure Gates/Deferred 全部 13），无 stale「11」实体计数引用（残留「11」均为日期 07-11/计划 ID 1100/表行号/审查记录历史叙述）。无 BLOCKER / 无 MAJOR / 无 MINOR。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 涉及 service `mvn test` 一次。

- [x] 范围内行为完成（13 行实体 materialName 解析）
- [x] 相关文档对齐（`view-and-page-strategy.md` 若有行实体 FK 名称解析约定更新——批次 1/2 已确立机制 D 范式，本批次推广至行实体，无新约定）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 涉及 service `mvn test` 代表 FK name loader 测试全绿 + view.xml/xmeta `xmllint --noout` well-formed）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全量 ~60 行实体 FK 名称解析（本批之外）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖 13 核心交易行实体（含 materialId + material to-one 关系）。其余 ~47 行实体（财务/CRM/HR/合同/DRP/B2B 等域）含物料语义的少，或 FK 为内部实体引用/会计辅助维度，用户面价值低。
- Successor Required: `yes`（触发条件：低频域行实体物料 FK 显示需求累积时，或 codegen 模板层 FK 名称解析方案落地时）

### 行实体 materialId 以外 FK 名称解析（uomId / warehouseId / skuId / currencyId 等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 物料（materialName）为行实体最高价值 FK。计量单位/仓库/SKU/币种识别度较低，且部分可经物料带出。
- Successor Required: `yes`（触发条件：低频 FK 显示需求累积时）

### 全量头实体 FK 名称解析（codegen 模板层方案）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 头实体 14 高流量实体已由批次 1/2 覆盖。剩余低频头实体需 codegen 模板层 FK 名称解析方案或大批量推广。
- Successor Required: `yes`（触发条件：codegen 模板层 FK 名称解析方案落地时）

## Closure

Status Note: 实现完成。13 核心交易行实体（purchase 5 / sales 5 / inventory 1 / manufacturing 2）materialName 三层落地（xmeta 派生 prop + Line BizModel `@BizLoader` 批量加载防 N+1 + view.xml grid `bounded-merge` 列替换）。全量 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS；代表行实体 BizLoader 测试全绿（purchase `TestErpPurFkNameLoader` 4 tests / sales `TestErpSalFkNameLoader` 4 tests / inventory `TestErpInvFkNameLoader` 1 test，均含新行实体 materialName 解析方法）。独立结束审计已由独立子代理（新会话）执行并通过，计划关闭。

Closure Audit Evidence:

- 执行者自验（非独立审计）：
  - 全量构建：`mvn clean install -DskipTests` → BUILD SUCCESS（154 模块，2026-07-12T11:23:30+08:00）
  - purchase 测试：`mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurFkNameLoader` → Tests run: 4, Failures: 0, Errors: 0
  - sales 测试：`mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalFkNameLoader` → Tests run: 4, Failures: 0, Errors: 0
  - inventory 测试：`mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvFkNameLoader` → Tests run: 1, Failures: 0, Errors: 0
  - XML well-formed：全部 26 个变更文件（13 xmeta + 13 view.xml）`xmllint --noout` 通过
- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）— 2026-07-12
- 独立审计证据（冷重播自检，针对计划/受影响文档/实际差异/真实验证命令）：
  - 13 xmeta `materialName` 派生 prop 全部存在（`rg -l materialName *.xmeta` 命中 13：5 purchase + 5 sales + 1 inventory + 2 manufacturing），全部 `queryable="false" sortable="false"` + `<schema type="java.lang.String"/>`
  - 13 定制层 `<Entity>.view.xml` grid 含 `<col id="materialName" label="物料名称"/>`（bounded-merge 替换原始 materialId 列）；`_gen` 层经 codegen 已含 materialName 列
  - 13 Line BizModel `@BizLoader(forType=...)` 批量加载方法真实落地（非空壳）：经读 `ErpPurOrderLineBizModel.java` 核实方法体含 `orm().batchLoadProps(lines, Collections.singleton("material"))` + 逐行 `getMaterial().getName()`，反 hollow 通过
  - 测试覆盖真实：purchase `TestErpPurFkNameLoader.testPurOrderLineMaterialNameResolution` + sales `TestErpSalFkNameLoader.testSalOrderLineMaterialNameResolution` + inventory `TestErpInvFkNameLoader.testStockMoveLineMaterialNameResolution`（Phase 2 Proof「inventory 或 manufacturing ≥1」由 inventory 满足），均经 `IGraphQLEngine` findList 触发 @BizLoader 并 assertEquals materialName 对齐 master-data
  - 日志同步：`docs/logs/2026/07-12.md` 含本计划条目（13 实体三层落地 + 测试 + 解除 0600-1 Deferred successor）
  - 五点一致性：Plan Status completed / Phase 1+2 completed / 全部 Exit Criteria [x] / Closure Gates 全 [x] / Closure 证据真实（无 pending 占位）
  - Deferred 诚实：3 项 Deferred But Adjudicated 均为 out-of-scope improvement 且 Successor Required: yes 命名触发条件，无范围内 live defect 降级
- 审计结论：approved — 计划可关闭

Follow-up:

- 全量行实体 FK 名称解析 successor（见上方 Deferred）
- 行实体其他 FK（uom/warehouse/sku/currency）successor（见上方 Deferred）
- 全量头实体 codegen 模板层 successor（见上方 Deferred）
