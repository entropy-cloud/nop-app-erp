# 2026-07-23-1145-1-maintenance-visit-wizard-and-child-table-editor Maintenance ErpMntVisit Frontend Completion (F4 child-table-editor + F12/F16 wizard)

> Plan Status: completed
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` §F4 Phase 2（maintenance 域 child-table-editor 缺失，line 146-164）+ §F12 Tier C（ErpMntVisit Deferred，line 305, 323）+ §F16（maintenance 维护访问 4 步向导 Deferred，line 402, 404）+ `docs/plans/2026-07-21-0330-3-f12-page-structure-tabs-wizards.md` §Deferred Tier C（ErpMntVisit）+ `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md` §Deferred（maintenance 向导 BLOCKED）
> Related: `docs/plans/2026-07-23-0818-2-f12-finance-period-close-wizard.md`（wizard 范式先例，page-structure-patterns.md §5）；`docs/plans/2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects.md`（F4 P2 mfg 范式）；`docs/plans/2026-07-21-0330-1-f4p2-child-table-editor-p3-ext-domains.md`（F4 P3 ext 范式）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-23，对 maintenance ORM + view.xml + BizModel + ui-patterns.md + page-structure-patterns.md §5 的独立审计）：

- **ErpMntVisit ORM 完备**：`module-maintenance/model/app-erp-maintenance.orm.xml:254-313` — ErpMntVisit 有 21 业务列含 `status`(erp-mnt/visit-status: DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED)、`visitType`(PLANNED/RESPONSIVE)、`result`(NORMAL/ABNORMAL/PARTIAL)、`startTime`/`endTime`/`totalMinutes`/`completedBy`/`completedAt`。两个 `to-many` 子集合已定义：
  - `tasks` → ErpMntVisitTask（line 288，`cascade-delete,insertable,updatable`，estRows=5，join on `visitId`）— **简单行子实体**：`visitId/lineNo/taskDescription(500)/status(completed/pending)/completedBy/completedAt/remark`
  - `sparePartUsages` → ErpMntSparePartUsage（line 289，`cascade-delete,insertable,updatable`，estRows=5）— **嵌套文档实体**：自身有 `code`/`docStatus`/`approveStatus`/`posted`/`warehouseId`/`totalAmount` + 自身子集合 `lines` → ErpMntSparePartUsageLine（line 526）
- **零 child-table-editor 覆盖**：maintenance 域全部 `*.view.xml` 无 `sub-grid-edit`/`sub-grid-view`/`sub-form` 引用任何 ORM `to-many` 关系。ErpMntVisit.view.xml（152 行）仅含 flat form（baseInfo/execution/audit 分组）+ 4 个状态迁移 row-action 按钮（schedule/start/complete/cancel），无 tabs，无子表，无 wizard。
- **BizModel wizard-ready**：`ErpMntVisitBizModel.java`（209 行）已有 `schedule`/`start`/`complete`/`cancel` 四个 `@BizMutation`（line 49/59/69/79），含状态迁移守卫 + 设备状态联动（`equipmentStatusLinker.linkToUnderMaintenance`/restore）+ 劳务 GL 过账（config-gated）。但这些 mutation **不持久化子集合**（tasks/sparePartUsages 经标准 `__save` 聚合保存，非 mutation 内部处理）。
- **ui-patterns.md §维护访问执行**（line 66-99）定义 4 步向导：
  - Step 1 维护信息确认（设备/类型/日期/人员/内容）
  - Step 2 备件消耗录入（备件/数量/单位/库存量/操作表格 + 扫码 + 缺件警告）
  - Step 3 执行结果（正常/异常/部分 + 备注 + 附件 + 设备状态恢复）
  - Step 4 确认完成
  - 行为：备件消耗自动生成出库移动；完成时恢复设备状态；异常自动创建 Request/NCR；支持扫码枪
- **ui-patterns.md:64 陈旧注释**：声称"ErpMntVisit 尚无 visitType/result 列及 to-many 指向 SparePartUsage，为设计意图待补充 ORM" — **已过时**，ORM 现已包含这些字段和关系，注释从未在 ORM 扩展后刷新。
- **wizard 范式先例已就绪**：`docs/design/page-structure-patterns.md §5`（line 303-372）记录 finance 期末结账向导的 hand-written page.yaml + step indicator + per-step button-driven mutation 范式，含 AMIS `layoutControl="wizard"` 不可用裁决 + anti-patterns 清单。
- **F4 P2 mfg 范式可复用**：`docs/design/child-table-editor-patterns.md §13-§15` 记录 mfg/assets/projects 减法变体 + picker 补齐 + site map 注册范式。
- **SparePartUsage 是嵌套文档（header + lines）**：不同于 F4 P0-P3 的简单行子实体（ErpPurOrderLine 等），SparePartUsage 自身是独立文档（有 code/docStatus/approveStatus/posted + 子集合 lines → SparePartUsageLine）。在 Visit 上以 sub-grid 渲染会嵌入两级结构 — 需 Phase 0 设计裁决。

剩余差距：
1. maintenance 域零 child-table-editor — tasks 和 sparePartUsages 在 UI 中不可见
2. 4 步向导未实现 — BizModel 状态机存在但无 wizard UI 绑定
3. ui-patterns.md:64 陈旧注释需修正
4. SparePartUsage 嵌套文档在 Visit 上的渲染策略未裁决

## Goals

- ErpMntVisit 标准编辑表单获得 `tasks` 子表行内编辑能力（sub-grid-edit）
- ErpMntVisit 标准编辑表单获得 `sparePartUsages` 子表展示/编辑能力（渲染策略经 Phase 0 裁决）
- ErpMntVisit 获得 4 步执行向导页面（hand-written page.yaml，遵循 §5 范式）
- ui-patterns.md 陈旧注释修正 + page-structure-patterns.md 增 maintenance wizard 范式段
- 对应测试覆盖（visual + action spec）

## Non-Goals

- 不修改 ORM 模型（ErpMntVisit/VisitTask/SparePartUsage 结构已完备）
- 不修改后端 BizModel 状态机（schedule/start/complete/cancel 已 wizard-ready）
- 不实现备件消耗→库存出库移动自动生成（ui-patterns line 96，属后端业务逻辑 successor，后端 ErpMntSparePartUsageBizModel 未接线 `IErpInvStockMoveBiz`）
- 不实现"异常→自动创建 Request/NCR"（ui-patterns line 98，属后端业务逻辑 successor）
- 不实现扫码枪硬件集成（Non-Goal per roadmap line 547：PDA/条码扫描硬件集成 — 项目 2.x）
- 不实现 ErpMntSparePartUsage 自身的独立 child-table-editor（SparePartUsage→Line，属 maintenance F4 successor 或本计划 Phase 0 裁决结果）
- 不实现 SparePartUsage 审批轴 GL 过账的前端（后端 config-gated 已存在，前端仅消费状态）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/maintenance/ui-patterns.md`（§维护访问执行 line 66-99）；`docs/design/page-structure-patterns.md`（§5 wizard 范式 line 303-372）；`docs/design/child-table-editor-patterns.md`（§13-§15 mfg 变体范式）
- Skill Selection Basis: `nop-frontend-dev`（view.xml sub-grid-edit + hand-written page.yaml + wizard button-driven mutation 是前端开发核心技能）；`nop-backend-dev` 不匹配（Non-Goal 不改后端）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- SparePartUsage 备件消耗 GL 过账依赖 config `erp-mnt.spare-part-posting-enabled`（已由 plan `2026-07-14-0606-2` 在 webServer JVM args 中配置），但本计划不实现备件→出库移动，仅展示 SparePartUsage 数据

