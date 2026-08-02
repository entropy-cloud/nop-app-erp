# ARM-MA4 hr 薪酬/过账/模拟引擎代码质量专属审查报告（A4.4）

> 里程碑：MA4（代码与前端质量层 / 代码实现质量维度）
> Roadmap 工作项：A4.4（hr 代码质量审计——S 级，92 mutation 全域第二高[finance 137 之后]/ 测试比 0.16 全域最低）
> Plan：`docs/plans/2026-07-29-0430-1-audit-remediation-ma4-hr-code-quality.md`
> 行为基线：`docs/design/human-resource/{state-machine,payroll,payroll-simulation,recruitment,shift-scheduling,competency-management,employee-survey}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/code-quality-audit-prompt.md`（7 重点领域 + P0-P3 严重性指南）
> 实仓快照：2026-07-29（`find module-hr -path "*service*" -name "*.java"` = 68 文件 / 37 BizModel；核心组件全部存在；个税税率表 seed 7 档末档 `rangeUpperLimit:null` 实证）
> 裁决：**Verdict = ⚠️(P1)**——hr 薪酬/过账/模拟链路代码实现质量在**社保基数钳制算术（SocialInsuranceCalculator clamp min/max）/ 公积金回退基数 / 累计预扣法编排结构（IncomeTaxCalculator 累计快照逐月写回）/ 社保/公积金/个税 BigDecimal 全域货币类型安全 / 模拟 What-If 覆盖重算隔离（recalculateWithOverrides 克隆源快照不污染）/ 跨域 Facade（SalaryPostingExecutor→IErpFinVoucherBiz REQUIRES_NEW，hr production 代码零 `daoFor(ErpFin*)` 跨域写）/ 异常规范化（全 NopException + ErpHrErrors `erp.err.hr.*` ErrorCode + 作用域参数键）** 七面扎实；零 P0——无活跃**静默**数据破坏路径（个税高档 NPE 为**响亮崩溃**非静默错算，详见 §5.2 P0 评估）。**4 项新 P1**（P1-MA4-016 `IncomeTaxCalculator.resolveBracket` 个税高档税率 NPE——累计应纳税所得额 > 960000 触达末档 `rangeUpperLimit:null` 致 `compareTo(null)` 抛 NPE，runPayroll 整批回滚/单员工 calculateSalary 失败，薪酬算术直接影响实发工资；P1-MA4-017 业财过账链路不完整——计提凭证 SALARY(270) + 社保公司 SOCIAL_INSURANCE_ER(290) + 公积金公司 HOUSING_FUND_ER(300) 三类 PostingEvent 永不生成[`tryPostAccrual` 死代码零调用方 + 公司承担金额 socialInsuranceER/housingFundER 计算后丢弃]，payroll.md §6/§9.1 声明的 approveStatus→APPROVED 联动过账未落地，GL 永远仅收发放凭证 SALARY_PAYMENT(280)；P1-MA4-018 个税累计数据健壮性——`IncomeTaxCalculator.parseCumulativeData:173` `catch(Exception ignored)` 静默吞 JSON 解析异常致累计损坏时静默重置为空 → 少预扣个税（直接影响实发/税务合规）；P1-MA4-019 hr 薪酬/过账链路测试有效性系统性不足[测试/mutation 比 0.16 全域最低]——个税高档边界 / 过账悬挂 / 累计 JSON 解析失败 / 公司承担过账缺失 / 计提从未触发 五大异常路径零覆盖）+ **2 项新 P2** watch-only（P2-MA4-008 可维护性热点合并[Survey/PayrollBankFile 18 行 CRUD 桩治理 + Timesheet 硬编码字符串 P1-MA2-044 复核维持 + IncomeTaxCalculator 死代码首循环 + applyOverride/readSalaryField 重复 switch-case + loadEmployee* 重复 dao-for 模式]；P2-MA4-009 自动化防护[compliance checker 无 hr 薪酬算术回归门控——高档 NPE / 累计解析失败 / 公司承担过账无 CI 门控]）。MA1/MA2/MA3 已知 finding 运行时复核 **10 项中 9 项「如登记」无升级**；**1 项复核发现新代码层缺陷**——P1-MA2-048（markPaid/tryPostAccrual 吞异常悬挂）复核时发现**整条计提+公司承担过账链路未接线**（P1-MA4-017，MA2 P1-MA2-047 仅标 javadoc drift + posted 死字段，未发现 tryPostAccrual 零调用方 + ER 金额丢弃）。本审计原则上**无 P0**（个税高档 NPE 为响亮崩溃非静默错算 + 公司承担过账缺失为可经试算平衡发现的漏记 + parseCumulativeData 静默吞需损坏 JSON 前置）。

---

## 1. 范围与基线

### 1.1 在范围（代码实现质量，非状态机业务正确性）

`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/` 员工与组织 + 考勤与工资 + 过账链路 + 计算引擎代码（68 文件，含 15 测试）：

| 子系统 | 组件 | 文件 | 职责 |
|--------|------|------|------|
| **计算引擎** | 个税累计预扣 | `payroll/IncomeTaxCalculator.java` | 七级累进 + 累计快照逐月写回 cumulativeData |
| | 社保/公积金 | `payroll/SocialInsuranceCalculator.java` | 基数钳制 + 个人/公司比例分离 |
| | 税率表解析 | `payroll/TaxBracketParser.java` + `TaxBracket.java` | JSON 税率表解析 + 升序排序 |
| **薪酬编排** | 薪酬核算 | `payroll/PayrollCalculator.java` | 基本工资→津贴→加班→社保→个税→实发编排 + What-If 覆盖重算 |
| | 薪酬 Facade | `entity/ErpHrSalaryBizModel.java` | calculateSalary/runPayroll/markPaid/voidSalary/generateBankFile |
| **过账链路** | 派发器 | `posting/SalaryPostingDispatcher.java` | tryPostAccrual/tryPostPayment（吞异常语义） |
| | 执行器 | `posting/SalaryPostingExecutor.java` | 跨域经 IErpFinVoucherBiz Facade |
| | 科目文档 | `posting/SalaryPostingProvider.java` | SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER/HOUSING_FUND_ER 四类 |
| **模拟引擎** | What-If | `entity/ErpHrSalarySimulationBizModel.java` | createSimulation→adjustItem→submitForReview→approve→convertToFormal |
| **员工与组织** | 员工/合同/招聘 | `entity/{ErpHrEmployee,ErpHrEmploymentContract,ErpHrRecruitment}BizModel.java` | 调岗/续签/到期/招聘 hire 联动 |
| | 调查/发展计划 | `entity/{ErpHrSurvey,ErpHrDevelopmentPlan}BizModel.java` | CRUD 桩 / 差距→计划生成 |

