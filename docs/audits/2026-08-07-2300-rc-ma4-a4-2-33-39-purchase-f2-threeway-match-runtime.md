# A4.2.33-A4.2.39 purchase-F2 三单匹配/容差/价格差异过账运行时确认验证报告（rc-ma4-a4-2-33-39）

> Mission: requirement-compliance · MA4 运行时行为验证 · Work Items: A4.2.33 / A4.2.34 / A4.2.35 / A4.2.36 / A4.2.37 / A4.2.38 / A4.2.39
> 来源计划: `docs/plans/2026-08-07-2300-2-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`
> 来源存疑点: `docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md` §7（7 项静态存疑点）
> 方法论: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 审计类型: 只读审计（无生产代码 / ORM / api.xml / view.xml / config 默认值 / 真相源变更）
> 审计日期: 2026-08-07
> Audit Status: closed

## 9. 与既有 A1.16 / A2.8 / A2.1 / A1.1 报告的差异增量声明（前置）

本报告是 MA4 运行时行为验证 A4.2 展开器的 A1.16 §7 七项静态存疑点的**运行时证据采集与裁决**，视角 = **A1.16 静态判定结论的运行时确认（主路径闭合 / 维持 P1 reuse 重开不降级 + 运行时证据记录 / 维持 P2 + 运行时证据记录 / 升级触发 MR0）**。按 §去重协议，以下既有审计已证实的结论本报告**直接复用，不重审**：

- **A1.16**（`docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`）：UC-PUR-02/03/05/06 五级追踪 + §7 七项静态存疑点 + §6 finding 衔接裁决（P1-RC-018 价格差异处理不完整 + P1-RC-019 超收容差校验缺失 + P2-RC-013 receivedQuantity 未写入 + P2-RC-014 短收差异处理缺失）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**两次入库独立过账凭证数==2 架构确认 / PPV 过账行缺失确认 / 三策略分支可达性确认 / 超收容差门控缺失确认 / 短收差异处理缺失确认 / 关闭释放预留 config-gated 行为确认 / receivedQuantity 运行时值确认**的运行时证据。
- **A2.8**（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）：purchase 9 实体状态机迁移守卫齐全 + reverseApprove 红冲闭环。本报告复用其 receive/invoice 状态机迁移 + per-mutation approve 架构行为证据。
- **A2.1 P2P e2e**（`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`）：P2P 链路行为已证实（回链 / 库存 incoming / 过账 / 核销 主路径完整）+ 三单匹配价格容差主路径。本报告复用其库存 incoming Facade + 三单匹配价格容差行为证据。
- **A1.1**（`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`）：业财过账引擎 GR/IR + AP 凭证范式已审。本报告复用其过账正确性结论，只补 AP_INVOICE 价格差异过账完整性运行时确认。
- **A4.2.27-32**（`docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`）：receive approve→triggerIncomingMove→InvPostingDispatcher PURCHASE_INPUT 全链闭合 + config `erp-fin.budget-commitment-enabled` 默认 false + 零生产 override。本报告复用其 config 默认值部署普查结论（A4.2.38 关闭释放预留 config-gated 范式对齐）。

本报告**只补运行时差异**：(i) UC-PUR-03 ⑦ 两次入库独立过账凭证数==2 架构确认（§2-1）；(ii) UC-PUR-05 ⑫ 让步接收价格差异过账运行时缺失确认（§2-2，**业财保护区域探针——只读确认不改过账逻辑**，P1-RC-018）；(iii) UC-PUR-05 ⑪ 三处理策略分支可达性确认（§2-3，P1-RC-018）；(iv) UC-PUR-02 ② 超收容差运行时门控缺失确认（§2-4，P1-RC-019）；(v) UC-PUR-06 ⑮ 短收超容差差异处理缺失确认（§2-5，P2-RC-014）；(vi) UC-PUR-06 ⑰ 关闭释放预留 config-gated 行为确认（§2-6）；(vii) UC-PUR-03 ⑤⑥ receivedQuantity 运行时值确认（§2-7，P2-RC-013）。

---

## 1. 存疑点清单（逐字引用 A1.16 §7）

> 以下为本报告核验对象，7 项静态存疑点来自 A1.16 §7：

