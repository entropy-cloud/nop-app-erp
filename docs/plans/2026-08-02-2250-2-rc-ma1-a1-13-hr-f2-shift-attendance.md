# 2026-08-02-2250-2 rc-ma1-a1-13-hr-f2-shift-attendance hr-F2 排班与考勤需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.13（MA1 需求追踪矩阵审计 — hr-F2 排班与考勤：UC-HR-02 休假申请流程 + UC-HR-06 考勤跟踪 + UC-HR-09 排班管理）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.13
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.13 的 0.2 依赖）、`2026-08-02-2250-1-rc-ma1-a1-12-hr-f1-employee-organization.md`（A1.12，同 hr 域同范式，员工主数据为排班/考勤前置）、`2026-08-02-2231-2-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`（A1.11 done，同范式参考）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.13 给出 UC 清单 = `UC-HR-02/06/09`（3 UC），锚点 `use-cases.md:15 / :63 / :101`（baseline inventory :69/:73/:76 + 切片索引 :347 确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/human-resource/use-cases.md`：
  - UC-HR-02 休假申请流程（`:15`）：员工创建 LeaveRequest 选休假类型(ANNUAL/SICK/etc.) → 填起止日期/原因，系统自动计算 durationDays → 提交 status=SUBMITTED → 审批人审批通过(APPROVED)或驳回(REJECTED) → APPROVED 扣减假期余额 + 通知考勤模块该时段标记休假 → REJECTED 可修改重提；异常：余额不足禁止提交、日期重叠校验、审批人超时自动转派；跨域：考勤(ErpHrAttendance.leaveRequestId 关联)。
  - UC-HR-06 考勤跟踪（`:63`）：系统从打卡机/移动端获取签到时间 → 创建/更新 ErpHrAttendance 记录 clockIn/clockOut → 根据排班规则计算 workHours/lateMinutes/earlyLeaveMinutes → 当天未打卡且无请假记录则 isAbsent=true → 若当天有已批准 LeaveRequest 关联 leaveRequestId；异常：多次打卡以最后一次为准、跨天打卡处理、设备故障支持手工补卡；跨域：休假(LeaveRequest 联动)、薪酬(考勤数据作薪资输入)。
  - UC-HR-09 排班管理（`:101`）：HR 创建班次模板(ErpHrShift) 配置起止时间/宽容期/是否需打卡 → 为员工分配排班(ErpHrShiftAssignment) 支持单个/批量/按轮换模板分配 → 员工发起排班调换申请(ErpHrShiftSwapRequest) 经审批后交换班次 → 休假审批通过后自动关联并标记排班缺席(isAbsent=true) → 排班数据作考勤迟到/早退/缺勤计算标准输入；异常：同一员工同一天重复排班拦截、调换申请目标员工已有冲突排班时拒绝；跨域：ErpHrAttendance(考勤基于排班计算)、ErpHrLeaveRequest(休假联动标记缺席)。

- **L2 owner doc 设计参考**：`docs/design/human-resource/shift-scheduling.md`（班次模板 + 排班分配 + 轮换模板 + 换班 + 排班→考勤计算 + §二/§四/§五 状态机）+ `docs/design/human-resource/state-machine.md`（适用对象一休假 + 排班分配/换班章节[若已补]；P2-MA2-052 watch-only 已登记 state-machine.md 缺考勤/排班/换班独立章节）+ `docs/design/human-resource/README.md`。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验）**：
  - **休假申请（UC-HR-02）**：`module-hr/erp-hr-service/.../entity/ErpHrLeaveRequestBizModel.java`（LeaveRequest 5 态 SUBMITTED/APPROVED/REJECTED + durationDays 自动计算 + 余额扣减；执行时核验：余额不足守卫 + 日期重叠校验 + 审批人超时自动转派 + APPROVED 通知考勤标记休假）+ `ErpHrLeaveBalanceBizModel.java`（假期余额）+ `processor/ErpHrLeaveRequestCancelProcessor.java`（cancel 红冲恢复经 onLeaveCancelled leaveRequestId 匹配回退排班 SCHEDULED）+ `AbstractErpHrLeaveRequestProcessor.java`；`TestErpHrLeaveEngine`。
  - **考勤跟踪（UC-HR-06）**：`ErpHrAttendanceBizModel.java`（clockIn/clockOut 创建/更新 + workHours/lateMinutes/earlyLeaveMinutes 计算 + isAbsent 判定 + leaveRequestId 关联；执行时核验：多次打卡以最后一次为准 + 跨天打卡处理 + 手工补卡）+ `scheduling/ShiftAttendanceCalculator.java`（基于排班计算迟到/早退）+ `processor/ErpHrShiftCalcAttendanceProcessor.java`（排班→考勤计算）；`TestErpHrAttendanceEngine`。**UK_HR_ATTENDANCE_EMP_DATE 存在**（arm-index P1-MA2-091 注记 attendance 表有 UK，assignment 表无）。
  - **排班管理（UC-HR-09）**：`ErpHrShiftBizModel.java`（班次模板：起止时间/宽容期/是否需打卡）+ `ErpHrShiftAssignmentBizModel.java`（assignSingle/assignBatch/copyFromPeriod；**P1-MA2-091 TOCTOU pre-check + erp_hr_shift_assignment 无 (employeeId,assignmentDate) UK，resolved R1.28，HEAD 复核**）+ `processor/ErpHrShiftAssignmentAssignSingleProcessor.java` + `ErpHrShiftAssignmentAssignBatchProcessor.java`（R6.1 per-mutation 拆分）+ `ErpHrShiftSwapRequestBizModel.java`（4 态换班 + approve 副作用交换 shiftId；目标员工冲突排班拒绝）+ `processor/ErpHrShiftSwapRequestSubmitProcessor.java`/`ErpHrShiftSwapRequestApproveProcessor.java` + `ErpHrShiftRotationPatternBizModel.java`（轮换模板）+ `processor/ErpHrShiftRotationPatternGenerateRotationProcessor.java`；`TestErpHrShiftScheduling`。

- **L4 测试证据现状**：`TestErpHrLeaveEngine`（休假状态机 + 余额扣减 + 取消红冲）+ `TestErpHrAttendanceEngine`（考勤打卡 + 迟到/早退/缺勤 + 排班计算）+ `TestErpHrShiftScheduling`（班次模板 + 单个/批量/轮换分配 + 换班 + 冲突拒绝）。E2E：`tests/e2e/business-actions/hr-leave-shift-linkage.action.spec.ts`（休假→排班联动）+ `hr-leave-attendance.action.spec.ts`（休假→考勤）+ `hr-shift-swap.action.spec.ts`（换班）+ `hr-shift-assignment.action.spec.ts`（排班分配）+ `hr-shift-rotation.action.spec.ts`（轮换）。**执行时核验断言强度**（验收标准强断言 vs 仅冒烟）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（A2.7b）= hr 考勤与工资八组件状态机审查**：Verdict 主路径状态迁移守卫齐全（请假 5 态全迁移 + approve/cancel 触发排班联动 / 换班 4 态全迁移 + approve 副作用交换 shiftId）+ @BizMutation 事务回滚保证请假→排班联动失败原子性 + 请假 cancel 红冲恢复经 onLeaveCancelled 回退排班 SCHEDULED。**零 P0**；本切片相关 **P1**：P1-MA2-046（排班分配 status 无 dict 绑定 raw VARCHAR，UC-HR-09 维度）+ P1-MA2-091（排班分配 TOCTOU + 无 UK，**resolved R1.28**，HEAD 复核）；**P2**：P2-MA2-052（state-machine.md 缺考勤/排班/换班章节）。
  - **`docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`（A4.4）**：hr 考勤/排班维度代码质量，本切片复核 resolved 状态（P2-MA4-008 可维护性热点）。
  - **注意**：A2.7b/A4.4 覆盖**状态机迁移守卫/事务边界/代码质量**，但本切片从**需求契约↔实现符合性**视角补差异（UC-HR-02 休假余额扣减/日期重叠/超时转派/考勤联动 + UC-HR-06 打卡规则/排班计算/缺勤判定/手工补卡 + UC-HR-09 班次模板/单批量轮换分配/换班冲突/休假联动标记缺席 + resolved finding HEAD 复核）。

- **arm-index 既有 finding 衔接**：排班与考勤相关——`P1-MA2-046`（排班分配 status 无 dict raw VARCHAR，resolved 状态执行时 HEAD 复核）/ `P1-MA2-091`（排班分配 TOCTOU + 无 UK，**resolved R1.28**，HEAD 复核确认 UK 已加）/ `P1-MA1-022`（跨域 daoFor 只读 hr 投影）/ `P2-MA2-052`（state-machine.md 缺章节 watch-only）。UC-HR-02 审批人超时自动转派、UC-HR-06 多次打卡/跨天打卡/手工补卡、UC-HR-09 轮换模板分配为候选新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` hr 休假/考勤/排班/换班同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及 ORM 结构变更（如排班分配 UK）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.13 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.13 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.13 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-13-hr-f2-shift-attendance.md`，含方法论 §6 **9 段全部内容**：①UC-HR-02/06/09 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含休假状态机 + 考勤打卡 + 排班分配/换班/轮换 Processor 全链）③测试证据（注明断言强度）④运行时行为证据（复用 A2.7b/A4.4，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 3 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-HR-02（休假类型 + durationDays 自动计算 + 5 态审批 + 余额扣减 + 日期重叠校验 + 审批人超时自动转派 + 考勤联动标记）+ UC-HR-06（打卡 clockIn/clockOut + 排班规则计算 workHours/late/early + isAbsent 判定 + LeaveRequest 关联 + 多次打卡以最后一次为准 + 跨天打卡 + 手工补卡）+ UC-HR-09（班次模板 + 单个/批量/轮换分配 + 换班申请审批交换 + 休假联动标记缺席 + 重复排班拦截 + 冲突排班拒绝 + 排班作考勤标准输入），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-HR-09 排班分配 P1-MA2-091 TOCTOU + UK（resolved R1.28，HEAD 复核确认 UK 已加则 dedup 闭环，仍 open 则维持 P1）** + 排班分配 status 无 dict（P1-MA2-046 HEAD 复核）；**UC-HR-02 审批人超时自动转派**（执行时 HEAD 核验：实现倾向接受、缺失倾向 P1）；**UC-HR-06 多次打卡/跨天打卡/手工补卡**（执行时核验异常路径覆盖）；UC-HR-09 轮换模板分配（执行时核验）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / hr use-cases / shift-scheduling.md / state-machine.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.13 只覆盖 UC-HR-02/06/09；UC-HR-01/05/07/08/12 归 A1.12，UC-HR-03/04/10/11 归 A1.14）。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A2.7b/A4.4 已证实状态机迁移守卫/事务边界/代码质量，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.13 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.13 UC 锚点）+ `docs/design/human-resource/use-cases.md`（L1 真相源）+ `docs/design/human-resource/shift-scheduling.md` + `state-machine.md` + `README.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.7b/A4.4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.7b/A4.4 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrLeaveEngine,TestErpHrAttendanceEngine,TestErpHrShiftScheduling`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-13-hr-f2-shift-attendance.md`（落盘 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [ ] `Proof` 对 UC-HR-02/06/09 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:15/:63/:101` 验收标准原文（禁止转述）；L2 引用 `shift-scheduling.md`（班次模板 + 排班分配 + 轮换 + 换班 + 排班→考勤 + §二/§四/§五 状态机）+ `state-machine.md`（休假 + 排班/换班，标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpHrLeaveRequestBizModel.java:line`（5 态 + durationDays + 余额扣减）+ `ErpHrLeaveBalanceBizModel:line` + `ErpHrLeaveRequestCancelProcessor:line`（cancel 红冲 onLeaveCancelled）+ `ErpHrAttendanceBizModel.java:line`（clockIn/clockOut + workHours/late/early + isAbsent + leaveRequestId 关联）+ `ShiftAttendanceCalculator:line`（排班→考勤计算）+ `ErpHrShiftCalcAttendanceProcessor:line` + `ErpHrShiftBizModel:line`（班次模板）+ `ErpHrShiftAssignmentBizModel.java:line`（assignSingle/assignBatch/copyFromPeriod）+ `ErpHrShiftAssignmentAssignSingleProcessor`/`AssignBatchProcessor:line` + `ErpHrShiftSwapRequestBizModel:line`（4 态换班 + approve 交换 shiftId）+ `ErpHrShiftSwapRequestSubmitProcessor`/`ApproveProcessor:line` + `ErpHrShiftRotationPatternBizModel:line` + `ErpHrShiftRotationPatternGenerateRotationProcessor:line`；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.7b/A4.4 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-HR-02——①休假类型(ANNUAL/SICK/etc.)；②durationDays 自动计算；③5 态审批(SUBMITTED→APPROVED/REJECTED)；④APPROVED 扣减假期余额；⑤日期重叠校验；⑥余额不足禁止提交；⑦审批人超时自动转派；⑧APPROVED 通知考勤标记休假。UC-HR-06——⑨clockIn/clockOut 创建/更新；⑩排班规则计算 workHours/lateMinutes/earlyLeaveMinutes；⑪未打卡且无请假→isAbsent=true；⑫已批准 LeaveRequest 关联 leaveRequestId；⑬多次打卡以最后一次为准；⑭跨天打卡处理；⑮设备故障手工补卡。UC-HR-09——⑯班次模板(起止时间/宽容期/是否需打卡)；⑰单个/批量/轮换分配；⑱换班申请审批交换班次；⑲目标员工冲突排班拒绝；⑳休假审批通过自动关联标记排班缺席(isAbsent=true)；㉑同一员工同一天重复排班拦截；㉒排班作考勤迟到/早退/缺勤计算标准输入。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` **resolved finding HEAD 复核**：对排班与考勤相关 finding（P1-MA2-046 排班分配 status 无 dict raw VARCHAR / P1-MA2-091 排班分配 TOCTOU + 无 UK[resolved R1.28] / P1-MA1-022 跨域 daoFor hr 投影 / P2-MA2-052 state-machine.md 缺章节——**resolved 状态执行时经 arm-index grep 确认，未确认者按"未定"处理**）在当前 HEAD 代码实际落地（按逻辑非行号核验），逐条记录复核结论。**P1-MA2-091 关键**：HEAD 复核 `erp_hr_shift_assignment` 是否已加 `(employeeId, assignmentDate, shiftId)` UK（R1.28 声称已加），若已加则 UC-HR-09㉑ 重复排班拦截 dedup 闭环，仍 open 则维持 P1 触发 MR1。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-HR-09 排班分配（P1-MA2-091 HEAD 复核：resolved 则接受 on ㉑，open 则 dedup P1-MA2-091；P1-MA2-046 status 无 dict HEAD 复核）。UC-HR-02⑦ 审批人超时自动转派（执行时 HEAD 核验：实现倾向接受、缺失倾向 P1①功能缺失）。UC-HR-06⑬⑭⑮ 异常路径（多次打卡/跨天/手工补卡，执行时核验：实现倾向接受、缺失倾向 P1②异常路径未实现）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-HR-02/06/09 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.7b/A4.4 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-㉒ 有明确分级（非悬空"待查"）；P1-MA2-091 UK HEAD 复核结论已记录（重复排班拦截关键证据）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-13-hr-f2-shift-attendance.md`（落盘 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` hr 休假/考勤/排班/换班同域同控制点（如 P1-MA2-046/091）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-HR-02⑦ 审批人超时自动转派缺失 = 需求契约视角新维度 / UC-HR-06⑬⑭⑮ 异常路径缺失若发现）→ 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-HR-09㉑ 重复排班与 P1-MA2-091 同根因则交叉引用而非重复新建。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如审批人超时自动转派运行时触发、多次打卡/跨天打卡运行时边界、轮换模板分配运行时批量行为、休假→排班联动标记缺席运行时一致性；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A2.7b（考勤与工资八组件状态机 + 请假 5 态/换班 4 态全迁移 + approve/cancel 触发排班联动 + cancel 红冲 onLeaveCancelled + P1-MA2-046/091 + P2-MA2-052 finding）+ A4.4（考勤/排班维度代码质量 P2-MA4-008）已证实结论，列明本切片只补的需求视角差异（UC-HR-02 休假余额扣减/日期重叠/超时转派/考勤联动 + UC-HR-06 打卡规则/排班计算/缺勤判定/手工补卡 + UC-HR-09 班次模板/单批量轮换分配/换班冲突/休假联动标记缺席 + resolved finding HEAD 复核）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [ ] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.2 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03cef698bffes6PBp6WvzCh1K1`，fresh session，未起草本计划）。逐项实测核验：roadmap 对齐（A1.13 / UC-HR-02/06/09 / Deps=M0.1+M0.2 done / Skill）、3 UC 锚点 :15/:63/:101 全匹配（完整枚举无跳无合并）、L3 全部 16 个代码路径存在（ErpHrLeaveRequestBizModel entity:45 + ErpHrLeaveBalanceBizModel + LeaveRequestCancelProcessor + ErpHrAttendanceBizModel + ShiftAttendanceCalculator scheduling:16 + ErpHrShiftCalcAttendanceProcessor + ErpHrShiftBizModel + ErpHrShiftAssignmentBizModel assignSingle:69/assignBatch:78/copyFromPeriod:88 + AssignSingle/AssignBatchProcessor + ShiftSwapRequestBizModel + Submit/ApproveProcessor + ShiftRotationPatternBizModel + GenerateRotationProcessor）、L4 3 测试 + 5 E2E 全存在、L5 dedup 输入 A2.7b/A4.4 存在 + arm-index finding IDs 全命中（**P1-MA2-091 arm-index:277 "✅ resolved (R1.28 done)" 与本计划声称一致** + P1-MA2-046 A2.7b:389 + P1-MA1-022 + P2-MA2-052 watch-only:472）、跨切片边界正确（UC-HR-01/05/07/08/12→A1.12、UC-HR-03/04/10/11→A1.14，12 HR UC 无重叠）、只读审计删门控有据（:128-137）、保护区域 ORM UK ask-first 延后 MR0/MR1 合规、P1-MA2-091 HEAD 复核框架正确（:82 条件式"R1.28 声称已加，若已加则 dedup 闭环，仍 open 则维持 P1"非既成事实断言）。**无阻塞 issue**。次要非阻塞观察：P1-MA2-091 实际源自 A2.17（并发审计 arm-index:530）非 A2.7b，但其存在性/resolved-R1.28 状态/HEAD 复核框架均正确（方法论要求执行时 grep arm-index 权威去重），不影响执行契约。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.13 报告 9 段齐全 + UC-HR-02/06/09 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.13 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及 ORM 结构变更（排班分配 UK）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <待独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理填充>
- Evidence: <待填充>

Follow-up:

- finding 修复属 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）successor，非阻塞本审计闭环（§Deferred But Adjudicated 已 adjudicated）
