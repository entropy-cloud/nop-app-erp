# 2026-07-03-1018-2-projects-cost-collection 项目成本归集（工时成本 + 预算控制 + 归集汇总 + 业财过账）

> Plan Status: active
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.6；`docs/design/projects/cost-collection.md`
> Related: `docs/plans/2026-07-02-0700-2-finance-expense-claim-employee-advance.md`（费用报销 done，报销行原生 projectId 为归集来源）、`docs/plans/2026-07-01-0811-1-finance-posting-engine-foundation.md`（过账 Provider 基座 done）
> Mission: erp
> Work Item: 2.6 Project 成本归集
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **projects 实体已完备（非新建）**：`ErpPrjProject`（status `erp-prj/project-status` DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED；budget/committedCost/actualCost/billedAmount）、`ErpPrjTask`（status `erp-prj/task-status`）、`ErpPrjTimesheet`（status `erp-prj/timesheet-status` DRAFT/SUBMITTED/APPROVED；hours/costRate/costAmount/posted/postedAt/postedBy）、`ErpPrjActivityType`（costRate + subjectId）、`ErpPrjProjectType`（defaultSubjectId）、`ErpPrjProjectUser`（role 文本，**无费率列**）、`ErpPrjBudget`+`ErpPrjBudgetLine`（costCategory/subjectId/plannedAmount/committedAmount/actualAmount）、`ErpPrjCostCollection`+`ErpPrjCostCollectionLine`（costCategory/sourceBillType/sourceBillCode/subjectId/amount；头 posted/exchangeRate/amountSource/amountFunctional）、`ErpPrjMilestone`（isBillingTrigger/billingAmount）、`ErpPrjBilling`+`Line`。
- **BizModel 全为空 CRUD 壳**：`ErpPrjProjectBizModel`/`ErpPrjTimesheetBizModel`/`ErpPrjCostCollectionBizModel` 等均**无 `@BizMutation`/`@BizQuery`**——无工时提交状态机、无成本率解析、无人工成本计算、无过账、无预算检查、无归集汇总、无项目状态引用校验。`ErpPrjErrors`/`ErpPrjConstants`/`ErpPrjConfigs` 已存在为空骨架（须扩展，非新建）。
- **无 projects 过账 Provider**：grep 确认 `module-projects/` 下无 `IErpFinAcctDocProvider` 实现。工时成本凭证（借项目成本/贷应付职工薪酬，businessType `PROJECT_COST_COLLECTION`，`cost-collection.md §8`）尚未落地。
- **费用报销行原生携带归集维度**：`ErpFinExpenseClaimLine`（finance.orm.xml:858）含 `projectId`(propId5)+`costCenterId`——费用报销已 done（计划 0700-2），其过账已存在；项目可作为该凭证辅助核算维度，归集来源就绪。
- **成本率载体缺口（设计偏差）**：`cost-collection.md §2.2` 声明「用户级别 > 角色级别 > 活动类型级别」优先级，但 ORM 中**仅有 `Timesheet.costRate`（按单填写）与 `ActivityType.costRate`**；`ProjectUser.role` 为纯文本无费率列，无用户级/角色级独立费率实体。须在 Decision 中裁决解析优先级（见 Task Route）。
- **DAG 依赖方向**（对齐 `data-dependency-matrix.md`）：projects→master-data R（物料/职员/科目/币种/组织）；**finance→projects ORM R**（费用报销行 projectId 经 ORM refEntityName 引用 ErpPrjProject，合法——finance 是 DAG 顶可 R 业务域）；**projects→finance S 写**（过账经 `IErpFinAcctDocProvider`，projects 实现 Provider）+ **projects→finance R 读**（费用报销归集只读查 ErpFinExpenseClaim）。**禁止且未采用**：finance→projects 业务表 S 写（matrix §3.2 明禁）——费用归集由 projects 自写，非 finance 回写。projects-service 须 compile 依赖 `app-erp-finance-service`（过账接口宿主 + 报销单只读 I*Biz，无环）。
- **剩余差距**：(1) 无工时提交状态机 + 成本率解析 + 人工成本计算 + 过账；(2) 无预算控制（WARNING/STRICT）；(3) 无成本归集汇总（ErpPrjCostCollection 自动归集 + 项目 actualCost 回写）；(4) 无项目状态引用校验（OPEN 才能引用 / 关闭冻结）。

## Goals

