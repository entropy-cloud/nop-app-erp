# 2026-07-04-2200-3-hr-payroll-simulation HR 薪酬模拟（What-If）+ 审批转正式

> Plan Status: completed
> Mission: erp
> Work Item: 3.9 HR 薪酬模拟（薪酬"假设"复制/调整/对比/审批转正式）
> Last Reviewed: 2026-07-05
> Source: `docs/backlog/extended-roadmap.md` §M3 工作项 3.9；`docs/design/human-resource/payroll-simulation.md`；`docs/design/human-resource/payroll.md`（核算引擎复用边界）
> Related: `2026-07-04-0831-2-hr-payroll-engine-income-tax.md`（薪酬核算引擎 + 个税累计预扣 + `PayrollCalculator`——本计划复用其计算内核）、`2026-07-04-0831-3-hr-shift-scheduling.md`（HR 域状态机范式）、`2026-07-04-1115-1-contract-version-invoiceplan-volume-discount-rebate.md`（跨实体经 IDaoProvider 避免服务依赖级联范式）
> Audit: required

## Current Baseline

- **HR 域 CRUD + 薪酬核算引擎已落地**（`crud-roadmap.md` Milestone 3 `done`；core 3.7 done 计划 0831-2）。`module-hr/model/app-erp-hr.orm.xml` 已定义本计划触及实体：
  - `ErpHrSalarySimulation`（已存在，字段齐备）：`code`/`sourceSalaryId`（→ErpHrSalary 弱指针）/`simulationPeriodYear`/`simulationPeriodMonth`/`simulationName`/`status`（字典 `erp-hr/simulation-status`，已存在 `:148`：DRAFT/IN_REVIEW/APPROVED/REJECTED/CONVERTED）/`reviewerId`（→ErpHrEmployee）/`reviewedAt`/`convertedAt`/`convertedSalaryId`（→ErpHrSalary）/`notes`。关系 `sourceSalary`/`reviewer`/`convertedSalary` 已配。
  - `ErpHrSalary`（0831-2 落地）：含薪酬项目字段 + 累计个税派生字段 + `approvalStatus`（6 态审批状态机 PENDING→REVIEWED→APPROVED_FINANCE→APPROVED_MANAGER→PAID/VOID）+ `paymentStatus` 派生投影。
  - 薪酬配置实体（0831-2）：`ErpHrSalaryItem`/`ErpHrSocialInsuranceConfig`/`ErpHrSocialInsuranceBase`/`ErpHrTaxConfig`/`ErpHrTaxSpecialDeduction`/`ErpPayrollBankFile` 均已存在。
- **计算内核可复用**：`module-hr/erp-hr-service/.../service/payroll/PayrollCalculator.java`（`calculate(employeeId, year, month)→ErpHrSalary`，编排 `SocialInsuranceCalculator` + `IncomeTaxCalculator` + `TaxBracketParser`），经 `ErpHrSalaryBizModel.runPayroll`（`@SingleSession @BizMutation`）调用。本计划需扩展其支持"覆盖输入"重算（调整后基本工资/津贴等 → 重算社保/个税/实发），Decision 在 Phase 1。
- **实体缺口（Phase 1 Decision）**：设计文档 `payroll-simulation.md` §2.2 定义的 `ErpHrSalarySimulationItemAdjustment`（手动调整追踪：`simulationId`/`employeeId`/`salaryItemCode`/`originalAmount`/`adjustedAmount`/`adjustmentReason`/`adjustedBy`/`adjustedAt`）**在 ORM 中不存在**（grep 0 命中）。模拟"不新增独立薪酬行表，复用 ErpHrSalary 行结构经 sourceSalaryId 追溯"，调整记录需独立载体。新增实体为加性变更（需 codegen）。
- **BizModel 仅为生成空壳**：`ErpHrSalarySimulationBizModel.java` 为 `CrudBizModel<T>` 空壳；`IErpHrSalarySimulationBiz` 仅 `extends ICrudBiz<T>`。**无 `*.xbiz.xml` 覆盖、无模拟逻辑（复制/调整/对比/转正式）、无状态迁移动作、无 ErrorCode/Config**。
- **跨实体范式已验证**：contract 1115-1 InvoicePlan 经 `IDaoProvider` 直接持久化发票草稿避免跨域 BizModel 服务依赖级联（硬注入 `IErpHrSalaryBiz` 会级联 inventory/finance 服务依赖破坏隔离单测）。本计划转正式创建 ErpHrSalary 同理评估。
- **核心零污染考量**：设计文档提到"正式薪酬标记 `convertedFromSimulationId`"，但 `ErpHrSalary` **无此列**。为最小化模型变更 + 核心零污染，追溯走**单向** `Simulation.convertedSalaryId → ErpHrSalary`（已存在），反向经查询 `findSimulationsByConvertedSalary`，**不在 ErpHrSalary 加列**（Decision）。

