# RC MA4 A4.2.17/18/19/20/21 — HR 排班/考勤域异常路径运行时影响面验证（A1.13 §7 SP-1..SP-5）验证报告

> Audit Status: closed
> 里程碑：MA4（运行时行为验证层）
> 工作项：A4.2.17 / A4.2.18 / A4.2.19 / A4.2.20 / A4.2.21（MA4 运行时行为验证 — A1.13 §7 SP-1..SP-5：UC-HR-02⑦ 审批人超时自动转派缺失 + UC-HR-06⑬ 多次打卡 reject + UC-HR-06⑭ 夜班跨天 clockOut 阻断 + UC-HR-06⑮ 设备故障补卡缺失 + UC-HR-09⑲ 换班跨日期语义）
> 验证 plan：`docs/plans/2026-08-07-0530-2-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 Q1 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §去重协议）
> 输入存疑点：A1.13 §7 SP-1..SP-5（`docs/audits/2026-08-02-2344-rc-ma1-a1-13-hr-f2-shift-attendance.md:269-277`）
> 关联既有裁决：A1.13 §5 UC-HR-02 = **P1**（⑦审批人超时自动转派缺失）/ UC-HR-06 = **P1**（⑬多次打卡 reject + ⑭跨天 clockOut + ⑮补卡缺失）/ UC-HR-09 = **接受**（7 验收标准全 PASS）
> 关联同型范式：A4.2.12/A4.2.13/A4.2.14/A4.2.15/A4.2.16（A1.12 §7 族 done — config-gate + 逃生路径可达性范式，本验证对齐其「运行时影响面确认不撤销既有 P1 裁决」判据框架）+ A4.2.5（REJECTED 工单逃生路径可达性 → 决策树分支① 不升 P1 范式，本验证 A4.2.21 watch-only 裁决对齐）
> 关联 finding：`P1-RC-011`（UC-HR-02⑦ 审批人超时自动转派缺失，MR1 todo）+ `P1-RC-012`（UC-HR-06⑬ 多次打卡 reject，MR1 todo）+ `P1-RC-013`（UC-HR-06⑭ 跨天 clockOut，MR1 todo）+ `P1-RC-014`（UC-HR-06⑮ 补卡缺失，MR1 todo）+ `P2-MA2-052`（state-machine.md 缺考勤/排班/换班章节，watch-only）+ `P1-MA2-091`（排班分配 UK，resolved R1.28）
> 验证性质：**只读运行时影响面探查**（下游耦合分析 + 逃生路径可达性 + XMeta 字段级守卫普查 + 调度配置 census；不改代码/ORM/api.xml/view.xml/config 默认值/真相源；方法论 §5 保护区域，roadmap 预授权类目「只读评估」）
> 验证日期：2026-08-07
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **A4.2.17（SP-1 P1-RC-011）** 审批人超时自动转派缺失运行时影响面 | **维持 P1-RC-011**（SUBMITTED 休假悬挂与薪酬核算**完全解耦**——`PayrollCalculator.sumUnpaidLeaveDays:316-332` + `sumUsedDays:187-201` 均仅读 `status=APPROVED`；无 leave-approver-timeout job bean → 无自动转派。运行时影响 = SLA/流程效率类，**不阻塞发薪、不致缺勤误判、不破坏 GL/活跃数据**，Q4 强制实现义务不撤销，降级证据记录指导 MR1 优先级） | 不降级 / 不升 P0 / 归 MR1 |
| **A4.2.18（SP-2 P1-RC-012）** 多次打卡 reject 运行时误判面 | **维持 P1-RC-012**（reject 行为与 L1「以最后一次为准」相反 CONFIRMED；逃生路径 = 标准 CRUD save/update 绕过 reject 守卫；无正式考勤申诉工单机制[grep appeal/申诉/工单/ticket 零命中]。无活跃数据破坏，Q4 强制实现义务不撤销） | 不降级 / 不升 P0 / 归 MR1 |
| **A4.2.19（SP-3 P1-RC-013）** 夜班跨天 clockOut 运行时阻断 | **维持 P1-RC-013**（`ClockOutProcessor:19-20` + `AbstractErpHrAttendanceProcessor.findAttendance:32-38` 按 `eq("date", date)` 查找无跨天回退 CONFIRMED；夜班签退运行时不可用；calc 侧 `isCrossDayShift:24-28` 已实现。逃生路径同 A4.2.18 = 标准 CRUD。无活跃数据破坏，Q4 强制实现义务不撤销） | 不降级 / 不升 P0 / 归 MR1 |
| **A4.2.20（SP-4 P1-RC-014）** 设备故障补卡运行时替代与越权风险 | **维持 P1-RC-014**（无 makeUp/manualClock/补卡 mutation CONFIRMED；逃生路径 = CrudBizModel 标准 save/update；XMeta `_ErpHrAttendance.xmeta` clockIn/clockOut/source **无字段级 auth 守卫** → 越权风险代码层证实。无活跃数据破坏，Q4 强制实现义务不撤销） | 不降级 / 不升 P0 / 归 MR1 |
| **A4.2.21（SP-5 UC-HR-09⑲）** 换班跨日期语义 | **登记 watch-only residual**（`ShiftSwapRequestSubmitProcessor:18-41` 未校验 source/target 同日期 CONFIRMED；跨日期换班经 `ApproveProcessor:31-34` 互换 shiftId 后 assignment 仍合法[UK 满足] + calcAttendance 重算正确，**无活跃数据破坏**；L1 UC-HR-09 `use-cases.md:101` 异常段仅要求「冲突排班拒绝」**未要求同日期校验** → §2 P2① 边界场景弱 + L1 无显式要求 → watch-only 不升 P1） | 登记 watch-only（非 arm-index 新 finding 行，归 §去重协议同型范式 A4.2.5） |
| 新 finding（arm-index 行） | **0**（四项 P1 维持既有 arm-index 行不撤销；A4.2.21 watch-only residual 记本报告非 arm-index 行，对齐 A4.2.5/A4.2.12 范式） | 无新控制点 |
| MR0 触发 | **无**（五项均无 P0：§2 P0①活跃数据破坏 ②安全/隔离 ③核心循环断裂 ④会计过账破坏 均不成立——SUBMITTED 不破坏 GL、reject/跨天/补卡缺口均有标准 CRUD 逃生路径、换班跨日期无数据破坏） | — |

**整体裁决**：A1.13 §7 SP-1..SP-5 五项静态存疑点经下游薪酬耦合分析 + 逃生路径可达性探查 + XMeta 字段级守卫普查 + 调度配置 census **CONFIRMED**：

- **SP-1（A4.2.17）**：`resolveApproverId:203-206` return null + 全 module-hr grep `timeout/escalat/reassign/autoForward` **零业务命中** + 仅 1 个 `.job.yaml`（`erp-hr-contract-expiry.job.yaml`）**无 leave-approver-timeout job**；下游薪酬耦合度分析：`PayrollCalculator.sumUnpaidLeaveDays:316-332` 仅读 `status=APPROVED` 的 SICK/PERSONAL 休假 + `sumUsedDays:187-201` 仅聚合 `status=APPROVED` + `onLeaveApproved` 仅 APPROVED 时触发 → **SUBMITTED 休假悬挂与薪酬核算/余额/考勤缺勤完全解耦** → 运行时影响 = SLA/流程效率类（员工等待审批），不阻塞发薪、不致缺勤误判。**维持 P1-RC-011**（Q4=(a) 异常路径强制实现义务不撤销），降级证据记录指导 MR1 优先级。
- **SP-2（A4.2.18）**：`ErpHrAttendanceClockInProcessor:32-35` reject 守卫 CONFIRMED；逃生路径 = `ErpHrAttendanceBizModel extends CrudBizModel` 标准 save/update mutation 可绕过 reject；全 module-hr grep `appeal/申诉/工单/ticket` **零命中**（无正式申诉工单）。**维持 P1-RC-012**。
- **SP-3（A4.2.19）**：`ErpHrAttendanceClockOutProcessor:19-20` + `AbstractErpHrAttendanceProcessor.findAttendance:32-38` 按 `eq("date", date)` 精确查找无跨天回退 CONFIRMED；calc 侧 `ShiftAttendanceCalculator.isCrossDayShift:24-28` + `calcEarlyLeaveMinutes:61-74` 夜班 endTime 取次日已实现强测覆盖；clocking 侧运行时阻断 = 夜班次日签退查不到记录。逃生路径同 SP-2 = 标准 CRUD。**维持 P1-RC-013**。
- **SP-4（A4.2.20）**：全 module-hr grep `makeUp/manualClock/supplement/adjustClock/补卡` **零命中** CONFIRMED；`ErpHrAttendanceBizModel` 仅 clockIn/clockOut/getTodayAttendance 三方法；XMeta `_ErpHrAttendance.xmeta:36,40` clockIn/clockOut + `:60` source **无字段级 auth 守卫** → CrudBizModel 标准 save/update 可直接修改 clockIn/clockOut 绕过字段守卫（越权风险代码层证实）。**维持 P1-RC-014**。
- **SP-5（A4.2.21）**：`ErpHrShiftSwapRequestSubmitProcessor:18-41` 校验 target 非空但**未校验 source.assignmentDate == target.assignmentDate**；`ApproveProcessor:31-34` 互换 source/target 的 shiftId。跨日期换班（empA 7/1 早班 ↔ empB 7/2 中班）交换后：source(7/1).shiftId=中班、target(7/2).shiftId=早班——assignment 仍合法（UK `(employeeId,assignmentDate,shiftId,delVersion)` 满足，不同员工不同日期）+ calcAttendance 按新 shiftId 重算正确 → **无活跃数据破坏**；L1 `use-cases.md:101` 异常段仅要求「调换申请目标员工已有冲突排班时拒绝」（= 同员工同日冲突），**未要求同日期校验** → §2 P2① 边界场景弱（主路径换班审批完整 + 无数据破坏，仅语义可疑）。**登记 watch-only residual**（非 arm-index 新 finding 行，归 §去重协议同型范式 A4.2.5「REJECTED 工单逃生路径无数据破坏 → 不升 P1」）。

按 §2 分级判据三源复核五项均无 P0 升级、四项 P1 维持不降级、A4.2.21 watch-only：①**§2 P0①④活跃数据/GL 破坏**对五项均不成立（SUBMITTED 解耦 + reject/跨天/补卡缺口均有逃生路径 + 换班跨日期无数据破坏）；②**§2 P1②异常路径未实现**对 SP-1/2/3/4 成立（Q4=(a) 强制实现义务不撤销）；③**§2 P2①边界场景弱**对 SP-5 成立（L1 无显式同日期要求 + 无数据破坏）。

**不触发 MR0，无新 arm-index finding 行（四项 P1 维持既有行 + A4.2.21 watch-only residual 记报告），维持 A1.13 §5 既有裁决（UC-HR-02 P1 / UC-HR-06 P1 / UC-HR-09 接受）。** 本验证**不实施 P1 修复或同日期守卫**（plan Non-Goals），仅本报告落盘 + roadmap/log/arm-index 注记同步。

> **与 A1.12 §7 族方向一致性声明**：本验证同属 HR 域 MA4 运行时影响面确认范式。A4.2.12/A4.2.13（config-gate 部署普查）+ A4.2.14/A4.2.15（config-gate 部署普查）+ A4.2.16（逃生路径可达性）+ 本验证 A4.2.17-21（异常路径运行时影响面）均为「只读探查 → 维持既有裁决 + 0 新 arm-index finding」范式。本验证 SP-2/3/4 逃生路径可达性对齐 A4.2.16（逃生路径可达但运营完整性部分满足）；SP-5 watch-only 裁决对齐 A4.2.5（REJECTED 工单逃生路径无数据破坏 → watch-only 不升 P1）。

---

## 1. 输入存疑点原文 + L1/L2/L3 锚点

### 1.1 输入存疑点原文（A1.13 §7 SP-1..SP-5，逐字引用）

> **SP-1（P1-RC-011 运行时触发面）**：审批人超时自动转派缺失在运行时的实际影响面——SUBMITTED 休假长期未审批的累积量、是否影响薪酬核算（UC-HR-04 缺勤数据来源）。MA4 A4.2 运行时确认 SUBMITTED 悬挂量与薪酬核算的耦合度。
>
> **SP-2（P1-RC-012 多次打卡 reject 运行时误判面）**：员工误触多次 clockIn 被拒后的逃生路径（HR 手工 DB 修正频度）、是否存在考勤申诉工单。MA4 A4.2 运行时确认 reject 行为的运营影响。
>
> **SP-3（P1-RC-013 夜班跨天 clockOut 运行时阻断）**：夜班员工实际 clockOut 失败率、是否普遍存在「夜班次日补录」的临时运维流程。MA4 A4.2 运行时确认夜班占比与 clockOut 阻断频度。
>
> **SP-4（P1-RC-014 设备故障补卡运行时替代）**：设备故障时 HR 是否经标准 CRUD 直接 update ErpHrAttendance.clockIn/clockOut 绕过（CrudBizModel 默认 save/update 不受字段级守卫保护），存在越权风险。MA4 A4.2 运行时确认补卡替代路径与权限。
>
> **SP-5（UC-HR-09⑲ 换班跨日期语义）**：`ErpHrShiftSwapRequestSubmitProcessor` 未校验 source/target assignment 同日期，跨日期换班的运行时语义（empA 7/1 早班 ↔ empB 7/2 中班 → 交换后 empA 7/1 上中班 / empB 7/2 上早班，语义可疑）。L1 未显式要求同日期校验，但运行时可能产生困惑。MA4 A4.2 确认是否需补同日期守卫。
> — `docs/audits/2026-08-02-2344-rc-ma1-a1-13-hr-f2-shift-attendance.md:269-277`

### 1.2 A1.13 §5 既有裁决（输入，本验证复核分层一致性）

| UC | A1.13 §5 裁决 | 关键断言 | 本验证复核对象 |
|----|--------------|---------|---------------|
| UC-HR-02 | **P1** | ⑦审批人超时自动转派 = **未实现**（`resolveApproverId:203-206` return null + 无 timeout/escalat 业务代码 + 无 leave-timeout job） | SP-1：运行时影响面（SUBMITTED 悬挂 vs 薪酬耦合度） |
| UC-HR-06 | **P1** | ⑬多次打卡 = **行为相反**（reject 而非 last-wins）/ ⑭跨天打卡 = **clocking 侧未实现**（夜班跨天 clockOut 查不到记录）/ ⑮手工补卡 = **未实现** | SP-2：reject 逃生路径 + 申诉工单 / SP-3：夜班 clockOut 阻断 + 补录运维 / SP-4：补卡替代路径 + 越权风险 |
| UC-HR-09 | **接受** | ⑲目标员工冲突拒绝 + ㉑重复排班拦截全 PASS | SP-5：换班跨日期语义（是否需补同日期守卫） |

本验证**不重复核实**断言代码逻辑（A1.13 §5 已证 + A2.7b 状态机复用 pass + A4.4 代码质量复用 pass），只评运行时影响面差异（下游耦合度 + 逃生路径 + 字段守卫 + 调度 census + 跨日期数据后果）。

### 1.3 L1 需求契约（逐字）

**UC-HR-02⑦**（`use-cases.md:24` 异常段）：「审批人超时自动转派」——L1 显式声明异常路径。

**UC-HR-06⑬⑭⑮**（`use-cases.md:72` 异常段）：「多次打卡以最后一次为准；跨天打卡处理；设备故障时支持手工补卡」——L1 显式声明三项异常路径。

**UC-HR-09⑲**（`use-cases.md:101` 异常段）：「调换申请目标员工已有冲突排班时拒绝」——L1 **仅要求冲突排班拒绝**（= 同员工同日冲突），**未显式要求 source/target 同日期校验**。

### 1.4 L2 owner doc 契约（设计参考，非真相源）

- `state-machine.md:51` §适用对象一 休假 §4 异常：逐字「审批人长期不处理 | 超时自动转上级或代班人（可配置）」——与 L1⑦一致要求超时自动转派。
- `shift-scheduling.md §4.2:201-212`：逐字「夜班（23:00-08:00）等跨天班次…自动识别次日 endTime」——L2 要求跨天处理（calc 侧已实现，clocking 侧未实现 = A1.13 §5 已裁决）。
- `shift-scheduling.md §5.1/§5.2`：换班调换设计——**未显式要求同日期校验**（§5.2 仅述「一对一交换班次」）。

### 1.5 L3 实现锚点（live repo 实测，本验证复核）

| 组件 | file:line（写时实测） | 行为断言 |
|------|----------------------|----------|
| 审批人解析（SP-1） | `module-hr/erp-hr-service/.../entity/ErpHrLeaveRequestBizModel.java:203-206` | `resolveApproverId` 注释自承「非关键——仅记录审批轨迹」直接 `return null` |
| 调度 job census（SP-1） | `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml`（唯一）+ `scheduler.yaml:1`（`enabled: true`） | 全仓仅 1 个 hr `.job.yaml`（合同到期），**无** leave-approver-timeout job |
| 无薪假聚合（SP-1 下游耦合） | `module-hr/erp-hr-service/.../payroll/PayrollCalculator.java:316-332` | `sumUnpaidLeaveDays` `eq("status", APPROVED)` + `in("leaveType", [SICK,PERSONAL])`——**仅读 APPROVED**，SUBMITTED 不计 |
| 余额已用聚合（SP-1 下游耦合） | `module-hr/erp-hr-service/.../entity/ErpHrLeaveRequestBizModel.java:187-201` | `sumUsedDays` `eq("status", APPROVED)`——**仅聚合 APPROVED**，SUBMITTED 不扣余额 |
| 出勤聚合（SP-1 下游耦合） | `module-hr/erp-hr-service/.../payroll/PayrollCalculator.java:282-310` | `summarizeAttendance` 读 ErpHrAttendance `isAbsent` + `workHours`，**不读 LeaveRequest**，SUBMITTED 休假不影响出勤统计 |
| 休假联动（SP-1 下游耦合） | `module-hr/erp-hr-service/.../processor/ErpHrLeaveRequestApproveProcessor.java` + `ErpHrShiftOnLeaveApprovedProcessor` | `onLeaveApproved` 仅 APPROVED 时触发排班标记缺席，SUBMITTED 不触发 |
| clockIn reject 守卫（SP-2） | `module-hr/erp-hr-service/.../processor/ErpHrAttendanceClockInProcessor.java:32-35` | `if (attendance.getClockIn() != null) throw new NopException(ERR_ALREADY_CLOCKED_IN)`——**首次签到后拒绝重复打卡** |
| clockOut 查找键（SP-3） | `module-hr/erp-hr-service/.../processor/ErpHrAttendanceClockOutProcessor.java:19-20` + `AbstractErpHrAttendanceProcessor.findAttendance:32-38` | `findAttendance(employeeId, today)` + `eq("date", date)` 精确查找，**无跨天回退**（查 date=yesterday） |
| 跨天判定（SP-3 calc 侧） | `module-hr/erp-hr-service/.../scheduling/ShiftAttendanceCalculator.java:24-28,61-74` | `isCrossDayShift`（endTime < startTime）+ `calcEarlyLeaveMinutes` 夜班 endTime 取次日——calc 侧已实现强测覆盖 |
| Attendance BizModel 方法集（SP-4） | `module-hr/erp-hr-service/.../entity/ErpHrAttendanceBizModel.java:38-102` | `extends CrudBizModel<ErpHrAttendance>` + 仅 clockIn/clockOut/getTodayAttendance 三 mutation/query，**无 makeUp mutation**；标准 save/update 继承可用 |
| Attendance XMeta 字段守卫（SP-4） | `module-hr/erp-hr-meta/src/main/resources/_vfs/erp/hr/model/ErpHrAttendance/_ErpHrAttendance.xmeta:36,40,60` | clockIn/clockOut/source prop **无 `auth` 属性**——无字段级守卫，CrudBizModel save/update 可直接写 |
| 换班提交（SP-5） | `module-hr/erp-hr-service/.../processor/ErpHrShiftSwapRequestSubmitProcessor.java:18-41` | 校验 target 非空（`ERR_SHIFT_SWAP_TARGET_OCCUPIED`）但**未校验 source.assignmentDate == target.assignmentDate** |
| 换班审批互换（SP-5） | `module-hr/erp-hr-service/.../processor/ErpHrShiftSwapRequestApproveProcessor.java:31-34` | 互换 source/target 的 `shiftId` + 记录 swapRequestId/replacedByAssignmentId 双向追溯 + 重置 SCHEDULED |

---

## 2. Phase 1 — 运行时影响面证据采集

### 2.1 A4.2.17 审批人超时自动转派缺失运行时影响面（SP-1）

#### 2.1.1 静态判定复核（A1.13 §6 已证）

- `ErpHrLeaveRequestBizModel.resolveApproverId:203-206` 注释「审批人取当前用户关联的员工记录（非关键——仅记录审批轨迹）」直接 `return null`——CONFIRMED。
- 全 module-hr grep `timeout|escalat|reassign|autoForward` 跨 `*.java` **零业务命中**（仅 session/lock/transaction timeout 基础设施，grep `leave-approver-timeout|leaveApproverTimeout` 零命中）。
- 调度 census：全仓 `.job.yaml` 仅 1 个 hr 作业 `erp-hr-contract-expiry.job.yaml`（UC-HR-07 合同到期）；`scheduler.yaml:1` 仅 `enabled: true` 无 leave-approver-timeout bean——CONFIRMED 无自动转派/催办机制。

#### 2.1.2 下游薪酬核算耦合度分析（SP-1 核心）

**SP-1 关键问题**：SUBMITTED 休假长期未审批是否影响 UC-HR-04 薪酬核算缺勤数据来源？

逐消费点普查（live code 实测）：

| 薪酬核算消费点 | file:line | 读取的 LeaveRequest 状态 | SUBMITTED 是否影响 |
|--------------|-----------|--------------------------|-------------------|
| 无薪假扣减 | `PayrollCalculator.sumUnpaidLeaveDays:316-332` | `eq("status", APPROVED)` + `in("leaveType", [SICK,PERSONAL])` | **否**——仅 APPROVED 的 SICK/PERSONAL 计入无薪假，SUBMITTED 不计 |
| 休假余额已用 | `ErpHrLeaveRequestBizModel.sumUsedDays:187-201` | `eq("status", APPROVED)` | **否**——仅 APPROVED 扣减余额，SUBMITTED 不扣（`checkLeaveBalance:133-153` 余额检查在 submit 时算的是已 APPROVED 占用，submit 本身的 SUBMITTED 不计入） |
| 出勤缺勤判定 | `PayrollCalculator.summarizeAttendance:282-310` | 读 `ErpHrAttendance.isAbsent`（**不直接读 LeaveRequest**） | **否**——attendance.isAbsent 由 `ErpHrShiftCalcAttendanceProcessor` 计算，仅 APPROVED 休假经 `onLeaveApproved` 标记 assignment.isAbsent |
| 排班缺席联动 | `ErpHrShiftOnLeaveApprovedProcessor`（经 `ErpHrLeaveRequestApproveProcessor:20-31` 触发） | 仅 APPROVED 时触发 | **否**——SUBMITTED 不触发 `onLeaveApproved`，排班不标记缺席 |

**耦合度裁决**：**SUBMITTED 休假悬挂与薪酬核算/余额/考勤缺勤完全解耦**。SUBMITTED 状态的休假既不计入无薪假扣减（不影响发薪金额），也不扣减休假余额（不影响余额可用性），也不触发排班/考勤缺席标记（不影响出勤统计）。**SUBMITTED 长期悬挂的唯一运行时影响 = 员工等待审批的 SLA 延迟（流程效率类），不阻塞发薪、不致缺勤误判、不破坏 GL/活跃数据**。

#### 2.1.3 降级证据 vs Q4 强制实现义务

- **降级证据**：SUBMITTED 与薪酬解耦 → 运行时影响 = SLA/流程效率类（员工等待），非活跃数据破坏、非核心循环断裂。此证据**可指导 MR1 修复优先级排序**（相比影响 GL 的 P1 优先级更低），并证实"不触发 MR0"（无 P0）。
- **Q4 强制实现义务不撤销**：Q4 裁决=(a) P0/P1 必须实现禁止方案 B 无例外。L1⑦ + L2 `state-machine.md:51` 一致要求超时自动转派，L3 完全缺失 → §2 P1② 异常路径未实现成立。运行时影响面证据**不改变 P1 分级**（仅指导优先级），修复义务仍归 MR1（纯 BizModel + scheduler 接线 + config key 预授权自动执行，不触 §5 ask-first）。

**结论**：**维持 P1-RC-011**（Q4 强制实现义务不撤销，降级证据记录指导 MR1 优先级）。不升 P0（SUBMITTED 解耦无活跃数据破坏）、不降 P2（L1 异常路径明确要求）、不撤销 arm-index 行。

### 2.2 A4.2.18 多次打卡 reject 运行时误判面（SP-2）

#### 2.2.1 静态判定复核

`ErpHrAttendanceClockInProcessor:32-35` `if (attendance.getClockIn() != null) throw new NopException(ERR_ALREADY_CLOCKED_IN)`——CONFIRMED reject 行为与 L1⑬「以最后一次为准」相反。测试与实现同步偏离：`TestErpHrAttendanceEngine#testDuplicateClockInBlocked` + E2E `hr-leave-attendance` 均断言 reject（A1.13 §3 已证）。

