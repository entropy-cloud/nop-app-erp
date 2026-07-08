# CRM/客服/人力域报表确定性数值期望值表（1045-2 数据驱动渲染断言）

> Owner: `docs/plans/2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md` Phase 1 Exit Criteria
> 数据基线: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{crm,cs,hr}_*.csv`（12 CSV，1045-1 CRM 5 表 8 行 + CS 3 表 5 行 + HR 4 表 7 行）+ 跨域加性追加（`erp_md_partner` +1 EMPLOYEE 行 / `erp_fin_ar_ap_item` +2 EMPLOYEE_ADVANCE/EXPENSE_CLAIM·OPEN 行）
> 聚合口径源: `ErpCrmReportBizModel`（`buildLeadConversionFunnelDataset` :192 / `buildForecastAccuracyDataset(Long)` :225）/ `ErpCsReportBizModel`（`buildTicketSlaCsatSummaryDataset(String)` :184）/ `ErpHrReportBizModel`（`buildEmployeeNetBalanceDataset` :214 / `buildPayrollSimulationComparisonDataset(Long)` :282）
> 范式承接: `docs/analysis/2026-07-09-0930-3-mfg-mnt-qa-kpi-expected-values.md`（报表 `assertReportRenderedWithValue` 数值断言范式）

## 0. 确定性前提

- Playwright webServer 每次启动前 `rm -f db/erp.mv.db db/erp.trace.db`（fresh-DB 重置）+ `-Dnop.orm.init-database-data=true`，84 CSV 按拓扑序确定性插入。
- CRM/CS/HR 三域为**纯报表域（无看板 BizModel）**，5 报表读域表非 GL（区别于 0930-3 各域含看板 KPI 断言）。故本表仅含报表渲染数值断言，无看板 KPI 断言。
- **日期漂移裁决**：三域报表 `buildXxxDataset` **均无日期区间过滤**（区别于 mfg/mnt/qa 看板）。CRM lead-conversion-funnel 零参 `findAll()`；CRM forecast-accuracy 仅 forecastId 过滤；CS ticket-sla-csat-summary 仅 ticketType 过滤（SLA 经 `isSlaCompleted` 内存派生）；HR employee-net-balance 零参（聚合 OPEN/PARTIAL ar_ap_item）；HR payroll-simulation-comparison 强制 simulationId。故 spec **无需传 startDate/endDate**，断言天然不依赖运行时日期（确定性来自 seed 行本身，镜像 0930-3 范式约束）。
- **数值格式**：报表模板 `num`/`alert` 单元格 `numberFormat=#,##0.00`（千分位 + 2 位小数）；`cell`/`header`/`title` 用 `General`（原样文本）。helper `assertReportRenderedWithValue` 剥离千分位逗号后匹配，故期望 token 统一写去逗号形式（如 `50000.00`）。
- **seed 漂移同步机制**：若 1045-1 三域 seed CSV 变更（行增减/金额改动/枚举改动），本表期望 token 须同步更新，对应 `*.value.spec.ts` 的 `expectedTokens` 亦须同步（与 1445-2/2210-2/0930-3 同机制）。

## 1. 种子数据关键行（期望值派生依据）

### 1.1 CRM（`erp_crm_*.csv`，1045-1）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| erp_crm_stage | id=1 STAGE-001 | STAGE_NAME=验证 / SEQUENCE=10 / DEFAULT_PROBABILITY=30 | — |
| erp_crm_stage | id=2 STAGE-002 | STAGE_NAME=报价 / SEQUENCE=20 / DEFAULT_PROBABILITY=60 | — |
| erp_crm_lead | id=1 LEAD-2026-001 | STAGE_ID=1 / EXPECTED_REVENUE=50000.0000 / DOC_STATUS=QUALIFIED | — |
| erp_crm_lead | id=2 LEAD-2026-002 | STAGE_ID=2 / EXPECTED_REVENUE=80000.0000 / DOC_STATUS=QUALIFIED | — |
| erp_crm_forecast_period | id=1 FP-2026-Q3 | PERIOD_TYPE=QUARTERLY / PERIOD_START=2026-07-01 / STATUS=OPEN | — |
| erp_crm_forecast | id=1（无 code） | PERIOD_ID=1 / COMMIT_AMOUNT=50000.0000 / WEIGHTED_AMOUNT=45000.0000 / BEST_CASE_AMOUNT=80000.0000 / EXPECTED_CLOSED_REVENUE=0 / OPPORTUNITY_COUNT=2 | — |
| erp_crm_forecast_line | id=1 | FORECAST_ID=1 / LEAD_ID=1 / WEIGHTED_REVENUE=15000.0000 / FORECAST_CATEGORY=COMMIT | — |
| erp_crm_forecast_line | id=2 | FORECAST_ID=1 / LEAD_ID=2 / WEIGHTED_REVENUE=48000.0000 / FORECAST_CATEGORY=BEST_CASE | — |

