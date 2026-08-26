# ck-notify — notify（跨域通知派发子系统）实现代码检查报告

> 工作项：C8.1（notify 分册）。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-notify` 手写生产代码——erp-notify-service 11 文件（`NotificationDispatcher`（模板渲染/频控合并/站内构造/外发通道）、`NotificationMergeCoordinator`、`NotificationRecipientResolver`（ROLE/ORG/USER_LIST/PARTNER 四策略）、`ErpSysNotificationBizModel`（薄委派：notify/markRead/markAllRead/findUnread/findRead/countUnread）+ 3 个 per-mutation Processor（Notify/MarkRead/MarkAllRead + Abstract 基类）、`NoopEmailSender`/`NoopSmsSender`、Configs/Constants/Errors）+ 3 个 CRUD BizModel（2 个 stub）+ dao 层。跨文件核实：`module-notify/model/app-erp-notify.orm.xml`（3 实体 versionProp + UK_SYS_NOTIFY_TPL_TYPE/UK_SYS_NOTIFY_READ_NOTIF_USER）、`app-service.beans.xml`（8 bean）、`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`（25 模板种子 vs 代码事件常量对账）、消费域调用面（logistics/aps 的 NOTIFY_EVENT 常量 + wf/cs/fin/sal/crm/mfg/hr 接线清单自 README）。owner docs：`docs/design/notify/`（README/inbox-patterns/use-cases）+ `docs/architecture/notification-strategy.md`。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。erp-notify-service 全部 11 文件通读（无抽样）+ orm/beans 接线 + seed 对账（25 种子 vs 全仓 NOTIFY_EVENT 事件名 grep）+ arm-index notify 条目（P2-RC-081、A1.51/A4.2 切片）现状复核。
> 切片边界：`erp-notify-web` 收件箱页面仅按 inbox-patterns.md 抽查口径（status≠已读约束的后端遵守面）；`erp-notify-api` 骨架与 `_gen/` 不查；wf listener 侧（.xwf）不深查（属各业务域报告范畴）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-notify-001（D7/D2）外发通道在调用方事务内、通知持久化之前派发——EMAIL/SMS 不可逆副作用先于 commit 与 saveEntity 执行，与 README 自述「txn().afterCommit」不符

- **控制点 A**：`app/erp/notify/service/dispatch/NotificationDispatcher.java#dispatch`（L74-89：`for (String userId : recipients) { notifications.add(mergeOrPersist(...)); }` → `for (ErpSysNotification n : notifications) { dispatchExternalChannels(n, channels); }`——**外发循环在 dispatch 内完成**，此时新构造实例 `n.getId()==null`（尚未落库））
- **控制点 B**：`app/erp/notify/service/processor/ErpSysNotificationNotifyProcessor.java#notify`（L37-46：`List<ErpSysNotification> result = dispatcher.dispatch(template, context);` **之后**才 `for (...) { if (n.getId() == null) dao.saveEntity(n); else dao.updateEntity(n); }`——持久化晚于外发）+ grep 实证 `afterCommit` 全模块 main 代码**零命中**
- **证据**：调用链：业务域 @BizMutation（如 wf listener/finance REQUIRES_NEW 异常记录器）→ `notify()`（@BizMutation 加入调用方事务）→ dispatch → `sendEmailIfPossible/sendSmsIfPossible` **真实外呼** → 返回后才 saveEntity → 调用方事务**提交或回滚**。两个缺口：① 调用方事务在外发之后回滚（后续业务步骤异常）→ **邮件/短信已发出但通知行与业务事实一并回滚**——用户收到一条从未发生的通知（不可逆副作用泄漏）；② saveEntity/后续步骤失败（被 notify 外层 catch 吞掉）→ 外发已发生而站内无记录。owner doc README Successor 节逐字「当前同步派发 + **`txn().afterCommit`** 满足单实例部署」——实现与自述不符（无任何 afterCommit 挂钩）。当前部署缓解：EMAIL/SMS 默认 disabled（Noop 实现零副作用）——通道一旦启用缺陷即活化。
- **问题**：D7 事务边界（外部副作用未挂 afterCommit）+ D2 与 owner doc 声明漂移。
- **建议修复方向**：外发通道迁移到 `txn().afterCommit`（平台 ITransactionTemplate）或落 PENDING + job 异步外发（与 Successor 的 nop-message 演进对齐的最小形态）；至少将 dispatchExternalChannels 移到 NotifyProcessor 持久化之后。
- **arm-index 裁决**：新增（grep「afterCommit 通知/外发 事务」arm-index 零命中；P2-RC-081 是总开关维度）。

