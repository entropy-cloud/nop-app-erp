# 2026-07-22-1400-1-f16-high-risk-gantt-bom-scan F16 高风险复杂页面 successor（aps 甘特图 + mfg BOM 树 + inventory 扫码确认）

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F16（line 359-382 / 547）+ `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md` §Deferred But Adjudicated「高风险 F16 页面」
> Related: `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md`（F16 低风险批 predecessor，§Deferred 明确本计划为高风险 successor）；`docs/plans/2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md`（F13 拖拽 PoC 裁决先例：AMIS service scope 拖拽不可行→row-action 降级；native timeline/calendar 不可行→each+tpl 降级）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-22，对 3 个高风险 F16 目标页面的后端就绪度 + AMIS 组件能力 + Nop echarts 集成范式 + F13 拖拽/降级先例 + 独立后端就绪度审计 ses_076def075ffe）：

### 本计划范围：3 个高风险 F16 页面

| # | 域 | 页面 | 后端就绪度 | 前端 AMIS 能力 | 风险等级 |
|---|---|------|-----------|--------------|---------|
| 1 | aps | 排产甘特图（ErpApsOperationOrder） | **PARTIAL gap**：设计文档 `scheduling.md §8.3` 规定 `getGanttData()` 但**无 Java 实现**（grep 全 module-aps 零命中 Gantt/gantt）；原始字段就绪（plannedStartDateT/plannedEndDateT/machineId/workOrderId/status on `_ErpApsOperationOrder`）；`ErpApsOperationOrderBizModel` 仅有 `earliestCompletionDate`/`checkFeasibility` 两个 @BizQuery | echarts 已集成（dashboards `type:chart` + adaptor 返回 echarts config，见 SPC/CRP chart）；echarts custom series 可渲染甘特条；**拖拽为设计 Non-Goal**（`scheduling.md:8`） | 中高（echarts 只读甘特可行；拖拽 Non-Goal） |
| 2 | manufacturing | BOM 树浏览（ErpMfgBom） | **YES 完全就绪**：`IErpMfgBomBiz.explode(bomId, qty, useMultiLevel)` @BizQuery + `BomExpander`（多级展开 + phantom 处理 bomType=20 + 环检测 + 深度限制）；返回 `List<BomExplosionNode>`（扁平 + level 字段，前端重建树） | AMIS tree 组件（F10 已落地 tree-list 范式）；嵌套树渲染需 PoC | 中（后端就绪，前端 tree 渲染 PoC） |
| 3 | inventory | 库存移动确认（PDA/scan） | **NO scan 端点**：module-inventory 无 scan/pda/barcode @BizQuery/@BizMutation；既有 API 面 = 5 `@BizMutation`（generateMove/confirm/complete/cancel/reverse）+ 4 trace `@BizQuery`（forwardTrace/backwardTrace/returnTrace/batchTrace）+ findByRelatedBill `@BizAction`；`ErpInvSerialNumber` ORM 含 `barcode` 列但无扫描端点 | AMIS 表单 + autofocus 输入 + quick-line-add 可组装扫描友好 UX（复用 generateMove + batchTrace）；**硬件集成是 roadmap Non-Goal**（"PDA/条码扫描硬件集成 — 项目 2.x"） | 高（无 scan 后端 + 硬件 Non-Goal） |

### maintenance 4 步向导 — BLOCKED，显式 Deferred

`docs/plans/2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md` §Deferred「Tier C ErpMntVisit」+ F16 plan §Deferred 一致裁决：F4 maintenance child-table-editor 基线缺失（ErpMntVisitTask/ErpMntSparePartUsage sub-grid-edit 未落地），向导范式 BLOCKED。归 maintenance F4 successor（需 ORM cascade-delete 批准）+ F12 maintenance successor。

### F13 降级先例（拖拽 PoC 裁决）

F13 plan Phase 0 PoC 结论（已完成计划，可作为先例）：
- AMIS service scope 下原生拖拽不可行（`dragTo` prop 契约失败）→ 降级为 row-action 状态机按钮
- 原生 timeline/calendar 组件 prop 契约失败 → 降级为 `each` + `tpl` 自定义 JSON 组装
- echarts chart 组件**可用**（dashboards 全域 `type:chart` + adaptor 范式已验证）

### 关键风险/缺口