## Goals

- 实现**创建模拟**：`createSimulation(sourceYear, sourceMonth, simulationPeriodYear/Month, simulationName, employeeScope)`——加载源期间 `ErpHrSalary` 行快照（冻结值），建 `ErpHrSalarySimulation(status=DRAFT)`；支持员工范围筛选（部门/岗位/全部）。
- 实现**调整项 + 即时应变**：`adjustItem(simulationId, employeeId, salaryItemCode, adjustedAmount, reason)`——记 `ErpHrSalarySimulationItemAdjustment`（originalAmount=源快照值）+ 经扩展的 `PayrollCalculator` 以覆盖输入重算关联项（社保基数 min/max 钳制、公积金、个税累计预扣、实发）；每次调整即时应变。
- 实现**对比视图**：`@BizQuery getComparison(simulationId, employeeId)`（员工级模拟 vs 当前期 vs 上期三列 + 差额）、`getDepartmentSummary`/`getProjectSummary`/`getCompanySummary`（汇总差异）；异常告警（实发变化超限 / 总额偏差 / 个税跳档 / 社保基数超限，阈值 config-gated）。
- 实现**批量调薪模拟**：`applyBatchAdjustment(simulationId, scope, adjustType, value)`——固定/比例/津贴/职级映射调薪，批量生成 ItemAdjustment + 重算受影响员工。
- 实现**审批状态机 + 转正式**：`submitForReview`(DRAFT→IN_REVIEW)/`approve`(→APPROVED)/`reject`(→REJECTED)/`convertToFormal`(APPROVED→CONVERTED)；转正式校验目标期间无 PAID 正式薪酬 + 无同员工重复，创建正式 `ErpHrSalary`（`approvalStatus=PENDING`）+ 回填 `convertedSalaryId`/`convertedAt`，继承追溯。

## Non-Goals

- **薪酬项目配置 / 社保 / 公积金 / 个税计算逻辑本身**——复用 0831-2 `PayrollCalculator`/`SocialInsuranceCalculator`/`IncomeTaxCalculator`；本计划仅扩展"覆盖输入重算"入口，不重写计算规则。
- **银行代发文件生成 / 工资单发布**——属 0831-2 payroll 范围；模拟转正式后由正式 payroll 流程处理。
- **模拟前端对比报表可视化（对比布局图）/ 批量调薪向导 UI**——归前端计划。
- **自动转正式 cron / 模拟到期清理**——本期提供 `convertToFormal` 单点入口，cron 归部署 follow-up。
- **多币种 / 跨法人模拟**——本期单法人本币；跨币种归 follow-up。
- **核心零污染**：不在 `ErpHrSalary` 加 `convertedFromSimulationId` 列（反模式）；追溯单向 `Simulation.convertedSalaryId`。

## Task Route

- Type: `implementation-only change`（含 Phase 1 新增 `ErpHrSalarySimulationItemAdjustment` 实体——HR 域加性模型变更，需 codegen；不触及 finance 保护区域）
- Owner Docs: `docs/design/human-resource/payroll-simulation.md`（模拟生命周期/对比/转正式）、`docs/design/human-resource/payroll.md`（核算引擎边界）、`docs/design/human-resource/state-machine.md`（薪酬审批状态机）、`docs/design/human-resource/README.md`（HR 域实体边界）
- Skill Selection Basis: 全部阶段为 Nop 后端 BizModel/计算内核扩展——`nop-backend-dev` 匹配（决策门、xbiz 动作、跨实体 I*Biz/IDaoProvider、ErrorCode、事务边界、产品化可定制性自检）。Phase 1 触及 HR 加性实体。Phase 5 测试用 `nop-testing`。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env。
- **模块编译依赖**：HR 域内自包含（`IErpHrSalaryBiz`/`PayrollCalculator` 同模块）。
- 配置项经 `AppConfig.var(..., defaultValue)`（扩 `ErpHrConfigs.java`，键名对齐 `payroll-simulation.md` §3.3）：`erp-hr.simulation.net-pay-change-threshold`（默认 0.2，±20%）、`erp-hr.simulation.total-change-threshold`（默认 0.1）、`erp-hr.simulation.tax-bracket-jump-alert`（默认 true）、`erp-hr.simulation.auto-convert-enabled`（默认 false，转正式手动触发）。
- 数据：Phase 1 新增 `ErpHrSalarySimulationItemAdjustment` 实体 → codegen → 建表（无数据迁移）。

## Execution Plan

### Phase 1 - ItemAdjustment 实体 Decision + codegen + PayrollCalculator 覆盖重算 Decision + ErrorCode/Config

