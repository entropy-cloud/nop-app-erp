# A1.16 purchase-F2 三单匹配与差异 需求-实现符合性审计报告（rc-ma1-a1-16）

> Mission: requirement-compliance · Work Item: A1.16（UC-PUR-02 三单匹配 + UC-PUR-03 部分入库与分批收货 + UC-PUR-05 价格差异 + UC-PUR-06 数量差异）
> 来源计划: `docs/plans/2026-08-03-0100-2-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`
> 方法论: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 审计类型: 只读审计（无代码/ORM/api.xml/view.xml/真相源变更）
> 审计日期: 2026-08-03

## 9. 与既有 MA2 / P2P e2e / A1.1 / A1.15 报告的差异增量声明（前置）

本报告是 **requirement-compliance** mission MA1 切片 A1.16 的五级追踪审计，视角 = **需求契约（L1 use-cases）→ 实现符合性**。按 §去重协议，以下既有审计已证实的结论本报告**直接复用，不重审**：

- **A2.8**（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）：purchase 9 实体状态机迁移守卫齐全 + 跨域写经 I*Biz Facade + reverseApprove 红冲闭环。settle/reverseSettlement 守卫已审（P1-MA2-003 维持）。本报告复用其 L5 行为证据。
- **A2.1 P2P e2e**（`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`）：P2P 链路行为已证实（三单匹配 / 部分入库 / 差异处理主路径完整）+ P1-MA2-003 settle 三单匹配复核 resolved（plan 2026-07-29-2322-1 方案 A）+ P2-MA2-004 dead config read watch-only + P2-MA2-005 owner doc 内部不一致 watch-only + P2-MA2-007 价格锁缺失 watch-only + P2-MA2-008 settle 并发归 A2.17。
- **A1.1**（`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`）：业财过账引擎范式（Provider 路由 + VoucherBillR 业财回链 + GR/IR 暂估应付）已审，本切片引用其过账正确性结论，只补采购发票 AP_INVOICE 的价格差异过账完整性。
- **A1.15**（`docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`）：UC-PUR-01/08 主流程与请购已审（GOODS_RECEIPT→PURCHASE_INPUT / PURCHASE_INVOICE→AP_INVOICE 命名漂移 P2-RC-011 + 承付恢复 reuse P1-MA2-083 重开 + 多供应商拆分 P1-RC-017）。本切片核验三单匹配触发路径归 A1.15（发票 approve 调 `threeWayMatcher.match`），本切片只补"三单匹配规则 + 差异处理"需求视角。

本报告**只补需求视角差异**：(i) UC-PUR-02 四条验收标准逐条（回链三元组 / 数量匹配超收容差 / 价格匹配价格容差+匹配状态 / 可追溯）；(ii) UC-PUR-03 四条验收标准逐条（第一次后已入库60 / 第二次后已入库100派生 / 两次入库各自独立过账凭证数==2 / 未全部入库前不自动关闭）；(iii) UC-PUR-05 四条验收标准逐条（差异计算 / 价格容差触发匹配状态 / 三处理策略{拒绝/审批后接收/接收并过账差异}完整性 / 让步接收价格差异科目过账行）；(iv) UC-PUR-06 五条验收标准逐条（短收数量计算 / <=容差继续或关闭 / >容差触发差异处理 / 按实际入库过账非订单 / 关闭()→作废+释放预留）；(v) **resolved/watch-only finding HEAD 复核**：P1-MA2-003（方案A 落地确认）+ P2-MA2-004/005/007（维持 watch-only 确认）；(vi) **新发现需求分歧**：UC-PUR-05 ③④ 价格差异处理不完整（P1-RC-018）+ UC-PUR-02 ② 超收容差校验 receive-vs-order 完全缺失（P1-RC-019）+ UC-PUR-03 ①② 订单行 receivedQuantity 列存在但从未写入（P2-RC-013）+ UC-PUR-06 ③ 短收超容差差异处理未实现（P2-RC-014）。

---

## 1. 需求契约原文（L1，逐字引用）

> 真相源：`docs/design/purchase/use-cases.md`（层级 2 功能契约，§4 真相源层级）。以下逐字引用，不转述。

### UC-PUR-02 三单匹配(订单/入库/发票)（`use-cases.md:55`）

| 项目 | 原文 |
|------|------|
| 场景 | 验证订单、入库、发票三方一致性。见 three-way-match.md §匹配规则。 |
| 可验证断言 | `// 回链三元组` `发票行.来源单类型 == 采购入库` / `发票行.来源单号 == 入库单.单号` / `发票行.来源行号 == 入库行.行号`<br>`// 数量匹配(见 §数量匹配)` `入库数量之和 <= 订单数量 * (1 + 超收容差)`<br>`// 价格匹配(见 §价格差异)` `|发票单价 - 订单单价| <= 订单单价 * 价格容差` `否则 → 匹配状态 = 价格差异待处理`<br>`// 可追溯` `每条发票行 → 可追溯到入库行 与 订单行` |
| 涉及机制 | three-way-match.md |

### UC-PUR-03 部分入库与分批收货（`use-cases.md:81`）

| 项目 | 原文 |
|------|------|
| 场景 | 一个订单分多次入库(部分到货)。 |
| 行为链路 | `订单(数量=100) 审核通过` / `第一次入库(数量=60) 审核通过` / `第二次入库(数量=40) 审核通过` |
| 可验证断言 | `订单行.已入库数量 == 60     // 第一次后` / `订单行.已入库数量 == 100    // 第二次后(派生字段)` / `凭证数量 == 2              // 两次入库各自独立过账` / `订单.单据状态 != 已关闭     // 未全部入库前不自动关闭` |
| 涉及机制 | state-machine.md、three-way-match.md §数量匹配 |

### UC-PUR-05 价格差异(发票价 ≠ 订单价)（`use-cases.md:130`）

| 项目 | 原文 |
|------|------|
| 场景 | 供应商发票单价高于采购订单约定价。见 three-way-match.md §价格差异。 |
| 可验证断言 | `差异 = 发票单价 - 订单单价` / `若 |差异| > 订单单价 * 价格容差:` `匹配状态 = 价格差异待处理` / `// 处理策略(见 §不匹配的处理策略)` `策略 ∈ {拒绝, 审批后接收, 接收并过账差异}` / `// 让步接收时` `存在过账行: 科目 == 价格差异科目 且 金额 == 差异 * 数量` |
| 涉及机制 | three-way-match.md §价格差异/§不匹配的处理策略 |

### UC-PUR-06 数量差异(入库 ≠ 订单)（`use-cases.md:151`）

