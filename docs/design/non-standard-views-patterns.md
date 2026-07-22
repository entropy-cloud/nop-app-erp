# F13 非标准视图模式（看板 / 时间线 / 日历）

> 设计 owner doc。**稳定产品基线**，非迁移历史或执行状态。
> 实施计划：`docs/plans/2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md`
> 路线图：`docs/backlog/frontend-ui-roadmap.md` §F13（line 318-334 / 544）
> 兄弟范式：`docs/design/page-structure-patterns.md`（F12/F16 标准页面结构）

## 1. 目的与范围

本文档定义 F13 非标准列表视图（**看板 / 时间线 / 日历**）的应用层业务语义、AMIS 组件选择、状态变更 mutation 契约与反模式自检表。**与标准 CRUD 列表视图互补**：标准 CRUD 适合单实体表格浏览与编辑，非标准视图适合按状态/时间/日期维度的视觉化浏览与状态切换。

**7 落地页面**：

| # | 域 | 页面 | 类型 | 文件 |
|---|---|------|------|------|
| 1 | crm | 商机看板 | 看板（动态列） | `module-crm/erp-crm-web/.../pages/ErpCrmLead/opportunity-kanban.page.yaml` |
| 2 | cs | 工单看板 | 看板（固定 6 列） | `module-cs/erp-cs-web/.../pages/ErpCsTicket/kanban.page.yaml` |
| 3 | projects | 任务看板 | 看板（固定 4 列） | `module-projects/erp-prj-web/.../pages/ErpPrjTask/kanban.page.yaml` |
| 4 | crm | 活动时间线 | 时间线（原生） | `module-crm/erp-crm-web/.../pages/ErpCrmActivity/timeline.page.yaml` |
| 5 | cs | 工单操作时间线 | 时间线（原生） | `module-cs/erp-cs-web/.../pages/ErpCsTicketAction/timeline.page.yaml` |
| 6 | crm | 活动日历 | 日历（原生） | `module-crm/erp-crm-web/.../pages/ErpCrmActivity/calendar.page.yaml` |
| 7 | hr | 团队休假日历 | 日历矩阵（custom table） | `module-hr/erp-hr-web/.../pages/ErpHrLeaveRequest/team-vacation-calendar.page.yaml` |

**Non-Goals**：
- 经营看板（dashboard）类视图——见 `docs/design/page-structure-patterns.md` §3
- 甘特图（aps 排产）——属 F16 高风险 territory，需独立 PoC（拖拽 + 缩放 + 颜色编码 + 约束叠加）
- 新增 React/Vue 自定义组件——优先 AMIS 原生组件 + custom JSON 组装
- 跨域聚合视图（如全公司看板）——本范式聚焦单域列表页的非标准视图

## 2. 看板范式（含拖拽降级策略）

### 2.1 Phase 0 Explore (a) 裁决：拖拽不可用 → 列式 crud + row-action

**PoC 结论**：grep 实时仓库 `nop-entropy/nop-frontend-support/nop-web-site/target/classes/META-INF/resources/assets/` 证实 AMIS bundle **不含「跨 crud 行拖拽」原生组件**。vendor-amis-*.js 仅暴露行内 sortable + input-table 列拖拽。HTML5 drag-drop 在 AMIS React 渲染层下需手写大量 dnd 适配器，且**与状态机语义冲突**：

- ErpPrjTask 状态机仅允许相邻态迁移（TODO→IN_PROGRESS→DONE/BLOCKED），任意态拖拽会被后端 mutation 拒绝
- ErpCsTicket 状态机严格（NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED/REOPEN/CANCEL）
- ErpCrmLead moveStage 经 LeadProcessor 守卫 isWonStage + 阶段序列

**降级方案**：每列一个状态过滤的 crud（列式布局视觉接近看板）+ 行级 row-action「移动到状态/阶段」按钮（dialog 选目标 → 触发对应状态机 mutation）。状态变更语义完整保留，拖拽 UX 归 §7 successor。

### 2.2 落地范式（独立 page.yaml，列式 crud）

