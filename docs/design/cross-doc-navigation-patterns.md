# 跨单据导航与关联回链范式（Cross-Doc Navigation Patterns）

> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F9、`docs/design/purchase/ui-patterns.md`（跨单据导航链路设计 §Forward flow + §Reverse trace）、`docs/design/sales/ui-patterns.md`、`docs/design/inventory/ui-patterns.md`、`docs/design/manufacturing/ui-patterns.md`、`docs/design/child-table-editor-patterns.md`（copy-line-from-order 范畴说明）、`docs/architecture/view-and-page-strategy.md`、`../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`
> 落地计划：`docs/plans/2026-07-20-0629-3-f9-cross-document-navigation.md`（F9 4 核心域跨单据导航 + copy-line-from-order）

## 1. 目的与范围

固化「ERP 头实体详情页/列表页跨单据导航与关联回链」的标准范式，供 4 核心域（purchase / sales / inventory / manufacturing）以及长尾域后续按图施工。

**适用范围**：
- 详情页（或列表页行）→ 下游单据新建（携带头上下文 URL 参数）
- 详情页（或列表页行）→ 上下游关联单据查看（drawer + `fixedProps` 子表）
- 子表编辑态「从源单导入行」（copy-line-from-order）

**不适用**：
- 多级下钻（PO → Receive → StockMove → Ledger → Voucher 5 级链）→ F12 + F16 范畴
- 关联单据自动刷新 + WebSocket 实时推送 → notify inbox successor
- 后端 `copyLinesFromOrder` `@BizMutation`（优化候选，性能问题触发时再上）
- 凭证回链详情页（ErpFinVoucher 跨域）→ finance 域 successor

## 2. 导航链路图（4 核心域）

```
purchase:  RFQ → Quotation → PO → Receive → Invoice → Payment → Voucher
sales:     Quotation → SO → Delivery → Invoice → Receipt → Voucher
inventory: StockMove → Source/Dest Bills → Related Moves → Ledger
mfg:       WorkOrder → MaterialIssue → JobCard → Completion → Voucher
```

每条链路上，本范式落地「1 级关联」：头实体详情/行 → 直接上下游单据查看/新建。

## 3. Phase 1 范式裁决（Explore + Decision 记录）

### 3.1 Explore：AMIS input-table `headerToolbar` slot 在 sub-grid-edit 模式是否支持

**核实证据**（实时仓库 + 平台 xdef）：

- `nop-entropy/docs-for-ai` 源 xdef：`nop-kernel/nop-xdefs/src/main/resources/_vfs/nop/schema/xui/grid.xdef:11-19`，`<grid>` 元素的子节点集合为 `cols / itemCheckableOn / prefixRow / affixRow / selection / filter / orderBy`，**无 `<headerToolbar>` / `<actions>` / `<actions>` 子元素定义**。
- 抽样本项目 `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceiveLine/ErpPurReceiveLine.view.xml:52-162`（F4 P0 落地的 `<grid id="sub-grid-edit">`）— 仅含 `<cols>`，无 headerToolbar 先例。
- 全仓库 grep `actionType="link"` / `headerToolbar` 在所有 `*.xml` 中 0 命中（项目层无既存用法）。

**裁决**：`<grid id="sub-grid-edit">` 在 nop xview.xdef 不支持 `<headerToolbar>` slot。copy-line-from-order 按钮放置采用 **fallback 方案 B：cell-level 自定义控件**（编辑态头表单 `lines` cell 上方追加 `<cell id="copyFromXxx" custom="true" notSubmit="true">` + `<gen-control>` 返回 AMIS button）。

**替代方案对比表**：

| 方案 | 落地点 | 可行性 | 选择 |
|------|--------|--------|------|
| A. page-actions 按钮在头表单外 | form.xdef 无 `<actions>` 子元素 | ❌ 不可行（form 无 actions；simple page 有 actions 但仅 wrapper 模式） | 否决 |
| B. cell-level 自定义控件嵌头表单 | form cells 内追加 `<cell custom="true">` + `<gen-control>` 返回 AMIS button | ✅ 可行（form.xdef:84-97 支持 cell + gen-control） | **采纳** |
| C. grid `<actions>` 区 | grid.xdef 无 `<actions>` 子元素 | ❌ 不可行 | 否决 |

