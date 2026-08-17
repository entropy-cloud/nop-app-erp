# 项目管理域 - 成本归集

## 目的

详细说明项目域的成本归集机制，包括工时成本计算、项目辅助核算、预算控制与成本报表。

> **费用报销/员工借款 owner doc**：本文多处引用"费用报销"作为项目成本归集来源（§1.3/§3.3/§四），其业务语义与 owner 实体现在 [`../finance/expense-claim.md`](../finance/expense-claim.md)（ErpFinExpenseClaim/Line + ErpFinEmployeeAdvance）。报销行原生携带 `projectId`/`costCenterId` 归集维度。

---

## 一、项目辅助核算模型

### 1.1 辅助核算维度

项目作为财务凭证的辅助核算维度，通过 `projectId` 字段标注：

```
凭证分录行扩展字段：
        │
        ├─► projectId（项目编码）
        ├─► taskId（任务编码，可选）
        └─► activityType（活动类型，可选）
```

### 1.2 科目与项目的关系

| 科目类型 | 项目标注场景 | 说明 |
|----------|--------------|------|
| 成本科目 | 项目费用、采购、领料 | 归集项目成本 |
| 收入科目 | 项目销售收入 | 归集项目收入 |
| 费用科目 | 项目相关费用 | 归集期间费用 |

### 1.3 项目成本分类

| 成本类型 | 来源 | 说明 |
|----------|------|------|
| 直接人工 | 工时记录 | 成员投入项目的人工成本 |
| 直接材料 | 采购/领料 | 项目专用物料成本 |
| 直接费用 | 费用报销 | 项目直接发生的费用 |
| 间接费用 | 分摊 | 按比例分摊的公共费用 |

---

## 二、工时成本计算

### 2.1 工时记录内容

工时记录包含：项目编码、任务编码、活动类型（开发/测试/实施等）、成员编码、工作日期、工时（小时）、标准成本率（元/小时）、人工成本（= 工时 × 成本率）。

### 2.2 成本率配置

| 配置级别 | 优先级 | 说明 |
|----------|--------|------|
| 用户级别 | 最高 | 特定用户的成本率 |
| 角色级别 | 中 | 某角色（如高级工程师）的成本率 |
| 活动类型级别 | 低 | 某活动类型的默认成本率 |

> **实现约定（RC-R1.60 / P1-RC-048 更新）**：解析链 = **单填 &gt; 用户级 &gt; 角色级 &gt; 活动类型 &gt; 全局默认**——
> `Timesheet.costRate`（按单填写，显式录入优先）→ `ErpPrjProjectUser.costRate`（用户级费率，按 projectId+userId 查成员行）→ `ErpPrjRole.costRate`（角色级费率，`ErpPrjProjectUser.role` 纯文本按 `ErpPrjRole.code` 精确匹配）→ `ErpPrjActivityType.costRate`（活动类型默认）→ `erp-prj.default-labor-cost-rate`（全局 config）。
> 五处皆无抛 `ERR_COST_RATE_NOT_AVAILABLE`。L1（`use-cases.md:38`）「用户费率 &gt; 角色费率 &gt; 活动类型费率」三级优先级运行时成立；单填保持最高为 RC-R1.60 Phase 1 D2 裁决（显式录入优先，既有行为兼容——L1 字面未定义单填与三级的关系，A1.34 §9 businessType drift 冻结条款不适用费率）；用户级/角色级费率为 null 或成员行/角色缺失时跳过对应 tier。
> 载体落地：`ErpPrjProjectUser.costRate`（propId 13，可空无默认）+ 新增 `ErpPrjRole` 实体（code/name/costRate + UK(code)，纯加性，2026-08-12 A 类批量授权 + 双独立子 agent 批准，plan `docs/plans/2026-08-16-2043-3`）；角色级费率为全局角色码（非 per-project 角色）语义。角色管理 UI 为 Non-Goal（标准 CRUD 生成）。

### 2.3 工时提交流程

```
工时提交
        │
        ├─► 校验：项目状态为 OPEN（进行中）
        │
        ├─► 校验：任务存在且状态允许（待开始/进行中）
        │
        ├─► 获取成员成本率（用户→角色→活动类型优先级）
        │
        ├─► 计算人工成本 = 工时 × 成本率
        │
        ├─► 保存工时记录
        │
        ├─► 发布工时成本过账事件（post-commit）
        │
        └─► 返回成功
                │
                ▼ (异步)
        财务域生成项目成本凭证
                │
                ├─► 借：项目成本（按项目类型配置的科目）
                │
                └─► 贷：应付职工薪酬（或直接人工）
```

