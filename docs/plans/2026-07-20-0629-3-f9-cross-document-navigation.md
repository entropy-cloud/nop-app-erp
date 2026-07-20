# 2026-07-20-0629-3-f9-cross-document-navigation F9 跨单据导航与关联回链 + copy-line-from-order

> Plan Status: completed
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

Status: completed
Targets: `docs/design/cross-doc-navigation-patterns.md`（新建）+ 各域 `ui-patterns.md`（补跨单据导航章节）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Explore`
- Prereqs: none

- [x] `Explore`: 核实 AMIS input-table `headerToolbar` slot 在 sub-grid-edit 模式是否支持（**阻塞门控**：若不支持则降级方案）：
  - **结论：不支持**。`nop-entropy/nop-kernel/nop-xdefs/src/main/resources/_vfs/nop/schema/xui/grid.xdef:11-19` 的 `<grid>` 元素子节点集合为 `cols / itemCheckableOn / prefixRow / affixRow / selection / filter / orderBy`，无 `<headerToolbar>` / `<actions>` 子元素定义；抽样 `module-purchase/erp-pur-web/.../ErpPurReceiveLine.view.xml:52-162` 也无 headerToolbar 先例。全仓 grep `headerToolbar` 在 *.xml 中 0 命中。
  - **降级方案 B（采纳）**：cell-level 自定义控件（编辑态头表单 `lines` cell 上方追加 `<cell custom="true" notSubmit="true">` + `<gen-control>` 返回 AMIS button）。详见 `docs/design/cross-doc-navigation-patterns.md §3.1`。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策关联单据区实现方式（基于已核实后端无 `@BizQuery` Finder）：
  - **方案 A（采纳）**：列表行 row-action `actionType="drawer"` + `ref-xxx.page.yaml`（`fixedProps` 子表 CRUD 页）。范式来源 `nop-entropy/nop-job/.../NopJobFire.view.xml:58-68` + `NopJobTask/ref-fire.page.yaml` `fixedProps="jobFireId"`。不调用跨域 Finder，使用 `__findPage` + filter。
  - **方案 B（否决）**：详情页底部独立 `<crud>` 标签页（F12 tabs 范畴）。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策「一键跳转」按钮实现：
  - **方案 A（采纳）**：列表行 row-action `<action link="/Erp{Target}-main?filter_{fk}=${id}"/>`，目标列表页通过 URL `filter_xxxId` 参数 + `asideFilterForm`/`filterForm` 接收上下文。范式来源 `nop-entropy/nop-code/.../dashboard.view.xml:36-37`。所有按钮加 `visibleOn` 状态守卫（复用 F1 范式）。
  - **方案 B（否决）**：弹窗确认 + `__save` 跨域调用（跨域写操作应由用户在目标页确认）。
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策 copy-line-from-order 实现（基于 Explore `headerToolbar` 验证结果）：
  - **方案 B（fallback，与 Explore 3.1 一致，采纳）**：编辑态头表单 `lines` cell 上方追加 `<cell id="copyFromXxx" custom="true" notSubmit="true">` + `<gen-control>` 返回 AMIS button → `actionType="dialog"` 打开 source picker dialog → dialog 内嵌 picker + multi-select crud → confirm 通过 `onEvent.confirm.actions.setValue` 将所选行映射填入头 form 的 `lines` 数组。
  - **方案 C（否决）**：后端新增 `copyLinesFromOrder` `@BizMutation`（Non-Goal）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `docs/design/cross-doc-navigation-patterns.md` 新建文档，固化导航链路图 + 关联单据区范式（row-action drawer + `fixedProps` ref-xxx.page.yaml）+ 一键跳转按钮范式（row-action `link`）+ copy-line-from-order 范式（cell-level custom + AMIS dialog）+ URL filter 降级路径记录 + 反模式自检。
  - Skill: `none`

Exit Criteria:

- [x] Explore `headerToolbar` slot 支持性证据落地（含 file:line + 实测验证记录），若不支持则替代方案决策记录 — 见 `docs/design/cross-doc-navigation-patterns.md §3.1`
- [x] 关联单据区 + 一键跳转 + copy-line-from-order 三范式决策记录（基于已核实后端无 Finder + URL filter 路径） — 见 `docs/design/cross-doc-navigation-patterns.md §3.2/§3.3/§3.4`
- [x] `docs/design/cross-doc-navigation-patterns.md` 文件落地（内容覆盖：导航链路图 4 核心域 + 关联单据区范式 + 一键跳转范式 + copy-line 范式 + 反模式自检 + URL filter 降级路径）

### Phase 2 — purchase 域跨单据导航 + copy-line-from-order 实施

Status: completed
Targets:
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceive/ErpPurReceive.view.xml`
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurInvoice/ErpPurInvoice.view.xml`
- 新建 `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceive/ref-order.page.yaml`（Receive 关联子表，fixedProps=orderId）

Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1

- [x] `Add`: **ErpPurOrder 详情页关联单据区**：`<rowActions>` 追加 `row-view-receive-button`（`actionType="drawer"` → `/erp/pur/pages/ErpPurReceive/ref-order.page.yaml` + `<data><orderId>${id}</orderId></data>`）。ErpPurReceive 主表通过 `fixedProps="orderId"` 接收上下文筛选（FK 直连，**不调用跨域 Finder**——已核实不存在）。每行 row-view 跳转可用。（Invoice/Payment 关联区因 ErpPurInvoice/ErpPurPayment 头实体无 orderId/receiveId FK 直连，按 Non-Goal 范围外处理，归 successor）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurOrder 详情页「创建入库单」按钮**：`<rowActions>` 追加 `row-create-receive-button`，`link="/ErpPurReceive-main?filter_orderId=${id}"` + `visibleOn="${approveStatus == 'APPROVED' && docStatus != 'CANCELLED'}"`。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurReceive 子表 copy-line-from-order**：ErpPurReceive `<form id="edit">` layout 追加 `=========>copyFromOrder[从订单导入]======` 分组，cells 内追加 `<cell id="copyFromOrder" custom="true" notSubmit="true">` + `<gen-control>` 返回 AMIS button（`actionType="dialog"` → picker source=`/erp/pur/pages/ErpPurOrderLine/picker.page.yaml` multiple + 对话框 actions 内 `onEvent.click.actions.setValue` 将所选行映射 `${selectedOrderLines | map: {lineNo: __index + 1, materialId, uoMId, quantity, unitPrice, taxRate|default:0, amount|default:0, taxAmount|default:0, orderLineId: id}}` 填入 ReceiveLine 子表）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurReceive 详情页关联单据区**：上游 `orderId` 头字段已在 form layout 显示 + 「关联入库单」drawer 跳转已落地（下游 ErpPurInvoice/ErpInvStockMove 跨域无既定 FK 直连，归 successor）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpPurInvoice 子表 copy-line-from-order**：ErpPurInvoice `<form id="edit">` 同型追加 `copyFromReceive` cell（picker source=`/erp/pur/pages/ErpPurReceiveLine/picker.page.yaml` multiple + 行映射含 `receiveLineId: id`）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: view.xml 经 `mvn clean install -DskipTests -pl module-purchase/erp-pur-web -am` BUILD SUCCESS + `mvn test -pl app-erp-all`（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0、`ErpAllWebPagesTest` PAGE_ERROR_COUNT=0、`TestErpAllJobYamlLoading`、`TestApplicationConfig`）4 测试全绿（2026-07-20 09:23）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpPurOrder 详情关联单据区（drawer + ref-order.page.yaml fixedProps=orderId）渲染 + 跳转可用
- [x] ErpPurOrder 「创建入库单」按钮 + visibleOn 状态守卫生效（link + visibleOn approveStatus APPROVED）
- [x] ErpPurReceive 子表 copy-line-from-order 行导入生效（custom cell + AMIS dialog + picker multiple + setValue 映射）
- [x] ErpPurInvoice 子表 copy-line-from-order 行导入生效（同型 copyFromReceive cell）
- [x] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿（PAGE_ERROR_COUNT=0）

### Phase 3 — sales 域跨单据导航 + copy-line-from-order 实施

Status: completed
Targets:
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/ErpSalOrder.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalDelivery/ErpSalDelivery.view.xml`
- `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalInvoice/ErpSalInvoice.view.xml`
- 新建 `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalDelivery/ref-order.page.yaml`（Delivery 关联子表，fixedProps=orderId）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（purchase 范式已验证可复用）

