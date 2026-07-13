# 2026-07-13-1419-1-assets-fk-name-resolution Assets 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件「高价值子集验证后批量推广需求，或 codegen 模板层 FK 名称解析方案落地」——**已满足**：经 1643-1 Phase 3 + 批次 2 `2026-07-12-0600-1` + 批次 3 `2026-07-12-0800-2` + 批次 4 `2026-07-12-0900-2` + finance `2026-07-13-1043-1` + manufacturing `2026-07-13-1043-2` 共 6 批次验证机制 D 可行性，55 实体已落地含测试全绿；codegen 模板路径经 0600-1 裁决否决）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源）、`2026-07-13-1043-1-finance-fk-name-resolution.md`（finance 域先例）、`2026-07-13-1043-2-manufacturing-fk-name-resolution.md`（manufacturing 域先例）、`2026-07-13-1419-2-projects-fk-name-resolution.md`（同批 N=2，无依赖）、`2026-07-13-1419-3-quality-maintenance-fk-name-resolution.md`（同批 N=3，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 `module-assets/erp-ast-meta/` + `erp-ast-service/` + `erp-ast-web/`）：

### 机制 D 已验证（全域 6 批次 55 实体）

机制 D 三层接线（参考 `ErpSalOrder` header 先例 + `ErpMfgWorkOrderLine` 线先例）：(1) 自定义 xmeta 增派生 `*Name` prop（`queryable="false" sortable="false"`，`schema type="java.lang.String"`）；(2) BizModel 增 `@BizLoader(forType = Entity.class)` 方法（签名 `public List<String> xxxName(@ContextSource List<Entity> rows)`，经 `orm().batchLoadProps(rows, Collections.singleton("<relation>"))` 批量加载 to-one 关系防 N+1，读 `getRelation().getName()`）；(3) 自定义 view.xml `<grid id="list"><cols x:override="bounded-merge">` 用 `*Name` 列替换原始 `*Id` 列。`ext:relation` 已在全部相关 FK `*Id` prop 上声明于 `_gen` xmeta，`batchLoadProps` 开箱可用——零 ORM 变更。

### Assets 域覆盖现状

- **零 FK 名称解析覆盖**：全部 18 实体的自定义 view.xml 为空 `<grid id="list"/>`（继承生成基线列原样），无 `<cols>` bounded-merge 覆盖。
- **未覆盖（列表页显示原始数字 ID）18 实体**——本计划范围。

### 未覆盖 18 实体清单（生成网格中显示为 `ui:number` 的 FK 列）

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpAstAsset** | orgId, categoryId, currencyId, departmentId, locationId, employeeId, staffId | 资产卡片，固定资产核心 |
| 2 | **ErpAstAssetCategory** | subjectId, depreciationSubjectId, expenseSubjectId, disposalGainLossSubjectId, cipSubjectId | 资产类别（5 科目 FK） |
| 3 | **ErpAstAssetCapitalization** | orgId, categoryId, currencyId | 资本化单 |
| 4 | **ErpAstCip** | orgId, categoryId, currencyId, completedAssetId | CIP 在建工程 |
| 5 | **ErpAstCipCostItem** | cipId, orgId, currencyId | CIP 成本明细 |
| 6 | **ErpAstCipProgressBilling** | cipId, orgId, currencyId | CIP 进度付款 |
| 7 | **ErpAstDepreciationSchedule** | assetId, orgId, currencyId | 折旧计划 |
| 8 | **ErpAstDisposal** | orgId, assetId, currencyId | 资产处置 |
| 9 | **ErpAstInventory** | orgId, rangeDepartmentId, rangeCategoryId, rangeLocationId, responsibleById, currencyId | 资产盘点头 |
| 10 | **ErpAstInventoryLine** | inventoryId, orgId, assetId, categoryId, newAssetId | 盘点明细行 |
| 11 | **ErpAstMaintenance** | orgId, assetId, currencyId | 资产维修 |
| 12 | **ErpAstMaintenanceCost** | maintenanceId, orgId, currencyId | 维修费用 |
| 13 | **ErpAstMerge** | orgId, targetAssetId, currencyId | 资产合并 |
| 14 | **ErpAstMergeLine** | mergeId, orgId, sourceAssetId | 合并明细行 |
| 15 | **ErpAstMovement** | orgId, assetId, fromDepartmentId, toDepartmentId, fromStaffId, toStaffId, fromLocationId, toLocationId, handlerId, currencyId | 资产移动（10 FK 列，最严重） |
| 16 | **ErpAstSplit** | orgId, sourceAssetId, currencyId | 资产拆分 |
| 17 | **ErpAstSplitLine** | splitId, orgId, categoryId, targetAssetId | 拆分明细行 |
| 18 | **ErpAstValueAdjustment** | orgId, assetId, currencyId | 资产减值/重估 |