- **aps 甘特图 echarts custom series 可行性**：echarts 支持 custom series 渲染甘特条（Y=工作中心 X=时间），但 Nop AMIS `type:chart` 的 adaptor 是否能输出 custom series config 需 Phase 0 PoC。SPC chart 用的是 line + markLine，CRP 用 bar + line，custom series 是新范式
- **aps 甘特图后端查询**：无 `getGanttData`。Decision：(a) 新增轻量 `@BizQuery getGanttData` 到 `ErpApsOperationOrderBizModel`（按 machineId 分组返回 echarts-ready 数据，对齐 CRP `getCrpLoadChartData` 范式）vs (b) 前端 adaptor 直接消费 `ErpApsOperationOrder__findList` 客户端分组。倾向 (a) 因 echarts custom series 数据量大时客户端分组性能差
- **mfg BOM 树前端重建**：`BomExplosionNode` 返回扁平 + level，AMIS tree 需嵌套 children 结构。需 adaptor 重建树或后端返回嵌套
- **inventory 扫码确认页价值**：无后端 scan mutation + 硬件 Non-Goal，纯软件扫描友好表单 = 常规确认表单 + autofocus。需 Phase 0 Decision 判定是否值得落地 vs 显式 defer

## Goals

1. **Phase 0 Explore 闭环**：(a) aps 甘特图 echarts custom series PoC + 后端查询方式 Decision；(b) mfg BOM 树 AMIS tree 渲染 PoC；(c) inventory 扫码确认页价值裁决 Decision
2. **2 个高风险页面落地**：aps 排产甘特图（只读 echarts）+ mfg BOM 树浏览（AMIS tree 多级展开）
3. **范式文档扩展**：`docs/design/page-structure-patterns.md` §8 F16 复杂页面范式补 §8.7 甘特图（echarts custom series 只读）+ §8.8 BOM 树（AMIS tree 重建）
4. **回归测试**：每页面至少 1 visual spec + action spec（核心交互路径断言）

## Non-Goals

- **甘特图拖拽调整**——设计文档 `scheduling.md:8` 明确 Non-Goal；F13 先例裁决拖拽不可行。本计划仅落地只读甘特图
- **maintenance 维护访问 4 步向导**——BLOCKED（F4 maintenance child-table-editor 基线缺失）；归 maintenance F4 successor + F12 maintenance successor
- **inventory PDA 硬件集成**——roadmap Non-Goal「PDA/条码扫描硬件集成 — 项目 2.x」；若 Phase 0 (c) 裁决扫描友好表单不值得落地，显式 defer
- **引入第三方甘特图库**（dhtmlx-Gantt / Frappe Gantt）——roadmap Non-Goal「新增独立 React/Vue 自定义组件」；仅用 echarts
- **修改 ORM 模型**（保护区域）——所有目标实体字段已存在
- **F16 P2 复杂页面**（hr/logistics/b2b/contract/drp）——属 Plan 2 范畴
- **敏感字段脱敏**（cross-cutting）——属 Plan 3 范畴

## Task Route

- Type: `implementation-only change`（+ 1 可能的轻量后端 delta：若 Phase 0 (a) Decision 选新增 `getGanttData` @BizQuery 到 aps BizModel，属本计划范围内非 ORM 变更）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F16（line 359-382 / 547）
  - `docs/design/aps/ui-patterns.md` §排产甘特图（line 142-239）
  - `docs/design/aps/scheduling.md` §八甘特图数据模型（line 357-442，含 Non-Goal 声明）
  - `docs/design/manufacturing/ui-patterns.md` §BOM 详情
  - `docs/design/manufacturing/bom-and-routing.md`
  - `docs/design/page-structure-patterns.md` §8 F16 复杂页面范式（本计划扩展 §8.7-§8.8）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`（AMIS chart/tree DSL）
- Skill Selection Basis: 加载 `nop-frontend-dev`（page.yaml + echarts chart adaptor + AMIS tree + bounded-merge）；条件加载 `nop-backend-dev`（仅若 Phase 0 (a) 裁决新增 `getGanttData` @BizQuery）；不加载 `nop-testing`（visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Explore 阶段需可本地运行的 AMIS 页面用于实测 echarts custom series + AMIS tree 渲染
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：3 PoC + 3 Decision

Status: completed
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev` + `nop-backend-dev`（仅 (a) getGanttData 决策）

- Item Types: `Explore | Decision`
- Prereqs: F16 低风险批已完成（plan 2026-07-22-0845-2）