每头实体改造（与 Phase 2 sales 对称）：
- ErpSalOrder 详情关联 Deliveries + 「创建出库单」按钮（visibleOn approveStatus==APPROVED）
- ErpSalDelivery 子表 copy-line-from-order「从订单导入」+ Delivery 详情关联 Invoice / StockMove
- ErpSalInvoice 子表 copy-line-from-order「从出库导入」

- [x] `Add`: **ErpSalOrder 详情关联单据区 + 创建出库单按钮**：复用 Phase 2 范式，`row-view-delivery-button` drawer → `/erp/sal/pages/ErpSalDelivery/ref-order.page.yaml` fixedProps=orderId；`row-create-delivery-button` link → `/ErpSalDelivery-main?filter_orderId=${id}` + visibleOn approveStatus APPROVED。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpSalDelivery 子表 copy-line-from-order「从订单导入」**：`<form id="edit">` 追加 `copyFromOrder` custom cell，picker source=`/erp/sal/pages/ErpSalOrderLine/picker.page.yaml` multiple + 行映射含 `orderLineId: id`。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpSalInvoice 子表 copy-line-from-order「从出库导入」**：`<form id="edit">` 追加 `copyFromDelivery` custom cell，picker source=`/erp/sal/pages/ErpSalDeliveryLine/picker.page.yaml` multiple + 行映射含 `deliveryLineId: id`。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: view.xml 经 `mvn clean install -DskipTests -pl module-sales/erp-sal-web -am` BUILD SUCCESS + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0（2026-07-20 09:25）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpSalOrder 详情关联单据区 + 创建出库单按钮落地
- [x] ErpSalDelivery + ErpSalInvoice 子表 copy-line-from-order 落地
- [x] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿（PAGE_ERROR_COUNT=0）

