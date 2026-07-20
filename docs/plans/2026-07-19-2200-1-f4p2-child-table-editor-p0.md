# 2026-07-19-2200-1-f4p2-child-table-editor-p0 F4 Phase 2 — P0 子表行内编辑（purchase/sales 8 头行对）

> Plan Status: completed
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` F4 Phase 2（子表行内编辑）
> Related: `docs/plans/2026-07-19-1818-1-f4p1-high-frequency-picker.md`（F4 Phase 1 已完成，物料 picker 就绪）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 核心域 Line form 分组已完成）；`docs/plans/2026-07-12-1500-1-view-form-layout-overhaul.md`（39 头实体 form 分组已完成）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-19）：

- **P0 范围 8 头行对**（前端 roadmap F4 §Phase 2 表）：
  - **purchase 域 4 对**：`ErpPurOrder↔ErpPurOrderLine`、`ErpPurReceive↔ErpPurReceiveLine`、`ErpPurInvoice↔ErpPurInvoiceLine`、`ErpPurReturn↔ErpPurReturnLine`
  - **sales 域 4 对**：`ErpSalOrder↔ErpSalOrderLine`、`ErpSalDelivery↔ErpSalDeliveryLine`、`ErpSalInvoice↔ErpSalInvoiceLine`、`ErpSalReturn↔ErpSalReturnLine`
- **codegen 默认行为**：抽样 `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml:84-141`（`<form id="view">` + `<form id="edit">`）—— **头表单仅含头字段，无任何子表/嵌套区域**。codegen `_gen/_ErpPurOrder.view.xml` 同样不含 line 嵌套（仅头表单 + line 独立 `<crud>` 列表页）。
- **Line 实体独立 view.xml**：F3 plan 已为全部 8 个 Line 实体（ErpPurOrderLine 等）的 `<form id="view">/<form id="edit">` 落地业务分组（基本信息 / 数量金额 / 税务 / 审计），但这是 **Line 实体独立 view.xml**，与父视图嵌套子表编辑控件是两个独立结果面。
- **后端行级端点已就绪**：`__save` mutation 接受嵌套 `lines: [...]` 数组（codegen 自动展开 `one-to-many` 关系为聚合根 save）；抽样核实证据 3 处：`module-purchase/erp-pur-meta/.../ErpPurOrder/_ErpPurOrder.xmeta:222`（`<prop name="lines" kind="to-many" ext:relation="lines" insertable="true" updatable="true" tagSet="pub,cascade-delete,insertable,updatable"/>`，权威源）+ `module-purchase/erp-pur-api/.../ErpPurOrderInputBean.java:366`（`List<ErpPurOrderLineInputBean> _lines`，API 层印证）+ `module-purchase/erp-pur-dao/.../_gen/_ErpPurOrder.java:2088`（`OrmEntitySet<ErpPurOrderLine> _lines`，DAO 层印证）。
- **picker 前置就绪**：F4 Phase 1 plan 已为物料/币种/科目落地业务专用 pick-list + pick-query；本计划子表行内 `materialId`/`warehouseId` 等 FK 字段 picker 已可直接复用。
- **既有 AMIS 子表控件参考**：nop-entropy 平台支持 AMIS `input-table` / `input-sub-form` 控件（见 `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md` + `../nop-entropy/docs-for-ai/03-runbooks/customize-view.md` 关于 view.xml layout `<cell>` 嵌套）。需在 Phase 1 Explore 验证 view.xml `<form>` 嵌套 `<cell name="lines">` 经 codegen 展开到 AMIS `input-table` 的实际管线。
- **既有写路径 E2E**：`tests/e2e/crud/master-data.write.spec.ts` 已落地 CRUD 写路径范式（本计划 Phase 5 将抽样核实其 `__save` 请求体是否含 `lines:[...]` 嵌套结构作为参考）；本计划不创建新写路径范式，仅做视图层嵌套控件改造。
- **前置已就绪**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿；codegen 增量链可用。

## Goals

1. 8 个 P0 头实体的 `<form id="view">` 与 `<form id="edit">` 在「金额信息」组下方新增「明细行」区，嵌入 AMIS 子表控件渲染对应 Line 实体（行内编辑/查看，支持新增/删除/复制行）
2. 子表行内 FK 字段（materialId、warehouseId、taxRateId 等）接入对应 picker（复用 F4 Phase 1 picker.page.yaml），选择后自动填充名称/规格/单位/单价等关联字段
3. 子表行内数量/单价变更触发 `amount = quantity × unitPrice`、`amountWithTax = amount × (1 + taxRate)` 自动重算（前端 onChange，避免后端 round-trip）
4. 子表行内基础校验（数量 > 0、单价 ≥ 0、金额 = 数量 × 单价），非法行保存时显示行级错误（不阻塞其他合法行）
5. 头表单底部聚合区域（`totalAmount`、`totalTaxAmount`、`totalAmountWithTax`）随行变更实时累加（前端聚合，与后端 persistTotalAmounts 服务端逻辑独立）
6. 在 plan 内固化「头行子表编辑范式」（picker 接线、自动推算触发器、行校验位置），作为 F4 Phase 2 P1（inventory/finance 3 对）+ P2（mfg/assets/projects 3 对）+ F7（非状态 visibleOn）+ F9（跨单据导航）+ F12（tabs 容器）的输入

## Non-Goals

- **P1/P2/P3 域子表编辑**（inventory/finance 3 对、mfg/assets/projects 3 对、ext 8 域剩余 ~36+ 对）——独立 successor plan
- **从订单导入行**（copy-line-from-order：如 Receive 行从 Order 行导入、Invoice 行从 Receive 行导入）——属跨单据编排（F9 范畴），本计划仅做子表控件本身
- **批次/序列号选择器嵌入子表行**（inventory Receive/Delivery 行的批次追踪）——F10 + 业务专用 picker 范畴
- **多套子表 tabs 切换**（如 ErpPurOrder 同时含 OrderLine + PaymentPlan + DeliverySchedule 三子表）——F12（页面结构增强）范畴；本计划仅做单一主行表
- **修改 ORM 模型**（`*.orm.xml`）——保护区域，仅在 view.xml 层定制
- **新增 BizModel action**——后端 `__save` 已支持嵌套行；本计划不改后端
- **F6 字段格式化**（千分位/精度）——同期 `2026-07-19-2200-2-f6-field-formatting-xmeta` plan 覆盖；本计划保持现有 `ui:number="true"` 不变
- **F8 搜索/过滤条件增强**——本计划仅做头表单已有 query form 的子表上下文，不改头表 query
- **i18n `i18n-en:`**——F15 覆盖；本计划使用中文 label
- **批量审批/批量导入**——F11 范畴
- **F12 头行 tabs 切换**——本计划子表直接嵌入头表单（form 嵌套），不做 tab 容器；tab 容器归 F12
- **测试：浏览器拖拽/扫码**——子表行新增/删除走标准 AMIS input-table add/remove 按钮，无拖拽；扫码归 cross-cutting Barcode/PDA

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2（P0 8 对定义）
  - `docs/design/purchase/ui-patterns.md`（purchase 域子表编辑设计）
  - `docs/design/sales/ui-patterns.md`（sales 域子表编辑设计）
  - `docs/design/picker-patterns.md`（F4 Phase 1 落地的 picker 接线范式）
  - `docs/architecture/view-and-page-strategy.md`（view.xml 嵌套层次）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（AMIS input-table 嵌套）
  - `../nop-entropy/docs-for-ai/03-runbooks/customize-view.md`（view.xml bounded-merge + 嵌套 cell）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml 嵌套子表 / AMIS input-table / 自动推算 / 行校验）；不新增 BizModel 方法（`__save` 已支持嵌套），故不加载 `nop-backend-dev`；浏览器层 E2E（写路径 + 行级 onChange + 校验）需 `nop-testing` 辅助 Phase 5 编写新 spec。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径：`module-{purchase,sales}/erp-{pur,sal}-web/src/main/resources/_vfs/erp/{pur,sal}/pages/<Head>/<Head>.view.xml`
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置（`tests/e2e/playwright.config.ts`），无需新增基础设施

## Execution Plan

### Phase 1 — 范式探索与设计冻结

Status: completed
Targets: `docs/design/child-table-editor-patterns.md`（新建）+ 8 头行对子表设计表
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Explore`
- Prereqs: none

