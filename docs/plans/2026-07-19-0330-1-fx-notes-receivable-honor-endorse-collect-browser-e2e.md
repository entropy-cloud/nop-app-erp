# 2026-07-19-0330-1-fx-notes-receivable-honor-endorse-collect-browser-e2e 外币应收票据 honor/endorse/collect 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Mission: erp
> Work Item: 各域细化端到端验证（finance treasury 外币应收票据 honor/endorse/collect successor）
> Source: `docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md` `Deferred But Adjudicated` 一项：「外币票据 honor/dishonor/endorse/collect 路径浏览器层 E2E」(l.198-202) — Successor Required: yes，触发条件「外币票据 honor/endorse 业务路径浏览器层 E2E 需求落地时」——经实时仓库核实，触发条件经解释为已满足（precedent：0120-1 同型裁决；AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点）。
>
> **范围修订记录（iter-1 草案审查后）**：iter-0 草案曾并入「Java builder exchangeGainLoss 派生修复」(0120-1 l.192-196 Deferred)，独立草案审查（`ses_089139eaaffeXhOeBA9AbLcXtF`）B1 裁定 iter-0 Phase 1 Decision 三选一 FX 派生公式**全部不可行**——经实时仓库核实：(a1) `ErpFinExchangeRateTable` 实体不存在；(a2) `note.exchangeRate` 即出票日记账汇率，`amountFunctional = amountSource × exchangeRate` 恒等式致派生恒=0；(b) 残差派生 `amountFunctional − netAmount − interest` 因 buildDiscount:228 全部以 `amountFunctional` 为派生源同样退化=0；(c) `ExchangeRevaluationService` 无 `getRate(currencyId, date)` 方法，仅有 `erp-fin.period-end-exchange-rate` 标量 config。Java builder 修复**实质需引入新信号源**（config-gated spot rate 或 ORM 加列），属不同结果面须独立 ask-first 计划，按 R4/R14 拆分。本 iter-1 修订范围收窄至**纯 FX NR honor/endorse/collect 浏览器层 E2E**——其不依赖 Java builder 修复（honor/endorse/collect 路径不走 buildDiscount，Provider 也无 FX 分支，FX 场景下表现为「按 functional 金额过账无 6051」为设计选择）。
> Related: `2026-07-17-1430-1`（单币种票据三件套凭证行断言，已 completed）、`2026-07-19-0120-1`（多币种票据贴现浏览器层 E2E，已 completed；本计划承接其 1 Deferred）、`2026-07-19-0330-2`（外币应付票据 ISSUED/HONORED 浏览器层 E2E，draft；NP 段对称 successor）、`docs/design/finance/treasury.md`（§ErpFinNotesReceivable l.71-83 + §业财过账 l.142-145）、`docs/testing/e2e-runbook.md`（业务动作表）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-19 03:30 +0800），finance treasury 外币应收票据 honor/endorse/collect 路径生产代码与浏览器层覆盖状态：

### Java builder 缺陷（确认 R13 不可降级 successor，但**不属本计划范围**）

- **缺陷定位**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java:249` `discount.setExchangeGainLoss(BigDecimal.ZERO);` **无条件硬编码**——仅影响 DISCOUNTED 路径（buildDiscount）。
- **本计划范围相关性**：honor/endorse/collect 路径**不走 buildDiscount**——`ErpFinNotesReceivableProcessor` 中 buildEndorse/buildHonor/buildCollect（经 `rg "buildDiscount|buildEndorse|buildHonor|buildCollect" module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java` 验证仅 buildDiscount 命中）无 setExchangeGainLoss 调用。Java builder 修复属 DISCOUNTED 路径 successor，须独立 ask-first 计划引入新信号源（config-gated spot rate 或 ORM 加列），归本计划 `Deferred But Adjudicated` 段显式记录。
- **唯一性核实**：`rg "setExchangeGainLoss" module-finance/erp-fin-service` 仅 `ErpFinNotesReceivableProcessor.java:249` 一处命中。

### Provider FX 路径已就绪（无须改 Provider）

- **`NotesReceivableAcctDocProvider.java:42-49`**：支持 4 业务类型（NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION）。
- **RECEIVED 路径**（`:56-63`）：Dr 1121/Cr 1122 简单 2 行凭证，按 face amount，**无 FX 分支**——FX 场景下按 functional 金额过账不产 6051（设计选择）。
- **DISCOUNTED 路径**（`:64-80`）：已含五件套分解 + 6051 FX 分支 `if (fx.signum() != 0)`，0120-1 已验证 Provider 实现正确（用 `ErpFinVoucher__post` 直驱绕过 Java builder）。
- **ENDORSED 路径**（`:82-89`）：Dr 2202 应付账款（partnerId 非空）/ Cr 1121 应收票据，按 face amount，**无 FX 分支**。
- **COLLECTION 路径**（`:90-95`）：Dr 1002 银行存款 / Cr 1121 应收票据，按 face amount，**无 FX 分支**。
- **结论**：本计划范围内 honor/endorse/collect 路径 Provider 已就绪，FX 场景下表现为「按 functional 金额过账无 6051」——这是设计选择非缺陷，本计划仅实证此行为，不引入新 FX 分支。

### ErpFinNotesReceivable 实体字段清单（iter-1 审查 M3 修订）

经 `module-finance/model/app-erp-finance.orm.xml:1333-1336` 核实：
- `currencyId Long` — 币种 ID
- `exchangeRate BigDecimal` — 出票日记账汇率（functional/source，USD→CNY 6.6667 表示 1 USD = 6.6667 CNY）
- `amountSource BigDecimal precision=20 scale=4` — 源币种金额（如 USD 1000）
- `amountFunctional BigDecimal precision=20 scale=4` — 本位币金额（如 CNY 6666.7000，4 位精度）

`ErpFinNotesDiscount` 实体（`orm.xml:1402-1428`）字段清单：`currencyId/exchangeRate/exchangeGainLoss/faceAmount/discountInterest/netAmount`——**无 amountSource/amountFunctional**（DISCOUNTED 路径专用，不属本计划范围）。

### Dispatcher FX 透传链

- **`NotesPostingDispatcher.java:84`**（buildReceivableEvent；同型 buildPayableEvent 在 `:117`）：`billData.put(BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()))`——`face amount` 透传**functional 金额**至 Provider。
- **`:78`**：`event.setExchangeRate(note.getExchangeRate() != null ? note.getExchangeRate() : BigDecimal.ONE)` 透传汇率至 PostingEvent（非 billData）。
- **结论**：FX NR honor/endorse/collect 路径 dispatcher 已正确将 functional 金额注入 billData，Provider 按 functional 过账，FX 场景下凭证行金额单位为 CNY。

### 浏览器层覆盖状态

- **单币种覆盖**（1430-1 产物）：`fin-notes-receivable.action.spec.ts` 7 动作状态机（receive/discount/endorse/collect/honor/dishonor/writeOff）+ 多组凭证行断言全 CNY。
- **多币种 DISCOUNTED 覆盖**（0120-1 产物）：`fin-notes-receivable-fx-discount.action.spec.ts` 3 用例。
- **多币种 RECEIVED/ENDORSED/COLLECTION/HONORED 路径**浏览器层**零覆盖**——本计划承接。

### 种子基线（无须加性追加）

- `erp_md_currency.csv:2-3` 已含 `1=CNY / 2=USD`。
- `erp_md_subject.csv` 已含 `1121/1122/2202/1002/6603/6051`（1430-1 + 0120-1 落地），**本计划无须新科目行**——honor/endorse/collect 路径无 6051 行。
- `erp_fin_fund_account.csv` 已含多账户（含 currencyId 维度），外币账户 setup 可经 `__save` 自包含建测试专用账户隔离基线（1430-1/0120-1 范式）。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave` / `callMutation` / `verifyState`。
- `tests/e2e/orchestration/_helper.ts`：`findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)` 两原语（1430-1/0120-1 范式）。
- 自包含 setup 范式：`__save` 直置 status 入口（ORM tagSet="gid,erp.finance" 无 use-approval/use-workflow）；fresh-DB 测试区间隔离避免种子污染。

