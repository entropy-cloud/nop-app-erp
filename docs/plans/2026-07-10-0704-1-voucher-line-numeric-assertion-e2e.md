# 2026-07-10-0704-1 业财过账凭证行精确数值断言浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Mission: erp
> Work Item: 各域细化端到端验证（业财过账凭证行 subjectCode + debitAmount/creditAmount 精确数值断言）
> Source: `2026-07-09-0814-2` Deferred「业财过账凭证精确数值断言」+ `2026-07-09-1249-1` Deferred「业财过账凭证数值断言」+ `2026-07-10-0335-1` Deferred「业财过账凭证精确数值断言」+ `2026-07-09-2004-2` Deferred「红字凭证借贷精确数值镜像断言」
> Related: `2026-07-09-1249-1`（P2P/O2C 编排链源）、`2026-07-09-2004-2`（反向冲销源）、`2026-07-10-0335-1`（Return approve posted 源）
> Audit: required

## Current Baseline

- **P2P/O2C 编排链 E2E 已全绿**（`1249-1`）：`runP2pChain` / `runO2cChain` 经 GraphQL `__save` + `submitForApproval` + `approve` 驱动全链，每步 `verifyState` 断言 approveStatus 翻转。Invoice approve 后断言 `posted=true` + `ErpFinVoucherBillR`(billCode=invoice.code) 存在 + `ErpFinArApItem` openAmount=含税总额/OPEN。**但未查询凭证行 `ErpFinVoucherLine` 断言每行 subjectCode + debitAmount/creditAmount 精确数值**——仅断言凭证存在性（voucher_bill_r total≥1）和辅助账 openAmount。
- **反向冲销 E2E 已全绿**（`2004-2`）：`runP2pReverse` / `runO2cReverse` 经 `ErpFinVoucher__reverse` 产红字凭证，断言原凭证 isReversed=true + 红字凭证 reversalOfVoucherId 回链 + 域单据 posted=false/approveStatus APPROVED→REJECTED。**但未断言红字凭证行与原正常凭证行的借贷镜像关系**——仅断言凭证级标志。
- **Return approve E2E 已全绿**（`0335-1`）：PurReturn/SalReturn approve 后 `posted=true`（布尔标志）。**但未断言 PURCHASE_RETURN/SALES_RETURN 过账凭证行科目码 + 金额**。
- **凭证行实体字段已核实**（`ErpFinVoucherLine` ORM `app-erp-finance.orm.xml:324-358`）：`subjectCode`(VARCHAR, 科目码如"1403")、`dcDirection`(VARCHAR, DEBIT/CREDIT)、`debitAmount`(DECIMAL)、`creditAmount`(DECIMAL)、`voucherId`(BIGINT, FK)、`registerShortName=true`（有独立 GraphQL 端点 `ErpFinVoucherLine__findPage`）。
- **过账 Provider 凭证行结构已核实**（Java 源码）：
  - **AP_INVOICE**（`PurAcctDocProvider.java:62-68`）：Dr 1403 在途物资=TOTAL_AMOUNT(不含税) + Dr 2221 进项税=TOTAL_TAX_AMOUNT / Cr 2202 应付账款=TOTAL_AMOUNT_WITH_TAX(价税合计)。P2P 种子值：50 / 6.5 / 56.5。
  - **AR_INVOICE**（`SalAcctDocProvider.java:64-70`）：Dr 1131 应收账款=TOTAL_AMOUNT_WITH_TAX / Cr 6001 主营业务收入=TOTAL_AMOUNT / Cr 2221 销项税=TOTAL_TAX_AMOUNT。O2C 种子值：113 / 100 / 13。
  - **PURCHASE_RETURN**（`PurAcctDocProvider.java:69-73`）：Dr 2202 应付账款-暂估=TOTAL_AMOUNT(不含税) / Cr 1401 库存商品=TOTAL_AMOUNT。金额=退货行 totalAmount 不含税。
  - **SALES_RETURN**（`SalAcctDocProvider.java:71-75`）：Dr 1401 库存商品=TOTAL_COST / Cr 6401 主营业务成本=TOTAL_COST。金额=退货成本。
  - **红字凭证**（`reverseProcess` / `ErpFinPostingProcessor.buildReversalDraft`）：红字凭证行与原正常凭证行 **subjectCode 不变、dcDirection 不变、金额取负**（debitAmount 或 creditAmount 为负值，方向列与原行相同）。例如原行 1403 DEBIT debitAmount=50 → 红字行 1403 DEBIT debitAmount=-50。`postingType=REVERSAL`。
