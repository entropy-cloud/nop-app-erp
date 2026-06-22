# 待办事项

## 目的

使用此文件列出 AI 可以检查或执行的候选工作。

待办事项不是需求、owner docs 或计划的替代品。它仅有助于选择下一个切片。

## 工作项

| 优先级 | 项目 | 需求 | Owner Doc | 计划 | 状态 | AI 自主权 | 阻塞项 | 最后检查 |
| -------- | ----------------------------- | -------------------------------------------------- | ------------------------------- | -------------- | ------------------- | ----------- | ---------------------------------------------- | ------------ |
| P0 | ERP 业务域选择 | `docs/requirements/product-scope.md` | `docs/architecture/project-vision.md` | `none` | `needs-requirement` | `ask-first` | `human must decide ERP domains in scope` | 2026-06-22 |
| P1 | ORM 模型第一批实体 | `docs/requirements/product-scope.md` | `docs/design/app-overview.md` | `none` | `blocked` | `plan-first` | `blocked on ERP domain selection (P0)` | 2026-06-22 |
| P2 | 多模块代码生成 | `docs/requirements/product-scope.md` | `docs/architecture/system-baseline.md` | `none` | `blocked` | `plan-first` | `blocked on ORM entity design (P1)` | 2026-06-22 |

## 就绪不变量

`ready` 意味着以下所有条件都为真：

- 需求路径存在并具有可测试的验收标准
- owner doc 路径存在且对此切片已知不过时
- `docs/context/project-context.md` 中的验证命令是真实的
- 无阻塞性未解决问题或明确标记为非阻塞
- 保护区域在 `docs/context/ai-autonomy-policy.md` 中配置
- 已检查规划触发条件

`Plan: none` 仅在项目明确符合 `docs/plans/00-plan-authoring-and-execution-guide.md` 中的无计划路径时有效。如果需要计划，请将 AI 自主权设置为 `plan-first`，直到计划审计通过。

代理可以凭证据将过时的行从 `ready` 降级为 `needs-*` 或 `blocked`。代理不得在无人确认或无人批准的 owner-doc 证据的情况下将行升级为 `ready`、将自主权更改为 `implement` 或清除阻塞项。

设置后的就绪行示例：

```md
| P0 | User Management first slice | `docs/requirements/2026-05-21-0900-user-management.md` | `docs/design/app-overview.md` | `docs/plans/2026-05-21-1000-user-management-plan.md` | `ready` | `plan-first` | `none` | `2026-05-21` |
```

## 状态值

- `idea` - 未准备好实施
- `needs-requirement` - 存在原始输入但不存在实现就绪需求
- `needs-design` - 需求存在但 owner doc 缺失或过时
- `ready` - AI 可以根据自主权标签继续
- `in-progress` - 当前正在实施或计划中
- `blocked` - 在阻塞项解决之前无法继续
- `done` - 已完成并验证

## AI 自主权值

使用 `docs/context/ai-autonomy-policy.md` 中的值：

- `implement`
- `plan-first`
- `ask-first`
- `research-only`
- `blocked`

## 选择规则

当被要求在未命名任务的情况下继续时，选择优先级最高的 `ready` 项目，其 `AI Autonomy` 为 `implement` 且 `Blocker` 为 `none`。

实施前，确认链接的需求、owner doc、计划字段、自主权政策和规划触发条件仍然有效。不要仅从聊天推断就绪状态。

如果表过时，请降级行或在实施前询问。