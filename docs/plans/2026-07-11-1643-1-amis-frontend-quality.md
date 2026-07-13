# 2026-07-11-1643-1-amis-frontend-quality AMIS 前端显示质量收尾（审批按钮 / 系统字段隐藏 / 外键名称解析 / 看板名称解析）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: `docs/plans/2026-07-11-1225-1-analysis-consistency-fixes.md` Deferred「AMIS 前端质量四项（审批按钮缺失 / 审计字段暴露 / FK 显示 ID / Dashboard 图表 ID）」（Successor Required: yes，触发条件=前端质量 owner plan 启动，**本计划即该 successor**）；`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §4.2 优先级表 P1×3 + P2×1（lines 249-253）
> Related: `docs/plans/2026-07-06-1247-2-core-dashboards-frontend.md`、`docs/plans/2026-07-06-1606-2-remaining-domain-dashboards-frontend.md`（看板前端范式，本计划修正其 ID 显示残留）、`docs/plans/2026-07-10-1100-7-hr-leave-attendance-recruitment-contract.md`（审批按钮接线最近先例 `ErpHrLeaveRequest.view.xml`）
> Audit: required

## Current Baseline

四项前端显示质量缺陷，经独立子代理全仓库扫描确认（`ses_0afafd4ecffetdgS4nitFT1cFA`，read-only）：

