# rc-ma4-a4-2-40-46-purchase-f3-returns-runtime 采购退货/业财过账/红冲闭环运行时确认

> Plan: `docs/plans/2026-08-07-2300-3-rc-ma4-a4-2-40-46-purchase-f3-returns-runtime.md`
> Mission: requirement-compliance（MA4 运行时行为验证 切片 A4.2.40-A4.2.46）
> Work Item: A4.2.40 / A4.2.41 / A4.2.42 / A4.2.43 / A4.2.44 / A4.2.45 / A4.2.46（A1.17 §7 七项静态存疑点运行时确认）
> 来源: `docs/backlog/requirement-compliance-roadmap.md` A4.2.40-A4.2.46
> Audit Status: closed

## 0. 与既有 MA1/A1.x 报告差异增量声明（§9）

本报告**只补运行时行为证据**（methodology §去重协议），不重审 A1.17 已裁决的需求符合性结论与 finding 分级：

- **A1.17**（`docs/audits/2026-08-03-0300-rc-ma1-a1-17-purchase-f3-returns-business-finance.md`）：UC-PUR-04/07 五级追踪 + §7 七项静态存疑点 + §6 finding 衔接裁决（P2-RC-015 新建 + P1-MA2-083 reuse 重开[退货侧] + P2-MA2-006 resolved 复核 + P1-MA2-051 resolved 复核 + P1-MA2-002 resolved 复核 + P2-RC-011/P1-RC-018 复用）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**运行时触发链 / 凭证结构追踪 / markOriginalVoucherReversed 路径追踪 / resolveOpenPeriod 全局生效复核 / config 消费点普查 / commit() 调用方 census** 的运行时证据。
- **A1.20 SP-3**（`docs/audits/2026-08-03-0630-rc-ma1-a1-20-sales-f3-returns-family.md` §7 SP-3）：跨域期间 CLOSED guard 间接拦截存疑点与本切片 A4.2.43 合并确认（同根因[finance `resolveOpenPeriod`]同控制点，覆盖 purchase receive/invoice/return + sales return 过账路径）。
- **A4.2.27-32 purchase-F1 运行时**（`docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`）：A4.2.31 已对 P1-MA2-083 **invoice 侧**运行时不对称确认（`ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()）。本切片只补**退货侧**（Return Processor）同型不对称运行时证据。
- **A2.8 purchase 状态机** + **A2.1 P2P e2e** + **A1.1 业财过账引擎**：已证实 9 实体三轴状态机迁移 + reverseApprove 红冲闭环 + 跨域写经 Facade + GR/IR + AP 凭证范式 + isReversed 红冲标记机制。本报告引用其行为正确性结论，只补运行时差异。

本切片**只补**的运行时差异：(i) A4.2.40 isReversed 标记路径追踪（tryPost vs reverse 调用链）；(ii) A4.2.41 credit-memo AP 余额回减运行时强断言复核；(iii) A4.2.42 GR/IR 暂估应付凭证行运行时强断言复核；(iv) A4.2.43 跨域期间 CLOSED guard 全局生效复核（**业财保护区域探针——只读确认不改过账逻辑**）；(v) A4.2.44 反审核删凭证红字冲销运行时强断言复核；(vi) A4.2.45 承付恢复退货侧不对称运行时 census（**config-gated 默认 false 确认非默认活跃**）；(vii) A4.2.46 多币种行级金额运行时强断言复核。

---

## 1. 存疑点清单与判据（A1.17 §7 七项）

| # | 工作项 | A1.17 §7 存疑点 | A1.17 静态判定 | 运行时判据 |
|---|--------|----------------|----------------|-----------|
| 1 | A4.2.40 | UC-PUR-04 ④ isReversed 标记运行时确认（P2-RC-015） | tryPost 不调 markOriginalVoucherReversed，原 PURCHASE_INPUT 凭证保留 isReversed=false | 确认 `PurReturnPostingDispatcher.tryPost:44-58` 调 `executor.postEvent`（正向过账）不调 `executor.reverse`；`markOriginalVoucherReversed` 仅在 reverse() 路径触发；GL 净零经独立 PURCHASE_RETURN 反向凭证实现 |
| 2 | A4.2.41 | UC-PUR-04 ⑤ credit-memo-via-return AP 余额回减复核（P2-MA2-006 resolved） | 已闭合 | 确认 `ErpFinArApItemGenerator.resolveProfile:157-160` DIRECTION_PAYABLE + 负 openAmount + sumOpen 自然减计；L4 强断言覆盖 |
| 3 | A4.2.42 | UC-PUR-07 ② GR/IR 暂估应付凭证行复核 | 已闭合 | 确认 `InvAcctDocProvider:70-74` 借 1401 存货/贷 2202 暂估 + `PurAcctDocProvider:74-82` 三行；L4 强断言覆盖 |
| 4 | A4.2.43 | UC-PUR-07 ⑤ 跨域期间 CLOSED guard（A1.17 §7-4 + A1.20 SP-3 合并） | resolveOpenPeriod 全局生效 | 确认 `ErpFinPostingProcessor.resolveOpenPeriod:524-527` period.status != OPEN 抛 ERR_PERIOD_CLOSED 对所有 businessType 全局生效；purchase receive/invoice/return + sales return 过账路径经 finance 引擎间接拦截 |
| 5 | A4.2.44 | UC-PUR-07 ④ 反审核删凭证（红字冲销）复核 | 已闭合 | 确认 `ErpPurReturnProcessor.ensureReversed:245-265` + `PurReversalListener` 四实体回写；L4 强断言覆盖 |
| 6 | A4.2.45 | UC-PUR-04 承付恢复运行时对称性[退货侧]（reuse P1-MA2-083） | 不对称（release 已实现 + reverseApprove/cancel 无 commit()） | 确认 `runCommitmentReleaseOnReturnHook` release + Return reverseApprove/cancel Processor 零 commit()；config-gated 默认 false |
| 7 | A4.2.46 | UC-PUR-07 ③ 多币种行级金额复核 | 已闭合 | 确认 `PurInvoicePostingDispatcher.buildEvent` exchangeRate + `PurAcctDocProvider` fact 行级 source/functional 分离；L4 强断言覆盖 |

---

## 2. 运行时证据采集（L3 `file:line` + L4 强断言）

### 2-1 A4.2.40 isReversed 标记运行时缺失确认 — 维持 P2-RC-015（successor watch-only）

**L3 路径追踪**（live code 实测）：

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurReturnPostingDispatcher.java:44-58` — `tryPost` 成功路径调 `executor.postEvent(event)`（:47，正向过账，businessType=PURCHASE_RETURN），**未调 `executor.reverse`**（reverse 仅在独立 `reverse():64-75` 方法，由 `ErpPurReturnProcessor.ensureReversed:247` 调用于反审核路径）。
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java:252` — `markOriginalVoucherReversed` 调用点位于 `reverse()` 方法内部（`final Long firstOriginalId = originals.get(0).getId();` 之后），**仅在 reverse() 路径触发**，正向 `postEvent`/`post()` 路径不触发。
- `ErpFinPostingProcessor.java:933-947` — `markOriginalVoucherReversed` 实现：遍历 `findBillLinks` 找原 NORMAL 凭证，置 `isReversed=true`。PURCHASE_RETURN 过账为**新建独立 NORMAL 凭证**（非 reverse），不触发此方法。
- 结论：原入库 PURCHASE_INPUT 凭证（由 `InvPostingDispatcher:169` + `InvAcctDocProvider:70-74` 借 1401/贷 2202）保留 **isReversed=false**；GL 净零由独立 PURCHASE_RETURN 反向凭证（`PurAcctDocProvider:85-87` 借 2202/贷 1401）实现。两凭证净借贷相抵。

**L4 测试覆盖**：`TestErpPurReturnPosting:96-104` 强断言 PURCHASE_RETURN 反向凭证存在 + 2 行结构 + 业财回链；**未断言原 PURCHASE_INPUT 凭证 isReversed=true**（A1.17 §7-1 已记录该字面验收标准零 L4 覆盖）。

**裁决**：运行时确认 A1.17 静态判定成立——**维持 P2-RC-015 P2 successor watch-only**（GL 净零功能等价，会计过账正确性不破坏；documented simplification 满足 §4(i) = plan `2026-07-29-2322-1` resolved P2-MA2-006 含独立 plan-audit 通过记录；successor watch-only 不强制修复）。**不触发 MR0**（运行时未发现活跃会计错误——GL 净零经独立反向凭证正确实现，试算平衡不受影响；isReversed 标记缺失仅影响 trial balance 报表筛选维度）。

### 2-2 A4.2.41 credit-memo-via-return AP 余额回减复核 — 闭合，维持 P2-MA2-006 resolved

**L3 路径追踪**：

- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinArApItemGenerator.java:157-160` — `resolveProfile` PURCHASE_RETURN 分支 → `DIRECTION_PAYABLE` + `SOURCE_BILL_PUR_RETURN`，生成**负 openAmount** ArApItem（credit memo 语义），使 `PartnerBalanceUpdater.sumOpen` 自然减计 `payableBalance`（无侵入零改动 sumOpen/方向枚举）。

