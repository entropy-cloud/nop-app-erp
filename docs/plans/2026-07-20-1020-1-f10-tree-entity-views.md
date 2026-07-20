# 2026-07-20-1020-1-f10-tree-entity-views F10 — 树形实体视图（4 实体 tree CRUD 页面）

> Plan Status: completed
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` §F10（树形实体视图 P2 — todo）
> Related: `docs/plans/2026-07-19-1818-1-f4p1-high-frequency-picker.md`（ErpMdSubject picker 已有 `isLeaf=1` 过滤，本计划升级为 tree-select）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 已为 ErpMdSubject 等树形实体落地 form 分组，本计划保留 form 不动，仅改造 grid 与 page 层）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-20，独立子代理审查后修正）：

- **F10 实际范围 = 4 个真树形实体**（经 ORM 核实有 `parentId` self-FK 列）。原 roadmap §F10 表列 6 实体，经实时仓库逐个核实：
  - ✅ `ErpMdMaterialCategory`（`module-master-data/erp-md-web`，ORM 含 `parentId` 列 ✓）
  - ✅ `ErpMdSubject`（`module-master-data/erp-md-web`，ORM 含 `parentId` + `isLeaf` 列 ✓；凭证录入科目 picker 已有 `isLeaf=1` 过滤）
  - ✅ `ErpHrDepartment`（`module-hr/erp-hr-web`，ORM 含 `parentId` 列 ✓）
  - ✅ `ErpCsServiceCatalogItem`（`module-cs/erp-cs-web`，ORM 含 `parentId` 列 ✓；含 `categoryId`/`ticketTypeId`/`slaPolicyId`/`fulfillmentProcessId` 等业务字段）
  - ❌ `ErpMfgBom`（**不在本计划范围**）：经核实 ORM **无 `parentId` self-FK 列**；其层级关系经子行实体（`ErpMfgBomLine` 通过 `bomId` 反向）表达，非自引用树；标准 tree CRUD 范式（`@TreeChildren` + `filter_parentId=__null`）不适用。归 F16 BOM 复杂手写页面 successor（多级展开/折叠 + phantom 节点图标 + 工艺路线流向图）。
  - ❌ `ErpAstAssetCategory`（**不在本计划范围**，原 roadmap `ErpAstCategory` 命名错误，实际实体名为 `ErpAstAssetCategory`）：经核实 ORM **无 `parentId` 列**；资产类别是扁平分类表（`code`/`name`/5 个科目 FK 字段），非树形结构。无 tree CRUD 语义。
- **当前页面结构 = codegen 默认 CRUD**（实时核实 4 实体手写层 view.xml）：所有 4 实体 `<pages>` 段均为 `<crud name="main"/>` 默认引用，使用 `<grid id="list">` 平铺分页表格，**未配置 `tree-list` grid、未声明 `@TreeChildren` selection、未配置 `loadDataOnce` 与 `pager="none"`、无 `add-child` simple page**。
- **当前 picker = 默认扁平 picker**（核实 `ErpMdSubject/picker.page.yaml` + `ErpMdMaterialCategory/picker.page.yaml`）：codegen 标准 wrapper 引用 view.xml `<grid id="pick-list"/>`，**未启用 tree-select**。
- **后端 `__findList` 是 Nop Platform `CrudBizModel` 内置 @BizQuery**（`../nop-entropy-wt/nop-entropy-feat-agent/nop-service-framework/nop-biz/src/main/java/io/nop/biz/crud/CrudBizModel.java:1528-1532`）：`public List<T> findList(@Optional @Name("query") QueryBean query, FieldSelectionBean selection, IServiceContext context)` 自动暴露于所有继承 `CrudBizModel<T>` 的 BizModel，无需手写。本计划无需新增后端方法。支持 `filter_parentId=__null` URL 参数 + `@TreeChildren(max:N)` GraphQL selection 嵌套返回子节点。
- **后端 ORM 关系模型已就绪**：4 实体均含 `parentId` self-FK 列；`ErpMdSubject` 额外含 `isLeaf` 标志位用于科目叶子判定，picker 已过滤。
- **Nop Platform 树形 CRUD 范式已文档化**（`../nop-entropy/docs-for-ai/03-runbooks/build-tree-crud-page.md`）：
  1. `<grid id="tree-list" x:prototype="list"><selection>children @TreeChildren(max:5)</selection></grid>`
  2. `<crud name="main" grid="tree-list"><table loadDataOnce="true" sortable="false" pager="none"><api url="@query:Xxx__findList/{@listSelection}?filter_parentId=__null"/></table></crud>`
  3. `<simple name="add-child" form="add"><data><parentId>$id</parentId></data></simple>` + rowActions `row-add-child-button`
- **真实参考实例可用**：`../nop-entropy-wt/nop-entropy-feat-agent/nop-auth/nop-auth-web/src/main/resources/_vfs/nop/auth/pages/NopAuthResource/NopAuthResource.view.xml` 完整示范 `@TreeChildren` + tree-select 父节点选择器 + `add-child` simple page + visibleOn `resourceType != 'TOPM'` 父节点约束。
- **F3 P0 已为 4 实体落地 `<form id="view">` + `<form id="edit">` + `<form id="query">` 分组**（核实 `ErpMdSubject.view.xml:73-132` 含 baseInfo/auxiliary/control/audit 4 组），本计划保留 form 不动。
- **前置验证基线**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；F1/F3/F4 P0/F4 P1/F5/F6/F8/F9 plan 已全绿。

## Goals

1. **4 树形实体 main.page.yaml 接线 tree-list grid + tree CRUD**：每实体 view.xml 新增 `<grid id="tree-list" x:prototype="list"><selection>children @TreeChildren(max:5)</selection></grid>`；`<crud name="main" grid="tree-list">` 含 `loadDataOnce="true"` + `sortable="false"` + `pager="none"` + `filter_parentId=__null`。
2. **4 树形实体 rowActions 新增 `row-add-child-button`**：触发 `<simple name="add-child">` 弹窗，预填 `parentId=$id` 上下文。
3. **4 树形实体 `edit/add` 表单 `parentId` 字段升级为 tree-select 控件**：经 `<cell id="parentId"><gen-control><tree-select clearable="@:true"><source><url>@query:Xxx__findList/{@listSelection}?filter_parentId=__null&filter_id__ne=$id</url></source></tree-select></gen-control></cell>` 嵌入 AMIS tree-select 下拉，显示完整树形结构供父节点选择（替代当前扁平 input-table 弹窗 picker），URL `filter_id__ne=$id` 排除自身防循环引用。
4. **4 树形实体 picker.page.yaml 升级为 tree picker**：`<picker name="picker"><table loadDataOnce="true" sortable="false" pager="none"><api url="@query:Xxx__findList/{@listSelection}?filter_parentId=__null"/></table></picker>`，沿用 tree-list grid 让用户在弹窗中以树形结构浏览选择节点。
5. **每树形实体保留其域专用业务约束**：
   - `ErpMdSubject` 保留 `isLeaf=1` 过滤于凭证录入科目 picker（完成 F4 P1 inventory plan Deferred「树形科目 picker」successor 触发条件）
   - `ErpCsServiceCatalogItem` 保留 `ticketTypeId` + `slaPolicyId` + `fulfillmentProcessId` 业务字段（F12 工单详情目录树 successor 依赖）
6. **Playwright visual spec 扩展**：新建 `tests/e2e/visual/tree-entity-views.visual.spec.ts` 含 4 用例，每实体 1 用例覆盖：tree grid 渲染 + 展开/折叠 + add-child 弹窗 + parentId tree-select 控件 + parentId 根节点过滤 URL。
7. **更新 `docs/design/<domain>/ui-patterns.md`**：每域新增「树形 CRUD 范式」段落固化列集 + tree-select 配置 + add-child simple page + 域专用业务约束；新建 `docs/design/tree-entity-patterns.md` 跨域范式参考（对齐 `child-table-editor-patterns.md` 文档结构）。
8. **完成 F4 P1 inventory plan Deferred「树形科目 picker」successor 触发条件**（plan `2026-07-20-0629-1` 显式列出 F10 为 finance voucher 前置之一；本计划完成后 F4 finance voucher plan 的科目 tree-select 前置就绪）。

## Non-Goals

- **ErpMfgBom 树形 CRUD**——经核实 ORM 无 `parentId` 列，层级经子行实体表达，标准 tree CRUD 范式不适用；归 F16 BOM 复杂手写页面 successor（多级展开 + phantom 节点 + 工艺路线流向）。
- **ErpAstAssetCategory 树形 CRUD**——经核实 ORM 无 `parentId` 列，资产类别为扁平分类表（非树形结构）；roadmap §F10 原列此项为命名错误（实际实体 `ErpAstAssetCategory`）+ 范围错误（非树形），本计划显式排除并建议 roadmap 更正。
- **修改后端 BizModel action**——`__findList` 已由 `CrudBizModel` 内置（`CrudBizModel.java:1528-1532`）；本计划不改后端 Java，仅 view.xml + page.yaml + AMIS 层定制。
- **修改 ORM 模型**——保护区域，`parentId` self-FK 列已存在；本计划不动模型。
- **F11 批量操作**（树形批量审批/批量启用停用子树）——F11 plan 范畴；本计划仅做单节点 CRUD + tree-select 选择。
- **F12 详情页 tabs 容器**——F12 范畴；本计划 4 实体仅做树形 CRUD，不做嵌套 tabs。
- **F13 树形可视化**（如组织架构图节点嵌入员工缩略）——F13 范畴；本计划使用 AMIS 内置 tree 表格不做自定义可视化。
- **F16 组织架构图复杂页面**（节点含员工嵌入 + 搜索高亮）——F16 P2 范畴；本计划 ErpHrDepartment 仅做标准 tree CRUD。
- **跨树拖拽节点**（修改 parentId 经拖拽）——AMIS tree 表格不支持原生拖拽，归 watch-only residual。
- **树形权限继承**（如部门权限自动继承到子部门）——业务规则范畴，非前端 UI。
- **i18n `i18n-en:`**——F15 范畴；本计划使用中文 label。
- **像素级视觉回归**——独立 plan `2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md` 覆盖。

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F10
  - `docs/design/master-data/ui-patterns.md`（物料分类 + 科目树业务语义）
  - `docs/design/human-resource/ui-patterns.md`（组织架构树业务语义）
  - `docs/design/customer-service/ui-patterns.md`（服务目录树业务语义）
  - `docs/architecture/view-and-page-strategy.md`（view.xml grid 与 page 层结构）
  - `../nop-entropy/docs-for-ai/03-runbooks/build-tree-crud-page.md`（树形 CRUD 范式权威文档）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（crud grid 引用 + page-level table 配置）
  - 平台源码参考：`../nop-entropy-wt/nop-entropy-feat-agent/nop-service-framework/nop-biz/src/main/java/io/nop/biz/crud/CrudBizModel.java:1528-1532`（`findList` 内置 @BizQuery 实现）
  - 真实 view.xml 参考：`../nop-entropy-wt/nop-entropy-feat-agent/nop-auth/nop-auth-web/src/main/resources/_vfs/nop/auth/pages/NopAuthResource/NopAuthResource.view.xml`（完整示范 `@TreeChildren` + tree-select + add-child）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml grid 定制 + crud grid 引用 + tree-select gen-control + simple page add-child + AMIS tree 表格）；不改后端 Java（`__findList` 为 `CrudBizModel` 内置），故不加载 `nop-backend-dev`；visual spec 扩展需 `nop-testing`。

## Infrastructure And Config Prereqs

- 手写层 view.xml 路径（4 实体）：
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdMaterialCategory/ErpMdMaterialCategory.view.xml`
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdSubject/ErpMdSubject.view.xml`
  - `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrDepartment/ErpHrDepartment.view.xml`
  - `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsServiceCatalogItem/ErpCsServiceCatalogItem.view.xml`
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新展开 page.yaml
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置（`tests/e2e/playwright.config.ts`）
- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — 范式裁决与每实体 tree 业务约束冻结

Status: completed
Targets: `docs/design/tree-entity-patterns.md`（新建跨域范式参考）+ `docs/design/<domain>/ui-patterns.md`（每域新增树形 CRUD 段落）
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 在 plan 内记录每树形实体的业务约束与 tree 列集（约束式表格）。最少列集：`code` + `name` + `parentId` + 域专用 ≤ 3 字段。基于实时仓库 ORM 字段核实：
  - **ErpMdMaterialCategory**（已核实 view.xml grid 含 `code` `name` `parentId` `sortNum` `priceValidationLevel` 等列）：`code` `name` `parentId` `sortNum` `priceValidationLevel`
  - **ErpMdSubject**（已核实 view.xml grid 含 `code` `name` `parentId` `subjectClass` `direction` `isLeaf` `status` 等列）：`code` `name` `parentId` `subjectClass` `direction` `isLeaf` `status`；picker 保留 `isLeaf=1` 过滤（凭证录入只选叶子科目）
  - **ErpHrDepartment**（已核实 view.xml grid 含 `code` `name` `parentId` `manager` `costCenterId` `orgId`）：`code` `name` `parentId` `manager` `costCenterId` `orgId`
  - **ErpCsServiceCatalogItem**（已核实 ORM 字段）：`code` `name` `parentId` `ticketTypeId` `slaPolicyId` `fulfillmentProcessId` `isActive` `sequence`
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策 tree-select 父节点选择器是否过滤自身（避免循环引用）：
  - **方案 A（采纳）**：edit/add `parentId` tree-select URL 加 `filter_id__ne=$id` 排除自身（防节点成为自身父节点）；AMIS tree-select 不支持原生 `excludeSelf`，需 URL filter 实现
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策 add-child simple page 表单继承策略：
  - **方案 A（采纳）**：`<simple name="add-child" form="add">` 复用既有 `<form id="add">` + `<data><parentId>$id</parentId></data>` 显式注入父节点上下文（对齐 `build-tree-crud-page.md §3` + NopAuthResource.view.xml:172-178 范式）
  - Skill: `nop-frontend-dev`
- [x] `Decision`: 决策 tree grid 默认展开层级（`@TreeChildren(max:N)`）：
  - **方案 A（采纳）**：`max:5` 对齐 NopAuthResource 范式（5 级足够覆盖物料分类/科目/部门等典型层级）
  - Skill: `nop-frontend-dev`
- [x] `Add`: 新建 `docs/design/tree-entity-patterns.md` 固化跨域范式：tree-list grid 模板 + tree-select 父节点控件模板 + add-child simple page 模板 + 4 实体列集表 + 域专用业务约束（对齐 `child-table-editor-patterns.md` 结构作为未来 P3/ext 域 tree 实体参考）；附录记录 ErpMfgBom/ErpAstAssetCategory 排除原因
  - Skill: `nop-frontend-dev`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] `docs/design/tree-entity-patterns.md` 文件存在且包含 4 实体列集表 + 范式模板段落 + ErpMfgBom/ErpAstAssetCategory 排除附录
- [x] 每域 `docs/design/<domain>/ui-patterns.md` 新增「树形 CRUD 范式」段落（3 域：master-data 共用一份段落覆盖 MaterialCategory/Subject、human-resource、customer-service）

### Phase 2 — 4 实体 view.xml tree-list grid + tree CRUD 接线

Status: completed
Targets: 4 实体 view.xml + main.page.yaml（codegen 重新展开）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy` (4/4 items tagged Add)
- Prereqs: Phase 1 决策冻结

