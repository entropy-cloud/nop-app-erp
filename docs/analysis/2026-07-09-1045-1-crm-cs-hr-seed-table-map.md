# CRM/客服/人力域业务交易单据种子 — 表清单、列映射、加载拓扑序与范围裁决

> Owner: `docs/plans/2026-07-09-1045-1-crm-cs-hr-transaction-seeds.md` Phase 1 Exit Criteria
> 权威源: `module-{crm,cs,hr}/model/app-erp-*.orm.xml` + `module-finance/model/app-erp-finance.orm.xml`(`ErpFinArApItem`) + `module-master-data/model/app-erp-master-data.orm.xml`(`ErpMdPartner`)（逐表逐列核实，非采信旧记忆）
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（21 张主数据 CSV 已 seed）
> 前序种子范式: `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（运营域域表「直 seed」范式）+ `docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`（跨两域范式，本计划最贴近镜像）

## 0. 约定（与 1234-1 / 1445-1 / 2210-1 / 0930-1 / 0930-2 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（1234-1/1445-1/2210-1/0930-1/0930-2 经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`。
- 日期列值 `YYYY-MM-DD`；本批无 mandatory DATETIME 业务列（CS ticket 的 deadline/start/end datetime、HR respondedAt/adjustedAt 等均 opt，一律省略）。
- **关键事实（报表读源反推）**：CRM/CS/HR 三域为**纯报表域（无看板 BizModel）**，各 1 个 `ErpXxxReportBizModel` 共 5 张报表，读域表非 GL。故 seed 域表即令报表非空，**无需 seed GL 凭证**（镜像运营/制造/维护+质量域范式）。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

本批 12 张新表（CRM 5 + CS 3 + HR 4）仅引用 1234-1 已 seed 主数据 + 本批先 seed 的上游域表；2 处既有 CSV 加性追加（md_partner +1 行 / fin_ar_ap_item +2 行）引用已 seed 主数据（org/acctSchema/currency/period）+ 本批追加的 partner 行：

```
[1234-1 主数据(已 seed)] md_organization(2) / md_currency(1) / md_partner(1-4)
  → [本批跨域追加] md_partner +1 行(id=5 EMPLOYEE 类型)              ← 早于 fin_ar_ap_item 追加行
[CRM 域]
  erp_crm_stage → erp_crm_lead(stageId FK→stage)
  erp_crm_forecast_period → erp_crm_forecast(periodId FK→period)
    → erp_crm_forecast_line(forecastId FK→forecast + leadId FK→lead)
[CS 域]
  erp_cs_ticket_type → erp_cs_ticket(ticketTypeId FK→type, customerId FK→md_partner)
    → erp_cs_survey(ticketId FK→ticket)
[HR 域]
  erp_hr_department → erp_hr_employee(departmentId FK→department)
  erp_hr_salary_simulation → erp_hr_salary_simulation_item_adj(simulationId FK→simulation + employeeId FK→employee)
[HR 跨域 finance 扩展]
  erp_fin_ar_ap_item +2 行(partnerId FK→md_partner id=5；引用 1234-1 已 seed org=2/acctSchema=1/currency=1/period=1)
```

