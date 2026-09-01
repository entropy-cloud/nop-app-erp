# finance 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（测试共享夹具见 `app-erp-test-data`，边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2a1（plan `docs/plans/2026-09-01-1245-1-m12a1-finance-seed-expansion.md`，2026-09-01）。此前 7 张 finance 表 seed（accounting_period(+status) / ar_ap_item / gl_balance / voucher(+line,+bill_r)，P2P+O2C 财务产物链）见 `docs/architecture/seed-data.md` 历史批次段。
> **过账语义 owner doc**：`docs/design/finance/posting.md`（businessType 映射 / posted 一致性裁决 / 冲销机制）；本文件只登记种子数据面，不重复过账语义。

## 种子数据范围（M1.2a1 批次 31 表 + 凭证扩展）

finance 域 33 规格实体 + 5 运行时补充实体中，7 表已由历史批次 seed；本批补齐其余 **26 规格表 + 5 运行时补充实体（共 31 CSV）**，并对既有凭证族 3 文件做**加性扩展（16 行 → 32 行）**，达成 finance 域全量 seed 覆盖（33+5 / 33+5，运行时口径）。

### 26 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpFinApDocument（AP 单据摄取） | `erp_fin_ap_document.csv` | 2 | P+N-TERM | —（PARTNER_ID 可选→ErpMdPartner〔已seed〕id=3） |
| ErpFinApDocumentLog（摄取日志） | `erp_fin_ap_document_log.csv` | 2（1/头） | P | DOCUMENT_ID→ErpFinApDocument〔本批〕 |
| ErpFinBadDebt（坏账核销/收回） | `erp_fin_bad_debt.csv` | 2 | P+N-TERM | SOURCE_AR_AP_ITEM_ID→ErpFinArApItem〔已seed〕id=3、PARTNER_ID/ACCT_SCHEMA_ID/CURRENCY_ID→ErpMd*〔已seed〕、PERIOD_ID 可选→ErpFinAccountingPeriod〔已seed〕 |
| ErpFinBankReconciliation（银行余额调节表头） | `erp_fin_bank_reconciliation.csv` | 2 | P+N-TERM | FUND_ACCOUNT_ID/STATEMENT_ID→ErpFin*〔本批〕、ORG_ID→ErpMdOrganization〔已seed〕 |
| ErpFinBankReconciliationLine（调节行） | `erp_fin_bank_reconciliation_line.csv` | 2（1/头×P 头） | P | RECONCILIATION_ID→ErpFinBankReconciliation〔本批〕 |
| ErpFinBankStatement（银行对账单头） | `erp_fin_bank_statement.csv` | 2 | P×2 | FUND_ACCOUNT_ID→ErpFinFundAccount〔本批〕、ORG_ID〔已seed〕 |
| ErpFinBankStatementLine（对账单行） | `erp_fin_bank_statement_line.csv` | 2（1/头） | P | STATEMENT_ID→ErpFinBankStatement〔本批〕、CURRENCY_ID→ErpMdCurrency〔已seed〕 |
| ErpFinBudgetScenario（预算场景头） | `erp_fin_budget_scenario.csv` | 2 | P+N-TERM | ACCT_SCHEMA_ID/CURRENCY_ID/ORG_ID→ErpMd*〔已seed〕 |
| ErpFinBudgetLine（预算行） | `erp_fin_budget_line.csv` | 2 | P | SCENARIO_ID→ErpFinBudgetScenario〔本批〕、SUBJECT_ID/SUBJECT_CODE→ErpMdSubject〔已seed〕（6601/6603）、PERIOD_ID→ErpFinAccountingPeriod〔已seed〕 |
| ErpFinBudgetCarryForwardLog（预算结转日志） | `erp_fin_budget_carry_forward_log.csv` | 1 | P | SCENARIO_ID/SOURCE_SCENARIO_ID/TARGET_SCENARIO_ID→ErpFinBudgetScenario〔本批〕（2025 关闭场景→2026 主场景） |
| ErpFinBudgetControlLog（预算控制日志） | `erp_fin_budget_control_log.csv` | 2 | P×2（PASS/WARNED） | SCENARIO_ID/BUDGET_LINE_ID→ErpFinBudget*〔本批〕、SUBJECT_ID→ErpMdSubject〔已seed〕、PERIOD_ID→ErpFinAccountingPeriod〔已seed〕；SOURCE_BILL_CODE 引用既有 seed 单据号（PO-2026-001 / EC-2026-001〔本批〕） |
| ErpFinBudgetRollforwardLog（预算滚动日志） | `erp_fin_budget_rollforward_log.csv` | 1 | P | SCENARIO_ID/SOURCE/TARGET_SCENARIO_ID→ErpFinBudgetScenario〔本批〕 |
| ErpFinReconciliation（核销单头） | `erp_fin_reconciliation.csv` | 3 | P×2+N-TERM | PARTNER_ID/ACCT_SCHEMA_ID/CURRENCY_ID/ORG_ID→ErpMd*〔已seed〕 |
| ErpFinReconciliationLine（核销行） | `erp_fin_reconciliation_line.csv` | 3（1/头） | P | RECONCILIATION_ID→ErpFinReconciliation〔本批〕、PAYMENT_ITEM_ID/INVOICE_ITEM_ID→ErpFinArApItem〔已seed〕（复现 P2P/O2C 已核销对：2↔1、4↔3） |
| ErpFinExpenseClaim（费用报销头） | `erp_fin_expense_claim.csv` | 2 | P+N-TERM | CLAIMANT_ID→ErpMdEmployee〔跨域:md·已seed〕、CURRENCY_ID/ORG_ID〔已seed〕 |
| ErpFinExpenseClaimLine（报销行） | `erp_fin_expense_claim_line.csv` | 2（1/头） | P | CLAIM_ID→ErpFinExpenseClaim〔本批〕 |
| ErpFinEmployeeAdvance（员工预支） | `erp_fin_employee_advance.csv` | 2 | P+N-TERM | EMPLOYEE_ID→ErpMdEmployee〔跨域:md·已seed〕、CURRENCY_ID/ORG_ID〔已seed〕 |
| ErpFinFundAccount（资金账户） | `erp_fin_fund_account.csv` | 3 | P×2+N-DIS | SUBJECT_ID→ErpMdSubject〔已seed〕（1002）、CURRENCY_ID〔已seed〕 |
| ErpFinVoucherTemplate（凭证模板头） | `erp_fin_voucher_template.csv` | 2 | P+N-DIS | ACCT_SCHEMA_ID→ErpMdAcctSchema〔已seed〕；BUSINESS_TYPE ∈ `erp-fin/business-type` |
| ErpFinVoucherTemplateLine（模板行） | `erp_fin_voucher_template_line.csv` | 2（1/头） | P | TEMPLATE_ID→ErpFinVoucherTemplate〔本批〕；ACCOUNT_KEY ∈ `erp-fin/account-key` |
| ErpFinGlMappingRule（GL 科目映射规则） | `erp_fin_gl_mapping_rule.csv` | 2 | P+N-DIS | ORG_ID→ErpMdOrganization〔已seed〕；BUSINESS_TYPE/ACCOUNT_KEY ∈ 字典；P 行 TARGET_SUBJECT_CODE=1405（=既有默认科目，净零语义：即使被解析器命中也与现行为一致） |
| ErpFinIntercompanyMatch（内部交易匹配） | `erp_fin_intercompany_match.csv` | 2 | P×2（字典无终态值，N-TERM 不适用——规格表偏差登记见 plan） | PERIOD_ID→ErpFinAccountingPeriod〔已seed〕、AR_ORG_ID/AP_ORG_ID→ErpMdOrganization〔已seed〕 |
| ErpFinIntercompanyTransferPrice（内部转移定价） | `erp_fin_intercompany_transfer_price.csv` | 2 | P+N-DIS | ORG_ID/FROM_ORG_ID/TO_ORG_ID→ErpMdOrganization〔已seed〕、MATERIAL_ID→ErpMdMaterial〔已seed〕 |
| ErpFinPostingException（过账异常工作台） | `erp_fin_posting_exception.csv` | 2 | P(RETRIED)+N-TERM(IGNORED) | —（ORG_ID/ACCT_SCHEMA_ID→ErpMd*〔已seed〕；VOUCHER_ID 留空） |
| ErpFinConsolidationElimination（合并抵消） | `erp_fin_consolidation_elimination.csv` | 2 | P+N-TERM(POSTED) | PERIOD_ID→ErpFinAccountingPeriod〔已seed〕、MATCH_ID→ErpFinIntercompanyMatch〔本批〕、FROM/TO_ORG_ID→ErpMdOrganization〔已seed〕 |
| ErpFinTrialBalance（试算平衡表） | `erp_fin_trial_balance.csv` | 3 | P | ACCT_SCHEMA_ID→ErpMdAcctSchema〔已seed〕、PERIOD_ID→ErpFinAccountingPeriod〔已seed〕、SUBJECT_ID→ErpMdSubject〔已seed〕；数值镜像 gl_balance（1002/1122/5001 三科目） |

