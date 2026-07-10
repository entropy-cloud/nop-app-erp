# 2026-07-10-1100-4-budget-management 预算管理（编制/控制/对比）

> Plan Status: active
> Last Reviewed: 2026-07-10 (iteration 2 — consensus)
> Source: `docs/design/finance/budget.md`（107 行完整设计）+ use-case 审计 UC-FIN-11/13 🔶
> Related: `2026-07-02-0700-2` Follow-up（预算控制不存在）；`core-business-roadmap.md` 无对应工作项
> Audit: required

## Current Baseline

### 已实现

- **`ErpFinVoucher.postingType` 字段已存在**（VARCHAR 20，字典 `erp-fin/posting-type`：NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL）。`module-finance/model/app-erp-finance.orm.xml:270`
- **`ErpFinVoucherLine` 是权威本期发生额来源**：过账引擎**不维护** `ErpFinGlBalance`（`ProfitLossClosingService`/`AnnualCloseService`/`BadDebtProvisionService` 注释明确"GlBalance 在当前阶段未由过账引擎维护，故以 VoucherLine 为权威本期发生额来源"）。损益结转 / 坏账准备 / 年度结转从 `ErpFinVoucherLine` 聚合；报表/看板从 `ErpFinGlBalance` 读取（GlBalance 仅由年度结转写入快照）。
- **`ErpMdCostCenter` 实体已落地**（含 `isBudgetable` 列，`master-data.orm.xml:1002`）
- **`ErpMdSubject.isBudgetable` 列已存在**（`master-data.orm.xml:830`，propId 15，BOOLEAN 默认 false）——无需新增
- **`ErpFinVoucherLine` 含 `costCenterId` 维度**（`finance.orm.xml:351`），预算控制可按成本中心粒度从凭证行聚合
- **项目域预算控制**（`ErpPrjBudget`/`ErpPrjBudgetLine` + WARNING/STRICT）已 done（extended-roadmap 2.6）——但这是项目级预算，**非** finance 域预算管理
- **期间控制** `ErpFinAccountingPeriodStatus.glStatus` 已实现，预算凭证可复用
- **费用报销**已预留预算门控钩子点（`erp-fin.expense-budget-check-enabled`，默认 false，`ErpFinConstants.java:43`）

### 剩余差距

- **`ErpFinBudgetScenario`/`ErpFinBudgetLine`/`ErpFinBudgetControlLog` 三个设计实体均未落地**（ORM 中无定义）
- **`erp-fin/posting-type` 字典无 BUDGET 值**（现有 5 值面向凭证用途分类）
- **`IErpFinBudgetControlBiz` SPI 未定义**（全仓 grep 零命中）
- **预算编制/控制/对比**全链路零实现零测试（use-case 审计 UC-FIN-11/13 🔶）
- **【关键前置缺陷】实际数聚合未按 postingType 隔离预算凭证**：`ProfitLossClosingService.findPostedVoucherIds`（:167-173）、`BadDebtProvisionService.findPostedVoucherIds`（:128）筛选 POSTED+非红冲凭证时**无 postingType 过滤**。一旦引入 `postingType=BUDGET` 的预算凭证，其损益类行会被错误结转进实际损益 / 坏账余额，污染真实财务。本计划必须先修复此隔离缺口（见 Phase 2 Fix 项）。

### 设计来源

`docs/design/finance/budget.md`（107 行）完整定义了：
- **范式**：预算作为 `PostingType=BUDGET` 的"影子凭证"，与实际凭证并行入账，复用凭证引擎/GlBalance/试算平衡
- **3 个实体**：`ErpFinBudgetScenario`（方案，含状态机 + 控制级别）、`ErpFinBudgetLine`（明细行，科目×期间×维度）、`ErpFinBudgetControlLog`（控制日志）
- **8 条业务规则**：审批即过账、控制钩子位置、承付款生成、实际数派生、预算对比、期间控制、多账套独立、HARD 级别硬拦截
- **参考**：iDempiere `Fact.java:78-84` 四种 PostingType

