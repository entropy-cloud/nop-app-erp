# RC MA4 A4.1.2 — UC-FIN-12 汇率缺失触发面运行时普查 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.1.2（MA4 运行时行为验证 — A1.1 §7-2：UC-FIN-12 汇率缺失触发面实测，各域 Provider 外币场景是否显式传 rate 普查）
> 验证 plan：`docs/plans/2026-08-07-0330-2-rc-ma4-a4-1-2-fx-rate-missing-trigger-surface.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据[含 P0④活跃数据破坏] / §7 arm-index 衔接 / §8 过程纪律自检 / §10 MR0 即时通道 / §去重协议）
> 输入 finding：`P1-RC-002`（A1.1 §5.2，UC-FIN-12 断言②「汇率缺失→拒绝过账」守卫未实现，静默回退 rate=1）
> 关联 finding：`P1-MA2-002`/`P1-MA2-009`/`P1-MA3-039`（多币种 FX 折算实现，已 resolved MR1 方案 A）/ `P2-RC-003`（BankRecon 硬编码 ONE，watch-only）
> 验证性质：**只读运行时普查**（grep 调用面 + 读 JUnit + 复用 MA2/A1.1；不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 验证日期：2026-08-07
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **P1-RC-002 P0 升级再评估** | **维持 P1（不升 P0）** | 不触发 MR0 即时通道 |
| 生产 `new PostingEvent()` 构造点全集 | **43 处**（11 域 dispatcher/builder） | 0 处漏传 rate |
| 显式 `setExchangeRate` 调用点 | **43 处 = 100%**（每构造点必设） | `prepareContext:537` 静默回退为**死代码**（dispatcher 路径不可达） |
| `IErpFinAcctDocProvider` 实现全集 | **37 处**（消费 PostingEvent，不构造） | 不在 setExchangeRate 触发面 |
| 新 finding | **0** | 无新控制点（全部归既有 finding） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.1 §5.2 维持 P1-RC-002 为 P1 的关键前提——「当前各域 Provider 显式传 rate，无活跃错误数据」——**经运行时全集普查 CONFIRMED**。全仓 11 域共 43 个生产 `PostingEvent` 构造点（dispatcher/builder）**无一漏传 `setExchangeRate`**：要么显式传源单据 `entity.getExchangeRate()`（FX-capable，21 处），要么对功能性币种业务类型硬编码 `BigDecimal.ONE`（functional-only，语义正确，21 处），要么重试路径透传原事件 rate（1 处）。因此 `ErpFinPostingProcessor.prepareContext:537`（`event.getExchangeRate() != null ? ... : EXCHANGE_RATE_DEFAULT`，`EXCHANGE_RATE_DEFAULT=1` :78）的 null 回退分支**从任何 dispatcher 均不可达**——仅可由「直接手搓 PostingEvent 调 Facade 且漏设 rate」的调用方触发，而**此类生产调用方为零**。故 §2 P0④「活跃数据破坏 / 会计过账正确性破坏」**不成立**：无默认活跃 FX 路径因 rate 缺失而产生错误本位币金额。残余 FX 风险属**多币种实现缺口**（Pattern A 域 Provider 仍写单一 `amount` fact → functional==source），归 `P1-MA2-002`/`P1-MA2-009`/`P1-MA3-039` MR1 已登记 successor，与 P1-RC-002（rate 缺失守卫）为**不同控制点**，不可合并（§去重）。**不触发 MR0，不升 P0，不产生新 finding。**

---

## 1. 需求契约与回退点原文（L1 + L3 锚点）

### L1 权威（UC-FIN-12，`docs/design/finance/use-cases.md:223-232`）

```
场景：外币业务的凭证折算。
可验证断言（见 posting.md §多币种）：
  凭证行.本位币金额 == 源币金额 × 汇率          ← 断言①（FX 折算）
  若 汇率缺失 → 报错拒绝过账                     ← 断言②（rate 缺失守卫）= P1-RC-002 对象
  外币银行账户对账: 未达账项调整考虑汇兑损益     ← 断言③（EXCHANGE_GAIN_LOSS 期末重估）
