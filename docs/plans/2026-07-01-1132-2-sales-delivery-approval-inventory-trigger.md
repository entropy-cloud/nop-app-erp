# 2026-07-01-1132-2 销售单据审批状态机 + 出库触发库存移动（含可用量校验）

> Plan Status: active
> Mission: erp
> Work Item: core-business-roadmap P1 / 1.2（Sales Order BizModel：审批/出库触发/过账）——本批次聚焦其中的「出库触发 + 过账接线」核心
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/core-business-roadmap.md` P1 工作项 1.2
> Related: `docs/plans/2026-07-01-1132-1-purchase-receipt-approval-inventory-trigger.md`（同批，N=1 先于本计划；purchase 为镜像参考，本计划为其销售对称项）、`docs/plans/2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（其 Follow-up「purchase/sales 调用方接入 generateMove」的后继——本计划消费其 `generateMove` 契约）、`docs/plans/2026-07-01-0811-1-finance-posting-engine-foundation.md`（存货过账引擎，本计划不直接调用）、`docs/design/sales/state-machine.md`（销售单据三轴状态机权威源）、`docs/design/inventory/cross-domain.md`（`generateMove` 跨域契约 + 可用量校验规则）、`docs/design/flow-overview.md`（L2：销售出库→库存扣减）、`docs/design/sales/README.md`
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：codegen 完成、CRUD 全绿（18 域 90 冒烟测试），全部 BizModel 仍为 `CrudBizModel<T>` 空壳。库存域 StockMove BizModel（1.3）与财务过账引擎（1.5）已 `completed`：`IErpInvStockMoveBiz.generateMove(StockMoveRequest)` 已落地，**出库类（moveType=20）在 CONFIRM 校验可用量**（`availableQuantity ≥ quantity`，不足抛 `NopException` + 整事务回滚；`erp-inv.allow-negative-stock=true` 时跳过），DONE 后库存域 `InvAcctDocProvider`（非默认，声明 `SALES_OUTPUT`）触发存货估值凭证（借主营业务成本/贷存货）。**销售域尚未消费该契约**——本计划是 sales→inventory 跨域调用方，且是首个依赖「出库可用量校验」的业务域（销售独有，采购入库不校验）。

**销售域 BizModel/I*Biz 现状**（实时核实）：`module-sales/erp-sal-service/.../service/entity/ErpSalDeliveryBizModel extends CrudBizModel<ErpSalDelivery> implements IErpSalDeliveryBiz`（空壳）；`IErpSalDeliveryBiz extends ICrudBiz<ErpSalDelivery>`（空壳）。`ErpSalOrderBizModel` 同为空壳。`docs/design/sales/state-machine.md` 定义的三轴审批状态机（与采购镜像对称）**尚未实现**。

**实体模型已就绪**（实时核实 `module-sales/model/app-erp-sales.orm.xml`）：
- `ErpSalOrder`（销售订单头）：`code`/`customerId`/`warehouseId`/`businessDate`/`currencyId`/`exchangeRate`(**DECIMAL 20,8**)/金额族(**DECIMAL 20,4**，规范无 VARCHAR 陷阱)/`docStatus`(dict `erp-sal/doc-status`)/`approveStatus`(dict `erp-sal/approve-status`)/`receivedStatus`(dict `erp-sal/received-status`)/`deliveryStatus`(dict `erp-sal/delivery-status`)/`posted`/`postedAt`/`postedBy`/`approvedBy`/`approvedAt`；关系 `customer`→ErpMdPartner、`lines`→ErpSalOrderLine。
- `ErpSalDelivery`（销售出库单头，**本计划主角**）：`code`/`orgId`/`orderId`(→ErpSalOrder)/`customerId`/`warehouseId`/`businessDate`/`currencyId`/`exchangeRate`(DECIMAL)/金额族(DECIMAL)/`docStatus`/`approveStatus`/`posted`/`postedAt`/`postedBy`/`approvedBy`/`approvedAt`；关系 `order`→ErpSalOrder、`customer`→ErpMdPartner、`warehouse`→ErpMdWarehouse、`lines`→ErpSalDeliveryLine。**注意**：出库单自身无 `deliveryStatus`（进度回写到订单 `ErpSalOrder.deliveryStatus`）。
- `ErpSalDeliveryLine`（出库单行）：`deliveryId`/`orderLineId`/`lineNo`/`materialId`/`skuId`/`uoMId`/`quantity`(**DECIMAL 20,4**，实发)/`unitPrice`(DECIMAL)/`taxRate`(DECIMAL)/`taxAmount`(DECIMAL)/`amount`(DECIMAL)/`warehouseId`(出库库位)/`batchNo`；关系 `material`/`sku`/`uoM`。

