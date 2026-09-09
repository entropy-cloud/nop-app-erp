# ck-logistics-r3 — logistics U16 五维符合性审计报告（ai-check-r3 M1.15）

> 工作项：M1.15（六单元全格；本报告 = U16 logistics 格，姊妹格见 `ck-maintenance-r3.md` / `ck-aps-r3.md` / `ck-notify-r3.md` / `ck-master-data-r3.md` / `ck-common-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `e331c55b2`（脏面 = 1 条 untracked 计划文件，tracked 零修改）——口径同 `ck-maintenance-r3.md` 头注。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §1/§2 + §3.3 U16 行 + §6 勘误 E1。
> 范围：logistics 全域——承运商（carrier/carrier-config + gateway client SPI + webhook）/运费结算（delivered→freight posting 消费侧 + landed cost 联动）/发运（shipment 状态机 10 边 + parcel/log/line 族 + tracking poll + draft escalation 双 job）/交付预约（delivery window/booking）（`module-logistics/erp-log-{dao,service}` src/main）；owner docs `docs/design/logistics/`（carrier-integration/delivery-window/state-machine/README/use-cases/seed-data）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1——`LogisticsFreightProvider` 仅 implements `IErpFinAcctDocProvider` 纯函数产出 VoucherFact（消费侧合规），引擎 resolveProvider/post 编排归 fin-1（M1.1 已收官）；common 抽象族行为归 U20（logistics 实体无 approveStatus 列 + posted 恒 false → 守卫惰性 = pur-003 族样本；save 通道盲区 common-011-r3）；聚合横切面归 U21；notify 本体归 U11（log 消费点 2 事件记录）。
> 零生产代码改动：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U16 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套 + 15 维度走查（域焦点：承运商调度/运费过账消费侧/发运状态机；共性②⑧⑨⑮）+ ⑮抽样 7 断言 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional 0/main 面 LocalDate.now=0）；checker 零漂移；XGEN 12 处全为 erp-log-meta dict 校验点。②跨域**全部经 I*Biz**（sal delivery/order + fin voucher + inv landed cost + notify 四边注入），跨域 daoFor 零命中——**②维边界全仓最优**；唯一缺口：`AbstractErpLogShipmentDeliveredProcessor.java:27,52` IDaoProvider 注入（消费点 :266 AcctSchemaResolver/:290 daoFor 自域实体）无豁免注释 = **新立 P3-CK-log-014-r3**（同层 GatewayDispatcher/两 job 均有注释，范式不对称）；⑧shipment-status 6 值逐值 writers 全活零死值 + 10 边矩阵守卫在位（`ErpLogShipmentStateMachine.java:54-113`）+ 终态无出边 + cancel 对 DELIVERED 幂等短路；⑨gateway Registry/Dispatcher/6 Processor/2 job 接线完整 + job yaml 双键对齐（tracking-poll cronExpr=bean 同键，对照 aps-010 漂移族 log 为合规样本）；**10 个自定义 mutation 零专属 FNPT = 新立 P3-CK-log-015-r3**；⑮7 断言 4 一致 3 漂移（2 归并 log-003/008 + 1 新立 log-013-r3 + 1 命名微漂移注记） | **finding**（3 新立 P3：013/014/015；复用 1 + 归并 10，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 发运追踪页（复杂手写页清单成员） | validate:flux step[1/3] 0 error/999/855；325 ERR 全 variant 族（log 命中 8 条同族不立项）；AMIS 0 / ORM flux 缺失 0。页面面 8 实体 CRUD 页集 + shipment-tracking 双载体全量清点：8/8 view.xml 保留层继承；CRUD 页 `@query:ErpLog*__findPage/__get` REST 合规、死分支 0（本域轴 status）、i18nEn 在位；**发运追踪页缺陷 = 新立 P2-CK-log-012-r3**：注册页 `shipment-tracking.page.yaml:43-48` timeline 数据源 `{query:{limit:5000}}` 无 shipmentId 过滤 → 渲染全部发运单混合日志（头注释 :3 与页面文案 :71 均自称按发运单过滤），选择发运单仅影响信息条；修复版 `shipment-tracking.flux.yaml`（:49-60 含 `filter_shipmentId`）全仓零引用未注册（auth :19 仅指向 page.yaml）= 功能缺陷 + 死产物双面。E2E：3 个 log spec PageObject 合规 + E2E_ENGINE 缺省 flux；visual 族 spec 零 log 页触达 | **finding**（1 新立 P2：012） |
| **DIM-S seed 数据** | §1.3 全套 + logistics 8 表 seed（M1.4b 批） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；porcelain 空；**372 CSV + 1 SQL** 冻结值；`erp_log_*` **8 CSV** 精确在册（carrier/carrier_config/shipment/shipment_line/shipment_log/shipment_parcel/delivery_window/delivery_booking）；log deploy `_seed_*.sql` = 0（登记处一致）；非过账域静态 seed 无 posted 行面 N/A | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P2 Shipment 清单行 | `mvn test -pl module-logistics/erp-log-service` **66/0/0/0 全绿**（= 锚点 66 零增量，两轮同值）；14 测试类 + `_cases` 14 用例根一一对应；RECORDING = 0；**P2 Shipment 链全覆盖**：PostingEnd/FreightPosting/Path2LandedCost/PostingFaultInjection/SalesDeliveryLinkage/CarrierGatewayIntegration/ShipmentGateway/StateMachineMatrix（12）；**公开动作覆盖 10/10**（advise/completeShipment/cancelShipment/handleTrackingWebhook/scanForPolling/book/releaseForShipment/markArrived/markMissed/save 均有测试引用，零缺口动作 = 0） | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套 | `--strict` **PASS exit 0** + `--self-test` **PASS**；log 探针族 CAT-1 19 维持清零零回归；WHITELIST log 条目 **0**（记录条目数 0，与计划基线一致）；`@Locale` 缺失 = 空；meta 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-logistics.md`（C8.1）全 11 条逐一比对 + r2 只读目录 + §Mission 基线快照。**本轮新立 4 条（1 P2 + 3 P3）**；历史 11 ID 零覆写（r1 族最大号 CK-log-011，新立自 012 起）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 1 条

