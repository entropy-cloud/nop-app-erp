# 2026-08-14-0456-3-erplog-shipment-state-machine-bean 物流域 ErpLogShipment.status 实体级状态机 Bean（M4.57）

> Plan Status: active
> Review Hold: §11.2 M4 (i) plan-first 人工/owner-doc 门控**已于 2026-08-14 经人工确认解除**（见 Draft Review Record 门控确认记录）（DELIVERED 触发 FREIGHT 运费过账 path-1 + config-gated 到岸成本自动创建 path-2）。门控非起草者/审查者可自主解除——经人工确认解除；已转 `active` 进入实施。
> Last Reviewed: 2026-08-14
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.57（ErpLogShipment.status），plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（:329 + 风险展开 :452）
> Related: M4 同结构先例 `2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`（M4.29 StockMove done，`posted` 不入轴 + Facade 接线范式）+ `2026-08-14-0930-2-quality-m4-state-machine-beans.md`（M4.58-62 Quality done）；M0.1 契约 + M1.3 批量迁移模板固化于 `docs/architecture/entity-state-machine-bean.md §11`
> Mission: entity-state-machine
> Work Item: M4.57
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。DELIVERED 触发 path-1 FREIGHT 运费过账（`IErpFinVoucherBiz.post`，直接调用 `InvPostingExecutor` 范式）+ path-2 config-gated 到岸成本自动创建（`IErpInvLandedCostBiz.generateFreightLandedCost`，默认 OFF）。声明 §11.2 M4 硬约束：(i) plan-first；(ii) 过账时序/编排/失败回退不改；(iii) `posted` 不入轴；(iv) 跨域副作用保留原 Processor/`I*Biz` 路径；(v) 既有红冲闭环不改。
>
> **规则 14 bundling 声明**：M4.57 为 logistics 域唯一状态轴工作项（单实体单轴），无需 bundling。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md`（:329 + 风险展开 :452）+ 实仓核实。M4.29 StockMove Bean 已落地 done，`ErpInvStockMoveStateMachine` + Facade 接线范式是本计划的**直接参照模板**（同为 status 轴 + Facade 集中守卫 + DELIVERED/DONE 过账）。

- **ErpLogShipment**（M4.57 status，单轴，GatewayDispatcher Facade 集中守卫）：
  - **status 6 态**（`erp-log/shipment-status`，`app-erp-logistics.orm.xml:39-46`）：DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED。常量 `ErpLogConstants:9-14`。
  - **writer（全部集中在 `GatewayDispatcher`，5 call-sites）**：`advise:73` 写 ADVISED（DRAFT→ADVISED，幂等 short-circuit if already ADVISED）；`writeBackSuccess:319`（private，`completeShipment` 调用）写 DISPATCHED（ADVISED→DISPATCHED，网关下单成功后）；`advanceTracking:179` 写 IN_TRANSIT（DISPATCHED→IN_TRANSIT，TRACKING_EVENT_IN_TRANSIT/PICKED_UP）；`advanceTracking:168` 写 DELIVERED（→DELIVERED，TRACKING_EVENT_DELIVERED）；`cancelShipment:153` 写 CANCELLED（多源 DRAFT/ADVISED/DISPATCHED/IN_TRANSIT→CANCELLED）。
  - **关键架构事实——守卫在 Facade 不在 Processor**：6 per-mutation Processor（Advise/CompleteShipment/CancelShipment/HandleTrackingWebhook/ScanForPolling/Save）均委托 `GatewayDispatcher` 公共方法（Advise/CompleteShipment/CancelShipment 为单行委托；HandleTrackingWebhook/ScanForPolling 含 HMAC/解析/轮询逻辑后委托 `advanceTracking`/`onDelivered`）。**Processor 本身不含内联矩阵守卫**——所有 status 逻辑集中在 `GatewayDispatcher`（431 行）。这与 inventory `ErpInvStockMoveProcessor`（Processor 持守卫）不同。**Bean 接入点 = `GatewayDispatcher`**（注入 Bean + 替换内联 `Objects.equals` 链）。
  - **幂等 short-circuit vs 迁移边（关键 Decision）**：`advise:72-75` if already ADVISED → return（幂等）；`completeShipment:87-98` if DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED → return（幂等）；`cancelShipment:134-136` if CANCELLED/DELIVERED → return（幂等）；`advanceTracking:165-166` if already DELIVERED → return false（幂等）。这些是**动态流程控制**（非纯迁移边），Decision 裁定保留在 Dispatcher 不入 Bean。
  - **advanceTracking DELIVERED 无来源态守卫（doc vs code drift 候选）**：`advanceTracking:168` 写 DELIVERED 时**无 `Objects.equals(from, CONST)` 来源态守卫**——任何非 DELIVERED 状态均可被推进到 DELIVERED。owner-doc §2 迁移表（state-machine.md :49）仅声明 `IN_TRANSIT→DELIVERED`。四方对照须裁定此 drift：分类为 implementation drift（code 比 doc 宽松）或 intentional legacy（网关回调可达性宽于理想迁移图），并登记 successor。
  - **领域错误码**：`ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION`（`erp.err.log.shipment-illegal-transition`，`ErpLogErrors:32`，参数 shipmentCode + currentStatus + expectedStatus）；`ERR_LOG_SHIPMENT_ALREADY_DELIVERED`（`erp.err.log.shipment-already-delivered`，`ErpLogErrors:43`，参数 shipmentCode，`AbstractErpLogShipmentDeliveredProcessor.onDelivered:77` 终态守卫）。
  - **既有测试（layer 3 基线）**：`TestErpLogShipmentGateway`（`testFullStateMachineFlow:68` DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED；`testCancelShipment:181` ADVISED→CANCELLED；gateway retry/dead-letter/webhook）+ `TestErpLogShipmentPostingEnd`（path-1/path-2 e2e）+ `TestErpLogFreightPosting`（duplicate/settled/idempotent）+ `TestErpLogPath2LandedCost`（auto-create/idempotent）+ `TestErpLogCarrierGatewayIntegration`（polling/webhook）+ `TestErpLogShipmentTrackingNoUk` + `TestErpLogShipmentCrudSmoke` + `TestLogPostingFaultInjection`。
  - **副作用（DELIVERED）**：path-1 SALES_DELIVERY → `IErpFinVoucherBiz.post(FREIGHT)` + `markSettled`（`freightSettlementStatus=SETTLED`）；path-2 PURCHASE_RECEIPT → config-gated `IErpInvLandedCostBiz.generateFreightLandedCost`（默认 OFF）。`posted` 列存在（`:209`）但 Shipment 当前无独立 `setPosted` writer（freight settlement 经 `freightSettlementStatus` 独立轴，非 posted）。
  - **无矩阵测试**。

- **既有 Bean 注册**：`_vfs/erp/log/beans/app-service.beans.xml`（52 行）——`GatewayDispatcher`（L27-28）+ 6 Processor（L39-50）+ `LogisticsFreightProvider`（L32-33）+ `ErpLogCarrierGatewayRegistry`（L14-20）已注册。**Shipment SM Bean 未注册**（greenfield）。
- **M4.29 接线模板（直接参照）**：`ErpInvStockMoveStateMachine`（168 行，status 轴，cancel 多源，`posted` 不入轴，抛 common `ERR_ILLEGAL_STATUS_TRANSITION` + `action` 参数）；`ErpInvStockMoveProcessor:101-135`（try/catch Bean → map to domain ErrorCode with common as cause）。**差异**：logistics 守卫在 Facade（GatewayDispatcher），inventory 在 Processor——Bean 接入点不同，但 Bean 形状一致。
- **common 层非法迁移码**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`（M4.29 已复用）。
- **合规基线**：`docs/audits/compliance-baseline.md` R5=0、R11=0。
- **owner doc 覆盖**：`docs/design/logistics/state-machine.md` §适用对象（Shipment 6 态完整 + §2 迁移表 + §实现约定 path-1/path-2 + Deferred 标注 P1-MA2-078 cancel 审批 + P1-MA2-079 部分签收）。**owner doc 已覆盖**——四方对照直接对照 §2 迁移表。

