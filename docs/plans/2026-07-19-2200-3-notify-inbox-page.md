# 2026-07-19-2200-3-notify-inbox-page 通知收件箱页面（用户面通知中心）

> Plan Status: active
> Last Reviewed: 2026-07-19
> Source: `docs/backlog/frontend-ui-roadmap.md` §跨域建议 §5（通知收件箱，dependency graph 中归属 Phase 1a 但尚未实施 —— 见 `frontend-ui-roadmap.md:403,428` `subgraph 阶段1a` 含 `Cnotify` 节点）
> Related: `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`（通知派发子系统已完成，后端 notify/markRead/markAllRead/findUnread/countUnread 已就绪）；`docs/plans/2026-07-06-0642-1-operational-notification-consumers.md` + `2026-07-06-0642-2-approval-workflow-notifications.md`（通知消费者与审批通知已落地）；`docs/architecture/notification-strategy.md`（通知策略权威）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-19）：

- **后端完全就绪**：抽样 `module-notify/erp-notify-service/src/main/java/app/erp/notify/service/entity/ErpSysNotificationBizModel.java`（182 行）：
  - `@BizMutation notify(eventType, context)` — 通知派发主入口（line 62-88）
  - `@BizMutation markRead(notificationId)` — 单条标记已读，写入 ErpSysNotificationRead（line 91-108）
  - `@BizMutation markAllRead(userId)` — 批量标记已读，返回处理条数（line 111-127）
  - `@BizQuery findUnread(userId)` — 返回未读列表（line 130-133）
  - `@BizQuery countUnread(userId)` — 返回未读计数（line 136-139）
  - 数据模型：`ErpSysNotification`（通知实例，含 templateId/notificationType/recipientUserId/recipientPartnerId/channel/subject/body/payloadJson/status/sentAt）+ `ErpSysNotificationRead`（已读记录，含 notificationId/userId/readTime）
  - 频控合并机制：`mergeGroupId` + `mergeCount`（line 56 + ORM）
- **前端零定制**：抽样 `module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/pages/ErpSysNotification/ErpSysNotification.view.xml`（20 行）仅含 codegen 默认空 `<grid id="list"/>` + `<form id="view"/>`；`_gen/_ErpSysNotification.view.xml`（174 行）含 codegen 默认 CRUD（add-button + row-update-button + row-delete-button + batch-delete-button）—— **用户面收件箱完全缺失**。
- **菜单已暴露但语义错误**：`module-notify/erp-notify-web/.../auth/_erp-notify.action-auth.xml:8-20` 暴露 `ErpSysNotification-main`（通知实例）+ `ErpSysNotificationRead-main`（已读记录）+ `ErpSysNotificationTemplate-main`（模板）三个菜单 —— 但 `ErpSysNotification-main` 直接调 codegen `main.page.yaml` 显示**全域所有用户的通知**（不过滤 recipientUserId），且暴露 add/update/delete 按钮（用户不应创建/编辑/删除系统通知）。
- **接收人维度已建模**：`ErpSysNotification.recipientUserId`/`recipientPartnerId`/`recipientDeptId` 三字段支持用户/伙伴/部门三种接收人维度（`_gen/_ErpSysNotification.view.xml:30-37` codegen 默认列展示）。当前 BizModel 的 `findUnread/markAllRead` 仅按 `recipientUserId` 过滤，partnerId/deptId 维度的 inbox 派生属 successor（见 Deferred）。
- **未读计数前端无显示**：grep `countUnread` 在 `module-notify/erp-notify-web/` 零命中；全局 header / sidebar 无未读小角标。
- **当前用户身份获取**：Nop 平台 AMIS `${loggedInUser}` 或 GraphQL `ctx.userId` 可获取当前用户 ID；本计划收件箱页面以此过滤 `recipientUserId == $currentUserId`。
- **前置已就绪**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿；E2E 套件（`tests/e2e/playwright.config.ts` webServer 配置）可用。

## Goals