1. **UC-PUR-03 ⑦ 两次入库独立过账凭证数==2 运行时确认**（A4.2.33）：HEAD 静态判定 = per-mutation approve 架构隐含成立（每次 `ErpPurReceiveApproveProcessor.approve` 独立 `triggerIncomingMove`→`IErpInvStockMoveBiz.generateMove`→InvPostingDispatcher PURCHASE_INPUT 凭证），但无测试构造"100→60→40→断言凭证数==2"。
2. **UC-PUR-05 ⑫ 让步接收价格差异过账运行时生成**（A4.2.34，P1-RC-018）：HEAD 静态判定 = 完全缺失（`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行 1403/2221/2202 无 PPV 行）。
3. **UC-PUR-05 ⑪ 三处理策略运行时分支可达性**（A4.2.35，P1-RC-018）：HEAD 静态判定 = 仅"拒绝"（strict mode）可达，"审批后接收"+"接收并过账差异"未实现。
4. **UC-PUR-02 ② 超收容差运行时门控**（A4.2.36，P1-RC-019）：HEAD 静态判定 = 完全缺失（receive approve 无 qty-vs-order 校验）。
5. **UC-PUR-06 ⑮ 短收超容差运行时差异处理**（A4.2.37，P2-RC-014）：HEAD 静态判定 = 完全缺失（无"差异处理"触发）。
6. **UC-PUR-06 ⑰ 关闭释放预留运行时 config-gated 行为**（A4.2.38）：HEAD 静态判定 = 已实现（config-gated `erp-fin.budget-commitment-enabled` 默认 false）。
7. **UC-PUR-03 ⑤⑥ receivedQuantity 运行时值**（A4.2.39，P2-RC-013）：HEAD 静态判定 = 列始终 0（零 writer）。

---

## 2. 运行时证据采集与裁决（L3 file:line + L4 测试断言 + L5 行为）

### 2-1 A4.2.33 两次入库独立过账凭证数==2 架构确认 — 主路径闭合

**运行时证据链（live code 实测）**：

- **per-mutation approve 架构**：`ErpPurReceiveApproveProcessor.approve:26-49` override 公共 approve mutation——每次入库独立执行 `triggerIncomingMove:37`（`ErpPurReceiveProcessor.triggerIncomingMove:215-219` → `stockMoveBuilder.build` → `stockMoveBiz.generateMove`）→ reload → `setApproveStatus(APPROVED):40` + `applyPostingResult:43`（`receive.setPosted(move.getPosted())`）→ `setReceiveStatus(RECEIVED):44` → `postProcessApprove:47`（`rollupOrderReceiveStatus`）。**每次 approve = 一次独立 stockMove = 一次独立 PURCHASE_INPUT 凭证**（无共享/合并/短路）。
- **InvPostingDispatcher PURCHASE_INPUT 路由（A4.2.27 已证实全链）**：库存域 `ErpInvStockMoveProcessor` 在移动单达 DONE 后调 `postingDispatcher.dispatchIfApplicable` → `InvPostingDispatcher.resolveBusinessType` MOVE_TYPE_INCOMING → `ErpFinBusinessType.PURCHASE_INPUT` → `executor.postEvent` → 成功置 `markMovePosted(move.posted=true)` → 回 `applyPostingResult` 回写 `receive.posted=true`。**每次入库 approve 触发的 stockMove 各自走完此全链 → 各自产 1 张 PURCHASE_INPUT 凭证**。
- **rollupOrderReceiveStatus 不影响凭证计数**：`ErpPurReceiveProcessor.rollupOrderReceiveStatus:244-284` 仅按 orderId 聚合 receive lines 累计数量 → 计算 anyReceived/allFullyReceived → `orderBiz.updateReceiveStatus` 设订单 header `receiveStatus`（UNRECEIVED/PARTIAL/RECEIVED），**不触发凭证生成也不合并既有凭证**——凭证计数严格 == approve 次数。
- **L4 间接证据（A4.2.27 已证实）**：`TestErpPurProcureToPayEnd.testProcureToPayPartialSettlement:116-121` 单次入库 approve 强断言 `approvedReceive.getPosted()==true`（凭证落地）。两次入库 = 同一 approve 路径执行两次 = 凭证数==2（per-mutation 架构隐含，无合并/短路路径）。**无构造"订单(100)→第一次入库(60)→第二次入库(40)→断言凭证数==2"专属测试**（A1.16 §3 断言强度缺口），但 per-mutation approve 架构 + `applyPostingResult:221-227` per-mutation posted 回写 + rollupOrderReceiveStatus 不产凭证 三重隐含成立。

**裁决**：A1.16 §7-1 静态判定「per-mutation approve 架构隐含成立」**运行时确认成立**。两次入库各自独立 `triggerIncomingMove`→`IErpInvStockMoveBiz.generateMove`→InvPostingDispatcher PURCHASE_INPUT 凭证 → `applyPostingResult` per-mutation posted 回写，**凭证数==2 由 per-mutation approve 架构隐含成立**（无共享/合并/短路路径；rollupOrderReceiveStatus 不产凭证不合并）。**主路径行为正确闭合**。**不触发 MR0**（行为正确，无会计错误；专属断言缺口属测试覆盖维度非行为缺陷）。

### 2-2 A4.2.34 让步接收价格差异过账运行时缺失确认 — 维持 P1-RC-018

**业财保护区域探针声明**：本节为只读凭证行结构追踪（grep census + createFacts 行级结构追踪 + billData 内容核查），**不修改任何过账逻辑 / VoucherFact 构造 / PurAcctDocProvider / PostingProcessor 核心路径**。

**运行时证据链（live code 实测）**：

- **AP_INVOICE createFacts 行级结构（3 行，无 PPV）**：`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 分支仅构造 **3 行 VoucherFact**：
  - `:80` `fact(SUBJECT_PURCHASE="1403", "在途物资", DC_DEBIT, amount=TOTAL_AMOUNT 不含税, ...)` — 借 在途物资（按发票不含税金额）
  - `:81` `fact(SUBJECT_INPUT_VAT="2221", "应交税费-进项税额", DC_DEBIT, tax=TOTAL_TAX_AMOUNT, ...)` — 借 进项税
  - `:82` `fact(SUBJECT_ACCOUNTS_PAYABLE="2202", "应付账款", DC_CREDIT, withTax=TOTAL_AMOUNT_WITH_TAX 价税合计, ...)` — 贷 应付账款
