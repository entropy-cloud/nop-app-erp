# 物流域 — 承运商网关集成设计

## 目的

详细说明承运商网关（Carrier Gateway）的 SPI 三层架构、承运商接入流程、异步派发与重试机制、追踪回调与比价（Rate Shopping）设计。本设计是 `logistics/README.md` §SPI 契约的实施级深化。

---

## 一、SPI 三层架构（Client / Factory / Registry）

参考 🟢 Metasfresh `ShipperGatewayClient.java:37-58` + `ShipperGatewayClientFactory.java:29-34` + `ShipperGatewayServicesRegistry.java:43-125`。

### 1.1 第一层：IErpLogCarrierGatewayClient（承运商网关客户端）

每个承运商实现此接口，封装与该承运商网关的 HTTP/gRPC 通信细节。

```java
interface IErpLogCarrierGatewayClient {

    // 提交发运订单（下单），返回运单号 + 面单
    DeliveryOrderResult completeDeliveryOrder(DeliveryOrderRequest request);

    // 获取面单标签（支持多包裹）
    List<PackageLabel> getPackageLabelsList(String shipmentNo);

    // 预约取件/发运通知
    void adviseShipment(ShipmentAdvice advice);

    // 追踪运单
    TrackingResult trackShipment(String trackingNo);

    // 取消发运
    void cancelShipment(String shipmentNo);

    // 获取运费报价
    RateQuoteResult getRateQuote(RateQuoteRequest request);
}
```

### 1.2 第二层：IErpLogCarrierGatewayClientFactory（承运商客户端工厂）

per-carrier 配置化创建 client。每个网关实现一个 Factory bean，读取 `ErpLogCarrierConfig` 注入凭证和端点。

```java
interface IErpLogCarrierGatewayClientFactory {
    String getGatewayId();                              // 如 "dhl" / "sf" / "jd"
    IErpLogCarrierGatewayClient newClientForCarrierId(String carrierId);
        // 内部行为：
        //   1. 查 ErpLogCarrierConfig (by carrierId)
        //   2. 解密 apiKey/apiSecret (EncryptionHelper)
        //   3. 构造配置化的 client 实例
}
```

🟢 `DhlShipperGatewayClientFactory.java:16-47`：`newClientForCarrierId` 读取配置 → 注入 auth token → 返回 DhlShipperGatewayClient(config)。

### 1.3 第三层：ErpLogCarrierGatewayRegistry（自动聚合注册中心）

Nop IoC 自动聚合所有 Factory，提供 dispatcher 入口。

```java
@Component
class ErpLogCarrierGatewayRegistry {
    @Inject Map<String, IErpLogCarrierGatewayClientFactory> factories;  // key = gatewayId

    IErpLogCarrierGatewayClient getClient(String carrierId) {
        ErpLogCarrier carrier = carrierDao.getById(carrierId);
        IErpLogCarrierGatewayClientFactory factory = factories.get(carrier.getGatewayId());
        if (factory == null) throw new NopException(ERR_LOG_CARRIER_GATEWAY_NOT_FOUND);
        return factory.newClientForCarrierId(carrierId);
    }

    // 比价：收集所有可用承运商的报价
    List<RateQuoteResult> getRateQuotes(RateQuoteRequest request);
}
```

### 1.4 中立 DTO 模型包

放 `erp-log-service` 模块的 `spi/model/` 包，承运商无关 POJO：

| DTO | 用途 |
|-----|------|
| `DeliveryOrderRequest` | 发运下单请求：收/发件人、包裹列表、服务类型 |
| `DeliveryOrderResult` | 下单结果：承运商单号、面单 URL、预计送达日期 |
| `PackageLabel` | 面单：包裹编号、label URL/Base64、格式（PDF/ZPL） |
| `ShipmentAdvice` | 预约取件：时间窗口、地址、联系人 |
| `TrackingResult` | 追踪结果：状态码、位置、时间线列表 |
| `TrackingEvent` | 追踪事件：时间、位置、状态描述 |
| `RateQuoteRequest` | 比价请求：起始地/目的地、包裹重量/尺寸、服务类型 |
| `RateQuoteResult` | 比价结果：承运商、服务类型、运费、预计时效 |
| `Address` | 地址：国家/省/市/区/详细地址/邮编 |
| `ParcelInfo` | 包裹信息：重量/长/宽/高/申报价值 |

