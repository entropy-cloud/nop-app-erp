# 讨论编写指南

## 目的

使用 `docs/discussions/` 捕获模糊工作的多轮澄清。

## 适用场景

- PM 仅可用于部分确认
- 原型含义不清楚
- 存在多种需求解释
- 开发人员否则会直接从原始文件推断领域规则

## 包含内容

- 正在讨论的源文件
- 未解决的问题
- 候选解释
- 已确认的决策
- 阻止实施的未解决项

## 规则

讨论文件用于澄清，而非最终真相。将已解决的结论移入 `docs/requirements/`、`docs/design/` 或 `docs/architecture/`。

## 文件名指南

优先使用日期文件名：

- `docs/discussions/YYYY-MM-DD-HHmm-topic.md`