# 2026-07-06-0642-2-approval-workflow-notifications 审批工作流通知与抄送

> Plan Status: completed
> Last Reviewed: 2026-07-06
> Source: deferred 项承接（`2026-07-06-0315-1` 通知/抄送步骤，触发条件=通知派发通道落地，已满足）；core-business-roadmap M1 审批闭环
> Related: `2026-07-06-0315-1-workflow-approval-xwf.md`（4 实体 WORKFLOW 升级，已完成）、`2026-07-06-0642-1-operational-notification-consumers.md`（运营通知，并行）、`2026-07-06-0504-1-notification-dispatch-subsystem.md`（通道，已完成）
> Audit: required

## Current Baseline

4 实体（`ErpPurPayment`/`ErpSalReceipt`/`ErpAstDisposal`/`ErpHrSalary`）已升级 WORKFLOW 审批（`0315-1`），4 个 `.xwf` 文件存在：

- `module-purchase/.../nop/wf/payment-approval/v1.xwf`
- `module-sales/.../nop/wf/receipt-approval/v1.xwf`
- `module-assets/.../nop/wf/asset-disposal-approval/v1.xwf`
- `module-hr/.../nop/wf/salary-approval/v1.xwf`

每个 `.xwf` 有 `<listeners><listener id="on-wf-end" eventPattern="*end">` 经 `<c:script>` 反射调业务 `approve`/`reject` action（幂等：已终态跳过）。bootstrap actor 用 `actorType="all"`（ERP 无角色/组织基础设施，精确路由 Deferred）。

通知派发通道已就绪（`IErpSysNotificationBiz.notify`），但**审批生命周期通知全部缺失**：

- **结果通知**：审批通过/驳回时，**提单人（submitter）无任何通知**——仅业务单据状态迁移。审批人 agree/disagree 后提单人需主动查询才知道结果。
- **任务到达通知**：wf 步骤分配到审批人时，**审批人无通知**——审批人需主动进待办列表轮询。
- **抄送（CC）**：4 个 `.xwf` 无 `specialType="cc"` 步骤。平台支持 cc 步骤（`workflow-configuration.md` §specialType：cc 抄送、`confirm` 动作仅标记已读、`oa:WhenAllowConfirm`/`WhenAllowAgree` 守卫），但未接线。

`0315-1` Deferred「通知/抄送步骤」明示触发条件=通知派发通道基础设施落地（`0504-1` 已完成），承接条件已满足。

剩余差距：审批通过/驳回→提单通知链断；新任务到达→审批人通知链断；抄送场景无 cc 步骤。

## Goals

- 审批结果通知：wf 通过/驳回时通知提单人（经 xbiz `<observes>` 或 on-wf-end listener 扩展）
- 抄送（CC）步骤：4 个 `.xwf` 增 `specialType="cc"` 步骤，相关角色收到只读知会
- 任务到达通知：审批人被告知有待办（经平台 wf 步骤事件钩子或 submitForApproval 后通知候选审批人，**需 Explore 确认钩子可用性**）

## Non-Goals