---

## 二、承运商接入流程

### 2.1 新增承运商步骤

| 步骤 | 操作 | 涉及文件 |
|------|------|----------|
| 1 | 实现 `IErpLogCarrierGatewayClient`（网关通信逻辑） | 新建 `client/DhlCarrierGatewayClient.java` |
| 2 | 实现 `IErpLogCarrierGatewayClientFactory`（配置化创建 + `@Service` bean） | 新建 `factory/DhlCarrierGatewayClientFactory.java` |
| 3 | 在 `ErpLogCarrier` 表新增记录（`carrierName` + `gatewayId` + `trackingUrlTemplate`） | 页面管理或 seed 数据 |
| 4 | 在 `ErpLogCarrierConfig` 表新增配置（`apiEndpoint` + `apiKey`/`apiSecret` + 服务类型） | 页面管理或 seed 数据 |
| 5 | （可选）在 `RateQuoteProvider` 扩展注册比价实现 | 实现 `getRateQuote()` |

**新增承运商 = 1 个 Factory bean + 1 个 Client 实现 + 配置记录，零改 Registry/commons**（🟢 `DhlShipperGatewayClientFactory.java:16-47` 范式）。

### 2.2 连通性测试流程

```
管理员保存 CarrierConfig
        │
        ├─► 保存后弹出"是否测试连通性？"
        │
        ├─► 是 → 调用 client.trackShipment("TEST") 或 client.getRateQuote(...)
        │           │
        │           ├─► 成功 → 显示 ✅ 连通成功，网关延迟 XXms
        │           │
        │           └─► 失败 → 显示 ❌ 失败详情（连接超时/认证失败/服务不可达）
        │
        └─► 否 → 仅保存，后续在列表页可手动触发测试
```

### 2.3 承运商凭证安全

| 规则 | 说明 |
|------|------|
| 加密存储 | `apiKey` / `apiSecret` / `credentials` 使用 Nop `EncryptionHelper` 加密（AES-256-GCM） |
| 页面脱敏 | 显示 `sk****ey`（仅末尾 4 位可见） |
| 传输保护 | 仅 Bean 中解密使用，不序列化到前端 |
| 轮换支持 | 支持凭证版本号，新旧凭证共存到切换完成 |

---

## 三、关键 API 操作详解

### 3.1 completeDeliveryOrder（发运下单）

```
发货员点击"确认发运" (DRAFT → ADVISED)
        │
        ▼
ErpLogCarrierGatewayRegistry.getClient(carrierId)
        │
        ▼
client.completeDeliveryOrder(request)
   request = {
       shipmentCode,         // 发运单号
       serviceType,          // 标准/加急/经济
       sender: { name, phone, address },
       receiver: { name, phone, address },
       parcels: [{ weight, length, width, height, declaredValue }],
       referenceNo           // 关联单号（对方系统可查）
   }
        │
        ▼
   ┌─ 成功 ──→ DeliveryOrderResult { trackingNo, labelUrl, estimatedDelivery }
   │                │
   │                ├─► 回写 ErpLogShipment.trackingNo
   │                ├─► 回写 ErpLogShipment.labelUrl
   │                ├─► 回写 ErpLogShipmentParcel 各包裹 trackingNo/labelUrl
   │                └─► 状态迁移 ADVISED → DISPATCHED
   │
   └─ 失败 ──→ 记录 ErpLogShipmentLog(error)
                    │
                    └─► 进入异步重试队列
```

### 3.2 getPackageLabelsList（获取面单）

```
场景：已下单但面单获取失败，或需要补打面单
        │
        ▼
client.getPackageLabelsList(shipmentNo)
        │
        ▼
   ┌─ 成功 ──→ List<PackageLabel> { parcelNo, labelUrl, format }
   │               │
   │               └─► 更新各包裹 labelUrl
   │
   └─ 失败 ──→ 返回错误，可在页面手动重试
```