### 1.2 不在范围（Non-Goals 见 plan）

- A2.7a/b hr 状态机业务正确性（done）——本审计复核其 finding 运行时状态，不重复审计
- A4.1a finance 过账 Facade（IErpFinVoucherBiz）实现质量（done）——本审计复核 hr 侧 dispatcher 调用点错误传播
- A4.8 view.xml drift / A5.3 测试覆盖深度统计 / A6.x 权限注解完整性 / A3.3 owner doc drift
- 代码缺陷批量修复（在 MR2/MR1）

---

## 2. 7 重点领域逐项审查

### 领域 1：架构和边界完整性（裁决：**PASS——hr production 代码零跨域 `daoFor(ErpFin*)` 写，边界全合规**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| SalaryPostingDispatcher 经 IErpFinVoucherBiz Facade 过账（非凭证直写） | ✅ PASS | `SalaryPostingExecutor:23-29` 经 `voucherBiz.post(event, context)` Facade（REQUIRES_NEW 承接）。`SalaryPostingDispatcher` 仅组装 `PostingEvent`（finance 域 dao 实体作值对象传递），无 `daoFor(ErpFinVoucher)` 直写凭证 |
| 计算引擎读 master/配置合规 | ✅ PASS | `SocialInsuranceCalculator`/`IncomeTaxCalculator`/`PayrollCalculator` 全部 `daoFor(ErpHr*)`（社保基数/配置/税率表/考勤/休假）——**全部同域（hr）只读**。无 `daoFor(ErpMdSubject/ErpMdPartner)` 跨域读科目；科目经 config `erp-hr.default-payroll-subject-id` 字符串解析（`SalaryPostingDispatcher.buildAccrualEvent:82-86`）。**无跨域 master 读违规** |
| 招聘 hire 联动建员工+合同经 I\*Biz | ✅ PASS | `ErpHrRecruitmentBizModel.hire:105-119` 经 `employeeBiz.saveEntity` + `employmentContractBiz.saveEntity`（同域 I\*Biz），非 daoFor 直写。createEmployeeFromRecruitment 异常包装 `ERR_RECRUITMENT_EMPLOYEE_CREATE_FAILED` |
| hr Dashboard facade read-only 聚合（P1-MA1-022 永久接受复核） | ⚠️ 维持 | Dashboard facade read-only 聚合经 P1-MA1-022 永久接受。**复核「如登记」**——非薪酬/过账/模拟链路本审计主路径，无活跃数据破坏 |
| 模拟引擎跨实体读 salary 经 I\*Biz + Employee 只读 | ✅ PASS | `ErpHrSalarySimulationBizModel` 经 `IErpHrSalaryBiz`（同模块）+ `daoProvider().daoFor(ErpHrSalary/ErpHrEmployee/ErpHrPosition/ErpHrDepartment)`（**全部同域 hr** 只读）。无跨域违规 |

**裁决**：hr 薪酬/过账/模拟链路**跨域写经 I\*Biz Facade 全合规**，跨域读零违规（与 assets/mfg 不同——hr 计算引擎不读 ErpFinAccountingPeriod/ErpMdSubject）。**无新边界违规站点**。Dashboard facade read-only 永久接受（P1-MA1-022）。

### 领域 2：核心实现正确性（裁决：**FAIL——个税高档 NPE（P1-MA4-016）+ 业财过账链路不完整（P1-MA4-017）+ P1-MA2-048 吞异常悬挂复核「如登记」**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **个税累计预扣法算术正确性** | ❌ FAIL | `IncomeTaxCalculator.calculate:65-111` 累计结构（cumGross/cumThreshold/cumSpecial/cumAdditional → cumTaxableIncome → 累计税额 − 累计已预扣 = 当月应纳）公式与 payroll.md §4.5 一致，cumTaxableIncome 负值钳零（`:79-81`）、cumTaxAmount 负值钳零（`:87-89`）、monthTax 负值钳零（`:93-95`）三重兜底正确。**但 `resolveBracket:204-214` 在累计应纳税所得额 > 末档上界时 NPE**（详 P1-MA4-016）——**直接影响实发工资（个税）的高档收入算术缺陷** |
| **社保基数钳制算术正确性** | ✅ PASS | `SocialInsuranceCalculator.clamp:112-121` `min(max(base, lower), upper)` 双向钳制；`calculate:49-61` 跳过 HOUSING_FUND 只算五险；`calculateHousingFund:66-85` 优先 housingFundBase 缺失回退社保基数。TestErpHrPayrollEngine.testSocialInsuranceBaseClampingAndGrossNet 断言基数 50000 钳到上限 32694 后个人扣款=32694×8%=2615.52 / 公积金个人=32694×12%=3923.28 **数值精确**。算术正确 |
| **PayrollCalculator.calculate 编排正确性** | ✅ PASS | `calculate:54-159` 按 payroll.md §十一 顺序：出勤比例→基本工资→津贴→加班→应发→社保个人→公积金个人→个税（专项扣除=社保个人+公积金个人）→实发。无薪假扣减 config-gated（`:77-82`）。无考勤视为全勤（`:69-71`）避免新员工 0 工资。全 BigDecimal + HALF_UP + salaryRoundingScale 精度 |
| **calculateSalary/runPayroll 事务边界与部分失败** | ⚠️ 部分 | `ErpHrSalaryBizModel.runPayroll:80-94` 在单一 @BizMutation 事务内逐员工循环 calculate→saveEntity，单员工 calculate 抛异常（如 P1-MA4-016 NPE）→**整批回滚**（@BizMutation 事务回滚），无 per-employee 子事务/跳过隔离。existsNonVoidSalary 幂等跳过（`:86-88`）。runPayroll 无错误隔离——与 assets executeBatchDepreciation 的 try/catch 跳过不同（hr 整批 fail-fast）。归 P1-MA4-016 影响（高档员工致整批失败）+ P2-MA4-008 顺手 |
| **SalarySimulationEngine What-If 模拟算术** | ✅ PASS | `PayrollCalculator.recalculateWithOverrides:180-202` 克隆源 ErpHrSalary 快照（`base.cloneInstance()`）+ 清主键（`orm_propValue(1,null)` 避免误用为已存实体）+ overrides 覆盖 + recalculateDerived 重算 gross/tax/net。社保/公积金沿用源期间值（master 驱动，design decision 见 javadoc `:162-168`）。不污染源快照 ✓。convertToFormal per-employee 冲突 skip + all-conflict throw 双层容错（`ErpHrSalarySimulationBizModel:461-525`） |
| **generateBankFile 银行文件生成幂等** | ⚠️ 部分 | `ErpHrSalaryBizModel.generateBankFile:135-178` 选 PENDING+APPROVED 薪酬 → 批量置 PAID + 生成 bankFile。**无幂等键守卫**——重复调 generateBankFile 对同期会因 findPayableSalaries 已无 PENDING 返回空抛 `ERR_NO_APPROVED_SALARY_FOR_BANK_FILE`（间接幂等），但**已 PAID 薪酬的 bankFileId 回写循环（`:173-176`）与 PAID 置位（`:155-158`）在 markPaid 已 PAID 后再次 generateBankFile 不会重复置 PAID**（findPayableSalaries 过滤 PENDING）。间接幂等成立，非显式幂等键。归 P2 watch-only |
| **SalaryPostingDispatcher tryPostPayment/tryPostAccrual 吞异常悬挂（P1-MA2-048 复核）** | ❌ 维持+新发现 | `SalaryPostingDispatcher.tryPostPayment:66-79` `catch(Exception){ LOG.warn/error; return false }` 吞咽过账失败。`ErpHrSalaryBizModel.markPaid:112` **忽略返回值无条件置 PAID**——复核「如 P1-MA2-048 登记」。**新发现 P1-MA4-017**：`tryPostAccrual:46-60` **死代码零调用方**（grep 全 hr 模块 + xbiz 零引用），`SalaryPostingProvider` 声明支持 SALARY(270)/SOCIAL_INSURANCE_ER(290)/HOUSING_FUND_ER(300) 但**三类 PostingEvent 永不生成**——仅 tryPostPayment(SALARY_PAYMENT 280) 在 markPaid 触发。payroll.md §6/§9.1 声明 approveStatus→APPROVED 联动计提+公司承担过账**完全未落地**。「**发现新代码层缺陷**」（P1-MA4-017） |
| **公司承担社保/公积金金额持久化与过账** | ❌ FAIL | `PayrollCalculator.calculate:107-115` 计算 socialInsuranceER/housingFundER（公司承担）为**局部变量**，注释 `:156-157` 声称"暂存 remark"但**代码无 setRemark 调用**——公司承担金额计算后丢弃。无 SOCIAL_INSURANCE_ER/HOUSING_FUND_ER PostingEvent 生成。GL 永远不收公司承担社保/公积金凭证。详 P1-MA4-017 |
| **expireOverdueContracts 批量过期** | ✅ PASS | `ErpHrEmploymentContractBizModel.expireOverdueContracts:73-90` 逐合同 try/catch 单失败 LOG.warn 跳过不影响他合同（与 assets executeBatchDepreciation 同型逻辑隔离）。返回已过期列表 |

