# M0.3 四项基线记录（ai-check-r3）

> 记录时间：2026-09-06（mission `ai-check-r3`，plan `docs/plans/2026-09-06-1451-2-cjk-detection-baseline.md` Phase 2 实测）
> 执行目录：`docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，路径已登记于本轮索引头部 `ai-check-r3-index.md`）
> 工作树状态：dirty——本计划产物（`tools/check-hardcoded-cjk.mjs` 新增、`docs/audits/cjk-baseline.md` 新增、本执行目录、计划勾选）；零 `module-*`/`app-erp-all` 生产代码改动（M0 范围纪律）
> 原始日志：`m0-3-mvn-install.log` / `m0-3-mvn-test.log` / `m0-3-compliance-checker.log` / `m0-3-i18n-coverage-checker.log`（同目录）

## 四项基线实测与对照

| # | 基线命令 | 实测结果 | 对照现值（2026-09-04 known-good 行 / plan Current Baseline） | 判定 |
| --- | --- | --- | --- | --- |
| 1 | `mvn clean install -DskipTests` | **BUILD SUCCESS**（156 reactor 模块，01:57 min，exit 0） | 156 模块 BUILD SUCCESS | ✅ 一致 |
| 2 | `mvn test`（全 reactor） | **BUILD SUCCESS：4,006 tests / 0 failures / 0 errors / 1 skipped**（exit 0；逐类 `Tests run` 汇总 719 行） | 2026-09-04 行：3,975 tests / 1 failure（hr `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed` 预存）/ 1 error（drp `TestErpDrpCrossDock#testStagingTimeoutFallbackJob` 预存） | ✅ **优于基线**：零新增失败；且两项预存失败**已不再复现**（hr 8/0/0/0、drp `TestErpDrpCrossDock` 14/0/0/0 全绿——由 09-04 后的 ai-check 修复批吸收解决），MV.1 零新增失败对照面以本行 0-failure 干净底账为准 |
| 3 | `bash docs/audits/nop-compliance-checker.sh` | **exit 0**；R2c=**1542**（R1d=14 / R2a=34 / R2b=242 / R2c=1542 / R2d=38 / R3=5 / R6=2 / R10=14 / R12a=71 / R12b=66 / R12c=42；R1a-c/R4/R5/R7/R8/R11=0） | 2026-09-04 行：exit 0，R2c=1542 零漂移 | ✅ R2c=1542 精确一致，零漂移 |
| 4 | `bash docs/audits/i18n-coverage-checker.sh` | **exit 0，PASS**（373 文件：354 view.xml + 19 action-auth.xml；DEFECTS=0 / COVERAGE GAPS=0） | 对照 PASS（F15 门控） | ✅ 一致 |

## 合规 checker 与 compliance-baseline 机器可读块的已知注记

checker 为纯 reporter（gate 逻辑在 CI `compliance.yml`）。本次实测 R2b=242 / R12a=71 高于 `compliance-baseline.md §BASELINE` 块的 240/70（R2c=1542 高于块值 1537）——该差值即 known-good-baselines 2026-08-31/09-04 行已登记的「+5 已被 ai-check 批/后续合法吸收」同源漂移（checker 命中演进而机器可读块未随附上调注记），**非本计划引入**（本计划零生产代码变更，M0.6 收官时以 `git status` 复核）。此漂移的 baseline-raise 裁决归 compliance-baseline owner 流程（successor），不阻塞本计划：本计划基线对照面以 known-good-baselines 2026-09-04 行现值（R2c=1542）为准且已精确一致。

## CJK 基线快照（M0.2 脚本口径冻结）

- 冻结值：CAT-1=335 / CAT-2=204 / CAT-3=390 / CAT-4=1,700（违规承载文件 453；CAT-5 注释 CJK 20,967 行豁免仅统计）。
- 探针对照与偏差：见 `docs/audits/cjk-baseline.md §口径（脚本口径冻结）`（2026-08-31 探针 2183 raw 行精确复现、CAT-1 335 精确一致，CAT-2/3/4 偏差逐项登记）。
- 双写一致性：`docs/audits/cjk-baseline.md` SNAPSHOT 块与本目录 `m0-3-cjk-baseline-snapshot.md` **diff 为空**（generated 2026-09-06T09:12:45.292Z 同一快照体）。
- 门控验证：`node tools/check-hardcoded-cjk.mjs --strict` 当前绿（exit 0）；anti-fake-green 注入重放（注入 CAT-1 样本 exit 1 → 移除恢复 exit 0）已于 Phase 1 执行通过，重放程序登记于 cjk-baseline.md §anti-fake-green 自证。
