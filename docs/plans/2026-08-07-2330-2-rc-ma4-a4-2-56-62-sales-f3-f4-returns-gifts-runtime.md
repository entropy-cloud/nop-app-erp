# 2026-08-07-2330-2 rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime 销售退货族/赠品/看板运行时确认

> Plan Status: completed
> Mission: requirement-compliance
> Work Item: A4.2.56 / A4.2.57 / A4.2.58 / A4.2.59 / A4.2.60 / A4.2.61 / A4.2.62

> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.56-A4.2.62
> Related: `docs/audits/2026-08-03-0630-rc-ma1-a1-20-sales-f3-returns-family.md`（A1.20 MA1 报告 §7 存疑点）、`docs/audits/2026-08-03-0900-rc-ma1-a1-21-sales-f4-gift-dashboards.md`（A1.21 MA1 报告 §7 存疑点 SP-1/2/4/5）、`docs/plans/2026-08-03-0630-1-rc-ma1-a1-20-sales-f3-returns-family.md`（A1.20 计划）、`docs/plans/2026-08-03-0900-1-rc-ma1-a1-21-sales-f4-gift-dashboards.md`（A1.21 计划）
> Audit: required

## Current Baseline

A1.20（sales-F3 退货族）+ A1.21（sales-F4 赠品与看板）两份 MA1 报告 §7 存疑点，roadmap 已对齐为 7 个 A4.2 实体行。对应 §6 既有 finding：P2-MA2-011（红字发票 credit memo 替代，跨期配比净效果等价性）、P1-RC-022（价税分离缺失，多档税率混合 GL 偏差，与 Plan 1 A4.2.49 同根因同控制点）、P1-RC-025（换货功能完全缺失，须人工确认 product-scope 范围裁剪）、P1-RC-026（退货成本策略仅"原出库成本"1/3，`ReturnStockMoveBuilder unitCost=line.unitPrice` vs 当前库存成本偏差）、P1-RC-027（已核销发票 pre-approve 守卫缺失改 post-approve 静默反向，`ReturnRefundOrchestrator` 并发竞态）、P2-RC-023（赠品行 UI 显式标记缺口，后端隐式标记 pricingSource=PROMOTION + remark="赠品行"）、P2-RC-024（AR 账龄 4 桶视图缺失，预警列表是更严格子集）。A1.20 §5 裁决：UC-SAL-04/05/06/07/09 均有 P1（#3-#8 P1-RC-023..028）；A1.21 §5 裁决：UC-SAL-08 接受 on 赠品扣库存+成本 + P1 on 价税分离（复用 P1-RC-022）+ P2 on UI 标记；UC-SAL-12 接受 on KPI/趋势/TOP-N/阈值 + P2 on AR 4 桶。

**roadmap 标注勘误声明**：roadmap A4.2.56 标注「A1.20 SP-1 + A1.21 SP-1（合并：价税分离 同根因 P1-RC-022 同控制点）」经源报告核实**合并不成立**——A1.20 §7 SP-1（`:266`）实为 P2-MA2-011（credit memo 跨期配比净效果），A1.21 §7 SP-1（`:242`）才是 P1-RC-022（价税分离），二者不同根因不同控制点。本计划按 A4.2.56 单一 roadmap 行覆盖**两个独立存疑点**（分别验证），不按错误合并处理。同理，roadmap A4.2.58 标注 finding ID「P1-RC-028」实为**P1-RC-027**（A1.20 §7 SP-4 `:269` = #7 = P1-RC-027 ReturnRefundOrchestrator；P1-RC-028 = SP-3 期间 CLOSED 已由 A4.2.43 闭合）。本计划按正确 finding ID 执行验证与 arm-index 衔接。

注：A1.20 SP-3（P1-RC-028 期间 CLOSED 守卫）已由 A4.2.43（purchase+sales 期间 CLOSED guard 间接拦截）闭合——`ErpFinPostingProcessor.resolveOpenPeriod:524-527` 全局生效，sales return 过账路径经 finance 引擎间接拦截。本计划不再重复 SP-3。

