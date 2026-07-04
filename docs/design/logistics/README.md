# 物流/运输域（logistics）— 运输管理模块设计

## 目的

设计运输管理（TMS）模块：发运单（Shipment）+ 承运商网关（Carrier Gateway）SPI，解决"怎么发/找谁运/运单号/面单"问题。补齐 P1 独立扩展模块缺口。

## 模块定位（Decision：独立扩展工程）

> **裁决**：TMS 定位为**独立扩展工程 `module-logistics`**（逻辑工程名 `app-erp-logistics`），是 18 域正式基线之一（第二批扩展域，见 `product-scope.md`）。

- 工程范式参考 `docs/design/l10n/cn-golden-tax.md`（独立工程 + 凭证指针反查核心域）。
- 命名：实体 `ErpLog*`，表名 `erp_log_*`，字典 `erp-log/*`，appName `erp-log`（两级）。
- **考虑的替代方案**：纳入核心域子模块（拒绝，因 product-scope 明确外部集成为延迟范围）。

## 边界

- 本模块负责：发运单（承运商/运单号/面单/包裹）、承运商配置、承运商网关对接（下单/取面单/追踪）。
- **与 sales 的边界**：logistics 发运单 = "怎么发/找谁运/运单号/面单"；sales 出库单（`ErpSalDelivery`，`module-sales/...orm.xml:339`）= "要发什么"。两者**弱指针关联**（发运单侧反查出库单），**不在 `ErpSalDelivery` 加 carrierId**（核心零污染）。
- 本模块不负责：库存出库写账（inventory/sales 域）；客户主数据（master-data）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2。**SPI 形态**：Metasfresh 黄金参考三层 Client/Factory/Registry。

> **实现状态补注（plan 2026-07-04-1115-3）**：三层 SPI + 运费 path-1 过账已落地。当前仅 `mock` 承运商 stub（无外部 HTTP，覆盖全链行为验证）；真实承运商 HTTP 集成、比价生产路径、path-2 采购运费到岸成本分摊（依赖 finance Landed Cost，`costing-methods.md:40` Deferred）归 follow-up。

### 主证：🟢 Metasfresh shipper.gateway（源码全读）

**为什么必须三层而非单层**：单层 Provider 会丢失"per-carrier 配置化 client"能力（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证明每个承运商需要独立配置化 client 实例）。三层 = Client（具体承运商交互）+ ClientFactory（per-carrier 配置化创建）+ Registry（自动聚合）。

### 裁决 D3：运费双路径

| 运费类型 | 过账路径 | 凭证方向 |
|---|---|---|
| 销售运费 | sales 配送行 / `FREIGHT` 凭证 | 走 sales 域（🟢 Odoo `delivery/models/sale_order.py:13,67-72` 配送行范式） |
| 采购运费 | Landed Cost（到岸成本） | 走 finance 到岸成本分摊（`costing-methods.md:287-309`：借 存货 / 贷 应付） |

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-logistics` |
| appName | `erp-log` (两级) |
| 权威模型 | `model/app-erp-logistics.orm.xml` |
| 实体包 | `app.erp.log.dao.entity` |
| 表前缀 | `erp_log_` |
| 类名前缀 | `ErpLog*` |
| 字典命名空间 | `erp-log/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 承运商（Carrier） | 承运商主数据：顺丰/DHL/京东物流等，配置网关标识 |
| 承运商配置（CarrierConfig） | 每个承运商的 API 端点、凭证（加密）、追踪 URL 模板、扩展参数 |
| 发运单（Shipment） | 发运订单：关联出库单、承运商、运单号、面单、运费、状态 |
| 发运明细（ShipmentLine） | 发运单下具体产品行：物料、数量、包装说明 |
| 包裹（Parcel） | 物理包裹拆分：包裹编号、重量、尺寸、运单号（一单多包裹场景） |
| 网关日志（ShipmentLog） | 承运商网关交互记录：请求/响应报文、状态、错误信息 |