> `stage` 先于 `lead`：lead.stageId 可选 FK→stage（seed 填以驱动 funnel）。
> `forecast_period` 先于 `forecast`：forecast.periodId mandatory FK→period。
> `forecast`+`lead` 先于 `forecast_line`：forecast_line.forecastId+leadId mandatory FK。
> `ticket_type` 先于 `ticket`：ticket.ticketTypeId mandatory FK→type。
> `ticket` 先于 `survey`：survey.ticketId mandatory FK→ticket。
> `department` 先于 `employee`：employee.departmentId 可选 FK→department（seed 填以驱动小计行）。
> `salary_simulation` 先于 `simulation_item_adj`：simulationId mandatory FK。
> `employee` 先于 `simulation_item_adj`：employeeId mandatory FK。
> `md_partner` 追加行（id=5）先于 `fin_ar_ap_item` 追加行（partnerId=5）：partner 属 1234-1 主数据批，finance ar_ap_item 属 1445-1 批，加载序天然满足。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 CRM 域（customer-relationship）— 5 表

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_crm_stage | ID; CODE(M); STAGE_NAME(M); SEQUENCE(M int); ORG_ID(FK org=2,opt); DEFAULT_PROBABILITY(opt int); IS_WON_STAGE(opt bool); REMARK(opt) | 2 |
| erp_crm_lead | ID; CODE(M); ORG_ID(FK org=2,opt); LEAD_TYPE(M, dict erp-crm/lead-type: LEAD/OPPORTUNITY); PARTNER_ID(FK partner,opt); STAGE_ID(FK stage,opt,seed 填非 null 驱动 funnel); EXPECTED_REVENUE(opt); DOC_STATUS(M, dict erp-crm/lead-doc-status: NEW/QUALIFIED/CONVERTED/LOST/CANCELLED) | 2 |
| erp_crm_forecast_period | ID; CODE(M); ORG_ID(FK org=2,opt); PERIOD_TYPE(M, dict erp-crm/forecast-period-type: MONTHLY/QUARTERLY/ANNUAL); PERIOD_START(M DATE); PERIOD_END(M DATE); LABEL(opt); STATUS(M, dict erp-crm/forecast-period-status: OPEN/CLOSED/FROZEN); IS_CURRENT(opt bool) | 1 |
| erp_crm_forecast | ID; ORG_ID(FK org=2,opt); PERIOD_ID(FK period,M); CURRENCY_ID(FK currency=1,opt); COMMIT_AMOUNT(opt); UPSIDE_AMOUNT(opt); WEIGHTED_AMOUNT(opt); BEST_CASE_AMOUNT(opt); OPPORTUNITY_COUNT(opt int); COMMIT_OPPORTUNITY_COUNT(opt int); EXPECTED_CLOSED_REVENUE(opt) | 1 |
| erp_crm_forecast_line | ID; FORECAST_ID(FK forecast,M); ORG_ID(FK org=2,opt); LEAD_ID(FK lead,M); PROBABILITY(opt int); EXPECTED_REVENUE(opt); WEIGHTED_REVENUE(opt); FORECAST_CATEGORY(M, dict erp-crm/forecast-category: COMMIT/UPSIDE/BEST_CASE); INCLUDED_IN_COMMIT(opt bool); STAGE_NAME(opt) | 2 |

> **注意（forecast 无 CODE 列）**：`ErpCrmForecast` 实体无 `code` 列（无 UK 约束，仅 index），CSV 不含 CODE。其余 CRM 表均有 UK_*_CODE_ORG，code 须域内唯一。

### 2.2 CS 域（customer-service）— 3 表

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_cs_ticket_type | ID; CODE(M); NAME(M); DEFAULT_PRIORITY(opt, dict erp-cs/ticket-priority: LOW/NORMAL/HIGH/URGENT); SEQUENCE(opt int); REMARK(opt) | 2 |
| erp_cs_ticket | ID; CODE(M); ORG_ID(FK org=2,opt); SUBJECT(M); CUSTOMER_ID(FK partner=1/2 customer,M); TICKET_TYPE_ID(FK ticket_type,M); PRIORITY(M, dict erp-cs/ticket-priority); SOURCE(opt, dict erp-cs/ticket-source); IS_SLA_COMPLETED(opt bool); STATUS(M, dict erp-cs/ticket-status: NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED); DOC_STATUS(M, dict erp-cs/doc-status: DRAFT/ACTIVE/CANCELLED); APPROVE_STATUS(M, dict wf/approve-status: UNSUBMITTED/SUBMITTED/APPROVED/REJECTED); BUSINESS_DATE(M DATE) | 2 |
| erp_cs_survey | ID; ORG_ID(FK org=2,opt); TICKET_ID(FK ticket,M); CSAT_SCORE(opt int,seed 填非 null 驱动 avg 非零); NPS_SCORE(opt int,seed 填非 null 驱动 avg 非零); SURVEY_CHANNEL(opt, dict erp-cs/survey-channel); COMMENT(opt) | 1 |

> **CS ticket 无 `posted` 列**：ticket 实体含 `ORG_ID`+`BUSINESS_DATE`(M)+`docStatus`+`approveStatus` 但**无 `posted` 列**（报表读 status/isSlaCompleted 非 posted）。