### 3.2 Decision：关联单据区实现方式（基于已核实后端无 `@BizQuery` Finder）

**核实证据**（plan Current Baseline）：
- `rg "findByPurOrderId|findByReceiveId|findByInvoiceId|findByDeliveryId"` 在 `module-purchase` + `module-sales` 范围内 0 匹配（无跨域 Finder）。
- module-purchase 内仅 `ErpPurDashboardBizModel.java:68,102,131,175,208` 含 5 个 `@BizQuery` 聚合方法（非 FK 跨单据查询）。

**裁决**：**方案 A（采纳）** = 列表行 row-action `actionType="drawer"` + `ref-xxx.page.yaml`（`fixedProps` 子表 CRUD 页）。详情/列表行点「关联入库单」→ 打开 drawer，渲染 ErpPurReceive 子表 CRUD，外键 `orderId` 固定为当前行 id。

**否决方案 B** = 详情页底部独立 `<crud>` tabs 容器（F12 范畴）。

**范式来源**：`nop-entropy/nop-job/.../NopJobFire.view.xml:58-68`（`view-tasks-button` drawer + `/nop/job/pages/NopJobTask/ref-fire.page.yaml` with `fixedProps="jobFireId"`）。

### 3.3 Decision：「一键跳转」按钮实现

**裁决**：**方案 A（采纳）** = 列表行 row-action `<action link="/Erp{Target}-main?filter_{fk}=${id}"/>`，目标列表页通过 URL 参数 + `asideFilterForm` / `filterForm` 接收上下文筛选。

**范式来源**：`nop-entropy/nop-code/.../dashboard.view.xml:36-37`（`link="/nop/code/pages/code-browser/main?indexId=${id}"`）。

**状态守卫**：所有「创建下游单据」按钮均加 `visibleOn`（如 PO「创建入库单」按钮 `visibleOn="${approveStatus == 'APPROVED' && docStatus != 'CANCELLED'}"`，复用 F1 visibleOn 范式）。

**否决方案 B** = 弹窗确认 + 跨域 `__save` 调用（跨域写操作应由用户在目标页确认）。

### 3.4 Decision：copy-line-from-order 实现

**裁决**：**方案 B（fallback，与 Explore 3.1 一致）** = 编辑态头表单 `lines` cell 上方追加 `<cell id="copyFromXxx" custom="true" notSubmit="true">` + `<gen-control>` 返回 AMIS button。

**button 行为**：
1. `actionType="dialog"` 打开 source picker dialog
2. dialog body 内嵌 AMIS `picker`（source = 源头实体 `picker.page.yaml`）+ AMIS `crud`（multi-select，源 = 源头实体行 `__findPage` + `filter_xxxId`）
3. dialog actions 的 confirm 通过 `onEvent.confirm.actions.setValue` 将所选行映射填入头 form 的 `lines` 数组（materialId / quantity / unitPrice / uoMId / taxRate 复制；lineNo 重新生成；amount 由行内 onEvent 推算）

**否决方案 C** = 后端新增 `copyLinesFromOrder` `@BizMutation`（Non-Goal，本范式不改后端；性能问题触发时再上 successor plan）。

## 4. 反模式自检

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 关联单据区调用跨域 `@BizQuery` Finder（已核实不存在） | 用 `fixedProps` 子表 + `__findPage` filter（code-gen 自动生成 filter 接收） |
| 「一键跳转」用 `__save` 跨域写（绕过用户确认） | 用 `link` URL 跳转 + 目标页用户确认 |
| copy-line 按钮挂在 `<grid id="sub-grid-edit">` 内的 headerToolbar（grid.xdef 不支持） | 挂在头表单 `<cell custom="true">` + `<gen-control>` |
| copy-line 后端新增 `@BizMutation` | 前端 `setValue` 映射 + 既有 `__save` 持久化 |
| 导航按钮无 visibleOn 状态守卫 | 所有创建下游单据按钮必加 `visibleOn` 状态守卫 |
| 详情页底部嵌独立 `<crud>` tabs 容器 | 用 row-action drawer + ref-xxx.page.yaml（F12 tabs 范畴之外） |

