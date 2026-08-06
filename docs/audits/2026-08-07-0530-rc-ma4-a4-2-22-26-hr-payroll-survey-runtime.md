# A4.2.22-A4.2.26 hr-F3 薪酬/工时/调研域过账触发链与桩功能运行时确认验证报告（rc-ma4-a4-2-22-26）

> Mission: requirement-compliance · MA4 运行时行为验证 · Work Items: A4.2.22 / A4.2.23 / A4.2.24 / A4.2.25 / A4.2.26
> 来源计划: `docs/plans/2026-08-07-0530-3-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`
> 来源存疑点: `docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md` §7（5 项静态存疑点）
> 方法论: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 审计类型: 只读审计（无生产代码 / ORM / api.xml / view.xml / config 默认值 / 真相源变更）
> 审计日期: 2026-08-07
> Audit Status: closed

## 9. 与既有 A1.14 / A2.7b / A4.4 报告的差异增量声明（前置）

本报告是 MA4 运行时行为验证 A4.2 展开器的 A1.14 §7 五项静态存疑点的**运行时证据采集与裁决**，视角 = **A1.14 静态判定结论的运行时确认（维持 P1 或升级触发 MR0）**。按 §去重协议，以下既有审计已证实的结论本报告**直接复用，不重审**：

- **A1.14**（`docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md`）：UC-HR-03/04/10/11 五级追踪 + §7 五项静态存疑点 + §6 finding 衔接裁决（P1-MA4-017 / P1-MA2-041 / P1-MA2-043 reuse 重开 + P1-RC-015 / P1-RC-016 新建）。本报告复用其 L3 代码路径静态判定 + §6 finding 编号，只补**运行时触发链 / 零调用方 / 零 writer / billData 内容 / 测试自述**的运行时证据。
- **A2.7b**（`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`）：工资支付轴 + markPaid 跨域 Facade 行为已证实。本报告复用其 markPaid→`IErpFinVoucherBiz.post()` REQUIRES_NEW Facade 证据。
- **A4.4**（`docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`）：P1-MA4-017（计提+公司承担过账链路未接线）登记 + 链路代码质量七面。本报告复用其 017 静态判定，补运行时触发链证据。

本报告**只补运行时差异**：(i) 计提+公司承担过账触发链运行时确认（§2-1/§2-2，会计正确性类，**业财保护区域探针——只读确认不改过账逻辑**）；(ii) 桩功能/数据校验运行时缺口确认（§2-3/§2-4/§2-5，确认功能不可达且结果表为空）。

---

## 1. 存疑点清单（逐字引用 A1.14 §7）

> 以下为本报告核验对象，5 项静态存疑点来自 A1.14 §7：

1. **UC-HR-04 ⑯ 计提+公司承担过账运行时触发链**（A4.2.22）：approve→APPROVED 时计提 SALARY(270) + 290/300 event 是否生成（HEAD 静态判定 = 永不生成，tryPostAccrual 零调用方）。
2. **UC-HR-04 公司承担金额运行时丢弃确认**（A4.2.23）：`PayrollCalculator:110/:115` socialInsuranceER/housingFundER 计算后是否经 remark/billData 传递到 PostingEvent（HEAD 静态判定 = 丢弃，无 setRemark）。
3. **UC-HR-03 ②24h 校验运行时拦截**（A4.2.24）：同一日多条 TimesheetLine hours 之和 > 24 是否被拦截（HEAD 静态判定 = 无校验）。
4. **UC-HR-11 ㉖匿名 respondentHash 运行时防重复**（A4.2.25）：匿名模式重复提交是否被拦截（HEAD 静态判定 = 无 writer/无校验）。
5. **UC-HR-11 ㉘㉙ CLOSED 自动聚合 + eNPS 运行时计算**（A4.2.26）：CLOSED 时 ErpHrSurveyResult 是否自动聚合 + eNPS 是否计算（HEAD 静态判定 = 无 mutation/无算法）。

---

## 2. 运行时证据采集与裁决（L3 file:line + L5 行为）

### 2-1 A4.2.22 计提+公司承担过账运行时触发链确认 — 维持 P1-MA4-017