### 2.4 工时成本凭证示例

```
凭证分录：
        │
        ├─► 借：510101 项目开发成本（项目A）   8000 元
        │
        └─► 贷：2211 应付职工薪酬              8000 元

说明：
        │
        ├─► 工时：10 小时 × 成本率 800 元/小时 = 8000 元
        └─► 辅助核算：projectId = "PRJ2024001", activityType = "开发"
```

> **多币种工时过账实现注记（RC-R1.64 / P1-MA1-010 业务逻辑层闭合，2026-08-17，L1 UC-PRJ-02/06「多币种折算到统一币种」）**：
> `TimesheetPostingDispatcher.buildEvent` 汇率解析替代 `setExchangeRate(ONE)` 硬编码——三态：
> currencyId=null / 币种不存在（保守放行，镜像 R1.42 守卫 D2）/ 本位币（`ErpMdCurrency.isFunctional=TRUE`）→ rate=1 回退（单币种行为保持）；
> 非本位币 → `ErpMdExchangeRate` 按 from=currencyId + to=本位币 + `validFrom<=voucherDate<=validTo` 边界匹配（validFrom 降序最近生效优先，limit 1，rateType 信息性维度不过滤）解析，
> 本位币缺失或汇率行未命中 → 抛 `ErpFinErrors.ERR_EXCHANGE_RATE_REQUIRED`（复用 finance R1.42 语义，跨域单码处理）。
> 折算落地为 **buildEvent + Provider 双点协同**：`ProjectCostCollectionProvider.fact()` 增 `setAmount(functional)` + `setAmountSource(source)` + `setAmountFunctional(source×rate)`
> （镜像 `PurAcctDocProvider` 范式，amount 保持本位币/功能金额语义）——上例 USD 项目场景：amountSource=8000 USD、amountFunctional=8000×rate，GL 借贷按本位币记账，试算平衡不破坏。
> 汇率缺失错误路径（D1(ii) 选项 α）：buildEvent 在 `tryPost` try 块外抛错传播 → approve 事务回滚 → 单据保持 **SUBMITTED**（与既有 buildEvent 校验错误先例 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED` 同型，无告警派发；操作员补录 `ErpMdExchangeRate` 后重提显式可恢复）。
> 边界查询经 `dateBetween(epoch/2999 哨兵)` 表达（XMeta 过滤算子白名单无 ge/le，对齐 contract/hr 域先例）；跨域只读经 `IBizObjectManager` 按名解析（对齐 `ErpFinPostingProcessor.findCurrencyById` 范式，零新增 daoFor 站点）。
> 归集头 exchangeRate metadata 面（`ProjectCostAggregator`/`MaterialCostAggregator`/`ExpenseCostAggregator`）与 PnL 快照折算归 P2-RC-050 successor（消费侧=PnL 聚合/辅助核算，非 voucher 路径）。

---

## 三、项目预算控制

### 3.1 预算结构

项目预算包含：项目编码、总预算、已使用预算、剩余预算（= 总预算 - 已使用预算）、控制模式（WARNING/STRICT）。

> **实现约定**：本期以**项目级** `erp-prj.budget-control-mode`
> config（WARNING 默认 / STRICT）实现预算控制，而非预算头 `ErpPrjBudget.controlMode` 字段。
> 总预算取 `ErpPrjProject.budget`，已使用 = 该项目所有归集行金额之和。行级 `committedAmount/actualAmount`
> 仍记录备查但不参与拦截。待多预算行项目粒度需求落地时改为行级 STRICT（successor）。

### 3.2 预算控制模式

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| WARNING | 超预算时警告，允许继续 | 研发项目、创新性工作 |
| STRICT | 超预算时拦截，拒绝提交 | 固定报价项目、成本敏感项目 |

### 3.3 预算检查时机

| 检查时机 | 检查对象 | 说明 |
|----------|----------|------|
| 工时提交 | 人工成本 | 检查累计人工成本是否超预算 |
| 采购下单 | 采购金额 | 检查采购金额是否超预算 |
| 费用报销 | 报销金额 | 检查费用是否超预算 |

> **实现注记（RC-R1.62 / P1-RC-051 报销路径闭合，2026-08-16）**：L1 UC-PRJ-04「报销审核(标注项目时)」时机已运行时成立——`ExpenseCostAggregator.refreshExpenseCost` 在归集行写入前调 `BudgetChecker.check(projectId, 本次新增归集金额 Σ)`（STRICT 超预算抛 `ERR_BUDGET_EXCEEDED` 拒绝 → @BizMutation 事务回滚零落库 / WARNING 放行）。三时机现状：工时提交（既有 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook`）+ 采购审核（RC-R1.61 物料归集 `ErpPrjCostCollectionAggregateMaterialCostProcessor` merge）+ 报销审核（本行）——L1 三时机全数闭合。`TestErpPrjExpenseAggregation` 新增 STRICT 拒绝/WARNING 放行断言。
>
> **设计后果（残留风险，RC-R1.62 Phase 1 Decision 3）**：`closeProject` 于项目仍 OPEN 时触发费用归集刷新（`ErpPrjProjectCloseProjectProcessor`）——若 STRICT 模式且存在超预算待归集报销行，预算检查将抛 `ERR_BUDGET_EXCEEDED` 使 closeProject 事务回滚（WARNING 默认放行）。该后果在 UC-PRJ-04「报销审核预算检查」语义下可接受（关闭前预算拦截 = 防御性校验）。