#### 2.2.2 逃生路径可达性 + 申诉工单 census

**逃生路径**（员工误触多次 clockIn 被拒后如何修正）：

| 路径 | mutation | file:line | 可达性 |
|------|----------|-----------|--------|
| 标准 CRUD save/update | CrudBizModel 继承的 `save`/`update` | `ErpHrAttendanceBizModel extends CrudBizModel:38` | **可达**——HR 持 ErpHrAttendance 写权限者可经标准 save/update 直接修改 `clockIn` 字段，绕过 reject 守卫（与 SP-4 越权风险同根因） |

**申诉工单 census**：全 module-hr grep `appeal|申诉|工单|ticket` **零业务命中**（exit code 1）——**无正式考勤申诉工单机制**。员工误触 clockIn 被拒后须线下联系 HR 经标准 CRUD 修正。

#### 2.2.3 运行时误判面评估

- **误判场景**：员工首次 clockIn 后误以为未签到再次 clockIn → reject `ERR_ALREADY_CLOCKED_IN`。此时首次 clockIn 时间戳已记录（`:36` `setClockIn(currentTimestamp)` 在 reject 守卫之后，故 reject 时 clockIn 已是首次值），数据完整。
- **运营影响**：reject 不破坏数据（首次 clockIn 已存），仅造成员工困惑 + HR 介入修正的运营开销。无活跃数据破坏。

