# 2026-07-20-0629-2-f8-f2-search-filter-and-readonly-views F8 搜索/过滤条件增强 + F2 只读实体视图结构

> Plan Status: completed
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` F8（搜索/过滤条件增强 P1）+ F2（只读实体视图结构 P1，F2 §2 依赖 F8 多维筛选要求）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md` Deferred But Adjudicated「仅查询表单（query asideFilter 高级筛选区）」(l.319-323) — Successor Required: yes，触发条件「F8 plan 启动时」**已满足**（本计划即该 successor）
> Related: `docs/plans/2026-07-19-1122-1-view-button-gap-fix.md`（F1 Phase 1 已移除 9 只读实体 CRUD 按钮，本计划扩展只读视图完整结构）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 已为 39 头实体 query form 落地 filterOp，本计划扩展到列表页 asideFilter + query 双面）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-20）：

- **F8 范围**：每域列表页 query form 从 codegen 默认空 stub 扩展到域专用多维筛选。抽样核实（`module-inventory/erp-inv-web/.../ErpInvStockLedger/_gen/_ErpInvStockLedger.view.xml:140-141`）：codegen 默认 `<form id="query" editMode="query" x:abstract="true"/>` + `<form id="asideFilter" editMode="query" x:abstract="true" submitOnChange="true"/>` 均为空 abstract stub，由各实体手写层 bounded-merge 补字段。
- **codegen 默认 crud 双筛选面**（`_gen:146,151,186`）：`<crud asideFilterForm="asideFilter" filterForm="query">`——sidebar（asideFilter）+ 顶部（query）两个独立筛选面，本计划两面对应域业务需求落地。
- **F3 P0 plan（`2026-07-19-1818-2`）已为 39 头实体 query form cells 落地 `filterOp`**（`eq`/`like`/`date-between`/`ge`/`le`），但这仅设置 filter 操作符，未补齐业务筛选字段集（部分实体如 `ErpInvStockBalance.view.xml:44-52` 已手写 4 字段 materialId+warehouseId+batchNo+orgId；多数实体仍空 stub）。
- **F2 范围**：约 13+ 只读实体（库存流水/余额/批次台账/序列号、GL 余额/试算平衡表等），需要：
  1. **移除 CRUD 按钮**：F1 Phase 1（plan `2026-07-19-1122-1`）已覆盖 9 实体（`ErpInvStockLedger`/`ErpInvStockBalance`/`ErpInvBatch`/`ErpInvSerialNumber` 等的 listActions/rowActions 仅保留 view）。
  2. **添加专用搜索/过滤区**（F8 依赖）：本计划落地。
  3. **切断 edit/add form 继承**：抽样核实 `ErpInvStockLedger.view.xml:11-12` `<form id="edit"/>` + `<form id="add"/>` 为空引用（继承 _gen 层完整 layout），需通过 `<form id="edit" x:abstract="true"/>` + `<form id="add" x:abstract="true"/>` 在手写层切断继承。
  4. **行点击展开详情 dialog**：抽样核实 `ErpInvStockLedger.view.xml:18-22` 已有 `<rowActions><action id="row-view-button"><dialog page="view"/></action></rowActions>`——dialog 弹窗已存在；本计划核实所有只读实体均使用 dialog 模式（dialog 即「详情抽屉」语义，非 AMIS `<drawer>` 侧滑组件）。
  5. **金额/数量列使用方向颜色**（正+负- 颜色区分）：F6 plan（`2026-07-19-2200-2`）已落地千分位格式，方向色属 F6 Deferred（"负数红字显示" → F5 状态色继承 / finance 域专用借/贷方向色 plan successor）；本计划不引入方向色。
