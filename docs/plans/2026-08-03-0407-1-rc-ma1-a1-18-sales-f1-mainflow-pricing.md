# 2026-08-03-0407-1 rc-ma1-a1-18-sales-f1-mainflow-pricing sales-F1 主流程与价格需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.18（MA1 需求追踪矩阵审计 — sales-F1 主流程与价格）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.18
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.18 的 0.2 依赖）、`2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-reconciliation.md`（同 MA1 审计范式）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，过账链路范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.18 给出 UC 清单 = `UC-SAL-01/11`（2 UC），含 `use-cases.md:20` / `:231` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/sales/use-cases.md`：
  - UC-SAL-01 标准销售全流程（`:20`）：订单审核通过（触发可用量校验 + 出库移动单生成）→ 出库单审核通过（扣减库存：可用量/现有量 -= 出库明细数量之和）→ 发票审核通过（生成应收凭证：业务类型 == SALES_DELIVERY（存货估值红冲）且来源 == 出库单；业务类型 == AR_INVOICE 且来源 == 发票；两者已过账 == true）→ 收款单审核通过核销发票（发票收款状态 未收→部分/已收清；客户应收余额 == 发票金额 - 已核销金额）。回链断言：出库单.来源单号==订单.单号；发票行.来源单号==出库单.单号。
  - UC-SAL-11 销售价格管理（`:231`）：`ErpSalPriceList`（头/行）+ `ErpSalPricingRule` + `ErpSalCustomerPriceResolver`（`IErpMdCustomerPriceResolver` SPI）+ `ErpSalPricingRuleEngine`（纯函数式）+ `ErpSalOrderBizModel.applyPricingRules` + 行级折扣字段（`discountRate`/`discountAmount`/`pricingSource`）+ `ErpMdPartner.customerGroup`。断言：价格清单（客户组/物料/数量阶梯/生效区间）；取价优先级 手工价 > 价格清单（匹配客户组/物料/阶梯）> SKU 默认档；促销规则（买赠/满减/折扣）；促销与价格清单可叠加；最低价校验（最终售价 < SKU.minPrice → 按配置拒绝/警告，见 UC-MD-04）；价税分离（折扣后金额 = 原金额 - 促销优惠；税额 = 折扣后金额 / (1+税率) × 税率）。

- **L3 代码实现现状（实测）**——功能**已大量实现**（非 stub），但存在与 L1 的结构性偏离：
  - **订单审核**（`ErpSalOrder`）：`ErpSalOrderApproveProcessor.java:48-62` → `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` **仅校验客户激活 + 信用额度**（`requireCustomerActive` + `creditLimitChecker.check`）；afterStateChange 钩子 `runCommitmentCommitHook`/`runIntercompanyApproveHook`（均 config-gated 默认关）。**注意（疑似缺口 G1）**：UC-SAL-01 `:27` 要求订单审核"触发可用量校验 + 出库移动单生成"，但订单审核**不调用任何库存 Facade**（`@Inject` 簇 `ErpSalOrderProcessor.java:54-67` 无 `IErpInvStockMoveBiz`/`IErpInvStockBalanceBiz`）；`TestErpSalOrderApproval.java:34-35` Javadoc 明示"仅状态推进，不触发库存/凭证"；MA2 `2026-07-28-0400-arm-ma2-sales-state-machine.md:21` 同证。可用量校验实际在**出库审核**环节执行（见下）。
  - **出库审核**（`ErpSalDelivery`）：`ErpSalDeliveryApproveProcessor.java:37` → `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove`（跨域 Facade）→ 库存域 `ErpInvStockMoveProcessor.doConfirm:86-98` → `validateAvailable:116-136`（不足抛 `ERR_AVAILABLE_INSUFFICIENT`，`@BizMutation` 回滚，出库单保持 SUBMITTED）。库存扣减 + 过账由库存域 `generateMove`（businessLinked=true）完成。**注意（疑似缺口 G2）**：UC-SAL-01 `:43` 要求凭证业务类型 == `SALES_DELIVERY`，但 `ErpFinBusinessType.java` **无 `SALES_DELIVERY` 常量**；出库过账由库存域 `InvAcctDocProvider` 用 `SALES_OUTPUT(20)`（Dr 6401 主营业务成本 / Cr 1401 库存商品）实现——功能等价（存货估值红冲），命名/责任层偏离。
  - **发票审核**（`ErpSalInvoice`）：`ErpSalInvoiceApproveProcessor.java:35` → `SalInvoicePostingDispatcher.tryPost:39-73`（businessType=`AR_INVOICE`，billData 含 TOTAL_AMOUNT/TOTAL_TAX_AMOUNT/TOTAL_AMOUNT_WITH_TAX/CUSTOMER_ID）→ `SalAcctDocProvider.createFacts:73-93`（Dr 1131 应收 = TOTAL_WITH_TAX；Cr 6001 主营业务收入 = TOTAL；Cr 2221 销项税 = TOTAL_TAX）。红冲 `reverse:58` 硬前置。
  - **收款 + 核销**（`ErpSalReceipt`）：审核 `ErpSalReceiptApproveProcessor.java` → `SalReceiptPostingDispatcher.tryPost`（`RECEIPT`，Dr 1002 / Cr 1131）；核销 `ErpSalReceiptSettleProcessor.java` → `ReceiptSettler.settle:55-111`（校验 approveStatus=APPROVED / amount ≤ 发票余额 / amount ≤ 收款余量），写 `ErpSalReceiptLine`，`recomputeInvoiceReceived:161-177` 更新 `ErpSalInvoice.receivedAmount`/`receivedStatus`。**注意（疑似缺口 G6）**：UC-SAL-01 `:49` 断言"客户应收余额 == 发票金额 - 已核销金额"——sales 域 `ReceiptSettler` **不直接更新 `ErpMdPartner.receivableBalance`**；客户应收余额由 finance `PartnerBalanceUpdater.setReceivableBalance` 经 `ErpFinArApItem`（RECEIVABLE + AR_INVOICE）层更新（`TestErpSalOrderToCashEnd.java:333-336` 文档化双层设计）。
  - **价格**（UC-SAL-11）：ORM `ErpSalPriceList`（`app-erp-sales.orm.xml:1000`）/ `ErpSalPriceListLine`（`:1053`）/ `ErpSalPricingRule`（`:1106`）齐全；`ErpSalCustomerPriceResolver.java:40-176`（SPI 实现，按 partnerId/customerGroup/isActive/期间/币种/阶梯匹配，priority 小者胜）；`ErpSalPricingRuleEngine.java:35-114`（纯函数，PERCENT_DISCOUNT/AMOUNT_OFF/GIFT/PRICE_OVERRIDE × LINE/ORDER，stackable/priority/期间/客户组过滤）；`ErpSalOrderBizModel.applyPricingRules:96-114`（加载规则、解析客户组、调引擎、`persistPricingResult:143` 重算行金额与订单合计）。**注意（疑似缺口 G3/G4/G5）**：`applyPricingRules` **仅应用促销层**；取价优先级链（手工 > 价格清单 > SKU 默认）在 master-data `ErpMdMaterialSkuBizModel` 实现（非 sales 层）；`applyPricingRules` **不调用最低价校验**（UC-MD-04，`ErpMdMaterialSkuBizModel.java:189-202` 独立存在）；`recomputeLineAmount:172-179` 仅 `amount = gross − discountAmount`，**不按公式重算 taxAmount**（UC-SAL-11 `:253-255`）。

- **L4 测试证据现状**（`module-sales/erp-sal-service/src/test/`）：订单审核 `TestErpSalOrderApproval`（14 方法，状态机 + 信用强断言，**无可用量校验测试**）；出库端到端 `TestErpSalOrderToDeliveryEnd`（库存 20→10、posted=true、SALES_OUTPUT 凭证存在——**仅合计非行级 Dr/Cr**）；发票过账 `TestErpSalInvoicePosting`（AR 凭证 totalDebit/totalCredit=113 + countLines=3——**仅合计+计数**）；O2C 全链 `TestErpSalOrderToCashEnd`（状态/posted/receivedStatus 强；AR 凭证**仅 totalDebit=113**）；价格引擎单测 `TestErpSalPricingRuleEngine`（10 方法，强——discountRate/amount/pricingSource/赠品/stackable/priority/期间/客户组）；价格端到端 `TestErpSalPricingEndToEnd`（7 场景——**仅断言 status==0 冒烟，不校验行级折扣/赠品/合计重算**，缺口 G8）。E2E：`tests/e2e/orchestration/o2c-chain.spec.ts`（**行级凭证断言** SALES_OUTPUT `{6401 DEBIT 1200}/{1401 CREDIT 1200}`、AR_INVOICE `{1131 DEBIT 113}/{6001 CREDIT 100}/{2221 CREDIT 13}` + AR 辅助项 direction/openAmount/status）+ `o2c-reverse.spec.ts`（红冲行级负金额）。MA5 `2026-07-29-1430-arm-ma5-e2e-effectiveness.md:51,138` 评 orchestration E2E 为 strong。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（O2C 全链主证据）：§1 链路矩阵 :16-29；§2.1 可用量+扣减 PASS :39-46；§2.2 信用额度 PASS :53-65；§2.3 AR openAmount 生命周期 PASS :70-77；§2.4 退货红冲 PASS（含 P2）:83-93；§2.5 多币种 O2C **FAIL P1-MA2-009** :95-112；§2.6 收入/成本时点 PASS（含 P2）。
  - `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（7 实体 × 三轴状态机）：§1.1 :20-27、§2.1 PROC vs INLINE :46-54。裁决 ⚠️(P1)：P1-MA2-056/057 + P2-MA2-056/057/058。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5 sales 采样）：6 Processor + SalAcctDocProvider + 3 PostingDispatcher + DeliveryStockMoveBuilder + ReceiptSettler；裁决 ⚠️(P1)；P1-MA4-021/022。
  - `docs/audits/2026-07-29-0749-arm-ma4-pur-sal-inv-view-xml-drift.md`（A4.7）：sales 域**零漂移**。
  - **陈旧/失实证据**：`docs/audits/2026-07-06-use-case-implementation-audit.md:71-86` 部分 UC 标 ✅ 建立在早期引用上，须重新核验。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（订单级可用量校验缺失命名/责任层偏离 / 价格最低价校验缺失 / 价税分离缺失等）。

