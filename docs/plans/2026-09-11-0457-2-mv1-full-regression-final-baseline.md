---
status: active
mission: ai-check-r3
work-item: MV.1
group: "2026-09-11-0457"
verify: [test]
---

# 2026-09-11-0457-2 MV.1 全量回归与终态基线登记（五命令门 + known-good-baselines 终态行）

## Current Baseline

- Roadmap MV.1 行 `todo`（`docs/backlog/ai-check-r3-roadmap.md`，deps MI.9 + M2.9，Skill: `closure-audit-prompt` 独立子代理）。MI.9 已收官：plan `2026-09-07-1715-3` 独立子代理 closure audit 通过（2026-09-08），known-good-baselines 2026-09-08 MI 终态行在案。M2.9 由同批 N=1 计划 `2026-09-11-0457-1` 承载（双索引回填 + R2c 1542→1543 基线上调），本计划执行顺序居其后。
- **MV.1 五命令门**（roadmap MV.1 行）：① `mvn clean install -DskipTests` BUILD SUCCESS；② 全 reactor `mvn test` 零新增失败（对照 M0.3 基线行登记的预存失败清单）；③ `bash docs/audits/nop-compliance-checker.sh` 不高于 M0.3 快照（合法新增走 baseline-raise——M2.9 上调后 R2c=1543）；④ `node tools/check-hardcoded-cjk.mjs --strict` 全绿；⑤ `bash docs/audits/i18n-coverage-checker.sh` 全绿。
- **对照面**：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1，预存失败清单=无——09-04 行 hr/drp 两项预存失败已由修复批吸收解决）+ 2026-09-08 MI 终态行（CAT1..4=0/0/0/0，白名单 27 文件四要素齐备）+ 2026-09-09 compliance 基线裁决行（R2b=242/R2c=1542/R12a=71）+ N=1 计划上调后 R2c=1543。
- **当前实仓状态**（2026-09-11 复核）：checker 全表 R2b=242 / R2c=1543 / R12a=71、其余 16 规则=基线（R2c +1 漂移待 N=1 合法上调闭环）；CJK report mode CAT1..4=0/0/0/0；`--strict` PASS exit 0。
- **测试计数口径**：M2.x 各批收尾全 reactor 数字（4023/4012/4011 等）为各批 T0 局部 HEAD 口径且含并行姊妹批归因披露，不可直接互比；本计划终态行以**最终 HEAD 单次全量实跑**为准，新增测试计数差值沿 M2.x 全归因披露纪律对账（对照面预存失败清单=无，故零新增失败判据 = failures/errors 均 0）。
- 本计划为只读回归 + 登记计划：唯一写入面 = `docs/testing/known-good-baselines.md` 终态行 + 本计划 + 当日日志；**零生产代码/页面/seed/ORM/api.xml 改动**。

## Goals

- 五命令全量回归全绿（合法基线内，零新增失败、零新增漂移）。
- `docs/testing/known-good-baselines.md` 登记 `ai-check-r3-mv1` 终态行（验证命令组 + 全 reactor 数字 + checker 终态计数 + CJK 终态 + 日期与本计划指针）。

## Non-Goals

- 不翻转 roadmap 状态块（MV.1 状态由 owner/engine 依独立结束审计处置）。
- 不做 MV.2（索引终态校验）/ MV.3（方法学沉淀）/ MG.1 / MG.2 工作。
- 不改任何生产代码、页面、seed、ORM、api.xml、checker 脚本本体。
- 不开新 baseline-raise：若回归暴露 M2.9 上调面之外的新增漂移，按已知失败模式「Compliance 基线漂移」移交独立裁决计划并登记为收官阻塞，不在本计划内放宽基线。
- 不处置 validate:flux 325 条 `variant=primary` 既有漂移（跨仓库保护区 successor，trigger「validate:flux exit 0 恢复」未满足，且非 roadmap MV.1 命令面）。

## Phase 1 — 全量回归五命令

> 统一类型：Proof（1 项 Proof）。
> Skill: none
> Targets: 只读验证；无文件改动
> Prereqs: N=1 计划（`2026-09-11-0457-1`，M2.9）执行完成

- [ ] <Proof> 五命令组全量实跑并记录数字：① `mvn clean install -DskipTests` BUILD SUCCESS（156 reactor 模块）；② 全 reactor `mvn test` 零新增失败（failures=0 / errors=0；对照 M0.3 行预存失败清单=无；skipped 口径注记）；③ `bash docs/audits/nop-compliance-checker.sh` exit 0 且全 19 规则 actual ≤ 机器块（R2b=242 / R2c=1543 / R12a=71，其余=基线）；④ `node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0（CAT1..4 不高于 0/0/0/0）；⑤ `bash docs/audits/i18n-coverage-checker.sh` 全绿——若任一门出现新增失败/漂移，按已知失败模式移交独立裁决计划并登记为收官阻塞，不在本计划内放宽
      - Skill: none

Exit Criteria:

- [ ] 五命令全绿实跑在案，数字记入勾选注记并落 `## Verification` pass 线

## Phase 2 — known-good-baselines 终态行登记

> 统一类型：Add（1 项 Add）。
> Skill: none
> Targets: `docs/testing/known-good-baselines.md`
> Prereqs: Phase 1 完成

- [ ] <Add> 登记 `ai-check-r3-mv1` 终态行：五命令组实跑数字 + 全 reactor Results 聚合 + checker 终态计数（19 规则全表）+ CJK 终态（CAT1..4 + `--strict`）+ 日期 + Git State + 本计划指针；与 M0.3 行（零新增失败对照面）、MI 终态行（CAT 终态）、2026-09-09 基线裁决行 + N=1 上调（R2c=1543）交叉一致
      - Skill: none

Exit Criteria:

- [ ] 终态行落盘且与 Phase 1 实跑数字逐项一致

## Phase 3 — 独立收官审计

> 统一类型：Proof（1 项 Proof）。
> Skill: closure-audit-prompt（roadmap MV.1 行指定）
> Targets: 只读审计；无文件改动
> Prereqs: Phase 2 完成

- [ ] <Proof> 独立子代理 closure audit（新会话，不重用执行者上下文；执行者不自我审计）：按 `docs/skills/closure-audit-prompt.md` 复核本计划全部门控证据（五命令实跑数字、终态行交叉一致、零新增失败/零新增漂移判据）；审计回执落 `## Closure`；未通过则本计划保持打开并按审计意见修复后重审
      - Skill: closure-audit-prompt

Exit Criteria:

- [ ] 独立子代理收官审计通过，回执在案于 `## Closure`

## Draft Review Record

- dispatch review #review-2026-09-09-210030-2026-09-11-0457-2-mv1-full-regression-final-baseline-1-39ba4545 to w0t0p0:7E46A50C-5511-423A-A2C6-12F8CF3D55DD
- 2026-09-11：iteration 1，共识 approved #review-2026-09-09-210030-2026-09-11-0457-2-mv1-full-regression-final-baseline-1-39ba4545

## Verification

## Closure
