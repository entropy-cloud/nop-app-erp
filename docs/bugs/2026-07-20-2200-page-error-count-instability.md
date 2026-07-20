# PAGE_ERROR_COUNT 环境不稳定（H-2）

> Status: active
> Discovered: 2026-07-20（独立审计 H-2）
> Severity: high（验证噪声：基线信号被环境污染）
> Source Audit: `docs/audits/2026-07-20-independent-open-ended-audit.md`
> Plan: `docs/plans/2026-07-20-2200-1-audit-findings-remediation.md`（H-2）
> Cross-ref: `docs/testing/known-good-baselines.md` Known Failures 段；`docs/plans/2026-07-20-1020-3-f4p2-child-table-editor-p2-mfg-assets-projects.md` §Closure Gates 实测段

## 症状

`mvn test -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest` 在**同一仓库状态**下，PAGE_ERROR_COUNT 在 `0` 与 `203`/`213` 间跳变：

- `2026-07-20-0629-1` 实测：`PAGE_ERROR_COUNT=0`（plan 证据行）
- `2026-07-20-1020-3` 实测：`PAGE_ERROR_COUNT=203`（同日同分支，stash 改动后基线同为 203）

错误堆栈固定为：

```
java.lang.ClassCastException: class org.antlr.v4.runtime.misc.ParseCancellationException
  cannot be cast to class org.antlr.v4.runtime.RecognitionException
  at io.nop.xui.builder.XuiPageBuilder.parsePage(...)  // ANTLR parser 全局故障
```

非 view.xml 真实回归——若回归则 count 应单调上升，而非 0↔203 跳变。

## 根因

JDK 26（zulu-26）下 ANTLR 4 runtime 与平台 parser 字节码不兼容，触发 `ParseCancellationException → RecognitionException` 的强转失败，连带所有 `getPage()` 调用失败。

环境依赖：

- 本机 JDK：zulu-26（`JAVA_HOME` 默认）
- 平台 ANTLR 版本：见 `nop-entropy/pom.xml` 的 `antlr.version` 属性

## 影响范围

- `ErpAllWebPagesCollectTest` 在 CI/本地随机失败 → 全 reactor `mvn test` 不稳定
- 不能作为 view.xml 回归的可靠信号源
- 后续计划无法在 Closure Gates 中用此测试做置信回退

## 处置决策（H-2）

按 plan `2026-07-20-2200-1` H-2 Decision 默认决策树执行**方案 B + C**：

- **方案 C**（始终执行，本文件即其产物）：在 `docs/bugs/` 与 `docs/testing/known-good-baselines.md` Known Failures 段记录已知限制
- **方案 B**（运行时缓解）：`ErpAllWebPagesCollectTest` 加 `@Disabled` 注解，附明确重新启用条件
- **方案 A**（不执行）：`pom.xml` 修复 antlr 版本兼容性——**需要 ask-first**，本 autonomous 运行无人工批准，留待后续

## 重新启用条件（满足任一即可移除 `@Disabled`）

1. **方案 A 落地**：`pom.xml` 调整 `antlr.version` 使其与 JDK 26 兼容（需人工批准与平台团队协调）
2. **JDK 切换**：本地 `JAVA_HOME` 从 zulu-26 切换为 zulu-21 / temurin-21 等兼容版本
3. **平台修复**：nop-entropy 修复 ANTLR parser 在 JDK 26 下的兼容性（`nop-entropy/ai-dev/logs/` 跟踪）

## 重新启用验证步骤

```bash
# 1. 在 PR 中移除 @Disabled
# 2. 本地运行（在目标 JDK 下）
mvn test -pl app-erp-all -Dtest=ErpAllWebPagesCollectTest
# 3. 确认输出 "=== PAGE_ERROR_COUNT: 0 ===" 且 BUILD SUCCESS
# 4. 在 docs/testing/known-good-baselines.md 新基线行 Notes 中注明 "PAGE_ERROR_COUNT=0 稳定（H-2 修复）"
# 5. 删除 @Disabled，将本 bug 笔记 Status 改为 resolved
```

## 防御性措施（已落地）

- 本 bug 笔记作为活性错误，与 `known-good-baselines.md` 交叉引用（L-1 落地后纳入活跃错误列）
- `@Disabled` 注解文本嵌入 plan ID 与本文件路径，便于后续 grep 定位
