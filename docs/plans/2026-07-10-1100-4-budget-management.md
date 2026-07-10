# 2026-07-10-1100-4-budget-management 预算管理（编制/控制/对比）

> Plan Status: draft
> Last Reviewed: 2026-07-10
> Source: `docs/design/finance/budget.md`（107 行完整设计）+ use-case 审计 UC-FIN-11/13 🔶
> Related: `2026-07-02-0700-2` Follow-up（预算控制不存在）；`core-business-roadmap.md` 无对应工作项
> Audit: required

## Current Baseline

### 已实现

- **`ErpFinVoucher.postingType` 字段已存在**（VARCHAR 20，字典 `erp-fin/posting-type`：NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL）。`module-finance/model/app-erp-finance.orm.xml:270`
- **凭证引擎/试算平衡/GlBalance 聚合** 全链路成熟（`ErpFinVoucher` → `ErpFinVoucherLine` → `ErpFinGlBalance`）
- **`ErpMdCostCenter` 实体已落地**（含 `isBudgetable` 列，`master-data.orm.xml`），预算行可用成本中心维度
- **项目域预算控制**（`ErpPrjBudget`/`ErpPrjBudgetLine` + WARNING/STRICT）已 done（extended-roadmap 2.6）——但这是项目级预算，**非** finance 域预算管理
- **期间控制** `ErpFinAccountingPeriodStatus.glStatus` 已实现，预算凭证可复用
- **费用报销**已预留预算门控钩子点（`erp-fin.expense-budget-check-enabled`，默认 false，`2026-07-02-0700-2` Follow-up）

### 剩余差距

- **`ErpFinBudgetScenario`/`ErpFinBudgetLine`/`ErpFinBudgetControlLog` 三个设计实体均未落地**（ORM 中无定义）
- **`ErpFinGlBalance` 无 `postingType` 维度**（24 列，无过账类型区分）——无法分离预算余额与实际余额
- **`erp-fin/posting-type` 字典无 BUDGET/COMMITMENT 值**（现有值面向凭证用途分类，非预算分类维度）
- **`IErpFinBudgetControlBiz` SPI 未定义**（全仓 grep 零命中，`0700-2:22` 确认）
- **`ErpMdSubject.isBudgetable` 列未落地**（`ErpMdCostCenter.isBudgetable` 已有但 Subject 上没有）
- **预算编制/控制/对比**全链路零实现零测试（use-case 审计 UC-FIN-11/13 🔶）

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

- 实现预算编制（方案 + 明细行），支持审批后生成 BUDGET 影子凭证
- 实现预算控制（HARD/WARN/NONE 三级），在采购订单/付款审核时同步校验预算余量
- 实现预算对比报表（预算 vs 实际 vs 承付款 vs 余量），复用凭证行按 postingType 分组
- 前端 CRUD 页面替换占位页

## Non-Goals

- **承付款（Commitment）完整生命周期**——采购订单 APPROVED 生成 COMMITMENT 凭证 + CANCELLED/invoiced 红冲 COMMITMENT。本期仅实现 BUDGET 影子凭证 + ACTUAL 实际数对比；承付款机制归 successor（设计已登记规则 3）
- **多级 `.xwf` 预算审批工作流链**——本期审批经 DIRECT 模式 + 状态机
- **滚动预算自动复制**——本期仅手动创建方案；滚动预算自动从年度预算派生归 successor
- **预算调整审批版本链**（`parentScenarioId` 版本链 + 差异对比）——本期支持字段但版本链管理归 successor
- **跨账套预算聚合**——本期单账套独立预算

## Task Route

