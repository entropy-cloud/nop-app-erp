# ck-notify-r3 — notify U11 五维符合性审计报告（ai-check-r3 M1.15，派发子系统本体归属格）

> 工作项：M1.15（六单元全格；本报告 = U11 notify 格——**§3.2 共享代码唯一归属：通知派发子系统本体（模板驱动派发/收件箱管线）归本格**，各域通知发送调用仅在各域切片记录消费点；姊妹格见 `ck-maintenance-r3.md` / `ck-aps-r3.md` / `ck-logistics-r3.md` / `ck-master-data-r3.md` / `ck-common-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `e331c55b2`（脏面 = 1 条 untracked 计划文件，tracked 零修改）——口径同 `ck-maintenance-r3.md` 头注。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §1/§2 + §3.3 U11 行 + §6 勘误 E1。
> 范围：notify 子系统本体——模板驱动派发（NotificationDispatcher + renderTemplate + 频控合并 MergeCoordinator + RecipientResolver 五型）/收件箱管线（Notification/Read/Template 三实体 + markRead/markAllRead + inbox 页）/跨域事件面（49 个 NOTIFY_EVENT_* 常量 + 12 个 wf.* 事件接线消费）（`module-notify/erp-notify-{dao,service}` src/main）；owner docs `docs/design/notify/`（inbox-patterns/README/seed-data/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎归 fin-1（notify 无过账面）；common 抽象族行为归 U20（notify 实体无 posted/approveStatus 列 → 守卫惰性 = pur-003 族样本）；聚合横切面归 U21（user TOPM 注册维度⑭特例本格核对）；**全域消费点归各域格**（本格记录 5 例抽样：ct approval-task/cs sla-overdue/fin posting-exception/sal credit-over-limit/ast wf 三事件）。
> 零生产代码改动：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U11 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套 + 15 维度走查（域焦点：模板驱动派发/收件箱管线/nop-message 跨域事件——**子系统本体归属格 §3.2**）+ ⑮抽样 8 断言 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional 0/LocalDate.now=0）；checker 零漂移；XGEN 5 处全为 erp-notify-meta dict 校验点。②daoFor 命中 5 文件：RecipientResolver 有 javadoc 理由（平台实体无应用层 IBiz 包装 + 纯读）；Dispatcher/MergeCoordinator/NotifyProcessor/AbstractProcessor 4 处 daoProvider 直用自身实体（模式内用法非 I*Biz 替代场景，补注释归 common-014-r3 全仓族宽松面）——跨域写零命中；⑧notification-status：SENT/MERGED 有 writer，PENDING 仅 ORM default（代码路径恒覆写 SENT）+ FAILED 零 writer 零 reader（README:97,100「预留」登记在案 = 预留死值非新缺口）；template-status DRAFT→ACTIVE 唯一通道 = 未守卫 CRUD update（notify-006 同点）；recipient-resolver PARTNER 恒 WARN 空返回（README:83 占位标注一致）；⑨**nop-message 异步总线零消费**（仅 javadoc successor 提及，与 README:144 一致——successor 未启动非缺陷）；FNPT：实体级 6 + inbox 层 2（inbox-query/inbox-mutation 专属在位），3 个服务型动作挂实体级兜底 = 形态注记不立项（与 mnt/aps/log/md 页面动作族不同，inbox UI 动作已有专属 FNPT）；⑮8 断言 4 一致 4 漂移（2 归并 notify-001/002 + 1 归并 011-r3 同点 + 1 README 事件名漂移并入 002 计数） | **finding**（2 新立：1 P2 + 1 P3；归并 8，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 通知收件箱 + **维度⑭特例：user TOPM 注册 grep** | `npm run validate:flux` step[1/3] 0 error/999/855；325 ERR 全 variant 族（notify 命中 2 条同族不立项）；AMIS 0 / ORM flux 缺失 0。**⑭特例核对 pass**：聚合器 `app.action-auth.xml:23` 含 `/erp/notify/auth/erp-notify.action-auth.xml` x:extends 引入；user TOPM 段原文（erp-notify.action-auth.xml:6-14）：`resource id="notify-inbox" resourceType="TOPM" roles="user"` + 子资源 `ErpSysNotification-inbox SUBM roles="user" component="FLUX"`——非 admin-only，聚合链完整。页面面 3 视图族 + inbox 手写页：3/3 view.xml 保留层继承、inbox 全量 i18nEn 双语（316 行）、无 /r/ 裸 REST（全 @query:/@mutation:）、admin view bounded-merge 裁列符合 inbox-patterns:105 反模式要求；**缺陷 = 新立 P3-CK-notify-011-r3**：inbox「全部」tab 数据源仅 findUnread（:261，无 findRead 拼接）→ 已读通知在全部 tab 永不出现（vs inbox-patterns.md:46「客户端拼接 findUnread + findRead」）+ selection 请求实体不存在的 `read` 字段（:265，xmeta/orm 零该列）恒空列。E2E：notify-inbox spec PageObject 合规 + E2E_ENGINE 缺省 flux | **finding**（1 新立 P3：011） |
| **DIM-S seed 数据** | §1.3 全套 + `erp_sys_notification_template.csv`（27 行，0825 聚合先例）+ deploy 同步登记处 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；porcelain 空；**372 CSV + 1 SQL** 冻结值；`erp_sys_notification_template.csv` **27 数据行**实数核对（28 行含表头）全 ACTIVE；**deploy 同步义务核对 pass**：`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql` 三方言命中，seed-data.md §同步义务登记处 :107「✅ 已聚合（2026-08-25，erp_sys_notification_template.csv）三方言 diff 零差异」逐字在案（M1.10 先例口径）；模板内容与已接线事件的缺口 = notify-002 finding 面（B 维），seed 资产本体完整性与同步义务本格 pass | **pass**（002 缺口归并注记） |
| **DIM-T 单元测试** | §1.4 全套 + 通知派发集成测试（C16 族快照纪律） | `mvn test -pl module-notify/erp-notify-service` **23/0/0/0 全绿**（= 锚点 23 零增量，两轮同值）；6 测试类全 `@NopTestConfig(localDb=true)` JunitAutoTestCase 快照式 + `_cases` 6 族 22 用例（input/output/tables 成对）；RECORDING = 0；**派发集成测试在位**：`TestErpSysNotificationCrossDomain`（posting-alert/sla-overdue/多事件独立三用例 = C16 族跨域派发冒烟）+ Dispatch/Subscription/SeedTemplates/TemplateLifecycle/RecipientResolverRuntime；动作覆盖：notify 49 处/markRead 4 处/markAllRead 4 处/findUnread 9 处/countUnread 14 处均有引用，**findRead 零测试引用**（仅 inbox 页消费）→ 归并 011-r3 同控制点注记（DIM-F+DIM-T 双面） | **pass**（011-r3 归并态注记） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套 | `--strict` **PASS exit 0** + `--self-test` **PASS**；notify 探针族 CAT-1 15 维持清零零回归；WHITELIST notify 条目 **0**（记录条目数 0，与计划基线一致）；`@Locale` 缺失 = 空；meta 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-notify.md`（C8.1）全 8 条逐一比对 + r2 只读目录 + §Mission 基线快照。**本轮新立 3 条（1 P2 + 2 P3）**；历史 8 ID 零覆写（r1 族最大号 CK-notify-008，新立自 009 起）。

