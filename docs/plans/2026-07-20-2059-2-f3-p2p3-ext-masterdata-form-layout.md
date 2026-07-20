# 2026-07-20-2059-2-f3-p2p3-ext-masterdata-form-layout F3 P2+P3 — 扩展 8 域 + 主数据 Form 布局分组（F3 收尾）

> Plan Status: active
> Last Reviewed: 2026-07-20
> Source: `docs/backlog/frontend-ui-roadmap.md` §F3（优先级表 P2 = ext: crm/cs/hr/aps/logistics/b2b/contract/drp；P3 = master-data）
> Related: `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（F3 P0 核心 4 域）；`docs/plans/2026-07-20-2059-1-f3-p1-mfg-tier-form-layout.md`（同期 F3 P1 successor，mfg 5 域）；`docs/plans/2026-07-12-1500-1-view-form-layout-overhaul.md`（首批 39 头实体含 ext 域 8 实体 + master-data 4 实体）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-20，独立子代理 ses_08069fab5ffe 完整核对 9 域）：

- **F3 P0 范式已确立**（plan `2026-07-19-1818-2`）：`<layout x:override="replace">` + `=========>groupName[中文标签]======` 分组标记，审计组 `^` 缺省折叠；`add` 经 `x:prototype="edit"` 继承。
- **1500-1 在 9 域已覆盖 12 个头实体**（应排除）：
  - master-data 4：`ErpMdMaterial` / `ErpMdPartner` / `ErpMdSubject` / `ErpMdOrganization`
  - crm 2：`ErpCrmLead` / `ErpCrmActivity`
  - 其余各域 1：`ErpHrEmployee` / `ErpLogShipment` / `ErpCsTicket` / `ErpApsSchedule` / `ErpB2bEdiDoc` / `ErpCtContract`
  - **drp 域 1500-1 零覆盖**（全 7 实体 codegen-default，greenfield）
- **本计划范围 — codegen-default 待修实体**（9 域共 **146 个** view.xml 为空壳）：

  | 域 | view.xml 总数 | 已分组 | codegen-default |
  |----|--------------:|-------:|----------------:|
  | crm | 34 | 2 | 32 |
  | cs | 16 | 1 | 15 |
  | hr | 36 | 1 | 35 |
  | aps | 6 | 1 | 5 |
  | logistics | 7 | 1 | 6 |
  | b2b | 13 | 1 | 12 |
  | contract | 15 | 1 | 14 |
  | drp | 7 | 0 | 7 |
  | **P2 小计（8 ext 域）** | **134** | **8** | **126** |
  | master-data（P3） | 24 | 4 | 20 |
  | **合计** | **158** | **12** | **146** |

- **本计划实际落地范围（交易主实体 + Line 子实体过滤器 + P3 字典 triage）**：套用 1818-2 过滤器。预估 **~90 实体**：
  - **P2 ext 域交易头 + Line（~80）**：crm（Campaign/Forecast/Quota/Sequence/Opportunity/Quote 等 + ForecastLine/QuotaLine 等）/ cs（SlaPolicy/Entitlement/Survey/KnowledgeBase/CannedResponse/Contract 等）/ hr（Salary/SalarySimulation/Recruitment/LeaveRequest/Attendance/Timesheet/EmploymentContract/Position 等 + TimesheetLine/SalarySimulationItemAdjustment 等；注：薪酬登记实体为 `ErpHrSalary`，ORM 无 `ErpHrPayroll`，仅有 `ErpHrPayrollBankFile` 银行代发文件配置实体归 Non-Goal）/ aps（OperationOrder/Constraint 等）/ logistics（ShipmentLine/Carrier/CarrierConfig/ShipmentParcel 等）/ b2b（Asn/AsnLine/EdiFormat/PartnerProfile/CodeMapping 等）/ contract（ContractLine/InvoicePlan/RebateAgreement/RebateAccrual/VolumeDiscount/ContractVersion/Template 等）/ drp（Plan/Line/Parameter 等）
  - **P3 master-data（~10，经 triage）**：triage 裁决——≤8 字段纯字典（Currency/UoM/TaxRate/SettlementMethod 等 ~12 个）维持 codegen 默认不作分组；仅对有业务分组价值的 ~10 实体（Warehouse/Employee/ExchangeRate/AcctSchema/MaterialCategory/MaterialSku/PartnerContact/BankAccount 等）落地分组。
- **设计文档缺口（关键）**：9 域 `ui-patterns.md` **均存在但均无** form 布局分组段落（仅含页面级 wireframe）。**注意路径用全域名**：`docs/design/crm/`、`docs/design/customer-service/`（**非** `cs/`）、`docs/design/human-resource/`（**非** `hr/`）、`docs/design/aps/`、`docs/design/logistics/`、`docs/design/b2b/`、`docs/design/contract/`、`docs/design/drp/`、`docs/design/master-data/`。**本计划 Phase 0 必须先为 9 域 ui-patterns.md 补建 form 布局段落（全部为既有文件追加段落，无需新建文件），再实施 view.xml。**
- **drp 域特殊性**：`ErpDrpPlan.view.xml` 已有 `<grid>` + 自定义 actions（runDrp/approveAll/generateOrder 按钮）但零 form layout；其余 6 实体完全 codegen-default。
- **前置已就绪**：codegen 模板已跳过 seq-default id；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿。

## Goals

1. 9 域 `ui-patterns.md` 各新增「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落（对齐 1818-2 范式）
2. **P3 字典 triage 决策**：在 plan 内表格化记录 master-data 20 实体的 triage 分类（纯字典 → 维持默认 vs 有分组价值 → 落地）
3. ~90 个交易头实体 + Line 子实体 view.xml 的 `<form id="view">` 与 `<form id="edit">` 实现 `====>` 分组；审计组缺省折叠
4. 全部 in-scope 实体的 `<form id="query">` 填充 5+ 查询字段 + filterOp
5. **F3 路线图项完全收尾**：F3 P0（核心 4 域）+ F3 P1（mfg 5 域）+ 本计划（ext 8 域 + master-data）= 全 18 域 form 布局分组覆盖

## Non-Goals

- **F3 P1 mfg 5 域**——同期 plan `2026-07-20-2059-1` 处理
- **F4 Phase 2 子表编辑**——本计划仅做 Line 实体独立 view.xml form 分组，不涉及父视图内嵌子表控件
- **F6 字段格式化 / F8 asideFilter / F1 按钮 / F5 状态标签 / F10 树形视图**——均已有独立计划完成
- **修改 ORM 模型 / picker.page.yaml / action-auth.xml / i18n**——保护区域或独立 F 项
- **master-data 纯字典实体 form 分组**（~12 个，见 P3 triage）——维持 codegen 默认
- **view.xml layoutControl="wizard"/"tabs"**（F12 覆盖）

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F3（优先级表 P2/P3）
  - 9 域 `docs/design/<domain>/ui-patterns.md`（注意 cs=`docs/design/customer-service/`、hr=`docs/design/human-resource/`，其余同 short 名；各新增 form 布局段落）
  - `docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（Phase 0 范式）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`（form override=replace）
- Skill Selection Basis: 加载 `nop-frontend-dev`（form layout override=replace + cells filterOp + bounded-merge）；P3 master-data 含树形实体 `ErpMdMaterialCategory`（F10 已落地 tree grid）——本计划仅补其 form 分组，不改 tree grid，故不另加载技能；不涉及 BizModel/xbiz，不加载 `nop-backend-dev`；不写自动化测试，不加载 `nop-testing`。

## Infrastructure And Config Prereqs

- 同 plan `2026-07-20-2059-1`（`_dump/nop-app/` + `mvn clean install -DskipTests` codegen 增量 + 手写层 view.xml 路径 `module-<domain>/erp-<short>-web/.../pages/<Entity>/<Entity>.view.xml`，short 名：crm/cs/hr/aps/log/b2b/ct/drp/md）

## Execution Plan

### Phase 0 — 范式对齐 + 9 域 ui-patterns.md form 布局段落补建 + P3 字典 triage 决策 + 实体清单冻结

Status: planned
Targets: 9 域 `docs/design/<domain>/ui-patterns.md`（各新增 form 布局段落）+ plan 内 P3 triage 表
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] `Decision`: 8 ext 域交易头 + Line 子实体确切清单冻结（套用 1818-2 过滤器；排除纯配置/规则/模板字典实体如 crm 的 QuoteTemplate/LeadScoreConfig/SequenceStep、hr 的 SurveyQuestion/ShiftRotationPattern 等低频配置）。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: **P3 master-data 字典 triage**——表格化 20 实体分类：(A) 纯字典 ≤8 字段维持 codegen 默认（Currency/UoM/TaxRate/SettlementMethod/CostCenter/Location/UoMConversion/SubjectMapping 等 ~12）；(B) 有分组价值落地（Warehouse/Employee/ExchangeRate/AcctSchema/AcctSchemaCoa/MaterialCategory/MaterialSku/PartnerContact/BankAccount/PartnerAddress/SupplierApproval/SysConfig 等 ~10，部分按业务关键字段分组）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 9 个 `ui-patterns.md` 各追加「主实体 form 布局分组」+「Line 子实体 form 分组模板」段落（9 文件均既有；cs 路径=`docs/design/customer-service/`、hr 路径=`docs/design/human-resource/`，非 short 名）。
  - Skill: `none`

Exit Criteria:

- [ ] 9 域 in-scope 实体清单 + P3 triage 表在 plan 中冻结
- [ ] 9 个 `ui-patterns.md` 各新增 form 布局段落

### Phase 1 — crm 域

Status: planned
Targets: `module-crm/erp-crm-web/.../pages/ErpCrm*/ErpCrm*.view.xml`（Phase 0 冻结清单，交易头 ~12：Campaign/Forecast/Quota/Sequence/Opportunity/Quote/BundlePricing/Territory/Stage 等 + Line：ForecastLine/QuotaLine/BundlePricingLine/LeadScoreLine 等）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [ ] `Add`: crm Line 子实体 + 交易主实体 view.xml form 分组；Campaign 突出预算/日期/状态；Forecast 突出周期/版本/金额；Quota 突出区域/周期/指标；Sequence 突出线索/阶段编排。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: crm view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpCrmCampaign` + `ErpCrmForecast` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] crm in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [ ] crm view.xml `xmllint --noout` 全通过