- [x] `Add`: 4 实体 view.xml 各新增 `<grid id="tree-list" x:prototype="list"><selection>children @TreeChildren(max:5)</selection></grid>`（在已有 `<grid id="list">` 之后平行声明，不影响 list grid）
  - Skill: `nop-frontend-dev`
- [x] `Add`: 4 实体 view.xml 各修改 `<pages><crud name="main" grid="tree-list">` + 内嵌 `<table loadDataOnce="true" sortable="false" pager="none"><api url="@query:Xxx__findList/{@listSelection}?filter_parentId=__null"/></table></crud>`
  - Skill: `nop-frontend-dev`
- [x] `Add`: 4 实体 view.xml 各新增 `<pages><simple name="add-child" form="add"><data><parentId>$id</parentId></data></simple></pages>`
  - Skill: `nop-frontend-dev`
- [x] `Add`: 4 实体 view.xml 各修改 `<form id="edit">` + `<form id="add">` 的 `<cell id="parentId">` 升级为 `<gen-control><tree-select clearable="@:true"><source><url>@query:Xxx__findList/{@listSelection}?filter_parentId=__null&filter_id__ne=$id</url></source></tree-select></gen-control>`
  - Skill: `nop-frontend-dev`
- [x] `Add`: 4 实体 view.xml 各修改 `<crud name="main"><rowActions>` 在既有 rowActions 基础上追加 `<action id="row-add-child-button" level="primary" label="新增子节点">` 触发 simple/add-child 弹窗（label 中文，F15 i18n successor）
  - Skill: `nop-frontend-dev`
