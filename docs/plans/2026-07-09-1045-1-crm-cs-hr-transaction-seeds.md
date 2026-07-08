# 2026-07-09-1045-1-crm-cs-hr-transaction-seeds CRM/客服/人力域交易单据种子数据

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md` Deferred「其他扩展域交易种子（...CRM/CS/HR/logistics/b2b/contract/drp/aps）」中 **CRM/CS/HR 子集**（Successor Required: yes，触发条件「当对应扩展域看板/报表端到端数值回归需交易数据时，按域逐批补 seed」——**已满足**：CRM/CS/HR 域 5 张报表 `ErpCrmReportBizModel`/`ErpCsReportBizModel`/`ErpHrReportBizModel` 已就绪但缺数据）；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-2210-1-operational-domain-transaction-seeds.md`（completed，运营域种子范式 + 本计划数据层前置）、`docs/plans/2026-07-09-0930-1-manufacturing-transaction-seeds.md`（completed，单域种子范式）、`docs/plans/2026-07-09-0930-2-maintenance-quality-transaction-seeds.md`（completed，跨两域种子范式，本计划最贴近镜像）、`docs/plans/2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md`（同批 N=2 断言层后继，本计划数据层前置）
> Audit: required

## Current Baseline

实时仓库逐项核实（`ls`/`rg`/`read`，非采信旧记忆）：

- **既有种子库（72 CSV）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 含 21 主数据（1234-1）+ 23 P2P/O2C（1445-1）+ 13 运营域（2210-1：库存/资产/项目）+ 4 制造域（0930-1）+ 11 维护+质量域（0930-2）。**CRM/CS/HR 域零 CSV**（`ls _init-data/` 核实无 `erp_crm_*`/`erp_cs_*`/`erp_hr_*` 文件）。
- **CRM/CS/HR 域为纯报表域（无看板）**：glob 全三 service 模块，`ErpCrmDashboardBizModel`/`ErpCsDashboardBizModel`/`ErpHrDashboardBizModel` **均不存在**（区别于 finance/mfg/mnt/qa 等有看板的域）。三域各 1 个 `ErpXxxReportBizModel`，共 **5 张报表**：
  - CRM（模板根 `/nop/main/report/crm/`）：`lead-conversion-funnel`（`ErpCrmReportBizModel.java:192`）+ `forecast-accuracy`（:225）。
  - CS（模板根 `/nop/main/report/cs/`）：`ticket-sla-csat-summary`（`ErpCsReportBizModel.java:184`）。
  - HR（模板根 `/nop/main/report/hr/`）：`employee-net-balance`（`ErpHrReportBizModel.java:214`）+ `payroll-simulation-comparison`（:282）。
