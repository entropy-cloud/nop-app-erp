# RC MA1 A1.4 — finance-F4 银行对账与未达账项 需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.4（MA1 需求追踪矩阵审计 — finance-F4 银行对账与未达账项）
> 审计 plan：`docs/plans/2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-reconciliation.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/finance/use-cases.md`（UC-FIN-09/14，2 UC）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §finance + §切片索引 A1.4 + §基线分歧登记 D-01
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-02
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **2** | P1-RC-004（UC-FIN-09/14 自动勾对"对方账号模糊匹配"缺失）/ P1-RC-005（UC-FIN-09/14 下月初自动红冲缺失，config key 无 scheduler 消费）→ 待 MR1（R1.0 展开为 RC-R1.n） |
| **P2**（新登记） | **3** | P2-RC-001（导入幂等 dedup key 偏离 + 跨多条 statement 去重范围）/ P2-RC-002（valueDate→transactionDate 简化）/ P2-RC-003（多币种调整 exchangeRate=ONE 硬编码）→ successor watch-only |
| **P2**（复用） | **1** | P2-MA3-024（余额调节恒等式简化 2 项 vs spec 4 项，watch-only，追加 RC 交叉引用） |
| **接受**（符合需求契约） | **1 验收标准族** | UC-FIN-14 断言 B4（调整凭证生成）+ B6（与 AR/AP 解耦，经 MA2 证实）+ UC-FIN-09/14 调整凭证 businessType=BANK_RECON_ADJ 生成路径 |
| MA2 既有行为证据复用 | 解耦 + 物化视图简化 | 无升级（详见 §4 / §9） |
| 报告校正项 | **1** | 2026-07-06 审计 `:116` 引用 `AutoReconJob` 标 UC-FIN-09 ✅ 失实（该类为 AR/AP 子系统，非银行对账，且已于 2026-07-18 batch 迁移删除） |

**整体裁决**：A1.4 切片 2 UC 五级追踪矩阵填齐。银行对账主体（导入幂等去重 + 自动勾对 MATCHED/UNMATCHED/SUSPENSE 三态 + 余额调节恒等式差额=0 守卫 + 未达账项 BANK_RECON_ADJ 调整凭证生成 + 手动红冲 + 期间 CLOSED 守卫 + 与 AR/AP 解耦）经 L3-L5 四级证据确认实现存在且主路径可运行。**两项 P1 需求分歧**：①UC-FIN-09 断言②/UC-FIN-14 断言②「自动勾对按 (金额, 反向方向, valueDate±N天, **对方账号**) 模糊匹配」的**对方账号维度缺失**（ORM 无 counterpartyAccount 列，`BankLedgerQuery.findCandidates` 不过滤对方账号——同额同日不同对方账号的交易可能被错误 MATCHED，属未文档化的静默缺口）；②UC-FIN-09 断言④/UC-FIN-14 断言⑤「下月初**自动**红冲（跨期还原）」的**自动调度缺失**（config key `erp-fin.bank-recon-auto-reverse-next-month` 定义于 `ErpFinConstants.java:289` 但无 scheduler/cron 消费，仅手动 `reverse()` 存在——临时调整凭证不会自动跨期还原，会计余额潜在错报）。两项均按 §2 判据定为 P1（功能维度缺失 / 自动功能完全缺失），按 §10 经 MR1 批量修复通道修复（R1.0 展开为 RC-R1.n）；**无 P0**——错误匹配经手动取消可逆 + 余额恒等式下游兜底；自动红冲缺失可由手动 reverse 补救，非默认活跃路径破坏。三项 P2 为次要验收标准偏差（主路径 OK，边界场景弱）。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。以下逐 UC 逐字引用验收标准原文，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-FIN-09 银行对账与未达账项（`use-cases.md:165`）

```
可验证断言（见 bank-reconciliation.md）：
导入银行对账单(bankTxnCode 幂等去重)
自动勾对: 金额 + 反向方向 + valueDate±N天 + 对方账号 模糊匹配
余额调节恒等式:
  银行余额 + 在途(企已记银未记) == 账面余额 + 未达(银已记企未记)
  差额 == 0 才可 RECONCILED
未达账项 → 生成调整凭证(businessType=BANK_RECON_ADJ), 下月红冲
```

### UC-FIN-14 银行对账与未达账项（`use-cases.md:269`，更详细断言版）

```
可验证断言：
// 导入幂等
导入银行对账单 → 以 (fundAccount, statementDate, bankTxnCode) 去重, 重复导入报错

// 自动勾对
按 (金额, 反向方向, valueDate±N天, 对方账号) 模糊匹配
命中唯一 → MATCHED; 多候选 → UNMATCHED; 金额对户名差 → SUSPENSE

// 余额调节恒等式
银行余额 + 在途(企已记银未记) == 账面余额 + 未达(银已记企未记)
差额 == 0 才可 RECONCILED, 否则抛异常

// 未达账项调整
RECONCILED 时若存在未达 → 生成调整凭证(businessType=BANK_RECON_ADJ)
下月初自动红冲(跨期还原)

// 与 AR/AP 核销解耦
银行对账只确认钱到账/已付, 不替代发票核销(ErpFinReconciliation)
```

