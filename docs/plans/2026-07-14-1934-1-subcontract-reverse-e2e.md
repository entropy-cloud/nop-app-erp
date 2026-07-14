# 2026-07-14-1934-1-subcontract-reverse-e2e 委外红冲浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-14
> Mission: erp
> Work Item: 各域细化端到端验证（manufacturing 委外红冲 reverseCompletion 浏览器层 E2E）
> Source: `docs/plans/2026-07-14-1825-1-manufacturing-subcontract-reverse.md` Deferred「委外红冲浏览器层 E2E」（Successor Required: yes，触发条件「本计划 completed 后」——**已满足**：1825-1 Plan Status=completed，`reverseCompletion` 域级红冲动作 + `MfgSubcontractReversalListener` 后端均已落地）；`docs/plans/2026-07-14-0035-2-subcontract-lifecycle-e2e-extension.md` Deferred「委外红冲 E2E」（Successor Required: yes，触发条件「委外红冲后端 successor 落地时」——**已满足**：1825-1 即该后端 successor）
> Related: `2026-07-14-1825-1`（委外红冲后端 completed，本计划解除其 Deferred）、`2026-07-14-0035-2`（委外生命周期 E2E 扩展 completed，本计划解除其 Deferred）、`2026-07-13-0701-2`（委外 E2E + 前端 completed）、`2026-07-13-0455-1`（委外引擎 completed）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 截至 2026-07-14 19:34 +0880）：

### 委外红冲后端能力已落地（1825-1 completed）

- **`IErpMfgSubcontractOrderBiz.reverseCompletion`**（`erp-mfg-dao:72`）`@BizMutation` + `@Name("subcontractOrderId") Long` + `IServiceContext context`——IBiz 先行。
- **`ErpMfgSubcontractOrderProcessor.reverseCompletion`**（`:233`）public 入口编排，protected step 方法拆分：
  - `validateCanReverse`——COMPLETED + posted==true 前置守卫，否则 `ERR_SUBCONTRACT_CANNOT_REVERSE`（`ErpMfgErrors:247`）。
  - `reverseGlPostings`（`:261`）——经注入 `IErpFinVoucherBiz.reverse` 逐一红冲 `-SF`/`-SR`/`-SI` billHeadCode，config-gated 同正向 `erp-mfg.subcontract-posting-enabled`，失败以 try/catch 吞异常保持幂等。
  - `reverseInventoryMoves`（`:288`）——经注入 `IErpInvStockMoveBiz.reverse` 反向 issue OUTGOING + receipt MANUFACTURE 移动单。
  - `doReverseCompletion`——posted=false + postedAt=null + docStatus→CANCELLED。
- **`ErpMfgSubcontractOrderBizModel.reverseCompletion`**（`:70`）`@Override` 委托 Processor。
- **`MfgSubcontractReversalListener`**（110 行）`implements IErpFinVoucherReversedListener`——财务侧直接红冲 SUBCONTRACT_ISSUE/RECEIPT/FEE 凭证时回退委外单 posted=false + docStatus→CANCELLED。注册为 Bean（`app-service.beans.xml:55`）。
- **后端测试**：`TestErpMfgSubcontractReverse` 4 测试全绿（正路径/非法状态守卫/幂等守卫/ReversalListener 路径），mfg-service 122 tests 0 failures。

### 委外正向生命周期 E2E 已落地（0701-2 + 0035-2 completed）