| 原 ID | 修复在位证据（T0） |
| --- | --- |
| P1-CK-log-001（post 幂等命中返回 null → markSettled 悬挂，F1.1 引擎级修复） | 调用点 `AbstractErpLogShipmentDeliveredProcessor.java:121-124` `voucherId != null` 分支 markSettled 在位；引擎侧 `ErpFinPostingProcessor.java:139-144` 幂等命中返回既有 POSTED 凭证 id（非 null）+ javadoc :122-123 自证——null 分支仅剩理论不可达，F1.1 修复传导有效（归属 fin-1 引擎，本格复核消费侧收口） |

### 2.2 归并（同型 open 追加证据至原 ID）— 10 条

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-log-002 | 派发点 `GatewayDispatcher.java:65` + `AbstractErpLogShipmentDeliveredProcessor.java:50` 在位；三库 `_seed_erp-notify.sql:118` 仅 log.draft-escalation（7201），两事件名 module-notify 全部 sql/csv 零命中（notify-002 全仓族并案原样） |
| P2-CK-log-003 | `ErpLogShipmentHandleTrackingWebhookProcessor.java:66-68` 仍返回 `carrier.getCode()`；CarrierConfig apiKey/apiSecret/credentials getter 生产零消费；owner doc `carrier-integration.md:304`「webhookSecret」断言漂移同点原样 |
| P2-CK-log-004 | `GatewayDispatcher.java:255-263` 仅 eq(trackingNo)，UK `(trackingNo,carrierId,delVersion)`（orm:233）跨承运商碰撞面原样 |
| P2-CK-log-005 | `AbstractErpLogShipmentDeliveredProcessor.java:206-212` 非终值即直写 DELIVERED 无数量聚合原样 |
| P2-CK-log-006 | `GatewayDispatcher.java:117/:123` 重试循环唯一 catch NopException，非 Nop RuntimeException 穿出无死信无告警原样 |
| P2-CK-log-007 | `ErpLogDeliveryBookingBizModel.java:73-76/:167-185` MISSED 仍算 active + priorityScore 仅写不读（:102/:159-160）原样 |
| P2-CK-log-008 | `:203-207` 生效期按 `CoreMetrics.today()` vs :213-214 星期按 bookedDate 双基准原样 |
| P2-CK-log-009 | 全域 defaultPrepareUpdate override 0 命中；基类锁对本域惰性（无 approveStatus 列 + posted 恒 false）；xmeta status/freightSettlementStatus/trackingNo + Carrier gatewayId/isActive + CarrierConfig credentials 全部可直改原样（save 通道盲区升级注记归 common-011-r3） |
| P3-CK-log-010 | README:106-111 四键零消费（含 `ErpLogConfigs.java:9/:47` 声明后零读取）原样 |
| P3-CK-log-011 | 四点全在位：posted 零 writer 但 `ErpLogShipment.view.xml:69` 列展示 + freightSettlementStatus 无 PENDING 初始化（唯一 writer SETTLED :278）+ not-found 误抛 `ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION`（:284-287，NOT_FOUND 码 :67/:70 定义未用）+ parsePayload :91-93 无类型防御原样 |