> **D-01 基线分歧注记**（`rc-requirement-baseline-inventory.md §基线分歧登记 D-01`）：UC-FIN-09/14 标题完全相同（"银行对账与未达账项"），断言重叠（导入幂等/自动勾对/恒等式/调整凭证/红冲/解耦），UC-FIN-14 为更详细断言版（显式 dedup 三元组 + MATCHED/UNMATCHED/SUSPENSE 三态判定 + "自动"红冲 + ErpFinReconciliation 解耦）。roadmap 已裁决两者同属 A1.4 切片。本报告按 L1 **逐字引用两条验收标准，不合并**（§9 真相源冻结条款）。断言重叠观察：两条 UC 的"自动勾对维度 / 余额调节恒等式 / 未达调整凭证"三处语义一致，UC-FIN-14 额外显式 (a) dedup 三元组 (b) 三态判定 (c) "自动"红冲 (d) 解耦实体名——本报告逐条进入 L5 判读，重叠断言不重复定级。

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/bankrecon/`，6 个 .java 文件）。L3 引用格式遵循 §1 L3 规范（含行号）。调用链：BizModel facade（薄）→ 6 Processor/helper 类（R6.1 per-mutation 拆分）。

### 2.1 导入幂等路径（UC-FIN-09 断言① / UC-FIN-14 断言①）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 导入入口 | `BankStatementImporter.java` importStatement:44-107（校验 :46-49 → requireBankAccount :51 → 批次校验 :54-58 → assertNoDuplicates :59 → 写头 :61-73 → 写行 :75-102 → 合计回写 :103-105） | ✅ |
| 账户类型守卫 | `BankStatementImporter.java` requireBankAccount:109-122（非 BANK 抛 `ERR_FUND_ACCOUNT_NOT_BANK` :117-119） | ✅ |
| 行级校验 | `BankStatementImporter.java` validateLine:124-141（必填 + 非负 + DC 合法 + strict-refno :137-140） | ✅ |
| **dedup key**（实现） | `BankStatementImporter.java` assertNoDuplicates:147-178（refNo 优先 → seenRefNo + existsByRefNo :158-162；缺失回退 `(transactionDate, amount, dcDirection)` 组合键 → seenComposite + existsByComposite :163-176） | ⚠️ **dedup key 偏离 L1**（见 §5） |
| **dedup 范围**（实现） | `BankStatementImporter.java` findStatementIdByAccount:198-207（`fundAccountId` 过滤 + `statementDate DESC` + `limit 1`——**仅查最近一条 statement** 作去重范围） | ⚠️ **跨多条 statement 重复不检出**（见 §5） |
| strict-refno 配置 | `AppConfig.var(CONFIG_BANK_IMPORT_STRICT_REFNO, false)` :52 | ✅ |

### 2.2 自动勾对路径（UC-FIN-09 断言② / UC-FIN-14 断言②）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 自动勾对入口 | `BankStatementMatcher.java` autoMatch:41-74（loadUnmatchedLines :51 → 逐行 oppositeDirection :53 → findCandidates :58-59 → 1 候选 MATCHED :61-65 / 0 候选 UNMATCHED :66-67 / ≥2 候选 SUSPENSE :68-71） | ✅ 三态判定逻辑 |
| 反向方向映射 | `BankStatementMatcher.java` oppositeDirection:105-113（DEBIT→CREDIT / CREDIT→DEBIT / else null） | ✅ |
| 候选查询 | `BankLedgerQuery.java` findCandidates:39-84（null 守卫 :41-44 → 日期窗口 :45-46 → findVoucherIdsInWindow :50 → 按 subjectId+dcDirection+amount+voucherId 过滤 :63-72 → 排除 occupied :76-83） | ⚠️ **对方账号过滤缺失**（见 §5） |
| **日期窗口列**（实现） | `BankLedgerQuery.java` findCandidates:45-46 + `BankStatementMatcher.java`：窗口基于 `txnDate`（= `line.getTransactionDate()`，:59），非 `valueDate` | ⚠️ **valueDate 列缺失**（见 §5） |
| 凭证窗口查询 | `BankLedgerQuery.java` findVoucherIdsInWindow:87-102（docStatus=POSTED + voucherDate∈[from,to] + 排除 isReversed :96-98） | ✅ |
| 已占用排除 | `BankLedgerQuery.java` findOccupiedLineIds:105-123（按 statement.fundAccountId 反查 MATCHED/MANUAL_MATCHED 行的 matchedLineId） | ✅ |
| 日期窗口配置 | `BankLedgerQuery.java` resolveDaysWindow:125-132（`CONFIG_BANK_MATCH_TOLERANCE_DAYS` 默认 3） | ✅ |

### 2.3 余额调节恒等式路径（UC-FIN-09 断言③ / UC-FIN-14 断言③）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 调节表生成 | `BankReconciliationBuilder.java` generate:47-117（requireStatement :48 → requireFundAccount :49 → assertPeriodNotClosed :50 → 聚合 UNMATCHED 行 :52-63 → 恒等式计算 :65-82 → 不平衡抛 `ERR_BANK_RECON_NOT_BALANCED` :77-82 → 写调节表 :84-97 → 写调整行 :99-115） | ✅ 守卫存在 |
| **恒等式（实现 2 项简化）** | `BankReconciliationBuilder.java` generate:67-75（`diff = (statementBalance − bookBalance) − (bankCreditUnrecorded − bankDebitUnrecorded)`，即 `statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded`；**在途 = 0** Non-Goal） | ⚠️ **简化 2 项 vs spec 4 项**（复用 P2-MA3-024） |
| 期间 CLOSED 守卫 | `BankReconciliationBuilder.java` assertPeriodNotClosed:191-201（glStatus=CLOSED 抛 `ERR_BANK_RECON_PERIOD_CLOSED` :198-200） | ✅ |
| 精度配置 | `BankReconciliationBuilder.java` reconcilePrecision:225-228（`CONFIG_RECONCILE_PRECISION` 默认 0.01） | ✅ |

### 2.4 未达调整凭证路径（UC-FIN-09 断言④ / UC-FIN-14 断言④⑤）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| post 入口 | `BankReconciliationBuilder.java` post:119-131（DRAFT 守卫 :121-123 → 调 adjustmentVoucherBuilder.post :127 → docStatus=POSTED :129） | ✅ |
| 调整凭证构造 | `BankReconAdjustmentVoucherBuilder.java` post:58-95（聚合 totalBankCredit/Debit :63-71 → 空 guard :72-74 → 解析科目 :76-78 → 构造 PostingEvent :80-93 → voucherBiz.post :94） | ✅ 凭证生成 |
| businessType | `BankReconAdjustmentVoucherBuilder.java` post:81（`event.setBusinessType(ErpFinBusinessType.BANK_RECON_ADJ)`） | ✅ |
| **exchangeRate**（实现） | `BankReconAdjustmentVoucherBuilder.java` post:86（`event.setExchangeRate(BigDecimal.ONE)`——**硬编码 rate=1**） | ⚠️ **多币种未处理**（见 §5） |
| 凭证分录生成 | `BankReconAdjAcctDocProvider.java` createFacts:51-70（bankCredit>0 → Dr 银行 / Cr 调整 :59-63；bankDebit>0 → Dr 调整 / Cr 银行 :64-68；2 或 4 条 VoucherFact） | ✅ 借贷平衡 |
| 手动红冲 | `BankReconciliationBuilder.java` reverse:133-142（POSTED 守卫 :135-137 → adjustmentVoucherBuilder.reverse :138 → docStatus=CANCELLED :140）；`BankReconAdjustmentVoucherBuilder.java` reverse:97-102（hasAdjustmentVoucher guard :98-100 → voucherBiz.reverse :101） | ✅ 手动红冲存在 |
| **自动红冲调度**（实现） | config key `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH` 定义于 `ErpFinConstants.java:289`；`rg "CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH\|auto-reverse\|AutoReverse\|autoReverse" module-finance` **仅 1 命中（定义本身）**——**无 scheduler/cron/Job bean 消费** | ⚠️ **下月自动红冲完全缺失**（见 §5） |
| 调整凭证反查 | `BankReconAdjustmentVoucherBuilder.java` hasAdjustmentVoucher:105-107 / countAdjustmentLinks:109-116（按 ErpFinVoucherBillR.businessType=BANK_RECON_ADJ 反查，对齐 findBillLinks 范式） | ✅ |

### 2.5 与 AR/AP 解耦（UC-FIN-14 断言⑥）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 独立实体/表 | ErpFinBankStatement/Line/Reconciliation（银行对账）vs ErpFinReconciliation（AR/AP 核销）——独立 ORM 实体 + 独立 BizModel + 独立表 | ✅ 解耦（MA2 证实） |

---

## 3. 测试证据（L4，注明断言强度）

> 断言强度分档引用 MA5（`docs/audits/2026-07-29-1430-arm-ma5-finance-test-coverage.md:74,92` 评级"✅ 深 / ✅ 匹配"）。4 测试类 19 @Test 于 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/`。