- **报表读域表非 GL（逐方法核实 entity 读源）**：
  - CRM `buildLeadConversionFunnelDataset`(:192)：`findAll()` 读 `erp_crm_lead`（按 `stageId` 非 null 聚合 leadCount/expectedRevenue），再 `findAll()` 读 `erp_crm_stage`（解析 stageName）。**零 query 过滤**（无日期/status 过滤，仅 `stageId!=null` 跳过）。
  - CRM `buildForecastAccuracyDataset(Long forecastId)`(:225)：`findAllByQuery` 读 `erp_crm_forecast`（periodId mandatory FK→`erp_crm_forecast_period`，可选 forecastId 过滤）+ `erp_crm_forecast_line`（forecastId mandatory FK→forecast，**leadId mandatory FK→`erp_crm_lead`**，按 forecastId 聚合 commitAmount/weightedAmount/bestCaseAmount）。**零 status/日期过滤**。
  - CS `buildTicketSlaCsatSummaryDataset(String ticketType)`(:184)：`findAllByQuery` 读 `erp_cs_ticket`（customerId mandatory FK→partner，ticketTypeId mandatory FK→`erp_cs_ticket_type`，可选 ticketType 过滤；SLA 命中经布尔列 `isSlaCompleted` 内存派生，**无 status/docStatus 过滤**）+ `erp_cs_survey`（ticketId mandatory FK→ticket，csatScore/npsScore 经 `orm_propValueByName` 读取，null 经 nz 归零）+ `erp_cs_ticket_type`（解析 ticketTypeName）。
  - HR `buildEmployeeNetBalanceDataset`(:214)：**唯一跨域读取**——经注入 biz `IErpFinArApItemBiz.findOpenItems(direction,context)`（`ErpFinArApItemBizModel.java:52`，过滤 `direction` + `status IN [OPEN,PARTIAL]`）读 finance `erp_fin_ar_ap_item`，再内存按 `sourceBillType` 二次过滤：预支余额=`direction=RECEIVABLE`+`sourceBillType=EMPLOYEE_ADVANCE`、报销余额=`direction=PAYABLE`+`sourceBillType=EXPENSE_CLAIM`，按 partnerId 汇总 openAmountFunctional；再 `findAllByQuery` 读 `erp_md_partner` 解析姓名（partnerId=null 跳过）。
  - HR `buildPayrollSimulationComparisonDataset(Long simulationId,context)`(:282)：**simulationId 为强制入参**（null 直接返回空集 :283）；`findAllByQuery` 读 `erp_hr_salary_simulation_item_adj`（simulationId+employeeId mandatory FK，按 employeeId/salaryItemCode 排序）+ `findAllByQuery` 读 `erp_hr_employee`（departmentId 驱动 `DEPT_SUBTOTAL` 聚合行）。employee 表需 `erp_hr_department` 存在以产出小计行。
- **三域报表当前数值为 0/空集**：CRM/CS/HR 域零 seed → 5 报表均空集（lead-conversion-funnel 无 lead；forecast-accuracy 无 forecast；ticket-sla-csat 无 ticket；payroll-simulation 无 simulation；employee-net-balance：`erp_fin_ar_ap_item` 4 行全 `SETTLED` 且 sourceBillType 为 AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT，**无 EMPLOYEE_ADVANCE/EXPENSE_CLAIM 且无 OPEN/PARTIAL** → `findOpenItems` 返回空）。
- **最小 seed 表集（经数据源反推）**：
  - CRM：`erp_crm_stage`（lead 聚合分组键 + 名解析）+ `erp_crm_lead`（funnel PRIMARY，≥1 行 stageId 非 null）+ `erp_crm_forecast_period`（forecast.periodId mandatory FK）+ `erp_crm_forecast`（forecast-accuracy 头）+ `erp_crm_forecast_line`（forecast-accuracy 行，forecastId+leadId mandatory FK）。
  - CS：`erp_cs_ticket_type`（ticket.ticketTypeId mandatory FK + 名解析）+ `erp_cs_ticket`（customerId mandatory FK→partner，PRIMARY）+ `erp_cs_survey`（ticketId mandatory FK，csatScore/npsScore 非空以驱动 avg 非零）。
  - HR：`erp_hr_department`（employee.departmentId + 小计行）+ `erp_hr_employee`（payroll-sim employeeId FK；**注意**：1234-1 seed 的是 master-data `erp_md_employee` IDs 1-3，HR 域 `erp_hr_employee` **未 seed**，须新建）+ `erp_hr_salary_simulation`（payroll-sim 头，simulation_item_adj.simulationId mandatory FK）+ `erp_hr_salary_simulation_item_adj`（payroll-sim 行）+ **跨域 finance 扩展**：`erp_fin_ar_ap_item` 追加 `EMPLOYEE_ADVANCE`(RECEIVABLE)/`EXPENSE_CLAIM`(PAYABLE) 行（status=OPEN，partnerId 指向员工型 partner）+ **跨域 master-data 扩展**：`erp_md_partner` 追加员工型 partner 行（PARTNER_TYPE=EMPLOYEE，供 ar_ap_item.partnerId 引用 + 姓名解析）。