### Phase 2 — hr 域

Status: planned
Targets: `module-hr/erp-hr-web/.../pages/ErpHr*/ErpHr*.view.xml`（交易头 ~15：Salary/SalarySimulation/Recruitment/LeaveRequest/Attendance/Timesheet/EmploymentContract/Position/Competency/DevelopmentPlan/SocialInsuranceBase/TaxConfig/LeaveBalance/Shift/SalaryItem 等 + Line：TimesheetLine/SalarySimulationItemAdjustment/DevelopmentPlanItem 等；注：ORM 无 `ErpHrPayroll`，薪酬登记为 `ErpHrSalary`）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [ ] `Add`: hr Line 子实体 + 交易主实体 view.xml form 分组；Salary 突出员工/周期/应付/社保/个税/实发金额组；SalarySimulation 突出 scenario/adjustment；Recruitment 突出 funnel 状态/职位；LeaveRequest 突出日期/类型/审批；Timesheet 突出项目/任务/工时；EmploymentContract 突出合同期/薪酬。大表单设 `size="lg"`。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: hr view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpHrSalary` + `ErpHrTimesheet` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] hr in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [ ] hr view.xml `xmllint --noout` 全通过

### Phase 3 — cs + contract 域

Status: planned
Targets: `module-cs/erp-cs-web/.../ErpCs*.view.xml`（~9：SlaPolicy/Entitlement/Survey/KnowledgeBase/CannedResponse/Contract/Team/TicketType/TicketAction 等）+ `module-contract/erp-ct-web/.../ErpCt*.view.xml`（~10：ContractLine/InvoicePlan/RebateAgreement/RebateAccrual/VolumeDiscount/ContractVersion/Template/ConsumptionLine/RebateTier/RebateSettlement 等）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [ ] `Add`: cs 实体 form 分组（SlaPolicy 突出响应/解决时长策略；Entitlement 突出服务目录/有效期；Survey 突出 CSAT 评分；KnowledgeBase 突出分类/状态）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: contract 实体 form 分组（ContractLine 突出物料/数量/金额；InvoicePlan 突出开票里程碑/日期/金额；RebateAgreement/Accrual 突出返利规则/累计金额；VolumeDiscount 突出阶梯/折扣；ContractVersion 突出版本号/生效日期/isCurrent）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: cs + contract view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpCtInvoicePlan` + `ErpCsSlaPolicy` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] cs + contract in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [ ] cs + contract view.xml `xmllint --noout` 全通过

