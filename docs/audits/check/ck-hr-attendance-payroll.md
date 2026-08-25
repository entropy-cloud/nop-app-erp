# ck-hr-attendance-payroll — human-resource「考勤与薪酬」切片实现代码检查报告

> 工作项：C6.2。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-hr/erp-hr-service/src/main/java` 考勤与薪酬切片 44 个手写生产文件——payroll 计算引擎 5 类（`payroll/PayrollCalculator`、`IncomeTaxCalculator`、`SocialInsuranceCalculator`、`TaxBracketParser`、`TaxBracket`）+ 过账 3 类（`posting/SalaryPostingDispatcher`、`SalaryPostingExecutor`、`SalaryPostingProvider`）+ 调度计算 1 类（`scheduling/ShiftAttendanceCalculator`）+ 状态机 3 Bean（LeaveRequest/SalaryApproval/SalaryPayment）+ job 1 类（`ErpHrLeaveApproverTimeoutJob`）+ BizModel 16（Attendance/LeaveRequest/LeaveBalance/Salary/SalaryItem/SalarySimulation/SalarySimulationItemAdjustment/PayrollBankFile/TaxConfig/TaxSpecialDeduction/SocialInsuranceBase/SocialInsuranceConfig/Shift/ShiftAssignment/ShiftRotationPattern/ShiftSwapRequest）+ Processor 15（Abstract×7 + ClockIn/ClockOut/Leave Submit+Approve+Cancel/Salary Calculate+RunPayroll+MarkPaid+GenerateBankFile+PostApproval+ApprovalGuard/Simulation Create+Adjust+Batch+Convert/Shift AssignSingle+Batch+Copy+CalcAttendance+OnLeaveApproved+OnLeaveCancelled+GenerateRotation/Swap Submit+Approve）。跨文件核实：`module-hr/model/app-erp-hr.orm.xml`（16 实体 versionProp/UK/dict 全值列集）、`_vfs/erp/hr/beans/app-service.beans.xml`（50 bean 接线）、`ErpHrSalary.xbiz`（审批轴 5 mutation）、`app-erp-all/_vfs/nop/job/conf/erp-hr-leave-approver-timeout.job.yaml`（D4）、税率表种子 CSV（速算扣除数表核对）、测试 6 文件行为语义交叉验证（PayrollEngine 10 用例/PostingChain/IncomeTaxCalculator/LeaveEngine/ShiftScheduling/AttendanceCrossDay+MakeUp）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。切片文件逐行深读 + 平台语义实证（`IErpFinVoucherBiz.post` REQUIRES_NEW——module-finance `ErpFinVoucherBizModel.java:74` 实证；`FilterBeans.eq(name,null)`=IS NULL——任务给定已验证先例；@BizMutation 事务仅 GraphQL mutation 入口——hr-org 切片已实证沿用）+ arm-index 复用裁决。
> 切片边界：组织/员工/合同/招聘（org/employee/contract/recruitment 族）归 C6.1 已查勿重复（P2-CK-hr-004 CRUD 同型已登记员工实体，本切片并入 salary/attendance/leave 实体同型面）；competency/survey/timesheet 族归前轮已审（P1-MA2-041/042/043 已裁决）；`ErpHrReportBizModel`（读侧报表聚合）本切片仅模式抽查（见剩余风险）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-hr2-001（D6）出勤折算分母恒为硬编码 22 且有薪假按缺勤扣薪——不足 22 个打卡日的月份全员基本工资被折减、带薪年假双路径扣款

- **控制点**：`app/erp/hr/service/payroll/PayrollCalculator.java#summarizeAttendance`（L294-304 逐考勤记录计数 `if (!Boolean.TRUE.equals(a.getIsAbsent())) presentDays = presentDays.add(BigDecimal.ONE);` + L306 `summary.requiredDays = DEFAULT_REQUIRED_WORK_DAYS;`——分母无条件恒 22，注释声明的「应出勤日」从未按月推导）+ L42 `DEFAULT_REQUIRED_WORK_DAYS = new BigDecimal("22")` + L68-73 `attendanceRatio = actualDays.divide(requiredDays, 6, HALF_UP)` + L86 `basicSalary = monthlySalary.multiply(attendanceRatio)`
- **证据**：① **actualDays = 当月非缺勤打卡记录条数**（无排班/日历参与），2 月（中国约 17-20 个工作日）全员出满勤也只 17-20 天 < 22 → 基本工资按 17/22≈77% 发放，**每个短月全员系统性少发**；法定节假日多的月份同理（10 月约 18 个工作日 → 82%）。② **有薪假扣薪**：员工休 5 天带薪年假当月打卡 17 天 → ratio=17/22，年假（shift-scheduling.md §6.1 逐字「薪资计算时按休假类型（**年假有薪**/事假无薪）处理扣款」）被当作缺勤折减；若 calcAttendance 曾跑过休假日（`AbstractErpHrShiftProcessor#upsertAttendanceForLeave` L72-84 写 `isAbsent=true`）路径相同。③ **病假双重扣减**：`deductUnpaidLeave=true` 时 SICK/PERSONAL 经 L77-82 再按 `unpaidLeaveDays/requiredDays` 扣减一次——与考勤缺勤路径叠加双扣（且 SICK 在中国实务为病假工资折算非全无薪，javadoc 自认「视 SICK/PERSONAL 为无薪假」简化）。④ 无考勤记录整月 → actualDays 兜底=requiredDays 全额（L70-71），「完全不打卡」反而比「打 20 天卡」发得多——口径自相矛盾。测试 `TestErpHrPayrollEngine` 全部用例零考勤种子（走全勤兜底），该折算路径零断言（P1-MA4-019 测试有效性缺口的新面）。
- **问题**：核心薪酬算术缺陷，用户可见错误金额（少发工资），每个非 22 工作日月份必然触发。owner doc payroll.md §5.2「出勤比例 = 实际出勤日 / 应出勤日」中应出勤日语义未落地。
- **建议修复方向**：requiredWorkDays 按月推导（工作日历或排班天数聚合，config 兜底）；有薪假类型（ANNUAL/MARRIAGE 等）计入 actualDays 或从折算中豁免；无薪假扣减与考勤缺勤二选一防双扣；补足 22≠短月/年假/病假三场景测试。
- **arm-index 裁决**：新增（A4.4 hr 代码质量审计覆盖 PayrollCalculator 算术（clamp/BigDecimal/累计预扣）但未覆盖出勤折算分母与假别区分；grep arm-index「出勤比例/应出勤日/年假 扣薪」零命中）。

### P1-CK-hr2-002（D8）generateBankFile 批量发放翻 PAID 不触发 SALARY_PAYMENT(280) 凭证——银行文件路径发放的薪酬应付职工薪酬永不冲减

- **控制点**：`app/erp/hr/service/processor/ErpHrSalaryGenerateBankFileProcessor.java#generateBankFile`（L34-63 循环 `s.setPaymentStatus(paymentStateMachine.markPaidTargetStatus()); ... salaryDao().updateEntity(s);` 直接翻 PAID，**全方法零 `postingDispatcher` 调用**）对照 `ErpHrSalaryMarkPaidProcessor#markPaid` L40 `postingDispatcher.tryPostPayment(salary);`（单笔路径有 280 过账）
- **证据**：两条到 PAID 的路径过账不对称：`markPaid`（单笔）→ 280 发放凭证（借 应付职工薪酬/贷 银行存款）；`generateBankFile`（批量，设计 §七的主发放通道）→ 只置 PAID + paymentDate + batchNo，GL 侧 280 凭证缺失 → 应付职工薪酬科目余额虚挂、期末试算可发现但源头断裂。owner doc 内部两处表述：payroll.md §5.2 步骤 4「生成银行代发文件 → **过账：SALARY_PAYMENT 凭证** → 更新 paymentStatus = PAID」（银行文件路径含过账）vs §9.1 表「SALARY_PAYMENT 触发时机：paymentStatus → PAID（**由 markPaid 触发**）」——本条按 §5.2 主流程语义登记，修复时 owner doc 需一并裁决对齐。测试 `testGenerateBankFileTransfersSalariesToPaid` 断言 PAID 翻转但不断言凭证（背书现状而非设计）。
- **附注（同方法次要面，不另立）**：L38-40 文件内容行为占位——`序号,employeeId,净额,工资`，账号列写员工 ID 非银行账号（设计 §7.2「序号｜账号｜户名｜金额｜用途」缺户名列），bankId 仅存档不参与分组（设计「按银行分组」未落地）——生成的文件不可供银行实际使用。
- **问题**：业财一致性缺陷（D8 撤销方向同族见 P3 关联注记——本条是漏记不是悬挂）。
- **建议修复方向**：generateBankFile 循环内复用 markPaid 链路（守卫 + tryPostPayment + PAID），或批量后逐条补投 280（沿用计提链失败隔离 + 告警范式）；文件模板按 §7.2 补齐账号/户名（读员工 bankAccountId）。
- **arm-index 裁决**：新增（P1-MA4-017 覆盖 270/290/300 计提链未接线（已修 RC-R1.89），280 维度当时「既有路径」成立；本条是**第二条 PAID 通道**的 280 缺失，grep「generateBankFile 过账/银行文件 凭证」arm-index 零命中）。

### P1-CK-hr2-003（D6/D10）runPayroll 无逐员工失败隔离——任一员工缺合同/社保基数/税务配置即整批回滚，与 UC-HR-04「跳过并告警」明确分歧

