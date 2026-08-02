# 2026-07-31-1330-1-mg-g1-g4-compliance-guard-and-context-sync MG G.1+G.4 持续 guard 激活确认与基线/上下文最终同步

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MG（G.1 compliance checker 基线更新 + G.4 更新 project-context.md + README.md 已知失败模式）
> Related: `2026-07-31-1705-2-v1-v2-full-build-test-and-compliance-baseline-adjudication.md`（V.2 已落定 BASELINE 块）、`2026-07-31-1330-2-mg-g2-g3-lessons-and-skills-distillation.md`（G.2/G.3 知识沉淀，与本计划独立但互补）
> Audit: required

## Current Baseline

- HEAD = `1ae8337b`（2026-07-31 19:52，MV V.3/V.4/V.5 收口提交）。
- **G.1 状态**：`docs/audits/compliance-baseline.md` §BASELINE 机器可读块已由 V.2（plan `2026-07-31-1705-2`）落定为 R2a=38 / R2b=325 / R2c=1250 / R12c=40 / R5=0（R5 经 Fix 回 0，其余 baseline-raise），V.2 复跑 checker 全 19 规则 actual ≤ baseline 零裸漂移。
- V.2（1705-2）之后至 HEAD（1ae8337b）期间的工作 = V.3/V.4/V.5（plan `2026-07-31-1705-3`）= E2E spec 参数补齐（noCapaReason）+ arm-index 标签回填 + full-green 复验，**零 Java/ORM/生产代码变更**。因此预期当前 HEAD checker 仍零漂移，但 G.1 要求对绝对当前 HEAD 做最终确认复跑。
- **G.4 状态 — 陈旧文案（authoritative 值来自 V.1 = 156 reactor 模块 / 1902 单测）**。经 grep `154|~2890|146 reactor|312\+ 测试` 复核全域顶层文档，当前基线段的陈旧站点（历史注记段除外）：
  - `README.md:60` — `146 reactor 模块`；`:61` — `312+ 测试`（远早于审计基线，严重过时）。
  - `AGENTS.md:157` — `154 reactor 模块全绿基线`。
  - `docs/context/codebase-map.md:11` — `154 个 reactor 模块`；`:13` — `154 = 根 mvn validate...reactor`（含依赖算术 `得 152，比 154 少 2`）。**mandatory-read「当前结构」段，标「权威」口径**——V.1 已落定 156，必须同步。
  - `audit-remediation-roadmap.md:10` — `19 域、154 模块`；`:272` — `JUnit（~2890 测试）`；`:276` — `154 模块` + `~2890 测试`。
  - `docs/requirements/product-scope.md:58` — `154 reactor 模块全绿`；`:77` — `所有 154 模块可独立编译通过`；`:80` — `~2890 单元测试 0 failures`（均为「成功指标（已达成）」当前基线段）。
  - `compliance-baseline.md:208` — `~2890 测试`出现在 M0 锚点注记中解释历史漂移（**历史注记，保留不动**——它记录的是 M0 锚点时刻的实测 1756 与起草粗估 ~2890 的分歧，V.2 注记已落定 1902）。
  - `docs/skills/closure-audit-prompt.md:35` — `154 模块全 reactor` 出现在方法论示例中（**非基线声明，归 Non-Goal**，见 Deferred）。
  - 各 roadmap per-work-item 行（如 `:202` R3.4 行 `154 模块 BUILD SUCCESS`）与 `docs/backlog/README.md` 中的 `154 模块` 为**带日期的逐工作项历史验证记录**（**保留不动**，同 compliance-baseline.md 历史注记类别）。
- **G.4 状态 — 已知失败模式**：`docs/context/project-context.md` 有 §AI 阻塞条件 与 §AI 代理注意事项，但无集中的「已知失败模式」速查段；本次审计-修复任务暴露的反复失败模式（compliance 基线漂移 / closure-pending 计划 / `@Inject private` / 业财过账吞异常悬挂）尚未沉淀为持久上下文引用。

