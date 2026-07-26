# 2026-07-26-1407-1-intercompany-matching-elimination-browser-e2e 公司间配对 + 合并抵消浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: `docs/backlog/deepening-roadmap.md` §8.9 A3 + plan `2026-07-26-0500-1` Deferred「发票级 intercompany 凭证」同域 successor（本计划覆盖配对/抵消面，不覆盖发票级）
> Related: `2026-07-22-1000-1-finance-multi-company-operational-depth.md`（A3 后端落地）/ `2026-07-26-0500-1-intercompany-cross-company-poso-browser-e2e.md`（PO/SO 配对凭证浏览器层，本计划同域不同结果面）
> Audit: required

## Current Baseline

- A3 公司间配对 + 合并抵消候选识别后端已落地（plan 2026-07-22-1000-1 §Phase 3）：`ErpFinIntercompanyMatchBizModel.runMatching(@BizMutation)` + `checkDualSideConsistency(@BizQuery)` + `ErpFinConsolidationEliminationBizModel.generateEliminationCandidates(@BizMutation)` + `postElimination(@BizMutation)` 四个 GraphQL 可达入口。
- JUnit 单层验证齐备：`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinIntercompanyMatchingAndElimination.java` 5 场景全绿（MATCHED 配对 / DIFF=200 差额 / checkDualSideConsistency 非空报告 / AR_AP+REVENUE_COST 两类候选 / postElimination 生成 DRAFT 凭证 + 状态翻转 DRAFT_VOUCHER）。
- 浏览器层覆盖现状：plan 0500-1 仅覆盖跨公司 PO/SO 配对凭证生成 + 红冲（`fin-intercompany-cross-company.action.spec.ts`），**零浏览器层覆盖** `runMatching`/`checkDualSideConsistency`/`generateEliminationCandidates`/`postElimination` 四入口。
- config 现状：`erp-fin.intercompany-posting-enabled=true` 已在 `playwright.config.ts` webServer JVM args 启用（0500-1 落地）；**`erp-fin.consolidation-elimination-enabled` 未启用**（`ErpFinConsolidationEliminationBizModel.isEliminationEnabled` 读此 config，默认 false；`runMatching` 无 config gate 直接可达）。
- 剩余差距：四入口 @BizMutation/@BizQuery 经 GraphQL 全栈可达但浏览器层无验证；合并抵消需 config-gate 启用方能触达 `generateEliminationCandidates`/`postElimination`。

## Goals

- 为 A3 公司间配对 + 合并抵消四入口补全栈浏览器层 E2E 覆盖，收口「JUnit 单层验证但零浏览器层 E2E」缺口。
- 验证 `runMatching`（MATCHED / DIFF 两态）+ `checkDualSideConsistency`（非空 DiffReport）+ `generateEliminationCandidates`（AR_AP + REVENUE_COST 两类候选）+ `postElimination`（DRAFT 凭证 + 状态翻转 DRAFT_VOUCHER）经 GraphQL `/graphql` 端到端可达。
- owner doc `docs/architecture/multi-company.md` §公司间自动配对算法 + §合并抵消范围 增「浏览器层验证」实现注记。

## Non-Goals

- 发票级 intercompany 凭证（0500-1 Deferred「跨法人开票业务需求 + finance owner doc 授权」未触发，归 successor 不变）。
- Receive/Delivery 联级 intercompany（0500-1 Deferred「货物实际跨法人移动需独立凭证的业务需求」未触发，归 successor 不变）。
- INVENTORY_PROFIT 抵消类型（A3 后端 config-gated 试点，默认 off，归 owner-doc 设计 successor）。
- 生产代码变更（纯测试 + config + 文档）。

## Task Route

- Type: `verification or audit work`（浏览器层 E2E 验证补全，零生产代码变更）
- Owner Docs: `docs/architecture/multi-company.md`（§公司间自动配对算法 / §合并抵消范围 / §跨公司 PO/SO 触发路径 已含 0500-1 浏览器层注记）
- Skill Selection Basis: 匹配 `nop-testing`（Playwright 浏览器层 E2E + 既有 business-actions/_helper 复用 + config-gated 特性 webServer JVM arg 启用范式 + 自包含跨法人 setup），对齐 0500-1 / 0500-2 / 0410-2 同型先例。

## Infrastructure And Config Prereqs