- [x] `Add`: 4 实体 view.xml 各修改 `<pages><picker name="picker">` 内嵌 `<table loadDataOnce="true" sortable="false" pager="none"><api url="@query:Xxx__findList/{@listSelection}?filter_parentId=__null"/></table></picker>`，picker grid 引用 `tree-list` 让用户在弹窗中以树形选择
  - Skill: `nop-frontend-dev`
- [x] `Add`: ErpMdSubject picker.page.yaml（凭证录入科目 picker）保留 `isLeaf=1` 过滤，但 grid 升级为 tree-list 让用户在树形结构中浏览叶子节点（`filter_isLeaf=1` URL 过滤 + tree-list grid 显示层级上下文）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 4 实体 view.xml 均含 `<grid id="tree-list">` + `<crud name="main" grid="tree-list">` + `<simple name="add-child">` + `<cell id="parentId">` tree-select gen-control
- [x] 修改后运行 `mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS（codegen 重新展开 page.yaml 不报错）
- [x] `mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` PAGE_ERROR_COUNT=0（codegen page 展开校验）

### Phase 3 — Playwright visual spec + 文档对齐

Status: completed
Targets: `tests/e2e/visual/tree-entity-views.visual.spec.ts`（新建）+ 各域 ui-patterns.md 更新
Skill: `nop-frontend-dev | nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 view.xml 接线完成

