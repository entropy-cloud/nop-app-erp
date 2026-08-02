# rc-ma1-a1-17-purchase-f3-returns-business-finance purchase-F3 退货与业财需求符合性审计

> Plan: `docs/plans/2026-08-03-0100-3-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`（active）
> Mission: requirement-compliance（MA1 切片 A1.17）
> Work Item: A1.17（MA1 需求追踪矩阵审计 — purchase-F3 退货与业财：UC-PUR-04 采购退货 + UC-PUR-07 业财一体过账(入库与发票)）
> 来源: `docs/backlog/requirement-compliance-roadmap.md` A1.17
> Audit Status: closed

## 0. 与既有 MA2/A1.x 报告差异增量声明（§9）

本报告**只补需求视角差异**（methodology §去重协议）：

- **A2.8 purchase 状态机审查**（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）已证实：9 实体三轴状态机迁移守卫齐全 + reverseApprove 红冲闭环强一致（doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse 经 IErpFinVoucherBiz Facade）+ 跨域写经 I*Biz Facade + **P1-MA2-051（rollbackReceive 不对称）+ P2-MA2-006（returns.md red invoice drift）** finding 已登记。本切片**不重审**状态机迁移/红冲闭环/跨域 Facade，只补 UC-PUR-04/07 验收标准视角的差异。
- **A2.1 P2P e2e** + **A1.1 业财过账引擎**（`docs/audits/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`）已证实：P2P 链路行为 + 业财过账引擎 GR/IR + AP 凭证范式 + isReversed 红冲标记 + 业财回链 VoucherBillR 范式。本切片**引用其过账正确性结论**，只补采购侧触发契约 + 退货红冲闭环的需求视角差异。
- **A1.15**（`docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`）已审 UC-PUR-01/08：①已确认 GOODS_RECEIPT→PURCHASE_INPUT / PURCHASE_INVOICE→AP_INVOICE 命名漂移登记 **P2-RC-011**（合并登记，本切片 UC-PUR-07 ①② 复用）；②承付恢复 reuse **P1-MA2-083** 重开（Q4=(a) 下方案B Deferred 不成立，本切片 UC-PUR-04 退货侧复核）；③多供应商拆分 P1-RC-017（不归本切片）。
- **A1.16**（`docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`）已审 UC-PUR-02/03/05/06：UC-PUR-05 ⑪⑫ 价格差异处理不完整（P1-RC-018，AP_INVOICE 无 PPV 过账行）+ UC-PUR-02 ② 超收容差校验缺失（P1-RC-019）。本切片 UC-PUR-07 ② AP_INVOICE 凭证行完整性核验**不重复 P1-RC-018 PPV 维度**（PPV = 发票 vs 订单价差异过账；本切片 UC-PUR-07 ② = 借 GR/IR + 进项税 / 贷 AP 三行基础结构，与 P1-RC-018 维度互补不重叠）。
- **A1.6 finance-F6 期间结账**（`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md`）已审 UC-FIN-06/07 期间结账机制。本切片 UC-PUR-07 ⑤ 期间控制核验**仅核验采购侧过账拒绝行为**（finance 引擎 ERR_PERIOD_CLOSED 触发链路），不重审期间结账机制本身（归 A1.6）。
- **A2.1 finance GRNI 冲回**（P1-MA2-001，归 A2.1 finance 会计保护区域）—— returns.md「正向 receive→invoice 暂估冲回」documented simplification 同根因。本切片**交叉引用不重审**。

本切片**只补**的需求视角差异：(i) UC-PUR-04 五条验收标准逐条（①来源回链 + ②库存可用量回减 + ③红冲过账凭证 + ④原入库关联凭证 isReversed + ⑤已开票退货贷项/红冲）+ (ii) UC-PUR-07 五条验收标准逐条（①入库过账 GOODS_RECEIPT 借存货贷GR/IR + ②发票过账 借GR/IR+进项税 贷AP + ③多币种本位币==源币*汇率 + ④反审核删凭证已过账=false + ⑤期间CLOSED拒绝过账/反审核）+ (iii) **resolved finding HEAD 复核**（P1-MA2-051 receive 悬挂 + P2-MA2-006 credit-memo-via-return + P1-MA2-083 承付恢复 退货侧 + P1-MA2-002 多币种）。

---

## 1. 需求契约原文（L1，逐字引用）

### UC-PUR-04 采购退货（`docs/design/purchase/use-cases.md:104-126`）

```
场景:已入库的货物退回供应商。见 returns.md。

行为链路:
创建退货单(关联原入库单) → 审核通过

可验证断言:
退货单.来源单号 == 入库单.单号
库存余额[物料, 仓库].可用量 -= 退货明细数量之和

// 红冲过账(反方向)
存在凭证: 业务类型 == 入库红冲 且 来源单号 == 退货单.单号
原入库单关联凭证被标记红冲

// 若已开票
若入库单已有发票: 该发票需红冲或生成贷项(见 returns.md)
```

### UC-PUR-07 业财一体过账(入库与发票)（`docs/design/purchase/use-cases.md:172-198`）

```
场景:验证业务单据审核自动触发财务过账。见 ../finance/posting.md。

可验证断言:
// 入库过账(审核时)
入库单.审核通过 →
  生成凭证(业务类型=GOODS_RECEIPT, 来源=入库单)
  凭证行: 借 存货科目, 贷 暂估应付(GR/IR)
  写入业财回链(来源类型=采购入库)
  入库单.已过账 = true

// 发票过账
发票.审核通过 →
  生成凭证: 借 暂估应付(GR/IR) + 进项税, 贷 应付账款
  发票.已过账 = true

// 多币种
凭证行.本位币金额 == 源币金额 * 汇率

// 反审核
入库单.反审核 → 删除关联凭证(经业财回链反查), 入库单.已过账 = false

// 期间控制
若 期间.总账状态 == 已结账: 入库单/发票 不可过账/不可反审核
```

---

## 2. 实现证据（L3，`file:line`）

