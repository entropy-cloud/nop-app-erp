# ck-logistics — logistics（运输管理 TMS）实现代码检查报告

> 工作项：C8.1（logistics 分册）。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-logistics` 手写生产代码——erp-log-service 27 文件（`ErpLogShipmentBizModel` + 6 个 per-mutation Processor（advise/completeShipment/cancelShipment/handleTrackingWebhook/save/scanForPolling）+ `AbstractErpLogShipmentDeliveredProcessor` 运费过账/到岸成本编排基类、`GatewayDispatcher`（网关派发/重试/死信/追踪推进）、`LogisticsFreightProvider`（FREIGHT 过账 Provider）、`MockCarrierGatewayClientFactory` + 三层 SPI（Registry/Client/ClientFactory）、`ErpLogShipmentStateMachine`（10 边 Bean）、`ErpLogDeliveryBookingBizModel`（配送窗口预约引擎 RC-R1.84）、2 个 job（TrackingPoll RC-R1.38 / DraftEscalation RC-R1.37）、Configs/Constants/Errors、spi/model 10 个中立 DTO）+ 8 个 CRUD BizModel（6 个 stub）+ dao/api 层。跨文件核实：`module-logistics/model/app-erp-logistics.orm.xml`（8 实体 versionProp/UK 清单）、`app-service.beans.xml`（17 bean）、app-erp-all 2 个 job.yaml、finance `ErpFinVoucherBizModel.post`（REQUIRES_NEW）+ `ErpFinPostingProcessor.process`（幂等 null 语义）、sales `ErpSalOrderBizModel.updateDeliveryStatus` / `ErpSalDeliveryProcessor` rollup、`md.dao.AcctSchemaResolver`、notify seed（`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`）、测试 13 文件清单。owner docs：`docs/design/logistics/`（README/state-machine/delivery-window/carrier-integration/use-cases）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。erp-log-service 全部 27 文件通读（无抽样）+ orm/beans/job-yaml 接线核对 + 平台源码实证 2 处（`QueryBean.addOrderField(name, desc)` L433 实证第二参为 desc——本域 6 处全部正确；`ErpFinPostingProcessor.process` L139-143 幂等命中返回 null）+ arm-index logistics 相关条目（P1-RC-083/084/085/086/087、P2-RC-073、P1-MA2-092、P0-MA2-019、P1-MA2-080）逐条现状复核。
> 切片边界：`erp-log-web` AMIS 页面契约 drift 不深查（仅核对 posted 列消费）；`erp-log-api` 骨架与 `_gen/` 产物不查；测试代码仅用于行为语义交叉验证。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-log-001（D8）onDelivered 的 post 返回 null（幂等命中）分支不 markSettled——DELIVERED 运单运费结算永久悬挂 PENDING 且无告警（P1-CK-fin-003 logistics 传导站点）

- **控制点 A**：`app/erp/log/service/processor/AbstractErpLogShipmentDeliveredProcessor.java#onDelivered`（L120-124：`String voucherId = voucherBiz.post(event, context); if (voucherId != null) { gatewayDispatcher.saveShipment(markSettled(shipment)); }`——**post 返回 null 时既不 markSettled 也不进 catch 告警分支**，方法静默返回）
- **控制点 B**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#process`（L139-143：`if (alreadyPosted(event, event.getAcctSchemaId(), context)) { LOG.info("过账幂等命中（源单已过账），空操作..."); return null; }`——平台侧幂等命中 = null，无返回态区分）+ `ErpFinVoucherBizModel#post`（L74-78 `@Transactional(REQUIRES_NEW)`——**凭证在独立事务提交**，外层 webhook 事务回滚不影响已提交凭证）
- **证据**：触发序列（任务点名「P1-CK-fin-003 幂等传导面」确认成立）：① webhook DELIVERED → advanceTracking 落 DELIVERED → onDelivered → post 在 REQUIRES_NEW 内**已提交** FREIGHT 凭证 → 随后 `saveShipment(markSettled)` 或事务内任意后续步骤失败（乐观锁 version 冲突等）→ 外层 @BizMutation 回滚：运单回到 IN_TRANSIT、SETTLED 未落、**凭证孤立存在**；② 承运商重发 DELIVERED webhook（或轮询）→ advance 再次推进 DELIVERED → onDelivered → `SETTLED.equals(...)` 为 false（上次回滚）→ 继续 → post 幂等命中 **return null** → `if (voucherId != null)` 不成立 → 运单永久 PENDING。**该路径无异常、无告警**（dispatchFreightFailureAlert 仅在 catch 内），且 logistics 无 finance DeferredPostingSweepJob 覆盖（owner doc state-machine.md §8 自认「logistics 网关无 DeferredPostingSweepJob，告警是失败恢复的唯一闭环入口」）——悬挂完全静默。对照：`ErrpFinPostingProcessor` 注释明确「源业务单据的 posted 标志由域调用方在 post() 成功返回后自行置位」，而 null=已过账的成功形态被本域当作"未成功"丢弃。
- **问题**：D8 跨域返回值契约消费错误 + D2 失败告警盲区——同型 P1-CK-fin-003（ck-finance-posting.md，其 grep 的 8 个 dispatcher 同型站点不含 logistics——本条为 logistics 站点独立登记）。
- **建议修复方向**：post 返回 null（幂等命中=源单已过账）视为成功并 markSettled（与 finance 侧「幂等命中=补偿成功」语义对齐）；或随 P1-CK-fin-003 的引擎三态化修复联动。
- **arm-index 裁决**：同型登记（P1-CK-fin-003 家族，logistics 站点新增计数）。

