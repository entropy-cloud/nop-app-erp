# 物流/运输域（logistics）— 设计骨架

## 目的

设计运输管理（TMS）模块：发运单（Shipment）+ 承运商网关（Carrier Gateway）SPI，解决"怎么发/找谁运/运单号/面单"问题。补齐 P1 独立扩展模块缺口。

## 模块定位（Decision：独立扩展工程）

> **裁决**：TMS 定位为**可选独立扩展工程 `module-logistics`**，**不纳入 product-scope 的 10 域基线**（`product-scope.md:49-52` 延迟范围），作为可选模块按需组装。

- 工程范式参考 `docs/design/l10n/cn-golden-tax.md`（独立工程 + 凭证指针反查核心域）。
- 命名：实体 `ErpLog*`，表名 `erp_log_*`，字典 `erp-log/*`，appName `app-erp-log`。
- **考虑的替代方案**：纳入核心域子模块（拒绝，因 product-scope 明确外部集成为延迟范围）。

**实施级设计延迟声明**：本文为**设计骨架**（模块定位 + 最小实体清单 + **三层 SPI 契约**），深化到实施级延迟到**客户物流集成需求确认**时触发。

## 边界

- 本模块负责：发运单（承运商/运单号/面单/包裹）、承运商配置、承运商网关对接（下单/取面单/追踪）。
- **与 sales 的边界**：logistics 发运单 = "怎么发/找谁运/运单号/面单"；sales 出库单（`ErpSalDelivery`，`module-sales/...orm.xml:339`）= "要发什么"。两者**弱指针关联**（发运单侧反查出库单），**不在 `ErpSalDelivery` 加 carrierId**（核心零污染）。
- 本模块不负责：库存出库写账（inventory/sales 域）；客户主数据（master-data）。

## 设计依据