| 项目 | 原文 |
|------|------|
| 场景 | 入库数量少于订单(短收)。 |
| 可验证断言 | `短收数量 = 订单数量 - 入库数量之和` / `若 短收数量 <= 容差: 订单可继续入库或手动关闭` / `若 短收数量 > 容差: 触发差异处理(见 §数量差异)` / `// 按实际入库过账,不按订单` `凭证金额 基于 实际入库数量, 非 订单数量` / `// 长期未收的余量` `订单.关闭() → 单据状态 = 已作废, 释放预留` |
| 涉及机制 | three-way-match.md §数量差异 |

---

## 2. 实现证据（L3，`file:line`，含跨域调用链）

### 三单匹配核心（UC-PUR-02/05/06 共用）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ThreeWayMatcher.java:37-108` — 匹配引擎。回链路径（经实时仓库核实的字段名，非 design 概念名）：发票行 `receiveLineId`（`:63`）→ `loadReceiveLine`（`:66`/`:149-152`）→ 入库行 `orderLineId`（`:88`，`_ErpPurReceiveLine.java` 字段，**非** design 概念名 `source_order_line_id`）→ `loadOrderLine`（`:89`/`:154-157`）→ 订单行 `unitPrice`（`:92`）。
  - 数量匹配（`:71-85`）：`invoiceQty.compareTo(receivedQty) > 0` → strict 抛 `ERR_INVOICE_QTY_MISMATCH` / 非 strict `LOG.warn` 放行。**强制不得超入库为基线，无容差**（`:53` 注释「qtyTolerance 当前保留为配置项占位」+ `:54-56` qtyTolerance 计算后被空守护置零，**invoice 侧未使用**——P2-MA2-004 dead config read watch-only 确认）。
  - 价格匹配（`:87-106`）：`orderPrice.signum() > 0 && priceDiffPercent(invoicePrice, orderPrice).compareTo(priceTolerance) > 0`（`:93`，`priceDiffPercent = |invoicePrice-orderPrice|/orderPrice*100` `:110-115`）→ strict 抛 `ERR_INVOICE_PRICE_MISMATCH` / 非 strict `LOG.warn` 放行。**无"匹配状态=价格差异待处理"持久化字段**（invoice 无 matchStatus 列，仅 strict 拒绝 / 非 strict warn 放行二元行为）。
  - **receive-vs-order 超收容差校验完全缺失**——`match` 方法只遍历 invoice lines 做 invoice-vs-receive 比较，**无 receive-vs-order 容差校验**；`erp-pur.match-qty-tolerance` 配置两侧（invoice-vs-receive + receive-vs-order）均未应用（P1-RC-019 详述）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceProcessor.java:161-165` — `validateBusinessRulesForApprove`：`requireSupplierActive` + `threeWayMatcher.match(invoice.getCode(), lines, null)`（发票审核时执行三单匹配，strictOverride=null 走配置）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurInvoiceApproveProcessor.java:18-31` — override `approve`：`validateBusinessRulesForApprove`（含三单匹配）+ `doPosting`（AP_INVOICE）+ `doApprove` + `runCommitmentReleaseOnInvoiceApproveHook`。

### 部分入库/分批（UC-PUR-03）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveApproveProcessor.java:26-49` — override `approve`：每次入库独立 `triggerIncomingMove`（`:37`）+ `applyPostingResult`（`:43`，`receive.posted = move.getPosted()`）+ `setReceiveStatus(RECEIVED)` + `postProcessApprove`（`rollupOrderReceiveStatus`）。**两次入库各自独立过账**（每次 approve → 一次 stockMove → 一次 PURCHASE_INPUT 凭证）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java:215-219/221-227/244-284` — `triggerIncomingMove`（`IErpInvStockMoveBiz.generateMove` Facade）+ `applyPostingResult`（posted 来自 move.getPosted()）+ `rollupOrderReceiveStatus`（按 orderId 聚合 receive lines 累计数量 → 计算任何已收/全部收齐 → `orderBiz.updateReceiveStatus(orderId, rolled, context)` 设订单 header `receiveStatus`: UNRECEIVED/PARTIAL/RECEIVED）。**仅更新 header receiveStatus，不写 orderLine.receivedQuantity**（P2-RC-013 详述）。
- `module-purchase/erp-pur-dao/src/main/java/app/erp/pur/dao/entity/_gen/_ErpPurOrderLine.java:77-78/268/1131-1141` + `module-purchase/model/app-erp-purchase.orm.xml:636` — `receivedQuantity`（已收货数量）列存在（defaultValue=0），但 `rg setReceivedQuantity` 生产代码零 writer（仅 `_gen` 框架 setter）→ **列始终为 0**。

### 价格差异过账（UC-PUR-05 ④）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java:64-94` — `createFacts` AP_INVOICE 分支（`:74-82`）仅 3 行 fact：`SUBJECT_PURCHASE`（1403 在途物资, DEBIT, TOTAL_AMOUNT=invoice.totalAmount 不含税）+ `SUBJECT_INPUT_VAT`（2221 进项税, DEBIT, TOTAL_TAX_AMOUNT）+ `SUBJECT_ACCOUNTS_PAYABLE`（2202 应付账款, CREDIT, TOTAL_AMOUNT_WITH_TAX 价税合计）。**无"价格差异科目"过账行**（无 1404/6601 PPV 科目行；无 `差异*数量` 金额计算）——P1-RC-018 详述。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurInvoicePostingDispatcher.java:71-89` — `buildEvent`：billData 仅放 TOTAL_AMOUNT/TOTAL_TAX_AMOUNT/TOTAL_AMOUNT_WITH_TAX/SUPPLIER_ID，**无 invoice-vs-order 价格差异数据传递给 Provider**（Provider 无从计算差异行）。

### 数量差异/短收（UC-PUR-06）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ThreeWayMatcher.java:71-85` — invoice-vs-receive 数量校验（invoice 不得超入库，强制无容差）。**无 receive-vs-order 短收容差校验**（P2-RC-014 详述）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java:166-168` — `validateBusinessRulesForApprove` 仅 `requireSupplierActive`，**无短收/超收容差校验**。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderCancelProcessor.java:16-60` — `cancel` → `beforeCancel`（`:41-44`）→ `processor.runCommitmentReleaseHook(entity, context)`（释放预算承付预留，config-gated `erp-fin.budget-commitment-enabled` 默认 false）+ `runIntercompanyReverseHook` + `doCancel`（docStatus=CANCELLED）。**关闭释放预留已实现（config-gated）**。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurOrderProcessor.java:222-233` — `runCommitmentReleaseHook`（调 `budgetCommitmentBiz.release`，容错 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 静默跳过）。
- 凭证金额基于实际入库：`ErpPurReceiveProcessor.triggerIncomingMove` → `ReceiveStockMoveBuilder.build(receive, lines, context)` 使用 receive line 实际数量（非订单数量）→ InvPostingDispatcher PURCHASE_INPUT 凭证金额基于实际入库数量（A2.1/A2.4 已证实）。