### P1-CK-log-002（D4/D2，B1 族）G4 告警闭环的两个事件模板种子缺失——log.gateway-dead-letter 与 log.freight-posting-failure 经 notify 静默跳过，死信/过账失败悬挂不可感知

- **控制点 A**：`app/erp/log/service/gateway/GatewayDispatcher.java`（L65 `NOTIFY_EVENT_GATEWAY_DEAD_LETTER = "log.gateway-dead-letter"` + L395 `dispatchDeadLetterAlert(shipment, code, errMsg)`——网关重试耗尽死信的唯一告警出口）+ `AbstractErpLogShipmentDeliveredProcessor.java`（L49 `NOTIFY_EVENT_LOG_FREIGHT_POSTING_FAILURE = "log.freight-posting-failure"` + L135 `dispatchFreightFailureAlert`——运费过账失败的唯一告警出口）
- **控制点 B**：`module-notify/deploy/sql/mysql/_seed_erp-notify.sql`（全 25 个种子 notificationType 实证：含 `log.draft-escalation`（ID 7201，RC-R1.37 落地时同步 seed）但**无 `log.gateway-dead-letter`、无 `log.freight-posting-failure`**；oracle/postgresql 两份 seed 同型核对一致）+ `app/erp/notify/service/processor/ErpSysNotificationNotifyProcessor#notify`（L36-38 `template == null → LOG.warn("无 ACTIVE 模板，config-gated 静默跳过") → return emptyList`——notify 侧设计即静默跳过）
- **证据**：repo 全仓 `*.sql` grep（排除 target）两事件名零命中。owner doc state-machine.md §8 逐字：「**网关异常告警落地**（G4 错误传播分级，P1-MA2-080）……logistics 网关无 DeferredPostingSweepJob 覆盖（比 peer dispatcher 更严重），**告警是失败恢复的唯一闭环入口**」「DELIVERED-PENDING 运费过账悬挂同理（onDelivered 失败派发告警，期末前置检查兜底）」——两条链的告警派发调用了 notify，但模板缺失使其在部署库上**恒被静默跳过**（仅 notify 侧一行 WARN 日志）。P1-MA2-080 的修复（plan 2026-07-30-0341-2 Phase 3）落地了派发代码但未落地模板种子——修复不完整形态（同 cs.entitlement-expiry 先例）。
- **问题**：D4 告警接线断裂（B1 失败无告警通道的变体：告警通道存在但载体缺失）——死信发运单与运费悬挂 PENDING 均不可运营感知，违背 G4 修复目标。
- **建议修复方向**：三库 seed 补两行模板（对齐 log.draft-escalation 7201 范式：USER_LIST/ROLE 接收人 + ACTIVE 状态）；修复阶段可与 ck-notify 的模板覆盖对账 finding（P1-CK-notify-002）联动批量补齐。
- **arm-index 裁决**：新增（grep arm-index「gateway-dead-letter / freight-posting-failure 模板」零命中；P1-MA2-080 resolved 记录的是派发代码维度，未覆盖种子维度）。

### P2-CK-log-003（D5）webhook HMAC 密钥取承运商公开编码 carrier.getCode()——签名校验无真实认证强度，凭证基础设施（CarrierConfig.credentials/apiSecret）零消费

- **控制点**：`app/erp/log/service/processor/ErpLogShipmentHandleTrackingWebhookProcessor.java#resolveWebhookSecret`（L66-68：`return carrier.getCode();`——javadoc 自认「mock 测试可控；真实部署可扩展 credentials JSON 内 webhookSecret」）+ `verifySignature`（L70-88，机制本身正确：HmacSHA256 + hex + `MessageDigest.isEqual` 常量时间比较 + 默认必填 `DEFAULT_WEBHOOK_SIGNATURE_REQUIRED=true`）
- **证据**：`ErpLogCarrier.code` 是业务公开编码（URL/查询参数直接使用，webhook 入参即 carrierCode）；`ErpLogCarrierConfig` ORM 已有 `apiKey/apiSecret/credentials(jsonText)` 三列（orm L145-147）承载真实凭证（README「加密存储的凭证」），但 `resolveWebhookSecret` 从不读取——持有承运商编码的任何方都能计算合法签名，**验签退化为形式**。当前缓解：(a) handleTrackingWebhook 是 @BizMutation（GraphQL 面需登录态，未暴露匿名 REST——与 b2b P2-RC-066 REST 未实体化同形态）；(b) 真实承运商 HTTP 集成为 Non-Goal（mock only）。REST/webhook 端点落地时该缺口升级为 P0/P1 级伪造入口（可驱动 DELIVERED → 触发 FREIGHT 凭证 + sales 交付回写）。
- **问题**：D5 认证边界——安全控制存在但密钥源公开。
- **建议修复方向**：resolveWebhookSecret 改读 `ErpLogCarrierConfig.credentials` JSON 的 `webhookSecret`（config 缺失时回退 FAIL-CLOSE 拒绝而非回退 code）；owner doc 同步登记「真实承运商集成前的密钥契约」。
- **arm-index 裁决**：新增（grep「webhookSecret/验签密钥/resolveWebhookSecret」零命中；P1-RC-085 是轮询 job 维度）。

