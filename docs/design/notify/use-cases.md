# 通知派发子系统用例（Notify Use Cases）

> 本文是 notify 子系统的**需求契约**（功能验收用例），从"期望行为"出发组织可验证场景。
> 机制/边界/接线权威：[`README.md`](README.md)（定位/边界/核心业务对象/派发链）+ [`inbox-patterns.md`](inbox-patterns.md)（收件箱前端范式）+ `docs/architecture/notification-strategy.md`（通知类型/频控/通道策略权威）。
> 用例编号前缀 `UC-SYS-NN`：对齐 notify appName `erp-notify` / 二级简称 `sys` / 表前缀 `erp_sys_*` / 实体 `ErpSys*`，与 `docs/design/use-case-authoring-guide.md §1` 的 `SYS` 域简称一致。

> **补写纪律声明（Q1 根因守卫）**：本文描述的是**期望行为/需求契约**，来源可追溯到 notify `README.md` + `inbox-patterns.md` + `notification-strategy.md` 三份权威文档，**不是**"当前实现文档化"。MA1 A1.51 切片将另行核验实现是否符合本文（L1 需求契约 vs L3-L5 实现）。本编写过程**未参照运行时代码**（BizModel/Dispatcher/Resolver 实现细节），仅引用上述设计权威文档的契约条款。

## 状态轴速查

```
通知实例 lifecycle (ErpSysNotification.status, 字典 notification-status):
  PENDING  — 待发送（预留，当前同步派发不经过）
  SENT     — 已发送（站内消息落库即此态）
  MERGED   — 已合并（频控命中既有实例）
  FAILED   — 发送失败（预留）

模板状态 (字典 template-status):
  DRAFT → ACTIVE   仅 ACTIVE 模板参与派发

已读状态: 派生自 ErpSysNotificationRead 关联（唯一键 notificationId+userId），非 lifecycle status
```

> **关键约束**：`ErpSysNotification.status` 是通知 **lifecycle**，**非已读状态**。已读状态派生自 `ErpSysNotificationRead` 关联。收件箱页面禁用 `<filter><eq name="status" value="READ"/>`（见 `inbox-patterns.md §2`）。

---

## UC-SYS-01 通知模板生命周期与查找

**场景**：业务事件经 `notify(eventType, context)` 派发时，按业务事件键 `notificationType` 查找 ACTIVE 模板；管理员维护模板的 DRAFT→ACTIVE 生命周期。

**可验证断言**（来源 `README.md §核心业务对象` + `§状态机`）：
```
模板(ErpSysNotificationTemplate) subjectTpl/bodyTpl 为 ${var} 插值模板
按业务事件键 notificationType 查找模板
仅 status=ACTIVE 的模板参与派发（DRAFT 不参与）

业务事件无 ACTIVE 模板时:
  config-gated 静默跳过（WARN），不抛错给调用方

模板状态迁移: DRAFT → ACTIVE
```

**涉及机制**：`README.md §核心业务对象`、`§状态机`、`notification-strategy.md §实现方案`（通知模板）

---

## UC-SYS-02 通知实例端到端派发链

**场景**：业务域调用统一入口 `IErpSysNotificationBiz.notify(eventType, context)`，子系统完成模板渲染→接收人解析→频控合并→站内落库→config-gated 外发的完整派发链，且对调用方是 best-effort。

**可验证断言**（来源 `README.md §定位` + `§子系统结构与派发链` + `§边界`）：
```
统一派发入口: IErpSysNotificationBiz.notify(eventType, context)
派发链顺序（不可乱序）:
  1. 查找 ACTIVE 模板（按 notificationType）
  2. 模板渲染（${var} 插值）
  3. 接收人解析（RecipientResolver）
  4. 频控合并（MergeCoordinator）
  5. 站内消息落库（ErpSysNotification）
  6. config-gated 外发通道派发

站内消息派发即落 ErpSysNotification 表（status=SENT）

子系统对业务方是 best-effort 服务:
  通知派发失败不回滚调用方业务事实
  notify() 内部 try/catch + config-gated 静默跳过
```

**涉及机制**：`README.md §定位`、`§子系统结构与派发链`、`§边界`（best-effort 边界）

---

## UC-SYS-03 接收人多策略解析

**场景**：按模板 `recipientResolver` 类型解析接收人 userId 集合，支持角色/组织/用户列表/合作伙伴四类解析策略。

**可验证断言**（来源 `README.md §接收人解析策略`）：
```
recipientResolver = ROLE:
  角色名 → NopAuthRole → NopAuthUserRole → userId（复用平台 nop-auth）

recipientResolver = ORG:
  deptId → NopAuthUser

recipientResolver = USER_LIST:
  模板配置 userId 列表（静态 + 支持 ${var} 从 context 插值，如提单人 submitterUserId）

recipientResolver = PARTNER:
  partnerId → 用户映射
  （占位：partner→user 映射未建立，WARN 返回空，config-gated）
```

**涉及机制**：`README.md §接收人解析策略`、`notification-strategy.md §实现方案`（接收人配置）

---

## UC-SYS-04 频控窗口合并

**场景**：同一类型通知在时间窗口内合并，避免通知轰炸；命中窗口内既有实例则合并（`mergeCount`+1），否则新建。

