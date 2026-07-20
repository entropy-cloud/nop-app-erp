# 2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects F4 Phase 2 — P2 子表行内编辑（mfg/assets/projects 3 头行对）

> Plan Status: active
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` F4 Phase 2（子表行内编辑）P2 mfg/assets/projects；`docs/plans/2026-07-20-0629-1-f4p2-child-table-editor-p1-inventory.md` Deferred But Adjudicated「P2/P3 头行对」(l.246) — Successor Required: yes，触发条件「F4 Phase 2 P2 plan 启动时」**已满足**（本计划即该 successor）
> Related: `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（F4 Phase 2 P0 8 头行对已落地，范式已固化）；`docs/plans/2026-07-20-0629-1-f4p2-child-table-editor-p1-inventory.md`（F4 Phase 2 P1 inventory 3 头行对已落地，退化变体范式已扩展到 `child-table-editor-patterns.md §12`）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 已为核心 4 域 Line 落地 form 分组；本计划 P2 域 Line 实体独立 view.xml form 分组归 F3 P1 successor，本计划仅在已有 form 上加 sub-grid-edit grid）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-20，独立子代理审查后修正）：

- **P2 范围 3 头行对**（roadmap F4 §Phase 2 表 + F4 P1 inventory plan Deferred 承接「P2 mfg/assets/prj 3 对」；经实时仓库 ORM 核实每对存在 `<to-many name="lines">` 关系 + 实际字段集）：
  - **mfg**: `ErpMfgWorkOrder ↔ ErpMfgWorkOrderLine`（ORM `module-manufacturing/model/app-erp-manufacturing.orm.xml` 含 `<to-many name="lines" refEntityName="...ErpMfgWorkOrderLine" tagSet="pub,cascade-delete,insertable,updatable">` ✓；WorkOrderLine 实际字段经 ORM 核实：`workOrderId` `lineNo` `lineType` (OUTPUT/INPUT/BYPRODUCT) + `materialId` `skuId` `uoMId` + `plannedQuantity` `actualQuantity` `scrappedQuantity` + `sourceWarehouseId` `destWarehouseId` + `remark`；**无 quantity × standardCost 可乘字段**，属 P1 退化变体；实际数量拆 planned/actual/scrapped 三态，非单一 quantity）
  - **assets**: `ErpAstInventory ↔ ErpAstInventoryLine`（**ErpAstInventory 是资产盘点单**，非 ErpAstCip——后者经核实 ORM 无 `<to-many>` 到 CipCostItem，不适用子表编辑；ErpAstInventory ORM `<to-many name="lines" refEntityName="...ErpAstInventoryLine">` ✓；InventoryLine 实际字段：`inventoryId` `lineNo` + `assetId` `assetCodeSnapshot` `assetNameSnapshot` `categoryId` + `bookQuantity` `actualQuantity` `varianceQuantity` `varianceType` + `bookValue` `assessedValue` `varianceAmount` + `disposition` + `remark`；**含减法可推算对**：`varianceQuantity = actualQuantity - bookQuantity` + `varianceAmount = assessedValue - bookValue`，属 P0 类带 onEvent 自动推算变体）
  - **projects**: `ErpPrjCostCollection ↔ ErpPrjCostCollectionLine`（ORM `<to-many name="lines" refEntityName="...ErpPrjCostCollectionLine" tagSet="pub,cascade-delete,insertable,updatable">` ✓；CostCollectionLine 实际字段：`costCollectionId` `lineNo` + `costCategory` `sourceBillType` `sourceBillCode` + `subjectId` `taskId` + `amount` + `remark`；**无 quantity × unitPrice 可乘字段**，属 P1 退化变体）