### 3.3 adviseShipment（预约取件）

```
场景：需要预约承运商上门取件
        │
        ▼
client.adviseShipment(advice)
   advice = {
       shipmentCode,
       pickupDate, pickupTimeFrom, pickupTimeTo,  // 取件时间窗口
       pickupAddress,
       contactPerson, contactPhone,
       parcels: [{ weight, dimensions }]
   }
        │
        ▼
   ┌─ 成功 ──→ 确认预约，预约编号回写
   │
   └─ 失败 ──→ 记录错误，可更换时间窗口重试
```

### 3.4 cancelShipment（取消发运）

```
场景：发运单取消时需通知承运商
        │
        ▼
client.cancelShipment(shipmentNo)
        │
        ▼
   ┌─ 成功 ──→ 承运商已取消
   │
   └─ 失败 ──→
         ├─ 网关不支持取消 → 标记"人工取消"，通知联系承运商线下处理
         └─ 网关报错 → 记录日志，人工介入
```

---

## 四、异步派发流程（post-commit + 重试 + 死信）

### 4.1 异步派发总览

承运商网关调用走异步（🟢 `DeliveryOrderWorkpackageProcessor.java:103-140`），不阻塞主事务。

```
发运单创建/确认发运事务
        │
        ▼ (事务内)
状态迁移 DRAFT → ADVISED (事务提交)
        │
        ▼ (post-commit)
投递到 async 队列 (nop-job)
        │
        └─► 异步执行：
              ├─► client.completeDeliveryOrder()
              ├─► 成功 → ADVISED → DISPATCHED
              │
              └─► 失败 → 进入重试队列
```

### 4.2 重试策略

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 最大重试次数 | 3 | `erp-log.gateway-max-retries` |
| 重试间隔 | 指数退避 | 第 1 次 30s → 第 2 次 2min → 第 3 次 10min |
| 超时 | 30s | `erp-log.gateway-timeout-secs` |
| 重试触发条件 | 网络超时 (HTTP 5xx/Timeout) | 认证失败 (4xx) 不自动重试 |

### 4.3 重试流程

```
异步任务开始
        │
        ├─► client.completeDeliveryOrder()
        │           │
        │           ├─► 成功 → 状态迁移 DISPATCHED，记录日志，结束
        │           │
        │           └─► 失败（超时/5xx）
        │                       │
        │                       ├─► 重试计数 < 3 → 等待间隔后重试
        │                       │
        │                       └─► 重试已达上限 → 进入死信队列
        │
        └─► 失败（4xx 认证错误）
                    │
                    └─► 不重试，直接设 state=ERROR，标记"认证失败需人工处理"
```

### 4.4 死信处理

| 阶段 | 操作 |
|------|------|
| 死信投递 | 重试 3 次耗尽后，发运单保留 ADVISED 状态，标记 `gatewayError=true` |
| 通知 | 通知发货员/物流主管：承运商 XX 发运单 XX 网关异常需处理 |
| 人工干预 | 发货员检查原因（凭证过期/网络故障/配置错误）→ 修复 → 点击"手动重试" |
| 手动重试 | 重新投递异步任务，继续走完整流程 |
| 兜底 | 人工确认承运商已线下处理 → 手动补充运单号 → 强制迁移至 DISPATCHED |

### 4.5 幂等防护

承运商网关接口实现需支持幂等：`completeDeliveryOrder` 使用 `referenceNo`（发运单号）作为幂等键。重试时使用同一 `referenceNo`，网关返回已有结果。

---

## 五、追踪更新 Webhook（承运商 → ERP 状态回调）

### 5.1 回调端点

```
POST /r/log/webhook/tracking/{carrierCode}
```

| 请求头 | 说明 |
|--------|------|
| `X-Signature` | HMAC-SHA256 签名（使用 CarrierConfig 中配置的 webhookSecret） |
| `X-Carrier-Code` | 承运商标识 |

### 5.2 签名验证

```java
boolean verifySignature(String payload, String signature, String secret) {
    String expected = HmacUtils.hmacSha256Hex(secret, payload);
    return MessageDigest.isEqual(expected.getBytes(), signature.getBytes());
}
```