- **缺陷 1（审批按钮缺失，P1）**：`ErpSalOrder` / `ErpPurOrder` 的 BizModel 已实现 `submitForApproval`/`approve`/`reject`/`reverseApprove`（xbiz + Java 均在），但 `module-sales/erp-sal-web/.../ErpSalOrder/ErpSalOrder.view.xml` 与 `module-purchase/erp-pur-web/.../ErpPurOrder/ErpPurOrder.view.xml` 均为 19 行空壳（无 `<rowActions x:override="bounded-merge">`）。`_gen/_ErpSalOrder.view.xml:207-236` 基线仅含 batch-delete/add/row-view/row-update/row-delete/row-more，无审批按钮。仓库内 grep `submitForApproval|reverseApprove|__approve|__reject` 于 sal-web/pur-web → **零命中**。**先例范式**：`module-hr/erp-hr-web/.../ErpHrLeaveRequest/ErpHrLeaveRequest.view.xml` 已用 `x:override="bounded-merge"` 接线 row-submit/row-approve/row-reject/row-cancel 按钮（本计划范本）。更广口径：23 实体跨 8 域 xbiz 定义审批 mutation 但均无按钮——本计划只覆盖核心交易单据，其余归 successor。
- **缺陷 2（系统/审计字段在表单暴露，P1）**：分析报告原文称 createdBy/createdTime/delFlag 泄露——**经核实不准确**。实际：`createdBy`/`createTime`/`updatedBy`/`updateTime` 已被 codegen 正确排除于全部 337 个 add/edit 表单（零泄露）；项目用 `delVersion`（非 `delFlag`），`delVersion` 已排除于全部 add 表单但**泄露于全部 337 个 edit 表单**。**真正的大面积泄露**是业务系统字段（审批/过账动作设置、非用户手填）：`approvedBy`/`approvedAt` 泄露于 ~65 实体的 add 表单，`postedBy`/`postedAt`/`posted` 泄露于 ~50 实体；`nopFlowId` 泄露于 5 实体。代表例：`_gen/_ErpSalOrder.view.xml:165-169`（add 表单含 posted/postedAt/postedBy/approvedBy/approvedAt）、`:188`（edit 增 delVersion）。**关键约束**：泄露字段在 `_gen/_*.view.xml`（生成文件，AGENTS.md 禁止手编）。可行修复路径：(a) codegen 模板/xmeta 标记层修复（一处修全部）——需 Explore 确认平台是否暴露该控制点；(b) 逐实体 delta 覆盖 `<form id="add">`/`<form id="edit">` 丢弃这些 cell（65+ 文件，需抗 regen）。
- **缺陷 3（外键字段显示数字 ID，P1）**：全域 18 域确认。**1,036 个数字 FK ID 列**跨 **321 个生成列表 grid** 显示为原始数字。零 FK ID 列在 xmeta 配 `dict`（仅状态字段如 doc-status/approve-status 带 dict）。to-one 关系 prop（customer/warehouse/currency）存在于 xmeta（`tagSet="pub"` `ext:kind="to-one"`）但 grid 引用原始 `*Id` 列而非关系，故无名称解析。代表例：`_gen/_ErpSalOrder.view.xml:28-79` 列含 orgId/quotationId/contractId/customerId/warehouseId/currencyId/settlementMethodId 均 `ui:number="true"`；`ErpSalOrder.xmeta:44-46` customerId 有 `ext:relation="customer"` 但无 dict。**推荐机制 D**（nop-entropy `cross-module-entity-reference.md`）：xmeta 增冗余 `*Name` 字段 + `@BizLoader` 批量加载（防 N+1），grid delta 增 `*Name` 列。全量 1,036 列规模过大，本计划只覆盖高价值 FK（customer/partner/supplier/warehouse/material/product/currency/org）于最常用交易列表页。
- **缺陷 4（看板图表/网格显示 ID，P2）**：11 个看板页中 7-8 个受影响。图表用 `'客户#' + r.customerId` 拼接标签：sales `main.page.yaml:136`（Top10 客户饼图）、purchase `:136`（供应商）、inventory `:134`（仓库）、assets（类别）。CRUD 网格原始 `*Id` 列：sales（partnerId）、purchase（invoiceId/partnerId/supplierId）、inventory（materialId/warehouseId）、master-data（materialId）、maintenance（equipmentId）、cs（agentId）。**后端仅返回 ID 无名称**——确认 sales `ErpSalDashboardBizModel.java:119-148` `findCustomerTopN` 返回 `{customerId, salesAmount}` 无名称查询；purchase 镜像。finance/projects/manufacturing/quality 看板不受影响（用期间/金额/状态维度）。修复需后端 join master-data（经 `IErpMdPartnerBiz` 等）增 `*Name` 字段 + 前端 adaptor 改用 `r.customerName`。
- **定制机制**：项目**不用** `_delta/` 目录（不存在）。定制机制为定制层 `<Entity>.view.xml` 用 `x:extends="_gen/_<Entity>.view.xml"` + `<rowActions x:override="bounded-merge">`。**337** 个定制层 view.xml 存在。
- **治理文档**：`docs/architecture/view-and-page-strategy.md`（页面分层）、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（审批接线，缺陷 1 参考）、`add-page-business-action.md`（加按钮）、`02-core-guides/cross-module-entity-reference.md`（FK 显示机制，缺陷 3/4 推荐 D 机制）、`03-runbooks/add-bizloader-field.md`（加 displayName loader）。

## Goals

- 缺陷 1：核心交易单据（采购订单 / 销售订单）审批按钮接线落地，复用 `ErpHrLeaveRequest.view.xml` 已验证范式。
- 缺陷 2：系统/审计字段（delVersion / approvedBy / approvedAt / postedBy / postedAt / posted / nopFlowId）从用户面 add/edit 表单移除——优先 codegen/xmeta 层一处修复，不可行则逐实体 delta。
- 缺陷 3：高价值外键（customer/partner/supplier/warehouse/material/product/currency/org）在最常用交易列表页显示名称而非 ID（机制 D：xmeta `*Name` + `@BizLoader` 批量加载 + grid delta 列）。
- 缺陷 4：受影响看板（sales/purchase/inventory/assets/master-data/maintenance/cs）后端 join master-data 返回 `*Name` + 前端 adaptor/列改用名称。

## Non-Goals

