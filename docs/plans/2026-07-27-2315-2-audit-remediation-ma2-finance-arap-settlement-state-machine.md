# 2026-07-27-2315-2-audit-remediation-ma2-finance-arap-settlement-state-machine MA2 finance 状态机审查 — AR/AP 核销（A2.5c）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A2.5c finance 状态机审查 — AR/AP 核销（S 级拆分 3/3）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.5c）
> Related: `docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`（A2.5a 凭证状态机，done；其 §Deferred But Adjudicated 显式将"AR/AP 核销状态机"交接 A2.5c；过账引擎产出的 AR/AP 凭证与辅助账生成的金额一致性 `ErpFinArApItemGenerator` 经 A2.5a 确认）；`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 P2P，P1-MA2-003 付款核销缺三单匹配 / P2-MA2-008 并发核销 lost-update 已登记供本审计从状态机角度复核）；`docs/plans/2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e.md`（A2.2 O2C，P1-MA2-009 多币种收款核销汇兑损益未实现 / P2-MA2-013 订单维度核销未实现 / P2-MA2-014 并发核销已登记）；`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（A2.3，preCheck 未核销 AR-AP 阻断/提示 + 坏账准备充足性门控交接）；`docs/plans/2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`（P1-MA1-018 enum↔dict 漂移含 EXCHANGE_GAIN_LOSS 待 MR1）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/finance/ar-ap-reconciliation.md`+`bad-debt.md`+`bank-reconciliation.md`+`state-machine.md`（owner doc）
> Audit: required

## Current Baseline

AR/AP 核销是 ERP 确认债权债务清偿的核心环节，保证应收应付辅助账余额与总账一致。核销对象不是源单据而是**辅助账项**（`ErpFinArApItem`）——发票/收付款过账时由 `ErpFinArApItemGenerator` 生成辅助账项，核销在辅助账项层面多对多匹配并回写其 `settledAmount/openAmount/status`。AR/AP 核销状态机横跨三类组件：(1) **辅助账项状态机**（`ErpFinArApItem.status` dict `erp-fin/ar-ap-status`：OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）；(2) **核销单状态机**（`ErpFinReconciliation.docStatus` dict `erp-fin/reconciliation-status`：DRAFT/POSTED/REVERSED 头+行结构）；(3) **坏账核销状态机**（`ErpFinBadDebt` approveStatus + docType WRITE_OFF/RECOVERY，驱动 ArApItem ↔ WRITTEN_OFF 迁移）。owner doc `ar-ap-reconciliation.md`（322 行）定义核销模型/流程/状态/余额/账龄/冲销/汇兑损益核销规则；`bad-debt.md` 定义坏账核销/恢复 + 期末 allowance 充足性门控；`state-machine.md` **无 AR/AP 核销独立状态机章节**（散落在 ar-ap-reconciliation.md）。A2.5a done（凭证状态机）；A2.5b 覆盖期间/预算状态机；本审计 A2.5c 覆盖**AR/AP 核销状态机**。

实时仓库已落地的 AR/AP 核销实现（逐项核实）：

- **辅助账项状态机轴**（`ErpFinArApItem`，`module-finance/model/app-erp-finance.orm.xml:752`）：`status` dict `erp-fin/ar-ap-status`（**5 态**：OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF — WRITTEN_OFF 由 plan 2026-07-05-0540-1 新增）；`direction`（RECEIVABLE/PAYABLE）；`amountSource/Functional` + `settledAmountSource/Functional` + `openAmountSource/Functional`（派生）；`partnerId`/`dueDate`/`acctSchemaId`。
- **辅助账项生成**（`ErpFinArApItemGenerator`）：发票/收付款过账时由过账引擎 `ErpFinPostingProcessor` 调用 Provider 生成辅助账项（AR_INVOICE/AP_INVOICE/RECEIPT/PAYMENT 等业务类型）；A2.5a 已确认与凭证金额一致性。
- **核销单状态机轴**（`ErpFinReconciliation` 头 + `ErpFinReconciliationLine` 行）：头 `docStatus` dict `erp-fin/reconciliation-status`（**3 态**：DRAFT/POSTED/REVERSED）；头 `direction`/`partnerId`/`acctSchemaId`/`businessDate`/`totalAmountSource/Functional`/`fxGainLoss`；行 `paymentItemId`/`invoiceItemId`→`ErpFinArApItem`/`settledAmountSource/Functional`。
- **核销结算器**（`ReconciliationSettler.java`）：
  - `settle():32-47`：过账结算，按核销行回写双方辅助账 settled/open/status，计算核销单头合计。
  - `reverseSettle():52-60`：红冲结算，按原核销行相反数恢复双方金额与状态（settled-=amt / open+=amt / 状态降级）。
  - `applySettlement():62-81`：纯算术回写双方 settledAmount/openAmount/status。
  - `resolveStatus():83-93`：**状态机裁决核心**——`settled<=0`→OPEN / `settled>=total`→SETTLED / else PARTIAL。**CANCELLED 与 WRITTEN_OFF 永不由 ReconciliationSettler 设置**——CANCELLED 入口？WRITTEN_OFF 入口在坏账核销。
- **核销单 BizModel Facade**（`ErpFinReconciliationBizModel.java`）：CRUD + `settle(head, lines):132`（委托 ReconciliationSettler）；校验在 BizModel 编排层（同方向核销/同往来单位/金额校验/日期校验/状态校验 — ar-ap-reconciliation.md §核销约束）。
- **域侧核销器**（双路径）：`ReceiptSettler`（sales `module-sales/erp-sal-service`）`settle():55` + `PaymentSettler`（purchase `module-pur-service`）`settle():55`——经 `ErpSalReceiptBizModel.settle`/`ErpPurPaymentBizModel.settle` @BizMutation 入口，按 `SettlementAllocation`（master-data）分配核销金额到发票项。**与 finance 核销单 ErpFinReconciliation 的关系**：域侧 settle 是否创建/更新 ErpFinReconciliation 头+行，还是直接经 ReconciliationSettler 回写 ArApItem？需核实两条路径的状态机一致性。
- **坏账核销状态机**（`ErpFinBadDebt` + `ErpFinBadDebtProcessor.java` + `ErpFinBadDebtBizModel.java`）：
  - `writeOff():61-71`：`requireOpenArApItem` → 创建 WRITE_OFF 坏账单 → 若 `!isWriteOffApprovalRequired()` 直接 `executeWriteOff`（ArApItem→WRITTEN_OFF + BAD_DEBT_WRITE_OFF 凭证 + APPROVED）。
  - `recover():73-82`：`requireWrittenOffArApItem` → 创建 RECOVERY 坏账单 → 反向（ArApItem WRITTEN_OFF→OPEN）。
  - `submit/approve/reject:86-107`：审批状态机。
  - `reverseApprove():124-164`：反审核红冲闭环——红冲 BAD_DEBT_WRITE_OFF/RECOVERY 凭证 + ArApItem 状态对称回退（writeOff 反向 WRITTEN_OFF→OPEN；recovery 反向 OPEN→WRITTEN_OFF）+ APPROVED→REJECTED。事务边界跟随 Facade @BizMutation，红冲凭证失败抛 NopException 触发事务回滚（强一致）。
  - 坏账单类型 dict `erp-fin/bad-debt-type`（WRITE_OFF/RECOVERY）；审批状态 `erp-fin/approve-status`。
- **票据核销联动 AR/AP**（`ErpFinNotesReceivable`/`ErpFinNotesPayable`）：`writeOff` 红冲 + 辅助账 CANCELLED；票据核销同方向核销联动 AR/AP（TestErpFinNotesReceivablePosting 注释：「票据核销联动 AR/AP（同方向核销）」）。
- **期末结账交互**（A2.3 done）：preCheck `findUnsettledArApCodes` 扫描未核销 AR/AP（status≠SETTLED/CANCELLED/WRITTEN_OFF）——已排除 WRITTEN_OFF（已核销项非"未核销"问题）；坏账准备充足性门控（allowance shortfall 阻断）。
- **测试覆盖**：服务层 `TestErpSalReceiptSettlement`（部分/全额/超额核销守卫）/`TestErpPurPaymentSettlement`/`TestErpSalOrderToCashEnd`（核销链）/`TestErpPurProcureToPayEnd`/`TestErpSalReturnRefund`（预核销）/`TestErpFinBadDebt`（writeOff/recover）/`TestErpFinBadDebtReversal`（反审核红冲闭环 ArApItem 状态对称）/`TestErpFinNotesReceivableStateMachine`+`TestErpFinNotesPayableStateMachine`（票据 writeOff）；浏览器层 `fin-ar-ap-auto-reconciliation`+`fin-bank-reconciliation`+`fin-bad-debt-reverse-*`+`p2p-chain`/`o2c-chain` 核销环节。

**已登记的直指 AR/AP 核销状态机的 MA2 finding（本审计须复核其状态机行为）**：

- `P1-MA2-003`（todo MR1）：付款核销缺发票三单匹配完成态复核——`PaymentSettler.settle` 仅校验发票 approveStatus=APPROVED，不复核三单匹配完成态。**AR/AP 状态机 scope**：核销前置校验与辅助账项状态迁移的关系。
- `P1-MA2-009`（todo MR1）：多币种 O2C 收款核销汇兑损益完全未实现——`SalAcctDocProvider.RECEIPT` 只生成借银行/贷应收同金额，无 6051 汇兑损益 plug。**AR/AP 状态机 scope**：核销时本位币金额折算与辅助账 openAmountFunctional 回写——`ReconciliationSettler.applySettlement` 用 `settledAmountFunctional` 回写，若核销时无 FX 折算则辅助账本位币余额错误。
- `P2-MA2-008`+`P2-MA2-014`（watch-only，A2.17）：`PaymentSettler.settle`/`ReceiptSettler.settle`「读 invoiceBalance→写 PaymentLine→recompute」无悲观/乐观锁，并发核销同一发票可双读双写过付/过收。**AR/AP 状态机 scope**：辅助账项并发回写——`ReconciliationSettler.applySettlement` 无 `@Version`，并发核销同 ArApItem 状态竞态（SETTLED 判定漂移）。
- `P2-MA2-013`（watch-only）：订单维度核销未实现——`SettlementAllocation`+`ReceiptSettler` 仅按 invoiceId 维度，预收款 against order before invoice 未实现。**AR/AP 状态机 scope**：预收/预付辅助账项（无发票对应）的核销路径。
- `P1-MA2-002`（todo MR1）：多币种 P2P 本位币凭证路径未验证——`VoucherFact` 单一 amount 字段。**AR/AP 状态机 scope**：辅助账项 amountFunctional 与凭证 amountFunctional 一致性（A2.5a 已部分复核）。
- `P2-MA1-019`（watch-only）：fromCode 异常类型（与核销无直接关联，顺手复核）。

**但从未做过一次覆盖 AR/AP 核销状态机（辅助账项 + 核销单 + 坏账核销三组件）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：辅助账项 5 态——CANCELLED 是否为真实业务等待点（ReconciliationSettler 永不设置 CANCELLED，入口在哪？票据 writeOff 置 CANCELLED？还是过账红冲？）；WRITTEN_OFF 是终态还是可恢复（坏账 recover/reverseApprove 可 WRITTEN_OFF→OPEN——非终态）；核销单 3 态（DRAFT/POSTED/REVERSED）是否完整（有无 CANCELLED 草稿废弃路径）；坏账单状态机（approveStatus UNSUBMITTED/SUBMITTED/APPROVED/REJECTED × docType WRITE_OFF/RECOVERY 组合的语义清晰性）。
- **转换完整性**：辅助账项每个状态的所有传入/传出转换——OPEN→PARTIAL（部分核销）/ OPEN/PARTIAL→SETTLED（全额核销）/ SETTLED/PARTIAL→OPEN/PARTIAL（reverseSettle 状态降级）/ OPEN→WRITTEN_OFF（writeOff）/ WRITTEN_OFF→OPEN（recover/reverseApprove）/ ???→CANCELLED（**入口不明**）；核销单 DRAFT→POSTED（settle）/ POSTED→REVERSED（reverseSettle）/ DRAFT→?（草稿废弃路径？）；坏账单审批状态机 submit/approve/reject/reverseApprove 转换。
- **终端状态与恢复**：SETTLED（可经 reverseSettle 恢复→非真终态）/ WRITTEN_OFF（可经 recover/reverseApprove 恢复→非真终态）/ CANCELLED（终态？）；REVERSED 核销单是否可再核销（新核销单）；坏账单 APPROVED（可经 reverseApprove→REJECTED）。
- **异常路径**：超额核销（`allow-over-reconcile=false` 拒绝）/ 跨往来单位核销（拒绝）/ 已结账期间核销（拒绝——但守卫在哪？P1-MA2-021 期间侧）/ 并发核销（P2-MA2-008/014 无锁）/ 坏账核销金额超 openAmount（`ERR_BAD_DEBT_WRITE_OFF_AMOUNT_EXCEEDS_OPEN` 守卫）/ recover 非 WRITTEN_OFF 项（`requireWrittenOffArApItem` 守卫）；幂等性（重复核销同核销单）。
- **可达性**：CANCELLED 辅助账项是否可达（ReconciliationSettler 不设置——若仅票据 writeOff 设置，则非票据路径的辅助账项永不可达 CANCELLED，dict 项死状态）；辅助账项从生成（OPEN）到每个终态的可达性；坏账单 REJECTED 后是否可重新 submit。
- **角色与权限**：核销动作（手工核销财务员 / 自动核销配置财务管理员 / 核销冲销财务员+原因）绑定角色；坏账核销 writeOff/recover 审批门控（`write-off-require-approval` config）；危险操作（坏账核销影响报表 / 核销冲销恢复余额）。
- **外部依赖**：辅助账项生成依赖过账引擎 Provider（`ErpFinArApItemGenerator`）；域侧核销器（ReceiptSettler/PaymentSettler）与 finance 核销单（ErpFinReconciliation）的**双路径一致性**——是否两条路径都经 ReconciliationSettler 回写 ArApItem，还是各自直接回写（状态机分歧风险）；票据核销联动 AR/AP（同方向核销）；期末结账 preCheck 扫描未核销项。
- **TODO/任务策略**：未核销 AR/AP 账龄逾期是否产生催收/付款待办（账龄分级风险等级）；坏账核销 APPROVED 待审批是否产生待办；核销冲销是否产生待办；是否存在期望有人行动但不产生待办的状态（长期 OPEN 辅助账项静默下沉——期末仅提示非阻断 P1-MA2-017）。
- **场景演练**：(a) 应收核销快乐路径（AR 发票→收款→核销→ArApItem SETTLED）；(b) 部分核销（OPEN→PARTIAL→继续核销→SETTLED）；(c) 核销冲销（POSTED→REVERSED + ArApItem 状态降级）；(d) 坏账核销（OPEN→writeOff→WRITTEN_OFF + BAD_DEBT_WRITE_OFF 凭证）；(e) 坏账收回（WRITTEN_OFF→recover→OPEN + RECOVERY 凭证）；(f) 坏账反审核（APPROVED→reverseApprove→REJECTED + 红冲凭证 + ArApItem 对称回退）；(g) 多币种核销汇兑损益（当前未实现 P1-MA2-009——核销时本位币折算缺失）；(h) 并发核销（P2-MA2-008/014——双读双写过付）；(i) 票据核销联动（票据 writeOff→辅助账 CANCELLED）。
- **与设计文档一致性**：`ar-ap-reconciliation.md §核销状态` 发票核销状态 4 态（UNRECONCILED/PARTIAL/RECONCILED/OVER）vs dict 5 态（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）——命名与数量均不一致；`§核销状态` 收付款核销状态 3 态（UNRECONCILED/PARTIAL/RECONCILED）vs dict（辅助账项统一）；`§核销冲销` 描述 `reversalFlag=true` 布尔 vs 实现 `docStatus=REVERSED`（owner doc 已注记实现 schema）；`state-machine.md` 无 AR/AP 核销独立状态机章节（散落在 ar-ap-reconciliation.md §核销状态 + bad-debt.md）。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（CANCELLED 辅助账项不可达致 dict 死状态 / 域侧与 finance 双路径核销状态机分歧致辅助账余额不一致 / 坏账核销 reverseApprove 红冲失败致 ArApItem 与凭证悬挂半状态 [强一致回滚是否真覆盖] / 多币种核销辅助账本位币余额错误 [P1-MA2-009 升级评估] / 并发核销辅助账 SETTLED 判定漂移 [P2-MA2-008/014 升级评估]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **AR/AP 辅助账项状态机**（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）+ **核销单状态机**（DRAFT/POSTED/REVERSED 头+行）+ **坏账核销状态机**（ErpFinBadDebt approveStatus × docType WRITE_OFF/RECOVERY + reverseApprove 红冲闭环）做系统性业务审查，产出审计报告。**严格限定 A2.5c scope = AR/AP 核销状态机**；会计凭证状态机归 A2.5a（done）、期间/预算状态机归 A2.5b。
- 重点核验 9 个已识别控制点：(1) 状态定义清晰性（CANCELLED 入口不明 / WRITTEN_OFF 非终态 / 核销单无 CANCELLED 草稿废弃 / 坏账单 approveStatus×docType 组合）；(2) 转换完整性（辅助账项全转换 / 核销单全转换 / 坏账单审批状态机 / 票据核销联动）；(3) 终端与恢复（SETTLED/WRITTEN_OFF 非真终态可恢复 / REVERSED 核销单可再核销 / CANCELLED 终态归属）；(4) 异常路径（超额/跨往来单位/已结账期间/并发/坏账金额超限/recover 非 WRITTEN_OFF/幂等）；(5) 可达性（CANCELLED 可达性 / 坏账 REJECTED 后重提）；(6) 角色权限（核销动作角色 / 坏账审批门控）；(7) 外部依赖（**域侧 ReceiptSettler/PaymentSettler 与 finance ReconciliationSettler 双路径一致性** / 辅助账生成依赖过账引擎 / 票据核销联动 / 期末 preCheck）；(8) TODO/任务策略（账龄催收待办 / 坏账审批待办 / 长期 OPEN 静默下沉）；(9) 场景演练（应收/部分/冲销/坏账核销/收回/反审核/多币种/并发/票据联动）。
- 复核已登记 finding 在 AR/AP 核销状态机运行时的行为影响：P1-MA2-003（付款核销三单匹配前置）/ P1-MA2-009（多币种核销汇兑损益 + 辅助账本位币回写升级评估）/ P2-MA2-008+P2-MA2-014（并发核销辅助账 SETTLED 判定漂移升级评估）/ P2-MA2-013（订单维度核销/预收预付路径）/ P1-MA2-002（辅助账 amountFunctional 与凭证一致性），标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.x finance/AR-AP 核销状态机 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.5c 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.5a 会计凭证状态机 — done；本审计只确认过账引擎产出 AR/AP 凭证与辅助账生成金额一致性（A2.5a 已确认 `ErpFinArApItemGenerator`），不做凭证状态机审查。
- **不**审计 A2.5b 期间/预算状态机 — 期间/预算状态机归 A2.5b。本审计只确认期间 CLOSED_FINAL 时核销是否被阻止（期间侧守卫 P1-MA2-021）。
- **不**审计 A2.1/A2.2 P2P/O2C 端到端编排正确性 — done；本审计只复核核销环节的**辅助账项状态机迁移**正确性。
- **不**审计 A2.3 期末结账链路 — done；本审计只确认 preCheck 未核销项扫描与坏账准备门控的 AR/AP 状态机交互。
- **不**审计 A4.1b finance 代码质量 — 核销/坏账 Processor 代码质量（异常处理/N+1/索引）系统性审查归 A4.1b；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发核销 lost-update 风险归 A2.17；本审计只标注观察到的并发敏感点（P2-MA2-008/014 辅助账并发回写、坏账 reverseApprove 并发）。
- **不**审计银行对账（bank-reconciliation）独立状态机 — 银行勾对状态（bank-match-status UNMATCHED/MATCHED/MANUAL_MATCHED/SUSPENSE）是银行对账子系统的独立状态机，归 owner doc `bank-reconciliation.md`；本审计只确认银行对账与 AR/AP 核销的交互边界（收款核销触发银行勾对）。
- **不**审计 Non-Goal 子项（owner doc 已裁定）：自动核销定时调度（`ar-ap-auto-recon-cron` deferred follow-up）、订单维度核销预收款（P2-MA2-013 successor）、多币种核销汇兑损益实现（P1-MA2-009 MR1 裁决）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/ar-ap-reconciliation.md`（核销模型/流程/状态/余额/账龄/冲销/汇兑损益核销规则 — 权威，**需复核 §核销状态 4 态命名 vs dict 5 态漂移 + reversalFlag vs docStatus=REVERSED**）；`docs/design/finance/bad-debt.md`（坏账核销/恢复 + 期末 allowance 充足性门控 — 坏账状态机权威）；`docs/design/finance/bank-reconciliation.md`（银行对账与核销交互边界）；`docs/design/finance/state-machine.md`（**无 AR/AP 核销独立章节，散落在 ar-ap-reconciliation.md** — 需复核是否应补独立章节）；`docs/design/finance/posting.md`（辅助账生成 ErpFinArApItemGenerator 契约）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.5c 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：会计/财务（AR/AP 辅助账/核销/坏账核销/汇兑损益）与 ORM 模型（`module-finance/model/*.orm.xml` ar-ap-status/reconciliation-status/bad-debt-type 字典）是 ask-first **最高级别**保护区域。P0 即时修复若触及 `ReconciliationSettler`/`ErpFinReconciliationBizModel`/`ReceiptSettler`/`PaymentSettler`/`ErpFinBadDebtProcessor`/ar-ap-status 字典，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（`project-context.md §AI 阻塞条件`）。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - AR/AP 核销状态机系统性业务审查