**业财保护区域探针声明**：本节为只读触发链追踪（grep census + PostingEvent 构造点追踪 + 调用方追踪），**不修改任何过账逻辑 / VoucherFact / PostingProcessor 核心路径**。

**运行时证据链（live code 实测）**：

- **markPaid→280 发放是唯一活跃过账路径**：`ErpHrSalaryBizModel.markPaid:98` → `ErpHrSalaryMarkPaidProcessor` → `postingDispatcher.tryPostPayment(salary)`（`ErpHrSalaryMarkPaidProcessor.java:36`）→ `buildPaymentEvent:139` 组装 `ErpFinBusinessType.SALARY_PAYMENT`（280）→ `SalaryPostingExecutor` → `IErpFinVoucherBiz.post()` REQUIRES_NEW Facade（A2.7b 已证实）。
- **tryPostAccrual 零生产调用方**：`rg -n "tryPostAccrual" --type java module-hr` 命中全集 = `SalaryPostingDispatcher.java:67`（方法定义）+ javadoc 注释（`:35/:37/:61` 自述「零调用方死代码」）+ 测试注释（`TestErpHrPayrollEngine.java:408/:411/:440` 自述 Deferred）。**无任何 markPaid/approve/runPayroll 路径调用 tryPostAccrual** → 计提 SALARY(270) PostingEvent **永不构造**。
- **approve 路径零过账触发**：`ErpHrSalaryBizModel` approveStatus 轴由平台 `approve` 通用 action 承载（`ErpHrSalaryBizModel.java:42-45` javadoc 自述「审批轴由平台管理」），`ErpHrSalaryBizModel` + 其 Processor **零 `postingDispatcher` 调用**（grep `postingDispatcher` 全 hr main 仅 `ErpHrSalaryMarkPaidProcessor:36` + `ErpHrSalaryBizModel:56` 字段声明，approve 无消费）→ approve→APPROVED **不触发任何过账**。
- **290/300 PostingEvent 永不构造**：`SalaryPostingDispatcher` 全方法集 = `tryPostAccrual:67`（仅组装 270）+ `tryPostPayment:88`（仅组装 280）+ `buildAccrualEvent:127`（SALARY 270）+ `buildPaymentEvent:139`（SALARY_PAYMENT 280）+ `buildEvent:151`（通用）。**无 `tryPostSocialInsuranceER` / `tryPostHousingFundER` 方法**（grep `tryPostSocialInsuranceER|tryPostHousingFundER` 全 hr main 零命中）→ 290/300 event **永不构造**。
- **Provider 消费侧就绪但事件源断裂（关键细化）**：`SalaryPostingProvider.getSupportedBusinessTypes:53-60` 注册 SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER(290)/HOUSING_FUND_ER(300) 四类 + `createFacts:91-102` 实现 290/300 分录（借 管理费用-社保/公积金 / 贷 应付职工薪酬-社保/公积金）；`ErpFinBusinessType` 枚举含 270/280/290/300 全四档（`ErpFinBusinessType.java:40-43`）。**会计消费侧（createFacts）逻辑完整就绪，但因派发侧（Dispatcher）永不构造 290/300 PostingEvent，Provider 的 290/300 分支在生产运行时结构上不可达**（仅当 PostingEvent businessType=290/300 到达时才触发，而该事件永不到达）。此细化**强化**而非削弱 P1-MA4-017 证据：会计逻辑已实现 → MR1 修复面收窄为「派发侧接线 + ER 持久化 + approve 联动」，createFacts 无需重写。
- **测试自述 Deferred**：`TestErpHrPayrollEngine.java:408-440` 注释自述「公积金公司承担(300) 过账链路未实现——tryPostAccrual 为零调用方死代码，290/300 event 永不组装」+ 断言「Deferred：计提 SALARY(270) 凭证未生成（tryPostAccrual 零调用方死代码，approve 未接线——017 successor）」（`:440`）。

