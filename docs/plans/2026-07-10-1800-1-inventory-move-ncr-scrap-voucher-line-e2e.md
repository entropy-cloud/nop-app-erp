# 2026-07-10-1800-1-inventory-move-ncr-scrap-voucher-line-e2e 业财过账凭证行精确数值断言扩展（库存移动过账 + NCR SCRAP 过账）

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Source: deferred items from `2026-07-10-0704-1`（库存移动过账凭证行断言）+ `2026-07-10-0335-2`（NCR SCRAP 过账数值断言）+ `2026-07-10-0704-1`（NCR SCRAP 过账凭证行断言）
> Related: `2026-07-10-0704-1-voucher-line-numeric-assertion-e2e.md`（凭证行断言基线计划），`2026-07-09-1249-1-p2p-o2c-orchestration-e2e.md`（P2P/O2C 编排链基线），`2026-07-10-0335-2-ncr-capa-resolve-mnt-request-e2e.md`（NCR resolve 门控基线）
> Audit: required

## Current Baseline

- `orchestration/_helper.ts` 已建立两原语（0704-1 落地）：`findVoucherIdByBillCode(page, billCode, postingType?)`（经 `ErpFinVoucherBillR` 反查 voucherId）+ `assertVoucherLines(page, voucherId, expectedLines[])`（经 `ErpFinVoucherLine__findPage` 查行，按 subjectCode 匹配，逐行断言 dcDirection + debitAmount + creditAmount）。
- **已覆盖**（0704-1）：AP_INVOICE（p2p-chain）/ AR_INVOICE（o2c-chain）/ 红字 AP_INVOICE（p2p-reverse）/ 红字 AR_INVOICE（o2c-reverse）/ PURCHASE_RETURN（pur-return）/ SALES_RETURN（sal-return）共 6 条业财链路凭证行断言。
- **未覆盖**（Deferred from 0704-1）：
  - **库存移动过账**（PURCHASE_INPUT / SALES_OUTPUT）：P2P Receive approve → INCOMING 移动 DONE → `InvPostingDispatcher.dispatchPurchaseInput` → Dr 1401 / Cr 2202；O2C Delivery approve → OUTGOING 移动 DONE → `InvPostingDispatcher.dispatchSalesOutput` → Dr 6401 / Cr 1401。两条链路已运行（stockMove.posted=true 已在 1249-1 断言），但凭证行级科目码/金额未断言。
  - **NCR SCRAP 过账**（NCR_SCRAP）：NCR resolve（SCRAP 处置，默认 AUTO_POST 配置 `erp-qua.ncr-posting-mode=AUTO_POST`）→ `dispatchFinancialImpact` → `NcrPostingDispatcher` → `NcrScrapAcctDocProvider` → Dr 6711 营业外支出 / Cr 1401 库存商品。0335-2 仅断言 resolve 门控 + status=RESOLVED（dispositionType=CONCESSION 干净隔离），SCRAP 过账凭证行未断言。
- **后端代码确认**（live repo 核实）：
  - `InvAcctDocProvider.java:22-31`：PURCHASE_INPUT（借 1401 / 贷 2202）+ SALES_OUTPUT（借 6401 / 贷 1401）+ MANUFACTURING_RECEIPT/ISSUE 等；`InvPostingDispatcher` 在 stock move DONE 时分派。
  - `NcrScrapAcctDocProvider.java:37-38`：SUBJECT_LOSS="6711" + SUBJECT_INVENTORY="1401"；amount = `SCRAP_AMOUNT`（恒正 = NCR.quantity × `ErpInvStockBalance.avgCost`）。`NcrPostingDispatcher` 在 resolve 时（默认 AUTO_POST 配置）或 `postNcr` 时分派，`reverseNcr` 红冲。
  - `ErpQaNonConformanceBizModel.java:105`：`postNcr(@Name("ncrId") Long ncrId, ...)` @BizMutation（手动入口，AUTO_POST 模式下 resolve 已自动过账，postNcr 抛 ERR_NCR_ALREADY_POSTED 守卫）。`:124`：`reverseNcr`。
  - `ErpQaConfigs`：`ncr-posting-mode` 默认 `AUTO_POST`——resolve 时 SCRAP 处置自动触发 `dispatchFinancialImpact`→过账。