### P2-CK-log-004（D8）findShipmentByTrackingNo 不按 carrierId 收窄——跨承运商运单号碰撞时 A 承运商 webhook 推进 B 承运商运单并触发其运费过账

- **控制点**：`app/erp/log/service/gateway/GatewayDispatcher.java#findShipmentByTrackingNo`（L267-277：`q.addFilter(eq("trackingNo", trackingNo)); q.addOrderField("trackingNo", false); return dao.findFirstByQuery(q);`——**仅 trackingNo 过滤**）+ 调用方 `ErpLogShipmentHandleTrackingWebhookProcessor#handleTrackingWebhook`（L33 已解析 `carrier = findCarrierByCode(carrierCode)`，L52 却只用 trackingNo 定位运单，carrier 仅用于验签）
- **证据**：ORM `UK_LOG_SHIPMENT_TRACKING_CARRIER (trackingNo, carrierId, delVersion)`（orm L233）的键含 carrierId——**跨承运商同 trackingNo 合法共存**（UK 语义即按承运商区分运单号）；两承运商各自运单同号时，A 的 webhook（或轮询 `advanceTrackingViaPolling` 用 shipment 自己的 carrierId 取 client 但事件流经 webhook 路径时）会命中 B 的运单（findFirst 按 trackingNo 升序取第一条）→ 推进错误运单状态 → DELIVERED 触发错误运单的运费过账/交付回写。logistics README「承运商配置……一个承运商可有多套配置」多承运商并存是基线形态。
- **问题**：D8 查询边界缺失（webhook 上下文已有 carrierId 却不用于定位）。
- **建议修复方向**：findShipmentByTrackingNo 增加 carrierId 过滤重载，webhook 路径传 `carrier.getId()`；轮询路径保持按 shipment.carrierId 语义。
- **arm-index 裁决**：新增（P1-MA2-092 是 trackingNo UK 并发维度，已 resolved R1.28；查询收窄维度零命中）。

### P2-CK-log-005（D8）交付状态回写无数量 rollup——单一 Shipment DELIVERED 直接把销售订单 deliveryStatus 写成 DELIVERED，覆盖 sales 出库审核的 PARTIAL 语义

- **控制点 A**：`AbstractErpLogShipmentDeliveredProcessor#notifySalesDeliveryStatus`（L206-212：`ErpSalOrder order = salOrderBiz.get(delivery.getOrderId(), true, context); if (order == null || SALES_DELIVERY_STATUS_DELIVERED.equals(order.getDeliveryStatus())) return; salOrderBiz.updateDeliveryStatus(delivery.getOrderId(), DELIVERED, context);`——**只要订单当前不是 DELIVERED 就无条件写 DELIVERED**）
- **控制点 B**：sales 侧权威口径 `ErpSalDeliveryProcessor`（L280-309：按行数量聚合 `anyDelivered/allFullyDelivered` → DELIVERED/**PARTIAL**/UNDELIVERED 的 rollup——`updateDeliveryStatus` 本身是裸 setter（`ErpSalOrderBizModel` L348-360），rollup 逻辑在出库审核调用方）
- **证据**：RC-R1.85（P1-RC-087）owner doc state-machine.md §7 称「复用既有 IErpSalOrderBiz.updateDeliveryStatus（发货进度语义与 sales 出库审核 rollup 同字段）」——但该 Facade 仅 setter，logistics 绕过 rollup 直接写终值。多批次发运场景（订单 100 件，出库 40 件 approved → PARTIAL；该 40 件的 Shipment DELIVERED → 订单被写成 DELIVERED）后续 60 件的出库审核 rollup 会纠正回 PARTIAL，但窗口期内订单交付状态失真且交付看板/报表口径被污染（DELIVERED 后业务可能停止后续发运跟进）。
- **问题**：D8 跨域反写语义降级（终值直写 vs rollup）。
- **建议修复方向**：notifySalesDeliveryStatus 复用 sales rollup 逻辑（在 sales 侧暴露按 delivery 维度的重算 Facade），或仅当该 delivery 是订单唯一/末张有效出库单时才写 DELIVERED，否则写 PARTIAL。
- **arm-index 裁决**：新增（P1-RC-087 resolved RC-R1.85 记录的是回写链路落地维度，rollup 语义缺口未登记）。

