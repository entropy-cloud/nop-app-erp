# 2026-08-03-0407-3 rc-ma1-a1-20-sales-f3-returns-family sales-F3 退货族需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.20（MA1 需求追踪矩阵审计 — sales-F3 退货族）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.20
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.20 的 0.2 依赖）、`2026-08-03-0407-1-rc-ma1-a1-18-sales-f1-mainflow-pricing.md` + `2026-08-03-0407-2-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`（同批次 sales 审计，先 F1/F2 后 F3）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.20 给出 UC 清单 = `UC-SAL-04/05/06/07/09`（5 UC），含 `use-cases.md:99/:132/:149/:165/:200` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/sales/use-cases.md`（机制见 `docs/design/sales/returns.md`）：
  - UC-SAL-04 销售退货退款（已开票，`:99`）：创建退货单（关联原出库单）→ 审核通过（入库恢复库存：可用量 += 退货数量）→ 生成红字发票（关联原发票，金额取负，原应收被冲减）→ 创建退款单（核销原收款，原收款核销被反向释放已核销金额）。状态断言：退货单.returnStatus=全额（若退完）、refundStatus=已退（若款已退）。回链：退货单.来源单号==出库单。
  - UC-SAL-05 未开票退货冲减暂估应收（`:132`）：退货审核→库存恢复；冲减暂估应收（若出库时已暂估应收）；不生成红字发票（因无原发票）；订单未交货量回填（未交货量 = 订单数量 - 已出库 + 退货）。
  - UC-SAL-06 退货换货（`:149`）：退货单（returnType=换货）审核→库存恢复；换货生成新销售出库单（关联退货单）→扣库存；若价差：补差价开票或退款；退货单与换货单通过 sourceBill 双向关联。
  - UC-SAL-07 退货成本处理（`:165`）：退货入库成本 = 策略（原出库成本 | 当前库存成本 | 退货协议价），由配置 `erp-sal.return-cost-method` 决定；库存余额成本层（CostLayer）按该成本增加。
  - UC-SAL-09 退货约束校验（`:200`）：退货数量 > 未退货量 → 拒绝；退货关联发票已核销 → 需先撤回核销再退货；退货期间已结账 → 拒绝（期间控制）；超额退货（超原出库量）→ 拒绝。

- **L3 代码实现现状（实测）**——UC-SAL-04 部分实现（红字发票以 credit-memo 替代，P2-MA2-011 已登记 documented simplification）；UC-SAL-05/06/07/09 多处缺失：
  - **退货审核 + 库存恢复**（UC-SAL-04/05 共有）：`ErpSalReturnBizModel.java:22-40`（仅 cancel；审批经 `approval-support.xbiz`）；`ErpSalReturnApproveProcessor.java:32-55`（validate → `triggerIncomingMove` → flush → `triggerPosting` → `refundOrchestrator.orchestrateRefund` → setApproveStatus APPROVED + posted）；`ErpSalReturnProcessor.triggerIncomingMove:285-290` → `IErpInvStockMoveBiz.generateMove`（跨域，direction=INCOMING，`originReturnedMoveId` 追溯 `:292-300`）；`ReturnStockMoveBuilder.java:34-47`（`relatedBillType=ERP_SAL_RETURN`，`:64 unitCost = line.unitPrice`，`:25` Javadoc "按原出库成本冲减存货估值口径"）。库存恢复行为**已实现**（`TestErpSalReturnInventory` 证 20+4=24 行级）。
  - **红字发票（UC-SAL-04，缺口 #1）**：**无红字 `ErpSalInvoice` 实体/生成路径**。实际用 `SalReturnPostingDispatcher.java:51-65`（businessType=`SALES_RETURN`）→ `SalAcctDocProvider.java:83-87`（Dr 1401 库存商品 / Cr 6401 主营业务成本，**成本/存货侧 GL，非收入/AR 侧**）+ finance `ErpFinArApItemGenerator` 生成负向 AR 辅助项（credit memo，`TestErpSalReturnPosting:106-116` 证 openAmountFunctional=−RETURN_WITH_TAX 行级）。**已登记为 P2-MA2-011 documented simplification**（owner-doc drift，`returns.md §红字发票处理` doc 与实现偏离）；`ErpSalReturn` 无 `originalInvoiceId`/`redInvoiceId` 列。
  - **退款 + 核销反向（UC-SAL-04，缺口 #2）**：`ReturnRefundOrchestrator.orchestrateRefund:49-57`（找客户已核销发票 receivedAmount>0）→ `reverseSettlementsForInvoice:79-99`（逐正向 `ErpSalReceiptLine` 调 `receiptSettler.reverseSettlement`）→ `ReceiptSettler.reverseSettlement:116-137`（生成负向 ReceiptLine + 重算 receivedStatus/writtenOffStatus）。**无独立退款单实体**（仅负向 ReceiptLine）；退款方式路由（原路退回/其他账户/预收款抵扣/现金）为 `ReturnRefundOrchestrator.java:33` 文档化 Non-Goal；`restoreRefund:64-67` 为 MVP no-op。
  - **未开票退货路径（UC-SAL-05，缺口 #3/#4）**：库存恢复同上；**暂估应收条件冲减缺失**（`SalReturnPostingDispatcher.buildEvent:84-103` 无论是否暂估应收统一发 SALES_RETURN posting + credit memo，无分支）；**未交货量更新缺失**（无 `undeliveredQty` 字段/逻辑，`ErpSalReturnProcessor.doApprove:193-207` 不调任何订单量更新；`ErpSalOrderBizModel.updateDeliveryStatus:242` 仅 rollup 头级 deliveryStatus；grep `undeliveredQty|未交货` 无生产代码）。
  - **换货（UC-SAL-06，缺口 #5）**：**完全缺失**。ORM `ErpSalReturn`（`app-erp-sales.orm.xml:857-934`）**无 `returnType` 列**；无 `换货` 分支；无换货新出库单生成；无价差开票/退款；无 sourceBill 双向关联。grep `换货|exchange.*return|sourceBill` 无生产匹配。
  - **退货成本策略（UC-SAL-07，缺口 #6）**：配置键 `erp-sal.return-cost-method` **未声明**（`ErpSalConstants.java:74-76` 仅 `return-reason-required`+`return-approval-required`；跨模块 grep `return-cost-method|returnCostMethod|CONFIG_RETURN_COST` 为空）。**仅实现"原出库成本"1/3 策略**（`ReturnStockMoveBuilder.java:65 unitCost=line.unitPrice` + `SalReturnPostingDispatcher:109-117 computeTotalCost=Σ qty×unitPrice`）；"当前库存成本"/"退货协议价"缺失；CostLayer 经库存域 `StockMoveBookkeeper` 间接更新（无销售域直写）。
  - **退货约束校验（UC-SAL-09，缺口 #7/#8）**：已实现守卫：`ReturnQtyValidator.java:46-66`（maxReturnable=delivered−alreadyReturned，超抛 `ERR_RETURN_QTY_EXCEED`，单一守卫覆盖"未退货量"+"超额"）；`requireSourceDeliveryApproved:230-241`（`ERR_RETURN_DELIVERY_NOT_APPROVED`）；`requireReasonIfConfigured:243-255`（`ERR_RETURN_REASON_REQUIRED`）；`requireCustomerActive:357-367`；`requireLinesNonEmpty:350-355`。**缺失守卫**：①"退货关联发票已核销→需先撤回核销"——无 pre-approve 守卫，改为 post-approve `ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` 静默反向，无 `ERR_RETURN_INVOICE_SETTLED`；②"退货期间已结账→拒绝"——无 `requirePeriodOpen`/`isPeriodClosed`（`ErpSalReturnProcessor.validateBusinessRulesForApprove:173-179` 无），无 `ERR_RETURN_PERIOD_CLOSED`（`ErpSalErrors.java:182-209` 无）。
  - **跨域 Facade**：`IErpInvStockMoveBiz`（generateMove/findByRelatedBill/reverse，`ErpSalReturnProcessor:59,289,297-298,308,314,319`）；`IErpFinVoucherBiz`（post/reverse，经 `SalPostingExecutor`）；`IErpMdPartnerBiz`/`IErpMdAcctSchemaBiz`（读）。

- **L4 测试证据现状**（`module-sales/erp-sal-service/src/test/`）：UC-SAL-09 状态机 `TestErpSalReturnApproval`（7 方法，状态/客户激活/源出库/原因守卫）；UC-SAL-09 数量 `TestErpSalReturnQty`（4 方法，12>10 拒、部分允、累积 5>3 拒、累积内允——**强**）；UC-SAL-04/05 库存 `TestErpSalReturnInventory`（行级余额 20+4=24、幂等、反向 24−4=20）；追溯 `TestErpSalReturnTrace`（originReturnedMoveId 双向）；UC-SAL-04 过账 `TestErpSalReturnPosting`（**行级凭证** totalDebit/totalCredit=20+2 行 + ArApItem.openAmount=−24 行级；反向取消）；UC-SAL-04 退款 `TestErpSalReturnRefund`（已收→反向核销负向 ReceiptLine+receivedStatus=UNRECEIVED 行级；未收→no-op）；UC-SAL-04 端到端 `TestErpSalReturnRefundEndToEnd`（全链+异常拒）。E2E `tests/e2e/business-actions/sal-return.action.spec.ts`（审核路径**行级凭证** 1401/6401 强；拒绝+取消状态级）。**缺口测试**：UC-SAL-05 暂估应收冲减（仅 no-op 测 `:117-138`）、UC-SAL-06 换货（路径不存在）、UC-SAL-07 成本策略切换（无）、UC-SAL-09 已核销发票 pre-approve 拒（无）、UC-SAL-09 期间 CLOSED 拒（无）。P1-MA4-021（SalReversalListener 3/4 回滚路径零覆盖 + STANDARD 红冲成本不变量零覆盖）**resolved（R2.14 done）**。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9）：§1.1 ErpSalReturn :25；维度3 终态；维度4(g) 退货退款 PASS（`ReturnRefundOrchestrator.orchestrateRefund` 闭环 + `SalReversalListener.rollbackReturn` 对称）；P2-MA2-058（`:316`，returnStatus/refundStatus/writtenOffStatus 非 ORM 存储，derived-view documented simplification）。裁决 ⚠️(P1)。
  - `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`：P2-MA2-011（arm-index:469，**显式登记红字发票缺口为 documented simplification**——impl 用 SALES_RETURN posting + 负向 ArApItem credit memo 替代红字 `ErpSalInvoice`，功能等价减 AR 余额但 GL 击成本/存货侧非收入/AR 侧）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：`ReturnRefundOrchestrator`/`ReceiptSettler`/`SalAcctDocProvider` 代码质量 PASS；P1-MA4-021（**resolved R2.14**）。
  - `docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`：`ReceiptSettler`/`reverseSettlement` 对称性。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（换货完全缺失 / 未交货量更新缺失 / 退货成本策略 1/3 + 配置键未声明 / 已核销发票 pre-approve 守卫缺失 / 期间 CLOSED 守卫缺失 / 暂估应收条件冲减缺失等）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P2-MA2-011`（红字发票 doc drift，documented simplification，watch-only MR1 owner-doc 更新）、`P2-MA2-058`（returnStatus/refundStatus/writtenOffStatus 非 ORM 存储，documented simplification）、`P2-MA2-057`（SalReversalListener asymmetry，watch-only）、`P1-MA4-021`（测试覆盖，**resolved R2.14**）、`P2-MA4-010(c)`（WithdrawApproval Processor 死代码，watch-only）、`P1-MA2-057`（INLINE withdraw 守卫，MR1，状态机非 F3 机制）、`P2-MA2-041`（finance reconciliation 无 CLOSED_FINAL 守卫，finance 侧非 sales 退货）。**RC 系列对 sales 为零**。本切片新发现的静默缺口（#3 未交货量更新 / #5 换货完全缺失 / #6 退货成本策略 1/3+配置键未声明 / #7 已核销发票 pre-approve 守卫缺失 / #8 期间 CLOSED 守卫缺失 / #4 暂估应收条件冲减）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及会计过账逻辑（红字发票/credit memo/退货成本）或 ORM 结构（补 returnType/returnStatus 列）的修复行须 ask-first（§5 保护区域暂停协议）。红字发票缺口（#1）已有 P2-MA2-011 documented simplification 记录——MA1 须复核该简化是否符合 §4 三判据（人工批准记录），若不符合则重新分级。