- `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`（416 行）当前 7 测试：full chain（正向全链 + 3 段 GL 凭证行数值断言 + posted=true）+ DRAFT→issue 非法守卫 + MRP 释放 + 多行发料 + 部分收货 + cancel 正路径 + cancel 守卫。
- `runSubcontractChain(page, options?)`（`_helper.ts:1079`）编排单行委外链至 COMPLETED：建测试专用 component/product 物料 → 备货（setupQty=10/componentUnitCost=5）→ 建单（DRAFT, processingFee=50）+ 1 行（lineQty=2）→ submit→approve → issueMaterials（全量）→ receiveFinished（receivedQty=1）→ postProcessingFee → COMPLETED + posted=true。
- `SUBCONTRACT_EXPECT`（`:1056`）确定性期望值：setupQty=10 / componentUnitCost=5 / lineQty=2 / issueCost=10 / processingFee=50 / receivedQty=1 / receiptCost=50。
- 三段正向 GL 凭证行已断言（spec :133-155）：
  - SUBCONTRACT_ISSUE `{code}-SI` Dr 1408=issueCost(10) / Cr 1401=issueCost(10)
  - SUBCONTRACT_RECEIPT `{code}-SR` Dr 1405=receiptCost(50) / Cr 1408=receiptCost(50)
  - SUBCONTRACT_FEE `{code}-SF` Dr 1408=processingFee(50) / Cr 2202=processingFee(50)
- `cleanupSubcontract`（`_helper.ts:1237`）逆序清理链路下游产物。
- **凭证行断言原语就绪**：`findVoucherIdByBillCode(page, billCode, postingType?)`（`:100`）按 ErpFinVoucherBillR 反查凭证 id，可选按 `postingType`（NORMAL/REVERSAL）过滤——区分原/红字凭证的唯一手段；`assertVoucherLines(page, voucherId, expectedLines[])` 逐行断言 subjectCode + dcDirection + debitAmount/creditAmount。

### 缺失：委外红冲浏览器层 E2E

- `reverseCompletion` 后端已落地但**零浏览器层 E2E 覆盖**——COMPLETED 委外单红冲后的状态回退（posted=false + docStatus=CANCELLED）、三段 GL 红字凭证生成、库存反向移动、非法状态守卫均未在浏览器层验证。
- 这是 manufacturing 域**唯一**具备完整正向过账生命周期 E2E + 后端红冲能力但缺乏红冲浏览器层 E2E 的业务单据。采购/销售/库存/资产/质量域的红冲浏览器层 E2E 已分别由 `2004-2`（p2p/o2c-reverse）/`0730-2`（NCR reverse）/`1218-1`（assets VALUE_ADJUSTMENT reverse）落地。

### 实现偏离已知约束（1825-1 Closure 记录）

- MANUFACTURE 类型库存移动单反向冲销受 inventory 域 `inverseMoveType` 限制（MANUFACTURE 不反转），receipt 移动单 sourceWarehouseId 为空时其反向冲销 bookkeeper destWarehouseId 为空；`canSafelyReverse` 守卫跳过不满足仓库前置的移动单（best-effort，try/catch 吞异常保持幂等）。**OUTGOING issue 移动单反向正常**（materials 回退仓库）。
- 故 receipt（MANUFACTURE）移动反向为 best-effort，E2E 不对其做硬断言；issue（OUTGOING）移动反向可断言。

### 剩余差距

委外红冲浏览器层 E2E 缺失：`reverseCompletion` GraphQL 浏览器层可达性 + 状态回退 + 三段 GL 红字凭证行精确数值断言（同向取负）+ issue 移动反向 + 非法状态守卫。本计划补齐此缺口，收口 manufacturing 委外段的业财闭环浏览器层验证。

## Goals

- `reverseCompletion` 经 GraphQL `ErpMfgSubcontractOrder__reverseCompletion` 浏览器层全栈可达：COMPLETED 委外单红冲后 posted=false + docStatus=CANCELLED 可观测（经 `verifyState` `__get` 独立断言）。
- 三段 GL 红字凭证行精确数值断言：SUBCONTRACT_ISSUE/RECEIPT/FEE 各生成 REVERSAL 凭证，凭证行同向取负（dcDirection 不变，金额取负，对齐 0704-1/2004-2 既有 reverse voucher line 断言范式）；原 NORMAL 凭证 isReversed=true。
- issue OUTGOING 移动反向验证（materials 回退仓库，`IErpInvStockMoveBiz.reverse` 生成反向冲销移动单可观测）。
- 非法状态守卫：RECEIVED（未 COMPLETED）调用 reverseCompletion 抛 `ERR_SUBCONTRACT_CANNOT_REVERSE` + docStatus 不变。