### 2.1 复用 — 0 条（8/8 open 无 fixed 同型）

### 2.2 归并（同型 open 追加证据至原 ID）— 8 条

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-notify-001 | `NotificationDispatcher.dispatch` L95-97 外发先于落库（L90-93 构造/合并时 n.getId()==null）；`ErpSysNotificationNotifyProcessor` L47 dispatch → L49-55 saveEntity；`grep afterCommit module-notify` 排除 target 零命中；README.md:144「txn().afterCommit」逐字漂移在案。EMAIL/SMS 默认 false + Noop 实现缓解注记 |
| P1-CK-notify-002 | **现症扩大：缺口 8 → 34**。代码侧全仓 NOTIFY_EVENT_* 常量去重 49 + wf.* 字面 12 = **61 个已接线事件**；ACTIVE 种子 27 行覆盖 15 常量 + 12 wf.* = 27。r1 原 8 缺口全在位（log×2/aps×3/crm.sequence-overdue/hr.contract-expiry-warning/cs.entitlement-expiry——r1 误记 hr.contract-expiry，实际常量 `ErpHrContractExpiryJob.java:29`）；**新增 26**：ct×11/ast×3/hr×2（leave-approver-timeout/salary-posting-failure）/inv×2/mfg×3/mnt×2/prj×1/b2b×1/cs.fulfillment-approval-request，各缺失类型 ≥1 派发点 rg 实证。静默跳过无对账机制原样（NotifyProcessor L43-45 WARN return emptyList） |
| P2-CK-notify-003 | Dispatcher L120-131 命中合并返回既有实例后 L95-97 仍无条件 dispatchExternalChannels 原样 |
| P2-CK-notify-004 | MarkReadProcessor L16 getEntityById 可 null → L19-22 n==null 仍 L24-26 saveEntity 孤儿行；L19 userId 取 n.getRecipientUserId() 非调用者无身份校验原样（**修复所需错误码已定义未接线** → 009-r3 关联注记） |
| P2-CK-notify-005 | Dispatcher L177-180 EmailMessage 仅 subject/text、L193-195 SmsMessage 仅 text；RecipientResolver 仅产出 userId 集合无地址解析；NoopEmailSender:21 getTo() 恒 null 原样 |
| P2-CK-notify-006 | 三 BizModel stub extends 基类；三实体 ORM 零 posted/approveStatus 列 → 守卫惰性；Template.status DRAFT↔ACTIVE 可直改原样（save 通道盲区归 common-011-r3） |
| P3-CK-notify-007 | BizModel L107-109 countUnread=unreadOf().size() + L128 全量 + L138 notIn 无上限 + MarkAllReadProcessor 逐条 isRead 冗余 N+1 原样 |
| P3-CK-notify-008 | L218-219 缺失 key 静默空串 + **现症精化**：toCamelKey（L231-237）实为去下划线拼接（`a_b`→`ab`）非 camelCase 转换，永远无法命中真实 camelCase key——宽松匹配面比 r1 描述更窄且方法名误导；同型 RecipientResolver.interpolateConfig L216-217 缺失插值空串 → USER_LIST 静默为空整条丢弃原样 |