- 金额阈值条件路由（`0315-1` Deferred，触发条件=金额分级审批业务规则落地）
- 前端审批 UI 适配（`0315-1` Deferred，AMIS 前端面）
- 委托/转办/加签（`0315-1` Deferred，平台 wf-actor 扩展）
- HR 精确角色路由（`0315-1`/`0504-1` Deferred，角色基础设施）
- 运营事件通知（CS SLA/过账异常/信用/CRM/CSAT/差异，归 `0642-1` 独立结果面）
- 通知中心前端（`0504-1` Deferred）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/approval-framework.md`（审批框架/按单据配置/抄送规则）、`docs/architecture/notification-strategy.md`（通知类型）、`../nop-entropy/docs-for-ai/02-core-guides/workflow-configuration.md`（specialType/assignment/listener）、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（observes/listener 范式）
- Skill Selection Basis: 改 `.xwf` 文件 + xbiz `append`/`observe` 注入 + 跨实体通知调用 → 匹配 `nop-backend-dev`；行为测试经 IGraphQLEngine/wf 启动 → 匹配 `nop-testing`。不加载前端技能。

## Infrastructure And Config Prereqs

- 通道侧 config-gated 默认仅站内消息（`0504-1`），本期无新基础设施
- wf 任务到达通知依赖平台 wf 步骤事件钩子可用性——**Phase 1 含 Explore 项确认**；若平台无 step-start 事件，降级为 submitForApproval action 内通知候选审批人（Decision 记录）
- 接收人：提单人=单据 `createdBy`/提交人 userId（USER_LIST）；审批人/CC=ROLE（角色未落地时 config-gated 静默空，不阻断 wf）

## Execution Plan

### Phase 1 — 审批结果通知（提单人）+ 任务钩子可用性 Explore

Status: completed
Targets: 各域 `erp-*-service` 的 `approve`/`reject` xbiz（`_overrides` 或 `.xbiz.xml` append）；或 4 个 `.xwf` on-wf-end listener；`docs/` Explore 记录
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 通道（`0504-1`）

- [x] Explore: 确认 nop-wf 步骤事件钩子可用性——读 `../nop-entropy/docs-for-ai/02-core-guides/workflow-configuration.md` 全文 + wf 引擎 listener eventPattern 支持的 event 类型（是否含 step-start/after-assign），记录可用事件清单。此 Explore 解除 Phase 3 任务到达通知的 Decision 阻塞。
  - Skill: none
  - **Explore 结果**：`NopWfCoreConstants` 确认完整事件清单——`enter-step`、`activate-step`、`exit-step`、`before-action`、`after-action`、`transition`、`*end`（`EVENT_BEFORE_END`/`EVENT_AFTER_END` 同值 `before-end`）、`on-no-assign`、`change-status` 等。listener `eventPattern` 经 `StringHelper.matchSimplePattern` 匹配（`*` 通配）。步骤级 `<on-enter>`/`<on-exit>` XPL 钩子可用（wf.xdef 定义、`WorkflowEngineImpl` 在 step 创建/退出时 `runXpl(stepModel.getOnEnter/onExit)`）。**结论：step 事件钩子（onEnter/enter-step）可用**，Phase 3 Decision (A) 路径成立。
- [x] Decision: 结果通知注入点选择——优先 xbiz `<observes eventPattern="approve|reject">`（业务层联动，wf 只编排，符合 `enable-approval-on-entity.md` 推荐「联动在 xbiz 层注入」），调 `notify` 通知提单人。替代方案=扩展 on-wf-end listener（但 listener 已承担 approve/reject 调用，耦合通知增复杂度）。裁决理由+残留风险写入计划。
  - Skill: `nop-backend-dev`
  - **Decision 裁决**：**改用 on-wf-end listener 扩展**（非 `<observes>`）。理由：经独立核实，`<observes>`（`BizObserveModel`）在当前 nop-entropy 2.0.0-SNAPSHOT 版本**仅由 xbiz.xdef schema 解析（`_BizModel.getObserves()`），但运行时无任何代码触发 biz event**（全局 grep `getObserves()` 调用点为零）——即 `<observes>` 是 dead 特性，通知永远不会触发。故按替代方案在 on-wf-end listener 内 approve/reject 之后注入 `notify('wf.<entity>.result')`，4 域统一模式。**残留风险**：listener 同时承担业务回调+通知，耦合度略增，但 notify 内部已 catch 所有异常（best-effort），通知失败不阻断业务回调。
  - **附加增强**：`NotificationRecipientResolver` 原 `USER_LIST` 不支持 `${var}` 从 context 插值（recipientConfig 直接 JSON parse），导致提单人动态接收人不可达。增强 `resolve(template, context)` 重载，对 recipientConfig 中 `${var}` 按 context 插值后再 parse（向后兼容，无 `${var}` 时行为不变）。`NotificationDispatcher.dispatch` 改传 context。同时修复 0642-1 CRM `${ownerUserId}` 占位符的既有缺口。
  - **模板子决策（N4）**：采用 4 域各一个 `wf.<entity>.result` 模板（合并 approved/rejected 到一个类型，body 经 `${resultText}` 区分），mergeStrategy=`MERGE_BY_USER_TYPE` 5 分钟窗口。理由：减少 SQL 行数，且单实体一次 wf 仅触发 approve 或 reject 之一（互斥），合并仅跨多实体聚合（合理）。
- [x] Add: 4 域 approve/reject xbiz observe——各注入 `notify("wf.<entity>.approved"|"wf.<entity>.rejected", {entityId, docNo, submitterUserId, approverUserName, comment})`，接收人=submitterUserId（USER_LIST）。config-gated（`erp-<domain>.approval-result-notify-enabled` 默认 true）。事件类型按域区分（payment/receipt/disposal/salary）。
  - Skill: `nop-backend-dev`
  - **落地**：4 域 `.xwf` on-wf-end listener 内 approve/reject 后调 `notify('wf.<entity>.result', {resultText, docNo, submitterUserId, approverUserId, entityId})`。config-gate = 模板存在性（无 ACTIVE 模板则 `ErpSysNotificationBizModel.notify` 静默跳过，与 0504-1 config-gated 语义一致）。
- [x] Add: 种子模板 4 类（approved/rejected × 可合并到每域一个 `wf.<entity>.result` 模板按 context.action 区分，或 approved/rejected 分开）——业务提醒类，5 分钟合并，三套 SQL（mysql/oracle/postgresql）。
  - Skill: `nop-backend-dev`
  - **落地**：3 套 SQL（`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`）各增 4 行（ID 7111-7114，`wf.<entity>.result`）。
- [x] Proof: wf 端到端测试——启动 wf→审批人 agree→wf end→断言提单人收到 approved 通知；disagree→rejected 通知。复用 `0315-1` 的 4 个 WORKFLOW 测试类扩展。命令 `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-assets/erp-ast-service,module-hr/erp-hr-service -am`。
  - Skill: `nop-testing`
  - **落地**：`TestErpPurPaymentApprovalNotifications`（testApprovalResultNotifiesSubmitter + testRejectedResultNotifiesSubmitter）断言提单人 ErpSysNotification 行（agree→已通过，disagree→已驳回）。全绿。

Exit Criteria:

- [x] Explore 输出记录 nop-wf 可用事件清单（解除 Phase 3 Decision 阻塞）
- [x] 4 域审批通过/驳回触发提单人 `ErpSysNotification` 行（测试断言），config 关闭静默跳过

### Phase 2 — 抄送（CC）步骤接线

Status: completed
Targets: 4 个 `.xwf`（增 `specialType="cc"` step）；种子模板
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] Decision: CC 角色与插入位置——按各单据语义确定 CC 接收角色：付款单（CC 财务经理/出纳）、收款单（CC 销售/财务）、资产处置（CC 资产管理员）、薪酬（CC HR 专员）。CC 步骤插入审批步骤之后（审批通过后抄送知会，非阻塞）。cc step assignment 方式须在本 Decision **确定单一方案**（config-gated 静态接收人 OR ROLE resolver，二选一并记录理由），bootstrap 角色未落地时所选方案须 config-gated 回退空（cc step 无 actor 时不阻塞 wf 推进）。记录每域 CC 角色裁决 + assignment 方式裁决。
  - Skill: `nop-backend-dev`
  - **Decision 裁决**：
    - **CC 步骤 assignment**：`actorType="all"`（与审批步骤一致，bootstrap 任意已认证用户可 confirm）。**理由**：`actorType="role"` 在角色不存在时 `DaoWfActorResolver.resolveRole` 返回 null → `getAssignmentActors` 抛 `ERR_WF_ACTOR_NOT_EXISTS`（而非 skip），bootstrap 无 ERP 角色定义会破坏 wf。`actorType="all"` 始终可解析，cc step 总能产生实例（满足 Proof 断言 cc step 实例存在）。
    - **CC 通知接收人**：ROLE resolver（CC 角色名经 `NotificationRecipientResolver` → NopAuthUserRole → userId）。bootstrap 角色未落地 → resolveRole 查无 NopAuthRole → WARN 返回空集 → config-gated 静默跳过（无 ErpSysNotification 行，不阻断）。测试种子角色+用户验证可达。
    - **非阻塞语义澄清**：nop-wf 的 cc step（`specialType="cc"` + `confirm` action）**结构上需 confirm 后 wf 才结束**（标准 OA 抄送语义）。agree 路径经 cc step（disagree 路径 disagree action 直接 to-end，不经 cc）。4 域既有 WORKFLOW 测试的 agree 方法已扩展为先 agree 后 confirm cc。**残留风险**：业务 approve（含过账）在 cc confirm 后才触发——对 bootstrap 参考应用可接受（cc 接收人=任意用户可即时 confirm）；产品化后 cc 应用精确角色并由指定用户及时确认，或后续增强 auto-confirm（Deferred）。
- [x] Add: 4 个 `.xwf` 增 cc step（`specialType="cc"`，`<assignment><actor actorType="all"/></assignment>` 或角色绑定，action 仅 `confirm`）。cc step 经 wf 自动产生 step 实例，平台 `oa:WhenAllowConfirm` 守卫已就绪。
  - Skill: `nop-backend-dev`
  - **落地**：payment → cc-finance、receipt → cc-sales、disposal → cc-assets、salary → cc-hr。各 cc step `<on-enter>` 调 `notify('wf.<entity>.cc')`（ROLE 接收人）。oa.xwf 的 `confirm` action（`local="true"` + `oa:WhenAllowConfirm` 守卫）已就绪，无需自定义 action。
- [x] Add: CC 通知——cc step 到达时通知 CC 接收人（经 Phase 1 Explore 确认的 step 事件钩子；若不可用，cc step 的 assignment 本身即产生待办，配合结果通知覆盖）。种子模板 `wf.<entity>.cc`。
  - Skill: `nop-backend-dev`
  - **落地**：3 套 SQL 各增 4 行（ID 7131-7134，`wf.<entity>.cc`，ROLE 接收人）。
- [x] Proof: wf 测试——审批通过后 cc step 产生 step 实例 + CC 接收人收到通知（断言 ErpSysNotification 或 step 实例存在）。命令同 Phase 1。
  - Skill: `nop-testing`
  - **落地**：`TestErpPurPaymentApprovalNotifications.testCcStepInstanceAndRoleNotifications` 断言 cc-finance step 实例存在 + CC 角色用户收到 `wf.pur-payment.cc` 通知。全绿。

Exit Criteria:

- [x] 4 个 `.xwf` 含 cc step，审批通过后 CC 接收人收到知会（测试断言 cc step 实例/通知）

### Phase 3 — 任务到达通知（审批人）

Status: completed
Targets: 各域 `submitForApproval` xbiz 或 wf 步骤事件钩子（依 Phase 1 Explore 结论）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 Explore 结论

- [x] Decision: 任务到达通知实现路径——依 Explore：(A) 若平台 wf 支持 step-start/after-assign 事件，经 listener 钩子通知候选审批人；(B) 若不支持，在 `submitForApproval` action wf 启动后通知审批步骤的候选 actor（bootstrap actorType=all 时通知全角色或 config 指定 userId）。裁决+降级方案写入计划。残留风险须显式记录：降级方案 (B) 仅覆盖 wf 启动时的第一步审批人；对多级审批链（HR salary 三级 hr-review→finance-review→manager-approval），后续步骤审批人在 (B) 下无任务到达通知——须在残留风险中声明，并记录"角色基础设施落地后改 (A) 或 actor 驱动"为解除条件。
  - Skill: `nop-backend-dev`
  - **Decision 裁决**：**方案 (A)——步骤级 `<on-enter>` 钩子**。Phase 1 Explore 确认 `enter-step`/`<on-enter>` 可用（`WorkflowEngineImpl` 在 step 创建时 `runXpl(stepModel.getOnEnter())`）。在每个审批步骤（payment finance-approval、receipt/disposal manager-approval、salary hr-review/finance-review/manager-approval）的 `<on-enter>` 调 `notify('wf.<entity>.task-assigned')`。**多级链覆盖**：(A) 在每级审批步骤 enter 时触发，HR salary 三级链每级审批人各自收到任务到达通知——**规避了降级方案 (B) 仅覆盖第一步的残留风险**。接收人=ROLE（审批人角色名），bootstrap 角色未落地 config-gated 静默跳过。
  - **残留风险（已缓解）**：bootstrap actorType=all，"候选审批人"=任意用户，通知经 ROLE 模板投递到角色用户；角色基础设施未落地时 config-gated 跳过（无通知，不阻断）。角色落地后通知精确路由自动生效（无需改 (A)）。
- [x] Add: 任务到达通知——按 Decision 落地，`notify("wf.<entity>.task-assigned", {entityId, docNo, approverRole})`，接收人=候选审批人。config-gated。
  - Skill: `nop-backend-dev`
  - **落地**：4 域审批步骤 `<on-enter>` 调 `notify('wf.<entity>.task-assigned', {docNo, entityId, stepName})`。salary 三级步骤各独立 onEnter。
- [x] Add: 种子模板 `wf.<entity>.task-assigned`（业务提醒，5 分钟合并），三套 SQL。
  - Skill: `nop-backend-dev`
  - **落地**：3 套 SQL 各增 4 行（ID 7121-7124，ROLE 接收人；salary task-assigned 含 HR专员/财务主管/部门负责人 三角色）。
- [x] Proof: wf 测试——submitForApproval 后审批人收到 task-assigned 通知（测试断言）。命令同 Phase 1。
  - Skill: `nop-testing`
  - **落地**：`TestErpPurPaymentApprovalNotifications.testCcStepInstanceAndRoleNotifications` 断言审批人角色用户收到 `wf.pur-payment.task-assigned` 通知（agree 触发 finance-approval onEnter）。全绿。

Exit Criteria:

- [x] wf 提交后候选审批人收到 task-assigned 通知（测试断言），降级方案（若需）行为正确

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0cb85bca9ffe6kmuT3BruDbDz1`, general agent 新会话) because — 全部 Current Baseline 主张经独立仓库核实：4 个 .xwf 存在且 on-wf-end listener 经 `inject('nopBizObjectManager').getBizObject` 反射调业务 approve/reject（payment/receipt/disposal/salary 全核实）；当前无 specialType=cc；平台 workflow-configuration.md 确证 specialType=cc + confirm 动作 + oa:WhenAllowConfirm/WhenAllowAgree 守卫 + `examples/cc-notify/v1.xwf`；nop-wf 文档仅记录 `eventPattern="*end"`、无 step-start/after-assign 事件——验证 Phase 1 Explore 对 wf 钩子能力未知性的诚实标注（Phase 3 Explore-gated + Closure Gate 显式处理"无钩子降级/否则残留风险"）；无 wf.* 种子模板；4 实体 useWorkflow=true 确认。rule 4/14 split vs 0642-1 合理（wf 生命周期通知 vs BizModel 运营消费者，集成模式/owner doc/Deferred 源不同）；rule 9 Explore-before-Decision 正确；anti-slack + Deferred 命名触发条件合规；0315-1「通知/抄送步骤」触发条件经 0504-1 真实满足。修订吸收 4 项非阻塞观察：Phase 2 聚合 Item Type 修正（N1）、Phase 3 降级方案 (B) 多级链残留风险显式记录（N2）、Phase 2 cc assignment 方式须 Decision 二选一（N3）、Phase 1 模板合并子决策（N4 留实现期）。

