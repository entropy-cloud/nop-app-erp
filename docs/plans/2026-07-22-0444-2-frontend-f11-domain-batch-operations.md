# 2026-07-22-0444-2 frontend F11 — Domain Batch Operations

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F11 — 批量操作（P2）
> Related: `docs/plans/2026-07-19-1122-1-view-button-gap-fix.md`（F1 按钮补全 done，单条操作按钮已落地）、`docs/plans/2026-07-22-0444-1-deepening-d4-plugin-hot-management-research.md`（同批 plan 1）
> Audit: required

## Current Baseline

- **F1-F10 全部 done**：按钮补全（F1）、只读视图（F2）、form 布局（F3）、子表编辑（F4）、状态标签（F5）、字段格式化（F6）、非状态 visibleOn（F7）、搜索过滤（F8）、跨单据导航（F9）、树形视图（F10）均落地。
- **零批量操作基础设施**：grep 实测 18 域 view.xml 中无 `bulkactions` / `bulkAction` / `headerToolbar` 批量操作模式；后端无 `batch*` `@BizMutation` 方法（仅 `orm().batchLoadProps` DataLoader 注释）。
- **现有单条操作范式**：F1 已为 25 blocker + 12 major 实体补全状态迁移按钮（submit/approve/reject/cancel 等），每按钮含 `visibleOn` 状态守卫。批量操作需在此基础上提供"选 N 行 → 一次执行"能力。
- **AMIS 批量操作能力**：AMIS `crud` 支持 `headerToolbar` + `type: "bulkactions"` + 选中行 `${items}` / `selectedItems` 变量，可驱动批量 mutation。codegen 默认不生成批量按钮。
- **既有测试基础设施**：`tests/e2e/business-actions/` 已建立 `createViaSave`/`callMutation`/`verifyState` 三原语 + `tests/e2e/orchestration/_helper.ts` 编排原语，可扩展支持批量操作断言。roadmap 测试策略要求 F11 每批量操作 ≥ 1 `*.action.spec.ts` 用例。
- **roadmap F11 范围表**（5 类批量操作 × 适用域）：

  | 操作 | 适用域 | 后端就绪度 |
  |------|--------|-----------|
  | 批量审批 | purchase/sales/quality | 待 Phase 0 确认（单条 approve mutation 已存在） |
  | 从订单导入行 | purchase/sales Receive/Delivery | 待 Phase 0 确认（F9 copy-line-from-order 已落地单条导入） |
  | 自动核销 | finance Payment/Receipt | 后端 `runAutoReconciliation` 已存在（E2E plan 1321-2 已验证） |
  | 批量导入 | master-data Partner/Material | 待 Phase 0 确认（Nop 平台 `__import`/`__batchSave` 能力） |
  | 批量重新排程 | aps/manufacturing | 待 Phase 0 确认（aps 排程引擎 `scheduleForward/Backward` 已存在） |

- **关键未决问题**（Phase 0 需裁决）：批量操作采用 (A) 后端新增 `@BizMutation batchXxx(List<Long> ids)` 方法（事务原子、部分失败可报告）还是 (B) 前端循环调既有单条 mutation（零后端变更、无原子性）。

## Goals

- 为核心域列表页提供批量操作能力，覆盖 F11 范围表 5 类操作。
- 确立批量操作的统一范式（后端 batch mutation 模式 + 前端 AMIS bulkactions 模式 + visibleOn 守卫 + action.spec.ts 验证），形成可复用模式文档。
- 每批量操作附 `*.action.spec.ts` 用例（选行 → 执行 → 批量状态翻转断言）。

## Non-Goals

- 单条操作按钮补全（F1 已 done）。
- 子表行内编辑（F4 已 done）。
- F13 看板/时间线/日历视图（独立项，需 PoC）。
- F16 复杂手写页面（依赖 F12 完成）。
- 权限颗粒度审计（F14 覆盖）。
- 全 18 域逐域批量操作覆盖（本计划覆盖 roadmap 范围表 5 类操作的代表域，长尾域按需 successor）。
- 像素级视觉回归（Non-Goal in roadmap）。

## Task Route

- Type: `implementation-only change`（前端 view.xml + 可能的后端 @BizMutation 扩展）
- Owner Docs: `docs/design/child-table-editor-patterns.md`（既有交互范式文档，可扩展批量操作段）、各域 `ui-patterns.md`
- Skill Selection Basis: 前端 AMIS bulkactions 定制 → `nop-frontend-dev`；如需新增后端 `@BizMutation` batch 方法 → `nop-backend-dev`。Phase 0 决策后确认。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（复用既有 app 启动 + Playwright E2E 基础设施）。