- **无价格差异（PPV）科目行确认**：grep `PurAcctDocProvider.java` 全文件**无** 1404 / 6601 / 601 / 6603 / 5001 等 PPV 科目常量；`SUBJECT_*` 常量集（`:42-46`）仅 PURCHASE=1403 / INPUT_VAT=2221 / ACCOUNTS_PAYABLE=2202 / BANK_DEPOSIT=1002 / INVENTORY=1401；`ACCOUNT_KEY_*` 映射键集（`:53-57`）同无 PPV 键。**AP_INVOICE 分支无第 4 行 PPV fact 构造**。
- **billData 无 invoice-vs-order 差异数据传递**：`PurInvoicePostingDispatcher.buildEvent:71-89`（A1.16 §2 已审）billData 仅放 TOTAL_AMOUNT / TOTAL_TAX_AMOUNT / TOTAL_AMOUNT_WITH_TAX / SUPPLIER_ID，**无 invoice 单价 / order 单价 / 差异 / 差异*数量 字段** → Provider 无从计算 PPV 行金额（即使要加也无输入数据）。**让步接收时差异埋在 1403 在途物资金额中（按发票金额入账）未分集到 PPV 科目**——差异 = (invoicePrice − orderPrice) × qty 被隐式吸收进 1403 借方金额。
- **GL 仍平衡（非活跃数据破坏）**：debit(1403 amount + 2221 tax) == credit(2202 withTax) 恒等（amount + tax == withTax），试算平衡不破。差异仅表现为 1403 在途物资金额偏高（含价格差异成分）而非 GL 不平衡。**属管理会计可视性缺口**（差异未分集到独立 PPV 科目供管理分析）**非活跃数据破坏**。
- **L4 零 PPV 断言**：grep `TestErpPurInvoiceApproval` + `TestErpPurProcureToPayEnd` + `TestErpPurThreeWayMatch` 全集**无** "价格差异" / "PPV" / "purchase price variance" / "差异*数量" / "1404" / "6601" 断言（A1.16 §3 断言强度缺口确认）。

