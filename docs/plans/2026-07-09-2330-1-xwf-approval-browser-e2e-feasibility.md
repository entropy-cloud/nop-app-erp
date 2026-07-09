# 2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility 审批工作流（xwf）浏览器层 E2E 可行性探索与代表性覆盖

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Mission: erp
> Work Item: 各域细化端到端验证（xwf / useWorkflow 审批轴浏览器层 E2E）
> Source: deferred 项承接 `docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md` Deferred「Payment/Receipt xwf 浏览器层 E2E」（Successor Required: yes，触发条件「当 xwf 浏览器层审批 API 验证可行 / nop 用户 wf 委托配置落地时」——**待本计划 Explore 裁决是否满足**）+ `docs/plans/2026-07-09-2004-2-reverse-voucher-e2e.md` Deferred「域审批轴 `ErpXxx__reverseApprove` / Payment-Receipt xwf 反向」useWorkflow 子集
> Related: `docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`（Phase 3 探针实证 user:$0 阻塞，本计划源头）、`docs/plans/2026-07-06-0315-1-workflow-approval-xwf.md`（xwf 接线后端 + 4 个 `.xwf` 定义）、`docs/plans/2026-07-09-0814-2-business-action-graphql-e2e.md`（业务动作浏览器层范式源）、`docs/architecture/approval-framework.md`（审批框架 §按单据类型配置 WORKFLOW）
> Audit: required

## Current Baseline

- **xwf（useWorkflow）审批轴浏览器层不可达已实证**（`2026-07-09-1249-1` Phase 3 探针）：`nop` 浏览器用户调 Payment `submitForApproval`，xwf 返回 `步骤[submit]不允许被用户[<nop uuid>]调用,步骤的参与者限定为[user:$0]`——wf `submit` 步骤参与者限定为 `user:$0`（SYS id=0），`nop` 用户不匹配致 submit 被拒，Payment 停留 UNSUBMITTED，approve 因状态守卫失败。后端 Java 集成测试经 `setUserId("0")` 规避此约束，浏览器层无此出口。该探针 spec 裁决后已删除。
- **useApproval（approvalStatus 轴）浏览器层已覆盖**（`2026-07-09-1249-1`）：PO/Receive/Invoice、SO/Delivery/Invoice 的 `submitForApproval`→`approve` DIRECT `@BizMutation` 在浏览器层全绿（这些实体审批虽后端经 use-approval→xwf 接线 `2026-07-04-2050-1`/`2026-07-06-0315-1`，但 BizModel 仍以 DIRECT `submitForApproval`/`approve` 入口暴露，浏览器层可达）。**本计划不重复覆盖此轴**，聚焦 useWorkflow 轴（Payment/Receipt/Disposal/Salary submit 被 wf 步骤参与者直接拦截）。
- **useWorkflow（`useWorkflow="true"`）实体经 ORM 核实共 4 个，各有一个 `.xwf` 工作流定义**（`grep 'useWorkflow="true"' module-*/model/*.orm.xml` + `find -name "*.xwf"`）：
  - **purchase Payment**（`module-purchase/model/app-erp-purchase.orm.xml:928` `useWorkflow="true"`）→ `module-purchase/erp-pur-service/_vfs/nop/wf/payment-approval/v1.xwf`
  - **sales Receipt**（`module-sales/model/app-erp-sales.orm.xml:720` `useWorkflow="true"`）→ `module-sales/erp-sal-service/_vfs/nop/wf/receipt-approval/v1.xwf`
  - **assets Disposal**（`module-assets/model/app-erp-assets.orm.xml:580` `useWorkflow="true"`）→ `module-assets/erp-ast-service/_vfs/nop/wf/asset-disposal-approval/v1.xwf`
  - **hr Salary**（`module-hr/model/app-erp-hr.orm.xml:668` `useWorkflow="true"`）→ `module-hr/erp-hr-service/_vfs/nop/wf/salary-approval/v1.xwf`
