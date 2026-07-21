# 2026-07-22-0444-2 frontend F11 — Domain Batch Operations

> Plan Status: active
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

Status: planned
Targets: 本计划 Decision 记录 + `docs/design/child-table-editor-patterns.md`（如扩展）
Skill: nop-backend-dev

- Item Types: `Decision | Explore`
- Prereqs: F1 done（单条操作按钮已存在）

- [ ] `Explore`: 5 类批量操作后端就绪度盘点 — 逐项核实单条 mutation 是否存在、是否可直接循环复用、部分失败语义是否可接受；核实 Nop 平台 `__batchSave`/`__import`/`__batchModify` 是否满足批量导入/批量更新需求
  - Skill: nop-backend-dev
- [ ] `Decision`: 批量操作执行模式裁决 — (A) 后端新增 `@BizMutation batchXxx(List<Long> ids)` 原子事务 vs (B) 前端循环调单条 mutation vs (C) 混合（导入用平台 `__batchSave`，审批/核销用新增 batch mutation）。记录选择、替代方案、残留风险（如部分失败报告格式、超时阈值）
  - Skill: nop-backend-dev
- [ ] `Decision`: 部分失败处理策略裁决 — 批量操作中部分行失败时（如某些行状态不满足迁移条件），采用 (a) 整体回滚 vs (b) 逐行执行 + 返回成功/失败清单 vs (c) 预校验 + 全部满足才执行。记录选择与理由
  - Skill: nop-backend-dev

Exit Criteria:

> 本阶段产出执行模式裁决，解除后续阶段的后端/前端实现路径阻塞。

- [ ] 5 类批量操作后端就绪度盘点完成，每项标注就绪/需新增/不可达
- [ ] 执行模式裁决 + 部分失败处理策略已记录（含选择、替代方案、残留风险）

### Phase 1 - 批量审批（purchase/sales/quality）

Status: planned
Targets: `module-{purchase,sales,quality}/erp-*-web/.../pages/*/Erp*.view.xml`、可能的后端 BizModel batch mutation
Skill: nop-frontend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 0 执行模式裁决

- [ ] `Add`: 后端批量审批 mutation（如裁决为模式 A）— purchase `ErpPurOrder`/sales `ErpSalOrder`/quality 代表实体新增 `@BizMutation batchApprove(List<Long> ids)`（或裁决确认的签名），含部分失败处理策略
  - Skill: nop-backend-dev
- [ ] `Add`: 前端 AMIS bulkactions 按钮 — 3 域代表实体列表页 `headerToolbar` 增 `type: "bulkactions"` 批量审批按钮，含 `visibleOn` 选中行状态守卫（仅 SUBMITTED 行可批量审批）+ 确认对话框 + 成功/失败反馈
  - Skill: nop-frontend-dev
- [ ] `Proof`: 批量审批 action.spec.ts — ≥ 1 用例/域：选多行 SUBMITTED → 批量审批 → `verifyState` 断言全部 APPROVED + 混合非法状态行守卫（如部分行非 SUBMITTED 时的反馈）
  - Skill: none

Exit Criteria:

- [ ] 3 域代表实体批量审批前端按钮可达 + 后端 mutation 可调
- [ ] action.spec.ts 覆盖批量审批正路径 + 部分失败守卫

### Phase 2 - 其余批量操作（从订单导入行 + 自动核销 + 批量导入 + 批量重新排程）

Status: planned
Targets: `module-{purchase,sales,finance,master-data,aps,manufacturing}/erp-*-web/.../pages/*/Erp*.view.xml`、可能的后端 BizModel
Skill: nop-frontend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1 批量审批范式确立

- [ ] `Add`: 从订单导入行 — purchase Receive / sales Delivery 列表页批量"从订单导入行"按钮（复用 F9 copy-line-from-order 范式，扩展为批量选订单 → 批量导入行）
  - Skill: nop-frontend-dev
- [ ] `Add`: 自动核销 — finance Payment/Receipt 列表页"自动核销"批量入口（后端 `runAutoReconciliation` 已存在，前端接线批量触发）
  - Skill: nop-frontend-dev
- [ ] `Add`: 批量导入 — master-data Partner/Material 列表页批量导入入口（核实 Nop 平台 `__import` 能力 + AMIS `import` 按钮接线）
  - Skill: nop-frontend-dev
- [ ] `Add`: 批量重新排程 — aps/manufacturing 列表页"批量重新排程"按钮（后端排程引擎已存在，前端批量触发）
  - Skill: nop-frontend-dev
- [ ] `Proof`: action.spec.ts — 每类批量操作 ≥ 1 用例（选行 → 执行 → 结果断言）
  - Skill: none

Exit Criteria:

- [ ] 4 类剩余批量操作前端按钮可达
- [ ] action.spec.ts 覆盖各批量操作正路径

### Phase 3 - 模式文档 + roadmap 同步

Status: planned
Targets: `docs/design/child-table-editor-patterns.md`（或新 `batch-operation-patterns.md`）、`docs/backlog/frontend-ui-roadmap.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2 完成

- [ ] `Add`: 批量操作模式文档 — 记录 AMIS bulkactions 范式 + 后端 batch mutation 模式 + 部分失败处理 + visibleOn 守卫 + 反模式自检表（≥ 5 项）
  - Skill: none
- [ ] `Add`: roadmap 同步 — `docs/backlog/frontend-ui-roadmap.md` F11 状态 `todo → done` + 退出标准 F11 项勾选 + 本计划落地证据
  - Skill: none

Exit Criteria:

- [ ] 批量操作模式文档存在且含反模式自检表
- [ ] F11 roadmap 退出标准勾选

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is（独立子代理 ses_07990b013ffe）— 全部基线声明经仓库证据验证（F11 范围表 + 零 bulkactions + 零 batch @BizMutation + runAutoReconciliation/scheduleForward 后端就绪 + _helper.ts 三原语）；无阻塞项。范围作为单结果表面成立（Rule 14：共享 AMIS bulkactions + backend batch mutation 模式 + 共享 owner doc + 共享验证路径）。非阻塞建议：Phase 1 条件项（"如裁决为模式 A"）将在 Phase 0 裁决后具体化，不会静默丢弃。

## Closure Gates

> 完整仓库验证在此处：结束时运行一次。

- [ ] 范围内行为完成（5 类批量操作 + 模式文档 + roadmap 同步）
- [ ] 相关文档对齐（批量操作模式文档 / child-table-editor-patterns 扩展）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `npx playwright test` 批量操作 action.spec 全绿
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 长尾域批量操作扩展

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划覆盖 roadmap 范围表 5 类操作的代表域；长尾域（crm/cs/hr/logistics/b2b/contract/drp 等）批量操作按需逐域补齐
- Successor Required: yes（触发：业务客户明确要求某长尾域批量操作）

### 批量操作性能优化（分页/异步）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前批量操作面向管理后台低频场景；超大批量（> 1000 行）的异步任务化 + 进度条为性能优化
- Successor Required: yes（触发：单次批量操作行数 > 500 且响应 P95 > 3s）

## Closure

Status Note: _待执行后填写_

Closure Audit Evidence:

- Auditor / Agent: _待独立结束审计_
- Evidence: _待记录_

Follow-up:

- 长尾域批量操作扩展（触发条件见上）
- 批量操作性能优化（触发条件见上）