**结论**：**维持 P1-RC-012**（行为与 L1 字面相反，Q4 强制实现义务不撤销）。逃生路径 = 标准 CRUD（与 SP-4 越权风险同根因，归 P1-RC-014 一并修复 makeUp mutation 时覆盖）；无正式申诉工单。不升 P0（reject 不破坏数据）、不降级（L1⑬ 字面要求 last-wins）。

### 2.3 A4.2.19 夜班跨天 clockOut 运行时阻断（SP-3）

#### 2.3.1 静态判定复核

- `ErpHrAttendanceClockOutProcessor:19-20` `findAttendance(employeeId, today)`——CONFIRMED 按 clockOut 当日查找。
- `AbstractErpHrAttendanceProcessor.findAttendance:32-38` `eq("date", date)` 精确匹配——**无跨天回退**（查 date=yesterday 或 shift-driven 归属日期）。
- calc 侧 `ShiftAttendanceCalculator.isCrossDayShift:24-28`（endTime < startTime）+ `calcEarlyLeaveMinutes:61-74` 夜班 endTime 取 `assignmentDate.plusDays(1)`——calc 侧跨天已实现强测覆盖（testCalcAttendanceCrossDayNightShift/EarlyLeave）。
- 运行时场景：夜班 23:00 Mon 签到（date=Mon）、08:00 Tue 签退 → clockOut 查 date=Tue → null → 抛 `ERR_NOT_CLOCKED_IN`。

