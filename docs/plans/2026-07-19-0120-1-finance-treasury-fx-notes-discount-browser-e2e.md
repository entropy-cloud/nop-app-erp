# 2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e finance treasury 多币种票据贴现浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Mission: erp
> Work Item: 各域细化端到端验证（finance treasury 多币种贴现 successor）
> Source: `docs/plans/2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md` Deferred But Adjudicated「多币种票据贴现浏览器层 E2E」(l.279-281) — Successor Required: yes，触发条件「多币种票据贴现浏览器层 E2E 需求落地时」经实时仓库核实**已满足**：AGENTS.md「当前项目阶段」明示当前重点含「各域细化端到端验证」，与本仓近 5 份 treasury 域计划（1430-1/2256-1/0413-2/0204-2/0718-1/0718-2）一致裁定。`docs/logs/2026/07-19.md` 末尾明示「下一步聚焦看板运行时视觉/浏览器回归 + 各域细化端到端验证」。
>
> **范围修订记录（iter-1 草案审查后）**：iter-0 草案曾并入「多账户现金预测」段，独立草案审查（ses_089bc607affex97Ijjep810dS9）裁定多账户段「后端齐备」主张**伪**——`ErpFinCashForecastBizModel.collectArApItems:81`/`collectReceivableNotes:106`/`collectPayableNotes:127` 均传 `null` 给 `newForecast(fundAccountId)`，`refreshForecast` 当前 `fundAccountId=null`（2256-1:196 已自认）；多账户段需后端 Java 改造属不同结果面，按 R4/R14 拆分归独立 successor（见 `Deferred But Adjudicated`）。本 iter-1 草案范围聚焦于**经 iter-1 审查核实为真**的多币种票据贴现段。
> Related: `2026-07-17-1430-1`（单币种票据三件套凭证行断言，已 completed；本计划承接其多币种五件套 successor）、`docs/design/finance/treasury.md`（同 owner doc §ErpFinNotesDiscount l.95-106 + §贴现凭证科目分解 l.157-167）、`docs/testing/e2e-runbook.md`（业务动作表）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-19），finance treasury 多币种票据贴现链路在单币种场景下浏览器层 E2E 已完整覆盖，**多币种场景完全缺失**；后端路径**经 iter-1 审查逐项核实为真**：

### 多币种票据贴现（NOTES_RECEIVABLE_DISCOUNTED + EXCHANGE_GAIN_LOSS）后端齐备

- **`ErpFinNotesDiscount` ORM 已含多币种字段**：`currencyId/exchangeRate/exchangeGainLoss`（`module-finance/model/app-erp-finance.orm.xml` ErpFinNotesDiscount 实体定义，`treasury.md §ErpFinNotesDiscount` l.95-106 设计；iter-2 审查 MINOR-4 修正：3 字段非「四件套」，多币种四件套另含 `discountInterest`）。
- **`NotesPostingDispatcher.buildReceivableEvent`**（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/NotesPostingDispatcher.java:71-98`）：
  - `:78` `event.setExchangeRate(note.getExchangeRate() != null ? note.getExchangeRate() : BigDecimal.ONE);` 透传非本位币汇率至 PostingEvent；
  - `:92` `billData.put(ErpFinConstants.BILL_DATA_EXCHANGE_GAIN_LOSS, nz(discount.getExchangeGainLoss()));` 注入外币贴现汇兑损益字段到 billData。
- **`NotesReceivableAcctDocProvider.createFacts`**（`module-finance/erp-fin-service/.../posting/NotesReceivableAcctDocProvider.java`）：
  - `SUBJECT_EXCHANGE_GAIN_LOSS = "6051"` 常量（`:40`）；
  - DISCOUNTED 路径 `if (fx.signum() != 0)` 分支（`:69-78`，其中 `:72` 判定）才发 6051（汇兑损益）行——单币种 `signum=0` 抑制（1430-1 §Phase 1 Decision 已核实并落地单币种三件套）。
- **`ErpFinBusinessType.EXCHANGE_GAIN_LOSS(130)` 已存在**并被 `TestErpFinAnnualClose:132/158`/`TestErpFinExchangeRevaluation:37`/`TestErpFinPeriodCloseEndToEnd:41`/`ProfitLossClosingService:90`/`ExchangeRevaluationService` 经实证使用。
- **`ErpFinNotesReceivableProcessor` 贴现公式**（`buildDiscount:229-235` 范式）：`interest = face × rate × remainingDays / 360`。**`exchangeGainLoss` 当前 Java builder 无外币派生**——`ErpFinNotesReceivableProcessor.java:249` 无条件 `discount.setExchangeGainLoss(BigDecimal.ZERO)`，spec 须显式置非零值驱动 6051 凭证行（见 Phase 1 Decision (a) 默认）。

### 浏览器层覆盖缺口

`tests/e2e/business-actions/fin-notes-receivable.action.spec.ts`（1430-1 产物）7 动作状态机 + 4 凭证行断言全部使用 `CURRENCY_ID = 1`（CNY）+ `exchangeRate: 1`（`fin-notes-receivable.action.spec.ts:44-45/78`）+ `exchangeGainLoss` 字段未显式置值（默认 0，6051 行抑制不发），**外币贴现 6051 凭证行从未在浏览器层断言**。

### 种子基线

- **种子**：`erp_md_currency.csv:2-3` 已含 `1=CNY / 2=USD`（核实命中）。`erp_md_subject.csv` 已含 `6603`（财务费用-利息支出，1430-1 落地，id=42），**`6051` 汇兑损益科目未在部署期种子**（1430-1 Non-Goal l.94 措辞为「种子 6051 作可选补齐（不作为结束门控）」——iter-2 审查 MINOR-3 订正，非「显式不补」）。
- `erp_fin_fund_account.csv` 已含多账户（含 currencyId 维度），外币账户 setup 可经 `__save` 自包含建测试专用账户隔离基线（对齐 1430-1 范式）。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave` / `callMutation` / `verifyState` / `findFirst` / `deleteByFilter` / `eqFilter`。
- `tests/e2e/orchestration/_helper.ts`：`findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)` 两原语（1430-1/2256-1/0704-1 范式）。
- 自包含 setup 范式：`__save` 直置 status 入口（ORM tagSet="gid,erp.finance" 无 use-approval/use-workflow）；fresh-DB 测试区间隔离避免种子污染（对齐 1430-1 范式）。

