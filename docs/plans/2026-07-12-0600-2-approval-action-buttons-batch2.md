# 2026-07-12-0600-2-approval-action-buttons-batch2 审批动作按钮第二批（21 实体 rowActions 接线）

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「23 实体全口径审批按钮」（Successor Required: yes，触发条件=对应实体审批入口用户面需求落地，**本计划即该 successor**）；`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §4.2 P1 缺陷 1
> Related: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Phase 1（批次 1：ErpPurOrder / ErpSalOrder 2 实体落地范式）
> Audit: required

## Current Baseline

批次 1（计划 1643-1 Phase 1）已为 2 核心订单（ErpPurOrder / ErpSalOrder）落地审批按钮范式：定制层 `<Entity>.view.xml` 的 `<crud name="main"><rowActions x:override="bounded-merge">` 接线 row-submit-button / row-approve-button / row-reject-button / row-cancel-button，调 `@mutation:<Entity>__submitForApproval|__approve|__reject|__cancel?id=$id`，`visibleOn` 按 `approveStatus` 状态门控（提交=UNSUBMITTED‖REJECTED、批准/驳回=SUBMITTED、作废=docStatus≠CANCELLED）。范本文件：`module-purchase/erp-pur-web/.../ErpPurOrder/ErpPurOrder.view.xml:33-69`。

**剩余缺口**（经独立子代理全仓库扫描确认 `ses_0acad9798ffeAEjy2eIhelfw2R`）：

以下 21 实体的审批 mutation 已就绪（经定制 xbiz 显式声明或经 `_gen` `_*.xbiz` 继承 `/nop/wf/base/approval-support.xbiz` 的标准 mutation 集 `submitForApproval` / `approve` / `reject` / `reverseApprove` / `withdrawApproval`），BizModel Java 实现已就绪，但定制层 `<Entity>.view.xml` 为空壳（无 `<rowActions>` 审批按钮），用户无法从列表页发起审批。

> **mutation 可达性说明**：21 实体中 20 个的 `_gen` xbiz 均 `x:extends="/nop/wf/base/approval-support.xbiz"`，继承完整 5-mutation 集。ErpInvCostAdjust 例外——其 `_gen` xbiz 无 `x:extends`，但定制 `ErpInvCostAdjust.xbiz` 经 Processor 显式声明全部审批 mutation（submit/approve/reject/reverseApprove/withdrawApproval）。因此全部 21 实体的 `submitForApproval` / `approve` / `reject` 均经 GraphQL 可调用。4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）的 `submitForApproval` 内部可能触发 xwf 工作流，但 mutation 入口本身仍经 `__submitForApproval` 可调用。

| # | 实体 | 域 | 定制 xbiz 显式声明 | useWorkflow |
|---|------|----|--------------------|:-----------:|
| 1 | ErpPurReceive | purchase | submit / approve / reverseApprove | — |
| 2 | ErpPurInvoice | purchase | submit / approve / reverseApprove | — |
| 3 | ErpPurReturn | purchase | submit / approve / reverseApprove | — |
| 4 | ErpPurPayment | purchase | submit / approve / reverseApprove | ✓ |
| 5 | ErpPurRequisition | purchase | submit（approve/reject 经继承） | — |
| 6 | ErpSalDelivery | sales | submit / approve / reject / reverseApprove | — |
| 7 | ErpSalInvoice | sales | submit / approve / reject / reverseApprove | — |
| 8 | ErpSalQuotation | sales | submit / approve / reject / reverseApprove | — |
| 9 | ErpSalReturn | sales | submit / approve / reject / reverseApprove | — |
| 10 | ErpSalReceipt | sales | submit / approve / reject / reverseApprove | ✓ |
| 11 | ErpMfgWorkOrder | manufacturing | submit / approve / reject / reverseApprove | — |
| 12 | ErpQaRecall | quality | submit / approve / reject / reverseApprove | — |
| 13 | ErpAstAssetCapitalization | assets | submit / approve / reject / reverseApprove | — |
| 14 | ErpAstDisposal | assets | submit / approve / reject / reverseApprove / withdrawApproval | ✓ |
| 15 | ErpAstMerge | assets | submit / approve / reject / reverseApprove | — |
| 16 | ErpAstSplit | assets | submit / approve / reject / reverseApprove | — |
| 17 | ErpAstValueAdjustment | assets | submit / approve / reject / reverseApprove | — |
| 18 | ErpFinExpenseClaim | finance | submit / approve / reject / reverseApprove | — |
| 19 | ErpFinEmployeeAdvance | finance | submit / approve / reject / reverseApprove | — |
| 20 | ErpInvCostAdjust | inventory | submit / approve / reject / reverseApprove / withdrawApproval（定制 xbiz 显式声明，无 _gen 继承） | — |
| 21 | ErpHrSalary | hr | submit（approve/reject 经继承 + wf listener 回调） | ✓ |

> **按钮集裁定**：全部 21 实体统一接线 submit / approve / reject 三按钮——20 个实体 `approve` / `reject` 经 `approval-support.xbiz` 继承可达，ErpInvCostAdjust 经定制 xbiz Processor 显式声明可达；无需按定制 xbiz 显式声明区分按钮集。

- 定制机制：`<Entity>.view.xml` 用 `x:extends="_gen/_<Entity>.view.xml"` + `<rowActions x:override="bounded-merge">`；项目不用 `_delta/` 目录。
- 状态守卫：后端由 `approval-support.xbiz`（或各实体 Java BizModel）兜底——即使前端 `visibleOn` 失效也返回业务异常，不产生非法迁移。
- GraphQL mutation 调用经浏览器层 E2E 已验证范式（0335-1 审批门控 DIRECT 业务动作、1249-1 P2P/O2C 编排链）。

## Goals

- 21 实体定制层 `<Entity>.view.xml` 的 `<rowActions>` 统一接线 submit / approve / reject 三按钮，调各实体经 xbiz 显式声明或经 `approval-support.xbiz` 继承的 `__submitForApproval` / `__approve` / `__reject` mutation，`visibleOn` 按 `approveStatus` 状态门控。
- 用户可从列表页直接发起审批操作，无需手工构造 GraphQL mutation。

## Non-Goals

- **drawer / edit 表单内审批入口**——与 1643-1 同口径，仅 rowActions；form 内审批入口归后续 successor。
- **useWorkflow (xwf) 实体完整 submit→approve 浏览器层 E2E**——4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）的 `submitForApproval` 内部触发 xwf 工作流。按钮接线本身正确（mutation 经 GraphQL 可调用）；但完整 submit→approve 浏览器层 E2E 经 2330-1 裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）。本计划接线按钮但不增 xwf E2E。
- **reverseApprove（反审批）按钮**——reverseApprove 为审批后撤销，语义不同于审批生命周期三步（提交/批准/驳回）。本计划仅覆盖审批正向生命周期。reverseApprove 按钮归后续 successor（触发条件：反审批用户面需求落地时）。
- **`cancel` / `post` / `reverse` 等非审批业务动作按钮**——这些是各实体的领域特定动作（如采购订单 cancel、凭证 post），不属审批轴。归各域业务动作 successor。
- **新增后端 mutation 或 xbiz 声明**——所有 mutation 已在 xbiz/Java 中定义（或经 `approval-support.xbiz` 继承），本计划仅接线前端按钮。

## Task Route

- Type: `implementation-only change`（后端 mutation 已就绪，仅前端 view.xml 静态资源接线；不改 API/模型/认证/契约）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（审批接线范式）、`../nop-entropy/docs-for-ai/03-runbooks/add-page-business-action.md`（加业务动作按钮）
- Skill Selection Basis: 涉 AMIS view.xml rowActions bounded-merge 定制 → 匹配 `nop-frontend-dev`（XView 三层 / rowActions / bounded-merge / visibleOn 状态门控）。无后端变更 → `nop-backend-dev` 不匹配。无新 Java 测试 → `nop-testing` 仅用于回归。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。前端 view.xml 静态资源接线，无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 核心交易实体审批按钮（purchase + sales，10 实体）

Status: completed
Targets: ErpPurReceive / ErpPurInvoice / ErpPurReturn / ErpPurPayment / ErpPurRequisition / ErpSalDelivery / ErpSalInvoice / ErpSalQuotation / ErpSalReturn / ErpSalReceipt 的定制层 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（xbiz/Java mutation 已就绪）

- [x] `Decision`: 裁决按钮集——全部 21 实体统一接线 submit / approve / reject 三按钮。理由：20 个实体 `approve` / `reject` 经 `approval-support.xbiz` 继承 GraphQL 可达；ErpInvCostAdjust 经定制 xbiz Processor 显式声明全部审批 mutation 可达——两种路径均支持统一按钮集，无需按定制 xbiz 显式声明区分。`visibleOn` 统一采用 `approveStatus == 'UNSUBMITTED' || approveStatus == 'REJECTED'`（提交）/ `approveStatus == 'SUBMITTED'`（批准/驳回）。替代方案：按定制 xbiz 显式声明区分（否决——两种路径均可达，区分无技术依据且徒增维护复杂度）。残留风险：4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）的 `submitForApproval` 内部触发 xwf 工作流——按钮接线正确（mutation 可调用），但完整 submit→approve 浏览器层 E2E 经 2330-1 裁决不可行；按钮回归经 view.xml well-formed + 既有 DIRECT 实体 E2E 范式间接覆盖。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 10 实体定制层 view.xml 增 `<crud name="main"><rowActions x:override="bounded-merge">` 接线 submit / approve / reject 三按钮，调 `@mutation:<Entity>__submitForApproval|__approve|__reject?id=$id` + `confirmText` + `visibleOn` 状态门控，镜像 `ErpPurOrder.view.xml:33-69` 范式。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 10 view.xml `xmllint --noout` well-formed + 既有 P2P/O2C E2E（`p2p-*.spec.ts` / `o2c-*.spec.ts` / `approval-gated-*.spec.ts`）回归无新增失败（按钮接线为静态资源，mutation 调用经既有 E2E 间接覆盖）。
  - Skill: none

Exit Criteria:

- [x] 10 核心交易实体 view.xml 含 submit / approve / reject 三按钮且 `xmllint --noout` well-formed
- [x] 既有审批相关 E2E 回归无新增失败

### Phase 2 - 跨域实体审批按钮（assets / finance / manufacturing / quality / inventory / hr，11 实体）

Status: completed
Targets: ErpAstAssetCapitalization / ErpAstDisposal / ErpAstMerge / ErpAstSplit / ErpAstValueAdjustment / ErpFinExpenseClaim / ErpFinEmployeeAdvance / ErpMfgWorkOrder / ErpQaRecall / ErpInvCostAdjust / ErpHrSalary 的定制层 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式已验证

- [x] `Add`: 11 实体定制层 view.xml 增 `<rowActions>` 接线 submit / approve / reject 三按钮，同 Phase 1 范式。ErpAstDisposal / ErpHrSalary 为 `useWorkflow="true"` 实体——submit 触发 xwf 工作流但 mutation 入口经 GraphQL 可调用，按钮接线与其他实体同范式（完整 xwf submit→approve E2E 归 Non-Goal）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 11 view.xml `xmllint --noout` well-formed + 涉及域既有 E2E 回归无新增失败（如 `mfg-chain.spec.ts` WorkOrder 审批轴、`approval-gated-*.spec.ts` Return/Recall 审批轴）。
  - Skill: none

Exit Criteria:

- [x] 11 跨域实体 view.xml 含 submit / approve / reject 审批按钮且 `xmllint --noout` well-formed
- [x] 涉及域既有 E2E 回归无新增失败

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0aca4c106ffe9wmODvQNEHhWaw`) — 21 实体空壳 + ErpPurOrder 范式核心主张核实为真。3 BLOCKER：Non-Goals 虚假声明 ErpPurPayment/ErpSalReceipt/ErpAstDisposal 审批经 xwf 非 DIRECT（实际 xbiz 有 DIRECT approve）/ ErpPurRequisition "approve 在 Java" 虚假（经继承可达）/ Phase 1 Decision 对 ErpPurPayment 自相矛盾。2 MAJOR：mutation 可达性表混淆定制 xbiz 与 GraphQL 可达性 / ErpHrSalary 描述误导。1 MINOR：Phase 2 计数错误。
- Independent draft review iteration 2: `needs revision` (`ses_0ac980de0ffehuQqSlpCb1T4dy`) — 全部 iteration 1 BLOCKER/MAJOR/MINOR 修复经核实。新 1 MAJOR：ErpInvCostAdjust _gen xbiz 无 x:extends（不继承 approval-support.xbiz），line 17 笼统声明虚假——3 按钮结论仍成立（定制 xbiz 经 Processor 显式声明）但理由需修正。Rule 14 拆分经裁决合法（defer+successor 机制）。
- Independent draft review iteration 3: `accept` (`ses_0ac94a594ffeZtAEq91uouhf15`) — ErpInvCostAdjust 例外在 4 处（mutation 可达性说明 / 按钮集裁定 / Phase 1 Decision 理由 / 表 row 20）一致且正确。无 BLOCKER / 无 MAJOR / 无 MINOR。anti-slack 合规。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 既有 Playwright E2E 回归一次。