#### 2.3.2 逃生路径 + 补录运维 census

**逃生路径**（夜班员工 clockOut 失败后如何补签退）：同 SP-2 = 标准 CRUD save/update 直接修改 attendance.clockOut（HR 持写权限者经 CrudBizModel 继承 mutation）。

**补录运维 census**：全 module-hr grep `补录|nextDayMakeUp|crossDayClockOut` **零业务命中**——**无「夜班次日补录」自动化临时运维流程**，全靠 HR 手工标准 CRUD。

#### 2.3.3 运行时阻断面评估

- **阻断场景**：夜班（跨天）员工次日签退时功能不可用（抛 ERR_NOT_CLOCKED_IN）。
- **运营影响**：clockIn 记录已存（date=Mon），仅 clockOut 缺失 → workHours 无法计算（`computeWorkHours:94-100` clockIn/clockOut 任一为 null 返回 0）→ 当日工时少计。但 calcAttendance 按排班计算迟到/早退仍正确（calc 侧跨天已实现）。无活跃数据破坏（clockIn 完整，clockOut 缺失可后补）。

**结论**：**维持 P1-RC-013**（clocking 侧跨天未实现，Q4 强制实现义务不撤销）。逃生路径 = 标准 CRUD（与 SP-2/SP-4 同根因）；无补录自动化流程。不升 P0（功能不可用非数据破坏）、不降级（L1⑭ + L2 §4.2 要求跨天处理）。修复方案 A（clockOut 查找逻辑增强：date=today 无记录回退查 date=yesterday 且夜班 assignment）纯 BizModel 预授权自动执行。