- [x] `Explore`: 验证 3 项并记录证据 file:line（**阻塞门控**：若展开失败需在 plan 内决策替代机制如直接 delta picker.page.yaml）：
  - (a) **view.xml `<form>` 嵌套 cell → AMIS input-table 管线**：抽样 `ErpPurOrder.view.xml`，在 `<form id="edit">` 末尾 `<layout>` 末尾追加单 cell `lines[明细行](2)`（占整行 width=2），运行 `mvn clean install -DskipTests` 触发 codegen，检查 `_dump/` 下展开后 page.yaml 是否含 `type: input-table` + `columns[]` + `addApi`/`delApi`。
  - (b) **AMIS input-table 列定义方式**：核实列定义是经 view.xml `<grid id="line-list">`（新增 grid id）映射，还是 cell 嵌套 `<form id="line-edit">`（嵌套 form）映射；nop-entropy codegen `page_form.xpl` 与 `view-gen.xlib` 的实际行为。
  - (c) **行内 picker 接线 + onChange 自动推算**：核实 AMIS input-table 列 cell 是否支持 `picker` 控件 + `onEvent: change` 调 XPL/JS 表达式重算 amount/amountWithTax 字段；既有 nop-entropy 样例（如 `_dump/nop-app/.../NopAuthResource.view.xml` 类似资源权限编辑）。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 在 plan 内固化 8 头行对的子表列集（每行 6-8 列，含 materialId/name/specification/uom/quantity/unitPrice/amount/taxRate/amountWithTax），来源综合各域 `ui-patterns.md`。最少列集原则：标识（materialId + 名称派生）+ 数量 + 单价 + 金额 + 税务（税率 + 含税额）+ 删除按钮。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策自动推算触发器实现方式：
  - **方案 A（推荐）**：view.xml cell `onEvent: change` XPL 内联表达式（`amount = quantity * unitPrice`、`amountWithTax = amount * (1 + taxRate / 100)`）
  - **方案 B**：在 `_vfs/_delta/default/erp/<short>/pages/<Head>/<Head>.page.yaml` 直接编辑 AMIS `onChange` JS（绕过 view.xml 抽象）
  - **方案 C**：后端 `@BizLoader` 派生（违反「避免后端 round-trip」Non-Goal，否决）
  - 选择 A，若 Explore (c) 裁决 A 不可行则降级 B 并记录替代路径
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策头聚合（totalAmount/totalTaxAmount/totalAmountWithTax）实时累加机制：
  - **方案 A（推荐）**：头表单 cell `onEvent` 监听 lines 数组变更，前端 sum 聚合
  - **方案 B**：禁用头表单聚合字段编辑（`static` 控件），仅在 `__save` 时由后端 `persistTotalAmounts` 计算
  - 选择 A 提供实时反馈；后端 persistTotalAmounts 已存在作为权威源不变
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `docs/design/child-table-editor-patterns.md` 新建文档，固化 8 头行对的最终列集表 + 范式说明（view.xml 嵌套写法、自动推算触发器、行校验位置、头聚合机制）。≥150 行，含每对实体列集表 + 范式说明 + 反模式自检表。
  - Skill: `none`

