# 复杂后台页面模式总览（Complex Admin Page Pattern Catalog）

> Status: 研究分析（为后续简化 view.xml 配置方案做输入）。
> 范围：不局限于 nop-app-erp，面向一般后台管理系统、银行后台、ERP 后台等管理页面常见模式。
> 数据来源：nop-app-erp 18+1 域 ~354 个保留层 view.xml + Nop 平台 `page-dsl-pattern-catalog.md` / `xview.xdef` + 通用 ERP/管理后台经验归纳。
> 相关项目内固化范式：`docs/design/page-structure-patterns.md`（tabs/仪表板/wizard）、`docs/design/child-table-editor-patterns.md`（头行子表）、`docs/design/tree-entity-patterns.md`（树形）、`docs/design/non-standard-views-patterns.md`（看板/时间线/日历）、`docs/design/cross-doc-navigation-patterns.md`（关联导航）、`docs/design/picker-patterns.md`、`docs/design/batch-operation-patterns.md`、`docs/design/dashboards.md`。

## 0. 本文要解决的问题

低代码/配置式后台（如 Nop view.xml、AMIS schema、各类 admin 框架）的复杂页面，本质上是少数**页面模式**的反复组合。但现有配置往往把"业务意图"与"渲染细节"混在一起，导致：

- 同一种业务模式在不同页面被写成不同结构的 XML/JSON；
- 大量样板（裁剪列、状态色块、审批按钮、子表引用）逐页重复；
- 新人无法从配置一眼读出"这是哪种页面模式、数据如何提交"。

本文先把"复杂页面"分解为**两个正交维度**（布局结构 × 数据交互），给出有限的模式清单；再给出**模式组合矩阵**和**选型决策树**；最后提炼一组**页面描述原语**，作为后续设计"简化页面定义方案"的输入。目标：让一份页面配置能用一句话说清"它属于哪个模式、子表怎么提交"。

---

## 1. 分析方法

1. **平台层**：以 Nop `xview.xdef` 为权威，确定页面类型原语（`crud` / `picker` / `simple` / `tabs` / `wizard`）及其合法组合。
2. **项目层**：统计 nop-app-erp 354 个保留层 view.xml 的 `<pages>` 结构、`<view path=>` 嵌套、`actionType="drawer/link"`、`@mutation` 动作、`gen-control`、`asideFilterForm` 等高频标记。
3. **一般化**：脱离具体框架，归纳管理后台/银行后台/ERP 后台的通用页面形态，标注每种模式在 Nop 中的落地路径。
4. **正交分解**：把"长什么样"（布局）和"数据怎么走"（交互/提交）拆开，避免把"详情页"与"子表随头提交"这种本可独立选择的维度耦合进单一概念。

---

## 2. 两个正交维度

任何复杂后台页面都可由两个正交维度描述：

### 维度 A：布局结构（Layout Pattern）—— "页面长什么样"

页面区域的拓扑组织。决定了用户视觉路径和交互焦点。

### 维度 B：数据交互（Data Interaction）—— "数据怎么加载与提交"

实体之间的数据关系、加载策略、提交时机、写路径。这是配置式后台最容易藏复杂度、也最该被显式声明的地方。

> 关键洞察：传统描述常把两者耦合（如"主从页""头行页"既指布局又指提交）。拆开后会发现，同一个布局可挂多种数据策略——例如"详情布局"既可承载"子表随头提交"，也可承载"子表独立 CRUD"，还可承载"子表只读"。**这正是简化配置的最大杠杆**：让布局和数据策略各自声明、自由组合。

---

## 3. 维度 A：布局结构模式清单

下表是管理后台常见的 12 种布局模式。括号内为 Nop 落地方式。

| # | 模式 | 典型外观 | 典型场景 |
|---|------|---------|---------|
| A1 | 单表 CRUD 列表页 | 顶部筛选 + 表格 + 行内/弹窗编辑 | 主数据、配置表、日志 |
| A2 | 头行单据详情页 | 表单（常分 tab/分组）+ 内嵌子表 | 单据录入：采购单/销售单/凭证 |
| A3 | 主从分屏页 | 上列表+下明细 / 左列表+右详情 | 工单浏览、银行流水核对 |
| A4 | Tabs 工作台/多 tab 详情 | 多标签切换，每 tab 一个子页 | 员工档案、设备仪表板、按状态分列表 |
| A5 | 树形管理页 | 左树（或树表合一）+ 右内容 | 科目、分类、部门、菜单 |
| A6 | 看板（Kanban） | 按状态/阶段分列的卡片墙 | 商机、工单、任务 |
| A7 | 时间线 / 日历 / 甘特 | 时间轴/日历网格/甘特条 | 活动流水、排班、排产 |
| A8 | 经营看板（Dashboard） | KPI 卡片 + 趋势图 + 预警列表 | 域级经营监控 |
| A9 | 向导（Wizard） | 分步表单 + 步骤指示器 + 状态守卫 | 期末结账、引导式执行 |
| A10 | 关联子页 drawer/弹窗 | 行按钮打开抽屉，内含子表/子详情 | 关联单据查看、下游单据创建 |
| A11 | 多表联查对比页 | 多列表/多区域并列 + 差异高亮 | 三单匹配、版本 diff、对账 |
| A12 | 报表/打印页 | 参数表单 + 渲染区（表格/HTML/图表） | 资产负债表、单据打印 |

下面逐一展开。

### A1. 单表 CRUD 列表页（Flat CRUD List）

- **结构**：`筛选区(filterForm/asideFilterForm) + 数据表格(grid) + 行操作(rowActions) + 列表操作(listActions)`；编辑态用弹窗/drawer/行内或独立页。
- **变体**：
  - 顶筛 vs 左筛（`asideFilterForm` 左侧过滤面板，43 处使用，适合多维度主数据）。
  - 行内编辑 vs 弹窗编辑 vs 抽屉编辑。
  - 上列表+下表单（`layoutMode="bottom-detail"`）——本模式与 A3 的过渡态。