- [x] 范围内行为完成（21 实体审批按钮接线）
- [x] 相关文档对齐（`view-and-page-strategy.md` 若有审批按钮约定更新）——审批按钮 rowActions 范式已由 1643-1 批次 1（ErpPurOrder/ErpSalOrder）确立为本项目既定约定，本批次 21 实体纯复制推广，无新约定需更新
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + 21 view.xml `xmllint --noout` well-formed + 既有 E2E 回归无新增失败）——154 模块 BUILD SUCCESS；21 view.xml `xmllint --noout` 全 OK；按钮接线为静态资源，mutation 调用经既有 P2P/O2C/approval-gated E2E 间接覆盖
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 3 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### reverseApprove（反审批）按钮

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: reverseApprove 为审批后撤销操作，语义不同于审批正向生命周期三步（提交/批准/驳回）。本计划仅覆盖正向审批入口。
- Successor Required: `yes`（触发条件：反审批用户面需求落地时）

### drawer / edit 表单内审批入口

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 与 1643-1 同口径，仅 rowActions 列表页入口。form 内审批入口属不同交互面。
- Successor Required: `yes`（触发条件：表单内审批入口用户面需求落地时）

### useWorkflow (xwf) 实体完整 submit→approve 浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）按钮已接线（mutation 经 GraphQL 可调用），但完整 submit→approve 浏览器层 E2E 经 2330-1 权威裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）。需 nop-entropy 平台变更。
- Successor Required: `yes`（触发条件：nop-entropy 平台支持浏览器层测试用户身份映射时）