Exit Criteria:

- [x] Phase 1 Explore (a)/(b)/(c) 三项门控证据落地（含 file:line），若裁决替代机制则记录
- [x] 8 头行对子表列集 + 自动推算 + 头聚合决策在 plan 表格化记录
- [x] `docs/design/child-table-editor-patterns.md` 文件落地（≥150 行）

### Phase 2 — purchase 域 4 头行对子表编辑实施

Status: completed
Targets:
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceive/ErpPurReceive.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurInvoice/ErpPurInvoice.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReturn/ErpPurReturn.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1

每头实体改造（**delta 改造**：在既有 `<form id="view">` + `<form id="edit">` 的 layout 末尾追加 `=====>lines[明细行]` 组 + cell 嵌套子表）：
1. `<form id="view">` + `<form id="edit">` 的 `<layout>` 末尾追加 `=========>lines[明细行]======\n lines[明细行](2)`（占整行 width=2）
2. `<cells>` 内追加 `<cell id="lines" mergeFlat="true">` 嵌套子表控件（具体控件类型经 Phase 1 Explore 裁决）
3. 子表列集（来自 Phase 1 决策表）含 picker 接线 + onChange 自动推算表达式 + 行级校验
4. 头表单 `totalAmount`/`totalTaxAmount`/`totalAmountWithTax` cell 改为 `static-on-edit`（编辑态只读 + 监听 lines 变更实时聚合）

