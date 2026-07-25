# 2026-07-26-0410-2 承付会计（Commitment Accounting）浏览器层 E2E

> Plan Status: active
> Last Reviewed: 2026-07-26
> Source: 近期深化后端特性浏览器层验证缺口 —— 承付会计（A2，plan `2026-07-21-1206-2` §承付会计）落地采购订单 commit/release 三接入点 + `2026-07-24-1351-3` 扩展销售订单 commit hook，经 JUnit 覆盖（`TestErpFinBudgetCommitment` 4 场景 + `TestErpPurOrderCommitment` 3 场景 + fin-service 229 测试），但**零浏览器层 E2E**。AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点。
> Related: `docs/plans/2026-07-21-1206-2-finance-budget-multi-year-carryforward.md`（A2 承付落地）、`docs/plans/2026-07-24-1351-3-commitment-accounting-expansion.md`（销售订单 commit hook 扩展）
> Audit: required

## Current Baseline

承付（COMMITMENT）影子凭证范式已落地（A2）：订单审核 → `CommitmentVoucherGenerator.generateCommitment` 生成 `postingType=COMMITMENT` 凭证（`CommitmentVoucherGenerator.java:61`），释放 → `reverseCommitment` 红冲（`:77`，按 billType+billCode 反查原凭证逐张红冲 + `isReversed=true` + `reversalOfVoucherId` 回链）。

**三接入点**（config-gated `erp-fin.budget-commitment-enabled` 默认 false）：
- **#1 订单审核 commit**：采购 `ErpPurOrderProcessor.approve:90` → `commit(PURCHASE_ORDER, ...)`；销售 `ErpSalOrderProcessor.approve:97` → `runCommitmentCommitHook` → `commit(SALES_ORDER, ...)`（金额=order.totalAmountWithTax）。
- **#2 订单反审核/作废 release**：采购 `ErpPurOrderProcessor.reverseApprove:113`/`cancel` → `runCommitmentReleaseHook:239-245` → `budgetCommitmentBiz.release(...)` **无 try-catch**（`release()` 在无未红冲承付时抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`，`ErpFinBudgetCommitmentBizModel:88-91`）；销售 `ErpSalOrderProcessor.reverseApprove:120`/`cancel:132` → `runCommitmentReleaseHook:359-370` → `release(...)` **有 try-catch 容错**（捕获后 LOG.debug 跳过）。
- **#3 发票审核 release**（A2 原始，**本计划草案审查 iteration 1 发现遗漏**）：采购 `ErpPurInvoiceProcessor.approve:94`（impl `:306-325`）→ `releaseIfPresent(PURCHASE_ORDER, invoice.orderCode)`；销售 `ErpSalInvoiceProcessor.approve:96`（impl `:347-365`）→ `releaseIfPresent(SALES_ORDER, invoice.orderCode)`。发票审核时按订单 code 反查并红冲原承付凭证（语义：发票入账后承付转实际 AP/AR，释放占用）。

科目配置：采购 `erp-fin.budget-commitment-subject-code`；销售 `erp-fin.budget-commitment-sales-subject-code`（独立配置）。

凭证结构（`writeCommitmentVoucher:119-182`）：voucher `postingType=COMMITMENT` + `docStatus=POSTED` + `code="COMMITMENT-"+uuid12`；**单行**（subjectCode 来自 config-resolved 科目，dcDirection 跟随科目方向，debit/credit=absAmount，`businessType=billType`，memo="订单承付占用"）；billR `billType=SALES_ORDER_COMMITMENT|PURCHASE_ORDER_COMMITMENT` + `billCode=订单 code`。红冲（`writeReversalFromLines:184,232-238`）：**dcDirection 不变**，`debitAmount`↔`creditAmount` **互换**，`amountSource/amountFunctional = origDebit+origCredit`（正数），`reversalOfVoucherId` 回链。

**既有链路与接入点 #3 的交互**（草案审查 iteration 1 关键发现）：`runP2pChain`/`runO2cChain`（`orchestration/_helper.ts:262,439`）的编排末端**包含发票审核**。因此 config 启用后，链路返回时承付已被接入点 #3 释放（原凭证 `isReversed=true` + 红冲凭证已存在）。此后若 spec 再对**采购订单**做 `reverseApprove`，接入点 #2 的 `release()` 因无未红冲承付抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 且**无 try-catch** → mutation 失败。这影响既有 `p2p-reverse-approve.spec.ts` 等消费 `runP2pChain` 后反转采购订单的 spec，是 global config 启用的前置阻塞。