### 2.3 新立 `-r3` — 3 条（1 P2 + 2 P3）

**P2-CK-notify-009-r3**（DIM-B D4/D10 死错误码族）
- **控制点**：`ErpNotifyErrors.java:29-93` 定义 14 个 ErrorCode；排除定义文件后全仓引用计数：仅 ERR_NOTIFY_RECIPIENT_RESOLVE_FAILED（2）与 ERR_NOTIFY_RENDER_FAILED（2）被消费，**其余 12 个 uses=0**（TEMPLATE_NOT_ACTIVE/CHANNEL_DISABLED/TEMPLATE_NOT_FOUND/TEMPLATE_DUPLICATE_TYPE/SUBSCRIPTION_NOT_FOUND/SUBSCRIPTION_DUPLICATE/**INSTANCE_NOT_FOUND**/CHANNEL_PROVIDER_FAILED/LOCALE_NOT_SUPPORTED/DISPATCH_RETRY_EXHAUSTED/RECIPIENT_EMPTY/EVENT_TYPE_INVALID）。
- **问题**：错误码面系统性未接线——INSTANCE_NOT_FOUND 恰是 notify-004 修复（markRead not-found 拒绝）所需码已定义未用；O-11 扩展码无任何「预留」owner-doc 裁决标注，deny-by-default 反面（define-but-never-raise）使异常路径静默走通式失败。
- **三态裁决**：新立（r1 仅验证「错误码全 NopException + 中文描述」，未查死码面）。P2（异常路径契约面系统性缺口；随 notify-004/009 修复批次可半数收口，故非 P1）。
- **修复方向**：M2.x——按 notify-004（markRead not-found→INSTANCE_NOT_FOUND）/005（RECIPIENT_EMPTY）/003（DISPATCH_RETRY_EXHAUSTED）修复批次逐码接线；确认「预留」语义的码补 owner-doc 裁决注记。