### 2.4 A4.2.20 设备故障补卡运行时替代与越权风险（SP-4）

#### 2.4.1 静态判定复核

- 全 module-hr grep `makeUp|manualClock|supplement|adjustClock|补卡` **零业务命中**——CONFIRMED 无手工补卡 mutation。
- `ErpHrAttendanceBizModel:38-102` 仅 clockIn/clockOut/getTodayAttendance 三方法（+ 继承 CrudBizModel 标准 CRUD）。

#### 2.4.2 标准 CRUD 绕过 + 字段级守卫普查（SP-4 核心）

**SP-4 关键问题**：HR 是否经标准 CRUD 直接 update `ErpHrAttendance.clockIn/clockOut` 绕过字段守卫？普通员工是否也能改自己打卡？

**字段级守卫 census**（`_ErpHrAttendance.xmeta` live 实测）：

| 字段 | file:line | `auth` 属性 | 字段级守卫 |
|------|-----------|------------|-----------|
| clockIn | `_ErpHrAttendance.xmeta:36` | **无** | **无字段级守卫** |
| clockOut | `_ErpHrAttendance.xmeta:40` | **无** | **无字段级守卫** |
| source | `_ErpHrAttendance.xmeta:60` | **无** | **无字段级守卫** |

**越权风险裁决**（代码层证实）：CrudBizModel 标准 save/update mutation 经 RBAC 实体级权限控制（`app.action-auth.xml` ErpHrAttendance 写权限），但**无字段级 auth 守卫** → 持 ErpHrAttendance 写权限者（含 HR 角色）可经标准 save/update 直接修改 clockIn/clockOut/source 字段，绕过 clockIn/clockOut Processor 的 reject/cross-day 守卫。**越权风险代码层证实**（XMeta 无字段级 auth → 无 field-level protection）。

**普通员工越权评估**：clockIn/clockOut mutation 以 `employeeId` 为入参（非上下文 derive），标准 CRUD save/update 的权限由 RBAC 实体级控制——若普通员工被授予 ErpHrAttendance 写权限（非默认，需显式授权），理论上可改任意员工打卡（无行级/org 级隔离过滤 employeeId）。但默认 RBAC 配置下普通员工仅持 clockIn/clockOut 自助 mutation 权限，不持标准 CRUD save/update 权限——故越权风险**限于 HR 角色经标准 CRUD 绕过字段守卫**（非默认普通员工可越权）。

#### 2.4.3 运行时替代路径评估

- **替代路径**：设备故障时 HR 经标准 CRUD save/update 直接补 clockIn/clockOut（绕过字段守卫）——代码可达但**无 source=MANUAL 标记 + 无 reason 必填 + 无审计字段写回**（补卡记录与正常打卡混同，无法区分）。
- **运营影响**：补卡数据完整性部分满足（时间值可补），但缺审计可追溯性（无 reason/操作人/手动标记）。

**结论**：**维持 P1-RC-014**（手工补卡 mutation 完全缺失，Q4 强制实现义务不撤销）。越权风险代码层证实（XMeta 无字段级 auth）。逃生路径 = 标准 CRUD（绕过字段守卫，无审计标记）。不升 P0（无活跃数据破坏，补卡经标准 CRUD 可达但缺审计）、不降级（L1⑮ 要求手工补卡 mutation）。修复义务 = 新增 makeUpClockIn/makeUpClockOut mutation（HR 角色守卫 + source=MANUAL + reason 必填 + 审计字段），纯 BizModel 预授权自动执行。