- **工时成本计算 + 状态机**：`IErpPrjTimesheetBiz` 实现 `submit`（DRAFT→SUBMITTED，校验项目 OPEN + 任务允许 + 取成本率 + 算 `costAmount=hours×costRate` + 预算检查）、`approve`（SUBMITTED→APPROVED + 触发 `PROJECT_COST_COLLECTION` 过账 借项目成本/贷应付职工薪酬 + posted=true）、`reject`/`cancel`。成本率解析优先级见 Task Route Decision。
- **预算控制**：`IErpPrjBudgetBiz` / 工时提交时按项目 `budget` + 控制模式（WARNING 警告放行 / STRICT 超预算拦截）检查「已使用 + 拟新增 > 总预算」；`erp-prj.budget-control-mode` 默认 WARNING。
- **成本归集汇总**：工时 APPROVED → 自动生成/追加 `ErpPrjCostCollection` 行（costCategory=人工/sourceBillType=TIMESHEET/sourceBillCode=工时单号）；`refreshActualCost(projectId)` 聚合归集行金额回写 `ErpPrjProject.actualCost`。
- **项目状态引用校验**：只有 OPEN 项目可被新单据（工时/报销）引用；COMPLETED/CANCELLED 冻结（拒绝新归集）；`IErpPrjProjectBiz` 提供 `closeProject`（OPEN→COMPLETED，检查无未归集成本后冻结）。
- **费用报销归集（projects 驱动只读聚合，config-gated）**：`erp-prj.expense-aggregation-enabled`（默认 true）时，projects `refreshExpenseCost(projectId)` 只读查询已审核费用报销行（projectId 命中）→ 写归集行（costCategory=费用/sourceBillType=EXPENSE）+ 预算检查 + 幂等去重；`closeProject` 关闭前强制刷新（对齐 data-dependency-matrix：projects 自写归集表，finance 从不写业务表）。
- 行为测试覆盖：工时成本率解析（单填/活动类型默认/无默认）、预算 WARNING/STRICT、过账凭证、归集汇总 + actualCost 回写、项目状态引用校验、关闭冻结、费用报销归集。

## Non-Goals

- **采购入库 / 库存领料 → 项目归集**：`cost-collection.md §4.1` 另两来源；需改 purchase/inventory Processor 加项目归集钩子（config-gated）。**触发条件**：采购入库/领料标注项目归集需求落地时（successor）。
- **项目成本资本化（CIP→固定资产）/ 结转损益**：`cost-collection.md §4.3/§4.4`；依赖资产资本化过账（CAPITALIZATION，assets 域 done）与项目类型→科目映射的完整配置。**触发条件**：项目关闭成本结转 + 资本化需求时。
- **项目开票（ErpPrjBilling）触发发票 + 里程碑结算**：工作项 3.12 范畴。**触发条件**：合同/项目开票深化时。
- **项目成本报表**：`cost-collection.md §5`；nop-report 面。**触发条件**：报表需求时。
- **用户级/角色级独立费率配置实体**：当前无载体（见 baseline 缺口）；本期以 Timesheet.costRate + ActivityType.costRate 解析。**触发条件**：多级费率配置需求时。
- **工时审批多级工作流**：本期以单级 approve 简化。**触发条件**：多级审批需求时。

## Task Route