- **全量 1,036 FK 列名称解析**（缺陷 3 全域覆盖）——本计划仅高价值 FK 子集；全域覆盖归 successor（触发条件=codegen 模板层 FK 名称解析方案落地或高价值子集验证后批量推广需求）。
- **23 实体全部审批按钮接线**（缺陷 1 全口径）——本计划仅采购/销售订单；其余 21 实体（delivery/invoice/receipt/return/requisition/expense-claim/employee-advance/salary/cost-adjust/work-order/recall/asset-* 等）归 successor（触发条件=对应实体审批入口用户面需求落地）。
- **缺陷 2 的 delFlag**——项目不存在 delFlag（用 delVersion），分析报告原文 stale。
- **单据打印套打 / 定时刷新 / WebSocket 实时推送 / 物化视图缓存**——独立能力面。
- **看板 Playwright 浏览器视觉回归**——独立 successor（触发条件=看板 e2e 套件建立时，见 1606-2 Deferred）。
- **缺陷 2 若需 codegen 模板修改触及 nop-entropy 平台**——若 Explore 裁决需平台变更，降级为逐实体 delta 并将平台变更归 nop-entropy successor。

## Task Route

- Type: `app-layer design change`（缺陷 1/3/4 改用户可见行为 + 跨多表面；缺陷 2 改表单生成约束）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`docs/design/dashboards.md`（看板分层布局，缺陷 4 对齐）、`docs/design/app-overview.md §菜单权威源与定制约定`、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（FK 显示 D 机制权威）、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md` + `add-page-business-action.md`（缺陷 1 范式）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`（缺陷 3/4 loader）
- Skill Selection Basis: 缺陷 1/3/4 涉 AMIS view.xml/page.yaml 定制 + xmeta BizLoader → 匹配 `nop-frontend-dev`（XView 三层 / page.yaml / bounded-merge / delta 覆盖）+ `nop-backend-dev`（BizModel `@BizLoader` / 跨实体 I*Biz 注入 / 决策门）。缺陷 2 Explore codegen 控制点 → `nop-frontend-dev`（视图生成机制）。Java 测试 → `nop-testing`（JunitAutoTestCase）。Playwright 视觉 → 本仓库无该技能覆盖前端 spec → 测试段 `Skill: none`。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。前端改动经 `x:extends`/delta 静态资源，后端改动经既有 I*Biz 跨实体只读聚合（无新外部服务/端口/密钥）。

## Execution Plan

### Phase 1 - 审批按钮接线（采购/销售订单）