**可验证断言**（来源 `README.md §关键组件` + `§配置项` + `notification-strategy.md §频控规则`）：
```
合并组 key = notificationType + "#" + recipientUserId

频控窗口（ErpNotifyConstants）:
  业务提醒: 5 分钟（300s）— 同一用户、同一类型合并为一条
  异常告警: 1 分钟（60s）— 同一错误类型合并，包含发生次数
  系统通知: 不合并 — 每条独立发送

命中窗口内既有实例:
  合并（mergeCount += 1），实例 status = MERGED
未命中:
  新建实例（status = SENT）

总开关: erp-notify.merge-enabled（默认 true）
```

**涉及机制**：`README.md §关键组件`（NotificationMergeCoordinator）、`§配置项`、`notification-strategy.md §频控规则`

---

## UC-SYS-05 已读状态管理

**场景**：用户标记通知已读（单条/全部），系统维护已读记录防重复；已读状态独立于通知 lifecycle status。

**可验证断言**（来源 `README.md §核心业务对象` + `§关键组件`）：
```
markRead(notificationId):
  写 ErpSysNotificationRead 记录
markAllRead:
  批量标记当前用户全部未读为已读

唯一键 (notificationId, userId) 防重复标记已读

已读状态派生自 ErpSysNotificationRead 关联:
  ErpSysNotificationRead 存在该 (notificationId, userId) 行 → 已读
  不存在 → 未读
  （非 ErpSysNotification.status lifecycle 字段）

countUnread:
  实时统计当前用户未读数
```

**涉及机制**：`README.md §核心业务对象`（ErpSysNotificationRead）、`§关键组件`（markRead/markAllRead/countUnread）、`inbox-patterns.md §6`

---

## UC-SYS-06 用户面收件箱后端查询

**场景**：用户面收件箱按未读/已读/全部三态切换浏览，后端需在 AMIS 无法直接获取当前 userId 的约束下提供"userId 可选回退 ctx"的查询端点。

**可验证断言**（来源 `inbox-patterns.md §1` + `§2`）：
```
findUnread / countUnread / markAllRead 三方法 userId 参数扩展为 @Optional:
  留空时回退 ctx.getUserId()（前端不传 userId，后端从 JWT 鉴权上下文解析）
  显式传 userId 时仍按 userId 过滤（向后兼容 admin 跨用户场景与单元测试）

未读 tab (findUnread):
  recipientUserId == userId 且 id NOT IN ErpSysNotificationRead
已读 tab (findRead):
  ErpSysNotificationRead.userId == userId 反查 ErpSysNotification
全部 tab:
  客户端拼接 findUnread + findRead 去重

禁用 <filter><eq name="status" value="READ"/>:
  ErpSysNotification.status 是通知 lifecycle（SENT/MERGED/FAILED），非已读状态
```

**涉及机制**：`inbox-patterns.md §1`（userId 来源裁决）、`§2`（数据源策略）、`README.md §关键组件`

---

## UC-SYS-07 外发通道 config-gated 降级

**场景**：站内消息为默认通道；邮件/短信外发经平台 `nop-integration` SPI 派发，受 config-gated 控制，无供应商/未启用时静默跳过不阻断业务。

**可验证断言**（来源 `README.md §通道与失败语义` + `§配置项` + `notification-strategy.md §实现方案`）：
```
站内消息 (IN_APP):
  默认通道，派发即落 ErpSysNotification 表

邮件 (EMAIL):
  经 nop-integration 的 IEmailSender.sendEmail(EmailMessage) SPI 派发
短信 (SMS):
  经 nop-integration 的 ISmsSender.sendMessage(SmsMessage) SPI 派发
  （不新建平行通道适配层，与平台默认路线一致）

config-gated 配置项（命名空间 erp-notify.*）:
  erp-notify.enabled        默认 true   通知总开关
  erp-notify.email-enabled  默认 false  邮件通道启用
  erp-notify.sms-enabled    默认 false  短信通道启用

无供应商 / 通道未启用时:
  WARN 跳过，不阻断业务事实
  （不抛出给调用方；各业务消费者额外在调用外包 try/catch warn-and-continue）

失败降级:
  notify() 整体 try/catch
  模板缺失/渲染失败/接收人解析失败/落库失败 均不抛出给调用方（返回空列表）
```

**涉及机制**：`README.md §通道与失败语义`、`§配置项`、`notification-strategy.md §实现方案`

---

## 用例与测试的衔接

- 模板生命周期（UC-SYS-01）→ 验证 DRAFT 不参与派发 + 无 ACTIVE 模板 config-gated 跳过
- 派发链（UC-SYS-02）→ 验证 notify() 完整链路 + best-effort 不回滚调用方
- 接收人解析（UC-SYS-03）→ 验证四类 resolver 策略（ROLE/ORG/USER_LIST/PARTNER 占位）
- 频控合并（UC-SYS-04）→ 验证窗口内合并 mergeCount+1 vs 新建 + 三类通知窗口差异
- 已读状态（UC-SYS-05）→ 验证唯一键防重 + countUnread 翻转 + status 非 read
- 收件箱后端（UC-SYS-06）→ 验证 userId 可选回退 ctx + 三态切换 + 禁用 status=READ filter
- 外发通道（UC-SYS-07）→ 验证 config-gated 默认 false + 无供应商 WARN 跳过不阻断 + SPI 复用 nop-integration

## 参考机制文档

- `README.md` — 定位/边界/核心业务对象/派发链/状态机/配置项
- `inbox-patterns.md` — 收件箱前端范式（userId 来源/数据源策略/列集/动作）
- `docs/architecture/notification-strategy.md` — 通知类型/频控规则/通道策略/消费者接线清单（权威）
