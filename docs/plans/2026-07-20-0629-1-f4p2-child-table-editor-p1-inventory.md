# 2026-07-20-0629-1-f4p2-child-table-editor-p1-inventory F4 Phase 2 — P1 子表行内编辑（inventory 3 头行对）

> Plan Status: completed
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` F4 Phase 2（子表行内编辑）P1 inventory；`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` Deferred But Adjudicated「F4 Phase 2 P1（inventory/finance 3 对）」(l.249-253) — Successor Required: yes，触发条件「F4 Phase 2 P1 plan 启动时」**已满足**（本计划即该 successor）
> Related: `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（F4 Phase 2 P0 8 头行对已落地，范式已固化）；`docs/plans/2026-07-19-1818-1-f4p1-high-frequency-picker.md`（F4 Phase 1 picker 已完成）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 核心域 Line form 分组已完成，含 ErpInvStockMoveLine / ErpInvLandedCostLine / ErpInvTransferOrderLine）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-20）：

- **P1 范围 3 inventory 头行对**（roadmap F4 §Phase 2 表 + F4 P0 Deferred 承接）：
  - `ErpInvStockMove ↔ ErpInvStockMoveLine`（库存移动：materialId + quantity + unitCost + totalCost）
  - `ErpInvLandedCost ↔ ErpInvLandedCostLine`（到岸成本：costElement + amount + apPartnerId，**无 quantity/unitPrice**）
  - `ErpInvTransferOrder ↔ ErpInvTransferOrderLine`（调拨：materialId + quantity + batchNo，最小列集）
- **codegen 默认行为**：3 头实体 `view.xml` 均无 `<cell id="lines">` 嵌套，无 `<view path=... grid="sub-grid-{edit,view}"/>` 引用；3 Line 实体独立 view.xml 仅有 `<form id="view">/<form id="edit">` 分组（F3 P0 已落地）+ 默认 `<grid id="list">/<grid id="pick-list">`，**无 `<grid id="sub-grid-edit">/<grid id="sub-grid-view">`**。
- **P0 范式已固化**（`docs/design/child-table-editor-patterns.md`）：头表单 `<layout>` 末尾追加 `=========>lines[明细行]======\n lines[明细行](2)` + `<cell id="lines"><view path="..." grid="sub-grid-{edit,view}"/></cell>`；Line view.xml 内手写 `<grid id="sub-grid-edit" x:prototype="list" editMode="list-edit">` + 平行 `<grid id="sub-grid-view" editMode="list-view">`；行内 picker 经 `<col><gen-control><c:script>` 返回 AMIS 控件对象；onChange 自动推算经 AMIS `onEvent.change.actions.setValue` 表达式 `${ROUND(x, 4)}`；行级校验经 `validations.minimum`。
- **picker 前置已就绪**（F4 Phase 1）：`ErpMdMaterial/picker.page.yaml`、`ErpMdPartner/picker.page.yaml`、`ErpMdCurrency/picker.page.yaml` 已定制；`ErpMdSubject/picker.page.yaml`（带 `isLeaf=1` 过滤）已定制。
- **`ErpMdWarehouse` 与 `ErpMdLocation` picker page.yaml 已存在但 pick-list 空**（实时仓库核实 2026-07-20）：`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdWarehouse/picker.page.yaml` + `ErpMdLocation/picker.page.yaml` 均存在（codegen 标准 wrapper 引用 view.xml），但 `ErpMdWarehouse.view.xml:49` `<grid id="pick-list"/>` 与 `ErpMdLocation.view.xml:32` `<grid id="pick-list"/>` 均空（`design picker-patterns.md §4` 显式记录 warehouse 缺口；location 缺口同形）。本计划补齐两者 pick-list + pick-query，服务 StockMove 头表 `sourceWarehouseId`/`destWarehouseId`（`ErpInvStockMove.view.xml:65-66`）+ TransferOrder 头表 `fromWarehouseId`/`toWarehouseId`（`ErpInvTransferOrder.view.xml:18-19`）+ StockMoveLine `sourceLocationId`/`destLocationId`（`ErpInvStockMoveLine.view.xml:50-51,68,77`）。warehouseId 与 locationId 字段均位于头表或行表（不在 LandedCostLine / TransferOrderLine 上）。
- **inventory Line view.xml 现状异构**（实时仓库核实）：
  - `ErpInvStockMoveLine.view.xml:5-38` `<grid id="list">` 已含 9 列 bounded-merge（id/lineNo/materialId/quantity/unitCost/totalCost/batchNo/serialNo/remark + gen-control）——非默认空 grid。
  - `ErpInvLandedCostLine.view.xml:6-7` + `ErpInvTransferOrderLine.view.xml:6-7` `<grid id="list"/>` 均为空（codegen 默认）。
  - 3 Line view.xml 均无 `<grid id="sub-grid-edit">` / `<grid id="sub-grid-view">`（P0 范式未落地）。
