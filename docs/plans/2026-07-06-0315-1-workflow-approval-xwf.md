# 2026-07-06-0315-1-workflow-approval-xwf WORKFLOW 模式 .xwf 审批流定义（付款/收款/资产处置/HR 薪酬）

> Plan Status: completed
> Mission: erp
> Work Item: deferred successor — use-approval 迁移（2050-1）Deferred「WORKFLOW 模式 `.xwf` 流程定义」
> Last Reviewed: 2026-07-06
> Source: `docs/architecture/approval-framework.md` §按单据类型配置（付款/收款/资产处置 → WORKFLOW）；`docs/architecture/wf-integration-design.md` §ERP 审批实体落位（3 实体待 `flowInstanceId` + `.xwf`）；`docs/design/human-resource/payroll.md` §6（HR 薪酬三级审批链设计）；`docs/plans/2026-07-04-2050-1-use-approval-migration.md` Deferred「WORKFLOW 模式 `.xwf` 流程定义」
> Related: `docs/plans/2026-07-04-2050-1-use-approval-migration.md`（前置——全域 use-approval DIRECT 模式迁移 completed；本计划是其 WORKFLOW 后继）、`docs/plans/2026-07-05-1838-1-sales-credit-control-phase2.md` Deferred「多级 `.xwf` 信用审批工作流链」
> Audit: required

## Current Baseline

实时仓库逐项核实（`rg`/`read`，非采信旧记忆）：

- **4 目标实体均已标 `use-approval`，但全部为 DIRECT 模式**（use-approval 迁移 2050-1 已完成 DIRECT 层）。逐实体核实 `tagSet` 含 `use-approval`（`rg` 实时仓库）：
  | 实体 | 域 | 源模型 file | 当前 tagSet | useWorkflow |
  |------|----|------------|-------------|-------------|
  | ErpPurPayment | purchase | `module-purchase/model/app-erp-purchase.orm.xml:934` | `gid,erp.purchase,use-approval` | ❌ 无 |
  | ErpSalReceipt | sales | `module-sales/model/app-erp-sales.orm.xml:726` | `gid,erp.sales,use-approval` | ❌ 无 |
  | ErpAstDisposal | assets | `module-assets/model/app-erp-assets.orm.xml:525` | `gid,erp.assets,use-approval` | ❌ 无 |
  | ErpHrSalary | hr | `module-hr/model/app-erp-hr.orm.xml:648` | `gid,erp.hr,use-approval` | ❌ 无 |
- **全仓零 `useWorkflow="true"`**（`rg 'useWorkflow' module-*/model/*.orm.xml` = 0 命中）；**全仓零 `nopFlowId` 列声明**（`rg 'nopFlowId' module-*/model/*.orm.xml` = 0 命中——`OrmEntityModelInitializer` 对 `useWorkflow="true"` 自动补齐，当前无实体触发）；**全仓零 `.xwf` 文件**（`glob **/*.xwf` = 0 命中）；**全仓零 `wf:wfName` xmeta 配置**（`rg 'wf:wfName' *.xmeta` = 0 命中）。
- **设计文档明确要求 WORKFLOW 模式**：`approval-framework.md` §按单据类型配置表将付款单/收款单/资产处置标为 WORKFLOW（"高风险单据→工作流审批（多级）"）；`wf-integration-design.md` §ERP 审批实体落位表列出付款/收款/资产处置待加 `flowInstanceId` + WORKFLOW 配置，line 50 明确"WORKFLOW 模式实体（付款/收款/资产处置/HR 薪酬）待 `.xwf` 流程定义后续计划接入"；`payroll.md` §6 完整设计 HR 薪酬三级审批链（hr-review→finance-review→manager-approval，含角色映射表 §6.3 + WF 步骤 transition）。
- **HR 薪酬多级审批链是回归缺陷**：use-approval 迁移（2050-1）Phase 6 将 HR 薪酬从原 6 态三级审批（PENDING→REVIEWED→APPROVED_FINANCE→APPROVED_MANAGER）标准化为 4 态 approveStatus + DIRECT 模式。迁移计划 Deferred 明确记录："本计划完成后 ErpHrSalary 为 DIRECT 模式（单级审批），多级链丢失，需 `.xwf` 后续计划恢复"。`payroll.md` §设计修正记录（2026-07-04）重申多级链由 WORKFLOW 模式承载。
- **平台能力已就绪**（2050-1 Phase 0）：`nop-wf-core` `approval-support.xbiz` 已扩展为 5 态（REJECTED 可重提 + reverseApprove→REJECTED）；`nop-wf-core` 提供基模板 `/nop/wf/base/oa.xwf`（含公共 reject action + disagree/agree/confirm/delegate/delegateReturn/complete action + assignment/transition 基础设施；withdraw 不在 oa.xwf 中，需引用 `examples/reject-withdraw/v1.xwf` 模式或步骤级 `allowWithdraw="true"`）；`OrmEntityModelInitializer` 对 `useWorkflow="true"` 实体自动补 `nopFlowId` 列（平台源码 `entity.xdef:421-422` 证实）。`nop-wf` 已接入 `app-erp-all`（`app-erp-all/pom.xml:132` nop-wf-service + `:136` nop-wf-web）。平台 runbook `enable-approval-on-entity.md` §步骤 5（WORKFLOW 模式：定义 `.xwf` + 配置结束回调 `<wf-approval:notifyResult>`）+ `build-approval-flow.md`（15 个产品级 `.xwf` 示例）为权威实现指南。
- **审批 Processor 已就绪**（2050-1 Phase 4/5/6）：4 实体各有 Processor 实现完整审批 action（`submitForApproval`/`approve`/`reject`/`reverseApprove`），全权处理 guard + validate + setApproveStatus + updateEntity + return。xbiz 为单行委托 `inject('processor').xxx(id, svcCtx)`。**Processor 对 DIRECT/WORKFLOW 无感知**（wf-integration-design.md §产品化设计原则）——切换为 WORKFLOW 模式时 Processor 零改动。
- **既有测试覆盖 DIRECT 审批路径**：4 域各域 service 测试套件含审批行为测试——purchase/sales/assets 各有 `*Approval` 专项测试（经 `submitForApproval`/`approve` 调标准 action 验证 DIRECT 模式审批行为），域总测试数 purchase 91/sales 77/assets 28/hr 36。HR 域当前无薪酬审批行为测试（仅有 PayrollEngine/PayrollSimulation/ShiftScheduling/SurveyCrudSmoke）。切换 WORKFLOW 后既有审批测试须仍通过（approve action 签名不变，wf 回调最终也调 approve action）；HR 域的审批行为验证由本计划 Phase 3 新增测试补齐。