### P2-CK-log-006（D2）completeShipment 重试循环仅 catch NopException——真实网关非 Nop RuntimeException 逃逸：无网关日志、无死信标记、无告警，事务直接回滚

- **控制点**：`app/erp/log/service/gateway/GatewayDispatcher.java#completeShipment`（L123-138：`for (int attempt = 0; attempt <= maxRetries; attempt++) { try { ...client.completeDeliveryOrder(request)... } catch (NopException e) { lastFailure = e; retryable = isRetryable(e); ... } }` + L140 `deadLetter(...)`——**catch 面仅 NopException**；MockClient 按 SPI 惯例抛 NopException（L74-84），但 `IErpLogCarrierGatewayClient` 接口无任何异常契约声明（javadoc 无 throws/异常类型约定），真实 HTTP 客户端（Non-Goal 但为 SPI 目标形态）抛 RuntimeException/IOException 包装属常态）
- **证据**：非 Nop 异常路径：client 抛 RuntimeException → 穿出循环 → 穿出 completeShipment → @BizMutation 回滚——**无 writeLog（无 ErpLogShipmentLog 记录）**、无死信 remark、无 dispatchDeadLetterAlert。owner doc state-machine.md §4 异常路径「网关下单超时/失败 → 自动重试 → 重试耗尽后……生成 ErpLogShipmentLog 记录错误 + 通知发货员」对非 Nop 异常形态整体落空。对照：`onDelivered`/`handlePurchaseReceiptDelivered` 均用 `catch (Exception e)`。
- **问题**：D2 异常闭环（catch 面过窄）——未来真实网关接入时故障不可观测。
- **建议修复方向**：catch (Exception e)，非 Nop 包装为 `ERR_LOG_GATEWAY_CALL_FAILED`（不设 httpStatus → 不可重试）后走统一重试/死信路径；SPI 接口 javadoc 补异常契约。
- **arm-index 裁决**：新增（grep「catch NopException 网关/重试 catch 面」零命中；P1-MA2-080 是告警派发维度）。

### P2-CK-log-007（D3/D6）爽约后「优先重新预约权」死机制——markMissed 后 findActiveByShipment 仍返回 MISSED 预约，重新 book 被 ERR_LOG_BOOKING_DUPLICATE 阻断；priorityScore 全域零消费

- **控制点 A**：`app/erp/log/service/entity/ErpLogDeliveryBookingBizModel.java#book`（L72-75：`if (findActiveByShipment(shipmentId, context) != null) { throw new NopException(ERR_LOG_BOOKING_DUPLICATE)... }`）+ `#findActiveByShipment`（L166-184：内存仅剔除 `BOOKING_STATUS_CANCELLED`——**MISSED 预约仍算"有效"**）
- **控制点 B**：`#markMissed`（L144-161：置 MISSED + missedFee + `priorityScore + 10`（BOOKING_MISSED_PRIORITY_SCORE_STEP））——grep 实证 `priorityScore` 生产代码**零消费**（仅 markMissed 写入 + api bean 搬运；无任何读取/排序/优先分配逻辑）
- **证据**：owner doc delivery-window.md 流程 2 步骤 4 逐字「客户获得**优先重新预约权**（priorityScore 提升）」；实现注记 D2 裁决的 successor 清单仅含「CONFIRMED 自动推进与 MISSED 自动扫描未自动化——未人工标记的过期预约停留 BOOKED 占位容量」——**未包含"重新预约被阻断"**。现状：爽约后同发运单再约必须先经 CRUD update_ 手工把预约改成 CANCELLED（pur-003 族裸 CRUD 面）或等发运单终态联动释放——「优先重新预约」语义完全无载体。
- **问题**：D3 预约状态机死端（MISSED 无出边到可重约状态）+ 虚拟优先级字段。
- **建议修复方向**：markMissed 时同步释放容量（currentBooked -1）并将旧预约置 CANCELLED（或 findActiveByShipment 剔除 MISSED + book 时把 MISSED 视为已释放），重约时携带 priorityScore 排序语义；owner doc 补裁决。
- **arm-index 裁决**：新增（grep「markMissed 重新预约/priorityScore」零命中；P1-RC-086 resolved RC-R1.84 记录的是预约引擎落地维度）。

### P2-CK-log-008（D6）窗口生效期校验以 today 为基准而非 bookedDate——预约日期在窗口失效期外仍可预约（与星期校验的 bookedDate 基准不一致）

