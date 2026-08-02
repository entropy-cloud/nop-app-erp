# 04 finance 过账期间解析依赖系统时钟，月初滚动致测试套件红（伪装为最近变更回归）

> 来源：VERIFY 验证 `docs/plans/2026-07-31-2115-1-r6-1-finance-d-mutation-per-mutation-split.md` 时发现
> 关联：`docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md`（同族「测试时间不可控」类问题，彼条聚焦生产代码 `LocalDate.now()`，本条聚焦**测试侧未冻结 `CoreMetrics` 时钟**）
> 状态：**open / 需独立计划修复**（非 R6.1 引入，不阻塞 R6.1 交付——R6.1 已证行为等价零回归）

## 问题

- 每月月初（系统时钟跨月，例如 2026-07-31 → 2026-08-01），finance 域 4 个测试类的部分用例由绿转红，合计 **1 failure + 10 errors**：
  - `TestErpFinBadDebtReversal`（3 errors）— `erp_fin_accounting_period` 断言 `MONTH=8/START_DATE=2026-08-01/END_DATE=2026-08-31`，期望 7 月
  - `TestErpFinEmployeeAdvanceCashRepayReversal`（3 errors）— 同上期间错配（`NAME=2026-08` 等）
  - `TestErpFinNotesPayableStateMachine`（4 errors）— `erp_fin_notes_payable` 断言 `POSTED=false` 期望 `true`、`VERSION` 偏低
  - `TestErpFinDashboard.testTrendMonthlySeries`（1 failure）— `趋势包含 6 月` 期望 `true` 实际 `false`（趋势窗口相对「今天」滚动）
- 影响范围：仅 finance 域 `*-service` 测试套件；月初当天 `mvn test -pl module-finance/erp-fin-service -am` 红，月内其余时段绿。严重性：中（CI 红嗓音 + 容易被误判为最近代码变更回归）。

## 复现