- **`user:$0` 阻塞根因已定位（非「未定位」）**：读 `payment-approval/v1.xwf` 描述段明示「submit 起始步骤无 assignment（由 ApprovalFlowHelper.start 自动 complete，sysUser 兜底）」。`submit` step 无 `<assignment>`，由引擎 `ApprovalFlowHelper.start` 自动 complete，**sysUser(id=0) 兜底**——故 `nop` 浏览器用户调 Payment `submitForApproval` 时被引擎按 sysUser(0) 解析，`nop` 用户不匹配 `user:$0` 致 submit 被拒。此为 **nop-entropy 工作流引擎的 sysUser 兜底默认**，非本仓 `nop_sys_wf_*` 种子/配置可调（经核实本仓及 `_dump` 无 `nop_sys_wf*` 种子 CSV）。
- **后端测试的规避出口不可用于浏览器层**：后端 `TestErpPurPaymentWorkflowApproval` 等经 `setUserId("0")`+`setUserName("SYS")` 注入上下文 + 直接调 `IWorkflow` Java API（`step.invokeAction()`）驱动 wf 步骤；浏览器层经 GraphQL `/graphql`，既无 `setUserId` 出口也无 Java `IWorkflow` 直调路径。**这是本计划 Phase 1 Explore 必须裁决的核心**——nop-entropy 是否提供浏览器层 nop 用户 wf 委托/代理机制使 `nop` 被引擎接受为合法步骤参与者。
- **useApproval（approvalStatus 轴，DIRECT）浏览器层已覆盖且与 useWorkflow 不同**（`2026-07-09-1249-1`）：PO/Receive/Invoice、SO/Delivery/Invoice 的 `submitForApproval`→`approve` DIRECT `@BizMutation` 全绿。**注意**：manufacturing WorkOrder（`module-manufacturing/model/app-erp-manufacturing.orm.xml:562` `tagSet="...use-approval"`，**无** `useWorkflow`）经核实属此 DIRECT useApproval 轴（`ErpMfgWorkOrderProcessor.submitForApproval/approve` 为 DIRECT status-flip，无 wf 调用），与 1249-1 已证可达的 PO/SO 同轴——**故 WorkOrder 不在本计划 useWorkflow 范围内**（2004-1 曾将其误标「xwf 审批门控」）。purchase-sales Return / quality Recall 同理经核实非 `useWorkflow="true"`，属 useApproval/approval-support DIRECT 范畴。**本计划严格限定于上述 4 个 `useWorkflow="true"` 实体**。
- **当前 E2E 套件基线**（`docs/testing/e2e-runbook.md`）：全套件绿色，无 `tests/e2e/approval/` 目录。业务动作层（business-actions/，6 代表域 DIRECT 状态机）+ 跨域编排层（orchestration/，P2P/O2C 链 + 财务侧红冲）均已落地，仅 useWorkflow 审批轴为唯一浏览器层缺口。
- **剩余差距**：useWorkflow 审批轴浏览器层可达性未经系统性探索，`2026-07-09-1249-1` Deferred「Payment/Receipt xwf」与 `2004-2` Deferred「Payment-Receipt xwf 反向」指向同一未解阻塞，缺一份权威裁决。

## Goals

- **Phase 1 裁决 useWorkflow 审批轴浏览器层可行性**：基于已定位的 `user:$0` 根因（引擎 sysUser 兜底，`.xwf` submit step 无 assignment），探索使 `nop` 浏览器用户被 nop-entropy 工作流引擎接受为合法步骤参与者的可行路径（wf 委托/代理机制 / nop 用户身份映射 / 引擎配置），产出可执行/不可执行裁决。
- **若可行（Phase 2）**：覆盖 useWorkflow 审批轴代表性实体——finance Payment（资金收付浏览器层 E2E）+ assets Disposal（或 hr Salary，证明范式跨域可复用），证明 xwf 浏览器层范式。
- **Phase 3（无论可行与否）**：将指向同一阻塞的 Deferred 项收敛为单一权威裁决记录（`docs/testing/e2e-runbook.md` + 本计划 Closure）——可行则 RELEASE 各 successor Deferred 并登记覆盖证据；不可行则记录精确阻塞根因（引擎 sysUser 兜底 + 无浏览器层委托出口）+ 触发条件，消除散乱。

## Non-Goals