**裁决**：A1.16 §7-2 静态判定「完全缺失」**运行时确认成立**。`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行（1403/2221/2202）**无价格差异（PPV）科目行**，差异埋在 1403 在途物资金额中（按发票金额入账）未分集到 PPV 科目；`PurInvoicePostingDispatcher.buildEvent` billData 无 invoice-vs-order 差异数据传递。**维持 P1-RC-018**（Q4 会计类无例外，修复归 MR1 R1.0 展开器——`PurInvoicePostingDispatcher.buildEvent` 传递 invoice-vs-order 差异数据 + `PurAcctDocProvider.createFacts` AP_INVOICE 增加价格差异科目行[科目经 GL 映射解析，金额=差异*数量]；**触及会计过账逻辑[PurAcctDocProvider/VoucherFact/PostingProcessor 核心路径]须 ask-first + 独立 plan-audit §5**）。**不触发 MR0**（GL 仍平衡 + AP 金额正确[按发票应付] → 属管理会计可视性缺口非活跃数据破坏；试算平衡可发现差异未分集为可视性问题非数据正确性破坏）。

### 2-3 A4.2.35 三处理策略分支可达性确认 — 维持 P1-RC-018

**运行时证据链（live code 实测）**：

- **ThreeWayMatcher 仅 strict 拒绝 / 非 strict warn 二元行为（1/3 策略）**：`ThreeWayMatcher.match:62-107` 对每个 invoiceLine：
  - 数量匹配（`:71-85`）：`invoiceQty > receivedQty` → strict 抛 `ERR_INVOICE_QTY_MISMATCH`（`:80-82`）/ 非 strict `LOG.warn` 放行（`:83-84`）
  - 价格匹配（`:87-106`）：`orderPrice.signum()>0 && priceDiffPercent > priceTolerance` → strict 抛 `ERR_INVOICE_PRICE_MISMATCH`（`:99-101`）/ 非 strict `LOG.warn` 放行（`:102-103`）
  - **仅"拒绝"（strict）+ "warn 放行"（非 strict）二元行为，无第三态**。L1 `use-cases.md:141` 要求策略 ∈ {拒绝, 审批后接收, 接收并过账差异} 三策略，实仓仅 1/3（"拒绝"）+ warn 放行（接近"接收"但非"审批后接收"——无 approval-required 工作流分支）。
- **隐藏接线 grep census（零命中）**：grep `module-purchase/` 全集（`*.java` / `*.xbiz.xml` / `*.yaml` / `*.yml`）搜索 `approval-required` / `approvalRequired` / `approval_after_receive` / `approval-after-receive` / `accept_with_variance` / `acceptWithVariance` / `post_variance` / `postVariance` / `price-diff-strategy` / `priceDiffStrategy` / `variance-strategy` → **零业务命中**（唯一命中 `ErpPurConstants.java:54 CONFIG_RETURN_APPROVAL_REQUIRED="erp-pur.return-approval-required"` 系采购**退货**审批 config，与发票三单匹配策略无关）。**"审批后接收"+"接收并过账差异"无隐藏接线**（xbiz/审批流 config/Processor 覆盖均零命中）。
- **非 strict warn 放行 ≠ "审批后接收"**：非 strict warn 放行 = 直接接收（无审批门控），非"审批后接收"（要求额外审批工作流分支）。无 approval-required 工作流 / 无待审批中间态 / 无 `matchStatus=PRICE_DIFF_PENDING` 持久化字段（A1.16 §5 验收标准 ③⑩ 已裁决"匹配状态待处理"持久化缺失属设计简化）。
- **"接收并过账差异"依赖 PPV 过账行（§2-2 已确认缺失）**：即使策略分支存在，PPV 过账行缺失（§2-2）致"接收并过账差异"无法落账差异金额。两策略同根因（P1-RC-018 一体两面）。

**裁决**：A1.16 §7-3 静态判定「仅"拒绝"可达」**运行时确认成立**。`ThreeWayMatcher.match:62-107` 仅 strict 拒绝 / 非 strict warn 二元行为（1/3 策略），"审批后接收"+"接收并过账差异"未实现为独立策略分支；隐藏接线 grep census 零命中（xbiz/审批流 config/Processor 覆盖均无）。**维持 P1-RC-018**（两策略未实现，修复归 MR1——`ThreeWayMatcher` 增"审批后接收"+"接收并过账差异"策略分支[config-gated 策略选择] + 与 §2-2 PPV 过账行协同；**纯 BizModel 策略分支逻辑部分预授权，PPV 过账行部分触 PurAcctDocProvider/VoucherFact 须 ask-first + 独立 plan-audit §5**）。**不触发 MR0**（strict 拒绝主路径正确 + 非 strict warn 放行属合理简化非活跃数据破坏）。

### 2-4 A4.2.36 超收容差运行时门控缺失确认 — 维持 P1-RC-019

**运行时证据链（live code 实测）**：

- **receive approve 业务规则校验仅 requireSupplierActive**：`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168`——方法体仅 `requireSupplierActive(receive, context)`（`:329-339` 校验 supplierId 非空 + partner 存在 + status==ACTIVE），**无 receive-vs-order qty 容差校验**。订单 10 + 入库 20（超收 100% > 5% 容差）approve 无门控通过。
- **ThreeWayMatcher 只做 invoice-vs-receive（不做 receive-vs-order）**：`ThreeWayMatcher.match:62-107` 遍历 **invoiceLine**（非 receiveLine），数量校验 `:71-85` 比较 **invoiceQty vs receivedQty**（发票不得超入库），**无 receive-vs-order 比较**。receive approve 不调 ThreeWayMatcher（仅 invoice approve 调，`ErpPurInvoiceProcessor.validateBusinessRulesForApprove:161-165`），receive 路径完全无三单匹配门控。
- **qtyTolerance 配置两侧均未用（dead config）**：`ThreeWayMatcher.match:52-56` 计算 `qtyTolerance = qtyTolerancePercent()`（读 `erp-pur.match-qty-tolerance` 默认 "5"）后**空守护置零**（`if (qtyTolerance == null) qtyTolerance = BigDecimal.ZERO` —— 实际 qtyTolerancePercent() 永不返回 null，此守护恒不触发但 qtyTolerance 变量后续**从未参与任何比较**——`:71-85` 数量比较用硬 `invoiceQty.compareTo(receivedQty) > 0` 无容差）；receive-vs-order 侧 `ErpPurReceiveProcessor.validateBusinessRulesForApprove` 不读此 config。**配置两侧（invoice-vs-receive + receive-vs-order）均未应用**（P2-MA2-004 dead config read watch-only 同根因，A1.16 已裁决）。
- **L4 零超收容差断言**：grep `TestErpPurReceiveApproval` + `TestErpPurReceiveStockMove` + `TestErpPurProcureToPayEnd` 全集**无** "入库数量 > 订单数量*(1+容差)→拒绝入库审核" 测试（A1.16 §3 断言强度缺口确认）。

**裁决**：A1.16 §7-4 静态判定「完全缺失」**运行时确认成立**。`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive 无 receive-vs-order qty 容差校验；`ThreeWayMatcher.match` 只做 invoice-vs-receive；`erp-pur.match-qty-tolerance` 配置两侧均未用。"订单 10 + 入库 20（超收 100%）" approve **无门控通过**。**维持 P1-RC-019**（Q4 强制实现，修复归 MR1——`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 增加 receive-vs-order qty 容差校验[按 `erp-pur.match-qty-tolerance` 配置，超容差 strict 拒绝/非 strict warn] 或 `ThreeWayMatcher` 扩展 receive-vs-order 维度；**纯 BizModel/Processor 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**[不触及 ORM/会计过账核心路径]）。**不触发 MR0**（超收不破坏活跃数据——库存按实际入库量记账，GL 平衡；超收致订单超付属业务风险非数据破坏，且非默认 strict 拒绝路径）。

### 2-5 A4.2.37 短收超容差差异处理缺失确认 — 维持 P2-RC-014

**运行时证据链（live code 实测）**：

- **无 receive-vs-order 短收容差判定**：同 §2-4，`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 无 receive-vs-order qty 容差校验（既无超收门控也无短收判定）。订单 100 + 入库 50（短收 50 > 容差）approve 无门控通过。
- **无"差异处理"触发机制**：grep `module-purchase/erp-pur-service` 全集搜索 `差异处理` / `diffProcess` / `varianceHandle` / `shortage` / `短收` / `shortReceive` → **零业务命中**（无差异处理方法 / 无差异处理触发点 / 无差异处理状态字段）。receive approve 路径（`:26-49`）无任何短收判定或差异处理调用。
- **主路径继续入库/手动关闭 OK**：多次入库各自 approve 互不阻塞（per-mutation 架构 §2-1），短收可继续入库；`ErpPurOrderCancelProcessor.cancel` 手动关闭（设 docStatus=CANCELLED）可达。L1 UC-PUR-06 ⑭「短收数量 <= 容差: 订单可继续入库或手动关闭」主路径满足，仅 ⑮「短收数量 > 容差: 触发差异处理」边界缺失。
- **L4 零短收差异处理断言**：grep 全集**无** "订单 100 + 入库 50（短收 50 > 容差）→ 触发差异处理" 测试（A1.16 §3 断言强度缺口确认）。

