# 人力资源管理域 - UI 模式指南

## 目的

定义 HR 域统一 UI 模式与组件规范，确保 AMIS 页面实现的一致性。本文档面向 AMIS 页面开发者与 Nop 平台视图模型（view.xml）配置者。

## 设计原则

- 每个实体对应一套标准 CRUD 页面（列表+表单+详情）
- 状态相关页面使用平台 `app-action` 组件 + 条件按钮
- 看板视图（Kanban）用于管道管理类页面（招聘、休假审批）
- 标签页布局用于员工档案等聚合信息
- 树形组件用于组织架构
- 甘特图插件用于排班/休假日历（第三方集成）

---

## 一、员工列表页

### 1.1 页面布局

```
┌──────────────────────────────────────────────────────────────┐
│  [筛选条件栏]                                                 │
│  部门 ▼  职位 ▼  状态 ▼  关键词 □□□□□□□□  搜索                    │
├──────────────────────────────────────────────────────────────┤
│  [批量操作: 导出 / 调动 / 离职]    [+ 新增员工]               │
├──────────────────────────────────────────────────────────────┤
│  □ │ 工号 │ 姓名  │ 部门  │ 职位 │ 状态 │ 电话  │ 操作      │
│  ☑ │ E001 │ 张三  │ 技术部 │ 开发 │ ACT  │ 138.. │ ✎  ⋮   │
│  □ │ E002 │ 李四  │ 市场部 │ 主管 │ PRO  │ 139.. │ ✎  ⋮   │
├──────────────────────────────────────────────────────────────┤
│  [分页控件]                                                   │
└──────────────────────────────────────────────────────────────┘
```

### 1.2 筛选器

| 筛选 | 组件 | 数据源 |
|------|------|--------|
| 部门 | TreeSelect | ErpHrDepartment（层级） |
| 职位 | Select | ErpHrPosition |
| 状态 | Select（多选） | dict `erp-hr/employment-status` |
| 关键词 | InputText | 模糊匹配 name/code/phone |

### 1.3 列表列

| 列 | 排序 | 说明 |
|----|------|------|
| 工号 | Y | code，链接到详情页 |
| 姓名 | Y | fullName |
| 部门 | Y | departmentId → name |
| 职位 | Y | positionId → name |
| 雇佣状态 | Y | 带颜色标签（ACTIVE=绿 / PROBATION=黄 / RESIGNED=灰） |
| 手机号 | N | — |
| 入职日期 | Y | hireDate |
| 操作 | N | 编辑/更多（dropdown） |

### 1.4 条件按钮

- `employmentStatus=ACTIVE` 时显示：【办理离职】【部门调动】
- `employmentStatus=PROBATION` 时显示：【转正】【办理离职】

---

## 二、员工详情页

### 2.1 标签页布局