- **helper 原语已就绪**：`findItems(page, 'ErpFinVoucherLine', eqFilter('voucherId', vid), 'subjectCode dcDirection debitAmount creditAmount')` 可查凭证行。`findItems(page, 'ErpFinVoucherBillR', eqFilter('billCode', code), 'voucherId')` 可反查 voucherId。
- **剩余差距**：P2P/O2C/Return/Reverse 四条已覆盖链路的过账凭证行精确数值断言缺失。现有断言停留在「凭证存在 + 辅助账 openAmount」级别，凭证行级科目码/借贷金额未被验证——若 Provider 的 subjectCode 映射错位（如科目码拼写错误）、金额计算错误（如不含税/含税混用）、红冲借贷镜像断裂（如反向凭证未翻转方向），现有断言无法捕获。

## Goals

- 在 P2P/O2C 编排链 spec（`p2p-chain.spec.ts` / `o2c-chain.spec.ts`）中新增凭证行精确数值断言：查询 Invoice approve 产生的 AP_INVOICE/AR_INVOICE 凭证行，逐行断言 `subjectCode` + `debitAmount`/`creditAmount` 匹配 Posting Provider 结构派生的期望值
- 在反向冲销 spec（`p2p-reverse.spec.ts` / `o2c-reverse.spec.ts`）中新增红字凭证行同向取负断言：查询红字凭证行，断言每行 subjectCode/dcDirection 与原正常凭证行一致、debitAmount/creditAmount 为原行负值（方向不变、金额取负）
- 在 Return business-action spec（`pur-return.action.spec.ts` / `sal-return.action.spec.ts`）中新增 PURCHASE_RETURN/SALES_RETURN 过账凭证行科目码 + 金额断言
- 在 `orchestration/_helper.ts` 中提取可复用凭证行断言原语 `assertVoucherLines`，供正向/反向/Return 三场景共用

## Non-Goals

- 库存移动过账凭证行断言（PURCHASE_INPUT/SALES_OUTPUT 经 `InvAcctDocProvider`）——Receive/Delivery approve 触发的库存凭证行结构不同（Dr/Cr 1401/2202 或 6401/1401），归独立 successor（触发条件：需验证库存移动过账凭证行级正确性时）
- NCR SCRAP 过账凭证行断言（`NcrScrapAcctDocProvider` 借 6711/贷 1401）——需 NCR resolve + postNcr 前置链路，属不同结果面（触发条件：需验证 NCR SCRAP 过账凭证行级正确性时，见 `0335-2` Deferred）
- 全域全业务类型过账凭证行覆盖——本计划覆盖 P2P/O2C/Return/Reverse 四条核心业财链路凭证行，其余业务类型同范式 successor
- GL 余额（`gl_balance`）写入断言——`persistVoucher` 仅写 voucher/voucher_line/voucher_bill_r，不写 gl_balance（`1249-1` 核实）；GL 余额级断言属期末结账 successor

## Task Route

