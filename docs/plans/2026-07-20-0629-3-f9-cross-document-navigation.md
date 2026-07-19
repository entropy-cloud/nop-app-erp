# 2026-07-20-0629-3-f9-cross-document-navigation F9 跨单据导航与关联回链 + copy-line-from-order

> Plan Status: active
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` F9（跨单据导航与关联回链 P1）；`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` Deferred But Adjudicated「从订单导入行（copy-line-from-order：Receive←Order / Invoice←Receive 等）」(l.243-247) — Successor Required: yes，触发条件「F9 跨单据导航与关联回链 plan 启动时」**已满足**（本计划即该 successor）
> Related: `docs/plans/2026-07-19-1122-1-view-button-gap-fix.md`（F1 已完成，状态驱动 visibleOn 已就绪，本计划所有导航按钮均带状态守卫）；`docs/plans/2026-07-19-1818-1-f4p1-high-frequency-picker.md`（picker 范式参考）；`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（子表控件范式，copy-line-from-order 在子表行层落地）；`docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`（P2P/O2C 浏览器层链路 E2E 已落地，本计划前端导航按钮复用同样的业务路径）
> Audit: required

## Bundling Justification (Rule 4 + Rule 14)

本计划同时承载 3 个子特性：关联单据区（A）+ 一键跳转（B）+ copy-line-from-order（C）。Rule 4 要求「一个计划一个结果面」，Rule 14 允许「同一组件多功能优先使用一个 owner plan」。bundling 裁决：

- **共享 owner doc**：3 子特性均归属 `docs/backlog/frontend-ui-roadmap.md §F9`（roadmap §F9 §4 「一键跳转如采购订单页 → 创建入库单（**携带订单上下文**）」明确涵盖 copy-from-order 语义）；F4 P0 Deferred 也明确指向「F9 跨单据导航与关联回链 plan」。
- **共享 view.xml 编辑表面**：3 子特性均修改 `ErpPurOrder.view.xml` / `ErpPurReceive.view.xml` 等头实体 view.xml 的 form view 区 + sub-grid-edit toolbar 区；若拆为 3 plan，3 个独立 session 将并发编辑同一文件集合，违反 Rule 14 反并发原则。
- **共享验证路径**：3 子特性均经 Playwright action spec 验证（按钮点击 → URL 跳转 → 行为断言），无独立验证路径。
- **共享 owner doc 行为契约**：roadmap F9 §验收标准「核心域跨单据导航链接实现」覆盖全部 3 子特性，无独立 closure criteria。
- **裁决**：bundling 满足 Rule 14「同一组件多功能一个 plan」，不违反 Rule 4（结果面统一为「跨单据交互」：用户感知是「在文档间移动数据/视图」，无论交互原语是 navigation link / creation button / data import picker）。

## Current Baseline

基于实时仓库核实（2026-07-20）：

- **F9 范围**：详情页底部添加关联单据区 + 一键跳转按钮（如采购订单页 → 创建入库单携带订单上下文）。覆盖核心域：
  - **purchase**: RFQ → Quotation → PO → Receive → Invoice → Payment → Voucher
  - **sales**: Quotation → SO → Delivery → Invoice → Receipt → Voucher
  - **inventory**: StockMove → Source/Dest Bills → Related Moves → Ledger
  - **manufacturing**: WorkOrder → MaterialIssue → JobCard → Completion → Voucher