| UC | 测试引用 | 断言强度 | 覆盖判定 |
|---|---|---|---|
| UC-FIN-09/14 断言①（导入幂等 refNo） | `TestErpFinBankStatementImport.java#testDuplicateByRefNoRejected:81-98` | **强** | ✅ 同 refNo 重复导入抛 NopException |
| UC-FIN-09/14 断言①（非 BANK 拒绝） | `TestErpFinBankStatementImport.java#testNonBankAccountRejected:101-112` | **强** | ✅ CASH 账户抛 NopException |
| UC-FIN-09/14 断言①（strict-refno） | `TestErpFinBankStatementImport.java#testStrictRefNoMissingRejected:115-134` | **强** | ✅ strict=true 缺 refNo 抛 NopException |
| UC-FIN-09/14 断言①（组合键去重） | `TestErpFinBankStatementImport.java#testCompositeKeyDedupWhenNoRefNo:137-153` | **强** | ✅ 无 refNo 同 (date,amount,dc) 拒绝 |
| UC-FIN-09/14 断言①（跨账号允许） | `TestErpFinBankStatementImport.java#testImportDistinctStatementsNoCrossAccountDuplicate:156-178` | **强** | ✅ 不同账户同 refNo 允许 |
| UC-FIN-09/14 断言①（happy path） | `TestErpFinBankStatementImport.java#testImportHappyPath:54-78` | **强** | ✅ DRAFT + 合计回写 + UNMATCHED 初始化 |
| UC-FIN-09/14 断言②（唯一→MATCHED） | `TestErpFinBankStatementMatch.java#testUniqueCandidateMatched:63-90` | **强** | ✅ matched=1 + matchedLineId 回写 |
| UC-FIN-09/14 断言②（多候选→SUSPENSE） | `TestErpFinBankStatementMatch.java#testMultipleCandidatesStayUnmatched:93-120` | **强** | ✅ suspense=1（注意：实现多候选→SUSPENSE，spec UC-FIN-14 文本为"多候选→UNMATCHED"，实现为 SUSPENSE——spec 内部 09/14 断言重叠处 09 无三态、14 三态，实现取 14 三态语义，本报告记录实现选择） |
| UC-FIN-09/14 断言②（无候选→UNMATCHED） | `TestErpFinBankStatementMatch.java#testNoCandidateUnmatched:123-145` | **强** | ✅ unmatched=1 |
| UC-FIN-09/14 断言②（同向拒绝） | `TestErpFinBankStatementMatch.java#testDirectionOppositeRequired:148-168` | **强** | ✅ 同向不匹配 |
| UC-FIN-09/14 断言②（manualMatch） | `TestErpFinBankStatementMatch.java#testManualMatchMarksManualMatched:171-194` | **强** | ✅ MANUAL_MATCHED + matchedLineId |
| UC-FIN-09/14 断言②（occupied 排除） | `TestErpFinBankStatementMatch.java#testMatchedLineIdOccupiedExcludedFromLaterCandidates:224-252` | **强** | ✅ 已占用分录不再被勾对 |
| UC-FIN-09/14 断言②（重复勾对拒绝） | `TestErpFinBankStatementMatch.java#testManualMatchRejectsAlreadyMatched:197-221` | **强** | ✅ 已勾对行重复勾对抛 NopException |
| UC-FIN-09/14 断言③（平衡） | `TestErpFinBankReconciliation.java#testGenerateBalancedNoUnrecorded:71-90` | **强** | ✅ isBalanced + diff=0 + DRAFT |
| UC-FIN-09/14 断言③（不平衡拒绝） | `TestErpFinBankReconciliation.java#testGenerateUnbalancedRejected:93-111` | **强** | ✅ 抛 NopException |
| UC-FIN-09/14 断言③（期间 CLOSED 拒绝） | `TestErpFinBankReconciliation.java#testPeriodClosedRejectsGenerate:174-194` | **强** | ✅ 抛 NopException |
| UC-FIN-09/14 断言④（无未达不产凭证） | `TestErpFinBankReconciliation.java#testPostNoAdjustmentVoucherWhenNoUnrecorded:114-133` | **强** | ✅ post 后 billR count=0 |
| UC-FIN-09/14 断言④（post 产凭证+reverse） | `TestErpFinBankReconciliation.java#testPostGeneratesAdjustmentVoucherAndReverse:136-171` | **中**（仅断言凭证存在性 + billR 计数 + docStatus + reversal 计数，**未断言行级 Dr/Cr/科目/金额**） | ⚠️ 调整凭证**存在性**强断言，**行级细节**无断言（见 §7 存疑点） |
| UC-FIN-09/14 全链 | `TestErpFinBankReconciliationEndToEnd.java`（1 @Test，import→match→generate→post→reverse） | **中** | ✅ 全链冒烟（行级断言同上） |
| E2E | `tests/e2e/business-actions/fin-bank-recon.action.spec.ts`（282 行，3 case，import `assertVoucherLines` from `_helper`） | **中** | ✅ 浏览器层三态状态机全栈可达（MA5 评级引用） |
| **覆盖空洞** | 跨多条 statement 去重 / 对方账号匹配 / valueDate 窗口 / 多币种调整 / 下月自动红冲 | — | ❌ 均**无测试** |