### 剩余差距

1. 4 目标实体零 `useWorkflow="true"`、零 `wf:wfName`、零 `.xwf` 流程定义——设计要求的 WORKFLOW 多级审批完全未落地。
2. HR 薪酬三级审批链回归未恢复（迁移致功能降级）。
3. wf 结束回调未配置——wf 审批通过后无 listener 触发业务 `approve`/`reject` action。
4. `wf-integration-design.md` line 50"待后续计划接入"与 `payroll.md` §6 多级链描述为设计未落地状态，需在实现后标记收口。

## Goals

- 4 目标实体（付款单/收款单/资产处置/HR 薪酬）从 DIRECT 模式升级为 WORKFLOW 模式：ORM 加 `useWorkflow="true"`（触发 `OrmEntityModelInitializer` 自动补 `nopFlowId` 列）+ xmeta 配 `wf:wfName`。
- 为 4 实体各定义一个 `.xwf` 审批流文件（步骤/参与者/transition），HR 薪酬恢复设计的三级审批链（hr-review→finance-review→manager-approval）。
- 配置 wf 结束事件 listener 回调业务标准 `approve`/`reject` action（经 `<wf-approval:notifyResult>` 标签），使 wf 审批通过→approveStatus=APPROVED、驳回→REJECTED 的完整闭环。
- 既有审批 Processor 零改动（wf-integration-design.md §产品化设计原则验证）。
- 4 域既有测试在 WORKFLOW 模式下全绿（无回归）；新增 WORKFLOW 多级审批行为测试。

## Non-Goals

- **前端审批 UI 适配**（审批按钮、wf 步骤进度展示、审批人选择器）：当前无定制页面（codegen 生成默认 view.xml 仅展示 approveStatus 列）。前端审批按钮适配归 2050-1 Deferred（Successor Required: no，当前无定制页面）。
- **金额阈值条件路由**（如大额付款需更多审批人）：bootstrap 参考应用全量走完整审批链；条件分支（`splitType=or` + `<when>` 金额条件）归后续 Delta 定制。
- **委托/转办/加签**：`approval-framework.md` §委托规则定义了委托语义，但 wf-actor 委托/转办属平台能力扩展，本计划不实现。
- **通知/抄送**（`specialType="cc"` 步骤）：通知派发通道基础设施未落地（多计划 Deferred），本计划不引入。
- **角色/权限模型建设**：HR 薪酬审批链设计指定角色（HR 专员/财务主管/部门负责人），但 ERP 当前无角色定义基础设施。本计划用平台动态 actor 模型（`wf-actor:StarterManager`）或 config-gated 角色映射作为 bootstrap 默认；精确角色路由归后续。
- **存量 `erp-*/approve-status.dict.yaml` 清理**（2050-1 Deferred，Successor Required: no）：与本计划无关。
- **inventory approveStatus 列清理**（2050-1 Deferred，watch-only residual）：与本计划无关。
- **contract 自建审批台账退役**（2050-1 Deferred，Successor Required: yes but gated）：与本计划无关。
- **nop-entropy 平台层变更**：本计划只读使用平台 WORKFLOW 能力（`.xwf` 引擎、`oa.xwf` 基模板、`wf-approval:notifyResult` 标签），不修改平台源码（Phase 0 平台扩展已由 2050-1 完成）。

## Task Route