**字典权威值**（实时核实 `module-sales/erp-sal-meta/.../dict/erp-sal/`）：
- `approve-status`：`UNSUBMITTED`=10 / `SUBMITTED`=20 / `APPROVED`=30 / `REJECTED`=40（与采购同）。
- `doc-status`：`DRAFT`=10 / `ACTIVE`=20 / `CANCELLED`=30。
- `delivery-status`（发货进度，int，派生）：`UNDELIVERED`=10 / `PARTIAL`=20 / `DELIVERED`=30。
- `received-status`（收款进度，int，派生）：`UNRECEIVED`=10 / `PARTIAL`=20 / `RECEIVED`=30（属发票/收款域，本计划不驱动）。

**库存跨域契约**（实时核实，已实现）：
- `generateMove(StockMoveRequest)`：出库类（moveType=20）在 CONFIRM 由库存域校验可用量（`availableQuantity ≥ quantity`），不足抛 `NopException`——**销售出库审核依赖此校验**：`approve` 调 `generateMove`，可用量不足则异常上抛、整个出库单审核回滚（对齐 `sales/state-machine.md` §4「出库可用量不足→APPROVED 时拒绝、整个审核回滚」与 `cross-domain.md`）。业务联动自动 DONE，`(relatedBillType,relatedBillCode)` 幂等。
- 出库类 DONE：库存域按移动加权平均快照 `unitCost`=当前 `avgCost` 写流水（**调用方不传 `unitCost`**），扣减余额，触发 `SALES_OUTPUT` 凭证。

**过账归属关键约束**（实时核实，**本计划核心约束**）：
- 库存域 `InvAcctDocProvider`（非默认）已声明 `SALES_OUTPUT`(20)。`ErpFinAcctDocRegistry`：**两个非默认 Provider 声明同一 businessType = 启动期 fail-fast**（`ERR_DUPLICATE_PROVIDER`）。故 **sales 域不得再注册声明 `SALES_OUTPUT` 的 Provider**——销售出库的存货过账（结转成本）由库存域（移动单 DONE）独占。本计划 sales 侧不新增过账 Provider。
- 过账入口为具体类 `ErpFinPostingService.post(PostingEvent)`（package `app.erp.fin.service.posting`，**无 `IErpFinPostingService` 接口**）；本计划不直接调用——移动单 DONE 时由库存域内部触发。

**模块依赖现状**（实时核实 pom.xml）：
- `erp-sal-service` 当前 compile 依赖仅 `app-erp-sales-dao`/`-meta`；已有 `app-erp-master-data-service`（**test scope**，跨域主数据测试数据）；**无 inventory / finance 依赖**。
- DAG 已核实无环（同 N=1）：inventory/finance 均不依赖 sales。故 sales 可安全新增 `inventory-dao`（compile）+ `inventory-service`（test）+ `master-data-dao`（compile，客户启用校验）依赖而不引入环。

**关键约束/陷阱**（实时核实）：
- 销售域金额/数量列均为 **DECIMAL**（规范，无采购域的 VARCHAR 陷阱）——构建 `StockMoveLineRequest.quantity`/`unitCost` 直接用 BigDecimal。
- **出库 `unitCost` 由库存域维护**（移动加权平均快照），sales 调用方**不传 `unitCost`**（传了也会被库存域出库逻辑覆盖为 `avgCost` 快照）；sales 出库单行的 `unitPrice`（售价）≠ 存货成本，不得作为 `unitCost` 传入。
- 可用量校验在库存域 `confirm()` 内执行（非 sales 侧预检）——sales `approve` 调 `generateMove` 即触发；不足则异常回滚整个审核。`erp-inv.allow-negative-stock=true` 时库存域跳过校验（销售侧无须额外处理）。
- `ErpSalDelivery.posted` 语义：本计划定义为「其触发的出库移动单的存货过账是否成功」（`delivery.posted = move.posted`）；凭证回链 `billCode`=移动单 code。
- 客户启用状态校验字段：`ErpMdPartner.status`（dict `erp-md/active-status`），与供应商同实体。

