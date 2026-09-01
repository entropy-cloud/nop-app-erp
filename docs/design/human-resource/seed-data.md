# 人力资源域种子数据（Seed Data）

> Owner: 本文件（hr 域「种子数据」owner doc；plan `2026-09-01-2255-2-m13-hr-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 hr 节（32 实体逐行规格权威源）+ 对账表（计数权威源）
> 纪律来源: `docs/design/field-formatting-patterns.md` §9（F7 PII 集权威源）+ plan `2026-08-11-1030-1`（E4.2 MaskHelper 范式）

## 1. 范围与文件清单

M1.3 批次（2026-09-02）为 hr 域补齐 32 个 seed CSV（既有 4 表 employee / department / salary_simulation / salary_simulation_item_adj 文件 `erp_hr_salary_simulation.csv` / `erp_hr_salary_simulation_item_adj.csv` 由 plan `1045-1` 落地，本批不触碰）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`（`DataInitInitializer.loadCsvData` 按表名查找契约）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_hr_position.csv | 3 | 头/独立 | P | 职位（销售经理/客服专员/销售代表） |
| erp_hr_employment_contract.csv | 3 | 头/独立 | P+N-TERM | emp1 两期合同（15000）+ emp2 合同（8000）；EXPIRED 终态行 1 |
| erp_hr_leave_balance.csv | 3 | 头/独立 | P | emp1 ANNUAL+SICK / emp2 ANNUAL，fiscalYear 2026 |
| erp_hr_leave_request.csv | 3 | 头/独立 | P+N-TERM | ANNUAL APPROVED + SICK REJECTED（终态）+ PERSONAL CANCELLED（终态） |
| erp_hr_attendance.csv | 3 | 头/独立 | P | 2026-05 打卡行（UK employee+date） |
| erp_hr_timesheet.csv | 3 | 头 | P+N-TERM | APPROVED / SUBMITTED / REJECTED（终态）；TOTAL_HOURS = Σ lines |
| erp_hr_timesheet_line.csv | 4 | 子表（timesheet） | P | 每头 1~2 行，PROJECT_ID/TASK_ID 可选留空 |
| erp_hr_salary.csv | 2 | 头/独立 | P | 2025-11 两员工核算行（值中性化契约见 §4） |
| erp_hr_salary_item.csv | 3 | 头/独立 | P | BASIC/ALLOWANCE/SOCIAL 三项目（UK code+org） |
| erp_hr_payroll_bank_file.csv | 2 | 头/独立 | P+N-TERM | 2025-11 GENERATED（对账 18190.00/2 行）+ 2025-10 CONFIRMED（终态） |
| erp_hr_recruitment.csv | 3 | 头/独立 | P+N-TERM | SCREENING / OFFERED / HIRED（终态）；候选人 PII 伪值见 §3 |
| erp_hr_shift.csv | 3 | 头/独立 | P | FIXED / ROTATING / FLEXIBLE 班次模板 |
| erp_hr_shift_assignment.csv | 4 | 子表（shift） | P+N-TERM | PRESENT×2 + ABSENT（关联 leave 1）+ CANCELLED（终态） |
| erp_hr_shift_rotation_pattern.csv | 2 | 头/独立 | P | WEEKLY / MONTHLY 轮换模板（PATTERN_DATA 用 `|` 分隔避免 CSV 引号） |
| erp_hr_shift_swap_request.csv | 2 | 头/独立 | P+N-TERM | APPROVED（终态）+ PENDING；SOURCE_ASSIGNMENT_ID 必填指向本批 |
| erp_hr_survey.csv | 2 | 头 | P+N-TERM | OPEN + CLOSED（终态）；TOTAL_QUESTIONS/TOTAL_RESPONSES 与子表一致 |
| erp_hr_survey_question.csv | 3 | 子表（survey） | P | survey1 两题（RATING）+ survey2 一题（ENPS） |
| erp_hr_survey_response.csv | 3 | 子表（survey） | P | 匿名答卷（RESPONDENT_HASH=anon-000N，EMPLOYEE_ID 留空） |
| erp_hr_survey_answer.csv | 5 | 头/独立（FK→response+question） | P | 评分值与 survey_result 聚合一致（survey1 4 行 + survey2 1 行，见 §5） |
| erp_hr_survey_result.csv | 2 | 子表（survey） | P | 聚合行，DRIVER_SCORES/QUESTION_BREAKDOWN 为 JSON |
| erp_hr_competency.csv | 3 | 头/独立 | P | BEHAVIOR/SKILL/KNOWLEDGE 三类字典 |
| erp_hr_competency_level.csv | 4 | 子表（competency） | P | comp1/comp2 各两级 |
| erp_hr_role_competency.csv | 3 | 头/独立 | P | position×competency 要求矩阵 |
| erp_hr_employee_assessment.csv | 3 | 头 | P+N-TERM | MANAGER COMPLETED / SELF SUBMITTED / 360 COMPLETED（终态） |
| erp_hr_assessment_detail.csv | 4 | 子表（assessment） | P | 与 gap_analysis 实际等级一致（见 §5） |
| erp_hr_gap_analysis.csv | 3 | 头/独立 | P | gap 值 = required − actual；severity 与值匹配 |
| erp_hr_development_plan.csv | 3 | 头 | P+N-TERM | IN_PROGRESS / COMPLETED（终态）/ CANCELLED（终态） |
| erp_hr_development_plan_item.csv | 4 | 子表（plan） | P+N-TERM | IN_PROGRESS / NOT_STARTED / ACHIEVED（终态）/ NOT_STARTED（随计划取消） |
| erp_hr_social_insurance_base.csv | 2 | 头/独立 | P | emp1/emp2 SHENZHEN 15000/15000（值中性化契约见 §4） |
| erp_hr_social_insurance_config.csv | 3 | 头/独立 | P | 仅 SHANGHAI 三险种（值中性化契约见 §4） |
| erp_hr_tax_config.csv | 1 | 头/独立 | P | year=2025 七级累进税率表（JSON 列带 CSV 引号转义） |
| erp_hr_tax_special_deduction.csv | 2 | 头/独立 | P | year=2025 两行（verified true/false 各一） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`ErpHrEmployee`（ID 1/2）、`ErpHrDepartment`（ID 1/2）、`erp_md_organization`（ORG_ID=2）、`erp_md_currency`（ID 1）、`erp_md_bank_account`（ID 1）。
- **〔本批〕链路**：position → role_competency → competency → competency_level；employee_assessment → assessment_detail → competency；gap_analysis ← development_plan_item；shift → shift_assignment ← shift_swap_request；survey → survey_question / survey_response → survey_answer；survey → survey_result；timesheet → timesheet_line；employment_contract →（payroll 读）salary → payroll_bank_file（BANK_ID→erp_md_bank_account 1）。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 引用完整性断言门禁，白名单零增量）；可选 FK 一律留空（timesheet_line.projectId/taskId、recruitment.employeeId、survey.targetDepartmentId 等）。