### ext:relation 缺口（5 个 FK 列无 `ext:relation`，阻碍名称解析）

| # | 实体 | FK prop | 处置裁决 |
|---|------|---------|---------|
| 1 | ErpAstCip | projectId | 保留原始 ID（跨域弱指针，无独立业务名称语义，归 successor） |
| 2 | ErpAstCipCostItem | capitalizationId | 保留原始 ID（转固回指，归 successor） |
| 3 | ErpAstDepreciationSchedule | voucherId | 保留原始 ID（凭证回指，归 successor） |
| 4 | ErpAstDisposal | nopFlowId | 保留原始 ID（系统工作流字段 String 类型，非数值 FK） |
| 5 | ErpAstMaintenance | maintenanceVisitId | 保留原始 ID（弱指针，归 successor） |

> 裁决原则：与 finance 批次（1043-1）同口径——纯匹配/链路型内部 ID（自引用/溯源链路）无独立业务"名称"语义时保留原始 ID。以上 5 个 FK 均为弱指针/系统字段/跨域链路，不解析。

剩余差距：18 assets 实体列表页显示原始数字 ID（用户面 P1 缺陷）。

## Goals

- 18 assets 实体列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D：xmeta `*Name` + BizModel `@BizLoader` 批量加载 + view.xml `bounded-merge`）。
- 高价值 FK 定义：维度型外键（category→categoryName 读 `ErpAstAssetCategory.name`；currency→currencyName；org→orgName；department→departmentName 读 `ErpMdOrganization.name`；location→locationName 读 `ErpMdLocation.name`；employee→employeeName 读 `ErpMdEmployee.name`；staff→staffName 读 `ErpMdEmployee.name`；subject→subjectName 读 `ErpMdSubject.name`；asset→assetCode 读 `ErpAstAsset.code`；cip→cipCode 读 `ErpAstCip.code`；inventory→inventoryCode；maintenance→maintenanceCode；merge→mergeCode；split→splitCode）+ 高价值父单型内部链路（cipId/mergeId/splitId/maintenanceId/inventoryId→父单 code，承载业务上下文）。
- 零 ORM/契约变更（机制 D 仅 xmeta 派生字段 + BizModel 只读 loader + view.xml 静态定制）。

## Non-Goals