**剩余差距**：(1) ErpSalDelivery 三轴审批状态机；(2) 审核通过 → 构造 `StockMoveRequest`(OUTGOING) 调 `generateMove`（可用量不足回滚）+ `posted` 接线 + `deliveryStatus` 回写；(3) 反审核/作废 → 反向冲销移动单；(4) 服务层集成测试。

## Goals

- **销售出库审批状态机落地**：`IErpSalDeliveryBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit` 方法签名；`ErpSalDeliveryBizModel` 实现三轴迁移（与采购镜像对称），每条校验前置，违反抛 `NopException`；客户启用校验。
- **出库审核触发库存移动（含可用量校验）**：`approve(deliveryId)` 通过后构造 `StockMoveRequest`（`moveType`=OUTGOING(20)、`relatedBillType`=`"ERP_SAL_DELIVERY"`、`relatedBillCode`=`delivery.code`、`sourceWarehouseId`=`delivery.warehouseId`、行取自 `ErpSalDeliveryLine`：`materialId`/`skuId`/`uoMId`/`quantity`/`batchNo`，**不传 `unitCost`**），调 `IErpInvStockMoveBiz.generateMove(...)`；可用量不足由库存域抛 `NopException` 致整个审核回滚（对齐 state-machine §4 销售独有约束）。
- **posted 接线 + 发货状态回写**：`delivery.posted = move.posted`、`approveStatus`=APPROVED、`approvedBy/At`；按「累计已发 / 订单数量」重算源 `ErpSalOrder.deliveryStatus`（UNDELIVERED→PARTIAL→DELIVERED），BigDecimal 比较。
- **反向冲销路径**：`reverseApprove`/`cancel` 前置：已生成出库移动单者须先 `IErpInvStockMoveBiz.reverse(moveId)` 冲销（库存域冲销流水/余额/红字凭证）；未冲销即拒绝。
- **服务层集成测试证明**：审核→出库移动单+库存余额扣减+凭证+`posted=true`、可用量不足拒绝+审核回滚、幂等重审、负库存配置放行、发货状态回写、反审核须先冲销，全部可重复通过。

## Non-Goals

- **不修改任何 `model/*.orm.xml` / `.api.xml`**（保护区域）。
- **不注册 sales 域过账 Provider（`SalAcctDocProvider`）**——`SALES_OUTPUT` 已由库存域 `InvAcctDocProvider` 独占（非默认），sales 再声明触发 `ERR_DUPLICATE_PROVIDER`。`AR_INVOICE`/`RECEIPT` 的 Provider 属销售发票/收款计划（1.7），不在本计划。
- **不实现销售发票 / 收款单 / 核销**——属 1.7（销售到收款端到端）；本计划只覆盖「订单→出库」段。
- **不实现销售订单（ErpSalOrder）审核状态机 + 客户信用额度校验**——订单审核仅状态推进；信用额度校验需客户信用数据（`flow-overview.md` §2.2），属独立关注点。本计划聚焦出库单（消费 `generateMove`、解除 Follow-up 阻塞的核心面）。范围缩小理由见 Task Route。
- **不实现报价→订单转化（1.0b）**——属独立工作项，不同 owner doc（`quotation.md`）。
- **不实现销售退货（1.10）**——退货触发入库移动单 + 红字发票 + 退款，属独立工作项。
- **不接入 nop-wf 审批流**——审核 = 直接状态迁移（对齐 StockMove/N=1 基线）；nop-wf 编排为 Follow-up。
- **不做出库单行级超发容差校验**——属独立规则。
- **不做异步过账派发**——过账由库存移动 DONE 同步驱动。

## Task Route

