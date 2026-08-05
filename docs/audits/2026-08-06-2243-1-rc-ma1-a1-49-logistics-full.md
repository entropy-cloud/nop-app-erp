# A1.49 logistics 全域需求-实现符合性审计报告

> Audit Status: closed
> Work Item: A1.49（MA1 需求追踪矩阵审计 — logistics 全功能 UC-LOG-01~07，7 UC）
> Mission: requirement-compliance
> Source Plan: `docs/plans/2026-08-06-2243-1-rc-ma1-a1-49-logistics-full.md`
> 方法论契约: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> 真相源层级: §4 — L1=`docs/design/logistics/use-cases.md`（权威功能契约）/ L2=logistics owner doc（state-machine.md/README.md/delivery-window.md/carrier-integration.md，设计参考非真相源）/ L3-L5 实仓代码与测试
> 审计性质: **只读审计**（无代码/ORM/api.xml/view.xml/真相源变更）；分歧记入报告不直改真相源（§9 冻结条款）

---

## §9 与 MA2 报告差异增量声明（首段，§去重协议）

本切片为 **logistics 域首份 RC 审计**。既有 MA2 状态机/业财链路行为审计报告 `2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`（A2.15 aps+logistics 合并）已证实的运行时行为（发运单 6 态状态机迁移守卫 + 网关重试 maxRetries=3 指数退避 + deadLetter 死信保留 ADVISED + scanForPolling 轮询兜底 + DELIVERED→`IErpFinVoucherBiz.post` Facade + path-2 到岸成本 config-gated + trackingNo UK[ref P1-MA2-092 resolved R1.28]）作为**既有证据输入**，本报告直接引用不重新核实行为本身（§去重协议 MA1↔MA2 第 1 条）。

**本报告只补"需求契约↔实际行为"差异**（MA1 视角 = use-case 验收标准视角；MA2 视角 = 状态机/链路行为视角）。差异增量集中在 MA2 未覆盖的需求契约维度：①UC-LOG-01 重复发运防护（relatedBillType+relatedBillCode 维度，非 trackingNo 维度）；②UC-LOG-01 DRAFT 24h 未确认升级通知；③UC-LOG-03 轮询调度未接线（cron 死 config）+ 超 3 天追踪异常标记；④UC-LOG-04 ShipmentDeliveredEvent 机制漂移（直接 Facade 调用替代事件订阅）；⑤UC-LOG-05 EncryptionHelper 凭证加密存储 stub + 连通性测试 action；⑥UC-LOG-07 配送窗口容量预约逻辑完全缺失；⑦UC-LOG-06 通知 sales 交付状态 + signatureImage/POD。**无 logistics 专属 MA2 报告除 A2.15 外**（logistics 与 aps 合并于 A2.15）。

---

## §1 需求契约原文（L1，逐字引用 `docs/design/logistics/use-cases.md`）

> L1 逐字引用验收标准原文（禁止转述，§1 L1 格式）。UC-LOG-06 在文件末尾 `---` 后归一化（0.2 清单化）。每个 UC 的验收标准逐条进入 §5 L5 判读。

### UC-LOG-01 发运单创建（`use-cases.md:5-15`）
- 正常流程：「1. 发货员选择关联出库单（`relatedBillType` + `relatedBillCode`）2. **系统校验该出库单尚未创建非 CANCELLED 发运单（重复发运防护）** 3. 发货员填写发运信息：承运商、收货地址、包裹信息 4. 系统自动生成发运单号（`code`），状态 DRAFT 5. 发货员可添加发运明细行（物料 + 数量）、包裹拆分」
- 异常路径：「重复发运：系统拒绝，提示"该出库单已存在有效发运单"；承运商未配置：提示"请先配置承运商"」
- 补充说明：「DRAFT 状态发运单可编辑、可删除（逻辑删除）。**超过 24 小时未确认的 DRAFT 发运单触发升级通知**。」

### UC-LOG-02 承运商派发（`use-cases.md:17-27`）
- 正常流程：「2. 状态迁移 DRAFT → ADVISED 3. **系统异步调用承运商网关 `adviseShipment()`（post-commit + nop-job）** 4. 网关返回成功 → 获取运单号（`trackingNo`）和面单 URL（`labelUrl`）5. 状态迁移 ADVISED → DISPATCHED 6. 生成网关日志（`ErpLogShipmentLog`）标记成功」
- 异常路径：「网关超时/失败：自动重试（最多 3 次，指数退避）；重试耗尽：保留 ADVISED，标记网关异常，通知发货员人工处理；承运商拒接：保留 ADVISED，通知更换承运商」
- 补充说明：「异步调用不阻塞主事务——发运单创建和状态迁移立即完成，网关调用在后台执行。」

### UC-LOG-03 追踪更新（`use-cases.md:29-39`）
- 正常流程：「2. 网关回调本系统暴露的追踪端点，或本系统定时轮询追踪接口 3. 系统解析追踪结果，更新 `ErpLogShipment` 的 `trackingNo` 关联信息 4. 如状态变为"已签收"：IN_TRANSIT → DELIVERED，记录 `actualDeliveryDate` 和 `signedBy` 5. 如状态仍在途：更新追踪信息，记录 `ErpLogShipmentLog` 6. DELIVERED 后触发运费过账流程」
- 异常路径：「追踪长时间无更新（超过预计送达日期 3 天）：系统标记"追踪异常"，通知物流主管；部分签收：状态保持 IN_TRANSIT，记录当前已签收信息；货物退回：根据退回原因决定进入 CANCELLED（需审批）」
- 补充说明：「追踪回调端点需认证和签名验证，防止伪造追踪数据。**定时轮询间隔可配置（默认 4 小时）**。」

### UC-LOG-04 运费过账（`use-cases.md:41-51`）
- 正常流程：「2. 系统判断关联单类型：`relatedBillType=SALES_DELIVERY` → 走销售运费 FREIGHT 凭证 / `relatedBillType=PURCHASE_RECEIPT` → 走采购到岸成本分摊（Landed Cost）3. **系统发布 `ShipmentDeliveredEvent`，finance 域订阅** 4. finance 域执行过账：生成凭证（销售）或触发到岸成本计算（采购）5. `freightSettlementStatus` 更新为 SETTLED」
- 异常路径：「运费未设置：发运单 DELIVERED 时 `freightAmount` 为空 → 人工补充运费后手动触发过账；过账失败：生成凭证异账 → 保留 PENDING 状态，人工修正后重试」
- 补充说明：「过账模式可配置：**AUTO_POST（自动）或 MANUAL_POST（人工确认后触发）**。采购运费作为到岸成本分摊到入库物料成本，详见 `costing-methods.md:287-309`。」