## 5. URL filter 降级路径（已合并到 §8，本节为索引）

详细降级路径与实施证据见 §8「URL filter 降级路径（含实施证据）」。

## 6. 各域落地实体清单

| 域 | 头实体 | 关联单据 drawer | 一键跳转 link | copy-line 子表 |
|----|--------|----------------|---------------|----------------|
| purchase | ErpPurOrder | ref-order.page.yaml in ErpPurReceive (fixedProps=orderId) | 「创建入库单」→ `/ErpPurReceive-main?filter_orderId=${id}`（visibleOn approved） | ErpPurReceive 子表「从订单导入」+ ErpPurInvoice 子表「从入库导入」 |
| sales | ErpSalOrder | ref-order.page.yaml in ErpSalDelivery (fixedProps=orderId) | 「创建出库单」→ `/ErpSalDelivery-main?filter_orderId=${id}`（visibleOn approved） | ErpSalDelivery 子表「从订单导入」+ ErpSalInvoice 子表「从出库导入」 |
| inventory | ErpInvStockMove | ref-move.page.yaml in ErpInvStockLedger (fixedProps=moveId) | 「查看流水」→ `/ErpInvStockLedger-main?filter_moveId=${id}` | — |
| mfg | ErpMfgWorkOrder | ref-work-order.page.yaml in ErpMfgMaterialIssue + ErpMfgJobCard (fixedProps=workOrderId) | 「查看完工入库」`link="/ErpInvStockMove-main"` **降级路径**（mfg→inv 跨域非 FK，Closure Evidence 命名实体 = `row-view-completion-move-button`） | — |

## 7. 实施证据（2026-07-20 Phase 2-4 落地）

**purchase 域**（4 文件）：
- `module-purchase/erp-pur-web/.../ErpPurOrder.view.xml` — 新增 `row-create-receive-button`（link + visibleOn approved）+ `row-view-receive-button`（drawer）
- `module-purchase/erp-pur-web/.../ErpPurReceive/ErpPurReceive.view.xml` — `<form id="edit">` 追加 `copyFromOrder` custom cell + AMIS gen-control button → dialog + picker + setValue 映射
- `module-purchase/erp-pur-web/.../ErpPurInvoice/ErpPurInvoice.view.xml` — `<form id="edit">` 追加 `copyFromReceive` custom cell（同型）
- `module-purchase/erp-pur-web/.../ErpPurReceive/ref-order.page.yaml` — 新建（fixedProps=orderId）

**sales 域**（4 文件）：
- `module-sales/erp-sal-web/.../ErpSalOrder.view.xml` — 新增 `row-create-delivery-button` + `row-view-delivery-button`
- `module-sales/erp-sal-web/.../ErpSalDelivery/ErpSalDelivery.view.xml` — `<form id="edit">` 追加 `copyFromOrder` cell
- `module-sales/erp-sal-web/.../ErpSalInvoice/ErpSalInvoice.view.xml` — `<form id="edit">` 追加 `copyFromDelivery` cell
- `module-sales/erp-sal-web/.../ErpSalDelivery/ref-order.page.yaml` — 新建（fixedProps=orderId）

**inventory 域**（2 文件）：
- `module-inventory/erp-inv-web/.../ErpInvStockMove.view.xml` — 新增 `row-view-ledger-button`（drawer）+ `row-view-ledger-link-button`（link）
- `module-inventory/erp-inv-web/.../ErpInvStockLedger/ref-move.page.yaml` — 新建（fixedProps=moveId）