- **种子范式已建立（2210-1/0930-1/0930-2 交付）**：列名=ORM 列 `code`/`ID` 显式提供（FK 跨表）/省略审计列+TENANT_ID/布尔小写 `true`/`false`/日期 `YYYY-MM-DD`/posted 统一 false（报表读域表非 GL）/加载拓扑序经 ORM `getEntityModelsInTopoOrder()` 自动排序/域内金额自洽。落盘范式见 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`。
- **可复用固定主数据 ID**（1234-1/1445-1/2210-1 已 seed，`erp_md_*.csv` 核实）：orgId=2（ERP-CO）/currencyId=1（CNY）/acctSchemaId=1/periodId=1（会计期间 OPEN）/partner IDs 1-2（CUSTOMER CUST-001/002）+3-4（SUPPLIER SUP-001/002）/material IDs 1-4/uom IDs 1-4/warehouse 1-2/employee IDs 1-3（`erp_md_employee` EMP-001~003，**非** erp_hr_employee）。
- **CRM/CS/HR 域报表相关头实体已有标准字段**（model/*.orm.xml，Phase 1 逐表核实）：CS `ErpCsTicket` 含 `ORG_ID`+`BUSINESS_DATE`+`docStatus`+`approveStatus`；CRM/HR 报表读源实体（lead/forecast/ticket/simulation/ar_ap_item）是否含 `posted`/`businessDate` 由 Phase 1 核实（不影响报表读源，posted 非报表过滤列）。这些域**不在** AGENTS.md「7 域 posted/businessDate ask-first blocked」集合内（该集合经 0056-1 已完成）。
- **保护区域**：纯部署期数据（新增 CSV + 2 处既有 CSV 加性追加）+ 分析文档。**零 `*.orm.xml`/`*.xbiz.xml`/`*.page.yaml`/`*.view.xml`/Java 生产代码变更**（镜像 0930-2）。属 `plan-first`（跨三域 FK + 拓扑序 + HR 跨域 finance/master-data 扩展裁决 + 涉及 >5 文件）。

剩余差距：(1) CRM 2 报表空集（缺 stage/lead/forecast 链）；(2) CS 报表空集（缺 ticket_type/ticket/survey）；(3) HR payroll-sim 空集（缺 department/employee/simulation 链）+ employee-net-balance 空集（缺 EMPLOYEE_ADVANCE/EXPENSE_CLAIM ar_ap_item + 员工 partner）；(4) 三域 seed 表映射分析文档未派生。

## Goals

- 在 72 CSV 基础上新增 CRM/CS/HR 域最小连通种子集，使三域 **5 张报表数值转非空可观测**：CRM lead-conversion-funnel + forecast-accuracy；CS ticket-sla-csat-summary；HR payroll-simulation-comparison + employee-net-balance。
- seed 引用 1234-1/1445-1 固定主数据 ID，域内金额/计数自洽，列名严格对齐 ORM `code`。HR 跨域 finance ar_ap_item 行追加 `EMPLOYEE_ADVANCE`/`EXPENSE_CLAIM` sourceBillType（status=OPEN），master-data `erp_md_partner` 追加员工型 partner 行，供 employee-net-balance 驱动。
- 落盘 CRM/CS/HR seed 表映射分析文档（表清单 + 列角色 M/FK/opt + 拓扑序 + 范围裁决 + HR 跨域扩展裁决 + 域内金额自洽约束）。
- 解除 2210-1 Deferred「其他扩展域交易种子（CRM/CS/HR 子集）」。

## Non-Goals

- **不** seed GL 凭证/业财一体——三域报表读域表（lead/forecast/ticket/simulation）/finance ar_ap_item 状态列，非 GL/Voucher。HR ar_ap_item 行作为可观测独立行插入（无凭证回链，报表只读 ar_ap_item.status/sourceBillType/direction/openAmountFunctional）；触发条件：CRM/CS/HR 域业财一体端到端数值回归需 GL 串联时。
- **不** seed 三域看板——CRM/CS/HR **无看板 BizModel**（基线已核实），无看板可 seed。
- **不**做精确 KPI/报表数值断言——本计划解除「数据存在」阻塞（报表非空可观测）；精确断言是 N=2 `2026-07-09-1045-2` 后继层。
- **不** seed 其他扩展域（logistics/b2b/contract/drp/aps/master-data）——logistics/b2b/contract/drp/aps 无看板无报表（seed 不解除任何阻塞），master-data 看板/报表读源已由 1234-1 主数据 seed（值已非空，归独立 successor）；触发条件：对应域端到端数值回归需交易数据时。
- **不** seed CRM 配置表（product_config_rule/price_rule/bundle_pricing 等）/CS 知识库/entitlement/catalog/SLA 策略/HR 薪酬/休假/考勤/排班/胜任力主表——这些表不被范围内 5 报表 `QueryBean` 直接读（lead/forecast/ticket/simulation 非强制配置 FK 可留 null）；按需 successor。
- **不**改后端 `@BizQuery`/报表模板/ORM——纯部署数据层（镜像 0930-2）。

## Task Route

- Type: `implementation-only change`（纯部署期种子 CSV + 分析文档，零生产代码变更）
- Owner Docs: `docs/architecture/seed-data.md`（种子范式 + Non-Goals 段）、`docs/testing/e2e-runbook.md`（种子库启动段 + 域清单）、`docs/design/crm/README.md`（CRM 报表口径）、`docs/design/customer-service/README.md`（CS 报表口径）、`docs/design/human-resource/payroll-simulation.md`（HR payroll-sim 口径）、`docs/design/finance/expense-claim.md`（员工借款/报销→ar_ap_item 口径，位于 finance 域）、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`（0930-2 范式，本计划镜像）
- Skill Selection Basis: `none`——可用技能集（nop-backend-dev=BizModel 逻辑 / nop-frontend-dev=AMIS view.xml/page.yaml 页面 / nop-testing=Java JunitAutoTestCase + Playwright E2E / nop-debugging / nop-git-master / deep-interview）均不覆盖部署期 CSV 种子编写（数据建模 + 列映射 + FK 拓扑，非后端逻辑/前端页面/E2E 测试）。Phase 3 GraphQL 抽样验证属数据可观测性确认，不涉及 BizModel 方法编写，仍无匹配技能。镜像 0930-2 口径。
- Bundling 裁决（规则 14）：CRM/CS/HR 分属不同 module/不同 owner-doc，严格说不满足「同一 owner doc」；但三者 (a) 共享完全相同的种子范式（镜像 0930-2 域表直 seed）、(b) 共享同一验证路径（fresh-DB + GraphQL 非空 + E2E 0 回归）、(c) 共享同一后继 N=2 断言层、(d) 均为纯报表域（无看板）。拆分会产生三个近乎复制的微计划，正是规则 14 欲避免的 clutter。HR 的跨域 finance/master-data 扩展作为该批次内的显式 Decision（非独立结果表面）承载。结果表面类型一致（交易种子解除报表空集阻塞），故合并为一个 plan。