- **控制点**：`ErpLogDeliveryBookingBizModel#validateWindowBookable`（L201-211：`LocalDate today = CoreMetrics.today(); ... boolean inEffect = (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to));`——**生效期判定用当天**）对照 L212-218（星期匹配判定用 `bookedDate.getDayOfWeek()`——同方法两判据基准不同）
- **证据**：owner doc delivery-window.md 实现注记：「`book`（**窗口有效期内**[isActive + effectiveFrom/effectiveTo] + 星期匹配 + 容量守卫）」——「窗口有效期内」的自然语义是**所约时段**在生效期内。反例：窗口 effectiveTo=2026-08-31，today=08-26，用户约 bookedDate=09-15（星期恰匹配）→ inEffect 按 today 判 true → 预约落库到窗口失效期之后，容量计数也计入已失效窗口。测试 `TestErpLogDeliveryBooking`「窗口过期」用例以 today 为基准构造（过期的窗口今天约不了——该方向正确），未覆盖 bookedDate 越界方向。
- **问题**：D6 日期边界（判据基准混用）。
- **建议修复方向**：inEffect 改用 `bookedDate != null ? bookedDate : today` 判定（与星期判据同基准）；补 bookedDate 越界测试。
- **arm-index 裁决**：新增（任务点名「日期边界」维度；grep「effectiveFrom bookedDate/窗口生效期」零命中）。

### P2-CK-log-009（D5，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——Shipment status/freightSettlementStatus/trackingNo、Carrier gatewayId/isActive、CarrierConfig credentials 等可经 update_ 直改

- **控制点**：`ErpLogShipmentBizModel`（守卫仅 `defaultPrepareSave` 的 trackingNo/relatedBill 重复检查——**defaultPrepareUpdate 未 override**：status/freightSettlementStatus/labelUrl/actualDeliveryDate/signedBy 可直改，DELIVERED→IN_TRANSIT 复活 + SETTLED→PENDING 粉饰绕过状态机 Bean 与 onDelivered 幂等守卫）+ `ErpLogCarrierBizModel`（gatewayId 直改可联动 P2-CK-log-003 密钥语义）+ `ErpLogCarrierConfigBizModel`（apiKey/apiSecret/credentials 明文直改）+ Line/Parcel/Log/Window 5 个 stub BizModel 无任何守卫
- **证据**：与全域命名 mutation 守卫体系（advise/completeShipment/cancelShipment 经 ErpLogShipmentStateMachine 10 边矩阵）形成旁路面。另注：`posted`/`status` 经 update_ 可写但 posted 无任何 writer 语义（见 011）。
- **问题**：同型 P1-CK-pur-003 / P2-CK-cs-012 / P2-CK-b2b-009 族——logistics 站点登记（不复用展开）。
- **建议修复方向**：defaultPrepareUpdate 守卫：status/freightSettlementStatus 变更仅允许经命名 mutation；CarrierConfig 凭证字段建议拒绝 CRUD 写入或强制掩码（TestErpLogCarrierConfigCredentialMasking 已有掩码读取面）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，logistics 站点新增计数）。

### P3-CK-log-010（D4）README 配置点 4 键零消费——erp-log.enabled / async-dispatch / log-retention-days / gateway-timeout-secs 声明后无任何 main 代码读取

- **控制点**：`docs/design/logistics/README.md §配置点`（6 键表中 4 键无消费）对照 grep 实证：`erp-log.enabled`、`erp-log.async-dispatch`、`erp-log.log-retention-days` 在 ErpLogConfigs **无常量声明**且 main 代码零命中；`CONFIG_GATEWAY_TIMEOUT_SECS`（ErpLogConfigs L9）声明后零读取（真实超时归 Non-Goal）。已消费键：gateway-max-retries / retry-base-interval-secs / tracking-poll-cron / shipment-settlement-mode / webhook-signature-required / sales-freight-expense-subject / draft-escalation-hours+cron / path2-landed-cost-auto-create / booking-missed-fee。
- **证据**：async-dispatch 维度复用 arm **P2-RC-073**（UC-LOG-02 async-dispatch 死 config——同步替代异步，watch-only todo 已立案）；其余 3 键（enabled 总开关/log-retention-days/gateway-timeout-secs）为新增计数。README 业务规则 1「承运商网关调用走异步（post-commit + nop-job），不阻塞主事务」与 `GatewayDispatcher.completeShipment` 同步内联 + `Thread.sleep`（L476-483，单次重试退避封顶 60s，默认 3 次重试最长约 12.5 分钟持有事务/连接）的漂移一并归 P2-RC-073 裁决。
- **问题**：D4 声明 vs 实现（同 b2b P2-CK-b2b-004 形态——enabled 总开关承诺落空）。
- **建议修复方向**：二选一：实现 erp-log.enabled 总门（mutation 入口拒绝）或从 README 配置表移除；log-retention-days 落一个清理 job 或移除声明。
- **arm-index 裁决**：部分复用（async-dispatch = P2-RC-073，不重复登记）；enabled/log-retention-days/gateway-timeout-secs 新增。

