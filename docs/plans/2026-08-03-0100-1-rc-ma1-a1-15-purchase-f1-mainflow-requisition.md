# 2026-08-03-0100-1 rc-ma1-a1-15-purchase-f1-mainflow-requisition purchase-F1 主流程与请购需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.15（MA1 需求追踪矩阵审计 — purchase-F1 主流程与请购：UC-PUR-01 标准采购全流程主路径 + UC-PUR-08 请购转订单）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.15
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.15 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，业财过账范式 + GR/IR + AP 凭证为 UC-PUR-01/07 过账链同根因参考）、`2026-08-02-2250-1-rc-ma1-a1-12-hr-f1-employee-organization.md`（A1.12 done，同 MA1 审计范式参考）、`2026-08-02-2250-3-rc-ma1-a1-14-hr-f3-payroll-survey.md`（A1.14 done，最新同范式参考）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.15 给出 UC 清单 = `UC-PUR-01/08`（2 UC），锚点 `use-cases.md:19 / :204`（baseline inventory :85/:92 + 切片索引确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/purchase/use-cases.md`：
  - UC-PUR-01 标准采购全流程主路径（`:19`）：请购单审核 → 生成采购订单审核 → 收货创建入库单(关联订单)审核 → 收票创建发票(关联入库)审核 → 创建付款单审核核销发票。验收标准：①订单.来源单号==请购单.单号（回链）；②入库单行.订单行号回链订单行；③入库审核时库存余额[物料,仓库].可用量 += 入库明细数量之和；④入库审核生成凭证 businessType=GOODS_RECEIPT 来源=入库单；⑤发票审核生成凭证 businessType=PURCHASE_INVOICE 来源=发票；⑥入库单.已过账==true 且 发票.已过账==true；⑦付款核销：发票.付款状态 未付→部分/已付清；⑧往来单位.应付余额==发票金额-已核销金额。跨域：inventory（入库 incoming）/ finance（GOODS_RECEIPT + PURCHASE_INVOICE 过账）。
  - UC-PUR-08 请购转订单（`:204`）：请购单审批后转化为采购订单。验收标准：①前置：请购单.审核状态==已审核（必要条件）；②由请购单生成订单；③订单行(数量/物料)继承自请购单行，可编辑；④一个请购可拆多个订单(不同供应商/到货期)，生成订单数>=1；⑤幂等：请购单.已转订单==true 标记后不可重复转化；⑥再次转化→报错或返回已转化。

- **L2 owner doc 设计参考**：`docs/design/purchase/state-machine.md`（三轴 docStatus/approveStatus/paidStatus + 9 实体状态机）+ `docs/design/purchase/three-way-match.md`（§回链关系，UC-PUR-01 验收标准②回链契约）+ `docs/design/purchase/README.md` + `docs/design/flow-overview.md §2.1`（订单审核锁定价格 P2-MA2-007 watch-only）。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验，路径已确认存在）**：
  - **请购（UC-PUR-08 转订单 + UC-PUR-01 step1）**：`ErpPurRequisitionBizModel.java`（convertToOrder + 已转订单 幂等标记）+ `processor/ErpPurRequisitionApproveProcessor.java` + `ErpPurRequisitionProcessor.java` + `ErpPurRequisitionSubmitForApprovalProcessor/Reject/ReverseApprove/WithdrawApproval/CancelProcessor`（R6.5 per-mutation 拆分落地）+ `ErpPurRequisitionLineBizModel.java`。
  - **订单（UC-PUR-01 step2）**：`ErpPurOrderBizModel.java` + `processor/ErpPurOrderApproveProcessor.java`（R6.5；订单审核触发承付 release/commit + 价格锁 P2-MA2-007）+ `ErpPurOrderProcessor.java` + per-mutation Processor 族 + `ErpPurOrderLineBizModel.java`。
  - **入库（UC-PUR-01 step3）**：`ErpPurReceiveBizModel.java`（关联订单）+ `processor/ErpPurReceiveApproveProcessor.java`（R6.5；入库审核 → IErpInvStockMoveBiz.generateMove Facade 库存 incoming + GOODS_RECEIPT 过账触发）+ `ErpPurReceiveProcessor.java` + per-mutation 族 + `ErpPurReceiveLineBizModel.java`。**执行时核验**：GOODS_RECEIPT 过账触发路径（PurInvoicePostingDispatcher 只覆盖 PURCHASE_INVOICE；GOODS_RECEIPT 经 ReceiveApproveProcessor 或 IErpFinAcctDocProvider 注册——执行时确认）。
  - **发票（UC-PUR-01 step4）**：`ErpPurInvoiceBizModel.java` + `processor/ErpPurInvoiceApproveProcessor.java`（R6.5；发票审核 → PURCHASE_INVOICE 过账 + 承付 release [P1-MA2-083 resolved R1.27]）+ `ErpPurInvoiceProcessor.java`（D-mutation facade）+ per-mutation 族 + `ErpPurInvoiceLineBizModel.java`。
  - **付款核销（UC-PUR-01 step5）**：`ErpPurPaymentBizModel.java` + `processor/ErpPurPaymentSettleProcessor.java`（R6.5；付款核销发票）+ `entity/PaymentSettler.java`（读 invoiceBalance→写 PaymentLine + recomputeInvoicePaid 事后聚合；**P1-MA2-003 resolved plan 2026-07-29-2322-1 方案 A：注入 ThreeWayMatcher + recheckThreeWayMatchAtSettle 强制 strict 复核 + config-gated `erp-pur.settle-recheck-three-way-match` 默认 false**；并发核销无锁 P2-MA2-008 watch-only 归 A2.17）+ `ErpPurPaymentProcessor.java` + per-mutation 族 + `ErpPurPaymentLineBizModel.java`。
  - **过账 Dispatchers**：`posting/PurInvoicePostingDispatcher.java`（PURCHASE_INVOICE）+ `posting/PurPaymentPostingDispatcher.java`（PAYMENT）+ `posting/PurReturnPostingDispatcher.java`（PURCHASE_RETURN，归 A1.17）+ `posting/PurReversalListener.java`（finance→purchase 反向回滚 Invoice/Receipt/Return，**P1-MA2-051 rollbackReceive 不对称 receive APPROVED+posted=false 悬挂**，归 A1.17 receive 侧）。**UC-PUR-01 主路径核验 invoice/payment 侧 dispatch**；GOODS_RECEIPT 路径执行时确认（无独立 PurReceivePostingDispatcher，可能经 Provider 注册或 ReceiveApproveProcessor 内嵌）。

- **L4 测试证据现状**：`TestErpPurProcureToPayEnd`（UC-PUR-01 P2P 全链）+ `TestErpPurRequisitionConvertToOrder`（UC-PUR-08 转订单 + 幂等）+ `TestErpPurRequisitionApproval` + `TestErpPurRequisitionToOrderEnd` + `TestErpPurOrderApproval` + `TestErpPurOrderToReceiveEnd` + `TestErpPurReceiveApproval` + `TestErpPurReceiveStockMove`（库存 incoming）+ `TestErpPurInvoiceApproval` + `TestErpPurInvoicePosting`（PURCHASE_INVOICE 凭证）+ `TestErpPurPaymentApproval` + `TestErpPurPaymentSettlement`（核销 + 派生 paidStatus）+ `TestErpPurPaymentWorkflowApproval` + `TestErpPurOrderCommitment`（承付）+ `TestErpPurBudgetControlIntegration`（预算硬拦截 UC-FIN-11 跨域）。E2E：`tests/e2e/orchestration/p2p-chain.spec.ts`（UC-PUR-01 全链）+ `p2p-reverse-approve.spec.ts` + `p2p-reverse.spec.ts` + `crud/purchase.smoke.spec.ts`。**执行时核验断言强度**：UC-PUR-01 验收标准 ①-⑧ + UC-PUR-08 ①-⑥ 各验收标准是否有强断言（而非仅冒烟状态流转）。

- **L5 既有证据（MA2/A4 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8）= purchase 9 实体状态机审查**：Verdict 主路径状态迁移守卫齐全（PROC 路径 `validateNotCancelled`/`validateTransition*`/`validateBusinessRules*` 三段守卫 + `doApprove`/`doReject`/`doReverseApprove`/`doCancel` 四动作齐全）+ @BizMutation 事务回滚保证 approve 触发的跨域写（承付 commit/release + 库存 incoming + 过账 AP_INVOICE/PAYMENT/PURCHASE_INPUT + AVL SUSPENDED）失败原子性 + reverseApprove 红冲闭环强一致 + 跨域写经 I*Biz Facade（production 代码无 daoFor 跨域写直写）。**零 P0**；本切片相关 **P1**：P1-MA2-050（INLINE reject/withdrawApproval 绕过 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移，**resolved R1.17**）+ P1-MA2-051（PurReversalListener.rollbackReceive 不对称——归 A1.17 receive 侧，本切片 invoice/payment 侧引用）；**P2**：P2-MA2-053（三种并行模式 owner doc 未声明）+ P2-MA2-054（死代码 WithdrawApproval/Reject Processor 未接线，**closed MR5 R5.8**，与 P1-MA2-050 联动）+ P2-MA2-055（payment writtenOffStatus 复用 paid-status 字典语义漂移）。
  - **`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（audit-remediation A2.1 P2P 端到端审计）**：P2P 链路行为已证实（回链 / 库存 incoming / 过账 / 核销 主路径完整），本切片只补"需求契约↔实际行为"差异（验收标准逐条视角）。本切片相关 **P1（已 resolved）**：P1-MA2-003（付款核销缺发票三单匹配完成态复核，**resolved plan 2026-07-29-2322-1 方案 A**，HEAD 复核 UC-PUR-01 step5 settle 前置）；**P2**：P2-MA2-007（订单审核价格锁缺失 watch-only）+ P2-MA2-008（PaymentSettler 并发核销无锁归 A2.17）。
  - **`docs/audits/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done）= 业财过账引擎审计**：GOODS_RECEIPT/PURCHASE_INVOICE/AP 凭证链路范式已审（Provider 路由 + VoucherBillR 业财回链 + GR/IR 暂估应付），本切片引用其过账正确性结论，只补采购侧触发契约。
  - **注意**：A2.8/P2P e2e 覆盖**状态机迁移守卫/事务边界/跨域 Facade/P2P 链路行为**。本切片从**需求契约↔实现符合性**视角补差异（UC-PUR-01 八条验收标准逐条 + UC-PUR-08 六条验收标准逐条 + **resolved finding HEAD 复核**：P1-MA2-083 resolved R1.27 承付恢复 + P1-MA2-050 resolved R1.17 INLINE 补 isCancelled 守卫[方案B] + P2-MA2-054 closed MR5 R5.8 Processor 接线 + P1-MA2-003 resolved plan 2026-07-29-2322-1 方案 A settle 三单匹配复核 + P2-MA2-006 returns.md resolved）。

- **arm-index 既有 finding 衔接**：主流程与请购相关——`P1-MA2-050`（INLINE reject/withdrawApproval 绕过 isCancelled/requireSupplierActive/requireLinesNonEmpty 守卫致 CANCELLED 单据 approveStatus 副轴漂移，**resolved R1.17 方案B**——INLINE 路径补 isCancelled 等守卫，HEAD 复核守卫落地）/ `P2-MA2-054`（死代码 WithdrawApproval/Reject Processor 未接线，**closed MR5 R5.8**——purchase WithdrawApproval/Reject per-mutation Processor 经 R5.1 xbiz source 接线 + R5.8 BizModel cancel repoint，与 P1-MA2-050 联动 HEAD 复核 Processor 接线激活）/ `P1-MA2-083`（AP/AR 发票冲销不恢复承付，**resolved R1.27**，HEAD 复核 ErpPurInvoiceProcessor.reverseApprove/cancel 调 commit() 恢复承付）/ `P1-MA2-003`（付款核销缺发票三单匹配完成态复核，**resolved plan 2026-07-29-2322-1 方案 A**——PaymentSettler 注入 ThreeWayMatcher + recheckThreeWayMatchAtSettle 强制 strict 复核 + config-gated `erp-pur.settle-recheck-three-way-match` 默认 false，HEAD 复核 UC-PUR-01 step5 付款核销前置）/ `P2-MA2-007`（订单审核价格锁缺失 watch-only）/ `P2-MA2-008`（PaymentSettler 并发核销无锁归 A2.17）/ `P2-MA2-053`（三种并行模式 owner doc 未声明 watch-only）/ `P2-MA2-055`（payment writtenOffStatus 字典语义漂移 watch-only）/ `P0-MA1-021`（CostAdjustmentPostingDispatcher Facade，**resolved sustained done**，purchase 跨域 Facade 范式参考）/ `P1-MA1-022`（跨域只读 daoFor 维持治理缺陷）。UC-PUR-01 库存 incoming 回链完整性 / UC-PUR-01 GOODS_RECEIPT 过账触发路径 / UC-PUR-08 转订单幂等"已转订单"标记为候选新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` purchase 主流程/请购/转订单/承付/核销同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；**触及会计过账逻辑（PurInvoicePostingDispatcher/PurPaymentPostingDispatcher/PostingProvider/VoucherFact/PostingProcessor 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类）。