### P1-CK-notify-002（D4/D2）模板种子与已接线事件类型系统性失同步——8+ 个已派发事件无 ACTIVE 模板且无任何对账机制，静默跳过设计使漂移不可见（cs 先例已实际发生）

- **控制点 A**：`module-notify/deploy/sql/mysql/_seed_erp-notify.sql`（种子 25 个 notificationType 实证）对照全仓代码事件常量：**缺失** `log.gateway-dead-letter`、`log.freight-posting-failure`（logistics G4 告警，ck-logistics P1-CK-log-002）、`aps.workorder-no-routing`、`aps.operation-workcenter-missing`、`aps.dispatch-material-shortage`（ck-aps P2-CK-aps-006）、`crm.sequence-overdue`、`hr.contract-expiry`、`cs.entitlement-expiry`（后三个为 crm/hr/cs 域报告范畴的既有缺口——cs.entitlement-expiry 即任务点名的先例）；oracle/postgresql 两份 seed 同型核对一致；repo 全仓 `*.sql` grep（排除 target）上述事件名零命中
- **控制点 B**：`ErpSysNotificationNotifyProcessor#notify`（L36-38：`template == null → LOG.warn("无 ACTIVE 模板，config-gated 静默跳过") → return emptyList`）——静默跳过本身是 owner doc 裁决的设计（README「业务事件无 ACTIVE 模板时 config-gated 静默跳过（WARN）」），**但全子系统无对账机制**：无 job/查询/启动校验核对「代码常量事件集 ⊆ 模板表 ACTIVE 集」，种子漂移只能靠人工发现
- **证据**：任务点名「模板缺失时静默跳过——notify 侧是 fail-closed 还是静默」定论：**静默（WARN 日志）**，by design；但设计的前提（模板完备）无守护——本次对账实证 8+ 事件失同步，其中 logistics 两条是「唯一告警闭环入口」（影响定级见各域分册）。wf.* 9 个、fin/mfg/sal/cs 大部分已 seed（25 个在册）——失同步是渐进发生的（RC-R1.37 落地时同步 seed 了 log.draft-escalation，其后新增告警链均未回补）。
- **问题**：D4 接线对账缺失（设计裁决的静默跳过 + 无覆盖校验 = 漂移系统性不可见）。
- **建议修复方向**：① 批量补 seed（8+ 行，三库同步）；② 增加对账查询/job（扫代码常量或维护清单 vs 模板表，缺口派发管理员通知或启动 WARN 聚合）；③ 修复阶段裁决是否为「关键告警类事件」提供 config-gated fail-loud（如 log.gateway-dead-letter 无模板时升级 ERROR）。
- **arm-index 裁决**：新增（grep「模板种子/覆盖对账」arm-index 零命中；cs.entitlement-expiry 先例在 cs 域报告登记为该域 finding，本条登记 notify 侧系统性对账缺口 + logistics/aps 6 个新站点）。

### P2-CK-notify-003（D6）频控合并不覆盖外发通道——merge 命中后仍对同一实例再次发送邮件/短信，每次合并增量外发一次

- **控制点**：`NotificationDispatcher#dispatch`（L74-88：`mergeOrPersist` 命中窗口返回**既有**实例（mergeInto 仅改内存）→ `for (ErpSysNotification n : notifications) { dispatchExternalChannels(n, channels); }` **无条件对合并结果再次外发**）
- **证据**：owner doc README 派发链图「频控合并（MergeCoordinator）→ 站内消息落库 → 外发通道」+ notification-strategy 频控语义（业务提醒 5 分钟窗合并为一条）——合并的目的是**减少打扰**；现实现对合并组内每次新事件：站内 mergeCount+1（正确）+ **EMAIL/SMS 再发一封内容几乎相同的消息**（违背频控目的；启用 email-enabled 后用户在 5 分钟窗内收 N 封）。mergeInto 追加的 body 标记 `[合并 +n]` 使重复邮件内容也错位。
- **问题**：D6 频控语义仅覆盖站内通道。
- **建议修复方向**：merge 命中路径跳过 dispatchExternalChannels（或仅当 mergeCount 达阈值/窗口关闭时发汇总）；与 001 的 afterCommit 迁移联动实施。
- **arm-index 裁决**：新增（grep「合并 外发/merge email 重复」零命中）。

