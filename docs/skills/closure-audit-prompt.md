# 结束审计提示


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


在独立检查计划切片是否实际完成时使用此提示。

所有创建的计划都需要结束审计。

```text
阅读 `AGENTS.md`、`docs/index.md`、活动需求/设计文档、活动计划、最新相关日志条目和实时更改代码。

审计声称的实现是否真正关闭。

检查 `docs/context/ai-autonomy-policy.md` 审查者可用性。冷重播不是第二位审查者，从不批准保护区域、未解决的产品风险或真相源冲突。

此审计必须由独立子代理或审查者运行，而不是由实施代理继续同一关闭决策。

重点关注：
- 实时行为是否符合规定的需求
- 计划的关闭门控是否实际满足
- 证明是否存在于文件和验证结果中，而不仅仅是聊天中
- 在支持的基线更改的地方是否更新了文档
- 是否有任何剩余差距仍在范围内
- 任务路由和记录的技能使用是否仍然与交付的工作匹配
- 是否在没有持久证据的情况下放宽了任何自主权或待办事项状态
- 是否隐藏了验证失败或未运行的命令

**强制验证范围检查（历史教训：多次因 scoped `-pl` 验证声明 full-green 导致假阳性）：**
- 如果计划声称实现完成，验证必须是 **full reactor `mvn test`**（整仓库全栈测试），而不只是 scoped `-pl` 局部测试。
- scoped `-pl` 验证可以作为「开发中快速反馈」手段，但不能作为「声明完成」的唯一依据。
- 当计划声明的验证范围是 scoped 时，必须在 Closure 部分注明 `⚠️ scoped verification only`，并列出哪些模块未被验证。
- 仅当 full reactor `mvn clean install -DskipTests` + `mvn test` 全部通过时，才能声明 `full-green`。
- 特殊例外：如果 full reactor 已知存在计划范围外的前置基线失败（经 `git stash --include-untracked` 证明是前置失败而非本计划引入），可以 scoped 声明完成但必须在 Closure 部分附上前置失败证据。

按严重性排序，首先返回发现。
如果关闭被阻止，说 `needs revision` 并列出确切缺少的证明或更改。
如果切片可接受，说 `passes closure audit` 并记录任何剩余风险。关闭结果必须在计划的 `## Closure` 部分留下持久证据，可选择性链接到每日日志或存储的审计文件。不要仅基于实施代理的自我记录证据批准关闭。
```