- Type: `app-layer design change`（新增 ORM 实体 + 修改 GlBalance 结构 + 新增跨域 SPI）
- Owner Docs: `docs/design/finance/budget.md`（权威设计）、`docs/design/finance/posting.md`（IErpFinFactsValidator 扩展点）
- Skill Selection Basis: 新增 ORM 实体 + 修改 GlBalance + BizModel + 跨域 SPI → nop-backend-dev；GraphQL Engine 测试 → nop-testing

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - ORM 模型变更：预算实体 + GlBalance postingType 维度

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`、`module-master-data/model/app-erp-master-data.orm.xml`
Skill: nop-backend-dev

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] Decision: `ErpFinVoucher.postingType` 字典扩展策略
  - 现状：`erp-fin/posting-type` 字典有 5 值（NORMAL/OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL），面向**凭证用途分类**
  - 设计文档要求：新增 BUDGET/COMMITMENT 值用于预算分类
  - **选择扩展现有字典**：新增 `BUDGET`/`COMMITMENT` 两个值到 `erp-fin/posting-type`。NORMAL 等同于 ACTUAL（实际凭证的默认值）
  - 替代方案：新增独立列 `postingCategory`(ACTUAL/BUDGET/COMMITMENT/RESERVATION)——rejected，增加正交维度复杂度且现有凭证均为 ACTUAL=可由 NORMAL 推断；iDempiere 范式也是单列多值
  - 残留风险：OPENING_BALANCE/ADJUSTMENT/CLOSING/REVERSAL 值理论上既可是 ACTUAL 也可是 BUDGET（如预算调整）——本期接受 NORMAL=BUDGET 用于预算凭证，其余值不混用
  - Skill: nop-backend-dev

- [ ] Decision: `ErpFinGlBalance` 新增 `postingType` 列
  - 现状：GlBalance 24 列无 postingType——所有余额混在一起
  - 设计文档要求：预算余量 = 预算凭证累计 − 实际凭证累计，需按 postingType 分组
  - **选择新增列**：`postingType VARCHAR(20)`（默认 NORMAL，字典同上）+ 修改唯一键包含 postingType
  - 影响范围：GlBalance 汇总逻辑需按 postingType 分组聚合；期末结账 GlBalance 重建需感知 postingType
  - 保护区域：此为 finance 域核心实体结构变更，需 owner-doc 确认
  - Skill: nop-backend-dev

- [ ] Add: `erp-fin/posting-type` 字典新增 `BUDGET`/`COMMITMENT` 值
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinGlBalance` 新增 `postingType` 列（VARCHAR 20, 默认 NORMAL, 字典 `erp-fin/posting-type`）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetScenario`（预算方案头）
  - 字段（对照 `budget.md:17-33`）：id/code/name/orgId, acctSchemaId, fiscalYear, scenarioType(字典 `erp-fin/budget-scenario-type`: ANNUAL/ROLLING/ADJUSTMENT), parentScenarioId(nullable), validFrom/validTo, currencyId/exchangeRate/amountSource/amountFunctional, controlLevel(字典 `erp-fin/budget-control-level`: NONE/WARN/HARD), docStatus(字典 `erp-fin/budget-status`: DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED), approveStatus(共用 `erp-fin/approve-status`), 标准审计字段
  - 关系：to-one acctSchema/currency/org/parentScenario；to-many lines(→ErpFinBudgetLine)
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetLine`（预算明细行）
  - 字段（对照 `budget.md:37-53`）：id/scenarioId/lineNo/orgId, acctSchemaId, periodId(→ErpFinAccountingPeriod), subjectId/subjectCode, costCenterId(nullable), departmentId/projectId/partnerId/warehouseId/materialId(均 nullable 辅助维度), budgetAmountSource/budgetAmountFunctional, currencyId/exchangeRate, 标准审计字段
  - 关系：to-one scenario/period/subject/costCenter + 辅助维度 to-one
  - 注意：commitmentAmount/actualAmount/availableAmount **不落库**（派生，查询时从凭证行聚合计算）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBudgetControlLog`（预算控制日志）
  - 字段（对照 `budget.md:55-68`）：id/orgId/businessDate, scenarioId/budgetLineId, sourceBillType/sourceBillCode, subjectId/costCenterId/projectId/periodId, requestedAmount, committedAmount, actionResult(字典 `erp-fin/budget-action`: PASS/WARNED/BLOCKED), operatorId/operatedAt/reason, 标准审计字段
  - Skill: nop-backend-dev

- [ ] Add: `ErpMdSubject` 新增 `isBudgetable BOOLEAN`（默认 false）
  - 在 `module-master-data/model/app-erp-master-data.orm.xml` 中 ErpMdSubject 实体新增列
  - Skill: nop-backend-dev

- [ ] Add: 新增字典 `erp-fin/budget-scenario-type`、`erp-fin/budget-control-level`、`erp-fin/budget-status`、`erp-fin/budget-action`
  - Skill: nop-backend-dev

- [ ] Add: 执行 `mvn clean install -DskipTests`（module-finance + module-master-data 链）触发增量代码生成
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] ORM 变更后 `mvn clean install -DskipTests`（module-finance + module-master-data 链）BUILD SUCCESS
- [ ] `ErpFinBudgetScenario`/`ErpFinBudgetLine`/`ErpFinBudgetControlLog` Entity/DAO 生成
- [ ] `ErpFinGlBalance` 含 `postingType` getter
- [ ] `ErpMdSubject` 含 `isBudgetable` getter

### Phase 2 - 预算编制 BizModel + 审批过账引擎

Status: planned
Targets: `module-finance/erp-fin-service/`
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: `ErpFinBudgetScenarioBizModel`（CrudBizModel）
  - 标准 CRUD + 三轴状态机（DRAFT→SUBMITTED→APPROVED / →REJECTED→DRAFT / APPROVED→CANCELLED）
  - `@BizMutation approve`：审核时生成 BUDGET 影子凭证
  - Skill: nop-backend-dev

- [ ] Add: `BudgetVoucherGenerator`（预算凭证生成器，`erp-fin-service/.../support/`）
  - 审核通过时：遍历 `ErpFinBudgetLine`，按 `subject.direction` 自动取借贷方向（资产/费用→借方，负债/收入→贷方）
  - 创建 `ErpFinVoucher`(postingType=BUDGET) + `ErpFinVoucherLine`（每预算行→一凭证行）
  - 凭证走正常 `DRAFT → POSTED` 流程，写入 `ErpFinGlBalance`(postingType=BUDGET)
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
  - 查找命中的预算行（subjectId + costCenterId + periodId 匹配的 BUDGET postingType 余额）
  - 计算 availableAmount = budgetBalance − actualBalance（均从 GlBalance 按 postingType 聚合）
  - 按 scenario.controlLevel 决定：NONE→PASS / WARN→写日志放行 / HARD→余额不足抛异常
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinErrors` 新增错误码
  - `ERR_BUDGET_EXCEEDED`（预算超支，HARD 级别拦截）
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 集成测试 `TestErpFinBudgetEndToEnd`
  - 场景 1（编制+审批+过账）：创建方案 + 预算行 → 审核 → BUDGET 凭证生成 → GlBalance(postingType=BUDGET) 有余额
  - 场景 2（HARD 拦截）：方案 controlLevel=HARD + 预算行 1000 → 模拟 actualAmount 800 → check(300) → BLOCKED
  - 场景 3（WARN 放行）：同上 controlLevel=WARN → check(300) → WARNED + 写日志
  - 场景 4（NONE 不控制）：controlLevel=NONE → check(9999) → PASS
  - 场景 5（CANCELLED 红冲）：方案 APPROVED→CANCELLED → 红冲 BUDGET 凭证 → GlBalance 归零
  - Skill: nop-testing

Exit Criteria:

- [ ] 预算方案审核 → BUDGET 影子凭证生成 → GlBalance 更新 全链路验证
- [ ] 预算控制 HARD/WARN/NONE 三级行为正确
- [ ] GraphQL Engine 集成测试全绿（≥5 场景）

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

- Independent draft review iteration 1: pending

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

### GlBalance postingType 唯一键变更的存量数据迁移

- Classification: `watch-only residual`
- Why Not Blocking Closure: 新增 postingType 列默认 NORMAL，现有 GlBalance 行自动获得 NORMAL 值，无需迁移脚本。唯一键变更需确认是否影响现有结账逻辑
- Successor Required: no

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 承付款机制（COMMITMENT postingType）successor