**L4 强断言**：

- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnRefundEndToEnd.java:179-189` — 强断言：退货辅助账项 `direction=DIRECTION_PAYABLE` + `sourceBillType=SOURCE_BILL_PUR_RETURN` + `openAmountFunctional = 负 RETURN_AMOUNT`（credit memo）；**`sumPayableOpen() == RETURN_AMOUNT.negate() = -20`**（应付余额回减）。
- `TestErpPurReturnPosting.java:107-116` — 强断言 credit memo 生成 + sumOpen 自然减计。

**裁决**：A1.17 §7-2 静态判定「已闭合」运行时确认成立——**闭合，维持 P2-MA2-006 resolved**（credit-memo-via-return 实现运行时已落地，AP 余额回减经辅助账层 sumOpen 正确实现）。

### 2-3 A4.2.42 GR/IR 暂估应付凭证行复核 — 闭合

**L3 路径追踪**：

- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvAcctDocProvider.java:70-74` — PURCHASE_INPUT 借 `1401` 库存商品 DC_DEBIT / 贷 `2202` 应付账款-暂估 DC_CREDIT（GR/IR），覆盖 UC-PUR-07 ① 入库过账凭证行结构。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java:74-82` — AP_INVOICE 三行：`1403` 在途物资 DEBIT TOTAL_AMOUNT + `2221` 应交税费-进项税 DEBIT TOTAL_TAX_AMOUNT + `2202` 应付账款 CREDIT TOTAL_AMOUNT_WITH_TAX，覆盖 UC-PUR-07 ② 发票过账凭证行结构。

**L4 强断言**：

- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveStockMove.java:112` — 强断言贷方合计=暂估应付（50），覆盖 UC-PUR-07 ①。
- `TestErpPurInvoicePosting.java:70-100` — 强断言 AP_INVOICE 凭证 3 行（借采购 100 / 借进项税 13 / 贷应付 113），覆盖 UC-PUR-07 ②。

