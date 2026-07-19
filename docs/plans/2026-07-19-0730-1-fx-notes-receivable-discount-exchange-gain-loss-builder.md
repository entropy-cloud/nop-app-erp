# 2026-07-19-0730-1-fx-notes-receivable-discount-exchange-gain-loss-builder 外币应收票据贴现 exchangeGainLoss 派生缺失修复 + E2E 真实 mutation 路径迁移

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Source: `docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md` `Deferred But Adjudicated` 一项：「`ErpFinNotesReceivableProcessor.buildDiscount` 外币 exchangeGainLoss 派生缺失（执行期发现 Java builder 缺陷）」 + `docs/plans/2026-07-19-0330-1-fx-notes-receivable-honor-endorse-collect-browser-e2e.md` 同型 Deferred 项 — Successor Required: yes，触发条件「外币票据业务实际生产路径触发 6051 派生缺失被复现时，或票据业务多币种深化需求落地时」经实时仓库核实**已满足**：0120-1 Phase 1 冷核实已实证 `ErpFinNotesReceivableProcessor.java:249` `discount.setExchangeGainLoss(BigDecimal.ZERO)` **无条件硬编码**，致 `discount` @BizMutation 永远不会触发 6051 凭证行；该缺陷使 owner doc `docs/design/finance/treasury.md §业财过账 l.143/162` 明示的「NOTES_RECEIVABLE_DISCOUNTED [借/贷] 汇兑损益(外币)」语义在生产路径不可达，属规则 13 不可降级的「owner-doc 漂移」。
> Related: `2026-07-17-1430-1`（单币种票据三件套凭证行断言，已 completed；本计划 DISCOUNTED 单币种路径不动）、`2026-07-19-0120-1`（多币种票据贴现浏览器层 E2E + 本 Java builder 缺陷 successor 登记，已 completed；本计划承接其 Deferred）、`2026-07-19-0330-1`（外币应收票据 honor/endorse/collect 浏览器层 E2E，已 completed；同型 successor 登记无 6051 路径不在本计划范围）、`2026-07-19-0330-2`（外币应付票据 Issued/Honored 浏览器层 E2E，已 completed；NP Provider FX 分支引入归独立 successor）、`docs/design/finance/treasury.md`（§业财过账 l.140-164 + §关键业务规则 l.184-195）、`docs/design/finance/posting.md`（IErpFinAcctDocProvider 过账机制）、`docs/testing/e2e-runbook.md`（业务动作表）
> Mission: erp
> Work Item: 各域细化端到端验证（finance treasury 多币种贴现后端 Java builder 缺陷修复 + 真实 mutation 路径 E2E 迁移）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-19 07:21 +08:00）：

### 已落地（不动）

- **Provider FX 6051 分支已正确实现** — `NotesReceivableAcctDocProvider.java:64-80` DISCOUNTED 路径已读取 `BILL_DATA_EXCHANGE_GAIN_LOSS` 并按 signum 分支：fx>0 → `Dr 6051=fx`；fx<0 → `Cr 6051=-fx`；fx==0 → 抑制 6051 行（3 行凭证）。0120-1 Phase 1 已实证经 `ErpFinVoucher__post` 直驱 `billData.EXCHANGE_GAIN_LOSS=5.00` 路径产 4 行凭证 Dr 6051=5.00。
- **Dispatcher 透传链完整** — `NotesPostingDispatcher.java:84` `billData.FACE_AMOUNT = nz(note.getAmountFunctional())`（凭证行金额全部 functional）+ `:90-92` DISCOUNTED 分支透传 `discountInterest`/`netAmount`/`exchangeGainLoss` 至 billData。
- **0120-1 浏览器层覆盖已交付** — `tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts` 3 测试：(1) FX 状态机生命周期经 `discount` mutation（3 行凭证无 6051）+ (2) FX 6051 触发分支经 `ErpFinVoucher__post` 直驱（4 行凭证 Dr 6051=5.00）+ (3) 单币种 6051 抑制对照（3 行凭证）。
- **单币种 DISCOUNTED 凭证行断言** — `fin-notes-receivable.action.spec.ts`（1430-1 产物）已覆盖 CNY note discount 路径 3 行凭证断言。

### 缺陷定位（本计划对象）

- **`ErpFinNotesReceivableProcessor.java:249`** — `buildDiscount(...)` 方法**无条件硬编码** `discount.setExchangeGainLoss(BigDecimal.ZERO)`，导致：
  1. 外币票据经 `discount` @BizMutation 时，`exchangeGainLoss` 永远为 0；
  2. Dispatcher 透传 `BILL_DATA_EXCHANGE_GAIN_LOSS=0` 至 Provider；
  3. Provider `:72` `fx.signum()==0` 抑制 6051 行；
  4. 外币贴现产生的已实现汇兑损益**在生产路径不可达**。
- **`IErpFinNotesReceivableBiz.discount` 签名缺汇率入参** — `IErpFinNotesReceivableBiz.java:26-31` 现签名为 `@BizMutation discount(notesId, discountDate, bankId, discountRate, context)`（注解 + 5 行参数），无贴现日即期汇率入参，Builder 无法派生 `exchangeGainLoss`。

### owner-doc 期望行为（已落地，无须修订）

- `docs/design/finance/treasury.md` **l.143** 表格明示 `NOTES_RECEIVABLE_DISCOUNTED | 票据贴现 | 借：银行存款(实得) / 借：财务费用-利息支出(贴现息) / [借/贷] 汇兑损益(外币) / 贷：应收票据(票面)`。
- **l.157-164** 「贴现凭证科目分解（借 Metasfresh `Doc_BankStatement.java:206-547` 五科目分解范式）」明示 4 行凭证含 `借/贷：汇兑损益（外币贴现时，exchangeGainLoss）`。
- **l.214** 证据强度表「外币贴现汇兑损益 | 🟢 | Metasfresh `Doc_BankStatement.java:482-547` 源码实测」。
- 结论：owner doc 已明示 FX gain/loss 为 DISCOUNTED 路径的预期行为；当前 Java 实现与之**漂移**（规则 13 不可降级项）。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave` / `callMutation` / `verifyState`（0120-1 已用于 FX discount）。
- `tests/e2e/orchestration/_helper.ts`：`findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)` 两原语（1430-1 范式）。
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinNotesReceivableStateMachine.java`：`testDiscountComputesInterestAndNetAmount` 现有 discount 单测范式（驱动 `notesBiz.discount(noteId, discountDate, bankId, rate, ctx)`）。

### 剩余差距

1. `ErpFinNotesReceivableProcessor.buildDiscount` 硬编码 `exchangeGainLoss=0` —— 阻塞 FX gain/loss 在生产路径产生。
2. `IErpFinNotesReceivableBiz.discount` 签名缺贴现日即期汇率入参 —— Builder 无法派生非零 `exchangeGainLoss`。
3. 0120-1 spec 测试 (2) 因缺陷不得不以 `ErpFinVoucher__post` 直驱 workaround 验证 Provider 6051 分支 —— 缺陷修复后须迁移至真实 `discount` mutation 路径，否则 workaround 永久化掩盖缺陷修复正确性。

## Goals