**测试证据汇总**：导入幂等（refNo/composite/strict/cross-account）+ 自动勾对三态 + 方向校验 + manualMatch + occupied 排除 + 恒等式平衡/不平衡/期间 CLOSED + 调整凭证存在性 + 手动红冲——强断言覆盖。**未覆盖**：跨多条 statement 去重、对方账号匹配维度、valueDate 窗口、多币种调整（exchangeRate=ONE）、下月自动红冲（config key 无消费）。调整凭证测试仅断言凭证存在性 + billR 计数，**未断言行级 Dr/Cr/科目/金额**（`BankReconAdjAcctDocProvider.createFacts` 产出的 2-4 条 VoucherFact 的行级正确性无断言）。

---

## 4. 运行时行为证据（L5，复用 MA2/E2E + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，**不重新核实行为本身**；本切片只补"需求契约↔实际行为"差异。

### 4.1 复用 MA2 已证实行为（`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`）

| MA2 已证实行为 | 引用 | 本切片复用判定 |
|---|---|---|
| 银行对账为独立子系统（ErpFinBankStatement/Line/Reconciliation 独立于 ErpFinReconciliation） | MA2 :48,223,365（声明银行对账与 AR/AP 核销解耦，Non-Goal 不合并） | ✅ 复用（UC-FIN-14 断言⑥ 解耦证实） |
| 银行对账只确认钱到账/已付，不替代发票核销 | MA2 :48（独立机制，sourceBillCode=PAYMENT/RECEIPT 仅交叉查询） | ✅ 复用（UC-FIN-14 断言⑥） |

### 4.2 复用 MA3/MA4/MA5 既有证据

