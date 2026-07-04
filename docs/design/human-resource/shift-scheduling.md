# 排班管理（Shift Scheduling）

## 目的

详细设计排班管理功能：班次模板定义 → 员工排班分配 → 考勤自动比对排班 → 迟到/早退按排班计算 → 排班调换审批 → 休假自动联动。实现对复杂排班场景（固定班、倒班、轮班）的业务支持。

## 设计边界

- 本设计负责：班次模板维护、员工排班分配（按人/按组/按日期）、排班与打卡的迟到/早退/缺勤计算、排班调整/交换审批、休假与排班的联动标记。
- **与 attendance 的边界**：考勤打卡数据（clockIn/clockOut）由 ErpHrAttendance 记录；本设计通过 `ErpHrShiftAssignment` 提供"该员工当天应上什么班"的标准，供 attendance 模块计算迟到/早退/缺勤。
- **与 leave 的边界**：休假审批通过后，本设计自动将对应日期的排班标记为"休假缺席"，不再算旷工。
- 本设计不负责：打卡机集成、加班费自动计算（加班费由 payroll 引用考勤数据计算）。

---

## 一、班次模板（ErpHrShift）

### 1.1 实体定义

| 字段 | 含义 |
|------|------|
| id/code/orgId | 标准 |
| name | 班次名称（如"早班"、"中班"、"夜班"） |
| shiftType | 班次类型 dict `erp-hr/shift-type`：FIXED（固定班）/ ROTATING（倒班）/ FLEXIBLE（弹性班） |
| startTime | 上班时间（如 08:00） |
| endTime | 下班时间（如 17:00） |
| graceLateMinutes | 迟到宽容分钟数（如 15 分钟内不算迟到） |
| graceEarlyLeaveMinutes | 早退宽容分钟数 |
| requireClockIn | 是否需要打卡签到（true/false） |
| requireClockOut | 是否需要打卡签退（true/false） |
| restStartTime/restEndTime | 休息时段（午休/晚休，用于计算实际工时） |
| totalWorkMinutes | 标准工时（分钟，派生 = endTime - startTime - restDuration） |
| allowOvertime | 是否允许加班（超出标准工时部分计为加班） |
| colorHex | 排班看板显示颜色 |
| description | 班次说明 |
| 标准审计字段 | |

**预置班次示例**：

| code | 名称 | 起止时间 | 类型 | 工时 |
|------|------|----------|------|------|
| MORNING | 早班 | 08:00-17:00 | FIXED | 8h |
| AFTERNOON | 中班 | 14:00-23:00 | FIXED | 8h |
| NIGHT | 夜班 | 23:00-08:00 | FIXED | 8h（跨天） |
| FLEX_AM | 弹性上午 | 07:00-09:00 弹性到岗 | FLEXIBLE | 8h |
| ROTATE_A | 倒班A | 08:00-20:00 | ROTATING | 12h |
| ROTATE_B | 倒班B | 20:00-08:00 | ROTATING | 12h（跨天） |

### 1.2 班次模板管理

```
HR/排班管理员
    │
    ├─► 创建/编辑班次模板（ErpHrShift）
    │       ├─ 设置班次类型/时间/宽容期
    │       └─ 适用于全公司或指定部门
    │
    ├─► 班次模板列表（网格视图 + 时间轴预览）
    │
    └─► 班次生效/失效管理（effectiveFrom/effectiveTo）
```

> 🟢 Odoo `hr_attendance.ShiftTemplate`（班次模板 + 宽容期）。
> 🟢 AureusERP `shift_template.php`（班次模板维护）。

---

## 二、排班分配（ErpHrShiftAssignment）

### 2.1 实体定义

| 字段 | 含义 |
|------|------|
| id/orgId | 标准 |
| employeeId | 员工（→ErpHrEmployee） |
| shiftId | 班次（→ErpHrShift） |
| assignmentDate | 排班日期 |
| actualStartTime | 实际打卡签到时间（从 ErpHrAttendance 反写） |
| actualEndTime | 实际打卡签退时间 |
| isAbsent | 是否缺勤（自动计算或手动标记） |
| absenceReason | 缺勤原因 dict：LEAVE（休假）/ LATE_NOT_CLOCKED（迟到未打卡）/ OTHER |
| leaveRequestId | 关联休假申请（→ErpHrLeaveRequest，若有） |
| swapRequestId | 关联排班调换申请（→ErpHrShiftSwapRequest，若有） |
| replacedByAssignmentId | 被替换的排班（调换场景） |
| status | dict：SCHEDULED（已排班）/ PRESENT（已到岗）/ ABSENT（缺勤）/ CANCELLED（取消） |
| 标准审计字段 | |

