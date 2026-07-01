# 2026-07-01-0811-2 库存移动 BizModel：状态机 + 流水/余额 + 可用量校验 + 存货过账

> Plan Status: active
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/core-business-roadmap.md` P1 工作项 1.3（StockMove BizModel：库存移动/流水/余额）
> Related: `docs/plans/2026-07-01-0811-1-finance-posting-engine-foundation.md`（本计划 Phase 3 的存货过账消费其引擎；同批，N=1 先于本计划）、`docs/design/inventory/state-machine.md`（移动单状态机权威源）、`docs/design/inventory/cross-domain.md`（`generateMove` 契约 + 一致性规则权威源）、`docs/design/flow-overview.md`（L2 跨域协作层：采购入库/销售出库→库存）、`docs/design/inventory/README.md`
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：codegen 完成、待 BizModel 业务逻辑深化。库存域 CRUD 冒烟测试已绿（`TestErpInvStockMoveCrudSmoke`，`ErpInvStockMove`→`ErpInvStockMoveLine` 头-行对，5 类标准 CRUD），`erp-inv-service` 已含 `app-erp-master-data-service` test 依赖（CRUD 冒烟计划已加，跨域主数据已上 classpath）。但 StockMove BizModel 仍为 `CrudBizModel<ErpInvStockMove>` **空壳**——无状态机、无流水、无余额驱动、无过账。

**BizModel/I*Biz 现状**（实时核实）：`IErpInvStockMoveBiz extends ICrudBiz<ErpInvStockMove>`（仅继承标准 CRUD，**无自定义方法**）；`ErpInvStockMoveBizModel extends CrudBizModel<ErpInvStockMove> implements IErpInvStockMoveBiz`（空壳）。`cross-domain.md` 定义的 `IErpInvStockMoveBiz.generateMove(...)` 契约**尚未实现**——采购入库/销售出库的跨域调用方（purchase/sales）尚不存在，但契约须先落地以解除其阻塞。

**实体模型已就绪**（实时核实 `module-inventory/model/app-erp-inventory.orm.xml`）：
- `ErpInvStockMove`（移动单头）：`code`/`moveType`(int dict `erp-inv/operation-type`)/`orgId`/`businessDate`/`sourceWarehouseId`/`sourceLocationId`/`destWarehouseId`/`destLocationId`/`docStatus`(int dict `erp-inv/move-status`)/`approveStatus`/`posted`(bool)/`postedAt`/`postedBy`/`relatedBillType`/`relatedBillCode`；关系 `sourceWarehouse`/`destWarehouse`/`lines`。
- `ErpInvStockMoveLine`（移动单行）：`moveId`/`lineNo`/`materialId`/`skuId`/`uoMId`/`quantity`(VARCHAR quantity 域)/`unitCost`/`totalCost`/`currencyId`/`batchNo`/`serialNo`/`sourceLocationId`/`destLocationId`。
- `ErpInvStockLedger`（库存流水，**不可变**）：`moveId`/`moveLineId`/`materialId`/`skuId`/`warehouseId`/`locationId`/`quantity`/`unitCost`/`totalCost`/`balanceQuantity`(结存)/`balanceTotalCost`(结存)/`costMethod`(int dict `erp-md/cost-method`)/`acctSchemaId`/`businessDate`/`batchNo`/`serialNo`。
- `ErpInvStockBalance`（库存余额）：`materialId`/`skuId`/`warehouseId`/`locationId`/`batchNo`/`totalQuantity`/`reservedQuantity`/`lockedQuantity`/`availableQuantity`(=total−reserved−locked)/`costMethod`/`avgCost`/`totalCost`/`currencyId`。
- `ErpInvReservation`/`ErpInvReservationLine`（记录级预留，`sourceBillType`/`sourceBillCode`/`status` dict `erp-inv/reservation-status`）。

**字典权威值**（实时核实 `module-inventory/erp-inv-meta/.../dict/erp-inv/`）：
- `move-status`（docStatus）：10 草稿 / 20 已确认 / 30 已完成 / 40 已取消。
- `operation-type`（moveType）：10 入库 / 20 出库 / 30 内部调拨 / 40 制造。

**状态机**（`docs/design/inventory/state-machine.md` 权威）：DRAFT → CONFIRMED（出库类前置：可用量充足；出库类**增预留量**）→ DONE（写**不可变**流水、更新余额、释放预留、发存货过账事件）/ → CANCELLED（释放预留）；DONE 的纠错路径是**生成反向冲销移动单**（非反审核）。业务单据联动的移动单通常自动 DRAFT→CONFIRMED→DONE。

**跨域契约与一致性**（`docs/design/inventory/cross-domain.md` 权威）：
- `IErpInvStockMoveBiz.generateMove(...)`：purchase/sales Processor 调用，传入物料/SKU、仓库/库位、数量、批次、关联源单；返回生成的移动单。
- **可用量校验**：出库前校验 `total−reserved−locked` 是否充足；不足拒绝 + 整个业务单据审核回滚。
- **余额更新与流水写入必须在同一事务**（由库存域 Processor 保证）。
- **单位成本由库存流水维护**（StockLedger 含 `unitCost`/`totalCost`），财务域消费。
- **同法人内部调拨无凭证**（仅库存移动）；跨法人调拨才生成内部交易凭证。
- 幂等：同一业务单据对同一移动单的触发生成需幂等。

**关键约束/陷阱**（实时核实）：
- `quantity`/`unitCost`/`totalCost`/`totalQuantity`/`availableQuantity`/`avgCost` 均为 **VARCHAR 存储**（quantity/amount/unitPrice 域，DECIMAL 以字符串持久化）——可用量校验与成本计算须 `BigDecimal`，按列类型不可数值直比。
- `business-type` 字典（finance）**无 INTER_TRANSFER 项**——同法人内部调拨本就不过账（cross-domain 明确），跨法人调拨过账属 Follow-up（届时需扩字典 + 跨组织逻辑）。
- 过账引擎（`ErpFinPostingService`/`ErpFinAcctDocRegistry`/`IErpFinAcctDocProvider`）由同批 `2026-07-01-0811-1` 提供；本计划 Phase 3 接入。

**剩余差距**：(1) StockMove 状态机 + `generateMove` 契约 + 幂等；(2) 不可变流水写入 + 余额驱动（预留/扣减/移动加权平均成本）+ 可用量校验 + 负库存配置；(3) DONE 发存货过账事件 + inventory 域 `InvAcctDocProvider` 注册（存货估值凭证）+ 端到端验证。

## Goals

- **`IErpInvStockMoveBiz.generateMove(...)` 契约落地**：按 `cross-domain.md` 签名实现，供 purchase/sales 跨域调用；业务单据联动自动推进 DRAFT→CONFIRMED→DONE，幂等（同源单重复触发不产生第二张移动单）。
- **StockMove 状态机正确**：DRAFT→CONFIRMED→DONE/CANCELLED 迁移按 `inventory/state-machine.md` 落地，每条迁移的前置/结果/角色对齐；出库类 CONFIRMED 增预留、DONE 释放并扣减；CANCELLED 释放预留。
- **不可变流水 + 余额驱动**：DONE 写一条 `ErpInvStockLedger`（含结存 `balanceQuantity`/`balanceTotalCost`，不可变）；同一事务更新 `ErpInvStockBalance`（`totalQuantity`/`avgCost`/`totalCost`/`reservedQuantity`/`availableQuantity`），移动加权平均成本由流水维护。
- **可用量校验 + 负库存配置**：出库类 CONFIRMED 校验 `availableQuantity ≥ quantity`，不足抛 `NopException` + 回滚；全局配置 `erp-inv.allow-negative-stock`（默认 false）开启时跳过校验。
- **存货过账接入**：DONE 发 `PostingEvent`（入库→`PURCHASE_INPUT`、出库→`SALES_OUTPUT`；同法人内部调拨不发）；inventory 域实现 `InvAcctDocProvider` 注册到 `ErpFinAcctDocRegistry`，生成存货估值凭证，移动单 `posted=true`。
- **服务层集成测试证明**：状态迁移、幂等、流水不可变、余额正确性、可用量不足拒绝、负库存放行、端到端过账，全部可重复通过。

## Non-Goals

- **不修改任何 `model/*.orm.xml` / `.api.xml`**（保护区域）——复用已生成实体；无 ORM 变更即无需 regen。
- **不实现 purchase/sales 调用方**——`generateMove` 契约先落地，调用方属后续 purchase/sales BizModel 计划；本计划测试直接调 `IErpInvStockMoveBiz`。
- **不做跨法人调拨内部交易凭证**——`business-type` 字典缺 INTER_TRANSFER，且 cross-domain 定位为 Follow-up（需扩字典 + 跨组织科目，涉及保护区域须人工批准）。
- **不做记录级预留（ErpInvReservation）的订单/工单预留管理**——本计划状态机的「预留量」作用于 `ErpInvStockBalance.reservedQuantity` 聚合；显式 `ErpInvReservation` 单据管理（按销售订单/工单预留、有效期）属独立功能。
- **不做盘点单状态机与盘盈/盘亏移动单生成**——盘点是独立状态机（`inventory/state-machine.md` 文末），盘点差异生成移动单走本计划移动单流程，但盘点本身不在本计划。
- **不做批次/序列号全生命周期**——移动单行携带 `batchNo`/`serialNo`，启用批次物料的必填校验纳入可用量维度，但批次状态机/序列号销售锁定等完整逻辑属 Follow-up。
- **不做两步调拨在途库（IN_TRANSIT）**——`ErpInvTransferOrder` 的两步调拨属独立功能；本计划覆盖单步移动单。
- **不做异步过账派发**——同 `2026-07-01-0811-1` Non-Goal；DONE 同步触发过账事件，异步接线为 Follow-up。

## Task Route

- Type: `architecture change`（`IErpInvStockMoveBiz.generateMove` 是 purchase/sales 跨域同步调用的契约边界，影响库存一致性核心）+ `implementation-only change`（greenfield BizModel 方法 + Provider，不改公共 API 契约或 ORM）。
- Owner Docs: `docs/design/inventory/state-machine.md`、`docs/design/inventory/cross-domain.md`、`docs/design/inventory/README.md`、`docs/design/finance/posting.md`（存货过账）、`docs/architecture/testing-strategy.md`（测试 runbook + CRUD 冒烟沉淀的跨域主数据依赖模式）、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（CrudBizModel 扩展、I*Biz 跨域注入）。
- Skill Selection Basis: `Skill: none`（实施）。`docs/skills/README.md` 现有技能均为审计/审查方法，无 BizModel 编写技能匹配；实施遵循平台 service-layer 指南与 cross-domain/state-machine 文档。独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`；移动单状态机正确性用 `state-machine-business-review-prompt.md` 复核（见 Phase 1 Proof）。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-inv-app` 已含 `quarkus-jdbc-h2`；服务层测试 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`）。
- `erp-inv-service` 已含 `app-erp-master-data-service` test 依赖（CRUD 冒烟计划已加）——StockMove 跨域引用 `ErpMdMaterial`/`Warehouse`/`Location`/`UoM`/`Currency`，测试以 `createPrereqs()` 自建主数据（复用 CRUD 冒烟 runbook 模式）。
- **新增结构性依赖**：Phase 3 给 `erp-inv-service/pom.xml` 加 compile scope 依赖 `app-erp-finance-service`（注入 `IErpFinPostingService`；inventory→finance 单向无环，已实时核实）。
- 全局配置 `erp-inv.allow-negative-stock`（默认 false）：经 Nop 配置机制读取（`project-context.md` 配置管理）；测试以测试 scope `application.yaml` 或 `@NopTestConfig` 注入覆盖。
- 无数据迁移/回滚脚本需求（greenfield BizModel + Provider，复用已存在实体）。

## Execution Plan

### Phase 1 - generateMove 契约 + StockMove 状态机 + 幂等

Status: planned
Targets: `module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/IErpInvStockMoveBiz.java`（增 `generateMove` 等方法签名）、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockMoveBizModel.java`（实现）、`module-inventory/erp-inv-service/src/test/.../entity/`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（状态机/契约不依赖过账引擎）

- [ ] `Decision`：`generateMove` 签名与推进策略。裁决：签名按 `cross-domain.md`——`generateMove(moveType, lines, sourceWarehouseId/locationId, destWarehouseId/locationId, businessDate, relatedBillType, relatedBillCode, orgId, acctSchemaId, currencyId)` 返回 `ErpInvStockMove`。业务单据联动（`relatedBillType` 非空）自动 DRAFT→CONFIRMED→DONE 一次推进（cross-domain「业务联动的移动单通常自动推进到 DONE」）；独立创建的移动单（`relatedBillType` 空，如盘点调整）停在 CONFIRMED 待库管员二次确认。备选（被否）：始终停在 CONFIRMED——业务联动场景库管员无须介入，强加二次确认致单据沉没。残留风险：业务联动自动 DONE 假设触发方（purchase/sales）已做完所有校验；若调用方校验不足，错误移动单会直接 DONE（须在 `generateMove` 契约文档要求调用方先自校验）。
  - Skill: none
- [ ] `Decision`：幂等键。裁决：`(relatedBillType, relatedBillCode)` 为业务联动移动单的幂等键——同源单重复 `generateMove` 反查已有移动单直接返回，不新建（对齐状态机§异常路径「重复触发幂等」）。独立移动单（无源单）不参与幂等（允许手工多次创建）。备选（被否）：用移动单 `code`——`code` 是生成后产物，调用方传入的是源单号。残留风险：独立移动单无幂等，重复手工提交会产生重复单（由 UI 防重 + 库管员确认缓解，属可接受运营行为）。
  - Skill: none
- [ ] `Add`：`IErpInvStockMoveBiz` 增 `generateMove(...)`、`confirm(moveId)`、`complete(moveId)`、`cancel(moveId)`、`reverse(moveId)`（生成反向冲销移动单）方法签名；`ErpInvStockMoveBizModel` 实现状态迁移——每条迁移校验前置 `docStatus`（DRAFT→CONFIRMED→DONE 有向无环；CANCELLED 仅从 DRAFT/CONFIRMED），迁移违反抛 `NopException`。`@BizMutation` 自动事务，余额/流水同事务（cross-domain 一致性规则，Phase 2 落地写入）。
  - Skill: none
- [ ] `Proof`：服务层集成测试（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + master-data test 依赖，`createPrereqs()` 自建物料/仓库/库位）——`testGenerateMoveBusinessLinkedAutoCompletes`（业务联动 → DRAFT→CONFIRMED→DONE）、`testGenerateMoveIdempotent`（同源单二次返回同一移动单）、`testManualMoveStopsAtConfirmed`（独立移动单停 CONFIRMED）、`testIllegalTransitionRejected`（DONE→CONFIRMED 等非法迁移抛 `NopException`）、`testCancelReleasesReservation`（出库类 CONFIRMED 后 CANCELLED 释放预留）。`mvn test -pl module-inventory/erp-inv-service -am` 全绿。
  - Skill: none
- [ ] `Proof`：移动单状态机正确性复核——用 `docs/skills/state-machine-business-review-prompt.md` 针对终态/可达性/异常路径（重复触发、冲销=新建反向单非回退）自检，记录结论于本阶段执行落地（非阻塞门控，但须执行）。
  - Skill: state-machine-business-review-prompt

Exit Criteria:

> 本阶段交付状态机 + 契约 + 幂等。完整仓库 `mvn test` 归 Closure Gates。

- [ ] `generateMove`/`confirm`/`complete`/`cancel`/`reverse` 5 个行为测试存在且 `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [ ] 业务联动自动推进 + 幂等 + 非法迁移拒绝均经测试证明

### Phase 2 - 不可变流水 + 余额驱动 + 可用量校验 + 负库存配置

Status: planned
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockMoveBizModel.java`（增流水/余额逻辑）、`.../stock/ErpInvStockBalanceCalculator.java`（或等效内聚组件）、`module-inventory/erp-inv-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] `Decision`：成本计算方法。裁决：DONE 时按 `ErpInvStockBalance.costMethod`（移动加权平均为基线，`costMethod` dict `erp-md/cost-method`）计算——入库：新 `avgCost = (旧 totalCost + 入库 totalCost) / (旧 totalQuantity + 入库 quantity)`；出库：`unitCost = 当前 avgCost`（快照固化写入流水），`totalCost -= unitCost × quantity`。`StockLedger.balanceQuantity`/`balanceTotalCost` 记录写流水时的结存结存快照（不可变）。`BigDecimal` 运算，写入 VARCHAR 列。备选（被否）：FIFO/批次成本——`costMethod` 支持多方法但本计划基线只落地移动加权平均，FIFO/批次属 Follow-up。残留风险：负库存下移动加权平均的 `avgCost` 可能失真（cross-domain §负库存会计处理 已约定「自然平滑」，但持续负库存的 `avgCost` 须监控——见 Deferred 负库存监控告警）。
  - Skill: none
- [ ] `Decision`：预留量作用对象。裁决：状态机的「预留量」作用于 `ErpInvStockBalance.reservedQuantity`/`availableQuantity` 聚合（出库类 CONFIRMED：`reservedQuantity += qty`、`availableQuantity -= qty`；DONE：`totalQuantity -= qty`、`reservedQuantity -= qty`、重算 `availableQuantity`/成本；CANCELLED：`reservedQuantity -= qty`、`availableQuantity += qty`）。**不**创建 `ErpInvReservation` 记录级预留单（那是按订单/工单的显式预留，独立功能）。备选（被否）：每次 CONFIRMED 建 `ErpInvReservation` 单——混同两个预留概念，且本计划 Non-Goal 排除记录级预留管理。残留风险：聚合预留不区分「为哪个源单预留」，无法按订单释放单笔预留（记录级预留管理 Follow-up 落地后补齐按源单精细化）。
  - Skill: none
- [ ] `Add`：`complete(moveId)` 落地——同一事务内：(1) 按行写 `ErpInvStockLedger`（不可变，含 `balanceQuantity`/`balanceTotalCost` 结存快照、`unitCost`/`totalCost`）；(2) 更新 `ErpInvStockBalance`（按 `materialId`×`warehouseId`×`locationId`×`batchNo` 维度 upsert，重算 `totalQuantity`/`avgCost`/`totalCost`/`reservedQuantity`/`availableQuantity`）；(3) 入库类增加余额、出库类扣减、内部调拨扣源加目的。冲销 `reverse` 生成反向移动单走同流程（cross-domain「冲销本质是反向移动」）。
  - Skill: none
- [ ] `Add`：`confirm(moveId)` 落地——出库类/内部调拨源库位校验 `availableQuantity ≥ quantity`（`BigDecimal` 比较），不足抛 `NopException`（cross-domain「不足拒绝 + 整个业务单据审核回滚」）；通过则增预留。读取 `erp-inv.allow-negative-stock` 配置，`true` 时跳过校验。
  - Skill: none
- [ ] `Proof`：`testCompleteWritesImmutableLedger`（DONE 后流水存在且 `balanceQuantity`/`balanceTotalCost` 正确；二次写入失败/不可改）、`testIncomingUpdatesBalanceAvgCost`（入库后 `avgCost`/`totalQuantity` 按移动加权平均正确）、`testOutgoingDeductsBalance`（出库后 `totalQuantity` 减少、成本按 avgCost 快照）、`testConfirmInsufficientAvailableRejected`（出库可用量不足 → `NopException` + 无预留增加）、`testNegativeStockConfigAllowsShortage`（`allow-negative-stock=true` 时不足仍可确认）。
  - Skill: none

Exit Criteria:

> 本阶段交付流水/余额/可用量核心记账。完整仓库 `mvn test` 归 Closure Gates。

- [ ] 5 个记账行为测试存在且 `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [ ] 流水不可变 + 移动加权平均成本 + 可用量校验/负库存配置均经测试证明

### Phase 3 - 存货过账接入（DONE→PostingEvent + InvAcctDocProvider）+ 收尾

Status: planned
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockMoveBizModel.java`（DONE 发事件）、`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/InvAcctDocProvider.java`、`module-inventory/erp-inv-service/src/test/.../`、`docs/logs/2026/07-01.md`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2 + **`2026-07-01-0811-1`（过账引擎）已 active 并可消费 `PostingEvent`**

- [ ] `Decision`：存货过账 businessType 映射。裁决：DONE 时按 `moveType`+`relatedBillType` 派生——入库(10) → `PURCHASE_INPUT`(10)；出库(20) → `SALES_OUTPUT`(20)；**同法人内部调拨(30) 不发事件**（cross-domain「同法人调拨无凭证」）。跨法人调拨/制造(40) 过账属 Follow-up（`business-type` 字典缺 INTER_TRANSFER、制造类须配合制造域）。备选（被否）：内部调拨发 INTER_TRANSFER——字典无此项且同法人无须凭证。残留风险：采购退货入库/销售退货出库当前都映射到 PURCHASE_INPUT/SALES_OUTPUT 的通用存货科目，退货与正流的科目差异（如退货走采购退货专户）须后续在模板/科目映射层区分。
  - Skill: none
- [ ] `Decision`：过账失败不阻塞移动单终态（cross-domain 合约）。裁决：`complete(moveId)` 在流水/余额同事务落库（DONE 终态确立）**之后**调用过账；过账调用以 **try/catch 包裹**——成功：置 `ErpInvStockMove.posted=true`；失败：吞异常并记录错误日志，**移动单保持 DONE、`posted=false`**，由 Deferred 的兜底扫描重试。理由：cross-domain.md §与财务域协作 + state-machine.md §7 明确「移动单完成即视为库存记账成功，凭证生成是后置异步动作，失败不影响移动单终态」。备选（被否）：过账异常上抛致 DONE 回滚——直接违反合约，库存已实物移动却记账失败。残留风险：`posted=false` 的 DONE 单依赖兜底扫描才最终过账（扫描未接线前，这些单的凭证会缺失——须在 Deferred 异步派发落地前以测试证明失败路径可观测）。
  - Skill: none
- [ ] `Add`：`complete(moveId)` 末尾（流水/余额同事务确立 DONE 后）：(1) 同法人内部调拨跳过过账；(2) 否则构造 `PostingEvent`（`businessType`=PURCHASE_INPUT/SALES_OUTPUT、`billHeadCode`=移动单 code、`acctSchemaId`、`billData`=流水汇总：物料/仓库/数量/单位成本/总成本）调用注入的 `IErpFinPostingService.post(...)`；(3) 成功 → 置 `ErpInvStockMove.posted=true`（inventory 侧自行置位，引擎不持有源实体——与 `2026-07-01-0811-1` Non-Goals 对齐）；失败 → 吞异常记日志，保持 `posted=false`（见上方 Decision）。**结构性依赖**：`erp-inv-service/pom.xml` 新增 **compile** scope 依赖 `app-erp-finance-service`（过账 SPI `IErpFinPostingService`/`IErpFinAcctDocProvider` 位于 `module-finance/erp-fin-service`；`erp-fin-api` 仅含生成的 CRUD GraphQL 契约、不含过账 SPI，故依赖 `app-erp-finance-service` 而非 `app-erp-finance-api`；inventory→finance 单向，finance 为 DAG 顶无环——已实时核实 `erp-fin-service/pom.xml` 不依赖 inventory、`app-erp-all` 聚合两者）。
  - Skill: none
- [ ] `Add`：`InvAcctDocProvider implements IErpFinAcctDocProvider`（inventory 域，**非默认** Provider——Registry 中优先于默认 fallback）——`getSupportedBusinessTypes()` 返回 `{PURCHASE_INPUT, SALES_OUTPUT}`；`createFacts` 按流水分行产出存货估值 `VoucherFact`（入库：借存货/贷暂估应付；出库：借结转成本/贷存货；科目按模板 `accountKey`/`subjectCode`），金额取自流水 `totalCost`。注册为 Bean 由 `ErpFinAcctDocRegistry` 自动聚合（域 Provider 优先，覆盖默认 Provider 对同 key 的兜底）。
  - Skill: none
- [ ] `Proof`：端到端测试 `testMoveDoneGeneratesVoucherAndPosted`（业务联动入库 `generateMove`→自动 DONE→`ErpFinVoucher`+`ErpFinVoucherLine`+`ErpFinVoucherBillR` 落库、移动单 `posted=true`（boolean）、移动单 `docStatus`=已完成(30)；**凭证** `docStatus`=已过账）；`testInternalTransferNoPosting`（同法人内部调拨 DONE 不产生凭证、`posted` 保持 false）；`testPostingFailureLeavesMoveDonePostedFalse`（注入抛异常的 PostingService → 移动单仍 DONE(30)、`posted=false`、无凭证）。`mvn test -pl module-inventory/erp-inv-service -am` 全绿。证明引擎 + 首个业务域 Provider 端到端打通，且过账失败不阻塞库存终态。
  - Skill: none
- [ ] `Add`：更新当日开发日志 `docs/logs/2026/07-01.md`（按 `docs/logs/00-log-writing-guide.md`，时间倒序），记录 StockMove BizModel + 存货过账接入 + 验证状态。
  - Skill: none

Exit Criteria:

> 本阶段交付存货过账端到端打通。完整仓库 `mvn test` 归 Closure Gates。

- [ ] 端到端过账测试 + 内部调拨不过账测试 + 过账失败不阻塞终态测试存在且 `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [ ] 当日日志已记 StockMove BizModel 落地与验证状态

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e4f38ba0ffe5ZSvMS2H0ApiG3，独立 general 子代理，新会话）— 基线主张全部实时核实属实（IErpInvStockMoveBiz 空壳、移动单/流水/余额/预留实体与列、move-status/operation-type 字典 10/20/30/40、VARCHAR 数量金额、business-type 无 INTER_TRANSFER、erp-inv-service 已含 master-data test 依赖、cross-domain generateMove 契约、inventory→finance 单向无环、状态机预留聚合模型与 auto-progression 与 owner doc 一致、Rule 14 bundling 合理、Plan 1 顺序正确）。2 项阻塞：(B1) Phase 3 Proof 称移动单 `docStatus=已过账`，但 move-status 字典只有 草稿/已确认/已完成/已取消，无「已过账」（posted 是独立 boolean）——混淆凭证状态与移动单状态；(B2) 同步过账失败语义未定义，若 `post()` 异常上抛会致 DONE 回滚，违反 cross-domain「失败不影响移动单终态」合约。4 项建议：S1 五个 Decision 缺残留风险（Rule 9）；S2 跨模块 `posted=true` 由谁置位未定；S3「finance-service API」措辞松，须明确 compile 依赖 `app-erp-finance-service`；S4 末条 Follow-up 缺触发条件。迭代 1 已修订：B1→Proof 改「移动单 `docStatus`=已完成(30) + `posted=true`(boolean)，凭证 `docStatus`=已过账」；B2→新增「过账失败不阻塞终态」Decision（try/catch 包裹，失败保持 DONE+posted=false，兜底扫描重试）+ `testPostingFailureLeavesMoveDonePostedFalse`；S1→五个 Decision 各补残留风险；S2→Phase 3 Add 明确 inventory 侧 `complete()` 成功后自行置 `posted=true`（与 Plan 1 引擎不持有源实体对齐）；S3→Phase 3 Add + Infra prereqs 明确 compile 依赖 `app-erp-finance-service`；S4→Follow-up 补触发条件。
- Independent draft review iteration 2: **accept / consensus**（ses_0e4e94d3effeQfWvehrpEYhgHt，独立 general 子代理，新会话）— 迭代 1 的 B1（move docStatus=已完成+posted boolean，已过账仅凭证）/B2（过账失败不阻塞终态 Decision + 失败路径测试）/S1（6 Decision 均补残留风险）/S2（inventory complete() 置 posted=true）/S3（compile 依赖 app-erp-finance-service，实时核实 artifactId + 无环）/S4（Follow-up 触发条件）全部经实时仓库核实 RESOLVED；无新阻塞；anti-slack 全净；6 Deferred + 7 Follow-up 均含触发；Rule 9/4/14 满足；与 Plan 1 seam 一致。1 项非阻塞 S1：Phase 3 称「无 erp-fin-api 模块」事实不准（erp-fin-api 实存，仅含生成 CRUD 契约）—— 决策本身正确（过账 SPI 在 erp-fin-service），已据建议修订措辞为「erp-fin-api 仅含生成 CRUD GraphQL 契约、不含过账 SPI，故依赖 app-erp-finance-service」。**共识达成**：计划为可接受的执行契约。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划为 greenfield BizModel + Provider + 测试，结束时运行一次完整仓库验证。

- [ ] 范围内行为完成：generateMove 契约 + 状态机 + 流水/余额 + 可用量/负库存 + 存货过账接入全部落地，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` P1 工作项 1.3 标注进展；当日日志已记
- [ ] 已运行验证：`mvn test -pl module-inventory/erp-inv-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（跨法人调拨/记录级预留/盘点/批次序列号全生命周期/两步调拨在途/FIFO成本 均为计划内 Non-Goal，非范围内降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 跨法人调拨内部交易凭证

- Classification: `optimization candidate`
- Why Not Blocking Closure: `business-type` 字典缺 INTER_TRANSFER 项（须人工批准扩字典，保护区域）；同法人内部调拨本就无凭证（cross-domain 明确）。跨法人须双方组织科目配置 + 内部利润处理。
- Successor Required: yes（触发条件：启用跨法人调拨时，先扩 `business-type` 字典 + 配置内部往来科目）

### 记录级预留（ErpInvReservation）按订单/工单预留管理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 状态机预留量作用于 `StockBalance` 聚合已满足移动单可用量校验；显式按销售订单/工单预留 + 有效期 + `ErpInvReservation` 单据管理是独立功能。
- Successor Required: yes（触发条件：需要按订单/工单显式预留库存时）

### 盘点单状态机与盘盈/盘亏移动单生成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 盘点是独立状态机（COUNTING）；盘点差异生成的移动单走本计划移动单流程，但盘点本身（盘点单 CRUD + 差异计算）不在本计划。
- Successor Required: yes（触发条件：实施盘点功能时）

### 批次/序列号全生命周期（过期校验、销售锁定）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 移动单行携带 `batchNo`/`serialNo` 并纳入可用量维度；批次过期校验/序列号已售锁定等完整状态机属 Follow-up。
- Successor Required: yes（触发条件：启用批次有效期/序列号追踪时）

### 两步调拨在途库（IN_TRANSIT）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划覆盖单步移动单；`ErpInvTransferOrder` 的两步调拨（在途库、SHIPPED/IN_TRANSIT/RECEIVED 头级状态）是独立功能。
- Successor Required: yes（触发条件：实施跨仓库两步调拨时）

### FIFO / 批次成本核算

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划基线落地移动加权平均（`costMethod` 默认）；FIFO/批次成本属 Follow-up。
- Successor Required: yes（触发条件：启用 FIFO 或批次计价时）

## Closure

Status Note: 计划可关闭的条件——StockMove 状态机 + generateMove 契约 + 幂等 + 不可变流水 + 余额驱动（移动加权平均）+ 可用量校验/负库存配置 + 存货过账端到端（DONE→凭证→`posted=true`）全部落地，行为测试全绿，根 `mvn test` BUILD SUCCESS 无回归，当日日志已记。

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理（新会话）填写>

Follow-up:

- 跨法人调拨内部交易凭证（见上方 Deferred）
- 记录级预留管理（见上方 Deferred）
- 盘点单状态机（见上方 Deferred）
- 批次/序列号全生命周期（见上方 Deferred）
- 两步调拨在途库（见上方 Deferred）
- FIFO/批次成本（见上方 Deferred）
- purchase/sales 调用方接入 `generateMove`（触发条件：purchase/sales BizModel 计划起草并实现入库/出库触发时）
