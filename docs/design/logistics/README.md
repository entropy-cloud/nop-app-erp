# 物流/运输域（logistics）— 运输管理模块设计

## 目的

设计运输管理（TMS）模块：发运单（Shipment）+ 承运商网关（Carrier Gateway）SPI，解决"怎么发/找谁运/运单号/面单"问题。补齐独立扩展模块缺口。

## 模块定位（Decision：独立扩展工程）

> **裁决**：TMS 定位为**独立扩展工程 `module-logistics`**（逻辑工程名 `app-erp-logistics`），是 18 域正式基线之一（第二批扩展域，见 `../requirements/product-scope.md`）。

- 工程范式参考 `../l10n/cn-golden-tax.md`（独立工程 + 凭证指针反查核心域）。
- **考虑的替代方案**：纳入核心域子模块（拒绝，因 product-scope 明确外部集成为延迟范围）。

## 边界

- 本模块负责：发运单（承运商/运单号/面单/包裹）、承运商配置、承运商网关对接（下单/取面单/追踪）。
- **与 sales 的边界**：logistics 发运单 = "怎么发/找谁运/运单号/面单"；sales 出库单（`ErpSalDelivery`）= "要发什么"。两者**弱指针关联**（发运单侧反查出库单），**不在 `ErpSalDelivery` 加 carrierId**（核心零污染）。
- 本模块不负责：库存出库写账（inventory/sales 域）；客户主数据（master-data）。
- 持久化字段、字典、状态码以 `module-logistics/model/app-erp-logistics.orm.xml` 为权威源。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 运费双路径裁决（D3）

| 运费类型 | 过账路径 | 凭证方向 |
|---|---|---|
| 销售运费 | sales 配送行 / `FREIGHT` 凭证 | 走 sales 域（配送行范式） |
| 采购运费 | Landed Cost（到岸成本） | 走 finance 到岸成本分摊（借 存货 / 贷 应付） |

> **基线范围**：承运商网关三层 SPI + 运费 path-1（销售运费）过账为产品基线。承运商以 `mock` stub 覆盖全链行为验证（无外部 HTTP）；真实承运商 HTTP 集成、比价生产路径、path-2 采购运费到岸成本分摊（依赖 finance Landed Cost，Deferred）为后续范围。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-logistics` |
| appName | `erp-log`（两级） |
| 权威模型 | `module-logistics/model/app-erp-logistics.orm.xml` |
| 实体包 | `app.erp.log.dao.entity` |
| 表前缀 | `erp_log_` |
| 类名前缀 | `ErpLog*` |
| 字典命名空间 | `erp-log/*` |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| 承运商（ErpLogCarrier） | 承运商主数据：顺丰/DHL/京东物流等，配置网关标识、承运商类型（快递/货运/空运/海运/铁路）、往来单位、默认追踪 URL 模板、最大包裹重量、支持服务类型 |
| 承运商配置（ErpLogCarrierConfig） | 每个承运商的参数化配置：服务类型、接口地址、加密存储的凭证（OAuth2/token/apiKey）、追踪 URL 模板、面单打印格式、扩展参数。一个承运商可有多套配置（不同组织/服务类型） |
| 发运单（ErpLogShipment） | 发运订单：关联出库/入库单（弱指针）、承运商及其配置、运单号、面单、运费及结算状态、收/发货人信息与地址、预计/实际送达、签收人、状态 |
| 发运明细（ErpLogShipmentLine） | 发运单下具体产品行：物料、数量、单位、包装说明 |
| 包裹（ErpLogShipmentParcel） | 物理包裹拆分：包裹编号、重量、尺寸（长宽高）、独立运单号、面单 URL、申报价值（一单多包裹场景） |
| 网关日志（ErpLogShipmentLog） | 承运商网关交互记录：网关标识、操作类型（下单/取面单/追踪/取消）、请求/响应报文、HTTP 状态、错误信息、是否成功、执行时间 |

字段、类型、精度、字典码以 `module-logistics/model/app-erp-logistics.orm.xml` 为权威源。

## 状态机

发运单状态：`DRAFT → ADVISED → DISPATCHED → IN_TRANSIT → DELIVERED`（或 `CANCELLED`）。运费结算状态：`PENDING → SETTLED`。详细规则见 [`state-machine.md`](state-machine.md)。

## 承运商网关 SPI

承运商网关采用三层 SPI（Client / ClientFactory / Registry），保证"新增承运商 = 1 个 bean，零改 commons"。三层接口签名、Registry 实现、中立 DTO 包、承运商集成规则与反模式警示详见 [`../../architecture/logistics-integration.md`](../../architecture/logistics-integration.md)。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 销售出库发运 | sales（ErpSalDelivery） | 弱指针反查（发运单 → 出库单），核心零污染：发运单 `relatedBillType=SALES_DELIVERY` |
| 采购入库发运 | purchase（ErpPurReceipt） | 弱指针反查（发运单 → 入库单），运费入到岸成本：发运单 `relatedBillType=PURCHASE_RECEIPT` |
| 采购运费分摊 | finance/costing-methods | 采购运费走 Landed Cost；运费凭证条目不写入 logistics 域 |
| 承运商往来单位 | master-data（ErpMdPartner） | `ErpLogCarrier.partnerId` → `ErpMdPartner` |
| 发运明细物料 | master-data（ErpMdMaterial） | `ErpLogShipmentLine.materialId` → `ErpMdMaterial` |
| 业务组织 | master-data（ErpMdOrganization） | 所有实体 `orgId` → `ErpMdOrganization` |
| 发运处理人 | master-data（ErpMdEmployee） | `ErpLogShipment.shipperId` → `ErpMdEmployee` |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

### 发运单生命周期规则

1. **异步下单**：承运商网关调用走异步（post-commit + nop-job），不阻塞主事务。网关超时/失败不影响发运单创建，通过重试 + 日志兜底。
2. **核心零污染**：承运商关联全在 logistics 发运单侧（`ErpLogShipment.carrierId` + 弱指针反查出库单），**不在 `ErpSalDelivery` 加 carrierId**。
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

承运商凭证安全、超时配置、限流保护、日志保留等技术集成规则详见 [`../../architecture/logistics-integration.md`](../../architecture/logistics-integration.md)。

## 配置点

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `erp-log.enabled` | false | TMS 模块是否启用 |
| `erp-log.async-dispatch` | true | 承运商下单是否异步 |
| `erp-log.gateway-timeout-secs` | 30 | 网关调用超时（秒） |
| `erp-log.gateway-max-retries` | 3 | 网关失败最大重试次数 |
| `erp-log.shipment-settlement-mode` | AUTO | 运费结算模式：AUTO/MANUAL |
| `erp-log.log-retention-days` | 180 | 网关日志保留天数 |

## 菜单归属

新增 logistics 域 TOPM「运输管理」（可选），分组：承运商、承运商配置、发运单。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、运输管理模型、运费双路径、跨域协作 |
| `state-machine.md` | 发运单状态机 |
| `carrier-integration.md` | 承运商集成细节 |
| `delivery-window.md` | 交付时间窗 |
| `use-cases.md` | 用例说明 |
| `ui-patterns.md` | 页面与交互模式 |

## 参考

- `docs/architecture/logistics-integration.md`（承运商网关三层 SPI 契约 + 集成规则 + 反模式）
- `docs/design/l10n/cn-golden-tax.md`（独立工程范式样板）
- `docs/design/finance/costing-methods.md`（Landed Cost 采购运费）
- `docs/design/logistics/state-machine.md`（发运单状态机）
- `docs/design/logistics/use-cases.md`（用例说明）