## Non-Goals

- **receipt（MANUFACTURE）移动反向硬断言**——1825-1 Closure 记录 inventory 域 `inverseMoveType` 限制致 receipt 移动反向为 best-effort（canSafelyReverse 守卫可能跳过）。E2E 不对其做硬断言（避免 flaky），归 watch-only residual。
- **财务侧 MfgSubcontractReversalListener 浏览器层路径**——财务侧直接红冲凭证经 `VoucherReversedEvent` 派发回退，为域级 reverseCompletion 的 fallback（用户一般经域级动作红冲，非直接红冲凭证）。后端 `TestErpMfgSubcontractReverse` 已覆盖 ReversalListener 路径。浏览器层验证域级 reverseCompletion（主路径）即代表业财闭环。
- **多行委外单红冲**——单行红冲为代表验证（runSubcontractChain 默认 lineCount=1）；多行红冲的 GL 汇总/分列范式经 0035-2 多行正向已验证，红冲同构。
- **WorkOrder（非委外）红冲**——1825-1 Deferred，触发条件「WorkOrder 完工红冲业务需求落地时」未满足。
- **委外退货（RETURNED 状态）**——1825-1 Deferred，触发条件「委外退货业务需求落地时」未满足。退货（不合格品退回供应商）与红冲（纠错全量撤销）为不同业务流。
- **委外 Portal / 来料质检 / 损耗 / 批次序列号 / 独立单据实体**——0455-1 既有 successor，触发条件未变。
- **零生产代码/契约/ORM 模型变更**——纯消费侧测试新增。

## Task Route

- Type: `verification or audit work`（扩展现有 Playwright E2E 套件覆盖至委外红冲 reverseCompletion 浏览器层，纯消费侧测试新增）
- Owner Docs: `docs/design/manufacturing/subcontracting.md`（§实现偏离补注增红冲段，1825-1 已更新）、`docs/testing/e2e-runbook.md`（业务动作套件 + 委外段）
- Skill Selection Basis: 浏览器层 Playwright E2E 测试扩展（GraphQL mutation 驱动 @BizMutation + 既有 orchestration/_helper.ts 原语复用）；`nop-testing` 技能面向 JunitAutoTestCase 后端快照测试非 Playwright 浏览器层（1218-2/1218-1/0742-1/0742-2 既有 E2E 计划均 `Skill: none`）→ `Skill: none`
- Protected Areas: 不触及 ORM 模型 / 不触及 finance 保护区域（经跨域 Facade 调用，不直接操作 finance 实体）；测试文件在根 `tests/e2e/` 非 reactor 模块

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（webServer fresh-DB + 种子 + auth fixtures）。
- `playwright.config.ts` webServer JVM args 已含 `-Derp-mfg.subcontract-posting-enabled=true`（0701-2 范式，红冲与正向过账同门控）。无新增 config/端口/环境变量/密钥。
- 种子 COA 已完备（1408/1401/1405/2202/1416/1417 由 0455-1/0035-1 落地）。

## Execution Plan

### Phase 1 - reverseCompletion 浏览器层 E2E（状态 + GL 红字凭证行 + issue 移动反向 + 守卫）

Status: completed
Targets: `tests/e2e/orchestration/mfg-subcontract-chain.spec.ts`（新增 reverse test blocks）；`tests/e2e/orchestration/_helper.ts`（`cleanupSubcontract` 扩展红字凭证清理，if Explore 裁定需要）
Skill: `none`

