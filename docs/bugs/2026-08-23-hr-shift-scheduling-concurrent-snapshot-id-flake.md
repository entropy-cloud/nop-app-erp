# 2026-08-23 TestErpHrShiftScheduling 并发排班用例快照 id 非确定性 flake（M3.3 seq-default 迁移引入）

## 现象

`module-hr/erp-hr-service` `TestErpHrShiftScheduling.testConcurrentAssignSameEmployeeDayNoDuplicate` 在 `mvn test`（全量）与 `mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrShiftScheduling`（单类）下**概率性失败**（实测 ~40-50%，2 连败 + 3 连胜交替）：

```
AutoTestException[errorCode=nop.err.autotest.output-row-not-exists,
params={file=.../testConcurrentAssignSameEmployeeDayNoDuplicate/output/tables/erp_hr_shift_assignment.csv, id=3, ...},
desc=数据校验失败：数据表[erp_hr_shift_assignment]的id为[3]的记录不存在]
```

JUnit 层断言（count==1 无重复、友好错误码二选一）始终通过——失败仅在自动测试三层比对的 output/tables 快照层：快照记录存活行 `id=3`，失败运行中存活行为其他 id（`id=4`）。

## 根因

id 生成机制在 M3.3（`317b4f795`，id-string-migration「hr 域主键/外键 string 化迁移」）由 H2 自增/确定性分配改为 **`seq-default` 共享序列**：

- 两线程均在 `saveEntity` 时消费共享 `default` 序列（emp=1、shift=2、assignment 两线程各取 3/4）；
- `flush` 的 UK（`UK_HR_SHIFT_ASSIGNMENT_NATURAL`）竞争决定存亡：先落库者存活，后落库者抛 `ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE`；
- **存活行 id 取决于线程调度**（先消费序列者未必先落库），而 `nop.err.autotest.output-row-not-exists` 校验按快照字面 id 取行——快照 `id=3` 仅在存活行恰为 id=3 时通过。

该 flake 自 M3.3 快照重录（重录会话幸运命中 id=3）即潜伏，2026-08-23 B1 全量回归与闭包审计复跑均未触发；本日 VERIFY 复跑两次全量/单类运行均确定性暴露（机器负载下 TOCTOU 路径出现率升高）。

## 影响

- 全量 `mvn test` 概率性红（非 B1 变更引入——hr-service 先于 app-erp-all 运行，且 B1 变更面零 hr 域代码）；
- 快照重录（RECORDING 模式）同样概率性产出 id=3 或 id=4，无法根治。

## 修复

测试层最小修复（零生产代码/ORM/契约变更）：

1. `TestErpHrShiftScheduling` 并发方法捕获存活行实体（`AtomicReference<ErpHrShiftAssignment> survivor` + 成功线程 `compareAndSet`）后 `setVar("ErpHrShiftAssignment@id", survivor.getId())`——`setVar` 覆盖 save 期自动注册的（可能为败者行的）同名 var（`seq-default` 不自动注册 @var，M0.2 实证；`addVar` 会因 `putIfAbsent` 冲突后缀化而不可用）；
2. 快照 `output/tables/erp_hr_shift_assignment.csv` 行 `ID=3` → `ID=@var:ErpHrShiftAssignment@id`（校验按 id 取行，`@var` 解析到实际存活 id；RECORDING 侧 `TagVarCollector` 按前缀 + 值匹配同样回写 `@var`，往返稳定）。

验证：修复后单类 **8/8 连跑全绿**（修复前 2 败 3 胜）+ 全量 `mvn test` BUILD SUCCESS（3811/0/0/1，与 known-good 基线零漂移）。

## 复现

```bash
for i in 1 2 3 4 5 6 7 8; do mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrShiftScheduling 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)"; done
```

修复前：SUCCESS/FAILURE 交替（~40-50% 失败率）；修复后：8/8 SUCCESS。

## 状态

- **fixed**（2026-08-23，VERIFY 步：根因 = M3.3 seq-default 迁移引入的存活行 id 竞态，修复 = survivor id `@var` 引用；8/8 + 全量 3811/0/0/1 复确认）。