- **arm-index 既有 finding 衔接**：sales 相关既有 finding（`P1-MA2-009` 多币种 O2C / `P1-MA2-056` Contract reverseApprove / `P1-MA2-057` INLINE withdraw 守卫 / `P2-MA2-010` 发票>订单金额 / `P2-MA2-011` 红字发票 doc drift / `P2-MA2-012` 信用控制 doc drift / `P2-MA2-013` 收款核销订单维度 / `P2-MA2-014` ReceiptSettler 并发 / `P2-MA2-015` 跨月收入成本 / `P2-MA2-056/057/058` sales doc drift / `P2-MA2-073` 承付测试缺 Dr/Cr / `P1-MA1-022` 跨域 daoFor）。**RC 系列（`P*-RC-*`）对 sales 为零**——A1.18 将是 sales 域首个 RC 切片。本切片新发现的静默缺口（G1 订单级可用量校验缺失 / G3 最低价校验缺失 / G4 价税分离缺失 / G5 取价优先级链 / G8 价格端到端冒烟）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及会计过账逻辑（如 SALES_DELIVERY/AR_INVOICE 凭证生成）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.18 切片的五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.18 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.18 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`，含方法论 §6 **9 段全部内容**：①UC-SAL-01/11 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，调用链列全）③测试证据（注明断言强度）④运行时行为证据（复用 MA2/E2E，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：禁止 UC 跳号、禁止验收标准抽样、禁止跨 UC 合并行；UC-SAL-01/11 各一矩阵行。
- 对候选缺口给出分级结论：G1 订单级可用量校验缺失、G2 SALES_DELIVERY 命名/责任层偏离、G3 最低价校验缺失、G4 价税分离缺失、G5 取价优先级链、G6 客户应收余额双层更新、G7 JUnit 凭证仅合计、G8 价格端到端冒烟——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / sales use-cases / owner doc 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.19-A1.21 sales-F2/F3/F4 各自独立 plan；A1.18 只覆盖 UC-SAL-01/11）。
- **不执行 MA4 运行时探针展开**（A4.1 展开器读取本报告静态存疑点清单后追加 A4.1.n 实体行；本计划只产出存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：MA2 已证实行为直接引用，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.18 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.18 UC 锚点）+ `docs/design/sales/use-cases.md`（L1 真相源）+ `docs/design/sales/state-machine.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4/MA5 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用既有 MA2 报告 + E2E recordings（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（如 `mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalOrderToCashEnd,TestErpSalPricing*`）或读 E2E 录制，不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`（新建，先填 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-SAL-01/11 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:20/:231` 验收标准原文（禁止转述）；L2 引用 `state-machine.md` 对应 section（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-sales/erp-sal-service/.../processor/<BizModel|Processor>.java:<line>`（订单/出库/发票/收款/核销/价格 调用链列全，含跨域 `IErpInvStockMoveBiz`/`IErpFinVoucherBiz`/`IErpMdPartnerBiz`）；L4 引用 `Test*.java#method` / E2E spec（注明断言强度，引用 MA5 评级）；L5 复用 MA2/E2E 已证实行为 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①G1 订单审核不触发可用量校验/出库移动单（UC-SAL-01 `:27`，实际挪至出库审核 `ErpSalDeliveryProcessor.triggerOutgoingMove:241`）；②G2 无 `SALES_DELIVERY` 业务类型（`ErpFinBusinessType.java` 仅 `SALES_OUTPUT(20)`，库存域 InvAcctDocProvider 实现，命名/责任层偏离）；③AR_INVOICE 凭证 facts（Dr 1131/Cr 6001/Cr 2221，`SalAcctDocProvider:73-93`）；④回链断言（出库.来源单号==订单 / 发票行.来源==出库单，`DeliveryStockMoveBuilder`/`ErpSalInvoiceLine.deliveryLineId`）；⑤G6 客户应收余额由 finance `PartnerBalanceUpdater` 双层更新（非 sales ReceiptSettler 直写）；⑥G3 最低价校验缺失（`applyPricingRules` 不调 UC-MD-04）；⑦G4 价税分离缺失（`recomputeLineAmount:172-179` 不重算 taxAmount）；⑧G5 取价优先级链（手工>清单>SKU）在 master-data 实现，sales 层仅 audit 日志；⑨价格清单/促销规则/stackable/priority/期间/客户组（`ErpSalCustomerPriceResolver`/`ErpSalPricingRuleEngine` 已实现，单测强）；⑩G8 价格端到端仅 status==0 冒烟。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受，取最高）：候选缺口 G1（核心业务循环/异常路径）、G3/G4（功能缺失/价税正确性）、G5（行为偏离）属"功能缺失/异常路径未实现"——若确认为 P0/P1 则定级并触发 §10（本计划仅登记）；G2（命名/责任层偏离，功能等价经 E2E 行级证据）倾向 P2；G6（双层设计已文档化）倾向接受/P2；G7/G8（断言强度）倾向 P2；既有 P1-MA2-009（多币种 O2C）须裁决复用 or 增量。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-SAL-01/11 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 MA2 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 G1-G8 有明确分级（非悬空"待查"）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` sales 同域同控制点（如 P1-MA2-009 多币种、P2-MA2-010/011/013/073 等）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如多币种 O2C 实际过账金额/汇兑损益、价格优先级链运行时取值顺序等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（O2C 链路/可用量/AR 生命周期）+ `2026-07-28-0400-arm-ma2-sales-state-machine.md`（状态机）等已证实行为，列明本切片只补的需求视角差异（订单级可用量校验缺失 / SALES_DELIVERY 命名偏离 / 价格最低价+价税分离缺失 / 价格端到端冒烟等）。
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

