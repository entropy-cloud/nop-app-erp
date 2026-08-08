# hr-shift-rotation E2E regenerate 断言与后端逻辑删除漂移（pre-existing，非本切片引入）

## 问题

- `hr-shift-rotation.action.spec.ts:174`「regenerate=true cancels old SCHEDULED rows and rebuilds same count」失败：`cancelled.length` 期望 > 0，实测 0（`findItems` 查 `status=CANCELLED` 零命中）。
- 影响：hr E2E 套件 35 passed / 1 failed（2026-08-08 RC-R1.5 验证时实测）；该 spec 自 2026-07-30 起为红色（spec/backend 漂移），非 RC-R1.5 代码引入（RC-R1.5 仅触 attendance clockIn 路径）。

## 复现

- 环境：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/hr-shift-rotation.action.spec.ts`
- 前置：app runner 含当前 HEAD 后端代码（`ErpHrShiftRotationPatternGenerateRotationProcessor`）。
- 触发：首次 `generateRotation`（6 行 SCHEDULED）→ `regenerate=true` 重生成（返回 6 行 OK）→ 反查 `status=CANCELLED` 旧行 → 0 行。

## 诊断方法

- 直接：失败断言是 spec `:229` 的 CANCELLED 计数；先核查后端 `generateRotation` 行为——`ErpHrShiftRotationPatternGenerateRotationProcessor.deleteExistingAssignments:183-199` 实际执行 `dao.deleteEntity(a)`（逻辑删除 delVersion++），**不置 status=CANCELLED**，代码注释自陈「仅状态置 CANCELLED 不变 delVersion 会触发 duplicate-key」（UK_HR_SHIFT_ASSIGNMENT_NATURAL 兼容）。
- 时间线定位：spec 自 07-18 未改（git log 仅 1 提交 0713a6a7f）；后端由 `a0b941a3f`（2026-07-30 R1.28「hr regenerate UK 兼容补救」）从置 CANCELLED 改为逻辑删除；`2c4cb8b95`（08-01 R6.7 Processor 拆分）保留该行为。
- 排除假设：非 RC-R1.5 变更所致（变更文件 = ClockInProcessor + IBiz javadoc + TestErpHrAttendanceEngine + attendance E2E 段，与 rotation 完全不相交）；非 app 重启/DB 状态问题（spec 自包含建数 + playwright webServer 每次 fresh DB，本复现亦 fresh DB）。

## 根本原因

- R1.28 UK 兼容修复（`a0b941a3f`）将 regenerate 的「CANCEL 旧行」语义实现为**逻辑删除**（delVersion++，行保留但 delVersion>0，查询默认过滤不可见），而 E2E spec（07-18 时代契约「CANCEL 旧 6 SCHEDULED」）仍断言 `status=CANCELLED` 可见行——契约漂移未同步 spec。
- 语义影响：对业务可见效果（旧行不再 SCHEDULED、同键可重排）等价，但「CANCELLED 状态行」不再是 regenerate 的产物。

## 修复

- 待独立修复（不在 RC-R1.5 范围）：二选一——(a) spec 断言改为「旧行不再 SCHEDULED」（查同范围 SCHEDULED 数 = 重建数 + delVersion>0 不可见即可）或补查逻辑删除语义；(b) 后端改回显式 CANCELLED 状态 + 处理 UK 冲突（回归 R1.28 的 duplicate-key 问题，不推荐）。
- 修复归属：非 P1/P2 finding 驱动；建议随 hr 域后续 MR1 批次（如 RC-R1.6/RC-R1.7 同域计划）顺带对齐，或在 e2e-runbook 已知失败段登记。

## 测试

- 无自动化回归覆盖变更（本笔记仅登记漂移事实）。复现证据 = 上述 playwright 命令两次独立运行均复现（2026-08-08 07:2x/07:3x）。
- 级别：e2e（spec 断言与后端契约对齐验证）。

## 受影响的工件

- `tests/e2e/business-actions/hr-shift-rotation.action.spec.ts:207-229` - regenerate 后 CANCELLED 反查断言（陈旧契约）
- `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/processor/ErpHrShiftRotationPatternGenerateRotationProcessor.java:183-199` - `deleteExistingAssignments` 逻辑删除（当前行为）
- `module-hr/erp-hr-service/_cases/.../erp_hr_shift_assignment.csv` - R1.28 快照（逻辑删除行 delVersion>0）

## 未来重构注意事项

- 若有人把 regenerate 的「CANCEL」改回显式 `setStatus(CANCELLED)`：必须先处理 `UK_HR_SHIFT_ASSIGNMENT_NATURAL`（employeeId,assignmentDate,shiftId,delVersion）同键重排 duplicate-key——R1.28 正是因此弃 CANCELLED 改逻辑删除。
- 若有人重写 `deleteExistingAssignments`：保持「删除行 delVersion>0、重建行 delVersion=0」的 UK 兼容不变量；同时评估 E2E spec `:229` 断言是否需要随语义同步。

## 预防差距（可选）

- R1.28 行为变更（reject→逻辑删除）未同步既有 E2E spec 断言——行为变更计划应包含 E2E 断言同步检查项。
- hr E2E 全套件未纳入统一回归门控（本次为 RC-R1.5 验证 hr-* 套件时发现）。
