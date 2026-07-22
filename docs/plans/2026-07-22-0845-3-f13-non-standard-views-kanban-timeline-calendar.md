# 2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar F13 非标准视图模式（看板/时间线/日历）

> Plan Status: active
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F13（line 318-334 / 544）+ `docs/design/page-structure-patterns.md` §1 不适用段（拖拽式看板视图、甘特图、日历 → F13 / F16）
> Related: `docs/plans/2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md`（F12 Tier D successor —— 本计划 7 页面均为独立 `page.yaml` 视图自带 `findPage` 数据源，**不消费 Plan 1 的 form tabs**，无硬依赖；可并行推进）；`docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md`（F16 P1 低风险批，与本计划正交）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-22，对 7 个 F13 目标页面的现状 + AMIS 组件库支持度 + Nop view.xdef schema + `docs/design/page-structure-patterns.md` §1 不适用段 + roadmap §F13 line 318-334 + roadmap §测试策略 F13 行 line 404）：

### F13 目标页面盘点（7 页面，全部 todo）

| # | 域 | 页面 | 类型 | 现状 | 后端就绪度 |
|---|---|------|------|------|-----------|
| 1 | crm | 商机看板（Lead-as-Opportunity Kanban） | 看板+拖拽 | ❌ 无：ErpCrmLead 标准列表 crud（**注：CRM 域不建模独立 ErpCrmOpportunity 实体**，商机经 `ErpCrmLead` + `leadType=OPPORTUNITY` 判别字段建模，见 `module-crm/model/app-erp-crm.orm.xml:12,191`；`stageId` FK 字段属 ErpCrmLead，引用 ErpCrmStage 实体——非字典） | ✅ ErpCrmLead 既有 + leadType 字段 + stageId 字段（FK → ErpCrmStage 实体）；ErpCrmStage 实体提供动态列 |
| 2 | cs | 工单看板（Ticket Kanban） | 看板+拖拽 | ❌ 无：ErpCsTicket 标准列表 crud | ✅ ErpCsTicket 既有 + status 字段；SLA 超时需前端组装（既有 slaDueAt） |
| 3 | projects | 任务看板（Task Kanban） | 看板+拖拽 | ❌ 无：ErpPrjTask 标准列表 crud | ✅ ErpPrjTask 既有 + status 字段（TODO/IN_PROGRESS/DONE/BLOCKED）+ priority + blockedReason |
| 4 | crm | 活动时间线（Activity Timeline） | 时间线 | ❌ 无 | ✅ ErpCrmActivity 既有 + activityType + occurredAt |
| 5 | cs | 活动日志（Action Log Timeline） | 时间线 | ❌ 无 | ✅ ErpCsTicketAction 既有 + fromStatus/toStatus + operatedAt + operator |
| 6 | crm | 活动日历（Activity Calendar） | 日历 | ❌ 无 | ✅ ErpCrmActivity 既有 + scheduledAt |
| 7 | hr | 团队休假日历（Team Vacation Calendar） | 日历矩阵 | ❌ 无 | ✅ ErpHrLeaveRequest 既有（**注：实体名 ErpHrLeaveRequest，非 ErpHrLeave**；hr 域 ORM 仅定义 ErpHrLeaveRequest + ErpHrLeaveBalance）+ employeeId + leaveType + startDate/endDate |

### AMIS 组件库支持度（需 Phase 0 PoC 核实）

| AMIS 组件 | 用途 | Nop 集成就绪度 | 风险 |
|-----------|------|--------------|------|
| `crud` + custom render | 看板列（每列一个 status filter） | ✅ 已知（标准 crud） | 低 |
| 拖拽（HTML5 drag-drop / AMIS `dragTo`） | 看板卡片拖拽切换 stage/status | ⚠️ 未确认：AMIS 是否原生支持跨 crud 拖拽，需 PoC | 高 |
| `timeline` | 纵向时间线 | ⚠️ 未确认：AMIS 是否有原生 timeline 组件，或需 custom JSON 组装 | 中 |
| `calendar` | 日历视图（日/周/月） | ⚠️ 未确认：AMIS 是否有原生 calendar 组件 | 中 |
| 矩阵日历（行=员工，列=日期） | HR 团队休假 | ⚠️ 未确认：需 custom table 组装 | 中 |

### 关键风险/缺口

