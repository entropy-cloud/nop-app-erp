# 2026-07-02-1000-1-finance-treasury-notes 资金/票据（承兑汇票/贴现/授信/现金预测）

> Plan Status: active
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.8（费用报销/票据/资金——本计划覆盖「票据/资金」子面；费用报销+员工借款已于计划 0700-2 完成）；`docs/design/finance/treasury.md`
> Related: `2026-07-02-0700-2-finance-expense-claim-employee-advance.md`（同工作项 1.8 前序子面，显式将票据/资金 deferred 至本计划）、`2026-07-02-0300-3-ar-ap-settlement-subledger.md`（ErpFinArApItem/ErpFinReconciliation 核销机制，票据收到/背书复用）、`2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（过账 Provider 基础设施）
> Mission: erp
> Work Item: 资金/票据 BizModel + 业财过账（1.8 票据/资金子面，extended M2 触发的 M1 deferred 闭环）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **目标实体不存在**：全仓 `rg "ErpFinNotesReceivable|ErpFinNotesPayable|ErpFinNotesDiscount|ErpFinCreditFacility|ErpFinCashForecast"` 在 `*.orm.xml` 零命中。`module-finance/model/app-erp-finance.orm.xml` 现有 30 余实体（含 0700-2 新增的 ExpenseClaim/ExpenseClaimLine/EmployeeAdvance），**无票据/贴现/授信/现金预测实体**。`treasury.md` 的实体为「建议命名，待 ORM 计划落地」。
- **资金账户已存在**：`ErpFinFundAccount`（`finance.orm.xml:616`，accountType/bankName/bankAccount/currencyId/subjectId/currentBalance），票据的 bankId/discountId/creditFacilityId 可 FK 到此或新实体。`ErpFinBankStatement(+Line)`/`ErpFinBankReconciliation(+Line)` 已存在（银行对账，与票据解耦，见 `bank-reconciliation.md:11`）。
- **业财业务类型枚举**：`ErpFinBusinessType`（`erp-fin-dao/.../ErpFinBusinessType.java`）现 18 常量，max=`EMPLOYEE_ADVANCE_SETTLE(180)`。treasury.md 定义 7 新类型（NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION、NOTES_PAYABLE_ISSUED/HONORED、CREDIT_FACILITY_INTEREST），码段 190–250 连续无冲突。
- **过账基础设施**（0811-1/2030-1/0300-2/0700-2 已落地）：入口 facade `IErpFinVoucherBiz.post`/`reverse`；`IErpFinAcctDocProvider` SPI + `ErpFinAcctDocRegistry`；共享执行器模式（executor 无 `@Transactional`）。新 Provider 注册 `app-service.beans.xml`，产 facts 调 `IErpFinVoucherBiz.post`。0700-2 的 `ExpenseClaimPostingDispatcher`/`EmployeeAdvancePostingDispatcher` 为 finance 自有域派发器范式。
- **辅助账/核销**（0300-3 已落地）：`ErpFinArApItem`（应收应付明细账）+ `ErpFinReconciliation`(+Line)（核销单，create/post/reverse + 5 项约束）。**关键约束（草案审查核实）**：`ErpFinReconciliationBizModel` 的核销机制**消费已存在的 ArApItem**（`create()` 取 `invoiceItemId`/`paymentItemId` 引用已持久化项，`validateLine` 加载双方为 ArApItem 并强制**同 direction**）——它**不创建** ArApItem。故票据侧 ArApItem **必须由 `ErpFinArApItemGenerator.resolveProfile` 生成**（`resolveProfile` 返回 null 则不生成、无可核销项）。当前 `resolveProfile`（switch on businessType）识别 AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT/PURCHASE_RETURN/SALES_RETURN/EXPENSE_CLAIM/EMPLOYEE_ADVANCE，**default→null**。票据「收到抵客户应收」（RECEIVED，抵 AR_INVOICE，同 direction=RECEIVABLE）与「背书抵供应商应付」（ENDORSED，抵 AP_INVOICE，同 direction=PAYABLE）方向均匹配，须在 `resolveProfile` 增 NOTES_RECEIVABLE_RECEIVED→RECEIVABLE 与 NOTES_RECEIVABLE_ENDORSED→PAYABLE 两个 case 生成票据侧 ArApItem，方可经 Reconciliation 核销。
- **往来单位主数据**：`ErpMdPartner`（supplier/customer/partnerType），`ErpMdPartner.receivableBalance`/`payableBalance` 余额缓存（0300-3 由辅助账驱动）。
- **现金预测派生**：`treasury.md` 明确 `ErpFinCashForecast` 由定时任务（nop-job）聚合 AR/AP/票据到期数据生成，非手工录入。nop-job 接线属运营自动化基线（全仓 `rg "nop-job|@Scheduled"` 在 service 层未落地）。
- **证据诚实性**：`treasury.md §证据诚实性声明` 明确「4 个开源 ERP 均无中式承兑汇票实体」，票据全生命周期属 ⚪ 领域常识（依据中国《企业会计准则》），开源零覆盖。可借鉴仅为 Metasfresh `Doc_BankStatement` 的科目分解范式（银行对账单处理，非票据）。
- **剩余差距**：(1) 无应收/应付票据/贴现/授信/现金预测实体与状态机；(2) 无 7 类票据 businessType 与凭证；(3) 票据收到/背书未与 AR/AP 辅助账核销联动；(4) 授信额度强一致校验未落地；(5) 现金预测无派生机制（nop-job 未接线）。

## Goals

- ORM 加性新增 `ErpFinNotesReceivable`（应收票据，7 态状态机）+ `ErpFinNotesPayable`（应付票据）+ `ErpFinNotesDiscount`（贴现明细）+ `ErpFinCreditFacility`（授信额度）+ `ErpFinCashForecast`（现金预测，物化视图）五实体（finance 源模型，重新 codegen），含多币种四件套 + 业财三件套(posted)。
- 应收票据 `IErpFinNotesReceivableBiz` 状态机（receive→discount/endorse/collection→honored/dishonored/writeOff）+ 业财过账（NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION 四类型），对齐 `treasury.md §状态机`。
- 应付票据 `IErpFinNotesPayableBiz` 状态机（issue→honored/dishonored/writeOff）+ 业财过账（NOTES_PAYABLE_ISSUED/HONORED 两类型）。
- 贴现凭证科目分解五件套（借银行存款实得/借财务费用-利息支出贴现息/[借/贷]汇兑损益外币/贷应收票据票面），借 Metasfresh `Doc_BankStatement` 范式。
- **授信额度强一致校验**：开银承（notesType=BANK_ACCEPTANCE）前同步校验 `creditFacility.availableAmount >= 票面`，不足抛 `NopException`（`treasury.md §关键业务规则 1`）。
- 票据收到抵客户应收、背书抵供应商应付，经 `ErpFinReconciliation`（同方向核销）联动 AR/AP 辅助账。
- 现金预测 `ErpFinCashForecast` 提供**手动触发的批量聚合方法**（不依赖 nop-job 定时；提供 `refreshForecast(fromDate,toDate)` BizMutation，聚合 AR/AP/票据到期数据），nop-job 接线归 Follow-up。
- 行为测试覆盖五实体状态机/七凭证/贴现科目分解/授信校验/票据核销联动/红字冲减。

## Non-Goals

- **费用报销/员工借款**：已于计划 0700-2 完成，不在本计划范围。
- **银行对账（ErpFinBankReconciliation）/资金账户余额维护**：属 `bank-reconciliation.md`，票据与银行对账解耦（`bank-reconciliation.md:11`）。票据贴现的资金到账后**不**自动驱动银行对账。
- **nop-job 定时自动生成现金预测**：nop-job 定时基线未落地；本计划提供手动触发的 `refreshForecast` 方法，定时调度归 Follow-up（触发条件：nop-job 接线时）。
- **多级审批人路由（nop-wf）**：票据注销（WRITE_OFF）审批 = 直接状态迁移 + `@BizMutation` + 配置门控（对齐全仓基线），不接入 nop-wf 工作流引擎。
- **拒付转应收的完整追索流程（诉讼/坏账）**：DISHONORED 仅标记终态并（应收票据）转挂应收账款科目；后续催收/坏账核销属信用管理面，归 Follow-up。
- **授信利息自动计提定时任务**：CREDIT_FACILITY_INTEREST 过账类型登记，但贷款利息的定期自动计提归运营自动化（nop-job），本计划仅支持手动触发利息凭证。
- **现金流量表/资金报表 UI（nop-report）**：仅提供 `ErpFinCashForecast` 数据与查询，不做 nop-report 报表渲染。
- **电子票据（ECDS）集成/票据影像附件**：属外部集成，归 Follow-up。

## Task Route

- Type: `implementation-only change`（业务逻辑 + ORM 模型加性增量）。**注**：触及 ask-first 保护区域 `module-finance/model/app-erp-finance.orm.xml`（新增 5 实体 + 新字典）。延续 0700-2 在 finance 源模型新增实体的既有先例，Phase 1 含 codegen 回归。
- Owner Docs: `docs/design/finance/treasury.md`（实体/状态机/过账/授信/证据声明）、`docs/design/finance/posting.md`（Provider 三层 + 冲销）、`docs/design/finance/ar-ap-reconciliation.md`（核销机制复用约束——同 direction 强制）、`docs/design/finance/bank-reconciliation.md:11`（票据与银行对账解耦）。
- Skill Selection Basis: ORM 实体增量 + BizModel 状态机 + 过账 Provider + 跨实体辅助账/核销 + 错误码 → 加载 `nop-backend-dev`（覆盖 IBiz、跨实体访问、过账 Provider、CodeGen 增量回归自检、反模式自检）。
- **Decision（现金预测生成机制）**：**选择**手动触发的批量聚合 `refreshForecast(fromDate,toDate)` BizMutation（聚合 ArApItem 到期 + 票据到期），不依赖 nop-job。**替代**：① 等 nop-job 接线后做定时自动（阻塞本计划，且 nop-job 未落地，rejected）；② 仅物化视图不做聚合方法（无法主动刷新，rejected）。**残留风险**：无定时调度需人工触发（运营低频，可接受）。
- **Decision（票据核销经 ErpFinReconciliation vs 直写辅助账）**：**选择**票据收到/背书经 `ErpFinReconciliation`（同方向抵销：应收票据收到抵 AR 应收同 direction=RECEIVABLE；背书抵 AP 应付同 direction=PAYABLE，方向匹配 0300-3 同 direction 约束）。**前置**：核销消费已存在的 ArApItem，故票据侧 ArApItem 须经 `ErpFinArApItemGenerator.resolveProfile`（增 RECEIVED→RECEIVABLE、ENDORSED→PAYABLE 两 case）先生成。**替代**：直写辅助账（0700-2 员工抵扣跨方向无法走 Reconciliation 才直写；票据是同方向，应走正式核销单保持可审计，rejected 直写）。**残留风险**：核销单生成增加事务步骤（低频票据，可接受）。
- **Decision（授信额度占用回写）**：**选择**银承开出时 increment `ErpFinCreditFacility.usedAmount`、兑付/注销时 decrement（同步强一致，`treasury.md §规则1`）。**替代**：派生计算（每次 SUM 未到期银承票面，高频查询性能差，rejected）。**残留风险**：并发开出银承的额度竞争——由强一致校验 + 乐观锁 version 兜底。

## Infrastructure And Config Prereqs

- 配置项（`treasury.md §配置点`）：`erp-fin.credit-check-on-issue`（默认 true，开银承前强制校验授信可用额度）、`erp-fin.notes-discount-rate-default`（默认空，缺省时提示配置）、`erp-fin.notes-writeoff-approval-required`（默认 true，票据注销需审批门控）。经 `AppConfig.var(..., defaultValue)` 读取，缺失走默认，无 .env。
- 模块依赖：`erp-fin-service` 已 compile 依赖 master-data-dao（partner/currency 主数据）。无新增端口/密钥/外部服务/数据迁移（新实体为新建表）。
- **保护区域门控**：D 触及 `model/*.orm.xml`（ask-first）。ORM 阶段实施前须：人工批准 + 本计划草案审查通过。审查者可用性 = `subagent`。

## Execution Plan

### Phase 1 — ORM 实体增量（五实体）+ 字典 + 枚举 + 重新 codegen + 回归

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`（新增 5 实体 + 新字典 `<dict>`）、`erp-fin-dao/.../ErpFinBusinessType.java`（扩 190–250）、经 codegen 重新生成 dao entity/meta/`_app.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: **人工批准**（model/*.orm.xml ask-first）+ 本计划草案审查通过。

- [ ] `Add`：`ErpFinNotesReceivable`（应收票据）—— code/orgId/notesType(dict erp-fin/notes-type: BANK_ACCEPTANCE=10/COMMERCIAL_ACCEPTANCE=20)/notesNo/drawerName/drawerBank/payeeName/issueDate/dueDate/多币种四件套(currencyId/exchangeRate/amountSource/amountFunctional)/partnerId(→ErpMdPartner,出票客户往来)/endorsementFromId(可空,→ErpFinNotesReceivable)/sourceBillType/sourceBillCode/discountId(可空,→ErpFinNotesDiscount)/status(dict erp-fin/notes-receivable-status: RECEIVED/DISCOUNTED/ENDORSED/COLLECTION_PENDING/HONORED/DISHONORED/WRITE_OFF)/posted+postedBy+postedAt + 标准审计。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinNotesPayable`（应付票据）—— code/orgId/notesType/notesNo/payeeName/payeeBank/issueDate/dueDate/多币种四件套/partnerId(→ErpMdPartner,收款方往来)/creditFacilityId(可空,→ErpFinCreditFacility)/sourceBillType/sourceBillCode/status(dict erp-fin/notes-payable-status: ISSUED/HONORED/DISHONORED/WRITE_OFF)/posted+postedBy+postedAt + 标准审计。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinNotesDiscount`（贴现明细）—— id/notesReceivableId(→ErpFinNotesReceivable)/orgId/discountDate/bankId(→ErpFinFundAccount)/faceAmount/discountInterest/netAmount/currencyId/exchangeRate/exchangeGainLoss/posted+postedBy+postedAt + 标准审计。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinCreditFacility`（授信额度）—— code/orgId/fundAccountId(→ErpFinFundAccount,授信银行账户；银行信息由 FundAccount.bankName 承载，不另设 bankId)/facilityType(dict erp-fin/credit-facility-type: BANK_ACCEPTANCE_LINE=10/LOAN_LINE=20)/totalAmount/usedAmount(派生,持久化)/availableAmount(派生,=total−used)/validFrom/validTo/status + 标准审计（含 version 乐观锁列）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinCashForecast`（现金预测，物化视图）—— id/orgId/fundAccountId/forecastDate/sourceBillType/sourceBillCode/direction(dict erp-fin/cash-flow-direction: INFLOW=10/OUTFLOW=20)/partnerId/amountSource/amountFunctional + 标准审计。
  - Skill: `nop-backend-dev`
- [ ] `Add`：新字典 `erp-fin/notes-type`、`erp-fin/notes-receivable-status`、`erp-fin/notes-payable-status`、`erp-fin/credit-facility-type`、`erp-fin/cash-flow-direction`（ORM `<dict>` 或 meta dict 源，按现仓字典生成范式）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinBusinessType` 追加 `NOTES_RECEIVABLE_RECEIVED(190)`/`NOTES_RECEIVABLE_DISCOUNTED(200)`/`NOTES_RECEIVABLE_ENDORSED(210)`/`NOTES_RECEIVABLE_COLLECTION(220)`/`NOTES_PAYABLE_ISSUED(230)`/`NOTES_PAYABLE_HONORED(240)`/`CREDIT_FACILITY_INTEREST(250)`；`business-type.dict.yaml` 同步。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：重新 codegen 后，finance 既有 CRUD + 0300-3 辅助账/核销 + 0700-2 报销/借款套件全绿（加性新实体不破坏既有）；本地化 `mvn test -pl module-finance/erp-fin-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 5 新实体 + 字典 + 枚举 + codegen 无回归（既有 finance 套件绿）。解除 Phase 2/3 对实体与业务类型的阻塞。

- [ ] 五实体 + 字典 + 枚举落库（codegen 产物更新）；既有 finance 测试无回归

### Phase 2 — 应收/应付票据状态机 + 授信强一致校验 + 配置

Status: planned
Targets: `module-finance/erp-fin-service/.../entity/ErpFinNotesReceivableBizModel.java`(新)、`.../entity/ErpFinNotesPayableBizModel.java`(新)、`.../biz/IErpFinNotesReceivableBiz.java`/`IErpFinNotesPayableBiz.java`(新)、`.../biz/IErpFinCreditFacilityBiz.java`(新,额度占用回写)、`.../ErpFinErrors.java`(扩)、`.../ErpFinConstants.java`(扩 SOURCE_BILL_NOTES_*)、`app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（实体/字典/枚举存在）。

- [ ] `Add`：`IErpFinNotesReceivableBiz` 声明状态机契约 receive/discount/endorse/collect/honor/dishonor/writeOff（`@BizMutation`+`@Name`）。`ErpFinNotesReceivableBizModel` 实现 7 态迁移：RECEIVED→DISCOUNTED/ENDORSED/COLLECTION_PENDING；COLLECTION_PENDING→HONORED/DISHONORED；任何非终态→WRITE_OFF（配置门控 `erp-fin.notes-writeoff-approval-required`）。每迁移校验前置态，违例抛 `NopException`+`ErpFinErrors` 作用域码。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpFinNotesPayableBiz` 声明状态机契约 issue/honor/dishonor/writeOff。`ErpFinNotesPayableBizModel` 实现：ISSUED→HONORED/DISHONORED；任何非终态→WRITE_OFF。
  - Skill: `nop-backend-dev`
- [ ] `Add`：**授信额度强一致校验**——`ErpFinNotesPayableBizModel.issue` 在 notesType=BANK_ACCEPTANCE 时，若 `erp-fin.credit-check-on-issue=true`，注入 `IErpFinCreditFacilityBiz`，校验 `creditFacility.availableAmount >= 票面`，不足抛 `NopException`；校验通过 increment `usedAmount`（乐观锁 version 兜底并发竞争）。honor/writeOff 时 decrement。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：授信额度并发竞争处理——**选择**乐观锁 version（`ErpFinCreditFacility` 将含标准 version 审计列）+ 重试。**替代**：悲观锁（降低并发，rejected）。**残留风险**：高并发开出银承时重试失败（运营低频票据，可接受）。
  - Skill: none
- [ ] `Add`：贴现计算——`ErpFinNotesReceivableBizModel.discount(discountDate, bankId, discountRate)` 生成 `ErpFinNotesDiscount`：discountInterest=票面×贴现率×剩余天数/360，netAmount=票面−贴现息；置 NotesReceivable.status=DISCOUNTED + discountId。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpFinNotesReceivableStateMachine`/`TestErpFinNotesPayableStateMachine`（状态迁移正向/反向/非法迁移；授信额度不足拒绝；银承占用/释放额度）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinNotes*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付两票据状态机 + 授信强一致校验 + 贴现计算（不含过账凭证，Phase 3）。解除 Phase 3 对状态机的阻塞。

- [ ] 两票据状态迁移 + 授信校验单测通过；贴现计算正确

### Phase 3 — 过账（7 业务类型 + Provider + 派发器）+ 票据核销联动 AR/AP + 现金预测聚合 + 红字冲减

Status: planned
Targets: `module-finance/erp-fin-service/.../posting/NotesReceivableAcctDocProvider.java`(新)、`.../posting/NotesPayableAcctDocProvider.java`(新)、`.../posting/NotesPostingDispatcher.java`(新,复用共享 executor)、`.../ErpFinArApItemGenerator.java`(扩 resolveProfile NOTES_*)、`.../ErpFinCashForecastBizModel.java`(新,refreshForecast 聚合)、`erp-fin-service/.../_vfs/erp/fin/beans/app-service.beans.xml`、`ErpFinConstants.java`(扩 SOURCE_BILL_*)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2（状态机/授信/贴现）；过账基础设施（0811-1/2030-1）+ 辅助账（0300-3）已完成。

- [ ] `Add`：`NotesReceivableAcctDocProvider` 产四类型 facts——RECEIVED：借应收票据/贷应收账款（抵客户欠款）；DISCOUNTED：借银行存款(netAmount)/借财务费用-利息支出(discountInterest)/[借/贷]汇兑损益(exchangeGainLoss)/贷应收票据(faceAmount)（科目分解五件套）；ENDORSED：借应付账款(抵供应商)/贷应收票据；COLLECTION：借银行存款/贷应收票据。注册 beans。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`NotesPayableAcctDocProvider` 产两类型 facts——ISSUED：借应付账款/贷应付票据；HONORED：借应付票据/贷银行存款。注册 beans。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`NotesPostingDispatcher`（复用共享 executor，无 `@Transactional`；dispatcher 置 finance 自有域，票据状态迁移边界触发）——状态迁移后组装 PostingEvent(businessType, billHeadCode, billData 含 PARTNER_ID/TOTAL_AMOUNT/CURRENCY_ID/DEPARTMENT_ID + 贴现明细 discountInterest/netAmount/exchangeGainLoss + orgId + acctSchemaId) 调 `IErpFinVoucherBiz.post`；成功置 posted=true，失败吞异常保持原态+posted=false（对齐 0700-2 合约）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：**票据核销联动 AR/AP**——应收票据 RECEIVED 时经 `IErpFinReconciliationBiz` 建核销单抵客户 AR_INVOICE 应收（同 direction=RECEIVABLE）；背书 ENDORSED 时抵供应商 AP_INVOICE 应付（同 direction=PAYABLE）。**关键**：核销机制消费已存在的 ArApItem（不创建），故票据侧 ArApItem **必须由 `ErpFinArApItemGenerator.resolveProfile` 生成**——增两个 case：`NOTES_RECEIVABLE_RECEIVED`→`new SourceProfile(DIRECTION_RECEIVABLE, SOURCE_BILL_NOTES_RECEIVABLE)`、`NOTES_RECEIVABLE_ENDORSED`→`new SourceProfile(DIRECTION_PAYABLE, SOURCE_BILL_NOTES_ENDORSED)`（RECEIVED 生成应收侧项抵 AR；ENDORSED 生成应付侧项抵 AP）。`resolvePartnerId` 增 `PARTNER_ID` 直接采用（billData 已携带 partnerId）。保持既有 case 纯加性不动。`ErpFinConstants` 扩 SOURCE_BILL_NOTES_RECEIVABLE/SOURCE_BILL_NOTES_ENDORSED。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinCashForecastBizModel.refreshForecast(fromDate,toDate)`——批量聚合 ArApItem 未核销到期项（INFLOW=应收到期/OUTFLOW=应付到期）+ 票据到期项（应收票据到期 INFLOW/应付票据到期 OUTFLOW），写入 `ErpFinCashForecast`（先清区间再写入）。`@BizMutation`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`reverse`/`writeOff` 对已 posted=true 票据——调 `IErpFinVoucherBiz.reverse` 红字冲销；已核销票据回滚核销单（reverse）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：贴现息科目归属——**选择**走「财务费用-利息支出」（`treasury.md §规则2`），不冲减应收票据。**替代**：冲减应收票据（虚减费用虚增资产，反模式，rejected）。**残留风险**：无（符合会计准则）。
  - Skill: none
- [ ] `Proof`：`TestErpFinNotesReceivablePosting`（RECEIVED→抵 AR 凭证+核销；DISCOUNTED→五件套科目分解凭证；ENDORSED→抵 AP 凭证+核销；COLLECTION→承兑凭证）、`TestErpFinNotesPayablePosting`（ISSUED→应付票据凭证+授信占用；HONORED→兑付凭证+授信释放）、`TestErpFinCashForecastRefresh`（refreshForecast 聚合 AR/AP/票据到期）、红字冲减单测。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinNotes*,TestErpFinCashForecast*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付过账端到端（状态迁移→凭证+核销联动）+ 现金预测聚合 + 红字冲减。完整仓库验证属 Closure Gates。

- [ ] 七票据凭证落库 + posted=true；票据核销联动 AR/AP；现金预测 refreshForecast 正确；reverse→红冲 + 核销回滚
- [ ] 端到端（收到票据→贴现→托收承兑 / 开应付票据→兑付）单测全绿

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dfb6f842ffep070eFjd3klH1m`，独立 general 子代理，对照实时仓库复核）。1 BLOCKER（B1）：Phase 3 票据核销项「或由核销驱动」不可行——`ErpFinReconciliationBizModel` 消费已存在 ArApItem 而非创建，票据侧 ArApItem 须经 `ErpFinArApItemGenerator.resolveProfile` 生成；且仅提 NOTES_RECEIVABLE_RECEIVED 漏 ENDORSED（背书抵 AP 须 PAYABLE 侧 ArApItem）。S 级 nit：实体计数 32→30+、NotesReceivable.partnerId 误标 ErpMdEmployee 应为 ErpMdPartner、CreditFacility version 前向引用、bankId/fundAccountId 冗余、`>>` 重复。**已修订**：Current Baseline 补核销机制消费-不创建事实 + 两 case（RECEIVED→RECEIVABLE/ENDORSED→PAYABLE）；Decision 补前置；Phase 3 改 resolveProfile 增两 case 生成票据侧 ArApItem；partnerId→ErpMdPartner；CreditFacility 去冗余 bankId 用 fundAccountId + version 审计列；实体计数 30+；`>>` 修正。
- Independent draft review iteration 2: <待独立子代理复审>

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成：五实体状态机 + 七凭证 + 贴现科目分解 + 授信强一致 + 票据核销联动 + 现金预测聚合 + 红字冲减，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` 1.8 标注票据/资金子面完成；当日日志已记；`treasury.md` 偏离（现金预测手动触发/拒付追索/电子票据 Non-Goal）补注
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（改动模块）
- [ ] 无范围内项目静默降级（银行对账/nop-job/nop-wf/拒付追索/电子票据均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 保护区域（model/*.orm.xml）实施前已获人工批准
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### nop-job 定时自动生成现金预测

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: nop-job 定时基线未落地；本计划提供手动触发的 refreshForecast 方法满足主动刷新需求。
- Successor Required: yes（触发条件：nop-job 接线时接入定时调度）

### 拒付转应收的完整追索/坏账核销

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: DISHONORED 仅标记终态并转挂应收账款科目；后续催收/诉讼/坏账属信用管理面。
- Successor Required: yes（触发条件：信用管理/坏账模块启动时）

### 电子票据（ECDS）集成/票据影像附件

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属外部系统集成，核心票据生命周期（纸质/电子同构）已覆盖。
- Successor Required: yes（触发条件：对接 ECDS 或票据影像系统时）

## Closure

Status Note: <待草案审查 + 各阶段执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理>
- Evidence: <task id / log link>

Follow-up:

- nop-job 定时现金预测（见上方 Deferred）
- 拒付追索/坏账核销（见上方 Deferred）
- 电子票据 ECDS 集成（见上方 Deferred）