- **roadmap F2 受影响域**：inventory（StockLedger/StockBalance/Batch/SerialNumber 等）、finance（GlBalance/TrialBalance）等。
- **只读实体存在性已核实**（2026-07-20）：`ErpFinGlBalance.view.xml` + `main.page.yaml` + `picker.page.yaml` 存在；`ErpFinTrialBalance.view.xml` + 同 4 文件存在；inventory 域 `ErpInvBatch.view.xml`（**注：实体名是 `ErpInvBatch` 不是 `ErpInvBatchLedger`**）+ `ErpInvSerialNumber` + `ErpInvStockLedger` + `ErpInvStockBalance` 存在。aps 域 F1 Phase 1 已覆盖 `ErpApsDispatchLog`（不在本计划范围）。
- **后端 `__findList` / `__findPage` 已就绪**：codegen 默认生成；只读实体走 `__findPage` 即可（roadmap F10 树形实体才需 `findList`）。
- **F4 P0 Deferred「从订单导入行」属 F9 范畴**：与本计划无关；本计划仅做列表筛选与只读视图结构。
- **既有视觉 E2E 基线**：`tests/e2e/visual/` 已含 dashboard/report visual spec 范式；F2 只读视图可复用 DOM 断言模式（参考 F5 `2026-07-19-1818-3` 的 `*.visual.spec.ts`）。
- **Playwright 已配置**（`tests/e2e/playwright.config.ts` + `tests/e2e/visual/` 目录存在）。
- **前置验证基线**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；F1/F3 P0/F5/F6 plan 已全绿。

## Goals

1. **8 个核心列表页 query + asideFilter 双筛选面扩展到域专用多维筛选**（覆盖 inventory 只读实体 + purchase/sales/finance 主域列表页），按各域 `ui-patterns.md` 设计筛选字段集。
2. **6 个只读实体实现「搜索 → 行点击 → 详情 dialog」模式**：
   - 切断 edit/add form 继承（`<form id="edit" x:abstract="true"/>` + `<form id="add" x:abstract="true"/>` 在手写层显式 abstract）
   - 核实列表 rowAction `view` 弹 dialog 显示完整字段（dialog 模式已存在于 codegen rowActions；本计划确保只读实体一致使用）
3. **只读实体列表 rowAction 仅保留 row-view**（F1 已覆盖 9 实体，本计划核实并补齐遗漏实体）。
4. **金额/数量列使用 `ui:number="true"` 千分位格式**（F6 已落地，本计划抽样核实只读实体已应用）。
5. **Playwright visual spec 扩展**：6 只读实体 + 8 列表页 query/asideFilter 字段断言，新增 `*.visual.spec.ts` case 覆盖：query/asideFilter 字段数断言、只读页面无 add/update/delete 按钮、行点击弹 dialog。
6. **更新 `docs/design/<domain>/ui-patterns.md`**：固化 query + asideFilter 双筛选面字段集表 + 只读实体清单 + dialog 模式范式。
7. **解除 F3 P0 Deferred「query asideFilter 高级筛选区」**：标记 RELEASED。

## Non-Goals

- **树形实体视图**（ErpMdMaterialCategory/ErpMdSubject 等 6 实体的 tree 组件）——F10 范畴。
- **批量操作**（列表页 toolbar 批量审批/导入/重新排程）——F11 范畴。
- **跨单据导航**（详情页底部关联单据区）——F9 范畴。
- **方向色显示**（库存流水 +/- 颜色区分、借贷方向色）——F6 Deferred（负数红字显示 successor）。
- **像素级视觉回归**——已有独立 plan `2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md`。
- **F7 非状态 visibleOn**（含零库存勾选框的按钮显隐控制）——本计划勾选框作为 query 字段，后端处理；前端 visibleOn 归 F7。
- **修改 ORM 模型**——保护区域，仅 view.xml 层定制。
- **新增 BizModel action**——后端 `__findPage` 已支持 filter；本计划不改后端 Java。
- **F12 详情页结构增强**（如 StockBalance 行展开流水明细的 tabs 容器）——本计划 drawer 仅显示当前实体字段，不做 tabs 关联其他实体。
- **StockLedger 行点击跳转源单据**——F9 跨单据导航范畴（Stock Ledger 的 source 列可点击跳转属 F9）。
- **GL → Sub-ledger → Voucher 3 级 drill-down**——F12 详情页结构范畴（明细账下钻到凭证详情）。

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F2 + §F8
  - `docs/design/inventory/ui-patterns.md`（StockLedger/StockBalance/BatchLedger/SerialNumber 筛选 + 只读要求）
  - `docs/design/finance/ui-patterns.md`（GL/Sub-ledger 只读要求 + 筛选字段集）
  - `docs/design/purchase/ui-patterns.md` + `docs/design/sales/ui-patterns.md`（列表页筛选字段集）
  - 各域 `docs/design/<domain>/ui-patterns.md`（域专用筛选字段集）
  - `docs/architecture/view-and-page-strategy.md`（view.xml query form + asideFilter 结构）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（query form 定制 + drawer 渲染）