- **AMIS 拖拽组件支持度**：roadmap §F13 line 326-328 明确要求「拖拽阶段切换（isWonStage 列禁止拖出，丢失列只读）」「拖拽变更状态」「拖拽调整」。AMIS 原生是否支持跨 crud 行拖拽需 Phase 0 PoC；若不支持，降级为 row-action「移动到阶段」按钮（非拖拽，但保留状态切换语义）
- **AMIS timeline / calendar 原生组件**：Nop AMIS 库（`nop-entropy/nop-web/.../amis/`）是否包含 timeline/calendar 组件需 Phase 0 PoC；若不包含，需 custom JSON 组装（AMIS tpl + 数组 map）或引入第三方 AMIS 扩展
- **Playwright 拖拽测试**：roadmap §测试策略 F13 line 404 明确「❌ 无自动测试策略；拖拽交互无法用标准 E2E 覆盖；建议：视觉效果用截图对比，拖拽用 Playwright `dragTo`（需 PoC）」。需 Phase 0 验证 Playwright `dragTo` 在 Nop AMIS 渲染下的可用性
- **状态变更后端 mutation**：拖拽切换 stage/status 需后端 `@BizMutation` 支持（如 `ErpCrmLead__moveStage(leadId, targetStageId)` 针对 leadType=OPPORTUNITY 子集）。需核实既有 mutation 是否覆盖，若不覆盖需新增（轻量 BizModel delta，本计划范围内，见 Task Route）

## Goals

1. **Phase 0 Explore 闭环**：(a) AMIS 拖拽组件 PoC（看板跨列拖拽）；(b) AMIS timeline/calendar 组件支持度 PoC；(c) Playwright `dragTo` 测试可用性 PoC；(d) 状态变更后端 mutation 就绪度核实
2. **7 F13 页面落地**：3 看板（CRM 商机 + CS 工单 + Project 任务）+ 2 时间线（CRM 活动 + CS 活动日志）+ 2 日历（CRM 活动 + HR 团队休假）
3. **范式文档新建**：`docs/design/non-standard-views-patterns.md`（**NEW**）记录看板/时间线/日历 3 类范式 + 拖拽降级策略 + custom JSON 组装模式
4. **回归测试**：visual spec（截图对比）+ action spec（状态变更断言，若 Playwright `dragTo` PoC 通过则覆盖拖拽路径）

## Non-Goals

- **甘特图**（aps 排产）—— 属 F16 高风险 territory（拖拽 + 缩放 + 颜色编码 + 约束叠加），需独立 PoC；归 `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md` §Deferred 高风险 successor
- **F12 page-structure / F16 复杂手写页面**—— 属 Plan 1 / Plan 2 范畴
- **新增独立 React/Vue 自定义组件**—— 优先 AMIS 原生组件 + custom JSON 组装；若 PoC 裁决 AMIS 完全不支持某视图类型，降级为「该页面 defer 到 AMIS 升级或引入第三方扩展」并记录到 Deferred
- **修改 ORM 模型**（保护区域）—— 所有目标实体字段已存在
- **拖拽持久化的事务边界/并发控制**—— 状态变更经既有 `@BizMutation`（事务包装由平台处理）；本计划不引入新事务范式
- **跨域报表级聚合**（如全公司看板视图）—— 本计划聚焦单域列表页的非标准视图；跨域聚合归 dashboard successor

## Task Route

- Type: `implementation-only change`（+ 可能的轻量 BizModel delta：若 Explore (d) 裁决既有 mutation 不覆盖状态切换，新增 `moveStage`/`changeStatus` `@BizMutation` 到对应域 BizModel；该决策在 Phase 0 (d) 后落地，属本计划范围内，非 ORM 变更）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F13（line 318-334 / 544）
  - `docs/design/crm/ui-patterns.md` §商机 + §活动
  - `docs/design/cs/ui-patterns.md` §工单
  - `docs/design/projects/ui-patterns.md` §任务
  - `docs/design/human-resource/ui-patterns.md` §休假
  - `docs/design/page-structure-patterns.md` §1 不适用段（本计划新建独立范式文档）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`（AMIS DSL 模式目录）
  - `../nop-entropy/docs-for-ai/04-reference/`（AMIS 组件库参考）
- Skill Selection Basis: 加载 `nop-frontend-dev`（page.yaml + AMIS 组件 + custom JSON 组装 + bounded-merge）；条件加载 `nop-backend-dev`（仅若 Explore (d) 裁决需新增 moveStage/changeStatus mutation —— 本计划范围内轻量 BizModel delta，非 ORM 变更）；不加载 `nop-testing`（既有 visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **Explore 阶段需可本地运行的 AMIS 页面**用于实测拖拽 + timeline/calendar 渲染
- **Playwright `dragTo` PoC 需本地运行 app**（base_url=http://127.0.0.1:8080）
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：4 PoC + Decision

Status: planned
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`
- Prereqs: F4 P3 crm/cs 域子表基线 + 既有 ErpCrmLead（含 leadType=OPPORTUNITY 商机判别）/ErpCsTicket/ErpPrjTask/ErpCrmActivity/ErpCsTicketAction/ErpHrLeaveRequest 实体