### Phase 4 — aps + logistics + b2b + drp 域

Status: planned
Targets: 4 域 view.xml（aps ~4：OperationOrder/Constraint/DispatchRule/OpRouting；logistics ~5：ShipmentLine/Carrier/CarrierConfig/ShipmentParcel/ShipmentLog；b2b ~9：Asn/AsnLine/EdiFormat/PartnerProfile/CodeMapping/MftConfig/EdiLog 等；drp 7：Plan/Line/Parameter/SafetyStockCalc/CrossDock/DockAppointment/LeadTimeRecord）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0

- [ ] `Add`: aps 实体 form 分组（OperationOrder 突出 workOrder/machine/plannedStart-End/优先级/feasible）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: logistics 实体 form 分组（ShipmentLine 突出物料/数量/重量；Carrier 突出 gatewayId/服务类型；CarrierConfig 突出 API 密钥脱敏引用 F7 范式）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: b2b 实体 form 分组（Asn 突出 partner/PO 引用/5 阶段状态；AsnLine 突出物料/匹配状态；EdiFormat 突出格式类型/方向；PartnerProfile 突出 partner/凭据）。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: drp 实体 form 分组（Plan 突出 scenario/warehouse/状态；Line 突出物料/源仓库/补货类型/目标单据；Parameter 突出安全库存/周期；SafetyStockCalc 突出计算结果）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 4 域 view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpDrpPlan` + `ErpB2bAsn` + `ErpApsOperationOrder` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] aps + logistics + b2b + drp in-scope 实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [ ] 4 域 view.xml `xmllint --noout` 全通过

### Phase 5 — master-data P3（triage 后落地集）

Status: planned
Targets: `module-master-data/erp-md-web/.../pages/ErpMd*/ErpMd*.view.xml`（Phase 0 triage B 类 ~10 实体：Warehouse/Employee/ExchangeRate/AcctSchema/AcctSchemaCoa/MaterialCategory/MaterialSku/PartnerContact/BankAccount 等）

Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 0（P3 triage 决策）

- [ ] `Add`: master-data B 类实体 form 分组（Warehouse 突出 code/org/地址/isActive；Employee 突出工号/部门/职位/状态；ExchangeRate 突出币种对/日期/汇率；AcctSchema 突出账套/科目表/本位币；MaterialCategory 突出 tree parentId —— 仅 form 分组不改 tree grid；MaterialSku 突出物料/规格/单位）。
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: master-data B 类 view.xml `xmllint --noout` 全通过；浏览器抽样验证 `ErpMdWarehouse` + `ErpMdEmployee` form 分组。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] master-data B 类实体 view.xml form view/edit 含分组；query 含 5+ 字段 + filterOp
- [ ] master-data B 类 view.xml `xmllint --noout` 全通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_0805d1529ffeoshpPzPTwhR9YB`) — 2 blockers：(B1) `ErpHrPayroll` 不存在（ORM 0 命中，仅有 `ErpHrPayrollBankFile` 配置实体）；(B2) cs/hr `ui-patterns.md` 路径用全域名（`customer-service/`、`human-resource/`）非 short 名，reviewer 检查 short 路径误报 missing。全 9 域实体计数（158/12/146）经 spot-check 全部核实准确；P2+P3 bundling（rule 14）+ 单 plan 规模（~2x F3 P0）被认可保持不拆。已修订：移除 `ErpHrPayroll`/`Payroll` 引用，hr 薪酬登记对齐为 `ErpHrSalary`；Phase 0 + Owner Docs + Current Baseline 显式标注 cs=`customer-service/`、hr=`human-resource/` 全域名路径。
- Independent draft review iteration 2: **accept** (`ses_080543300ffeOPcmPWuSNt3J30`) — B1+B2 均已修复（`ErpHrPayroll` 移除，`ErpHrSalary` ORM:714 确认为薪酬登记；cs/hr ui-patterns.md 全域名路径 `customer-service/`/`human-resource/` 经 `ls` 确认存在并在 Current Baseline/Owner Docs/Phase 0 三处显式标注「既有文件追加」）。Plan 可晋 `active`。