| 证据 | 引用 | 本切片复用判定 |
|---|---|---|
| bank-reconciliation.md 存在 | `2026-07-28-1510-arm-ma3-design-completeness-scan.md:72,195` | ✅ L2 设计参考存在 |
| badge 漂移波及 BankReconciliation/BankStatement | `2026-07-29-0430-arm-ma4-finance-mfg-view-xml-drift.md:97,116`（P2-MA4-014 watch-only） | ✅ view.xml 维度，非本切片需求契约维度 |
| 测试覆盖证据 | `2026-07-29-1430-arm-ma5-finance-test-coverage.md:74,92`（"✅ 深 / ✅ 匹配"） | ✅ L4 断言强度引用 |
| 恒等式简化登记 | `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（P2-MA3-024 watch-only） | ✅ 复用（本切片断言③ 引用，不新建） |
| bank-stmt-status 字典矛盾 | P1-MA3-028（done R2.3） | ✅ 已修复，本切片不复核 |

### 4.3 本切片需求视角差异增量（MA2 未覆盖）

| 差异点 | MA2 视角 | RC 视角（需求契约） | 本切片裁决 |
|---|---|---|---|
| 对方账号匹配维度 | MA2 状态机维度无此对象 | UC-FIN-09 断言②/UC-FIN-14 断言② 明确要求「对方账号 模糊匹配」为 4 维度之一 | **P1-RC-004**（维度缺失，§5 详述） |
| 下月自动红冲 | MA2 未审查（状态机维度无调度对象） | UC-FIN-09 断言④/UC-FIN-14 断言⑤ 明确要求「下月红冲」/「下月初**自动**红冲」 | **P1-RC-005**（自动调度完全缺失，§5 详述） |
| 导入幂等 dedup key | MA2 未审查 | UC-FIN-09/14 断言① 要求 `(fundAccount, statementDate, bankTxnCode)` 三元组 | **P2-RC-001**（key 偏离 + 范围仅最近一条 statement，主路径 OK，§5 详述） |
| valueDate 列 | MA2 未审查 | UC-FIN-09/14 断言② 要求 `valueDate±N天` | **P2-RC-002**（列缺失，transactionDate 代理，§5 详述） |
| 多币种调整 | MA2 §5.12 仅述通用多币种路径 | UC-FIN-09/14 L1 静默；L2 规则 10 要求汇兑损益 | **P2-RC-003**（exchangeRate=ONE 硬编码，主路径单币种 OK，§5 详述） |

### 4.4 E2E 行为证据（复用）

- `tests/e2e/business-actions/fin-bank-recon.action.spec.ts`（282 行，3 case）：银行对账三态状态机（generate→DRAFT→post→POSTED 产 BANK_RECON_ADJ→reverse→CANCELLED 红冲）浏览器层全栈可达（per plan baseline，断言强度引用 MA5/A5.6 评级）。E2E setup 含 1 条 UNMATCHED CREDIT 行触发调整凭证（Dr 1002 / Cr 2240OTHER）。
- 本切片无新 E2E 探针需求（存疑点见 §7）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 5.1 五级追踪矩阵（2 UC，每 UC 一行，不合并；D-01 断言重叠在报告记录）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-FIN-09** 银行对账与未达账项 | `use-cases.md:165` ①导入(bankTxnCode 幂等去重) ②自动勾对(金额+反向方向+valueDate±N天+对方账号 模糊匹配) ③恒等式(银行余额+在途==账面余额+未达,差额=0 才可 RECONCILED) ④未达→调整凭证(BANK_RECON_ADJ),下月红冲 | `bank-reconciliation.md §业务规则`（设计参考；schema 补注 :139-150 记录实现偏离：bankTxnCode→refNo / valueDate 列缺失 / 物化视图未采用 / 恒等式简化 / exchangeRate=ONE / auto-reverse config 无消费——**L2 部分偏离已记录但未经 §4 人工批准，冲突以 L1 为准**） | ①`BankStatementImporter.importStatement:44-107` + assertNoDuplicates:147-178（refNo 优先回退组合键，dedup key 偏离）+ findStatementIdByAccount:198-207（仅最近一条 statement 范围）②`BankStatementMatcher.autoMatch:41-74` + `BankLedgerQuery.findCandidates:39-84`（subjectId+dcDirection+amount+日期窗口，**无对方账号过滤** + txnDate 非 valueDate）③`BankReconciliationBuilder.generate:47-117`（2 项简化恒等式 :67-75，不平衡抛异常 :77-82，期间 CLOSED 守卫 :191-201）④`BankReconciliationBuilder.post:119-131`→`BankReconAdjustmentVoucherBuilder.post:58-95`（BANK_RECON_ADJ :81，exchangeRate=ONE :86）+ `BankReconAdjAcctDocProvider.createFacts:51-70`；手动 reverse:133-142 存在，**自动红冲无 scheduler 消费**（config key :289 仅定义） | ①`TestErpFinBankStatementImport`（6，强：refNo/composite/strict/cross-account/happy）②`TestErpFinBankStatementMatch`（7，强：唯一 MATCHED/多 SUSPENSE/无 UNMATCHED/同向拒绝/manual/occupied）③`TestErpFinBankReconciliation`（5，强：平衡/不平衡/CLOSED 拒绝/无未达不产凭证/post+reverse）④调整凭证测试**仅断言存在性+计数，未断言行级**；跨多条 statement 去重/对方账号/valueDate/多币种/自动红冲**无测试** | ①主路径（同 statement refNo 去重）行为已证实；跨多条 statement 范围缺口静态确认 ②三态判定行为已证实（MA5 深/匹配）；对方账号维度缺失静态确认 ③恒等式守卫行为已证实 ④调整凭证生成+手动红冲行为已证实（MA2 解耦复用）；自动红冲缺失静态确认 | **P1**（断言②对方账号缺失→P1-RC-004；断言④下月红冲自动缺失→P1-RC-005；断言①dedup 偏离→P2-RC-001；断言②valueDate→P2-RC-002；断言③恒等式简化→复用 P2-MA3-024。取最高=P1） |
| **UC-FIN-14** 银行对账与未达账项 | `use-cases.md:269` ①导入幂等(fundAccount,statementDate,bankTxnCode)去重,重复报错 ②自动勾对(金额,反向方向,valueDate±N天,对方账号)→MATCHED/UNMATCHED/SUSPENSE ③恒等式差额=0 才可 RECONCILED 否则抛异常 ④RECONCILED+未达→调整凭证(BANK_RECON_ADJ) ⑤下月初**自动**红冲(跨期还原) ⑥与 AR/AP 解耦(ErpFinReconciliation) | 同 UC-FIN-09（L2 单一银行对账机制，无 09/14 之分；schema 补注同上） | 断言①-⑤同 UC-FIN-09 调用链；断言⑥独立实体/表/BizModel（ErpFinBankStatement vs ErpFinReconciliation） | 同 UC-FIN-09（4 测试类 19 @Test + E2E） | 断言⑥解耦行为已证实（MA2 :48,223,365）；断言①-⑤同 UC-FIN-09 | **P1**（断言②对方账号→P1-RC-004；断言⑤自动红冲→P1-RC-005；断言①dedup→P2-RC-001；断言②valueDate→P2-RC-002；断言③恒等式→P2-MA3-024；断言④调整凭证生成→接受；断言⑥解耦→接受。取最高=P1） |

### 5.2 分级判据命中明细（§2）

#### P1-RC-004 — UC-FIN-09/14 自动勾对"对方账号模糊匹配"维度缺失

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」
- **三源对照**：
  - L1（`use-cases.md:172,279`）：逐字「自动勾对: 金额 + 反向方向 + valueDate±N天 + **对方账号** 模糊匹配」/「按 (金额, 反向方向, valueDate±N天, **对方账号**) 模糊匹配」——**对方账号是 4 维度之一**。
  - L2（`bank-reconciliation.md §业务规则 2`：98）：逐字「按 `(amountSource, direction 反向, valueDate ± N 天, counterpartyAccount)` 模糊匹配」——**L2 与 L1 一致，均要求对方账号维度**。
  - L3（`BankLedgerQuery.findCandidates:39-84`）：候选过滤为 `subjectId + dcDirection + amount + voucherId(日期窗口)`（:63-72），**无 counterpartyAccount 过滤**；ORM `ErpFinBankStatementLine`（`app-erp-finance.orm.xml:1133-1173`）**无 counterpartyAccount/counterpartyName 列**（仅 refNo/description），`ErpFinVoucherLine` 亦无对方账号列承载。
- **运行时影响**：自动勾对仅按金额+方向+日期匹配。当两笔交易同额同日但**对方账号不同**时：(a) 若账面仅 1 笔候选 → 错误 MATCHED（银行行勾对到错误对方账号的凭证行）；(b) 若账面多笔候选 → SUSPENSE（过度挂起，需人工干预）。场景 (a) 是**错误匹配**（非仅精度下降），影响对账准确性。错误匹配可经 manualMatch 取消重勾（可逆），且余额恒等式下游兜底（聚合错误会触发不平衡拒绝），故非 §2 P0④「活跃数据破坏」（与 P0 示例「凭证重复过账」的默认触发面不同）。
- **严重性**：major（匹配正确性破坏，但可逆 + 下游兜底；需同额同日不同对方账号的前置条件）
- **未文档化**：`bank-reconciliation.md` schema 补注（:139-150）记录了 bankTxnCode→refNo / valueDate 缺失 / 物化视图未采用 / 恒等式简化 / exchangeRate=ONE / auto-reverse 等偏离，但**未记录对方账号匹配维度缺失**——属**未文档化的静默缺口**（无 §4 三判据人工批准）。
- **修复义务**：§5 Q4=(a) 强制实现，禁止方案 B。经 MR1（R1.0 展开为 RC-R1.n）。修复触及 **ORM 结构变更**（ErpFinBankStatementLine + ErpFinVoucherLine 增 counterpartyAccount 列）+ 匹配算法（BankLedgerQuery.findCandidates 增对方账号过滤）→ **须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，ORM 结构变更类）。
- **与既有 finding 关系**：grep arm-index finance 银行对账域无同控制点 finding。**新建 P1-RC-004**（§7 裁决详见 §6）。

#### P1-RC-005 — UC-FIN-09/14 下月初自动红冲缺失（config key 无 scheduler 消费）

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失」（"自动"红冲的调度功能完全缺失）
- **三源对照**：
  - L1（`use-cases.md:176,288`）：逐字「未达账项 → 生成调整凭证(businessType=BANK_RECON_ADJ), **下月红冲**」/「下月初**自动红冲**(跨期还原)」——UC-FIN-14 断言⑤ 显式「自动」。
  - L2（`bank-reconciliation.md §业务规则 6`：108 + schema 补注 :150）：逐字「posted 联动：调节表 RECONCILED 时若存在未达账项,生成调整凭证(isReversed=false),**下月初自动红冲**(跨期还原)」+「`erp-fin.bank-recon-auto-reverse-next-month`（默认 true，实际红冲由定时任务触发，本计划交付 `reverse` 入口 + 手动可触发）」——L2 显式承认调度未接线。
  - L3：config key `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH` 定义于 `ErpFinConstants.java:289`；手动红冲 `BankReconciliationBuilder.reverse:133-142` + `BankReconAdjustmentVoucherBuilder.reverse:97-102` 存在；但 `rg "CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH\|auto-reverse\|AutoReverse\|autoReverse" module-finance` **仅 1 命中（定义本身）**——**无 scheduler/cron/Job bean/IJobInvoker 消费该 config key**。`scheduler.yaml`（`app-erp-all/.../nop/job/conf/scheduler.yaml`）无银行对账红冲条目（仅 AR/AP auto-recon 等已迁移至 nop-batch 的作业）。
- **运行时影响**：未达账项调整凭证（BANK_RECON_ADJ）生成后持久存在，**不会在下月初自动红冲**。临时调整凭证本应跨期还原（下月红冲恢复正确余额），缺失自动红冲导致：调整凭证持续挂账 → 银行存款科目 + 未达调整对方科目余额潜在错报（资产/负债错报）。可由出纳手动触发 `reverse()` 补救（手动入口存在），故非 §2 P0④「活跃数据破坏」（非默认活跃路径破坏，需人工遗漏触发）。config key 默认 true 但无消费——**运维以为自动生效但实际不执行**，属隐性失效。
- **严重性**：major（会计余额潜在错报，但手动补救存在 + 需人工遗漏触发）
- **修复义务**：§5 Q4=(a) 强制实现，禁止方案 B。经 MR1（R1.0 展开为 RC-R1.n）。修复 = 接线 scheduler（nop-batch job.yaml 注册下月初红冲作业 + 消费 config key 门控 + 批量调 `BankReconciliationBuilder.reverse`）→ 纯调度接线 + BizModel 调用，按 roadmap 预授权类目（代码逻辑修复）可自动执行，**不触发 §5 ask-first**（不触及 ORM/会计过账核心路径 VoucherFact/PostingProcessor，仅调用既有 reverse 入口）。
- **与既有 finding 关系**：grep arm-index 无"银行对账自动红冲"主题 finding。**新建 P1-RC-005**（§7 裁决详见 §6）。

### 5.3 P2 命中明细（新登记 + 复用）

#### P2-RC-001 — 导入幂等 dedup key 偏离 + 跨多条 statement 去重范围（UC-FIN-09/14 断言①）

- **命中判据**：§2 **P2①**「需求契约的次要验收标准未完全满足（主路径 OK，边界场景弱）」
- **三源对照**：
  - L1（`use-cases.md:171,276`）：UC-FIN-09「bankTxnCode 幂等去重」/ UC-FIN-14「以 `(fundAccount, statementDate, bankTxnCode)` 去重」。
  - L3（`BankStatementImporter.assertNoDuplicates:147-178`）：refNo 优先（全局 + 最近一条 statement 范围），缺失回退 `(transactionDate, amount, dcDirection)`；`findStatementIdByAccount:198-207` `limit 1` statementDate DESC——**仅查最近一条 statement**。
  - 偏离：(a) key = refNo（银行参考号，语义近似 bankTxnCode）或组合键，非 L1 三元组；(b) 范围仅最近一条 statement，**跨多条 statement 的重复 refNo 不检出**。
- **运行时影响**：主路径（同 statement / 最近一条 statement 范围内的重复导入拒绝）OK；边界场景（refNo 跨多条 statement 重复）保护失效，但银行 refNo 通常全局唯一（跨 statement 重复罕见），且余额恒等式下游兜底。`bank-reconciliation.md:147` schema 补注记录 key 偏离（refNo 优先，不新增 bankTxnCode 列）但**未经 §4 人工批准**。
- **修复义务**：P2 登记不强制。经 MR1 successor 或独立 plan（修复触及 ORM 加 bankTxnCode 列须 ask-first；或改 findStatementIdByAccount 去除 limit 1 全量扫描同 account 所有 statement 的 refNo，纯代码修复可自动执行）。

#### P2-RC-002 — valueDate→transactionDate 简化（UC-FIN-09/14 断言②）

- **命中判据**：§2 **P2①**「需求契约的次要验收标准未完全满足（主路径 OK，边界场景弱）」
- **三源对照**：
  - L1（`use-cases.md:172,279`）：「valueDate±N天」。
  - L3（`BankLedgerQuery.findCandidates:45-46` + `BankStatementMatcher.autoMatch:59`）：窗口基于 `txnDate` = `line.getTransactionDate()`；ORM `ErpFinBankStatementLine`（:1133-1173）**无 valueDate 列**。
- **运行时影响**：日期窗口 ±N 天功能存在（主路径 OK），仅用 transactionDate（银行记账日）代理 valueDate（资金实际到账日）。对同期同日交易无影响；跨期未达判定依据（valueDate，per `bank-reconciliation.md:43`）弱化，但未达账项经 UNMATCHED 状态独立承载（不依赖 valueDate 列）。`bank-reconciliation.md:139-149` schema 补注隐含记录（物化视图/列简化）。
- **修复义务**：P2 登记不强制。修复触及 ORM 加 valueDate 列须 ask-first。

#### P2-RC-003 — 多币种调整 exchangeRate=ONE 硬编码（UC-FIN-09/14 L1 静默 / L2 规则 10）

- **命中判据**：§2 **P2①**「需求契约的次要验收标准未完全满足（主路径 OK，边界场景弱）」（L1 静默，L2 规则 10 要求）
- **三源对照**：
  - L1（UC-FIN-09/14）：**静默**（未提及银行对账多币种；UC-FIN-12 通用多币种属 A1.1 切片）。
  - L2（`bank-reconciliation.md §业务规则 10`：116）：逐字「外币账户对账时,未达账项调整凭证需考虑汇兑损益(关联 businessType=EXCHANGE_GAIN_LOSS 已有字典)」。
  - L3（`BankReconAdjustmentVoucherBuilder.post:86`）：`event.setExchangeRate(BigDecimal.ONE)`——**硬编码 rate=1**。
- **运行时影响**：单币种银行账户（主路径）rate=1 正确；外币银行账户（边界）rate=1 致 amountFunctional=amountSource（无 FX 折算）→ GL 本位币余额错报。与 P1-RC-002（A1.1，汇率缺失守卫）+ P1-MA3-039（FX 折算缺失）同主题但**不同控制点**（BankReconAdjustmentVoucherBuilder:86 硬编码 vs ErpFinPostingProcessor:537 回退）。
- **修复义务**：P2 登记不强制。修复触及会计过账逻辑（BankReconAdjustmentVoucherBuilder 解析资金账户汇率 + 汇兑损益科目）须 ask-first。

### 5.4 复用既有 finding

#### 复用 P2-MA3-024 — 余额调节恒等式简化 2 项 vs spec 4 项（UC-FIN-09/14 断言③）

- **既有 finding**：`P2-MA3-024`（owner doc vs code drift，余额调节恒等式简化，watch-only，`2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`）。
- **本切片复用判定**：UC-FIN-09/14 断言③「银行余额 + 在途 == 账面余额 + 未达」4 项恒等式；实现 `BankReconciliationBuilder.generate:67-75` 为 2 项简化（`statementBalance − bookBalance = bankCreditUnrecorded − bankDebitUnrecorded`，在途=0 Non-Goal per `bank-reconciliation.md:149`）。**同根因同控制点**（恒等式简化），**复用 P2-MA3-024**，不新建编号（§7 裁决）。arm-index P2-MA3-024 行追加 RC 交叉引用注记。

### 5.5 接受类结论汇总

| UC 断言 | 接受依据 |
|---|---|
| UC-FIN-09 断言①（去重主路径） | refNo + 组合键去重 + 非 BANK 拒绝 + strict-refno + 跨账号允许——6 强断言覆盖主路径（P2-RC-001 仅覆盖边界） |
| UC-FIN-09 断言②（勾对三态+方向） | MATCHED/SUSPENSE/UNMATCHED 三态 + 反向方向 + manualMatch + occupied 排除——7 强断言覆盖（P1-RC-004 仅对方账号维度缺失） |
| UC-FIN-09 断言③（恒等式守卫） | 平衡/不平衡拒绝/期间 CLOSED 拒绝——3 强断言覆盖（P2-MA3-024 仅简化公式，守卫存在） |
| UC-FIN-09 断言④（调整凭证生成） | BANK_RECON_ADJ 调整凭证生成 + 借贷平衡（Dr/Cr 对）+ 手动红冲——存在性强断言覆盖（P1-RC-005 仅自动红冲缺失；行级细节交 §7 存疑点） |
| UC-FIN-14 断言④（RECONCILED+未达→调整凭证） | 同上 |
| UC-FIN-14 断言⑥（与 AR/AP 解耦） | 独立实体/表/BizModel，MA2 :48,223,365 证实 |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` finance 银行对账同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本切片 finding 关系 | 裁决 |
|---|---|---|---|
| P2-MA3-024 余额调节恒等式简化 | owner doc vs code drift（恒等式 2 项简化） | 同根因同控制点（恒等式简化） | **复用**（UC-FIN-09/14 断言③ 引用，追加 RC 交叉引用注记，不新建） |
| P1-MA3-028 bank-stmt-status 字典矛盾 | doc 字典自相矛盾 | 已 done R2.3，不同控制点（字典 vs 匹配/红冲） | 不相关 |
| P1-MA7-001 ErpFinVoucherBillR 缺索引 | 索引完整性 | 已 done R3.6，不同控制点（索引 vs 匹配/红冲） | 不相关 |
| P2-MA4-014 badge 漂移 | view.xml drift | view.xml 维度，非需求契约维度 | 不相关 |
| P1-MA2-002/009 多币种 FX | 通用多币种凭证路径 | 覆盖多币种 FX 主题（UC-FIN-12），与 P2-RC-003 同主题不同控制点 | P2-RC-003 交叉引用 |
| P1-MA3-039 persistVoucher amountSource=amountFunctional | FX 折算缺失（通用模板） | 与 P2-RC-003 同主题不同控制点（BankReconAdjustmentVoucherBuilder:86 vs ErpFinPostingProcessor） | P2-RC-003 交叉引用 |
| P1-RC-002（A1.1）汇率缺失守卫 | rate 缺失→回退 1（引擎层） | 与 P2-RC-003 同主题不同控制点（引擎回退 vs bank-recon 硬编码） | P2-RC-003 交叉引用 |

