# 通知收件箱前端范式（用户面通知中心）

> 本文档固化通知收件箱页面的设计决策、数据源策略、列集、详情 drawer、动作接线与已读状态显示机制。
> 持久化字段定义权威：`module-notify/model/app-erp-notify.orm.xml`（`ErpSysNotification` / `ErpSysNotificationRead`）。
> 后端 BizModel 权威：`module-notify/erp-notify-service/.../ErpSysNotificationBizModel.java`。
> 通知策略权威：`docs/architecture/notification-strategy.md`。
> 落地计划：`docs/plans/2026-07-19-2200-3-notify-inbox-page.md`。

## 设计目标

为每个登录用户提供一个**以自身为接收人**的通知收件箱，支持：

1. 按未读 / 已读 / 全部三态切换浏览
2. 顶部显示未读条数（`countUnread` 实时拉取）
3. 单条标记已读 + 批量「全部标记已读」
4. 行点击打开详情 drawer 查看完整内容（subject + body + payloadJson + sentAt + channel）
5. 多维筛选（notificationType / channel / sentAt 日期段 / lifecycle status）
6. 用户面菜单与 admin 后台菜单分离（用户仅见 inbox，admin 另见实例/模板/已读审计）

## 关键决策

### 1. 当前用户身份获取（userId 来源）

**约束**：Nop AMIS 渲染上下文不暴露 `${loggedInUser.userId}` / `${$user.userId}` 等模板变量；`LoginApi__getLoginUserInfo` 需显式 accessToken 入参（无 token 时 `parseAuthToken(null)` → NPE），而 AMIS 不易直接读取 SPA 在 `localStorage["auth:v2"]` 中持久化的 JWT。

**裁决**：扩展既有 `findUnread` / `countUnread` / `markAllRead` 三个 BizModel 方法为「userId 可选」——`@Optional @Name("userId") String userId`，留空时回退到 `ctx.getUserId()`（与既有 `markRead` 内部「优先 `recipientUserId`，回退 `ctx.getUserId()`」模式一致）。前端 AMIS 调用这些端点时**不传 userId 入参**，由后端从 JWT 鉴权上下文自动解析。

```java
private String resolveUserId(String userId, IServiceContext ctx) {
    if (userId != null && !userId.isEmpty()) return userId;
    return ctx == null ? null : ctx.getUserId();
}
```

**理由**：

- 避免前端跨 SPA/AMIS 边界传 JWT，减少 token 泄漏面。
- 与 `markRead` 既有模式对齐，零行为变更（向后兼容：显式传 userId 时仍按 userId 过滤，供 admin 跨用户场景与单元测试保留）。
- 仅扩展既有方法签名（`@Name` → `@Optional @Name`），不新增 BizModel 方法，不破坏既有 GraphQL 契约（参数仍可显式传）。

### 2. 数据源策略（三 tab 选择）

| Tab | API | 过滤机制 | 说明 |
| --- | --- | --- | --- |
| 未读 | `@query:ErpSysNotification__findUnread` | 后端按 `recipientUserId == userId` + `id NOT IN ErpSysNotificationRead` 过滤 | 直接端点，返回 list，crud 经 adaptor 拍平为 `{items, count}` |
| 已读 | `@query:ErpSysNotification__findRead` | 后端按 `ErpSysNotificationRead.userId == userId` 反查 `ErpSysNotification` | 与 `findUnread` 对称的简洁端点 |
| 全部 | 客户端拼接 `findUnread` + `findRead` | 客户端合并去重 | 不带 filter 的简单合并，避免新加 findInbox 方法 |

**禁用** `<filter><eq name="status" value="READ"/></filter>`：`ErpSysNotification.status` 字段是通知 lifecycle 状态（`SENT` / `MERGED` / `FAILED`，见 `ErpNotifyConstants` 与 BizModel line 167 `unreadOf` 的 filter），不是已读状态。已读状态派生自 `ErpSysNotificationRead` 关联，**不能**复用 lifecycle status。

**降级裁决**：Phase 1 Explore (c) 候选 (A) GraphQL sub-query `id notIn (...)` 因 Nop GraphQL 过滤 sub-query 语法非简单 TreeBean 不直接可用；候选 (B) 双查询客户端拼接性能与一致性差，仅在「全部」tab 用作合并机制（已读/未读两 list 客户端去重，规模可控）；候选 (C) 后端新增 `findRead` 端点——Phase 1 Explore (a) 裁决前端无法直接获取 userId，故必须有「后端 ctx.getUserId() fallback」端点；`findRead` 与既有 `findUnread` 对称、复用 `unreadOf` 同一模式，是最小扩展。**最终**：已读 tab 走 `findRead`，全部 tab 客户端拼接 `findUnread` + `findRead`。

### 3. 列集

未读 / 已读 / 全部三 tab 共用统一列集：

| 列 | 字段 | 说明 |
| --- | --- | --- |
| 类型 | `notificationType` | 业务事件键 |
| 渠道 | `channel` | IN_APP / EMAIL / ... |
| 标题 | `subject` | 模板渲染后 |
| 发送时间 | `sentAt` | sortable |

`body` / `payloadJson` 不在列展示（详情 drawer 内查看）。`recipientUserId` / `recipientPartnerId` / `recipientDeptId` / `templateId` / `errorMsg` / `delVersion` 等管理员字段不在用户面暴露。

### 4. 行操作

