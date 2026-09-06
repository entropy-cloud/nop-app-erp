---
status: active
mission: ai-check-r3
work-item: M0.2+M0.3+M0.4
group: "2026-09-06-1451"
verify: [test]
---

# 2026-09-06-1451-2 M0.2+M0.3+M0.4 CJK 硬编码检测脚本、基线快照与页面 yaml 源头链探针

## Current Baseline

（实仓核验 2026-09-06）

- `tools/check-hardcoded-cjk.mjs` **不存在**；`tools/` 目录已有 30+ 同型检测脚本范式（`check-bizmodel-annotations.mjs`、`check-orm-relations.mjs` 等）与 `mission-driver.sh`；Node 工具链可用（`.mjs` 惯例成熟）。
- `docs/audits/cjk-baseline.md` **不存在**（本计划创建，承载基线快照 + 白名单登记账）。
- 本轮唯一规范执行目录 `docs/audits/check/<YYYY-MM-DD-HHmm>-ai-check-r3/` 尚未创建；`docs/audits/check/` 已有历史轮目录（r1 报告行入 `ai-check-index.md`、r2 目录 `2026-08-28-2049-ai-check-r2`）——历史轮次目录只读，新开一轮执行才建新时间戳目录（roadmap 横切关注点 9）。
- 范式文件在位：`docs/audits/i18n-coverage-checker.sh`（F15 交付，view.xml + action-auth 层 390 文件 PASS，**不扫 Java 运行时面与 page/flux yaml**——本轮新脚本只补盲区，不重建既有 checker）；`docs/audits/compliance-baseline.md`（数值基线 + 单向收紧 + 调高须独立计划裁决的门控范式，R2c=1542 现值）。
- 2026-08-31 探针口径（脚本须复核并以脚本口径冻结，偏差显式登记）：LOG 含中文 335/483；异常路径中文参数约 195（throw 粗口径 171 + `.param` 精确 24）；非注释运行时字符串 CJK 1813 行；`return "中文"` 13 + 中文 String 常量 20 + `@Description("中文")` 约 22；`*.page.yaml`/`*.flux.yaml` 含 CJK 2183 行 / 文件数约 121~147（`i18nEn` 仅 1 处）；Java 注释中文约 20,555 行（CAT-5 豁免不计）。
- `docs/testing/known-good-baselines.md` 最新行 2026-09-04：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；app-erp-all 70/0/0/1；compliance checker R2c=1542 零漂移；全 reactor 2 个预存失败（hr `TestErpHrDepartmentPositionDeleteGuard` / drp `TestErpDrpCrossDock`）——本计划四项基线记录与 MV.1 零新增失败对照的现值。
- 依赖：`docs/architecture/i18n-compliance.md`（M0.1，plan `2026-09-06-1451-1`）为 CAT 分级判定准绳，须先行完成。

## Goals

- 交付确定性 CJK 硬编码检测脚本（五类分级 + 文件级白名单 + `--baseline`/`--strict` 门控 + anti-fake-green 自证），机械违规零遗漏，脚本绿 = 闭环。
- 冻结全量基线快照、初始化本轮唯一规范执行目录、记录四项基线（build / test / compliance checker / i18n-coverage-checker）并登记 known-good-baselines `ai-check-r3-m0` 行。
- 产出页面 yaml 源头链修复策略矩阵（codegen 产物 vs 手写页逐类判定），供 MI.7/MI.8 消费。

## Non-Goals

- 不修复任何 CJK 违规（红 → 绿是 MI.x 修复批的义务；本计划只落红线）。
- 不修改任何生产代码、`*Errors.java`、页面 yaml、seed CSV。
- 不扩展 `i18n-coverage-checker.sh` 本身（MI.8 的防复发门控并入 M0.2 脚本时才实施）。
- 不产生 `ck-*` 审计报告（roadmap 横切关注点 13：i18n 工具链产物 = 脚本输出 + 基线登记，不产 ck-* 报告）。

## Phase 1 — M0.2 检测脚本交付

> Skill: none（roadmap M0.2 指定 none；反 fake-green 纪律沿用 `docs/audits/i18n-coverage-checker.sh` 既有范式）
> Item Types: `Add | Proof`
> Prereqs: plan `2026-09-06-1451-1`（M0.1 i18n-compliance.md 判定准绳）

