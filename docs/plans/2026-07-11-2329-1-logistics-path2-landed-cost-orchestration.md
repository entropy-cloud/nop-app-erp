# 2026-07-11-2329-1 logistics-path2-landed-cost-orchestration

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Source: Deferred item `2026-07-04-1115-3` §3.18 path-2 采购运费到岸成本分摊（trigger met by `2026-07-10-1100-3` Landed Cost engine completion）; `2026-07-10-1100-3` Follow-up §logistics path-2 运费→到岸成本单自动编排
> Related: `2026-07-04-1115-3`（path-2 事件占位）; `2026-07-10-1100-3`（Landed Cost 引擎）; `2026-07-04-1115-3` Deferred「真实承运商 HTTP」（Non-Goal，gated on vendor credentials）
> Audit: required

## Current Baseline

- **Landed Cost 引擎已就绪**（plan `2026-07-10-1100-3` completed）：`ErpInvLandedCost`/`ErpInvLandedCostLine` 头行实体 + `LandedCostAllocationEngine`（3 种分摊方法 BY_AMOUNT/BY_QUANTITY/BY_WEIGHT）+ `ErpInvLandedCostProcessor`（审核编排：分摊→CostAdjust 成本层更新→LANDED_COST(490) 过账）+ `LandedCostAcctDocProvider`。手工创建 Landed Cost → 审核分摊 → GL 凭证全链已验证（9 tests 全绿）。
- **logistics path-2 为空占位**：`ErpLogShipmentBizModel.onDelivered` PURCHASE_RECEIPT 分支（L161-166）仅构造 `ShipmentDeliveredEvent`（5 字段：shipmentId/shipmentCode/relatedBillType/relatedBillCode/carrierId）→ INFO 日志 → `freightSettlementStatus=SETTLED`。**无凭证、无 Landed Cost、无 GL 分录**。
- **ShipmentDeliveredEvent 未派发**：构造后立即丢弃，无事件总线、无监听者、无订阅者。
- **轮询路径缺失**：`scanForPolling`→`advanceTrackingViaPolling`→`advanceTracking` 将 status 翻为 DELIVERED 但**不调用 `onDelivered`**，故轮询驱动的 DELIVERED 运单不触发 path-1 过账也不触发 path-2 占位。
- **模块依赖缺失**：`erp-log-service` 对 `app-erp-inventory-dao` 仅 test-scope（pom.xml L32），无 compile-scope 依赖。
- **弱链接**：`ErpLogShipment.relatedBillCode` 承载 receive 的 **code**（String），非 `receiveId`。无 `receiveId` 列。
- **IErpInvLandedCostBiz 仅 CRUD + approve + allocate**：无自动创建/生成方法（`IErpInvLandedCostBiz.java:21-27`）。
- **IErpPurReceiveBiz** 在 `erp-pur-dao`，仅 CRUD + cancel + IApprovableBiz（`IErpPurReceiveBiz.java:16-19`）。inventory-service 已 compile-scope 依赖 purchase-dao（`ErpInvLandedCostProcessor` 经 `daoProvider.daoFor(ErpPurReceive.class)` 读 receive）。
- **ErpInvLandedCost.receiveId** 为 mandatory BIGINT FK（ORM L1322），`supplierId` 可空（L1326）。
- **ErpLogShipment 运费字段**：`freightAmount` DECIMAL(20,4)（ORM L200）、`freightCurrencyId` BIGINT（L204）、`freightTerms` VARCHAR(20)（L208）、`freightSettlementStatus` VARCHAR(20)（L212）。

## Goals

- logistics PURCHASE_RECEIPT 运单 DELIVERED 时自动创建 DRAFT 状态 `ErpInvLandedCost`（FREIGHT 成本要素行 + 运费金额），将 freight → landed cost → inventory cost allocation 链路打通。
- 修复轮询路径 `scanForPolling` DELIVERED 不调用 `onDelivered` 的遗漏。
- config-gated 向后兼容（默认关闭）。