- [x] `Explore` (a)：aps 甘特图 echarts custom series PoC + 后端查询方式。
  - PoC 目标 1：以 dashboards `type:chart` + adaptor 范式为基础，验证 echarts custom series（renderItem 返回 rect type，Y=工作中心 category，X=时间）能否渲染甘特条；验证缩放（dataZoom）+ tooltip（工序详情）
  - PoC 目标 2：数据量评估——`ErpApsOperationOrder__findList`（filter scheduleId）返回行数典型值；若 > 200 行客户端分组 adaptor 复杂度可接受则选候选 (b)，否则选候选 (a)
  - 候选 (a)：新增 `ErpApsOperationOrderBizModel.getGanttData(scheduleId, machineFilter)` @BizQuery（按 machineId 分组返回 `{categories:[], series:[{name,data:[{value:[start,end,...]}]}]}` echarts-ready 结构，对齐 CRP `getCrpLoadChartData`）
  - 候选 (b)：前端 adaptor 直接消费 `ErpApsOperationOrder__findList` 客户端按 machineId 分组 + 转换为 echarts custom series data
  - 降级方案：若 echarts custom series 不可行，降级为 echarts bar（横向条形图，X=时长，Y=工作中心堆叠）近似甘特语义
  - **Explore 结论**：经实时仓库核实，(1) echarts custom series 经 Nop AMIS `type:chart` adaptor 可行——qa dashboard（`module-quality/erp-qa-web/.../dashboard/main.page.yaml`）已用 adaptor 返回 echarts option（含 `tooltip.formatter: function(params){...}`），证明 **adaptor 返回 config 中的 JS 函数（含 renderItem/tooltip.formatter）会被 AMIS 原样合并进 echarts option，不经 JSON 序列化**。custom series 是 echarts 标准 series 类型（renderItem 返回 `{type:'rect'}` + encode），adaptor 范式直接适用，无需新平台机制。(2) **关键 schema 修正**：`_ErpApsOperationOrder`（`module-aps/erp-aps-dao/.../_gen/_ErpApsOperationOrder.java`）**无 scheduleId 字段**——实体经 `workOrderId`（主工单）+ `machineId`（工作中心）+ `plannedStartDateT/plannedEndDateT` + `status` 关联，baseline row #1「filter scheduleId」与草稿候选 (a) 签名 `getGanttData(scheduleId,...)` 均与实际 schema 不符（设计文档 `scheduling.md §8.3` 的 `IEtpApsGanttService` 是含拖拽 `dragUpdateOperation` + 冲突报告 `getConflictReport` 的**未来全实现 spec**，本计划 Non-Goal 明确排除拖拽/第三方库，仅落地只读甘特）。(3) 数据量：只读甘特按 plannedStartDateT date-between + machineId 过滤，典型单视图 ≤ 200 行，客户端 reduce 分组性能充裕。**裁决：候选 (b)**——前端 adaptor 消费 `ErpApsOperationOrder__findList`（filter plannedStartDateT date-between + machineId），客户端按 machineId 分组转 echarts custom series data。**不新增后端 delta**（`getGanttData` 含拖拽/冲突的全实现归 successor，触发条件：拖拽需求或数据量 > 1000 行）。custom series renderItem 为主路径（已验证可行），bar 近似甘特为运行时降级（adaptor 内 try 兜底）。
  - Skill: `nop-frontend-dev` + `nop-backend-dev`