- [x] Add: 新建 `tools/check-hardcoded-cjk.mjs`——扫描 main Java（排除 `target/`、`_gen/`、`src/test/`、注释行）+ `*.page.yaml`/`*.flux.yaml`；五类分级：CAT-1 LOG 语句中文 / CAT-2 异常路径中文参数（口径含「throw 语句至分号携带 CJK 字符串字面量」+ `.param(...)` 精确口径）/ CAT-3 运行时字符串中文（`return "中文"`、中文 String 常量、setter 默认业务数据、拼接型；**文件级白名单豁免机制**）/ CAT-4 页面 yaml 中文无 `i18nEn` / CAT-5 注释（豁免不计违规）。
- [x] Add: `--baseline` 落快照 + `--strict` 门控（任何新增违规即非零退出），基线文件落 `docs/audits/cjk-baseline.md`——对齐 `compliance-baseline.md` 范式：单向收紧（actual 只降不升），调高须独立计划裁决 + per-site 证据；白名单登记节（格式按 `docs/architecture/i18n-compliance.md` 定义：文件 + 理由 + owner doc 指针 + 裁决来源）。
- [x] Add: anti-fake-green 自证——脚本含可注入缺陷的自检路径（或在 cjk-baseline.md 登记可重放的注入验证程序）：注入一个 CAT-1 样本必须被捕获，移除后恢复绿。
- [x] Proof: 全量扫描跑通，五类分级计数与 2026-08-31 探针数量级一致；口径敏感项（throw 跨行计数、page/flux yaml 文件数）以脚本口径为准，与探针数的偏差逐项登记于 cjk-baseline.md 口径节。（实测冻结值 CAT1=335/CAT2=204/CAT3=390/CAT4=1700，探针 yaml 2183 raw 行精确复现；偏差登记见 `docs/audits/cjk-baseline.md` 口径节）

Exit Criteria:

- [x] `tools/check-hardcoded-cjk.mjs` 全量扫描跑通并输出五类分级计数；计数与 2026-08-31 探针数量级一致，口径敏感项偏差逐项登记于 cjk-baseline.md 口径节（失败模式：脚本无法跑通 / 偏差超数量级且无登记解释）。
- [x] anti-fake-green 自证通过：注入一个 CAT-1 样本被捕获（非零退出），移除后恢复绿。
- [x] 本地化验证（解除 Phase 2 依赖）：`--baseline` 成功落快照，且 `--strict` 在注入新增违规时非零退出。

## Phase 2 — M0.3 基线快照与执行目录初始化

> Skill: none（roadmap M0.3 指定 none）
> Item Types: `Add | Proof`
> Prereqs: Phase 1（脚本可用）

- [x] Add: `mkdir -p docs/audits/check/<执行时戳>-ai-check-r3/`——本轮唯一规范执行目录（本轮内所有 plan 幂等复用同一路径，不新建别的目录）；目录路径登记于本轮索引头部（`docs/audits/check/` 下本轮目录的 index 或 roadmap 执行登记处）。（实际目录：`docs/audits/check/2026-09-06-1645-ai-check-r3/`，路径登记于该目录 `ai-check-r3-index.md` 头部）
- [x] Proof: 跑 M0.2 脚本 `--baseline` 落全量基线快照（口径冻结：Current Baseline 登记的 2026-08-31 探针数，与 roadmap §目的口径一致），快照同写入执行目录与 `docs/audits/cjk-baseline.md`，两处一致。
- [x] Proof: 记录四项基线于执行目录：`mvn clean install -DskipTests`（对照 156 模块 BUILD SUCCESS）、`mvn test`（预存失败清单对照 2026-09-04 行 hr/drp 两项，零新增）、`bash docs/audits/nop-compliance-checker.sh`（对照 R2c=1542）、`bash docs/audits/i18n-coverage-checker.sh`（对照 PASS）。
- [x] Add: `docs/testing/known-good-baselines.md` 登记 `ai-check-r3-m0` 行（含预存失败清单，作 MV.1 零新增失败对照面）。

Exit Criteria:

- [x] 执行目录 `docs/audits/check/<执行时戳>-ai-check-r3/` 已创建，路径登记于本轮索引头部。
- [x] 全量基线快照双写一致：执行目录与 `docs/audits/cjk-baseline.md` 内容一致（不一致 = 失败）。
- [x] 四项基线记录齐备并对照现值：build 156 模块 BUILD SUCCESS / `mvn test` 仅 hr/drp 两项预存失败且零新增 / compliance R2c=1542 零漂移 / i18n-coverage PASS。（实测 2026-09-06：`mvn test` 4,006 tests / **0 failures**——hr/drp 两项预存失败已由前序修复批解决不再复现，全绿优于对照面；compliance R2c=1542 精确一致；证据 `m0-3-baseline-records.md` + `m0-3-*.log`）
- [x] `docs/testing/known-good-baselines.md` 已新增 `ai-check-r3-m0` 行（含预存失败清单）。