### 对标依据

| 开源 ERP | 预算管理 | 状态 |
|----------|---------|------|
| **iDempiere** | GL Budget（PostingType=BUDGET 影子凭证范式） | 核心内置 |
| **Metasfresh** | 预算编制 + 控制 | 核心内置 |
| **赤龙 ERP** | 无预算模块（反向印证） | — |
| **本项目** | 设计完备但零实现 | **gap** |

## Goals

- 实现预算编制（方案 + 明细行），支持审批后生成 BUDGET 影子凭证（postingType=BUDGET）
- **前置修复**：实际数聚合（损益结转/坏账/年报）隔离 BUDGET 凭证，确保预算凭证不污染真实财务
- 实现预算控制（HARD/WARN/NONE 三级），在采购订单/付款审核时同步校验预算余量（余额从 VoucherLine 按 postingType 聚合）
- 实现预算对比报表（预算 vs 实际 vs 余量），复用凭证行按关联凭证 postingType 分组
- 前端 CRUD 页面替换占位页

## Non-Goals

- **承付款（Commitment）完整生命周期**——采购订单 APPROVED 生成 COMMITMENT 凭证 + CANCELLED/invoiced 红冲 COMMITMENT。本期仅实现 BUDGET 影子凭证 + ACTUAL 实际数对比；承付款机制归 successor（设计已登记规则 3）
- **多级 `.xwf` 预算审批工作流链**——本期审批经 DIRECT 模式 + 状态机
- **滚动预算自动复制**——本期仅手动创建方案；滚动预算自动从年度预算派生归 successor
- **预算调整审批版本链**（`parentScenarioId` 版本链 + 差异对比）——本期支持字段但版本链管理归 successor
- **跨账套预算聚合**——本期单账套独立预算

## Task Route

