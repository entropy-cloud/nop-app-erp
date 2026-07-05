# 2026-07-06-0642-2-approval-workflow-notifications 审批工作流通知与抄送

> Plan Status: active
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

Status: planned
Targets: 各域 `erp-*-service` 的 `approve`/`reject` xbiz（`_overrides` 或 `.xbiz.xml` append）；或 4 个 `.xwf` on-wf-end listener；`docs/` Explore 记录
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 通道（`0504-1`）

- [ ] Explore: 确认 nop-wf 步骤事件钩子可用性——读 `../nop-entropy/docs-for-ai/02-core-guides/workflow-configuration.md` 全文 + wf 引擎 listener eventPattern 支持的 event 类型（是否含 step-start/after-assign），记录可用事件清单。此 Explore 解除 Phase 3 任务到达通知的 Decision 阻塞。
  - Skill: none
- [ ] Decision: 结果通知注入点选择——优先 xbiz `<observes eventPattern="approve|reject">`（业务层联动，wf 只编排，符合 `enable-approval-on-entity.md` 推荐「联动在 xbiz 层注入」），调 `notify` 通知提单人。替代方案=扩展 on-wf-end listener（但 listener 已承担 approve/reject 调用，耦合通知增复杂度）。裁决理由+残留风险写入计划。
  - Skill: `nop-backend-dev`
- [ ] Add: 4 域 approve/reject xbiz observe——各注入 `notify("wf.<entity>.approved"|"wf.<entity>.rejected", {entityId, docNo, submitterUserId, approverUserName, comment})`，接收人=submitterUserId（USER_LIST）。config-gated（`erp-<domain>.approval-result-notify-enabled` 默认 true）。事件类型按域区分（payment/receipt/disposal/salary）。
  - Skill: `nop-backend-dev`
- [ ] Add: 种子模板 4 类（approved/rejected × 可合并到每域一个 `wf.<entity>.result` 模板按 context.action 区分，或 approved/rejected 分开）——业务提醒类，5 分钟合并，三套 SQL（mysql/oracle/postgresql）。
  - Skill: `nop-backend-dev`
- [ ] Proof: wf 端到端测试——启动 wf→审批人 agree→wf end→断言提单人收到 approved 通知；disagree→rejected 通知。复用 `0315-1` 的 4 个 WORKFLOW 测试类扩展。命令 `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-assets/erp-ast-service,module-hr/erp-hr-service -am`。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] Explore 输出记录 nop-wf 可用事件清单（解除 Phase 3 Decision 阻塞）
- [ ] 4 域审批通过/驳回触发提单人 `ErpSysNotification` 行（测试断言），config 关闭静默跳过

### Phase 2 — 抄送（CC）步骤接线

Status: planned
Targets: 4 个 `.xwf`（增 `specialType="cc"` step）；种子模板
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] Decision: CC 角色与插入位置——按各单据语义确定 CC 接收角色：付款单（CC 财务经理/出纳）、收款单（CC 销售/财务）、资产处置（CC 资产管理员）、薪酬（CC HR 专员）。CC 步骤插入审批步骤之后（审批通过后抄送知会，非阻塞）。cc step assignment 方式须在本 Decision **确定单一方案**（config-gated 静态接收人 OR ROLE resolver，二选一并记录理由），bootstrap 角色未落地时所选方案须 config-gated 回退空（cc step 无 actor 时不阻塞 wf 推进）。记录每域 CC 角色裁决 + assignment 方式裁决。
  - Skill: `nop-backend-dev`
- [ ] Add: 4 个 `.xwf` 增 cc step（`specialType="cc"`，`<assignment><actor actorType="all"/></assignment>` 或角色绑定，action 仅 `confirm`）。cc step 经 wf 自动产生 step 实例，平台 `oa:WhenAllowConfirm` 守卫已就绪。
  - Skill: `nop-backend-dev`
- [ ] Add: CC 通知——cc step 到达时通知 CC 接收人（经 Phase 1 Explore 确认的 step 事件钩子；若不可用，cc step 的 assignment 本身即产生待办，配合结果通知覆盖）。种子模板 `wf.<entity>.cc`。
  - Skill: `nop-backend-dev`
