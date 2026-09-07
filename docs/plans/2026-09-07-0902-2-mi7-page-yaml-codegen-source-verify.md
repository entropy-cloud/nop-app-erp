---
status: active
mission: ai-check-r3
work-item: MI.7
group: "2026-09-07-0902"
verify: [test]
---

# 2026-09-07-0902-2 MI.7 页面 yaml CJK 清剿——codegen 源面核验与零违规裁决

## Current Baseline

- M0.4 冻结策略矩阵（`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-4-page-yaml-source-map.md`，plan `2026-09-06-1451-2` Phase 3 产出）：含 CJK 的 `*.page.yaml`/`*.flux.yaml` 全量 147 文件三步机械核查（stub 内容核查 → 引用完整性核查 → 注释误匹配排除）判定为——**codegen 产物（GenPage stub）39 文件，CAT-4 违规合计 0**（全部仅注释含 CJK，CAT-5 豁免不计）；**手写页 108 文件，CAT-4 违规合计 1700**（= SNAPSHOT 冻结值；归 MI.8）。stub∩违规 = 空集（`comm` 实测）。
- 即 MI.7 工作项的前提面（「codegen 产物类改模型源 view.xml `i18n-en`、xmeta 补属性」）在冻结口径下**违规面为零**：39 个 stub 正文仅含 `GenPage` 引用、无任何页面文案节点；页面实际文案由 view.xml 渲染，而 view.xml 层已由 F15 `i18n-coverage-checker.sh` 全覆盖门控（判定准绳表 #7「view.xml 已全覆盖，page/flux yaml 是剩余面」；M0.3 基线登记 F15 绿，MI.1~MI.5b 未触及 view.xml 层）。
- 判定准绳（`docs/architecture/i18n-compliance.md`）：#7 codegen 产物改模型源 `i18n-en`（禁改生成物）+ #8 `_` 前缀生成 yaml 禁手改；纪律锚 `docs/lessons/06-codegen-product-edit-overwrite.md`。
- 防复发门控现状：M0.2 脚本 `tools/check-hardcoded-cjk.mjs --strict` 已覆盖 CAT-4（page/flux yaml 层，基线 886 yaml 文件），stub 若未来被生成链写入 CJK 文案即触发 strict 门控。
- 本计划为 Proof-heavy 核验与裁决计划：不预改任何生产文件；若核验发现冻结矩阵与实仓不符（stub 出现正文违规），按 Phase 2 例外路径改模型源后 codegen 重生成验证（MI.7 原语义）。
- 依赖状态：M0.4 done + MI.4 done（roadmap MI.7 依赖行）；本批执行顺序居本批三计划之第 2。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行 + MI.4/MI.5a/MI.5b 收官零新增失败。

## Goals

- 按冻结矩阵机械复验 codegen 源面：39 stub 分类与 0 CAT-4 违规在当前 HEAD 复现（禁止抽样，全量 147 文件核查程序重放）。
- codegen 重生成链洁净证明：全量 build 触发生成链后 stub 面保持 0 违规，`--strict` 与 F15 双门控绿。
- 裁决记录落地：codegen 源面在冻结口径下零违规、无需模型源变更（或例外路径的修复证据），MI.7 语义闭合。

## Non-Goals

- 不动手写页 108 文件 1700 行 CAT-4（MI.8 范围，本批计划 `0902-3`）。
- 不改 `_` 前缀生成文件、`_gen/`、xmeta 生成物（lesson 06）；不改 view.xml（F15 已全覆盖，无 gap 证据）。
- 不改脚本口径与 SNAPSHOT 块；不动 ORM/api.xml；不建英文 i18n 承载镜像。

## Phase 1 — 冻结矩阵机械复验（全量 147 文件）

> 统一类型：Proof-heavy（2 项 Proof）。
> Skill: none（roadmap MI.7 行指定）
> Targets: 只读核验；产物 = 勾选注记数字（不产 ck-* 报告，横切关注点 13）
> Prereqs: plan `2026-09-06-1451-2`（M0.2/M0.3/M0.4）完成

- [ ] <Proof> 重放 M0.4 §机械复验命令（stub/手写分类 + 冻结口径计数）：断言分类结果 = 39 stub / 108 手写 / stub∩CAT4 违规 = 空集 / CAT4 总数 1700，与冻结矩阵逐项一致（若不一致，逐文件列出分歧并在 Phase 2 例外路径处置，数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 CAT-4 违规文件全集不含任何 GenPage stub（`grep -c web:GenPage` 复核）+ `bash docs/audits/i18n-coverage-checker.sh` 绿（view.xml 层门控未漂移），数字记入勾选注记
      - Skill: none

