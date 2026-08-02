# 2026-08-03-0100-3 rc-ma1-a1-17-purchase-f3-returns-business-finance purchase-F3 退货与业财需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.17（MA1 需求追踪矩阵审计 — purchase-F3 退货与业财：UC-PUR-04 采购退货 + UC-PUR-07 业财一体过账(入库与发票)）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.17
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.17 的 0.2 依赖）、`2026-08-03-0100-1-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（A1.15 同批，主流程入库/发票为退货原单与业财过账前置）、`2026-08-03-0100-2-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`（A1.16 同批，三单匹配/差异过账同域参考）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，业财过账引擎 GR/IR + AP 凭证范式为 UC-PUR-07 同根因参考）、`2026-08-02-2250-3-rc-ma1-a1-14-hr-f3-payroll-survey.md`（A1.14 done，最新同范式参考）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.17 给出 UC 清单 = `UC-PUR-04/07`（2 UC），锚点 `use-cases.md:104 / :172`（baseline inventory :88/:91 + 切片索引确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/purchase/use-cases.md`：
  - UC-PUR-04 采购退货（`:104`）：已入库的货物退回供应商。行为链路：创建退货单(关联原入库单)→审核通过。验收标准：①退货单.来源单号==入库单.单号（回链原入库）；②库存余额[物料,仓库].可用量 -= 退货明细数量之和（库存 outgoing）；③红冲过账(反方向)：存在凭证 businessType==入库红冲 且 来源单号==退货单.单号；④原入库单关联凭证被标记红冲（isReversed）；⑤若已开票：若入库单已有发票→该发票需红冲或生成贷项（见 returns.md）。
  - UC-PUR-07 业财一体过账(入库与发票)（`:172`）：验证业务单据审核自动触发财务过账。验收标准：①入库过账(审核时)：入库单.审核通过→生成凭证(businessType=GOODS_RECEIPT, 来源=入库单) + 凭证行 借存货科目 贷暂估应付(GR/IR) + 写入业财回链(来源类型=采购入库) + 入库单.已过账=true；②发票过账：发票.审核通过→生成凭证 借暂估应付(GR/IR)+进项税 贷应付账款 + 发票.已过账=true；③多币种：凭证行.本位币金额==源币金额*汇率；④反审核：入库单.反审核→删除关联凭证(经业财回链反查) + 入库单.已过账=false；⑤期间控制：若期间.总账状态==已结账→入库单/发票不可过账/不可反审核。