### P2-CK-notify-004（D5/D10）markRead 输入边界——不存在通知也写孤儿已读行 + 无接收人身份校验（任意登录用户可按 id 标记他人通知已读）

- **控制点 A**：`app/erp/notify/service/processor/ErpSysNotificationMarkReadProcessor#markRead`（L15-28：`ErpSysNotification n = dao.getEntityById(notificationId); String userId = n != null ? n.getRecipientUserId() : null; if (userId == null) { userId = ctx.getUserId(); } if (!isRead(...)) { readDao.saveEntity(newReadEntry(notificationId, userId)); } return n;`——**n==null 不拒绝**：以当前用户为 userId 给**不存在的 notificationId** 写入 ErpSysNotificationRead 孤儿行（UK (notificationId,userId) 不拦截不存在 id）并返回 null）
- **控制点 B**：同方法 L17-20——userId 取**通知的 recipientUserId 而非调用者**：任意登录用户持任意 notificationId 即可替接收人标记已读（markAllRead 经 unreadOf 按 ctx 用户过滤无此问题；findUnread/countUnread 的 userId 参数亦为 @Optional 裸透传——任意用户可查他人收件箱，同族边界）
- **证据**：inbox-patterns.md 收件箱范式按登录用户视角设计；markRead 的「以接收人为准」注释只对**通知归属判定**有意义（回退 ctx 用户），但未校验 `ctx.getUserId().equals(n.getRecipientUserId())`（管理员代读场景未裁决）。孤儿已读行污染 notIn 过滤集（微小）+ 返回 null 的 GraphQL 契约含糊。
- **问题**：D5 权限/入参边界（越权标记 + 幽灵行）。
- **建议修复方向**：n==null 抛 ERR（新增 NOT_FOUND 码或复用平台 UnknownEntityException）；校验调用者 ∈ {recipientUserId, 管理员}；findUnread/countUnread 的 userId 参数补权限裁决（owner doc 裁决后实施）。
- **arm-index 裁决**：新增（arm-index 的 markRead 命中为 A1.51 切片摘要文本，非该维度 finding）。

### P2-CK-notify-005（D5）EMAIL/SMS 通道无收件地址载体——EmailMessage/SmsMessage 只设 subject/text，收件人解析只产出 userId，启用外发后邮件无法投递

- **控制点**：`NotificationDispatcher#sendEmailIfPossible`（L156-171：`EmailMessage mail = new EmailMessage(); mail.setSubject(...); mail.setText(...); emailSender.sendEmail(mail);`——**无 setTo/setRecipients**）+ `#sendSmsIfPossible`（L173-186 同型：SmsMessage 仅 setText，无手机号）+ `NotificationRecipientResolver`（全文只产出 `Set<String>` userId——**无 user→email/手机号 解析**）
- **证据**：接收人解析四策略（ROLE/ORG/USER_LIST/PARTNER）均以 nop-auth userId 为终点；启用 `erp-notify.email-enabled=true` 并接入真实 IEmailSender 后，所有邮件**无收件人**（取决于 nop-integration 实现是抛错还是静默丢弃）。owner doc README「经 nop-integration 的 IEmailSender/ISmsSender SPI 派发」未声明地址解析归属；Successor 清单亦无「地址解析」条目——属实现缺口而非裁决缺口。当前 Noop 实现掩盖。
- **问题**：D5 通道完备性（结构性无法投递）。
- **建议修复方向**：user→email/手机号 解析（nop-auth NopAuthUser.email/手机号字段或通知偏好实体——后者 owner doc 已标 ask-first successor）；修复阶段先裁决地址源归属。
- **arm-index 裁决**：新增（grep「收件地址/email setTo」零命中）。

### P2-CK-notify-006（D5，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——Template status（DRAFT↔ACTIVE）可直改、Notification 实例可裸改