- Type: `app-layer design change + implementation`（工时状态机 + 成本率解析 + 预算控制 + 归集汇总 + PROJECT_COST_COLLECTION 过账 Provider + 项目状态引用校验 + 费用报销归集接入；纯服务层 + 既有实体，不新增实体/列/字典，不触及 model/*.orm.xml）。
- Owner Docs: `docs/design/projects/cost-collection.md`（辅助核算/工时成本/预算控制/归集来源/凭证注册）、`docs/design/finance/posting.md`（IErpFinAcctDocProvider 机制）、`docs/architecture/data-dependency-matrix.md`（finance→projects R + projects→finance 过账 Provider）。
- Skill Selection Basis: BizModel + 跨实体（工时/预算/归集/项目/活动类型）+ 状态机 + 过账 Provider + 跨域（projects→finance 过账 S 写 + projects→finance 读报销单 R）+ 事务 + 错误码 → 加载 `nop-backend-dev`；测试 `nop-testing`；草案/结束审计 `plan-audit-prompt.md`/`closure-audit-prompt.md`。
- **Decision（成本率解析优先级，补 baseline 缺口）**：**选择** `Timesheet.costRate`（按单填写，最高）→ `ErpPrjActivityType.costRate`（活动类型默认）→ `erp-prj.default-labor-cost-rate`（全局默认，config）；**用户级/角色级**无独立载体，本期不实现（Non-Goal）。**替代**：新建用户费率实体（超出「不触及 model」范围，rejected 本期）。**残留风险**：设计 §2.2「用户>角色>活动类型」与实现「单填>活动类型>默认」存在偏差，须在 `cost-collection.md §2.2` 补注实现偏差与 successor 触发条件。
- **Decision（归集汇总触发时机）**：**选择** 工时 APPROVED 时同步生成/追加 `ErpPrjCostCollection` 行（与过账同事务），`refreshActualCost` 聚合回写项目 actualCost。**替代**：定时任务批量归集（延迟可见，与 §4.2「归集流程全景」实时性预期不符，rejected）。**残留风险**：归集与过账同事务，过账失败则归集回滚（符合强一致预期）。
- **Decision（业务类型标识，补 baseline）**：**选择** 复用既有枚举 `ErpFinBusinessType.PROJECT_COST_COLLECTION(110)`（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java:23`）作为工时成本过账 businessType；**不新增枚举/字典**（保持「不触及 model/无契约变更」）。设计 `cost-collection.md §8` 写作 `PROJECT_LABOR_COST` 为命名偏差，Phase 3 补注实际为 `PROJECT_COST_COLLECTION`。**替代**：新增 `PROJECT_LABOR_COST` 枚举（finance 契约编辑，破坏「纯服务层」边界，rejected 本期）。**残留风险**：工时/费用/采购归集共用同一 businessType，须在 Provider 内按 sourceBillType 区分借方科目。
- **Decision（过账 Provider 接口归属）**：`IErpFinAcctDocProvider` 位于 **`module-finance/erp-fin-service`**（非 finance-dao）；故 projects-service 须新增 compile 依赖 `app-erp-finance-service`（与 `assets-service` 一致，见 `module-assets/erp-ast-service/pom.xml`）。无 Maven 环（finance-service→projects-dao，projects-service→finance-service，不同层）。**残留风险**：projects-service 引入对 finance-service 的编译期依赖（仅接口层，无环）。
- **Decision（费用报销归集触发方向，对齐 data-dependency-matrix）**：**选择** **projects 驱动**——projects `IErpPrjCostCollectionBiz.refreshExpenseCost(projectId)` 只读查询已审核 `ErpFinExpenseClaim`/`Line`（经 `IErpFinExpenseClaimBiz`，projects→finance **只读 R**）+ 写 `erp_prj_cost_collection`（**projects 自写**）+ 预算检查 + refreshActualCost；触发为 projects 侧（`closeProject` 聚合前调 + 暴露为手动/批量入口，`erp-prj.expense-aggregation-enabled` config-gated）。**依据**：`data-dependency-matrix.md §3.2:160`「finance 对业务域是纯读——从不写业务表」+ `§4.2:217` 成本归集为 **projects 触发** `confirmCollection()`。**替代**：finance 报销审批 `approve` 调 projects 写归集（**finance→projects 业务表写，违反 matrix §3.2/§4.4，rejected**）。**残留风险**：非实时（聚合在 projects 侧刷新时发生，非报销审批瞬间）；closeProject 前强制刷新保证关账时数据完整。
- **Decision（预算检查控制模式）**：**选择** 项目级 `erp-prj.budget-control-mode`（WARNING 默认/STRICT）；STRICT 模式超预算抛 `ErpPrjErrors.ERR_BUDGET_EXCEEDED`。**替代**：预算行级 STRICT（粒度过细，rejected 本期，行级 committed/actual 仍记录但不拦截）。设计 `cost-collection.md §3.1` 暗示预算头级 controlMode 字段，本期以项目级 config 实现，Phase 3 补注偏差。**残留风险**：项目级模式对多预算行项目粒度粗（行级金额仍记录备查）。

## Infrastructure And Config Prereqs

- 配置项：`erp-prj.budget-control-mode`（默认 WARNING）、`erp-prj.default-labor-cost-rate`（默认空=无全局默认费率，工时无费率且活动类型无费率时抛 `ERR_COST_RATE_NOT_AVAILABLE`）、`erp-prj.default-payroll-subject-id`（默认空=应付职工薪酬贷方科目；为空时抛 `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`）、`erp-prj.expense-aggregation-enabled`（默认 true，费用报销归集开关）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖（**一处 pom 变更**）：`module-projects/erp-prj-service/pom.xml` 新增 compile 依赖 `app-erp-finance-service`（既承载 `IErpFinAcctDocProvider` 过账接口，又提供 `IErpFinExpenseClaimBiz` 只读查报销单——projects→finance 过账 S 写 + 报销单 R 读，均合法层，无环）；master-data-dao 经 projects-dao 传递可用。**不改 finance-service pom**（无 finance→projects 业务表写，对齐 matrix §3.2）。
- **无 ORM 变更**（不加实体/列/字典/枚举）：project/task/timesheet/budget/costCollection/activityType 表列齐备；businessType 复用既有 `PROJECT_COST_COLLECTION`。**故无 ask-first 保护区域门控**（纯服务层 + 既有表 + 一处 pom 依赖变更，不改实体/契约/finance 侧）。
- 无数据迁移；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — 工时成本计算 + 状态机 + 成本率解析 + PROJECT_COST_COLLECTION 过账 + 测试

Status: planned
Targets: `module-projects/erp-prj-service/.../entity/ErpPrjTimesheetBizModel.java`(扩)、`IErpPrjTimesheetBiz.java`(扩)、`ProjectCostCollectionProvider.java`(新, IErpFinAcctDocProvider for PROJECT_COST_COLLECTION)、`CostRateResolver.java`(新)、`ErpPrjErrors.java`(扩)、`ErpPrjConstants.java`(扩)、`ErpPrjConfigs.java`(扩)、`module-projects/erp-prj-service/pom.xml`(新 finance-service compile 依赖)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 过账 Provider 基座（done）；**Phase 1 起步落实 erp-prj-service→app-erp-finance-service compile 依赖**（`IErpFinAcctDocProvider` 接口宿主）。

- [ ] `Add`：`IErpPrjTimesheetBiz.submit`（DRAFT→SUBMITTED：校验项目 status=OPEN + 任务 status∈{TODO,IN_PROGRESS} + `CostRateResolver` 取费率 + `costAmount=hours×costRate` + 预算检查 Phase 2 接线占位）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`CostRateResolver` 解析 Timesheet.costRate → ActivityType.costRate → `erp-prj.default-labor-cost-rate`；皆无抛 `ERR_COST_RATE_NOT_AVAILABLE`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpPrjTimesheetBiz.approve`（SUBMITTED→APPROVED + 触发 `PROJECT_COST_COLLECTION` 过账 + posted=true）/ `reject`（SUBMITTED→DRAFT）/ `cancel`。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`ProjectCostCollectionProvider implements IErpFinAcctDocProvider`（businessType=`PROJECT_COST_COLLECTION`；createFacts 借项目成本科目(项目类型 defaultSubjectId)/贷应付职工薪酬科目(`erp-prj.default-payroll-subject-id`，为空抛 `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`)；按 sourceBillType 区分借方科目；凭证分录行标 projectId 辅助维度）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：成本率解析优先级 + 过账科目映射，见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：`TestErpPrjTimesheetCost`（成本率 单填>活动类型>默认>无费率抛错；submit 校验项目OPEN/任务允许；approve 过账凭证 借项目成本/贷应付职工薪酬 + posted + projectId 辅助维度；非法迁移抛错）。`mvn test -pl module-projects/erp-prj-service -am -Dtest=TestErpPrjTimesheetCost*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付工时状态机 + 成本率解析 + 过账。解除 Phase 2 归集汇总（工时 APPROVED 触发）+ 预算检查接线基线。

- [ ] 工时状态机 + 成本率解析 + PROJECT_COST_COLLECTION 过账单测通过

### Phase 2 — 预算控制 + 成本归集汇总 + 项目状态引用校验 + 测试

Status: planned
Targets: `ErpPrjBudgetBizModel.java`(扩)、`IErpPrjBudgetBiz.java`(扩)、`ErpPrjCostCollectionBizModel.java`(扩)、`IErpPrjCostCollectionBiz.java`(扩)、`ErpPrjProjectBizModel.java`(扩)、`IErpPrjProjectBiz.java`(扩)、`BudgetChecker.java`(新)、`ProjectCostAggregator.java`(新)、`ErpPrjConfigs.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（工时 APPROVED 触发归集）。

- [ ] `Add`：`BudgetChecker.check(projectId, addAmount)`——按 `erp-prj.budget-control-mode`（WARNING 警告放行 / STRICT 抛 `ERR_BUDGET_EXCEEDED`）；工时 submit 接线调用。
  - Skill: `nop-backend-dev`
- [ ] `Add`：工时 APPROVED → `ProjectCostAggregator` 生成/追加 `ErpPrjCostCollection` 行（costCategory=人工/sourceBillType=TIMESHEET/sourceBillCode/amount=costAmount/subjectId=活动类型科目）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpPrjProjectBiz.refreshActualCost(projectId)`（聚合归集行 → 回写 actualCost）/ `closeProject`（OPEN→COMPLETED，检查无未归集成本后冻结）/ 项目状态引用校验（OPEN 才可被新工时/报销引用，COMPLETED/CANCELLED 拒绝）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：归集触发时机（APPROVED 同事务）+ 预算控制模式（项目级 WARNING/STRICT），见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：`TestErpPrjBudgetAndCollection`（预算 WARNING 放行 + STRICT 超预算拦截；工时 APPROVED→归集行生成 + actualCost 回写；closeProject 冻结后新归集拒绝；引用校验非 OPEN 项目拒绝）。`mvn test -pl module-projects/erp-prj-service -am -Dtest=TestErpPrjBudgetAndCollection*`。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 预算控制 + 归集汇总 + actualCost 回写 + 项目状态引用校验单测通过

### Phase 3 — 费用报销归集（projects 驱动只读聚合）+ 端到端 + 文档/日志

Status: planned
Targets: `IErpPrjCostCollectionBiz.java`(扩 refreshExpenseCost)、`ErpPrjCostCollectionBizModel.java`(扩)、`ExpenseCostAggregator.java`(新, projects 侧只读查报销单+自写归集)、`ErpPrjProjectBizModel.java`(扩 closeProject 前刷新)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/projects/cost-collection.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1/2（归集汇总 + 预算检查基线 + projects→finance 依赖）；费用报销 done（0700-2）。

- [ ] `Add`：`IErpPrjCostCollectionBiz.refreshExpenseCost(projectId)`——`ExpenseCostAggregator` 经 `IErpFinExpenseClaimBiz` **只读**查询已审核 `ErpFinExpenseClaim`/`Line`（projectId 命中）→ 写/更新 `erp_prj_cost_collection` 行（costCategory=费用/sourceBillType=EXPENSE/sourceBillCode=报销单号/amount=行金额/subjectId=行科目，**projects 自写**）+ 预算检查 + refreshActualCost；幂等（按 sourceBillType+sourceBillCode 去重，已归集的不重复）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：触发接线——`closeProject` 关闭前强制调 `refreshExpenseCost`（保证关账费用完整）；另暴露为手动/批量 BizMutation 入口（`erp-prj.expense-aggregation-enabled` config-gated，关闭则 closeProject 跳过费用刷新）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：费用报销归集触发方向（projects 驱动只读 R + 自写，对齐 matrix §3.2/§4.2；rejected finance→projects 业务表写）+ 业务类型复用 PROJECT_COST_COLLECTION，见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：端到端 `TestErpPrjExpenseAggregation`（报销 approve（行带 projectId）→ projects refreshExpenseCost → 归集行生成 + actualCost 回写 + 预算检查 + 幂等去重；config-gated 关闭时 closeProject 跳过；非 OPEN 项目不归集）。`mvn test -pl module-projects/erp-prj-service -am -Dtest=TestErpPrjExpenseAggregation*`（报销单经 finance-service 测试 Bean 提供者构造）。
  - Skill: `nop-testing`
- [ ] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.6 标注 done；`cost-collection.md` 偏离补注：§2.2 成本率解析（单填>活动类型>默认，用户/角色级 Non-Goal）、§3 预算控制（项目级 config 而非预算头 controlMode 字段）、§4.1 费用归集触发（projects 驱动只读聚合，非 finance 回写——对齐 data-dependency-matrix）、§8 businessType 实际为 `PROJECT_COST_COLLECTION`（非 `PROJECT_LABOR_COST`）。
  - Skill: none

Exit Criteria:

> Phase 3 交付费用报销归集接入 + 端到端。完整仓库验证属 Closure Gates。

- [ ] 费用报销归集（config-gated）+ 端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0da319a70ffefBIK2TfCuAG8dg`，独立 general 子代理）。3 BLOCKER：(B1) `PROJECT_LABOR_COST` 在 `ErpFinBusinessType` 不存在（实际为 `PROJECT_COST_COLLECTION(110)`）；(B2) `IErpFinAcctDocProvider` 在 finance-service（非 finance-dao），须明确 erp-prj-service 加 finance-service compile 依赖；(B3) Phase 3 finance→projects 调用须将 finance-service pom 的 app-erp-projects-dao 由 test 提升为 compile，原 Targets 漏列。nits：hook 顺序未指定、贷方科目来源缺失、§3 预算模式偏差未补注、anti-slack「必要时」。**已修订**：复用 `PROJECT_COST_COLLECTION` 枚举（不新增，保持无契约变更）+ 新增 Decision 说明 design §8 命名偏差；Phase 1 Targets 加 erp-prj-service pom + finance-service 依赖；Phase 3 Targets 加 finance-service pom projects-dao test→compile；hook 改述 approve `tryPost` 之后；贷方科目加 `erp-prj.default-payroll-subject-id` config；§3 预算模式偏差并入 Phase 3 文档补注；移除「必要时」改为明确跨模块说明。Mission/Work Item 头按 mission-driver 规范保留。
- Independent draft review iteration 2: **needs revision**（`ses_0da1be13cffeZ2erBwmkN2CFQv`，独立 general 子代理）。iter-1 B1/B2/B3 均 resolved。**新 BLOCKER (B4)**：Phase 3 `ErpFinExpenseClaimBizModel.approve` 调 `collectFromExpense` 属 **finance→projects 业务表 S 写**，违反 `data-dependency-matrix.md §3.2:160`「finance 从不写业务表」+ `§4.4` 单向 S 写规则；matrix `§4.2:217` 建模成本归集为 **projects 触发** `confirmCollection()`。nits：Decision 残留风险缺失、DAG 框架低估 S 写。**已修订**：Phase 3 重设计为 **projects 驱动只读聚合**（projects `refreshExpenseCost` 经 `IErpFinExpenseClaimBiz` 只读 R 查报销单 + projects 自写 erp_prj_cost_collection + 幂等去重，closeProject 前强制刷新，config-gated）；移除 finance hook 与 finance-service pom 翻转（infra 改为一处 pom 变更）；新增 Decision（费用报销归集触发方向）含 matrix 依据 + rejected finance→projects 写 + 残留风险；补全各 Decision 残留风险；DAG 框架改述含 S 写/R 读分层；Goals/Phase 3 文档补注 §4.1 触发方向偏离。
- Independent draft review iteration 3: **accept / consensus**（`ses_0da01897dffepJ7ELJ1lJGMaKU`，独立 general 子代理）。B4 **确认已解决**：Phase 3 已重设计为 projects 驱动（`refreshExpenseCost` 经 `IErpFinExpenseClaimBiz` 只读 R 查报销单 + projects 自写 erp_prj_cost_collection，closeProject 前刷新 + config-gated）；无 finance→projects 业务表写；仅一处 pom 变更（erp-prj-service→finance-service），finance-service pom 不再翻 projects-dao scope（仍 test）；Decision 引用 matrix §3.2:160 + §4.2:217 并 reject finance→projects 写。iter-1 修复（PROJECT_COST_COLLECTION 复用 / Provider 在 finance-service）重确认 intact。projects→finance R 读与既有 projects→finance S 写同向，无 DAG 反转/无环/无 matrix 反模式。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成：工时成本（状态机+成本率解析+过账）+ 预算控制 + 归集汇总(actualCost 回写) + 项目状态引用校验 + 费用报销归集(config-gated)，行为测试通过
- [ ] 相关文档对齐：`extended-roadmap.md` 2.6 done；当日日志已记；`cost-collection.md §2.2` 偏离补注
- [ ] 已运行验证：`mvn test -pl module-projects/erp-prj-service -am`（+ finance 既有套件无回归）；根 `mvn clean install -DskipTests`
- [ ] 无范围内项目静默降级（采购/领料归集、资本化、开票、报表、多级费率、多级审批 均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 采购入库 / 库存领料 → 项目归集

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需改 purchase/inventory Processor 加项目归集钩子；本期聚焦工时 + 费用报销两来源。
- Successor Required: yes（触发条件：采购入库/领料标注项目归集需求落地时）

### 项目成本资本化（CIP→固定资产）/ 关闭结转损益

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖项目类型→科目完整配置 + CAPITALIZATION 过账联动 assets 域；属项目结转面。
- Successor Required: yes（触发条件：项目关闭成本结转 + 资本化需求时）

### 项目开票（里程碑结算触发发票）/ 成本报表 / 用户级·角色级费率配置实体 / 工时多级审批

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为独立深化面（开票属 3.12；报表属 nop-report；多级费率无载体本期不建；审批本期单级）。
- Successor Required: yes（触发条件：对应需求落地时）

## Closure

（待结束后填写）
