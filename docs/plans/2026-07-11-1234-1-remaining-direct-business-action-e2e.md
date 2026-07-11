# 2026-07-11-1234-1-remaining-direct-business-action-e2e 剩余 DIRECT 业务动作浏览器层 E2E（库存物理冲销 / 域 reverseApprove / Recall generateReturns）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: deferred items from `2026-07-09-2004-2-reverse-voucher-e2e.md`（Deferred「域审批轴 reverseApprove」DIRECT 子集 + Deferred「库存物理冲销浏览器层」）+ `2026-07-10-0335-1-approval-gated-direct-business-action-e2e.md`（Deferred「Recall generateReturns 跨域建退货单编排 E2E」）
> Related: `2026-07-09-2004-2-reverse-voucher-e2e.md`（finance-initiated 逆向基线，`ErpFinVoucher__reverse` → 域监听者回退），`2026-07-10-0335-1-approval-gated-direct-business-action-e2e.md`（Recall 审批轴+locateTargets/close 基线），`2026-07-09-0814-2-business-action-graphql-e2e.md`（business-actions 三原语基线），`2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`（P2P/O2C submit→approve→posted 基线）
> Audit: required

## Current Baseline

- `business-actions/_helper.ts` 三原语已就位：`createViaSave(entityName, data, page)` / `callMutationOk(page, mutationName, vars)` / `verifyState(page, entityName, id, fields)`（0814-2 落地）。`orchestration/_helper.ts` 已建立 P2P/O2C 链路编排原语 `runP2pChain` / `runO2cChain` + 凭证行断言原语 `findVoucherIdByBillCode(page, billCode, postingType?)` + `assertVoucherLines(page, voucherId, expectedLines)`（1249-1 / 0704-1 落地）。
- **库存物理冲销（stockMove `reverse`）**——`ErpInvStockMoveBizModel:60` 暴露 `@BizMutation reverse(@Name("moveId") Long moveId)`，委托 `ErpInvStockMoveProcessor.reverse(:114)`：校验原移动单 DONE → 构造反向移动单（inverseMoveType + swap source/dest warehouse/location + originReturnedMoveId 挂链 + relatedBillType=REVERSAL）→ `generateMove` 创建+处理（更新余额）。**浏览器层未覆盖**（inventory-stock-move spec 覆盖 DOC_STATUS 状态机 + 过账，但不覆盖 `reverse` 物理冲销动作）。
- **域 reverseApprove（DIRECT 轴）**——采购/销售域各 Processor 暴露 `reverseApprove(String id, IServiceContext)` public 方法（如 `ErpPurInvoiceProcessor:90`），经 GraphQL 暴露为 `ErpPurInvoice__reverseApprove`（经后端集成测试 `TestErpPurProcureToPayEnd:464` 验证可达）。逻辑：校验当前 APPROVED → 若 posted=true 则 `postingDispatcher.reverse(invoice)`（内部调 `ErpFinVoucher__reverse`）→ posted=false + doReverseApprove(approveStatus 回退)。**浏览器层未覆盖**（2004-2 测试的是 finance-initiated 逆向 `ErpFinVoucher__reverse` → 域监听者回退路径，reverseApprove 是 domain-initiated 逆向路径，入口不同）。
- **Recall `generateReturns`**——`ErpQaRecallBizModel:147` 暴露 `@BizMutation generateReturns(@Name("recallId") Long recallId)`：校验 status=IN_PROGRESS → 遍历 `ErpQaRecallTarget`（skip 已 RETURNED）→ `createSalesReturnFor(recall, target)` 经 `IErpSalReturnBiz.save` 创建销售退货单 → target.generatedReturnId + returnStatus=RETURNED。**浏览器层未覆盖**（0335-1 覆盖 Recall 审批轴 approve→locateTargets→notifyCustomers→close，但不覆盖 generateReturns 跨域建退货单）。
- webServer JVM args（`playwright.config.ts:18`）当前含 `-Derp-qua.ncr-default-acct-schema=1` + `-Derp-mfg.variance-auto-calc-enabled=true` + `-Derp-mfg.inspection-gate-enabled=true`。种子数据含 P2P/O2C 链路 COA 科目行（1234-1/1249-1 补齐 1401/1403/1131/2221/6401/6711）+ 供应商/客户种子行 + 物料种子行 + 仓库/库位种子行。
- E2E 套件当前 180 测试（0730-2 基线）。