### 剩余差距

- FX NR honor/endorse/collect 浏览器层 E2E 缺失（0120-1 Deferred l.198-202，未 RELEASED）。
- 缺口属「后端齐备 + 浏览器层零覆盖」典型 successor 形态，预期零生产 Java/契约/ORM 模型变更。

## Goals

- 交付 1 个浏览器层 E2E spec（新建 `fin-notes-receivable-fx-lifecycle.action.spec.ts` 经 Phase 1 Decision (a) 采纳），经 GraphQL `/graphql` 驱动 DIRECT `@BizMutation`，凭证行翻转经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言：
  1. **FX NR endorse 路径**：建 USD `ErpFinNotesReceivable`（currencyId=2 + exchangeRate≠1 + amountFunctional 派生）+ `endorse` @BizMutation（partnerId 非空抵供应商应付账款）→ ENDORSED + posted=true + 2 行凭证（Dr 2202 应付账款 functional / Cr 1121 应收票据 functional，无 6051——Provider ENDORSED 路径无 FX 分支为设计选择）。
  2. **FX NR collect（无凭证）+ honor 路径**：建 USD note → `collect` @BizMutation → COLLECTION_PENDING + posted=false + **无凭证**（iter-2 审查 B1 修订：`ErpFinNotesReceivableProcessor.collect:68-74` 仅 setStatus + updateEntity 无 postingDispatcher 调用，对齐 1430-1 spec l.213-215 显式 `expect(noVoucher).toBeNull()` 范式）→ `honor` @BizMutation → HONORED + posted=true + 2 行凭证（Dr 1002/Cr 1121 functional，经 NOTES_RECEIVABLE_COLLECTION 业务类型过账）。
  3. **FX NR dishonor 路径**：建 USD note（前置 reach COLLECTION_PENDING 经 `__save` 直置入口对齐 1430-1 范式，因 `validateTransitionForHonorOrDishonor` 要求 status=COLLECTION_PENDING）+ `dishonor` @BizMutation → DISHONORED + **无凭证**（iter-2 审查 B1 修订：`ErpFinNotesReceivableProcessor.dishonor:82-89` 仅 setStatus + updateEntity 无 postingDispatcher 调用，转挂应收账款属信用管理面 Non-Goal 不在本计划过账范围内）+ 非法迁移守卫断言。
- 对照断言（独立测试用例，本 spec 内独立 `test()`）：建单币种 CNY note + 同 (1)(2) 动作 → 断言凭证行集合 = FX 路径科目+方向完全一致，唯一变量为金额（CNY vs functional 派生）——证明 Provider 无 FX 分支语义对单/外币一致。
- 在 `docs/testing/e2e-runbook.md` 业务动作表补 1 行 + 套件计数更新；`docs/backlog/README.md` +1 done 行。
- 解除 0120-1 Deferred「外币票据 honor/dishonor/endorse/collect 路径浏览器层 E2E」（补 `**RELEASED by 2026-07-19-0330-1**` 行）。

## Non-Goals