**裁决**：社保基数钳制 + 公积金回退 + 累计预扣公式结构 + 模拟隔离 + 批量过期五面算术正确；**核心缺陷**：(1) P1-MA4-016 个税高档 NPE（直接影响实发工资）；(2) P1-MA4-017 业财过账链路不完整（计提+公司承担永不生成）；(3) P1-MA2-048 吞异常悬挂复核「如登记」。

### 领域 3：类型和契约质量（裁决：**PASS（BigDecimal 全域货币类型安全，一处 P3 集中点）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 薪酬金额 BigDecimal 类型安全（gross/net/deduction/税额） | ✅ PASS | PayrollCalculator/IncomeTaxCalculator/SocialInsuranceCalculator 全 BigDecimal；grossSalary/basicSalary/socialInsurance/housingFund/taxAmount/netSalary/positionAllowance/... 全 BigDecimal。无 double/float 货币类型。HALF_UP + salaryRoundingScale（默认 2 位）精度统一 |
| 个税累进税率表数据结构 | ✅ PASS | `TaxBracket`（rangeUpperLimit/rate/quickDeduction BigDecimal 三元组）+ `TaxBracketParser.parse` JSON 数组解析 + 升序排序（null 末档排末尾 `:38-42`）。数据结构清晰，可配置化（ErpHrTaxConfig.taxBrackets JSON 字符串）。**但 resolveBracket 对末档 null 处理缺陷**（P1-MA4-016，类型-契约角度：null 上界契约未在 resolveBracket 防御） |
| SalarySimulationEngine 参数返回契约 | ✅ PASS | `recalculateWithOverrides(base, overrides:Map<String,BigDecimal>, targetYear, targetMonth)` 返回内存 ErpHrSalary。overrides key 为 ErpHrSalary 薪酬项目字段名（applyOverride switch 8 字段，未知字段忽略 `:234-236` 为未来扩展点）。返回内存对象不持久化（BizModel 层决定落库）契约清晰 |
| 37 BizModel 状态迁移参数一致性（P1-MA2-044 工时单硬编码字符串扩散复核） | ⚠️ 维持 | `ErpHrTimesheetBizModel.submit:38,43` 硬编码 `"DRAFT"`/`"SUBMITTED"` vs ErpHrConstants（**复核「如 P1-MA2-044 登记」**）。**未扩散到其他 BizModel**——Employee/Contract/Recruitment/LeaveRequest/Salary/SalarySimulation/ShiftSwap/DevelopmentPlan 全部经 ErpHrConstants 常量。工时单为孤例，归 P2-MA4-008 顺手 |
| SocialInsuranceCalculator 返回 BigDecimal[]（个人+公司）契约 | ⚠️ P3 | `calculate`/`calculateHousingFund` 返回 `BigDecimal[]{个人, 公司}` 位置契约——可读性弱（[0]/[1] 无语义名）。功能正确。归 P2-MA4-008 顺手（可改 VO 或 record） |

**裁决**：BigDecimal 货币类型安全扎实；税率表数据结构清晰可配置化；**唯一类型-契约缺陷是 resolveBracket 对末档 null 上界未防御**（P1-MA4-016）。工时单硬编码字符串 P1-MA2-044 复核维持未扩散。