**采购 release hook 容错缺口（latent defect，rule 13 不可降级）**：`ErpPurOrderProcessor.runCommitmentReleaseHook:239-245` 调 `release(...)` 无 try-catch，与销售 hook `:359-370`（有 try-catch）不对称。当承付已被接入点 #3 释放后，采购订单反审核/作废会抛错阻断业务流。本计划须先修复此缺口（镜像销售 hook 容错或改调 `releaseIfPresent`），方可安全启用 global config。

既有浏览器层 BUDGET 影子凭证断言范式（`tests/e2e/orchestration/_helper.ts:findBudgetVoucherIdByCode:120`）：按 `ErpFinVoucherBillR.billCode` 反查 + `postingType=BUDGET` 过滤 + `reversalOfVoucherId` 区分正向/红冲。承付为同型（postingType=COMMITMENT），helper 可镜像扩展。

剩余差距：承付凭证生成 + 三接入点释放闭环经 JUnit 单层验证，但**全栈浏览器层路径未验证**；采购 release hook 容错缺口未修复（阻塞 global config 启用）。

## Goals

- 修复采购 release hook 容错缺口（latent defect，镜像销售 hook try-catch，解除 global config 启用阻塞）
- 验证订单审核（接入点 #1）→ COMMITMENT 凭证生成（销售 + 采购两路径，order-only setup 隔离接入点 #3 干扰）
- 验证订单反审核（接入点 #2）→ COMMITMENT 凭证红冲回链（order-only setup，在发票创建前反转）
- 验证发票审核（接入点 #3）→ COMMITMENT 凭证红冲释放（full-chain，承付转实际 AP/AR 语义）

## Non-Goals

- 预算控制 check hook（HARD/WARN）—— 已由 `fin-expense-claim-budget.action.spec.ts` 覆盖（plan 1218-2），属不同结果面
- 资金承诺（付款单/收款单 commitment）—— 1351-3 Deferred（`ErpFinPayment/Receipt` 领域对象不存在）
- commitment 跨年度结转 —— A2 Deferred（触发：跨年度 commitment 余额处理）
- 预算多年度滚动/结转 —— A2 已落地，非本计划
- ORM/契约/codegen/字典变更 —— 本计划含 1 处应用层 Java Fix（采购 release hook 容错）+ 测试 + config + 文档

## Task Route

- Type: `verification or audit work`（含 1 处 rule-13 不可降级 latent defect Fix）
- Owner Docs: `docs/design/finance/budget.md`（§承付会计 + §承付占用/释放 SPI + §接入点表）、`docs/design/finance/posting.md`（§承付 COMMITMENT 实际过账段）
- Skill Selection Basis: `nop-backend-dev`（采购 release hook 容错 Fix —— Processor 后置 hook + ErrorCode 守卫 + 跨实体 release 调用）+ `nop-testing`（Playwright 浏览器层 E2E + orchestration/_helper 镜像扩展 + config-gated 特性 webServer JVM arg 启用范式）

## Infrastructure And Config Prereqs

- webServer JVM args（`playwright.config.ts` webServer.command）追加（**须在 Phase 1 Fix 落地后**，否则采购 release hook 无容错致既有 spec 回归失败）：
  - `-Derp-fin.budget-commitment-enabled=true`
  - `-Derp-fin.budget-commitment-sales-subject-code=<code>`（Phase 1 Explore 选定种子已有 CREDIT 方向科目）
  - `-Derp-fin.budget-commitment-subject-code=<code>`（Phase 1 Explore 选定）
- No infra prereqs beyond existing baseline（fresh-DB H2 + 既有 webServer 启动链）。

## Execution Plan

### Phase 1 - 采购 release hook 容错 Fix + Explore（三接入点 + 科目选定 + 红冲行结构 + 全链路回归影响）

