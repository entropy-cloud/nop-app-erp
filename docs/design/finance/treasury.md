# 资金管理 / 票据（Treasury & Notes）

## 目的

设计中式承兑汇票（应收/应付票据）、票据贴现、银行授信额度、现金预测的业务语义、状态机与业财过账。补齐仅有 `ErpFinFundAccount` + 银行对账、缺票据/贴现/授信/现金预测的 P0 缺口。

## 边界

- 本模块负责：应收票据（银承/商承）生命周期、应付票据、票据贴现、银行授信额度控制、现金流量预测。
- 本模块不负责：银行对账（`bank-reconciliation.md`，票据与银行对账解耦）；日常收付款（finance 收付款单）；银行账户主数据（master-data 域的 `ErpFinFundAccount`）。
- 实体为**建议命名，待 ORM 计划落地**（`model/app-erp-finance.orm.xml` 是 ask-first 保护区域，本文件不复述 schema）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §1.2。

### ⚠️ 证据诚实性声明（关键）

**4 个开源 ERP 均无中式承兑汇票实体**，初版计划误引以下证据已修正：

- 🟢 `BankingAcctDocProvider.java:40`（Metasfresh，实测）只注册 `C_BankStatement`（银行对账单），**不处理票据**。
- 🟢 `ApInvoiceHead.java:207-229`（赤龙，实测）是收款银行账户三件套，**无票据字段**。
- 🟢 `X_C_Payment.java:2259-2271`（iDempiere，实测）TenderType 6 种无 Notes，属西方支票文化。

**结论**：本设计的票据全生命周期属 ⚪ 领域常识（依据中国《企业会计准则》），开源零覆盖。可借鉴的仅为 **Metasfresh `Doc_BankStatement` 的科目分解范式**（🟢 `Doc_BankStatement.java:206-547`，处理银行对账单的五科目分解，**非票据**，仅作科目分解参考）。

### 核心设计点

1. **应收票据 vs 应付票据方向对称**：应收票据=资产/借方，应付票据=负债/贷方（⚪ 会计准则）。
2. **科目分解五件套**（借 Metasfresh 范式）：资产/在途/手续费/利息/汇兑损益——🟢 `Doc_BankStatement.java:206-547`（`createFacts_TrxAmt`/`BankFee`/`Interest`/`CurrencyExchangeGainOrLoss`）。
3. **贴现息走财务费用-利息支出**（非冲减应收票据）；外币贴现产生汇兑损益——🟢 `Doc_BankStatement.java:482-547` 范式。
4. **票据独立成表走 SPI 过账**（反 iDempiere 把 Check 塞进 tenderType）。

## 实体清单

> 表前缀 `erp_fin_`、类名 `ErpFin*`、字典 `erp-fin/*`。以下为建议命名，待 ORM 计划落地。

### ErpFinNotesReceivable（应收票据，表 `erp_fin_notes_receivable`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| notesType | dict `erp-fin/notes-type`：BANK_ACCEPTANCE=10（银行承兑）/COMMERCIAL_ACCEPTANCE=20（商业承兑） |
| notesNo | 票据号 |
| drawerName/drawerBank | 出票人/出票行 |
| payeeName | 收款人（本方） |
| issueDate/dueDate | 出票日/到期日 |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套（票面金额） |
| endorsementFromId | 背书来源（从哪张票背书而来，可空） |
| sourceBillType/sourceBillCode | 来源业务单（销售收款/票据背书，凭证指针反查） |
| discountId | 关联贴现明细（若已贴现） |
| status | dict `erp-fin/notes-receivable-status`：见状态机 |
| posted/postedBy/postedAt | 业财三件套 |
| 标准审计字段 | |

**状态机（7 态）**：

```
收到 (RECEIVED)
  ├─ 贴现 → 已贴现 (DISCOUNTED)
  │            └─ 到期托收 → 托收中 (COLLECTION_PENDING) → 承兑 (HONORED, 终态) / 拒付 (DISHONORED)
  ├─ 背书转让 → 已背书 (ENDORSED)
  └─ 到期托收 → 托收中 (COLLECTION_PENDING) → 承兑 (HONORED) / 拒付 (DISHONORED)
任何非终态 → 注销 (WRITE_OFF, 终态，需审批)
```

| 状态 | 业务含义 |
|---|---|
| RECEIVED | 收到票据，挂账应收票据（资产） |
| DISCOUNTED | 已向银行贴现取得资金 |
| ENDORSED | 已背书转让给供应商（抵应付） |
| COLLECTION_PENDING | 到期已送银行托收，等待兑付 |
| HONORED | 终态：银行承兑付款 |
| DISHONORED | 终态：拒付（转为应收账款追索） |
| WRITE_OFF | 终态：注销（票据遗失/作废，需审批） |