**3 看板结构共性**：
- `type: page` 独立 page.yaml（同 F16 三单匹配范式，不消费 view.xml）
- 顶部 `type: form` 过滤器（项目/客户/线索 ID）
- 主体 `type: grid`（水平多列）每列一个 `type: crud`，按状态/stage 过滤
- 每列 crud 含 `headerToolbar: []` + `footerToolbar: ["statistics"]` + `syncLocation: false`
- 行级 row-action 按列状态动态暴露：仅当前列的状态可触发的 mutation 暴露按钮
- 卡片字段（custom render via `type: tpl`）：标题 + 优先级色块 + SLA/截止日期 + blockReason

**ErpPrjTask 任务看板（4 列）**：
```yaml
- type: grid
  columns:
    - type: crud  # TODO → startTask
      api: { data: { query: "...filter_status:\"TODO\"...", variables: { sid: "${id}" } } }
      columns: [..., { type: operation, buttons: [{ label: "开始", api: { query: "mutation...ErpPrjTask__startTask..." } }] }]
    - type: crud  # IN_PROGRESS → completeTask / blockTask
    - type: crud  # DONE（只读）
    - type: crud  # BLOCKED → unblockTask（className: bg-danger-subtle）
```

**ErpCsTicket 工单看板（6 列）**：NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED，按 erp-cs/ticket-status 字典。SLA 超时卡片 🔴 标记 tpl：
```yaml
- name: deadlineDateTime
  type: tpl
  tpl: '<span class="${deadlineDateTime && !isSlaCompleted ? "text-danger" : "text-muted"}">${deadlineDateTime ? deadlineDateTime : "—"} ${deadlineDateTime && !isSlaCompleted ? "🔴" : ""}</span>'
```
NEW 列 `className: "bg-warning-subtle"` 高亮（待分派闪烁语义）。

**ErpCrmLead 商机看板（动态列）**：阶段从 ErpCrmStage 实体读取，**经 `service + each` 动态渲染一列 crud per stage**：
```yaml
- type: service
  name: stagesLoader
  api: { data: { query: "...ErpCrmStage__findPage(orderBy:\"sequence ASC\")..." }, adaptor: "return { data: { stages, stageOptions } }" }
  body:
    - type: each
      name: stages
      body:
        - type: crud  # filter_leadType="OPPORTUNITY" + filter_stageId=${id}
          title: "${stageName} ${isWonStage ? ' 🏆' : ''}"
          columns: [..., { type: operation, buttons: [{ label: "移动到阶段", visibleOn: "${!isWonStage}", dialog: { ...toStageId picker source: "${stageOptions}" } }] }]
```

### 2.3 守卫实现（isWonStage / BLOCKED / 终态）

| 守卫 | 实现层 | 实现 |
|------|--------|------|
| ErpCrmLead isWonStage 禁止拖出 | 前端 visibleOn + 后端 LeadProcessor | row-action `visibleOn: "${!isWonStage}"` 隐藏「移动到阶段」按钮；后端 mutation 二次守卫拒绝非法迁移 |
| ErpPrjTask BLOCKED 必填 blockReason | 后端 BizModel + 前端 dialog | `blockTask` mutation dialog 收集 blockReason（textarea required）；后端 `ERR_TASK_BLOCK_REASON_REQUIRED` 抛错 |
| ErpCsTicket 终态（CLOSED/CANCELLED）只读 | 前端无 row-action | CLOSED/CANCELLED 列 crud 仅展示字段，无 operation 列 |
| ErpPrjTask DONE 只读 | 前端无 row-action | DONE 列 crud 仅展示，无 operation 列 |

**关键约束**：守卫优先在后端 BizModel 强制（防绕过），前端 visibleOn 仅作 UX 预拦截。

## 3. 时间线范式（原生 vs custom 组装）

### 3.1 Phase 0 Explore (b) 裁决：原生 `type: timeline` 存在，直接使用

**PoC 结论**：实时仓库 `assets/Timeline-Brcmgfvk.js` 注册 `TimelineRenderer`（`type: timeline`）。vendor-amis-*.js grep 确认 `U({type:\`timeline\`...})`。

### 3.2 落地范式（service + adaptor 映射 + 原生 timeline）

**共性结构**：
```yaml
- type: service
  name: xxxTimeline
  api:
    data: { query: "...findPage(...orderBy:\"<dateField> DESC\")..." }
    adaptor: |
      // 将实体行映射为 timeline items: [{time, title, detail, color}]
      const items = rows.map(function(r){
        return { time: r.<dateField>, title: '[type] ...', detail: r.<descField>, color: typeColor[r.<typeField>] };
      });
      return { data: { items } };
  body:
    - type: timeline
      items: "${items}"
      direction: vertical
      placeholder: "暂无..."
```