- [x] `Add`: 新建 `tests/e2e/visual/tree-entity-views.visual.spec.ts` 含 4 用例（每实体 1 用例），断言：
  - tree grid 渲染（DOM 含 tree-table 结构 + 展开/折叠图标）
  - 根节点列表（`filter_parentId=__null` URL 守卫 + `__findList` 端点被调用 + 响应非空）
  - rowActions 含 `row-add-child-button` 按钮
  - add-child simple page 弹窗打开后 `parentId` 字段已预填且为 tree-select 控件
  - picker 弹窗 tree grid 渲染（DOM 断言对齐 `dashboards.visual.spec.ts` 范式）
  - Skill: `nop-testing`
- [x] `Proof`: 4 用例全部 PASS（`npx playwright test tree-entity-views.visual.spec.ts`）
  - Skill: `nop-testing`
- [x] `Add`: 更新 `docs/design/master-data/ui-patterns.md` + `human-resource/ui-patterns.md` + `customer-service/ui-patterns.md`，每域新增「树形 CRUD 范式」段落引用 `tree-entity-patterns.md` 范式 + 列集表 + 域专用约束
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] `tests/e2e/visual/tree-entity-views.visual.spec.ts` 文件存在且 4 用例 PASS
- [x] 3 域 ui-patterns.md 各含「树形 CRUD 范式」段落