1. **新建用户面通知收件箱页面**：`module-notify/erp-notify-web/.../pages/ErpSysNotification/inbox.page.yaml` —— 经 AMIS 直接配置（不经 codegen view.xml），按当前登录用户 `recipientUserId` 过滤，支持未读/已读/全部 tab 切换 + 类型/渠道/日期多维筛选
2. **覆盖 `ErpSysNotification.view.xml`**：在保留层定制 codegen 默认 main crud —— 移除 add/update/delete 按钮（用户不应编辑系统通知）；保留 row-view-button；新增 row-mark-read-button + list-mark-all-read-button + row-mark-unread-button；新增未读粗体 + 已读灰化样式
3. **未读计数前端显示**：收件箱页面顶部显示「未读 N 条」+ 全局 header 角标（`app-erp-all/.../header.page.yaml` delta 或独立 plan；本计划交付收件箱页内计数，header 角标归 successor）
4. **未读/已读切换 + 批量标记已读**：调用既有 `markRead` / `markAllRead` mutation；操作后实时刷新列表 + 计数（不经全页面 reload）
5. **详情抽屉**：行点击打开 drawer 显示通知完整内容（subject + body + payloadJson 渲染 + sentAt + channel）；drawer 内含「标记已读」按钮（若当前未读）
6. **action-auth 菜单重排**：将 `ErpSysNotification-main`（通知实例）改为 `ErpSysNotification-inbox`（我的通知），user-facing 显示；模板管理 + 已读记录管理保留为管理员菜单（仅在 admin 角色下显示）
7. **Playwright E2E 覆盖**：新建 `tests/e2e/business-actions/notify-inbox.action.spec.ts` 验证 markRead / markAllRead / 未读计数 翻转

## Non-Goals

- **全局 header 未读角标实时刷新**（顶部导航栏小角标 + WebSocket 推送）—— 本计划交付收件箱页内未读计数；header 角标 + 实时推送归独立基础设施 plan（涉及全局 layout delta + WebSocket 接入）
- **邮件/短信/IM 渠道集成**——`channel` 字段已建模但渠道适配器归 dispatch 层（已由 0504-1 plan 覆盖后端）
- **通知偏好设置页**（用户配置「接收哪些类型 + 哪个渠道」）—— successor；当前用户接收由 `recipientUserId` + 模板订阅决定
- **partnerId / deptId 维度 inbox**——本计划仅按 `recipientUserId`（个人）过滤；伙伴/部门通知中心属 successor（业务模式不同：partner inbox 需 partner 用户登录态映射，dept inbox 需 dept-leader 权限校验）
- **通知模板编辑器（admin）**——模板管理已有 codegen 默认页面 + 已由 0504-1 覆盖；本计划不改模板编辑
- **通知中心批量删除 / 归档**——本计划仅交付标记已读/未读；删除/归档属 successor（涉及数据保留策略）
- **WebSocket 实时推送 + 浏览器桌面通知**——基础设施 successor；本计划刷新机制走 AMIS `service` reload（手动/操作后刷新）
- **修改 ORM 模型**（`*.orm.xml`）——保护区域，仅在 view.xml + page.yaml + action-auth.xml + delta 层
- **新增 BizModel 方法**——既有 `markRead/markAllRead/findUnread/countUnread` 已覆盖收件箱所有操作；本计划不改后端
- **国际化 i18n**（`i18n-en:`）——F15 覆盖；本计划使用中文 label
- **F12 tabs 容器**（通知详情走 drawer 非 tab）——本计划用 drawer；tab 容器归 F12
- **审批通知专用渲染**（审批类通知含「去审批」按钮跳转审批工作台）——successor；本计划通用渲染所有通知类型
- **F11 批量操作**（批量删除/批量按类型标记已读）——本计划仅做「全部标记已读」；其他批量操作归 F11

## Task Route

