# 2026-07-05-0115-2-finance-bank-reconciliation 银行对账（流水导入 + 自动勾对 + 余额调节表 + 未达账项调整凭证）

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/design/finance/bank-reconciliation.md`（银行流水↔账面流水勾对 + 未达账项 + 余额调节表）；0300-3 Non-Goal「银行对账」承接
> Related: `2026-07-02-0300-3-ar-ap-settlement-subledger.md`（AR/AP 核销，与银行对账解耦）、`2026-07-02-1000-3-finance-period-close.md`（期间控制：CLOSED 不可生成新调节表）、`2026-07-01-0811-1-finance-posting-engine-foundation.md`（凭证生成入口）、`2026-07-04-1600-1-batch-scheduling-architecture.md`
> Mission: erp
> Work Item: 银行对账（Bank Reconciliation）业务逻辑
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **银行对账四实体已存在（codegen，空 CrudBizModel 壳）**：
  - `ErpFinFundAccount`（`finance.orm.xml:672`）：`accountType`(erp-fin/fund-account-type BANK/CASH/ALIPAY/WECHAT/OTHER)、`subjectId`→会计科目、`bankName`/`bankAccount`、`currencyId`、`openingBalance`/`currentBalance`、`status`。**资金账户余额载体已具备**。
  - `ErpFinBankStatement`（:706）：`fundAccountId`、`statementDate`、`beginningBalance`/`endingBalance`、`totalDebit`/`totalCredit`、`importTime`、`docStatus`(erp-fin/voucher-status)。**银行对账单头已具备**。
  - `ErpFinBankStatementLine`（:738）：`statementId`、`transactionDate`、`description`(银行摘要)、`refNo`(银行参考号)、`dcDirection`(erp-fin/dc-direction DEBIT/CREDIT)、`amount`、`currencyId`、`balanceAfter`、`matchStatus`(**ext:dict 错误指向 `erp-fin/ar-ap-status`**——契约漂移)、`matchedLineId`→`ErpFinVoucherLine`。
  - `ErpFinBankReconciliation`（:770）：`fundAccountId`、`statementId`、`reconciliationDate`、`bookBalance`/`statementBalance`、`unreconciledDiff`、`isBalanced`、`reconciledAt`/`reconciledBy`、`docStatus`。+ `ErpFinBankReconciliationLine`（:806）：`adjustmentType`/`dcDirection`/`amount`/`side`。
- **契约漂移（须 Fix，规则 13 不可降级）**：
  1. `ErpFinBankStatementLine.matchStatus` ext:dict 指向 `erp-fin/ar-ap-status`（OPEN/PARTIAL/SETTLED/CANCELLED），**语义错误**——银行勾对需要 UNMATCHED/MATCHED/MANUAL_MATCHED/SUSPENSE。`erp-fin/bank-match-status` 字典**不存在**。
  2. `BANK_RECON_ADJ` 业务类型缺失——`ErpFinBusinessType` 枚举末位 `FREIGHT(310)`（`ErpFinBusinessType.java`），字典 `erp-fin/business-type` 无 bank-recon 项。未达账项调整凭证无业务类型可用。
- **`ErpFinBankLedgerLine`（账面流水物化视图）不存在**：设计 `bank-reconciliation.md` 假设此实体承载勾对反向指针。实际账面流水来源 = 命中资金账户科目的已过账 `ErpFinVoucherLine`。
- **凭证生成入口已具备**：`IErpFinVoucherBiz.post`/`reverse`（0811-1）；凭证行 `ErpFinVoucherLine` + `ErpFinVoucherBillR`（按 billType 回链）。未达账项调整凭证可经此入口生成，billType=BANK_RECON_ADJ。
- **期间控制已具备**：`ErpFinAccountingPeriodStatus.glStatus`（1000-3）；设计 §业务规则9「期间 CLOSED 不可生成新调节表」可查。
- **与 AR/AP 核销解耦**：0300-3 + 设计 §关键决策 明确银行对账（银行 vs 账面）独立于 AR/AP 核销（发票 vs 收付款），二者经 sourceBillCode=PAYMENT/RECEIPT 交叉查询不耦合。
- **BizModel 空壳**：5 个银行/资金实体 BizModel 均为 codegen 空 `CrudBizModel`。
- **剩余差距**：(1) matchStatus 字典漂移 + BANK_RECON_ADJ 业务类型缺失；(2) 无对账单导入（幂等去重）；(3) 无自动勾对算法（流水↔凭证行）；(4) 无余额调节表生成 + 平衡校验；(5) 无未达账项调整凭证；(6) 无期间控制门控。

## Goals

- **ORM 字典增量（ask-first 保护区域）**：新增 `erp-fin/bank-match-status` 字典（UNMATCHED/MATCHED/MANUAL_MATCHED/SUSPENSE）并修正 `ErpFinBankStatementLine.matchStatus` ext:dict 漂移；向 `erp-fin/business-type` 加 `BANK_RECON_ADJ` 选项 + 同步 `ErpFinBusinessType` 枚举常量。**不新增实体、不加列**（账面流水经凭证行查询承载，见 Decision）。
- **对账单导入** `IErpFinBankStatementBiz.importStatement(fundAccountId, lines)`（`@BizMutation`）：幂等去重（按 `refNo` 优先，缺失回退 `(transactionDate, amount, dcDirection)` 组合键，config-gated 严格度），行初始 `matchStatus=UNMATCHED`。
- **自动勾对算法** `BankStatementMatcher`：按 `(amountSource, dcDirection 反向, transactionDate ± N 天, counterparty)` 模糊匹配候选已过账 `ErpFinVoucherLine`（命中资金账户 subject）；唯一命中 → MATCHED + 回写 `matchedLineId`；多候选 → UNMATCHED 等待人工；金额一致但凭据不唯一 → SUSPENSE。方向语义对齐（银行借=账面贷，资金流出）。
- **手工勾对** `IErpFinBankStatementLineBiz.manualMatch(lineId, voucherLineId)`（`@BizMutation`）：标 MANUAL_MATCHED。
- **余额调节表** `IErpFinBankReconciliationBiz.generate(statementId)`（`@BizMutation`）：聚合已勾对 + 未达账项，计算 `bookBalance`/`statementBalance`/在途/未达，恒等式 `bankBalance + 在途 = bookBalance + 未达` 校验，`unreconciledDiff=0` 置 `isBalanced=true`，否则抛 `NopException` 阻止完成。
- **未达账项调整凭证**：调节表平衡且存在「银行已记企业未记」项时，生成 `BANK_RECON_ADJ` 调整凭证（经 `IErpFinVoucherBiz.post`）；下月初红冲（跨期还原，config-gated）。
- **期间控制门控**：对账单所属期间 `glStatus=CLOSED` 不可生成新调节表（`§业务规则9`）。
- 行为测试覆盖导入幂等、自动勾对（唯一/多候选/SUSPENSE）、手工勾对、调节表平衡/不平衡、未达账项调整凭证、期间门控。

## Non-Goals

- **`ErpFinBankLedgerLine` 账面流水物化视图实体**：设计假设的物化视图实体不新增——账面流水经查询已过账 `ErpFinVoucherLine`（命中资金账户 subject）按需承载，勾对状态持久在 `ErpFinBankStatementLine.matchStatus`/`matchedLineId`。**Decision 见 Task Route**（避免新实体 + populate 机制）。
- **银行存款外币汇兑重估**：1000-3 Deferred 项，前置条件是「科目级币种标记」（master-data `ErpMdSubject` 加币种属性），属独立结果表面，非银行对账前置。归后续（触发条件：`ErpMdSubject` 落地科目级币种标记时）。
- **多币种对账汇兑损益**：设计 §业务规则10 提及外币账户未达账项调整考虑汇兑损益；本计划单币种对账为主，多币种汇兑归银行存款外币重估后续。
- **对账单导入文件格式解析（MT940/CSV/Excel 解析器）**：本计划交付导入入口（接收已解析行集合）；外部文件格式解析（MT940/CSV 解析）属集成层，归后续（触发条件：接入具体银行文件格式时）。
- **资金账户余额与账面流水定期对账任务（nop-job）**：设计 §业务规则7 定期对账兜底；属定时任务，归 1600-1 batch 架构下后续接线。
- **银行对账 AMIS 页面/UI**：CRUD 页面已由 codegen 生成；勾对交互页面（双栏勾对 UI）归前端 roadmap。
- **与 AR/AP 核销的自动联动**：设计 §业务规则8 明确解耦，不自动联动。

## Task Route

- Type: `architecture change`（新增银行对账业务机制：流水↔凭证行勾对 + 余额调节 + 未达账项凭证）+ 一处 ask-first ORM 字典增量（保护区域）。**不新增实体、不加列**——仅加 1 字典 + 1 字典选项 + 修 1 ext:dict 漂移 + 同步枚举。
- Owner Docs: `docs/design/finance/bank-reconciliation.md`（实体/业务规则/关键决策/菜单）、`docs/design/finance/posting.md`（凭证生成）、`docs/design/finance/ar-ap-reconciliation.md`（与银行对账的区别，解耦依据）、`docs/architecture/data-dependency-matrix.md`（finance 内部，无跨域新增）。
- Skill Selection Basis: BizModel `@BizMutation`/`@BizQuery` + 跨实体查询（VoucherLine/FundAccount/AccountingPeriodStatus）+ 凭证生成 + ORM 字典 ask-first + 错误码 + 事务 → 加载 `nop-backend-dev`（覆盖 IBiz、跨实体访问、凭证生成、ask-first 流程、反模式自检）。
- **Decision（账面流水承载方式）**：**选择**查询已过账 `ErpFinVoucherLine`（按 FundAccount.subjectId 过滤命中资金账户科目的分录）按需承载账面流水，勾对状态持久在 `ErpFinBankStatementLine.matchStatus`/`matchedLineId`。**替代**：① 新增 `ErpFinBankLedgerLine` 物化视图实体 + populate 机制（设计假设）——新增实体 + populate 触发点 + 双向指针同步，复杂度高，且勾对状态已可在 BankStatementLine 侧单点持久，rejected；② 每次.full-scan VoucherLine（性能差，rejected）。**残留风险**：按需查询需 FundAccount.subjectId 已配置（未配置则该账户无可勾对账面流水，前置检查覆盖）。
- **Decision（matchStatus 字典漂移修正）**：**选择**新增 `erp-fin/bank-match-status`（UNMATCHED/MATCHED/MANUAL_MATCHED/SUSPENSE）+ 修正 `ErpFinBankStatementLine.matchStatus` ext:dict。**替代**：复用 `erp-fin/ar-ap-status`（语义错误，SETTLED/PARTIAL 不表达勾对态，rejected）。**残留风险**：ext:dict 改动须 codegen 重生成 dict 产物 + xmeta。
- **Decision（未达账项调整凭证业务类型）**：**选择**新增 `BANK_RECON_ADJ(320)` 到 `erp-fin/business-type` 字典 + 同步 `ErpFinBusinessType` 枚举（枚举 javadoc 明示「新增字典项时须同步追加枚举常量」）。**替代**：复用既有业务类型（无匹配语义，rejected）。**残留风险**：枚举/字典双源须一致（code=320）。
- **Decision（对账单/调节表 docStatus + 过账态承载）**：**选择**复用既有 `erp-fin/voucher-status`（DRAFT/POSTED/CANCELLED，已核实存在）。BankStatement docStatus=DRAFT（导入）→（勾对完成后由 generate 调节表驱动，不在头单建独立 RECONCILING 态，RECONCILING 由「有未勾对行」派生）；BankReconciliation generate 时 docStatus=DRAFT，`post` 平衡+生成未达调整凭证后 docStatus=POSTED，`reverse` 红冲调整凭证后 docStatus=CANCELLED（终态）。**过账态仅由 docStatus 表达**——ErpFinBankReconciliation 无 posted 三件套、无 adjustVoucherId 列（已核实 orm:773-793），未达调整凭证经 `ErpFinVoucherBillR`(businessType+billType=BANK_RECON_ADJ, billCode=调节表 code) 反查定位（对齐 `ErpFinPostingProcessor.findBillLinks` 既有范式），不持久化 FK。**替代**：① 新增 `erp-fin/bank-stmt-status`（DRAFT/RECONCILING/RECONCILED/CANCELLED，设计 §实体清单）——增加字典 churn，且 RECONCILING 可派生、RECONCILED=POSTED，rejected；② 加 posted 三件套 + adjustVoucherId 列以对齐其他业务单据范式——违反本计划「不加列」约束、扩大保护区域，rejected（BillR 反查已满足定位需求）。**残留风险**：voucher-status 无 RECONCILING 显式态（由派生表达，可接受）；既有 `erp-fin/business-type` 字典(string code)与 `ErpFinBusinessType` 枚举常量名已存在历史发散（如 PRODUCTION_COST↔MANUFACTURING_COST_CLOSE），本计划新增 BANK_RECON_ADJ 两侧名称一致，但审计时勿混淆既有漂移。
- **Decision（导入幂等键）**：**选择** `refNo` 优先（银行参考号，存在时唯一），缺失回退 `(transactionDate, amount, dcDirection)` 组合，严格度经 `erp-fin.bank-import-strict-refno` 配置（true=缺 refNo 拒绝）。**替代**：① 设计的 `bankTxnCode`（须加列，rejected 避免加列）；② 无去重（重复导入风险，rejected）。**残留风险**：refNo 非全局唯一时（极少）依赖配置严格度兜底。

## Infrastructure And Config Prereqs

- 配置项：`erp-fin.bank-match-tolerance-days`(默认 3，勾对日期窗口)、`erp-fin.bank-import-strict-refno`(默认 false，缺 refNo 时是否拒绝)、`erp-fin.bank-recon-auto-reverse-next-month`(默认 true，未达调整凭证下月红冲)、`erp-fin.bank-recon-precision`(引用 `erp-fin.reconcile-precision` 默认 0.01)。经 `AppConfig.var(..., defaultValue)` 读取。
- 模块依赖：全在 finance 内部（VoucherLine/FundAccount/AccountingPeriodStatus 同域）；无新增跨模块依赖。
- **保护区域门控**：`module-finance/model/app-erp-finance.orm.xml` ask-first（加 bank-match-status 字典 + business-type 加 BANK_RECON_ADJ 选项 + 修 matchStatus ext:dict）+ `ErpFinBusinessType.java` 枚举同步。Phase 1 实施前须：人工批准 + 本计划草案审查通过 + codegen 重生成。
- 无数据迁移（不加实体/列，仅加字典选项 + 修 ext:dict）；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — ORM 字典增量（ask-first）+ 导入幂等

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（bank-match-status 字典 + matchStatus ext:dict 修正 + business-type 加 BANK_RECON_ADJ）、`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`（加 BANK_RECON_ADJ(320)）、`.../entity/ErpFinBankStatementBizModel.java`（扩 importStatement）、`.../biz/IErpFinBankStatementBiz.java`（扩）、`.../service/BankStatementImporter.java`（新）、`.../ErpFinErrors.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Decision | Proof`
- Prereqs: **人工批准**（model/*.orm.xml ask-first，字典加值/ext:dict 修正）+ 本计划草案审查通过。

- [x] `Fix`：新增 `erp-fin/bank-match-status` 字典（UNMATCHED/MATCHED/MANUAL_MATCHED/SUSPENSE）；修正 `ErpFinBankStatementLine.matchStatus` ext:dict 由 `erp-fin/ar-ap-status` → `erp-fin/bank-match-status`（契约漂移修正，规则 13 不可降级）。
  - Skill: none
- [x] `Fix`：向 `erp-fin/business-type` 字典加 string option `code="BANK_RECON_ADJ" value="BANK_RECON_ADJ"`（字典为字符串 code，与既有 PURCHASE_INPUT 等同层）；同步 `ErpFinBusinessType` 枚举加常量 `BANK_RECON_ADJ(320)`（枚举为数值 code=320，枚举 javadoc「新增字典项时须同步追加」——两侧**名称**一致，字典 string code 与枚举数值 code 各按其层契约）。
  - Skill: none
- [x] `Add`：`BankStatementImporter.importStatement(fundAccountId, statementDate, lines)` —— 校验 FundAccount.accountType=BANK；幂等去重（refNo 优先，缺失回退组合键，`erp-fin.bank-import-strict-refno` 严格度）；行初始化 matchStatus=UNMATCHED；写 ErpFinBankStatement(头, docStatus=DRAFT) + Lines；`importTime` 戳。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpFinBankStatementBiz.importStatement(@Name("fundAccountId") Long, @Name("statementDate") Date, @Name("lines") List)`（`@BizMutation`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinErrors` 扩展 `ERR_BANK_STMT_DUPLICATE` / `ERR_FUND_ACCOUNT_NOT_BANK` / `ERR_BANK_IMPORT_REFNO_REQUIRED`。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpFinBankStatementImport`——导入成功 + 行 UNMATCHED；重复导入（同 refNo）拒绝 ERR_BANK_STMT_DUPLICATE；非 BANK 账户拒绝；strict-refno=true 缺 refNo 拒绝。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinBankStatementImport*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付字典增量（解除 matchStatus/BANK_RECON_ADJ 漂移）+ 导入幂等。解除 Phase 2 勾对（需 UNMATCHED 行 + VoucherLine 查询）阻塞。