### 付款核销三单匹配复核（UC-PUR-02 ④ 可追溯 + 三单匹配完成态）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/PaymentSettler.java:60/151-191` — `@Inject ThreeWayMatcher threeWayMatcher`（`:60`）+ `requireInvoiceForSettle`（`:151-175`，APPROVED 守卫 + 供应商匹配 + **config-gated `erp-pur.settle-recheck-three-way-match` 默认 false** 调 `recheckThreeWayMatchAtSettle`）+ `recheckThreeWayMatchAtSettle`（`:181-191`，强制 strict `threeWayMatcher.match(code, lines, Boolean.TRUE)`，失败包装 `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED` 含 cause 链）。**R1.8 P1-MA2-003 方案 A 落地**。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 覆盖 | 断言强度 |
|------|------|---------|
| `TestErpPurThreeWayMatch.java` | UC-PUR-02/05 三单匹配（严格模式数量超入库拒绝 ERR_INVOICE_QTY_MISMATCH + 严格模式价格超容差拒绝 ERR_INVOICE_PRICE_MISMATCH + 非严格模式放行 + 无 receiveLineId 跳过 + 容差内通过 + 集成路径 approve 非严格放行）| **强断言**（每场景一 @Test，断言错误码 + 状态） |
| `TestErpPurSettleThreeWayMatchRecheck.java` | P1-MA2-003 方案 A settle 三单匹配二次门控（价格超容差拒绝 ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED + 数量超入库拒绝 + 匹配通过 settle 成功 paidStatus=PARTIAL）| **强断言**（3 @Test，cause 链 + paidAmount 保持 0） |
| `TestErpPurReceiveApproval.java` | UC-PUR-03 入库审批状态机（submit/reject/resubmit + 非法迁移拒绝 + 供应商停用拒绝 + 草稿作废）| **强断言**（单次入库 approve 状态迁移；**无两次入库独立过账凭证数==2 断言**） |
| `TestErpPurReceiveStockMove.java` | UC-PUR-03 入库库存 incoming（IErpInvStockMoveBiz.generateMove）| 强断言（单次入库） |
| `TestErpPurOrderApproval.java` | UC-PUR-06 ⑤ 订单审批 + 作废（cancel → CANCELLED + 已作废不可提交）| **强断言**（含 SoD 守卫）；**未断言 cancel 后 commitment release 回写**（config-gated 默认 false） |
| `TestErpPurInvoiceApproval.java` | UC-PUR-05 发票审批状态机（三轴 + 供应商停用 + 作废）| 强断言（状态迁移；**无价格差异过账行断言**——本切片关键缺口） |
| `TestErpPurProcureToPayEnd.java`（A1.15 引用）| P2P 全链含部分入库场景 | 强断言（A1.15 已审） |
| E2E `tests/e2e/orchestration/p2p-chain.spec.ts`（A1.15 引用）| P2P 全链 E2E | 强断言（A1.15 已审） |

**断言强度缺口（本切片关键）**：
- **UC-PUR-05 ④ 价格差异科目过账行零测试**——无任何测试断言"让步接收时存在过账行：科目==价格差异科目 且 金额==差异*数量"（L1 字面验收标准零覆盖；与 L3 实现缺失一致——P1-RC-018）。
- **UC-PUR-03 ③ 两次入库独立过账凭证数==2 零直接断言**——`TestErpPurReceiveApproval` 仅单次入库 approve；无构造"订单(100)→第一次入库(60)→第二次入库(40)→断言凭证数==2"的测试（行为由 per-mutation approve 架构隐含，但无显式断言）。
- **UC-PUR-03 ①② 订单行.已入库数量零断言**——无测试断言 `orderLine.receivedQuantity == 60/100`（与列始终为 0 一致——P2-RC-013）。
- **UC-PUR-02 ② 超收容差零断言**——无测试构造"入库数量 > 订单数量*(1+容差)→拒绝入库审核"（与实现缺失一致——P1-RC-019）。

---

## 4. 运行时行为证据（L5）

按 §去重协议，L5 行为证据复用 A2.8 / A2.1 P2P e2e / A1.1 / A1.15 已证实结论：

- **9 实体三轴状态机 + 跨域 Facade + reverseApprove 红冲闭环**：行为已证实（A2.8）。
- **P2P 链路行为**：三单匹配 / 部分入库 / 差异处理 主路径行为已证实（A2.1）。settle 守卫齐全（P1-MA2-003 维持）。
- **业财过账引擎范式**：GR/IR 暂估应付 + AP 应付 + 跨域经 IErpFinVoucherBiz.post() REQUIRES_NEW Facade（A1.1）。
- **三单匹配规则行为**：invoice-vs-receive 数量硬拒绝 + invoice-vs-order 价格容差 + strict/non-strict 开关（A2.1 §2.1 已证实）。
- **GOODS_RECEIPT→PURCHASE_INPUT / PURCHASE_INVOICE→AP_INVOICE 命名漂移行为等价**（A1.15 P2-RC-011）。

**本切片差异（需求契约↔行为）**：
- UC-PUR-02 ② 入库数量之和 <= 订单数量*(1+超收容差)——**receive-vs-order 超收容差校验完全缺失**，超收运行时无门控（P1-RC-019）。
- UC-PUR-02 ③ |发票单价-订单单价|<=订单单价*价格容差 否则→匹配状态=价格差异待处理——**无"匹配状态=价格差异待处理"持久化字段**（strict 拒绝 / 非 strict warn 放行二元行为，无中间"待处理"态）。
- UC-PUR-03 ①② 订单行.已入库数量==60/100——**receivedQuantity 列存在但始终为 0**（rollupOrderReceiveStatus 仅更新 header receiveStatus，P2-RC-013）；header receiveStatus（UNRECEIVED/PARTIAL/RECEIVED）正确派生。
- UC-PUR-03 ③ 凭证数量==2——**行为由 per-mutation approve 架构隐含成立**（每次入库 approve 独立触发 stockMove→PURCHASE_INPUT 凭证），但无直接断言（静态存疑点 §7）。
- UC-PUR-05 ③ 策略∈{拒绝,审批后接收,接收并过账差异}——**仅"拒绝"实现**（strict mode）；"审批后接收"+"接收并过账差异"未实现为独立策略分支（P1-RC-018）。
- UC-PUR-05 ④ 让步接收价格差异科目过账行——**完全缺失**（PurAcctDocProvider AP_INVOICE 仅 3 行，无价格差异科目行，P1-RC-018）。
- UC-PUR-06 ③ 短收数量>容差→触发差异处理——**完全缺失**（无 receive-vs-order 容差门控 + 无"差异处理"触发，P2-RC-014）。
- UC-PUR-06 ④ 凭证金额基于实际入库——**已实现**（stockMoveBuilder 用 receive line 实际数量，A2.1/A2.4 证实）。
- UC-PUR-06 ⑤ 订单.关闭()→作废+释放预留——**已实现**（cancel→CANCELLED + runCommitmentReleaseHook 释放承付，config-gated 默认 false）。

