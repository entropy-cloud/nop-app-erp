# 2026-08-03-1232-2-flux-f13-nonstandard-views-rewrite F13 非标准视图 Flux 原生重写（降级消除）

> Plan Status: draft
> Last Reviewed: 2026-08-03
> Source: 用户决策（2026-08-03）——界面全面转向 nop-chaos-flux；`docs/design/flux-complex-pages.md` §3.3/§4.3-4.4（看板/日历/时间线映射）
> Related: `2026-08-03-1232-1`（CRUD 基础设施，前置）、`2026-08-03-1232-3`（F16）、`2026-08-03-1232-5`（文档漂移回填）
> Audit: required

## Current Baseline

- **F13 现状（AMIS 实现 + 组件级降级，`2026-08-03-1000` 深度分析确认）**：
  - 看板 3：`prj/ErpPrjTask/kanban.page.yaml`（252 行，4 列）、`cs/ErpCsTicket/kanban.page.yaml`（317 行，6 列 + SLA 🔴 + YAML anchor）、`crm/ErpCrmLead/opportunity-kanban.page.yaml`（动态列 stage 循环）——**拖拽降级为列式 crud + row-action 按钮**
  - 时间线 2：`crm/ErpCrmActivity/timeline.page.yaml`（83 行，**原生 timeline prop 绑定失败 → each+tpl 降级**）、`cs/ErpCsTicketAction/timeline.page.yaml`（同降级，文件头注释「同 crm activity-timeline」）
  - 日历 2：`crm/ErpCrmActivity/calendar.page.yaml`（103 行，**原生 calendar React #130 → 按日分组卡片网格降级**）、`hr/ErpHrLeaveRequest/team-vacation-calendar.page.yaml`（矩阵 table）
  - hr 组织架构图 `hr/dashboard/org-chart.page.yaml`（145 行，AMIS tree → each+tpl 缩进降级）——**归本计划或 P3？本计划纳入**（tree 是 flux 原生强项，与 F13 同批处理）
- **Flux 原生能力（`flux-guide/design-patterns/` 实测）**：
  - `kanban`：data 扁平图结构（root/column/card/divider 节点）、draggable/columnDraggable/wipStrict、onCardMove/onColumnReorder/onCardClick、filterText/filterCard/filterTags、columnWidth
  - `timeline`：items（time/title/detail/icon/level）、mode（left/right/alternate）、orientation（horizontal/vertical）、reverse
  - `calendar`：view（month/week/day）、events（id/title/start/end/status/color）、resources（嵌套+open）、firstDayOfWeek/showWeekends/locale、onEventChange/onEventCreate/onEventClick、loadAction
  - `tree`：data/labelField/keyField/childrenKey/searchable/initiallyExpanded；`tree-select`：options/childrenSource 懒加载
- **后端 mutation 现状（实测）**：
  - cs 看板：`ErpCsTicketBizModel` 四态 mutation 前置态严格——`assign`(←NEW, 需 assignedToId)、`start`(←ASSIGNED)、`resolve`(←IN_PROGRESS)、`close`(←RESOLVED, 且 SLA 违约需 remark)；另有 reopen；**CANCELLED 列无 mutation**。`ErpCsTicket/kanban.page.yaml:86,140,192,242` 引用 assign/start/resolve/close
  - crm 商机：`ErpCrmLead__moveStage`（`ErpCrmLeadBizModel.java:105-108`，LeadProcessor 守卫）——任意态迁移有守卫
  - prj 任务：`ErpPrjTaskBizModel.java:102-163` 仅 `startTask/completeTask/blockTask/unblockTask`（**严格相邻态守卫** TODO→IN_PROGRESS→DONE/BLOCKED），**无 updateStage/moveStage**；`non-standard-views-patterns.md` §2.1 当初降级正式理由 = 「状态机仅允许相邻态迁移，任意态拖拽会被后端 mutation 拒绝」——**flux kanban 无列级拖拽限制（draggable 为全局 boolean），自由拖拽与相邻态守卫冲突必须裁决**
  - crm 活动：`ErpCrmActivityBizModel` 19 行裸 CrudBizModel（无自定义 mutation/query）；实体仅 `activityDate`（DATE、mandatory，orm.xml:549），**无 start/end datetime、无 status 字段**——flux calendar 事件契约要求 ISO start/end，时间粒度拖拽无法映射 DATE-only 字段
  - hr 休假：`ErpHrLeaveRequest` mandatory 字段 employeeId/leaveType/status/businessDate/code——拖拽创建通道需配套表单收集
