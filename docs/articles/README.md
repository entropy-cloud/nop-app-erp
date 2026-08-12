# 方法论文章索引

## 目的

`docs/articles/` 存放面向外部的**解释性方法论长文**——讨论 AGE（Attractor-Guided Engineering）工作流、Loop Engineering、Mission Driver 等驱动本项目开发的控制 Loop 设计思想与实证分析。

这些文章解释「为什么这样设计」，面向想从本项目案例学习 Nop Platform 与 AI 辅助开发方法论的读者，不属于项目内部的执行性文档。

## 与 `docs/skills/` 的边界

| 目录 | 性质 | 内容 | 读者动作 |
|------|------|------|----------|
| `docs/articles/` | 解释性方法论长文 | 设计思想、决策逻辑、运行数据分析（叙述性） | 阅读理解「为什么」 |
| `docs/skills/` | 可复用审查/审计方法 | 提示词剧本、审查检查清单、审计执行步骤（操作性） | 复制执行「怎么做」 |

简言之：**articles 解释方法论，skills 复用方法**。两者互补，不互相替代。

## 文章清单

| 文章 | 主题 |
|------|------|
| `mission-driver-key-clarifications.md` | Mission Driver 概念澄清：它解决什么问题，为什么这样设计——概念体系定位（Mission Driver / Loop Engineering / AGE 三层次）、内部设计思想（Plan 关闭契约与中间状态合法性、生成与验收分离、文件化状态、循环嵌套、循环即智能）与能力边界（不定义方向、期望/实际吸引子、压力测试） |
| `loop-engineering-x-attractor.md` | Loop Engineering 实践：Plan Loop（单次变更的关闭契约）与 Mission Driver Loop（自动路线图编排）的工程设计、嵌套解耦与运行数据（以 nop-app-erp 22 天 154 模块全栈开发为实证案例） |
| `mission-driver--loop-engineering.md` | Mission Driver：Loop Engineering 的一种通用参考实现——通用 AI 任务驱动引擎如何通过 loop 嵌套实现局部容错与稳定保障 |

## 维护规则

- 新增文章聚焦方法论与设计分析，不承载项目执行状态（执行状态归 `docs/logs/`、`docs/plans/`）。
- 文章引用的实证数据（模块数/测试数/时间线）应注明快照日期；陈旧后更新或标注历史快照。
- 本清单为目录索引；写作规范见 `docs/references/document-naming-and-timeliness.md`。