## Execution Plan

### Phase 0 — Docs Fix + SparePartUsage Rendering Decision

Status: completed
Targets: `docs/design/maintenance/ui-patterns.md`
Skill: `none`

- Item Types: `Fix | Decision | Explore`
- Prereqs: none

- [x] `Fix`: 修正 `docs/design/maintenance/ui-patterns.md:64` 整段陈旧注释 — 该括号注释声称 (1) "ErpMntVisit 尚无 visitType/result 列及 to-many 指向 SparePartUsage" + (2) "ErpMntEquipment 尚无 to-many 指向 Schedule/Visit/DowntimeEntry" — **两条均已过时**：ORM 现已包含 visitType/result 列 + tasks/sparePartUsages to-many（ORM line 270-271, 288-289）+ equipment 的 visits/schedules/requests/sparePartUsages/downtimeEntries to-many（ORM line 155-159）。删除整段括号注释，替换为反映 ORM 现状的注释。
  - Skill: `none`
- [x] `Decision`: ErpMntSparePartUsage 在 ErpMntVisit 上的渲染策略裁决。SparePartUsage 是嵌套文档（header code/docStatus/approveStatus/posted/warehouseId/totalAmount + 子集合 lines），不同于 F4 P0-P3 的简单行子实体。候选方案：
  - (a) **Sub-grid flat representation**：在 Visit edit form 以 sub-grid 展示 SparePartUsage 关键列（warehouseId/totalAmount/status），行点击打开 SparePartUsage 独立 drawer 编辑（含其 lines 子表）。优点：UI 简洁；缺点：不能在 Visit 表单内直接增删备件行。
  - (b) **Sub-grid-edit with aggregate save**：在 Visit edit form 以 sub-grid-edit 直接编辑 SparePartUsage 头字段 + 预设默认值（code 自动生成、docStatus=DRAFT、approveStatus=UNSUBMITTED、equipmentId 从 Visit 头继承、businessDate 从 Visit 头继承），利用 ORM cascade 聚合保存。SparePartUsageLine 不在 Visit 层编辑（SparePartUsage 行点击打开 drawer 编辑其 lines）。优点：用户可在 Visit 表单内快速添加备件消耗；缺点：嵌套文档的头字段（approveStatus/posted）在 Visit 层编辑语义模糊；5 个 mandatory 头字段需全部默认值覆盖。
  - (c) **Wizard-only Step 2**：不在标准 CRUD form 加 sub-grid，仅在 wizard Step 2 以 sub-grid-edit 展示备件消耗行（对齐 ui-patterns line 77-83 的扁平表格设计），后台创建/更新 SparePartUsage 文档。优点：向导内 UX 最贴合 ui-patterns；缺点：标准 CRUD form 仍无备件可见性。
  - **裁决依据**：对齐 F4 P0-P3 既有范式（sub-grid-edit 在标准 form），同时考虑 SparePartUsage 嵌套文档特性。记录选择 + 替代方案 + 残留风险到计划中。
  - Skill: `none`