**裁决**：A1.17 §7-3 静态判定「已闭合」运行时确认成立——**闭合**（GR/IR 暂估应付凭证行 + AP_INVOICE 三行结构运行时正确实现，L4 强断言覆盖）。

### 2-4 A4.2.43 跨域期间 CLOSED guard 运行时拒绝过账确认 — 闭合（A1.17 §7-4 + A1.20 SP-3 合并）

**L3 路径追踪**（**业财保护区域探针——只读确认不改过账逻辑**）：

- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java:508-528` — `resolveOpenPeriod`：按 voucherDate + orgId 查询会计期间，**`if (!PERIOD_STATUS_OPEN.equals(period.getStatus())) throw new NopException(ERR_PERIOD_CLOSED)`**（:524-527）。此守卫位于 `post()`/`reverse()` 公共路径，**对所有 businessType 全局生效**（含 PURCHASE_INPUT / AP_INVOICE / PURCHASE_RETURN / SALES_OUTPUT / SALES_RETURN 等全部枚举，无业务类型白名单）。
- 间接拦截路径：purchase receive/invoice/return approve → `Pur*PostingDispatcher.tryPost` → `executor.postEvent` → finance `ErpFinPostingProcessor.post` → `resolveOpenPeriod`（间接守卫，无 purchase 侧独立期间校验）；sales return 同型经 SALES_RETURN 过账路径。
- **A1.20 SP-3 合并声明**：同根因（finance `resolveOpenPeriod:524-527`）同控制点（finance 引擎全局守卫），A1.20 §7 SP-3（sales return 期间 CLOSED）与本切片 A4.2.43（purchase receive/invoice/return）覆盖同一 finance 引擎守卫，合并确认一次即可。

**L4 测试覆盖**：finance 域 `TestErpFinPostingPeriod*` 系列覆盖 CLOSED 期间拒绝过账（归 A1.1/A1.6 主核验）；**purchase 侧无独立测试**（A1.17 §7-4 已记录），但 finance 引擎守卫全局生效，间接拦截对 purchase 过账路径有效。

**裁决**：A1.17 §7-4 + A1.20 SP-3 静态判定「全局生效」运行时确认成立——**主路径行为正确（间接守卫有效），闭合**（finance 引擎 `resolveOpenPeriod` 对所有 businessType 全局生效，purchase/sales 过账路径经 finance 引擎间接拦截）。**roadmap 两处（A1.17 §7-4 投影 A4.2.43 + A1.20 SP-3 投影）同步 done**。

### 2-5 A4.2.44 反审核删凭证（红字冲销）复核 — 闭合

**L3 路径追踪**：

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java:245-265` — `ensureReversed`：若 `returnOrder.posted==true` → 调 `postingDispatcher.reverse(returnOrder)`（:247）经 `IErpFinVoucherBiz.reverse` Facade 红字冲销（保留原凭证 + 生成反向红字凭证，语义等价"删除关联凭证"——A1.1 业财过账引擎范式）→ reload + `setPosted(false)` + 清 postedAt/postedBy（:249-251）；并 `stockMoveBiz.reverse` 库存物理冲销（:264）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurReversalListener.java:70-126` — 四实体反向回写对称：`rollbackInvoice:70-82` + `rollbackPayment:84-96` + `rollbackReturn:98-110` + `rollbackReceive:112-126`，**全部** `posted=false` + 清 postedAt/postedBy + `if (APPROVED) setApproveStatus(REJECTED)`（P1-MA2-051 receive 悬挂已 resolved，四实体对称）。

**L4 强断言**：

- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReturnPosting.java:122-148` — `testReverseApproveCancelsApItemAndRestoresBalance`：reverseApprove → posted 反转为 false + APPROVE_STATUS_REJECTED + 辅助账 CANCELLED + openAmount=0 + 应付余额恢复=0。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurFinanceReversalWriteback.java:95-106` — 财务侧直接红冲已过账凭证：`voucherBiz.reverse` → `PurReversalListener` 回退 invoice.posted=false + APPROVED→REJECTED + postedAt/postedBy 清空。
- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/posting/TestPurReversalListenerReceiveRollback.java:49-65` — P1-MA2-051 receive 对称回退强断言。