Status: planned
Targets: `module-finance/erp-fin-service/.../service/reconciliation/ReconciliationSettler.java`（settle:32-47/reverseSettle:52-60/applySettlement:62-81/resolveStatus:83-93）；`.../service/entity/ErpFinReconciliationBizModel.java`（CRUD + settle:132 + 校验编排）；`.../service/processor/ErpFinBadDebtProcessor.java`（writeOff:61-71/recover:73-82/submit/approve/reject:86-107/reverseApprove:124-164/executeWriteOff/executeRecovery）；`.../service/entity/ErpFinBadDebtBizModel.java`（writeOff/recover Facade）；`module-sales/erp-sal-service/.../service/entity/ReceiptSettler.java`（settle:55）+ `ErpSalReceiptBizModel.settle`；`module-purchase/erp-pur-service/.../service/entity/PaymentSettler.java`（settle:55）+ `ErpPurPaymentBizModel.settle`；`module-finance/erp-fin-service/.../service/posting/ErpFinArApItemGenerator.java`（辅助账生成）；`module-finance/erp-fin-service/.../service/processor/ErpFinNotesReceivableProcessor.java`+`ErpFinNotesPayableProcessor.java`（票据 writeOff 联动 AR/AP）；`module-finance/model/app-erp-finance.orm.xml`（ErpFinArApItem:752/ErpFinReconciliation:817/ErpFinBadDebt 字段 + ar-ap-status/reconciliation-status/bad-debt-type 字典）；`docs/design/finance/ar-ap-reconciliation.md`+`bad-debt.md`+`bank-reconciliation.md`+`state-machine.md`；服务层 `TestErpSalReceiptSettlement`+`TestErpPurPaymentSettlement`+`TestErpSalOrderToCashEnd`+`TestErpPurProcureToPayEnd`+`TestErpSalReturnRefund`+`TestErpFinBadDebt`+`TestErpFinBadDebtReversal`+`TestErpFinNotesReceivableStateMachine`+`TestErpFinNotesPayableStateMachine`；浏览器层 `fin-ar-ap-auto-reconciliation`+`fin-bank-reconciliation`+`fin-bad-debt-reverse-*`+`p2p-chain`/`o2c-chain`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-018 enum↔dict 漂移 + P2-MA1-019 fromCode 已登记待 MR1）；A2.1-A2.2 done（P1-MA2-003/009/002 + P2-MA2-008/013/014 已登记供本审计从状态机角度复核）；A2.3 done（preCheck 未核销项扫描 + 坏账门控交互）；A2.5a done（辅助账生成 ErpFinArApItemGenerator 与凭证金额一致性已确认）