- **种子 COA 完备**（1249-1 修复）：1401/1403/1131/2221/6401/2202 已在 `erp_md_subject.csv` 种子中，`findByCode` 全局按码解析可达。6711 未在种子 COA 中——**预置科目 gap**：`NcrScrapAcctDocProvider` 使用 SUBJECT_LOSS="6711"，种子 `erp_md_subject.csv` 无 6711 行，`resolveSubjects` 会抛 `ERR_SUBJECT_NOT_FOUND` → 过账优雅降级 posted=false。**需补种子科目 6711**（镜像 1249-1 补 5 科目范式）。
- **测试数据确定性**（核实）：
  - P2P chain：Receive qty=10 price=5 → PURCHASE_INPUT Dr 1401 = 50 / Cr 2202 = 50（`InvAcctDocProvider` 读 move line totalCost = qty × unitCost；Receive 触发的 INCOMING 移动 unitCost 来源 Receive line price）。
  - O2C chain：setup 备货 20@120 → Delivery qty=10 → SALES_OUTPUT Dr 6401 = 1200 / Cr 1401 = 1200（MOVING_AVERAGE 加权后 unitCost=120，出库 10 × 120 = 1200）。
  - NCR SCRAP：amount = NCR.quantity × `ErpInvStockBalance.avgCost`（`NcrPostingDispatcher` 查 `ErpInvStockBalance` 按 materialId 取 avgCost）。MAT_1 在 WH-RAW 无种子余额（0704-2/O2C 核实），**需前置备货**使 avgCost 可达（镜像 O2C chain generateMove INCOMING 范式）。备货后 avgCost 确定性 → SCRAP amount 确定性。
- E2E 套件当前 166 测试（0704-2 校准基线）。

## Goals

- 扩展业财过账凭证行精确数值断言覆盖至库存移动过账（PURCHASE_INPUT / SALES_OUTPUT）+ NCR SCRAP 过账（NCR_SCRAP），使凭证行级科目码/金额错误可被浏览器层 E2E 捕获。
- 解除 0704-1 两项 Deferred + 0335-2 一项 Deferred。

## Non-Goals

- 预付款/预收款/费用报销等非库存移动过账路径的凭证行断言（不同 Provider 结构，归独立 successor）。
- NCR RETURN 处置路径过账（触发退货编排副作用，归不同结果面）。
- 报表下载产物字节级 diff / 像素级截图基线 diff（1728-1/2330-2 既定 Deferred，独立能力面）。
- O2C setup move 自身的 PURCHASE_INPUT 过账断言（备货步骤副作用，非业务链路过账——凭证清理经既有 `cleanupStockMove` 覆盖）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/e2e-runbook.md`（§凭证行精确数值断言段扩展），`docs/design/inventory/state-machine.md`（§过账 Provider），`docs/design/quality/state-machine.md`（§NCR 财务影响规则）
- Skill Selection Basis: `nop-testing`（测试编写技能，但本计划为 Playwright E2E spec 非平台 JunitAutoTestCase，技能中无 Playwright 专项路由 → `Skill: none`；测试编写遵循 0704-1 已验证范式）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（E2E 套件 + Playwright + Quarkus webServer 不变）。
- 种子 COA 补 6711 行（`erp_md_subject.csv` 加性行，非生产代码变更）。

## Execution Plan

### Phase 1 — 种子科目补齐 + P2P/O2C 库存移动过账凭证行断言

Status: completed
Targets: `tests/e2e/orchestration/p2p-chain.spec.ts`, `tests/e2e/orchestration/o2c-chain.spec.ts`, `tests/e2e/orchestration/_helper.ts`, `_vfs/_init-data/erp_md_subject.csv`
Skill: none

- Item Types: `Add | Fix | Proof`
- Prereqs: 0704-1 完成（assertVoucherLines/findVoucherIdByBillCode 原语已落地）

- [x] Add: 种子 `erp_md_subject.csv` 补 6711 科目行（科目编码 6711 / 名称 营业外支出-报废损失 / 类别 损益类），镜像 1249-1 补 5 科目范式。使 `NcrScrapAcctDocProvider` SUBJECT_LOSS="6711" `findByCode` 可达。
  - Skill: none
- [x] Add: `p2p-chain.spec.ts` 扩展——Receive approve 触发的 INCOMING 移动 posted=true 后，经 `findVoucherIdByBillCode(page, r.receiveMove.code)` 反查 PURCHASE_INPUT 凭证 voucherId，`assertVoucherLines` 断言 Dr 1401 = 50 / Cr 2202 = 50（qty=10 × unitCost=5）。
  - Skill: none
- [x] Add: `o2c-chain.spec.ts` 扩展——Delivery approve 触发的 OUTGOING 移动 posted=true 后，经 `findVoucherIdByBillCode(page, r.deliveryMove.code)` 反查 SALES_OUTPUT 凭证 voucherId，`assertVoucherLines` 断言 Dr 6401 = 1200 / Cr 1401 = 1200（qty=10 × 加权 unitCost=120）。
  - Skill: none
- [x] Proof: `_helper.ts` 清理覆盖核实——`cleanupStockMove`（`_helper.ts:191-207`）内部已调用 `cleanupVoucherByBillCode(page, move.code)`（line 198），`cleanupP2p`（`:329`）经 `cleanupStockMove(page, r.receiveMove, ...)` 覆盖 Receive 移动凭证；`cleanupO2c`（`:527,536`）经 `cleanupStockMove(page, r.deliveryMove/r.setupMove, ...)` 覆盖 Delivery + setup 移动凭证。既有清理已覆盖库存移动过账凭证，无需新增清理代码。
  - Skill: none

Exit Criteria:

> 库存移动过账凭证行断言落地，P2P Receive 移动 PURCHASE_INPUT + O2C Delivery 移动 SALES_OUTPUT 凭证行可断言。后续 NCR SCRAP 过账断言需种子科目 6711 可达（解除阻塞）。

- [x] `p2p-chain.spec.ts` + `o2c-chain.spec.ts` 新增 assertVoucherLines 断言全绿（P2P Receive move voucher Dr 1401=50/Cr 2202=50；O2C Delivery move voucher Dr 6401=1200/Cr 1401=1200）
- [x] 种子 `erp_md_subject.csv` 6711 行存在且 `findByCode` 可达（本地 `npx playwright test tests/e2e/orchestration/ --workers=1` 验证，0 回归）

### Phase 2 — NCR SCRAP 过账凭证行断言

Status: completed
Targets: `tests/e2e/business-actions/quality-ncr-scrap-posting.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1（种子科目 6711 补齐）

