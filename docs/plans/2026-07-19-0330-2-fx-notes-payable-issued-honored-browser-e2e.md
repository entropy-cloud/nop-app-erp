# 2026-07-19-0330-2-fx-notes-payable-issued-honored-browser-e2e 外币应付票据 ISSUED/HONORED 多币种路径浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Mission: erp
> Work Item: 各域细化端到端验证（finance treasury 外币应付票据 successor）
> Source: `docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md` `Deferred But Adjudicated` 一项：「应付票据外币 ISSUED/HONORED 多币种路径浏览器层 E2E」(l.204-208) — Successor Required: yes，触发条件「外币应付票据业务路径浏览器层 E2E 需求落地时」——经实时仓库核实，触发条件经解释为已满足（precedent：0120-1 同型裁决；AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点；1430-1 已覆盖单币种，外币段同型规则但不同实体（`ErpFinNotesPayable` 非 `ErpFinNotesReceivable`）+ 不同科目分解（2202/2203 vs 1121/1122）+ 不同 Provider（`NotesPayableAcctDocProvider` 无 FX 分支））。
> Related: `2026-07-17-1430-1`（单币种票据三件套凭证行断言——含 NP ISSUED/HONORED 单币种 NORMAL + writeOff REVERSAL 凭证行断言，已 completed）、`2026-07-19-0120-1`（多币种票据贴现浏览器层 E2E + NP 外币 successor 登记，已 completed）、`2026-07-19-0330-1`（外币应收票据 honor/endorse/collect 浏览器层 E2E，draft；本计划为 NP 段并行 successor，无强依赖——可独立执行）、`docs/design/finance/treasury.md`（§ErpFinNotesPayable l.79-93 + §业财过账 l.146-147）、`docs/testing/e2e-runbook.md`（业务动作表）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-19 03:30 +0800），finance treasury 外币应付票据生产路径与浏览器层覆盖状态：

### NP Provider + Processor 状态（无须改生产代码）

- **`NotesPayableAcctDocProvider.java:36-40, 47-63`**：支持 2 业务类型（NOTES_PAYABLE_ISSUED / NOTES_PAYABLE_HONORED），均**无 FX 分支**——按 `BILL_DATA_FACE_AMOUNT` 简单 Dr/Cr 2 行凭证（ISSUED `:49-52`: Dr 2202 应付账款 partnerId 非空 / Cr 2203 应付票据；HONORED `:56-57`: Dr 2203 应付票据 / Cr 1002 银行存款）。FX 场景下表现为「按 functional 金额过账不产 6051」——这是设计选择（不同于 NR DISCOUNTED 科目分解），非缺陷。
- **`ErpFinNotesPayableProcessor`**：经 `rg "setExchangeGainLoss" module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesPayableProcessor.java` 零命中——无 FX 派生逻辑，也无硬编码 ZERO 占位（不同于 NR buildDiscount 的 R13 缺陷）。
- **关键 Processor 方法核实**（iter-1 审查 MAJOR-2 修订）：
  - `doIssue:119-128` → setStatus ISSUED + postingDispatcher.tryPostPayable(NOTES_PAYABLE_ISSUED) → 产 NORMAL 凭证。
  - `doHonor:130-139` → setStatus HONORED + postingDispatcher.tryPostPayable(NOTES_PAYABLE_HONORED) → 产 NORMAL 凭证。
  - **`doDishonor:141-144`** → setStatus DISHONORED + `noteDao().updateEntity(note)` **无 postingDispatcher 调用 → 不产凭证**（对齐 1430-1 test 3 `expect(noVoucher).toBeNull()` 显式断言）。
  - `doWriteOff:146-154` → if posted=true: postingDispatcher.reversePayable(NOTES_PAYABLE_ISSUED) 产 REVERSAL 红字凭证（同向取负）+ setStatus WRITE_OFF + 清 posted 三件套。
- **结论**：NP 路径 FX 行为符合设计，无须 Java builder 修复。本计划为纯浏览器层 E2E 场景扩展，零生产代码变更预期。

### ErpFinNotesPayable 实体字段清单（iter-1 审查 MINOR-5 修订）

经 `module-finance/model/app-erp-finance.orm.xml:1374-1377` 核实：
- `currencyId Long` — 币种 ID
- `exchangeRate BigDecimal` — 出票日记账汇率
- `amountSource BigDecimal precision=20 scale=4` — 源币种金额（4 位精度，非 2 位）
- `amountFunctional BigDecimal precision=20 scale=4` — 本位币金额（4 位精度）

ORM tagSet 经核实 `:1363` = `"gid,erp.finance"` 无 use-approval/use-workflow（DIRECT 路径可达）。

### Dispatcher FX 透传链（iter-1 审查 MINOR-3 修订）