### 5 运行时补充实体 CSV（缺 className 集，列集以 ORM 为准）

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpFinCashForecast（现金预测） | `erp_fin_cash_forecast.csv` | 2 | P×2（INFLOW/OUTFLOW） | FUND_ACCOUNT_ID→ErpFinFundAccount〔本批〕、PARTNER_ID→ErpMdPartner〔已seed〕；SOURCE_BILL_* 为自由字符串软引用（无 to-one） |
| ErpFinCreditFacility（授信额度） | `erp_fin_credit_facility.csv` | 2 | P+N-DIS(INACTIVE) | FUND_ACCOUNT_ID 可选→ErpFinFundAccount〔本批〕 |
| ErpFinNotesPayable（应付票据） | `erp_fin_notes_payable.csv` | 2 | P×2（ISSUED） | PARTNER_ID→ErpMdPartner〔已seed〕、CREDIT_FACILITY_ID 可选→ErpFinCreditFacility〔本批〕（行 1 占用授信链） |
| ErpFinNotesReceivable（应收票据） | `erp_fin_notes_receivable.csv` | 2 | P×2（RECEIVED/DISCOUNTED） | PARTNER_ID→ErpMdPartner〔已seed〕 |
| ErpFinNotesDiscount（票据贴现） | `erp_fin_notes_discount.csv` | 1 | P | NOTES_RECEIVABLE_ID→ErpFinNotesReceivable〔本批〕（联动 NR-2026-001，STATUS=DISCOUNTED）、BANK_ID→ErpFinFundAccount〔本批〕 |