## 实体清单

> 表前缀 `erp_log_`、类名 `ErpLog*`、字典 `erp-log/*`。权威定义见 `model/app-erp-logistics.orm.xml`。

### ErpLogCarrier（承运商，表 `erp_log_carrier`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| carrierName | 承运商名（顺丰/DHL/京东物流） |
| carrierType | 承运商类型 dict `erp-log/carrier-type`：EXPRESS（快递）/FREIGHT（货运）/AIR（空运）/SEA（海运）/RAIL（铁路） |
| gatewayId | 网关标识（对应 ClientFactory 的 code，如 dhl/sf） |
| partnerId | 承运商往来单位（→ErpMdPartner） |
| isActive | 是否启用 |
| trackingUrlTemplate | 默认追踪 URL 模板（可被 config 覆盖） |
| maxParcelWeight | 最大包裹重量（kg） |
| supportedServiceTypes | 支持服务类型（JSON 数组） |
| 标准审计字段 | |

### ErpLogCarrierConfig（承运商配置，表 `erp_log_carrier_config`）

承运商参数化配置（🟢 `ShipperConfig.java:35-47` 范式）。每个承运商可以有多套配置（不同业务组织/不同服务类型）。

| 字段 | 含义 |
|---|---|
| id/carrierId/orgId | 标准 |
| configCode | 配置编码 |
| serviceType | 服务类型（标准/加急/经济） |
| apiEndpoint | 接口地址 |
| apiKey | API 密钥（加密存储，OAuth2/token/apiKey） |
| apiSecret | API 密钥（加密存储） |
| credentials | 完整凭证 JSON（加密存储，备用字段） |
| trackingUrlTemplate | 追踪 URL 模板（覆盖 carrier 默认） |
| printFormat | 面单打印格式 |
| additionalProperties | 扩展参数（JSON） |
| isActive | 是否启用 |
| 标准审计字段 | |

### ErpLogShipment（发运单，表 `erp_log_shipment`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| carrierId | 承运商（→ErpLogCarrier） |
| carrierConfigId | 承运商配置（→ErpLogCarrierConfig） |
| relatedBillType/relatedBillCode | 关联出库单弱指针（SALES_DELIVERY + ErpSalDelivery 编号，核心零污染） |
| shipmentDate | 发运日期 |
| trackingNo | 承运商运单号 |
| labelUrl | 面单 URL/文件 |
| freightAmount/freightCurrencyId | 运费 |
| freightSettlementStatus | 运费结算状态 dict `erp-log/settlement-status`：PENDING/SETTLED |
| totalWeight | 总重量（kg） |
| totalVolume | 总体积（m³） |
| totalParcels | 总包裹数 |
| receiverName | 收货人姓名 |
| receiverPhone | 收货人电话 |
| receiverAddress | 收货地址 |
| receiverCountry/receiverProvince/receiverCity/receiverDistrict | 收货地址拆分 |
| senderName | 发货人姓名 |
| senderPhone | 发货人电话 |
| senderAddress | 发货地址 |
| estimatedDeliveryDate | 预计送达日期 |
| actualDeliveryDate | 实际送达日期 |
| signedBy | 签收人 |
| status | dict `erp-log/shipment-status`：DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED |
| remark | 备注 |
| 标准审计字段 | |

### ErpLogShipmentLine（发运明细，表 `erp_log_shipment_line`）

| 字段 | 含义 |
|---|---|
| id/shipmentId/orgId | 标准 |
| lineNo | 行号 |
| materialId | 物料（→ErpMdMaterial） |
| quantity | 数量 |
| unit | 单位 |
| packageDescription | 包装说明 |
| 标准审计字段 | |

### ErpLogShipmentParcel（包裹，表 `erp_log_shipment_parcel`）