🟢 复用 `integration-pattern.md` 入站签名机制。

### 5.3 回调处理流程

```
承运商推送追踪事件
        │
        ▼
HMAC 验签 ──失败──→ 401 拒绝，记录 ErpLogShipmentLog
        │
        ▼ (通过)
解析事件 payload
        │
        ▼
查找发运单 (by trackingNo)
        │
        ├─► 未找到 → 记录日志，返回 404
        │
        └─► 找到发运单
                    │
                    ├─► 更新追踪事件到 ErpLogShipmentLog
                    │
                    ├─► 判断事件类型：
                    │     ├─ PICKED_UP     → DISPATCHED → IN_TRANSIT（如仍为 DISPATCHED）
                    │     ├─ IN_TRANSIT    → 更新位置/时间戳
                    │     ├─ OUT_FOR_DELIVERY → 更新预计送达时间
                    │     ├─ DELIVERED     → IN_TRANSIT → DELIVERED
                    │     │                     ├─► 记录 actualDeliveryDate / signedBy
                    │     │                     └─► 触发运费过账流程
                    │     ├─ EXCEPTION     → 记录异常（退回/拒收/地址错误）
                    │     └─ RETURNED      → IN_TRANSIT → CANCELLED（需要审批）
                    │
                    └─► 返回 200 OK
```

### 5.4 轮询兜底

对于不支持回调的承运商，提供定时轮询：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-log.tracking-poll-cron` | `0 0 */4 * * ?` | 每 4 小时轮询一次 |
| `erp-log.tracking-poll-max-days` | 30 | 仅轮询近 30 天内 DISPATCHED/IN_TRANSIT 的记录 |

---

## 六、比价（Rate Shopping）

### 6.1 触发场景

| 场景 | 触发方式 |
|------|----------|
| 手动比价 | 发货员在发运单创建页点击"比价"按钮 |
| 自动推荐 | 系统根据预设规则（最低价/最快时效）自动推荐承运商 |

### 6.2 比价流程

```
发货员填写发运信息（收件地址、包裹重量/尺寸）
        │
        ▼
点击"比价"按钮
        │
        ▼
ErpLogCarrierGatewayRegistry.getRateQuotes(request)
   request = {
       senderAddress,
       receiverAddress,
       parcels: [{ weight, length, width, height }],
       serviceTypes: ["STANDARD", "EXPRESS"]  // 可选，多个服务类型
   }
        │
        ▼
Registry 遍历所有激活且有 Client 的承运商
   for each (factory in factories) {
       client = factory.newClientForCarrierId(carrierId);
       result = client.getRateQuote(request);  // 并行调用
       results.add(result);
   }
        │
        ▼
返回比价结果列表
   ┌────┬──────────┬────────┬────────┬──────────┬──────┐
   │排名│ 承运商    │ 服务   │ 运费    │ 预计时效 │ 推荐 │
   ├────┼──────────┼────────┼────────┼──────────┼──────┤
   │ 1  │ 顺丰速运  │ 标快   │ ¥18    │ 1-2 天   │ ⭐   │
   │ 2  │ 京东物流  │ 标准   │ ¥15    │ 2-3 天   │      │
   │ 3  │ DHL       │ Express│ ¥45    │ 1 天     │      │
   └────┴──────────┴────────┴────────┴──────────┴──────┘
        │
        ▼