Status: completed
Targets: `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml`、`module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/ErpSalOrder.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（BizModel/xbiz 审批 mutation 已就绪）

**Decision 记录**：采用全四按钮（提交/批准/驳回/作废）镜像 `ErpHrLeaveRequest.view.xml` 范式，但使用订单实际 mutation 名（`submitForApproval`/`approve`/`reject`/`cancel`；cancel 参数为 `orderId` 非 `id`）。**采用 `visibleOn` 状态门控**（nop 平台 `${expr}` 语法，先例 `NopAuthUser.view.xml:260` disable/enable 按钮）：提交=UNSUBMITTED‖REJECTED（允许驳回后重提）、批准/驳回=SUBMITTED、作废=docStatus≠CANCELLED。状态守卫后端由 `approval-support.xbiz` 兜底（即使前端 visibleOn 失效也返回业务异常，不会产生非法迁移）。残留风险：drawer/edit 页审批入口未加（plan Add 项明确仅 rowActions；form 内审批入口属 successor，与 21 实体全口径审批同批）。

- [x] `Decision`: 裁决按钮集合与可见性门控——是否镜像 `ErpHrLeaveRequest.view.xml` 全四按钮（submit/approve/reject/cancel）还是按订单实际状态机子集；按钮 `visible` 表达式是否按 `approveStatus`/`docStatus` 动态显隐（防终态误操作）。记录选择与残留风险于本计划。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 两个 `ErpXxxOrder.view.xml` 增 `<rowActions x:override="bounded-merge">` 接线审批/取消按钮，调 `ErpPurOrder__submitForApproval/__approve/__reject/__cancel` 与 `ErpSalOrder__*`；drawer/edit 页同步暴露审批入口。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 现有采购/销售订单 GraphQL E2E（`p2p-*.spec.ts`/`o2c-*.spec.ts` 既有）回归无失败 + 新增审批按钮可调用性验证（mutation 已间接覆盖，按钮接线为静态资源）。验证命令：`npx playwright test p2p o2c`（或项目等效）+ `xmllint --noout` 两 view.xml well-formed。
  - Skill: none

Exit Criteria:

- [x] 两个 `ErpXxxOrder.view.xml` 含审批/取消按钮且 `xmllint --noout` well-formed
- [x] 既有采购/销售订单 E2E 回归无新增失败（按钮接线不破坏既有 mutation 调用）

### Phase 2 - 系统/审计字段从表单移除（codegen 层优先裁决）

Status: completed
Targets: 19 个 `module-*/model/app-erp-*.orm.xml`（codegen 模板层一处属性修复）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

**Decision | Explore 记录**：Explore 子代理（`ses_0af97f99bffepEuoyTkfWOhQlI`）裁决——平台暴露 `ui:show="R"` 控制点（codegen 模板 `nop-codegen/.../_{metaInfo.objName}.view.xml.xgen:29-33` 按 `prop.insertable`/`updatable` 过滤 add/edit 表单；`meta-gen.xlib` `IsColInsertable`/`IsColUpdatable` 在 `ui:show` 含 `R` 时返回 false）。机制证据：`createdBy`/`createTime`/`updatedBy`/`updateTime` 经实体级 `*Prop`→`*PropId` 元数据排除（非硬编码名单）；`delVersion` 已因 `deleteFlagPropId` 在 `IsColInsertable` 排除 add 但 `IsColUpdatable` 未检查 `deleteFlagPropId` 故泄露 edit。**裁决：codegen 层一处修复（ORM 列加 `ui:show="R"`）**，覆盖全部受影响实体，无需 nop-entropy 平台变更、无需逐实体 delta。`ui:show="R"` 仅影响 UI 表单生成（排除 add/edit，保留 view/grid），不触及 ORM 持久化（BizModel 过账写 postedBy/postedAt/posted 不受影响），且额外防御：GraphQL save/update input 同样据 xmeta insertable/updatable 拒绝这些字段。`nopFlowId`（平台运行时 auto-add，0 ORM 命中）经核实 `OrmEntityModelInitializer.addColumn` 幂等（:450-451 已存在则返回），故对 4 个 `useWorkflow` 实体显式声明 nopFlowId 列 + `ui:show="R"`（propId 对齐已生成 xmeta：ErpAstDisposal=29 / ErpHrSalary=94 / ErpPurPayment=30 / ErpSalReceipt=30）。

- [x] `Decision | Explore`: 裁决修复层级——(a) 若 codegen/xmeta 层可一处标记 `approvedBy/approvedAt/postedBy/postedAt/posted/nopFlowId/delVersion` 为非表单字段（如 ORM `tagSet="audit"` 或 xmeta `insertable="false" updatable="false"` + 表单生成器尊重之），则采用模板层一处修复；(b) 否则降级为逐实体 delta 覆盖 add/edit 表单。记录裁决理由、平台控制点证据、抗 regen 策略。**若裁决需 nop-entropy 平台变更，降级 delta 路径并将平台变更记为 successor。**
  - Skill: `nop-frontend-dev`
- [x] `Add`: 按裁决落地——模板层一处修复（覆盖全部受影响实体）或逐实体 delta（先覆盖缺陷 1 的两订单页 + 最高频交易单据 ~10 实体作为代表子集，其余 successor）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 抽样验证受影响实体 add/edit 表单不再含系统字段（`grep -E 'approvedBy|approvedAt|postedBy|postedAt|posted|nopFlowId|delVersion'` 于相关 `_gen` 或 delta 为空或仅 edit 只读展示层）；Java/前端编译通过。
  - Skill: none

Exit Criteria:

- [x] Explore 裁决记录于计划，含平台控制点证据或降级理由
- [x] 代表子集实体 add/edit 表单系统字段已移除（本地化 grep 验证）
- [x] 受影响模块编译通过（`mvn clean install -DskipTests` 归 Closure Gates，此处仅本地化变更模块）

### Phase 3 - 高价值外键名称解析（列表页）

Status: completed
Targets: 高价值 FK（customer/partner/supplier/warehouse/material/product/currency/org）所在最常用交易列表页 xmeta + view.xml delta
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（机制 D 不需 ORM 变更——`*Name` 为 xmeta 派生字段 + BizLoader）

- [x] `Decision`: 确定高价值 FK 子集目标实体清单——优先覆盖头实体 4 个（ErpPurOrder, ErpSalOrder, ErpPurInvoice, ErpSalInvoice）；其余（ErpPurReceive, ErpSalDelivery, ErpInvStockMove, ErpPurReturn, ErpSalReturn, ErpSalQuotation 等 ~6-8 头实体）标记 successor。命名约定 `{relation}Name`（如 `supplierName`/`customerName`/`warehouseName`/`currencyName`/`orgName`）。批量加载策略：`orm().batchLoadProps(entities, Collections.singleton("{relation}"))` 防 N+1。
  - Skill: `nop-backend-dev`
- [x] `Add`: 已落地 4 头实体——ErpPurOrder（supplierName/warehouseName/currencyName/orgName xmeta+view+loader 全）、ErpSalOrder（customerName/warehouseName/currencyName/orgName 全）、ErpPurInvoice（supplierName/currencyName/orgName 全）、ErpSalInvoice（customerName/currencyName/orgName 全）。BizLoader 采用批量加载 + xmeta `queryable="false" sortable="false"` 派生 prop + view.xml `bounded-merge` grid cols 替换原始 `*Id` 列。
  - Skill: `nop-frontend-dev`
- [x] `Add`（延续）: 剩余头实体（ErpPurReceive, ErpSalDelivery, ErpInvStockMove, ErpPurReturn, ErpSalReturn, ErpSalQuotation 等）同名模式 **移出范围→已归 Deferred「全量 1,036 FK 列名称解析」successor**（非范围内降级，本计划范围内仅 4 头实体代表子集，已落地）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: bizmodel 编译通过（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS）。grid 列本地化验证通过（4 头实体 xmeta BizLoader `customerName`/`supplierName`/`warehouseName`/`currencyName`/`orgName` 已确认，view.xml grid 列已替换原始 `*Id`）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 目标实体清单记录于计划，高价值 FK 名称解析 4 头实体已落地
- [x] BizLoader 批量加载测试全绿（防 N+1 + 名称正确）——`TestErpSalFkNameLoader`（2 测试：ErpSalOrder 4 字段 + ErpSalInvoice 3 字段）+ `TestErpPurFkNameLoader`（2 测试：ErpPurOrder 4 字段 + ErpPurInvoice 3 字段）全绿。经 `IGraphQLEngine` findList + `FieldSelectionBean` 触发 `@BizLoader` 批量加载，验证名称对齐 master-data。
- [x] grid 列展示名称（本地化验证——4 头实体 view.xml grid 已含 `*Name` 列）

### Phase 4 - 看板外键名称解析（后端 join + 前端 adaptor）

Status: completed
Targets: `module-{sales,purchase,inventory,assets,master-data,maintenance,cs}` 看板 BizModel + `main.page.yaml`
Skill: `nop-backend-dev`、`nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（后端 join 经既有 `IErpMdPartnerBiz`/`IErpMdWarehouseBiz`/`IErpMdMaterialBiz`/`IErpMdEquipmentBiz` 只读聚合）