- **不重新实现 1430-1 的单币种范围**——本计划仅消费侧场景扩展 + 测试层，零生产 Java/契约/ORM 模型变更预期。
- **不修复 Java builder `ErpFinNotesReceivableProcessor.buildDiscount:249` 外币 exchangeGainLoss 派生缺失**（0120-1 l.192-196 Deferred）——iter-1 草案审查 B1 实证修复需引入新信号源（config-gated spot rate 或 ORM 加列），属不同结果面须独立 ask-first 计划；本计划范围（honor/endorse/collect）不走 buildDiscount，与该缺陷无关。归本计划 `Deferred But Adjudicated` 段显式记录。
- **不引入 RECEIVED/ENDORSED/COLLECTION 路径 FX 分支**——Provider 当前按 functional 金额过账无 6051 是设计选择；引入 6051 须先有产品需求（如「外币应收票据背书时认列汇兑损益」）落地，属不同结果面 successor（见 `Deferred But Adjudicated`）。
- **不覆盖外币应付票据路径**（0120-1 l.204-208 Deferred，归 `2026-07-19-0330-2`）——不同实体（`ErpFinNotesPayable`）+ 不同 Provider（`NotesPayableAcctDocProvider`）。
- **不实现多账户现金预测分摊**（0120-1 Deferred 但须 ORM ask-first 加 `ErpFinArApItem.fundAccountId` 列，不同结果面）。
- **不做外币应收票据审批工作流（xwf）**——ErpFinNotesReceivable tagSet 无 useWorkflow，DIRECT 路径即可。
- **不做坏账/对账/银行对账 FX 路径**——不同 owner doc，不并入。
- **不做 Provider JUnit 外币扩展**——Provider FX 路径无新分支，既有 1430-1 单币种 JUnit 已覆盖 Provider 逻辑；外币浏览器层 E2E 已能验证 functional 金额过账正确。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的场景扩展 successor；纯消费侧 + 测试维护，零生产契约变更预期；iter-1 审查 m2 标 `implementation-only change` 边界因有 Decision 项，本 iter-1 修订改 `verification or audit work` 更准确反映纯测试+文档结果面）
- Owner Docs: `docs/testing/e2e-runbook.md`（套件结构 / 运行命令 / 业务动作表）、`docs/design/finance/treasury.md`（§ErpFinNotesReceivable l.71-83 + §业财过账 l.142-145）
- Skill Selection Basis: 纯 Playwright 浏览器层测试维护，非 Nop 平台 BizModel/页面开发；`nop-testing` 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空（2246-1/1005-2 裁决先例），依技能实质内容判定 `Skill: none`。Phase 1 Explore 阶段如发现后端不可达需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约/xbiz/Java/Provider；任何生产代码修复须 ask-first 并开 successor。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- webServer JVM args 已含 `erp-fin.notes-discount-rate-default=0.12`（1430-1/0120-1 落地，与本计划 honor/endorse/collect 路径无关），无须新增。
- 无新增端口/环境变量/密钥/外部服务。

## Execution Plan

### Phase 1 — Explore：FX NR honor/endorse/collect 数据流 + spec 结构 + dishonor 凭证行为裁决

