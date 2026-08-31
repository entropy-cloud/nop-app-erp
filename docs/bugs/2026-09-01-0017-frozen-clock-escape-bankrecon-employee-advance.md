# finance 冻结时钟迁移逃逸：BankReconAutoReverseJob / EmployeeAdvanceCashRepay 快照内嵌日历值，月初滚动必红

> 来源：mission `comprehensive-test-data-and-visual-coverage` VERIFY 步（2026-09-01，系统日期跨月当日）
> 关联：`docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`（同族根因；本条为其「冻结时钟基础设施落地后」的**测试侧逃逸残例**）
> 状态：**closed**（当轮修复并复绿，见下）

## 问题

- 2026-09-01（月初）fin-service 全量 `mvn test`：533 用例 **3 errors**，失败模式与 2026-08-01 家族完全一致——`erp_fin_accounting_period` 快照行字面 `NAME=2026-08 / MONTH=8` vs 实际 `2026-09 / 9`：
  - `TestErpFinBankReconAutoReverseJob.testCurrentMonthReconNotReversed`（1 error，`MONTH 9 vs 8`）
  - `TestErpFinEmployeeAdvanceCashRepay.testCashRepayFullAmountSettlesAndGeneratesVoucher` / `testCashRepayPartialAmountUpdatesFields`（2 errors，`NAME/MONTH 2026-09/9 vs 2026-08/8`）
- 严重性：中（月初当天套件红嗓音，易误判为最近变更回归；月内其余时段绿）。

## 根本原因

- 两测试类的 seed/快照**按录制日所在月固化日历值**，且未接 `FinFrozenClockExtension`：
  - `TestErpFinBankReconAutoReverseJob`（plan 2026-08-07-1932-3 引入，晚于冻结时钟基础设施）seed「当月」期间用裸 `LocalDate.now()`（同时违反平台规则：取当前时间必须走 `CoreMetrics`，否则自外于 `IClock`/TestClock 时间线）；快照行 `YEAR/MONTH` 未掩码。
  - `TestErpFinEmployeeAdvanceCashRepay`（plan 2026-07-18-0718-2 引入）`seedCurrentMonthOpenPeriod()` 用 `CoreMetrics.today()`（合规）但类级未冻结时钟，快照行 `CODE/NAME/YEAR/MONTH` 字面录制 2026-08。
- 同类路径的兄弟测试 `TestErpFinEmployeeAdvanceCashRepayReversal` 已正确接 `@RegisterExtension static FinFrozenClockExtension`（快照对齐参考日 2026-07）——本条两类是迁移遗漏，非机制缺失。

## 判别要点

- 失败字段均为日历派生列（`NAME/CODE/YEAR/MONTH`），业务断言（Java assert 层）全绿；voucher 日期列因 `@var:` 机制运行相对化，不受跨月影响——据此可直接定位为快照日历时间炸弹而非业务回归。
- 当日代码变更（仅注释 + 新增无关测试类）不可能影响期间语义，佐证为时钟滚动。

## 修复（已落地）

- 两测试类补 `@RegisterExtension static FinFrozenClockExtension finClock`（复用 `app.erp.common.test.AbstractFrozenClockExtension` 线程本地机制，仅冻结日期，不动 `currentTimeMillis`/`nanoTime`）。
- `TestErpFinBankReconAutoReverseJob` 裸 `LocalDate.now()` → `CoreMetrics.today()`（生产侧 `ErpFinBankReconAutoReverseHelper` 扫描 cutoff 本就读 `CoreMetrics.currentDate()`，冻结后「当月」seed 与扫描口径同源）。
- 3 个快照期间行对齐 fin 参考日 2026-07（对齐兄弟冻结测试的既有约定；`START_DATE/END_DATE` 沿用 `*` 掩码）。
- 复绿：fin-service `mvn test` 533/533 + `app-erp-all` 69/69 全绿（BUILD SUCCESS）。

## 未来重构注意事项

- 新增「seed/断言含当前期间」的测试时，**必须**同时接域冻结时钟扩展并对齐参考日快照；评审时以「该测试在任意系统日期运行是否同绿」为检查项。
- 快照中出现字面年月值（`20XX-XX`）且 seed 源自 `today()`，即为时间炸弹特征。

## 预防差距

- 冻结时钟迁移（2026-08-01 计划）无「逃逸检测」：未 sweep「`today()`/`now()` seed + 字历字面快照」组合。后续可在 compliance/audit 轮加一条静态检查（测试类引用 `CoreMetrics.today()` 且 `_cases` 快照含 `\d{4}-\d{2}` 字历列 → 提示接冻结时钟或掩码）。