- **控制点**：`app/erp/hr/service/processor/ErpHrSalaryRunPayrollProcessor.java#runPayroll`（L25-32 `for (ErpHrEmployee emp : activeEmployees) { ... ErpHrSalary salary = payrollCalculator.calculate(emp.getId(), year, month); salaryDao().saveEntity(salary); }`——循环体无 try/catch、无跳过告警，任一 calculate 抛错向上传播，@BizMutation 事务整体回滚）+ 三个必然抛错源实证：`PayrollCalculator#calculate` L56-59（无合同 → `ERR_EMPLOYMENT_CONTRACT_NOT_FOUND`）、`SocialInsuranceCalculator#calculate` L37-42（无社保基数 → `ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND`）、`IncomeTaxCalculator#findTaxConfig` L194-204（当年无 TaxConfig → `ERR_TAX_CONFIG_NOT_FOUND`）
- **证据**：owner doc use-cases.md UC-HR-04 异常段逐字「**员工缺失合同或薪资配置时跳过并告警**」；实现整批 fail-fast——一个新入职未配社保基数的员工会让全公司当月薪酬核算全部失败（已保存的前 N 个员工随事务回滚）。任务指定核查点「E2E 曾揭示种子员工缺配置抛错」与此同源（E2E 靠种子补配置绕过，产品面无隔离）。对照同域 job 范式：`ErpHrLeaveApproverTimeoutJob#runTimeoutEscalation` L134-143 已示范 per-item try/catch 隔离。
- **问题**：核心批量循环脆弱性 + owner doc 明确异常路径未落地（同型于 P1-MA4-016 时代「runPayroll 整批回滚」影响面描述，但该 finding 只修了 resolveBracket NPE 本体，隔离语义未修）。
- **建议修复方向**：runPayroll 循环体 per-employee try/catch（失败收集 + `IErpSysNotificationBiz` 告警事件 + 结果 Map 含失败清单），与 P1-CK-hr2-013 的批处理结构优化协同。
- **arm-index 裁决**：新增（P1-MA4-016 修复记录明确「runPayroll 整批循环无 per-employee 隔离」仅作为 NPE 影响面叙述，R1.26 只修 null 防御；grep「跳过并告警/逐员工隔离」零独立 finding）。

### P1-CK-hr2-004（D6）SocialInsuranceCalculator 完全忽略 effectiveFrom/effectiveTo 有效期——年调后同城同险种多行配置重复计扣、基数历史行任取

- **控制点**：`app/erp/hr/service/payroll/SocialInsuranceCalculator.java#findBase`（L87-94 `q.addFilter(eq("employeeId", employeeId)); q.setLimit(1);`——**零有效期/排序过滤，多行基数任取第一行**）+ `#findConfigs`（L96-101 仅 `eq("cityCode", cityCode)`——同城全部配置行全量返回）+ `#calculate` L52-59（对返回的**全部**非 HOUSING_FUND 配置行累加 `employeeTotal.add(clamped.multiply(employeeRate))`——同城同险种两行即双倍扣缴）+ `#findHousingFundConfig` L103-110（取第一行 HOUSING_FUND 无期限过滤）
- **证据**：ORM 实证 `ErpHrSocialInsuranceConfig`/`ErpHrSocialInsuranceBase` 均含 `effectiveFrom`/`effectiveTo` 列（`module-hr/model/app-erp-hr.orm.xml` 列集抽取实证）；owner doc payroll.md §2.2「基数调整周期：每年 7 月（全国大部分城市）」+ §2.3 配置表含 effectiveFrom/effectiveTo——年调后插入新一行（如深圳 2026.07 起）而旧行不删时：社保个人扣款 = 旧行 + 新行两倍基数比例。README.md §日期范围命名变体承认「区间互斥校验的接入为后续 follow-up」——但那是**写入侧互斥校验**，读取侧对有效期零过滤使「同一城市多期间行并存」直接错算，比互斥缺失更早触发。`findBase` 同理：员工 2025 基数行 + 2026 基数行并存时任取（无 orderBy，DB 返回序不定）→ 核算基数不确定。approve 时重算 ER（`SalaryPostingDispatcher#recomputeSocialInsuranceER` L282-286 复用同一 calculator）同患——两时点可能取到不同行。
- **问题**：中国本地化配置化（README 关键规则 3）的核心读取语义缺失，数据形态完全合法（模型支持）即触发双倍扣款——用户可见错误扣缴金额。
- **建议修复方向**：`findConfigs`/`findBase` 按核算期（year/month → 当月 15 日或月末）过滤 `effectiveFrom <= 期 <= effectiveTo`（null 视为无限）+ 同险种多命中取最新 effectiveFrom；`findHousingFundConfig` 同改。
- **arm-index 裁决**：新增（A4.4 审计赞扬「社保基数钳制算术（clamp min/max）扎实」但未覆盖有效期维度；grep「effectiveFrom 社保/基数 有效期」零命中）。

### P1-CK-hr2-005（D6）getComparison 对比视图的合计/扣款五行恒返回 0——SALARY_ITEM_CODES 含 5 个派生字段 readSalaryField 不处理

- **控制点**：`app/erp/hr/service/entity/ErpHrSalarySimulationBizModel.java#getComparison`（L145-155 `for (String itemCode : SALARY_ITEM_CODES) { ... row.put("simulatedAmount", readSalaryField(simulated, itemCode)); ... row.put("diff", readSalaryField(simulated, itemCode).subtract(nz(baseline))); }`）对照 `#readSalaryField` L515-539（switch 仅处理 8 个输入项字段，`default: return BigDecimal.ZERO;`）+ `SALARY_ITEM_CODES` L545-549（13 项含 `grossSalary/socialInsurance/housingFund/taxAmount/netSalary` 5 个派生字段）
- **证据**：设计 payroll-simulation.md §3.2 对比布局表明确要求展示「应发合计/社保(个人)/公积金(个人)/个税/实发合计」行；实现对这 5 个 itemCode 一律 `readSalaryField → default → ZERO`——simulatedAmount 恒 0、diff = 0 − 当期实际值 = 负的当期金额（如应发合计行显示 diff=-16100）。模拟功能的核心交付面（对比视图，§三 整节）数据错误。`AbstractErpHrSalarySimulationProcessor#readSalaryField` L149-173 同样副本同患。注意 `simulated` 对象本身字段正确（`recalculateDerived` 已算好 gross/tax/net）——纯读取映射遗漏。
- **问题**：用户可见正确性错误（模拟对比核心视图五行全错），修复面小（switch 补 5 case 或专用读取）。
- **建议修复方向**：readSalaryField switch 补 `grossSalary/socialInsurance/housingFund/taxAmount/netSalary` 5 case（BizModel 与 Abstract 双副本同步修，联动 P3-CK-hr2-019 legacy dup 一并收敛）；补对比视图断言测试。
- **arm-index 裁决**：新增（P2-MA4-008 登记「applyOverride-readSalaryField 重复 switch-case」可维护性面，未登记其 default 分支吞派生字段的行为缺陷；grep「对比视图/getComparison」零命中）。

### P2-CK-hr2-006（D7/D8）tryPostPayment 无去重守卫叠加凭证 REQUIRES_NEW 先行提交——markPaid 外层回滚后重试产生重复 280 发放凭证

- **控制点**：`app/erp/hr/service/posting/SalaryPostingDispatcher.java#tryPostPayment`（L170-184：**无 `alreadyPosted` 前置检查**，直接 `executor.postEvent(event)`）对照 `#tryPostAccrual` L86-91（`if (alreadyPosted(buildBillCode(salary), SALARY)) { ...; return true; }` 计提链有去重守卫——同族不对称）+ `ErpFinVoucherBizModel.java` L74 平台实证 `@Transactional(propagation = REQUIRES_NEW)`（凭证独立事务提交，不随外层回滚）
- **证据**：时序：T1 markPaid 守卫通过 → tryPostPayment 280 凭证在 REQUIRES_NEW 事务**先提交** → T1 外层后续 `updateEntity` 乐观锁冲突（并发 voidSalary/编辑已提交版本 +1）→ 外层回滚但**凭证已落库** → 薪酬仍 PENDING → 用户重试 markPaid → tryPostPayment 无守卫再过一张 280 → 应付职工薪酬/银行存款双倍冲减。计提链（270/290/300）同场景由去重守卫兜底（reverseApprove→再 approve 幂等），280 是唯一无守卫路径。generateBankFile 修 P1-CK-hr2-002 时若接线过账需一并补。
- **问题**：事务边界与幂等不对称（D7），触发需并发/重试窗口，但后果是 GL 双倍凭证（静默——无告警，因凭证都「成功」）。
- **建议修复方向**：tryPostPayment 复制计提链守卫（alreadyPosted(billCode, SALARY_PAYMENT) 命中即返回 true），一行式修复。
- **arm-index 裁决**：新增（P1-MA4-017 修复引入计提守卫时明确未覆盖 280；RC-R1.89 记录「280 既有路径」维持原样；grep「tryPostPayment 去重/280 重复凭证」零命中）。

### P2-CK-hr2-007（D5，同型 P1-CK-pur-003 族——并入 P2-CK-hr-004 家族 hr2 站点）salary/attendance/leave/simulation 全实体通用 CRUD 无守卫——ErpHrSalary__update_ 可直写 approveStatus=APPROVED 绕过整条计提链、直写 paymentStatus=PAID 绕过 280 凭证

