# 2026-07-02-0300-3-ar-ap-settlement-subledger

> Plan Status: active
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.6/1.7 闭环段（核销）+ M4 前置；`docs/design/finance/ar-ap-reconciliation.md`（核销/辅助账/账龄）
> Related: `2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（AP 文档流 + 域级核销，前置）、`2026-07-02-0300-2-sales-invoice-receipt-bizmodel.md`（AR 文档流 + 域级核销，前置）
> Mission: erp
> Work Item: AR/AP 辅助账（ErpFinArApItem）生成 + 正式核销单（ErpFinReconciliation）+ 往来余额/账龄
> Audit: required

## Current Baseline

实时仓库已核实的事实（逐一打开 ORM/字典确认）：

- **应收应付辅助账** `ErpFinArApItem`（`module-finance/model/app-erp-finance.orm.xml:368`）：`direction`(erp-fin/ar-ap-direction RECEIVABLE=10/PAYABLE=20)、`sourceBillType`(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT)、`sourceBillCode`、`partnerId`、`acctSchemaId`、`businessDate`/`dueDate`、金额族 `amountSource`/`amountFunctional` + `settledAmountSource`/`settledAmountFunctional` + `openAmountSource`/`openAmountFunctional`、`status`(erp-fin/ar-ap-status OPEN=10/PARTIAL=20/SETTLED=30/CANCELLED=40)、`periodId`。
- **核销单头** `ErpFinReconciliation`(:414)：`direction`、`partnerId`、`acctSchemaId`、`totalAmountSource`/`totalAmountFunctional`、`fxGainLoss`、`docStatus`(erp-fin/reconciliation-status DRAFT=10/POSTED=20/REVERSED=30)。
- **核销单行** `ErpFinReconciliationLine`(:453)：`paymentItemId`→`ErpFinArApItem`(付款/收款项) × `invoiceItemId`→`ErpFinArApItem`(发票项) × `settledAmountSource`/`settledAmountFunctional`。
- **往来余额缓存** `ErpMdPartner.receivableBalance`/`payableBalance`（DECIMAL 20,4，`module-master-data/.../orm.xml:301-302`）。
- **过账事件流**：AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT 经 `PostingEvent`(billType+businessType+billHeadCode+billData) 流入 finance（`IErpFinVoucherBiz.post`）—— 0300-1/0300-2 落地后这些事件已在 finance 侧可观察，为 ArApItem 生成提供数据源（依赖方向正确：purchase/sales → finance 经事件）。
- **BizModel 空壳**：codegen 已生成 `ErpFinArApItemBizModel`/`ErpFinReconciliationBizModel`/`ErpFinReconciliationLineBizModel` 空 `CrudBizModel`（与 0811-1 核实的 finance 服务结构一致）。
- **域级核销已存在（0300-1/0300-2）**：`ErpPurPaymentLine`(paymentId→invoiceId+amount) 与 `ErpSalReceiptLine`(receiptId→invoiceId+amount) 是 purchase/sales 域级核销载体，回写发票 paidStatus/receivedStatus。**与 finance 辅助账/核销单存在双面关系**（见 Decision）。
- **剩余差距**：(1) ArApItem 未在发票/收付款过账时生成（辅助账为空）；(2) 无 ErpFinReconciliation 核销 BizModel；(3) 往来余额缓存未由辅助账驱动；(4) 无账龄查询。

## Goals

- 发票/付款/收款审核过账时，finance 侧自动生成 `ErpFinArApItem` 辅助账（AP_INVOICE/AR_INVOICE 生成应付/应收正项；PAYMENT/RECEIPT 生成付款/收款项），维护 `openAmount` 与 `status`。
- `IErpFinReconciliationBiz` 核销单 BizModel（create + post + reverse）：核销行将付款/收款项与发票项多对多匹配，post 后回写 ArApItem `settledAmount`/`openAmount`/`status`(OPEN→PARTIAL→SETTLED)，并维护 `ErpMdPartner.payableBalance`/`receivableBalance`。
- 核销约束（`ar-ap-reconciliation.md §业务规则`）：同方向、同往来单位、金额不超未核销余额、双方已过账（来源单据 posted=true）。
- 账龄查询（read-only，按 invoice_date/due_date 基准，`erp-fin.ar-aging-base`/`ap-aging-base` 配置）。

## Non-Goals

- **域级核销（ErpPurPaymentLine/ErpSalReceiptLine）的移除/统一**：0300-1/0300-2 已落地 purchase/sales 域级核销（回写 paidStatus/receivedStatus），作为运营核销权威保留；本计划的 ErpFinReconciliation 是 finance 域正式核销单（GL/账龄视角），二者关系由 Decision 裁定，不在本计划移除域级核销。
- **自动核销规则（按金额/比例/账龄/到期日）与定时自动核销任务（nop-job）**：`ar-ap-reconciliation.md §自动核销`，属运营自动化，需 nop-job 接线。
- **多币种汇兑损益核销凭证**：`ErpFinReconciliation.fxGainLoss` 字段已存在，但汇兑差异计算 + 汇兑损益凭证（EXCHANGE_GAIN_LOSS）属期末汇兑调整面（period-close/exchange），本计划只记录 fxGainLoss 占位计算（若配置），完整汇兑损益核算留 follow-up。
- **银行对账（ErpFinBankReconciliation）/资金账户（ErpFinFundAccount）**：属 `bank-reconciliation.md`/`treasury.md`。
- **核销凭证（核销本身生成 GL 凭证）**：`ar-ap-reconciliation.md` 指明「核销不直接生成凭证，凭证由收付款审核时生成」；本计划遵循，核销只影响辅助账/余额。
- **账龄报表 UI / 账龄定时快照**：仅提供查询能力，不做 nop-report 报表与定时快照物化。

## Task Route

- Type: `architecture change`（新增跨域辅助账生成机制 + finance 核销面；ArApItem 生成涉及在过账管线中挂接 finance 侧步骤，属跨结果表面的机制决策）。
- Owner Docs: `docs/design/finance/ar-ap-reconciliation.md`（核销模型/流程/约束/账龄）、`docs/design/finance/posting.md`（过账管线 + Provider/Validator 扩展点）、`docs/architecture/data-dependency-matrix.md`（跨域依赖方向）。
- Skill Selection Basis: 涉及 finance BizModel/IBiz + 跨域辅助账生成机制（过账管线挂接点决策）+ 错误码 + 事务 → `nop-backend-dev`；ArApItem 生成点的架构决策（过账管线内 vs 独立 listener）需对照 `processor-extension-pattern.md`。

## Infrastructure And Config Prereqs

- 核销配置项（`ar-ap-reconciliation.md §配置项`）：`erp-fin.ar-aging-base`(默认 due_date)、`erp-fin.ap-aging-base`(默认 due_date)、`erp-fin.reconcile-precision`(默认 0.01)、`erp-fin.allow-over-reconcile`(默认 false)。经 `AppConfig.var` 读取。无外部服务/端口/密钥。
- 依赖：0300-1/0300-2 必须先完成（AP/AR 发票 + 收付款过账事件已在 finance 可观察）。finance-service 已是 DAG 顶层，无新增模块依赖方向问题。
- 无数据迁移；ArApItem 表已存在（codegen）。

## Execution Plan

### Phase 1 — ArApItem 辅助账生成（过账管线挂接）

Status: planned
Targets: `module-finance/erp-fin-service/.../posting/ErpFinArApItemGenerator.java`（或扩 `ErpFinPostingProcessor`）、`IErpFinArApItemBiz.java`、`ErpFinArApItemBizModel.java`、`ErpFinErrors.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 0300-1 + 0300-2 完成（PostingEvent for AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT 在 finance 可观察）。

