# rc-ma1-a1-13-hr-f2-shift-attendance hr-F2 排班与考勤需求符合性审计

> 报告类型：MA1(RC) 需求-实现符合性五级追踪审计（methodology §1-§10）
> 切片：A1.13（hr-F2 排班与考勤）
> UC 清单：UC-HR-02 休假申请流程 + UC-HR-06 考勤跟踪 + UC-HR-09 排班管理（3 UC / 22 验收标准）
> 来源 plan：`docs/plans/2026-08-02-2250-2-rc-ma1-a1-13-hr-f2-shift-attendance.md`
> HEAD：`c51ef3f2a`（2026-08-02 实测）
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）

---

## 9. 与 MA2/MA4 报告差异增量声明（methodology §6 段落 9，提前到篇首便于阅读）

本切片复用以下既有审计已证实结论（methodology §去重协议，不重复验证行为本身）：

- **A2.7b**（`2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`，hr 考勤与工资八组件状态机审查）已证实：
  - 休假 5 态全迁移（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）+ approve/cancel 触发排班联动；
  - 换班 4 态全迁移（PENDING/APPROVED/REJECTED/CANCELLED）+ approve 副作用交换 shiftId；
  - `@BizMutation` 事务回滚保证请假→排班联动失败原子性；
  - 请假 cancel 红冲恢复经 `onLeaveCancelled` 回退排班 SCHEDULED；
  - finding P1-MA2-046（排班分配 status 无 dict）/ P1-MA2-091（排班分配 TOCTOU + 无 UK，resolved R1.28）/ P2-MA2-052（state-machine.md 缺考勤/排班/换班章节，watch-only）。
- **A4.4**（`2026-07-29-0430-arm-ma4-hr-code-quality.md`，hr 薪酬/过账/模拟引擎代码质量）已证实：hr production 代码零跨域 `daoFor(ErpFin*)` 写；本切片复核范围（leave/attendance/shift）hr 域内 `daoFor(ErpHrAttendance/ErpHrShiftAssignment)` 均为**同域**访问，不触发 P1-MA1-022 跨域投影；P2-MA4-008（可维护性热点）watch-only。

**本切片只补的需求视角差异**（既有 MA2/MA4 未从需求契约视角裁决的验收标准逐条对照）：
- UC-HR-02：①休假类型 ②durationDays 自动计算 ③5 态审批 ④APPROVED 扣减余额 ⑤日期重叠 ⑥余额不足禁止提交 ⑦**审批人超时自动转派**（新维度，A2.7b 未覆盖） ⑧APPROVED 通知考勤标记休假；
- UC-HR-06：⑨clockIn/clockOut ⑩排班规则计算 workHours/late/early ⑪未打卡→isAbsent ⑫LeaveRequest 关联 leaveRequestId ⑬**多次打卡以最后一次为准**（新维度） ⑭**跨天打卡处理**（新维度，A2.7b 仅覆盖 calc 侧跨天，未覆盖 clocking 侧） ⑮**设备故障手工补卡**（新维度）；
- UC-HR-09：⑯班次模板 ⑰单个/批量/轮换分配 ⑱换班审批交换 ⑲目标员工冲突拒绝 ⑳休假联动标记缺席 ㉑重复排班拦截 ㉒排班作考勤标准输入；以及 resolved finding（P1-MA2-046/091）HEAD 复核。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 真相源：`docs/design/human-resource/use-cases.md`（methodology §4 层级 2 权威功能契约）。

### UC-HR-02 休假申请流程（`use-cases.md:15`）

> **概述** | 员工提交休假申请，经审批后生效，扣减假期余额并联动考勤
> **基本流程** | 1. 员工创建 LeaveRequest，选择休假类型（ANNUAL/SICK/etc.）2. 填写起止日期、原因，系统自动计算 durationDays 3. 提交 → status = SUBMITTED 4. 审批人收到待办，审批通过（→APPROVED）或驳回（→REJECTED）5. 若 APPROVED 扣减假期余额，通知考勤模块在该时段标记为休假 6. 若 REJECTED 员工可修改后重新提交
> **异常** | 余额不足禁止提交；日期重叠校验；审批人超时自动转派
> **跨域协作** | 考勤（ErpHrAttendance.leaveRequestId 关联）

验收标准（8 条，编号①-⑧供矩阵引用）：①休假类型(ANNUAL/SICK/etc.) ②durationDays 自动计算 ③5 态审批(SUBMITTED→APPROVED/REJECTED，含 DRAFT/CANCELLED) ④APPROVED 扣减假期余额 ⑤日期重叠校验 ⑥余额不足禁止提交 ⑦审批人超时自动转派 ⑧APPROVED 通知考勤标记休假。

### UC-HR-06 考勤跟踪（`use-cases.md:63`）

> **概述** | 每日打卡签到/签退，系统自动计算迟到、早退、旷工
> **基本流程** | 1. 系统从打卡机/移动端获取签到时间 2. 创建/更新 ErpHrAttendance 记录 clockIn、clockOut 3. 根据排班规则计算 workHours、lateMinutes、earlyLeaveMinutes 4. 当天未打卡且无请假记录则 isAbsent = true 5. 若当天有已批准的 LeaveRequest，关联 leaveRequestId
> **异常** | 多次打卡以最后一次为准；跨天打卡处理；设备故障时支持手工补卡
> **跨域协作** | 休假（LeaveRequest 联动）；薪酬（考勤数据作为薪资输入）

验收标准（7 条，编号⑨-⑮）：⑨clockIn/clockOut 创建/更新 ⑩排班规则计算 workHours/lateMinutes/earlyLeaveMinutes ⑪未打卡且无请假→isAbsent=true ⑫已批准 LeaveRequest 关联 leaveRequestId ⑬多次打卡以最后一次为准 ⑭跨天打卡处理 ⑮设备故障手工补卡。

### UC-HR-09 排班管理（`use-cases.md:101`）

> **概述** | 定义班次模板，为员工分配排班，支持轮换排班、排班调换申请与审批，休假自动联动标记缺席
> **基本流程** | 1. HR 创建班次模板（ErpHrShift），配置起止时间、宽容期、是否需打卡等 2. 为员工分配排班（ErpHrShiftAssignment），支持单个/批量/按轮换模板分配 3. 员工可发起排班调换申请（ErpHrShiftSwapRequest），经审批后交换班次 4. 休假审批通过后自动关联并标记排班为缺席（isAbsent=true）5. 排班数据作为考勤迟到/早退/缺勤计算的标准输入
> **异常** | 同一员工同一天重复排班时拦截；调换申请目标员工已有冲突排班时拒绝
> **跨域协作** | ErpHrAttendance（考勤迟到/早退基于排班计算）；ErpHrLeaveRequest（休假联动标记缺席）