### 凭证族扩展（既有 3 文件，16 行 → 32 行）

既有 4 张凭证（PZ-2026-001..004，P2P+O2C 产物）**零修改**；本批追加 4 张（voucher id 5..8 / line id 9..16 / bill_r id 5..8），全部 `DOC_STATUS=POSTED`、`PERIOD_ID=1`、静态日期 2026-07（冻结时钟纪律）、借贷平衡、`voucher_bill_r` 反查闭环。**`erp_fin_voucher.csv` 表头加性扩展 3 列**（POSTING_TYPE / REVERSAL_OF_VOUCHER_ID / REMARK；既有 4 行新列留空 = null，`findPostedVoucherIds` 等过滤器按 `isNull(postingType) or NORMAL` 语义与原行为一致）：

| 凭证 | businessType | 分录（金额） | 语义 |
|---|---|---|---|
| V5 PZ-2026-005（NORMAL） | NOTES_RECEIVABLE_RECEIVED | Dr 1121 应收票据 500 / Cr 1122 应收账款 500 | 收票（联动 NR-2026-001） |
| V6 PZ-2026-006（NORMAL） | EMPLOYEE_ADVANCE_SETTLE | Dr 2241 其他应付款-员工 300 / Cr 1221 其他应收款-员工预支 300 | 预支抵扣报销（联动 EA-2026-001 与 EC-2026-001） |
| V7 PZ-2026-007（NORMAL，被红冲 isReversed=true） | NOTES_PAYABLE_ISSUED | Dr 2202 应付账款 800 / Cr 2203 应付票据 800 | 银承开出置换应付（联动 NP-2026-001，posting.md 借贷方向逐字对齐） |
| V8 PZ-2026-008（REVERSAL，reversalOfVoucherId=7） | NOTES_PAYABLE_ISSUED | Dr 2202 −800 / Cr 2203 −800（红字，镜像 C13 REVERSAL 录制范式） | 误开红冲对（红字冲销机制演示） |

