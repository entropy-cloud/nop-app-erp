# MA2 aps+logistics 状态机审查（A2.15）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> 域/功能模块：aps（OperationOrder 5 态 + Schedule 排产方案 3 态 + scheduleForward/scheduleBackward + insertRushOrder 区间重排 + ErpApsSchedulingProcessor/ErpApsAtpCtpServiceImpl 跨域只读）+ logistics（Shipment 6 态 + 承运商网关 SPI + handleTrackingWebhook 回调 + onDelivered FREIGHT 过账 Facade + path-2 到岸成本 config-gated + scanForPolling 轮询兜底 + ErpLogShipmentLog）（C 级合并，2 域）
> 审计 plan：`docs/plans/2026-07-28-1249-1-audit-remediation-ma2-aps-logistics-state-machine.md`
> 行为基线：`docs/design/aps/state-machine.md` + `docs/design/logistics/state-machine.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 范围：2 域全部状态承载实体（plan baseline：aps OperationOrder 5 态 + Schedule 3 态 + 排产引擎 + 跨域只读 + logistics Shipment 6 态 + 网关 SPI + 轮询 + 过账 Facade），实际审查经逐文件全文阅读 + grep 验证
> 审计执行：2026-07-28
> 上游基线：MA1 done（P1-MA1-022 aps 扩展跨域只读 daoFor 已登记，本审计复核状态机角度）；A2.6a mfg done（WorkOrder 级联 aps OperationOrder 联动）；A2.11 inventory done（logistics 关联出库单锁定 + aps ErpInvReservation 跨域只读）；A2.14 ext-domains done（logistics FREIGHT 过账 tryPost 吞异常同型范式 P1-MA2-074 maintenance）；aps/logistics DIRECT 业务动作 + 跨域编排 E2E 已落地（2026-07-14-0508-1 + 2026-07-14-0941-2 happy path 已验证）

## 1. 审查范围与状态字段清单

| 域 / 实体 | 状态轴（dict） | 实现文件 | 审查方式 |
|----------|---------------|----------|---------|
| **aps / ErpApsOperationOrder**（工序工单 5 态） | `status`(erp-aps/operation-order-status DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED) | `ErpApsOperationOrderBizModel.java`（start/complete/cancel + scheduleForward/scheduleBackward/insertRushOrder 委托） | 全文逐行 |
| **aps / 排产引擎**（scheduleForward/scheduleBackward POJO） | — | `ErpApsSchedulingEngine.java`（贪心前/后向填充 plannedStart/EndDateT + WorkCenterTimeline capacity=1） | 全文逐行 |
| **aps / 排产编排 Processor** | — | `ErpApsSchedulingProcessor.java`（run + insertRushOrder 区间重排 + loadPendingOrders/loadPlannedInWindow + persist） | 全文逐行 |
| **aps / 工作中心时间轴** | — | `WorkCenterTimeline.java`（findFreeSlotForward/Backward + Interval overlaps） | 全文逐行 |
| **aps / ErpApsSchedule**（排产方案 3 态） | `status`(erp-aps/schedule-status DRAFT/PUBLISHED/ARCHIVED) | `ErpApsScheduleBizModel.java`（publish/archive + ILLEGAL_STATUS 守卫） | 全文逐行 |
| **aps / ATP-CTP 跨域只读** | — | `ErpApsAtpCtpServiceImpl.java`（atpAvailable/sumReserved + simulateCtp + buildShadowOps daoFor ErpInv*/ErpMfgBom*） | 全文逐行 |
| **aps / CRP 负荷来源 SPI** | — | `ApsLoadSourceProvider.java`（findScheduledSlots 只读 PLANNED OperationOrder） | 全文逐行 |
| **logistics / ErpLogShipment**（发运单 6 态） | `status`(erp-log/shipment-status DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED) + `freightSettlementStatus`(erp-log/settlement-status PENDING/SETTLED) | `ErpLogShipmentBizModel.java`（advise/completeShipment/cancelShipment/handleTrackingWebhook/scanForPolling/onDelivered/handlePurchaseReceiptDelivered） | 全文逐行 |
| **logistics / 网关编排 Dispatcher** | — | `GatewayDispatcher.java`（advise/completeShipment/cancelShipment/advanceTracking/scanForPolling + 重试 + deadLetter） | 全文逐行 |
| **logistics / FREIGHT 过账 Provider** | — | `LogisticsFreightProvider.java`（FREIGHT 借销售费用-运费/贷银行存款|应付账款） | 全文逐行 |
| **logistics / 承运商网关 SPI** | — | `IErpLogCarrierGatewayClient` + `IErpLogCarrierGatewayClientFactory` + `ErpLogCarrierGatewayRegistry` + `MockCarrierGatewayClientFactory` | 全文逐行 |

2 域状态字段分布在 aps(2: OperationOrder/Schedule) + logistics(2: Shipment 双轴 status/freightSettlementStatus) 共 4 个状态承载实体 + 排产引擎/网关 SPI/过账 Provider 助手（与 plan baseline 一致 ✓）。

---

## 2. 10 维度审查

### 2.1 维度「状态定义」

**裁决：PASS（含 owner doc drift 注记）」

#### aps OperationOrder（5 态）

✅ **每个状态表达「等待什么」**（owner doc aps/state-machine.md §1 表）：DRAFT=从 WorkOrder 创建未排产，等待 APS 运算 / PLANNED=APS 已排产赋值，等待车间确认 / IN_PROGRESS=车间执行中，等待完工 / FINISHED 终态 / CANCELLED 终态。dict `erp-aps/operation-order-status`（`app-erp-aps.orm.xml:8-14`）5 项与 `ErpApsConstants.OP_STATUS_*`（`ErpApsConstants.java:11-15`）1:1 对齐 ✓。

#### aps Schedule（3 态）

✅ **3 态语义清晰**（owner doc aps/state-machine.md §适用对象隐含 + `ErpApsScheduleBizModel.java:19-23` Javadoc 显式声明）：DRAFT=排产方案草稿，可重排 / PUBLISHED=已发布锁定为执行参照 / ARCHIVED 终态（历史归档）。dict `erp-aps/schedule-status`（`app-erp-aps.orm.xml:15-19`）3 项与 `ErpApsConstants.SCHEDULE_STATUS_*` 1:1 对齐 ✓。

#### logistics Shipment（6 态 + freightSettlementStatus 副轴）

✅ **6 态语义清晰**（owner doc logistics/state-machine.md §1 表）：DRAFT=草稿待确认发运 / ADVISED=已预约承运商，等待接单 / DISPATCHED=承运商已接单，等待运输更新 / IN_TRANSIT=在途，等待签收 / DELIVERED 终态触发运费过账 / CANCELLED 终态。dict `erp-log/shipment-status`（`app-erp-logistics.orm.xml:39-46`）6 项与 `ErpLogConstants.SHIPMENT_STATUS_*`（`ErpLogConstants.java:9-14`）1:1 对齐 ✓。

✅ **freightSettlementStatus 副轴 2 态**（PENDING/SETTLED，dict `erp-log/settlement-status`）：DELIVERED 后运费结算独立轴，允许 status=DELIVERED 终态但 freightSettlementStatus=PENDING（过账悬挂窗口期）。语义清晰，与 status 主轴正交。

**本维度无新发现。**

---

### 2.2 维度「转换完整性」

**裁决：FAIL（aps start/complete/cancel 完全缺状态守卫 + logistics 部分签收未实现）」

#### aps OperationOrder 迁移矩阵（实仓 vs owner doc §2）

```
DRAFT ──scheduleFwd/Bwd──→ PLANNED  ✅ 引擎 setStatus(PLANNED) on slot found（ErpApsSchedulingEngine.java:93,149）
DRAFT ──schedule conflict──→ DRAFT  ✅ 引擎保持 DRAFT on NO_AVAILABLE_SLOT（:85,122,132,140）
PLANNED ──insertRushOrder 区间重排──→ DRAFT  ✅ Processor:101-106 回退低优先级工序
PLANNED ──start──→ IN_PROGRESS  ⚠️ start:110-114 无 status 守卫（见下）
IN_PROGRESS ──complete──→ FINISHED  ⚠️ complete:121-125 无 status 守卫
PLANNED|IN_PROGRESS ──cancel──→ CANCELLED  ⚠️ cancel:129-135 无 status 守卫
FINISHED ──?──→ (owner doc 终态无出边)  ❌ start/complete/cancel 可从 FINISHED 触发（无守卫）
CANCELLED ──?──→ (owner doc 终态无出边)  ❌ start/complete/cancel 可从 CANCELLED 触发（无守卫）
```

⚠️ **P1-MA2-077 aps OperationOrder start/complete/cancel 完全缺状态守卫**：
- `ErpApsOperationOrderBizModel.start:108-117` —— **if/else 两分支逻辑完全相同**（`if status==PLANNED setStatus(IN_PROGRESS) else setStatus(IN_PROGRESS)`，:110-114 死代码），**无任何前置 status 校验**——FINISHED/CANCELLED/DRAFT 工序均可被 start 设为 IN_PROGRESS。
- `complete:121-126` —— 直接 `setStatus(FINISHED)`，**无前置 status 校验**——DRAFT/PLANNED/CANCELLED 工序均可被设为 FINISHED。
- `cancel:129-135` —— 直接 `setStatus(CANCELLED)`，**无前置 status 校验**——FINISHED 终态工序可被取消（违反 owner doc §3「终态不可恢复」）。

**违反 owner doc §2 迁移图（PLANNED→IN_PROGRESS→FINISHED 单向链 + PLANNED|IN_PROGRESS→CANCELLED 限定源态）+ §3 终态不可恢复**。

**裁决 P1 非 P0**：(1) aps 域是**纯内存排产算法 + 无 GL/库存/质量副作用**（`ErpApsSchedulingEngine` POJO 无 Spring/DB 依赖，:21-24 Javadoc 明示；start/complete/cancel 仅 setStatus + updateEntity，无跨域 Facade 调用）；(2) start/complete/cancel 是**手工 @BizMutation**，需运营主动触发，非自动流转；(3) ErpApsOperationOrder `versionProp="version"`（`app-erp-aps.orm.xml:57`）乐观锁将 silent lost-update 降级为 detectable conflict；(4) **与 qa P0-MA2-017 不同型**——qa 是 P0 因 passInspection 缺守卫**绕过强制质检门控**致不合格品 silent 入库，aps 无类似强门控被绕过；(5) 按 finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-039~042 owner doc 契约漂移裁决范式 P1。

#### aps Schedule 迁移矩阵（实仓 vs owner doc）

```
DRAFT ──publish──→ PUBLISHED  ✅ publish:43-57 守卫 status==DRAFT + 否则 ERR_APS_SCHEDULE_ILLEGAL_STATUS
DRAFT|PUBLISHED ──archive──→ ARCHIVED  ✅ archive:61-80 守卫 status∈{DRAFT,PUBLISHED} + 已 ARCHIVED 幂等返回
ARCHIVED ──?──→ (终态无出边)  ✅ archive 拒绝非 DRAFT/PUBLISHED 源态
```

✅ **Schedule 3 态全迁移守卫齐全**——`ErpApsScheduleBizModel.publish/archive` 均显式校验 src status 后迁移，幂等守卫完整（已 ARCHIVED 返回）。owner doc §2 完全对齐。

#### logistics Shipment 迁移矩阵（实仓 vs owner doc §2）

```
DRAFT ──advise──→ ADVISED  ✅ advise:54-69 守卫 status∈{DRAFT,ADVISED} + 幂等（已 ADVISED 直接返回）
ADVISED ──completeShipment 网关下单──→ DISPATCHED  ✅ completeShipment:76-120 守卫 + 重试 + deadLetter
ADVISED ──网关重试耗尽──→ ADVISED（死信保留）✅ deadLetter:324-332 写 remark + ErpLogShipmentLog
DISPATCHED ──advanceTracking(PICKED_UP|IN_TRANSIT)──→ IN_TRANSIT  ✅ advanceTracking:168-175 仅从 DISPATCHED
IN_TRANSIT ──advanceTracking(DELIVERED)──→ DELIVERED  ✅ advanceTracking:156-167 + 幂等（已 DELIVERED 返回 false）
IN_TRANSIT ──部分签收──→ IN_TRANSIT（保持，等待剩余）❌ 未实现（见 P1-MA2-079）
DRAFT|ADVISED|DISPATCHED|IN_TRANSIT ──cancelShipment──→ CANCELLED  ✅ cancelShipment:123-148 守卫（拒 DELIVERED/CANCELLED）+ DISPATCHED+ 网关取消
DELIVERED ──?──→ (终态无出边)  ✅ cancelShipment 拒绝 DELIVERED 源态
CANCELLED ──?──→ (终态无出边)  ✅ cancelShipment 幂等（已 CANCELLED 返回）
```

⚠️ **P1-MA2-079 logistics 部分签收完全未实现**：owner doc §2 ASCII 图 + §4 异常路径表显式声明「部分签收 → 记录部分签收，状态保持 IN_TRANSIT（等待剩余）」，代码 `advanceTracking()` 仅处理完整 `TRACKING_EVENT_DELIVERED`（`ErpLogConstants.java:39`），**无 TRACKING_EVENT_PARTIAL 常量 + 无部分签收字段（receivedQuantity/partialSignedQty）+ 无部分签收记录路径**——grep 全 `module-logistics/erp-log-service/src/main` `partial|Partial|PARTIAL|部分签收` 零业务命中。按 finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-039~042 + inv P1-MA2-063 owner doc 契约漂移裁决范式 P1。**不破坏主路径**——完整签收 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 完整覆盖发运生命周期；部分签收是 owner doc Deferred 业务场景（承运商回调暂只发完整 DELIVERED 事件）。

**本维度新发现 2 项 P1（P1-MA2-077 aps 无守卫 / P1-MA2-079 logistics 部分签收未实现）。**

---

### 2.3 维度「终端状态和恢复」

**裁决：PASS（owner doc 与代码对齐；aps 终态不可恢复经 P1-MA2-077 守卫缺失间接破坏，归 2.2）」

#### aps FINISHED/CANCELLED 终态

✅ **owner doc §3 终态声明**：FINISHED/CANCELLED 不可直接恢复，需新建 OperationOrder；FINISHED 不可重排。`ErpApsSchedulingEngine` 排产入口 `loadPendingOrders:145-155` 仅查 `status=DRAFT`（`ErpApsSchedulingProcessor.java:147`）——**FINISHED/CANCELLED 工序经引擎路径不可重排** ✓。`insertRushOrder:78` 仅查 `status∈{PLANNED,IN_PROGRESS}`（:174-175）——**FINISHED/CANCELLED 不参与区间重排** ✓。

⚠️ **但 P1-MA2-077 start/complete/cancel 无守卫**——FINISHED 工序可经手工 `cancel` 动作降级 CANCELLED（违反终态不可恢复），归 2.2 裁决。

#### aps Schedule ARCHIVED 终态

✅ `archive` 守卫拒绝 ARCHIVED→任何迁移（archive 已 ARCHIVED 幂等返回；publish 仅 DRAFT 源态）。ARCHIVED 不可恢复 ✓。

#### logistics DELIVERED/CANCELLED 终态

✅ **owner doc §3 终态声明**：DELIVERED 后退货走 sales 域标准退货；CANCELLED 可新建发运单关联原出库单。
- `cancelShipment:126-129` 拒绝 DELIVERED 源态（DELIVERED 不可取消，须走 sales 退货）✓
- `cancelShipment:126-128` 幂等守卫（已 CANCELLED 返回）✓
- CANCELLED 后新建发运单——`advise:54-69` 接受 DRAFT 源态，新发运单 DRAFT 不受历史 CANCELLED 影响 ✓
- DELIVERED + freightSettlementStatus=SETTLED 后 `onDelivered:168-171` 抛 `ERR_LOG_SHIPMENT_ALREADY_DELIVERED` 幂等守卫 ✓

**归档与活跃区分**：`useLogicalDelete="true" deleteFlagProp="delVersion"`（aps/logistics ORM 全实体声明）承载废弃归档，与业务终态 CANCELLED 正交。

**本维度无新发现（终态语义 owner doc 与代码对齐；aps 终态被绕过的实质归 2.2 P1-MA2-077）。**

---

### 2.4 维度「异常路径」

**裁决：FAIL（logistics 网关异常重试耗尽 + DELIVERED 过账失败缺告警闭环 + aps 并发排产交接 A2.17）」

#### aps 异常路径

| 异常场景 | 实仓处理 | 裁决 |
|----------|---------|------|
| 工作中心无可用时段（NO_AVAILABLE_SLOT） | `ErpApsSchedulingEngine.scheduleForward:82-88 / scheduleBackward:131-136` 标记 conflict + 保持 DRAFT + SchedulingResult.addConflict | ✅ PASS |
| 后向排产交期不可达（DEADLINE_NOT_REACHABLE） | `scheduleBackward:139-145` 标记 conflict + setFeasible(false) + 保持 DRAFT | ✅ PASS |
| 后向排产无终点（NO_DEADLINE） | `scheduleBackward:121-126` 标记 conflict + 保持 DRAFT | ✅ PASS |
| 插单区间内有 IN_PROGRESS 工序 | `insertRushOrder:81-87` 抛 ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE（硬约束，不回退执行中工序） | ✅ PASS |
| **并发排产同一工作中心产能双倍占用** | `ErpApsSchedulingEngine` 纯内存算法，两并发 scheduleForward 调用读同一批 DRAFT + timeline 不互相可见 → 同 work center 时段双占 | ⚠️ **交接 A2.17**（owner doc §4 显式声明"乐观锁或资源锁防止产能双倍占用"未落地，见 §6 并发敏感点） |
| **PLANNED→DRAFT 重排未限定区间致全局重排（牛顿效应）** | `insertRushOrder:62-122` **窗口限定**：windowStart=earliestStartDateT，windowEnd=latestEndDateT+buffer，maxWindowDays=30 config 兜底；`loadPlannedInWindow:169-179` 仅查窗口内同 machineId 的 PLANNED/IN_PROGRESS；IN_PROGRESS 硬约束不回退；优先级比较（opPriority > rushPriority 才回退） | ✅ **PASS（证伪候选 P0）**——重排范围严格限定在窗口内同工作中心低优先级工序，无全局重排风险 |

#### logistics 异常路径

| 异常场景 | 实仓处理 | 裁决 |
|----------|---------|------|
| 网关下单超时/失败（5xx/408） | `completeShipment:101-116` 指数退避重试 maxRetries=3（`erp-log.gateway-max-retries`）+ intervals=30,120,600（`erp-log.retry-base-interval-secs`） | ✅ 重试机制 PASS |
| 网关重试耗尽 | `deadLetter:324-332` 保留 ADVISED + 写 remark + ErpLogShipmentLog（GATEWAY_RETRY_EXHAUSTED/GATEWAY_NON_RETRYABLE） | ⚠️ **P1-MA2-080**（缺告警闭环/TODO，见下） |
| 承运商拒接（4xx 不可重试） | `isRetryable:359-366` 仅 5xx/408 可重试，4xx 立即 deadLetter | ✅ PASS |
| webhook 签名无效 | `handleTrackingWebhook:109-114` ERR_LOG_WEBHOOK_SIGNATURE_INVALID（config-gated webhook-signature-required 默认 true） | ✅ PASS |
| webhook 运单未找到 | `handleTrackingWebhook:121-124` 返回 null（静默丢弃，防承运商误报） | ✅ PASS |
| **DELIVERED 运费过账失败悬挂** | `onDelivered:187-200` try/catch 吞异常 + LOG.warn/error + 保持 freightSettlementStatus=PENDING | ⚠️ **P1-MA2-080**（同型悬挂，见下） |
| **网关取消失败（DISPATCHED+ 取消）** | `cancelShipment:139-144` 调 client.cancelShipment 抛异常即整体回滚（@BizMutation 事务） | ✅ PASS |
| 追踪长时间无更新 | `scanForPolling:195-210` 轮询 DISPATCHED/IN_TRANSIT 推进 + DELIVERED 翻转补调 onDelivered | ✅ PASS（但 owner doc §4 "超预计送达 3 天标记追踪异常" 未落地——P2 watch-only） |
| **关联出库单取消发运后锁定未释放** | `cancelShipment` 仅 setStatus(CANCELLED) + saveShipment，**无显式释放出库单锁定动作**（relatedBillType/relatedBillCode 是弱指针非显式锁） | ✅ **PASS（物流侧角度）**——owner doc §1/§2 "关联出库单锁定/释放" 是概念性弱指针约束（销售侧检查"已有活跃发运单"），物流侧 CANCELLED 后允许新建发运单关联原出库单，无显式锁需释放。归 P2-MA2-072 owner doc drift watch-only |

⚠️ **P1-MA2-080 logistics 网关异常重试耗尽 + DELIVERED 过账失败缺告警闭环/TODO（合并裁决）**：

(a) **网关异常重试耗尽无告警闭环**：`GatewayDispatcher.deadLetter:324-332` 仅写 remark + ErpLogShipmentLog（错误日志），**不派发 `IErpSysNotificationBiz` 告警 + 无 4h 升级 cron job**——owner doc §8 显式声明「ADVISED 网关异常标记后 4 小时升级通知物流主管」未落地。grep 全 `module-logistics/erp-log-service/src/main` `IErpSysNotificationBiz|notify|escalat|4h|升级` 零业务命中。ADVISED+remark=错误 的发运单**静默悬挂**等待运营人工发现（依赖 LOG.error 运维扫日志）。与 b2b P1-MA2-073 + cs P2-MA2-067 同型 missing-automation。

(b) **DELIVERED 运费过账失败悬挂无自动恢复路径**：`ErpLogShipmentBizModel.onDelivered:187-200` try/catch 吞所有异常（NopException LOG.warn / 其他 LOG.error）返回，**不向上传播**——freightSettlementStatus 保持 PENDING + status=DELIVERED 终态。**关键差异**：`scanForPolling:195-210` 仅扫描 `status∈{DISPATCHED,IN_TRANSIT}`（:198-200），**不重试已 DELIVERED-PENDING 运单**——PENDING 悬挂**无自动恢复路径**（比 hr P1-MA2-048 + assets P1-MA2-060 + projects P1-MA2-068 peer dispatcher 更严重——那些至少有 `DeferredPostingSweepJob` 兜底；logistics 无对应 sweep）。仅手工 webhook 重发或运营手工调 onDelivered 可恢复。

**裁决 P1 非 P0（合并）**：(1) 失败模式需承运商网关故障/finance 过账引擎异常（基础设施故障/配置错误，非正常路径）；(2) `LOG.warn/error` 提供运维可见性；(3) deadLetter + ErpLogShipmentLog 提供审计轨迹；(4) 与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 MANUAL_POST + projects P1-MA2-068 + maintenance P1-MA2-074 **同型根因**（按既定裁决范式 P1）；(5) 业财不一致可经期末试算平衡人工发现。**aggravator**：logistics 无 DeferredPostingSweepJob 兜底（比 peer 严重），MR1 裁决时优先补 sweep 或扩展 scanForPolling 至 DELIVERED-PENDING 重试。

**本维度新发现 1 项 P1（P1-MA2-080 合并告警闭环）；1 处候选 P0 经证据证伪（aps PLANNED→DRAFT 重排范围严格窗口限定）；1 处交接 A2.17（aps 并发排产）。**

---

### 2.5 维度「可达性」

**裁决：PASS」

#### aps 可达性

- 从 DRAFT 可达所有状态：DRAFT（初始）→PLANNED（scheduleForward/Backward 成功）→IN_PROGRESS（start）→FINISHED（complete）✓；PLANNED|IN_PROGRESS→CANCELLED（cancel）✓；CANCELLED 也可从 DRAFT 直接 cancel ✓。
- **PLANNED→DRAFT 回退路径合法**（owner doc §5）：经 `insertRushOrder:101-106` 受影响区间低优先级工序回退 DRAFT ✓。仅 `ErpApsSchedulingProcessor.insertRushOrder` 可触发回退（@BizMutation 入口），非 PLANNED 自主动作 ✓。
- 所有状态至少一条入边 ✓；无不可达状态。
- 无死锁：终态 FINISHED/CANCELLED 经 owner doc §3 无出边（P1-MA2-077 守卫缺失不改变设计意图，归 2.2）。

#### logistics 可达性

- 从 DRAFT 可达所有状态：DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED ✓；DRAFT/ADVISED/DISPATCHED/IN_TRANSIT→CANCELLED ✓。
- ADVISED 重试耗尽死信保留 ADVISED（自循环可达，等待人工/重试恢复）✓。
- 所有状态至少一条入边 ✓；无不可达状态（部分签收保持 IN_TRANSIT 自循环，但 P1-MA2-079 未实现——无新出边）。
- 无死锁：DELIVERED 经 `onDelivered` PENDING→SETTLED 推进或保持 PENDING 等待手工恢复（P1-MA2-080 无自动 sweep 但手工可恢复）。

**本维度无新发现。**

---

### 2.6 维度「角色和权限」

**裁决：FAIL（aps 取消执行中工序 + logistics 取消在途 缺审批门控）」

#### aps 角色权限（owner doc §6）

| 迁移 | owner doc 执行角色 | 实仓实现 | 裁决 |
|------|-------------------|---------|------|
| DRAFT→PLANNED（自动排产） | 系统（APS 引擎） | `scheduleForward/Backward` @BizMutation 无角色校验（信任调用方权限层） | ✅ PASS（系统自动） |
| PLANNED→IN_PROGRESS | 车间调度员 | `start` @BizMutation 无角色校验 | ✅ PASS |
| IN_PROGRESS→FINISHED | 车间作业人员（报工） | `complete` @BizMutation 无角色校验 | ✅ PASS |
| PLANNED\|IN_PROGRESS→CANCELLED | **计划员/生产主管** | `cancel` @BizMutation **无审批流** | ⚠️ **P1-MA2-078** |
| PLANNED→DRAFT（重排） | APS 引擎/计划员 | `insertRushOrder` @BizMutation 无角色校验 | ✅ PASS |

#### logistics 角色权限（owner doc §6）

| 迁移 | owner doc 执行角色 | 实仓实现 | 裁决 |
|------|-------------------|---------|------|
| DRAFT→ADVISED | 发货员 | `advise` @BizMutation 无角色校验 | ✅ PASS |
| ADVISED→DISPATCHED | 网关（系统自动） | `completeShipment` @BizMutation 无角色校验 | ✅ PASS |
| ADVISED→CANCELLED | 发货员 | `cancelShipment` @BizMutation 无审批 | ✅ PASS（owner doc §6 仅"需确认承运商未开始处理"，网关侧已 cancel 确认） |
| DISPATCHED→IN_TRANSIT / IN_TRANSIT→DELIVERED | 网关回调/系统自动 | `advanceTracking` 无角色校验（系统自动） | ✅ PASS |
| DRAFT→CANCELLED | 发货员 | `cancelShipment` @BizMutation 无审批 | ✅ PASS |
| **IN_TRANSIT→CANCELLED（退货/取消在途）** | **发货员+物流主管审批** | `cancelShipment` @BizMutation **无审批流** | ⚠️ **P1-MA2-078** |

⚠️ **P1-MA2-078 aps 取消执行中工序 + logistics 取消在途 缺审批门控（合并裁决）**：

(a) **aps IN_PROGRESS→CANCELLED**：owner doc aps/state-machine.md §6 显式声明「取消执行中的工序：需生产主管审批，因已产生实际报工数据」+ §审查提示「执行中工序取消是否需要生产主管审批」。代码 `ErpApsOperationOrderBizModel.cancel:129-135` **无任何审批流或角色校验**——任意角色可取消 IN_PROGRESS 工序（已报工数据存在）。

(b) **logistics IN_TRANSIT→CANCELLED（退货场景）**：owner doc logistics/state-machine.md §6 显式声明「IN_TRANSIT→CANCELLED（退货/取消在途）：需物流主管审批，因涉及逆向物流和运费争议」+ §审查提示「货物退回 Return to Sender 的审批权限是否落实」。代码 `GatewayDispatcher.cancelShipment:123-148` **无任何审批流或角色校验**——任意角色可取消在途发运单触发逆向物流。

**裁决 P1 非 P0（合并）**：(1) 仍是手工 @BizMutation 动作，需运营主动触发（非自动流转）；(2) 取消后状态正确（CANCELLED 终态）；(3) aps 侧取消不直接破坏库存/GL（纯排产，副作用经下游 mfg 工单级联，归 mfg A2.6a 已 done 联动复核）；(4) logistics 侧网关 cancel 已落实（DISPATCHED+ 经 `client.cancelShipment` 防止承运商侧双发），仅缺内部审批流；(5) 按 owner doc 契约漂移裁决范式 P1（与 contract P1-MA2-072 NEGOTIATION→TERMINATED 缺路径 + finance P1-MA2-020 反结账 kill-switch 同型）。**不破坏主路径主终态**——危险操作缺少审批门控但操作本身业务正确。

**本维度新发现 1 项 P1（P1-MA2-078 合并取消审批）。**

---

### 2.7 维度「外部依赖」

**裁决：PASS（含跨域 Facade 合规确认 + 跨域只读异常路径经事务回滚覆盖）」

#### aps 外部依赖

| 外部场景 | 内部处理 | 裁决 |
|----------|---------|------|
| WorkOrder 下达触发创建 OperationOrder（main→aps） | 经 mfg `ErpMfgScheduleToJobCardProcessor:195-227` 弱参照 sourceScheduleId 关联，**aps 侧无反向依赖 mfg**（aps 不监听 WorkOrder 事件，OperationOrder 由 mfg/手工创建） | ✅ PASS（aps 是被动方） |
| WorkOrder 取消级联取消 OperationOrder | mfg 侧 `IErpMfgWorkOrderBiz` 调 aps cancel——**aps 侧无级联监听**（归 mfg A2.6a 联动状态机角度） | ✅ PASS（归 A2.6a） |
| ErpApsConstraint 维护约束（排产输入） | 同域 `constraintDao()` 直接读取（`ErpApsSchedulingProcessor.loadMaintenanceConstraints:157-165`） | ✅ PASS（同域） |
| 跨域只读 ErpInvReservation/ErpInvStockBalance/ErpMfgBom/ErpMfgBomOperation | `ErpApsAtpCtpServiceImpl` + `ErpApsSchedulingProcessor`（P1-MA1-022 已登记）| ✅ PASS（见下 MA1 复核） |
| CRP 负荷来源 SPI（mfg→aps 只读 PLANNED OperationOrder） | `ApsLoadSourceProvider.findScheduledSlots` 实现 `IErpApsLoadSourceProvider`（mfg-dao 接口），aps 侧只读导出 | ✅ PASS |

#### logistics 外部依赖

| 外部场景 | 内部处理 | 裁决 |
|----------|---------|------|
| sales 出库事件触发自动创建草稿 | owner doc §7 "sales 域发布出库事件，本域订阅生成发运单草稿"——**本期 logistics 侧未订阅**（grep `IErpSalDeliveryBiz|onSalesDelivery|subscribeDelivery` 零匹配），发运单经手工创建（owner doc §7 "用户手工创建发运单（主要渠道）"）。属 owner doc Deferred 集成 | ✅ PASS（Deferred，主要渠道是手工） |
| 承运商网关回调（异步追踪） | `handleTrackingWebhook` 端点 + HMAC 签名校验（config-gated）+ 幂等 advanceTracking | ✅ PASS |
| **DELIVERED→IErpFinVoucherBiz.post FREIGHT 过账（path-1）** | `ErpLogShipmentBizModel.onDelivered:186-200` **直接调用** `voucherBiz.post(event, context)` Facade（参 inventory InvPostingExecutor 范式，REQUIRES_NEW 跨域失败隔离）——非事件订阅模型（owner doc §7 实现裁决补注 plan 2026-07-04-1115-3 + 2026-07-11-2329-1 已登记） | ✅ PASS（Facade 合规） |
| **path-2 到岸成本 config-gated IErpInvLandedCostBiz.generateFreightLandedCost** | `handlePurchaseReceiptDelivered:213-247` config-gated（`erp-log.path2-landed-cost-auto-create` 默认 false）+ freightAmount>0 + 失败保 PENDING 可重试 | ✅ PASS（config-gated + 失败语义对齐 path-1） |
| 轮询兜底 scanForPolling | `scanForPolling:195-210` 扫 DISPATCHED/IN_TRANSIT + DELIVERED 翻转补调 onDelivered（与 webhook 一致，path-1 + path-2 均受益） | ✅ PASS |
| 跨域写 | grep 全 `module-logistics/erp-log-service/src/main` `daoFor(ErpFin|ErpInv).saveEntity\|daoFor(ErpFin|ErpInv).updateEntity` **零匹配**——logistics production 代码**无跨域 daoFor 写直写**，全部跨域写经 I*Biz Facade | ✅ PASS（关键合规点） |

**本维度无新发现。**

---

### 2.8 维度「TODO/任务策略」

**裁决：FAIL（logistics 网关异常重试耗尽无 TODO——P1-MA2-080 已登记）」

#### aps TODO 策略（owner doc §8）

| 状态 | owner doc TODO | 实仓 | 裁决 |
|------|---------------|------|------|
| DRAFT | 否 | 无 TODO（等待排产输入） | ✅ PASS |
| PLANNED | 是（pool 车间调度员） | 无显式 TODO 生成（owner doc Deferred "PLANNED 超 24h 未开工催办" 未实现） | ✅ PASS（Deferred） |
| IN_PROGRESS | 是（assigned 操作工） | `assignedToId` 字段承载（`app-erp-aps.orm.xml:76`），无自动 TODO | ✅ PASS |
| FINISHED/CANCELLED | 否 | — | ✅ PASS |

⚠️ owner doc §8 "PLANNED 超过计划开工时间 24h 未改为 IN_PROGRESS 时产生催办" 未实现——与 cs P2-MA2-067 同型 missing-automation，**不破坏主路径**（PLANNED 工序经车间调度员主动 pull，无催办不产生悬挂数据）。**P2 watch-only**（合并 P2-MA2-071 owner doc 章节缺失时一并注记）。

#### logistics TODO 策略（owner doc §8）

| 状态 | owner doc TODO | 实仓 | 裁决 |
|------|---------------|------|------|
| DRAFT | 是（assigned 发货员） | `assignedToId` 字段承载，无自动升级 TODO（owner doc "DRAFT 超 24h 升级" 未实现） | ✅ PASS（Deferred） |
| ADVISED | 是（assigned 系统等待接单 + **异常人工处理**） | **网关异常重试耗尽无 TODO 派发**（见 P1-MA2-080） | ⚠️ **P1-MA2-080** |
| DISPATCHED/IN_TRANSIT | 否（系统自动追踪） | — | ✅ PASS |
| DELIVERED/CANCELLED | 否 | — | ✅ PASS |

⚠️ **ADVISED 网关异常重试耗尽是否产 TODO**——**否**。`deadLetter` 仅写 remark + 日志，**不派发 `IErpSysNotificationBiz` TODO**。owner doc §8 "ADVISED 网关异常标记后 4 小时升级通知" 未实现。**这是 P1-MA2-080 (a) 的核心**——「期望有人行动但不产生待办的状态（案例静默下沉）」反模式（`state-machine-business-review-prompt.md` 反模式表「静默状态」）。归 P1-MA2-080。

**本维度无新发现（实质归 P1-MA2-080）。**

---

### 2.9 维度「场景演练（最重要）」

**裁决：PASS（11 个场景经证据覆盖；2 个失败场景归 P1）」

#### aps 场景演练

| 场景 | 演练 | 裁决 |
|------|------|------|
| (a) 前向排产 happy path | DRAFT 工序 → scheduleForward → 引擎 findFreeSlotForward → setStatus(PLANNED) + plannedStart/EndDateT 写回（`ErpApsSchedulingEngine.scheduleForward:73-97`）| ✅ PASS（TestErpApsSchedulingEngine + E2E aps-schedule 覆盖） |
| (b) 后向排产（产能不足告警） | DRAFT 工序无 latestEndDateT → NO_DEADLINE conflict / 推算开工早于 earliestStartDateT → DEADLINE_NOT_REACHABLE + setFeasible(false)（`scheduleBackward:121-145`）| ✅ PASS |
| (c) 插单区间重排 | 急单 priority=10 同 machineId 窗口重叠 → 背景工序 priority=50 > 10 回退 DRAFT → 窗口内 DRAFT 工序重排 → frozen PLANNED 作为已占区间（`insertRushOrder:62-122` + `seedFrozenPlanned:173-187`）| ✅ PASS（E2E aps-rush-order 覆盖） |
| (d) 并发排产同一工作中心 | 两并发 scheduleForward 读同一 DRAFT + timeline 不互见 → 双占 | ⚠️ **交接 A2.17**（§6 并发敏感点 #1） |
| (e) 取消执行中工序审批 | IN_PROGRESS 工序 → cancel → setStatus(CANCELLED) **无审批门控** | ⚠️ **P1-MA2-078** |

#### logistics 场景演练

| 场景 | 演练 | 裁决 |
|------|------|------|
| (f) 正常发运送达 | DRAFT→advise→ADVISED→completeShipment 网关 success→DISPATCHED→webhook IN_TRANSIT→IN_TRANSIT→webhook DELIVERED→DELIVERED + onDelivered voucherBiz.post FREIGHT + freightSettlementStatus=SETTLED（`ErpLogShipmentBizModel:79-201` + `GatewayDispatcher:54-177`）| ✅ PASS（E2E log-shipment + log-delivered-freight-posting 覆盖） |
| (g) 网关异常重试 3 次耗尽 + 人工干预 | completeShipment retry loop 3 次（intervals 30,120,600）全失败 → deadLetter 保留 ADVISED + remark + ErpLogShipmentLog（`completeShipment:101-119`）| ⚠️ **P1-MA2-080 (a)**（无告警/TODO 派发 + 无 4h 升级） |
| (h) 部分签收（保持 IN_TRANSIT） | advanceTracking 仅处理完整 DELIVERED，**无部分签收路径** | ⚠️ **P1-MA2-079** |
| (i) 货物退回审批（IN_TRANSIT→CANCELLED） | cancelShipment 守卫 IN_TRANSIT 源态 + 调 client.cancelShipment + setStatus(CANCELLED) **无审批门控** | ⚠️ **P1-MA2-078** |
| (j) 关联出库单取消发运锁定释放 | cancelShipment setStatus(CANCELLED)，relatedBill 弱指针无显式锁释放（物流侧角度 PASS） | ✅ PASS（P2-MA2-072 owner doc drift） |
| (k) DELIVERED 过账失败悬挂 | onDelivered try/catch 吞异常 + PENDING + scanForPolling 不重试已 DELIVERED | ⚠️ **P1-MA2-080 (b)** |

**本维度无新发现（场景演练覆盖 5 个失败场景归 P1-MA2-077~080 + A2.17）。**

---

### 2.10 维度「与设计文档一致性」

**裁决：FAIL（aps owner doc §6 审批 + logistics §2 部分签收 + §4 网关异常 + §6 取消审批 + §8 升级 漂移）」

#### aps owner doc 一致性

| owner doc 声明 | 实仓 | 裁决 |
|---------------|------|------|
| §2 迁移图 PLANNED→DRAFT 重排回退 | `insertRushOrder:101-106` 落地 + 区间窗口限定 | ✅ 一致 |
| §4 并发排产乐观锁/资源锁防止产能双倍占用 | **未落地**（纯内存算法无锁） | ⚠️ 交接 A2.17 |
| §4 重排范围限定（区间重排而非全局） | `insertRushOrder` windowStart/windowEnd + 优先级守卫 + IN_PROGRESS 硬约束 | ✅ 一致 |
| §6 取消执行中工序审批 | **未落地**（cancel 无审批） | ⚠️ P1-MA2-078 |
| §8 PLANNED 超 24h 催办 TODO | **未落地** | ⚠️ P2 watch-only |

#### logistics owner doc 一致性

| owner doc 声明 | 实仓 | 裁决 |
|---------------|------|------|
| §2 网关异常重试（最多 3 次指数退避） | `completeShipment:101-116` maxRetries=3 + intervals=30,120,600 落地 | ✅ 一致 |
| §2 部分签收（保持 IN_TRANSIT） | **未实现** | ⚠️ P1-MA2-079 |
| §4 网关异常人工干预 | deadLetter 落地 + **无 4h 升级告警** | ⚠️ P1-MA2-080 (a) |
| §4 追踪长时间无更新超 3 天标记 | `scanForPolling` 轮询推进 + **无超期标记/告警** | ⚠️ P2 watch-only |
| §6 取消在途审批 | **未落地**（cancelShipment 无审批） | ⚠️ P1-MA2-078 |
| §6 ADVISED→CANCELLED 需确认承运商未开始处理 | cancelShipment 调 client.cancelShipment 确认（DISPATCHED+）| ✅ 一致 |
| §7 DELIVERED FREIGHT 过账 Facade（实现裁决补注） | `onDelivered` 直接调 `IErpFinVoucherBiz.post` Facade 落地（plan 2026-07-04-1115-3 + 2026-07-11-2329-1 补注） | ✅ 一致 |
| §7 path-2 到岸成本 config-gated | `handlePurchaseReceiptDelivered` config-gated 默认 false 落地 | ✅ 一致 |
| §8 ADVISED 异常 4h 升级 + DRAFT 24h 升级 | **未实现** | ⚠️ P1-MA2-080 (a) + P2 watch-only |
| §1/§2 关联出库单"锁定/释放"显式锁机制 | **概念性弱指针**（relatedBillType/Code），无显式锁实体 | ⚠️ P2-MA2-072 |

**本维度无新发现（实质归 P1-MA2-077~080 + P2-MA2-071/072）。**

---

## 3. 已登记 finding 运行时影响复核（MA1 finding 状态机角度）

| Finding ID | 原描述 | 状态机角度复核 | 终态 |
|-----------|--------|--------------|------|
| `P1-MA1-022`（aps 扩展） | aps 跨域只读 daoFor（`ErpApsAtpCtpServiceImpl` ErpInvReservation/ErpInvStockBalance/ErpMfgBom/ErpMfgBomOperation + `ErpApsSchedulingProcessor` 仅同域 ErpAps*，**无跨域**——aps 排产 Processor 只读同域 maintenance constraint） | **状态机角度无升级**——跨域只读是 ATP/CTP 查询 + 排产约束输入的副作用，**不参与 OperationOrder 状态机迁移判定**（start/complete/cancel 不读跨域；scheduleForward/Backward/insertRushOrder 只读同域 status=DRAFT filter + maintenance constraint）。跨域只读异常最坏情况是 wrong planned dates 或 ATP 误判，**不产生非法状态迁移**。异常路径经 @BizMutation 事务回滚覆盖（scheduleForward/insertRushOrder 均 @BizMutation） | 维持 P1（仅治理缺陷） |

**MA1 finding 状态机角度无升级**（logistics 侧 MA1 A1.13 已确认 logistics 直接调 IErpFinVoucherBiz.post Facade 平台规范 + log 无跨域 daoFor 站点，本审计无新复核点）。

---

## 4. 并发敏感点（交接 A2.17）

> 本审计标注观察到的并发敏感点，不做系统性并发正确性裁决（归 A2.17）。

| # | 敏感点 | 实仓证据 | 风险 | 处置 |
|---|-------|---------|------|------|
| 1 | aps 并发排产同一工作中心产能双倍占用 | `ErpApsSchedulingEngine` 纯内存 POJO（:21-24），两并发 scheduleForward 调用读同一批 DRAFT 工序 + WorkCenterTimeline 不互相可见 → 同 work center 时段双占。owner doc §4 显式声明"乐观锁或资源锁防止产能双倍占用"**未落地** | 产能预留不变量破坏（两工序 plannedStart/End 重叠） | 交接 A2.17 |
| 2 | aps 并发 insertRushOrder 同一工作中心 | 同 #1（insertRushOrder 也调 newEngine 内存算法） | 同 #1 | 交接 A2.17 |
| 3 | aps 并发 Schedule publish/archive | `ErpApsSchedule.versionProp="version"`（`app-erp-aps.orm.xml:119`）乐观锁 | silent lost-update → detectable conflict（versionProp 降级） | 交接 A2.17（降级重要事实） |
| 4 | aps 并发 OperationOrder start/complete/cancel | `ErpApsOperationOrder.versionProp="version"`（`app-erp-aps.orm.xml:57`）乐观锁 | silent lost-update → detectable conflict | 交接 A2.17（降级重要事实） |
| 5 | logistics 并发更新同一发运单（advise/completeShipment/cancelShipment/advanceTracking） | `GatewayDispatcher` 经 `dao.getEntityById + dao.saveOrUpdateEntity`（:40 Javadoc 明示），`ErpLogShipment.versionProp="version"`（`app-erp-logistics.orm.xml:167`）乐观锁 | silent lost-update → detectable conflict（versionProp 降级；与全域 7+ 域状态机实体降级范式一致） | 交接 A2.17（降级重要事实） |
| 6 | logistics 并发 webhook + 轮询同一运单 | `advanceTracking` 幂等守卫（已 DELIVERED 返回 false）+ versionProp 乐观锁 | 幂等守卫 + 乐观锁双层兜底 | 交接 A2.17 |

**6 处并发敏感点交接 A2.17**，含 @Version 透明乐观锁降级重要事实——aps OperationOrder/Schedule + logistics Shipment/ShipmentLine/ShipmentParcel/ShipmentLog/Carrier/CarrierConfig 全部声明 `versionProp="version"`（grep `app-erp-aps.orm.xml` + `app-erp-logistics.orm.xml` 确认），silent lost-update → detectable conflict。

---

## 5. 发现汇总

### 5.1 P0 发现

**零 P0**（4 个候选 P0 经证据证伪或降级）：

| 候选 P0 | 证据/裁决 | 终态 |
|---------|----------|------|
| aps 并发排产产能双倍占用 | owner doc §4 显式 Deferred 至 A2.17 + 交接（§4 #1） | 交接 A2.17 |
| aps PLANNED→DRAFT 重排未限定区间致全局重排 | **证伪**——`insertRushOrder:62-122` 严格窗口限定（windowStart/windowEnd + 优先级 + IN_PROGRESS 硬约束 + maxWindowDays=30 兜底） | 证伪 |
| logistics DELIVERED 运费过账失败悬挂无告警闭环 | 同型根因（finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 + projects P1-MA2-068 + maintenance P1-MA2-074）按既定裁决 P1；失败模式需过账引擎异常 + LOG 可见性 | 降级 P1-MA2-080 (b) |
| logistics 网关异常重试耗尽无告警/TODO 致发运单悬挂 | 同 b2b P1-MA2-073 + cs P2-MA2-067 同型 missing-automation；deadLetter 日志可见 + 手工可恢复 | 降级 P1-MA2-080 (a) |
| logistics 关联出库单取消发运后锁定未释放致重复发运阻断 | **证伪（物流侧角度）**——relatedBillType/Code 是概念性弱指针非显式锁，物流侧 CANCELLED 后允许新建发运单关联原出库单；锁定/释放归 sales 侧（如有）| 证伪（归 P2-MA2-072 owner doc drift） |

### 5.2 P1 发现（4 项新 finding）

| Finding ID | 域 | 描述 | 目标 MR | 同型根因 |
|-----------|---|------|---------|---------|
| `P1-MA2-077` | aps | **OperationOrder start/complete/cancel 完全缺状态守卫**——start:108-117 if/else 死代码无 status 校验；complete:121-126 无 status 校验；cancel:129-135 无 status 校验。FINISHED→CANCELLED / FINISHED→IN_PROGRESS / CANCELLED→IN_PROGRESS 等非法迁移可达，违反 owner doc §2 迁移图 + §3 终态不可恢复。不破坏主路径（aps 纯内存算法无 GL/库存副作用 + 手工 @BizMutation + versionProp 乐观锁降级）。**与 qa P0-MA2-017 不同型**（aps 无强门控被绕过） | MR1 | finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-039~042 + inv P1-MA2-063 owner doc 契约漂移 |
| `P1-MA2-078` | aps+logistics | **aps 取消执行中工序（IN_PROGRESS→CANCELLED）+ logistics 取消在途（IN_TRANSIT→CANCELLED）缺审批门控**（合并）——owner doc 两域 §6 显式声明「需生产主管审批 / 需物流主管审批」（危险操作），代码 `ErpApsOperationOrderBizModel.cancel` + `GatewayDispatcher.cancelShipment` 均无任何审批流或角色校验，任意角色可执行。不破坏主路径（仍是手工动作 + 终态正确 + logistics 网关 cancel 已落实） | MR1 | contract P1-MA2-072 + finance P1-MA2-020 kill-switch owner doc 契约漂移 |
| `P1-MA2-079` | logistics | **部分签收完全未实现**——owner doc §2 ASCII 图 + §4 异常路径表显式声明「部分签收 → 记录部分签收，状态保持 IN_TRANSIT（等待剩余）」，代码 `advanceTracking()` 仅处理完整 DELIVERED 事件，无 TRACKING_EVENT_PARTIAL 常量 + 无部分签收字段 + 无部分签收记录路径（grep 零匹配）。不破坏主路径（完整签收链完整覆盖发运生命周期） | MR1 | finance P1-MA2-031 + mfg P1-MA2-035/036 + hr P1-MA2-039~042 + inv P1-MA2-063 owner doc 契约漂移 |
| `P1-MA2-080` | logistics | **网关异常重试耗尽 + DELIVERED 运费过账失败缺告警闭环/TODO**（合并）——(a) `deadLetter` 写 remark + ErpLogShipmentLog 但**不派发 IErpSysNotificationBiz 告警 + 无 4h 升级 cron job**（owner doc §8 未落地）；(b) `onDelivered` try/catch 吞异常保 PENDING **不派发告警 + scanForPolling 不重试 DELIVERED-PENDING**（无 DeferredPostingSweepJob 兜底，比 peer 严重）。不破坏主路径（失败模式需基础设施异常 + LOG 可见性 + 手工 webhook 重发可恢复） | MR1 | finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 MANUAL_POST + projects P1-MA2-068 + maintenance P1-MA2-074 tryPost 吞异常同型 + b2b P1-MA2-073 + cs P2-MA2-067 missing-automation 同型 |

### 5.3 P2 发现（2 项新 watch-only）

| Finding ID | 域 | 描述 | 处置 |
|-----------|---|------|------|
| `P2-MA2-071` | aps+logistics/docs | **state-machine.md 缺多状态承载实体/助手独立章节**——aps state-machine.md 仅含 OperationOrder 章节，无 Schedule 排产方案状态机 + 排产引擎 scheduleForward/scheduleBackward/insertRushOrder 区间重排独立章节；logistics state-machine.md 仅含 Shipment 章节，无 ErpLogShipmentLog 追踪日志 + 网关 SPI 重试/死信策略 + scanForPolling 轮询 + FREIGHT 过账 Facade 实现裁决独立章节。与全域 P2-MA2-053/056/059/062/063/065/070 同型（owner doc 缺独立章节） | watch-only，MR1 顺手——方案 A（推荐）各域 state-machine.md 新增缺独立章节；方案 B 交叉链接到各 owner doc |
| `P2-MA2-072` | logistics/docs | **关联出库单"锁定/释放"是概念性弱指针非显式锁**——owner doc §1/§2「关联出库单锁定（不允许重复发运）」「释放关联出库单锁定」描述暗示显式锁机制，实际 `relatedBillType`/`relatedBillCode` 是弱指针字段（无锁实体/锁字段/锁状态）。"不允许重复发运"经销售侧检查（如有）或运营流程约束保证，物流侧无显式锁需释放。与 sales P2-MA2-058（returnStatus/refundStatus 派生视图）同型 owner doc drift。无运行时影响（物流侧 CANCELLED 后允许新建发运单关联原出库单） | watch-only，MR1 顺手——owner doc §1/§2 标注「关联出库单锁定/释放是概念性弱指针，经销售侧或运营流程约束保证」 |

### 5.4 MA1/MA2 已登记 finding 复核汇总

| Finding ID | 报告 | 状态机角度 | 终态 |
|-----------|------|----------|------|
| `P1-MA1-022`（aps 扩展） | ma1-platform-conformance-bc-tier | 仅治理缺陷，异常路径经 @BizMutation 事务回滚覆盖 | 维持 P1 |
| logistics 跨域写 | ma1-platform-conformance-bc-tier（logistics ✅） | logistics production 代码无 daoFor 跨域写直写，全部经 I*Biz Facade（IErpFinVoucherBiz.post + IErpInvLandedCostBiz） | 维持 ✅ |

---

## 6. 控制点裁决汇总

| 控制点（plan baseline） | 裁决 | 证据 |
|-----------------------|------|------|
| 状态定义清晰性（aps PLANNED/IN_PROGRESS / logistics ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED） | ✅ PASS | §2.1 dict + 常量 1:1 对齐 |
| 转换完整性（aps OperationOrder 迁移 + PLANNED→DRAFT 重排 + insertRushOrder 区间重排 / logistics 网关异常重试幂等 + 部分签收） | ⚠️ FAIL | §2.2 P1-MA2-077（aps 无守卫）+ P1-MA2-079（logistics 部分签收未实现） |
| 终端与恢复（aps FINISHED 不可重排 / logistics DELIVERED 退货走 sales） | ✅ PASS | §2.3 终态语义对齐（aps 终态被绕过归 P1-MA2-077） |
| 异常路径（aps 并发排产 + 重排范围 / logistics 网关异常重试耗尽 + 过账悬挂 + 出库单锁定释放） | ⚠️ FAIL | §2.4 P1-MA2-080（告警闭环）；并发交接 A2.17；重排范围 + 出库单锁定 PASS |
| 可达性（aps PLANNED→DRAFT 回退） | ✅ PASS | §2.5 全状态可达 + 无死锁 |
| 角色权限（aps 取消执行中工序审批 / logistics 取消在途审批） | ⚠️ FAIL | §2.6 P1-MA2-078（缺审批门控） |
| 外部依赖（aps WorkOrder 级联 / logistics 网关回调 + FREIGHT 过账 Facade） | ✅ PASS | §2.7 跨域 Facade 全合规 |
| TODO 任务策略（logistics 网关异常重试耗尽是否产 TODO） | ⚠️ FAIL | §2.8 P1-MA2-080（不产 TODO，案例静默下沉） |
| 场景演练（11 个代表性场景） | ⚠️ PARTIAL | §2.9 6 场景 PASS + 5 场景失败归 P1 + A2.17 |
| 与设计文档一致性（aps §2/§4/§6 / logistics §2/§4/§6/§7/§8） | ⚠️ FAIL | §2.10 实质归 P1-MA2-077~080 + P2-MA2-071/072 |

---

## 7. 状态图与迁移矩阵

### 7.1 aps OperationOrder 5 态状态图（实仓）

```
                    ┌── scheduleForward/Backward ──→ PLANNED ──start(no guard)──→ IN_PROGRESS
                    │                                     │                            │
                    │                           insertRushOrder                      complete
                    │                           (区间重排回退)                       (no guard)
                    │                                     │                            │
                    │                                     ↓                            ↓
       (WorkOrder 创建)                               DRAFT                           FINISHED (终态)
                    │                                     ↑                            │
                    │                                     │                            │
                    │                                     └────────────────────────────┘
                    │                                       (P1-MA2-077: start/complete/cancel 无守卫，
                    │                                        FINISHED/CANCELLED 可被非法迁移)
                    │
                    └──cancel(no guard)──→ CANCELLED (终态)
                                            ↑
                                            │
                                  cancel(no guard) from PLANNED/IN_PROGRESS