- **控制点**：`ErpSysNotificationTemplateBizModel`（17 行 stub：status 可经 update_ 直改 DRAFT↔ACTIVE——绕过「仅 ACTIVE 参与派发」的治理面：下线一个模板 = 直改 DRAFT（静默停摆对应事件全部通知，且无审计）；notificationType 直改可劫持 UK 归属）+ `ErpSysNotificationBizModel`（stub CRUD 面：recipientUserId/status/mergeCount 可直改——收件箱数据完整性旁路）+ `ErpSysNotificationReadBizModel`（已读记录可伪造/删除）
- **问题**：同型 P1-CK-pur-003 族——notify 站点登记（不复用展开）。模板 status 直改的静默停摆面是本域特有放大器（与 P1-CK-notify-002 的漂移不可见叠加）。
- **建议修复方向**：Template defaultPrepareUpdate/Save 守卫 status 变更（或补 publish/deactivate 命名 mutation）；Notification/Read 拒绝 CRUD 写入（只读派生产物）或白名单 remark。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，notify 站点新增计数）。

### P3-CK-notify-007（D9）unreadOf/countUnread/markAllRead 全量加载——countUnread 加载全部实体仅取 size；notIn 大集合无上限

- **控制点**：`ErpSysNotificationBizModel#countUnread`（L104-106：`return unreadOf(resolveUserId(...)).size();`——加载该用户**全部**未读通知实体 + 全部已读记录仅为计数）+ `#unreadOf`（L85-103：先 findAllByQuery 全部 Read 记录、再 `notIn("id", readIds)` 全量查 Notification，**无 limit**）+ `AbstractErpSysNotificationProcessor#unreadOf`（同型副本 L64-88）+ `ErpSysNotificationMarkAllReadProcessor#markAllRead`（L15-27：对 unreadOf 结果**逐条再 isRead**（冗余 N+1——unreadOf 已排除已读）后逐条 saveEntity）
- **证据**：收件箱前端轮询 countUnread（inbox-patterns.md「data-source countUnread 轮询」）——高频路径全量加载；长期用户（千级通知/已读）每轮 O(N) 实体物化。countUnread 应下推 SQL count；notIn 大 IN 集合在部分 DB 有长度/性能约束。
- **问题**：D9 性能（读路径 N+1/全量）。
- **建议修复方向**：countUnread 改 count 查询（`findCount`）；unreadOf 分页 + 已读集走 exists 子查询或 Read 表反向 join；markAllRead 批量构造去掉逐条 isRead。
- **arm-index 裁决**：新增（grep「countUnread 全量/notIn 性能」零命中）。

### P3-CK-notify-008（D6）renderTemplate 缺失 key 静默空串 + toCamelKey 宽松匹配——模板与 context 键漂移渲染为空洞通知且不可见