### 2.3 HR 域（human-resource）— 4 表

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_hr_department | ID; CODE(M); NAME(M); ORG_ID(FK org=2,opt); REMARK(opt) | 2 |
| erp_hr_employee | ID; CODE(M); FIRST_NAME(M); LAST_NAME(M); FULL_NAME(opt,seed 填); GENDER(M, dict erp-hr/gender: MALE/FEMALE); DEPARTMENT_ID(FK department,opt,seed 填驱动小计行); HIRE_DATE(M DATE); EMPLOYMENT_STATUS(M, dict erp-hr/employment-status: ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED); EMPLOYEE_TYPE(M, dict erp-hr/employee-type: FULL_TIME/PART_TIME/CONTRACTOR/INTERN); ORG_ID(FK org=2,opt) | 2 |
| erp_hr_salary_simulation | ID; CODE(M); ORG_ID(FK org=2,opt); SIMULATION_PERIOD_YEAR(M int); SIMULATION_PERIOD_MONTH(M int); SIMULATION_NAME(opt); STATUS(M, dict erp-hr/simulation-status: DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED); BUSINESS_DATE(M DATE) | 1 |
| erp_hr_salary_simulation_item_adj | ID; SIMULATION_ID(FK simulation,M); EMPLOYEE_ID(FK employee,M); SALARY_ITEM_CODE(M); ORIGINAL_AMOUNT(M decimal); ADJUSTED_AMOUNT(M decimal); ADJUSTMENT_REASON(opt, dict erp-hr/adjustment-reason: SALARY_CHANGE/ALLOWANCE_CHANGE/BONUS_CHANGE/MANUAL_ENTRY); ORG_ID(FK org=2,opt) | 2 |

> **关键事实（erp_hr_employee 未 seed）**：1234-1 seed 的是 master-data `erp_md_employee`(EMP-001~003)，HR 域 `erp_hr_employee` **此前零 seed**（报表 payroll-sim 的 employeeId FK 须指向本表，非 md_employee）。本批新建。
> **salary_simulation 有 BUSINESS_DATE(M)**：与 0056-1 标准字段补齐一致，CSV 必填。

### 2.4 跨域 finance 扩展（HR employee-net-balance 驱动）— 1 表追加

| 表 | 追加行列 | 备注 |
|----|----------|------|
| erp_fin_ar_ap_item | +2 行（id=5 EMPLOYEE_ADVANCE/RECEIVABLE/OPEN + id=6 EXPENSE_CLAIM/PAYABLE/OPEN，partnerId=5 指向员工型 partner） | 既有 CSV 加性追加，引用既有列（DIRECTION/PARTNER_ID/SOURCE_BILL_TYPE/STATUS/OPEN_AMOUNT_FUNCTIONAL 等），**既有列无 `posted`**（实体无 posted 列）|

### 2.5 跨域 master-data 扩展（HR employee-net-balance 驱动）— 1 表追加

| 表 | 追加行列 | 备注 |
|----|----------|------|
| erp_md_partner | +1 行（id=5, PARTNER_TYPE=EMPLOYEE, STATUS=ACTIVE） | 既有 CSV 加性追加，供 ar_ap_item.partnerId=5 引用 + 姓名解析；对齐 `docs/design/finance/expense-claim.md` 员工-as-partner 设计 |