- Type: `architecture change`（sales→inventory 跨域同步调用契约边界，含销售独有的可用量校验依赖）+ `implementation-only change`（greenfield BizModel，不改公共 API/ORM）。
- Owner Docs: `docs/design/sales/state-machine.md`（三轴状态机，与采购镜像，聚焦差异：出库可用量校验、并发扣减）、`docs/design/inventory/cross-domain.md`（`generateMove` 调用方契约 + 可用量校验规则）、`docs/design/finance/posting.md`（过账归属，仅引用）、`docs/architecture/testing-strategy.md`、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`。
- Skill Selection Basis: `Skill: none`（实施）。`docs/skills/README.md` 无 BizModel 编写技能匹配；遵循平台 service-layer 与 state-machine/cross-domain 文档。独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`；状态机正确性用 `state-machine-business-review-prompt.md` 复核（见 Phase 1 Proof）。
- 范围缩小理由（规则 10）：工作项 1.2 标题「Sales Order BizModel（审批/出库触发/过账）」，解除 Follow-up 阻塞、非平凡的核心是「出库触发库存（含可用量校验）+ 过账接线」；订单审核（状态 only）+ 信用额度校验是可分离的不同结果面，移出范围为显式后续。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-sal-app` 已含 `quarkus-jdbc-h2`；`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`）。
- **新增结构性依赖**（实时核实无环）：
  - `erp-sal-service/pom.xml` 新增 **compile** scope 依赖 `app-erp-inventory-dao`（`IErpInvStockMoveBiz` + `StockMoveRequest`/`StockMoveLineRequest` DTO）。
  - `erp-sal-service/pom.xml` 新增 **compile** scope 依赖 `app-erp-master-data-dao`（`IErpMdPartnerBiz`/`ErpMdPartner` 客户启用校验）。
  - `erp-sal-service/pom.xml` 新增 **test** scope 依赖 `app-erp-inventory-service`（`generateMove` Bean 实现）；`app-erp-master-data-service`（test）已存在。
- 无数据迁移/回滚脚本需求（greenfield BizModel）。

## Execution Plan

### Phase 1 - 销售出库审批状态机（三轴）+ 客户启用校验

Status: planned
Targets: `module-sales/erp-sal-dao/src/main/java/app/erp/sal/biz/IErpSalDeliveryBiz.java`（增签名）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalDeliveryBizModel.java`（实现）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/ErpSalErrors.java`、`module-sales/erp-sal-service/src/test/.../entity/`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [ ] `Decision`：状态迁移前置与目标态。裁决：与采购镜像（`sales/state-machine.md` §2）——提交 `UNSUBMITTED(10)→SUBMITTED(20)`；审核通过 `SUBMITTED→APPROVED(30)`（前置客户启用 + 后续 Phase 2 出库触发，可用量校验在库存域 CONFIRM 内）；驳回/重提/撤销提交/反审核（APPROVED→REJECTED，前置冲销出库移动单）/作废。违反抛 `NopException(ERR_ILLEGAL_STATUS_TRANSITION)`。备选（被否）：单状态机——三轴分离避免组合爆炸。残留风险：撤销提交在无 nop-wf 时未防审核人介入（Follow-up）。
  - Skill: none
- [ ] `Decision`：可用量校验归属。裁决：**不在 sales 侧预检可用量**——`approve` 直接调 `generateMove(OUTGOING)`，由库存域 `confirm()` 内 `validateAvailable`（`availableQuantity ≥ quantity`）裁决；不足抛 `NopException` 上抛致整个出库单审核回滚（对齐 state-machine §4 销售独有 + cross-domain「不足拒绝+审核回滚」）。备选（被否）：sales 侧先查 `ErpInvStockBalance` 预检——重复校验逻辑、且与库存域并发窗口不一致。残留风险：依赖库存域校验正确性（已由 0811-2 测试覆盖）；`erp-inv.allow-negative-stock=true` 时库存域跳过，销售侧无须额外处理。
  - Skill: none
- [ ] `Add`：`IErpSalDeliveryBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove`/`withdrawSubmit` 方法签名；`ErpSalDeliveryBizModel` 实现迁移——校验前置、落地 `approvedBy`/`approvedAt`(`CoreMetrics`)。客户启用校验经 `IErpMdPartnerBiz`/`ErpMdPartner.status`（dict `erp-md/active-status`），停用抛 `ERR_PARTNER_INACTIVE`。`@SingleSession @Transactional`，`IErpInvStockMoveBiz`/`IErpMdPartnerBiz` 经 `@Inject` 包级可见字段注入。
  - Skill: none
- [ ] `Proof`：服务层集成测试（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + master-data/inv test 依赖，`createPrereqs()` 自建客户/物料/仓库/库位/单位/币种 + 预置库存余额）——`testSubmitRejectResubmit`、`testIllegalTransitionRejected`、`testInactiveCustomerRejected`、`testCancelFromDraft`。`mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: none
- [ ] `Proof`：审批状态机正确性复核——用 `docs/skills/state-machine-business-review-prompt.md` 针对终态/可达性/反审核目标态(REJECTED)/出库可用量异常路径自检，结论记录于本阶段（须执行）。
  - Skill: state-machine-business-review-prompt