**ErpCrmActivity 活动时间线**：
- dateField = `activityDate`（实体实际字段，非 plan baseline 提及的 occurredAt）
- typeField = `activityType`（NOTE/CALL/EMAIL/MEETING）
- 类型图标 + 颜色：CALL=📞蓝 / EMAIL=✉️灰 / MEETING=👥绿 / NOTE=📝黄

**ErpCsTicketAction 操作时间线**：
- dateField = `createTime`（实体操作发生时间戳，非 plan baseline 提及的 operatedAt）
- 标题：`[actionType] fromStatus → toStatus · 操作人: operatorId`
- 类型着色：ASSIGN=蓝 / CLOSE=绿 / CANCEL=红 / ESCALATE=黄 / NOTE=灰

### 3.3 反模式

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 字段名臆测（occurredAt/operatedAt） | 实时仓库核实实体实际字段（activityDate/createTime） |
| 拖拽 timeline 节点（无业务语义） | timeline 仅展示，状态变更经独立 mutation |
| 自实现 timeline 组件 | 用原生 `type: timeline`（assets 已注册） |

## 4. 日历范式（日/周/月 + 矩阵）

### 4.1 Phase 0 Explore (b) 裁决：原生 calendar 用于标准视图，矩阵用 custom table

**PoC 结论**：实时仓库 `assets/Calendar-BRmv1Joe.js` 注册 `CalendarRenderer`（`type: calendar`）。原生 calendar 支持 schedules（[{title, start, end, color, className}]）的日/周/月视图，但**不支持「行=员工，列=日期」矩阵范式**。

### 4.2 落地范式 A：原生 calendar（CRM 活动日历）

```yaml
- type: service
  api:
    data: { query: "...findPage(...orderBy:\"activityDate ASC\")..." }
    adaptor: |
      // 按 activityDate 映射 schedules
      const schedules = rows.map(function(a){
        const dateStr = String(a.activityDate).substring(0, 10);
        return { title: '[' + a.activityType + '] ' + ..., start: dateStr, end: dateStr, color: typeColor[a.activityType] };
      });
      return { data: { schedules } };
  body:
    - type: calendar
      schedules: "${schedules}"
      viewMode: "month"
```

### 4.3 落地范式 B：custom 矩阵 table（HR 团队休假日历）

**矩阵范式**：行=员工，列=日期。原生 calendar 不支持，改用 `type: table` + adaptor 后端组装：

```yaml
- type: service
  api:
    data: { query: "...ErpHrLeaveRequest__findPage(filter_status:\"APPROVED\"...)" }
    adaptor: |
      // 1. 解析选中月份 → days[]
      // 2. 提取员工列表 → employees[] + empIdx{}
      // 3. 构建 matrix[empIdx][day] = [{type, label, color}]
      // 4. 生成 columns（员工列 + 每日列，含 weekend className）+ rows（每员工一行的 cells tpl spans）
      // 5. leaveType 颜色：ANNUAL=蓝/SICK=黄/PERSONAL=橙/MARRIAGE=粉/MATERNITY=紫/FUNERAL=灰/COMPENSATORY=青
      return { data: { rows, columns, monthLabel, totalEmployees } };
  body:
    - type: tpl  # 图例
    - type: table
      source: "${rows}"
      columns: "${columns}"
```

**冲突检测可视化**：同日多员工休假 → 单元格多色块叠加（无需独立高亮逻辑，矩阵自然呈现）。

### 4.4 反模式

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 在 custom JSON 组装中硬编码日期（`'2026-07-22'`） | adaptor 按选中月份动态计算 days[] |
| timeline 缺失 scheduledAt 时崩溃 | adaptor null-check + placeholder fallback |
| 用原生 calendar 实现矩阵范式 | 用 custom `type: table`（原生 calendar 不支持行=员工、列=日期） |
| 字段名臆测（scheduledAt） | 实时仓库核实（activityDate/startDate/endDate） |

## 5. 状态变更 mutation 契约

### 5.1 既有 mutation 全覆盖（不需新增 BizModel delta）