## Non-Goals

- 真实承运商 HTTP 集成（DHL/顺丰/京东）—— gated on vendor credentials（`2026-07-04-1115-3` Deferred 保持）。
- Landed Cost 自动审核——auto-created 为 DRAFT，用户人工审核（已有 `ErpInvLandedCostProcessor.approve` 全链）。
- 运费 AP 应付凭证自动生成——Landed Cost 审核后经 `LANDED_COST(490)` 过账（借存货/贷应付）已由 1100-3 覆盖，本计划不重复。
- 多段到岸成本累计管理——同一入库单多次追加（1100-3 Deferred，gated on 多段运输分次录入需求）。
- Shipment 行级运费分摊——`ErpLogShipmentLine` 无运费字段，运费仅头级；分摊按 Landed Cost 引擎的 BY_QUANTITY/BY_AMOUNT 在审核时计算。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/logistics/state-machine.md`（path-2 偏离补注）; `docs/design/finance/costing-methods.md` §到岸成本; `docs/design/logistics/carrier-integration.md`
- Skill Selection Basis: `nop-backend-dev`（BizModel 方法 + 跨实体 I*Biz 注入 + config-gated）; 不涉及前端（`nop-frontend-dev` 不匹配）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. Mock carrier stub 已就绪（`MockCarrierGatewayClientFactory` gatewayId="mock"）。
- 新增 config key：`erp-log.path2-landed-cost-auto-create`（默认 `false`，向后兼容）。

## Execution Plan

### Phase 1 - IErpInvLandedCostBiz 扩展 + 模块依赖

Status: completed
Targets: `module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/IErpInvLandedCostBiz.java`; `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvLandedCostBizModel.java`; `module-logistics/erp-log-service/pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: none

- [x] Decision: 方法放置——在 `IErpInvLandedCostBiz` 新增 `generateFreightLandedCost` 方法，inventory 域内部解析 receiveCode→receiveId（inventory-service 已 compile-scope 依赖 purchase-dao）。logistics 仅传入运费数据，不直接依赖 purchase-dao。
  - 替代方案 A：logistics 添加 purchase-dao compile-scope 依赖自行解析 code→ID → 增加物流→采购耦合，logistics 过度膨胀
  - 替代方案 B：经事件总线异步通知 → 需共享事件类型模块 + inventory→logistics 反向依赖（环），复杂度过高
  - 选定方案：inventory I*Biz 新增方法，内部解析，logistics 仅传数据。AP partner 默认取 receive 的 supplierId（运费通常付给同一供应商），用户在 DRAFT 审核时可修改
  - Skill: `nop-backend-dev`
- [x] Add: `IErpInvLandedCostBiz.generateFreightLandedCost(@Name("receiveCode") String receiveCode, @Name("freightAmount") BigDecimal freightAmount, @Name("freightCurrencyId") Long freightCurrencyId, @Name("freightExchangeRate") BigDecimal freightExchangeRate, IServiceContext context)` → 返回 `ErpInvLandedCost`（DRAFT 状态）
  - 内部逻辑：daoFor(ErpPurReceive.class).findFirstByCode(receiveCode) → 取 receiveId/supplierId/warehouseId；构造 ErpInvLandedCost（docStatus=DRAFT, approveStatus=UNSUBMITTED, allocationMethod=BY_AMOUNT 默认, receiveId, supplierId, currencyId, exchangeRate, totalCostAmount=freightAmount, businessDate=today）；构造 ErpInvLandedCostLine（costElement=FREIGHT, amount=freightAmount, apPartnerId=supplierId）；save 落库
  - 幂等门控：同 receiveId 已有非 CANCELLED LandedCost 时抛新 ErrorCode `ERR_LANDED_COST_DRAFT_EXISTS`（区别于 approve-time `ERR_LANDED_COST_ALREADY_ALLOCATED`，避免操作员诊断歧义）
  - Skill: `nop-backend-dev`