- **codegen 默认行为**（抽样核实 `ErpMfgWorkOrder.view.xml` + `ErpAstInventory.view.xml` + `ErpPrjCostCollection.view.xml` 3 头实体手写层）：3 头实体 view.xml 均无 `<cell id="lines">` 嵌套，无 `<view path=... grid="sub-grid-{edit,view}"/>` 引用；3 Line 实体独立 view.xml 仅有 codegen 默认 `<form id="view"/>/<form id="edit"/>` + 默认 `<grid id="list"/>/<grid id="pick-list"/>`，**无 `<grid id="sub-grid-edit">/<grid id="sub-grid-view">`**。
- **P0 范式已固化**（`docs/design/child-table-editor-patterns.md`）：头表单 `<layout>` 末尾追加 `=========>lines[明细行]======\n lines[明细行](2)` + `<cell id="lines"><view path="..." grid="sub-grid-{edit,view}"/></cell>`；Line view.xml 内手写 `<grid id="sub-grid-edit" x:prototype="list" editMode="list-edit">` + 平行 `<grid id="sub-grid-view" editMode="list-view">`；行内 picker 经 `<col><gen-control><c:script>` 返回 AMIS 控件对象；onChange 自动推算经 AMIS `onEvent.change.actions.setValue` 表达式 `${ROUND(x, 4)}`；行级校验经 `validations.minimum`。
- **P1 退化变体范式已扩展**（`child-table-editor-patterns.md §12`）：无可乘字段实体（如 inventory LandedCostLine 无 quantity × unitPrice）不引入 onEvent 自动推算，仅 picker + 行级校验 `amount >= 0`；本计划 ErpMfgWorkOrderLine + ErpPrjCostCollectionLine 属此变体。
- **picker 前置已就绪 + 缺口经实时仓库重新核实**：
  - `ErpMdMaterial/picker.page.yaml`（mfg WorkOrderLine.materialId + assets InventoryLine.assetId 间接经 ErpAstAsset picker + projects CostCollectionLine.subjectId 经 ErpMdSubject picker）已定制（F4 Phase 1 + F4 P1 inventory plan 落地）
  - ✅ **ErpAstAsset picker.page.yaml + 非空 pick-list grid 均存在**（实时仓库核实 `module-assets/erp-ast-web/.../ErpAstAsset/picker.page.yaml` 存在 + `ErpAstAsset.view.xml:41` `<grid id="pick-list">` 含 6 业务列 + line 110 `<form id="pick-query">` + line 144 `<picker filterForm="pick-query"/>`）—— **本计划无需补齐 ErpAstAsset picker**
  - ⚠️ **ErpPrjProject picker.page.yaml 存在但 pick-list grid 空**（实时仓库核实 `module-projects/erp-prj-web/.../ErpPrjProject/picker.page.yaml` 存在 + `ErpPrjProject.view.xml:70` `<grid id="pick-list"/>` 空引用）；若 ErpPrjCostCollectionLine 含 `taskId` 需 ErpPrjTask picker（核实并补齐）
  - **本计划不涉及 ErpMdProduct 或 ErpMfgProduct 实体**（前期草案错误引用，已删除）—— mfg WorkOrderLine 直接使用 ErpMdMaterial（filter `productFlag` 如需，但实际 WorkOrderLine.materialId 可指向任何物料无需过滤）
- **后端 `__save` 嵌套行端点已就绪**：codegen 自动展开 `<to-many name="lines">` 关系为聚合根 save（P0/P1 已抽样验证 3 处证据 `xmeta` + `InputBean` + DAO 三层印证）；实时仓库已核实 3 P2 头实体 InputBean 均含 `_lines` 字段（grep `ErpMfgWorkOrderInputBean.java:478-485` + `ErpPrjCostCollectionInputBean.java:198-205` 含 `_lines`；ErpAstInventory 同形）。
- **后端 BizModel 无既有行级自动推算冲突**（抽样核实）：`ErpMfgWorkOrderBizModel` / `ErpAstInventoryBizModel` / `ErpPrjCostCollectionBizModel` 无公开行级重算方法；本计划 ErpAstInventoryLine 前端 onEvent setValue（varianceQuantity / varianceAmount 减法推算）不与后端冲突（后端处理只读字段如 `assetCodeSnapshot` 快照）。
- **ErpMfgWorkOrder 特殊性**（实时仓库核实）：`_gen/_ErpMfgWorkOrder.view.xml:156,182` 仅 2 `<layout>` 块（view + edit forms），**无 `<cell id="lines">` 嵌套，无 tabs 容器**；mfg-chain E2E spec（`tests/e2e/orchestration/mfg-chain.spec.ts`）经 GraphQL 驱动 WorkOrder + WorkOrderLine + MaterialIssue + JobCard 多聚合根协作，零 view.xml tabs 断言，**本计划在 WorkOrder form view/edit 嵌入 lines cell 不与 mfg-chain spec 冲突**。
- **前置验证基线**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；Playwright webServer 已配置；F4 P0 8 头行对 + F4 P1 inventory 3 头行对的 sub-grid-edit 写路径 E2E 已落地（`tests/e2e/crud/` + `tests/e2e/orchestration/`），可作 P2 写路径 spec 范式参考。