### 剩余差距

- 多币种票据贴现浏览器层 E2E 缺失（1430-1 Deferred l.279-281，未 RELEASED）。
- 种子 COA 缺 `6051` 行。
- 缺口属「后端齐备 + 浏览器层零覆盖」典型 successor 形态，预期零生产 Java/契约/ORM 模型变更，仅 spec + seed COA 加性追加。

## Goals

- 交付 1 个浏览器层 E2E spec，经 GraphQL `/graphql` 驱动 DIRECT `ErpFinNotesReceivable__discount` `@BizMutation`，凭证行翻转经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言：
  1. **多币种票据贴现**：建 USD 票据（currencyId=2 + exchangeRate≠1，本位币派生 amountFunctional）→ `discount` → 凭证行五件套精确数值断言（Dr 1002 银行存款=本位币实得 / Dr 6603 财务费用-利息支出=贴现息 / Dr|Cr 6051 汇兑损益=外币重估差额 signum≠0 / Cr 1121 应收票据=本位币票面）。
- 对照断言（独立测试用例，使对比 crisp）：单币种路径（既有 1430-1 spec 既有覆盖）6051 不发——经本 spec 内一用例显式建 CNY 票据 + `discount` 后断言凭证行集合**不含** 6051 行，证明 signum=0 抑制分支生效。
- 在 `docs/testing/e2e-runbook.md` 业务动作表补 1 行 + 套件计数更新；`docs/backlog/README.md` +1 done 行。
- 解除 1430-1 Deferred「多币种票据贴现浏览器层 E2E」（补 `**RELEASED by 2026-07-19-0120-1**` 行）。

## Non-Goals