- **`NotesPostingDispatcher.java:117`**：`billData.put(BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()))`——`face amount` 透传**functional 金额**至 Provider。
- **REVERSAL FX 行为**：`NotesPostingDispatcher.reversePayable:53-55`（同型 reverseReceivable）→ `executor.reverse(note.getCode(), businessType)` → 平台 `IErpFinVoucherBiz.reverse` 按 `billHeadCode + businessType` 反查原 NORMAL 凭证 → 复制凭证行同向取负（dcDirection 不变金额取负）生成 REVERSAL 凭证 + 标记原凭证 isReversed=true。FX 场景下：原 NORMAL 凭证行金额已为 functional（dispatcher 透传 functional），REVERSAL 行同向取负仍为 functional——FX 正确性继承自原 NORMAL 凭证已为 functional，无须 REVERSAL 路径单独 FX 处理。
- **结论**：FX NP ISSUED/HONORED 路径 dispatcher 已正确将 functional 金额注入 billData，writeOff REVERSAL 路径 FX 正确性继承自原 NORMAL，全部无须 Provider/Processor 改造。

### 浏览器层覆盖状态（iter-1 审查 MINOR-2 修订）

- **单币种覆盖**（1430-1 产物）：`fin-notes-payable.action.spec.ts` 5 测试（issue/honor/dishonor/writeOff/守卫）覆盖：
  - issue NORMAL 凭证行断言（Dr 2202/Cr 2203 CNY）；
  - honor NORMAL 凭证行断言（Dr 2203/Cr 1002 CNY）；
  - writeOff REVERSAL 凭证行断言（Dr -2202/Cr -2203 CNY 同向取负）；
  - dishonor **显式断言无凭证**（`expect(noVoucher).toBeNull()`）；
  - 守卫断言。
- **多币种 ISSUED/HONORED/writeOff REVERSAL 路径**浏览器层**零覆盖**——本计划承接。
- **多币种 dishonor 路径**：单币种已显式断言无凭证，FX 段同样无凭证（对齐 `doDishonor:141-144` 仅 setStatus 无 posting 范式）；本计划纳入 dishonor 守卫断言供 spec 行为对称（iter-1 审查 MINOR-4 修订：dishonor 无 posting → 无 FX 语义，FX 段覆盖仅为 spec 行为对称非 FX 特定验证）。

### 种子基线（无须加性追加）

