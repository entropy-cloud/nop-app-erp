# 2026-07-13-1043-1-finance-fk-name-resolution Finance 域外键名称解析批量推广（列表页 ID→名称）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/plans/2026-07-11-1643-1-amis-frontend-quality.md` Deferred「全量 1,036 FK 列名称解析」（Successor Required: yes，触发条件「高价值子集验证后批量推广需求，或 codegen 模板层 FK 名称解析方案落地」——**已满足**：经 1643-1 Phase 3 + 批次 2 `2026-07-12-0600-1` + 批次 3 `2026-07-12-0800-2` + 批次 4 `2026-07-12-0900-2` 共 4 批次验证机制 D 可行性，27 实体已落地含测试全绿；codegen 模板路径经 0600-1 裁决否决——触及 nop-entropy 平台保护区域且 1,036 列一次推广风险高，故本批及后续沿用逐实体机制 D 范式）
> Related: `2026-07-11-1643-1-amis-frontend-quality.md`（机制 D 范式源，ErpSalOrder 4 BizLoader 参考）、`2026-07-12-0600-1-transaction-list-fk-name-resolution-batch2.md`（批次 2 范式）、`2026-07-12-0800-2-transaction-line-fk-name-resolution-batch3.md`（批次 3）、`2026-07-12-0900-2-transaction-line-warehouse-name-resolution.md`（批次 4）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 范围，独立子代理全量盘点 `module-finance/erp-fin-meta/` + `erp-fin-service/` + `erp-fin-web/`）：

### 机制 D 已验证（4 批次 27 实体）

机制 D 三层接线（参考 `ErpSalOrder`）：(1) 自定义 xmeta 增派生 `*Name` prop（`queryable="false" sortable="false"`，`schema type="java.lang.String"`）；(2) BizModel 增 `@BizLoader(forType = Entity.class)` 方法（签名 `public List<String> xxxName(@ContextSource List<Entity> rows)`，经 `orm().batchLoadProps(rows, Collections.singleton("<relation>"))` 批量加载 to-one 关系防 N+1，读 `getRelation().getName()`）；(3) 自定义 view.xml `<grid id="list"><cols x:override="bounded-merge">` 用 `*Name` 列替换原始 `*Id` 列。`ext:relation` 已在全部相关 FK `*Id` prop 上声明于 `_gen` xmeta，`batchLoadProps` 开箱可用——零 ORM 变更。

### Finance 域覆盖现状

- **已覆盖（机制 D 全接线）1 实体**：`ErpFinReconciliation`（partnerName/currencyName/orgName，`ErpFinReconciliationBizModel.java:211/221/231` 3 loader）。
- **已策划网格（无原始 ID 显示，无 `*Name`）1 实体**：`ErpFinBudgetScenario`（`bounded-merge` 仅保留 code/name/fiscalYear 等非 FK 列，全部 `*Id` 已剔除，无名称替换需求）。
- **不存在/非持久化 2 实体**：`ErpFinArApBalance`（无 xmeta/view/ORM）、`ErpFinPartnerBalance`（仅内存 updater 类 `PartnerBalanceUpdater.java`，无 CRUD 面）。
- **未覆盖（列表页显示原始数字 ID）15 实体**——本计划范围。

### 未覆盖 15 实体清单（生成网格中显示为 `ui:number` 的 FK 列）