## Goals

- 为 ErpLogShipment 的 status 轴落地一个实体级 `ErpLogShipmentStateMachine` Bean，严格无状态，承载 5 命名动作迁移矩阵 + 终态/初始态分类 + 只读 `transitions()` 元数据。
  - 5 动作：advise DRAFT→ADVISED、completeShipment ADVISED→DISPATCHED、advanceToInTransit DISPATCHED→IN_TRANSIT、advanceToDelivered {ADVISED,DISPATCHED,IN_TRANSIT}→DELIVERED（**刻意收紧**——见 Decision (C)，引入 Bean 守卫排除 DRAFT/CANCELLED 等非法来源态）、cancelShipment {DRAFT,ADVISED,DISPATCHED,IN_TRANSIT}→CANCELLED。
- 将固定来源态/目标态判断改调 Bean：`GatewayDispatcher` 内联 `Objects.equals` 守卫 → Bean `assertCanXxx`（try/catch common 码 → cause-chain 领域码 `ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION`）。**幂等 short-circuit 保留在 Dispatcher**（动态流程控制）；**DELIVERED 过账 + 到岸成本 + 告警 + 网关重试/死信保留原位**。
- 层 2 四方对照裁定 advanceTracking DELIVERED 无来源态守卫 drift（doc `IN_TRANSIT→DELIVERED` vs code `any→DELIVERED`）。
- 新增层 1 矩阵完备性测试；层 3 既有集成测试全绿回归。
- 保持全部既有外部行为不变（错误码、过账时序、幂等行为、网关重试/死信）。

