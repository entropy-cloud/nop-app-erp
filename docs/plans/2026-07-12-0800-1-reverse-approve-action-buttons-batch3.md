# 2026-07-12-0800-1-reverse-approve-action-buttons-batch3 反审批动作按钮第三批（23 实体 rowActions reverseApprove 接线）

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Source: `docs/plans/2026-07-12-0600-2-approval-action-buttons-batch2.md` Deferred「reverseApprove（反审批）按钮」（Successor Required: yes，触发条件=反审批用户面需求落地时，**本计划即该 successor**）；`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §4.2 P1 缺陷 1 审批按钮收尾
> Related: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md`（批次 1：ErpPurOrder / ErpSalOrder 2 实体，4 按钮含 cancel）；`docs/plans/2026-07-12-0600-2-approval-action-buttons-batch2.md`（批次 2：21 实体 submit / approve / reject 三按钮）
> Audit: required

## Current Baseline

审批按钮接线经两个前置计划已覆盖 submit / approve / reject（+ 批次 1 的 cancel）：

- **批次 1**（计划 1643-1 Phase 1）：ErpPurOrder / ErpSalOrder 2 核心订单，4 按钮（submit / approve / reject / cancel），范本 `module-purchase/erp-pur-web/.../ErpPurOrder/ErpPurOrder.view.xml:33-69`。
- **批次 2**（计划 0600-2）：21 跨域实体（purchase 5 + sales 5 + assets 5 + finance 2 + manufacturing 1 + quality 1 + inventory 1 + hr 1），3 按钮（submit / approve / reject）。

**剩余缺口**：全部 23 审批实体均**无 reverseApprove（反审批）按钮**接线。用户批准后无法从列表页撤销审批使单据回到可编辑状态，必须手工构造 GraphQL mutation。

**reverseApprove mutation 可达性**（经实时仓库核实）：

- 22 实体经定制层 `*.xbiz` 显式声明 `reverseApprove`（`rg -l "reverseApprove" --glob '*.xbiz' module-*/erp-*-service/` 确认 22 命中：purchase 5 / sales 6 / assets 5 / finance 2 / mfg 1 / quality 1 / inventory 1 / hr 1）。
- ErpPurRequisition 例外：定制 xbiz 未显式声明，但 `_ErpPurRequisition.xbiz` 经 `x:extends="/nop/wf/base/approval-support.xbiz"` 继承完整 5-mutation 集（含 reverseApprove），GraphQL 可达（与 0600-2 submit/approve/reject 可达性裁定同路径）。
- 故全部 23 实体的 `__reverseApprove` mutation 经 GraphQL 可调用。

**reverseApprove 语义与状态门控**：

- reverseApprove = 审批人撤销已做出的审批，`approveStatus: APPROVED → REJECTED`（经 `ErpPurOrderProcessor.java:239` 等各域 Processor 及 `approval-support.xbiz` `entity.approveStatus = 'REJECTED'` 核实）。单据到 REJECTED 后，submit 按钮 visibleOn（`UNSUBMITTED || REJECTED'`）覆盖，故仍可重新提交审批。与 reject 的区别仅在起始态：reject 从 SUBMITTED 驳回，reverseApprove 从 APPROVED 反审批——两者终态均为 REJECTED。与 cancel（作废单据，docStatus → CANCELLED）语义不同。
- `visibleOn` 应为 `${approveStatus == 'APPROVED'}`（仅已批准单据可反审批），与现有 `visibleOn` 模式对齐：
  - submit = `approveStatus == 'UNSUBMITTED' || approveStatus == 'REJECTED'`
  - approve / reject = `approveStatus == 'SUBMITTED'`
  - cancel（仅批次 1 两订单）= `docStatus != 'CANCELLED'`
- 状态守卫由后端 `approval-support.xbiz`（或各实体 Java BizModel）兜底——即使前端 visibleOn 失效也返回业务异常，不产生非法迁移。

**定制机制**：定制层 `<Entity>.view.xml` 用 `x:extends="_gen/_<Entity>.view.xml"` + `<rowActions x:override="bounded-merge">`；项目不用 `_delta/` 目录（同批次 1/2）。

**23 实体清单**（与批次 1+2 完全对齐）：