- **数据契约适配**（`flux-complex-pages.md` §4.3-4.4）：看板需后端聚合返回扁平图结构（列定义 + 卡片）+ **列→mutation 路由映射**；日历需日期窗口查询（新增）
- **E2E**：F13 已有 visual spec 7 用例 + action spec 3 用例（AMIS 引擎）；`E2E_ENGINE=flux` 可切换
- **范式文档**：`docs/design/non-standard-views-patterns.md` §2-§4 描述 AMIS 降级实现（含 3 处文档漂移待回填——属 P5）

## Goals

- 7 个 F13 页面（3 kanban + 2 timeline + 2 calendar）+ 1 个 org-chart 全部以 flux 原生组件重写，**消除拖拽/原生渲染降级**
- 看板拖拽持久化：**列→mutation 路由机制定义**（cs 四态 + crm moveStage + prj 相邻态受限拖拽的裁决），无效拖拽 UX（非相邻态被拒后的回退提示）与参数收集（assignedToId/remark）
- 日历事件持久化：按实体实际数据模型裁决（crm DATE-only 字段的拖拽语义映射或受限；创建通道的 mandatory 字段收集表单）
- 时间线以原生 timeline 渲染（items 数据契约），替换 each+tpl
- org-chart 以 flux tree 渲染（可折叠/展开/搜索）
- 复用既有后端 mutation；新增仅限只读聚合查询（看板图结构/日历日期窗口）
- E2E：**重写现有 F13 visual spec 的 AMIS 选择器为 flux 契约**（现有 7 用例直接使用 `.cxd-Crud/.cxd-Service/.cxd-Table` 原始选择器，绕过 engine 抽象，flux 渲染后必然失败）；action spec（裸 GraphQL）引擎无关无需改

## Non-Goals

- 标准 CRUD 迁移 → P1（前置）
- F16 特殊页（甘特/向导/B 族其余页）→ P3
- 占位页/未实现项 → P4
- 范式文档漂移回填（§2-§4 更新为 flux 实现）→ P5
- 甘特图（aps）→ P3

## Task Route

- Type: `implementation-only change`（页面重写；后端仅新增只读聚合查询）
- Owner Docs: `docs/design/non-standard-views-patterns.md`（F13 范式）、`docs/design/flux-complex-pages.md` §3.3/§4.3-4.4、`docs/design/crm/ui-patterns.md`、`docs/design/customer-service/ui-patterns.md`、`docs/design/human-resource/ui-patterns.md`、`docs/design/projects/ui-patterns.md`
- Skill Selection Basis: `nop-frontend-dev`（页面重写）+ `nop-backend-dev`（若新增聚合查询 BizModel）

## Infrastructure And Config Prereqs

- 前置：P1 Phase 0-1（render-mode=flux 基础设施 + 标准 CRUD 验证）完成，`nop.web.render-mode=flux` 可运行
- 后端新增聚合查询（可选，仅查询无模型变更）：`findBoardData`（看板）、`findByDateRange`（日历）——若复用既有 @BizQuery 可省略

## Execution Plan

### Phase 0 - 拖拽持久化契约裁决（前置 Decision）

Status: planned
Targets: 无代码（裁决）
Skill: `nop-frontend-dev`

- Item Types: `Decision`（拖拽语义裁决）
- Prereqs: P1 完成（菜单翻转 + 标准 CRUD 基线）

- [ ] Decision: prj 看板拖拽语义——相邻态受限拖拽（前端预校验，非相邻拖拽拒绝 + 回退提示）vs 新增统一 mutation（违反「零后端改动」需明确范围）。依据：`ErpPrjTaskBizModel` 仅相邻态 mutation + `non-standard-views-patterns.md` §2.1 降级理由。记录理由与替代方案、残留风险
      - Skill: `nop-frontend-dev`
