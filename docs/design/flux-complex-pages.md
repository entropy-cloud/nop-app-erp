# Flux 复杂页面实现设计：nop-chaos-flux 控件落地方案

> 状态：**已实施**（P1-P4 全部落地，`2026-08-03-1232-1/2/3/4`；本节为实施后一致版本，`2026-08-03-1232-5` Phase 3 回填）
> 范围：nop-app-erp 30 个核心复杂页 + 附属形态（见 `docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md`）的 Flux DSL 实现方案
> 前置分析：`docs/analysis/2026-07-11-flux-integration-strategy-analysis.md`（集成策略）、`docs/analysis/2026-07-20-complex-ui-controls-inventory-for-flux.md`（控件盘点）、`docs/backlog/frontend-ui-roadmap.md`（Flux 全量迁移决策）
> Flux 权威文档：`~/app/nop-chaos-flux/flux-guide/`（入口 `README.md`，cookbook 在 `design-patterns/`，类型定义在 `flux-types/*.d.ts`）

---

## 1. 背景与设计目标

### 1.1 问题：AMIS 复杂页面的降级现状（历史，已全部消除）

> **实施后状态（`2026-08-03-1232-5` Phase 3 回填）**：下表所述 AMIS 降级已**全部由 flux 原生组件消除**（P2 `2026-08-03-1232-2` F13 + P3 `2026-08-03-1232-3` F16 + P4 `2026-08-03-1232-4` 占位页）。下表保留作历史背景参考，当前权威见 §3 控件映射总表。

nop-app-erp 前端原全部由 AMIS 渲染。深度分析（`2026-08-03-1000`）确认：30 个核心复杂页面均已实现，但存在**组件级降级**：

| 页面 | AMIS 实现（已废弃） | 降级原因 | flux 最终形态 |
|------|----------|---------|---------|
| crm/cs 时间线（timeline.page.yaml） | each + tpl 手写 | 原生 `type: timeline` prop 契约经 service scope 失败 | flux `timeline`（P2） |
| crm 活动日历（calendar.page.yaml） | 按日分组卡片网格 | 原生 `type: calendar` React 渲染报错（#130） | flux `calendar`（P2） |
| hr 组织架构图（org-chart.page.yaml） | each + tpl 缩进列表 | AMIS tree 渲染器不适配 | flux `tree`（P2） |
| aps 排产甘特（schedule-gantt.page.yaml） | echarts custom series **只读** | AMIS 无拖拽/缩放组件 | flux `gantt`（P3，拖拽/缩放/依赖连线） |
| 合同版本对比（version-diff.page.yaml） | 元数据表 + `<pre>` 并排 | 数据模型无字段级 diff（此降级保留，与引擎无关） | flux `diff-view`（content 双栏，字段级 diff 仍受数据模型限制） |
| 3 个看板（prj/cs/crm kanban） | 列式 crud + row-action 按钮 | AMIS 无拖拽卡片 | flux `kanban`（P2，拖拽） |

另有 16 个占位页面（SPC 三件套等）与 4 个未实现设计项（资产处置向导等）已由 P4 落地（12 flux 实现 + 4 Deferred 带触发条件）。

### 1.2 决策依据（Flux 全量迁移，2026-08-03 用户决策）

> **决策更新（`2026-08-03-1232-5` Phase 3 回填）**：本节原引用 roadmap「Flux 渲染引擎备选」段的旧决策——~~「标准 CRUD 页面继续用 view.xml → amis 路径（97.6% 页面是零定制继承桩，无迁移收益）」~~——已被用户 2026-08-03 决策推翻。**flux 为唯一权威渲染引擎，后续不再考虑 AMIS**。roadmap 已更新为「Flux 全量迁移」（见 `frontend-ui-roadmap.md`）。

`frontend-ui-roadmap.md`「Flux 全量迁移」段现行决策：

- `nop-web-site` 由 `nop-chaos-next` 打包，**flux 为唯一权威渲染引擎**（菜单全 19 域 `component="FLUX"`，`E2E_ENGINE` 缺省 flux）
- **标准 CRUD 页面经 view.xml + flux-web.xlib 零修改输出 flux JSON**（`nop.web.render-mode=flux` 动态替换 5 个 Gen* 标签，354 CRUD + 352 picker 全量 0 错误）
- **复杂页面（F13/F16 等）以 flux DSL 编写 page.yaml/flux.yaml**，获得拖拽/缩放等原生交互能力
- AMIS schema 页经 flux amis-compat 通路仍可渲染（过渡期）；AMIS 退役路径触发条件见 roadmap

### 1.3 本设计文档的目标

1. 说明 nop-entropy 如何以 flux 模式启动与渲染（§2）
2. 给出 30 个核心复杂页 → flux 控件的**完整映射**（§3）
3. 逐类给出**实现设计**（schema 骨架 + 数据契约 + 事件持久化）（§4）
4. 定义**数据联动模式**与**迁移优先级**（§5/§6）
5. 登记**未决问题**（§7）

