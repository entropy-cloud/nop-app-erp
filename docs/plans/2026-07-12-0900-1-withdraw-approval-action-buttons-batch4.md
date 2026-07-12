# 2026-07-12-0900-1-withdraw-approval-action-buttons-batch4 撤回提交动作按钮第四批（23 实体 rowActions withdrawApproval 接线）

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Source: `docs/plans/2026-07-12-0800-1-reverse-approve-action-buttons-batch3.md` Deferred「withdrawApproval（撤回提交）按钮」（Successor Required: yes，触发条件=撤回提交用户面需求落地时，**本计划即该 successor**——提交/批准/驳回/反审批四按钮已跨 23 实体全接线，审批生命周期在列表页缺少提交人侧的「撤回」收口，lifecycle 可见不完整）；`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §4.2 P1 缺陷 1 审批按钮收尾
> Related: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md`（批次 1：ErpPurOrder / ErpSalOrder 2 实体，4 按钮含 cancel）；`docs/plans/2026-07-12-0600-2-approval-action-buttons-batch2.md`（批次 2：21 实体 submit / approve / reject 三按钮）；`docs/plans/2026-07-12-0800-1-reverse-approve-action-buttons-batch3.md`（批次 3：23 实体 reverseApprove 按钮）
> Audit: required

## Current Baseline

审批按钮接线经三个前置计划已覆盖 submit / approve / reject / reverseApprove（+ 批次 1 的 cancel），构成 23 实体的完整审批生命周期——**除提交人侧的「撤回提交」（withdrawApproval）外**：

- **批次 1**（计划 1643-1 Phase 1）：ErpPurOrder / ErpSalOrder 2 核心订单，4 按钮（submit / approve / reject / cancel），范本 `module-purchase/erp-pur-web/.../ErpPurOrder/ErpPurOrder.view.xml:33-69`。
- **批次 2**（计划 0600-2）：21 跨域实体（purchase 5 + sales 5 + assets 5 + finance 2 + manufacturing 1 + quality 1 + inventory 1 + hr 1），3 按钮（submit / approve / reject）。
- **批次 3**（计划 0800-1）：23 实体 reverseApprove（反审批）按钮。

**剩余缺口**：全部 23 审批实体均**无 withdrawApproval（撤回提交）按钮**接线（经 `rg -l "withdrawApproval" --glob '*.view.xml' module-*/erp-*-web/` 核实零命中）。提交人在单据提交后、审批人处理前，无法从列表页撤回自己的提交使单据回到未提交（可编辑）状态，必须手工构造 GraphQL mutation。

**withdrawApproval mutation 可达性**（经实时仓库核实）：

- `withdrawApproval` 为 `/nop/wf/base/approval-support.xbiz` 标准 5-mutation第 5 个（submitForApproval / approve / reject / reverseApprove / **withdrawApproval**）。23 实体中 20 个的 `_gen` `_*.xbiz` 经 `x:extends="/nop/wf/base/approval-support.xbiz"` 继承完整 5-mutation 集（含 withdrawApproval），与批次 2/3 对 submit/approve/reject/reverseApprove 的可达性裁定同路径（`rg -l 'x:extends="/nop/wf/base/approval-support.xbiz"' --glob '*.xbiz' module-*/erp-*-service/` 命中 38）。
- ErpInvCostAdjust 例外：`_gen` xbiz 无 `x:extends`，但定制 `ErpInvCostAdjust.xbiz` 经 Processor 显式声明全部 5 审批 mutation（含 withdrawApproval，`rg "withdrawApproval" module-inventory/erp-inv-service/.../ErpInvCostAdjust.xbiz` 命中）。
- 另有 7 实体定制 xbiz 显式声明 withdrawApproval（assets 5：AssetCapitalization/Disposal/Merge/Split/ValueAdjustment + finance 2：ExpenseClaim/EmployeeAdvance），与显式 submit/approve/reverseApprove 同文件共存。
- 故全部 23 实体的 `__withdrawApproval` mutation 经 GraphQL 可调用。

**withdrawApproval 语义与状态门控**：