**裁决**：A1.14 §7-1 静态判定「永不生成」**运行时确认成立**。GL 在 markPaid 路径**仅收 280 SALARY_PAYMENT 发放凭证**；approve→APPROVED 不触发计提；270/290/300 永不入 GL。**会计错误未活跃**（GL 从未收到错误凭证，而是**缺凭证** → 费用+应付职工薪酬低估 + 资产负债表失衡，可经期末试算平衡人工发现）→ **不触发 MR0**（MR0 触发条件 = 运行时发现会计错误已活跃）。**维持 P1-MA4-017（Q4 会计正确性类 reuse 重开，修复归 MR1 + 触及会计过账核心路径须 ask-first + 独立 plan-audit）**。

### 2-2 A4.2.23 公司承担金额运行时丢弃确认 — 维持 P1-MA4-017

**运行时证据链（live code 实测）**：

- **ER 金额计算后丢弃**：`PayrollCalculator.java:108-110` 计算 `socialInsuranceER = social[1]...` + `:113-115` 计算 `housingFundER = fund[1]...`，二者均为**局部变量**；`calculate` 方法 :130-158 持久化字段集 = basicSalary/positionAllowance/.../socialInsurance(EE)/housingFund(EE)/taxAmount/netSalary/...，**`salary.setSocialInsuranceER` / `salary.setHousingFundER` 调用零命中**（grep 跨全 module-hr 零命中）。
- **注释 :156-157 与实现矛盾**：`:156-157` 注释声称「公司承担部分暂存 remark 用于过账派发器读取（避免扩展实体字段）；正式存档于 PostingEvent.billData 而非持久化」，但 grep `setRemark` 跨 `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/payroll/` **零命中** → **无 setRemark 调用**，注释与实现不符（ER 金额既未暂存 remark，也未存 billData）。
- **ORM 无 ER 列**：grep `socialInsuranceEr|housingFundEr|social_insurance_er|housing_fund_er` 跨 `module-hr/model/app-erp-hr.orm.xml` **零命中** → ErpHrSalary 实体无 ER 持久化列（与 P1-MA4-017 一致）。
- **buildEvent billData 不含 ER**：`SalaryPostingDispatcher.buildEvent:162-172` billData 写入键集 = `BILL_DATA_SALARY_ID` / `BILL_DATA_EMPLOYEE_ID` / `BILL_DATA_DEPARTMENT_ID` / `BILL_DATA_COST_CENTER_ID` / `BILL_DATA_GROSS_AMOUNT` / `BILL_DATA_NET_AMOUNT` / `BILL_DATA_DEBIT_SUBJECT_CODE` / `BILL_DATA_CREDIT_SUBJECT_CODE` / `BILL_DATA_SOURCE_BILL_TYPE`（9 键）。**不含 ER 金额键**。注意 `ErpHrConstants.java:84-85` 定义了 `BILL_DATA_SOCIAL_INSURANCE_ER` / `BILL_DATA_HOUSING_FUND_ER` 常量，但 **buildEvent 从不引用这两个常量**（死常量）→ billData 结构性不含 ER 金额。

**裁决**：A1.14 §7-2 静态判定「丢弃」**运行时确认成立**。公司承担 socialInsuranceER/housingFundER 计算后**静默丢弃**（无 ORM 列 + 无 setRemark + billData 不含 ER 键）。**维持 P1-MA4-017**（ER 持久化 + billData 传递为 MR1 修复的组成部分，触及 ORM 结构变更须 ask-first）。

### 2-3 A4.2.24 24h 校验运行时拦截确认 — 维持 P1-RC-015

**运行时证据链（live code 实测）**：

- **submit 路径无 24h 校验**：`ErpHrTimesheetBizModel.java:35-47` `submit` 仅做 DRAFT→SUBMITTED 状态守卫（`:39-43` reject 非 DRAFT）+ `setStatus(SUBMITTED)`，**无 hours 聚合 / 无 24h 上限校验**。
- **TimesheetLine 为 18 行 CRUD 桩**：`ErpHrTimesheetLineBizModel.java` 全文 18 行，`extends CrudBizModel<ErpHrTimesheetLine>` 零业务方法 → TimesheetLine 新增/编辑经平台默认 save 路径，**无聚合校验 mutation**。
- **grep census 零业务命中**：`rg -n "MAX_HOURS|maxHours|totalHours.*24|hours.*>.*24|24.*hours"` 跨 `module-hr/erp-hr-service/src/main` **零命中**（全 hr 业务代码无 24h 上限常量、无聚合校验逻辑）。
- **totalHours 零业务 writer**：grep `setTotalHours` 跨全 module-hr 命中全集 = `ErpHrTimesheetOutputBean.java:96` / `ErpHrTimesheetInputBean.java:95`（API bean setter）+ `_ErpHrTimesheet.java:397/:734`（codegen `_gen` setter）。**无业务代码 writer**（BizModel/Processor 零 setTotalHours）→ `ErpHrTimesheet.totalHours` 列（orm.xml:592）**派生字段未实现**，运行时永远 null/手工录入。

