# 计划审计提示


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


在实施前独立审查执行计划时使用此提示。

所有创建的计划都需要此审查。

```text
阅读 `AGENTS.md`、`docs/index.md`、`docs/process/application-development-workflow.md`、活动需求/设计文档，以及 `docs/plans/` 下的活动文件。

将计划作为执行契约进行审计。

检查 `docs/context/ai-autonomy-policy.md` 审查者可用性。冷重播不是第二位审查者，从不批准保护区域、未解决的产品风险或真相源冲突。

重点关注：
- 当前基线是否诚实
- 目标和非目标是否清晰
- 关闭门控是否真实
- 是否存在隐藏依赖或未解决的需求差距
- 是否有范围内的缺陷或契约差距被悄悄降级
- 任务路由和记录的技能使用是否诚实、必要且与 owner docs 匹配
- AI 是否在没有人工确认或人工批准的 owner-doc 证据的情况下放宽了待办事项或上下文
- 在实施前是否对过时文档或遗留模式冲突进行了分类
- 证明和验证是否覆盖每个验收标准

按严重性排序，首先返回发现。
除非改变风险评估，否则不要表扬计划。

如果发现阻塞问题，说 `needs revision` 并列出要更改的确切文件/部分。
如果没有阻塞问题，说 `passes draft review` 并列出剩余风险（如有）。
```