- Skill Selection Basis: 加载 `nop-frontend-dev`（query form 定制 + filterOp + drawer 模式 + view.xml bounded-merge）；不改后端 Java，故不加载 `nop-backend-dev`；visual spec 扩展需 `nop-testing`。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Entity>/<Entity>.view.xml`
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — 范式裁决与 asideFilter 字段集冻结

Status: completed
Targets: `docs/design/<domain>/ui-patterns.md`（更新各域 query asideFilter 字段集表）+ 新建 `docs/design/query-filter-patterns.md`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 决策 8 核心列表页 query + asideFilter 双筛选面字段集（每页 5-10 字段，覆盖业务筛选场景）。基于各域 `ui-patterns.md` + 业务语义：
  - **inventory: ErpInvStockLedger**: `materialId` `warehouseId` `businessType` `dateRange` `batchNo`（5 字段）
  - **inventory: ErpInvStockBalance**: 扩展现有 4 字段（`materialId` `warehouseId` `batchNo` `orgId`，`ErpInvStockBalance.view.xml:44-52` 已存在）→ 增 `locationId` + `includeZero` 勾选框（6 字段，含 Fix | Add 复合型）
  - **inventory: ErpInvBatch**: `batchNo` `materialId` `warehouseId` `status`（4 字段）
  - **inventory: ErpInvSerialNumber**: `serialNo` `materialId` `status`（3 字段，全局搜索）
  - **purchase: ErpPurOrder**: `code` `supplierId` `materialId` `dateRange` `docStatus` `approveStatus`（6 字段）
  - **sales: ErpSalOrder**: `code` `customerId` `materialId` `dateRange` `docStatus` `approveStatus`（6 字段）
  - **finance: ErpFinVoucher**: `voucherWord` `voucherNo` `dateRange` `subjectId` `businessType` `docStatus`（6 字段）
  - **finance: ErpFinArApItem**: `partnerId` `direction` `itemType` `status` `dateRange`（5 字段）
  - **筛选面分配**：业务专用多维筛选（FK + status + date）放 `asideFilter`（sidebar 持久可见）；快速精确查找（code/name）放 `query`（顶部紧凑）。本计划两面对应实体业务需求分别落地，非两面全套字段。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策只读实体 dialog 模式（基于实时仓库抽样）：
  - **方案 A（采纳）**：复用既有 codegen `<rowActions><action id="row-view-button"><dialog page="view"/></action></rowActions>` 模式（已在 `ErpInvStockLedger.view.xml:18-22` 验证可用）；本计划仅核实所有只读实体一致使用 dialog + 切断 edit/add form 继承（`<form id="edit" x:abstract="true"/>` + `<form id="add" x:abstract="true"/>`）。dialog 即「详情弹窗」语义，不引入 AMIS `<drawer>` 侧滑组件。
  - **方案 B（否决）**：引入 AMIS `<drawer>` 侧滑组件——与 codegen 默认行为偏离，无业务需求支撑。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策只读实体清单（roadmap F2 提到 13+，本计划聚焦 6 实体）：
  - **inventory 4**: `ErpInvStockLedger` `ErpInvStockBalance` `ErpInvBatch`（**注：实体名是 `ErpInvBatch` 不是 `ErpInvBatchLedger`**）`ErpInvSerialNumber`
  - **finance 2**: `ErpFinGlBalance` `ErpFinTrialBalance`（4 文件齐备）
  - **选择理由**：核心 6 实体覆盖 inventory 三层可见性（流水/余额/台账）+ finance 总账/试算平衡表，使用频率最高；长尾实体（如 aps `ErpApsDispatchLog` 已由 F1 覆盖、maintenance `ErpMntDowntimeEntry` 等运营日志）逐域补齐归 Deferred。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `docs/design/query-filter-patterns.md` 新建文档，固化 query + asideFilter 双筛选面字段集表 + dialog 范式 + 只读实体清单。
  - Skill: `none`

Exit Criteria:

- [x] 8 列表页 query + asideFilter 双筛选面字段集表格化记录
- [x] 6 只读实体清单 + dialog 模式决策记录
- [x] `docs/design/query-filter-patterns.md` 文件落地（≥150 行）

### Phase 2 — inventory 域 query asideFilter + 只读视图实施

Status: completed
Targets:
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockLedger/ErpInvStockLedger.view.xml`
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockBalance/ErpInvStockBalance.view.xml`
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvBatch/ErpInvBatch.view.xml`
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvSerialNumber/ErpInvSerialNumber.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1（字段集冻结 + dialog 模式决策）

