# ARM MA2 — hr 考勤与工资状态机业务审查（A2.7b，S 级拆分 2/2）

> Audit Status: closed
> Mission: audit-remediation
> Work Item: A2.7b hr 状态机审查 — 考勤与工资（S 级拆分 2/2）
> Source Plan: `docs/plans/2026-07-28-0230-2-audit-remediation-ma2-hr-attendance-payroll-state-machine.md`
> Skill: `docs/skills/state-machine-business-review-prompt.md`（+ 项目定制化层 `docs/skills/README.md §项目定制化层`）
> Reviewed: 2026-07-28
> Scope: **请假审批状态机**（`ErpHrLeaveRequest.status` dict `erp-hr/leave-status` 5 态）+ **考勤打卡**（`ErpHrAttendance` 无 enum status，仅 `isAbsent` 布尔 + clockIn/clockOut）+ **工时单状态机**（`ErpHrTimesheet.status` dict `erp-hr/timesheet-status` 4 态）+ **排班分配状态机**（`ErpHrShiftAssignment.status` 无 dict 绑定 raw VARCHAR(50)，4 值经 `ErpHrConstants.ASSIGNMENT_STATUS_*` 常量）+ **换班申请状态机**（`ErpHrShiftSwapRequest.status` dict `erp-hr/swap-status` 4 态）+ **工资审批-支付双轴状态机**（`ErpHrSalary.approveStatus` dict `wf/approve-status` 4 态 + `paymentStatus` dict `erp-hr/salary-payment-status` 3 态 + `posted` 布尔）+ **薪酬仿真状态机**（`ErpHrSalarySimulation.status` dict `erp-hr/simulation-status` 5 态）+ **银行付款文件状态机**（`ErpHrPayrollBankFile.status` dict `erp-hr/bank-file-status` 3 态）。员工/合同/招聘/考核/发展计划归 A2.7a（已 done）。
> Related: A2.5a/b/c finance 状态机审查三拆分全 done（`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` + `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` + `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`，dict 死状态 + IGNORED 悬挂同型裁决范式 P1-MA2-031/032）；A2.6a/b manufacturing 状态机审查两拆分全 done（`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md` + `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`，dict 死状态 + owner doc 漂移 + posted 标记双轴 + 过账 dispatcher tryPost 容错同型裁决范式 P1-MA2-035/036/037/038）；A2.7a hr 员工与组织状态机审查（S 级拆分 1/2）已 done（`docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`，P1-MA2-039/040/041/042 + P2-MA2-047~051）；A2.3 期末结账端到端 done（工资过账经 finance 凭证链 PostingDispatcher 同型范式）；`docs/design/human-resource/state-machine.md`（请假/考勤/工时单/工资审批-支付双轴 owner doc）+ `payroll.md` + `payroll-simulation.md` + `shift-scheduling.md`（owner doc）。

## 1. 裁决

**Verdict: pass（零 P0、6 项新 P1、1 项新 P2 watch-only）**

hr 考勤与工资八组件状态机（请假 5 态 + 考勤布尔 + 工时单 4 态 + 排班分配 4 值 + 换班 4 态 + 工资双轴 approveStatus 4+paymentStatus 3+posted + 仿真 5 态 + 银行文件 3 态）核心契约经实仓逐项证据确认：**主路径状态迁移守卫齐全**（请假 5 态全迁移 + approve/cancel 触发排班联动 / 工资支付轴 PENDING→PAID/VOID 双守卫 / 仿真 5 态全迁移 / 换班 4 态全迁移 + approve 副作用交换 shiftId）、`@BizMutation` 事务回滚保证请假→排班联动失败原子性、工资 markPaid 触发跨域过账经 `IErpFinVoucherBiz.post()` REQUIRES_NEW Facade（hr production 代码无 `daoFor(ErpFin*)` 直写已确认）、请假 cancel 红冲恢复经 `onLeaveCancelled` leaveRequestId 匹配回退排班 SCHEDULED、仿真 convertToFormal per-employee 冲突 skip + all-conflict throw 双层容错、银行文件 generateBankFile 批量设 PAID + 创建 GENERATED 文件。

**关键裁决（计划假设证伪/确认）**：