## Infrastructure And Config Prereqs

- 既有 72 CSV 种子库已落地（CRM/CS/HR 域可读取）——前置已满足。
- 平台 `DataInitInitializer` + `-Dnop.orm.init-database-data=true` + fresh-DB 重置（webServer 已含 `rm -f db/erp.mv.db`）——无需改。
- 回滚策略：纯新增 CSV + 2 处既有 CSV 加性追加 + 分析文档，删除/回退追加即回滚。

## Execution Plan

### Phase 1 - CRM/CS/HR 域 seed 表映射 + 范围裁决（Proof + Decision）

Status: completed
Targets: `module-crm/model/app-erp-crm.orm.xml`、`module-cs/model/app-erp-cs.orm.xml`、`module-hr/model/app-erp-hr.orm.xml`、`module-finance/model/app-erp-finance.orm.xml`（`ErpFinArApItem`）、`module-master-data/model/app-erp-master-data.orm.xml`（`ErpMdPartner`）、`docs/design/crm/README.md`、`docs/design/finance/expense-claim.md`、`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`
Skill: `none`

- Item Types: `Proof | Decision`
- Prereqs: 既有 72 CSV 种子库（CRM/CS/HR 域可读取）

- [x] `Proof`：逐表派生列映射——读 CRM（stage/lead/forecast_period/forecast/forecast_line）+ CS（ticket_type/ticket/survey）+ HR（department/employee/salary_simulation/salary_simulation_item_adj）+ finance（ar_ap_item 既有列）+ master-data（partner 既有列）ORM，标注每列角色（M=mandatory/FK=引用已 seed 主数据 ID/opt=可选留空），核实 mandatory 业务列全可填、FK 全指向已 seed 主数据（org=2/currency=1/acctSchema=1/period=1/partner 1-4/material 1-4）或范围内更低拓扑表（forecast_line.forecastId→forecast / forecast_line.leadId→lead / simulation_item_adj.simulationId→simulation / simulation_item_adj.employeeId→employee / ar_ap_item 既有列对齐 1445-1）。核实 dict code（crm: lead-type/lead-doc-status/forecast-status/forecast-category；cs: ticket-priority/ticket-status/doc-status/wf-approve-status；hr: gender/employment-status/employee-type）对齐 dict.yaml。核实 `erp_hr_employee` 确未 seed（区别 `erp_md_employee`）、`erp_fin_ar_ap_item` 无 EMPLOYEE_ADVANCE/EXPENSE_CLAIM 行、`erp_md_partner` 无 EMPLOYEE 类型行。产出 seed 表映射分析文档（`docs/analysis/2026-07-09-1045-1-crm-cs-hr-seed-table-map.md`）。
      - Skill: `none`