### ErpFinNotesPayable（应付票据，表 `erp_fin_notes_payable`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| notesType | dict `erp-fin/notes-type`：BANK_ACCEPTANCE/COMMERCIAL_ACCEPTANCE |
| notesNo | 票据号 |
| payeeName/payeeBank | 收款人/收款行 |
| issueDate/dueDate | 出票日/到期日 |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套 |
| creditFacilityId | 占用的授信额度（银承，→ErpFinCreditFacility） |
| sourceBillType/sourceBillCode | 来源业务单（采购付款开票） |
| status | dict `erp-fin/notes-payable-status`：ISSUED（已开出）/HONORED（已兑付，终态）/DISHONORED（拒付）/WRITE_OFF |
| posted/postedBy/postedAt | 业财三件套 |
| 标准审计字段 | |

**应付票据方向**：开出时 贷 应付票据（负债），到期兑付时 借 应付票据 / 贷 银行存款。与应收票据方向对称。

### ErpFinNotesDiscount（票据贴现明细，表 `erp_fin_notes_discount`）

| 字段 | 含义 |
|---|---|
| id/notesReceivableId/orgId | 标准 |
| discountDate | 贴现日 |
| bankId | 贴现银行 |
| faceAmount | 票面金额 |
| discountInterest | 贴现息（=票面×贴现率×剩余天数/360） |
| netAmount | 实得金额（=票面−贴现息） |
| currencyId/exchangeRate | 多币种 |
| exchangeGainLoss | 汇兑损益（外币贴现时） |
| posted/postedBy/postedAt | 业财三件套 |
| 标准审计字段 | |

### ErpFinCreditFacility（银行授信额度，表 `erp_fin_credit_facility`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| bankId/fundAccountId | 授信银行/资金账户 |
| facilityType | dict `erp-fin/credit-facility-type`：BANK_ACCEPTANCE_LINE（银承额度）/LOAN_LINE（贷款额度） |
| totalAmount | 授信总额 |
| usedAmount | 已用额度（派生，=Σ未到期银承票面） |
| availableAmount | 可用额度（派生，=total−used） |
| validFrom/validTo | 有效期 |
| 标准审计字段 | |

### ErpFinCashForecast（现金预测，物化视图，表 `erp_fin_cash_forecast`）

类比 `ErpFinBankLedgerLine`（`bank-reconciliation.md`），由 AR/AP 到期 + 票据到期派生，非手工录入。

| 字段 | 含义 |
|---|---|
| id/orgId/fundAccountId | 标准 |
| forecastDate | 预测日期 |
| sourceBillType/sourceBillCode | 来源（AR_INVOICE 到期/AP_INVOICE 到期/NOTES_RECEIVABLE 到期/NOTES_PAYABLE 到期） |
| direction | dict：INFLOW（流入）/OUTFLOW（流出） |
| amountSource/amountFunctional | 金额 |
| 标准审计字段 | |

## 业财过账（businessType）

复用 `IErpFinAcctDocProvider`（见 `posting.md`），新增 businessType：

| businessType | 触发 | 借贷方向（典型） |
|---|---|---|
| NOTES_RECEIVABLE_RECEIVED | 收到应收票据 | 借：应收票据 / 贷：应收账款（抵客户欠款） |
| NOTES_RECEIVABLE_DISCOUNTED | 票据贴现 | 借：银行存款(实得) / 借：财务费用-利息支出(贴现息) / [借/贷] 汇兑损益(外币) / 贷：应收票据(票面) |
| NOTES_RECEIVABLE_ENDORSED | 背书转让 | 借：应付账款(抵供应商) / 贷：应收票据 |
| NOTES_RECEIVABLE_COLLECTION | 到期托收承兑 | 借：银行存款 / 贷：应收票据 |
| NOTES_PAYABLE_ISSUED | 开出应付票据 | 借：应付账款 / 贷：应付票据 |
| NOTES_PAYABLE_HONORED | 应付票据到期兑付 | 借：应付票据 / 贷：银行存款 |
| CREDIT_FACILITY_INTEREST | 授信利息 | 借：财务费用-利息支出 / 贷：银行存款 |

**贴现凭证科目分解**（借 Metasfresh `Doc_BankStatement.java:206-547` 五科目分解范式，非票据证据）：

```
借：银行存款（实得金额 netAmount）
借：财务费用-利息支出（贴现息 discountInterest）
借/贷：汇兑损益（外币贴现时，exchangeGainLoss）
贷：应收票据（票面金额 faceAmount）
```

## 跨域协作