Exit Criteria:

> 本阶段交付三轴状态机 + 客户校验。完整仓库 `mvn test` 归 Closure Gates。

- [ ] 4 个状态机行为测试存在且 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [ ] 非法迁移拒绝 + 客户停用拒绝均经测试证明（解除 Phase 2 出库触发的状态前置阻塞）

### Phase 2 - 出库审核触发库存移动（generateMove + 可用量校验）+ posted 接线 + 发货状态回写

Status: planned
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalDeliveryBizModel.java`（approve 调 generateMove）、`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/.../DeliveryStockMoveBuilder.java`（构造 StockMoveRequest）、`module-sales/erp-sal-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 + **库存域 StockMove BizModel（0811-2）已 completed 可消费 `generateMove`**

- [ ] `Decision`：过账归属与 `posted` 语义。裁决：销售出库的存货估值过账（结转成本）由库存域独占——`approve` 调 `generateMove(OUTGOING)`，移动单 DONE 后库存域触发 `SALES_OUTPUT`（`billHeadCode`=移动单 code）。sales **不注册任何过账 Provider**（避免与 `InvAcctDocProvider` 的 `SALES_OUTPUT` 非默认声明冲突致 `ERR_DUPLICATE_PROVIDER`）。`delivery.posted = move.posted`、`postedAt`=`CoreMetrics.currentDateTime()`、`postedBy`=当前用户。备选（被否）：sales 自注册 `SALES_OUTPUT` Provider——启动 fail-fast。残留风险：凭证回链绑定移动单 code（与 N=1 同，可接受）。
  - Skill: none
- [ ] `Decision`：`StockMoveRequest` 字段映射（出库差异点）。裁决：`moveType`=OUTGOING(20)；`relatedBillType`=`"ERP_SAL_DELIVERY"`；`relatedBillCode`=`delivery.code`（幂等键）；`sourceWarehouseId`=`delivery.warehouseId`（出库源仓），`destWarehouseId`=null（发往客户无目的仓）；`businessDate`/`orgId`/`acctSchemaId`/`currencyId` 取自出库单。行映射：`materialId`/`skuId`/`uoMId`/`quantity`(BigDecimal 直读 DECIMAL)/`batchNo`——**不传 `unitCost`**（出库由库存域按移动加权平均 `avgCost` 快照，售价 `unitPrice` 不得作为成本传入）。`StockMoveLineRequest.currencyId` 不传（库存域 `newLines` 回退到头 `currencyId`）。`sourceLocationId` 取决于 `deliveryLine.warehouseId` 语义：该列 displayName 为「出库库位」但字段名 `warehouseId` 且无关系声明——实施时核实 ErpMdLocation 模型，若实为库位 ID 则映射 `sourceLocationId`，否则留空走整仓余额（库存余额按 `warehouseId×locationId×batchNo` 维度，locationId 为 null 合法）。备选（被否）：传售价作 `unitCost`——售价≠存货成本，会污染流水成本。残留风险：`deliveryLine.warehouseId` 语义未定时按整仓余额（locationId=null）为安全默认；成本口径由库存域权威维护。
  - Skill: none
- [ ] `Add`：`approve(deliveryId)` 末尾（置 APPROVED 后、同事务内）：(1) `DeliveryStockMoveBuilder` 构造 `StockMoveRequest`；(2) 调 `IErpInvStockMoveBiz.generateMove(request)`——可用量不足由库存域抛 `NopException` 致整个 approve 回滚（出库单保持 SUBMITTED）；(3) `delivery.posted = move.posted`、`postedAt`/`postedBy`；(4) 重算源 `ErpSalOrder.deliveryStatus`（UNDELIVERED/PARTIAL/DELIVERED，BigDecimal 比较）。
  - Skill: none