| 计划假设 | 裁决 | 证据 |
|---|---|---|
| 工时单 APPROVED/REJECTED dict 死状态 + approve/reject 迁移完全缺失（候选 P0） | **确认死状态 + 降 P1** | `ErpHrTimesheetBizModel.java`（47 行全文读）**仅 `submit:35-46`（DRAFT→SUBMITTED）+ 守卫 L38 硬编码 `"DRAFT"` + L43 硬编码 `"SUBMITTED"`**——**无 approve/reject BizMutation 方法**。`TIMESHEET_STATUS_APPROVED/REJECTED` 常量定义于 `ErpHrConstants.java`（同包其他状态常量模式），dict `erp-hr/timesheet-status` 4 态（DRAFT/SUBMITTED/APPROVED/REJECTED，dict.yaml 确认）。owner doc `state-machine.md §适用对象三 工时表 L175-201` 明确声明「DRAFT→submit→SUBMITTED→approve→APPROVED（终态：审核通过，工时归集到项目成本）/ reject→REJECTED（员工修改后重新提交）」+ §场景 F L196-201 演练全链。**APPROVED/REJECTED 两态确认不可达（dict 死状态）**。但**不破坏主路径**——DRAFT→SUBMITTED 完整覆盖工时录入提交生命周期；SUBMITTED 是审批等待点（owner doc 标注「等待项目经理审批」），approve/reject 迁移缺失意味着工时单长期停留在 SUBMITTED，但**不影响已实现路径运行时正确性**（无悬挂半状态、无数据错误）。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + A2.6b P1-MA2-036 + hr A2.7a P1-MA2-039/040/041/042 同型裁决：dict 项不可达 + owner doc 声明但代码无实现 → **P1-MA2-043**（MR1 实现 approve/reject 迁移 + 工时归集 projects/cost-collection 或 owner doc §适用对象三 + §场景 F 标注 Deferred + 删除 dict 两项）。**非 P0**——主路径未破坏、无悬挂半状态、无数据错误。 |
| 工时单 BizModel 硬编码 "DRAFT"/"SUBMITTED" 字符串（候选 P1） | **确认 P1** | `ErpHrTimesheetBizModel.java:38` `Objects.equals(timesheet.getStatus(), "DRAFT")` + L43 `timesheet.setStatus("SUBMITTED")`——**硬编码字符串**而非 `ErpHrConstants.TIMESHEET_STATUS_*` 常量。与同域其他 BizModel（`ErpHrLeaveRequestBizModel` 用 `LEAVE_STATUS_*` / `ErpHrSalaryBizModel` 用 `PAYMENT_*` / `ErpHrSalarySimulationBizModel` 用 `SIMULATION_STATUS_*` / `ErpHrShiftSwapRequestBizModel` 用 `SWAP_STATUS_*`）**不一致**。`ErpHrConstants.java` 已定义 timesheet 常量（同包模式），但本类未引用。**P1-MA2-044**（MR1 改用常量——纯一致性/可维护性缺陷，无运行时影响）。 |
| 银行文件 UPLOADED/CONFIRMED dict 死状态 + 桩 BizModel（候选 P0/P1） | **确认死状态 P1** | `ErpHrPayrollBankFileBizModel.java`（18 行全文读）**仅 CRUD 继承**——`extends CrudBizModel<ErpHrPayrollBankFile>` 无任何状态迁移方法。`BANK_FILE_STATUS_UPLOADED/CONFIRMED` 常量定义于 `ErpHrConstants.java:72`（`BANK_FILE_STATUS_GENERATED`）+ 后续 UPLOADED/CONFIRMED 常量，仅 `ErpHrSalaryBizModel.generateBankFile:168` 设 `BANK_FILE_STATUS_GENERATED`——**UPLOADED/CONFIRMED 无任何 setStatus writer**。owner doc `payroll.md §七 银行文件生成 L439+` + `state-machine.md L255`（「银行文件生成：查询 paymentStatus=PENDING AND approveStatus=APPROVED」）声明银行文件生命周期（生成→上传→确认），dict `erp-hr/bank-file-status` 3 态（GENERATED/UPLOADED/CONFIRMED，dict.yaml 确认）。**UPLOADED/CONFIRMED 两态确认不可达（dict 死状态）**。但**不破坏主路径**——银行文件生成 + 批量 PAID 完整覆盖薪酬发放主路径；UPLOADED/CONFIRMED 是 owner doc Deferred 能力（银行回单自动对账 + 实际转账执行归 successor，payroll.md L11「本设计不负责：银行实际转账执行」），缺失状态机不产生悬挂数据（已 PAID 工资不依赖银行文件状态确认）。按同型裁决 → **P1-MA2-045**（MR1 实现 upload/confirm 迁移或 owner doc §七 标注「银行回单对账 Deferred」+ 删除 dict 两项）。**非 P0**——config-gated/Deferred 语义，不影响资金流（generateBankFile 已批量设 PAID，资金流在 PAID 时确认而非银行文件确认时）。 |
| 排班分配 status 无 dict 绑定 raw VARCHAR（候选 P1 清晰性缺陷） | **确认 P1 清晰性缺陷** | ORM `app-erp-hr.orm.xml:1186` `<column name="status" stdSqlType="VARCHAR" precision="50" stdDataType="string"/>`——**无 `ext:dict` 属性**（对比同文件 :493 leave-status / :587 timesheet-status / :735 salary-payment-status / :865 simulation-status / :1106 bank-file-status / :1279 swap-status 均有 `ext:dict`）。`module-hr/erp-hr-meta/src/main/resources/_vfs/dict/erp-hr/` **无 `assignment-status.dict.yaml`**（dict.yaml 清单逐项确认）。4 值经 `ErpHrConstants.ASSIGNMENT_STATUS_*`（:98-101 SCHEDULED/PRESENT/ABSENT/CANCELLED）常量定义。owner doc `shift-scheduling.md §二 L85` 声明「status \| dict：SCHEDULED（已排班）/ PRESENT（已到岗）/ ABSENT（缺勤）/ CANCELLED（取消）」——**owner doc 声明 dict 但 ORM 无 dict 绑定（漂移）**。UI/验证器无法枚举合法值（同 mfg A2.6a P2-MA2-044 字典命名漂移同型但更严重——完全无 dict）。**P1-MA2-046**（MR1 裁决——方案 A 新增 `assignment-status.dict.yaml` + ORM 加 `ext:dict="erp-hr/assignment-status"`；方案 B owner doc §二 标注「status 为内部常量，无 dict 绑定」）。**非 P0**——状态值经常量集中管理，运行时正确性不受影响（仅 UI/验证层缺陷）。 |
| SalaryPostingDispatcher javadoc "无 posted 字段" vs ORM posted（候选 P1 doc/code drift） | **确认 P1 doc/code drift + posted 字段死字段** | `SalaryPostingDispatcher.java:27-28` javadoc 声称「对齐 assets/projects 失败语义：过账失败吞异常记日志、保持原状态、`posted=false`（**薪酬实体无 posted 字段**，由调用方在日志层判定），不阻塞终态」。**实仓 ORM `app-erp-hr.orm.xml:758` `<column name="posted" stdSqlType="BOOLEAN" defaultValue="false" propId="93"/>`——posted 字段存在**。owner doc `payroll.md:368` 「业财过账 \| `posted` \| boolean \| `PostingDispatcher`」**owner doc 声明 posted 字段存在**。全 `module-hr/erp-hr-service/src/main` grep `setPosted\|\.posted\|getPosted` **零匹配**——**posted 字段从未被任何代码写入或读取（死字段）**。javadoc 漂移 + 死字段双重缺陷。**P1-MA2-047**（MR1 裁决——方案 A 修正 javadoc + markPaid 按 tryPostPayment 结果设 posted=true/false；方案 B 删除 ORM posted 字段 + 修正 javadoc + owner doc payroll.md:368）。**非 P0**——纯文档/治理缺陷，不破坏运行时正确性（markPaid 主路径经 tryPostPayment 容错返回 + PAID 终态）。 |
| 工资过账 tryPostPayment 吞异常致 posted=false 悬挂无告警闭环（候选 P0） | **降 P1（同型裁决）** | `SalaryPostingDispatcher.tryPostPayment:66-79` try/catch 吞所有异常返回 boolean（`NopException` LOG.warn / 其他 LOG.error），**不向上传播**。`ErpHrSalaryBizModel.markPaid:97-118` 调用 `postingDispatcher.tryPostPayment(salary):112` 后**无条件** `salary.setPaymentStatus(PAYMENT_PAID):114`——**不检查返回值、不设 posted**。若 finance 过账引擎异常（如 `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED` 配置缺失 / finance 凭证引擎故障）：(a) salary 进入 PAID 终态；(b) 无 finance 凭证创建；(c) `posted` 永远 false（且永不写入，同 P1-MA2-047）；(d) **异常被 hr dispatcher 吞掉，不进入 finance 过账异常工作台**（finance `ErpFinPostingException` 工作台仅捕获 finance 侧未处理异常，hr dispatcher 已 catch 致 finance 侧无 PENDING 记录）；(e) **期末结账前置检查不覆盖此悬挂**（全 `module-finance/erp-fin-service/src/main/java/.../period/` grep `ErpHrSalary\|salary.*posted\|SALARY` 零匹配——期末结账不扫描 salary PAID-without-voucher）。**比 finance A2.5a P1-MA2-032（IGNORED 悬挂）更严重**——P1-MA2-032 至少有期末结账前置检查 PENDING 异常间接兜底，salary 侧无任何兜底。但**非 P0**：(1) 失败模式需 finance 过账引擎异常（配置错误/基础设施故障，非正常路径）；(2) LOG.warn/error 提供运维可见性（人工监控）；(3) 与 mfg posting dispatcher tryPost 容错 + finance P1-MA2-032 IGNORED 悬挂**同型根因**（按既定裁决范式 P1）；(4) 不破坏 markPaid 主路径（PAID 终态正确，仅凭证缺失）；(5) 业财不一致可经期末试算平衡人工发现（虽无自动门控）。按同型裁决 → **P1-MA2-048**（MR1 裁决——方案 A markPaid 检查 tryPostPayment 返回值 + 失败时设 posted=false + 派发 IErpSysNotificationBiz 告警 + 不进 PAID 终态；方案 B owner doc payroll.md §业财过账 标注「过账失败吞异常为容错设计，业财不一致经期末试算平衡人工发现」+ posted 字段语义化）。 |
| orphan dict salary-approval-status 6 态（已登记 P2-MA1-020，复核状态机角度） | **维持 P2，状态机角度无升级** | dict `erp-hr/salary-approval-status`（6 态 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID，dict.yaml 确认）存在但 `ErpHrSalary.approveStatus` 实际引用 `wf/approve-status`（4 态，ORM:736 确认）。orphan dict 仅在 i18n + javadoc 残留引用，**无 column 使用、无 setStatus writer、无状态机判定依赖**。owner doc `payroll.md §审批状态标准化 L26-29` 明示「裁决将 `approvalStatus`（原 6 态）拆分为标准三轴...原 6 态混入了审批流中间步骤与业务执行终态，违反三轴分离原则。标准化后 approveStatus → 标准 4 态 wf/approve-status」——**owner doc 已声明废弃，orphan dict 是历史残留**。本审计复核确认：orphan dict **在工资状态机上不引入悬挂**（6 态无 writer 无 column → 纯 i18n/javadoc 残留，运行时零影响）。维持 **P2-MA1-020**（MR1 顺手清理：删除 orphan dict 定义 + i18n + javadoc 引用）。 |
| 跨域只读 daoFor(ErpMdPartner)（已登记 P1-MA1-022，复核状态机角度） | **维持 P1-MA1-022，状态机角度无升级** | `ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)` 只读（员工净余额报表聚合）。本审计复核：跨域只读是报表聚合副作用，**不参与任何状态迁移判定、不在状态迁移异常路径**（report BizModel 无 setStatus 调用，纯查询聚合）。状态机角度无升级，维持 **P1-MA1-022**（MR1 治理，9 域合并）。 |