- Type: `implementation-only change`
- Owner Docs:
   - `docs/backlog/frontend-ui-roadmap.md` §跨域建议 §5（通知收件箱）+ §跨域建议 §10（敏感操作确认：批量标记已读前确认）
  - `docs/architecture/notification-strategy.md`（通知策略权威）
  - `docs/design/notify/`（若存在；若不存在本计划落地首个 notify 域设计文档）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md` §6（动作直连业务 API）+ §8（薄 page.yaml wrapper）
  - `../nop-entropy/docs-for-ai/03-runbooks/add-page-business-action.md`（页面业务动作）
  - `../nop-entropy/docs-for-ai/03-runbooks/build-related-drawer-page.md`（关联详情 drawer）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml + page.yaml 定制 + AMIS service reload + drawer）；不新增 BizModel 方法（既有 4 个端点已覆盖），故不加载 `nop-backend-dev`；Phase 4 浏览器 E2E 不加载技能（`nop-testing` 仅覆盖后端 JUnit/IGraphQLEngine，Playwright 浏览器 E2E 不在其范围），范式参考 `tests/e2e/business-actions/_helper.ts` + 既有同型 spec（如 `cs-ticket.action.spec.ts`）。

## Infrastructure And Config Prereqs

- view.xml 路径：`module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/pages/ErpSysNotification/ErpSysNotification.view.xml`
- 新 page.yaml 路径：`module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/pages/ErpSysNotification/inbox.page.yaml`
- action-auth.xml 路径：`module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/auth/_erp-notify.action-auth.xml`
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量（view.xml 修改）+ 直接重启（page.yaml/action-auth.xml）
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置；测试用户身份 + 种子通知数据（既有部署期种子 + 0504-1/0642-1 已覆盖通知种子基础）

## Execution Plan

### Phase 1 — 设计冻结与 view.xml 主 crud 改造

Status: planned
Targets:
- `module-notify/erp-notify-web/.../pages/ErpSysNotification/ErpSysNotification.view.xml`
- `docs/design/notify/inbox-patterns.md`（新建，首个 notify 域设计文档）

Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Fix | Explore`
- Prereqs: none

- [ ] `Explore`: 验证 4 项并记录证据 file:line：
  - (a) **AMIS `${loggedInUser}` / 当前用户身份获取**：核实 Nop 平台 AMIS 模板变量 `${loggedInUser.userId}` / `${$user.userId}` / GraphQL query `ctx.userId` 在 page.yaml 中可用路径；抽样既有 NopAuthUser 视图或 dashboard BizModel（`ErpMdDashboardBizModel` 等）使用范式。
  - (b) **AMIS `service` reload + `crud` filter 联动**：核实 AMIS `crud` 经 `filter` 块 + `service` 包裹可手动 reload（如标记已读后刷新列表）；既有 `dashboards.visual.spec.ts` / `reports.amis-download.spec.ts` 范式。
  - (c) **`@query:ErpSysNotification__findUnread` 直接端点可行性 + 已读 tab 过滤机制**：核实能否在 inbox.page.yaml 直接调用 `__findUnread` 而非 `__findPage`（避免 client-side 过滤）；若可，未读 tab 直接走 `findUnread`、已读 tab 走 `findPage` + filter `id in (readIds)`、全部 tab 走 `findPage`。**额外裁决已读 tab 过滤机制**（候选 A/B/C 见 Phase 2 决策表），明确禁用 `status=READ` 误用。
  - (d) **`ErpAllWebPagesCollectTest` 存在性 + inbox.page.yaml 是否被纳入 page 收集校验**：核实 `ErpAllWebPagesCollectTest`（位于 `app-erp-all` 或 `app-erp-web`）是否扫描 `_vfs/erp/notify/pages/ErpSysNotification/inbox.page.yaml` 经 XDef 校验；若不扫描，本计划 Phase 4 退出标准改用 `xmllint --noout` + 启动后抽样渲染验证。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策 inbox 数据源策略：
  - **方案 A（首选）**：未读 tab → `__findUnread(userId)` 直接端点；已读 tab → `__findPage` + filter `recipientUserId == userId` + sub-query 已读记录；全部 tab → `__findPage` + filter `recipientUserId == userId`。
  - **方案 B**：三 tab 均走 `__findPage` + filter `recipientUserId == userId` + client-side 区分未读/已读（性能较差，已读列表大时性能下降）。
  - 选择 A，若 Explore (c) 裁决 `__findUnread` 不适合 page.yaml 直接接入（如返回非 page 结构）则降级 B
  - Skill: `nop-frontend-dev`