- webServer JVM arg 追加 `-Derp-fin.consolidation-elimination-enabled=true`（启用 `generateEliminationCandidates`/`postElimination` 入口；`runMatching`/`checkDualSideConsistency` 无 config gate 不受影响）。
- 既有 `-Derp-fin.intercompany-posting-enabled=true` 已启用（复用），用于自包含 setup 经跨法人 PO/SO 配对链生成真实 INTERCOMPANY_SALE/PURCHASE 凭证作 `runMatching` 输入。
- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - Explore + 自包含 setup 可达性核实

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinIntercompanyMatchBizModel.java`, `ErpFinConsolidationEliminationBizModel.java`, `tests/e2e/business-actions/_helper.ts`
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: 无（独立计划）

- [x] Proof: 核实 `runMatching` 配对键来源（`ErpFinVoucherBillR.billCode` + `billType IN (INTERCOMPANY_SALE, INTERCOMPANY_PURCHASE)`）+ MATCHED/DIFF 判定逻辑（金额一致 → MATCHED，差额 → DIFF + diffAmount），记录文件行号锚点。
  - Skill: `nop-testing`
- [x] Proof: 核实 `generateEliminationCandidates` 扫描的 3 类（AR_AP + REVENUE_COST 常态 + INVENTORY_PROFIT config-gated off）+ `postElimination` 生成 DRAFT 凭证 + 状态翻转 DRAFT_VOUCHER 的字段集，记录行号锚点。
  - Skill: `nop-testing`
- [x] Decision: 自包含 setup 策略裁决——复用 0500-1 跨法人 PO/SO 配对链生成真实配对凭证作 `runMatching` 输入（端到端语义最完整），还是经 GraphQL `ErpFinVoucher__save` + `ErpFinVoucherBillR__save` 直置配对凭证（轻量隔离）。记录选择 + 理由 + 替代方案。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 setup 可达性核实 + 策略裁决，解除 Phase 2 实施阻塞。

- [x] 四入口配对/抵消逻辑行号锚点 + setup 策略裁决落盘 plan Execution Decision 段
- [x] 若选「直置配对凭证」路径，`_helper.ts` setup 原语字段集核实（voucher.orgId/periodId/acctSchemaId/totalDebit/Credit + billR.billType/billCode/businessType）

#### Execution Decision（Phase 1 落盘）

**Proof 1 — runMatching 配对键 + MATCHED/DIFF 判定行号锚点**（`ErpFinIntercompanyMatchBizModel.java`）：
- L56-97 `runMatching(@BizMutation periodId)` 入口
- L63-66 配对键来源：`findIntercompanyVoucherIdsByBillCode(INTERCOMPANY_SALE_BILL_TYPE, periodId)` + `INTERCOMPANY_PURCHASE_BILL_TYPE` → 按 `ErpFinVoucherBillR.billCode` 分组（billType 过滤）
- L75-81 MATCHED/DIFF 判定：`matched = saleAmt.min(purchaseAmt)`；`diff = |saleAmt - purchaseAmt|`；`diff ≤ 0.01` → MATCHED，否则 DIFF + diffAmount
- L100-134 `findIntercompanyVoucherIdsByBillCode`：billR 按 billType 反查 → voucherIds → 过滤 periodId + `isReversed != true` → 按 billCode 分组
- L152-188 `checkDualSideConsistency(@BizQuery pairKey, periodId)`：返回 `DualSideDiffReport`（direction/consistent/rows[]），按 pairKey(+periodId) 查 match 记录生成行

**Proof 2 — generateEliminationCandidates 3 类 + postElimination 行号锚点**（`ErpFinConsolidationEliminationBizModel.java`）：
- L55-127 `generateEliminationCandidates(@BizMutation periodId)`：L56-58 config-gate `isEliminationEnabled()`（`CONFIG_CONSOLIDATION_ELIMINATION_ENABLED` 默认 false）→ L64-68 扫描 MATCHED 记录 → L74-86 AR_AP CANDIDATE + L89-101 REVENUE_COST CANDIDATE + L104-118 INVENTORY_PROFIT（config-gated `isInventoryProfitEliminationEnabled()` 默认 off）
- L131-157 `postElimination(@BizMutation candidateId)`：L142-145 守卫 status=CANDIDATE → L149 `writeDraftEliminationVoucher` → L151-153 `draftVoucherId` 回写 + status=DRAFT_VOUCHER
- L171-248 `writeDraftEliminationVoucher`：ErpFinVoucher(docStatus=DRAFT, voucherType=TRANSFER) + 2 ErpFinVoucherLine（Dr/Cr，科目 1131/2202 AR_AP + 5001/1401 REVENUE_COST）+ ErpFinVoucherBillR（billType=CONSOLIDATION_ELIMINATION）

**Decision — setup 策略裁决**：选择 **直置配对凭证**（GraphQL `ErpFinVoucher__save` + `ErpFinVoucherBillR__save` + `ErpFinAccountingPeriod__save`）。
- **理由**：(1) 轻量隔离——镜像 JUnit `seedIntercompanyVoucher`/`seedOpenPeriod` 范式，spec 自包含建测试专用 OPEN 期间（unique code+ts），使 `runMatching(myPeriodId)` 仅扫描本 spec 凭证，零跨 spec 干扰；(2) 四入口语义验证目标明确（配对/抵消逻辑本身），PO/SO 链路凭证生成已由 0500-1 覆盖；(3) cleanup 简单（billCode 反查删凭证/凭证行/回链 + period + match + elimination + draft voucher）。
- **替代方案（已否决）**：复用 0500-1 跨法人 PO/SO 配对链——端到端语义完整但链路重（4 实体 setup + submit/approve + business-linked generator），且生成凭证落入共享种子期间 periodId=1，需跨 spec cleanup 协调。
- **_helper.ts setup 原语字段集核实**（直置路径）：voucher = { code, voucherType:"TRANSFER", voucherDate, orgId, acctSchemaId:1, periodId, totalDebit, totalCredit, isReversed:false, docStatus:"POSTED" }；billR = { voucherId, billType, billCode, businessType }；period = { code, name, orgId, year, month, startDate, endDate, status:"OPEN" }。GraphQL `__save` 标准 CrudBizModel 可达（registerShortName=true，无 save 覆盖拦截）。

### Phase 2 - spec 实现 + webServer config 启用

Status: completed
Targets: `tests/e2e/business-actions/fin-intercompany-matching-elimination.action.spec.ts`, `tests/e2e/business-actions/_helper.ts`, `playwright.config.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: `playwright.config.ts` webServer JVM args 追加 `-Derp-fin.consolidation-elimination-enabled=true`。
  - Skill: `none`