### 1.1 审查范围

- **请假审批状态机**：`ErpHrLeaveRequest`（ORM `orm.xml:481-530`，列 `status` dict `erp-hr/leave-status` 在 :493）+ `ErpHrLeaveRequestBizModel.java`（227 行，5 迁移方法 submit/approve/reject/cancel + 守卫 `requireStatus:136-143`/`checkLeaveBalance:145-165`/`checkDateOverlap:167-187` + 余额聚合 `sumUsedDays:199-213` + 排班联动 `shiftBiz.onLeaveApproved:96`/`onLeaveCancelled:117`）。
- **考勤打卡**：`ErpHrAttendance`（ORM `orm.xml:665-711`，无 status enum 列，仅 `isAbsent` 布尔 :677 + clockIn/clockOut 时间戳）+ `ErpHrAttendanceBizModel.java`（122 行，clockIn/clockOut 幂等守卫 + `computeWorkHours`）。
- **工时单状态机**：`ErpHrTimesheet`（ORM `orm.xml:577-619`，列 `status` dict `erp-hr/timesheet-status` 在 :587）+ `ErpHrTimesheetBizModel.java`（47 行，**仅 submit + 硬编码字符串 + 无 approve/reject**）。
- **排班分配状态机**：`ErpHrShiftAssignment`（ORM `orm.xml:1170-1228`，列 `status` **无 dict 绑定** raw VARCHAR(50) 在 :1186 + `isAbsent` 布尔 :1181）+ 多 BizModel 写入：`ErpHrShiftAssignmentBizModel.java`（205 行，`assignSingle:59-67` SCHEDULED + `activeStatuses:171-177`）+ `ErpHrShiftBizModel.calcAttendance:55-110`（写 ABSENT :74/:86 / PRESENT :103）+ `onLeaveApproved:124-136`（请假范围置 ABSENT）+ `onLeaveCancelled:139-154`（leaveRequestId 匹配回退 SCHEDULED）+ `ErpHrShiftRotationPatternBizModel.deleteExistingAssignments:177-194`（regenerate SCHEDULED→CANCELLED :190）+ `ErpHrShiftSwapRequestBizModel.approve:78-110`（交换 shiftId + 重置 SCHEDULED）。
- **换班申请状态机**：`ErpHrShiftSwapRequest`（ORM `orm.xml:1266-1324`，列 `status` dict `erp-hr/swap-status` 在 :1279）+ `ErpHrShiftSwapRequestBizModel.java`（144 行，4 迁移方法 submit/approve/reject/cancel + `assertTransition:134-142` + approve 副作用交换 shiftId :92-94 + 重置 SCHEDULED :101-102）。
- **工资审批-支付双轴状态机**：`ErpHrSalary`（ORM `orm.xml:714-782`，列 `approveStatus` dict `wf/approve-status` 在 :736 + `paymentStatus` dict `erp-hr/salary-payment-status` 在 :735 + `posted` 布尔 :758）+ `ErpHrSalaryBizModel.java`（254 行，支付轴 markPaid:97-118 双守卫:100-111 + voidSalary:121-131 + generateBankFile:134-178 + calculateSalary/runPayroll + findPayableSalaries:240）。
- **薪酬仿真状态机**：`ErpHrSalarySimulation`（ORM `orm.xml:854-907`，列 `status` dict `erp-hr/simulation-status` 在 :865）+ `ErpHrSalarySimulationBizModel.java`（1129 行，5 迁移方法 createSimulation:70-95/submitForReview:385-402/approve:405-421/reject:424-439/convertToFormal:442-532 + per-employee 冲突 skip :465/:470 + all-conflict throw :510-525）。
- **银行付款文件状态机**：`ErpHrPayrollBankFile`（ORM `orm.xml:1095-1130`，列 `status` dict `erp-hr/bank-file-status` 在 :1106 默认 GENERATED）+ `ErpHrPayrollBankFileBizModel.java`（**18 行 CRUD 桩无状态迁移**）+ `ErpHrSalaryBizModel.generateBankFile:168` 设 GENERATED。
- **跨域过账**：`SalaryPostingDispatcher.java`（155 行，tryPostAccrual:46/tryPostPayment:66 吞异常返回 boolean + javadoc :27-28 drift）+ `SalaryPostingExecutor.java`（包装 `IErpFinVoucherBiz.post()` REQUIRES_NEW）+ `SalaryPostingProvider.java`（178 行，实现跨域 `IErpFinAcctDocProvider` :36，4 业务类型 SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER/HOUSING_FUND_ER）。
- **跨域访问**：`ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)` 只读（员工净余额报表，P1-MA1-022 同型）+ `:78` 注入 `IErpFinArApItemBiz` 只读。**无 `daoFor(ErpFin*)` 直写**（全经 I*Biz Facade，合规）。
- **Processor 类**：**module-hr 无任何 `*Processor.java`**——状态迁移全在 BizModel 内联。`PayrollCalculator`/`IncomeTaxCalculator`/`SocialInsuranceCalculator`（工资计算 helper）+ `ShiftAttendanceCalculator`（考勤计算 helper）是计算非状态编排。
- **owner doc**：`docs/design/human-resource/state-machine.md`（§适用对象一休假 + §适用对象三工时表 + §适用对象四薪酬审批 + §场景 F 工时表提交审批 + L255 银行文件生成）+ `payroll.md`（§五/§六/§七 工资审批-支付双轴 + orphan dict §审批状态标准化 + §七 银行文件生成 + posted 字段 :368）+ `payroll-simulation.md`（仿真 5 态 + convertToFormal）+ `shift-scheduling.md`（§二排班分配 + §四考勤计算 + §五换班 + 请假联动）。
- **测试**：`TestErpHrLeaveEngine`（请假 submit/approve/reject/cancel + 余额 + 重叠）+ `TestErpHrAttendanceEngine`（clockIn/clockOut 幂等）+ `TestErpHrShiftScheduling`（排班/分配/换班/轮班 + calcAttendance）+ `TestErpHrSalaryWorkflowApproval`（工资 approveStatus + paymentStatus markPaid/void）+ `TestErpHrPayrollEngine`（工资计算 + runPayroll）+ `TestErpHrPayrollSimulation`（仿真全链 DRAFT→IN_REVIEW→APPROVED/REJECTED→CONVERTED）。**无独立工时单 approve/reject 测试**（因未实现）+**无银行文件 UPLOADED/CONFIRMED 测试**（因未实现）+**无排班分配 dict 测试**（因无 dict）。

### 1.2 可达性摘要