### UC-LOG-05 承运商集成（`use-cases.md:53-63`）
- 正常流程：「1. 管理员创建 `ErpLogCarrier`：填写承运商名称、选择 `gatewayId` 2. 管理员创建 `ErpLogCarrierConfig`：配置 API 端点、凭证、服务类型 3. **凭证输入时系统自动加密存储（`EncryptionHelper`），页面脱敏显示** 4. **管理员可进行连通性测试：系统调用 `IErpLogCarrierGatewayClient. adviseShipment()` 或 `trackShipment()` 验证配置** 5. 配置生效后，前端发运单可选择该承运商」
- 异常路径：「连通性测试失败：显示错误详情；同一 carrier 多配置时选择冲突：发运单需选择具体配置（`carrierConfigId`）」
- 补充说明：「新增承运商 = 1 个 `@Service` Factory bean + 对应 Client 实现，零改 commons/Registry。」

### UC-LOG-07 配送时间窗口管理（`use-cases.md:65-75`）
- 正常流程：「2. 系统展示该客户可用窗口（按星期过滤 + 容量检查）3. **选择时段后校验 `currentBooked < maxCapacity`** 4. **确认预约 → `currentBooked += 1`**，发运单关联预约记录 5. 配送执行后 → 预约状态更新为 ARRIVED/DELIVERED 6. 发运单取消/完成 → 释放预约 `currentBooked -= 1`」
- 异常路径：「容量不足：拒绝预约；爽约（MISSED）：**触发爽约费计算，`priorityScore` 提升获得优先重新预约权**；窗口过期：`effectiveTo` 到期后自动失效；重复预约：同一发运单不可重复预约（幂等校验）」
- 补充说明：「窗口定义维度：客户 × 星期 × 时间段。爽约费金额从系统参数配置读取。」

### UC-LOG-06 签收确认（`use-cases.md:79-89`，文件末尾 `---` 后归一化）
- 正常流程：「2. 系统通过以下任一渠道获取签收信息：承运商网关回调（自动）/ 承运商回传签收单/POD（人工上传）/ 发货员确认（线下得知后手动操作）3. **记录 `actualDeliveryDate`、`signedBy`、`signatureImage`（如支持）** 4. 状态迁移 IN_TRANSIT → DELIVERED 5. **如关联销售出库单：通知 sales 域更新订单交付状态** 6. 触发运费过账（UC-LOG-04）」
- 异常路径：「货物损坏/短少：客户拒签 → 承运商退回 → 走 IN_TRANSIT→CANCELLED（退货审批）；客户仅签收部分货物：记录当前签收数量，状态保持 IN_TRANSIT；签收信息不一致（系统 vs 实际）：发货员人工修正签收记录」
- 补充说明：「POD（交付证明）作为附件存储在 `ErpLogShipment` 的 `labelUrl` 扩展字段或附件关联。签收时间作为运费过账基准时间。」

---

## §2 实现证据（L3 代码路径，`file#method` 方法锚点 + 关键行为断言）

> 代码锚点 `rg "<method>" <file>` 可定位；行号为写时实测导航（2026-08-06），漂移不构成引用失效（§1 引用锚点纪律）。

| 组件 | 锚点 | 关键行为断言 |
|------|------|-------------|
| 发运单聚合根 | `ErpLogShipmentBizModel.java`（130 行，`@BizModel` L39） | 6 `@BizMutation`（save:93 / advise:99 / completeShipment:105 / cancelShipment:111 / handleTrackingWebhook:117 / scanForPolling:126）单行委托各 per-mutation Processor（R6.7）；`defaultPrepareSave:60` 自动 businessDate + trackingNo+carrierId 重复前置校验（:68-81）+ DB UK `UK_LOG_SHIPMENT_TRACKING_CARRIER` flush-catch 兜底（R1.28） |
| 网关编排 Facade | `gateway/GatewayDispatcher.java`（431 行） | `advise:62` DRAFT→ADVISED 守卫+幂等；`completeShipment:84` ADVISED→DISPATCHED 经 `client.completeDeliveryOrder` + 重试循环（maxRetries config 默认 3 + `parseRetryIntervals` 默认 30,120,600 指数退避 + `isRetryable` 5xx/408）+ 失败 `deadLetter:332`（保留 ADVISED + remark + `dispatchDeadLetterAlert:346` 派发 `IErpSysNotificationBiz`）；`advanceTracking:162` DELIVERED 设 `actualDeliveryDate`(L169)+`signedBy`(L171)；`scanForPolling:203` 查 DISPATCHED/IN_TRANSIT(limit 100) 调 `trackShipment`；`cancelShipment:131` DISPATCHED+ 经 `client.cancelShipment` 防双发 |
| DELIVERED 过账基类 | `processor/AbstractErpLogShipmentDeliveredProcessor.java`（231 行，abstract 非 bean） | `onDelivered:75` 按 `relatedBillType` 分流（:80-85 PURCHASE_RECEIPT→`handlePurchaseReceiptDelivered:124` / 默认 SALES path-1）+ path-1 `CONFIG_SHIPMENT_SETTLEMENT_MODE` AUTO/MANUAL 门控（:88-93）+ `voucherBiz.post(buildFreightPostingEvent)` 直接调用（:96，非事件订阅）+ 成功 `markSettled:216` SETTLED + 失败 catch 保 PENDING + `dispatchFreightFailureAlert:161`（R1.16）；`buildFreightPostingEvent:181` businessType=FREIGHT；path-2 config-gated `erp-log.path2-landed-cost-auto-create`（默认 false）调 `landedCostBiz.generateFreightLandedCost` |
| webhook Processor | `processor/ErpLogShipmentHandleTrackingWebhookProcessor.java`（98 行） | `handleTrackingWebhook:31` HMAC `verifySignature:70`（HmacSHA256，config `erp-log.webhook-signature-required` 默认 true）+ parsePayload + `advanceTracking` + `writeWebhookLog` + DELIVERED 调 `onDelivered` |
| 轮询 Processor | `processor/ErpLogShipmentScanForPollingProcessor.java`（34 行） | `scanForPolling:19` 调 `gatewayDispatcher.scanForPolling` + 对 DELIVERED 运单调 `onDelivered`（失败容忍保 PENDING） |
| 承运商/配置/窗口 BizModel | `ErpLogCarrierBizModel`/`ErpLogCarrierConfigBizModel`/`ErpLogDeliveryWindowBizModel`（各 17 行） | **纯 CrudBizModel 空壳**（仅构造器，无自定义 @BizMutation/@BizQuery/@BizAction 方法） |
| 网关 SPI 三层 | `spi/IErpLogCarrierGatewayClient`+`spi/IErpLogCarrierGatewayClientFactory`+`spi/ErpLogCarrierGatewayRegistry`（91 行）+ `spi/mock/MockCarrierGatewayClientFactory`（147 行） | Registry `getClient:60` carrier→gatewayId→factory→newClient；Mock Factory `getGatewayId="mock"` + 内部 MockClient（trackShipment 确定性 IN_TRANSIT→DELIVERED） |
| 运费 Provider | `posting/LogisticsFreightProvider.java`（115 行） | `implements IErpFinAcctDocProvider`，`getSupportedBusinessTypes={FREIGHT}`，`createFacts` path-1 SALES_DELIVERY Dr 销售运费/Cr 存货 |
| 事件 POJO | `event/ShipmentDeliveredEvent.java`（68 行） | POJO 类（非 bean），javadoc:12 显式「事件保持不派发（无事件总线），仅作为结构化交接意图日志记录」；`publishDeliveredEvent`（基类 L221）仅 LOG.info 不派发 |
| 配置键 | `ErpLogConfigs.java`（34 行） | gateway-max-retries(默认 3)/retry-base-interval-secs(默认 30,120,600)/tracking-poll-cron(默认 `0 0 */4 * * ?`)/shipment-settlement-mode(AUTO/MANUAL)/webhook-signature-required(默认 true)/path2-landed-cost-auto-create(ErpLogConstants:53 默认 false) |