- [x] Add: `erp-log-service/pom.xml` 新增 `app-erp-inventory-dao` compile-scope 依赖（test→compile 升级）
  - 验证无环：inventory-dao 不依赖 logistics（grep 确认零引用）
  - Skill: none
- [x] Add: `erp-log-service/pom.xml` 新增 `app-erp-inventory-service` test-scope 依赖——Phase 3 集成测试需 inv-service bean（`ErpInvLandedCostBizModel`/`ErpInvLandedCostProcessor`）在 logistics test classpath 上。现有 finance-service compile-scope 不传递 inv-service。
  - Skill: none

Exit Criteria:

> Phase 1 交付 SPI 扩展方法 + 模块依赖。解除 Phase 2 编排阻塞需验证方法可编译可达。

- [x] `IErpInvLandedCostBiz.generateFreightLandedCost` 编译通过且 `ErpInvLandedCostBizModel` 实现可注入
- [x] `erp-log-service` 对 `app-erp-inventory-dao` compile-scope 依赖生效（inventory I*Biz 接口在 logistics 编译期可见）

### Phase 2 - Freight-to-LandedCost 编排接线

Status: completed
Targets: `module-logistics/erp-log-service/src/main/java/app/erp/log/service/event/ShipmentDeliveredEvent.java`; `module-logistics/erp-log-service/src/main/java/app/erp/log/service/entity/ErpLogShipmentBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Decision`
- Prereqs: Phase 1

- [x] Add: `ShipmentDeliveredEvent` 增 `freightAmount`(BigDecimal) + `freightCurrencyId`(Long) 两字段 + 构造器/getter 扩展（用于结构化日志记录交接意图，非事件总线准备——事件保持不派发）
  - Skill: none
- [x] Add: `ErpLogShipmentBizModel` 注入 `IErpInvLandedCostBiz landedCostBiz`
  - Skill: `nop-backend-dev`
- [x] Decision: Path-2 失败语义——path-2 `generateFreightLandedCost` 失败时是否保持 `freightSettlementStatus=PENDING`（对齐 path-1 可重试语义）还是吞异常并 mark SETTLED（对齐现有 path-2 占位行为）。
  - 选定方案：保持 PENDING + ERROR 日志——path-2 失败时 `freightSettlementStatus` 保持 PENDING（不 mark SETTLED），允许后续 `scanForPolling`/webhook 重入 `onDelivered` 重试。与 path-1 失败语义一致（path-1 post 失败也保持 PENDING）。
  - 替代方案 A：吞异常 + mark SETTLED → 一旦 SETTLED，`onDelivered` 幂等守卫（L155-158）永久阻止重入，transient 失败导致 freight→landed-cost 链路永久丢失，无重试路径无监控
  - 替代方案 B：吞异常 + mark SETTLED + ERROR 级日志补偿 → 日志可观测但无自动恢复，依赖人工发现
  - 残留风险：PENDING 重试需 `scanForPolling` 或手动 webhook 重放；若运单不再有轮询/webhook 事件则卡在 PENDING 需人工介入
  - Skill: `nop-backend-dev`
- [x] Add: `onDelivered` PURCHASE_RECEIPT 分支改造——config-gated `erp-log.path2-landed-cost-auto-create`（默认 false）：若开启且 `freightAmount > 0`，调用 `landedCostBiz.generateFreightLandedCost(...)`；成功 → INFO 日志 + mark SETTLED；失败 → ERROR 日志 + 保持 PENDING（对齐上方 Decision）。若 `freightAmount` 为 null 或 ≤ 0 → mark SETTLED（无可分摊运费，视为无 path-2 职责）
  - Skill: `nop-backend-dev`
- [x] Add: 新增 `ErpLogConstants.CONFIG_PATH2_LANDED_COST_AUTO_CREATE` 常量 = `"erp-log.path2-landed-cost-auto-create"`
  - Skill: none