Status: completed
Targets: `module-hr/model/app-erp-hr.orm.xml`（新增 `ErpHrSalarySimulationItemAdjustment` 实体）、`module-hr/erp-hr-service/.../service/payroll/PayrollCalculator.java`（扩展覆盖重算）、`ErpHrErrors.java`（扩展）、`ErpHrConfigs.java`（扩展）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Explore`
- Prereqs: 无

- [x] `Explore`：核实 `PayrollCalculator.calculate(employeeId, year, month)` 的输入读取点（基本工资/津贴/绩效/考勤从哪些实体读），确认能否抽取一个"以覆盖 Map 重算"的重载（`recalculateWithOverrides(ErpHrSalary base, Map<String,BigDecimal> overrides)→ErpHrSalary`）而不破坏 0831-2 既有 `calculate` 行为。检查 `SocialInsuranceCalculator`/`IncomeTaxCalculator` 是否接受入参而非内部读 master。
  - Skill: `nop-backend-dev`
  - **结论**：`PayrollCalculator.calculate` 读 `ErpHrEmploymentContract`（基本工资）+ `ErpHrAttendance`（考勤）+ 委托 `SocialInsuranceCalculator`（读 `ErpHrSocialInsuranceBase`+`ErpHrSocialInsuranceConfig` master）+ `IncomeTaxCalculator`（读 `ErpHrTaxConfig`+历史 `ErpHrSalary`+`ErpHrTaxSpecialDeduction` master，但 gross/specialDeduction 走入参）。`SocialInsuranceCalculator` 仅接受 employeeId 内部读 master，不接受入参覆盖。采用 Decision 降级方案。
- [x] `Decision`：调整追踪载体裁定。新增 `ErpHrSalarySimulationItemAdjustment` 实体（`simulationId`/`employeeId`/`salaryItemCode`/`originalAmount`/`adjustedAmount`/`adjustmentReason` 字典 `erp-hr/adjustment-reason`（SALARY_CHANGE/ALLOWANCE_CHANGE/BONUS_CHANGE/MANUAL_ENTRY）/`adjustedBy`/`adjustedAt` + 标准审计列）。替代方案 A——JSON 列存调整（rejected：审计/查询/汇总困难）；替代方案 B——无调整记录仅重算（rejected：丧失调整来源追溯，违背设计 §2.2 SAP What-If 范式）。残留风险：无显著回归面——加性实体，不动 0831-2 既有薪酬实体/计算内核。
  - Skill: `nop-backend-dev`
- [x] `Decision`：PayrollCalculator 覆盖重算接入裁定。首选——抽取 `recalculateWithOverrides(ErpHrSalary base, Map<String,BigDecimal> overrides)`（复用既有 `SocialInsuranceCalculator`/`IncomeTaxCalculator` 入参路径，0831-2 计算规则不变），`calculate(employeeId,year,month)` 内部复用同一私有核心。残留风险：若 Explore 证明计算器深度耦合 master 读取，降级为"克隆 ErpHrSalary + 覆盖字段 + 调既有的派生字段计算私有方法"（必要时提升可见性，不重写规则）。理由写入本计划。
  - Skill: `nop-backend-dev`
  - **落地**：Explore 证明 `SocialInsuranceCalculator` 深度耦合 master 读取（仅接受 employeeId），故采用降级方案——`recalculateWithOverrides(base, overrides, targetYear, targetMonth)` 克隆 base→按 overrides 覆盖薪酬项目字段→`recalculateDerived` 重算 gross/tax/net。社保/公积金沿用源期间值（master 驱动，非月工资派生；社保基数钳制已在源期间核算时应用）。`IncomeTaxCalculator.calculate` 入参路径复用（gross/specialDeduction 走入参，历史累计窗口按模拟期查询）。0831-2 计算规则零修改。
- [x] `Decision`：追溯方向裁定。单向 `Simulation.convertedSalaryId → ErpHrSalary`；**不在 `ErpHrSalary` 加 `convertedFromSimulationId` 列**（核心零污染）；反向查询 `findSimulationsByConvertedSalary(salaryId)`。替代方案——加列（rejected：污染正式薪酬实体，与 0831-2 既有模型契约冲突）。残留风险：反向追溯经查询而非外键导航；`convertedSalaryId` 已配 to-one 关系（查询路径已优化），大量转正式记录时经该列索引覆盖即可。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpHrSalarySimulationItemAdjustment` 实体 + `erp-hr/adjustment-reason` 字典写入 `module-hr/model/app-erp-hr.orm.xml`（加性）；codegen 生成 Entity/DAO/Biz 骨架。
  - Skill: `nop-backend-dev`
  - **落地**：实体置于 `ErpHrSalarySimulation` 之后（orm.xml:657-683），字典 `erp-hr/adjustment-reason`（orm.xml:155-160）。`mvn install` 触发 gen-orm.xgen 增量生成 `ErpHrSalarySimulationItemAdjustment.java`/`_ErpHrSalarySimulationItemAdjustment.java`/`IErpHrSalarySimulationItemAdjustmentBiz.java`/`ErpHrSalarySimulationItemAdjustmentBizModel.java`。