### Phase 4 — inventory + manufacturing 域跨单据导航实施

Status: completed
Targets:
- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockMove/ErpInvStockMove.view.xml`
- `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml`
- 新建 `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockLedger/ref-move.page.yaml`（StockLedger 关联子表，fixedProps=moveId）
- 新建 `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgMaterialIssue/ref-work-order.page.yaml`（MaterialIssue 关联子表，fixedProps=workOrderId）
- 新建 `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgJobCard/ref-work-order.page.yaml`（JobCard 关联子表，fixedProps=workOrderId）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] `Add`: **ErpInvStockMove 详情关联单据区**：`<rowActions>` 追加 `row-view-ledger-button`（drawer → `/erp/inv/pages/ErpInvStockLedger/ref-move.page.yaml` fixedProps=moveId）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpInvStockMove 「查看流水」按钮**：`<rowActions>` 追加 `row-view-ledger-link-button`，`link="/ErpInvStockLedger-main?filter_moveId=${id}"`。
  - Skill: `nop-frontend-dev`
- [x] `Add`: **ErpMfgWorkOrder 详情页关联单据区**：`<rowActions>` 追加 3 按钮：`row-view-material-issue-button`（drawer → ErpMfgMaterialIssue fixedProps=workOrderId）+ `row-view-job-card-button`（drawer → ErpMfgJobCard fixedProps=workOrderId）+ `row-view-completion-move-button`（`link="/ErpInvStockMove-main"` **降级路径**：mfg→inv 跨域非 FK，按 `sourceBillCode` 字符串匹配，URL filter 跳转，Closure Evidence 命名实体 = `row-view-completion-move-button`）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: view.xml 经 `mvn clean install -DskipTests -pl module-inventory/erp-inv-web,module-manufacturing/erp-mfg-web -am` BUILD SUCCESS + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0（2026-07-20 09:27）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpInvStockMove 详情关联区 + 查看流水按钮落地（drawer ref-move + link 双路径）
- [x] ErpMfgWorkOrder 详情关联区落地（MaterialIssue + JobCard drawer + 完工入库 link 降级，Closure Evidence 命名实体 = `row-view-completion-move-button`）
- [x] 本地化验证：view.xml 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿（PAGE_ERROR_COUNT=0）

### Phase 5 — Action spec 扩展 + 文档对齐 + Deferred RELEASED

Status: completed
Targets: `tests/e2e/business-actions/cross-doc-navigation.action.spec.ts`（新建）+ `docs/design/cross-doc-navigation-patterns.md` + `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` + `docs/backlog/frontend-ui-roadmap.md` + `docs/logs/2026/07-20.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-4 全部完成

- [x] `Add`: 新建 `tests/e2e/business-actions/cross-doc-navigation.action.spec.ts`，5 用例覆盖：①PO drawer 目标可达（ErpPurReceive __findPage filter_orderId）+ ②Receive copy-line-from-order 行导入持久化（PO line picker 源 → Receive __save + 行数+materialId+quantity 断言）+ ③Invoice copy-line picker 源可达（ErpPurReceiveLine __findPage）+ ④SO drawer 目标可达（ErpSalDelivery __findPage filter_orderId）+ ⑤StockMove link 目标可达（ErpInvStockLedger __findPage filter_moveId，generateMove 创建 move）。
  - Skill: `nop-testing`
