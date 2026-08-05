# view.xdef `<complex>` 界面 × nop-chaos-flux 控件：能否覆盖本项目所有复杂页面？

> 日期：2026-08-05
> 性质：深度分析（研究级，非 owner doc）
> 范围：view.xdef `<complex>` 页面类型（xview.xdef + flux-web.xlib 生成链）与 nop-chaos-flux 全部最新控件的组合能力边界，对本项目全部复杂页面的逐页覆盖判定
> 方法：平台源码实证（xview.xdef / flux-web.xlib / 各 container_*.xpl / 测试夹具与单测）× 控件盘点（nop-chaos-flux 20+ 包 / flux-guide design-patterns）× 项目实现层逐页核查（20 个 flux.yaml + 24 报表 + 44 非标准 page.yaml）
> 相关：`docs/design/flux-complex-pages.md`（30 复杂页实现设计）、`docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md`（30 页全景）、`docs/analysis/2026-07-31-1300-xview-schema-assessment.md`（complex 休眠期评估，已被 2026-08-01 激活推翻）、`docs/backlog/frontend-ui-roadmap.md`（Flux 全量迁移决策）

---

## 1. 结论摘要

**不能。** view.xdef `<complex>` 界面 + nop-chaos-flux 最新控件的组合**无法独立完成本项目的所有复杂页面**，且当前实现层**已实际证明**这一点：

| 判定 | 页面类型 | 证据 |
|---|---|---|
| ✅ **complex 可覆盖** | 标准容器组合页：tabs 工作台、group 网格布局、header/footer/aside + crud/simple 外壳、基础 wizard | `ErpAllFluxPagesTest` 354 CRUD + 352 picker + 38 ref + 15 tabs 经 view.xml → flux-web 100% 合法 JSON |
| ❌ **complex 不可覆盖** | 以 flux 专有交互控件为核心页：3 kanban、2 timeline、2 calendar、1 gantt、BOM/组织 tree、diff-view、steps、loop 分组、chart/cards 看板、collapse、页面级 picker | 上述页面全部以 **20 个 `.flux.yaml` 直写**落地（`module-*/erp-*-web/.../pages/` 逐个核对），**0 个 view.xml 使用 `<complex>`**（`grep -rl "<complex" --include="*.view.xml"` 全仓库 0 命中） |

**根因（平台实证）**：`<complex>` 四槽位（header/footer/aside/body）内只能容纳 `UiContainerModel` 的 6 个子类型——`crud/picker/simple/tabs/wizard/group`（xview.xdef:67-217, 226-231）。flux 专有控件（kanban/gantt/calendar/timeline/tree/diff-view/steps/chart/cards/collapse/loop/data-source 等）**不是 UiContainerModel 子类型**，且 flux-web.xlib 生成链**没有任意 renderer 透传通道**（`container_simple.xpl` 只输出 flux `<form>`；xview 的 `beforeForm/afterForm` xjson 透传点在 flux-web 全库 0 消费）。即：**complex 提供的是「标准容器编排外壳」，不是「控件嵌入能力」**。

---

## 2. 平台能力边界实证（view.xdef complex → flux JSON 生成链）

### 2.1 模型侧：xview.xdef（`nop-kernel/nop-xdefs/.../schema/xui/xview.xdef`）

```xml
<pages>
    <complex name="!string" xdef:name="UiComplexPageModel" xdef:ref="UiPageModel" xdef:bean-tag-prop="type">
        <header xdef:ref="UiContainerModel" xdef:body-type="list" xdef:bean-body-prop="body" />   <!-- :227 -->
        <footer xdef:ref="UiContainerModel" xdef:body-type="list" xdef:bean-body-prop="body" />   <!-- :228 -->
        <aside  xdef:ref="UiContainerModel" xdef:body-type="list" xdef:bean-body-prop="body" />   <!-- :229 -->
        <body   xdef:ref="UiContainerModel" xdef:body-type="list" xdef:bean-body-prop="body" />   <!-- :230 -->
    </complex>
</pages>
```

`UiContainerModel`（:67-217）允许的子标签全集：`crud`、`picker`、`simple`、`tabs`（tab 递归引用 UiContainerModel）、`wizard`（step 递归）、`group`（body 递归）。**无 `<custom>`/`<component>`/xjson 槽**——类型封闭。

### 2.2 生成侧：flux-web.xlib（`nop-frontend-support/nop-web/.../xlib/flux-web.xlib`）