---

## 2. Flux 渲染机制：nop-entropy 侧能力

### 2.1 模式开关与页面回退（P1 实测结论，`2026-08-03-1232-1`）

| 机制 | 位置 | 说明 |
|------|------|------|
| 渲染模式配置 | `WebConfigs.CFG_WEB_RENDER_MODE`（`nop.web.render-mode`，默认 `amis`） | 设为 `flux` 时强制使用 flux-web 渲染管线 |
| **render-mode 动态替换**（P1 实测） | `nop-web/.../xlib/web.xlib` 顶部 `x:post-extends` 引入 `impl_flux_mode.xpl` | `nop.web.render-mode=flux` 时，`GenPage/GenForm/GenGrid/GenInputTable/GenTable` 五标签被 `x:override=replace` 动态替换为 flux-web.xlib 版本——现有 page.yaml 的 `x:gen-extends: <web:GenPage .../>` **零修改**即输出 flux JSON |
| flux.yaml 文件类型 | `page.register-model.xml` 注册 `flux.yaml` | 与 page.yaml 平行 |
| 页面回退 | `PageModelLoaderFactory`：flux 模式下优先加载同名 `*.flux.yaml` | 若存在 `main.flux.yaml` 则优先于 `main.page.yaml` |
| 页面结构修正 | `WebPageHelper.fixPage` flux 分支 | 跳过 AMIS 特有处理（className 注入、group.body 归一化） |
| **菜单 component 翻转**（P1 实测） | `*.action-auth.xml` `component="AMIS"` → `component="FLUX"` | RouteRenderer 按 pageType 选渲染器：`pageType === 'flux'` → `FluxRouteEntry`（动态加载 flux 运行时）。全 19 域已翻转（0 AMIS 残留） |

> 启动方式：`-Dnop.web.render-mode=flux`（或 application.yaml 配置）。回退语义：**同目录同名 `*.flux.yaml` 优先**，不存在时仍加载 `*.page.yaml`（此时 page.yaml 中 `x:gen-extends` 必须指向 flux-web.xlib 才能输出 flux JSON）。P1 实测：354 标准 CRUD + 352 picker + 38 ref + 15 tabs 在 flux 模式 100% 输出合法 JSON（`ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0）。

### 2.2 渲染标签库（nop-entropy 已实现）

| 标签库 | 位置 | 内容 |
|--------|------|------|
| `flux-web.xlib` | `nop-web/.../xlib/flux-web.xlib`（1020 行） | GenPage/GenContainerModel/GenForm/GenGrid/GenAction/GenInputTable/GenTable 等 29 个标签 |
| `flux-control.xlib` | `nop-web/.../xlib/flux-control.xlib`（1182 行） | edit-tree-parent/edit-decimal/edit-short/edit-byte 等 domain→控件映射，输出 flux JSON（tree-select/input-number 等） |

`flux-web.xlib` 的容器分派（`GenContainerModel`）：

| UiContainerModel.type | 分派标签 | 输出 |
|----------------------|---------|------|
| `crud` | container_crud.xpl | flux crud |
| `simple` | container_simple.xpl | flux 简单页 |
| `tabs` | container_tabs.xpl | flux tabs |
| `wizard` | container_wizard.xpl | flux wizard |
| `group` | container_group.xpl | flux group |
| `picker` | — | **不支持**（Flux 无页面级 picker schema，配置抛 `nop.err.web.unknown-page-type`） |

> 含义：**既有 view.xml（tabs/wizard/group 结构）可经 flux-web.xlib 直接生成 flux JSON**，无需重写 view.xml；但页面级 picker 与手写 AMIS DSL（page.yaml 直写）不走此路径。

### 2.3 双栈路由（nop-chaos-next 侧）

- `menu-config.json` 菜单项带 `pageType` 字段（`builtin`/`amis`/`flux` 等），路由层按 pageType 分发到不同渲染器
- `RouteRenderer` 已按 `pageType === 'amis'` 走 AmisRouteRenderer；flux 分支加载 `*.flux.yaml` 产物交给 flux SchemaRenderer
- 宿主契约 `RendererEnv`：`fetcher`（ajax 统一出口）、`notify`/`confirm`/`alert`、`loadDict`/`loadPage`、`hasRole`、`functions`/`filters`（表达式扩展）、`importLoader` 等（`flux-guide/11-host-integration.md`）

### 2.4 三种页面编写路径（决策矩阵）

| 路径 | 适用 | 说明 |
|------|------|------|
| **view.xml + flux-web:GenPage** | 标准 CRUD、tabs/wizard/group 结构页 | 复用现有 view.xml，仅切换 xlib；97.6% 继承桩零修改 |
| **page.yaml 直写 flux schema** | 复杂页面（看板/甘特/日历/收件箱） | 本设计的核心路径；`x:gen-extends` 可继续用于 meta 推导 |
| **flux.yaml 文件** | 与 AMIS page.yaml 并存时 | flux 模式优先加载；适合「同页面双栈共存」过渡期 |

### 2.5 complex 页面类型（view.xml 模型化优先，2026-08-01 平台新增）

> 用户方向（2026-08-03）：**不删除 page.yaml，只新增 flux.yaml**；form/grid/页面整体布局尽量通过 view.xml 模型定义；利用 xview.xdef 新增的 complex 页面定义能力。
>
> **实际取舍（2026-08-06 核对）**：该方向在「模型可表达」范围内已实现——标准 CRUD/tabs/60 子表/4 树形全部 view.xml 模型驱动；但 complex 槽位**无法承载 flux 专有控件**（见 §7 #9 平台能力核对），故 20 个复杂页（F13/F16/wizard/占位页）整体以 page.yaml/flux.yaml 整页直写落地。这是平台能力边界下的正确实现，偏离裁决见 `docs/retrospectives/2026-08-06-1400-flux-page-organization-deviation.md`。

`xview.xdef` `<pages>` 在 crud/picker/simple/tabs/wizard/group 之外新增 **`<complex>`**（提交 f7c45373d，2026-08-01）：

```xml
<pages>
    <complex name="main" xdef:name="UiComplexPageModel">
        <header><simple name="hdr" form="header-form"/></header>
        <footer><simple name="ftr" form="footer-form"/></footer>
        <aside><crud name="aside-crud" grid="list"><table name="t"><api url="@query:X__findPage"/></table></crud></aside>
        <body><simple name="bdy" form="body-form"/></body>
    </complex>
