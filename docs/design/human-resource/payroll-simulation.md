# 薪酬模拟（Payroll Simulation）

## 目的

详细设计薪酬"假设"模拟功能：在正式核算前，允许 HR 复制上一期薪酬数据创建模拟版本，修改后对比差异，预览实发变化，经审批后转为正式薪酬核算。降低薪酬调整风险，避免错误发放。

## 设计边界

- 本设计负责：薪酬模拟版本的生命周期管理、复制上期数据、模拟 vs 当前期 vs 上期对比视图、模拟结果审批转正式。
- **与 payroll 的边界**：模拟版本基于 `ErpHrSalary` 扩展，共享薪酬项目定义（`ErpHrSalaryItem`）、社保配置（`ErpHrSocialInsuranceConfig`）、个税配置（`ErpHrTaxConfig`）和公式引擎。模拟版本审批通过后转化为正式 `ErpHrSalary` 进入支付流程。
- 本设计不负责：薪酬项目配置、社保/公积金/个税计算逻辑本身（复用现有 payroll 计算引擎）；银行文件生成；工资单发布。

---

## 一、薪酬模拟实体（ErpHrSalarySimulation）

### 1.1 实体设计

`ErpHrSalarySimulation` 继承 `ErpHrSalary` 的所有字段，扩展模拟特有字段：

| 字段 | 含义 |
|------|------|
| id/code/orgId | 标准 |
| sourceSalaryId | 源薪酬记录（从哪期复制而来，→ErpHrSalary） |
| simulationPeriodYear/simulationPeriodMonth | 模拟期间（与正式 payroll 的 year/month 对齐） |
| simulationName | 模拟名称（如"2026-07 调薪试算 v2"） |
| status | 状态 dict `erp-hr/simulation-status`：DRAFT / IN_REVIEW / APPROVED / REJECTED / CONVERTED |
| reviewerId | 审批人（→ErpHrEmployee） |
| reviewedAt | 审批时间 |
| convertedAt | 转正式时间 |
| convertedSalaryId | 转正式后生成的 ErpHrSalary ID |
| notes | 模拟备注 |
| 标准审计字段 | |

> `ErpHrSalarySimulation` 不新增独立的薪酬行表，复用 `ErpHrSalary` 已有的薪酬项目行结构（通过 `sourceSalaryId` 追溯）。模拟修改记录在 `ErpHrSalarySimulationItemAdjustment` 中。

### 1.2 状态机

```
草稿 (DRAFT)
  ├─ 提交审核 → 审核中 (IN_REVIEW)
  └─ 取消 → 已驳回 (REJECTED) [终态]

审核中 (IN_REVIEW)
  ├─ 审批通过 → 已审批 (APPROVED)
  └─ 驳回 → 已驳回 (REJECTED) [终态]

已审批 (APPROVED)
  ├─ 转正式 → 已转正式 (CONVERTED) [终态]
  └─ 驳回 → 已驳回 (REJECTED) [终态]
```

### 1.3 状态迁移

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT→IN_REVIEW | HR 薪酬专员 | 至少一项模拟调整已记录 | 生成审批待办 |
| IN_REVIEW→APPROVED | 审批人（HR 经理/财务） | — | 模拟锁定，可转正式 |
| IN_REVIEW→REJECTED | 审批人 | — | 模拟退回，HR 可修改后重新提交 |
| APPROVED→CONVERTED | HR 薪酬专员 | 目标期间无正式薪酬冲突 | 创建正式 ErpHrSalary，标记 paymentStatus=PENDING |

---

## 二、复制上期数据

### 2.1 复制流程

```
HR 选择"创建模拟"
    │
    ├─► 选择源薪酬期间（如 2026-06）
    │
    ├─► 系统加载该期间所有 ErpHrSalary 行
    │
    ├─► 创建 ErpHrSalarySimulation（status=DRAFT）
    │       ├─ 复制每位员工的薪酬项目行
    │       ├─ 复制累计个税数据（年初至源期间末）
    │       ├─ 复制考勤/加班/休假数据快照（源期间值）
    │       └─ 所有金额默认 = 源期间值
    │
    ├─► HR 在模拟中编辑：
    │       ├─ 调整基本工资（调薪场景）
    │       ├─ 调整津贴标准
    │       ├─ 调整绩效奖金
    │       ├─ 修改出勤天数（模拟补班/缺勤）
    │       └─ 添加/删除薪酬项目
    │
    └─► 每次编辑触发重新计算（即时应变）
```