- `erp_md_currency.csv:2-3` 已含 `1=CNY / 2=USD`。
- `erp_md_subject.csv` 已含 `2202/2203/1002`（1430-1 落地），**本计划无须新科目行**——NP 路径无 6051 行。
- `erp_fin_fund_account.csv` 已含多账户（含 currencyId 维度），外币账户 setup 可经 `__save` 自包含建测试专用账户隔离基线。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave` / `callMutation` / `verifyState`。
- `tests/e2e/orchestration/_helper.ts`：`findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)` 两原语（1430-1 范式）。
- 自包含 setup 范式：`__save` 直置 status 入口（ORM tagSet 无 use-approval/use-workflow）；fresh-DB 测试区间隔离避免种子污染。

### 剩余差距

- FX NP ISSUED/HONORED 浏览器层 E2E 缺失（0120-1 Deferred l.204-208，未 RELEASED）。
- FX NP writeOff REVERSAL 浏览器层覆盖（1430-1 单币种已覆盖，外币段属本计划范围内对称扩展）。
- FX NP dishonor 浏览器层覆盖（spec 行为对称，无 FX 特定语义——iter-1 审查 MINOR-4 已订正）。
- 缺口属「后端齐备 + 浏览器层零覆盖」典型 successor 形态，预期零生产 Java/契约/ORM 模型变更。

## Goals

- 交付 1 个浏览器层 E2E spec（新建 `fin-notes-payable-fx-lifecycle.action.spec.ts` 经 Phase 1 Decision (a) 采纳），经 GraphQL `/graphql` 驱动 DIRECT `@BizMutation`，凭证行翻转经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言：
  1. **FX NP ISSUED 路径**：建 USD `ErpFinNotesPayable`（currencyId=2 + exchangeRate≠1 + amountFunctional 派生）+ `issue` @BizMutation → ISSUED + posted=true + 2 行凭证（Dr 2202 应付账款 functional / Cr 2203 应付票据 functional，无 6051——Provider 无 FX 分支为设计选择）。
  2. **FX NP HONORED 路径**：前置 ISSUED note → `honor` @BizMutation → HONORED + posted=true + 2 行凭证（Dr 2203 / Cr 1002 functional）。
  3. **FX NP writeOff REVERSAL 路径**：前置 ISSUED note → `writeOff` @BizMutation → WRITE_OFF + posted=false + REVERSAL 红字凭证行同向取负（Dr 2202=-functional / Cr 2203=-functional 对原 ISSUED 凭证）+ 原 NORMAL 凭证 isReversed=true。
  4. **FX NP dishonor 路径**：建 USD note → `dishonor` @BizMutation → DISHONORED + 显式断言无凭证（对齐 1430-1 test 3 + `doDishonor:141-144` 仅 setStatus 无 posting 范式）+ 非法迁移守卫断言。
- 对照断言（独立测试用例，iter-1 审查 MAJOR-1 修订）：本 spec 内独立 `test()` 建单币种 note（CNY）+ 同 (1)(2)(3) 动作 → 断言凭证行集合 = FX 路径科目+方向完全一致，唯一变量为金额（CNY vs functional 派生）——证明 Provider 无 FX 分支语义对单/外币一致。
- 在 `docs/testing/e2e-runbook.md` 业务动作表补 1 行 + 套件计数更新；`docs/backlog/README.md` +1 done 行。
- 解除 0120-1 Deferred「应付票据外币 ISSUED/HONORED 多币种路径浏览器层 E2E」（补 `**RELEASED by 2026-07-19-0330-2**` 行）。

## Non-Goals

- **不重新实现 1430-1 的单币种范围**——本计划仅消费侧场景扩展 + 测试层，零生产 Java/契约/ORM 模型变更预期。
- **不引入 NP Provider FX 分支**——Provider 当前按 functional 金额过账无 6051 是设计选择；引入 6051 须先有产品需求（如「外币应付票据开出时认列汇兑损益」）落地，属不同结果面 successor（见 `Deferred But Adjudicated`）。
- **不覆盖外币应收票据路径**（0120-1 + 0330-1 范围）——不同实体（`ErpFinNotesReceivable`）+ 不同 Provider（`NotesReceivableAcctDocProvider` 有 DISCOUNTED FX 分支）+ 不同 owner-doc 段（§ErpFinNotesReceivable）。
- **不实现多账户现金预测分摊**（0120-1 Deferred 但须 ORM ask-first 加 `ErpFinArApItem.fundAccountId` 列，不同结果面）。
- **不做外币应付票据审批工作流（xwf）**——ErpFinNotesPayable tagSet 无 useWorkflow，DIRECT 路径即可。
- **不做坏账/对账/银行对账 FX 路径**——不同 owner doc，不并入。
- **不做 NP Provider JUnit 外币扩展**——NP Provider FX 路径无新分支，既有 1430-1 单币种 JUnit 已覆盖 Provider 逻辑；外币浏览器层 E2E 已能验证 functional 金额过账正确。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的场景扩展 successor；纯消费侧 + 测试维护，零生产契约变更预期）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构 / 运行命令 / 业务动作表）、`docs/design/finance/treasury.md`（§ErpFinNotesPayable l.79-93 + §业财过账 l.146-147）
- Skill Selection Basis: 纯 Playwright 浏览器层测试维护，非 Nop 平台 BizModel/页面开发；`nop-testing` 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空（2246-1/1005-2 裁决先例），依技能实质内容判定 `Skill: none`。Phase 1 Explore 阶段如发现后端不可达需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约/xbiz/Java/Provider；任何生产代码修复须 ask-first 并开 successor。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- webServer JVM args 已含 `erp-fin.notes-discount-rate-default=0.12`（1430-1/0120-1 落地，与 NP 无关），无须新增。
- 无新增端口/环境变量/密钥/外部服务。

## Execution Plan

### Phase 1 — Explore：FX NP 数据流 + spec 结构 + REVERSAL FX 行为裁决

Status: completed
Targets:
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesPayableProcessor.java`（doIssue/doHonor/doDishonor/doWriteOff 全方法 + 数据流，iter-1 审查 MAJOR-2 已预核实：doDishonor:141-144 无 posting / doWriteOff:146-154 if posted 调 reversePayable）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/NotesPostingDispatcher.java`（buildPayableEvent FX 字段透传链 + reversePayable REVERSAL 路径，iter-1 审查 MINOR-3 已预核实：reversePayable 按 billHeadCode + businessType 反查原 NORMAL 凭证复制行同向取负）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/NotesPayableAcctDocProvider.java`（ISSUED/HONORED 路径无 FX 分支核实）
  - `tests/e2e/business-actions/fin-notes-payable.action.spec.ts`（1430-1 产物——单币种 NP 5 测试范式：issue/honor/dishonor/writeOff/守卫 + 3 凭证行断言 ISSUED/HONORED NORMAL + writeOff REVERSAL）
  - `module-finance/model/app-erp-finance.orm.xml`（ErpFinNotesPayable 实体字段 currencyId/exchangeRate/amountSource/amountFunctional 核实，iter-1 审查 MINOR-5 已预核实：l.1374-1377 含四字段，scale=4）
Skill: `nop-debugging`

- Item Types: `Decision | Proof`
- Prereqs: none