- useApproval（approvalStatus 轴，DIRECT）浏览器层覆盖——`2026-07-09-1249-1` 已覆盖 PO/Receive/Invoice/SO/Delivery/Invoice。**manufacturing WorkOrder / purchase-sales Return / quality Recall 经 ORM 核实均非 `useWorkflow="true"`**，属此 DIRECT useApproval 轴（2004-1 曾将 WorkOrder 误标 xwf），归独立 DIRECT 覆盖 successor，不在此计划。
- 全 4 useWorkflow 实体浏览器层覆盖——本计划覆盖 Payment + Disposal/Salary 两代表性实体证明范式，另一（Receipt 同 Payment 同型 / 未选中的 Disposal/Salary）归 successor（触发条件：本计划证明范式可复用时）。
- 修改 nop-entropy 平台工作流引擎源码——平台级修改超本项目范围；若唯一出路是平台修改，裁决为「不可行」并记录。
- 审批工作流前端 UI（AMIS 审批按钮/审批意见框交互）——本计划经 GraphQL 调 `@BizMutation`（与既有 business-actions/orchestration spec 同范式），AMIS 按钮→action 端到端归 successor。
- 业财过账凭证精确数值断言——Payment 过账产物归 finance 数值断言层 successor（同 1249-1 Deferred）。

## Task Route

