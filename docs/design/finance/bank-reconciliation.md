# 银行对账(Bank Reconciliation)

## 目的

设计出纳月结的银行对账能力:银行流水(外部对账单)与账面流水(来自收付款凭证)逐笔勾对 + 未达账项处理 + 余额调节表。

## 设计范式

银行对账 = **银行流水(外部对账单)** 与 **账面流水(来自收付款凭证)** 的逐笔勾对。对照 ERPNext Bank Reconciliation、管伊佳账户流水、通用出纳月结。

**与 AR/AP 核销的区别**(重要):ErpFinReconciliation(发票↔收付款)是"发票 vs 收付款";银行对账是"银行 vs 账面"。两者是**两套独立机制**,不能合并:一笔收款既要在 ErpFinReconciliation 核销发票,也要在 ErpFinBankStatement 与银行流水勾对——两者独立。

## 实体清单

### ErpFinBankStatement(银行对账单头,表 `erp_fin_bank_statement`)

| 字段 | 含义 |
|---|---|
| id/code/name/orgId | 标准 |
| fundAccountId | 关联 ErpFinFundAccount(必须 accountType=BANK) |
| acctSchemaId | 账套 |
| currencyId | 币种(与资金账户一致) |
| statementDate | 对账单日期(月末日) |
| openingBalance | 对账单期初余额 |
| closingBalance | 对账单期末余额(银行侧) |
| businessDate | 业务日期(取 statementDate) |
| docStatus | dict `erp-fin/bank-stmt-status`:DRAFT/RECONCILING/RECONCILED/CANCELLED |
| approveStatus | dict `erp-fin/approve-status`:UNSUBMITTED/SUBMITTED/APPROVED/REJECTED |
| posted/postedBy/postedAt | 业财三件套(已调节完成时 posted=true,触发未达账项调整凭证) |
| 标准审计字段 | |

**状态机**:`DRAFT → RECONCILING → RECONCILED`(终态,差异已处理且 posted=true);`RECONCILING → DRAFT`(重开,要求反 posted);`RECONCILED → CANCELLED`(红冲未达账项调整凭证)。

### ErpFinBankStatementLine(银行流水明细,表 `erp_fin_bank_statement_line`)

即"银行对账单逐笔",外部导入。

| 字段 | 含义 |
|---|---|
| id/statementId/lineNo/orgId | 标准 |
| fundAccountId | 资金账户 |
| txnDate | 交易日期(银行记账日) |
| valueDate | 价值日期(资金实际到账日,跨期未达账项判定依据) |
| bankTxnCode | 银行流水号(去重幂等键,导入时唯一) |
| counterpartyName/counterpartyAccount/counterpartyBank | 对方户名/账号/开户行 |
| direction | dict `erp-fin/bank-txn-direction`:DEBIT(借,银行扣款)/CREDIT(贷,银行入账) |
| amountSource | 源币金额 |
| currencyId/exchangeRate/amountFunctional | 多币种四件套 |
| memo | 银行摘要 |
| matchStatus | dict `erp-fin/bank-match-status`:UNMATCHED/MATCHED/MANUAL_MATCHED/SUSPENSE |
| matchedLedgerLineId | 关联账面流水(ErpFinBankLedgerLine.id),nullable |
| isReconciled/reconciledAt/reconciledBy | 勾对标志 |
| 标准审计字段 | |

### ErpFinBankLedgerLine(账面流水视图,表 `erp_fin_bank_ledger_line`,物化视图)

**来源**:所有命中资金账户科目的已过账凭证分录行。物化表保证勾对状态独立持久。

| 字段 | 含义 |
|---|---|
| id/orgId/fundAccountId | 标准 |
| voucherId/voucherLineId | 关联凭证(账面流水即凭证行视图) |
| sourceBillType/sourceBillCode | 来源单据(PAYMENT/RECEIPT,回链) |
| businessDate | 凭证日期 |
| direction | DEBIT/CREDIT |
| currencyId/exchangeRate/amountSource/amountFunctional | 多币种四件套 |
| counterpartyName/counterpartyAccount | 对方信息(取自凭证行 partner) |
| partnerId | 往来单位 |
| matchStatus/matchedStatementLineId/isReconciled | 勾对反向指针 |
| 标准审计字段 | |

### ErpFinBankReconciliation(余额调节表,表 `erp_fin_bank_reconciliation`)

一次月结产出一张,对账单单头 RECONCILED 时生成。