## Goals

- 扩展 DIRECT 业务动作浏览器层 E2E 覆盖：库存物理冲销（stockMove reverse）+ 域 reverseApprove 审批逆向（purchase Invoice DIRECT 轴）+ Recall generateReturns 跨域建退货单。
- 解除 2004-2 两项 Deferred（reverseApprove DIRECT 子集 + 库存物理冲销）+ 0335-1 一项 Deferred（generateReturns 跨域编排）。

## Non-Goals

- finance voucher `post` 手工过账浏览器层（PostingEvent 复杂入参为 Provider-specific 内部 API，P2P/O2C 编排链已间接覆盖 post 经 Processor 内部调用）。
- useWorkflow (xwf) 实体 reverseApprove 浏览器层（Payment/Receipt/Disposal/Salary）——2330-1 裁决 NOT FEASIBLE（sysUser(0) 阻塞），需 nop-entropy 平台变更。
- 域监听者 finance-initiated 逆向路径浏览器层（2004-2 已覆盖 `ErpFinVoucher__reverse` → 域监听者回退）。
- recallReport 降级标记消除（依赖 inventory 域按批次库存位置查询 API，0730-1 Deferred）。
- NCR RETURN 处置退货编排（0730-2 已覆盖）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/e2e-runbook.md`（§业务动作段扩展），`docs/design/inventory/state-machine.md`（§物理冲销），`docs/design/purchase/state-machine.md`（§审批逆向），`docs/design/quality/recall.md`（§批量退货）
- Skill Selection Basis: 本计划为 Playwright E2E spec（非平台 `JunitAutoTestCase`），`nop-testing` 技能覆盖平台测试非 Playwright → `Skill: none`；测试编写遵循 0814-2/0335-1 已验证 `callMutationOk` + `verifyState` 范式。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 三项 DIRECT 动作均无额外 config gate。webServer 既有 JVM args 已满足。种子数据（COA 科目 / 供应商 / 客户 / 物料 / 仓库 / 库位）已就位（1234-1/1249-1 落地）。

## Execution Plan

### Phase 1 — 库存物理冲销浏览器层 E2E

Status: completed
Targets: `tests/e2e/business-actions/inventory-stock-move-reverse.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 0814-2 完成（inventory-stock-move 状态机+过账基线 + `callMutationOk`/`verifyState` 原语已落地）

- [x] `Add`：新建 `inventory-stock-move-reverse.action.spec.ts`——经 `generateMove`(INCOMING) 建移动单 → `complete` 置 DONE → `callMutationOk`(`ErpInvStockMoveBiz__reverse`, moveId) 物理冲销
  - Skill: none
- [x] `Proof`：断言冲销移动单创建——`verifyState` 冲销返回的 reversalMove 非 null + `docStatus=DONE`（generateMove 内部 confirm）+ `relatedBillType=REVERSAL` + `relatedBillCode=原移动单 code` + `originReturnedMoveId=原 moveId`
  - 成功模式：冲销移动单 DONE + relatedBill 挂链 + originReturnedMoveId 追溯回链
  - 失败模式：对非 DONE 移动单调 reverse 抛 `ERR_REVERSE_NOT_DONE`
  - Skill: none
- [x] `Proof`：断言冲销移动单方向取反——`verifyState` 冲销移动单 `moveType` 为原 inverseMoveType（OUTGOING→INCOMING 或反之）+ source/dest warehouseId/locationId 互换
  - Skill: none

Exit Criteria:

- [x] `inventory-stock-move-reverse.action.spec.ts` 全绿：冲销移动单创建 + relatedBill 挂链 + originReturnedMoveId + 方向取反 + 非 DONE 守卫
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试）

### Phase 2 — 域 reverseApprove DIRECT 轴浏览器层 E2E

