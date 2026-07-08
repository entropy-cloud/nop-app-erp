# 03 12 个域 60+ 处 `LocalDateTime.now()` 导致测试时间不可控

> 来源审计：`docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`（C-5 测试不可控性）
> 关联计划：`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` H-2

> 状态（2026-07-08 更新，✅ 已全部修复）：生产代码 `LocalDateTime.now()` 已由 1915-1 H-2 全部修复（实时 grep 0 命中）；测试代码 32 处已由 `docs/plans/2026-07-08-0517-1-test-code-localdatetime-now-cleanup.md` 全部修复（实时 grep 0 命中）。`LocalDate.now()` 残留（生产 14 文件/28 处 + 测试 23 文件/106 处，修正原「生产 4 + 测试 ~96」低估）已由 `docs/plans/2026-07-08-0637-2-localdate-now-cleanup.md` 全部修复（实时 grep `LocalDate\.now\(\)` / `LocalDateTime\.now\(\)` 跨 `**/src/**/*.java` 均 0 命中）。本 bug 文档请求的 `docs/context/conventions.md`「时间 API 使用约定」已增补（含 LocalDate.now() + LocalDateTime.now() 双重禁令，残留附注已移除）。本 bug 闭环。

## 问题

- 12 个业务域的 BizModel / Processor / Executor 中直接调用 `java.time.LocalDateTime.now()`（或 `Clock.systemDefaultZone()`）获取当前时间，未通过平台统一 `CoreClock` 或注入式 `ITimeService`
- 估计 60+ 处直接调用，散布在 `module-{purchase,sales,inventory,finance,assets,projects,manufacturing,quality,maintenance,cs,hr,logistics}/erp-*-service/.../`
- 影响：单元测试无法控制时间，断言 `postedAt` / `createdAt` / `businessDate` 等字段时出现"测试时间敏感"的脆弱性；严重性：中（测试覆盖与可重现性）

## 复现

- 环境：codegen 后的 12 个业务域 `*-service` 模块（BizModel 骨架 + Processor 模板）
- 触发：在 BizModel 中调用 `entity.setPostedAt(LocalDateTime.now())`，随后在单测中断言该字段等于固定时刻会失败
- 最小复现脚本：暂无（依赖 codegen 后的具体调用点）

## 诊断方法

- 诊断难度：直接（grep 即可定位）
- 调查路径：审计 §3.5 测试可控性扫描 → grep `LocalDateTime.now()` 跨 `module-*/erp-*-service/` → 命中 60+ 处
- 决定性证据：`grep -rn "LocalDateTime.now()" module-*/erp-*-service/src/main/java/ | wc -l` 输出 ≥ 60

## 根本原因

- 平台能力未对齐：nop-entropy 提供 `CoreMetrics.currentTimeMillis()`（见 `ai-defaults.md`），但代码生成器模板未将"获取当前时间"替换为平台统一 API
- 缺少时间服务抽象：项目缺少 `ITimeService` 之类的时间提供者接口（或未使用 `CoreClock`），导致生成代码直接调 JDK 静态方法

## 修复

- 此为系统性 bug，修复需在 codegen 模板层或全局替换层完成（不在本整改计划范围内）
- 待落地动作：
  - 选定平台时间 API：使用 `CoreClock.currentTimeMillis()` 转 `LocalDateTime`，或新建 `ITimeService`（`Clock` 抽象）注入到 BizModel
  - codegen 模板修正：在 `nop-cli` 的 BizModel 模板中替换 `LocalDateTime.now()` → `timeService.now()`
  - 全局 grep 替换：codegen 后跑一次全局重构脚本，将已生成代码中的 `LocalDateTime.now()` 替换为平台 API
  - 单测基类提供 `TestClock`（固定时刻 + 时区），由 `@NopTestConfig` 注入

## 测试

- 暂无自动化测试覆盖（修复未落地）
- 待落地后补充：单测基类注入固定 Clock 后断言时间字段的回归测试

## 受影响的工件

- 12 个域 `*-service` 模块的 BizModel / Processor / Executor / Dispatcher 类（codegen 产物）
- `nop-entropy` 的 codegen 模板（如 `service-template/*.xpt`）— 待源仓库更新
- `docs/context/conventions.md` — 时间 API 使用约定（需增补"禁止直接 `LocalDateTime.now()`"规则）

## 未来重构注意事项

- 时间获取抽象：禁止在业务代码中直接调 `LocalDateTime.now()` / `System.currentTimeMillis()`，必须经平台 API 或注入的服务
- 测试 Clock 注入：所有涉及时间的单测必须能注入固定 Clock，否则断言时间字段的测试一律视为脆弱测试

## 预防差距

- codegen 模板审查未覆盖"硬编码时间获取"反模式
- `AGENTS.md` 的"Nop Platform 特定规则"段落未明确禁止 `LocalDateTime.now()`（建议增补到平台规则段落）
