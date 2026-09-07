---
status: active
mission: ai-check-r3
work-item: MI.9
group: "2026-09-07-1715"
verify: [test]
---

# 2026-09-07-1715-3 MI.9 MI 收官（--strict 全绿 + 门控零回归 + 终态登记 + 独立收官审计）

## Current Baseline

- MI 里程碑现状（`docs/backlog/ai-check-r3-roadmap.md` MI 表）：MI.1（Errors `@Locale("zh-CN")` 22 文件）、MI.2（LOG 批 1 fin+ast 93 行）、MI.3（LOG 批 2 七域 165 行）、MI.4（LOG 批 3 九域 77 行 → CAT-1 全域归零 335→0）、MI.5a（异常参数 1/2 五模块 136 行）、MI.5b（异常参数 2/2 十二域 68 行 → CAT-2 全域归零 204→0）、MI.7（页面 yaml codegen 源零违规裁决）全部 done（plans `2026-09-06-2104-1/2/3`、`2026-09-07-0043-1/2/3`、`2026-09-07-0902-2`）；MI.6 批 1 done（fin/ast/cs 181 行，plan `2026-09-07-0902-1`）；MI.8 批 1 done（fin/prj 382 行，plan `2026-09-07-0902-3`）。
- 剩余缺口（2026-09-07 checker 实跑）：CAT-3 = 209 行 / 88 文件 / 16 域（MI.6 批 2 = 本批计划组 N=1 计划 `1715-1` 范围）；CAT-4 = 1318 行 / 82 文件 / 17 域（MI.8 批 2 = 本批计划组 N=2 计划 `1715-2` 范围）；CAT-1/2 = 0。MI.9 依赖 MI.1~MI.8（含 5a/5b）全部完成——经同批 N=1/N=2 计划按序执行闭合，本计划执行顺序居本批三计划之第 3。
- 收官判据（roadmap MI.9 行）：`tools/check-hardcoded-cjk.mjs --strict` 全绿（CAT-1/2/3/4 = 0 或白名单显式登记）；compliance checker + i18n-coverage-checker 零回归；`docs/testing/known-good-baselines.md` 登记 MI 终态行；独立子代理 closure audit。
- 白名单现状：`docs/audits/cjk-baseline.md` §WHITELIST 已登记 11 文件（MI.6 批 1：C1 `@Description` 族 + C2 功能型契约窄类，四要素齐备）；批 1 起白名单机制语义 = 文件级 per-CAT 豁免。终态断言口径 = checker report mode 全域 per-domain CAT1..4 计数全 0（白名单文件已出计数）。
- 对账链基准：CAT-1 335→0（MI.2 93 + MI.3 165 + MI.4 77）；CAT-2 204→0（MI.5a 136 + MI.5b 68）；CAT-3 390→0（批 1 181 + 批 2 209）；CAT-4 1700→0（批 1 382 + 批 2 1318）；各批批注账行已记 `cjk-baseline.md` §批注账。
- 既有门控基线：compliance checker M0.3 快照（R2b=242 / R2c=1542 / R12a=71；MI.4/MI.5a/MI.5b 已登记的批外预存漂移归 successor 独立基线裁决，非本计划范围）；i18n-coverage-checker（F15，view.xml + action-auth 层）全绿；`npm run validate:flux` step [1/3] 导出 0 error（整体 exit 1 余项 = 325 条 `variant=primary` 既有外部漂移，successor 在案）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1；预存失败清单为 MV.1 零新增失败对照面）；MI 历批收官零新增失败。
- Skill: `closure-audit-prompt`（roadmap MI.9 行指定，独立子代理新会话执行）。

## Goals

- MI 终态机械断言：checker report mode 全域 CAT-1/2/3/4 = 0（白名单豁免文件已出计数）+ `--strict` PASS + `--self-test` PASS + 白名单登记四要素完整性核对。
- 门控零回归：i18n-coverage-checker 全绿、compliance checker 零漂移、全量 build + 全 reactor test 零新增失败（对照 M0.3 基线行预存失败清单）。
- 终态登记：known-good-baselines 登记 MI 终态行；批注账对账链（四条 335→0 / 204→0 / 390→0 / 1700→0）完整性核对；独立子代理 closure audit（`closure-audit-prompt`，新会话，执行者不自我审计）。

## Non-Goals

- 不改任何生产代码、页面文件、seed、ORM、checker 脚本本体（本计划为只读收官 + 登记；checker 漂移处置走独立基线裁决计划，不属本计划）。
- 不处置 successor 在案的批外事项：compliance checker R2b/R2c/R12a 批外预存漂移基线裁决、`variant=primary` 325 条跨仓库保护区漂移（nop-chaos-flux dist 基线裁决）、`period-close-wizard/main.page.yaml` `\'` 转义既有缺陷与 dashboard/kanban 重复 `then:` 键卫生候选（`0902-3` Closure minor 遗留）。
- 不翻转 roadmap 状态块（MI.6/MI.8/MI.9 状态由 owner/engine 依收官审计结论处置，AI 不自行重排或发明工作项）；不回写 finding 索引（M1.x 审计阶段产物）。