- **控制点**：`NotificationDispatcher#renderTemplate`（L196-219：`Object val = context.containsKey(key) ? context.get(key) : context.get(toCamelKey(key, context)); String replacement = val == null ? "" : val.toString();`——**缺失 key 渲染为空串，无 WARN**；`toCamelKey`（L221-226）把 `a_b` 去下划线后匹配 `ab`——snake/dotted 变体可能意外命中无关 key）+ `NotificationRecipientResolver#interpolateConfig`（L246-261 同型：config 占位符缺失渲染空串——`USER_LIST ${submitterUserId}` 的 submitterUserId 缺失时静默产出空 userIds → 接收人空 → 整条通知静默丢弃（dispatch L71-75 WARN 仅为「接收人为空」不指向根因））
- **证据**：模板渲染缺失键与模板种子缺失（002）同属「静默降级不可见」家族：调用方 context 键拼错（如 shipmentCode vs shipment_code vs code）时用户收到「…通知：」空洞正文，无任何日志可定位。对照：渲染**异常**有 ERR_NOTIFY_RENDER_FAILED + 外层 catch ERROR 日志——缺失键不是异常路径故全静默。
- **问题**：D6 渲染语义（缺失键应至少 WARN 或渲染占位标记）。
- **建议修复方向**：缺失 key 时 WARN（含 templateId+key）或渲染 `[${key}]` 原文占位；toCamelKey 匹配命中时 DEBUG 记录。
- **arm-index 裁决**：新增（grep「渲染 空 key/toCamelKey」零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | notify 受影响面 | 结论 |
| --- | --- | --- |
| `P2-RC-081`（UC-SYS-07 通知总开关 erp-notify.enabled 零消费，watch-only） | `ErpNotifyConfigs` L15 声明 + README 配置表「默认 true」；grep main 代码仍零消费（notifyProcessor/dispatcher 均不读） | **复用不展开**（现状一致，watch-only todo 已立案；与 b2b-004/logistics-010/aps-009 家族同型） |
| `P1-CK-pur-003` 族（CRUD update 无守卫） | Template/Notification/Read 三实体 | 同型登记为 P2-CK-notify-006 |
| cron 键漂移家族 | notify 无自有 job（异步外发归 nop-message successor） | 不适用（D4 无 job 面） |
| `P1-CK-fin-003` / `P0-CK-mfg-001` | notify 无过账/固定幂等键形态（合并组 key 非幂等键——README Successor 已裁决通知去重键 successor） | 不适用 |
| B1 告警通道完整性（任务点名「B1 模式的告警闭环依赖本子系统」） | fin.posting-exception（ErpFinPostingExceptionRecorder REQUIRES_NEW 隔离调用）等告警链依赖 notify 落地；模板缺失时告警链断裂 | 缺口归 P1-CK-notify-002（种子对账）登记 |
| orgId 隔离族 | ErpSysNotification 无 orgId 列（全局子系统，userId 单轴）——README 边界未承诺 org 隔离 | 不适用 |
| cs.entitlement-expiry 模板缺失先例（cs 域报告已登记） | 同根缺口（seed 失同步） | 计入 P1-CK-notify-002 的 8+ 站点清单（不重复展开） |

## 验证为正确（显式排除，防误报）

- **模板缺失静默跳过是 owner-doc 裁决的设计**（任务点名「fail-closed 还是静默」定论）：README「通道与失败语义」+ 状态机节「业务事件无 ACTIVE 模板时 config-gated 静默跳过（WARN）」在案；`notify()` 整体 try/catch best-effort（任何失败不抛给调用方返回空列表）与 README「子系统对业务方是 best-effort 服务」逐字一致——**静默本身验证为正确**，缺对账机制登记为 P1-CK-notify-002。
- **PENDING/FAILED 非死 dict 状态（B2 通过）**：README 状态机节逐字「PENDING 待发送（**预留**，当前同步派发不经过）」「FAILED 发送失败（**预留**）」——owner doc 显式预留裁决在案；外发失败不置 FAILED（站内已落库 SENT，外发属附加通道）与 best-effort 语义自洽。
- **合并-持久化分工正确**：`mergeInto` 仅内存修改（javadoc 自认），持久化统一由 NotifyProcessor 收口（`id==null → saveEntity / id!=null → updateEntity`，新建/合并两路径均落库）——「dispatcher 不保存」不是缺陷。
- **频控合并主干正确**：`findMergeable` 按 (notificationType, recipientUserId) + 窗口 `createTime >= now-window` + PENDING/SENT/MERGED 三态 + **已读排除**（isRead 逐条判过才可合并——已读通知不再吞新事件）+ `addOrderField("createTime", true)` 降序取最新（平台 L433 实证 desc）+ limit 10 防大扫描；mergeGroupId = type#userId 与 README 常量一致。
- **接收人解析四策略正确**：ROLE 两跳（roleName→NopAuthRole→NopAuthUserRole→userId，无匹配 WARN 返回空）；ORG deptId→NopAuthUser；USER_LIST 静态 + `${var}` context 插值（submitterUserId 动态接收人——RC-R1.37 依赖面在位）；PARTNER 占位 WARN 返回空（README ⚠️ 占位标注一致）。nop-auth 平台实体 IDaoProvider 只读有 javadoc 理由（平台实体无应用层 IBiz）。
- **已读模型正确**：已读派生自 ErpSysNotificationRead 关联（非 lifecycle status）——`unreadOf` 过滤 `status in (SENT, MERGED)` + notIn 已读集；`UK_SYS_NOTIFY_READ_NOTIF_USER (notificationId,userId)` 防重复标记；markRead 幂等（isRead 判过才写）。inbox-patterns「禁用 status=READ 过滤」后端无违反（全模块无 READ 字面量）。
- **`addOrderField` 布尔参数正确**：findUnread/findRead `("sentAt", true)` 降序最新优先 ✓、findMergeable `("createTime", true)` 降序取最新 ✓（平台 QueryBean L433-438 实证第二参为 desc）。
- **D1 机械扫描零命中**：module-notify 全部 main 代码 `System.currentTimeMillis/new Date()/LocalDateTime.now()/@Inject private/extends RuntimeException/printStackTrace`/字符串 `==` 零命中（时间全部 CoreMetrics；ERR_NOTIFY_* 全部 NopException + 中文描述）。
- **beans.xml 8 bean 接线完整**：Resolver + Coordinator + Noop×2（经类型注入满足 Dispatcher 的 @Inject IEmailSender/ISmsSender）+ Dispatcher（构造注入两协作者）+ 3 Processor，无孤立声明。
- **parseConfig/interpolateConfig 防御在位**：非法 JSON 抛 ERR_NOTIFY_RECIPIENT_RESOLVE_FAILED（非静默）；插值 quoteReplacement 防正则注入。