- **Nop 落地**：`<crud name="main" grid="list" filterForm="query">`，占项目 ~85% 页面。
- **简化要点**：此模式高度同构，可完全模板化默认生成，仅需声明"列白名单 + 筛选字段 + 行操作"。

### A2. 头行单据详情页（Master-Detail Document）

- **结构**：一个头表单 + 一个或多个内嵌子表区，头与子表在同一编辑事务里。
- **外观变体**：平铺分组 vs 表单内 tab（`layoutControl="tabs"`，本项目 15 处）。
- **典型**：采购订单（基本/金额/明细行/审批/审计）、会计凭证（分录行）。
- **Nop 落地**：`<form>` 的 `<cell id="lines"><view path="...Line.view.xml" grid="sub-grid-edit|sub-grid-view"/></cell>`，32 处使用。
- **关键决策**：子表是"随头提交"还是"独立提交"——见维度 B。**本模式的布局不变，但数据策略差异巨大**。

### A3. 主从分屏页（Master-Detail Split）

- **结构**：两个独立区域（上/下 或 左/右），主区选中驱动从区加载。
- **典型**：工单列表+工单明细、银行交易列表+凭证详情、邮件列表+邮件正文。
- **与 A2 的区别**：A3 的从区是**独立加载、通常只读浏览**，主从不在同一编辑事务；A2 的子表与头共事务编辑。
- **Nop 落地**：`layoutMode="bottom-detail"`，或 page.yaml 内 `crud + service/crud` 经 reload 联动。
- **简化要点**：声明"主实体 + 从实体 + 联动键"即可，无需手写联动 reload。

### A4. Tabs 工作台 / 多 tab 详情

- **结构**：多标签，每 tab 一个子页（表单/列表/图表）。
- **两个子型**：
  - **表单级 tab**（轻量）：单表单内多 group 分 tab 显示（`layoutControl="tabs"`）。数据一次拉取。
  - **页级 tab**（重型）：跨实体多 crud/simple 拼装（`<pages><tabs>`），每 tab 独立加载策略。典型：员工档案=基本信息+合同+考勤+休假+工时。
- **Nob 落地**：表单级 15 处；页级 3 处（员工/设备/资产仪表板）。
- **加载策略**：一次拉全部 vs 每 tab 懒加载（`mountOnEnter=true` + `unmountOnExit=false`）。

### A5. 树形管理页（Tree CRUD）

- **结构**：自引用树（`parentId`）的 CRUD。三种外观：
  - **树表合一**：表格带展开/折叠行（最常见，本项目 4 处）。
  - **左树右内容**：左树导航，右侧选中节点的详情/子表。
  - **树形选择器**：tree-select 下拉。
- **典型**：会计科目、物料分类、部门组织、服务目录、菜单资源。
- **Nop 落地三件套**：`grid id="tree-list"` + `<selection>children @TreeChildren(max:5)</selection>` + `<table loadDataOnce="true" sortable="false" pager="none">` + `<simple name="add-child"><data><parentId>$id</parentId></data></simple>`。
- **关键点**：不分页、一次性加载、禁用列排序、新增子节点预填父上下文。

### A6. 看板（Kanban）

- **结构**：按状态/阶段分列的卡片墙，支持跨列移动（=状态切换）。
- **典型**：商机看板（动态列从阶段表读取）、工单看板（固定 6 列）、任务看板（4 列）。
- **落地现实**：AMIS 无原生"跨 crud 行拖拽"，本项目降级为**列式 crud + 行级"移动到状态"按钮**。卡片字段经 `tpl` 自定义渲染（优先级色块、SLA 红灯）。
- **关键点**：状态变更必须走状态机 mutation（非直接 update），前端 visibleOn 仅预拦截，后端二次守卫。

### A7. 时间线 / 日历 / 甘特

- **结构**：以时间维度为主轴的可视化。
  - **时间线**（`type:timeline`）：操作流水、活动记录。
  - **日历**（`type:calendar` 日/周/月；或 custom 矩阵 table 做"行=员工/资源，列=日期"）。
  - **甘特**（echarts custom series 只读）：排产、项目进度。
- **典型**：工单操作时间线、活动日历、团队休假日历矩阵、APS 排产甘特。

### A8. 经营看板（Dashboard / KPI）

- **结构**：顶部 KPI 卡片 + 中部趋势图 + 底部明细/预警列表。支持期间筛选、权限过滤。
- **典型**：销售/采购/库存/财务/资产/项目/制造/维护/质量 9 大域看板。
- **落地**：独立 page.yaml，`service` 聚合 + `crud` 明细 + `chart`/`tpl` 卡片。阈值配置化（NopSysVariable）。

### A9. 向导（Wizard）

- **结构**：分步表单 + 步骤指示器 + 顺序约束 + 状态守卫。
- **两个子型**：
  - **流程向导**：驱动 Facade mutation 链推进状态机（如期末结账 preCheck→close→finalize）。
  - **执行型向导**：引导操作员按步完成一次执行，含中间结果录入（如维护访问执行 4 步）。
- **与 tabs 的区别**：tabs 并行无序；wizard 有顺序约束，前置步完成才解锁后续。
- **落地现实**：view.xml `layoutControl="wizard"` AMIS 未实现，必须手写 page.yaml + 预渲染步骤指示器。

### A10. 关联子页 drawer / 弹窗（Related Drawer）

- **结构**：列表行按钮打开抽屉/弹窗，内含关联子表或子详情。
- **典型**：采购订单→关联入库单、设备→巡检/保养/备件/仪表板（一台设备挂 6 个 drawer）。
- **Nop 落地**：`<action actionType="drawer"><dialog page="ref-xxx.page.yaml"><data>{id:${id}}</data>` + `fixedProps` 固定外键子表。
- **高频**：本项目 69 文件含 `actionType="drawer"`，是最主流的"主从钻取"手法。

### A11. 多表联查对比页（Multi-Doc Compare）

- **结构**：多个列表/区域并列，共享过滤，差异高亮。
- **典型**：三单匹配（PO/Receive/Invoice）、合同版本 diff、对账比对。
- **落地**：独立 page.yaml，顶部共享 filter form + 并列 crud + 差异预警 crud。

### A12. 报表 / 打印页（Report / Print）