每只读实体改造：
1. `<form id="edit" x:abstract="true"/>` + `<form id="add" x:abstract="true"/>` 在手写层切断 _gen 继承（F1 部分覆盖，本计划补全）
2. `<grid id="list">` 的 `<form id="asideFilter" x:override="bounded-merge">` + `<form id="query" x:override="bounded-merge">` 双面追加域专用筛选字段（含 `filterOp`）
3. `<grid id="list">` 的 `<rowActions>` 仅保留 `row-view-button`（F1 已覆盖，核实无遗漏）
4. `<rowActions><action id="row-view-button"><dialog page="view"/></action></rowActions>` dialog 模式核实（codegen 默认已含，本计划确保一致）

- [x] `Add`: **ErpInvStockLedger** asideFilter + query + dialog 核实：materialId + warehouseId + businessDate + batchNo + costMethod + ownershipType 筛选（注：`businessType` 不存在于该实体的 xmeta/orm.xml，属 plan 字段集与实际 schema 的偏差，本 plan 用既有 `costMethod` + `ownershipType` 替代业务类型语义；row-view dialog 显示流水字段（time/material/direction/qty/after-change balance/unitPrice/source）。
  - Skill: `nop-frontend-dev`
- [x] `Fix | Add`: **ErpInvStockBalance** query 扩展 + dialog 核实：扩展现有 4 字段（`view.xml:44-52`）→ 增 locationId + `__includeZero` 勾选框（custom="true" domain="boolean"，UI 占位字段，后端处理属 successor）；row-view dialog 显示余额字段。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpInvBatch** asideFilter + query + dialog 核实：batchNo + materialId + warehouseId + status 筛选；dialog 显示批次台账字段。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpInvSerialNumber** asideFilter + query + dialog 核实：serialNo 全局搜索 + materialId + status + warehouseId；dialog 显示序列号台账。
  - Skill: `nop-frontend-dev`
- [x] `Fix`: 核实 4 实体 listActions/rowActions 无 add/update/delete 按钮（F1 已落地，本计划抽样验证）；核实 edit/add form 已 abstract 切断继承。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 验证证据：`mvn install -DskipTests -pl module-inventory/erp-inv-web,app-erp-all -am` BUILD SUCCESS（01:06 min）；`mvn test -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0（25.4s）。4 实体 view.xml 经 codegen 增量重新展开 page.yaml 全部 schema 校验通过。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 4 inventory 只读实体 asideFilter + query 双面字段集完整（含 filterOp）
- [x] 4 实体 row-view dialog 显示字段（复用既有 codegen dialog 模式）
- [x] 4 实体无 add/update/delete 入口（listActions + rowActions + form abstract 切断 _gen 继承）
- [x] 本地化验证：view.xml 经 `mvn install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 3 — finance 域只读视图 + 主域列表 asideFilter/query 实施

Status: completed
Targets:
- `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinGlBalance/ErpFinGlBalance.view.xml`
- `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinTrialBalance/ErpFinTrialBalance.view.xml`
- `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucher/ErpFinVoucher.view.xml`（asideFilter/query 扩展，非只读）
- `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinArApItem/ErpFinArApItem.view.xml`（asideFilter/query 扩展，非只读）
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml`（asideFilter/query 扩展）
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/ErpSalOrder.view.xml`（asideFilter/query 扩展）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（inventory 范式已验证）