### 2.2 ErpHrSalarySimulationItemAdjustment

每次手动修改的记录，用于追踪变化来源：

| 字段 | 含义 |
|------|------|
| id/simulationId/employeeId | 标准 |
| salaryItemCode | 薪酬项目（引用 ErpHrSalaryItem.code） |
| originalAmount | 源期间值 |
| adjustedAmount | 模拟调整后值 |
| adjustmentReason | 调整原因 dict：SALARY_CHANGE（调薪）/ ALLOWANCE_CHANGE（津贴调整）/ BONUS_CHANGE（绩效变更）/ MANUAL_ENTRY（手工录入） |
| adjustedBy | 调整人 |
| adjustedAt | 调整时间 |

> 🟢 SAP SuccessFactors "What-If Simulation" 的调整追踪机制。

---

## 三、对比视图

### 3.1 对比维度

| 视角 | 对比项 | 说明 |
|------|--------|------|
| 员工级 | 模拟值 vs 当前期 vs 上期 | 逐员工查看三列对比 |
| 部门汇总 | 应发合计/实发合计 | 部门维度汇总差异 |
| 项目汇总 | 各薪酬项目合计变化 | 哪项调整影响最大 |
| 公司级 | 总人工成本变化 | 高管/财务视角 |

### 3.2 对比布局

```
员工薪酬对比（张三）
┌──────────────┬──────────┬──────────┬──────────┬──────────┐
│ 薪酬项目      │ 上期(6月) │ 当前期(7月)│ 模拟值    │ 差额     │
├──────────────┼──────────┼──────────┼──────────┼──────────┤
│ 基本工资      │ 10,000   │ 10,000   │ 12,000   │ +2,000   │
│ 岗位津贴       │ 2,000    │ 2,000    │ 3,000    │ +1,000   │
│ 绩效奖金       │ 3,000    │ 3,500    │ 3,500    │ 0        │
│ 加班费         │ 800      │ 600      │ 600      │ 0        │
│ 应发合计       │ 15,800   │ 16,100   │ 19,100   │ +3,000   │
│ 社保(个人)     │ -1,200   │ -1,200   │ -1,400   │ -200     │
│ 公积金(个人)   │ -500     │ -500     │ -600     │ -100     │
│ 个税           │ -300     │ -350     │ -650     │ -300     │
│ 实发合计       │ 13,800   │ 14,050   │ 16,450   │ +2,400   │
└──────────────┴──────────┴──────────┴──────────┴──────────┘
```

> 🟢 Odoo Payroll "Comparison Report" 差异报表。

### 3.3 异常值告警

| 告警规则 | 阈值（可配置） | 说明 |
|----------|---------------|------|
| 实发变化超限 | `erp-hr.simulation.net-pay-change-threshold`（默认 ±20%） | 该员工实发变化超过阈值时高亮 |
| 总额偏差大 | `erp-hr.simulation.total-change-threshold`（默认 ±10%） | 部门/公司总人工成本变化超阈值 |
| 个税跳档 | 税率档位变化 | 模拟后员工税率跳档时告警（如 10%→20%） |
| 社保基数超限 | 基数超出上下限 | 调整后基数 > 上限或 < 下限时提醒 |

---

## 四、模拟转正式

### 4.1 转换流程

```
模拟 APPROVED → HR 点击"转正式"
    │
    ├─► 校验目标期间（simulationPeriodYear/Month）无已 PAID 的正式薪酬
    │
    ├─► 校验目标期间同员工无重复正式薪酬
    │
    ├─► 创建正式 ErpHrSalary 记录（paymentStatus = PENDING）
    │       ├─ 每位员工的薪酬项目行 = 模拟调整后的值
    │       ├─ 继承 simulationId 方便追溯
    │       └─ 标记 convertedFromSimulationId
    │
    ├─► 更新 ErpHrSalarySimulation.status = CONVERTED
    │
    └─► 正式薪酬进入审批流（payroll §5.2 流程）
```

### 4.2 冲突处理

| 场景 | 处理 |
|------|------|
| 目标期间已有 DRAFT 正式薪酬 | **本期简化为拒绝**（`ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE`，含 DRAFT/PENDING 等全部非 VOID）。design 原为「覆盖确认」，需前端二次确认交互，归前端计划后续实现 |
| 目标期间已有 PAID 正式薪酬 | 不允许转正式，提示先作废（`ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT`） |
| 部分员工已在目标期间有薪酬 | 仅转换无冲突的员工（部分冲突仅转无冲突） |