- **结构**：参数表单 + 渲染区（表格/HTML/图表），支持导出。
- **典型**：资产负债表、利润表、单据打印模板。
- **落地**：nop-report 引擎 + page.yaml 渲染容器（`service` 内 `html` 拍平）。

---

## 4. 维度 B：数据交互模式清单

这是配置式后台**最该显式声明、却最常被埋进渲染细节**的维度。核心问题：**子表/关联数据如何加载与提交？**

### B1. 单实体读写（Single Entity）

- 单表的 CRUD，无子表。对应 A1。
- 写路径：`__save` / `__update` / `__delete`。

### B2. 聚合根随头提交（Cascade Save，子表与头共事务）

- 子表行作为头的嵌套 `lines:[...]` 随头 `__save` 一次性提交。ORM 声明 `<to-many cascade-delete,insertable,updatable>`。
- **适用**：1:N 头行单据，行是头的强一致组成部分（采购单行、凭证分录）。
- **判定**：xmeta 有 `lines` prop + InputBean 有 `_lines` 字段。
- **关键收益**：一个事务原子保存头+行，无需两步。

### B3. 子表独立 CRUD（FK 关联，独立提交）

- 子表有自己的 CRUD 页面和 `__save`，仅以 FK 字段（如 `orderId`/`equipmentId`）关联到头。
- **适用**：ORM 无 cascade 关系；或业务上子表生命周期独立于头（配置项、评分明细、操作日志的"可编辑"场景）。
- **判定**：ORM 无 `<to-many cascade>` → `__save` 拒绝嵌套 → 必须独立提交。
- **落地**：子表独立 `main.page.yaml` + 按 FK 筛选；或 drawer 内 `fixedProps` 子表。

### B4. 子表只读（系统生成的日志/流水）

- 子表由系统/网关/工作流自动写入，UI 只读浏览（action log、状态迁移日志、网关回调日志）。
- **注意**：ORM 可能仍标 `cascade-delete`（为级联清理），但**业务上禁止前端 CRUD**（破坏审计完整性）。
- **落地**：只用 `sub-grid-view`，无 `sub-grid-edit`，无增删按钮。

### B5. 跨实体独立 tab（每 tab 独立加载）

- 多 tab 详情，每 tab 指向不同实体，各自 `findPage` + `filter_xxxId` 独立加载。
- **适用**：员工档案（基本信息/合同/考勤/休假/工时分属不同实体）。
- **加载策略**：懒加载（`mountOnEnter=true`，切 tab 才拉）+ 保留 DOM（`unmountOnExit=false`）。

### B6. 联动驱动（主区选中驱动从区）

- 主从分屏，主区选中行 → 从区按主键 reload。非共事务。
- **落地**：AMIS `reload target` 联动。

### B7. 跨页跳转（Link 钻取）

- 行按钮 `link` 跳转到另一实体列表页，URL 带 `filter_xxxId` 预填上下文。非弹窗。
- **适用**：创建下游单据（PO→创建入库单）、自引用导航（任务→子任务，规避 GenPage 自引用 cycle）。

### B8. 状态机写（审批/过账，非裸字段更新）

- 业务动作（提交/审批/驳回/过账/反过账/作废）必须走状态机 mutation，**不可用 `__update` 改 status 字段绕过**。
- **守卫分层**：前端 `visibleOn` 预拦截 + 后端 mutation 二次校验。

> **B2 vs B3 是简化配置的核心分水岭**：同一个"头行详情页"布局（A2），可能随头提交（B2）也可能独立提交（B3）。当前 view.xml 无法在顶层一眼看出区别，必须深入到 ORM 关系或 InputBean 才能判断。**简化方案应让"提交策略"成为页面描述的一等字段。**

---

## 5. 横切交互模式（Cross-Cutting）

这些模式可叠加在任意布局之上，是页面"加料"的高频手段，也是样板重复的重灾区。

| 模式 | 说明 | 项目频率 | 简化潜力 |
|------|------|---------|---------|
| 审批/过账状态机动作 | 行/列表按钮直挂 `@mutation`，按状态 `visibleOn` | 35+26 文件 | 高：可声明"状态机动作集"自动展开 |
| 批量操作 | `listActions` + `batch=true`，后端 batch mutation | 6 文件 | 中：3 模式（原子/builtin/全局触发） |
| 条件可见 visibleOn | 按单据状态/字段值显隐按钮/列 | 59 文件 | 高：可声明"状态守卫"而非手写表达式 |
| 跨页导航 link/drawer | `link` 跳转 / `drawer`+fixedProps 子表 | 8 link + 69 drawer | 高：可声明"关联导航契约" |
| Picker 选择器 | FK 字段弹窗选择，含树形/扁平/业务专用 | 352 picker | 高：FK 字段可自动接线 |
| 行内自动推算 | onEvent.change→setValue 推算金额/差异 | 头行单据通用 | 高：可声明"派生公式" |
| 状态色块 gen-control | status/approveStatus 列渲染彩色标签 | 95 文件 | 极高：可声明 dict→色块映射 |
| 字段格式化 | 金额/日期/百分比格式 | 横切 | 高：domain 驱动默认格式 |
| 脱敏字段 | 敏感字段 password/mask | 子表 | 中：domain 标记自动脱敏 |

---

## 6. 模式组合矩阵（哪些模式常一起出现）

实际页面多是"布局 × 数据 × 横切"的组合。下表是高频组合（项目内实证）。