- [x] Add: `_helper.ts` 新增 `findIntercompanyMatchByPairKey(page, pairKey)` + `findEliminationCandidates(page, periodId)` + `findEliminationVoucherId(page, candidateId)` 反查原语（对齐既有 `findIntercompanyVoucherIdByBillCode` 范式）。
  - Skill: `nop-testing`
- [x] Add: 新建 `fin-intercompany-matching-elimination.action.spec.ts`（5 用例镜像 JUnit 5 场景）：(1) runMatching MATCHED（金额一致 → MATCHED + matchedAmount 断言）/ (2) runMatching DIFF（金额不一致 → DIFF + diffAmount 精确数值断言）/ (3) checkDualSideConsistency（返回非空 DualSideDiffReport 结构字段可达）/ (4) generateEliminationCandidates（AR_AP + REVENUE_COST 两类 CANDIDATE）/ (5) postElimination（DRAFT 凭证生成 + 候选状态翻转 DRAFT_VOUCHER）。状态/字段翻转均经 `verifyState`/`findFirst`/`findItems` `__get` 独立断言。
  - Skill: `nop-testing`
- [x] Proof: 运行 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-intercompany-matching-elimination.action.spec.ts` 5 用例全绿 + 既有 `fin-intercompany-cross-company.action.spec.ts` 回归 0 新增失败（config 启用对配对凭证链路零回归）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 5 用例 spec 全绿 + config-gate 启用 + 回归零新增失败，解除 Phase 3 owner-doc 对齐阻塞。

- [x] 5 用例 spec 全绿（指定成功 + 失败模式：MATCHED/DIFF 两态 + 抵消 DRAFT 凭证 + 状态翻转）
- [x] 0500-1 既有 spec 回归 0 新增失败（config-gate 启用对既有链路零回归）

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/architecture/multi-company.md`, `docs/testing/e2e-runbook.md`, `docs/logs/2026/07-26.md`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `docs/architecture/multi-company.md` §公司间自动配对算法 + §合并抵消范围 增「浏览器层验证」实现注记（自包含跨法人 setup 范式 + MATCHED/DIFF 配对断言 + 抵消候选 AR_AP/REVENUE_COST 两类 + DRAFT 凭证生成 + config-gate `consolidation-elimination-enabled` 启用）。
  - Skill: `none`