- [ ] 维度「状态定义」：审查辅助账项 5 态（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）语义清晰性——每个状态名是否清楚表达业务等待点；CANCELLED 是否为真实业务等待点（ReconciliationSettler.resolveStatus 永不设置 CANCELLED——入口在哪？票据 writeOff？过账红冲？）；WRITTEN_OFF 是终态还是可恢复（recover/reverseApprove 可 WRITTEN_OFF→OPEN——非终态）；核销单 3 态（DRAFT/POSTED/REVERSED）是否完整（有无 CANCELLED 草稿废弃？）；坏账单 approveStatus×docType 组合的语义清晰性（WRITE_OFF+APPROVED / RECOVERY+APPROVED / REJECTED 后状态）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「转换完整性」：列出辅助账项每个状态的所有传入/传出转换——OPEN→PARTIAL（部分核销 applySettlement）/ OPEN/PARTIAL→SETTLED（全额核销 resolveStatus settled>=total）/ SETTLED/PARTIAL→OPEN/PARTIAL（reverseSettle 状态降级）/ OPEN→WRITTEN_OFF（executeWriteOff）/ WRITTEN_OFF→OPEN（executeRecovery/reverseApprove 反向）/ ???→CANCELLED（**入口不明——重点核验**）；核销单 DRAFT→POSTED（settle）/ POSTED→REVERSED（reverseSettle）/ DRAFT→?（草稿废弃路径——useLogicalDelete？）；坏账单审批状态机 submit（UNSUBMITTED→SUBMITTED）/ approve（SUBMITTED→APPROVED + 触发 executeWriteOff/Recovery）/ reject（→REJECTED）/ reverseApprove（APPROVED→REJECTED + 红冲闭环）；票据核销联动 AR/AP 转换；是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「终端状态和恢复」：列出所有终端状态——SETTLED（可经 reverseSettle 恢复→非真终态）/ WRITTEN_OFF（可经 recover/reverseApprove 恢复→非真终态）/ CANCELLED（终态？入口不明则不可达终态）；REVERSED 核销单是否可再核销（经新核销单 DRAFT→POSTED）；坏账单 APPROVED（可经 reverseApprove→REJECTED 恢复→非真终态）；归档与活动辅助账项是否可区分（settled/open 金额 + status）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「异常路径」：核验全覆盖——超额核销（`allow-over-reconcile=false` 拒绝 / OVER 状态 owner doc 声明但 dict 无 OVER 项——漂移）/ 跨往来单位核销（拒绝）/ 已结账期间核销（拒绝——**守卫在哪？P1-MA2-021 期间侧，核销单 BizModel 是否校验期间状态**）/ 并发核销（P2-MA2-008/014 无锁——辅助账 applySettlement 无 @Version 状态竞态）/ 坏账核销金额超 openAmount（`ERR_BAD_DEBT_WRITE_OFF_AMOUNT_EXCEEDS_OPEN` 守卫）/ recover 非 WRITTEN_OFF 项（`requireWrittenOffArApItem` 守卫）/ reverseApprove 未过账坏账单（`ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED` 守卫）；幂等性（重复核销同核销单 / 重复 writeOff 同 ArApItem）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「可达性」：从生成（OPEN）每个状态是否可达——**重点 CANCELLED**（ReconciliationSettler 不设置——若仅票据 writeOff 设置则非票据路径辅助账项永不可达 CANCELLED，dict 项死状态）；辅助账项从 OPEN 到 SETTLED/WRITTEN_OFF 的可达性；坏账单 REJECTED 后是否可重新 submit（validateTransitionForSubmit 是否允许 REJECTED→SUBMITTED）；是否有死循环或不可达终态路径。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「角色和权限」：每个转换绑定执行角色——手工核销（财务员）/ 自动核销配置（财务管理员）/ 核销冲销（财务员+原因 ar-ap-reconciliation.md §核销权限）/ 坏账核销 writeOff/recover（审批门控 `write-off-require-approval` config）；危险操作（坏账核销影响报表 / 核销冲销恢复余额 / reverseApprove 反审核）；多角色冲突（核销员 vs 坏账审批员 vs 会计）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「外部依赖」：**重点——域侧核销器（ReceiptSettler/PaymentSettler）与 finance 核销单（ErpFinReconciliation）的双路径一致性**：是否两条路径都经 ReconciliationSettler 回写 ArApItem status，还是各自直接回写（状态机分歧风险——若域侧直接回写不经 resolveStatus 则状态裁决逻辑分叉）；辅助账项生成依赖过账引擎 Provider（`ErpFinArApItemGenerator`——A2.5a 已确认金额一致性）；票据核销联动 AR/AP（同方向核销，票据 writeOff→辅助账 CANCELLED）；期末结账 preCheck 扫描未核销项（`findUnsettledArApCodes` status≠SETTLED/CANCELLED/WRITTEN_OFF——已排除 WRITTEN_OFF）；外部步骤失败是否阻断状态迁移。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「TODO/任务策略」：每个非终端状态是否产生正确类型待办——未核销 AR/AP 账龄逾期是否产生催收/付款待办（账龄分级风险等级 ar-ap-reconciliation.md §账龄分级，但无显式待办生成——长期 OPEN 静默下沉）；坏账核销 APPROVED 待审批是否产生待办；核销冲销是否产生待办；是否存在期望有人行动但不产生待办的状态（长期 OPEN 辅助账项——期末仅提示非阻断 P1-MA2-017）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 应收核销快乐路径（AR 发票过账→生成 ArApItem OPEN→收款→核销→applySettlement→SETTLED）；(b) 部分核销（OPEN→PARTIAL→继续核销→SETTLED）；(c) 核销冲销（POSTED→reverseSettle→REVERSED + ArApItem 状态降级 SETTLED→PARTIAL/OPEN）；(d) 坏账核销（OPEN→writeOff→executeWriteOff→WRITTEN_OFF + BAD_DEBT_WRITE_OFF 凭证）；(e) 坏账收回（WRITTEN_OFF→recover→executeRecovery→OPEN + RECOVERY 凭证）；(f) 坏账反审核（APPROVED→reverseApprove→红冲凭证 + ArApItem 对称回退 + REJECTED——强一致回滚是否真覆盖半状态）；(g) 多币种核销汇兑损益（当前未实现 P1-MA2-009——核销时本位币折算缺失，applySettlement 用 settledAmountFunctional 回写错误）；(h) 并发核销（P2-MA2-008/014——双读双写过付 + 辅助账 SETTLED 判定漂移）；(i) 票据核销联动（票据 writeOff→辅助账 CANCELLED）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「与设计文档一致性」：每个状态/转换在 `ar-ap-reconciliation.md`/`bad-debt.md`/`state-machine.md` 是否有匹配——**重点漂移**：(1) `ar-ap-reconciliation.md §核销状态` 发票核销状态 4 态（UNRECONCILED/PARTIAL/RECONCILED/OVER）vs dict 5 态（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）命名+数量均不一致（OVER 状态 owner doc 声明但 dict 无 OVER 项）；(2) `§核销状态` 收付款核销状态 3 态（UNRECONCILED/PARTIAL/RECONCILED）命名与 dict 不一致；(3) `§核销冲销` 描述 `reversalFlag=true` 布尔 vs 实现 `docStatus=REVERSED`（owner doc 已注记实现 schema）；(4) `state-machine.md` 无 AR/AP 核销独立状态机章节（散落在 ar-ap-reconciliation.md §核销状态 + bad-debt.md）；(5) `bad-debt.md` 坏账状态机与 `ErpFinBadDebtProcessor` reverseApprove 红冲闭环一致性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「多币种核销辅助账本位币回写（项目特定，P1-MA2-009 复核）」：核验 `ReconciliationSettler.applySettlement` 用 `settledAmountFunctional` 回写辅助账 openAmountFunctional——若核销时无 FX 折算（SalAcctDocProvider.RECEIPT 无 6051 汇兑损益 plug，P1-MA2-009），则辅助账本位币余额错误（外币发票 × 发票汇率 vs 外币收款 × 收款汇率差未 plug）；复核 `ErpFinReconciliationLine.settledAmountSource/Functional` 双字段是否真承载源币/本位币分离（vs VoucherFact 单 amount 字段 P1-MA2-002）；核销时汇兑损益凭证生成路径（ar-ap-reconciliation.md §汇兑损益核销规则 vs 实现）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「并发核销辅助账状态竞态（项目特定，P2-MA2-008/014 复核）」：核验 `ReconciliationSettler.applySettlement` 无 `@Version`/无悲观锁——并发核销同一 ArApItem（如并发两笔收款核销同一发票），两事务均读到 openAmount=X，各自 applySettlement 写 settled+=amt/open-=amt，后提交覆盖先提交——辅助账余额漂移 + SETTLED 判定错误（应 SETTLED 但两事务均判 PARTIAL）；`ErpFinArApItem` 是否有 `version` 字段（@Version 乐观锁）；域侧 ReceiptSettler/PaymentSettler 同型并发缺口（P2-MA2-008/014 已登记交接 A2.17，本审计复核辅助账侧状态竞态严重性）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 复核已登记 MA2 finding AR/AP 核销状态机角度：P1-MA2-003（付款核销三单匹配前置与 ArApItem 状态迁移关系）/ P1-MA2-009（**多币种核销辅助账本位币回写升级评估**：是否破坏辅助账余额正确性——影响总账与辅助账对账）/ P2-MA2-008+P2-MA2-014（**并发核销辅助账 SETTLED 判定漂移升级评估**：是否破坏状态机裁决）/ P2-MA2-013（订单维度核销/预收预付辅助账路径）/ P1-MA2-002（辅助账 amountFunctional 与凭证一致性）。标注每项终态（仅治理缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`（含：辅助账项状态机状态图 + 核销单状态机状态图 + 坏账核销状态机状态图、各维度通过/失败裁决、9 控制点 PASS/FAIL、MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

- [ ] 辅助账项状态机（5 态）+ 核销单状态机（3 态）+ 坏账核销状态机（approveStatus×docType）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [ ] 9 个已识别控制点（状态定义 / 转换完整性 / 终端与恢复 / 异常路径 / 可达性 / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [ ] state-machine-business-review 10 维度 + 2 项目特定维度（多币种核销辅助账回写 / 并发核销状态竞态）至少一句裁决（含「本维度无发现」）
- [ ] MA2 finding 运行时影响复核结论已记录（含 P1-MA2-009 多币种核销辅助账本位币升级评估 + P2-MA2-008/014 并发核销 SETTLED 漂移升级评估裁决）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: AR/AP 核销状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.x finance/AR-AP 核销状态机行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（CANCELLED 辅助账项不可达致 dict 死状态 [若破坏状态机] / 域侧与 finance 双路径核销状态机分歧致辅助账余额不一致 / 坏账 reverseApprove 红冲失败致 ArApItem 与凭证悬挂半状态 [若强一致回滚有缺口] / 多币种核销辅助账本位币余额错误 [若 P1-MA2-009 升级] / 并发核销辅助账 SETTLED 判定漂移 [若 P2-MA2-008/014 升级]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。注意：本审计对已登记 finding（P1-MA2-003/009/002 + P2-MA2-008/013/014）只复核状态机运行时影响不重复登记根因；若发现新 P1（如 CANCELLED 入口缺失 / 核销状态命名漂移 OVER 项 / 域侧 finance 双路径状态机分歧 / 核销单无 CANCELLED 草稿废弃）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.x finance/AR-AP 核销状态机 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05badf796ffeLEPzETOQW1bR6R`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`ReconciliationSettler.resolveStatus:83-93` 仅返回 OPEN/PARTIAL/SETTLED 永不设置 CANCELLED/WRITTEN_OFF ✓ / `applySettlement:62-81` 无 @Version/无锁 ✓ / `reverseSettle:52-60` 存在 reverse=true ✓ / `ErpFinBadDebtProcessor.writeOff:61-71`+`recover:73-82`+`reverseApprove:124-164` 对称回退（writeOff 反向 WRITTEN_OFF→OPEN:148 / recovery 反向 OPEN→WRITTEN_OFF:155 / APPROVED→REJECTED:161 / finPostingExecutor.reverse:136）✓；dict erp-fin/ar-ap-status 5 态 + reconciliation-status 3 态 + bad-debt-type WRITE_OFF/RECOVERY ✓；ErpFinArApItem.status:752 + ErpFinReconciliation.docStatus:817 ✓；域侧 ReceiptSettler.settle:55 + PaymentSettler.settle:55 双路径存在 ✓；`ar-ap-reconciliation.md §核销状态:135-152` 发票 4 态（UNRECONCILLED/PARTIAL/RECONCILLED/OVER）vs dict 5 态命名+数量均不一致 ✓ + 实现注记 reversalFlag vs docStatus=REVERSED ✓；`state-machine.md` 无 AR/AP 核销独立章节（仅对象一凭证+对象二期间）✓；arm-index findings P1-MA2-002/003/009 + P2-MA2-008/013/014 全部存在 ✓。检查清单全部 PASS（基线准确性/格式/结果表面——3 组件 ArApItem/Reconciliation/BadDebt 共享 ar-ap-status 轴+共享验证路径，rule-14 合理/Item 类型/技能/反松弛/不可降级/范围清晰/结束门控/退出标准）。**采纳的非阻塞精化**：reverseApprove 行号 `124-154+` 精化为 `124-164`（已应用至 Current Baseline + Targets）。Goals「9 控制点」与 Exit Criteria「10+2 维度」二元性镜像 A2.5a 已接受范式（9=重点子集，12 维度各自独立裁决项），无歧义。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。AR/AP 核销触及会计保护区域，P0 即时修复须额外人工确认。