- **L2 owner doc 设计参考**：`docs/design/purchase/returns.md`（采购退货 + §红字发票处理；**P2-MA2-006 resolved plan 2026-07-29-2322-1：owner doc 已更新为反映 credit-memo-via-return 实现——顶部"实现偏离记录"块说明 PURCHASE_RETURN 过账 + 负 ArApItem credit memo + 功能等价性[AP 余额回减经辅助账层 sumOpen] + 裁决[保留 credit-memo-via-return 不回退]，流程图重写为实现实际路径，历史红字 ErpPurInvoice 构想保留为参考**）+ `docs/design/purchase/state-machine.md`（退货单状态机）+ `docs/design/finance/posting.md`（业财过账机制 + GR/IR + 红冲）+ `docs/design/finance/period-close.md`（期间控制）+ `docs/design/purchase/README.md`。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验，路径已确认存在）**：
  - **采购退货（UC-PUR-04）**：`ErpPurReturnBizModel.java`（创建退货单关联原入库单 + 来源回链）+ `processor/ErpPurReturnApproveProcessor.java`（R6.5；退货审核 → 库存 outgoing[IErpInvStockMoveBiz Facade] + PURCHASE_RETURN 过账 + 红冲原 GOODS_RECEIPT 凭证）+ `processor/ErpPurReturnProcessor.java`（D-mutation facade）+ `processor/ErpPurReturnReverseApproveProcessor/Cancel/Reject/WithdrawApproval/SubmitForApprovalProcessor`（per-mutation 族）+ `ErpPurReturnLineBizModel.java`（退货明细）+ `posting/PurReturnPostingDispatcher.java`（PURCHASE_RETURN 过账 + 红冲原凭证 isReversed）。
  - **业财过账（UC-PUR-07 入库 + 发票）**：`posting/PurInvoicePostingDispatcher.java`（PURCHASE_INVOICE 过账：借 GR/IR + 进项税 贷 应付账款）+ `posting/PurPaymentPostingDispatcher.java`（PAYMENT，归 A1.15 核验侧）+ `posting/PurReturnPostingDispatcher.java`（PURCHASE_RETURN，归 UC-PUR-04）+ `posting/PurReversalListener.java`（finance→purchase 反向回滚 Invoice/Receipt/Return；**P1-MA2-051 rollbackReceive 不对称：冲销后 receive APPROVED+posted=false 悬挂——receive 侧归本切片 UC-PUR-07 反审核/红冲行为核验**）。**GOODS_RECEIPT 触发路径**：无独立 PurReceivePostingDispatcher（执行时核验：经 ErpPurReceiveApproveProcessor 内嵌 or IErpFinAcctDocProvider 注册 or 缺失——归 A1.15 主核验，本切片 UC-PUR-07 ①验收标准侧复核 GOODS_RECEIPT 凭证行 借存货 贷GR/IR 完整性）。
  - **已开票退货（UC-PUR-04 ⑤）**：贷项/红冲发票处理（执行时核验：经负 ArApItem credit memo [P2-MA2-006 resolved 实现] or 红字 ErpPurInvoice——returns.md 已裁决 credit-memo-via-return 路径，本切片核验运行时是否落实）。
  - **多币种（UC-PUR-07 ③）**：凭证行本位币金额==源币*汇率（执行时核验；**P1-MA2-002 多币种状态机角度无影响**，本切片核验过账金额计算视角）。
  - **期间控制（UC-PUR-07 ⑤）**：期间 CLOSED 时入库/发票不可过账/不可反审核（执行时核验；与 A1.6 finance-F6 期间结账交叉）。
  - **承付恢复（UC-PUR-04 退货后承付）**：`ErpPurReturnProcessor`/相关 Processor 退货后承付恢复（**P1-MA2-083 resolved R1.27 AP/AR 发票冲销恢复承付**——HEAD 复核退货侧是否同样恢复承付）。

- **L4 测试证据现状**：`TestErpPurReturnApproval`（退货审核主路径）+ `TestErpPurReturnQty`（退货数量）+ `TestErpPurReturnInventory`（库存 outgoing）+ `TestErpPurReturnPosting`（PURCHASE_RETURN 过账 + 红冲）+ `TestErpPurReturnTrace`（来源回链）+ `TestErpPurReturnRefundEndToEnd`（已开票退货贷项/红冲端到端）+ `TestErpPurReturnCommitmentRelease`（退货承付恢复）+ `TestErpPurFinanceReversalWriteback`（finance→purchase 反向回写）+ `posting/TestPurReversalListenerReceiveRollback`（P1-MA2-051 receive 回滚不对称）+ `posting/TestErpPurPostingDispatcherFailureHangs`（过账失败悬挂）+ `TestErpPurInvoicePosting`（PURCHASE_INVOICE 凭证 借GR/IR+进项税 贷AP）+ `TestErpPurMultiCurrencyPosting`（多币种本位币金额）+ `TestErpPurToInvToFinPostingEnd`（采购→库存→财务过账端到端）。E2E：`tests/e2e/orchestration/p2p-reverse.spec.ts`（P2P 红冲链）+ `tests/e2e/business-actions/pur-return.action.spec.ts`（退货动作）。**执行时核验断言强度**：UC-PUR-04 ①-⑤ + UC-PUR-07 ①-⑤ 各验收标准是否有强断言（红冲凭证 isReversed 标记 / 已开票退货贷项 / GR/IR 暂估应付科目 / 反审核删凭证 / 期间 CLOSED 拒绝过账）。