```
┌──────────────────────────────────────────────────────────────┐
│  基本信息 │ 劳动合同 │ 薪酬记录 │ 考勤 │ 休假 │ 工时表        │
├──────────────────────────────────────────────────────────────┤
│   [当前标签页内容]                                            │
│                                                              │
│   头像 ｜ 姓名: 张三  工号: E001                              │
│   ──────────────────────────────────────────                  │
│   部门: 技术部    职位: 高级开发工程师                          │
│   直属上级: 王五  入职: 2024-03-01                            │
│   状态: ● ACTIVE  类型: FULL_TIME                            │
│   ──────────────────────────────────────────                  │
│   [编辑个人信息]                                              │
│                                                              │
│   · 出生日期: 1990-05-15  ｜ 性别: 男                         │
│   · 证件号: **************  ｜ 证件类型: 身份证                 │
│   · 手机: 138****0000      ｜ 邮箱: zhangsan@xxx.com          │
│   · 紧急联系人: 李四 / 139****0001                            │
│   · 银行账户: 工行 ****1234     ｜ 社保号: SZ********          │
│   · 个税档案号: ********                                        │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 各标签页内容

| 标签 | 内容 |
|------|------|
| 基本信息 | 个人信息、联系方式、银行账户、社保/个税信息 |
| 劳动合同 | 合同列表（当前合同+历史合同）、到期提醒 |
| 薪酬记录 | 月度薪酬列表（各月应发/实发/社保/个税）、年度汇总 |
| 考勤 | 月度考勤汇总（出勤/迟到/早退/旷工）、打卡日历 |
| 休假 | 假期余额、休假申请历史 |
| 工时表 | 工时填报记录、按项目/月份汇总 |

---

## 三、组织架构图

### 3.1 布局

```
┌──────────────────────────────────────────────────────────────┐
│  CEO                                                         │
│    │                                                         │
│    ├── 技术部                                                 │
│    │    ├── 前端组                                            │
│    │    │    ├── 张三（组长）                                  │
│    │    │    ├── 李四                                         │
│    │    │    └── 王五                                         │
│    │    │                                                      │
│    │    └── 后端组                                            │
│    │         ├── 赵六（组长）                                  │
│    │         └── 孙七                                         │
│    │                                                          │
│    ├── 市场部                                                 │
│    │    └── ...                                               │
│    │                                                          │
│    └── 财务部                                                 │
│         └── ...                                               │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 交互

- 树节点展开/折叠
- 点击部门节点显示部门详情（成本中心、人数、负责人）
- 点击员工节点跳转到员工详情页
- 搜索高亮匹配节点
- 支持按部门/职位过滤

### 3.3 实现方案

AMIS 使用 `tree` 组件或 `formula` 控制 `crud` 的父子关系。

数据源：`ErpHrDepartment`（parentId 自引用）+ `ErpHrEmployee`（departmentId 关联）。

---

## 四、休假日历

### 4.1 团队休假视图

```
月份:  2026-07                               [◀]  [▶]
┌──────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│ 成员  │ 27  │ 28  │ 29  │ 30  │ 1   │ 2   │ 3   │
│       │ 六  │ 日  │ 一  │ 二  │ 三  │ 四  │ 五  │
├──────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
│ 张三  │ ░░░ │ ░░░ │     │     │ ███ │ ███ │ ███ │  ← 年假
│ 李四  │     │     │ ▓▓▓ │ ▓▓▓ │     │     │     │  ← 事假
│ 王五  │     │     │     │     │     │     │     │
│ 赵六  │ ░░░ │ ░░░ │ ░░░ │ ░░░ │ ░░░ │     │     │  ← 年假
│ 孙七  │     │     │     │ ███ │ ███ │ ███ │ ███ │  ← 年假
└──────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
               图例: ███ 年假  ▓▓▓ 事假  ░░░ 病假
```

### 4.2 交互

- 月份切换
- 点击日期查看当天请假详情
- 每个成员行显示该月可用假期余额
- 颜色区分休假类型
- 用于资源分配冲突检测（同一人同一时间两单请假自动告警）

### 4.3 数据源

ErpHrLeaveRequest（APPROVED）+ ErpHrEmployee。

---

## 五、工时表录入

### 5.1 周工时网格

```
项目: [下拉选择] ▼   任务: [自动补全]   周: 2026 W27 (6/29-7/5)

┌──────────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
│ 项目     │ 周一 │ 周二 │ 周三 │ 周四 │ 周五 │ 周六 │ 周日 │
│          │ 6/29 │ 6/30 │ 7/1  │ 7/2  │ 7/3  │ 7/4  │ 7/5  │
├──────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤
│ ERP-销   │ 8h   │ 4h   │      │      │      │      │      │
│ 售模块    │      │      │      │      │      │      │      │
├──────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤
│ ERP-采   │      │ 4h   │ 6h   │ 8h   │ 6h   │      │      │
│ 购模块    │      │      │      │      │      │      │      │
├──────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤
│ 日常事   │      │      │ 2h   │      │ 2h   │      │      │
│ 务       │      │      │      │      │      │      │      │
├──────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤
│ 合计     │ 8h   │ 8h   │ 8h   │ 8h   │ 8h   │ 0h   │ 0h   │  ← 40h
└──────────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘

[+ 添加行]  [保存草稿]  [提交审批]
```