## arm-index 复用 or 新增裁决（汇总）

- **新增** 7 条：`外发先于持久化/事务`（001）/`种子对账缺失`（002）/`合并不覆盖外发`（003）/`markRead 边界`（004）/`外发无地址载体`（005）/`渲染缺失键静默`（008）+ 006（pur-003 族同型站点）+ 007（性能）。
- **同型登记**：P2-CK-notify-006（pur-003 族）。
- **复用（注记不展开）**：P2-RC-081（总开关零消费——现状一致）。
- **同型核查不适用**：cron 键漂移家族（无 job）、P1-CK-fin-003（无过账）、P0-CK-mfg-001（无固定幂等键——去重键 successor 已在 owner doc 裁决）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 2 | P1-CK-notify-001、P1-CK-notify-002 |
| P2 | 4 | P2-CK-notify-003..006 |
| P3 | 2 | P3-CK-notify-007、P3-CK-notify-008 |

按主维度：D7×1（001）、D4×1（002）、D6×2（003、008）、D5×2（004、005、006 归 D5 桶——精确：004 D5、005 D5、006 D5 共 3）、D9×1（007）。（精确主维度归属：001 D7、002 D4、003 D6、004 D5、005 D5、006 D5、007 D9、008 D6。）

同型/复用裁决：同型登记 1（006 pur-003 族）+ arm 复用不登记 1（P2-RC-081）+ 同型核查不适用 3。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-notify-service 全部 11 文件通读 + orm（3 实体 versionProp/UK）+ beans 8 bean + 三库 seed 25 类型 vs 全仓 NOTIFY_EVENT 常量对账（8+ 缺口实证）+ 平台 addOrderField 实证 + 消费域接线面（logistics/aps 常量 + README 清单）+ owner docs（README/inbox-patterns）+ D1 机械扫描。
- **未深查**：wf .xwf listener 侧派发调用（各业务域范畴）；`erp-notify-web` 收件箱页面（仅口径抽查）；`erp-notify-api` 骨架；`_gen/`；nop-integration-api 的 IEmailSender/ISmsSender 真实实现行为（EmailMessage 无收件人时的平台侧处理——005 定性基于消息构造面）；A4.2 运行时确认切片（arm-index 2026-08-07）引用的测试侧补充未逐一复核。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-notify-001** 的实际影响依赖 EMAIL/SMS 通道启用状态（默认 false + Noop 实现下当前零副作用）——若产品基线明确「外发通道长期不启用」，可降级 P2（仍需修 afterCommit 语义防启用即活化）。
  2. **P1-CK-notify-002** 的种子缺失清单以 `module-notify/deploy/sql/*/​_seed_erp-notify.sql` 为唯一 seed 真相源（repo 全仓 *.sql grep）；若存在部署期别的模板初始化通道，需重新对账。crm.sequence-overdue / hr.contract-expiry / cs.entitlement-expiry 三站的域内登记状态需与对应域报告核对避免双计。
  3. **P2-CK-notify-004** 的越权面取决于 GraphQL 层是否有实体级数据权限（enable-action-auth=false 的 dev/prod profile 下无；test profile 有 FNPT 但未逐条核对 notify 的 action-auth 注册）——建议修复阶段按 %test profile 实测。