## FK 闭环图

```
[跨域:md·已seed] erp_md_subject(2=1002/3=1122/5=2202/6=5001/8=6601/21=1231/38=2241/39=1221/40=1121/41=2203/42=6603) ──SUBJECT_ID──▶ 预算行/试算平衡/资金账户
[跨域:md·已seed] erp_md_partner(1 华东/2 华南/3 北方钢铁/5 张三员工) ──PARTNER_ID──▶ 坏账/核销头/票据/现金预测
[跨域:md·已seed] erp_md_employee(1 张三/2 李四) ──CLAIMANT_ID/EMPLOYEE_ID──▶ 报销头/预支头
[跨域:md·已seed] erp_md_organization(1 GROUP-HQ/2 ERP-CO)、erp_md_currency(1)、erp_md_acct_schema(1)、erp_md_material(1) ──各维度──▶ 全部新表
[已seed·fin] erp_fin_accounting_period(1 2026-07 OPEN)、erp_fin_ar_ap_item(1..6) ──PERIOD_ID/SOURCE_AR_AP_ITEM_ID/PAYMENT_ITEM_ID/INVOICE_ITEM_ID──▶ 预算行/坏账/核销行/试算平衡/内部交易匹配
[本批] erp_fin_fund_account(1..3) ──FUND_ACCOUNT_ID/BANK_ID──▶ 对账单头/调节表头/现金预测/授信/贴现
[本批] erp_fin_bank_statement(1..2) ──STATEMENT_ID──▶ 对账单行 / 调节表头
[本批] erp_fin_bank_reconciliation(1..2) ──RECONCILIATION_ID──▶ 调节行
[本批] erp_fin_budget_scenario(1..2) ──SCENARIO_ID/SOURCE/TARGET──▶ 预算行/控制日志/结转日志/滚动日志
[本批] erp_fin_expense_claim(1..2) ──CLAIM_ID──▶ 报销行
[本批] erp_fin_intercompany_match(1..2) ──MATCH_ID──▶ 合并抵消
[本批] erp_fin_notes_receivable(1..2) ──NOTES_RECEIVABLE_ID──▶ 票据贴现
[本批] erp_fin_credit_facility(1..2) ──CREDIT_FACILITY_ID──▶ 应付票据（可选）
[本批] erp_fin_ap_document(1..2) ──DOCUMENT_ID──▶ 摄取日志
[本批] erp_fin_voucher_template(1..2) ──TEMPLATE_ID──▶ 模板行
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空、白名单零增量（`TestErpSeedDataIntegrity` 4/4 全绿背书）。

## 与既有 7 表 seed 的衔接（语义一致性约束）

- **凭证扩展的 posted 一致性（决策 A，凭证族内追加）**：既有 ar_ap_item / gl_balance 种子**零修改零追加**。追加 4 凭证全部经 bill_r 可反查（V5↔NR-2026-001 / V6↔EA-2026-001 / V7·V8↔NP-2026-001）；V7+V8 为净零红冲对，V5/V6 落在无 gl_balance 行的资产/负债科目（1121/2241/1221/2203）上，科目 1122 的行级聚合变化只影响试算平衡重算（C13 快照已按协议重录），静态 gl_balance 与 P&L 结转不受影响——执行期曾以 6601 费用科目试载触发 C13 损益结转锚点漂移（PL 合计 1280→1580），已按 M1.1b「seed 侧 Fix」先例改为全资产负债表科目方案。
- **源单据回链补全**：本批 `erp_fin_expense_claim` 行 1 = EC-2026-001、`erp_fin_employee_advance` 行 1 = EA-2026-001——既有 `ar_ap_item` id=5/6 OPEN 行的 SOURCE_BILL_CODE 由此获得真实源单据（此前为悬空单据号）；EC-2026-001 `POSTED=false`（GL 过账未 seed，避免违反「posted=true 当且仅当有凭证经 bill_r 串联」裁决；EA-2026-001 `POSTED=true` 经 bill_r id=6 串联成立）。
- **核销单复现既有 SETTLED 态**：核销单 1/2 + 核销行（paymentItem↔invoiceItem = ar_ap_item 2↔1、4↔3）将 1445-1 批「ar_ap_item 直表达 SETTLED」的核销补为可反查文档（seed-data.md 历史登记的 reconciliation Deferred 就此解除）；核销单 3 为 REVERSED 终态（N-TERM）。
- **银行对账/坏账与 C14 自包含**：C14 集成用例自建全部所需单据并按 id 定位（`@var` 机制），种子行不参与其动作路径（调节生成按 statementId 定位、坏账准备读取 ar_ap_item 与 1231 科目凭证行——种子零 1231 凭证行）。
- **期末前置检查零干扰**：posting_exception 种子取 RETRIED/IGNORED（非 PENDING/RETRYING/MANUAL，不阻断结账扫描）；凭证族全部 POSTED（不进 unposted 清单）；无 1231 科目凭证行（allowance 账面保持 0）。
- **维度值复用既有值域**：组织 1（GROUP-HQ，内部交易族文档头）/ 2（ERP-CO，业务单据）；partner 1/2/3/5；employee 1/2；期间 1（2026-07 OPEN）；科目编码见 FK 闭环图；全部静态日期落在 2026-05~2026-08（业务单据集中于 2026-07 参考期，票据到期 2026-10~2027-01）。
- **AP 摄取与管道门控**：ap_document 种子取 DRAFTED/FAILED 状态（`processPending` 仅扫描 RECEIVED，种子零被摄取）；文件名加 `seed-fin-ap-` 前缀与既有用例文件名空间隔离（重传去重守卫按 fileName 查询）。

## 用例指示编码与 negative 行语义

编码定义见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」通用约定 5：

- **P（最小正例行）**：全部 31 表均有。
- **N-TERM（终态行）**：ap_document id=2 `FAILED`（解析失败终态）；bad_debt id=2 `APPROVAL_STATUS=REJECTED`（驳回终态）；bank_reconciliation id=2 `REVERSED`（调节红冲终态）；budget_scenario id=2 `CLOSED`（附 CLOSED_AT，2025 预算关闭终态）；reconciliation id=3 `REVERSED`；expense_claim id=2 `CANCELLED`（审批后取消终态）；employee_advance id=2 `REJECTED`；posting_exception id=2 `IGNORED`（人工判定终态）；consolidation_elimination id=2 `POSTED`（抵消过账终态）。`intercompany_match` 字典（UNMATCHED/MATCHED/DIFF）无终态值，N-TERM 不适用——登记为规格表偏差（2 行 P 覆盖 MATCHED/DIFF 两态）。
- **N-DIS（禁用行）**：fund_account id=3 `INACTIVE`（停用账户）；credit_facility id=2 `INACTIVE`；voucher_template id=2 `IS_ACTIVE=false`；gl_mapping_rule id=2 `IS_ACTIVE=false`；intercompany_transfer_price id=2 `IS_ACTIVE=false`。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准，首跑门禁拦截 voucher_template / posting_exception 误含 REMARK 列即修，M1.1c price_list 先例同型）；省略审计列（delVersion/version/createdBy/createTime/updatedBy/updateTime）；ISO 日期；小写布尔；ID < 100000（`zz-sequence-advance.sql` 序列推进值域约束）；字典码 ∈ `erp-fin/*` 字典（BUSINESS_TYPE / ACCOUNT_KEY / 各状态列逐值核对 ORM dict）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 133→164）。