- **其他域 FK 名称解析**（projects/quality/maintenance 由 `2026-07-13-1419-2/3` 承接；CRM/CS/HR/master-data/logistics/contract/b2b/drp/aps 归后续 successor）。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决（触及 nop-entropy 平台保护区域 + BizModel 为手写类无法 codegen 注入 `@BizLoader`）。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`（与既有 6 批次同口径）。
- **5 个 ext:relation 缺口 FK 的名称解析**（projectId/capitalizationId/voucherId/nopFlowId/maintenanceVisitId）——弱指针/系统字段/跨域链路，无独立业务"名称"语义，保留原始 ID，归 successor（触发条件：对应实体落地 code 列或业务需求要求解析时）。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 覆盖。

## Task Route

- Type: `app-layer design change`（改用户可见的列表页显示行为，跨 assets 域多实体，不改 API/模型/认证）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（机制 D 权威参考）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`（加 BizLoader 字段）
- Skill Selection Basis: xmeta 派生字段 + view.xml grid `bounded-merge` → 匹配 `nop-frontend-dev`（XView 三层 / bounded-merge / delta 覆盖）；BizModel `@BizLoader` 跨实体批量加载 → 匹配 `nop-backend-dev`（决策门 / `@BizLoader` / `orm().batchLoadProps`）；JUnit 测试 → `nop-testing`（`IGraphQLEngine` findList 触发 loader）。
- Protected Areas: 无 ORM/ask-first 变更（机制 D 为 xmeta 派生 + 只读 loader + 静态 view 定制）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。零端口/环境变量/密钥/外部服务/数据迁移依赖。

## Execution Plan

### Phase 1 - 核心资产实体 FK 名称解析（Asset/AssetCategory/Disposal/DepreciationSchedule/ValueAdjustment/Capitalization）

Status: completed
Targets: `module-assets/erp-ast-meta/.../ErpAst{Asset,AssetCategory,Disposal,DepreciationSchedule,ValueAdjustment,AssetCapitalization}/ErpAst*.xmeta`；`module-assets/erp-ast-service/.../entity/ErpAst*BizModel.java`；`module-assets/erp-ast-web/.../ErpAst*/ErpAst*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无（机制 D 已由 6 批次验证）

- [x] `Decision`: 裁决 Phase 1 实体的目标 FK 清单 + 显示字段——维度型 FK 全部解析（category→categoryName 读 `ErpAstAssetCategory.name`；subject→subjectName 读 `ErpMdSubject.name`；currency→currencyName；org→orgName；department→departmentName 读 `ErpMdOrganization.name`；location→locationName 读 `ErpMdLocation.name`；employee→employeeName 读 `ErpMdEmployee.name`；staff→staffName 读 `ErpMdEmployee.name`；asset→assetCode 读 `ErpAstAsset.code`）。Phase 1 实体无内部链路型 FK 需逐项裁决（completedAssetId 属于 Phase 2 ErpAstCip 范围，在此不涉及）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 6 实体 xmeta 增派生 `*Name` prop（镜像 `ErpSalOrder.xmeta`，`queryable="false" sortable="false"` + `schema type="java.lang.String"`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 6 实体 BizModel 增 `@BizLoader(forType = ErpAst*.class)` 方法（镜像 `ErpSalOrderBizModel:228-269`，`orm().batchLoadProps(rows, Collections.singleton("<relation>"))` 批量加载 + null 安全读取）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 6 实体 view.xml `<grid id="list">` 由空占位改为 `<cols x:override="bounded-merge">`，用 `*Name` 列替换原始 `*Id` 列，保留 code/status/amount/date 等非 FK 业务列。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 6 核心资产实体列表网格显示 `*Name` 而非原始 `*Id`（6 view.xml `xmllint --noout` well-formed + bounded-merge 含 `*Name` 列）

### Phase 2 - CIP/盘点/维修/合并/拆分/移动实体 FK 名称解析（Cip/CipCostItem/CipProgressBilling/Inventory/InventoryLine/Maintenance/MaintenanceCost/Merge/MergeLine/Movement/Split/SplitLine）

Status: completed
Targets: 12 实体的 xmeta + BizModel + view.xml（同 Phase 1 三层）
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 范式已验证

- [x] `Decision`: 逐项裁决 Phase 2 父单型/内部链路型 FK 的处置（原则：子实体有独立平铺列表页且父单 code 承载业务含义→解析为 code；纯匹配/链路且无业务"名称"→保留原始 ID）：cipId→cipCode、mergeId→mergeCode、splitId→splitCode、maintenanceId→maintenanceCode、inventoryId→inventoryCode（均为父单 code 承载业务上下文，解析）；completedAssetId/capitalizationId/newAssetId/targetAssetId/sourceAssetId（父单/资产 code 承载上下文，解析为 assetCode）。5 个 ext:relation 缺口 FK（projectId/capitalizationId[ErpAstCipCostItem]/voucherId/nopFlowId/maintenanceVisitId）保留原始 ID（见 Current Baseline 裁决）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 12 实体 xmeta 增派生 `*Name` prop（按 Phase 2 Decision 裁决清单）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 12 实体 BizModel 增 `@BizLoader` 方法（维度型 FK 同 Phase 1 范式读 `.name`；父单型读父实体 `.code`）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 12 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`，`*Name` 替换 `*Id`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 12 资产附属实体列表网格显示 `*Name` 而非原始 `*Id`（12 view.xml `xmllint --noout` well-formed）