**裁决**：A1.14 §7-3 静态判定「无校验」**运行时确认成立**。员工同一日多条 TimesheetLine hours 之和 > 24h **不被拦截**（无聚合校验 mutation）+ totalHours **永远 null/手工录入**（派生字段未实现）。**维持 P1-RC-015**（24h 校验 + totalHours 汇总修复归 MR1，纯 BizModel 代码逻辑修复按 roadmap 预授权可自动执行，不触 §5 ask-first）。

### 2-4 A4.2.25 匿名 respondentHash 运行时防重复确认 — 维持 P1-RC-016

**运行时证据链（live code 实测）**：

- **Survey/Response 均为 18 行 CRUD 桩**：`ErpHrSurveyBizModel.java` 全文 18 行 + `ErpHrSurveyResponseBizModel.java` 全文 18 行，均 `extends CrudBizModel<...>` 零业务方法（无 publish/close/submitRespondent mutation）。
- **grep census 零 writer/零校验**：`rg -n "respondentHash|setRespondentHash|respondent_hash"` 跨 `module-hr/erp-hr-service/src/main` **零命中**（全 hr 业务代码无 respondentHash 写入、无唯一性校验）。
- **ORM 列存在但零消费**：`app-erp-hr.orm.xml:1429` `respondentHash` 列（VARCHAR(100)，propId=4，ErpHrSurveyResponse 实体）存在，但**零业务 writer + 零唯一性约束（无 UK）+ 零校验** → 匿名防重复机制**完全缺失**（ORM 列存在但无运行时消费）。

**裁决**：A1.14 §7-4 静态判定「无 writer/无校验」**运行时确认成立**。匿名模式重复提交**不被拦截**（无 respondentHash 写入 + 无唯一性约束）。**维持 P1-RC-016**（respondentHash 写入 + 唯一性校验修复归 MR1，纯 BizModel 代码逻辑修复可自动执行；ORM UK 增设若需则触 ask-first）。

### 2-5 A4.2.26 CLOSED 自动聚合 + eNPS 运行时计算确认 — 维持 P1-RC-016

**运行时证据链（live code 实测）**：

- **SurveyResult 为 18 行 CRUD 桩**：`ErpHrSurveyResultBizModel.java` 全文 18 行，`extends CrudBizModel<ErpHrSurveyResult>` 零业务方法（零 aggregateResult mutation + 零 eNPS 计算方法）。
- **grep census 零 survey 聚合命中**：`rg -ni "aggregateResult|calculateEnps|publishSurvey|closeSurvey|archiveSurvey"` 跨 `module-hr/erp-hr-service/src/main` 在 survey BizModel **零命中**。命中的 `aggregate*` 全部位于**胜任力/差距分析域**（`AssessmentAggregator` / `ErpHrGapAnalysisBizModel` / `ErpHrEmployeeAssessmentBizModel` / 其 Processor），**与调研域无关** → 调研域聚合算法**完全缺失**。
- **CLOSED 状态迁移无聚合触发**：`ErpHrSurveyBizModel` 18 行桩无 close mutation（grep `closeSurvey` 零命中）→ **CLOSED 状态不可达**（无 DRAFT→OPEN→CLOSED 迁移 writer），故「CLOSED 触发聚合」**结构上不可能发生**。
- **eNpsScore 零 writer**：`app-erp-hr.orm.xml:1357`（ErpHrSurvey.eNpsScore，派生）+ `:1499`（ErpHrSurveyResult.eNpsScore）列存在，grep `setENpsScore|seteNpsScore` 跨全 module-hr 业务代码**零命中** → eNPS 得分**永远 null**。