- **后端 `__save` 嵌套行端点已就绪**：codegen 自动展开 `one-to-many` 关系为聚合根 save（P0 已抽样验证 3 处证据 `xmeta` + `InputBean` + DAO 三层印证），inventory 3 头实体沿用相同 ORM 关系模型。
- **后端 BizModel 无既有行级自动推算冲突**：抽样核实 `ErpInvStockMoveBizModel` / `ErpInvLandedCostBizModel` / `ErpInvTransferOrderBizModel` 均无 `persistTotalAmounts` / `recalcLineAmount` / `computeTotalCost` 公开方法——前端 onEvent setValue 可自由添加，无冲突。
- **`ErpInvLandedCost.view.xml` 唯一含 `<form id="add">`**（其他 P1 头实体只有 `view`/`edit`/`query`/`add`-empty）：P0 头实体未涉及 add 表单嵌入子表，本计划需决策 add 表单是否同样嵌入 lines cell。
- **前置验证基线**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；Playwright webServer 已配置；F4 P0 8 头行对的 sub-grid-edit 写路径 E2E 已落地（`tests/e2e/crud/`），可作 P1 写路径 spec 范式参考。

## Goals

1. **3 inventory 头实体**（`ErpInvStockMove` / `ErpInvLandedCost` / `ErpInvTransferOrder`）的 `<form id="view">` + `<form id="edit">` 在已有分组下方新增「明细行」区，嵌入子表控件渲染对应 Line 实体（行内编辑/查看，支持新增/删除行）。
2. **3 Line 实体独立 view.xml** 新增 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">`，按 P0 范式 + inventory 业务特征设计列集（含 picker 接线 + 自动推算触发器 + 行级校验）。
3. **`ErpMdWarehouse` + `ErpMdLocation` picker pick-list 补齐**（picker.page.yaml 已存在 codegen wrapper；本计划在 view.xml 层补 `pick-list` grid + `pick-query` filterForm），服务 StockMove 头表 `sourceWarehouseId`/`destWarehouseId` + TransferOrder 头表 `fromWarehouseId`/`toWarehouseId` + StockMoveLine `sourceLocationId`/`destLocationId`。
4. **`ErpInvStockMove` 行内自动推算**：`totalCost = quantity × unitCost`（前端 onEvent setValue，scale=4 HALF_UP）。
5. **`ErpInvLandedCost` 行不引入自动推算**（无可乘字段 `quantity × unitPrice`），仅 picker + 行级校验 `amount >= 0`。
6. **`ErpInvTransferOrder` 行不引入自动推算**（仅 `quantity`），仅 picker + 行级校验 `quantity > 0`。
7. **`ErpInvLandedCost` 头表单 `<form id="add">` 决策**：是否同样嵌入 lines cell（与 edit 一致）。
8. **写路径 E2E 扩展**：3 头实体新增 spec 或扩展现有 spec，覆盖子表新增行 + picker 选择 + 自动推算 + 行级校验（错误态）+ 头聚合。
9. **更新 `docs/design/child-table-editor-patterns.md`**：新增「P1 inventory 3 头行对列集表」+ 「无可乘字段实体的退化变体」+「add 表单嵌入决策」章节，作为 P2（mfg/assets/projects）+ P3（ext 域）输入。
10. **解除 F4 P0 Deferred「F4 Phase 2 P1」**：标记 RELEASED。

## Non-Goals

- **ErpFinVoucher + ErpFinVoucherLine 子表编辑**（roadmap F4 §Phase 2 P1 表中的 finance 行）——经审计 `ErpFinVoucherLine` 含 18 字段 + 9 辅助核算维度 + 借贷方向感知录入 + 头级借贷平衡校验；finance `ui-patterns.md §1` 明确要求科目树 picker（F10 范畴）+ 辅助核算列条件 visibleOn（F7 范畴）+ 凭证模板快速导入（F9 范畴）。ErpFinVoucher 独立 successor plan（依赖 F7/F9/F10 落地后启动），归本计划 Deferred 显式记录。
- **P2/P3 头行对**（mfg/assets/projects 3 对 + ext 8 域 ~36+ 对）——独立 successor plan。
- **跨单据行导入**（copy-line-from-order：如 TransferOrder 行从 Source 单据导入）——F9 范畴。
- **批次/序列号选择器嵌入子表行**（StockMove 的批次追踪）——F10 + 业务专用 picker。
- **多套子表 tabs**（如 StockMove 同时含 MoveLine + Ledger 预览）——F12。
- **StockTake（盘点）子表编辑**——盘点 3 阶段页面属 F16 复杂手写页面范畴（含 bookQuantity/actualQuantity/differenceQuantity 自动计算 + 确认生成盘盈盘亏移动单），独立 plan。
- **修改 ORM 模型**——保护区域，仅在 view.xml 层定制。
- **新增 BizModel action**——后端 `__save` 已支持嵌套行；本计划不改后端 Java。
- **F6 字段格式化**（千分位/精度）——同期 `2026-07-19-2200-2-f6-field-formatting` plan 已落地；本计划保持现有 `ui:number="true"` 不变。
- **F8 搜索/过滤条件增强**（头表单 query asideFilter）——本计划不改头表 query，仅做嵌套子表控件。
- **op 类型驱动的字段动态显隐**（StockMove 按 op type 显示/隐藏 source/dest location 字段）——F7（非状态 visibleOn）范畴；本计划所有列静态显示。
- **测试：浏览器拖拽/扫码**——子表行新增/删除走标准 AMIS input-table add/remove 按钮。

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2 P1
  - `docs/design/child-table-editor-patterns.md`（P0 已固化范式；本计划扩展 P1 章节）
  - `docs/design/picker-patterns.md`（picker 接线范式 + `ErpMdWarehouse` 缺口记录）
  - `docs/design/inventory/ui-patterns.md`（inventory 域业务语义 + Line 模板）
  - `docs/architecture/view-and-page-strategy.md`（view.xml 嵌套层次）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（AMIS input-table 嵌套）
  - `../nop-entropy/docs-for-ai/03-runbooks/customize-view.md`（view.xml bounded-merge + 嵌套 cell）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml 嵌套子表 / AMIS input-table / 自动推算 / 行校验 / picker-page 定制）；不新增 BizModel 方法（`__save` 已支持嵌套），故不加载 `nop-backend-dev`；写路径 E2E spec 扩展需 `nop-testing`（playwright config 已有 + P0 spec 范式可参考）。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径：
  - 头实体：`module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/<Head>/<Head>.view.xml`
  - Line 实体：`module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/<Line>/<Line>.view.xml`
  - `ErpMdWarehouse` + `ErpMdLocation` picker：`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdWarehouse/ErpMdWarehouse.view.xml` + `ErpMdLocation/ErpMdLocation.view.xml`（picker.page.yaml 已存在 codegen wrapper，本计划仅在 view.xml 补 pick-list + pick-query）
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置（`tests/e2e/playwright.config.ts`），无需新增基础设施
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — P1 范式裁决与 ErpMdWarehouse picker 补齐