- **L5 既有证据（MA2/A4 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8）= purchase 9 实体状态机审查**：Verdict 主路径状态迁移守卫齐全 + reverseApprove 红冲闭环强一致（PROC 路径 doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse 经 IErpFinVoucherBiz Facade）+ 跨域写经 I*Biz Facade。**零 P0**；本切片相关 **P1**：**P1-MA2-051（PurReversalListener.rollbackReceive 不对称——冲销后 receive APPROVED+posted=false 悬挂，归本切片 UC-PUR-07 反审核行为核验，HEAD 复核 resolved 状态）**；**P2**：P2-MA2-006（returns.md red invoice drift **resolved plan 2026-07-29-2322-1**，HEAD 复核 credit-memo-via-return 实现落地）。
  - **`ma2-procure-to-pay-e2e`（A2.8 P2P 端到端审计）**：P2P 链路 + 红冲行为已证实，本切片只补"需求契约↔实际行为"差异（UC-PUR-04/07 验收标准逐条视角）。
  - **`docs/audits/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done）= 业财过账引擎审计**：GOODS_RECEIPT/PURCHASE_INVOICE/红冲凭证链路范式已审（Provider 路由 + VoucherBillR 业财回链 + GR/IR 暂估应付 + isReversed 红冲标记），本切片引用其过账正确性结论，只补采购侧触发契约 + 退货红冲闭环。
  - **注意**：A2.8/P2P e2e/A1.1 覆盖**状态机迁移守卫/事务边界/跨域 Facade/红冲闭环/GR-IR-AP 凭证链路**。本切片从**需求契约↔实现符合性**视角补差异（UC-PUR-04 五条 + UC-PUR-07 五条验收标准逐条 + **resolved finding HEAD 复核**：P1-MA2-051 receive 悬挂 + P2-MA2-006 credit-memo-via-return + P1-MA2-083 承付恢复 + P1-MA2-002 多币种）。

- **arm-index 既有 finding 衔接**：退货与业财相关——`P1-MA2-051`（PurReversalListener.rollbackReceive receive APPROVED+posted=false 悬挂，**resolved 状态 HEAD 复核**，归本切片 UC-PUR-07 反审核行为）/ `P2-MA2-006`（returns.md red invoice drift **resolved plan 2026-07-29-2322-1**，HEAD 复核 credit-memo-via-return 实现）/ `P1-MA2-083`（AP/AR 发票冲销不恢复承付 **resolved R1.27**，HEAD 复核退货侧是否同样恢复承付）/ `P1-MA2-002`（多币种状态机角度无影响，本切片过账金额计算视角复核）/ `P1-MA2-001`（GRNI 冲回，**会计保护区域归 A2.1 finance**，本切片交叉引用不重审）/ `P0-MA1-021`（CostAdjustmentPostingDispatcher Facade **resolved sustained done**，跨域 Facade 范式参考）/ `P1-MA1-022`（跨域只读 daoFor 维持治理缺陷）。UC-PUR-04 红冲凭证 isReversed 标记 / UC-PUR-04 已开票退货贷项/红冲 / UC-PUR-07 GR/IR 暂估应付科目完整性 / UC-PUR-07 期间 CLOSED 拒绝过账 / UC-PUR-07 反审核删凭证 为候选新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` purchase 退货/红冲/GR-IR/AP/期间控制同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；**触及会计过账逻辑（PurReturnPostingDispatcher/PurInvoicePostingDispatcher/PostingProvider/VoucherFact/PostingProcessor/红冲 isReversed/GR-IR 暂估应付 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类——本切片为会计过账密集切片，P0/P1 发现概率高）。