- Type: `verification work`（浏览器层 E2E 断言深化，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/design/finance/posting.md`（过账机制 + Provider 科目映射）、`docs/testing/e2e-runbook.md`（E2E 运行手册跨域编排层段）
- Skill Selection Basis: `nop-testing`（E2E 套件 @BizMutation + GraphQL findPage 范式，`1249-1` 已验证 helper 复用）；无后端代码变更，不加载 `nop-backend-dev`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 复用既有 Playwright 基础设施 + 种子 COA（`1249-1` 补齐 1401/1403/1131/2221/6401 使过账 Provider 科目码可达）+ orchestration helper 链式驱动/清理原语。

## Execution Plan

### Phase 1 - P2P/O2C 编排链凭证行精确数值断言

Status: completed
Targets: `tests/e2e/orchestration/_helper.ts`（新增 `assertVoucherLines` 原语 + `findVoucherIdByBillCode` 查询）、`tests/e2e/orchestration/p2p-chain.spec.ts`、`tests/e2e/orchestration/o2c-chain.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: `1249-1` P2P/O2C 链路 spec + helper 已全绿

- [x] `Add`：`orchestration/_helper.ts` 新增可复用凭证行断言原语：
  - `findVoucherIdByBillCode(page, billCode, postingType?)`：经 `ErpFinVoucherBillR`(billCode) 反查 voucherId，可选按 `ErpFinVoucher.postingType`(NORMAL/REVERSAL) 过滤
  - `assertVoucherLines(page, voucherId, expectedLines[])`：经 `ErpFinVoucherLine__findPage`(voucherId) 查凭证行，逐行断言 `subjectCode` + `dcDirection` + 借贷金额。期望行表由调用方按 Provider 结构传入。NORMAL 凭证行金额为正（debitAmount>0 或 creditAmount>0）；REVERSAL 红字凭证行金额为负（方向不变、金额取负，如原 Dr 50 → 红 Dr -50）。容差：BigDecimal 精度经 `Number()` 转换后 `toBe` 精确匹配（金额为确定性种子派生，无浮点误差风险——P2P 行 10×5=50 / 税 6.5 / 合计 56.5；O2C 行 10×10=100 / 税 13 / 合计 113）
  - Skill: `nop-testing`
- [x] `Proof`：`p2p-chain.spec.ts` 新增凭证行断言——Invoice approve 后经 `findVoucherIdByBillCode(invoice.code, 'NORMAL')` 取 AP_INVOICE 凭证 id → `assertVoucherLines` 断言 3 行：subjectCode=1403 debit=50、subjectCode=2221 debit=6.5、subjectCode=2202 credit=56.5（派生自 `P2P_EXPECT` 常量）
  - Skill: `nop-testing`
- [x] `Proof`：`o2c-chain.spec.ts` 新增凭证行断言——Invoice approve 后经 `findVoucherIdByBillCode(invoice.code, 'NORMAL')` 取 AR_INVOICE 凭证 id → `assertVoucherLines` 断言 3 行：subjectCode=1131 debit=113、subjectCode=6001 credit=100、subjectCode=2221 credit=13（派生自 `O2C_EXPECT` 常量）
  - Skill: `nop-testing`

Exit Criteria:

- [x] AP_INVOICE 凭证 3 行（1403 Dr 50 / 2221 Dr 6.5 / 2202 Cr 56.5）+ AR_INVOICE 凭证 3 行（1131 Dr 113 / 6001 Cr 100 / 2221 Cr 13）精确数值断言全绿，每行 subjectCode + debitAmount/creditAmount 均经验立断言

### Phase 2 - 反向冲销红字凭证行同向取负断言

Status: completed
Targets: `tests/e2e/orchestration/p2p-reverse.spec.ts`、`tests/e2e/orchestration/o2c-reverse.spec.ts`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 `assertVoucherLines` 原语已落地

- [x] `Proof`：`p2p-reverse.spec.ts` 新增红字凭证断言——经 `findVoucherIdByBillCode(invoice.code, 'REVERSAL')` 取红字凭证 id → `assertVoucherLines` 断言 3 行（subjectCode/dcDirection 不变、金额取负）：subjectCode=1403 DEBIT debitAmount=**-50**、subjectCode=2221 DEBIT debitAmount=**-6.5**、subjectCode=2202 CREDIT creditAmount=**-56.5**。同时断言原正常凭证行（voucherId=originalVoucherId）金额不变（正数），验证红冲仅新增红字凭证不修改原凭证行
  - Skill: `nop-testing`