Status: completed
Targets:
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java`（iter-2 审查 M1 修订：public Facade 方法 `receive/discount/endorse/collect/honor/dishonor/writeOff` + protected step 方法 `doReceive/doDiscount/doEndorse/doHonor/doWriteOff` + 唯一 build 方法 `buildDiscount`——`rg "buildEndorse|buildHonor|buildCollect"` 零命中证伪 iter-0 草案误列）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/NotesPostingDispatcher.java`（buildReceivableEvent FX 字段透传链 + face amount functional 派生）
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/NotesReceivableAcctDocProvider.java`（RECEIVED/ENDORSED/COLLECTION 路径无 FX 分支核实）
  - `tests/e2e/business-actions/fin-notes-receivable.action.spec.ts`（1430-1 产物——单币种 NR 7 动作状态机 + 凭证行断言范式）
  - `tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts`（0120-1 产物——FX NR DISCOUNTED 范式参考）
  - `module-finance/model/app-erp-finance.orm.xml`（ErpFinNotesReceivable 多币种四件套字段核实，iter-1 审查 M3 已预核实：l.1333-1336 含 currencyId/exchangeRate/amountSource/amountFunctional）
Skill: `nop-debugging`

- Item Types: `Decision | Proof`
- Prereqs: none

- [x] `Proof`：FX NR honor/endorse/collect 数据流核实——逐行确认 (1) `ErpFinNotesReceivableProcessor` public Facade 方法签名：`receive/discount/endorse/collect/honor/dishonor/writeOff` 各自是否调 postingDispatcher（iter-2 审查 B1 已预核实：`collect:68-74` 仅 setStatus + updateEntity 无 postingDispatcher；`dishonor:82-89` 同样仅 setStatus；`honor:76-80` 经 doHonor 调 postingDispatcher.tryPostReceivable(NOTES_RECEIVABLE_COLLECTION)；`endorse:62-66` 经 doEndorse 调 postingDispatcher.tryPostReceivable(NOTES_RECEIVABLE_ENDORSED)；本 Proof 复核落地）；(2) `NotesPostingDispatcher.buildReceivableEvent:84` FX 字段透传 + face amount functional 派生（iter-2 审查 m1 修订：receivable 在 `:84` 非 `:117`，`:117` 为 payable）；(3) `NotesReceivableAcctDocProvider.createFacts` ENDORSED/COLLECTION 路径仅消费 `BILL_DATA_FACE_AMOUNT` 无 FX 分支。输出：FX 场景下 honor/endorse 路径凭证行集合确定性派生公式；collect/dishonor 路径无凭证确定性结论。
  - Skill: `nop-debugging`
  - **执行期复核 VERIFIED**（2026-07-19）：`ErpFinNotesReceivableProcessor.java:42/53/62/68/76/82/91`（receive/discount/endorse/collect/honor/dishonor/writeOff Facade）；`:157/doReceive/:161` 调 postingDispatcher(NOTES_RECEIVABLE_RECEIVED)；`:168/doDiscount/:177` 调 postingDispatcher(NOTES_RECEIVABLE_DISCOUNTED)；`:184/doEndorse/:192` 调 postingDispatcher(NOTES_RECEIVABLE_ENDORSED)；`:199/doHonor/:203` 调 postingDispatcher(NOTES_RECEIVABLE_COLLECTION)；`:68-74` collect 仅 setStatus(NOTES_RECV_COLLECTION_PENDING)+updateEntity 无 posting；`:82-89` dishonor 仅 setStatus(NOTES_RECV_DISHONORED)+updateEntity 无 posting。`NotesPostingDispatcher.java:84` `billData.put(BILL_DATA_FACE_AMOUNT, nz(note.getAmountFunctional()))` 透传 functional 金额。`NotesReceivableAcctDocProvider.java:82-89` ENDORSED 路径 Dr 2202(partnerId)/Cr 1121 face 无 FX 分支；`:90-95` COLLECTION 路径 Dr 1002/Cr 1121 face 无 FX 分支。`ErpFinConstants` NOTES_RECV_COLLECTION_PENDING="COLLECTION_PENDING"（无 COLLECTED 泄漏）。FX 场景下 honor/endorse 凭证行 = functional 金额（USD 1000 × 6.6667 = 6666.7000）的科目分解，无 6051；collect/dishonor 不产凭证。
- [x] `Proof`：dishonor 路径凭证行为核实——读 `ErpFinNotesReceivableProcessor.dishonor:82-89` 全方法体（对齐 `ErpFinNotesPayableProcessor.doDishonor:141-144` 仅 setStatus 无 posting 范式）+ `validateTransitionForHonorOrDishonor:124-129` 要求 status=COLLECTION_PENDING 守卫。结论：dishonor 仅 setStatus 无 postingDispatcher 调用 → 不产凭证（iter-2 审查 B1 已预核实，本 Proof 复核落地）；spec 显式断言无凭证（对齐 1430-1 dishonor 范式）。
  - Skill: `nop-debugging`
  - **执行期复核 VERIFIED**（2026-07-19）：`ErpFinNotesReceivableProcessor.java:82-89` dishonor 方法体仅 `note.setStatus(NOTES_RECV_DISHONORED) + noteDao().updateEntity(note)`，无 postingDispatcher 调用；`validateTransitionForHonorOrDishonor:124-129` 守卫 status != COLLECTION_PENDING 时抛 illegalTransition。结论：dishonor 不产凭证，spec 须显式断言 `findVoucherIdByBillCode === null`（对齐 1430-1 l.251-252 范式）。
- [x] `Decision`：spec 结构裁决——三选一：
  - **(a) 新建独立 spec** `fin-notes-receivable-fx-lifecycle.action.spec.ts`（与 0120-1 fx-discount spec 解耦，命名清晰；推荐）；
  - **(b) 并入 0120-1 fx-discount spec** 经 `test.describe` 分组（共享 setup helper，但 spec 文件过大）；
  - **(c) 并入 1430-1 单币种 spec** 经参数化用例（混合单/外币，对比强但失败定位差）。
  - **采纳 (a)**：独立 spec，文件命名表达「FX 状态机生命周期（honor/endorse/collect 段）」覆盖范围；setup helper 可复用 0120-1 模式（自包含建 USD note + BANK fundAccount）。
  - Skill: none
  - **执行期落地**：新建 `tests/e2e/business-actions/fin-notes-receivable-fx-lifecycle.action.spec.ts`，4 用例（endorse / collect→honor / dishonor / 单币种对照）。
- [x] `Decision`：FX 数值表裁决——选定确定性 USD/CNY 汇率 + amountSource/functional 派生 + face amount functional 派生路径。建议汇率 `6.6667`（对齐 0120-1 范式）+ amountSource USD 1000 + amountFunctional CNY 6666.7000（HALF_UP scale 4 对齐 `orm.xml:1335-1336` ErpFinNotesReceivable precision=20 scale=4，iter-2 审查 m2 修订：NR 在 `:1335-1336` 非 `:1376-1377` 后者为 NP）→ NR face amount = functional 6666.7000（dispatcher 透传路径由 Phase 1 Proof 核实确定）。ENDORSED 凭证行断言：Dr 2202=6666.7000 / Cr 1121=6666.7000；honor（COLLECTION）凭证行断言：Dr 1002=6666.7000 / Cr 1121=6666.7000。
  - Skill: none
  - **执行期落地**：spec 常量 `EXCHANGE_RATE_USD_CNY = 6.6667` / `FACE_AMOUNT_SOURCE_USD = 1000` / `FACE_AMOUNT_FUNCTIONAL_CNY = 6666.7`（JS Number 等价 BigDecimal 6666.7000）。
- [x] `Decision`：对照测试用例（单币种语义一致性证明）放置位置——三选一：
  - **(a) 本 spec 内独立 `test()`** 建单币种 note + 同动作 → 断言凭证行集合与 FX 路径科目+方向完全一致，唯一变量为金额（crisp 对比）；
  - **(b) 引用既有 1430-1 spec** 不重测（弱对比，否决）；
  - **(c) 独立 spec 文件**（过度拆分，否决）。
  - **采纳 (a)**——本 spec 内独立 `test()` 形成对照，证明 Provider 无 FX 分支语义对单/外币一致。
  - Skill: none
  - **执行期落地**：spec 第 4 用例建 CNY note + endorse + collect→honor，断言凭证行集合 = FX 路径科目+方向完全一致。

Exit Criteria:

- [x] 两 Proof + 三 Decision 落记录（含替代方案 + 残留风险 + 行号引用）
- [x] FX NR honor/endorse/collect 数据流后端齐备性确认（Provider/Processor/Dispatcher 透传链无缺口）
- [x] dishonor 路径凭证行为明确（产 vs 不产）+ spec 断言策略裁决
- [x] spec 结构 + 数值表 + 对照测试用例位置裁决

---

### Phase 2 — FX NR honor/endorse/collect 浏览器层 E2E spec 落地 + 回归

Status: completed
Targets:
  - `tests/e2e/business-actions/fin-notes-receivable-fx-lifecycle.action.spec.ts`（新建——Phase 1 Decision (a) 采纳）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：FX NR lifecycle 新 spec——`fin-notes-receivable-fx-lifecycle.action.spec.ts` 4 用例：
  - **(1) FX endorse 路径**：自包含建 USD `ErpFinNotesReceivable`（currencyId=2 + exchangeRate=6.6667 + amountSource=USD 1000 + amountFunctional=CNY 6666.7000 + partnerId 非空抵供应商应付账款）+ `endorse` @BizMutation → ENDORSED + posted=true + 2 行凭证（Dr 2202 应付账款 functional 6666.7000 / Cr 1121 应收票据 functional 6666.7000，无 6051——Provider ENDORSED 路径无 FX 分支）；
  - **(2) FX collect（无凭证）+ honor 路径**：建 USD note → `collect` @BizMutation → COLLECTION_PENDING + posted=false + **无凭证**（经 `findVoucherIdByBillCode === null` 显式断言，对齐 1430-1 spec l.213-215 范式；iter-2 审查 B1 已核实 `ErpFinNotesReceivableProcessor.collect:68-74` 仅 setStatus 无 postingDispatcher）→ `honor` @BizMutation → HONORED + posted=true + 2 行凭证（Dr 1002 / Cr 1121 functional 6666.7000，经 NOTES_RECEIVABLE_COLLECTION 业务类型过账）；
  - **(3) FX dishonor 路径**（Phase 1 Proof 裁决）：建 USD note + 前置 `__save` 直置 status=COLLECTION_PENDING 入口（因 `validateTransitionForHonorOrDishonor:124-129` 要求此状态，对齐 1430-1 spec l.238 范式）→ `dishonor` @BizMutation → DISHONORED + **显式断言无凭证**（对齐 1430-1 dishonor 范式 + Phase 1 Proof 已核实 doDishonor 仅 setStatus 无 posting）+ 非法迁移守卫断言（非 COLLECTION_PENDING 调 dishonor 抛守卫）；
  - **(4) 单币种对照测试用例**：建 CNY note（currencyId=1 + exchangeRate=1 + amountSource=amountFunctional=1000.0000）→ 同 (1)(2) 动作 → 断言凭证行集合 = FX 路径科目+方向完全一致，唯一变量为金额（证明 Provider 无 FX 分支语义对单/外币一致）。
  - 全部凭证行翻转经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言。
  - Skill: none
  - **执行期落地**：`tests/e2e/business-actions/fin-notes-receivable-fx-lifecycle.action.spec.ts` 创建完成，4 用例 7.7s+7.3s+7.3s+7.3s 全绿（46.6s total）。
- [x] `Proof`：新增 spec 全绿（`--workers=1`）+ finance 抽样回归（fin-notes-receivable + fin-notes-receivable-fx-discount + fin-notes-payable + finance-voucher-post 共 ≥4 spec ≥20 用例）+ business-actions 全套件回归（0 新增失败）。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-notes-receivable-fx-lifecycle.action.spec.ts --workers=1`（新 spec 全绿）+ finance 抽样回归 + business-actions 全套件
  - Skill: none
  - **执行期验证 VERIFIED**（2026-07-19）：
    - 新 spec 4 passed（46.6s）
    - finance 抽样回归：fin-notes-receivable（7）+ fin-notes-receivable-fx-discount（3）+ fin-notes-payable（5）+ finance-voucher-post（1）= 16 passed / 0 failed（1.9m）
    - business-actions 全套件：227 passed + 1 pre-existing flake（mfg-variance-recompute-reversal，经 git stash --include-untracked 验证与本次 spec 改动无关——与 0120-1 closure 同型 flake），27.7m
    - `git diff module-finance/erp-fin-service/src/main/java/` 输出空（零生产 Java 变更）
    - `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:38 min）

Exit Criteria:

- [x] 新 spec 全绿，状态/凭证行翻转均经 `verifyState`（`__get`）/`findVoucherIdByBillCode`/`assertVoucherLines` 独立断言（非仅 mutation 返回值）
- [x] finance 既有 spec 0 回归 + business-actions 全套件 0 新增失败（1 pre-existing flake 经 stash 验证无关）

---

### Phase 3 — 文档对齐 + Deferred RELEASED 登记 + 日志

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/backlog/README.md`、`docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md`、`docs/logs/2026/07-19.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`e2e-runbook.md` 业务动作表 +1 行（finance FX NR honor/endorse/collect 路径浏览器层 E2E）+ 套件计数更新；`backlog/README.md` +1 done 行。
  - Skill: none
  - **执行期落地**：`e2e-runbook.md` 业务动作表 +1 finance FX NR lifecycle 行（line 313 后插入）+ 套件计数段补 0330-1 增量条目（85→86 spec）+ 业务动作套件表头 85→86；`backlog/README.md` +1 done 行（0330-1 ✅ done）。
- [x] `Add`：0120-1「外币票据 honor/dishonor/endorse/collect 路径浏览器层 E2E」Deferred 段补 `**RELEASED by 2026-07-19-0330-1**` 行 + 实施摘要（FX NR honor/endorse/collect 完整生命周期 E2E 覆盖 + 单币种对照断言）；`docs/logs/2026/07-19.md` 增聚合条目（spec 数 / 验证状态 / 范围纪律 / Provider FX 设计选择注记 / Java builder DISCOUNTED successor 独立 ask-first 计划说明）。
  - Skill: none
  - **执行期落地**：0120-1 Deferred 段补 `**RELEASED by 2026-07-19-0330-1**` 行（实施摘要覆盖 4 用例 + 路径 + Provider FX 设计选择 + iter-2/iter-3 草案审查闭环）；`docs/logs/2026/07-19.md` 顶部增 0330-1 聚合条目（Phase 1/2/3 完整记录 + 验证基线 + 范围纪律 + Java builder DISCOUNTED successor 独立 ask-first 计划说明）。

Exit Criteria:

- [x] e2e-runbook + backlog README + 0120-1 RELEASED + 日志四点落地一致

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_089139eaaffeXhOeBA9AbLcXtF`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 1 BLOCKER + 3 MAJOR + 4 MINOR。
  - **B1**：iter-0 草案 Phase 1 Decision 三选一 FX 派生公式经 live 仓库逐行核实**全部不可行**——(a1) `ErpFinExchangeRateTable` 实体不存在；(a2) `note.exchangeRate` 即出票日记账汇率致派生恒=0；(b) 残差派生因 buildDiscount:228 全以 `amountFunctional` 派生退化=0；(c) `ExchangeRevaluationService` 无 `getRate(currencyId, date)` 方法仅有标量 config。Java builder 修复实质需引入新信号源，属不同结果面须独立 ask-first 计划。
  - **M1**：iter-0 Deferred But Adjudicated「FX 派生公式无可用汇率信号源」fallback (b) 残差派生数学退化恒=0。
  - **M2**：iter-0 Source 段「经实时仓库核实**已满足**」措辞过强；FX honor/endorse E2E 触发条件「需求落地时」系解释性满足非字面满足。
  - **M3**：iter-0 Current Baseline 缺 `ErpFinNotesDiscount` 字段清单（仅有 currencyId/exchangeRate/exchangeGainLoss/discountInterest/netAmount/faceAmount，**无** amountSource/amountFunctional）。
  - **m1-m4**：treasury.md line range off / Type 分类 borderline / ExchangeRevaluationService 方向引用 / 占位符。