| 实体 | 状态切换 mutation | 守卫 |
|------|------------------|------|
| ErpCrmLead | `moveStage(leadId, toStageId)` (`ErpCrmLeadBizModel:84`) | LeadProcessor 守卫 isWonStage 禁止拖出 + 阶段序列 |
| ErpCsTicket | `assign`/`start`/`resolve`/`close`/`reopen`/`cancel` (`ErpCsTicketBizModel:109-242`) | 状态机严格：每个 mutation 校验前置态；close 守卫 SLA 违约需 remark；cancel 拒绝终态 |
| ErpPrjTask | `startTask`/`completeTask`/`blockTask(blockReason)`/`unblockTask` (`ErpPrjTaskBizModel:103-163`) | 状态机严格：blockTask 守卫 `ERR_TASK_BLOCK_REASON_REQUIRED`；startTask 守卫前置任务完成（STRICT/WARN 模式） |

### 5.2 GraphQL 调用契约（row-action 触发）

```yaml
api:
  method: post
  url: /graphql
  dataType: raw
  data:
    query: "mutation(${'$'}id:Long){ ErpPrjTask__startTask(taskId:${'$'}id){ id status } }"
    variables:
      id: "${id}"  # crud 行 scope 的 id 字段
```

**reload 链**：状态变更后 reload 受影响的列 crud（如 startTask 后 reload `colTodo,colInProgress`）。

### 5.3 守卫层级（前端 + 后端双层）

- **前端 visibleOn**：按当前行状态预过滤 row-action 可见性（避免 UX 困惑）
- **后端 mutation 校验**：拒绝非法状态迁移（防绕过），抛 NopException + 域 ErrorCode

## 6. 反模式自检表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 拖拽跨 crud 行（AMIS 不原生支持） | 列式 crud + row-action「移动到状态/阶段」按钮 |
| row-action 直接调 `__update`（绕过状态机） | 调对应状态机 mutation（startTask/resolve/moveStage） |
| 守卫仅前端 visibleOn | 守卫在后端 BizModel 强制（前端仅 UX 预拦截） |
| BLOCKED 列允许无原因拖入 | blockTask mutation dialog 收集 blockReason（required） |
| timeline/calendar 字段名臆测（occurredAt/scheduledAt/operatedAt） | 实时仓库核实实体实际字段（activityDate/createTime/startDate/endDate） |
| 商机看板硬编码 N 列 stage code | 经 service + each 从 ErpCrmStage 动态读取 |
| 自实现 timeline/calendar 组件 | 用原生 `type: timeline` / `type: calendar`（assets 已注册） |
| HR 矩阵日历用原生 calendar（不支持） | 用 custom `type: table`（行=员工，列=日期） |
| 在 custom JSON 组装中硬编码日期 | adaptor 按选中月份动态计算 |
| GraphQL `mutation` 调用绕过状态机（直接 update status） | 经对应状态机 mutation（保留守卫 + 操作日志写入） |
| `type: timeline` 缺 items 时无 placeholder | 配置 `placeholder: "暂无..."` |

## 7. 参考

- 路线图：`docs/backlog/frontend-ui-roadmap.md` §F13（line 318-334 / 544）
- 兄弟范式：`docs/design/page-structure-patterns.md` §3（仪表板）/ §8（F16 复杂页面）
- AMIS 组件资产：`nop-entropy/nop-frontend-support/nop-web-site/target/classes/META-INF/resources/assets/`
  - `Timeline-Brcmgfvk.js`（TimelineRenderer 注册）
  - `Calendar-BRmv1Joe.js`（CalendarRenderer 注册）
  - `vendor-amis-C3Fz2yFP.js.gz`（`U({type:\`timeline\`...})` + `U({type:\`calendar\`...})` 注册）
- 状态机契约：
  - `module-crm/erp-crm-service/.../ErpCrmLeadBizModel.java:84`（moveStage）
  - `module-cs/erp-cs-service/.../ErpCsTicketBizModel.java:109-242`（assign/start/resolve/close/reopen/cancel）
  - `module-projects/erp-prj-service/.../ErpPrjTaskBizModel.java:103-163`（startTask/completeTask/blockTask/unblockTask）
- 实施 plan：`docs/plans/2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md`
- 成功 successor（拖拽 UX 升级）：触发条件 = AMIS 原生拖拽组件可用 或 第三方 AMIS 拖拽扩展引入