- [x] 字典漂移修正 + BANK_RECON_ADJ 落地（codegen 重生成后 matchStatus 指向 bank-match-status）；导入幂等单测通过
- [x] 本地化验证：改动模块类型检查/编译通过（解除 Phase 2 编译依赖）

### Phase 2 — 自动勾对 + 手工勾对

Status: completed
Targets: `.../service/BankStatementMatcher.java`（新）、`.../entity/ErpFinBankStatementLineBizModel.java`（扩 autoMatch/manualMatch）、`.../biz/IErpFinBankStatementLineBiz.java`（扩）、`.../service/BankLedgerQuery.java`（新，VoucherLine 按需查询）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（matchStatus 字典 + 导入落 UNMATCHED 行）。

- [x] `Add`：`BankLedgerQuery.findCandidates(fundAccount, amount, oppositeDirection, transactionDate, daysWindow)` —— 查已过账 `ErpFinVoucherLine`（subjectId=FundAccount.subjectId + amount 匹配 + dcDirection 反向），经 voucherId join 头表 `ErpFinVoucher.voucherDate` 落在 `[transactionDate − N, transactionDate + N]` 窗口内（日期列在凭证**头** ErpFinVoucher.voucherDate 上，VoucherLine 无日期列，须 join）。返回候选行。方向语义：银行 DEBIT(借/扣款) ↔ 账面 CREDIT(贷/资金流出)。
  - Skill: `nop-backend-dev`