- [ ] `Decision`：反审核/作废的冲销机制（Design A：内部冲销 + 幂等双冲销保护）。裁决：`reverseApprove`/`cancel` 在内部完成冲销——(a) 按幂等键 `(relatedBillType=ERP_SAL_DELIVERY, relatedBillCode=delivery.code)` 经注入的 `IErpInvStockMoveBiz` 反查既有出库移动单（**无 ORM FK，纯查询，不改 model**；移动单实体类经 inventory-dao compile 依赖对 sales 可见）；(b) 按 `(relatedBillType=REVERSAL, relatedBillCode=原移动单.code)` 查是否已存在反向冲销移动单——已存在则跳过（幂等防双冲销，因库存域 `reverse()` 不查既有 REVERSAL、重复调用会产出多张冲销单），不存在则调 `IErpInvStockMoveBiz.reverse(originalMoveId)` 生成反向冲销移动单（库存域 DONE 时冲销流水/余额/红字凭证）；(c) 冲销确立后才 APPROVED→REJECTED（或 docStatus→CANCELLED）。非 APPROVED 且无移动单者（草稿/已驳回）直接迁移。备选（被否，Design B「调用方先外部冲销、reverseApprove 仅校验前置」）：把原子反审核拆两步、调用方易遗漏冲销致状态与库存不一致。残留风险：库存域 `reverse()` 不改原单状态，故「是否已冲销」须经 REVERSAL 移动单二跳查询判定（已纳入裁决，不依赖任何 delivery 侧字段）。
  - Skill: none
- [ ] `Add`：`reverseApprove(deliveryId)`（反审核，APPROVED→REJECTED）与 `cancel(deliveryId)`（任意非终态→docStatus=CANCELLED）——按上述 Design A 实现：反查既有出库移动单；已存在 REVERSAL 移动单则跳过冲销，否则调 `IErpInvStockMoveBiz.reverse(moveId)`；冲销确立后迁移状态。原移动单缺失（APPROVED 却查不到，数据不一致）抛 `NopException(ERR_MOVE_NOT_FOUND)`。
  - Skill: none
- [ ] `Proof`：`testApproveGeneratesOutgoingMoveAndPosting`（审核→出库移动单 DONE、库存余额扣减、`ErpFinVoucher`+`ErpFinVoucherLine`+`ErpFinVoucherBillR` 落地、`delivery.posted=true`、`approveStatus`=APPROVED；posting 测试主数据 setup 复用 `2026-07-01-0811-2` 存货过账测试 `createPrereqs()` 模式：`ErpFinAcctSchema` + 币种 + `ErpMdSubject`(6401 主营业务成本/1401 库存商品)）、`testApproveInsufficientAvailableRollsBack`（库存不足→`NopException`+出库单保持 SUBMITTED+无移动单+无凭证）、`testApproveIdempotent`、`testNegativeStockConfigAllowsShortage`（`allow-negative-stock=true`→不足仍可审核）、`testDeliveryStatusRollupToOrder`、`testReverseApproveInternallyReversesMove`（反审核→内部生成反向冲销移动单 + 红字凭证 + APPROVED→REJECTED；二次反审核幂等不产生第二张冲销单）。`mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: none

Exit Criteria:

> 本阶段交付出库触发 + 可用量校验 + 过账接线 + 状态回写 + 反向冲销前置。完整仓库 `mvn test` 归 Closure Gates。

- [ ] 6 个出库触发行为测试存在且 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [ ] 可用量不足拒绝+回滚、负库存放行、幂等、发货状态回写、反审核须先冲销 均经测试证明

### Phase 3 - 端到端串联 + 收尾

Status: planned
Targets: `module-sales/erp-sal-service/src/test/.../`、`docs/logs/2026/07-01.md`、`docs/backlog/core-business-roadmap.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] `Proof`：端到端测试 `testOrderToDeliveryToEnd`（建订单+出库单→提交→审核→出库移动单 DONE+库存余额扣减+存货凭证+`posted=true`+订单 `deliveryStatus` 回写→可用量不足分支拒绝→反审核须先冲销→冲销后红字凭证净额为 0）。证明 sales→inventory→finance 三域经 `generateMove` 端到端打通，含销售独有可用量门控。`mvn test -pl module-sales/erp-sal-service -am` 全绿。
  - Skill: none