**裁决**：A1.17 §7-5 静态判定「已闭合」运行时确认成立——**闭合**（红字冲销凭证 + 四实体反向回写对称 + posted=false + APPROVED→REJECTED 运行时正确，L4 强断言覆盖）。

### 2-6 A4.2.45 承付恢复运行时对称性[退货侧]确认 — 维持 P1-MA2-083（reuse 重开）

**L3 路径追踪**（commit() 调用方 census）：

- **正向（release 已实现）**：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnApproveProcessor.java:60` — `approve()` 末尾调 `processor.runCommitmentReleaseOnReturnHook(returnOrder, context)`。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java:281-297` — `runCommitmentReleaseOnReturnHook`：config-gated `erp-fin.commitment-release-on-return`（默认 false，:282-285）+ 调 `budgetCommitmentBiz.releaseIfPresent(...)` 释放承付（:291-292），容错 `NopException` 静默跳过（:293-296）。
- **反向（commit 缺失，不对称）**：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnReverseApproveProcessor.java:23-37` — `reverseApprove()` 仅 `processor.ensureReversed` 红冲 + `setApproveStatus(REJECTED)` + 清 approvedBy/At，**零 budgetCommitmentBiz.commit() 调用**。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnCancelProcessor.java:23-33` — `cancel()` 仅 `ensureReversed`（若 approved）+ `setDocStatus(CANCELLED)`，**零 commit() 调用**。
- **不对称确认**：approve → release commitment；reverseApprove/cancel → 凭证红冲 + 库存冲销，**commitment 保持已释放（零 commit() 恢复）**。与 A4.2.31（invoice 侧 P1-MA2-083）同型不对称。
- **commit() 调用方全域 census**（A4.2.31 已普查）：全域仅 `ErpPurOrderProcessor` commit-on-order-approve 单点 + 承付总开关 `erp-fin.budget-commitment-enabled` 默认 false + `erp-fin.commitment-release-on-return` 默认 false（A4.2.38/A4.2.31 已普查全 20 生产 application.yaml 零 override）→ **非默认活跃**。