- [x] `Add`：`BankStatementMatcher.autoMatch(statementId)` —— 遍历 UNMATCHED 行，调 `BankLedgerQuery.findCandidates`：唯一命中 → MATCHED + 回写 matchedLineId；多候选 → 保持 UNMATCHED；金额一致但凭据不唯一（如 refNo 部分匹配）→ SUSPENSE。返回匹配报告（matched/unmatched/suspense 计数）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpFinBankStatementLineBiz.autoMatch(@Name("statementId") Long)` 与 `manualMatch(@Name("lineId") Long, @Name("voucherLineId") Long)`（`@BizMutation`，manualMatch 标 MANUAL_MATCHED + 回写 matchedLineId）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpFinBankStatementMatch`——唯一命中 MATCHED + matchedLineId 正确；多候选 UNMATCHED；SUSPENSE；方向相反校验；手工勾对 MANUAL_MATCHED。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinBankStatementMatch*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付自动/手工勾对。解除 Phase 3 调节表（需勾对结果）阻塞。

- [x] 自动勾对（唯一/多候选/SUSPENSE）+ 手工勾对单测通过；matchedLineId 正确回写

### Phase 3 — 余额调节表 + 未达账项调整凭证 + 期间门控

Status: completed
Targets: `.../entity/ErpFinBankReconciliationBizModel.java`（扩 generate/post/reverse）、`.../biz/IErpFinBankReconciliationBiz.java`（扩）、`.../service/BankReconciliationBuilder.java`（新）、`.../service/BankReconAdjustmentVoucherBuilder.java`（新）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2（勾对结果）。