---

## 5. 五级追踪矩阵 + 每 UC 符合性结论

### 矩阵（4 行，每 UC 一行）

| UC | L1 需求契约 | L2 owner doc（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|----|----|----|----|----|----|
| **UC-PUR-02** | `use-cases.md:55` 三单匹配（①回链三元组 + ②数量匹配超收容差 + ③价格匹配价格容差+匹配状态 + ④可追溯） | `three-way-match.md §匹配规则/§回链关系/§数量匹配/§价格差异`（**P2-MA2-005 内部不一致 watch-only：§一致性规则:104「失败则拒绝」vs §不匹配处理策略:99「默认非严格 warn+放行」**；设计参考，**冲突以 L1 为准**）+ `state-machine.md §异常路径`（三单匹配失败发票审核拦截） | `ThreeWayMatcher.java:37-108`（invoice-vs-receive 数量硬比较 + invoice-vs-order 价格容差 + strict 开关；**receive-vs-order 超收容差校验缺失**）+ `ErpPurInvoiceProcessor.java:161-165`（approve 调 match）+ `ErpPurInvoiceApproveProcessor.java:18-31` + `PaymentSettler.java:151-191`（R1.8 P1-MA2-003 settle 复核） | `TestErpPurThreeWayMatch`（强，5 @Test 覆盖 invoice-vs-receive 数量+invoice-vs-order 价格+strict/非 strict+无回链跳过+容差内）+ `TestErpPurSettleThreeWayMatchRecheck`（强，settle 复核 3 场景） | 行为已证实（A2.8+A2.1）；**② receive-vs-order 超收容差校验完全缺失**（qtyTolerance 配置两侧均未用）；③ 无"匹配状态=价格差异待处理"持久化字段 | **接受 on ①③④；P1 on ② 超收容差校验缺失（P1-RC-019 新）** |
| **UC-PUR-03** | `use-cases.md:81` 部分入库与分批收货（①第一次后已入库60 + ②第二次后已入库100派生 + ③两次入库各自独立过账凭证数==2 + ④未全部入库前不自动关闭） | `state-machine.md §2/§场景A`（多次入库各自 approve 触发库存 incoming）+ `three-way-match.md §数量匹配`（多次部分入库） | `ErpPurReceiveApproveProcessor.java:26-49`（每次入库独立 triggerIncomingMove+applyPostingResult）+ `ErpPurReceiveProcessor.java:215-284`（triggerIncomingMove + rollupOrderReceiveStatus 仅更新 header receiveStatus）+ `_ErpPurOrderLine.java:1131-1141` + ORM `:636`（receivedQuantity 列存在零 writer） | `TestErpPurReceiveApproval`（强，单次入库状态机）+ `TestErpPurReceiveStockMove`（强，单次 incoming）；**无两次入库凭证数==2 断言 + 无 orderLine.receivedQuantity==60/100 断言** | 行为已证实（A2.8+A2.1）；①② receivedQuantity 列存在但始终 0（header receiveStatus 正确派生）；③ per-mutation approve 架构隐含两次独立过账（无直接断言）；④ 订单不自动关闭（receiveStatus 派生不触发 docStatus 变更） | **接受 on ③④；P2 on ①② receivedQuantity 列未写入（P2-RC-013 新）** |
| **UC-PUR-05** | `use-cases.md:130` 价格差异（①差异=发票单价-订单单价 + ②价格容差触发匹配状态 + ③三处理策略{拒绝/审批后接收/接收并过账差异} + ④让步接收价格差异科目过账行 科目==价格差异科目 且 金额==差异*数量） | `three-way-match.md §价格差异/§不匹配的处理策略`（差异超阈值警告/拦截 + 差异计入采购价格差异科目） | `ThreeWayMatcher.java:87-106`（价格容差 + strict 拒绝/非 strict warn；**无"匹配状态=价格差异待处理"持久化**）+ `PurAcctDocProvider.java:74-82`（AP_INVOICE 仅 3 行 1403/2221/2202，**无价格差异科目行**）+ `PurInvoicePostingDispatcher.java:71-89`（billData 无差异数据） | `TestErpPurThreeWayMatch#testPriceOverToleranceRejectedInStrictMode`（强，strict 价格超容差拒绝）；**③ 三策略完整性零断言 + ④ 价格差异科目过账行零断言** | 行为已证实（A2.1 三单匹配价格容差主路径）；**③ 仅"拒绝"实现（strict），"审批后接收"+"接收并过账差异"未实现**；**④ 让步接收价格差异科目过账行完全缺失**（AP_INVOICE 在途物资按发票金额入账，差异埋在存货价值中未分集到 PPV 科目） | **接受 on ①②；P1 on ③④ 价格差异处理不完整（P1-RC-018 新）** |
| **UC-PUR-06** | `use-cases.md:151` 数量差异/短收（①短收数量=订单数量-入库数量之和 + ②<=容差继续/关闭 + ③>容差触发差异处理 + ④按实际入库过账非订单 + ⑤关闭()→作废+释放预留） | `three-way-match.md §数量差异`（入库<订单正常部分收货 + 入库>订单超收超出容差拒绝）+ `state-machine.md §2`（订单 cancel→CANCELLED） | `ThreeWayMatcher.java:71-85`（invoice-vs-receive 数量；**无 receive-vs-order 短收容差**）+ `ErpPurReceiveProcessor.java:166-168`（approve 仅 requireSupplierActive，无短收容差校验）+ `ErpPurOrderCancelProcessor.java:16-60`（cancel→CANCELLED+runCommitmentReleaseHook 释放承付 config-gated）+ `ErpPurOrderProcessor.java:222-233`（runCommitmentReleaseHook）+ `ErpPurReceiveProcessor.triggerIncomingMove`（实际入库数量过账） | `TestErpPurOrderApproval#testOrderCancelFromDraft`（强，cancel→CANCELLED + 已作废不可提交）；**③ 短收超容差差异处理零断言**；④⑤ 经 A2.1 证实 | 行为已证实（A2.1+A2.8）；① 派生可计算（rollupOrderReceiveStatus 内部 map）；② 主路径继续入库 OK；**③ 短收超容差"触发差异处理"完全缺失**；④ 已实现（实际入库过账）；⑤ 已实现（cancel 释放承付 config-gated） | **接受 on ①②④⑤；P2 on ③ 短收超容差差异处理缺失（P2-RC-014 新）** |

### 逐条验收标准分级（§3 完整枚举，17 条）