- [x] Fix: `ErpLogShipmentBizModel.scanForPolling`（L139-141 当前仅委托 `gatewayDispatcher.scanForPolling`）——轮询驱动 DELIVERED 翻转后补调 `onDelivered`（当前仅 `handleTrackingWebhook` L131 路径触发）。`onDelivered` 是 BizModel protected 方法，轮询结果需在 BizModel 层处理而非 Dispatcher 层。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付编排接线和轮询路径修复。解除 Phase 3 测试阻塞需验证 path-2 分支代码可达。

- [x] config-gate 开启时 PURCHASE_RECEIPT DELIVERED → `generateFreightLandedCost` 调用路径编译可达
- [x] 轮询路径 DELIVERED → `onDelivered` 调用路径编译可达（path-1 + path-2 均受益）

### Phase 3 - 集成测试 + owner doc

Status: completed
Targets: `module-logistics/erp-log-service/src/test/java/app/erp/log/service/`; `docs/design/logistics/state-machine.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add: `TestErpLogPath2LandedCost` 集成测试——配置开启 + mock carrier + PURCHASE_RECEIPT shipment（freightAmount>0, relatedBillCode=测试 receive code）→ completeShipment DELIVERED → 断言 `ErpInvLandedCost` DRAFT 创建（receiveId 匹配、costElement=FREIGHT、amount 匹配、docStatus=DRAFT、approveStatus=UNSUBMITTED）
  - 正路径 + freightAmount=null 跳过路径 + 幂等（重复 DELIVERED 拒绝）路径
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-logistics -am`（含新增测试 + 既有 logistics 14 tests 回归无失败）
  - Skill: none
- [x] Add: `docs/design/logistics/state-machine.md` path-2 偏离补注——标注 path-2 已从事件占位升级为 Landed Cost 自动编排（config-gated），引用 1100-3 引擎
  - Skill: none

Exit Criteria:

> Phase 3 交付集成测试和文档对齐。

- [x] `TestErpLogPath2LandedCost` 正路径/跳过/幂等三场景全绿
- [x] 既有 logistics 14 tests 回归无失败
- [x] `state-machine.md` path-2 补注在场

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0ae285b74ffeU4ScvPvhPrRYC0) — B1: path-2 失败语义未 adjudicate（需 Decision 记录选择/替代/残留风险）; B2: Phase 3 集成测试需 inv-service test-scope 依赖未声明; S1: 轮询修复调用点描述不准（onDelivered 在 BizModel 非 Dispatcher）; S2: 错误码复用致诊断歧义; S5: Phase 3 skill 应为 nop-testing; S6: GatewayDispatcher 包路径错误。
- Independent draft review iteration 2: accept (ses_0ae1e0aa1ffeDi2VuGOw2tbbT7) — B1/B2/S1/S2/S5/S6 全部修订正确，内部一致，失败语义残留风险在 Decision 中裁定。无新阻塞项。**草案审查收敛，状态 draft→active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [x] 范围内行为完成（path-2 auto-create + 轮询路径修复）
- [x] 相关文档对齐（`logistics/state-machine.md` path-2 补注；`1115-3`/`1100-3` Deferred 解除标注）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-logistics -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 真实承运商运费 → Landed Cost 完整 AP 流程

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期使用 mock carrier + freightAmount 字段值打通编排链路。真实承运商 HTTP 返回的运费报价自动填充 freightAmount 归真实集成 successor。
- Successor Required: yes（触发条件：具体承运商接入需求 + 凭证就绪时）

### 运费汇率解析精细化

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期 exchangeRate 简单解析（freightCurrencyId == 本位币时 rate=1，否则取 receive.exchangeRate 兜底）。多币种精细化汇率归 finance 汇兑 successor。
- Successor Required: no

## Closure