- 环境：任何 finance `*-service` 测试运行（`mvn test -pl module-finance/erp-fin-service -am`），在系统日期 = 某月 1 日（且测试种子/快照的「当前期间」为上月）时。
- 触发：系统时钟跨入新月度首日。`NotesPostingDispatcher:79/108` 取 `voucherDate = note.getIssueDate() != null ? issueDate : CoreMetrics.today()`；`ErpFinPostingProcessor:495 resolveOpenPeriod(voucherDate)` 要求一个包含该日期的 OPEN 期间。`TestErpFinNotesPayableStateMachine` 仅 seed 7 月期间且不设 `issueDate` → `voucherDate = 今天` = 8/1 → 落在 seed 期间外 → 过账失败（无 OPEN 期间/期间未开）→ `POSTED=false`、`VERSION` 低于快照期望。
- 最小复现：在 8/1 运行 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinNotesPayableStateMachine`。

## 诊断方法

- 诊断难度：非平凡——失败**形似 R6.1 回归**（`POSTED=false` 正是已知失败模式「过账吞异常致 posted 悬挂」），极易误判为 R6.1 Processor 拆分破坏了过账。
- 关键判别实验（决定性证据）：将工作树 R6.1 变更全部 `git stash -u`（回到 HEAD，即 R6.1 之前的 finance 代码），运行同一组 4 个失败测试类 → **结果完全一致：`Tests run: 17, Failures: 1, Errors: 10`**，且逐条 error message 字段级相同（`MONTH=8 expected=7`、`POSTED=false expected=true VERSION=2 expected=4` 等）。
  - 结论：失败与 R6.1 **完全无关**，R6.1 行为等价（零回归）。真正变量是「系统日期从 7/31 → 8/1」。
- 被拒绝的假设：(a) R6.1 NotesPayable Processor 拆分引入过账缺陷——被隔离实验证伪；(b) `TestErpFinNotesPayableStateMachine` 查询「当前期间」——该测试为纯 Java 单测，自身只 seed 固定 7 月期间，不按日期查期间，期间错配来自过账链路用 `CoreMetrics.today()`。
- 旁证：`docs/testing/known-good-baselines.md` 最近一次全量 `mvn test` 绿基线为 2026-07-31（R2.8/R2.9，0 failures/0 errors），与「月初滚动致红」时间点吻合。

## 根本原因

- finance 过账链路用平台 API `CoreMetrics.today()`（`NotesPostingDispatcher` / 过账期间解析）确定凭证日期对应的「当前会计期间」——生产代码**已**用平台 API（合规），但 `CoreMetrics` 默认时钟即系统时钟（`CoreMetrics.java:21-40` 默认 `IClock` 包装 `System.currentTimeMillis()`/`LocalDate.now()`）。
- finance 测试侧**未注册固定 `CoreMetrics` 测试时钟**（`CoreMetrics.registerClock`，`CoreMetrics.java:44` 可设静态时钟），导致期间解析随墙钟漂移；种子/快照按「录制日所在月」固化，月初滚动即失配。
- 与 `2026-07-07-1915` 同族但层次不同：前者是生产代码直接 `LocalDate.now()`（已修），本条是生产代码已合规用 `CoreMetrics` 但**测试未冻结时钟**。

## 修复（待落地，需独立计划）

- 方向：为 finance 日期敏感测试注册固定 `CoreMetrics` 测试时钟（如固定到录制基准日 2026-07-15），使期间解析与快照/种子确定性对齐。`@BeforeEach` 注册、`@AfterEach` 恢复 `CoreMetrics.defaultClock()`。
- 备选/补充：纯 Java 单测（如 `TestErpFinNotesPayableStateMachine`）改为在 seed note 上显式 `setIssueDate(固定7月日)`，使凭证日期不依赖「今天」；快照需相应重录。
- 不应采用的「修复」：按新月度重录快照——只是把时间炸弹推到下个月 1 号，且改变快照语义。
- 范围提示：触及会计期间保护区域 + 测试基础设施（全局静态时钟 + 潜在并行测试交互），按 `AGENTS.md` 规划规则需独立计划 + 独立审计。

## 测试

- 暂无新增自动化覆盖（修复未落地）。
- 待落地后：验证「固定时钟下 4 个测试类跨月度滚动仍绿」的回归测试；并确认全局 `registerClock` 不污染并行同 JVM 其他测试。

## 受影响的工件

- `module-finance/erp-fin-service/.../posting/NotesPostingDispatcher.java:79,108` — `voucherDate = issueDate ?: CoreMetrics.today()`
- `module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java:495` — `resolveOpenPeriod(voucherDate)` 要求 OPEN 期间
- `module-finance/erp-fin-service/.../dashboard`（`TestErpFinDashboard.testTrendMonthlySeries`）— 趋势窗口相对「今天」
- `nop-entropy/nop-kernel/nop-api-core/src/main/java/io/nop/api/core/time/CoreMetrics.java:44` — `registerClock(IClock)`（修复入口）
- 失败测试类：`TestErpFinBadDebtReversal` / `TestErpFinEmployeeAdvanceCashRepayReversal` / `TestErpFinNotesPayableStateMachine` / `TestErpFinDashboard`

## 未来重构注意事项

- 任何「按日期/期间解析」的业务路径，其测试必须冻结 `CoreMetrics` 时钟；否则该测试即时间炸弹（每月初爆）。
- 看到 finance 域 `POSTED=false` / 期间 `MONTH` 错配的测试红，**优先怀疑月初时钟滚动**，而非最近变更回归；用「stash 最近变更 → 同样红」的隔离实验快速证伪。
- 若未来引入并行测试执行，全局 `CoreMetrics.registerClock` 的静态副作用需改为线程级/上下文级时钟，避免跨用例污染。

## 预防差距

- finance 测试基础设施缺「固定测试时钟」基类/约定（`2026-07-07-1915` 修复生产侧时未同步覆盖测试侧 `CoreMetrics` 冻结）。
- `mvn test` 无「月初时钟滚动」维度的 CI 矩阵，时间炸弹只在自然月初暴露。