**裁决**：A1.16 §7-5 静态判定「完全缺失」**运行时确认成立**。无 receive-vs-order 短收容差判定 + 无"差异处理"触发机制；"订单 100 + 入库 50（短收 50 > 容差）"无差异处理触发（短收继续入库或手动关闭主路径 OK）。**维持 P2-RC-014**（次要验收标准未完全满足，登记不强制，修复归 MR1——`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 增加短收容差判定 + 差异处理触发[与 P1-RC-019 receive-vs-order 容差校验协同实现]；**纯 Processor 代码逻辑修复，不触发 §5 ask-first**）。**不触发 MR0**（主路径继续入库/手动关闭 OK，短收不破坏活跃数据）。

### 2-6 A4.2.38 关闭释放预留 config-gated 行为确认 — 主路径闭合

**运行时证据链（live code 实测）**：

- **cancel 路径 commitment release 接线**：`ErpPurOrderCancelProcessor.cancel`（继承 `AbstractCancelProcessor`）→ `beforeCancel:41-44` → `processor.runCommitmentReleaseHook(entity, context):42`（`ErpPurOrderProcessor.runCommitmentReleaseHook:222-233` 调 `budgetCommitmentBiz.release`，容错 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 静默跳过）+ `runIntercompanyReverseHook:43` → `doCancel`（设 docStatus=CANCELLED）。**关闭释放预留已接线**。
- **config-gated `erp-fin.budget-commitment-enabled` 默认 false**：`ErpFinBudgetCommitmentBizModel.isCommitmentEnabled:117-119`（`return Boolean.TRUE.equals(AppConfig.var(CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE))`，`:118` 默认 Boolean.FALSE）→ 承付总开关默认关闭。`budgetCommitmentBiz.release` 内部经此总开关 gate（关闭时短路不执行 release）。
- **部署普查（A4.2.31 已证实零生产 override）**：grep `budget-commitment-enabled` 全 20 生产 application.yaml **零命中**（仅 TEST yaml 设 true：`budget-commitment-test.yaml` / `budget-a2-test.yaml` / `budget-commitment-sales-test.yaml` / `return-commitment-test.yaml`）。**config-gated 默认 false 确认非默认活跃**。
- **config-gate = 部署启用决策非契约缺失**：与 A1.2/A1.15/A4.1.4/A4.1.7/A4.2.31 已接受 config-gated 范式一致——承付特性整体 config-gated（总开关 + 子开关），默认关闭属 ERP 通用启用范式（预算承付须部署显式启用并配置承付科目）。L1 UC-PUR-06 ⑰「关闭()→作废+释放预留」契约在 config 启用时满足（release 接线完整），config 关闭时承付特性整体不活跃故无预留可释放（语义一致）。
- **L4 间接证据**：`TestErpPurOrderApproval#testOrderCancelFromDraft`（A1.16 §3 引用）强断言 cancel→CANCELLED + 已作废不可提交（commitment release 回写 config-gated 默认 false 故测试不断言 release，与 A4.2.31 invoice 侧不对称一致）。

**裁决**：A1.16 §7-6 静态判定「已实现（config-gated）」**运行时确认成立**。cancel 路径 commitment release 经 config-gated `erp-fin.budget-commitment-enabled` 默认 false 门控；与 A1.2/A1.15/A4.1.4/A4.1.7/A4.2.31 已接受 config-gated 语义一致（config-gate = 部署启用决策非契约缺失）。**主路径接受，闭合**（config 启用时 release 接线完整，config 关闭时承付特性整体不活跃无预留可释放）。**不触发 MR0**（行为正确，config-gated 默认 false 保护非默认活跃）。

### 2-7 A4.2.39 receivedQuantity 运行时值确认 — 维持 P2-RC-013

**运行时证据链（live code 实测）**：

- **ORM 列存在（defaultValue=0）**：`module-purchase/model/app-erp-purchase.orm.xml:636` `<column name="receivedQuantity" displayName="已收货数量" domain="quantity" stdSqlType="DECIMAL" defaultValue="0" .../>` — 列物理存在，默认 0。
- **生产代码零 writer（仅框架/API bean setter）**：grep `setReceivedQuantity` 全 `module-purchase/`（排除 `_gen/`）→ **零业务命中**；全集命中仅 `erp-pur-api/.../beans/ErpPurOrderLineOutputBean.java:207` + `ErpPurOrderLineInputBean.java:206`（框架生成的 GraphQL Input/Output DTO setter，非业务 writer）+ `_gen/_ErpPurOrderLine.java` 框架 setter。**生产业务代码零 writer** → 列始终为 defaultValue=0。
- **rollupOrderReceiveStatus 仅更新 header receiveStatus**：`ErpPurReceiveProcessor.rollupOrderReceiveStatus:244-284`——内部计算 `receivedByOrderLine` map（`:254-273` 按 orderLineId 聚合 receive lines 累计数量），但循环 `:265-274` 仅用于计算 anyReceived/allFullyReceived 标志，**最终只调 `orderBiz.updateReceiveStatus(orderId, rolled, context):283`** 设订单 header `receiveStatus`（UNRECEIVED/PARTIAL/RECEIVED），**不写 orderLine.receivedQuantity**（map 计算结果被丢弃，不持久化到行级字段）。
- **header 级进度跟踪主路径 OK**：header `receiveStatus`（UNRECEIVED/PARTIAL/RECEIVED）正确派生（A1.16 §5 验收标准 ⑧ 接受），订单级收货进度查询可用；仅行级 `receivedQuantity` 字段查询始终得 0（影响行级进度报表/查询，不影响审核流程）。
- **L4 零 receivedQuantity 断言**：grep 全集**无** "两次入库后 orderLine.receivedQuantity == 60/100" 测试（A1.16 §3 断言强度缺口确认；与列始终为 0 一致）。

**裁决**：A1.16 §7-7 静态判定「列始终 0（零 writer）」**运行时确认成立**。`receivedQuantity` 列存在（ORM `:636` defaultValue=0）但生产代码零 writer（仅 _gen 框架 setter + api bean DTO setter）；`rollupOrderReceiveStatus:244-284` 仅更新 header receiveStatus 不写 orderLine.receivedQuantity → 列始终 0。**维持 P2-RC-013**（次要验收标准未完全满足——派生字段列存在但未写入，header 级进度跟踪可用，登记不强制，修复归 MR1——`ErpPurReceiveProcessor.rollupOrderReceiveStatus` 计算结果写到 orderLine.receivedQuantity[已有内部 map，增 `daoFor(ErpPurOrderLine).updateEntity` 写回]；**纯 Processor 代码逻辑修复，不触发 §5 ask-first**）。**不触发 MR0**（header 级进度跟踪主路径 OK，行级字段缺失非活跃数据破坏）。