- **修复 Java builder 缺陷**：`ErpFinNotesReceivableProcessor.buildDiscount` 派生外币贴现 `exchangeGainLoss`，使其在 owner-doc 明示的「[借/贷] 汇兑损益(外币)」语义下可达。
- **扩展 `discount` 契约**：`IErpFinNotesReceivableBiz.discount` 增可选贴现日即期汇率入参；保留旧签名向后兼容（委派新签名 + null 汇率）。
- **迁移 0120-1 spec workaround 至真实 mutation 路径**：测试 (2)「FX 6051 触发分支」由 `ErpFinVoucher__post` 直驱改为真实 `discount(exchangeRate=spotRate)` mutation，验证 Builder → Dispatcher → Provider 全链 FX 6051 产生。
- **JUnit 回归**：覆盖新签名正路径（FX note + 汇率入参 → exchangeGainLoss 非零 + 6051 凭证行）+ 旧签名向后兼容（FX note 无汇率入参 → exchangeGainLoss=0 + 3 行凭证无 6051，对齐当前行为）+ 单币种抑制（CNY note 即使传汇率入参也 exchangeGainLoss=0）。
- **owner-doc 实现注记**：`treasury.md §业财过账` 补「贴现 exchangeGainLoss 派生公式」实现注记段，闭合 owner-doc 漂移。

## Non-Goals

- **不引入 NR RECEIVED/ENDORSED/COLLECTION 路径 FX 分支（6051 行）**——0330-1 Deferred 显式 successor，owner-doc l.143 仅 DISCOUNTED 明示 FX 语义；其余路径 FX 重估属期末结账 `ExchangeRevaluationService` 范围，不属本计划。
- **不引入 NP Provider FX 分支**——0330-2 Deferred 显式 successor；NP ISSUED/HONORED 路径 owner-doc 未明示 FX 语义，属不同结果面。
- **不实现多账户现金预测分摊**——0120-1 Deferred 须 ORM ask-first 加 `ErpFinArApItem.fundAccountId` 列，不同结果面。
- **不引入汇率源数据表（CurrencyRateTable）**——本期汇率由 `discount` mutation 显式入参传入，运营层汇率源（央行汇率/银行挂牌汇率）属配置层 successor。
- **不做外币重估公式精细化裁决**（按到期日 vs 贴现日 vs 票面日汇率）——本期采用「贴现日即期汇率 vs 票面原汇率」最简化语义，符合 Metasfresh `Doc_BankStatement.java:482-547` 范式；多汇率选择规则属 successor。
- **不做凭证化外的字段物化**——`ErpFinNotesDiscount` 现有 `exchangeGainLoss` 列（propId=11，precision=20 scale=4）已承载，无须 ORM 加性变更。
- **不做 `discount` 审批工作流（xwf）**——`ErpFinNotesReceivable` tagSet 无 useWorkflow，DIRECT 路径即可（与 1430-1 一致）。

## Task Route

- Type: `implementation-only change`（缺陷定位已由 0120-1/0330-1 完成并 RELEASED 登记；本计划 Phase 1 Proof 1 仅是 cold-replay 复核 + 决策落地，主体为应用层 Java + 测试 + owner-doc 注记实施）
- Owner Docs: `docs/design/finance/treasury.md`（§业财过账 l.140-164 + §关键业务规则 l.184-195）、`docs/design/finance/posting.md`（IErpFinAcctDocProvider 过账机制）
- Skill Selection Basis: `nop-backend-dev`（应用层 Java BizModel/Processor/IBiz 契约扩展 + JUnit）+ `nop-testing`（Playwright E2E spec 迁移）；后端 protected area = accounting/finance postings → plan-first，本计划即 plan-first 切片。

## Infrastructure And Config Prereqs

- webServer JVM args 须新增 `-Derp-fin.notes-fx-gain-loss-enabled=true`（默认 false 向后兼容，关闭时 Builder 沿用 ZERO 行为，开启时按本计划公式派生）。
- 种子 `erp_md_subject.csv` 6051 汇兑损益行已由 0120-1 落地（id=44, EXPENSE/DEBIT），无须加性追加。
- 无外部服务/密钥/CORS 依赖。

## Execution Plan

### Phase 1 — Decision：贴现 exchangeGainLoss 派生公式 + 契约扩展形状

