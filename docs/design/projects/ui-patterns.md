# 项目管理域页面设计要点

> 本文档定义项目管理域关键业务页面的结构布局、交互模式与导航流程。
> 字段定义以 `model/app-erp-projects.orm.xml` 为准，业务语义与状态机见 `state-machine.md`、`cost-collection.md`。

## 设计原则

1. **项目树与任务看板**：项目以树形展示（项目 → 任务 → 子任务），任务支持看板视图（按状态分组拖拽）。
2. **工时录入简洁**：工时记录支持周视图批量录入（按日期×项目×任务 × 活动类型 × 时长），减少逐条填写。
3. **成本/预算可视化**：项目详情页展示预算执行进度条（预算 vs 实际成本），超阈值标记。
4. **项目作为辅助核算维度**：其他域的业务单据（采购/销售/费用）可通过选择项目编码关联到项目，在项目详情页集中展示关联单据。

## 页面清单

| 页面 | 类型 | 主要用户 | 复杂度 |
|------|------|----------|--------|
| 项目编辑 | 表单 | 项目经理 | ★★☆ |
| 项目详情（仪表板） | 仪表板式详情 | 项目经理 | ★★★ |
| 任务看板 | 看板视图 | 项目成员 | ★★☆ |
| 工时录入 | 周视图表格 | 项目成员 | ★★☆ |
| 工时报表 | 报表 | 项目经理 | ★★☆ |
| 项目成本分析 | 图表 | 项目经理/财务员 | ★★☆ |

## 各页面设计要点

### 项目详情（仪表板）

**页面入口**：项目管理 → 项目列表 → 点击项目

```
┌────────────────────────────────────────────────────────────┐
│ 项目: PRJ-2026-001  移动端 App 开发  🟢 进行中              │
├────────────────────────────────────────────────────────────┤
│ ┌─ 基本信息 ───────────┐ ┌─── 预算/成本 ────────────────┐ │
│ │ 客户: 某某公司        │ │ 预算: ¥ 500,000               │ │
│ │ 项目经理: 张三         │ │ 已发生成本: ¥ 320,000 (64%)   │ │
│ │ 起止: 2026-01-01~06-30│ │ ████████████░░░░░░░░ 64%      │ │
│ │ 项目类型: 交付项目     │ │ 预算剩余: ¥ 180,000           │ │
│ └───────────────────────┘ └──────────────────────────────┘ │
├────────────────────────────────────────────────────────────┤
│ 任务进度: 25/40 任务已完成 (62.5%)                         │
│ ██████████████████████░░░░░░░░░░░░░░ 62.5%                │
├────────────────────────────────────────────────────────────┤
│ 关联单据                                                    │
│ ┌────┬──────────┬────────┬────────┬──────────────┐       │
│ │ 类型│ 单号     │ 金额   │ 日期   │ 操作          │       │
│ │ 采购│ PO-001   │ 50,000 │ 02-15 │ [查看]         │       │
│ │ 费用│ EXP-001  │ 2,000  │ 03-01 │ [查看]         │       │
│ │ 工时│ TS-001   │ 8,000  │ 03-15 │ [查看]         │       │
│ └────┴──────────┴────────┴────────┴──────────────┘       │
└────────────────────────────────────────────────────────────┘
```

**要点**：
- 预算进度条颜色：<80%=绿色、80-95%=黄色、>95%=红色
- 关联单据自动聚合（从其他域按 projectId 查询）

### 任务看板

**页面入口**：项目管理 → 项目详情 → 任务看板

```
┌─────────┬──────────┬─────────┬──────────┐
│ 待开始   │ 进行中    │ 已完成   │ 阻塞     │
├─────────┼──────────┼─────────┼──────────┤
│ ┌─────┐ │ ┌─────┐  │ ┌─────┐ │ ┌─────┐  │
│ │TASK1│ │ │TASK2│  │ │TASK5│ │ │TASK4│  │
│ │张四  │ │ │李五  │  │ │张三  │ │ │王六  │  │
│ │优先级│ │ │进行中│  │ │已完成│ │ │阻塞  │  │
│ │高    │ │ │60%   │  │ │     │ │ │需资源│  │
│ └─────┘ │ └─────┘  │ └─────┘ │ └─────┘  │
│ ┌─────┐ │ ┌─────┐  │         │          │
│ │TASK3│ │ │TASK6│  │         │          │
│ │王五  │ │ │赵七  │  │         │          │
│ │中    │ │ │20%   │  │         │          │
│ └─────┘ │ └─────┘  │         │          │
└─────────┴──────────┴─────────┴──────────┘
```

**要点**：
- 卡片拖拽切换状态（自动更新 taskStatus）
- 卡片显示：任务标题、负责人、进度百分比、优先级标记（紧急=红/高=橙/普通=黄/低=灰）[注：依据 ORM 字典 erp-prj/priority: URGENT(紧急)/HIGH(高)/NORMAL(普通)/LOW(低)]
- 阻塞列卡片需填写阻塞原因 [注：当前 ORM 中 ErpPrjTask 尚无 blockReason 独立字段，设计意图，待补充 ORM]

### 工时录入

**页面入口**：项目管理 → 工时管理