### 3.4 预算检查流程

```
业务单据提交
        │
        ├─► 提取项目编码
        │
        ├─► 查询项目预算
        │
        ├─► 计算拟新增成本
        │
        ├─► 判断：(已使用预算 + 拟新增成本) > 总预算？
        │           │
        │           ├─► 否 → 允许提交
        │           │
        │           └─► 是 → 根据控制模式处理
        │                       │
        │                       ├─► WARNING → 提示警告，允许提交
        │                       │
        │                       └─► STRICT → 拒绝提交，提示超预算
```

---

## 四、项目成本归集流程

### 4.1 归集来源

| 来源 | 触发方式 | 凭证生成 |
|------|----------|----------|
| 工时记录 | 工时提交事件 | 借：项目成本 / 贷：应付职工薪酬 |
| 采购订单 | 采购入库审核 | 借：项目成本 / 贷：应付账款 |
| 费用报销（[owner](../finance/expense-claim.md)） | 报销审核 | 借：项目费用 / 贷：现金/银行 |
| 库存领料 | 领料单审核 | 借：项目成本 / 贷：存货 |

> **实现约定**：本期费用报销归集为 **projects 驱动只读聚合**
> （`IErpPrjCostCollectionBiz.refreshExpenseCost` 经 `IErpFinExpenseClaimBiz` 只读 R 查报销单 + projects 自写
> `erp_prj_cost_collection`），而非 finance 回写——对齐 `data-dependency-matrix.md §3.2:160`「finance 从不写业务表」
> + `§4.2:217` 成本归集为 projects 触发 `confirmCollection()`。
>
> **实现注记（RC-R1.61，P1-RC-049 落地）**：采购入库物料归集已实现——purchase 侧入库审核（`ErpPurReceiveApproveProcessor.approve`）
> 在入库移动单生成后经既有 purchase→projects 边调 `IErpPrjCostCollectionBiz.aggregateMaterialCost` 跨域 Facade
> （行级 projectId 解析：`ErpPurReceiveLine.orderLineId → ErpPurOrderLine.projectId`，null 跳过），projects 侧
> `ErpPrjCostCollectionAggregateMaterialCostProcessor` 守卫链（config 门控 `erp-prj.material-aggregation-enabled`
> 默认 true → `requireReferenceable` 单一咽喉 → 预算检查 STRICT 拒绝/WARNING 放行 → 幂等去重）后经
> `MaterialCostAggregator` 自写归集行（costCategory=MATERIAL / sourceBillType=PURCHASE_RECEIVE /
> sourceBillCode=入库单号-行号 / amount=入库行金额不含税）+ 头 totalAmount 累加 + actualCost 增量回写。
> 接线方向裁决：候选 A（InvPostingDispatcher→projects Facade，须新增 inventory→projects 依赖 + 矩阵修订）与
> 候选 B（projects 聚合器只读 inventory，反向边禁止）均因模块依赖约束否决，选候选 C（purchase 侧触发经既有边，
> 零新增依赖零矩阵修订）；STRICT 预算拒绝异常传播 → 入库审核回滚（L1「采购审核拒绝该笔归集」）。
> **领料归集（MATERIAL）载体缺失登记**：本仓无「项目领料单」实体（领料为制造专用 MFG_ISSUE 写 WIP，mfg/inventory
> orm.xml 零 projectId 列）→ scope 解释登记（watch-only residual，successor 触发条件=项目领料单实体落地）。
> **分包归集（SUBCONTRACT）载体缺失登记**：manufacturing 域委外链完备（`ErpMfgSubcontractOrder` +
> SUBCONTRACT_ISSUE/RECEIPT/FEE 三腿 posting）但 mfg orm.xml 零 projectId 列（项目维度不可达），purchase 域无
> 分包单据类型 → scope 解释登记（watch-only residual，successor 触发条件=分包单/委外链加项目维度列落地）。