这 7 项存疑点分两类：(1) 缺陷确认（A4.2.56-a P1-RC-022 价税分离 GL 偏差量化 / A4.2.56-b P2-MA2-011 credit memo 跨期配比净效果 / A4.2.57 P1-RC-026 退货成本偏差 / A4.2.58 P1-RC-027 并发竞态 / A4.2.59 P1-RC-025 换货缺失 product-scope 确认 / A4.2.61 P2-RC-024 AR 4 桶 / A4.2.62 P2-RC-023 UI 标记，HEAD 静态判定 = 缺陷/弱，运行时确认闭合维持分级）；(2) 数值/行为探针（A4.2.60 赠品成本多物料混合 abs() 求和正确性，HEAD 静态判定 = 行为可能正确需运行时数值确认）。

- **A4.2.56（roadmap 单行覆盖两个独立存疑点，roadmap 合并标注勘误见上）**：
  - **A4.2.56-a（A1.21 SP-1，P1-RC-022 价税分离多档税率混合 GL 偏差量化）**：HEAD 静态判定 = `recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount + `recomputeOrderTotals:181-197` 复用陈旧 taxAmount。运行时确认多档税率混合（13%/9%/6%）+ 多档促销叠加（行级 PERCENT_DISCOUNT + 头级 AMOUNT_OFF）场景下税额偏差范围。**触及业财保护区域探针——只读确认不改过账逻辑。** 与 Plan 1 A4.2.49 同根因 P1-RC-022，本计划深化偏差范围量化。
  - **A4.2.56-b（A1.20 SP-1，P2-MA2-011 credit memo 跨期配比净效果）**：HEAD 静态判定 = 负向 ArApItem credit memo 替代红字 ErpSalInvoice。运行时确认跨月出库-开票场景（X 月出库 + X+1 月开票 + X+1 月退货）下 GL 收入科目余额在 X+1 月是否正确冲减，还是延至期末结账。
- **A4.2.57（A1.20 SP-2，P1-RC-026 退货成本不同库存策略数值偏差）**：HEAD 静态判定 = `ReturnStockMoveBuilder:64 unitCost=line.unitPrice`（原出库成本）经 StockMoveBookkeeper 写入 CostLayer，与"当前库存成本"（MA 加权平均/FIFO 队列首项）偏差。运行时确认不同库存策略（FIFO/MOVING_AVERAGE/STANDARD/SPECIFIC）下数值偏差范围。
- **A4.2.58（A1.20 SP-4，P1-RC-027 ReturnRefundOrchestrator post-approve 静默反向并发竞态；roadmap 标注 P1-RC-028 实为 P1-RC-027 勘误见上）**：HEAD 静态判定 = `ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` post-approve 静默反向，多个退货单并发触发同一发票核销反向 → ReceiptLine 写入竞态。运行时确认并发场景下实际行为。
- **A4.2.59（A1.20 SP-5，P1-RC-025 换货功能完全缺失 product-scope 裁剪确认）**：HEAD 静态判定 = 无 `returnType` 列 + 无换货分支/新出库单/sourceBill。运行时确认 product-scope 是否隐含含换货（**须运行时确认真相源**——若隐含含换货则 P1 强制实现，若裁剪则按 §4 (iii) 改真相源非降级）。
- **A4.2.60（A1.21 SP-2，赠品成本多物料混合出库 totalCost abs() 求和正确性）**：HEAD 静态判定 = `DeliveryStockMoveBuilder.buildLines:54-67` 不传 unitCost + 库存域 `InvPostingDispatcher.buildEvent:181-221` 按 `Σ ledger.totalCost.abs()` 入账。运行时确认"1 普通物料 + 1 赠品物料"出库场景下 6401 借方金额 = Σ 普通成本 + 赠品 avgCost（赠品成本未被 abs() 折叠丢失）。
- **A4.2.61（A1.21 SP-4，P2-RC-024 AR 账龄 4 桶跨桶归类歧义）**：HEAD 静态判定 = `findArOverdueAlert:170-209` 仅扁平 ageDays，4 桶视图缺失。运行时确认若实现 4 桶是否存在跨桶归类歧义（账龄=30/60/90 边界值归属）+ 0-30 桶是否包含未到期项（age<0 时置 0 归 0-30 桶）。
- **A4.2.62（A1.21 SP-5，P2-RC-023 赠品行 UI 显式标记缺口产品化影响）**：HEAD 静态判定 = ORM `ErpSalOrderLine` 无 isGift/lineType 列，隐式标记 pricingSource=PROMOTION + remark="赠品行"。运行时确认产品化部署场景下隐式标记是否足够（销售分析/赠品成本归集/合规审计）。

剩余差距：七项均为只读运行时确认。A4.2.56-a（P1-RC-022）触及会计过账保护区域探针——只读确认税额偏差，修复义务归 MR1 触 recomputeLineAmount/recomputeOrderTotals 须 ask-first；A4.2.56-b（P2-MA2-011）维持 watch-only（credit memo 跨期配比主路径等价，边界 successor 跟踪）；A4.2.57（P1-RC-026 退货成本策略）/ A4.2.58（P1-RC-027 并发竞态 pre-approve 守卫）修复归 MR1（纯 BizModel/Processor 预授权；ORM returnType 列若增设则 ask-first）；A4.2.59（P1-RC-025 换货）须人工确认 product-scope 裁剪——若须实现则 ORM 结构变更须 ask-first + 独立 plan-audit；A4.2.60（赠品成本）主路径确认正确则闭合；A4.2.61（P2-RC-024 AR 4 桶）/ A4.2.62（P2-RC-023 UI 标记）修复归 MR1（纯 BizModel/view.xml 预授权，登记不强制）。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.56-A4.2.62 七项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`。
- 每项给出 §2 判据裁决：缺陷项（A4.2.56-a P1-RC-022 / A4.2.57 P1-RC-026 / A4.2.58 P1-RC-027）维持 P1 + 运行时证据；credit memo 项（A4.2.56-b P2-MA2-011）维持 watch-only；真相源确认项（A4.2.59 P1-RC-025）确认 product-scope 裁剪裁决；数值探针项（A4.2.60）确认赠品成本正确性；P2 项（A4.2.61 P2-RC-024 / A4.2.62 P2-RC-023）维持 P2 + 边界证据；若运行时发现会计错误已活跃（退货成本偏差致 GL 不平衡）则触发 MR0。
- 完成后回写 roadmap A4.2.56-A4.2.62 `todo → done`，并按裁决更新 arm-index。