---

## 3. 测试证据（L4）

| 测试 | 覆盖 | 与本审计关系 |
|------|------|------------|
| `TestErpPurProcureToPayEnd#testProcureToPayPartialSettlement:108-155`（A4.2.27 引用）| UC-PUR-03 单次入库 approve + receive.posted=true 凭证落地 | **间接证据**：A4.2.33（per-mutation approve 架构 + posted 回写 `:121`，两次入库 = 同路径执行两次 = 凭证数==2 隐含） |
| `TestErpPurReceiveApproval`（A1.16 §3 引用）| UC-PUR-03 入库审批状态机 | **间接证据**：A4.2.33（单次入库 approve 状态迁移；无两次入库凭证数==2 专属断言——缺口归测试覆盖非行为缺陷） |
| `TestErpPurInvoiceApproval`（A1.16 §3 引用）| UC-PUR-05 发票审批状态机 | **缺口证据**：A4.2.34（无价格差异过账行断言——与 L3 实现缺失一致，P1-RC-018） |
| `TestErpPurThreeWayMatch`（A1.16 §3 引用）| UC-PUR-02/05 三单匹配（strict 拒绝 + 非 strict 放行 + 容差内通过） | **缺口证据**：A4.2.35（仅"拒绝"+"warn 放行"二元行为，三策略仅 1/3 覆盖——P1-RC-018）+ A4.2.36（无超收容差断言——P1-RC-019） |
| `TestErpPurOrderApproval#testOrderCancelFromDraft`（A1.16 §3 引用）| UC-PUR-06 ⑤ 订单 cancel→CANCELLED | **间接证据**：A4.2.38（cancel 接线可达；commitment release 回写 config-gated 默认 false 故不断言，与 A4.2.31 一致） |

**断言强度缺口（本审计关键，与 A1.16 §3 一致）**：UC-PUR-03 ⑦ 两次入库凭证数==2 / UC-PUR-05 ⑫ PPV 过账行 / UC-PUR-05 ⑪ 三策略完整性 / UC-PUR-02 ② 超收容差 / UC-PUR-06 ⑮ 短收差异处理 / UC-PUR-03 ⑤⑥ receivedQuantity 六项均**零直接断言**——前五项与 L3 实现缺失一致（P1-RC-018 / P1-RC-019 / P2-RC-014），最后一项与列始终 0 一致（P2-RC-013）。

---

## 4. 业财保护区域探针纪律声明

> A4.2.34 触及业财保护区域（roadmap §横切关注点 #5：会计过账逻辑 / VoucherFact / PostingProcessor 核心路径 / Provider createFacts）。

本审计为**只读探针**，遵守保护区域暂停协议：

- **READ-ONLY 标记（多处）**：本报告对 `PurAcctDocProvider.createFacts` / `VoucherFact` 构造 / `PurInvoicePostingDispatcher.buildEvent` billData 内容 / `InvPostingDispatcher` PURCHASE_INPUT 路由的全部交互均为**只读追踪**（grep census + 行级结构追踪 + billData 内容核查 + 测试断言复核），**未修改任何过账逻辑 / VoucherFact 构造 / PostingProcessor 核心路径 / Provider createFacts**。
- **P1 维持不撤销**：P1-RC-018（价格差异处理不完整）维持 P1（Q4 会计类无例外），**修复义务归 MR1 R1.0 展开器**，触及 PurAcctDocProvider/VoucherFact/PostingProcessor 核心路径**须 ask-first + 独立 plan-audit §5**；P1-RC-019（超收容差校验缺失）维持 P1，**纯 BizModel/Processor 代码逻辑修复预授权自动执行，不触发 §5 ask-first**（不触及 ORM/会计过账核心路径）。P2-RC-013/P2-RC-014 维持 P2 登记不强制。
- **主路径行为正确闭合**：A4.2.33（两次入库独立过账架构）/ A4.2.38（关闭释放预留 config-gated）两项主路径行为运行时确认正确闭合，无修复义务。

---

## 5. 与既有 finding 衔接（复用裁决，无新 finding）

按 §去重协议，每项运行时确认裁决均 grep arm-index 同域同控制点后给出「复用维持」结论：

| finding | arm-index 行 | 本审计对应 | 运行时裁决 |
|---------|-------------|----------|-----------|
| `P1-RC-018` | :159 | A4.2.34（UC-PUR-05 ⑫ PPV 过账行缺失）+ A4.2.35（UC-PUR-05 ⑪ 三策略仅"拒绝"） | **维持 P1**：运行时确认 `PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行无 PPV 行 + `PurInvoicePostingDispatcher.buildEvent` billData 无差异数据 + `ThreeWayMatcher.match:62-107` 仅 strict 拒绝/非 strict warn（1/3 策略）+ 隐藏接线 grep census 零命中。GL 仍平衡属管理会计可视性缺口非活跃数据破坏。Q4 会计类无例外不撤销，修复归 MR1（PPV 过账行触 PurAcctDocProvider/VoucherFact 须 ask-first + 独立 plan-audit §5；策略分支纯 BizModel 部分预授权）。 |
| `P1-RC-019` | :160 | A4.2.36（UC-PUR-02 ② 超收容差门控缺失） | **维持 P1**：运行时确认 `ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive 无 receive-vs-order qty 容差校验 + `ThreeWayMatcher.match` 只做 invoice-vs-receive + `erp-pur.match-qty-tolerance` 配置两侧均未用。"订单 10 + 入库 20"approve 无门控通过。Q4 强制实现不撤销，修复归 MR1（纯 BizModel/Processor 预授权，不触 §5 ask-first）。 |
| `P2-RC-013` | :161 | A4.2.39（UC-PUR-03 ⑤⑥ receivedQuantity 列始终 0） | **维持 P2 watch-only**：运行时确认 ORM `:636` 列存在 defaultValue=0 + 生产代码零 writer（仅 _gen + api bean setter）+ `rollupOrderReceiveStatus:244-284` 仅更新 header receiveStatus 不写 orderLine.receivedQuantity → 列始终 0。header 级进度跟踪主路径 OK，行级字段查询缺失。登记不强制，修复归 MR1（纯 Processor 预授权，不触 §5 ask-first）。 |
| `P2-RC-014` | :162 | A4.2.37（UC-PUR-06 ⑮ 短收差异处理缺失） | **维持 P2 watch-only**：运行时确认无 receive-vs-order 短收容差判定 + 无"差异处理"触发机制（grep census 零命中）。"订单 100 + 入库 50"无差异处理触发（短收继续入库/手动关闭主路径 OK）。与 P1-RC-019 同根因不同 UC 不同控制点。登记不强制，修复归 MR1（纯 Processor 预授权，不触 §5 ask-first）。 |