### 2.2 排班分配方式

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| 单个分配 | 选择员工 + 日期 + 班次 | 个别调整 |
| 批量分配 | 选择员工组 + 日期范围 + 班次 | 批量排班 |
| 模板分配 | 按轮换模板自动生成（见 §三） | 倒班/轮班场景 |
| 复制上期 | 复制上周/上月排班 | 固定班场景 |

**批量分配操作**：

```
排班管理员选择：
    │
    ├─► 员工范围（部门/岗位/多选）
    │
    ├─► 日期范围（如 2026-07-01 ~ 2026-07-31）
    │
    └─► 班次（如"早班"）
    │
    └─► 点击"分配" → 批量创建 ErpHrShiftAssignment
```

### 2.3 排班看板

日历视图（月度/周度）展示每位员工的每日排班：

```
七月 2026                   早班      中班    夜班    休
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│ 员工      │ 7/1 (三) │ 7/2 (四) │ 7/3 (五) │ 7/4 (六) │
├──────────┼──────────┼──────────┼──────────┼──────────┤
│ 张三      │ ■早班    │ ■早班    │ ■中班    │ □休      │
│ 李四      │ ■中班    │ ■中班    │ ■夜班    │ □休      │
│ 王五      │ ■夜班    │ □休      │ ■早班    │ ■早班    │
└──────────┴──────────┴──────────┴──────────┴──────────┘
```

> 🟢 Odoo `hr_attendance` 排班日历视图。

---

## 三、轮换排班（Rotating Shift Pattern）

### 3.1 轮换模板（ErpHrShiftRotationPattern）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| patternType | 轮换类型 dict：CYCLE_DAYS（按天轮换）/ CYCLE_WEEKS（按周轮换） |
| patternData | 轮换序列（JSON 数组，定义每天/每周的班次 code） |
| rotateInterval | 轮换间隔（如每 7 天轮换） |
| startDate | 轮换起始日期（首次分配日期） |
| groupId | 轮换组（→ErpHrShiftRotationGroup） |

**示例：三班两倒（按周轮换）**：

```json
// 第1周 → 第2周 → 第3周 → 循环
["MORNING", "AFTERNOON", "NIGHT"]
```

**示例：做二休二（按天轮换）**：

```json
// Day1 → Day2 → Day3 → Day4 → 循环
["MORNING", "MORNING", "OFF", "OFF"]
```

### 3.2 轮换组（ErpHrShiftRotationGroup）

| 字段 | 含义 |
|------|------|
| id/name/orgId | 标准 |
| members | 组成员（→ErpHrEmployee 列表，JSON） |
| patternId | 关联轮换模板 |
| staggerDays | 组内错峰天数（如 A 组比 B 组晚 1 天开始轮换） |

### 3.3 轮换生成

```
HR 选择轮换模板 + 轮换组
    │
    ├─► 系统根据 patternData + startDate + 成员列表自动生成排班
    │
    ├─► 按 staggerDays 错开各成员的轮换起点
    │
    ├─► 生成指定日期范围内的 ErpHrShiftAssignment
    │
    ├─► 预览确认 → 批量保存
    │
    └─► 后续支持"重新生成"（更新已有排班）
```

---

## 四、迟到/早退/缺勤计算

### 4.1 计算规则

考勤模块读取 `ErpHrShiftAssignment` 获取员工当天的标准班次，然后比对实际打卡时间：

