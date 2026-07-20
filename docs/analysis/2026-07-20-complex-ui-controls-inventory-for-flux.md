# 复杂前端控件盘点：nop-app-erp 视角下的 nop-chaos-flux 控件需求

> 日期：2026-07-20（v2 — 补充 flux-guide 审计结果）
> 范围：nop-app-erp 18+1 域全部 337 hand-written view.xml + 726 page.yaml + 设计文档
> 数据来源：nop-chaos-flux-master `flux-guide/`（31 个 design-patterns + flux-types）
> 目标：识别 nop-app-erp 需要的、但 nop-chaos-flux 尚未提供的非标准控件

---

## 1. Flux 已覆盖的控件（无需新增）

以下控件在 flux-guide 中已有完整设计和类型定义，nop-app-erp 可直接使用：

| 控件 | Flux type | 来源文档 |
|------|-----------|---------|
| **Tabs** | `type: "tabs"` | `08-tabs-state.md`, `design-patterns/tabs.md` |
| **Wizard** | `type: "wizard"` | `design-patterns/wizard.md` |
| **Chart** (bar/line/pie/scatter/area) | `type: "chart"` | `design-patterns/chart.md` |
| **Steps** | `type: "steps"` | `design-patterns/steps-timeline.md` |
| **Timeline** (vertical/horizontal) | `type: "timeline"` | `design-patterns/steps-timeline.md` |
| **Tree / TreeSelect** | `type: "tree"`, `type: "tree-select"` | `design-patterns/tree.md` |
| **QrCode** | `type: "qrcode"` | `design-patterns/remaining-components.md` |
| **File Upload** | `type: "input-file"`, `type: "input-image"` | `design-patterns/file-upload.md` |
| **Card / Cards list** | `type: "card"`, `type: "cards"` | `design-patterns/cards.md`, `design-patterns/content-display.md` |
| **Status / Mapping** | `type: "status"` | `design-patterns/content-display.md` |
| **Combo / InputTable** | `type: "combo"`, `type: "input-table"` | `01-quickstart.md`, `design-patterns/combo-input-table.md` |
| **Transfer / Picker** | `type: "transfer"`, `type: "picker"` | `design-patterns/picker-transfer.md`, `remaining-components.md` |
| **Crud** | `type: "crud"` | `design-patterns/crud.md` |
| **Form fields** (input-text/number/email/select/switch/etc.) | 各种 input-* | `design-patterns/form-basic-fields.md`, `form-advanced-fields.md` |
| **DetailView / DetailField** | `type: "detail-view"`, `type: "detail-field"` | `remaining-components.md` |
| **Condition Builder** | `type: "condition-builder"` | `remaining-components.md` |
| **Carousel** | `type: "carousel"` | `remaining-components.md` |
| **Audio / Video** | `type: "audio"`, `type: "video"` | `remaining-components.md` |
| **Fragment / Loop / Recurse / Reaction** | 结构节点 | `07-structural-nodes.md` |

**结论**：nop-app-erp 的 F12（Tabs/向导/仪表板）、F13 中的 Timeline、Chart、Steps、Tree 等场景均可由 Flux 原生覆盖，不需要新增渲染器。

---

## 2. 真实控件缺口（Flux 未覆盖）

以下为 nop-app-erp 需要的、但 flux-guide 中**不存在**的控件：

### 2.1 甘特图（Gantt Chart）

| 属性 | 值 |
|------|------|
| **影响域** | APS 排产 (`module-aps`)、项目管理 (`module-projects`)、制造工单 (`module-manufacturing`) |
| **场景** | 工作中心×时间线资源排布、项目任务 WBS 时间线、工单计划 vs 实际进度 |
| **核心交互** | Y=资源/工作中心 X=时间线、水平条拖拽调整、缩放(日/周/月)、依赖连线、约束叠加层(灰色维护块)、优先级颜色编码 |
| **设计文档** | `docs/design/aps/scheduling.md §8`（完整 JSON 数据契约） |
| **Flux 状态** | ❌ `type: "gantt"` 不存在。Chart 不支持甘特条渲染，Timeline 不支持资源行+时间线二维布局 |
| **建议优先级** | **P1** — 排产是 ERP 核心差异化功能 |
| **工作量** | 高 — 需独立渲染器，含拖拽引擎和缩放控制 |