## Non-Goals

- 不修改任何 `model/*.orm.xml` / `model/*.api.xml` / 字典 yaml。
- 不迁移 `posted`（§11.2 M4 (iii)）；`freightSettlementStatus` 独立轴不迁移（非 status 轴）。
- 不修改共享骨架 `Abstract*Processor`（module-common-service 零改动）。
- 不改变幂等 short-circuit 行为（保留在 Dispatcher）。
- 不改变网关重试/死信/告警逻辑（`GatewayDispatcher` 网关编排保留原位）。
- 不改变 cancel 审批工作流状态（Deferred P1-MA2-078——`cancelShipment` 经状态守卫 + 网关覆盖，审批工作流 successor）。
- 不实现部分签收（Deferred P1-MA2-079——承运商支持部分签收回调时）。
- 不引入全局 CRUD 写锁（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。
- 不迁移 ErpLogShipmentLine/Parcel/Log（子表/日志，非 status 轴）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M0.2 清单 + M1.3 模板 §11 + **M4.29 StockMove 同结构范本**；落地 1 Bean + GatewayDispatcher Facade 接线 + 测试 + 四方对照。**M4 plan-first**——DELIVERED 触发 FREIGHT 过账 + 到岸成本）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 模板 + §11.2 M4 变体 + §1 单轴命名 + §9 生成路径）、`docs/design/logistics/state-machine.md`（§Shipment 6 态 + §2 迁移表 + §实现约定）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（:329,:452）、`docs/architecture/processor-extension-pattern.md`、`docs/skills/state-machine-business-review-prompt.md`、`docs/plans/2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`（M4.29 直接范本）
- Skill Selection Basis: 路线图 M4.57 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「GatewayDispatcher Facade 接线、Bean 注册、`@Inject` 非 private、cause-chaining 错误码、过账副作用保留」；`nop-testing` 匹配「矩阵表驱动测试 + 既有 8 个集成测试回归」。`state-machine-business-review-prompt.md` 匹配层 2 四方对照。M4.29 范本可直接镜像 Bean 形状；Facade 接线点差异（Dispatcher vs Processor）须 Decision 裁定。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护运费过账行为（DELIVERED 触发 path-1 FREIGHT 凭证 + path-2 config-gated 到岸成本）。在人工/owner-doc 确认前为阻塞前置。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖。无数据迁移。

## Execution Plan

### Phase 1 - ErpLogShipment status Bean + GatewayDispatcher 接线（M4.57）

Status: planned
Targets: `module-logistics/erp-log-service/src/main/java/app/erp/log/service/statemachine/ErpLogShipmentStateMachine.java`、`.../beans/app-service.beans.xml`、`.../gateway/GatewayDispatcher.java`、`.../test/.../statemachine/TestErpLogShipmentStateMachineMatrix.java`
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done（已满足）；M4.29 `ErpInvStockMoveStateMachine` 范本已 done