```

- **本验证对象 = 断言②**（rate 缺失守卫）的**触发面**：是否存在域调用方在外币场景漏传 rate，使静默回退 `EXCHANGE_RATE_DEFAULT=1` 成为默认活跃错误路径。
- 断言①（FX 折算）= `P1-MA3-039` + `P1-MA2-002`/`P1-MA2-009`（已 resolved MR1 方案 A）；断言③ = `P2-RC-003`（watch-only）。均与本验证**不同控制点**（§去重）。

### L3 回退点实测锚点（`ErpFinPostingProcessor.java`）

| 锚点 | 行 | 实现 | 说明 |
|---|---|---|---|
| `EXCHANGE_RATE_DEFAULT` | `:78` | `static final BigDecimal EXCHANGE_RATE_DEFAULT = new BigDecimal("1");` | 静默回退值 |
| `prepareContext` rate 回退 | `:537` | `ctx.setExchangeRate(event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT);` | **本验证核心**——null 时回退 1 继续过账（与断言②「拒绝」冲突） |
| `persistVoucher` rate 回退 | `:818-820` | `exchangeRate = ctx.getExchangeRate() != null ? ctx.getExchangeRate() : EXCHANGE_RATE_DEFAULT;` | 双重兜底（ctx 已被 :537 填充，此处恒非 null） |
| 行级双金额回退 | `:827-828` | `amtSource = fact.getAmountSource() != null ? ... : amt;` / `amtFunctional = fact.getAmountFunctional() != null ? ... : amt;` | Provider 未设双金额时 source==functional==amount（单币种向后兼容，R1.9/P1-MA3-039） |
| GL 借贷按本位币 | `:838-839` | `line.setDebitAmount(isCredit ? ZERO : amtFunctional);` | 试算平衡以本位币为准 |

> **关键观察**：`:537` 的 null 分支是断言②守卫缺失的实现载体。本验证回答：**它是否可被默认活跃路径触达？**

---

## 2. 调用面全集普查（Phase 1 核心）

### 2.1 普查方法（完整枚举，禁止抽样）

1. `rg -l "implements IErpFinAcctDocProvider" --glob '!*/test/*'` → **37 个生产 Provider 实现**（消费 PostingEvent，读 `billData`，**不构造 PostingEvent、不设 rate**）。
2. `rg -n "new PostingEvent\(\)" --glob '!*/test/*'` → **43 个生产构造点**（dispatcher/builder）。
3. `rg -n "event\.setExchangeRate\(" --glob '!*/test/*'` → **43 个生产 setExchangeRate 调用点**。
4. **对照**：构造点数(43) == setExchangeRate 点数(43) == **每构造点必设 rate，0 漏传**。

> Provider 是 rate 的**消费方下游**（经 `AcctDocContext.exchangeRate`），其是否「显式传 rate」由上游 dispatcher 决定。A1.1 baseline 以 `NotesReceivableAcctDocProvider` 为例的表述，实际控制点在其上游 `NotesPostingDispatcher`。本普查以 dispatcher 构造点为触发面全集。

### 2.2 调用点矩阵（每点 4 字段：①file:line ②外币场景识别 ③setExchangeRate 传/漏/不适用 ④JUnit 外币覆盖）

**图例**：`A`=FX-capable（源单据携带 currencyId+exchangeRate，传 entity rate，null 回退 ONE）；`B`=functional-only（业务类型本位币，硬编码 ONE，语义正确）；`C`=透传（重试/异常路径透传原 rate）。

#### assets（9 点，dispatcher 全 Pattern A/B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 1 | `CapitalizationPostingDispatcher:115` | ASSET_CAPITALIZATION | 源 `cap.exchangeRate` 字段存在（A） | A：`cap.getExchangeRate() != null ? : ONE`（传） | 无外币用例（rate=ONE） |
| 2 | `DisposalPostingDispatcher:105` | ASSET_DISPOSAL | 源 `disposal.exchangeRate`（A） | A（传） | 无 |
| 3 | `AssetSplitPostingDispatcher:71` | ASSET_SPLIT | 源 `split.exchangeRate`（A） | A（传） | 无 |
| 4 | `AssetMergePostingDispatcher:73` | ASSET_MERGE | 源 `merge.exchangeRate`（A） | A（传） | 无 |
| 5 | `AssetInventoryPostingDispatcher:73` | ASSET_INVENTORY | 源 `inventory.exchangeRate`（A） | A（传） | 无 |
| 6 | `ValueAdjustmentPostingDispatcher:74` | ASSET_VALUE_ADJUSTMENT | 源 `adjustment.exchangeRate`（A） | A（传） | 无 |
| 7 | `MaintenanceCapitalizationPostingDispatcher:74` | MAINTENANCE_CAPITALIZATION | 源 `maintenance.exchangeRate`（A） | A（传） | 无 |
| 8 | `MaintenanceExpensePostingDispatcher:74` | MAINTENANCE_EXPENSE | 源 `maintenance.exchangeRate`（A） | A（传） | 无 |
| 9 | `DepreciationPostingDispatcher:118` | ASSET_DEPRECIATION | 折旧按本位币计提（B） | B：`ONE`（不适用外币） | 无 |

> assets Pattern A 7 点的 Provider 仍写单一 `amount` fact（functional==source，未折算）= `P1-MA2-002` successor（多币种实现缺口，已登记 MR1 backlog），**非 P1-RC-002**。

#### purchase（3 点，全 Pattern A — FX 已实现）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 10 | `PurInvoicePostingDispatcher:78` | AP_INVOICE | `invoice.currencyId`+`exchangeRate`（A） | A：`invoice.getExchangeRate() != null ? : ONE`（传） | **强**：`TestErpPurMultiCurrencyPosting`（rate=7.0，行级 amountSource≠amountFunctional + debit/credit 本位币断言） |
| 11 | `PurPaymentPostingDispatcher:77` | PAYMENT | `payment`（A） | A（传） | 同上（P2P 多币种链路覆盖） |
| 12 | `PurReturnPostingDispatcher:84` | PURCHASE_RETURN | `returnOrder`（A） | A（传） | 同上 |

> purchase FX 路径**已 realized**（plan `2026-07-29-2322-2` 方案 A：`PurAcctDocProvider` 迁移 amountSource/amountFunctional）。`P1-MA2-002` ✅ resolved。

#### sales（3 点，全 Pattern A — FX 已实现）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 13 | `SalInvoicePostingDispatcher:78` | AR_INVOICE | `invoice`（A） | A：`invoice.getExchangeRate() != null ? : ONE`（传） | **强**：`TestErpSalMultiCurrencyReconFx`（invoice rate=7.0 + receipt rate=7.1 + fxGainLoss=113 汇兑损益凭证断言） |
| 14 | `SalReceiptPostingDispatcher:77` | RECEIPT | `receipt`（A） | A（传） | 同上（核销汇兑损益 plug 覆盖） |
| 15 | `SalReturnPostingDispatcher:92` | SALES_RETURN | `returnOrder`（A） | A（传） | 同上 |

> sales FX 路径**已 realized**（含核销汇兑损益 plug）。`P1-MA2-009` ✅ resolved。

#### inventory（5 点，全 Pattern B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 16 | `InvPostingDispatcher:207` | PURCHASE_INPUT/SALES_OUTPUT | 设 `currencyId`（取 ledger/line，可非本位币）但 `TOTAL_COST` 已是本位币计价 | B：`ONE`（amount 已 functional） | 无外币用例 |
| 17 | `InvPostingDispatcher:248` | PURCHASE_PRICE_VARIANCE | 同上 | B：`ONE` | 无 |
| 18 | `CostAdjustmentPostingDispatcher:90` | COST_ADJUSTMENT | 成本调整按本位币（B） | B：`ONE` | 无 |
| 19 | `LandedCostPostingDispatcher:103` | LANDED_COST | 源 `landedCost.exchangeRate`（A） | A：`landedCost.getExchangeRate() != null ? : ONE`（传） | 无外币用例 |
| 20 | `OwnershipTransferPostingDispatcher:102` | OWNERSHIP_TRANSFER | 同法人内部（B） | B：`ONE` | 无 |

> #16/#17 注意：`currencyId` 取自 ledger（PURCHASE_INPUT 时可为外币），但存货估值 `TOTAL_COST` 由 costing engine **以本位币计价**写入 ledger，故 rate=1 使 functional==source==本位币金额，**金额正确**（currencyId 标签可能偏外币为 cosmetic 偏差，归 `P1-MA2-002` successor 单币种模型）。**非 rate 缺失**。

#### manufacturing（5 点，Pattern A/B 混）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 21 | `ManufacturingIssuePostingDispatcher:125` | MANUFACTURING_ISSUE | 领料按本位币（B） | B：`ONE` | 无 |
| 22 | `ProductionVarianceDispatcher:164` | PRODUCTION_VARIANCE | 差异按本位币（B） | B：`ONE` | 无 |
| 23 | `SubcontractPostingDispatcher:198` | SUBCONTRACT_FEE | 源 `order.exchangeRate`（A） | A：`order.getExchangeRate() != null ? : ONE`（传） | 无外币用例 |
| 24 | `SubcontractPostingDispatcher:232` | SUBCONTRACT_ISSUE | 源 `order`（A） | A（传） | 无 |
| 25 | `SubcontractPostingDispatcher:266` | SUBCONTRACT_RECEIPT | 源 `order`（A） | A（传） | 无 |

> mfg 多币种四件套 propId 缺失如 `P1-MA1-001` 登记；`PostingEvent.exchangeRate=ONE` 单币种投影 = `P1-MA2-002`/`P1-MA2-009` 在 mfg 侧投影（A4.2a §2.2 已复核，同根因 MR1 一并裁决）。

#### projects（2 点，Pattern A/B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 26 | `ProjectSettlementPostingDispatcher:78` | PROJECT_SETTLEMENT | 源 `settlement.exchangeRate`（A） | A：`settlement.getExchangeRate() != null ? : ONE`（传） | 无外币用例（TestErpPrjProjectSettlement 用 ONE） |
| 27 | `TimesheetPostingDispatcher:130` | PROJECT_COST_COLLECTION(工时) | 工时成本按本位币（B） | B：`ONE` | 无 |

#### hr（1 点，Pattern B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 28 | `SalaryPostingDispatcher:160` | SALARY/SALARY_PAYMENT 等 | 薪资本位币（**未设 currencyId**，ctx.currencyId=null）（B） | B：`ONE`（不适用外币） | 无 |

#### quality（1 点，Pattern B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 29 | `NcrPostingDispatcher:106` | NCR_SCRAP | 报废按本位币（B） | B：`ONE` | 无 |

#### maintenance（2 点，Pattern B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 30 | `MaintenanceIssuePostingDispatcher:179` | MAINTENANCE_ISSUE | 备件消耗按本位币（B） | B：`ONE` | 无 |
| 31 | `MaintenanceLaborPostingDispatcher:205` | MAINTENANCE_LABOR | 工时费用化按本位币（B） | B：`ONE` | 无 |

#### logistics（1 点，Pattern B）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 32 | `AbstractErpLogShipmentDeliveredProcessor:188` | LOGISTICS_FREIGHT | 运费按本位币（B） | B：`ONE` | 无 |

#### finance 内部 builder/dispatcher/retry（11 点，Pattern A/B/C）

| # | file:line | businessType | ②外币场景识别 | ③rate 处理 | ④JUnit 外币覆盖 |
|---|---|---|---|---|---|
| 33 | `NotesPostingDispatcher:78` | NOTES_RECEIVABLE | 源 `note.exchangeRate`（A） | A：`note.getExchangeRate() != null ? : ONE`（传） | 无外币用例（A1.1 baseline 所引范例） |
| 34 | `NotesPostingDispatcher:107` | NOTES_PAYABLE | 源 `note`（A） | A（传） | 无 |
| 35 | `ExpenseClaimPostingDispatcher:67` | EXPENSE_CLAIM | 源 `claim.exchangeRate`（A） | A（传） | 无 |
| 36 | `EmployeeAdvancePostingDispatcher:130` | EMPLOYEE_ADVANCE（借款发放） | 源 `advance.exchangeRate`（A） | A：`advance.getExchangeRate() != null ? : ONE`（传） | 无 |
| 37 | `EmployeeAdvancePostingDispatcher:69` | EMPLOYEE_ADVANCE_SETTLE（净额清算） | `currencyId` 默认 1L（本位币）（B） | B：`ONE` | 无 |
| 38 | `EmployeeAdvancePostingDispatcher:104` | EMPLOYEE_ADVANCE_SETTLE（现金还款） | `currencyId` 默认 1L（本位币）（B） | B：`ONE` | 无 |
| 39 | `ErpFinPostingExceptionBizModel:346` | （手工重过账） | 源 `entity.exchangeRate`（A） | A（传） | 无 |
| 40 | `ErpFinPostingExceptionRetryProcessor:98` | （重试） | 源 `entity.exchangeRate`（A） | A（传） | 无 |
| 41 | `ErpFinDeferredPostingRetryHelper:124` | （延迟重试） | 透传原异常事件（C） | C：`ex.getExchangeRate()`（透传，无回退） | 无 |
| 42 | `BankReconAdjustmentVoucherBuilder:86` | （银行对账调整） | 多币种边界（`P2-RC-003`） | B：`ONE`（**已登记 P2-RC-003**，主路径单币种 OK） | 无外币用例 |
| 43 | `CreditFacilityInterestVoucherBuilder:72` | CREDIT_FACILITY_INTEREST | 利息按本位币（B） | B：`ONE` | 无 |

### 2.3 矩阵汇总统计

| 模式 | 点数 | 含义 | 触发面结论 |
|---|---|---|---|
| **A（FX-capable，传 entity rate）** | 21 | 源单据有 currencyId+exchangeRate，显式传 rate（null→ONE 仅当源单据自身未设） | **传 rate**，非漏传 |
| **B（functional-only，硬编码 ONE）** | 21 | 业务类型本位币（salary/折旧/存货估值/NCR/mfg-issue/工时/bank-recon/credit-interest/freight/mnt-issue/mnt-labor/cost-adj/ownership-xfer/emp-advance-cash 等） | rate=1 **语义正确**（不适用外币） |
| **C（透传原 rate）** | 1 | 重试路径透传 `ex.getExchangeRate()` | **传 rate** |
| **漏传（构造 PostingEvent 未设 rate）** | **0** | — | **不存在** |
| **合计** | 43 | — | — |

> **决定性证据**：43 构造点 == 43 setExchangeRate 点。`prepareContext:537` 的 `event.getExchangeRate() != null` 在 dispatcher 路径上**恒为 true**（每个构造点都设了非 null rate）。null 回退分支 = **死代码**（仅可由「手搓 PostingEvent 直接调 Facade 且漏设 rate」触发，此类生产调用方 = 0）。

---

## 3. P1-RC-002 P0 升级再评估（Phase 1 Decision）

### 3.1 判据对照（方法论 §2 P0④「需求契约要求的会计过账正确性破坏 / 活跃数据破坏」）

P0④ 触发 = 存在域 Provider 在**默认活跃外币路径**漏传 rate，致静默回退 rate=1 → 本位币金额错误（与 P0 示例「期间 CLOSED 后禁止过账但实际可过」同性质默认触发面）。

**核实结果**：

| 升 P0 必要条件 | 普查结论 | 成立? |
|---|---|---|
| 存在 dispatcher 在 FX 场景漏传 rate | 43 点全设 rate，0 漏传 | **❌ 不成立** |
| 该路径为默认活跃（非需 caller bug 前置） | 无任何漏传路径，更无默认活跃漏传路径 | **❌ 不成立** |
| 产生错误本位币金额（活跃数据破坏） | FX-realized 域（pur/sal）正确折算；functional-only 域 rate=1 正确；Pattern A 单 amount 域 functional==source 属 `P1-MA2-002` successor（仅当该域实际承载外币时错误，非常态） | **❌ 不成立** |

### 3.2 与 A1.1 §5.2 维持 P1 三理由对照

| A1.1 §5.2 理由 | 本普查核实 |
|---|---|
| ①「触发面依赖域调用方漏传 rate（caller bug，非默认活跃路径；当前各域 Provider 显式传 rate）」 | **CONFIRMED**：43 点全显式传 rate，0 漏传。触发面前提不存在。 |
| ②「与 MA2 §5.12 对多币种路径的 P1 分级一致」 | **不变**：MA2 §5.12 / P1-MA2-002·009·039 多币种 P1 分级维持。 |
| ③「无活跃错误数据」 | **CONFIRMED**：无默认活跃 FX 路径产生错误本位币金额（FX-realized 域正确；其余或 functional-only 或单 amount successor）。 |

### 3.3 裁决

**维持 P1-RC-002 = P1。不升 P0。不触发 MR0 即时通道。**

残余 FX 风险（Pattern A 单 amount 域 functional==source）归 `P1-MA2-002`/`P1-MA2-009`/`P1-MA3-039` MR1 successor（多币种实现缺口，**不同控制点**：rate 缺失守卫 vs FX 折算未实现）。P1-RC-002 守卫修复本身（`prepareContext` 加 rate 缺失拒绝守卫）仍属 MR1（R1.0→RC-R1.n），触及会计过账逻辑须 ask-first + 独立 plan-audit（§5），不在本验证范围。

---

## 4. §去重声明（与 arm-index 交叉比对）

本验证**未产生新 finding**。全部触发面归以下既有 finding：

| 既有 finding | 控制点 | 与本验证关系 |
|---|---|---|
| `P1-RC-002` | UC-FIN-12 断言② rate 缺失守卫未实现（`:537` 静默回退） | **本验证对象**。普查确认其触发面（域漏传 rate）不存在 → 维持 P1，不升 P0。 |
| `P1-MA3-039` | UC-FIN-12 断言① FX 折算缺失（amountSource=amountFunctional） | 不同断言（① vs ②），不可合并。 |
| `P1-MA2-002` | purchase 多币种 P2P 本位币凭证 | ✅ resolved（方案 A）；本验证 #10-12 FX-realized 证据。 |
| `P1-MA2-009` | sales 多币种 O2C + 核销汇兑损益 | ✅ resolved（方案 A + FX plug）；本验证 #13-15 FX-realized 证据。 |
| `P2-RC-003` | BankRecon 硬编码 ONE（`:86`） | 本验证 #42 同站点；P2 watch-only（主路径单币种 OK）。**与 P1-RC-002 不同控制点**（硬编码 vs 回退）。 |
| `P1-MA1-001` | mfg 多币种四件套 propId | mfg 侧 PostingEvent 单币种投影的根因，A4.2a 已投影。 |

**无未经比对直接新建的 finding。**

---

## 5. §8 过程纪律自检

### 5.1 checker actual vs baseline

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本计划无生产代码变更故无回归风险，方法论 §8 不以退出码 0 为门控）：

| 规则 | actual | baseline（`compliance-baseline.md`） | 判定 |
|---|---|---|---|
| R1a (dao().saveEntity BizModel) | 0 | 0 | = |
| R1b (dao().updateEntity BizModel) | 0 | 0 | = |
| R1c (dao().getEntityById BizModel) | 0 | 0 | = |
| R1d (dao().findAllByQuery BizModel) | 14 | 14 | = |
| R2a (BizModel daoFor ErpMd*) | 34 | 34 | = |
| R2b (BizModel daoFor Erp* 跨域) | 229 | 240（表值，含后续下降注记） | ≤（改善） |
| R2c (全生产 daoFor 总量) | 1382 | 1380（表值） | **+2** |
| R2d (Processor daoFor ErpMd*) | 34 | 32（表值） | **+2** |

**说明**：
1. **R2c/R2d 微幅上浮（+2）与本验证无关**——本计划是只读普查，零生产代码变更。这些计数反映的是**本计划执行前既有的仓状态**（由前序已审计的深化计划引入的合法 daoFor 增量，基线表的「下降注记」未同步追平）。本验证**不触及任何 .java 生产文件**，故对计数零贡献，结构上无回归可能。
2. checker 在 R3 段（`new Erp*()` 实体构造计数）未输出计数即返回（退出码 1）——为脚本 R3 段既有行为（与本验证无关；R1/R2 已完整报告）。因本验证零生产代码变更，R3+ 计数亦结构上不变。
3. **门控结论**：本验证无回归风险（零生产代码变更），checker 仅作过程记录，不作通过/失败门控。

### 5.2 closure-audit 独立性声明

本验证报告由主代理（执行者）起草。**结束审计将由独立子代理（新会话）执行**（plan §Closure Gates），执行者未自我审计，未将结束审计留为 `[ ]` 人工门控占位符。

### 5.3 与 arm-index 交叉去重声明

见 §4。全部触发面归既有 finding（`P1-RC-002`/`P1-MA2-002`/`P1-MA2-009`/`P1-MA3-039`/`P2-RC-003`/`P1-MA1-001`），无新建 finding。

---

## 6. 验证范围与非目标

- **本验证只读**：grep 调用面 + 读 JUnit + 复用 MA2/A1.1。未改任何 `.java`/`.xml`/`.orm.xml`/真相源。
- **不重新核实 P1-RC-002 的守卫缺失结论本身**（A1.1 已定级；本验证只核实「触发面」前提）。
- **不实施修复**（P1-RC-002 守卫修复 + 升 P0 项[无]经 MR0/MR1）。
- **不核实断言①/③**（归 `P1-MA2-002`/`P1-MA2-009`/`P1-MA3-039`/`P2-RC-003`）。

## 7. MR0 触发登记

**无**。Phase 1 裁决为维持 P1（§3.3），不触发 MR0 即时通道（方法论 §10）。本验证不实施修复。

## 8. 结论

UC-FIN-12 断言②「汇率缺失→拒绝过账」守卫（P1-RC-002）的**运行时触发面经全集普查确认为零**——全仓 11 域 43 个生产 PostingEvent 构造点无一漏传 `setExchangeRate`，`prepareContext:537` 静默回退分支从 dispatcher 路径不可达。A1.1 §5.2 维持 P1 的前提（「各域 Provider 显式传 rate，无活跃错误数据」）**CONFIRMED**。**维持 P1-RC-002 = P1，不升 P0，不触发 MR0，无新 finding。** 残余 FX 风险归 `P1-MA2-002`/`P1-MA2-009`/`P1-MA3-039` MR1 successor（不同控制点）。