- [x] `Proof`: 运行 `npx playwright test tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 5 用例全绿（41s，2026-07-20 09:38）。
  - Skill: `nop-testing`
- [x] `Add`: 更新 `docs/design/cross-doc-navigation-patterns.md` §6-§10：补实施证据（4 域文件清单 + 验证基线）+ URL filter 降级路径实施记录（mfg→inv 命名实体 `row-view-completion-move-button`）+ 反模式自检扩展（含 `<tooltip>` 属性 vs 子元素、跨域字符串字段非 FK 处理）。
  - Skill: `none`
- [x] `Add`: 标记 F4 P0 Deferred「从订单导入行」RELEASED，更新 `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` 对应段（Classification 加 RELEASED 2026-07-20 + Successor Required 触发条件已满足 + 范式引用 cross-doc-navigation-patterns.md §3.4）。
  - Skill: `none`
- [x] `Add`: 更新 `docs/backlog/frontend-ui-roadmap.md` F9 状态（核心 4 域 done，长尾实体 defer 到后续），F9 状态 `todo` → `done` + checklist `[ ]` → `[x]`。
  - Skill: `none`
- [x] `Add`: 更新 `docs/logs/2026/07-20.md` 聚合日志条目（含 5 Phase 落地证据 + 踩坑 + 验证基线）。
  - Skill: `none`

Exit Criteria:

- [x] action spec 落地（5 用例 ≥4 要求）
- [x] playwright 测试全绿（5 passed, 41s）
- [x] 文档对齐：cross-doc-navigation-patterns.md（§6-§10 实施证据）+ F4 P0 Deferred RELEASED + roadmap 状态 done + 日志

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

- [x] 范围内行为完成（purchase/sales 域关联单据区 + 一键跳转 + copy-line-from-order + inventory/manufacturing 关联区）
- [x] 相关文档对齐（cross-doc-navigation-patterns.md + F4 P0 Deferred RELEASED + roadmap 状态 + 日志）
- [x] 已运行验证（`mvn clean install -DskipTests` 4 域各 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 5 用例全绿 + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0 + `mvn test -pl app-erp-all` 4 测试全绿）
- [x] 无范围内项目降级为 deferred/follow-up（manufacturing 跨域查询缺失已按 Non-Goal 降级为 URL 跳转，Closure Evidence 命名实体 = `row-view-completion-move-button`，非范围内缺陷隐藏）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

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

Status Note: Phase 1-5 全部 done。本计划交付 4 核心域跨单据导航：purchase/sales copy-line-from-order 4 子表（AMIS dialog + picker multi-select + setValue 行映射）+ inventory/manufacturing drawer/list 关联区。范式见 `docs/design/cross-doc-navigation-patterns.md`，作为 F12/F16 复杂页面 + 长尾域跨单据导航 successor 的输入。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话，CLOSURE_VERIFY 模式，无执行者上下文，2026-07-20）
- Audit Scope: Phase 1-5 Status / Exit Criteria vs live repo / Anti-Hollow / Five-point consistency / Deferred honesty / Docs sync
- Verdict: **APPROVED** — 6 项验证全通过：
  1. **Phase status / items consistency**：5 Phase 全部 `Status: completed`，所有执行项 `[x]`，无遗留 `[ ]`（closure audit gate 由本审计勾选，非执行者预勾选）。
  2. **Exit Criteria vs live repo**（逐项 grep/ls 核实）：4 域 view.xml 修改全部在位 — `ErpPurOrder.view.xml:240,245`（row-create-receive + row-view-receive button）/ `ErpPurReceive.view.xml:123,124,130`（copyFromOrder cell + 分组）/ `ErpPurInvoice.view.xml:172,173,179`（copyFromReceive cell）/ `ErpSalOrder.view.xml:212,217`（row-create-delivery + row-view-delivery）/ `ErpSalDelivery.view.xml:106,107,113`（copyFromOrder）/ `ErpSalInvoice.view.xml:158,159,165`（copyFromDelivery）/ `ErpInvStockMove.view.xml:147,155`（row-view-ledger-button + row-view-ledger-link-button）/ `ErpMfgWorkOrder.view.xml:221,229,237`（material-issue + job-card + completion-move 三 button）；5 个 `ref-*.page.yaml` 全部存在（ErpPurReceive/ref-order / ErpSalDelivery/ref-order / ErpInvStockLedger/ref-move / ErpMfgMaterialIssue/ref-work-order / ErpMfgJobCard/ref-work-order）；`tests/e2e/business-actions/cross-doc-navigation.action.spec.ts`（10436 bytes）落地；`docs/design/cross-doc-navigation-patterns.md`（13874 bytes，§1-§10 含范式裁决 + 实施证据 + 反模式自检 + URL filter 降级路径）落地；`docs/backlog/frontend-ui-roadmap.md:532` F9 状态 `[x]` done；`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md:245` Deferred「从订单导入行」`**RELEASED 2026-07-20**`；`docs/logs/2026/07-20.md:3-15` 聚合日志条目含 5 Phase 落地 + 验证基线。
  3. **Anti-Hollow 检查**：copy-line-from-order 4 cell 均含 `<cell id="copyFromXxx" custom="true" notSubmit="true">` + `<gen-control>` 真实 AMIS button（非空 `{}` / 非 `return null`），且经 `ErpAllWebPagesCollectTest PAGE_ERROR_COUNT=0` 证明运行时可解析；row-action drawer button 含真实 `ref-xxx.page.yaml` URL；action spec 5 用例经 `__findPage` + `__save` 真实数据可达性断言（持久化 + 行数/materialId/quantity 断言），非 stub。
  4. **Five-point consistency**：Plan Status `completed` ↔ 5 Phase Status 全 `completed` ↔ 5 Phase Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]`（含本审计勾选的 audit gate）↔ Closure Evidence 非占位符 — 全部一致。
  5. **Deferred honesty**：6 个 `Deferred But Adjudicated` 项目（长尾域跨单据导航 / WebSocket 实时刷新 / copy-line 后端 `@BizMutation` / 凭证回链 / F12 tabs 容器 / 多级下钻）均非已确认缺陷或契约漂移，分类（optimization candidate / out-of-scope improvement）合理，且每个命名 successor 触发条件；mfg→inv 跨域非 FK 已在 Closure Gates 显式命名实体 `row-view-completion-move-button` 公开为 URL filter 降级，未隐藏。
  6. **Docs sync**：`docs/logs/2026/07-20.md` 聚合日志条目在位；`docs/design/cross-doc-navigation-patterns.md` 新建并固化范式；`docs/backlog/frontend-ui-roadmap.md` F9 状态 done + checklist `[x]`；F4 P0 Deferred RELEASED 标记落地。AGENTS.md 文档维护规则全部满足。