- [ ] `Decision`（GatewayDispatcher Facade 接线 + 幂等/DELIVERED drift 三项裁决）：(A) **Bean 接入 GatewayDispatcher**（非 per-mutation Processor——守卫集中在 Facade）。Dispatcher 注入 `@Inject ErpLogShipmentStateMachine`（非 private）。(B) **幂等 short-circuit 保留在 Dispatcher**——`advise:72-75` if already ADVISED → return、`completeShipment:87-98` if DISPATCHED+ → return、`cancelShipment:134-136` if CANCELLED/DELIVERED → return、`advanceTracking:165-166` if already DELIVERED → return false 均为动态流程控制（非纯迁移边），保留原位。Bean 只替换**前向迁移守卫**（source-state assert），不接管幂等短路。(C) **advanceTracking DELIVERED 无来源态守卫——刻意收紧**：code `:168` 无 `Objects.equals(from, CONST)` 来源态守卫（任何非 DELIVERED 状态均可被推进到 DELIVERED，包括 DRAFT/CANCELLED），owner-doc §2 迁移表（state-machine.md :49）仅声明 `IN_TRANSIT→DELIVERED`。Bean **刻意编码合法来源态集 = {ADVISED,DISPATCHED,IN_TRANSIT}**（排除 DRAFT/CANCELLED）——这是**引入新守卫（行为变更）**而非"按代码实况"：DRAFT 发运单无承运商/trackingNo（advanceTracking 不可达）；CANCELLED 是终态不应可逆到 DELIVERED。此项**是行为收紧**（Bean `assertCanAdvanceToDelivered` 在 DRAFT/CANCELLED 时抛异常，而当前 code 无抛异常），但因 DRAFT→DELIVERED 实践中不可达（无 trackingNo 无法匹配发运单）+ CANCELLED→DELIVERED 是逻辑错误，收紧为**安全改善**。四方对照登记此 drift 为 `intentional narrowing`（code drift：code 比 owner-doc 更宽松，Bean 向 owner-doc 收紧方向靠拢但不完全一致——owner-doc 仅 IN_TRANSIT 单源，Bean 含 ADVISED/DISPATCHED 作为合法中间态推进路径，因 advanceTracking 可从 DISPATCHED 直接到 DELIVERED）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`：落地 `ErpLogShipmentStateMachine` Bean——5 动作矩阵（advise DRAFT→ADVISED、completeShipment ADVISED→DISPATCHED、advanceToInTransit DISPATCHED→IN_TRANSIT、advanceToDelivered {ADVISED,DISPATCHED,IN_TRANSIT}→DELIVERED（**刻意收紧**，排除 DRAFT/CANCELLED——见 Decision (C)）、cancelShipment {DRAFT,ADVISED,DISPATCHED,IN_TRANSIT}→CANCELLED）+ 对应 `assertCanXxx(String status)` + `*TargetStatus()` + `isTerminal`/`initialStatuses`/`terminalStatuses` + `transitions()`。严格无状态。非法边抛 common 码 `ERR_ILLEGAL_STATUS_TRANSITION` + `action`/`currentStatus`/`expectedStatus`。镜像 M4.29 `ErpInvStockMoveStateMachine` 结构。
  - Skill: `nop-backend-dev`
- [ ] `Add`：在 `_vfs/erp/log/beans/app-service.beans.xml` 注册（紧邻 `GatewayDispatcher` L27-28）。
  - Skill: `nop-backend-dev`
- [ ] `Add`（接线，GatewayDispatcher Facade）：`GatewayDispatcher` 注入 `@Inject ErpLogShipmentStateMachine`（非 private）；`advise:62-73` 前向守卫改调 `stateMachine.assertCanAdvise(status)`（幂等短路保留在前）；`completeShipment:84-98` 前向守卫改调 `assertCanCompleteShipment`（`writeBackSuccess:319` 目标态改调 `completeShipmentTargetStatus()`）；`advanceTracking:162-184` 前向守卫改调 `assertCanAdvanceToInTransit`/`assertCanAdvanceToDelivered`；`cancelShipment:131-153` 前向守卫改调 `assertCanCancelShipment`（多源）。try/catch common 码 → cause-chain `ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION`。网关重试/死信/告警 + DELIVERED 过账编排 + 到岸成本 + `freightSettlementStatus` 保留原位。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：层 1 矩阵完备性（greenfield 表驱动，镜像 M4.29 `TestErpInvStockMoveAndStockTakeStateMachines` 范式）——(a) 无重复/冲突边；(b) 全部 5 边可达；(c) 各 `assertCanXxx` 合法来源态通过、非法来源态抛 common 码；(d) `transitions()` 与显式方法语义一致；(e) 初始={DRAFT}/终态={DELIVERED,CANCELLED}。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] `ErpLogShipmentStateMachine` Bean 存在、已注册、严格无状态；GatewayDispatcher 委托 Bean 前向守卫，内联 `Objects.equals` 矩阵判断已移除（幂等短路保留）。
- [ ] Shipment 层 1 矩阵测试本地 `mvn test -pl module-logistics/erp-log-service -am -Dtest=TestErpLogShipmentStateMachineMatrix` 全绿。

### Phase 2 - 层 2 四方对照 + 层 3 既有回归

Status: planned
Targets: `module-logistics/erp-log-service/src/test/`（既有集成测试，零新建）
Skill: `nop-testing` + `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: Phase 1（Bean + Dispatcher 接线已落地）