Status: completed
Targets: `docs/design/finance/treasury.md`（实现注记段）、`docs/plans/2026-07-19-0730-1-...md`（决策落地）
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`

Prereqs: 无

- [x] **Decision 1：`discount` 契约扩展形状**
  - 选择：在 `IErpFinNotesReceivableBiz` 增**可选**新参数 `@Name("exchangeRate") BigDecimal exchangeRate`（贴现日即期汇率），保留现有 4 参数签名为**默认委派**（`exchangeRate=null` 走原 ZERO 路径）。Java 接口以重载方法实现（旧签名委派新签名 `exchangeRate=null`）；GraphQL 层 Nop 自动将可选参数暴露为 `Input` 类型，调用方可省略。
  - 替代方案考虑与否决：
    - (b) 修改原签名为必填参数：破坏既有 1430-1 单币种 spec + 0120-1 spec test (1) + TestErpFinNotesReceivableStateMachine 既有 6 处调用，**否决**。
    - (c) 用 `Map<String, Object> options` 包装扩展参数：损失类型安全 + 与既有 Nop IBiz 风格不一致（既有 discount 已用扁平 `@Name` 参数），**否决**。
  - 残留风险：Nop GraphQL schema 增量由 Java `@Name` 重载方法自动派生为 `ErpFinNotesReceivable__discount` 入参表多一可选 `exchangeRate: Input<BigDecimal>` 字段（运行期生成，无须手工编辑 `*.xbiz.xml`）；下游 consumer 透明（参数可选不影响既有调用）。
  - Skill: `nop-backend-dev`

- [x] **Decision 2：exchangeGainLoss 派生公式（cash-at-spot plug 范式）**
  - 选择：**cash-at-spot plug 范式**——`Dr 1002`（银行存款）行按贴现日即期汇率折算实际收到的现金；`Dr 6603`（贴现息）保持既有 book-rate 口径；`Cr 1121`（应收票据）保持 book-rate 票面口径；`exchangeGainLoss` 作为 **plug** 吸收复式平衡差额：
    - `discountInterestFunctional = note.amountFunctional × discountRate × remainingDays / 360`（既有 functional 口径公式不动，HALF_UP scale=2，作为 `Dr 6603` 行金额）；
    - `discountInterestSource = note.amountSource × discountRate × remainingDays / 360`（source 口径中间量，用于派生 netAmountSource）；
    - `netAmountSource = note.amountSource − discountInterestSource`；
    - `discount.netAmount = netAmountSource × exchangeRate`（**贴现日即期汇率折算 cash-at-spot**，HALF_UP scale=4，作为 `Dr 1002` 行金额）；
    - `exchangeGainLoss = note.amountFunctional − discountInterestFunctional − discount.netAmount`（**plug 平衡差额**，HALF_UP scale=4，作为 `Dr/Cr 6051` 行金额）；
    - `exchangeRate` 取自入参（贴现日即期汇率 functional/source）；未传入或 note 单币种或 config 关闭时 → exchangeRate=note.exchangeRate（兜底）→ netAmount = 既有 functional 口径 + exchangeGainLoss=0（向后兼容）；
    - **符号语义（经会计正确性推导）**：`exchangeRate > note.exchangeRate`（外币升值）→ `discount.netAmount > faceFunctional − discountInterestFunctional` → `exchangeGainLoss < 0` → `Cr 6051`（**汇兑收益**：外币资产实现值 > 账面，settling 产生 gain，记贷方）；`exchangeRate < note.exchangeRate`（外币贬值）→ `exchangeGainLoss > 0` → `Dr 6051`（**汇兑损失**：账面 > 实现值，loss 记借方）。对齐 Provider `:72-78` signum 分支（fx>0 Dr 6051；fx<0 Cr 6051=fx.negate()）。
    - **数值示例（决策依赖）**：note.amountSource=USD 100 / note.exchangeRate=6.6667 / amountFunctional=CNY 666.67 / discountRate=0.12 / remainingDays=30 / spotRate=6.7000（外币升值场景）：
      - discountInterestFunctional = 666.67 × 0.12 × 30 / 360 = 6.67（Dr 6603）
      - discountInterestSource = 100 × 0.12 × 30 / 360 = 1.00 USD
      - netAmountSource = 100 − 1 = 99 USD
      - discount.netAmount = 99 × 6.7000 = 663.3000（Dr 1002）
      - exchangeGainLoss = 666.67 − 6.67 − 663.3000 = −3.3000 → Cr 6051=3.3000（汇兑收益）
      - 复式平衡验证：Dr 663.3000 + Dr 6.67 = 669.97 ≡ Cr 666.67 + Cr 3.3000 = 669.97 ✓
  - 替代方案考虑与否决：
    - (b) `exchangeGainLoss = netAmountSource × (exchangeRate − note.exchangeRate)` 不动 `discount.netAmount`（保留 functional 口径）：经独立草案审查 iter-1 BI-1 实测**产生不平衡凭证**——Dr 1002=660 + Dr 6603=6.67 + Dr 6051=3.30 = 669.97 ≠ Cr 1121=666.67（差 3.30），过账引擎将拒绝或 posted=false，**否决**。
    - (c) `exchangeGainLoss = note.amountSource × (exchangeRate − note.exchangeRate)`（按全票面 re-value，不含 discountInterest）：偏离实际现金流（贴现银行按 netAmount 结算而非 faceAmount），**否决**。
    - (d) 引入「票面汇率 vs 贴现日汇率 vs 到期日汇率」三汇率选择：超出 owner-doc 范围，属 successor，**否决**。
  - 残留风险：
    - **凭证复式平衡约束（会计正确性硬约束）**：公式必须保持 `Dr 1002 + Dr 6603 ± Dr/Cr 6051 ≡ Cr 1121`（Provider `NotesReceivableAcctDocProvider.java:64-80` 科目分解范式的硬约束）；Phase 2 Proof 2/3 JUnit **必须断言** `Σ Dr == Σ Cr` 平衡。
    - 「贴现息按 book rate 折算」与「cash-at-spot」混用语义：与 Metasfresh `Doc_BankStatement.java:482-547` 五科目分解范式一致（贴现息在过账日已实质发生按 book rate 锁定；现金按 settlement rate 实现）；如未来要求贴现息也按 spot rate 折算，公式需调整，归 successor。
  - Skill: `nop-backend-dev`

- [x] **Decision 3：config-gate 开关默认值**
  - 选择：`erp-fin.notes-fx-gain-loss-enabled` 默认 `false`（关闭时 Builder 沿用 ZERO 行为，向后兼容）；webServer JVM args 显式启用（对齐 1100-6 MAINTENANCE_ISSUE / 0949-1 MAINTENANCE_LABOR / 0718-1 CREDIT_FACILITY_INTEREST 范式）。
  - 替代方案考虑与否则：
    - (b) 默认 true（无 config-gate）：破坏既有 1430-1 / 0120-1 单币种断言 + TestErpFinNotesReceivableStateMachine 既有 discount 单测（exchangeGainLoss 断言非 0），**否决**。
  - 残留风险：默认关闭意味着运营层未启用前生产路径仍走 ZERO；E2E 启用 config 后验证派生路径；如未来要求默认启用，单独 successor 切换默认值。
  - Skill: `nop-backend-dev`

- [x] **Proof 1：实时仓库核实决策依赖锚点**
  - 核实 `ErpFinNotesReceivable.amountSource/exchangeRate/amountFunctional` 字段在 ORM 与运行时均非空（外币票据 setup 路径）+ `ErpFinNotesDiscount.exchangeRate/exchangeGainLoss` 列已存在（propId=10/11）+ `NotesReceivableAcctDocProvider.createFacts:64-80` 已读取 `BILL_DATA_EXCHANGE_GAIN_LOSS` 并 signum 分支（无须改 Provider）。
  - 实时仓库核实（HEAD 2026-07-19 +08:00）：`_ErpFinNotesDiscount.java:61-66` 确认 `exchangeRate` propId=10、`exchangeGainLoss` propId=11 已落库；`NotesReceivableAcctDocProvider.java:64-80` DISCOUNTED 路径读取 `BILL_DATA_EXCHANGE_GAIN_LOSS` 并 `fx>0 → Dr 6051=fx` / `fx<0 → Cr 6051=-fx` / `fx==0 → 抑制`；`_ErpMdCurrency.java:45-46` `isFunctional` propId=6 已落库；`ExchangeRevaluationService.java:280-287` private `resolveFunctionalCurrencyId` 范式经 `eq("isFunctional", Boolean.TRUE)` 反查（Processor 内联同名 private helper 对齐）。
  - Skill: `none`

Exit Criteria:

- [x] 3 Decision 项均落地（选择/替代方案/残留风险记录在本计划）+ 1 Proof 项实时仓库核实记录。
- [x] owner-doc `docs/design/finance/treasury.md §业财过账` 增「DISCOUNTED exchangeGainLoss 派生公式实现注记」段（≤30 行，含公式 + 符号语义 + config-gate）。

### Phase 2 — Fix：Java Builder/Processor/IBiz/BizModel 契约扩展 + JUnit 回归

Status: completed
Targets:
- `module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinNotesReceivableBiz.java`（IBiz 接口新增重载方法声明）
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinNotesReceivableBizModel.java`（BizModel 新增重载方法 + 旧签名委派）
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java`（`discount` 公共方法 + `buildDiscount` 派生公式）
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinConstants.java`（+1 config 常量；reader 内联 Processor 不引入 ErpFinConfigs 抽象）
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinNotesReceivableStateMachine.java`（+3 测试方法）
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinNotesReceivablePosting.java`（+1 posting 集成测试方法 `testDiscountFxPosts6051VoucherLineAndBalance`，归入既有 posting 测试类——经实时仓库核实该类已存在 5 既有 @Test 含 `testDiscountPostsFiveLineDecomposition`，是 posting 集成测试的自然归属）

Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`

Prereqs: Phase 1 完成（3 Decision 落地）

- [x] **Fix 1：`ErpFinNotesReceivableProcessor.buildDiscount` 派生 `exchangeGainLoss`（cash-at-spot plug 范式）**
  - 改动：`buildDiscount` 方法签名扩展 `BigDecimal exchangeRate`（贴现日即期汇率）入参，方法体按 Decision 2 公式分支：
    - 当 `notesFxGainLossEnabled() && note.currencyId 非本位币 && exchangeRate != null` 时，按 cash-at-spot plug 范式派生：先计算 `discountInterestFunctional`（既有 functional 口径，不动）+ `discountInterestSource` + `netAmountSource` + `discount.netAmount = netAmountSource × exchangeRate`（覆盖原 functional 口径 netAmount）+ `exchangeGainLoss = note.amountFunctional − discountInterestFunctional − discount.netAmount`（plug）；
    - 否则保持原 `discountInterest = faceAmountFunctional × rate × days / 360` + `netAmount = faceAmountFunctional − discountInterest`（functional 口径）+ `exchangeGainLoss = ZERO`；
    - `discount.setExchangeRate(exchangeRate != null ? exchangeRate : note.exchangeRate)`（覆盖原 `setExchangeRate(note.exchangeRate != null ? ... : ONE)`，使 ErpFinNotesDiscount.exchangeRate 列承载实际派生用汇率）；
    - `note.currencyId 非本位币` 判定：经 `AcctSchemaResolver` 公开 helper 不可达（实时仓库核实仅有 `resolvePrimarySchemaId` + `naturePriority`，`resolveFunctionalCurrencyId` 仅作为 private 副本存在于 4 处 service 类如 `ExchangeRevaluationService:280` 查询 `ErpMdCurrency.isFunctional=TRUE`），本计划**在 Processor 内联同名 private helper**（对齐既有 4 处 private 副本范式，不抽取公共 helper 避免范围扩张，归 successor）；备选：`AcctSchemaResolver.resolvePrimarySchemaId + ErpMdAcctSchema.functionalCurrencyId` 两步反查。
  - Skill: `nop-backend-dev`
- [x] **Fix 2：`discount` 公共方法签名扩展**
  - 改动：`ErpFinNotesReceivableProcessor.discount(...)` 公共方法增 `BigDecimal exchangeRate` 末参 + `requireDiscountInputs` 守卫扩展（仅当 config 启用且 note 外币时强制 exchangeRate 非空，否则报 `ERR_NOTES_DISCOUNT_FX_RATE_REQUIRED` 新 ErrorCode；本位币 note 透明忽略 exchangeRate）。
  - 实施偏离（已纠正）：iter-1 守卫逻辑在执行期经实证产生与 Decision 2 公式（`未传入 → exchangeRate=note.exchangeRate 兜底 → ZERO`）冲突——若 config 启用 + FX note + exchangeRate=null 时抛错，则 Proof 2 `testDiscountFxFallbackWhenSpotRateNull`「向后兼容」断言不可达。改为**不抛错**（null 静默走 ZERO 兜底，对齐 Decision 2 公式语义）；`ERR_NOTES_DISCOUNT_FX_RATE_REQUIRED` ErrorCode 保留作为未来 hard-fail 模式 successor 候选（默认不抛）。同时执行期发现 Nop GraphQL BizModel **不允许同名 action 重载**（`方法[FunctionModel:discount(5)]和[FunctionModel:discount(6)]对应的action名称重复`），改为单 5 参数方法（`exchangeRate` 作为可选 GraphQL Input 字段，调用方可省略；旧 4 参数 GraphQL 调用透传 `null`）。
  - Skill: `nop-backend-dev`
- [x] **Add 1：`IErpFinNotesReceivableBiz.discount` 重载声明**
  - 改动：新增 `@BizMutation ErpFinNotesReceivable discount(@Name("notesId") Long, @Name("discountDate") LocalDate, @Name("bankId") Long, @Name("discountRate") BigDecimal, @Name("exchangeRate") BigDecimal, IServiceContext)` 重载；保留原 4 参数签名为 default 方法委派（`return discount(notesId, discountDate, bankId, discountRate, null, context);`）。
  - 实施偏离（已纠正）：Nop 不允许 BizMutation 同名重载，IBiz/BizModel 改为**单 5 参数方法**（GraphQL 层 `exchangeRate` 为可选 Input，省略时透传 null 走兜底路径）。下游既有 4 参数 GraphQL 调用透明（参数可选）。
  - Skill: `nop-backend-dev`
- [x] **Add 2：`ErpFinNotesReceivableBizModel.discount` 重载实现**
  - 改动：BizModel 同样新增 5 参数 `discount` 重载方法委派 Processor；旧 4 参数 `discount` 方法体改为 `return discount(notesId, discountDate, bankId, discountRate, null, context);`。
  - 实施偏离（已纠正）：BizModel 改为单 5 参数方法（对齐 Add 1 偏离纠正）。
  - Skill: `nop-backend-dev`
- [x] **Add 3：`ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED` 常量 + Processor 内联 reader**
  - 改动：`ErpFinConstants` +1 常量 `String CONFIG_NOTES_FX_GAIN_LOSS_ENABLED = "erp-fin.notes-fx-gain-loss-enabled"`；`ErpFinNotesReceivableProcessor` 内置私有 reader 方法 `private boolean notesFxGainLossEnabled() { return AppConfig.var(ErpFinConstants.CONFIG_NOTES_FX_GAIN_LOSS_ENABLED, Boolean.FALSE); }`（直接调用点 reader 对齐既有 11+ 处 config 读取范式：`ExchangeRevaluationService.java:254` `AppConfig.var(ErpFinConstants.CONFIG_BANK_FX_REVALUATION_ENABLED, Boolean.TRUE)` / `ErpFinAccountingPeriodProcessor.java:726`；**不引入 ErpFinConfigs reader 抽象层**，因代码库 `ErpFinConfigs.java` 仅 5 行空接口零 reader 方法，引入新抽象违反既有约定）。
  - Skill: `nop-backend-dev`
- [x] **Add 4：`ErpFinErrors.ERR_NOTES_DISCOUNT_FX_RATE_REQUIRED` ErrorCode**
  - 改动：`ErpFinErrors` +1 ErrorCode（描述：「外币票据贴现启用 FX 派生时贴现日汇率必填」+ `ARG_NOTES_CODE`/`ARG_CURRENCY_ID` 参数），对齐既有 `ERR_NOTES_*` 范式。
  - 实施注：ErrorCode 已落地（保留作未来 hard-fail 模式 successor 候选）；当前 `requireDiscountInputs` 默认不抛错（见 Fix 2 偏离纠正）。
  - Skill: `nop-backend-dev`
- [x] **Proof 2：JUnit 回归（含凭证平衡断言）**
  - 改动：`TestErpFinNotesReceivableStateMachine` 增 3 测试方法：
    - `testDiscountFxWithSpotRateDerivesExchangeGainLossCashAtSpot`：建 USD note（currencyId=2 + exchangeRate=6.6667 + amountSource=USD 100 + amountFunctional=CNY 666.67）+ config 启用 + `discount(exchangeRate=6.7000)`（外币升值）→ 断言 `discount.exchangeGainLoss = -3.3000`（负数 → Cr 6051 汇兑收益，对齐 Decision 2 符号语义）+ `ErpFinNotesDiscount.exchangeRate=6.7000`（覆盖原 note.exchangeRate）+ `ErpFinNotesDiscount.netAmount=663.3000`（cash-at-spot）+ `ErpFinNotesDiscount.discountInterest=6.67`（functional 口径不动）。
    - `testDiscountFxFallbackWhenSpotRateNull`：建 USD note + config 启用 + `discount(exchangeRate=null)`（旧签名委派）→ 断言 `discount.exchangeGainLoss=0` + `ErpFinNotesDiscount.exchangeRate=6.6667`（note.exchangeRate 透传）+ `ErpFinNotesDiscount.netAmount=660.00`（functional 口径兜底）—— 向后兼容。
    - `testDiscountFxSuppressedByConfigGate`：建 USD note + config 关闭 + `discount(exchangeRate=6.7000)` → 断言 `discount.exchangeGainLoss=0`（config 关闭抑制派生）。
    - 既有 `testDiscountComputesInterestAndNetAmount`（CNY note 单币种路径）零回归。
    - **setup 注记**：3 新测试均不直接验证 6051 凭证行（仅断言 `ErpFinNotesDiscount` 实体字段），无须 `seedSubject("6051", "汇兑损益")`（6051 seed 仅 Proof 3 posting 集成测试需要）。
  - 验证命令：`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinNotesReceivableStateMachine`。
  - Skill: `nop-testing`
- [x] **Proof 3：JUnit posting 集成验证 6051 行 + 复式平衡断言**
  - 改动：`TestErpFinNotesReceivablePosting`（`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/`，经实时仓库核实已存在 5 既有 @Test）增 1 新方法 `testDiscountFxPosts6051VoucherLineAndBalance`：建 USD note + config 启用 + `discount(exchangeRate=6.7000)` → 反查 `ErpFinVoucherBillR` by `billHeadCode=note.code` + `businessType=NOTES_RECEIVABLE_DISCOUNTED` → 断言凭证行 4 行：
    - `Dr 1002 = 663.3000`（netAmount cash-at-spot）；
    - `Dr 6603 = 6.67`（discountInterest functional）；
    - `Cr 6051 = 3.3000`（exchangeGainLoss 取负 + Cr 方向，对齐 Provider `:74-76` `fx<0 → Cr 6051=-fx`）；
    - `Cr 1121 = 666.67`（faceAmount functional）；
    - **复式平衡断言（Decision 2 硬约束）**：`Σ Dr = 663.3000 + 6.67 = 669.97` ≡ `Σ Cr = 3.3000 + 666.67 = 669.97`（`assertTrue(sumDr.compareTo(sumCr) == 0)`）。
    - **setup 注记**：新测试 setup 须 `seedSubject("6051", "汇兑损益")`（既有 `seedBase` 未含 6051，须加性 seed 行；对齐 0120-1 既有种子追加范式）。
  - 验证命令：`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinNotesReceivablePosting#testDiscountFxPosts6051VoucherLineAndBalance`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 6 实现项（2 Fix + 4 Add，含 ErrorCode）落地，2 Proof JUnit 回归全绿：`TestErpFinNotesReceivableStateMachine` +3 测试方法（既有 1 + 新增 3，state machine 层不验证凭证）+ `TestErpFinNotesReceivablePosting` +1 测试方法（posting 集成层，**必须含复式平衡断言 `Σ Dr == Σ Cr`** = 669.97 ≡ 669.97，Decision 2 硬约束），`mvn test -pl module-finance/erp-fin-service -am` 全模块 0 failure/0 error。
