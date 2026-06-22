# AI 自主政策

## 目的

本文件定义 AI 代理何时可以无需询问继续执行，何时必须停止等待人工输入。

保持简短且项目特定。每当团队希望 AI 采取更多或更少主动性时更新此文件。

AI 可以通过标记工作为更受约束来使此文件更严格，但 AI 不得放宽保护区域、将 `ask-first`/`blocked`/`research-only` 工作更改为 `implement`，或在没有明确人工确认或标记为人工批准的 owner-doc 证据的情况下移除阻塞项。

AI 编写或修改的文档（包括 owner docs）不能作为放宽自主权、清除阻塞项、标记文档新鲜或降级保护区域的证据，除非人工明确批准该证据。

## 自主级别

在待办事项/路线图工作项上使用这些标签（它们是每个项目的，而非 `project-context.md` 中的全局字段）：

- `implement` - AI 可以在阅读列出的需求、owner doc 和验证命令后实施。
- `plan-first` - AI 可以起草或更新计划，但实施需等待计划审计和下表要求的任何保护区域批准。
- `ask-first` - AI 必须在更改代码或用户可见行为之前询问。
- `research-only` - AI 可以检查、总结和提出选项，但不得修改产品行为。
- `blocked` - AI 在阻塞项解决之前不得继续（在文件中或通过人工确认）。

没有明确标签的工作项默认级别为 `implement`。默认值受文档新鲜度（`project-context.md`）和下面的保护区域控制。人工可以通过编辑此文件收紧项目默认值；AI 可以根据证据收紧（永不放宽）。

## 审查者可用性

为复制的项目设置一个值：

- 审查者可用性：`subagent`

如果此值仍然是占位符，将审查者可用性视为 `none`，并将保护区域或高风险计划视为阻塞，直到配置人工/子代理审查。

规则：

- `human` 或 `subagent` - 使用该审查者进行所需的计划和结束审计。
- `none` - 冷重播仅可用于非保护、非高风险计划。冷重播不是第二位审查者；它是在实施上下文被搁置后执行的记录自查。
- 保护区域、未解决的产品风险或真相源冲突仍然需要人工/子代理审查或必须保持阻塞。

## AI 无需询问即可继续的情况

- 工作项标记为 `implement`（或没有标签且默认为 `implement`）或用户直接请求本地低风险更改
- 需求或 owner doc 使用具体验收标准描述工作的预期行为
- 对于待办事项选择的工作，待办事项行是 `ready`，没有陈旧链接，不需要缺失的计划
- `docs/context/project-context.md` 中的验证命令是真实命令，不是占位符
- 此文件中的保护区域占位符已替换为真实条目或显式 `none`
- `docs/context/project-context.md` 中的文档新鲜度为 `fresh`，或活动切片已明确验证新鲜的需求、owner doc、codebase-map 路由和触及代码区域
- 任务不触及下面的保护区域
- 未解决的问题明确标记为非阻塞

## AI 必须询问或停止的情况

- 当需求或 owner doc 模糊时更改产品范围
- 在没有 owner doc 和测试策略的情况下更改数据库/模型形状、数据删除、支付、认证、权限、部署或外部集成行为
- 为外部系统发明未在已提交的集成文档或测试中描述的行为
- 因命令缺失、损坏或太慢而跳过所需验证
- 关闭审计、验证、文档或检查清单证据缺失的计划
- 当实时代码和 owner docs 冲突且解决冲突会改变用户可见行为或公共契约时继续执行
- 在没有人工确认或人工批准的 owner-doc 证据的情况下放宽自主标签、保护区域规则或阻塞项
- 当活动切片的文档新鲜度为 `stale`、`unknown` 或 `partially stale` 时继续实施；首先执行基线研究或 plan-first 对齐切片

## 保护区域

为复制的项目填写这些。

如果此表仍然包含占位符，AI 必须将支付、认证/权限、数据删除、数据库/模型形状、部署和外部集成视为 `ask-first` 或 `blocked`，直到表替换为真实条目或显式 `none`。

| 区域 | 规则 | 必需证据 |
| ----------------------------- | ----------- | -------------------------------- |
| `model/*.orm.xml` 模式 | ask first | design doc + plan audit |
| `model/*.api.xml` 契约 | ask first | design doc + plan audit |
| `data deletion` | ask first | owner doc + tests |
| `accounting/finance` postings | plan-first | owner doc + tests |
| `auth/permissions` | plan-first | owner doc + tests |
| `deployment / external integrations` | plan-first | owner doc + tests |

保护区域规则含义：

- `ask first` - 规划或实施前需要人工批准。
- `plan-first` - AI 可以起草计划，但实施需要计划审计加上表中的必需证据。如果审查者可用性为 `none`，实施保持阻塞。
- `research-only` 或 `blocked` - AI 不得更改产品行为。

## 待办事项选择规则

如果用户要求 AI 在未命名任务的情况下继续工作，选择 `docs/backlog/README.md` 中自主权为 `implement` 且阻塞项为 `none` 的最高优先级项目。

在实施所选项目之前，重新检查规划触发条件。`Plan: none` 不免除计划指南。

用户对本地低风险编辑的直接请求不需要待办事项行，但仍必须满足无计划路径和验证规则。

如果没有安全的 `implement` 项目存在，总结最高的阻塞、`plan-first` 或 `ask-first` 项目并请求决策。