- **剩余差距**：A1.20 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.20 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.20 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-20-sales-f3-returns-family.md`，含方法论 §6 **9 段全部内容**。
- 对 5 UC（UC-SAL-04/05/06/07/09）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 红字发票 credit-memo 替代（复核 P2-MA2-011 documented simplification 是否满足 §4 三判据）、#2 无独立退款单、#3 未交货量更新缺失、#4 暂估应收条件冲减缺失、#5 换货完全缺失、#6 退货成本策略 1/3+配置键未声明、#7 已核销发票 pre-approve 守卫缺失、#8 期间 CLOSED 守卫缺失、#9 returnStatus/refundStatus 非 ORM（复核 P2-MA2-058）、#10 测试缺口——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。对既有 documented simplification（#1/#9）按 §4 复核人工批准证据，若不符合三判据则重新打开并入 MR1。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；既有行追加 RC 注记）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/returns.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.18/1.19/1.21 各自独立 plan；A1.20 只覆盖 UC-SAL-04/05/06/07/09）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：退货审核/退款闭环行为由 A2.9/O2C 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议，**§4 三判据为本切片复核 documented simplification 的关键**）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.20 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.20 UC 锚点）+ `docs/design/sales/use-cases.md`（L1 真相源）+ `docs/design/sales/returns.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalReturn*`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-20-sales-f3-returns-family.md`（新建，先填 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-SAL-04/05/06/07/09 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:99/:132/:149/:165/:200` 验收标准原文；L2 引用 `returns.md` 对应 section（§红字发票/§退款处理/§退货类型/§退货成本处理/§异常处理，标注"设计参考，冲突以 L1 为准"，注意 `:213-241` 红字发票 doc drift、`:88-93` returnStatus drift 注记）；L3 引用 `module-sales/.../processor/ErpSalReturnProcessor.java:<line>` / `ErpSalReturnApproveProcessor` / `ReturnStockMoveBuilder` / `SalReturnPostingDispatcher` / `ReturnRefundOrchestrator` / `ReceiptSettler.reverseSettlement` / `ReturnQtyValidator`（含跨域 `IErpInvStockMoveBiz`/`IErpFinVoucherBiz`）；L4 引用 `TestErpSalReturn*.java#method` / E2E spec（注明断言强度）；L5 复用 MA2 A2.9/O2C + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-SAL-04 库存恢复（可用量+=退货，已实现 `TestErpSalReturnInventory` 行级）；②#1 红字发票 credit-memo 替代（`SalReturnPostingDispatcher:84-103` SALES_RETURN posting + 负向 ArApItem，**非**红字 `ErpSalInvoice` 实体，GL 击成本/存货侧非收入/AR 侧——复核 P2-MA2-011 是否满足 §4 三判据）；③#2 无独立退款单（仅负向 ReceiptLine，`ReturnRefundOrchestrator:79-99`）；④UC-SAL-04 退款核销反向（已实现 `TestErpSalReturnRefund` 行级）；⑤#3 UC-SAL-05 未交货量更新缺失（无 undeliveredQty 字段/逻辑）；⑥#4 UC-SAL-05 暂估应收条件冲减缺失（`buildEvent:84-103` 无分支）；⑦#5 UC-SAL-06 换货完全缺失（无 returnType 列 `app-erp-sales.orm.xml:857-934`，无换货分支）；⑧#6 UC-SAL-07 退货成本策略 1/3（仅原出库成本 `ReturnStockMoveBuilder:65`，配置键 `erp-sal.return-cost-method` 未声明 `ErpSalConstants:74-76`）；⑨#7 UC-SAL-09 已核销发票 pre-approve 守卫缺失（无 `ERR_RETURN_INVOICE_SETTLED`，改 post-approve 静默反向 `reverseSettlementsForInvoice:79-99`）；⑩#8 UC-SAL-09 期间 CLOSED 守卫缺失（无 `requirePeriodOpen`/`ERR_RETURN_PERIOD_CLOSED`）；⑪UC-SAL-09 数量守卫已实现（`ReturnQtyValidator:46-66` 强）；⑫#9 returnStatus/refundStatus 非 ORM（复核 P2-MA2-058 三判据）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#5 换货完全缺失（UC-SAL-06 整个 UC 缺失）属"功能完全缺失"——倾向 P1（须人工确认是否为范围裁剪，若 product-scope 含换货则 P1 强制实现）；#6 退货成本策略 1/3（UC-SAL-07 功能实质偏离）属"行为实质偏离验收标准"——倾向 P1；#3 未交货量更新缺失（UC-SAL-05 派生断言不可满足）——倾向 P1；#7/#8 守卫缺失（UC-SAL-09 异常路径/期间控制未实现）——倾向 P1（期间控制涉会计正确性）；#1/#9 复核既有 documented simplification 是否满足 §4 三判据（人工批准记录证据标准），不满足则重新打开并入 MR1；#2/#4/#10 倾向 P2。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-SAL-04/05/06/07/09 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.9/O2C 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#10 有明确分级（非悬空"待查"）；#1/#9 documented simplification 复核结论已记录（满足/不满足 §4 三判据）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-20-sales-f3-returns-family.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` sales 退货同域同控制点（如 P2-MA2-011 红字发票、P2-MA2-058 returnStatus、P1-MA4-021 测试覆盖）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 列明差异依据。#3/#5/#6/#7/#8 为**未登记**缺口，须新建并 grep 比对。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）；记录 #1/#9 documented simplification 复核结论（满足三判据则维持 P2 watch-only；不满足则按 §4 重新打开为 P1 入 MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如负向 ArApItem credit memo 实际对客户应收余额的净效果、退货成本在不同库存策略下 CostLayer 实际取值、期间 CLOSED 下退货审核实际是否被拦截等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-sales-state-machine.md`（退货状态机 + 退款闭环 PASS）+ `2026-07-27-1949-...-order-to-cash-e2e.md`（P2-MA2-011 红字发票 doc drift）+ `2026-07-29-0430-...-code-quality.md`（代码质量 PASS + P1-MA4-021 resolved），列明只补的需求视角差异（换货缺失 / 未交货量缺失 / 成本策略 1/3 / 守卫缺失 / 暂估应收冲减缺失 / documented simplification 复核结论）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；既有行（P2-MA2-011/P2-MA2-058）追加 RC 复核注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据；#1/#9 复核结论已记录
- [ ] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03be5cf5bffetJmaRM2UoK4BNJ，fresh session，未起草本计划）。规则 1-13 全 PASS：(1) Deps A1.20=0.2 done；(2) 单结果表面（A1.20 报告 UC-SAL-04/05/06/07/09）；(3) 格式 + 命名合规（N=3 = F3 在 F1/F2 之后）；(4) UC 覆盖精确（baseline-inventory:354）；(5) Baseline 6/6 spot-check 全 PASS——`ErpSalReturn` ORM `app-erp-sales.orm.xml:857-934` 无 returnType/redInvoiceId/originalInvoiceId 列 / `SalReturnPostingDispatcher.java:87` 用 SALES_RETURN 非 红字 ErpSalInvoice / `ReturnRefundOrchestrator.orchestrateRefund:49-57` 经 ReceiptSettler 生成负向 ReceiptLine 无独立退款单 + restoreRefund:64-67 MVP no-op / `erp-sal.return-cost-method` 跨模块 grep 空（`ErpSalConstants:74-76` 仅 reason/approval-required）/ `ErpSalErrors` 无 ERR_RETURN_PERIOD_CLOSED/ERR_RETURN_INVOICE_SETTLED / `ReturnQtyValidator:46-66` qty 守卫 + ERR_RETURN_QTY_EXCEED 存在；arm-index P2-MA2-011(:469)/P2-MA2-058(:507)/P1-MA4-021(:601) 核验存在且特征正确；(6) 方法论 §1-§10 + §4 三判据 + §7 + §8 + §9 + §去重（A2.9/O2C）对齐；(7) 反松弛；(8) typing；(9) Closure Gates audit-only 有据 + 含 §4 复核；(10) Non-Goals 守约；(11) **Q4 vs documented simplification 正确**——P2-MA2-011/P2-MA2-058 §4 三判据复核 wired 进 Phase 1 Decision(:90)+Phase 2 Add(:109)+两段 exit，双向逻辑（满足则维持 P2 watch-only；不满足则 §4 重新打开为 P1 入 MR1），不静默接受；(12) **UC-SAL-06 换货不自决**——Phase 1(:90)+Deferred(:150) 均须人工确认是否 product-scope 范围裁剪（裁剪→§4 出口 iii 改真相源非降级；未裁剪→P1 强制实现 Q4）；(13) #5 换货属功能完全缺失（§2 P1①）出口仅 §5 product-scope 修订（非 §4 三判据，§4 仅适用于方案 B 关闭），正确区分。无阻塞。Non-blocking（已评估，无需修订）：①"倾向 P1"措辞立即被"须人工确认范围裁剪"限定，不预判；②"RC 系列对 sales 为零"指零已完成 RC 审计报告（A1.18/19 同批存在但未完成），意图清晰；③Phase 1 prereqs "M0.1+M0.2" 比 roadmap Deps "0.2" 更严但正确（0.2 传递依赖 0.1）。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.20 报告 9 段齐全 + 5 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议 + §4 三判据一致；与 rc-requirement-baseline-inventory A1.20 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯 + documented simplification 复核结论可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及会计过账逻辑（红字发票/credit memo/退货成本）或 ORM 结构（补 returnType/returnStatus 列）的修复行须 ask-first + 独立 plan-audit（§5）。#5 换货缺失须人工确认是否为 product-scope 范围裁剪（若裁剪则改真相源非降级；若未裁剪则 P1 强制实现）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；#5 待人工确认 product-scope 范围）