- **控制点**：`app/erp/hr/service/entity/` 下切片 16 个 BizModel 中除 LeaveRequest/Salary/Simulation 等有 `defaultPrepareSave` 兜底 businessDate/status 外，**零 `defaultPrepareUpdate`/`defaultPrepareDelete` 守卫**——`ErpHrSalaryBizModel` 仅 L82-88 defaultPrepareSave（businessDate 兜底）；`ErpHrAttendanceBizModel`/`ErpHrLeaveRequestBizModel` 同型
- **证据**：① `ErpHrSalary__update_` 可直接写 `approveStatus=APPROVED`——绕过 `ErpHrSalary.xbiz` approve mutation（guard + approvedBy/approvedAt + `ErpHrSalaryPostApprovalProcessor` 计提三连）→ **270/290/300 凭证永不生成且 posted 保持 false 无告警**（比 hr-org 切片的通用旁路更重：过账触发点被整体旁路）；② 直写 `paymentStatus=PAID` 绕过 markPaid → 无 280 凭证无 paymentDate；③ attendance `isAbsent/workHours` 可手改直接影响薪酬输入；④ leave `status` 可绕状态机写 APPROVED（无余额校验/无排班联动）；⑤ simulation status 可跳审批直达 APPROVED（convertToFormal 仅查 APPROVED）。本切片实体的同型面比 hr-org 五实体更贴近钱。
- **问题**：同 P1-CK-pur-003/P2-CK-hr-004 全域同型——命名动作链守卫可被通用 CRUD 旁路；hr2 站点按约定同型登记（不复用展开），①② 为本站点独有加重面，修复优先。
- **建议修复方向**：`defaultPrepareUpdate/Delete` 校验：salary 的 approveStatus/paymentStatus 变更仅允许经命名 mutation（CRUD 侧拒绝终态与跨轴写入）；leave status 拒绝非法值；与 pur-003 族修复方案联动裁决。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，hr2 站点新增计数，并入 P2-CK-hr-004 家族）。

### P2-CK-hr2-008（D5）休假余额守卫对「无余额记录」静默跳过——与 getBalance 同数据相反结论（守卫放行 vs 余额显示负数）

- **控制点**：`app/erp/hr/service/processor/AbstractErpHrLeaveRequestProcessor.java#checkLeaveBalance`（L110-113 `ErpHrLeaveBalance balance = findBalance(...); if (balance == null) { return; }`——无余额行即不校验，视为无限额）对照 `ErpHrLeaveRequestBizModel#getBalance` L128-132（无余额行按 0 计算 `entitled+carried-used` → 已用超发时返回**负数**）
- **证据**：员工无 LeaveBalance 行（HR 未配置该假别年度额度，UK `(employeeId,leaveType,fiscalYear,orgId)` 保证至多一行）：submit/approve 守卫直接跳过 → 可提交审批任意天数；同时 getBalance 对同一员工返回负余额（前端余额显示「-15 天」）。两 API 对同一数据给出「可请」与「超支」相反结论。UC-HR-02 前置条件「假期余额充足」的判定口径未定义未实现。
- **问题**：守卫缺口 + 语义矛盾（D5）；触发条件常见（新年度未导入额度、新假别）。
- **建议修复方向**：owner doc 裁决二选一：(a) 无余额行=0 额度，submit 拒绝（与 getBalance 一致）；(b) 无余额行=不限额（记录型假别），getBalance 同步返回 null/∞ 语义。修复时两处（BizModel 副本与 Abstract 副本）同步。
- **arm-index 裁决**：新增（grep「余额 不足 无记录/balance null 跳过」零命中）。

### P2-CK-hr2-009（D5/D8）日期重叠守卫仅同假别——跨假别同期休假可双双 APPROVED 且排班 leaveRequestId 互相覆盖致先批休假的取消联动失效

- **控制点**：`app/erp/hr/service/processor/AbstractErpHrLeaveRequestProcessor.java#checkDateOverlap`（L130 `q.addFilter(eq("leaveType", leave.getLeaveType()));`——重叠检测限定同假别；且 approve 路径不查重叠：`ErpHrLeaveRequestApproveProcessor#approve` L23-33 无 checkDateOverlap 调用）+ `app/erp/hr/service/processor/ErpHrShiftOnLeaveApprovedProcessor.java#onLeaveApproved`（L22-28 循环 `a.setLeaveRequestId(leaveRequestId)` 无条件覆盖——后批休假覆盖先批的关联）+ `ErpHrShiftOnLeaveCancelledProcessor#onLeaveCancelled`（L24 `if (leaveRequestId.equals(a.getLeaveRequestId()))` 仅解除匹配行）
- **证据**：员工年假 7/1-7/10 SUBMITTED + 病假 7/5-7/15 SUBMITTED（不同假别，submit 重叠检查互不命中）→ 两笔均 approve → ① 7/5-7/10 双假并存（同日双份假条；考勤 upsert 双写）；② 后批准的病假覆盖 7/5-7/10 排班的 leaveRequestId → 取消年假时 onLeaveCancelled 匹配不到（已被病假 ID 覆盖）→ 年假取消后排班 ABSENT 标记残留。设计 state-machine.md §4 异常「并发提交同一时段 | 后端校验时间段重叠，拒绝后提交的请求」未限定假别（人在同一时段只能休一种假）。
- **问题**：入参边界（重叠维度）+ 数据一致性（取消联动断链）。
- **建议修复方向**：checkDateOverlap 去掉 leaveType 过滤（或改为同假别强拦截+跨假别告警，按产品语义裁决）；approve 补重叠复检；onLeaveApproved 对已绑定其他 leaveRequestId 的排班跳过或告警而非覆盖。
- **arm-index 裁决**：新增（grep「日期重叠 假别/leaveRequestId 覆盖」零命中）。

### P2-CK-hr2-010（D7）ErpHrSalary 无 (employeeId, year, month) UK + existsNonVoidSalary 前置检查 TOCTOU——并发核算产生同员工同月重复薪酬行

- **控制点**：`module-hr/model/app-erp-hr.orm.xml` ErpHrSalary 实体（unique-keys 为空——python 抽取实证，对照 ShiftAssignment 有 `UK_HR_SHIFT_ASSIGNMENT_NATURAL`、LeaveBalance 有 `UK_HR_LEAVE_BALANCE_EMP_TYPE_YEAR_ORG`）+ `app/erp/hr/service/processor/AbstractErpHrSalaryProcessor.java#existsNonVoidSalary` L66-77（应用层查重后保存，无 DB 约束兜底）+ `ErpHrSalarySimulationItemAdjustment` 同样无 UK（`recordAdjustment` find-then-save 并发可双行，collectOverrides 取值序不定）
- **证据**：并发 calculateSalary 与 runPayroll（或两个 calculateSalary）同时通过 `assertNotDuplicated` 查询（互不可见未提交行）→ 双双 saveEntity → 同员工同月两条 PENDING 薪酬。后续 markPaid 各自过账 280 → 双倍发放凭证；个税累计链 `findPreviousCumulative` 取「最大 month」快照，同月两行取值不定。convertToFormal 的 `hasNonVoidSalary` 同 TOCTOU。ShiftAssignment 族已有 UK + flush-catch 翻译范式（P1-MA2-091 修复）可直接复用。
- **问题**：D7 并发重复 + 无 DB 兜底（同型 UK 缺失族）。
- **建议修复方向**：ORM 加 UK `(employeeId, year, month, delVersion)`（走 dual-agent-approval）+ saveEntity 后 flush-catch 翻译为 `ERR_SALARY_ALREADY_EXISTS`（镜像 doCreateAssignment 范式）；SimulationItemAdjustment 加 `(simulationId, employeeId, salaryItemCode)` UK。
- **arm-index 裁决**：新增（P1-MA2-091 是 ShiftAssignment UK 修复先例非 salary；grep「salary 唯一约束/重复薪酬」零命中）。

### P2-CK-hr2-011（D6）findActiveContract 名不副实——无 status 过滤无排序，多合同员工（调动旧约+新约/历史合同）任取一行作核算基数

- **控制点**：`app/erp/hr/service/payroll/PayrollCalculator.java#findActiveContract`（L273-280 `q.addFilter(eq("employeeId", employeeId)); q.setLimit(1);`——方法名 Active 但零 status 过滤、零 orderBy，DB 返回序即选中序）
- **证据**：调动链（hr-org 已查）产生 TERMINATED 旧约 + ACTIVE 新约两条记录；hire 续签/多次合同同理。`limit(1)` 无排序 → 选中哪条不确定（H2/MySQL 返回序不保证），可能以已终止合同的旧月薪核算当月薪酬（月薪源头错 → gross/tax/net 全链错）。无合同员工正确抛错（L56-59），有**多**合同员工静默任取。社保基数 approve 重算（Dispatcher）不经合同，不受影响；仅月薪基数维度。
- **问题**：核算输入选择非确定性（D6/D10）。
- **建议修复方向**：过滤 `status=ACTIVE`（+ `endDate >= 核算期 or endDate null`）+ `orderBy startDate desc` 取最新；无 ACTIVE 时回退抛错语义按产品裁决（历史员工补发场景）。
- **arm-index 裁决**：新增（grep「findActiveContract/合同 月薪 任取」零命中）。