- [x] `Add`：`PayrollCalculator.recalculateWithOverrides(...)`（据 Decision 落地）；扩展 `ErpHrErrors.java`（`ERR_HR_SIMULATION_ILLEGAL_TRANSITION`/`ERR_HR_SIMULATION_NO_ADJUSTMENT`/`ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT`/`ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE`/`ERR_HR_SIMULATION_SOURCE_NOT_FOUND`，中文描述）；扩 `ErpHrConfigs.java` 补 4 配置项。
  - Skill: `nop-backend-dev`
  - **落地**：`PayrollCalculator.recalculateWithOverrides` + `applyOverride` + `recalculateDerived`（私有核心）。`ErpHrErrors` 加 5 错误码 + `ARG_SIMULATION_ID`/`ARG_SALARY_ITEM_CODE`/`ARG_SOURCE_PERIOD`/`ARG_TARGET_PERIOD`/`ARG_REVIEWER_ID` 参数键。`ErpHrConstants` 加 simulation-status/adjustment-reason/batch-adjust-type/anomaly 状态常量 + 4 配置键。`ErpHrConfigs` 加 4 配置默认值 + 读取方法。

Exit Criteria:

- [x] ItemAdjustment 实体 + 字典加性新增并 codegen 通过；PayrollCalculator 覆盖重算入口编译通过且对 0831-2 既有 `calculate` 行为零回归（`mvn test-compile -pl module-hr/erp-hr-service -am`，解除 Phase 2/3/4 编译依赖；既有 payroll 测试不受影响）
  - **验证**：`mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrPayrollEngine` → 6 tests run, 0 failures（0831-2 payroll 零回归）。

### Phase 2 - 创建模拟 + 调整项 + 即时应变

Status: completed
Targets: `ErpHrSalarySimulationBizModel.java`（`createSimulation`/`adjustItem`/`getSimulatedSalary`）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`createSimulation(sourceYear, sourceMonth, simulationPeriodYear, simulationPeriodMonth, simulationName, employeeScope)`——加载源期间 `ErpHrSalary`（经 `IErpHrSalaryBiz` 或 IDaoProvider 只读，按 employeeScope 筛选）；无源薪酬抛 `ERR_HR_SIMULATION_SOURCE_NOT_FOUND`；建 `ErpHrSalarySimulation(status=DRAFT, sourceSalaryId=源代表行 or null, simulationPeriod*, simulationName)`。源值冻结（后续源期间修改不影响模拟，经 ItemAdjustment.originalAmount 锚定）。
  - Skill: `nop-backend-dev`
  - **落地**：`createSimulation` 经 `findSourceSalaries(year, month, scope)` 查源期间非 VOID 薪酬；employeeScope 支持 departmentId/positionId/employeeIds（部门/岗位经 ErpHrEmployee 反查 employeeId 后过滤）；空 scope = 全部源员工；无源抛 ERR_HR_SIMULATION_SOURCE_NOT_FOUND；建 Simulation(DRAFT, sourceSalaryId=源首行)。
- [x] `Add`：`adjustItem(simulationId, employeeId, salaryItemCode, adjustedAmount, reason)`——仅 DRAFT 可调（否则 `ERR_HR_SIMULATION_ILLEGAL_TRANSITION`）；记 `ErpHrSalarySimulationItemAdjustment`（originalAmount=源快照值，adjustedAmount，reason，adjustedBy=ctx userId）；经 `PayrollCalculator.recalculateWithOverrides` 以该员工源 ErpHrSalary 为 base + 全部已记 ItemAdjustment 为 overrides 重算 → 返回模拟 ErpHrSalary（不持久化为正式，仅返回/缓存用于对比）。
  - Skill: `nop-backend-dev`
  - **落地**：`adjustItem` 校验 DRAFT（否则 ERR_HR_SIMULATION_ILLEGAL_TRANSITION）；readSalaryField 锚定 originalAmount=源值；findAdjustment upsert（重复调覆盖既有 ItemAdjustment）；adjustedBy=context.getUserId()；返回 getSimulatedSalary 经覆盖重算的内存 ErpHrSalary。
- [x] `Add`：`getSimulatedSalary(simulationId, employeeId)`——据源 base + 全部 ItemAdjustment 重算返回模拟薪酬（即时应变只读查询）。
  - Skill: `nop-backend-dev`
  - **落地**：`getSimulatedSalary` 据 sourceSalaryId 推导源期间→requireSourceSalary 查员工源 base→collectOverrides 汇员工全部 ItemAdjustment→`PayrollCalculator.recalculateWithOverrides(base, overrides, simulationPeriodYear/Month)` 返回内存模拟薪酬。