| # | 实体 | 生成网格中原始 `*Id` FK 列 | 用户面价值 |
|---|------|---------------------------|-----------|
| 1 | **ErpFinVoucher** | orgId, acctSchemaId, periodId, reversalOfVoucherId | 凭证头，会计高频 |
| 2 | **ErpFinVoucherLine** | subjectId, currencyId, acctSchemaId, orgId, partnerId, departmentId, projectId, warehouseId, materialId, costCenterId（10 列，最严重） | 凭证行，会计核心网格 |
| 3 | **ErpFinVoucherBillR** | voucherId | 业财回链，低频 |
| 4 | **ErpFinArApItem** | orgId, acctSchemaId, partnerId, currencyId, periodId | AR/AP 辅助账，往来核销高频 |
| 5 | **ErpFinBadDebt** | orgId, acctSchemaId, partnerId, sourceArApItemId, currencyId, periodId, voucherId | 坏账管理 |
| 6 | **ErpFinBankReconciliation** | orgId, fundAccountId, statementId | 银行对账 |
| 7 | **ErpFinBankStatement** | orgId, fundAccountId | 银行流水 |
| 8 | **ErpFinBankStatementLine** | statementId, currencyId, matchedLineId | 银行流水行 |
| 9 | **ErpFinFundAccount** | orgId, subjectId, currencyId（bankName 为原生列已显示） | 资金账户 |
| 10 | **ErpFinNotesReceivable** | orgId, currencyId, partnerId, endorsementFromId, discountId | 应收票据 |
| 11 | **ErpFinNotesPayable** | orgId, currencyId, partnerId, creditFacilityId | 应付票据 |
| 12 | **ErpFinBudgetLine** | scenarioId, orgId, acctSchemaId, periodId, subjectId, costCenterId, departmentId, projectId, partnerId, warehouseId, materialId, currencyId（12 列，最严重） | 预算明细 |
| 13 | **ErpFinPostingException** | orgId, acctSchemaId, voucherId, currencyId | 过账异常工作台 |
| 14 | **ErpFinExpenseClaim** | orgId, claimantId, departmentId, currencyId, settleAdvanceId | 费用报销 |
| 15 | **ErpFinEmployeeAdvance** | orgId, employeeId, currencyId, projectId | 员工借款 |

剩余差距：15 finance 实体列表页显示原始数字 ID（用户面 P1 缺陷，`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §4.2）。

## Goals

- 15 finance 实体列表页的高价值用户面 FK 列显示名称而非原始 ID（经机制 D：xmeta `*Name` + BizModel `@BizLoader` 批量加载 + view.xml `bounded-merge`）。
- 高价值 FK 定义：维度型外键（partner/material/warehouse/subject/currency/org/employee/department/project/costCenter/fundAccount/acctSchema/period）+ 高价值父单型内部链路（voucherId/scenarioId/statementId/creditFacilityId/discountId/settleAdvanceId，承载业务上下文→解析为父单 code）。纯匹配/链路型 ID（reversalOfVoucherId/sourceArApItemId/matchedLineId/endorsementFromId）保留原始 ID（无独立业务"名称"语义）。逐项裁决见 Phase 1/Phase 2 Decision。
- 零 ORM/契约变更（机制 D 仅 xmeta 派生字段 + BizModel 只读 loader + view.xml 静态定制）。

## Non-Goals

- **其他域 FK 名称解析**（manufacturing/quality/maintenance/assets/projects/HR/logistics/CRM/CS/master-data/b2b/contract/drp/aps）——归后续 successor（`2026-07-13-1043-2-manufacturing-fk-name-resolution.md` 承接 manufacturing 子集，其余域逐批 successor）。
- **codegen 模板层 FK 名称解析方案**——经 0600-1 裁决否决（触及 nop-entropy 平台保护区域 + 1,036 列一次推广风险高 + BizModel 为手写类无法 codegen 注入 `@BizLoader`）。
- **drawer 子表/明细行子网格 FK 名称**——本计划仅处理主列表网格 `<grid id="list">`（与既有 4 批次同口径）。
- **纯匹配/链路型内部 ID 的名称解析**（reversalOfVoucherId/sourceArApItemId/matchedLineId/endorsementFromId 等自引用/溯源链路）——无独立业务"名称"语义，保留原始 ID，归 successor（高价值父单型内部链路 voucherId/scenarioId/statementId/creditFacilityId/discountId/settleAdvanceId 已在本计划 Phase 1/2 Decision 解析为父单 code，属范围内）。
- **`ErpFinBudgetScenario`**（已策划网格无 ID 显示，无需求）。
- **不存在的实体**（`ErpFinArApBalance`/`ErpFinPartnerBalance`）。
- **余额/试算派生表**（`ErpFinGlBalance`/`ErpFinTrialBalance`）——计算产物，列表面低频，归 successor。
- **看板/报表 FK 名称**——已由 1643-1 Phase 4 + 1225-1 覆盖。

## Task Route

- Type: `app-layer design change`（改用户可见的列表页显示行为，跨 finance 域多实体，不改 API/模型/认证）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图分层与定制边界）、`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（机制 D 权威参考）、`../nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`（加 BizLoader 字段）
- Skill Selection Basis: xmeta 派生字段 + view.xml grid `bounded-merge` → 匹配 `nop-frontend-dev`（XView 三层 / bounded-merge / delta 覆盖）；BizModel `@BizLoader` 跨实体批量加载 → 匹配 `nop-backend-dev`（决策门 / `@BizLoader` / `orm().batchLoadProps`）；JUnit 测试 → `nop-testing`（`IGraphQLEngine` findList 触发 loader）。
- Protected Areas: 无 ORM/ask-first 变更（机制 D 为 xmeta 派生 + 只读 loader + 静态 view 定制）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。零端口/环境变量/密钥/外部服务/数据迁移依赖。