| 环节 | 实现 | 结论 |
|---|---|---|
| 页面级分派 | `impl_GenPage.xpl:18-43`：type ∈ {crud, picker, simple, tabs, wizard, group, **complex**} 全部分支 | complex 有专门分派 → `page_complex.xpl` |
| complex 输出 | `page_complex.xpl`：`type: page` + header/footer/aside/body 四槽位（仅 `body.size() > 0` 的槽位输出） | 测试 `TestFluxWebGen.testComplexPageEmitsFourSlots`（:363）+ 夹具 `test-flux-complex.view.xml` 验证 |
| 容器级分派 | `GenContainerModel`：crud/simple/tabs/wizard/group → 各自 container_*.xpl | **picker 不在分派之列**——`flux-web.xlib:31-32` 注释明示「picker 不在分派之列（Flux 无页面级 picker schema），配置 picker 落入 otherwise 抛 `nop.err.web.unknown-page-type`」(:60-61) |
| simple 容器 | `container_simple.xpl`：仅输出 flux `<form>`（submitAction/loadAction 归一化） | **无 free-form 槽透传**：`beforeForm/afterForm/beforeBody/afterBody` 在 flux-web 目录 grep = 0 命中 |
| tabs 容器 | `container_tabs.xpl:4-28`：items 循环，tab body 支持**内联容器**（再分派 GenContainerModel）或 `LoadPage`（:9,:21） | tab 内可再嵌标准容器 ✓ |
| wizard 容器 | `container_wizard.xpl:10-36`：steps 循环，step body 同 tabs 语义；wizard 属性仅透传 `mode/action*Label`（:6-7，startStep/initFetch/className 等丢弃） | 基础向导 ✓；`valuesPath/formId 闸/statusPath/onComplete` 等 flux 高级向导机制**不经 view.xml 可达** |
| group 容器 | `container_group.xpl:20-40`：flux `grid` + `items:[{colSpan,rowSpan,body}]`；columns/gap/autoFlow/alignItems/justifyItems 映射（responsiveColumns 暂不输出） | 二维网格布局 ✓ |
| crud 容器 | `container_crud.xpl` → `grid_crud.xpl`（无 page 外壳片段） | body 级 crud ✓ |
| 表单格级透传 | `flux-web.xlib:421`（cell genControl）、`:512`（col genControl） | 仅**表单单元格/表格列**级可嵌任意控件，**页面槽位级无此通道** |

**能力边界结论**：

1. complex 可表达：四槽位外壳 + 标准容器任意嵌套（crud/simple/tabs/wizard/group）。
2. complex **不可表达**：
   - flux 专有交互控件（kanban/gantt/calendar/timeline/tree/diff-view/steps/chart/cards/collapse/loop/statistics/data-source/transfer/condition-builder 等）——非 UiContainerModel 子类型，无槽位透传；
   - 页面级 picker（生成器抛错）；
   - 高级 wizard 语义（valuesPath/formId/statusPath/onComplete）；
   - 任意 xjson 直插（xview 定义了 xjson 透传点但 flux-web 不消费）。
3. 突破手段仅有两条：**整页 `.flux.yaml` 直写**（项目实际路径），或**未来扩展 flux-web.xlib** 使 `container_simple.xpl` 消费 `beforeForm/afterForm` xjson 透传（平台增强候选，见 §6）。

---

## 3. nop-chaos-flux 控件盘点（结合最新状态）

| 包 | 代表控件 | 与复杂页的关联 |
|---|---|---|
| flux-renderers-basic | page/dialog/drawer/tabs/text/button/container/flex/loop/dynamic-renderer | loop 用于净需求分组 |
| flux-renderers-layout | grid/button-group/collapse/dropdown-button/steps/timeline/wizard | steps（ASN）、timeline（EDI/发运/活动）、wizard（向导）、collapse（EDI 报文） |
| flux-renderers-form | input/select/checkbox-group/date/textarea/markdown/period | 标准表单 |
| flux-renderers-form-advanced | combo/array-editor/condition-builder/picker/transfer/tree-controls/tag-list/input-table/key-value/icon-picker/upload | input-table（薪酬批改/凭证模板）、picker（表单字段级） |
| flux-renderers-data | crud/table-renderer/list/pagination/data-source/statistics/tree/chart | data-source（全复杂页取数）、tree（BOM/组织）、chart（看板/SPC，含 referenceLines/band/markers） |
| flux-renderers-content | cards/carousel/diff-view/html/image/json-view/alert/progress/qrcode/status | cards（KPI 卡）、diff-view（版本对比）、status（SLA 色块） |
| flux-renderers-scheduling | barcode-input/calendar/gantt/kanban | calendar（活动/休假）、gantt（排产）、kanban（任务/工单/商机） |
| 其他 | mobile/code-editor/spreadsheet/flow-designer/report-designer/word-editor/formula/runtime/i18n | 报表/流程设计（非本项目复杂页核心） |