### P3-CK-log-011（D3/D10）杂项：posted 列零 writer 但列表页展示（恒 false）+ freightSettlementStatus 创建无 PENDING 初始化（NULL）+ not-found 误用非法迁移错误码 + parsePayload 无类型防御

- **控制点 A**：`module-logistics/erp-log-web/.../ErpLogShipment.view.xml`（L70 `<col id="posted" sortable="true"/>`）对照 grep `setPosted` 生产代码零命中——`ErpLogShipment.posted`（orm propId 41, defaultValue=false）永不写 true（状态机 Bean javadoc 自认「本实体无独立 setPosted writer，运费结算经 freightSettlementStatus 独立轴」），列表页「已过账」恒 false 展示误导用户；
- **控制点 B**：`freightSettlementStatus`（orm propId 14 无 mandatory/defaultValue，xmeta 无 defaultValue，defaultPrepareSave 不设）——CRUD 建单落 **NULL** 而非 PENDING：`onDelivered` 的 SETTLED 判等对 NULL 兼容（`SETTLED.equals(null)=false` 通过），但按 PENDING 过滤的查询/看板漏 NULL 行，README「运费结算状态：PENDING → SETTLED」初始态缺位；
- **控制点 C**：`GatewayDispatcher#loadShipment`（L296-299 not-found 抛 `ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION`——`ERR_LOG_SHIPMENT_NOT_FOUND`（Errors L65-66）定义未用，同 b2b P3-CK-b2b-012 族）+ `handleTrackingWebhook`（L53-55 shipment==null **静默 return null**，`ERR_LOG_SHIPMENT_NOT_FOUND_BY_TRACKING` 定义未用——调用方无法区分"未找到"与"签名外拒绝"）；
- **控制点 D**：`ErpLogShipmentHandleTrackingWebhookProcessor#parsePayload`（L91-93 `(Map<String, Object>) JsonTool.parseNonStrict(payload)` 无 instanceof 检查——payload 为 JSON 数组/标量时 ClassCastException、`"null"` 时 NPE，外部输入直达 500 而非友好错误码；`ERR_LOG_WEBHOOK_EVENT_UNSUPPORTED` 定义未用）。
- **arm-index 裁决**：新增（NOT_FOUND 误码维度同型 b2b-012 家族新站点；posted 列/NULL 初始态/parse 防御均零命中）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | logistics 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-fin-003`（post 幂等 null 传导面） | `AbstractErpLogShipmentDeliveredProcessor#onDelivered` `voucherId != null` 消费（L122）——finance 报告 grep 的 8 dispatcher 不含 logistics | **同型登记为 P1-CK-log-001**（logistics 站点；无 sweep job 兜底 + advanceTracking 幂等守卫阻断重试入口，悬挂比 fin 域更彻底） |
| `P1-CK-pur-003` 族（CRUD update 无守卫） | Shipment/Carrier/CarrierConfig + 5 stub | 同型登记为 P2-CK-log-009 |
| cron 键漂移家族（P3-CK-crm-014 / mfg-013 / inv-021 / sal-023 / qa-019 / hr-012 / cs-014） | `erp-log-tracking-poll.job.yaml`/`erp-log-draft-escalation.job.yaml` cronExpr 消费键与 bean 内层空值跳过键**同一键**（`erp-log.tracking-poll-cron` / `erp-log.draft-escalation-cron`） | **核查不成立**（单键模式，对齐 R1.35 修复形态；对照 aps 域两 job 为漂移形态，见 ck-aps P3-CK-aps-010） |
| `P2-RC-073`（async-dispatch 死 config） | `completeShipment` 仍同步内联 + Thread.sleep（现状一致） | 复用不展开（归 P3-CK-log-010 注记） |
| `P1-RC-083`（重复发运防护，resolved RC-R1.83） | defaultPrepareSave relatedBillType+relatedBillCode 非 CANCELLED 守卫在位（L87-102） | 修复在位验证 |
| `P1-RC-084`（DRAFT 24h 升级，resolved RC-R1.37） | ErpLogDraftEscalationJob + job.yaml + seed 7201 在位 | 修复在位验证 |
| `P1-RC-085`（轮询兜底，resolved RC-R1.38） | ErpLogTrackingPollJob + dead config 转活跃在位 | 修复在位验证 |
| `P1-RC-086`（预约引擎，resolved RC-R1.84） | book/releaseForShipment/markArrived/markMissed + UK_LOG_DELIVERY_BOOKING_SHIPMENT 在位 | 修复在位验证（MISSED 死端为新增维度 P2-CK-log-007） |
| `P1-RC-087`（交付回写，resolved RC-R1.85） | notifySalesDeliveryStatus @Nullable 容错 + try/catch 降级在位 | 修复在位验证（rollup 语义缺口为新增维度 P2-CK-log-005） |
| `P1-MA2-092`（trackingNo UK，resolved R1.28） | UK_LOG_SHIPMENT_TRACKING_CARRIER(trackingNo,carrierId,delVersion) + 前置校验 + SaveProcessor flush-catch 翻译在位 | 修复在位验证 |
| `P1-MA2-080`（G4 告警，resolved plan 2026-07-30-0341-2） | 死信/过账失败告警派发代码在位，**模板种子缺失** | 派发在位 + 种子缺口登记为 P1-CK-log-002 |
| orgId 隔离族（P2-CK-fin2-007 / mfg-007 / hr-003④ / b2b-003） | Shipment orgId 经表单录入（buildFreightPostingEvent 透传 shipment.getOrgId() → AcctSchemaResolver；orgId null 时 resolvePrimarySchemaId 返回 null 传 finance 处理）；booking.setOrgId(window.getOrgId()) 在位；WO 链路 N/A | 读侧无独立 filter 缺口形态；orgId=null 的过账行为归 finance 域（剩余风险注记，不登记） |
| `P0-CK-mfg-001`（固定幂等键吞增量） | webhook 无 findByFixedKey 幂等形态（防重 = advanceTracking 状态幂等 + versionProp + UK） | 核查不适用 |