### 2.5 A4.2.21 换班跨日期语义运行时确认（SP-5）

#### 2.5.1 静态判定复核

`ErpHrShiftSwapRequestSubmitProcessor:18-41`：
- `:20-23` 加载 source/target assignment（`requireEntity`）
- `:25-28` 校验 target 非空（`ERR_SHIFT_SWAP_TARGET_OCCUPIED`）
- `:29-39` 新建 PENDING swapRequest（`setSourceAssignmentId` + `setTargetAssignmentId` + `setSwapDate(source.assignmentDate)`）
- **未校验 source.assignmentDate == target.assignmentDate**——CONFIRMED。

#### 2.5.2 跨日期换班运行时数据后果分析

**跨日期换班场景**：empA 7/1 早班（source.assignmentDate=7/1, shiftId=早班）↔ empB 7/2 中班（target.assignmentDate=7/2, shiftId=中班）。

经 `ErpHrShiftSwapRequestApproveProcessor:31-34` 互换 shiftId 后：
- source(7/1).shiftId = 中班（原 empB 的）
- target(7/2).shiftId = 早班（原 empA 的）

**数据后果逐项核验**：

| 维度 | 后果 | 是否数据破坏 |
|------|------|-------------|
| UK 完整性 | UK `(employeeId,assignmentDate,shiftId,delVersion)`（`app-erp-hr.orm.xml:1210`，P1-MA2-091 R1.28）——source 行 (empA,7/1,中班) + target 行 (empB,7/2,早班) 仍满足 UK（不同员工不同日期） | **否** |
| assignment 合法性 | 两 assignment 均有效（status 重置 SCHEDULED `:41-42`，shiftId 指向有效 Shift） | **否** |
| calcAttendance 重算 | `ErpHrShiftCalcAttendanceProcessor` 按新 shiftId 重算迟到/早退/缺勤——source(7/1) 按中班标准、target(7/2) 按早班标准，calc 侧正确 | **否** |
| 语义合理性 | empA 7/1 上中班（原 empB 班次）、empB 7/2 上早班（原 empA 班次）——**非真正「同日互换」**，但双方各上对方一个班次，无遗漏/重复 | **否（语义可疑但无数据破坏）** |
| 双向追溯 | swapRequestId + replacedByAssignmentId 双向记录 `:36-39`——审计可追溯 | **否** |

**裁决**：跨日期换班**无活跃数据破坏**（assignment 合法 + UK 满足 + calc 重算正确 + 审计可追溯）。语义上非真正「同日互换」（empA 在 7/1 上中班而非 empB 的 7/2 中班），但 L1 UC-HR-09 `use-cases.md:101` 异常段**仅要求「调换申请目标员工已有冲突排班时拒绝」**（= 同员工同日冲突），**未显式要求 source/target 同日期校验**。

#### 2.5.3 §2 判据裁决（决策树）

按 plan 要求的决策树：跨日期换班是否致活跃数据破坏 → 否 → watch-only 不升 P1。

- **§2 P0①活跃数据破坏**：✗（assignment 合法 + calc 重算正确 + 无 GL/库存破坏）
- **§2 P1②异常路径未实现**：✗（L1 未要求同日期校验，非异常路径缺失；换班审批主路径完整 = UC-HR-09⑱接受）
- **§2 P2①边界场景弱**：**✓**（主路径换班审批完整 + 无数据破坏，仅跨日期换班语义可疑——empA/empB 各上对方一个班次但非同日互换，边界场景弱）
- **§2 接受**：部分成立（UC-HR-09⑲目标员工冲突拒绝 + ㉑重复排班拦截主路径 PASS），但跨日期语义可疑 = 边界场景弱

**裁决**：**登记 watch-only residual**（§2 P2① 边界场景弱）。L1 未要求同日期校验 → 不构成 P1 异常路径缺失；无活跃数据破坏 → 不升 P1/P0。watch-only residual 记本报告（非 arm-index 新 finding 行，归 §去重协议同型范式 A4.2.5「REJECTED 工单逃生路径无数据破坏 → watch-only 不升 P1」）。修复建议（归 successor/部署决策，非 MR1 强制）= `SubmitProcessor` 增 `source.assignmentDate == target.assignmentDate` 守卫 + `ERR_SHIFT_SWAP_CROSS_DATE` 错误码（纯 BizModel 预授权，但 L1 未要求故不归 MR1 强制）。

---

## 3. Phase 1 Exit Criteria 复核

- [x] **验证报告落盘 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`，含五项存疑点各自裁决 + file:line 证据 + §2 判据命中分支**：本文件 §0/§1/§2 含五项裁决（P1-RC-011/012/013/014 维持 + A4.2.21 watch-only）+ 全 file:line 证据（§1.5 表 + §2.1-2.5 逐项）+ §2 判据命中分支（§5.1）。
- [x] **每项裁决明确：维持 P1（Q4 强制实现，不降级）+ 降级证据记录，或升级触发 MR0；UC-HR-09⑲ watch-only（若有）已按 §去重协议裁决**：四项 P1 维持（§5.1，Q4 不撤销 + 降级证据指导 MR1 优先级）；A4.2.21 watch-only 经 §去重协议裁决**不新建 arm-index 行**（归同型范式 A4.2.5，§5.2）；无升级 MR0（五项均无 P0）。

---

## 4. 多维度审计（`docs/skills/multi-dimensional-audit-prompt.md`）

按多维审计提示要求，对每个维度至少给出一句裁决：

| 维度 | 裁决 |
|------|------|
| **需求正确性** | L1 UC-HR-02:24⑦ + UC-HR-06:72⑬⑭⑮ 三项异常路径明确要求（实现缺失/相反 → P1 维持）；L1 UC-HR-09:101⑲**仅要求冲突拒绝未要求同日期校验**（→ A4.2.21 watch-only 非 P1）。无「承诺但没有证据」项（五项均有 file:line 证据 + 下游耦合分析）。 |
| **owner-doc 对齐** | L2 `state-machine.md:51`⑦ + `shift-scheduling.md §4.2`⑭ 与 L1 一致要求；L2 `shift-scheduling.md §5.1/§5.2` 换班**未要求同日期校验**（与 A4.2.21 watch-only 裁决一致）。无 owner doc 与 L1 冲突。 |
| **架构或边界影响** | 无新跨模块依赖 / API 契约变更 / 保护区域触碰。本验证零代码变更，五项均为既有机制的影响面确认。 |
| **验证充分性** | 下游耦合假设可证伪：若 SUBMITTED 计入无薪假/余额，§2.1 grep 会命中 status=SUBMITTED（实测全 APPROVED）；逃生路径不可达假设可证伪：若 CrudBizModel save/update 不暴露，§2.2/2.3/2.4 会发现（实测标准 CRUD 可达）；字段守卫假设可证伪：若 XMeta clockIn/clockOut 有 auth 属性，§2.4 grep 会命中（实测无 auth）。 |
| **回归风险** | 本验证零代码变更（只读评估 + 文档更新），无脆弱路径引入。SUBMITTED 解耦 + 标准 CRUD 逃生路径是稳定基线。 |
| **路由和技能选择正确性** | 任务路由 = verification or audit work（只读评估），Skill = `multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。匹配。换路由无遗漏。 |
| **待办或自主权策略漂移** | 范围未无声扩大；四项 P1 维持不降级（Q4 强制实现义务不撤销），A4.2.21 watch-only residual 是验证**输出**非范围内项目降级（plan Deferred But Adjudicated 正确分类 P1 修复义务归 MR1）。 |
| **view.xml gen-control 契约**（项目特定维度） | 不适用——本验证对象是后端运行时影响面（调度/下游耦合/逃生路径/字段守卫），不触及 delta view 前端层。本维度无发现。 |