### 5.2 设计要点

- 项目下拉：从 `project` 域读取活跃项目（支持搜索）
- 任务自动补全：输入关键词模糊匹配任务名称
- 每日合计校验：单日不可超过 24h，不可超过标准工时（可配置）
- 每周合计显示：按项目汇总 + 按周汇总
- 行内编辑：直接点击单元格输入数字
- 复制上周模板：快速填充相似周
- 🟢 Axelor `TimesheetLine.js` React 前端周网格组件

---

## 六、招聘管道看板

### 6.1 Kanban 布局

```
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ 新进     │  │ 筛选中   │  │ 面试中   │  │ 待发Offer│  │ 已录用   │
│ (3)      │  │ (5)      │  │ (4)      │  │ (2)      │  │ (1)      │
├──────────┤  ├──────────┤  ├──────────┤  ├──────────┤  ├──────────┤
│ ┌──────┐ │  │ ┌──────┐ │  │ ┌──────┐ │  │ ┌──────┐ │  │ ┌──────┐ │
│ │张三  │ │  │ │李四  │ │  │ │王五  │ │  │ │赵六  │ │  │ │孙七  │ │
│ │后端  │ │  │ │Java  │ │  │ │高级  │ │  │ │20K   │ │  │ │7/15  │ │
│ │来自:  │ │  │ │5年   │ │  │ │3轮   │ │  │ │已发  │ │  │ │入职  │ │
│ │Boss  │ │  │ │      │ │  │ │安排中│ │  │ │7/10  │ │  │ │      │ │
│ │7/2   │ │  │ │7/3   │ │  │ │7/8   │ │  │ │到期  │ │  │ │      │ │
│ └──────┘ │  │ └──────┘ │  │ └──────┘ │  │ └──────┘ │  │ └──────┘ │
│ ┌──────┐ │  │ ┌──────┐ │  │ ┌──────┐ │  │          │  │          │
│ │周八  │ │  │ │吴九  │ │  │ │       │ │  │          │  │          │
│ │前端  │ │  │ │全栈  │ │  │ │       │ │  │          │  │          │
│ └──────┘ │  │ └──────┘ │  │ └──────┘ │  │          │  │          │
└──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘

              [拖拽候选人到对应阶段更新状态]
```

### 6.2 卡片内容

| 字段 | 说明 |
|------|------|
| 姓名 | 候选人姓名 |
| 应聘职位 | positionId → name |
| 来源渠道 | source（带图标） |
| 更新时间 | 最近状态变更时间 |
| 操作按钮 | 面试安排 / 发 Offer / 拒绝（根据阶段显示不同操作） |

### 6.3 实现方案

AMIS CRUD + `list` / `cards` 模式，或自定义 Kanban 插件。状态变更通过拖拽触发 `status` 字段更新。

> 🟢 Odoo `hr_recruitment` Kanban 视图（参考 drag-and-drop + stage 统计）。
> 🟢 AureusERP `recruitment.php` 面试流程阶段视图。

---

## 七、薪酬审批页

### 7.1 薪酬汇总表

```
薪酬期间: 2026-06      总应发: ¥1,280,000.00   总实发: ¥986,000.00

┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ 部门     │ 人数     │ 应发合计  │ 社保(公)  │ 个税     │ 实发合计  │
├──────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ 技术部   │ 45       │ 480,000  │ 72,000   │ 38,000   │ 370,000  │
│ 市场部   │ 20       │ 210,000  │ 31,500   │ 15,000   │ 163,500  │
│ 财务部   │ 10       │ 120,000  │ 18,000   │ 10,000   │ 92,000   │
│ ...      │ ...      │ ...      │ ...      │ ...      │ ...      │
├──────────┼──────────┼──────────┼──────────┼──────────┼──────────┤
│ 合计     │ 100      │ 1,280,000│ 192,000  │ 82,000   │ 986,000  │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘

[审核]  [导出]  [查看明细]
```