- [x] Add: 新建 `quality-ncr-scrap-posting.action.spec.ts`——NCR SCRAP 过账完整路径 E2E（依赖默认 AUTO_POST 配置，resolve 自动触发过账）：(1) 前置备货 MAT_1（generateMove INCOMING 确定 avgCost，镜像 O2C setup 范式，记录 setup move code）；(2) 建 NCR（dispositionType=SCRAP, quantity=N, materialId=MAT_1）；(3) submitReview → IN_REVIEW；(4) resolve → RESOLVED（无 CAPA 空集放行，镜像 0335-2 注释裁决）——resolve 内部 `dispatchFinancialImpact` 在默认 AUTO_POST 配置下自动触发 `NcrPostingDispatcher` 过账，posted=true；(5) `findVoucherIdByBillCode(page, ncr.code)` 反查 NCR_SCRAP 凭证；(6) `assertVoucherLines` 断言 Dr 6711 = SCRAP_AMOUNT / Cr 1401 = SCRAP_AMOUNT（SCRAP_AMOUNT = NCR.quantity × 备货 avgCost）。
  - Skill: none
  - 实现裁决（avgCost 来源）：MAT_1(materialId=1) 种子余额行（warehouseId=1, avgCost=120）为 materialId=1 唯一行。NcrPostingDispatcher.resolveStockBalance 按 materialId(limit 1) 确定性返回此行 avgCost=120。SCRAP posting 不扣减物理库存（NcrPostingDispatcher 注释），故无需前置备货——备货会为 materialId=1 新增第二行致 limit 1 不确定。SCRAP_AMOUNT = qty(1) × 120 = 120。
  - 配置修复：NcrPostingDispatcher.resolveAcctSchemaId 读 config erp-qua.ncr-default-acct-schema（默认空→null），致凭证行 acctSchemaId 非空约束失败。webServer JVM 属性增 -Derp-qua.ncr-default-acct-schema=1（种子 ACCT-FIN-01），解除阻塞。
- [x] Proof: 清理 NCR SCRAP 产物——`reverseNcr` 红冲后 `cleanupVoucherByBillCode(page, ncr.code)` 清 NCR_SCRAP 凭证 + `cleanupVoucherByBillCode(page, setupMove.code)` 清备货移动 PURCHASE_INPUT 凭证 + 备货移动清理（整行删余额安全，镜像 O2C setup 清理）+ NCR 逻辑删除。保护共享 DB 数值断言基线（inventory dashboard）。
  - Skill: none
  - 实现裁决：因未前置备货（avgCost 来源裁决），无 setup move 凭证/余额清理。清理简化为：reverseNcr 红冲（验证红冲路径可达）→ cleanupVoucherByBillCode(page, ncr.code) 删 NORMAL+REVERSAL 凭证 → deleteById 删 NCR。SCRAP posting 仅写 voucher/voucher_line/voucher_bill_r（不写 gl_balance），不污染 finance dashboard 基线。

Exit Criteria:

> NCR SCRAP 过账凭证行断言落地。Phase 1 + Phase 2 共 3 条新过账路径凭证行断言。