## Goals

- **G.1**：对当前 HEAD 复跑 `nop-compliance-checker.sh`，确认 §BASELINE 机器可读块与实测一致（预期零裸漂移）；若有残余漂移则裁决并落地（Fix 或经 per-site 证据 baseline-raise）。正式闭包 G.1。
- **G.4**：将权威实测值（156 模块 / 1902 测试）同步到所有陈旧文案站点；在 `project-context.md` 与 `README.md` 增加简洁的「已知失败模式」速查段（自包含内联摘要，指向 `docs/lessons/` 与 `docs/skills/` 求详情）。

## Non-Goals

- 重新裁决 compliance 基线值（V.2 已裁决，本计划仅在出现新漂移时才裁决）。
- 提取失败模式为 `docs/lessons/` 详细 lesson 或 `docs/skills/` 新提示（归 G.2/G.3，plan `-1330-2`）。
- 重跑全量 `mvn test`（V.1 已建立绿基线，HEAD 无生产代码变更；G.1 仅跑 checker 不跑 mvn）。
- 修改 compliance-baseline.md 历史注记段中的「154 模块 / ~2890」表述（这些是各裁决时刻的历史记录，改写会伪造历史）。

## Task Route

- Type: `verification or audit work`（G.1 checker 复跑）+ `implementation-only change`（G.4 文档同步）
- Owner Docs: `docs/audits/compliance-baseline.md`、`docs/context/project-context.md`、`README.md`、`docs/backlog/audit-remediation-roadmap.md`、`AGENTS.md`
- Skill Selection Basis: `none` — G.1 是既有 checker 的验证复跑，G.4 是机械文档同步；两者均不涉及审计方法选择或 Nop 平台开发模式。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（checker 脚本与文档均已就位）。

## Execution Plan

### Phase 1 - G.1 compliance checker 最终确认复跑

Status: completed
Targets: `docs/audits/compliance-baseline.md`（仅在出现漂移时追加注记 + 更新 BASELINE 块）
Skill: `none`

- Item Types: `Proof | Fix | Decision`
- Prereqs: MV done（已满足）

- [x] `Proof` 复跑 `bash docs/audits/nop-compliance-checker.sh` 对照 §BASELINE 机器可读块，记录每规则 actual vs baseline。
      - Skill: `none`
- [x] `Decision` 裁决漂移（若有）：逐项 `Fix`（驱动回降）或 `baseline-raise`（per-site git diff `0e963531d..HEAD` 证据 + 合法性分类 + 显式更新 BASELINE 块）。
      - Skill: `none`
- [x] 若出现 baseline-raise：在 compliance-baseline.md 追加注记段（对齐 V.2/1057-1/0823-1 先例）+ 更新 §BASELINE 机器可读块；若 Fix：落地代码修复 + 域单模块 `mvn test` 验证。
      - Skill: `none`

Exit Criteria:

> 仅证明 checker 与当前 HEAD 一致；完整 mvn build 属 Closure Gates（本计划无生产代码变更时 G.1 不触发 mvn）。

- [x] checker 全 19 规则 actual ≤ baseline（零裸漂移），或出现的漂移已逐项裁决并落地（Fix 或 baseline-raise 带 per-site 证据）

**Phase 1 Evidence**（2026-07-31 复跑 HEAD=1ae8337b）：全 19 规则 actual ≤ baseline 零裸漂移，无 Fix 无 baseline-raise。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 17 | 17 | ✅ |
| R2a/R2b/R2c/R2d | 38/325/1250/28 | 38/325/1250/28 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 42 | 42 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

G.1 正式闭包：V.2 落定的 BASELINE 块（R2a=38/R2b=325/R2c=1250/R12c=40，R5 经 Fix 回 0）在 V.3/V.4/V.5 零生产代码变更后保持稳定，checker 与当前 HEAD 一致。

### Phase 2 - G.4 基线文案同步 + 已知失败模式上下文段