**裁决**：A1.14 §7-5 静态判定「无 mutation/无算法」**运行时确认成立**。CLOSED 不产出任何聚合数据 + eNPS 永远 null → **调研结果表运行时永远空**（无聚合 mutation 写入 ErpHrSurveyResult）。**维持 P1-RC-016**（aggregateResult + eNPS 计算 + 仪表盘修复归 MR1，纯 BizModel 代码逻辑修复可自动执行）。

---

## 3. 测试证据（L4）

| 测试 | 覆盖 | 与本审计关系 |
|------|------|------------|
| `TestErpHrPayrollEngine.java:408-440` | UC-HR-04 公司承担过账缺失（Deferred 标注） | **直接证据**：测试注释自述「tryPostAccrual 零调用方死代码，290/300 event 永不组装」+ 断言「Deferred：计提 SALARY(270) 凭证未生成」（`:440`），与本报告 §2-1 运行时确认一致 |
| `TestErpHrSurveyCrudSmoke.java` | UC-HR-11 调研 CRUD 冒烟 | **间接证据**：仅冒烟（㉖㉗㉘㉙ 零断言），无 respondentHash/aggregate/eNPS 覆盖 → 与 §2-4/§2-5 缺口确认一致 |
| （UC-HR-03 工时表 / UC-HR-04 计提） | — | **无独立运行时测试**：hr 侧无 timesheet 24h/approve 路径 + 无 approve→计提过账路径可达测试，与 §2-1/§2-3 不可达确认一致 |

---

## 4. 业财保护区域探针纪律声明

> A4.2.22 / A4.2.23 触及业财保护区域（roadmap §横切关注点 #5：会计过账逻辑 / VoucherFact / PostingProcessor 核心路径）。

本审计为**只读探针**，遵守保护区域暂停协议：

- **READ-ONLY 标记（6 处）**：本报告对 `SalaryPostingDispatcher` / `SalaryPostingProvider` / `ErpFinBusinessType` / `PayrollCalculator` / `buildEvent billData` / `IErpFinVoucherBiz.post()` 的全部交互均为**只读追踪**（grep census + 构造点追踪 + 调用方追踪 + billData 内容核查），**未修改任何过账逻辑 / VoucherFact 构造 / PostingProcessor 核心路径 / Provider createFacts**。
- **P1 维持不撤销**：P1-MA4-017 维持 P1（Q4 会计正确性类 reuse 重开），**修复义务归 MR1**，触及会计过账核心路径 + ORM 结构变更（ER 持久化）**须 ask-first + 独立 plan-audit**（roadmap §横切关注点 #5 + 预授权声明「会计过账逻辑变更须 ask-first」）。
- **修复面收窄证据**：§2-1 确认 `SalaryPostingProvider.createFacts` 290/300 分支会计逻辑已实现就绪 → MR1 修复面 = 派发侧接线（tryPostAccrual/290/300 调用方 + approve 联动）+ ER 持久化（ORM ask-first），createFacts 无需重写。此为 ask-first 决策的输入证据，**本审计不擅自实施**。

---

## 5. 与既有 finding 衔接（复用裁决，无新 finding）

按 §去重协议，每项运行时确认裁决均 grep arm-index 同域同控制点后给出「复用维持」结论：