**反窄化自检**：本验证覆盖 7 维度（需求/owner-doc/架构/验证充分性/回归风险/路由/待办漂移）+ 1 项目特定维度（view.xml，不适用），非单维深挖。每个维度已给出裁决。

---

## 5. Phase 2 — 五项存疑点裁决（§2 判据 + finding 衔接）

### 5.1 §2 判据复核（五项）

| 存疑点 | §2 接受 | §2 P0①④ | §2 P1①② | §2 P2① | 最终裁决 |
|--------|--------|---------|---------|--------|---------|
| A4.2.17（审批人超时） | ✗（⑦异常路径未实现） | ✗（SUBMITTED 与薪酬解耦无活跃数据破坏） | **✓**（§2 P1② 异常路径未实现——resolveApproverId return null + 无 leave-timeout job） | ✗（L1 明确要求非边界弱） | **维持 P1-RC-011**（降级证据记录：SUBMITTED 解耦指导 MR1 优先级） |
| A4.2.18（多次打卡 reject） | ✗（⑬行为相反） | ✗（reject 不破坏数据，首次 clockIn 已存） | **✓**（§2 P1① 行为实质偏离 + §2 P1② 异常路径） | ✗（L1 明确要求 last-wins） | **维持 P1-RC-012** |
| A4.2.19（跨天 clockOut） | ✗（⑭clocking 侧未实现） | ✗（clockIn 完整，clockOut 缺失可后补非数据破坏） | **✓**（§2 P1② 异常路径未实现 clocking 侧） | ✗（L1+L2 §4.2 明确要求跨天） | **维持 P1-RC-013** |
| A4.2.20（补卡缺失） | ✗（⑮补卡 mutation 缺失） | ✗（标准 CRUD 逃生路径可达非活跃数据破坏） | **✓**（§2 P1② 异常路径未实现——无 makeUp mutation） | ✗（L1 明确要求手工补卡） | **维持 P1-RC-014**（越权风险代码层证实） |
| A4.2.21（换班跨日期） | 部分（⑲冲突拒绝主路径 PASS） | ✗（跨日期换班无数据破坏 + calc 重算正确） | ✗（L1 未要求同日期校验，非异常路径缺失） | **✓**（§2 P2① 边界场景弱——主路径换班审批完整 + 无数据破坏，仅语义可疑） | **登记 watch-only residual**（非 arm-index 新行） |

**取最高原则**：A4.2.17/18/19/20 仅 §2 P1 成立 → 维持 P1；A4.2.21 仅 §2 P2① 成立 → watch-only。

### 5.2 finding 衔接裁决（§7 复用 or 新增）

| 既有 arm-index 行 | 控制点 | 本验证关系 | 裁决 |
|-------------------|--------|-----------|------|
| `P1-RC-011`（UC-HR-02⑦ 审批人超时，MR1 todo，`:150`） | 审批人超时自动转派 | 本验证 A4.2.17 CONFIRMED 缺失 + SUBMITTED 与薪酬解耦 → 降级证据记录 | **不新建/不撤销**——追加 A4.2.17 运行时影响面确认注记于既有 P1-RC-011 行（维持 P1，降级证据指导 MR1 优先级） |
| `P1-RC-012`（UC-HR-06⑬ 多次打卡 reject，MR1 todo，`:151`） | clockIn reject 守卫 | 本验证 A4.2.18 CONFIRMED reject + 逃生路径 = 标准 CRUD | **不新建/不撤销**——追加 A4.2.18 运行时影响面注记 |
| `P1-RC-013`（UC-HR-06⑭ 跨天 clockOut，MR1 todo，`:152`） | clockOut 跨天查找 | 本验证 A4.2.19 CONFIRMED clocking 侧未实现 + 无补录自动化 | **不新建/不撤销**——追加 A4.2.19 运行时影响面注记 |
| `P1-RC-014`（UC-HR-06⑮ 补卡缺失，MR1 todo，`:153`） | makeUp mutation 缺失 + 越权风险 | 本验证 A4.2.20 CONFIRMED 缺失 + XMeta 无字段级 auth（越权风险代码层证实） | **不新建/不撤销**——追加 A4.2.20 越权风险代码层证据注记 |
| A4.2.21 watch-only residual（换班跨日期语义） | source/target 同日期校验 | 本验证新发现但 L1 未要求 + 无数据破坏 → §2 P2① | **不新建 arm-index 行**——归 §去重协议同型范式 A4.2.5（REJECTED 工单逃生路径无数据破坏 → watch-only 不升 P1），记本报告 §0/§5 非 arm-index finding 行（因 §2 裁决为 P2 watch-only 且 L1 无显式要求，watch-only residual 记报告足以指导 successor） |
| `P1-MA2-091`（排班分配 UK，resolved R1.28） | UK 兜底 | 本验证 A4.2.21 引用 UK 满足作跨日期换班无数据破坏证据 | **不新建**——控制点不同（UK 兜底 vs 同日期校验），仅作数据后果证据引用 |
| `P2-MA2-052`（state-machine.md 缺考勤/排班/换班章节，watch-only） | owner doc 章节 | 本验证 A4.2.21 引用 shift-scheduling.md §5 未要求同日期 | **不新建**——维持 watch-only |

**新 finding 数 = 0**（四项 P1 维持既有 arm-index 行不撤销；A4.2.21 watch-only residual 记报告非 arm-index 行，对齐 A4.2.5/A4.2.12 范式）。