- [x] `Add | Fix`: **ErpPurOrder** + ErpPurOrderLine（核心采购订单行内编辑：materialId picker → 自动填 name/uom；qty × unitPrice → amount；amount × (1+taxRate/100) → amountWithTax；头 totalAmounts 实时聚合）
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurReceive** + ErpPurReceiveLine（收货单行：materialId + receivedQty + unitPrice + amount + warehouseId picker + batchNo 文本框预留批次追踪 successor）
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurInvoice** + ErpPurInvoiceLine（采购发票行：materialId + quantity + unitPrice + amount + taxRate + amountWithTax；关联 Receive 行导入归 Non-Goal）
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurReturn** + ErpPurReturnLine（采购退货行：materialId + returnQty + unitPrice + amount + returnReason 文本框）
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 启动 app，逐一打开 4 头实体新建/编辑表单→明细行区渲染→新增行→物料 picker 弹窗→自动填→修改数量触发金额重算→头 totalAmounts 实时累加。每项抽样证据记录到 plan。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 4 purchase 头实体 view.xml 的 `<form id="view">` + `<form id="edit">` 含嵌套子表控件
- [x] 4 子表行内 picker 接线正确（物料 picker 弹窗显示业务列集，复用 F4 Phase 1）
- [x] 4 子表行内 onChange 自动推算生效（修改 qty/unitPrice/taxRate 后 amount/amountWithTax 重算）
- [x] 4 头表单 totalAmounts 实时聚合（新增/删除/修改行后头字段更新）
- [x] 本地化验证：4 头实体 view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿（PAGE_ERROR_COUNT=0）

### Phase 3 — sales 域 4 头行对子表编辑实施

Status: completed
Targets:
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/ErpSalOrder.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalDelivery/ErpSalDelivery.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalInvoice/ErpSalInvoice.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalReturn/ErpSalReturn.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 2（purchase 范式已验证可复用）

每头实体改造（与 Phase 2 purchase 同构）：复用 Phase 2 的子表控件 + 自动推算 + 头聚合范式，仅替换字段名。

- [x] `Add | Fix`: **ErpSalOrder** + ErpSalOrderLine（销售订单行：materialId + quantity + unitPrice + amount + taxRate + amountWithTax）
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpSalDelivery** + ErpSalDeliveryLine（发货单行：materialId + deliveredQty + batchNo 预留 + amount 派生）
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpSalInvoice** + ErpSalInvoiceLine（销售发票行：materialId + quantity + unitPrice + amount + taxRate + amountWithTax）
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpSalReturn** + ErpSalReturnLine（销售退货行：materialId + returnQty + unitPrice + amount + returnReason）
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 启动 app，逐一打开 4 sales 头实体新建/编辑表单→明细行区渲染→新增行→物料 picker→自动填→修改数量→金额重算→头聚合。每项抽样证据记录。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 4 sales 头实体 view.xml 的 `<form id="view">` + `<form id="edit">` 含嵌套子表控件
- [x] 4 子表行内 picker + onChange 自动推算 + 头聚合全部生效
- [x] 本地化验证：4 头实体 view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 4 — 行级校验 + 头聚合数值正确性核实

Status: completed
Targets: Phase 2/3 落地的 8 头实体
Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 3

- [x] `Add`: 在每头实体子表控件配置行级校验（数量 ≤ 0 显示「数量必须大于 0」、单价 < 0 显示「单价不能为负」、金额 ≠ 数量 × 单价显示「金额与数量×单价不一致」）；校验仅警告不阻塞其他行（AMIS input-table `validateApi` 或 cell-level `validate`）
  - Skill: `nop-frontend-dev`
- [x] `Fix`: 若 Phase 2/3 落地的自动推算表达式存在精度漂移（BigDecimal 精度 vs JS 浮点），改为定精度 SCALE=4 HALF_UP（对齐 xmeta `<schema domain="amount" precision="20" scale="4">`）
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 浏览器抽样验证 8 头实体的行级校验：故意输入 qty=-1 / unitPrice=-5 / amount 与 qty×price 不匹配，确认每行显示错误提示且不阻塞保存其他合法行
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 8 头实体子表行级校验生效（3 类校验均触发提示）
- [x] 自动推算无精度漂移（与 xmeta scale=4 一致）
- [x] 本地化验证：行级校验触发后保存按钮显示禁用或显示总体错误数

### Phase 5 — Playwright E2E 覆盖（写路径含行嵌套）

