# 2026-07-07 known-good-baselines 长期空白的原因与改进

> 关联计划：`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` H-3

## 原始源输入

`docs/testing/known-good-baselines.md` 模板于项目 bootstrap 阶段创建，截至 2026-07-07 综合审计时仍为空白模板（仅含 `<YYYY-MM-DD>` 占位行）。

## 缺少或误导的内容

- 模板的占位行使用了破坏 Markdown 表格格式的字符串（多列被压到一列），导致后续 AI 会话即使想填充也无清晰表头
- 模板未给"何时记录基线"的最低频次（如"每个完整切片完成时"或"每次全绿验证后"）
- 模板未指明"基线必须包含哪些命令通过状态"（如 `mvn clean install -DskipTests` 是必填还是选填）

## 仅在实施过程中发现的内容

- 项目从 2026-06-22 开始有大量"full-green verification"提交（`git log --oneline | grep full-green` 命中 50+ 条），但这些已知良好状态从未沉淀到 `known-good-baselines.md`
- 原因：提交消息标注了验证状态，但没人（包括 AI 代理）主动将其转录到 baselines 文件
- 项目上下文 `docs/context/project-context.md` 引用了 baselines 文件但未指明"必须更新"

## 下次应提前在流程中移动的内容

- "更新 known-good-baselines.md" 应作为每个非平凡计划结束审计的强制检查项（已加入 `docs/plans/00-plan-authoring-and-execution-guide.md` 的退出标准模板）
- "提交消息中包含 full-green 标记时同步更新 baselines" 应作为 git-master 技能的内置步骤

## 应创建哪些新技能、审计提示或工作流规则

- 审计提示：审计时应优先扫描 `docs/testing/known-good-baselines.md` 是否含至少一条非占位基线条目；空文件直接降级审计评分
- 工作流规则：每个计划关闭前，最新一次 `mvn clean install -DskipTests` 全绿的提交必须转录到 baselines（commit hash + 日期 + scope + 命令）

## 行动项（已落地）

- 已填充基线条目：2026-07-07 commit `957c288e` 的 full green baseline（见 `docs/testing/known-good-baselines.md`）
- 已增补：本整改计划 Phase 1 增量构建的 package-scoped baseline