### 6.2 新建 finding 裁决

| Finding ID | UC | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|---|---|---|---|---|
| **P1-RC-004** | UC-FIN-09/14 断言② | 自动勾对对方账号匹配维度缺失 | arm-index 无任何 finding 覆盖银行对账匹配维度（既有均为字典/索引/badge/FX 主题）；L1+L2 均要求该维度，L3 静默缺失（未文档化） | **新建** |
| **P1-RC-005** | UC-FIN-09 断言④/UC-FIN-14 断言⑤ | 下月自动红冲调度完全缺失 | arm-index 无"银行对账自动红冲"主题 finding；config key 定义无消费属隐性失效 | **新建** |
| **P2-RC-001** | UC-FIN-09/14 断言① | 导入幂等 dedup key 偏离 + 跨多条 statement 范围 | arm-index 无 dedup key/scope finding | **新建** |
| **P2-RC-002** | UC-FIN-09/14 断言② | valueDate→transactionDate 简化 | arm-index 无 valueDate 列 finding | **新建** |
| **P2-RC-003** | UC-FIN-09/14（L1 静默/L2 规则 10） | 多币种调整 exchangeRate=ONE | P1-RC-002/P1-MA3-039/P1-MA2-002/009 同主题不同控制点（引擎 vs bank-recon builder） | **新建**（交叉引用） |