### 2.2 看板 / Kanban Board

| 属性 | 值 |
|------|------|
| **影响域** | CRM (`module-crm`)、客服 (`module-cs`)、项目管理 (`module-projects`)、HR (`module-hr`) |
| **场景** | 招聘管道、工单状态追踪、任务看板、商机阶段管理 |
| **核心交互** | 多列管道、拖拽卡片跨列、卡片内容模板化、动态列数、SLA 超时标记 |
| **设计文档** | `docs/design/human-resource/ui-patterns.md §6`、`docs/design/crm/ui-patterns.md` |
| **Flux 状态** | ❌ `type: "kanban"` 不存在。Cards 不支持列分组和跨列拖拽 |
| **建议优先级** | **P2** — 多域共享，非核心阻断 |
| **工作量** | 高 — 需独立渲染器，含拖拽引擎 |

### 2.3 排班日历 / 休假日历（Calendar / Scheduling Grid）

| 属性 | 值 |
|------|------|
| **影响域** | HR (`module-hr`)、制造 (`module-manufacturing`)、财务 (`module-finance`) |
| **场景** | 月度排班矩阵（员工×日期）、团队休假日历（颜色编码按假别）、会计期间日历（开/关/锁定状态） |
| **核心交互** | 行=员工 列=日期矩阵、颜色编码色块、月切换、冲突检测、轮换模式预览 |
| **设计文档** | `docs/design/human-resource/shift-scheduling.md`（356行）、`docs/design/human-resource/ui-patterns.md §4` |
| **Flux 状态** | ❌ 无日历矩阵渲染器。有 `react-day-picker` 依赖，但那是单日期选择器，不是月视图排班矩阵 |
| **建议优先级** | **P2** — HR 排班选配，但休假日历影响面广 |
| **工作量** | 高 — 需独立渲染器 |

### 2.4 条码输入 / 扫描识别（Barcode Scanner Input）

| 属性 | 值 |
|------|------|
| **影响域** | 库存 (`module-inventory`)、采购 (`module-purchase`)、制造 (`module-manufacturing`)、质量 (`module-quality`) |
| **场景** | PDA 仓库操作（收货/上架/拣货/发货/盘点/领料/质检）、物料条码/批次/序列号/库位多类型自动识别 |
| **核心交互** | 相机扫描输入框、手动输入回退、批量扫描缓冲、扫描成功/失败即时反馈 |
| **设计文档** | `docs/design/inventory/barcode-integration.md`（168行） |
| **Flux 状态** | ❌ `type: "barcode-input"` 不存在。QrCode 只负责展示二维码，不负责扫描识别 |
| **建议优先级** | **P3** — 项目级 Non-Goal（"PDA/条码扫描硬件集成 — 项目 2.x"），但控件定义可提前预留 |
| **工作量** | 中 — 表单控件子类型，底层依赖 WebRTC + barcode-detection API |

### 2.5 版本对比 Diff 视图

| 属性 | 值 |
|------|------|
| **影响域** | 合同 (`module-contract`)、B2B (`module-b2b`) |
| **场景** | 合同版本双栏 diff、EDI 报文语法高亮对比、数值差值箭头 |
| **核心交互** | 双栏 diff（新增=绿/删除=红/修改=黄）、仅差异行过滤 |
| **设计文档** | `docs/design/contract/ui-patterns.md` |
| **Flux 状态** | ❌ `type: "diff-view"` 不存在 |
| **建议优先级** | **P3** — F16 项目，选配 |
| **工作量** | 中 — 可集成 diff-match-patch |

### 2.6 SPC 控制图（Control Chart）