Status: planned
Targets: `module-purchase/erp-pur-service/.../processor/ErpPurOrderProcessor.java:239-245`（release hook 容错 Fix）+ `module-sales/erp-sal-service/.../processor/ErpSalOrderProcessor.java:359-370`（容错范式参照）+ `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv` + `module-finance/erp-fin-service/.../budget/CommitmentVoucherGenerator.java:184-238`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: 无

- [ ] Fix: `ErpPurOrderProcessor.runCommitmentReleaseHook`（`:239-245`）补 try-catch 容错 —— 镜像销售 hook `:359-370` 范式（catch `NopException` 含 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 时 LOG.debug 跳过，不阻断 reverseApprove/cancel 业务流）。替代方案：改调 `releaseIfPresent`（`:102-113` 已含容忍语义），Phase 1 Explore 裁决两案择一
  - Skill: `nop-backend-dev`
- [ ] Proof: 核实种子 `erp_md_subject.csv` 含 CREDIT 方向负债/权益科目码，选定销售 + 采购两 config 值；若种子缺合适科目，追加种子行（加性）
  - Skill: `nop-testing`
- [ ] Proof: 核实 `writeReversalFromLines:184,232-238` 红冲凭证行级结构（dcDirection 不变 + debit/credit 互换 + amountSource/Functional=origDebit+origCredit 正数），确定断言期望值表
  - Skill: `nop-testing`