## Goals

1. **3 P2 头实体**（`ErpMfgWorkOrder` / `ErpAstInventory` / `ErpPrjCostCollection`）的 `<form id="view">` + `<form id="edit">` 在已有分组下方新增「明细行」区，嵌入子表控件渲染对应 Line 实体（行内编辑/查看，支持新增/删除行）。
2. **3 Line 实体独立 view.xml** 新增 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">`，按 P0/P1 范式 + 实际字段集（经 ORM 核实）设计列集（含 picker 接线 + 自动推算触发器（仅 ErpAstInventoryLine）+ 行级校验）：
   - **ErpMfgWorkOrderLine sub-grid-edit**：`lineNo` `lineType` `materialId` `uoMId` `plannedQuantity` `actualQuantity` `scrappedQuantity` `sourceWarehouseId` `destWarehouseId` `remark`（10 列；退化变体无 onEvent 自动推算；行级校验 `plannedQuantity >= 0` + `actualQuantity >= 0`）
   - **ErpAstInventoryLine sub-grid-edit**：`lineNo` `assetId` `categoryId` `bookQuantity` `actualQuantity` `varianceQuantity` `bookValue` `assessedValue` `varianceAmount` `varianceType` `disposition` `remark`（12 列；含 onEvent 自动推算 `varianceQuantity = actualQuantity - bookQuantity` + `varianceAmount = assessedValue - bookValue`）
   - **ErpPrjCostCollectionLine sub-grid-edit**：`lineNo` `costCategory` `sourceBillType` `sourceBillCode` `subjectId` `taskId` `amount` `remark`（8 列；退化变体无 onEvent 自动推算；行级校验 `amount >= 0`）
3. **ErpPrjProject picker pick-list grid 补齐**（picker.page.yaml 已存在 codegen wrapper；本计划在 view.xml 层补 `pick-list` grid + `pick-query` filterForm，对齐 P1 inventory plan ErpMdWarehouse 补齐范式）；服务 ErpPrjCostCollectionLine.taskId（经 ErpPrjTask 间接）或 CostCollection 头表 `projectId` 反向追溯。
4. **写路径 E2E 扩展**：3 头实体新增 spec 或扩展现有 spec：
   - `tests/e2e/orchestration/mfg-chain.spec.ts` 已存在 WorkOrder + WorkOrderLine 创建链路，扩展内联断言子表行字段（materialId/plannedQuantity/sourceWarehouseId）持久化
   - 新建 `tests/e2e/crud/ast-inventory.write.spec.ts`（assets 盘点 + 行写路径，含 varianceQuantity 自动推算断言）
   - 新建 `tests/e2e/crud/prj-cost-collection.write.spec.ts`（项目成本归集 + 行写路径）
5. **更新 `docs/design/child-table-editor-patterns.md`**：新增「§13 P2 mfg/assets/projects 3 头行对列集表」+「§14 减法变体（varianceQuantity = actual - book）扩展范式」+「§15 ErpAstCip 不适用原因 + ErpAstInventory 替换裁决」段落，作为 P3（ext 8 域）输入。
6. **解除 F4 P1 inventory plan Deferred「P2/P3 头行对」P2 子集**：标记 RELEASED。