### 1.2 CS（`erp_cs_*.csv`，1045-1）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| erp_cs_ticket_type | id=1 TT-COMPLAINT | NAME=投诉 / DEFAULT_PRIORITY=HIGH | — |
| erp_cs_ticket_type | id=2 TT-INQUIRY | NAME=咨询 / DEFAULT_PRIORITY=NORMAL | — |
| erp_cs_ticket | id=1 TKT-2026-001 | TICKET_TYPE_ID=1 / IS_SLA_COMPLETED=true / STATUS=RESOLVED / BUSINESS_DATE=2026-07-03 | — |
| erp_cs_ticket | id=2 TKT-2026-002 | TICKET_TYPE_ID=2 / IS_SLA_COMPLETED=false / STATUS=IN_PROGRESS / BUSINESS_DATE=2026-07-04 | — |
| erp_cs_survey | id=1 | TICKET_ID=1 / CSAT_SCORE=5 / NPS_SCORE=9 / SURVEY_CHANNEL=EMAIL | — |

### 1.3 HR（`erp_hr_*.csv` + 跨域加性追加，1045-1）

| 表 | 行 | 关键列 | 值 |
|----|----|--------|-----|
| erp_hr_department | id=1 DEPT-SALES | NAME=销售部 | — |
| erp_hr_department | id=2 DEPT-CS | NAME=客服部 | — |
| erp_hr_employee | id=1 HR-EMP-001 | FULL_NAME=赵明 / DEPARTMENT_ID=1 | — |
| erp_hr_employee | id=2 HR-EMP-002 | FULL_NAME=钱华 / DEPARTMENT_ID=2 | — |
| erp_hr_salary_simulation | id=1 SIM-2026-001 | SIMULATION_PERIOD_YEAR=2026 / MONTH=7 / STATUS=DRAFT | — |
| erp_hr_salary_simulation_item_adj | id=1 | SIMULATION_ID=1 / EMPLOYEE_ID=1 / SALARY_ITEM_CODE=BASE_SALARY / ORIGINAL_AMOUNT=10000.00 / ADJUSTED_AMOUNT=11000.00 | — |
| erp_hr_salary_simulation_item_adj | id=2 | SIMULATION_ID=1 / EMPLOYEE_ID=2 / SALARY_ITEM_CODE=BASE_SALARY / ORIGINAL_AMOUNT=8000.00 / ADJUSTED_AMOUNT=8500.00 | — |
| erp_md_partner（追加） | id=5 EMP-PTN-001 | NAME=张三员工往来 / PARTNER_TYPE=EMPLOYEE | — |
| erp_fin_ar_ap_item（追加） | id=5 ARAP-EA-001 | DIRECTION=RECEIVABLE / PARTNER_ID=5 / SOURCE_BILL_TYPE=EMPLOYEE_ADVANCE / OPEN_AMOUNT_FUNCTIONAL=1000.00 / STATUS=OPEN | — |
| erp_fin_ar_ap_item（追加） | id=6 ARAP-EC-001 | DIRECTION=PAYABLE / PARTNER_ID=5 / SOURCE_BILL_TYPE=EXPENSE_CLAIM / OPEN_AMOUNT_FUNCTIONAL=300.00 / STATUS=OPEN | — |

## 2. variables 选择与确定性裁决（Phase 1 item 2）

| 报表 | 入参签名 | variables 选择 | 依据 |
|------|---------|---------------|------|
| lead-conversion-funnel | `buildLeadConversionFunnelDataset()` 零参 | 无 variables（仅 `reportName`） | `findAll()` 读 lead 零过滤，确定性来自 seed 行 |
| forecast-accuracy | `buildForecastAccuracyDataset(Long forecastId)` | `data:{forecastId:"1"}` | 传范围内 forecast id 锁定单 forecast，避免多 forecast 干扰（镜像 fin-income-statement `data:{periodId}` 范式） |
| ticket-sla-csat-summary | `buildTicketSlaCsatSummaryDataset(String ticketType)` | `data:{ticketType:"1"}` | 传范围内 ticket_type id 锁定单类型，使 `ticketTypeName=投诉` 确定性非「(全部)」桶 |
| employee-net-balance | `buildEmployeeNetBalanceDataset()` 零参 | 无 variables（仅 `reportName`） | 聚合全 OPEN/PARTIAL ar_ap_item，确定性来自 seed 行 |
| payroll-simulation-comparison | `buildPayrollSimulationComparisonDataset(Long simulationId, ctx)` | `data:{simulationId:"1"}` | **simulationId 强制入参**（null 返回空集），传范围内 simulation id |

