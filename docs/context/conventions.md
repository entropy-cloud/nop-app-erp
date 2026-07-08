# 项目约定

## 目的

本文件记录 AI 代理应默认应用的项目范围规则。

保持简短。如果规则变得详细成为查找材料，将细节移至 `docs/references/` 并在此处链接。

## 文件输入/输出

- 重要输入应在实现前写入文件。
- 重要输出应写回仓库，而不仅仅留在聊天中。
- 原始输入属于 `docs/input/`。
- 综合的实现就绪需求属于 `docs/requirements/`。

## 设计拆分

- 需求/应用行为设计属于 `docs/requirements/` 和 `docs/design/`。
- 技术架构设计属于 `docs/architecture/`。
- 交叉引用而非在多个文档中重复相同规则。

## 审查规则

- 高风险或高度模糊的需求和设计草案应获得独立子代理或审查者通过。
- 每个创建的计划在实施前需要独立草案审查，在完成前需要结束审计。
- 自我审查或自行记录的结束证据不能用于标记创建的计划完成。
- 独立审查应引用文件和证据，而不仅仅说"看起来不错"。
- 如果没有独立审查者可用，在计划或日志中记录该限制。冷重播不是第二位审查者，本身永远无法解决保护区域或真相源冲突。

## Bug 规则

- 每个非平凡 Bug 修复应添加或更新自动化测试覆盖。
- 如果自动化覆盖不可能，记录原因和手动证明。

## 注释策略

- 默认优先不添加注释。
- 仅当本地约束容易被误读且代码本身不足以表达时才添加注释。

## 验证规则

- 保持 `docs/context/project-context.md` 中的验证命令最新。
- 不要报告未实际运行的命令的验证成功。
- 复制模板后不要保留占位符验证命令。

## 时间 API 使用约定

- 生产代码与测试代码**禁止**直接调用 `java.time.LocalDateTime.now()` / `java.time.LocalDate.now()` / `java.time.Clock.systemDefaultZone()`。
- 统一经平台时间 API：`io.nop.api.core.time.CoreMetrics.currentDateTime()`（`LocalDateTime`）、`CoreMetrics.currentDate()`（`LocalDate`）、`CoreMetrics.currentTimeMillis()`（`long`）。权威参考：`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`。
- 理由：测试与生产共享同一 `CoreClock` 时间源，保证语义一致；并为未来 `CoreClock` mock 可注入（固定时刻断言）留出统一收口点。
- 截至 2026-07-08，生产与测试代码中 `LocalDate.now()` 与 `LocalDateTime.now()` 均已归零（生产 LocalDate 14 文件/28 处 + 测试 23 文件/106 处见 `docs/plans/2026-07-08-0637-2-localdate-now-cleanup.md`；LocalDateTime 见 `docs/plans/2026-07-08-0517-1-test-code-localdatetime-now-cleanup.md`）。