```

### 7.2 aps Schedule 3 态状态图（实仓）

```
DRAFT ──publish(守卫)──→ PUBLISHED ──archive(守卫)──→ ARCHIVED (终态)
  │                                                  ↑
  └──────────────archive(守卫)───────────────────────┘
```

### 7.3 logistics Shipment 6 态状态图（实仓）

```
            advise(守卫+幂等)         completeShipment(守卫+重试3次+死信)
DRAFT ──────────────────→ ADVISED ───────────────────────────→ DISPATCHED
  │                         │                                       │
  │                         │ deadLetter(无告警 P1-MA2-080a)        │
  │                         │ (保留 ADVISED + remark + 日志)        │ advanceTracking
  │                         ↓                                       │ (PICKED_UP|IN_TRANSIT)
  │                       ADVISED (自循环等待恢复)                    ↓
  │                                                               IN_TRANSIT
  │                                                                  │
  │                                                                  │ advanceTracking(DELIVERED)+幂等
  │                                                                  │ + onDelivered(过账 PENDING→SETTLED)
  │                                                                  ↓
  │                                                              DELIVERED (终态)
  │                                                                  │
  │                                                       (PENDING 悬挂 P1-MA2-080b)
  │
  └──cancelShipment(守卫)──→ CANCELLED (终态)
                              ↑
                              │
                  cancelShipment from ADVISED/DISPATCHED/IN_TRANSIT
                  (IN_TRANSIT→CANCELLED 缺审批 P1-MA2-078)
                  (DISPATCHED+ 经 client.cancelShipment 网关取消)