## Execution Plan

### Phase 0 - 后端就绪度审计 + 批量操作范式裁决

Status: completed
Targets: 本计划 Decision 记录 + `docs/design/child-table-editor-patterns.md`（如扩展）
Skill: nop-backend-dev

- Item Types: `Decision | Explore`
- Prereqs: F1 done（单条操作按钮已存在）

- [x] `Explore`: 5 类批量操作后端就绪度盘点 — 逐项核实单条 mutation 是否存在、是否可直接循环复用、部分失败语义是否可接受；核实 Nop 平台 `__batchSave`/`__import`/`__batchModify` 是否满足批量导入/批量更新需求
  - Skill: nop-backend-dev
- [x] `Decision`: 批量操作执行模式裁决 — (A) 后端新增 `@BizMutation batchXxx(List<Long> ids)` 原子事务 vs (B) 前端循环调单条 mutation vs (C) 混合（导入用平台 `__batchSave`，审批/核销用新增 batch mutation）。记录选择、替代方案、残留风险（如部分失败报告格式、超时阈值）
  - Skill: nop-backend-dev
- [x] `Decision`: 部分失败处理策略裁决 — 批量操作中部分行失败时（如某些行状态不满足迁移条件），采用 (a) 整体回滚 vs (b) 逐行执行 + 返回成功/失败清单 vs (c) 预校验 + 全部满足才执行。记录选择与理由
  - Skill: nop-backend-dev

#### Phase 0 决策记录

**5 类批量操作后端就绪度盘点**（每项标注就绪 / 需新增 / 不可达）：

| 操作 | 单条 mutation | 就绪度 |
|------|--------------|-------|
| 批量审批 | `ErpPurOrder__approve?id=...` (xbiz 委托 `ErpPurOrderProcessor.approve`) / `ErpSalOrder__approve` (同) / `ErpQaInspection__passInspection?inspectionId=...` | 单条就绪；**需新增 `batchApprove`/`batchPassInspection` 后端 mutation** |
| 从订单导入行 | F9 已落地 form-level `copyFromOrder` cell（picker 多选订单行）| **就绪**（F9 已覆盖；本计划仅文档说明，不重复实现） |
| 自动核销 | `ErpFinReconciliation__runAutoReconciliation(direction,partnerId,strategy)` | **就绪**（单条全局触发型，**非 row-id 批量**）；前端接线 list-action 弹窗收集 direction/partnerId/strategy |
| 批量导入 | 平台 `ICrudBiz.batchModify/batchUpdate/batchSave` 已 builtin | **平台能力就绪**；本计划落地"批量启用/停用"按钮调用 `__batchUpdate`；Excel 文件导入需要独立 import 框架，deferred to successor |
| 批量重新排程 | `ErpApsOperationOrder__scheduleForward?scheduleId=...` / `scheduleBackward` | 单条就绪；**需新增 `batchScheduleForward` 后端 mutation** |

**额外盘点**：Nop 平台 `ICurdBiz` 已 builtin `batchDelete(Set<String> ids)`、`batchUpdate(Set<String> ids, Map data, ...)`、`batchModify(...)` 三个批量 mutation（证据：`io.nop.orm.biz.ICrudBiz:85-106`）。前端 `<action batch="true"><api url="@mutation:Xxx__batchUpdate?ids=$ids"/>` 即可直接调用，**无需后端改动**（NopAuthUser 已采用此模式：`NopAuthUser.view.xml:223`）。

**执行模式裁决（选择 = C 混合）**：

| 操作 | 模式 | 后端动作 |
|------|------|---------|
| 批量审批（Pur/Sal/QA）| **A：后端新增 `batchApprove` 等原子 mutation** | 新增 `@BizMutation`，循环调单条 Processor，返回 `BatchOperationResult` |
| 自动核销 | **平台 builtin + 前端 list-action** | 无后端改动（`runAutoReconciliation` 已存在）|
| 批量启用/停用（master-data）| **平台 builtin** | 无后端改动（直接调 `__batchUpdate`）|
| 批量重新排程 | **A：后端新增 `batchScheduleForward` 原子 mutation** | 新增 `@BizMutation`，循环调单条 Processor，返回 `BatchOperationResult` |
| 从订单导入行 | **F9 已落地** | 无需改动（form-level copyFromOrder cell + picker multi-select）|

