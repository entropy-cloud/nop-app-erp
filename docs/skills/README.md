# 技能索引

使用此目录存储可复用提示和工作流剧本。

这些不是一次性聊天消息。它们是可复用的仓库记忆。

技能应主要捕获可复用工作方法、审查方法或审计方法。不要将技能用作需求真相、设计真相或架构真相的替代品。

技能库不是吸引子。如果不通过 `AGENTS.md`、`docs/index.md`、活动需求和 owner docs 进行路由，大型技能库通常会退化为结构化氛围编码。

这些提示是复制项目的通用默认值。复制模板后，**必须**根据项目的真实 owner docs、保护区域、验证堆栈、命名约定、已知失败模式和误报容忍度进行定制。

## 技能路由规则

选择技能前：

1. 首先阅读相关需求和 owner docs。
2. 使用 `AGENTS.md` 对任务类型进行分类。
3. 通过匹配工作方法而非仅业务标签来选择技能。
4. 如果多个技能都可能适合，请在实施前请独立子代理或审查者选择。
5. 如果没有现有技能明显适合，记录 `Skill: none` 并继续正常的文档驱动工作流。
6. 对于非平凡计划，在计划中记录技能选择依据和审查结果。

不要添加广泛的业务场景技能来替代项目特定的 owner docs。如果场景经常重复，请首先检查是否缺少路由、owner docs 或计划指南。仅当可复用工作方法稳定时才提升技能。

## 技能注册表

| 技能 | 使用场景 | 不使用场景 | 必需输入 | 预期输出 |
| ----------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------- |
| `age-practice-gap-audit-prompt.md` | 仓库需要比较实时实践与预期 AGE 工作流 | 任务是本地功能实现 | AGE 基线文档、当前仓库结构、活动文档、采样实时证据 | `docs/analysis/` 下的分析笔记，含优先级差距 |
| `document-audit-prompt.md` | 需求、设计或架构文档可能不完整或不一致 | 任务琐碎且本地 | 目标文档路径、相关输入或 owner docs | 审计发现和修订目标 |
| `design-doc-audit-prompt.md` | `docs/design/` 需要重新验证为应用层行为基线 | 需要单一更窄的审计（状态机、计划） | 所有 `docs/design/` 文件、相关需求、存在时的 `domain-design-guidelines.md`、**必须含 `erp-survey/` 覆盖矩阵作为功能对标基准** | 按严重性排序的发现和处理结果 |
| `design-completeness-scan-prompt.md` | 主动扫描 `docs/design/` 以查找目标范围内缺失的域/文档/功能 | 验证现有文档（改用 `design-doc-audit-prompt.md`） | `docs/design/` 树、`product-scope.md`、路线图、`flow-overview.md`、**必须含 `erp-survey/` 全部报告（否则会遗漏未设计的功能）** | 优先级差距列表，驱动下一轮文档添加 |
| `state-machine-business-review-prompt.md` | 工作流状态机（订单/审批/争议/生命周期）需要正确性审查 | 更改琐碎或与转换无关 | 定义状态机的 owner doc、相关需求 | P0–P3 发现、裁决、可达性/角色/外部摘要 |
| `plan-audit-prompt.md` | 非平凡计划在实施前准备好接受挑战 | 尚无计划 | 计划文件、相关需求和 owner docs | 通过/失败审计，含具体问题 |
| `closure-audit-prompt.md` | 实施声称完成并需要独立结束审查 | 工作仍在进行中 | 计划、验证证据、相关更改文档 | 结束裁决和剩余差距 |
| `requirement-gap-retrospective-prompt.md` | 落地工作仍未达到预期，需求管道需要诊断 | 需求仍在起草中 | 原始输入、需求/讨论文档、交付结果 | 回顾发现和流程修正 |
| `multi-dimensional-audit-prompt.md` | 高风险工作需要同时跨多个维度挑战 | 单一对象审计已足够 | 相关需求/owner docs、计划或更改区域、验证证据 | 按维度分组的发现 |
| `open-ended-audit-prompt.md` | 正常检查清单之外可能存在隐藏问题 | 工作仅需要狭窄的结构化审计 | 相关需求/owner docs、计划（如有）、日志、实时更改代码 | 对抗性发现和未知风险笔记 |
| `index-routing-audit-prompt.md` | 文档索引或目录结构需要路由有效性审查 | 索引没有路由角色或琐碎 | 顶层索引、子索引、目标文件 | 覆盖表、角色测试结果、结构发现 |
| `bug-diagnosis-prompt.md` | Bug 真实存在但根本原因尚未证明 | 缺陷已经明显且本地 | Bug 报告、owner docs、复现路径、验证命令 | 确认原因和证明路径 |
| `code-quality-audit-prompt.md` | 审查代码的行为风险和实现质量 | 仅需要格式或琐碎细节 | 更改文件、owner docs、测试或验证证据 | 按严重性排序的发现 |
| `code-refactor-discovery-prompt.md` | 结构清理候选需要在重构前发现 | 结构目标已经达成一致 | 目标区域、owner docs、当前代码 | 排名重构候选 |
| `code-refactor-prompt.md` | 行为保留结构重构工作是任务 | 任务更改支持的行为 | 目标区域、不变量、验证命令 | 安全重构执行和证明 |
| `orm-model-audit-prompt.md` | `<domain>/model/*.orm.xml` 需要规范与完整性审计（类型/长度/字典/标准字段/业务字段/关系） | 单模块内部审计、需求综合 | `domain-design-guidelines.md` §10/§11、平台 `orm-model-design.md`、所有 orm.xml | 按维度的问题清单 + 裁决 + 字段补齐统计 |
| `cross-module-dependency-audit-prompt.md` | 多模块跨工程数据依赖合理性、DAG 合规性、外部实体引用一致性审计 | 单模块审计、需求综合 | `module-boundaries.md`、`data-dependency-matrix.md`、`cross-module-entity-reference.md`、所有 orm.xml | DAG 验证结果 + 外部实体声明完整性矩阵 + 裁决 |
| `nop-platform-conformance-audit-prompt.md` | 项目设计与实现对 Nop Platform 最佳实践的遵循度审计 | 业务设计审计（用 design-doc-audit）、ORM 字段审计（用 orm-model-audit） | `../nop-entropy/docs-for-ai/` 全部、项目 architecture 文档 | 12 维度合规率 + 反模式清单 + 裁决 |