- [x] `Proof`：FX NP 数据流核实——逐行确认 (1) `ErpFinNotesPayable` 含 `currencyId/exchangeRate/amountSource/amountFunctional` 多币种四件套字段（已预核实 `orm.xml:1374-1377`）；(2) `NotesPostingDispatcher.buildPayableEvent` FX 字段透传 + functional 金额注入 billData（已预核实 `:117`）；(3) `NotesPayableAcctDocProvider.createFacts` ISSUED/HONORED 路径仅消费 `BILL_DATA_FACE_AMOUNT`（按 functional 派生）无 FX 分支。
  - Skill: `nop-debugging`
  - **落地核实（2026-07-19 执行期）**：`orm.xml:1374-1377` 经实时仓库核实含 currencyId(Long) + exchangeRate(precision=20 scale=8) + amountSource(precision=20 scale=4) + amountFunctional(precision=20 scale=4)，tagSet="gid,erp.finance"（无 use-approval/use-workflow，DIRECT 路径可达）。`NotesPostingDispatcher.java:107,117` 核实 currencyId/exchangeRate 透传至 PostingEvent，`:117` `billData.put(BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()))` 注入 functional 金额。`NotesPayableAcctDocProvider.java:36-40,45-58` 核实 EnumSet 仅 NOTES_PAYABLE_ISSUED/HONORED 两业务类型 + createFacts 仅消费 face amount + ISSUED 路径（Dr 2202 应付账款 partnerId + Cr 2203 应付票据）/ HONORED 路径（Dr 2203 应付票据 / Cr 1002 银行存款）均按 functional face 派生，无 6051 FX 分支（设计选择非缺陷）。
- [x] `Proof`：dishonor + writeOff REVERSAL FX 行为核实——(1) `doDishonor:141-144` 仅 setStatus 无 postingDispatcher 调用 → dishonor 不产凭证（iter-1 审查 MAJOR-2 已预核实，本 Proof 复核落地）；(2) `doWriteOff:146-154` if posted=true 调 reversePayable → REVERSAL 红字凭证同向取负（iter-1 审查 MINOR-3 已预核实）；(3) FX 场景下 REVERSAL 凭证行金额继承自原 NORMAL（functional）无须 REVERSAL 路径单独 FX 处理。
  - Skill: `nop-debugging`
  - **落地核实（2026-07-19 执行期）**：`ErpFinNotesPayableProcessor.java:141-144` doDishonor 仅 `setStatus(NOTES_PAY_DISHONORED) + noteDao().updateEntity(note)` 零 postingDispatcher 调用 → dishonor 永不产凭证（FX 段亦然，无 FX 特定语义）。`doWriteOff:146-155` 核实 if Boolean.TRUE.equals(note.getPosted()) 调 `postingDispatcher.reversePayable(note, NOTES_PAYABLE_ISSUED)`，随后 setStatus(WRITE_OFF)+setPosted(false)+清 postedAt/postedBy。`NotesPostingDispatcher.reversePayable:53-55` → `executor.reverse(note.getCode(), businessType)`，平台按 billHeadCode + businessType 反查原 NORMAL 凭证（行金额已 functional），复制凭证行同向取负生成 REVERSAL 凭证 → FX 正确性继承自原 NORMAL 已 functional，无须 REVERSAL 路径单独 FX 处理。
- [x] `Decision`：spec 结构裁决——三选一：
  - **(a) 新建独立 spec** `fin-notes-payable-fx-lifecycle.action.spec.ts`（与 1430-1 单币种 spec 解耦，命名清晰；推荐）；
  - (b) 并入 1430-1 spec 经 `test.describe` 分组（共享 setup helper，但 spec 文件过大）；
  - (c) 参数化用例 单/外币共享 spec（混合对比强但失败定位差）。
  - **采纳 (a)**：独立 spec，命名表达「FX 状态机生命周期」覆盖范围；setup helper 可复用 1430-1 模式（自包含建 USD note + BANK fundAccount）。
  - Skill: none
- [x] `Decision`：FX 数值表裁决——选定确定性 USD/CNY 汇率 + amountSource/functional 派生 + face amount functional 派生路径。建议汇率 `6.6667`（对齐 0120-1 范式）+ amountSource USD 1000 + amountFunctional CNY 6666.7000（HALF_UP scale 4 对齐 `orm.xml:1376-1377` precision=20 scale=4）→ NP face amount = functional 6666.7000（dispatcher 透传路径已预核实 `:117`）。凭证行断言：ISSUED Dr 2202=6666.7000 / Cr 2203=6666.7000；HONORED Dr 2203=6666.7000 / Cr 1002=6666.7000；writeOff REVERSAL Dr 2202=-6666.7000 / Cr 2203=-6666.7000。
  - Skill: none

Exit Criteria:

- [x] 两 Proof + 两 Decision 落记录（含替代方案 + 残留风险 + 行号引用）
- [x] FX NP 数据流后端齐备性确认（Provider/Processor/Dispatcher 透传链无缺口）
- [x] dishonor + writeOff REVERSAL FX 行为明确 + spec 断言策略裁决
- [x] spec 结构 + 数值表裁决

---