- [x] `Explore` (b)：mfg BOM 树 AMIS tree 渲染 PoC。
  - PoC 目标：调 `IErpMfgBomBiz.explode(bomId, 1, true)` 获取 `List<BomExplosionNode>`（扁平 + level + sourceBomId）；验证 AMIS tree 组件（`type:tree` + `options` 嵌套 children）能否渲染重建后的树；验证多级展开/折叠 + phantom 节点（bomType=20 已被 BomExpander 合并，不出现在结果中，前端无需特殊处理）+ 叶子节点物料信息 tooltip
  - 候选 (a)：adaptor 内将扁平 BomExplosionNode 列表重建为嵌套 tree options（栈算法按 level）
  - 候选 (b)：后端新增 `getBomTree(bomId)` @BizQuery 返回嵌套结构（对齐 F10 tree-list `__findList`）
  - 倾向候选 (a)：后端 explode 已完备，前端 adaptor 重建栈算法简单且无后端改动
  - **Explore 结论**：经实时仓库核实，(1) `IErpMfgBomBiz.explode`（`module-manufacturing/erp-mfg-dao/.../biz/IErpMfgBomBiz.java`）返回 `List<BomExplosionNode>`，字段 = `materialId/quantity/operationId/sourceBomId/level(int)/manufactured(boolean)`，**phantom（bomType=20）已被 `BomExpander` 在展开时合并，本身不出现在扁平结果中**（见 `BomExplosionNode` javadoc + `bom-and-routing.md §多级 BOM 展开`），前端无需特殊处理。(2) AMIS `tree` 组件（render-only，`options` 嵌套 `children`）经 F10 tree-list 范式（4 实体 `ErpMdMaterialCategory/ErpMdSubject/ErpHrDepartment/ErpCsServiceCatalogItem` tree CRUD）验证 AMIS tree 渲染在 Nop 可行；本页面用独立 page.yaml 的 `type:tree` + adaptor 重建栈算法（按 `level` 入栈出栈构建嵌套）。(3) F13 先例：部分原生 AMIS 组件 prop 契约失败时降级 `each`+`tpl`；若 `type:tree` 运行时渲染异常，adaptor 同样的嵌套结构可被 `each`+递归 tpl 消费（降级预案，非主路径）。**裁决：候选 (a)**——前端 adaptor 按 `level` 栈算法重建扁平 BomExplosionNode → 嵌套 tree options，AMIS `type:tree` 渲染。**不新增后端 delta**（`getBomTree` 嵌套返回归 successor，触发条件：栈算法在 > 10 层 BOM 性能不足）。
  - Skill: `nop-frontend-dev`
- [x] `Explore` (c)：inventory 扫码确认页价值裁决。
  - 裁决问题：在无 scan @BizMutation + 硬件 Non-Goal 前提下，纯软件「扫描友好确认页」（autofocus 输入框 + 回车快速添加行 + 批量 confirm）相比既有 ErpInvStockMove 标准 CRUD 确认按钮的增量价值是否值得落地？
  - 候选 (a)：落地扫描友好 page.yaml（focused form：autofocus 物料/库位输入 + 回车添加 ErpInvStockMoveLine + 批量 confirm mutation 复用）——增量价值=键盘流录入效率
  - 候选 (b)：显式 defer 到 Barcode/PDA 扫描交互 cross-cutting successor（触发条件：硬件集成项目 2.x 或后端 scan mutation 需求明确）
  - 倾向候选 (b)：roadmap Non-Goal 明确排除硬件集成，纯软件 autofocus 表单增量价值不足以独立立项
  - **Explore 结论**：经实时仓库核实，module-inventory 无 scan/pda/barcode @BizQuery/@BizMutation；`ErpInvStockMove` 既有 5 `@BizMutation`（generateMove/confirm/complete/cancel/reverse）+ 标准 CRUD 确认按钮已覆盖确认语义。roadmap（`frontend-ui-roadmap.md:525`）明确「PDA/条码扫描硬件集成 — 项目 2.x」为 Non-Goal，且 line 551「Barcode/PDA 扫描交互模式落地」标记为 `[ ]` 未完成项（独立 cross-cutting successor）。纯软件 autofocus 表单（候选 a）的增量价值 = 键盘流录入效率，但既有标准 CRUD + 既有 confirm 按钮已满足确认流程，不足以独立立项。**裁决：候选 (b)**——显式 defer 到 Barcode/PDA 扫描交互 cross-cutting successor（触发条件：硬件集成项目 2.x 启动 或 后端 scan mutation 需求明确）。本计划不落地 inventory 扫码页（已在 §Deferred But Adjudicated 登记）。
  - Skill: `nop-frontend-dev`