## Draft Review Record

- Independent draft review iteration 1: needs-revision（ses_082a49d29ffeq3p35LAO0TN2Qe，2026-07-20）——发现 5 blocking issues：(B1) 假基线声称 6 实体均有 parentId（实际 ErpMfgBom 无）；(B2) ErpMfgBom 不属 tree CRUD 结果面（应用子行实体表达层级）；(B3) ErpAstCategory 命名错误（实际 ErpAstAssetCategory）+ 该实体也无 parentId；(B4) `__findList` 内置性未引证；(B5) F4 dependency 措辞过强。iteration 2 修订：缩减至 4 真树形实体（剔除 ErpMfgBom + ErpAstAssetCategory，记录于 Deferred）；补充 `CrudBizModel.java:1528-1532` 内置引证；F4 措辞限定为「successor 触发条件就绪」非「finance voucher plan 可启动」。
- Independent draft review iteration 2: acceptable-as-is（ses_0828d6c23ffeWSNBWFFONvEPOk，2026-07-20）——B1-B5 全部 resolved；4 实体 parentId 列经 ORM 重新核实（master-data.orm.xml:251,820,832 + hr.orm.xml:350 + cs.orm.xml:697）；ErpMfgBom/ErpAstAssetCategory 排除经 ORM 核实（mfg.orm.xml:193-230 + ast.orm.xml:262-326）；CrudBizModel.java:1528-1532 引证准确；无新 blocking issues。Plan Status 翻转为 active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次 `mvn clean install -DskipTests` + `mvn -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest test` + `npx playwright test tree-entity-views.visual.spec.ts`。