验收标准（7 条，编号⑯-㉒）：⑯班次模板(起止时间/宽容期/是否需打卡) ⑰单个/批量/轮换分配 ⑱换班申请审批交换班次 ⑲目标员工冲突排班拒绝 ⑳休假审批通过自动关联标记排班缺席(isAbsent=true) ㉑同一员工同一天重复排班拦截 ㉒排班作考勤迟到/早退/缺勤计算标准输入。

### L2 owner doc（设计参考，冲突以 L1 为准）

- `docs/design/human-resource/shift-scheduling.md`：§一 班次模板 + §二 排班分配（含 status dict 声明）+ §三 轮换排班 + §四 迟到/早退/缺勤计算（含 §4.2 跨天班次）+ §五 排班调换 + §六 休假联动 + §九 关键业务规则。
- `docs/design/human-resource/state-machine.md`：§适用对象一 休假（5 态 + §4 异常路径含"审批人长期不处理→超时自动转上级或代班人（可配置）"）。**注**：P2-MA2-052 watch-only——state-machine.md 缺考勤/排班分配/换班独立章节（散落在 shift-scheduling.md）。

---

## 2. 实现证据（L3 代码路径，含行号）

### UC-HR-02 休假申请（5 态 + durationDays + 余额 + 重叠 + 联动）

| 验收标准 | 实现路径（HEAD `c51ef3f2a`） |
|---------|------------------------------|
| ①休假类型 / ②durationDays / ③5 态 | `ErpHrLeaveRequestBizModel.java:44-107`（submit/approve/reject/cancel 四 mutation + `defaultPrepareSave:62-72` DRAFT 默认 + `computeDurationDays:74-79` ChronoUnit.DAYS.between+1 含首尾）；`AbstractErpHrLeaveRequestProcessor`（共享 helper） |
| ④扣减余额 / ⑥余额不足禁止 | `ErpHrLeaveRequestBizModel.checkLeaveBalance:133-153`（remaining = entitled+carried−Σused，不足抛 `ERR_LEAVE_BALANCE_INSUFFICIENT`）；`sumUsedDays:187-201`（聚合 status=APPROVED 的 durationDays）；`getBalance:109-120`（对外查询）；`ErpHrLeaveBalanceBizModel.java:20-37`（额度维护 CRUD，usedDays 派生不落库） |
| ⑤日期重叠 | `ErpHrLeaveRequestBizModel.checkDateOverlap:155-175`（employeeId+leaveType+status∈{APPROVED,SUBMITTED}+startDate≤endDate 且 endDate≥startDate 重叠判定 → `ERR_LEAVE_DATE_OVERLAP`） |
| ⑦审批人超时自动转派 | **未实现**——`resolveApproverId:203-206` 注释「审批人取当前用户关联的员工记录（非关键——仅记录审批轨迹）」直接 `return null`；全 module-hr grep `timeout/escalat/reassign/autoForward` 零业务命中（仅 session/lock/transaction timeout）；scheduler.yaml 仅注册 `ErpHrContractExpiryJob`（UC-HR-07），**无** leave-approver-timeout job bean |
| ⑧APPROVED 通知考勤标记 | `ErpHrLeaveRequestApproveProcessor.java:20-31`（SUBMITTED→APPROVED 守卫 + 审计字段 + `shiftBiz.onLeaveApproved(leave.getId(), context)` 联动调用）；`ErpHrShiftOnLeaveApprovedProcessor.java:18-29`（检索员工休假日期范围内的排班，逐条置 isAbsent=true / absenceReason=LEAVE / leaveRequestId / status=ABSENT）；`ErpHrLeaveRequestCancelProcessor.java:19-26`（APPROVED→CANCELLED 守卫 + `shiftBiz.onLeaveCancelled` 红冲恢复） |
| cancel 红冲 | `ErpHrShiftOnLeaveCancelledProcessor`（回退排班 SCHEDULED / isAbsent=false / leaveRequestId=null，A2.7b 已证实） |

### UC-HR-06 考勤跟踪（打卡 + 计算 + 缺勤 + 联动）