- Type: `app-layer design change`（新增 ORM 实体 + 扩展凭证 postingType 字典 + 新增跨域 SPI + **前置修复实际数聚合隔离 BUDGET 凭证**）
- Owner Docs: `docs/design/finance/budget.md`（权威设计）、`docs/design/finance/posting.md`（IErpFinFactsValidator 扩展点）
- Skill Selection Basis: 新增 ORM 实体 + BizModel + 跨域 SPI + 聚合隔离修复 → nop-backend-dev；GraphQL Engine 测试 → nop-testing
- 保护区域：`module-finance/model/*.orm.xml` 模式属 ask-first 保护区域（`docs/context/ai-autonomy-policy.md:69`）。本计划新增 3 实体 + 扩展凭证字典属加性变更，经 mission-driver 授权的 plan-first 路线；**不修改** `ErpFinGlBalance` / `ErpFinVoucherLine` / `ErpMdSubject` 结构（isBudgetable 列已存在），降低保护区域风险。`ProfitLossClosingService`/`BadDebtProvisionService` 聚合过滤修复属 finance 服务层行为变更，需在计划审计中确认。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - ORM 模型变更：预算实体 + postingType 字典扩展

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`
Skill: nop-backend-dev

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] Decision: `ErpFinVoucher.postingType` 字典扩展策略（仅凭证层，不动 GlBalance）
  - 现状：`erp-fin/posting-type` 字典有 5 值（NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL），面向**凭证用途分类**
  - 设计文档要求：新增 BUDGET/COMMITMENT 值用于预算分类
  - **选择扩展现有字典**：新增 `BUDGET`（本期范围）/`COMMITMENT`（Deferred）两个值到 `erp-fin/posting-type`。预算凭证 postingType=BUDGET；实际凭证保持 NORMAL（=ACTUAL）
  - **不动 `ErpFinGlBalance`**：过账引擎本就不维护 GlBalance（baseline 已核实），预算余额/实际余额/可用余额统一从 `ErpFinVoucherLine` 按关联 `ErpFinVoucher.postingType` 聚合（与损益结转/坏账/报表/看板同权威源），避免引入 GlBalance 结构变更这一最高风险项
  - 替代方案 A：GlBalance 新增 postingType 列 + 唯一键——rejected，过账引擎不写 GlBalance，新增列无人维护且破坏年度结转快照语义，且 GlBalance 无 costCenterId 维度无法支撑成本中心粒度预算控制
  - 替代方案 B：新增独立列 `postingCategory`(ACTUAL/BUDGET/COMMITMENT)——rejected，增加正交维度复杂度且现有凭证均为 ACTUAL=NORMAL 可推断
  - 残留风险：OPENING_BALANCE/ADJUSTMENT 等值理论上既可是实际也可是预算（如预算调整）——本期约束 BUDGET 仅用于预算凭证，其余值不混用
  - Skill: nop-backend-dev

- [ ] Add: `erp-fin/posting-type` 字典新增 `BUDGET` 值（`COMMITMENT` 值登记但本期不使用，归 Deferred）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetScenario`（预算方案头）
  - 字段（对照 `budget.md:17-33`）：id/code/name/orgId, acctSchemaId, fiscalYear, scenarioType(字典 `erp-fin/budget-scenario-type`: ANNUAL/ROLLING/ADJUSTMENT), parentScenarioId(nullable), validFrom/validTo, currencyId/exchangeRate/amountSource/amountFunctional, controlLevel(字典 `erp-fin/budget-control-level`: NONE/WARN/HARD), docStatus(字典 `erp-fin/budget-status`: DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED), approveStatus(共用 `erp-fin/approve-status`), 标准审计字段
  - 关系：to-one acctSchema/currency/org/parentScenario；to-many lines(→ErpFinBudgetLine)
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetLine`（预算明细行）
  - 字段（对照 `budget.md:37-53`）：id/scenarioId/lineNo/orgId, acctSchemaId, periodId(→ErpFinAccountingPeriod), subjectId/subjectCode, costCenterId(nullable,→ErpMdCostCenter), departmentId/projectId/partnerId/warehouseId/materialId(均 nullable 辅助维度), budgetAmountSource/budgetAmountFunctional, currencyId/exchangeRate, 标准审计字段
  - 关系：to-one scenario/period/subject/costCenter + 辅助维度 to-one
  - 注意：commitmentAmount/actualAmount/availableAmount **不落库**（派生，查询时从 `ErpFinVoucherLine` 按关联凭证 postingType 聚合计算）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetControlLog`（预算控制日志）
  - 字段（对照 `budget.md:55-68`）：id/orgId/businessDate, scenarioId/budgetLineId, sourceBillType/sourceBillCode, subjectId/costCenterId/projectId/periodId, requestedAmount, committedAmount, actionResult(字典 `erp-fin/budget-action`: PASS/WARNED/BLOCKED), operatorId/operatedAt/reason, 标准审计字段
  - Skill: nop-backend-dev

- [ ] Add: 新增字典 `erp-fin/budget-scenario-type`、`erp-fin/budget-control-level`、`erp-fin/budget-status`、`erp-fin/budget-action`
  - Skill: nop-backend-dev

- [ ] Add: 执行 `mvn clean install -DskipTests`（module-finance 链）触发增量代码生成
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] ORM 变更后 `mvn clean install -DskipTests`（module-finance 链）BUILD SUCCESS
- [ ] `ErpFinBudgetScenario`/`ErpFinBudgetLine`/`ErpFinBudgetControlLog` Entity/DAO 生成
- [ ] `erp-fin/posting-type` 字典含 BUDGET 值
- [ ] **`ErpFinGlBalance` 无任何结构变更**（本计划不动该实体）

### Phase 2 - 预算编制 BizModel + 审批过账引擎

Status: planned
Targets: `module-finance/erp-fin-service/`
Skill: nop-backend-dev

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1

