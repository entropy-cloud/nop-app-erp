# 文档审计提示


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


在实施前审计需求和设计文档时使用此提示。

```text
阅读 `AGENTS.md`、`docs/index.md`、`docs/process/application-development-workflow.md`、`docs/input/`、`docs/requirements/`、`docs/design/` 和 `docs/architecture/` 下的活动文件。

审计应用层实现任务的当前文档基线。

重点关注：
- 缺失的范围边界
- 伪装成已确定需求的未解决问题
- 原始输入与综合需求不匹配
- 需求与 owner docs 不匹配
- 原型被误认为完整需求源的地方

按严重性排序，首先返回发现。
对于每个发现，包括：
- 标题
- 受影响的文件
- 当前差距
- 实施风险
- 建议

如果没有发现，明确说明并记录剩余风险。
```