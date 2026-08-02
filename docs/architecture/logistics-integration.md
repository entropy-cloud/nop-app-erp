# 物流集成 / 承运商网关（集成层）

## 目的

设计运输管理（TMS）的承运商网关对接契约层：三层承运商网关 SPI（Client / ClientFactory / Registry）+ 中立 DTO 包。本文承载集成契约层，业务语义见 `docs/design/logistics/README.md`。

## 模块定位（Decision：独立扩展工程）

> **裁决**：TMS 定位为**独立扩展工程 `module-logistics`**（逻辑工程名 `app-erp-logistics`），是 18 域正式基线之一（第二批扩展域，见 `product-scope.md`）。

- 工程范式参考 `docs/design/l10n/cn-golden-tax.md`（独立工程 + 凭证指针反查核心域）。
- 命名：实体 `ErpLog*`，表名 `erp_log_*`，字典 `erp-log/*`，appName `erp-log`（两级）。
- **本文档位置**：本架构文档描述承运商网关集成契约（SPI/Registry/DTO）；实体表前缀归 `module-logistics` 工程。
- **考虑的替代方案**：纳入核心域子模块（拒绝，因 product-scope 明确外部集成为延迟范围）。

## 边界

- 本模块负责：承运商网关 SPI、承运商配置参数化、中立 DTO 契约。
- **核心零污染**：全程弱指针反查 sales/purchase，核心域零字段新增（凭证指针模式）。
- 本模块不负责：业务单据本身（sales/purchase 域）；库存写入（inventory 域）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2。**SPI 形态**：Metasfresh 黄金参考三层 Client/Factory/Registry。

### 主证：🟢 Metasfresh shipper.gateway（源码全读）

**为什么必须三层而非单层**：单层 Provider 会丢失"per-carrier 配置化 client"能力（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证明每个承运商需要独立配置化 client 实例）。三层 = Client（具体承运商交互）+ ClientFactory（per-carrier 配置化创建）+ Registry（自动聚合）。

## SPI 契约（核心交付物：三层，照搬 Metasfresh 形态）

> 三层 SPI 保证"新增承运商 = 1 个 bean，零改 commons"。

### 第一层：IErpLogCarrierGatewayClient（承运商网关客户端）

具体承运商交互契约（对应 🟢 `ShipperGatewayClient.java:37-58`）。

```java
interface IErpLogCarrierGatewayClient {
    // 提交发运订单（下单）
    DeliveryOrderResult completeDeliveryOrder(DeliveryOrderRequest request);

    // 获取面单
    List<PackageLabel> getPackageLabelsList(String shipmentNo);

    // 预约取件/发运
    void adviseShipment(ShipmentAdvice advice);

    // 追踪
    TrackingResult trackShipment(String trackingNo);

    // 取消发运
    void cancelShipment(String shipmentNo);
}
```

### 第二层：IErpLogCarrierGatewayClientFactory（承运商客户端工厂）

per-carrier 配置化创建 client（对应 🟢 `ShipperGatewayClientFactory.java:29-34` + `DhlShipperGatewayClientFactory.java:16-47`）。

```java
interface IErpLogCarrierGatewayClientFactory {
    String getGatewayId();                              // 如 "dhl"/"sf"
    IErpLogCarrierGatewayClient newClientForCarrierId(String carrierId);
        // 读取 ErpLogCarrierConfig，注入凭证/端点，返回配置化的 client 实例
}
```

### 第三层：ErpLogCarrierGatewayRegistry（自动聚合注册中心）

Nop IoC 自动聚合（对应 🟢 `ShipperGatewayServicesRegistry.java:43-125`，`@Inject` Map 自动收集 Factory）。

```java
@Component
class ErpLogCarrierGatewayRegistry {
    @Inject Map<String, IErpLogCarrierGatewayClientFactory> factories;  // 按 gatewayId 自动聚合

    IErpLogCarrierGatewayClient getClient(String carrierId) {
        ErpLogCarrier carrier = ...;                      // 查承运商
        IErpLogCarrierGatewayClientFactory factory = factories.get(carrier.gatewayId);
        return factory.newClientForCarrierId(carrierId);  // 配置化创建
    }
}
```