### 2.3 新立 `-r3` — 4 条（1 P2 + 3 P3）

**P2-CK-log-012-r3**（DIM-F 功能缺陷 + 死产物）
- **控制点**：`erp-log-web/.../dashboard/shipment-tracking.page.yaml:43-48`（timeline 数据源无 shipmentId 过滤，limit 5000 全量拉取）+ `:83`（`source: ${trackingData?.items ?? []}` 渲染全部发运单日志）vs 修复版 `shipment-tracking.flux.yaml:49-60`（含 `filter_shipmentId` 但全仓零引用）；auth 注册 `erp-log.action-auth.xml:19` 仅指向 page.yaml。
- **问题**：菜单可达的发运追踪页对任一发运单展示**所有发运单**的网关日志混合时间线（跨承运商/跨订单信息串页），且页头注释与正文文案均自称按发运单过滤——功能与自述背离；已修复的 flux 重写版未接线成死产物（孪生漂移家族 mfg-023-r3 的反向形态：权威版有缺陷、修复版无注册）。
- **三态裁决**：新立（r1 log 族明示 web 面仅 posted 列一处深查，:6 边界登记；本控制点无历史 ID）。P2（用户可见信息串页 + 已修复产物浪费）。
- **修复方向**：M2.x 前端批——page.yaml 补 shipmentId 过滤或切换注册到 flux 版并删除孪生；与 mfg-023-r3 孪生家族统一「双载体权威裁决」。

**P3-CK-log-013-r3**（DIM-B ⑮ owner-doc 漂移/守卫过松）
- **控制点**：`ErpLogDeliveryBookingBizModel.java:65-66` book 仅拒 CANCELLED/DELIVERED vs `delivery-window.md:117` D2 裁决「BOOKED = 预约创建态（发运单 **DRAFT/ADVISED** 期预约）」；正向测试 `TestErpLogDeliveryBooking.java:214-222` 恰以 DISPATCHED 运单预约成功。
- **问题**：DISPATCHED/IN_TRANSIT 在途运单仍可预约新窗口、占用窗口容量，与 owner doc 预约时机语义不符。
- **三态裁决**：新立（r1 log-007 仅覆盖 MISSED 死端与 priorityScore，未涉预约时机守卫）。P3。
- **修复方向**：M2.x——book 状态白名单收窄至 DRAFT/ADVISED（或 owner doc 登记放宽裁决）；先写在途预约拒绝失败测试。

**P3-CK-log-014-r3**（DIM-B ② 豁免注释缺口）
- **控制点**：`AbstractErpLogShipmentDeliveredProcessor.java:27,52` IDaoProvider 注入零豁免理由注释（消费点 :266/:290）；同层 GatewayDispatcher（Facade 编排范式 javadoc）/ErpLogTrackingPollJob/ErpLogDraftEscalationJob 均有注释——范式不对称。
- **三态裁决**：新立（同族 fin3-017-r3/fin4-022-r3/ast-028-r3/qa-027-r3/mfg2-026-r3/mfg3-019-r3/hr2-029-r3/drp-024-r3；全仓汇总归 common-014-r3）。P3。
- **修复方向**：M2.x 补一行豁免 javadoc（对齐 GatewayDispatcher 措辞）；与全仓族同批收口。

**P3-CK-log-015-r3**（DIM-B ⑨ FNPT 家族）
- **控制点**：保留层 `_erp-log.action-auth.xml` 仅 8 实体 × {query,mutation} 通用 FNPT 对（16 个）；10 个自定义 mutation（Shipment 6 + Booking 4：advise/completeShipment/cancelShipment/handleTrackingWebhook/scanForPolling/book/releaseForShipment/markArrived/markMissed）零专属注册。
- **三态裁决**：新立（同族 mnt-023-r3/aps-015-r3 本轮新立）。P3。
- **修复方向**：M2.x 按物流员角色补 FNPT 注册，与家族同批。