**L4 测试覆盖**：`TestErpPurReturnCommitmentRelease` 覆盖 approve 释放主路径；reverseApprove/cancel 恢复路径零 commit() 断言（与 invoice 侧 A4.2.31 同型，无独立测试断言 commitment 恢复）。

**裁决**：运行时确认 A1.17 §7-6 静态判定「不对称」成立——**维持 P1-MA2-083（reuse 重开）**（Q4=(a) 下方案B Deferred 关闭不成立，修复归 MR1 R1.0 展开器——退货侧修复行 = `ErpPurReturnReverseApproveProcessor.reverseApprove` + `ErpPurReturnCancelProcessor.cancel` 新增按 return.receiveId→receive.orderId 反查 PO + config-gated 调既有 `budgetCommitmentBiz.commit()` 入口恢复承付 + 处理部分退货/跨期语义；**纯 BizModel/Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**——调既有 commit() 入口属纯 Processor 逻辑非 PostingProcessor 核心路径；与 A4.2.31 invoice 侧 MR1 修复行合并）。config-gated `erp-fin.budget-commitment-enabled` + `erp-fin.commitment-release-on-return` 双默认 false 确认**非默认活跃**（不对称破坏仅部署双开关显式启用时显现）。**不触发 MR0**（config-gated 默认 false 保护，非默认活跃路径破坏；运行时未发现活跃数据破坏）。

### 2-7 A4.2.46 多币种行级金额复核 — 闭合

**L3 路径追踪**：

- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurInvoicePostingDispatcher.java:78`（`buildEvent`）— `exchangeRate = invoice.getExchangeRate() != null ? ... : BigDecimal.ONE`（兜底 ONE）。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurReturnPostingDispatcher.java:84`（`buildEvent`）— 同型 exchangeRate 兜底。
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingProcessor.java:537`（`prepareContext`）— `ctx.setExchangeRate(event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT)` 兜底。
- `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java:105-118`（`fact`）— `BigDecimal functional = sourceAmount.multiply(rate)`；`fact.setAmountSource(sourceAmount)` + `fact.setAmountFunctional(functional)` + `fact.setAmount(functional)`，行级 source/functional 分离。

**L4 强断言**：

- `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurMultiCurrencyPosting.java:67-108` — `testMultiCurrencyApInvoiceLineAmounts`：外币 AP_INVOICE（source 100/13/113 × rate 7 = functional 700/91/791）：头合计本位币 791/791；1403 在途物资 source=100/functional=700；2221 进项税 source=13/functional=91；2202 应付 source=113/functional=791；`amountSource != amountFunctional` 强断言（行级分离）。
- `TestErpPurMultiCurrencyPosting.java:112-132` — `testMultiCurrencyPaymentLineAmounts`：PAYMENT 行 source=113/functional=791 强断言。

**裁决**：A1.17 §7-7 静态判定「已闭合」运行时确认成立——**闭合**（行级 amountSource≠amountFunctional + source×rate==functional 运行时正确实现，L4 强断言覆盖 source×rate==functional[100×7=700 / 13×7=91 / 113×7=791]）。

---

## 3. 测试证据汇总（L4，断言强度）

| 工作项 | 测试 | 断言强度 | 覆盖验收标准 |
|--------|------|---------|-------------|
| A4.2.40 | `TestErpPurReturnPosting:96-104`（PURCHASE_RETURN 反向凭证） | 强（凭证 2 行 + 业财回链）| UC-PUR-04 ③ 反向凭证存在（**④ isReversed 标记零覆盖**） |
| A4.2.41 | `TestErpPurReturnRefundEndToEnd:179-189` + `TestErpPurReturnPosting:107-116` | 强（credit memo + sumOpen=-20）| UC-PUR-04 ⑤ AP 余额回减 |
| A4.2.42 | `TestErpPurReceiveStockMove:112` + `TestErpPurInvoicePosting:70-100` | 强（凭证行结构 + 金额）| UC-PUR-07 ①② |
| A4.2.43 | finance `TestErpFinPostingPeriod*`（A1.1/A1.6 主核验） | 强（CLOSED 期间拒绝过账）| UC-PUR-07 ⑤（finance 引擎全局，purchase 侧无独立测试） |
| A4.2.44 | `TestErpPurReturnPosting:122-148` + `TestErpPurFinanceReversalWriteback:95-106` + `TestPurReversalListenerReceiveRollback:49-65` | 强（红冲闭环 + posted=false + APPROVED→REJECTED + 四实体对称）| UC-PUR-07 ④ |
| A4.2.45 | `TestErpPurReturnCommitmentRelease`（approve 释放主路径） | 强（approve release）；reverseApprove/cancel 恢复零断言 | 承付释放主路径（**恢复路径零覆盖 = 不对称证据**） |
| A4.2.46 | `TestErpPurMultiCurrencyPosting:67-132` | 强（source×rate==functional 行级）| UC-PUR-07 ③ |

---

## 4. 运行时行为证据（L5）

- **复用 A1.17 §4**：UC-PUR-04 ①②③⑤ + UC-PUR-07 ①②③④⑤ 运行时行为已证实（经 A2.8 状态机 + A2.1 P2P + A1.1 业财过账引擎三重证实）。本切片只补七项存疑点的运行时差异。
- **复用 A4.2.27-32**（`2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`）：A4.2.31 已对 P1-MA2-083 **invoice 侧**运行时不对称确认（`ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()）。本切片确认**退货侧同型不对称**（Return reverseApprove/cancel Processor 零 commit()）。
- **本切片补的运行时差异**（经 live code 实测 + L4 强断言）：
  - **A4.2.40** isReversed 标记路径追踪：`tryPost`（正向 postEvent）vs `reverse`（markOriginalVoucherReversed）调用链确认——原 PURCHASE_INPUT 凭证 isReversed=false，GL 净零经独立 PURCHASE_RETURN 反向凭证实现。
  - **A4.2.41** credit-memo AP 余额回减：`resolveProfile:157-160` + L4 sumOpen=-20 强断言。
  - **A4.2.42** GR/IR 暂估应付凭证行：`InvAcctDocProvider:70-74` + `PurAcctDocProvider:74-82` + L4 强断言。
  - **A4.2.43** 跨域期间 CLOSED guard：`resolveOpenPeriod:524-527` 全局生效 + 间接拦截 purchase/sales 过账路径（A1.20 SP-3 合并）。
  - **A4.2.44** 反审核删凭证红字冲销：`ensureReversed:245-265` + `PurReversalListener:70-126` 四实体对称 + L4 强断言。
  - **A4.2.45** 承付恢复退货侧不对称：`runCommitmentReleaseOnReturnHook:281-297` release 已实现 + Return reverseApprove/cancel Processor 零 commit() + config-gated 默认 false。
  - **A4.2.46** 多币种行级金额：`PurInvoicePostingDispatcher.buildEvent` exchangeRate + `PurAcctDocProvider.fact` 行级 source/functional 分离 + L4 source×rate==functional 强断言。