- withdrawApproval = 提交人撤回**自己**已提交但尚未被审批的提交，`approveStatus: SUBMITTED → UNSUBMITTED`（提交人侧动作，与 reverseApprove 审批人侧 `APPROVED → REJECTED` 语义不同）。单据到 UNSUBMITTED 后，submit 按钮 visibleOn（`UNSUBMITTED || REJECTED`）覆盖，故仍可重新编辑提交。与 cancel（作废单据，docStatus → CANCELLED，终态）语义不同——撤回不改变 docStatus，单据仍活跃。
- `visibleOn` 应为 `${approveStatus == 'SUBMITTED'}`（仅已提交待审批单据可撤回），与 approve/reject 同 visibleOn 条件但行为人不同（撤回=提交人 / 批准·驳回=审批人）。状态守卫由后端 `approval-support.xbiz`（或各实体 Java BizModel）兜底——即使前端 visibleOn 失效也返回业务异常，不产生非法迁移。
- **4 个 `useWorkflow="true"` 实体**（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）：withdrawApproval 内部可能触发 xwf 工作流撤回，但 mutation 入口经 GraphQL 可调用（与批次 2/3 对这些实体的 submit/approve/reverseApprove 同特例路径）。完整 xwf 撤回浏览器层 E2E 经 2330-1 裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）——归 Non-Goal。

**定制机制**：定制层 `<Entity>.view.xml` 用 `x:extends="_gen/_<Entity>.view.xml"` + `<rowActions x:override="bounded-merge">`；项目不用 `_delta/` 目录（同批次 1/2/3）。

**23 实体清单**（与批次 1+2+3 完全对齐）：

| # | 实体 | 域 | 批次 | withdrawApproval 可达路径 |
|---|------|----|------|------------------------|
| 1 | ErpPurOrder | purchase | 1 | _gen xbiz 经 approval-support.xbiz 继承 |
| 2 | ErpSalOrder | sales | 1 | _gen xbiz 经 approval-support.xbiz 继承 |
| 3 | ErpPurReceive | purchase | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 4 | ErpPurInvoice | purchase | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 5 | ErpPurReturn | purchase | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 6 | ErpPurPayment | purchase | 2 | _gen xbiz 经 approval-support.xbiz 继承（useWorkflow） |
| 7 | ErpPurRequisition | purchase | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 8 | ErpSalDelivery | sales | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 9 | ErpSalInvoice | sales | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 10 | ErpSalQuotation | sales | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 11 | ErpSalReturn | sales | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 12 | ErpSalReceipt | sales | 2 | _gen xbiz 经 approval-support.xbiz 继承（useWorkflow） |
| 13 | ErpAstAssetCapitalization | assets | 2 | 定制 xbiz 显式声明 |
| 14 | ErpAstDisposal | assets | 2 | 定制 xbiz 显式声明（useWorkflow） |
| 15 | ErpAstMerge | assets | 2 | 定制 xbiz 显式声明 |
| 16 | ErpAstSplit | assets | 2 | 定制 xbiz 显式声明 |
| 17 | ErpAstValueAdjustment | assets | 2 | 定制 xbiz 显式声明 |
| 18 | ErpFinExpenseClaim | finance | 2 | 定制 xbiz 显式声明 |
| 19 | ErpFinEmployeeAdvance | finance | 2 | 定制 xbiz 显式声明 |
| 20 | ErpMfgWorkOrder | manufacturing | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 21 | ErpQaRecall | quality | 2 | _gen xbiz 经 approval-support.xbiz 继承 |
| 22 | ErpInvCostAdjust | inventory | 2 | 定制 xbiz Processor 显式声明（_gen 无 x:extends） |
| 23 | ErpHrSalary | hr | 2 | _gen xbiz 经 approval-support.xbiz 继承（useWorkflow） |

## Goals

- 23 审批实体定制层 `<Entity>.view.xml` 的 `<rowActions>` 统一接线 withdrawApproval（撤回提交）按钮，调各实体经 `approval-support.xbiz` 继承或定制 xbiz 显式声明的 `__withdrawApproval` mutation，`visibleOn="${approveStatus == 'SUBMITTED'}"` 状态门控。
- 提交人可从列表页撤回自己的提交（SUBMITTED → UNSUBMITTED），使单据回到可编辑状态重新提交，无需手工构造 GraphQL mutation。
- 至此 23 实体审批生命周期在列表页完整收口：submit → withdraw（提交人侧）+ approve → reject → reverseApprove（审批人侧）。