- Independent draft review iteration 1: `accept`（独立子代理 ses_03be611ffffeJRSM0mUJclPYDt，fresh session，未起草本计划）。规则 1-10 全 PASS：(1) Deps A1.18=0.2 done；(2) 单结果表面（A1.18 报告，无跨切片合并，§3 合规）；(3) 格式完整 + 命名合规；(4) UC 覆盖 UC-SAL-01/11 精确（rc-baseline-inventory:352,433 `✅ 一致`）；(5) Baseline 4 项强制 spot-check 全 PASS——`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅客户+信用无库存 Facade / `ErpFinBusinessType.java:13-69` 无 SALES_DELIVERY 常量仅 SALES_OUTPUT(20)+SALES_RETURN(150) / `SalAcctDocProvider.createFacts:73-94` Dr 1131/Cr 6001/Cr 2221 / `ErpSalOrderBizModel.applyPricingRules:96-114` 不调最低价 + `recomputeLineAmount:172-179` 不重算 taxAmount；UC 锚点 use-cases.md:20/27/43/49/231/253-255 核验一致；(6) 方法论 §1-§10 + §去重 + §7 复用or新增 + §8 自检 + §9 冻结 + §10 MR0/MR1 对齐；(7) 反松弛合规；(8) item typing 合规；(9) Closure Gates audit-only 删除 build/test 有据（§8 reporter-not-gate）；(10) Non-Goals 守约。无阻塞。Non-blocking（已评估，无需修订）：①基础设施段"若需…可跑"为诊断工具描述非范围交付物，可保留；②`createFacts` 行区间 `:73-93` vs 实际 `:73-94`（闭括号 off-by-one，与 A1.4 sibling 同类微漂移，无影响）。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。（§8 含 checker 实测记录，但 checker 是 reporter 非门控；门控真值在 CI workflow。）

- [x] 范围内行为完成：A1.18 报告 9 段齐全 + 2 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.18 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（如 SALES_DELIVERY/AR_INVOICE 凭证生成）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: A1.18 sales-F1 主流程与价格需求符合性审计已闭环。审计报告 9 段齐全落盘（`docs/audits/2026-08-03-0430-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`，68KB），2 UC（UC-SAL-01/11）逐条验收标准五级追踪完成，候选缺口 G1-G8 全部裁决定级（G1→P1-RC-020 / G2→P2-RC-016 / G3→P1-RC-021 / G4→P1-RC-022 / G7→P2-RC-017 / G5/G6/G8 经裁决接受或并入既有证据，详见报告 §5/§6），5 条新 RC finding 已登记入 `docs/audits/arm-index.md` 对应分区，无 P0 即时通道触发，无范围内项目降级为 deferred/follow-up。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-audit fresh session，非执行者会话；本会话由 mission-driver `2026-08-02-204249-mission-driver` 调度）
- 证据 1（结果表面存在 + 完整性）: `docs/audits/2026-08-03-0430-rc-ma1-a1-18-sales-f1-mainflow-pricing.md` 实仓存在（68KB）；报告含 §0 TL;DR + §1-§9（9 段）+ §10 Verdict 共 12 个块级标题，覆盖方法论 §6 全部 9 段要求（L1 逐字 / L3 file:line / L4 断言强度 / L5 复用声明 / 五级矩阵 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / MA2 差异增量）。
- 证据 2（arm-index 衔接落地）: `docs/audits/arm-index.md` 已写入 5 条 sales/A1.18 新 RC finding——`P1-RC-020`（UC-SAL-01 ① 订单级可用量校验缺失）、`P1-RC-021`（UC-SAL-11 ⑥ 最低价校验缺失）、`P1-RC-022`（UC-SAL-11 ⑦ 价税分离缺失）、`P2-RC-016`（UC-SAL-01 ⑤ SALES_DELIVERY 命名漂移）、`P2-RC-017`（UC-SAL-01 ⑥ JUnit AR 凭证仅合计+计数）；每条均含 §2 判据编号、MR1 修复通道、todo 修复行 + §5 ask-first 评估。
- 证据 3（只读审计守约）: 本审计无代码/ORM/api.xml/view.xml/真相源变更（git status 仅计划文件本次审计编辑），符合 Non-Goals "不修改真相源 / 不修改代码"。
- 证据 4（独立草案审查已记录）: Draft Review Record iteration 1 = `accept`（独立子代理 `ses_03be611ffffeJRSM0mUJclPYDt` fresh session，规则 1-10 全 PASS，Baseline 4 项 spot-check 全 PASS）。
- 证据 5（文本一致性）: Plan Status = completed / Phase 1 Status = completed / Phase 2 Status = completed / 两 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Deferred But Adjudicated 有明确 successor（MR0/MR1）。
- 语义核验: 结束审计子代理已读取实时报告 + arm-index finding 行 + 计划全文，确认报告 9 段齐全、finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）、G1-G8 候选缺口全部有明确分级（非悬空"待查"）、§8 过程纪律自检段含 checker actual vs baseline 实测记录 + 独立性声明 + 交叉去重声明；无 P0 触发 MR0 即时通道；finding 的修复实施按 Deferred But Adjudicated 显式移交 MR1（R1.0 展开 RC-R1.n），符合 §10 与 §14（不可降级项目规则）——finding 本身非"已确认缺陷被静默降级"，而是"审计结果表面的固有移交"。

Follow-up:

- MR1（R1.0）按本报告 P1-RC-020/021/022 三条 P1 finding 展开 RC-R1.n 修复行（详见 arm-index todo 列）；P2-RC-016/017 为 successor watch-only 不强制。
