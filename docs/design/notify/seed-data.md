# notify 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2c（plan `docs/plans/2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md`，2026-09-02）。此前 1 张 notify 表 seed（notification_template 27 行聚合，plan `2026-08-25-0330-1`）见 `docs/architecture/seed-data.md` 历史批次段。
> **业务语义 owner docs**：`docs/design/notify/`（README.md / inbox-patterns.md / use-cases.md）+ `docs/architecture/notification-strategy.md`；本文件只登记种子数据面，不重复业务语义。

## 种子数据范围（M1.2c 批次 2 表）

notify 域 3 规格实体中，notification_template 已由历史批次 seed；本批补齐其余 **2 规格表（3 行）**，达成 notify 域全量 seed 覆盖（3 / 3，跨域派发三件套：template〔已seed〕+ notification + read）。

### 2 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpSysNotification（通知实例） | `erp_sys_notification.csv` | 2 | P(SENT)+N-TERM(FAILED) | —（必填列仅 id/notificationType/channel/status）；TEMPLATE_ID 可选→ErpSysNotificationTemplate〔已seed 7101/7102〕，notificationType 与模板 1:1 匹配 |
| ErpSysNotificationRead（通知已读记录） | `erp_sys_notification_read.csv` | 1 | P（挂 notification 7301） | NOTIFICATION_ID→ErpSysNotification〔本批 7301〕；USER_ID=`demo-user`（与通知行 recipientUserId 一致，满足 `UK_SYS_NOTIFY_READ_NOTIF_USER`） |

## 行语义

| ID | 行 | 关键字段 | 语义 |
|---|---|---|---|
| 7301 | P | TEMPLATE_ID=7101（cs.sla-overdue）、recipient=demo-user、CHANNEL=IN_APP、STATUS=SENT、SENT_AT=2026-06-30T09:00:00、MERGE_COUNT=1 | 模板 7101 `${var}` 模式的忠实具体渲染快照；站内信落库即 SENT（`docs/design/notify/README.md` lifecycle 语义） |
| 7302 | N-TERM | TEMPLATE_ID=7102（fin.posting-exception）、recipient=demo-user、STATUS=FAILED、SENT_AT 空、ERROR_MSG 填充 | 发送失败终态行（正文为自由渲染文本，引用 `V-DEMO-001` 为演示占位单号，非既有单据软引用）。**注意**：`FAILED` 为当前同步派发路径不产生的预留态（owner doc 登记口径），此行为演示终态行、非运行时产物 |
| 7303 | P（read） | NOTIFICATION_ID=7301、USER_ID=demo-user、READ_TIME=2026-06-30T10:00:00 | 已读状态派生自本表存在性（非 lifecycle status，见 inbox-patterns.md 反模式）；时序 10:00 > 09:00 自洽 |

## FK 闭环图

```
[已seed·notify] erp_sys_notification_template(7101 cs.sla-overdue / 7102 fin.posting-exception，0330-1 聚合 27 行)
   ──TEMPLATE_ID（可选 FK）──▶ erp_sys_notification(7301/7302)
[本批] erp_sys_notification(7301) ──NOTIFICATION_ID──▶ erp_sys_notification_read(7303)
```

零悬空：7301/7302/7303 与模板 71xx 段及运行时派发段（`zz-sequence-advance.sql` 起 100000）三段零冲突；已读行仅挂种子通知 id，与运行时派发行（≥ 100000）零交集。

## 干扰面零漂移设计（本批核验结论）

- **recipient=`demo-user` 隔离**：`demo-user` 是**展示用非登录接收人**（非 `nop_auth_user` seed 用户；`recipientUserId` 为 VARCHAR 弱语义无 ORM to-one，引用完整性门禁与查询均不受影响；虚构用户字符串有 `_cases` 运行时快照 `autotest-ref` 等先例）。E2E `notify-inbox.action.spec` 全部查询/动作键于登录用户 `nop`（`findUnread(userId:"nop")` 按 `recipientUserId=userId` 过滤；`findRead` 按已读记录 `userId=nop` 反查）→ 种子 3 行与 E2E 查询集零交集；spec 断言全部为相对/下界（自建唯一 eventType + `Date.now()` 后缀，`countUnread >= 1`，`afterN == beforeN - 1`），清理仅删自建行。
- **FAILED 行对未读面结构不可见**：`unreadOf` 过滤 `status IN (SENT, MERGED)`（`ErpSysNotificationBizModel`），7302 天然不入任何未读统计。
- **收件箱展示面**：`/erp/notify/pages/ErpSysNotification/inbox.page.yaml` 三 tab 按当前登录用户过滤，demo-user 行不出现在任何登录用户的收件箱（admin 后台 `ErpSysNotification-main` findPage 可见——预期演示语义）。
- 快照机制 `_chgType` 增量记录：`app-erp-all/_cases` 中 `erp_sys_notification*.csv` 仅为运行时派发行（id ≥ 100000）增量快照，种子纯加性行不入既有快照（M1.2b 实证先例）。

## 用例指示编码与 negative 行语义

- **P（最小正例行）**：notification 7301（SENT）+ read 7303。
- **N-TERM（终态行）**：notification 7302 `FAILED`（发送失败终态，供 lifecycle 守卫负路径与失败展示语义）。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列；时间戳 ISO-T（`2026-06-30T09:00:00`，对齐 `erp_fin_posting_exception.csv` 先例）；小写布尔不涉及；ID < 100000（notify 沿模板 7xxx 段取 7301+）；字典码 ∈ `erp-notify/notification-channel`（IN_APP）+ `erp-notify/notification-status`（SENT/FAILED）。
- 静态固定日期（冻结时钟纪律，禁 `now()`/滚动期间语义——静态 2026-06-30 与 300s/60s 合并窗口零交互）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 218→237）。