**manufacturing 域**（3 文件）：
- `module-manufacturing/erp-mfg-web/.../ErpMfgWorkOrder.view.xml` — 新增 3 按钮：`row-view-material-issue-button`（drawer）+ `row-view-job-card-button`（drawer）+ `row-view-completion-move-button`（link 降级）
- `module-manufacturing/erp-mfg-web/.../ErpMfgMaterialIssue/ref-work-order.page.yaml` — 新建（fixedProps=workOrderId）
- `module-manufacturing/erp-mfg-web/.../ErpMfgJobCard/ref-work-order.page.yaml` — 新建（fixedProps=workOrderId）

**验证证据**：
- `mvn clean install -DskipTests`（4 域 -pl -am 各 BUILD SUCCESS）
- `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0
- `mvn test -pl app-erp-all`（含 ErpAllWebPagesTest PAGE_ERROR_COUNT=0 + ErpAllWebPagesCollectTest + TestErpAllJobYamlLoading + TestApplicationConfig）4 测试全绿
- `npx playwright test tests/e2e/business-actions/cross-doc-navigation.action.spec.ts` 5 用例全绿（PO drawer 目标可达 + Receive copy-line 持久化行数+materialId+quantity 断言 + Invoice copy-line picker 源可达 + SO drawer 目标可达 + StockMove link 目标可达）

## 8. URL filter 降级路径（含实施证据）

**当目标列表页非同域时**（如 mfg → inv 跨域 ErpInvStockMove）：`fixedProps` 子表 drawer 仍可用（子表页是独立 page.yaml，与父实体解耦），但**若需嵌入更多过滤维度**（如 mfg WorkOrder → inv StockMove 按 `sourceBillCode` 字符串匹配，非 FK）：

- 降级为单纯 `link` URL 跳转：`<action link="/ErpInvStockMove-main"/>`（无 filter，用户手动筛）
- 或在子表 drawer 内用 `initApi` + `filter` 显式构造 GraphQL query（复杂度高，本范式不实现，归 successor）

**已实施域**（本范式 Phase 4）：
- purchase / sales / inventory：FK 关联，使用 `fixedProps` 完整路径
- manufacturing：mfg → inv 跨域非 FK 关联（WorkOrder → 完工入库 StockMove 按 `sourceBillCode`），降级为单纯 `link` URL 跳转 + Closure Evidence 命名实体 `row-view-completion-move-button`（见 `ErpMfgWorkOrder.view.xml`）

## 9. 反模式自检（更新）

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 关联单据区调用跨域 `@BizQuery` Finder（已核实不存在） | 用 `fixedProps` 子表 + `__findPage` filter（code-gen 自动生成 filter 接收） |
| 「一键跳转」用 `__save` 跨域写（绕过用户确认） | 用 `link` URL 跳转 + 目标页用户确认 |
| copy-line 按钮挂在 `<grid id="sub-grid-edit">` 内的 headerToolbar（grid.xdef 不支持） | 挂在头表单 `<cell custom="true">` + `<gen-control>` |
| copy-line 后端新增 `@BizMutation` | 前端 `setValue` 映射 + 既有 `__save` 持久化 |
| 导航按钮无 visibleOn 状态守卫 | 所有创建下游单据按钮必加 `visibleOn` 状态守卫 |
| 详情页底部嵌独立 `<crud>` tabs 容器 | 用 row-action drawer + ref-xxx.page.yaml（F12 tabs 范畴之外） |
| `<action>` 内用 `<tooltip>` 子元素（xdef 不支持） | `<action tooltip="...">` 用属性（xdef:action.xdef:14） |
| mfg → inv 跨域按字符串字段（sourceBillCode）当作 FK 用 fixedProps | 降级为单纯 link 跳转，Closure Evidence 命名实体记录 |

## 10. 参考文件

- `../nop-entropy/docs-for-ai/03-runbooks/build-related-drawer-page.md` — fixedProps drawer 子表范式权威手册
- `../nop-entropy/docs-for-ai/03-runbooks/add-page-business-action.md` — row-action button + visibleOn 范式
- `docs/design/child-table-editor-patterns.md` — 子表行内编辑基础（F4 P0），copy-line-from-order 在其上叠加源头导入按钮
- `docs/design/picker-patterns.md` — picker 接线基础（F4 Phase 1）