### 7.2 审批操作

| 审批状态 | 显示操作 |
|----------|----------|
| 待审核 | HR 审核：[通过] [退回（附原因）] |
| 已复核 | 财务：[复核通过] [退回] |
| 财务已审批 | 经理：[审批] [退回] |
| 经理已审批 | [生成银行文件] [发放] |

---

## 八、仪表盘

### 8.1 布局

```
┌──────────────────────┬──────────────────────┐
│  总人数: 128         │  本月入职: 3         │
│  ● ACTIVE 118        │  本月离职: 2         │
│  ● PROBATION 8       │  净增长: +1          │
│  ● RESIGNED 2        │                      │
├──────────────────────┼──────────────────────┤
│  [部门人数分布]       │  [月度入离职趋势]     │
│  ┌────────────┐      │  ┌────────────┐      │
│  │ 饼图       │      │  │ 折线图     │      │
│  │ 技术部35%  │      │  │ 1-6月趋势  │      │
│  │ 市场部20%  │      │  │            │      │
│  └────────────┘      │  └────────────┘      │
├──────────────────────┼──────────────────────┤
│  [休假余额预警]       │  [待办事项]          │
│  ┌────────────┐      │  ┌────────────┐      │
│  │ 年假将过期  │      │  │ 待审批休假 5  │    │
│  │ 张三: 5天  │      │  │ 待审批工时 3  │    │
│  │ 李四: 3天  │      │  │ 合同到期提醒 2 │    │
│  └────────────┘      │  └────────────┘      │
└──────────────────────┴──────────────────────┘
```

### 8.2 指标卡片

| 指标 | 显示方式 | 数据源 |
|------|----------|--------|
| 总人数/在职/试用/离职 | 数值 + 颜色标签 | ErpHrEmployee.employmentStatus |
| 入离职趋势 | 折线图（月粒度） | employmentStatus 按时间序列 |
| 部门人数分布 | 饼图/柱状图 | departmentId 聚合 |
| 休假余额预警 | 列表（30天内年假过期） | ErpHrLeaveRequest 余额 |
| 待办审批 | 数量+列表 | TODO 系统 |

---

## 九、UI 组件映射

| UI 模式 | AMIS 组件 | 说明 |
|---------|-----------|------|
| 列表页 | `crud` + `filter` | 标准 CRUD 列表 |
| 详情页 | `tabs` + `form` | 标签页布局 |
| 组织架构 | `tree` | 层级树 |
| 休假日历 | `calendar` + 自定义渲染 | 甘特风格休假日历 |
| 工时录入 | `grid` + `input-number` | 周网格，行内编辑 |
| Kanban | `cards` / 自定义拖拽 | 招聘管道 |
| 审批页 | `crud` + 条件按钮 | 按审批状态显示操作 |
| 仪表盘 | `grid` + `chart` | 图表 + 指标卡片 |

---

## 十、布局与导航

| 菜单路径 | 对应页面 |
|----------|----------|
| hr/employee | 员工列表 |
| hr/employee/:id | 员工详情 |
| hr/department | 部门列表/组织架构 |
| hr/position | 职位列表 |
| hr/contract | 劳动合同列表 |
| hr/attendance | 考勤管理 |
| hr/timesheet | 工时表（个人/团队） |
| hr/salary | 薪酬核算（HR） |
| hr/salary/:id | 薪酬明细/审批 |
| hr/leave | 休假申请（员工） |
| hr/leave/calendar | 团队休假日历（主管） |
| hr/recruitment | 招聘管道 Kanban |
| hr/recruitment/request | 招聘需求 |
| hr/recruitment/candidate | 候选人列表 |
| hr/recruitment/interview | 面试安排 |
| hr/recruitment/offer | Offer 管理 |
| hr/dashboard | HR 仪表盘 |