## 3. 敏感字段脱敏伪值纪律（F7 + 保守扩展）

- **F7 PII 集 4 字段**（idCardNo / mobilePhone / bankAccountId / socialSecurityNo，权威源 `field-formatting-patterns.md` §9.7.2；另 taxFileNo §9.7.3 隐藏项）**仅存在于 `ErpHrEmployee`**（已 seed、非本批）——本批 32 表经 ORM 逐列核对**零 F7 列**，无需落地 F7 值；MaskHelper（E3.1/E4.2）读路径脱敏对 seed 值透明。
- **批内 PII 邻接列 = `erp_hr_recruitment` 候选人三列**（candidateName / candidatePhone / candidateEmail）。此三列**不在 E3.1/E4.2 masking 面**（MaskHelper 仅覆盖 ErpHrEmployee PII + 保密金额面），伪值纪律是唯一保护——**load-bearing**。值格式约定（可重放）：
  - candidateName：`张应聘` / `李求职` / `王候选`（语义化虚构姓名，非真实个人）；
  - candidatePhone：`13800001021` 起顺序号（138-0000-01xx 明显合成段）；
  - candidateEmail：`<name>-empNNN@example.com`（IANA 保留文档域，不可能送达真实邮箱）。
- **薪酬金额列保守纳入同纪律**（超出 F7 集的保守口径，`salaryAmount` 非 F7 成员）：employment_contract（ANNUAL_SALARY/MONTHLY_SALARY/SOCIAL_INSURANCE_BASE/HOUSING_FUND_BASE）、salary 全金额列、recruitment.OFFER_SALARY、payroll_bank_file.TOTAL_AMOUNT、social_insurance_base/config 基数与限额、tax_config.TAX_THRESHOLD、tax_special_deduction.MONTHLY_AMOUNT。全部为确定性整数/规整小数演示伪值（如 15000.00 / 0.16），无任何 production-grade 真实个人数据（roadmap Non-Goal）。
- 批内 plan-first 保护区独立 plan-audit 已通过（fresh session `ses_fa1ca7ed6ffeJjneNygHBvCCiX`，APPROVED，批准记录落盘该 plan）。