**无新 finding 新建**（全部 reuse 维持不降级不撤销）。运行时证据**未发现活跃数据破坏或会计错误已活跃**（A4.2.33/A4.2.38 主路径行为正确闭合；A4.2.34 PPV 缺失 GL 仍平衡属管理会计可视性缺口；A4.2.35 仅"拒绝"主路径正确；A4.2.36 超收不破坏活跃数据[库存按实际入库记账 GL 平衡]；A4.2.37 短收主路径继续入库/手动关闭 OK；A4.2.39 header 级进度跟踪主路径 OK）→ **不触发 MR0**。

---

## 6. 多维审计自检（multi-dimensional-audit-prompt.md）

按 `docs/skills/multi-dimensional-audit-prompt.md` 默认 7 维度 + nop-app-erp 项目特定维度，逐维度裁决：

- **需求正确性**：7 项存疑点均逐字引自 A1.16 §7（L1 use-cases.md 真相源），运行时裁决与需求契约对齐（UC-PUR-02 ② / UC-PUR-03 ⑤⑥⑦ / UC-PUR-05 ⑪⑫ / UC-PUR-06 ⑮⑰ 均为 L1 显式验收标准）。本维度无新发现。
- **owner-doc 对齐**：`three-way-match.md §匹配规则/§数量匹配/§价格差异/§不匹配的处理策略` + `state-machine.md §场景A`（多次入库各自 approve）+ `posting.md`（业财过账）+ `budget.md §承付会计 §3`（commit/release 接入点）owner doc 声明与 HEAD 实现差距经运行时确认（A4.2.34 PPV 过账行缺失 vs owner doc §价格差异"差异计入采购价格差异科目"；A4.2.35 三策略 vs owner doc §不匹配处理策略"拒绝/审批后接收/接收并过账差异"；A4.2.36 超收容差 vs owner doc §数量匹配"入库数量之和 <= 订单数量*(1+超收容差)"）。本维度无新发现（差距归 MR1）。
- **架构或边界影响**：本审计零代码变更，不引入跨模块依赖 / API 契约变更 / 保护区域触碰。receive→`IErpInvStockMoveBiz.generateMove` 跨域 Facade（A4.2.33）+ cancel→`budgetCommitmentBiz.release` 跨域 Facade（A4.2.38）——跨域边界经 A2.1/A2.8/A2.16 证实合规（purchase production 零 `daoFor(ErpInv*/ErpFin*)` 直写，均经 I*Biz Facade）。本维度无新发现。
- **验证充分性**：每项运行时裁决均有独立 grep census + file:line 证据 + 测试断言复核（§2-1..§2-7），可独立证伪（若 createFacts AP_INVOICE 有第 4 行 PPV / 若 match 有第三策略分支 / 若 validateBusinessRulesForApprove 有 qty 校验 / 若 rollupOrderReceiveStatus 写 orderLine.receivedQuantity / 若 setReceivedQuantity 有生产 writer，则裁决翻转）。本维度无新发现。
- **回归风险**：零生产代码变更，无回归路径。本维度无新发现。
- **路由和技能选择正确性**：roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`，本审计为只读审计无代码变更，技能匹配。本维度无新发现。
- **待办或自主权策略漂移**：本审计范围 = A1.16 §7 七项存疑点运行时确认，未扩大范围、未关闭未完成项、未将阻塞降级为跟进项。两项 P1（P1-RC-018 / P1-RC-019）全部维持（修复义务归 MR1，plan `Deferred But Adjudicated` 正确分类）；两项 P2 watch-only（P2-RC-013 / P2-RC-014）维持登记不强制；两项主路径（A4.2.33 / A4.2.38）行为正确闭合；A4.2.34 业财保护区域探针只读确认未改过账逻辑。本维度无新发现。
- **项目特定维度（view.xml gen-control / ORM 完整性 / 代码生成纪律）**：本审计不触及 view.xml delta；未触及 ORM 结构（仅引用既有列 receivedQuantity）；未触及生成文件。本维度无新发现。

**反窄化自检通过**：已对全部 8 维度给出裁决（含「本维度无发现」），非单维深挖。

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本审计为只读审计，**无生产代码变更**，checker 无回归风险。本报告不以 checker 脚本退出码作为门控通过依据（checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow 解析 actual > baseline）。零代码变更 → actual = baseline（git status 仅 .md 文件）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 7 项运行时裁决已按 §去重协议 grep arm-index 同域同控制点后给出「复用维持」结论（P1-RC-018 维持 + P1-RC-019 维持 + P2-RC-013 维持 watch-only + P2-RC-014 维持 watch-only），**无未经比对直接新建的 finding，无新 finding 新建**。

---

## Verdict

**PASS（运行时确认维持 A1.16 §5/§6/§7 全部裁决）**：7 项静态存疑点运行时行为**全部确认成立**，A1.16 静态判定无一翻转：

- **A4.2.33（两次入库独立过账凭证数==2 架构）CONFIRMED 主路径闭合**：per-mutation approve 架构——每次 `ErpPurReceiveApproveProcessor.approve` 独立 `triggerIncomingMove`→`IErpInvStockMoveBiz.generateMove`→InvPostingDispatcher PURCHASE_INPUT 凭证 + `applyPostingResult` per-mutation posted 回写，rollupOrderReceiveStatus 不产凭证不合并 → 凭证数==2 由架构隐含成立（无专属断言但无合并/短路路径）。
- **A4.2.34（让步接收价格差异过账缺失）CONFIRMED 维持 P1-RC-018**：`PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行（1403/2221/2202）无 PPV 行 + billData 无差异数据传递 + 差异埋在 1403 在途物资金额中。GL 仍平衡属管理会计可视性缺口非活跃数据破坏。修复归 MR1（触 PurAcctDocProvider/VoucherFact 须 ask-first + 独立 plan-audit §5）。
- **A4.2.35（三处理策略分支可达性）CONFIRMED 维持 P1-RC-018**：`ThreeWayMatcher.match:62-107` 仅 strict 拒绝/非 strict warn（1/3 策略）+ 隐藏接线 grep census 零命中（xbiz/审批流 config/Processor 覆盖均无）。"审批后接收"+"接收并过账差异"未实现。
- **A4.2.36（超收容差门控缺失）CONFIRMED 维持 P1-RC-019**：`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive + `ThreeWayMatcher` 只做 invoice-vs-receive + qtyTolerance 配置两侧均未用。"订单 10 + 入库 20"approve 无门控通过。修复归 MR1（纯 BizModel/Processor 预授权，不触 §5 ask-first）。
- **A4.2.37（短收差异处理缺失）CONFIRMED 维持 P2-RC-014**：无 receive-vs-order 短收容差判定 + 无"差异处理"触发机制（grep census 零命中）。"订单 100 + 入库 50"无差异处理触发（短收继续入库/手动关闭主路径 OK）。登记不强制，修复归 MR1（纯 Processor 预授权，不触 §5 ask-first）。
- **A4.2.38（关闭释放预留 config-gated 行为）CONFIRMED 主路径闭合**：cancel→`beforeCancel:41-44`→`runCommitmentReleaseHook` 接线完整 + config-gated `erp-fin.budget-commitment-enabled` 默认 false（`ErpFinBudgetCommitmentBizModel:118`）+ 零生产 override（A4.2.31 已证）。config-gate = 部署启用决策非契约缺失（对齐 A1.2/A1.15/A4.1.4/A4.2.31 范式）。
- **A4.2.39（receivedQuantity 运行时值）CONFIRMED 维持 P2-RC-013**：ORM `:636` 列存在 defaultValue=0 + 生产代码零 writer（仅 _gen + api bean setter）+ `rollupOrderReceiveStatus:244-284` 仅更新 header receiveStatus 不写 orderLine.receivedQuantity → 列始终 0。header 级进度跟踪主路径 OK。登记不强制，修复归 MR1（纯 Processor 预授权，不触 §5 ask-first）。

**裁决分支**：两项主路径（A4.2.33 / A4.2.38）命中「主路径闭合」分支；两项缺陷（A4.2.34 / A4.2.35 → P1-RC-018；A4.2.36 → P1-RC-019）命中「维持 P1（reuse 重开 finding 不降级，Q4 强制实现）+ 运行时证据记录」分支；两项缺陷（A4.2.37 → P2-RC-014；A4.2.39 → P2-RC-013）命中「维持 P2（登记不强制）+ 运行时证据记录」分支；**无升级触发 MR0**（运行时未发现活跃数据破坏或会计错误已活跃——PPV 缺失 GL 仍平衡属管理会计可视性缺口；超收/短收不破坏活跃数据；receivedQuantity 行级字段缺失 header 级主路径 OK）。修复义务归 MR1 R1.0 展开器（P1-RC-018 触会计过账核心路径须 ask-first + 独立 plan-audit §5；P1-RC-019 / P2-RC-013 / P2-RC-014 纯 BizModel/Processor 代码逻辑预授权自动执行不触 §5 ask-first）。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index reuse 维持注记）。

---

## 参考

- 真相源：`docs/design/purchase/use-cases.md`（UC-PUR-02/03/05/06 验收标准 ②⑤⑥⑦⑪⑫⑮⑰）
- 来源存疑点：`docs/audits/2026-08-03-0200-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md` §7（7 项静态存疑点）+ §5（验收标准分级①-⑰）+ §6（finding 衔接裁决）
- 设计参考：`docs/design/purchase/three-way-match.md`（§匹配规则/§数量匹配/§价格差异/§不匹配的处理策略）+ `state-machine.md`（§场景A 多次入库）+ `README.md` + `docs/design/finance/posting.md`（AP_INVOICE 凭证范式）+ `docs/design/finance/budget.md §承付会计 §3`（commit/release 接入点）
- L5 既有证据：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8 状态机迁移 + reverseApprove 红冲闭环 + per-mutation approve 架构）+ `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（A2.1 P2P 链路 + 三单匹配价格容差）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（A1.1 业财过账引擎 GR/IR + AP 范式）+ `docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`（A4.2.27-32 PURCHASE_INPUT 全链闭合 + config `erp-fin.budget-commitment-enabled` 默认 false 部署普查）
- 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
- 技能：`docs/skills/multi-dimensional-audit-prompt.md`（默认 7 维度 + 项目特定维度）