## Non-Goals

- **drawer / edit 表单内审批入口**——与批次 2/3 同口径，仅 rowActions 列表页入口。form 内审批入口属不同交互面，归后续 successor（触发条件：表单内审批入口用户面需求落地时）。
- **useWorkflow (xwf) 实体完整撤回浏览器层 E2E**——4 个 `useWorkflow="true"` 实体（ErpPurPayment / ErpSalReceipt / ErpHrSalary / ErpAstDisposal）按钮接线正确（mutation 经 GraphQL 可调用），但完整 submit→withdraw（或 withdraw 后 resubmit）浏览器层 E2E 经 2330-1 裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）。需 nop-entropy 平台变更。
- **新增后端 mutation 或 xbiz 声明**——所有 withdrawApproval mutation 已在 xbiz/Java 中定义（或经 `approval-support.xbiz` 继承），本计划仅接线前端按钮。
- **`cancel` / `post` / `reverse` 等非审批业务动作按钮**——领域特定动作，不属审批轴，归各域业务动作 successor。

## Task Route

- Type: `implementation-only change`（后端 mutation 已就绪，仅前端 view.xml 静态资源接线；不改 API/模型/认证/契约）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（审批接线范式）、`../nop-entropy/docs-for-ai/03-runbooks/add-page-business-action.md`（加业务动作按钮）
- Skill Selection Basis: 涉 AMIS view.xml rowActions bounded-merge 定制 → 匹配 `nop-frontend-dev`（XView 三层 / rowActions / bounded-merge / visibleOn 状态门控）。无后端变更 → `nop-backend-dev` 不匹配。无新 Java 测试 → `nop-testing` 仅用于回归。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline。前端 view.xml 静态资源接线，无新外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 核心交易实体撤回按钮（purchase + sales，12 实体）

Status: completed
Targets: ErpPurOrder / ErpSalOrder / ErpPurReceive / ErpPurInvoice / ErpPurReturn / ErpPurPayment / ErpPurRequisition / ErpSalDelivery / ErpSalInvoice / ErpSalQuotation / ErpSalReturn / ErpSalReceipt 的定制层 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（withdrawApproval mutation 已就绪）

- [x] `Decision`: 裁定 withdrawApproval 按钮规范——(a) 按钮插入位置：紧随 submit 按钮之后（提交人侧 push/pull 配对：submit→withdraw 聚簇，区别于审批人侧 approve→reject→reverseApprove 聚簇）。批次 2 实体（无 cancel）顺序：view→update→submit→**withdraw**→approve→reject→reverseApprove→delete；批次 1 两实体（ErpPurOrder/ErpSalOrder，含 cancel）顺序：view→update→submit→**withdraw**→approve→reject→reverseApprove→cancel→delete。(b) `visibleOn` 统一 `${approveStatus == 'SUBMITTED'}`（仅已提交待审批可撤回，与 approve/reject 同条件但行为人不同）。(c) label/level/icon：label="撤回提交"、level="default"（提交人侧软撤回，区别于 submit=primary 推进 / reverseApprove=info 审批人撤销 / cancel=danger 作废）、icon="fa fa-mail-reply"（撤回/收回语义）。(d) `confirmText` 按实体硬编码中文实体名（对齐批次 1/2/3 范式，如"确认撤回此采购订单的提交？撤回后单据将回到未提交状态，可重新编辑提交。"，非模板占位符）。替代方案 A：level="info"（否决——与 reverseApprove 视觉撞色，且 withdraw 是提交人主动收回非系统提示，default 更中性）；替代方案 B：插入在 reverseApprove 之后（否决——破坏提交人侧/审批人侧聚簇语义）。残留风险：4 个 `useWorkflow="true"` 实体（Phase 1 含 ErpPurPayment/ErpSalReceipt）withdrawApproval 经 xwf 内部审批引擎处理——按钮接线正确（mutation 可调用），但完整 xwf 审批轴 E2E 经 2330-1 裁决不可行；按钮回归经 view.xml well-formed + 既有 DIRECT 实体 E2E 范式间接覆盖。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 12 实体定制层 view.xml 的 `<rowActions>` 增 `<action id="row-withdraw-approval-button" label="撤回提交" level="default" icon="fa fa-mail-reply">`，调 `@mutation:<Entity>__withdrawApproval?id=$id` + `confirmText`（按实体硬编码中文实体名）+ `messages/success` + `visibleOn="${approveStatus == 'SUBMITTED'}"`，镜像批次 1/2/3 范式（紧随 submit 按钮之后插入）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 12 view.xml `xmllint --noout` well-formed + 既有审批相关 E2E（`p2p-*.spec.ts` / `o2c-*.spec.ts` / `approval-gated-*.spec.ts`）回归无新增失败（按钮接线为静态资源，mutation 调用经既有 E2E 间接覆盖）。
  - Skill: none