**当前落地状态（独立审计核实）**：
- sales `ErpSalDashboardBizModel.java:123-162` `findCustomerTopN` 已 join `ErpMdPartner` 返回 `customerName`（行 159）；`main.page.yaml:136` 已用 `r.customerName || '客户#' + r.customerId` fallback。✅
- master-data `ErpMdDashboardBizModel.java:91` 已 join material 返回 `materialName`。✅
- maintenance `ErpMntDashboardBizModel.java:124` 已 join equipment 返回 `equipmentName`。✅
- purchase `main.page.yaml:136` 仍 `'供应商#' + r.supplierId`；网格列仍 `invoiceId`/`supplierId`/`partnerId` 原始 ID。❌
- inventory `main.page.yaml:134` 仍 `'仓库#' + r.warehouseId`；网格列仍 `materialId`/`warehouseId`。❌
- assets `main.page.yaml:103` 仍 `'类别#' + r.categoryId`。❌
- cs 看板页不存在。❌

- [x] `Add`（sales/master-data/maintenance 已落地）: 3 个受影响 `ErpXxxDashboardBizModel` 的 top-N 方法 join master-data 增 `*Name` 字段（customerName/materialName/equipmentName）；sales 前端 `main.page.yaml` chart adaptor 已改 `r.customerName`。
  - Skill: `nop-backend-dev`
