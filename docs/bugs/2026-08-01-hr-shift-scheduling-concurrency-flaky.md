# 05 hr 排班并发测试 testConcurrentAssignSameEmployeeDayNoDuplicate 时序敏感偶发红（两友好错误码路径之一未断言）

> 来源：VERIFY 验证 `docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（纯文档，零代码）时发现
> 关联：`docs/plans/2026-07-30-0841-2` R1.28 P1-MA2-091（引入 UK 兜底 + flush 翻译友好错误码的本测试）
> 状态：**open / 需独立计划修复**（非 Q4 引入，不阻塞 Q4 交付——Q4 为纯设计文档；HR 模块自 `2c4cb8b95` R6.7 起未变，Q1 全量验证同会话刚绿）

## 问题

- `app.erp.hr.service.TestErpHrShiftScheduling.testConcurrentAssignSameEmployeeDayNoDuplicate` 偶发失败，报错：
  `expected: <erp.err.hr.shift-assignment-duplicate> but was: <erp.err.hr.shift-duplicate-assignment>`。
- 影响范围：仅该单个并发测试用例；HR 域其余测试全绿。严重性：低-中（CI 噪音 + 易被误判为最近变更回归；数据完整性不变量始终成立，无真实数据缺陷）。

## 复现

- 环境：`mvn test`（全量 reactor）或 `mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrShiftScheduling`。
- 触发：**时序依赖**——2 线程经 `CountDownLatch` 同时为同员工同日同班次排班。调度器恰好让线程 1 完成完整事务（INSERT + commit）在线程 2 进入**前置检查**之前时，线程 2 命中前置检查路径而非 UK 碰撞路径，测试即红。无法确定性复现（依赖 OS 线程调度）。
- 旁证：同一 HEAD 下，本次全量 `mvn test` 红（该用例），但隔离重跑 `TestErpHrShiftScheduling` 立即绿（`Tests run: 14, Failures: 0`）；同会话 Q1 全量验证（同一代码态）该用例绿。

## 诊断方法

- 诊断难度：非平凡——失败**形似最近 HR 变更回归**，但本批次为零代码文档变更，markdown 不可能影响 Java 线程调度。
- 关键判别证据：(a) `git diff` 确认本批次仅 `.md` 文件，零 `.java`/`.xml`；(b) HR 模块自 `2c4cb8b95`（R6.7）起未变，Q1 验证（同会话、同代码态）该用例绿；(c) 隔离重跑该测试类立即 14/14 绿。三者共同证伪「代码回归」假设。
- 被拒绝的假设：(a) Q4 设计文档引入回归——被「零代码」证伪；(b) HR 排班逻辑缺陷——被「数据完整性断言（line 145 仅 1 条 active）始终通过 + 隔离重跑绿」证伪。

## 根本原因

- 生产侧 `ErpHrShiftAssignmentBizModel` 对「同员工同日重复排班」有**两条**友好错误码路径：
  1. **前置检查** `assertNoExistingAssignment:149` — 查询发现已存在 active 排班 → 抛 `ERR_SHIFT_DUPLICATE_ASSIGNMENT`。
  2. **UK 兜底** `doCreateAssignment:128` — `saveEntity` 后显式 `flushSession`，命中 `UK_HR_SHIFT_ASSIGNMENT_NATURAL` → 经 `UniqueConstraintHelper` 翻译为 `ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE`。
- 测试 `TestErpHrShiftScheduling:150` 的并发断言**只接受路径 2 的错误码**（`ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE`），未断言路径 1。当线程调度为顺序（线程 1 先提交，线程 2 进前置检查）时，失败方抛路径 1 的 `ERR_SHIFT_DUPLICATE_ASSIGNMENT`，断言失败。
- 测试注释（line 146「若有线程抛错，应为友好错误码（非 ERR_ORM_DATA_EXCEPTION）」）的真实意图是验证错误被翻译为友好码，而非限定具体哪一条；`assertEquals` 单码过于狭窄。

## 修复（待落地，需独立计划）

- 方向：放宽 `TestErpHrShiftScheduling:146-154` 的断言——任一友好错误码（`ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE` 或 `ERR_SHIFT_DUPLICATE_ASSIGNMENT`）均视为通过；两者都正确阻止重复且都是翻译后的友好码（非原始 `ERR_ORM_DATA_EXCEPTION`）。数据完整性断言（line 145 仅 1 条 active）是真正的不变量，已始终成立。
- 备选：若要确定性验证 UK 碰撞路径（路径 2），须在两条路径之间插入同步栅栏强制真并发——复杂度高、本身又引入测试脆弱性，不推荐。
- 范围提示：仅测试侧单文件改动，低风险，可纳入最近一个 HR/测试健壮性批次，无需独立重审计。

## 测试

- 暂无新增自动化覆盖（修复未落地）。
- 待落地后：连续重跑 `mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrShiftScheduling` 多次（如 10 次）确认零红，验证时序鲁棒性。

## 受影响的工件

- `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrShiftScheduling.java:104-155` — 并发测试，断言过窄（修复入口）
- `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/ErpHrShiftAssignmentBizModel.java:128,149` — 两条友好错误码路径（行为正确，无需改）

## 未来重构注意事项

- 并发测试断言「友好错误码」时，应断言「属于友好码集合」而非「等于某一具体码」——只要存在多条合法翻译路径，单码 `assertEquals` 必然时序脆弱。
- 看到 HR `shift-duplicate-assignment` vs `shift-assignment-duplicate` 错误码漂移的测试红，**优先怀疑并发时序**，而非最近变更回归；用「隔离重跑该测试类」快速证伪。
- 该测试的数据完整性不变量（UK 保证仅 1 条 active 排班）始终成立——偶发红仅是错误码断言面过窄，不代表真实数据缺陷。