**关键 grep 站点（§3 候选缺口核验）**：
- 重复发运防护（relatedBillType+relatedBillCode）：`rg "relatedBillType|relatedBillCode" erp-log-service/src/main` 仅命中常量定义+事件 POJO+分流读侧，**零重复校验逻辑**；ORM `app-erp-logistics.orm.xml:223-226` unique-keys 仅 `UK_LOG_SHIPMENT_CODE_ORG` + `UK_LOG_SHIPMENT_TRACKING_CARRIER`，**无 (relatedBillType,relatedBillCode) UK**。
- 24h 升级通知：`rg "escalation|24" erp-log-service/src/main` **零业务命中**。
- 轮询调度接线：`rg "IScheduler|QuartzJob|job.yaml|batch.xml|IJobInvoker|@Scheduled" module-logistics` **零命中**（仅 Dockerfile + config 定义）；`erp-log.tracking-poll-cron` 仅常量+IErpLogShipmentBiz javadoc 引用，**无 scheduler 消费**；`erp-log.async-dispatch`（README 配置点）**生产代码零消费**。
- 追踪异常 3 天：`rg "追踪异常|tracking.*exception|overdue.*notify|3.*天" erp-log-service/src/main` **零业务命中**。
- EncryptionHelper：`rg "EncryptionHelper" module-logistics` 仅 `IErpLogCarrierGatewayClientFactory:5` javadoc + `MockCarrierGatewayClientFactory:60` 注释，**零 import 零调用**。
- 配送窗口容量：`rg "maxCapacity|currentBooked|priorityScore|noShow|MISSED|capacity" erp-log-service/src/main` **零业务命中**（仅 DDL `erp_log_delivery_window` 列）；`ErpLogDeliveryBooking` 实体 ORM **不存在**（delivery-window.md:61 标"预留"）。
- signatureImage/POD：`rg "signatureImage|POD" module-logistics` **零命中**。
- 通知 sales：`rg "IErpSalDeliveryBiz|IErpSalOrderBiz|sales.*notify|通知 sales" erp-log-service/src/main` **零命中**。
- 部分签收：`rg "partial|Partial" erp-log-service/src/main` **零业务命中**（P1-MA2-079 控制点）。

---

## §3 测试证据（L4 测试断言引用 + 断言强度）

| 测试 | 方法 | 覆盖 UC | 断言强度 |
|------|------|--------|---------|
| `TestErpLogShipmentGateway` | `testFullStateMachineFlow:68` | UC-LOG-02 | **强断言**（DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 全迁移 + trackingNo/labelUrl 回写 + status 行级断言） |
| `TestErpLogShipmentGateway` | `testGateway5xxRetryDeadLetter:102` / `testGateway4xxNoRetryDeadLetter:122` | UC-LOG-02 异常 | **强断言**（5xx 重试 3 次后死信保留 ADVISED + remark 错误 / 4xx 不重试死信） |
| `TestErpLogShipmentGateway` | `testWebhookInvalidSignatureRejected:138` / `testWebhookIdempotentDuplicate:158` | UC-LOG-03 | **强断言**（HMAC 签名失败拒绝 + 重复 webhook 幂等） |
| `TestErpLogShipmentGateway` | `testCancelShipment:181` | UC-LOG-03/06 | **强断言**（DISPATCHED cancel 经 client.cancelShipment 防双发） |
| `TestErpLogCarrierGatewayIntegration` | `testPollingAdvancesMultipleShipments:50` / `testWebhookInTransitThenDelivered:68` | UC-LOG-03 | **强断言**（轮询推进多运单 + webhook IN_TRANSIT→DELIVERED） |
| `TestErpLogFreightPosting` | `testSalesFreightPostedAndSettled:64` / `testDuplicateDeliveredIdempotentThrows:89` / `testPurchaseReceiptNoVoucher:111` | UC-LOG-04 | **强断言**（SALES 运费凭证生成 + SETTLED + 重复 DELIVERED 幂等抛错 + PURCHASE_RECEIPT 无 FREIGHT 凭证） |
| `TestErpLogPath2LandedCost` | `testPath2AutoCreateLandedCost:78` / `testPath2SkipWhenFreightAmountNull:115` / `testPath2IdempotentRejectsDuplicateDelivered:139` | UC-LOG-04 path-2 | **强断言**（config 开启自动建 DRAFT 到岸成本 + freightAmount≤0 跳过 + 重复幂等拒绝） |
| `TestErpLogShipmentPostingEnd` | `testSalesDeliveryFullLifecycleWithFreightVoucher:62` / `testPurchaseReceiptFullLifecycleNoFreightVoucher:93` | UC-LOG-04 端到端 | **强断言**（全生命周期 + FREIGHT 凭证借贷行级断言） |
| `TestErpLogShipmentTrackingNoUk` | `testDuplicateTrackingNoRejected:43` | UC-LOG-01/03 | **强断言**（trackingNo+carrierId 重复拒绝，R1.28） |
| `TestErpLogShipmentCrudSmoke` | `testCreateHead:40` 等 5 方法 | UC-LOG-01 | **仅冒烟**（CRUD 创建/查询/更新/删除/明细关系，无重复发运/24h 升级断言） |
| `TestErpLogCarrierConfigCredentialMasking` | `testCredentialsNotExposedViaFindPage:37` | UC-LOG-05 | **强断言**（凭证不暴露于 findPage——脱敏显示层验证） |
| `TestLogPostingFaultInjection` | `testFreightPostingFailureDispatchesAlert:39` | UC-LOG-04 异常 | **强断言**（过账失败派发告警，纯单测无 IoC） |

**断言缺口**：UC-LOG-01 重复发运防护（relatedBill 维度）零断言；UC-LOG-01 24h 升级通知零断言；UC-LOG-03 追踪异常 3 天零断言；UC-LOG-03 轮询调度接线零断言；UC-LOG-07 容量/爽约/priority/幂等零断言（实体空壳）；UC-LOG-06 signatureImage/POD/sales 通知零断言。MA5 未对 logistics 域单独评级（logistics CRUD smoke 归 P1-MA5-012 残余风险 5 域之一，未 seed 故空数据是正确态）。

---

## §4 运行时行为证据（L5）