## 验证为正确（显式排除，防误报）

- **webhook 幂等主路径正确**（任务点名「幂等守卫第二次 webhook 无重复凭证」）：`advanceTracking` 已 DELIVERED 短路返回 false（GatewayDispatcher L181-184）→ `advanced && DELIVERED` 才调 onDelivered（webhook Processor L59-61）→ 第二次同事件 webhook 不重复过账；`onDelivered` 顶部 SETTLED 守卫（L95-98）双保险；并发双 webhook 经 versionProp 乐观锁一方回滚。**唯一漏洞是 post-null 传导（P1-CK-log-001），非幂等设计本身**。
- **HMAC 机制实现正确**：HmacSHA256 + hex 编码 + `MessageDigest.isEqual` 常量时间比较（L83-84，防时序侧信道）+ 空签名/空 secret 拒绝 + 默认必填 true——密钥源问题（P2-CK-log-003）与机制问题分离。
- **状态机 Bean + 接线双层守卫完整**：`ErpLogShipmentStateMachine` 10 边矩阵与 owner doc §2 图一致（含 advanceToDelivered {ADVISED,DISPATCHED,IN_TRANSIT} Decision C 刻意收紧排除 DRAFT/CANCELLED）；终态 DELIVERED/CANCELLED 无出边；GatewayDispatcher 五个动作全部 assertCan + 领域码映射 + common 作 cause（契约 §7 范式）。
- **RC-R1.83/R1.84/R1.85/R1.87/R1.88... 修复在位**：见注记表（逐条代码锚点核对：relatedBill 守卫 L87-102 / booking 容量+星期+UK / 交付回写 @Nullable / draft-escalation job+seed / tracking-poll job）。
- **path-2 config-gated 分支语义正确**：`handlePurchaseReceiptDelivered`（L149-183）三分支与 owner doc 实现约定逐字一致（config off → publishDeliveredEvent 占位 + SETTLED；freightAmount ≤0/null → SETTLED + INFO；autoCreate on → generateFreightLandedCost 成功 SETTLED / 失败保持 PENDING 可重入）。
- **`addOrderField` 布尔参数 4 处全部正确**（平台源码实证 `QueryBean.java` L433-438 第二参为 desc）：findCarrierByCode `("code", false)` 升序确定性 ✓、findShipmentByTrackingNo `("trackingNo", false)` ✓（排序本身正确，过滤缺失归 P2-CK-log-004）。
- **LogisticsFreightProvider 借贷方向正确**：借销售费用-运费（6601 config 可覆写）/贷按 freightTerms 分流（PREPAID→1002 银行存款、COLLECT→2202 应付 + partnerId 维度仅 COLLECT 携带）；readDecimal null→ZERO 防御；amount 经 `domain="amount"` scale=4。
- **死信链路主干正确**：isRetryable 按 httpStatus ≥500/408 判定（4xx 不重试）、parseRetryIntervals 非法段回退 30s、sleepSilently 封顶 60s + 中断恢复、errorCode 超 100 截断、writeLog 落 ErpLogShipmentLog、dispatchDeadLetterAlert 通知失败降级不阻断。
- **D1 机械扫描零命中**：`System.currentTimeMillis`/`new Date()`/`LocalDateTime.now()`/`@Inject private`/`extends RuntimeException`/`printStackTrace`/字符串 `==` 比较——module-logistics 全部 main 代码 grep 零命中（时间全部 CoreMetrics，异常全部 NopException + ErpLogErrors 中文描述）。
- **beans.xml 17 bean 接线完整（D4）**：Registry（ioc:collect-beans by-type 聚合 SPI）+ MockFactory + Dispatcher + StateMachine + FreightProvider + 6 Processor + 2 job，无孤立声明；2 个 job.yaml invoker bean/method 对应正确。
- **job 双键单键模式**：两 job cronExpr 消费键 = bean 空值跳过键（非漂移家族，见注记表）；execute 均 try/catch 包裹 + LOG.error 接力。
- **booking 容量并发防护在位**：容量读-改-写经 `ErpLogDeliveryWindow.version` 乐观锁（updateEntity 冲突回滚）+ `UK_LOG_DELIVERY_BOOKING_SHIPMENT(shipmentId,delVersion)` 防并发双约；释放计数下限 0 守卫。
- **CONFIRMED 预约状态非死状态**：owner doc delivery-window.md D2 裁决显式「CONFIRMED = 预留确认态……本切片经通用 update 入口可达」——有裁决的可达性（B2 通过）。
- **cancelShipment 网关取消语义正确**：DISPATCHED/IN_TRANSIT 经 `client.cancelShipment(trackingNo ?: code)` 防承运商侧双发；失败异常上抛回滚（fail-closed，owner doc「异常 → 人工处理」由错误码承载）。