- [ ] Fix: 实际数聚合隔离 BUDGET 凭证（**关键前置安全修复，必须先于 BUDGET 凭证引入完成**）
  - 缺陷：`ProfitLossClosingService.findPostedVoucherIds`（:167-173）、`BadDebtProvisionService.findPostedVoucherIds`（:128）筛选 POSTED+非红冲凭证时无 postingType 过滤。引入 BUDGET 凭证后，其损益/费用类行会被错误结转进实际损益 / 坏账余额，污染真实财务（违反设计 `budget.md` 规则 4/6/8：实际数仅来自 ACTUAL 凭证）
  - 修复：在这些方法及任何"实际数"聚合点增加 `postingType != BUDGET`（或显式 `postingType = NORMAL`）过滤；同步检查 `AnnualCloseService`、试算平衡、财务看板/报表的 VoucherLine 聚合是否需要同等隔离
  - Skill: nop-backend-dev

- [ ] Proof: BUDGET 凭证隔离回归测试 `TestErpFinBudgetIsolation`
  - 场景：在期间内创建一张 postingType=BUDGET 的损益类凭证（POSTED）→ 运行损益结转 → 断言 BUDGET 凭证行**未**进入结转金额；运行坏账余额派生 → 断言 BUDGET 凭证行**未**计入
  - 此回归证明实际财务不受 BUDGET 凭证污染，是预算凭证安全引入的门控测试
  - Skill: nop-testing

- [ ] Add: `ErpFinBudgetScenarioBizModel`（CrudBizModel）
  - 标准 CRUD + 三轴状态机（DRAFT→SUBMITTED→APPROVED / →REJECTED→DRAFT / APPROVED→CANCELLED）
  - `@BizMutation approve`：审核时生成 BUDGET 影子凭证
  - Skill: nop-backend-dev