Exit Criteria:

- [ ] 147 文件分类复验与冻结矩阵一致（39/108/空集/1700），stub 违反面 = 0 断言在案
- [ ] F15 view.xml 层 checker 绿

## Phase 2 — codegen 重生成链洁净证明 + MI.7 零违规裁决

> 统一类型：Proof-heavy（1 Proof + 1 Decision | Fix + 1 Proof；例外路径才触发 Fix）。
> Skill: none
> Targets: 无预置代码改动；例外路径 = `module-*/erp-*-web` 对应 `<Entity>.view.xml` 模型源（仅当 Phase 1/2 实测发现 stub 正文违规）
> Prereqs: Phase 1 完成

- [ ] <Proof> `mvn clean install -DskipTests` 全量 build（触发 gen-page.xgen / gen-i18n.xgen 生成链）BUILD SUCCESS；build 后重放 Phase 1 分类复验 + `node tools/check-hardcoded-cjk.mjs --strict`：断言重生成未引入任何 stub 正文 CJK（39/108/空集/1700 持平，strict 绿），数字记入勾选注记
      - Skill: none
- [ ] <Decision> MI.7 零违规裁决：基于 Phase 1/2 证据记录——codegen 源面在冻结口径下 CAT-4 = 0，「改模型源 view.xml `i18n-en` / xmeta 补属性」的原修复面为空集，无需任何模型源变更；替代方案（前瞻性为 view.xml 冗余补 `i18n-en` 属性——F15 已 0 gap 门控、无可观察结果差异且扩大 diff 面，否决；修改脚本豁免/口径——无违规可豁免，否决）与残余风险（未来 stub 生成链写入 CJK 文案——由 `--strict` CAT-4 门控 + F15 view.xml 层门控双兜底，登记于裁决注记）一并记录；例外路径：若 Phase 1/2 实测发现 stub 正文违规，本项改为记录修复证据（view.xml `i18n-en` 补齐 → 重生成 → strict 绿）
      - Skill: none
- [ ] <Proof> 例外路径兜底断言：`node tools/check-hardcoded-cjk.mjs --strict` exit 0（无论是否触发例外路径，收官态必须 strict 绿），对照 M0.3 基线零新增违规
      - Skill: none

Exit Criteria:

- [ ] 重生成链洁净证明在案：build 前后 stub 分类与 CAT-4 计数持平，strict 绿
- [ ] MI.7 零违规裁决（或例外路径修复证据）落入本计划 Decision 注记

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0902-2-mi7-page-yaml-codegen-source-verify-1-98d2be3b to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-05-123532-mission-driver-2026-09-07-0902-2-mi7-page-yaml-codegen-source-verify-1-98d2be3b（审查中补 Closure Gates 缺失——ledger 格式门控证据注记 + Phase 2 Targets 例外路径触发面校正为 Phase 1/2；基线数字已对实仓复验——M0.4 冻结矩阵 39 stub/0 违规、108 手写/1700 违规、全量 147、stub∩违规空集逐项一致，roadmap MI.7 行 `Skill: none` 与依赖 M0.4+MI.4 在案，引用脚本/文档/上游计划路径全部存在）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处一次：Phase 2 已聚合全量 `mvn clean install -DskipTests`（触发生成链）+ `node tools/check-hardcoded-cjk.mjs --strict` + F15 `bash docs/audits/i18n-coverage-checker.sh`。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：147 文件全量分类复验与冻结矩阵一致（39/108/空集/1700 持平）+ codegen 重生成链洁净证明在案（build 前后 stub 面零违规、strict 绿）
- gate 2 相关文档对齐：MI.7 零违规裁决（或例外路径修复证据）已落本计划 Decision 注记；冻结矩阵与 `docs/audits/cjk-baseline.md` SNAPSHOT 块保持零改动（Non-Goals 义务）
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `bash docs/audits/i18n-coverage-checker.sh` 绿 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS——均绿，见 Verification；若例外路径触发（发生 view.xml 模型源变更），另复跑 `bash docs/audits/nop-compliance-checker.sh` 断言零漂移（漂移则按已知失败模式「Compliance 基线漂移」开独立基线裁决后方可闭包；未触发则无生产变更，此项在闭包注记记 N/A 并注明）
- gate 4 无范围内项目降级为 deferred/follow-up
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）
- gate 6 文本一致性已验证：frontmatter `status`（ledger 完成态派生）与 Phase 勾选注记数字、Verification pass 线、Closure 审计回执及 `docs/logs/` 条目一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——回执落 Closure 节
- gate 8 结束证据存在于文件中（Phase 勾选注记数字 + 验证命令输出 + Closure 审计回执）

## Verification

## Closure