### 领域 4：错误处理和操作安全（裁决：**FAIL——parseCumulativeData 静默吞异常致少预扣个税（P1-MA4-018）+ P1-MA2-048 吞异常悬挂复核**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| hr 代码异常全部扩展 NopException + ErrorCode（`erp.err.hr.*`） | ✅ PASS | `ErpHrErrors` 全 `erp.err.hr.*` 前缀 + 中文描述 + ARG_* 作用域参数键（56 ErrorCode）。PayrollCalculator/SocialInsuranceCalculator/IncomeTaxCalculator/BizModel 全业务异常 `throw new NopException(ErpHrErrors.ERR_*)`。社保基数/配置/税率/合同缺失 + 状态非法迁移 + 余额不足/日期重叠/重复打卡 ErrorCode 齐全 |
| **parseCumulativeData JSON 解析异常处理** | ❌ FAIL | `IncomeTaxCalculator.parseCumulativeData:158-176` `catch(Exception ignored){}` **静默吞** JSON 解析异常返回空 map → 累计数据损坏时 findPreviousCumulative 返回空 → 当月个税按"无累计历史"计算（累计应纳税所得额仅当月）→ **少预扣个税**（直接影响实发/税务合规）。详 P1-MA4-018。同型静默吞扩散到测试 `TestErpHrPayrollEngine.extractCumulativeData:347-355`（测试也静默吞，致缺陷对测试不可见） |
| 薪酬算术溢出/过账失败错误传播 | ⚠️ 部分 | 配置缺失（社保基数/配置/税率/合同/科目）硬前置抛 NopException ✓。**过账失败** SalaryPostingDispatcher 吞咽返回 false（设计容错，非抛 ErrorCode）——但 markPaid 忽略返回值（P1-MA2-048）。**算术溢出** BigDecimal 无溢出风险。过账悬挂闭环缺陷归 P1-MA4-017/P1-MA2-048 |
| 日期重叠/重复打卡/余额不足错误传播 | ✅ PASS | `ERR_LEAVE_DATE_OVERLAP`/`ERR_ALREADY_CLOCKED_IN`/`ERR_NOT_CLOCKED_IN`/`ERR_LEAVE_BALANCE_INSUFFICIENT`/`ERR_SALARY_ILLEGAL_STATUS_TRANSITION`/`ERR_SALARY_ALREADY_EXISTS` ErrorCode 完整 + ARG_* 作用域参数。LeaveRequest.checkDateOverlap/Attendance.clockIn/Salary.assertNotDuplicated 守卫齐全 |
| 批量过期/批量计提部分失败告警闭环 | ⚠️ 部分 | `expireOverdueContracts:85-87` 单失败 LOG.warn 记录 contractId+reason，**无 IErpSysNotificationBiz 派发**（同 assets executeBatchDepreciation）。归 P1-MA4-017 同型（过账/失败无告警入口） |

**裁决**：异常规范化扎实（全 NopException + erp.err.hr.* + 作用域参数键）；**核心缺陷**：parseCumulativeData 静默吞 JSON 异常致少预扣个税（P1-MA4-018，错误处理致静默算术错误）。过账悬挂闭环归 P1-MA4-017/P1-MA2-048。

### 领域 5：测试有效性（裁决：**FAIL——异常路径系统性零覆盖 + 个税高档边界未测（P1-MA4-019），测试/mutation 比 0.16 全域最低）**

> hr 15 测试。本审计抽样薪酬/过账/模拟核心测试（TestErpHrPayrollEngine 6 方法 / TestErpHrPayrollSimulation / TestErpHrSalaryWorkflowApproval）。

| 测试类 | 方法数 | 覆盖 | 断言强度 |
|--------|--------|------|---------|
| `TestErpHrPayrollEngine` | 6 | 社保钳制+gross/net / 累计税跨月 / runPayroll 幂等 / 审批状态机+PAID 锁 / 非法迁移 / 银行文件 | **强**——社保/公积金**数值精确断言**（2615.52/3923.28）+ net=应发−扣款恒等式断言；状态机断言 status；幂等断言 ErrorCode |
| `TestErpHrPayrollSimulation` | ~12 | createSimulation/adjustItem/批量/异常/anomaly/审批/convertToFormal/冲突 | 中——主路径 + 冲突 skip |
| `TestErpHrSalaryWorkflowApproval` | - | 审批工作流 | 中 |

**测试空洞（P1-MA4-019）**：

1. **个税高档税率边界（>960000 累计）零覆盖**——`resolveBracket` 末档 `rangeUpperLimit:null` NPE（P1-MA4-016）对测试完全不可见。TestErpHrPayrollEngine seedTaxConfig 写入 7 档含末档 null（`:277-285`），但所有测试员工月薪 ≤ 30000（年累计 ≤ 360000，最高触达第 2-3 档），**永不触达末档 null 分支**。高档收入（月薪 > 80000 或高额奖金月）个税计算路径零覆盖
2. **过账悬挂零覆盖**——markPaid 忽略 tryPostPayment 返回值致 posted=false 窗口（P1-MA2-048），无 mock post 抛异常→断言 PAID+无凭证测试。现有 markPaid 测试（testApprovalStateMachineAndPaidLock）全为过账成功路径
3. **累计 JSON 解析失败静默吞零覆盖**——parseCumulativeData `catch(Exception ignored)` 静默重置（P1-MA4-018），无损坏 cumulativeData→断言少预扣个税测试
4. **公司承担社保/公积金过账缺失零覆盖**——P1-MA4-017 三类 PostingEvent 永不生成，无断言"计提凭证/公司承担凭证未生成"的负向测试（也无正向测试因功能未实现）
5. **计提过账（SALARY 270）从未触发零覆盖**——tryPostAccrual 死代码，approve→APPROVED 联动过账未落地，无测试触发 approve 后断言计提凭证生成（因功能未实现）

**裁决**：主路径断言强度扎实（社保/公积金数值精确 + net 恒等式 + 状态机 + 幂等 ErrorCode），但**异常路径系统性零覆盖 + 个税高档边界未测**——hr 测试/mutation 比 0.16 全域最低，异常路径覆盖是重点（P1-MA4-019，目标 MR2）。与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A4.3 P1-MA4-014 互补不重叠。

### 领域 6：可维护性和未来变更风险（裁决：**PASS（P2 watch-only 重复模式 + CRUD 桩治理）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| Survey/PayrollBankFile 18 行 CRUD 桩（P1-MA2-041/045 治理状态复核） | ⚠️ 维持 | `ErpHrSurveyBizModel`/`ErpHrPayrollBankFileBizModel` 各 18 行纯 CrudBizModel 继承无扩展。**复核「如 P1-MA2-041/045 登记」**——状态机缺失已登记（调查 OPEN/CLOSED/ARCHIVED + 银行文件 UPLOADED/CONFIRMED 死状态），本审复核桩的治理状态：桩仍为桩，MR1 补状态机时一并充实。归 P2-MA4-008 |
| 个税累进税率表配置化可扩展性 | ✅ PASS | 税率表经 ErpHrTaxConfig.taxBrackets JSON 字符串配置（非硬编码），TaxBracketParser 解析。年度可差异化（findTaxConfig by year）。可扩展性良好——税改只需更新配置数据 |
| IncomeTaxCalculator.findPreviousCumulative 死代码首循环 | ⚠️ P2 | `findPreviousCumulative:132-139` 首循环 parse cd 但**从未使用**（仅注释说明），实际逻辑在第二循环 `:140-153`（找最大 month 快照）。冗余死代码，可读性干扰。归 P2-MA4-008 顺手删除 |
| applyOverride/readSalaryField 重复 switch-case | ⚠️ P2 | `PayrollCalculator.applyOverride:204-238` + `ErpHrSalarySimulationBizModel.readSalaryField:700-724` 两处 8 字段 switch-case 几乎逐字相同。提取候选：公共 ErpHrSalaryFields helper。归 P2-MA4-008 |
| loadEmployeeJobGrades/loadEmployeeDepartments/loadDepartmentNames 重复 dao-for 模式 | ⚠️ P2 | `ErpHrSalarySimulationBizModel` 三 helper 重复 `daoFor(ErpHrEmployee).findAllByQuery + Map 构建`模式。提取候选。归 P2-MA4-008 |
| 92 mutation（全域第二高）+ 测试比 0.16 全域最低测试债务 | ⚠️ P2 | 测试债务风险——mutation 绝对数高（finance 137 > hr 92 > mfg 74）+ 测试比最低（hr 0.16 < assets 0.23 < mfg 0.41 < finance 0.47）。归 P1-MA4-019（测试有效性）+ A5.3（测试覆盖深度统计） |