| 验收标准 | 实现路径 |
|---------|----------|
| ⑨clockIn/clockOut | `ErpHrAttendanceBizModel.java:59-68`（clockIn/clockOut mutation 委托 Processor）；`ErpHrAttendanceClockInProcessor.java:19-39`（当日无记录则新建，source=CARD；**clockIn 已有值则抛 `ERR_ALREADY_CLOCKED_IN`**）；`ErpHrAttendanceClockOutProcessor.java:18-29`（无 clockIn 抛 `ERR_NOT_CLOCKED_IN`；置 clockOut + `computeWorkHours`）；`AbstractErpHrAttendanceProcessor.java:32-50`（findAttendance by employeeId+date，computeWorkHours 分钟/60 保留 2 位） |
| ⑩排班规则计算 | `ErpHrShiftCalcAttendanceProcessor.java:24-76`（读 assignment.shift 标准班次 vs attendance 实际打卡，调 `ShiftAttendanceCalculator` 算 late/early/absent，结果写 ErpHrAttendance + 同步 assignment.status）；`ShiftAttendanceCalculator.java:38-74`（calcLateMinutes / calcEarlyLeaveMinutes，含 grace 宽容期）；`ErpHrShiftBizModel.calcAttendance:64-69` 入口 |
| ⑪未打卡→isAbsent | `ShiftAttendanceCalculator.isAbsentByNoClockIn:79-84`（requireClockIn=true 且 clockIn=null → true）；`ErpHrShiftCalcAttendanceProcessor:46-55`（absentByNoClock → upsertAttendanceForAbsent + assignment.status=ABSENT） |
| ⑫LeaveRequest 关联 | `ErpHrShiftCalcAttendanceProcessor:35-43`（assignment.leaveRequestId 非空 → upsertAttendanceForLeave 置 attendance.leaveRequestId + isAbsent=true）；`ErpHrShiftBizModel.upsertAttendanceForLeave:117-129` |
| ⑬多次打卡以最后一次为准 | **未实现（行为相反）**——`ErpHrAttendanceClockInProcessor.java:32-35` `if (attendance.getClockIn() != null) throw ERR_ALREADY_CLOCKED_IN`（首次签到后**拒绝**重复打卡，与 L1「以最后一次为准」字面相反） |
| ⑭跨天打卡处理 | **部分实现（仅 calc 侧）**——`ShiftAttendanceCalculator.calcEarlyLeaveMinutes:61-74` + `isCrossDayShift:24-28`（夜班 endTime 取次日，迟到/早退基准正确，A2.7b/test 证实 calc 侧跨天正确）；**clocking 侧未实现**——`ErpHrAttendanceClockOutProcessor.java:19-20` `findAttendance(employeeId, today)` 按 clockOut 当日查找，夜班跨天签退（如 23:00 签到 Mon、次日 08:00 签退 Tue）查 date=Tue → null → 抛 `ERR_NOT_CLOCKED_IN` |
| ⑮设备故障手工补卡 | **未实现**——全 module-hr grep `makeUp/manualClock/补卡/supplement/adjustClock` 零命中；`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法，无手工补卡 mutation |

### UC-HR-09 排班管理（班次模板 + 分配 + 换班 + 联动）

| 验收标准 | 实现路径 |
|---------|----------|
| ⑯班次模板 | `ErpHrShiftBizModel.java:44-61`（CrudBizModel 标准 CRUD）；ORM `app-erp-hr.orm.xml` ErpHrShift 实体（startTime/endTime/graceLateMinutes/graceEarlyLeaveMinutes/requireClockIn/requireClockOut/restStartTime/restEndTime/totalWorkMinutes/allowOvertime/colorHex，shift-scheduling.md §1.1 字段表全映射） |
| ⑰单个/批量/轮换分配 | `ErpHrShiftAssignmentBizModel.java:68-93`（assignSingle/assignBatch/copyFromPeriod 三 mutation）；`ErpHrShiftAssignmentAssignSingleProcessor.java:15-20`（requireShift + assertNoExistingAssignment + doCreateAssignment）；`ErpHrShiftAssignmentAssignBatchProcessor`（per-mutation 拆分，R6.1）；`ErpHrShiftAssignmentCopyFromPeriodProcessor`；`ErpHrShiftRotationPatternBizModel.java:58-68` + `ErpHrShiftRotationPatternGenerateRotationProcessor.java:51-85`（patternData JSON 解析 + 校验 + staggerDays 错峰逐成员逐日生成 + regenerate 逻辑删除重生成） |
| ⑱换班审批交换 | `ErpHrShiftSwapRequestBizModel.java:55-87`（submit/approve/reject/cancel 4 态 mutation）；`ErpHrShiftSwapRequestApproveProcessor.java:19-50`（PENDING→APPROVED 守卫 + 双方 assignment 互换 shiftId + 记录 swapRequestId + replacedByAssignmentId 双向追溯 + 重置 SCHEDULED 等待 calcAttendance 重算） |
| ⑲目标员工冲突拒绝 | `ErpHrShiftSwapRequestSubmitProcessor.java:18-41`（target==null 或 targetAssignment 不存在 → `ERR_SHIFT_SWAP_TARGET_OCCUPIED`）；UK `(employeeId,assignmentDate,shiftId,delVersion)` 保证每员工每日至多 1 active 排班，post-swap 无新建冲突 |
| ⑳休假联动标记缺席 | `ErpHrShiftOnLeaveApprovedProcessor.java:18-29`（APPROVED → 检索排班 + isAbsent=true + absenceReason=LEAVE + leaveRequestId + status=ABSENT） |
| ㉑重复排班拦截 | `ErpHrShiftAssignmentBizModel.assertNoExistingAssignment:147-153` + `existsActiveAssignment:155-163`（pre-check 抛 `ERR_SHIFT_DUPLICATE_ASSIGNMENT`）+ ORM `app-erp-hr.orm.xml:1210` `<unique-key name="UK_HR_SHIFT_ASSIGNMENT_NATURAL" columns="employeeId,assignmentDate,shiftId,delVersion"/>`（DB 兜底，**P1-MA2-091 resolved R1.28 HEAD 复核确认 UK 已加**）；`doCreateAssignment:121-134` flush + `UniqueConstraintHelper.isUniqueConstraintViolation` → `ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE` 友好翻译 |
| ㉒排班作考勤标准输入 | `ErpHrShiftCalcAttendanceProcessor.java:29-32`（读 assignment.shift 作标准班次 → `ShiftAttendanceCalculator` 计算迟到/早退/缺勤，结果写 attendance） |

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 覆盖验收标准 | 断言强度 | 关键断言 |
|------|-------------|---------|---------|
| `TestErpHrLeaveEngine#testFullApprovalFlowActivatesShiftLinkage:65-91` | ②③⑧ | **强** | DRAFT→submit→APPROVED 状态 + assignment.isAbsent=true + absenceReason=LEAVE + status=ABSENT |
| `TestErpHrLeaveEngine#testInsufficientBalanceBlocksSubmit:93-112` | ⑥ | **强** | assertThrows + `ERR_LEAVE_BALANCE_INSUFFICIENT` 错误码精确匹配 |
| `TestErpHrLeaveEngine#testDateOverlapBlocksSubmit:114-133` | ⑤ | **强** | assertThrows + `ERR_LEAVE_DATE_OVERLAP` 错误码 + 状态保持 DRAFT |
| `TestErpHrLeaveEngine#testCancelRevertsShiftLinkage:135-168` | ③④⑧cancel | **强** | cancel 后 isAbsent=false + status=SCHEDULED + getBalance 恢复 10（余额回退强断言） |
| `TestErpHrLeaveEngine#testDurationDaysAutoCalc:170-187` | ② | **强** | 7/1~7/5 == 5（含首尾） |
| `TestErpHrLeaveEngine#testRejectFromSubmitted / #testIllegalTransitionApprovedToSubmit:189-222` | ③ | **强** | REJECTED 终态 + 非法迁移 assertThrows |
| `TestErpHrAttendanceEngine#testClockInClockOutComputesWorkHours:52-67` | ⑨⑩ | **强** | clockIn 非空 + clockOut 非空 + workHours 计算非空 |
| `TestErpHrAttendanceEngine#testDuplicateClockInBlocked:69-78` | ⑬（反向证实偏离） | **强** | assertThrows + `ERR_ALREADY_CLOCKED_IN`——**证实实现与 L1「以最后一次为准」相反**（测试与实现同步偏离 L1） |
| `TestErpHrAttendanceEngine#testClockOutWithoutClockInBlocked:80-87` | ⑨异常 | **强** | `ERR_NOT_CLOCKED_IN` |
| `TestErpHrShiftScheduling#testAssignSingleEnforces...:78-97` | ㉑ | **强** | 重复分配 assertThrows + `ERR_SHIFT_DUPLICATE_ASSIGNMENT` |
| `TestErpHrShiftScheduling#testConcurrentAssignSameEmployeeDayNoDuplicate:103-159` | ㉑并发 | **强** | 2 线程并发 → UK 兜底仅 1 active 行 + 友好错误码（P1-MA2-091 R1.28 fix 验证） |
| `TestErpHrShiftScheduling#testAssignBatchCreatesForEveryEmployeeEveryDay:161-180` | ⑰批量 | **强** | 2 员工×3 天 == 6 条 |
| `TestErpHrShiftScheduling#testCopyFromPeriodAlignsByDayOffset:182-201` | ⑰复制 | **强** | 偏移对齐 + findByEmployeeAndDate 非空 |
| `TestErpHrShiftScheduling#testRotationGenerateStaggerAndRegenerate:203-240` | ⑰轮换 | **强** | staggerDays=1 错峰（empB 7-1 无班/7-2 有班）+ regenerate 有效排班数不变 |
| `TestErpHrShiftScheduling#testRotationInvalidPatternThrows:242-253` | ⑰异常 | **强** | `ERR_SHIFT_ROTATION_PATTERN_INVALID` |
| `TestErpHrShiftScheduling#testCalcAttendanceOnTimeNoLateNoEarlyLeave:255-276` | ⑩㉒ | **强** | grace 15 内 late=0/early=0 + isAbsent=false |
| `TestErpHrShiftScheduling#testCalcAttendanceLateAndEarlyLeave:278-299` | ⑩㉒ | **强** | 08:30→late=30 / 16:00→early=60 精确数值 |
| `TestErpHrShiftScheduling#testCalcAttendanceCrossDayNightShift:301-323` | ⑩⑭calc侧 | **强** | 夜班 23:00→08:00 次日，late=0/early=0（跨天基准） |
| `TestErpHrShiftScheduling#testCalcAttendanceCrossDayNightShiftEarlyLeave:325-344` | ⑩⑭calc侧 | **强** | 次日 06:00 签退 → early=120 精确 |
| `TestErpHrShiftScheduling#testCalcAttendanceAbsentByNoClockIn:346-369` | ⑪㉒ | **强** | requireClockIn=true 无打卡 → isAbsent=true + assignment.status=ABSENT + absenceReason=LATE_NOT_CLOCKED |
| `TestErpHrShiftScheduling#testSwapApproveSwapsShiftsBetweenAssignments:371-402` | ⑱ | **强** | APPROVED 后双方 shiftId 互换 + swapRequestId 双向追溯 |
| `TestErpHrShiftScheduling#testSwapIllegalTransitionRejects:404-427` | ⑱异常 | **强** | REJECTED→approve assertThrows + `ERR_SHIFT_SWAP_ILLEGAL_STATUS_TRANSITION` |
| `TestErpHrShiftScheduling#testLeaveApprovedMarksAbsentAndCancelReverts:429-466` | ⑧⑳⑬cancel | **强** | 7-1 SCHEDULED / 7-2/7-3 ABSENT + leaveRequestId 关联 + cancel 后回退 SCHEDULED + leaveRequestId=null |
| E2E `tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts` | ⑧⑳ | 中-强 | 浏览器层 GraphQL 全栈可达 |
| E2E `tests/e2e/business-actions/hr-leave-attendance.action.spec.ts:90-189` | ②③⑤⑨⑬ | **强** | DRAFT→SUBMITTED→APPROVED→CANCELLED 状态断言 + 重叠守卫「重叠」token + clockIn/clockOut + **重复 clockIn 守卫「已签到」token（证实⑬偏离）** |
| E2E `tests/e2e/business-actions/hr-shift-swap.action.spec.ts` | ⑱ | 中-强 | 换班审批全栈 |
| E2E `tests/e2e/business-actions/hr-shift-assignment.action.spec.ts` | ⑰㉑ | 中-强 | 排班分配全栈 |
| E2E `tests/e2e/business-actions/hr-shift-rotation.action.spec.ts` | ⑰轮换 | 中-强 | 轮换生成全栈 |