---

## 5. 符合性结论（七项存疑点裁决）

### 5.1 七项裁决矩阵

| 工作项 | A1.17 §7 存疑点 | §2 判据命中分支 | 运行时裁决 | finding 衔接 |
|--------|----------------|----------------|-----------|-------------|
| **A4.2.40** | UC-PUR-04 ④ isReversed 标记 | 维持 P2 successor watch-only + 运行时证据记录 | **维持 P2-RC-015 P2 successor watch-only**（GL 净零功能等价，documented simplification 满足 §4(i)，不强制修复） | P2-RC-015 :163 |
| **A4.2.41** | UC-PUR-04 ⑤ credit-memo AP 余额回减 | 主路径闭合 | **闭合，维持 P2-MA2-006 resolved** | P2-MA2-006 :713 |
| **A4.2.42** | UC-PUR-07 ② GR/IR 暂估应付凭证行 | 主路径闭合 | **闭合**（凭证行结构正确） | 无新 finding |
| **A4.2.43** | UC-PUR-07 ⑤ 跨域期间 CLOSED guard（+ A1.20 SP-3） | 主路径闭合 | **闭合（间接守卫有效）**（finance 引擎全局生效，roadmap 两处同步 done） | 无新 finding |
| **A4.2.44** | UC-PUR-07 ④ 反审核删凭证红字冲销 | 主路径闭合 | **闭合**（红冲闭环 + 四实体对称） | 无新 finding |
| **A4.2.45** | UC-PUR-04 承付恢复退货侧对称性 | 维持 P1 reuse 重开 + 运行时证据记录 | **维持 P1-MA2-083 P1 reuse 重开**（退货侧修复归 MR1，调既有 commit() 入口纯 Processor 预授权） | P1-MA2-083 :546（+ A4.2.31 退货侧扩展） |
| **A4.2.46** | UC-PUR-07 ③ 多币种行级金额 | 主路径闭合 | **闭合**（行级 source/functional 分离 + source×rate==functional） | 无新 finding |