- **F1 已就绪**：状态驱动 `visibleOn` 全部落地（plan `2026-07-19-1122-1`）；本计划所有导航/跳转按钮可复用 visibleOn 状态守卫（如「创建入库单」按钮仅在 PO `approveStatus==APPROVED` 时可见）。
- **F4 P0 已就绪**：子表控件范式已落地（plan `2026-07-19-2200-1`）；本计划「copy-line-from-order」在子表行 toolbar 添加「从源单导入」按钮，复用 AMIS input-table toolbar 机制。
- **既有后端跨单据查询能力**（实时仓库核实 2026-07-20）：抽样 `rg "findByPurOrderId|findByReceiveId|findByInvoiceId|findByDeliveryId"` 在 `module-purchase` + `module-sales` 范围内 **0 匹配**；module-purchase 内仅 `ErpPurDashboardBizModel.java:68,102,131,175,208` 含 5 个 `@BizQuery` 聚合方法（非 FK 跨单据查询）。**本计划所有「关联单据区」与「一键跳转」均使用 `__findPage` + filter URL 跳转** 或 `__get` + 显式 ID 跳转，**不调用跨域 `@BizQuery` Finder 方法**（不存在）； Non-Goal 保持不变（不改后端 Java）。
- **后端 `__findPage` 支持过滤**：导航跳转目标列表页通过 URL 参数 + filter 机制接收上下文（如 `/p/ErpPurReceive__findPage?filter_purOrderId=123`），关联单据区使用嵌入 `<crud>` + 默认 `filterForm` 读取 URL 上下文。
- **F4 P0 sub-grid-edit grid 无 headerToolbar 先例**（实时仓库核实）：`ErpPurReceiveLine.view.xml` 等 8 P0 Line view.xml 的 `<grid id="sub-grid-edit">` 仅含 `<cols>`，无 `<headerToolbar>` / `<actions>` / 任何 toolbar 元素；本计划 Phase 1 Explore 必须先验证 AMIS input-table `headerToolbar` slot 在 sub-grid-edit 模式是否支持，再决策 copy-line-from-order 按钮放置位置。
- **roadmap F9 §4「一键跳转」**：如采购订单页 → 创建入库单（携带订单上下文）。当前缺失；本计划新增 `<button>` 调用 navigation 跳转 + URL 参数。**注：F9 §4「携带订单上下文」语义本身即涵盖「copy-line-from-order」（在目标新建页预填源单据字段），本计划将 copy-line-from-order 作为 §4 的子模式实现**。
- **F4 P0 Deferred「从订单导入行」**：本计划显式承接（receive 子表行 toolbar 「从订单导入」按钮 → picker 选 PO → 选 PO 行 → 行数据填入 receive 子表）。
- **既有视觉 E2E 基线**：`tests/e2e/business-actions/` 已有 `*.action.spec.ts` 范式（点击按钮→状态翻转断言）；本计划导航按钮可复用 action spec 模式（参考 `tests/e2e/business-actions/p2p-chain.action.spec.ts` 等既有 spec）。
- **前置验证基线**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；F1/F4 P0/F5/F6 plan 已全绿。

## Goals

1. **4 核心域详情页底部新增「关联单据」区**（purchase PO + Receive + sales SO + Delivery + inventory StockMove + manufacturing WorkOrder = 6 详情页）：
   - 向上游导航：来源单据链接（row-view 跳转目标单据详情）
   - 向下游导航：目标单据链接列表（如 PO 详情显示其全部 Receive/Invoice/Payment `<crud>` 嵌入列表 + filter URL 上下文）
   - 每关联单据显示编号、状态、金额、日期 + row-view 跳转
   - 实现路径：嵌入 `<crud>` + `asideFilterForm` + URL `filter_xxxId` 上下文（**不**依赖跨域 `@BizQuery` Finder；使用 `__findPage` + filter）
2. **4 详情页新增「一键跳转」按钮**（purchase PO → Receive 新建、sales SO → Delivery 新建、inventory StockMove → StockLedger 列表、manufacturing WorkOrder → MaterialIssue 列表）：
   - `<button actionType="link" to="/erp/{short}/pages/{Target}/add?{context}=${id}"/>` 携带 URL 参数
   - 状态驱动 visibleOn（如 PO `approveStatus==APPROVED` 时才显示「创建入库单」）
3. **4 子表 toolbar 新增「从源单导入行」按钮**（copy-line-from-order）：
   - ErpPurReceive 子表：「从订单导入」→ PO picker → 选 PO 行 → 行数据映射填入
   - ErpPurInvoice 子表：「从入库导入」→ Receive picker → 选行
   - ErpSalDelivery 子表：「从订单导入」→ SO picker → 选行
   - ErpSalInvoice 子表：「从出库导入」→ Delivery picker → 选行
   - 实现路径：sub-grid-edit grid 的 `headerToolbar` slot 或 `<actions>` 区按钮 → AMIS dialog → 嵌入 source picker + line multi-select → `onEvent.confirm.actions.setValue` 行映射（**Phase 1 Explore 必须先验证 headerToolbar 支持**）