| 场景 | 条件 | 结果 |
|------|------|------|
| 准时 | clockIn ≤ shift.startTime + graceLateMinutes | lateMinutes = 0 |
| 迟到 | clockIn > shift.startTime + graceLateMinutes | lateMinutes = clockIn - shift.startTime |
| 严重迟到 | clockIn > shift.startTime + shift.graceLateMinutes + threshold | 标记严重迟到 |
| 早退 | clockOut < shift.endTime - graceEarlyLeaveMinutes | earlyLeaveMinutes = shift.endTime - clockOut |
| 缺勤（有排班无打卡） | 有 assignment 且 requireClockIn=true 但无 clockIn | isAbsent = true |
| 缺勤（休假标记） | assignment.leaveRequestId 非空 | isAbsent = true, absenceReason=LEAVE |
| 正常 | clockIn ≤ startTime+grace 且 clockOut ≥ endTime-grace | 全勤 |

### 4.2 跨天班次处理

夜班（23:00-08:00）等跨天班次的迟到/早退计算：

```
班次：NIGHT（startTime=23:00, endTime=08:00）
    │
    ├─► 迟到基准：当天 23:00（clockIn > 23:00+grace → 迟到）
    │
    └─► 早退基准：次日 08:00（clockOut < 08:00-grace → 早退）
```

> 🟢 Odoo `hr_attendance` 跨天班次处理（自动识别次日 endTime）。

---

## 五、排班调换（Shift Swap）

### 5.1 排班调换申请（ErpHrShiftSwapRequest）

| 字段 | 含义 |
|------|------|
| id/code/orgId | 标准 |
| requesterId | 申请人（→ErpHrEmployee） |
| targetEmployeeId | 目标员工（→ErpHrEmployee，可选；为空则开放给全员认领） |
| sourceAssignmentId | 申请人的原排班（→ErpHrShiftAssignment） |
| targetAssignmentId | 目标员工的原排班（双人交换时） |
| swapDate | 调换日期 |
| reason | 调换原因 |
| status | 状态 dict `erp-hr/swap-status`：PENDING / APPROVED / REJECTED / CANCELLED |
| approvedById | 审批人（→ErpHrEmployee） |
| 标准审计字段 | |

### 5.2 调换流程

```
员工 A 发起排班调换请求
    │
    ├─► 选择要调换的日期 + 自己的班次
    │
    ├─► 方式 1：指定员工 B（一对一交换）
    │       └─ 选择 B 的某天班次
    │
    ├─► 方式 2：开放给全员（B 认领）
    │       └─ 发布到排班市场，B 可主动匹配
    │
    ├─► 提交 → status = PENDING
    │
    ├─► 审批人（主管/HR）审核
    │       ├─ APPROVED：
    │       │   ├─ A 的 assignment → 替换为 B 的班次
    │       │   ├─ B 的 assignment → 替换为 A 的班次
    │       │   └─ 双方 assignment 记录 swapRequestId
    │       │
    │       └─ REJECTED：退回，排班不变
    │
    └─► 调换完成后，考勤按新排班计算
```

> 🟢 Odoo `hr_attendance.ShiftSwapRequest`（排班调换 + 审批流）。
> 🟢 AureusERP `shift_swap.php`（排班调换管理）。

---

## 六、休假联动

### 6.1 联动规则

```
休假审批通过（LeaveRequest → APPROVED）
    │
    ├─► 系统检索该员工在休假日期范围内的 ErpHrShiftAssignment
    │
    ├─► 自动标记：
    │       ├─ isAbsent = true
    │       ├─ absenceReason = LEAVE
    │       ├─ leaveRequestId = 关联休假
    │       └─ status = ABSENT
    │
    ├─► 考勤模块该日不计算迟到/早退/旷工
    │
    └─► 薪资计算时按休假类型（年假有薪/事假无薪）处理扣款
```

### 6.2 休假取消联动

```
休假取消（LeaveRequest → CANCELLED，原先已 APPROVED）
    │
    └─► 解除排班标记：
            ├─ isAbsent = false
            ├─ absenceReason = null
            ├─ leaveRequestId = null
            └─ status → SCHEDULED（如有打卡数据则 PRESENT）
```

### 6.3 排班与假期余额的关系

| 场景 | 处理 |
|------|------|
| 员工排班日 + 年假审批通过 | 排班标记为休假缺席，扣年假余额 |
| 员工排班日 + 事假审批通过 | 排班标记为休假缺席，扣事假余额（或无薪） |
| 员工排班日 + 病假审批通过 | 排班标记为休假缺席，扣病假余额 |
| 员工休假日（OFF） + 休假申请 | 提醒该日本身是休日，是否需要改为工作日 |