- Item Types: `Decision | Add | Proof`
- Prereqs: 1825-1 后端 reverseCompletion completed + 0035-2 `runSubcontractChain` 正向链至 COMPLETED 范式

- [x] `Decision | Explore`: 裁定 `cleanupSubcontract` 是否需扩展以清理红字凭证——`cleanupVoucherByBillCode`（`_helper.ts:206`）按 ErpFinVoucherBillR.billCode 删除凭证且**无 postingType 过滤**，故 NORMAL 与 REVERSAL 凭证（共享同一 billCode 如 `{code}-SF`）应已被既有 cleanup 覆盖（6 张凭证 = 3 NORMAL + 3 REVERSAL）。执行期 grep 核实确认无需扩展（若发现仅按 NORMAL 过滤则扩展含 REVERSAL，对齐 2004-2 既有 reverse cleanup 范式）。裁定结果记入执行日志。
  - Skill: `none`
  - 裁定结果（执行期实时仓库核实）：`cleanupVoucherByBillCode` 确无 postingType 过滤，按 billCode 删 voucher_line/voucher/voucher_bill_r，NORMAL+REVERSAL 共享同一 billCode（如 `{code}-SF`）一并删除 → **凭证清理无需扩展**。但执行期另发现红冲生成的 **REVERSAL 库存移动单**为独立实体（`ErpInvStockMoveProcessor.reverse:133-134`：relatedBillType=REVERSAL + relatedBillCode=原移动单 code），既有 `cleanupStockMove` 仅按 move.id 清理原移动单，REVERSAL 移动单 ledgers/lines/move 须单独清理 → 新增 `cleanupReverseStockMoveIfExists`（无红冲时 no-op 安全）。
- [x] `Add`: **reverseCompletion 正路径 test**（`mfg-subcontract-chain.spec.ts` 新增 test block）
  - `runSubcontractChain(page)` 产 COMPLETED + posted=true 委外单（复用既有正向链，含 3 段正向 GL 凭证）
  - `callMutationOk(page, 'ErpMfgSubcontractOrder', 'reverseCompletion', { subcontractOrderId: r.order.id }, 'id docStatus posted')`
  - `verifyState` 经 `__get` 断言 posted=false + docStatus=CANCELLED
  - **三段 GL 红字凭证行精确数值断言**（`findVoucherIdByBillCode(page, code+'-SI', 'REVERSAL')` 等）：
    - SUBCONTRACT_ISSUE 红字：Dr 1408=−issueCost(−10) / Cr 1401=−issueCost(−10)（dcDirection 不变金额取负）
    - SUBCONTRACT_RECEIPT 红字：Dr 1405=−receiptCost(−50) / Cr 1408=−receiptCost(−50)
    - SUBCONTRACT_FEE 红字：Dr 1408=−processingFee(−50) / Cr 2202=−processingFee(−50)
  - 原三段 NORMAL 凭证 isReversed=true（经 `findFirst` ErpFinVoucher by id 断言 isReversed 字段）
  - **issue OUTGOING 移动反向**：实现偏离修正——库存域 reverse 生成独立 REVERSAL 移动单（relatedBillType=REVERSAL + relatedBillCode=原 issue 移动单 code，非原 relatedBillType），故按 `findFirst(ErpInvStockMove, relatedBillType=REVERSAL + relatedBillCode=r.issueMove.code)` 断言存在（计划原述 relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE ≥2 经实时仓库核实不成立，已对齐实际反向链接语义）
  - Skill: `none`
- [x] `Add`: **非法状态守卫 test**（`mfg-subcontract-chain.spec.ts` 新增 test block）
  - `runSubcontractChain(page, { stopAfterIssue: true })` 产 ISSUED 委外单 → 手动 receiveFinished 至 RECEIVED（未 COMPLETED）
  - `callMutation(page, 'ErpMfgSubcontractOrder', 'reverseCompletion', { subcontractOrderId: r.order.id }, 'id')` 断言 errors 非空（`ERR_SUBCONTRACT_CANNOT_REVERSE`）
  - `verifyState` 断言 docStatus 保持 RECEIVED（不变）
  - Skill: `none`
