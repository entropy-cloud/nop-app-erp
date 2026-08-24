# 2026-08-25 closePeriod 同事务 FX 凭证未 flush → 损益结转凭证永缺 FX 腿（P1 会计正确性）

## 现象

closePeriod（含反结账后重结账）生成的 PERIOD_CLOSE 损益结转凭证**永缺汇兑损益（FX）结转腿**，且试算平衡表快照同样缺失同事务期末凭证（FX + PL 两凭证全部）腿：

- C13 场景（期间收入 1130 + FX 损失 150）：PERIOD_CLOSE = Dr 5001 1130 / Cr 4103 1130（1130），应为 Dr 5001 1130 + Dr 4103 150 ↔ Cr 4103 1130 + Cr 6603 150（1280）。
- 结账后汇兑损益科目（6603）净额**不归零**（净借 150 残留），违反 `period-close.md §步骤5`（费用类科目结转至本年利润；「结账后汇兑损益科目余额归零」为派生推论，佐证 `ar-ap-reconciliation.md:297` 重估先于结账排序）。
- 两套件对等价业务场景（期间收入 + FX 损益）钉死相反结转总额：fin-service `TestErpFinProfitLossClosing.testProfitLossClosingIncludesFxGainLoss`（跨 session 已 flush）断言含 FX（150）；app-erp-all `TestErpC13FinPeriodCloseReverse`（同事务）断言不含 FX（1130）——同型业务两语义。

## 根因

`ErpFinAccountingPeriodProcessor.closeGlModule:165-170`：`exchangeRevaluationService.revalue()` 经 `CloseVoucherWriter.writeVoucher`（voucherDao/lineDao/billRDao 直接 saveEntity，**无 flush**，CloseVoucherWriter.java:104/128/136）写 FX 重估凭证后，紧接 `profitLossClosingService.close()` 以 `findAllByQuery` DB 直查聚合（ProfitLossClosingService.java:79）——同事务未 flush 的 FX 凭证行对聚合不可见。

同型放大：`populateTrialBalanceForAllSchemas`（试算平衡快照，DB 直查）在 PL 凭证写入后执行，同样见不到未 flush 的 FX/PL 凭证（C13 快照 `erp_fin_trial_balance.csv` 实证：仅 seed 科目行，无 6603/4103 腿）。closeAnnual（AnnualCloseService 同事务聚合 CLP 凭证）静态同型不可见（12 月 config-gated 分支，C13 不覆盖）。

反结账→重结账路径同样缺腿：红字凭证被 isReversed 过滤（语义正确）+ 新 FX 凭证同事务未 flush → 任何 closePeriod 路径下 FX 腿均结不出。

## 证据链

- 源审计：`docs/audits/2026-08-24-2233-open-audit-integration-test.md` P1 发现 OA-02（首轮发现 + 代码级复核确认）。
- 实仓锚点：`ErpFinAccountingPeriodProcessor.closeGlModule`（revalue → close 无 flush 边界）、`CloseVoucherWriter.writeVoucher`（saveEntity 无 flush）、`ProfitLossClosingService.closeForSchema:79`（DB 直查聚合）、flush 先例 `ErpFinAccountingPeriodClosePeriodProcessor:95`（`facade.orm().flushSession()`）。
- 快照实证：C13 `erp_fin_trial_balance.csv` 缺 6603/4103 腿；`TestErpC13FinPeriodCloseReverse` PL_TOTAL=1130 vs fin-service FX 语义 150。

## 影响

任何启用汇兑重估（`erp-fin.exchange-revaluation-enabled`，默认 true）且当期存在外币暴露的 closePeriod：本年利润少计 FX 损益、汇兑损益科目结账后余额不归零、试算平衡表漏期末凭证腿。P1 会计正确性。

## 修复

选型③写侧自防御：`CloseVoucherWriter.writeVoucher` 末尾统一 `flushSession()`（对齐 `facade.orm().flushSession()` 先例）——单点覆盖全部 6 调用方（ProfitLossClosing/ExchangeRevaluation/AnnualClose/BadDebtProvision/AbstractErpFinReconciliation/ErpFinBadDebt）及后续同事务 DB 读（PL 聚合/试算平衡/年度结转聚合）。AST 折旧凭证走 `IErpFinVoucherBiz.post` REQUIRES_NEW 已提交可见、INV 成本重算不产 GL 凭证且自带 flush——无需额外边界。落地：plan `docs/plans/2026-08-25-0330-2-fix-closeperiod-fx-flush-profit-loss.md`（含 C13 断言/快照翻转 1130→1280 与 owner-doc 裁决）。

## 状态

fixed（2026-08-25，plan 2026-08-25-0330-2；同型 closeAnnual 聚合可见性经选型③顺带覆盖，年度结账端到端验证仍归 successor 触发条件）