### P2-CK-hr2-012（D6）applyBatchAdjustment 四类调整全部写入 basicSalary——ALLOWANCE（津贴调整）语义错置，调津贴实际调了基本工资

- **控制点**：`app/erp/hr/service/processor/ErpHrSalarySimulationApplyBatchAdjustmentProcessor.java#applyBatchAdjustment`（L59-65 `BigDecimal newBasic = nz(r.source.getBasicSalary()).add(adjustment); ... recordAdjustment(simulationId, empId, "basicSalary", ...)`——无论 adjustType 一律加到 basicSalary 并记 "basicSalary" 调整行）+ `#resolveBatchAdjustment` L361-362（`case BATCH_ADJUST_TYPE_ALLOWANCE: return numericValue;`——值解析与 FIXED 相同后同样进 basicSalary 路径）
- **证据**：owner doc payroll-simulation.md §5.1 调整类型四分类逐字含「**津贴调整（+/- 固定金额）**」；实现把津贴调整记成基本工资调整：调整记录（ErpHrSalarySimulationItemAdjustment.salaryItemCode="basicSalary"）与重算链全部落在基本工资——用户以为调岗位津贴，实际调了基本工资（个税/社保基数联动口径全错，且转正式后 formal.basicSalary 虚高）。设计 §2.2 adjustment-reason dict 有 `ALLOWANCE_CHANGE` 值，实现 recordAdjustment 恒传 `ADJUSTMENT_REASON_SALARY_CHANGE`（L65）。
- **问题**：批量调薪语义错误（模拟面，用户可见错误数据；转正式后污染正式薪酬）。
- **建议修复方向**：ALLOWANCE 分支改写 positionAllowance（recordAdjustment "positionAllowance" + reason=ALLOWANCE_CHANGE）；LEVEL_MAP 语义（职级映射表调薪——按档设新值 vs 增量）随 owner doc 一并裁决。
- **arm-index 裁决**：新增（A4.4 覆盖 Simulation What-If 隔离性未覆盖批调类型分发；grep「津贴调整 basicSalary」零命中）。

### P2-CK-hr2-013（D9）computeAllEmployeeSims 每员工 5+ 查询全量重算——四个汇总/异常 @BizQuery 每次打开 N×6 次查询；runPayroll 同型 N+1

- **控制点**：`app/erp/hr/service/entity/ErpHrSalarySimulationBizModel.java#computeAllEmployeeSims`（L607-612 循环内 `collectOverrides(simulation.getId(), empId)`（1 查询/员工）+ `payrollCalculator.recalculateWithOverrides(...)` → `IncomeTaxCalculator.calculate` 内 findTaxConfig + sumSpecialDeduction + findPreviousCumulative 各 1 查询/员工）——被 `getDepartmentSummary`/`getProjectSummary`/`getCompanySummary`/`findAnomalies` 四个 @BizQuery 各自全量调用（L171/198/226/249），同一请求周期内无缓存；`getComparison` L135 再叠加 getSimulatedSalary 单员工重算 + `applyBatchAdjustment` L40 也全量调
- **证据**：1000 员工规模：每次部门汇总/公司汇总/异常扫描视图 ≈ 1000×5 = 5000 查询（ TaxConfig 同年同结果被查 1000 次）；runPayroll 每员工 existsNonVoidSalary + 合同 + 考勤 + 无薪假 + 税配置 + 专项扣除 + 累计 + 基数 + 配置×2 ≈ 10 查询（`ErpHrSalaryRunPayrollProcessor` L26-29 + calculators）——月度批处理可接受但同构。对照 hr-org P3-CK-hr-014/ck-hr-003 全实体内存聚合家族。
- **问题**：D9（N+1 簇）——看板/汇总视图为同步 @BizQuery，规模增长后打开即超时风险；正确性无影响。
- **建议修复方向**：TaxConfig/专项扣除按 (year,month) 批量预取一次（Map 缓存入参化）；collectOverrides 一次 `eq simulationId` 全量查回按 employeeId 分组；汇总视图与 computeAllEmployeeSims 共享单次计算结果。
- **arm-index 裁决**：新增（同族注记 P3-CK-hr-003/P3-CK-hr-014 全量聚合家族——本条是 N+1 查询维度更重站点）。

### P2-CK-hr2-014（D5）generateRotation 活跃排班口径仅 SCHEDULED 且新建无 UK flush-catch——PRESENT/ABSENT 日被视为空位，插入命中 UK 抛未翻译原始异常

- **控制点**：`app/erp/hr/service/processor/ErpHrShiftRotationPatternGenerateRotationProcessor.java#findActiveAssignment`（L96-105 `eq("status", ASSIGNMENT_STATUS_SCHEDULED)`——仅查 SCHEDULED，PRESENT/ABSENT 日返回 null 视为可插入）+ `#newAssignment` L107-116（saveEntity 后**无 flushSession + UniqueConstraintHelper 翻译**）+ `#deleteExistingAssignments` L175-193（regenerate 也仅删 SCHEDULED）
- **证据**：对照同族 `AbstractErpHrShiftAssignmentProcessor#doCreateAssignment` L48-73（活跃口径 = SCHEDULED+PRESENT+ABSENT 三态 + flush-catch 翻译 `ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE`——P1-MA2-091 修复范式）；`ErpHrShiftAssignmentBizModel#findByEmployeeAndDate` L104 也用三态口径。员工某日已到岗（PRESENT）时跑轮换生成：findActiveAssignment 判空 → newAssignment 插入 → 命中 `UK_HR_SHIFT_ASSIGNMENT_NATURAL(employeeId,assignmentDate,shiftId,delVersion)` → 原始 duplicate-key SQL 异常冒出（非 NopException 错误码），且因无 flushSession 可能延迟到提交期才爆。regenerate=true 同患（PRESENT/ABSENT 不被逻辑删除，重生成行与之撞键）。
- **问题**：同实体两条生成路径口径/防御不一致（D5/D1 族——P1-MA2-091 修复未覆盖 rotation 路径）。
- **建议修复方向**：GenerateRotation 的 findActiveAssignment/newAssignment 对齐 Abstract 基类三态口径 + flush-catch 翻译（或直接复用基类方法）。
- **arm-index 裁决**：新增（P1-MA2-091 修复记录限 assignSingle/Batch/Copy 路径；grep「generateRotation UK/轮换 重复」零命中）。

### P3-CK-hr2-015（D3）shift-assignment-status dict CANCELLED 无 writer（死状态）+ 旧 6 值 salary-approval-status dict 成孤儿声明

- **控制点**：`module-hr/model/app-erp-hr.orm.xml` L177-182（dict `erp-hr/shift-assignment-status` 含 `CANCELLED` 选项——grep 全 erp-hr-service 零 `setStatus(ASSIGNMENT_STATUS_CANCELLED)` writer；排班取消实际走逻辑删除 deleteEntity，GenerateRotationProcessor L187-188 注释自证「仅状态置 CANCELLED 不变 delVersion 会触发 duplicate-key」故弃用状态改删除）+ L77-84（dict `erp-hr/salary-approval-status` 6 值 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID——approveStatus 列已改用 `wf/approve-status`（L742 实证），全仓无列引用该旧 dict，仅 `ErpHrConstants.java:5` javadoc 残留引用）
- **证据**：assignment CANCELLED：设计 §2.1 声明该值但无迁移/无取消 mutation（state-machine.md §适用对象五 预留清单亦未收录 assignment）；旧审批 dict 是「设计修正记录」（payroll.md §设计修正记录：原 6 态拆三轴）后未清理的孤儿声明——UI 若误绑定会显示不存在于 wf 四态的选项。
- **问题**：dict 死状态（B2）+ 死声明（维护噪音）。
- **建议修复方向**：assignment CANCELLED 要么补 cancel mutation（设计 §2.1 语义）要么按 MA2-045 先例 owner doc 标 Deferred 预留；salary-approval-status 删除 dict 声明（ORM 变更走 dual-agent）+ 修 ErpHrConstants javadoc。
- **arm-index 裁决**：新增（P1-MA2-045/046 覆盖 bank-file/timesheet/assignment dict 绑定维度并 resolved；CANCELLED 死值与孤儿审批 dict 未登记——grep 零命中）。

### P3-CK-hr2-016（D4/D2，同型 P3-CK-hr-012/013 家族）LeaveApproverTimeoutJob cron 三层键漂移 + 顶层失败仅 LOG.error 无告警通道

- **控制点**：`app/erp/hr/service/job/ErpHrLeaveApproverTimeoutJob.java#execute`（L96-100 `String cron = resolveCronConfig(); if (StringHelper.isEmpty(cron)) { LOG.info("...skipped..."); return; }`——内层门控键 `erp-hr.leave-approver-timeout-cron` 默认空=执行体永远跳过）对照 `app-erp-all/_vfs/nop/job/conf/erp-hr-leave-approver-timeout.job.yaml`（L2 `enabled: "@cfg:nop.job.erp-hr-leave-approver-timeout.enabled|false"` + L7 `cronExpr: "@cfg:nop.job.erp-hr-leave-approver-timeout.cron-expr|0 0 1 * * ?"` 外层双层键）+ L105-107 `catch (Exception e) { LOG.error("erp-hr-leave-approver-timeout-failed", e); }` 无 IErpSysNotificationBiz 告警
- **证据**：与 ErpHrContractExpiryJob（P3-CK-hr-012/013）完全同构：真正启用需同时配 3 键；只配 `nop.job.*.enabled=true` 时 job 每日被触发但 execute 打 info 即跳过——超时转派静默不发生。单条隔离（L139-142 warn）与幂等守卫（L159-161）在位——纯家族键漂移 + 无告警站点。beans.xml L46 `erpHrLeaveApproverTimeoutJob` + job.yaml invoker 对上，非孤立 job。
- **问题**：同型家族 hr2 站点（mfg-013/inv-021/sal-023/qa-019/hr-012）。
- **建议修复方向**：与家族联合裁决（统一 nop-job 双层键删内层键，或 README 明示三层键清单）；顶层 catch 加 notify 告警（同 hr-013 建议）。
- **arm-index 裁决**：同型登记（P1-RC-011 已修复转派功能本体（RC-R1.4 done），键漂移与告警通道是其残留工程面，归本 mission 家族）。