| 字段 | 含义 |
|---|---|
| id/shipmentId/orgId | 标准 |
| parcelNo | 包裹编号（1/2/3...或承运商子运单号） |
| trackingNo | 承运商运单号（一单多包裹各有独立单号） |
| labelUrl | 该包裹面单 URL |
| weight | 重量（kg） |
| length/width/height | 尺寸（cm） |
| declaredValue | 申报价值 |
| isActive | 是否有效 |
| 标准审计字段 | |

### ErpLogShipmentLog（网关交互日志，表 `erp_log_shipment_log`）

| 字段 | 含义 |
|---|---|
| id/shipmentId/orgId | 标准 |
| gatewayId | 网关标识 |
| actionType | 操作类型（CREATE_SHIPMENT/GET_LABEL/TRACK/CANCEL） |
| requestBody | 请求报文 |
| responseBody | 响应报文 |
| httpStatus | HTTP 状态码 |
| errorCode | 错误码 |
| errorMessage | 错误信息 |
| isSuccess | 是否成功 |
| executedAt | 执行时间 |
| 标准审计字段 | |

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

## 业务规则

### 发运单生命周期规则

1. **异步下单**：承运商网关调用走异步（post-commit + nop-job），不阻塞主事务（🟢 `DeliveryOrderWorkpackageProcessor.java:103-140`）。网关超时/失败不影响发运单创建，通过重试 + 日志兜底。
2. **核心零污染**：承运商关联全在 logistics 发运单侧（`ErpLogShipment.carrierId` + 弱指针反查出库单），**不在 `ErpSalDelivery` 加 carrierId**（反 🟢 Odoo `delivery/models/sale_order.py:13` 烘焙进销售订单污染）。
3. **运费双路径**（裁决 D3）：发运单的运费按关联单类型分流——关联销售出库 → sales FREIGHT 凭证；关联采购入库 → Landed Cost。
4. **状态一致性**：发运单状态机由本域控制，追踪更新由网关回调或定时轮询驱动，不依赖业务域事件。
5. **包裹拆分规则**：发运单可在创建时或创建后拆分为多个物理包裹（`ErpLogShipmentParcel`），每个包裹可有独立追踪单号。包裹拆分不影响发运单层面的运费计算。
6. **面单生成时机**：发运单进入 ADVISED 状态后，系统调用承运商网关获取面单 URL，存储于 `labelUrl` 及每个包裹的 `labelUrl`。面单打印失败不阻止状态迁移（可重试）。
7. **重复发运防护**：同一出库单（`relatedBillType` + `relatedBillCode`）只能创建一条非 CANCELLED 的发运单。取消后再发运需新建。
8. **运单号回写规则**：承运商网关成功下单后返回的运单号回写 `trackingNo`；若网关下单返回失败，系统自动发起重试（最多 3 次，指数退避），重试耗尽后标记网关失败并由人工干预。

### 运费规则

- **销售运费**：发运单的 `freightAmount` 在 DELIVERED 后自动生成 FREIGHT 凭证，过账到 sales 域的配送费用科目。
- **采购运费**：发运单关联采购入库时，`freightAmount` 作为到岸成本（Landed Cost）分摊到入库物料成本。
- **运费计费模式**：支持预付（PREPAID）和到付（COLLECT），由发运单 `freightTerms` 字段标识。
- **多包裹运费拆分**：当发运单有多个包裹且运费按包裹计费时，`freightAmount` 为最终总额，各包裹运费记录于 `ErpLogShipmentParcel` 的 `declaredValue` 扩展。

### 承运商集成规则

- **凭证安全**：`ErpLogCarrierConfig` 中的 `apiKey`、`apiSecret`、`credentials` 字段必须加密存储（Nop 加密组件 `EncryptionHelper`），页面展示脱敏。
- **超时配置**：网关调用默认超时 30s，可在 5-120s 范围内按承运商配置。
- **限流保护**：`ErpLogCarrierGatewayRegistry` 应对同一承运商并发调用进行限流（令牌桶），防止网关拒绝服务。
- **日志保留**：`ErpLogShipmentLog` 保留所有网关交互记录，按 `actionType` 分类索引，定期归档（>180 天）。