- [ ] Proof: wf 测试——审批通过后 cc step 产生 step 实例 + CC 接收人收到通知（断言 ErpSysNotification 或 step 实例存在）。命令同 Phase 1。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 个 `.xwf` 含 cc step，审批通过后 CC 接收人收到知会（测试断言 cc step 实例/通知）

### Phase 3 — 任务到达通知（审批人）

Status: planned
Targets: 各域 `submitForApproval` xbiz 或 wf 步骤事件钩子（依 Phase 1 Explore 结论）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 Explore 结论

- [ ] Decision: 任务到达通知实现路径——依 Explore：(A) 若平台 wf 支持 step-start/after-assign 事件，经 listener 钩子通知候选审批人；(B) 若不支持，在 `submitForApproval` action wf 启动后通知审批步骤的候选 actor（bootstrap actorType=all 时通知全角色或 config 指定 userId）。裁决+降级方案写入计划。残留风险须显式记录：降级方案 (B) 仅覆盖 wf 启动时的第一步审批人；对多级审批链（HR salary 三级 hr-review→finance-review→manager-approval），后续步骤审批人在 (B) 下无任务到达通知——须在残留风险中声明，并记录"角色基础设施落地后改 (A) 或 actor 驱动"为解除条件。
  - Skill: `nop-backend-dev`
- [ ] Add: 任务到达通知——按 Decision 落地，`notify("wf.<entity>.task-assigned", {entityId, docNo, approverRole})`，接收人=候选审批人。config-gated。
  - Skill: `nop-backend-dev`
- [ ] Add: 种子模板 `wf.<entity>.task-assigned`（业务提醒，5 分钟合并），三套 SQL。
  - Skill: `nop-backend-dev`
- [ ] Proof: wf 测试——submitForApproval 后审批人收到 task-assigned 通知（测试断言）。命令同 Phase 1。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] wf 提交后候选审批人收到 task-assigned 通知（测试断言），降级方案（若需）行为正确

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0cb85bca9ffe6kmuT3BruDbDz1`, general agent 新会话) because — 全部 Current Baseline 主张经独立仓库核实：4 个 .xwf 存在且 on-wf-end listener 经 `inject('nopBizObjectManager').getBizObject` 反射调业务 approve/reject（payment/receipt/disposal/salary 全核实）；当前无 specialType=cc；平台 workflow-configuration.md 确证 specialType=cc + confirm 动作 + oa:WhenAllowConfirm/WhenAllowAgree 守卫 + `examples/cc-notify/v1.xwf`；nop-wf 文档仅记录 `eventPattern="*end"`、无 step-start/after-assign 事件——验证 Phase 1 Explore 对 wf 钩子能力未知性的诚实标注（Phase 3 Explore-gated + Closure Gate 显式处理"无钩子降级/否则残留风险"）；无 wf.* 种子模板；4 实体 useWorkflow=true 确认。rule 4/14 split vs 0642-1 合理（wf 生命周期通知 vs BizModel 运营消费者，集成模式/owner doc/Deferred 源不同）；rule 9 Explore-before-Decision 正确；anti-slack + Deferred 命名触发条件合规；0315-1「通知/抄送步骤」触发条件经 0504-1 真实满足。修订吸收 4 项非阻塞观察：Phase 2 聚合 Item Type 修正（N1）、Phase 3 降级方案 (B) 多级链残留风险显式记录（N2）、Phase 2 cc assignment 方式须 Decision 二选一（N3）、Phase 1 模板合并子决策（N4 留实现期）。

## Closure Gates

- [ ] 范围内行为完成（结果通知 4 域 + CC 步骤 4 xwf + 任务到达通知 4 域 + 种子模板）
- [ ] 相关文档对齐（`approval-framework.md` 抄送/通知章节；`notification-strategy.md` 审批通知类型；当日日志）
- [ ] 已运行验证：根 `mvn clean install -DskipTests` + 4 域 `mvn test -pl <域 service> -am`
- [ ] 无范围内项目降级为 deferred/follow-up（Phase 3 若 Explore 判定平台无钩子且降级方案成立则落地，否则记录残留风险但不得静默降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

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

Status Note: <待执行完成后填写>

Closure Audit Evidence:

- <待独立结束审计>

Follow-up:

- 金额阈值条件路由（见上方 Deferred）
- HR 精确角色路由（见上方 Deferred，角色基础设施触发）