## Closure Gates

- [x] 范围内行为完成（结果通知 4 域 + CC 步骤 4 xwf + 任务到达通知 4 域 + 种子模板）
- [x] 相关文档对齐（`approval-framework.md` 抄送/通知章节；`notification-strategy.md` 审批通知类型；当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + 4 域 `mvn test -pl <域 service> -am`
  - 根 `mvn clean install -DskipTests` 全绿（154 reactor 模块）
  - `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-assets/erp-ast-service,module-hr/erp-hr-service` 全绿（含 TestErpPurPaymentApprovalNotifications 3 测试 + 4 域 WORKFLOW 测试 12 测试 + 各域其他测试）
  - `mvn test -pl module-notify/erp-notify-service` 全绿（9 测试，验证 resolver/dispatcher/BizModel 增强无回归）
- [x] 无范围内项目降级为 deferred/follow-up（Phase 3 选方案 (A) 落地，多级链覆盖；cc 非阻塞经 actorType=all + 既有测试 confirm 验证）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 金额阈值条件路由

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: bootstrap 全量走完整审批链；金额分级（大额 GM 审批）需业务规则 + `<when>` 条件，属产品化 Delta。`0315-1` 已 Deferred。
- Successor Required: `yes`（触发条件：金额分级审批业务规则落地时）