- [ ] `Explore` (a)：AMIS 拖拽组件 PoC（看板跨列拖拽）。
  - PoC 目标：以 ErpPrjTask（4 状态 TODO/IN_PROGRESS/DONE/BLOCKED，最简）为试点，构建 page.yaml 含 4 列 crud（每列 `filter_status=XXX`）+ 卡片 custom render（id/title/priority/blockedReason）+ AMIS `dragTo` 或 HTML5 drag-drop 跨列迁移
  - 验证：(i) AMIS 是否原生支持跨 crud 拖拽；(ii) 若不支持，HTML5 drag-drop 在 AMIS 渲染下是否可用；(iii) 拖拽触发 `doAction(update, {status: targetStatus})` 调用 `__batchUpdate` mutation
  - 降级方案：若拖拽完全不可用，改为 row-action「移动到状态」按钮（dialog 选目标状态 → 触发 batchUpdate）—— 保留状态切换语义但丢失拖拽 UX
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (b)：AMIS timeline/calendar 组件支持度 PoC。
  - PoC 目标：grep `nop-entropy/nop-web/.../amis/` + `nop-entropy/docs-for-ai/` 查找 timeline/calendar 原生组件；若无，custom JSON 组装 PoC（AMIS tpl + 数组 map 渲染纵向时间线 + 月矩阵）
  - 验证：(i) 原生组件存在 → 直接用；(ii) 不存在 → custom JSON 组装可行性 + 渲染效果
  - 降级方案：若 custom JSON 组装渲染效果差，该页面 defer 到「AMIS 升级或引入第三方扩展」并记录到 Deferred
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (c)：Playwright `dragTo` 测试可用性 PoC。
  - PoC 目标：在 Phase 0 (a) 拖拽 PoC 页面上，Playwright `page.dragTo(sourceSelector, targetSelector)` 验证拖拽动作可被测试捕获 + 状态变更可断言
  - 验证：(i) dragTo 是否触发 AMIS 拖拽事件；(ii) 状态变更后 DOM/crud 数据可断言
  - 降级方案：若 dragTo 不可用，改为截图对比（visual spec only）+ row-action「移动到状态」按钮的 action spec（非拖拽路径）
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (d)：状态变更后端 mutation 就绪度核实。
  - 核实范围：ErpCrmLead（既有商机状态切换 mutation？stageId 字段切换；leadType=OPPORTUNITY 子集）、ErpCsTicket（changeStatus 既有？）、ErpPrjTask（changeStatus 既有？）
  - 每实体报告：(i) 既有 `@BizMutation` 是否覆盖状态切换 + (ii) 若不覆盖，BizModel delta 新增轻量 mutation 的可行性（本计划范围内，见 Task Route）+ (iii) 状态机守卫（如 isWonStage 禁止拖出、BLOCKED 必填 blockedReason）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`：基于 Explore (a)+(b)+(c)+(d) 结果，确定 7 页面实现方式。
  - **3 看板**：若 Explore (a) PoC 通过 → 拖拽 + custom render；否则降级 row-action「移动到状态」按钮 + 列式 crud 布局（视觉接近看板但无拖拽）。状态变更经 Explore (d) 裁决的 mutation 路径
  - **2 时间线**：若 Explore (b) 原生 timeline 存在 → 直接用；否则 custom JSON 组装（AMIS tpl + 数组 map）
  - **2 日历**：若 Explore (b) 原生 calendar 存在 → 直接用；否则 custom JSON 组装（矩阵 table）
  - **测试策略**：若 Explore (c) PoC 通过 → 拖拽路径 action spec；否则截图对比 visual spec + 状态变更 action spec（经 row-action 路径）
  - 残留风险：(i) 拖拽完全不可用 → 看板降级为列式 crud（视觉接近但 UX 退化），3 看板统一降级；(ii) custom JSON 组装的 timeline/calendar 渲染效果可能不如原生组件，需 PoC 后业务方确认
  - Skill: none

Exit Criteria:

- [ ] 4 Explore 结论已记录；对应 Decision 已落地
- [ ] 7 页面实现方式明确（含降级路径）

### Phase 1 — 3 看板页面落地

Status: planned
Targets: `module-{crm,cs,projects}/erp-{crm,cs,prj}-web/.../pages/{Entity}/kanban.page.yaml`（**NEW** 独立页面）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（3/3 items tagged Add）
- Prereqs: Phase 0 Explore (a)+(d) 完成

- [ ] `Add`：CRM 商机看板（基于 ErpCrmLead leadType=OPPORTUNITY 子集）
  - 实现：`module-crm/erp-crm-web/.../pages/ErpCrmLead/opportunity-kanban.page.yaml`（**NEW**）含 N 列 crud（每列 `filter_stageId=XXX&filter_leadType=OPPORTUNITY`，列从 ErpCrmStage 实体动态读取）+ 卡片 custom render（leadName/amount/customerName/expectedCloseDate）+ 拖拽（若 PoC 通过）或 row-action「移动到阶段」（降级）。isWonStage 列禁止拖出（visibleOn 守卫或后端 mutation 拒绝）；丢失列只读（LOST stage 列 crud `readOnly=true`）
  - 状态变更：经 Explore (d) 裁决的 mutation（既有或新增 BizModel delta `moveStage`）
  - 菜单接入：`erp-crm.action-auth.xml` 新增 `crm-opportunity-kanban` 菜单项
  - Skill: `nop-frontend-dev`
- [ ] `Add`：CS 工单看板
  - 实现：`module-cs/erp-cs-web/.../pages/ErpCsTicket/kanban.page.yaml`（**NEW**）含 N 列 crud（按 status 字典动态列）+ SLA 超时卡片 🔴 标记（`visibleOn: slaDueAt < now()`）+ 待分派列闪烁高亮（CSS class 或 AMIS className）+ 拖拽变更状态
  - 菜单接入：`erp-cs.action-auth.xml` 新增 `cs-ticket-kanban` 菜单项
  - Skill: `nop-frontend-dev`
- [ ] `Add`：Project 任务看板
  - 实现：`module-projects/erp-prj-web/.../pages/ErpPrjTask/kanban.page.yaml`（**NEW**，基于 Phase 0 (a) PoC）含 4 列（TODO/IN_PROGRESS/DONE/BLOCKED）+ 优先级颜色（priority 字段色块）+ BLOCKED 卡片必填阻塞原因（拖入 BLOCKED 列时 dialog 收集 blockedReason）+ 拖拽
  - 菜单接入：`erp-prj.action-auth.xml` 新增 `prj-task-kanban` 菜单项
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 3 看板页面落地 + 菜单可达
- [ ] 状态变更经 mutation 路径生效（拖拽或 row-action）
- [ ] isWonStage/LOST/BLOCKED 守卫生效（按域）

### Phase 2 — 2 时间线页面落地

Status: planned
Targets: `module-{crm,cs}/erp-{crm,cs}-web/.../pages/{Entity}/timeline.page.yaml`（**NEW**）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (b) 完成

- [ ] `Add`：CRM 活动时间线
  - 实现：`module-crm/erp-crm-web/.../pages/ErpCrmActivity/timeline.page.yaml`（**NEW**）含纵向时间线（时间倒序）+ 类型图标（activityType 字典映射 icon）+ 时间 + 标题 + 摘要。按 Explore (b) Decision 用原生 timeline 或 custom JSON 组装
  - 数据源：`ErpCrmActivity__findPage?orderBy=occurredAt DESC` + `gql:selection` 含 activityType/title/description/occurredAt
  - 菜单接入：`erp-crm.action-auth.xml` 新增 `crm-activity-timeline` 菜单项
  - Skill: `nop-frontend-dev`
- [ ] `Add`：CS 活动日志时间线
  - 实现：`module-cs/erp-cs-web/.../pages/ErpCsTicketAction/timeline.page.yaml`（**NEW**）含纵向操作时间线（操作人 + 时间 + from→to 状态变迁，箭头渲染）+ filter_ticketId 筛选特定工单的操作历史
  - 数据源：`ErpCsTicketAction__findPage?filter_ticketId=$id&orderBy=operatedAt DESC`
  - 菜单接入：`erp-cs.action-auth.xml` 新增 `cs-action-log` 菜单项（或作为 ErpCsTicket 详情 drawer 的子页，不独立菜单）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 2 时间线页面落地 + 菜单/drawer 可达
- [ ] 时间倒序 + 类型图标 + 状态变迁箭头渲染正确

### Phase 3 — 2 日历页面落地

Status: planned
Targets: `module-{crm,hr}/erp-{crm,hr}-web/.../pages/{Entity}/calendar.page.yaml`（**NEW**）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (b) 完成

- [ ] `Add`：CRM 活动日历
  - 实现：`module-crm/erp-crm-web/.../pages/ErpCrmActivity/calendar.page.yaml`（**NEW**）含日/周/月视图 + 活动数量标记（某天 N 条活动显示 badge）+ 点击日期创建/编辑活动浮层。按 Explore (b) Decision 用原生 calendar 或 custom JSON 组装
  - 数据源：`ErpCrmActivity__findPage?filter_scheduledAt_between=$monthStart,$monthEnd`
  - 菜单接入：`erp-crm.action-auth.xml` 新增 `crm-activity-calendar` 菜单项
  - Skill: `nop-frontend-dev`
- [ ] `Add`：HR 团队休假日历
  - 实现：`module-hr/erp-hr-web/.../pages/ErpHrLeaveRequest/team-vacation-calendar.page.yaml`（**NEW**）含月矩阵（行=员工，列=日期）+ 休假类型颜色编码块（leaveType 字典颜色：年假=蓝/病假=黄/事假=橙等）+ 冲突检测（同日多员工同类型休假高亮）
  - 数据源：`ErpHrLeaveRequest__findPage?filter_startDate_between=$monthStart,$monthEnd` + `gql:selection` 含 employeeId/leaveType/startDate/endDate（**注：实体名 ErpHrLeaveRequest，非 ErpHrLeave**）
  - 菜单接入：`erp-hr.action-auth.xml` 新增 `hr-team-vacation-calendar` 菜单项
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 2 日历页面落地 + 菜单可达
- [ ] 日/周/月切换（CRM）+ 月矩阵 + 颜色编码 + 冲突检测（HR）生效

### Phase 4 — 范式文档新建 + 回归测试

Status: planned
Targets: `docs/design/non-standard-views-patterns.md`（**NEW**）+ `tests/e2e/visual/` + `tests/e2e/business-actions/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1-3 完成