### 4.2 归集流程全景

```
多来源成本归集
        │
        ├─► 工时提交 → 工时成本凭证 → 项目成本归集（LABOR）
        │
        ├─► 采购入库（标注项目）→ 入库移动单生成后 purchase 侧触发 Facade → 项目成本归集（MATERIAL）
        │         （RC-R1.61：ErpPurReceiveApproveProcessor → IErpPrjCostCollectionBiz.aggregateMaterialCost）
        │
        ├─► 费用报销（标注项目）→ 费用凭证 → 项目费用归集（EXPENSE）
        │
        ├─► 领料出库（标注项目）→ 领料凭证 → 项目成本归集（领料载体缺失，scope 解释登记，successor）
        │
        ├─► 分包（委外加工单）→ 分包凭证 → 项目成本归集（项目维度不可达，scope 解释登记，successor）
        │
        └─► 销售发票（标注项目）→ 收入凭证 → 项目收入归集
                    │
                    ▼
            项目成本/收入汇总表
                    │
                    └─► 项目利润 = 收入 - 成本（四分类：人工/物料/费用/分包）
```

> **CostCollection.docStatus dict-value drift（P1-MA2-069 Deferred）**：`ErpPrjCostCollection.docStatus` 绑定 `erp-prj/project-status` 字典（DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED），但 `ProjectCostAggregator`/`ExpenseCostAggregator` 经聚合器单一入口写入 `DOC_STATUS_APPROVED="APPROVED"`——该值**不在 project-status 字典内**，是已知 dict-value drift。归集行为正确（聚合器单一入口写入 + 幂等去重），仅按 dict 筛选层（如下拉过滤）失效。successor 独立 `erp-prj/cost-collection-status` 字典 + ORM `ext:dict` 改绑时收敛。详见 `state-machine.md §适用对象三 CRUD 桩实体状态机（Deferred）`。

### 4.3 项目关闭时的成本结转

> **关闭前置（P1-MA2-067）**：`closeProject` 在成本结转前经 `validateTasksFinished` 校验项目下无未结束任务（task-status 为 TODO/IN_PROGRESS/BLOCKED），STRICT 模式（`erp-prj.strict-project-task-completion-check` 默认 true）抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS`，WARN 模式 LOG.warn 放行。详见 `state-machine.md §迁移完整性 OPEN→COMPLETED`。

```
项目关闭（COMPLETED/CANCELLED）
        │
        ├─► 检查是否有未归集成本
        │
        ├─► 执行成本结转（如有）
        │
        ├─► 生成项目成本/利润报表
        │
        └─► 冻结项目（不可再被引用）
```

### 4.4 项目成本资本化与费用化

项目成本根据项目类型和用途，决定资本化（转入资产）还是费用化（计入当期损益）：

| 项目类型 | 成本处理 | 凭证方向 |
|----------|----------|----------|
| 自建资产项目（CIP） | 资本化 → 完工后转入固定资产 | 借：在建工程 / 贷：项目成本 |
| 研发项目 | 费用化 → 计入研发费用 | 借：研发费用 / 贷：项目成本 |
| 客户项目 | 费用化 → 计入主营业务成本 | 借：主营业务成本 / 贷：项目成本 |
| 内部改善项目 | 部分资本化（符合资本化条件的部分） | 按评估结果拆分 |

> 项目关闭时，资本化项目触发 `IErpFinAcctDocProvider CAPITALIZATION` 业务类型生成资产入账凭证。

---

## 五、项目成本报表

### 5.1 报表类型

| 报表 | 内容 | 用途 |
|------|------|------|
| 项目成本明细表 | 按成本类型、期间汇总 | 成本分析 |
| 项目利润表 | 收入 - 成本 = 利润 | 效益评估 |
| 预算执行表 | 预算 vs 实际 | 预算控制 |
| 工时统计表 | 按成员/任务/活动汇总 | 资源利用率分析 |

### 5.2 报表维度

| 维度 | 说明 |
|------|------|
| 项目 | 按项目汇总 |
| 任务 | 按任务汇总 |
| 成员 | 按成员汇总 |
| 期间 | 按会计期间汇总 |
| 活动类型 | 按活动类型汇总 |

---

## 六、跨域协作

### 6.1 与财务域协作

```
项目域 → 财务域：
        │
        ├─► 工时成本数据（Timesheet）
        ├─► 项目编码用于凭证辅助核算
        └─► 预算数据用于预算控制