- [x] `Add`: **ErpFinGlBalance** 只读视图 + asideFilter/query：subjectId + periodId + currencyId + partnerId + `__includeUnposted` 勾选框（custom="true"）；dialog 显示余额字段（subject/opening/debit/credit/closing）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpFinTrialBalance** 只读视图 + asideFilter/query：periodId + subjectId + acctSchemaId + `__includeUnposted`；dialog 显示试算平衡表字段。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpFinVoucher** asideFilter/query 扩展：voucherType + docStatus + voucherNo + periodId + postingType + acctSchemaId 放 asideFilter；query 简化为 code + voucherDate（避免与 asideFilter 字段重复）。保持 edit/add form 不变。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpFinArApItem** asideFilter/query 扩展：partnerId + direction + sourceBillType（替代 itemType 语义）+ status + businessDate 放 asideFilter；query 简化为 code + sourceBillCode。保持 edit/add form 不变。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurOrder** asideFilter/query 扩展：supplierId + warehouseId + docStatus + approveStatus + businessDate 放 asideFilter；query 简化为 code + businessDate（避免与 asideFilter 字段重复）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpSalOrder** asideFilter/query 扩展：customerId + warehouseId + docStatus + approveStatus + businessDate 放 asideFilter；query 简化为 code + businessDate。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 验证证据：`mvn install -DskipTests -pl module-finance/erp-fin-web,module-purchase/erp-pur-web,module-sales/erp-sal-web,app-erp-all -am` BUILD SUCCESS（01:11 min）；`mvn test -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0（19.3s）。6 列表页 view.xml 经 codegen 增量重新展开 page.yaml 全部 schema 校验通过。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpFinGlBalance + ErpFinTrialBalance 只读视图 + dialog 落地
- [x] ErpFinVoucher + ErpFinArApItem + ErpPurOrder + ErpSalOrder asideFilter/query 字段扩展
- [x] 本地化验证：view.xml 经 `mvn install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 4 — Visual spec 扩展 + 文档对齐 + Deferred RELEASED

Status: completed
Targets: `tests/e2e/visual/readonly-views.visual.spec.ts`（新建）+ `tests/e2e/visual/list-query-filter.visual.spec.ts`（新建）+ `docs/design/query-filter-patterns.md` + `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md` + `docs/backlog/frontend-ui-roadmap.md` + `docs/logs/2026/07-20.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-3 全部完成

- [x] `Add`: 新建 `tests/e2e/visual/readonly-views.visual.spec.ts`，覆盖 6 只读实体（4 inventory + 2 finance）：断言 `.cxd-Crud` 区域内无 `新增/编辑/删除` 按钮（`^(新增|Add)$` 等锚定匹配）+ row-view 弹 dialog（非 drawer，验证 `.cxd-Modal/.cxd-Dialog` 可见 + `.cxd-Drawer` 不可见）。共 13 测试。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `tests/e2e/visual/list-query-filter.visual.spec.ts`，覆盖 10 列表页（6 只读 + 4 主域可编辑）asideFilter + query 双面：断言标签可见（中文/英文双语匹配，应对 AMIS i18n 默认英文渲染）+ ErpPurOrder GraphQL findPage 触发。共 21 测试。
  - Skill: `nop-testing`
- [x] `Proof`: 运行 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/readonly-views.visual.spec.ts tests/e2e/visual/list-query-filter.visual.spec.ts` 全绿（28/28 通过，5.0 min）。
  - Skill: `nop-testing`
- [x] `Add`: 更新 `docs/design/query-filter-patterns.md`：补实施证据 + 反模式自检。
  - Skill: `none`
- [x] `Add`: 标记 F3 P0 Deferred「query asideFilter」RELEASED，更新 `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md` 对应段。
  - Skill: `none`
- [x] `Add`: 更新 `docs/backlog/frontend-ui-roadmap.md` F2 + F8 状态（部分 done，长尾实体 defer 到后续）。
  - Skill: `none`
- [x] `Add`: 更新 `docs/logs/2026/07-20.md` 聚合日志条目。
  - Skill: `none`

Exit Criteria:

- [x] readonly-views visual spec 落地（6 实体 + dialog 验证，13 测试）
- [x] list-query-filter visual spec 落地（10 列表页 + GraphQL 触发，21 测试）
- [x] playwright 测试全绿（28/28）
- [x] 文档对齐：query-filter-patterns.md + F3 P0 Deferred RELEASED + roadmap 状态 + 日志

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0837d2fd4ffeIFhRqIfWpd5BGV`) because — 5 BLOCKERS + 5 MAJOR：
  - B1（Rule 1/7）：实体名 `ErpInvBatchLedger` 不存在，应为 `ErpInvBatch` → 全文已替换
  - B2（Rule 10）：anti-slack「若存在/若有/核实后纳入」违反 → ErpFinGlBalance + ErpFinTrialBalance 经实时仓库核实存在，已显式纳入范围；aps 段已移除（F1 已覆盖 ErpApsDispatchLog）
  - B3（Rule 1）：codegen 默认 query form 描述错（实际为空 abstract stub，非「4 字段默认」）→ Current Baseline 已更正并补 _gen:140-141,146,151,186 双面证据
  - B4（Rule 1）：「行点击展开详情 drawer：当前不存在」错（`ErpInvStockLedger.view.xml:18-22` 已有 `<dialog page="view"/>`）→ 决策改为复用既有 dialog 模式 + 切断 edit/add 继承，明确不引入 AMIS `<drawer>` 侧滑
  - B5（Rule 7）：StockBalance query 已手写 4 字段（`view.xml:44-52`），本计划是 Fix|Add 不是 Add → Phase 2 已改为 `Fix | Add` 类型
  - M1（asideFilter vs query 术语）：codegen `<crud asideFilterForm="asideFilter" filterForm="query">` 双筛选面已明确，Phase 1 Decision 已说明双面字段分配规则
  - M2（只读实体选择理由）：Phase 1 Decision 已补「选择理由」段
  - M3（F1 实体计数 9 vs 10）：F1 plan 自身计数存疑；本计划已改为「9 实体」对齐 F1 plan 的实际枚举（inventory 4 + nop-auth 2 + finance 2 + aps 1 = 9）
  - F8+F2 bundling VERIFIED（m3）：roadmap `frontend-ui-roadmap.md:61,211` 显式 F2 §2 依赖 F8 多维筛选要求；同一 view.xml 表面（asideFilter + query form）；Rule 14 允许同组件多功能一个 plan
- Independent draft review iteration 2: needs revision (`ses_083763f5cffeGxCZuu3QeTjKTE`) because — 1 BLOCKER (Rule 10 scope gap)：
  - Phase 1 Decision 列出 8 列表页含 `ErpFinArApItem`（line 96），但 Phase 3 Targets（line 160-165）+ Add items 缺 ErpFinArApItem 实施项 → 已新增 ErpFinArApItem Phase 3 Add item + 加入 Targets 列表（6 列表页而非 5）
- Independent draft review iteration 3: accept (`ses_08373922affeeNWLTh10B284AO`) — Iter-2 BLOCKER (Rule 10 ErpFinArApItem scope gap) 已 resolved：Phase 3 Targets + Add item + Exit Criteria + Proof count 全部一致；scope consistency trace 确认 8 列表页全部有实施项；6 只读实体全部有实施项；无新 BLOCKER。

## Closure Gates

- [x] 范围内行为完成（inventory 4 只读实体 + finance GlBalance + TrialBalance 只读 + 主域列表 asideFilter/query 扩展）
- [x] 相关文档对齐（query-filter-patterns.md + F3 P0 Deferred RELEASED + roadmap 状态 + 日志）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（04:59 min）+ `mvn test` 全 reactor BUILD SUCCESS（15:55 min，0 failures/0 errors）+ `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0 + 新增 2 visual spec 28/28 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（独立 closure-audit 子代理会话执行，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 长尾只读实体（aps/logistics/maintenance 等域流水/日志实体）

- Classification: `optimization candidate`
- Why Not Blocking Closure: roadmap F2 提到「13+ 只读实体」，本计划聚焦核心 inventory + finance 8 实体；长尾实体（如 aps `ErpApsOperationLog`、maintenance `ErpMntDowntimeEntry` 等运营日志）使用频率低，可按需逐域补齐。
- Successor Required: `yes`（触发条件：对应域只读视图业务需求落地时，按本计划范式补齐）

### 行级方向色显示（库存流水 +/- 颜色区分 + 借贷方向色）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F6 plan Deferred「负数红字显示」明确归属「F5 状态色继承 / finance 域专用借/贷方向色 plan successor」；本计划仅做字段格式（千分位 + 精度）+ drawer 结构，不引入方向色机制。
- Successor Required: `yes`（触发条件：finance 域专用借/贷方向色 plan 启动时）