| 来源 | 证据 |
|------|------|
| MA2 A2.15（既有证据，§去重协议直接引用） | 发运单 6 态状态机迁移守卫齐全（advise/completeShipment/cancelShipment/advanceTracking 全 src 守卫 + 幂等守卫）；网关重试 maxRetries=3 指数退避 + deadLetter 死信保留 ADVISED；scanForPolling 轮询兜底；DELIVERED→`IErpFinVoucherBiz.post` Facade；path-2 到岸成本 config-gated；trackingNo UK（P1-MA2-092 resolved R1.28）|
| L4 测试（本切片） | UC-LOG-02 状态机全流程 + 网关重试死信 + webhook 签名幂等；UC-LOG-04 运费分流凭证 + path-2 到岸成本 + 过账失败告警——均强断言通过 |
| E2E | `tests/e2e/business-actions/log-path2-landed-cost-auto-create.action.spec.ts`（2 用例正路径 DRAFT 头+行字段精确数值断言 + freightAmount=0 边界）承接 path-2 后端落地 |

**L5 存疑点**（交 §7 静态存疑点清单 → MA4 展开）：①重复发运防护缺失是否致重复运费过账（需运行时双 shipment 同出库单 DELIVERED 探针）；②轮询调度缺失的运行时影响（manual-only 轮询下 IN_TRANSIT 长期不推进）；③UC-LOG-07 容量超卖运行时影响。

---

## §5 符合性结论（五级追踪矩阵 + 每 UC 结论，§2 判据取最高）

> 每 UC 一段；结论列明 §2 命中判据编号 + 三源对照 + §4 三判据复核 + 触及保护区域标注。新 finding 见 §6（P1-RC-083~087 / P2-RC-073~078）。

### UC-LOG-01 发运单创建 — **P1**（接受 on 主路径 + 2 新 P1）
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| 选关联出库单(relatedBillType+relatedBillCode) | :12 | 字段存在（ORM:174-175 tagSet=var） | CRUD smoke | ✅ | 接受 |
| **重复发运防护**（校验出库单无有效发运单） | :12 | ❌ 零校验逻辑 + 无 DB UK | ❌ 零断言 | ❌ | **P1-RC-083** |
| 填承运商/收货地址/包裹 | :12 | defaultPrepareSave + 字段 | CRUD smoke | ✅ | 接受 |
| 自动生成 code, DRAFT | :12 | 平台 CrudBizModel code gen + 默认 status | testCreateHead | ✅ | 接受 |
| 明细行/包裹拆分 | :12 | ErpLogShipmentLine/Parcel 实体 | testLineRelation | ✅ | 接受 |
| 异常:重复发运拒绝 | :14 | ❌（同上） | ❌ | ❌ | **并入 P1-RC-083** |
| 异常:承运商未配置提示 | :14 | ⚠️ 校验延迟到网关调用时（gatewayRegistry.getClient 抛 ERR） | — | 行为等价 | 接受（P2 倾向，验证延迟到 advise） |
| 补充:DRAFT 可编辑/逻辑删除 | :15 | delFlag 平台机制 | testDeleteHead | ✅ | 接受 |
| **补充:24h 未确认升级通知** | :15 | ❌ 零实现 + 无 config key | ❌ 零断言 | ❌ | **P1-RC-084** |

**结论**：UC-LOG-01 = 接受 on 选关联出库单+code 自动生成+明细/包裹+DRAFT 可编辑/逻辑删除主路径，**P1**（重复发运防护完全缺失[P1-RC-083 §2 P1① 功能实质偏离验收标准] + 24h 升级通知完全缺失[P1-RC-084 §2 P1② 异常路径/自动化未实现]）。§4 三判据：owner doc state-machine.md:124 + README §业务规则 §7 均为活跃要求（重复发运防护 + 24h 升级）**无 Deferred 标注**，git log 全 AI commits 无人工批准痕迹 → 三判据均不成立 → Q4=(a) 强制实现。**触及区域**：P1-RC-083 修复若加 (relatedBillType,relatedBillCode,status) DB UK **须 ORM ask-first + 独立 plan-audit §5 ORM 类**；P1-RC-084 纯调度接线+notify 预授权不触 ask-first。

### UC-LOG-02 承运商派发 — **接受 on 主路径 + P2**
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| DRAFT→ADVISED | :19 | advise:62 守卫+幂等 | testFullStateMachineFlow | MA2 ✅ | 接受 |
| **异步调网关 adviseShipment(post-commit+nop-job)** | :20 | ⚠️ 同步实现（completeShipment 内联重试 Thread.sleep）+ `erp-log.async-dispatch` 死 config | — | 行为等价（网关被调用） | **P2-RC-073** |
| 返回 trackingNo+labelUrl 回写 | :20 | writeBackSuccess:313 | testFullStateMachineFlow | ✅ | 接受 |
| ADVISED→DISPATCHED | :20 | completeShipment:84 | testFullStateMachineFlow | MA2 ✅ | 接受 |
| ShipmentLog 成功 | :20 | writeLog:366 | testFullStateMachineFlow | ✅ | 接受 |
| 异常:重试 3 次指数退避 | :26 | completeShipment 重试循环(maxRetries=3 + 30,120,600) | testGateway5xxRetryDeadLetter | MA2 ✅ | 接受 |
| 异常:重试耗尽保留 ADVISED 标异常通知 | :26 | deadLetter:332 + dispatchDeadLetterAlert | testGateway5xxRetryDeadLetter | MA2 ✅（R1.16） | 接受（reuse P1-MA2-080 resolved） |
| 异常:拒接通知换承运商 | :26 | deadLetter non-retryable 分支 + 告警 | testGateway4xxNoRetryDeadLetter | ✅ | 接受 |

**结论**：UC-LOG-02 = 接受 on 主路径（状态机+trackingNo 回写+ShipmentLog+重试退避+死信告警，MA2 A2.15 已证实 + 强测），**P2**（async-dispatch 死 config 同步替代异步[P2-RC-073 §2 P2① 次要验收标准，网关被调用行为等价，仅 post-commit 异步语义漂移 watch-only]）。§4 三判据：owner doc README §业务规则 §1 + 配置点 `erp-log.async-dispatch` 文档化"异步"但代码同步——属实现机制偏离，**§9 记入分歧不直改真相源**。修复纯调度接线预授权不触 ask-first。

### UC-LOG-03 追踪更新 — **接受 on 主路径 + P1 + P2 + reuse**
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| 回调认证/签名验证 | :39 | verifySignature:70 HmacSHA256(config-gated 默认必填) | testWebhookInvalidSignatureRejected | ✅ | 接受 |
| 解析追踪更新 trackingNo 关联 | :31 | parsePayload:91 + findShipmentByTrackingNo:229 | testWebhookInTransitThenDelivered | ✅ | 接受 |
| 已签收 IN_TRANSIT→DELIVERED 记 actualDeliveryDate/signedBy | :36 | advanceTracking:162-174 | testWebhookInTransitThenDelivered | MA2 ✅ | 接受 |
| 在途 ShipmentLog | :36 | writeWebhookLog:241 | testWebhookInTransitThenDelivered | ✅ | 接受 |
| DELIVERED 触发运费过账 | :36 | onDelivered:75 | TestErpLogFreightPosting | ✅ | 接受 |
| **超 3 天追踪异常标记通知** | :38 | ❌ 零实现 | ❌ 零断言 | ❌ | **P2-RC-074** |
| 部分签收保持 IN_TRANSIT | :38 | ❌ 零实现（P1-MA2-079 控制点） | ❌ | — | **reuse P1-MA2-079** |
| 货物退回 CANCELLED 审批 | :38 | cancelShipment:131（审批门控缺失=P1-MA2-078） | testCancelShipment | MA2 | **reuse P1-MA2-078** |
| **轮询间隔可配置默认 4h** | :39 | ⚠️ config key `tracking-poll-cron` 存在但**无 scheduler 消费**（scanForPolling 仅 manual @BizMutation） | ❌ 零断言 | ❌ | **P1-RC-085** |