| # | 验收标准 | UC | HEAD 状态 | 分级 |
|---|---------|-----|---------|------|
| ① | 发票行.来源单类型 == 采购入库 / 发票行.来源单号 == 入库单.单号 / 发票行.来源行号 == 入库行.行号（回链三元组） | PUR-02 | `ErpPurInvoiceLine.receiveLineId`（回链入库行）+ `ErpPurReceiveLine.orderLineId`（回链订单行）经 `ThreeWayMatcher.loadReceiveLine:149-152`/`loadOrderLine:154-157` 实时仓库核实 ✅（字段名 receiveLineId/orderLineId 非 design 概念名 source_receive_line_id/source_order_line_id，命名漂移行为等价，归 P2-RC-011 上下文不单独登记） | 接受 |
| ② | 入库数量之和 <= 订单数量 * (1 + 超收容差) | PUR-02 | **`ThreeWayMatcher.match` 只做 invoice-vs-receive 比较（`:62-107`），无 receive-vs-order 容差校验**；`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive 无 qty 校验；`erp-pur.match-qty-tolerance` 配置 `:52-56/122-124` 读取后空守护置零未使用（P2-MA2-004）；**receive approve 运行时无超收门控** | **P1 → P1-RC-019**（功能实质偏离验收标准，§2 P1①） |
| ③ | \|发票单价 - 订单单价\| <= 订单单价 * 价格容差 否则→匹配状态 = 价格差异待处理 | PUR-02 | `ThreeWayMatcher.java:87-106` 价格容差比较 + strict 拒绝 ERR_INVOICE_PRICE_MISMATCH / 非 strict warn 放行 ✅（价格容差计算正确）；**"匹配状态=价格差异待处理"无持久化字段**（invoice 无 matchStatus 列，仅 strict/非 strict 二元行为） | 接受（价格容差计算正确；"匹配状态待处理"持久化缺失属设计简化，strict 拒绝/非 strict warn 已覆盖语义，非悬空待处理态） |
| ④ | 每条发票行 → 可追溯到入库行 与 订单行（可追溯） | PUR-02 | `ThreeWayMatcher` 回链路径 invoiceLine.receiveLineId→receiveLine→receiveLine.orderLineId→orderLine ✅ + `PaymentSettler.recheckThreeWayMatchAtSettle:181-191`（R1.8 P1-MA2-003 方案 A settle 三单匹配完成态复核，config-gated）✅ | 接受 |
| ⑤ | 订单行.已入库数量 == 60（第一次后） | PUR-03 | **`receivedQuantity` 列存在（ORM `:636` defaultValue=0）但生产代码零 writer**（`rg setReceivedQuantity` 仅 `_gen` 框架 setter）；`rollupOrderReceiveStatus:244-284` 内部计算 receivedByOrderLine map 但仅 `orderBiz.updateReceiveStatus` 更新 header receiveStatus，**不写 orderLine.receivedQuantity** → 列始终 0 | **P2 → P2-RC-013**（派生字段列存在但未写入，header receiveStatus 正确派生） |
| ⑥ | 订单行.已入库数量 == 100（第二次后，派生字段） | PUR-03 | 同 ⑤（receivedQuantity 始终 0） | **P2 → P2-RC-013**（同 ⑤） |
| ⑦ | 凭证数量 == 2（两次入库各自独立过账） | PUR-03 | `ErpPurReceiveApproveProcessor.approve:26-49` 每次 approve 独立 `triggerIncomingMove`→`IErpInvStockMoveBiz.generateMove`→InvPostingDispatcher PURCHASE_INPUT 凭证 ✅（per-mutation 架构隐含两次独立过账）；**无直接"凭证数==2"断言**（静态存疑点 §7） | 接受（行为由架构隐含成立；断言缺口入静态存疑点） |
| ⑧ | 订单.单据状态 != 已关闭（未全部入库前不自动关闭） | PUR-03 | `rollupOrderReceiveStatus:244-284` 仅设 header receiveStatus（UNRECEIVED/PARTIAL/RECEIVED），**不触发 docStatus 变更**；订单 docStatus 仅经 cancel/reverseApprove 显式迁移 ✅ | 接受 |
| ⑨ | 差异 = 发票单价 - 订单单价 | PUR-05 | `ThreeWayMatcher.priceDiffPercent:110-115` `diff = invoicePrice.subtract(orderPrice).abs()` ✅（差异计算正确，转百分比比较） | 接受 |
| ⑩ | 若 \|差异\| > 订单单价 * 价格容差 → 匹配状态 = 价格差异待处理 | PUR-05 | `ThreeWayMatcher.java:93-104` 价格超容差 strict 拒绝 / 非 strict warn ✅（同 ③，"匹配状态待处理"持久化缺失属设计简化） | 接受（同 ③） |
| ⑪ | 策略 ∈ {拒绝, 审批后接收, 接收并过账差异}（三处理策略完整性） | PUR-05 | **仅"拒绝"实现**（strict mode `:99-101` 抛 ERR_INVOICE_PRICE_MISMATCH）；**"审批后接收"未实现为独立策略分支**（无 approval-required 工作流分支，非 strict 直接 warn 放行接近"接收"而非"审批后接收"）；**"接收并过账差异"未实现**（无独立策略 + 依赖 ④ PPV 过账行缺失） | **P1 → P1-RC-018**（功能实质偏离验收标准——三策略仅 1/3 实现，§2 P1①） |
| ⑫ | 让步接收时 存在过账行: 科目 == 价格差异科目 且 金额 == 差异 * 数量 | PUR-05 | **`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行**（1403 在途物资 DEBIT invoice.totalAmount + 2221 进项税 DEBIT + 2202 应付账款 CREDIT withTax）；**无"价格差异科目"行**（无 1404/6601 PPV）；`PurInvoicePostingDispatcher.buildEvent:71-89` billData 无 invoice-vs-order 差异数据传递；**让步接收时差异埋在 1403 在途物资金额中（按发票金额入账）未分集到 PPV 科目** | **P1 → P1-RC-018**（功能完全缺失——价格差异科目过账行零实现，§2 P1①；会计类 Q4 无例外） |
| ⑬ | 短收数量 = 订单数量 - 入库数量之和 | PUR-06 | `ErpPurReceiveProcessor.rollupOrderReceiveStatus:244-284` 内部 receivedByOrderLine map 计算 ✅（派生可计算，虽不持久化到 orderLine.receivedQuantity，见 ⑤⑥） | 接受（派生可计算） |
| ⑭ | 若 短收数量 <= 容差: 订单可继续入库或手动关闭 | PUR-06 | 多次入库各自 approve 互不阻塞 ✅ + `ErpPurOrderCancelProcessor.cancel` 手动关闭 ✅（主路径继续入库/手动关闭 OK，容差门控缺失见 ⑮） | 接受 |
| ⑮ | 若 短收数量 > 容差: 触发差异处理 | PUR-06 | **完全缺失**——`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 无 receive-vs-order 短收容差校验；无"差异处理"触发机制；`erp-pur.match-qty-tolerance` 配置两侧均未用（P1-RC-019 同根因） | **P2 → P2-RC-014**（次要验收标准未满足，主路径[继续入库/手动关闭]OK 边界[超容差差异处理]缺失，§2 P2①） |
| ⑯ | 凭证金额 基于 实际入库数量, 非 订单数量 | PUR-06 | `ErpPurReceiveProcessor.triggerIncomingMove:215-219` → `ReceiveStockMoveBuilder.build(receive, lines, context)` 使用 receive line 实际数量 → InvPostingDispatcher PURCHASE_INPUT 凭证金额基于实际入库 ✅（A2.1/A2.4 证实） | 接受 |
| ⑰ | 订单.关闭() → 单据状态 = 已作废, 释放预留 | PUR-06 | `ErpPurOrderCancelProcessor.cancel:21-60` → `doCancel` 设 docStatus=CANCELLED ✅ + `beforeCancel:41-44` 调 `runCommitmentReleaseHook`（`ErpPurOrderProcessor.java:222-233` 调 `budgetCommitmentBiz.release`，config-gated `erp-fin.budget-commitment-enabled` 默认 false，容错 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）✅ | 接受（关闭释放预留已实现，config-gated 属承付特性整体门控，A1.2/A1.15 已接受） |

### resolved finding HEAD 复核（关键证据，§逻辑非行号）

| finding | arm-index 声称 | HEAD 复核结论 |
|---------|--------------|--------------|
| **P1-MA2-003**（付款核销缺发票三单匹配完成态复核） | ✅ resolved (plan 2026-07-29-2322-1 方案 A) | **已落地（方案A）**。`PaymentSettler.java:60` 注入 `ThreeWayMatcher threeWayMatcher`；`requireInvoiceForSettle:171-173` config-gated 调 `recheckThreeWayMatchAtSettle`；`recheckThreeWayMatchAtSettle:181-191` 强制 strict 复核（`threeWayMatcher.match(code, lines, Boolean.TRUE)`），失败包装 `ERR_SETTLE_INVOICE_MATCH_NOT_COMPLETED` 含 cause 链；`isSettleRecheckEnabled:177-179` 读 `CONFIG_SETTLE_RECHECK_THREE_WAY_MATCH`（默认 false）；`TestErpPurSettleThreeWayMatchRecheck` 含 3 @Test 覆盖（价格超容差阻断 + 数量超入库阻断 + 匹配通过 + cause 链 + paidAmount 保持 0）。**与 UC-PUR-02 ④ 可追溯 + 三单匹配完成态契约一致性已落地**。 |
| **P2-MA2-004**（ThreeWayMatcher dead config read） | watch-only | **维持 watch-only——且 receive-vs-order 维度升级为独立 P1（P1-RC-019）**。`ThreeWayMatcher.java:52-56/122-124` qtyTolerance 计算后被空守护置零，invoice 侧未使用（confirmed）；A2.1 报告曾假定「qty 容差语义仅作用于 receive-vs-order 侧」，但 HEAD 实仓复核 `ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive **无 receive-vs-order 容差校验**——配置两侧均未用。dead config read 本身维持 P2 watch-only（易误导，MR1 清理或文档化）；**L1 UC-PUR-02 ② 超收容差校验缺失升级为独立 P1-RC-019**（不同控制点：dead config read[配置层面] vs 超收容差校验缺失[业务规则层面]）。 |
| **P2-MA2-005**（three-way-match.md 内部不一致） | watch-only | **维持 watch-only**。`three-way-match.md §一致性规则:104`「三单匹配校验在发票审核时执行，失败则拒绝审核」vs `§不匹配的处理策略:99`「非严格模式（默认）：超容差差异提示警告，允许审核通过」——**HEAD 仍不一致**（两段未统一「严格模式生效时」限定语）。代码遵循可配 strict-default-false（warn+放行）。owner doc drift，行为正确，本切片不重开。 |
| **P2-MA2-007**（订单审核价格锁缺失） | watch-only | **维持 watch-only**。`ErpPurOrderProcessor.validateBusinessRulesForApprove:167-170` 仅 requireSupplierActive + runBudgetCheckHook，**无价格锁**（无 `priceLock`/`lockPrice` 字段或方法，无 approve 后置 orderLine.unitPrice 修改守卫）。与 UC-PUR-05 价格差异契约相关（订单审核后若可改价则影响价格匹配基准），L1 §UC-PUR-01 + flow-overview.md §2.1 标注为 watch-only（L2 owner doc 已声明）。本切片不重开。 |

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决，§7）