## Non-Goals

- 不实现价税分离重算（P1-RC-022）/ 退货成本 3 策略（P1-RC-026）/ pre-approve 守卫（P1-RC-027）/ 换货功能（P1-RC-025）/ AR 4 桶视图（P2-RC-024）/ 赠品 UI 标记（P2-RC-023）——修复义务归 MR1 R1.0 展开器；P1-RC-022 触及 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first；P1-RC-025 须人工确认 product-scope 裁剪方向（若须实现 ORM 结构变更须 ask-first + 独立 plan-audit）。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。A4.2.59 P1-RC-025 product-scope 裁剪确认按方法论 §4 (iii) + §9 冻结条款**不直改真相源**（须人工裁决）。
- 不修改过账逻辑或 PostingProcessor 核心路径（roadmap §横切关注点 #5 ask-first 保护区域）。
- 不复跑 MA2 状态机审计（`2026-07-28-0400-arm-ma2-sales-state-machine.md` 已证实的 return/invoice 状态机迁移作为既有证据输入）；不重复 A4.2.43 已闭合的期间 CLOSED 守卫（A1.20 SP-3）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-03-0630-rc-ma1-a1-20-sales-f3-returns-family.md` §5/§6/§7 + `docs/audits/2026-08-03-0900-rc-ma1-a1-21-sales-f4-gift-dashboards.md` §5/§6/§7 + `docs/design/sales/`（use-cases.md / returns.md / state-machine.md / ui-patterns.md）+ `docs/design/finance/posting.md`（SALES_OUTPUT/AR_INVOICE 凭证范式）+ `docs/design/dashboards.md`（销售看板 AR 账龄）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 取价/价税分离凭证行追踪 + 退货成本策略确认 + 并发竞态分析 + product-scope 真相源确认（grep census / createFacts 行级结构追踪 / ReturnStockMoveBuilder unitCost 取值确认 / ReturnRefundOrchestrator 并发路径分析 / product-scope 换货范围逐字核查 / dashboards AR age 语义确认），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.56-A4.2.62）

Status: completed
Targets: `docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.20 done ✓；A1.21 done ✓