**裁决**：个税税率表配置化可扩展性良好；**主要可维护性风险是 CRUD 桩治理 + 死代码 + 重复模式 + 测试债务**（P2-MA4-008 watch-only）。工时单硬编码字符串 P1-MA2-044 复核维持（孤例未扩散）。

### 领域 7：自动化和防护覆盖（裁决：**FAIL——无 hr 薪酬算术回归门控（P2-MA4-009）**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| compliance checker 规则守护跨域 daoFor | ✅ PASS | hr 薪酬/过账/模拟链路**零跨域 `daoFor(ErpFin*)`/`daoFor(ErpMd*)`**（领域 1 已确认）。compliance checker R2d 跨域 daoFor 检查对 hr 无漏检站点（hr 计算引擎同域只读 hr 实体）。**hr 边界本审计 PASS，无需 R2d 扩展** |
| R8 Processor 无 xbiz 规则 | ✅ PASS | hr 无 Processor 链路（薪酬经 BizModel + Dispatcher + Facade，非 Processor-extension-pattern）。R8 不适用 |
| 薪酬算术回归测试门控 | ❌ FAIL | TestErpHrPayrollEngine 覆盖社保钳制/gross-net 数值断言 ✓。**但个税高档边界（P1-MA4-016 NPE）+ 累计 JSON 解析失败（P1-MA4-018）无回归门控**——高档收入算术路径对 CI 不可见。归 P1-MA4-019 补测试后形成门控 |
| 过账回归门控 | ❌ FAIL | markPaid 过账成功路径有测试 ✓。**但过账悬挂（P1-MA2-048）+ 计提/公司承担缺失（P1-MA4-017）无门控**——无 mock post 抛异常→断言 posted=false 测试 + 无负向断言"计提凭证未生成"。归 P1-MA4-017/019 |
| 并发回归门控 | ⚠️ 部分 | hr 10 个状态机实体全部声明 versionProp（A2.17 已确认透明乐观锁降级）。无薪酬核算并发测试（runPayroll 并发 + markPaid 并发）。归 P2-MA4-009 |

**裁决**：hr 边界本审计 PASS（零跨域 daoFor，compliance checker 无漏检）；**核心防护缺口是无薪酬算术/过账回归门控**（P2-MA4-009 watch-only，归 P1-MA4-019 补测试后形成 CI 门控）。薪酬算术直接影响实发工资，防护优先级高。

---

## 3. MA1/MA2/MA3 已知 finding 运行时复核

> 每项标记「如 owner doc 声明」（无新代码层缺陷）或「发现新代码层缺陷」。