### 中立 DTO 包（承运商无关，对应 🟢 Metasfresh `spi/model/`）

放 logistics-service 的 `spi/model/` 包，承运商无关 POJO：

- `DeliveryOrderRequest` / `DeliveryOrderResult`（发运下单请求/结果）
- `PackageLabel`（面单）
- `ShipmentAdvice`（预约取件）
- `TrackingResult`（追踪结果）
- `Address`（地址）
- `ParcelInfo`（包裹信息）

**新增承运商 = 1 个 `@Service` Factory bean + 对应 Client 实现，零改 commons/Registry**（🟢 `DhlShipperGatewayClientFactory.java:16-47` 范式）。

## 承运商集成规则

- **凭证安全**：`ErpLogCarrierConfig` 中的 `apiKey`、`apiSecret`、`credentials` 字段必须加密存储（Nop 加密组件 `EncryptionHelper`），页面展示脱敏。
- **超时配置**：网关调用默认超时 30s，可在 5-120s 范围内按承运商配置。
- **限流保护**：`ErpLogCarrierGatewayRegistry` 应对同一承运商并发调用进行限流（令牌桶），防止网关拒绝服务。
- **日志保留**：`ErpLogShipmentLog` 保留所有网关交互记录，按 `actionType` 分类索引，定期归档（>180 天）。

## 反模式警示

- ⛔ **单层 Provider SPI**——丢失 per-carrier 配置化 client 能力（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证明每个承运商需独立配置）。必须三层。
- ⛔ **Odoo 命名约定派发**（🟢 `stock_delivery/models/delivery_carrier.py:50-51` `getattr(self,'%s_send_shipping'%delivery_type)`）——反射脆弱，与 iDempiere 反射 Doc 工厂同类。本项目用类型安全 Map 注册。
- ⛔ **在 `ErpSalDelivery` 加 carrierId**（核心污染）——🟢 Odoo `delivery/models/sale_order.py:13` 反例；承运商关联在 logistics 发运单侧。
- ⛔ **网关调用阻塞主事务**——承运商不可用时整个发运创建失败。必须异步（post-commit + nop-job 重试）。
- ⛔ **明文存储承运商凭证**——API 密钥和令牌必须加密存储，页面脱敏显示。

## 证据强度标注

| 证据 | 强度 | 说明 |
|---|---|---|
| SPI 三层形态（Client/Factory/Registry） | 🟢 | Metasfresh `ShipperGatewayClient.java:37-58` + `ShipperGatewayClientFactory.java:29-34` + `ShipperGatewayServicesRegistry.java:43-125` 源码实测 |
| per-carrier 配置化 client | 🟢 | `DhlShipperGatewayClientFactory.java:16-47` 源码实测 |
| 中立 DTO 放 SPI 模块 | 🟢 | Metasfresh `spi/model/` 源码实测 |
| 承运商配置参数化 | 🟢 | `ShipperConfig.java:35-47` 源码实测 |
| 异步下单 | 🟢 | `DeliveryOrderWorkpackageProcessor.java:103-140` 源码实测 |
| Odoo 命名约定派发（反模式） | 🟢 | `stock_delivery/models/delivery_carrier.py:50-51` 源码实测 |
| Odoo sale.order.carrier_id（反模式） | 🟢 | `delivery/models/sale_order.py:13` 源码实测 |
| 运费双路径 | 🟢 | Odoo `sale_order.py:67-72` + 本项目 `costing-methods.md` Landed Cost |
| 本项目 ErpSalDelivery | 🟢 | `module-sales/...orm.xml` 实测 |
| 包裹拆分 | 🟢 | Metasfresh `M_Shipment_Package` 范式 |
| 凭证加密存储 | 🟢 | Nop `EncryptionHelper` 平台能力 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2（设计依据）
- `docs/design/logistics/README.md`（业务语义：发运单生命周期、运费规则、跨域协作）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/design/finance/costing-methods.md`（Landed Cost 采购运费）
- `model/app-erp-logistics.orm.xml`（权威 ORM 模型）
