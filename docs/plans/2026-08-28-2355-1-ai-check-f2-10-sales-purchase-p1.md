# 2026-08-28-2355-1 ai-check F2.10 sales+purchase P1 簇修复

> Plan Status: done（**用户人工批准独立草案审查**——2026-08-28，用户「现在全部人工批准，继续目标」授权全部 plan 人工批准；子 agent 通道结构性不可用（7/7 启动失败），按 plan-guide #12 以人工审查作为独立审查替代，同 F2.4-F2.9 先例）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/ai-check-roadmap.md` F2.10（todo）；`docs/audits/check/ck-sales.md`（sal-001/002/004）+ `ck-purchase.md`（pur-001/002/003）
> Related: `2026-08-28-2340-1`（F2.9）/ `docs/design/sales/returns.md` / `docs/design/purchase/state-machine.md`
> Audit: required（保护区域修复须独立 plan-audit——本批经用户人工批准替代）

## Purpose

修复 sales+purchase P1 簇（sal-004 已由 F1.3 修复、pur-002/003 已由 F1.2/F1.3 修复，本批处理余项）：

- **P1-CK-pur-001（D3）** 采购看板主查询消费 docStatus=ACTIVE 死状态（全域零 writer）→ KPI/趋势/TOP N/及时率/预警恒空。
- **P1-CK-sal-001（D6/D8）** ReturnRefundOrchestrator 客户级全量反转核销——不限于退货关联发票（无关联发票也触发）。
- **P1-CK-sal-002（D3/D6）** sales Dashboard countActiveOrders 死状态 ACTIVE → orderCount/conversionRate 恒 0。
- **P1-CK-sal-003（sales 侧收口）** 延迟过账 sweep 重试成功后源单 posted 不回写 → cancel/reverseApprove 改按凭证存在性判红冲前置。

## Execution（全部完成，见 Closure Evidence）

- pur-001：`ErpPurDashboardBizModel` 三处 `eq(docStatus, ACTIVE)` → `and(eq(approveStatus, APPROVED), ne(docStatus, CANCELLED))`。
- sal-002：`ErpSalDashboardBizModel.countActiveOrders` 同口径修复。
- sal-001：`ReturnRefundOrchestrator.orchestrateRefund` 限定退货关联发票（退货行 deliveryLineId → 发票行链路，对齐 `ErpSalReturnProcessor.validateInvoiceNotSettled`）；无关联发票 → 跳过（owner doc returns.md RC-R1.19「无关联发票跳过」）。
- sal-003：`ErpSalInvoiceProcessor.hasActivePosting(code)`（ErpFinVoucherBillR 反查未红冲 AR_INVOICE 凭证）→ invoice reverseApprove/cancel 红冲前置 = `posted || hasActivePosting`。

## Tests

- `TestErpPurDashboard#testKpiCountsApprovedOrdersWithRealDocStatus`（DRAFT+APPROVED 计入、CANCELLED 不计——修复前 ACTIVE 过滤恒 0）
- `TestErpSalDashboard#testKpiCountsApprovedOrdersWithRealDocStatus`（同）
- `TestErpSalReturnExchange#testPriceDifferenceNegativeRefunds` 改断言：无关发票核销不被反转（修复前按客户全量反转）
- `TestErpSalReturnRefund#testReceivedReturnReversesSettlement` 改场景：部分核销（PARTIAL）发票 + deliveryLineId 关联（完全核销被 pre-approve 守卫拒绝是既有正确语义）→ 反向核销行仍生成

## Validation

- `mvn test -pl module-sales/erp-sal-service` 全绿（315+ 测试）
- `mvn test -pl module-purchase/erp-pur-service` 全绿（340+ 测试）
- compliance 零漂移
- ai-check 73/533 → 77/533 fixed（sal-001/002/003 收口 + pur-001）

## Deferred But Adjudicated

### sal-003 finance 侧（F2.1 已修）+ P2/P3 余项

- Classification: `out-of-scope improvement`——sales 侧已收口；P2/P3 在 F3.x
- Successor Required: `yes`

## Closure

Status Note: 4 P1 finding 处理（3 修复 + 1 联动收口）→ F2.10 → done，ai-check 73/533 → 77/533 fixed。

Closure Audit Evidence:
- Reviewer / Agent: **用户人工批准**（独立 closure-audit 替代——子 agent 通道不可用，按 plan-guide #13 记录 successor trigger：子 agent 通道恢复或用户人工裁决；F2.4-F2.9 同模式已获批）
- Evidence: 见 Execution/Tests/Validation 三节（2 新 dashboard 测试红→绿 + exchange/refund 测试改断言反映修复语义 + sal 316/pur 341 全绿 + compliance 零漂移）
