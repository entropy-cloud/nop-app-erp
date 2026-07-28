# 2026-07-28-0230-2-audit-remediation-ma2-hr-attendance-payroll-state-machine MA2 hr 状态机审查 — 考勤与工资（A2.7b）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.7b hr 状态机审查 — 考勤与工资（S 级拆分 2/2）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.7b）
> Related: `docs/plans/2026-07-28-0230-1-audit-remediation-ma2-hr-employee-organization-state-machine.md`（A2.7a 员工与组织，S 级拆分 1/2，先执行——工资 runPayroll 依赖员工 employmentStatus=ACTIVE/PROBATION）；`docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`+`2026-07-28-0109-2-...-mrp-bom-...`（A2.6a/b manufacturing 状态机审查范式，全 done——dict 死状态 + owner doc 漂移 + posted 标记双轴同型裁决 P1-MA2-035/036）；`docs/plans/2026-07-27-2315-2-audit-remediation-ma2-finance-arap-settlement-state-machine.md`（A2.5c finance AR/AP 核销——工资过账经 finance 凭证链 PostingDispatcher 同型范式）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/human-resource/state-machine.md`（请假/考勤/工资审批支付状态机）+`payroll.md`+`payroll-simulation.md`+`shift-scheduling.md`（owner doc）
> Audit: required

## Current Baseline

hr（人力资源）域 S 级状态机审查拆分 2 片：**A2.7a = 员工与组织类状态机**（先执行，done 后执行本计划）；**A2.7b = 考勤与工资类状态机**（请假审批 / 考勤打卡 / 排班换班 / 工资审批-支付双轴 / 薪酬仿真）。本审计 A2.7b 聚焦**考勤与薪酬生命周期**——owner doc `state-machine.md` 请假/考勤/工资 + `payroll.md` 工资审批-支付 + `payroll-simulation.md` 仿真 + `shift-scheduling.md` 排班/换班。

实时仓库已落地的考勤与工资状态机实现（逐项核实，路径 `module-hr/`）：

- **请假审批状态机**（`ErpHrLeaveRequest`，ORM `app-erp-hr.orm.xml:481-530`）：列 `status` dict `erp-hr/leave-status`（**5 态**：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED，dict `orm.xml:50-56`）。
  - 迁移实现（`ErpHrLeaveRequestBizModel.java` 227 行）：`submit:73-83`(DRAFT→SUBMITTED，守卫 `requireStatus:136-143` + `checkLeaveBalance:145-165` + `checkDateOverlap:167-187`) / `approve:86-98`(SUBMITTED→APPROVED + `shiftBiz.onLeaveApproved:96` 跨实体副作用——排班置 ABSENT) / `reject:101-108`(SUBMITTED→REJECTED) / `cancel:111-119`(APPROVED→CANCELLED + `shiftBiz.onLeaveCancelled:117` 回退排班 SCHEDULED)。
  - 请假余额联动：`sumUsedDays:199-213` 聚合已 APPROVED 请假天数 → `ErpHrLeaveBalance` 余额校验。
- **考勤打卡**（`ErpHrAttendance`，ORM `orm.xml:665-711`）：**无 enum status 列**——仅 `isAbsent` 布尔（ORM:677）+ clockIn/clockOut 时间戳。
  - 迁移实现（`ErpHrAttendanceBizModel.java` 122 行）：`clockIn:52-73`(幂等守卫 `ERR_ALREADY_CLOCKED_IN:66-69`) / `clockOut:76-88`(守卫 `ERR_NOT_CLOCKED_IN:80-83`)。`isAbsent` 由 `ErpHrShiftBizModel.calcAttendance` 写入（非此处）。
- **工时单状态机**（`ErpHrTimesheet`，ORM `orm.xml:577-619`）：列 `status` dict `erp-hr/timesheet-status`（**4 态**：DRAFT/SUBMITTED/APPROVED/REJECTED，dict `orm.xml:57-62`）。
  - 迁移实现（`ErpHrTimesheetBizModel.java` 47 行）：**⚠️ 仅 `submit:35-46` 实现（DRAFT→SUBMITTED，守卫 L38-42 期望 DRAFT）**。**APPROVED/REJECTED 迁移未实现**（无 approve/reject 方法）→ APPROVED/REJECTED 两态**无 writer**（候选死状态）。**⚠️ 使用硬编码字符串 `"DRAFT"`/`"SUBMITTED"`（L38/L43）** 而非 `ErpHrConstants`——与其他 BizModel 不一致。
- **排班分配状态机**（`ErpHrShiftAssignment`，ORM `orm.xml:1170-1228`）：列 `status`（ORM:1186）**⚠️ 无 dict 绑定——raw VARCHAR(50)** + `isAbsent` 布尔（ORM:1181）。状态值仅由 `ErpHrConstants.ASSIGNMENT_STATUS_*`（L98-101：SCHEDULED/PRESENT/ABSENT/CANCELLED）定义。
  - 迁移实现（隐式，由多个 BizModel 写入）：`ErpHrShiftAssignmentBizModel`（205 行 `assignSingle:59-67` 创建 SCHEDULED）+ `ErpHrShiftBizModel.calcAttendance:55-110`（写 ABSENT L74/L86 或 PRESENT L103）+ `ErpHrShiftBizModel.onLeaveApproved:124-136`（请假范围置 ABSENT + leaveRequestId）+ `ErpHrShiftBizModel.onLeaveCancelled:139-154`（回退 SCHEDULED，仅 leaveRequestId 匹配 L146）+ `ErpHrShiftRotationPatternBizModel.deleteExistingAssignments:177-194`（regenerate 时 SCHEDULED→CANCELLED L190）+ `ErpHrShiftSwapRequestBizModel`（换班批准后交换 shiftId + 重置 SCHEDULED）。
  - **⚠️ 无 dict.yaml**（无 `assignment-status.dict.yaml`）——UI/验证器无法枚举合法值（audit gap，与 mfg A2.6a 字典命名漂移 P2 同型但更严重——完全无 dict）。
- **换班申请状态机**（`ErpHrShiftSwapRequest`，ORM `orm.xml:1266-1324`）：列 `status` dict `erp-hr/swap-status`（**4 态**：PENDING/APPROVED/REJECTED/CANCELLED，dict `orm.xml:166-171`）。
  - 迁移实现（`ErpHrShiftSwapRequestBizModel.java` 144 行）：`submit:49-75`(创建 PENDING L72) / `approve:78-110`(PENDING→APPROVED + 交换双方 shiftId L92-94 + 设 replacedByAssignmentId L98-99 + 重置 SCHEDULED L101-102) / `reject:113-120`(PENDING→REJECTED) / `cancel:123-130`(PENDING→CANCELLED)。守卫 `assertTransition:134-142`。
- **工资审批-支付双轴状态机**（`ErpHrSalary`，ORM `orm.xml:714-782`）：**双轴**：列 `approveStatus` dict `wf/approve-status`（ORM:736，平台标准 4 态 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）+ 列 `paymentStatus` dict `erp-hr/salary-payment-status`（ORM:735，**3 态**：PENDING/PAID/VOID，dict `orm.xml:72-76`）+ `posted` 布尔（ORM:758）。
  - 迁移实现（`ErpHrSalaryBizModel.java` 254 行）：支付轴 `markPaid:97-118`(PENDING→PAID，**双守卫**：approveStatus 须 APPROVED L100-105 且 paymentStatus 须 PENDING L106-111 + `postingDispatcher.tryPostPayment:112`) / `voidSalary:121-131`(→VOID，守卫拒已 PAID `ERR_SALARY_LOCKED_AFTER_PAID:124-127`) / `generateBankFile:134-178`(批量 PENDING→PAID + 创建 `ErpHrPayrollBankFile` status=GENERATED L168) / `runPayroll:79-94`(批量计算 ACTIVE/PROBATION 员工，filter L232-238) / `calculateSalary:67-76`(单人计算)。
  - **审批轴 UNSUBMITTED↔SUBMITTED↔APPROVED/REJECTED 迁移不在此类**——委托平台 `approval-support.xbiz` 标准动作（javadoc L36-40）。**module-hr 无 `.xbiz.xml` 覆盖**（grep 零匹配）——确认平台 DIRECT 模式默认行为与 owner doc `state-machine.md §四` 一致。
  - **⚠️ orphan dict `erp-hr/salary-approval-status`（6 态 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID，`orm.xml:77-84`）存在但 `ErpHrSalary.approveStatus` 实际引用 `wf/approve-status`**——orphan dict 6 态无 column 使用（P2-MA1-020 已登记 MR1，本审计复核工资侧）。
- **薪酬仿真状态机**（`ErpHrSalarySimulation`，ORM `orm.xml:854-907`）：列 `status` dict `erp-hr/simulation-status`（**5 态**：DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED，dict `orm.xml:148-154`）。
  - 迁移实现（`ErpHrSalarySimulationBizModel.java` 1129 行）：`createSimulation:70-95`(DRAFT L92) / `submitForReview:385-402`(DRAFT→IN_REVIEW，守卫 L389 + 须有≥1 调整 L395-398) / `approve:405-421`(IN_REVIEW→APPROVED) / `reject:424-439`(IN_REVIEW→REJECTED) / `convertToFormal:442-532`(APPROVED→CONVERTED + 创建正式 `ErpHrSalary` approveStatus=UNSUBMITTED/paymentStatus=PENDING L500-501，守卫 L446 + per-employee PAID_CONFLICT L465/DUPLICATE L470 skip + all-conflict throw L510-525)。
- **银行付款文件状态机**（`ErpHrPayrollBankFile`，ORM `orm.xml:1095-1130`）：列 `status` dict `erp-hr/bank-file-status`（**3 态**：GENERATED/UPLOADED/CONFIRMED，dict `orm.xml:132-136`，默认 GENERATED）。
  - 迁移实现（`ErpHrPayrollBankFileBizModel.java`）：**⚠️ 18 行 CRUD 桩——无状态迁移方法**。仅 `ErpHrSalaryBizModel.generateBankFile:168` 设 GENERATED。**UPLOADED/CONFIRMED 两态无 writer**（候选死状态——银行回单上传/确认未实现）。
- **跨域访问**（考勤/工资代码）：**工资过账跨域写 finance 凭证**——`SalaryPostingDispatcher.java`（155 行）`tryPostAccrual:46`(APPROVED_MANAGER→计提凭证) + `tryPostPayment:66`(PAID→付款凭证)，**吞异常返回 boolean**（与 mfg posting dispatcher tryPost 同型容错）；`SalaryPostingExecutor.java` 包装 `IErpFinVoucherBiz.post()`（REQUIRES_NEW）；`SalaryPostingProvider.java` 实现跨域 `IErpFinAcctDocProvider`(L36)。`ErpHrReportBizModel.java:78` 注入 `IErpFinArApItemBiz`（员工净余额报表只读）。**无 `daoFor(ErpFin*)` 直写**（全经 I*Biz Facade，合规）。`ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)` 只读（P1-MA1-022 同型已登记）。
- **Processor 类**：**module-hr 无任何 `*Processor.java`**——状态迁移全在 BizModel 内联。`PayrollCalculator`/`IncomeTaxCalculator`/`SocialInsuranceCalculator`（工资计算 helper）+ `ShiftAttendanceCalculator`（考勤计算 helper）是计算非状态编排。
- **测试覆盖**：`TestErpHrLeaveEngine`（请假 submit/approve/reject/cancel + 余额 + 重叠）+`TestErpHrAttendanceEngine`（clockIn/clockOut）+`TestErpHrShiftScheduling`（排班/分配/换班/轮班 + calcAttendance）+`TestErpHrSalaryWorkflowApproval`（工资 approveStatus + paymentStatus markPaid/void）+`TestErpHrPayrollEngine`（工资计算 + runPayroll）+`TestErpHrPayrollSimulation`（仿真 DRAFT→IN_REVIEW→APPROVED/REJECTED→CONVERTED）。**无独立工时单 approve/reject 测试**（因未实现）+**无银行文件 UPLOADED/CONFIRMED 测试**（因未实现）+**无排班分配 dict 测试**（因无 dict）。

**已登记的直指考勤/工资状态机的 finding（本审计须复核其状态机行为）**：

- `P2-MA1-020`（todo MR1，hr）：残留 orphan dict `erp-hr/salary-approval-status`（6 态）。**状态机 scope**：`ErpHrSalary.approveStatus` 实际引用 `wf/approve-status`（4 态），orphan 6 态无 column 使用。本审计复核 owner doc `payroll.md §审批状态标准化` 是否声明 6 态历史状态机已废弃 + 确认 orphan dict 在工资状态机上不引入悬挂（6 态无 writer 无 column → 纯 i18n/javadoc 残留）。
- `P1-MA1-022`（todo MR1，9 域合并）：hr `daoFor(ErpMdPartner)` 只读（report 聚合）。**状态机 scope**：跨域只读是报表聚合副作用，不破坏状态机裁决——本审计复核其在状态迁移异常路径无悬挂。

**但从未做过一次覆盖考勤与工资状态机（请假 + 考勤 + 工时单 + 排班分配 + 换班 + 工资双轴 + 仿真 + 银行文件八组件）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：考勤 isAbsent 布尔（无 enum——是否应建模为状态）；排班分配 status 无 dict（raw VARCHAR + 常量——清晰性缺陷）；工时单 APPROVED/REJECTED 是等待点（审批结果）；工资双轴 approveStatus×paymentStatus 组合语义（APPROVED+PENDING=待付 / APPROVED+PAID=已付 / REJECTED+PENDING=驳回待处理）；仿真 CONVERTED 终态；银行文件 UPLOADED/CONFIRMED 等待点（桩未实现）。
- **转换完整性**：工时单 **DRAFT→SUBMITTED 实现 / SUBMITTED→APPROVED/REJECTED 未实现**（迁移缺失——dict 两项死状态）；排班分配 SCHEDULED→PRESENT/ABSENT（calcAttendance）/ ABSENT→SCHEDULED（onLeaveCancelled 回退，仅 leaveRequestId 匹配）/ SCHEDULED→CANCELLED（regenerate）/ 换班批准交换；工资支付轴 PENDING→PAID（markPaid 双守卫）/ →VOID（voidSalary）/ 批量 generateBankFile；审批轴委托平台；仿真 5 态全迁移；银行文件 **GENERATED→UPLOADED/CONFIRMED 未实现**（桩）；请假 5 态全迁移 + approve 触发排班 onLeaveApproved + cancel 触发 onLeaveCancelled。
- **终端状态与恢复**：工资 PAID/VOID 终态（VOID 不可恢复？voidSalary 守卫拒已 PAID）；仿真 CONVERTED 终态（不可恢复——已转正式）；请假 APPROVED→CANCELLED（cancel 红冲恢复，非真终态）/ REJECTED 终态；排班分配 CANCELLED 终态（regenerate 后不可恢复）；考勤打卡无终态（每日记录）。
- **异常路径**：请假余额不足（守卫 checkLeaveBalance 拒绝）；请假日期重叠（守卫 checkDateOverlap 拒绝）；重复打卡（幂等守卫）；工资 markPaid 未审批（双守卫拒绝 approveStatus 非 APPROVED）；voidSalary 已 PAID（守卫拒绝）；工资过账失败（SalaryPostingDispatcher **吞异常返回 boolean**——posted=false 悬挂？与 mfg posting dispatcher tryPost 同型容错，需核验工资侧是否有悬挂告警闭环）；仿真 convertToFormal 冲突（per-employee skip + all-conflict throw）；onLeaveCancelled leaveRequestId 不匹配（仅回退匹配项——部分回退？）。
- **可达性**：**工时单 APPROVED/REJECTED 是否可达**（无 approve/reject writer → dict 死状态，同 finance P1-MA2-031 + mfg P1-MA2-035/036 同型）；**银行文件 UPLOADED/CONFIRMED 是否可达**（桩无 writer → 死状态）；排班分配 CANCELLED 可达性（仅 regenerate）；工资 REJECTED approveStatus 可达性（平台审批动作）；仿真从 DRAFT 到 CONVERTED 可达性。
- **角色与权限**：请假 submit（员工）/approve（主管）/工资 approve（财务主管）/markPaid（出纳）/runPayroll（HR）；危险操作（markPaid 触发 finance 凭证过账——跨域写会计保护区域 / voidSalary 已过账工资作废 / generateBankFile 批量付款影响资金）；多角色冲突（HR runPayroll vs 财务 approve vs 出纳 markPaid）。
- **外部依赖**：工资过账经 `IErpFinVoucherBiz`（finance 凭证链，REQUIRES_NEW，跨域写会计保护区域）；工资计提/付款 `IErpFinAcctDocProvider`（Provider 反向注册）；员工净余额报表 `IErpFinArApItemBiz`（只读）；请假 approve→排班 onLeaveApproved（同域跨实体）；外部步骤失败是否阻断状态迁移（SalaryPostingDispatcher 吞异常——**状态迁移与过账解耦**，需核验 posted=false 悬挂）。
- **TODO/任务策略**：请假 SUBMITTED 产生主管审批 TODO；工资 SUBMITTED 产生财务审批 TODO；银行文件 GENERATED 产生出纳上传 TODO（桩未实现——静默？）；工时单 SUBMITTED 产生审批 TODO（approve 未实现——长期 SUBMITTED 静默下沉？）；考勤缺卡是否产生 TODO（calcAttendance 置 ABSENT 是否告警）；仿真 IN_REVIEW 产生审批 TODO。
- **场景演练**：(a) 请假快乐路径（DRAFT→submit→SUBMITTED→approve→APPROVED + 余额扣减 + 排班置 ABSENT）；(b) 请假 reject/cancel（SUBMITTED→REJECTED / APPROVED→cancel→CANCELLED + 排班回退 SCHEDULED）；(c) 考勤打卡（clockIn→clockOut + calcAttendance 置 PRESENT/ABSENT）；(d) **工时单审批**（DRAFT→SUBMITTED→APPROVED/REJECTED——**approve/reject 未实现，演练确认缺口**）；(e) 排班分配生命周期（assign SCHEDULED→calcAttendance PRESENT/ABSENT→regenerate CANCELLED）；(f) 换班（submit PENDING→approve→交换 shiftId + 重置 SCHEDULED）；(g) 工资双轴（UNSUBMITTED→平台审批→APPROVED→markPaid→PAID + 凭证过账 + generateBankFile）；(h) 工资 void（PENDING→VOID / 已 PAID 拒绝）；(i) 仿真全链（DRAFT→IN_REVIEW→APPROVED→convertToFormal→CONVERTED + 创建正式工资）；(j) **银行文件**（GENERATED→UPLOADED→CONFIRMED——**桩未实现，演练确认缺口**）；(k) 工资过账失败（tryPostPayment 吞异常→posted=false 悬挂？）；(l) 并发 markPaid 同工资（无 @Version——交接 A2.17）。
- **与设计文档一致性**：`state-machine.md`/`payroll.md`/`payroll-simulation.md`/`shift-scheduling.md` vs 实现——**重点漂移**：(1) **工时单 APPROVED/REJECTED owner doc 声明但未实现**（漂移——裁决 P1 dict 死状态 / owner doc 标注 Deferred）；(2) **银行文件 UPLOADED/CONFIRMED owner doc 声明但桩未实现**（漂移）；(3) **排班分配 status 无 dict**（owner doc shift-scheduling.md 是否声明——清晰性缺陷）；(4) **工时单使用硬编码字符串** vs ErpHrConstants（不一致）；(5) orphan dict salary-approval-status 6 态 vs 实际 wf/approve-status 4 态（owner doc `payroll.md §审批状态标准化` 已声明废弃——确认 owner doc 一致性）；(6) **SalaryPostingDispatcher javadoc L27-28 声称"薪酬实体无 posted 字段"但 ORM:758 定义 posted**（doc/code drift——需核验）；(7) 工资过账 tryPostPayment 吞异常是否 owner doc 声明容错语义。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（**工时单 approve/reject 迁移完全缺失致 APPROVED/REJECTED 死状态** [若破坏业务路径——按同型裁决 P1 dict 死状态非 P0] / **工资过账 tryPostPayment 吞异常致 posted=false 悬挂无告警闭环** [若破坏业财一致——需核验是否有期末结账前置检查兜底] / **银行文件 UPLOADED/CONFIRMED 未实现致付款文件生命周期断裂** [若影响资金流——config-gated 或 Deferred]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **请假审批状态机**（5 态）+ **考勤打卡**（isAbsent 布尔）+ **工时单状态机**（4 态）+ **排班分配状态机**（无 dict，4 值常量）+ **换班申请状态机**（4 态）+ **工资审批-支付双轴状态机**（approveStatus 4 态 + paymentStatus 3 态 + posted）+ **薪酬仿真状态机**（5 态）+ **银行付款文件状态机**（3 态）做系统性业务审查，产出审计报告。**严格限定 A2.7b scope = 考勤与工资类状态机**；员工/合同/招聘/考核/发展计划归 A2.7a。
- 重点核验已识别控制点：(1) 状态定义清晰性（考勤 isAbsent 布尔 / 排班分配无 dict / 工资双轴组合语义 / 银行文件等待点）；(2) 转换完整性（**工时单 approve/reject 未实现** / 排班分配多 writer / 工资支付轴双守卫 / **银行文件 UPLOADED/CONFIRMED 未实现**）；(3) 终端与恢复（PAID/VOID/CONVERTED/CANCELLED 终态）；(4) 异常路径（请假余额不足/重叠 / 重复打卡 / markPaid 未审批 / voidSalary 已 PAID / **工资过账吞异常 posted=false 悬挂** / onLeaveCancelled 部分回退）；(5) 可达性（**工时单 APPROVED/REJECTED / 银行文件 UPLOADED/CONFIRMED 死状态**）；(6) 角色权限（markPaid 触发跨域会计写 / generateBankFile 资金影响）；(7) 外部依赖（工资过账 IErpFinVoucherBiz 跨域写 / AcctDocProvider 反向注册 / 请假→排班联动）；(8) TODO 任务策略（工时单/银行文件长期 SUBMITTED/GENERATED 静默）；(9) 场景演练（12 个代表性场景）。
- 复核已登记 finding 在考勤/工资状态机运行时的行为影响：P2-MA1-020（orphan dict——确认工资状态机无悬挂）/ P1-MA1-022（daoFor 跨域只读——状态机角度无升级），标注终态。
- scope matrix §状态机正确性 hr 列 `⚠️(P1)(A2.7a✅;A2.7b❓)` → `⚠️(P1)(A2.7a✅;A2.7b✅)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.7b 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.7a 员工与组织类状态机（员工/合同/招聘/考核/发展计划/调查） — 那是 S 级拆分 1/2，先执行；本审计只确认工资 runPayroll 依赖员工 employmentStatus=ACTIVE/PROBATION（A2.7a 复核员工侧）。
- **不**审计 A2.3 期末结账端到端 — done；本审计只复核工资过账经 finance 凭证链的状态机迁移正确性（过账是 markPaid 的副作用）。
- **不**审计 A2.5 finance 凭证状态机 — done；本审计只确认工资过账经 finance I*Biz（hr 代码无 `daoFor(ErpFin*)` 已确认）。
- **不**审计 A4.4 hr 代码质量 — BizModel/Calculator 代码质量（异常处理/N+1/索引）系统性审查归 A4.4；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发 markPaid/runPayroll 归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 owner doc 已裁定 Deferred 偏离是否应实现（银行回单自动对账 / 工时单自动审批 / 考勤 IoT 集成 / 个税精算） — 这些是 owner doc Deferred/Non-Goal，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/human-resource/state-machine.md`（请假/考勤/工时单/工资审批-支付双轴 + §实现偏离补注 — **需复核工时单 approve/reject 未实现 + 银行文件未实现漂移 + 排班分配无 dict**）；`docs/design/human-resource/payroll.md`（工资审批-支付双轴 + orphan dict §审批状态标准化 + 银行付款 + posted 字段 — **需复核 SalaryPostingDispatcher javadoc 与 ORM posted drift**）；`docs/design/human-resource/payroll-simulation.md`（仿真 5 态 + convertToFormal）；`docs/design/human-resource/shift-scheduling.md`（排班/分配/换班/轮班 + 请假联动 — **需复核分配 status 无 dict**）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.7b 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：考勤/工资状态机本身非 ask-first 最高级保护区域，但**工资过账副作用触及 finance 凭证链**（markPaid 触发 IErpFinVoucherBiz.post 跨域写会计保护区域）+ **generateBankFile 影响资金流**。P0 即时修复若触及 `ErpHrSalaryBizModel`/`SalaryPostingDispatcher`/`ErpHrTimesheetBizModel`/`ErpHrPayrollBankFileBizModel`/`ErpHrLeaveRequestBizModel`/`ErpHrShiftBizModel`，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计/资金保护区域）。ORM 字典变更（leave-status/timesheet-status/salary-payment-status/simulation-status/bank-file-status/swap-status）属 ask-first。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 考勤与工资状态机系统性业务审查

Status: completed
Targets: `module-hr/erp-hr-service/.../service/entity/ErpHrLeaveRequestBizModel.java`（submit:73-83/approve:86-98+shiftBiz.onLeaveApproved:96/reject:101-108/cancel:111-119+shiftBiz.onLeaveCancelled:117 + 守卫 requireStatus:136-143/checkLeaveBalance:145-165/checkDateOverlap:167-187/sumUsedDays:199-213）；`.../service/entity/ErpHrAttendanceBizModel.java`（clockIn:52-73 幂等:66-69/clockOut:76-88 守卫:80-83）；`.../service/entity/ErpHrTimesheetBizModel.java`（submit:35-46 守卫:38-42 + **硬编码字符串 DRAFT/SUBMITTED L38/L43 + approve/reject 未实现**）；`.../service/entity/ErpHrShiftAssignmentBizModel.java`（assignSingle:59-67 SCHEDULED + activeStatuses:171-177）；`.../service/entity/ErpHrShiftBizModel.java`（calcAttendance:55-110 写 ABSENT:74,86/PRESENT:103 + onLeaveApproved:124-136/onLeaveCancelled:139-154 leaveRequestId 匹配:146）；`.../service/entity/ErpHrShiftRotationPatternBizModel.java`（generateRotation:55-94 + deleteExistingAssignments:177-194 CANCELLED:190）；`.../service/entity/ErpHrShiftSwapRequestBizModel.java`（submit:49-75/approve:78-110 交换 shiftId:92-94+重置 SCHEDULED:101-102/reject:113-120/cancel:123-130 + assertTransition:134-142）；`.../service/entity/ErpHrSalaryBizModel.java`（calculateSalary:67-76/runPayroll:79-94 filter:232-238/markPaid:97-118 双守卫:100-111+postingDispatcher.tryPostPayment:112/voidSalary:121-131 守卫:124-127/generateBankFile:134-178 GENERATED:168 + findPayableSalaries:240）；`.../service/entity/ErpHrSalarySimulationBizModel.java`（createSimulation:70-95/submitForReview:385-402/approve:405-421/reject:424-439/convertToFormal:442-532 + per-employee skip:465,470/all-conflict throw:510-525）；`.../service/entity/ErpHrPayrollBankFileBizModel.java`（18 行 CRUD 桩无状态迁移）；`.../service/posting/SalaryPostingDispatcher.java`（tryPostAccrual:46/tryPostPayment:66 吞异常返回 boolean + javadoc:27-28 "无 posted 字段" drift）+`SalaryPostingExecutor.java`（IErpFinVoucherBiz.post REQUIRES_NEW）+`SalaryPostingProvider.java`（IErpFinAcctDocProvider:36）；`module-hr/model/app-erp-hr.orm.xml`（leave-status:50-56/timesheet-status:57-62/salary-payment-status:72-76/salary-approval-status orphan:77-84/simulation-status:148-154/bank-file-status:132-136/swap-status:166-171 + ErpHrLeaveRequest:481-530 status:493/ErpHrAttendance:665-711 isAbsent:677/ErpHrTimesheet:577-619 status:587/ErpHrShiftAssignment:1170-1228 status:1186 无 dict+isAbsent:1181/ErpHrShiftSwapRequest:1266-1324 status:1279/ErpHrSalary:714-782 approveStatus:736+paymentStatus:735+posted:758/ErpHrSalarySimulation:854-907 status:865/ErpHrPayrollBankFile:1095-1130 status:1106）；`docs/design/human-resource/state-machine.md`+`payroll.md`+`payroll-simulation.md`+`shift-scheduling.md`；服务层 `TestErpHrLeaveEngine`+`TestErpHrAttendanceEngine`+`TestErpHrShiftScheduling`+`TestErpHrSalaryWorkflowApproval`+`TestErpHrPayrollEngine`+`TestErpHrPayrollSimulation`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P2-MA1-020 orphan dict + P1-MA1-022 跨域只读已登记待 MR1，本审计复核状态机角度）；A2.5a/b/c done（finance 状态机 dict 死状态 + posted 双轴 + 过账 tryPost 容错同型裁决范式 P1-MA2-031/032）；A2.6a/b done（mfg 状态机 posted 标记 + 过账 dispatcher tryPost 同型范式）；A2.7a done（员工 employmentStatus=ACTIVE/PROBATION 是 runPayroll 前置——A2.7a 复核员工侧）

- [x] 维度「状态定义」：审查考勤 isAbsent 布尔（无 enum——是否应建模为状态）；排班分配 status 无 dict（raw VARCHAR + 常量——清晰性缺陷）；工时单 APPROVED/REJECTED 等待点；工资双轴 approveStatus×paymentStatus 组合语义；仿真 CONVERTED 终态；银行文件 UPLOADED/CONFIRMED 等待点（桩未实现）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：列出工时单每个状态传入/传出——**DRAFT→SUBMITTED 实现 / SUBMITTED→APPROVED/REJECTED 未实现**（迁移缺失——重点）；排班分配多 writer 迁移矩阵（assign SCHEDULED / calcAttendance PRESENT/ABSENT / onLeaveApproved ABSENT / onLeaveCancelled SCHEDULED / regenerate CANCELLED / swap 交换）；换班 4 态全迁移 + approve 副作用（交换 shiftId + 重置 SCHEDULED）；工资支付轴 PENDING→PAID/VOID + 批量 generateBankFile + 审批轴委托平台；仿真 5 态全迁移；银行文件 **GENERATED→UPLOADED/CONFIRMED 未实现**（桩）；请假 5 态全迁移 + approve/cancel 触发排班联动。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：工资 PAID/VOID 终态（VOID 不可恢复？voidSalary 守卫拒已 PAID）；仿真 CONVERTED 终态（不可恢复）；请假 APPROVED→CANCELLED（cancel 红冲恢复 + 排班回退，非真终态）/ REJECTED 终态；排班分配 CANCELLED 终态；考勤打卡无终态（每日记录）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——请假余额不足（守卫拒绝）/ 日期重叠（守卫拒绝）/ 重复打卡（幂等）/ markPaid 未审批（双守卫拒绝）/ voidSalary 已 PAID（守卫拒绝）/ **工资过账 tryPostPayment 吞异常→posted=false 悬挂**（与 mfg posting dispatcher tryPost 同型容错——重点核验是否有悬挂告警闭环 / 期末结账前置检查兜底）/ 仿真 convertToFormal 冲突（per-employee skip + all-conflict throw）/ onLeaveCancelled leaveRequestId 不匹配（仅回退匹配项——部分回退？）/ runPayroll 批量单失败隔离。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：**重点——工时单 APPROVED/REJECTED 是否可达**（无 approve/reject writer → dict 死状态，同 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决）；**银行文件 UPLOADED/CONFIRMED 是否可达**（桩无 writer → 死状态）；排班分配 CANCELLED 可达性（仅 regenerate）；工资 REJECTED approveStatus 可达性（平台审批动作）；仿真从 DRAFT 到 CONVERTED 可达性；是否有死循环或不可达终态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个转换绑定执行角色——请假 submit（员工）/approve（主管）/工资 approve（财务主管）/markPaid（出纳）/runPayroll（HR）/换班 approve（主管）；危险操作（**markPaid 触发 finance 凭证过账跨域写会计保护区域** / voidSalary 已过账工资作废 / generateBankFile 批量付款影响资金）；多角色冲突（HR runPayroll vs 财务 approve vs 出纳 markPaid）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：**工资过账经 IErpFinVoucherBiz**（finance 凭证链 REQUIRES_NEW 跨域写会计保护区域——重点核验状态迁移与过账一致性 + tryPostPayment 吞异常语义）；工资计提/付款 IErpFinAcctDocProvider（Provider 反向注册）；员工净余额报表 IErpFinArApItemBiz（只读）；请假 approve→排班 onLeaveApproved（同域跨实体）；外部步骤失败是否阻断状态迁移（SalaryPostingDispatcher 吞异常——状态迁移与过账解耦）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：每个非终端状态是否产生正确类型待办——请假 SUBMITTED 主管审批 TODO；工资 SUBMITTED 财务审批 TODO；银行文件 GENERATED 出纳上传 TODO（桩未实现——静默？）；工时单 SUBMITTED 审批 TODO（approve 未实现——长期 SUBMITTED 静默下沉？）；考勤缺卡 calcAttendance 置 ABSENT 是否告警；仿真 IN_REVIEW 审批 TODO；是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 请假快乐路径（DRAFT→submit→SUBMITTED→approve→APPROVED + 余额扣减 + 排班 ABSENT）；(b) 请假 reject/cancel（+ 排班回退 SCHEDULED）；(c) 考勤打卡（clockIn→clockOut + calcAttendance）；(d) **工时单审批**（DRAFT→SUBMITTED→APPROVED/REJECTED——**未实现，演练确认缺口**）；(e) 排班分配生命周期（SCHEDULED→PRESENT/ABSENT→regenerate CANCELLED）；(f) 换班（PENDING→approve→交换+重置）；(g) 工资双轴（UNSUBMITTED→平台审批→APPROVED→markPaid→PAID + 凭证 + generateBankFile）；(h) 工资 void（PENDING→VOID / 已 PAID 拒绝）；(i) 仿真全链（→CONVERTED + 创建正式工资）；(j) **银行文件**（GENERATED→UPLOADED→CONFIRMED——**桩未实现**）；(k) **工资过账失败**（tryPostPayment 吞异常→posted=false 悬挂？）；(l) 并发 markPaid 同工资（无 @Version，交接 A2.17）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`payroll.md`/`payroll-simulation.md`/`shift-scheduling.md` 是否有匹配——**重点漂移**：(1) **工时单 APPROVED/REJECTED owner doc 声明但未实现**（漂移）；(2) **银行文件 UPLOADED/CONFIRMED owner doc 声明但桩未实现**（漂移）；(3) **排班分配 status 无 dict**（owner doc 是否声明——清晰性缺陷）；(4) **工时单硬编码字符串** vs ErpHrConstants（不一致）；(5) orphan dict salary-approval-status 6 态 vs 实际 wf/approve-status 4 态（owner doc §审批状态标准化 已声明废弃——确认一致性）；(6) **SalaryPostingDispatcher javadoc L27-28 "无 posted 字段" vs ORM:758 posted**（doc/code drift——重点核验）；(7) 工资过账 tryPostPayment 吞异常 owner doc 是否声明容错语义。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 考勤/工资状态机角度：P2-MA1-020（orphan dict——确认工资状态机无悬挂）/ P1-MA1-022（daoFor 跨域只读——状态机角度无升级）。标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（含：请假/考勤/工时单/排班分配/换班/工资双轴/仿真/银行文件状态图、各维度通过/失败裁决、控制点 PASS/FAIL、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。仅本阶段交付的本地化检查列在此。

- [x] 请假（5 态）+ 考勤（isAbsent）+ 工时单（4 态）+ 排班分配（4 值无 dict）+ 换班（4 态）+ 工资双轴（approveStatus 4+paymentStatus 3+posted）+ 仿真（5 态）+ 银行文件（3 态）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义 / 转换完整性[含工时单 approve/reject + 银行文件未实现] / 终端与恢复 / 异常路径[含工资过账吞异常悬挂] / 可达性[含工时单/银行文件死状态] / 角色权限 / 外部依赖[含工资过账跨域写] / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 考勤/工资状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 hr 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**工时单 approve/reject 迁移完全缺失致 APPROVED/REJECTED 死状态** [按同型裁决 P1 dict 死状态非 P0，不破坏已实现主路径] / **工资过账 tryPostPayment 吞异常致 posted=false 悬挂无告警闭环** [若破坏业财一致——需核验期末结账前置检查是否兜底；若仅治理缺陷则 P1] / **银行文件 UPLOADED/CONFIRMED 未实现致付款文件生命周期断裂** [若影响资金流——config-gated/Deferred 则 P1]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计/资金保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。注意：本审计对已登记 finding（P2-MA1-020/P1-MA1-022）只复核状态机运行时影响不重复登记根因；若发现新 P1（如工时单 APPROVED/REJECTED 死状态 [同 finance P1-MA2-031 同型] / 银行文件 UPLOADED/CONFIRMED 死状态 / 排班分配 status 无 dict 清晰性缺陷 / 工时单硬编码字符串 / SalaryPostingDispatcher javadoc vs ORM posted drift / 工资过账 posted=false 悬挂无告警闭环 [同 finance P1-MA2-032 IGNORED 悬挂同型]）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 hr 列终态标记（`⚠️(P1)(A2.7a✅;A2.7b❓)` → `⚠️(P1)(A2.7a✅;A2.7b✅)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05b35bef6ffepVtabT884kdpFx`，独立 general 子代理，fresh-context，对照实时仓库逐行复核）。VERDICT = accept，**无 BLOCKER**。核实要点：6 个 dict（leave-status:50-56/timesheet-status:57-62/salary-payment-status:72-76/simulation-status:148-154/bank-file-status:132-136/swap-status:166-171 + orphan salary-approval-status:77-84）行号精确 ✓；**ErpHrTimesheetBizModel 47 行仅 submit + 硬编码 "DRAFT"/"SUBMITTED" L38/L43 + 无 approve/reject** ✓；**ErpHrShiftAssignment.status:1186 无 ext:dict raw VARCHAR + 无 assignment-status.dict.yaml** ✓；**ErpHrPayrollBankFileBizModel 18 行 CRUD 桩无迁移** ✓；**Salary 3 列（approveStatus wf/approve-status:736 + paymentStatus:735 + posted:758）** ✓；**SalaryPostingDispatcher 155 行 tryPostPayment 吞异常返回 boolean + javadoc L27-28 "无 posted 字段" vs ORM L758 posted drift** ✓；module-hr 无 .xbiz.xml + 无 *Processor.java ✓；测试文件 + owner doc + 关联计划 + P2-MA1-020/P1-MA1-022 登记均存在 ✓。检查清单全 PASS（基线极其精确——行号逐一吻合；单一结果表面考勤与工资；Item 类型；技能匹配；反松弛无禁用词；不可降级项工时单/银行文件死 dict + posted 悬缺路由 P0/P1；结束门控含独立审计+全量验证在 Closure Gates；退出标准可观察）。**非阻塞说明**：scope matrix hr 列当前实仓为 `❓S拆`，本计划 Goals 写的 `⚠️(P1)(A2.7a✅;A2.7b❓)` 是 A2.7a 先执行后的前瞻状态（A2.7a 先执行将 `❓S拆`→`⚠️(P1)(A2.7a✅;A2.7b❓)`，本审计再推进至 `...A2.7b✅`），与 prereq 链一致无需修改。Plan Status 转 active。

## Closure Audit Record

- Independent closure audit iteration 1: **pass after blocker fix**（`ses_059fdadc2ffeo6hY19M2Oscivb`，独立 general 子代理，fresh-context，对照实时仓库逐项验证 closure gates）。初始 VERDICT = **fail**（1 BLOCKER：roadmap `docs/backlog/audit-remediation-roadmap.md` A2.7b 行未从 `todo` 推进至 `done` + header 未更新——执行者遗漏了 post-completion step 4b「roadmap 工作项 ❌→✅」）。其余 7 gates 全 PASS：审计报告 431 行实质内容 + Verdict pass 零 P0 + 8 组件全覆盖 + 10 维度表 + 6 P1 + 1 P2 + MA1 复核表 ✓；arm-index 报告行 done + P1-MA2-043~048 + P2-MA2-052 + A2.7b summary + intro 段 ✓；scope matrix hr 列 `⚠️P1(A2.7a✅;A2.7b✅)` + 叙述段 ✓；git status 仅 docs/ 修改零代码变更 ✓；BUILD SUCCESS 信任执行者证据 ✓；finding ID 连续无冲突（A2.7a 止 042/051 → A2.7b 起 043/052）✓。**Blocker 修复**：执行者将 roadmap A2.7b `todo`→`done` + header v10→v11 + 补 A2.7b 完成摘要。修复后 closure audit 判 pass，Closure Gates 8 项全 `[x]`。Plan Status `completed`。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。工资过账触及会计/资金保护区域，P0 即时修复须额外人工确认。

- [x] 范围内行为完成（A2.7b 考勤与工资状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/payroll/payroll-simulation/shift-scheduling owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-hr/erp-hr-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.7a 员工与组织类状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 那是 S 级拆分 1/2，先执行（done）。本审计只确认工资 runPayroll 依赖员工 employmentStatus=ACTIVE/PROBATION（A2.7a 复核员工侧）。
- Successor Required: `no`——A2.7a 先于本计划执行（done）。

### A4.4 hr 代码质量审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做考勤/工资状态机**业务正确性**审查；BizModel/Calculator 代码质量（异常处理/N+1/索引）系统性审查归 A4.4。
- Successor Required: `yes`——A4.4 执行时复核。

### A2.17 并发与乐观锁（并发 markPaid/runPayroll）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（markPaid/runPayroll 无 @Version / generateBankFile 批量 / 排班 calcAttendance 竞态），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### owner doc 已裁定 Deferred 偏离本身（银行回单自动对账 / 工时单自动审批 / 考勤 IoT 集成 / 个税精算）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 Deferred/Non-Goal。本审计只确认其在状态机上不引入悬挂（死状态归状态定义清晰性维度裁决为 P1/P2 dict 死状态清理或 owner doc 标注 Deferred 而非实现）。
- Successor Required: `yes`——各 successor 触发条件满足时（如银行回单自动对账上线 / 工时单自动审批 / 考勤 IoT / 个税精算）。

## Closure

Status Note: A2.7b hr 考勤与工资状态机系统性业务审查完成。八组件（请假 5 态 + 考勤布尔 + 工时单 4 态 + 排班分配 4 值 + 换班 4 态 + 工资双轴 approveStatus 4+paymentStatus 3+posted + 仿真 5 态 + 银行文件 3 态）核心契约经实仓逐项证据确认；零 P0（六个候选 P0 经证据证伪或按同型裁决降 P1）；6 项新 P1（P1-MA2-043~048）+ 1 项新 P2（P2-MA2-052）登记 arm-index 待 MR1；MA1 finding 运行时复核无升级。scope matrix §状态机正确性 hr 列推进至 `⚠️P1(A2.7a✅;A2.7b✅)`。Plan Status `completed`。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（fresh-context），会话 `ses_059fdadc2ffeo6hY19M2Oscivb`（详见上方 `## Closure Audit Record` iteration 1）——初始 VERDICT = fail（1 BLOCKER：roadmap A2.7b 行未从 `todo` 推进至 `done` + header 未更新），执行者修复后复判 pass，Closure Gates 8 项全 `[x]`
- Evidence: 审计报告 `docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（53847 bytes，431+ 行，10 维度裁决表 + 8 组件状态图 + Verdict pass 零 P0）；arm-index.md:33 报告行 `done` + P1-MA2-043~048 + P2-MA2-052 登记（待 MR1）；scope matrix `audit-remediation-scope-and-dimension-matrix.md:103` hr 列 `⚠️P1(A2.7a✅;A2.7b✅)`；roadmap A2.7b 行 `todo`→`done` + header v10→v11；git status 仅 `docs/` 修改零代码变更；零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-hr/erp-hr-service -am` 作回归基线确认 BUILD SUCCESS
- Daily Log: `docs/logs/2026/07-28.md`（A2.7b 完成摘要已记录）

Follow-up:

- 6 项 P1（P1-MA2-043~048）+ 1 项 P2（P2-MA2-052）经 R1.0 展开机制进入 MR1 批量修复（已登记 arm-index §P1 汇总，非阻塞跟进）
- 并发敏感点 5 处（markPaid/runPayroll 无 @Version / generateBankFile 批量 / 排班 calcAttendance 竞态 / 排班分配多 writer / 仿真 convertToFormal）交接 A2.17 系统性并发正确性裁决