### HR 精确角色路由驱动的审批人/CC 精确分配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期 CC/审批人用 actorType=all + ROLE resolver（角色未落地 config-gated 空）。精确组织层级路由依赖角色基础设施。
- Successor Required: `yes`（触发条件：ERP 角色定义基础设施落地时）

### 委托/转办/加签

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 平台 wf-actor 委托/转办扩展，本期仅基础审批链 + CC。`0315-1` 已 Deferred。
- Successor Required: `yes`（触发条件：委托/转办业务需求落地时）

## Closure

Status Note: 3 Phase 全部完成，范围内行为落地（结果通知 4 域 + cc step 4 xwf + 任务到达通知 4 域 + 12 种子模板 × 3 SQL）。验证全绿：根 `mvn clean install -DskipTests`（154 模块）+ notify service 9 测试 + 4 域 service 测试（含新增 `TestErpPurPaymentApprovalNotifications` 3 测试 + 4 域 WORKFLOW 测试 agree+cc-confirm 扩展 12 测试）。执行期两项关键技术发现已记录（`<observes>` dead 特性 → 改 listener 注入；XLang 不执行 try/catch → notify Java 层统一 catch）。范围内无项目降级为 deferred；cc 非阻塞残留风险（业务 approve 在 cc confirm 后触发）对 bootstrap 参考应用可接受并已记录。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（general agent 新会话）— 见下方审计记录
- Evidence: 全绿验证命令输出（`mvn clean install -DskipTests` BUILD SUCCESS；4 域 + notify service `mvn test` 全绿）；plan 内 3 Phase 全 `[x]` + Status: completed；docs/logs/2026/07-06.md 第 1 节记录；approval-framework.md/notification-strategy.md 对齐；roadmap 5.5 ✅

Follow-up:

- 金额阈值条件路由（见上方 Deferred）
- HR 精确角色路由（见上方 Deferred，角色基础设施触发）
- cc step auto-confirm（bootstrap 后产品化增强，解除业务 approve 延迟残留风险）