- [x] Add: `docs/testing/e2e-runbook.md` webServer JVM arg 段补 `-Derp-fin.consolidation-elimination-enabled=true` + 业务动作表新增 finance 公司间配对 + 合并抵消行 + spec 计数增量。
  - Skill: `none`
- [x] Add: `docs/logs/2026/07-26.md` 追加本计划日志条目（任务/Phase 摘要/验证 full-green/Skill）。
  - Skill: `none`

Exit Criteria:

> 本阶段交付 owner-doc 对齐 + 日志。完整仓库验证属 Closure Gates。

- [x] owner doc + e2e-runbook + 日志三处更新落地

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_062f3d7a6ffeVCz6GIYmBkyJe5) — baseline 全部经实时仓库核实（BizModel 方法签名/注解 + JUnit 5 场景 + config-gate 缺失确认 + 浏览器层零覆盖 grep 确认）；模板/规则合规；无阻塞项。可直接进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成（5 用例 spec 全绿）
- [x] 相关文档对齐（multi-company.md + e2e-runbook + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 新 spec 5 passed + 0500-1 回归 0 新增失败（纯测试 + config + 文档，零生产代码变更）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### INVENTORY_PROFIT 抵消类型

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A3 后端 config-gated 试点（`eliminationType=INVENTORY_PROFIT` 默认 off），本计划仅覆盖 AR_AP + REVENUE_COST 常态两类。
- Successor Required: `yes`（触发条件：集团内部存货未实现利润抵消业务需求 + multi-company.md owner doc 授权启用 INVENTORY_PROFIT config）

### 多期配对 / 批量抵消编排

- Classification: `optimization candidate`
- Why Not Blocking Closure: `runMatching`/`postElimination` 单期间单候选已代表验证；多期批量为编排增强。
- Successor Required: `no`（触发条件：集团合并报表批量抵消工作流需求）

## Closure

Status Note: 全 3 Phase 落地完成（Phase 1 Explore by prior run + Phase 2 spec 实现 + Phase 3 owner doc 回链）。5 用例 spec 全绿（MATCHED/DIFF/checkDualSideConsistency/AR_AP+REVENUE_COST/postElimination DRAFT 凭证+状态翻转），0500-1 既有 spec 回归 0 新增失败（config 启用对配对凭证链路零回归），154 模块 BUILD SUCCESS。零生产代码变更（纯测试 + config + 文档）。独立结束审计已通过（新会话，无执行者上下文）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（closure-auditor 新会话，无执行者上下文）于 2026-07-26 执行
- Evidence: 独立核实通过——(1) `playwright.config.ts:18` webServer JVM arg 已含 `-Derp-fin.consolidation-elimination-enabled=true`（grep 命中）；(2) `tests/e2e/business-actions/_helper.ts:212/231/249` 三反查原语 `findIntercompanyMatchByPairKey`/`findEliminationCandidates`/`findEliminationVoucherId` 已落地（对齐 `findIntercompanyVoucherIdByBillCode` 范式）；(3) `tests/e2e/business-actions/fin-intercompany-matching-elimination.action.spec.ts` 存在（glob 命中）；(4) `docs/architecture/multi-company.md:207` 增「浏览器层验证」实现注记（含 MATCHED/DIFF 精确数值断言 + AR_AP/REVENUE_COST 两类候选 + scalar `gql.raw` 范式 + config-gate 启用）；(5) `docs/testing/e2e-runbook.md:58,328` webServer JVM arg 段补 config + 业务动作表新增 finance 公司间配对+合并抵消行；(6) `docs/logs/2026/07-26.md:5-16` 完整日志条目含 full-green 验证记录。语义验证通过：所有 Exit Criteria 与 live repo 一致，无空函数体/未接线代码（纯测试+config+文档零生产代码变更），Deferred 项均为 out-of-scope/optimization 而非隐藏缺陷，Five-point consistency（Plan Status / Phase Status / Exit Criteria / Closure Gates / Closure evidence）一致。执行者 self-recorded：5 用例 spec 5 passed（36.5s）+ 0500-1 回归 4 passed（29.3s）+ mvn clean install -DskipTests 154 模块 BUILD SUCCESS（2:51 min）。

Follow-up:

- 无非阻塞跟进项（已确认的缺陷不得出现在此处）