| 业务形态 | 布局 | 数据提交 | 横切叠加 | 代表 |
|---------|------|---------|---------|------|
| **标准 ERP 单据页**（最高频模板） | A2 头行详情 + 表单 tabs | B2 随头提交 | 审批动作 + 状态色块 + 关联下游 drawer + picker + 行内推算 | 采购单/销售单/凭证 |
| 主数据管理 | A1 单表 CRUD | B1 单实体 | 批量启停 + picker + asideFilter | 物料/往来单位/员工 |
| 树形主数据 | A5 树形 | B1 单实体 | tree-select + add-child | 科目/分类/部门 |
| 员工/资产档案 | A4 页级 tabs | B5 跨实体独立 tab | drawer 触发 + 懒加载 | 员工档案/资产卡片 |
| 设备/对象全息视图 | A4 tabs + A10 drawer | B5 + B4 只读日志 | 多 drawer 子页 | 设备仪表板 |
| 业务处理工作台 | A6 看板 / A1 列表 | B8 状态机写 | visibleOn 守卫 + 批量 | 商机/工单/任务 |
| 经营监控 | A8 dashboard | 聚合查询 | 期间筛选 + 权限 | 各域看板 |
| 引导式作业 | A9 向导 | B8 状态机链 | 步骤守卫 | 期末结账/维护执行 |
| 主从浏览核对 | A3 分屏 / A11 联查 | B6 联动 / 独立加载 | 差异高亮 | 三单匹配/银行核对 |
| 关联单据追溯 | A10 drawer | B3/B7 | fixedProps/link | PO→入库单 |

> **最高价值抽象目标**：「标准 ERP 单据页」模板在采购/销售/库存/财务/合同大量重复。它 = 主表 grid(状态色块) + 表单 tabs 分组(基本/金额/明细子表/审批/审计) + asideFilter + 一组审批动作 + 关联下游/凭证 drawer。把这套组合沉淀为一个声明式脚手架，能覆盖项目半数以上页面。

---

## 7. 从模式到简化配置（页面描述原语）

基于上述分析，若要把现有 view.xml（业务意图与渲染细节混杂）简化为一份"一眼可读"的页面描述，建议提取以下**页面描述原语**（page description primitives）。每个原语只声明业务意图，渲染细节由模式模板自动展开。

### 7.1 顶层：页面身份与模式

```
page:
  entity: ErpPurOrder          # 主实体
  layout: master-detail-tabs   # 布局模式（A2 + 表单 tabs）—— 一句话定位
  submit: cascade              # 提交策略（B2 随头）—— 一句话定数据语义
```

仅需 `entity + layout + submit` 三项，即可让读者瞬间知道"这是什么页、子表怎么提交"。当前 view.xml 无法在顶层表达 `layout` 和 `submit`，必须推断。

### 7.2 原语清单（建议）

| 原语 | 作用 | 当前痛点（被埋在渲染细节里） |
|------|------|---------------------------|
| `layout` | 声明布局模式（A1–A12） | 当前要读 `<pages>` 结构 + `<cell><view>` 才能反推 |
| `submit` | 声明子表提交策略（B2/B3/B4/B5） | 当前要查 ORM cascade / InputBean 才知道 |
| `columns` | 列白名单（替代 bounded-merge 样板） | 297 处 bounded-merge 重复写 |
| `filters` | 筛选字段（顶筛/左筛） | filterForm/pick-query 分散 |
| `sections` | 表单分组/tab 定义 | layout 文本里 `====>` 标记难读 |
| `children` | 子表声明（实体 + 提交策略 + 只读?） | `<cell><view path grid=>` 隐式 |
| `actions` | 业务动作集（状态机动作 + 守卫） | 逐个手写 `<action>` + visibleOn |
| `statusTag` | dict→色块映射 | 95 处 gen-control 重复 |
| `relations` | 关联导航契约（drawer/link/fixedProps） | 69 drawer + 8 link 分散 |
| `derived` | 行内/头级派生公式 | onEvent.setValue 散落各 col |
| `picker` | FK 选择器（可默认自动） | 352 picker 多为 codegen 默认 |

### 7.3 简化方向（建议，非最终方案）

1. **模式模板化**：把 §6 的高频组合（尤其"标准 ERP 单据页"）做成模板，页面描述只写"差异"。
2. **默认即正确**：FK picker、dict 色块、domain 格式化、状态机动作按钮，应由元数据自动生成，而非每页声明。
3. **提交策略上提**：`submit: cascade | independent | readonly | lazy-tabs` 作为顶层声明，让数据语义脱离渲染层。
4. **声明守卫而非表达式**：`guard: approveStatus==SUBMITTED` 取代散落的 visibleOn 字符串。
5. **关联导航契约化**：`relation: { to: ErpPurReceive, via: orderId, open: drawer }` 取代手写 drawer+fixedProps。

> 这部分是"后续简化方案"的输入，不在本文展开实现。建议下一步用 §6 的"标准 ERP 单据页"模板做 PoC：选一个真实页面（如 `ErpPurOrder`），用上述原语写一份简化描述，对比现有 view.xml 的行数与可读性。

---

## 8. view.xml 扩展机制设计分析（如何让复杂页面描述更充分）

§7 提出了"页面描述原语"作为简化方向。本节回答更尖锐的问题：**这些原语在 Nop view.xml 平台机制上如何落地？平台是否需要扩展 schema——比如在 form 中加 subTables、在 cell 中加 view 抽象、新增 page 类型、或改进现有 page 类型？** 以下结论基于 nop-entropy 源码实证（标注 `文件:行号`），由独立子代理深度核实。

### 8.1 痛点的两类本质（决定方案归属）

前文痛点可归为两类，必须分开治理：

| 类别 | 痛点 | 本质 | 治理层 |
|------|------|------|--------|
| **应用层复用** | C（标准单据页模板大量重复）、行内派生公式散落 | 同一套模式每页重写 | **应用层**（模板库），零平台改动 |
| **平台能力缺口** | A（头聚合刷新无处配置）、B（子表 toolbar 无 slot） | schema/codegen 未提供配置点 | **平台最小补丁** |

混为一谈会导致"为复用问题去改平台 schema"的过度设计。

### 8.2 平台现状核实（源码实证，关键证据）

> schema 根：`nop-entropy/nop-kernel/nop-xdefs/.../_vfs/nop/schema/xui/`；codegen 根：`nop-entropy/nop-frontend-support/nop-web/.../_vfs/nop/web/xlib/`。

1. **`<cell><view>` 是封闭内容模型（痛点 A 根因）**：`disp.xdef:39` 的 `<view>`（`UiRefViewModel`）仅允许 `<data>` 子节点 + 固定属性，**无 `xdef:unknown-tag`/`xdef:unknown-attr`**（开放性须显式声明，见 `xdef.xdef:50/87`）。故 `onEvent`/`actions`/派生配置无法直接挂在 `<view>` 上。