| 对端 | 协作内容 |
|---|---|
| finance/reconciliation | 应收票据收到抵客户应收账款、背书抵供应商应付账款，通过 `ErpFinReconciliation` 核销 |
| sales | 销售收款可选开票据（sourceBillType=AR_RECEIPT） |
| purchase | 采购付款可选开应付票据（sourceBillType=AP_PAYMENT） |
| bank-reconciliation | **解耦**：票据贴现的资金到账后参与银行对账，但票据生命周期独立（`bank-reconciliation.md:11` 已明确两套独立机制） |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-fin.credit-check-on-issue` | true | 开银承前是否强制校验授信可用额度 |
| `erp-fin.notes-discount-rate-default` | — | 默认贴现率（缺省时提示配置） |
| `erp-fin.cash-forecast-cron` | —（默认不执行，运维启用配置键生效） | 现金预测定时刷新 cron（聚合 AR/AP + 票据到期）。SCHEDULED：`ErpFinCashForecastJob` + `scheduler.yaml` 已接线（plan 2026-07-05-0306-1），登记于 `docs/architecture/job-scheduling.md` §3.1 `erp-fin-cash-forecast-refresh`。空值=跳过门控 |
| `erp-fin.cash-forecast-window-days` | 30 | 现金预测默认向前预测窗口（天），job 派生 [today, today+window] |

## 关键业务规则

1. **授信额度强一致校验**：开银承（应付票据 notesType=BANK_ACCEPTANCE）前同步校验 `creditFacility.availableAmount >= 票面`（参考 `budget.md:74` 钩子模式），不足则阻断并抛 `NopException`。
2. **贴现息不冲减应收票据**：贴现息走"财务费用-利息支出"，贷方冲减的是"应收票据"票面全额（科目分解五件套），不直接冲减。
3. **拒付转应收**：应收票据 DISHONORED 时转为应收账款继续追索（开票人对持票人负连带责任）。
4. **票据注销强制审批**：WRITE_OFF 属高风险操作（票据遗失/作废），需财务主管审批。
5. **现金预测派生**：`ErpFinCashForecast` 由定时任务（nop-job）聚合 AR/AP/票据到期数据生成，不手工录入。
   > **实现注（计划 1000-1）**：nop-job 定时基线未落地；当前提供手动触发的批量聚合方法 `IErpFinCashForecastBiz.refreshForecast(fromDate,toDate)`（聚合 ArApItem 未核销到期 + 票据到期，先清区间再写入），定时调度归 Follow-up（触发条件：nop-job 接线时）。拒付转应收的完整追索/坏账核销为 Non-Goal（见计划 1000-1 Deferred）。
   >
   > **银行存款外币汇兑重估已落地**（plan `2026-07-05-0540-2`）：期末结账 GL 段 `ExchangeRevaluationService` 扩展重估外币 `ErpFinFundAccount` 银行存款余额（`currentBalance` × 期末汇率 vs 科目账面本位币聚合），差额生成 EXCHANGE_GAIN_LOSS 凭证（借/贷银行存款科目 / 贷/借汇兑损益），与 AR/AP 重估同业务类型同事务同汇率源，config-gated `erp-fin.bank-fx-revaluation-enabled`。本位币账户不重估。详见 `period-close.md §汇兑重估`。
   >
   > **电子票据外部系统明确不在范围**：本项目票据模块聚焦**组织内部**的票据记账生命周期（收到/贴现/背书/托收/拒付/兑付/注销），**不对接任何外部电子票据系统**。人行 ECDS（电子商业汇票系统）已于 2024-07 正式下线关闭（被新一代票据系统取代），作为术语已废弃；新一代票据系统对接属银行/票交所接口集成，属强本地化运营层，本项目不承担。GitHub 实测亦证实开源 ERP（含国产 jsh/redragon/xingyun）均无电子票据系统对接实现。

## 反模式警示

- ⛔ **把票据塞进 payment.tenderType**（反 🟢 iDempiere `X_C_Payment.java:2267` Check 模式）——会丢失票据生命周期（贴现/背书/托收/拒付），票据必须独立成表。
- ⛔ **贴现息冲减应收票据**——应走财务费用-利息支出，否则虚减费用、虚增资产。
- ⛔ **误引 metasfresh.md:83 / redragon-erp.md:41 作为票据证据**——源码回查确认两者均非票据证据（前者只注册银行对账单，后者是收款银行账户字段）。

## 菜单归属

finance 域「资金管理」分组：应收票据、应付票据、票据贴现、授信额度、现金预测。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| 中式承兑汇票全生命周期 | ⚪ | 开源零覆盖（4 个开源 ERP 均无票据实体），依据中国《企业会计准则》领域常识 |
| 科目分解五件套（资产/在途/手续费/利息/汇兑） | 🟢 | Metasfresh `Doc_BankStatement.java:206-547` 源码实测（**银行对账单处理，非票据，仅作科目分解参考**） |
| 贴现息走财务费用 | 🟢 | Metasfresh `Doc_BankStatement.java:384,473` 源码实测（PayBankFee_Acct/B_InterestExp_Acct） |
| 外币贴现汇兑损益 | 🟢 | Metasfresh `Doc_BankStatement.java:482-547` 源码实测 |
| 票据塞 tenderType（反模式） | 🟢 | iDempiere `X_C_Payment.java:2259-2271,2267` 源码实测 |
| BankingAcctDocProvider 不处理票据 | 🟢 | Metasfresh `BankingAcctDocProvider.java:40` 源码实测（只注册 C_BankStatement） |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §1.2（设计依据）
- `docs/design/finance/posting.md`（IErpFinAcctDocProvider 过账机制）
- `docs/design/finance/bank-reconciliation.md:11`（票据与银行对账解耦）
- `docs/design/finance/budget.md:74`（强一致校验钩子模式参考）
