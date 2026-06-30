# 物流/运输域用例

> 本文描述物流/运输管理模块的核心业务用例。每个用例覆盖触发场景、正常流程、异常路径和涉众。

## UC-LOG-01 发运单创建

| 项目 | 内容 |
|------|------|
| **涉众** | 发货员、销售出库单 |
| **触发条件** | 发货员选择销售出库单并发起发运，或系统在销售出库审核后自动创建发运草稿 |
| **前置条件** | 出库单状态为已审核、库存已出账 |
| **正常流程** | 1. 发货员选择关联出库单（`relatedBillType` + `relatedBillCode`）<br>2. 系统校验该出库单尚未创建非 CANCELLED 发运单（重复发运防护）<br>3. 发货员填写发运信息：承运商、收货地址、包裹信息<br>4. 系统自动生成发运单号（`code`），状态 DRAFT<br>5. 发货员可添加发运明细行（物料 + 数量）、包裹拆分 |
| **后置条件** | 发运单在 DRAFT 状态，等待确认发运 |
| **异常路径** | 重复发运：系统拒绝，提示"该出库单已存在有效发运单"<br>承运商未配置：提示"请先配置承运商" |
| **补充说明** | DRAFT 状态发运单可编辑、可删除（逻辑删除）。超过 24 小时未确认的 DRAFT 发运单触发升级通知。 |

## UC-LOG-02 承运商派发

| 项目 | 内容 |
|------|------|
| **涉众** | 发货员、承运商网关（IErpLogCarrierGatewayClient） |
| **触发条件** | 发货员确认发运（从 DRAFT 提交到 ADVISED） |
| **前置条件** | 承运商有效且已配置、发运单信息完整 |
| **正常流程** | 1. 发货员点击"确认发运"<br>2. 状态迁移 DRAFT → ADVISED<br>3. 系统异步调用承运商网关 `adviseShipment()`（post-commit + nop-job）<br>4. 网关返回成功 → 获取运单号（`trackingNo`）和面单 URL（`labelUrl`）<br>5. 状态迁移 ADVISED → DISPATCHED<br>6. 生成网关日志（`ErpLogShipmentLog`）标记成功 |
| **后置条件** | 发运单状态 DISPATCHED，运单号已回写，关联出库单锁定 |
| **异常路径** | 网关超时/失败：自动重试（最多 3 次，指数退避）<br>重试耗尽：保留 ADVISED，标记网关异常，通知发货员人工处理<br>承运商拒接：保留 ADVISED，通知更换承运商 |
| **补充说明** | 异步调用不阻塞主事务——发运单创建和状态迁移立即完成，网关调用在后台执行。面单获取如失败可单独重试（`getPackageLabelsList`），不影响 DISPATCHED 状态。 |

## UC-LOG-03 追踪更新

| 项目 | 内容 |
|------|------|
| **涉众** | 承运商网关回调、系统追踪服务、客户 |
| **触发条件** | 承运商网关回调追踪端点，或定时任务轮询网关追踪接口 |
| **前置条件** | 发运单状态为 DISPATCHED 或 IN_TRANSIT |
| **正常流程** | 1. 承运商扫描/更新物流状态<br>2. 网关回调本系统暴露的追踪端点，或本系统定时轮询追踪接口<br>3. 系统解析追踪结果，更新 `ErpLogShipment` 的 `trackingNo` 关联信息<br>4. 如状态变为"已签收"：IN_TRANSIT → DELIVERED，记录 `actualDeliveryDate` 和 `signedBy`<br>5. 如状态仍在途：更新追踪信息，记录 `ErpLogShipmentLog`<br>6. DELIVERED 后触发运费过账流程 |
| **后置条件** | 发运单状态同步更新 |
| **异常路径** | 追踪长时间无更新（超过预计送达日期 3 天）：系统标记"追踪异常"，通知物流主管<br>部分签收：状态保持 IN_TRANSIT，记录当前已签收信息<br>货物退回：根据退回原因决定进入 CANCELLED（需审批） |
| **补充说明** | 追踪回调端点需认证和签名验证，防止伪造追踪数据。定时轮询间隔可配置（默认 4 小时）。 |

## 用例四：运费过账（UC-LOG-04）

| 项目 | 内容 |
|------|------|
| **涉众** | 财务、销售域、采购域 |
| **触发条件** | 发运单 DELIVERED 后，运费过账触发 |
| **前置条件** | 发运单状态为 DELIVERED、`freightAmount` 不为空 |
| **正常流程** | 1. 发运单进入 DELIVERED<br>2. 系统判断关联单类型：<br>   - `relatedBillType=SALES_DELIVERY` → 走销售运费 FREIGHT 凭证<br>   - `relatedBillType=PURCHASE_RECEIPT` → 走采购到岸成本分摊（Landed Cost）<br>3. 系统发布 `ShipmentDeliveredEvent`，finance 域订阅<br>4. finance 域执行过账：生成凭证（销售）或触发到岸成本计算（采购）<br>5. `freightSettlementStatus` 更新为 SETTLED |
| **后置条件** | 运费已过账，凭证已生成 |
| **异常路径** | 运费未设置：发运单 DELIVERED 时 `freightAmount` 为空 → 人工补充运费后手动触发过账<br>过账失败：生成凭证异账 → 保留 PENDING 状态，人工修正后重试 |
| **补充说明** | 过账模式可配置：AUTO_POST（自动）或 MANUAL_POST（人工确认后触发）。采购运费作为到岸成本分摊到入库物料成本，详见 `costing-methods.md:287-309`。 |