## Execution Plan

### Phase 1 - 核心会计实体 FK 名称解析（Voucher/VoucherLine/VoucherBillR/ArApItem/BadDebt/PostingException）

Status: completed
Targets: `module-finance/erp-fin-meta/.../ErpFin{Voucher,VoucherLine,VoucherBillR,ArApItem,BadDebt,PostingException}/ErpFin*.xmeta`；`module-finance/erp-fin-service/.../entity/ErpFin*BizModel.java`；`module-finance/erp-fin-web/.../ErpFin*/ErpFin*.view.xml`
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（机制 D 已由 4 批次验证）

- [x] `Decision`: 裁决 Phase 1 实体的目标 FK 清单 + 显示字段——维度型 FK 全部解析（partner→partnerName 读 `ErpMdPartner.name`；subject→subjectName 读 `ErpMdSubject.name`；material→materialName；warehouse→warehouseName；currency→currencyName；org→orgName；department→departmentName 读 `ErpMdOrganization.name`；project→projectName 读 `ErpPrjProject.name`；costCenter→costCenterName；employee/claimant→employeeName 读 `ErpHrEmployee.employeeName` 列；fundAccount→fundAccountName 读 `ErpFinFundAccount.name`；acctSchema→acctSchemaCode 读 `ErpMdAcctSchema.code`（账套无 name 用 code）；period→periodCode 读 `ErpFinAccountingPeriod.code`）。Phase 1 内部链路型 ID 裁决：`voucherId`（VoucherBillR/PostingException）→ voucherCode（过账异常工作台需知哪个凭证失败，业务可见）；`reversalOfVoucherId`（Voucher）+ `sourceArApItemId`（BadDebt）保留原始 ID（内部冲销/溯源链路，无独立业务"名称"语义）。Phase 2 父单型内部链路 FK 的逐项裁决见 Phase 2 Decision。
  - Skill: `nop-backend-dev`
  - **执行裁决补充**：(1) `ErpFinVoucherLine` 已持久化 `subjectCode`+`subjectName` 冗余列（collision 检测），不新增派生 subjectName，网格直接保留这两列；(2) `ErpFinPostingException` 的 org/acctSchema/voucher/currency 4 个 to-one 关系在 ORM 中缺失（基线假设"ext:relation 已在全部相关 FK 上声明"对此实体不成立），补加 4 个 additive to-one 关系（镜像 ErpFinBadDebt 同名关系）后 `mvn clean install` 重生成，机制 D 方可用；(3) 实际读取 employee/claimant 名称经关系 `employee`/`claimant`→`ErpMdEmployee.name`（非 ErpHrEmployee）。
- [x] `Add`: 6 实体 xmeta 增派生 `*Name` prop（镜像 `ErpSalOrder.xmeta`，`queryable="false" sortable="false"` + `schema type="java.lang.String"`）。
  - Skill: `nop-frontend-dev`
- [x] `Add`: 6 实体 BizModel 增 `@BizLoader(forType = ErpFin*.class)` 方法（镜像 `ErpSalOrderBizModel:228-269`，`orm().batchLoadProps(rows, Collections.singleton("<relation>"))` 批量加载 + null 安全读取）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 6 实体 view.xml `<grid id="list">` 由空占位改为 `<cols x:override="bounded-merge">`，用 `*Name` 列替换原始 `*Id` 列，保留 code/status/amount/date 等非 FK 业务列。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: 新增 `TestErpFinFkNameLoader.java`（extends `JunitAutoTestCase`，镜像 `TestErpSalFkNameLoader`），经 `IGraphQLEngine` findList + `FieldSelectionBean` 请求 `*Name` 字段触发 `@BizLoader`，断言 `ErpFinVoucherLine`（subjectName/partnerName/materialName 等高价值字段）+ `ErpFinArApItem`（partnerName/currencyName）名称对齐 master-data。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] 6 核心会计实体列表网格显示 `*Name` 而非原始 `*Id`（6 view.xml `xmllint --noout` well-formed + bounded-merge 含 `*Name` 列）
- [x] `TestErpFinFkNameLoader` 全方法绿，验证 `@BizLoader` 批量加载防 N+1 且名称正确