## Phase 3 — M0.4 页面 yaml 源头链探针

> Skill: none（roadmap M0.4 指定 none；生成产物纪律锚 `docs/lessons/` lesson 06）
> Item Types: `Decision | Proof`
> Prereqs: Phase 2（脚本口径冻结 + 执行目录就绪）

- [x] Decision: 成文 codegen 产物 vs 手写页判定标准（路径约定 / delta 覆盖关系 / xgen 溯源核查程序），理由与替代方案（如仅按路径启发式判定）及残留风险记录于 source-map 文档头部——判定错误的代价是 MI.7 改生成物被覆盖（lesson 06），故标准须可机械复核查验。
- [x] Proof: 对含 CJK 的 `*.page.yaml`/`*.flux.yaml`（M0.2/M0.3 冻结口径，全部文件无抽样）逐文件判定生成链并产出修复策略矩阵，落执行目录 `m0-4-page-yaml-source-map.md`：codegen 产物类标注「改 view.xml / xmeta 模型源 + `i18n-en` 属性，经 codegen 重生成验证，禁改生成物」；手写页类标注「直接补 `i18nEn`」；每文件附目标 owner 域与 `docs/design/i18n-glossary.md` 术语表条目引用（文案英译复用 414 token 基准，新词登记扩充）。（实测：全量 147 文件无抽样——39 codegen stub 全部 0 违规 / 108 手写页承载全部 1,700 违规，stub∩违规=空集，机械核查程序三步可重放）

Exit Criteria:

- [x] 判定标准成文于 source-map 文档头部：含理由、替代方案（如仅按路径启发式判定）与残留风险，且附可机械复核查验程序（失败模式：仅启发式无溯源核查步骤）。
- [x] `m0-4-page-yaml-source-map.md` 覆盖冻结口径下全部含 CJK 页面 yaml 文件（无抽样遗漏），每文件含生成链判定、修复策略、owner 域与 `docs/design/i18n-glossary.md` 条目引用。

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-06-1451-2-cjk-detection-baseline-1-30e3981b to opencode-reviewer-2026-09-06-150411
- 2026-09-06：iteration 1，共识 accept #review-2026-09-05-123532-mission-driver-2026-09-06-1451-2-cjk-detection-baseline-1-30e3981b

## Closure Gates

> Ledger 格式：完成状态由引擎派生（Phase 计数项全 `[x]` + `## Verification` pass 行 + `## Closure` 审计回执对），本节为闭包核对散文记录，不承载计数复选框（计数域纪律 01-file-ledger §2.5）。以下 8 项门控已由独立结束审计（2026-09-06）逐项核验通过。