### 4.3 追溯方向（实现偏离补注）

- **单向追溯**：`Simulation.convertedSalaryId → ErpHrSalary`（已存在外键 + to-one 关系）。
- **不在 `ErpHrSalary` 加 `convertedFromSimulationId` 列**（核心零污染——避免污染 0831-2 既有薪酬实体模型契约）。
- **反向追溯**：经查询 `findSimulationsByConvertedSalary(salaryId)` 补全，而非外键导航。

### 4.4 PayrollCalculator 覆盖重算（实现偏离补注）

- `SocialInsuranceCalculator` 深度耦合 master 读取（仅接受 employeeId，不接受入参覆盖）。
- 故覆盖重算采用降级方案：克隆源 `ErpHrSalary` → 按 overrides 覆盖薪酬项目字段 → 重算 gross/tax/net。
- **社保/公积金沿用源期间值**（master 驱动，非月工资派生；社保基数钳制已在源期间核算时由 `SocialInsuranceCalculator.clamp` 应用）。
- 0831-2 计算规则零修改，`IncomeTaxCalculator.calculate` 入参路径复用（gross/specialDeduction 走入参，历史累计窗口按模拟期查询）。

---

## 五、薪资调整批量模拟

### 5.1 批量调薪场景

```
HR 选择批量调薪模拟
    │
    ├─► 选择员工范围（部门/岗位/职级筛选）
    │
    ├─► 选择调整类型：
    │       ├─ 固定调薪（基本工资 +X 元）
    │       ├─ 比例调薪（基本工资 ×X%）
    │       ├─ 津贴调整（+/- 固定金额）
    │       └─ 职位级别调薪（按职级映射表）
    │
    ├─► 预览影响人数/总额/人均增幅
    │
    ├─► 确认生成模拟 → 每条调整记录写入 ErpHrSalarySimulationItemAdjustment
    │
    └─► 系统重新计算所有受影响员工的模拟薪酬
```

> 🟢 SAP SuccessFactors "Mass What-If" 批量调薪模拟。

### 5.2 调薪影响报表

| 维度 | 内容 |
|------|------|
| 调薪人数 | 覆盖员工数 |
| 调薪总额 | 月成本增加总计 |
| 人均调薪 | 平均增幅（元/%） |
| 部门分布 | 各部门调薪总额及增幅 |
| 职级分布 | 不同职级调薪情况 |
| 个税影响 | 调薪导致的个税增加 |
| 公司成本影响 | 含社保/公积金公司部分的总额变化 |

---

## 六、跨域协作

| 对端 | 协作方式 |
|------|---------|
| ErpHrSalary | CONVERTED 时创建正式薪酬记录 |
| ErpHrSalaryItem | 复用薪酬项目定义和公式引擎 |
| ErpHrSocialInsuranceConfig | 社保计算复用 |
| ErpHrTaxConfig | 个税计算复用 |

---

## 七、关键业务规则总结

1. **模拟不影响正式数据**：CONVERTED 前，模拟数据不影响任何正式薪酬、余额、历史记录
2. **复制时冻结源数据**：复制操作瞬间快照源薪酬行的值，后续源期间修改不影响已创建的模拟
3. **即时应变**：每次调整一个项目后自动重新计算所有关联项目（个税、社保、实发）
4. **审批必过不可跳过**：模拟值不可直接从 DRAFT 转正式，必须有审批记录
5. **目标期间去重**：同一员工同一期间只能有一条正式薪酬（模拟转正前检查冲突）
6. **可追溯**：模拟通过 `sourceSalaryId` 关联回源薪酬。**追溯单向** `Simulation.convertedSalaryId → ErpHrSalary`（不在 ErpHrSalary 加 `convertedFromSimulationId` 列，核心零污染）；反向经查询 `findSimulationsByConvertedSalary(salaryId)` 补全

## 参考

- `docs/design/human-resource/payroll.md`（薪酬核算基础设计）
- `docs/design/human-resource/README.md`（HR 域基础实体）
- `docs/design/human-resource/state-machine.md`（薪酬状态机）
- 🟢 Odoo Payroll "Simulation Mode"（模拟模式 + 差异报表）
- 🟢 SAP SuccessFactors "What-If Simulation"（假设模拟 + 批量调薪）
- ⚪ 中国薪酬管理实务：调薪前模拟社保/个税影响是标准操作