- [ ] Add: `BudgetVoucherGenerator`（预算凭证生成器，`erp-fin-service/.../support/`）
  - 审核通过时：遍历 `ErpFinBudgetLine`，按 `subject.direction` 自动取借贷方向（资产/费用→借方，负债/收入→贷方）
  - 创建 `ErpFinVoucher`(postingType=BUDGET) + `ErpFinVoucherLine`（每预算行→一凭证行，携带 costCenterId 维度）
  - 凭证走正常 `DRAFT → POSTED` 流程；预算余额从 `ErpFinVoucherLine`（关联凭证 postingType=BUDGET）聚合，**不写 `ErpFinGlBalance`**（过账引擎本就不维护 GlBalance）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetScenarioProcessor`（审核编排）
  - approve 步骤：状态校验 → 调用 `BudgetVoucherGenerator.generate()` → 凭证过账 → 状态 APPROVED
  - cancel 步骤（APPROVED→CANCELLED）：生成红冲 BUDGET 凭证（复用现有 `reverseProcess`）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetLineBizModel`（CrudBizModel）
  - 标准 CRUD + `defaultPrepareQuery`（按 scenarioId/periodId/subjectId 过滤）
  - `@BizQuery getBudgetVsActual`：预算对比查询（见 Phase 4）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetControlLogBizModel`（CrudBizModel，只读日志）
  - Skill: nop-backend-dev

- [ ] Add: `IErpFinBudgetControlBiz` SPI 接口（在 erp-fin-dao 或 erp-fin-api）
  ```java
  public interface IErpFinBudgetControlBiz {
      BudgetCheckResult check(
          @Name("subjectId") Long subjectId,
          @Name("costCenterId") Long costCenterId,
          @Name("periodId") Long periodId,
          @Name("amount") BigDecimal amount,
          @Name("sourceBillType") String sourceBillType,
          @Name("sourceBillCode") String sourceBillCode,
          IServiceContext context);
  }
  ```
  - `BudgetCheckResult`：actionResult(PASS/WARNED/BLOCKED) + availableAmount + budgetLineId
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetControlBiz` 实现（in erp-fin-service）
  - 查找命中的预算行（subjectId + costCenterId + periodId 匹配）
  - 计算 availableAmount = budgetBalance − actualBalance：**均从 `ErpFinVoucherLine` 聚合**（budgetBalance = 关联凭证 postingType=BUDGET 的行累计；actualBalance = 关联凭证 postingType=NORMAL 的行累计；按 subjectId + costCenterId + periodId 分组）。VoucherLine 含 costCenterId（finance.orm.xml:351）支撑成本中心粒度
  - 按 scenario.controlLevel 决定：NONE→PASS / WARN→写日志放行 / HARD→余额不足抛异常
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinErrors` 新增错误码
  - `ERR_BUDGET_EXCEEDED`（预算超支，HARD 级别拦截）
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 集成测试 `TestErpFinBudgetEndToEnd`
  - 场景 1（编制+审批+过账）：创建方案 + 预算行 → 审核 → BUDGET 凭证生成 → 从 `ErpFinVoucherLine`（关联凭证 postingType=BUDGET）聚合得预算余额
  - 场景 2（HARD 拦截）：方案 controlLevel=HARD + 预算行 1000 → 模拟 actualAmount（NORMAL 凭证）800 → check(300) → BLOCKED
  - 场景 3（WARN 放行）：同上 controlLevel=WARN → check(300) → WARNED + 写日志
  - 场景 4（NONE 不控制）：controlLevel=NONE → check(9999) → PASS
  - 场景 5（CANCELLED 红冲）：方案 APPROVED→CANCELLED → 红冲 BUDGET 凭证 → 预算余额（VoucherLine 聚合）归零
  - 场景 6（隔离）：BUDGET 凭证存在下运行损益结转 → 实际损益不含 BUDGET 行（依赖前置 Fix + 隔离回归测试）
  - Skill: nop-testing

Exit Criteria:

- [ ] BUDGET 凭证隔离回归测试全绿（BUDGET 凭证不污染实际损益/坏账）
- [ ] 预算方案审核 → BUDGET 影子凭证生成 → VoucherLine 聚合预算余额 全链路验证
- [ ] 预算控制 HARD/WARN/NONE 三级行为正确（从 VoucherLine 聚合 actual/budget）
- [ ] GraphQL Engine 集成测试全绿（≥6 场景）

### Phase 3 - 预算控制跨域集成

Status: planned
Targets: `module-purchase/erp-pur-service/`（采购审核集成）、`module-finance/erp-fin-service/`（付款审核集成）
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] Add: 配置项 `erp-fin.budget-check-enabled`（默认 false，向后兼容）
  - Skill: nop-backend-dev

- [ ] Add: `ErpPurOrderProcessor.validateBusinessRulesForApprove` 增加预算控制钩子
  - 在现有信用额度校验之后：
  ```
  if config("erp-fin.budget-check-enabled", false):
      for each orderLine mapped to subjectId + costCenterId:
          budgetControlBiz.check(subjectId, costCenterId, periodId, lineAmount, "PURCHASE_ORDER", order.code, context)
  ```
  - 注入 `@Inject @Nullable IErpFinBudgetControlBiz budgetControlBiz`
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinPaymentProcessor.validateBusinessRulesForApprove` 增加预算控制钩子
  - 付款审核时检查对应费用科目的预算余量
  - Skill: nop-backend-dev

- [ ] Add: 解除 `erp-fin.expense-budget-check-enabled` 占位门控
  - 费用报销审核时，`budget-check-enabled=true` 则调用 `IErpFinBudgetControlBiz.check`
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 集成测试 `TestErpFinBudgetControlIntegration`
  - 场景 1（采购订单 HARD 拦截）：设预算 + budget-check-enabled=true → 采购订单审核超预算 → 拦截
  - 场景 2（采购订单 WARN 放行）：同上 WARN → 放行 + 写 BudgetControlLog
  - 场景 3（config 关闭）：budget-check-enabled=false → 不检查（向后兼容）
  - Skill: nop-testing

Exit Criteria:

- [ ] 采购订单审核时预算控制正确拦截/放行
- [ ] BudgetControlLog 审计记录正确写入
- [ ] config 关闭时向后兼容（现有测试回归无失败）

### Phase 4 - 预算对比报表 + 前端页面

Status: planned
Targets: `module-finance/erp-fin-service/`（报表查询）、`module-finance/erp-fin-web/`
Skill: nop-backend-dev, nop-frontend-dev

- Item Types: `Add`
- Prereqs: Phase 3

- [ ] Add: `ErpFinBudgetLineBizModel.getBudgetVsActual` GraphQL 查询
  - 按 `(acctSchemaId, subjectId, periodId, costCenterId, projectId)` 分组 `ErpFinVoucherLine`，关联 `ErpFinVoucher.postingType` 得到三列：
    - Budget（postingType=BUDGET 凭证行累计）
    - Actual（postingType=NORMAL 凭证行累计）
    - Available（Budget − Actual）
  - 返回结构化报表数据
  - Skill: nop-backend-dev

- [ ] Add: 种子报表模板（nop-report）—— 预算 vs 实际对比表
  - Skill: nop-backend-dev

- [ ] Add: 前端页面替换占位
  - `budget-scenario/main.page.yaml` — 预算方案列表（grid + 审批操作）
  - `budget-scenario/edit.page.yaml` — 方案编辑（头字段 + 预算行内嵌 grid）
  - `budget-control-log/main.page.yaml` — 控制日志查询
  - Skill: nop-frontend-dev

Exit Criteria:

- [ ] 预算对比查询返回正确数据（Budget/Actual/Available 三列）
- [ ] 3 个前端页面替换占位，AMIS 加载无报错

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0b659948b8ffe3QVGxsN3HuEVwq) — 6 blocking：B1 baseline 错（`ErpMdSubject.isBudgetable` 已存在 orm.xml:830）；B2 架构前提错误（过账引擎不维护 GlBalance，VoucherLine 为权威源）；B3 关键缺陷（损益结转/坏账无 postingType 过滤，BUDGET 凭证会污染实际财务）；B4 ORM ask-first 未标注；B5 postingType 字典设计漂移；B6 GlBalance 无 costCenterId。
- Independent draft review iteration 2: accept (ses_0b644de95ffevvRgG0D3IwKk5r) — B1-B6 全部 resolved（isBudgetable 已存在 :830；过账引擎不维护 GlBalance 注释已核实；损益结转 :167-173 / 坏账 :176-182 无 postingType 过滤缺陷已核实且 Fix+回归测试已落地——AnnualCloseService 为间接 BUDGET→GlBalance 泄漏关键 choke point 已纳入 Fix 检查范围；ask-first 已标注；字典仅扩展凭证层；VoucherLine costCenterId :351 支撑成本中心粒度）；移除 GlBalance 结构变更后单一 VoucherLine 聚合结果表面一致，无矛盾引用，Deferred GlBalance 迁移项已移除；无新阻塞项。**草案审查收敛，状态 draft→active。**

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs/design/finance/budget.md` 标注已实现；use-case 审计 UC-FIN-11/13 标注已实现；`0700-2` Follow-up 解除；`core-business-roadmap.md` 新增预算管理工作项）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-finance/erp-fin-service` + `mvn test -pl module-purchase/erp-pur-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 承付款（Commitment）完整生命周期

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅 BUDGET + ACTUAL 对比。承付款（采购订单 APPROVED → COMMITMENT 凭证 → CANCELLED/invoiced 红冲）为更精确的预算管控（encumbrance accounting），设计已登记规则 3
- Successor Required: yes（触发条件：承付款会计需求落地时）

### 滚动预算自动复制 + 调整版本链管理

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期手动创建方案；滚动预算从年度预算自动按月拆分 + 调整预算 parentScenarioId 版本链差异对比归 successor
- Successor Required: yes

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 承付款机制（COMMITMENT postingType）successor
