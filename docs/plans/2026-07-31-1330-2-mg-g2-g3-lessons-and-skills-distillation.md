# 2026-07-31-1330-2-mg-g2-g3-lessons-and-skills-distillation MG G.2+G.3 审计-修复任务知识沉淀：lessons 失败模式 + skills 审计方法复用化

> Plan Status: active
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

Status: planned
Targets: `docs/lessons/07-*.md` ... `docs/lessons/NN-*.md`、`docs/lessons/README.md`
Skill: `none`

- Item Types: `Decision | Add | Proof`
- Prereqs: MV done（已满足）

- [ ] `Decision` 从候选清单（6 模式）去重裁决：排除已被 `01`–`06` 覆盖的（如 `06` 已覆盖 codegen 产物编辑覆盖）；确定本轮提升的 lesson 清单与编号（续 `07`+）。记录排除理由。
      - Skill: `none`
- [ ] `Add` 为每个入选模式创建 lesson 文件：标题 + 失败模式一句话 + 具体 case（引用 arm-index finding ID / 计划 / file:line 证据）+ 「如何避免」自检清单 + 「何时复发」触发条件。以 `06-codegen-product-edit-overwrite.md`（含完整 自检清单/决策树/反模式 段）为结构参考范本；不照搬单一模板（`04` 与 `06` 结构有差异，按内容形态选最贴合的结构）。
      - Skill: `none`
- [ ] `Proof` 更新 `docs/lessons/README.md` §Lessons 列表登记新 lesson（一句话摘要）。
      - Skill: `none`

Exit Criteria:

- [ ] 每个新 lesson 文件含：可识别失败模式 + 至少一个 mission 内具体 case（带证据引用）+ 自检清单
- [ ] `docs/lessons/README.md` 列表含新条目

### Phase 2 - G.3 稳定审计方法提升为 skills

Status: planned
Targets: `docs/skills/<new>-prompt.md`、`docs/skills/README.md`
Skill: `none`

- Item Types: `Decision | Add`
- Prereqs: Phase 1 完成（skill 候选可引用 lesson 的失败模式上下文）

- [ ] `Decision` 裁决 skill 候选提升标准：仅当方法经 ≥2 次独立应用验证稳定且可复用、有明确输入/输出、不被现有 skill 覆盖时提升。对每个候选（compliance 基线漂移裁决法 / closure-pending 检测法 / 其他）裁决 yes/no + 记录理由。
      - Skill: `none`
- [ ] `Add` 为每个入选方法创建 skill 提示文件：使用场景 / 不使用场景 / 必需输入 / 预期输出 / 步骤 / 自检反模式清单。遵循既有 skill 提示结构（如 `closure-audit-prompt.md` / `plan-audit-prompt.md`）。复制通用默认后按本项目真实 owner docs、保护区域、已知失败模式定制（对齐 README §技能路由规则）。
      - Skill: `none`
- [ ] `Add` 更新 `docs/skills/README.md` §技能注册表 登记 新 skill 行（使用场景/不使用场景/必需输入/预期输出）。新 skill 属审计/审查方法 → 一并加入 §入门技能（审计类入门集合）；若为非入门方法则仅在注册表登记、不入 §入门技能并在 Decision 中说明理由。
      - Skill: `none`

Exit Criteria:

- [ ] 每个新 skill 文件含四要素（使用场景/不使用场景/必需输入/预期输出）+ 反模式自检
- [ ] `docs/skills/README.md` 注册表含新行
- [ ] 新 skill 经「复制后必须定制」规则定制（引用本项目真实 owner docs，非通用模板填充）

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is`（task `ses_047f4bb26`，fresh session）— 无 blocking。基线声明全部 live 复核通过（6 P0 + 191 P1 + 3 跨维度 = arm-index 实测；6 lessons 01-06 存在；skills 注册表四要素格式；R3.5=14 closure-pending；V.5=102 陈旧标签；候选失败模式均真实复发）。G.2+G.3 bundling 合理（共享证据基 + 顺序阶段）。mvn-gate 删除合理。Non-blocking（已采纳）：compliance-drift 证据纠正（1057-2 非独立裁决轮，真正 3 轮 = 1057-1/0823-1/V.2）；lesson 结构以 `06` 为参考范本（非单一模板）；skills 计数 `约 23`→`22`；`如适用` anti-slack 改为显式条件。**收敛达成，转 active。**

## Closure Gates

> 纯 .md 知识产物，无代码/ORM/test 变更。删除 mvn 验证门控。

- [ ] 范围内行为完成（G.2 lesson 文件 + G.3 skill 文件 + 两处 README 登记）
- [ ] 相关文档对齐（lessons/README + skills/README + 与 plan `-1330-1` G.4「已知失败模式」段交叉引用一致）
- [ ] 已运行验证：grep 复核新文件存在 + README 登记 + 无悬空引用；compliance checker 零新增命中（纯 .md）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

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

Status Note: <pending closure>

Closure Audit Evidence:

- Auditor / Agent: <pending independent closure audit>
- Evidence: <pending>

Follow-up:

- 若新 skill 在后续任务中被验证有效或发现缺口，更新该 skill 提示。
- plan `-1330-1` G.4「已知失败模式」段所列模式在本计划扩展为详细 lesson 后，可在 project-context.md 补交叉引用（归 G.4 闭包或 successor）。