4. **导航跳转 URL 参数传递**：跳转目标列表页支持 URL `?filter_xxxId=...` 自动应用筛选（复用 `__findPage` filter 机制 + codegen `asideFilterForm`/`filterForm` 读取 URL 上下文）。
5. **导航按钮状态驱动 visibleOn**：所有「创建下游单据」按钮只在合法状态下可见（复用 F1 visibleOn 范式）。
6. **Playwright action spec 扩展**：新建 `tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 含 4 用例覆盖：关联单据区渲染 + 一键跳转 URL 参数 + copy-line-from-order 行导入持久化 + StockMove 查看流水跳转。
7. **更新 `docs/design/cross-doc-navigation-patterns.md`**：固化跨单据导航模式 + copy-line-from-order 范式 + URL filter 降级路径。
8. **解除 F4 P0 Deferred「从订单导入行」**：标记 RELEASED。

## Non-Goals

- **修改后端 BizModel action**——本计划仅 view.xml + page.yaml + AMIS 层定制；所有跨单据查询使用 `__findPage` + filter URL（已核实后端无 Finder，见 Current Baseline）；不新增 `@BizQuery` / `@BizMutation` 方法。
- **修改 ORM 模型**——保护区域，仅 view.xml 层定制。
- **新增后端服务方法**——若 owner doc 设计的导航链后端能力缺失（如 manufacturing WorkOrder → MaterialIssue 跨域查询），降级为 URL filter 跳转，归 Deferred successor。
- **F12 详情页 tabs 容器**（如 ErpPurOrder 头+行 tabs）——本计划关联单据区是 view form 底部的简单 list/grid，不做 tabs 容器。
- **F12 工作台页面**（如「我的待办」聚合跨单据）——本计划仅做单实体详情的关联单据区。
- **F13 看板/时间线视图**（如 CRM 活动时间线）——F13 范畴。
- **复杂审批工作流导航**（xwf 任务到工作流详情）——xwf 已有平台 UI。
- **关联单据自动刷新 + WebSocket**——本计划关联单据区静态加载（form 渲染时一次性查询），实时刷新归 notify inbox plan Deferred（WebSocket 基础设施 successor）。
- **导航链路完整业务流程编排**（P2P/O2C 全链路 E2E）——已有 plan `2026-07-09-1249-1` + `2026-07-09-1249-2` 落地；本计划仅做前端 UI 导航。
- **i18n `i18n-en:`**——F15 覆盖；本计划使用中文 label。
- **像素级视觉回归**——独立 plan 覆盖。

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F9
  - `docs/design/purchase/ui-patterns.md`（purchase 跨单据导航链路设计 §Forward flow + §Reverse trace）
  - `docs/design/sales/ui-patterns.md`（sales 跨单据导航链路设计）
  - `docs/design/inventory/ui-patterns.md`（inventory 跨页面导航流程图）
  - `docs/design/manufacturing/ui-patterns.md`（manufacturing 跨单据导航）
  - `docs/design/child-table-editor-patterns.md`（copy-line-from-order 范畴说明）
  - `docs/architecture/view-and-page-strategy.md`
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（AMIS CRUD 关联列表 + 按钮 navigation）
- Skill Selection Basis: 加载 `nop-frontend-dev`（关联单据区 list/grid + 一键跳转 button + URL 参数 + copy-line picker 弹窗 + AMIS input-table toolbar）；不改后端 Java，故不加载 `nop-backend-dev`；action spec 扩展需 `nop-testing`。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Head>/<Head>.view.xml`
- 可能需要新建 picker page.yaml：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Head>/source-picker.page.yaml`（如 PO 选择器用于 Receive copy-line）
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — 范式裁决与导航链路冻结

Status: planned
Targets: `docs/design/cross-doc-navigation-patterns.md`（新建）+ 各域 `ui-patterns.md`（补跨单据导航章节）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Explore`
- Prereqs: none