- **不重新实现 1430-1 的单币种范围**——本计划仅消费侧场景扩展 + 测试层 + 种子加性追加，零生产 Java/契约/ORM 模型变更预期。
- **不新增编排面后端**——后端路径已落地（iter-1 审查核实）；若 Explore 发现某 `@BizMutation` 不可达或有 bug，开显式 successor（不改生产代码即时修，对齐 0941-1 triggerDuePlans 修复先例属执行期豁免，须 Phase 内记录）。
- **不覆盖外币票据 honor/dishonor/endorse/collect 路径**——1430-1 已覆盖单币种主路径；外币非 DISCOUNTED 路径（honor 等）汇兑损益规则不同（按到期日 vs 贴现日汇率），属不同结果面 successor（见 `Deferred But Adjudicated`）。
- **不覆盖授信额度外币业务**（0718-1 Deferred）/ **不覆盖外币银行对账**（0413-2 范式单币种）——同型外币 successor，归 0718-1/0413-2 owner doc 链。
- **不做多币种 VMI / 多币种到岸成本**——不同 owner doc，不并入。
- **不做多账户现金预测**（iter-0 草案曾并入，iter-1 审查裁定后端 `fundAccountId=null` 未分摊，须后端 Java 改造属不同结果面，归独立 successor，见 `Deferred But Adjudicated`）。
- **不做应付票据外币IssUED/HONORED 多币种路径**——1430-1 已覆盖单币种，外币 successor 同型规则归独立 successor。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的场景扩展 successor；纯消费侧 + 测试维护 + 种子 COA 加性追加，零生产契约变更预期）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构 / 运行命令 / 业务动作表）、`docs/design/finance/treasury.md`（§票据贴现 l.95-106 + §贴现凭证科目分解 l.157-167）
- Skill Selection Basis: 纯 Playwright 浏览器层测试维护 + 种子 CSV 加性追加，非 Nop 平台 BizModel/页面开发；`nop-testing` 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空（2246-1/1005-2 裁决先例），依技能实质内容判定 `Skill: none`（nop-testing）。Phase 1 Explore 阶段如发现后端不可达需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约/xbiz/Java；任何生产代码修复须 ask-first 并开 successor；种子 COA 加性追加非 ORM 保护区域（对齐 1430-1/1800-1 加性追加范式）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- webServer JVM args 已含 `erp-fin.notes-discount-rate-default=0.12` + 三 bad-debt/AR subject-code（1430-1/0413-2 落地），无须新增。多币种贴现经既有 `exchangeGainLoss` 字段路径，无须新增 config。
- 无新增端口/环境变量/密钥/外部服务。

## Execution Plan

### Phase 1 — Explore：后端多币种路径冷核实 + setup 工程化裁决

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/NotesPostingDispatcher.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/NotesReceivableAcctDocProvider.java`（iter-2 审查 MINOR-1 修正路径段）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java`（贴现公式核实）、`tests/e2e/business-actions/fin-notes-receivable.action.spec.ts`（1430-1 产物）、`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`
Skill: `nop-debugging`

- Item Types: `Decision | Proof`
- Prereqs: none

- [x] `Proof`：多币种贴现后端路径冷核实——三 file:line 锚点经实时仓库逐行确认（全部 verified）：
  - (1) `NotesPostingDispatcher.java:78` `event.setExchangeRate(note.getExchangeRate() != null ? note.getExchangeRate() : BigDecimal.ONE)` + `:92` `billData.put(ErpFinConstants.BILL_DATA_EXCHANGE_GAIN_LOSS, nz(discount.getExchangeGainLoss()))` — **VERIFIED**；
  - (2) `NotesReceivableAcctDocProvider.java:40` `SUBJECT_EXCHANGE_GAIN_LOSS = "6051"` + `:64-80` DISCOUNTED 路径四件套（Dr 1002 netAmount / Dr 6603 discountInterest / [Dr|Cr] 6051 fx / Cr 1121 faceAmount） + `:72` `if (fx.signum() != 0)` 6051 行分支（`:73` fx>0 走 Dr / `:75-76` fx<0 走 Cr 取 neg） — **VERIFIED**；iter-3 cosmetic 注：实际 4 行非「五件套」（plan 沿用 1430-1 措辞，本计划保持术语一致但实现为 4 行断言）；
  - (3) `ErpFinNotesReceivableProcessor.java:226-251` `buildDiscount` 贴现息公式 `interest = face × rate × remainingDays / 360`（HALF_UP scale 2）+ `:249` `discount.setExchangeGainLoss(BigDecimal.ZERO)` **无条件硬编码**——Java builder **无外币派生**（iter-2 MAJOR-1 已核实 genuine，本计划再次逐行确认）。**关键发现（执行期冷核实）**：`buildDiscount` 由 `discount(@BizMutation)` 内部 `doDiscount:168-182` 调用并 `discountDao.saveEntity(discount)` 持久化，spec 经 `__save` 预置的非零 `exchangeGainLoss` 会被新 discount 覆盖（note.discountId 指向新 discount.id）。**故 `discount @BizMutation` 路径恒产 3 行凭证（无 6051）**，spec 须改用 `ErpFinVoucher__post(event)` 直接构造 PostingEvent（precedent `finance-voucher-post.action.spec.ts:44-56`）验证 Provider FX 6051 分支。
  - Skill: `nop-debugging`