| finding | 本审计对应 | 运行时裁决 |
|---------|----------|-----------|
| `P1-MA4-017`（arm-index :841） | A4.2.22 / A4.2.23（UC-HR-04 ⑯ 计提+公司承担过账链路 + ER 丢弃） | **reuse 维持 P1**：运行时确认 tryPostAccrual 零调用方 + 290/300 event 永不构造 + ER 金额丢弃 + billData 不含 ER + Provider 消费侧就绪但事件源断裂。Q4 会计正确性类无例外，audit-remediation 方案B Deferred 关闭不成立。修复归 MR1 + ask-first（会计过账核心路径 + ORM ER 持久化）。 |
| `P1-MA2-041`（arm-index :503） | A4.2.25 / A4.2.26（UC-HR-11 ㉗ publish/close） | **reuse 维持 P1**：运行时确认 ErpHrSurveyBizModel 仍 18 行桩 + close mutation 零命中 → CLOSED 不可达。Q4=(a) 下方案B Deferred 关闭不成立，修复归 MR1。 |
| `P1-MA2-043`（arm-index :505） | A4.2.24（UC-HR-03 ④⑤ approve/reject） | **reuse 维持 P1**：运行时确认 ErpHrTimesheetBizModel 仍仅 submit + grep approve/reject 零业务命中。Q4=(a) 下方案B Deferred 关闭不成立，修复归 MR1。（本审计 §2-3 主要核验 ②③ 24h/totalHours 维度，④⑤ 状态机维度为 P1-MA2-043 reuse 范围，一并确认维持。） |
| `P1-RC-015`（arm-index :154） | A4.2.24（UC-HR-03 ②③ 24h 校验 + totalHours 汇总） | **维持 P1**：运行时确认 grep MAX_HOURS/24h 零业务命中 + setTotalHours 零业务 writer。缺口存在，修复归 MR1（纯 BizModel 预授权，不触 ask-first）。 |
| `P1-RC-016`（arm-index :155） | A4.2.25 / A4.2.26（UC-HR-11 ㉖㉘㉙ 匿名防重复 + 聚合 + eNPS） | **维持 P1**：运行时确认 respondentHash 零 writer/零校验 + aggregateResult/calculateEnps 零 survey 命中 + eNpsScore 零 writer + 结果表永远空。缺口存在，修复归 MR1（纯 BizModel 预授权；ORM UK 若增设则 ask-first）。 |

**无新 finding 新建**（全部 reuse 维持 / 重开不降级）。运行时证据**未发现会计错误已活跃**（GL 从未收到错误凭证，而是缺凭证可经试算平衡发现）→ **不触发 MR0**。

---

## 6. 多维审计自检（multi-dimensional-audit-prompt.md）

按 `docs/skills/multi-dimensional-audit-prompt.md` 默认 7 维度 + nop-app-erp 项目特定维度，逐维度裁决：