> 来源 `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2。**⚠️ SPI 形态修正**：初版计划单层 `ICarrierGatewayProvider` → **改三层**（Metasfresh 黄金参考）。

### 主证：🟢 Metasfresh shipper.gateway（源码全读）

**为什么必须三层而非单层**：单层 Provider 会丢失"per-carrier 配置化 client"能力（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证明每个承运商需要独立配置化 client 实例）。三层 = Client（具体承运商交互）+ ClientFactory（per-carrier 配置化创建）+ Registry（自动聚合）。

### 裁决 D3：运费双路径

| 运费类型 | 过账路径 | 凭证方向 |
|---|---|---|
| 销售运费 | sales 配送行 / `FREIGHT` 凭证 | 走 sales 域（🟢 Odoo `delivery/models/sale_order.py:13,67-72` 配送行范式） |
| 采购运费 | Landed Cost（到岸成本） | 走 finance 到岸成本分摊（`costing-methods.md:287-309`：借 存货 / 贷 应付） |

## 实体清单（最小骨架，标注延迟）

> 表前缀 `erp_log_`、类名 `ErpLog*`、字典 `erp-log/*`。以下为建议命名，待客户需求触发后落地 ORM。

### ErpLogCarrier（承运商，表 `erp_log_carrier`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| carrierName | 承运商名（顺丰/DHL/京东物流） |
| gatewayId | 网关标识（对应 ClientFactory 的 code，如 dhl/sf） |
| partnerId | 承运商往来单位（→ErpMdPartner） |
| isActive | 是否启用 |
| 标准审计字段 | |

### ErpLogCarrierConfig（承运商配置，表 `erp_log_carrier_config`）

承运商参数化配置（🟢 `ShipperConfig.java:35-47` 范式）。

| 字段 | 含义 |
|---|---|
| id/carrierId/orgId | 标准 |
| apiEndpoint | 接口地址 |
| credentials | 凭证（加密存储，OAuth2/token/apiKey） |
| trackingUrlTemplate | 追踪 URL 模板 |
| additionalProperties | 扩展参数（JSON） |
| 标准审计字段 | |

### ErpLogShipment（发运单，表 `erp_log_shipment`）

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| carrierId | 承运商（→ErpLogCarrier） |
| relatedBillType/relatedBillCode | 关联出库单弱指针（SALES_DELIVERY + ErpSalDelivery 编号，核心零污染） |
| shipmentDate | 发运日期 |
| trackingNo | 承运商运单号 |
| labelUrl | 面单 URL/文件 |
| freightAmount/freightCurrencyId | 运费 |
| status | dict `erp-log/shipment-status`：DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED |
| 标准审计字段 | |

#### ErpLogShipmentLine（发运明细）/ ErpLogShipmentParcel（包裹）/ ErpLogShipmentLog（网关交互日志）

发运单下的包裹明细与承运商网关交互记录（请求/响应报文），结构从略（骨架）。

## SPI 契约（核心交付物：三层，照搬 Metasfresh 形态）

> 这是本骨架的核心。三层 SPI 保证"新增承运商 = 1 个 bean，零改 commons"。

### 第一层：IErpLogCarrierGatewayClient（承运商网关客户端）

具体承运商交互契约（对应 🟢 `ShipperGatewayClient.java:37-58`）。

```
interface IErpLogCarrierGatewayClient {
    // 提交发运订单（下单）
    DeliveryOrderResult completeDeliveryOrder(DeliveryOrderRequest request);

    // 获取面单
    List<PackageLabel> getPackageLabelsList(String shipmentNo);

    // 预约取件/发运
    void adviseShipment(ShipmentAdvice advice);
}
```

### 第二层：IErpLogCarrierGatewayClientFactory（承运商客户端工厂）

per-carrier 配置化创建 client（对应 🟢 `ShipperGatewayClientFactory.java:29-34` + `DhlShipperGatewayClientFactory.java:16-47`）。

```
interface IErpLogCarrierGatewayClientFactory {
    String getGatewayId();                              // 如 "dhl"/"sf"
    IErpLogCarrierGatewayClient newClientForCarrierId(String carrierId);
        // 读取 ErpLogCarrierConfig，注入凭证/端点，返回配置化的 client 实例
}
```

### 第三层：ErpLogCarrierGatewayRegistry（自动聚合注册中心）

Nop IoC 自动聚合（对应 🟢 `ShipperGatewayServicesRegistry.java:43-125`，`@Inject` Map 自动收集 Factory）。

```
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
- `Address`（地址）

**新增承运商 = 1 个 `@Service` Factory bean + 对应 Client 实现，零改 commons/Registry**（🟢 `DhlShipperGatewayClientFactory.java:16-47` 范式）。

## 业务规则

1. **异步下单**：承运商网关调用走异步（post-commit + nop-job），不阻塞主事务（🟢 `DeliveryOrderWorkpackageProcessor.java:103-140`）。网关超时/失败不影响发运单创建，通过重试 + 日志兜底。
2. **核心零污染**：承运商关联全在 logistics 发运单侧（`ErpLogShipment.carrierId` + 弱指针反查出库单），**不在 `ErpSalDelivery` 加 carrierId**（反 🟢 Odoo `delivery/models/sale_order.py:13` 烘焙进销售订单污染）。
3. **运费双路径**（裁决 D3）：发运单的运费按关联单类型分流——关联销售出库 → sales FREIGHT 凭证；关联采购入库 → Landed Cost。

## 跨域协作

| 对端 | 协作方式 |
|---|---|
| sales（ErpSalDelivery） | 弱指针反查（发运单 → 出库单），核心零污染 |
| finance/costing-methods | 采购运费走 Landed Cost（`costing-methods.md:287-309`） |
| master-data（ErpMdPartner） | 承运商往来单位 |

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-log.enabled` | false | TMS 模块是否启用 |
| `erp-log.async-dispatch` | true | 承运商下单是否异步 |

## 反模式警示

- ⛔ **单层 Provider SPI**——丢失 per-carrier 配置化 client 能力（🟢 `DhlShipperGatewayClientFactory.java:34-47` 证明每个承运商需独立配置）。必须三层。
- ⛔ **Odoo 命名约定派发**（🟢 `stock_delivery/models/delivery_carrier.py:50-51` `getattr(self,'%s_send_shipping'%delivery_type)`）——反射脆弱，与 iDempiere 反射 Doc 工厂同类。本项目用类型安全 Map 注册。
- ⛔ **在 `ErpSalDelivery` 加 carrierId**（核心污染）——🟢 Odoo `delivery/models/sale_order.py:13` 反例；承运商关联在 logistics 发运单侧。

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

## 参考

- `docs/analysis/2026-06-30-0001-advanced-scenario-design-comparison.md` §3.2（设计依据）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/design/finance/costing-methods.md:287-309`（Landed Cost 采购运费）
- `docs/requirements/product-scope.md:49-52`（延迟范围）