## Closure Gates

- [ ] 范围内行为完成（Phase 0–5 全部 done；~90 实体 form 分组落地）
- [ ] 相关文档对齐：9 域 `ui-patterns.md` 各新增 form 布局段落
- [ ] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 全绿（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）+ 全 in-scope view.xml `xmllint --noout` 通过 + 浏览器抽样分组渲染
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### master-data 纯字典实体 form 分组（A 类 ~12 个）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Currency/UoM/TaxRate/SettlementMethod/CostCenter/Location/UoMConversion/SubjectMapping 等字段数 ≤8 的纯字典，业务分组收益低；维持 codegen 默认。Phase 0 triage 表显式分类。
- Successor Required: `no`（除非业务用户反馈可用性问题）

### F4 Phase 2 子表编辑嵌入父视图（ext 8 域头行对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 Line 实体独立 view.xml form 分组；ext 域父视图内嵌子表行内编辑控件属 F4 Phase 2 P3 结果面（roadmap F4 §Phase 2 P3 ext 8 域 ~36 对 ⏳ 待启动）。
- Successor Required: `yes`（触发条件：F4 Phase 2 P3 ext 8 域 plan 启动时）

### F8 asideFilter 高级筛选区（ext 8 域列表页）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅做 form query 基线；列表页 asideFilter 双筛选面属 F8 结果面（核心 8 列表页已落地，ext 域列表页 asideFilter 独立 successor）。
- Successor Required: `yes`（触发条件：F8 扩展域列表页筛选 plan 启动时）

### logistics 域敏感字段脱敏（CarrierConfig API Key/Secret）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨切面 UI 模式（roadmap §跨域建议 4「敏感字段脱敏」）独立结果面；本计划 CarrierConfig form 仅分组不脱敏。
- Successor Required: `yes`（触发条件：敏感字段脱敏 plan 启动时）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <待填写>

Follow-up:

- F4 Phase 2 P3 ext 8 域头行对子表编辑 successor
- F8 ext 8 域列表页 asideFilter successor
- 敏感字段脱敏跨切面 plan