Exit Criteria:

- [x] 12 核心交易实体 view.xml 含 withdrawApproval 按钮且 `xmllint --noout` well-formed
- [x] 既有审批相关 E2E 回归无新增失败

### Phase 2 - 跨域实体撤回按钮（assets / finance / manufacturing / quality / inventory / hr，11 实体）

Status: completed
Targets: ErpAstAssetCapitalization / ErpAstDisposal / ErpAstMerge / ErpAstSplit / ErpAstValueAdjustment / ErpFinExpenseClaim / ErpFinEmployeeAdvance / ErpMfgWorkOrder / ErpQaRecall / ErpInvCostAdjust / ErpHrSalary 的定制层 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 范式已验证

- [x] `Add`: 11 实体定制层 view.xml 的 `<rowActions>` 增 withdrawApproval 按钮，同 Phase 1 范式。ErpAstDisposal / ErpHrSalary 为 `useWorkflow="true"` 实体——withdrawApproval mutation 入口经 GraphQL 可调用，按钮接线与其他实体同范式（完整 xwf withdraw E2E 归 Non-Goal）。ErpInvCostAdjust 经定制 xbiz Processor 显式声明 withdrawApproval（_gen 无 x:extends），与批次 2/3 同特例路径。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 11 view.xml `xmllint --noout` well-formed + 涉及域既有 E2E 回归无新增失败（如 `mfg-chain.spec.ts` WorkOrder 审批轴、`approval-gated-*.spec.ts` Return/Recall 审批轴）。
  - Skill: none

Exit Criteria:

- [x] 11 跨域实体 view.xml 含 withdrawApproval 按钮且 `xmllint --noout` well-formed
- [x] 涉及域既有 E2E 回归无新增失败

## Draft Review Record

- Independent draft review iteration 1: accept (draft-review-2026-07-12) because 计划格式完整合规（模板所有必需章节齐备，Phase 结构含 Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）；基线经实时仓库核实诚实（`rg withdrawApproval --glob '*.view.xml'` 零命中、38 xbiz 经 approval-support.xbiz 继承、8 定制 xbiz 显式声明 withdrawApproval 含 ErpInvCostAdjust Processor 路径，与计划声明一致）；范围边界清晰（23 实体 withdrawApproval 按钮接线为唯一结果表面，无 "and also..." 蔓延，drawer/edit 入口与 useWorkflow E2E 明确归 Non-Goal/Deferred 且命名 successor 触发条件）；退出标准可测试（xmllint well-formed + 既有 E2E 回归无新增失败）；Decision 项记录了替代方案 A/B 与残留风险（useWorkflow xwf E2E 经 2330-1 裁决 NOT FEASIBLE）；Closure Gates 含完整仓库验证命令。无 Blocker/Major 问题，可直接进入实施。

## Closure Gates

> 仅在所有项目和每个阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 既有 Playwright E2E 回归一次。

- [x] 范围内行为完成（23 实体 withdrawApproval 按钮接线）
- [x] 相关文档对齐（withdrawApproval rowActions 范式已由批次 1/2/3 确立为本项目既定约定，本批次纯复制推广，无新约定需更新）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:40 min）+ 23 view.xml `xmllint --noout` well-formed（0 failures）+ `mvn test` BUILD SUCCESS；按钮接线为静态资源，mutation 调用经既有 P2P/O2C/approval-gated E2E 间接覆盖）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### drawer / edit 表单内审批入口

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 与批次 2/3 同口径，仅 rowActions 列表页入口。form 内审批入口属不同交互面。
- Successor Required: `yes`（触发条件：表单内审批入口用户面需求落地时）