### 6.3 双向可追溯

- **新 finding → arm-index**：P1-RC-004 / P1-RC-005 / P2-RC-001 / P2-RC-002 / P2-RC-003 将写入 `arm-index.md` RC 发现追踪分区（§7 归档纪律）。
- **finding → 修复**：2 P1 待 MR1 R1.0 展开为 RC-R1.n 修复行；3 P2 为 successor watch-only（本审计不实施修复）。
- **既有 finding 复用注记**：UC-FIN-09/14 断言③ 引用 P2-MA3-024（不新建编号）；P2-RC-003 交叉引用 P1-RC-002/P1-MA3-039/P1-MA2-002/009。

---

## 7. 静态存疑点清单（供 MA4 A4.1 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行；无则注明。

1. **UC-FIN-09/14 断言② 对方账号缺失致错误 MATCHED 的实际触发率**：L3 静态确认 `BankLedgerQuery.findCandidates` 无对方账号过滤，但「同额同日不同对方账号且账面仅 1 候选」的实际发生率属运行时数据普查——交 MA4 A4.1（业财展开器，Deps=MA1 done）按需追加 A4.1.n 实体行展开（构造同额同日不同 partner 的 voucher line + bank line，运行 autoMatch 观察是否错误 MATCHED）。
2. **UC-FIN-09/14 断言④ 调整凭证行级 Dr/Cr/科目/金额正确性**：L4 仅断言凭证存在性 + billR 计数，`BankReconAdjAcctDocProvider.createFacts:51-70` 产出的 2-4 条 VoucherFact 的行级（Dr bankSubject / Cr adjSubject / 金额 = bankCredit 或 bankDebit）正确性无测试断言——交 MA4 A4.1 按需展开（运行 post 后断言 ErpFinVoucherLine 行级 subjectCode/dcDirection/debitAmount/creditAmount）。
3. **UC-FIN-09/14 断言① 跨多条 statement refNo 重复的实际检出**：L3 静态确认 `findStatementIdByAccount:198-207` 仅查最近一条 statement，但「refNo 跨多条 statement 重复 + 漏检致重复入账」的实际影响需运行时构造多 statement 场景验证——交 MA4 A4.1 按需展开。
4. **UC-FIN-14 断言⑤ config key 默认 true 但无消费的运维认知**：L3 静态确认无 scheduler 消费 `erp-fin.bank-recon-auto-reverse-next-month`，但「运维是否误以为自动红冲生效」属部署面普查——交 MA4 A4.1 按需展开（核查 scheduler.yaml / nop-batch job.yaml 全量 + 部署文档）。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（P1-RC-004/005 均为 P1），按 §10 **不触发 MR0**。两 finding 经 MR1 批量修复通道（R1.0 展开为 RC-R1.n）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-02 实测）