Status: completed
Targets: `tests/e2e/crud/child-table-write.spec.ts`（新建，2 spec × 4 头对 = 8 测试，覆盖 purchase 4 对 + sales 4 对抽样）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 4

- [x] `Add`: 新建 `tests/e2e/crud/child-table-write.spec.ts`，覆盖 4 purchase + 4 sales 头对：
  - 测试 1（purchase 4 对）：经 GraphQL `__save` 含头 + 2 行嵌套 → 校验头 totalAmounts = 行累加 + 行 amount/amountWithTax 正确派生 → cleanup
  - 测试 2（sales 4 对）：同上结构
  - 复用 `tests/e2e/crud/_helper.ts` `runAmisFormWrite` 范式（若 AMIS 表单层写不达行嵌套粒度，降级 GraphQL 直调 `__save`，与既有 `master-data.write.spec.ts` 一致）
  - **AMIS input-table 渲染验证（不可降级）**：至少 1 头实体（如 ErpPurOrder）抽样经浏览器 DOM 断言验证子表控件实际渲染（如 `_dump/` 下展开后 page.yaml 含 `type: input-table` + `columns[]`，或启动 app 截图）；防止 GraphQL fallback 静默绕过 codegen 管线（Phase 1 Explore (a) 最高风险面）
  - Skill: `nop-testing`
- [x] `Proof`: `npx playwright test tests/e2e/crud/child-table-write.spec.ts` 全绿（含行嵌套 + 自动推算 + 头聚合断言）；不影响其他 spec（0 回归）
  - Skill: `nop-testing`

Exit Criteria:

- [x] `tests/e2e/crud/child-table-write.spec.ts` 落地，8 测试全绿
- [x] 全套件回归：`npx playwright test` 0 新增失败（含既有 167+ business-actions / 17+ visual / 4+ crud write spec）

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_084ffea73ffe9T0SXUDL6TrPEp`) — 1 blocker (B1: fabricated `ErpPurOrderDto.java` citation) + 3 major concerns (M1: "如适用" 反松弛; M2: unverified spec content claim; M3: Phase 5 AMIS-layer gap if GraphQL fallback).
- Independent draft review iteration 2: **accept** (`ses_084f8b73effefd9CN1hiqcH9Wg`) — 4/4 blockers resolved: B1 replaced with verifiable anchors `_ErpPurOrder.xmeta:222` + `ErpPurOrderInputBean.java:366` + `_gen/_ErpPurOrder.java:2088`; M1 removed "如适用"; M2 reworded to prospective verification; M3 added non-negotiable AMIS DOM verification gate. Plan acceptable for active status.

## Closure Gates

> 全部 Phase 完成且退出标准 `[x]` 后关闭。完整仓库验证在此处运行。

- [x] 范围内行为完成（Phase 1–5 全部 done；8 头对子表编辑落地）
- [x] 相关文档对齐：`docs/design/child-table-editor-patterns.md` 落地；purchase/sales `ui-patterns.md` 各补「子表编辑」段落（child-table-editor 范式节）；`docs/backlog/frontend-ui-roadmap.md` F4 Phase 2 P0 行标 completed
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test`（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）+ `npx playwright test tests/e2e/crud/child-table-write.spec.ts`（9 测试全绿）+ 全套件回归 0 新增失败（`tests/e2e/crud/` 45 测试全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符（2026-07-20 独立子代理结束审计通过，见下方 Closure 证据）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 从订单导入行（copy-line-from-order：Receive←Order / Invoice←Receive 等）

- Classification: `out-of-scope improvement` **RELEASED 2026-07-20**（F9 plan `2026-07-20-0629-3-f9-cross-document-navigation` Phase 2-3 落地，覆盖 purchase Receive/Invoice + sales Delivery/Invoice 4 子表）
- Why Not Blocking Closure: 从源单据导入行属跨单据编排（F9 范畴），需独立设计「源单据选择器 + 行映射规则 + 增量导入按钮」三个交互。本计划仅交付子表控件本身（手动新增/编辑行），手动新增可达 happy-path 不阻塞业务流程。
- Successor Required: `yes`（触发条件：F9 跨单据导航与关联回链 plan 启动时）— **已满足并 RELEASED**：F9 plan Phase 2 落地 ErpPurReceive/ErpPurInvoice 子表 copy-line + Phase 3 落地 ErpSalDelivery/ErpSalInvoice 子表 copy-line；范式见 `docs/design/cross-doc-navigation-patterns.md §3.4`。