发货员选择 → 自动回填承运商 + 服务类型 + 运费
```

### 6.3 比价缓存策略

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 缓存 TTL | 30 分钟 | 同一起止地 + 同包裹规格的比价结果缓存 |
| 缓存键 | senderHash + receiverHash + parcelsHash | SHA-256 摘要 |

### 6.4 承运商不参与比价

- `ErpLogCarrier` 的 `isActive=false` 不参与
- 可在 Carrier 上标记 `rateShoppingEnabled=true/false` 独立控制
- 实时报价失败时降级使用 CarrierConfig 配置的参考运费

---

## 七、网关日志与审计

### 7.1 ErpLogShipmentLog 记录内容

| 字段 | 取值示例 |
|------|----------|
| actionType | `COMPLETE_DELIVERY_ORDER` / `GET_LABEL` / `TRACK` / `CANCEL` / `RATE_QUOTE` |
| requestBody | `{"parcels":[...], "receiver":{...}}` |
| responseBody | `{"trackingNo":"SF123456","labelUrl":"..."}` |
| httpStatus | `200` / `500` / `401` |
| errorCode | `TIMEOUT` / `AUTH_FAILED` / `RATE_LIMITED` |
| isSuccess | `true` / `false` |
| executedAt | `2026-06-30T10:00:00` |

### 7.2 日志保留策略

| 规则 | 说明 |
|------|------|
| 保留期限 | 180 天（`erp-log.log-retention-days`） |
| 归档 | 180 天后的日志自动归档到历史表 `erp_log_shipment_log_arch` |
| 索引 | `(shipmentId, actionType)` + `(executedAt)` 复合索引 |

---

## 八、配置点汇总

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-log.enabled` | false | TMS 模块是否启用 |
| `erp-log.async-dispatch` | true | 承运商下单是否异步 |
| `erp-log.gateway-timeout-secs` | 30 | 网关调用超时（秒） |
| `erp-log.gateway-max-retries` | 3 | 网关失败最大重试次数 |
| `erp-log.retry-base-interval-secs` | 30 | 重试基础间隔（指数退避起点） |
| `erp-log.tracking-poll-cron` | `0 0 */4 * * ?` | 追踪轮询 cron |
| `erp-log.tracking-poll-max-days` | 30 | 追踪轮询最大天数 |
| `erp-log.rate-quote-cache-ttl-min` | 30 | 比价缓存 TTL（分钟） |
| `erp-log.log-retention-days` | 180 | 网关日志保留天数 |
| `erp-log.shipment-settlement-mode` | AUTO | 运费结算模式：AUTO/MANUAL |

## 九、反模式提示

- ⛔ **单层 Provider** — 丢失 per-carrier 配置化 client 能力。必须三层 Client/Factory/Registry（🟢 `DhlShipperGatewayClientFactory.java:34-47`）。
- ⛔ **网关调用阻塞主事务** — 承运商不可用时整个发运创建失败。必须异步（post-commit + nop-job）。
- ⛔ **明文存储凭证** — API 密钥必须加密存储。
- ⛔ **假设所有承运商支持回调** — 必须提供轮询兜底机制。
- ⛔ **比价结果硬编码** — 必须走实时 API 查询 + 缓存，不可写死运费表。
- ⛔ **无幂等设计** — 重试可能导致重复下单。必须用 `referenceNo` 幂等键。

## 十、证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| SPI 三层 Client/Factory/Registry | 🟢 | Metasfresh `ShipperGatewayClient.java:37-58` + `ShipperGatewayClientFactory.java:29-34` + `ShipperGatewayServicesRegistry.java:43-125` |
| per-carrier 配置化 client | 🟢 | `DhlShipperGatewayClientFactory.java:16-47` |
| 异步下单（workpackage） | 🟢 | `DeliveryOrderWorkpackageProcessor.java:103-140` |
| 中立 DTO 放 spi/model | 🟢 | Metasfresh `spi/model/` |
| 凭证加密存储 | 🟢 | Nop `EncryptionHelper` |
| 面单标签获取 | 🟢 | Metasfresh `DhlShipperGatewayClient.getPackageLabelsList` |
| Webhook 回调 | 🟢 | 本项目 `integration-pattern.md` |
| 比价（Rate Shopping） | ⚪ | 领域常识，延迟到客户需求确认 |
| 包裹拆分 | 🟢 | Metasfresh `M_Shipment_Package` |

## 参考

- `logistics/README.md` §SPI 契约（三层 SPI 总述）
- `logistics/state-machine.md`（发运单状态机，含网关异常处理）
- `logistics/use-cases.md`（用例说明）
- `model/app-erp-logistics.orm.xml`（权威 ORM 模型）
- `architecture/integration-pattern.md`（Webhook 入站签名验证）
- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2（设计依据）