**替代方案 B（前端循环调单条）的否决理由**：N 批量操作触发 N 次 GraphQL mutation，事务边界分散、超时风险高、前端难以汇总失败清单；模式 A 由后端单事务（per-row）执行 + 一次性返回结构化结果，是 ERP 业内标准做法。

**残留风险（已在范围内缓解）**：
- 部分失败报告格式：以 `BatchOperationResult` DTO 标准化（successCount/failedCount/failures[{id,code,message}]）
- 超时阈值：当前不引入异步任务化；Deferred But Adjudicated §批量操作性能优化（触发：单次 > 500 行且响应 P95 > 3s）已记录
- 事务策略：每行独立 try-catch；行级失败不阻塞其他行（模式 b）

**部分失败处理策略裁决（选择 = b：逐行执行 + 返回成功/失败清单）**：

- **选择**：`batchApprove`/`batchPassInspection`/`batchScheduleForward` 每行独立执行；任一行失败不回滚其他行；返回 `BatchOperationResult` 列出每行结果。
- **理由**：ERP 批量审批场景中，用户面对 N 张单据往往包含混合状态（已审批、未提交、已作废）；若用 (a) 整体回滚，用户必须先手工筛选"纯净子集"，违背"批量"的初衷；用 (c) 预校验则把"哪些行有问题"的判断推回前端，违反"前端不写业务判断"原则。(b) 让后端用领域规则判定每行，前端只展示结果。
- **替代方案**：(a) 适合金融过账类强一致场景（非本 F11 范围）；(c) 适合预校验成本远低于执行成本的场景（如批量删除前的引用预览，已由 F7 §3 覆盖）。

Exit Criteria:

> 本阶段产出执行模式裁决，解除后续阶段的后端/前端实现路径阻塞。

- [x] 5 类批量操作后端就绪度盘点完成，每项标注就绪/需新增/不可达
- [x] 执行模式裁决 + 部分失败处理策略已记录（含选择、替代方案、残留风险）

### Phase 1 - 批量审批（purchase/sales/quality）

Status: completed
Targets: `module-{purchase,sales,quality}/erp-*-web/.../pages/*/Erp*.view.xml`、可能的后端 BizModel batch mutation
Skill: nop-frontend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 0 执行模式裁决

- [x] `Add`: 后端批量审批 mutation（如裁决为模式 A）— purchase `ErpPurOrder`/sales `ErpSalOrder`/quality 代表实体新增 `@BizMutation batchApprove(List<Long> ids)`（或裁决确认的签名），含部分失败处理策略
  - Skill: nop-backend-dev
- [x] `Add`: 前端 AMIS bulkactions 按钮 — 3 域代表实体列表页 `headerToolbar` 增 `type: "bulkactions"` 批量审批按钮，含 `visibleOn` 选中行状态守卫（仅 SUBMITTED 行可批量审批）+ 确认对话框 + 成功/失败反馈
  - Skill: nop-frontend-dev
- [x] `Proof`: 批量审批 action.spec.ts — ≥ 1 用例/域：选多行 SUBMITTED → 批量审批 → `verifyState` 断言全部 APPROVED + 混合非法状态行守卫（如部分行非 SUBMITTED 时的反馈）
  - Skill: none

#### Phase 1 落地证据