```

**部分签收（IN_TRANSIT 自循环）**：owner doc §2 声明但代码未实现（P1-MA2-079）。

---

## 8. 剩余风险

1. **aps 并发排产产能双倍占用（交接 A2.17）**——内存算法 + 无锁，生产高并发场景需 A2.17 裁决（悲观锁/乐观锁/资源锁/串行化任一）。
2. **logistics 过账悬挂无 DeferredPostingSweepJob 兜底（P1-MA2-080 aggravator）**——比 peer dispatcher 更严重，MR1 裁决时优先补 sweep 或扩展 scanForPolling 至 DELIVERED-PENDING 重试。
3. **aps OperationOrder 守卫缺失（P1-MA2-077）**——FINISHED 工序可被手工 cancel/complete/start 致终态语义破坏，虽无 GL/库存副作用但破坏运营数据一致性（FINISHED 工序的报工数据 vs CANCELLED 状态矛盾）。
4. **危险操作缺审批（P1-MA2-078）**——aps 取消执行中工序 + logistics 取消在途在多角色运营环境需审计/合规审查，MR1 裁决时补审批流或 owner doc 标注「审批经流程外保证」。
5. **owner doc 多处 Deferred 未注记（P2-MA2-071/072）**——审查者/开发者期望与实现不一致，MR1 顺手补注。

---

## 9. 审查范围摘要

- **审查范围**：aps（OperationOrder 5 态 + Schedule 3 态 + 排产引擎 + 跨域只读 + CRP SPI）+ logistics（Shipment 6 态 + 网关 SPI + 轮询 + FREIGHT 过账 Facade + path-2 到岸成本），2 域全部状态承载实体 + 助手类。
- **可达性摘要**：全状态可达，无不可达状态，无死锁。
- **角色/权限摘要**：aps cancel + logistics cancelShipment 缺审批门控（P1-MA2-078），其余迁移信任调用方权限层。
- **外部依赖摘要**：跨域 Facade 全合规（IErpFinVoucherBiz.post + IErpInvLandedCostBiz + aps 跨域只读经 @BizMutation 事务回滚覆盖）。
- **裁决：FAIL**（4 项新 P1 + 2 项新 P2 watch-only；零 P0；6 处并发敏感点交接 A2.17）。
- **跳过的区域**：无（plan baseline 全部控制点覆盖）。

---

## 10. 矩阵更新

### 10.1 状态机正确性维度 aps/log 列推进

| 维度 | aps（前） | aps（后） | logistics（前） | logistics（后） |
|------|----------|----------|---------------|---------------|
| 状态机正确性 | ❓ | **⚠️P1(A2.15✅)** | ❓ | **⚠️P1(A2.15✅)** |

**aps/log 两域状态机核心契约（aps OperationOrder 5 态 + Schedule 3 态 + 排产引擎 + insertRushOrder 区间重排 + 跨域只读 + logistics Shipment 6 态 + 网关 SPI 重试 + 轮询兜底 + FREIGHT 过账 Facade + path-2 到岸成本 config-gated）经证据逐项确认；零 P0**（5 个候选 P0 经证据证伪或降级：aps 并发排产交接 A2.17 / aps 重排全局化证伪严格窗口限定 / logistics 过账悬挂降级 P1 同型根因 / logistics 网关异常告警降级 P1 missing-automation 同型 / logistics 出库单锁定释放证伪概念性弱指针）；**4 项新 P1**（P1-MA2-077 aps start/complete/cancel 缺守卫 / P1-MA2-078 aps+logistics cancel 缺审批 / P1-MA2-079 logistics 部分签收未实现 / P1-MA2-080 logistics 网关异常 + 过账失败缺告警闭环）；**2 项新 P2** watch-only（P2-MA2-071 owner doc 缺独立章节 / P2-MA2-072 出库单锁定概念性弱指针 owner doc drift）；1 项已登记 MA1 finding（P1-MA1-022 aps 跨域只读）运行时复核**无升级**；**6 处并发敏感点交接 A2.17** 含 @Version 透明乐观锁降级（aps/logistics 全部状态承载实体声明 versionProp）。

### 10.2 索引更新

- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`（状态机正确性 aps/log 列推进至 ⚠️P1(A2.15✅)）
- P1 索引：`docs/audits/arm-index.md §P1 发现汇总`（新增 P1-MA2-077~080 4 行 + §A2.15 新增项段落）
- P2 索引：`docs/audits/arm-index.md §P2 发现汇总`（新增 P2-MA2-071/072 2 行）
- 报告清单：`docs/audits/arm-index.md §报告清单`（新增本报告行）

---

> Verdict: **FAIL**（4 项 P1 阻塞 MR1 修复，2 项 P2 watch-only；零 P0；状态机核心契约经证据确认，候选 P0 经证伪或降级）
> 审计执行：2026-07-28
> Skill：`docs/skills/state-machine-business-review-prompt.md`