| # | 实体 | 域 | 批次 | reverseApprove 可达路径 |
|---|------|----|------|------------------------|
| 1 | ErpPurOrder | purchase | 1 | 定制 xbiz 显式声明 |
| 2 | ErpSalOrder | sales | 1 | 定制 xbiz 显式声明 |
| 3 | ErpPurReceive | purchase | 2 | 定制 xbiz 显式声明 |
| 4 | ErpPurInvoice | purchase | 2 | 定制 xbiz 显式声明 |
| 5 | ErpPurReturn | purchase | 2 | 定制 xbiz 显式声明 |
| 6 | ErpPurPayment | purchase | 2 | 定制 xbiz 显式声明 |
| 7 | ErpPurRequisition | purchase | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 8 | ErpSalDelivery | sales | 2 | 定制 xbiz 显式声明 |
| 9 | ErpSalInvoice | sales | 2 | 定制 xbiz 显式声明 |
| 10 | ErpSalQuotation | sales | 2 | 定制 xbiz 显式声明 |
| 11 | ErpSalReturn | sales | 2 | 定制 xbiz 显式声明 |
| 12 | ErpSalReceipt | sales | 2 | 定制 xbiz 显式声明 |
| 13 | ErpAstAssetCapitalization | assets | 2 | 定制 xbiz 显式声明 |
| 14 | ErpAstDisposal | assets | 2 | 定制 xbiz 显式声明 |
| 15 | ErpAstMerge | assets | 2 | 定制 xbiz 显式声明 |
| 16 | ErpAstSplit | assets | 2 | 定制 xbiz 显式声明 |
| 17 | ErpAstValueAdjustment | assets | 2 | 定制 xbiz 显式声明 |
| 18 | ErpFinExpenseClaim | finance | 2 | 定制 xbiz 显式声明 |
| 19 | ErpFinEmployeeAdvance | finance | 2 | 定制 xbiz 显式声明 |
| 20 | ErpMfgWorkOrder | manufacturing | 2 | 定制 xbiz 显式声明 |
| 21 | ErpQaRecall | quality | 2 | 定制 xbiz 显式声明 |
| 22 | ErpInvCostAdjust | inventory | 2 | 定制 xbiz Processor 显式声明（_gen 无 x:extends） |
| 23 | ErpHrSalary | hr | 2 | 定制 xbiz 显式声明 |

## Goals

- 23 审批实体定制层 `<Entity>.view.xml` 的 `<rowActions>` 统一接线 reverseApprove（反审批）按钮，调各实体经 xbiz 显式声明或经 `approval-support.xbiz` 继承的 `__reverseApprove` mutation，`visibleOn="${approveStatus == 'APPROVED'}"` 状态门控。
- 用户可从列表页直接撤销审批，使已批准单据回到驳回状态（可重新编辑提交），无需手工构造 GraphQL mutation。

## Non-Goals

- **withdrawApproval（撤回提交）按钮**——withdrawApproval 语义为提交人撤回自己的提交（`SUBMITTED → UNSUBMITTED`），不同于 reverseApprove（审批人撤销审批，`APPROVED → REJECTED`）。本计划仅覆盖 reverseApprove。withdrawApproval 按钮归后续 successor（触发条件：撤回提交用户面需求落地时）。
- **drawer / edit 表单内审批入口**——与批次 2 同口径，仅 rowActions 列表页入口。form 内审批入口属不同交互面，归后续 successor。
- **useWorkflow (xwf) 实体完整 reverseApprove 浏览器层 E2E**——4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）按钮接线正确（mutation 经 GraphQL 可调用），但完整审批轴 submit→approve→reverseApprove 浏览器层 E2E 经 2330-1 裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）。
- **新增后端 mutation 或 xbiz 声明**——所有 reverseApprove mutation 已在 xbiz/Java 中定义（或经 `approval-support.xbiz` 继承），本计划仅接线前端按钮。

## Task Route

- Type: `implementation-only change`（后端 mutation 已就绪，仅前端 view.xml 静态资源接线；不改 API/模型/认证/契约）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（审批接线范式）、`../nop-entropy/docs-for-ai/03-runbooks/add-page-business-action.md`（加业务动作按钮）
- Skill Selection Basis: 涉 AMIS view.xml rowActions bounded-merge 定制 → 匹配 `nop-frontend-dev`（XView 三层 / rowActions / bounded-merge / visibleOn 状态门控）。无后端变更 → `nop-backend-dev` 不匹配。无新 Java 测试 → `nop-testing` 仅用于回归。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。前端 view.xml 静态资源接线，无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 核心交易实体反审批按钮（purchase + sales，12 实体）

Status: completed
Targets: ErpPurOrder / ErpSalOrder / ErpPurReceive / ErpPurInvoice / ErpPurReturn / ErpPurPayment / ErpPurRequisition / ErpSalDelivery / ErpSalInvoice / ErpSalQuotation / ErpSalReturn / ErpSalReceipt 的定制层 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（reverseApprove mutation 已就绪）