- [ ] `Proof`：层 2 四方对照——dict `erp-log/shipment-status`（6 值）↔ owner-doc §2 迁移表 ↔ Bean 元数据 ↔ 全部 writer（GatewayDispatcher 5 call-sites + 创建写 DRAFT + 幂等短路 + CRUD 路径排除）。**DELIVERED drift finding**：advanceTracking 无来源态守卫 → 分类 + successor。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`：层 3 既有命名动作回归——复用 8 个集成测试（`TestErpLogShipmentGateway` testFullStateMachineFlow + cancel + retry/dead-letter/webhook；`TestErpLogShipmentPostingEnd` path-1/path-2 e2e；`TestErpLogFreightPosting` duplicate/settled/idempotent；`TestErpLogPath2LandedCost`；`TestErpLogCarrierGatewayIntegration`；`TestErpLogShipmentTrackingNoUk`；`TestErpLogShipmentCrudSmoke`；`TestLogPostingFaultInjection`），证明 Dispatcher 写回、FREIGHT 过账时序、到岸成本编排、幂等行为、网关重试/死信/告警、终态守卫（`ERR_LOG_SHIPMENT_ALREADY_DELIVERED`）不变。本地 `mvn test -pl module-logistics/erp-log-service -am` 全绿。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 层 3 既有集成测试全绿（零行为回归）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_0031377f3ffe8HXAUhT7rMCF2v`) — 零信任实仓核实 17/17 项 baseline 声明全 pass，无 BLOCKER。1 MAJOR：advanceToDelivered 来源态集 {ADVISED,DISPATCHED,IN_TRANSIT} 与"按代码实况不收紧"声明矛盾——code 无来源态守卫（任何非 DELIVERED→DELIVERED 含 DRAFT/CANCELLED），指定排除集即收紧。已修正：Decision (C) 改为**「刻意收紧」**显式声明行为变更 + 安全改善理由（DRAFT 无 trackingNo 不可达、CANCELLED 终态不可逆）+ 四方对照登记 `intentional narrowing`。2 MINOR 已修正：幂等 short-circuit 行号订正（64-71→72-75, 163-165→165-166）；"thin delegate" 措辞修正（HandleTrackingWebhook/ScanForPolling 含 HMAC/解析逻辑后委托，非纯单行委托）。
- Independent draft review iteration 2: `accept` (`ses_0030e8b9bffemP8FmO6i0ds67X`) — focused re-review of Decision (C) fix。MAJOR RESOLVED: yes。Decision (C) 现显式声明 advanceToDelivered 来源态集 {ADVISED,DISPATCHED,IN_TRANSIT} 为「刻意收紧」（行为变更非"按代码实况"），Goal / Add item / Deferred 四处一致。实仓确认 `GatewayDispatcher.advanceTracking:168` DELIVERED 写入确无来源态守卫（仅幂等短路 165-166），指定更窄集确为收紧——现已正确承认而非否认。IN_TRANSIT 写入有 `DISPATCHED.equals(status)` 守卫（:178-181）作对照（DELIVERED 无守卫的不对称属实）。无残余不一致。草案审查收敛，保持 `Plan Status: draft` + Review Hold。
- Plan review (MISSION_DRIVER 2026-08-13-193118-mission-driver): `review ran — held` — format/completeness/scope/closure 四项全 pass（模板必选段齐备；Exit Criteria 含确切 mvn 命令与测试名；单实体单轴无 scope creep；Closure Gates 定义验证证据）。零信任实仓复核 5 writer call-site 行号（advise:73/writeBackSuccess:319/advanceTracking:179/advanceTracking:168/cancelShipment:153）+ 4 幂等短路 + Decision (C) drift（advanceTracking DELIVERED 无来源态守卫 vs IN_TRANSIT 有 DISPATCHED 守卫）全数属实。无新增 BLOCKER/MAJOR。**Review Hold 维持**：§11.2 M4 (i) plan-first 人工/owner-doc 门控触及 DELIVERED→FREIGHT 过账(path-1)+到岸成本(path-2) 会计/财务保护区，属 project-context.md §AI 阻塞条件硬停止——非审查者可自主解除（batch-consistent with 0930-1/0810-x/1146-x/1931-x/0456-1/0456-2）。计划不 promote 至 active，留 draft 待人工/owner-doc 门控确认。
- Plan review (MISSION_DRIVER 2026-08-13-193118-mission-driver, 2nd pass): `review ran — held` — format/completeness/scope/closure 四维度复核全部就绪，无除门控外的 BLOCKER/MAJOR。独立实仓复核 `GatewayDispatcher.java`（431 行）确认基线零漂移：advise:73 写 ADVISED + 幂等短路 :72-75；writeBackSuccess:319 写 DISPATCHED + completeShipment 幂等返回 :87-92；advanceTracking:179 写 IN_TRANSIT **有** DISPATCHED 来源态守卫 :178；advanceTracking:168 写 DELIVERED **无**来源态守卫（仅幂等 :165-166）——Decision (C) drift 属实（任何非 DELIVERED 含 DRAFT/CANCELLED 可推进到 DELIVERED）；cancelShipment:153 写 CANCELLED + 幂等返回 :134-137。IN_TRANSIT vs DELIVERED 守卫不对称属实。Decision (C)「刻意收紧」为 advanceToDelivered 来源态集 {ADVISED,DISPATCHED,IN_TRANSIT} 排除 DRAFT/CANCELLED——**确认为真实行为变更**（当前 code 允许 CANCELLED→DELIVERED 静默翻转并触发 path-1/path-2 过账；Bean 将抛异常阻断），使本计划**非纯行为保持提取**，强化 M4 plan-first 门控必要性。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为外部依赖（M4.29 direct 先例同形门控于 2026-08-13 经人工确认解除但为 inventory 域特定；project-context.md 会计/财务保护区 AI 硬停止），审查者不可自主解除。保持 `Plan Status: draft` + Review Hold；门控解除后转 `active`。
- Plan review (MISSION_DRIVER 2026-08-13-193118-mission-driver, 3rd pass): `review ran — held` — format/completeness/scope/closure 四项全 pass，无除门控外的 BLOCKER/MAJOR/MINOR。零信任实仓复核：(1) §11.2 M4 治理约束确认——`entity-state-machine-bean.md:279-289` 明文 M4=财务影响/保护域全部 plan-first，(i)「触及受保护行为（过账...）时不因 StateMachine Bean 抽象而免除人工/owner-doc 门控」+ :289「触及财务过账自主权时停并 ask-first」；(2) `GatewayDispatcher.java` 5 writer call-site + 4 幂等短路 + IN_TRANSIT(:178 DISPATCHED 守卫)/DELIVERED(:168 无守卫) 不对称全数属实，Decision (C) drift 非伪造。Review Hold 维持：唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控（触及 DELIVERED→FREIGHT 过账 path-1 + 到岸成本 path-2 会计/财务保护区），为 project-context.md「AI 阻塞条件」硬停止的外部依赖，审查者不可自主解除（batch-consistent with 0930-1/0810-x/1146-x/1931-x/0456-1/0456-2）。计划保持 `Plan Status: draft` + Review Hold，门控确认后转 `active`。
- Plan review (MISSION_DRIVER 2026-08-14-070716-mission-driver): `review ran — held` — 四项清单（格式合规/完备性/范围/闭环证据）全部 pass，零 BLOCKER/MAJOR/MINOR。零信任实仓复核（本 pass）全数属实：dict `erp-log/shipment-status` 6 值 orm.xml:39-46；`ErpLogConstants:9-14`；5 writer call-site（advise:73 ADVISED / writeBackSuccess:319 DISPATCHED / advanceTracking:168 DELIVERED 无来源态守卫仅幂等 :165-166 / advanceTracking:179 IN_TRANSIT 有 DISPATCHED 守卫 / cancelShipment:153 CANCELLED）与 4 幂等短路（advise 守卫 :66-71 + 幂等跳过 :72-75、completeShipment :87-92、cancelShipment :134-136）精确；`ErpLogErrors:32`（illegal-transition 3 参数）/`:43`（already-delivered）精确；GatewayDispatcher 431 行；beans.xml 52 行（51 换行无尾 NL）注册位 L14-20/L27-28/L32-33/L39-50 精确；logistics 域 `statemachine/` 目录不存在 = SM Bean greenfield；8 个既有测试全存在（TestLogPostingFaultInjection 在 `processor/` 子包）；roadmap M4.57=`todo`+plan-first。**Review Hold 维持且不可自解**：唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控（缺失上游人工决策）。强化点：本计划 Decision (C) 为**真实行为收紧**（当前 code 允许 CANCELLED→DELIVERED 静默翻转并触发 path-1/path-2 过账，Bean 将抛异常阻断），非纯行为保持提取——logistics 域无同域 M4 人工确认先例（M4.29 为 inventory 域特定，2026-08-13 人工解除），`ai-autonomy-policy.md:9` 明禁 AI 无人工确认/人工批准 owner-doc 证据时移除阻塞项；batch 内 0456-1 于 2026-08-14 人工确认转 active，0456-2 同批仍 held，逐一确认模式与本计划未确认状态一致。按 escape-hatch：保持 `Plan Status: draft` + Review Hold（front matter line 4），门控解除后追加确认记录并转 `active`。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-14 人工确认解除）**（§11.2 M4 (i)）。人工/owner 于 2026-08-14 确认「以行为保持（含 Decision (C) advanceToDelivered 刻意收紧，排除 DRAFT/CANCELLED 来源态）的矩阵集中化方式迁移 shipment 单轴、DELIVERED→FREIGHT 过账 path-1 + 到岸成本 path-2 完整保留」可接受，门控解除。据此将 Plan Status 由 `draft` 转 `active`。