**断言强度小结**（methodology §1 L4 引用 MA5 评级）：3 JUnit 测试类 + 5 E2E spec 全部为**强断言或中-强断言**（状态精确值 + 错误码精确匹配 + 数值断言），无仅冒烟。**未覆盖**：⑦审批人超时自动转派（零测试）、⑬last-wins 正向路径（测试仅覆盖 reject 反向）、⑭clocking 侧跨天 clockOut（测试仅覆盖 calc 侧跨天）、⑮手工补卡（零测试）—— 与 L3 实现缺口同步，纳入 §5 符合性结论。

---

## 4. 运行时行为证据（L5，复用 A2.7b/A4.4 + 本切片差异）

| 验收标准 | L5 行为证据来源 |
|---------|----------------|
| ②③④⑤⑥⑧（leave 主链 + 联动） | A2.7b 已证实（5 态全迁移 + approve/cancel 触发排班联动 + @BizMutation 事务回滚 + cancel 红冲 onLeaveCancelled）；本切片 JUnit + E2E 强断言复证实 |
| ⑨⑩⑪⑫（attendance 主链） | A2.7b 已证实（打卡端点 + 排班计算 + isAbsent 判定 + leaveRequestId 关联）；本切片 JUnit 强断言复证实 |
| ⑭calc 侧跨天 | A2.7b 已证实 + 本切片 testCalcAttendanceCrossDayNightShift/EarlyLeave 强数值断言复证实 |
| ⑯⑰⑱⑲⑳㉑㉒（shift 主链） | A2.7b 已证实（班次模板 CRUD + 单/批/轮换分配 + 换班 4 态 + approve 副作用交换 shiftId + 重复排班守卫 + 休假联动 + 排班作考勤标准）；P1-MA2-091 R1.28 UK fix 经 testConcurrentAssignSameEmployeeDayNoDuplicate 强证实 |
| **⑦审批人超时自动转派** | **无运行时证据**（无 job/scheduler 消费）——本切片新发现 |
| **⑬多次打卡 last-wins** | **运行时行为=reject**（与 L1 相反）——JUnit + E2E 双重证实 |
| **⑭clocking 侧跨天 clockOut** | **运行时行为=抛 ERR_NOT_CLOCKED_IN**（夜班跨天签退查不到当日记录）——静态推断（无测试覆盖） |
| **⑮手工补卡** | **无运行时证据**（无 mutation）——本切片新发现 |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 裁决，methodology §2 判据）