- [x] 本地化 typecheck：`mvn compile -pl module-finance/erp-fin-dao,module-finance/erp-fin-service -am` 通过（IBiz 接口变更下游 BizModel/Processor 编译一致）。

### Phase 3 — E2E 真实 mutation 路径迁移 + 回归

Status: completed
Targets:
- `tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts`（迁移测试 (2) + 数值表更新 + 注释段更新）
- `playwright.config.ts`（webServer JVM args 增 config）
- `docs/testing/e2e-runbook.md`（业务动作表 + config JVM arg 段 + 凭证行断言表更新）

Skill: `nop-testing`

- Item Types: `Fix | Proof`

Prereqs: Phase 2 完成（Java Builder/IBiz 已可调真实 5 参数 `discount` mutation）

- [x] **Fix 4：迁移 0120-1 spec 测试 (2) 至真实 `discount` mutation 路径**
  - 改动：`fin-notes-receivable-fx-discount.action.spec.ts` 测试 (2)「FX 6051 触发分支」由 `ErpFinVoucher__post` 直驱改为 `discount(notesId, discountDate, bankId, discountRate, exchangeRate=6.7000)` 真实 mutation：
    - 期望：status=DISCOUNTED + posted=true + 4 行凭证（`Dr 1002=663.3000` netAmount cash-at-spot + `Dr 6603=6.67` discountInterest functional + `Cr 6051=3.3000` exchangeGainLoss 取负 Cr 方向 + `Cr 1121=666.67` faceAmount functional）；
    - 数值表更新：`exchangeGainLoss` 由「确定性占位非派生 5.00」改为「公式派生值 `= note.amountFunctional − discountInterestFunctional − (netAmountSource × spotRate) = 666.67 − 6.67 − 663.3000 = -3.3000`（Cr 6051=3.3000 汇兑收益）」；`netAmount` 由 660.00 改为 663.3000（cash-at-spot）；
    - 注释段更新：删除「Java builder 缺陷 workaround」段，替换为「真实 mutation 路径已修复（cash-at-spot plug 范式 + 4 行凭证平衡）」段，对齐 0120-1 Deferred RELEASED 注记。
  - Skill: `nop-testing`