> GraphQL 范式（`data` 内联 map + `$xxx:BigDecimal`）：镜像 `fin-income-statement.value.spec.ts`（`query($reportName:String!,$periodId:BigDecimal){ ErpFinReport__renderHtml(reportName:$reportName,data:{periodId:$periodId}) }`）。

## 3. 报表渲染期望 token（`assertReportRenderedWithValue`）

### 3.1 CRM lead-conversion-funnel `ErpCrmReport__renderHtml(reportName="lead-conversion-funnel")`

口径（`buildLeadConversionFunnelDataset` :192）：`findAll()` 读 lead，按 `stageId!=null` 分组聚合 `leadCount` + `expectedRevenue` 合计；`stageName` 经 stage.id 批量解析。

| stageId | stageName | leadCount | expectedRevenue |
|---------|-----------|-----------|-----------------|
| 1 | 验证 | 1 | 50000.00 |
| 2 | 报价 | 1 | 80000.00 |
| 合计 | — | 2 | 130000.00 |

| token（去千分位） | 派生 |
|------------------|------|
| `线索转化漏斗表` | 报表 title（template `<value>线索转化漏斗表</value>`） |
| `验证` | stage 1 stageName |
| `报价` | stage 2 stageName |
| `50000.00` | stage 1 expectedRevenue（`#,##0.00` → `50,000.00` → 去逗号） |
| `80000.00` | stage 2 expectedRevenue |

### 3.2 CRM forecast-accuracy `ErpCrmReport__renderHtml(reportName="forecast-accuracy", data:{forecastId:"1"})`

口径（`buildForecastAccuracyDataset(1)` :225）：`findAllByQuery(id=1)` 读 forecast + `findAllByQuery(forecastId IN [1])` 读 forecast_line 聚合 `lineCount` + `lineWeightedRevenue`（Σ weightedRevenue）。

| 字段 | 值 | 派生 |
|------|----|------|
| forecastId | 1 | seed forecast.id=1 |
| commitAmount | 50000.00 | forecast.COMMIT_AMOUNT |
| weightedAmount | 45000.00 | forecast.WEIGHTED_AMOUNT |
| bestCaseAmount | 80000.00 | forecast.BEST_CASE_AMOUNT |
| opportunityCount | 2 | forecast.OPPORTUNITY_COUNT |
| lineCount | 2 | 2 forecast_line 行 |
| lineWeightedRevenue | 63000.00 | 15000 + 48000 |

| token（去千分位） | 派生 |
|------------------|------|
| `销售预测准确率表` | 报表 title |
| `50000.00` | commitAmount |
| `45000.00` | weightedAmount |
| `80000.00` | bestCaseAmount |
| `63000.00` | lineWeightedRevenue |

### 3.3 CS ticket-sla-csat-summary `ErpCsReport__renderHtml(reportName="ticket-sla-csat-summary", data:{ticketType:"1"})`

口径（`buildTicketSlaCsatSummaryDataset("1")` :184）：`findAllByQuery(ticketTypeId=1)` 读 ticket（仅 ticket 1），按 ticketTypeId 聚合 `totalTickets`/`slaCompletedCount`(isSlaCompleted=true)/`slaBreachedCount`(false) + survey（ticketId=1）csat/nps 均值；`ticketTypeName` 经 ticket_type.id 解析。

| ticketTypeId | ticketTypeName | totalTickets | slaCompleted | slaBreached | surveyCount | avgCsat | avgNps |
|-------------|----------------|--------------|--------------|-------------|-------------|---------|--------|
| 1 | 投诉 | 1 | 1 | 0 | 1 | 5.00 | 9.00 |

avgCsat = 5/1 = 5.00（scale 2 HALF_UP）；avgNps = 9/1 = 9.00。