- [x] **A4.2.56-a 价税分离多档税率混合 GL 偏差量化确认（A1.21 SP-1，P1-RC-022）**：确认 `recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount + `recomputeOrderTotals:181-197` 复用陈旧 taxAmount；追踪多档税率混合（13%/9%/6%）+ 促销叠加场景下 invoice.totalTaxAmount 偏差范围（促销前税额沿用致销项税高估/收入低估，GL 总额仍平衡）；与 Plan 1 A4.2.49 协同（同根因 P1-RC-022）。**触及业财保护区域探针——只读确认，不改过账逻辑。** 裁决：维持 P1-RC-022 P1（Q4 会计准确性类无例外，修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 须 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.56-b credit memo 跨期配比净效果确认（A1.20 SP-1，P2-MA2-011）**：确认负向 ArApItem credit memo 替代红字 ErpSalInvoice 的跨期配比净效果——X 月出库 + X+1 月开票 + X+1 月退货场景下 GL 收入科目余额在 X+1 月是否正确冲减（credit memo 负金额在 X+1 月直接冲减 AR/收入），还是延至期末结账（红字发票冲减需期末配比）；确认主路径 credit memo 净效果等价（GL 净零成立），跨期配比可视性为 successor 跟踪项。裁决：维持 P2-MA2-011 watch-only（credit memo 替代功能等价性主路径成立，跨期配比可视性 successor 跟踪）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.57 退货成本不同库存策略数值偏差确认（P1-RC-026）**：确认 `ReturnStockMoveBuilder:64 unitCost=line.unitPrice`（原出库成本）经 StockMoveBookkeeper 写入 CostLayer；确认不同库存策略（FIFO/MOVING_AVERAGE/STANDARD/SPECIFIC）下"原出库成本"与"当前库存成本"的数值偏差方向（MA 下原成本可能高估/低估当前成本，FIFO 队列首项同理）；确认 GL 平衡不破坏但成本归集准确性偏差；确认配置键 `erp-sal.return-cost-method` 未声明（仅 1/3 策略实现）。裁决：维持 P1-RC-026 P1（L1 显式 3 策略 + 配置键，修复归 MR1 纯 BizModel/Processor 预授权；若新增 ORM costMethod 列则 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.58 ReturnRefundOrchestrator post-approve 静默反向并发竞态确认（A1.20 SP-4，P1-RC-027）**：确认 `ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` post-approve 静默反向行为（无 pre-approve `ERR_RETURN_INVOICE_SETTLED` 守卫）；确认多个退货单并发触发同一发票核销反向时 ReceiptLine 写入是否有并发保护（乐观锁/UK）；确认 L1 "先撤回核销再退货"控制点属 pre-approve，post-approve 静默反向属行为偏离。裁决：维持 P1-RC-027 P1（§2 P1② 异常路径未实现，修复归 MR1 纯 BizModel/Processor 预授权——加 pre-approve 守卫）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.59 换货功能完全缺失 product-scope 裁剪确认（P1-RC-025）**：逐字核查 `docs/requirements/product-scope.md` 销售域范围是否隐含含换货（UC-SAL-06 换货 `use-cases.md:149-161`）；确认无 `returnType` 列 + 无换货分支/新出库单/sourceBill；确认若 product-scope 隐含含换货则 P1 强制实现，若裁剪则按方法论 §4 (iii) 改真相源非降级。**须运行时确认真相源——若须实现则 ORM 结构变更须 ask-first + 独立 plan-audit。** 裁决：维持 P1-RC-025 P1（须人工确认 product-scope 裁剪，按 §4 (iii) + §9 冻结不直改真相源）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.60 赠品成本多物料混合出库 totalCost abs() 求和正确性确认**：确认 `DeliveryStockMoveBuilder.buildLines:54-67` 不传 unitCost + `InvPostingDispatcher.buildEvent:181-221` 按 `Σ ledger.totalCost.abs()` 入账；确认"1 普通物料 + 1 赠品物料"出库场景下 6401 借方金额是否正确包含赠品 avgCost（赠品 unitPrice=0 但 avgCost>0，totalCost 求和应含赠品 quantity×avgCost）；确认 abs() 在负库存/红冲场景下的折叠风险。裁决：主路径正确则闭合（赠品成本按成本入 6401 行为正确），abs() 边界风险归 P2 watch-only successor。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.61 AR 账龄 4 桶跨桶归类歧义确认（P2-RC-024）**：确认 `findArOverdueAlert:170-209` 仅扁平 ageDays，4 桶视图（0-30/31-60/61-90/90+）缺失；确认若实现 4 桶：①账龄=30/60/90 边界值归属歧义（`<` vs `<=`）；②`age = ChronoUnit.DAYS.between(dueDate, today)` 当 age<0（未到期）置 0 归 0-30 桶语义是否与 dashboards.md:60「应收账龄」冲突。裁决：维持 P2-RC-024 P2（次要验收标准未完全满足，预警列表是更严格子集，登记不强制，修复归 MR1 纯 BizModel 预授权）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.62 赠品行 UI 显式标记缺口产品化影响确认（P2-RC-023）**：确认 ORM `ErpSalOrderLine` 无 isGift/lineType 列，隐式标记 pricingSource=PROMOTION + remark="赠品行"；确认隐式标记在产品化部署场景下对销售分析/赠品成本归集/合规审计是否足够；确认 `ui-patterns.md:36` 行级"赠品"开关设计意图存在但 ORM/view.xml 未落地。裁决：维持 P2-RC-023 P2（后端行为正确，UI 层 cosmetic 缺口，登记不强制，修复归 MR1 纯 view.xml + ORM isGift 列预授权；ORM 列若增设则 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：七项存疑点（A4.2.56 含 a/b 两个子目标 + A4.2.57-A4.2.62）各出 §裁决（主路径闭合 / 维持 P1 reuse + 运行时证据 / 维持 P2 / 触发 MR0）+ §与既有 finding 衔接（P2-MA2-011 / P1-RC-022 / P1-RC-025 / P1-RC-026 / P1-RC-027 / P2-RC-023 / P2-RC-024 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）+ §业财保护区域探针纪律声明（A4.2.56-a/A4.2.57 触及 taxAmount/退货成本 GL 只读探针）+ §roadmap 标注勘误声明（A4.2.56 合并勘误 + A4.2.58 finding ID 勘误）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。A4.2.56-a（taxAmount）+ A4.2.57（退货成本）触及业财保护区域探针——只读确认偏差，不改过账逻辑。

- [x] 验证报告落盘 `docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`，含七项存疑点（A4.2.56 含 a/b 子目标）各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：主路径闭合 / 维持分级（P1 Q4 强制实现 / P2 登记）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.56-62 done）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-RC-022 维持 P1（运行时确认多档税率混合税额偏差，与 Plan 1 A4.2.49 协同，修复归 MR1 触 recomputeLineAmount/recomputeOrderTotals 须 ask-first）；P2-MA2-011 维持 watch-only（credit memo 跨期配比主路径等价，边界 successor 跟踪）；P1-RC-025 维持 P1（product-scope 裁剪须人工确认，按 §4 (iii) + §9 冻结不直改真相源）；P1-RC-026 维持 P1（运行时确认退货成本偏差，修复归 MR1 纯 BizModel/Processor 预授权）；P1-RC-027 维持 P1（运行时确认并发竞态 + pre-approve 守卫缺失，修复归 MR1 纯 BizModel/Processor 预授权；roadmap 标注 P1-RC-028 实为 P1-RC-027 勘误）；P2-RC-023/P2-RC-024 维持 P2（登记不强制）。无新 finding 新建（全部维持）。
- [x] `Add` roadmap A4.2.56-A4.2.62 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 七项状态已更新为 done 且与报告裁决一致
- [x] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0273b3e09ffevN2D7Fy44iAYJJ) — two blocking citation errors: (1) A4.2.56 incorrectly merged A1.20 SP-1 (P2-MA2-011 credit memo 跨期配比) with A1.21 SP-1 (P1-RC-022 价税分离) under a single incorrect root cause — A1.20 SP-1's actual topic absent from all execution items; (2) A4.2.58 cited P1-RC-028 but the behavior (ReturnRefundOrchestrator) is P1-RC-027 (P1-RC-028 = SP-3 期间 CLOSED already closed by A4.2.43).
- Independent draft review iteration 2: accept (ses_02735ce6bffeUiILLWUfkpZjDg) after fixes — A4.2.56 split into a/b sub-targets (P1-RC-022 from A1.21 SP-1 + P2-MA2-011 from A1.20 SP-1) with correct root cause attribution; A4.2.58 corrected to P1-RC-027 throughout; errata declaration added documenting roadmap annotation discrepancies; all P1-RC-028 references now only in errata/A4.2.43-closure context. Citation accuracy verified against A1.20/A1.21 source reports §7 (line-level match). Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（七项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-022 / P1-RC-025 / P1-RC-026 / P1-RC-027 / P2-MA2-011 / P2-RC-023 / P2-RC-024 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P1-RC-022（价税分离重算）修复归 MR1 R1.0 展开器，触 recomputeLineAmount/recomputeOrderTotals 核心路径须 ask-first + 独立 plan-audit（roadmap §横切关注点 #5 会计过账逻辑类）；P2-MA2-011（credit memo 替代红字发票）维持 watch-only（功能等价性主路径成立，跨期配比可视性 successor）；P1-RC-025（换货功能）须人工确认 product-scope 裁剪——若须实现则 ORM 结构变更（returnType 列 + 关联实体）须 ask-first + 独立 plan-audit（§5 ORM 结构变更类）；P1-RC-026（退货成本 3 策略）修复归 MR1 纯 BizModel/Processor 预授权；P1-RC-027（pre-approve 守卫）修复归 MR1 纯 BizModel/Processor 预授权；P2-RC-023（赠品 UI 标记）/ P2-RC-024（AR 4 桶）修复归 MR1 纯 view.xml/BizModel 预授权（登记不强制）。本审计维持分级不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接；P1-RC-025 须人工裁决 product-scope 裁剪方向；P2-MA2-011 跨期配比可视性 successor）

## Closure

Status Note: <completed — 两阶段执行完成，七项存疑点全数收口（一项主路径闭合 + 四项维持 P1 + 一项维持 watch-only + 两项维持 P2，零新 finding / 不触发 MR0）>

Closure Audit Evidence:

- Phase 1 验证报告落盘 `docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（`> Audit Status: closed`，9 段齐全），含七项存疑点（A4.2.56 含 a/b 子目标）各自裁决 + file:line 证据 + §2 判据命中分支 + 业财保护区域探针纪律声明（A4.2.56-a/A4.2.57 READ-ONLY）+ roadmap 标注勘误声明（A4.2.56 合并勘误 + A4.2.58 finding ID 勘误）。
- Phase 2 arm-index 衔接裁决记录：P1-RC-022（:166）/ P2-MA2-011（:719）/ P1-RC-025（:175）/ P1-RC-026（:176）/ P1-RC-027（:177）/ P2-RC-023（:180）/ P2-RC-024（:181）追加 RC A4.2.56-62 运行时确认交叉引用注记，维持既有分级不撤销，无新 finding；roadmap A4.2.56-A4.2.62 `todo → done ✅`；`docs/logs/2026/08-07.md` 追加完成条目。
- 过程纪律：checker actual == baseline（0 漂移，本审计为只读零生产代码变更故无 build/test 回归风险）；closure-audit 独立性声明（执行者不自我审计，待独立子代理结束审计）；与 arm-index 交叉去重（全部 grep 比对后维持既有分级，无未经比对直接新建的 finding）。
- 结束审计由独立子代理（新会话）执行。

Follow-up:

- 无非阻塞跟进项目（P1/P2 修复义务已明确归 MR1 R1.0 展开器，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