- **请假 5 态全部可达**：DRAFT（新建 default `defaultPrepareSave:60-62`）→SUBMITTED（`submit:80` 守卫 DRAFT）→APPROVED（`approve:92` 守卫 SUBMITTED + 余额 + 排班联动）/ REJECTED（`reject:105` 守卫 SUBMITTED）；APPROVED→CANCELLED（`cancel:115` 守卫 APPROVED + 排班回退 SCHEDULED）。REJECTED 是终态。
- **考勤无 enum 状态**：每日记录（`isAbsent` 布尔由 `ErpHrShiftBizModel.calcAttendance` 写入），无终态概念。clockIn 幂等守卫（`ERR_ALREADY_CLOCKED_IN:66-69`）+ clockOut 守卫（`ERR_NOT_CLOCKED_IN:80-83`）。
- **工时单 4 态中 2 态可达，2 态不可达**：DRAFT（新建 default）→SUBMITTED（`submit:43` 守卫 DRAFT）；**APPROVED/REJECTED 无任何 setStatus writer → 不可达**（P1-MA2-043）。SUBMITTED 是长期等待点（approve 未实现，工时单静默下沉——但归 owner doc Deferred，非数据缺陷）。
- **排班分配 4 值全部可达（无 dict 但常量集中）**：SCHEDULED（`assignSingle:139` 创建 + `onLeaveCancelled:150` 回退 + swap `approve:101-102` 重置）→PRESENT（`calcAttendance:103` 有打卡）→ABSENT（`calcAttendance:74/:86` 无打卡或 `onLeaveApproved:133` 请假）→CANCELLED（`deleteExistingAssignments:190` regenerate）。CANCELLED 是终态。
- **换班 4 态全部可达**：PENDING（`submit:72` 创建）→APPROVED（`approve:106` 守卫 PENDING + 交换 shiftId）/ REJECTED（`reject:117` 守卫 PENDING）/ CANCELLED（`cancel:127` 守卫 PENDING）。APPROVED/REJECTED/CANCELLED 是终态。
- **工资双轴**：审批轴 4 态经平台 `approval-support.xbiz` DIRECT 模式（module-hr 无 `.xbiz.xml` 覆盖，grep 零匹配确认）；支付轴 3 态——PENDING（calculateSalary/runPayroll 创建 default + 仿真 convertToFormal:501 创建）→PAID（`markPaid:114` 双守卫 + tryPostPayment / `generateBankFile:156` 批量）→VOID（`voidSalary:128` 守卫拒已 PAID）。PAID/VOID 是终态。**posted 字段永不可达（死字段，从未写入）**（P1-MA2-047）。
- **仿真 5 态全部可达**：DRAFT（`createSimulation:92`）→IN_REVIEW（`submitForReview:399` 守卫 DRAFT + 须有调整）→APPROVED（`approve:416` 守卫 IN_REVIEW）/ REJECTED（`reject:435` 守卫 IN_REVIEW）；APPROVED→CONVERTED（`convertToFormal:527` 守卫 APPROVED + per-employee 冲突 skip + 创建正式工资 UNSUBMITTED/PENDING）。REJECTED/CONVERTED 是终态。
- **银行文件 3 态中 1 态可达，2 态不可达**：GENERATED（`generateBankFile:168` 创建）；**UPLOADED/CONFIRMED 无任何 setStatus writer → 不可达**（P1-MA2-045，桩 BizModel + owner doc Deferred 银行回单对账）。

### 1.3 角色/权限摘要

owner doc `state-machine.md` + `payroll.md` + `shift-scheduling.md` + `payroll-simulation.md` 定义角色矩阵（请假 submit=员工 / approve=主管 / 工资 approve=财务主管 / markPaid=出纳 / runPayroll=HR / 换班 approve=主管 / 仿真 approve=HR 总监）。本审计未做权限绑定运行时验证（归 A4.4 hr 代码质量审计 + A6 权限审计），状态机层面无角色漂移反模式。**危险操作门控**：(a) **markPaid 触发 finance 凭证过账跨域写会计保护区域**（`IErpFinVoucherBiz.post()` REQUIRES_NEW）——经 I*Biz Facade 合规，但 tryPostPayment 吞异常致悬挂（P1-MA2-048）；(b) **generateBankFile 批量付款影响资金流**（批量设 PAID + 创建银行文件，资金流在 PAID 时确认）——无 config-gate，但经 @BizMutation 事务回滚保证失败原子性；(c) **voidSalary 已过账工资作废**（守卫拒已 PAID `ERR_SALARY_LOCKED_AFTER_PAID:124-127`）——终态保护正确；(d) **多角色冲突**（HR runPayroll 创建 PENDING → 财务 approve APPROVED → 出纳 markPaid PAID）——双轴状态机分离设计正确，无角色越权路径。**保护区域注意**：P1-MA2-048 修复若触及 `ErpHrSalaryBizModel.markPaid`/`SalaryPostingDispatcher`，须 owner doc 描述预期行为 + 独立 plan-audit + 人工确认（会计保护区域）。

### 1.4 外部依赖摘要

- **跨域写 finance 凭证**：工资过账经 `IErpFinVoucherBiz.post()`（finance 凭证链 REQUIRES_NEW 跨域写会计保护区域）+ `IErpFinAcctDocProvider`（Provider 反向注册，`SalaryPostingProvider` :36）——**hr production 代码无 `daoFor(ErpFin*)` 直写**（全经 I*Biz Facade，合规）。
- **跨域只读**：`ErpHrReportBizModel.java:268` `daoFor(ErpMdPartner)` 只读（P1-MA1-022 同型，状态机角度无升级）+ `:78` `IErpFinArApItemBiz` 只读（员工净余额报表）。
- **同域跨实体**：请假 approve→排班 `onLeaveApproved`（置 ABSENT + leaveRequestId）+ cancel→`onLeaveCancelled`（leaveRequestId 匹配回退 SCHEDULED）+ 排班 calcAttendance→考勤（写 isAbsent）+ 仿真 convertToFormal→正式工资（创建 UNSUBMITTED/PENDING）+ 换班 approve→排班分配（交换 shiftId + 重置 SCHEDULED）。**外部步骤失败不阻断状态迁移**：SalaryPostingDispatcher 吞异常→状态迁移与过账解耦（P1-MA2-048）。
- **依赖员工 employmentStatus**：`ErpHrSalaryBizModel.findActiveEmployees:232-238` filter `ACTIVE/PROBATION`——依赖 A2.7a 复核的员工状态机（A2.7a done，员工在职状态正确）。

### 1.5 并发敏感点（交接 A2.17）

- **markPaid 无 @Version 透明乐观锁**：`ErpHrSalary` ORM 确认有 `versionProp`（透明乐观锁），并发 markPaid 同工资→detectable conflict（降级，非 P0/P1）。交接 A2.17。
- **generateBankFile 批量设 PAID 无锁**：`findPayableSalaries:240-247` 读 PENDING→批量 `setPaymentStatus(PAYMENT_PAID):156`，并发 generateBankFile + markPaid 同工资可双读双写。ErpHrSalary versionProp 乐观锁降级为 detectable conflict。交接 A2.17。
- **排班 calcAttendance 竞态**：`ErpHrShiftBizModel.calcAttendance:55-110` 读 assignment→写 assignment+attendance，并发 calcAttendance 同员工同日可竞态。ErpHrShiftAssignment/ErpHrAttendance versionProp 乐观锁降级。交接 A2.17。
- **runPayroll 批量无锁**：`runPayroll:79-94` 遍历 ACTIVE/PROBATION 员工批量计算，并发 runPayroll + calculateSalary 同员工经 `assertNotDuplicated:72` 守卫 + ErpHrSalary versionProp 降级。交接 A2.17。
- **onLeaveApproved/onLeaveCancelled 排班批量写**：请假 approve/cancel 触发排班批量 setStatus，并发 approve+cancel 同请假经 `requireStatus` 守卫互斥（请假 status 单点仲裁）。交接 A2.17。

**降级重要事实**：ErpHrSalary / ErpHrLeaveRequest / ErpHrShiftAssignment / ErpHrShiftSwapRequest / ErpHrSalarySimulation / ErpHrAttendance / ErpHrPayrollBankFile 7 个 hr 状态机实体**全部声明 versionProp**（透明乐观锁），silent lost-update → detectable conflict（stale 异常需重试）。交接 A2.17 系统性并发审计。

## 2. 状态机逐组件审查

### 2.1 请假审批状态机（ErpHrLeaveRequest，5 态）

#### 状态定义（PASS）

dict `erp-hr/leave-status`（dict.yaml 确认）：DRAFT（草稿，编辑中）/ SUBMITTED（已提交，等待主管审批）/ APPROVED（已批准，等待休假执行 + 排班联动激活）/ REJECTED（已驳回，终态）/ CANCELLED（已取消，红冲恢复 APPROVED 后的请假）。每个状态清楚表达等待点（DRAFT=等待提交 / SUBMITTED=等待审批 / APPROVED=等待休假执行或取消 / REJECTED+CANCELLED=终态）。

#### 转换矩阵（PASS）