---

## 七、报表与统计

| 指标 | 计算方式 | 用途 |
|------|----------|------|
| 出勤率 | 实际出勤天数 / 应出勤天数 | 部门/个人出勤统计 |
| 迟到率 | 迟到天数 / 应出勤天数 | 纪律管理 |
| 排班覆盖率 | 已排班天数 / 日历天数 | 排班完整性 |
| 调换率 | 调换次数 / 总排班数 | 排班稳定性 |
| 休假占用率 | 休假天数 / 应出勤天数 | 休假模式分析 |

---

## 八、跨域协作

| 对端 | 协作方式 |
|------|---------|
| ErpHrAttendance | 排班作为迟到/早退/缺勤计算的标准输入 |
| ErpHrLeaveRequest | 休假审批通过后自动标记排班为缺席 |
| ErpHrEmployee | 引用员工主数据 |
| payroll（ErpHrSalary） | 考勤数据（迟到/早退/缺勤天数）作为薪资计算输入 |

---

## 九、关键业务规则总结

1. **排班是考勤计算的标准依据**：迟到/早退/缺勤都基于排班而非固定时间
2. **一人一天一排班**：同一员工同一日期只能有一条 ErpHrShiftAssignment
3. **休假自动覆盖排班**：休假审批通过后自动标记排班为缺席，不另外要求 HR 手动操作
4. **排班调换需审批**：任何排班变更通过 swap 流程，不可直接修改 assignment
5. **宽容期内不视为迟到/早退**：由班次模板的 graceLateMinutes/graceEarlyLeaveMinutes 控制
6. **跨天班次日历归属**：夜班排班归属到开始日期，迟到/早退按班次时间计算

## 参考

- `docs/design/human-resource/README.md`（HR 域基础实体）
- `docs/design/human-resource/state-machine.md`（休假/考勤状态机）
- `docs/design/human-resource/payroll.md`（考勤数据引用薪酬计算）
- `docs/design/human-resource/use-cases.md`（UC-HR-06 考勤跟踪）
- 🟢 Odoo `hr_attendance`（排班模板 + 打卡 + 迟到计算 + 排班调换）
- 🟢 AureusERP `shift_template.php`、`shift_swap.php`（排班模板 + 调换）

## 实现偏离补注（plan 2026-07-04-0831-3 落地后追加）

- **轮换组建模**：本期按 Phase 2 Decision 不新增 `ErpHrShiftRotationGroup` 实体、不加 `staggerDays` 列，组成员与错峰天数作为 `generateRotation(patternId, groupMemberIds, staggerDays, dateRange, regenerate)` 的瞬态方法入参传入。「重新生成」时调用方须重传 groupMemberIds/staggerDays（持久化层无法重读组定义）。设计 §3.2 的 RotationGroup 实体作为 successor 候选（触发条件：组定义频繁复用或需多套错峰方案时）。
- **迟到/早退/缺勤结果落点**：本期按 Phase 1 Decision 计算结果写 `ErpHrAttendance` 已有列（lateMinutes/earlyLeaveMinutes/isAbsent），`ErpHrShiftAssignment` 保持「该员工当天应上什么班」的标准输入角色、不加结果列。排班看板若需展示迟到/缺勤须 join Attendance（本期看板可视化属 Non-Goal）。
- **开放给全员认领的排班市场**：本期 Non-Goal，仅支持指定员工一对一交换。开放认领需排班市场匹配机制（独立结果表面），归 follow-up。
- **排班看板/日历 AMIS 可视化**：本期 Non-Goal，本计划只提供数据契约。前端可视化归前端计划。
- **出勤率/调换率/排班覆盖率报表**：本期 Non-Goal，归 follow-up。
- **nop-job 定时提醒/自动生成 cron**：本期手动/外部调度触发；cron 注册归 follow-up。
- **`assignmentDate` 字段查询运算符限制**：XMeta 限制 `assignmentDate` 字段仅支持 `eq/in/dateBetween/dateTimeBetween`，跨日期范围查询统一用 `FilterBeans.dateBetween`。