- [x] `Add`：`BankReconciliationBuilder.generate(statementId)` —— 期间门控（对账单所属期间 glStatus≠CLOSED，`§业务规则9`）；聚合：bookBalance=FundAccount.currentBalance、statementBalance=对账单 endingBalance、在途（账面有银行无：MATCHED 反向？以勾对状态推导）、未达（银行有账面无：UNMATCHED 行）；恒等式校验 `bankBalance + 在途收款 − 在途付款 = bookBalance + 银行已记企业未记`，diff=0 置 isBalanced=true，否则抛 `NopException` ERR_BANK_RECON_NOT_BALANCED；写 ErpFinBankReconciliation(docStatus=DRAFT) + adjustmentLines。
  - Skill: `nop-backend-dev`
- [x] `Add`：未达账项调整凭证——`BankReconAdjustmentVoucherBuilder`：调节表平衡且存在「银行已记企业未记」项时，生成 `BANK_RECON_ADJ` 调整凭证（经 `IErpFinVoucherBiz.post`，落库时 `ErpFinVoucherBillR` 的 `businessType`+`billType`=BANK_RECON_ADJ + `billCode`=调节表 code 回链，**反向按 businessType 反查**——对齐 `ErpFinPostingProcessor.findBillLinks` 既有范式）。`post(reconciliationId)` 置 docStatus=POSTED——**过账态仅由 docStatus=POSTED 表达**（ErpFinBankReconciliation 无 posted 三件套、无 adjustVoucherId 列，见 Decision D4；调整凭证经 BillR 反查定位，不持久化 FK）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`reverse(reconciliationId)` —— 红冲未达账项调整凭证（经 `IErpFinVoucherBiz.reverse`，按 `ErpFinVoucherBillR.businessType`=BANK_RECON_ADJ 反查本期调整凭证）+ 调节表 docStatus POSTED→CANCELLED（voucher-status 含 CANCELLED，终态；红冲后调节表不可重用，须重新 generate）。下月初自动红冲经 config-gated（`erp-fin.bank-recon-auto-reverse-next-month`，归定时任务 Non-Goal，本计划交付 reverse 入口 + 手动可触发）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：在途/未达推导口径——**选择**以 `ErpFinBankStatementLine.matchStatus` + 凭证行存在性推导：银行行 MATCHED=已勾对；银行行 UNMATCHED=「银行已记企业未记」（未达账项，需调整）；账面凭证行无对应银行行=「企业已记银行未记」（在途，下月消除）。**替代**：独立在途/未达标记列（须加列，rejected）。**残留风险**：推导依赖勾对完整性（autoMatch + manualMatch 覆盖率）。
  - Skill: none