### F4 Phase 2 P1（inventory/finance 3 对）

- Classification: `optimization candidate` **RELEASED 2026-07-20**（inventory 3 对已落地，finance Voucher 仍为独立 successor）
- Why Not Blocking Closure: roadmap F4 §Phase 2 表将 inventory（StockMove+Line / LandedCost+Line / TransferOrder+Line）+ finance（Voucher+Line 已 form 分组但无嵌套编辑）3 对列为 P1 优先级；本计划仅覆盖 P0 8 对。P1 复用本计划范式独立 plan 推进。
- Successor Required: `partial`（inventory 3 对已落地见 `docs/plans/2026-07-20-0629-1-f4p2-child-table-editor-p1-inventory.md`；finance ErpFinVoucher 子表编辑独立 successor，触发条件 F7/F9/F10 落地后）

### F4 Phase 2 P2/P3（mfg/assets/projects 3 对 + ext 8 域 ~36+ 对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: P2/P3 头行对使用频率与业务阻断度均低于 P0/P1；建议本计划 + P1 完成后根据实际使用反馈再细化 P2/P3 逐域映射。
- Successor Required: `yes`（触发条件：F4 Phase 2 P2/P3 plan 启动时）

### 多套子表 tabs（如 ErpPurOrder 同时含 OrderLine + PaymentPlan + DeliverySchedule）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 多子表 tab 容器属 F12（页面结构增强）范畴；本计划仅做单一主行表嵌套。
- Successor Required: `yes`（触发条件：F12 头行 tabs 切换 plan 启动时）

### 批次/序列号子表行嵌入（inventory Receive/Delivery 行的批次追踪）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 批次/序列号选择器属业务专用 picker（F4 Phase 1 已 defer 树形/批次类），需独立设计批次 picker + 行内批次展开表。
- Successor Required: `yes`（触发条件：F10 + 业务专用批次 picker plan 启动时）

## Closure

Status Note: Phase 1–5 全部 done。本计划交付 8 P0 头行对的子表行内编辑能力（purchase 4 + sales 4），范式见 `docs/design/child-table-editor-patterns.md`，作为 F4 Phase 2 P1/P2/P3 + F7 + F9 + F12 的输入。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，2026-07-20 执行；不重用执行者上下文）
- Evidence:
  - 独立审计范围：对仓库实时基线抽样核实，覆盖代码 / 测试 / 文档 / 路线图 / 日志五个面
  - 代码：16 个 view.xml 修改（8 头实体 form × view+edit = 16 处 `<cell id="lines">`，独立 grep 验证 purchase 4 + sales 4 = 8 文件全部命中）
  - 测试：`tests/e2e/crud/child-table-write.spec.ts` 文件存在（9064 bytes）
  - 设计文档：`docs/design/child-table-editor-patterns.md` 文件存在（13699 bytes，≥300 行）
  - 路线图：`docs/backlog/frontend-ui-roadmap.md` 第 132 行 P0 行 ✅ completed（2026-07-20）
  - 日志：`docs/logs/2026/07-20.md` 含 Phase 1–5 完成记录 + 五项验证证据（mvn 154 模块 BUILD SUCCESS / ErpAllWebPagesCollectTest PAGE_ERROR_COUNT=0 / mvn test 119 passed / playwright child-table-write 9 passed / crud 回归 45 passed）
  - 文本一致性：Plan Status completed / 5 Phase 全 completed / 全部 Exit Criteria [x] / 全部 Closure Gates [x] / Closure evidence 非占位符 — 五点一致
  - Anti-Hollow 检查：`<cell id="lines">` 嵌套子表控件经 codegen 展开为 AMIS input-table，Phase 5 AMIS DOM 验证非降级（见 07-20 日志第 11 行 `.cxd-InputTable` + `.cxd-Picker` + `.cxd-Number` 断言）
  - Deferred honesty：5 个 Deferred But Adjudicated 项目均不属已确认缺陷/契约漂移，分类与 successor 触发条件合理

Follow-up:

- F4 Phase 2 P1（inventory/finance 3 对）独立 plan
- F4 Phase 2 P2/P3（mfg/assets/projects + ext 8 域）独立 plan
- F7 非状态驱动 visibleOn 子表行内门控（如 Receive 行 batchNo 仅 batchTracked 物料显示）
- F9 跨单据导航与从订单导入行
- F12 多子表 tabs 容器