**字典码值（已核实 dict.yaml）**：
- `erp-crm/lead-type`：LEAD/OPPORTUNITY → 本批 lead 用 `OPPORTUNITY`
- `erp-crm/lead-doc-status`：NEW/QUALIFIED/CONVERTED/LOST/CANCELLED → 本批 lead 用 `QUALIFIED`（活跃态）
- `erp-crm/forecast-period-type`：MONTHLY/QUARTERLY/ANNUAL → 本批用 `QUARTERLY`
- `erp-crm/forecast-period-status`：OPEN/CLOSED/FROZEN → 本批用 `OPEN`
- `erp-crm/forecast-category`：COMMIT/UPSIDE/BEST_CASE → 本批用 `COMMIT`+`BEST_CASE`
- `erp-cs/ticket-priority`：LOW/NORMAL/HIGH/URGENT → 本批用 `HIGH`+`NORMAL`
- `erp-cs/ticket-status`：NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED → 本批用 `RESOLVED`+`IN_PROGRESS`
- `erp-cs/doc-status`：DRAFT/ACTIVE/CANCELLED → 本批用 `ACTIVE`
- `wf/approve-status`：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED → 本批 ticket 用 `APPROVED`+`UNSUBMITTED`
- `erp-cs/survey-channel`：EMAIL/PHONE/PORTAL/CHAT → 本批用 `EMAIL`
- `erp-hr/gender`：MALE/FEMALE → 本批用 `MALE`+`FEMALE`
- `erp-hr/employment-status`：ACTIVE/PROBATION/RESIGNED/TERMINATED/RETIRED → 本批用 `ACTIVE`
- `erp-hr/employee-type`：FULL_TIME/PART_TIME/CONTRACTOR/INTERN → 本批用 `FULL_TIME`
- `erp-hr/simulation-status`：DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED → 本批用 `DRAFT`
- `erp-hr/adjustment-reason`：SALARY_CHANGE/ALLOWANCE_CHANGE/BONUS_CHANGE/MANUAL_ENTRY → 本批用 `SALARY_CHANGE`
- `erp-md/partner-type`：CUSTOMER/SUPPLIER/BOTH/EMPLOYEE → 追加 partner 用 `EMPLOYEE`
- `erp-md/active-status`（partner.STATUS）：ACTIVE → 追加 partner 用 `ACTIVE`
- `erp-fin/ar-ap-direction`：RECEIVABLE/PAYABLE → 追加 ar_ap_item 用 `RECEIVABLE`+`PAYABLE`
- `erp-fin/ar-ap-status`：OPEN/PARTIAL/SETTLED → 追加 ar_ap_item 用 `OPEN`（报表 `findOpenItems` 仅取 OPEN+PARTIAL）
- `SOURCE_BILL_TYPE`（无 dict，自由字符串）：EMPLOYEE_ADVANCE / EXPENSE_CLAIM（与 `ErpFinConstants` 常量 + `ErpFinArApItemGenerator` 口径一致）

## 3. seed 设计（引用 1234-1 已 seed 主数据固定 ID；通用 orgId=2(ERP-CO)、currencyId=1(CNY)、acctSchemaId=1、periodId=1）

### 3.1 CRM 链（2 stage + 2 lead 覆盖不同 stageId 驱动 funnel + forecast 链）

- **stage**（2 行）：1=`STAGE-001` 验证(sequence=10, defaultProbability=30)；2=`STAGE-002` 报价(sequence=20, defaultProbability=60)。
- **lead**（2 行，不同 stageId）：1=`LEAD-2026-001`(OPPORTUNITY, partnerId=1, stageId=1, expectedRevenue=50000, docStatus=QUALIFIED)；2=`LEAD-2026-002`(OPPORTUNITY, partnerId=2, stageId=2, expectedRevenue=80000, docStatus=QUALIFIED)。
- **forecast_period**（1 行）：1=`FP-2026-Q3`(QUARTERLY, 2026-07-01~2026-09-30, status=OPEN, isCurrent=true)。
- **forecast**（1 行，无 code）：1=orgId=2, periodId=1, currencyId=1, commitAmount=50000, weightedAmount=45000, bestCaseAmount=80000, opportunityCount=2, commitOpportunityCount=1。
- **forecast_line**（2 行，每 lead 一行）：1=forecastId=1, leadId=1, probability=30, expectedRevenue=50000, weightedRevenue=15000, forecastCategory=COMMIT, includedInCommit=true, stageName=验证；2=forecastId=1, leadId=2, probability=60, expectedRevenue=80000, weightedRevenue=48000, forecastCategory=BEST_CASE, includedInCommit=false, stageName=报价。

**金额自洽**：forecast_line.weightedRevenue = expectedRevenue × probability/100（行1: 50000×0.30=15000 ✓；行2: 80000×0.60=48000 ✓）。

### 3.2 CS 链（2 ticket_type + 2 ticket 不同 type + isSlaCompleted 混合 + survey 驱动 avg）

- **ticket_type**（2 行）：1=`TT-COMPLAINT` 投诉(defaultPriority=HIGH)；2=`TT-INQUIRY` 咨询(defaultPriority=NORMAL)。
- **ticket**（2 行，不同 ticketTypeId + isSlaCompleted 混合）：1=`TKT-2026-001`(subject=产品功能咨询, customerId=1, ticketTypeId=1, priority=HIGH, isSlaCompleted=true, status=RESOLVED, docStatus=ACTIVE, approveStatus=APPROVED, businessDate=2026-07-03)；2=`TKT-2026-002`(subject=交期延迟投诉, customerId=2, ticketTypeId=2, priority=NORMAL, isSlaCompleted=false, status=IN_PROGRESS, docStatus=ACTIVE, approveStatus=UNSUBMITTED, businessDate=2026-07-04)。
- **survey**（1 行，csat/nps 非空驱动 avg 非零）：1=orgId=2, ticketId=1, csatScore=5, npsScore=9, surveyChannel=EMAIL。