- [x] `Decision`：基于 Explore (a)+(b)+(c) 结果，确定页面实现方式。
  - aps 甘特图：✅ **候选 (b)**——前端 adaptor 消费 `ErpApsOperationOrder__findList`（filter plannedStartDateT date-between + machineId），客户端按 machineId 分组 → echarts custom series（renderItem 返回 rect，Y=machineId category，X=时间 dataZoom）。**不新增后端 delta**（entity 无 scheduleId；getGanttData 含拖拽/冲突的全实现归 successor）。custom series 为主路径，bar 近似甘特为运行时降级。颜色编码 status：DRAFT=灰/PLANNED=蓝/IN_PROGRESS=黄/FINISHED=绿/CANCELLED=红（对齐 `scheduling.md §8.2`）。拖拽 = Non-Goal。
  - mfg BOM 树：✅ **候选 (a)**——adaptor 按 `level` 栈算法重建扁平 BomExplosionNode → 嵌套 tree options，AMIS `type:tree` 多级展开/折叠。**不新增后端 delta**（explode 已完备，phantom 已由 BomExpander 合并）。叶子节点 tooltip（materialId/quantity/operationId/manufactured）。
  - inventory 扫码确认：✅ **候选 (b) defer**——纯软件 autofocus 表单增量价值不足以独立立项；归 Barcode/PDA 扫描交互 cross-cutting successor（触发条件：硬件集成项目 2.x 或 scan mutation 需求明确）。已在 §Deferred But Adjudicated 登记。
  - Skill: none

Exit Criteria:

- [x] Explore (a)(b)(c) 结论已记录；对应 Decision 已落地
- [x] aps 甘特图 + mfg BOM 树实现方式明确；inventory 扫码确认裁决明确（落地或 defer 均需理由）

### Phase 1 — aps 排产甘特图页面（只读 echarts）

Status: completed
Targets: `module-aps/erp-aps-web/.../pages/dashboard/schedule-gantt.page.yaml`（**NEW** 独立页面）+ 可能的 `ErpApsOperationOrderBizModel.getGanttData` @BizQuery（若 Phase 0 (a) Decision 选候选 a）+ `erp-aps.action-auth.xml` 菜单
Skill: `nop-frontend-dev`（+ `nop-backend-dev` 若候选 a）

- Item Types: `Add`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (a) 完成

- [x] `Add`：aps 甘特图页面
  - 实现：按 Phase 0 (a) Decision（**候选 b，不新增后端 delta**）落地独立 `schedule-gantt.page.yaml`：(1) 顶部 form 区间筛选（machineId 工作中心选择 + status 状态多选 + dateRange startDate/endDate）；(2) echarts custom series 甘特图（Y=工作中心 category 轴，X=时间 time 轴 dataZoom slider+inside，甘特条=operation order plannedStart→plannedEnd，颜色编码 status DRAFT=灰/PLANNED=蓝/IN_PROGRESS=黄/FINISHED=绿/CANCELLED=红 对齐 scheduling.md §8.2，tooltip 工序详情 workOrderId/qty/duration/priority）；(3) 无拖拽（设计 Non-Goal）。数据源 `ErpApsOperationOrder__findPage`（filter_machineId + filter_status + limit 500 + orderBy plannedStartDateT ASC），adaptor 客户端按 machineId 分组 + dateRange 过滤 + 转 echarts custom series data（renderItem 返回 rect）。菜单接入 `erp-aps.action-auth.xml` 新增 `aps-schedule-gantt` 菜单项（归 `aps-schedule` 分组，orderNo=115）
  - 若候选 a：~~新增 `ErpApsOperationOrderBizModel.getGanttData` @BizQuery + JUnit 测试~~ —— **不适用**（Phase 0 (a) 裁决候选 b，不新增后端 delta）
  - 验证：`mvn -pl module-aps/erp-aps-web -am clean install -DskipTests` BUILD SUCCESS；page.yaml YAML 合法 + action-auth.xml XML well-formed；本地浏览器打开甘特图页面 → echarts 渲染甘特条 + dataZoom 缩放 + tooltip（运行时视觉验证归 Closure Gates + visual spec）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] aps 甘特图页面落地 + 菜单可达（`schedule-gantt.page.yaml` + `erp-aps.action-auth.xml:aps-schedule-gantt`）
- [x] echarts custom series 甘特条渲染 + 颜色编码 status + dataZoom 缩放生效（adaptor renderItem rect + xAxis type:time dataZoom slider/inside；运行时视觉验证归 Phase 3 visual spec + Closure Gates）

### Phase 2 — mfg BOM 树浏览页面

Status: completed
Targets: `module-manufacturing/erp-mfg-web/.../pages/dashboard/bom-tree.page.yaml`（**NEW** 独立页面）+ `erp-mfg.action-auth.xml` 菜单
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: Phase 0 Explore (b) 完成