- [x] 范围内行为完成（4 实体 tree CRUD + tree-select + add-child + tree picker）
- [x] 相关文档对齐（`docs/design/tree-entity-patterns.md` + 3 域 ui-patterns.md 段落）
- [x] 已运行验证（`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0 + `npx playwright test tree-entity-views.visual.spec.ts` 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ErpMfgBom 多级 BOM 展开/折叠图（F16 P1 复杂页面）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经 ORM 核实 ErpMfgBom 无 `parentId` self-FK 列；其层级关系经子行实体（`ErpMfgBomLine` 通过 `bomId` 反向）表达，非自引用树；roadmap §F16 明确将「BOM 树浏览（多级展开/折叠、phantom 节点图标、工艺路线水平流向图）」列为 P1 复杂手写页面，需自定义可视化组件。F16 BOM 复杂页面 successor 不基于本计划 tree-list grid（不同结果面），需独立设计
- Successor Required: yes（F16 BOM 树浏览 plan 启动时另起独立设计）

### ErpAstAssetCategory（roadmap §F10 原列范围错误）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经 ORM 核实 ErpAstAssetCategory（原 roadmap 错记为 `ErpAstCategory`）无 `parentId` 列；资产类别为扁平分类表（5 个科目 FK + 折旧方法），非树形结构。本计划建议 roadmap 维护者在下次更新时将 ErpAstAssetCategory 从 §F10 移除
- Successor Required: no

### ErpHrDepartment 组织架构图节点嵌入员工（F16 P2 复杂页面）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap §F16 明确将「组织架构图（节点含员工嵌入、搜索高亮、点击跳转）」列为 P2 复杂手写页面，需自定义 AMIS 组件，本计划不引入
- Successor Required: yes（F16 组织架构图 plan 启动时基于本计划 ErpHrDepartment tree-list grid 扩展）

### 跨树拖拽节点修改 parentId

- Classification: `watch-only residual`
- Why Not Blocking Closure: AMIS 内置 tree 表格不支持原生拖拽；交互价值低（parentId 经 edit 表单修改已可用）；若未来 AMIS 升级支持拖拽或引入第三方 dnd 库可重新评估
- Successor Required: no

### 树形权限继承（部门权限自动继承到子部门）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 业务规则范畴（属于权限模型设计），非前端 UI 范畴
- Successor Required: no

## Closure

Status Note: 计划已通过独立草案审查 iteration 2 (acceptable-as-is) 并完成全 3 Phase 实施，独立结束审计会话已逐项核实实时仓库证据并接受关闭。Plan Status 翻转为 completed。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文，由 mission-driver closure-audit 路径触发）
- Audit Date: 2026-07-20
- Audit Session: 独立会话（fresh context，不携带 EXECUTE 执行者上下文）
- Live-repo verification (grep/glob/read against `./`):
  - `docs/design/tree-entity-patterns.md` 存在（9662 bytes）含 4 实体列集表 + tree-list/tree-select/add-child 范式模板 + ErpMfgBom/ErpAstAssetCategory 排除附录 ✓
  - 3 域 `ui-patterns.md`（master-data/customer-service/human-resource）各含「树形 CRUD 范式」段落并引用 `tree-entity-patterns.md` ✓
  - 4 实体 view.xml 各含 `<grid id="tree-list" x:prototype="list"><selection>children @TreeChildren(max:5)</selection></grid>` + `<crud name="main" grid="tree-list">` + `row-add-child-button`（label 中文「新增子节点」）+ `<simple name="add-child" form="add">` + edit/add 表单 `<cell id="parentId">` 的 `<tree-select>` gen-control：
    - `module-master-data/erp-md-web/.../ErpMdMaterialCategory.view.xml` ✓
    - `module-master-data/erp-md-web/.../ErpMdSubject.view.xml`（picker 含 `filter_isLeaf=1` 保留凭证录入叶子过滤 ✓）
    - `module-hr/erp-hr-web/.../ErpHrDepartment.view.xml` ✓
    - `module-cs/erp-cs-web/.../ErpCsServiceCatalogItem.view.xml` ✓
  - `tests/e2e/visual/tree-entity-views.visual.spec.ts` 存在（9977 bytes）含 4 实体 visual 用例 ✓
  - `docs/logs/2026/07-20.md` 含 F10 完整日志条目，记录 3 Phase 落地 + 154 模块 BUILD SUCCESS + `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0 + Playwright 12/12 PASS + 回归 28/28 PASS ✓