**计数自洽**：ticket_type=1(COMPLAINT) 桶 totalTickets=1/slaCompleted=1；ticket_type=2(INQUIRY) 桶 totalTickets=1/slaBreached=1；survey 挂 ticket 1 → COMPLAINT 桶 surveyCount=1/avgCsat=5/avgNps=9。

### 3.3 HR payroll-sim 链（2 department + 2 employee 不同部门驱动小计 + simulation 链）

- **department**（2 行）：1=`DEPT-SALES` 销售部；2=`DEPT-CS` 客服部。
- **employee**（2 行，不同 departmentId）：1=`HR-EMP-001`(赵明, MALE, departmentId=1, hireDate=2024-03-01, ACTIVE, FULL_TIME)；2=`HR-EMP-002`(钱华, FEMALE, departmentId=2, hireDate=2023-07-15, ACTIVE, FULL_TIME)。
- **salary_simulation**（1 行）：1=`SIM-2026-001`(2026年7月, status=DRAFT, businessDate=2026-07-05)。
- **salary_simulation_item_adj**（2 行，不同 employeeId + 同 salaryItemCode）：1=simulationId=1, employeeId=1, BASE_SALARY, original=10000, adjusted=11000, SALARY_CHANGE；2=simulationId=1, employeeId=2, BASE_SALARY, original=8000, adjusted=8500, SALARY_CHANGE。

**金额自洽**：difference = adjusted − original（行1: +1000；行2: +500）；部门小计 dept1=1000, dept2=500（DEPT_SUBTOTAL 行）。

### 3.4 HR employee-net-balance 跨域扩展（partner 追加 + ar_ap_item 追加）

- **md_partner +1 行**：5=`EMP-PTN-001`(张三员工往来, PARTNER_TYPE=EMPLOYEE, STATUS=ACTIVE, contactPerson=张三)。
- **fin_ar_ap_item +2 行**：
  - 5=`ARAP-EA-001`(RECEIVABLE, partnerId=5, EMPLOYEE_ADVANCE, businessDate=2026-07-05, amount=1000/open=1000, STATUS=OPEN)。
  - 6=`ARAP-EC-001`(PAYABLE, partnerId=5, EXPENSE_CLAIM, businessDate=2026-07-06, amount=300/open=300, STATUS=OPEN)。

**金额自洽**：OPEN 态 openAmount=amount（全额未核销），settled=0；employee-net-balance 报表 partner 5: advanceBalance=1000, expenseBalance=300, netBalance=700（员工欠公司）。

## 4. 范围 Decision（Phase 1 item 2）

### 4.a 范围 Decision — CRM 5 表 + CS 3 表 + HR 4 表为最小集

**选择**：仅 seed 范围内 5 报表 `QueryBean`/`findAll` 直接读的域表（CRM stage/lead/forecast_period/forecast/forecast_line + CS ticket_type/ticket/survey + HR department/employee/salary_simulation/simulation_item_adj），每域最小连通集（≥2 行覆盖关键分组维度）。

**替代方案（rejected）**：
- (a) 同时 seed CRM 配置表（product_config_rule/price_rule/bundle_pricing/territory/team/campaign 等）/CS 配置表（knowledge_base/sla_policy/entitlement/catalog）/HR 配置表（salary/salary_item/leave/attendance/shift/competency/social_insurance）→ **rejected**：这些表不被范围内 5 报表直接读（lead/ticket/simulation 的配置 FK 均非强制可留 null），seed 徒增参照复杂度且不解除报表阻塞。归 Deferred（触发条件：对应域配置/执行链端到端回归需这些数据时）。

**残留风险**：参照完整性遗漏（FK 引用未 seed 上游）→ 启动期 `DataInitInitializer` 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。

### 4.b HR 跨域扩展 Decision — 追加 ar_ap_item + 追加员工型 partner

**选择**：方案 A —— `erp_fin_ar_ap_item` 追加 EMPLOYEE_ADVANCE(RECEIVABLE)/EXPENSE_CLAIM(PAYABLE)+status=OPEN 行 + `erp_md_partner` 追加 PARTNER_TYPE=EMPLOYEE 行，解除 HR employee-net-balance 报表阻塞。