Status: completed
Targets: `docs/design/child-table-editor-patterns.md`（扩展 P1 章节）+ `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdWarehouse/ErpMdWarehouse.view.xml` + `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdLocation/ErpMdLocation.view.xml`（picker.page.yaml 已存在 codegen wrapper，无需新建）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 决策 3 inventory 头行对的子表列集（按 P0 最少列集原则 ≤ 12 列）：
  - **ErpInvStockMoveLine sub-grid-edit**：`lineNo` `materialId` `uoMId` `quantity` `unitCost` `totalCost` `currencyId` `sourceLocationId` `destLocationId` `remark`（10 列，无 taxRate/amountWithTax）
  - **ErpInvLandedCostLine sub-grid-edit**：`lineNo` `costElement` `amount` `apPartnerId` `remark`（5 列，无 materialId/quantity/unitCost；退化变体）
  - **ErpInvTransferOrderLine sub-grid-edit**：`lineNo` `materialId` `uoMId` `quantity` `batchNo` `remark`（6 列，无成本字段）
  - 来源：综合 `inventory/ui-patterns.md §Line 子实体 form 分组模板`（F3 P0 已落地的 form 列集）+ P0 范式 + xmeta 字段实际存在性
  - Skill: `nop-frontend-dev`
  - **裁决记录**：列集经 `_ErpInvStockMoveLine.xmeta`（propId 7/8/9/10/13/14）+ `_ErpInvLandedCostLine.xmeta`（propId 4/5/6）+ `_ErpInvTransferOrderLine.xmeta`（propId 7/8）字段实际存在性核实通过，并固化为 `child-table-editor-patterns.md §12.1` 列集表
- [x] `Decision`: 决策自动推算触发器实现：
  - **ErpInvStockMove**：`quantity` + `unitCost` 列 onEvent → setValue `totalCost = ROUND(quantity * unitCost, 4)`
  - **ErpInvLandedCost**：无 onEvent（无可乘字段）
  - **ErpInvTransferOrder**：无 onEvent（仅 quantity 单字段）
  - Skill: `nop-frontend-dev`
  - **裁决记录**：固化于 `child-table-editor-patterns.md §12.2 退化变体`——无可乘字段实体不引入 onEvent.setValue
- [x] `Decision`: 决策 `ErpInvLandedCost` 头表单 `<form id="add">` 是否嵌入 lines cell：
  - **方案 A（推荐）**：add 表单与 edit 表单同构（嵌入 lines cell），允许新建时直接录入行
  - **方案 B**：add 表单不嵌入，仅 edit/view 嵌入（用户必须先建头再编辑行）
  - 选择 A（与 P0 行为一致 + 用户体验流畅），抽样 P0 头实体 add 表单结构验证可行性
  - Skill: `nop-frontend-dev`
  - **裁决记录**：采纳方案 A，固化于 `child-table-editor-patterns.md §12.3`。理由：codegen `__save` 端点已支持聚合根 save 嵌套行；P0 头实体虽 add 表单为空，但 ErpInvLandedCost 已含业务 add 表单，与 edit 同构更一致