**控件能力充足**：所有复杂页所需的交互控件 nop-chaos-flux 均已提供（2026-07-20 盘点时的 SPC chart 缺口已由 2026-08-03 referenceLines/band/markers 补齐）。**瓶颈不在控件，在 view.xdef → flux 的生成通道**。

---

## 4. 逐页覆盖判定（30 核心复杂页 + 24 报表 + 44 非标准页）

> 判据：**V** = 仅凭 view.xdef complex（四槽位 + 标准容器）可达；**F** = 必须 flux.yaml 直写（控件非 UiContainerModel 子类型）；**H** = 混合（外壳可 complex，核心控件仍需直写）；**A** = 当前经 amis-compat 渲染的 AMIS schema 页（非 flux 原生，也不经 complex）。

### 4.1 A. 经营看板 10+1（`*/dashboard/main.page.yaml` + ErpCsQualityDashboard）→ **F（chart/cards/data-source 非容器类型）**

每页 = data-source（KPI/趋势/预警）+ cards（KPI 卡）+ chart（趋势）+ crud（预警）。其中 data-source/chart/cards 均非 UiContainerModel 子类型，**complex 槽位无法容纳**；即使外壳套 complex，槽内仍是死路。当前实现：AMIS schema page.yaml 经 flux amis-compat 渲染（dashboard smoke 绿）。

### 4.2 B. 特殊 dashboard 子页 10 → **全部 F**

| 页面 | 文件（当前实现） | 核心控件 | complex 可达？ |
|---|---|---|---|
| 排产甘特 | `aps/dashboard/schedule-gantt.flux.yaml` | **gantt**（拖拽/缩放/依赖连线） | ❌ 非容器类型 |
| 三单匹配 | `pur/dashboard/three-way-match.flux.yaml` | crud×4 + status 差异色 | ⚠️ 理论可（纯 crud），实际直写 |
| BOM 树 | `mfg/dashboard/bom-tree.flux.yaml` | **tree** + 后端 findBomTree | ❌ tree 非容器类型 |
| 组织架构 | `hr/dashboard/org-chart.flux.yaml` | **tree**（findDepartmentTree） | ❌ 同上 |
| 薪酬审批 | `hr/dashboard/payroll-approval.flux.yaml` | data-source 聚合 + crud + input-table 行内编辑 | ❌ data-source/input-table 非容器类型 |
| 发运追踪 | `log/dashboard/shipment-tracking.flux.yaml` | **timeline** + 双 data-source | ❌ |
| 净需求 | `drp/dashboard/net-requirement.flux.yaml` | data-source + **loop** + table 嵌套分组 | ❌ |
| 版本对比 | `ct/dashboard/version-diff.flux.yaml` | **diff-view** + 元数据对比表 | ❌ |
| ASN 流程 | `b2b/dashboard/asn-flow.flux.yaml` | **steps** + crud rowClick | ❌ |
| EDI 详情 | `b2b/dashboard/edi-detail.flux.yaml` | **timeline** + table + **collapse** | ❌ |

### 4.3 C. F13 非标准视图族 7 → **全部 F（已在 flux.yaml 落地）**

| 页面 | 文件 | 核心控件 |
|---|---|---|
| prj 任务看板 | `prj/ErpPrjTask/kanban.flux.yaml` | **kanban** + onCardMove 路由 |
| cs 工单看板 | `cs/ErpCsTicket/kanban.flux.yaml` | **kanban** + SLA 标记 |
| crm 商机看板 | `crm/ErpCrmLead/opportunity-kanban.flux.yaml` | **kanban** 动态列 |
| crm 活动时间线 | `crm/ErpCrmActivity/timeline.flux.yaml` | **timeline** |
| cs 操作时间线 | `cs/ErpCsTicketAction/timeline.flux.yaml` | **timeline** |
| crm 活动日历 | `crm/ErpCrmActivity/calendar.flux.yaml` | **calendar** + onEventChange |
| hr 休假日历 | `hr/ErpHrLeaveRequest/team-vacation-calendar.flux.yaml` | **calendar** 资源视图 |