| 源 → 目标 | 触发 | 守卫 | 副作用 | 裁决 |
|---|---|---|---|---|
| ∅→DRAFT | 新建（defaultPrepareSave:60-62） | — | 设 status=DRAFT | PASS |
| DRAFT→SUBMITTED | submit:74-83 | requireStatus(DRAFT):76 + checkLeaveBalance:78 + checkDateOverlap:79 | 设 status=SUBMITTED | PASS |
| SUBMITTED→APPROVED | approve:87-98 | requireStatus(SUBMITTED):89 + checkLeaveBalance:91（二次校验防 submit 后余额变化） | 设 APPROVED + approvedAt + approverId + **shiftBiz.onLeaveApproved:96**（排班置 ABSENT） | PASS |
| SUBMITTED→REJECTED | reject:102-108 | requireStatus(SUBMITTED):104 | 设 REJECTED（终态） | PASS |
| APPROVED→CANCELLED | cancel:112-119 | requireStatus(APPROVED):114 | 设 CANCELLED（红冲恢复）+ **shiftBiz.onLeaveCancelled:117**（排班回退 SCHEDULED） | PASS |

非法跳转守卫齐全（requireStatus 拒非预期态）。无非法向前/向后跳转。余额校验在 submit + approve 双点（防 submit 后余额被其他请假消耗）。

#### 终端与恢复（PASS）

- REJECTED：终态（无出边），合法业务结束（请假被拒）。
- CANCELLED：APPROVED 后的红冲恢复出口（非真终态，是 APPROVED 的撤销），经 cancel 迁移 + 排班回退。owner doc 设计正确。
- 无重新激活路径需求（REJECTED 是终态，重新申请经新建 DRAFT）。

#### 异常路径（PASS）

- 余额不足：`checkLeaveBalance:156-164` 守卫拒绝（`ERR_LEAVE_BALANCE_INSUFFICIENT`）。
- 日期重叠：`checkDateOverlap:183-185` 守卫拒绝（`ERR_LEAVE_DATE_OVERLAP`），排除自身（excludeSelf）。
- 重复提交：requireStatus 拒非 DRAFT（已 SUBMITTED/APPROVED 的请假不可重复 submit）。
- approve 时余额变化：approve 二次校验 checkLeaveBalance（防 submit 后其他请假消耗余额）。
- cancel 排班部分回退：`onLeaveCancelled:146` 仅回退 leaveRequestId 匹配的排班（设计正确——避免回退其他请假的 ABSENT 标记）。

#### 可达性（PASS）

5 态全部可达（见 §1.2）。无死循环、无不可达终态。

#### 场景演练（PASS）

- **(a) 快乐路径**：DRAFT→submit（余额校验 + 重叠校验）→SUBMITTED→approve（二次余额 + 排班 onLeaveApproved 置 ABSENT）→APPROVED。余额扣减经 `sumUsedDays` 聚合 APPROVED 请假。
- **(b) reject/cancel**：SUBMITTED→reject→REJECTED（终态）/ APPROVED→cancel（排班 onLeaveCancelled 回退 SCHEDULED）→CANCELLED。
- 余额重叠校验在 submit + approve 双点正确。

#### 与设计文档一致性（PASS）

owner doc `state-machine.md §适用对象一 休假` + `shift-scheduling.md §四 请假联动` 与实现一致。无漂移。

### 2.2 考勤打卡（ErpHrAttendance，无 enum status，isAbsent 布尔）

#### 状态定义（PASS with note）

考勤**无 enum status 列**——仅 `isAbsent` 布尔 + clockIn/clockOut 时间戳。设计选择：考勤是每日记录（非工作流对象），isAbsent 布尔由 `ErpHrShiftBizModel.calcAttendance` 写入（基于排班 + 打卡计算）。**非状态机对象**——是每日快照记录。无反模式（isAbsent 是条件字段，非状态）。无需建模为状态机。

#### 转换矩阵（PASS）

clockIn（无→有打卡）+ clockOut（有 clockIn→有 clockOut）+ calcAttendance 写 isAbsent/lateMinutes/earlyLeaveMinutes。幂等守卫齐全（clockIn 拒已打卡 `ERR_ALREADY_CLOCKED_IN:66-69` / clockOut 拒未打卡 `ERR_NOT_CLOCKED_IN:80-83`）。

#### 异常路径（PASS）

重复打卡幂等守卫正确。calcAttendance 缺打卡置 ABSENT（`absentByNoClock:80-81`）。

#### 与设计文档一致性（PASS）

owner doc `shift-scheduling.md §四 考勤计算` 与实现一致。无漂移。

### 2.3 工时单状态机（ErpHrTimesheet，4 态）— **FAIL（P1-MA2-043 + P1-MA2-044）**

#### 状态定义（PASS）

dict `erp-hr/timesheet-status`（dict.yaml 确认）：DRAFT（草稿）/ SUBMITTED（已提交，等待审批）/ APPROVED（已批准，终态：工时归集项目成本）/ REJECTED（已驳回）。每个状态清楚表达等待点。

#### 转换矩阵（**FAIL**）

| 源 → 目标 | 触发 | 守卫 | 副作用 | 裁决 |
|---|---|---|---|---|
| ∅→DRAFT | 新建 | — | — | PASS |
| DRAFT→SUBMITTED | submit:36-46 | 硬编码 `"DRAFT"`:38 | 设 硬编码 `"SUBMITTED"`:43 | **FAIL（P1-MA2-044 硬编码字符串）** |
| SUBMITTED→APPROVED | **未实现**（无 approve 方法） | — | — | **FAIL（P1-MA2-043 死状态）** |
| SUBMITTED→REJECTED | **未实现**（无 reject 方法） | — | — | **FAIL（P1-MA2-043 死状态）** |

**DRAFT→SUBMITTED 实现但用硬编码字符串**（P1-MA2-044）。**SUBMITTED→APPROVED/REJECTED 完全缺失**（P1-MA2-043 死状态）。

#### 终端与恢复（**FAIL**）

APPROVED/REJECTED dict 声明为终态但**不可达**（死状态）。SUBMITTED 是长期等待点（approve 未实现，工时单静默下沉——但归 owner doc Deferred 工时归集 projects/cost-collection，非数据缺陷）。

#### 异常路径（PARTIAL）

submit 守卫正确（拒非 DRAFT）。但 APPROVED/REJECTED 路径完全缺失，无法演练 reject 后修改重提。

#### 可达性（**FAIL**）

4 态中 2 态可达，2 态不可达（APPROVED/REJECTED 死状态）。**P1-MA2-043**。

#### 场景演练（**FAIL**）

- **(d) 工时单审批**：DRAFT→SUBMITTED→APPROVED/REJECTED——**approve/reject 未实现，演练确认缺口**。owner doc §场景 F 声明全链但代码仅实现 submit。

#### 与设计文档一致性（**FAIL**）

owner doc `state-machine.md §适用对象三 工时表 L175-201` + §场景 F L196-201 声明 DRAFT→SUBMITTED→APPROVED/REJECTED 全链 + 工时归集 projects/cost-collection，**代码仅实现 submit**。漂移。**P1-MA2-043**（owner doc 标注 Deferred 或实现迁移）。

### 2.4 排班分配状态机（ErpHrShiftAssignment，无 dict，4 值常量）— **FAIL（P1-MA2-046）**

#### 状态定义（**FAIL** 清晰性缺陷）

ORM `status` **无 dict 绑定**（raw VARCHAR(50) :1186）。4 值经 `ErpHrConstants.ASSIGNMENT_STATUS_*`（:98-101 SCHEDULED/PRESENT/ABSENT/CANCELLED）常量。owner doc `shift-scheduling.md §二 L85` 声明「dict：SCHEDULED/PRESENT/ABSENT/CANCELLED」——**owner doc 声明 dict 但 ORM 无 dict 绑定（漂移）**。**P1-MA2-046**（清晰性缺陷——UI/验证器无法枚举合法值）。每个值语义清晰（SCHEDULED=已排班 / PRESENT=已到岗 / ABSENT=缺勤 / CANCELLED=取消）。

#### 转换矩阵（PASS）

多 writer 迁移矩阵：