### P3-CK-hr2-017（D10）TaxBracketParser 空/畸形税率表致 resolveBracket 裸 IOOBE、坏 rate 值致 NPE——配置数据错误未翻译为业务错误码；latest 月 cumulativeData null 时累计静默重置

- **控制点**：`app/erp/hr/service/payroll/TaxBracketParser.java#parse`（L20-26 `taxBracketsJson` null/空/非 List → 返回**空 list**；L52-53 `NumberFormatException → return null` 静默丢字段）→ `IncomeTaxCalculator#resolveBracket` L229 `TaxBracket selected = brackets.get(0);`（空表 IndexOutOfBoundsException）+ L89 `cumTaxableIncome.multiply(bracket.getRate())`（rate null → NPE）+ `#findPreviousCumulative` L154-156 `if (latest != null && latest.getCumulativeData() != null)`——最新历史月 cumulativeData 为 null（手录薪酬/历史数据）时 result 保持空 map，累计基础静默归零重算（免征额从 0 重新累计 → 当月少预扣）
- **证据**：TaxConfig 为 CRUD 裸桩（ErpHrTaxConfigBizModel 17 行无校验），HR 手工清空/贴坏 taxBrackets JSON 后下一次核算以裸 IOOBE 冒出（非 NopException）；P1-MA4-018 修复了 cumulativeData **损坏 JSON** 响亮失败，但 **null**（合法列空值）路径仍是静默重置。首循环逐行 parseCumulativeData（L136-143，P2-MA4-008 已登记死代码面）副作用使任意历史月损坏都会抛——行为上反而扩大了响亮失败面，保留注记。
- **问题**：D10 配置边界 + 累计链健壮性残余缺口。
- **建议修复方向**：parse 空表/字段缺失抛 `NopException`（新错误码 ERR_TAX_BRACKETS_INVALID）；findPreviousCumulative 对 latest 无 cumulativeData 时 LOG.warn + 按声明式回退（重放历史 gross 重建或显式报错，按 owner doc 裁决）；顺带删除 L136-143 死循环（P2-MA4-008 站点）。
- **arm-index 裁决**：新增（P1-MA4-016/018 覆盖末档 NPE 与损坏 JSON；空表 IOOBE/null rate/null cumulativeData 三面未登记）。

### P3-CK-hr2-018（D6/D8）加班费率/加班阈值/月工作日硬编码——README 声明的三个配置点无 accessor 无消费

- **控制点**：`app/erp/hr/service/payroll/PayrollCalculator.java`（L40 `DEFAULT_OVERTIME_HOURLY_RATE = new BigDecimal("50")` 加班时薪硬编码、L299 `BigDecimal standardHours = new BigDecimal("8");` 加班阈值硬编码、L42 `DEFAULT_REQUIRED_WORK_DAYS = 22`）对照 `docs/design/human-resource/README.md` §配置点（`erp-hr.max-overtime-hours-per-month` 默认 36 / `erp-hr.default-work-hours-per-day` 默认 8 两键声明）+ `ErpHrConfigs.java` 全文无对应 accessor（grep 实证零键）
- **证据**：设计 §5.2「加班费（加班小时 × 加班费率）——费率由合同/政策决定」；实现固定 50 元/时且无 36h 月上限（法定加班上限未拦截）；跨天班次实际标准工时应取 Shift.totalWorkMinutes（排班已有该列）而实现固定 8。README 关键规则 6「个税起征点、月加班工时上限、每日标准工时均为可配置项」部分未落地（taxThreshold 已配置化，其余三键无）。
- **问题**：中国本地化配置化承诺部分未兑现（README 反模式警示「中国本地化硬编码」同族）。
- **建议修复方向**：ErpHrConfigs 补 `maxOvertimeHoursPerMonth`/`defaultWorkHoursPerDay`/`overtimeHourlyRate` accessor + PayrollCalculator 接线；加班阈值优先取当日排班 Shift 派生工时。
- **arm-index 裁决**：新增（grep「加班费率 硬编码/max-overtime」零命中）。

### P3-CK-hr2-019（D1，同型 P3-CK-hr-011）五个 BizModel 保留 processor 化前完整 legacy dup 死代码副本——双源漂移风险面扩大

- **控制点**：`ErpHrShiftBizModel.java` L94-171（requireLeaveRequest/upsertAttendanceForAbsent/ForLeave/newAttendance/updateAttendance/saveOrUpdateAttendance/updateAssignmentStatus/findAssignmentsByEmployeeRange 与 `AbstractErpHrShiftProcessor` 逐方法重复，全部 mutation 已委托 processor 后零生产调用方）+ `ErpHrShiftAssignmentBizModel.java` L111-197（doCreateAssignment/requireShift/assertNoExistingAssignment/existsActiveAssignment/activeStatuses 与 Abstract 基类重复）+ `ErpHrShiftRotationPatternBizModel.java` L70-169（findActiveAssignment/newAssignment/parseAndValidateSequence/buildShiftCodeMap/deleteExistingAssignments 与 GenerateRotationProcessor 重复）+ `ErpHrLeaveRequestBizModel.java` L137-213（checkLeaveBalance/checkDateOverlap/findBalance/sumUsedDays 与 Abstract 重复）+ `ErpHrSalarySimulationBizModel.java` L373-891（约 400 行 helper 与 AbstractErpHrSalarySimulationProcessor 全面重复）
- **证据**：同 hr-org P3-CK-hr-011 模式（processor 化重构后未删旧副本）。**漂移已发生实例**：P1-CK-hr2-005 的 readSalaryField 缺陷需双处同修；P2-CK-hr2-014 的口径分歧正是 BizModel 副本（三态）与 Processor（SCHEDULED 单态）分叉的活例——两副本 `findActiveAssignment` 状态口径已不一致（BizModel L165-171 三态 in vs RotationProcessor L101 单态 eq），静态分析难发现。
- **问题**：可维护性（有行为影响潜质——已有一处实际分叉）。
- **建议修复方向**：删除五处 BizModel 死副本（保留委托入口与 @BizLoader 活代码），或提取共享 helper 单源；与 pur-003 族修复解耦可先行。
- **arm-index 裁决**：同型登记（P3-CK-hr-011 + P2-MA4-008「applyOverride-readSalaryField 重复 switch/loadEmployee* 重复 dao-for」部分复用——本条是该家族 5 个新站点，含一处实际分叉证据）。

### P3-CK-hr2-020（D3，需求分歧只登记不裁决）leave REJECTED 无重新提交边——「修改后重新提交」只能新建申请，与 salary 审批轴 REJECTED→SUBMITTED 处理不对称