## 4. C17 集成用例值中性化契约（维护者必读）

`app-erp-all` 集成用例 `TestErpC17HrSalaryPayment` 以 `nop.orm.init-database-data=true` **装载全局 seed**，其核算断言（emp1 GROSS=15000 / 社保 1200 / 公积金 1800 / 个税 210 / NET=11790；emp2 GROSS=8000）依赖以下 seed 不变量——**修改本批 CSV 前必读**：

1. `erp_hr_employment_contract`：emp1 全部行 MONTHLY_SALARY=15000.00、emp2 全部行=8000.00（`findActiveContract` 为 employeeId eq + limit 1，**无 status/orderBy 过滤**，任一行被选中均须同值）。
2. `erp_hr_social_insurance_base`：emp1/emp2 行逐值镜像 SHENZHEN / 15000.00 / 15000.00（`findBase` 同为 limit 1 无过滤；`SocialInsuranceCalculator` 按 base.cityCode 取配置）。
3. `erp_hr_social_insurance_config`：**禁用 SHENZHEN 行**（`findConfigs` 对该城市全部非 HOUSING_FUND 配置求和，C17 自种 SHENZHEN PENSION 15%/8% + HOUSING_FUND 12%/12%；加行即双重计数）。本批仅 SHANGHAI。
4. `erp_hr_tax_config`：仅 year=2025（`findTaxConfig` eq year=2026 limit 1，C17 自种 2026 档）。
5. `erp_hr_tax_special_deduction`：仅 year=2025（`sumSpecialDeduction` eq (employee,2026,7) 求和 verified 行）。
6. `erp_hr_salary`：仅 year=2025（`findPreviousCumulative` eq (employee,2026,month<7)；且不与 C17 的 (emp,2026,7) 创建面冲突；本批行 posted=false / UNSUBMITTED / PENDING 不触 xwf/过账面）。
7. `erp_hr_attendance` / `erp_hr_leave_request` / `erp_hr_shift_assignment`：日期冻结 2026-05，**无任何行落入 2026-07 窗口**（`summarizeAttendance` 窗口过滤 + `sumUnpaidLeaveDays` 只计 APPROVED SICK/PERSONAL 并写入快照列 UNPAID_LEAVE_DAYS；本批 SICK=REJECTED / PERSONAL=CANCELLED 双保险）。

## 5. 域内数值自洽约束（批内一致性）

- timesheet.TOTAL_HOURS = Σ timesheet_line.HOURS（16.00 / 6.50 / 8.00）。
- payroll_bank_file 行 1（2025-11 GENERATED）TOTAL_AMOUNT=18190.00 = salary 两行 NET_SALARY 之和（11790+6400），RECORD_COUNT=2；行 2（2025-10 CONFIRMED）为历史终态演示。
- salary 行自洽：NET = GROSS − 社保个人 − 公积金个人 − 个税 − 其他扣款。
- survey.TOTAL_QUESTIONS/TOTAL_RESPONSES = 子表行数；survey_result 聚合 = survey_answer 均值（survey1 avg 4.00 = (4+5+3+4)/4；survey2 avg 9.00）。
- assessment_detail.actualLevel 与 gap_analysis.actualLevel 一致（comp1→3、comp2→2）；gap_value = required − actual。
- development_plan_item.gapId 指向同员工同胜任力的 gap 行（row1→gap1）；ACHIEVED 项对应 COMPLETED 计划（plan2）。

## 6. 装载与验证

- 装载：`DataInitInitializer` 按依赖拓扑序自动排序；列头为 DB 列名大写下划线、省略审计列（DEL_VERSION/VERSION/CREATED_BY/CREATE_TIME/UPDATED_BY/UPDATE_TIME/POSTED 走默认值）、ISO 日期、小写布尔、ID < 100000、静态冻结日期（无 now()/滚动期间语义，bug 家族 `2026-09-01-0017`/`0058` 纪律）。
- 门禁：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity`（scope-pinning + 零孤儿 CSV + 行数>0 + 引用完整性零悬空白名单零增量 + CSV 基线常量 269）。
- 快照重录义务：本批为纯加性插入，集成快照 `output/tables/*.csv` 为 `_chgType` 变更行机制（M1.2b 实证不入既有快照）；`TestErpC17HrSalaryPayment` 行为面由 §4 契约覆盖。