- **查看详情**（所有 tab）：`actionType: dialog` 打开 drawer，显示完整通知内容（subject + body + payloadJson JSON viewer + sentAt + channel + notificationType）
- **标记已读**（未读 tab）：`actionType: ajax` 调 `@mutation:ErpSysNotification__markRead?notificationId=$id`，操作后 reload 当前 crud + 顶部未读计数 service
- **标记已读**（drawer 内未读按钮）：同上，drawer 关闭并 reload

### 5. 列表级操作

- **全部标记已读**（所有 tab）：`actionType: ajax` 调 `@mutation:ErpSysNotification__markAllRead`（不传 userId，后端 fallback `ctx.getUserId()`），`confirmText` 二次确认，操作后 reload `unreadCountService` + 三个 tab crud

### 6. 未读计数实时显示

顶部 `service`（`name: unreadCountService`）调 `@query:ErpSysNotification__countUnread`，响应经 adaptor 拍平为 `{ unreadCount: n }`，body 内 `tpl` 渲染 `未读 ${unreadCount} 条`。任何标记已读 / 全部标记已读 / tab 切换后均 reload 此 service，确保计数与列表一致。

### 7. 多维筛选

筛选表单（`type: form` `mode: inline`）字段：

- `notificationType`（input-text）：业务事件键
- `channel`（select）：IN_APP / EMAIL（数据字典可后置）
- `sentAt` 起 / 止（input-date）：发送日期段
- `status`（select）：仅 lifecycle 三态 SENT / MERGED / FAILED（**非已读状态**）

筛选表单的「刷新」按钮 `actionType: reload` `target` 指向三个 crud + 未读计数 service。

### 8. 菜单结构（user-facing vs admin）

`module-notify/erp-notify-web/.../auth/_erp-notify.action-auth.xml` 重排：

| Resource | displayName | orderNo | 可见性 | url |
| --- | --- | --- | --- | --- |
| `ErpSysNotification-inbox` | 我的通知 | 10001 | 所有登录用户（permissions=`ErpSysNotification:query`） | `/erp/notify/pages/ErpSysNotification/inbox.page.yaml` |
| `ErpSysNotification-main` | 通知实例（管理员） | 10010 | admin-only（permissions=`ErpSysNotification:query,ErpSysNotification:mutation`） | `/erp/notify/pages/ErpSysNotification/main.page.yaml` |
| `ErpSysNotificationRead-main` | 已读记录（审计） | 10011 | admin-only | `/erp/notify/pages/ErpSysNotificationRead/main.page.yaml` |
| `ErpSysNotificationTemplate-main` | 通知模板（管理员） | 10012 | admin-only | `/erp/notify/pages/ErpSysNotificationTemplate/main.page.yaml` |

## 文件清单

| 路径 | 角色 |
| --- | --- |
| `module-notify/erp-notify-web/.../pages/ErpSysNotification/ErpSysNotification.view.xml` | admin 后台视图（裁列、移除 add/update/delete 按钮、保留 row-view drawer） |
| `module-notify/erp-notify-web/.../pages/ErpSysNotification/inbox.page.yaml` | 用户面收件箱页面（手写 AMIS，不经 codegen） |
| `module-notify/erp-notify-web/.../auth/_erp-notify.action-auth.xml` | 菜单与权限重排（inbox user-facing + admin 菜单 admin-only） |
| `module-notify/erp-notify-service/.../ErpSysNotificationBizModel.java` | 扩展 findUnread/countUnread/markAllRead userId 可选回退 ctx.getUserId() |
| `tests/e2e/business-actions/notify-inbox.action.spec.ts` | Playwright E2E 覆盖 markRead / markAllRead / 未读计数翻转 |

## 反模式

- ❌ 在 inbox.page.yaml 中调 `@query:ErpSysNotification__findPage` 后用 `<filter><eq name="status" value="READ"/></filter>` 模拟已读 tab —— `status` 是 lifecycle，非已读状态。
- ❌ 在 inbox.page.yaml 中暴露 add / update / delete 按钮 —— 用户不应编辑/创建/删除系统通知。
- ❌ 在 view.xml 中保留 codegen 默认 add-button / batch-delete-button / row-update-button / row-delete-button 给用户面 —— 仅 admin 后台可暴露，且 admin 也无需 add/update（系统通知由 `notify()` 派发产生）。
- ❌ 把 `markAllRead` / `findUnread` / `countUnread` 设计为「必须前端传 userId」—— AMIS 无法直接取当前用户，强制前端传 userId 会迫使人手在 page.yaml 里硬编码或复杂 SPA 桥接。
- ❌ 把 `${loggedInUser.userId}` 写进 page.yaml —— 当前 Nop AMIS 渲染上下文未注入此变量，运行时为空字符串，会损坏 GraphQL 查询。
- ❌ 把 `payloadJson` 原始 JSON 直接渲染到列 —— 内容长且不友好；应放详情 drawer 内经 JSON viewer 友好展示。

## Successor（超出本设计范围）

- 全局 header 未读小角标（需修改全局 layout delta + 可能 WebSocket 推送）
- 通知偏好设置页（需新建 `ErpSysNotificationPreference` 实体 + ORM 模型变更）
- partnerId / deptId 维度 inbox（需 partner 用户登录态映射 / dept-leader 权限校验）
- `markUnread` 反向操作（当前 BizModel 单向 markRead）
- 通知批量删除 / 归档 + 数据保留策略
- 审批类通知专用渲染（含「去审批」跳转按钮，依赖 xwf 浏览器层可达性突破）
- i18n（`i18n-en:` 属性）—— F15 i18n plan 覆盖