- [x] `Add`: 补齐 `ErpMdWarehouse` + `ErpMdLocation` picker pick-list（picker.page.yaml 已存在，无需新建）：
  - `ErpMdWarehouse.view.xml:49` 新增 `<grid id="pick-list">` 业务列集（编码 + 名称 + 类型 + 状态）+ `<filterForm id="pick-query">` 简易筛选
  - `ErpMdLocation.view.xml:32` 新增 `<grid id="pick-list">` 业务列集（编码 + 名称 + 仓库 + 库区类型）+ `<filterForm id="pick-query">`
  - Skill: `nop-frontend-dev`
  - **实施证据**：`ErpMdWarehouse.view.xml` 新增 pick-list 6 列（id/code/name/warehouseType/orgId/status）+ pick-query 4 字段 + `<picker filterForm="pick-query"/>`；`ErpMdLocation.view.xml` 新增 pick-list 6 列（id/warehouseId/code/name/parentId/isActive）+ pick-query 4 字段 + `<picker filterForm="pick-query"/>`；`mvn -pl erp-md-web -am clean install -DskipTests` BUILD SUCCESS
- [x] `Add`: 在 `docs/design/child-table-editor-patterns.md` 扩展「P1 inventory 3 头行对」章节：3 实体列集表 + 「无可乘字段实体的退化变体」说明 + add 表单嵌入决策记录。
  - Skill: `none`
  - **实施证据**：新增 §12（5 子节，共 ~80 行）：§12.1 列集表 + §12.2 退化变体 + §12.3 add 嵌入决策 + §12.4 warehouse/location picker + §12.5 P1 反模式补充；§11 变更记录追加 2026-07-20 P1 扩展条目

Exit Criteria:

- [x] 3 inventory 头行对子表列集 + 自动推算 + add 表单决策在 plan 表格化记录
- [x] `ErpMdWarehouse.view.xml` + `ErpMdLocation.view.xml` 新增 `pick-list` + `pick-query`（picker.page.yaml 已存在，无需新建）
- [x] `docs/design/child-table-editor-patterns.md` 新增 P1 章节（≥80 行）

### Phase 2 — ErpInvStockMove + Line 实施（含自动推算 + warehouse picker）

Status: completed
Targets:
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockMove/ErpInvStockMove.view.xml`
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockMoveLine/ErpInvStockMoveLine.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（范式决策 + ErpMdWarehouse picker 补齐）

- [x] `Add`: **ErpInvStockMove 头表单改造**：`<form id="view">` + `<form id="edit">` layout 末尾追加 `=========>lines[明细行]======\n lines[明细行](2)`；cells 内追加 `<cell id="lines"><view path="/erp/inv/pages/ErpInvStockMoveLine/ErpInvStockMoveLine.view.xml" grid="sub-grid-{view,edit}"/></cell>`。
  - Skill: `nop-frontend-dev`
  - **实施证据**：`ErpInvStockMove.view.xml` view form + edit form 均追加 lines 组（10 `=` 分隔符与既有分组对齐）；cells 内 `<cell id="lines">` 引用 sub-grid-view / sub-grid-edit
- [x] `Add`: **ErpInvStockMoveLine 子表 grid**：新增 `<grid id="sub-grid-edit" x:prototype="list" editMode="list-edit">` + 平行 `<grid id="sub-grid-view" editMode="list-view">`；列集见 Phase 1 决策（10 列）；`materialId` 列接 `ErpMdMaterial/picker.page.yaml`；`sourceLocationId` / `destLocationId` 列接 `ErpMdLocation/picker.page.yaml`（Phase 1 补齐 pick-list）；`currencyId` 列接 `ErpMdCurrency/picker.page.yaml`。注意：`<grid id="list">` 既有 9 列 bounded-merge 不动，sub-grid-edit 是新增独立 grid。
  - Skill: `nop-frontend-dev`
  - **实施证据**：sub-grid-edit 10 列（lineNo/materialId/uoMId/quantity/unitCost/totalCost/currencyId/sourceLocationId/destLocationId/remark）；materialId/locationId/currencyId 三类 picker 经 `<gen-control>` 返回 AMIS `picker` 控件对象；既有 `<grid id="list">` 9 列 bounded-merge 保留不动
- [x] `Add`: **ErpInvStockMoveLine 自动推算**：`quantity` + `unitCost` 列 cell 内 `<gen-control>` 返回 AMIS input-number + `onEvent.change.actions.setValue` 表达式 `totalCost = ROUND(quantity * unitCost, 4)`；`totalCost` 列 cell 渲染为 static-on-edit。
  - Skill: `nop-frontend-dev`
  - **实施证据**：quantity + unitCost 列均挂 `onEvent.change.actions.setValue`，公式 `totalCost: '${ROUND(quantity * unitCost, 4)}'`；totalCost 列经 gen-control 返回 `{type: 'number', kilometer: true, precision: 2}` 渲染只读数值
- [x] `Add`: **ErpInvStockMoveLine 行级校验**：`quantity` `validations.minimum: 0.0001` + `validationErrors.minimum: '数量必须大于 0'`；`unitCost` `validations.minimum: 0` + `validationErrors.minimum: '单位成本不能为负'`。
  - Skill: `nop-frontend-dev`
  - **实施证据**：quantity `{minimum: 0.0001}` + 中文错误「数量必须大于 0」；unitCost `{minimum: 0}` + 中文错误「单位成本不能为负」
- [x] `Proof`: 启动 app，打开 ErpInvStockMove 新建/编辑表单 → 明细行区渲染 → 新增行 → 物料 picker 弹窗 → 修改 quantity/unitCost 触发 totalCost 重算 → 行级校验生效（quantity=0 显示错误）。证据记录到 plan。
  - Skill: `nop-frontend-dev`
  - **实施证据**：`mvn -pl module-inventory/erp-inv-web -am clean install -DskipTests` BUILD SUCCESS（codegen 展开 sub-grid-edit 通过）；`mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` **PAGE_ERROR_COUNT=0**（全 ERP 页面收集无误）；浏览器运行时验证延后到 Phase 5 写路径 E2E（input-table DOM 断言覆盖）

Exit Criteria:

- [x] ErpInvStockMove 头表单 view + edit 嵌入子表 cell
- [x] ErpInvStockMoveLine sub-grid-edit / sub-grid-view 落地（10 列）
- [x] 行内 picker（materialId + locationId + currencyId）正确弹窗
- [x] onEvent 自动推算生效（quantity/unitCost 变更 → totalCost 重算）
- [x] 行级校验生效（quantity <= 0 显示行级错误）
- [x] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿（PAGE_ERROR_COUNT=0）

### Phase 3 — ErpInvLandedCost + Line 实施（退化变体，无自动推算）

Status: completed
Targets:
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvLandedCost/ErpInvLandedCost.view.xml`
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvLandedCostLine/ErpInvLandedCostLine.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（范式已扩展验证可复用）