- [ ] 范围内行为完成（A2.5c AR/AP 核销状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、ar-ap-reconciliation/bad-debt/bank-reconciliation/state-machine owner doc 结论已反映）
- [ ] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.5a 会计凭证状态机 + A2.5b 期间/预算状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.5a done；A2.5b 覆盖期间/预算。本审计只确认过账引擎产出 AR/AP 凭证与辅助账金额一致性（A2.5a 已确认）+ 期间 CLOSED_FINAL 核销守卫（期间侧）。
- Successor Required: `no`（A2.5a 已 done）/ `yes`（A2.5b 执行时复核期间侧）。

### A2.1/A2.2 P2P/O2C 端到端编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: done；本审计只复核核销环节辅助账项状态机迁移正确性，不做端到端编排审查。
- Successor Required: `no`——A2.1/A2.2 已 done。

### A4.1b finance 代码质量审计 — 预算/AR-AP/成本/期间

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做 AR/AP 核销状态机**业务正确性**审查；核销/坏账 Processor 代码质量（异常处理类型/N+1/索引/辅助方法）系统性审查归 A4.1b。
- Successor Required: `yes`——A4.1b 执行时复核。

### A2.17 并发与乐观锁（并发核销/坏账 reverseApprove/辅助账状态竞态）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（P2-MA2-008/014 辅助账并发回写、坏账 reverseApprove 并发、resolveStatus 竞态），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### 银行对账独立状态机 + 自动核销定时调度 + 订单维度核销预收款 + 多币种核销汇兑损益实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定——银行对账（bank-match-status）是独立子系统状态机归 bank-reconciliation.md；自动核销定时调度（`ar-ap-auto-recon-cron` deferred follow-up）；订单维度核销预收款（P2-MA2-013 successor）；多币种核销汇兑损益实现（P1-MA2-009 MR1 裁决）。
- Successor Required: `yes`——各 successor 触发条件满足时（如银行对账子系统深化/自动核销调度上线/预收款业务需求/多币种核销 MR1 裁决落地）。

## Closure

Status Note: <起草中——待独立草案审查通过后填写>

Closure Audit Evidence:

- <待 closure audit 时填写>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
