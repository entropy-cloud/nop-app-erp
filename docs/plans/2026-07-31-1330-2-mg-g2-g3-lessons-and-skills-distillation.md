# 2026-07-31-1330-2-mg-g2-g3-lessons-and-skills-distillation MG G.2+G.3 审计-修复任务知识沉淀：lessons 失败模式 + skills 审计方法复用化

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MG（G.2 新失败模式提升为 docs/lessons/ + G.3 重复审计维度提升为 docs/skills/ 新提示）
> Related: `2026-07-31-1330-1-mg-g1-g4-compliance-guard-and-context-sync.md`（G.4「已知失败模式」段列出本计划扩展的模式清单）、`docs/audits/arm-index.md`（finding 真相源）、`docs/lessons/README.md`、`docs/skills/README.md`
> Audit: required

## Current Baseline

- `docs/lessons/` 现有 6 个编号 lesson（`01`–`06`）+ `README.md`。格式约定：编号文件，每条为持久工程经验（非日志），含具体 case 与自检清单。最近一条 `06-codegen-product-edit-overwrite.md`（2026-07-21）。
- `docs/skills/` 现有 22 个可复用审计/审查提示文件 + `README.md`（技能注册表：使用场景/不使用场景/必需输入/预期输出 列）。README §技能路由规则 明令「仅当可复用工作方法稳定时才提升技能」「不要将技能用作需求/设计/架构真相的替代品」。
- 本次审计-修复任务（MA1–MA7 / MR1–MR5 / MV）在 `docs/audits/arm-index.md` 沉积 **6 P0 + 191 P1 + 3 跨维度发现**，以及 ~50 份计划 closure 记录。其中反复出现、尚未提升为 lesson/skill 的模式（候选清单见下方 Decision 项）：
  1. **Compliance 基线漂移**（3 轮裁决：1057-1 / 0823-1 / V.2；另有 1057-2 触及 R8 基线为副作用非独立裁决）— 加深计划新增 daoFor/import 后 checker 漂移，closure 未核对基线。
  2. **Closure-pending 计划**（R3.5 发现 14 份「completed」缺独立 closure audit）— 计划标记完成却无独立子代理 closure 证据。
  3. **`@Inject private`**（R5，R3.4 `ErpRoleDataAuthChecker` 复发）— 违反 Nop IoC 硬规则「@Inject 字段不能 private」。
  4. **业财过账 `tryPost` 吞异常悬挂**（R1.16，跨域 12 findings）— 异常被吞致 `posted=false` 悬挂无告警闭环。
  5. **arm-index 陈旧 `todo (R*.x)` 标签**（V.5 发现 102 条已 done 未回填）— 修复完成未回填索引状态。
  6. **dict 死状态**（MR1 跨多域）— 状态机 dict 声明不可达/无迁移的状态值。
- 稳定可复用审计方法候选（for skills）：
  - **Compliance 基线漂移裁决法**（per-site git diff 分类 + baseline-raise vs Fix + BASELINE 块更新，已稳定经 4+ 轮验证）。
  - **Closure-pending 计划检测与批量 closure-audit**（R3.5 形式化：候选清单 → 独立子代理 fresh session 逐份 closure → 回填证据）。

## Goals

- **G.2**：将本次任务暴露的**新**反复失败模式提升为 `docs/lessons/` 编号文件（续 `07`+），每条含具体 case（引用 arm-index/计划证据）+ 自检清单，遵循 `01`–`06` 既有约定。
- **G.3**：将**稳定且可复用**的审计/处置方法提升为 `docs/skills/` 新提示文件，并在 `docs/skills/README.md` 注册表登记（使用场景/不使用场景/必需输入/预期输出）。

## Non-Goals

- 重新裁决 mission finding（`arm-index.md` 是记录，本计划只提炼经验）。
- 提升已被现有 lesson `01`–`06` 或现有 skill 覆盖的模式（去重）。
- 为一次性、未复发的模式创建 skill（仅稳定可复用方法提升）。
- 修改 `arm-index.md` finding 状态（归 MR/V，已闭包）。

## Task Route

- Type: `implementation-only change`（知识产物创建）
- Owner Docs: `docs/lessons/README.md`、`docs/skills/README.md`、`docs/audits/arm-index.md`（证据源）
- Skill Selection Basis: `none` — 本计划本身是知识沉淀（meta）；不调用审计方法 skill。最终自检可用 `development-wisdom-gate-prompt.md` 验证产出质量，但非执行必需。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - G.2 失败模式提炼为 lessons

Status: completed
Targets: `docs/lessons/07-*.md` ... `docs/lessons/NN-*.md`、`docs/lessons/README.md`
Skill: `none`

- Item Types: `Decision | Add | Proof`
- Prereqs: MV done（已满足）