- **控制点**：`app/erp/hr/service/statemachine/ErpHrLeaveRequestStateMachine.java#assertCanSubmit`（L39-43 仅 DRAFT 合法——REJECTED 源抛非法边）+ grep 全 hr main 零 REJECTED→DRAFT/编辑重置 mutation 对照 use-cases.md UC-HR-02 基本流程 6「若 REJECTED 员工可修改后重新提交」+ 场景 B「可修改原因重新提交」；对照 `ErpHrSalaryApprovalStateMachine#assertCanSubmit` L48-55（UNSUBMITTED/null/**REJECTED** 均可 submit——驳回后重提已实现）
- **证据**：驳回的休假单永久死终态，员工只能新建申请（旧单留痕可接受但与 owner doc 措辞及同域 salary 轴先例不一致）。设计状态图（state-machine.md §2）本身无 REJECTED 出边——doc 图与用例文本也互相矛盾，按检查纪律登记不裁决。
- **问题**：D3 需求分歧（doc 内部矛盾 + 跨轴不对称）。
- **建议修复方向**：owner doc 裁决：(a) 放开 assertCanSubmit 含 REJECTED（镜像 salary 轴）；(b) use-cases 措辞改「新建申请重新提交」。连带核对前端驳回单交互。
- **arm-index 裁决**：新增（P2-RC-010 是招聘状态回退维度；grep「REJECTED 重提 leave」零命中）。

### P3-CK-hr2-021（D5/D10）computeWorkHours 无负值守卫 + 手工补卡无未来时间校验——负工时可落库并进入加班/出勤聚合

- **控制点**：`app/erp/hr/service/entity/ErpHrAttendanceBizModel.java#computeWorkHours`（L161-167 `Duration.between(clockIn, clockOut).toMinutes()` 无符号检查——clockOut 早于 clockIn 返回**负**小时数）+ `#doMakeUp` L99-131（补卡 clockTime 无「不晚于当前时间」校验、无 clockIn<clockOut 顺序校验，L124-128 两者齐备即直接 computeWorkHours）+ 补卡 date null 时 `findAttendance` 的 `eq("date", null)` 生成 IS NULL 查询（平台语义实证）不命中即新建 date=null 行（依赖 DB 非空约束兜底）
- **证据**：补卡是绕过打卡时序守卫的受控通道（RC-R1.7），但通道本身无时间方向校验：HR 补签退 09:00 / 补签到 08:30（早于签到）→ workHours=-0.5；未来时间戳补卡 → 未来考勤行。负/未来 workHours 进入 `PayrollCalculator#summarizeAttendance`（L298-303 workHours>8 计加班——负值被跳过，无直接错算）与工时报表。正常打卡路径 clockOut=now ≥ clockIn 由时间单调性天然保证。
- **问题**：入参边界缺失（同 P3-CK-mfg-012 负数族 hr2 站点）。
- **建议修复方向**：computeWorkHours 负值归零或抛错；doMakeUp 校验 clockTime 非未来 + 方向；date null 前置拒绝。
- **arm-index 裁决**：新增（RC-R1.7 修复记录声明守卫=角色+reason，未含时间边界；同族注记 P3-CK-mfg-012/P3-CK-hr-009）。

### P3-CK-hr2-022（D8）leave 联动三处小漂移：onLeaveCancelled 无条件置 SCHEDULED、approve 清空转派 approverId、resolveApproverId 恒 null

- **控制点**：`ErpHrShiftOnLeaveCancelledProcessor.java#onLeaveCancelled`（L28 `a.setStatus(ASSIGNMENT_STATUS_SCHEDULED)` 无条件——设计 shift-scheduling.md §6.2 逐字「status → SCHEDULED（**如有打卡数据则 PRESENT**」）+ `ErpHrLeaveRequestApproveProcessor#approve` L30 `leave.setApproverId(resolveApproverId(context));`（`AbstractErpHrLeaveRequestProcessor#resolveApproverId` L175-178 `return null;`——approve 把超时转派 job 写入的 approverId **清空**，转派审计轨迹丢失）+ 依赖超时 job 才有 approverId 值的非常规数据流
- **证据**：① 已到岗（有 clockIn）日休假取消后排班回 SCHEDULED 而非 PRESENT，actualStartTime/EndTime 不回填——下次 calcAttendance 才自愈；② approve 显式 set null 覆盖 job 转派目标：转派→审批两步后 approverId 字段为 null，无法追溯实际审批人（approvedAt 有值 approverId 无值的半审计态）。
- **问题**：联动/审计小漂移（D8），无金额影响。
- **建议修复方向**：onLeaveCancelled 查 attendance 有打卡则置 PRESENT；approve 保留已有 approverId（resolveApproverId 实装取当前用户关联员工或留空不覆盖）。
- **arm-index 裁决**：新增（P1-RC-011 修复了转派 job 本体，approverId 被 approve 清空是其与 approve 链的交互缺口；grep「approverId 清空/取消 PRESENT」零命中）。

### P3-CK-hr2-023（D6/D10）休假期间边界三处：跨月休假整单漏算无薪扣减、跨年休假全额计入起始年余额、endDate<startDate 得 0 天可提交

- **控制点**：`app/erp/hr/service/payroll/PayrollCalculator.java#sumUnpaidLeaveDays`（L323-324 `ge("startDate", periodStart)` + `le("endDate", periodEnd)`——**双端闭区间包含过滤**：7/28-8/05 的病假在 8 月核算时因 startDate<8/1 被整单排除，无薪扣减漏算；反之完全落在期内但跨年的 12/28-1/05 durationDays 22 天全额计入）+ `AbstractErpHrLeaveRequestProcessor#sumUsedDays` L160-166（`dateBetween("startDate", yearStart, yearEnd)` 按起始年全额计——次年 1 月那 5 天不在任何年度 used 中，两年余额口径失真）+ `#computeDurationDays` L98-103（`Math.max(days, 0)`——endDate 早于 startDate 时 durationDays=0 静默通过提交/审批，落库语义矛盾单据）
- **证据**：三处均为期间切分缺失（按整单而非按期间交集切分天数）；0 天单可 APPROVED 并联动排班标记（onLeaveApproved 按日期范围标记——倒序区间 dateBetween 反查 assignments 也为空，无爆炸但产生 0 天已批准脏数据）。
- **问题**：D6 边界（中低频：跨月请假/跨年请假/录入倒序）。
- **建议修复方向**：sumUnpaidLeaveDays 改「区间与核算期交集天数」；sumUsedDays 同切分或按天展开；submit 校验 endDate>=startDate 拒绝倒序。
- **arm-index 裁决**：新增（grep「跨月 无薪/跨年 余额/durationDays 0」零命中）。

### P3-CK-hr2-024（D5/D7）排班调换 submit/approve 边界：无同日期校验（跨日交换语义未定义）+ 同 assignment 多 PENDING 双批准无守卫

- **控制点**：`app/erp/hr/service/processor/ErpHrShiftSwapRequestSubmitProcessor.java#submit`（L20-39：不校验 source/target assignmentDate 同日、不校验 source 归属 requester、不查同 assignment 已有 PENDING swap）+ `ErpHrShiftSwapRequestApproveProcessor#approve`（L19-49：不校验双方 assignment 是否已被前一个已批准 swap 改写过/已逻辑删除后的悬挂——requireEntity 删除行会抛但已 swap 的再次互换无守卫）
- **证据**：跨日 swap：A 周一班 ↔ B 周二班 → 各自在原日期换成对方班次类型（周一上 B 的周二班型、周二上 A 的周一班型）——与「同日换班」直觉语义偏差且设计 §5.2 未明确；同一 source 发两个 PENDING swap 先后 approve → 班次被换两次（第二单以已换班次为源再换）。均无金额影响、可人工撤销。
- **问题**：入参边界 + 并发/顺序守卫缺失（P3，当前消费面无放大）。
- **建议修复方向**：submit 校验同 assignmentDate + source.employeeId==requester + 存在 PENDING swap 即拒；approve 前校验双方 status 仍为 SCHEDULED。
- **arm-index 裁决**：新增（grep「swap 同日/调换 校验」零命中）。

### P3-CK-hr2-025（D7）休假余额并发扣减竞态——approve 读派生 used 无锁，两笔并发 approve 均过校验超额审批

- **控制点**：`app/erp/hr/service/processor/ErpHrLeaveRequestApproveProcessor#approve`（L27 `checkLeaveBalance(leave, context)` → `AbstractErpHrLeaveRequestProcessor#sumUsedDays` L159-173 实时 SUM 查询 APPROVED 天数——读-验-写无余额行锁/无版本碰撞点：余额扣减是**派生值**（used=Σ approved），ErpHrLeaveBalance 行本身从不被 approve 更新，无乐观锁冲突可用）
- **证据**：余额剩 5 天，两笔各 4 天的年假并发 approve：T1/T2 都读到 used=N（互不可见对方未提交行）→ 均通过 → 合计用 8 天超 3 天。UK/乐观锁不拦截（写的是 LeaveRequest 行，Balance 行不动）。触发需同员工同假别并发审批（同一审批人双开页面/双人审批），窗口窄；事后可通过 cancel 修复（余额自动回补）。
- **问题**：任务指定核查点「休假余额并发扣减竞态」确认存在，影响可逆（P3）。
- **建议修复方向**：approve 对 Balance 行做一次 `updateEntity` 触碰（version+1 乐观锁串行化）或 DB 端 `SELECT FOR UPDATE`（validateBeforeUpdate 钩子）；或接受并在 owner doc 登记。
- **arm-index 裁决**：新增（grep「余额 并发 竞态」零命中）。

### P3-CK-hr2-026（D6/D8）convertToFormal 部分冲突清单静默丢弃 + 个税跳档告警为有效税率近似且仅检升档

- **控制点**：`app/erp/hr/service/processor/ErpHrSalarySimulationConvertToFormalProcessor.java#convertToFormal`（L39-104 `List<Map<String, Object>> conflicts` 组装后仅在**全员冲突**时用于选错误码——部分冲突时循环结束直接 `simulation.setStatus(CONVERTED)` L106，conflicts 未返回未记录，调用方无法得知哪些员工被跳过）对照设计 payroll-simulation.md §4.2「部分员工已在目标期间有薪酬 | 仅转换无冲突的员工」——跳过语义实现但结果不可见；+ `ErpHrSalarySimulationBizModel#taxBracketJumped` L805-810（`simRate.subtract(srcRate).compareTo(0.05) > 0`——有效税率差 >5pp 作代理，边际 3%→10% 跳档若有效税率变化 <5pp 漏报；降档（10%→3%，退税场景）不检测）
- **证据**：部分冲突静默跳过的员工当月无正式薪酬，HR 以为全员已转——需逐人比对才发现；`convertedSalaryId` 仅存 firstConvertedId（L107，设计 §4.3 单 FK 已裁决接受，注记不另立）。
- **问题**：结果可见性（D8）+ 告警近似（D6，注释自认近似）。
- **建议修复方向**：convertToFormal 返回值/通知携带 conflicts 清单（或 notes 字段落盘）；taxBracketJumped 若需精确改查税率档位对比（resolveBracket 两边档位 index 比较）。
- **arm-index 裁决**：新增（grep「conflicts 丢弃/跳档 近似」零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | hr2 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` / `P2-CK-hr-004`（CRUD update 无守卫全域族） | salary（直写 approveStatus 绕计提/直写 paymentStatus 绕 280）/attendance/leave/simulation 16 实体 | 同型登记为 P2-CK-hr2-007（并入 hr-004 家族） |
| cron 三层键漂移家族（mfg-013/inv-021/sal-023/qa-019/hr-012）+ job 无告警（hr-013） | `erp-hr.leave-approver-timeout-cron` 内层键 + `nop.job.*` 外层双层键 + 顶层 catch 仅 log | 同型登记为 P3-CK-hr2-016 |
| `P3-CK-hr-011` legacy dup + `P2-MA4-008` 重复 switch/重复 dao-for | Shift/ShiftAssignment/ShiftRotationPattern/LeaveRequest/SalarySimulation 五 BizModel 死副本（含一处实际口径分叉证据） | 同型登记为 P3-CK-hr2-019 |
| `P3-CK-mfg-012` 负数入参静默族 | computeWorkHours 负值/补卡未来时间 | 同族站点 P3-CK-hr2-021 |
| `P2-CK-fin2-007`/`P2-CK-mfg-007`/`P2-CK-hr-003` orgId 隔离族 + limit 截断族 | `findActiveEmployees`/`findPayableSalaries`/`findPayrollSummary(limit 2000)`/`computeAllEmployeeSims(limit 10000)` 均无 orgId 过滤、findPayrollSummary 硬 limit 静默截断 | orgId 维度并入 P2-CK-hr2-013（N+1 站点内注明）；limit 2000 截断同 hr-003 族——**不另立**（修复时随 013 一并处理，此注记即登记面） |
| 计提红冲对称性 Deferred（payroll.md §6.5 successor，对齐 P1-MA2-083 范式） | **voidSalary 对 APPROVED 已计提薪酬的作废**：paymentStatus PENDING + approveStatus APPROVED + posted=true + 270/290/300 已过账 → void 后凭证不红冲、posted 仍 true、无告警 | Deferred 家族的第三条撤销路径（原注记仅列反审/驳回），**随 successor 一并裁决不另立**——修复该 successor 时须把 voidSalary 纳入触发面 |
| `P1-MA2-045`（bank file UPLOADED/CONFIRMED 死状态） | payroll.md §七 Deferred 注记在位（18 行桩 + 预留声明） | 复用不重复登记（验证为正确节印证） |
| `P1-RC-011/012/013/014`（超时转派/多次打卡/跨天/补卡） | 逐一 HEAD 复核全部已修复在位（见验证为正确节） | 复用 resolved，不登记 |
| `P1-MA4-016/017/018`（末档 NPE/计提链/累计损坏 JSON） | HEAD 复核修复在位 | 复用 resolved，不登记 |
| `P2-MA4-008`（IncomeTaxCalculator 死代码首循环等 6 合并项） | 首循环 L136-143 仍在（现为副作用校验循环） | 复用，随 P3-CK-hr2-017 修复一并清理 |

## 验证为正确（显式排除，防误报）

- **七级累进个税税率表与速算扣除数种子全对**：`erp_hr_tax_config` 种子（`TestErpHrPayrollEngine` case CSV + E2E）七档 `36000/0.03/0`、`144000/0.10/2520`、`300000/0.20/16920`、`420000/0.25/31920`、`660000/0.30/52920`、`960000/0.35/85920`、`null/0.45/181920`——与中国个税法（2018 修正）综合所得表逐值一致；**边界等号正确**：`resolveBracket` L235 `compareTo(upper) > 0 → continue`，收入恰等于 36000/144000 等上限时取**低档**（≤ 语义），任务指定核查点通过。
- **末档 null 防御在位（P1-MA4-016 已修）**：`IncomeTaxCalculator#resolveBracket` L231-233 `if (b.getRangeUpperLimit() == null) { selected = b; break; }` + `TaxBracketParser#parse` L38-42 排序 null 归末——高收入员工不再 NPE，`testHighTaxBracketIntegrationE2e` 覆盖。
- **累计个税损坏 JSON 响亮失败（P1-MA4-018 已修）**：`parseCumulativeData` L184-190 LOG.warn + `ERR_HR_CUMULATIVE_DATA_CORRUPT`——不静默重置（null cumulativeData 残余面归 P3-CK-hr2-017）。
- **计提过账链已接线（P1-MA4-017 / RC-R1.89 已修）**：`ErpHrSalary.xbiz` approve mutation（L64-79）状态写回后委托 `ErpHrSalaryPostApprovalProcessor#postAccruals`——270→290→300 顺序、非 short-circuit `&` 聚合、三条全成才 `posted=true`、`shouldSkip` 幂等防御、`alreadyPosted` 去重守卫命中计成功——与 payroll.md §6.5 记载一致；`testCompanyBornePostingAccrualChainPositive`/`testReverseApproveReApproveDedupAndCatchUp` 等覆盖。
- **过账失败 G3 告警在位**：四条 try* 路径 catch 后 `dispatchFailureAlert`（`hr.salary-posting-failure` + stage/errorCode/postingNo）+ notify 自身降级不阻断——悬挂可运营感知。
- **组织/账套/本位币解析在位**：`applyOrgAndSchema` L343-349 salary.orgId 回退员工 org → `AcctSchemaResolver` 主账套 → 本位币——RC-R1.89 补齐的空 schema 静默零凭证缺口已闭合。
- **markPaid 双轴守卫完整（D3/D5 核查点）**：`approvalStateMachine.assertCanMarkPaid`（require APPROVED）+ `paymentStateMachine.assertCanMarkPaid`（require PENDING）双前置 + 领域码映射 cause 保留；**「已 PAID 薪资可否 void」→ 不可**：`ErpHrSalaryBizModel#voidSalary` L117-120 显式 `ERR_SALARY_LOCKED_AFTER_PAID` 守卫（payroll.md §6.4/§11.6 语义落地）；VOID→VOID 二次作废被 assertCanVoid 拒。
- **paymentStatus/approveStatus dict writer 全覆盖（任务指定 D3 核查点）**：`erp-hr/salary-payment-status` 三值——PENDING（calculateSalary/runPayroll/convertToFormal）、PAID（markPaid/generateBankFile）、VOID（voidSalary）；`wf/approve-status` 四值——UNSUBMITTED（calculate + withdrawApproval xbiz）、SUBMITTED（submitForApproval）、APPROVED（approve）、REJECTED（reject）。**无死状态**（对照 dict L72-76/L742 列绑定）。simulation-status 5 值（DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED）与 swap-status 4 值（PENDING/APPROVED/REJECTED/CANCELLED）亦全有 writer——唯一死值为 assignment CANCELLED（P3-CK-hr2-015）。
- **乐观锁全实体在位（D5）**：切片 16 实体（Salary/Attendance/LeaveRequest/LeaveBalance/Shift/ShiftAssignment/ShiftSwapRequest/ShiftRotationPattern/TaxConfig/TaxSpecialDeduction/SocialInsuranceBase/Config/SalaryItem/SalarySimulation/ItemAdjustment/PayrollBankFile）ORM 全部 `versionProp="version"`（python 抽取逐实体实证）。
- **D1 机械扫描全零**：切片 44 文件 `System.currentTimeMillis/LocalDateTime.now/new Date()`=0（一律 CoreMetrics）、`extends RuntimeException/Exception`=0（全 NopException + ErpHrErrors）、`@Inject private`=0、字典字符串 `==` 比较=0（两处 `== null` 命中为正确 null 判断）；`StringHelper`/`JsonTool` 平台工具使用规范。
- **beans.xml 接线完整（D4）**：50 bean 含 calculators×4 + posting×3 + 全部 16 processor + 3 状态机 + `erpHrLeaveApproverTimeoutJob`；`erp-hr-leave-approver-timeout.job.yaml` invoker `bean/method` 与 beans.xml L46 对上；`ErpHrSalary.xbiz` 注入的 Guard/PostApproval/StateMachine bean id 均在——无孤立声明/漏调。salary 无 nop-job 注册与 payroll.md §实现约定「cron 自动核算归 follow-up」一致（D4 salary=N/A 声明）。
- **RC 系列 HEAD 复核全部在位**：跨天签退回退（ClockOutProcessor L42-52 昨日+跨天排班双条件，孤儿 assignment 保守拒绝）、多次打卡 last-wins（ClockInProcessor 覆盖式 setClockIn，P1-RC-012 修复）、手工补卡（doMakeUp HR 角色 fail-closed + reason 必填 + source=MANUAL + remark 审计，MANUAL dict 孤儿值有 A4.2.145 先例注记）、超时转派 job（上级→部门负责人兜底链 + 幂等守卫 + 单条隔离 + SCAN_LIMIT 200 分页 + updateTime 窗口语义妥协有注释论证，P1-RC-011 修复）。
- **attendance UK + 冲突兜底**：`UK_HR_ATTENDANCE_EMP_DATE(employeeId,date)` 在位；ShiftAssignment `UK_HR_SHIFT_ASSIGNMENT_NATURAL(employeeId,assignmentDate,shiftId,delVersion)` + doCreateAssignment flush-catch 翻译（P1-MA2-091 修复范式）主路径在位（rotation 旁路归 P3-CK-hr2-014——注意其逻辑删除设计（delVersion 参与 UK）本身正确且注释自证理由）。
- **cancel 余额返还无需显式回滚**：余额 used 为派生 SUM（APPROVED 聚合），cancel→CANCELLED 自动退出聚合——设计良好，无冗余失同步面（对照「扣减/返还双写失衡」类风险显式排除）。
- **submit+approve 双重余额校验**：两入口均 checkLeaveBalance（submit L20 + approve L27），重复校验偏保守方向正确。
- **monthTax 负值归零符合预扣规则**：累计预扣预缴年内不退（多扣部分年度汇算清缴退），`cumPrepaidAfter` 按实扣累计口径一致（L96-100）。
- **模拟隔离正确**：`recalculateWithOverrides` 克隆源 `cloneInstance` + 清主键 `orm_propValue(1, null)` 不污染正式行；simulation 无正式表落库路径（CONVERTED 前零正式写入）——设计「模拟不影响正式数据」落地。
- **PII/薪酬脱敏在位**：Salary 13 金额字段 + cumulativeData、SocialInsuranceBase 两基数、ItemAdjustment 两金额全部 @BizLoader 委托 MaskHelper fail-closed（E3.1 范式）。
- **processor 经 daoFor(本域实体) 更新 + 跨域经 I\*Biz**：posting 跨域经 `IErpFinVoucherBiz` Facade（REQUIRES_NEW 独立事务边界声明）、notify 经 `IErpSysNotificationBiz`、排班联动经 `IErpHrShiftBiz`——processor-extension-pattern 合规；PayrollCalculator 读本域配置/合同经 daoProvider 属计算引擎只读范式（对齐 projects TimesheetPostingDispatcher 读 master 模式，P1-MA1-022 读侧豁免内）。
- **calcLateMinutes/calcEarlyLeaveMinutes/跨天边界正确（D6 核查点）**：`diffMinutes <= grace → 0`（宽限期含等号，设计 §4.1「clockIn ≤ start+grace 不算迟到」一致）；跨天班次早退基准取次日 endTime（§4.2）；`isCrossDayShift` end≤start 判定含 24h 边界。
- **makeUpClockIn/Out 复用 computeWorkHours 重算**：补齐双卡后按既有口径重算 workHours（L124-128）。
- **generateBankFile 批内原子性**：@BizMutation 单事务（循环 updateEntity + bankFile save 同事务）——无部分提交窗口（对照 hr-org「job 直调无事务」不适用：本方法经 GraphQL mutation 入口有事务）。

## arm-index 复用 or 新增裁决（汇总）

- 新增关键符号（arm-index 零命中）：`出勤比例 22`/`年假 扣薪`/`generateBankFile 过账`/`跳过并告警`/`effectiveFrom 社保`/`getComparison 派生字段`/`tryPostPayment 去重`/`salary UK`/`findActiveContract`/`ALLOWANCE basicSalary`/`余额 null 跳过`/`重叠 假别`/`generateRotation UK`/`assignment CANCELLED 死状态`/`孤儿审批 dict`/`TaxBracketParser 空表`/`加班费率 硬编码`/`REJECTED 重提 leave`/`computeWorkHours 负值`/`approverId 清空`/`跨月 无薪`/`swap 同日`/`余额 并发`/`conflicts 丢弃` → **新增**。
- **复用（不重复登记，报告中注记）**：
  - CRUD 无守卫 → **P1-CK-pur-003 族 / P2-CK-hr-004**（本 mission）——P2-CK-hr2-007 同型登记（hr2 站点并入）。
  - cron 三层键 + job 无告警 → **P3-CK-hr-012/013 家族**（本 mission）——P3-CK-hr2-016 同型登记。
  - legacy dup → **P3-CK-hr-011**（本 mission）+ **P2-MA4-008**（arm）——P3-CK-hr2-019 同型登记（新站点 + 实际分叉证据）。
  - 银行文件 UPLOADED/CONFIRMED 死状态 → **P1-MA2-045**（resolved Deferred）——验证为正确，不登记。
  - 超时转派/多次打卡/跨天/补卡缺失 → **P1-RC-011/012/013/014**（全 resolved）——HEAD 复核在位，不登记。
  - 末档 NPE/计提链未接线/累计静默重置 → **P1-MA4-016/017/018**（全 resolved）——复核在位，不登记。
  - IncomeTaxCalculator 死代码首循环 → **P2-MA4-008**（arm）——随 P3-CK-hr2-017 修复清理，不单独计数。
  - 负数入参静默 → **P3-CK-mfg-012 族**——P3-CK-hr2-021 同族注记。
  - orgId 隔离/limit 截断 → **P2-CK-hr-003/fin2-007/mfg-007 族**——并入 P2-CK-hr2-013 注记。
  - 计提红冲缺失 → payroll.md §6.5 Deferred successor——voidSalary 路径随家族裁决（注记表），不另立。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 5 | P1-CK-hr2-001..005 |
| P2 | 9 | P2-CK-hr2-006..014 |
| P3 | 12 | P3-CK-hr2-015..026 |

按主维度：D6×7（001/004/005/011/012/018/023 跨 D10 主归 D6）、D5×5（008/009 主 D5；007 同型主 D5；014/021/024 主 D5/D7 计 D5×3 → 精确：007 D5、008 D5、009 D5、014 D5、021 D5、024 D5）、D7×4（006/010/013 跨 D9 主 D9→013 计 D9、025）、D8×3（002/022/026）、D3×3（015/020/016 跨 D4 主 D4→016 计 D4）、D9×1（013）、D4×1（016）、D2×0（016 跨计）、D1×1（019）、D10×1（017）。（精确主维度归属：001 D6、002 D8、003 D6、004 D6、005 D6、006 D7、007 D5、008 D5、009 D5、010 D7、011 D6、012 D6、013 D9、014 D5、015 D3、016 D4、017 D10、018 D6、019 D1、020 D3、021 D5、022 D8、023 D6、024 D5、025 D7、026 D8。）

同型/复用裁决：同型登记 4（007 pur-003 族 / 016 cron 家族 / 019 hr-011+MA4-008 / 021 mfg-012 族注记）+ 复用不登记 9 项（MA2-045/RC-011/012/013/014/MA4-016/017/018/MA4-008-首循环）+ Deferred 家族并入 1（voidSalary 红冲）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 44 生产文件逐行深读（PayrollCalculator 345 行/IncomeTax 247/SocialInsurance 127/Dispatcher 394/Simulation BizModel 944 行/Abstract 479 行等）；ORM 16 实体 versionProp/UK/列集/dict 全值 python 抽取；beans.xml 50 bean 与 xbiz/job-yaml 注入核对；税率种子 CSV 与法规表逐值核对；平台语义实证（REQUIRES_NEW/eq-null/事务入口沿用已验证先例）；测试 6 文件用例语义交叉（PayrollEngine 10 用例断言口径逐条比对——出勤折算与对比视图零断言即为 001/005 佐证）；arm-index 全 hr 相关条目（MA2-039..091/MA4-016..019/RC-011..016/MA2-045/046/052/088 等）逐条裁决。
- **未深查**：`ErpHrReportBizModel`（375 行读侧报表聚合，仅模式抽查——@BizQuery 只读 + 跨域 daoFor 读在 P1-MA1-022 豁免内，其正确性归报表域后续）；`SalaryPostingProvider` 本体（createFacts 模板面，PostingChain 测试 5 组覆盖，凭证模板正确性属 finance 域已检范围）；erp-hr-web AMIS 页面与后端契约 drift（归 C8.2）；leave submit 时 `dateBetween` 传 null 边界的平台行为（GraphQL NonNull 拦截主入口，Java 直调面未验证——若复现可并入 023 修复）；`TaxBracket` 类（纯 record）；测试代码正确性（仅用于行为语义交叉验证）；xmeta 层入参校验对 021/024 触发面的进一步收窄。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-hr2-002**（generateBankFile 无 280 过账）——owner doc 内部两处表述冲突（§5.2 银行文件路径含过账 vs §9.1 表 bind 到 markPaid）：若裁决「银行文件生成仅出文件、发放确认走逐条 markPaid」则本条降为「设计歧义 + 文档对齐」P3；但当前实现**没有任何路径**为银行文件批量翻 PAID 的薪酬补 280，建议以「银行文件批量发放后 GL 应付职工薪酬余额」的集成断言实证定级。
  2. **P1-CK-hr2-001**（22 天分母）——若产品裁决「考勤未全面铺开前 actualDays 兜底全勤即关闭折算」（config-gated 关闭出勤折算），则短月扣薪不可触发降 P2；但字面上「打 20 天卡按 20/22 发薪」在当前代码无条件生效，且与「零打卡全勤」口径矛盾无争议，建议以 2 月场景集成测试实证。
  3. **P2-CK-hr2-004**（社保有效期）——若运营约定「年调时物理更新旧行而非插新行」（当前 seed 单行），双倍扣缴不可触发；但 ORM 有效期列 + 设计「年调 7 月」语义明确支持多行并存，读取侧零过滤的字面事实无争议。与 P2-CK-hr2-011（findActiveContract）同属「多行历史数据形态下的读取语义」族，建议主 agent 统一定级方向。
  4. **P1-CK-hr2-005 修复面**——readSalaryField 双副本（BizModel + Abstract）须同步修，且 AMIS 对比页若只消费 8 个输入行则用户面影响收窄（后端仍错）——修复时核对 `erp-hr-web` 对比页消费列。