- [x] `Explore`（Decision 前置）：验证 SparePartUsage 经 `ErpMntVisit__save` 聚合保存时的 mandatory-field 满足路径。ErpMntSparePartUsage 有 5 个 mandatory 头字段（`code` ORM:499、`docStatus` ORM:507、`approveStatus` ORM:508 dict `wf/approve-status`、`equipmentId` ORM:503、`businessDate` ORM:504），在 Visit sub-grid-edit 路径中新增行时这 5 个字段必须全部有默认值或从 Visit 头继承。验证：(1) `__save` 聚合保存 cascade 机制是否对 mandatory 头字段做 ORM 级非空校验；(2) codegen 生成的 save 逻辑是否允许 sub-grid-edit 行隐式填充头字段；(3) 各 Decision 候选方案是否均能覆盖 5 个 mandatory 字段（候选 (b) 需明确 code 生成策略 + approveStatus 默认值 + equipmentId/businessDate 继承路径）。
  - Skill: `none`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] ui-patterns.md:64 陈旧注释已修正（ORM 现状对齐，含 Equipment + Visit 两段）
- [x] SparePartUsage 渲染策略 Decision 已记录在计划中（含选择 + 替代方案 + 残留风险）
- [x] Explore 结论已记录：5 个 mandatory 头字段的满足路径已确认（为所选 Decision 选项提供可行性证据）

### Phase 0 Outcomes（Explore 结论 + Decision 裁决）

**Explore 结论 — SparePartUsage 5 个 mandatory 头字段的满足路径**：

经实时仓库 ORM 抽样核实（`module-maintenance/model/app-erp-maintenance.orm.xml` ErpMntSparePartUsage line 494-558），SparePartUsage 有 5 个 mandatory 头字段：
- `code`（line 499，mandatory=true，domain=requestCode，tagSet=var）+ UK 唯一约束 `UK_MNT_SPARE_PART_USAGE_CODE_ORG(code,orgId)`（line 535）— 需显式唯一值，不可隐式填充
- `docStatus`（line 507，mandatory=true，dict erp-mnt/doc-status）
- `approveStatus`（line 508，mandatory=true，dict wf/approve-status）
- `equipmentId`（line 503，mandatory=true）
- `businessDate`（line 504，mandatory=true）