### Phase 2 — FX NP 浏览器层 E2E spec 落地（含单币种对照）+ 回归

Status: completed
Targets:
  - `tests/e2e/business-actions/fin-notes-payable-fx-lifecycle.action.spec.ts`（新建——Phase 1 Decision (a) 采纳）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：FX NP lifecycle 新 spec——`fin-notes-payable-fx-lifecycle.action.spec.ts` 5 用例：
  - **(1) FX ISSUED 路径**：自包含建 USD `ErpFinNotesPayable`（currencyId=2 + exchangeRate=6.6667 + amountSource=USD 1000 + amountFunctional=CNY 6666.7000 + partnerId 非空）+ `issue` @BizMutation → ISSUED + posted=true + 2 行凭证（Dr 2202 应付账款 functional 6666.7000 / Cr 2203 应付票据 functional 6666.7000，无 6051——Provider 无 FX 分支）；
  - **(2) FX HONORED 路径**：前置 ISSUED note → `honor` @BizMutation → HONORED + posted=true + 2 行凭证（Dr 2203 / Cr 1002 functional 6666.7000）；
  - **(3) FX writeOff REVERSAL 路径**：前置 ISSUED note → `writeOff` @BizMutation → WRITE_OFF + posted=false + REVERSAL 红字凭证行同向取负（Dr 2202=-6666.7000 / Cr 2203=-6666.7000 对原 ISSUED NORMAL 凭证）+ 原 NORMAL 凭证 isReversed=true；
  - **(4) FX dishonor 路径**（无 FX 特定语义，spec 行为对称纳入）：建 USD note → `dishonor` @BizMutation → DISHONORED + 显式断言无凭证（对齐 1430-1 test 3 + Phase 1 Proof 已核实 doDishonor 无 posting）+ 非法迁移守卫断言（如已 ISSUED 调 dishonor 抛守卫等）；
  - **(5) 单币种对照测试用例**（iter-1 审查 MAJOR-1 修订）：建 CNY note（currencyId=1 + exchangeRate=1 + amountSource=amountFunctional=1000.0000）→ 同 (1)(2)(3) 动作 → 断言凭证行集合 = FX 路径科目+方向完全一致，唯一变量为金额（1000.0000 vs 6666.7000）——证明 Provider 无 FX 分支语义对单/外币一致。
  - 全部凭证行翻转经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言。
  - Skill: none
  - **落地核实（2026-07-19 执行期）**：`tests/e2e/business-actions/fin-notes-payable-fx-lifecycle.action.spec.ts` 已落地 327 行，5 用例全部断言经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立原语（非 mutation 返回值），与 0330-1 同型结构。其中用例 (4) FX dishonor 守卫断言经 `callMutation`（不抛 errors 时 toBeTruthy 满足）+ `verifyState` 复核状态不变。用例 (5) 单币种对照三动作（issue/honor/writeOff）凭证行集合经 `assertVoucherLines` 断言与 FX 路径科目+方向完全一致。
- [x] `Proof`：新增 spec 全绿（`--workers=1`）+ finance 抽样回归（fin-notes-payable + fin-notes-receivable + fin-notes-receivable-fx-discount + finance-voucher-post 共 ≥4 spec ≥20 用例）+ business-actions 全套件回归（0 新增失败）。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-notes-payable-fx-lifecycle.action.spec.ts --workers=1`（新 spec 全绿）+ finance 抽样回归 + business-actions 全套件
  - Skill: none
  - **落地核实（2026-07-19 执行期）**：
    - 新 spec：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-notes-payable-fx-lifecycle.action.spec.ts --workers=1` → **5 passed (46.6s)**。
    - finance 抽样回归：`fin-notes-payable + fin-notes-receivable + fin-notes-receivable-fx-discount + fin-notes-receivable-fx-lifecycle + finance-voucher-post`（5 spec ≥20 用例）→ **20 passed (2.4m)** 0 新增失败。
    - business-actions 全套件：**232 passed / 1 failed**（27.7m）——唯一失败为 `mfg-variance-recompute-reversal` 预存 flake（DB 共享状态累积致 `reportCompletion` docStatus 未翻 COMPLETED，与 0120-1 closure / 0330-1 closure 同型 flake）；经 `git stash --include-untracked` 验证 **without my changes 同样失败**，与本计划无关。
    - `git diff module-finance/erp-fin-service/src/main/java/` 输出空（零生产 Java 变更）。
    - `mvn clean install -DskipTests` → **154 模块 BUILD SUCCESS（01:38 min）**。

Exit Criteria:

- [x] 新 spec 全绿（含 5 用例），状态/凭证行翻转均经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言（非仅 mutation 返回值）
- [x] 单币种对照测试用例断言凭证行集合 = FX 路径科目+方向完全一致（iter-1 审查 MAJOR-1 闭合）
- [x] finance 既有 spec 0 回归 + business-actions 全套件 0 新增失败（1 预存 flake 经 git stash 验证与本计划无关）