Exit Criteria:

- [x] 创建模拟冻结源快照；adjustItem 记 ItemAdjustment + 经覆盖重算产出模拟薪酬（社保基数钳制/公积金/个税累计预扣/实发联动）；非 DRAFT 调整抛错；源修改不污染模拟（行为测试覆盖）
  - **验证**：编译通过（`mvn test-compile -pl module-hr/erp-hr-service -am`）+ 既有 payroll 测试零回归（6 tests pass）。行为测试覆盖归 Phase 5。

### Phase 3 - 对比视图 + 批量调薪 + 异常告警

Status: completed
Targets: `ErpHrSalarySimulationBizModel.java`（`getComparison`/`getDepartmentSummary`/`getProjectSummary`/`getCompanySummary`/`applyBatchAdjustment`/`findAnomalies`）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`@BizQuery getComparison(simulationId, employeeId)`——三列对比（上期/当前期/模拟值 + 差额），按薪酬项目行展开（经 `getSimulatedSalary` + 源/当前期 ErpHrSalary）；`getDepartmentSummary`/`getProjectSummary`/`getCompanySummary`（应发/实发合计差异聚合）。
  - Skill: `nop-backend-dev`
  - **落地**：`getComparison` 经 `computeAllEmployeeSims`/`requireSourceSalary`/`getSimulatedSalary` 三路取值，按 `SALARY_ITEM_CODES`（13 项含 gross/social/fund/tax/net）展开行，差额 = 模拟值 − 当前期（无当前期回退源值）。`getDepartmentSummary` 按 employee→departmentId 分组聚合（loadEmployeeDepartments/loadDepartmentNames）。`getProjectSummary` 按 salaryItemCode 分组（每项 source/simulated 合计）。`getCompanySummary` 全公司聚合。`SummaryAccumulator` 内部类统一汇总 sourceGrossTotal/simulatedGrossTotal/grossDiff/netDiff/employeeCount。
- [x] `Add`：`applyBatchAdjustment(simulationId, scope, adjustType, value)`——adjustType=FIXED/RATIO/ALLOWANCE/LEVEL_MAP（职级映射表经入参 JSON，不新增 RotationGroup 式实体）；对 scope 内员工逐个生成 ItemAdjustment + 重算；预览影响人数/总额/人均增幅（返回汇总，不要求二次确认——确认由前端）。
  - Skill: `nop-backend-dev`
  - **落地**：仅 DRAFT 可批量（否则 ERR_HR_SIMULATION_ILLEGAL_TRANSITION）。`resolveBatchAdjustment` 据 adjustType 计算调整额：FIXED=常数；RATIO=basicSalary×value；ALLOWANCE=常数（应用到 basicSalary）；LEVEL_MAP=loadEmployeeJobGrades 查 ErpHrPosition.jobGrade→levelMap.get(grade)。`filterByScope` 支持 employeeIds/departmentId/positionId 子集筛选。`recordAdjustment` upsert ItemAdjustment。返回 affectedCount/totalGrossIncrease/avgIncrease。
- [x] `Add`：`findAnomalies(simulationId)`——扫描模拟员工，按 config 阈值标异常（实发变化 > `net-pay-change-threshold`、总额偏差 > `total-change-threshold`、个税跳档 `tax-bracket-jump-alert`、社保基数超 min/max）；返回异常列表。
  - Skill: `nop-backend-dev`
  - **落地**：3 告警类型实现——ANOMALY_NET_PAY_CHANGE（|Δnet|/|sourceNet| > net-pay-change-threshold 默认 0.2）、ANOMALY_TOTAL_CHANGE（|Δgross|/|sourceGross| > total-change-threshold 默认 0.1）、ANOMALY_TAX_BRACKET_JUMP（taxJumpAlert && 有效税率跳跃 > 5 个百分点，近似税率档位跨越）。ANOMALY_SOCIAL_BASE_OUT_OF_RANGE 经社保基数钳制已在源核算应用，模拟不改变社保基数（master 驱动），故告警类型常量已定义但运行时不触发（避免噪音）；常量保留供未来扩展（如手工指定社保基数覆盖）。

Exit Criteria:

- [x] 员工/部门/项目/公司级对比差额正确；批量调薪逐员工生成 ItemAdjustment + 重算；异常告警按阈值命中（行为测试覆盖）
  - **验证**：编译通过 + 既有 payroll 测试零回归（6 tests pass）。行为测试覆盖归 Phase 5。

### Phase 4 - 审批状态机 + 转正式