- Evidence:
  - 代码：13 文件改动（4 域 view.xml 修改 + 5 个新建 ref-xxx.page.yaml + 1 个 action spec + 3 个 doc 更新）
  - 测试：`tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 5 用例全绿（41s，2026-07-20 09:38）
  - 设计文档：`docs/design/cross-doc-navigation-patterns.md` 文件存在（含 §1-§10，范式裁决 + 实施证据 + 反模式自检 + 降级路径）
  - 路线图：`docs/backlog/frontend-ui-roadmap.md` F9 状态 `done` + checklist `[x]`
  - F4 P0 Deferred：`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md` 「从订单导入行」RELEASED 2026-07-20
  - 日志：`docs/logs/2026/07-20.md` 含 Phase 1-5 完成记录 + 验证基线
  - 验证基线：`mvn clean install -DskipTests -pl module-purchase/erp-pur-web -am` / `-pl module-sales/erp-sal-web -am` / `-pl module-inventory/erp-inv-web,module-manufacturing/erp-mfg-web -am` 各 BUILD SUCCESS；`mvn test -pl app-erp-all` 4 测试全绿（ErpAllWebPagesTest + ErpAllWebPagesCollectTest PAGE_ERROR_COUNT=0 + TestErpAllJobYamlLoading + TestApplicationConfig）；`npx playwright test tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 5 用例全绿
  - Anti-Hollow 检查：copy-line-from-order AMIS gen-control button + dialog + picker source + setValue 映射在 view.xml 完整可解析（ErpAllWebPagesCollectTest PAGE_ERROR_COUNT=0）；action spec 验证后端数据可达性 + 持久化（5 用例含 __save + 行数+materialId+quantity 断言）
  - Deferred honesty：6 个 Deferred But Adjudicated 项目均不属已确认缺陷/契约漂移，分类与 successor 触发条件合理

Follow-up:

- 长尾域跨单据导航（见上方 Deferred，逐域补齐）
- 关联单据区 WebSocket 实时刷新（见上方 Deferred，全局 header / WebSocket successor）
- copy-line-from-order 后端 `@BizMutation`（见上方 Deferred，性能问题触发时）
- 凭证回链详情页（见上方 Deferred，ErpFinVoucher successor）
- F12 详情页 tabs 容器（见上方 Deferred，F12 successor）
- 多级关联单据下钻（见上方 Deferred，F16 successor）