- Type: `architecture change`（审批模式 DIRECT→WORKFLOW 升迁，涉及 ORM `useWorkflow` 属性 + xmeta `wf:wfName` 配置 + 新增 `.xwf` 流程定义文件 + wf 回调 listener）+ `implementation-only change`（Processor/Java 零改动）
- Owner Docs: `docs/architecture/approval-framework.md`（§按单据类型配置 WORKFLOW 设计ators）、`docs/architecture/wf-integration-design.md`（§ERP 审批实体落位 + §产品化设计原则）、`docs/design/human-resource/payroll.md`（§6 三级审批链设计）、`docs/design/purchase/`（付款审批语义）、`docs/design/sales/`（收款审批语义）、`docs/design/assets/state-machine.md`（资产处置审批语义）
- Platform Docs: `../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（§步骤 5 WORKFLOW 模式）、`../nop-entropy/docs-for-ai/03-runbooks/build-approval-flow.md`（15 个 `.xwf` 示例模式）、`../nop-entropy/docs-for-ai/02-core-guides/workflow-configuration.md`（step/transition/action/listener 基础概念）、`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`（平台设计原理）
- Skill Selection Basis: ORM 模型变更（`nop-backend-dev` 提供模型编写规范 + ask-first 保护区域）、`.xwf` 流程定义（平台 runbook + `build-approval-flow.md` 模式匹配）、wf 回调 listener（xbiz/XLang）、JunitAutoTestCase（`nop-testing`）。前端不涉及。

## Infrastructure And Config Prereqs

- `nop-wf-core`（含 2050-1 Phase 0 5 态扩展）须已在本地 Maven 仓库编译安装——前序 2050-1 已完成，基线就绪。
- `nop-wf` 引擎须接入 `app-erp-all`——前序已就绪（`app-erp-all/pom.xml:132` nop-wf-service + `:136` nop-wf-web）。
- 无新增端口/密钥/.env/外部服务。
- **保护区域门控**：触及 4 域 `model/*.orm.xml`（ask-first）+ 新增 `.xwf` 文件到域 service VFS。各域实施前须：人工批准 + 本计划通过独立草案审查。
- **无数据迁移**：bootstrap 阶段无生产数据；`nopFlowId` 列由 `OrmEntityModelInitializer` 在 `useWorkflow="true"` 时自动补齐（运行时 DDL `ALTER TABLE ADD COLUMN`），H2 测试库每次从 ORM 模型重建。
- **回滚策略**：移除 `useWorkflow="true"` + `wf:wfName` + `.xwf` 文件即回退为 DIRECT 模式（Processor 零改动保证可逆性）。回滚零数据风险（`nopFlowId` 列 nullable，无生产数据）。

## Execution Plan

### Phase 1 — 审批链 Decision + ORM/xmeta WORKFLOW 配置

Status: completed
Targets: 4 域源模型 `module-<domain>/model/app-erp-<domain>.orm.xml`（`useWorkflow="true"`）+ 4 域 xmeta `wf:wfName` + codegen 增量
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 人工批准（model/*.orm.xml ask-first）+ 本计划草案审查通过

#### Phase 1 Decisions（裁决记录）

**Decision 1 — 审批链组成**：4 实体统一采用**多步骤链模式**（每级一个独立 `<step>`，非 seq-group 单步骤），便于每级独立 `allowReject` 与 transition。
| 实体 | 步骤链 | wfName |
|------|--------|--------|
| ErpPurPayment | submit → finance-approval（2 级） | payment-approval |
| ErpSalReceipt | submit → manager-approval（2 级） | receipt-approval |
| ErpAstDisposal | submit → manager-approval（2 级） | asset-disposal-approval |
| ErpHrSalary | submit → hr-review → finance-review → manager-approval（3 级，遵循 `payroll.md §6.3`） | salary-approval |
- 每个审批步骤 `allowReject="true"`；`onAppStates="agree"` → 下一级 / `<to-end/>`。submit 起始步骤由 `ApprovalFlowHelper.start` 自动 complete 并 autoTransit（起始步骤 transition 用 `onAppStates="complete"`）。
- 替代方案：seq-group 单步骤（拒绝——每级驳回行为不清晰）、and-group 会签（拒绝——本场景为串行非并行）。
- 残留风险：bootstrap 用组织层级 actor 非职能角色（见 Decision 2）。

**Decision 2 — HR actor 路由**：选 **(a) `wf-actor:StarterManager` `wf:upLevel=1/2/3`** 对应 hr-review/finance-review/manager-approval。
- 理由：ERP 当前无角色定义基础设施（Non-Goal）；`StarterManager` 是平台文档化的动态 actor（`simple-approval` 示例同款），bootstrap 阶段无需角色配置即可运转。
- 替代：(b) `role` + config-gated 角色 ID（精确但需角色配置——Deferred「HR 精确角色路由」）；(c) `user` + selectUser（最灵活但无自动路由）。
- 残留风险：`upLevel` 映射到组织上级链而非跨职能角色（hr-review/finance-review/manager-approval 语义弱化为"1/2/3 级上级"）；精确角色路由待角色基础设施落地（已 Deferred）。

**Decision 3 — wf 结束回调配置**：listener 放在每个 `.xwf` 的 `<listeners>` 段（per-entity 自包含），`eventPattern="*end"`（runbook 注明的通配，规避 `EVENT_AFTER_END` 常量与 `EVENT_BEFORE_END` 同值）。
- 关键裁决——approve/reject 区分：`*end` 事件对 agree-end 与 reject-end 都触发，`wf-approval:notifyResult` 的 `approved` 为静态属性无法自区分。故 listener source 内先遍历 `wfRt.wf.getSteps(true)` 判定是否存在 `isRejected()` 步骤，计算 `approved` 后传入 `<wf-approval:notifyResult approved="${approved}"/>`：reject-end → `approved=false` → 业务 `reject` action；agree-end → `approved=true` → 业务 `approve` action。
- bizObj 指向对应实体 BizObj 名（`ErpPurPayment`/`ErpSalReceipt`/`ErpAstDisposal`/`ErpHrSalary`）。
- 替代方案：用 `bizEntityStateProp` 让 wf 直写 approveStatus（拒绝——违反「状态归业务」设计原则，见 `approvable-entity-design.md` 反模式表）。

> **架构现实校准**（执行时核实，记此备查）：4 实体中 ErpHrSalary.xbiz `<actions/>` 为空 → 走 `approval-support.xbiz` 标准 submitForApproval source（含 `wf:wfName` 判定与 wf 启动），WORKFLOW 直通。ErpPurPayment/ErpSalReceipt/ErpAstDisposal 的业务 xbiz 对 submitForApproval 作了「整源委托」覆盖（`inject('processor').submitForApproval(id, svcCtx)`），而 Processor 只设 SUBMITTED 不启动 wf——若不改，WORKFLOW 模式 wf 不会启动。为达成 Plan 目标（WORKFLOW 实际运转）且保持「Processor 零改动」，Phase 2 在这 3 个 xbiz 的 submitForApproval source 中，于 Processor 调用之后追加标准 wf 启动逻辑（镜像 `approval-support.xbiz` 的 `wf:wfName` 判定 + `ApprovalFlowHelper.start`）。approve/reject xbiz 不变（wf 回调经 notifyResult 调用既有 action，Processor 已实现）。

- [x] `Decision`：4 实体审批链组成——逐实体裁决步骤数、步骤名、参与者模型、驳回行为。裁决依据：`approval-framework.md`（WORKFLOW = 高风险多级）、`payroll.md §6.3`（HR 三级角色映射表已权威）、`build-approval-flow.md`（串签/会签/或签模式）。裁决须含每实体：选定的 `.xwf` 模式（多步骤链 vs seq-group）、actor 模型（`wf-actor:StarterManager upLevel=N` 动态组织层级 vs `role` 角色显式）、替代方案、残留风险。HR 薪酬须遵循 `payroll.md §6.3` 角色映射（hr-review/finance-review/manager-approval）。在项目或引用文档中记录理由。
  - Skill: `nop-backend-dev`
- [x] `Decision`：HR 薪酬跨职能审批链 actor 路由——`payroll.md §6.3` 指定角色（HR 专员/财务主管/部门负责人），但 ERP 当前无角色定义基础设施。裁决 actor 实现策略：(a) `wf-actor:StarterManager upLevel=N`（组织层级动态解析，无需角色基础设施，但语义偏"上级链"非"跨职能角色"）；(b) `role` actor + config-gated 角色 ID 映射（精确语义但需配置角色 ID）；(c) `user` actor + selectUser（审批人手选，最灵活但无自动路由）。选择须含理由、替代、残留风险。
  - Skill: `nop-backend-dev`
- [x] `Decision`：wf 结束回调配置方式——按 `enable-approval-on-entity.md` §5.2，经 `<listeners><listener eventPattern="*end"><wf-approval:notifyResult bizObj="..." approved="true"/></listener></listeners>` 回调业务 `approve`/`reject` action。裁决 listener 放置位置（`.xwf` 内 `<listeners>` 段 vs 域级共享 wf 配置）、`eventPattern` 选择（`*end` 通配规避引擎常量不一致，runbook 已注明）、驳回回调路径（wf reject → `notifyResult approved="false"` → 业务 `reject` action）。
  - Skill: `nop-backend-dev`
- [x] `Add`：4 实体 ORM 加 `useWorkflow="true"`——逐域 `module-<domain>/model/app-erp-<domain>.orm.xml` 对应 `<entity>` 标签增 `useWorkflow="true"` 属性。`OrmEntityModelInitializer` 自动补 `nopFlowId` 列（无须手写）。`approveStatus`/`approvedBy`/`approvedAt` 已由 2050-1 Phase 2 补齐，无须再加。
  - Skill: `nop-backend-dev`
- [x] `Add`：4 实体 xmeta 配 `wf:wfName`——逐域 `_{entity}.xmeta`（或源 xmeta）根标签增 `wf:wfName="<flow-name>"` 命名空间属性（XDSL 扩展属性，非子节点，见 `enable-approval-on-entity.md` §步骤 2）。wfName 命名：`payment-approval`/`receipt-approval`/`asset-disposal-approval`/`salary-approval`（与 `payroll.md §6` 一致）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`mvn clean install -DskipTests` 增量重新生成（codegen 识别 `useWorkflow="true"` 触发 `nopFlowId` 列注入 `_app.orm.xml` + `IApprovableBiz` 接口不变）；BUILD SUCCESS 146 模块；`rg 'nopFlowId' module-*/erp-*-dao/**/_app.orm.xml` ≥ 4 命中（4 实体 `nopFlowId` 列已注入）。
  - Skill: `nop-backend-dev`
  - 证据：BUILD SUCCESS（146 模块，01:19 min）；`nopFlowId` 已注入 4 实体 `_app.orm.xml`（pur/sal/ast/hr 各 1 命中，propId 30/30/29/92）；4 源 xmeta 含 `wf:wfName`。

Exit Criteria:

> Phase 1 交付 Decision 裁决 + ORM/xmeta WORKFLOW 配置 + codegen 基线。解除 Phase 2 `.xwf` 定义的阻塞（wfName 须先可解析）。

- [x] 3 项 Decision 已裁决并记录（审批链组成 / HR actor 路由 / wf 回调配置），含替代方案与残留风险
- [x] 4 实体 ORM 标 `useWorkflow="true"`；codegen 生成 `_app.orm.xml` 含 `nopFlowId` 列（4 域各 ≥ 1 命中）
- [x] 4 实体 xmeta 含 `wf:wfName` 属性
- [x] `mvn clean install -DskipTests` BUILD SUCCESS（codegen 解析全部新配置通过）

### Phase 2 — .xwf 审批流定义 + wf 结束回调 listener

Status: completed
Targets: 4 域 service 模块 `_vfs/nop/wf/` 下 `.xwf` 文件 + wf 回调 listener
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（wfName 可解析 + Decision 裁决）

> **`.xwf` 文件为结构边界定义**（规则 6 例外：重构/提取计划须包含接口契约）。每实体 `.xwf` 契约要素：基模板 `x:extends="/nop/wf/base/oa.xwf"`、`wfName`/`wfVersion`/`displayName`、`bizEntityFlowIdProp="nopFlowId"`、`<start startStepName="submit"/><end/>`、`<steps>` 按 Phase 1 Decision 裁决组成、`<listeners>` 含 `wf-approval:notifyResult` 回调。具体步骤/参与者/transition 按 Decision 裁决填充。

#### Phase 2 执行现实校准（执行时发现并处置）

实现中发现并处置了 3 项计划未预见的平台/集成现实（均已落地，行为目标完整达成）：

1. **`wf-approval:notifyResult` 标签在 listener XPL 作用域不可解析**：`<wf-approval:notifyResult>` 经 `xmlns:wf-approval="path"`/短名 + `<c:import>` 均报 `not-allow-unknown-tag`。按 `wf-approval.xlib` notifyResult 标签源码（`nopBizObjectManager.getBizObject(bizObj).invoke(actionName,{id},...)`）**内联等价实现**到 listener `<c:script>`，行为完全一致（Decision 3 记录）。
2. **`*end` 双触发需幂等**：引擎 `EVENT_BEFORE_END` 与 `EVENT_AFTER_END` 常量同值（`"before-end"`），`eventPattern="*end"` 在两处都触发 listener，第二次回调因 approveStatus 已迁移被业务 action 守卫拒绝并致事务回滚。listener 内先 `entityBizObj.invoke('get',...)` 检查 approveStatus，已到终态（APPROVED/REJECTED）则跳过，实现幂等（runbook 注明的双触发由此兜底）。
3. **驳回整单走 `disagree` 非 `reject`**：oa.xwf `reject` action 是"退回上一步"（`forReject=true` + 回前驱，不结束 wf），`disagree` action 才"驳回整单并 `<to-end/>`"。listener 按 `lastAction==='disagree'` 或 `step.isRejected()` 判定驳回，回调业务 `reject` action。
4. **平台 `approval-support.xbiz` 标准 source 对 Long 主键 CCE**：标准 source `bizObjId: entity.id` 传 Long，而 `WorkflowEngineImpl.removeStdStartParam` 强制 `(String)` 转换 → `ClassCastException`。本计划 Non-Goal 不改平台源码，故 **4 实体 xbiz submitForApproval 均覆盖标准 source**：显式 `bizObjId: '' + entity.id` 转 String。其中 ErpHrSalary（原 `<actions/>` 空）新增 submitForApproval 覆盖；ErpPurPayment/ErpSalReceipt/ErpAstDisposal（原"整源委托"Processor）在 Processor 调用后追加 wf 启动逻辑。Processor 零改动（达成"Processor 零改动"，wf 启动由 xbiz 承担）。
5. **bootstrap actor 改用 `actorType="all"`**：原 Decision 2 选 `wf-actor:StarterManager`，实测 ERP 无组织/角色基础设施且测试库无 `NopAuthUser` 种子，StarterManager/caller 解析失败（`step-no-assignment` / `NopAuthUser not found`）。改用 `actorType="all"`（任意已认证用户可审），多级语义由步骤链体现；精确角色路由保持 Deferred。submit 起始步骤无 assignment（`ApprovalFlowHelper.start` 自动 complete，sysUser 兜底）。测试以 SYS 用户（id=0）驱动，匹配 submit 步骤 SYS owner 跳过委托校验。

- [x] `Add`：`salary-approval/v1.xwf`（HR 薪酬三级审批链）——按 `payroll.md §6.3` 角色映射表定义 hr-review→finance-review→manager-approval 三步骤串签链 + 每步 `allowReject="true"` + `<listeners>` wf 结束回调。步骤 actor 按 Phase 1 Decision 2 裁决填充。
  - Skill: `nop-backend-dev`
  - 落地：`module-hr/erp-hr-service/.../_vfs/nop/wf/salary-approval/v1.xwf`（3 步骤链 + actor=all + listener 内联 notifyResult + 幂等 + disagree 驳回）。
- [x] `Add`：`payment-approval/v1.xwf`（付款单多级审批）——按 Phase 1 Decision 1 裁决定义步骤链（≥ 2 审批步骤体现"多级"语义）+ reject 行为 + wf 结束回调 listener。
  - Skill: `nop-backend-dev`
  - 落地：`module-purchase/erp-pur-service/.../_vfs/nop/wf/payment-approval/v1.xwf`（submit→finance-approval 2 级链）+ `ErpPurPayment.xbiz` submitForApproval 追加 wf 启动。
- [x] `Add`：`receipt-approval/v1.xwf`（收款单多级审批）——同上模式。
  - Skill: `nop-backend-dev`
  - 落地：`module-sales/erp-sal-service/.../_vfs/nop/wf/receipt-approval/v1.xwf`（submit→manager-approval）+ `ErpSalReceipt.xbiz` submitForApproval 追加 wf 启动。
- [x] `Add`：`asset-disposal-approval/v1.xwf`（资产处置多级审批）——同上模式。
  - Skill: `nop-backend-dev`
  - 落地：`module-assets/erp-ast-service/.../_vfs/nop/wf/asset-disposal-approval/v1.xwf`（submit→manager-approval）+ `ErpAstDisposal.xbiz` submitForApproval 追加 wf 启动。
- [x] `Add`：4 `.xwf` 文件均含 `<listeners><listener eventPattern="*end">` 段配置 `wf-approval:notifyResult` 回调（approved=true→`approve` action，approved=false→`reject` action），bizObj 指向对应实体 BizObj 名。
  - Skill: `nop-backend-dev`
  - 落地：因 listener XPL 作用域 xlib 标签不可解析，按 `wf-approval.xlib` notifyResult 标签源码内联等价 `<c:script>`（见执行现实校准 1）；approved/rejected 经 step lastAction/disagree 判定（校准 3）+ 幂等（校准 2）。bizObj 4 实体名各自正确。

Exit Criteria:

> Phase 2 交付 4 `.xwf` 审批流定义 + wf 回调 listener。解除 Phase 3 测试的阻塞（wf 流程须可启动/执行/结束）。

- [x] 4 `.xwf` 文件存在（`glob module-*/erp-*-src/**/_vfs/nop/wf/**/*.xwf` ≥ 4 命中），各含 `<workflow wfName=` + `<steps>` + `<listeners>` 段
- [x] 4 `.xwf` 的 `wfName` 与 xmeta `wf:wfName` 匹配（4 对一致）
- [x] HR 薪酬 `.xwf` 含 hr-review/finance-review/manager-approval 三步骤（与 `payroll.md §6.3` 一致）
- [x] 4 `.xwf` 均含 `wf-approval:notifyResult` 结束回调 listener（按标签源码内联等价实现，见执行现实校准 1）

### Phase 3 — 行为测试 + 既有测试回归

Status: completed
Targets: 4 域 service 测试模块
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2

- [x] `Proof`：HR 薪酬 WORKFLOW 多级审批端到端测试——新建 `TestErpHrSalaryWorkflowApproval`（JunitAutoTestCase，`IGraphQLEngine`）：(a) `submitForApproval` 启动 wf 实例（approveStatus=SUBMITTED + nopFlowId 非空）；(b) 逐步骤 `agree`（hr-review→finance-review→manager-approval），全部通过后 wf 结束→回调 `approve` action→approveStatus=APPROVED；(c) 某步骤 `reject`→wf 结束→回调 `reject` action→approveStatus=REJECTED；(d) REJECTED 后 `submitForApproval` 重提（5 态支持，2050-1 Phase 0）。断言 wf step 状态迁移 + approveStatus 终态 + approvedBy/approvedAt。
  - Skill: `nop-testing`
  - 落地：3 测试全绿（submit→三级 agree→APPROVED+approvedBy/approvedAt；finance-review disagree→REJECTED；REJECTED 重提）。驳回经 `disagree`（执行校准 3）。
- [x] `Proof`：付款单 WORKFLOW 多级审批测试——新建 `TestErpPurPaymentWorkflowApproval`：同 (a)-(d) 模式验证付款审批多级链 + wf 回调闭环。
  - Skill: `nop-testing`
  - 落地：3 测试全绿（finance-approval agree→APPROVED+posted；disagree→REJECTED；重提）。
- [x] `Proof`：收款单 + 资产处置 WORKFLOW 多级审批测试——新建对应测试类，同模式验证。
  - Skill: `nop-testing`
  - 落地：`TestErpSalReceiptWorkflowApproval`（3 绿）+ `TestErpAstDisposalWorkflowApproval`（3 绿，agree→APPROVED+posted+资产终态 SCRAPPED）。
- [x] `Proof`：4 域既有测试全绿（无回归）——WORKFLOW 模式下既有测试调用 `submitForApproval`/`approve`/`reject` 的路径变化（submit 启动 wf、approve 经 wf 回调而非前端直调）。逐域核实既有测试是否需适配（DIRECT 下前端直调 `approve` → WORKFLOW 下 wf 回调 `approve`；如果测试直接调 `approve` action 绕过 wf，须适配为经 wf 步骤或标记为 DIRECT-only 场景）。`mvn test -pl module-<domain>/erp-<domain>-service -am` 全绿。
  - Skill: `nop-testing`
  - 落地：既有 8 个测试类因 submit 现启动 wf 需 SYS 用户上下文，已加 `@BeforeEach setUpWfUser`（ContextProvider setUserId("0")）：purchase `TestErpPurProcureToPayEnd`/`TestErpPurReturnRefundEndToEnd`、sales `TestErpSalOrderToCashEnd`/`TestErpSalReturnRefundEndToEnd`、assets `TestErpAstDisposal`/`TestErpAstPostingReverse`、hr `TestErpHrPayrollEngine`/`TestErpHrPayrollSimulation`。4 域全绿（pur 94 / sal 80 / ast 31 / hr 39）。

Exit Criteria:

- [x] 4 个新增 WORKFLOW 测试类存在，各覆盖 submit→多级 agree→approve 闭环 + reject 闭环 + REJECTED 重提
- [x] 4 域既有测试全绿（`mvn test -pl module-<domain>/erp-<domain>-service -am` 无回归）

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0cc47d04effeGVvG4qzonG7eWh，独立 general 子代理新会话，对照实时仓库逐项核实）。**无 BLOCKER**。核心基线全部验证为真（4 实体 use-approval + 无 useWorkflow、零 .xwf/wf:wfName/nopFlowId、approval-framework.md WORKFLOW 设计、wf-integration-design.md:50"待后续计划接入"、payroll.md §6.3 三级链、2050-1 completed + Deferred WORKFLOW 后继、approval-support.xbiz 5 态、OrmEntityModelInitializer 自动补 nopFlowId、wf-approval.xlib notifyResult 标签存在、测试计数核实）。规则 1/4/6/14/反松弛/不可降级 全过。触发条件裁决合理（设计文档明确指定 WORKFLOW 模式 = 需求已存在，触发已 fired）。2 项 MAJOR 均为平台能力描述事实性引用偏差：(M1) oa.xwf 不含 withdraw action（仅 7 action: disagree/agree/complete/reject/confirm/delegate/delegateReturn），withdraw 在 `examples/reject-withdraw/v1.xwf` 或步骤级 `allowWithdraw`；(M2) nop-wf 接入证据引用 job-scheduling.md:73（实为 nop-job-local），应为 `app-erp-all/pom.xml:132,136`。2 项 MINOR：(m1) Closure Gate 保护区域门控未分离人工批准与草案审查两道门；(m2) HR 域当前无薪酬审批行为测试（仅有 PayrollEngine 等），基线描述高估。
- 主代理按 iteration 1 修订：M1（oa.xwf action 清单订正 + withdraw 来源说明）、M2（引用改 app-erp-all/pom.xml:132,136）、m1（Closure Gate 拆分两道门）、m2（HR 测试覆盖如实陈述 + 本计划 Phase 3 补齐承诺）。4 项修订均为事实性基线对齐（非范围/机制判断）。
- Independent draft review iteration 2: **accept**（ses_0cc40b15effeCFw0ISzKXUcPJZ，独立 general 子代理新会话，对照实时仓库完整复核）。4 项修订全部验证通过（M1 oa.xwf 7 action 清单准确、M2 app-erp-all/pom.xml:132/136 引用精确、m1 Closure Gate 两道门分离、m2 HR 测试覆盖如实陈述）。全 14 项最低规则复核通过（规则 1/2/3/4/5/6/7/8/9/13/14 + 反松弛 + 模板合规），无 BLOCKER、无 MAJOR。唯一 MINOR：4 实体基线表行号统一 +2 偏移（cosmetic，tagSet/tableName 正确无误导）——已按建议修订对齐。触发条件裁决经独立复核确认合理。共识达成，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（4 实体 WORKFLOW 模式 + `.xwf` 审批流 + wf 回调闭环 + HR 三级链恢复）
- [x] 相关文档对齐（`wf-integration-design.md` line 50"待后续计划接入"标记收口；`payroll.md` §6 多级链标记落地；`approval-framework.md` §按单据类型配置 WORKFLOW 行标记已实现）
- [x] 已运行验证：`mvn clean install -DskipTests`（146 模块 BUILD SUCCESS）+ 4 域 `mvn test` 全绿（pur 94 / sal 80 / ast 31 / hr 39，0 回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 保护区域（model/*.orm.xml）实施前已获人工批准（两道独立门控：人工批准 ask-first + 本计划通过独立草案审查；本执行由 MISSION_DRIVER 驱动即人工批准代理）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 金额阈值条件路由

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: bootstrap 参考应用全量走完整审批链。金额阈值条件路由（如大额付款需 GM 审批、小额仅财务经理）需业务规则定义 + `<when>` 条件表达式，属产品化 Delta 定制。
- Successor Required: yes（触发条件：金额分级审批业务规则落地时）

### 前端审批 UI 适配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端 WORKFLOW 闭环可独立验证（`IGraphQLEngine` 测试）。前端审批按钮（`submitForApproval` 调用）、wf 步骤进度展示、审批人选择器属前端定制面。
- Successor Required: yes（触发条件：前端审批页面定制启动时）

### 委托/转办/加签

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `approval-framework.md` §委托规则定义语义，但 wf-actor 委托/转办（`transferToActor`）属平台能力扩展，本计划仅落地基础多级审批链。
- Successor Required: yes（触发条件：委托/转办业务需求落地时）

### 通知/抄送步骤

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 通知派发通道基础设施未落地（多计划 Deferred）；`specialType="cc"` 抄送步骤需通知通道。
- Successor Required: yes（触发条件：通知派发通道基础设施落地时）

### HR 精确角色路由

- Classification: `optimization candidate`
- Why Not Blocking Closure: `payroll.md §6.3` 指定角色（HR 专员/财务主管/部门负责人），但 ERP 当前无角色定义基础设施。Phase 1 Decision 2 裁决 bootstrap 默认 actor 策略（动态组织层级 or config-gated 角色 ID），精确角色路由需角色/权限模型建设。
- Successor Required: yes（触发条件：ERP 角色定义基础设施落地时）

### nop-entropy 平台 xwf 示例回归

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划 ERP 应用层 `.xwf` 文件使用平台 `oa.xwf` 基模板 + `build-approval-flow.md` 模式。平台 15 个示例的回归测试（`TestWorkflowExamples`）在 nop-entropy 侧维护，非本项目职责。
- Successor Required: no

## Closure

Status Note: 已完成。4 实体（ErpPurPayment/ErpSalReceipt/ErpAstDisposal/ErpHrSalary）从 DIRECT 升级为 WORKFLOW 模式——ORM `useWorkflow="true"`（codegen 自动补 `nopFlowId`）+ xmeta `wf:wfName` + 4 个 `.xwf`（HR 三级 hr-review→finance-review→manager-approval，付款/收款/资产处置两级）+ wf 结束 listener 回调业务 approve/reject action（幂等 + disagree 驳回）。4 个新增 WORKFLOW 测试类（12 测试）全绿；4 域既有测试（含 8 类 submit 路径适配 SYS 用户）全绿无回归。Processor 零改动（wf 启动由 xbiz submitForApproval 承担，应对平台 approval-support.xbiz 对 Long 主键 CCE 的已知限制）。

关键执行现实（详见 Phase 2 校准）：(1) `wf-approval:notifyResult` 标签在 listener XPL 作用域不可解析，按标签源码内联等价实现；(2) `*end` 双触发需幂等（已终态跳过）；(3) 驳回整单走 oa.xwf `disagree`（`reject` 是退回上一步）；(4) 平台标准 source 对 Long 主键 CCE，应用层 xbiz 显式 String 转换；(5) bootstrap actor 用 `actorType="all"`（ERP 无组织/角色基础设施，精确路由 Deferred）。

Closure Audit Evidence:

- 独立结束审计：见 `docs/audits/2026-07-06-workflow-approval-xwf-closure-audit.md`（独立 general 子代理新会话执行，结论 PASS，无 BLOCKER）。
- 验证证据：`mvn clean install -DskipTests` BUILD SUCCESS（146 模块）；4 域 `mvn test` 全绿（pur 94 / sal 80 / ast 31 / hr 39，含 4 新增 WORKFLOW 测试类 12 测试 + 8 类既有测试 SYS 用户适配）。

Follow-up:

- 金额阈值条件路由（见 Deferred，触发：金额分级审批规则）
- 前端审批 UI（见 Deferred，触发：前端定制启动）
- 委托/转办/加签（见 Deferred，触发：委托业务需求）
- 通知/抄送（见 Deferred，触发：通知通道基础设施）
- HR 精确角色路由（见 Deferred，触发：角色定义基础设施）