## Closure

Status Note: **计划已执行完成**（结束审计待独立子代理）。Phase 1（purchase 5 + sales 5 共 10 实体）+ Phase 2（assets 5 + finance 2 + manufacturing 1 + quality 1 + inventory 1 + hr 1 共 11 实体）共 21 实体定制层 `<Entity>.view.xml` 的 `<crud name="main"><rowActions x:override="bounded-merge">` 已接线 submit / approve / reject 三按钮，调 `@mutation:<Entity>__submitForApproval|__approve|__reject?id=$id` + `confirmText` + `visibleOn` 按 `approveStatus` 状态门控，镜像批次 1 `ErpPurOrder.view.xml:33-69` 范式（去掉 Non-Goal 的 cancel 按钮）。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；21 view.xml `xmllint --noout` 全 well-formed；按钮接线为静态资源，mutation 调用经既有 P2P/O2C/approval-gated E2E 间接覆盖。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_0ac034e26ffetlUzEiFSy71Evh`，general，无执行者上下文）
- Evidence:
  - VERDICT: `pass`（无 BLOCKER / 无 MAJOR）
  - 21 实体逐文件核实：每文件含 6 action（row-view/row-update[drawer]/row-submit/row-approve/row-reject/row-delete），3 审批按钮 mutation 实体名与文件实体一致（0 copy-paste 错配），中文 label 与实体匹配，visibleOn 守卫表达式精确（submit=UNSUBMITTED‖REJECTED、approve·reject=SUBMITTED）
  - `xmllint --noout` 21 文件：21 OK / 0 FAIL
  - 特例 mutation 可达性核实：ErpInvCostAdjust 定制 xbiz Processor 显式声明 5 mutation（_gen 无 x:extends）；ErpPurRequisition/ErpHrSalary `_gen` xbiz `x:extends="/nop/wf/base/approval-support.xbiz"` 继承 approve/reject
  - `git status`：0 `_gen/_*.view.xml` 被改，恰好 21 个定制层 `*.view.xml` 为 M
  - 计划内部一致性：Phase 1/2 均 Status: completed + 全 [x]；Plan Status: completed；Closure Gates 1-6 [x]；源计划 1643-1 Deferred Successor Progress 注记指向本计划
  - 验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS

Follow-up:

- reverseApprove 按钮 successor（见上方 Deferred）
- drawer/edit 表单审批入口 successor（见上方 Deferred）
- useWorkflow (xwf) approve 按钮 successor（见上方 Deferred）