- [x] `Proof`：`TestErpFinBankReconciliation`——平衡（diff=0, isBalanced=true）+ 不平衡抛错 + 未达账项调整凭证生成（BANK_RECON_ADJ）+ reverse 红冲 + 期间 CLOSED 拒绝生成。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinBankReconciliation*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付余额调节表 + 未达账项调整凭证 + 期间门控。完整仓库验证属 Closure Gates。

- [x] 调节表平衡校验 + 未达账项调整凭证（BANK_RECON_ADJ）+ reverse + 期间门控单测通过

### Phase 4 — 端到端 + 文档/日志

Status: completed
Targets: `.../TestErpFinBankReconciliationEndToEnd.java`（新）、`docs/logs/2026/{07-05}.md`、`docs/design/finance/bank-reconciliation.md`（实现权威 schema 补注）、`docs/architecture/job-scheduling.md`（资金账户对账兜底作业登记，若新增）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1/2/3。

- [x] `Proof`：`TestErpFinBankReconciliationEndToEnd`——导入流水 → autoMatch → manualMatch 余项 → generate 调节表 → 平衡 → post 生成未达调整凭证 → reverse 红冲全链。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinBankReconciliationEndToEnd*`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`docs/design/finance/bank-reconciliation.md` 补注实现权威 schema（ErpFinBankLedgerLine 物化视图未采用 → 改为 VoucherLine 按需查询；matchStatus 字典 bank-match-status；docStatus 复用 voucher-status；幂等键 refNo）——ORM 为唯一真相源，设计草图与实现偏离处补注；若新增资金账户对账兜底作业，登记 job-scheduling.md。
  - Skill: none

Exit Criteria:

> Phase 4 交付端到端 + 文档对齐。

- [x] 端到端全链单测通过
- [x] 当日日志已记；bank-reconciliation.md 实现 schema 补注完成

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0d1d9be90ffe3gM77AULz0tbMJ`，独立 general 子代理，对照实时仓库复核）。1 BLOCKER（B1）：Phase 3 未达账项调整凭证过账引用 `posted` 三件套 + `adjustVoucherId` 回写，但 `ErpFinBankReconciliation`（orm:773-793）无这些列，与「不加列」声明矛盾。1 MAJOR（M1）：`reverse` docStatus 终态未指定。已修订：B1 改为「过账态仅由 docStatus=POSTED 表达 + 调整凭证经 ErpFinVoucherBillR(billType=BANK_RECON_ADJ) 反查」，Decision D4 重写明确无 posted 三件套/无 adjustVoucherId 列 + 替代方案加 posted 三件套列(rejected 违反不加列)；M1 明确 reverse→docStatus=CANCELLED 终态；S1 BankLedgerQuery 改 join ErpFinVoucher.businessDate（businessDate 在头表）；S2 business-type 字典 string code + 枚举数值 code 两侧名称一致澄清；S3 既有字典↔枚举名历史发散在 D4 残留风险一行提示。
- Independent draft review iteration 2: **accept / consensus**（`ses_0d1d41aa5ffetstNkJwpGx5ay8`，独立 general 子代理，新会话，对照实时仓库复核）。B1 已解决（Phase 3 post/reverse 不再写 posted 三件套/adjustVoucherId；Decision D4 明确「过账态仅由 docStatus + ErpFinVoucherBillR 反查」；核实 orm:773-793 确无 posted/adjustVoucherId 列；ErpFinVoucherBillR orm:387-390 含 billType+billCode+businessType，`ErpFinPostingProcessor.findBillLinks:666-672`+`reverseProcess:161` 已用此范式）。M1 已解决（reverse→docStatus=CANCELLED 终态，voucher-status.dict.yaml 含 DRAFT/POSTED/CANCELLED）。无新 BLOCKER。复审发现 1 MAJOR（S1 修订引入：BankLedgerQuery 误写 `ErpFinVoucher.businessDate`，实际头表列为 `voucherDate` orm:240）**已修正**为 voucherDate；1 Minor（BillR 反查用 businessType 字段非 billType，对齐 findBillLinks 范式）**已吸收**。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：导入幂等 + 自动/手工勾对 + 余额调节表 + 未达账项调整凭证 + 期间门控落地，行为测试通过
- [x] 相关文档对齐：`bank-reconciliation.md` 实现 schema 补注；当日日志已记
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（改动模块）；根 `mvn test -fae` 无回归
- [x] 无范围内项目降级为 deferred/follow-up（ErpFinBankLedgerLine 物化视图 / 银行存款外币重估 / 多币种汇兑 / 文件格式解析 / 资金账户对账定时任务 / 勾对 UI / AR-AP 联动 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 保护区域（bank-match-status 字典 + BANK_RECON_ADJ 业务类型 + matchStatus ext:dict 修正）实施前已获人工批准
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将本项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 银行存款外币汇兑重估

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前置条件是 `ErpMdSubject` 科目级币种标记（master-data），属独立结果表面；本计划单币种对账为主。
- Successor Required: yes（触发条件：`ErpMdSubject` 落地科目级币种标记时，承接 1000-3 Deferred）