- Type: `verification or audit work`（浏览器层端到端可行性探索与验证，纯消费侧测试新增 + 平台机制探索，零生产业务代码/契约/ORM 模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件运行手册）、`docs/architecture/approval-framework.md`（审批框架 §按单据类型配置 WORKFLOW）、4 个 `.xwf` 定义（payment/receipt/asset-disposal/salary-approval）、`../nop-entropy/docs-for-ai/`（工作流引擎机制，Phase 1 Explore 关键参考）
- Skill Selection Basis: `nop-testing`（E2E 套件 @BizMutation 经 GraphQL 驱动范式，0814-2/1249-1/2004-1 已验证）；`nop-backend-dev`（确认 useWorkflow 实体 `submitForApproval`/`approve` 签名 + wf 步骤参与者机制，Phase 1 Explore 关键）；`nop-debugging`（Phase 1 探索 sysUser 兜底 + wf 委托机制根因）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests` → `app-erp-all/target/quarkus-app/quarkus-run.jar`
- Node.js + `npm install`（Playwright 依赖已就绪）
- fresh-DB 重置机制不变（`rm -f db/erp.mv.db`，种子非幂等）
- 复用既有 Playwright 基础设施（webServer fresh-DB + 91 CSV 种子 + 序列推进 + auth fixtures）
- 无新增端口/环境变量/密钥/外部服务（Phase 1 裁决聚焦 nop-entropy 工作流引擎机制；`user:$0` 根因经核实为引擎 sysUser 兜底，非本仓种子/配置可调，故不预期 `_init-data/` 种子调整）

## Execution Plan

### Phase 1 - useWorkflow 审批轴浏览器层可行性裁决

Status: completed
Targets: （探查）nop-entropy 工作流引擎 sysUser 兜底 + wf 委托机制；4 个 `.xwf` 定义；`tests/e2e/approval/`（临时探针 spec，裁决后保留或删除由 Decision 定）
Skill: `nop-backend-dev | nop-debugging`

- Item Types: `Explore | Decision`
- Prereqs: 1249-1 Phase 3 探针证据（user:$0 阻塞）+ 已读 `.xwf` 证据（submit step 无 assignment + sysUser 兜底）+ 既有 auth fixtures

- [x] `Explore`：核实 nop-entropy 工作流引擎 sysUser 兜底解析逻辑——读 `ApprovalFlowHelper.start` + `IWorkflow`/`XwfRuntime` 步骤参与者 resolver，确认 `submit` 自动 complete 时如何确定「操作人」（sysUser(0) 兜底的具体代码路径），以及 `nop` 浏览器用户身份（经 auth fixtures 注入）如何被引擎接收。产出：sysUser 兜底的确切机制 + `nop` 用户是否可被映射为合法操作人。
  - Skill: `nop-backend-dev | nop-debugging`
  - **证据**：`WorkflowEngineImpl.start:218` 调 `getActors(startStep.assignment=null, ...)` → `WfActorAssignSupport.getActors:194`（assignment==null 时取 selectedActors，空）→ `WorkflowEngineImpl.newSteps:274-283`：actors 空 + assignment==null 时 **fallback `wfRt.getWf().getManagerActor()`（payment-approval/v1.xwf 无 `<auth>` → null）→ `wfRt.getSysUser()`**（`WfRuntime.getSysUser:229` → `resolveUser(IWfActor.SYS_USER_ID="0")`）。故 submit step actor=sysUser(0)，`newStepForActor:358-369` 因 actorType=USER → `getOwner:278` 返回 actor 本身 → stepRecord.ownerId="0"。随后 `ApprovalFlowHelper.start:20` 调 `getLatestStartStep().invokeAction(COMPLETE, ctx)` → `WorkflowStepImpl.invokeAction:228` 先 `allowCallByUser` → `WorkflowEngineImpl.allowCallByUser:1053-1072`：owner=sysUser(0)，`owner.getActorId()("0") != ctx.getUserId()(nop uuid)` → 落 `canBeDelegatedBy`。**nop 用户身份经 `-Dnop.auth.service-public=true` 旁路 auth 但不改变 ctx.getUserId()（仍为 nop uuid），引擎按 sysUser(0) 解析 owner，nop 不匹配 → submit 被拒**（与 1249-1 探针证据一致）。
- [x] `Explore`：探查 nop-entropy 是否提供浏览器层 wf 委托/代理机制使 `nop` 被接受为合法步骤参与者——(a) wf 委托 API（用户委托 wf 权限）、(b) nop 用户身份映射/伪装机制（浏览器层等效 `setUserId`）、(c) `.xwf` 步骤 `<assignment>` 放宽（如 `actorType` 通配/角色经种子配置）。每条路径给可执行/不可执行 + 证据（读引擎源 + 平台 `docs-for-ai/`）。
  - Skill: `nop-backend-dev | nop-debugging`
  - **证据**：
    - **(a) wf 委托 API（最可行候选）——经实测不可执行**：`WfActorAssignSupport.canBeDelegatedBy:71-77` → `IUserDelegateService.canDelegate(userId, ownerId, scope)` → `DaoUserDelegateService.getDelegateOwnerIds:45-58`（读 `NopAuthUser.substitutionMappings` 即 `NopAuthUserSubstitution` 行）。机制存在且 `DaoUserDelegateService` 已在 `auth-dao.beans.xml` 注册。**但经临时探针 spec 实测阻断**：建 `NopAuthUserSubstitution(userId=nopUuid, substitutedUserId="0")` 前须先物化 sysUser(0) 为真实 `nop_auth_user` 行（FK 校验），而 `NopAuthUser.userId` 列 `tagSet="seq"`（nop-auth/model/nop-auth.orm.xml:38-39）→ `__save` 时 seq 生成器**覆盖显式 userId="0"**，实测创建的 SYS 用户 userId="3d0538b1653c42c0bc40cb158614742c"（UUID），`findFirst(userId="0")` 返回 null → 委托 FK「类型为[用户]，id为[0]的记录不存在」失败。**sysUser(0) 是引擎虚拟用户（SYS_USER_ID 常量），浏览器层 `__save` 无法物化为真实可引用行**。后端测试经 `setUserId("0")`+Java IWorkflow 直调绕过（1249-1 已证），浏览器层无此出口。
    - **(b) nop 用户身份映射/伪装（浏览器层等效 setUserId）——不存在**：读 nop-entropy 全量 wf/auth 模块，浏览器层（GraphQL `/graphql`）无等效 `setUserId` 的用户身份注入/伪装 API。`-Dnop.auth.service-public=true` 仅旁路 auth 校验，不改 `ctx.getUserId()`（仍为登录用户 nop uuid）。后端 `ContextProvider.getOrCreateContext().setUserId("0")` 是线程局部出口，浏览器层 GraphQL 请求无此注入点。
    - **(c) `.xwf` submit step `<assignment>` 放宽——属生产行为变更，非浏览器层可行路径**：若改 payment-approval/v1.xwf submit step 增 `<assignment><actor actorType="all"/></assignment>`，则 start step actor=all、owner=null → `allowCallByUser` 走 `actor.containsUser(nopUuid)`（`IWfActor.containsUser:75` ACTOR_TYPE_ALL → true）放行。**但此为生产工作流定义变更**（削弱 submit 起始步骤的 sysUser 自动 complete 语义，改变审批门控行为），超出「浏览器层可行性」范围（Non-Goal: 修改平台工作流引擎行为；`.xwf` 虽在本仓但属生产审批契约）。
- [x] `Explore`：临时探针 spec 验证最可行路径——经 browser-layer 真实调 Payment `submitForApproval`，配候选可行路径，观察是否越过 user:$0 拦截到达 wf 推进/APPROVED。探针 spec 裁决后保留（若可行，纳入 Phase 2 正式覆盖）或删除（若不可行，避免污染套件）。
  - Skill: `nop-testing`
  - **证据**：临时探针 spec `tests/e2e/approval/_probe.spec.ts` 经 fresh-DB 实测路径 (a)（委托）：login→取 nop uuid→建 NopAuthUser(userId="0")→建 substitution(nop→0)→建 Payment→submitForApproval。**实测阻断在 substitution 建**（sysUser(0) 物化失败，seq 覆盖 "0" 为 UUID）。委托路径不可达，未越过 user:$0 拦截。探针 spec 裁决「不可行」后已**删除**（避免污染套件），server/DB 已清理。
- [x] `Decision`：记录 useWorkflow 审批轴浏览器层可行性裁决——选择（可行 / 不可行）、考虑的替代方案（各 Explore 路径）、残留风险（若可行：路径是否依赖平台配置/种子改动影响既有套件；若不可行：是否需平台支持）。裁决理由写入计划并引用 Explore 证据 + `.xwf`/引擎源证据。
  - Skill: none
  - **裁决：不可行（NOT FEASIBLE）**。三条候选路径均阻断：(a) 委托机制存在但 sysUser(0) 无法经浏览器层物化为真实可引用用户（seq PK 覆盖）；(b) 浏览器层无用户身份注入/伪装 API；(c) `.xwf` 放宽属生产审批契约变更（非浏览器层可行路径）。**残留风险**：裁决依赖 nop-entropy 当前版本（seq PK 策略 + 无浏览器层身份映射 API + sysUser 兜底）；若平台后续支持浏览器层测试用户身份映射 / 委托免 sysUser 物化 / sysUser 种子物化，需重评（见 Deferred「useWorkflow 审批轴浏览器层覆盖（若 Phase 1 裁决不可行）」触发条件）。

Exit Criteria:

- [x] sysUser 兜底解析逻辑已核实，`nop` 用户身份接收机制有结论（sysUser 兜底经 `newSteps:274-283` fallback，nop uuid 不匹配 owner="0"，无浏览器层身份映射出口）
- [x] 至少 2 条 wf 委托/代理可行路径经 Explore 评估，每条附可执行/不可执行证据（3 条路径评估：委托阻断于 seq PK / 身份映射不存在 / .xwf 放宽属生产变更）
- [x] 可行性 Decision 已记录（不可行 / NOT FEASIBLE），理由引用 Explore + `.xwf`/引擎源证据

### Phase 2 - useWorkflow 审批轴代表性实体覆盖（条件性，Phase 1 裁决为「可行」时执行）

Status: completed（条件跳过 — Phase 1 裁决「不可行」，执行条件不满足；Exit Criteria 移入下方 Deferred「useWorkflow 审批轴浏览器层覆盖（若 Phase 1 裁决不可行）」）
Targets: `tests/e2e/approval/payment.action.spec.ts`、`tests/e2e/approval/asset-disposal.action.spec.ts`（或 `salary.action.spec.ts`，二选一证明跨域可复用；新建）；`tests/e2e/approval/_helper.ts`（xwf 浏览器层范式 helper）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 裁决「可行」+ 可行路径已验证（探针 spec wf 推进/APPROVED）

- [x] `Add`：`approval/_helper.ts`——xwf 浏览器层审批 helper（封装 Phase 1 裁决的可行路径：登录/委托/身份映射 + `submitForApproval`→`approve` 编排），复用 business-actions `_helper.ts` 三原语（createViaSave/callMutation/verifyState）。
  - Skill: `nop-testing`
  - **条件跳过**：Phase 1 裁决不可行（委托阻断于 sysUser(0) seq PK 物化 / 无浏览器层身份映射 / .xwf 放宽属生产变更），无可行路径可封装。
- [x] `Add`：`payment.action.spec.ts`——purchase Payment useWorkflow 审批轴 E2E：建 Payment → `submitForApproval`（越过 user:$0，wf 推进）→ `approve`（→APPROVED，wf listener 回调 Processor approve action）；状态翻转经 `__get` 独立断言。资金收付浏览器层可达性证明。
  - Skill: `nop-testing`
  - **条件跳过**：同上。
- [x] `Add`：`asset-disposal.action.spec.ts`（或 `salary.action.spec.ts`）——第二个 useWorkflow 实体审批轴 E2E（跨域证明范式可复用，区别于 Payment 的另一 `.xwf` 定义）；状态翻转经 `__get` 独立断言。
  - Skill: `nop-testing`
  - **条件跳过**：同上。
- [x] `Proof`：approval 套件全绿 + 既有套件无回归。
  - 验证命令：`npx playwright test tests/e2e/approval/ --workers=1`（局部）+ Closure Gates 全套件
  - Skill: `nop-testing`
  - **条件跳过**：同上。

Exit Criteria:

- [x] Payment useWorkflow 审批轴浏览器层 E2E 全绿（submit→wf 推进→approve，状态翻转经 `__get` 断言）— **条件跳过**（Phase 1 裁决不可行，Exit Criteria 移入 Deferred）
- [x] 第二个 useWorkflow 实体（Disposal/Salary）审批轴浏览器层 E2E 全绿，证明范式跨域可复用 — **条件跳过**（同上）

> **若 Phase 1 裁决「不可行」**：Phase 2 跳过，本阶段 Exit Criteria 移入 Deferred But Adjudicated（带触发条件=平台工作流引擎支持浏览器层测试用户/委托时），Phase 3 仍执行。

### Phase 3 - Deferred 收敛与文档对齐

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/plans/2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`、`docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md`、`docs/plans/2026-07-09-2004-2-reverse-voucher-e2e.md`（既有 Deferred RELEASE 标记）、`docs/logs/2026/07-09.md`、`docs/backlog/README.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 裁决（Phase 2 无论执行与否）

- [x] `Add`：`e2e-runbook.md` 增「useWorkflow 审批轴浏览器层」段——可行性裁决结论 + sysUser 兜底机制说明 + `.xwf` submit step 无 assignment 证据 + （若可行）覆盖范式 + （若不可行）精确阻塞根因 + successor 触发条件；套件计数更新为实测值。
  - **完成**：新增「useWorkflow 审批轴浏览器层（xwf）— 可行性裁决：不可行」段（sysUser 兜底机制 root cause `WorkflowEngineImpl.newSteps:274-283` + 3 路径评估 + 后续步骤 `WorkflowService__invokeAction` 可达性 + successor 触发条件）；既有 Payment/Receipt xwf Deferred 段更新为权威裁决不可行；套件计数 133 不变（纯探索无新 spec）。
- [x] `Add`：将指向同一阻塞的 Deferred 项收敛——可行则各 successor Deferred（1249-1 Payment/Receipt xwf + 2004-2 Payment-Receipt xwf 反向 useWorkflow 子集）标 RELEASED（本计划 Closure 登记）；不可行则在各 plan Deferred 旁补「经 2330-1 权威裁决不可行，触发条件=...」交叉引用，消除散乱。
  - **完成**（裁决不可行路径）：1249-1 Deferred「Payment/Receipt xwf」+ 2004-2 Deferred「Payment/Receipt xwf 反向」+「域审批轴 reverseApprove」useWorkflow 子集 + 2004-1 Deferred「审批工作流（xwf）域业务动作」均补「经 2330-1 权威裁决不可行」交叉引用 + 重评触发条件（平台支持浏览器层身份映射/委托免 sysUser 物化/sysUser 种子物化时）。
- [x] `Add`：每日日志 + backlog + known-good-baselines 更新。
  - **完成**：`docs/logs/2026/07-10.md` +2330-1 条目（Phase 1/2/3 摘要 + 验证状态）；`docs/backlog/README.md` +2330-1 ✅ done 行；`docs/testing/known-good-baselines.md` +2026-07-10 基线行。

Exit Criteria:

- [x] e2e-runbook 含 useWorkflow 审批轴浏览器层段（裁决结论 + sysUser 兜底机制 + `.xwf` 证据 + 范式/根因 + 套件计数实测值）
- [x] 指向同一阻塞的 Deferred 项收敛为单一权威裁决（RELEASED 标记或交叉引用，无散乱）— 裁决不可行路径：3 plan Deferred 补交叉引用

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0b8759173ffeuP9IhgitX8yOgl`，独立 general 子代理，新会话冷重播无执行者上下文) — 1 BLOCKER + 1 MAJOR + 3 MINOR，全部经 live repo 核实：
  - B1（BLOCKER，baseline dishonesty）：声称「wf 定义文件位置未定位 / `find -name "*.wf.xml"` 零命中」为假——wf 定义以 `.xwf` 扩展名存在（payment/receipt/asset-disposal/salary-approval），`payment-approval/v1.xwf` 描述段明示 submit step 无 assignment + sysUser 兜底。
  - M1（MAJOR）：WorkOrder 经 ORM 核实为 `use-approval` DIRECT（无 `useWorkflow="true"`），误归 useWorkflow 范围；与 Non-Goal「useApproval 轴已覆盖」自相矛盾。实际 `useWorkflow="true"` 实体仅 Payment/Receipt/Disposal/Salary 4 个。
  - m1/m2/m3：infra prereq 条件措辞 / Draft Review 占位 / 条件分支处理（m3 通过）。
  - **已修复**：B1——baseline 重写为 `.xwf` 已定位 + sysUser 兜底根因 + 后端 setUserId/Java IWorkflow 出口不可用于浏览器层；Phase 1 Explore 收窄至引擎 sysUser 解析 + wf 委托机制。M1——WorkOrder 移出范围，useWorkflow 域经 ORM 核实纠正为 Payment/Receipt/Disposal/Salary 4 个，Phase 2 改为 Payment + Disposal/Salary 二选一；Non-Goal/Deferred 补 useApproval DIRECT 轴 successor。m1——infra prereq 措辞修正。