- **需求正确性**：5 项存疑点均逐字引自 A1.14 §7（L1 use-cases.md 真相源），运行时裁决与需求契约对齐（UC-HR-03 ② / UC-HR-04 ⑯ / UC-HR-11 ㉖㉘㉙ 均为 L1 显式验收标准）。本维度无新发现。
- **owner-doc 对齐**：`payroll.md §五/§六/§九`（双轴状态机 + 计提 270+290+300 过账）+ `employee-survey.md §状态机/§匿名模式/§结果分析`（DRAFT→OPEN→CLOSED→ARCHIVED + respondentHash + CLOSED 自动聚合 + eNPS）+ `state-machine.md §三 工时表`（APPROVED/REJECTED）owner doc 声明与 HEAD 实现差距经运行时确认（方案B Deferred 标注）。本维度无新发现（差距归 MR1）。
- **架构或边界影响**：本审计零代码变更，不引入跨模块依赖 / API 契约变更 / 保护区域触碰。markPaid→`IErpFinVoucherBiz.post()` REQUIRES_NEW Facade 跨域边界经 A2.7b 证实合规（hr production 零 `daoFor(ErpFin*)` 直写）。本维度无新发现。
- **验证充分性**：每项运行时裁决均有独立 grep census + file:line 证据 + 调用方追踪（§2-1..§2-5），可独立证伪（若 tryPostAccrual 有调用方 / 若 setTotalHours 有业务 writer / 若 respondentHash 有 writer，则裁决翻转）。本维度无新发现。
- **回归风险**：零生产代码变更，无回归路径。本维度无新发现。
- **路由和技能选择正确性**：roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`，本审计为只读审计无代码变更，技能匹配。本维度无新发现。
- **待办或自主权策略漂移**：本审计范围 = A1.14 §7 五项存疑点运行时确认，未扩大范围、未关闭未完成项、未将阻塞降级为跟进项。五项 P1 全部维持（修复义务归 MR1，plan `Deferred But Adjudicated` 正确分类）。本维度无新发现。
- **项目特定维度（view.xml gen-control / ORM 完整性 / 代码生成纪律）**：本审计不触及 view.xml delta；ORM 列存在性核查（respondentHash :1429 / eNpsScore :1357/:1499 / totalHours :592）确认列存在但零业务 writer，属「列存在 writer 缺失」运行时缺口非 ORM 结构缺陷；未触及生成文件。本维度无新发现。

**反窄化自检通过**：已对全部 8 维度给出裁决（含「本维度无发现」），非单维深挖。

---

## 7. 过程纪律自检

- [x] **checker 退出码门控核查**：本审计为只读审计，**无生产代码变更**，checker 无回归风险。本报告不以 checker 脚本退出码作为门控通过依据（checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow 解析 actual > baseline）。零代码变更 → actual = baseline（git status 仅 .md 文件）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 5 项运行时裁决已按 §去重协议 grep arm-index 同域同控制点后给出「复用维持」结论（P1-MA4-017 / P1-MA2-041 / P1-MA2-043 reuse 维持 + P1-RC-015 / P1-RC-016 维持），**无未经比对直接新建的 finding，无新 finding 新建**。

---

## Verdict

**PASS（运行时确认维持 A1.14 §5/§6/§7 全部裁决）**：5 项静态存疑点运行时行为**全部确认成立**，A1.14 静态判定无一翻转：

- **A4.2.22（计提+公司承担过账触发链）CONFIRMED 维持 P1-MA4-017**：tryPostAccrual 零调用方 + 290/300 event 永不构造 + approve 零过账触发 + Provider 消费侧就绪但事件源断裂 → GL 仅收 280。会计错误**未活跃**（缺凭证可经试算平衡发现）→ **不触发 MR0**。修复归 MR1 + ask-first。
- **A4.2.23（公司承担金额丢弃）CONFIRMED 维持 P1-MA4-017**：ER 局部变量计算后丢弃（无 ORM 列 + 无 setRemark + billData 不含 ER 键 + 死常量 BILL_DATA_*_ER）。
- **A4.2.24（24h 校验）CONFIRMED 维持 P1-RC-015**：grep MAX_HOURS/24h 零业务命中 + setTotalHours 零业务 writer。
- **A4.2.25（匿名 respondentHash）CONFIRMED 维持 P1-RC-016**：respondentHash 零 writer/零校验/零 UK。
- **A4.2.26（CLOSED 聚合 + eNPS）CONFIRMED 维持 P1-RC-016**：aggregateResult/calculateEnps/closeSurvey 零 survey 命中 + eNpsScore 零 writer + 结果表永远空。

**裁决分支**：全部命中「维持 P1（reuse 重开 finding 不降级，Q4 强制实现）+ 运行时证据记录」分支；**无升级触发 MR0**（运行时未发现会计错误已活跃）。修复义务归 MR1 R1.0 展开器（A4.2.22/A4.2.23 触及会计保护区域 + ORM 结构须 ask-first + 独立 plan-audit；A4.2.24-A4.2.26 纯 BizModel 预授权）。

**本审计不实施修复**（只读审计，结果表面 = 本报告 + arm-index reuse 维持注记）。

---

## 参考

- 真相源：`docs/design/human-resource/use-cases.md`（UC-HR-03/04/10/11 验收标准 ②③④⑤⑯㉖㉗㉘㉙）
- 来源存疑点：`docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md` §7（5 项静态存疑点）+ §5（验收标准分级①-㉙）+ §6（finding 衔接裁决）
- 设计参考：`docs/design/human-resource/payroll.md`（§五/§六/§九 双轴状态机 + 计提 270/290/300 过账）+ `employee-survey.md`（§状态机/§匿名模式/§结果分析）+ `state-machine.md §三 工时表` + `docs/design/finance/posting.md`（过账触发链）
- L5 既有证据：`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（A2.7b markPaid 跨域 Facade）+ `docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`（A4.4 P1-MA4-017 登记）
- 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
- 技能：`docs/skills/multi-dimensional-audit-prompt.md`（默认 7 维度 + 项目特定维度）