## Closure Gates

- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 范围内行为完成（status Bean + GatewayDispatcher Facade 接线 + 层 1 矩阵 + 层 2 四方对照 + 层 3 回归）
- [ ] 相关文档对齐（roadmap M4.57 → done；DELIVERED drift 登记）
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-logistics/erp-log-service -am` 全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### advanceTracking DELIVERED 刻意收紧（排除 DRAFT/CANCELLED）

- Classification: `intentional narrowing (behavior change — safety improvement)`
- Why Not Blocking Closure: code `:168` 无来源态守卫（任何非 DELIVERED→DELIVERED），Bean 刻意编码合法来源态 = {ADVISED,DISPATCHED,IN_TRANSIT}（排除 DRAFT/CANCELLED）。DRAFT 无 trackingNo 不可达；CANCELLED 是终态不应可逆。此收紧引入新守卫（行为变更）但为安全改善，非语义破坏。四方对照登记为 `intentional narrowing`。
- Successor Required: yes（触发条件 = PM 要求 DELIVERED 严格仅从 IN_TRANSIT 可达时进一步收紧 Bean 边 + 补 Dispatcher 显式守卫）

### cancelShipment 审批工作流（IN_TRANSIT→CANCELLED 需审批）

- Classification: `watch-only residual (Deferred P1-MA2-078)`
- Why Not Blocking Closure: 当前 `cancelShipment` 经状态守卫 + 网关 `client.cancelShipment`（DISPATCHED+ 防双发）覆盖。物流主管审批工作流（cancel-approve 动作 + 角色-resource 种子）留 successor。Bean 编码当前实现的 cancel 边。
- Successor Required: yes（触发条件 = 审批工作流 SPI 落地时实现 cancel-approve 动作 + config-gated 角色-resource 门控）

### 部分签收（partial delivery）

- Classification: `watch-only residual (Deferred P1-MA2-079)`
- Why Not Blocking Closure: 当前 `advanceTracking` 仅处理完整 TRACKING_EVENT_DELIVERED。部分签收回调须 ORM ask-first 加列 + 累计签收判定。
- Successor Required: yes（触发条件 = 承运商支持部分签收回调时）

### Delta 覆盖运行时实证

- Classification: `optimization candidate`
- Why Not Blocking Closure: M4 保护域单项 Delta 可选；归 M5.3。
- Successor Required: yes（触发条件 = M5.3 最终跨域 Delta 覆盖回归）

### 全局 CRUD 写锁

- Classification: `watch-only residual`
- Why Not Blocking Closure: M0.1 §9 裁定选项 (c) 显式排除。
- Successor Required: no

## Closure

Status Note: _待执行后填写_

Closure Audit Evidence:

- Auditor / Agent: _待执行后填写_
- Evidence: _待执行后填写_

Follow-up:

- <待执行后填写；Deferred 项均为既定 successor>
