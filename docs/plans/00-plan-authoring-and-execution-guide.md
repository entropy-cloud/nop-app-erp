# 计划起草与执行指南

## 目标

`docs/plans/` 用于需要明确范围、结束标准和证明的非平凡执行切片。

## 何时编写计划

当任务满足以下条件时编写计划：

- 更改 API、数据库/模型、认证、集成、部署或公共契约行为
- 跨多个功能表面更改用户可见行为
- 涉及多个模块并更改共享行为
- 预计需要多个 AI 会话
- 修改超过 5 个文件或可能超过大约 200 行更改
- 需要分阶段实施或在结束前明确证明

## 计划决策表

| 范围 | 计划级别 | 审计规则 | 示例 |
| ----------------------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| 琐碎的本地编辑 | 无计划 | 无草案审查 | 打字错误/文案更改、单一样式调整、仅测试清理 |
| 非平凡跟踪工作 | 完整计划 | 需要独立草案审查和独立结束审计 | 带文档/测试更新的小型 UI 改进、带明确现有测试的简单本地 bug 修复 |
| 契约、数据/模型、API、认证、权限、集成、部署、跨表面、陈旧文档冲突或明显高风险范围 | 完整计划 | 需要独立草案审查和独立结束审计 | 结账流程、登录行为、数据迁移、外部 webhook、多模块重构 |

如果不确定，使用完整计划。

## 最低规则

1. **从实时基线开始。** 先读取仓库，然后编写 `Current Baseline`。不要依赖记忆或旧计划。对于全新功能，基线必须盘点功能将触及或矛盾的所有现有代码 — 硬编码值、缺失的钩子、不兼容的模式。盘点是必需的。
2. **编写目标和非目标。** 如果任何一个不清楚，计划边界尚未就绪。
3. **使用复选框进行执行和结束。** 未勾选的项目意味着在结束前未完成的工作。
4. **一个计划，一个结果表面。** 如果计划需要多个独立结束标准，则范围太广。拆分它。共享相同行为契约和结束标准的多模块提取或迁移仍然是**一个**结果表面 — 不要过度拆分。
5. **结束前证明。** 在仓库包含每个退出标准的可验证证明之前，不要标记计划完成。
6. **无代码设计转储。** 计划捕获范围、证明和结束逻辑，而非低级实现细节。例外：重构和提取计划**必须**包含提取模块之间的接口契约 — 这些是结构边界定义，而非实现伪代码。
7. **用类型标记项目。** 每个执行项目必须是 `Fix`、`Add`、`Decision`、`Proof` 或 `Follow-up`。`Fix` 涵盖缺陷修复；`Add` 涵盖新增代码或配置。项目可能携带多个类型（例如 `Decision | Add`）；当携带多个类型时，所有隐含义务都适用。已确认的实时缺陷或契约漂移必须是 `Fix`，而非 `Follow-up`。当一个阶段中 80%+ 的项目共享一种类型时，在阶段级别声明统一类型而非每个项目单独声明（例如 `Phase 1 — Fix-heavy (8/10 items tagged Fix)`）。
8. **刻意记录技能使用。** 对于可复用技能重要的每个阶段或项目，记录 `Skill: <name>` 或 `Skill: none`。技能选择工作方法，而非业务真相。如果命名了技能，其必需输入和预期输出必须已经从 `docs/skills/README.md` 和引用的 owner docs 中明确。
9. **记录带有理由的决策。** 每个 `Decision` 项目必须记录选择、考虑的替代方案以及任何残留风险。将理由写入计划或引用的文档中。如果决策需要在提交前进行原型设计或探索，添加临时的 `Explore` 项目，该项目必须在 `Decision` 解决之前完成。框架强制或明显的选择（例如"必须匹配现有框架模式"）可以作为约束记录，无需完整的替代方案分析。
10. **结束前检查清单完整性。** 在标记计划完成之前，范围内的任何检查清单项目都不得保持未勾选状态。要么完成它，要么明确将其移出范围并写入理由。计划批准后的范围缩小是范围变更，必须记录理由；静默从范围中移除项目是违规行为。
11. **结束前文本一致性。** 结束前，验证 `Plan Status`、每个阶段的 `Status`、每个阶段的 `Exit Criteria`、`Closure Gates` 和 `docs/logs/` 条目都一致。顶部显示 `completed` 而内部某个阶段仍显示 `draft` 是不允许的。
12. **独立草案审查和结束审计。** 在独立草案审查将计划修订为可接受的执行契约之前，不要实施创建的计划；不要将其标记为完成作为完成最后一个实现切片的副作用。使用单独的审查通过。结束审计**不得**在执行会话中运行：必须由独立子代理（不重用执行者上下文的新会话）执行直到通过；执行者不得自我审计，不得勾选结束审计门控，不得将其留为 `[ ]` 作为"人工门控"占位符。如果没有独立代理可用，执行者必须显式生成新的子代理会话进行审计；否则计划保持打开状态。保护区域、未解决的产品风险和真相源冲突需要人工/子代理审查或保持阻塞。
13. **不可降级项目**不能降级为非阻塞跟进：已确认的实时缺陷、已确认的契约漂移、已确认的 owner-doc 漂移以及仓库中已修复的 CI/lint 规则。
14. **同一组件的多个功能优先使用一个 owner plan。** 当多个独立功能属于同一组件（相同的 owner doc、相同的结果表面）时，将它们写为单个计划中的阶段，而不是每个功能一个计划。仅当它们具有实质性不同的结束标准、owner-doc 义务或验证路径时才拆分为单独的计划。这是规则 4（"一个计划，一个结果表面"）针对常见"组件功能增强"形状的具体形式，旨在防止队列被每个功能一个计划的碎片 clutter。