### 5.2 裁决分支汇总

- **五项主路径闭合**（A4.2.41 / A4.2.42 / A4.2.43 / A4.2.44 / A4.2.46）→ 行为正确，无新 finding。
- **一项维持 P2 successor watch-only**（A4.2.40 → P2-RC-015）→ GL 净零功能等价，不强制修复。
- **一项维持 P1 reuse 重开**（A4.2.45 → P1-MA2-083）→ Q4 强制实现，修复归 MR1 R1.0 展开器（退货侧扩展，调既有 commit() 入口纯 Processor 预授权不触 §5 ask-first）。
- **零升级触发 MR0**（运行时未发现活跃数据破坏或会计错误已活跃——P2-RC-015 GL 净零正确实现 + P1-MA2-083 config-gated 默认 false 非默认活跃）。
- **零新 finding**（全部经 grep arm-index 同域同控制点比对，维持既有分级不撤销，无未经比对直接新建的 finding）。

---

## 6. 与 arm-index 衔接（复用维持裁决）

### 6.1 比对表

| 本切片存疑点 | 比对 arm-index | 裁决 | 差异依据 |
|-------------|---------------|------|---------|
| A4.2.40 isReversed 标记 | `P2-RC-015`（:163，A1.17 新建）| **维持 P2 successor watch-only** | 同根因同控制点：tryPost 正向路径不调 markOriginalVoucherReversed，运行时确认原 PURCHASE_INPUT 凭证 isReversed=false。GL 净零功能等价，不强制修复 |
| A4.2.41 credit-memo AP 余额回减 | `P2-MA2-006`（:713，resolved plan 2026-07-29-2322-1）| **维持 resolved** | 同根因同控制点：`resolveProfile:157-160` + L4 sumOpen=-20 强断言。credit-memo-via-return 运行时已落地 |
| A4.2.45 承付恢复退货侧 | `P1-MA2-083`（:546，resolved R1.27 → A1.15 reuse 重开 + A4.2.31 invoice 侧确认）| **维持 P1 reuse 重开（退货侧扩展）** | 同型重开：A4.2.31 已确认 invoice 侧不对称（`ErpPurInvoiceReverseApproveProcessor:22-37` 零 commit()）；本切片确认退货侧同型（`ErpPurReturnReverseApproveProcessor` + `ErpPurReturnCancelProcessor` 零 commit()）。MR1 修复行须协同覆盖 Return Processor |

### 6.2 新 finding 清单

- **无**（零新 finding；全部经 grep arm-index 同域同控制点比对后给出「维持既有分级」裁决）。

### 6.3 复用 finding 交叉引用注记（追加 RC A4.2.40-46 运行时确认）