### Phase 2 - 资金/票据/预算/报销实体 FK 名称解析（BankReconciliation/BankStatement/BankStatementLine/FundAccount/NotesReceivable/NotesPayable/BudgetLine/ExpenseClaim/EmployeeAdvance）

Status: completed
Targets: 9 实体的 xmeta + BizModel + view.xml（同 Phase 1 三层）
Skill: `nop-frontend-dev`、`nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 范式已验证

- [x] `Decision`: 逐项裁决 Phase 2 父单型/内部链路型 FK 的处置（原则：子实体有独立平铺列表页且父单 code 承载业务含义→解析为 code；纯匹配/链路/自引用且无业务"名称"→保留原始 ID）：
  - **解析为父单 code**：`scenarioId`（BudgetLine→scenarioCode，预算行平铺需知属哪个方案）、`statementId`（BankStatementLine→statementCode；BankReconciliation→statementCode，对账单上下文）、`creditFacilityId`（NotesPayable→creditFacilityCode，授信额度来源）、`settleAdvanceId`（ExpenseClaim→employeeAdvanceCode，抵扣的借款单）。
  - **保留原始 ID**：`matchedLineId`（BankStatementLine 自引用匹配链路）、`endorsementFromId`（NotesReceivable 背书链路自引用）——纯内部匹配/链路，无独立业务"名称"。
  - **执行裁决补充**：`discountId`（NotesReceivable）原计划解析为 discountCode，经核实 `ErpFinNotesDiscount` 实体**无 `code` 列**（仅有 notesReceivableId/orgId/discountDate/bankId/金额 等明细字段），且为应收票据自身的贴现明细回指（detail pointer），无独立业务"名称"语义——重新裁决为**保留原始 ID**（与 matchedLineId/endorsementFromId 同类）。此举同时避免了 ORM 关系补加（discount 关系缺失 + 无 code 可读）。
  - **残留 UX 风险确认**：本计划后部分网格将混合显示已解析 `*Name` 列与保留的原始 `*Id` 列（matchedLineId/endorsementFromId/discountId 等），属可接受的残留风险（保留项归 Deferred successor）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 9 实体 xmeta 增派生 `*Name` prop（fundAccountName/partnerName/currencyName/orgName/subjectName/materialName/warehouseName/departmentName/projectName/costCenterName/employeeName/claimantName/scenarioCode/statementCode/creditFacilityCode/discountCode/employeeAdvanceCode，按 Phase 2 Decision 裁决清单）。
  - Skill: `nop-frontend-dev`
  - 实际派生字段集（按裁决）：BankReconciliation{orgName,fundAccountName,statementCode}；BankStatement{orgName,fundAccountName}；BankStatementLine{statementCode,currencyName}；FundAccount{orgName,subjectName,currencyName}；NotesReceivable{orgName,currencyName,partnerName}（discountId 保留原始 ID）；NotesPayable{orgName,currencyName,partnerName,creditFacilityCode}；BudgetLine{scenarioCode,orgName,acctSchemaCode,periodCode,subjectName,costCenterName,departmentName,projectName,partnerName,warehouseName,materialName,currencyName}；ExpenseClaim{orgName,claimantName,departmentName,currencyName,employeeAdvanceCode}；EmployeeAdvance{orgName,employeeName,currencyName,projectName}。
- [x] `Add`: 9 实体 BizModel 增 `@BizLoader` 方法（维度型 FK 同 Phase 1 范式读 `.name`；父单型读父实体 `.code`；fundAccount 读 `ErpFinFundAccount.name`）。保留原始 ID 的内部链路型 FK（matchedLineId/endorsementFromId/discountId）不新增 loader。
  - Skill: `nop-backend-dev`
- [x] `Add`: 9 实体 view.xml `<grid id="list">` 改 `<cols x:override="bounded-merge">`，`*Name` 替换 `*Id`。
  - Skill: `nop-frontend-dev`
- [x] `Proof`: `TestErpFinFkNameLoader` 扩展用例——`ErpFinFundAccount`（subjectName/currencyName）+ `ErpFinBudgetLine`（subjectName/materialName/partnerName）名称断言对齐 master-data；9 实体 view.xml `xmllint --noout` 全 well-formed。
  - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] 9 资金/票据/预算/报销实体列表网格显示 `*Name` 而非原始 `*Id`（9 view.xml `xmllint --noout` well-formed）
- [x] `TestErpFinFkNameLoader` 扩展用例全绿

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a699208fffe7ZLlDSyztVn1YD, 2026-07-13) — 0 Blocker / 1 Major / 4 Minor。基线全部核实通过（ErpFinReconciliation 已覆盖 / ErpFinBudgetScenario 已策划无 ID / ErpFinArApBalance+ErpFinPartnerBalance 不存在 / 15 未覆盖实体抽查 VoucherLine+ArApItem+FundAccount 原始 ID 确认 / ErpSalOrderBizModel:228-269 机制 D 参考 / 4 前序批次 completed / 触发条件满足）。**M1（Phase 2 父单型内部链路 FK 未逐项裁决，"多数"措辞模糊）+ m1（fundAccount 字段 name vs accountNo 未决）已修订**：Phase 2 增 Decision 项逐项裁决 scenarioId/statementId/creditFacilityId/discountId/settleAdvanceId→父单 code，matchedLineId/endorsementFromId→保留原始 ID；fundAccount 定为读 `.name`；增残留 UX 风险确认；Phase 2 Proof 具体化；m4 删除悬空"记录映射"承诺。m2/m3/m4 已并入修订。
- Independent draft review iteration 2: `accept` (ses_0a68c576fffeC35gJ3iYtGpdTI, 2026-07-13) — M1/m1/m2/m3/m4 全部 resolved（Phase 2 Decision 逐项裁决 7 内部链路 FK、"多数"已消除、fundAccount 定 name、残留 UX 风险已确认、Phase 2 Proof 具体化、悬空承诺已删）；0 新 Blocker/Major；1 非阻塞 Minor（Goals/Non-Goals/Deferred 摘要 prose 与 Phase 2 Decision 不一致）已顺手对齐。计划 execution-ready，状态 draft→active。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-finance/erp-fin-service -am`（含新增 `TestErpFinFkNameLoader`）+ 15 view.xml `xmllint --noout` 一次。