- [x] `Decision`：seed 范围 + HR 跨域扩展 + posted 裁决——(a) 范围 Decision：CRM 5 表 + CS 3 表 + HR 4 表（department/employee/salary_simulation/simulation_item_adj）为最小集（考虑替代方案「含配置表」vs「仅报表读源」+ 残留风险：配置表不被报表读，徒增参照复杂度 → 选仅报表读源）。(b) **HR 跨域扩展 Decision**：employee-net-balance 需 `erp_fin_ar_ap_item` 含 EMPLOYEE_ADVANCE(RECEIVABLE)/EXPENSE_CLAIM(PAYABLE)+status=OPEN 行（考虑替代方案 A「追加 ar_ap_item + 追加员工型 partner」vs B「employee-net-balance 归 Deferred」+ 残留风险：A 触及 finance+master-data 既有 CSV 加性追加但解除报表阻塞 / B 致 HR 2 报表之一仍空 → 选 A，加性追加不破坏既有行，partner 追加 EMPLOYEE 类型行对齐 `docs/design/finance/expense-claim.md` 员工-as-partner 设计）。(c) posted Decision：CRM/CS/HR 新增域表统一 `posted=false`（报表读域表非 GL，镜像 0930-2）；HR ar_ap_item 追加行 posted 沿用 1445-1 既有 ar_ap_item 列约定（报表只读 status/sourceBillType/direction，posted 非过滤列，Phase 1 核实既有列是否含 posted 后定）。(d) ID 分配 Decision：新表 ID 自 1 起；既有表追加行 ID 接续（partner 自 5 起，ar_ap_item 自 5 起）。本项记录每表关键日期/status/dict/跨域 ID 选择依据。
      - Skill: `none`

Exit Criteria:

- [x] CRM/CS/HR seed 表映射分析文档落盘，每表标注列角色 M/FK/opt + FK 目标 ID + dict code 核实 + 拓扑序 + 范围/HR 跨域扩展/posted/ID Decision + 残留风险。