2. **`<cell>` 已有 `<actions>` 但 codegen 完全不消费（"死能力"）**：`disp.xdef:81` 声明了 `<actions xdef:ref="actions.xdef"/>`（`<cell>` 与 `<col>` 均继承 disp），但 nop-web 源码 grep `cell.*actions / dispMeta.actions` **零命中**。`GenFormSimpleCell`（`web.xlib:349-433`）cell→控件分派只有三条路：`genControl` 存在→eval；否则 `view` 存在→`GenDispView`；否则→`DefaultControl`。→ 痛点 B 中"子表按钮只能 fallback 到 custom cell"**不是 schema 不允许，而是 codegen 从未实现 cell-actions 渲染**。

3. **`<disp>` 已有 `<gen-control>` 逃逸口**：`disp.xdef:66`，可写任意 XPL 生成控件 JSON，纯 view 层、Delta 兼容。这是当前唯一已验证的 app 级扩展点。

4. **grid 无 toolbar slot；主表有、子表缺（痛点 B）**：`grid.xdef` 的 `<grid>` 子节点无 `headerToolbar/actions`。但**主表** `grid_crud.xpl:53-60` 有 `headerToolbar`（filter-toggler/GenActions/bulkActions/...）；**子表** `GenInputTable`（`web.xlib:593-614`）只产 `removable/addable/editable` 三布尔，**无任何 actions/toolbar 注入点**。→ 痛点 B 是 codegen 层缺失，而非 AMIS 能力缺失（AMIS input-table 本身支持 actions/toolbar）。

5. **`<form>` 无 subTables 概念**：`form.xdef:84-97` 的 `<cells>` 仅含 `<cell>`。子表完全靠 `<cell><view grid=...>` 间接表达。

6. **`crud.layoutMode` 是"休眠钩子"（痛点 C 布局的钥匙）**：`xview.xdef:72` 已声明 `layoutMode="string"`，注释明确写"`bottom-detail` 表示上面是列表、下方是明细"，但 `page_crud.xpl` **从未引用 `pageModel.layoutMode`**。→ 主从分屏布局所需的最小钩子，平台**已在 schema 层预留，codegen 未接线**。

7. **平台无任何声明式子表/派生/级联先例**：xdefs + xlibs 全树 grep `sub-grid/subTable/derived/master-detail` 仅命中主表 headerToolbar。"子表"目前纯粹是 `<cell><view grid="sub-grid-edit|view">` 的**约定俗成用法**（本项目 32 页复用），平台未把它提升为一等概念。

8. **codegen 展开链**：`GenFormBody`→`GenFormCell`→`GenFormSimpleCell`（分派中心）→ `GenDispView`（`refView.grid` 以 `view` 结尾→`GenTable` 只读；否则→`GenInputTable` 可编辑）。已存在的 hook：`cell.genControl`/`col.genControl`、`<view addable/removable/editable>`（已透传 `web.xlib:563-568`）、`view.controlLib`（本项目已用：`ErpPurOrder.view.xml` 顶部 `<controlLib>/erp/xlib/control.xlib</controlLib>`）。

9. **扩展点边界**：view 层 Delta（`x:extends`/`x:override`/`x:prototype`）、`gen-control`、`controlLib` 均为受支持的 app 级扩展。但**平台 xlib tag（GenInputTable 等）无文档化的 app 级 Delta 覆盖机制**——覆盖须改平台源码。

10. **模型层边界（决定哪些信息该在 view vs 模型）**：
    - `cascade`/`to-many` 是 ORM 真相：`orm/entity.xdef:134-145` 的 `<to-many>` 有 `refProp`/`cascadeDelete`/`autoCascadeDelete`。**提交策略（级联/独立）应来自 ORM，不应在 view 重复声明**。
    - 派生字段公式在模型层**无声明能力**：xmeta.xdef grep `derived/computed/expr` 零命中。即 `amount=qty*price` 这类行内推算无法在 objMeta 声明，目前只能存在于 view 的 `gen-control`。

### 8.3 四方案评估

| 维度 | 方案1 form.subTables | 方案2 增强 cell.view | 方案3 新page类型 | 方案4 改进现有属性 |
|------|------|------|------|------|
| 解决痛点 | A·B·C(部分) | A·B | C·D(弱) | C(布局) |
| 需改平台文件 | form.xdef + GenFormBody | disp.xdef + GenInputTable | xview.xdef + 新模板 + GenPage | page_crud.xpl(+crud属性) |
| 改动面 | 中—大 | **小—中** | 大 | **小** |
| 向后兼容 | 需双路并存 | **高（可选子节点）** | 高（新类型可选） | **高（可选属性）** |
| 哲学契合度 | **高（仅展示声明）/ 低（若含 submit）** | **高** | 中（塞垂直语义） | 混合（layout 高 / submit 低） |
| app 能否独立交付 | 否 | 否 | 否 | layoutMode 否/宏 是 |

**各方案要点**：

- **方案 1（form 加 subTables）**：把子表从"埋在 layout 占位 + cell 两处"提升为 form 的 `<subTables>` 一等公民，集中声明每个子表的**展示属性**——引用哪个子表 view、标题、占位宽度、编辑/只读 grid、toolbar actions、加载策略。**这些都是展示语义，不碰数据关联**（FK、`cascade`、提交策略全来自 ORM），因此不重复 ORM 真相。它解决 cell+view 写法的三个真实问题：① 子表声明分散在 layout 文本占位与 cell 两处、改一处要动两处；② form 顶层看不出有几个子表；③ 子表级配置（toolbar）无处集中。**边界**：`<subTable>` 不应包含 `submit="cascade|independent"` 字段——那才是 ORM `<to-many cascade>` 真相，重复声明会双维护漂移；只读性（editGrid/viewGrid）属展示语义，可声明。**与方案 2 正交可组合**：方案 2 增强单个 cell-view 的能力，方案 1 把多个子表集中提升并顶层可见（subTable 元素本身即可复用方案 2 放宽后的 actions/onEvent）。改动面：`form.xdef` 增 `<subTables>`，`GenFormBody` 识别并展开——本质是 cell+view 的**集中化语法糖**，展开成同样的 AMIS input-table，与现有 cell+view 双路并存、向后兼容。