| 字段 | 含义 |
|---|---|
| id/code/orgId/acctSchemaId/fundAccountId/currencyId | 标准 |
| statementId | 关联对账单 |
| periodId | 期间 |
| businessDate | 调节基准日 |
| bankBalance | 银行对账单余额 |
| bookBalance | 账面余额(资金账户 currentBalance) |
| amtInTransitIn/amtInTransitOut | 企业已记银行未记(在途收款/付款) |
| amtBankNotInBooks | 银行已记企业未记(未达账项合计) |
| bankAdjBalance/bookAdjBalance | 调节后双方余额(应相等,否则不平) |
| diffAmount | 差额(≠0 报错) |
| adjustVoucherId | 未达账项调整凭证(生成机制凭证) |
| docStatus/posted/postedBy/postedAt/approveStatus | 三轴 |
| 标准审计字段 | |

**状态机**:`DRAFT → RECONCILED`(diff=0 且 posted=true)→ CANCELLED(红冲调整凭证)。无 RECONCILING(调节表生成即终态)。

## 业务规则

1. **对账单导入幂等**:以 `(fundAccountId, statementDate, bankTxnCode)` 为唯一键去重,重复导入报"已存在"。

2. **自动勾对算法**:按 `(amountSource, direction 反向, valueDate ± N 天, counterpartyAccount)` 模糊匹配,命中唯一记录则 MATCHED;多条候选则 UNMATCHED 等待人工;金额一致但对方户名差一的标 SUSPENSE 待核。

3. **方向语义对齐**:银行"借"= 企业账面"贷"(资金流出),反之亦然。勾对时必须方向相反且金额相等。

4. **未达账项**:RECONCILED 时仍有 UNMATCHED 的行:
   - 银行有、账面无 → amtBankNotInBooks,需查实是否漏做凭证,月末生成暂估调整凭证(businessType=BANK_RECON_ADJ 新增到 erp-fin/business-type)。
   - 账面有、银行无 → amtInTransitIn/Out,属正常在途,下月对账自动消除。

5. **余额调节恒等式**:`bankBalance + amtInTransitIn − amtInTransitOut = bookBalance + amtBankNotInBooks`,diffAmount 必须 = 0,否则抛 NopException 阻止 RECONCILED。

6. **posted 联动**:调节表 RECONCILED 时若存在未达账项,生成调整凭证(isReversed=false),下月初自动红冲(跨期还原)。

7. **资金账户余额校验**:ErpFinFundAccount.currentBalance 必须等于 Σ 账面流水,定期对账任务兜底。

8. **与 AR/AP 核销解耦**:银行对账只确认"钱到账/已付",不替代发票核销。

9. **期间控制**:对账单所属期间若已 CLOSED(ErpFinAccountingPeriodStatus.glStatus=CLOSED)不可再生成新调节表。

10. **多币种**:外币账户对账时,未达账项调整凭证需考虑汇兑损益(关联 businessType=FX_REVALUATION 已有字典)。

## 与现有实体的关系

- **ErpFinFundAccount**:直接关联,accountType=BANK 才可对账;currentBalance 与 bookBalance 校验一致。
- **ErpFinVoucher/VoucherLine**:ErpFinBankLedgerLine 是凭证行的物化视图;未达账项调整凭证走标准凭证流程并通过 ErpFinVoucherBillR(billType=BANK_RECON_ADJ)回链。
- **ErpFinAccountingPeriod/AccountingPeriodStatus**:期间控制对账单可生成。
- **ErpFinReconciliation**:独立于本机制(发票核销 vs 银行勾对),但通过 sourceBillCode=PAYMENT/RECEIPT 共享收付款单号做交叉查询。
- **erp-fin/business-type 字典**:新增 BANK_RECON_ADJ(银行对账调整)。

## 关键决策

> **银行流水独立成表 + 双向勾对指针** —— 对照 ERPNext Bank Reconciliation 的"两表逐笔勾对"模型,而非把它塞进 AR/AP 核销表。**物化账面流水视图**避免每次对账扫描全量凭证行,性能与勾对状态独立性兼得。**未达账项用差额生成调整凭证并下月红冲**,符合中式"银行存款余额调节表"规范。

## 菜单归属

finance 域「银行对账」分组:银行对账单、账面流水、余额调节表。

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-erpnext.md`(Bank Reconciliation)
- `docs/design/finance/ar-ap-reconciliation.md`(核销机制,与银行对账的区别)
