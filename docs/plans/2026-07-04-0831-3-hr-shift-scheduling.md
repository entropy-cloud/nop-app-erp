# 2026-07-04-0831-3-hr-shift-scheduling HR 排班管理（班次模板/分配/轮换/调换/迟到早退缺勤/休假联动）

> Plan Status: completed
> Mission: erp
> Work Item: 3.8 HR 排班管理
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.8；`docs/design/human-resource/shift-scheduling.md`
> Related: `2026-07-04-0831-2-hr-payroll-engine-income-tax.md`（薪酬消费考勤数据，同批）、`docs/design/human-resource/state-machine.md`（休假/考勤状态机）
> Audit: required

## Current Baseline

- **排班相关实体已存在**（CRUD `done`）：`ErpHrShift`（`orm.xml:575` 班次模板，含 shiftType/startTime/endTime/graceLateMinutes/graceEarlyLeaveMinutes/requireClockIn/requireClockOut/totalWorkMinutes/allowOvertime）、`ErpHrShiftAssignment`（`:609` 排班分配）、`ErpHrShiftRotationPattern`（`:644` 轮换模板）、`ErpHrShiftSwapRequest`（`:671` 调换申请）。
- **I*Biz 接口与 BizModel 已存在为空壳**：`IErpHrShiftBiz`/`IErpHrShiftAssignmentBiz`/`IErpHrShiftRotationPatternBiz`/`IErpHrShiftSwapRequestBiz`（`erp-hr-dao/.../biz/`）**均已存在**，仅继承 `CrudBizModel` 基类、无自定义方法；对应 `ErpHrShift*BizModel` 同为 CRUD 空壳。本计划**扩展**这些已有接口添加分配/轮换/调换/计算/联动方法，不新建接口。
- **考勤/休假实体已存在**：`ErpHrAttendance`（`:431` clockIn/clockOut，**且已含** `lateMinutes`(`:441`)/`earlyLeaveMinutes`(`:442`)/`isAbsent`(`:443`)）、`ErpHrLeaveRequest`（`:342`，`leave-status` 字典含 APPROVED）、`ErpHrEmployee`（`:194`）、`ErpHrEmploymentContract`（`:303`）。
- **迟到早退计算结果落点（关键裁决依据）**：`ErpHrShiftAssignment`（`orm.xml:609-`）**不含** `lateMinutes`/`earlyLeaveMinutes`/`totalWorkMinutes`/`isSevereLate`；而 `ErpHrAttendance` 已含前三列。`shift-scheduling.md §4.1`+§八 定义**考勤模块**为迟到/早退/缺勤计算归属（读 ShiftAssignment 标准班次 vs 打卡）。故计算结果落 Attendance（已有列），ShiftAssignment 保持"该员工当天应上什么班"的标准输入角色——见 Phase 1 Decision。
- **`ErpHrShiftAssignment.absenceReason`（`:621`）无 `ext:dict`**，`erp-hr/absence-reason` 字典**不存在**；`status` 为自由 VARCHAR（SCHEDULED/PRESENT/ABSENT/CANCELLED，无字典约束）。一人一天一排班唯一性仅 BizModel 校验，无 DB 唯一索引。
- **轮换组建模缺口**：设计 `shift-scheduling.md §3.2` 的 `ErpHrShiftRotationGroup`（members + staggerDays + patternId）**未建模**；`ErpHrShiftRotationPattern.groupId`（`orm.xml:665`）为**自引用**（`ignoreDepends="true"`），**无 `staggerDays` 列**。故轮换组成员与错峰天数无持久化来源——见 Phase 2 Decision。
- **排班是考勤计算的标准依据**（`shift-scheduling.md §九`）：迟到/早退/缺勤基于排班而非固定时间；一人一天一排班；跨天班次（夜班）日历归属开始日期。

## Goals