- [ ] `Explore`: 核实 AMIS input-table `headerToolbar` slot 在 sub-grid-edit 模式是否支持（**阻塞门控**：若不支持则降级方案）：
  - 抽样 `module-purchase/erp-pur-web/.../ErpPurReceiveLine.view.xml` 的 `<grid id="sub-grid-edit">`（已核实当前仅含 `<cols>`，无 headerToolbar 先例）
  - 在 nop-entropy 平台测试 view.xml 内 `<grid id="sub-grid-edit"><headerToolbar><button actionType="dialog"/></headerToolbar>` 是否被 codegen 展开为 AMIS `input-table.headerToolbar`
  - 若不支持：决策替代放置（A）page-actions 按钮在头表单外，（B）cell-level 自定义控件嵌第一列，（C）grid `<actions>` 区（非 headerToolbar）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策关联单据区实现方式（基于已核实后端无 `@BizQuery` Finder）：
  - **方案 A（采纳）**：详情页 `<form id="view">` layout 末尾追加 `=========>relatedDocs[关联单据]======`，cells 内追加多个 `<cell id="relatedXxx"><crud name="related-list" grid="related-grid" filterForm="query" asideFilterForm="asideFilter"/></cell>` 嵌入只读 `<crud>` + URL `filter_xxxId` 上下文读取（不调用跨域 Finder，使用 `__findPage` + filter）。
  - **方案 B（否决）**：详情页底部独立 `<crud>` 标签页（F12 tabs 范畴）。
  - 选择 A（复用 codegen `<crud>` + filter URL 范式，无需后端 Finder）。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策「一键跳转」按钮实现：
  - **方案 A（采纳）**：详情页 `<form id="view">` `<page-actions>` 追加 `<button actionType="link" to="/erp/pur/pages/ErpPurReceive/add?purOrderId=${id}&supplierId=${supplierId}"/>`，目标新建页通过 URL 参数 + 自定义 AMIS `onEvent.mount` 加载头+行（或仅预填头字段，行由用户 copy-line-from-order 触发）。
  - **方案 B（否决）**：弹窗确认 + `__save` 跨域调用（跨域写操作应由用户在目标页确认）。
  - 选择 A（用户在目标页确认 + 复用 `__save` 链路）。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 copy-line-from-order 实现（基于 Explore `headerToolbar` 验证结果）：
  - **方案 A（推荐）**：sub-grid-edit grid 的 `headerToolbar` slot（若 Explore 验证支持）追加 `<button actionType="dialog">` 弹窗 → 弹窗内嵌 source picker + line multi-select → 用户选行 → AMIS `onEvent.confirm.actions.setValue` 将所选行映射填入子表。
  - **方案 B（fallback）**：若 Explore 裁决 headerToolbar 不支持，按钮放置按 Explore 替代方案（A/B/C）落地。
  - **方案 C（否决）**：后端新增 `copyLinesFromOrder` `@BizMutation`（Non-Goal，本计划不改后端）。
  - 选择 A 或 B（纯前端 + 既有 `__save` 持久化）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 在 `docs/design/cross-doc-navigation-patterns.md` 新建文档，固化导航链路图 + 关联单据区范式（`<crud>` + filter URL）+ 一键跳转按钮范式 + copy-line-from-order 范式 + URL filter 降级路径记录 + 反模式自检。
  - Skill: `none`

Exit Criteria:

- [ ] Explore `headerToolbar` slot 支持性证据落地（含 file:line + 实测验证记录），若不支持则替代方案决策记录
- [ ] 关联单据区 + 一键跳转 + copy-line-from-order 三范式决策记录（基于已核实后端无 Finder + URL filter 路径）
- [ ] `docs/design/cross-doc-navigation-patterns.md` 文件落地（内容覆盖：导航链路图 4 核心域 + 关联单据区范式 + 一键跳转范式 + copy-line 范式 + 反模式自检 + URL filter 降级路径）

### Phase 2 — purchase 域跨单据导航 + copy-line-from-order 实施