| Finding ID | 原描述 | 代码实现质量角度复核 | 终态 |
|-----------|--------|---------------------|------|
| `P1-MA1-022`（todo MR1，9 域合并跨域只读 daoFor） | hr Dashboard facade read-only 聚合永久接受 | hr 薪酬/过账/模拟**主路径**（PayrollCalculator/SocialInsuranceCalculator/IncomeTaxCalculator/SalaryPostingDispatcher/SalarySimulationBizModel）**零跨域 daoFor**（全同域 hr 只读）。Dashboard facade read-only 永久接受。**「如登记」**，本审计无 hr 薪酬链路新投影（hr 计算引擎不读 ErpFin/ErpMd，与 assets/mfg 不同） | 不升级（维持 P1，hr 薪酬链路无投影） |
| `P1-MA2-039`（todo MR1，员工 RESIGNED/TERMINATED/RETIRED 三态死状态 + 联动未实现） | 员工在职状态机迁移缺失 | **代码实现质量角度「如登记」**——状态机业务正确性归 A2.7a。ErpHrEmployeeBizModel.transferEmployee 守卫 isTransferable（ACTIVE/PROBATION）正确，算术/事务/类型无新缺陷。runPayroll findActiveEmployees 限 ACTIVE/PROBATION 等价"非在职停发"语义 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-040`（todo MR1，合同 SUSPENDED 死状态） | 合同 SUSPENDED dict 死状态 | **「如登记」**——ErpHrEmploymentContractBizModel 仅 renew/expireOverdue，无 SUSPEND 动作。代码实现质量角度非算术/事务缺陷 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-041`（todo MR1，调查三态死状态 + SurveyBizModel 18 行桩） | 调查 OPEN/CLOSED/ARCHIVED 死状态 + CRUD 桩 | **「如登记」**——ErpHrSurveyBizModel 仍 18 行桩（领域 6 复核）。桩治理状态待 MR1 状态机补齐时充实 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-042`（todo MR1，发展计划 DRAFT/CANCELLED + 计划项 OVERDUE 死状态） | 发展计划死状态 + 无 cancelPlan + 无 OVERDUE job | **「如登记」**——ErpHrDevelopmentPlanBizModel 状态机守卫（isValidPlanItemTransition）正确，generateDevelopmentPlan 排序（severityRank + gapValue）正确。死状态为业务正确性归 A2.7a | 不升级（维持 P1 待 MR1） |
| `P1-MA2-043`（todo MR1，工时单 APPROVED/REJECTED 死状态 + 仅 submit） | ErpHrTimesheetBizModel 仅 submit | **「如登记」**——submit 守卫 "DRAFT" 硬编码（P1-MA2-044）+ 仅 submit。代码实现质量角度非算术/事务缺陷 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-044`（todo MR1，工时单硬编码字符串 vs ErpHrConstants） | TimesheetBizModel 硬编码 "DRAFT"/"SUBMITTED" | **本审计复核确认**：`ErpHrTimesheetBizModel.submit:38,43` 硬编码字符串。**「如登记」**——**未扩散到其他 BizModel**（领域 3 确认，工时单为孤例）。归 P2-MA4-008 顺手 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-045`（todo MR1，银行文件 UPLOADED/CONFIRMED 死状态 + CRUD 桩） | PayrollBankFileBizModel 18 行桩 | **「如登记」**——ErpHrPayrollBankFileBizModel 仍 18 行桩（领域 6 复核）。generateBankFile 在 ErpHrSalaryBizModel 生成 bankFile 置 GENERATED（非 PayrollBankFileBizModel），桩治理待 MR1 | 不升级（维持 P1 待 MR1） |
| `P1-MA2-046`（todo MR1，排班分配 status 无 dict 绑定 raw VARCHAR） | ShiftAssignment.status raw VARCHAR | **「如登记」**——ErpHrShiftSwapRequestBizModel.approve 经 ErpHrConstants.ASSIGNMENT_STATUS_* 常量写（领域已确认）。状态机业务正确性归 A2.7b | 不升级（维持 P1 待 MR1） |
| `P1-MA2-047`（todo MR1，SalaryPostingDispatcher javadoc drift + ErpHrSalary.posted 死字段） | javadoc "无 posted 字段" + posted 死字段从未写入 | **本审计复核发现新代码层缺陷**：P1-MA2-047 仅标 javadoc drift + posted 死字段，**未发现整条计提+公司承担过账链路未接线**——tryPostAccrual 死代码零调用方 + socialInsuranceER/housingFundER 丢弃 + SALARY/SOCIAL_INSURANCE_ER/HOUSING_FUND_ER 三类 PostingEvent 永不生成（P1-MA4-017）。「**发现新代码层缺陷**」（P1-MA4-017） | 部分升级（P1-MA2-047 维持 + 新增 P1-MA4-017 过账链路不完整） |
| `P1-MA2-048`（todo MR1，工资过账 tryPostPayment/tryPostAccrual 吞异常致 posted=false 悬挂） | markPaid 吞异常致 posted=false 悬挂无告警闭环 | **本审计复核确认**：`SalaryPostingDispatcher.tryPostPayment:66-79` 吞咽返回 false + `ErpHrSalaryBizModel.markPaid:112` 忽略返回值无条件置 PAID。**「如登记」**——P1-MA2-048 原描述含 tryPostAccrual，本审确认 tryPostAccrual 不仅吞异常而是**完全死代码**（升级为 P1-MA4-017 过账链路不完整）。posted=false 悬挂本身维持 P1-MA2-048 | 部分升级（P1-MA2-048 维持 + tryPostAccrual 死代码升级 P1-MA4-017） |
| `P2-MA1-020`（todo MR1，hr owner doc drift） | salary-approval-status orphan dict | **「如登记」**——owner doc drift，非代码缺陷。归 A3 | 不升级（维持 P2） |
| `P1-MA3-009/010`（todo MR2，8 扩展域 owner doc drift） | hr 在 8 扩展域"全局视图"系统性缺位 | **「如登记」**——owner doc drift，非薪酬/过账/模拟代码缺陷。归 A3 | 不升级（维持 P1 待 MR2） |

**裁决**：12 项已知 finding 运行时复核 **11 项「如登记」无升级**；**1 项复核发现新代码层缺陷**——P1-MA2-047/048 复核时发现**整条计提+公司承担过账链路未接线**（P1-MA4-017，MA2 仅标 javadoc drift + 吞异常悬挂，未发现 tryPostAccrual 零调用方 + ER 金额丢弃 + 三类 PostingEvent 永不生成）。

---

## 4. P0-P3 finding 清单（按严重性排序）

### 4.1 P1 finding（4 项）

| Finding ID | 域 | 描述 | 严重性 | 影响 | 修复方式 | 目标 MR |
|-----------|-----|------|-------|------|---------|---------|
| `P1-MA4-016` | hr | **个税高档税率 NPE（薪酬算术缺陷，直接影响实发工资）**：`IncomeTaxCalculator.resolveBracket:204-214` 遍历七级累进税率表，逻辑为"跳过 income > rangeUpperLimit 的档位，选第一档 income ≤ rangeUpperLimit"。末档（>960000，45%，rate 0.45/quickDeduction 181920）`rangeUpperLimit=null`（seed `erp_hr_tax_config` 实证 + TestErpHrPayrollEngine.seedTaxConfig:284 写入 `rangeUpperLimit:null` + TaxBracketParser 注释声明末档 null 表"无上限"）。当累计应纳税所得额 > 960000（月薪 > ~80000 或高额奖金月的高收入员工），前 6 档全部 `income > upperLimit` 跳过，触达末档时 `cumulativeTaxableIncome.compareTo(b.getRangeUpperLimit())` = `compareTo(null)` → **NullPointerException**。`calculate()` 抛 NPE → `ErpHrSalaryBizModel.calculateSalary/runPayroll` 抛 NPE → @BizMutation 事务回滚。runPayroll 为整批循环无 per-employee 隔离，**任一高收入员工致整批回滚（全员工当月无薪酬）**；calculateSalary 单员工直接失败。薪酬算术直接影响实发工资（个税）的核心路径缺陷。 | major（**响亮崩溃**非静默错算——NPE 立即抛出无数据持久化，detectable；非 P0 详见 §5.2） | MR1——`resolveBracket` 末档 null 防御：`if(b.getRangeUpperLimit()==null){ selected=b; break; }`（在 compareTo 前），或将末档 rangeUpperLimit 设为 BigDecimal.MAX/极大数。触及薪酬保护区域（直接影响实发工资），修复须独立 plan-audit + 人工确认 + 补高档边界测试（P1-MA4-019） | MR1 |
| `P1-MA4-017` | hr | **业财过账链路不完整（计提+公司承担社保/公积金 PostingEvent 永不生成）**：(a) `SalaryPostingDispatcher.tryPostAccrual:46-60`（计提 SALARY 270：借 管理费用-工资 / 贷 应付职工薪酬）**死代码零调用方**——grep 全 hr 模块 + xbiz + beans.xml 零引用，仅 tryPostPayment(SALARY_PAYMENT 280) 在 `ErpHrSalaryBizModel.markPaid:112` 触发；(b) `SalaryPostingProvider.getSupportedBusinessTypes:53-60` 声明支持 SALARY/SALARY_PAYMENT/**SOCIAL_INSURANCE_ER(290)**/**HOUSING_FUND_ER(300)** 四类，`createFacts:91-102` 实现 SOCIAL_INSURANCE_ER/HOUSING_FUND_ER 分支，但**这三类 PostingEvent 永不生成**——无任何代码组装 SOCIAL_INSURANCE_ER/HOUSING_FUND_ER event；(c) `PayrollCalculator.calculate:107-115` 计算 socialInsuranceER/housingFundER（公司承担）为**局部变量**，注释 `:156-157` 声称"暂存 remark"但**无 setRemark 调用**——公司承担金额计算后丢弃。**owner doc 声明**：payroll.md §6 表（line 431-433）+ §9.1（line 533-536）声明 approveStatus→APPROVED 联动计提 SALARY(270) + SOCIAL_INSURANCE_ER(290) + HOUSING_FUND_ER(300)，"由 approve action 的 xbiz source append 触发"——**该 xbiz append 未落地**（hr 模块零 xbiz 文件）。后果：GL **永远仅收发放凭证 SALARY_PAYMENT(280)**，**缺工资计提（管理费用-工资/应付职工薪酬）+ 公司承担社保（管理费用-社保/应付职工薪酬-社保）+ 公司承担公积金（管理费用-公积金/应付职工薪酬-公积金）** → 严重业财不一致（费用低估 + 应付职工薪酬低估 + 资产负债表失衡），直至期末试算平衡人工发现。MA2 P1-MA2-047 仅标 javadoc drift + posted 死字段，**未发现整条计提+公司承担链路未接线**（本审新发现）。 | major（业财不一致——漏记凭证，可经试算平衡发现；员工实发工资正确[公司承担不影响个人 net]；非 P0 详见 §5.2） | MR1 裁决——方案 A（推荐）实现 approve→APPROVED 联动：(1) 接线 tryPostAccrual 在 approve action 触发（xbiz source append 或 BizModel 覆盖 approve）+ (2) PayrollCalculator 持久化 socialInsuranceER/housingFundER（新增字段或 remark）+ (3) 新增 tryPostSocialInsuranceER/tryPostHousingFundER 组装 290/300 event；方案 B 文档化为已知简化 + owner doc payroll.md §6/§9.1 标注"当前仅发放过账，计提+公司承担 successor"。触及会计保护区域，修复须独立 plan-audit + 人工确认。与 P1-MA2-047/048 协同 | MR1 |
| `P1-MA4-018` | hr | **个税累计数据健壮性（parseCumulativeData 静默吞异常致少预扣个税）**：`IncomeTaxCalculator.parseCumulativeData:158-176` 解析 cumulativeData JSON 时 `catch(Exception ignored){ return new HashMap(); }` **静默吞**所有解析异常返回空 map。当 cumulativeData 损坏（JSON 格式错误 / 字段类型不符 / 手工编辑出错），`findPreviousCumulative:140-153` 返回空 → 当月 `calculate:72-96` 按"无累计历史"计算：cumGross 仅当月 gross、cumThreshold 仅当月 5000、cumSpecial 仅当月 → 累计应纳税所得额远低于实际（年度累计被重置为单月）→ **累计税额低估 → 当月应纳个税低估（monthTax 偏小）→ 少预扣个税**（直接影响员工实发工资偏高 + 企业代扣代缴不足致税务合规风险）。同型静默吞模式扩散到测试 `TestErpHrPayrollEngine.extractCumulativeData:347-355`（测试也 `catch(Exception ignored)`，致缺陷对测试不可见——P1-MA4-019）。**附带**：`findPreviousCumulative:132-139` 死代码首循环（parse cd 但从未使用，仅注释），归 P2-MA4-008。触发条件：需 cumulativeData 损坏（happy path 由同 calculator 写入合法 JSON，不触发）；但一旦上游写入异常/数据迁移/手工修改即静默错算。 | major（静默算术错误——少预扣个税直接影响实发/税务合规；触发需损坏 JSON 前置但无告警/日志） | MR1——`parseCumulativeData` 移除静默吞：解析失败时 LOG.warn + 抛 NopException(ERR_HR_CUMULATIVE_DATA_CORRUPT 新增 ErrorCode，含 employeeId+year+month 作用域) 或保守降级为 LOG.warn + 返回空（但须可观测）。同步修复测试 extractCumulativeData 静默吞。触及薪酬保护区域，修复须独立 plan-audit | MR1 |
| `P1-MA4-019` | hr | **hr 薪酬/过账链路测试有效性系统性不足（测试/mutation 比 0.16 全域最低，异常路径零覆盖）**：(a) **个税高档税率边界（>960000 累计）零覆盖**——resolveBracket 末档 null NPE（P1-MA4-016）对测试不可见，TestErpHrPayrollEngine 所有员工月薪 ≤ 30000 永不触达末档；(b) **过账悬挂零覆盖**——markPaid 忽略 tryPostPayment 返回值致 posted=false 窗口（P1-MA2-048），无 mock post 抛异常→断言 PAID+无凭证测试；(c) **累计 JSON 解析失败静默吞零覆盖**——parseCumulativeData 静默重置（P1-MA4-018），无损坏 cumulativeData→断言少预扣个税测试；(d) **公司承担社保/公积金过账缺失零覆盖**——P1-MA4-017 三类 PostingEvent 永不生成，无负向断言"计提/公司承担凭证未生成"；(e) **计提过账（SALARY 270）从未触发零覆盖**——tryPostAccrual 死代码，无 approve→断言计提凭证生成测试。hr 92 mutation（全域第二高）× 15 测试 = 比 0.16 全域最低，异常路径覆盖是重点。 | major（测试空洞致 P1-MA4-016/017/018 + P1-MA2-048 回归无防护 + 薪酬算术边界 bug 对测试不可见——直接影响实发工资） | MR2 补——(1) 个税高档边界测试（月薪 100000 员工累计 > 960000→断言末档 45% 正确计算非 NPE，闭合 P1-MA4-016）；(2) 过账悬挂测试（mock post 抛异常→断言 posted=false/PAID+无凭证，闭合 P1-MA2-048 测试可见性）；(3) 累计 JSON 损坏测试（seed 损坏 cumulativeData→断言抛 ErrorCode 或 LOG.warn 可观测，闭合 P1-MA4-018）；(4) 公司承担过账负向测试（approve→断言计提+290+300 凭证生成或显式标注 successor，闭合 P1-MA4-017 测试可见性）。与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A4.3 P1-MA4-014 + A5.3 互补不重叠 | MR2 |

### 4.2 P2 finding（2 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA4-008` | **可维护性热点合并 6 项**：(a) **Survey/PayrollBankFile 18 行 CRUD 桩治理**——P1-MA2-041/045 已登记状态机缺失，本审复核桩仍为桩（MR1 补状态机时一并充实）；(b) **TimesheetBizModel 硬编码 "DRAFT"/"SUBMITTED"**——P1-MA2-044 复核确认未扩散到其他 BizModel（孤例），MR1 顺手改 ErpHrConstants；(c) **IncomeTaxCalculator.findPreviousCumulative:132-139 死代码首循环**——parse cd 但从未使用（仅注释），实际逻辑在第二循环，删除首循环；(d) **applyOverride/readSalaryField 重复 switch-case**——PayrollCalculator.applyOverride:204-238 + ErpHrSalarySimulationBizModel.readSalaryField:700-724 两处 8 字段 switch-case 几乎逐字相同，提取 ErpHrSalaryFields helper；(e) **loadEmployeeJobGrades/loadEmployeeDepartments/loadDepartmentNames 重复 dao-for 模式**——三 helper 重复 employee 查询+Map 构建，提取候选；(f) **SocialInsuranceCalculator 返回 BigDecimal[] 位置契约**——[0]/[1] 无语义名，可改 VO/record。 | watch-only，MR2 顺手——方案 A（推荐）(c) 删死代码 + (d) 提取公共 helper + (f) 改 VO；方案 B 接受现状 |
| `P2-MA4-009` | **自动化防护缺口 2 项**：(a) **无 hr 薪酬算术回归门控**——个税高档边界（P1-MA4-016 NPE）+ 累计 JSON 解析失败（P1-MA4-018）+ 社保钳制边界无 CI 门控（归 P1-MA4-019 补测试后形成门控）；(b) **无过账回归门控**——过账悬挂（P1-MA2-048）+ 计提/公司承担缺失（P1-MA4-017）无门控（归 P1-MA4-017/019 补测试后形成门控）。**注**：hr 边界本审计 PASS（零跨域 daoFor），compliance checker R2d 对 hr 无漏检站点，无需扩展。薪酬算术直接影响实发工资，防护优先级高。 | watch-only，MR2 顺手——P1-MA4-019 测试补齐后形成 CI 门控 |