- 实现**排班分配**：单个分配、批量分配（员工组×日期范围×班次）、复制上期，强制**一人一天一排班**唯一约束（`shift-scheduling.md §九.2`）。
- 实现**轮换排班生成**：按 `ErpHrShiftRotationPattern.patternData`（JSON 序列）+ `startDate` + 轮换组成员 + `staggerDays` 错峰，自动生成日期范围内 `ErpHrShiftAssignment`，支持重新生成（更新已有排班）。
- 实现**迟到/早退/缺勤计算**：读 `ErpHrShiftAssignment` 标准班次 vs `ErpHrAttendance` 实际打卡，按 `graceLateMinutes`/`graceEarlyLeaveMinutes` 计算 lateMinutes/earlyLeaveMinutes/isAbsent；跨天班次（夜班 endTime 次日）正确处理。
- 实现**排班调换审批工作流**：`ErpHrShiftSwapRequest` PENDING→APPROVED/REJECTED/CANCELLED，APPROVED 时双方 assignment 互换班次并记录 swapRequestId；支持指定员工一对一交换。
- 实现**休假联动**：`ErpHrLeaveRequest` APPROVED 时自动标记休假日期范围内 assignment 为缺席（isAbsent=true, absenceReason=LEAVE, leaveRequestId, status=ABSENT）；CANCELLED 时解除标记。

## Non-Goals

- **打卡机/考勤机硬件集成**——attendance 打卡数据来源归 follow-up。
- **加班费自动计算**——加班费由 payroll（计划 `0831-2`）读考勤数据计算，本计划只产出考勤派生字段（totalOvertimeHours 等）。
- **排班看板/日历 AMIS 可视化**——归前端计划；本计划只提供数据契约。
- **开放给全员认领的排班市场（开放调换）**——本期仅指定员工一对一交换；开放认领归 follow-up。
- **排班报表/出勤率/调换率统计**——归 follow-up。
- **nop-job 定时提醒/自动生成 cron**——手动/外部调度触发；cron 注册归 follow-up。

## Task Route

- Type: `implementation-only change`（Phase 1 仅新增 absence-reason 字典 + 绑定；迟到早退数值结果落 ErpHrAttendance 已有列，无 ErpHrShiftAssignment 模型变更——见 Phase 1 Decision）
- Owner Docs: `docs/design/human-resource/shift-scheduling.md`（分配/轮换/迟到早退/调换/休假联动基准）、`docs/design/human-resource/state-machine.md`（休假/考勤状态机）
- Skill Selection Basis: 全阶段 Nop 后端开发——`nop-backend-dev`（BizModel 自定义动作、跨实体 I*Biz 注入 attendance/leave、ErrorCode、事务边界）；Phase 4 测试 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env。
- 配置项经 `AppConfig.var(..., defaultValue)`：`erp-hr.shift-require-approval`（排班调换是否需审批，默认 true）、`erp-hr.shift-default-grace-late-minutes`（默认 15）、`erp-hr.shift-default-grace-early-leave-minutes`（默认 15）、`erp-hr.shift-cross-day-enabled`（默认 true）。
- 无数据迁移；ErpHrShiftAssignment 若补结果列为可空，存量行无影响。

## Execution Plan

### Phase 1 - 字典补齐 + 裁决（结果落点）

Status: completed
Targets: `module-hr/model/app-erp-hr.orm.xml`（新增 `erp-hr/absence-reason` 字典 + ShiftAssignment.absenceReason 绑定）、codegen
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] `Decision`（迟到早退计算结果落点）：**选择** 计算结果写入 `ErpHrAttendance`（已有 lateMinutes/earlyLeaveMinutes/isAbsent 列），`ErpHrShiftAssignment` 保持"标准班次输入"角色不加结果列、不改模型。**替代 A**：在 ShiftAssignment 上冗余结果列（rejected：与 Attendance 重复，违反 §4.1「考勤模块为计算归属」）。**替代 B**：双写（rejected：双源不一致风险）。**残留风险**：排班看板若需迟到/缺勤须 join Attendance（可接受）。此裁决使本期**无 ErpHrShiftAssignment 模型变更**。
  - Skill: `nop-backend-dev`
- [x] `Add`：新增 dict `erp-hr/absence-reason`（LEAVE/LATE_NOT_CLOCKED/OTHER），并给 `ErpHrShiftAssignment.absenceReason`（`orm.xml:621`）加 `ext:dict="erp-hr/absence-reason"` 绑定；codegen 再生成。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpHrErrors.java` 新增排班 ErrorCode（`ERR_SHIFT_DUPLICATE_ASSIGNMENT`/`ERR_SHIFT_CROSS_DAY_INVALID`/`ERR_SHIFT_SWAP_TARGET_OCCUPIED`/`ERR_SHIFT_ROTATION_PATTERN_INVALID`/`ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE`，中文描述）。确认 dict `erp-hr/shift-type`、`erp-hr/swap-status` 已存在（CRUD 已建，`orm.xml:95/100`）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`mvn clean install -DskipTests -pl module-hr -am` BUILD SUCCESS（absence-reason 字典 + 绑定 codegen 通过）。
  - Skill: none

Exit Criteria:

- [x] absence-reason 字典就绪、ShiftAssignment.absenceReason 绑定 codegen 通过；module-hr 单模块 `mvn install -DskipTests` 通过（解除 Phase 2 编译依赖）

### Phase 2 - 排班分配 + 轮换生成

Status: completed
Targets: **扩展**已有 `IErpHrShiftAssignmentBiz`/`IErpHrShiftRotationPatternBiz`（添加 assign*/generateRotation 方法）、`ErpHrShiftAssignmentBizModel`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`（统一 Add-heavy）
- Prereqs: Phase 1