### 采购退货（UC-PUR-04）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurReturnBizModel.java:21-39` — ErpPurReturn BizModel Facade（标准审批动作委托 per-mutation Processor；非审批动作 cancel 委托 ErpPurReturnCancelProcessor）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java:50-427` — 退货状态机编排 Processor（迁移守卫 `validateTransitionFor*:122-167` + 业务规则 `requireSourceReceiveApproved:345-356` 关联原入库 + `triggerOutgoingMove:228-233` 库存 outgoing Facade + `resolveSourceReceiveMoveId:235-243` 来源回链 + `ensureReversed:245-265` 红冲闭环 + `runCommitmentReleaseOnReturnHook:281-297` 退货承付释放）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnApproveProcessor.java:22-62` — ErpPurReturn approve per-mutation Processor（触发 outgoing move + flush + tryPost + runCommitmentReleaseOnReturnHook；approve 主路径完整复制 facade 流）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnReverseApproveProcessor.java` — 退货反审核 per-mutation Processor（红字冲销凭证 + ensureReversed 库存物理冲销）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurReturnLineBizModel.java` — 退货明细行 BizModel。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurReturnPostingDispatcher.java:31-103` — PURCHASE_RETURN 过账派发器（`buildEvent:77-94` 组装 PostingEvent[PURCHASE_RETURN, billHeadCode=return.code, TOTAL_AMOUNT, SUPPLIER_ID] + `tryPost:44-58` 失败吞异常保持 APPROVED+posted=false + `reverse:64-75` 红字冲销硬前置失败向上抛出）。
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinArApItemGenerator.java:144-187` — `resolveProfile` PURCHASE_RETURN 分支（`:157-160` DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount credit memo；使 PartnerBalanceUpdater.sumOpen 自然减计 payableBalance）。
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java:252 + :933-947` — `markOriginalVoucherReversed` 公共流程：reverse 调用时标记原正常凭证 isReversed=true（仅在 finance 侧 reverse() 路径触发；正向 tryPost 路径不触发——见 §5 候选缺口分析）。

### 业财过账（UC-PUR-07）

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveApproveProcessor.java:26-49` — ErpPurReceive approve per-mutation Processor（触发 `triggerIncomingMove` + `applyPostingResult`）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java:215-242` — receive 库存 incoming Facade + `applyPostingResult:221-227` posted 标志回写（receive.posted = move.posted；GOODS_RECEIPT 实际过账由 InvPostingDispatcher 经 `move.getPosted()` 透传）。
- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvPostingDispatcher.java:51-216` — 入库→`ErpFinBusinessType.PURCHASE_INPUT`（`:169`）经 InvAcctDocProvider 借 1401 库存商品 / 贷 2202 暂估应付（GR/IR）；本切片 GOODS_RECEIPT 触发路径归 A1.15 主核验，本切片仅核验凭证行 借存货贷GR/IR 完整性（会计正确性视角）。
- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvAcctDocProvider.java:22-30 + :59-70` — PURCHASE_INPUT 凭证行（借 1401 库存商品 / 贷 2202 应付账款-暂估 GR/IR），覆盖 UC-PUR-07 ① 凭证行结构。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurInvoicePostingDispatcher.java:26-102` — AP_INVOICE 过账派发器（`buildEvent:71-89` businessType=AP_INVOICE + billData 含 TOTAL_AMOUNT/TOTAL_TAX_AMOUNT/TOTAL_AMOUNT_WITH_TAX/SUPPLIER_ID + exchangeRate 默认 ONE 兜底 + voucherDate 解析）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java` — AP_INVOICE createFacts 三行（1403 在途物资 DEBIT TOTAL_AMOUNT + 2221 进项税 DEBIT TOTAL_TAX_AMOUNT + 2202 应付账款 CREDIT TOTAL_AMOUNT_WITH_TAX），覆盖 UC-PUR-07 ② 凭证行结构。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurReversalListener.java:40-142` — 采购域凭证红冲监听者（finance→purchase 反向回写）：`rollbackInvoice:70-82` + `rollbackPayment:84-96` + `rollbackReturn:98-110` + `rollbackReceive:112-126` 四实体全部 posted=false + APPROVED→REJECTED 对称回退（**HEAD 复核：rollbackReceive 已与其他三实体对称**，P1-MA2-051 已 resolved——详 §4 + §6）。
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java:505-528` — `resolveOpenPeriod` 期间状态守卫（period.status != OPEN 抛 ERR_PERIOD_CLOSED——覆盖 UC-PUR-07 ⑤ 期间 CLOSED 拒绝过账/反审核；finance 引擎对所有 businessType 全局生效）。

---

## 3. 测试证据（L4，注明断言强度）

### UC-PUR-04 采购退货

- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnTrace.java` — **强断言**（来源回链原入库单：退货单.来源单号 == 入库单.单号）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnInventory.java` — **强断言**（库存 outgoing：退货审核触发反向出库 + 库存余额回减 `:71` `testApproveGeneratesOutgoingMoveAndStockDecrease`；覆盖 UC-PUR-04 ②）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnPosting.java:82-148` — **强断言**（PURCHASE_RETURN 过账端到端：① `:96-104` 业财回链 + 凭证 2 行（借 2202 暂估 20 / 贷 1401 存货 20）覆盖 UC-PUR-04 ③；② `:107-116` DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount credit memo 覆盖 UC-PUR-04 ⑤；③ `:119` 库存余额回减（10−4=6）覆盖 UC-PUR-04 ②；④ `:122-148` reverseApprove 红冲 + 辅助账 CANCELLED + openAmount 归零 + 余额恢复覆盖 UC-PUR-04 反审核行为）。**未断言原入库 PURCHASE_INPUT 凭证 isReversed=true**（UC-PUR-04 ④ 字面验收标准零覆盖——见 §5 候选缺口）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnRefundEndToEnd.java:120-202` — **强断言**（已开票退货到退款连续链：① `:133` 发票审核 → AP_INVOICE 过账；② `:147` 退款核销 SETTLED；③ `:162-176` 退货审核 → PURCHASE_RETURN 红字凭证；④ `:178-184` 退货负 credit memo（DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount）；⑤ `:188-189` sumOpen = -20 应付余额回减；⑥ `:192-201` 反审核 → 辅助账 CANCELLED + 余额恢复。覆盖 UC-PUR-04 ⑤ + UC-PUR-07 反审核闭环）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnApproval.java` + `TestErpPurReturnQty.java` + `TestErpPurReturnCommitmentRelease.java` — **强断言**（审批主路径 + 数量校验 + 退货承付释放对称性）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurFinanceReversalWriteback.java` — **强断言**（finance→purchase 反向回写：voucherBiz.reverse(invoiceCode, AP_INVOICE) → invoice.posted=false + approveStatus REJECTED；ghost bill code 容错跳过）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/posting/TestPurReversalListenerReceiveRollback.java` — **强断言**（P1-MA2-051 receive 悬挂测试——HEAD 复核：rollbackReceive 已对称回退 APPROVED→REJECTED）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/posting/TestErpPurPostingDispatcherFailureHangs.java` — **强断言**（过账失败悬挂：mock post 抛异常 → posted=false 保持）。
- E2E `tests/e2e/orchestration/p2p-reverse.spec.ts`（P2P 红冲链）+ `tests/e2e/business-actions/pur-return.action.spec.ts`（退货动作）—— A5.6 已评级 E2E 强度。

### UC-PUR-07 业财过账

- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveStockMove.java:72 + :112` — **强断言**（receive 审核触发 incoming move + posted=true + 贷方合计=暂估应付 50；覆盖 UC-PUR-07 ① 入库过账 GOODS_RECEIPT→PURCHASE_INPUT 借存货贷GR/IR + 已过账=true）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoicePosting.java:70-100` — **强断言**（AP_INVOICE 凭证 3 行：借费用/采购 100 + 借进项税 13 + 贷应付 113；覆盖 UC-PUR-07 ② 发票过账 借GR/IR+进项税 贷AP + 已过账=true）。`testReverseApproveGeneratesRedVoucher` 覆盖反审核红冲。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurMultiCurrencyPosting.java:70-132` — **强断言**（外币 AP_INVOICE + PAYMENT，exchangeRate=7.0≠ONE：① `:79-80` 头合计按本位币 791；② `:93` 在途物资 debit=functional 700；③ `:102` 进项税 debit=91；④ `:107` 应付 credit=791；⑤ `:129-132` PAYMENT 借应付/贷银行本位币 791 + 源币 113。**完整覆盖 UC-PUR-07 ③ 多币种 凭证行.本位币金额==源币金额*汇率**）。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurToInvToFinPostingEnd.java` — **强断言**（purchase→inventory→finance 三域过账链：AP_INVOICE 凭证 + INV 估值凭证 借存货/贷暂估 + 双凭证业财回链一致性；覆盖 UC-PUR-07 ①②）。
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/...TestErpFinPostingPeriod*` — finance 引擎期间 CLOSED 拒绝过账测试（覆盖 UC-PUR-07 ⑤；finance 域测试，归 A1.1/A1.6 主核验）。

---

## 4. 运行时行为证据（L5，复用既有审计）

- **复用 A2.8**（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）：9 实体状态机迁移守卫齐全 + reverseApprove 红冲闭环强一致 + 跨域写经 I*Biz Facade + `P1-MA2-051 rollbackReceive 不对称` finding（**HEAD 已 resolved**）+ `P2-MA2-006 returns.md red invoice drift` finding（**HEAD 已 resolved**）。
- **复用 A2.1 P2P e2e**（`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`）：P2P 黄金路径 + 反向冲销链路行为已证实，本切片只补需求视角差异。
- **复用 A1.1 业财过账引擎**（`docs/audits/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`）：GOODS_RECEIPT/PURCHASE_INPUT + AP_INVOICE + 红冲凭证链路范式已审（Provider 路由 + VoucherBillR 业财回链 + GR/IR 暂估应付 + isReversed 红冲标记机制）。
- **复用 A1.15**（`docs/audits/2026-08-03-0145-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`）：UC-PUR-01 ④⑤ businessType 命名漂移（GOODS_RECEIPT→PURCHASE_INPUT / PURCHASE_INVOICE→AP_INVOICE）已登记 **P2-RC-011**（合并登记）。
- **复用 A1.16**（`docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`）：UC-PUR-05 ⑪⑫ 价格差异处理不完整 P1-RC-018（AP_INVOICE 无 PPV 过账行）+ UC-PUR-02 ② 超收容差校验缺失 P1-RC-019。本切片 UC-PUR-07 ② AP_INVOICE 凭证行核验与 P1-RC-018 维度互补不重叠。
- **本切片补的差异**（运行时行为经既有 JUnit 测试 + 代码站点确认）：
  - **UC-PUR-04 ①** 退货单.来源单号==入库单.单号 — `ErpPurReturn.receiveId` 关联 + `requireSourceReceiveApproved:345-356` 校验，运行时回链完整（TestErpPurReturnTrace 强断言）。
  - **UC-PUR-04 ②** 库存可用量-=退货数量 — `triggerOutgoingMove:228-233` → IErpInvStockMoveBiz.generateMove OUTGOING，运行时库存回减（TestErpPurReturnInventory + TestErpPurReturnPosting:119 强断言库存 10−4=6）。
  - **UC-PUR-04 ③** 红冲过账凭证 businessType==入库红冲 — 实仓 businessType=PURCHASE_RETURN（语义等价"入库红冲"），凭证行 借暂估应付 20 / 贷存货 20 反向 PURCHASE_INPUT（TestErpPurReturnPosting:96-104 强断言）。
  - **UC-PUR-04 ④** 原入库单关联凭证被标记红冲 (isReversed) — **HEAD 静态分析**：`PurReturnPostingDispatcher.tryPost` 调 `executor.postEvent`（正向过账），**不调 `executor.reverse`**；`markOriginalVoucherReversed:252+933-947` 仅在 finance 侧 `reverse()` 路径触发，正向 tryPost 路径不触发。故原入库 PURCHASE_INPUT 凭证保留 isReversed=false，**仅以独立 PURCHASE_RETURN 反向凭证实现 GL 净零**（功能等价但未字面满足 ④ isReversed 标记）。**与 P2-MA2-006 credit-memo-via-return resolved plan 2026-07-29-2322-1 同根因**（credit-memo-via-return 实现 = 独立反向凭证 + 负 ArApItem credit memo，不标记原凭证 isReversed）。
  - **UC-PUR-04 ⑤** 已开票退货贷项/红冲 — credit-memo-via-return 运行时已落地（`ErpFinArApItemGenerator.resolveProfile:157-160` DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount；TestErpPurReturnRefundEndToEnd:178-184 强断言；AP 余额回减经 sumOpen 自然减计）。**formal AP 侧红字发票未单独生成**（与 P2-MA2-001 GRNI 冲回 documented simplification 同根因，归 A2.1 finance 会计保护区——交叉引用不重审）。
  - **UC-PUR-07 ①** 入库过账 GOODS_RECEIPT 借存货贷GR/IR + 业财回链 + 已过账=true — `ErpPurReceiveProcessor.applyPostingResult:221-227` receive.posted=move.posted；InvPostingDispatcher:169 businessType=PURCHASE_INPUT；InvAcctDocProvider:22-30 借 1401 存货 / 贷 2202 暂估应付；VoucherBillR 业财回链；receive.posted=true 落地（TestErpPurReceiveStockMove + TestErpPurToInvToFinPostingEnd 强断言）。**GOODS_RECEIPT 触发路径归 A1.15 主核验，本切片仅核验凭证行完整性 = 通过**。
  - **UC-PUR-07 ②** 发票过账 借GR/IR+进项税 贷AP + 已过账=true — `PurInvoicePostingDispatcher.buildEvent:71-89` AP_INVOICE；`PurAcctDocProvider` 三行 1403/2221/2202；invoice.posted=true 落地（TestErpPurInvoicePosting + TestErpPurToInvToFinPostingEnd 强断言）。**注**：发票侧借方为 1403 在途物资（P2-RC-011 命名漂移，与 L1 字面"GR/IR"语义等价；与 A1.16 P1-RC-018 PPV 行缺失互补）。
  - **UC-PUR-07 ③** 多币种本位币==源币*汇率 — `PurInvoicePostingDispatcher.buildEvent:78` exchangeRate 兜底 ONE；`PurReturnPostingDispatcher.buildEvent:84` 同型；TestErpPurMultiCurrencyPosting:70-132 强断言 source×rate==functional（100×7=700 / 13×7=91 / 113×7=791）。
  - **UC-PUR-07 ④** 反审核删凭证+已过账=false — `ErpPurReturnProcessor.ensureReversed:245-265` 调 `postingDispatcher.reverse()` 经 IErpFinVoucherBiz Facade 红冲 + posted=false + 辅助账 cancelOnReverse；`PurReversalListener.rollbackInvoice/Payment/Return/Receive:70-126` 四实体全部 posted=false + APPROVED→REJECTED（TestErpPurReturnPosting:122-148 + TestErpPurFinanceReversalWriteback 强断言）。**注**：L1 字面"删除关联凭证"，实仓为"红字冲销凭证（保留 + 反向凭证）"语义等价（A1.1 业财过账引擎范式）。
  - **UC-PUR-07 ⑤** 期间CLOSED拒绝过账/反审核 — `ErpFinPostingProcessor.resolveOpenPeriod:524-527` period.status != OPEN 抛 ERR_PERIOD_CLOSED（finance 引擎对所有 businessType 全局生效，含 PURCHASE_INPUT/AP_INVOICE/PURCHASE_RETURN）。**采购侧无独立测试覆盖**，但 finance 引擎测试覆盖（A1.1/A1.6）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论）

### 5.1 五级追踪矩阵

| UC | L1 需求契约 | L2 owner doc | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|-------------|--------------|-------------|-------------|---------------|-----------|
| **UC-PUR-04** | `use-cases.md:104` 采购退货（①退货单.来源单号==入库单.单号 + ②库存可用量-=退货数量 + ③存在凭证 业务类型==入库红冲 来源==退货单.单号 + ④原入库单关联凭证被标记红冲 isReversed + ⑤若已开票 发票需红冲或生成贷项） | `returns.md §退货流程 + §红字发票处理（P2-MA2-006 resolved credit-memo-via-return）`（设计参考；**L1 冲突以 L1 为准**）+ `state-machine.md §退货单状态机` + `finance/posting.md §红冲` | `ErpPurReturnBizModel.java:21-39` + `ErpPurReturnProcessor.java:50-427`（关联原入库 `:345-356` + outgoing Facade `:228-233` + 来源回链 `:235-243` + ensureReversed `:245-265` + 承付释放 `:281-297`）+ `ErpPurReturnApproveProcessor.java:22-62` + `posting/PurReturnPostingDispatcher.java:31-103`（PURCHASE_RETURN + reverse + buildEvent）+ `ErpFinArApItemGenerator.java:157-160`（PURCHASE_RETURN credit memo） | `TestErpPurReturnTrace`（强）+ `TestErpPurReturnInventory:71`（强）+ `TestErpPurReturnPosting:82-148`（强，业财回链 + 凭证 2 行 + credit memo + 库存回减 + reverse）+ `TestErpPurReturnRefundEndToEnd:120-202`（强，已开票退货连续链）+ `TestErpPurReturnApproval/Qty/CommitmentRelease`（强）+ `TestErpPurFinanceReversalWriteback`（强）+ E2E p2p-reverse + pur-return.action | 行为已证实（A2.8 状态机 + 红冲闭环 + A2.1 P2P + A1.1 业财过账引擎）；③④ PURCHASE_RETURN 反向凭证语义等价"入库红冲"，**未字面标记原入库凭证 isReversed**（与 P2-MA2-006 credit-memo-via-return resolved plan 2026-07-29-2322-1 同根因）；⑤ credit-memo-via-return 运行时已落地 | **接受 on ①②③⑤；P2 on ④ isReversed 标记缺失（P2-RC-015 新，与 P2-MA2-006 同根因已 resolved，登记 successor）** |
| **UC-PUR-07** | `use-cases.md:172` 业财一体过账（①入库过账 GOODS_RECEIPT 借存货贷GR/IR + 业财回链 + 已过账=true + ②发票过账 借GR/IR+进项税 贷AP + 已过账=true + ③多币种本位币==源币*汇率 + ④反审核删凭证已过账=false + ⑤期间CLOSED拒绝过账/反审核） | `finance/posting.md §业财过账机制 + §GR/IR + §红冲` + `finance/period-close.md §期间控制`（设计参考） | `ErpPurReceiveApproveProcessor.java:26-49` + `ErpPurReceiveProcessor.java:215-242`（triggerIncomingMove + applyPostingResult）+ `InvPostingDispatcher.java:169` + `InvAcctDocProvider.java:22-30`（PURCHASE_INPUT 借存货贷GR/IR）+ `PurInvoicePostingDispatcher.java:26-102`（AP_INVOICE + exchangeRate 兜底）+ `PurAcctDocProvider`（1403/2221/2202 三行）+ `PurReversalListener.java:40-142`（四实体反向回写 posted=false+APPROVED→REJECTED）+ `ErpFinPostingProcessor.resolveOpenPeriod:505-528`（期间 CLOSED 守卫） | `TestErpPurReceiveStockMove:72,112`（强，库存 incoming + 暂估应付）+ `TestErpPurInvoicePosting:70-100`（强，AP_INVOICE 3 行 + reverse）+ `TestErpPurMultiCurrencyPosting:70-132`（强，本位币 source×rate）+ `TestErpPurToInvToFinPostingEnd`（强，跨域链）+ `posting/TestPurReversalListenerReceiveRollback`（强，receive 对称回退）+ `posting/TestErpPurPostingDispatcherFailureHangs`（强，悬挂） | 行为已证实（A1.1 业财过账引擎 + A2.1 P2P + A2.8 状态机 + A1.6 期间）；GOODS_RECEIPT→PURCHASE_INPUT / PURCHASE_INVOICE→AP_INVOICE 命名漂移行为等价（A1.15 P2-RC-011）；④ "删凭证"实仓为"红字冲销凭证"语义等价（A1.1 范式） | **接受 on ①②③④⑤**（① GOODS_RECEIPT 触发路径归 A1.15，本切片核验凭证行完整性=通过；② AP_INVOICE 三行结构通过，PPV 缺失归 A1.16 P1-RC-018；④ 反向回退对称通过；⑤ 期间 CLOSED 由 finance 引擎全局生效） |

### 5.2 验收标准逐条结论

| # | 验收标准（L1 字面） | 所属 UC | L3 实仓实现 | 结论 |
|---|---------------------|---------|-------------|------|
| ① | 退货单.来源单号 == 入库单.单号 | PUR-04 | `ErpPurReturn.receiveId` 关联 + `ErpPurReturnProcessor.requireSourceReceiveApproved:345-356`（校验原入库 APPROVED）+ `resolveSourceReceiveMoveId:235-243`（回链原入库 stockMove）。**回链完整** | 接受 |
| ② | 库存余额[物料, 仓库].可用量 -= 退货明细数量之和 | PUR-04 | `ErpPurReturnProcessor.triggerOutgoingMove:228-233` → `IErpInvStockMoveBiz.generateMove` OUTGOING Facade → 库存回减。TestErpPurReturnPosting:119 强断言（10−4=6） | 接受 |
| ③ | 存在凭证: 业务类型 == 入库红冲 且 来源单号 == 退货单.单号 | PUR-04 | `PurReturnPostingDispatcher.buildEvent:79` businessType=**PURCHASE_RETURN**（语义等价"入库红冲"——反向 PURCHASE_INPUT 借暂估/贷存货）；凭证 2 行 借 2202 暂估应付 20 / 贷 1401 存货 20；业财回链 billHeadCode=return.code。TestErpPurReturnPosting:96-104 强断言 | 接受（命名漂移语义等价，与 P2-RC-011 同型不单独登记） |
| ④ | 原入库单关联凭证被标记红冲（isReversed） | PUR-04 | **HEAD 静态分析**：`PurReturnPostingDispatcher.tryPost:44-58` 调 `executor.postEvent`（正向过账），**未调 `executor.reverse`**。`ErpFinPostingProcessor.markOriginalVoucherReversed:252+933-947` 仅在 reverse() 路径触发，tryPost 路径不触发——**原入库 PURCHASE_INPUT 凭证保留 isReversed=false**。仅以独立 PURCHASE_RETURN 反向凭证实现 GL 净零。L4 无测试断言"原入库凭证 isReversed=true" | **P2 → P2-RC-015**（架构性偏离 L1 字面 isReversed 标记，GL 净零功能等价；documented simplification 满足 §4 (i) 独立 plan-audit 通过记录 = plan 2026-07-29-2322-1 resolved P2-MA2-006；§2 P2① 次要验收标准未完全满足——主路径[GL 净零]OK 边界[isReversed 标记]弱） |
| ⑤ | 若入库单已有发票: 该发票需红冲或生成贷项 | PUR-04 | **credit-memo-via-return 运行时已落地**：`ErpFinArApItemGenerator.resolveProfile:157-160` PURCHASE_RETURN → DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount（AP 贷项 credit memo 语义）；`PartnerBalanceUpdater.sumOpen` 自然减计 payableBalance。TestErpPurReturnRefundEndToEnd:178-189 强断言（openAmountFunctional = 负 totalAmount + sumOpen=-20）。**formal AP 侧红字发票未单独生成**（与 P2-MA2-001 GRNI 冲回 documented simplification 同根因，归 A2.1 finance 会计保护区——交叉引用不重审） | 接受（credit memo 已落地辅助账层；formal AP 侧归 A2.1） |
| ① | 入库过账 GOODS_RECEIPT 凭证行 借存货 贷暂估应付(GR/IR) + 业财回链 + 入库单.已过账=true | PUR-07 | `ErpPurReceiveProcessor.applyPostingResult:221-227` receive.posted=move.posted；`InvPostingDispatcher.java:169` businessType=PURCHASE_INPUT；`InvAcctDocProvider:22-30` 借 1401 库存商品 / 贷 2202 应付账款-暂估（GR/IR）；VoucherBillR 业财回链；receive.posted=true 落地。**GOODS_RECEIPT 触发路径归 A1.15 主核验**，本切片仅核验凭证行完整性 = 通过。TestErpPurReceiveStockMove:72,112 + TestErpPurToInvToFinPostingEnd 强断言 | 接受（GOODS_RECEIPT→PURCHASE_INPUT 命名漂移行为等价，A1.15 P2-RC-011 已登记；触发路径归 A1.15） |
| ② | 发票过账 借暂估应付(GR/IR)+进项税 贷应付账款 + 发票.已过账=true | PUR-07 | `PurInvoicePostingDispatcher.buildEvent:73` businessType=AP_INVOICE；`PurAcctDocProvider` createFacts 三行：1403 在途物资 DEBIT invoice.totalAmount + 2221 进项税 DEBIT invoice.totalTaxAmount + 2202 应付账款 CREDIT invoice.totalAmountWithTax；invoice.posted=true 落地。TestErpPurInvoicePosting:70-100 强断言（"AP_INVOICE 凭证 3 行 借采购/借进项税/贷应付"）+ TestErpPurToInvToFinPostingEnd | 接受（借方 1403 在途物资 vs L1 字面"GR/IR"语义等价；与 A1.16 P1-RC-018 PPV 行缺失互补不重叠） |
| ③ | 多币种 凭证行.本位币金额 == 源币金额 * 汇率 | PUR-07 | `PurInvoicePostingDispatcher.buildEvent:78` exchangeRate 兜底 ONE；`PurReturnPostingDispatcher.buildEvent:84` 同型；`ErpFinPostingProcessor.prepareContext:537` exchangeRate 兜底 + VoucherFact 行级 amountSource/amountFunctional 分离。TestErpPurMultiCurrencyPosting:70-132 强断言（source×rate==functional：100×7=700 / 13×7=91 / 113×7=791；行级 amountSource≠amountFunctional） | 接受 |
| ④ | 反审核 入库单.反审核→删除关联凭证(业财回链反查) + 已过账=false | PUR-07 | `ErpPurReturnProcessor.ensureReversed:245-265` 调 `postingDispatcher.reverse()` 经 `IErpFinVoucherBiz.reverse` Facade 红冲（保留原凭证 + 生成反向红字凭证，**语义等价"删除"**——A1.1 业财过账引擎范式）+ posted=false + 辅助账 cancelOnReverse；`PurReversalListener.rollbackInvoice/Payment/Return/Receive:70-126` 四实体全部 posted=false + APPROVED→REJECTED 对称回退（**HEAD 复核：rollbackReceive 已与其他三实体对称——P1-MA2-051 resolved**）。TestErpPurReturnPosting:122-148 + TestErpPurFinanceReversalWriteback 强断言 | 接受（"删凭证"实仓为"红字冲销凭证"语义等价；P1-MA2-051 receive 悬挂 HEAD 复核 = 已 resolved） |
| ⑤ | 期间控制 期间.总账状态==已结账→不可过账/不可反审核 | PUR-07 | `ErpFinPostingProcessor.resolveOpenPeriod:524-527` period.status != OPEN 抛 `ERR_PERIOD_CLOSED`（finance 引擎对所有 businessType 全局生效，含 PURCHASE_INPUT/AP_INVOICE/PURCHASE_RETURN）。**采购侧无独立测试覆盖**，但 finance 引擎测试覆盖（A1.1/A1.6 主核验） | 接受（finance 引擎期间控制已生效；与 A1.6 finance-F6 期间结账交叉，本切片核验采购侧过账拒绝行为通过） |

### 5.3 resolved finding HEAD 复核（关键证据）

| Finding | arm-index 状态 | HEAD 复核结论 |
|---------|---------------|---------------|
| **P1-MA2-051** PurReversalListener.rollbackReceive receive 悬挂（APPROVED+posted=false 不对称） | ✅ resolved (R1.17 done, roadmap 2026-07-31) | **已落地（resolved 确认）**。HEAD `PurReversalListener.java:112-126` 现与其他三实体（Invoice:70-82 / Payment:84-96 / Return:98-110）完全对称——`rollbackReceive` 设 posted=false + postedAt/postedBy=null + **`if (Objects.equals(receive.getApproveStatus(), APPROVE_STATUS_APPROVED)) receive.setApproveStatus(APPROVE_STATUS_REJECTED)`**（:122-124）。L4 `TestPurReversalListenerReceiveRollback` 覆盖。**receive 悬挂缺口已闭合，方案A 落地（与其他三实体对齐降级 REJECTED）**。 |
| **P2-MA2-006** returns.md red invoice drift（credit-memo-via-return 实现） | ✅ resolved (plan 2026-07-29-2322-1) | **已落地（resolved 确认）**。HEAD `ErpFinArApItemGenerator.resolveProfile:157-160` PURCHASE_RETURN → DIRECTION_PAYABLE + SOURCE_BILL_PUR_RETURN + 负 openAmount credit memo。L4 `TestErpPurReturnPosting:107-116` + `TestErpPurReturnRefundEndToEnd:178-184` 强断言 credit memo 生成 + sumOpen 自然减计 payableBalance。`returns.md §红字发票处理` 已含「实现偏离记录」+「裁决」段落（:213-219）。**credit-memo-via-return 实现运行时已落地**。 |
| **P1-MA2-083** AP/AR 发票冲销后 commitment 未恢复（resolved R1.27，Q4=(a) 下重开） | ✅ resolved (R1.27 done) → A1.15 reuse 重开（Q4=(a) 下方案B Deferred 不成立） | **退货侧 HEAD 复核：已落地（与 invoice 侧不对称）**。HEAD `ErpPurReturnProcessor.runCommitmentReleaseOnReturnHook:281-297` 在退货审核后置调 `budgetCommitmentBiz.releaseIfPresent`（释放承付）——config-gated `erp-fin.commitment-release-on-return` 默认 false。**但退货 reverseApprove/cancel 无对应 commit() 恢复承付**（与 A1.15 invoice 侧 P1-MA2-083 重开同型）——`ErpPurReturnReverseApproveProcessor` + `ErpPurReturnCancelProcessor` 仅红冲 + doReverseApprove/doCancel，零 budgetCommitmentBiz.commit()。**本切片复核结论：承付恢复 reuse P1-MA2-083 重开（已在 A1.15 登记为 MR1 修复行），退货侧修复需协同**。**P2-MA2-082 退货 release 已实现（保守方向偏移 config-gated，归 P2-MA2-082 successor），但 reverseApprove/cancel 不对称——reuse P1-MA2-083 MR1 修复行扩展至 Return Processor**。 |
| **P1-MA2-002** 多币种 P2P 本位币凭证路径未验证（resolved plan 2026-07-29-2322-2 方案 A） | ✅ resolved (plan 2026-07-29-2322-2 方案 A) | **已落地（resolved 确认）**。HEAD `PurInvoicePostingDispatcher.buildEvent:78` + `PurReturnPostingDispatcher.buildEvent:84` + `PurPaymentPostingDispatcher`（PAYMENT）+ `InvPostingDispatcher`（PURCHASE_INPUT）全部 exchangeRate 兜底 + 透传；L4 `TestErpPurMultiCurrencyPosting:70-132` 强断言 source×rate==functional（行级 amountSource≠amountFunctional）。**多币种 P2P 本位币凭证路径已验证**。 |

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

### 6.1 比对表

| 候选缺口 | 比对（grep arm-index 同域同控制点） | 裁决 | 差异依据 |
|----------|-----------------------------------|------|---------|
| **UC-PUR-04 ④ 原入库关联凭证 isReversed 标记缺失** | grep `isReversed|markOriginalVoucher|原凭证.*红冲|入库.*凭证.*红冲` arm-index → 命中 **P2-MA2-006**（returns.md red invoice drift，credit-memo-via-return resolved plan 2026-07-29-2322-1）+ **P0-MA1-021**（CostAdjustmentPostingDispatcher 跨模块直写 isReversed，已 done）+ **P1-MA2-051**（rollbackReceive 不对称，已 resolved） | **新建 P2-RC-015** | **新控制点**：本切片复核 credit-memo-via-return 实现的副作用——**正向 tryPost 路径不调 `markOriginalVoucherReversed`**，故原入库 PURCHASE_INPUT 凭证保留 isReversed=false。**与 P2-MA2-006 同根因不同维度**：P2-MA2-006 = credit-memo-via-return 实现路径整体裁决（辅助账 credit memo 替代 GL formal AP 红字发票）；本 finding = 该实现路径的另一面副作用（原凭证 isReversed 标记缺失，但 GL 净零功能等价）。**§2 P2①**（次要验收标准未完全满足——主路径[GL 净零反向凭证]OK，边界[原凭证 isReversed 标记]弱）。documented simplification 满足 §4 (i)（plan 2026-07-29-2322-1 含独立 plan-audit 通过记录），登记 successor watch-only 不强制。 |
| **UC-PUR-04 ⑤ 已开票退货 formal AP 侧红字发票未单独生成** | grep `formal.*AP|GRNI|暂估冲回|正向 receive.*invoice` arm-index → 命中 **P1-MA2-001**（GRNI 冲回，归 A2.1 finance 会计保护区域） | **复用 P1-MA2-001**（交叉引用不重审） | 同根因同控制点：returns.md「正向 receive→invoice 暂估冲回」documented simplification（:186-211）显式声明「formal AP 侧红字未单独生成」与 P1-MA2-001 GRNI 冲回缺失同根因（自动冲回机制缺失）。归 A2.1 finance 会计保护区域。本切片交叉引用追加注记，不重审。 |
| **UC-PUR-07 ② AP_INVOICE 借方 1403 在途物资 vs L1 字面"GR/IR"** | grep `GOODS_RECEIPT|PURCHASE_INPUT|命名漂移|businessType` arm-index → 命中 **P2-RC-011**（A1.15 UC-PUR-01 ④⑤ businessType 命名漂移） | **复用 P2-RC-011**（合并登记不新建） | 同根因同控制点：L1 字面 GOODS_RECEIPT/PURCHASE_INVOICE → 实仓 PURCHASE_INPUT/AP_INVOICE；AP_INVOICE 借方 1403 在途物资 vs L1 字面"GR/IR"同属命名漂移行为等价。A1.15 已登记 P2-RC-011（合并），本切片追加 RC 视角注记不新建。 |
| **UC-PUR-07 ② AP_INVOICE 价格差异科目（PPV）过账行缺失** | grep `PPV|价格差异|purchase price variance|让步接收` arm-index → 命中 **P1-RC-018**（A1.16 UC-PUR-05 ⑪⑫ 价格差异处理不完整） | **复用 P1-RC-018**（互补不重审） | 互补维度：P1-RC-018 = 让步接收时 PPV 过账行缺失（会计正确性类 Q4）；本切片 UC-PUR-07 ② = AP_INVOICE 三行基础结构完整性。两者维度互补不重叠——基础三行结构（1403/2221/2202）通过，PPV 行缺失归 P1-RC-018 MR1 修复。 |
| **UC-PUR-07 ④ 反审核"删凭证" vs 实仓"红字冲销凭证"** | grep `红冲|reverseApprove|红字|isReversed|删除凭证` arm-index → 命中 A1.1 业财过账引擎范式（红冲闭环）+ P1-MA2-051（rollbackReceive 已 resolved）+ P2-MA2-057（SalReversalListener.rollbackDelivery 不对称 watch-only） | **复用 A1.1 + P1-MA2-051**（不新建） | 范式复用：A1.1 业财过账引擎已审"红字冲销凭证（保留 + 反向）"语义等价"删除关联凭证"。本切片复核 receive/return/invoice/payment 四实体反向回写对称（P1-MA2-051 resolved），行为正确。 |
| **UC-PUR-07 ⑤ 期间 CLOSED 拒绝过账（采购侧无独立测试）** | grep `CLOSED|期间控制|period.*status|ERR_PERIOD_CLOSED` arm-index → 命中 A1.6（UC-FIN-06/07 期间结账）+ A1.1 业财过账引擎 | **复用 A1.6 + A1.1**（不新建） | finance 引擎全局生效：`ErpFinPostingProcessor.resolveOpenPeriod:524-527` 对所有 businessType 全局守卫。归 A1.6/A1.1 主核验，本切片核验采购侧过账拒绝行为通过。 |
| **UC-PUR-04 退货承付恢复（reverseApprove/cancel 不对称）** | grep `commitment|承付|release-on-return|commit\(\)` arm-index → 命中 **P1-MA2-083**（A1.15 reuse 重开）+ **P2-MA2-082**（退货 release 保守方向偏移） | **复用 P1-MA2-083 + P2-MA2-082**（A1.15 MR1 修复行扩展至 Return Processor） | 同型重开：A1.15 已 reuse P1-MA2-083 重开（invoice 侧 reverseApprove/cancel 不对称无 commit()）。本切片复核退货侧同型——`runCommitmentReleaseOnReturnHook:281-297` release 已实现（P2-MA2-082 successor），但 reverseApprove/cancel 无 commit()。**修复行扩展**：A1.15 MR1 修复行须协同覆盖 `ErpPurReturnReverseApproveProcessor` + `ErpPurReturnCancelProcessor`。 |

### 6.2 新 finding 清单

- **P2-RC-015**（UC-PUR-04 ④ 原入库关联凭证 isReversed 标记缺失，**与 P2-MA2-006 同根因不同维度**）→ successor watch-only。

### 6.3 复用 finding 交叉引用注记

- **P1-MA2-001**（GRNI 冲回，归 A2.1 finance）：追加 RC A1.17 注记——UC-PUR-04 ⑤ 已开票退货 formal AP 侧红字发票未单独生成为本 finding 同根因投影。
- **P2-MA2-006**（returns.md red invoice drift，resolved plan 2026-07-29-2322-1）：追加 RC A1.17 注记——credit-memo-via-return 实现的副作用（原入库凭证 isReversed 不标记）由 P2-RC-015 独立登记（不同维度）。
- **P1-MA2-051**（PurReversalListener.rollbackReceive，resolved R1.17）：追加 RC A1.17 注记——HEAD 复核 confirmed 已与其他三实体对称回退，receive 悬挂缺口闭合。
- **P1-MA2-083**（承付恢复，A1.15 reuse 重开）：追加 RC A1.17 注记——退货侧（`ErpPurReturnReverseApproveProcessor` + `ErpPurReturnCancelProcessor`）同型不对称，MR1 修复行须扩展覆盖。
- **P1-MA2-002**（多币种 P2P，resolved plan 2026-07-29-2322-2 方案 A）：追加 RC A1.17 注记——HEAD 复核 confirmed 行级 amountSource≠amountFunctional + source×rate==functional 落地。
- **P2-MA2-082**（退货 release 保守方向偏移 config-gated）：追加 RC A1.17 注记——`runCommitmentReleaseOnReturnHook` 已实现 release，reverseApprove/cancel 对称恢复归 P1-MA2-083 MR1 修复行协同。
- **P2-RC-011**（businessType 命名漂移，A1.15）：追加 RC A1.17 注记——UC-PUR-07 ② AP_INVOICE 借方 1403 在途物资 vs L1"GR/IR"语义等价，合并登记不新建。
- **P1-RC-018**（UC-PUR-05 价格差异处理不完整，A1.16）：追加 RC A1.17 注记——UC-PUR-07 ② AP_INVOICE 三行基础结构与 P1-RC-018 PPV 行缺失互补不重叠。

---

## 7. 静态存疑点清单（供 MA4 展开）

1. **UC-PUR-04 ④ isReversed 标记运行时确认**（P2-RC-015）：HEAD 静态判定 = `PurReturnPostingDispatcher.tryPost` 不调 `markOriginalVoucherReversed`，原入库 PURCHASE_INPUT 凭证保留 isReversed=false。运行时可构造 receive approve（生成 PURCHASE_INPUT 凭证）→ return approve（生成 PURCHASE_RETURN 反向凭证）序列，断言原 PURCHASE_INPUT 凭证 isReversed 字段是否为 true（HEAD 静态分析预期 false，运行时确认闭合 P2-RC-015 决策）。
2. **UC-PUR-04 ⑤ credit-memo-via-return 运行时 AP 余额回减**（P2-MA2-006 resolved 复核）：HEAD 静态判定 = credit memo 经 `ErpFinArApItemGenerator.resolveProfile:157-160` 生成负 openAmount + sumOpen 自然减计。运行时 TestErpPurReturnRefundEndToEnd:188-189 已强断言 sumOpen=-20。**已闭合**。
3. **UC-PUR-07 ② GR/IR 暂估应付运行时凭证行**（已闭合）：`InvAcctDocProvider:22-30` 借 1401 存货 / 贷 2202 暂估应付；`PurAcctDocProvider` 三行（1403/2221/2202）。运行时 TestErpPurReceiveStockMove:112 + TestErpPurInvoicePosting:70-100 已强断言。
4. **UC-PUR-07 ⑤ 期间 CLOSED 运行时拒绝过账**（采购侧无独立测试）：HEAD 静态判定 = `ErpFinPostingProcessor.resolveOpenPeriod:524-527` 全局生效。运行时构造 CLOSED 期间 + receive/invoice/return approve → 期望 ERR_PERIOD_CLOSED（finance 域测试覆盖，采购侧未独立测试，MA4 可补采购侧 E2E 闭合）。
5. **UC-PUR-07 ④ 反审核运行时删凭证（红字冲销）**（已闭合）：`ErpPurReturnProcessor.ensureReversed:245-265` + `PurReversalListener` 四实体回写。运行时 TestErpPurReturnPosting:122-148 + TestErpPurFinanceReversalWriteback 已强断言。
6. **UC-PUR-04 承付恢复运行时对称性**（reuse P1-MA2-083）：HEAD 静态判定 = `runCommitmentReleaseOnReturnHook` release 已实现 + reverseApprove/cancel 无 commit()（不对称）。运行时可构造 erp-fin.budget-commitment-enabled=true + erp-fin.commitment-release-on-return=true → return approve（释放承付）→ return reverseApprove（无 commit() 恢复）→ 断言 commitment 凭证余额不归位（HEAD 静态判定 = 不归位，运行时确认闭合 P1-MA2-083 MR1 修复行扩展必要性）。
7. **UC-PUR-07 ③ 多币种行级金额运行时计算**（已闭合）：`PurInvoicePostingDispatcher.buildEvent:78` exchangeRate + `ErpFinPostingProcessor.prepareContext:537` + VoucherFact 行级 amountSource/amountFunctional 分离。运行时 TestErpPurMultiCurrencyPosting:70-132 已强断言（source×rate==functional）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总表如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更（只读审计），checker 无回归风险**；R2c/R2d 略升系仓库其他并发开发所致，非本审计引入。

  | 规则 | 描述 | baseline | actual | 差异 |
  |------|------|----------|--------|------|
  | R1a-R1c | dao() in BizModel | 0/0/0 | 0/0/0 |持平|
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 持平 |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 持平 |
  | R2b | BizModel daoFor(Erp*) 跨域 | 240 | 229 | −11（改善）|
  | R2c | 全生产代码 daoFor() 总量 | 1380 | 1382 | +2（外部并发开发引入）|
  | R2d | Processor daoFor(ErpMd*) | 32 | 34 | +2（外部并发开发引入）|
  | R3 | new Erp*() 构造实体 | 5 | 5 | 持平 |
  | R5/R7/R8/R11 | 各 | 0/0/0/0 | 0/0/0/0 | 持平 |
  | R6 | @Transactional in BizModel | 2 | 2 | 持平 |
  | R10 | REQUIRES_NEW 事务 | 6 | 6 | 持平 |
  | R12a/b/c | 共享内核 import | 69/66/40 | 69/66/40 | 持平 |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（P2-RC-015 新建并附差异依据；P1-MA2-001/P2-MA2-006/P1-MA2-051/P1-MA2-083/P1-MA2-002/P2-MA2-082/P2-RC-011/P1-RC-018 复用并交叉引用），无未经比对直接新建的 finding。

---

## 9. 报告 9 段完整性自检

| # | 段落 | 状态 |
|---|------|------|
| 1 | 需求契约原文（UC-PUR-04 + UC-PUR-07 验收标准逐字引用） | ✅ §1 |
| 2 | 实现证据（L3 file:line 含跨域调用链） | ✅ §2 |
| 3 | 测试证据（L4 Test*.java + 断言强度） | ✅ §3 |
| 4 | 运行时行为证据（L5 复用 A2.8/P2P/A1.1 + 本切片差异） | ✅ §4 |
| 5 | 符合性结论（五级追踪矩阵 + 每 UC 结论 + §2 判据编号） | ✅ §5 |
| 6 | 与 arm-index 衔接（复用 or 新增 裁决 + 双向可追溯） | ✅ §6 |
| 7 | 静态存疑点清单（供 MA4 展开） | ✅ §7 |
| 8 | 过程纪律自检段（checker actual vs baseline + 独立性 + 交叉去重） | ✅ §8 |
| 9 | 与 MA2/A1.x 报告差异增量声明 | ✅ §0 |

**9 段齐全**——本报告可定稿。

---

## 整体裁决

**FAIL（有需求-实现符合性分歧，但主路径行为正确）**：2 UC 中 **UC-PUR-04 接受 on ①②③⑤ + 1 项 P2 原入库凭证 isReversed 标记缺失（P2-RC-015 新建，与 P2-MA2-006 同根因不同维度，successor watch-only）；UC-PUR-07 接受 on ①②③④⑤（GOODS_RECEIPT 触发路径归 A1.15 + AP_INVOICE 借方命名漂移归 P2-RC-011 + PPV 行缺失归 P1-RC-018 + 期间控制归 A1.1/A1.6 全局生效 + 反向回退对称 P1-MA2-051 resolved）**。

- **UC-PUR-04 ④ 原入库凭证 isReversed 标记缺失（P2-RC-015，最高优先新 finding）**：L1 `use-cases.md:120` 逐字「原入库单关联凭证被标记红冲」。实仓 `PurReturnPostingDispatcher.tryPost:44-58` 调 `executor.postEvent`（正向过账），未调 `executor.reverse`；`ErpFinPostingProcessor.markOriginalVoucherReversed:252+933-947` 仅在 reverse() 路径触发——**原入库 PURCHASE_INPUT 凭证保留 isReversed=false**，仅以独立 PURCHASE_RETURN 反向凭证实现 GL 净零。**与 P2-MA2-006 credit-memo-via-return resolved plan 2026-07-29-2322-1 同根因不同维度**（P2-MA2-006 = 整体实现路径裁决；本 finding = 该路径的 isReversed 标记侧面）。GL 净零功能等价（会计过账正确性不破坏），documented simplification 满足 §4 (i)（独立 plan-audit 通过记录），定 P2 不强制修复，successor watch-only。

- **resolved finding HEAD 复核关键结论**：
  - **P1-MA2-051**（receive 悬挂）— HEAD 复核 confirmed 已 resolved（`PurReversalListener.rollbackReceive:112-126` 现与其他三实体对称回退 APPROVED→REJECTED），方案A 落地。
  - **P2-MA2-006**（credit-memo-via-return）— HEAD 复核 confirmed 已 resolved（`ErpFinArApItemGenerator.resolveProfile:157-160` + L4 强断言）。
  - **P1-MA2-083**（承付恢复）— HEAD 复核 confirmed **退货侧同型不对称**（`runCommitmentReleaseOnReturnHook` release 已实现 + reverseApprove/cancel 无 commit()），reuse P1-MA2-083 重开（A1.15 已登记 MR1 修复行），本切片确认修复行须扩展覆盖 Return Processor。
  - **P1-MA2-002**（多币种）— HEAD 复核 confirmed 已 resolved（`PurInvoicePostingDispatcher.buildEvent:78` + `PurReturnPostingDispatcher.buildEvent:84` exchangeRate 兜底 + L4 `TestErpPurMultiCurrencyPosting` 强断言）。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 登记）。finding 修复按 §10 经 MR1（R1.0 展开为 RC-R1.n）；P1-MA2-083 退货侧修复行须扩展覆盖 Return Processor（纯 BizModel/Processor 代码逻辑，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first——调既有 commit() 入口）；P2-RC-015 successor watch-only 不强制修复。