</pages>
```

- **四槽位**：header/footer/aside/body（每槽位为 UiContainerModel 容器列表，可嵌套 crud/simple/tabs/wizard/group）
- **输出**：`flux-web/impl_GenPage.xpl` complex 分派 → `page_complex.xpl` 输出 Flux PageSchema（type=page + 四槽位，空槽位不输出）
- **group 容器**：已实现 GridSchema 映射（columns/gap/autoFlow/alignItems/justifyItems；responsiveColumns 暂不输出）
- **测试**：TestFluxWebGen complex + group 用例（17→18 tests）+ `test-flux-complex.view.xml` 夹具

**设计原则（本页的页面实现分层，2026-08-06 按实施实况修正）**：

| 层 | 定义位置 | 覆盖 |
|----|---------|------|
| 标准 CRUD / tabs / 头行子表 / 树形 | view.xml `<crud>`/`<tabs>` + `<forms>`/`<grids>` 模型经 flux-web.xlib 输出 | 354 CRUD + 352 picker + 15 tabs + 60 子表对 + 4 树形 |
| 复杂页外壳（筛选区/状态区/主体组合） | **page.yaml/flux.yaml 整页直写**（`type: page` + body 数组；筛选经 body 顶部 `form`+`data-source` 承担） | F13/F16/wizard/占位页（20 flux.yaml，P2/P3/P4） |
| flux 专有交互控件（gantt/kanban/calendar/timeline/tree/diff-view/wizard/steps） | page.yaml/flux.yaml 直写 | 看板/甘特/日历/时间线/BOM/组织树/版本对比/向导 |
| **`<complex>` 四槽位（2026-08-01 平台新增 f7c45373d）** | view.xml `<pages><complex>` 定义 header/footer/aside/body | **仅承载标准容器**（crud/simple/tabs/wizard/group）；flux 专有控件**无槽内透传点**，生产 0 使用（测试夹具 `test-flux-complex.view.xml` 验证平台能力） |
| flux.yaml 双文件共存 | 新增同名 `*.flux.yaml`（不删 page.yaml） | 需要 flux 专属定义的页面 |

> **实施裁决（2026-08-06，`docs/retrospectives/2026-08-06-1400-flux-page-organization-deviation.md`）**：原设想的「complex 外壳 + 槽位内嵌自定义控件」候选通道未成立——`GenContainerModel` 仅对 crud/simple/tabs/wizard/group 五种容器分派，`container_simple.xpl` 仅输出整 `<form>`，**不存在 `beforeForm`/`afterForm` 之类的自定义控件透传点**。故复杂页一律「整页 page.yaml/flux.yaml 直写」（即原逃生通道「整页直写 + complex 仅用于标准布局」）；complex 槽位保留为平台能力，待槽位 arbitrary-node 透传能力落地后再评估（见 §7 #9）。

---

## 3. 复杂页面 → Flux 控件映射总表

> 控件能力摘要来自 `flux-guide/design-patterns/`。数据契约统一 `{items, total}`；取数统一 `data-source`（name 发布）或 `loadAction`。

### 3.1 A. 经营看板 Dashboard（10 + 1）

| 页面 | 现有 AMIS 结构 | Flux 控件组合 |
|------|---------------|--------------|
| 域级看板 ×10（fin/qa/inv/mfg/prj/pur/sal/ast/mnt/md） | KPI 卡 + 趋势图 + 预警 crud | `data-source`（每指标独立，`dependsOn` 并行）+ `card` + `chart`（line/bar）+ `crud` + `status`/`mapping` |
| cs 绩效看板 | 同上 | 同上（+ `table` 只读） |

### 3.2 B. 特殊 dashboard 子页（10）

| 页面 | AMIS 实现（降级） | Flux 控件 | 关键交互 |
|------|------------------|-----------|---------|
| 排产甘特图 | echarts **只读** | **`gantt`** | 拖拽/缩放/依赖连线（原生） |
| 三单匹配 | 差异 crud + 三表并列 | `crud` ×3 + `mapping`（差异色）+ `card` | 筛选联动 |
| BOM 树 | 栈算法 + AMIS tree | **`tree`**（data 绑定，labelField/keyField 配置）| 全量展开；子级懒加载需改用字段级 `tree-select`（childrenSource）或 data-source 分页 |
| 组织架构图 | each+tpl 缩进 | **`tree`**（labelField/keyField 配置） | 折叠/展开 |
| 薪酬核算审批 | service 聚合 + 分组表 | `crud` + `input-table`（可编辑批改）+ `data-source` | 行内编辑 + 批量批准 |
| 发运追踪 | 双查询时间线 | **`timeline`**（items.time/title/detail）+ `data-source` ×2 | 纵向时间线 |
| 净需求报表 | 分组 + 嵌套 table | `data-source`（分组聚合）+ `loop` + `table` | 分组折叠 |
| 合同版本对比 | 元数据表 + pre | **`diff-view`**（oldContent/newContent）| split/unified 切换（字段级 diff 仍受数据模型限制） |
| ASN 流程跟踪 | 状态色块流程条 | **`steps`**（items.status 驱动）| 当前步骤高亮 |
| EDI 事务详情 | 双查询 + payload 开关 | `timeline`（状态流转）+ `table`（日志）+ `collapse`（报文） | 折叠展开 |

### 3.3 C. F13 非标准视图族（7）——降级消除

| 页面 | AMIS 降级 | Flux 控件 | 消除的降级 |
|------|----------|-----------|-----------|
| prj 任务看板 | 列式 crud | **`kanban`**（data 图结构 + onCardMove） | 拖拽 |
| cs 工单看板 | 列式 crud + SLA tpl | **`kanban`** + `status`（SLA 🔴）| 拖拽 + 卡片模板 |
| crm 商机看板 | 动态列 each | **`kanban`**（动态列配置） | 拖拽 |
| crm 活动时间线 | each+tpl | **`timeline`**（原生） | 原生渲染 |
| cs 操作时间线 | each+tpl | **`timeline`** | 原生渲染 |
| crm 活动日历 | 卡片网格 | **`calendar`**（view month/week/day + onEventChange/onEventCreate） | 原生日历 + 拖拽 |
| hr 休假日历 | 矩阵 table | **`calendar`**（resources 资源分组） | 资源视图 |

### 3.4 D. 向导（2）

| 页面 | AMIS 实现 | Flux 控件 |
|------|----------|-----------|
| 期末结账向导（416 行，全库最复杂） | 手写 page.yaml（select 共享 scope + ajax + adaptor） | **`wizard`**（steps + `valuesPath` 分区 + `formId` 校验闸 + `onComplete` 聚合提交 + `statusPath`） |
| 维护访问 4 步向导 | 手写 page.yaml | **`wizard`**（同上模式） |

### 3.5 E. 独有形态（4）

| 页面 | AMIS 实现 | Flux 控件 |
|------|----------|-----------|
| 通知收件箱（329 行） | `type: tabs` + 客户端 JS 过滤 | **`tabs`**（items + valueOwnership scope）+ `crud` ×3 + `data-source`（countUnread 轮询） |
| party-search 手写 picker | main.picker.page.yaml | **`picker`**（表单字段级）+ `tree-select`（若需树形） |
| 凭证↔单据双向联查 | 2 个联查页 | `crud` + `data-source`（findByRelatedBill 系） |
| 凭证快速模板 | dialog + input-table | `dialog` + `input-table`（模板行编辑） |

### 3.6 F. view.xml 层复杂视图（15 tabs + 60 子表对 + 4 树形）

| 形态 | Flux 控件 | 说明 |
|------|----------|------|
| tabs 视图 15（单据/档案） | **`tabs`**（items 数组，valueOwnership=scope 持久化） | view.xml 层 `<pages><tabs>` 或 `<layoutControl="tabs">` 经 flux-web.xlib 生成 |
| 头行单据子表 60 对 | **`input-table`**（columns + item 行内模板 + 行公式）或 **`combo`** | 行内公式见 `examples/business-document-formula.md`（`$Arr` 需宿主注册） |
| 树形主数据 4 | **`tree`** / **`tree-select`** | labelField/keyField/childrenKey 可配置 |

---

## 4. 逐类实现设计

### 4.1 经营看板模式（A）

**结构**：page → `data-source`（KPI/趋势/预警，`name` 发布）→ 顶区 `card` ×N（KPI）+ `chart`（趋势）+ `crud`（预警列表）。

```json
{
  "type": "page",
  "title": "销售看板",
  "body": [
    { "type": "data-source", "name": "kpi", "action": "ajax",
      "args": { "url": "/r/ErpSalDashboard__getDashboardKpi" } },
    { "type": "data-source", "name": "trend", "action": "ajax",
      "args": { "url": "/r/ErpSalDashboard__getDashboardTrend" } },
    { "type": "grid", "columns": [
        { "type": "grid-col", "span": 4, "body": [
            { "type": "card", "title": "销售额", "body": [{ "type": "text", "text": "${kpi.salesAmount}" }] } ] }
      ] },
    { "type": "chart", "chartType": "line", "source": "${trend}",
      "xAxis": { "dataKey": "date" }, "yAxis": { "label": "金额" } },
    { "type": "crud", "source": "${alerts}", "columns": [
        { "name": "status", "label": "状态", "type": "status",
          "labelMap": { "OVERDUE": "超期" }, "levelMap": { "OVERDUE": "error" } } ] }
  ]
}
```

- **KPI 卡**：`card` + `text`（数据绑定 `${kpi.xxx}`，缺失兜底 `?? "-"`）
- **多指标并行**：多个 `data-source` 互不阻塞；`dependsOn: ["mdFilter"]` + `sendOn` 实现筛选联动（`examples/master-detail.md` 模式）
- **预警列表**：`crud` + `status`/`mapping` 列（替代 gen-control 字典色块）
- **轮询**：`data-source.interval` + `stopWhen`（替代 AMIS 手动 refresh）

### 4.2 甘特图模式（aps 排产）

**数据适配**：后端 `ErpApsOperationOrder` 聚合查询返回 `{ tasks: [{id, text, start, end, type, progress, children}], links: [{source, target, type, lag}] }`（对齐 `aps/scheduling.md §8` JSON 契约）。

```json
{
  "type": "gantt",
  "tasks": "${ganttData.tasks}", "links": "${ganttData.links}",
  "defaultZoom": "week", "draggable": true, "editable": true, "linkable": true,
  "onTaskDragEnd": { "action": "ajax", "args": {
      "url": "/r/ErpApsOperationOrder__updateSchedule", "method": "post",
      "data": { "opOrderId": "${event.id}", "start": "${event.start}", "end": "${event.end}" } } }
}
```

- **持久化**：拖拽/编辑事件（onTaskDragEnd/onTaskDoubleClick/onLinkDragEnd）→ ajax mutation。P3 实测裁决：新增 `ErpApsOperationOrder__updateSchedule(opOrderId, start, end)` `@BizMutation`（宿主=ErpApsOperationOrderBizModel，与 start/complete/cancel 同宿主），**不复用** `IEtpApsGanttService.dragUpdateOperation` 单参 spec（该 spec 仅设计理想未实现）；端点不内联产能校验（实体无资源分配粒度，归排程引擎 successor）
- **撤销**：gantt 内部撤销栈 + `onMount` 时快照
- 只读场景（计划对比）：`draggable: false, editable: false, linkable: false`

### 4.3 看板模式（kanban）

**数据适配**：后端查询返回**扁平图结构**（`id → BoardItem`），column 节点含 `children` + `data.title/cardLimit`，card 节点含 `parentId`。

```json
{
  "type": "kanban",
  "data": "${boardData}", "draggable": true, "columnDraggable": true,
  "wipStrict": true, "columnWidth": 300,
  "onCardMove": { "action": "ajax", "args": {
      "url": "/r/ErpCsTicket__resolve", "method": "post",
      "data": { "ticketId": "${event.cardId}" } } }
}
```

- **列 → mutation 映射**：列结构 = 字典值，后端聚合返回列定义（动态列），每列绑定一个状态迁移 mutation（cs 看板实际用 `ErpCsTicket__assign/start/resolve/close` 四态组合，见 `ErpCsTicket/kanban.page.yaml:86,140,192,242`；crm 商机用 `ErpCrmLead__moveStage`）
- **SLA 红点**：卡片标题模板经 `data.title` 拼接或 `card` 自定义节点渲染
- **WIP 限制**：列节点 `data.cardLimit` + `wipStrict`（超限禁入，替代后端只读校验的前端即时反馈）

### 4.4 日历模式（calendar）

**数据适配**：`{ events: [{id, title, start, end, status, color}], resources: [...] }`；活动/休假经 `@BizQuery` 按日期窗口聚合。

```json
{
  "type": "calendar", "view": "month",
  "events": "${calendarData.events}", "resources": "${calendarData.resources}",
  "firstDayOfWeek": 1, "showWeekends": true, "locale": "zh-CN",
  "loadAction": { "action": "ajax", "args": { "url": "/r/ErpCrmActivity__findByDateRange",
      "params": { "start": "${event.start}", "end": "${event.end}" } } },
  "onEventChange": { "action": "ajax", "args": {
      "url": "/r/ErpCrmActivity__reschedule", "method": "post",
      "data": { "activityId": "${event.id}", "start": "${event.start}", "end": "${event.end}" } } }
}
```

- **视图切换**：`view` month/week/day + `date` 绑定（受控）
- **拖拽创建**：`onEventCreate` 为创建通道唯一入口 → 新增 mutation + 刷新
- **资源分组**：`resources` 嵌套 + `open` 控制折叠（hr 休假矩阵按员工分组）

### 4.5 向导模式（wizard）

**关键机制**（`design-patterns/wizard.md` + `examples/wizard-values-path.md`）：

- 每步 body 内 form 声明 `valuesPath: "wizardData.stepN"` 分区，互不覆盖
- 步骤校验：step 上 `formId` 指向 body 内 form 的 id，校验通过才放行 Next
- 确认步纯展示不设 formId，跨步读取 `${wizardData.stepN.xxx}`（`?? 0`/`|| "-"` 兜底）
- `onComplete` 自行聚合多步数据为**一个请求**（不依赖 form submitAction）
- `statusPath: "wizardStatus"` 发布 `currentStepKey/stepCount/canGoNext/canGoPrev`（外部按钮/面包屑消费）

```json
{
  "type": "wizard", "linear": true, "mountOnEnter": true,
  "statusPath": "wizardStatus",
  "steps": [
    { "key": "preCheck", "title": "预检查", "formId": "preCheckForm",
      "body": [{ "type": "form", "id": "preCheckForm", "valuesPath": "wizardData.preCheck",
          "body": [{ "type": "crud", "source": "${preCheckResult}" }] }] },
    { "key": "closePeriod", "title": "关账", "formId": "closePeriodForm",
      "body": [{ "type": "form", "id": "closePeriodForm", "valuesPath": "wizardData.closePeriod",
          "body": [{ "type": "select", "name": "periodId", "options": "${openPeriods}" }] }] },
    { "key": "confirm", "title": "确认", "body": [{ "type": "text",
          "text": "将关账 ${wizardData.closePeriod.periodId ?? '-'}" }] }
  ],
  "onComplete": { "action": "ajax", "args": {
      "url": "/r/ErpFinAccountingPeriod__closePeriod", "method": "post",
      "data": { "periodId": "${wizardData.closePeriod.periodId}" } } }
}
```

- **期末结账映射**：4+1 步（preCheck→closePeriod→annualRollover→finalize + 反结账 dialog）→ 5 个 step，`visible` 表达式控制年度结转仅 12 月可见（`${period.month === 12}`）
- **反结账**：独立 `dialog` + 二次确认（`confirm` action）

### 4.6 收件箱/联查/凭证模板（E 族）

```json
{
  "type": "tabs", "valueOwnership": "scope", "valueStatePath": "inboxTab",
  "items": [
    { "title": "未读", "body": [{ "type": "crud", "source": "${unread}" }] },
    { "title": "已读", "body": [{ "type": "crud", "source": "${read}" }] },
    { "title": "全部", "body": [{ "type": "crud", "source": "${all}" }] }
  ]
}
```

- 未读计数：`data-source` + `interval` 轮询 countUnread → 徽标（`badge`）
- markAllRead：toolbar 按钮 ajax mutation + `refreshSource`
- 凭证快速模板：`dialog` + `input-table`（模板行）+ 行内公式（`examples/business-document-formula.md` 的 `$Arr`/`$Math` 模式）

### 4.7 头行单据（A2，60 对子表）

```json
{
  "type": "form", "submitScope": "surface",
  "body": [
    { "type": "grid", "columns": [
        { "type": "grid-col", "span": 6, "body": [{ "type": "select", "name": "supplierId", "label": "供应商" }] } ] },
    { "type": "input-table", "name": "lines", "label": "明细",
      "columns": [{ "label": "物料", "width": 200 }, { "label": "数量", "width": 100 }, { "label": "金额", "width": 120 }],
      "addable": true, "removable": true, "reorderable": true,
      "item": [
        { "type": "select", "name": "materialId" },
        { "type": "input-number", "name": "qty", "min": 1 },
        { "type": "text", "text": "${(qty ?? 0) * (price ?? 0)}" }
      ] },
    { "type": "text", "text": "合计：${$Math.round($Arr.sumProducts(lines, 'qty', 'price'), 2)}" }
  ],
  "submitAction": { "action": "ajax", "args": { "url": "/r/ErpPurOrder__save", "method": "post" } }
}
```

- 头行提交：`submitScope: "surface"` + `includeScope: "*"` 整表单
- 行内公式：`$Arr`（sumField/sumProducts）与 `$Math` 需宿主注册（`11-host-integration.md`）

---

## 5. 数据联动与跨页协作

| 需求 | Flux 机制 |
|------|----------|
| 取数 | `data-source`（name 发布到 scope，兄弟节点 `${name.xxx}` 消费）或 `crud.loadAction` |
| 分页契约 | `{ items, total }`；loadAction 后端按 `page`/`perPage` 返回（`pageField`/`pageSizeField` 可覆盖） |
| 级联筛选 | `dependsOn` + `sendOn`（dependsOn 决定重跑时机，sendOn 决定是否真发请求） |
| 弹窗编辑后刷新 | `submitScope:'surface'` + `onSubmitSuccess → refreshNearest`（沿 scope.parent 找 CRUD/tree/data-source） |
| 跨页参数 | `valuesPath` 隔离（不污染父 scope） |
| 行数据传递 | `${$slot.record}`（table）/ `${$slot.item}`（crud） |
| 轮询 | `data-source.interval` + `stopWhen` |
| 条件显隐 | `visible`（仍参与校验）/ `when`（不激活子树） |
| 集合展开 | `loop`（items + itemName/indexName） |
| 状态色 | `status`（labelMap/levelMap/iconMap）/ `mapping`（两级回退） |

---

## 6. 迁移策略与优先级

### 6.1 迁移优先级（P0 → P3）

| 优先级 | 页面 | 理由 |
|--------|------|------|
| **P0** | F13 降级页 7 个（3 kanban + 2 timeline + 2 calendar） | 消除最大交互缺口；kanban/calendar/timeline 是 flux 原生强项；后端 mutation 已就绪 |
| **P0** | aps 排产甘特（gantt） | 拖拽 = 当前最大单项 UX 缺口；`IEtpApsGanttService` spec 已存在 |
| **P1** | 2 个向导（wizard） | 416 行手写 AMIS → flux wizard 结构化重写，可维护性收益最大 |
| **P1** | 16 个占位页（SPC 三件套等） | flux 路径直接落地，不再走 AMIS |
| **P2** | B 族特殊 dashboard（BOM 树/组织树/diff/时间线/steps/三单匹配/薪酬审批/净需求/EDI/发运追踪） | tree/diff-view 直接替换降级实现；其余以 data-source + 组合控件渐进迁移 |
| **P2** | 10+1 经营看板 | chart/data-source 原生支持；可渐进迁移 |
| **P3** | 头行单据/收件箱/联查/party-search/凭证快速模板 | 已有 AMIS 可用；迁移收益小（除非行内公式/拖拽需求出现） |

### 6.2 过渡策略（双栈共存）

1. **逐页切换**：每页创建 `xxx.flux.yaml`（或改 page.yaml 的 x:gen-extends 为 flux-web:GenPage），菜单 pageType 切 `flux`；AMIS 版本保留直至验收
2. **同一实体双文件**：`main.page.yaml`（amis）+ `main.flux.yaml`（flux）并存，flux 模式优先加载，`nop.web.render-mode` 全局回退
3. **验收**：复用现有 E2E 框架——`E2E_ENGINE=flux` 环境变量切换引擎，测试代码零修改（`docs/logs/2026/07-16.md` 已验证该机制）
4. **后端零改动**：全部 @BizQuery/@BizMutation 复用；新增的仅限**聚合查询**（看板/甘特/日历取数）

### 6.3 后端配套（仅新增查询，无模型变更）

| 页面 | 新增后端查询（可选） |
|------|---------------------|
| 甘特 | `ErpApsOperationOrder__findGanttData`（tasks/links 聚合）+ `updateSchedule` 持久化端点（对应 §4.2 onTaskDragEnd） |
| 看板 | `findBoardData`（扁平图结构 + 列定义） |
| 日历 | `findByDateRange`（日期窗口）+ `reschedule`/`createEvent` 持久化端点（对应 §4.4 onEventChange/onEventCreate） |
| 组织树 | 复用现有 tree 查询（labelField/keyField 配置） |
| SPC | 复用 `getSpcControlChartData`（chart 增强前以 line chart 呈现） |

---

## 7. 未决问题与风险

| # | 问题 | 状态 | 建议 |
|---|------|------|------|
| 1 | **SPC 控制图**：~~flux `chart` 缺 UCL/LCL 控制限区域填充与规则违反标记~~ | **已解决（`2026-08-03-1232-4` P4 回填）**：nop-chaos-flux 于 2026-08-03 实现 chart `referenceLines`（UCL/LCL/CL 参考线）+ `band`（上下界阴影带）+ `markers`（失控点标记，dataKey 读 isOutOfControl）完整能力 + 9 单测 + schema.d.ts 重生成。SPC 三件套已以完整能力落地（无近似），见 `docs/design/quality/spc.md` §页面交互设计 |
| 2 | **Formula-input**：flux 无公式编辑器（ConditionBuilder 是条件构建器非公式编辑器） | flux 缺口 | 低频 P4，暂不阻塞；薪酬公式配置沿用现有 textarea + 校验 |
| 3 | **页面级 picker**：flux-web.xlib 不支持 picker 容器 | nop-entropy 侧已确认抛错 | party-search 需以表单字段级 `picker` 组件实现（pickerPage 引用），或扩展 flux-web.xlib |
| 4 | **拖拽持久化契约**：gantt/kanban/calendar 拖拽事件 → 后端 mutation 的参数契约（event payload 字段）需在落地页逐一核对 | 设计待验证 | 每页落地时以 flux-types 事件 payload 为准 |
| 5 | **`$Arr`/`$Math` 表达式扩展**：business-document-formula 依赖宿主注册 | nop-chaos-next 侧待确认 | 落地前确认 `nop-web-site` 宿主已注册 |
| 6 | **字段级 diff**：合同版本对比受数据模型限制（ErpCtContractVersion 仅 content blob） | 与引擎无关 | 维持元数据对比 + diff-view（content 双栏） |
| 7 | **i18n**：page.yaml 层无 i18n 机制（F15 延后项） | 与引擎无关 | **Follow-up 登记（`2026-08-03-1232-5` Phase 3）**：flux 有 `12-i18n.md`（`initFluxI18n` + `t()`），登记为**复杂页迁移横切要求**——触发条件 = P2/P3 页面重写（`2026-08-03-1232-2/3`）时随附处理复杂页硬编码中文文案（title/remark/按钮）。view.xml 层 i18n 已完成（351 文件），page.yaml 层全量实施随 P2/P3 页面重写落地 |
| 8 | **测试**：flux 渲染 E2E 选择器契约与 AMIS 不同 | `13-testing.md` 有 selector 速查 | 迁移页按 flux selector 重写 spec；`E2E_ENGINE=flux` 已验证可用 |
| 9 | **complex 槽位内嵌自定义控件**：view.xml `<complex>` 槽位能否直接内嵌 gantt/kanban/calendar 等 flux 专有控件 | **能力缺口（2026-08-06 平台核对，纠正此前「已解决」误标）**：`GenContainerModel` 仅分派 5 种标准容器（crud/simple/tabs/wizard/group，`flux-web.xlib:45-61`）；`container_simple.xpl` 仅输出整 `<form>`，无 `beforeForm`/`afterForm` 透传点；`TestFluxWebGen` 与 `test-flux-complex.view.xml` 夹具均无自定义控件嵌入用例 → **生产复杂页全部采用「整页 page.yaml/flux.yaml 直写」**（20 个 flux.yaml，P2/P3/P4 落地，E2E 全绿），complex 槽位生产 0 使用。偏离裁决见 `docs/retrospectives/2026-08-06-1400-flux-page-organization-deviation.md`。**successor 触发条件**：nop-entropy 为 complex 槽位提供 arbitrary-node 透传能力（或有强复用价值的「筛选区+状态区」页面出现）时，对 B 族 dashboard 评估 complex 外壳重构 |
| 10 | **flux 表达式运行时约束**（`2026-08-03-1232-3` P3 发现）：flux 表达式仅支持箭头**表达式体**（无块体 `{}`、无 `while`/`forEach`/`if-return`），需块体的 reduce/栈算法无法在 formula 客户端运行 | 实施期发现 | 移至后端聚合查询（BOM `findBomTree`/薪酬 `findPayrollSummary`/净需求 `findNetReqGroups` 均在 Java 侧完成 group-by/reduce）；安全访问用 `?.`，空值兜底用 `??`；grid 用 `columns:N + items:[{colSpan,body}]` |

---

## 8. 参考文档

### Flux 平台侧
- `~/app/nop-chaos-flux/flux-guide/README.md` — 入口 + 文件索引 + 渲染器注册
- `flux-guide/design-patterns/{gantt,kanban,calendar,wizard,steps-timeline,diff-view,chart,tree,tabs,crud,combo-input-table,data-source,conditional,content-display,page-dialog-drawer}.md`
- `flux-guide/examples/{master-detail,wizard-values-path,business-document-formula}.md`
- `flux-guide/11-host-integration.md` — RendererEnv 宿主契约
- `flux-guide/12-i18n.md`、`13-testing.md`、`15-error-handling.md`
- nop-entropy：`nop-web/.../xlib/flux-web.xlib`、`flux-control.xlib`、`WebConfigs.java`（render-mode）、`PageModelLoaderFactory.java`（flux.yaml 回退）、`page.register-model.xml`

### 项目侧
- `docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md` — 30 个核心复杂页全景（本设计的输入）
- `docs/analysis/2026-07-20-complex-ui-controls-inventory-for-flux.md` — 控件盘点（含 2026-07-22 更新：scheduling 包已提供 gantt/kanban/calendar/barcode-input）
- `docs/analysis/2026-07-11-flux-integration-strategy-analysis.md` — 集成策略（view.xml 兼容 vs 直写 vs 元编程）
- `docs/backlog/frontend-ui-roadmap.md` — F13/F16 + Flux 渲染引擎备选决策
- `docs/design/aps/scheduling.md` §8 — 甘特 JSON 数据契约
- `docs/design/non-standard-views-patterns.md` — F13 看板/时间线/日历范式（含待回填漂移）