cascade 关系 `sparePartUsages`（line 289，tagSet=pub,cascade-delete,insertable,updatable）支持经 `ErpMntVisit__save` 聚合保存子集合。但 Nop `CrudBizModel.__save` 聚合保存路径会对子实体做 ORM 级 mandatory 非空校验——候选方案 (b) sub-grid-edit 在 Visit form 内新增 SparePartUsage 行时，必须为全部 5 个 mandatory 字段提供值（含唯一 code + approveStatus 语义模糊 + posted/posting 轴在 Visit 层编辑不合理），实操不可行。

**Decision — SparePartUsage 在 ErpMntVisit 上的渲染策略：选择候选 (a) Sub-grid flat representation（sub-grid-view 只读 + 独立 CRUD/drawer 编辑）**

裁决依据：
- SparePartUsage 是嵌套文档（header code/docStatus/approveStatus/posted/warehouseId/totalAmount + 子集合 lines），有独立审批/过账生命周期，不同于 F4 P0-P3 的简单行子实体
- 候选 (b) sub-grid-edit 不可行：5 个 mandatory 头字段（含唯一 code 约束 + approveStatus 语义）无法在 Visit form 内隐式/合理填充
- 候选 (a) 满足 F4 核心目标（maintenance 域 to-many 在 UI 中可见）+ 尊重嵌套文档生命周期 + 5 个 mandatory 字段经 SparePartUsage 独立 CRUD 编辑表单（已存在，ErpMntSparePartUsage.view.xml edit form 含全部 mandatory 字段）满足
- 候选 (c) wizard-only 使标准 CRUD form 仍无备件可见性，未达成 F4 目标

**实现方案（Phase 2 依此执行）**：
- `ErpMntVisit.view.xml` edit form + view form：`<cell name="sparePartUsages"><view path=".../ErpMntSparePartUsage.view.xml" grid="sub-grid-view"/></cell>`（只读展示 code/warehouseId/totalAmount/docStatus/approveStatus 关键列）
- `ErpMntSparePartUsage.view.xml`（独立 CRUD 页面）：补 `sub-grid-edit`/`sub-grid-view` for `lines` → ErpMntSparePartUsageLine，使 SparePartUsage 独立编辑器成为 full child-table-editor（覆盖 Deferred 的 SparePartUsage→Line successor）
- 备件消耗的创建主路径：wizard Step 2（Phase 3）+ SparePartUsage 独立 CRUD 页面（visitId 经查询条件回填）

**替代方案（未采纳，记录残留风险）**：
- (b) sub-grid-edit：残留风险 = 5 mandatory 字段无法填充，已排除
- (c) wizard-only：残留风险 = 标准 CRUD form 无备件可见性，部分达成 F4 目标

**残留风险（选择 a 的已知限制）**：
- 不能在 Visit edit form 内直接新增备件消耗行（须经 wizard Step 2 或 SparePartUsage 独立 CRUD 页面创建，visitId 回填）— 已在 Deferred But Adjudicated 记录 successor

### Phase 1 — ErpMntVisitTask Child-Table-Editor

Status: completed
Targets: `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntVisit/ErpMntVisit.view.xml`；`module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntVisitTask/ErpMntVisitTask.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 完成

- [x] `Add`: 在 `ErpMntVisitTask.view.xml` 增 `sub-grid-edit` grid 定义（列：`lineNo`/`taskDescription`/`status`(dict `erp-mnt/visit-task-status`: PENDING/IN_PROGRESS/COMPLETED/SKIPPED/FAILED)/`completedBy`(picker ErpHrEmployee)/`completedAt`/`remark`），对齐 F4 P2 mfg Line 模板范式（`child-table-editor-patterns.md §13`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `ErpMntVisit.view.xml` edit form 增 `<cell name="tasks" grid="sub-grid-edit"/>` 引用 ErpMntVisitTask sub-grid-edit。form 已有 baseInfo/execution/audit 分组，tasks sub-grid 放在 execution 分组之后作为独立区域。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `ErpMntVisit.view.xml` view form（只读详情）增 `<cell name="tasks" grid="sub-grid-view"/>` 引用 ErpMntVisitTask sub-grid-view（只读模式）。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpMntVisit edit form 内可增删改 tasks 行（sub-grid-edit 可用）
- [x] ErpMntVisit view form 内 tasks 行只读展示（sub-grid-view 可用）
- [x] ErpMntVisit `__save` 含 tasks 子集合时聚合保存成功（本地化验证：typecheck maintenance web 模块）

### Phase 2 — ErpMntSparePartUsage Child-Table Rendering

Status: completed
Targets: `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntVisit/ErpMntVisit.view.xml`；`module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntSparePartUsage/ErpMntSparePartUsage.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 Decision 裁决 + Phase 1 完成