- [ ] Decision: cs 看板列→mutation 路由——onCardMove 单事件如何在 4 mutation 间按目标列路由（列定义携带 mutation 标识）；无效拖拽（非相邻）被拒后的 UX；drop 到 ASSIGNED 列的 assignedToId 收集；SLA 违约 close 的 remark 收集；CANCELLED 列无 mutation 的列归属（列禁用 or 新增 mutation）。记录理由
      - Skill: `nop-frontend-dev`
- [ ] Decision: crm 日历数据模型适配——`ErpCrmActivity` 仅 activityDate（DATE）无 start/end datetime：方案 A 拖拽仅改日期（映射到 activityDate，时间粒度禁用或按天对齐）/ 方案 B ORM 扩展（ask-first 保护区域，禁止）/ 方案 C 日历仅只读展示 + 弹窗编辑日期。记录理由
      - Skill: `nop-frontend-dev`
- [ ] Decision: onCardMove/onEventChange 事件 payload 契约（flux-types 无 DragEnd payload 文档）——以实测 flux 事件定义为准记录字段映射
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 4 个 Decision 已记录理由与替代方案，拖拽持久化语义明确（无「flux 自由拖拽 vs 后端相邻态守卫」悬而未决）
- [ ] 裁决结果反馈到 Phase 1/2 实现项（每页拖拽行为有定义）

### Phase 1 - 看板重写（3 页）

Status: planned
Targets: `module-{prj,cs,crm}-*/.../kanban*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`（拖拽降级消除）+ `Add`（flux schema）+ `Proof`
- Prereqs: Phase 0 裁决；P1 完成

- [ ] 数据适配：后端聚合查询返回扁平图结构 `{ [id]: { id, type, children, data:{title, cardLimit, tags, deadlineDateTime, isSlaCompleted} } }`；列定义来源——cs 字典值 / crm `ErpCrmStage` 实体查询（非字典）；按 Phase 0 裁决的拖拽语义实现
      - Skill: `nop-backend-dev`
- [ ] cs 看板重写：`{ type: "kanban", data, draggable, wipStrict, columnWidth, onCardMove → 列路由 mutation }`；SLA 红点经卡片节点 data（deadlineDateTime/isSlaCompleted）渲染；保留 YAML anchor 复用模式
      - Skill: `nop-frontend-dev`
- [ ] crm 商机看板重写：动态列（`ErpCrmStage` 查询）+ `onCardMove → ErpCrmLead__moveStage`
      - Skill: `nop-frontend-dev`
- [ ] prj 任务看板重写：4 列 + 按 Phase 0 裁决的拖拽语义（相邻态预校验 or 统一 mutation）
      - Skill: `nop-frontend-dev`
- [ ] Proof: 每看板拖拽 E2E（flux 引擎）——拖拽跨列 → mutation 调用断言 + 无效拖拽回退断言 + 数据刷新断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 3 个看板以 flux kanban 渲染（非列式 crud），拖拽按裁决语义可用且持久化到后端
- [ ] 无效拖拽（非相邻）回退 UX 有 E2E 断言
- [ ] 动态列（crm）与 SLA 标记（cs）行为等价于 AMIS 版本
- [ ] 拖拽 E2E 证据（flux 引擎）通过

### Phase 1 - 时间线重写（2 页）

Status: planned
Targets: `module-{crm,cs}-*/.../timeline.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`（原生渲染降级消除）+ `Add`
- Prereqs: P1 完成

- [ ] crm 活动时间线：`{ type: "timeline", items: [{time,title,detail,level}], mode, orientation }`，数据经 data-source 取 `ErpCrmActivity` 活动历史
      - Skill: `nop-frontend-dev`
- [ ] cs 操作时间线：同模式（cs 工单操作历史）
      - Skill: `nop-frontend-dev`
- [ ] Proof: 时间线渲染 E2E（flux 引擎）——items 渲染断言 + 时间/详情字段正确
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 2 个时间线以 flux 原生 timeline 渲染（非 each+tpl）
- [ ] 时间线 E2E 证据通过

### Phase 2 - 日历重写（2 页）

Status: planned
Targets: `module-{crm,hr}-*/.../calendar.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`（React #130 降级消除）+ `Add` + `Proof`
- Prereqs: Phase 0 裁决（crm 日历数据模型适配）；P1 完成