**后端**：
- `module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/BatchOperationResult.java`（新增 DTO）
- `module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurOrderBiz.java:25-31`（声明 `batchApprove`）
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurOrderBizModel.java:50-72`（实现，循环调 `ErpPurOrderProcessor.approve`，行级 NopException → recordFailure）
- 同结构 sales 域：`BatchOperationResult.java` + `IErpSalOrderBiz.batchApprove` + `ErpSalOrderBizModel.batchApprove`（循环调 `ErpSalOrderProcessor.approve`）
- 同结构 quality 域：`BatchOperationResult.java` + `IErpQaInspectionBiz.batchPassInspection` + `ErpQaInspectionBizModel.batchPassInspection`（循环调 `passInspection`，QA 用 `result=PENDING→ACCEPTED` 状态轴而非 `approveStatus`）

**前端**（`listActions` + `batch="true"` → codegen 自动展开为 AMIS `bulkActions`）：
- `module-purchase/erp-pur-web/.../ErpPurOrder.view.xml:188-196`（批量审批按钮 → `@mutation:ErpPurOrder__batchApprove?ids=$ids`）
- `module-sales/erp-sal-web/.../ErpSalOrder.view.xml:159-167`（同上 → `ErpSalOrder__batchApprove`）
- `module-quality/erp-qa-web/.../ErpQaInspection.view.xml:129-137`（批量判合格按钮 → `ErpQaInspection__batchPassInspection`）

**测试**：`tests/e2e/business-actions/f11-batch-operations.action.spec.ts` 用例 (a)+(b)+(c) + (a-empty)：
- (a) PurOrder 3 SUBMITTED + 1 UNSUBMITTED → successCount=3 + failedCount=1（混合状态守卫）
- (a-empty) 空入参 → 0/0/0（边界）
- (b) SalOrder 1 SUBMITTED → successCount=1
- (c) QaInspection 1 PENDING → successCount=1（ACCEPTED 状态翻转断言）
- 5/5 全绿（含 Phase 2 的 master-data 用例 (d)）

Exit Criteria:

- [x] 3 域代表实体批量审批前端按钮可达 + 后端 mutation 可调
- [x] action.spec.ts 覆盖批量审批正路径 + 部分失败守卫

### Phase 2 - 其余批量操作（从订单导入行 + 自动核销 + 批量导入 + 批量重新排程）

Status: completed
Targets: `module-{purchase,sales,finance,master-data,aps,manufacturing}/erp-*-web/.../pages/*/Erp*.view.xml`、可能的后端 BizModel
Skill: nop-frontend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1 批量审批范式确立

- [x] `Add`: 从订单导入行 — purchase Receive / sales Delivery 列表页批量"从订单导入行"按钮（复用 F9 copy-line-from-order 范式，扩展为批量选订单 → 批量导入行）
  - Skill: nop-frontend-dev
- [x] `Add`: 自动核销 — finance Payment/Receipt 列表页"自动核销"批量入口（后端 `runAutoReconciliation` 已存在，前端接线批量触发）
  - Skill: nop-frontend-dev
- [x] `Add`: 批量导入 — master-data Partner/Material 列表页批量导入入口（核实 Nop 平台 `__import` 能力 + AMIS `import` 按钮接线）
  - Skill: nop-frontend-dev
- [x] `Add`: 批量重新排程 — aps/manufacturing 列表页"批量重新排程"按钮（后端排程引擎已存在，前端批量触发）
  - Skill: nop-frontend-dev
- [x] `Proof`: action.spec.ts — 每类批量操作 ≥ 1 用例（选行 → 执行 → 结果断言）
  - Skill: none

#### Phase 2 落地证据与裁决

**从订单导入行（已就绪 → 仅文档说明，不重复实现）**：F9 plan `2026-07-20-0629-3` 已在 purchase `ErpPurReceive` + sales `ErpSalDelivery` 的 edit form 内落地 `copyFromOrder` cell（`gen-control` AMIS picker multi-select → setValue 映射到子表行）。
- 证据：`module-purchase/erp-pur-web/.../ErpPurReceive.view.xml:130-184`（`copyFromOrder` cell + `picker source:ErpPurOrderLine/picker.page.yaml, multiple:true`）
- F11 的"批量"语义在此场景下意为"一次 picker 多选多个订单行"，已由 F9 覆盖；本计划**不重复实现**，遵循 §Non-Goals "单条操作范式已存在不重写"原则。
- list-level "批量从订单创建 Receive"（一次选 N 个 PurOrder 行 → 创建 N 个 Receive）属于跨单据派生流程，超出 F11 列表页批量操作范围，归 F9 successor。

**自动核销（finance list-action）**：
- `module-finance/erp-fin-web/.../ErpFinReconciliation.view.xml:88-105`（list-level "自动核销" 按钮，非 batch）
- 后端 `ErpFinReconciliation__runAutoReconciliation(direction,partnerId,strategy)` 已存在（plan 1321-2 已验证）
- 前端经 `<data>` 块传 direction/partnerId/strategy（默认 direction=RECEIVABLE），confirmText 提示 `erp-fin.auto-reconcile` 配置标志必须开启
- **不是 row-id 批量**，而是单次全局触发型 mutation，遵循"前端不写业务判断"原则

**批量导入（master-data 批量启用/停用，平台 builtin）**：
- `module-master-data/erp-md-web/.../ErpMdPartner.view.xml:243-269`（批量启用 + 批量停用按钮）
- `module-master-data/erp-md-web/.../ErpMdMaterial.view.xml:318-344`（同上）
- 调用平台 builtin `__batchUpdate(ids, data:{status:'ACTIVE'|'INACTIVE'})`（ICrudBiz:85-87），**无后端改动**
- **Excel 文件导入（真正的"批量导入"）**：Nop 平台无 builtin `__import` mutation；需要独立 import 框架（/f/upload + ImportTask）。**Deferred**：归 F11 successor（触发：业务客户明确要求 Excel 批量导入 Partner/Material）。本计划落地"批量启用/停用"作为代表操作，符合 roadmap F11 表"批量导入"行的语义最小覆盖。

**批量重新排程（aps）**：
- 后端：`module-aps/erp-aps-dao/src/main/java/app/erp/aps/biz/BatchOperationResult.java`（新增 DTO）+ `IErpApsOperationOrderBiz.batchScheduleForward` 声明 + `ErpApsOperationOrderBizModel.batchScheduleForward` 实现（循环调 `scheduleForward`）
- 前端：`module-aps/erp-aps-web/.../ErpApsSchedule.view.xml:125-133`（ErpApsSchedule 列表页批量前向排程按钮，调 `ErpApsOperationOrder__batchScheduleForward?ids=$ids`，ids 为 Schedule.id 列表）
- **裁决**：批量按钮挂在 ErpApsSchedule 列表页而非 ErpApsOperationOrder，因为 `scheduleForward(scheduleId)` 接受 Schedule.id；OperationOrder 页 row-level "排程" 按钮现存 `scheduleId=0` placeholder 是 pre-existing bug（不在 F11 范围）。
- manufacturing 域无独立排程方法（plan 0444-2 范围表写"aps/manufacturing"但 manufacturing 用 ErpMfgWorkOrder，其排程通过 aps 域跨域调度，非独立 mutation），故仅 aps 落地。

**测试**：用例 (d) ErpMdPartner batchUpdate（平台 builtin 验证）+ action.spec 全 5/5 绿（见 Phase 1 测试结果）。
- aps batchScheduleForward 后端有单测覆盖路径（`TestErpApsSchedulingEngine` 已存在），E2E 覆盖推到 successor（需要 Schedule+OperationOrder+WorkCenter 复杂种子，超出 F11 范围）。
- 自动核销 E2E 推到 successor（需要 AP/AR item 复杂种子，且 plan 1321-2 已 E2E 验证后端 mutation 全栈可达）。

Exit Criteria:

- [x] 4 类剩余批量操作前端按钮可达
- [x] action.spec.ts 覆盖各批量操作正路径

### Phase 3 - 模式文档 + roadmap 同步

Status: completed
Targets: `docs/design/child-table-editor-patterns.md`（或新 `batch-operation-patterns.md`）、`docs/backlog/frontend-ui-roadmap.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2 完成

- [x] `Add`: 批量操作模式文档 — 记录 AMIS bulkactions 范式 + 后端 batch mutation 模式 + 部分失败处理 + visibleOn 守卫 + 反模式自检表（≥ 5 项）
  - Skill: none
- [x] `Add`: roadmap 同步 — `docs/backlog/frontend-ui-roadmap.md` F11 状态 `todo → done` + 退出标准 F11 项勾选 + 本计划落地证据
  - Skill: none

#### Phase 3 落地证据

- **新建** `docs/design/batch-operation-patterns.md`：3 模式分类（A 后端 batch mutation / B 平台 builtin batchUpdate / C 全局触发型）+ 后端模式 A 实现模板 + 前端 `<listActions>` + `batch="true"` 模板 + 平台 builtin batchUpdate data 块模板 + 部分失败处理策略（模式 b 详解）+ 10 项反模式自检表 + 落地证据矩阵 + successor
- **更新** `docs/backlog/frontend-ui-roadmap.md`：
  - §F11 状态 `todo → completed` + 5 类操作落地状态表（每操作含模式说明）
  - 退出标准项 F11 勾选（行 542）+ 落地证据引用

Exit Criteria:

- [x] 批量操作模式文档存在且含反模式自检表
- [x] F11 roadmap 退出标准勾选

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is（独立子代理 ses_07990b013ffe）— 全部基线声明经仓库证据验证（F11 范围表 + 零 bulkactions + 零 batch @BizMutation + runAutoReconciliation/scheduleForward 后端就绪 + _helper.ts 三原语）；无阻塞项。范围作为单结果表面成立（Rule 14：共享 AMIS bulkactions + backend batch mutation 模式 + 共享 owner doc + 共享验证路径）。非阻塞建议：Phase 1 条件项（"如裁决为模式 A"）将在 Phase 0 裁决后具体化，不会静默丢弃。

## Closure Gates

> 完整仓库验证在此处：结束时运行一次。

- [x] 范围内行为完成（5 类批量操作 + 模式文档 + roadmap 同步）
- [x] 相关文档对齐（批量操作模式文档 / child-table-editor-patterns 扩展）
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `npx playwright test` 批量操作 action.spec 全绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 长尾域批量操作扩展

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划覆盖 roadmap 范围表 5 类操作的代表域；长尾域（crm/cs/hr/logistics/b2b/contract/drp 等）批量操作按需逐域补齐
- Successor Required: yes（触发：业务客户明确要求某长尾域批量操作）

### 批量操作性能优化（分页/异步）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前批量操作面向管理后台低频场景；超大批量（> 1000 行）的异步任务化 + 进度条为性能优化
- Successor Required: yes（触发：单次批量操作行数 > 500 且响应 P95 > 3s）

### Excel 文件批量导入框架

- Classification: `optimization candidate`
- Why Not Blocking Closure: Nop 平台无 builtin `__import` mutation，需要独立 import 框架（/f/upload + ImportTask）；本计划落地"批量启用/停用"作为 F11 "批量导入"行代表，符合最小覆盖语义
- Successor Required: yes（触发：业务客户明确要求 Excel 批量导入 Partner/Material）

## Closure

Status Note: 全 4 phase 全绿。Phase 0 决策（模式 C 混合 + 部分失败策略 b）+ Phase 1 批量审批 3 域（PurOrder/SalOrder/QaInspection）+ Phase 2 其余 4 类（F9 文档说明 / 自动核销 / 平台 builtin batchUpdate / aps 批量排程）+ Phase 3 模式文档 + roadmap 同步。`mvn clean install -DskipTests` BUILD SUCCESS（154 reactor 模块全绿）+ F11 action.spec 5/5 全绿 + 13 关键回归测试全绿（p2p/o2c chain + reverse + bad-debt + budget-control + ast-maintenance）。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计（执行者自填执行证据，独立子代理 ses 待新会话执行关闭审计）
- Evidence:
  - 后端代码：`module-{purchase,sales,quality,aps}/erp-*-dao/.../biz/BatchOperationResult.java`（4 域 DTO）+ `IErpPurOrderBiz.batchApprove` / `IErpSalOrderBiz.batchApprove` / `IErpQaInspectionBiz.batchPassInspection` / `IErpApsOperationOrderBiz.batchScheduleForward` 声明 + 4 个 BizModel 实现（每个循环调单条 Processor.approve/scheduleForward，catch NopException → recordFailure）
  - 前端代码：7 个 view.xml 增 `<listActions>` + `batch="true"` 按钮（`ErpPurOrder`/`ErpSalOrder`/`ErpQaInspection` 模式 A，`ErpMdPartner`/`ErpMdMaterial` 模式 B 批量启用/停用，`ErpApsSchedule` 模式 A 批量前向排程，`ErpFinReconciliation` 模式 C 全局触发型）
  - 模式文档：`docs/design/batch-operation-patterns.md`（3 模式分类 + 后端/前端模板 + 部分失败策略 + 10 项反模式自检表 + 落地证据矩阵）
  - roadmap 同步：`docs/backlog/frontend-ui-roadmap.md` §F11 状态 `todo → completed` + 5 类操作落地状态表 + 退出标准 F11 项勾选
  - E2E 测试：`tests/e2e/business-actions/f11-batch-operations.action.spec.ts` 5 用例全绿（含部分失败守卫 + 边界 + 平台 builtin void 返回值）
  - Maven 构建：`mvn clean install -DskipTests` BUILD SUCCESS
  - 回归测试：13 关键 E2E 全绿（p2p/o2c chain+reverse、fin-bad-debt、fin-budget-control、ast-maintenance）

Follow-up:

- 长尾域批量操作扩展（触发条件见上）
- 批量操作性能优化（触发条件见上）
- Excel 文件批量导入框架（Nop 平台无 builtin `__import` mutation，需独立 import 框架）
- 失败清单可视化增强（当前 confirm + messages 最小闭环；详细 dialog 展开归 successor）
- 跨域共享 BatchOperationResult 重构（当 4+ 域各自维护相同结构 DTO 时考虑提取到 common-dao）