### `ErpFinBankLedgerLine` 账面流水物化视图

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划用 VoucherLine 按需查询承载账面流水，勾对状态单点持久在 BankStatementLine；物化视图的性能收益在当前数据量下不必要。
- Successor Required: yes（触发条件：单账户凭证行量 ≥ 数万、按需查询性能不足时）

### 对账单导入文件格式解析（MT940/CSV/Excel）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划交付导入入口（接收已解析行集合）；外部文件解析属集成层。
- Successor Required: yes（触发条件：接入具体银行文件格式时）

### 资金账户余额定期对账兜底任务（nop-job）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计 §业务规则7 定期对账兜底；属定时任务，归 1600-1 batch 架构接线。
- Successor Required: yes（触发条件：接线资金账户对账定时任务时）

## Closure

Status Note: 计划已全部 4 个 Phase 执行完毕，19 个新增测试全绿，全仓 `mvn clean install -DskipTests` + finance 模块 `mvn test` 通过，无回归。结束审计由独立子代理（新会话，无执行者上下文）执行并通过——执行者不再以「自我审计」或「人工门控占位符」名义保留未勾选项。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（opencode independent closure auditor，新会话，不重用执行者上下文）。独立复核对照实时仓库：`module-finance/model/app-erp-finance.orm.xml:117,759`（bank-match-status 字典 + matchStatus ext:dict 修正落地）、`ErpFinBusinessType` 枚举含 `BANK_RECON_ADJ(320)`、`erp-fin/business-type.dict.yaml` 含 string code `BANK_RECON_ADJ`、`_ErpFinBankStatementLine.xmeta` dict 指向 `erp-fin/bank-match-status`、4 个测试类（TestErpFinBankStatementImport / Match / Reconciliation / EndToEnd）存在、`docs/logs/2026/07-05.md` 与 `docs/design/finance/bank-reconciliation.md` 实现 schema 补注存在。
- Evidence:
  - 新增测试 19 case 全绿：`TestErpFinBankStatementImport`(6) + `TestErpFinBankStatementMatch`(7) + `TestErpFinBankReconciliation`(5) + `TestErpFinBankReconciliationEndToEnd`(1)
  - finance-service `mvn test`：Tests run: 145, Failures: 0, Errors: 0, Skipped: 0
  - 全仓 `mvn clean install -DskipTests`：BUILD SUCCESS（含 codegen 增量重生成 dict + xmeta 产物）
  - `docs/logs/2026/07-05.md`：执行当日日志已记（含验证状态）
  - `docs/design/finance/bank-reconciliation.md`：实现权威 schema 补注完成（与设计草图偏离处说明）
  - 独立结束审计复核：所有 Phase Status=completed、Exit Criteria 全 `[x]`、Closure Gates 全 `[x]`、Deferred 项均为计划内 Non-Goal（无范围内缺陷降级）；Anti-Hollow 复核通过（BankStatementImporter/BankStatementMatcher/BankReconciliationBuilder/BankReconAdjustmentVoucherBuilder 经 BizModel `@BizMutation` 与 IErpFinVoucherBiz.post/reverse 运行时可达，非空壳）

Follow-up:

- 银行存款外币汇兑重估（见上方 Deferred，承接 1000-3）
- ErpFinBankLedgerLine 物化视图（见上方 Deferred）
- 对账单文件格式解析（见上方 Deferred）
- 资金账户对账定时任务（见上方 Deferred）