- Independent draft review iteration 2: `accept` (`ses_0b86badd2ffepWtbp5rYluVssw`，独立 general 子代理，新会话冷重播无执行者上下文) — B1/M1 修复经 live repo 逐项核实 CLEAN（4 `.xwf` + sysUser 兜底 + 4 `useWorkflow="true"` ORM 标签 + WorkOrder 无 `useWorkflow` + Phase 1 Explore 收窄 + 无遗漏 useWorkflow 实体）。发现 1 MAJOR residue：line 14 stale 短语「与 manufacturing WorkOrder 执行动作」与 M1 修复自相矛盾（rule 11）。**已修复**：line 14 改为「聚焦 useWorkflow 轴（Payment/Receipt/Disposal/Salary submit 被 wf 步骤参与者直接拦截）」。审查者裁定此项修复后 verdict 应为 accept。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面）。若 Phase 2 执行（裁决可行），结束前运行完整 E2E 套件（含新增 approval spec + 既有 spec）+ 后端构建（确认 E2E 未污染后端）。若 Phase 2 跳过（裁决不可行），结束前运行既有 E2E 套件确认无回归（本计划仅探针 + 文档，探针已删除）+ 后端构建。

- [x] 范围内行为完成（Phase 1 可行性裁决有结论：不可行；Phase 2 条件跳过经 Phase 1 Decision 计划内裁决；Phase 3 Deferred 收敛完成）
- [x] 相关文档对齐（e2e-runbook useWorkflow 段 + 3 plan Deferred 收敛交叉引用 + 日志/backlog/baseline）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `npx playwright test tests/e2e/orchestration/`（4 passed，0 回归，wf/approval 邻近域）。**注**：本计划纯探索+文档（探针 spec 裁决后已删除），git 确认零 test/code 变更，全套件 133 测试与上一基线（2004-2）逐字节一致，故以邻近域 orchestration 回归 + 后端构建为验证面。
- [x] 无范围内项目降级为 deferred/follow-up（Phase 2 裁决不可行移入 Deferred 属 Phase 1 Decision 的计划内裁决，附触发条件，非范围内缺陷降级）
- [x] 独立草案审查已完成并记录（Draft Review iteration 1 needs revision + iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中（Phase 1 Decision + Explore 证据引用引擎源行号 + 探针实测 + e2e-runbook 段 + Deferred 交叉引用）