- [x] `Proof`：`o2c-reverse.spec.ts` 新增红字凭证断言——经 `findVoucherIdByBillCode(invoice.code, 'REVERSAL')` 取红字凭证 id → `assertVoucherLines` 断言 3 行：subjectCode=1131 DEBIT debitAmount=**-113**、subjectCode=6001 CREDIT creditAmount=**-100**、subjectCode=2221 CREDIT creditAmount=**-13**（subjectCode/dcDirection 不变、金额取负）
  - Skill: `nop-testing`

Exit Criteria:

- [x] P2P + O2C 红字凭证行同向取负（subjectCode/dcDirection 不变、debitAmount/creditAmount 为原行负值）断言全绿

### Phase 3 - Return approve 过账凭证行断言

Status: completed
Targets: `tests/e2e/business-actions/pur-return.action.spec.ts`、`tests/e2e/business-actions/sal-return.action.spec.ts`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 `assertVoucherLines` 原语已落地；`0335-1` Return spec 已落地（approve→posted=true + 清理原语）

- [x] `Proof`：`pur-return.action.spec.ts` 新增 PURCHASE_RETURN 凭证行断言——approve 后经 `findVoucherIdByBillCode(return.code, 'NORMAL')` 取凭证 id → `assertVoucherLines` 断言 2 行：subjectCode=2202 debit=不含税退货金额、subjectCode=1401 credit=不含税退货金额（派生自 Return 行 quantity×unitPrice 不含税）
  - Skill: `nop-testing`
- [x] `Proof`：`sal-return.action.spec.ts` 新增 SALES_RETURN 凭证行断言——approve 后经 `findVoucherIdByBillCode(return.code, 'NORMAL')` 取凭证 id → `assertVoucherLines` 断言 2 行：subjectCode=1401 debit=退货成本(TOTAL_COST)、subjectCode=6401 credit=退货成本（TOTAL_COST 派生自 Return 行 quantity×unitCost，需从 Return 行 or 库存移动产物获取 cost）
  - Skill: `nop-testing`

Exit Criteria:

- [x] PURCHASE_RETURN（2202 Dr / 1401 Cr 不含税退货金额）+ SALES_RETURN（1401 Dr / 6401 Cr 退货成本）凭证行精确数值断言全绿

### Phase 4 - 文档对齐

Status: completed
Targets: `docs/testing/e2e-runbook.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 全绿

- [x] `Add`：`docs/testing/e2e-runbook.md` 跨域编排层段增「凭证行精确数值断言」子段——`assertVoucherLines` 原语范式 + Provider 结构派生期望值表（AP_INVOICE 3 行 / AR_INVOICE 3 行 / PURCHASE_RETURN 2 行 / SALES_RETURN 2 行 / 红字镜像）+ 套件计数更新（167→167+N，N=新断言在既有 spec 内联不增 spec 文件）
  - Skill: none

Exit Criteria:

- [x] e2e-runbook 含凭证行断言子段 + Provider 期望值表 + 套件计数一致

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0b6db01f4ffe`，独立 general 子代理，新会话冷重播无执行者上下文) — 1 BLOCKER + 2 MINOR：
  - B1（BLOCKER）：Phase 2 红字凭证断言基于错误理解——声称借贷方向互换（Dr→Cr），实际 `ErpFinPostingProcessor.buildReversalDraft:575` 保持 dcDirection 不变、金额取负（debitAmount=-50 非 credit=50）。Phase 2 断言将全部失败。
  - m1（MINOR）：Task Route 引用 `docs/architecture/posting.md` 不存在，应为 `docs/design/finance/posting.md`。
  - m2（MINOR）：Phase 2/3 Item Types 标 `Add | Proof` 但无 Add 项，应为 `Proof`。
  - **已修复**：B1——重写 Phase 2 断言为同向取负（subjectCode/dcDirection 不变、debitAmount/creditAmount 为负值），修正 assertVoucherLines 原语描述支持负值匹配，修正 Goals + Current Baseline 红字凭证描述；m1——修正 owner doc 路径；m2——Phase 2/3 Item Types 改为 `Proof`。