- [x] `Proof`: `npx playwright test tests/e2e/orchestration/mfg-subcontract-chain.spec.ts --workers=1` 全绿（既有 7 + 新增 2 = 9 测试），既有套件无回归。
  - 验证命令：`npx playwright test tests/e2e/orchestration/mfg-subcontract-chain.spec.ts --workers=1`
  - 验证结果：9 passed（0 回归），见 `known-good-baselines` 2026-07-14 行。
  - Skill: `none`

Exit Criteria:

- [x] reverseCompletion 经 GraphQL 浏览器层可达：COMPLETED→CANCELLED + posted=false 经 `verifyState` 独立断言
- [x] 三段 GL 红字凭证行精确数值断言全绿（同向取负，dcDirection 不变金额取负）+ 原 NORMAL 凭证 isReversed=true
- [x] issue OUTGOING 移动反向可观测（反向冲销移动单存在）
- [x] RECEIVED（未 COMPLETED）调用 reverseCompletion 抛 `ERR_SUBCONTRACT_CANNOT_REVERSE` 守卫 + docStatus 不变

### Phase 2 - 文档对齐

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/logs/2026/07-14.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1 全绿

- [x] `Add`: `docs/testing/e2e-runbook.md` 委外段更新——增 reverseCompletion 浏览器层 E2E 行（COMPLETED→CANCELLED + 三段 GL 红字凭证行同向取负 + issue 移动反向 + ERR_SUBCONTRACT_CANNOT_REVERSE 守卫）+ 套件计数更新为实测值。
  - 验证：e2e-runbook 委外段已含 1934-1 扩展（⑤⑥）描述 + 套件计数 7→9。
  - Skill: `none`
- [x] `Add`: `known-good-baselines` + `backlog/README.md` + 每日日志更新（含 full-green 验证块）。
  - 验证：known-good-baselines 2026-07-14 行（subcontract 9 passed + mvn 154 BUILD SUCCESS + 17 CRUD smoke 预存环境 Known Failure 登记）；backlog/README +1934-1 ✅ done 行；logs/2026/07-14.md +1934-1 条目。
  - Skill: `none`
- [x] `Add`: `1825-1` Deferred「委外红冲浏览器层 E2E」+ `0035-2` Deferred「委外红冲 E2E」标 RELEASED（本计划 Closure 登记）。
  - 验证：1825-1 Deferred「委外红冲浏览器层 E2E」+ `0035-2` Deferred「委外红冲 E2E」各增 `**RELEASED by 2026-07-14-1934-1**` 行。
  - Skill: `none`

Exit Criteria:

- [x] e2e-runbook 委外段含 reverseCompletion 浏览器层 E2E 描述 + 套件计数实测值
- [x] 1825-1 / 0035-2 相关 Deferred RELEASED 登记于本计划 Closure

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_09f94c17effeo8oqYv0l1AsdIW`，独立 general 子代理，新会话冷重播无执行者上下文，2026-07-14) — 0 BLOCKER / 0 MAJOR / 4 MINOR。全部 load-bearing 事实主张经实时仓库逐行核实**零伪**：1825-1/0035-2 Plan Status=completed + Deferred 触发条件已满足 / reverseCompletion 后端 anti-hollow（Processor :233 + 4 protected step + ERR_SUBCONTRACT_CANNOT_REVERSE + MfgSubcontractReversalListener 均真实非占位）/ 7 测试 spec + SUBCONTRACT_EXPECT + findVoucherIdByBillCode postingType 参数 / forward voucher lines + reverse 同向取负范式（经 0704-1/0730-2/1218-1/0742-2 多源核实）/ 1825-1 Closure MANUFACTURE inverseMoveType 实现偏离与 Non-Goal 一致。4 MINOR（m1 validateCanReverse 行号 :331→:247 / m2 runSubcontractChain :1022→:1079 / m3 cleanupSubcontract :1159→:1237 / m4 Decision|Explore 预判 cleanupVoucherByBillCode:206 无 postingType 过滤已覆盖 NORMAL+REVERSAL）已全部修订落地。规则 4/7/9/10/14 + anti-slack + 技能记录全 PASS。草案可接受执行。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧测试新增（零生产代码/契约/ORM 模型变更）。结束前运行委外 E2E 子套件 + 全套件回归 + 后端构建（确认 E2E 未污染后端）。

- [x] 范围内行为完成（reverseCompletion 浏览器层 E2E：状态回退 + 三段 GL 红字凭证行 + issue 移动反向 + 守卫）
- [x] 相关文档对齐（e2e-runbook 委外段 + 套件计数 + known-good-baselines + 日志）
- [x] 已运行验证：`npx playwright test tests/e2e/orchestration/mfg-subcontract-chain.spec.ts --workers=1`（全绿 9 passed 0 回归）+ 全套件 `npx playwright test --workers=1`（343 passed，orchestration+business-actions 全绿 0 回归；17 CRUD smoke 失败为预存环境问题——fresh-DB+fresh-server 隔离仍失败，非本计划回归，本计划仅改 orchestration 测试不触及 crud/）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS 3:13，E2E 新增文件在根 tests/ 非 reactor 模块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### receipt（MANUFACTURE）移动反向硬断言

- Classification: `watch-only residual`
- Why Not Blocking Closure: 1825-1 Closure 记录 inventory 域 `inverseMoveType` 限制致 MANUFACTURE 类型移动单不反转，receipt 移动单反向冲销为 best-effort（canSafelyReverse 守卫可能跳过）。E2E 仅对 issue（OUTGOING）移动反向做硬断言；receipt 移动反向归 watch-only，避免 flaky 测试。GL 红字凭证（三段均生成，独立于库存移动反向）+ issue 移动反向 + 状态回退已充分覆盖业财闭环可观测性。
- Successor Required: `no`（inventory 域 inverseMoveType 限制为平台约束，非应用缺陷；若未来 inventory 域扩展 MANUFACTURE inverseMoveType 则可补硬断言）

### 财务侧 MfgSubcontractReversalListener 浏览器层路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 域级 reverseCompletion 为用户红冲主路径；财务侧直接红冲凭证经 VoucherReversedEvent 回退为 fallback。后端 `TestErpMfgSubcontractReverse` 已覆盖 ReversalListener 路径（d 用例）。浏览器层验证域级 reverseCompletion（主路径）即代表业财闭环。
- Successor Required: `yes`（触发条件：财务侧直接红冲凭证浏览器层 E2E 需求落地时）

### 多行委外单红冲

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 单行红冲为代表验证；多行红冲的 GL 汇总/分列范式经 0035-2 多行正向 E2E 已验证，红冲同构。
- Successor Required: `no`

## Closure

Status Note: 全 2 Phase 落地并验证通过。reverseCompletion 浏览器层 E2E 补齐 manufacturing 委外段业财闭环浏览器层验证缺口——状态回退（COMPLETED→CANCELLED+posted=false 经 `verifyState` 独立断言）+ 三段 GL 红字凭证行同向取负精确数值断言（SI/SR/SF dcDirection 不变金额取负）+ 原三段 NORMAL 凭证 isReversed=true + issue OUTGOING 移动反向（REVERSAL 移动单）+ ERR_SUBCONTRACT_CANNOT_REVERSE 守卫。`mfg-subcontract-chain.spec.ts` 9 passed（7 既有 + 2 新，0 回归）。执行期实现偏离修正：计划 item-104 原述按 relatedBillType=ERP_MFG_SUBCONTRACT_ISSUE 反向断言 ≥2 经实时仓库核实不成立（库存域 reverse 生成独立 REVERSAL 移动单 relatedBillType=REVERSAL+relatedBillCode=原移动单 code），已对齐实际反向链接语义；凭证清理无扩展（cleanupVoucherByBillCode 无 postingType 过滤），REVERSAL 移动单独立实体清理新增 cleanupReverseStockMoveIfExists。两 Deferred（1825-1/0035-2）均 RELEASED。结束审计已由独立子代理（新会话，冷重播无执行者上下文）执行并通过，见下 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver 独立 closure auditor，新会话冷重播，无执行者上下文）
- Evidence: 逐项核实全部 load-bearing 事实主张零伪：
  - **spec 测试计数**：`mfg-subcontract-chain.spec.ts` grep `^  test(` = **9**（7 既有 full-chain/guard/MRP/multi-line/partial-receive/cancel×2 + 2 新 reverseCompletion 正路径 + 守卫），与计划 7→9 一致。
  - **reverseCompletion 正路径**（spec :425-496）：pre-state 断言 COMPLETED+posted=true → callMutationOk reverseCompletion → verifyState `__get` 独立断言 CANCELLED+posted=false；三段 GL 红字凭证经 `findVoucherIdByBillCode(code+'-SI/-SR/-SF','REVERSAL')` 取 REVERSAL 凭证 id + `assertVoucherLines` 逐行断言 dcDirection 不变金额取负（Dr1408=−10/Cr1401=−10、Dr1405=−50/Cr1408=−50、Dr1408=−50/Cr2202=−50）；原三段 NORMAL 凭证 isReversed=true 经 findFirst ErpFinVoucher 断言；issue OUTGOING 移动反向按 findFirst(ErpInvStockMove, relatedBillType=REVERSAL + relatedBillCode=原 issue 移动单 code) 断言（实现偏离修正落地对齐库存域实际反向链接语义）。
  - **非法状态守卫**（spec :504-539）：stopAfterIssue→ISSUED 手动 receiveFinished→RECEIVED → callMutation 断言 errors 非空含「不允许红冲」语义 token + verifyState 断言 docStatus 保持 RECEIVED 不变。
  - **helper 原语**：`findVoucherIdByBillCode` postingType 参数（_helper.ts:100）/ `assertVoucherLines`（:159）/ `cleanupVoucherByBillCode` 无 postingType 过滤（:206）/ `cleanupReverseStockMoveIfExists`（:236）均存在且 cleanupSubcontract 调用之（:1273-1274）。
  - **文档对齐**：e2e-runbook.md:308 委外段含 1934-1 扩展 ⑤⑥ + 套件计数 9；known-good-baselines.md:13 2026-07-14 行 9 passed + 154 BUILD SUCCESS + 17 CRUD smoke 预存环境 Known Failure 登记；logs/2026/07-14.md:11-22 full-green 验证块；backlog/README +1934-1 ✅ done。
  - **Deferred RELEASED**：1825-1:160 + 0035-2:137 各增 `**RELEASED by 2026-07-14-1934-1**` 行，触发条件均已满足。
  - **Anti-Hollow**：两新测试均含实质性 mutation 调用 + verifyState/__get 独立断言 + 凭证行精确数值断言，无空函数体/return null/吞异常占位。
  - **五点一致性**：Plan Status=completed / Phase 1-2 Status=completed / Exit Criteria 全 [x] / Closure Gates 全 [x]（含本审计门控）/ 日志条目一致。
  - **Deferred 诚实**：3 项 Deferred But Adjudicated（receipt 移动反向 watch-only / 财务侧 ReversalListener 浏览器层路径 out-of-scope+successor / 多行红冲 out-of-scope）均正确分类并附 successor 触发条件，无范围内置缺陷隐藏。
  - **结论**：PASS——计划已真实落地，可关闭。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
