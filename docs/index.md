# nop-app-erp 文档索引

## 目的

此 `docs/` 树是 `nop-app-erp` 的持久记忆和路由表面。

- 在进行工作流、需求、设计或实现更改之前从这里开始
- 优先使用回答当前问题的最小文件
- 将持久结论保存在文件中，而不仅仅留在聊天中

## 路由权威

本文件是顶级文档路由器。

- `docs/index.md` 拥有导航和目录职责
- `AGENTS.md` 拥有代理工作流规则和执行期望
- `docs/design/` 和 `docs/architecture/` 拥有稳定的项目吸引子

## 首先阅读

| 如果你需要... | 首先阅读 | 然后阅读 |
| -------------------------------------------------------------------- | --------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| 了解强制 AI 上下文和当前项目状态 | `docs/context/README.md` | `docs/context/project-context.md`、`docs/context/ai-autonomy-policy.md`、`docs/context/codebase-map.md` |
| 了解轻量级默认开发工作流 | `docs/process/application-development-workflow.md` | `AGENTS.md` |
| 选择下一个 AI 就绪工作项 | `docs/backlog/README.md` | `docs/context/ai-autonomy-policy.md`、活动需求和 owner doc |
| 了解阶段级进度和依赖关系（当路线图存在时） | `docs/backlog/00-roadmap-authoring-guide.md` | `docs/backlog/implementation-roadmap.md` |
| 阅读原始 PM、原型、文章或卡片集输入 | `docs/input/README.md` | `docs/input/` 中的活动文件 |
| 阅读解释性方法论文章 | `docs/articles/README.md` | `docs/articles/` 下的相关文章 |
| 澄清模糊需求 | `docs/discussions/README.md` | `docs/requirements/00-requirement-synthesis-guide.md` |
| 编码前路由任务 | `AGENTS.md` | `docs/skills/README.md`、相关 owner doc 和 `docs/plans/00-plan-authoring-and-execution-guide.md` |
| 决定现有技能是否适用 | `docs/skills/README.md` | 相关 owner doc 和活动需求 |
| 了解项目目标和产品形态 | `docs/architecture/project-vision.md` | `docs/design/app-overview.md` |
| 了解当前应用层基线 | `docs/design/app-overview.md` | `docs/design/feature-inventory.md`、`docs/design/roles-and-permissions.md` |
| 了解哪个域文档拥有概念（多域项目） | `docs/design/domain-design-guidelines.md` | `docs/design/` 下的相关域文档 |
| 了解全局流程和状态/域/页面映射 | `docs/design/flow-overview.md` | `docs/design/` 下的相关域文档 |
| 了解当前技术基线 | `docs/architecture/system-baseline.md` | `docs/architecture/module-boundaries.md` |
| 了解与竞品的对标和超越点 | `docs/architecture/competitive-comparison.md` | `docs/design/erp-design-audit-checklist.md` |
| 了解持久化模型或字典真相 | `docs/context/source-of-truth-and-precedence.md` | `module-<domain>/model/app-erp-<domain>.orm.xml` |
| 了解 Nop 实现决策顺序 | `../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md` | `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md`、`../nop-entropy/docs-for-ai/02-core-guides/service-layer.md` |
| 决定更改应使用模型、Delta、钩子还是 Java | `../nop-entropy/docs-for-ai/INDEX.md` | `../nop-entropy/docs-for-ai/03-runbooks/` |
| 从 ORM 模型生成多模块项目 | `module-<domain>/model/app-erp-<domain>.orm.xml` | `../nop-entropy/docs-for-ai/03-runbooks/`（代码生成 runbooks） |
| 了解 owner-doc 优先级和真相源边界 | `docs/context/source-of-truth-and-precedence.md` | 相关 owner doc |
| 开始或审查非平凡实现 | `AGENTS.md` | `docs/skills/README.md`、`docs/plans/00-plan-authoring-and-execution-guide.md`、活动计划和 `docs/audits/00-audit-execution-guide.md` |
| 审查审计工作流或所需草案审查/结束审计规则 | `docs/audits/00-audit-execution-guide.md` | `docs/skills/` 中的相关提示 |
| 审计业务状态机的正确性和可达性 | `docs/skills/state-machine-business-review-prompt.md` | 定义状态机的 owner doc |
| 将设计文档审计为应用层行为基线 | `docs/skills/design-doc-audit-prompt.md` | `docs/design/README.md`、存在时的 `docs/design/domain-design-guidelines.md` |
| 了解哪些文档应使用日期文件名与稳定名称 | `docs/references/document-naming-and-timeliness.md` | 目标目录中的相关指南 |
| 快速复制推荐的文件名模式用于新的日期文档 | `docs/references/document-naming-and-timeliness.md` | `Quick Copy Set` 部分 |
| 复制现成的日期文档骨架 | `docs/examples/README.md` | 重命名最接近的 `.example.md` 文件 |
| 查看一个真实的小型功能演练 | `docs/examples/complete-small-app-walkthrough.md` | 然后从 `docs/examples/` 复制最接近的骨架 |
| 诊断端到端测试失败（Playwright） | `docs/references/playwright-e2e-guide.md` | `playwright.config.ts` |
| 检查更改后必须更新哪些文档 | `docs/references/maintenance-checklist.md` | `docs/design/` 或 `docs/architecture/` 中最相关的文件 |
| 查看最近的实现历史 | `docs/logs/index.md` | 最新的日期日志文件 |
| 查找过去的微妙回归 | `docs/bugs/00-bug-fix-note-writing-guide.md` | `docs/bugs/` 中的相关文件 |
| 记录或查看探索性/手动测试 | `docs/testing/index.md` | 相关的日期测试笔记 |
| 检查最新的已知良好验证状态 | `docs/testing/known-good-baselines.md` | 最新的日期测试或日志笔记 |
| 查看权衡或开放设计调查 | `docs/analysis/README.md` | 相关分析笔记 |
| 查看持久可复用工程经验 | `docs/lessons/README.md` | 相关编号经验 |
| 阅读实现就绪需求 | `docs/requirements/README.md` | 活动需求文件 |
| 查看为什么落地结果仍未达到预期 | `docs/retrospectives/README.md` | 相关回顾笔记 |

