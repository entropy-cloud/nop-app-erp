# 真相源与优先级

## 目的

本文件定义哪个工件回答哪个问题。

使用它来避免混淆稳定真相、执行笔记和历史上下文。

## 按问题划分的优先级

### 现在应该构建什么？

主要来源：

- `docs/requirements/`

支持来源：

- `docs/input/`
- `docs/discussions/`

规则：

- `docs/input/` 保留原始源材料
- `docs/requirements/` 是实现就绪的解释
- 如果它们不同，显式更新需求文件，而不是默默依赖聊天记忆

### 当前支持的应用行为是什么？

主要来源：

- `docs/design/`

规则：

- `docs/design/` 拥有应用层功能、流程、角色和页面行为
- 功能需求文件可能驱动变更，但稳定的应用行为应收敛到 `docs/design/` 下的 owner docs

### 当前支持的技术结构是什么？

主要来源：

- `docs/architecture/`

规则：

- `docs/architecture/` 拥有技术边界、模块职责和横切实现规则

### 数据库真相是什么？

主要来源：

- 项目的数据库模型文件

示例：

- `model/`
- schema DSL 文件
- ORM 模型定义

规则：

- 数据库定义由模型/模式工件拥有，而非计划文本或散文文档
- 文档可以解释意图，但模型文件是真相源

### API 契约真相是什么？

主要来源：

- API schema 文件、OpenAPI/GraphQL 定义、路由定义或后端契约测试

规则：

- 散文文档可以总结 API 意图，但可执行或 schema 级别的 API 契约获胜

### 外部集成真相是什么？

主要来源：

- 外部系统的集成契约文档
- 已提交的适配器配置或集成测试

规则：

- 不要仅从 UI 需求发明外部系统行为

### 环境/部署真相是什么？

主要来源：

- 部署清单
- 环境 schema 文件
- 基础设施配置

规则：

- 计划和文档可以描述部署意图，但已提交的部署/配置工件是操作源

### 此切片应如何执行和关闭？

主要来源：

- `docs/plans/`

规则：

- 计划是执行契约，而非长期 owner docs

### 执行期间实际发生了什么？

主要来源：

- `docs/logs/`

支持来源：

- `docs/testing/`
- `docs/bugs/`
- `docs/audits/`
- `docs/retrospectives/`

### 未来 AI 会话应从重复失败中学到什么？

主要来源：

- `docs/skills/`
- `docs/lessons/`

规则：

- 使用 `docs/skills/` 存储可复用提示和剧本
- 使用 `docs/lessons/` 存储可复用工程经验和警示模式

## 冲突解决

- 如果原始输入和综合需求不一致，在编码前更新 `docs/requirements/` 或重新打开澄清。
- 如果需求和 owner docs 不一致，决定需求是否更改支持的基线；然后显式更新 `docs/design/` 或 `docs/architecture/`。
- 如果实时代码和 owner docs 不一致，将其视为实现漂移或文档过时；不要默默选择一方。
- 如果解决冲突会改变用户可见行为、数据/模型形状、API 行为、认证/权限行为或外部集成行为，请停止并请求确认。
- 如果验证失败，即使实现看起来完成，计划也不会关闭。
- 如果模型/schema 文件和散文文档在数据库真相上不一致，模型/schema 文件获胜；有意更新散文文档或模型。

## 遗留或过时文档模式

当 `docs/context/project-context.md` 将活动切片的文档新鲜度标记为 `stale`、`unknown` 或 `partially stale` 时使用此模式。

- 实时代码和可执行契约是当前行为的证据，而非自动期望的行为。
- Owner docs 仅在针对实时代码、需求和人工/产品意图重新验证后才是预期的吸引子。
- 在更改行为之前，在需求、讨论、分析或计划文件中将每个冲突分类为 `implementation drift`、`doc drift` 或 `intentional legacy behavior`。
- 在基线审计或人工确认记录应保留与应更改的内容之前，AI 自主权默认为 `research-only` 或 `plan-first`。对于 `partially stale`，此限制仅适用于其需求、owner doc、codebase-map 路由或触及代码区域未验证为新鲜的切片。
- 不要在不记录漂移分类的情况下"修复"代码以匹配陈旧文档或重写文档以匹配代码。

## 简单经验法则

- 稳定行为和结构属于 owner docs
- 执行属于计划和日志
- 历史和诊断属于 bugs、audits、testing notes、retrospectives 和 lessons