Status: planned
Targets:
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceive/ErpPurReceive.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurInvoice/ErpPurInvoice.view.xml`
- 新建 `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/source-picker.page.yaml`（PO 选择器）

Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1

- [ ] `Add`: **ErpPurOrder 详情页关联单据区**：`<form id="view">` layout 末尾追加 `=========>relatedDocs[关联单据]======\n relatedReceives(2) relatedInvoices(2) relatedPayments(2)`；cells 内追加 3 个 `<cell>` 嵌入只读 `<crud>`，使用 `__findPage` + URL `filter_purOrderId=${id}` 上下文筛选（**不调用跨域 Finder**——已核实不存在）。每 crud 显示编号/状态/金额/日期 + row-view 跳转。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpPurOrder 详情页「创建入库单」按钮**：`<page-actions>` 追加 `<button actionType="link" to="/erp/pur/pages/ErpPurReceive/add?purOrderId=${id}&supplierId=${supplierId}" visibleOn="${approveStatus == 'APPROVED' && docStatus != 'CANCELLED'}">创建入库单</button>`。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpPurReceive 子表 copy-line-from-order**：ErpPurReceiveLine 的 `<grid id="sub-grid-edit">` 按 Phase 1 Explore 决策的按钮位置（`headerToolbar` 或替代方案）追加 `<button actionType="dialog" label="从订单导入">` → 弹 PO picker → 选 PO → 弹该 PO 行列表 multi-select → 用户选行 → AMIS `onEvent.confirm.actions.setValue` 将所选行映射填入 ReceiveLine 子表（materialId/quantity/unitPrice 复制，lineNo 重新生成）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpPurReceive 详情页关联单据区**：上游 `relatedOrder`（显示 PO 信息 + 跳转链接）+ 下游 `relatedInvoices` / `relatedStockMoves`（库存移动单关联）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpPurInvoice 子表 copy-line-from-order**：ErpPurInvoiceLine 的 sub-grid-edit 按 Phase 1 决策位置追加「从入库导入」按钮 → 弹 Receive picker → 选行。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，逐一验证：PO 详情关联单据区渲染 → 「创建入库单」按钮状态守卫 → Receive 子表「从订单导入」picker 弹窗 → 选行填入 → Invoice 子表「从入库导入」。证据记录到 plan。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] ErpPurOrder 详情关联单据区（3 cells）渲染 + 跳转可用
- [ ] ErpPurOrder 「创建入库单」按钮 + visibleOn 状态守卫生效
- [ ] ErpPurReceive 子表 copy-line-from-order 行导入生效
- [ ] ErpPurInvoice 子表 copy-line-from-order 行导入生效
- [ ] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 3 — sales 域跨单据导航 + copy-line-from-order 实施

Status: planned
Targets:
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/ErpSalOrder.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalDelivery/ErpSalDelivery.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalInvoice/ErpSalInvoice.view.xml`
- 新建 `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/source-picker.page.yaml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（purchase 范式已验证可复用）

每头实体改造（与 Phase 2 sales 对称）：
- ErpSalOrder 详情关联 Deliveries / Invoices / Receipts + 「创建出库单」按钮（visibleOn approveStatus==APPROVED）
- ErpSalDelivery 子表 copy-line-from-order「从订单导入」+ Delivery 详情关联 Invoice / StockMove
- ErpSalInvoice 子表 copy-line-from-order「从出库导入」

- [ ] `Add`: **ErpSalOrder 详情关联单据区 + 创建出库单按钮**：复用 Phase 2 范式，FK 字段名替换（supplierId→customerId）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpSalDelivery 子表 copy-line-from-order「从订单导入」**。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpSalInvoice 子表 copy-line-from-order「从出库导入」**。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，逐一验证 sales 域导航 + copy-line。证据记录到 plan。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] ErpSalOrder 详情关联单据区 + 创建出库单按钮落地
- [ ] ErpSalDelivery + ErpSalInvoice 子表 copy-line-from-order 落地
- [ ] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 4 — inventory + manufacturing 域跨单据导航实施

Status: planned
Targets:
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockMove/ErpInvStockMove.view.xml`
- `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml`

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [ ] `Add`: **ErpInvStockMove 详情关联单据区**：上游 `sourceBill`（来源业务单据 + 跳转）+ 下游 `relatedLedger`（流水明细跳转 StockLedger 列表 + filter moveId）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpInvStockMove 「查看流水」按钮**：`<button actionType="link" to="/erp/inv/pages/ErpInvStockLedger__findPage?filter_moveId=${id}">查看流水</button>`。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: **ErpMfgWorkOrder 详情页关联单据区**（URL filter 跳转实现路径——已核实跨域 Finder 不存在）：`<page-actions>` 或 cell 内追加 URL 跳转按钮组：「查看领料单」→ `/erp/mfg/pages/ErpMfgMaterialIssue__findPage?filter_workOrderId=${id}`、「查看报工单」→ `/erp/mfg/pages/ErpMfgJobCard__findPage?filter_workOrderId=${id}`、「查看完工入库」→ `/erp/inv/pages/ErpInvStockMove__findPage?filter_sourceBillCode=${code}`；不嵌入 `<crud>` 因 mfg → inv 跨域无既定 `__findPage` 关联模式。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，验证 StockMove + WorkOrder 详情关联区。证据记录到 plan。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] ErpInvStockMove 详情关联区 + 查看流水按钮落地
- [ ] ErpMfgWorkOrder 详情关联区落地（或降级为 URL 跳转按钮）
- [ ] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 5 — Action spec 扩展 + 文档对齐 + Deferred RELEASED