## Non-Goals

- **ErpAstCip + ErpAstCipCostItem 子表编辑**——经实时仓库 ORM 核实 ErpAstCip 无 `<to-many>` 到 CipCostItem（CipCostItem 经 `cipId` 反向指针 + BizModel `addCostItem(cipId, ...)` 单行 RPC API 设计，非聚合根嵌套）；强行实施需 ORM 修改（保护区域 Non-Goal）；归本计划 Deferred 显式记录（设计意图见 `docs/design/assets/cip.md`）。
- **ErpFinVoucher + ErpFinVoucherLine 子表编辑**（roadmap F4 §Phase 2 P1 表中的 finance 行）——独立 successor plan（依赖 F7 + F10 落地后启动；本批次 plan 1 + plan 2 完成后即可启动）；归本计划 Deferred 显式记录。
- **P3 头行对**（ext 8 域 ~36+ 对）——独立 successor plan；本计划严格限定 mfg/assets/projects 各 1 对共 3 对。
- **mfg 域其他头行对**（如 ErpMfgBom/Line、ErpMfgMaterialIssue/Line、ErpMfgSubcontractOrder/Line、ErpMfgMrpPlan/Line、ErpMfgForecast/Line、ErpMfgCostRollup/Line 等）——本计划仅取 WorkOrder/WorkOrderLine 1 对作为 mfg 域代表，其他对归 P3 扩展或独立 successor。
- **assets 域其他头行对**（如 ErpAstSplit/Line、ErpAstMerge/Line、ErpAstMaintenance/MaintenanceCost via `costLines` relation）——本计划仅取 ErpAstInventory/InventoryLine 1 对（标准 `lines` cell id 范式）；其他对归 successor。
- **projects 域其他头行对**（如 ErpPrjBudget/Line、ErpPrjBilling/Line、ErpPrjProjectSettlement/Line、ErpPrjProject/Task）——本计划仅取 CostCollection/CostCollectionLine 1 对；其他对归 successor。
- **跨单据行导入**（copy-line-from-order）——F9 范畴。
- **多套子表 tabs**（如 WorkOrder 同时含 WorkOrderLine + MaterialIssue + JobCard 等 tabs）——F12 范畴；本计划仅做单一子表编辑控件。
- **工序（ErpMfgBomOperation）行内编辑**（BOM 工序是另一类聚合根，非简单头行结构）——独立 plan 范畴。
- **ErpAstAsset picker 补齐**——经实时仓库核实 picker.page.yaml + 非空 pick-list grid 均已存在（`ErpAstAsset.view.xml:41,110,144`），本计划无需补齐。
- **修改 ORM 模型**——保护区域，仅在 view.xml 层定制。
- **新增 BizModel action**——后端 `__save` 已支持嵌套行；本计划不改后端 Java。
- **F6 字段格式化**（千分位/精度）——已落地（`2026-07-19-2200-2-f6-field-formatting`）；本计划保持现有 `ui:number="true"` 不变。
- **F7 非状态 visibleOn**（如 WorkOrder 字段值驱动显隐）——同期 plan `2026-07-20-1020-2` 范畴；本计划所有列静态显示。
- **测试：浏览器拖拽/扫码**——子表行新增/删除走标准 AMIS input-table add/remove 按钮。

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2 P2
  - `docs/design/child-table-editor-patterns.md`（P0 + P1 已固化范式；本计划扩展 P2 章节）
  - `docs/design/picker-patterns.md`（picker 接线范式 + P2 域专用 picker 缺口记录）
  - `docs/design/manufacturing/ui-patterns.md`（mfg 域业务语义 + WorkOrder/Line 模板）
  - `docs/design/assets/ui-patterns.md`（assets 域业务语义 + Inventory/InventoryLine 模板）
  - `docs/design/projects/ui-patterns.md`（projects 域业务语义 + CostCollection/Line 模板）
  - `docs/architecture/view-and-page-strategy.md`（view.xml 嵌套层次）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（AMIS input-table 嵌套）
  - `../nop-entropy/docs-for-ai/03-runbooks/customize-view.md`（view.xml bounded-merge + 嵌套 cell）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml 嵌套子表 / AMIS input-table / 自动推算 / 行校验 / picker-page 定制）；不新增 BizModel 方法（`__save` 已支持嵌套），故不加载 `nop-backend-dev`；写路径 E2E spec 扩展需 `nop-testing`（playwright config 已有 + P0/P1 spec 范式可参考）。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径（3 域）：
  - 头实体：`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml` + `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstInventory/ErpAstInventory.view.xml` + `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjCostCollection/ErpPrjCostCollection.view.xml`
  - Line 实体：`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrderLine/ErpMfgWorkOrderLine.view.xml` + `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstInventoryLine/ErpAstInventoryLine.view.xml` + `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjCostCollectionLine/ErpPrjCostCollectionLine.view.xml`
  - ErpPrjProject picker 补齐：`module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjProject/ErpPrjProject.view.xml`（picker.page.yaml 已存在 codegen wrapper）
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置（`tests/e2e/playwright.config.ts`）
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — P2 范式裁决与 ErpPrjProject picker pick-list 补齐