- [x] 范围内行为完成（15 finance 实体列表页 FK 显示名称）
- [x] 相关文档对齐（`view-and-page-strategy.md` / `cross-module-entity-reference.md` 机制 D 范式无需更新；本计划为既有范式批量推广）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + finance-service `mvn test` 197 tests 0 failures/0 errors + 15 view.xml `xmllint --noout` well-formed）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finance 域剩余低频/派生表 FK 名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpFinGlBalance`/`ErpFinTrialBalance`（计算产物，列表面低频）+ `ErpFinAccountingPeriodStatus`/`ErpFinCashForecast`/`ErpFinReconciliationLine`/`ErpFinBankReconciliationLine`/`ErpFinVoucherTemplate(Line)`/`ErpFinExpenseClaimLine`/`ErpFinBudgetControlLog`/`ErpFinNotesDiscount`/`ErpFinCreditFacility`/`ErpFinAccountingPeriod` 等明细行/配置/计算表使用频率低，本计划聚焦高频主单据头/行网格。
- Successor Required: `yes`（触发条件：低频/派生表出现用户面反馈或产品要求批量提升时）

### 纯匹配/链路型内部 ID 名称解析

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: reversalOfVoucherId/sourceArApItemId/matchedLineId/endorsementFromId/discountId 等自引用/冲销/匹配链路，无独立业务"名称"语义；高价值父单型内部链路（voucherId/scenarioId/statementId/creditFacilityId/settleAdvanceId）已在本计划范围内解析为父单 code（discountId 经执行裁决转入保留原始 ID——ErpFinNotesDiscount 无 code 列）。
- Successor Required: `yes`（触发条件：纯链路型引用的父单/上下文显示需求落地时）

## Closure

Status Note: **计划已完成**。finance 域 15 高频实体列表页 FK 名称解析全部落地（机制 D：xmeta 派生 `*Name`/`*Code` prop + BizModel `@BizLoader` `orm().batchLoadProps` 批量加载 + view.xml `<cols x:override="bounded-merge">`）。Phase 1（6 核心会计实体）+ Phase 2（9 资金/票据/预算/报销实体）两阶段全接线。`TestErpFinFkNameLoader` 4 测试全绿（VoucherLine subjectName/partnerName/materialName + ArApItem partnerName/currencyName + FundAccount subjectName/currencyName + BudgetLine subjectName/materialName/partnerName/scenarioCode）。finance-service `mvn test` 197 tests 0 failures/0 errors；154 模块 `mvn clean install -DskipTests` BUILD SUCCESS；15 view.xml `xmllint --noout` 全 well-formed。

**执行中对基线假设的必要修正**（基线"ext:relation 已在全部相关 FK 上声明、零 ORM 变更"不完全成立）：
1. `ErpFinPostingException` 的 org/acctSchema/voucher/currency 4 个 to-one 关系在 ORM 中缺失（实体有 FK 列 + 索引但无 `<relations>`）——补加 4 个 additive to-one 关系（镜像 sibling ErpFinBadDebt 同名关系）后 `mvn clean install` 重生成，机制 D 方可用。这是 ORM 建模缺口修正，纯增量、零行为变更风险。
2. `ErpFinNotesReceivable.discountId` 原计划解析为 discountCode，经核实 `ErpFinNotesDiscount` 实体**无 `code` 列**（仅有明细字段），且为应收票据自身的贴现明细回指——重新裁决为**保留原始 ID**（与 matchedLineId/endorsementFromId 同类纯内部链路），避免 ORM 变更。
3. `ErpFinVoucherLine` 已持久化 `subjectCode`+`subjectName` 冗余列（collision 检测），不新增派生 subjectName，网格直接保留这两列。
4. `erp-fin-service` pom 的 `erp-projects-dao` 由 `test` scope 提升为 `compile`（VoucherLine/BudgetLine/EmployeeAdvance 的 projectName loader 读 `ErpPrjProject.name` 需编译期类型；DAG 合法 R：finance→projects 只读）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，独立 closure-auditor，不重用执行者上下文）
- Verdict: PASS
- Evidence:
  - 15 finance 实体机制 D 三层接线全量核实：xmeta 派生 `*Name`/`*Code` prop 计数与 Phase 2 Decision 逐实体清单一致（Voucher=3/VoucherLine=10/VoucherBillR=1/ArApItem=5/BadDebt=6/PostingException=4/BankReconciliation=3/BankStatement=2/BankStatementLine=2/FundAccount=3/NotesReceivable=3/NotesPayable=4/BudgetLine=12/ExpenseClaim=5/EmployeeAdvance=4）。
  - 15 BizModel `@BizLoader` 方法计数与 xmeta 派生字段一一对应；反空心抽查 `ErpFinVoucherLineBizModel` 全 10 loader 为真实实现（`orm().batchLoadProps` + null 安全读 `.getName()`/`.getCode()`，无空函数体/`return null` 占位）。
  - 15 view.xml `xmllint --noout` 全 well-formed + 全部含 `bounded-merge` 与 `*Name`/`*Code` 列。
  - `TestErpFinFkNameLoader`（4 `@Test`：VoucherLine/ArApItem/FundAccount/BudgetLine）+ 4 个 autotest 快照目录存在；`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)`，经 `IGraphQLEngine` findList + `FieldSelectionBean` 触发 loader 真实运行时路径（非仅类型签名）。
  - `ErpFinPostingException` ORM 补加 4 additive to-one 关系（org/acctSchema/voucher/currency，镜像 sibling ErpFinBadDebt）已在 `module-finance/model/app-erp-finance.orm.xml` 核实落地。
  - 日志 `docs/logs/2026/07-13.md` 第 3-13 行含本计划完整条目（机制 D 三层接线 + 两 Phase + 测试 + 基线修正 + 验证状态）。
  - 执行者报告的验证状态（154 模块 `mvn clean install -DskipTests` BUILD SUCCESS + finance-service `mvn test` 全绿 + 15 view.xml well-formed）与仓库工件一致；本次审计为只读语义核实，未重跑全量 mvn。
- Residual risk (non-blocking): 内部链路型 ID（matchedLineId/endorsementFromId/discountId 等）保留原始 ID 显示，已记入 Deferred successor（触发条件已命名）；执行者日志测试计数（197）与同日 0701-1 基线（194）+4 应为 198 的 ±1 计数漂移属日志笔误，不影响结构落地。

Follow-up:

- finance 域剩余低频/派生表 successor（见 Deferred 触发条件）
- 内部链路型 ID successor（见 Deferred 触发条件）