**P2-CK-notify-010-r3**（DIM-B D5 越权——r1 豁免面纠错）
- **控制点**：`ErpSysNotificationMarkAllReadProcessor.java:16-18` `resolveUserId(userId, ctx)` **优先取调用方显式传入 userId**（AbstractProcessor L40-45 解析序）；unreadOf 仅消费 resolved 结果不强制 ctx。
- **问题**：任意登录用户传他人 id 可批量将其未读置已读。r1 P2-CK-notify-004 控制点 B 断言「markAllRead 经 unreadOf 按 ctx 用户过滤无此问题」与代码不符——本条为 r1 豁免面的纠错新立（原 ID 不动，其 markRead 控制点仍独立成立）；inbox-patterns.md:25 ctx 回退裁决针对「前端不传 userId」场景，未授权任意显式 userId 直通。
- **三态裁决**：新立（同 notify-004 不同控制点，对齐 pur-005/pur-015-r3 分立先例）。P2（越权写）。
- **修复方向**：M2.x——markAllRead/markRead 统一强制 `ctx.getUserId()` 或收件人身份校验（与 004 同批）。

**P3-CK-notify-011-r3**（DIM-F/DIM-T 收件箱实现漂移簇）
- **控制点**：`inbox.page.yaml:261` 全部 tab 数据源仅 `@query:ErpSysNotification__findUnread`（无 findRead 拼接）vs `inbox-patterns.md:46`「全部 = 客户端拼接 findUnread + findRead」；`:265` selection 请求实体不存在的 `read` 字段（xmeta/orm 零该列，:289-299 mapping 与 :315 tpl 消费 undefined）；`findRead` 动作 src/test + `_cases` 零测试引用。
- **问题**：已读通知在「全部」tab 永不出现（两工作 tab 掩盖）+ 幽灵字段 selection 的 GraphQL 校验风险 + 唯一消费该字段的页面无回归保护（DIM-F+DIM-T 双面同控制点）。
- **三态裁决**：新立（r1 切片边界明言 web 面仅口径抽查不深查）。P3。
- **修复方向**：M2.x 前端批——补 findRead 拼接或 owner doc 降级裁决 + 移除 read 幽灵列 + 补 findRead 用例。

### 2.4 归属标注（§3.2 + 跨域横切）

- **本体归属（本格收口）**：派发管线（Dispatcher/MergeCoordinator/RecipientResolver/NotifyProcessor）、模板族、收件箱管线全部行为缺陷归本格；008 toCamelKey 精化与 002 缺口扩大为本轮本体格增量证据。
- 全域消费点归各域格（本格 5 例抽样记录）：ct approval-task（ErpCtApprovalRecordBizModel:254）/cs sla-overdue（ErpCsTicketScanOverdueTicketsProcessor:216）/fin posting-exception（ErpFinPostingExceptionRecorder:161）/sal credit-over-limit（CreditLimitChecker:249）/ast wf 三事件（asset-disposal-approval v1.xwf:44,67,113）——全部经 `IErpSysNotificationBiz` 或 wf listener 合规。
- common 抽象族：守卫惰性 = pur-003 族样本；自身实体 daoFor 4 处无注记归 common-014-r3 宽松面注记（模式内用法）。
- 聚合横切面归 U21：user TOPM 聚合链本格核对 pass（维度⑭特例）。
- 跨域模板缺口族：log×2/aps×3/mnt×2 等新缺口证据归并 notify-002（原 ID 追加，各域格已交叉注记）。