Status: planned
Targets: `docs/design/child-table-editor-patterns.md`（扩展 P2 章节）+ ErpPrjProject view.xml pick-list grid 补齐
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] `Decision`: 在 plan 内记录 3 P2 头行对的子表列集（基于实时仓库 ORM 字段实际核实，**非估算**）：
  - **ErpMfgWorkOrderLine sub-grid-edit**：`lineNo` `lineType` `materialId` `uoMId` `plannedQuantity` `actualQuantity` `scrappedQuantity` `sourceWarehouseId` `destWarehouseId` `remark`（10 列；退化变体无 onEvent 自动推算；行级校验 `plannedQuantity >= 0` + `actualQuantity >= 0`）
  - **ErpAstInventoryLine sub-grid-edit**：`lineNo` `assetId` `categoryId` `bookQuantity` `actualQuantity` `varianceQuantity` `bookValue` `assessedValue` `varianceAmount` `varianceType` `disposition` `remark`（12 列；onEvent.change setValue `varianceQuantity = actualQuantity - bookQuantity` + `varianceAmount = assessedValue - bookValue`，scale=4 HALF_UP；行级校验 `actualQuantity >= 0`)
  - **ErpPrjCostCollectionLine sub-grid-edit**：`lineNo` `costCategory` `sourceBillType` `sourceBillCode` `subjectId` `taskId` `amount` `remark`（8 列；退化变体无 onEvent；行级校验 `amount >= 0`）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 ErpAstInventory 头表单嵌套 lines cell 兼容性：
  - **方案 A（采纳）**：直接在 `<form id="view">` + `<form id="edit">` 末尾追加 lines cell（实时仓库已核实 ErpAstInventory.view.xml 无既有嵌套结构或 tabs 容器）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 ErpMfgWorkOrder 头表单嵌套 lines cell 兼容性：
  - **方案 A（采纳）**：直接在 `<form id="view">` + `<form id="edit">` 末尾追加 lines cell（实时仓库已核实 `_gen/_ErpMfgWorkOrder.view.xml:156,182` 仅 2 layout 块，无 tabs 容器；mfg-chain spec 经 GraphQL 驱动不依赖 view.xml 结构）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 ErpPrjCostCollection 头表单嵌套 lines cell 兼容性：
  - **方案 A（采纳）**：直接在 `<form id="view">` + `<form id="edit">` 末尾追加 lines cell（同上范式）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 补齐 `ErpPrjProject.view.xml` `<grid id="pick-list">` 列集：`id` `code` `name` `projectTypeId` `status` `startDate` `endDate` + `<form id="pick-query">` filterForm（picker.page.yaml 已存在 codegen wrapper，本计划仅在 view.xml 补 pick-list + pick-query，对齐 P1 inventory ErpMdWarehouse 补齐范式；`projectTypeId` 是 FK 列名而非关系名 `projectType`，对齐 `ErpPrjProject.view.xml:11` 既有 grid 列）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 在 `docs/design/child-table-editor-patterns.md` 新增段落：
  - **§13 P2 mfg/assets/projects 3 头行对列集表**（基于实时仓库 ORM 字段）
  - **§14 减法变体扩展范式**（ErpAstInventoryLine `varianceQuantity = actualQuantity - bookQuantity` + `varianceAmount = assessedValue - bookValue`；与 P0 乘法变体 `amount = quantity × unitPrice` 并列）
  - **§15 ErpAstCip 不适用原因 + ErpAstInventory 替换裁决**（记录 ErpAstCip 无 `<to-many>` 到 CipCostItem 的实时仓库核实证据）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `docs/design/child-table-editor-patterns.md` 含 §13/§14/§15 新段落