- **方案 2（增强 cell 的 view 抽象）**：放宽 `disp.xdef:39` 的 `<view>`，允许挂 `onEvent`/`actions`（复用**已存在**的 `actions.xdef`，零新概念），`GenInputTable` 消费之生成子表 toolbar。改动面小、完全向后兼容、契合平台既定"提供逃逸口"路线。**最适合作为平台侧最小补丁**。

- **方案 3（新增 page 类型如 master-detail/workspace）**：顶层声明标准单据页布局与提交策略。**致命缺陷**："标准单据页"是**应用层模式**，硬塞进平台会让平台背负 ERP 垂直语义；提交策略同样重复 ORM。更适合做**应用层模板库**而非平台 page 类型。

- **方案 4（改进现有 page 属性）**：给 `<crud>` 激活已存在的休眠 `layoutMode="bottom-detail"`（schema 零改动，仅 `page_crud.xpl` 接线分支），ROI 极高。但 `submit="cascade|independent"` 属性应舍弃（重复 ORM），改为工具链从 ORM 推导回显。

### 8.4 推荐设计：分层组合，Delta 为主、平台补丁为辅

**核心判断**：经评估，subTables（方案 1）与 cell+view（方案 2）**能力等价、无额外好处**——subTables 能做的，cell 上的 `<view>` 加少量扩展都能做，且不引入两套并存的写法。故推荐**以方案 2（cell.view 增强）为唯一平台补丁核心**，subTables 降为"可选语法糖，非必需"。

推荐分三层：

1. **【立即可做·app 层·零平台改动】应用层模板库**：在 `/erp/xlib/control.xlib`（本项目已存在并已被引用）用既有 `gen-control` + `controlLib` 构建可复用 XPL 宏，封装"标准 ERP 单据页"的 tabs 分组（基本/金额/明细/审批/审计）+ 行内派生公式集，消除 32 页重复（痛点 C）+ 集中派生公式（痛点 A 的推算部分）。

2. **【平台补丁·唯一核心】cell 的 `<view>` 增少量扩展（方案 2）**：放宽 `disp.xdef:39` 的 `<view>` 内容模型，允许挂 `<actions>`（子表 toolbar）+ `<onEvent>`（头聚合/事件联动）。这是 cell 唯一缺失的两个能力（详见 §8.5 场景矩阵），一个扩展点全覆盖。复用 disp:81 已引用的 `actions.xdef`，零新概念；`GenInputTable` 消费这两个子节点。完全向后兼容（可选子节点）。

3. **【平台补丁·激活休眠】方案 4 之 layoutMode**：接线 `crud layoutMode="bottom-detail"`（痛点 C 的主从分屏布局），schema 零改动。

**提交策略可见性**：痛点 C"看不出子表是随头提交还是独立提交"由 codegen 从 ORM `<to-many cascade>` 推导，生成只读 badge/tooltip 回显，**不**在 view 声明 submit 字段。

**舍弃/降级**：① 方案 1/4 中的 `submit` 字段（重复 ORM 真相，舍弃）；② 方案 1 的 subTables（降为可选语法糖——与 cell+view 能力等价、徒增两套写法，仅当某 form 子表极多、为可读性需要集中声明时可选，非必需）；③ 方案 3 的新 page 类型（给平台塞 ERP 垂直语义，舍弃）。

**理由**：① 应用层模板库立刻交付（不动平台、不阻塞发布、完全 Delta 兼容）；② 平台补丁面最小——只需放宽 cell 的 `<view>` 一个元素 + 接线已存在的 `layoutMode`，都是"补完平台已预留但未实现的钩子"，不新增渲染链、不新增概念；③ 守住 Model→Delta→Java：cascade/to-many 真相留在 ORM，view 只表达展示，派生公式因模型层无声明能力（证据 10）合理归入 app 控件库宏。

### 8.5 cell 现状、子表场景矩阵与改动提案

cell（`disp.xdef`，cell/col 共享）已具备相当多能力，子表场景绝大多数已被覆盖，**仅缺 2 个能力且都在 `<view>` 上**。

**cell 已有能力**（disp.xdef 实证）：`<view path grid addable removable editable>`（引用子表 + 行增删改）、`<visibleOn>/<disabledOn>/<readonlyOn>/<requiredOn>`（整个子表条件显隐，已有）、`<selection>`（子表 graphql 字段）、`<gen-control>`（XPL 逃逸口）、`<actions>`（disp:81 已声明但 codegen 死能力）。

| 子表场景 | cell 现状 | 扩展 |
|---------|----------|------|
| 引用子表 view + grid | ✅ `<view path grid>` | — |
| 编辑态/只读态用不同 grid | ✅ 两 form 各引用 | — |
| 行可增删改控制 | ✅ `addable/removable/editable` | — |
| 整个子表条件显隐/禁用 | ✅ cell `<visibleOn>/<disabledOn>` | — |
| 子表 toolbar 按钮（导入行等） | ❌ view 封闭 | view 增 `<actions>` |
| 头聚合随子表行变化刷新 | ❌ 无事件挂载点 | view 增 `<onEvent>` |
| 行内派生公式（amount=qty×price） | ✅ col 层 `gen-control` | — |
| 列级条件显隐（科目驱动维度） | ✅ col `<visibleOn>` | — |
| 敏感字段脱敏 | ✅ col `gen-control` | — |
| 半只读 action log | ✅ 只引 sub-grid-view | — |

**平台补丁（唯一核心）：放宽 disp.xdef `<view>`，增 `<actions>` + `<onEvent>`**

```xml
<!-- disp.xdef:39 改为 -->
<view path="v-path" grid="string" form="string" ... addable/removable/editable/title>
    <data>xjson</data>
    <actions xdef:ref="actions.xdef"/>   <!-- 复用 disp:81 已引用的 actions.xdef，零新概念 -->
    <onEvent .../>                        <!-- AMIS 标准事件结构 -->
</view>
```