| 源 → 目标 | Writer | 守卫 | 裁决 |
|---|---|---|---|
| ∅→SCHEDULED | `assignSingle:139` / `assignBatch:86` / `copyFromPeriod:110` / 轮班生成 | assertNoExistingAssignment | PASS |
| SCHEDULED→PRESENT | `calcAttendance:103`（有打卡） | — | PASS |
| SCHEDULED→ABSENT | `calcAttendance:74`（leaveRequest）+ `:86`（无打卡）+ `onLeaveApproved:133`（请假 approve） | — | PASS |
| ABSENT→SCHEDULED | `onLeaveCancelled:150`（leaveRequestId 匹配回退）+ swap `approve:101-102`（重置） | leaveRequestId 匹配:146 | PASS |
| SCHEDULED→CANCELLED | `deleteExistingAssignments:190`（regenerate） | eq SCHEDULED:183 | PASS |

无非法跳转。多 writer 经 I*Biz Facade（assignmentBiz / daoProvider）+ leaveRequestId 匹配保证回退正确性。

#### 终端与恢复（PASS）

CANCELLED 是终态（regenerate 后不可恢复，设计正确——regenerate 是排班重建）。无重新激活需求。

#### 异常路径（PASS）

onLeaveCancelled leaveRequestId 不匹配时仅回退匹配项（设计正确——避免回退其他请假的 ABSENT）。swap approve 时 target 已不存在则 `ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE:88`。

#### 可达性（PASS）

4 值全部可达（见 §1.2）。无死循环。

#### 场景演练（PASS）

- **(e) 排班分配生命周期**：assign SCHEDULED→calcAttendance PRESENT/ABSENT→regenerate CANCELLED。全链可达。
- **请假联动**：approve→onLeaveApproved 置 ABSENT→cancel→onLeaveCancelled 回退 SCHEDULED（leaveRequestId 匹配）。

#### 与设计文档一致性（**FAIL**）

owner doc `shift-scheduling.md §二 L85` 声明 dict 但 ORM 无 dict 绑定。**P1-MA2-046**。其余 owner doc 描述与实现一致。

### 2.5 换班申请状态机（ErpHrShiftSwapRequest，4 态）

#### 状态定义（PASS）

dict `erp-hr/swap-status`（dict.yaml 确认）：PENDING（待审批）/ APPROVED（已批准，终态）/ REJECTED（已驳回，终态）/ CANCELLED（已取消，终态）。每个状态清楚表达等待点。

#### 转换矩阵（PASS）

| 源 → 目标 | 触发 | 守卫 | 副作用 | 裁决 |
|---|---|---|---|---|
| ∅→PENDING | submit:49-75 | target 非空:59 | 设 PENDING + 双方 assignmentId | PASS |
| PENDING→APPROVED | approve:78-110 | assertTransition(PENDING):81 | 交换 shiftId:92-94 + replacedByAssignmentId:98-99 + 重置 SCHEDULED:101-102 | PASS |
| PENDING→REJECTED | reject:113-120 | assertTransition(PENDING):116 | 设 REJECTED | PASS |
| PENDING→CANCELLED | cancel:123-130 | assertTransition(PENDING):126 | 设 CANCELLED | PASS |

无非法跳转。assertTransition 守卫齐全。

#### 终端与恢复（PASS）

APPROVED/REJECTED/CANCELLED 是终态（无出边）。无重新激活需求（重新申请经新建 PENDING）。

#### 异常路径（PASS）

approve 时 target 已不存在则 `ERR_SHIFT_ASSIGNMENT_NOT_SWAPPABLE:88`。submit 时 target 为空则 `ERR_SHIFT_SWAP_TARGET_OCCUPIED:60`。

#### 可达性（PASS）

4 态全部可达。无死循环。

#### 场景演练（PASS）

- **(f) 换班**：submit PENDING→approve（交换 shiftId + 重置 SCHEDULED）→APPROVED。全链可达。

#### 与设计文档一致性（PASS）

owner doc `shift-scheduling.md §五 调换审批` 与实现一致。无漂移。

### 2.6 工资审批-支付双轴状态机（ErpHrSalary，approveStatus 4 + paymentStatus 3 + posted）— **FAIL（P1-MA2-047 + P1-MA2-048）**

#### 状态定义（PASS with note）

**双轴**：approveStatus（dict `wf/approve-status` 4 态 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）+ paymentStatus（dict `erp-hr/salary-payment-status` 3 态 PENDING/PAID/VOID）+ posted 布尔（**死字段，从未写入，P1-MA2-047**）。组合语义清晰：APPROVED+PENDING=待付 / APPROVED+PAID=已付 / REJECTED+PENDING=驳回待处理 / VOID=作废。

#### 转换矩阵（**FAIL** posted 缺陷）

审批轴（委托平台 `approval-support.xbiz` DIRECT 模式，module-hr 无 `.xbiz.xml` 覆盖确认）：

| 源 → 目标 | 触发 | 裁决 |
|---|---|---|
| UNSUBMITTED→SUBMITTED→APPROVED/REJECTED | 平台标准审批动作 | PASS（DIRECT 模式默认行为与 owner doc `state-machine.md §四` 一致） |

支付轴（本类管理）：

| 源 → 目标 | 触发 | 守卫 | 副作用 | 裁决 |
|---|---|---|---|---|
| PENDING→PAID | markPaid:97-118 | 双守卫：approveStatus=APPROVED:100-105 + paymentStatus=PENDING:106-111 | **tryPostPayment:112（吞异常）+ 设 PAID:114（不检查返回值，不设 posted）** | **FAIL（P1-MA2-048 吞异常悬挂 + P1-MA2-047 posted 不设）** |
| PENDING→VOID | voidSalary:121-131 | 守卫拒已 PAID:124-127 | 设 VOID | PASS |
| PENDING→PAID（批量） | generateBankFile:134-178 | findPayableSalaries（APPROVED+PENDING） | 批量设 PAID:156 + 创建银行文件 GENERATED:168 | PASS（批量同 markPaid 单个语义，过账副作用经 I*Biz Facade） |

#### 终端与恢复（PASS）

PAID/VOID 是终态。voidSalary 守卫拒已 PAID（`ERR_SALARY_LOCKED_AFTER_PAID:124-127`）——PAID 不可作废，设计正确（已付工资作废须红冲而非 VOID）。无重新激活需求。

#### 异常路径（**FAIL**）

- markPaid 未审批：双守卫拒绝（`ERR_SALARY_ILLEGAL_STATUS_TRANSITION` approveStatus 非 APPROVED / paymentStatus 非 PENDING）。PASS。
- voidSalary 已 PAID：守卫拒绝（`ERR_SALARY_LOCKED_AFTER_PAID`）。PASS。
- **工资过账 tryPostPayment 吞异常→posted=false 悬挂无告警闭环**：**FAIL（P1-MA2-048）**。markPaid 无条件设 PAID 不检查 tryPostPayment 返回值，finance 过账引擎异常时 salary 进入 PAID 终态但无凭证，hr dispatcher 吞异常不进入 finance 工作台，期末结账前置检查不覆盖此悬挂（见 §1 关键裁决）。
- runPayroll 批量单失败隔离：经 `existsNonVoidSalary:86` 跳过已存在（幂等），单个 calculate 失败经 @BizMutation 事务回滚。PASS。

#### 可达性（PASS for status, **FAIL** for posted）

审批轴 4 态经平台可达。支付轴 3 态全部可达（PENDING/PAID/VOID）。**posted 字段永不可达（死字段）**（P1-MA2-047）。

#### 场景演练（**FAIL**）

- **(g) 工资双轴快乐路径**：UNSUBMITTED→平台审批→APPROVED→markPaid（tryPostPayment + 设 PAID）→PAID + 凭证 + generateBankFile。快乐路径正确。
- **(h) 工资 void**：PENDING→VOID / 已 PAID 拒绝。正确。
- **(k) 工资过账失败**：tryPostPayment 吞异常→salary 进入 PAID 但无凭证 + posted 永不写入 + 无告警闭环。**悬挂（P1-MA2-048）**。
- **(l) 并发 markPaid 同工资**：无显式 @Version 锁但 ErpHrSalary versionProp 透明乐观锁降级为 detectable conflict（交接 A2.17）。

#### 与设计文档一致性（**FAIL**）