- [ ] `Add`：范式文档新建 `docs/design/non-standard-views-patterns.md`
  - 落地：7 节完整文档（§1 目的与范围 / §2 看板范式含拖拽 + 降级策略 / §3 时间线范式含原生 vs custom 组装 / §4 日历范式含日/周/月 + 矩阵 / §5 状态变更 mutation 契约 / §6 反模式自检表 / §7 参考）
  - §2 详述 AMIS 拖拽 PoC 结论 + 降级 row-action「移动到状态」按钮范式 + isWonStage/LOST/BLOCKED 守卫实现
  - §6 反模式自检表：不在状态机守卫缺失下允许拖拽 / 不在 custom JSON 组装中硬编码日期 / 不在时间线缺失 scheduledAt 时崩溃
  - Skill: none
- [ ] `Proof`：visual spec + action spec
  - 落地：`tests/e2e/visual/f13-non-standard-views.visual.spec.ts`（**NEW**）覆盖 7 页面渲染（每页面 1 用例：DOM 结构断言 + 核心视觉元素）；若 Explore (c) PoC 通过，`tests/e2e/business-actions/f13-kanban-drag.action.spec.ts`（**NEW**）覆盖拖拽路径（3 看板各 1 用例：拖拽前状态 X → 拖拽后状态 Y 断言）；若 PoC 不通过，action spec 覆盖 row-action「移动到状态」路径
  - 验证：全 PASS（base_url=http://127.0.0.1:8080, SKIP_WEBSERVER=1）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 范式文档 §1-§7 落地