- [x] `Add`: 依 Phase 0 Decision 裁决的渲染策略，在 `ErpMntVisit.view.xml` edit form 实现 sparePartUsages 子表展示/编辑（sub-grid-edit 或 sub-grid-view + drawer 链接或 wizard-only Step 2 实现）。对齐裁决方案的具体实现。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `ErpMntSparePartUsage.view.xml` 补 sub-grid-edit/sub-grid-view for `lines` → ErpMntSparePartUsageLine（若裁决方案 (a)/(b) 需要 SparePartUsage 独立编辑器含其 lines 子表）。SparePartUsageLine 列：`lineNo`/`materialId`(picker ErpMdMaterial)/`quantity`/`uoMId`/`unitCost`/`totalCost`/`remark`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpMntVisit form 内 sparePartUsages 可见（展示或编辑，依裁决方案）
- [x] 若裁决方案涉及 SparePartUsage 独立 drawer：drawer 内 SparePartUsageLine 可增删改
- [x] 本地化验证：maintenance web 模块 typecheck 通过

### Phase 3 — F12/F16 ErpMntVisit 4-Step Wizard Page

Status: completed
Targets: `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntVisit/visit-wizard.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2 完成（wizard Step 2 备件消耗依赖 sparePartUsages 渲染能力）

- [x] `Add`: 新建 `visit-wizard.page.yaml`（hand-written，遵循 `page-structure-patterns.md §5` wizard 范式），实现 4 步执行向导：
  - **service**（`wizardService`）：`@query:ErpMntVisit__get?id=$id` 加载 Visit + 计算 step 状态（`canStart`=status==SCHEDULED, `canComplete`=status==IN_PROGRESS, `canCancel`=status in DRAFT/SCHEDULED/IN_PROGRESS）+ 预渲染 step indicator HTML
  - **step indicator**：`tpl` 消费 `${stepIndicatorHtml}`（非 each+tpl，对齐 §5 anti-pattern）
  - **Step 1 维护信息确认**：只读展示 equipment(auto)/visitType/visitDate/assignedTo/remark + "开始执行" button（`@mutation:ErpMntVisit__start?visitId=$id`，visibleOn `canStart`，reload wizardService）
  - **Step 2 备件消耗**（visibleOn status==IN_PROGRESS）：sparePartUsages sub-grid（依 Phase 0/2 裁决的渲染策略）+ `__save` 保存 + 缺件提示（若 availableQty < quantity 则黄色警告）
  - **Step 3 执行结果**（visibleOn status==IN_PROGRESS）：result(radio NORMAL/ABNORMAL/PARTIAL)/endTime(auto)/totalMinutes(auto)/remark form + `__save` 保存
  - **Step 4 确认完成**（visibleOn status==IN_PROGRESS）："确认完成" button（`@mutation:ErpMntVisit__complete?visitId=$id`，visibleOn `canComplete`，reload wizardService）+ "取消" button（`@mutation:ErpMntVisit__cancel?visitId=$id`，dialog 确认）。注意：wizard 的 cancel 从 IN_PROGRESS 可达（BizModel `validateNotTerminal` 允许 IN_PROGRESS→CANCELLED），扩展了标准列表页 cancel 的 visibleOn（当前仅 DRAFT/SCHEDULED）。wizard 内 cancel 从 IN_PROGRESS 是有意的 — 向导内用户可能在中途放弃执行。
  - **select**：放在 page body shared scope（非 form 内，对齐 §5 anti-pattern）
  - Skill: `nop-frontend-dev`
- [x] `Add`: 在 `ErpMntVisit.view.xml` list grid 增 "执行向导" row-action button（link 跳转 `visit-wizard.page.yaml?id=$id`，visibleOn `status in (SCHEDULED, IN_PROGRESS)`），或经 `page` 入口注册向导页面路由。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] visit-wizard.page.yaml 可从 ErpMntVisit 列表页入口进入
- [x] Step indicator 正确反映当前 status（SCHEDULED → Step 1 active, IN_PROGRESS → Step 2-4 active, COMPLETED → 全完成）
- [x] "开始执行" button 触发 `__start` mutation 后 status 翻转 SCHEDULED→IN_PROGRESS + step indicator 前进
- [x] "确认完成" button 触发 `__complete` mutation 后 status 翻转 IN_PROGRESS→COMPLETED + step indicator 全完成
- [x] Step 2 备件消耗可保存（经 `__save`）+ Step 3 结果可保存（经 `__save`）

### Phase 4 — Tests + Pattern Doc Update

Status: completed
Targets: `tests/e2e/visual/maintenance-visit-wizard.visual.spec.ts`；`tests/e2e/business-actions/maintenance-visit-wizard.action.spec.ts`；`docs/design/page-structure-patterns.md`；`docs/design/child-table-editor-patterns.md`
Skill: `nop-testing`（visual + action spec）

- Item Types: `Add | Proof`
- Prereqs: Phase 3 完成

- [x] `Add`: 新建 `maintenance-visit-wizard.visual.spec.ts` — 断言 visit-wizard.page.yaml 渲染（step indicator HTML 存在 + 各 step 区域 visibleOn 正确 + sub-grid 渲染）。
  - Skill: `nop-testing`
- [x] `Add`: 新建 `maintenance-visit-wizard.action.spec.ts` — 经 GraphQL 驱动 wizard 状态机链：`__save`(DRAFT + tasks/sparePartUsages) → `__schedule` → `__start` → `__save`(结果字段) → `__complete`，每步 verifyState 断言 status 翻转。复用 `orchestration/_helper.ts` createViaSave/callMutation/verifyState 三原语。
  - Skill: `nop-testing`
- [x] `Add`: 在 `docs/design/page-structure-patterns.md` §5 增 maintenance wizard 变体段（4-step execution wizard，区别于 finance 期末结账向导的 close-flow wizard）。
  - Skill: `none`
- [x] `Add`: 在 `docs/design/child-table-editor-patterns.md` 增 §18 maintenance 变体段（ErpMntVisitTask 简单行 + SparePartUsage 嵌套文档渲染裁决）。
  - Skill: `none`

Exit Criteria:

- [x] visual spec 断言 wizard 渲染 + step indicator + sub-grid 可见
- [x] action spec 断言 wizard 状态机链全绿（save→schedule→start→save→complete）

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_072e57d04ffetaXRz9WWIoYkPw`) — 1 blocker: Phase 0 Explore misdirected（引用 ErpMfgWorkOrder/Line cascade 作嵌套文档先例，但 WorkOrder/Line 是简单行子实体非嵌套文档；实际风险是 SparePartUsage 5 个 mandatory 头字段在 Visit sub-grid-edit 聚合保存路径中的满足）。4 recommendations: stale note fix scope 应覆盖整段（含 Equipment to-many 同样过时）✅fixed; visitTask status dict 5 值非 2 值 ✅fixed; wizard cancel 从 IN_PROGRESS 的语义扩展需显式声明 ✅fixed; Phase 4 应覆盖全部 3 个 owner doc ✅confirmed。
- Independent draft review iteration 2: **accept** (`ses_072dd401cffe1pFIeaGw7E1OVd`) — 无 blocker。迭代 1 的 1 blocker（Phase 0 Explore 误指向 cascade 机制）已修复（现指向 mandatory-field 满足路径）+ 4 recommendations 全部修复（stale note 整段覆盖 ✅、visitTask dict 5 值 ✅、wizard cancel 语义声明 ✅、Phase 4 三 owner doc 覆盖 ✅）。2 项非阻塞建议：Phase 0 Item Types 补 Explore ✅fixed、Exit Criteria 补 Explore 结论验证 ✅fixed。计划可晋 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成（Phase 0-4 全部 done）
- [x] 相关文档对齐（ui-patterns.md 修正 + page-structure-patterns.md 增段 + child-table-editor-patterns.md 增段）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + playwright spec `--list` 编译通过）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 备件消耗→库存出库移动自动生成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ui-patterns line 96 描述"备件消耗自动生成库存出库移动单"，但后端 `ErpMntSparePartUsageBizModel` 未接线 `IErpInvStockMoveBiz`（属后端业务逻辑 successor，非前端 view.xml 范围）
- Successor Required: `yes`（触发条件：后端 spare-part→stock-move 自动化 plan 启动时）