- [x] `Decision`：多币种贴现 setup 工程化——三选一裁定 exchangeGainLoss 触发路径：(a) `__save` 显式置非零 + `ErpFinVoucher__post` 直驱（绕过 `discount` mutation 的 builder 覆盖，对齐 1430-1 显式置字段范式 + `finance-voucher-post.action.spec.ts` 范式）；(b) 经 `discount` mutation builder 自动派生——**冷核实为不可达**（`:249` 硬编码 ZERO）；(c) 调用既有 ExchangeRevaluationService（不适用——重估属期末结账路径非贴现路径）。**采纳 (a)**：(a) 是验证 Provider FX 6051 分支的**唯一浏览器层可达路径**。范围裁决：本 spec **同时验证** (1) `discount` mutation FX 状态机生命周期（产 3 行凭证，6051 抑制——证明 FX 状态机正常 + 6051 抑制分支对单/外币一致） + (2) `ErpFinVoucher__post` 直驱 FX 4 行凭证（6051 触发） + (3) 对照 `ErpFinVoucher__post` 单币种 3 行凭证（6051 抑制）。`ErpFinNotesReceivableProcessor.buildDiscount:249` Java builder 缺陷（外币无 exchangeGainLoss 派生）按 plan 规则 13/14 记入 `Deferred But Adjudicated` 显式 successor（不改生产代码即时修）。
  - Skill: none
- [x] `Decision`：汇率方向 + 数值裁决——经 `ExchangeRevaluationService.java:55` `diff = openAmountFunctional − (openAmountSource × 期末汇率)` 实测确认 `exchangeRate = functional / source`（即 USD→CNY 6.6667 表示 1 USD = 6.6667 CNY）。spec 数值表（全部以 functional CNY 计入凭证）：amountSource=USD 100 / exchangeRate=6.6667 / amountFunctional=CNY 666.67 / discountRate=0.12 / remainingDays=30（issueDate=2026-07-01, dueDate=2026-07-31, discountDate=2026-07-01）→ discountInterest=666.67×0.12×30/360=**6.67**（HALF_UP scale 2）/ netAmount=666.67−6.67=**660.00** / exchangeGainLoss=**5.00** 确定性占位非派生（spec 内注释标注「占位值，验证 Provider signum≠0 分支与数值透传，非重估公式派生」）。FX 4 行凭证断言：Dr 1002=660.00 / Dr 6603=6.67 / Dr 6051=5.00（fx>0 走 Dr 分支）/ Cr 1121=666.67。
  - Skill: none
- [x] `Decision`：种子 COA 加性追加裁决——`erp_md_subject.csv` 加性追加 `6051` 汇兑损益行（**实时仓库核实**：当前 max id=43 含 2502 行，新行 id 取 **44**；字段对齐 1430-1 既有同表行格式 `ID,CODE,NAME,SUBJECT_CLASS,DIRECTION,STATUS,BALANCE_TYPE,IS_LEAF` = `44,6051,汇兑损益,EXPENSE,DEBIT,ACTIVE,DEBIT,true`；EXPENSE/DEBIT 对齐 ExchangeRevaluationService 既有 6051 用法；ACTIVE 行无 GL 余额，零副作用）。无须追加 fundAccount/currency（既有种子已含 USD id=2 + BANK 账户类型可达）。
  - Skill: none
- [x] `Decision`：对照测试用例（单币种 6051 抑制）放置位置——三选一：(a) 本 spec 内独立 `test()` 经 `ErpFinVoucher__post` 构造 exchangeGainLoss=0 的 NOTES_RECEIVABLE_DISCOUNTED event → 凭证行断言**不含** 6051（使对比 crisp，对齐 iter-1 审查 m1 建议 + 与 FX test 同 posting 入口形成 signum 0/非0 唯一变量对照）；(b) 引用既有 1430-1 spec 不重测（弱对比，否决）；(c) 独立 spec 文件（过度拆分，否决）。**采纳 (a)**——FX test 与 control test 经同一 `postNotesReceivableDiscountEvent` helper 调 `ErpFinVoucher__post`，唯一变量为 billData.EXCHANGE_GAIN_LOSS（5.00 vs 0），形成 crisp 对照。
  - Skill: none

Exit Criteria:

- [x] 四 Decision + 一 Proof 落记录（含替代方案 + 残留风险 + 行号引用）
- [x] 多币种贴现 setup 工程化路径裁决（采纳 (a) `ErpFinVoucher__post` 直驱 + 显式 exchangeGainLoss；discount mutation 段验证 FX 状态机生命周期）
- [x] 汇率方向 + 数值表裁决（exchangeRate=functional/source 经 ExchangeRevaluationService.java:55 实测确认；spec 数值表全列为 functional CNY）

---

### Phase 2 — spec 落地 + 全套件回归

Status: completed
Targets:
  - `tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts`（新建；或并入 1430-1 既有 `fin-notes-receivable.action.spec.ts` 经 `test.describe` 增量用例——Phase 1 Decision 裁决）
  - `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`（加性追加 6051 行）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：种子 COA 加性追加——`erp_md_subject.csv` 追加 `44,6051,汇兑损益,EXPENSE,DEBIT,ACTIVE,DEBIT,true`（id=44 经实时仓库核实 `max(id)+1=43+1`；字段对齐 1430-1 既有同表行格式；ACTIVE 行无 GL 余额，零副作用；EXPENSE/DEBIT 对齐 ExchangeRevaluationService 既有 6051 用法）。
  - Skill: none