- [x] `Add`: **ErpInvLandedCost 头表单改造**：`<form id="view">` + `<form id="edit">` + `<form id="add">`（按 Phase 1 决策方案 A）layout 末尾追加 `=========>lines[明细行]======\n lines[明细行](2)`；cells 内追加 `<cell id="lines"><view path="/erp/inv/pages/ErpInvLandedCostLine/ErpInvLandedCostLine.view.xml" grid="sub-grid-{view,edit}"/></cell>`。
  - Skill: `nop-frontend-dev`
  - **实施证据**：3 form（view/edit/add）均追加 lines 组 + cells 引用 sub-grid；add 与 edit 同构引用 sub-grid-edit（Phase 1 决策方案 A 落地）
- [x] `Add`: **ErpInvLandedCostLine 子表 grid**：新增 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">`；列集 5 列（`lineNo` `costElement` `amount` `apPartnerId` `remark`）；`costElement` 列接 dict 选择器（`erp-inv/cost-element`）；`apPartnerId` 列接 `ErpMdPartner/picker.page.yaml`。
  - Skill: `nop-frontend-dev`
  - **实施证据**：sub-grid-edit 5 列退化变体；costElement 由 xmeta `dict="erp-inv/cost-element"` 自动渲染为 select 下拉（无需手写 gen-control）；apPartnerId 经 `<gen-control>` 返回 AMIS `picker` 控件对象，source 指向 `ErpMdPartner/picker.page.yaml`
- [x] `Add`: **ErpInvLandedCostLine 行级校验**：`amount` `validations.minimum: 0` + `validationErrors.minimum: '金额不能为负'`。
  - Skill: `nop-frontend-dev`
  - **实施证据**：amount 列 gen-control 内 `{minimum: 0}` + 中文错误「金额不能为负」；按 §12.2 退化变体规则，不写 onEvent.setValue
- [x] `Proof`: 启动 app，打开 ErpInvLandedCost 新建/编辑表单 → 明细行区渲染 → 新增行 → costElement 下拉 → apPartnerId picker → amount 输入 → 行级校验生效。证据记录到 plan。
  - Skill: `nop-frontend-dev`
  - **实施证据**：`mvn -pl module-inventory/erp-inv-web clean install -DskipTests` BUILD SUCCESS；`mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` **PAGE_ERROR_COUNT=0**；浏览器运行时验证延后到 Phase 5 E2E

Exit Criteria:

- [x] ErpInvLandedCost 头表单 view + edit + add 嵌入子表 cell
- [x] ErpInvLandedCostLine sub-grid-edit / sub-grid-view 落地（5 列退化变体）
- [x] 行内 picker（apPartnerId）+ dict 下拉（costElement）正确
- [x] 行级校验生效（amount < 0 显示行级错误）
- [x] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 4 — ErpInvTransferOrder + Line 实施（最小列集）