财务域 → 项目域：
        │
        └─► 凭证过账状态反馈
```

> **工时过账失败告警（G3 错误传播分级）**：`TimesheetPostingDispatcher.tryPost` 过账失败（吞异常保持 APPROVED+posted=false 不阻塞终态）现派发 `IErpSysNotificationBiz` 告警（`prj.timesheet-posting-failure`），使 GL 缺 PROJECT_COST_COLLECTION 凭证悬挂可被运营感知（业财不一致经期末试算平衡人工发现，projects 不纳入期末前置检查覆盖矩阵）。

> **多币种汇率语义（RC-R1.64，与 finance 域同源）**：工时过账汇率由 projects 侧 `buildEvent` 显式解析（`ErpMdExchangeRate` 数据载体）后传入 finance 引擎，使 R1.42 `guardExchangeRate` 守卫对工时路径真正生效（修复前恒传 ONE 导致守卫结构性失效）；本位币判定载体 = `ErpMdCurrency.isFunctional` 字段（schema 级 `functionalCurrencyId` 细分归 successor，语义对齐 `posting.md` R1.42 注记）。跨域只读经 `IBizObjectManager` 按名解析 `IErpMdCurrencyBiz`/`IErpMdExchangeRateBiz`（对齐 finance 引擎既有范式）。

### 6.2 与采购域协作

```
采购订单标注项目编码 → 采购入库 → 成本归集到项目
        │
        └─► 采购域调用项目域校验项目状态（OPEN 才能引用）
```

### 6.3 与销售域协作

```
销售订单标注项目编码 → 销售发票 → 收入归集到项目
        │
        └─► 销售域调用项目域校验项目状态
```

---

## 七、关键业务规则

1. **项目状态控制**：只有 OPEN 状态的项目才能被新单据引用——**统一咽喉已消费（RC-R1.62 / P2-RC-048 闭合，2026-08-16）**：`IErpPrjProjectBiz.requireReferenceable` 现为项目归集守卫的单一咽喉——费用路径（`ExpenseCostAggregator.refreshExpenseCost`，本行接入）+ 采购路径（`ErpPrjCostCollectionAggregateMaterialCostProcessor`，RC-R1.61）+ 工时路径（`ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable` 内联自有校验保留为专项错误码语义）；P2-RC-048「API 存在生产代码零调用方」watch-only finding 随费用路径接入闭合。
2. **预算控制配置化**：支持 WARNING 和 STRICT 两种模式
3. **成本率优先级**：用户级别 > 角色级别 > 活动类型级别（**已实现，RC-R1.60 / P1-RC-048，2026-08-17**——`CostRateResolver.resolve` 五级解析链「单填 > 用户级 > 角色级 > 活动类型 > 全局默认」，用户级/角色级载体 `ErpPrjProjectUser.costRate` + `ErpPrjRole` 实体落地，详见 §2.2 实现约定）
4. **项目关闭冻结**：关闭后的项目不可再归集新成本
5. **辅助核算追溯**：所有项目相关凭证可按 projectId 汇总查询

---

## 八、凭证注册与预算检查

项目域实现 `IErpFinAcctDocProvider` 接口注册业务类型 `PROJECT_COST_COLLECTION`，在工时提交时触发工时成本凭证生成。

预算检查在业务单据提交时执行，根据项目预算的控制模式（WARNING/STRICT）决定是否允许提交。

> **实现约定**：设计原文写作 `PROJECT_LABOR_COST` 为命名偏差，
> 实际复用既有 `ErpFinBusinessType.PROJECT_COST_COLLECTION(110)` 枚举（保持「不新增 finance 契约」边界）。
> 工时/费用/采购归集共用同一 businessType，Provider 内按 `sourceBillType` 区分借方科目。