- [ ] visual spec + action spec 通过

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_078addb82ffe) — 2 blockers + 1 major + 1 minor：
  1. **BLOCKER**：`ErpCrmOpportunity` 实体不存在（CRM 以 ErpCrmLead + leadType=OPPORTUNITY 建模）
  2. **BLOCKER**：`ErpHrLeave` 实体不存在（hr 域 ORM 仅 ErpHrLeaveRequest + ErpHrLeaveBalance）
  3. **MAJOR**：Backend mutation scope 矛盾（Non-Goals 排除 BizModel 变更但 Deferred 声明 mutations in-scope）
  4. **MINOR**：Plan 1 依赖过度陈述（本计划 7 页面均为独立 page.yaml，不消费 Plan 1 form tabs）
- Independent draft review iteration 2: needs revision (ses_078a7e40affe) — 0 blockers，1 major + 2 minors：
  1. **MAJOR**：line 40 残留 `ErpCrmOpportunity__moveStage` 引用
  2. **MINOR**：Deferred 条目自相矛盾（in-scope 项放在 Deferred 段）
  3. **MINOR**：stage 字段名实际为 `stageId`（FK → ErpCrmStage 实体，非字典）
- Independent draft review iteration 3: accept (ses_078a45db3ffe) — 0 blockers, 0 majors, 1 trivial minor（line 106 "stage 字段切换" → "stageId 字段切换"，已在收敛后修正）。Entity audit 全绿：ErpCrmOpportunity 仅在显式反引用注释中出现；ErpHrLeave 仅在反引用注释中；filter_stageId 一致；ErpCrmStage 一致称实体。