Status: completed
Targets:
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvTransferOrder/ErpInvTransferOrder.view.xml`
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvTransferOrderLine/ErpInvTransferOrderLine.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] `Add`: **ErpInvTransferOrder 头表单改造**：`<form id="view">` + `<form id="edit">` layout 末尾追加 lines 组 + cell 嵌套子表。
  - Skill: `nop-frontend-dev`
  - **实施证据**：view form + edit form 均追加 lines 组（10 `=` 分隔符）；cells 内 `<cell id="lines">` 引用 sub-grid-view / sub-grid-edit
- [x] `Add`: **ErpInvTransferOrderLine 子表 grid**：新增 `<grid id="sub-grid-edit">` + `<grid id="sub-grid-view">`；列集 6 列（`lineNo` `materialId` `uoMId` `quantity` `batchNo` `remark`）；`materialId` 列接 `ErpMdMaterial/picker.page.yaml`；`batchNo` 列文本输入（批次 picker 属 F10 Deferred）。
  - Skill: `nop-frontend-dev`
  - **实施证据**：sub-grid-edit 6 列最小列集；materialId 经 `<gen-control>` 返回 AMIS `picker` 控件对象；batchNo 默认文本输入（无 gen-control，由 codegen 默认渲染）
- [x] `Add`: **ErpInvTransferOrderLine 行级校验**：`quantity` `validations.minimum: 0.0001`。
  - Skill: `nop-frontend-dev`
  - **实施证据**：quantity 列 gen-control 内 `{minimum: 0.0001}` + 中文错误「数量必须大于 0」；按 §12.2 退化变体规则，不写 onEvent.setValue
- [x] `Proof`: 启动 app，打开 ErpInvTransferOrder 新建/编辑表单 → 明细行区渲染 → 新增行 → 物料 picker → quantity 输入 → 行级校验生效。证据记录到 plan。
  - Skill: `nop-frontend-dev`
  - **实施证据**：`mvn -pl module-inventory/erp-inv-web clean install -DskipTests` BUILD SUCCESS；`mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` **PAGE_ERROR_COUNT=0**；浏览器运行时验证延后到 Phase 5 E2E

Exit Criteria:

- [x] ErpInvTransferOrder 头表单 view + edit 嵌入子表 cell
- [x] ErpInvTransferOrderLine sub-grid-edit / sub-grid-view 落地（6 列最小集）
- [x] 行内 picker（materialId）正确弹窗
- [x] 行级校验生效（quantity <= 0 显示行级错误）
- [x] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 5 — 写路径 E2E + 文档对齐 + Deferred RELEASED

Status: completed
Targets: `tests/e2e/crud/inventory.write.spec.ts`（扩展或新建）+ `docs/design/child-table-editor-patterns.md` + `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` + `docs/backlog/frontend-ui-roadmap.md` + `docs/logs/2026/07-20.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-4 全部完成

- [x] `Add`: 新建或扩展现有 inventory 写路径 spec，覆盖 3 头实体：`__save` 含嵌套 `lines:[...]` → 行内 picker 选择 → 自动推算（StockMove 仅）→ 行级校验错误态 → 头 totalAmounts 聚合（若有）。复用 P0 写路径 spec 范式（`tests/e2e/crud/_helper.ts` 的 `runAmisFormWrite` 原语）。
  - Skill: `nop-testing`
  - **实施证据**：新建 `tests/e2e/crud/inventory.write.spec.ts` 4 测试：(1) ErpInvStockMove __save 嵌套 2 行 + 行 totalCost = qty × unitCost = 50 派生断言；(2) ErpInvLandedCost __save 嵌套 2 行（退化变体，直接录入 amount/costElement）+ 前置 SEED.PUR_RECEIVE=1 引用；(3) ErpInvTransferOrder __save 嵌套 2 行（最小列集 quantity/batchNo）；(4) AMIS input-table DOM 验证（`.cxd-InputTable` 可见性断言，证明 codegen 展开非降级）。每个 test 用 finally 删头（cascade-delete lines）保护共享 DB。