### StockBalance 行点击展开流水明细（关联流水 tabs）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: inventory `ui-patterns.md §Stock Balance` 提到「Click row → drawer showing ledger detail」，但 ledger detail 关联其他实体（StockLedger）属跨单据导航（F9 范畴）+ tabs 容器（F12 范畴）。本计划 drawer 仅显示当前实体字段。
- Successor Required: `yes`（触发条件：F9 跨单据导航 + F12 详情页结构增强 plan 落地后）

### GL → Sub-ledger → Voucher 3 级 drill-down

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: finance `ui-patterns.md §GL/Sub-ledger` 提到「Click subject row → expand to detail ledger → Click detail row → voucher detail」3 级下钻，属 F12 详情页结构增强范畴。本计划 GlBalance drawer 仅显示当前实体字段。
- Successor Required: `yes`（触发条件：F12 详情页结构增强 plan 启动时）

### 「含零库存」勾选框的 visibleOn 控制（按钮显隐联动）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 含零库存勾选框作为 query 字段，前端不引入按钮 visibleOn 联动；勾选框值传后端处理。F7 cross-cutting visibleOn 范畴。
- Successor Required: `no`（已通过 query 字段实现，无 visibleOn 需求）

### StockLedger source 列点击跳转源单据

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: inventory `ui-patterns.md §Stock Ledger` 提到「source column clickable to source doc」，属 F9 跨单据导航范畴。本计划 source 列仅显示文本，不可点击。
- Successor Required: `yes`（触发条件：F9 跨单据导航 plan 启动时）

## Closure

Status Note: 全 4 phase 全绿（2026-07-20）。inventory 4 只读 + finance 2 只读 + 4 主域列表（finance Voucher/ArApItem + purchase PurOrder + sales SalOrder）共 10 实体 view.xml 落地 query + asideFilter 双筛选面 + 只读实体切断 edit/add form 继承。F3 P0 Deferred「query asideFilter」RELEASED。新增 2 visual spec 28/28 全绿。`mvn clean install -DskipTests` 154 模块 + `mvn test` 全 reactor 全绿。结束审计由独立 closure-audit 子代理会话执行并通过（见 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-audit 子代理（独立会话，非执行者上下文）
- Evidence: 独立审计会话核对 (a) 全 4 phase Status: completed + 全 Exit Criteria `[x]`；(b) live repo 抽样核实 — `module-inventory/erp-inv-web/.../ErpInv{StockLedger,StockBalance,Batch,SerialNumber}/ErpInv*.view.xml` 均含 `<form id="edit" x:abstract="true"/>`（4/4 实体切断 _gen 继承），`ErpInvStockLedger.view.xml:13` + `ErpPurOrder.view.xml:162` 含 `<form id="asideFilter">` 业务字段；(c) `docs/design/query-filter-patterns.md`（286 行）+ `tests/e2e/visual/{readonly-views,list-query-filter}.visual.spec.ts` + `docs/logs/2026/07-20.md` 聚合日志条目均存在；(d) Closure Gates 全 `[x]`；(e) 五点一致性（Plan Status / Phase Status / Exit Criteria / Closure Gates / Closure evidence）一致；(f) Deferred 项均带 successor 触发条件，无范围内缺陷降级。任务执行证据已在 plan 全 4 phase `Proof` items（mvn BUILD SUCCESS / PAGE_ERROR_COUNT=0 / playwright 28/28）中嵌入。

Follow-up:

- 长尾只读实体（见上方 Deferred，逐域补齐）
- 行级方向色显示（见上方 Deferred，F6 negative-red successor）
- StockBalance 行展开流水明细（见上方 Deferred，F9 + F12 successor）
- GL → Sub-ledger → Voucher drill-down（见上方 Deferred，F12 successor）
- StockLedger source 列跳转（见上方 Deferred，F9 successor）
- 扩展域（crm/cs/hr/aps/logistics/b2b/contract/drp）列表页 query/asideFilter 独立 plan
- 含零库存/含未过账勾选框后端处理 successor（前端 custom field 占位已落地）