**结论**：UC-LOG-03 = 接受 on 主路径（签名认证+解析+DELIVERED 状态/字段+ShipmentLog+运费过账触发，强测 + MA2 ✅），**P1**（轮询调度未接线[P1-RC-085 §2 P1① 功能完全缺失——config key 死，定时轮询不自动执行，对齐 projects P1-RC-053 死 config 范式]）+ **P2**（超 3 天追踪异常[P2-RC-074 边界场景 watch-only]）+ **reuse**（P1-MA2-078 审批门控 + P1-MA2-079 部分签收，均 resolved R1.25 via deferral——§去重协议同根因同控制点追加 RC 交叉引用不新建）。§4 三判据（P1-RC-085）：owner doc state-machine.md:111 + IErpLogShipmentBiz javadoc:44 显式"经 nop-job 周期触发"为期望非实现，**无 Deferred 标注**，git log 全 AI 无人工批准 → 三判据不成立 → Q4=(a) 强制实现。P1-RC-085 修复纯 scheduler 接线预授权不触 ask-first。

### UC-LOG-04 运费过账 — **接受 on 主路径 + P2**（**会计保护区域**）
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| relatedBillType 分流(SALES→FREIGHT/PURCHASE→到岸成本) | :48 | onDelivered:80-85 分流 + handlePurchaseReceiptDelivered:124 | TestErpLogFreightPosting + TestErpLogPath2LandedCost | ✅ | 接受 |
| **发布 ShipmentDeliveredEvent finance 订阅** | :48 | ⚠️ 直接 Facade 调用 `voucherBiz.post`（非事件订阅）+ 事件 POJO 仅 LOG.info 不派发 | — | 行为等价（finance 被调用） | **P2-RC-075** |
| freightSettlementStatus=SETTLED | :48 | markSettled:216 | testSalesFreightPostedAndSettled | ✅ | 接受 |
| freightAmount 空人工补 | :50 | ⚠️ path-1 AUTO 无 null 守卫（MANUAL 模式可人工） | — | MANUAL 模式覆盖 | 接受（存疑 SP-1） |
| 过账失败 PENDING 重试 | :50 | onDelivered catch 保 PENDING + dispatchFreightFailureAlert | TestLogPostingFaultInjection | MA2 ✅（R1.16） | 接受（reuse P1-MA2-080） |
| AUTO_POST/MANUAL_POST 配置 | :51 | CONFIG_SHIPMENT_SETTLEMENT_MODE AUTO/MANUAL（命名漂移功能等价）:88-93 | — | ✅ | 接受 |

**结论**：UC-LOG-04 = 接受 on 主路径（分流+SETTLED+失败 PENDING 告警+配置模式，强测端到端 + MA2 ✅），**P2**（ShipmentDeliveredEvent 死 POJO/机制漂移[P2-RC-075 §2 P2①——outcome"运费过账"经直接 Facade 调用达成行为等价，仅 L1 描述的"事件订阅"机制未实现，watch-only]）。§4 三判据（P2-RC-075）：owner doc state-machine.md §7 实现约定 :99-103 显式记录"直接调用范式…非事件订阅模型"——属 documented design change，但 git log 全 AI 无人工批准痕迹，判据(ii)不满足；行为等价故 P2 watch-only 非 P1。**会计保护区域标注**：UC-LOG-04 运费过账/到岸成本属会计过账路径，**任何过账逻辑变更须 ask-first + 独立 plan-audit §5 会计类**，且 path-2 到岸成本须与 finance 域 `costing-methods.md:287-309` 协调。零 P0（运费 GL 经 finance 过账引擎结构保证平衡，path-2 config 默认关，无活跃 GL 失衡风险）。

### UC-LOG-05 承运商集成 — **接受 on 主路径 + 2 P2**
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| Carrier+CarrierConfig(name/gatewayId/端点/凭证/服务类型) | :60 | 实体+CRUD 空壳 + ORM 字段 | CRUD | ✅ | 接受 |
| **EncryptionHelper 加密+脱敏** | :60 | ⚠️ 脱敏显示 ✅（masking test）/ 加密存储 stub（仅 javadoc） | testCredentialsNotExposedViaFindPage | mock-only 基线 | **P2-RC-076** |
| **连通性测试调网关 adviseShipment/trackShipment** | :60 | ❌ 无连通性测试 action（BizModel 空壳） | ❌ 零断言 | ❌ | **P2-RC-077** |
| 配置生效可选 + 多配置 carrierConfigId | :60/62 | ErpLogShipment.carrierConfigId 字段 + IDX | — | ✅ | 接受 |
| 新增承运商=1 Factory bean+Client | :63 | MockCarrierGatewayClientFactory @Service + Registry 零改 commons | — | ✅ | 接受 |

**结论**：UC-LOG-05 = 接受 on 主路径（Carrier/Config 实体+多配置选择+SPI 三层零改 commons 范式），**P2**（EncryptionHelper 加密存储 stub[P2-RC-076——README §基线范围 :29 显式"真实承运商 HTTP 集成…为后续范围"，mock-only 基线下加密 stub 对齐范围裁剪倾向，§4(iii) product-scope 范围 + 脱敏显示层已防护，watch-only successor] + 连通性测试 action 缺失[P2-RC-077 §2 P2① 次要验收标准，网关调用在 advise/tracking 时发生，专用 admin 测试 action 缺 watch-only]）。§4 三判据（P2-RC-076）：README §基线范围显式范围裁剪（真实承运商集成后续范围），倾向 §4(iii) 成立（范围裁剪）但无人工批准登记 → 声明 Q4=(a) 张力，mock-only 基线行为已验证故 P2。修复触 EncryptionHelper 加密预授权 + 真实 Factory 实现预授权不触 ask-first。