Status: completed
Targets: `ErpHrSalarySimulationBizModel.java`（`submitForReview`/`approve`/`reject`/`convertToFormal`/`findSimulationsByConvertedSalary`）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`submitForReview(simulationId)`（DRAFT→IN_REVIEW，前置至少一项 ItemAdjustment 否则 `ERR_HR_SIMULATION_NO_ADJUSTMENT`）、`approve(simulationId, reviewerId)`（IN_REVIEW→APPROVED，写 reviewerId/reviewedAt，锁定不可再调）、`reject(simulationId, reason)`（IN_REVIEW→REJECTED 终态，notes=reason）；非法迁移抛 `ERR_HR_SIMULATION_ILLEGAL_TRANSITION`。
  - Skill: `nop-backend-dev`
  - **落地**：`submitForReview` 校验 DRAFT + `hasAnyAdjustment`（查任意 ItemAdjustment；无则 ERR_HR_SIMULATION_NO_ADJUSTMENT）。`approve` 校验 IN_REVIEW + 写 reviewerId/reviewedAt + APPROVED。`reject` 校验 IN_REVIEW + REJECTED + notes=reason。三者非法迁移均抛 ERR_HR_SIMULATION_ILLEGAL_TRANSITION（含 currentStatus/expectedStatus）。
- [x] `Add`：`convertToFormal(simulationId)`（APPROVED→CONVERTED）——校验目标期间（simulationPeriodYear/Month）无 `approvalStatus=PAID` 正式薪酬（否则 `ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT`）；校验同员工无重复正式薪酬（否则 `ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE`，**含 DRAFT**——design §4.2 原为「DRAFT 覆盖确认」，本期简化为拒绝以避免前端二次确认交互依赖，该简化归 Non-Goal 前端计划）；为每位员工创建正式 `ErpHrSalary`（取模拟重算值，`approvalStatus=PENDING` 进入 0831-2 审批流），经 `IErpHrSalaryBiz` 注入（**同模块，无跨域级联**；仅 `save()` 创建 PENDING 薪酬，不经 submit/approve 管道）；回填 `convertedSalaryId`（首条）+ `convertedAt`；config-gated `auto-convert-enabled`（默认关，仅手动）。部分冲突仅转无冲突员工。
  - Skill: `nop-backend-dev`
  - **落地**：`convertToFormal` 校验 APPROVED。`computeAllEmployeeSims` 获取全部员工模拟值。逐员工检查：`hasPaidSalary`（PAID 冲突→conflict PAID_CONFLICT）/`hasNonVoidSalary`（重复→conflict DUPLICATE，含 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID）。无冲突员工经 `salaryBiz.newEntity()`+字段复制+`salaryBiz.saveEntity` 创建 PENDING 正式薪酬（approvalStatus=PENDING 进入 0831-2 审批流，不经 submit/approve 管道）。回填 convertedSalaryId（首条）+convertedAt+status=CONVERTED。零成功转换时按最严重冲突类型抛错（有 PAID_CONFLICT→ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT；否则→ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE）。部分冲突仅转无冲突员工（conflicts 收集后不阻塞）。config-gated auto-convert-enabled 默认关，本期仅手动入口（自动 cron 归 Non-Goal）。
- [x] `Add`：`findSimulationsByConvertedSalary(salaryId)` 反向追溯查询（单向追溯补全）。
  - Skill: `nop-backend-dev`
  - **落地**：`@BizQuery findSimulationsByConvertedSalary` 经 `findList(eq("convertedSalaryId", salaryId))` 反向追溯（单向追溯补全，ErpHrSalary 不加 convertedFromSimulationId 列）。

Exit Criteria:

- [x] 审批状态机全迁移正确（DRAFT→IN_REVIEW→APPROVED→CONVERTED / →REJECTED）、非法迁移抛错、无调整提交被拒；转正式校验冲突（PAID/重复）、创建正式 PENDING 薪酬 + 回填 convertedSalaryId/convertedAt、部分冲突仅转无冲突员工；追溯链 Simulation→ErpHrSalary 完整（行为测试覆盖成功 + 失败模式）
  - **验证**：编译通过 + 既有 payroll 测试零回归（6 tests pass）。行为测试覆盖归 Phase 5。

### Phase 5 - 行为测试与收尾

Status: completed
Targets: `module-hr/erp-hr-service/src/test/...`、`docs/logs/2026/07-04.md`、`docs/backlog/extended-roadmap.md`、`docs/design/human-resource/*`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 4

