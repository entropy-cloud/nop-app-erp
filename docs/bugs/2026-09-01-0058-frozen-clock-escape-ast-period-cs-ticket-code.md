# ast/cs 冻结时钟迁移逃逸：折旧期间 PERIOD 与工单号 TK 月前缀快照内嵌日历值，月初滚动必红

> 来源：mission `2026-08-29-1913-1-page-graphql-to-rest-migration` 收尾验证（2026-09-01，系统日期跨月当日，全 reactor `mvn test -fae`）
> 关联：`docs/bugs/2026-09-01-0017-frozen-clock-escape-bankrecon-employee-advance.md`（同族根因，fin 侧同日已修复并 closed；本条为其 **ast/cs 侧未迁移残例**）
> 状态：**open**（本轮不修，归属独立冻结时钟对齐切片，见文末 successor）

## 问题

2026-09-01（月初）全 reactor `mvn test -fae`（HEAD `211572283`，与 2026-08-31 全绿基线记录同一 commit，零代码差异），2 个模块 13 errors，全部为日历派生值快照失配：

- **module-assets** `TestErpAstMaintenance`（5 errors，15 用例中）：`erp_ast_depreciation_schedule.PERIOD` 字面 `2026-08` vs 实际 `2026-09`（`nop.err.match.field-value-not-expected`）。涉及 testReverseCapitalizeRollsBack / testCapitalizePathIndependentCreditsBank / testCapitalizePathWithDepreciationRecalc / testReverseCapitalizeLinkedVisitCreditsClearingRollsBack / testCapitalizePathLinkedVisitCreditsClearing。
- **module-cs** `TestErpCsCatalogFulfillmentEngine`（1 error，16 用例中）：`erp_cs_ticket_action.CONTENT` 工单号 `TK2026090001` vs 快照 `TK2026080001`。
- **module-cs** `TestErpCsTicketCreateEnrichment`（7 errors，8 用例中）：`erp_sys_notification` SUBJECT/BODY/PAYLOAD_JSON 内嵌工单号 `TK202609*` vs `TK202608*`；`nop_sys_sequence` 行 id `cs_ticket_code_seq_202608` 不存在（实际生成 `202609`）。

## 根本原因

与 0017 同族：测试 seed/快照按录制日所在月固化日历派生值（期间 PERIOD、工单编码 `TK+YYYYMM+序号`、序列行 id `cs_ticket_code_seq_YYYYMM`），测试类未接域冻结时钟扩展。08-31 录制基线时月值为 2026-08 全绿；09-01 时钟滚动即红。业务断言（Java 层）全绿，失败全部集中在 `check-match-fail`/`output-row-not-exists` 的日历字面列——快照时间炸弹特征，非业务回归。

## 判别要点

- 失败值全部含 `2026-08`→`2026-09` 滚动特征（PERIOD / TK 前缀 / seq id 月份段）。
- 当日执行零生产代码变更（执行计划为 page.yaml 资源迁移，且该批次此前已提交），排除回归归因。
- hr/drp 的 08-31 已知失败本轮**通过**（drp `TestErpDrpCrossDock` 等 98/98 绿），进一步佐证 08-31 那批失败亦与时钟/数据时点相关，非稳定回归。

## 修复方案（successor 切片执行，未落地）

按 0017 已验证配方：

1. 两域测试类补 `@RegisterExtension` 域冻结时钟扩展（复用 `app.erp.common.test.AbstractFrozenClockExtension` 线程本地机制，仅冻结日期）。
2. 快照行日历派生列对齐冻结参考日（fin 侧约定参考日 2026-07），不适用的列用 `*` 掩码（`PERIOD`、`TK2026080001`→掩码月份段或对齐参考日）。
3. `nop_sys_sequence` 断言行 id `cs_ticket_code_seq_202608` 随冻结参考日对齐（或掩码月份段，若平台序列行 id 不参与业务断言）。
4. 复绿命令：`mvn test -pl module-assets/erp-ast-service,module-cs/erp-cs-service` + 全 reactor `-fae` 复跑确认无新逃逸域（按 0017 经验，逐域排查「`today()` seed + 日历字面快照」组合）。

## 预防差距

同 0017「预防差距」节：冻结时钟迁移缺「逃逸检测」sweep；ast/cs 为第 2、3 个逃逸域，提示需一次全仓静态排查（测试类引用 `CoreMetrics.today()`/日期派生编码 且 `_cases` 快照含 `\d{4}-\d{2}`/`TK20\d{6}` 字历字面 → 提示接冻结时钟或掩码），并在 compliance/audit 轮落地检查项。