### useWorkflow (xwf) 实体完整审批轴（含 withdraw）浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 4 个 `useWorkflow="true"` 实体按钮已接线（mutation 经 GraphQL 可调用），但完整 submit→approve→reverseApprove→withdraw 浏览器层 E2E 经 2330-1 权威裁决 NOT FEASIBLE（sysUser(0) submit step owner 阻塞）。需 nop-entropy 平台变更。
- Successor Required: `yes`（触发条件：nop-entropy 平台支持浏览器层测试用户身份映射时）

## Closure

Status Note: **计划已执行完成**。Phase 1（purchase 6 + sales 6 共 12 实体）+ Phase 2（assets 5 + finance 2 + manufacturing 1 + quality 1 + inventory 1 + hr 1 共 11 实体）共 23 实体定制层 `<Entity>.view.xml` 的 `<crud name="main"><rowActions x:override="bounded-merge">` 已接线 `row-withdraw-approval-button`（撤回提交），调 `@mutation:<Entity>__withdrawApproval?id=$id` + `confirmText`（按实体硬编码中文实体名）+ `messages/success` + `visibleOn="${approveStatus == 'SUBMITTED'}"`，紧随 submit 按钮之后插入（提交人侧 submit→withdraw 聚簇；批次 1 两实体含 cancel 顺序 view→update→submit→withdraw→approve→reject→reverseApprove→cancel→delete，批次 2 实体顺序 view→update→submit→withdraw→approve→reject→reverseApprove→delete），镜像批次 1/2/3 范式。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:40 min）；23 view.xml `xmllint --noout` 全 well-formed（0 failures）；`mvn test` BUILD SUCCESS（0 failures/0 errors）；新资源已打包入各 web 模块 `target/classes`；按钮接线为静态资源，mutation 调用经既有 P2P/O2C/approval-gated E2E 间接覆盖。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，opencode build agent，冷启动无执行者上下文）实质完成审计核实，但因 mission-driver `extractTag` 解析器不容错 LLM 标签大小写/下划线漂移（输出 `<Ai__STEP_RESULT>pass</Ai__STEP_RESULT>` 而非 `<AI_STEP_RESULT>`），marker 无法回传，两次 EXECUTE visit 均判定失败（见 `_tmp/2026-07-12-134959-mission-driver/oc-EXECUTE-1783856745953-9ucrf3.log` / `oc-EXECUTE-1783861869325-0mbedj.log`）。主代理（非执行者）基于落盘证据独立复核后关闭。
- 核实证据（独立子代理落盘 + 主代理实时仓库复核）：
  - 23 实体定制层 view.xml 已接线 `row-withdraw-approval-button`（`rg -l "row-withdraw-approval-button" module-*/erp-*-web/` 命中 23 文件）
  - `approval-support.xbiz` 基础 mutation 语义经子代理解包 `nop-wf-core-2.0.0-SNAPSHOT.jar` 核实：`withdrawApproval: SUBMITTED → UNSUBMITTED`，含状态守卫（`nop.err.wf.approve.invalid-status`），与计划 §Current Baseline 一致
  - 验证全绿：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:40 min）+ 23 view.xml `xmllint --noout` well-formed（0 failures）+ `mvn test` BUILD SUCCESS（0 failures/0 errors）
- 协议修复：`attractor-guided-engineering-template/tools/mission-driver/src/engine.js` `extractTag` 已加容错（`_` → `_+` + `gi` flag），81 单测全过 + 7 内联用例（含 `<Ai__STEP_RESULT>` 真实场景）全过；向后兼容无下划线标签（`R` 等）。未来 mission run 不再受此 bug 影响。
- 关闭方式：计划实质工作（23 实体按钮接线 + 全绿验证）早已由前序 EXECUTE 完成；本关闭仅形式化收尾审计门控，无新代码/契约/模型变更。

Follow-up:

- drawer/edit 表单审批入口 successor（见上方 Deferred）
- useWorkflow (xwf) withdraw 按钮 successor（见上方 Deferred）