## 跨域协作

| 对端 | 协作方式 | 关键约束 |
|------|----------|----------|
| sales（ErpSalDelivery） | 弱指针反查（发运单 → 出库单），核心零污染 | 发运单 `relatedBillType=SALES_DELIVERY` |
| purchase（ErpPurReceipt） | 弱指针反查（发运单 → 入库单），运费入到岸成本 | 发运单 `relatedBillType=PURCHASE_RECEIPT` |
| finance/costing-methods | 采购运费走 Landed Cost（`costing-methods.md:287-309`） | 运费凭证条目不写入 logistics 域 |
| master-data（ErpMdPartner） | 承运商往来单位 | `ErpLogCarrier.partnerId` → `ErpMdPartner` |
| master-data（ErpMdMaterial） | 发运明细物料引用 | `ErpLogShipmentLine.materialId` → `ErpMdMaterial` |
| master-data（ErpMdOrganization） | 业务组织引用 | 所有实体 `orgId` → `ErpMdOrganization` |
| master-data（ErpMdEmployee） | 发运处理人（发货员） | `ErpLogShipment.shipperId` → `ErpMdEmployee` |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-log.enabled` | false | TMS 模块是否启用 |
| `erp-log.async-dispatch` | true | 承运商下单是否异步 |
| `erp-log.gateway-timeout-secs` | 30 | 网关调用超时（秒） |
| `erp-log.gateway-max-retries` | 3 | 网关失败最大重试次数 |
| `erp-log.shipment-settlement-mode` | AUTO | 运费结算模式：AUTO/MANUAL |
| `erp-log.log-retention-days` | 180 | 网关日志保留天数 |
| `erp-log.ncr-posting-mode` | MANUAL_POST | 不合格品处理后过账模式 |

## 反模式警示

- ⛔ **单层 Provider SPI**——丢失 per-carrier 配置化 client 能力（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证明每个承运商需独立配置）。必须三层。
- ⛔ **Odoo 命名约定派发**（🟢 `stock_delivery/models/delivery_carrier.py:50-51` `getattr(self,'%s_send_shipping'%delivery_type)`）——反射脆弱，与 iDempiere 反射 Doc 工厂同类。本项目用类型安全 Map 注册。
- ⛔ **在 `ErpSalDelivery` 加 carrierId**（核心污染）——🟢 Odoo `delivery/models/sale_order.py:13` 反例；承运商关联在 logistics 发运单侧。
- ⛔ **网关调用阻塞主事务**——🚫 承运商不可用时整个发运创建失败。必须异步（post-commit + nop-job 重试）。
- ⛔ **明文存储承运商凭证**——API 密钥和令牌必须加密存储，页面脱敏显示。

## 菜单归属

新增 logistics 域 TOPM「运输管理」（可选），分组：承运商、承运商配置、发运单。

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
| 运费双路径 | 🟢 | Odoo `sale_order.py:67-72` + 本项目 `costing-methods.md:287-309` |
| 本项目 ErpSalDelivery | 🟢 | `module-sales/...orm.xml:339` 实测 |
| 异步下单（Metasfresh WP) | 🟢 | `DeliveryOrderWorkpackageProcessor.java:103-140` 源码实测 |
| 包裹拆分 | 🟢 | Metasfresh `M_Shipment_Package` 范式 |
| 凭证加密存储 | 🟢 | Nop `EncryptionHelper` 平台能力 |

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2（设计依据）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/design/finance/costing-methods.md:287-309`（Landed Cost 采购运费）
- `docs/requirements/product-scope.md:49-52`（延迟范围）
- `model/app-erp-logistics.orm.xml`（权威 ORM 模型）
- `docs/design/logistics/state-machine.md`（发运单状态机）
- `docs/design/logistics/use-cases.md`（用例说明）