- **剩余差距**：A1.15 切片的五级追踪审计报告缺失 = MA4（A4.1 业财域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.15 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.15 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`，含方法论 §6 **9 段全部内容**：①UC-PUR-01/08 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含请购 + 订单 + 入库 + 发票 + 付款 + 过账 Dispatcher + PaymentSettler）③测试证据（注明断言强度）④运行时行为证据（复用 A2.8/P2P e2e/A1.1，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-PUR-01（①订单来源回链 + ②入库行回链订单行 + ③入库审核库存可用量+= + ④GOODS_RECEIPT 凭证 + ⑤PURCHASE_INVOICE 凭证 + ⑥入库/发票已过账 + ⑦付款核销派生 paidStatus + ⑧应付余额==发票-已核销）+ UC-PUR-08（①前置请购已审核 + ②生成订单 + ③订单行继承可编辑 + ④一请购拆多订单 + ⑤已转订单幂等标记 + ⑥重复转化报错），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-PUR-01 GOODS_RECEIPT 过账触发路径**（执行时 HEAD 核验：经 ReceiveApproveProcessor 内嵌 or Provider 注册 or 缺失→若缺失按 §2 P0/P1 定级）+ UC-PUR-01 库存 incoming 回链（执行时核验）+ UC-PUR-08 转订单幂等"已转订单"标记（执行时 HEAD 核验）+ **resolved finding HEAD 复核**：P1-MA2-083（resolved R1.27 承付恢复，HEAD 复核 ErpPurInvoiceProcessor.reverseApprove/cancel 调 commit()）+ P1-MA2-050（resolved R1.17 方案B INLINE 守卫，HEAD 复核）+ P2-MA2-054（closed MR5 R5.8 Processor 接线，HEAD 复核）+ P1-MA2-003（resolved plan 2026-07-29-2322-1 方案 A settle 三单匹配复核，HEAD 复核）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / purchase use-cases / state-machine.md / three-way-match.md / flow-overview.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.15 只覆盖 UC-PUR-01/08；UC-PUR-02/03/05/06 归 A1.16，UC-PUR-04/07 归 A1.17）。**UC-PUR-01 step3/4 涉及的 GR/IR + AP 过账正确性归 A1.1 业财过账引擎已审**，本切片核验采购侧触发契约 + GOODS_RECEIPT 触发路径，不重审过账引擎本身。**P1-MA2-051 PurReversalListener.rollbackReceive receive 侧悬挂归 A1.17**，本切片核验 invoice/payment 侧反向回滚。
- **不重跑既有状态机/P2P 链路行为审计**（§去重协议：A2.8/P2P e2e/A1.1 已证实状态机迁移守卫/事务边界/跨域 Facade/GR-IR-AP 凭证链路，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.15 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.15 UC 锚点）+ `docs/design/purchase/use-cases.md`（L1 真相源）+ `docs/design/purchase/state-machine.md` + `three-way-match.md` + `README.md` + `docs/design/flow-overview.md §2.1`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.8/P2P e2e/A1.1 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.8/P2P e2e/A1.1 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurProcureToPayEnd,TestErpPurRequisitionConvertToOrder,TestErpPurInvoicePosting,TestErpPurPaymentSettlement,TestErpPurOrderCommitment`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（落盘 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-PUR-01/08 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:19/:204` 验收标准原文（禁止转述）；L2 引用 `state-machine.md`（三轴 + 9 实体状态机）+ `three-way-match.md`（§回链关系）+ `flow-overview.md §2.1`（订单审核锁定价格，标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpPurRequisitionBizModel.java:line`（convertToOrder + 已转订单幂等）+ `ErpPurRequisitionApproveProcessor:line` + `ErpPurOrderBizModel.java:line` + `ErpPurOrderApproveProcessor:line`（承付 + 价格锁）+ `ErpPurReceiveBizModel.java:line`（关联订单）+ `ErpPurReceiveApproveProcessor:line`（库存 incoming Facade + GOODS_RECEIPT 触发）+ `ErpPurInvoiceBizModel.java:line` + `ErpPurInvoiceApproveProcessor:line`（PURCHASE_INVOICE + 承付 release）+ `ErpPurPaymentBizModel.java:line` + `ErpPurPaymentSettleProcessor:line` + `PaymentSettler.java:line`（核销 + recomputeInvoicePaid）+ `posting/PurInvoicePostingDispatcher:line` + `posting/PurPaymentPostingDispatcher:line`；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.8/P2P e2e/A1.1 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-PUR-01——①订单.来源单号==请购单.单号（回链写入）；②入库单行.订单行号回链订单行；③入库审核库存可用量+=（IErpInvStockMoveBiz.generateMove Facade）；④GOODS_RECEIPT 凭证生成（**关键：执行时核验触发路径——经 ReceiveApproveProcessor 内嵌 or Provider 注册 or 缺失**）；⑤PURCHASE_INVOICE 凭证生成（PurInvoicePostingDispatcher）；⑥入库/发票已过账标志；⑦付款核销派生 paidStatus（PaymentSettler.recomputeInvoicePaid）；⑧往来单位应付余额==发票-已核销。UC-PUR-08——①前置请购已审核校验；②生成订单；③订单行继承可编辑；④一请购拆多订单（不同供应商/到货期）；⑤已转订单幂等标记；⑥重复转化报错。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**：**P1-MA2-083 承付恢复（resolved R1.27）**——HEAD 复核 `ErpPurInvoiceProcessor.reverseApprove/cancel` 是否调 `commit()` 恢复承付（按发票关联 PO 反查）；**P1-MA2-050 INLINE 守卫（resolved R1.17 方案B）**——HEAD 复核 reject/withdrawApproval INLINE xbiz 路径是否已补 `isCancelled`/`requireSupplierActive`/`requireLinesNonEmpty` 守卫（方案B = INLINE + 守卫，非方案A Processor 迁移）；**P2-MA2-054 死代码 Processor 接线（closed MR5 R5.8，与 P1-MA2-050 联动）**——HEAD 复核 WithdrawApproval/Reject per-mutation Processor 是否经 xbiz source 接线激活（非死代码）；**P1-MA2-003 付款核销三单匹配复核（resolved plan 2026-07-29-2322-1 方案 A）**——HEAD 复核 `PaymentSettler.recheckThreeWayMatchAtSettle` + `erp-pur.settle-recheck-three-way-match` config-gated 接线（UC-PUR-01 step5 前置）；P2-MA2-006 returns.md red invoice drift（resolved plan 2026-07-29-2322-1）。逐条记录复核结论（已落地/回退/部分落地/仍 open successor）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-PUR-01 GOODS_RECEIPT 过账触发路径（HEAD 复核：已接线→接受 on ④；缺失→按 §2 P0③核心业务循环断裂 or P1①功能缺失定级）；UC-PUR-01 库存 incoming 回链/核销派生 paidStatus/应付余额（执行时 HEAD 核验）；UC-PUR-08 转订单幂等"已转订单"标记（HEAD 复核：缺失→P1①功能缺失）；resolved finding HEAD 复核（P1-MA2-083/P1-MA2-050/P2-MA2-054/P1-MA2-003）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-PUR-01/08 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.8/P2P e2e/A1.1 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-⑧ + ①-⑥ 有明确分级（非悬空"待查"）；**GOODS_RECEIPT 过账触发路径 HEAD 复核结论已记录**；P1-MA2-083/P1-MA2-050/P2-MA2-054/P1-MA2-003 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（落盘 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` purchase 主流程/请购/转订单/承付/核销同域同控制点（如 P1-MA2-050、P2-MA2-054、P1-MA2-083、P1-MA2-003、P2-MA2-007/008/053/055、P0-MA1-021、P1-MA1-022）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-PUR-08 转订单幂等标记 / UC-PUR-01 GOODS_RECEIPT 触发路径若为缺失）→ 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-PUR-01 业财过账与 A1.1/P1-MA2-083 同根因则交叉引用而非重复新建。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如 GOODS_RECEIPT 过账运行时 approve→APPROVED 触发链 / 库存 incoming 运行时回链写入 / 付款核销运行时派生 paidStatus / 转订单幂等运行时重复转化拦截；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 A2.8（9 实体状态机 + PROC 路径守卫 + 跨域 Facade + P1-MA2-050[resolved R1.17 方案B INLINE 守卫] + P2-MA2-053/054[closed MR5 R5.8]/055 finding）+ A2.1 P2P e2e（P2P 链路行为 + P1-MA2-003[resolved plan 2026-07-29-2322-1 方案 A settle 三单匹配复核] + P2-MA2-007/008 finding）+ A1.1（业财过账引擎 GR/IR + AP 凭证范式）已证实结论，列明本切片只补的需求视角差异（UC-PUR-01 八条验收标准逐条 + UC-PUR-08 六条验收标准逐条 + resolved finding HEAD 复核 R1.17[R1.17]/R1.27/MR5 R5.8/plan 2026-07-29-2322-1 落地确认）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_03cbb5c4dffeO2HqIYiESSt6vO`，fresh session，未起草本计划）。逐项实测核验：roadmap 对齐（A1.15 / UC-PUR-01/08 / Deps=0.2 done / Skill）、UC 锚点 :19/:204 全匹配、L3 全部 13 代码路径 + L4 5 测试 + p2p-chain E2E 全存在、A2.8 报告 + P1-MA2-083(resolved R1.27)/P2-MA2-006(resolved)/P2-MA2-007/008/053/055 watch-only/P0-MA1-021 sustained done 全 confirmed；跨切片边界正确（UC-PUR-02/03/05/06→A1.16、UC-PUR-04/07→A1.17、P1-MA2-051 receive 侧→A1.17、过账引擎→A1.1）；只读审计 + 会计保护区域 ask-first 合规；Q4 强调正确。**1 项 BLOCKER**：P1-MA2-050 resolved 状态误标 "resolved MR5 R5.8"（arm-index:254 实为 "resolved R1.17 done"；MR5 R5.8 实际关闭的是 P2-MA2-054 死代码 Processor 接线 + P1-MA3-048 孤儿 Processor）——P1-MA2-050（INLINE 守卫 bypass）与 P2-MA2-054（死代码 Processor 未接线）是两个不同 finding 被混淆。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_03cb6cb62ffe83wa3KE10H9jMN`，fresh session）。修复部分传播（4/6 处更正）但**残留 2 处**（L37 注意段 + L49 Goals 段）仍把 P1-MA2-050 标为 "resolved MR5 R5.8 Processor 接线"；并指出 R1.17 实施的是**方案B**（INLINE + isCancelled 守卫）非方案A（Processor 迁移），HEAD 复核描述应匹配方案B；另指出 finding 在 arm-index 注册为 P2-MA2-054（非 P1-MA2-054），L635 的 P1- 标签系 arm-index 内部 typo。
- Independent draft review iteration 3: `accept`（独立子代理 `ses_03cb399ddffeu4tZsfUtLsEpNP`，fresh session）。最终核验：`P1-MA2-054` 计数=0（全部用规范 P2-MA2-054）；所有 P1-MA2-050 引用一致 "resolved R1.17 方案B"；L85 HEAD 复核显式 "方案B = INLINE + 守卫，非方案A Processor 迁移"；arm-index P1-MA2-050=resolved R1.17 / P2-MA2-054=closed MR5 R5.8 双 confirmed；Plan Status: draft / Phase: planned / 24 checkboxes intact。**无阻塞 issue**，共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.15 报告 9 段齐全 + UC-PUR-01/08 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.15 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；**触及会计过账逻辑（PurInvoicePostingDispatcher/PurPaymentPostingDispatcher/PostingProvider/VoucherFact/PostingProcessor 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 已完成（2026-08-03）。A1.15 切片审计报告 `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md` 9 段齐全；UC-PUR-01 接受 on ①②③⑥⑦⑧ + P2 on ④⑤ 命名漂移（P2-RC-011）+ P1 on 承付恢复（reuse P1-MA2-083 重开）；UC-PUR-08 接受 on ①②③⑥ + P1 on ④ 多供应商拆分（P1-RC-017）+ P2 on ⑤ 幂等漂移（P2-RC-012）；零 P0。resolved finding HEAD 复核：P1-MA2-050/P2-MA2-054/P1-MA2-003 已落地，P1-MA2-083 方案B Deferred 在 Q4=(a) 下重开。arm-index.md 已更新（3 新 finding + 1 reuse 交叉引用 + A1.15 cross-ref block）。§8 checker actual = baseline 全等。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计待由独立子代理（新会话）执行
- Evidence: 报告路径 `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`；arm-index 行 `P1-RC-017` / `P2-RC-011` / `P2-RC-012` + A1.15 RC 交叉引用注记 + P1-MA2-083 行 RC 交叉引用注记；§8 checker 表（actual = baseline 全等）

Follow-up:

- finding 修复属 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）successor，非阻塞本审计闭环（§Deferred But Adjudicated 已 adjudicated）