- [x] `Decision`（轮换组与错峰持久化）：**选择** `generateRotation(patternId, groupMemberIds, staggerDays, dateRange)` 将组成员与错峰天数作为**瞬态方法入参**传入，本期不新增 `ErpHrShiftRotationGroup` 实体、不加 `staggerDays` 列。**替代 A**：新增 RotationGroup 实体 + staggerDays 列（rejected：本期过度建模，组定义为生成时参数）。**替代 B**：复用 RotationPattern.groupId 自引用存组（rejected：groupId 语义为组标识非成员列表）。**残留风险**：「重新生成」无法从持久化重读组定义，调用方须重新传入 groupMemberIds/staggerDays。
  - Skill: `nop-backend-dev`
- [x] `Add`：`assignSingle(employeeId, shiftId, assignmentDate)` 与 `assignBatch(employeeIds, shiftId, dateRange)` 与 `copyFromPeriod(sourceRange, targetRange)`——创建 ErpHrShiftAssignment（status=SCHEDULED）；强制一人一天一排班唯一约束，冲突抛 `ERR_SHIFT_DUPLICATE_ASSIGNMENT`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`generateRotation(patternId, groupMemberIds, staggerDays, dateRange)`——按 `ErpHrShiftRotationPattern.patternData`（JSON 序列 of shiftCode）+ `startDate` 循环，按 `staggerDays` 错峰各成员起点，批量生成 assignment；支持 `regenerate`（清旧重生成同范围，组定义由调用方重传）。
  - Skill: `nop-backend-dev`
- [x] `Add`：轮换序列校验——patternData 内 shiftCode 须为有效 ErpHrShift.code；CYCLE_DAYS/CYCLE_WEEKS 按 rotateInterval 循环；空/非法序列抛 `ERR_SHIFT_ROTATION_PATTERN_INVALID`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 单个/批量/复制分配创建 assignment 且一人一天唯一；轮换按 patternData+staggerDays 生成正确序列，regenerate 清旧重生成，非法 pattern 报错（行为测试覆盖）

### Phase 3 - 迟到/早退/缺勤计算（含跨天班次）

Status: completed
Targets: `IErpHrShiftBiz.calcAttendance`、与 `ErpHrAttendance` 跨实体协作
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 2

- [x] `Add`：`calcAttendance(employeeId, assignmentDate)`——读 ErpHrShiftAssignment.shift（startTime/endTime/graceLateMinutes/graceEarlyLeaveMinutes）vs ErpHrAttendance clockIn/clockOut；准时/迟到/严重迟到/早退/缺勤；**计算结果写入 `ErpHrAttendance`**（lateMinutes/earlyLeaveMinutes/isAbsent，已有列），并同步 `ErpHrShiftAssignment.status`（SCHEDULED→PRESENT 或 ABSENT，见 Phase 1 落点 Decision）。
  - Skill: `nop-backend-dev`
- [x] `Add`：跨天班次处理——夜班 endTime < startTime（如 23:00→08:00）时，clockOut 基准取次日 endTime，迟到基准取当日 startTime；日历归属开始日期（`shift-scheduling.md §4.2`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：缺勤标记——有 assignment 且 requireClockIn=true 但无 clockIn → isAbsent=true, status=ABSENT；assignment.leaveRequestId 非空 → isAbsent=true, absenceReason=LEAVE（休假覆盖，不另计旷工）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：跨实体读 attendance 经注入 `IErpHrAttendanceBiz`（同域 I*Biz），不直接 IDaoProvider。替代：直接 DAO（同域但仍优先 I*Biz 统一入口，rejected 直接 DAO）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 迟到/早退/缺勤计算与 `shift-scheduling.md §4.1` 规则表一致；跨天夜班 clockOut 次日基准正确；休假覆盖不计旷工（行为测试覆盖）