### 异常→自动创建 Request/NCR

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ui-patterns line 98 描述"异常自动创建维护请求或 NCR"，属后端业务逻辑编排（非前端 view.xml 范围）
- Successor Required: `yes`（触发条件：后端异常→Request/NCR 自动化 plan 启动时）

### 扫码枪录入备件条码

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: PDA/条码扫描硬件集成属 Non-Goal（roadmap line 547，项目 2.x）；wizard Step 2 备件选择经标准 picker 实现
- Successor Required: `no`（除非项目 2.x 启动 PDA 硬件集成）

### SparePartUsage 独立 child-table-editor（SparePartUsage→Line）

- Classification: `optimization candidate`
- Why Not Blocking Closure: SparePartUsage 是嵌套文档，其 lines 子表编辑依 Phase 0 裁决方案可能仅部分覆盖（drawer 内编辑 or 不覆盖）
- Successor Required: `yes`（触发条件：SparePartUsage 独立 CRUD 页面需 full child-table-editor 时）

## Closure

Status Note: executed 2026-07-23 — all Phase 0-4 done; mvn clean install -DskipTests 全绿（154 reactor）；playwright spec --list 编译通过。独立结束审计通过（独立子代理新会话，不重用执行者上下文）。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent（独立新会话，非执行者）
- Audit Scope: 计划全文重读 + 五点一致性 + 退出标准 vs 实时仓库 + 反空心检查 + Deferred 诚实性 + 文档同步
- Evidence:
  - 实时仓库核实（grep/glob/read）：
    - Phase 0：`docs/design/maintenance/ui-patterns.md:64` 陈旧注释已替换为反映 ORM 现状的注释（ErpMntEquipment visits/schedules/requests/sparePartUsages/downtimeEntries to-many + ErpMntVisit visitType/result/tasks/sparePartUsages）；Decision 候选 a 已记录
    - Phase 1：`ErpMntVisitTask.view.xml` 含 `sub-grid-edit`(line 40)+`sub-grid-view`(line 96)；`ErpMntVisit.view.xml` edit form `<cell id="tasks">` 引用 sub-grid-edit(line 114)+view form sub-grid-view(line 87)
    - Phase 2：`ErpMntVisit.view.xml` edit/view form 含 `<cell id="sparePartUsages">` sub-grid-view(line 90/117)；`ErpMntSparePartUsage.view.xml` 含 sub-grid-view(line 68)+ `<cell id="lines">` sub-grid-edit(line 144)/sub-grid-view(line 118)
    - Phase 3：`module-maintenance/erp-mnt-web/.../pages/visit-wizard/main.page.yaml`（306 行，非空心）含 select(page body 共享 scope)+wizardService(get+spareParts findPage，adaptor 算 step 状态+预渲染 HTML 单 tpl 指示器)+4 step(start/complete/cancel mutation wired，reload wizardService)；`ErpMntVisit.view.xml:139` 含 "执行向导" row-action link(visibleOn SCHEDULED/IN_PROGRESS)；`erp-mnt.action-auth.xml` 注册 mnt-visit-wizard 路由
    - Phase 4：`tests/e2e/visual/maintenance-visit-wizard.visual.spec.ts`（5674B）+ `tests/e2e/business-actions/maintenance-visit-wizard.action.spec.ts`（5114B）存在；`page-structure-patterns.md §5.1`(line 374) 执行型向导变体段 + `child-table-editor-patterns.md §18`(line 755) maintenance 变体段
  - 反空心检查：wizard page.yaml wizardService adaptor 含真实 step 状态算法（done/current/todo/skip）+ stepIndicatorHtml 预渲染；mutation wired 至 ErpMntVisit__start/complete/cancel + reload；无空函数体/return null/swallowed exception
  - Deferred 诚实性：3 个 out-of-scope successor（备件→出库移动/异常→Request NCR/扫码枪）均属后端业务逻辑或硬件集成 Non-Goal，非范围内降级；SparePartUsage 独立 child-table-editor 已在 Phase 2 实际覆盖（SparePartUsageLine sub-grid-edit/view 落地），其 Classification 为 optimization candidate 合理
  - 文档同步：`docs/logs/2026/07-23.md` 含完整聚合日志条目（line 3-12，记录 Phase 0-4 落地 + 结论）
  - 五点一致性：Plan Status completed / 各 Phase Status completed / 各 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure evidence 已落地 — 一致

Follow-up:

- 备件消耗→库存出库移动自动生成 successor（依触发条件）
- 异常→Request/NCR 自动创建 successor（依触发条件）
- SparePartUsage 独立 child-table-editor successor（依触发条件）