### 4.4 D. 向导 2 → **理论 V 但实质 F**

wizard 是标准容器类型，基础向导理论可达；但全库最复杂的 `fin/pages/period-close-wizard/main.flux.yaml`（5 步 + per-module 状态 + 反结账 dialog）与 `mnt/pages/visit-wizard/main.flux.yaml`（4 步）依赖 `valuesPath 分区 / formId 校验闸 / statusPath / onComplete 聚合提交`——这些机制 `container_wizard.xpl` **不映射**（仅 mode/action*Label），经 view.xml 表达会退化为无闸无分区的基础向导。**实质不可达**。

### 4.5 E. 独有形态 4 → **全部 F 或 A**

| 页面 | 当前实现 | 判定 |
|---|---|---|
| 通知收件箱 | `notify/ErpSysNotification/inbox.page.yaml`（329 行，AMIS tabs + 客户端 JS 过滤） | **A**；tabs 容器可达但客户端 JS 多维过滤不可经 schema 表达 |
| party-search 手写 picker | `md/pages/party-search/main.picker.page.yaml` | **F**；页面级 picker 在 GenContainerModel 抛错 |
| 凭证↔单据双向联查 | `fin/ErpFinVoucherBillR/{voucher-by-bill,bills-by-voucher}.page.yaml` | **A**；crud + data-source 组合，data-source 非容器 |
| 凭证快速模板 | `ErpFinVoucherTemplate.view.xml`（dialog + input-table 预览） | **H**；dialog 经表单动作可达，input-table 为表单格级控件 ✓ |

### 4.6 F. view.xml 层复杂视图 19（15 tabs + 60 子表 + 4 树形）→ **V（标准容器 + 表单格级控件）**

| 形态 | 判定 | 证据 |
|---|---|---|
| tabs 视图 15（单据/档案） | ✅ **V** | `<pages><tabs>` 或 form `layoutControl="tabs"` 经 flux-web 生成 flux tabs；`ErpAllFluxPagesTest` 15 tabs 合法 |
| 头行子表 60 对 | ✅ **V** | sub-grid-edit → 表单格级 `input-table`（GenInputTable），表单单元格透传通道存在 |
| 树形主数据 4 | ✅ **V** | 表单格级 `tree-select` / `edit-tree-parent` 控件（flux-control.xlib） |

### 4.7 24 报表页 + 44 非标准 page.yaml（dashboards/inbox/SPC 三件套/联查/向导等）

- 24 报表：统一「参数 form + `ErpXxxReport__renderHtml` + 下载按钮」AMIS schema → **A**（amis-compat 渲染）。其中 form 可经 view.xml 表达，但 `renderHtml` 容器 + 下载动作非标准容器/表单能力。
- 44 非标准页中的其余项（SPC 三件套 `spc-{chart,capability,sample}`、expense-claim、asset-stocktake、project-pnl、disposal-wizard、lead-conversion、stock-take-flow、cost-center、project-settlement、bank-*、budget-* 等）：SPC 三件套为 chart + crud（**F**，已有 1232-4 落地为 chart/bar + crud 组合页）；其余为「KPI 汇总卡 + crud 列表」型（cards + crud，**F**，cards 非容器类型；部分经 amis-compat 渲染为 **A**）。

### 4.8 汇总统计

| 类别 | 数量 | complex 可覆盖 | 实际实现路径 |
|---|---|---|---|
| A 看板 10+1 | 11 | 0 | amis-compat（A） |
| B 特殊 dashboard | 10 | 0 | 10 flux.yaml（F） |
| C F13 非标准族 | 7 | 0 | 7 flux.yaml（F） |
| D 向导 | 2 | 0（理论 V 实质 F） | 2 flux.yaml（F） |
| E 独有形态 | 4 | 0~1 | flux.yaml/amis-compat（F/A/H） |
| F view.xml 层 | 19 | 19 | view.xml → flux-web（V） |
| 24 报表 | 24 | 0 | amis-compat（A） |
| 44 非标准页 | 44 | 0~2 | amis-compat/flux.yaml（A/F） |

**30 个核心复杂页（A+B+C+D）中 complex 可覆盖 0 个**；19 个 view.xml 层复杂形态（tabs/子表/树形）全部可覆盖。**「complex 可覆盖」与「复杂页面」基本互补而非重叠**——complex 覆盖的是结构复杂但控件标准的页面，真正的交互复杂页全部依赖 flux.yaml 直写。