### Phase 4 - 排班调换审批 + 休假联动

Status: completed
Targets: `ErpHrShiftSwapRequestBizModel`（审批迁移+互换）、休假联动钩子
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 3

- [x] `Add`：`ErpHrShiftSwapRequest` 状态机 `submit`(→PENDING)/`approve`(PENDING→APPROVED，互换双方 assignment 班次 + 记录 swapRequestId + replacedByAssignmentId)/`reject`(→REJECTED)/`cancel`(→CANCELLED)；非法迁移抛 ErrorCode。
  - Skill: `nop-backend-dev`
- [x] `Add`：调换校验——目标员工目标日须有 assignment（否则抛 `ERR_SHIFT_SWAP_TARGET_OCCUPIED`）；审批后考勤按新排班计算。
  - Skill: `nop-backend-dev`
- [x] `Add`：休假联动 `onLeaveApproved(leaveRequestId)`——检索该员工休假日期范围内 assignment，标记 isAbsent=true/absenceReason=LEAVE/leaveRequestId/status=ABSENT；`onLeaveCancelled` 解除标记（status→SCHEDULED 或按打卡回 PRESENT）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：休假联动触发方式——leave 域审批通过后同步调用 `IErpHrShiftBiz.onLeaveApproved`（同域跨实体同步调用），不引入事件总线（同域无须解耦）。替代：领域事件（同域过度设计，rejected）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 调换审批 APPROVED 后双方 assignment 班次互换且可追溯；休假 APPROVED 标记缺席、CANCELLED 解除（行为测试覆盖）

### Phase 5 - 行为测试与收尾

Status: completed
Targets: `module-hr/erp-hr-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 4

- [x] `Proof`：`TestErpHrShiftScheduling`——一人一天唯一约束、批量/复制/轮换生成（含 staggerDays）、迟到/早退/缺勤/跨天夜班、调换审批互换、休假联动标记与解除。JunitAutoTestCase 覆盖成功/失败模式。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.8 标 done；`shift-scheduling.md` 偏离（开放认领调换/看板可视化/报表/cron Non-Goal）补注。
  - Skill: none

Exit Criteria:

- [x] 排班全行为测试通过（分配/轮换/迟到早退/调换/休假联动各路径）

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0d572afb5ffeIH2fFftvlPbHn5`，独立 general 子代理）。3 BLOCKER：(B1) baseline 虚称「无 IErpHrShiftBiz 接口」——实际 4 个 `IErpHrShift*Biz` 均存在为空壳；(B2) 迟到早退结果落点 Decision 含糊且 baseline 漏 `ErpHrAttendance` 已含 lateMinutes/earlyLeaveMinutes/isAbsent（orm.xml:441-443）——ShiftAssignment 实际不含这些列，「Phase 1 确认」不确定性无正当理由；(B3) 轮换组建模缺口——`ErpHrShiftRotationGroup` 未建模、`groupId` 自引用、无 `staggerDays` 列，generateRotation 无法从持久化读取组/错峰。nits：erp-hr/absence-reason 字典缺失、ErrorCode 拼写、头类型。**已修订**：baseline 改述接口已存在空壳 + 定值列事实 + 轮换组缺口；Phase 1 新增结果落点 Decision（落 Attendance、本期无 ShiftAssignment 模型变更）+ absence-reason 字典；Phase 2 新增轮换组/错峰瞬态入参 Decision + Targets 改「扩展已有接口」；Phase 3 calc 写 Attendance；拼写/头类型修正。
- Independent draft review iteration 2: **accept**（`ses_0d56d9174ffeNgPWXzqeLokvWp`，独立 general 子代理，冷重播）。B1/B2/B3 均确认 RESOLVED（4 接口经仓库核实存在空壳、Attendance 列事实属实、groupId 自引用 + 无 staggerDays 属实、两 Decision 含选择+替代+残留风险）。无新 BLOCKER。2 一致性 nit 已修订：Task Route 去除「微量模型调整」过期对冲（本期无 ShiftAssignment 模型变更）、Phase 1 头类型补 `Proof`（`Decision | Add | Proof`）。baseline spot-check 全 TRUE。Plan Status 置 `active`。

## Closure Gates