Status: completed
Targets: `README.md`、`AGENTS.md`、`docs/context/codebase-map.md`、`docs/backlog/audit-remediation-roadmap.md`、`docs/requirements/product-scope.md`、`docs/context/project-context.md`
Skill: `none`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成（文案使用 V.1 权威值，与 checker 状态一致）

- [x] `Fix` 同步权威实测值到陈旧文案站点（grep `154 模块|154 reactor|~2890|146 reactor|312+ 测试` 命中的当前基线段）：
      - `README.md:60` — `146 reactor 模块` → `156 reactor 模块`
      - `README.md:61` — `312+ 测试` → `1902 测试`
      - `AGENTS.md:157` — `154 reactor 模块` → `156 reactor 模块`
      - `audit-remediation-roadmap.md:10` — `154 模块` → `156 模块`
      - `audit-remediation-roadmap.md:272` — `~2890 测试` → `1902 测试`
      - `audit-remediation-roadmap.md:276` — `154 模块` → `156 模块`；`~2890 测试` → `1902 测试`
      - `docs/requirements/product-scope.md:58` — `154 reactor 模块全绿` → `156 reactor 模块全绿`
      - `docs/requirements/product-scope.md:77` — `所有 154 模块可独立编译通过` → `所有 156 模块可独立编译通过`
      - `docs/requirements/product-scope.md:80` — `~2890 单元测试` → `1902 单元测试`
      - `docs/context/codebase-map.md:11` — `154 个 reactor 模块` → `156 个 reactor 模块`
      - `docs/context/codebase-map.md:13` — `154 = 根...reactor` → `156 = 根...reactor`，依赖算术同步：原文 `得 152，比 154 少 2`（total = find + 2 关系），total 154→156 则 find 152→154、`比 154 少 2`→`比 156 少 2`；执行时复跑 `find module-* -name pom.xml -maxdepth 3 | wc -l` 确认 find 实测值后填入（保持 total = find + 2 不变量）
      - Skill: `none`
- [x] `Add` 在 `docs/context/project-context.md` 增「已知失败模式」段：简洁内联摘要列出本次审计-修复任务反复出现的模式（compliance 基线漂移 / closure-pending 计划缺独立 closure audit / `@Inject private` 违反 Nop IoC / 业财过账 `tryPost` 吞异常致 posted 悬挂），每条一句话 + 指向 `docs/lessons/` 求详情（G.2 持续补充详细 lesson）。
      - Skill: `none`
- [x] `Add` 在 `README.md` 验证状态段补一句指向 `docs/audits/compliance-baseline.md`（guard 基线）与 `docs/context/project-context.md`（已知失败模式）的引用，避免 README 成为第二个漂移真相源。
      - Skill: `none`

Exit Criteria:

- [x] grep 复核：`README.md` / `AGENTS.md` / `codebase-map.md` / `audit-remediation-roadmap.md` / `product-scope.md` 中无残留 `154`/`~2890`/`146 reactor`/`312+ 测试` 当前基线表述（grep 用宽松模式 `154|~2890|146 reactor|312\+ 测试` 以捕获 `154 个 reactor`/`154 = ` 等变体；逐条分类排除 compliance-baseline.md 历史注记、closure-audit-prompt.md 示例、roadmap/README per-work-item 带日期历史记录）
- [x] project-context.md 含「已知失败模式」段且每条为自包含内联摘要（非悬空文件引用）

**Phase 2 Evidence**（2026-07-31）：