### 4.3 P3 finding

- `PayrollCalculator.DEFAULT_OVERTIME_HOURLY_RATE=50` / `DEFAULT_REQUIRED_WORK_DAYS=22` 硬编码常量（加班费/月标准工作日，payroll.md §5.2 注"加班费由合同/政策决定"——当前硬编码回退）。可接受（合同未配置时回退），即时风险低，不单独登记。
- `PayrollCalculator.DEFAULT_OVERTIME_HOURLY_RATE` 注释声明"合同/政策决定"但代码未读合同字段（合同无加班费率字段）——配置化候选，归 P2-MA4-008。

---

## 5. 综合裁决

### 5.1 Verdict

**⚠️(P1)**——hr 薪酬/过账/模拟链路代码实现质量**核心扎实**（社保基数钳制算术 + 累计预扣结构 + BigDecimal 货币安全 + 模拟隔离 + 跨域 Facade + 异常规范化 + 边界零违规七面），但**个税高档 NPE（P1-MA4-016，直接影响实发工资）+ 业财过账链路不完整（P1-MA4-017，计提+公司承担永不生成）+ 个税累计健壮性（P1-MA4-018，静默吞致少预扣）+ 测试有效性（P1-MA4-019，异常路径零覆盖）** 四项 P1 缺陷需 MR1/MR2 修复。

