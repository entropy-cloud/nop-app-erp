# 2026-09-01-2115 mfg-subcontract-chain 预存 2 失败（红冲凭证 1405 借方 -60 vs 期望 -50）

## 现象

`tests/e2e/orchestration/mfg-subcontract-chain.spec.ts` 2 用例失败（plan `2026-09-01-1245-2-m12a2-manufacturing-seed-expansion` Phase 3 实测）：

1. `full chain: approve → issue → receive → post fee`（:114）
2. `reverseCompletion: COMPLETED→CANCELLED + posted=false + 3 reversal GL vouchers (negated) + ...`（:425）

失败断言：`subjectCode=1405 debitAmount` **Expected: -50, Received: -60**（error-context 截图与 trace 留存 `test-results/`）。

## 判定：预存失败，与本批种子无关

对照实验（M1.1b/M1.2a1「移除本批种子重启对照」先例）：将 M1.2a2 批次 26 张 `erp_mfg_*.csv` 移出 `_init-data/` → 重建 runner jar（168 CSV）→ fresh-DB 重启后复跑同 spec——**同样 7 passed / 2 failed，失败用例与断言差值逐字一致**。本批种子纯加性（零 FIRMED cost_rollup + 零默认 BOM，见 owner doc `docs/design/manufacturing/seed-data.md` 干扰面裁决），且委外红冲凭证由用例自建单据驱动，不消费种子行。

## 溯源线索（successor 排查起点）

- spec 最后一次实质修改 = plan `2026-08-23-0434`（id-string-migration E2E String 化），此后委外过账/红冲侧（`ErpMfgSubcontractOrderProcessor.reverseCompletion` → 凭证生成）若发生行为变化（如加工费重算口径、汇率或数量取值变化），spec 期望值（-50）未同步。
- 差值 +10 与 spec 自建数据（费用 50）不成整数倍关系，初判非「种子行混入聚合」，而是**金额计算口径漂移**——建议从红冲凭证生成时 1405 科目行金额的派生式入手（对照 `receiveFinished` 正向凭证的 1405 金额是否同为 -60）。
- 该 spec 不在 `known-good-baselines.md` 最新登记行的覆盖清单内（该行以 `mvn test -pl app-erp-all` + dashboards/reports 视觉为准），漂移窗口无法从基线登记精确圈定。

## 处置

归 successor（独立修复计划：Fix 或 spec 期望值同步，二选一经实仓裁决）。M1.2a2 批次按预存失败登记（同 hr/drp Non-Goal 回归家族待遇），不阻塞本批闭包。
