# ai-check-r2 M0.1 — 基线快照

> 落盘时间：2026-08-28-2049
> 路径：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-1-baseline-snapshot.md`
> 多次执行隔离：本文件归本轮 M0.1 子目录；不写到 `docs/audits/check/` 扁平空间。

## 来源

基线快照不重复运行 mvn install/test，**直接引用 `docs/testing/known-good-baselines.md` 最新全量行作为本轮工作树基线**——2026-08-28 已建立全量 3947 tests/0/0/1/669 的全绿基线（`docs/testing/known-good-baselines.md` 同日 0219-2 计划批行），距本 M0.1 落盘 4 小时内无后续执行面（git status 无生产代码变更），可直接锚定。

## 锚定行（引用，不重跑）

| 字段 | 值 |
|---|---|
| 日期 | 2026-08-28 |
| 来源 | local |
| 执行面 | `mvn clean install -DskipTests`（156 reactor 模块 BUILD SUCCESS，01:40）+ `mvn test`（全 reactor，surefire XML 权威聚合 **3947 tests / 0 failures / 0 errors / 1 skipped / 669 报告文件**）|
| 已知失败 | `ErpAllWebPagesCollectTest` `@Disabled`（JDK26/ANTLR 兼容性，预存 known failure）|
| evidence | `docs/testing/known-good-baselines.md` 2026-08-28 0219-2 行（与 0219-1 行 3941/668 差量 +6 tests/+1 文件全额归因 plan-0219-2）|

## compliance checker 锚定

| 规则 | baseline 锚定值 | M0.1 引用依据 |
|---|---|---|
| R1d | 14 | 2026-08-23 V.1 行 + 2026-08-28 mfg cost guard + ast depot 接 dao 等已规范登记 |
| R2a | 34 | 同上 |
| R2b | 237-240 | 2026-08-20 RC-R1.89 闭包行 + 后续 fin 派生登记 |
| R2c | 1505-1529 | RC-R1.89 闭包基线 + 2026-08-25 closePeriod FX flush 修复 0 漂移 |
| R2d | 38 | 同上 |
| R3 | 5 | 平台反模式零增量 |
| R4 | 0 | `extends RuntimeException` 零 |
| R5 | 0 | `@Inject private` 零（2026-07-31 R3.4 修复归零） |
| R6 | 2 | 同范围 |
| R7 | 0 | 系统时钟直读零（除 R7 盲区变体 LocalDateTime.now/YearMonth.now 登记） |
| R8 | 0 | Processor xbiz 接线 2026-08-01 归零 |
| R10 | 12 | 同范围 |
| R11 | 0 | 状态判断方法重复零 |
| R12a/b/c | 70/66/41 | 同范围 |

> 锚定行：2026-08-20 V.1 行（RC-R1.89 闭包）+ 2026-08-25 closePeriod FX flush 修复不漂移 + 2026-08-27 F1-F2.1 批 + 2026-08-28 E3.5 三批（3947/669 行）+ 2026-08-25 mfg 1 pre-existing + drp 7 pre-existing。

## 验证协议（M0.1 收官）

- **本 plan 不重跑 mvn install/test**（避免 13 分钟测试时长与已锚定的最新基线行覆盖冲突；本轮零代码改动基线无变化）
- **M0.4 收官**（基线检查阶段关闭）须 `git status` 确认仅 docs 变更（ai-check-r2 子目录新增 + ai-check-r2-index.md 落盘 + known-good-baselines.md 追加 `ai-check-r2-m0` 行）
- **不重复登记既有 baseline 行**（避免污染权威 known-good-baselines.md；本 M0.1 仅引用既有 3947/669 行）

## 已知 follow-up（与 M0.1 收口无关，留待后续 mission 评估）

- mfg 1 pre-existing（`TestErpMfgCompletionPosting` LOCATION_ID 漂移——08-20 已记录为预存 baseline，非本 mission 引入）
- drp 7 pre-existing（`IErpSysNotificationBiz` 测试隔离——M4.1 修复 + 2026-08-23 复核段仍记录为预存，非本 mission 引入）
- 36 flux-picker + visual 像素套件（`require("react")` 已修复，2026-08-04 残 vendor-react chunk；`tests/e2e/visual/` 像素套件归 flux 视觉基线 successor，不在 ai-check-r2 范围）

## 落盘物

- 本文件 `m0-1-baseline-snapshot.md`（基线快照索引）
- 后续 M0.2 / M0.3 / M0.4 在本子目录继续
- ai-check-r2-index.md（本轮统一索引）落盘于父目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/`