- [ ] `Add | Fix`: 改造 `ErpSysNotification.view.xml`：
  - 在 `<grids>` 内定制 `<grid id="list">` 列集（移除 templateId/recipientUserId/recipientPartnerId/recipientDeptId/payloadJson/errorMsg/delVersion 等管理员字段，保留 id/notificationType/channel/subject/status/sentAt + 新增「已读状态」派生列）
  - 在 `<forms>` 内定制 `<form id="view">` 为详情 drawer 友好布局（基本信息 + 内容 + 元数据分组）
  - 在 `<pages><crud name="main">` 内移除 `<listActions>` 的 `add-button` + `batch-delete-button`；移除 `<rowActions>` 的 `row-update-button` + `row-delete-button`；保留 `row-view-button`（drawer）；新增 `row-mark-read-button` + `row-mark-unread-button`（visibleOn 已读状态）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 在 `docs/design/notify/inbox-patterns.md` 新建文档，固化收件箱范式（数据源策略 + 列集 + 详情 drawer + action 接线 + 已读状态显示机制）。≥100 行。
  - Skill: `none`

Exit Criteria:

- [ ] Phase 1 Explore (a)/(b)/(c) 三项门控证据落地（含 file:line）
- [ ] inbox 数据源策略决策在 plan 记录
- [ ] `ErpSysNotification.view.xml` 改造落地：移除管理员字段 + 移除 add/update/delete 按钮 + 新增 mark-read/mark-unread 按钮
- [ ] `docs/design/notify/inbox-patterns.md` 落地（≥100 行）

### Phase 2 — inbox.page.yaml 新建 + 未读/已读/全部 tab 切换

Status: planned
Targets: `module-notify/erp-notify-web/.../pages/ErpSysNotification/inbox.page.yaml`（新建）
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`: 新建 `inbox.page.yaml` —— 完整 AMIS schema（不经 codegen，直接手写）：
  - 顶部 page header 显示「我的通知」+ 未读计数（经 `__countUnread` initApi 拉取，service 包裹）
  - 顶部 tab 切换：未读 / 已读 / 全部（每 tab 独立 crud + 独立 api）
  - 未读 tab：crud `api: @query:ErpSysNotification__findUnread`（参数 `userId: ${$user.userId}`）；行粗体显示；row-action 「标记已读」+ 「查看详情」
  - 已读 tab：crud `api: @query:ErpSysNotification__findPage` + filter `recipientUserId == ${$user.userId}` + **已读状态过滤机制经 Phase 1 Explore (c) 裁决**（候选：(A) GraphQL sub-query `id notIn (select notificationId from ErpSysNotificationRead where userId=$user)`；(B) 双查询客户端拼接；(C) 后端新增 `findRead` 端点 —— 后端 successor。**禁用 `<filter><eq name="status" value="READ"/></filter>`** —— `status` 字段是通知 lifecycle 状态 SENT/MERGED/FAILED（BizModel line 167），非已读状态）；行灰化显示
  - 全部 tab：crud `api: @query:ErpSysNotification__findPage` + filter `recipientUserId == ${$user.userId}`；已读状态派生列着色（经 join ErpSysNotificationRead 或 client-side 双查）
  - 顶部 listAction「全部标记已读」按钮（调 `__markAllRead(userId)`，confirmText 二次确认，操作后 service reload）
  - 行点击打开 drawer 显示通知详情（subject + body 渲染 + payloadJson JSON viewer + sentAt + channel）；drawer 内「标记已读」按钮（若当前未读）
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 多维筛选 query form：notificationType（下拉）+ channel（下拉）+ sentAt（date-between）+ status（下拉，仅 lifecycle status SENT/MERGED/FAILED 三态，**非已读状态**——已读状态派生自 ErpSysNotificationRead 关联，本筛选不覆盖）
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，登录普通用户 → 打开 inbox 页面 → 三 tab 切换渲染正确 → 未读 tab 行粗体 → 已读 tab 行灰化 → 「全部标记已读」操作后未读计数翻转归零。抽样证据记录到 plan。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `inbox.page.yaml` 文件落地，三 tab 切换渲染正确
- [ ] 未读计数经 `__countUnread` 实时显示，markAllRead 后翻转归零
- [ ] 详情 drawer 显示通知完整内容
- [ ] 本地化：page.yaml 经 `mvn clean install -DskipTests` 重新打包 + page.yaml 经 `xmllint --noout`（如适用）或 AMIS schema 抽样启动验证

### Phase 3 — action-auth 菜单重排（user-facing vs admin）

Status: planned
Targets: `module-notify/erp-notify-web/.../auth/_erp-notify.action-auth.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 2