## 计划状态流程

有意使用这些状态：

- `draft` - 计划存在但尚未通过独立草案审查
- `active` - 独立草案审查已收敛到可接受的执行契约，可以开始实施
- `completed` - 独立结束审计接受结束
- `superseded | replaced | deferred | cancelled` - 当计划不再以其原始形式拥有实时结束时使用

创建计划的推荐默认流程：

1. 创建第一个诚实草案为 `draft`
2. 运行独立草案审查直到草案可接受
3. 在 `## Draft Review Record` 中记录迭代
4. 将 `Plan Status` 更改为 `active`
5. 执行并更新阶段/工作流状态
6. 仅在独立结束审计后关闭

### 反松弛规则

结束前的每个范围内项目必须恰好处于一种状态：`landed`、`adjudicated as residual-risk-only`、`moved to explicit successor ownership` 或 `removed from scope with recorded reason`。

范围内项目禁止使用以下词语：`optional`、`if time permits`、`consider`、`maybe`、`nice to have`、`as needed`。如果项目确实是可选的，请明确将其移出范围，而不是让它处于模糊状态。

`Follow-up` 项目必须命名将其提升到范围内的触发条件（例如"当用户数量超过 10K 时"）。`Deferred But Adjudicated` 项目必须命名将重新打开它的事件或决策（例如"如果采用新 API，这项工作可能变得多余"）。

## 执行时

1. 实施前，直接修订计划直到独立草案审查未发现阻塞问题，然后默认将草案审查证据持久记录在计划中。
2. 草案审查期间保持新计划为 `Plan Status: draft`。仅在草案审查记录显示计划可接受执行后才更改为 `active`。
3. 开始切片时，将其 `Status` 更新为 `in progress`。
4. 完成切片时，将其 `Status` 更新为 `completed` 并勾选其所有执行项目和退出标准。
5. 执行阶段前，确认列出的 `Skill` 仍然匹配任务和可用输入。如果不匹配，在继续前更新计划。
6. owner-doc 对齐是**计划级**义务，而非固定的每阶段项目。阶段的退出标准应列出文档更新步骤**仅当该阶段实际更改实时基线、公共契约或 owner 行为时**。不要写 `No owner-doc update required` 作为填充槽的样板 — 如果没有更改，什么都不写。整体 owner-doc 一致性在结束步骤（文本一致性检查）中验证，而非在每个阶段退出中重复。这与规则 10（检查清单完整性）相同的逻辑：不要用无操作项目填充退出标准。
7. **完整仓库验证默认属于 Closure Gates，而非阶段退出标准。** 阶段的退出标准应仅包括证明该阶段交付其可观察结果以及解除后续阶段阻塞所需的检查（通常是重点单元测试或新代码的本地化类型检查）。不要在每个阶段退出中重复完整仓库的 `typecheck`/`build`/`lint`/`test` — 在结束时运行一次（Closure Gates 已经涵盖此）。例外：如果一个阶段更改了下一阶段立即依赖的公共契约，写一个本地化检查（例如"更改包的类型检查通过"）来解除阻塞，但不需要每个阶段都进行完整的 `build`。
8. 不要因为函数签名存在就标记切片完成。验证行为、错误处理和测试覆盖也已落地。
9. 如果项目无法完成，将其移至 `Deferred But Adjudicated` 并分类和说明原因。不要将其留在执行列表中未勾选。
10. 保持 `docs/logs/` 与计划进度同步。当所有阶段在一个 sprint 中涵盖同一功能时，计划结束时的单个聚合日志条目就足够了；仅当阶段跨越不同日期或不同可交付成果时才需要单独的阶段条目。