1. **文案同步**：11 处当前基线站点全部 Fix（README:60,61 / AGENTS:157 / codebase-map:11,13 / roadmap:10,272,276 / product-scope:58,77,80）。codebase-map 依赖算术实测复核：`find module-* -name pom.xml -maxdepth 3` = 154，total = 154+2 = 156，与 V.1 权威值一致（find 152→154、total 154→156，不变量 total=find+2 保持）。
2. **已知失败模式段**：`docs/context/project-context.md` 末尾新增「已知失败模式（速查）」段，4 条自包含内联摘要（compliance 基线漂移 / closure-pending 缺独立结束审计 / `@Inject private` 违反 Nop IoC / 业财过账吞异常致 posted 悬挂），每条一句规避指引 + 指向 `docs/audits/compliance-baseline.md`/`docs/skills/`/`docs/lessons/` 求详情。
3. **README 引用**：`README.md §验证状态` 补引导句，指向 `compliance-baseline.md`（guard 基线）与 `project-context.md`（已知失败模式）。
4. **grep 复核**：宽松模式 `154|~2890|146 reactor|312\+ 测试` 命中 5 文件，残余命中全部归类为排除集——codebase-map:13 的 `154` 为修正后的 find 计数（intentional）+ 历史快照注记；roadmap:3 changelog v-entries 与 roadmap:202 R3.4 per-work-item 行为带日期历史记录（Deferred 显式排除）；README/AGENTS/product-scope 零命中。无残余当前基线漂移。
5. **构建验证**：纯 .md 变更（无生产代码）。按 plan Closure Gates，G.4 为纯 .md 不触发 mvn；为确认全仓无下游影响，复跑 `mvn clean install -DskipTests` → BUILD SUCCESS（reactor `[1/156]` 确认 156 权威值，01:40 min）。

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（task `ses_047f4da8e`，fresh session）— 发现 1 项 blocking：`docs/requirements/product-scope.md:58,77,80` 当前基线段含陈旧 `154 模块`/`~2890 单元测试`，未被 Phase 2 覆盖且 Exit Criteria grep 漏扫。已修订：补入 Phase 2 Fix 清单 + 扩展 grep 范围。Non-blocking（已采纳）：Phase 1 item types 补 `Decision`；closure-audit-prompt.md:35 示例登记 Deferred watch-only。
- Independent draft review iteration 2: `needs-revision`（task `ses_047f08a1f`，fresh session）— 前述 blocking 已 RESOLVED；但独立 grep 发现新 blocking：`docs/context/codebase-map.md:11,13`（mandatory-read「当前结构」段，标「权威」口径）含 `154 个 reactor 模块`/`154 = ...`，原 grep 模式 `154 模块|154 reactor` 漏匹配 `154 个`/`154 = ` 变体。已修订：补入 Current Baseline + Phase 2 Targets/Fix + 宽松 grep 模式 `154|~2890|146 reactor|312\+ 测试`；codebase-map 依赖算术（total=find+2 不变量）执行时复跑 find 确认。Non-blocking（已采纳）：roadmap per-work-item 带日期历史记录登记 Deferred。
- Independent draft review iteration 3: `acceptable-as-is`（task `ses_047ecbf51`，fresh session）— 独立穷举 grep 复核：11 处当前基线陈旧站点（README:60,61 / AGENTS:157 / codebase-map:11,13 / product-scope:58,77,80 / roadmap:10,272,276）全部在 Phase 2 Fix 清单内；历史记录（roadmap:3 changelog / roadmap:202 per-work-item / compliance-baseline 注记 / closure-audit-prompt:35 示例 / backlog README per-work-item）全部正确排除。codebase-map 算术 live 复核：`find module-* -name pom.xml -maxdepth 3` = 154，total = 154+2 = 156，与 V.1 权威值一致。无 slack 词、item types 一致、Closure Gates 合理。**收敛达成，转 active。**

## Closure Gates

> 本计划以文档/checker 变更为主。G.1 仅跑 checker（非 mvn）。G.4 为纯 .md 变更。若 G.1 出现需代码 Fix 的漂移，则 Closure Gates 加跑受影响域 `mvn test` + 全量 `mvn clean install -DskipTests`。

- [x] 范围内行为完成（G.1 checker 零漂移或裁决落地 + G.4 文案同步 + 已知失败模式段）
- [x] 相关文档对齐（project-context / README / roadmap / AGENTS / compliance-baseline 一致）
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（+ 若 G.1 Fix 触发则域 `mvn test`）；compliance checker 零新增命中
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### compliance-baseline.md 历史注记中的 154/~2890 表述