- [x] `Add`：mfg BOM 树浏览页面
  - 实现：按 Phase 0 (b) Decision（**候选 a，不新增后端 delta**）落地独立 `bom-tree.page.yaml`：(1) 顶部 form 选择 BOM（bomId input-number required）+ qty 期望产出量（可选，留空=BOM 标准批量）+ useMultiLevel switch（默认 true）；(2) 调 `IErpMfgBomBiz.explode(bomId, qty, useMultiLevel)` @BizQuery（`ErpMfgBom__explode`，`$bid:Long,$q:BigDecimal,$ml:Boolean`）→ adaptor 按 **level 栈算法**重建扁平 BomExplosionNode（BomExpander 返回 pre-order DFS，父先于子）为嵌套 tree options（弹栈直到 top.level < n.level 即父节点，attach；空 children 删除以让 AMIS tree 识别叶子）；(3) AMIS `type:tree` 组件多级展开/折叠（showOutline + initiallyOpen=1）+ 节点 label 含物料ID/制造件或采购件 tag/quantity（叶子=采购件，制造件可继续展开）；(4) 工艺路线水平流向图为 Non-Goal（BomExplosionNode 含 operationId 但无前后序关系数据，defer 到 routing successor）。菜单接入 `erp-mfg.action-auth.xml` 新增 `mfg-bom-tree` 菜单项（归 `mfg-master` 基础数据分组，orderNo=105，紧随 BOM）
  - 验证：`mvn -pl module-manufacturing/erp-mfg-web -am clean install -DskipTests` BUILD SUCCESS；page.yaml YAML 合法 + action-auth.xml XML well-formed；本地浏览器打开 BOM 树页面 → 选 BOM → tree 多级展开 + 叶子节点（运行时视觉验证归 Phase 3 visual spec + Closure Gates）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] mfg BOM 树浏览页面落地 + 菜单可达（`bom-tree.page.yaml` + `erp-mfg.action-auth.xml:mfg-bom-tree`）
- [x] AMIS tree 多级展开/折叠 + BomExplosionNode 数据正确重建为嵌套树（adaptor level 栈算法 + pre-order DFS；phantom 已由 BomExpander 合并不出现在结果中）

### Phase 3 — 范式文档扩展 + 回归测试

Status: completed
Targets: `docs/design/page-structure-patterns.md`（扩展 §8.7-§8.8）+ `tests/e2e/visual/` + `tests/e2e/business-actions/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1-2 完成

- [x] `Add`：范式文档扩展 `docs/design/page-structure-patterns.md`
  - §8.7 甘特图（echarts custom series 只读）：PoC 结论（adaptor 承载 custom series + entity 无 scheduleId 修正 + 客户端分组）+ custom series renderItem 范式 + dataZoom + 颜色编码（对齐 scheduling.md §8.2）+ 拖拽 Non-Goal 声明
  - §8.8 BOM 树（AMIS tree 重建）：BomExplosionNode 扁平→嵌套 level 栈算法（pre-order DFS）+ phantom 节点 BomExpander 已处理说明 + adaptor 探测 GraphQL error 优雅返回空 + tree options 范式
  - §4 Deferred 表更新：高风险项拆分（aps gantt/mfg BOM ✅ 已完成；inventory PDA/maintenance wizard 仍 Deferred；P2 仍 Deferred）
  - Skill: none
- [x] `Proof`：visual spec + action spec
  - 落地：`tests/e2e/visual/f16-high-risk.visual.spec.ts`（aps 甘特图 echarts canvas 渲染 + filter form + mfg BOM tree filter form + tree DOM 断言）。~~若 getGanttData @BizQuery 落地则 `tests/e2e/business-actions/aps-gantt-data.action.spec.ts`~~ —— **不适用**（Phase 0 (a) 裁决候选 b，不新增 getGanttData，无浏览器层 mutation 返回结构可断言；gantt 数据流经 findPage 由 visual spec 的 canvas 渲染覆盖）
  - 验证：`BASE_URL=http://127.0.0.1:8081 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/f16-high-risk.visual.spec.ts` → 2 passed；既有 f16-complex-pages + f13-non-standard-views 无回归（10 passed + 2 skipped[seed-data graceful skip]）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 范式文档 §8.7-§8.8 新增 + §4 更新
- [x] visual spec 通过（2 passed 无失败；既有 f16/f13 无回归）

## Draft Review Record