- [x] `Decision` 从候选清单（6 模式）去重裁决：排除已被 `01`–`06` 覆盖的（如 `06` 已覆盖 codegen 产物编辑覆盖）；确定本轮提升的 lesson 清单与编号（续 `07`+）。记录排除理由。
      - Skill: `none`
      - 裁决结果：5 模式入选（`07` compliance 基线漂移 / `08` closure-pending 缺独立 closure audit / `09` 业财过账吞异常悬挂 / `10` dict 死状态 / `11` 索引状态未回填）；1 模式排除——`@Inject private` 已被 `docs/skills/README.md §已知失败模式 #6` 收录为速查项（单一规则无 case 复杂度，不另建 lesson，归 Deferred watch-only）。无候选被 `01`–`06` 直接覆盖（01 表前缀 / 02 重编号残留 / 03 内部状态文本 / 04 BizModel 契约 / 05 e2e 诊断 / 06 codegen 覆盖——主题均不同）。
- [x] `Add` 为每个入选模式创建 lesson 文件：标题 + 失败模式一句话 + 具体 case（引用 arm-index finding ID / 计划 / file:line 证据）+ 「如何避免」自检清单 + 「何时复发」触发条件。以 `06-codegen-product-edit-overwrite.md`（含完整 自检清单/决策树/反模式 段）为结构参考范本；不照搬单一模板（`04` 与 `06` 结构有差异，按内容形态选最贴合的结构）。
      - Skill: `none`
      - 产出：`07`（含决策树+自检清单+复发条件）/ `08`（含与 lesson 03 区分+反模式表）/ `09`（含决策树+跨域 case 表）/ `10`（含跨域 finding 裁决表+决策树）/ `11`（含三投影面闭合模型）。均引用 arm-index finding ID + plan 证据 + file:line。
- [x] `Proof` 更新 `docs/lessons/README.md` §Lessons 列表登记新 lesson（一句话摘要）。
      - Skill: `none`
      - 产出：README §Lessons 增 `07`–`11` 五行摘要 + 末尾提升裁决注记（含 `@Inject private` 排除理由）。

Exit Criteria:

- [x] 每个新 lesson 文件含：可识别失败模式 + 至少一个 mission 内具体 case（带证据引用）+ 自检清单
- [x] `docs/lessons/README.md` 列表含新条目

### Phase 2 - G.3 稳定审计方法提升为 skills

Status: completed
Targets: `docs/skills/<new>-prompt.md`、`docs/skills/README.md`
Skill: `none`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 完成（skill 候选可引用 lesson 的失败模式上下文）

- [x] `Decision` 裁决 skill 候选提升标准：仅当方法经 ≥2 次独立应用验证稳定且可复用、有明确输入/输出、不被现有 skill 覆盖时提升。对每个候选（compliance 基线漂移裁决法 / closure-pending 检测法 / 其他）裁决 yes/no + 记录理由。
      - Skill: `none`
      - 裁决结果：(1) compliance 基线漂移裁决法 = **YES**（4 轮独立验证 1057-1/0823-1/V.2/G.1；I/O 明确；`nop-platform-conformance-audit-prompt` 是平台最佳实践 12 维度定性审计，非 checker 基线漂移裁决，不覆盖）。(2) closure-pending 检测与批量 closure-audit = **YES**（R3.5 = Round 3 验证稳定；I/O 明确；`closure-audit-prompt` 是单计划审计，非检测+批量编排，不覆盖）。(3) 其他候选（dict 死状态检测 / 索引回填）= **NO**——前者已被 `state-machine-business-review-prompt` 覆盖，后者是单步操作非多步方法（已在 lesson 11 收录）。
- [x] `Add` 为每个入选方法创建 skill 提示文件：使用场景 / 不使用场景 / 必需输入 / 预期输出 / 步骤 / 自检反模式清单。遵循既有 skill 提示结构（如 `closure-audit-prompt.md` / `plan-audit-prompt.md`）。复制通用默认后按本项目真实 owner docs、保护区域、已知失败模式定制（对齐 README §技能路由规则）。
      - Skill: `none`
      - 产出：`compliance-baseline-drift-adjudication-prompt.md`（含项目定制化层注记 + 6 步裁决流程 + 7 项自检反模式 + 引用 lesson 07 + owner docs 背书）/ `closure-pending-detection-prompt.md`（含项目定制化层注记 + 5 步检测+批量编排 + 8 项自检反模式 + 引用 lesson 08 + 保护区域）。均按本项目真实 owner docs / 保护区域 / 已知失败模式定制（非通用模板填充）。
- [x] `Add` 更新 `docs/skills/README.md` §技能注册表 登记 新 skill 行（使用场景/不使用场景/必需输入/预期输出）。新 skill 属审计/审查方法 → 一并加入 §入门技能（审计类入门集合）；若为非入门方法则仅在注册表登记、不入 §入门技能并在 Decision 中说明理由。
      - Skill: `none`
      - 产出：§技能注册表 +2 行（四要素齐）+ §入门技能 +2 行。两 skill 均为审计/审查方法 → 按本仓库既有约定（入门集合 = 全部注册审计/审查方法，含 specialized 如 audit-remediation-roadmap-authoring）加入 §入门技能，无例外。

Exit Criteria:

- [x] 每个新 skill 文件含四要素（使用场景/不使用场景/必需输入/预期输出）+ 反模式自检
- [x] `docs/skills/README.md` 注册表含新行
- [x] 新 skill 经「复制后必须定制」规则定制（引用本项目真实 owner docs，非通用模板填充）

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is`（task `ses_047f4bb26`，fresh session）— 无 blocking。基线声明全部 live 复核通过（6 P0 + 191 P1 + 3 跨维度 = arm-index 实测；6 lessons 01-06 存在；skills 注册表四要素格式；R3.5=14 closure-pending；V.5=102 陈旧标签；候选失败模式均真实复发）。G.2+G.3 bundling 合理（共享证据基 + 顺序阶段）。mvn-gate 删除合理。Non-blocking（已采纳）：compliance-drift 证据纠正（1057-2 非独立裁决轮，真正 3 轮 = 1057-1/0823-1/V.2）；lesson 结构以 `06` 为参考范本（非单一模板）；skills 计数 `约 23`→`22`；`如适用` anti-slack 改为显式条件。**收敛达成，转 active。**

## Closure Gates

> 纯 .md 知识产物，无代码/ORM/test 变更。删除 mvn 验证门控。

- [x] 范围内行为完成（G.2 lesson 文件 + G.3 skill 文件 + 两处 README 登记）
- [x] 相关文档对齐（lessons/README + skills/README + 与 plan `-1330-1` G.4「已知失败模式」段交叉引用一致）
- [x] 已运行验证：grep 复核新文件存在 + README 登记 + 无悬空引用；compliance checker 零新增命中（纯 .md）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 未达提升阈值的候选模式

- Classification: `watch-only residual`
- Why Not Blocking Closure: 经 Decision 裁决未达「≥2 次独立应用 + 稳定可复用」阈值的模式不提升为 skill/lesson，避免技能库退化为结构化氛围编码（README §技能库不是吸引子）。
- Successor Required: `yes`（触发条件 = 该模式再次独立复发并积累第二例证据时，重新评估提升）

### 现有 skill 已覆盖的审计维度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 部分候选（如跨维度审计）已被 `multi-dimensional-audit-prompt.md` / `open-ended-audit-prompt.md` 覆盖，去重不重复创建。
- Successor Required: `no`

## Closure

Status Note: 执行者已完成 G.2（5 lessons 07-11）+ G.3（2 skills）。独立结束审计已由独立子代理（新会话，task `ses_047c82fddffe1v16IkfqoXkMcC`，cold context）执行并 **passes closure audit**。两 Phase `Status: completed`、所有 Phase 项与 Exit Criteria 已勾选、Plan Status `completed`、roadmap MG 表 G.2+G.3 `done`。本计划无 `> Source Audits:` 行（roadmap 源生计划），关闭 source audits 步骤跳过。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 fresh session（task `ses_047c82fddffe1v16IkfqoXkMcC`），执行者上下文未复用。
- Verdict: `passes closure audit`
- Evidence:
  - **范围行为**：7 新文件存在（5 lessons + 2 skills），working tree = 5 `??` lessons + 2 `??` skills + 2 `M` READMEs，纯 .md 零代码/ORM/test（删除 mvn 门控合理）。
  - **文档对齐**：lessons/README 登记 07-11 + @Inject private 排除注记；skills/README §技能注册表 + §入门技能 均含 2 新行；与 plan `-1330-1` G.4 4 模式交叉引用全覆盖（07/08/09 + @Inject private→skills README #6）。
  - **验证运行**：grep 确认 7 文件存在、README 行存在、**0 悬空引用**（21 引用路径全 resolve）、**23 个 arm-index finding ID 全存在**；纯 .md → checker 零影响。
  - **anti-hollow**：lessons 57-80 行实质内容（失败模式 + case + 证据引用 + 自检清单/决策树/反模式表）；skills 84-86 行（四要素 + 步骤 + 自检反模式 7/8 项 + 项目定制化层注记），引用真实 owner docs（抽样 compliance-baseline/processor-extension-pattern/data-dependency-matrix/shared-kernel-extraction-decision/module-boundaries/ma6-protected-area-discipline 全 EXISTS）。
  - **deferred honesty**：@Inject private（排除理由 plan + README + skills #6）/ dict 死状态 skill=NO（被 state-machine-business-review 覆盖）/ 索引回填 skill=NO（单步操作，在 lesson 11）+ Deferred But Adjudicated 段，均显式裁决非沉默降级。
  - **一致性**：Plan Status `completed`；两 Phase `Status: completed`；6 Phase item + 5 Exit Criteria + 8 Closure Gates 全 `[x]`；roadmap MG 表 G.2+G.3 `done`。
  - Non-blocking：少数 bare-filename 简写引用（posting-exemptions.md 等）无 `docs/` 前缀，均 resolve，纯 cosmetic。

Follow-up:

- 若新 skill 在后续任务中被验证有效或发现缺口，更新该 skill 提示。
- plan `-1330-1` G.4「已知失败模式」段所列模式在本计划扩展为详细 lesson 后，可在 project-context.md 补交叉引用（归 G.4 闭包或 successor）。