### 五级追踪矩阵

| UC | L1 | L2 | L3 | L4 | L5 | 结论 |
|----|----|----|----|----|----|------|
| UC-HR-02 | `use-cases.md:15` ①-⑧（见 §1 逐字引用） | `shift-scheduling.md §六` + `state-machine.md §适用对象一`（含 §4 异常「超时自动转上级或代班人」） | 见 §2 UC-HR-02 表（BizModel:44-107/Approve/Cancel/OnLeaveApproved Processor） | TestErpHrLeaveEngine 7 强断言 + E2E strong | A2.7b 证实主链 + 本切片复证实；**⑦无运行时证据** | **P1**（⑦异常路径未实现） |
| UC-HR-06 | `use-cases.md:63` ⑨-⑮ | `shift-scheduling.md §四`（§4.2 跨天班次）+ `state-machine.md`（缺考勤独立章节，P2-MA2-052 watch-only） | 见 §2 UC-HR-06 表（AttendanceBizModel/ClockIn/ClockOut/CalcAttendance Processor/ShiftAttendanceCalculator） | TestErpHrAttendanceEngine 4 强 + TestErpHrShiftScheduling calc 强 + E2E strong | A2.7b 证实主链 + calc 侧跨天；**⑬运行时=reject / ⑭clocking 侧跨天抛错 / ⑮无 mutation** | **P1**（⑬行为实质偏离 + ⑭⑮异常路径未实现） |
| UC-HR-09 | `use-cases.md:101` ⑯-㉒ | `shift-scheduling.md §一/§二/§三/§五/§九` + `state-machine.md`（缺排班/换班独立章节，P2-MA2-052 watch-only） | 见 §2 UC-HR-09 表（Shift/ShiftAssignment/ShiftSwapRequest/ShiftRotationPattern BizModel + 8 Processor） | TestErpHrShiftScheduling 11 强断言 + E2E strong | A2.7b 证实主链 + P1-MA2-091 UK fix 强证实 | **接受**（7 验收标准全 PASS） |

### 每 UC 裁决（methodology §2 取最高原则）

#### UC-HR-02 → **P1**

- ①②③④⑤⑥⑧ = **接受**（8 验收标准中 7 项五级全对齐，主路径 + 异常路径[余额/重叠/红冲]强测试覆盖）。
- ⑦审批人超时自动转派 = **P1**（§2 P1② 异常路径未实现）。L1 `use-cases.md:24` 异常段逐字「审批人超时自动转派」+ L2 `state-machine.md:51` 逐字「审批人长期不处理 | 超时自动转上级或代班人（可配置）」一致要求；L3 `resolveApproverId:203-206` return null + 全 module-hr 无 timeout/escalat/reassign 业务代码 + scheduler.yaml 仅注册合同到期 job 无 leave-timeout job → 异常路径完全缺失。**非 P0**：审批人超时不破坏活跃数据（休假仍处 SUBMITTED 待审批，余额暂扣不变，无 GL/库存破坏），属流程效率/SLA 类异常路径缺失非核心循环断裂。**新建 finding P1-RC-011**（见 §6）。

#### UC-HR-06 → **P1**