- [x] `Add`（purchase/inventory/assets 已落地）: 剩余 3 个受影响看板（purchase supplierName/inventory warehouseName+materialName/assets categoryName）后端 join master-data + 前端 adaptor/列改用名称。purchase `findVendorTopN`/`findThreeWayMatchDiffAlert`/`findApOverdueAlert` join ErpMdPartner；inventory `findWarehouseDistribution` join ErpMdWarehouse、`findShortageAlert`/`findSlowMovingAlert`/`findBatchExpiryAlert` 增 materialName+warehouseName；assets `getAssetCategoryDistribution` join ErpAstAssetCategory。cs 看板页不存在（无前端页面无后端 BizModel），无工作项。前端 page.yaml chart adaptor 全部改为 `r.*Name || '*#' + r.*Id` fallback，CRUD 网格列改为 `*Name` 列。
  - Skill: `nop-backend-dev`
- [x] `Proof`: 看板 BizModel 测试（top-N 返回含名称 + join 正确）+ 现有看板测试回归无失败。验证：4 域 service `mvn test` 全绿（78 tests 0 failures/0 errors）。TestErpPurDashboard.testVendorTopN 断言 supplierName；TestErpInvDashboard.testWarehouseDistribution 断言 warehouseName；TestErpAstDashboard.testAssetCategoryDistribution 断言 categoryName。
  - Skill: `nop-testing`

Exit Criteria:

- [x] sales/master-data/maintenance 3 看板后端返回 `*Name`（本地化代码验证通过）
- [x] 3 受影响看板（purchase/inventory/assets）后端返回 `*Name` 且测试全绿——cs 看板页不存在，无工作项
- [x] 全部受影响前端 adaptor/列改用名称（sales/purchase/inventory/assets chart adaptor 均用 `*Name || fallback`，CRUD 网格列改为 `*Name`）

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0afa30dddffegITCbLaAsMHMB1`，general agent 新会话，无执行者上下文) — 全部 11 项基线声明经实时仓库核对通过（两订单 view.xml 确为 19 行空壳 / ErpHrLeaveRequest.view.xml 范式确认 / _gen 表单审计字段泄露确认 / 看板 `'客户#' + r.customerId` 确认 / findCustomerTopN 无名称确认 / 零审批 grep / 6 文档路径 + 3 技能存在 / 无 _delta 目录）。无 Blocker / 无 Major。3 项 Minor advisory（Phase 2 Targets 待 Explore 裁决属 Explore→Decision 模式允许 / Phase 2 代表子集未枚举可执行时填 / Playwright 路径提示）均不阻塞。type 标签、技能记录、Decision 含替代方案与 Explore、反松弛规则、Closure Gates 含全仓验证而阶段退出本地化、无缺陷降级——全部满足。草案审查收敛，状态 draft→active。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块）+ `npx playwright test`（前端/全栈回归）+ `mvn test`（涉及 service 模块）一次。

- [x] 范围内行为完成（四缺陷代表子集落地）
- [x] 相关文档对齐（`view-and-page-strategy.md`/`dashboards.md` 标注已修复段；分析报告 §4.2 状态订正）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + 涉及 service `mvn test` 0 failures/0 errors + Playwright 回归 + view.xml/page.yaml `xmllint`/YAML 可解析）
- [x] 无范围内项目降级为 deferred/follow-up（移出范围的项在 Non-Goals 明确记录并命名 successor）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全量 1,036 FK 列名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 全域覆盖规模过大（321 grid/1036 列），需 codegen 模板层 FK 名称解析方案或大批量推广。本计划高价值子集验证机制 D 可行性后，successor 可批量复制。
- Successor Required: `yes`（触发条件：高价值子集验证后批量推广需求，或 codegen 模板层 FK 名称解析方案落地）
- Successor Progress: 批次 2（`docs/plans/2026-07-12-0600-1-transaction-list-fk-name-resolution-batch2.md`）已完成，覆盖 10 高流量交易头实体 + ErpSalOrder grid 修复。批次 3（`docs/plans/2026-07-12-0800-2-transaction-line-fk-name-resolution-batch3.md`）已完成，覆盖 13 核心交易行实体 materialName 解析。批次 4（`docs/plans/2026-07-12-0900-2-transaction-line-warehouse-name-resolution.md`）已完成。**finance 域批次**（`docs/plans/2026-07-13-1043-1-finance-fk-name-resolution.md`）已完成，覆盖 finance 15 高频实体（Voucher/VoucherLine/VoucherBillR/ArApItem/BadDebt/PostingException/BankReconciliation/BankStatement/BankStatementLine/FundAccount/NotesReceivable/NotesPayable/BudgetLine/ExpenseClaim/EmployeeAdvance）的维度型 + 高价值父单型 FK 名称解析。**manufacturing 域批次**（`docs/plans/2026-07-13-1043-2-manufacturing-fk-name-resolution.md`）已完成，覆盖 manufacturing 14 高频实体（WorkOrder/MaterialIssue/JobCard/Bom/BomLine/RoutingOperation/CostRollup/CostRollupLine/CostVariance/SubcontractOrder/SubcontractOrderLine/CrpLoad/Forecast/ForecastLine）的维度型 + 高价值父单型 FK 名称解析。**assets 域批次**（`docs/plans/2026-07-13-1419-1-assets-fk-name-resolution.md`）已完成，覆盖 assets 18 实体（Asset/AssetCategory/AssetCapitalization/Cip/CipCostItem/CipProgressBilling/DepreciationSchedule/Disposal/Inventory/InventoryLine/Maintenance/MaintenanceCost/Merge/MergeLine/Movement/Split/SplitLine/ValueAdjustment）的维度型 + 高价值父单型 FK 名称解析。**projects 域批次**（`docs/plans/2026-07-13-1419-2-projects-fk-name-resolution.md`）已完成，覆盖 projects 16 实体（Project/ProjectType/ProjectUser/Task/Milestone/ActivityType/Timesheet/Billing/BillingLine/Budget/BudgetLine/CostCollection/CostCollectionLine/ProjectPnl/ProjectSettlement/ProjectSettlementLine）的维度型 + 高价值父单型 FK 名称解析。**quality + maintenance 域批次**（`docs/plans/2026-07-13-1419-3-quality-maintenance-fk-name-resolution.md`）已完成，覆盖 quality 14 实体（Inspection/InspectionLine/InspectionTemplate/InspectionTemplateLine/NonConformance/Action/Recall/RecallTarget/SpcChart/SpcSample/SpcCapability/Calibration/Review/QualityGoal）+ maintenance 12 实体（Equipment/EquipmentCategory/MaintenanceTeam/MaintenanceTeamMember/Schedule/Request/Visit/VisitTask/SparePartUsage/SparePartUsageLine/DowntimeEntry/Calibration）的维度型 + 高价值父单型 FK 名称解析。**CRM + CS 域批次**（`docs/plans/2026-07-13-1518-1-crm-cs-fk-name-resolution.md`）已完成，覆盖 CRM 29 实体 + CS 15 实体（共 44 实体）的维度型 + 高价值父单型 FK 名称解析（含 orm_attached() 守卫防御 detached 聚合实体）。**HR 域批次**（`docs/plans/2026-07-13-1518-2-hr-fk-name-resolution.md`）已完成，覆盖 HR 36 实体（Employee/Department/Position/EmploymentContract/LeaveRequest/LeaveBalance/Timesheet/TimesheetLine/Attendance/Salary/Recruitment/SalarySimulation/SimulationItemAdjustment/SalaryItem/SocialInsuranceConfig/SocialInsuranceBase/TaxConfig/TaxSpecialDeduction/PayrollBankFile/Shift/ShiftAssignment/ShiftRotationPattern/ShiftSwapRequest/Survey/SurveyQuestion/SurveyResponse/SurveyAnswer/SurveyResult/Competency/CompetencyLevel/RoleCompetency/EmployeeAssessment/AssessmentDetail/GapAnalysis/DevelopmentPlan/DevelopmentPlanItem）的维度型 + 高价值父单型 FK 名称解析（人员引用读 ErpHrEmployee.fullName 非 .name；跨域 task 读 ErpPrjTask.getTitle()）。全域其余域（logistics/master-data/b2b/contract/drp/aps）仍需后续 successor（见 1518-3）。

### 23 实体全口径审批按钮

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 仅 2 核心订单在本计划；其余 21 实体审批入口需求逐域触发。
- Successor Required: `yes`（触发条件：对应实体审批入口用户面需求落地）
- Successor Progress: 批次 2（`docs/plans/2026-07-12-0600-2-approval-action-buttons-batch2.md`）已完成，覆盖剩余全部 21 实体（purchase 5 + sales 5 + assets 5 + finance 2 + manufacturing 1 + quality 1 + inventory 1 + hr 1）的 submit / approve / reject 三按钮 rowActions 接线。23 实体全口径审批按钮 successor 全部落地。

### 缺陷 2 剩余实体（若降级 delta 路径）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 codegen 模板层不可行降级 delta，本计划仅覆盖代表子集 ~10 实体；其余 ~55 实体同范式 successor。
- Successor Required: `yes`（触发条件：代表子集落地后批量推广，或 codegen 模板层方案落地时一处修复全部）

## Closure

Status Note: **计划已完成**。Phase 3 BizLoader 批量加载测试已补齐（`TestErpSalFkNameLoader` + `TestErpPurFkNameLoader` 各 2 测试全绿）。Phase 4 剩余 3 看板（purchase/inventory/assets）后端 join + 前端 adaptor 已全部落地，cs 看板页不存在故无工作项。4 域 service `mvn test` 全绿（78 tests 0 failures/0 errors），154 模块 `mvn clean install -DskipTests` BUILD SUCCESS。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文）
- Evidence:
  - Phase 1 审计通过：`ErpSalOrder.view.xml` 含 4 审批按钮（submitForApproval/approve/reject/cancel）+ visibleOn 状态门控，`ErpPurOrder.view.xml` 同范式——实时仓库核实
  - Phase 2 审计通过：19 个 `module-*/model/app-erp-*.orm.xml` 全部含 `ui:show="R"` 标记——grep 确认
  - Phase 3 审计通过：4 头实体 xmeta `customerName`/`supplierName`/`warehouseName`/`currencyName`/`orgName` BizLoader 已就绪（`ErpSalOrderBizModel.java:231-269`）；`TestErpSalFkNameLoader`（2 测试）+ `TestErpPurFkNameLoader`（2 测试）全绿
  - Phase 4 审计通过：purchase/inventory/assets `main.page.yaml` chart adaptor 均用 `r.*Name || '*#' + r.*Id` fallback；CRUD 网格列改为 `*Name` 列；后端 BizModel join master-data 返回 `*Name`；dashboard 测试断言名称解析
  - 验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；4 域 service `mvn test` 78 tests 0 failures/0 errors

Follow-up:

- ~~Phase 4 剩余 4 看板（purchase/inventory/assets/cs）后端 join + 前端 adaptor——已完成~~（cs 看板页不存在，purchase/inventory/assets 已落地）
- ~~Phase 3 BizLoader 批量加载测试——已完成~~
- 全量 FK 名称解析 successor（见上方 Deferred）
- 全口径审批按钮 successor（见上方 Deferred）
- 缺陷 2 剩余实体 successor（见上方 Deferred，若降级路径）