## Deferred But Adjudicated

### 全 4 useWorkflow 实体浏览器层覆盖（Receipt / 未选中的 Disposal/Salary）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖 Payment + 第二实体（Disposal/Salary）证明范式（若 Phase 1 裁决可行）。Receipt 与 Payment 同型（receipt-approval/v1.xwf 镜像 payment-approval），未选中的 Disposal/Salary 同 `.xwf` 范式，边际收益递减。
- Successor Required: `yes`
- Trigger Condition: 当本计划证明 xwf 浏览器层范式可复用（Phase 2 全绿）时，按实体推进。

### useApproval DIRECT 轴剩余域浏览器层覆盖（WorkOrder / Return / Recall）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: manufacturing WorkOrder / purchase-sales Return / quality Recall 经 ORM 核实非 `useWorkflow="true"`，属 useApproval DIRECT 轴（与 1249-1 已证可达的 PO/SO 同轴）。本计划严格限定 useWorkflow 轴。这些 DIRECT 域归独立 DIRECT 覆盖 successor。
- Successor Required: `yes`
- Trigger Condition: 当需按域推进 DIRECT useApproval 业务动作浏览器层覆盖时。

### useWorkflow 审批轴浏览器层覆盖（若 Phase 1 裁决不可行）

- Classification: `out-of-scope improvement`（受平台约束）
- Why Not Blocking Closure: 若 Phase 1 权威裁决不可行，useWorkflow 审批轴浏览器层需 nop-entropy 平台工作流引擎支持（浏览器层测试用户身份映射 / wf 委托 / sysUser 兜底放宽），超本项目范围。
- Successor Required: `yes`
- Trigger Condition: 当 nop-entropy 平台工作流引擎支持浏览器层测试用户/委托机制时（含 Payment/Receipt/Disposal/Salary 审批 + Payment-Receipt xwf 反向）。

