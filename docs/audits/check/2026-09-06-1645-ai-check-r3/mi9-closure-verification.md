# MI.9 收官验证证据（plan 2026-09-07-1715-3）

> 2026-09-08 执行。原始 maven 日志过大（GB 级）不落盘，本文件固化命令尾段与聚合数字；勾选注记与 `docs/plans/2026-09-07-1715-3-mi9-mi-closure-strict-green.md` 逐项对账。

## 并发污染事件与隔离处置

同机姊妹 agent 会话（opencode `build` agent，plan `2026-09-07-2200-1`）在同工作树并发 mvn 构建 + 源码编辑，污染首轮 4 次全量实跑（hr[117]/md[17]/fin/assets 伪失败，单模块/切片隔离复跑全绿证明非 HEAD 属性；与 `docs/logs/2026-09-02.md` 在案并发构建事故同型）。处置：`git clone` 至 `/tmp/mi9-verify` 钉住 HEAD `6c495dc2e`（0 脏文件）重跑全量序列。姊妹改动为 ASCII ErrorCode 键置换，对 CJK checker 计数与 compliance 计数无影响（真实树复证一致）。

## 命令尾段与聚合数字（clone @ 6c495dc2e）

- `mvn clean install -DskipTests` → exit 0，`[INFO] BUILD SUCCESS`，156/156 `SUCCESS [` 行，Total time 02:07 min
- `mvn test` → exit 0，`[INFO] BUILD SUCCESS`，Total time 33:00 min；Results 块聚合（41 测试模块）：**4006 tests / 0 failures / 0 errors / 1 skipped** = M0.3 基线行精确一致（surefire [INFO] 模块行双执行口径 7941/0/0/0）

## checker 实跑（真实树，稳定窗口复证）

- `node tools/check-hardcoded-cjk.mjs`（report）→ exit 0，CAT1..4 = 0/0/0/0（scanned 3430 java + 886 yaml，per-domain violations 节空，CAT5 = 20992 豁免）
- `--strict` → exit 0，`RESULT: PASS (--strict: 0 new violations vs frozen snapshot; 170 baseline files, totals CAT1..4=0/0/209/1318)`
- `--self-test` → exit 0，`RESULT: PASS (self-test green)` 13/13
- `bash docs/audits/i18n-coverage-checker.sh` → exit 0 `RESULT: PASS (0 defects; gaps informational, --strict OFF)`
- `bash docs/audits/nop-compliance-checker.sh` → exit 0，R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，与 M0.3 快照行逐值一致（零漂移）
- 白名单四要素脚本核对：27 条目全数通过（cats/理由/owner doc/裁决来源 + fs.existsSync），缺要素 = 0

## 对账链

CAT-1 335→0 = 93(MI.2)+165(MI.3)+77(MI.4)；CAT-2 204→0 = 136(MI.5a)+68(MI.5b)；CAT-3 390→0 = 181(MI.6批1)+209(MI.6批2)；CAT-4 1700→0 = 382(MI.8批1)+1318(MI.8批2)——与 `docs/audits/cjk-baseline.md` §批注账四行逐项一致。