### Phase 2 - CRM/CS/HR 域 seed CSV 编写（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{crm,cs,hr}_*.csv` + 追加 `erp_md_partner.csv` + `erp_fin_ar_ap_item.csv`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1 表映射 + 裁决

- [x] `Add`：编写 CRM CSV（stage ≥1 + lead ≥2 覆盖不同 stageId 驱动 funnel leadCount + forecast_period ≥1 + forecast ≥1 + forecast_line ≥1 leadId 指向范围内 lead）；CS CSV（ticket_type ≥1 + ticket ≥2 含不同 ticketTypeId + isSlaCompleted true/false 混合驱动 SLA 命中率 + survey ≥1 csatScore/npsScore 非空驱动 avg 非零，customerId 指向 partner 1/2 customer）；HR CSV（department ≥1 + employee ≥2 不同 departmentId 驱动小计行 + salary_simulation ≥1 + simulation_item_adj ≥2 employeeId 指向范围内 employee salaryItemCode 区分）；HR 跨域追加（`erp_md_partner` 追加 ≥1 PARTNER_TYPE=EMPLOYEE 行 id≥5；`erp_fin_ar_ap_item` 追加 ≥2 行：1 行 EMPLOYEE_ADVANCE/RECEIVABLE/OPEN + 1 行 EXPENSE_CLAIM/PAYABLE/OPEN，partnerId 指向员工 partner，openAmountFunctional 非零使 net 非零）。列名严格对齐 ORM `code`（`ID` 显式、省略审计+TENANT_ID、布尔小写、日期 `YYYY-MM-DD`），FK 引用固定 ID，域内金额自洽（ar_ap_item openAmount=amount-status=OPEN 全额未结；simulation_item_adj originalAmount/adjustedAmount 差值自洽）。
      - Skill: `none`

Exit Criteria:

- [x] CRM/CS/HR 新增 + 追加 CSV 落地 `_vfs/_init-data/`，列名经脚本逐表对齐 ORM `code`（0 错配），mandatory 业务列全填，FK 全指向已 seed ID，既有 CSV 仅加性追加（不改动既有行）。

### Phase 3 - fresh-DB seed 加载 + GraphQL 非空验证 + E2E 0 回归（Proof）

Status: completed
Targets: fresh-DB 启动、`/graphql` 抽样
Skill: `none`

- Item Types: `Proof`
- Prereqs: Phase 2 CSV 落地

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块，确认新 CSV 打包入 runner jar 无后端污染）+ fresh-DB 启动（删 `db/erp.mv.db` + `-Dnop.orm.init-database-data=true`）确认 72+N CSV 全 `load-csv-data` 成功（0 主键冲突 / 0 列映射错误 / 0 参照完整性失败）+ GraphQL 抽样 `ErpCrmReport__renderHtml`(lead-conversion-funnel/forecast-accuracy) / `ErpCsReport__renderHtml`(ticket-sla-csat-summary) / `ErpHrReport__renderHtml`(payroll-simulation-comparison 传范围内 simulationId / employee-net-balance) 5 报表返回非空 HTML + `npx playwright test`（既有 spec 0 回归）。指定成功模式（5 报表非空/0 回归）与失败模式（列映射错误→回 Phase 2 修 CSV / FK 失败→回 Phase 2 修 ID / employee-net-balance 仍空→回 Phase 1 核实 sourceBillType/direction/status 三元组）。
      - Skill: `none`

Exit Criteria:

- [x] fresh-DB seed 加载 0 冲突 + GraphQL 抽样 CRM/CS/HR 5 报表由空集转非空可观测 + 既有 E2E spec 0 回归。

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 3 全绿

- [x] `Add`：`docs/architecture/seed-data.md` 增「CRM/CS/HR 域交易单据种子」段（域表直 seed 范式 + 表清单 + HR 跨域 finance/master-data 扩展裁决 + Non-Goals 更新移除 CRM/CS/HR）+ `docs/testing/e2e-runbook.md` 种子库启动段 CSV 计数 72→72+N + 域清单补 CRM/CS/HR 域（纯报表域）+ `docs/testing/known-good-baselines.md` 增 CRM/CS/HR 域种子基线行；2210-1 Deferred「其他扩展域交易种子（CRM/CS/HR 子集）」登记解除（本计划 Closure 段登记）。
      - Skill: `none`