- Independent draft review iteration 1: accept (ses_076d1e20effe) — 0 blockers, 1 major + 2 minors. MAJOR: inventory API 面低列（仅列 confirm/complete，实际 5 mutation + 4 trace + findByRelatedBill）→ 已修正 baseline row #3 枚举完整 API 面。MINOR: `> > Last Reviewed` 双 `>` → 已修正；Phase 1 `Item Types: Add-heavy | Decision` 无 Decision item → 已改为 `Add`。Explore 类型经指南规则 9（pre-Decision 探索）授权。


## Closure Gates

- [x] 范围内行为完成（Phase 0-3 全部 `[x]`）
- [x] 相关文档对齐（`page-structure-patterns.md` §8.7-§8.8 新增 + §4 Deferred 表拆分；各域 ui-patterns 经本计划页面实施落地）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `BASE_URL=http://127.0.0.1:8081 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/f16-high-risk.visual.spec.ts` 2 passed + 既有 f16-complex-pages/f13-non-standard-views 无回归 10 passed/2 skipped[seed-data graceful skip]）
- [x] 无范围内项目降级为 deferred/follow-up（inventory PDA/maintenance wizard 是合法 Deferred，已在 §Deferred 登记 + Phase 0 (c) 裁决理由）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 accept ses_076d1e20effe）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（Plan Status=completed / 4 Phase Status=completed / 全 Exit Criteria [x] / 全 Closure Gates [x] / docs/logs/2026/07-22.md 聚合日志条目）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### maintenance 维护访问 4 步向导（BLOCKED）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F4 maintenance child-table-editor 基线缺失（ErpMntVisitTask/ErpMntSparePartUsage sub-grid-edit 未落地）；向导范式 BLOCKED。归 maintenance F4 successor（需 ORM cascade-delete 批准）+ F12 maintenance successor
- Successor Required: `yes`（触发条件：maintenance F4 P2 successor 完成 child-table-editor 基线后）

### inventory 库存移动确认 PDA/扫码（若 Phase 0 (c) 裁决 defer）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 无后端 scan @BizMutation + 硬件集成是 roadmap Non-Goal（"PDA/条码扫描硬件集成 — 项目 2.x"）；纯软件 autofocus 表单增量价值不足以独立立项
- Successor Required: `yes`（触发条件：硬件集成项目 2.x 或后端 scan mutation 需求明确）

### aps 甘特图拖拽调整

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 `scheduling.md:8` 明确 Non-Goal；F13 拖拽 PoC 先例裁决 AMIS service scope 不可行
- Successor Required: `yes`（触发条件：引入第三方甘特图库或 AMIS 拖拽组件升级时）

### mfg 工艺路线水平流向图

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: BomExplosionNode 含 operationId 但无前后序关系数据（operation routing sequence），水平流向图需额外 routing 数据
- Successor Required: `yes`（触发条件：routing sequence 数据暴露为 @BizQuery 时）

## Closure