- **剩余差距**：A1.17 切片的五级追踪审计报告缺失 = MA4（A4.1 业财域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.17 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.17 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`，含方法论 §6 **9 段全部内容**：①UC-PUR-04/07 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 Return BizModel/Processor + PurReturnPostingDispatcher + PurInvoicePostingDispatcher + PurReversalListener）③测试证据（注明断言强度）④运行时行为证据（复用 A2.8/P2P e2e/A1.1，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-PUR-04（①退货单来源回链原入库 + ②库存可用量-=退货数量 + ③红冲过账凭证 businessType==入库红冲 来源==退货单 + ④原入库关联凭证 isReversed 标记 + ⑤已开票退货红冲/贷项）+ UC-PUR-07（①入库过账 GOODS_RECEIPT 借存货贷GR/IR + 业财回链 + 已过账=true + ②发票过账 借GR/IR+进项税 贷AP + 已过账=true + ③多币种本位币==源币*汇率 + ④反审核删凭证已过账=false + ⑤期间CLOSED拒绝过账/反审核），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-PUR-04 红冲凭证 isReversed 标记完整性**（HEAD 核验：原入库关联凭证是否被标记 isReversed；缺失→P0④会计过账正确性破坏 or P1①按 §2 定级，**会计类 Q4 无例外**）+ UC-PUR-04 已开票退货贷项/红冲（HEAD 核验 credit-memo-via-return 运行时落地）+ UC-PUR-07 GR/IR 暂估应付科目完整性（HEAD 核验凭证行 借存货 贷GR/IR + 借GR/IR+进项税 贷AP）+ UC-PUR-07 期间CLOSED拒绝过账（HEAD 核验）+ UC-PUR-07 反审核删凭证（HEAD 核验）+ **resolved finding HEAD 复核**：P1-MA2-051（receive 悬挂 resolved 状态）+ P2-MA2-006（credit-memo-via-return resolved plan 2026-07-29-2322-1）+ P1-MA2-083（承付恢复 resolved R1.27，退货侧）+ P1-MA2-002（多币种）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / purchase use-cases / returns.md / state-machine.md / finance/posting.md / period-close.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.17 只覆盖 UC-PUR-04/07；UC-PUR-01/08 归 A1.15，UC-PUR-02/03/05/06 归 A1.16）。**UC-PUR-07 ① GOODS_RECEIPT 触发路径归 A1.15 主核验**，本切片核验 GOODS_RECEIPT 凭证行 借存货 贷GR/IR 完整性（会计正确性视角）；**P1-MA2-001 GRNI 冲回归 A2.1 finance 会计保护区域**，本切片交叉引用不重审；**UC-PUR-07 ⑤ 期间控制与 A1.6 finance-F6 期间结账交叉**，本切片核验采购侧过账拒绝行为，不重审期间结账机制。
- **不重跑既有状态机/P2P 链路/业财过账引擎审计**（§去重协议：A2.8/P2P e2e/A1.1 已证实状态机迁移/红冲闭环/GR-IR-AP 凭证链路，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.17 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.17 UC 锚点）+ `docs/design/purchase/use-cases.md`（L1 真相源）+ `docs/design/purchase/returns.md` + `state-machine.md` + `README.md` + `docs/design/finance/posting.md` + `period-close.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.8/P2P e2e/A1.1 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.8/P2P e2e/A1.1 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurReturnApproval,TestErpPurReturnPosting,TestErpPurReturnRefundEndToEnd,TestErpPurReturnCommitmentRelease,TestErpPurInvoicePosting,TestErpPurMultiCurrencyPosting,posting/TestPurReversalListenerReceiveRollback,posting/TestErpPurPostingDispatcherFailureHangs`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`（落盘 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [ ] `Proof` 对 UC-PUR-04/07 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:104/:172` 验收标准原文（禁止转述）；L2 引用 `returns.md`（采购退货 + §红字发票处理 + P2-MA2-006 resolved credit-memo-via-return 实现偏离记录，标注"设计参考，冲突以 L1 为准"）+ `state-machine.md`（退货单状态机）+ `finance/posting.md`（业财过账 + GR/IR + 红冲）+ `finance/period-close.md`（期间控制）；L3 引用 `ErpPurReturnBizModel.java:line`（关联原入库 + 来源回链）+ `processor/ErpPurReturnApproveProcessor:line`（库存 outgoing Facade + PURCHASE_RETURN 过账 + 红冲）+ `ErpPurReturnLineBizModel.java:line` + `posting/PurReturnPostingDispatcher:line`（PURCHASE_RETURN + 红冲 isReversed）+ `posting/PurInvoicePostingDispatcher:line`（PURCHASE_INVOICE 借 GR/IR+进项税 贷 AP）+ `posting/PurReversalListener:line`（rollbackReceive P1-MA2-051）；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.8/P2P e2e/A1.1 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-PUR-04——①退货单.来源单号==入库单.单号（回链）；②库存可用量-=退货明细数量之和（IErpInvStockMoveBiz Facade outgoing）；③红冲过账凭证 businessType==入库红冲 且 来源==退货单.单号（**关键会计正确性**）；④原入库单关联凭证被标记红冲（isReversed，**关键会计正确性**）；⑤已开票退货红冲/贷项（credit-memo-via-return 运行时落地）。UC-PUR-07——①入库过账 GOODS_RECEIPT 凭证行 借存货 贷暂估应付(GR/IR) + 业财回链(来源类型=采购入库) + 入库单.已过账=true；②发票过账 借暂估应付(GR/IR)+进项税 贷应付账款 + 发票.已过账=true；③多币种 凭证行.本位币金额==源币金额*汇率；④反审核 入库单.反审核→删除关联凭证(业财回链反查) + 已过账=false；⑤期间控制 期间.总账状态==已结账→不可过账/不可反审核。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` **resolved finding HEAD 复核**（会计正确性类关键证据）：**P1-MA2-051 PurReversalListener.rollbackReceive receive 悬挂**——HEAD 复核 resolved 状态（arm-index grep 确认；若仍 open 则按 §2 重新定级，冲销后 receive APPROVED+posted=false 悬挂属会计正确性类 Q4 维持 P1 触发 MR1）；**P2-MA2-006 returns.md red invoice drift（resolved plan 2026-07-29-2322-1）**——HEAD 复核 credit-memo-via-return 实现运行时落地（负 ArApItem credit memo + AP 余额回减经辅助账层 sumOpen）；**P1-MA2-083 承付恢复（resolved R1.27）**——HEAD 复核退货侧是否同样调 commit() 恢复承付（与 A1.15 invoice 侧对称性核验）；P1-MA2-002 多币种（过账金额计算视角复核）。逐条记录复核结论（已落地/回退/部分落地/仍 open successor）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-PUR-04 红冲凭证 isReversed 标记（HEAD 复核：完整→接受 on ④；缺失→P0④会计过账正确性破坏 or P1①按 §2 定级，**会计类 Q4 无例外**）；UC-PUR-04 已开票退货贷项/红冲（HEAD 复核 credit-memo-via-return）；UC-PUR-07 GR/IR 暂估应付科目完整性（HEAD 复核：缺失→P0④会计过账正确性）；UC-PUR-07 期间CLOSED拒绝过账（HEAD 核验）；UC-PUR-07 反审核删凭证（HEAD 核验）；UC-PUR-07 多币种本位币计算（HEAD 核验）；resolved finding HEAD 复核（P1-MA2-051/P2-MA2-006/P1-MA2-083/P1-MA2-002）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-PUR-04/07 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.8/P2P e2e/A1.1 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口有明确分级（非悬空"待查"）；**UC-PUR-04 红冲 isReversed + UC-PUR-07 GR/IR 科目完整性 HEAD 复核结论已记录（会计正确性类 Q4 关键证据）**；P1-MA2-051/P2-MA2-006/P1-MA2-083/P1-MA2-002 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`（落盘 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` purchase 退货/红冲/GR-IR/AP/期间控制同域同控制点（如 P1-MA2-051、P2-MA2-006、P1-MA2-083、P1-MA2-002、P1-MA2-001、P0-MA1-021、P1-MA1-022）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-PUR-04 红冲 isReversed 缺失 / UC-PUR-07 GR/IR 科目缺失 / 期间CLOSED拒绝过账缺失）→ 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-PUR-04/07 业财过账与 A1.1/P1-MA2-001/P1-MA2-083 同根因则交叉引用而非重复新建；P1-MA2-051 receive 悬挂在 A2.8 已登记则复用并追加 RC 视角注记。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如红冲 isReversed 运行时标记 / credit-memo-via-return 运行时 AP 余额回减 / GR/IR 暂估应付运行时凭证行 / 期间CLOSED运行时拒绝过账 / 反审核运行时删凭证 / 退货承付恢复运行时对称性；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 A2.8（9 实体状态机 + reverseApprove 红冲闭环 + 跨域 Facade + **P1-MA2-051 receive 悬挂 + P2-MA2-006 returns.md resolved** finding）+ P2P e2e（P2P 红冲链路行为）+ A1.1（业财过账引擎 GR/IR + AP + 红冲 isReversed 凭证范式）已证实结论，列明本切片只补的需求视角差异（UC-PUR-04 五条 + UC-PUR-07 五条验收标准逐条 + resolved finding HEAD 复核 P1-MA2-051/P2-MA2-006/P1-MA2-083/P1-MA2-002 落地确认）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [ ] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03cbae119ffeQTUT0qsrqQQ390`，fresh session，未起草本计划）。逐项实测核验：roadmap 对齐（A1.17 / UC-PUR-04/07 共 10 验收标准 / Deps=0.2 done / Skill）、UC 锚点 :104/:172 全匹配、L3 全部 7 代码路径 + L4 8 JUnit + 2 E2E 全存在、**7 项 L5 resolved-status 全 confirmed**（P1-MA2-051[归本切片, arm-index:255 resolved R1.17] / P2-MA2-006[arm-index:451 resolved plan 2026-07-29-2322-1 credit-memo-via-return] / P1-MA2-083[arm-index:284 resolved R1.27] / P1-MA2-002[arm-index:224 resolved plan 2026-07-29-2322-2 方案 A] / P1-MA2-001[arm-index:223 resolved 方案 B documented simplification, 归 A2.1 finance 保护区] / P0-MA1-021[arm-index:157 sustained done] / P1-MA1-022[arm-index:221 resolved]）；跨切片边界全 stated（UC-PUR-01/08→A1.15、UC-PUR-02/03/05/06→A1.16、GOODS_RECEIPT 触发→A1.15 本切片仅核验凭证行 借存货贷GR/IR 完整性、P1-MA2-001→A2.1 finance、UC-PUR-07 ⑤期间控制→A1.6 finance-F6）；**UC-PUR-04 ③④ 红冲 isReversed + UC-PUR-07 ①② GR/IR 科目完整性 Q4 framing sound**（每项显式"关键会计正确性"、缺失→P0④、会计类 Q4 无例外、无 scheme B/技术不可行降级、修复→MR0/MR1、会计过账核心路径 ask-first）。**无阻塞 issue**，共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核（含会计正确性类 P1-MA2-051/GR-IR/isReversed）+ finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.17 报告 9 段齐全 + UC-PUR-04/07 逐矩阵行 + resolved finding HEAD 复核（含 P1-MA2-051 receive 悬挂 + UC-PUR-04 红冲 isReversed + UC-PUR-07 GR/IR 科目 会计正确性类）+ finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.17 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；**触及会计过账逻辑（PurReturnPostingDispatcher/PurInvoicePostingDispatcher/PostingProvider/VoucherFact/PostingProcessor/红冲 isReversed/GR-IR 暂估应付 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类——本切片为会计过账密集切片）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <关闭时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立审计者或独立子代理>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- finding 修复属 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）successor，非阻塞本审计闭环（§Deferred But Adjudicated 已 adjudicated）