## 用例五：承运商集成（UC-LOG-05）

| 项目 | 内容 |
|------|------|
| **涉众** | 系统管理员、承运商配置人员 |
| **触发条件** | 新增承运商或修改现有承运商配置 |
| **前置条件** | 使用者有系统管理权限 |
| **正常流程** | 1. 管理员创建 `ErpLogCarrier`：填写承运商名称、选择 `gatewayId`（对应 ClientFactory）<br>2. 管理员创建 `ErpLogCarrierConfig`：配置 API 端点、凭证、服务类型<br>3. 凭证输入时系统自动加密存储（`EncryptionHelper`），页面脱敏显示<br>4. 管理员可进行连通性测试：系统调用 `IErpLogCarrierGatewayClient. adviseShipment()` 或 `trackShipment()` 验证配置<br>5. 配置生效后，前端发运单可选择该承运商 |
| **后置条件** | 承运商可被发运单使用 |
| **异常路径** | 连通性测试失败：显示错误详情（连接超时、认证失败），网络类故障建议检查端点，认证类故障建议检查凭证<br>同一 carrier 多配置时选择冲突：发运单需选择具体配置（`carrierConfigId`） |
| **补充说明** | 新增承运商 = 1 个 `@Service` Factory bean + 对应 Client 实现，零改 commons/Registry。`gatewayId` 作为 `IErpLogCarrierGatewayFactory.getGatewayId()` 返回值，唯一标识。 |

## UC-LOG-07 配送时间窗口管理

| 项目 | 内容 |
|------|------|
| **涉众** | 发货员、客户、配送调度员 |
| **触发条件** | 发货员预约配送时段或客户自助选择配送窗口 |
| **前置条件** | 客户已配置配送时间窗口（`ErpLogDeliveryWindow`） |
| **正常流程** | 1. 发货员/客户发起配送时段预约<br>2. 系统展示该客户可用窗口（按星期过滤 + 容量检查）<br>3. 选择时段后校验 `currentBooked < maxCapacity`<br>4. 确认预约 → `currentBooked += 1`，发运单关联预约记录<br>5. 配送执行后 → 预约状态更新为 ARRIVED/DELIVERED<br>6. 发运单取消/完成 → 释放预约 `currentBooked -= 1` |
| **后置条件** | 预约关联到发运单，容量更新 |
| **异常路径** | 容量不足：拒绝预约，提示客户选择其他时段<br>爽约（MISSED）：触发爽约费计算，`priorityScore` 提升获得优先重新预约权<br>窗口过期：`effectiveTo` 到期后自动失效，不再参与预约选择<br>重复预约：同一发运单不可重复预约（幂等校验） |
| **补充说明** | 窗口定义维度：客户 × 星期 × 时间段。`allowedShipmentTypes` 可过滤允许的发运类型（如仅销售送货、仅退货取件）。爽约费金额从系统参数配置读取。 |

---

## 用例六：签收确认（UC-LOG-06）

| 项目 | 内容 |
|------|------|
| **涉众** | 收货人、发货员、承运商 |
| **触发条件** | 货物送达收货地址，客户签收 |
| **前置条件** | 发运单状态为 IN_TRANSIT |
| **正常流程** | 1. 承运商送达货物，客户签收<br>2. 系统通过以下任一渠道获取签收信息：<br>   - 承运商网关回调（自动）<br>   - 承运商回传签收单/POD（人工上传）<br>   - 发货员确认（线下得知后手动操作）<br>3. 记录 `actualDeliveryDate`、`signedBy`、`signatureImage`（如支持）<br>4. 状态迁移 IN_TRANSIT → DELIVERED<br>5. 如关联销售出库单：通知 sales 域更新订单交付状态<br>6. 触发运费过账（UC-LOG-04） |
| **后置条件** | 发运单闭环 |
| **异常路径** | 货物损坏/短少：客户拒签 → 承运商退回 → 走 IN_TRANSIT→CANCELLED（退货审批）<br>客户仅签收部分货物：记录当前签收数量，状态保持 IN_TRANSIT<br>签收信息不一致（系统 vs 实际）：发货员人工修正签收记录 |
| **补充说明** | POD（交付证明）作为附件存储在 `ErpLogShipment` 的 `labelUrl` 扩展字段或附件关联。签收时间作为运费过账基准时间。 |