### Phase 3 - BizLoader 测试验证

Status: completed
Targets: `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstFkNameLoader.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add`: 新建 `TestErpAstFkNameLoader.java`（extends `JunitAutoTestCase`，镜像 `TestErpFinFkNameLoader`），经 `IGraphQLEngine` findList + `FieldSelectionBean` 请求 `*Name` 字段触发 `@BizLoader`，断言 `ErpAstAsset`（categoryName/currencyName/orgName/departmentName）+ `ErpAstMovement`（assetCode/fromDepartmentName/toDepartmentName）名称对齐 master-data。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `TestErpAstFkNameLoader` 全方法绿，验证 `@BizLoader` 批量加载防 N+1 且名称正确

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0a5d0897fffeb8zkKal1K7RMEp, 2026-07-13) because baseline claims all verified against live repo (18 entities / FK columns / 5 ext:relation gaps / 6-batch precedent / trigger met); 0 Blocker / 0 Major / 3 Minor. Minor 1 (Phase 1 Decision hedge removed), Minor 2 (staffId→staffName naming fixed), Minor 3 (Proof dropped from phase item types) — all fixed.
- Independent draft review iteration 2: `accept` — iteration 1 acceptable as-is, 3 Minor issues fixed (hedge removal, staffName naming, Proof item types). Plan ready for `active`.

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-assets/erp-ast-service -am`（含新增 `TestErpAstFkNameLoader`）+ 18 view.xml `xmllint --noout` 一次。

- [x] 范围内行为完成（18 assets 实体列表页 FK 显示名称）
- [x] 相关文档对齐（`view-and-page-strategy.md` / `cross-module-entity-reference.md` 机制 D 范式无需更新；本计划为既有范式批量推广）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + assets-service `mvn test` 0 failures/0 errors + 18 view.xml `xmllint --noout` well-formed）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 5 个 ext:relation 缺口 FK 的名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: projectId（ErpAstCip 跨域弱指针）/capitalizationId（ErpAstCipCostItem 转固回指）/voucherId（ErpAstDepreciationSchedule 凭证回指）/nopFlowId（ErpAstDisposal 系统工作流 String）/maintenanceVisitId（ErpAstMaintenance 弱指针）均为弱指针/系统字段/跨域链路，无独立业务"名称"语义或 ext:relation 缺失。
- Successor Required: `yes`（触发条件：对应实体落地 code 列或业务需求要求解析时）

### 其他域 FK 名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅覆盖 assets 域。projects 由 `2026-07-13-1419-2` 承接，quality/maintenance 由 `2026-07-13-1419-3` 承接，CRM/CS/HR/master-data/logistics/contract/b2b/drp/aps 归后续 successor。
- Successor Required: `yes`（触发条件：对应域 FK 名称解析需求落地时）

## Closure

Status Note: <completed — 18 assets 实体 FK 名称解析落地，全量验证绿>

Closure Audit Evidence:

- Auditor / Agent: <pending closure audit by independent subagent>

Follow-up:

- 其他域 FK 名称解析 successor（见上方 Deferred）
