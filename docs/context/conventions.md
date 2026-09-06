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

## i18n 与日志语言

- LOG 消息统一英文，不走 i18n。
- 异常路径参数传状态码/枚举名/字典值本身，不传中文散文（错误消息语义由 `ErrorCode.define` 中文模板承载）。
- `*Errors.java` 接口必须标注 `@Locale("zh-CN")`。
- `*.page.yaml`/`*.flux.yaml` 用户可见文案必须补 `i18nEn`（或模型源 `i18n-en` 属性）。
- 运行时字符串中文白名单制，豁免唯一通道 = 显式登记；`_` 前缀生成 i18n yaml 禁手改。
- 判定冲突以 `docs/architecture/i18n-compliance.md` 为唯一权威（判定准绳表、白名单登记格式、修复模式对照表）。

## 验证规则

- 保持 `docs/context/project-context.md` 中的验证命令最新。
- 不要报告未实际运行的命令的验证成功。
- 复制模板后不要保留占位符验证命令。
- **长输出命令（mvn test、mvn install、Playwright run、E2E 服务端日志等）必须先全量落盘再做摘要**。禁止 `cmd 2>&1 | tail -N > file` 这种写法——`tail` 在 `>` 重定向前执行，写入文件的只有尾部 N 行，前面几万行 BUILD 输出（失败用例 stack trace、Surefire 报告链接、模块级错误）全部丢光，恰好查不到失败细节。落盘路径用**项目根 `_tmp/`**（仓库统一临时区，`.gitignore:26 _tmp/` + 08-27 `/**/_tmp/` 模块内兜底；模块内 `_tmp/` 非合法位置；不要用全局 `/tmp/`，与项目先例 `_tmp/e2e-server.log`/`_tmp/v1-surefire-evidence/`/`_tmp/flux-page-validation-report.json`/`_tmp/<plan-id>-surefire-evidence/` 等一致）。正确模式：先 `cmd > _tmp/<purpose>.log 2>&1; echo "EXIT=$?"` 落盘，再 `grep -E "Tests run:|BUILD|ERROR|FAILURE" _tmp/<purpose>.log | tail -20` 出摘要；或 `cmd 2>&1 | tee _tmp/<purpose>.log | grep ... | tail -20` 同时保留全量与屏幕摘要。命名建议按用途/计划/里程碑（如 `_tmp/<plan-id>-<purpose>.log` 或 `_tmp/<purpose>-<milestone>.log`），便于一键追溯。失败排查从全量日志文件出发（`rg "FAIL|ERROR|Tests run.*Failures: [^0]" _tmp/<purpose>.log` 或 `less _tmp/<purpose>.log`），不要重跑命令后只取 tail；重要日志路径应写回当日 `docs/logs/{year}/{month}-{day}.md` 或 `docs/bugs/<id>.md` 证据节便于复盘。

## 时间 API 使用约定

- 生产代码与测试代码**禁止**直接调用 `java.time.LocalDateTime.now()` / `java.time.LocalDate.now()` / `java.time.Clock.systemDefaultZone()`。
- 统一经平台时间 API：`io.nop.api.core.time.CoreMetrics.currentDateTime()`（`LocalDateTime`）、`CoreMetrics.currentDate()`（`LocalDate`）、`CoreMetrics.currentTimeMillis()`（`long`）。权威参考：`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`。
- 理由：测试与生产共享同一 `CoreClock` 时间源，保证语义一致；并为未来 `CoreClock` mock 可注入（固定时刻断言）留出统一收口点。
- 历史清理状态与残留登记以对应清理计划与 `docs/testing/known-good-baselines.md` 为准（清理计划见 `docs/plans/2026-07-08-0637-2-localdate-now-cleanup.md`、`docs/plans/2026-07-08-0517-1-test-code-localdatetime-now-cleanup.md`）；新代码一律不得引入直接调用，核查以 `rg "LocalDate\.now\(\)|LocalDateTime\.now\(\)"` 为准。