codegen（`web.xlib:593` GenInputTable 增 toolbar + onEvent 透传，仿 `grid_crud.xpl:53`）：
```xml
<input-table removable addable editable ...>
    <toolbar j:list="true" xpl:if="${refView.actions}">
        <thisLib:GenActions actions="${refView.actions}" genScope="${genScope}"/>
    </toolbar>
    <columns j:list="true"><thisLib:GenGridCols .../></columns>
</input-table>
```
（`GenDispView` 需把 `refView.actions`/`refView.onEvent` 透传给 `GenInputTable`，仿现有 addable 透传 `web.xlib:563-568`。）

**关于 cell.actions（disp:81 死能力）**：建议**不激活**。子表按钮统一走 `cell.view.actions`（语义清晰：view 引用的那个 input-table 的 toolbar），避免 cell.actions 与 cell.view.actions 两处语义打架。

**平台补丁 B：接线 layoutMode（schema 已存在 `xview.xdef:72`，仅改 page_crud.xpl）**
```xml
<c:when test="${pageModel.layoutMode=='bottom-detail'}">
    <!-- 上 grid + 下 明细 form 的 AMIS 布局组合 -->
</c:when>
```

**应用层宏（零平台改动，立即可做）**：`/erp/xlib/control.xlib` 新增 `erp-doc-page` 等 layout 宏。

**可选（非必需）：form.subTables 语法糖**。若某 form 子表极多、为可读性想集中声明，可在 `form.xdef` 增 `<subTables>`，但它是 cell+view 的等价包装、展开成相同 AMIS，**不提供额外能力**，且带来"两套写法并存"的认知负担。默认不引入。

### 8.6 view.xml 新旧写法对照（以 ErpPurOrder 为例）

**现状写法**（痛点 C 重复 + 痛点 A/B 配置无处放）：
```xml
<form id="edit" size="lg" layoutControl="tabs">
  <layout>...lines[明细行](2)...</layout>
  <cells>
    <cell id="lines">
      <view path="/erp/pur/pages/ErpPurOrderLine/ErpPurOrderLine.view.xml" grid="sub-grid-edit"/>
      <!-- 想加"从订单导入行"按钮？无处可放 → 只能另起 custom cell + gen-control -->
    </cell>
    <!-- 头聚合 @totalAmount 随行编辑刷新 → 只能 autoBalance 按钮 + 兜底 -->
  </cells>
</form>
```

**简化后写法**（推荐组合落地后）：
```xml
<form id="edit" size="lg" layoutControl="tabs">
  <layout>
========>erp-doc-page[采购订单]======   <!-- app 宏：一键展开标准单据页 tabs 分组 -->
 lines[明细行](2)
  </layout>
  <cells>
    <cell id="lines">
      <view path="/erp/pur/pages/ErpPurOrderLine/ErpPurOrderLine.view.xml" grid="sub-grid-edit">
        <actions>                          <!-- 平台补丁A后：子表 toolbar 终于有处可放 -->
          <action id="import-from-order" label="从订单导入行" .../>
        </actions>
      </view>
    </cell>
    <!-- 行内派生公式不再散落每列：集中在 ErpPurOrderLine 的 controlLib 宏 -->
  </cells>
</form>
```

**可选写法：`<subTables>` 集中声明（非必需，与上面 cell 写法能力等价）**：

```xml
<form id="edit" size="lg" layoutControl="tabs">
  <layout> ...只留头字段分组... </layout>
  <subTables>
    <subTable id="lines" title="明细行" colspan="2"
              view="/erp/pur/pages/ErpPurOrderLine/ErpPurOrderLine.view.xml"
              editGrid="sub-grid-edit" viewGrid="sub-grid-view">
      <actions><action id="import-from-order" label="从订单导入行" .../></actions>
    </subTable>
  </subTables>
</form>
```

仅当某 form 子表极多、为可读性想集中声明时可选。它展开成与 cell+view 相同的 AMIS input-table，**不提供额外能力**，且带来"两套写法并存"的认知负担——默认用上面的 cell 写法即可。

### 8.7 模型边界（防漂移原则）

| 信息 | 真相归属 | view 应做什么 |
|------|---------|--------------|
| 头行关系 / to-many | ORM `<to-many>`（`entity.xdef:134`） | **只引用不声明**（`<view grid=>` 已是引用） |
| cascade 提交策略 | ORM `cascadeDelete/autoCascadeDelete`（`entity.xdef:145`） | **不重复声明**；建议 codegen 从 objMeta 推导并生成只读提示（tooltip/badge），满足"顶层可见"而不引入双维护 |
| 只读 vs 可编辑 vs 独立链接 | **展示语义** | view 声明（`<view editable=>` 已支持，`web.xlib:604` 透传） |
| 行内派生公式 | 模型层**无**声明能力（xmeta 无 derived，证据 10） | 归入 app controlLib 宏集中，单点维护 |

**核心原则：view 只表达"怎么看"，不表达"怎么存"。** 痛点 C"看不出提交策略"应通过**工具链回显模型真相**解决（codegen 从 ORM 推导只读 badge），而非在 view 再声明一份 submit 字段。这与 §7 的 `submit` 原语建议需要修正：`submit` 不应是 view 的声明字段，而应是工具链从 ORM 推导并**回显**的只读信息。

### 8.8 风险与开放问题

1. **AMIS input-table 三级嵌套未验证（痛点 D）**：GenInputTable 产 AMIS input-table，其嵌套能力未验证，降级独立 CRUD 仍是稳妥选择，本设计不触碰。
2. **cell-actions 语义归属**：证据 2 发现 `<cell><actions>`（disp:81）是死能力。补丁 A 激活 cell-view 的 actions 后，需明确"cell-view 级 actions（子表 toolbar）"与"cell 级 actions（disp:81）"的渲染区别，文档固化避免打架。
3. **头聚合实时刷新的运行时部分**：补丁 A 只解决"配置无处放"（schema），不解决"totalDebit 随行编辑实时重算"的**运行时**机制——这依赖 AMIS onEvent/公式联动，需 app 宏封装标准 onEvent 模板。schema 放宽是必要非充分条件。
4. **平台 xlib 无 app 级 Delta 覆盖机制**（证据 9）：所有改 GenInputTable/page_crud.xpl 的补丁**必须进平台源码**，不能由本项目独立完成。若不提上游 PR，痛点 A/B 的平台部分只能靠 gen-control 兜底（已验证可行但啰嗦）；痛点 C 仍可纯 app 解决。
5. **layoutMode=bottom-detail 的 AMIS 布局具体形态**未定稿（上 grid 下 form 的容器组合需原型验证）。