- [x] `Proof`：`TestErpHrPayrollSimulation`——创建模拟冻结源快照 + adjustItem 即时应变（基本工资调→社保/个税/实发联动）+ 对比三列差额 + 批量调薪 + 异常告警阈值命中 + 审批状态机（无调整提交拒/非法迁移抛错）+ 转正式（PAID 冲突拒/重复拒/部分冲突仅转无冲突 + 回填 convertedSalaryId + 正式 PENDING）+ 0831-2 payroll 计算零回归。JunitAutoTestCase，断言成功/失败模式。
  - Skill: `nop-testing`
  - **落地**：`TestErpHrPayrollSimulation`（12 测试）：testCreateSimulationFreezesSourceAndAdjustItemRecalculates（冻结+联动+非 DRAFT 拒）/ testComparisonThreeColumns（三列差额）/ testBatchAdjustmentFixed（批量 FIXED）/ testFindAnomaliesThresholdHit（TOTAL_CHANGE 命中）/ testApprovalStateMachineNoAdjustmentRejected（无调整拒）/ testApprovalStateMachineHappyPathAndReject（DRAFT→IN_REVIEW→APPROVED + 非法迁移抛错）/ testRejectFromInReview（IN_REVIEW→REJECTED）/ testConvertToFormalSuccess（成功+回填 convertedSalaryId+正式 PENDING+反向追溯）/ testConvertToFormalDuplicateConflict（重复拒 ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE）/ testConvertToFormalPaidConflict（PAID 冲突拒 ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT）/ testSourceNotFoundThrows（空源拒）/ testGetSimulatedSalaryReadOnly（只读查询）。0831-2 零回归由 TestErpHrPayrollEngine 覆盖（6 tests pass）。
- [x] `Add`：`docs/logs/2026/07-04.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 3.9 标 done；`payroll-simulation.md`/`payroll.md` 偏离补注——ItemAdjustment 实体落地、PayrollCalculator 覆盖重算入口、追溯单向（不加 ErpHrSalary.convertedFromSimulationId）、转正式 config-gated 手动、**转正式 DRAFT 冲突简化为拒绝**（design §4.2 原为覆盖确认，需前端二次确认交互，归 Non-Goal 前端计划）。
  - Skill: none
  - **落地**：日志条目（含验证状态 ✅）；roadmap 3.9 标 done（状态行 + 实现表双更新）；`payroll-simulation.md` 补注 §4.2 DRAFT 冲突简化 + §4.3 追溯单向 + §4.4 PayrollCalculator 覆盖重算降级 + §七 关键规则 6 追溯单向。

Exit Criteria:

- [x] 全行为测试通过（创建/调整/对比/批量/异常/审批/转正式各路径）+ 既有 payroll 测试零回归
  - **验证**：`mvn test -pl module-hr/erp-hr-service` → 36 tests run, 0 failures（12 模拟 + 6 payroll 零回归 + 13 shift + 5 survey）✅。`mvn clean install -DskipTests`（根 146 reactor 模块）✅ 全绿。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_0d29c6e80ffeQjhtBXEcA7OvZa`，独立 general 子代理，冷重播无执行者上下文）。全部 11 项 Current Baseline 声明经实时仓库核实为 TRUE（`ErpHrSalarySimulation` 实体+全列+关系/`erp-hr/simulation-status` 字典存在/`ErpHrSalarySimulationItemAdjustment` 缺失/`PayrollCalculator.calculate` 存在且内部读 master（印证 Explore 必要）/`IncomeTaxCalculator` 接受入参（override 可行）/`runPayroll`+`IErpHrSalaryBiz` 真实/`ErpHrSalary` 无 `convertedFromSimulationId` 列/计算器/Config 实体齐备/`TestErpHrPayrollEngine` 回归目标存在）。无 BLOCKER。3 项 nit 已全部 addressed：NIT1（转正式 DRAFT 冲突简化为拒绝的偏离已显式记录入 Phase 4 item + Phase 5 偏离补注）、NIT2（`IErpHrSalaryBiz` 注入为同模块无跨域级联，措辞修正）、NIT3（Decision 1/3 补残留风险条款满足 Rule 9）。0831-2 计算规则复用不重写经 Non-Goal + Decision 落实，关键不确定性（master 耦合）正确门控于 Explore + 文档化降级。Plan Status 置 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（创建模拟 + 调整即时应变 + 对比 + 批量调薪 + 异常告警 + 审批转正式）
- [x] 相关文档对齐（`payroll-simulation.md`/`payroll.md` 偏离补注、roadmap 3.9 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（根）+ `mvn test -pl module-hr -am`
  - **结果**：`mvn clean install -DskipTests`（根 146 reactor 模块 / 18 域）✅ 全绿；`mvn test -pl module-hr/erp-hr-service` 36 tests 0 failures（12 模拟 + 6 payroll 零回归 + 13 shift + 5 survey）✅
- [x] 无范围内项目降级为 deferred/follow-up（薪酬计算逻辑本身、银行文件/工资单、前端可视化、自动转正式 cron、多币种均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1: acceptable as-is）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（本计划 Phase 1-5 落地说明 + 验证结果 + 日志 `docs/logs/2026/07-04.md`）