Status Note: implemented — 3 Phase 全 done。独立结束审计通过（2026-07-12）。

Closure Audit Evidence:

- Executor: EXECUTE driver (2026-07-12)
- Phase 1: `IErpInvLandedCostBiz.generateFreightLandedCost` SPI + `ErpInvLandedCostProcessor` 实现（receiveCode 解析 + DRAFT 头/行构造 + `ERR_LANDED_COST_DRAFT_EXISTS` 幂等门控）+ `erp-log-service` inventory-dao compile-scope / inventory-service test-scope 依赖
- Phase 2: `ShipmentDeliveredEvent` +freightAmount/freightCurrencyId；`ErpLogShipmentBizModel` 注入 `IErpInvLandedCostBiz` + config-gated `handlePurchaseReceiptDelivered`（成功 SETTLED / 失败 PENDING 对齐 path-1）；`ErpLogConstants.CONFIG_PATH2_LANDED_COST_AUTO_CREATE`；`GatewayDispatcher.scanForPolling` 改返回 `List<ErpLogShipment>` + BizModel 补调 `onDelivered`（仅 DELIVERED 运单）
- Phase 3: `TestErpLogPath2LandedCost` 3 场景全绿（正路径 DRAFT 创建 + freightAmount=null 跳过 + 幂等拒绝）；既有 logistics 20 tests 回归无失败（23 total）；`state-machine.md` path-2 补注
- 验证：`mvn clean install -DskipTests` 全 reactor BUILD SUCCESS；`mvn test -pl module-logistics/erp-log-service -am` 23/23 全绿
- Independent Closure Auditor: 独立子代理（新会话，非执行者上下文），2026-07-12。审计结论：**approved / 通过**。
  - 五点一致性：Plan Status=completed / 三 Phase Status=completed / 三 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / Closure 证据在场 — 全部一致。
  - Anti-Hollow 验证（实时仓库逐文件核对，非信任 `[x]`）：
    - `IErpInvLandedCostBiz.generateFreightLandedCost` SPI 在 `erp-inv-dao/.../IErpInvLandedCostBiz.java:32`（@BizMutation）；实现委托 `ErpInvLandedCostBizModel:55→ErpInvLandedCostProcessor:137` — 完整链路 loadReceiveByCode→validateNoDraftExists→createLandedCostHead(DRAFT)→createFreightLine(FREIGHT)，非空壳。
    - `ERR_LANDED_COST_DRAFT_EXISTS` 实际定义于 `ErpInvErrors.java:144`，Processor:174 抛出 — 错误码真实存在。
    - `ErpLogShipmentBizModel.handlePurchaseReceiptDelivered`（:222）config-gated 三分支真实可达：config 关→publishDeliveredEvent+SETTLED；freightAmount≤0→SETTLED；成功→SETTLED；失败→保持 PENDING+ERROR 日志（无吞异常）。`onDelivered`（:176）经 `handleTrackingWebhook`（:137）与 `scanForPolling`（:152）两路径调用 — 运行时可达。
    - `GatewayDispatcher.scanForPolling` 返回 `List<ErpLogShipment>`，BizModel :146-158 仅对 DELIVERED 运单调 `onDelivered` — 轮询路径修复真实落地。
  - 依赖无环核对：inventory-dao 的 pom 无 logistics 引用（grep 零结果），logistics→inventory 单向。
  - 验证命令重跑：`mvn test -pl module-logistics/erp-log-service -am` → `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（独立会话实跑确认，非信任日志）。
  - Docs sync：`docs/logs/2026/07-12.md` 在场；`docs/design/logistics/state-machine.md` path-2 补注在 §1(:17)/§7(:103,:110)。
  - Deferred honesty：两项 Deferred（真实承运商 HTTP / 汇率精细化）均带触发条件，非范围内缺陷隐藏。

Follow-up:

- 真实承运商 HTTP 集成 —— 触发条件：具体承运商接入需求 + 凭证就绪