- [x] **Fix 5：保留测试 (3) 单币种 6051 抑制对照（CNY note 真实 mutation）**
  - 改动：测试 (3) 由 `ErpFinVoucher__post` 直驱改为 CNY note `discount(...)` 真实 mutation（不传 exchangeRate 或传 1）→ 期望 3 行凭证（无 6051）—— 与测试 (2) 形成对照（FX 触发 6051 vs CNY 抑制 6051）。
  - Skill: `nop-testing`
- [x] **Add 5：删除 spec 中 `ErpFinVoucher__post` 直驱代码块 + 相关 cleanup**
  - 改动：测试 (2)(3) 不再需要 `ErpFinVoucher__post` 直驱原语（仅留测试 (1)(2)(3) 三真实 mutation + 凭证行断言）；删除直驱路径专用的 billHeadCode 常量（如 `FX_VOUCHER_DIRECT_POST_BILL_CODE`）+ 相关 voucher cleanup（保留真实 mutation 的 voucher cleanup 经 `cleanupVoucherByBillCode(note.code)`）。
  - Skill: `nop-testing`
- [x] **Proof 4：E2E 套件回归**
  - 改动：`playwright.config.ts` webServer JVM args 增 `-Derp-fin.notes-fx-gain-loss-enabled=true`（对齐 Phase 1 Decision 3）。
  - 实测：`npx playwright test tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts`（3 测试全绿，含 FX 现金贴现 Cr 6051=3.3000 复式平衡 4 行凭证）+ `fin-notes-receivable.action.spec.ts`（1430-1 单币种 spec 7 测试 0 回归）+ `fin-notes-payable.action.spec.ts`（5 测试 0 回归）+ `finance-voucher-post.action.spec.ts`（1 测试 0 回归）= 16 测试全绿。
  - 实施注：webServer 实测监听端口 8011（`app-erp-all/src/main/resources/application.yaml:35` `quarkus.http.port=8011`），运行命令须 `PLAYWRIGHT_PORT=8011 npx playwright test ...`；如端口冲突或 CI 默认 8080 时改回默认。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 3 实现项（2 Fix + 1 Add）落地，1 Proof E2E 套件回归：FX discount 3 测试全绿（含迁移后真实 mutation 路径）+ 1430-1 单币种 spec 0 回归 + finance 抽样回归 0 新增失败。
- [x] webServer JVM args 增量已落地（`-Derp-fin.notes-fx-gain-loss-enabled=true`）。

### Phase 4 — owner-doc 对齐 + RELEASED 登记 + 日志

Status: completed
Targets:
- `docs/design/finance/treasury.md`（§业财过账 实现注记段）
- `docs/testing/e2e-runbook.md`（config JVM arg 段 + 凭证行断言表）
- `docs/plans/2026-07-19-0120-1-finance-treasury-fx-notes-discount-browser-e2e.md`（Deferred RELEASED）
- `docs/plans/2026-07-19-0330-1-fx-notes-receivable-honor-endorse-collect-browser-e2e.md`（同型 Deferred RELEASED）
- `docs/backlog/README.md`（+1 done 行）
- `docs/logs/2026/07-19.md`（聚合条目）