- [ ] Proof: 核实 config 启用 + Fix 落地后既有 `runP2pChain`/`runO2cChain` 链路 NORMAL 凭证断言不受影响（COMMITMENT 独立 postingType），并扫描全套件消费 `runP2pChain` 后反转采购订单的 spec（如 `p2p-reverse-approve.spec.ts`）确认 Fix 消除 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED` 回归
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [ ] 采购 release hook 容错 Fix 落地，purchase-service 局部 `mvn test` 全绿（既有 116 测试 0 回归）
- [ ] Explore 笔记记录科目选定 + 红冲行结构 + 全链路回归影响（写入 plan Execution Decision 段）

### Phase 2 - spec 实现（三接入点全栈验证）

Status: planned
Targets: `tests/e2e/business-actions/fin-commitment-accounting.action.spec.ts`（NEW）+ `tests/e2e/orchestration/_helper.ts`（新增 `findCommitmentVoucherIdByCode`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: `_helper.ts` 新增 `findCommitmentVoucherIdByCode(page, billCode, reversal)` —— 镜像 `findBudgetVoucherIdByCode:120`，`postingType='COMMITMENT'` 替换 `'BUDGET'`，按 `reversalOfVoucherId` 区分正向/红冲
  - Skill: `nop-testing`
- [ ] Add: 新建 `fin-commitment-accounting.action.spec.ts`，**两组 setup 隔离接入点 #3 干扰**：(A) order-only setup（经 `__save` 建订单 + `submitForApproval`/`approve` mutation，**不创建发票**）用于接入点 #1+#2；(B) full-chain `runP2pChain`/`runO2cChain` 用于接入点 #3
  - Skill: `nop-testing`
- [ ] Proof: (1) **接入点 #1 订单审核 commit（order-only）** —— 销售 + 采购各 approve 订单 → `findCommitmentVoucherIdByCode(order.code, false)` 非空 + `assertVoucherLines` 单行（config 科目 + dcDirection + amount=order.totalAmountWithTax）；(2) **接入点 #2 订单反审核 release（order-only，发票前反转）** —— reverseApprove 订单 → `findCommitmentVoucherIdByCode(order.code, true)` 非空 + 原凭证 `isReversed=true`（`__get`）+ 红冲行 dcDirection 不变/debit-credit 互换（按 Phase 1 期望值表）；(3) **接入点 #3 发票审核 release（full-chain）** —— runP2pChain/runO2cChain 末端发票 approve → 承付原凭证 `isReversed=true` + 红冲凭证存在（经 orderCode 反查）；(4) **采购 release hook 容错回归** —— full-chain 后 reverseApprove 采购订单不再抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（接入点 #2 容错生效）
  - Skill: `nop-testing`
- [ ] Add: cleanup 扩展清理 COMMITMENT 凭证（同 billCode + postingType=COMMITMENT，若既有 `cleanupVoucherByBillCode` 未覆盖）
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [ ] `fin-commitment-accounting.action.spec.ts` 全绿，断言三接入点 + 容错回归四组可观察结果

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: planned
Targets: `docs/design/finance/budget.md`（§接入点表补 #3 浏览器层覆盖 + §release hook 容错对称性注记）+ `docs/design/finance/posting.md`（承付段补浏览器层覆盖注记）+ `docs/testing/e2e-runbook.md`（业务动作表 + 承付行 + webServer JVM arg 段）+ `docs/bugs/`（采购 release hook 容错缺口登记，若有 bug 文档先例）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] Add: `budget.md` §承付会计 增「浏览器层验证」实现注记（三接入点全栈路径 + order-only/full-chain 双 setup 隔离范式 + helper 镜像 BUDGET）+ release hook 容错对称性注记（采购 Fix 对齐销售）
- [ ] Add: `e2e-runbook.md` 业务动作表 +finance 承付行（三接入点）+ webServer JVM arg 段补 `budget-commitment-enabled` + 两 subject-code + 已知限制（config 启用后既有链路额外生成 COMMITMENT 凭证）
- [ ] Add: `docs/bugs/` 登记采购 release hook 容错缺口 latent defect（对齐 AGENTS.md ops-rule 9，非显而易见回归：采购/销售 release hook 不对称致 config 启用后采购订单反审核阻断）—— 记录现象/根因/Fix/预防

Exit Criteria:

- [ ] owner doc + runbook 更新落地（仅此阶段实际更改 owner 行为文档）

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_0651bceddffe2dRuWC8CvaljYZ) because BLOCKER 测试设计损坏 —— 遗漏接入点 #3（发票审核 release），`runO2cChain`/`runP2pChain` 末端发票审核已释放承付，致销售路径假阳性 + 采购路径抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（release hook 无 try-catch）；Phase 1 Explore 过窄未映射三接入点；global config 启用回归风险低估；红冲行结构描述不准（dcDirection 不变非互换）。修正：基线补接入点 #3 + 采购 hook 容错缺口（latent defect rule 13）；Goals 重构为三接入点；Phase 1 增 Fix + 全链路回归扫描；Phase 2 重构为 order-only（#1+#2）+ full-chain（#3）双 setup 隔离；红冲行结构对齐代码
- Independent draft review iteration 2: accept (ses_065158d5dffePxljfCMRL0TM4P) — iteration-1 BLOCKER 全部 FIXED（5/5 经实时仓库核实：接入点 #3 文档化 / 采购 hook Fix in-scope / order-only+full-chain 双 setup 消除假阳性+抛错 / 红冲行结构精确匹配 / Closure Gates 全套件回归）。1 MINOR：Phase 3 Targets 列 `docs/bugs/` 但无交付项。修正：Phase 3 增 `docs/bugs/` 登记 item

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + 受影响 Playwright 套件一次。

- [ ] 范围内行为完成（三接入点全栈验证 + 采购 release hook 容错 Fix 四组断言全绿）
- [ ] 相关文档对齐（budget.md + posting.md + e2e-runbook）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/fin-commitment-accounting.action.spec.ts` 全绿 + **全套件 e2e 回归 0 新增失败**——config 启用影响全域，须全套件验证非仅 chain spec）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 资金承诺（付款单/收款单 commitment）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1351-3 Deferred —— `ErpFinPayment`/`ErpFinReceipt` 领域对象在本仓不存在（grep 全仓零命中）；资金承诺的前提是这些领域对象先构建
- Successor Required: `yes`（触发条件：付款单/收款单领域对象构建后 + budget owner doc 授权资金承诺语义）

### commitment 跨年度结转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2 Deferred —— commitment 一并结转语义复杂
- Successor Required: `yes`（触发条件：跨年度 commitment 余额处理需求）

### GL Mapping 接入 commitment 多维规则

- Classification: `optimization candidate`
- Why Not Blocking Closure: CommitmentAcctDocProvider dormant（createFacts 返回空列表），科目经 config 解析；多维规则接入归 GL Mapping rollout successor
- Successor Required: `yes`（触发条件：commitment 科目需多维差异化 + A1 resolver 稳定）

## Closure

Status Note: pending