> 本审计为**只读审计**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)`）。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382 | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

本切片声明与既有 MA2 报告的差异增量：

- **复用 MA2/MA3/MA4/MA5 已证实行为**（不重新核实）：
  - `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`：银行对账为独立子系统（ErpFinBankStatement vs ErpFinReconciliation），与 AR/AP 核销解耦，Non-Goal 不合并。本切片 UC-FIN-14 断言⑥ 的 L5 行为证据直接引用。
  - `2026-07-28-1510-arm-ma3-design-completeness-scan.md:72,195`：bank-reconciliation.md 存在（L2 设计参考）。
  - `2026-07-29-0430-arm-ma4-finance-mfg-view-xml-drift.md:97,116`：P2-MA4-014 badge 漂移波及 BankReconciliation/BankStatement（view.xml 维度，非本切片需求契约维度）。
  - `2026-07-29-1430-arm-ma5-finance-test-coverage.md:74,92`：4 测试类 19 @Test 覆盖评级"✅ 深 / ✅ 匹配"（L4 断言强度引用）。
  - `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（P2-MA3-024）：余额调节恒等式简化（watch-only，本切片断言③ 复用）。
- **本切片只补的需求视角差异**（MA2 未覆盖）：
  1. **UC-FIN-09/14 断言② 对方账号匹配维度缺失**（P1-RC-004）：MA2 状态机维度无此对象（匹配算法细节），L1+L2 均要求该维度，L3 静默缺失（未文档化）。本切片从需求契约 4 维度视角首次定级。
  2. **UC-FIN-09 断言④/UC-FIN-14 断言⑤ 下月自动红冲缺失**（P1-RC-005）：MA2 状态机维度无调度对象，config key 定义无消费属隐性失效。本切片从需求契约"自动"红冲视角首次定级。
  3. **UC-FIN-09/14 断言① dedup key 偏离 + 跨多条 statement 范围**（P2-RC-001）：MA2 未审查导入幂等细节。本切片从 L1 三元组视角定级为 P2（主路径 OK）。
  4. **UC-FIN-09/14 断言② valueDate→transactionDate 简化**（P2-RC-002）：MA2 未审查日期窗口列选择。
  5. **UC-FIN-09/14 多币种调整 exchangeRate=ONE**（P2-RC-003）：L1 静默，L2 规则 10 要求；MA2 §5.12 仅述通用多币种路径。本切片从 bank-recon 调整凭证 builder 视角定级为 P2（交叉引用 P1-RC-002/P1-MA3-039/P1-MA2-002/009）。
- **报告校正项**：`2026-07-06-use-case-implementation-audit.md:116,121` 将 UC-FIN-09 标 ✅，引用 **`AutoReconJob` 类**——该引用**失实**：`rg "AutoReconJob"` 证实全仓的 `ErpFinAutoReconJob` 是 **AR/AP 自动核销**作业（`IErpFinReconciliationBiz.runAutoReconciliation`，不同子系统），且已于 2026-07-18 plan `2026-07-18-1600-1-batch-migration-phase-1.md:278`（batch 迁移）**删除**。银行对账子系统无任何 `AutoReconJob`/`AutoReconciliationEngine`（银行对账仅 `BankStatementMatcher.autoMatch` 手动/触发式勾对，无定时自动勾对作业）。该 2026-07-06 审计的"✅"部分建立在失实引用上，本审计已重新核验并给出 P1 残留结论（P1-RC-004/005）。属历史审计证据校正，不重开 2026-07-06 报告（audit-remediation 范畴，本 RC 仅记录差异）。
- **MA2 finding 复核无升级**：本切片复核 MA2 已登记的银行对账旁证（解耦行为），运行时行为与 MA2 登记一致，**无升级 P0**。

---

## 10. Verdict

**Verdict: passes requirement-compliance audit**（带 2 项 P1 残留 + 3 项 P2 新登记 + 1 项 P2 复用 + 接受类验收标准族）

**审查范围**：UC-FIN-09/14 共 2 UC 五级追踪矩阵（L1-L5）+ 每 UC 符合性结论（§2 判据）+ 与 arm-index 衔接（§7 复用/新增裁决）+ 静态存疑点清单（供 MA4 A4.1 展开）+ 过程纪律自检 + 与 MA2 差异增量声明。

**接受类**：UC-FIN-09 断言①（去重主路径）/ 断言②（勾对三态+方向）/ 断言③（恒等式守卫）/ 断言④（调整凭证生成）；UC-FIN-14 断言④（RECONCILED+未达→调整凭证）/ 断言⑥（与 AR/AP 解耦）—— L3-L5 全证据一致。

**P1 残留**：P1-RC-004（对方账号匹配维度缺失）/ P1-RC-005（下月自动红冲缺失）→ MR1（R1.0 展开为 RC-R1.n）。P1-RC-004 修复触及 ORM 结构变更（加 counterpartyAccount 列）须 ask-first + 独立 plan-audit（§5）；P1-RC-005 修复为调度接线（纯代码逻辑，预授权自动执行）。

**P2 新登记**：P2-RC-001（dedup key/scope 偏离）/ P2-RC-002（valueDate 简化）/ P2-RC-003（多币种 rate=ONE）→ successor watch-only。

**P2 复用**：P2-MA3-024（恒等式简化，追加 RC 交叉引用）。

**P0**：无。不触发 MR0。

**剩余风险**：见 §7 静态存疑点清单（4 项交 MA4 A4.1 运行时展开）。