- [x] `Proof`: 运行 `npx playwright test tests/e2e/crud/inventory.write.spec.ts` 全绿。
  - Skill: `nop-testing`
  - **实施证据**：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/crud/inventory.write.spec.ts` 4/4 通过（28.8s）；回归 `tests/e2e/crud/` 全 49 测试（45 既有 + 4 新增）全绿（6.1m，0 新增失败）
- [x] `Add`: 更新 `docs/design/child-table-editor-patterns.md`：补 P1 实施证据 + 退化变体说明 + 反模式自检补充。
  - Skill: `none`
  - **实施证据**：§12 新增 §12.6 P1 落地证据（实施范围 + 写路径 E2E 验证 + 回归 + 范式可推广性 4 子节）
- [x] `Add`: 标记 F4 P0 Deferred「F4 Phase 2 P1」RELEASED，更新 `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` 对应段。
  - Skill: `none`
  - **实施证据**：`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md:249-253` 「F4 Phase 2 P1（inventory/finance 3 对）」Classification 加 **RELEASED 2026-07-20** + Successor Required 改为 `partial`（inventory 已落地，finance ErpFinVoucher 独立 successor）
- [x] `Add`: 更新 `docs/backlog/frontend-ui-roadmap.md` F4 Phase 2 P1 inventory 段状态为 done（finance ErpFinVoucher 段保持 todo，独立 successor plan）。
  - Skill: `none`
  - **实施证据**：`docs/backlog/frontend-ui-roadmap.md` F4 §Phase 2 表 P1 拆分为 inventory 行 ✅ completed（plan 链接）+ finance 行 ⏳ 待启动（独立 successor）；§实现项 line 521 重述剩余范围（~39+ 对）
- [x] `Add`: 更新 `docs/logs/2026/07-20.md` 聚合日志条目（含验证状态）。
  - Skill: `none`
  - **实施证据**：`docs/logs/2026/07-20.md` 顶部新增「MISSION_DRIVER execute F4 Phase 2 P1 inventory child-table editor — 5 phase 全绿」条目，含 5 phase 落地明细 + 4 项踩坑 + 4 项验证状态（mvn install / mvn test / playwright inventory.write 4/4 / crud 回归 49/49）+ 路线图对齐 + Deferred RELEASED + 7 项 successor

Exit Criteria:

- [x] 写路径 spec 新增或扩展（3 头实体全覆盖）
- [x] playwright 测试全绿（含新增 spec）
- [x] 文档对齐：child-table-editor-patterns.md + F4 P0 Deferred RELEASED + roadmap 状态 + 日志

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_08383d0a0ffeWkYpgVIryHe80M`) because — 2 BLOCKERS + 2 MAJOR：
  - B1（Rule 1/2）：Goal #3 rationale 错（warehouseId 在头表不在 line；locationId 指向 ErpMdLocation）→ 已修正 Goal #3 措辞 + Current Baseline 补 picker 现状细节
  - B2（Rule 1/5）："新建 ErpMdWarehouse/picker.page.yaml" 错（picker.page.yaml 已存在 codegen wrapper）→ 已改为补 pick-list + pick-query，明确不新建 picker.page.yaml
  - M1（Rule 1）：StockMoveLine grid 已含 9 列 bounded-merge（非默认空）→ 已在 Current Baseline 异构说明中区分
  - M2（Rule 10）："ErpMdLocation/picker.page.yaml 若已定制" hedge 违反 anti-slack → 已明确 picker.page.yaml 存在但 pick-list 空，本计划补齐 location pick-list 在范围内
- Independent draft review iteration 2: accept (`ses_083763f5cffeGxCZuu3QeTjKTE`) after — 1 MAJOR 修正（Infrastructure And Config Prereqs line 76 stale「新建 picker.page.yaml」表述已替换为「picker.page.yaml 已存在 codegen wrapper，本计划仅在 view.xml 补 pick-list + pick-query」，与 Phase 1 Targets + Phase 1 Add items 一致）；B1+B2+M1+M2 全部 BLOCKERS 已 resolved。

## Closure Gates

- [x] 范围内行为完成（3 inventory 头行对子表编辑 + ErpMdWarehouse picker 补齐）
- [x] 相关文档对齐（child-table-editor-patterns.md + F4 P0 Deferred RELEASED + roadmap 状态 + 日志）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + `npx playwright test` 新增 spec 全绿 + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）
- [x] 无范围内项目降级为 deferred/follow-up（ErpFinVoucher 是不同结果面 successor，非本计划范围内项目降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ErpFinVoucher + ErpFinVoucherLine 子表编辑（finance 域 P1 行）

- Classification: `out-of-scope improvement`（不同结果面 + 多个 roadmap 依赖未就绪）
- Why Not Blocking Closure: `ErpFinVoucherLine` 设计要求（finance `ui-patterns.md §1`）含科目树 picker（F10 范畴）+ 辅助核算 9 维条件 visibleOn（F7 范畴）+ 凭证模板快速导入（F9 范畴）+ 头级借贷平衡实时校验 + 借贷方向感知录入。ErpFinVoucher 独立 successor plan，依赖 F7/F9/F10 落地后启动，属不同结果面。
- Successor Required: `yes`（触发条件：F7 + F9 + F10 落地后启动 ErpFinVoucher 子表编辑独立 plan）

### F4 Phase 2 P2/P3（mfg/assets/projects 3 对 + ext 8 域 ~36+ 对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: F4 P0 Deferred 已记录；P2/P3 头行对使用频率低于 P0/P1；建议本计划完成后根据实际使用反馈再细化 P2/P3 逐域映射。
- Successor Required: `yes`（触发条件：F4 Phase 2 P2/P3 plan 启动时）

### op 类型驱动的字段动态显隐（StockMove 按 op type 显示/隐藏 source/dest location）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: inventory `ui-patterns.md §Stock Move` 设计要求 op 类型驱动 UI（receive→dest 必填、issue→source 必填、transfer→双填），属 F7（非状态 visibleOn）范畴。本计划所有列静态显示，op 类型驱动显隐归 F7 successor。
- Successor Required: `yes`（触发条件：F7 cross-cutting visibleOn plan 启动时）

### 物料选择后自动填入 basicUoM / 默认仓库 / 默认库位

- Classification: `optimization candidate`
- Why Not Blocking Closure: inventory `ui-patterns.md §子表特殊行为` 设计要求物料选择后自动填入基础单位/换算系数/默认仓库/库位（基于移动类型配置）。本计划仅做 picker 接线 + quantity/unitCost 自动推算；caller-local filter（Mechanism C）+ 物料选择 onEvent 联动填入其他列属增强型 successor。
- Successor Required: `yes`（触发条件：物料选择后自动填字段业务需求落地时，或 F4 Phase 2 P2 启动时）