---

## 参考

 - `docs/design/human-resource/README.md`
 - `docs/design/human-resource/use-cases.md`
 - `docs/design/human-resource/state-machine.md`
 - `docs/design/human-resource/payroll.md`
 - `docs/design/human-resource/recruitment.md`
 - 🟢 Axelor Human Resource 前端（React 组件 + 网格/日历/甘特图）
 - 🟢 Odoo `hr_recruitment` Kanban 视图实现
 - Nop AMIS 组件库 `../nop-entropy/docs-for-ai/02-core-guides/amislite.md`

## 树形 CRUD 范式（Department）

人力资源域含 1 个自引用树形实体：`ErpHrDepartment`（组织架构树）。遵循 `docs/design/tree-entity-patterns.md` 跨域范式。

### 列集表

| 实体 | tree-list grid 列集 | 域专用业务约束 |
|------|---------------------|----------------|
| `ErpHrDepartment` | `code` `name` `parentId` `manager` `costCenterId` `orgId` | 组织架构树，F16 复杂页面（节点嵌入员工 + 搜索高亮）successor |

### 配置要点

1. **tree-list grid**：克隆 `<grid id="list">` + 追加 `<selection>children @TreeChildren(max:5)</selection>`
2. **crud main grid="tree-list"**：`<table loadDataOnce="true" sortable="false" pager="none">` + URL `@query:ErpHrDepartment__findList/{@listSelection}?filter_parentId=__null`
3. **add-child simple page**：`<simple name="add-child" form="add"><data><parentId>$id</parentId></data></simple>` + rowActions 追加 `row-add-child-button`（用于「在某部门下新建子部门」）
4. **parentId tree-select 控件**：edit/add 表单 `<cell id="parentId">` 升级为 `<tree-select>`，URL 加 `filter_id__ne=$id` 排除自身防循环引用
5. **picker 升级**：picker.page.yaml table 改 tree 配置（`loadDataOnce + pager=none + filter_parentId=__null`）

### 与 F16 组织架构图复杂页面的边界

F10 仅做标准 tree CRUD（AMIS 内置 tree 表格 + tree-select + add-child）。F16 P2 successor 在此基础上构建复杂页面：节点嵌入员工缩略、搜索高亮、点击跳转员工详情，需自定义 AMIS 组件，不在本范式范围。

详细配置模板与反模式自检见 `docs/design/tree-entity-patterns.md`。

## 主交易实体 form 布局分组

> 适用范围：HR 域 12 个主交易实体（不含已 1500-1 覆盖的 `ErpHrEmployee`，不含 F10 tree 覆盖的 `ErpHrDepartment`）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 决策来源：`docs/plans/2026-07-20-2059-2-f3-p2p3-ext-masterdata-form-layout.md` Phase 0.B。
> 薪酬（Salary）/合同（EmploymentContract）/招聘（Recruitment）等大表单设 `size="lg"` 并按 业务实体（员工/周期）+ 金额 + 审批 + 过账 等业务关键字段分组。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpHrAttendance | baseInfo + clock + exception + audit |
| ErpHrLeaveRequest | baseInfo + detail + approval + audit |
| ErpHrRecruitment | baseInfo + headcount + pipeline + audit |
| ErpHrEmploymentContract | baseInfo + contract + salary + attachment + audit |
| ErpHrEmployeeAssessment | baseInfo + detail + result + audit |
| ErpHrDevelopmentPlan | baseInfo + detail + audit |
| ErpHrSurvey | baseInfo + schedule + eNps + stats + audit |
| ErpHrSalary | baseInfo + gross + deduction + net + payment + approval + posting + meta + audit |
| ErpHrSalarySimulation | baseInfo + period + review + audit |
| ErpHrShiftAssignment | baseInfo + actual + status + audit |
| ErpHrShiftSwapRequest | baseInfo + detail + approval + audit |
| ErpHrTimesheet | baseInfo + period + approval + audit |