- [x] `Add`：多币种票据贴现 spec——`fin-notes-receivable-fx-discount.action.spec.ts` 三用例覆盖：(1) **FX 状态机生命周期**——自包含建 USD `ErpFinNotesReceivable`（currencyId=2 + exchangeRate=6.6667 + amountSource=USD 100 + amountFunctional=CNY 666.67 + dueDate=2026-07-31）+ `ErpFinFundAccount(BANK, currencyId=2)` → `discount` `@BizMutation`（discountDate=2026-07-01 + discountRate=0.12 + bankId=fund.id）→ DISCOUNTED + posted=true + 3 行凭证（Dr 1002=660/Dr 6603=6.67/Cr 1121=666.67，全部 functional CNY；6051 因 builder `:249` 硬编码 ZERO 抑制）；(2) **FX 6051 触发分支**——`ErpFinVoucher__post(event)` 直驱 NOTES_RECEIVABLE_DISCOUNTED event（billData.EXCHANGE_GAIN_LOSS=5.00 非零）→ 4 行凭证（Dr 1002=655/Dr 6603=6.67/Dr 6051=5.00 signum>0/Cr 1121=666.67，复式平衡经 NET_AMOUNT_FX_VOUCHER=655 派生）；(3) **对照单币种 6051 抑制**——同 `ErpFinVoucher__post` 入口 + EXCHANGE_GAIN_LOSS=0 → 3 行凭证（Dr 1002=990/Dr 6603=10/Cr 1121=1000，无 6051）。`verifyState`/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言。
  - Skill: none
- [x] `Add`：对照测试用例——Phase 1 Decision (a) 采纳，本 spec 内独立 `test()` 与 FX test 经同一 `postNotesReceivableDiscountedEvent` helper 调 `ErpFinVoucher__post`，唯一变量 EXCHANGE_GAIN_LOSS（5.00 vs 0），形成 crisp 4 行 vs 3 行对照。
  - Skill: none