- 范围内行为完成：检测脚本（五类分级 + 文件级白名单 + `--baseline`/`--strict` + anti-fake-green）、基线快照双写、执行目录初始化与索引登记、四项基线记录、known-good-baselines `ai-check-r3-m0` 行、页面 yaml 源头链策略矩阵全部落地
- 相关文档对齐：`docs/audits/cjk-baseline.md`、`docs/testing/known-good-baselines.md`、本轮索引登记与实仓状态一致
- 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict` 绿（anti-fake-green 注入路径重放通过）；四项基线命令复跑零新增失败（`mvn clean install -DskipTests` / `mvn test` / `bash docs/audits/nop-compliance-checker.sh` / `bash docs/audits/i18n-coverage-checker.sh`）
- 无范围内项目降级为 deferred/follow-up
- 独立草案审查已完成并记录
- 文本一致性已验证：状态、阶段、门控和日志都一致
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为占位符
- 结束证据存在于文件中（执行目录产物 + Closure 节）

## Verification

- pass test ai-check-r3-m0-3-mvn-test exit=0 全 reactor `mvn test` BUILD SUCCESS：4006 tests / 0 failures / 0 errors / 1 skipped（2026-09-06T17:04 完成，原始日志 `docs/audits/check/2026-09-06-1645-ai-check-r3/m0-3-mvn-test.log`；独立审计实仓复核零生产代码变更，证据有效）
- pass cjk-strict ai-check-r3-m0-2 exit=0 `node tools/check-hardcoded-cjk.mjs --strict` exit 0：冻结基线零新增（CAT1=335/CAT2=204/CAT3=390/CAT4=1700，453 文件；独立审计现场复跑）
- pass cjk-self-test ai-check-r3-m0-2 exit=0 `node tools/check-hardcoded-cjk.mjs --self-test` 10/10 PASS（独立审计现场复跑）
- pass compliance-checker ai-check-r3-m0-3 exit=0 `bash docs/audits/nop-compliance-checker.sh` exit 0，R2c=1542 零漂移（独立审计现场复跑）
- pass i18n-coverage ai-check-r3-m0-3 exit=0 `bash docs/audits/i18n-coverage-checker.sh` PASS exit 0（独立审计现场复跑）
- pass test ai-check-r3-verify-2026-09-06-1905 exit=0 mission-driver verify run：全 reactor `mvn clean install -DskipTests`（5:09，156 模块 SUCCESS）+ `mvn test`（47:51）双 BUILD SUCCESS，Results 行精确汇总（含 app-erp-all `[WARNING] Tests run` 行）4006 tests / 0 failures / 0 errors / 1 skipped，与 `ai-check-r3-m0` 基线行精确一致、零新增失败；附属复跑 `--strict`/`--self-test` 双绿
- pass test ai-check-r3-verify-2026-09-06-2056 exit=0 mission-driver verify run：全 reactor `mvn clean install -DskipTests`（2:05 min，156/156 模块 SUCCESS）+ `mvn test`（18:30 min）双 BUILD SUCCESS exit 0，模块 Results 行汇总（INFO 40 模块 + app-erp-all `[WARNING] Tests run` 行 70/0/0/1）= 4006 tests / 0 failures / 0 errors / 1 skipped，与 `ai-check-r3-m0` 基线行精确一致、零新增失败

## Closure

Status Note: 全部 3 Phase 执行项目与退出标准 19/19 勾选且经独立结束审计实仓核验落地：检测脚本（五类分级 + 文件级白名单 + `--baseline`/`--strict` 单向收紧 + anti-fake-green 双通道自证，现场复跑 `--strict`/`--self-test` 双绿）、基线快照双写一致（diff 为空）、执行目录 `docs/audits/check/2026-09-06-1645-ai-check-r3/` 初始化与索引登记、四项基线记录（同日原始日志在案，`mvn test` 4006/0/0/1 全绿零新增失败）、known-good-baselines `ai-check-r3-m0` 行、147 文件页面 yaml 源头链策略矩阵全部落地；零生产代码变更（git status 复核），`docs/logs/2026/09-06.md` 与 roadmap 同步；无范围内项目降级。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（mission-driver CLOSURE_AUDIT 步派发，新会话，无执行者上下文）
- Evidence: 现场复跑 `node tools/check-hardcoded-cjk.mjs --strict`（exit 0，冻结值 CAT1=335/CAT2=204/CAT3=390/CAT4=1700）+ `--self-test`（10/10 PASS）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，R2c=1542）+ `bash docs/audits/i18n-coverage-checker.sh`（PASS exit 0）；执行目录产物（`ai-check-r3-index.md`、`m0-3-baseline-records.md`、`m0-3-*.log`、`m0-3-cjk-baseline-snapshot.md`、`m0-4-page-yaml-source-map.md`）与 `docs/audits/cjk-baseline.md`、`docs/testing/known-good-baselines.md` `ai-check-r3-m0` 行、`docs/logs/2026/09-06.md` 逐项核对一致

- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-09-06-1451-2-cjk-detection-baseline-1-eff103e7 to opencode-closure-auditor-2026-09-06-1752 models={exec:glm-5.3-flash,aud:glm-5.3-flash}
- accepted #audit-2026-09-05-123532-mission-driver-2026-09-06-1451-2-cjk-detection-baseline-1-eff103e7：独立结束审计通过——19/19 计数项实仓核验落地，`--strict`/`--self-test`/双 checker 现场复跑全绿，四项基线同日日志在案零新增失败，docs 同步一致；审计员按 plan-guide Minimum Rule 15 修复权限完成两处结构修正（Closure Gates 计数域外复选框转散文、`## Verification` pass 行补录）并补记本回执对。

Follow-up:

- 无（compliance checker R2b=242/R12a=71 相对 `compliance-baseline.md §BASELINE` 机器可读块的差值，归 compliance-baseline owner 流程 successor 处置，非本计划范围——见执行目录 `m0-3-baseline-records.md` 注记）