- [ ] 数据适配（按 Phase 0 Decision）：`{ events: [{id,title,start,end,status,color}], resources }`；crm 活动日期窗口查询（新增只读查询，DATE 对齐到天）
      - Skill: `nop-backend-dev`
- [ ] crm 活动日历：`{ type: "calendar", view, events, loadAction, onEventChange → 按裁决（改日期 or 受限）, onEventCreate → 创建通道（leadId/activityType/activityDate mandatory 收集表单） }`；firstDayOfWeek=1/locale=zh-CN
      - Skill: `nop-frontend-dev`
- [ ] hr 团队休假日历：resources 按员工分组 + 创建通道（employeeId/leaveType/status/businessDate/code mandatory 收集表单）
      - Skill: `nop-frontend-dev`
- [ ] Proof: 日历 E2E（flux 引擎）——视图切换/事件渲染/拖拽（按裁决）断言 + 创建通道表单断言
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 2 个日历以 flux 原生 calendar 渲染（非卡片网格/矩阵 table）
- [ ] 事件交互按 Phase 0 裁决语义实现，创建通道 mandatory 字段收集有 E2E 断言

### Phase 3 - org-chart 与收口

Status: planned
Targets: `module-hr/.../dashboard/org-chart.page.yaml`、`tests/e2e/visual/f13-non-standard-views.visual.spec.ts`
Skill: `nop-frontend-dev`

- Item Types: `Fix`（缩进降级消除）+ `Fix`（E2E 选择器重写）+ `Proof`
- Prereqs: Phase 2

- [ ] hr 组织架构图：flux `tree`（data 绑定 ErpHrDepartment 树 + labelField/keyField）+ 搜索
      - Skill: `nop-frontend-dev`
- [ ] **重写 F13 visual spec**：`f13-non-standard-views.visual.spec.ts` 的 `.cxd-Crud/.cxd-Service/.cxd-Table` 原始 AMIS 选择器改为 flux 契约选择器（按 `flux-guide/13-testing.md` selector 速查）
      - Skill: `nop-testing`
- [ ] Proof: org-chart 渲染 E2E（flux 引擎）——树层级展开/折叠断言；全部 8 页 F13 spec（flux 引擎）全绿
      - Skill: `nop-testing`
- [ ] Follow-up: F13 各页在 `non-standard-views-patterns.md` 的 §2-§4 更新为 flux 实现（交付 P5 的输入，不在此实施）
      - Skill: none

Exit Criteria:

- [ ] org-chart 以 flux tree 渲染（非 each+tpl 缩进）
- [ ] 8 页（3+2+2+1）全部 flux 原生，无降级残留
- [ ] F13 visual spec 重写完成，E2E（flux 引擎）全绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a16bc0affeYwiSYAAhId3Fnw) — prj mutation 不存在(B1)、cs 列→mutation 路由(B2)、crm 日历 DATE-only(B3)、visual spec 需重写(M1)、onCardMove payload 契约(N1)、SLA 字段(N2)、hr mandatory 收集(N3)、crm 列来源(N4)；已全部修订
- Independent draft review iteration 2: accept (ses_039fd49b2ffeyGb4WF5jcNdc4s) — 4 Decisions 与实测一致（prj 相邻态/cs 路由+cancelled 列/crm DATE-only/payload 无文档）、updateStage 引用已删、Baseline 重写精确、visual spec 重写项落定；非阻塞注记（close 无 remark 参数需先 update 再 close 的链路、moveStage 行号差 1、org-chart 需新增第 8 个 spec 用例）留实施期细化

## Closure Gates

- [ ] 范围内行为完成（8 页 flux 原生重写 + 拖拽/原生渲染降级全部消除）
- [ ] 相关文档对齐（范式文档 §2-§4 的 flux 更新交付 P5）
- [ ] 已运行验证（`E2E_ENGINE=flux npx playwright test` F13 相关 + 后端聚合查询单测）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 看板拖拽冲突检测（并发编辑）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 现有 mutation 由后端守卫（LeadProcessor 等）保证一致性，前端乐观更新即可；冲突提示属增强
- Successor Required: `no`

### SLA 超时实时刷新

- Classification: `optimization candidate`
- Why Not Blocking Closure: 看板 data-source 轮询（interval）可覆盖，非阻塞
- Successor Required: `no`

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