| 属性 | 值 |
|------|------|
| **影响域** | 质量 (`module-quality`) |
| **场景** | X-bar R 图、P 图、U 图等统计过程控制图，含上下控制限、中心线、异常点标记 |
| **核心交互** | 均值-极差双线图、控制限高亮、异常点自动标记（超出 UCL/LCL 红点）、规则违反指示（7 点同侧等） |
| **设计文档** | `docs/design/quality/spc-analysis.md` |
| **Flux 状态** | ❌ Chart 支持基础 bar/line/pie/scatter/area，但 SPC 需要：上下控制限区域填充（UCL/LCL 折线+区域阴影）、规则违反标记、中心线标注。这些是 chart 的增强模式，不是独立渲染器 |
| **建议优先级** | **P3** — 质量域专项 |
| **工作量** | 低 — Chart 扩展，增加控制限配置 + 规则标记 |

### 2.7 公式编辑器（Formula / Expression Input）

| 属性 | 值 |
|------|------|
| **影响域** | HR 薪酬 (`module-hr`)、财务 (`module-finance`) |
| **场景** | 薪酬项目计算公式配置（IF/SUM/AVG/ROUND 等）、凭证模板金额占位符映射 |
| **核心交互** | 表达式输入+语法高亮、字段下拉选取插入、即时计算结果预览 |
| **设计文档** | `docs/design/human-resource/payroll-simulation.md` |
| **Flux 状态** | ❌ `type: "formula-input"` 不存在。ConditionBuilder 是条件构建器，不是公式编辑器 |
| **建议优先级** | **P4** — 低频配置功能 |
| **工作量** | 中 — 表单控件子类型 |

### 2.8 工单进度仪表板（Work Order Progress Dashboard）

| 属性 | 值 |
|------|------|
| **影响域** | 制造 (`module-manufacturing`) |
| **场景** | 工单 4 阶段进度条（计划→领料→报工→完工）、工时 vs 标准颜色高亮、物料移动/JobCard 内嵌列表 |
| **核心交互** | 进度条分阶段着色、超时红色高亮、内嵌 CRUD 列表 |
| **设计文档** | `docs/design/manufacturing/ui-patterns.md §3` |
| **Flux 状态** | ❌ 无专用的工单进度条渲染器。但可以通过 Card + Steps + CRUD 组合实现 |
| **建议优先级** | **P3** — 组合组件，非独立渲染器 |
| **工作量** | 低 — 组合现有组件 |

---

## 3. 修正后的优先级矩阵

| 控件 | 修正后优先级 | 影响域数 | Flux 已有? | 工作量 | 核心程度 |
|------|------------|---------|-----------|-------|---------|
| 甘特图 Gantt | **P1** | 3 | ❌ 需新建 | 高 | ERP 核心差异化 |
| 看板 Kanban | **P2** | 4 | ❌ 需新建 | 高 | 非标准视图 |
| 排班日历 Calendar | **P2** | 3 | ❌ 需新建 | 高 | HR 核心 |
| 条码扫描 Barcode | **P3** | 4 | ❌ 需新建 | 中 | PDA 场景 |
| 版本对比 Diff | **P3** | 2 | ❌ 需新建 | 中 | 合同/B2B 专项 |
| SPC 控制图 | **P3** | 1 | 🔶 Chart 需增强 | 低 | 质量专项 |
| 公式编辑器 Formula | **P4** | 2 | ❌ 需新建 | 中 | 低频配置 |
| 工单进度仪表板 | **P3** | 1 | 🔶 可组合现有 | 低 | 制造专项 |

### 已确认不需要新增的控件（Flux 已覆盖）