## 9. 页面模式选型决策树

面对一个新页面需求，按以下顺序决策布局与数据策略。

```
1. 是单实体还是有头行/关联？
   ├─ 单实体
   │   └─ 自引用树(parentId)?
   │       ├─ 是 → A5 树形管理页
   │       └─ 否 → A1 单表 CRUD 列表页（数据 B1）
   └─ 有头行/关联 → 2

2. 头与子表的提交关系？
   ├─ 子表随头共事务（ORM cascade）→ B2 随头提交
   │   └─ 布局：A2 头行详情页（表单 tabs）
   ├─ 子表独立 CRUD（无 cascade / 独立生命周期）→ B3 独立提交
   │   └─ 布局：A10 drawer 子页 / 独立页 / A4 tab
   └─ 子表系统生成只读 → B4 只读
       └─ 布局：嵌入 sub-grid-view / A7 时间线

3. 是否多实体聚合视图？
   ├─ 是，跨实体多 tab → A4 页级 tabs（B5 懒加载）
   ├─ 是，并列对比 → A11 多表联查
   └─ 否 → 4

4. 交互焦点是状态/时间/指标？
   ├─ 状态切换为主 → A6 看板（B8 状态机写）
   ├─ 时间维度为主 → A7 时间线/日历/甘特
   ├─ KPI 指标为主 → A8 dashboard
   └─ 分步引导为主 → A9 向导（B8 状态机链）

5. 主区选中驱动从区浏览（非编辑）？
   └─ 是 → A3 主从分屏（B6 联动）
```

---

## 10. 反模式与陷阱（跨模式通用）

| 陷阱 | 说明 | 正解 |
|------|------|------|
| 用 `__update` 改 status 绕过状态机 | 业务动作不走 mutation，丢失守卫与日志 | 走状态机 mutation（B8） |
| 把 B3（独立提交）当 B2（随头提交）写 | ORM 无 cascade 却嵌套 `lines` 提交，`__save` 拒绝 | 先判定 ORM cascade；无则独立 CRUD |
| 把 B4（只读日志）做成可编辑 | ORM 标 cascade 但业务禁止前端改日志，破坏审计 | 只用 sub-grid-view |
| 表单级 tab 与页级 tab 混用 | 单实体多 group 用了重型 `<pages><tabs>` | 单实体用 `layoutControl="tabs"`；跨实体才用页级 |
| 关联导航用 drawer 触发自引用 cycle | 实体 X 的 drawer 指向生成 X 的 ref 页 → StackOverflow | 自引用导航用 `link`（GenPage 不递归解析 link） |
| 看板期望拖拽 | AMIS 无原生跨 crud 拖拽 | 列式 crud + 行级"移动到状态"按钮 |
| 向导用 view.xml `layoutControl="wizard"` | AMIS 未实现该渲染器 | 手写 page.yaml + 步骤指示器 |
| 守卫仅前端 visibleOn | 可被绕过 | 后端 mutation 二次校验 |
| 树形页用分页 + 列排序 | 破坏层级展示 | `loadDataOnce + sortable=false + pager=none` |
| 跨域用不存在的 filter_ 字段 | GraphQL 报未定义参数 | 先核实目标实体实际字段 |
| 大数据量子表全量可编辑渲染 | 性能差 | read-mostly 用 sub-grid-view + 单字段可编辑 |

---

## 11. 总结

1. **复杂后台页面 = 有限布局模式（12 种）× 数据交互模式（8 种）+ 横切交互（9 类）的组合**，而非无限发散。
2. **布局与数据是正交维度**，应分别声明。当前配置把两者耦合，是复杂度主因。
3. **项目实证**：354 页面中 ~85% 是单表 CRUD（A1+B1），复杂度集中在"标准 ERP 单据页"（A2+B2+横切）这一高价值模板。
4. **简化杠杆（分层）**：① 应用层先行——用既有 `gen-control`/`controlLib` 在 `/erp/xlib/` 做"标准单据页"模板宏，零平台改动消除 32 页重复与散落的派生公式；② 平台最小补丁——放宽 `<cell><view>` 复用已存在的 `actions.xdef`、接线已存在的休眠 `crud.layoutMode`，补完平台"已声明未实现"的钩子；③ 守住模型边界——picker/色块/格式化/状态机动作由元数据默认生成，`submit`（级联/独立）**不在 view 声明**（那是 ORM `<to-many cascade>` 真相），而由工具链推导回显，避免双维护漂移。
5. **下一步**：以"标准 ERP 单据页"（如 `ErpPurOrder`）为 PoC——先用 §7 原语 + §8.6 简化写法做 app 模板宏，量化对比现有 view.xml 的行数与可读性；平台补丁（`disp.xdef` view 放宽、`page_crud.xpl` layoutMode 接线）作为上游 PR 跟进，期间用 gen-control 兜底过渡。详见 §8。

## 12. 参考

- 平台权威：`../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`、`view-and-page-customization.md`、`nop-kernel/nop-xdefs/.../nop/schema/xui/xview.xdef`
- 项目内固化范式：`docs/design/page-structure-patterns.md`、`child-table-editor-patterns.md`、`tree-entity-patterns.md`、`non-standard-views-patterns.md`、`cross-doc-navigation-patterns.md`、`picker-patterns.md`、`batch-operation-patterns.md`、`dashboards.md`、`visible-on-patterns.md`、`query-filter-patterns.md`、`field-formatting-patterns.md`、`voucher-back-link-patterns.md`
- 架构：`docs/architecture/view-and-page-strategy.md`