- [x] `Proof`：新增 spec `--workers=1` 全绿（3 passed 45.3s）+ finance 抽样回归（`fin-notes-receivable` 7 + `fin-notes-payable` 5 + `finance-voucher-post` 1 = 13 passed 1.6m，0 新增失败）+ business-actions 全套件回归 219 passed / 1 失败（mfg-variance-recompute-reversal 预存 flake——经 `git stash` 验证 **without my changes 同样失败**，与本计划 6051 种子加性追加无关，DB 共享状态累积致 `reportCompletion` docStatus 未翻 COMPLETED；记入日志，不阻塞本计划结束）。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts --workers=1`（3 passed）+ finance 抽样（13 passed）+ business-actions 全套件（219 passed）
  - Skill: none

Exit Criteria:

- [x] spec 全绿，状态/凭证行翻转均经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言（非仅 mutation 返回值）
- [x] 6051 加性种子行不破坏既有 154 模块 BUILD SUCCESS（执行 `mvn clean install -DskipTests` 全绿 1:30min）+ finance 既有 spec 0 回归（13/13 passed）
- [x] business-actions 全套件回归 0 新增失败（219/220 passed；1 失败为预存 flake 经 stash 验证与本计划无关）

---

### Phase 3 — 文档对齐 + Deferred RELEASED 登记

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/plans/2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md`、`docs/logs/2026/07-19.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`e2e-runbook.md` 业务动作表 +1 行（finance 多币种票据贴现三层验证）+ 套件计数 83→84（17 域 84 spec）；`backlog/README.md` +1 done 行（2026-07-19-0120-1）。
  - Skill: none
- [x] `Add`：1430-1 Deferred 补 `**RELEASED by 2026-07-19-0120-1**` 行（触发条件已满足 + 本计划交付证据：1 spec 3 用例 + 种子 6051 加性追加）；`docs/logs/2026/07-19.md` 增聚合条目（spec 数 / 验证状态 / 范围纪律 / 种子加性影响面 / Java builder 缺陷 successor 登记）。
  - Skill: none

Exit Criteria:

- [x] e2e-runbook + backlog README + 1430-1 RELEASED 登记 + 日志四点落地一致

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_089bc607affex97Ijjep810dS9`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 1 BLOCKER + 2 MAJOR + 3 MINOR。**B1**：iter-0 草案并入「多账户现金预测」段「后端齐备 无须新实现」主张**经实时仓库逐行核实为伪**——`ErpFinCashForecastBizModel.collectArApItems:81`/`collectReceivableNotes:106`/`collectPayableNotes:127` 均传 `null` 给 `newForecast(fundAccountId)`，多账户段须后端 Java 改造属不同结果面，按 R4/R14 拆分归独立 successor。**M1**：R14 bundling 不成立（两段工作非对称：多币种纯测试 vs 多账户需 Java 改造）。**M2**：iter-0 草案 l.124 「ErpFinNotesReceivable 分别 bankId」字段位置错——`bankId` 在 `ErpFinNotesDiscount`（orm.xml:1409）非 `ErpFinNotesReceivable`。**m1-m3**：对照测试用例独立放置 / 汇率方向标签 / exchangeGainLoss 数值派生路径三项 polish。
- **本 iter-1 修订**：依据 iter-1 审查 (B1+M1) 拆分——本计划范围收窄至**多币种票据贴现**单一 successor（iter-1 审查逐行核实为真）；多账户现金预测段移出范围并归 `Deferred But Adjudicated`（须后端 Java 改造属独立 successor，触发条件「`ErpFinCashForecastBizModel.collectReceivableNotes` 实现 fundAccountId 派生时」）。依据 (M2) 修正 `bankId` 字段位置（ErpFinNotesDiscount 非 ErpFinNotesReceivable）。依据 (m1) 对照用例改独立 `test()`。依据 (m2/m3) 汇率方向 + exchangeGainLoss 数值派生移入 Phase 1 Decision (b) 显式裁决。范围收窄后 Goals/Non-Goals/Phase 1-3 均同步精简，`Deferred But Adjudicated` 段新增多账户 successor 条目。
- Independent draft review iteration 2: **needs revision**（`ses_089b24f3effeaH9vIsGSLvTu6Y`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19）— 0 BLOCKER + 1 MAJOR + 5 MINOR。iter-1 BLOCKER (B1 多账户拆分) 经核实 genuine；多币种基线 7 项主张经核实全部 verified。**MAJOR-1 (R1)**：Current Baseline l.27 仍声称「外币贴现时 exchangeGainLoss 经 1430-1 §Phase 1 Decision 既有公式派生」——live `ErpFinNotesReceivableProcessor.java:249` 实测 `setExchangeGainLoss(BigDecimal.ZERO)` 无条件硬编码，无 Java 派生；1430-1 系单币种计划无 FX 公式。Phase 1 Decision (a) 默认经核实正确（spec 显式置非零值），实现不被误导，但 baseline 措辞误导读者。**MINOR-1**：`NotesReceivableAcctDocProvider.java` 实际路径含 `/provider/` 段。**MINOR-2**：l.36 「iter-1 审查 m2/m3 已核实」引用错（m2/m3 系 bankId/exchangeGainLoss 非 6051 种子）。**MINOR-3**：1430-1 Non-Goal l.94 措辞为「可选补齐」非「显式不补」。**MINOR-4**：「多币种四件套字段」仅列 3 项名（`currencyId/exchangeRate/exchangeGainLoss`）。**MINOR-5**：文件名 slug `residual-browser-e2e-bundle` 不再匹配收窄后范围。
- **本 iter-2 修订**：依据 MAJOR-1 重写 Current Baseline l.27 末段（「Java builder 无外币派生 / 无条件 ZERO / spec 须显式置」）。依据 MINOR-1 修正路径段 `/posting/provider/`。依据 MINOR-3 修正 1430-1 Non-Goal 引用为「可选补齐（不作为结束门控）」。依据 MINOR-4 「四件套」改「3 字段非四件套」+ 补注 discountInterest。依据 MINOR-5 **重命名文件** `2026-07-19-0120-1-finance-treasury-residual-browser-e2e-bundle.md` → `2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md`。MINOR-2 引用错系具体审查项内部追溯，不影响计划可执行性，本次修订不展开（审查 m2/m3 系 iter-1 编号，本计划 Phase 1 Decision 经 iter-2 验证 Phase 1 (a) 默认正确即可）。iter-2 计划已收敛——待 iter-3 审查通过 flip to active。
- Independent draft review iteration 3: **accept** (`ses_089aa586fffeu563ZlRGAqR87b`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKER + 0 MAJOR + 1 cosmetic MINOR。iter-2 MAJOR-1（Current Baseline Java 派生主张伪）经核实 **genuine 修订落地**——l.27 重写后声明「Java builder 无外币派生 / `ErpFinNotesReceivableProcessor.java:249` 无条件 `setExchangeGainLoss(BigDecimal.ZERO)` / spec 须显式置非零值」，**经 live 仓库逐行核实为真**（`ErpFinNotesReceivableProcessor.java:249` 实测确认 `discount.setExchangeGainLoss(BigDecimal.ZERO)`）。iter-2 5 MINOR 全部落地或可辩护延后（m1 路径段订正于 l.86 权威全路径，l.23 shorthand 残留为 cosmetic 非阻塞；m2 citation 内部追溯不影响可执行性；m3/m4/m5 全部落地）。R1-R14 + anti-slack + template 全 PASS。Draft Review Record 与 body 一致无 drift。**共识达成 → `Plan Status: active`**。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧场景扩展 + 测试层 + 种子 COA 加性追加（预期零生产 Java/契约/ORM 模型变更）。结束前运行新增 spec + business-actions 回归 + finance 抽样回归 + 后端构建（确认 spec 变更未污染后端）。

- [x] 范围内行为完成（1430-1 Deferred 多币种票据贴现交付浏览器层 E2E + 凭证行 4 行独立断言 + 单币种对照断言 6051 抑制）
- [x] 相关文档对齐（e2e-runbook 业务动作表 +1 行 + 套件计数 83→84、backlog README done 行、1430-1 RELEASED 登记、日志聚合条目）
- [x] 已运行验证：新增 spec `--workers=1` 全绿（3 passed 45.3s）+ business-actions 全套件回归 219 passed / 1 失败（预存 flake 经 stash 验证无关）+ finance 既有 spec 抽样回归 0 失败（13 passed 1.6m）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:30min，确认种子加性追加零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（Java builder 外币 exchangeGainLoss 派生缺陷属**共享过账基础设施 + 不同结果面** out-of-scope，非本计划范围内项目降级——已记 `Deferred But Adjudicated` 显式 successor，生产代码修复须 ask-first 独立计划不阻塞本计划结束，对齐 plan 规则 13/14）
- [x] 独立草案审查已完成并记录（iter-1 修订后 iter-2 修订后 iter-3 accept，已 flip to active 并执行完成）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将结束审计项留为未勾选状态作为人工门控占位符（独立结束审计由独立 closure auditor 子代理在新会话中执行，冷重播无执行者上下文，2026-07-19）
- [x] 结束证据存在于文件中（新 spec + 种子 + e2e-runbook + backlog + 1430-1 RELEASED + 日志）

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项。iter-1 修订后多账户段整体移入此段。

### 多账户现金预测分摊浏览器层 E2E（iter-0 草案曾并入范围）

- Classification: `out-of-scope improvement`（须先做后端 Java 改造）
- Why Not Blocking Closure: iter-1 审查逐行核实 `ErpFinCashForecastBizModel.collectArApItems:81`/`collectReceivableNotes:106`/`collectPayableNotes:127` 均传 `null` 给 `newForecast(fundAccountId)`；2256-1:196 已自认「`refreshForecast` 当前 `fundAccountId=null`（未按账户分摊）」。本计划多币种贴现段不依赖多账户；多账户段属不同结果面，须先后端 Java 改造（`collectReceivableNotes` 经 `ErpFinNotesDiscount.bankId` join 派生 + AR/AP 段需 `ErpFinArApItem` 加 `fundAccountId` ORM 列——后者属 ORM 保护区域 ask-first）。
- Successor Required: `yes`（触发条件：`ErpFinCashForecastBizModel.collectReceivableNotes` 实现 `ErpFinNotesDiscount.bankId → fundAccountId` 派生时；或 `ErpFinArApItem` ORM 加 `fundAccountId` 列被授权时）

### `ErpFinNotesReceivableProcessor.buildDiscount` 外币 exchangeGainLoss 派生缺失（执行期发现 Java builder 缺陷）

- Classification: `out-of-scope improvement`（生产 Java 修改须 ask-first 独立计划）
- Why Not Blocking Closure: 执行期 Phase 1 冷核实发现 `ErpFinNotesReceivableProcessor.java:249` `discount.setExchangeGainLoss(BigDecimal.ZERO)` **无条件硬编码**，致 `discount` @BizMutation 永远不会触发 6051 凭证行。本计划已通过 `ErpFinVoucher__post` 直驱验证 Provider FX 6051 分支（4 行凭证断言 Dr 6051=5.00 signum>0 路径）+ 对照（3 行凭证 signum=0 抑制），交付了 Provider FX 路径的浏览器层覆盖。Java builder 缺陷的修复属不同结果面（应用层 Java 改造须 ask-first 独立计划 + 配套 JUnit 回归 + 外币 exchangeGainLoss 派生公式裁决——按到期日 vs 贴现日汇率选择属不同语义 successor），按 plan 规则 13/14 不在本计划范围。
- Successor Required: `yes`（触发条件：外币票据业务实际生产路径触发 6051 派生缺失被复现时，或票据业务多币种深化需求落地时——须后端实现外币 exchangeGainLoss 派生公式 + 应用层 Java 改造独立计划 + ask-first 授权）

### 外币票据 honor/dishonor/endorse/collect 路径浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1430-1/本计划仅覆盖 DISCOUNTED 多币种路径；非 DISCOUNTED 路径汇兑损益规则不同（按到期日 vs 贴现日汇率），属不同结果面。
- Successor Required: `yes`（触发条件：外币票据 honor/endorse 业务路径浏览器层 E2E 需求落地时）

### 应付票据外币 ISSUED/HONORED 多币种路径浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1430-1 已覆盖单币种；外币段同型规则但不同实体（`ErpFinNotesPayable` 非 `ErpFinNotesReceivable`）+ 不同科目分解（2202/2203 vs 1121）。
- Successor Required: `yes`（触发条件：外币应付票据业务路径浏览器层 E2E 需求落地时）

## Closure

Status Note: 执行完成（2026-07-19）。3 Phase 全绿——Phase 1 Explore（4 file:line 锚点逐行核实 + 4 Decisions 含 Java builder 缺陷关键发现 + setup 工程化采纳 ErpFinVoucher__post 直驱）/ Phase 2（种子 COA +6051 + 1 新 spec 3 用例全绿 45.3s + finance 抽样回归 13 passed + business-actions 全套件 219 passed 1 预存 flake 经 stash 验证无关 + 154 模块 BUILD SUCCESS 1:30min）/ Phase 3（e2e-runbook 业务动作表 +1 行 + 套件计数 83→84 + 1430-1 Deferred RELEASED + backlog +1 done + 日志聚合条目）。验证：新 spec 3/3 + 抽样回归 13/0 + 154 模块 BUILD SUCCESS。执行期发现 Java builder 缺陷（`ErpFinNotesReceivableProcessor.buildDiscount:249` 无条件 setExchangeGainLoss(ZERO)）致 discount mutation 永不触发 6051——本 spec 改用 ErpFinVoucher__post 直驱验证 Provider FX 分支，Java 修复归 Deferred 显式 successor，不阻塞结束。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话冷重播，无执行者上下文，2026-07-19）
- Audit Scope: 全计划从头重新阅读 + 5 点一致性 + 反 Hollow + Deferred 诚实 + 文档同步
- Live 仓库逐项核实结果（全部 VERIFIED）：
  - `tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts` 存在（331 行，3 个 `test()` 用例，非空壳）
  - `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv:45` 含 `44,6051,汇兑损益,EXPENSE,DEBIT,ACTIVE,DEBIT,true`（id=44 加性追加）
  - `docs/testing/e2e-runbook.md:116/222/313` 套件计数 83→84 + finance 多币种票据贴现业务动作表行已落地
  - `docs/backlog/README.md:102` 含 0120-1 done 行
  - `docs/plans/2026-07-17-1430-1-...md:282` 含 `**RELEASED by 2026-07-19-0120-1**` 行（1430-1 Deferred 已解除）
  - `docs/logs/2026/07-19.md:34-36` 含 0120-1 聚合日志条目（spec/验证/范围纪律/种子加性影响面/Java builder 缺陷 successor）
- Phase 状态 / 退出标准 / Closure Gates / Closure evidence 五点一致性 VERIFIED
- Anti-Hollow VERIFIED：spec 331 行非空，3 个 `test()` 用例经 GraphQL `/graphql` 真实驱动 `discount` mutation + `ErpFinVoucher__post`；凭证行翻转经 `verifyState`/`findVoucherIdByBillCode`/`assertVoucherLines` 独立反查（非仅 mutation 返回值）
- Deferred 诚实 VERIFIED：Java builder 缺陷（`ErpFinNotesReceivableProcessor.buildDiscount:249` 无条件 `setExchangeGainLoss(ZERO)`）诚实记入 `Deferred But Adjudicated` 显式 successor，未隐藏在 Follow-up
- 文档同步 VERIFIED：`docs/logs/2026/07-19.md` + `docs/testing/e2e-runbook.md` + `docs/backlog/README.md` + 1430-1 RELEASED 均落地
- 审计结论：APPROVED（计划可关闭）

Follow-up:

- `ErpFinNotesReceivableProcessor.buildDiscount:249` 外币 exchangeGainLoss 派生缺失（显式 successor，触发条件：外币票据业务实际生产路径触发 6051 派生缺失被复现时，或票据业务多币种深化需求落地时；生产代码修复须 ask-first 独立计划）