- owner doc `payroll.md:368` 声明 posted 字段 + PostingDispatcher，**代码 posted 字段从未写入 + SalaryPostingDispatcher javadoc :27-28 声称「无 posted 字段」**。**P1-MA2-047**（doc/code drift + 死字段）。
- owner doc `payroll.md §业财过账` 未声明 tryPostPayment 吞异常容错语义。**P1-MA2-048**（owner doc 须标注容错设计或实现告警闭环）。
- orphan dict `salary-approval-status` 6 态 vs 实际 wf/approve-status 4 态——owner doc `payroll.md §审批状态标准化 L26-29` 已声明废弃，orphan dict 是历史残留（维持 P2-MA1-020）。

### 2.7 薪酬仿真状态机（ErpHrSalarySimulation，5 态）

#### 状态定义（PASS）

dict `erp-hr/simulation-status`（dict.yaml 确认）：DRAFT（草稿）/ IN_REVIEW（审核中）/ APPROVED（已审批）/ REJECTED（已驳回，终态）/ CONVERTED（已转正式，终态）。每个状态清楚表达等待点。

#### 转换矩阵（PASS）

| 源 → 目标 | 触发 | 守卫 | 副作用 | 裁决 |
|---|---|---|---|---|
| ∅→DRAFT | createSimulation:70-95 | — | 设 DRAFT:92 | PASS |
| DRAFT→IN_REVIEW | submitForReview:385-402 | 守卫 DRAFT:389 + 须有≥1 调整:395-398 | 设 IN_REVIEW | PASS |
| IN_REVIEW→APPROVED | approve:405-421 | 守卫 IN_REVIEW:410 | 设 APPROVED + reviewerId + reviewedAt | PASS |
| IN_REVIEW→REJECTED | reject:424-439 | 守卫 IN_REVIEW:429 | 设 REJECTED + notes(reason) | PASS |
| APPROVED→CONVERTED | convertToFormal:442-532 | 守卫 APPROVED:446 + per-employee 冲突 skip:465(PAID)/470(DUPLICATE) + all-conflict throw:510-525 | 设 CONVERTED + 创建正式工资 UNSUBMITTED/PENDING:500-501 | PASS |

无非法跳转。守卫齐全。

#### 终端与恢复（PASS）

REJECTED/CONVERTED 是终态。CONVERTED 不可恢复（已转正式，设计正确——仿真是一次性转换）。REJECTED 后重新仿真经新建 DRAFT。

#### 异常路径（PASS）

convertToFormal 冲突双层容错：(a) per-employee skip（PAID_CONFLICT :465 / DUPLICATE :470）——跳过冲突员工继续转换其他；(b) all-conflict throw（:510-525）——全员冲突时按最严重错误抛出（PAID_CONFLICT 优先）。设计正确。

#### 可达性（PASS）

5 态全部可达（见 §1.2）。无死循环。

#### 场景演练（PASS）

- **(i) 仿真全链**：DRAFT→submitForReview（须有调整）→IN_REVIEW→approve→APPROVED→convertToFormal（per-employee 冲突 skip + 创建正式工资）→CONVERTED。全链可达。
- 全员冲突：convertToFormal all-conflict throw 正确。

#### 与设计文档一致性（PASS）

owner doc `payroll-simulation.md` 与实现一致。无漂移。

### 2.8 银行付款文件状态机（ErpHrPayrollBankFile，3 态）— **FAIL（P1-MA2-045）**

#### 状态定义（PASS）

dict `erp-hr/bank-file-status`（dict.yaml 确认）：GENERATED（已生成）/ UPLOADED（已上传）/ CONFIRMED（已确认）。每个状态清楚表达等待点（GENERATED=等待出纳上传 / UPLOADED=等待银行确认 / CONFIRMED=终态）。

#### 转换矩阵（**FAIL**）

| 源 → 目标 | 触发 | 守卫 | 副作用 | 裁决 |
|---|---|---|---|---|
| ∅→GENERATED | `ErpHrSalaryBizModel.generateBankFile:168` | findPayableSalaries 非空 | 设 GENERATED + 创建银行文件 | PASS |
| GENERATED→UPLOADED | **未实现**（BizModel 18 行 CRUD 桩） | — | — | **FAIL（P1-MA2-045 死状态）** |
| UPLOADED→CONFIRMED | **未实现**（同上） | — | — | **FAIL（P1-MA2-045 死状态）** |

**GENERATED 可达（generateBankFile 设），UPLOADED/CONFIRMED 完全缺失**（桩 BizModel）。

#### 终端与恢复（**FAIL**）

CONFIRMED dict 声明为终态但**不可达**（死状态）。GENERATED 是长期等待点（upload 未实现，银行文件静默下沉——但归 owner doc Deferred 银行回单自动对账 + 实际转账执行，payroll.md L11 明示「本设计不负责：银行实际转账执行」，非数据缺陷）。

#### 异常路径（PARTIAL）

generateBankFile 无 payable salaries 时 `ERR_NO_APPROVED_SALARY_FOR_BANK_FILE:141`（正确）。但 UPLOADED/CONFIRMED 路径缺失，无法演练银行回单对账异常。

#### 可达性（**FAIL**）

3 态中 1 态可达，2 态不可达（UPLOADED/CONFIRMED 死状态）。**P1-MA2-045**。

#### 场景演练（**FAIL**）

- **(j) 银行文件**：GENERATED→UPLOADED→CONFIRMED——**桩未实现，演练确认缺口**。owner doc §七 声明全链但代码仅实现 generateBankFile。

#### 与设计文档一致性（**FAIL**）

owner doc `payroll.md §七 银行文件生成 L439+` + `state-machine.md L255` 声明银行文件生命周期（生成→上传→确认），**代码仅实现 GENERATED**。漂移。**P1-MA2-045**（owner doc §七 标注 Deferred 或实现 upload/confirm 迁移）。payroll.md L11 已声明「本设计不负责：银行实际转账执行」——owner doc 边界已部分声明，但 §七 状态机章节未标注 Deferred。

## 3. 维度裁决汇总

| 维度 | 请假 | 考勤 | 工时单 | 排班分配 | 换班 | 工资双轴 | 仿真 | 银行文件 | 裁决 |
|---|---|---|---|---|---|---|---|---|---|
| 1. 状态定义 | PASS | PASS（布尔非状态机，设计正确） | PASS | **FAIL（P1-MA2-046 无 dict）** | PASS | PASS（posted 死字段 P1-MA2-047） | PASS | PASS | 8 组件中 6 PASS + 2 FAIL（清晰性/doc drift） |
| 2. 转换完整性 | PASS | PASS | **FAIL（P1-MA2-043 approve/reject 缺失）** | PASS | PASS | **FAIL（P1-MA2-048 吞异常悬挂）** | PASS | **FAIL（P1-MA2-045 upload/confirm 缺失）** | 8 组件中 5 PASS + 3 FAIL |
| 3. 终端与恢复 | PASS | N/A | **FAIL（APPROVED/REJECTED 死状态）** | PASS | PASS | PASS（PAID/VOID 终态正确） | PASS | **FAIL（CONFIRMED 死状态）** | 7 PASS + 2 FAIL（+ 1 N/A） |
| 4. 异常路径 | PASS | PASS | PARTIAL（无 approve/reject 路径） | PASS | PASS | **FAIL（P1-MA2-048 吞异常悬挂）** | PASS | PARTIAL（无 upload/confirm 路径） | 6 PASS + 1 FAIL + 2 PARTIAL（+ 1 N/A） |
| 5. 可达性 | PASS | N/A | **FAIL（2 态死状态）** | PASS | PASS | PASS（posted 死字段 P1-MA2-047） | PASS | **FAIL（2 态死状态）** | 6 PASS + 2 FAIL（+ 1 N/A） |
| 6. 角色与权限 | PASS | PASS | PASS | PASS | PASS | PASS（危险操作经 I*Biz 跨域合规） | PASS | PASS | 8 组件全 PASS（运行时验证归 A4.4/A6） |
| 7. 外部依赖 | PASS | PASS | PASS | PASS | PASS | **FAIL（P1-MA2-048 跨域过账吞异常）** | PASS | PASS | 7 PASS + 1 FAIL |
| 8. TODO/任务策略 | PASS | PASS | **FAIL（SUBMITTED 长期静默下沉）** | PASS | PASS | PASS（PAID 终态正确） | PASS | **FAIL（GENERATED 长期静默下沉）** | 6 PASS + 2 FAIL（归 owner doc Deferred） |
| 9. 场景演练 | PASS | PASS | **FAIL（approve/reject 缺失）** | PASS | PASS | **FAIL（过账失败悬挂）** | PASS | **FAIL（upload/confirm 缺失）** | 5 PASS + 3 FAIL |
| 10. 与设计文档一致性 | PASS | PASS | **FAIL（owner doc §场景 F 漂移 + P1-MA2-044 硬编码）** | **FAIL（P1-MA2-046 owner doc dict 漂移）** | PASS | **FAIL（P1-MA2-047 javadoc drift + P1-MA2-048 owner doc 未声明容错）** | PASS | **FAIL（owner doc §七 漂移）** | 4 PASS + 4 FAIL |