Skill: `none`

- Item Types: `Add`

Prereqs: Phase 3 完成

- [x] **Add 6：`treasury.md §业财过账` 实现注记段**
  - 改动：在「贴现凭证科目分解」段（l.157-164）后追加「DISCOUNTED exchangeGainLoss 派生实现注记（plan 2026-07-19-0730-1）」子段（≤30 行），含：**派生公式（cash-at-spot plug 范式）**——`discount.netAmount = netAmountSource × spotRate`（cash-at-spot，Dr 1002 行金额按贴现日即期汇率折算实际收到现金）+ `exchangeGainLoss = note.amountFunctional − discountInterestFunctional − discount.netAmount`（plug 平衡差额，Dr/Cr 6051）+ 符号语义（外币升值 → exchangeGainLoss<0 → Cr 6051 汇兑收益；外币贬值 → exchangeGainLoss>0 → Dr 6051 汇兑损失）+ 复式平衡约束（`Σ Dr ≡ Σ Cr`）+ config-gate `erp-fin.notes-fx-gain-loss-enabled` 默认 false + 向后兼容（旧签名 exchangeRate=null 走 ZERO 路径）+ successor（多汇率选择/汇率源表/NR 其他路径 FX 分支/NP Provider FX 分支）。
  - Skill: `none`
- [x] **Add 7：`e2e-runbook.md` 更新**
  - 改动：业务动作表 finance 行 `ErpFinNotesReceivable__discount` 注记「外币票据贴现：5 参数重载 `discount(exchangeRate)` 经 config `erp-fin.notes-fx-gain-loss-enabled=true` 启用，凭证行 4 行含 6051」；config JVM arg 段追加 `-Derp-fin.notes-fx-gain-loss-enabled=true`；凭证行断言表更新 DISCOUNTED FX 行（4 行：Dr 1002 / Dr 6603 / [Dr/Cr] 6051（外币升值场景为 Cr 6051=汇兑收益，外币贬值场景为 Dr 6051=汇兑损失）/ Cr 1121，对齐 treasury.md l.143 「[借/贷]」方向占位）。
  - Skill: `none`
- [x] **Add 8：0120-1 + 0330-1 Deferred RELEASED 登记**
  - 改动：0120-1 Deferred「`ErpFinNotesReceivableProcessor.buildDiscount` 外币 exchangeGainLoss 派生缺失」段补 `**RELEASED by 2026-07-19-0730-1**` 行 + 实施摘要（Builder 派生公式 + 5 参数重载 + config-gate + E2E 真实 mutation 迁移）；0330-1 同型 Deferred 段补 RELEASED 注记。
  - Skill: `none`
- [x] **Add 9：`docs/backlog/README.md` +1 done 行 + `docs/logs/2026/07-19.md` 聚合条目**
  - 改动：backlog README 增 1 行（0730-1 ✅ done，描述含：Java builder 缺陷修复 + 5 参数 IBiz 重载 + config-gate 默认 false + JUnit 4 新测试 + E2E 真实 mutation 路径迁移 + 0120-1/0330-1 Deferred RELEASED）；日志聚合条目按 `docs/logs/00-log-writing-guide.md` 格式。
  - Skill: `none`

Exit Criteria:

- [x] 4 文档对齐项落地（treasury.md 实现注记 + e2e-runbook + 0120-1/0330-1 RELEASED + backlog/logs）。
- [x] 0120-1 + 0330-1 Deferred 段均有 `**RELEASED by 2026-07-19-0730-1**` 行。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_088756bafffez4KDSnzbg1dixy）— 1 BLOCKING + 4 MAJOR + 5 MINOR：
  - **BI-1（已修订 iter-1）**：Decision 2 原公式 `exchangeGainLoss = netAmountSource × (exchangeRate − note.exchangeRate)` 不动 `discount.netAmount`，经独立审查冷核实产生不平衡凭证（Dr 1002=660 + Dr 6603=6.67 + Dr 6051=3.30 = 669.97 ≠ Cr 1121=666.67，差 3.30），会计正确性硬约束失败。修订为 **cash-at-spot plug 范式**：`discount.netAmount = netAmountSource × spotRate`（Dr 1002 行按贴现日即期汇率折算实际收到现金）+ `exchangeGainLoss = note.amountFunctional − discountInterestFunctional − discount.netAmount`（plug 吸收平衡差额）+ 符号语义反转（外币升值 → exchangeGainLoss<0 → **Cr 6051 汇兑收益**，非原 Dr 6051）。修订后数值示例经复式平衡验证：Dr 663.3000 + Dr 6.67 = Cr 666.67 + Cr 3.3000 = 669.97 ✓。
  - **MAJOR-1（iter-1 误判，iter-2 已纠正）**：iter-1 称 `TestErpFinNotesReceivablePosting` 不存在并建议合并入 `TestErpFinNotesReceivableStateMachine`。iter-2 经实时仓库核实**该类已存在**于 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinNotesReceivablePosting.java`（278 行，5 既有 @Test 含 `testDiscountPostsFiveLineDecomposition`），是 posting 集成测试的自然归属。iter-1 修订方向错误（误合并到 entity/ 包的 state machine 类），iter-2 已纠正为归入既有 `posting/TestErpFinNotesReceivablePosting` 类。
  - **MAJOR-2（已修订 iter-1）**：Add 3 `ErpFinConfigs.isNotesFxGainLossEnabled()` reader 抽象偏离代码库约定（`ErpFinConfigs.java` 仅 5 行空接口零 reader，既有 11+ 处 config 直调 `AppConfig.var(...)` 在调用点），改为 `ErpFinNotesReceivableProcessor` 内联 `private boolean notesFxGainLossEnabled()` reader。
  - **MAJOR-3（已修订 iter-1）**：Phase 2 Exit Criteria 「3 Fix + 3 Add」文本与实际「2 Fix + 4 Add」不符，已对齐为「2 Fix + 4 Add = 6 实现项落地」。
  - **MAJOR-4（已修订 iter-1）**：Decision 2 残留风险段补「凭证复式平衡约束（会计正确性硬约束）」+ Phase 2 Proof 3 JUnit 强制断言 `Σ Dr == Σ Cr`。
  - **MINOR-1（已修订 iter-1）**：`IErpFinNotesReceivableBiz.java:27-31` → `:26-31`。
  - **MINOR-2（已修订 iter-1）**：Fix 1 补注 functionalCurrencyId lookup 路径（iter-2 进一步纠正：`AcctSchemaResolver.resolveFunctionalCurrencyId` 公开 helper 不存在，改为内联 private helper 对齐 ExchangeRevaluationService:280 范式）。
  - **MINOR-3（已修订 iter-1）**：Task Route Type 由 `bug investigation + implementation-only change` 改为 `implementation-only change`。
  - **MINOR-4（已修订 iter-1）**：Decision 1 残留风险补注 Nop GraphQL schema 由 Java `@Name` 重载自动派生无须手工编辑 `*.xbiz.xml`。
  - **MINOR-5（已修订 iter-1）**：Deferred「config-gate 默认值切换为 true」分类由 `watch-only residual` 改为 `optimization candidate` + Successor Required: yes 附触发条件。
- Independent draft review iteration 2: `needs revision`（ses_088703e12ffeikYPF84l6HBm6K）— 1 BLOCKING + 2 MAJOR + 3 MINOR：
  - **BI-1（已修订 iter-2）**：iter-1 MAJOR-1 误判基础上修订方向错误（合并 Proof 3 到 `TestErpFinNotesReceivableStateMachine`）。iter-2 经实时仓库核实 `TestErpFinNotesReceivablePosting` **已存在**于 `posting/` 包（278 行 + 5 既有 @Test），是 posting 集成测试自然归属。已纠正：Proof 3 改为在既有 `TestErpFinNotesReceivablePosting` 增 1 新方法。
  - **MAJOR-1（已修订 iter-2）**：Fix 1 称「`AcctSchemaResolver.resolveFunctionalCurrencyId` 既有 helper 复用」事实错误——经实时仓库核实该公开 helper 不存在，仅有 4 处 private 副本（`BadDebtProvisionService:364` / `ExchangeRevaluationService:280` / `AnnualCloseService:388` / `ProfitLossClosingService:215` 查询 `ErpMdCurrency.isFunctional=TRUE`）。已纠正：Fix 1 改为 Processor 内联 private helper 对齐既有范式，备选 `resolvePrimarySchemaId + ErpMdAcctSchema.functionalCurrencyId` 两步反查。
  - **MAJOR-2（已修订 iter-2）**：Decision 2 数值示例与 Proof 3 复式平衡断言显示「Σ = 670.00」，实际算术为 669.97（663.3000 + 6.67 = 669.97；666.67 + 3.3000 = 669.97）。凭证平衡结论不变（669.97 ≡ 669.97），仅显示文本错误。已纠正 3 处（Decision 2 数值示例 / Decision 2 alt (b) 拒绝段 / Proof 3 平衡断言）为 669.97。
  - **MINOR-1（已修订 iter-2）**：Proof 3 setup 须 `seedSubject("6051", "汇兑损益")`（既有 seedBase 未含 6051），Proof 3 已加注记；Proof 2 state machine 测试不验证凭证无须该 seed，已加注记区分。
  - **MINOR-2（已修订 iter-2）**：Phase 2 Exit Criteria 测试计数「7」错误，实际 4 新测试方法（3 state machine + 1 posting 集成）+ 既有 1 零回归 = 5 个测试在 2 个测试类。已纠正为「2 Proof JUnit 回归全绿：StateMachine +3 + Posting +1 = 4 新测试方法」。
  - **MINOR-3（已修订 iter-2）**：Decision 2 alt (b) 拒绝段 Dr sum 显示「670.00」实为 669.97，与 MAJOR-2 同源，已一并纠正。
- Independent draft review iteration 3: `needs revision`（ses_0886c6011ffeNwbz0An7v5b6mH）— 1 BLOCKING + 1 MINOR：
  - **BI-1（已修订 iter-3）**：Phase 4 Add 6 owner-doc 实现注记仍记录**已拒绝的 alt (b) 公式** `netAmountSource × (exchangeRate − note.exchangeRate)`（iter-1 BI-1 已证产生不平衡凭证），与 Decision 2 已采纳的 cash-at-spot plug 范式不一致——若按此实施将永久编码 imbalance bug 到规则 13 保护的 owner-doc。已纠正：Add 6 注记公式改为与 Decision 2 / Proof 3 一致的 cash-at-spot plug 范式（`discount.netAmount = netAmountSource × spotRate` + `exchangeGainLoss = note.amountFunctional − discountInterestFunctional − discount.netAmount`）。
  - **MINOR-1（已修订 iter-3）**：Decision 2 数值示例使用「Dr 0（loss 路径）」双路径注记，与 Proof 3 清爽形式不一致；已统一为 Proof 3 同款清爽形式「Dr 663.3000 + Dr 6.67 = 669.97 ≡ Cr 666.67 + Cr 3.3000 = 669.97 ✓」。
- Independent draft review iteration 4: `accept`（ses_08869c20fffeOZ6XlR4uhTdDFR）— 0 BLOCKING + 1 non-blocking MINOR：
  - **MINOR-1（已修订 iter-4 inline）**：Add 7 凭证行断言表方向记号「Dr 6051」与全文选用外币升值场景（Cr 6051=汇兑收益）方向不一致；已统一为 `[Dr/Cr] 6051`（外币升值场景为 Cr 6051 / 外币贬值场景为 Dr 6051），对齐 treasury.md l.143 「[借/贷]」方向占位。
  - **一致性终审 PASS**：cash-at-spot plug 公式在 Decision 2 / Fix 1 / Proof 3 / Fix 4 / Add 6 / Add 7 六处参考点完全一致；数值示例（669.97 ≡ 669.97 复式平衡）清爽形式统一；iter-1 BI-1 + iter-2 BI-1/MAJOR-1/MAJOR-2 + iter-3 BI-1 全部修订收敛；iter-4 verdict `accept`，plan 可 flip to `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（Java builder 派生 + IBiz 重载 + JUnit 4 新测试 + E2E 真实 mutation 迁移 + owner-doc 注记 + 0120-1/0330-1 RELEASED）