## 结束时

在设置 `Plan Status: completed` 之前，完成以下所有操作：

**所有创建的计划：**

1. 检查每个阶段的 `Exit Criteria` — 每个必须是 `[x]`。
2. 检查每个 `Closure Gates` 项目 — 每个必须是 `[x]`。
3. 验证文本一致性：顶部状态、阶段状态、退出标准、结束门控和日志条目都一致。
4. 区分"接口存在"与"行为完整"。使用测试或演示验证实际运行时行为，而非仅类型签名。
5. 运行仓库的真实验证命令。对于主要结果表面是视觉、行为或 UX 驱动的计划，在计划中使用明确理由自定义验证门控。
6. 结束审计**不得**在执行会话中运行。生成独立子代理（新会话，无执行者上下文）进行审计直到通过；执行者不得自我审计，不得勾选结束审计门控，不得将其留为 `[ ]` 作为"人工门控"。如果没有独立代理可用，计划保持打开状态。
7. 如果计划使用了单独冷重播回退（见 `AGENTS.md` 审查者可用性回退），结束记录**必须**声明已使用它，并确认针对计划、受影响文档、实际差异和真实验证命令执行了冷重播自检。

**完整结束**（多会话、多模块或高风险计划 — 添加这些）：

7. 从头开始重新阅读整个计划，而不仅仅是最近的切片。
8. 在计划的 `Closure` 部分记录独立结束审计证据，并在存在时链接 `docs/audits/` 下的任何存储审计文件。

如果任何一项失败，计划保持打开状态。

## 模板

```md
# <plan-id> <title>

> Plan Status: draft
> Last Reviewed: YYYY-MM-DD
> Source: <requirement / bug / analysis / request>
> Related: <related plans, optional>
> Audit: required

## Current Baseline

- <当前真实情况>
- <剩余差距>

## Goals

- <要实现的结果>

## Non-Goals

- <明确排除的工作>

## Task Route

- Type: `<requirement clarification | app-layer design change | architecture change | implementation-only change | bug investigation | verification or audit work>`
- Owner Docs: `<paths>`
- Skill Selection Basis: `<为什么应用这些技能或不应用>`

## Infrastructure And Config Prereqs

- <此功能依赖的端口、环境变量、CORS、密钥、.env、外部服务>
- <如果没有，写"No infra prereqs beyond existing baseline">
- <对于数据迁移计划：包括回滚策略或脚本路径>

## Execution Plan

### Phase 1 - <name>

Status: planned
Targets: `<paths>`
Skill: `<skill-name | none>`

- Item Types: `Fix | Decision | Proof | Follow-up`
- Prereqs: <必须先完成的阶段或外部依赖>

- [ ] <implementation item>
      - Skill: `<skill-name | none>`
- [ ] <Decision: 在项目或引用文档中记录理由和替代方案>
  - Skill: `<skill-name | none>`
- [ ] <Proof: 指定测试策略（单元/集成/e2e）和确切验证命令>
  - Skill: `<skill-name | none>`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。不要用样板填充：当没有更改时完全省略 owner-doc 行（不要写 `No owner-doc update required` 来填充槽）。完整仓库的 `typecheck`/`build`/`lint`/`test` 属于 Closure Gates，而非此处（见执行时规则 7）。`docs/logs/` 是计划级结束步骤，而非每阶段项目。

- [ ] <此阶段交付的可观察结果 — 指定成功和失败模式>
- [ ] <本地化验证，仅当后续阶段依赖它时：重点单元测试或更改代码的类型检查>

## Draft Review Record

- Independent draft review iteration 1: <needs revision | acceptable as-is | accept> (<task/session id>) because <why>
- Independent draft review iteration 2: <needs revision | acceptable as-is | accept> (<task/session id>) after <what changed>

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：在结束时运行 `typecheck`/`build`/`lint`/`test`（或项目等效命令）一次。不要在阶段退出标准中重复这些 — 阶段仅验证其交付的内容以及解除后续阶段阻塞的内容（见执行时规则 7）。对于无代码更改的计划（仅文档），删除验证命令门控并说明原因。

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（指定哪些命令；如果需要，针对视觉/UX 域自定义）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### <item name>

- Classification: `watch-only residual | optimization candidate | out-of-scope improvement`
- Why Not Blocking Closure: <reason>
- Successor Required: `yes | no`

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
```