### 2.5 维度⑮断言抽样记录（2 doc × 8 断言：一致 4 / 漂移 4）

inbox-patterns.md 4 断言（:25 @Optional userId 回退 ctx ✓ / :44 findUnread 过滤公式 ✓ / :46 全部 tab 拼接 findUnread+findRead → **漂移 = 011-r3 新立** / :94-99 菜单表 orderNo+权限集 → **漂移**（实际 9001/10001/10004/10007 编号体系 + inbox 含 query+mutation 双 FNPT，doc 过时——软漂移注记不立项））；README.md 4 断言（:89 notify() try/catch 返回空列表 ✓ / :106 无 ACTIVE 模板 WARN 静默跳过 ✓ / :144「同步派发 + txn().afterCommit」→ **漂移 = notify-001 同点** / :126,145 事件名 hr.contract-expiry → **漂移**（实际 hr.contract-expiry-warning，并入 002 计数））。漂移 4 = 2 归并 + 1 新立 + 1 软漂移，扩样条款以新立 1 处理（抽样已覆盖全部 4 owner doc）。

## 3. 统计

| 级别 | 本轮新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 2（notify-001/002，002 缺口 8→34 扩大证据） |
| P2 | 2（notify-009-r3 死码族 + notify-010-r3 markAllRead 越权） | 0 | 4（notify-003..006） |
| P3 | 1（notify-011-r3 收件箱漂移簇） | 0 | 2（notify-007/008） |
| **合计** | **3** | **0** | **8** |

五格 verdict：DIM-B **finding**（2 新立）/ DIM-F **finding**（1 新立）/ DIM-S **pass**（002 归并注记）/ DIM-T **pass**（011 归并注记）/ DIM-I **pass**。历史 8 条 r1 ID 零覆写。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-notify-{dao,service} 全部 6 Processor + Dispatcher/MergeCoordinator/RecipientResolver 全方法走查 + 三实体 BizModel + 5 dict 逐值 writers + inbox 页 316 行逐段 + user TOPM 聚合链 + 机械程式全套实跑；owner docs 2 doc × 8 断言 + 61 事件接线 vs 27 模板全量对账；r1 8 条全量逐条复核；r2 目录核对；notify seed 27 行实数 + deploy 三方言登记处核对；全域消费点 5 例抽样。
- **未深查（边界归属）**：各域 49 常量派发点的业务语义（归各域格）；nop-message 异步总线 successor（未启动面，README:144 一致）；浏览器端 inbox 渲染回归（归看板专项）；EmailMessage/SmsMessage 平台侧通道实现（nop-message 依赖面）。
- **残留风险（登记不裁决）**：① 8 条归并 open 修复归 M2.x，P1 两条建议最优先——001（事务内外发时序）在 EMAIL/SMS 投产（config enabled）时升 P0 级阻断项，002（34 模板缺口）使全域告警静默失效（cs 先例已实际发生）；② 009-r3 死码接线与 004/010 修复同批设计（身份校验 + not-found 语义 + 重试耗尽三合一）；③ 预留死值面（PENDING/FAILED/PARTNER）与 README 占位标注一致性维持 watch。
- **successor 触发条件**：M1.17 收官完整性校验本报告 5/5 格；EMAIL/SMS 外发投产前 notify-001/005 必须修复；全域告警依赖投产前 notify-002 缺口清单（34）必须裁决（补模板 or 对账机制 or 显式 Non-Goal 登记）。