## 4. 已登记 finding 运行时影响复核（MA1 finding 状态机角度）

| Finding ID | 原登记 | 状态机运行时影响复核 | 终态裁决 |
|---|---|---|---|
| `P2-MA1-020` | hr 残留 orphan dict `erp-hr/salary-approval-status` 6 态 | **状态机角度无升级**：orphan dict 无 column 使用（`ErpHrSalary.approveStatus` 实际引用 `wf/approve-status` 4 态 ORM:736 确认）+ 无 setStatus writer + 无状态机判定依赖。owner doc `payroll.md §审批状态标准化 L26-29` 已声明废弃。orphan dict 是 i18n/javadoc 历史残留，**在工资状态机上不引入悬挂**。维持 P2（MR1 顺手清理）。 | 维持 P2-MA1-020 |
| `P1-MA1-022` | hr `daoFor(ErpMdPartner)` 跨域只读（9 域合并） | **状态机角度无升级**：跨域只读是报表聚合副作用（`ErpHrReportBizModel.java:268` 员工净余额报表），不参与任何状态迁移判定、不在状态迁移异常路径（report BizModel 无 setStatus 调用，纯查询聚合）。维持 P1（MR1 治理，9 域合并）。 | 维持 P1-MA1-022 |

## 5. 残留风险

1. **P1-MA2-048 工资过账悬挂**：markPaid 吞异常致 PAID-without-voucher，期末结账前置检查不覆盖。MR1 修复前，依赖运维监控 LOG.warn/error + 期末试算平衡人工发现业财不一致。属容忍风险（失败模式需 finance 过账引擎异常，非正常路径）。
2. **P1-MA2-043/045 死状态 owner doc Deferred**：工时单 approve/reject + 银行文件 upload/confirm 是 owner doc Deferred 能力（工时归集 projects/cost-collection + 银行回单自动对账）。MR1 裁决实现或 owner doc 标注 Deferred + 删除 dict 项前，工时单/银行文件长期停留在 SUBMITTED/GENERATED（静默下沉，非数据缺陷）。
3. **并发敏感点 5 处交接 A2.17**：markPaid / generateBankFile / calcAttendance / runPayroll / onLeaveApproved 批量写——7 个 hr 状态机实体全部声明 versionProp 透明乐观锁降级为 detectable conflict（stale 异常需重试）。A2.17 系统性并发审计复核。
4. **角色权限运行时验证归 A4.4/A6**：本审计状态机层面无角色漂移反模式，但未做权限绑定运行时验证。

## 6. 结论

hr 考勤与工资八组件状态机核心契约经实仓逐项证据确认：**主路径状态迁移守卫齐全**（请假 5 态全迁移 + 工资双轴 + 仿真 5 态 + 换班 4 态 + 排班分配 4 值 + 考勤打卡幂等）、事务边界清晰、跨域访问经 I*Biz Facade 合规、@Version 透明乐观锁降级。**零 P0**（六个候选 P0 经证据证伪或降级：工时单 APPROVED/REJECTED 死状态按同型裁决 P1 不破坏主路径 / 银行文件 UPLOADED/CONFIRMED 死状态 owner doc Deferred config-gated P1 / 工资过账吞异常悬挂按同型裁决 P1 非正常路径失败 + LOG 可见性）；**6 项新 P1**（P1-MA2-043 工时单 APPROVED/REJECTED 死状态 + owner doc 漂移 / P1-MA2-044 工时单硬编码字符串 / P1-MA2-045 银行文件 UPLOADED/CONFIRMED 死状态 + 桩 BizModel + owner doc 漂移 / P1-MA2-046 排班分配 status 无 dict 绑定 + owner doc 声明漂移 / P1-MA2-047 SalaryPostingDispatcher javadoc "无 posted 字段" drift + posted 死字段 / P1-MA2-048 工资过账 tryPostPayment 吞异常 posted=false 悬挂无告警闭环——全部按 finance P1-MA2-031/032 + mfg P1-MA2-035/036 + hr A2.7a P1-MA2-039~042 同型裁决）；**1 项新 P2** watch-only（P2-MA2-052 state-machine.md 缺考勤/工资/工时单/排班/换班/仿真/银行文件独立章节）；MA1 finding（P2-MA1-020 + P1-MA1-022）运行时复核**无升级**；并发敏感点 5 处交接 A2.17（含 @Version 透明乐观锁降级重要事实——7 个 hr 状态机实体全部声明 versionProp）。

**A2.7a + A2.7b hr 状态机审查 S 级拆分 1/2 + 2/2 全部 done**。scope matrix §状态机正确性 hr 列由 `⚠️P1(A2.7a✅;A2.7b❓)` 推进至 `⚠️P1(A2.7a✅;A2.7b✅)`。

---

## 附：独立审计证据索引

- ORM 模型源：`module-hr/model/app-erp-hr.orm.xml`（leave-status:50-56 / timesheet-status:57-62 / salary-payment-status:72-76 / orphan salary-approval-status:77-84 / bank-file-status:132-136 / simulation-status:148-154 / swap-status:166-171 / ShiftAssignment.status 无 dict :1186 / ErpHrSalary.approveStatus :736 + paymentStatus :735 + posted :758）
- dict.yaml 源：`module-hr/erp-hr-meta/src/main/resources/_vfs/dict/erp-hr/`（timesheet-status / bank-file-status / swap-status / simulation-status / leave-status / salary-payment-status / orphan salary-approval-status + **无 assignment-status.dict.yaml**）
- BizModel 源：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/entity/`（ErpHrLeaveRequestBizModel 227 行 / ErpHrAttendanceBizModel 122 行 / ErpHrTimesheetBizModel 47 行 / ErpHrShiftAssignmentBizModel 205 行 / ErpHrShiftBizModel 235 行 / ErpHrShiftSwapRequestBizModel 144 行 / ErpHrSalaryBizModel 254 行 / ErpHrSalarySimulationBizModel 1129 行 / ErpHrPayrollBankFileBizModel 18 行）
- 过账源：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/posting/`（SalaryPostingDispatcher 155 行 + SalaryPostingExecutor + SalaryPostingProvider 178 行）
- 常量源：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrConstants.java`（ASSIGNMENT_STATUS:98-101 / PAYMENT:36-38 / BANK_FILE_STATUS:72 / SWAP_STATUS:109-112 / LEAVE_STATUS:115-116,226-228 / SIMULATION_STATUS:124-128 / APPROVE_STATUS:30-33）
- owner doc：`docs/design/human-resource/`（state-machine.md §适用对象一休假 + §三工时表 + §四薪酬审批 + §场景 F / payroll.md §审批状态标准化 + §七银行文件 + posted:368 / payroll-simulation.md / shift-scheduling.md §二排班分配 + §四考勤 + §五换班）
- 测试：`module-hr/erp-hr-service/src/test/java/app/erp/hr/service/`（TestErpHrLeaveEngine / TestErpHrAttendanceEngine / TestErpHrShiftScheduling / TestErpHrSalaryWorkflowApproval / TestErpHrPayrollEngine / TestErpHrPayrollSimulation）
- 零 `.xbiz.xml`（module-hr grep 确认平台 DIRECT 模式）+ 零 `*Processor.java`（状态迁移全在 BizModel 内联）