- [ ] `Add`：更新当日开发日志 `docs/logs/2026/07-01.md`（时间倒序），记录销售出库审批+出库触发落地 + 验证状态。
  - Skill: none
- [ ] `Add`：`docs/backlog/core-business-roadmap.md` 工作项 1.2 标注进展（出库触发段 `done`；订单审核/信用额度/发票/收款仍 `todo`）。
  - Skill: none

Exit Criteria:

> 本阶段交付端到端打通 + 文档对齐。完整仓库 `mvn test` 归 Closure Gates。

- [ ] 端到端测试存在且 `mvn test -pl module-sales/erp-sal-service -am` 全绿
- [ ] 当日日志已记；roadmap 工作项 1.2 进展已标注

## Draft Review Record

- Independent draft review iteration 1: **accept / consensus**（ses_0e43cd7abffe5Iatny3h0asXhZ，独立 general 子代理，新会话）— 所有材料性主张经实时仓库核实属实：roadmap 1.2 范围缩小满足规则 10（理由充分）；销售状态机镜像采购 + 销售独有可用量校验（§4）匹配 owner doc；`generateMove` 出库类 CONFIRM 校验可用量、`bookOutgoing` 用 `avgCost` 快照忽略调用方 `unitCost`（故 sales 不传 unitCost 正确）；`InvAcctDocProvider` 非默认独占 SALES_OUTPUT、purchase/sales 再声明会 fail-fast（不注册 sales Provider 决策正确）；sales 实体 DECIMAL 无 VARCHAR 陷阱；模块 DAG 无环；`erp-inv.allow-negative-stock` 经 `AppConfig.var` 读取；N=1/N=2 拆分满足规则 4 与 14（不同域、不同 owner doc、不同结果面：INCOMING 不校验可用量 vs OUTGOING 含 `validateAvailable` 依赖）；反松弛/规则 7/8/9 全净；阶段退出仅本地化 `mvn test -pl module-sales`，全仓库验证归 Closure Gates。2 项非阻塞建议已吸收：S1 `deliveryLine.warehouseId`(displayName 出库库位) 语义未定时整仓余额默认 + 实施时核实 ErpMdLocation；S2 反审核双冲销保护——已统一为 Design A（内部冲销 + 按 `(REVERSAL,原单.code)` 二跳查询幂等防双冲销），与 N=1 对称。**共识达成**：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [ ] 范围内行为完成：出库审批三轴状态机 + 出库触发 `generateMove`（含可用量校验/负库存）+ `posted` 接线 + 发货状态回写 + 反向冲销前置 全部落地，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` 工作项 1.2 标注进展；当日日志已记
- [ ] 已运行验证：`mvn test -pl module-sales/erp-sal-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（订单审核/信用额度/发票/收款/退货/报价转化 均为计划内 Non-Goal，非范围内降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 销售订单（ErpSalOrder）审核状态机 + 客户信用额度校验

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 订单审核仅状态推进；信用额度校验需客户信用数据（`flow-overview.md` §2.2），是可分离关注点。
- Successor Required: yes（触发条件：实施报价→订单转化 1.0b 或订单信用控制时）

### nop-wf 审批流编排

- Classification: `optimization candidate`
- Why Not Blocking Closure: 审核 = 直接状态迁移（对齐 N=1/StockMove 基线）；nop-wf 编排属运营基础设施。
- Successor Required: yes（触发条件：需要多级审批人路由与流程挂起时）

### 销售发票/收款 Provider（AR_INVOICE/RECEIPT）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 1.7（销售到收款端到端）；本计划出库段不涉及应收/收款凭证。
- Successor Required: yes（触发条件：起草销售发票/收款 BizModel 计划时）

## Closure

Status Note: （待结束审计后填写）计划可关闭的条件——销售出库审批三轴状态机 + 出库审核触发 `generateMove`(OUTGOING，含可用量校验/负库存) + `posted` 接线 + 发货状态回写 + 反向冲销前置全部落地，行为测试全绿，根 `mvn test` BUILD SUCCESS 无回归，当日日志已记。

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计子代理填充）

Follow-up:

- 销售订单审核状态机 + 客户信用额度校验（见上方 Deferred）
- nop-wf 审批流编排（见上方 Deferred）
- 销售发票/收款 Provider（见上方 Deferred）
- 出库单行级超发容差校验（触发条件：实施超发控制规则时）