- ⑨⑩⑪⑫ = **接受**（主链 4 验收标准五级全对齐）。
- ⑬多次打卡以最后一次为准 = **P1**（§2 P1① 行为实质偏离验收标准，取最高）。L1 `use-cases.md:72` 异常段逐字「多次打卡以最后一次为准」；L3 `ErpHrAttendanceClockInProcessor.java:32-35` 实现**相反**——`if (clockIn != null) throw ERR_ALREADY_CLOCKED_IN`（首次签到后拒绝重复打卡）。**测试与实现同步偏离**：`TestErpHrAttendanceEngine#testDuplicateClockInBlocked:69-78` + E2E `hr-leave-attendance:182-185` 均断言 reject 行为。**非 P0**：打卡准确性分歧非活跃数据破坏（GL/库存不受影响），且 reject 行为可经手工 DB 修正。**新建 finding P1-RC-012**（见 §6）。
- ⑭跨天打卡处理 = **P1**（§2 P1② 异常路径未实现，clocking 侧）。L1 `use-cases.md:72` 异常段逐字「跨天打卡处理」；L3 calc 侧 `ShiftAttendanceCalculator:24-28/61-74` + 夜班 endTime 取次日**已实现且强测试覆盖**（testCalcAttendanceCrossDayNightShift/EarlyLeave）；**clocking 侧未实现**——`ErpHrAttendanceClockOutProcessor:19-20` 按 clockOut 当日 `findAttendance(employeeId, today)` 查找，夜班跨天签退（23:00 Mon 签到、08:00 Tue 签退）查 date=Tue → null → 抛 `ERR_NOT_CLOCKED_IN`。**非 P0**：影响夜班员工签退操作可用性，非数据破坏。**新建 finding P1-RC-013**（见 §6）。
- ⑮设备故障手工补卡 = **P1**（§2 P1② 异常路径未实现）。L1 `use-cases.md:72` 异常段逐字「设备故障时支持手工补卡」；L3 全 module-hr grep `makeUp/manualClock/补卡/supplement/adjustClock` 零命中，`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法，**无手工补卡 mutation**。**非 P0**：补卡缺失影响考勤数据完整性，非活跃数据破坏。**新建 finding P1-RC-014**（见 §6）。

#### UC-HR-09 → **接受**

- ⑯⑰⑱⑲⑳㉑㉒ = **全部接受**（7 验收标准五级全对齐，11 强断言 + 4 E2E 覆盖；班次模板字段全映射 + 单/批/复制/轮换分配全实现 + 换班 4 态 approve 交换 + 重复排班 pre-check + UK 兜底 + 休假联动标记缺席 + 排班作考勤标准输入）。
- **P1-MA2-091（resolved R1.28）HEAD 复核**：ORM `app-erp-hr.orm.xml:1210` `<unique-key name="UK_HR_SHIFT_ASSIGNMENT_NATURAL" columns="employeeId,assignmentDate,shiftId,delVersion"/>` **已存在** + `doCreateAssignment:121-134` flush + `UniqueConstraintHelper` 友好翻译 + `testConcurrentAssignSameEmployeeDayNoDuplicate:103-159` 强断言（2 线程并发 → 仅 1 active 行）。**UC-HR-09㉑ 重复排班拦截 dedup 闭环**（同根因同控制点，复用 P1-MA2-091，不新建）。
- **P1-MA2-046（resolved R1.15）HEAD 复核**：ORM `app-erp-hr.orm.xml:1192` `<column name="status" ext:dict="erp-hr/shift-assignment-status" ...>` **dict 绑定已加** + `module-hr/erp-hr-meta/.../erp-hr/shift-assignment-status.dict.yaml` 存在（4 值 SCHEDULED/PRESENT/ABSENT/CANCELLED）。**dict 绑定缺失已闭环**（owner doc `shift-scheduling.md §二 :85` dict 声明与 ORM 一致），维持 resolved。

### resolved finding HEAD 复核汇总（methodology §10 + 本计划 Phase 1 要求）

| Finding | arm-index 声称状态 | HEAD `c51ef3f2a` 复核结论 |
|---------|-------------------|--------------------------|
| P1-MA2-046（排班分配 status 无 dict raw VARCHAR） | ✅ resolved (R1.15) | **确认 resolved**——ORM :1192 ext:dict 已加 + dict yaml 存在 |
| P1-MA2-091（排班分配 TOCTOU + 无 UK） | ✅ resolved (R1.28) | **确认 resolved**——ORM :1210 UK_HR_SHIFT_ASSIGNMENT_NATURAL 已加 + 友好翻译 + 并发测试强覆盖 |
| P1-MA1-022（跨域只读 daoFor hr 投影） | ✅ resolved (plan 2026-07-29-2225-1) | **本切片 N/A**——leave/attendance/shift 链路 `daoProvider().daoFor(ErpHrAttendance/ErpHrShiftAssignment)` 均为**同域 hr** 访问（A4.4 已证实 hr production 零跨域 daoFor 写），不触发跨域投影 |
| P2-MA2-052（state-machine.md 缺考勤/排班/换班章节） | watch-only | **维持 watch-only**——state-machine.md 仅含适用对象一休假 + 二员工 + 三工时 + 四薪酬审批 + 五预留，**无**考勤/排班分配/换班独立章节（散落在 shift-scheduling.md/payroll.md），与 arm-index 登记一致 |
| P2-MA4-008（hr 可维护性热点 6 项） | watch-only | **维持 watch-only**——本切片范围（leave/attendance/shift）未发现新可维护性热点，A4.4 登记的 6 项（Survey/BankFile 桩 + Timesheet 硬编码 + IncomeTax 死代码 + applyOverride/readSalaryField 重复 + loadEmployee* dao-for + SocialInsurance BigDecimal[]）均在薪酬/员工域，非本切片控制点 |

### P0 即时通道

**本切片无 P0**——4 项新 P1（⑦/⑬/⑭/⑮）均为异常路径未实现或行为偏离，不构成 §2 P0①②③④（活跃数据破坏/安全/核心循环断裂/会计过账破坏）。MR1 通道修复。

---

## 6. 与 arm-index 衔接（methodology §7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` hr 休假/考勤/排班/换班同域同控制点（P1-MA2-046/091、P2-MA2-052、P2-MA4-008、P1-MA1-022）。裁决规则：同根因同控制点 → 复用（追加 RC 交叉引用）；新根因/新功能点/新维度 → 新建 `P1-RC-xxx`。

### 新建 finding（4 项 P1，均新维度——既有 MA2 状态机/A4.4 代码质量审计未从需求契约视角覆盖）

#### `P1-RC-011` UC-HR-02⑦ 审批人超时自动转派完全缺失

- **域/UC**：hr / UC-HR-02 断言⑦（异常路径）。
- **三源对照**：L1 `use-cases.md:24` 逐字「审批人超时自动转派」+ L2 `state-machine.md:51` 逐字「审批人长期不处理 | 超时自动转上级或代班人（可配置）」一致要求；L3 `ErpHrLeaveRequestBizModel.resolveApproverId:203-206` return null（注释自承「非关键——仅记录审批轨迹」）+ 全 module-hr grep `timeout/escalat/reassign/autoForward` 零业务命中 + `scheduler.yaml` 仅注册 `ErpHrContractExpiryJob`（UC-HR-07 合同到期）无 leave-approver-timeout job bean。
- **分级判据**：§2 P1②（异常路径未实现）。
- **非 P0 论证**：审批人超时不破坏活跃数据（休假仍处 SUBMITTED，余额暂扣不变，无 GL/库存破坏），属 SLA/流程效率类异常路径缺失。
- **与既有 finding 不同维度**：vs P1-MA2-039（员工 RESIGNED 死状态，不同控制点：员工状态机 vs 休假审批人超时）；vs P2-MA2-048（招聘 close 无守卫，不同控制点）。本切片从需求契约视角裁决审批人超时自动转派，既有 MA2 状态机审计未覆盖（A2.7b 覆盖 5 态迁移守卫但未覆盖超时自动转派异常路径）。
- **目标 MR**：MR1（R1.0 展开为 RC-R1.n）。
- **修复路径**：新增 leave-approver-timeout config key（如 `erp-hr.leave-approver-timeout-hours` 默认 48h）+ scheduler.yaml 注册 leave-approver-timeout job（扫描 SUBMITTED 超时 + approvedById 仍空的 LeaveRequest）+ job bean 调用 `IErpHrEmployeeBiz` 解析上级/代班人 + 更新 LeaveRequest.approverId（或派发 notify 提醒）。**纯 BizModel + scheduler 接线 + config key，按 roadmap 预授权类目[代码逻辑修复]可自动执行，不触发 §5 ask-first**（不触及 ORM/会计过账/数据删除）。

#### `P1-RC-012` UC-HR-06⑬ 多次打卡行为相反（reject 而非 last-wins）