## Closure

Status Note: 已完成。Phase 1 权威裁决 useWorkflow 审批轴（xwf）浏览器层**不可行（NOT FEASIBLE）**——sysUser 兜底根因（`WorkflowEngineImpl.newSteps:274-283` fallback sysUser(0) 作 submit step owner）经引擎源 + 临时探针 spec fresh-DB 实测定位；3 条候选路径均阻断（① wf 委托 `NopAuthUserSubstitution` nop→0 存在但 sysUser(0) 无法经浏览器层 `__save` 物化——`NopAuthUser.userId` `tagSet="seq"` 覆盖显式 "0" 为 UUID；② 浏览器层无用户身份映射 API；③ `.xwf` submit step 放宽属生产审批契约变更）。Phase 2 条件跳过（裁决不可行，Exit Criteria 移入 Deferred But Adjudicated）。Phase 3 收敛 1249-1/2004-2/2004-1 同一 xwf Deferred 为单一权威裁决（e2e-runbook「useWorkflow 审批轴浏览器层」段 + 各 plan Deferred 交叉引用）。纯探索+文档，零 ORM/契约/Java/spec 变更（探针 spec 裁决后已删除）。验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `npx playwright test tests/e2e/orchestration/`（4 passed 0 回归；git 确认零 test/code 变更，全套件 133 与上一基线一致）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计 PASS（`ses_0b829cf33ffe5CKX0wjJmj3Q7c`，独立 general 子代理，新会话冷重播无执行者上下文）— 7 项检查全 PASS：(1) Phase 1 NOT FEASIBLE 裁决 + 引擎源证据 live 验真（`WorkflowEngineImpl.newSteps:268-283` sysUser fallback / `WfActorAssignSupport.canBeDelegatedBy:71-77` / `nop-auth.orm.xml:38-39` userId tagSet="seq" 全匹配）；(2) 探针 spec 已删除无残留；(3) 全 Phase items [x] + Status completed；(4) 3 plan Deferred 交叉引用存在；(5) e2e-runbook/log/backlog/baseline 文档更新存在；(6) git 仅 8 docs/*.md 变更零 code；(7) 文本一致性 PASS。无 BLOCKER/MAJOR。

Follow-up:

- useWorkflow 审批轴浏览器层覆盖 successor（Payment/Receipt/Disposal/Salary 审批 + 反向，触发条件=nop-entropy 平台支持浏览器层测试用户身份映射 / 委托免 sysUser(0) 物化 / sysUser 种子物化时）— 见 Deferred But Adjudicated「useWorkflow 审批轴浏览器层覆盖（若 Phase 1 裁决不可行）」