- [x] `Decision`: 裁定 reverseApprove 按钮规范——(a) 按钮插入位置：批次 2 实体（无 cancel）插入 reject 按钮、delete 按钮之间；批次 1 两实体（ErpPurOrder/ErpSalOrder，已有 cancel）插入 reject 按钮、cancel 按钮之间（审批生命周期按钮聚簇：submit→approve→reject→reverseApprove→cancel→delete）。(b) `visibleOn` 统一 `${approveStatus == 'APPROVED'}`（仅已批准可反审批）。(c) label/level/icon：label="反审批"、level="info"（区别于 approve=success / reject=warning / cancel=danger）、icon="fa fa-undo"。(d) `confirmText` 按实体硬编码中文实体名（对齐批次 1/2 范式，如"确认反审批此采购订单？反审批后单据将回到驳回状态，可重新编辑提交。"，非模板占位符）。替代方案：level="danger"（否决——reverseApprove 非破坏性操作，单据到 REJECTED 仍可重提非作废，danger 语义误导）。残留风险：4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal，Phase 1 含 ErpPurPayment/ErpSalReceipt）reverseApprove 经 xwf 内部审批引擎处理——按钮接线正确（mutation 可调用），但完整 xwf 审批轴 E2E 经 2330-1 裁决不可行；按钮回归经 view.xml well-formed + 既有 DIRECT 实体 E2E 范式间接覆盖。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 12 实体定制层 view.xml 的 `<rowActions>` 增 `<action id="row-reverse-approve-button" label="反审批" level="info" icon="fa fa-undo">`，调 `@mutation:<Entity>__reverseApprove?id=$id` + `confirmText`（按实体硬编码中文实体名，如"确认反审批此采购订单？反审批后单据将回到驳回状态，可重新编辑提交。"）+ `messages/success` + `visibleOn="${approveStatus == 'APPROVED'}"`，镜像批次 1/2 范式（批次 2 实体插入 reject/delete 之间；批次 1 两实体插入 reject/cancel 之间）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 12 view.xml `xmllint --noout` well-formed + 既有审批相关 E2E（`p2p-*.spec.ts` / `o2c-*.spec.ts` / `approval-gated-*.spec.ts`）回归无新增失败（按钮接线为静态资源，mutation 调用经既有 E2E 间接覆盖）。
  - Skill: none

Exit Criteria:

- [x] 12 核心交易实体 view.xml 含 reverseApprove 按钮且 `xmllint --noout` well-formed
- [x] 既有审批相关 E2E 回归无新增失败

### Phase 2 - 跨域实体反审批按钮（assets / finance / manufacturing / quality / inventory / hr，11 实体）

Status: completed
Targets: ErpAstAssetCapitalization / ErpAstDisposal / ErpAstMerge / ErpAstSplit / ErpAstValueAdjustment / ErpFinExpenseClaim / ErpFinEmployeeAdvance / ErpMfgWorkOrder / ErpQaRecall / ErpInvCostAdjust / ErpHrSalary 的定制层 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式已验证

- [x] `Add`: 11 实体定制层 view.xml 的 `<rowActions>` 增 reverseApprove 按钮，同 Phase 1 范式。ErpAstDisposal / ErpHrSalary 为 `useWorkflow="true"` 实体——reverseApprove mutation 入口经 GraphQL 可调用，按钮接线与其他实体同范式（完整 xwf reverseApprove E2E 归 Non-Goal）。ErpInvCostAdjust 经定制 xbiz Processor 显式声明 reverseApprove（_gen 无 x:extends），与批次 2 同特例路径。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 11 view.xml `xmllint --noout` well-formed + 涉及域既有 E2E 回归无新增失败（如 `mfg-chain.spec.ts` WorkOrder 审批轴、`approval-gated-*.spec.ts` Return/Recall 审批轴）。
  - Skill: none

Exit Criteria:

- [x] 11 跨域实体 view.xml 含 reverseApprove 按钮且 `xmllint --noout` well-formed
- [x] 涉及域既有 E2E 回归无新增失败

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0abec7b3affeYQSCb9ywAID20G`) — 全部基线主张经实时仓库核实为真（22 xbiz reverseApprove / ErpPurRequisition 继承 / ErpInvCostAdjust Processor / 零 reverseApprove 按钮 / 23 实体清单 / visibleOn 语义）。2 MAJOR：M1 reverseApprove 终态实为 REJECTED 非 UNSUBMITTED（经 `ErpPurOrderProcessor.java:239` + `approval-support.xbiz` 核实，3 处订正）/ M2 域分布 purchase 6→5（ErpPurRequisition 经继承不在 22 显式声明中，自相矛盾）。2 MINOR：m1 批次 1 实体按钮插入位（reject/cancel 之间）未明确 / m2 confirmText `{entityLabel}` 占位符非批次 1/2 范式（改硬编码中文实体名）。全部修复。
- Independent draft review iteration 2: `needs revision` (`ses_0abe54ba3ffeNr7pYQQVXghmzf`) — iteration 1 四项修复全部经实时仓库核实正确（reverseApprove 终态 REJECTED 经 `ErpPurOrderProcessor.java:239` + Java 测试确认 / purchase 5 xbiz 计数核实 / 按钮插入位两批次分别明确 / confirmText 硬编码范式对齐）。1 MAJOR 残留：line 70 Non-Goals withdrawApproval 段仍有 `APPROVED → UNSUBMITTED` 遗漏（iteration 1 声称 3 处订正但此第 4 处遗漏，与 line 26 矛盾）。当场修正。
- Independent draft review iteration 3: `accept` — iteration 2 line 70 残留修正后，全 plan 无 UNSUBMITTED 误用（5 处 UNSUBMITTED 引用均为正确语境：submit visibleOn 表达式 / withdrawApproval 语义 / 审查记录历史叙述），reverseApprove 终态 APPROVED→REJECTED 全文一致。无 BLOCKER / 无 MAJOR / 无 MINOR。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 既有 Playwright E2E 回归一次。

- [x] 范围内行为完成（23 实体 reverseApprove 按钮接线）
- [x] 相关文档对齐（reverseApprove rowActions 范式已由批次 1/2 确立为本项目既定约定，本批次纯复制推广，无新约定需更新）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + 23 view.xml `xmllint --noout` well-formed + 既有 E2E 回归无新增失败）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### withdrawApproval（撤回提交）按钮

- Classification: `out-of-scope improvement` → **已由 successor `2026-07-12-0900-1` 全部落地（completed）**
- Why Not Blocking Closure: withdrawApproval 语义为提交人撤回自己的提交（SUBMITTED → UNSUBMITTED），不同于 reverseApprove（审批人撤销审批）。本计划仅覆盖审批人侧反审批入口。
- Successor Required: `yes`（触发条件：撤回提交用户面需求落地时）
- Successor Delivered: ✅ plan `2026-07-12-0900-1-withdraw-approval-action-buttons-batch4.md`（23 实体 withdrawApproval 按钮全接线，结束审计通过）

### drawer / edit 表单内审批入口

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 与批次 2 同口径，仅 rowActions 列表页入口。form 内审批入口属不同交互面。
- Successor Required: `yes`（触发条件：表单内审批入口用户面需求落地时）

### useWorkflow (xwf) 实体完整审批轴浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 4 个 `useWorkflow="true"` 实体按钮已接线（mutation 经 GraphQL 可调用），但完整 submit→approve→reverseApprove 浏览器层 E2E 经 2330-1 权威裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）。需 nop-entropy 平台变更。
- Successor Required: `yes`（触发条件：nop-entropy 平台支持浏览器层测试用户身份映射时）

## Closure

Status Note: **计划已执行完成**（结束审计通过）。Phase 1（purchase 6 + sales 6 共 12 实体）+ Phase 2（assets 5 + finance 2 + manufacturing 1 + quality 1 + inventory 1 + hr 1 共 11 实体）共 23 实体定制层 `<Entity>.view.xml` 的 `<crud name="main"><rowActions x:override="bounded-merge">` 已接线 `row-reverse-approve-button`（反审批），调 `@mutation:<Entity>__reverseApprove?id=$id` + `confirmText`（按实体硬编码中文实体名）+ `visibleOn="${approveStatus == 'APPROVED'}"`，镜像批次 1/2 范式（批次 1 两实体 reject→reverseApprove→cancel；批次 2 实体 reject→reverseApprove→delete）。验证：`mvn install -DskipTests` 154 模块 BUILD SUCCESS（exit 0，01:26 min）；23 view.xml `xmllint --noout` 全 well-formed；按钮接线为静态资源，mutation 调用经既有 P2P/O2C/approval-gated E2E 间接覆盖。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_0abd85676ffeRJs6XfIMaGHIbE`，general，无执行者上下文）
- Verdict: `pass` / Recommendation: `close`
- 核查结论：23/23 实体含按钮；button spec（id/label=反审批/level=info/icon=fa fa-undo/mutation 匹配实体名/visibleOn=APPROVED/confirmText 硬编码中文实体名零占位符）全通过；ordering（batch1 reject→reverseApprove→cancel、batch2 reject→reverseApprove→delete）正确；23/23 xmllint well-formed；mutation 可达性 22 xbiz 显式 + ErpPurRequisition 经 approval-support.xbiz 继承；零 _gen 生成文件被改、bounded-merge 全用；BUILD SUCCESS 确认。无 ISSUES。

Follow-up:

- ~~withdrawApproval 按钮 successor~~ — ✅ 已由 `2026-07-12-0900-1` 全部落地
- drawer/edit 表单审批入口 successor（见上方 Deferred）
- useWorkflow (xwf) reverseApprove 按钮 successor（见上方 Deferred）