按 §7 规则，每条 finding 产出前 grep `arm-index.md` 同域同控制点后裁决：

### 复用（同根因同控制点，追加 RC 交叉引用注记，不新建）

| 既有 finding | 本切片对应 | 裁决依据 |
|-------------|----------|---------|
| `P1-MA2-003` | UC-PUR-02 ④ settle 三单匹配完成态复核 | **同根因同控制点**（付款核销三单匹配复核）。方案 A 已落地（HEAD 复核确认）。arm-index 行状态=resolved 维持，追加 RC 视角交叉引用注记。 |
| `P2-MA2-004` | UC-PUR-02 ② receive-vs-order 超收容差校验缺失（P1-RC-019 的姊妹维度） | **部分复用**：P2-MA2-004 = dead config read（配置层面，watch-only）；P1-RC-019 = 超收容差校验缺失（业务规则层面，P1）。**不同控制点不可合并**（配置读取未用 vs 业务规则缺失），但同根因（qtyTolerance 配置两侧均未应用）。P2-MA2-004 维持 watch-only，P1-RC-019 新建 P1，交叉引用。 |
| `P2-MA2-005` | UC-PUR-02 ③ owner doc 内部不一致 | **同根因同控制点**（three-way-match.md §一致性规则 vs §不匹配处理策略）。维持 watch-only，追加 RC 视角交叉引用注记。 |
| `P2-MA2-007` | UC-PUR-05 订单审核价格锁缺失 | **同根因同控制点**（订单审核无价格锁，影响价格匹配基准）。维持 watch-only，追加 RC 视角交叉引用注记。 |

### 新增（新根因/新功能点/新维度）