- [ ] `Explore`：ArApItem 生成挂接点探查——候选：(A) 在 `ErpFinPostingProcessor` 过账成功后同事务追加 ArApItem 生成步骤（finance 内部，billData 已含 partnerId/amount/sourceBillType）；(B) 独立 `IErpFinFactsValidator` 复用同管线；(C) post-commit listener。对照 `processor-extension-pattern.md` 评估事务边界（ArApItem 须与凭证同事务强一致 vs 解耦）。产出结论供下方 Decision。
  - Skill: none
- [ ] `Decision`：ArApItem 生成机制——基于 Explore 结论选择挂接点（倾向 A：过账成功后同事务生成，保证「凭证 + 辅助账」强一致，符合 posting.md 三层模型第①层精神）。记录选择、替代方案（B/C）、残留风险（过账失败时 ArApItem 不生成，与 posted=false 一致，可接受）。
  - Skill: none
- [ ] `Add`：`ErpFinArApItemGenerator`——按 `PostingEvent.businessType` 生成 `ErpFinArApItem`：AP_INVOICE/AR_INVOICE → 应付/应收正项（direction=PAYABLE/RECEIVABLE，openAmount=amountFunctional，status=OPEN）；PAYMENT/RECEIPT → 付款/收款项（direction 对应，openAmount=amountFunctional）。从 billData 取 partnerId/amounts/sourceBillCode/acctSchemaId/businessDate。幂等：同 sourceBillCode+sourceBillType 已存在则跳过。
  - **billData 字段契约（与 0300-1/0300-2 的协调依赖）**：账龄（Phase 3）需 `dueDate`。`ErpPurInvoice`/`ErpSalInvoice` 仅有 `businessDate`（发票日期）**无显式 dueDate 列**；故 ArApItem.dueDate 在源单无到期日时落 null（ArApItem.dueDate 可空），账龄基准回退到 invoice_date。0300-1/0300-2 派发器须将 `businessDate` 写入 billData（已在其 billData 清单中）；完整到期日推导（付款条件 businessDate + paymentTerms）属 follow-up。生成器对 billData 缺失 dueDate 容错（null）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpFinArApItemBiz` 查询契约（findOpenItemsByPartner 等 `@BizQuery`，供核销与账龄使用）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpFinArApItemGeneration`——AP_INVOICE 过账后生成 PAYABLE ArApItem(openAmount=发票额, status=OPEN)；RECEIPT 过账后生成 RECEIVABLE 项；幂等（重复过账不重复生成）；红字 reverse 后生成 status=CANCELLED 或反向项（对齐冲销语义）。验证命令 `mvn test -pl module-finance/erp-fin-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] AP/AR 发票 + 收付款过账后 ArApItem 辅助账项可观察（direction/amount/open/status 正确），幂等
- [ ] ArApItem 生成与凭证同事务（过账失败时不生成，与 posted=false 一致）

### Phase 2 — ErpFinReconciliation 核销单 BizModel + 往来余额

Status: planned
Targets: `IErpFinReconciliationBiz.java`、`ErpFinReconciliationBizModel.java`、`ReconciliationSettler.java`、`PartnerBalanceUpdater.java`、`ErpFinErrors`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（ArApItem 已生成）。

- [ ] `Decision`：finance 核销单与域级核销（ErpPurPaymentLine/ErpSalReceiptLine）的关系——记录裁定：finance ErpFinReconciliation 作为 GL/账龄视角的正式核销单（period-end 正式核销），独立作用于 ArApItem；域级核销（0300-1/0300-2）作为运营核销权威（回写 paidStatus/receivedStatus）。二者并行，残留风险（双核销面需对账一致性兜底）记入 Deferred。替代方案（统一为单面）需重构 0300-1/0300-2，超范围。
  - Skill: none
- [ ] `Add`：`IErpFinReconciliationBiz` 契约 `create(post 提交头+行) / post(reconciliationId) / reverse(reconciliationId)`（`@BizMutation`）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpFinReconciliationBizModel`——`post`：校验约束（同 direction、同 partnerId、paymentItem/invoiceItem 均 status≠SETTLED/CANCELLED、核销金额 ≤ 各自 openAmount unless `erp-fin.allow-over-reconcile`、**核销日期不早于发票业务日期**（`ar-ap-reconciliation.md §核销约束` 项4）），调 `ReconciliationSettler` 回写双方 ArApItem `settledAmount`+=amt / `openAmount`−=amt / `status`(OPEN→PARTIAL→SETTLED 按 open vs 0)，置核销单 docStatus=POSTED。精度按 `erp-fin.reconcile-precision`，尾差调整末行。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`PartnerBalanceUpdater`——核销 post 后重算 `ErpMdPartner.payableBalance`/`receivableBalance`（= Σ对应 direction ArApItem openAmount by partner）。注入 `IErpMdPartnerBiz` 或 daoFor（机制 B）更新缓存字段。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`reverse`——生成反向核销（docStatus=REVERSED），恢复 ArApItem settled/open/status，重算 partner 余额（对齐 §核销冲销）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpFinReconciliation`（部分核销 status=PARTIAL、全额 SETTLED、跨 partner 拒绝、超额拒绝、reverse 恢复）、`TestErpFinPartnerBalance`（核销后 partner 余额正确更新）。验证命令 `mvn test -pl module-finance/erp-fin-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 核销 post 后 ArApItem settled/open/status 正确回写；约束违例拒绝；reverse 恢复
- [ ] partner payableBalance/receivableBalance 由辅助账 openAmount 驱动更新