### UC-LOG-07 配送时间窗口管理 — **P1**（容量预约逻辑完全缺失）
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| 按星期+容量展示窗口 | :72 | ❌ 无查询逻辑（空壳） | ❌ | ❌ | **P1-RC-086** |
| 校验 currentBooked<maxCapacity | :72 | ❌ 零实现 | ❌ | ❌ | 并入 |
| 确认 currentBooked+=1 | :72 | ❌ 零实现 | ❌ | ❌ | 并入 |
| 配送后 ARRIVED/DELIVERED | :72 | ❌ 零实现 | ❌ | ❌ | 并入 |
| 取消/完成 currentBooked-=1 | :72 | ❌ 零实现 | ❌ | ❌ | 并入 |
| 容量不足拒绝 | :74 | ❌ | ❌ | ❌ | 并入 |
| 爽约 MISSED 爽约费+priorityScore | :74 | ❌（priorityScore/missedFee 字段载体不存在） | ❌ | ❌ | 并入 |
| 窗口过期失效 | :74 | ❌ | ❌ | ❌ | 并入 |
| 重复预约幂等 | :74 | ❌（ErpLogDeliveryBooking 不存在） | ❌ | ❌ | 并入 |

**结论**：UC-LOG-07 = **P1**（配送窗口容量预约逻辑完全缺失[P1-RC-086 §2 P1① 功能完全缺失——`ErpLogDeliveryWindowBizModel` 17 行空壳 CRUD + `ErpLogDeliveryBooking` 预约实体 ORM 不存在[delivery-window.md:61 标"预留"] + currentBooked/maxCapacity/priorityScore/missedFee 零业务逻辑 + 零测试，UC-LOG-07 全部 9 验收标准不可满足）。§4 三判据：delivery-window.md:61 `ErpLogDeliveryBooking` 标"预留"为 AI 自标无人工批准痕迹 + owner doc 业务流程 :83-107 活跃要求未声明 Deferred + product-scope 未裁剪配送窗口 → 三判据均不成立 → Q4=(a) 强制实现。**触及 ORM 结构变更**（物化 `ErpLogDeliveryBooking` 实体 + 容量计数逻辑 + MISSED/priorityScore 字段）**须 ask-first + 独立 plan-audit §5 ORM 类**；容量预约/爽约费 BizModel 逻辑预授权。

### UC-LOG-06 签收确认 — **接受 on 主路径 + P1 + P2 + reuse**
| 验收标准 | L1 | L3 | L4 | L5 | 结论 |
|----------|----|----|----|----|------|
| 多渠道签收(网关回调/POD 上传/发货员确认) | :86 | webhook 自动 ✅ + completeShipment 手动 ✅ / **POD 上传 ❌** | — | 部分 | 部分（POD 并入 P2-RC-078） |
| actualDeliveryDate/signedBy | :86 | advanceTracking:169/171 ✅ | testWebhookInTransitThenDelivered | ✅ | 接受 |
| **signatureImage** | :86 | ❌ 零实现 | ❌ | ❌ | **P2-RC-078** |
| IN_TRANSIT→DELIVERED | :86 | advanceTracking:162 | testWebhookInTransitThenDelivered | MA2 ✅ | 接受 |
| **通知 sales 交付状态** | :86 | ❌ 零实现（无 IErpSalDeliveryBiz 调用） | ❌ 零断言 | ❌ | **P1-RC-087** |
| 触发运费过账 | :86 | onDelivered:75 | TestErpLogFreightPosting | ✅ | 接受 |
| 损坏拒签 IN_TRANSIT→CANCELLED 审批 | :88 | cancelShipment:131（审批门控=P1-MA2-078） | testCancelShipment | MA2 | reuse P1-MA2-078 |
| 部分签收保持 IN_TRANSIT | :88 | ❌（P1-MA2-079） | ❌ | — | reuse P1-MA2-079 |
| 签收不一致人工修正 | :88 | ⚠️ completeShipment 手动可修正 | — | 行为等价 | 接受 |

**结论**：UC-LOG-06 = 接受 on 主路径（webhook/手动签收+actualDeliveryDate/signedBy+IN_TRANSIT→DELIVERED+运费过账触发，强测 + MA2 ✅），**P1**（通知 sales 交付状态缺失[P1-RC-087 §2 P1④ 跨域契约行为不一致——L1 :86 明确"如关联销售出库单：通知 sales 域更新订单交付状态"，onDelivered 不调任何 sales I*Biz]）+ **P2**（signatureImage/POD 上传缺失[P2-RC-078 §2 P2① 次要验收标准，actualDeliveryDate/signedBy 主路径满足 watch-only]）+ **reuse**（P1-MA2-078 审批 + P1-MA2-079 部分签收）。§4 三判据（P1-RC-087）：use-cases.md:86 活跃跨域契约要求 + owner doc state-machine.md §7 外部依赖 :97"sales 域发布出库事件本域订阅"为反向（sales→logistics）未覆盖 logistics→sales 交付回写，**无 Deferred**，git log 全 AI → 三判据不成立 → Q4=(a) 强制实现。**跨域契约须与 sales 域协调 ask-first**（logistics→sales 交付状态回写接口）。

### 切片总览
- **零 P0**（重复发运防护缺失双 shipment 重复运费过账需特定条件非默认活跃破坏；运费 GL 经 finance 引擎平衡；path-2 默认关 → 无活跃数据破坏候选升 P0）。
- **5 新 P1**：P1-RC-083（重复发运防护）/ P1-RC-084（24h 升级）/ P1-RC-085（轮询调度）/ P1-RC-086（配送窗口容量）/ P1-RC-087（通知 sales）。
- **6 新 P2**：P2-RC-073~078。
- **reuse 3**：P1-MA2-078（审批门控）/ P1-MA2-079（部分签收）/ P1-MA2-080（过账失败告警）+ 引用 P1-MA2-092（trackingNo UK）+ P2-MA2-072（关联出库单弱指针）。

---

## §6 与 arm-index 衔接（复用 or 新增裁决，§7 规则）

> 执行前 grep arm-index logistics/shipment/carrier/freight/window/tracking 同域同控制点。当前至 **P1-RC-082 / P2-RC-072**（续编自 P1-RC-083 / P2-RC-073）。

### 新建 finding 裁决（均为新根因/新控制点/新维度，§7"新建"列）

| Finding ID | UC | 描述（简） | §2 判据 | 目标 MR | 修复状态 | 触及保护区域 |
|-----------|----|------|---------|--------|---------|-------------|
| `P1-RC-083` | UC-LOG-01 | **重复发运防护完全缺失**：L1（:12,14）要求校验出库单无有效发运单。L3 `defaultPrepareSave:68-81` 仅校验 trackingNo+carrierId 重复，**无 relatedBillType+relatedBillCode 维度校验**；ORM（app-erp-logistics.orm.xml:223-226）无 (relatedBillType,relatedBillCode) UK；grep 零重复防护逻辑。可致同一出库单创建多条非 CANCELLED 发运单→潜在重复运费过账（需双 shipment 均 DELIVERED，非默认活跃破坏故非 P0）。**与 P2-MA2-072 不同维度**：P2-MA2-072 = audit-remediation owner doc drift（关联出库单"锁定/释放"弱指针文本一致性）；本 finding = RC 需求契约视角（重复发运防护验证逻辑完全缺失），同代码站点不同审计轴。 | §2 P1①（功能实质偏离验收标准） | MR1（R1.0→RC-R1.n） | todo（本审计仅登记不实施；修复 = `defaultPrepareSave` 增 relatedBillType+relatedBillCode 非 CANCELLED 重复查询守卫 **或** 加 (relatedBillType,relatedBillCode,delVersion) DB UK；**触及 ORM 结构变更[DB UK]须 ask-first + 独立 plan-audit §5 ORM 类**，BizModel 校验逻辑预授权） | ORM ask-first |
| `P1-RC-084` | UC-LOG-01 | **DRAFT 24h 未确认升级通知完全缺失**：L1（:15）逐字「超过 24 小时未确认的 DRAFT 发运单触发升级通知」。L3 grep `escalation|24` erp-log-service/src/main 零业务命中；无 config key + 无 scheduler/cron/Job bean + 无 notify 调用。owner doc state-machine.md:124 活跃要求。 | §2 P1②（异常路径/自动化未实现） | MR1 | todo（修复 = 新增 `erp-log.draft-escalation-hours` config key + scheduler 注册 draft-escalation job[扫描 DRAFT 超 N 小时]+ 调 IErpSysNotificationBiz；纯调度接线+notify 预授权不触 ask-first） | 否 |
| `P1-RC-085` | UC-LOG-03 | **轮询调度未接线（cron 死 config）**：L1（:39）「定时轮询间隔可配置（默认 4 小时）」。L3 config key `erp-log.tracking-poll-cron`（默认 `0 0 */4 * * ?`）存在于 ErpLogConfigs:15 + IErpLogShipmentBiz javadoc:44，但 grep `IScheduler|QuartzJob|job.yaml|batch.xml` module-logistics **零命中**——scanForPolling 仅 manual @BizMutation，定时轮询不自动执行。对齐 projects P1-RC-053 + cs P1-RC-054 死 config 范式。 | §2 P1①（功能完全缺失——"定时"轮询调度缺失） | MR1 | todo（修复 = nop-batch job.yaml 注册 tracking-poll job + 消费 config key 门控 + 批量调 scanForPolling；纯调度接线预授权不触 ask-first） | 否 |
| `P1-RC-086` | UC-LOG-07 | **配送窗口容量预约逻辑完全缺失**：L1（:72-74）要求容量检查/currentBooked±1/ARRIVED-DELIVERED/爽约 MISSED 爽约费+priorityScore/窗口过期/重复预约幂等 9 验收标准。L3 `ErpLogDeliveryWindowBizModel` 17 行空壳 CRUD（仅构造器）+ `ErpLogDeliveryBooking` 预约实体 ORM **不存在**（delivery-window.md:61 标"预留"）+ currentBooked/maxCapacity/priorityScore/missedFee 零业务逻辑 + 零测试。9 验收标准全不可满足。 | §2 P1①（功能完全缺失）+ §2 P1⑤（零断言） | MR1 | todo（修复 = 物化 `ErpLogDeliveryBooking` 实体 + 容量预约 BizModel/Processor 逻辑[currentBooked±1 守卫 + MISSED 爽约费 + priorityScore + 窗口过期 + 幂等 UK]；**触及 ORM 结构变更[新增 ErpLogDeliveryBooking 实体]须 ask-first + 独立 plan-audit §5 ORM 类**，容量/爽约费逻辑预授权） | ORM ask-first |
| `P1-RC-087` | UC-LOG-06 | **通知 sales 交付状态缺失（跨域契约）**：L1（:86）逐字「如关联销售出库单：通知 sales 域更新订单交付状态」。L3 grep `IErpSalDeliveryBiz|IErpSalOrderBiz|sales.*notify` erp-log-service/src/main 零命中——onDelivered 仅调 finance voucherBiz.post + inventory landedCostBiz，**不调任何 sales I*Biz** 回写交付状态。 | §2 P1④（跨域契约行为不一致） | MR1 | todo（修复 = onDelivered SALES_DELIVERY 分支注入 IErpSalDeliveryBiz 调用回写交付状态 **或** 经 notify 子系统派发 sales 交付事件；**跨域契约须与 sales 域协调 ask-first**，BizModel/notify 接线预授权） | 跨域 sales ask-first |
| `P2-RC-073` | UC-LOG-02 | **async-dispatch 死 config（同步替代异步）**：L1（:20,27）「异步调用…post-commit+nop-job…不阻塞主事务」。L3 `completeShipment:84` 同步内联重试（Thread.sleep），`erp-log.async-dispatch`（README 配置点）生产代码零消费。网关被调用行为等价，仅 post-commit 异步语义漂移。 | §2 P2①（次要验收标准，行为等价 watch-only） | successor watch-only | todo（修复 = 接线 post-commit nop-job 异步派发 **或** owner doc 显式标注"同步实现，async-dispatch 为预留 config"；前者调度接线预授权，后者纯文档预授权） | 否 |
| `P2-RC-074` | UC-LOG-03 | **超 3 天追踪异常标记/通知缺失**：L1（:38）「超过预计送达日期 3 天：系统标记'追踪异常'，通知物流主管」。L3 grep `追踪异常|overdue.*notify|3.*天` 零业务命中。主路径追踪推进正常，边界（长期无更新）弱。 | §2 P2①（边界场景弱 watch-only） | successor watch-only | todo（修复 = scanForPolling/独立 job 增超期检测分支 + notify 物流主管；纯 BizModel/scheduler 预授权不触 ask-first） | 否 |
| `P2-RC-075` | UC-LOG-04 | **ShipmentDeliveredEvent 死 POJO（机制漂移）**：L1（:48）「发布 ShipmentDeliveredEvent，finance 域订阅」。L3 直接 Facade 调用 `voucherBiz.post`（非事件订阅）+ 事件 POJO javadoc:12 显式"不派发"仅 LOG.info。运费过账 outcome 经直接调用达成行为等价。 | §2 P2①（机制漂移行为等价 watch-only） | successor watch-only | todo（修复 = 移除死 POJO **或** 实际接线事件总线 + owner doc 实现约定对齐；纯代码/文档预授权；**会计过账路径变更若涉须 ask-first**） | 会计（若改过账路径） |
| `P2-RC-076` | UC-LOG-05 | **EncryptionHelper 凭证加密存储 stub**：L1（:60）「凭证输入时系统自动加密存储（EncryptionHelper）」。L3 脱敏显示 ✅（masking test）/ 加密存储 stub（IErpLogCarrierGatewayClientFactory:5 javadoc + Mock:60 注释，零 import 零调用）。README §基线范围:29 显式"真实承运商 HTTP 集成…为后续范围"。 | §2 P2①（mock-only 基线 successor watch-only，§4(iii) 范围裁剪倾向） | successor watch-only | todo（修复 = 真实 Factory 落地 EncryptionHelper 加密存储；真实承运商集成时实施，预授权不触 ask-first） | 否 |
| `P2-RC-077` | UC-LOG-05 | **连通性测试 action 缺失**：L1（:60）「管理员可进行连通性测试：系统调用 adviseShipment/trackShipment 验证配置」。L3 ErpLogCarrierConfigBizModel 空壳无连通性测试 @BizMutation；网关调用在 advise/tracking 时发生，专用 admin 测试 action 缺。 | §2 P2①（次要验收标准 watch-only） | successor watch-only | todo（修复 = ErpLogCarrierConfigBizModel 增 testConnectivity @BizMutation 调 client.trackShipment 试探；纯 BizModel 预授权不触 ask-first） | 否 |
| `P2-RC-078` | UC-LOG-06 | **signatureImage/POD 上传缺失**：L1（:86）「记录 signatureImage（如支持）」+「POD（人工上传）」。L3 grep `signatureImage|POD` module-logistics 零命中；actualDeliveryDate/signedBy 主路径满足。L1 已注"如支持"为可选。 | §2 P2①（次要验收标准 watch-only） | successor watch-only | todo（修复 = ErpLogShipment 加 signatureImage 列 + POD 上传 mutation；**触及 ORM 须 ask-first**，或 owner doc 显式标"如支持=当前不支持"） | ORM（若加列） |

### reuse 裁决（同根因同控制点，§7"复用"列——追加 RC 交叉引用不新建）

| 既有 Finding | 报告 | 本切片 reuse UC | 裁决 |
|-------------|------|----------------|------|
| `P1-MA2-078` | ma2-aps-logistics-state-machine | UC-LOG-03/06（IN_TRANSIT→CANCELLED 审批门控） | **复用**（同控制点：cancelShipment 审批门控缺失，resolved R1.25 via deferral）。从 RC 视角声明 Q4=(a) 张力：R1.25 经 owner doc Deferred 标注关闭，但 §4 三判据(ii) Deferred 为 AI 自标无人工批准痕迹——本审计不重开（§去重协议），但登记 Q4 张力供 MA2 复查时重新分级参考。 |
| `P1-MA2-079` | ma2-aps-logistics-state-machine | UC-LOG-03/06（部分签收） | **复用**（同控制点：部分签收完全未实现，resolved R1.25 via deferral）。同上 Q4=(a) 张力声明。 |
| `P1-MA2-080` | ma2-aps-logistics-state-machine | UC-LOG-02/04（网关重试耗尽+过账失败告警） | **复用**（同控制点：deadLetter/onDelivered 告警闭环，resolved R1.16 via implementation——**已真正实现** dispatchDeadLetterAlert + dispatchFreightFailureAlert，行为证据 PASS，仅引作行为证据非重开） |
| `P1-MA2-092` | ma2-concurrency-optimistic-lock | UC-LOG-01/03（trackingNo UK） | **复用**（resolved R1.28，引作 UC-LOG-01 trackingNo 维度重复防护行为证据——注意本切片 P1-RC-083 是 relatedBillType 维度不同控制点） |
| `P2-MA2-072` | ma2-aps-logistics-state-machine | UC-LOG-01（关联出库单弱指针） | **复用**（audit-remediation owner doc drift 维度；本切片 P1-RC-083 是 RC 需求契约维度不同审计轴，追加交叉引用不合并） |

**双向可追溯**：新 finding 已写入下方 arm-index RC 发现追踪分区（P1-RC-083~087 / P2-RC-073~078）；修复行预留 MR1（R1.0 展开为 RC-R1.n 时含 finding ID 交叉引用）。

---

## §7 静态存疑点清单（供 MA4 运行时探针展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。P0 即时通道评估见各项。

1. **SP-1（UC-LOG-01 重复发运→重复运费过账）**：重复发运防护缺失（P1-RC-083）下，若同一出库单创建 2 条发运单且均 DELIVERED，是否产生 2 张 FREIGHT 凭证致重复过账？运行时探针：双 shipment 同 relatedBillCode → 均 DELIVERED → 断言凭证数。**P0 评估**：需特定条件（双创建+双送达）非默认活跃，且每 shipment 独立过账非并发 UK 破坏 → 维持 P1 非 P0。
2. **SP-2（UC-LOG-03 轮询缺失运行时影响）**：轮询调度未接线（P1-RC-085）下，仅 webhook 驱动状态推进；若承运商不发 webhook，DISPATCHED 运单是否长期不推进至 DELIVERED→运费不过账？运行时探针：无 webhook 承运商 + manual 不调 scanForPolling → 状态滞留时长。
3. **SP-3（UC-LOG-07 容量超卖运行时影响）**：容量预约逻辑缺失（P1-RC-086）下，ErpLogDeliveryWindow.maxCapacity 列存在但无校验——是否多预约超卖？运行时探针：因 ErpLogDeliveryBooking 不存在，预约本身不可创建，故当前无超卖风险（实体缺失=功能缺失非数据破坏）；物化实体时须同时落地容量守卫。
4. **SP-4（UC-LOG-04 path-1 AUTO + freightAmount=null）**：onDelivered path-1 AUTO 模式下若 freightAmount=null，buildFreightPostingEvent 写入 null amount → voucherBiz.post 是否生成 0/null 金额凭证？运行时探针：null freightAmount + AUTO → 凭证行金额断言。倾向 MANUAL 模式覆盖（人工补），但 AUTO 无 null 守卫存疑。

---

## §8 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。区分门控退出码 vs 纯 reporter 退出码——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告为只读审计无生产代码变更，checker 无回归风险**。actual 实测汇总（vs compliance-baseline.md 基线，均在基线内）：

  | 规则 | actual（实测） | 基线 | 结论 |
  |------|--------------|------|------|
  | R1d | 14 | 14 | ≤ ✅ |
  | R2a | 34 | 34 | ≤ ✅ |
  | R2c（生产代码总计） | 1382 | 1380（+后续注记下调，实际基线块已吸收） | 持平无回归 ✅ |
  | R2d | 34 | 32（+注记） | 持平 ✅ |
  | R8 | 0 | 0 | ≤ ✅ |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index logistics/shipment/carrier/freight/window/tracking 同域同控制点后给出"复用 or 新增"裁决（5 新 P1 reuse 3 P1-MA2 + 6 新 P2），无未经比对直接新建的 finding。复用项均追加 RC 交叉引用注记不新建编号。
- [x] **真相源冻结声明（§9）**：本审计期间未修改任何真相源（use-cases.md / owner doc / product-scope.md）；分歧记入报告不直改。

---

## 报告 9 段完整性自检

- [x] §1 需求契约原文（逐字引用 UC-LOG-01~07）✅
- [x] §2 实现证据（L3 方法锚点+行为断言+grep 站点）✅
- [x] §3 测试证据（L4 引用+断言强度）✅
- [x] §4 运行时行为证据（L5，MA2 复用+E2E）✅
- [x] §5 符合性结论（五级矩阵逐 UC，§2 判据+§4 三判据+保护区域标注）✅
- [x] §6 与 arm-index 衔接（复用/新增裁决+双向可追溯）✅
- [x] §7 静态存疑点清单（4 存疑点+P0 评估）✅
- [x] §8 过程纪律自检（checker actual vs baseline+独立性+交叉去重+冻结声明）✅
- [x] §9 与 MA2 报告差异增量声明（首段）✅