- Independent draft review iteration 2: accept (`ses_0b6d5a24fffe`，独立 general 子代理，新会话冷重播) — B1/m1/m2 全部修复确认：`buildReversalDraft:575` 保持 dcDirection 不变 + 金额取负 经源码核实；Phase 2 断言（1403 DEBIT debit=-50 等）与源码一致；owner doc 路径修正；Item Types 修正。无新问题。

## Closure Gates

- [x] 范围内行为完成（P2P/O2C/Return/Reverse 四链路凭证行精确数值断言全绿）
- [x] 相关文档对齐（e2e-runbook 凭证行断言子段 + 套件计数）
- [x] 已运行验证：`npx playwright test tests/e2e/orchestration/ tests/e2e/business-actions/{pur-return,sal-return}.action.spec.ts --workers=1`（四链路全绿 0 回归）+ `mvn install -DskipTests`（154 模块 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 库存移动过账凭证行断言（PURCHASE_INPUT / SALES_OUTPUT）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Receive/Delivery approve 触发的库存移动经 `InvAcctDocProvider` 过账（PURCHASE_INPUT: Dr 1401 / Cr 2202；SALES_OUTPUT: Dr 6401 / Cr 1401），凭证行结构不同。本计划聚焦发票级过账（AP_INVOICE/AR_INVOICE）+ 退货 + 反向。
- Successor Required: `yes`
- Trigger Condition: 当需验证库存移动过账凭证行级科目码/金额正确性时。

### NCR SCRAP 过账凭证行断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: NCR resolve + postNcr（SCRAP 处置）经 `NcrScrapAcctDocProvider`（借 6711 报废损失 / 贷 1401 库存商品）过账。需 NCR resolve + CAPA 闭包前置链路 + config-gated postNcr 触发。属不同结果面。
- Successor Required: `yes`
- Trigger Condition: 当需验证 NCR SCRAP 过账凭证行级正确性时（见 `0335-2` Deferred）。

## Closure

Status Note: completed — 独立结束审计 PASS（ses_0b6c7943affenFhjf6u4J31etl，新会话冷重播），无 BLOCKER。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话 ses_0b6c7943affenFhjf6u4J31etl）冷重播审计 — VERDICT: PASS，0 BLOCKER，3 MINOR（closure 门控/日志待填写[本步已完成] + 无关 0704-2 untracked 文件[非本计划范围]）。
- 执行验证证据：`npx playwright test tests/e2e/orchestration/ tests/e2e/business-actions/pur-return.action.spec.ts tests/e2e/business-actions/sal-return.action.spec.ts --workers=1` → 8 passed（1.2m，含新凭证行断言：AP_INVOICE 3 行 / AR_INVOICE 3 行 / 红字同向取负 3+3 行 / PURCHASE_RETURN 2 行 / SALES_RETURN 2 行）；`mvn install -DskipTests` → BUILD SUCCESS（154 模块，1m22s）。
- 逐 Phase 核实：Phase1 `_helper.ts` 两原语 + p2p/o2c-chain 断言匹配 P2P/O2C_EXPECT 与 Pur/SalAcctDocProvider 源码；Phase2 红字断言匹配 `buildReversalDraft:575`(dcDirection 不变+金额取负)+`persistVoucher:646-648`(取负值路由至正确借/贷列) + 原凭证行不变；Phase3 pur-return 头 totalAmount=25(因 PurReturnPostingDispatcher:92 读头字段无 rollup)+sal-return computeTotalCost=5×10=50(行级聚合)；Phase4 e2e-runbook:258-282 凭证行断言子段+期望值表。
- 范围核实：`git diff --stat` 仅 6 个 tests/e2e/**.ts + docs/testing/e2e-runbook.md（+本计划/日志/backlog 文档），零生产 Java/ORM/model/.api.xml 变更。

Follow-up:

- 库存移动过账凭证行断言 / NCR SCRAP 过账凭证行断言 —— 见「Deferred But Adjudicated」各自 successor 触发条件。