| token（去千分位） | 派生 |
|------------------|------|
| `工单 SLA/CSAT 综合统计表` | 报表 title |
| `投诉` | ticketTypeId=1 ticketTypeName |
| `5.00` | avgCsat（csatSum 5 ÷ surveyCount 1） |
| `9.00` | avgNps（npsSum 9 ÷ surveyCount 1） |

### 3.4 HR employee-net-balance `ErpHrReport__renderHtml(reportName="employee-net-balance")`

口径（`buildEmployeeNetBalanceDataset` :214）：跨域经 `IErpFinArApItemBiz.findOpenItems(RECEIVABLE)` + 内存按 `sourceBillType=EMPLOYEE_ADVANCE` 二次过滤得 advanceByPartner；`findOpenItems(PAYABLE)` + `EXPENSE_CLAIM` 得 expenseByPartner；netBalance = advance − expense；partnerName 经 md_partner 解析。

| partnerId | partnerName | advanceBalance | expenseBalance | netBalance | netDirection |
|-----------|-------------|----------------|----------------|------------|--------------|
| 5 | 张三员工往来 | 1000.00 | 300.00 | 700.00 | 员工欠公司 |

netDirection = net.signum()>0 → 「员工欠公司」。

| token（去千分位） | 派生 |
|------------------|------|
| `员工净余额报表` | 报表 title（`员工净余额报表（预支 − 报销）` 子串） |
| `张三员工往来` | partner 5 partnerName |
| `员工欠公司` | netDirection（net=700>0） |
| `1000.00` | advanceBalance（EMPLOYEE_ADVANCE/RECEIVABLE/OPEN openAmountFunctional） |
| `700.00` | netBalance（1000 − 300） |

### 3.5 HR payroll-simulation-comparison `ErpHrReport__renderHtml(reportName="payroll-simulation-comparison", data:{simulationId:"1"})`

口径（`buildPayrollSimulationComparisonDataset(1)` :282）：`findAllByQuery(simulationId=1)` 读 simulation_item_adj 按 employeeId/salaryItemCode 升序 → 2 DETAIL 行；difference = adjusted − original；按 departmentId 聚合 DEPT_SUBTOTAL 行（HashMap 序，值确定序非确定，但两值均出现）。

| rowType | employeeName | departmentId | salaryItemCode | original | adjusted | difference |
|---------|--------------|--------------|----------------|----------|----------|------------|
| DETAIL | 赵明 | 1 | BASE_SALARY | 10000.00 | 11000.00 | 1000.00 |
| DETAIL | 钱华 | 2 | BASE_SALARY | 8000.00 | 8500.00 | 500.00 |
| DEPT_SUBTOTAL | 部门小计 | 1 | — | — | — | 1000.00 |
| DEPT_SUBTOTAL | 部门小计 | 2 | — | — | — | 500.00 |

| token（去千分位） | 派生 |
|------------------|------|
| `薪酬模拟对比报表` | 报表 title（`薪酬模拟对比报表（源 vs 模拟）` 子串） |
| `赵明` | emp 1 fullName |
| `钱华` | emp 2 fullName |
| `部门小计` | DEPT_SUBTOTAL 行 employeeName |
| `BASE_SALARY` | salaryItemCode |
| `10000.00` | emp 1 originalAmount |
| `11000.00` | emp 1 adjustedAmount |

## 4. spec 路由与 query 汇总

| spec 文件 | route | responseKey | variables |
|-----------|-------|-------------|-----------|
| `crm-lead-conversion-funnel.value.spec.ts` | `/lead-conversion-funnel` | `ErpCrmReport__renderHtml` | `{ reportName: 'lead-conversion-funnel' }` |
| `crm-forecast-accuracy.value.spec.ts` | `/forecast-accuracy` | `ErpCrmReport__renderHtml` | `{ reportName: 'forecast-accuracy', forecastId: '1' }` |
| `cs-ticket-sla-csat.value.spec.ts` | `/ticket-sla-csat-summary` | `ErpCsReport__renderHtml` | `{ reportName: 'ticket-sla-csat-summary', ticketType: '1' }` |
| `hr-employee-net-balance.value.spec.ts` | `/employee-net-balance` | `ErpHrReport__renderHtml` | `{ reportName: 'employee-net-balance' }` |
| `hr-payroll-simulation-comparison.value.spec.ts` | `/payroll-simulation-comparison` | `ErpHrReport__renderHtml` | `{ reportName: 'payroll-simulation-comparison', simulationId: '1' }` |