| 新 finding | UC | 与既有 finding 差异依据 |
|-----------|-----|----------------------|
| `P1-RC-018` | UC-PUR-05 ⑪⑫ 价格差异处理不完整（三策略仅"拒绝" + 让步接收价格差异科目过账行完全缺失） | **新功能点 + 新控制点**：L1 `use-cases.md:141-144` 显式要求三处理策略{拒绝/审批后接收/接收并过账差异} + 让步接收"存在过账行: 科目==价格差异科目 且 金额==差异*数量"。实仓 `ThreeWayMatcher` 仅 strict 拒绝/非 strict warn（1/3 策略）；`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行（1403/2221/2202）**无价格差异科目行**。**grep arm-index 「价格差异」「PPV」「purchase price variance」「让步接收」零命中**同控制点 finding：P1-MA2-003=settle 三单匹配复核，P2-MA2-004=dead config read，P2-MA2-007=订单价格锁——均非"价格差异科目过账行"。**与 A1.1 业财过账引擎审计不同维度**（A1.1=过账正确性范式，本切片=AP_INVOICE 价格差异过账完整性）。**会计类 Q4 无例外**——PPV 过账行缺失属会计过账正确性维度（差异埋在存货价值中未分集），须经 MR1 实现。 |
| `P1-RC-019` | UC-PUR-02 ② 超收容差校验（receive-vs-order）完全缺失 | **新功能点 + 新控制点**：L1 `use-cases.md:67` 显式「入库数量之和 <= 订单数量 * (1 + 超收容差)」。实仓 `ThreeWayMatcher.match:62-107` 只做 invoice-vs-receive 比较，**无 receive-vs-order 容差校验**；`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive。**A2.1 P2P e2e 报告曾假定「qty 容差语义仅作用于 receive-vs-order 侧」（line 47），但 HEAD 实仓复核该侧同样无校验**——配置两侧均未用。与 P2-MA2-004（dead config read，配置层面 watch-only）**同根因不同控制点**（配置读取未用 vs 业务规则缺失），不可合并。**grep arm-index 「超收」「over-receive」「qty tolerance」「receive-vs-order」零命中**业务规则缺失控制点。§2 P1①（功能实质偏离验收标准——超收运行时无门控）。 |
| `P2-RC-013` | UC-PUR-03 ⑤⑥ 订单行.已入库数量（receivedQuantity）列存在但从未写入 | **新控制点**：L1 `use-cases.md:94-95` 显式「订单行.已入库数量 == 60/100 (派生字段)」。ORM `app-erp-purchase.orm.xml:636` `receivedQuantity` 列存在（defaultValue=0）但 `rg setReceivedQuantity` 生产代码零 writer（仅 `_gen` 框架 setter）；`rollupOrderReceiveStatus:244-284` 内部计算 receivedByOrderLine map 但仅更新 header receiveStatus，**不写 orderLine.receivedQuantity** → 列始终 0。**grep arm-index 「receivedQuantity」「已入库数量」「已收货数量」零命中**。主路径（header receiveStatus 正确派生 UNRECEIVED/PARTIAL/RECEIVED）OK，边界（行级 receivedQuantity 字段查询始终得 0）缺失。§2 P2①（次要验收标准未完全满足——派生字段列存在但未写入，header 级进度跟踪可用）。 |
| `P2-RC-014` | UC-PUR-06 ⑮ 短收超容差差异处理未实现 | **新控制点**：L1 `use-cases.md:159` 显式「若 短收数量 > 容差: 触发差异处理(见 §数量差异)」。实仓 `ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 无 receive-vs-order 短收容差校验；无"差异处理"触发机制。与 P1-RC-019（超收容差校验缺失）**同根因**（receive-vs-order 容差逻辑整体缺失）但**不同 UC 不同控制点**（UC-PUR-02 ② 超收 vs UC-PUR-06 ⑮ 短收差异处理）。主路径（短收继续入库或手动关闭 ⑭）OK，边界（超容差差异处理触发）缺失。§2 P2①（次要验收标准未完全满足）。**grep arm-index 「短收」「short receive」「差异处理」零命中**同控制点。 |

### MR1 修复行预留（R1.0 展开器读取本报告后向 MR1 追加 RC-R1.n 实体行）

- **UC-PUR-05 ⑪⑫ 价格差异处理不完整**（P1-RC-018）：`ThreeWayMatcher` 增加"审批后接收"+"接收并过账差异"策略分支（config-gated 策略选择）+ `PurInvoicePostingDispatcher.buildEvent` 传递 invoice-vs-order 差异数据 + `PurAcctDocProvider.createFacts` AP_INVOICE 增加价格差异科目行（科目经 GL 映射解析，金额=差异*数量）。**触及会计过账逻辑（PurAcctDocProvider/VoucherFact/PostingProcessor 核心路径）须 ask-first + 独立 plan-audit**（§5 会计过账逻辑类）。
- **UC-PUR-02 ② 超收容差校验缺失**（P1-RC-019）：`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 增加 receive-vs-order qty 容差校验（按 `erp-pur.match-qty-tolerance` 配置，超容差 strict 拒绝/非 strict warn）；或 `ThreeWayMatcher` 扩展 receive-vs-order 维度。**纯 BizModel/Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**（不触及 ORM/会计过账核心路径）。
- **UC-PUR-03 ⑤⑥ receivedQuantity 未写入**（P2-RC-013）：`ErpPurReceiveProcessor.rollupOrderReceiveStatus` 计算收到 orderLine.receivedQuantity 持久化（已有内部 map，增 `daoFor(ErpPurOrderLine).updateEntity` 写回）。**纯 Processor 代码逻辑修复，不触发 §5 ask-first**。
- **UC-PUR-06 ⑮ 短收差异处理缺失**（P2-RC-014）：`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 增加短收容差判定 + 差异处理触发（与 P1-RC-019 receive-vs-order 容差校验协同实现）。**纯 Processor 代码逻辑修复，不触发 §5 ask-first**。

---

## 7. 静态存疑点清单（供 MA4 / A4.1 展开）

> 以下为本切片 L5 无法静态定论、需运行时确认的点（MA4 / A4.1 展开器读取）：

1. **UC-PUR-03 ⑦ 两次入库独立过账凭证数==2 运行时确认**：HEAD 静态判定 = per-mutation approve 架构隐含成立（每次 `ErpPurReceiveApproveProcessor.approve` 独立 `triggerIncomingMove`→`IErpInvStockMoveBiz.generateMove`→InvPostingDispatcher PURCHASE_INPUT 凭证），但无测试构造"订单(100)→第一次入库(60)→第二次入库(40)→断言凭证数==2"。运行时需构造两次入库序列断言 PURCHASE_INPUT 凭证数==2（A4.1 展开）。
2. **UC-PUR-05 ⑫ 让步接收价格差异过账运行时生成**：HEAD 静态判定 = 完全缺失（`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行无 PPV 行）。运行时需构造"订单单价10+发票单价20（差异100%>5%容差）+非 strict approve 放行"场景，断言生成的凭证行中是否存在"科目==价格差异科目 且 金额==差异*数量"（P1-RC-018 已确认缺失；运行时确认闭合修复优先级）。
3. **UC-PUR-05 ⑪ 三处理策略运行时分支可达性**：HEAD 静态判定 = 仅"拒绝"可达（strict mode）。运行时需确认"审批后接收"+"接收并过账差异"分支是否经其他路径（如审批流 config / xbiz 覆盖）可达（P1-RC-018 已确认未实现；运行时确认无隐藏接线）。
4. **UC-PUR-02 ② 超收容差运行时门控**：HEAD 静态判定 = 完全缺失（receive approve 无 qty-vs-order 校验）。运行时需构造"订单数量10+入库数量20（超收100%>5%容差）"场景，确认 receive approve 是否无门控通过（P1-RC-019 已确认缺失）。
5. **UC-PUR-06 ⑮ 短收超容差运行时差异处理**：HEAD 静态判定 = 完全缺失。运行时需构造"订单100+入库50（短收50>容差）"场景，确认是否无"差异处理"触发（P2-RC-014 已确认缺失）。
6. **UC-PUR-06 ⑰ 关闭释放预留运行时 config-gated 行为**：HEAD 静态判定 = 已实现（config-gated `erp-fin.budget-commitment-enabled` 默认 false）。运行时需启用 config 后构造 cancel 序列断言 commitment release 回写（A1.2/A1.15 已接受 config-gated 语义）。
7. **UC-PUR-03 ⑤⑥ receivedQuantity 运行时值**：HEAD 静态判定 = 列始终 0（零 writer）。运行时确认两次入库后 orderLine.receivedQuantity 查询值得 0（P2-RC-013 已确认）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline（machine-readable 块 `compliance-baseline.md` 锚点）：

  | 规则 | actual | baseline（A1.15 报告记录） | 状态 |
  |------|--------|----------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | = |
  | R1d | 14 | 14 | = |
  | R2a | 34 | 34 | = |
  | R2b | 229 | 229 | = |
  | R2c | 1382 | 1382 | = |
  | R2d | 34 | 34 | = |
  | R3 | 5 | 5 | = |
  | R4/R5/R7/R8/R11 | 0 | 0 | = |
  | R6 | 2 | 2 | = |
  | R10 | 6 | 6 | = |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | = |

  全 16 可计数规则 actual ≤ baseline（全 =），exit 0。**区分门控退出码 vs 纯 reporter 退出码**：checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计，无生产代码变更，checker 无回归风险**（actual = baseline 全等进一步印证零变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（P1-MA2-003/P2-MA2-004/P2-MA2-005/P2-MA2-007 复用 + RC 视角交叉引用；P1-RC-018/P1-RC-019/P2-RC-013/P2-RC-014 新增并附差异依据），无未经比对直接新建的 finding。

