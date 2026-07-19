# APS 视图按钮需求覆盖分析

## 分析范围

APS 域共 6 个实体：

| 实体 | 分类 | 说明 |
|------|------|------|
| ErpApsOperationOrder | CRUD+Custom | 工序工单（APS 排产核心实体） |
| ErpApsSchedule | CRUD+Custom | 排产方案（排产版本管理） |
| ErpApsConstraint | CRUD | 排产约束（配置实体） |
| ErpApsDispatchLog | Other | 派工日志（系统审计日志） |
| ErpApsDispatchRule | CRUD | 自动派工规则（配置实体） |
| ErpApsOpRouting | CRUD | 替代工艺路线（配置实体） |

**注**：用户提示的 PlanHeader / PlanLine / Forecast / CapacityPlan / SchedulingRule 未在代码库中找到对应实体。实际实现使用 OperationOrder + Schedule + Constraint 体系。以下分析基于实际存在的 6 个实体。

## 期望按钮推导依据

- CRUD 基线（METHODOLOGY §1.1）
- `docs/design/aps/ui-patterns.md` §列表页结构、编辑页通用结构、排产方案、排产约束配置
- `docs/design/aps/state-machine.md` §1-2（OperationOrder 五态迁移：DRAFT→PLANNED→IN_PROGRESS→FINISHED / CANCELLED）
- `docs/design/aps/README.md` §实体清单（Schedule 状态：DRAFT/PUBLISHED/ARCHIVED）
- `docs/design/aps/auto-dispatch.md` §排产约束配置 §一、§四（DispatchRule/DispatchLog 均为计算引擎支持的配置/日志实体，非业务主实体）
- `docs/design/aps/alternative-routing.md` §一（OpRouting 为排产引擎支持实体）

APS 是计算密集型域，核心动作（运行排产、提交排产、开始执行、完工、发布方案）不在生成 CRUD 范围内。

## 逐实体分析

### ErpApsOperationOrder — CRUD+Custom

- **期望按钮**：
  - toolbar: `add-button`, `batch-delete-button` (CRUD 基线)
  - toolbar: 无标准ID — "运行排产"（`run-scheduling-button`）— ui-patterns.md §列表页结构 toolbar `[新建] [运行排产] [导出]`
  - row: `row-view-button`, `row-update-button`, `row-delete-button` (CRUD 基线)
  - row: `row-submit-scheduling-button` — "提交排产" — ui-patterns.md §编辑页通用结构 "工具栏: [保存] [提交排产] [发布]" + §85 "工序编辑页的'提交排产'将工序加入排产队列"
  - row: `row-start-execution-button` — "开始执行" — ui-patterns.md §跨页面导航流 "工序工单列表 → [开始执行] → 工序执行" + state-machine.md §2 PLANNED→IN_PROGRESS
  - row: `row-complete-button` — "完工" — state-machine.md §2 IN_PROGRESS→FINISHED
  - row: `row-cancel-button` — "取消" — state-machine.md §2 DRAFT/PLANNED→CANCELLED

- **实际按钮**：toolbar `add-button`, `batch-delete-button`; row `row-view-button`, `row-update-button`, `row-delete-button` (均在 `row-more-button` 内)

- **差距**：
  - `row-submit-scheduling-button`: **missing** (blocker) — 核心业务操作：DRAFT→PLANNED 状态迁移的唯一入口。ui-patterns.md 明确描述"提交排产将工序加入排产队列"，当前 view.xml 无此按钮
  - `row-start-execution-button`: **missing** (blocker) — 核心业务操作：PLANNED→IN_PROGRESS 状态迁移。ui-patterns.md 跨页面导航流"开始执行"是计划员主要交互路径
  - `row-complete-button`: **missing** (major) — 核心终态迁移 IN_PROGRESS→FINISHED，state-machine.md §2 定义的标准迁移
  - `row-cancel-button`: **missing** (major) — state-machine.md §2 从 DRAFT 和 PLANNED 均有 CANCELLED 路径，是基本单据操作
  - toolbar `run-scheduling-button`: **missing** (minor) — ui-patterns.md 列表页结构 toolbar 标有"运行排产"按钮；属批量操作，频次低于逐行操作

- **判定**：**blocker** — APS 核心实体的核心业务按钮（submit-scheduling, start-execution）完全缺失

### ErpApsSchedule — CRUD+Custom