## arm-index 复用 or 新增裁决（汇总）

- **新增** 9 条：`post-null 不结算`（001，同型 fin-003 家族站点）/`告警模板种子缺失`（002）/`webhook 密钥=公开编码`（003）/`trackingNo 查询不收窄`（004）/`交付回写无 rollup`（005）/`重试 catch 面过窄`（006）/`爽约重约死端+priorityScore 零消费`（007）/`生效期判 today`（008）/`posted 列展示+NULL 初始态+误码+parse 防御`（011）+ 010 的 3 个新键。
- **同型登记**：P2-CK-log-009（pur-003 族）；P1-CK-log-001（fin-003 族传导站点）。
- **复用（注记不展开）**：P2-RC-073（async-dispatch 死 config）。
- **修复在位验证**：P1-RC-083/084/085/086/087、P1-MA2-092（R1.28）、P1-MA2-080（派发面）、P0-MA2-019 同族（trackingNo UK）。
- **同型核查不成立**：cron 键漂移家族（单键模式）、P0-CK-mfg-001（无固定幂等键形态）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 2 | P1-CK-log-001、P1-CK-log-002 |
| P2 | 7 | P2-CK-log-003..009 |
| P3 | 2 | P3-CK-log-010、P3-CK-log-011 |

按主维度：D8×4（001、004、005、009 归 D5 桶）、D4×2（002、010）、D5×2（003、009）、D2×1（006）、D3/D6×1（007 主 D3）、D6×1（008）、D3/D10×1（011）。（精确主维度归属：001 D8、002 D4、003 D5、004 D8、005 D8、006 D2、007 D3、008 D6、009 D5、010 D4、011 D3。）

同型/复用裁决：同型登记 2（001 fin-003 族、009 pur-003 族）+ arm 复用不登记 1（P2-RC-073）+ 修复在位验证 8 项 + 同型核查不适用 2 项（cron 漂移家族、mfg-001）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-log-service 27 文件全量通读 + 8 BizModel + orm（8 实体 versionProp/UK/index）+ beans/job-yaml 接线 + 平台源码 2 处实证（QueryBean.addOrderField、ErpFinPostingProcessor 幂等 null）+ finance post REQUIRES_NEW 事务边界 + sales updateDeliveryStatus/rollup 源码 + AcctSchemaResolver（orgId null → null 静默）+ notify seed 三库 + 测试 13 文件清单与 TestErpApsCapacityReservation 式关键断言抽查 + D1 机械扫描。
- **未深查**：`erp-log-web` AMIS 页面契约 drift（仅 posted 列一处）；`erp-log-api` 骨架；`_gen/` 产物；spi/model 10 个 DTO 逐字段（纯数据类）；MockCarrierGatewayClientFactory 的测试钩子线程安全（static volatile failureMode 为测试面）；E2E 浏览器层；多时区部署语义（全域 LocalDateTime 单时区假设——与 aps 域同注记）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-log-001** 的触发频率依赖「外层事务在 post（REQUIRES_NEW 已提交）之后失败回滚」的实际窗口（markSettled 乐观锁冲突/webhook 链后续异常）——建议修复阶段写集成测试复现：webhook → post 成功 → 强制 saveShipment 失败 → 重发 webhook → 断言 PENDING 悬挂 + 无告警。
  2. **P2-CK-log-002** 的种子缺失以 `_seed_erp-notify.sql` 为唯一 seed 真相源判定（repo 全仓 *.sql grep 零命中）；若存在本报告未覆盖的部署期 seed 通道（如环境专属初始化脚本），降级为 not-a-problem + 补对账。
  3. **P2-CK-log-005** 的实际影响面取决于业务是否使用「一订单多出库多次发运」形态（单出库单发运时 rollup 与直写等价）；建议修复阶段与 sales 域 owner 共同裁决回写口径。
  4. **P2-CK-log-008** 的「窗口有效期内」语义若被 owner 裁决为「管理侧当下有效即可」，降级为 not-a-problem（仅需统一两判据基准的注释）。