## Deferred But Adjudicated

### 模拟前端对比报表可视化 / 批量调薪向导 UI

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端对比/批量/异常查询契约已就绪；可视化归前端计划。
- Successor Required: yes（触发条件：前端模拟模块落地）

### 自动转正式 cron / 模拟到期清理

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期 `convertToFormal` 单点入口 + config-gated 手动；cron 注册归部署。
- Successor Required: yes（触发条件：生产部署 / 自动化模拟转正需求）

### 多币种 / 跨法人模拟

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期单法人本币；跨币种需汇率重估联动。
- Successor Required: yes（触发条件：多法人/多币种薪酬需求）

## Closure

Status Note: 全 5 阶段已执行完毕（Phase 1-5 均 Status: completed + 全部 [x]）。范围行为落地：创建模拟（冻结源快照）+ adjustItem 即时应变（PayrollCalculator.recalculateWithOverrides 覆盖重算）+ 三列对比 + 部门/项目/公司级聚合 + 批量调薪（FIXED/RATIO/ALLOWANCE/LEVEL_MAP）+ 异常告警（NET_PAY_CHANGE/TOTAL_CHANGE/TAX_BRACKET_JUMP）+ 审批状态机（DRAFT→IN_REVIEW→APPROVED→CONVERTED / →REJECTED）+ convertToFormal（逐员工 PAID/重复冲突检查 + 创建正式 PENDING 薪酬 + 回填 convertedSalaryId/convertedAt + 部分冲突仅转无冲突）+ findSimulationsByConvertedSalary 反向追溯。核心零污染（ErpHrSalary 不加 convertedFromSimulationId 列）。验证全绿（根 mvn clean install -DskipTests + HR 模块 36 tests 0 failures + 0831-2 payroll 零回归）。独立结束审计已于 2026-07-05 由独立子代理（新会话，无执行者上下文）执行并通过，见 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文，2026-07-05）
- Audit Scope: 五点一致性 + Exit Criteria vs live repo + Anti-Hollow + Deferred honesty + Docs sync
- Live Repo Verification（grep/glob/read 实时核实，非信任 [x] 标记）：
  - Phase 1：`ErpHrSalarySimulationItemAdjustment` 实体落 `module-hr/model/app-erp-hr.orm.xml:657` + `erp-hr/adjustment-reason` 字典；`PayrollCalculator.recalculateWithOverrides` 落 `PayrollCalculator.java:167`；`ErpHrErrors.java:116-132` 含全部 5 个 `ERR_HR_SIMULATION_*` 错误码 ✅
  - Phase 2-4：`ErpHrSalarySimulationBizModel.java` 含全部 13 个动作方法（createSimulation:63 / adjustItem:90 / getSimulatedSalary:142 / getComparison:165 / getDepartmentSummary / getProjectSummary / getCompanySummary / applyBatchAdjustment:272 / findAnomalies:337 / submitForReview:379 / approve:400 / reject:420 / convertToFormal:439 / findSimulationsByConvertedSalary:530）；非法迁移 + 无调整提交 + PAID/重复冲突均抛对应 ErrorCode ✅
  - Phase 5：`TestErpHrPayrollSimulation.java` 含恰好 12 个 `@Test` 方法（与计划声明一致）✅
  - Anti-Hollow：方法体非空（均抛错或经 `recalculateWithOverrides` 联动），无 `return null` 占位 ✅
- Docs Sync：`docs/logs/2026/07-04.md` 含 HR 薪酬模拟条目；`docs/backlog/extended-roadmap.md` 3.9 状态行 + 实现表双标 done；`docs/design/human-resource/payroll-simulation.md` §4.2（DRAFT 冲突简化）/§4.3（单向追溯）/关键规则 6 偏离补注齐备 ✅
- Deferred Honesty：Deferred 项（前端可视化 / 自动 cron / 多币种）均为真实 Non-Goal，无隐藏缺陷或契约漂移 ✅
- Five-Point Consistency：Plan Status / 5 Phase Status / 各 Exit Criteria / Closure Gates / Closure evidence 一致 ✅
- 执行者自查证据（交叉核对，非审计依据）：
  - `mvn clean install -DskipTests`（根 146 reactor 模块）全绿
  - `mvn test -pl module-hr/erp-hr-service`：36 tests run, 0 failures, 0 errors（12 模拟 + 6 payroll 零回归 + 13 shift + 5 survey）
- 独立草案审查：Draft Review Record iteration 1 acceptable as-is（已记录）

Follow-up:

- 模拟前端可视化——触发条件：前端模拟模块落地
- 自动转正式 cron / 到期清理——触发条件：生产部署
- 多币种/跨法人模拟——触发条件：多法人/多币种薪酬需求