**替代方案（rejected）**：
- (b) 方案 B「employee-net-balance 归 Deferred」→ **rejected**：致 HR 2 报表之一仍空集，本批 HR 域价值减半；且 ar_ap_item 追加为**加性追加**（不改既有 4 行 SETTLED 数据），partner 追加 EMPLOYEE 类型行对齐 `docs/design/finance/expense-claim.md` 员工-as-partner 设计，风险可控。

**残留风险与防护**：
- 既有 finance 报表/看板数值漂移 → **已核证无影响**：(1) finance 看板 `getDashboardKpi` 的 revenue/netProfit/expense 读 GL（voucher/income-statement），`arBalance`/`apBalance` 虽读 ar_ap_item open 但非 `finance.value.spec.ts` 断言字段（spec 仅断言 revenue=1130/netProfit=1130/expense=0）；(2) `fin-ar-ap-aging.value.spec.ts` 仅断言报表标题 token「应收应付账龄分析表」+「未核销余额合计」标签存在，不断言具体数值（OPEN 行加入后总数行有值但标签 token 仍在）；(3) HR employee-net-balance 的 `findOpenItems(direction)` 仅取 status IN [OPEN,PARTIAL]，既有 4 行全 SETTLED 不被取，仅本批 2 行 OPEN 被聚合。
- 加性追加破坏既有行 → CSV 仅在尾部 append，不动既有行；fresh-DB 重置保证幂等。

### 4.c posted Decision

**选择**：CRM/CS/HR 新增域表统一省略 `posted` 列（镜像 0930-2，报表读域表非 GL）：
- CRM stage/lead/forecast_period/forecast/forecast_line、CS ticket_type/ticket/survey、HR department/employee/salary_simulation/simulation_item_adj —— **实体本身无 `posted` 列**（逐表 ORM 核实：CRM 表均无 posted；CS ticket 无 posted（有 docStatus/approveStatus/businessDate）；HR 表无 posted）。故 CSV 不含 posted。
- HR 跨域 `erp_fin_ar_ap_item` 追加行 —— **实体无 `posted` 列**（ORM 核实 ar_ap_item 列集无 posted），CSV 沿用 1445-1 既有列约定（不含 posted）。
- HR 跨域 `erp_md_partner` 追加行 —— partner 无 posted 列。

> posted 裁决依据：三域 5 报表读域表/状态列非 posted（`buildLeadConversionFunnelDataset` 读 lead.stageId、`buildForecastAccuracyDataset` 读 forecast/forecast_line、`buildTicketSlaCsatSummaryDataset` 读 ticket.isSlaCompleted、`buildEmployeeNetBalanceDataset` 读 ar_ap_item.status/sourceBillType/direction、`buildPayrollSimulationComparisonDataset` 读 simulation_item_adj）。`posted` 非任何报表过滤列。

### 4.d ID 分配 Decision

- 新表 ID 自 1 起（每表独立 seq）。
- 既有表追加行 ID 接续：partner 自 5 起（既有 1-4）；ar_ap_item 自 5 起（既有 1-4）。
- 关键日期/status/dict/跨域 ID 选择依据见 §3 各链设计。

## 5. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**。所有 CRM/CS/HR 种子经 CSV INSERT 表达（含 2 处加性追加），无序列重置 / 批量 UPDATE 需求。故不补 `NN-init-crm-cs-hr-*.sql`。

## 6. seed 行数汇总

| 域 | 表数 | 行数 |
|----|------|------|
| crm（stage+lead+forecast_period+forecast+forecast_line） | 5 | 2+2+1+1+2 = 8 |
| cs（ticket_type+ticket+survey） | 3 | 2+2+1 = 5 |
| hr（department+employee+salary_simulation+simulation_item_adj） | 4 | 2+2+1+2 = 7 |
| hr 跨域 finance 扩展（fin_ar_ap_item 追加） | (+1 既有表) | +2 |
| hr 跨域 master-data 扩展（md_partner 追加） | (+1 既有表) | +1 |
| **合计** | **12 张新表 CSV + 2 处既有 CSV 加性追加** | **20 行新增 + 3 行追加** |

> 12 张新表 CSV 加入 `_vfs/_init-data/`，与既有 72 张 CSV（21 主数据 + 23 P2P/O2C + 13 运营域 + 4 制造域 + 11 维护+质量域）共存（总计 84 张 CSV + 2 处加性追加）。