- [ ] `ErpPrjProject.view.xml` 含非空 `<grid id="pick-list">` + `<form id="pick-query">`
- [ ] Phase 1 各 Decision 在 plan 内记录裁决（4 项）

### Phase 2 — 3 头实体 view.xml 嵌套子表 + 3 Line 实体 sub-grid-edit grid

Status: planned
Targets: 3 头实体 view.xml（form view/edit 嵌入 lines cell）+ 3 Line 实体 view.xml（sub-grid-edit + sub-grid-view grid）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy` (3/3 items tagged Add)
- Prereqs: Phase 1 决策冻结 + ErpPrjProject picker 补齐

- [ ] `Add`: 3 头实体 view.xml（ErpMfgWorkOrder + ErpAstInventory + ErpPrjCostCollection）`<form id="view">` + `<form id="edit">` `<layout>` 末尾追加 `=========>lines[明细行]======\n lines[明细行](2)` + 同步 `<cell id="lines"><view path="<LineEntity>/<LineEntity>.view.xml" grid="sub-grid-view"/></cell>`（form view）+ `<cell id="lines"><view path="<LineEntity>/<LineEntity>.view.xml" grid="sub-grid-edit"/></cell>`（form edit）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 3 Line 实体 view.xml 各新增 `<grid id="sub-grid-edit" x:prototype="list" editMode="list-edit">` + 平行 `<grid id="sub-grid-view" editMode="list-view">`，按 Phase 1 决策列集设计 `<cols>`：
  - 行内 picker 经 `<col><gen-control><c:script>` 返回 AMIS picker-service 控件（materialId/assetId/subjectId 等）
  - 行级校验经 `<col><validations>` 含 `minimum:0` 等约束
  - ErpAstInventoryLine 含 onEvent.change.setValue `varianceQuantity` + `varianceAmount`（减法变体）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 核实 3 头实体 `<form id="add">` 已通过 codegen 默认 `<form id="add" x:prototype="edit"/>` 自动继承 edit layout（含 lines cell）；若未继承则手写层显式 `<form id="add" x:prototype="edit"/>`（对齐 P1 inventory LandedCost 范式，无需重复嵌入）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 3 头实体 view.xml 均含 `<cell id="lines">` 嵌套 + layout 末尾 `>lines[明细行]` 分组
- [ ] 3 Line 实体 view.xml 均含 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">`
- [ ] 修改后运行 `mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS（codegen 重新展开 page.yaml 不报错）
- [ ] `mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` PAGE_ERROR_COUNT=0

### Phase 3 — Playwright 写路径 E2E 扩展

Status: planned
Targets: `tests/e2e/orchestration/mfg-chain.spec.ts`（扩展，内联断言 WorkOrderLine 子表写）+ `tests/e2e/crud/ast-inventory.write.spec.ts`（新建）+ `tests/e2e/crud/prj-cost-collection.write.spec.ts`（新建）
Skill: `nop-frontend-dev | nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 view.xml 接线完成