- Exit Criteria 复核：Phase 1/2/3 所有 `[x]` 项经实时仓库核实为真（非盲目信任勾选）
- Closure Gates 复核：8/8 项均 `[x]`（含本审计会话勾选的「结束审计由独立子代理执行」门控）
- 反空洞检查：4 实体 tree-list grid 经 `__findList` + `filter_parentId=__null` 在运行时实际接线（gql:selection 同时覆盖 URL 与 selection 属性，确保 AMIS 调用 findList 而非 findPage）；tree-select 控件、add-child simple page、row-add-child-button 均可达；无 `return null`/空函数体/吞异常占位
- Five-point consistency: Plan Status (`completed`) / 3 Phase Status (均 `completed`) / 各 Phase Exit Criteria (全 `[x]`) / Closure Gates (全 `[x]`) / Closure evidence (本段非占位) — 全部一致
- Deferred honesty: 5 项 Deferred But Adjudicated 均为非阻塞（ErpMfgBom/ErpAstAssetCategory 因 ORM 无 parentId 列、ErpHrDepartment 员工嵌入归 F16、跨树拖拽 AMIS 不支持、权限继承属业务规则），无在范围内的实时缺陷或契约漂移被隐藏为 deferred
- Pre-existing bugs noticed but out-of-scope（非 F10 引入，已在日志记录，归独立修复 plan，不阻塞本 plan 关闭）：`ErpMdMaterialCategory.priceValidationLevel_label` defaultValue 与 dict 不匹配；`ErpHrDepartment.manager` to-one col 缺 sub-selection — 已在 `docs/logs/2026/07-20.md` 第 9 行记录

Follow-up:

- F4 P1 inventory plan Deferred「树形科目 picker」successor 触发条件已就绪（ErpMdSubject tree-select 已落地）
- F12 工单详情目录树 successor 依赖已就绪（ErpCsServiceCatalogItem tree CRUD）
- F16 ErpHrDepartment 组织架构图复杂页面 successor 可基于本 plan tree-list grid 扩展
- 修复 ErpMdMaterialCategory.priceValidationLevel defaultValue / ErpHrDepartment.manager to-one sub-selection（独立 plan，不阻塞本 plan）