## Phase 1 — MI 终态机械断言

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 只读断言；无文件改动
> Prereqs: 本批计划 `2026-09-07-1715-1`（MI.6 批 2）与 `2026-09-07-1715-2`（MI.8 批 2）执行完成

- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs`（report mode）全域 per-domain CAT1..4 计数全 0 + `node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0 + `node tools/check-hardcoded-cjk.mjs --self-test` PASS（数字记入勾选注记，与 `cjk-baseline.md` 批注账各批对账链逐项核对一致）
      - Skill: none
- [ ] <Proof> `docs/audits/cjk-baseline.md` §WHITELIST 全条目四要素完整性核对（文件路径在盘 / 理由 / owner doc 指针 / 裁决来源），逐条核对结果记入勾选注记；缺任一要素的条目登记为收官阻塞项并回溯对应批次计划处置
      - Skill: none

Exit Criteria:

- [ ] report mode 全域 CAT1..4 = 0 且 `--strict` / `--self-test` PASS，批注账对账链一致
- [ ] 白名单登记四要素完整性核对完成，零缺要素条目

## Phase 2 — 门控零回归与全量验证

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 只读验证；无文件改动
> Prereqs: Phase 1 完成

- [ ] <Proof> `bash docs/audits/i18n-coverage-checker.sh` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 零漂移（不高于 M0.3 快照；若漂移，按已知失败模式「Compliance 基线漂移」移交独立基线裁决计划并登记为收官阻塞项，不在本计划内放宽基线）
      - Skill: none
- [ ] <Proof> `mvn clean install -DskipTests` BUILD SUCCESS + 全 reactor `mvn test` 零新增失败（surefire 聚合对照 M0.3 基线行预存失败清单逐项核销；数字记入勾选注记）
      - Skill: none

Exit Criteria:

- [ ] 双 checker 门控绿或漂移已显式移交独立裁决并登记阻塞
- [ ] 全量 build + 全 reactor test 零新增失败，与 M0.3 对照面一致

## Phase 3 — 终态登记 + 独立收官审计

> 统一类型：Proof（2 项 Proof）。
> Skill: closure-audit-prompt（Phase 3 第 2 项，roadmap MI.9 行指定）
> Targets: `docs/testing/known-good-baselines.md`（MI 终态行登记）
> Prereqs: Phase 2 完成

- [ ] <Proof> `docs/testing/known-good-baselines.md` 登记 `ai-check-r3-mi` 终态行（验证命令组 + 全 reactor 数字 + checker 终态计数 + 日期与本计划指针）；MI.6/MI.8 批注账收官行与本计划终态数字交叉核对一致
      - Skill: none
- [ ] <Proof> 独立子代理 closure audit（新会话，不重用执行者上下文；执行者不自我审计）：按 `docs/skills/closure-audit-prompt.md` 复核本计划全部门控证据（Phase 1~3 勾选注记、验证命令输出、批注账对账链、终态登记行）；审计回执落 `## Closure`；未通过则本计划保持打开并按审计意见修复后重审
      - Skill: closure-audit-prompt

Exit Criteria:

- [ ] known-good-baselines MI 终态行已登记且与批注账交叉一致
- [ ] 独立子代理收官审计通过，回执在案于 `## Closure`

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-07-1715-3-mi9-mi-closure-strict-green-1-c47d92ae to 2026-09-07-171530-mission-driver
- 2026-09-07：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-07-1715-3-mi9-mi-closure-strict-green-1-c47d92ae（补 Closure Gates 缺失——指南模板 8 门控按本计划只读收官性质定制；frontmatter `status: draft` → `active`；基线引用文件与 checker `--strict`/`--self-test`/report mode 能力已对实仓复验存在）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读收官 + 登记（零生产代码改动），验证命令组即结果表面本身（Phase 1~2 所列），完整仓库验证在 Phase 2 一次完成，此处为门控核对而非重复执行。

- [ ] 范围内行为完成（Phase 1~3 全部执行项与退出标准 `[x]`，勾选注记含数字红线）
- [ ] 相关文档对齐（`known-good-baselines.md` MI 终态行与 `cjk-baseline.md` 批注账对账链交叉一致；roadmap 状态翻转与 finding 索引回写属 Non-Goals，不在此门控）
- [ ] 已运行验证：`node tools/check-hardcoded-cjk.mjs`（report mode 全域 CAT1..4 = 0）+ `--strict` PASS exit 0 + `--self-test` PASS + `bash docs/audits/i18n-coverage-checker.sh` 全绿 + `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式「Compliance 基线漂移」移交独立基线裁决后方可闭包）+ `mvn clean install -DskipTests` BUILD SUCCESS + 全 reactor `mvn test` 零新增失败（对照 M0.3 基线行预存失败清单）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（见 Draft Review Record）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（Phase 勾选注记 + `## Closure` 审计回执）

## Verification

## Closure