- **域/UC**：hr / UC-HR-06 断言⑬（异常路径）。
- **三源对照**：L1 `use-cases.md:72` 逐字「多次打卡以最后一次为准」；L3 `ErpHrAttendanceClockInProcessor.java:32-35` `if (attendance.getClockIn() != null) throw new NopException(ERR_ALREADY_CLOCKED_IN)`（实现**相反**——首次签到后拒绝重复打卡）。
- **分级判据**：§2 P1①（行为实质偏离验收标准，取最高；同时命中 §2 P1② 异常路径未实现）。**测试与实现同步偏离 L1**：`TestErpHrAttendanceEngine#testDuplicateClockInBlocked:69-78` + E2E `hr-leave-attendance.action.spec.ts:182-185` 均断言 reject 行为（两测试反向证实实现偏离）。
- **非 P0 论证**：打卡准确性分歧非活跃数据破坏（GL/库存不受影响），reject 行为可经手工 DB 修正。
- **与既有 finding 不同控制点**：本切片为 hr 考勤打卡异常路径新维度，既有 MA2/MA4 未覆盖（A2.7b 覆盖 attendance 状态/isAbsent 判定但未覆盖多次打卡语义）。
- **目标 MR**：MR1（R1.0 展开为 RC-R1.n）。
- **修复路径**：`ErpHrAttendanceClockInProcessor.clockIn` 移除 `if (clockIn != null) throw` 守卫，改为 `attendance.setClockIn(CoreMetrics.currentTimestamp())` 覆盖原值（last-wins）+ 同步重算 workHours（若 clockOut 已存在）；测试调整为「第二次 clockIn 后 clockIn 时间戳为后值」。**纯 BizModel 代码逻辑修复 + 测试调整，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。

#### `P1-RC-013` UC-HR-06⑭ 跨天打卡 clockOut 侧未实现（夜班跨天签退查不到记录）

- **域/UC**：hr / UC-HR-06 断言⑭（异常路径，clocking 侧）。
- **三源对照**：L1 `use-cases.md:72` 逐字「跨天打卡处理」；L2 `shift-scheduling.md §4.2:201-212` 逐字「夜班（23:00-08:00）等跨天班次…自动识别次日 endTime」（设计要求跨天处理）；L3 calc 侧 `ShiftAttendanceCalculator.calcEarlyLeaveMinutes:61-74` + `isCrossDayShift:24-28` 夜班 endTime 取次日**已实现且强测试覆盖**（testCalcAttendanceCrossDayNightShift/EarlyLeave），**clocking 侧未实现**——`ErpHrAttendanceClockOutProcessor:19-20` `findAttendance(employeeId, today)` 按 clockOut 当日查找，夜班跨天签退（23:00 Mon 签到 date=Mon、08:00 Tue 签退）查 date=Tue → null → 抛 `ERR_NOT_CLOCKED_IN`。
- **分级判据**：§2 P1②（异常路径未实现，clocking 侧）。
- **非 P0 论证**：影响夜班员工签退操作可用性（功能不可用），非数据破坏；calc 侧跨天已正确，仅 clocking 入口阻断。
- **与既有 finding 不同控制点**：vs P1-RC-012（多次打卡 reject，不同代码路径：clockIn 守卫 vs clockOut 查找键）；本切片为 hr 考勤 clockOut 跨天新维度，既有 MA2/MA4 未覆盖（A2.7b 覆盖 calc 侧跨天但未覆盖 clocking 侧）。
- **目标 MR**：MR1（R1.0 展开为 RC-R1.n）。
- **修复路径**：`ErpHrAttendanceClockOutProcessor.clockOut` 查找逻辑增强——若 date=today 无记录，回退查 date=yesterday 且该 assignment 关联夜班（`ShiftAttendanceCalculator.isCrossDayShift(shift)`）的记录；或 `ErpHrAttendance` 增加 `originAssignmentDate` 列明确跨天归属（**后者触及 ORM 结构变更须 ask-first**，前者纯 BizModel 逻辑修复预授权自动执行）。

#### `P1-RC-014` UC-HR-06⑮ 设备故障手工补卡完全缺失