## 推荐默认路径

对于大多数中小型项目，默认路径是：

1. `docs/context/`
2. `docs/backlog/`（选择工作时）
3. `docs/input/`
4. `docs/requirements/`
5. `docs/design/` 和 `docs/architecture/`
6. 路由任务并选择候选可复用技能
7. `docs/plans/`（当规划触发条件适用时）
8. `docs/audits/`（审计工作流指导或存储的非平凡审计记录）
9. `docs/logs/`
10. `docs/bugs/`（需要时）

仅当任务复杂性或歧义证明合理时，才使用 `docs/discussions/`、额外的 `docs/testing/` 笔记、`docs/skills/`、`docs/analysis/` 和 `docs/retrospectives/`。

## 技能路由

| 如果任务是... | 首先阅读 | 然后决定 |
| --------------------------------------- | ----------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 模糊需求 | `docs/requirements/00-requirement-synthesis-guide.md` | 是否首先需要需求文件或讨论文件 |
| 非平凡实现 | `AGENTS.md` | 每个阶段或项目需要哪些技能，然后使用 `docs/plans/00-plan-authoring-and-execution-guide.md` |
| 文档、计划或结束验证 | `docs/skills/README.md` | 哪个审计提示或审查技能适用 |
| 重复已知方法或审查模式 | 相关 owner doc | 现有技能是否适用或是否应创建新技能 |

技能选择工作方法。它们不替代需求、设计、架构或 owner-doc 路由。

## 域快速参考（可选）

当项目有多个域时，添加快速参考表，以便通过一次查找即可将更改路由到正确的 owner doc 和技能。这是可选的；小型项目可以跳过。从真实项目填充表并将其保存在 `AGENTS.md` 或本文件中。

| 更改区域 | 首先阅读 | 要加载的技能 |
| ----------- | ---------- | ------------- |
| <area> | `docs/<path>` | `<skill-name \| none>` |

## 目录角色

- `docs/process/` - 工作流和操作流程文档
- `docs/context/` - 强制 AI 上下文、所有者优先级和项目范围约定
- `docs/backlog/` - 优先候选工作、AI 就绪下一个操作，以及需要阶段级进度时的可选路线图
- `docs/input/` - 原始外部输入和复制的源材料
- `docs/discussions/` - 可选需求澄清和未解决问题记录
- `docs/requirements/` - 综合的实现就绪需求文档
- `docs/design/` - 稳定的应用层功能和业务流 owner docs
- `docs/architecture/` - 稳定的技术基线和模块边界文档
- `docs/lessons/` - 从重复问题和恢复中提取的持久工程经验
- `docs/references/` - 稳定的查找指南和维护辅助工具
- `docs/articles/` - 面向外部的方法论和解释性文章
- `docs/examples/` - 可复制的日期工作文档骨架
- `docs/plans/` - 带有结束标准的执行计划
- `docs/audits/` - 审计方法和可选存储的审计记录
- `docs/skills/` - 可选可复用 AI 提示和审计/审查剧本
- `docs/logs/` - 带日期的实现记忆
- `docs/testing/` - 可选探索性和手动测试笔记
- `docs/bugs/` - 复杂回归历史和根本原因笔记
- `docs/analysis/` - 可选调查、比较和设计权衡
- `docs/retrospectives/` - 可选交付后差距分析和流程改进
- `docs/archive/` - 人工决策移至此处的非活动文档；保留用于历史参考

## 核心原则

使用文件保存持久真相。

- input 捕获需求来源
- context 捕获强制项目规则和真相源优先级
- backlog 捕获优先下一个操作和自主标签
- discussions 捕获不清楚的内容
- requirements 捕获应该构建什么
- design 和 architecture 捕获必须保持真实的内容
- source-of-truth precedence 告诉每个问题哪个工件获胜
- plans 捕获非平凡切片将如何结束
- audits 捕获声明如何受到挑战
- logs、tests 和 bug notes 保留证明和记忆
- retrospectives 解释为什么最后一次迭代仍然未达到目标

## 命名规则

- 稳定的 owner docs 保持稳定名称
- 时间敏感记录通常应包含日期
- 见 `docs/references/document-naming-and-timeliness.md`