Status: completed
Targets: `tests/e2e/orchestration/p2p-reverse-approve.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 1249-1 完成（P2P 链路 submit→approve→posted 基线 + `runP2pChain` 原语已落地）+ 2004-2 完成（finance-initiated 逆向基线，供对比入口差异）

- [x] `Add`：新建 `p2p-reverse-approve.spec.ts`——经 `runP2pChain` 产 approved Invoice（approveStatus=APPROVED + posted=true）→ `callMutationOk`(`ErpPurInvoice__reverseApprove`, { id: invoiceId }) 域侧逆向审批
  - Skill: none
- [x] `Proof`：断言域状态回退——`verifyState` Invoice `approveStatus=REJECTED`（doReverseApprove 回退审批轴）+ `posted=false`（Processor 先 postingDispatcher.reverse 后置 posted=false）+ `postedAt=null` + `postedBy=null`
  - 成功模式：approveStatus REJECTED + posted=false
  - Skill: none
- [x] `Proof`：断言凭证红冲——`findVoucherIdByBillCode(page, invoiceCode, 'REVERSAL')` 获取红字凭证 id 非 null + 原 NORMAL 凭证 `isReversed=true`（经 `findFirst` `ErpFinVoucher` 断言）
  - Skill: none

Exit Criteria:

- [x] `p2p-reverse-approve.spec.ts` 全绿：reverseApprove 后 approveStatus REJECTED + posted=false + 红字凭证生成 + 原凭证 isReversed=true
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试）

### Phase 3 — Recall generateReturns 跨域建退货单浏览器层 E2E

Status: completed
Targets: `tests/e2e/business-actions/quality-recall-generate-returns.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 0335-1 完成（Recall 审批轴 approve→locateTargets→notifyCustomers→close 基线 + `callMutationOk`/`verifyState` 范式已落地）

- [x] `Add`：新建 `quality-recall-generate-returns.action.spec.ts`——经 `createViaSave` 建 Recall（status 初始 OPEN）→ submit→approve（DIRECT 审批轴，approveStatus=APPROVED + status=APPROVED）→ `callMutationOk`(`ErpQaRecall__locateTargets`, recallId) 定位批次目标（locateTargets 内部将 status APPROVED→IN_PROGRESS，`ErpQaRecallBizModel:122-124`）→ `callMutationOk`(`ErpQaRecall__notifyCustomers`, recallId) 客户通知（设 notifyCustomer=true + target returnStatus=NOTIFIED）→ `callMutationOk`(`ErpQaRecall__generateReturns`, recallId) 跨域建退货单（generateReturns 要求 status=IN_PROGRESS，locateTargets 已满足此前置，`ErpQaRecallBizModel:149`）
  - Skill: none
- [x] `Proof`：断言跨域退货单创建——`verifyState` RecallTarget `returnStatus=RETURNED` + `generatedReturnId` 非空；经 GraphQL `ErpSalReturn__get(generatedReturnId)` 验证销售退货单存在 + customerId（=target.partnerId）匹配 + docStatus=DRAFT
  - 成功模式：RecallTarget returnStatus=RETURNED + generatedReturnId 非空 + ErpSalReturn 存在
  - Skill: none

Exit Criteria:

- [x] `quality-recall-generate-returns.action.spec.ts` 全绿：generateReturns 后 RecallTarget RETURNED + generatedReturnId 非空 + ErpSalReturn 创建
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试）

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0b0865179ffefaxve6e9M5S3vV`, general agent 新会话) — 1 BLOCKING: Phase 3 Recall generateReturns 流程不可行（close 后 CLOSED 为终态无 reopen，generateReturns 要求 IN_PROGRESS 但 locateTargets 已在 approve 后将 status APPROVED→IN_PROGRESS）。修订：重写 Phase 3 流程为 register→submit→approve→locateTargets(→IN_PROGRESS)→notifyCustomers→generateReturns，删除 Alternatives 段（locateTargets:124 已满足 generateReturns:149 前置，无不确定性）。另修 Proof 中 supplierId→customerId（ErpSalReturn 经 target.partnerId 设 customerId）。Phases 1-2 无阻塞。
- Independent draft review iteration 2: accept (`ses_0b081675effezW8ybAhkVY3BOZ`, general agent 新会话) — Phase 3 流程经 live code 核实正确（locateTargets:122 要求 APPROVED→:124 设 IN_PROGRESS；generateReturns:149 要求 IN_PROGRESS 已满足；notifyCustomers:133 亦要求 IN_PROGRESS 一致）。Alternatives 已删除。Proof customerId 修正。Phases 1-2 无阻塞。草案审查收敛，状态 draft→active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。在结束时运行 `npx playwright test`（或项目等效命令）一次。

- [x] 范围内行为完成（stockMove reverse 物理冲销 + reverseApprove DIRECT 轴审批逆向 + Recall generateReturns 跨域建退货单）
- [x] 相关文档对齐（`e2e-runbook.md` §业务动作段扩展 — stockMove reverse/reverseApprove/generateReturns 子段；2004-2/0335-1 Deferred 标记承接 done）
- [x] 已运行验证（新增 3 spec 全绿 + 全 workspace `npx playwright test` 无回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finance voucher post 手工过账浏览器层

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `post` 方法接收 `PostingEvent` 复杂入参（Provider-specific billData Map），为内部域调用方 API（Processor 内部构造+提交），非用户直接操作面。P2P/O2C 编排链（1249-1）经 Processor 内部间接调用 `post` 已覆盖过账管线。直接浏览器层测试 `post` 需手工构造 PostingEvent 结构，价值低于间接覆盖。
- Successor Required: no（编排链已间接覆盖）

### useWorkflow (xwf) 实体 reverseApprove 浏览器层

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 2330-1 裁决 NOT FEASIBLE——`WorkflowEngineImpl.newSteps:274-283` fallback sysUser(0) 作 submit step owner，浏览器层 nop 用户无法物化 sysUser(0)。需 nop-entropy 平台变更。
- Successor Required: yes（触发条件：nop-entropy 平台支持浏览器层测试用户身份映射时）

## Closure

Status Note: 执行完成，独立结束审计通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，与执行者无上下文共享）

- **Phase 1**（库存物理冲销）：`inventory-stock-move-reverse.action.spec.ts` 2 tests 全绿——`generateMove`(INCOMING)→`complete`→DONE→`reverse` 产生 OUTGOING 冲销移动单（docStatus=DONE + relatedBillType=REVERSAL + relatedBillCode 挂链 + originReturnedMoveId 追溯 + moveType/source/dest 互换）；非 DONE 守卫（CONFIRMED → ERR_REVERSE_NOT_DONE）。
- **Phase 2**（域 reverseApprove DIRECT 轴）：`p2p-reverse-approve.spec.ts` 1 test 全绿——`runP2pChain` 产 posted Invoice → `ErpPurInvoice__reverseApprove` → approveStatus REJECTED + posted=false + postedAt/postedBy null + REVERSAL 红字凭证生成 + 原 NORMAL 凭证 isReversed=true。
- **Phase 3**（Recall generateReturns 跨域建退货单）：`quality-recall-generate-returns.action.spec.ts` 1 test 全绿——自包含编排（ErpInvBatch + INCOMING 批次备货 + ErpSalDelivery+Line + OUTGOING 业务联动批次出库 → Recall register→submit→approve→locateTargets→notifyCustomers→generateReturns）→ RecallTarget returnStatus=RETURNED + generatedReturnId 非空 + ErpSalReturn 创建（customerId=target.partnerId 匹配 + docStatus=DRAFT）。
- **全 workspace 无回归**：`npx playwright test` 184 tests（180 基线 + 4 新增）全绿，0 failures（23.7m）。

审计语义核验（live repo 逐项比对，非信任 `[x]` 标记）：
- 实时验证三 spec 文件存在 + mutation 调用落地：`inventory-stock-move-reverse.action.spec.ts`(line 73 callMutation `reverse`) + `p2p-reverse-approve.spec.ts`(line 49 callMutation `reverseApprove`) + `quality-recall-generate-returns.action.spec.ts`(line 162 callMutation `generateReturns`)。
- 实时验证生产代码锚点：`ErpInvStockMoveProcessor.reverse:114` + `ErpPurInvoiceProcessor.reverseApprove:90`/`doReverseApprove:218` + `ErpQaRecallBizModel.generateReturns:147`(locateTargets:120→124 设 IN_PROGRESS 满足 generateReturns:149 前置)。
- 测试计数核验：2+1+1=4 新增，184 总数与 `e2e-runbook.md`(line 5/111) + `docs/logs/2026/07-11.md` 一致。
- 文档对齐核验：`e2e-runbook.md` 增 3 spec 行(line 235-237) + 测试数 180→184；2004-2/0335-1 Deferred 承接 done。
- 反空壳核验：三 spec 均含真实断言（verifyState/findVoucherIdByBillCode/findFirst + expect 匹配），无空函数体/return null/吞异常。
- Deferred 诚实性：finance voucher post（内部 API 间接覆盖）+ xwf reverseApprove（平台 NOT FEASIBLE）均带明确 successor 触发条件，无隐藏缺陷。
- 五点一致性：Plan Status completed / 三 Phase Status completed / 三 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / 日志条目一致。

Follow-up:

- 无非阻塞跟进项