- [ ] `Add`: `tests/e2e/orchestration/mfg-chain.spec.ts` 扩展内联断言：runMfgChain 已创建 WorkOrder + WorkOrderLine 子表（经 GraphQL `__save` 嵌套行）→ 子表行数 + 行字段（materialId/lineType/plannedQuantity/sourceWarehouseId）持久化断言；如 runMfgChain 已含此断言，本计划仅核实并标记 Proof
  - Skill: `nop-testing`
- [ ] `Add`: 新建 `tests/e2e/crud/ast-inventory.write.spec.ts`：自包含 setup 创建 ErpAstInventory + ErpAstInventoryLine 行（assetId + bookQuantity + actualQuantity + 经 onEvent 自动推算 varianceQuantity + bookValue + assessedValue + 自动推算 varianceAmount）→ `__save` 嵌套行持久化 → 断言行数 + 行字段 + 自动推算结果（varianceQuantity = actualQuantity - bookQuantity + varianceAmount = assessedValue - bookValue）；行级校验负路径（actualQuantity < 0 抛守卫）
  - Skill: `nop-testing`
- [ ] `Add`: 新建 `tests/e2e/crud/prj-cost-collection.write.spec.ts`：自包含 setup 创建 ErpPrjCostCollection + ErpPrjCostCollectionLine 行（costCategory + subjectId + taskId + amount）→ `__save` 嵌套行持久化 → 断言行数 + 行字段；行级校验负路径（amount < 0 抛守卫）
  - Skill: `nop-testing`
- [ ] `Proof`: 新增/扩展 spec 全部 PASS（`npx playwright test mfg-chain ast-inventory.write prj-cost-collection.write`）
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 3 头行对子表写路径覆盖（mfg 复用既有 spec 内联断言 + assets/projects 各 1 新 spec）
- [ ] 新增/扩展 spec 全绿

## Draft Review Record

- Independent draft review iteration 1: needs-revision（ses_082a3ee48ffeLNlCQX3N0iW8Zk，2026-07-20）——发现 3 blocking issues：(B1) ErpAstCip ↔ CipCostItem 经 ORM 核实无 `<to-many>` 关系不可实施；(B2) 全部 3 Line 实体字段名编造（WorkOrderLine 无 quantity/standardCost/totalCost；CipCostItem 无 costElement/supplierId；CostCollectionLine 无 costType/supplierId）；(B3) Picker baseline 错误（ErpAstAsset picker 已存在非空；ErpPrjProject picker.page.yaml 存在仅 pick-list 空）。另发现 5 non-blocking issues（M1 anti-slack `如适用`；M3 缺 ErpAllWebPagesCollectTest；M4 WorkOrder Decision B 无依据；m1 `ErpMdProduct`/`ErpMfgProduct` 实体不存在；m2 item type 误标）。iteration 2 修订：assets 对替换为 ErpAstInventory/InventoryLine（含减法自动推算变体）；全部字段名经 ORM 实时核实重写；picker baseline 更正；移除 ErpMdProduct 引用；补 ErpAllWebPagesCollectTest 至 Closure Gates；移除全部 anti-slack hedge 词。
- Independent draft review iteration 2: accept-with-minor-comments（ses_0828d15e7ffeBzuyXcF4R9b7GE，2026-07-20）——B1/B2/B3 + M3/m1 全部 resolved；3 Line 实体字段集经 ORM 重新核实（mfg.orm.xml:673-684 + assets.orm.xml:1222-1249 + projects.orm.xml:537-545）；减法变体范式正确；ErpAstAsset picker baseline 准确；ErpAllWebPagesCollectTest 在 Phase 2 Exit + Closure Gates。minor n1（`projectType` 应为 `projectTypeId` FK 列名）+ M1 残留 `如需` 已修复。Plan Status 翻转为 active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次 `mvn clean install -DskipTests` + `mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` + `npx playwright test mfg-chain ast-inventory.write prj-cost-collection.write`。