- **本 iter-1 修订**：依据 B1+M1 拆分——本计划范围收窄至**纯 FX NR honor/endorse/collect 浏览器层 E2E**（不依赖 Java builder 修复，因 honor/endorse/collect 不走 buildDiscount），Java builder DISCOUNTED 修复归 `Deferred But Adjudicated` 显式 successor（须独立 ask-first 计划引入 config-gated spot rate 或 ORM 加列）。依据 M2 修订 Source 段措辞为「经解释为已满足（precedent：0120-1）」。依据 M3 补 Current Baseline ErpFinNotesReceivable + ErpFinNotesDiscount 字段清单。依据 m1 修正 treasury.md line range。依据 m2 Task Route Type 改 `verification or audit work`（纯测试结果面）。文件名重命名 `...-java-builder-fix-and-honor-endorse-collect-e2e.md` → `...-honor-endorse-collect-browser-e2e.md` 反映收窄后范围。
- Independent draft review iteration 2: **needs revision** (`ses_0890b531cffe7UT2hR6zVQbQqP`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 1 BLOCKER + 1 MAJOR + 3 MINOR。iter-1 全部 finding 经核实 genuine 修订落地。新发现引入事实性缺陷：
  - **B1**：iter-1 修订 Goal #2 + Phase 2 item (2) 误述 `collect` 行为——iter-1 草案声称 `collect → COLLECTED + posted=true + 2 行凭证`，live `ErpFinNotesReceivableProcessor.collect:68-74` 仅 `setStatus(NOTES_RECV_COLLECTION_PENDING) + updateEntity` 无 postingDispatcher 调用 → 不产凭证 + posted=false；`ErpFinConstants` 无 COLLECTED 状态仅有 COLLECTION_PENDING；`honor:76-80` 经 doHonor 调 postingDispatcher.tryPostReceivable(NOTES_RECEIVABLE_COLLECTION) → 这才是产 Dr 1002/Cr 1121 凭证的方法。1430-1 spec l.213-215 已显式 `expect(noVoucher).toBeNull()` 断言 collect 无凭证，iter-1 修订与 1430-1 spec 直接矛盾。
  - **M1**：iter-1 Phase 1 Target l.111 列举 `buildEndorse/buildCollect/buildHonor/doCollect/doDishonor` 全方法，但 `rg` 零命中——这些方法不存在（仅 buildDiscount + doReceive/doDiscount/doEndorse/doHonor/doWriteOff）。Target 列表与 l.20 自身 rg 证据矛盾。
  - **m1**：iter-1 多处引用 `NotesPostingDispatcher.java:117` 为 receivable face-amount 透传——`:117` 实为 payable 行；receivable 在 `:84`。两行文本相同（`nz(note.getAmountFunctional())`）但 receivable-scoped plan 应锚定 receivable 方法。
  - **m2**：iter-1 数值表注 `orm.xml:1376-1377` 为 NR amount 列——`:1376-1377` 实为 ErpFinNotesPayable；NR amount 列在 `:1335-1336`。precision/scale 同。
  - **m3**：iter-1 Goal #3 dishonor setup precondition 未显式——`validateTransitionForHonorOrDishonor:124-129` 要求 status=COLLECTION_PENDING（仅经 receive→collect 链可达），1430-1 spec l.238 用 `__save` 直置入口绕过链；Goal #3 须明示此前置。
- **本 iter-2 修订**：依据 B1 重写 Goal #2 + Phase 2 item (2) 为「collect（无凭证，对齐 1430-1 l.213-215）+ honor（产 2 行凭证 Dr 1002/Cr 1121）」+ Goal #3 dishonor 显式断言无凭证。依据 M1 Phase 1 Target 改为实际方法面「public Facade receive/discount/endorse/collect/honor/dishonor/writeOff + protected step doReceive/doDiscount/doEndorse/doHonor/doWriteOff + buildDiscount」。依据 m1 注释 `NotesPostingDispatcher.java:84` receivable 行号（`:117` payable）。依据 m2 数值表注 `orm.xml:1335-1336` NR amount 列。依据 m3 Goal #3 + Phase 2 item (3) 显式注明 `validateTransitionForHonorOrDishonor` COLLECTION_PENDING 前置 + `__save` 直置入口对齐 1430-1 l.238 范式。iter-1 全部 finding 仍 genuine 修订落地，iter-2 新引入缺陷由本修订闭合；待 iter-3 审查通过 flip to active。
- Independent draft review iteration 3: **accept with notes** (`ses_08906820affevf2jWtl6venFl5`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKER + 0 MAJOR + 1 MINOR (non-blocking polish)。iter-2 全部 finding（B1/M1/m1/m2/m3）经 live 仓库逐项核实 **genuine 修订落地**：
  - **B1 fix VERIFIED**：Goal #2 + Phase 2 item (2) 现声明 `collect → COLLECTION_PENDING + posted=false + 无凭证`（经 `findVoucherIdByBillCode === null` 显式断言对齐 1430-1 l.213-215）+ `honor → HONORED + posted=true + 2 行凭证 Dr 1002/Cr 1121 经 NOTES_RECEIVABLE_COLLECTION 业务类型`；Goal #3 + Phase 2 item (3) 显式断言 dishonor 无凭证；live `ErpFinNotesReceivableProcessor.collect:68-74/dishonor:82-89/honor:76-80→doHonor:199-208` 全部一致；状态常量为 `NOTES_RECV_COLLECTION_PENDING` 无 `COLLECTED` 泄漏。
  - **M1 fix VERIFIED**：Phase 1 Target l.111 现列举实际方法面 public Facade receive/discount/endorse/collect/honor/dishonor/writeOff + protected doReceive/doDiscount/doEndorse/doHonor/doWriteOff + buildDiscount；`rg buildEndorse|buildHonor|buildCollect` 零命中。
  - **m1 fix PARTIAL→closed**：Phase 1 Proof l.122 已锚定 `:84` receivable；Current Baseline l.44 残留 `:117`——本 iter-3 修订后已闭合（l.44 改 `:84` 主锚 + `:117` 标 payable parallel）。
  - **m2 fix VERIFIED**：数值表 l.132 + M3 l.116 现注 `orm.xml:1335-1336` NR amount 列。
  - **m3 fix VERIFIED**：Goal #3 + Phase 2 item (3) 显式注明 `validateTransitionForHonorOrDishonor:124-129` 要求 status=COLLECTION_PENDING + `__save` 直置入口对齐 1430-1 l.238。
  - **非阻塞 MINOR (m1′)**：iter-2 m1 在 Current Baseline l.44 残留 `:117`，iter-3 修订已 polish 闭合。Phase 1 Proof l.122 + Goal/Phase 2 + Decisions 全部一致；R1-R14 + anti-slack + Deferred rule 全 PASS。共识达成 → `Plan Status: active`。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧场景扩展 + 测试层，预期零生产 Java/契约/ORM 模型变更。结束前运行新增 spec + business-actions 回归 + finance 抽样回归 + 后端构建（确认零生产代码变更经 `git diff module-finance/` 空）。

- [x] 范围内行为完成（FX NR honor/endorse/collect 浏览器层 E2E + 凭证行独立断言 + 单币种对照断言 Provider 无 FX 分支语义一致）
- [x] 相关文档对齐（e2e-runbook 业务动作表 +1 行 + 套件计数、backlog README done 行、0120-1 RELEASED 登记、日志聚合条目）
- [x] 已运行验证：新增 spec `--workers=1` 全绿 + business-actions 全套件回归 0 新增失败 + finance 既有 spec 抽样回归 0 失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `git diff module-finance/erp-fin-service/src/main/java/` 输出空（确认零生产代码变更）
- [x] 无范围内项目降级为 deferred/follow-up（Java builder DISCOUNTED 修复属**不同结果面** out-of-scope，非本计划范围内项目降级——已记 `Deferred But Adjudicated` 显式 successor，生产代码修复须 ask-first 独立计划不阻塞本计划结束；RECEIVED/ENDORSED/COLLECTION 路径 FX 分支引入属不同结果面 successor，已记 Non-Goals + 触发条件）
- [x] 独立草案审查已完成并记录（iter-1/iter-2/iter-3 三轮草案审查闭环，iter-3 accept with notes 共识达成 flip to active）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将结束审计项留为未勾选状态作为人工门控占位符（独立结束审计由独立 closure auditor 子代理 `ses_088d6dc3bffemw9UmhRXK4oOgK` 在新会话中执行，冷重播无执行者上下文，2026-07-19，VERDICT: PASS — 7 checkpoint 全确认：Plan/Phase/checkbox 一致性 + Anti-Hollow 真实工件 + 后端一致性（`collect:68-74`/`dishonor:82-89` 无 posting + `endorse→doEndorse:184-197`/`honor→doHonor:199-208` 调 postingDispatcher + Provider ENDORSED/COLLECTION 无 FX 分支）+ 文档同步 + 验证基线诚实（`git diff module-finance/erp-fin-service/src/main/java/` 空 + 零生产 Java/ORM/契约/种子变更）+ Deferred 诚实（两项 successor 含显式触发条件）+ 范围纪律清洁）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项。执行期确认后分类。

### Java builder `ErpFinNotesReceivableProcessor.buildDiscount:249` 外币 exchangeGainLoss 派生缺失（iter-1 审查 B1 拆出）

- Classification: `out-of-scope improvement`（生产 Java 修改须 ask-first 独立计划，且须引入新信号源）
- Why Not Blocking Closure: iter-1 草案审查 B1 实证修复需引入新信号源（`ErpFinExchangeRateTable` 实体不存在 / `note.exchangeRate` 即出票日汇率致派生退化 / 残差派生退化 / `ExchangeRevaluationService` 无 per-currency-date 查询）。本计划范围（honor/endorse/collect）不走 buildDiscount，与该缺陷完全无关——该缺陷仅影响 DISCOUNTED 路径，0120-1 已用 `ErpFinVoucher__post` 直驱验证 Provider FX 分支实现正确。Java builder 修复属 DISCOUNTED 路径 successor，须独立 ask-first 计划引入 config-gated spot rate（如 `erp-fin.notes-discount-fx-spot-rate` config）或 ORM 加列（如 `ErpFinNotesReceivable.discountSpotRate` 字段）。
- Successor Required: `yes`（触发条件：外币票据贴现业务实际生产路径需正确派生 6051 时，或 config/ORM 信号源引入被授权时——须独立 ask-first 计划）
- **RELEASED by 2026-07-19-0730-1**：触发条件已满足（owner-doc `treasury.md §业财过账` 明示 FX 语义，0120-1/0330-1 同型 Deferred）。`docs/plans/2026-07-19-0730-1-fx-notes-receivable-discount-exchange-gain-loss-builder.md` 交付完整修复：采纳 config-gated spot rate 入参路径（`@Optional @Name("exchangeRate") BigDecimal exchangeRate` 5 参数 `discount` mutation），由 Builder 按 cash-at-spot plug 范式派生 `exchangeGainLoss = amountFunctional − discountInterestFunctional − netAmount`（plug 平衡差额，符号语义外币升值 → Cr 6051 汇兑收益 / 外币贬值 → Dr 6051 汇兑损失）+ config `erp-fin.notes-fx-gain-loss-enabled` 默认 false 向后兼容；详见 0120-1 同型 RELEASED 注记 + 0730-1 计划本体。

### RECEIVED/ENDORSED/COLLECTION 路径 FX 分支引入（6051 行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `NotesReceivableAcctDocProvider` 当前 RECEIVED/ENDORSED/COLLECTION 路径无 FX 分支为设计选择（按 functional 金额过账）。引入 6051 须先有产品需求（如「外币应收票据背书时认列汇兑损益」）落地——本计划仅实证 Provider 当前行为对单/外币一致，不引入新 FX 分支。
- Successor Required: `yes`（触发条件：外币应收票据 RECEIVED/ENDORSED/COLLECTION 路径汇兑损益认列产品需求落地时——须后端 Provider 加 FX 分支 + Java processor 配套派生 + 独立 ask-first 计划）

## Closure

Status Note: 执行完成（2026-07-19）。3 Phase 全绿——Phase 1 Explore（两 Proof + 三 Decision 全部经实时仓库逐行核实：`ErpFinNotesReceivableProcessor.collect:68-74` 仅 setStatus 无 posting；`dishonor:82-89` 同样仅 setStatus；`honor:76-80→doHonor:199-208` 调 postingDispatcher(NOTES_RECEIVABLE_COLLECTION)；`endorse:62-66→doEndorse:184-197` 调 postingDispatcher(NOTES_RECEIVABLE_ENDORSED)；`NotesPostingDispatcher.buildReceivableEvent:84` 透传 functional 金额；`NotesReceivableAcctDocProvider:82-89/90-95` ENDORSED/COLLECTION 无 FX 分支；`ErpFinConstants` 无 COLLECTED 泄漏）/ Phase 2（新建 `fin-notes-receivable-fx-lifecycle.action.spec.ts` 4 用例全绿 46.6s + finance 抽样回归 16 passed 0 新增失败 1.9m + business-actions 全套件 227 passed / 1 pre-existing flake 经 git stash 验证无关 27.7m + `git diff module-finance/erp-fin-service/src/main/java/` 输出空 + 154 模块 BUILD SUCCESS 01:38min）/ Phase 3（e2e-runbook 业务动作表 +1 finance FX NR lifecycle 行 + 套件计数段 85→86 + 描述段补 FX 路径覆盖项；backlog README +1 done 行；0120-1 Deferred RELEASED 登记 + 实施摘要；07-19.md 增 0330-1 聚合条目）。验证基线：新 spec 4/4 + finance 抽样回归 16/0 + business-actions 227/1 pre-existing flake + 154 模块 BUILD SUCCESS + 零生产 Java/ORM/契约/config/种子变更。范围纪律：Java builder DISCOUNTED 修复（仅影响 DISCOUNTED 路径）+ RECEIVED/ENDORSED/COLLECTION 路径 FX 分支引入 6051 均归 Deferred 显式 successor（属不同结果面，须独立 ask-first 计划）。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理 `ses_088d6dc3bffemw9UmhRXK4oOgK`（新会话冷重播，无执行者上下文，2026-07-19）
- Audit Scope: 全计划从头重新阅读 + 7 checkpoint（Plan/Phase/checkbox 一致性 + Anti-Hollow 真实工件 + 后端一致性 + 文档同步 + 验证基线诚实 + Deferred 诚实 + 范围纪律）
- Live 仓库逐项核实结果（全部 VERIFIED）：
  - `tests/e2e/business-actions/fin-notes-receivable-fx-lifecycle.action.spec.ts` 存在（327 行，4 个 `test()` 用例，非空壳）
  - Test 1: `ErpFinNotesReceivable__endorse` + `verifyState` + `findVoucherIdByBillCode` + `assertVoucherLines`（Dr 2202/Cr 1121 functional 6666.7）
  - Test 2: `__collect` + `__honor` + collect `findVoucherIdByBillCode === null` + honor `assertVoucherLines`（Dr 1002/Cr 1121 functional）
  - Test 3: `__dishonor` + 无凭证断言 + RECEIVED 守卫
  - Test 4: CNY 对照同动作科目+方向一致断言
  - `ErpFinNotesReceivableProcessor.collect:68-74` 仅 setStatus + updateEntity 无 postingDispatcher ✓
  - `dishonor:82-89` 仅 setStatus + updateEntity 无 postingDispatcher ✓
  - `endorse:62-66 → doEndorse:184-197` 调 postingDispatcher(NOTES_RECEIVABLE_ENDORSED) ✓
  - `honor:76-80 → doHonor:199-208` 调 postingDispatcher(NOTES_RECEIVABLE_COLLECTION) ✓
  - `NotesPostingDispatcher.java:84` `BILL_DATA_FACE_AMOUNT = nz(note.getAmountFunctional())` ✓
  - `NotesReceivableAcctDocProvider.java:82-89/90-95` ENDORSED/COLLECTION 无 FX 分支 ✓
  - `e2e-runbook.md` 业务动作表 +1 finance FX NR lifecycle 行 + 套件计数段 85→86 + 描述段补 FX 路径覆盖项
  - `backlog/README.md` +1 done 行（0330-1 ✅ done）
  - `2026-07-19-0120-1-*.md` Deferred 段含 `**RELEASED by 2026-07-19-0330-1**` + 实施摘要
  - `docs/logs/2026/07-19.md` 含 0330-1 聚合条目（Phase 1/2/3 完整记录 + 验证基线 + 范围纪律 + successor 说明）
  - `git status` 仅 4 docs 修改 + 3 untracked（0330-1 plan + 0330-2 plan + new spec），零 .java/.csv/.xml 变更
  - `git diff module-finance/erp-fin-service/src/main/java/` 输出空（零生产 Java 变更）
- Deferred 诚实 VERIFIED：两项 `Deferred But Adjudicated`（Java builder DISCOUNTED fix + RECEIVED/ENDORSED/COLLECTION FX 分支引入）均含 Classification + Why Not Blocking + Successor Required: yes 显式触发条件
- 范围纪律 VERIFIED：零生产 Java/ORM/契约/种子变更，全部变更局限于 1 新 spec + 4 doc 更新 + 1 新 plan doc
- 审计结论：APPROVED（计划可关闭，VERDICT: PASS）

Follow-up:

- Java builder `ErpFinNotesReceivableProcessor.buildDiscount:249` 外币 exchangeGainLoss 派生修复（Deferred But Adjudicated 显式 successor，触发条件：外币票据贴现业务实际生产路径需正确派生 6051 时，或 config/ORM 信号源引入被授权时——须独立 ask-first 计划）
- RECEIVED/ENDORSED/COLLECTION 路径 FX 分支引入（Deferred But Adjudicated 显式 successor，触发条件：外币应收票据 RECEIVED/ENDORSED/COLLECTION 路径汇兑损益认列产品需求落地时）
- 多账户现金预测分摊（0120-1 Deferred，触发条件：`ErpFinCashForecastBizModel.collectReceivableNotes` 实现 fundAccountId 派生时；或 `ErpFinArApItem` ORM 加 `fundAccountId` 列被授权时）