### 2.4 归属标注（§3.2 + 跨域横切）

- posting 引擎内部归 fin-1：LogisticsFreightProvider 纯函数消费侧合规；F1.1 幂等修复引擎侧归属 fin-1（本格仅复核消费侧收口）。
- common 抽象族：守卫惰性（无 approveStatus 列）= pur-003 族样本；IDaoProvider 缺注释 1 文件 = log-014-r3 本格新立 + common-014-r3 全仓族；ServiceContextImpl 站点归 common-015-r3 全仓族（log 4 文件无 fallback 在清单内）。
- 聚合横切面归 U21：聚合器注册在位；FNPT 缺口 log-015-r3 本格新立。
- notify 消费点记录（归 U11 格）：log.gateway-dead-letter + log.freight-posting-failure 两事件经 I*Biz 合规；模板缺口归并 notify-002/log-002 双向族。

### 2.5 维度⑮断言抽样记录（3 doc × 7 断言：一致 4 / 漂移 3）

state-machine.md 3 断言（重试 3 次→耗尽保留 ADVISED + deadLetter ✓ / 迁移图 4 源→CANCELLED + 部分签收 Deferred + IN_TRANSIT→CANCELLED 审批 Deferred ✓ / §2「DRAFT→ADVISED 调用 adviseShipment」→ **漂移（轻微命名）**：实际动作名 `advise`，adviseShipment 为 SPI client 方法名）；carrier-integration.md 2 断言（:304 HMAC 用 CarrierConfig.webhookSecret → **漂移 = log-003 同点** / retry 3 次间隔基准 30s ✓）；delivery-window.md 2 断言（:115 窗口有效期 + 星期匹配 → **漂移 = log-008 同点** / :117 BOOKED = DRAFT/ADVISED 期预约 → **漂移 = 013-r3 新立**）。漂移 3 < 2 扩样条款按新立 1 处理（同点归并不重复扩样，抽样已覆盖全部 3 owner doc）。

## 3. 统计

| 级别 | 本轮新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 1（log-001） | 1（log-002） |
| P2 | 1（log-012-r3 追踪页串页） | 0 | 7（log-003..009） |
| P3 | 3（log-013/014/015-r3） | 0 | 2（log-010/011） |
| **合计** | **4** | **1** | **10** |

五格 verdict：DIM-B **finding**（3 新立 P3）/ DIM-F **finding**（1 新立 P2）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 11 条 r1 ID 零覆写（1 fixed 复核有效 + 10 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-log-{dao,service} 全部 6 Processor + 2 job + GatewayDispatcher/Registry + ShipmentStateMachine + DeliveryBooking/Window BizModel + LogisticsFreightProvider + AbstractErpLogShipmentDeliveredProcessor 全方法走查 + ErpLogErrors/Constants/Configs + 机械程式全套实跑；owner docs 3 doc × 7 断言；r1 11 条全量逐条复核；r2 目录核对；log seed 8 CSV 对账；追踪页双载体逐行抽查 + visual spec 触达面扫描。
- **未深查（边界归属）**：ErpFinPostingProcessor 引擎内部（归 fin-1）；sal 被写实体 updateDeliveryStatus 消费语义（归 sal 格 M1.13 已闭合）；承运商外部网关真实协议行为（MOCK 层测试覆盖）；浏览器端追踪页渲染回归（归看板专项）。
- **残留风险（登记不裁决）**：① 10 条归并 open 修复归 M2.x，P1 log-002（死信/过账失败告警模板缺失）建议与 notify-002 全仓族同批收口（34 缺口清单已并入 notify 格）；② log-012-r3 与 mfg-023-r3 孪生家族建议 U21/M1.16 统一「双载体权威裁决」机制；③ webhook HMAC（log-003）投产外发通道前必须修复（安全面）。
- **successor 触发条件**：M1.17 收官完整性校验本报告 5/5 格；外发网关投产（gateway enabled + 真实 carrier）前 log-003/004/006 必须修复；追踪页投产前 log-012-r3 必须修复。