| 控件 | 原错误归类 | Flux 实际状态 | 证据 |
|------|-----------|-------------|------|
| Tabs | 曾列为 P1 需新增 | ✅ `type: "tabs"` | `flux-guide/design-patterns/tabs.md` |
| Wizard | 曾列为 P2 | ✅ `type: "wizard"` | `flux-guide/design-patterns/wizard.md` |
| Chart | 曾列为 P2 | ✅ `type: "chart"` (bar/line/pie/scatter/area/stacked) | `flux-guide/design-patterns/chart.md` |
| Timeline | 曾列为 P3 | ✅ `type: "timeline"` (vertical/horizontal/alternate/reverse) | `flux-guide/design-patterns/steps-timeline.md` |
| Tree | 曾列为 P3 | ✅ `type: "tree"` + `type: "tree-select"` | `flux-guide/design-patterns/tree.md` |
| QrCode | 未提及 | ✅ `type: "qrcode"` | `flux-guide/design-patterns/remaining-components.md` |
| File Upload | 曾列为 P3 | ✅ `type: "input-file"` + `type: "input-image"` | `flux-guide/design-patterns/file-upload.md` |
| Steps | 未提及 | ✅ `type: "steps"` | `flux-guide/design-patterns/steps-timeline.md` |
| Card / Status | 未提及 | ✅ `type: "card"` + `type: "status"` | `flux-guide/design-patterns/content-display.md` |
| Condition Builder | 未提及 | ✅ `type: "condition-builder"` | `remaining-components.md` |

---

## 4. 对 flux-control.xlib 开发的影响（修正版）

**Phase 0**（flux-control.xlib 基线）：创建 ~60 个标签，对应现有 control.xlib 的 domain/stdDomain/stdDataType 映射，输出 Flux 格式。97.6% 的 view.xml 纯继承桩无需修改。

**Phase 1**（flux-web.xlib）：创建 GenFluxPage/GenFluxGrid/GenFluxForm 等标签。Tabs/Wizard/Steps 等 view.xml 已有概念的容器，flux-web.xlib 输出对应 Flux JSON。

**Phase 2（差异化控件）**：仅需新建 **3 个独立渲染器** — Gantt、Kanban、Calendar。这是真正的增量开发。

**Phase 3（专项增强）**：Barcode-input、Diff-view 为表单控件子类型；SPC 为 Chart 扩展。

---

## 5. 开源参考项目

已下载到 `~/sources/complex-controls/` 目录下（详见该目录 README.md）：

| 控件缺口 | 参考项目 | 目录 |
|---------|---------|------|
| 甘特图 | SVAR React Gantt v2.7.1 (MIT) | `react-gantt-svar/` |
| 甘特图 | DHTMLX Gantt CE v10.0.0 (MIT) | `dhtmlx-gantt/` |
| 看板 | react-kanban-kit (MIT) | `react-kanban-kit/` |
| 看板 | Planka v2.1.1 (MIT, 全应用) | `planka-app/` |
| 看板 | react-kanban v0.0.12 (MIT) | `react-kanban-simple/` |
| 排班日历 | Schedule-X (MIT, 框架无关) | `schedule-x-calendar/` |
| 排班日历 | react-big-calendar v1.20.0 (MIT) | `react-big-calendar/` |
| 条码扫描 | react-zxing v3.0.0 (MIT) | `react-zxing-barcode/` |
| 版本对比 | git-diff-view (MIT) | `git-diff-view/` |
| 版本对比 | react-diff-view v3.3.3 (MIT) | `react-diff-view/` |

此外 `~/sources/` 下已有的 `xyflow/`（流程图/节点编辑器，用于工艺路线）和 `amis/`（当前前端框架）也相关。

---

## 6. 参考文档

- `flux-guide/` — nop-chaos-flux 全部 31 个 design-patterns + 类型定义
- `docs/backlog/frontend-ui-roadmap.md` — F12/F13/F16
- `docs/analysis/2026-07-11-flux-integration-strategy-analysis.md` — Flux 集成策略
- `docs/design/aps/scheduling.md` — APS 甘特图
- `docs/design/human-resource/shift-scheduling.md` — HR 排班日历
- `docs/design/inventory/barcode-integration.md` — 条码/PDA