- **域/UC**：hr / UC-HR-06 断言⑮（异常路径）。
- **三源对照**：L1 `use-cases.md:72` 逐字「设备故障时支持手工补卡」；L3 全 module-hr grep `makeUp/manualClock/补卡/supplement/adjustClock` 零命中，`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法，**无手工补卡 mutation**（HR 无法为设备故障员工补录打卡时间）。
- **分级判据**：§2 P1②（异常路径未实现）。
- **非 P0 论证**：补卡缺失影响考勤数据完整性（缺勤误判/工时少计），非活跃数据破坏；可经手工 DB 修正。
- **与既有 finding 不同控制点**：vs P1-RC-012/013（不同异常路径：多次打卡/跨天/补卡三独立控制点）；本切片为 hr 考勤补卡新维度，既有 MA2/MA4 未覆盖。
- **目标 MR**：MR1（R1.0 展开为 RC-R1.n）。
- **修复路径**：`IErpHrAttendanceBiz` + `ErpHrAttendanceBizModel` 新增 `makeUpClockIn(employeeId, date, clockInTime, reason)` / `makeUpClockOut(employeeId, date, clockOutTime, reason)` mutation（HR 角色守卫 + source=MANUAL 标记 + reason 必填 + 审计字段写回）+ owner doc `shift-scheduling.md` 补注「手工补卡入口」。**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。

### 复用既有 finding（4 项，追加 RC 交叉引用注记不新建）

| Finding | 裁决 | 依据 |
|---------|------|------|
| `P1-MA2-046`（排班分配 status 无 dict） | **复用**，HEAD `c51ef3f2a` 确认 resolved (R1.15) | ORM :1192 ext:dict 已加 + dict yaml 存在；UC-HR-09⑯ 班次模板/分配 dict 维度本切片复核闭环 |
| `P1-MA2-091`（排班分配 TOCTOU + 无 UK） | **复用**，HEAD `c51ef3f2a` 确认 resolved (R1.28) | ORM :1210 UK_HR_SHIFT_ASSIGNMENT_NATURAL 已加 + 并发测试强覆盖；UC-HR-09㉑ 重复排班拦截 dedup 闭环（同根因同控制点，不新建 RC） |
| `P2-MA2-052`（state-machine.md 缺考勤/排班/换班章节） | **复用**，维持 watch-only | 本切片证实 state-machine.md 仍缺独立章节（散落在 shift-scheduling.md），追加 RC A1.13 交叉引用注记 |
| `P2-MA4-008`（hr 可维护性热点 6 项） | **复用**，维持 watch-only | 本切片范围未发现新热点，6 项均在薪酬/员工域非本切片控制点 |

### 双向可追溯

- **新 finding 入 arm-index**：P1-RC-011/012/013/014 产出即写入 `arm-index.md` MA1(RC) finding 分区（§归档规范 §7）。
- **修复行引用 finding**：MR1 的 RC-R1.n 修复行须含 finding ID 交叉引用（methodology §10 MR1 展开器机制）。
- **arm-index finding 回填修复状态**：修复完成后在 arm-index 对应行回填 `done`。

---

## 7. 静态存疑点清单（供 MA4 A4.2 运行时展开）

> methodology §6 段落 7：本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行。

- **SP-1（P1-RC-011 运行时触发面）**：审批人超时自动转派缺失在运行时的实际影响面——SUBMITTED 休假长期未审批的累积量、是否影响薪酬核算（UC-HR-04 缺勤数据来源）。MA4 A4.2 运行时确认 SUBMITTED 悬挂量与薪酬核算的耦合度。
- **SP-2（P1-RC-012 多次打卡 reject 运行时误判面）**：员工误触多次 clockIn 被拒后的逃生路径（HR 手工 DB 修正频度）、是否存在考勤申诉工单。MA4 A4.2 运行时确认 reject 行为的运营影响。
- **SP-3（P1-RC-013 夜班跨天 clockOut 运行时阻断）**：夜班员工实际 clockOut 失败率、是否普遍存在「夜班次日补录」的临时运维流程。MA4 A4.2 运行时确认夜班占比与 clockOut 阻断频度。
- **SP-4（P1-RC-014 设备故障补卡运行时替代）**：设备故障时 HR 是否经标准 CRUD 直接 update ErpHrAttendance.clockIn/clockOut 绕过（ CrudBizModel 默认 save/update 不受字段级守卫保护），存在越权风险。MA4 A4.2 运行时确认补卡替代路径与权限。
- **SP-5（UC-HR-09⑲ 换班跨日期语义）**：`ErpHrShiftSwapRequestSubmitProcessor` 未校验 source/target assignment 同日期，跨日期换班的运行时语义（empA 7/1 早班 ↔ empB 7/2 中班 → 交换后 empA 7/1 上中班 / empB 7/2 上早班，语义可疑）。L1 未显式要求同日期校验，但运行时可能产生困惑。MA4 A4.2 确认是否需补同日期守卫。

---

## 8. 过程纪律自检（methodology §8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下（HEAD `c51ef3f2a`，2026-08-02-2344 实测）：

  | 规则 | 描述 | 严重度 | actual 命中 |
  |------|------|--------|-------------|
  | R1a/b/c | BizModel dao().save/update/getEntityById/findAllByQuery | 🔴 高 | 0/0/0/14 |
  | R2a | BizModel daoFor(ErpMd*) | 🔴 高 | 34 |
  | R2b | BizModel daoFor(Erp*) 跨域 | 🔴 高 | 229 |
  | R2c | 全生产代码 daoFor() 总量 | 🔴 高 | 1382 |
  | R3 | new Erp*() 构造实体 | 🟡 中 | 5 |
  | R6 | @Transactional in BizModel | 🟢 低 | 2 |
  | R10 | REQUIRES_NEW 事务 | 🟡 中 | 6 |
  | R12a/b/c | 共享内核 import ErpFinBusinessType/PostingEvent/AcctSchemaResolver | 🟡 中 | 69/66/40 |

  **本审计无生产代码变更**（只读审计：未修改任何 .java/.xml/.yaml 真相源），checker actual = baseline 基线（与上次审计落锚后无回归风险）。**不以 checker 脚本退出码 0 作为门控通过依据**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告未引入新违规（本审计未改代码）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（methodology §8 + plan Closure Gates）。

- [x] **与 arm-index 交叉去重声明**：本报告全部 4 项新 finding（P1-RC-011/012/013/014）已按 methodology §7 规则 grep arm-index hr 休假/考勤/排班/换班同域同控制点（P1-MA2-046/091、P2-MA2-052、P2-MA4-008、P1-MA1-022、P1-MA2-039、P2-MA2-048）后给出「复用 or 新增」裁决（§6 表）。**无未经比对直接新建的 finding**：4 项均为需求契约视角新维度（审批人超时/多次打卡语义/clockOut 跨天/手工补卡），既有 MA2 状态机/A4.4 代码质量审计未从需求契约视角覆盖这些异常路径。

- [x] **未修改真相源声明**（methodology §9）：本审计期间未修改 `product-scope.md` / `use-cases.md` / owner doc 需求契约段落；发现的 L1↔L3 分歧（⑦/⑬/⑭/⑮）记入本报告 §5/§6，不直改真相源（§9 冻结条款）。

- [x] **报告 9 段完整性自检**（methodology §6 段落完整性自检）：§1 需求契约原文 ✅ / §2 实现证据 ✅ / §3 测试证据 ✅ / §4 运行时行为证据 ✅ / §5 符合性结论 ✅ / §6 与 arm-index 衔接 ✅ / §7 静态存疑点清单 ✅ / §8 过程纪律自检 ✅ / §9 与 MA2/MA4 报告差异增量声明 ✅（§9 提前到篇首便于阅读，内容完整）。9 段齐全。

---

## 报告产出总结

- **切片整体裁决**：A1.13 hr-F2 排班与考勤 = **P1**（3 UC 中 UC-HR-02/06 各 P1，UC-HR-09 接受）。
- **新 finding**：4 项 P1（P1-RC-011/012/013/014），0 项 P0，0 项 P2（本切片新发现维度均达 P1 阈值）。
- **复用 finding**：4 项（P1-MA2-046/091 HEAD 复核确认 resolved；P2-MA2-052/P2-MA4-008 维持 watch-only）。
- **修复通道**：4 项 P1 全部目标 MR1（R1.0 展开为 RC-R1.n）；其中 P1-RC-013 修复可选方案 B 触及 ORM 结构变更须 ask-first（若选方案 A 纯 BizModel 逻辑则预授权自动执行），其余 3 项纯 BizModel/scheduler 代码逻辑预授权自动执行。
- **resolved finding HEAD 复核**：P1-MA2-046 + P1-MA2-091 在 HEAD `c51ef3f2a` 2/2 确认 resolved 无回退；UC-HR-09㉑ 重复排班拦截 dedup 闭环。