Status: planned
Targets: `tests/e2e/business-actions/cross-doc-navigation.action.spec.ts`（新建）+ `docs/design/cross-doc-navigation-patterns.md` + `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` + `docs/backlog/frontend-ui-roadmap.md` + `docs/logs/2026/07-20.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-4 全部完成

- [ ] `Add`: 新建 `tests/e2e/business-actions/cross-doc-navigation.action.spec.ts`，覆盖 4-6 用例：PO 详情关联区渲染 + 「创建入库单」跳转 URL 参数正确 + Receive copy-line-from-order 行导入持久化（`__save` + 行数断言）+ ErpInvStockMove「查看流水」跳转。
  - Skill: `nop-testing`
- [ ] `Proof`: 运行 `npx playwright test tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 全绿。
  - Skill: `nop-testing`
- [ ] `Add`: 更新 `docs/design/cross-doc-navigation-patterns.md`：补实施证据 + 降级路径记录 + 反模式自检。
  - Skill: `none`
- [ ] `Add`: 标记 F4 P0 Deferred「从订单导入行」RELEASED，更新 `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` 对应段。
  - Skill: `none`
- [ ] `Add`: 更新 `docs/backlog/frontend-ui-roadmap.md` F9 状态（核心 4 域 done，长尾实体 defer 到后续）。
  - Skill: `none`
- [ ] `Add`: 更新 `docs/logs/2026/07-20.md` 聚合日志条目。
  - Skill: `none`

Exit Criteria:

- [ ] action spec 落地（≥4 用例）
- [ ] playwright 测试全绿
- [ ] 文档对齐：cross-doc-navigation-patterns.md + F4 P0 Deferred RELEASED + roadmap 状态 + 日志

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0837d175fffeSr9we0LFJU1t23`) because — 3 BLOCKERS + 6 MAJOR：
  - B1（Rule 1）：fabricated 后端 `findByPurOrderId` 等存在声明 → 已替换为实时仓库核实负向证据（`rg` 0 匹配），所有关联单据区改为 `<crud>` + URL filter 实现
  - B2（Rule 1+9）：Baseline「查询存在」vs Phase 1 Explore「核实存在」内部矛盾 → 已 resolved：Baseline 明确「Finder 不存在」，Phase 1 Explore 改为验证 headerToolbar slot（新的真实未知项）
  - B3（Rule 4）：bundling 3 子特性是否违反「一个计划一个结果面」 → 已新增 `Bundling Justification (Rule 4 + Rule 14)` 段说明共享 owner doc + 共享 view.xml 编辑表面 + 共享验证路径 + 共享 owner doc 行为契约；roadmap §F9 §4「携带订单上下文」明确涵盖 copy-from-order 语义
  - M1（Goals 依赖 Non-Goal 能力）：已修订 Goals #1 明确使用 `<crud>` + filter URL 实现路径，不依赖 Finder
  - M2（headerToolbar 假设未验证）：已新增 Phase 1 Explore 子项验证 + fallback 方案 A/B/C
  - M3（anti-slack range）：Goals 已改为具体计数（4 域 / 4 详情 / 4 子表 / 4 用例），无 range
  - M4（Phase 4 cross-domain 已知失败路径）：Phase 4 manufacturing 段已显式声明 URL filter 跳转实现路径
  - M5（Phase 1 Exit ≥150 行）：已替换为内容覆盖清单
  - M6（Closure Gate 静默降级）：已明确 purchase/sales core 不允许降级，inventory/manufacturing 若降级需 Closure Evidence 命名实体
- Independent draft review iteration 2: accept (`ses_083763f5cffeGxCZuu3QeTjKTE`) after — 1 MAJOR 修正（Non-Goals line 62 stale「`findByPurOrderId`」示例已替换为「使用 `__findPage` + filter URL（已核实后端无 Finder）」表述，与 Current Baseline + Phase 1 Decisions + Phase 2-4 implementation items 一致）；B1+B2+B3 全部 BLOCKERS 已 resolved。

## Closure Gates

- [ ] 范围内行为完成（purchase/sales 域关联单据区 + 一键跳转 + copy-line-from-order + inventory/manufacturing 关联区）
- [ ] 相关文档对齐（cross-doc-navigation-patterns.md + F4 P0 Deferred RELEASED + roadmap 状态 + 日志）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 + `npx playwright test` 新增 action spec 全绿 + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）
- [ ] 无范围内项目降级为 deferred/follow-up（manufacturing 跨域查询缺失若发生则降级 URL 跳转，非范围内缺陷隐藏）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 长尾域跨单据导航（crm/cs/hr/aps/logistics/b2b/contract/drp/assets/projects/quality/maintenance）

- Classification: `optimization candidate`
- Why Not Blocking Closure: roadmap F9 §典型导航链路表仅列 4 核心域；长尾域（如 crm Lead→Opportunity→Quotation、cs Ticket→Survey、hr Employee→Payroll）使用频率低，可按需逐域补齐。
- Successor Required: `yes`（触发条件：对应域跨单据导航业务需求落地时，按本计划范式补齐）

### 关联单据区自动刷新 + WebSocket 实时推送

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 关联单据区当前为静态加载（form 渲染时一次性查询）；实时刷新需 WebSocket 后端消息总线 + 浏览器 EventSource 接入，归 notify inbox plan Deferred（全局 header / WebSocket 基础设施 successor）。
- Successor Required: `yes`（触发条件：全局 WebSocket 基础设施 plan 启动时）

### copy-line-from-order 后端 copyLinesFromOrder `@BizMutation`

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划采用纯前端 + 既有 `__save`（picker 弹窗选行 → AMIS setValue 填入子表 → 用户保存触发 `__save`）；后端批量复制方法属增强型优化（避免前端行映射逻辑分散），但需独立后端 plan + 跨域接口审计。
- Successor Required: `yes`（触发条件：前端 copy-line 性能问题或行映射规则复杂度增长时）

### 「凭证回链」详情页（业务单据详情显示 voucher 链接 + voucher 反查业务单据）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P2P/O2C 编排 E2E plan（`2026-07-09-1249-1`）已落地凭证回链后端（`ErpFinVoucherBillR`）；本计划详情页关联区若包含 voucher 链接属 ErpFinVoucher 跨域导航，需核实 finance `IErpFinVoucherBiz.findByBillHeadCode` 查询接口；若缺失则归 successor。
- Successor Required: `yes`（触发条件：finance 域跨单据查询接口补齐后，或 ErpFinVoucher 子表编辑独立 plan 启动时）

### F12 详情页 tabs 容器（关联单据区作为独立 tab）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划关联单据区是 view form 底部的简单 cell 嵌套；tabs 容器（头+行+关联单据+审计日志 多 tab 切换）属 F12 详情页结构增强范畴。
- Successor Required: `yes`（触发条件：F12 工作台/详情页结构增强 plan 启动时）

### 多级关联单据下钻（如 PO → Receive → StockMove → StockLedger → Voucher 5 级链）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 1 级关联（PO → Receive/Invoice/Payment）；多级下钻需嵌套 drawer 或 tabs，属 F12 + F16 复杂页面范畴。
- Successor Required: `yes`（触发条件：业务用户需求多级单据追溯时，或 F16 复杂页面 plan 启动时）

## Closure

Status Note: <待执行完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 长尾域跨单据导航（见上方 Deferred，逐域补齐）
- 关联单据区 WebSocket 实时刷新（见上方 Deferred，全局 header / WebSocket successor）
- copy-line-from-order 后端 `@BizMutation`（见上方 Deferred，性能问题触发时）
- 凭证回链详情页（见上方 Deferred，ErpFinVoucher successor）
- F12 详情页 tabs 容器（见上方 Deferred，F12 successor）
- 多级关联单据下钻（见上方 Deferred，F16 successor）