---

## Verdict

**FAIL（有需求-实现符合性分歧）**：4 UC 中 **UC-PUR-02 接受 on ①③④ + 1 项 P1 超收容差校验缺失（P1-RC-019）；UC-PUR-03 接受 on ③④ + 1 项 P2 receivedQuantity 未写入（P2-RC-013）；UC-PUR-05 接受 on ①② + 1 项 P1 价格差异处理不完整含让步接收 PPV 过账行缺失（P1-RC-018）；UC-PUR-06 接受 on ①②④⑤ + 1 项 P2 短收差异处理缺失（P2-RC-014）**：

- **UC-PUR-05 ⑪⑫ 价格差异处理不完整（P1-RC-018，最高优先新 finding）**：L1 `use-cases.md:141-144` 显式要求三处理策略{拒绝/审批后接收/接收并过账差异} + 让步接收"存在过账行: 科目==价格差异科目 且 金额==差异*数量"。实仓 `ThreeWayMatcher` 仅实现"拒绝"（strict mode），"审批后接收"+"接收并过账差异"未实现；`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行（1403/2221/2202）**无价格差异科目行**，差异埋在 1403 在途物资金额中（按发票金额入账）未分集到 PPV 科目。**会计类 Q4 无例外**——须经 MR1 实现，触及 PurAcctDocProvider/VoucherFact 核心路径须 ask-first + 独立 plan-audit（§5）。
- **UC-PUR-02 ② 超收容差校验缺失（P1-RC-019）**：L1 `use-cases.md:67` 显式「入库数量之和 <= 订单数量 * (1 + 超收容差)」。实仓 `ThreeWayMatcher.match` 只做 invoice-vs-receive，`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 无 receive-vs-order 容差校验；A2.1 报告曾假定 qty 容差作用于 receive-vs-order 侧，HEAD 复核该侧同样无校验。超收运行时无门控。须经 MR1 实现。
- **UC-PUR-03 ⑤⑥ receivedQuantity 未写入（P2-RC-013）**：ORM `receivedQuantity` 列存在但生产代码零 writer，`rollupOrderReceiveStatus` 仅更新 header receiveStatus 不写 orderLine.receivedQuantity → 列始终 0。主路径（header 进度跟踪）OK，边界（行级字段查询）缺失。登记不强制。
- **UC-PUR-06 ⑮ 短收差异处理缺失（P2-RC-014）**：L1「短收数量>容差→触发差异处理」完全缺失，与 P1-RC-019 同根因（receive-vs-order 容差逻辑缺失）不同 UC 不同控制点。主路径（继续入库/手动关闭）OK，边界（超容差差异处理）缺失。登记不强制。

**零 P0**：UC-PUR-02 ①③④ 三单匹配核心（回链三元组 + 价格容差 + 可追溯 + settle 复核）行为正确（A2.1+A2.8+R1.8 三重证实）；UC-PUR-03 ③④ 两次入库独立过账（架构隐含）+ 不自动关闭行为正确；UC-PUR-05 ①② 差异计算 + 价格容差触发行为正确；UC-PUR-06 ①②④⑤ 短收计算 + 继续入库 + 实际入库过账 + 关闭释放预留行为正确。PPV 过账行缺失虽属会计过账维度但 GL 仍平衡（debit 在途物资+进项税 = credit 应付）+ AP 金额正确（按发票应付），属管理会计可视性缺口（差异未分集到 PPV）非活跃数据破坏，定 P1① 非 P0④。

**resolved finding HEAD 复核**：P1-MA2-003（方案A 落地确认）已落地；P2-MA2-004（dead config read 维持 watch-only，receive-vs-order 维度升级独立 P1-RC-019）+ P2-MA2-005（owner doc 内部不一致维持 watch-only）+ P2-MA2-007（订单价格锁缺失维持 watch-only）均维持 watch-only 无升级 P0。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 登记）。finding 修复按 §10 经 MR1（R1.0 展开为 RC-R1.n），触及会计过账逻辑（P1-RC-018 PurAcctDocProvider/VoucherFact）+ ORM 结构变更的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。

---

## 参考

- 真相源：`docs/design/purchase/use-cases.md:55/:81/:130/:151`（UC-PUR-02/03/05/06）
- 设计参考：`docs/design/purchase/three-way-match.md` + `state-machine.md` + `README.md`
- 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
- L5 既有证据：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8）+ `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（A2.1）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（A1.1）+ `docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（A1.15）
- resolved 裁决计划：`docs/plans/2026-07-29-2322-1-r1-8-pur-settle-three-way-match-recheck.md`（plan 2026-07-29-2322-1 方案A settle 三单匹配复核）