- Classification: `watch-only residual`
- Why Not Blocking Closure: 这些是各裁决时刻（0930-1 / 1057-1 / 0823-1 等）的历史记录，改写会伪造历史；权威当前值已在 V.2 注记与 BASELINE 块落定。
- Successor Required: `no`

### closure-audit-prompt.md 方法论示例中的 154 模块

- Classification: `watch-only residual`
- Why Not Blocking Closure: `docs/skills/closure-audit-prompt.md:35` 的 `154 模块全 reactor` 是验证范围示例（非基线声明），不误导当前状态；skills 文件归 G.3/plan `-1330-2` 所有权。
- Successor Required: `yes`（触发条件 = G.3 执行时若触及该 skill，顺手更新为 `156` 或泛化为 `N reactor 模块`）

### roadmap / backlog per-work-item 带日期历史记录中的 154

- Classification: `watch-only residual`
- Why Not Blocking Closure: `audit-remediation-roadmap.md:202`（R3.4 行 `154 模块 BUILD SUCCESS`）及 `docs/backlog/README.md` 中多处 `154 模块` 为**带日期的逐工作项验证记录**（记录该工作项完成时刻的实测值），改写会伪造历史；与 compliance-baseline.md 历史注记同类。
- Successor Required: `no`

### 详细 lesson / skill 提取

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: G.4 的「已知失败模式」段为简洁内联摘要；详细 case study 与可复用审计方法归 G.2/G.3（plan `-1330-2`）。
- Successor Required: `yes`（触发条件 = G.2/G.3 执行时，将逐项扩展为 lesson 07+ 与新 skill）

## Closure

Status Note: 执行者已完成 G.1（checker 零漂移确认）+ G.4（文案同步 + 已知失败模式段）。独立结束审计已由独立子代理（新会话，task `ses_047e11958ffehnSZNbTQYvLsnq`，cold context）执行并 **PASS**。两 Phase `Status: completed`、所有 Phase 项与 Exit Criteria 已勾选、Plan Status `completed`、roadmap MG 表 G.1+G.4 `done`。本计划无 `> Source Audits:` 行（roadmap 源生计划），关闭 source audits 步骤跳过。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task `ses_047e11958ffehnSZNbTQYvLsnq`，不复用执行者上下文）
- Evidence:
  - **Phase 1**：独立复跑 `bash docs/audits/nop-compliance-checker.sh` exit 0，全 19 规则 actual == baseline 零漂移（R2a=38/R2b=325/R2c=1250/R12c=40/R5=0 等），BASELINE 块与权威值一致。G.1 closed。
  - **Phase 2**：grep `154|~2890|146 reactor|312\+ 测试` 复核——README/AGENTS/product-scope 零残留当前基线值；codebase-map:13 残留 154 = 修正后 find 计数（intentional，total=find+2 不变量成立）；roadmap:3,202 残留 = changelog v-entry + R3.4 per-work-item 带日期历史记录（Exit Criteria 排除）。已知失败模式段（4 条自包含内联摘要）+ README 引用句均存在。`find module-* -name pom.xml -maxdepth 3` = 154 → total 156。
  - **一致性**：Plan Status `completed`；两 Phase `Status: completed`；8 项 Phase item + 3 Exit Criteria + 8 Closure Gates 全 `[x]`；roadmap MG 表 G.1+G.4 `done`、G.2/G.3 `todo`（sibling plan）。
  - **无 scope creep**：`git diff --name-only HEAD` = 6 `.md` 文件（零非 .md）；`nop-compliance-checker.sh` 未修改；无 Java/ORM/生产代码触及。
- Verdict: **PASS** — 计划可标记闭包。

Follow-up:

- G.2/G.3（plan `-1330-2`）扩展本计划「已知失败模式」段所列模式为详细 lessons + skills。