Exit Criteria:

- [x] seed-data.md 含 CRM/CS/HR 域种子段；e2e-runbook + known-good-baselines 种子库计数/域同步；2210-1 Deferred CRM/CS/HR 子集登记解除（本计划 Closure 段登记）。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_0bcd69df7ffe0G13pixJ69ruBn`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线硬事实（72 CSV 计数/CRM-CS-HR 零 seed/无看板/5 报表数据源行号/FK mandatory/erp_hr_employee 未 seed/ar_ap_item 4 行全 SETTELD 无 EMPLOYEE_*/partner 无 EMPLOYEE 类型/logistics-b2b-contract-drp-aps 无看板无报表）逐项 live 验真 PASS。无 BLOCKER。2 MAJOR：(1) Phase 1 Targets `module-human-resource/model/app-erp-hr.orm.xml` 路径不存在，实际为 `module-hr/model/app-erp-hr.orm.xml` → **已修复**；(2) Owner Docs + Phase 1 Targets + Decision 引用 `docs/design/human-resource/expense-claim.md` 不存在，实际为 `docs/design/finance/expense-claim.md` → **已修复**（3 处）。Skill:none 裁决经独立复核与 0930-2 一致确认。bunding 裁决（规则 14）+ 跨计划一致性（2210-1 Deferred 名 CRM/CS/HR + 1045-2 后继存在）均 PASS。3 MINOR 非阻塞（P2P/O2C 标签松散/HR dict 列表非穷举/renderHtml action 名 Phase 1 确认）。
- Independent draft review iteration 2: accept (`ses_0bcd12848ffeeJnBD2aRfpj0li`，独立 general 子代理，新会话冷重播) — M1（module-hr 路径，live 核实 `module-hr/model/app-erp-hr.orm.xml` 存在）+ M2（`docs/design/finance/expense-claim.md` 路径，live 核实存在）修复跨所有出现处一致落地；括注清理落地。一致性扫描全 PASS（Status draft/phases planned/检查项未勾选/无松弛词/3 Deferred 均附触发条件/Closure Gates 未勾选）。零 ORM 编辑（ask-first 不适用，仅 CSV + 2 处加性追加 + docs）。无新增缺陷。**激活：draft → active**（草案审查收敛为可执行契约，可开始实施）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。本计划结果表面为部署期数据（CSV + 2 处既有 CSV 加性追加），无生产 Java 代码变更；验证门控以 fresh-DB seed 加载 + 既有 E2E 0 回归 + GraphQL 非空一致为主。

- [x] 范围内行为完成（CRM 5 表 + CS 3 表 + HR 4 表 + 跨域 2 追加种子 CSV 落地 + 5 报表数值非空）
- [x] 相关文档对齐（seed-data.md + e2e-runbook + known-good-baselines）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ fresh-DB seed 加载（0 冲突/0 列映射错误/0 参照失败）+ GraphQL 抽样 5 报表非空 + `npx playwright test`（既有 spec 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（GL 凭证 seed/看板(不存在)/其他扩展域/配置表/精确数值断言均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CRM/CS/HR 域 GL 凭证/业财一体 seed

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三域报表读域表（lead/forecast/ticket/simulation）/finance ar_ap_item 状态列，非 GL/Voucher；1234-1 种子科目表无 CRM/CS/HR 域专用科目。HR ar_ap_item 行作为可观测独立行（无凭证回链）。seed GL 凭证不解除额外阻塞。
- Successor Required: `yes`
- Trigger Condition: 当 CRM/CS/HR 域业财一体端到端数值回归需 GL 串联（凭证↔源单据↔辅助账）时。

### CRM/CS/HR 域配置/执行链 seed（CRM product_config_rule/price_rule/bundle_pricing；CS knowledge_base/entitlement/catalog/sla_policy；HR salary/leave/attendance/shift/competency 主表）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 这些表不被范围内 5 报表 `QueryBean` 直接读（lead/forecast/ticket/simulation 非强制配置 FK 可留 null）。seed 它们不解除报表数值阻塞。
- Successor Required: `yes`
- Trigger Condition: 当对应域配置/执行链端到端回归需这些数据，或 lead/ticket/simulation 需引用真实配置时。

### 精确 CRM/CS/HR 域报表数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划解除「数据存在」阻塞（报表非空可观测）；精确断言「funnel leadCount=X / SLA 命中率=Y」需固定种子集确定性 + 断言逻辑，是 N=2 successor 层。
- Successor Required: `yes`
- Trigger Condition: 本计划固化后，由 `2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md` 承接。

## Closure

Status Note: 全部 4 Phase 完成，8 Closure Gates 全绿。CRM/CS/HR 三域 12 张新表 CSV + 2 处既有 CSV 加性追加（md_partner +1 EMPLOYEE / fin_ar_ap_item +2 EMPLOYEE_ADVANCE·EXPENSE_CLAIM·OPEN）落地，5 报表由空集转非空可观测且金额自洽。零 ORM/契约/Java 变更（纯部署期数据 + 文档）。解除 2210-1 Deferred「其他扩展域交易种子（CRM/CS/HR 子集）」。验证全绿：mvn clean install -DskipTests（154 模块）+ fresh-DB 84 CSV 0 冲突 + GraphQL 5 报表非空 + npx playwright test 74 passed 0 回归。精确数值断言由 N=2 后继 `2026-07-09-1045-2` 承接。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理，新会话冷重播（无执行者上下文），task id `ses_0bcb3da8fffezJz0GYK8xRfQxd`
- Evidence: 纯文件证据审计（未重跑 mvn/playwright，确定性再派生）。`ls _vfs/_init-data/` → 84 CSV，12 张新 `erp_{crm,cs,hr}_*` 齐全；`erp_md_partner.csv` 5 行（末行 EMPLOYEE）；`erp_fin_ar_ap_item.csv` 6 行（5=EMPLOYEE_ADVANCE/RECEIVABLE/OPEN，6=EXPENSE_CLAIM/PAYABLE/OPEN）。列对齐脚本 → **0 错配/14 表**；FK 校验脚本 → **0 FK 错误**（全指向 1234-1 主数据 org=2/currency=1/partner 1-5/md_employee 1-3 或拓扑更早同批表）。5 报表数据集从 CSV 确定性再派生 → 全非空且匹配 plan/analysis 声称值（CRM funnel 2 桶、forecast lineCount=2/Σweighted=63000、CS 2 桶含 survey 桶 avgCsat=5/avgNps=9、HR payroll-sim 2 DETAIL+2 DEPT_SUBTOTAL、HR employee-net-balance partner 5 advance=1000/expense=300/net=700）。`git status --porcelain` + `git diff --stat` → 仅 `*.csv` + `docs/**/*.md` 变更，**零** `.orm.xml/.xbiz.xml/.api.xml/.page.yaml/.view.xml/.beans.xml/.java` 修改。dict 校验脚本 → **0 错误/20 dict 列**。Plan 4 Phase completed + 全项 `[x]` + gates 1-6 `[x]`；e2e-runbook 标 84 CSV；seed-data/known-good-baselines/2210-1 deferred/backlog README 均反映 CRM/CS/HR 落地。非阻塞 MINOR：未跟踪 successor plan `2026-07-09-1045-2-…md` 预置（文档，非保护区域）。无 BLOCKER。VERDICT: PASS。

Follow-up:

- 无非阻塞跟进项（CRM/CS/HR 配置表/GL 凭证/精确数值断言均已在计划内 Non-Goals 段附触发条件，分别归 successor）。