- [x] 相关文档对齐（`treasury.md` 实现注记段 + `e2e-runbook.md` 业务动作表/config/凭证行表）
- [x] 已运行验证：
  - `mvn test -pl module-finance/erp-fin-service -am`（含新增 4 测试方法 0 failure/0 error）
  - `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）
  - `npx playwright test tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts`（3 测试全绿）
  - `npx playwright test tests/e2e/business-actions/fin-notes-receivable.action.spec.ts`（1430-1 单币种 spec 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留作未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Phase 1 Explore 结果 + 实时仓库核实）。执行期确认后分类。

### NR RECEIVED/ENDORSED/COLLECTION 路径 FX 分支引入（6051 行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner-doc `treasury.md §业财过账 l.143` 仅 DISCOUNTED 明示 FX 语义；RECEIVED/ENDORSED/COLLECTION 路径未明示 FX 重估，FX 重估由期末结账 `ExchangeRevaluationService` 统一处理（bank-fx-revaluation-enabled config-gated）。本计划仅修复 DISCOUNTED 缺陷，不动 NR 其他路径 Provider。
- Successor Required: `yes`（触发条件：owner-doc 显式要求 NR RECEIVED/ENDORSED/COLLECTION 路径在过账时即认列已实现汇兑损益时——须先更新 `treasury.md` owner doc 明示行为 + 独立计划）

### NP Provider FX 分支引入（6051 行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0330-2 Deferred 显式 successor；NP ISSUED/HONORED 路径 owner-doc 未明示 FX 语义；属不同实体 + 不同 Provider + 不同科目分解（2202/2203 vs 1121）。
- Successor Required: `yes`（触发条件：owner-doc 显式要求 NP 外币 ISSUED/HONORED 路径认列汇兑损益时）

### 多汇率选择精细化（票面日 vs 贴现日 vs 到期日）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划采用「贴现日即期汇率 vs 票面原汇率」最简化语义（Decision 2）；多汇率选择规则属产品决策 successor。
- Successor Required: `yes`（触发条件：产品要求贴现息按票面日汇率折算 / 到期日汇率折算等多汇率选择规则落地时）

### 汇率源数据表（CurrencyRateTable）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划汇率由 `discount` mutation 显式入参传入；央行汇率/银行挂牌汇率自动获取属配置层 successor。
- Successor Required: `yes`（触发条件：自动化汇率源接入需求落地时——须 ORM 加性 `erp_md_currency_rate` 表 + 独立 ask-first 计划）

### config-gate 默认值切换为 true

- Classification: `optimization candidate`
- Why Not Blocking Closure: 默认 false 向后兼容；运营层启用前生产路径仍走 ZERO。本计划 E2E 启用 config 验证派生路径。
- Successor Required: `yes`（触发条件：缺陷修复在生产稳定运行一段时间后，运营层确认无回归时单独 successor 切换默认值——纯常量改动 + 既有测试断言更新）

## Closure

Status Note: 计划仅在独立结束审计接受「Java builder 缺陷已修复 + IBiz 5 参数重载已落地 + JUnit 4 新测试全绿 + E2E 真实 mutation 路径迁移完成 + owner-doc 实现注记已对齐 + 0120-1/0330-1 Deferred RELEASED」后关闭。独立结束审计已在 fresh session（无执行者上下文）完成对实时仓库的逐项核实：Java Processor cash-at-spot plug 公式、IBiz/BizModel 单 5 参数方法、4 JUnit 测试方法（含 posting 集成复式平衡断言）、E2E spec 真实 mutation 迁移、owner-doc 注记段、0120-1/0330-1 RELEASED 行、backlog/logs 聚合条目均已在仓库中验证存在且语义一致。审计结论：所有范围内项目均已 landed，五点一致（Plan Status / Phase Status / Exit Criteria / Closure Gates / Closure 证据）通过，无范围内项目降级为 deferred/follow-up，无 anti-hollow 风险（Builder 公式经 posting 集成测试断言 `Σ Dr=669.97 ≡ Σ Cr=669.97` 复式平衡 + E2E 真实 mutation 路径产生 4 行凭证 Cr 6051=3.3000）。计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure audit subagent（fresh session，无执行者上下文冷重播；audit 会话于 2026-07-19 完成）
- Evidence:
  - **Java Processor（核实 landed）**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinNotesReceivableProcessor.java:233-286` `buildDiscount` 实现三联条件 `fxPlugEnabled = notesFxGainLossEnabled() && isForeignCurrency(note) && exchangeRate != null` 分支；cash-at-spot plug 公式 `netAmount = netAmountSource.multiply(exchangeRate).setScale(4, HALF_UP)` + `exchangeGainLoss = faceAmountFunctional.subtract(discountInterestFunctional).subtract(netAmount).setScale(4, HALF_UP)` 与 Decision 2 一致；兜底分支保留 ZERO + functional 口径向后兼容；`discount(...)` 公共方法 5 参数签名（`:58-65`）；内联 `notesFxGainLossEnabled()` / `isForeignCurrency(note)` / `resolveFunctionalCurrencyId()` private helper（`:375-398`，对齐 `ExchangeRevaluationService:280-287` 范式）。
  - **IBiz/BizModel 单 5 参数方法（核实 landed）**：`IErpFinNotesReceivableBiz.java:32-38` + `ErpFinNotesReceivableBizModel.java:42-51` 均为单 `discount(notesId, discountDate, bankId, discountRate, @Optional exchangeRate, context)` 方法（无同名重载，对齐 Nop GraphQL BizModel 限制）；`@Optional` 使下游既有 4 参数 GraphQL 调用透明兼容。
  - **Constants + ErrorCode（核实 landed）**：`ErpFinConstants.java:242` `CONFIG_NOTES_FX_GAIN_LOSS_ENABLED = "erp-fin.notes-fx-gain-loss-enabled"`；`ErpFinErrors.java:249` `ERR_NOTES_DISCOUNT_FX_RATE_REQUIRED` ErrorCode 已定义（保留作 hard-fail successor 候选，默认不抛对齐 Decision 2 兜底语义）。
  - **JUnit 测试（核实 landed）**：`TestErpFinNotesReceivableStateMachine.java` +3 测试方法（`testDiscountFxWithSpotRateDerivesExchangeGainLossCashAtSpot:75` / `testDiscountFxFallbackWhenSpotRateNull:113` / `testDiscountFxSuppressedByConfigGate:149`）；`TestErpFinNotesReceivablePosting.java:101` `testDiscountFxPosts6051VoucherLineAndBalance` posting 集成测试含复式平衡断言 `Σ Dr=669.97 ≡ Σ Cr=669.97`（Decision 2 硬约束）。
  - **E2E 真实 mutation 路径（核实 landed）**：`tests/e2e/business-actions/fin-notes-receivable-fx-discount.action.spec.ts` 测试 (2) 经 `discount(exchangeRate: SPOT_RATE_USD_CNY)` 真实 mutation（`:222`）触发 4 行凭证 `Cr 6051=3.3000` cash-at-spot plug（不再经 `ErpFinVoucher__post` 直驱 workaround）；直驱原语 `postVoucher`/`buildNotesReceivableDiscountedEvent` 已删除；`playwright.config.ts:18` webServer JVM args 增 `-Derp-fin.notes-fx-gain-loss-enabled=true`。
  - **owner-doc 对齐（核实 landed）**：`docs/design/finance/treasury.md:166-176` 「DISCOUNTED exchangeGainLoss 派生实现注记（plan 2026-07-19-0730-1，cash-at-spot plug 范式）」段含公式 + 符号语义 + config-gate；`docs/testing/e2e-runbook.md:53` config JVM arg 段 + `:315` 业务动作表 + `:410` 凭证行断言表 DISCOUNTED FX 4 行（含 [Dr/Cr] 6051 方向占位）。
  - **RELEASED + backlog/logs（核实 landed）**：`docs/plans/2026-07-19-0120-1-...md:197` + `docs/plans/2026-07-19-0330-1-...md:256` 同型 Deferred 段均含 `**RELEASED by 2026-07-19-0730-1**` 行 + 实施摘要；`docs/backlog/README.md:110` +1 done 行（`2026-07-19-0730-1 ✅ done plan-first`）；`docs/logs/2026/07-19.md:3-13` 聚合条目按 log writing guide 格式记录 4 Phase 全绿 + 验证状态。
  - **验证状态对齐**：日志条目（`:13` 验证状态段）记录 `mvn test -pl module-finance/erp-fin-service -am` JUnit 全绿（StateMachine 12/12 含新增 3 + Posting 6/6 含新增 1）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + Playwright `fin-notes-receivable-fx-discount.action.spec.ts` 3 passed + finance 抽样回归 16 passed 0 新增失败，与 Closure Gates 验证项一致。
  - **Anti-Hollow 检查通过**：Java Processor `buildDiscount` 公式分支经 posting 集成测试在运行时实际调用并产生 4 行凭证（含 Cr 6051=3.3000 汇兑收益）；E2E 经真实 `discount` mutation 全链触发（Builder → Dispatcher → Provider）非 mock；config-gate 默认 false 但 webServer JVM args 显式启用 true 以激活生产路径；`ERR_NOTES_DISCOUNT_FX_RATE_REQUIRED` ErrorCode 已落地（保留 successor 候选非空 stub）。
  - **Deferred 诚实性**：5 项 Deferred But Adjudicated 均非范围内降级（NR 其他路径 FX 分支 / NP Provider FX 分支 / 多汇率精细化 / CurrencyRateTable / config-gate 默认值切换）均明确 successor 触发条件，无已确认 live defect 隐藏。

Follow-up:

- NR RECEIVED/ENDORSED/COLLECTION 路径 FX 分支引入（须 owner-doc 显式要求 + 独立计划）
- NP Provider FX 分支引入（须 owner-doc 显式要求 + 独立计划）
- 多汇率选择精细化（须产品决策 + 独立计划）
- 汇率源数据表（须 ORM ask-first + 独立计划）