### 5.2 P0 评估

**无 P0**——无活跃**静默**数据破坏路径：

- **P1-MA4-016 个税高档 NPE**：**响亮崩溃**非静默错算——NPE 立即抛出、@BizMutation 事务回滚无数据持久化、detectable（首次高收入员工核算即失败）。不会静默多缴/少缴个税（calculate 失败则不发，非发错金额）。不满足 P0"数据丢失/静默破坏"标准，维持 P1。**但直接影响实发工资的高档收入算术缺陷，MR1 高优先级**。
- **P1-MA4-017 业财过账链路不完整**：漏记凭证（计提+公司承担），可经期末试算平衡发现（费用/应付低估）；员工实发工资正确（公司承担不影响个人 net）；非静默数据破坏。维持 P1。
- **P1-MA4-018 parseCumulativeData 静默吞**：需 cumulativeData 损坏前置（happy path 由同 calculator 写合法 JSON），属防御性/健壮性缺陷；一旦触发为静默少预扣，但前置条件限制触发面。维持 P1（非 P0——前置条件 + 间接幂等）。
- **P1-MA2-048 过账悬挂**：需过账引擎异常前置（基础设施故障/科目配置错误，非正常路径）+ LOG.warn 可见性，已登记 deferred。

### 5.3 剩余风险

1. **薪酬算术直接影响实发工资**——P1-MA4-016 高档 NPE（任一高收入员工致整批 runPayroll 失败）+ P1-MA4-018 少预扣个税，虽非 P0 但属高风区域，MR1 优先。
2. **业财过账链路不完整**——P1-MA4-017 GL 缺计提+公司承担凭证，资产负债表/利润表失衡，MR1 优先。
3. **测试债务全域最高**——hr 92 mutation × 15 测试 = 比 0.16 全域最低，P1-MA4-019 异常路径零覆盖，MR2 补测试形成防护。

### 5.4 与 MA2/A4.1a/A4.1b/A4.2a/A4.2b/A4.3 交叉去重

- **P1-MA4-016** 个税高档 NPE 为本审计新发现（MA2 状态机审查未覆盖个税算术边界），独立登记 MR1
- **P1-MA4-017** 业财过账链路不完整与 P1-MA2-047（javadoc drift + posted 死字段）/P1-MA2-048（吞异常悬挂）**不同代码层**——MA2 标 javadoc + 吞异常，本审发现**整条计提+公司承担链路未接线**（tryPostAccrual 死代码 + ER 丢弃 + 三类 event 永不生成），新登记 MR1 协同
- **P1-MA4-018** parseCumulativeData 静默吞为本审计新发现（MA2 未覆盖个税累计健壮性），独立登记 MR1
- **P1-MA4-019** 与 A4.1a P1-MA4-002 + A4.1b P1-MA4-005 + A4.2a P1-MA4-009 + A4.2b P1-MA4-011 + A4.3 P1-MA4-014 + A5.3 互补不重叠（各域测试空洞独立登记）
- **P2-MA4-008/009** 与 A4.1a P2-MA4-001/002 + A4.1b P2-MA4-003 + A4.2a P2-MA4-004 + A4.2b P2-MA4-005 + A4.3 P2-MA4-006/007 同型（可维护性热点 + 自动化防护），独立登记

**hr 域 MA4 代码质量终态在此收口：4 P1 + 2 P2，零 P0。** roadmap A4.4 推进至 done（待独立 closure audit）。