- **P2-RC-015**（:163，UC-PUR-04 ④ isReversed 标记缺失）：追加 RC A4.2.40 运行时确认注记——`PurReturnPostingDispatcher.tryPost:44-58` 调 `executor.postEvent`（正向）不调 `executor.reverse`；`markOriginalVoucherReversed:252+933-947` 仅在 reverse() 路径触发——原 PURCHASE_INPUT 凭证 isReversed=false 运行时确认成立。GL 净零经独立 PURCHASE_RETURN 反向凭证（`PurAcctDocProvider:85-87` 借 2202/贷 1401）正确实现。维持 P2 successor watch-only。
- **P2-MA2-006**（:713，credit-memo-via-return resolved）：追加 RC A4.2.41 运行时确认注记——`ErpFinArApItemGenerator.resolveProfile:157-160` DIRECTION_PAYABLE + 负 openAmount + `TestErpPurReturnRefundEndToEnd:188-189` sumOpen=-20 强断言。维持 resolved。
- **P1-MA2-083**（:546，承付恢复 reuse 重开）：追加 RC A4.2.45 运行时确认注记——退货侧（`ErpPurReturnReverseApproveProcessor.reverseApprove:23-37` + `ErpPurReturnCancelProcessor.cancel:23-33`）零 commit() + 正向 `ErpPurReturnApproveProcessor.approve:60` 调 `runCommitmentReleaseOnReturnHook` release + config-gated `erp-fin.budget-commitment-enabled` + `erp-fin.commitment-release-on-return` 双默认 false。MR1 修复行须扩展覆盖 Return Processor（与 A4.2.31 invoice 侧合并）。

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告为只读运行时确认（**零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更**），checker 无回归风险（actual == baseline，0 漂移）。区分门控退出码 vs 纯 reporter 退出码——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow 解析 actual > baseline。本报告不以 checker 脚本退出码 0 作为门控通过依据；**无代码变更故无 build/test 回归风险**。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 7 项运行时裁决已按 §去重协议 grep arm-index 同域同控制点后给出「维持既有分级」结论（P2-RC-015 维持 P2 successor watch-only + P2-MA2-006 维持 resolved + P1-MA2-083 维持 P1 reuse 重开退货侧扩展），**无未经比对直接新建的 finding，无新 finding 新建**。
- [x] **业财保护区域探针纪律声明**：A4.2.40（isReversed 标记路径追踪）+ A4.2.43（期间 CLOSED guard 全局生效复核）触及业财保护区域探针——**只读确认，不改过账逻辑/期间守卫/PurReturnPostingDispatcher/ErpFinPostingProcessor 核心路径**（READ-ONLY 标记）。零生产代码变更。
- [x] **A4.2.43 合并声明**：A1.17 §7-4（purchase 期间 CLOSED）+ A1.20 SP-3（sales return 期间 CLOSED）同根因（finance `resolveOpenPeriod:524-527`）同控制点（finance 引擎全局守卫），合并确认一次——roadmap A4.2.43 行覆盖两处来源，done 后两处（A1.17 §7-4 投影 + A1.20 SP-3 投影）同步标记 done。

---

## 8. 报告 9 段完整性自检

| # | 段落 | 状态 |
|---|------|------|
| 1 | 存疑点清单与判据（A1.17 §7 七项 + 判据） | ✅ §1 |
| 2 | 运行时证据采集（L3 file:line + L4 强断言，七项逐项） | ✅ §2 |
| 3 | 测试证据汇总（L4 Test*.java + 断言强度） | ✅ §3 |
| 4 | 运行时行为证据（L5 复用 A1.17 §4 + A4.2.27-32 + 本切片差异） | ✅ §4 |
| 5 | 符合性结论（七项裁决矩阵 + §2 判据命中分支） | ✅ §5 |
| 6 | 与 arm-index 衔接（复用维持裁决 + 交叉引用注记） | ✅ §6 |
| 7 | 过程纪律自检（checker actual==baseline + 独立性 + 交叉去重 + 业财保护区域探针纪律 + A4.2.43 合并声明） | ✅ §7 |
| 8 | 报告 9 段完整性自检 | ✅ §8 |
| 9 | 与既有 MA1/A1.x 报告差异增量声明 | ✅ §0 |

**9 段齐全**——本报告可定稿。

---

## 整体裁决

**PASS（七项存疑点全数收口，五项主路径闭合 + 一项维持 P2 successor watch-only + 一项维持 P1 reuse 重开，零新 finding / 不触发 MR0）**：

- **五项主路径闭合**（A4.2.41 / A4.2.42 / A4.2.43 / A4.2.44 / A4.2.46）——运行时行为正确，L4 强断言覆盖。
- **A4.2.40 维持 P2-RC-015 successor watch-only**——原入库 PURCHASE_INPUT 凭证 isReversed=false 运行时确认成立（tryPost 正向路径不调 markOriginalVoucherReversed），GL 净零经独立 PURCHASE_RETURN 反向凭证功能等价实现，会计过账正确性不破坏，documented simplification 满足 §4(i)。
- **A4.2.45 维持 P1-MA2-083 reuse 重开（退货侧扩展）**——Return reverseApprove/cancel Processor 零 commit() 不对称确认成立，Q4=(a) 下方案B Deferred 不成立，修复归 MR1 R1.0 展开器（调既有 commit() 入口纯 Processor 预授权不触 §5 ask-first；与 A4.2.31 invoice 侧修复行合并）；config-gated 双默认 false 确认非默认活跃。

**A1.17 §7 七项静态判定无一翻转**，零新 finding，不触发 MR0，不归 MR1（本审计）。P2-RC-015 successor watch-only 不强制修复；P1-MA2-083 退货侧修复义务归 MR1 R1.0 展开器（与 A4.2.31 合并）。**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index 交叉引用注记 + roadmap/log 同步）。