- **期望按钮**：
  - toolbar: `add-button`, `batch-delete-button` (CRUD 基线)
  - row: `row-view-button`, `row-update-button`, `row-delete-button` (CRUD 基线)
  - row: `row-run-scheduling-button` — "运行排产" — ui-patterns.md §排产方案 ("运行排产按钮在排产方案详情页出现，触发排产计算引擎")
  - row: `row-publish-button` — "发布方案" — ui-patterns.md §排产甘特图 `[运行排产] [发布方案] [保存布局]` + README.md §ErpApsSchedule 状态机 DRAFT→PUBLISHED
  - row: `row-archive-button` — "归档" — README.md §ErpApsSchedule 状态机 PUBLISHED→ARCHIVED（可选）

- **实际按钮**：toolbar `add-button`, `batch-delete-button`; row `row-view-button`, `row-update-button`, `row-delete-button` (均在 `row-more-button` 内)

- **差距**：
  - `row-run-scheduling-button`: **missing** (blocker) — 排产方案的核心功能入口："运行排产触发排产计算引擎"（ui-patterns.md §排产方案）。无此按钮则 Schedule 只相当于一个带日期的空头
  - `row-publish-button`: **missing** (major) — 状态机 DRAFT→PUBLISHED 的唯一入口（ui-patterns.md §排产甘特图 + README.md §ErpApsSchedule 状态机）

- **判定**：**blocker** — 排产方案实体缺少"运行排产"按钮使其失去业务意义

### ErpApsConstraint — CRUD

- **期望按钮**：CRUD 基线（配置/维护实体，ui-patterns.md §排产约束配置 仅描述列表+编辑，无专用按钮需求）
- **实际按钮**：CRUD 基线 ✓
- **差距**：无
- **判定**：**clean**

### ErpApsDispatchLog — Other

- **期望按钮**：仅 `row-view-button`（系统自动生成的审计日志，不可手动创建/编辑/删除；auto-dispatch.md §四定义其为引擎写入的只读日志）
- **实际按钮**：CRUD 基线（add, batch-delete, view, update, delete）
- **差距**：
  - `add-button`: **extra** (minor) — 派工日志由引擎自动创建，不应允许手工新增
  - `row-update-button`: **extra** (minor) — 日志一旦写入应不可变，编辑破坏审计完整性
  - `row-delete-button` / `batch-delete-button`: **extra** (minor) — 日志不应允许删除
- **判定**：**minor** — 多余的 CRUD 按钮不影响功能但不符合审计日志不可变原则

### ErpApsDispatchRule — CRUD

- **期望按钮**：CRUD 基线（规则配置实体，auto-dispatch.md §一 + §5.1 描述规则管理界面为启停开关和参数编辑，均为字段级操作，不需要独立 action 按钮）
- **实际按钮**：CRUD 基线 ✓
- **差距**：无
- **判定**：**clean**

### ErpApsOpRouting — CRUD

- **期望按钮**：CRUD 基线（替代工艺路线配置实体，alternative-routing.md §一描述为排产引擎支持的静态配置）
- **实际按钮**：CRUD 基线 ✓
- **差距**：无
- **判定**：**clean**

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| CRUD+Custom | ErpApsOperationOrder | 5 | blocker | submit-scheduling, start-execution, complete, cancel, run-scheduling 均缺失 |
| CRUD+Custom | ErpApsSchedule | 2 | blocker | run-scheduling, publish 缺失 |
| CRUD | ErpApsConstraint | 0 | clean | — |
| Other | ErpApsDispatchLog | 3 (extra) | minor | 系统日志不应具有 add/update/delete |
| CRUD | ErpApsDispatchRule | 0 | clean | — |
| CRUD | ErpApsOpRouting | 0 | clean | — |

### 总评
- 总实体数：6
- 无差距实体：3（50%）
- Blocker 差距：2 实体（ErpApsOperationOrder, ErpApsSchedule）
- Major 差距：0 单独实体（OperationOrder 的 major 差距与 blocker 共存）
- Minor/Info 差距：1 实体（ErpApsDispatchLog）

APS 是计算密集型域，合理预期其 CRUD 页面需要补充大量域专用按钮。两个核心实体（OperationOrder 和 Schedule）目前仅有 codegen 生成的 CRUD 基线，缺少所有排产业务按钮（提交排产、开始执行、完工、运行排产、发布方案）。这些按钮需要在 BizModel 层实现对应 `@BizMutation` 后再挂接到 view.xml 中。