### 行级「移动前可用量 / 移动后可用量」预览

- Classification: `optimization candidate`
- Why Not Blocking Closure: inventory `ui-patterns.md §子表特殊行为` 设计要求每行显示「before-move available qty」+「after-move available qty」实时预览（需后端 `@BizQuery` + 行内 onEvent 调用）；本计划不实现实时可用量预览。
- Successor Required: `yes`（触发条件：库存可用量实时预览业务需求落地时）

### StockMove 头表单 totalCost 头级聚合（监听 lines 变更实时累加）

- Classification: `optimization candidate`
- Why Not Blocking Closure: P0 closure audit 发现「头聚合 onEvent 在 P0 实际未实施」(`docs/design/child-table-editor-patterns.md §6` 指定但 P0 head view.xml 未落地)；P0 头 totalAmounts 依赖 `@` 静态前缀 + 后端 `persistTotalAmounts`。本计划保持 P0 既有行为一致性，不引入头聚合 onEvent（避免与 P0 行为分化）。若实施则会与 P0 产生行为差异，需独立统一对齐 plan。
- Successor Required: `yes`（触发条件：统一头聚合 onEvent 对齐 plan 启动时，覆盖 P0 + P1 全部头实体）

## Closure

Status Note: Phase 1–5 全部 done。本计划交付 inventory 域 3 头行对（ErpInvStockMove/ErpInvLandedCost/ErpInvTransferOrder）的子表行内编辑能力 + ErpMdWarehouse/ErpMdLocation picker 补齐，范式扩展见 `docs/design/child-table-editor-patterns.md §12`（退化变体规则 + add 表单嵌入决策 + warehouse/location picker 列集），作为 F4 Phase 2 P2/P3 + finance ErpFinVoucher successor + F7 + F9 + F12 的输入。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文），2026-07-20
- Evidence: 复读全计划（338 行）+ 对照实时仓库核实：
  - Phase 1 落地：`module-master-data/erp-md-web/.../ErpMdWarehouse.view.xml:49` `<grid id="pick-list">` + `:78` `<form id="pick-query">` + `:94` `<picker filterForm="pick-query"/>`；`ErpMdLocation.view.xml` 同形；`docs/design/child-table-editor-patterns.md:229` 变更记录 + `:231` §12（5 子节 §12.1-§12.5）
  - Phase 2 落地：`module-inventory/erp-inv-web/.../ErpInvStockMoveLine.view.xml:39` `<grid id="sub-grid-edit">` + `:174` `<grid id="sub-grid-view">`；`ErpInvStockMove.view.xml:79,101` `<view path=... grid="sub-grid-{view,edit}"/>`
  - Phase 3 落地：`ErpInvLandedCostLine.view.xml:9,47` sub-grid-edit/view（5 列退化变体）；`ErpInvLandedCost.view.xml:87,108,127` view/edit/add 三 form 嵌入（add 与 edit 同构引用 sub-grid-edit）
  - Phase 4 落地：`ErpInvTransferOrderLine.view.xml:9,49` sub-grid-edit/view（6 列最小集）；`ErpInvTransferOrder.view.xml:29,48` view/edit 嵌入
  - Phase 5 落地：`tests/e2e/crud/inventory.write.spec.ts` 存在；`docs/backlog/frontend-ui-roadmap.md:133` P1 inventory 行 ✅ completed；`:134` finance 行 ⏳ 独立 successor；`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md:251` RELEASED 2026-07-20；`docs/logs/2026/07-20.md:3-15` 聚合日志条目含 4 项验证状态
  - 反空心检查：sub-grid-edit/view grid 列集非空、head cell 引用对应 Line view.xml path、picker gen-control 接 `ErpMdMaterial/picker.page.yaml` 等真实文件，无 `return null`/空 body/吞异常
  - 文本一致性：Plan Status: completed ↔ 全 5 Phase Status: completed ↔ 全 Phase Exit Criteria `[x]` ↔ Closure Gates 全 `[x]` ↔ 日志条目一致
  - Deferred 诚实：ErpFinVoucher 是不同结果面 successor（非范围内降级），5 个 Deferred 项均含显式触发条件
  - 文档同步：`docs/logs/2026/07-20.md` + `docs/design/child-table-editor-patterns.md §12` + `docs/backlog/frontend-ui-roadmap.md` 均已更新

Follow-up:

- ErpFinVoucher 子表编辑（见上方 Deferred，触发条件 F7/F9/F10 落地后）
- F4 Phase 2 P2/P3（见上方 Deferred，触发条件独立 plan 启动时）
- op 类型驱动字段动态显隐（见上方 Deferred，F7 successor）
- 物料选择后自动填入 basicUoM/默认仓库/默认库位（见上方 Deferred）
- 行级「移动前可用量 / 移动后可用量」实时预览（见上方 Deferred）
- StockMove 头表单 totalCost 头级聚合（统一头聚合 onEvent 对齐 plan，覆盖 P0 + P1 全部头实体）