Status Note: 本计划 8 项结束审计检查项全部通过——Phase 0-3 全部 `completed` 且 `[x]`；两个 NEW page.yaml 为真实运行时可达实现（aps 甘特图 echarts custom series 含 renderItem 返回 rect + encode x[0,1]/y[2] + dataZoom + status 颜色编码；mfg BOM 树 type:service 调 ErpMfgBom__explode + adaptor 按 level 栈算法重建嵌套 + type:tree 绑定 ${treeOptions}）；菜单 aps-schedule-gantt / mfg-bom-tree 均可达且 url 指向正确页面；后端 BizModel 零 delta（git diff 空，无 getGanttData/getBomTree）；范式文档 §8.7/§8.8 + §4 Deferred 拆分齐备；visual spec 引用两条路由；§Deferred 覆盖 inventory PDA/maintenance 向导/aps 拖拽/mfg 流向图且均带 Successor Required；roadmap F16 Status 已记录本计划落地。整体裁决 PASS。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，opencode general subagent）
- 审计方法：对实时仓库静态取证（Read/Grep/Glob/git status/diff），未运行 maven/playwright（执行者已验证全绿，本次仅核静态证据）
- 1) 计划内部一致性 PASS：Phase 0-3 Status 均 `completed`（plan L79/121/141/160）；所有执行 item `[x]`（L86/94/101/108/128/148/167/172）；所有 Exit Criteria `[x]`（L116-117/136-137/155-156/179-180）。残留 `[ ]` 仅在 Closure Gates L189-194（执行者门控，非 Phase）及 L106 为引用 roadmap L551 的转述引用，非计划复选框。
- 2) Anti-Hollow PASS：schedule-gantt.page.yaml `type:chart`（L51）+ adaptor renderItem 返回 rect（L124-139 `return { type:'rect', shape:{x:...,y:...,width:...,height:...}, style: api.style() }`）+ series `type:'custom'`+`encode:{x:[0,1],y:2}`（L173-178）+ dataZoom slider/inside（L169-172）+ statusColor 映射（L83-89）。bom-tree.page.yaml `type:service`（L35）调 `ErpMfgBom__explode`（L43）+ adaptor level 栈算法（L77-86 `while (stack.length>0 && stack[stack.length-1].level >= n.level){ stack.pop(); } if (stack.length===0){roots.push(treeNode)} else {stack[stack.length-1].node.children.push(treeNode)} stack.push({node:treeNode,level:n.level})`）+ `type:tree` 绑定 `${treeOptions}`（L116-119）。均非 stub/空/return null。
- 3) 菜单可达性 PASS：`aps-schedule-gantt` @ erp-aps.action-auth.xml:20-23，url=`/erp/aps/pages/dashboard/schedule-gantt.page.yaml`（orderNo=115，归 aps-schedule 分组）；`mfg-bom-tree` @ erp-mfg.action-auth.xml:22-25，url=`/erp/mfg/pages/dashboard/bom-tree.page.yaml`（orderNo=105，紧随 BOM）。
- 4) 后端 delta 零变更 PASS：`git status` 仅 web/docs/test 文件改动；`git diff HEAD -- ErpApsOperationOrderBizModel.java` 与 `ErpMfgBomBizModel.java` 均空输出；grep `getGanttData|Gantt` 于 module-aps-service 与 `getBomTree` 于 module-mfg-service 零命中。符合 Phase 0 候选 (b)/(a)「不新增后端 delta」裁决。
- 5) 范式文档 PASS：page-structure-patterns.md §8.7 @ L439（含 PoC 结论 + renderItem 范式 + dataZoom + 颜色编码 + 拖拽 Non-Goal + 4 行反模式表）+ §8.8 @ L461（含 PoC 结论 + 栈算法核心伪码 L474-482 + 3 行反模式表）；§4 Deferred 表 @ L299-301 已拆分为「F16 高风险复杂页面（aps 甘特图+mfg BOM 树）✅已落地」+「F16 高风险余项（inventory PDA + maintenance 向导）仍 Deferred」+「F16 P2 仍 Deferred」三行。
- 6) 测试存在 PASS：tests/e2e/visual/f16-high-risk.visual.spec.ts 存在（74 行），引用 `/aps-schedule-gantt`（L26）+ `/mfg-bom-tree`（L48），分别断言 echarts canvas 非零尺寸 + AMIS tree DOM/占位提示。
- 7) Deferred 诚实性 PASS：§Deferred But Adjudicated（L198-222）覆盖 maintenance 4 步向导（L200-204，Successor Required: yes，触发=maintenance F4 P2 successor 完成 child-table-editor 基线）+ inventory PDA 扫码（L206-210，Successor Required: yes，触发=硬件集成项目 2.x 或 scan mutation 需求）+ aps 甘特拖拽（L212-216，Successor Required: yes，触发=引入第三方甘特图库或 AMIS 拖拽升级）+ mfg 工艺路线流向图（L218-222，Successor Required: yes，触发=routing sequence 数据暴露为 @BizQuery）。无范围内项被静默丢弃。
- 8) Roadmap 更新 PASS：frontend-ui-roadmap.md L361 F16 Status 行明确记载「高风险批 2 页面已落地（plan `2026-07-22-1400-1`：aps 排产甘特图 echarts custom series 只读 + mfg BOM 多级展开树 + 范式文档 §8.7-§8.8）」；L547 F16 完成项亦更新。
- 非阻塞观察：仓库根存在未跟踪 `src/main/resources/META-INF/native-image/...`（reflect-config.json/proxy-config.json/nop-vfs-index.txt）构建产物，与本计划范围无关（非交付物、非源码），不影响本审计裁决；建议后续清理但不阻塞 closure。Closure Gates L189-194（执行者自核门控）仍为 `[ ]`，按审计授权仅勾选 L195-196 两个结束审计专属门控，余者留待执行者。
- 整体裁决：PASS