- [x] `quality-ncr-scrap-posting.action.spec.ts` 新 spec 全绿（resolve AUTO_POST → voucher Dr 6711/Cr 1401 精确数值断言 + reverseNcr 红冲清理）
- [x] `npx playwright test tests/e2e/orchestration/ tests/e2e/business-actions/quality-ncr-scrap-posting.action.spec.ts --workers=1` 0 回归

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0b41af81fffeHCjhT8JLIgRe56) because two blockers: (B1) Phase 2 flow called `postNcr` after `resolve` but default AUTO_POST config means resolve already auto-posts → `postNcr` throws ERR_NCR_ALREADY_POSTED; (B2) false claim that setup move voucher cleanup "already covered" — `cleanupO2c()` has no such call, and `r.codes.setup` is a remark not a move code.
- Independent draft review iteration 2: needs revision (ses_0b40f6f20ffetjbPdx2XOQiLXZ) because B1+B2 fixes introduced B3: Phase 1 cleanup item proposed redundant explicit `cleanupVoucherByBillCode` calls, but `cleanupStockMove` (`_helper.ts:198`) already calls `cleanupVoucherByBillCode(page, move.code)` internally — `cleanupP2p`/`cleanupO2c` delegate to it for all three moves (Receive/Delivery/Setup), so voucher cleanup is already covered in the baseline.
- Independent draft review iteration 3: accept (ses_0b40ae535ffeQs9Jfwvv4k4nsI) — B3 fix substantively correct: Phase 1 cleanup item is coverage verification (no new code), Deferred cites `cleanupStockMove` accurately. Minor cosmetic label nit (`Add`→`Proof`) fixed. Plan is an acceptable execution contract.

## Closure Gates

> 完整仓库验证在此处运行。本计划为纯测试+种子数据+webServer 配置+预存 view.xml 修复（非本计划引入的回归），无生产代码/契约/ORM 模型变更。验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `npx playwright test --workers=1`（167 测试全绿，0 回归）。

- [x] 范围内行为完成（3 条新过账路径凭证行断言全绿）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` §凭证行精确数值断言段扩展）
- [x] 已运行验证（`mvn clean install -DskipTests` + `npx playwright test --workers=1`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### NCR reverseNcr 红字凭证行断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划验证 postNcr 正向凭证行。reverseNcr 红字凭证经 `buildReversalDraft` 同向取负（0704-1 已建立红字断言范式），结构可预测。归 successor。
- Successor Required: `yes`（触发条件：当需验证 NCR SCRAP 红冲凭证行级正确性时）

### O2C setup move PURCHASE_INPUT 过账断言

- Classification: `optimization candidate`
- Why Not Blocking Closure: setup move 为备货步骤副作用（非业务链路），其 PURCHASE_INPUT 过账凭证由既有 `cleanupStockMove`（`_helper.ts:198` 内部 `cleanupVoucherByBillCode`）经 `cleanupO2c`→`cleanupStockMove(page, r.setupMove, ...)` 覆盖清理。断言其凭证行无增量价值。
- Successor Required: `no`

## Closure

Status Note: completed (pending independent closure audit)

Closure Audit Evidence:

- Auditor / Agent: pending independent closure audit
- Evidence:
  - `mvn clean install -DskipTests` → BUILD SUCCESS（154 模块）
  - `npx playwright test --workers=1` → 167 passed（166 基线 + 1 新 NCR SCRAP spec），0 回归
  - Phase 1: p2p-chain.spec.ts PURCHASE_INPUT voucher Dr 1401=50/Cr 2202=50 ✓；o2c-chain.spec.ts SALES_OUTPUT voucher Dr 6401=1200/Cr 1401=1200 ✓
  - Phase 2: quality-ncr-scrap-posting.action.spec.ts NCR_SCRAP voucher Dr 6711=120/Cr 1401=120 ✓
  - 执行中修复的预存启动阻断（非本计划引入）：ErpFinBudgetScenario.view.xml confirmText 属性违规 / ErpHrLeaveRequest+ErpHrRecruitment.view.xml visibleOn 属性违规 / ErpInvLandedCost.view.xml reloadAfterSuccess 未定义子节点 / pricing-rule+sales-price-list 页面 view.xml 引用路径断裂
  - 执行中发现并解除的配置 gap：NcrPostingDispatcher.resolveAcctSchemaId 读 config erp-qua.ncr-default-acct-schema（默认空→null）致凭证行 acctSchemaId 非空约束失败，webServer JVM 属性增 -Derp-qua.ncr-default-acct-schema=1


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS**. All Phase 1+2 exit criteria verified: 3 voucher line assertions (PURCHASE_INPUT/SALES_OUTPUT/NCR_SCRAP) with exact subject codes and amounts, seed 6711 row exists, webServer config fixed, e2e-runbook.md aligned. No defects. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- NCR reverseNcr 红字凭证行断言（触发条件见 Deferred）