```
┌──────────────────────────────────────────────────────────┐
│ 期间: [本周] [上周] [自定义]  项目: [选择/全部]            │
├──────────────────────────────────────────────────────────┤
│ 成员: 张三                                                │
│ ┌────────┬──────┬──────┬──────┬──────┬──────┬──────┬────┐│
│ │ 项目/任务│ 周一  │ 周二  │ 周三  │ 周四  │ 周五  │ 周六  │合计│
│ ├────────┼──────┼──────┼──────┼──────┼──────┼──────┼────┤│
│ │ 项目A   │      │      │      │      │      │      │    ││
│ │ ├ 开发  │ 8h   │ 8h   │ 6h   │ 8h   │ 8h   │  -   │ 38h││
│ │ ├ 测试  │  -   │  -   │ 2h   │  -   │  -   │  -   │ 2h ││
│ │ │ 合计  │ 8h   │ 8h   │ 8h   │ 8h   │ 8h   │  -   │ 40h││
│ ├────────┼──────┼──────┼──────┼──────┼──────┼──────┼────┤│
│ │ 项目B   │  -   │  -   │  -   │  -   │  -   │  -   │  - ││
│ └────────┴──────┴──────┴──────┴──────┴──────┴──────┴────┘│
│ [提交] [保存草稿]                                          │
└──────────────────────────────────────────────────────────┘
```

**要点**：
- 单元格可直接编辑输入小时数（支持步进 0.5h）
- 提交后自动计算人工成本（工时 × 成员标准成本率）
- 周合计超过 40h 时给出提示（非强制）

## 跨页面导航流

```
项目列表 → [新建项目] → 项目详情
    ↓
项目详情 → [任务看板] → [拖拽变更状态] → [点击卡片编辑]
    ↓
项目详情 → [工时录入] → [提交] → 自发成本凭证
    ↓
项目详情 → [成本分析] → [穿透到凭证明细]
    ↓
其他域单据 → 选择项目 → 自动归集到项目成本
```

## 调研参考

| 设计点 | 参考来源 | 应用方式 |
|--------|----------|----------|
| 任务看板拖拽 | OFBiz#Project tasks | 看板视图按状态分组 |
| 工时周视图批量录入 | OFBiz#Timesheet | 日期×项目/任务的网格录入 |
| 项目成本归集 + 预算控制 | ERPNext#Cost Center + Project | 成本进度条 + 预算阈值 |
| 关联单据自动聚合 | Odoo#project | 项目详情聚合页展示关联业务单据 |

## 主交易实体 form 布局分组

> 适用范围：项目域 7 个主交易实体（不含已 1500-1 覆盖的 `ErpPrjProject` / `ErpPrjProjectSettlement` 与 F4P2 已覆盖的 `ErpPrjCostCollection`）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 决策来源：`docs/plans/2026-07-20-2059-1-f3-p1-mfg-tier-form-layout.md` Phase 0.C。
> 项目域主实体分化大：Billing/Budget 共享 baseInfo+amount+status+posting+audit；Timesheet 突出员工/工时/日期；Task 突出 DAG 前驱/状态；ProjectPnl 是超大表单（≥20 字段）按收入/成本/利润拆组。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpPrjBilling | baseInfo + amount + status + posting + audit |
| ErpPrjBudget | baseInfo + status + audit |
| ErpPrjMilestone | baseInfo + amount + status + audit |
| ErpPrjProjectPnl | baseInfo + revenue + cost + profit + amount + status + posting + audit（≥20 字段，size=lg） |
| ErpPrjProjectUser | baseInfo + audit |
| ErpPrjTask | baseInfo + schedule + hours + dag + audit |
| ErpPrjTimesheet | baseInfo + hours + status + posting + audit |

### ErpPrjTask 模板（DAG 前驱/状态实体）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 title[任务标题] projectId[项目]
 parentTaskId[父任务] assigneeId[指派给]
 priority[优先级] sortNum[排序]
=========>schedule[进度]======
 plannedStartDate[计划开始] plannedEndDate[计划结束]
 actualStartDate[实际开始] actualEndDate[实际结束]
=========>hours[工时]======
 estimatedHours[预估工时] actualHours[实际工时]
=========>dag[依赖]======
 dependsOnId[前驱任务] status[状态]
 blockReason[阻塞原因]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpPrjTimesheet 模板（员工/工时/日期）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[工时单号] orgId[业务组织]
 projectId[项目] taskId[任务]
 userId[员工] workDate[工作日期]
 activityTypeId[活动类型]
=========>hours[工时与成本]======
 hours[工时] costRate[成本费率]
 costAmount[成本金额] currencyId[币种]
=========>status[状态信息]======
 status[状态] approvedBy[审核人]
 approvedAt[审核时间]
=========>posting[过账信息]======
 posted[已过账] postedAt[过账时间]
 postedBy[过账人]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有项目域主实体的 `<form id="query">` 至少含 5 个查询字段。`code` 配 `filterOp=like`；`projectId`/`taskId`/`userId`/`status`/`docStatus` 配 `filterOp=eq`；`workDate`/`businessDate` 配 `filterOp=date-between`。

## Line 子实体 form 分组模板

> 适用范围：项目域 4 个 Line 子实体独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 项目域 Line 模板**统一**：baseInfo + amount + reference + audit（4 组）。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpPrjBillingLine | baseInfo + amount + reference + audit |
| ErpPrjBudgetLine | baseInfo + amount + reference + audit |
| ErpPrjCostCollectionLine | baseInfo + amount + reference + audit |
| ErpPrjProjectSettlementLine | baseInfo + amount + reference + audit |

### ErpPrjBudgetLine 模板（基准）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 lineNo[行号] costCategory[成本类别]
 subjectId[科目] taskId[任务]
=========>amount[金额信息]======
 plannedAmount[计划金额] committedAmount[承诺金额]
 actualAmount[实际金额]
=========>reference[业务关联]======
 budgetId[项目预算]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有项目域 Line 实体的 `<form id="query">` 至少含 5 个查询字段。`lineNo` 配 `filterOp=eq`；`costCategory`/`subjectId`/`taskId` 配 `filterOp=eq`；`budgetId` 配 `filterOp=eq`。