- [x] 范围内行为完成（分配 + 轮换 + 迟到早退缺勤 + 调换 + 休假联动）
- [x] 相关文档对齐（`shift-scheduling.md` 偏离补注、roadmap 3.8 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-hr -am`
- [x] 无范围内项目降级为 deferred/follow-up（打卡机集成/加班费/看板可视化/开放认领/报表/cron 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 开放给全员认领的排班市场

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期指定员工一对一交换覆盖核心调换；开放认领需排班市场匹配机制（独立结果表面）。
- Successor Required: yes（触发条件：开放调换/排班市场认领需求时）

### 打卡机硬件集成 / 排班看板 AMIS 可视化 / 出勤率调换率报表 / cron 定时提醒与自动生成

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期手动分配 + 手动触发计算覆盖核心闭环；可视化/报表/定时为增强。
- Successor Required: yes（触发条件：硬件集成/前端看板/报表/定时需求时）

## Closure

Status Note: 全部 5 个 Phase 状态为 completed 且退出标准全 `[x]`；Closure Gates 7/7 全勾；范围内行为（分配/轮换/迟到早退缺勤/调换/休假联动）已落地并通过行为测试；Deferred 项均为计划内 Non-Goal。可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未重用执行者上下文，自检 `tools/mission-driver/src/plan-check.mjs --strict`）
- Warehouse verification (read with grep/glob/read against live repo):
  - Phase 1: dict `erp-hr/absence-reason` 存在于 `module-hr/model/app-erp-hr.orm.xml:166` + `ErpHrShiftAssignment.absenceReason` 绑定 `ext:dict` 于 `:862`；5 排班 ErrorCode（`ERR_SHIFT_DUPLICATE_ASSIGNMENT`/`ERR_SHIFT_CROSS_DAY_INVALID`/`ERR_SHIFT_SWAP_TARGET_OCCUPIED`/`ERR_SHIFT_ROTATION_PATTERN_INVALID`/`ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE`）定义于 `module-hr/erp-hr-service/.../ErpHrErrors.java:81-97`。
  - Phase 2: `assignSingle`/`assignBatch`/`copyFromPeriod` 在 `IErpHrShiftAssignmentBiz.java:29-46` 接口 + `ErpHrShiftAssignmentBizModel.java:52-88` 实现；`generateRotation` 在 `IErpHrShiftRotationPatternBiz.java:38` + `ErpHrShiftRotationPatternBizModel.java:58`（patternData 校验抛 `ERR_SHIFT_ROTATION_PATTERN_INVALID`）。
  - Phase 3: `calcAttendance` 在 `IErpHrShiftBiz.java:35` + `ErpHrShiftBizModel.java:58`；跨天班次计算逻辑在 `scheduling/ShiftAttendanceCalculator.java:88-97`（抛 `ERR_SHIFT_CROSS_DAY_INVALID`），结果写 ErpHrAttendance。
  - Phase 4: 状态机 `submit`/`approve`/`reject`/`cancel` 在 `ErpHrShiftSwapRequestBizModel.java`（approve 互换双方 assignment + replacedByAssignmentId，校验抛 `ERR_SHIFT_SWAP_TARGET_OCCUPIED`/`ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE`）；`onLeaveApproved`/`onLeaveCancelled` 在 `ErpHrShiftBizModel.java:128,144`。
  - Phase 5: `TestErpHrShiftScheduling.java`（13 tests，覆盖成功/失败模式——`ERR_SHIFT_DUPLICATE_ASSIGNMENT` @:87、`ERR_SHIFT_ROTATION_PATTERN_INVALID` @:181）存在。
  - Docs sync: `docs/logs/2026/07-04.md` 已含本计划条目（行 3-28，含验证状态）；`docs/backlog/extended-roadmap.md:32,64` 工作项 3.8 标 ✅ done。
- Anti-Hollow check: 所有 BizModel 方法非空壳——已在测试中通过 `assignmentBiz.assignSingle(...)`/`shiftBiz.calcAttendance(...)`/`shiftBiz.onLeaveApproved(...)`/`rotationBiz.generateRotation(...)` 实际调用并断言（`TestErpHrShiftScheduling.java:80,148,200,375`），运行时可达。
- Five-point consistency: Plan Status=completed / 5 Phase Status=completed / 全 Exit Criteria `[x]` / Closure Gates 7/7 `[x]` / Closure 证据一致。

Follow-up:

- 打卡机硬件集成 / AMIS 排班看板 / 出勤率调换率报表 / nop-job cron 自动生成（均为计划内 Non-Goal，见 Deferred But Adjudicated）。