### 5.3 不触发 MR0 / 不归 MR1（本审计）

- **不触发 MR0**：无 P0（§2 P0①④对五项均不成立——SUBMITTED 解耦 + reject/跨天/补卡缺口均有逃生路径 + 换班跨日期无数据破坏）。
- **不归 MR1（本审计）**：四项 P1 维持既有 arm-index 行（修复义务仍归 MR1 R1.0 展开器，本审计不实施，plan Deferred But Adjudicated 正确分类）；A4.2.21 watch-only residual 归 successor/部署决策（L1 未要求，非 MR1 强制）。

---

## 6. 文档更新（预授权）

### 6.1 本验证报告落盘

`docs/audits/2026-08-07-0530-rc-ma4-a4-2-17-21-hr-shift-attendance-runtime.md`（本文件）。

### 6.2 P1-RC-011/012/013/014 arm-index 运行时影响面注记追加（§去重协议）

`docs/audits/arm-index.md` `:150-153` P1-RC-011/012/013/014 行 `修复` 列追加 A4.2.17-21 运行时影响面确认注记（维持 P1 不撤销，降级证据/越权证据指导 MR1 优先级与方案）。不新建 finding 行。

### 6.3 roadmap / log 同步

- `docs/backlog/requirement-compliance-roadmap.md` A4.2.17/A4.2.18/A4.2.19/A4.2.20/A4.2.21 `todo → done ✅`。
- `docs/logs/2026/08-07.md` 追加完成条目（裁决摘要 + 报告路径）。

---

## 7. 与 arm-index / 既有审计去重声明（§去重协议）

- **MA1 ↔ MA2 去重**：本验证复用 A2.7b（`2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`）状态机行为 pass 结论（休假 5 态 + 换班 4 态 + approve 触发联动 + cancel 红冲）+ A1.13 §3/§5 实现证据 + A4.4 hr 代码质量复用 pass，不重新核实行为本身（§去重协议 1-2）。
- **MA4 §7 族去重**：本验证与 A4.2.12-16（A1.12 §7 族 done）+ A4.2.1-11（A1.8-11 §7 族 done）同 MA4 HR 域运行时影响面确认范式，覆盖 A1.13 §7 SP-1..SP-5 五项，无范围重叠（A1.12 §7 族 vs A1.13 §7 族不同切片）。
- **arm-index 交叉去重**：本报告 0 新 finding（§5.2），全部经 grep arm-index 同域同控制点后裁决（P1-RC-011/012/013/014 追加注记 / P1-MA2-091 证据引用 / P2-MA2-052 维持 / A4.2.21 watch-only 归同型范式 A4.2.5 不新建行），无未经比对直接新建的 finding。
- **同型范式对比**：本验证四项 P1 维持对齐 A4.2.16（P2-RC-010 维持 watch-only）+ A4.2.4（P1-MA4-007 维持 resolved + residual watch-only）——「运行时影响面确认不撤销既有裁决」范式；A4.2.21 watch-only 对齐 A4.2.5（REJECTED 工单逃生路径无数据破坏 → §2 P2① watch-only 不升 P1）——「无活跃数据破坏 + L1 无显式要求 → watch-only」决策树范式。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下。**本报告无生产代码变更**（纯审计报告 + arm-index 注记 + roadmap/log 同步，零 Java/ORM/契约变更），checker 无回归风险。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。

  | 规则 | baseline | actual（本次实测） | 变化 |
  |------|----------|-------------------|------|
  | R1a dao().saveEntity (BizModel) | 0 | 0 | — |
  | R1b dao().updateEntity (BizModel) | 0 | 0 | — |
  | R1d dao().findAllByQuery (BizModel) | 14 | 14 | — |
  | R2a BizModel daoFor(ErpMd*) | 34 | 34 | — |
  | R2b BizModel daoFor(Erp*) 跨域 | 229 | 229 | — |
  | R2c 全生产代码 daoFor() 总量 | 1382 | 1382 | — |
  | R3 new Erp*() 构造实体 | 5 | 5 | — |

  本审计无生产代码变更，actual == baseline，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（methodology §8 + plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告 0 新 finding（§5.2），全部经 grep arm-index 同域同控制点后给出「复用 or 新增」裁决（P1-RC-011/012/013/014 追加注记 / P1-MA2-091 证据引用 / P2-MA2-052 维持 / A4.2.21 watch-only 归同型范式 A4.2.5 不新建行），无未经比对直接新建的 finding。

---

## 9. 结论

A1.13 §7 SP-1..SP-5 五项静态存疑点经下游薪酬耦合分析 + 逃生路径可达性探查 + XMeta 字段级守卫普查 + 调度配置 census：

- **SP-1（A4.2.17 审批人超时自动转派）**：`resolveApproverId:203-206` return null + 无 leave-approver-timeout job CONFIRMED；SUBMITTED 休假悬挂与薪酬核算/余额/考勤缺勤**完全解耦**（`sumUnpaidLeaveDays`/`sumUsedDays`/`onLeaveApproved` 均仅读/触发 APPROVED）→ 运行时影响 = SLA/流程效率类。**维持 P1-RC-011**（Q4 强制实现义务不撤销，降级证据指导 MR1 优先级）。
- **SP-2（A4.2.18 多次打卡 reject）**：reject 守卫 CONFIRMED；逃生路径 = 标准 CRUD；无正式申诉工单。**维持 P1-RC-012**。
- **SP-3（A4.2.19 跨天 clockOut）**：clocking 侧按 `eq("date", date)` 无跨天回退 CONFIRMED；calc 侧已实现；无补录自动化流程。**维持 P1-RC-013**。
- **SP-4（A4.2.20 补卡缺失 + 越权风险）**：无 makeUp mutation CONFIRMED；XMeta clockIn/clockOut/source **无字段级 auth 守卫**（越权风险代码层证实）；逃生路径 = 标准 CRUD 绕过字段守卫。**维持 P1-RC-014**。
- **SP-5（A4.2.21 换班跨日期）**：未校验 source/target 同日期 CONFIRMED；跨日期换班**无活跃数据破坏**（UK 满足 + calc 重算正确 + 审计可追溯）；L1 UC-HR-09 **未要求同日期校验** → §2 P2① 边界场景弱。**登记 watch-only residual**（非 arm-index 新行，归同型范式 A4.2.5）。
- **不触发 MR0，无新 arm-index finding 行**（四项 P1 维持既有行 + A4.2.21 watch-only residual 记报告），维持 A1.13 §5 既有裁决（UC-HR-02 P1 / UC-HR-06 P1 / UC-HR-09 接受）。

A1.13 §7 SP-1..SP-5 五项存疑点**闭合**。