- [ ] `Fix`: 改造 `_erp-notify.action-auth.xml`：
  - 新增 `ErpSysNotification-inbox` resource（displayName=「我的通知」/「通知中心」，url=`/erp/notify/pages/ErpSysNotification/inbox.page.yaml`，orderNo 前置如 10000，permissions = `ErpSysNotification:query` —— 所有登录用户可访问）
  - 修改既有 `ErpSysNotification-main`（admin 通知实例管理）orderNo 后置（如 10001）+ 增加 admin 角色权限限制（仅 admin 可见，permissions = `ErpSysNotification:query,ErpSysNotification:mutation`）
  - 保留 `ErpSysNotificationTemplate-main`（模板管理）admin-only；保留 `ErpSysNotificationRead-main`（已读记录审计）admin-only
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，普通用户登录 → 菜单仅显示「我的通知」（inbox）→ 不显示通知实例管理 / 模板管理 / 已读记录管理；admin 用户登录 → 菜单显示全部 4 项。抽样证据记录。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `_erp-notify.action-auth.xml` 重排落地：inbox user-facing + admin 菜单 admin-only
- [ ] 普通用户 / admin 用户菜单可见性差异化生效
- [ ] 本地化：action-auth.xml 经 `mvn clean install -DskipTests` 重新打包，启动后菜单差异化生效

### Phase 4 — Playwright E2E 覆盖

Status: planned
Targets: `tests/e2e/business-actions/notify-inbox.action.spec.ts`（新建）
Skill: `none`（Playwright 浏览器 E2E 不在 `nop-testing` 技能覆盖范围 —— 该技能仅覆盖 `JunitAutoTestCase / IGraphQLEngine / 快照录制回放` 后端测试。Playwright 范式参考 `tests/e2e/business-actions/_helper.ts` + 既有同型 spec 如 `cs-ticket.action.spec.ts`）

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [ ] `Add`: 新建 `tests/e2e/business-actions/notify-inbox.action.spec.ts`，覆盖：
  - **测试 1（未读列表 + 计数）**：经 GraphQL `__notify` 派发通知 → 切换 inbox 未读 tab → 断言未读计数 ≥ 1 + 未读 tab 列表含该通知
  - **测试 2（单条标记已读）**：未读 tab 行 markRead → 断言未读计数减 1 + 已读 tab 含该通知
  - **测试 3（全部标记已读）**：未读 tab 「全部标记已读」按钮 → 断言未读计数归零 + 已读 tab 含全部
  - 复用 `tests/e2e/business-actions/_helper.ts` 三原语（`callMutation`/`verifyState`/`findFirst`）；范式参考既有同型 spec（如 `cs-ticket.action.spec.ts`）
  - Skill: `none`
- [ ] `Proof`: `npx playwright test tests/e2e/business-actions/notify-inbox.action.spec.ts` 全绿（3 测试）；不影响其他 spec（0 回归）
  - Skill: `none`

Exit Criteria:

- [ ] `tests/e2e/business-actions/notify-inbox.action.spec.ts` 落地，3+ 测试全绿
- [ ] 全套件回归：`npx playwright test` 0 新增失败

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_084ff71a2ffeIuwOCO6oEY4kim`) — 1 blocker (B1: Source §4 should be §5; "deferred from Phase 1a" misreads dependency graph) + 4 major concerns (M1: Phase 2/3 exit criteria polluted Closure Gates territory; M2: nop-testing skill misfit for Playwright; M3: read-tab `<filter>status=READ</filter>` wrong; M4: Goal 8 process goal).
- Independent draft review iteration 2: **accept** (`ses_084f86c07ffeTp11hJQmOwmMlz`) — all blockers resolved: B1 §4→§5 + accurate "remaining" wording; M1 Phase 2/3 exit criteria localized; M2 Phase 4 Skill: none + reference to _helper.ts; M3 explicit禁用 status=READ + mechanism A/B/C adjudication in Phase 1 Explore (c); M4 Goal 8 removed. One minor Task Route ↔ Phase 4 Skill inconsistency (line 64 nop-testing claim) fixed in same revision pass after iteration 2 caught it. Plan acceptable for active status.

## Closure Gates

> 全部 Phase 完成且退出标准 `[x]` 后关闭。完整仓库验证在此处运行。

- [ ] 范围内行为完成（Phase 1–4 全部 done；inbox 页面 + view.xml 改造 + 菜单重排 + E2E 落地）
- [ ] 相关文档对齐：`docs/design/notify/inbox-patterns.md` 落地；`docs/architecture/notification-strategy.md` 增「前端收件箱」段落；`docs/backlog/frontend-ui-roadmap.md` §跨域建议 §5 通知收件箱行标 completed
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test`（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）+ `npx playwright test tests/e2e/business-actions/notify-inbox.action.spec.ts`（3+ 测试全绿）+ 全套件回归 0 新增失败
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 全局 header 未读角标 + WebSocket 实时推送

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: header 角标需修改全局 layout（`app-erp-all/.../_delta/.../header.page.yaml` 或类似），WebSocket 推送需后端消息总线 + 浏览器 EventSource 接入；均属基础设施 successor，超出「通知收件箱页面」结果面。
- Successor Required: `yes`（触发条件：全局 header / WebSocket 基础设施 plan 启动时）

### 通知偏好设置页（用户配置接收类型 + 渠道）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 偏好设置需新建实体（如 `ErpSysNotificationPreference`）+ ORM 模型变更（保护区域），属不同结果面。
- Successor Required: `yes`（触发条件：通知偏好产品需求落地时）

### partnerId / deptId 维度 inbox

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 伙伴 inbox 需 partner 用户登录态映射（外部伙伴用户系统），部门 inbox 需 dept-leader 权限校验（仅部门负责人可见部门通知）；BizModel 当前 `findUnread/markAllRead` 仅按 `recipientUserId` 过滤，partnerId/deptId 维度需扩展。
- Successor Required: `yes`（触发条件：外部伙伴用户系统 / 部门权限校验产品需求落地时）

### 标记未读（markUnread）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前 BizModel 仅有 markRead 单向路径，无 markUnread；用户场景「读了想再标记未读」属低频，可后置。
- Successor Required: `yes`（触发条件：用户反馈或产品需求落地时）

### 通知批量删除 / 归档 + 数据保留策略

- Classification: `optimization candidate`
- Why Not Blocking Closure: 删除/归档需定义保留期限（如 90 天自动归档）+ 数据保留策略 SOP；当前通知数据量低，无运维压力。
- Successor Required: `yes`（触发条件：通知数据量增长或合规要求落地时）

### 审批通知专用渲染（含「去审批」按钮跳转审批工作台）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 审批类通知（如 0642-2 落地的 ApprovalWorkflowNotifications）含审批上下文（`payloadJson.approvalId`），理想渲染应有「去审批」按钮跳转 xwf 工作台；但 xwf 浏览器层不可达（2026-07-09-2330-1 裁决），跳转目标需重新设计。
- Successor Required: `yes`（触发条件：xwf 审批工作台浏览器层可达性突破时）

### i18n（`i18n-en:` 属性）

- Classification: `optimization candidate`
- Why Not Blocking Closure: F15 覆盖；本计划使用中文 label。
- Successor Required: `yes`（触发条件：F15 i18n plan 启动时）

## Closure

Status Note: 待 Phase 1–4 全部 done 后填写。

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理（新会话，2026-07-XX）>
- Evidence: <待填>

Follow-up:

- 全局 header 未读角标 + WebSocket 实时推送（基础设施 plan）
- 通知偏好设置页（含新 ORM 实体）
- partnerId / deptId 维度 inbox
- markUnread 反向操作
- 通知批量删除 / 归档 + 数据保留策略
- 审批通知专用渲染（依赖 xwf 浏览器层可达性突破）