---

### Phase 3 — 文档对齐 + Deferred RELEASED 登记 + 日志

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md`、`docs/logs/2026/07-19.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`e2e-runbook.md` 业务动作表 +1 行（finance FX NP ISSUED/HONORED/writeOff REVERSAL 路径浏览器层 E2E）+ 套件计数更新；`backlog/README.md` +1 done 行。
  - Skill: none
  - **落地核实（2026-07-19 执行期）**：`docs/testing/e2e-runbook.md` 业务动作表新增 finance FX NP lifecycle 行（紧随 NR FX discount 行后）+ 套件计数段新增「plan 2026-07-19-0330-2 新增 1 spec」聚合条目 + 表头「86 业务动作 spec」→「87 业务动作 spec」；`docs/backlog/README.md` +1 done 行（`2026-07-19-0330-2` ✅ done）紧随 0330-1 done 行后。
- [x] `Add`：0120-1 NP 外币 Deferred 段补 `**RELEASED by 2026-07-19-0330-2**` 行 + 实施摘要（FX NP 完整生命周期 E2E 覆盖 + 单币种对照断言 Provider 无 FX 分支语义一致）；`docs/logs/2026/07-19.md` 增聚合条目（spec 数 / 验证状态 / 范围纪律 / Provider FX 设计选择注记 / dishonor 无 posting 注记）。
  - Skill: none
  - **落地核实（2026-07-19 执行期）**：`docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md` l.211 后追加「RELEASED by 2026-07-19-0330-2」段（含实施摘要：5 用例 FX NP ISSUED/HONORED/writeOff REVERSAL/dishonor + 单币种对照 + 范围纪律注记 NP Provider FX 分支 successor）；`docs/logs/2026/07-19.md` 顶部增聚合条目（背景/范围/新 spec 5 用例详述/范围纪律/验证状态）。

Exit Criteria:

- [x] e2e-runbook + backlog README + 0120-1 RELEASED + 日志四点落地一致

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_089136101ffe27vQoxjXc2FIP2`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 Blocker + 2 MAJOR + 6 MINOR。
  - **MAJOR-1**：iter-0 Goal l.50 承诺「独立测试用例」对照断言，但 Phase 2 执行项缺单币种对照 `test()`。
  - **MAJOR-2**：iter-0 Phase 1 `Deferred But Adjudicated` 段 l.191-195 推迟 dishonor 凭证行为「（如有）」属可立即核实项——live `ErpFinNotesPayableProcessor.doDishonor:141-144` 仅 setStatus 无 postingDispatcher 调用 → dishonor 永不产凭证。
  - **MINOR-1**：iter-0 l.42 「可选扩展」违反 anti-slack（forbidden word）。
  - **MINOR-2**：iter-0 l.23 「2 凭证行断言」低估——实际 1430-1 含 3 凭证行断言（ISSUED/HONORED NORMAL + writeOff REVERSAL）+ dishonor 显式断言无凭证。
  - **MINOR-3**：iter-0 Phase 1 Proof 未覆盖 REVERSAL FX 行为，但 Phase 2 item (4) 依赖。
  - **MINOR-4**：iter-0 Phase 1 Decision (b) rationale 不坦诚 dishonor FX 价值为零。
  - **MINOR-5**：iter-0 l.107 数值精度 scale 2 与 ORM scale 4 不符。
  - **MINOR-6**：iter-0 Closure Gate l.174 「确认零生产代码变更」措辞过松，mvn install 不验证此——须 git diff。
- **本 iter-1 修订**：依据 MAJOR-1 Phase 2 新增 item (5) 单币种对照测试用例（建 CNY note + 同动作 + 凭证行集合一致性断言）。依据 MAJOR-2 读 `ErpFinNotesPayableProcessor.doDishonor:141-144` 实证仅 setStatus 无 postingDispatcher → Phase 1 Proof 显式核实 + Phase 2 item (4) 显式断言无凭证对齐 1430-1 test 3。依据 MINOR-1 l.42 重写「外币段范围待 Phase 1 Decision 裁决」非「可选扩展」。依据 MINOR-2 Current Baseline 浏览器层覆盖状态改「3 凭证行断言（ISSUED/HONORED NORMAL + writeOff REVERSAL）+ dishonor 显式断言无凭证」。依据 MINOR-3 Phase 1 Proof 新增 REVERSAL FX 行为核实子项（reversePayable 按 billHeadCode + businessType 反查原 NORMAL 凭证复制行同向取负 + FX 正确性继承自原 NORMAL 已 functional）。依据 MINOR-4 Phase 2 item (4) 注「dishonor 无 posting → 无 FX 语义，FX 段覆盖仅为 spec 行为对称非 FX 特定验证」。依据 MINOR-5 数值表 scale 4 对齐 ORM precision=20 scale=4 + 数值改 6666.7000（4 位精度）。依据 MINOR-6 Closure Gate 改「`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `git diff module-finance/erp-fin-service/src/main/java/` 输出空（确认零生产代码变更）」。R1-R14 + anti-slack 全 PASS after iter-1 修订；待 iter-2 审查通过 flip to active。
- Independent draft review iteration 2: **accept** (`ses_0890b1f85ffejmgudXP34QdiGN`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKER + 0 MAJOR + 0 MINOR。iter-1 全部 8 项 finding（MAJOR-1/2 + MINOR-1~6）经 live 仓库逐项核实 **genuine 修订落地**：
  - **MAJOR-1 fix VERIFIED**：Phase 2 新增 item (5) 单币种对照测试用例 + Phase 2 Exit Criteria l.169 显式闭合 + Goal l.79 引用。
  - **MAJOR-2 fix VERIFIED**：无 "(如有)" 推迟；Phase 1 Proof l.124 显式核实 `doDishonor:141-144` 无 posting；Phase 2 item (4) l.158 显式断言无凭证；live `ErpFinNotesPayableProcessor.java:141-144` 仅 setStatus + updateEntity 零 postingDispatcher 调用。
  - **MINOR-1~6 fix VERIFIED**：无 anti-slack forbidden word / 3 凭证行断言含 writeOff REVERSAL / Phase 1 Proof 含 REVERSAL FX 行为 / dishonor 注「无 FX 特定语义」/ 数值 scale 4 (6666.7000) / Closure Gate 含 `git diff module-finance/erp-fin-service/src/main/java/` 输出空。
  - 16 项 live 仓库核实全部 PASS（NP Provider 2 businessTypes + 无 FX 分支 / Processor doIssue/doHonor 产凭证 + doDishonor 不产 + doWriteOff 反向 / ErpFinNotesPayable 字段 scale=4 + tagSet="gid,erp.finance" / Dispatcher :117 face amount functional / 1430-1 NP spec 5 测试 + 3 凭证行断言 + dishonor 显式无凭证 / treasury.md 引用段存在）。
  - R1-R14 + anti-slack + Deferred rule 全 PASS。共识达成 → `Plan Status: active`。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧场景扩展 + 测试层，预期零生产 Java/契约/ORM 模型变更。结束前运行新增 spec + business-actions 回归 + finance 抽样回归 + 后端构建 + git diff 确认零生产代码变更（iter-1 审查 MINOR-6 修订）。

- [x] 范围内行为完成（FX NP ISSUED/HONORED/writeOff REVERSAL 浏览器层 E2E + dishonor 显式无凭证断言 + 单币种对照测试用例）
- [x] 相关文档对齐（e2e-runbook 业务动作表 +1 行 + 套件计数、backlog README done 行、0120-1 RELEASED 登记、日志聚合条目）
- [x] 已运行验证：新增 spec `--workers=1` 全绿 + business-actions 全套件回归 0 新增失败 + finance 既有 spec 抽样回归 0 失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `git diff module-finance/erp-fin-service/src/main/java/` 输出空（确认零生产代码变更）
- [x] 无范围内项目降级为 deferred/follow-up（NP Provider FX 分支引入属不同结果面 successor，非本计划范围内项目降级——已记 Non-Goals + 触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将结束审计项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项。执行期确认后分类。

### NP Provider FX 分支引入（6051 行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `NotesPayableAcctDocProvider` 当前 ISSUED/HONORED 路径无 FX 分支为设计选择（按 functional 金额过账）。引入 6051 须先有产品需求（如「外币应付票据开出/兑付时认列汇兑损益」）落地——本计划仅实证 Provider 当前行为对单/外币一致，不引入新 FX 分支。
- Successor Required: `yes`（触发条件：外币应付票据 ISSUED/HONORED 路径汇兑损益认列产品需求落地时——须后端 Provider 加 FX 分支 + Java processor 配套派生 + 独立 ask-first 计划）

## Closure

Status Note: 执行完成（2026-07-19，主代理执行 3 Phase 全绿）。finance treasury 外币应付票据完整生命周期浏览器层 E2E 落地：1 新 spec（`fin-notes-payable-fx-lifecycle.action.spec.ts` 5 用例）覆盖 FX ISSUED + FX HONORED + FX writeOff REVERSAL + FX dishonor（无凭证）+ 单币种对照测试用例。所有凭证行翻转经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言（非仅 mutation 返回值）；单币种对照测试用例断言凭证行集合 = FX 路径科目+方向完全一致（iter-1 审查 MAJOR-1 闭合）；dishonor 显式断言无凭证对齐 1430-1 test 3 + Phase 1 Proof 核实 `doDishonor:141-144` 仅 setStatus 无 postingDispatcher 调用。**范围纪律**：NP Provider FX 分支引入 6051 归 Deferred 显式 successor（须先有产品需求落地 + 独立 ask-first 计划，不阻塞本计划结束）；dishonor 路径无 FX 特定语义，spec 行为对称纳入非 FX 特定验证；writeOff REVERSAL FX 正确性继承自原 NORMAL 已 functional 无须 REVERSAL 路径单独 FX 处理。验证全绿：新 spec 5/5（46.6s）+ finance 抽样回归 20 passed（2.4m，fin-notes-payable + fin-notes-receivable + fin-notes-receivable-fx-discount + fin-notes-receivable-fx-lifecycle + finance-voucher-post 共 5 spec 0 新增失败）+ business-actions 全套件 232 passed / 1 pre-existing flake（mfg-variance-recompute-reversal 经 git stash --include-untracked 验证与本次 spec 改动无关，与 0120-1/0330-1 closure 同型 flake，27.7m）+ `git diff module-finance/erp-fin-service/src/main/java/` 输出空（零生产 Java 变更）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:38 min）。结束审计由独立子代理（新会话冷重播无执行者上下文）执行——见下方 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，冷重播无执行者上下文，2026-07-19 执行）
- Audit Method: 冷重播自检——重新读取整个 plan + 对每个 Phase 退出标准、Closure Gates 和 owner doc 同步项逐项 grep/glob/read 核实 live 仓库，无执行者上下文承袭。
- Live Repo Evidence:
  - **新 spec 落地核实**：`ls -la tests/e2e/business-actions/fin-notes-payable-fx-lifecycle.action.spec.ts` → 18192 bytes / 377 lines，5 用例经 `grep -nE "^\s*test\("` 核实命中 line 164（FX ISSUED）/ 194（FX HONORED）/ 226（FX writeOff REVERSAL）/ 264（FX dishonor）/ 307（control 单币种对照）——与 Phase 2 item (1)(2)(3)(4)(5) 一一对应。
  - **独立断言原语使用核实**：`grep -c "verifyState\|findVoucherIdByBillCode\|assertVoucherLines" fin-notes-payable-fx-lifecycle.action.spec.ts` → 22 命中（凭证行翻转经独立断言而非 mutation 返回值，对齐 Phase 2 Exit Criteria）。
  - **零生产 Java 变更核实**：`git diff module-finance/erp-fin-service/src/main/java/ | wc -l` → 0；`git status --short` 显示仅 docs + 新 spec 改动，module-finance/erp-fin-* 无任何 Java/ORM/xbiz/contract 修改。
  - **e2e-runbook 业务动作表核实**：`grep -n "0330-2\|fin-notes-payable-fx-lifecycle" docs/testing/e2e-runbook.md` 命中 l.315（finance FX NP lifecycle 业务动作表行）+ l.428（套件计数 86→87 聚合条目）——Phase 3 item 1 落地。
  - **0120-1 Deferred RELEASED 核实**：`grep -n "RELEASED by 2026-07-19-0330-2" docs/plans/2026-07-19-0120-1-*.md` 命中 l.211 RELEASED 段含完整实施摘要——Phase 3 item 2 落地，successor 关系闭环。
  - **backlog README 核实**：`grep -n "2026-07-19-0330-2" docs/backlog/README.md` 命中 l.106 ✅ done 行（紧随 0330-1 done 行后）——Phase 3 backlog 落地。
  - **日志聚合条目核实**：`grep -n "0330-2\|FX NP\|fx-notes-payable" docs/logs/2026/07-19.md` 命中 l.3 标题 + l.5 背景 + l.6 范围（含 3 Phase 全绿 + 验证状态 + 范围纪律注记）——Phase 3 日志聚合条目落地。
- Five-Point Consistency: Plan Status `completed` ↔ 3 Phase Status 均 `completed` ↔ 各 Phase Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]`（含结束审计项）↔ Closure evidence 非占位符——全一致。
- Anti-Hollow Check: 新 spec 5 用例经独立 `verifyState`/`findVoucherIdByBillCode`/`assertVoucherLines` 原语翻转凭证行（22 处引用），无空函数体 / 无 `return null` 占位 / 无吞异常；spec 实际被 Playwright 套件加载（执行期 5/5 全绿 46.6s）。
- Deferred Honesty: `Deferred But Adjudicated` 段仅含 NP Provider FX 分支引入（out-of-scope improvement + 显式 successor 触发条件），无范围内缺陷隐藏。
- Five-point consistency PASS / Anti-hollow PASS / Deferred honesty PASS / Docs sync PASS / 零生产代码变更经 git diff 实证 PASS。审计结论：**APPROVED，本计划可关闭**。

Follow-up:

- NP Provider FX 分支引入（Deferred But Adjudicated 显式 successor，触发条件：外币应付票据 ISSUED/HONORED 路径汇兑损益认列产品需求落地时）
- 多账户现金预测分摊（0120-1 Deferred，触发条件：`ErpFinCashForecastBizModel.collectPayableNotes` 实现 fundAccountId 派生时；或 `ErpFinArApItem` ORM 加 `fundAccountId` 列被授权时）