## Closure Gates

- [ ] 范围内行为完成（Phase 0-4 全部 `[x]`）
- [ ] 相关文档对齐（`non-standard-views-patterns.md` 新建 + 各域 ui-patterns.md 非标准视图章节实施记录）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/visual/f13-non-standard-views.visual.spec.ts` + 若 PoC 通过 `tests/e2e/business-actions/f13-kanban-drag.action.spec.ts` 全 PASS + 既有核心域 E2E 无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（若 Explore 裁决某页面完全不可实现，移出范围并记录到 §Deferred But Adjudicated，不属此条）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 拖拽完全不可用降级（若 Explore (a) PoC 不通过）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 AMIS 完全不支持跨 crud 拖拽且 HTML5 drag-drop 在 AMIS 渲染下不可用，3 看板统一降级为列式 crud + row-action「移动到状态」按钮（视觉接近看板但无拖拽 UX，保留状态切换语义）
- Successor Required: `yes`（触发条件：AMIS 升级引入原生拖拽组件 或 引入第三方 AMIS 拖拽扩展）

### timeline/calendar 原生组件缺失降级（若 Explore (b) PoC 不通过）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 AMIS 无原生 timeline/calendar 组件且 custom JSON 组装渲染效果差（业务方不接受），相关页面 defer 到「AMIS 升级或引入第三方扩展」
- Successor Required: `yes`（触发条件：AMIS 升级引入原生 timeline/calendar 组件 或 业务方接受 custom 组装效果）

### Playwright dragTo 测试不可用降级（若 Explore (c) PoC 不通过）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 若 Playwright dragTo 在 AMIS 渲染下不可用，拖拽路径改为截图对比 visual spec + row-action action spec（非拖拽路径）；拖拽路径的自动化测试覆盖率降低但不阻断功能落地
- Successor Required: `yes`（触发条件：Playwright 引入 AMIS 拖拽测试支持 或 手动测试覆盖拖拽路径）

### 状态变更后端 mutation 新增（若 Explore (d) 裁决需新增）

> **注**：此项**不属 Deferred**——若 Explore (d) 裁决既有 mutation 不覆盖状态切换，BizModel delta 新增轻量 mutation（如 `ErpCrmLeadBizModel.moveStage`）**属本计划范围**（Task Route 已声明 + Skill 条件加载 `nop-backend-dev`）。Closure Gates 验证含该 mutation 的单元测试。仅当 mutation 复杂度升级（如需状态机重构）才移出范围到独立后端 successor。

### 甘特图（aps 排产）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 甘特图属 F16 高风险 territory（拖拽 + 缩放 + 颜色编码 + 约束叠加），需独立 PoC；本计划聚焦 F13 非标准列表视图（看板/时间线/日历）
- Successor Required: `yes`（触发条件：F16 高风险 successor plan 启动）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待执行后填写独立结束审计证据>

Follow-up:

- 拖拽 UX 升级 successor（若降级）—— 触发：AMIS 原生拖拽组件可用
- timeline/calendar 原生组件升级 successor（若降级）—— 触发：AMIS 升级或第三方扩展
- Playwright 拖拽测试覆盖升级 successor（若降级）—— 触发：Playwright AMIS 拖拽测试支持
- 甘特图 successor —— 触发：F16 高风险 plan 启动