### Phase 3 — 账龄查询 + 文档/日志

Status: planned
Targets: `IErpFinArApItemBiz`(扩查询)、`docs/logs/2026/{07-02}.md`、`docs/backlog/core-business-roadmap.md`、`docs/design/finance/ar-ap-reconciliation.md`(若实现偏离则补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2。

- [ ] `Add`：账龄查询 `@BizQuery`——按 partner 聚合 open ArApItem，按 `erp-fin.ar-aging-base`/`ap-aging-base`(invoice_date/due_date) 计算账龄区间（0-30/31-60/61-90/91-180/180+），返回未核销余额 × 区间。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpFinAging`（不同账龄区间分桶正确，基准日配置切换）。验证命令 `mvn test -pl module-finance/erp-fin-service -am`；根 `mvn test -fae` 无回归。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`docs/backlog/core-business-roadmap.md` 工作项 1.6/1.7 闭环段标注进展（M4 前置就绪）；`docs/design/finance/ar-ap-reconciliation.md` §核销明细表补注——该节描述的扁平 schema（sourceBillId/targetBillId/reversalFlag）与实现的头+行 ORM（ErpFinReconciliation + ErpFinReconciliationLine 的 direction/paymentItemId/invoiceItemId/settledAmount/docStatus）实质偏离，补注实现权威 schema（ORM 为唯一真相源）。
  - Skill: none

Exit Criteria:

- [ ] 账龄查询按配置基准日正确分桶（0-30/.../180+）
- [ ] 当日日志已记；roadmap 1.6/1.7 闭环段进展已标注

## Draft Review Record

- Independent draft review iteration 1: **accept / consensus**（ses_0e1060ab6ffe5ry6oi1rWjqVdx，独立 general 子代理，新会话，与 0300-1/0300-2 审查分离）— 全部 Current Baseline 主张经实时仓库核实属实（ErpFinArApItem/ErpFinReconciliation/ErpFinReconciliationLine 字段与关系逐行核实 @ orm.xml:368-481；字典 ar-ap-direction/ar-ap-status/reconciliation-status 值一致；`ErpMdPartner.receivableBalance`/`payableBalance` DECIMAL 20,4 @ master-data.orm.xml:301-302；`IErpFinVoucherBiz.post`+`PostingEvent`+`ErpFinPostingProcessor`+`IErpFinFactsValidator` 存在；ar-ap-reconciliation.md 约束/配置键/「核销不直接生成凭证」规则一致；空壳 BizModel；roadmap 1.6/1.7 todo+M4 一致）。**无 BLOCKER**。架构健全性核实通过：ArApItem 生成挂接在 finance 过账管线内（读 PostingEvent.billData 快照），**不引入反向依赖**（finance 为 DAG 顶，`data-dependency-matrix.md §4.4`「finance 不回写业务表」得到遵守）；同事务选项 A 与 Facade `post()` 的 REQUIRES_NEW 对齐，残留风险声明准确。`architecture change` 任务类型判定 **justified, leaning architecture**（扩展开跨域共享机制 + 多结果表面结构决策）。规则 9（Phase 1 Explore→Decision 顺序正确，两 Decision 均带备选+残留风险）、规则 4/14（同 owner doc ar-ap-reconciliation.md + 同 ErpFinArApItem 结果表面，合并正确）、反松弛、规则 10（汇兑损益凭证 deferred 合理，fxGainLoss 占位 stub 诚实）、命名/头/Closure Gates 全合规。非阻塞 S1/S2/S3 已吸收：S1 ar-ap-reconciliation.md §核销明细表扁平 schema vs 头+行 ORM 实质偏离，Phase 3 文档补注由条件式改为 firm Add（ORM 为唯一真相源）；S2 billData dueDate 协调依赖（源发票无 dueDate 列，ArApItem.dueDate 容错 null，账龄回退 invoice_date，完整到期日推导 follow-up）；S3 核销日期不早于发票业务日期约束补入 Phase 2。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [ ] 范围内行为完成：ArApItem 辅助账生成 + ErpFinReconciliation 核销 + partner 余额 + 账龄查询落地，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` 1.6/1.7 闭环段标注进展；当日日志已记
- [ ] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（自动核销/汇兑损益凭证/银行对账/核销凭证/账龄报表UI 均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 域级核销（ErpPurPaymentLine/ErpSalReceiptLine）与 finance 核销单（ErpFinReconciliation）双面对账一致性

- Classification: `watch-only residual`
- Why Not Blocking Closure: 二者并行（运营核销 vs GL/账龄核销），Phase 2 Decision 已裁定并行关系。双面 open 余额对账一致性兜底（如日终对账任务）属运营自动化。
- Successor Required: yes（触发条件：出现双面余额不一致或实施日终对账兜底时）

### 自动核销规则（按金额/比例/账龄/到期日）与定时自动核销（nop-job）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 需 nop-job 接线 + 规则配置；MVP 为手工核销单。
- Successor Required: yes（触发条件：接线 nop-job 实施自动核销时）

### 多币种汇兑损益核销凭证（EXCHANGE_GAIN_LOSS）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 完整汇兑差异核算属期末汇兑调整面（period-close/exchange），需跨期汇率；本计划只占位 fxGainLoss。
- Successor Required: yes（触发条件：实施期末汇兑损益调整时）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理（新会话）>

Follow-up:

- 双面核销对账一致性兜底（见上方 Deferred）
- 自动核销规则 / 定时自动核销（见上方 Deferred，需 nop-job）
- 汇兑损益核销凭证（见上方 Deferred，期末汇兑面）
- 账龄报表 UI / 定时账龄快照物化（触发条件：实施 nop-report 报表或日终快照时）