### ErpHrSalary 模板（薪酬记录，最大 33 字段实体）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 employeeId[员工] year[年] month[月]
 orgId[业务组织] businessDate[业务日期]
=========>gross[应发项]======
 basicSalary[基本工资] positionAllowance[岗位津贴]
 performanceBonus[绩效奖金] overtimePay[加班费]
 mealAllowance[餐补] transportAllowance[交通补贴]
 otherAllowance[其他补贴] grossSalary[应发合计]
=========>deduction[扣减项]======
 socialInsurance[社保] housingFund[公积金]
 taxAmount[个税] otherDeductions[其他扣减]
=========>net[实发]======
 netSalary[实发合计]
=========>payment[支付信息]======
 paymentStatus[支付状态] paymentDate[支付日期]
 paymentBatchNo[支付批次号] bankFileId[银行代发文件]
=========>approval[审核信息]======
 approveStatus[审核状态] approvedBy[审核人] approvedAt[审核时间]
=========>posting[过账信息]======
 posted[已过账] nopFlowId[工作流ID]
=========>meta[计算元数据]======
 performanceFactor[绩效系数] actualWorkDays[实际工作日]
 requiredWorkDays[应工作日] totalOvertimeHours[总加班时]
 unpaidLeaveDays[无薪假天数] cumulativeData[累计数据]
 reviewNote[审核备注]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpHrEmploymentContract 模板（劳动合同）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[合同编号] employeeId[员工]
 orgId[业务组织] businessDate[业务日期]
=========>contract[合同信息]======
 contractType[合同类型] signDate[签订日期]
 startDate[生效日期] endDate[到期日期]
 probationMonths[试用期(月)] workingHoursPerWeek[每周工时]
 status[状态]
=========>salary[薪酬信息]======
 annualSalary[年薪] monthlySalary[月薪]
 salaryCurrencyId[薪金币种] salaryPayMethod[支付方式]
 socialInsuranceBase[社保基数] housingFundBase[公积金基数]
=========>attachment[附件]======
 attachmentFileId[合同附件]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有 HR 域主实体的 `<form id="query">` 至少含 5 个查询字段。`code`/`name`/`planName`/`simulationName` 配 `filterOp=like`；`employeeId`/`orgId`/`status`/`approveStatus`/`paymentStatus`/`leaveType`/`contractType`/`surveyType` 配 `filterOp=eq`；`year`/`month` 配 `filterOp=eq`；含日期字段（`businessDate`/`date`/`startDate`/`endDate`/`signDate`）配 `filterOp=date-between`。

## Line 子实体 form 分组模板

> 适用范围：HR 域 3 个 Line/child 子实体（TimesheetLine / SalarySimulationItemAdjustment / DevelopmentPlanItem）独立 `view.xml` 的 form 分组。

### ErpHrTimesheetLine 模板（工时行）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 timesheetId[工时表] employeeId[员工]
 workDate[工作日期]
=========>detail[项目信息]======
 projectId[项目] taskId[任务]
 activityType[活动类型]
=========>hours[工时]======
 hours[工时] description[描述]
========^audit[审计信息]=========
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有 HR 域 Line 实体的 `<form id="query">` 至少含 5 个查询字段。`lineNo`（如有）配 `filterOp=eq`；外键头字段（`timesheetId`/`simulationId`/`planId`）配 `filterOp=eq`；`employeeId`/`projectId`/`competencyId` 配 `filterOp=eq`；含日期字段（`workDate`）配 `filterOp=date-between`。