- [ ] 范围内行为完成（3 P2 头行对 sub-grid-edit + sub-grid-view + picker + 自动推算（ErpAstInventoryLine 减法变体）+ 行级校验）
- [ ] 相关文档对齐（`docs/design/child-table-editor-patterns.md` §13/§14/§15 + 各域 ui-patterns.md 段落补充）
- [ ] 已运行验证（`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0 + `npx playwright test` 相关 spec 全绿）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### ErpAstCip + ErpAstCipCostItem 子表编辑（不适用范式）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经实时仓库 ORM 核实 ErpAstCip `<relations>` 仅含 4 to-one 关系（category/currency/org/completedAsset），**无 `<to-many>` 到 CipCostItem**；CipCostItem 经 `cipId` 反向指针 + `ErpAstCipBizModel.addCostItem(cipId, ...)` 单行 RPC API 设计（`ErpAstCipBizModel.java:45,70`），非聚合根嵌套；强行实施需 ORM 修改（保护区域 Non-Goal）；设计意图见 `docs/design/assets/cip.md:33,67,87`（CipCostItem 经跨域 hook `cipAssetId` 触发添加）；本计划用 ErpAstInventory/InventoryLine（标准 `<to-many name="lines">`）替换
- Successor Required: no（不适用范式，未来如需 CIP 头行嵌套须先 ORM 修改）

### ErpFinVoucher + ErpFinVoucherLine 子表编辑（finance）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap F4 §Phase 2 P1 表中的 finance 行；经审计 `ErpFinVoucherLine` 含 18 字段 + 9 辅助核算维度 + 借贷方向感知录入 + 头级借贷平衡校验；finance `ui-patterns.md §1` 明确要求科目树 picker（F10 范畴）+ 辅助核算列条件 visibleOn（F7 范畴）+ 凭证模板快速导入（F9 范畴）。ErpFinVoucher 独立 successor plan（依赖 F7 + F9 + F10 落地后启动，本批次 plan 1 + plan 2 完成后即可启动）
- Successor Required: yes

### P3 头行对（ext 8 域 ~36+ 对）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap F4 §Phase 2 P3 表；ext 8 域（crm/cs/hr/aps/logistics/b2b/contract/drp）各域 1-5 头行对预估 ~36 对；本计划仅取 P2 mfg/assets/projects 3 对作为范式代表，P3 全集独立 successor
- Successor Required: yes

### mfg/assets/projects 域其他头行对

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划每域仅取 1 对作为代表（mfg WorkOrder/Line + assets Inventory/InventoryLine + projects CostCollection/Line）；其他对（mfg Bom/Line + MaterialIssue/Line + SubcontractOrder/Line + MrpPlan/Line + Forecast/Line + CostRollup/Line；assets Split/Line + Merge/Line + Maintenance/MaintenanceCost via `costLines` relation；projects Budget/Line + Billing/Line + ProjectSettlement/Line + Project/Task）按相同范式补齐，触发条件「按域推进头行对子表编辑全覆盖」
- Successor Required: yes

### ErpMfgWorkOrder BOM/工序/成本 tabs 容器

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F12（页面结构 tabs/向导）范畴；mfg-chain E2E plan 已落地 WorkOrder + BOM/工序/成本多实体协作但仅作为 GraphQL 链路验证，前端 view.xml 未做 tabs 容器；本计划 WorkOrderLine 子表编辑控件可与未来 tabs 容器并存（tabs 一个 tab 内嵌入 sub-grid-edit）
- Successor Required: yes（F12 mfg WorkOrder plan 启动时与 WorkOrderLine sub-grid-edit 集成）

### ErpMfgBomOperation 工序行内编辑

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: BOM 工序是另一类聚合根（含工作中心 + 标准工时 + 工序依赖 DAG），非简单头行结构；属 F12 + 业务规则范畴
- Successor Required: no

## Closure

Status Note: 计划已通过独立草案审查 iteration 2 (accept-with-minor-comments，minor 已修复)，Plan Status 翻转为 active，可进入实施。