## 入门技能

- `age-practice-gap-audit-prompt.md`
- `document-audit-prompt.md`
- `design-doc-audit-prompt.md`
- `design-completeness-scan-prompt.md`
- `state-machine-business-review-prompt.md`
- `plan-audit-prompt.md`
- `closure-audit-prompt.md`
- `requirement-gap-retrospective-prompt.md`
- `multi-dimensional-audit-prompt.md`
- `open-ended-audit-prompt.md`
- `index-routing-audit-prompt.md`
- `bug-diagnosis-prompt.md`
- `code-quality-audit-prompt.md`
- `code-refactor-discovery-prompt.md`
- `orm-model-audit-prompt.md`
- `cross-module-dependency-audit-prompt.md`
- `nop-platform-conformance-audit-prompt.md`
- `code-refactor-prompt.md`

## 与工具原生技能的关系

`docs/skills/` 包含存储在仓库内并跨工具和编辑器保持可移植的方法和审计类型技能。一些 AI 工具也支持自己的原生技能加载（例如工具自动加载的项目本地技能目录）。两者互补而非竞争：

- 将可复用审查/审计方法和提示放在 `docs/skills/` 中，以便它们与文档一起版本化，并可供任何代理或人工读取
- 如果使用工具，请将工具加载的操作技能（特定于框架的操作指南、代码生成配方、调试配方）放在工具自己的约定中
- 无论技能物理位置如何，都通过 `AGENTS.md`、`docs/index.md` 和 owner docs 进行路由；技能选择工作方法，不替代 owner-doc 路由

模板本身保持工具中立，不假设任何特定的 AI 工具。