---

## 5. 对既有设计的校验（§4 与文档一致性）

| 文档表述 | 实证结果 |
|---|---|
| `flux-complex-pages.md` §2.5「页面外壳（四槽位）经 view.xml complex 定义，覆盖**全部复杂页**；flux 专有控件经槽位内 `<simple>` 包裹或 flux.yaml 直写」 | **外壳层覆盖全部复杂页 = 未兑现**：全仓库 0 个 view.xml 使用 `<complex>`；「槽位内 `<simple>` 包裹专有控件」依赖的 beforeForm/afterForm 透传在 flux-web **不存在**（§2.2）。实际落地 = 「整页 flux.yaml 直写」单一路径 |
| `non-standard-views-patterns.md` §0（1232-5 回填）「complex 槽位组合模式：页面外壳经 `<complex>` 四槽位定义」 | 与实现不一致（0 使用）；需修正为「复杂页 = flux.yaml 直写；complex 仅用于标准容器外壳页」 |
| `flux-complex-pages.md` §7 #9「complex 槽位内嵌自定义控件：已解决」 | **误判**：解决的是「fluc 专有控件经 flux.yaml 直写内嵌」（绕过 complex），不是「complex 槽位可内嵌」 |
| `2026-07-31-1300-xview-schema-assessment.md`「complex 休眠（无 page_complex.xpl）」 | 已被 2026-08-01 激活推翻 ✓（本报告基于激活后状态） |

---

## 6. 结论与建议

1. **直接回答**：不能。view.xdef `<complex>` 是**标准容器编排外壳**（四槽位 + crud/simple/tabs/wizard/group 嵌套），nop-chaos-flux 最新控件是**渲染原语**，两者之间**没有 schema 级嵌入通道**（生成器类型封闭 + xjson 透传未消费 + picker 抛错 + 高级 wizard 机制不映射）。本项目 30 个核心复杂页 0 个经 complex 实现，20 个 flux.yaml 直写即为最佳实践证据。

2. **正确分工（当前范式，建议固化）**：
   - view.xml（complex/crud/simple/tabs/wizard/group）→ 标准 CRUD、tabs 工作台、头行子表、树形主数据、基础外壳（354+352+38+15 已验证）；
   - `.flux.yaml` 直写 → 一切以 flux 专有控件为核心交互的页面（kanban/gantt/calendar/timeline/tree/diff-view/steps/loop/chart/cards/collapse/页面级 picker/高级 wizard）；
   - flux amis-compat → 过渡期 AMIS schema 页（24 报表 + 44 非标准页的未重写部分）。

3. **平台增强候选**（若希望复杂页也模型化）：扩展 `container_simple.xpl` 消费 xview 的 `beforeForm/afterForm` xjson 透传，使 simple 容器槽位可嵌任意 flux 节点——但这属于 nop-entropy 生成器变更，收益与 20 个 flux.yaml 的存量成本需权衡（flux.yaml 已为事实标准，见 `frontend-ui-roadmap.md`「Flux 全量迁移」决策）。

4. **文档修正项**（登记，非阻塞）：`flux-complex-pages.md` §2.5/§7#9 与 `non-standard-views-patterns.md` §0 的「complex 覆盖全部复杂页」表述需按 §5 校准。

---

## 7. 参考文档索引

- `nop-kernel/nop-xdefs/.../schema/xui/xview.xdef`（complex :226-231 / UiContainerModel :67-217）
- `nop-frontend-support/nop-web/.../xlib/flux-web.xlib`（GenContainerModel :31-65 / cell genControl :421 / col genControl :512）
- `flux-web/{impl_GenPage,page_complex,container_simple,container_crud,container_tabs,container_wizard,container_group}.xpl`
- `nop-web/.../test/java/io/nop/web/page/TestFluxWebGen.java`（complex 用例 :363）+ 夹具 `test-flux-complex.view.xml`
- `~/app/nop-chaos-flux/packages/`（20+ 渲染器包）+ `flux-guide/design-patterns/`（36+ cookbook）
- 本项目：20 个 `.flux.yaml`（§4 表内路径）、24 报表 `pages/report/*.page.yaml`、44 非标准 page.yaml、`ErpAllFluxPagesTest`、`docs/design/flux-complex-pages.md`、`docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md`、`docs/backlog/frontend-ui-roadmap.md`
