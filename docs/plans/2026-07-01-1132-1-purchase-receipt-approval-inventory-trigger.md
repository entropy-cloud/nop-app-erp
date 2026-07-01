# 2026-07-01-1132-1 采购单据审批状态机 + 入库触发库存移动

> Plan Status: completed
> Mission: erp
> Work Item: core-business-roadmap P1 / 1.1（Purchase Order BizModel：审批/入库触发/过账）——本批次聚焦其中的「入库触发 + 过账接线」核心
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/core-business-roadmap.md` P1 工作项 1.1
> Related: `docs/plans/2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（其 Follow-up「purchase/sales 调用方接入 generateMove」的后继——本计划消费其 `IErpInvStockMoveBiz.generateMove` 契约）、`docs/plans/2026-07-01-0811-1-finance-posting-engine-foundation.md`（存货过账引擎，本计划不直接调用，由库存移动 DONE 间接驱动）、`docs/design/purchase/state-machine.md`（采购单据三轴状态机权威源）、`docs/design/inventory/cross-domain.md`（`generateMove` 跨域契约权威源）、`docs/design/flow-overview.md`（L2 跨域协作层：采购入库→库存）、`docs/design/purchase/README.md`
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：codegen 完成、CRUD 全绿（18 域 90 冒烟测试），全部 BizModel 仍为 `CrudBizModel<T>` 空壳。库存域 StockMove BizModel（1.3）与财务过账引擎（1.5）已 `completed`：`IErpInvStockMoveBiz.generateMove(StockMoveRequest)` 契约已落地（业务联动自动 DRAFT→CONFIRMED→DONE、`(relatedBillType,relatedBillCode)` 幂等、出库类在 CONFIRM 校验可用量），存货过账由库存域 `InvAcctDocProvider`（非默认，声明 `PURCHASE_INPUT`/`SALES_OUTPUT`）在移动单 DONE 时触发。**采购域尚未消费该契约**——本计划是首个 purchase→inventory 跨域调用方。

**采购域 BizModel/I*Biz 现状**（实时核实）：`module-purchase/erp-pur-service/.../service/entity/ErpPurReceiveBizModel extends CrudBizModel<ErpPurReceive> implements IErpPurReceiveBiz`（空壳，无自定义方法）；`IErpPurReceiveBiz extends ICrudBiz<ErpPurReceive>`（空壳）。`ErpPurOrderBizModel` 同为空壳。`docs/design/purchase/state-machine.md` 定义的三轴审批状态机**尚未实现**。

**实体模型已就绪**（实时核实 `module-purchase/model/app-erp-purchase.orm.xml`）：
- `ErpPurOrder`（采购订单头）：`code`/`supplierId`/`warehouseId`/`businessDate`/`currencyId`/`exchangeRate`(VARCHAR)/金额族(均 **VARCHAR** 存储，见下陷阱)/`docStatus`(int dict `erp-pur/doc-status`)/`approveStatus`(int dict `erp-pur/approve-status`)/`paidStatus`(dict `erp-pur/paid-status`)/`receiveStatus`(dict `erp-pur/receive-status`)/`posted`(bool)/`postedAt`/`postedBy`/`approvedBy`/`approvedAt`；关系 `supplier`→ErpMdPartner、`warehouse`→ErpMdWarehouse、`lines`→ErpPurOrderLine。
- `ErpPurReceive`（采购入库单头，**本计划主角**）：`code`/`orgId`/`orderId`(→ErpPurOrder)/`supplierId`/`warehouseId`/`businessDate`/`currencyId`/`exchangeRate`(VARCHAR)/金额族(VARCHAR)/`receiveStatus`(dict `erp-pur/receive-status`)/`receiveType`(dict `erp-pur/receive-type`)/`docStatus`/`approveStatus`/`posted`/`postedAt`/`postedBy`/`approvedBy`/`approvedAt`；关系 `order`→ErpPurOrder、`supplier`→ErpMdPartner、`warehouse`→ErpMdWarehouse、`lines`→ErpPurReceiveLine。
- `ErpPurReceiveLine`（入库单行）：`receiveId`/`orderLineId`/`lineNo`/`materialId`/`skuId`/`uoMId`/`quantity`(**DECIMAL 20,4**，实收数量)/`rejectedQuantity`/`unitPrice`(**VARCHAR** domain unitPrice)/`taxRate`(VARCHAR)/`taxAmount`(VARCHAR)/`amount`(VARCHAR)/`warehouseId`(入库库位)/`batchNo`；关系 `material`/`sku`/`uoM`。

**字典权威值**（实时核实 `module-purchase/erp-pur-meta/.../dict/erp-pur/`）：
- `approve-status`（审核轴，int）：`UNSUBMITTED`=10 / `SUBMITTED`=20 / `APPROVED`=30 / `REJECTED`=40。
- `doc-status`（业务生命周期轴，int）：`DRAFT`=10 / `ACTIVE`=20 / `CANCELLED`=30。
- `receive-status`（收货进度，int，派生）：`UNRECEIVED`=10 / `PARTIAL`=20 / `RECEIVED`=30。

**库存跨域契约**（实时核实 `module-inventory/erp-inv-dao/.../biz/IErpInvStockMoveBiz.java` + `StockMoveRequest.java`，已实现）：
- `ErpInvStockMove generateMove(StockMoveRequest)`：`relatedBillType`+`relatedBillCode` 均非空时判定业务联动，自动 DRAFT→CONFIRMED→DONE；幂等键 `(relatedBillType, relatedBillCode)`，重复调用反查命中即返回既有移动单。
- `StockMoveRequest`：`moveType`(int：INCOMING=10/OUTGOING=20/INTERNAL=30/MANUFACTURE=40)/`orgId`/`businessDate`/`sourceWarehouseId`/`sourceLocationId`/`destWarehouseId`/`destLocationId`/`relatedBillType`/`relatedBillCode`/`acctSchemaId`/`currencyId`/`code`/`lines`。
- `StockMoveLineRequest`：`materialId`/`skuId`/`uoMId`/`quantity`(BigDecimal)/`unitCost`(BigDecimal，可选)/`currencyId`/`batchNo`/`serialNo`/`sourceLocationId`/`destLocationId`。
- 入库类（moveType=10）不校验可用量（增加库存）；DONE 后库存域 `InvPostingDispatcher` 按 moveType 派生 `PURCHASE_INPUT`(10)→`InvAcctDocProvider` 生成存货估值凭证（借存货/贷暂估应付），成功置移动单 `posted=true`，失败吞异常保持 `posted=false`（不阻塞移动单终态）。

**过账归属关键约束**（实时核实，**本计划核心约束**）：
- 库存域 `InvAcctDocProvider`（非默认）已声明 `PURCHASE_INPUT`(10) + `SALES_OUTPUT`(20)。`ErpFinAcctDocRegistry` 语义：**两个非默认 Provider 声明同一 businessType = 启动期 fail-fast**（`ERR_DUPLICATE_PROVIDER`）。故 **purchase 域不得再注册声明 `PURCHASE_INPUT` 的 Provider**——采购入库的存货过账由库存域（移动单 DONE）独占，`billHeadCode`=移动单 code。本计划 purchase 侧不新增过账 Provider。
- 过账入口为具体类 `ErpFinPostingService.post(PostingEvent)`（package `app.erp.fin.service.posting`，**无 `IErpFinPostingService` 接口**）；本计划不直接调用它——移动单 DONE 时由库存域内部触发。

**模块依赖现状**（实时核实 pom.xml）：
- `erp-pur-service` 当前 compile 依赖仅 `app-erp-purchase-dao`/`-meta`；**无 inventory / finance / master-data 依赖**（master-data 连 test scope 都没有，与 sales 不同）。
- `erp-sal-service` 已有 `app-erp-master-data-service`（**test scope**，注释说明用于跨域业务对象注册使 GraphQL `validateRefValue` 在 H2 通过）。
- DAG 已核实无环：finance 为纯 sink；inventory→finance 单向；inventory/finance 均不依赖 purchase。故 purchase 可安全新增 `inventory-dao`（compile）+ `inventory-service`（test）+ `master-data-service`（test）依赖而不引入环。

**关键约束/陷阱**（实时核实）：
- 采购域金额/单价列（`ErpPurOrder`/`ErpPurReceive` 及其行的 `exchangeRate`/`unitPrice`/`taxRate`/`amount` 族）声明 `domain="amount|unitPrice|..."` 但 **stdSqlType 被覆盖为 VARCHAR**（`stdDataType="string"`）——构建 `StockMoveLineRequest.unitCost`/`quantity` 时 `quantity`(DECIMAL) 可直接读，但 `unitPrice`(VARCHAR) 须 `new BigDecimal(str)` 解析，空值/格式异常须防护。（销售域无此问题，用 DECIMAL。）
- `ErpPurReceive.posted` 的语义：本计划定义为「其触发的入库移动单的存货过账是否成功」（`receive.posted = move.posted`），而非 purchase 域自有凭证——因为凭证由库存域以移动单为源生成。回链 `ErpFinVoucherBillR.billCode`=移动单 code，非入库单 code。
- 主数据跨域引用为机制 B（`notGenCode="true"`，实体类不在 purchase dao 生成，仅 EQL 导航）——故 purchase Java 代码引用 `ErpMdPartner`/`IErpMdPartnerBiz` 须 compile 依赖 `app-erp-master-data-dao`。
- 供应商启用状态校验字段：`ErpMdPartner.status`（dict `erp-md/active-status`），实时核实见 `module-master-data/model/app-erp-master-data.orm.xml`（partner 实体 status 列）。

**剩余差距**：(1) ErpPurReceive 三轴审批状态机（submit/approve/reject/cancel/reverse）落地；(2) 审核通过 → 构造 `StockMoveRequest`(INCOMING) 调 `generateMove` + 幂等 + `posted` 接线 + `receiveStatus`/订单 `receiveStatus` 回写；(3) 反审核/作废 → 反向冲销移动单；(4) 服务层集成测试证明端到端。

## Goals

- **采购入库审批状态机落地**：`IErpPurReceiveBiz` 增 `submit`/`approve`/`reject`/`cancel`/`reverseApprove` 方法签名；`ErpPurReceiveBizModel` 实现三轴迁移（审核轴 UNSUBMITTED→SUBMITTED→APPROVED/REJECTED、驳回→重提；业务轴 DRAFT→CANCELLED），每条迁移校验前置 `approveStatus`/`docStatus`，违反抛 `NopException`；`@BizMutation` 自动事务。
- **入库审核触发库存移动**：`approve(receiveId)` 通过后构造 `StockMoveRequest`（`moveType`=INCOMING(10)、`relatedBillType`=`"ERP_PUR_RECEIVE"`、`relatedBillCode`=`receive.code`、`destWarehouseId`=`receive.warehouseId`、行取自 `ErpPurReceiveLine`：`materialId`/`skuId`/`uoMId`/`quantity`/`unitCost`=`unitPrice`(VARCHAR→BigDecimal)/`batchNo`），调 `IErpInvStockMoveBiz.generateMove(...)`（业务联动自动 DONE、幂等）；`receive.posted = move.posted`、`receive.approveStatus`=APPROVED、`receive.approvedBy/At` 落地。
- **收货状态回写**：审核通过后按「累计已收 / 订单数量」重算 `receive.receiveStatus` 与源 `ErpPurOrder.receiveStatus`（UNRECEIVED→PARTIAL→RECEIVED），BigDecimal 比较，写 DECIMAL/VARCHAR 列按列类型。
- **反向冲销路径**：`reverseApprove(receiveId)`（反审核，目标态 REJECTED）或 `cancel` 前置：若已生成入库移动单，须先 `IErpInvStockMoveBiz.reverse(moveId)` 生成反向冲销移动单（库存域负责冲销流水/余额/红字凭证）；未冲销即拒绝反审核（对齐 state-machine §3 强约束）。
- **服务层集成测试证明**：审核→入库移动单生成+库存余额增加+存货过账凭证落地+`posted=true`、幂等重审空操作、可用量/供应商停用等异常拒绝、反审核须先冲销、收货状态回写正确，全部可重复通过。

## Non-Goals

- **不修改任何 `model/*.orm.xml` / `.api.xml`**（保护区域）——复用已生成实体；无 ORM 变更即无需 regen。
- **不注册 purchase 域过账 Provider（`PurAcctDocProvider`）**——`PURCHASE_INPUT` 已由库存域 `InvAcctDocProvider` 独占（非默认），purchase 再声明会触发 `ERR_DUPLICATE_PROVIDER` 启动 fail-fast。`AP_INVOICE`/`PAYMENT` 的 Provider 属采购发票/付款计划（1.4/1.6），不在本计划。
- **不实现采购发票 / 付款单 / 三单匹配 / 核销**——属 1.4（三单匹配）与 1.6（采购到付款端到端）；本计划只覆盖「订单→入库」段的入库触发。
- **不实现采购订单（ErpPurOrder）审核状态机**——订单审核仅状态推进（无库存/凭证触发），是可分离的轻量关注点；本计划聚焦入库单（消费 `generateMove`、解除 Follow-up 阻塞的核心面）。订单审核作为显式后续（见 Follow-up），范围缩小理由见 Task Route。
- **不实现请购单→订单转化（1.0a）与报价（1.0b）**——属独立工作项，不同 owner doc（`requisition.md`）。
- **不实现采购退货（1.9）**——退货触发的是出库移动单（OUTGOING）+ 红字发票，属独立工作项。
- **不接入 nop-wf 审批流**——本计划审核 = 直接状态迁移 + `@BizMutation`（对齐 StockMove 计划的同步状态机基线）；nop-wf 编排（提交流、挂起、审批人路由）为 Follow-up。仓内既有 `ErpPurOrder/approve.task.xml` 为手写示意骨架，且与实际 ORM 不符（引用了不存在的 `order.status`/`order.dept`/`order.orderNo`），不作为本计划依据。
- **不做订单行级超收容差校验**——属 1.4 三单匹配范畴。
- **不做异步过账派发**——过账由库存移动 DONE 同步驱动（库存域已实现 `@Transactional(REQUIRES_NEW)`）；purchase 侧无异步接线。

## Task Route

- Type: `architecture change`（purchase→inventory 是新的跨域同步调用契约边界，影响采购入库一致性）+ `implementation-only change`（greenfield BizModel 方法，不改公共 API 契约或 ORM）。
- Owner Docs: `docs/design/purchase/state-machine.md`（三轴状态机）、`docs/design/inventory/cross-domain.md`（`generateMove` 调用方契约）、`docs/design/finance/posting.md`（过账归属，仅引用不调用）、`docs/architecture/testing-strategy.md`（测试 runbook + CRUD 冒烟沉淀的跨域主数据依赖模式）、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（CrudBizModel 扩展、I*Biz 跨域注入）。
- Skill Selection Basis: `Skill: none`（实施）。`docs/skills/README.md` 现有技能均为审计/审查方法，无 BizModel 编写技能匹配；实施遵循平台 service-layer 指南与 state-machine/cross-domain 文档。独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`；审批状态机正确性用 `state-machine-business-review-prompt.md` 复核（见 Phase 1 Proof）。
- 范围缩小理由（规则 10 要求记录）：roadmap 工作项 1.1 标题为「Purchase Order BizModel（审批/入库触发/过账）」，其解除 Follow-up 阻塞、非平凡的核心是「入库触发库存 + 过账接线」（订单审核仅为状态翻转，无可观测下游）。按规则 4（一个计划一个结果面）与规则 14（同组件但「实质性不同的验证路径」例外），本计划聚焦入库单生命周期（消费 `generateMove` 的首个 purchase 调用方，验证路径跨 purchase→inventory→finance 三域集成）；订单审核（纯状态、无可独立验证的下游行为）移出范围为显式后续。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-pur-app` 已含 `quarkus-jdbc-h2`；服务层测试 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`）。
- **新增结构性依赖**（实时核实无环）：
  - `erp-pur-service/pom.xml` 新增 **compile** scope 依赖 `app-erp-inventory-dao`（`IErpInvStockMoveBiz` 接口 + `StockMoveRequest`/`StockMoveLineRequest` DTO 位于 inventory dao，package `app.erp.inv.biz`）。
  - `erp-pur-service/pom.xml` 新增 **compile** scope 依赖 `app-erp-master-data-dao`（`IErpMdPartnerBiz`/`ErpMdPartner` 用于供应商启用校验；master-data dao 为 DAG 叶子无环）。
  - `erp-pur-service/pom.xml` 新增 **test** scope 依赖 `app-erp-inventory-service`（`generateMove` Bean 实现，使测试可注入运行）+ `app-erp-master-data-service`（跨域主数据测试数据，复用 sales/inventory CRUD 冒烟 runbook 的 `createPrereqs()` 模式）。
- 无数据迁移/回滚脚本需求（greenfield BizModel，复用已存在实体）。

## Execution Plan

### Phase 1 - 采购入库审批状态机（三轴）+ 供应商启用校验

Status: completed
Targets: `module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurReceiveBiz.java`（增方法签名）、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurReceiveBizModel.java`（实现）、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/ErpPurErrors.java`（错误码）、`module-purchase/erp-pur-service/src/test/.../entity/`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（状态机不依赖库存调用）

- [x] `Decision`：状态迁移前置与目标态。裁决：按 `purchase/state-machine.md` §2——提交 `UNSUBMITTED(10)→SUBMITTED(20)`（前置 docStatus≠CANCELLED、行非空）；审核通过 `SUBMITTED→APPROVED(30)`（前置供应商启用 + 后续 Phase 2 库存触发）；驳回 `SUBMITTED→REJECTED(40)`；重提 `REJECTED→SUBMITTED`；撤销提交 `SUBMITTED→UNSUBMITTED`（仅当未触发下游，本计划无 nop-wf，简化为允许）；反审核 `APPROVED→REJECTED`（前置：已冲销所有入库移动单，见 Phase 2）；作废 `任意非终态→docStatus=CANCELLED(30)`（已 APPROVED 者须先冲销）。每条迁移校验前置，违反抛 `NopException(ERR_ILLEGAL_STATUS_TRANSITION)`。备选（被否）：单状态机——三轴分离避免组合爆炸（state-machine §三轴分离明确）。残留风险：撤销提交在无 nop-wf 时未防「审核人已介入」，需 nop-wf 接线后收紧（Follow-up）。
  - Skill: none
- [x] `Decision`：供应商启用校验时机与方式。裁决：`submit` 与 `approve` 均校验 `ErpMdPartner.status`（dict `erp-md/active-status`）为启用值——经注入的 `IErpMdPartnerBiz`（或 `daoProvider` 加载 `ErpMdPartner`）按 `supplierId` 查，停用抛 `NopException(ERR_PARTNER_INACTIVE)`（state-machine §4「供应商停用后开单拒绝」）。备选（被否）：仅 submit 校验——approve 距 submit 有时差，供应商可能在期间被停用。残留风险：依赖 master-data-dao compile 依赖（已纳入 Infra）。
  - Skill: none
- [x] `Add`：`IErpPurReceiveBiz` 增 `submit(receiveId)`/`approve(receiveId)`/`reject(receiveId)`/`cancel(receiveId)`/`reverseApprove(receiveId)`/`withdrawSubmit(receiveId)` 方法签名；`ErpPurReceiveBizModel` 实现迁移——每条校验前置 `approveStatus`/`docStatus`，落地 `approvedBy`(当前用户)/`approvedAt`(`CoreMetrics.currentDateTime()`)。`@SingleSession @Transactional`（平台 pointcut 自动应用），`IErpInvStockMoveBiz`/`IErpMdPartnerBiz` 经 `@Inject` 包级可见字段注入（Phase 2 接入库存调用；Phase 1 先注入占位不调用）。
  - Skill: none
- [x] `Proof`：服务层集成测试（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + master-data/inv test 依赖，`createPrereqs()` 自建供应商/物料/仓库/库位/单位/币种）——`testSubmitRejectResubmit`（提交→驳回→重提到 SUBMITTED）、`testIllegalTransitionRejected`（APPROVED→SUBMITTED 等非法迁移抛 `NopException`）、`testInactiveSupplierRejected`（供应商停用→submit 抛 `ERR_PARTNER_INACTIVE`）、`testCancelFromDraft`。`mvn test -pl module-purchase/erp-pur-service -am` 全绿。
  - Skill: none
- [x] `Proof`：审批状态机正确性复核——用 `docs/skills/state-machine-business-review-prompt.md` 针对终态/可达性/反审核目标态(REJECTED 非 UNSUBMITTED)/异常路径自检，结论记录于本阶段（非阻塞门控，须执行）。
  - Skill: state-machine-business-review-prompt

  > **复核结论（Verdict: pass，无 P0/P1）**：实现与 `state-machine.md` §2 全部 7 条迁移一致；终态 APPROVED（审核轴）/CANCELLED（单据轴）正确；反审核目标态为 REJECTED（非 UNSUBMITTED，对齐 §3/§11.4）；可达性无死状态/死锁，合法循环 UNSUBMITTED→SUBMITTED→REJECTED→SUBMITTED 退出条件为审核通过→APPROVED；非法迁移与供应商停用均抛 `NopException`（已测试）。剩余风险（非阻塞）：(a) 已 APPROVED 单据再次 `approve` 的幂等性（§4「重复审核=空操作」）由 Phase 2 在接入 `generateMove`（本身按 `(relatedBillType,relatedBillCode)` 幂等）时落地；(b) 角色职责分离未在代码强制（无 nop-wf，对齐基线，Follow-up）；(c) `withdrawSubmit` 在无 nop-wf 时未防审核人已介入（plan 已记残留风险）。

Exit Criteria:

> 本阶段交付三轴状态机 + 供应商校验。完整仓库 `mvn test` 归 Closure Gates。

- [x] 4 个状态机行为测试存在且 `mvn test -pl module-purchase/erp-pur-service -am` 全绿
- [x] 非法迁移拒绝 + 供应商停用拒绝均经测试证明（解除 Phase 2 库存触发的状态前置阻塞）

### Phase 2 - 入库审核触发库存移动（generateMove）+ posted 接线 + 收货状态回写

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurReceiveBizModel.java`（approve 调 generateMove）、`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/.../ReceiveStockMoveBuilder.java`（或等效内聚组件，构造 StockMoveRequest）、`module-purchase/erp-pur-service/src/test/.../`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 + **库存域 StockMove BizModel（0811-2）已 completed 可消费 `generateMove`**

- [x] `Decision`：过账归属与 `posted` 语义。裁决：采购入库的存货估值过账由库存域独占——`approve` 调 `generateMove(INCOMING)`，移动单业务联动自动 DONE 后库存域 `InvPostingDispatcher` 触发 `PURCHASE_INPUT`（`billHeadCode`=移动单 code）。purchase **不注册任何过账 Provider**（避免与 `InvAcctDocProvider` 的 `PURCHASE_INPUT` 非默认声明冲突致 `ERR_DUPLICATE_PROVIDER`）。`receive.posted` 语义 = 「其入库移动单的存货过账是否成功」：`receive.posted = move.posted`、`receive.postedAt`=`CoreMetrics.currentDateTime()`、`receive.postedBy`=当前用户。备选（被否）：purchase 自注册 `PURCHASE_INPUT` Provider——启动 fail-fast。残留风险：凭证回链绑定移动单 code 而非入库单 code，按入库单反查凭证须经移动单中转（可接受，移动单是库存记账权威源）。
  - Skill: none
- [x] `Decision`：`StockMoveRequest` 字段映射。裁决：`moveType`=INCOMING(10)；`relatedBillType`=`"ERP_PUR_RECEIVE"`（自由字符串，inventory 侧无字典约束）；`relatedBillCode`=`receive.code`（幂等键）；`destWarehouseId`=`receive.warehouseId`（入库目的仓），`sourceWarehouseId`=null（外部供应商无源仓）；`businessDate`=`receive.businessDate`；`orgId`/`currencyId` 取自入库单；`acctSchemaId` 因入库单实体无此列，改为按 `receive.orgId` 解析 `ErpMdAcctSchema`（存货估值凭证行 `acctSchemaId` 非空约束所需；未配置账套时为 null，过账由库存域吞异常置 `posted=false` 不阻塞入库）。行映射：每条 `ErpPurReceiveLine`→`StockMoveLineRequest`（`materialId`/`skuId`/`uoMId`/`quantity`=BigDecimal 直读 DECIMAL 列/`unitCost`=`new BigDecimal(line.unitPrice)` 解析 VARCHAR，空则 null/`batchNo`）。`StockMoveLineRequest.currencyId` 不传（库存域 `newLines` 回退到头 `currencyId`）。`destLocationId`=null 走整仓余额（`receiveLine.warehouseId` 无关系声明、语义未定，按整仓余额 locationId=null 为安全默认；库存余额按 `warehouseId×locationId×batchNo` 维度 upsert，locationId 为 null 合法）。备选（被否）：`sourceWarehouseId` 填虚拟在途仓——入库无在途语义，且 StockMove 入库类不要求源仓。残留风险：`unitPrice` VARCHAR 解析异常（空串/非法格式）须防护，解析失败抛 `NopException(ERR_INVALID_UNIT_PRICE)`。
  - Skill: none
- [x] `Add`：`approve(receiveId)`（幂等：已 APPROVED 空操作）：(1) `ReceiveStockMoveBuilder` 构造 `StockMoveRequest`；(2) 调 `IErpInvStockMoveBiz.generateMove(request)`（业务联动→DONE，幂等，重复审核返回既有移动单）；(3) `receive.posted = move.posted`、`postedAt`/`postedBy` 落地；(4) 重算 `receive.receiveStatus`(=RECEIVED) 与源 `ErpPurOrder.receiveStatus`（按累计已收 vs 订单数量：UNRECEIVED/PARTIAL/RECEIVED，BigDecimal 比较）。库存触发失败→整个 approve 事务回滚（`generateMove` 抛 `NopException` 上抛）。实现细节：跨域 `generateMove` 调用后重新加载 receive 并以 `updateEntity` 显式持久化（跨域会话扰动脏跟踪）；订单回写时当前单行始终计入（不依赖同事务查询可见性）。
  - Skill: none
- [x] `Decision`：反审核/作废的冲销机制（Design A：内部冲销 + 幂等双冲销保护）。裁决：`reverseApprove`/`cancel` 在内部完成冲销——(a) 按幂等键 `(relatedBillType=ERP_PUR_RECEIVE, relatedBillCode=receive.code)` 经注入的 `IErpInvStockMoveBiz` 反查既有入库移动单（**无 ORM FK，纯查询，不改 model**；移动单实体类经 inventory-dao compile 依赖对 purchase 可见）；(b) 按 `(relatedBillType=REVERSAL, relatedBillCode=原移动单.code)` 查是否已存在反向冲销移动单——已存在则跳过（幂等防双冲销），不存在则调 `IErpInvStockMoveBiz.reverse(originalMoveId)` 生成反向冲销移动单（库存域 DONE 时冲销流水/余额/红字凭证）；(c) 冲销确立后才 APPROVED→REJECTED（或 docStatus→CANCELLED）。`reverseApprove` 对已 REJECTED 单据幂等返回（无可冲销）。备选（被否，Design B「调用方先外部冲销、reverseApprove 仅校验前置」）：把原子反审核拆两步、调用方易遗漏冲销致状态与库存不一致。残留风险：库存域 `reverse()` 创建新 REVERSAL 移动单**不改原单状态**且**不传播 `acctSchemaId`**（移动单实体无该列），故冲销单的 SALES_OUTPUT 红字过账因凭证行 `acctSchemaId` 非空约束而失败、冲销单 `posted=false`——属库存域 `reverse()` 实现的已知边界，红字凭证生成需库存域 `reverse()` 传播 `acctSchemaId`（Follow-up）；「是否已冲销」须经 REVERSAL 移动单二跳查询判定（已纳入裁决，不依赖任何 receive 侧字段）。
  - Skill: none
- [x] `Add`：`reverseApprove(receiveId)`（反审核，APPROVED→REJECTED，已 REJECTED 幂等）与 `cancel(receiveId)`（任意非终态→docStatus=CANCELLED）——按上述 Design A 实现：反查既有入库移动单；已存在 REVERSAL 移动单则跳过冲销，否则调 `IErpInvStockMoveBiz.reverse(moveId)`；冲销确立后迁移状态。原移动单缺失（APPROVED 却查不到，数据不一致）抛 `NopException(ERR_MOVE_NOT_FOUND)`。
  - Skill: none
- [x] `Proof`：`testApproveGeneratesIncomingMoveAndPosting`（审核→入库移动单 DONE、库存余额增加、`ErpFinVoucher`+`ErpFinVoucherLine`+`ErpFinVoucherBillR` 落地、`receive.posted=true`、`receive.approveStatus`=APPROVED；posting 测试主数据 setup——`ErpMdAcctSchema`(按 org) + 币种 + `ErpMdSubject`(1401/2202/6401) + 开放会计期间，复用 `2026-07-01-0811-2` 存货过账测试 `createPrereqs()` 模式）、`testApproveIdempotent`（二次审核同入库单返回既有移动单，无第二张）、`testReceiveStatusRollupToOrder`（订单 `receiveStatus` 按 PARTIAL/RECEIVED 正确）、`testReverseApproveInternallyReversesMove`（反审核→内部生成反向冲销移动单 + 余额归零 + APPROVED→REJECTED；二次反审核幂等不产生第二张冲销单；红字凭证按库存域 `reverse()` 是否传播 `acctSchemaId` 决定，purchase 侧断言 posted↔凭证一致）。`mvn test -pl module-purchase/erp-pur-service -am` 全绿（13 测试：4 审批状态机 + 4 入库触发 + 5 CRUD 冒烟）。
  - Skill: none

Exit Criteria:

> 本阶段交付入库触发 + 过账接线 + 状态回写 + 反向冲销前置。完整仓库 `mvn test` 归 Closure Gates。

- [x] 4 个入库触发行为测试存在且 `mvn test -pl module-purchase/erp-pur-service -am` 全绿
- [x] 审核生成移动单+凭证+`posted=true`、幂等重审、收货状态回写、反审核内部冲销+幂等 均经测试证明

### Phase 3 - 端到端串联 + 收尾

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/.../`、`docs/logs/2026/07-01.md`、`docs/backlog/core-business-roadmap.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Proof`：端到端测试 `testOrderToReceiveToEnd`（建订单+入库单 UNSUBMITTED→提交→审核→入库移动单 DONE+库存余额增加+存货凭证(借存货50/贷暂估50)+`posted=true`+订单/入库单 `receiveStatus` 回写→反审核内部冲销→余额归零+APPROVED→REJECTED）。证明 purchase→inventory→finance 三域经 `generateMove` 端到端打通。`mvn test -pl module-purchase/erp-pur-service -am` 全绿。冲销红字凭证净额=0 受库存域 `reverse()` 不传播 `acctSchemaId` 限制（见 Phase 2 残留风险/Follow-up），purchase 侧断言余额归零而非凭证净额。
  - Skill: none
- [x] `Add`：更新当日开发日志 `docs/logs/2026/07-01.md`（按 `docs/logs/00-log-writing-guide.md`，时间倒序），记录采购入库审批+入库触发落地 + 验证状态。
  - Skill: none
- [x] `Add`：`docs/backlog/core-business-roadmap.md` 工作项 1.1 标注进展（入库触发段 `done`；订单审核/发票/付款/三单匹配仍 `todo`）。
  - Skill: none

Exit Criteria:

> 本阶段交付端到端打通 + 文档对齐。完整仓库 `mvn test` 归 Closure Gates。

- [x] 端到端测试存在且 `mvn test -pl module-purchase/erp-pur-service -am` 全绿
- [x] 当日日志已记；roadmap 工作项 1.1 进展已标注

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e43d1407ffeh4p2pwmVzKWmd1，独立 general 子代理，新会话）— 核心技术主张（过账归属冲突 `InvAcctDocProvider` 非默认独占 PURCHASE_INPUT、`generateMove` 契约、入库类不校验可用量、VARCHAR 金额/单价陷阱、dict 值、模块 DAG 无环、partner status 字段、空壳基线、反松弛/规则合规）全部经实时仓库核实属实；范围缩小（订单审核移出）有充分理由、非规则 10 静默降级。2 项阻塞：(B1) Current Baseline 写 `ErpPurReceiveBizModel extends CrudBizModel<ErpInvStockMove>`，实时仓库实际泛型为 `<ErpPurReceive>`（拷贝错误）；(B2) Phase 2 反审核/作废 Decision 自相矛盾——同时声称「前置调 `reverse()` 内部冲销」（Design A）与「未先冲销即拒绝 `ERR_REVERSE_PREREQUISITE_NOT_MET`」（Design B），且 Proof `testReverseApproveReversesMoveFirst`「未冲销→拒绝」与 Design A 不一致；又 receive 无移动单 FK 且禁改 ORM，须纯查询定位，而库存域 `reverse()` 创建新 REVERSAL 移动单不改原单状态，故「是否已冲销」须二跳查询。迭代 1 已修订：B1→泛型改为 `<ErpPurReceive>`；B2→新增 Design A 反审核 Decision（内部冲销 + 按 `(relatedBillType,relatedBillCode)` 纯查询定位原单 + 按 `(REVERSAL, 原单.code)` 幂等防双冲销；库存 `reverse()` 不改原单状态的残留风险已记），Add/Proof 统一为「内部冲销 + 幂等」，移除 Design B 的「拒绝」措辞与 `ERR_REVERSE_PREREQUISITE_NOT_MET`，新增 `ERR_MOVE_NOT_FOUND` 兜底。建议（非阻塞）一并吸收：S1 dict 拼写 `RECECEIVED`→`RECEIVED`；S2 Task Route 补规则 14 验证路径例外；S4 行映射补 `currencyId` 回退 + `receiveLine.warehouseId`(displayName 库位) 语义未定时整仓余额默认；S5 posting 测试主数据 setup 引用 0811-2 `createPrereqs()`。
- Independent draft review iteration 2: **accept / consensus**（ses_0e42683ffffeC4KSuBhY1bLgo6，独立 general 子代理，新会话）— B1 已解决（Current Baseline 泛型改为 `<ErpPurReceive>`，与实时 `ErpPurReceiveBizModel.java:11` 一致）；B2 已解决（Phase 2 仅保留 Design A「内部冲销 + 幂等双冲销保护」，Design B 记为被否备选；Design-B 的「拒绝」/`ERR_REVERSE_PREREQUISITE_NOT_MET` 措辞全清；检测机制为纯查询两跳 `(ERP_PUR_RECEIVE,code)→原单` + `(REVERSAL,原单.code)→既有冲销`，无 ORM FK/不改 model；与实时 `ErpInvStockMoveBizModel.reverse()`(L130-168) 语义一致——创建新 REVERSAL 移动单不改原单状态，故二跳查询判定合理；Decision/Add/Proof 三处一致）。无回归：`ERR_MOVE_NOT_FOUND` 兜底合理、S4 字段映射 `currencyId` 回退经实时 `newLines()` 核实、S5 posting 主数据 setup 在位、S1 dict 拼写、S2 规则 14 例外、反松弛/类型标注/Decision 替代方案+残留风险 全净。**共识达成**：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：入库审批三轴状态机 + 入库触发 `generateMove` + `posted` 接线 + 收货状态回写 + 反向冲销前置 全部落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 工作项 1.1 标注进展；当日日志已记
- [x] 已运行验证：`mvn test -pl module-purchase/erp-pur-service -am` 全绿（14 测试）；根 `mvn test -fae` = BUILD SUCCESS（无回归，146 reactor 模块）
- [x] 无范围内项目降级为 deferred/follow-up（订单审核/发票/付款/三单匹配/退货/请购转化 均为计划内 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 采购订单（ErpPurOrder）审核状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 订单审核仅状态推进（无库存/凭证下游），是可分离的轻量关注点；本计划聚焦解除 `generateMove` Follow-up 阻塞的入库单面。
- Successor Required: yes（触发条件：实施订单→入库前置校验或请购→订单转化 1.0a 时，一并落地订单审核状态机）

### nop-wf 审批流编排

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划审核 = 直接状态迁移 + `@BizMutation`（对齐 StockMove 同步状态机基线）；nop-wf（提交流/挂起/审批人路由/撤销提交收紧）属运营基础设施。
- Successor Required: yes（触发条件：需要多级审批人路由与流程挂起时接线 nop-wf）

### 采购发票/付款 Provider（AP_INVOICE/PAYMENT）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 1.4（三单匹配）与 1.6（采购到付款端到端）；本计划入库段不涉及应付/付款凭证。
- Successor Required: yes（触发条件：起草采购发票/付款 BizModel 计划时）

## Closure

Status Note: 计划可关闭——采购入库审批三轴状态机 + 入库审核触发 `generateMove`(INCOMING) + `posted` 接线 + 收货状态回写 + 反向冲销前置全部落地，行为测试全绿（模块 14 测试），根 `mvn test -fae` BUILD SUCCESS 无回归（146 reactor 模块），当日日志已记，roadmap 工作项 1.1 进展已标注。首个 purchase→inventory→finance 跨域调用方打通。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 ses_0e3ee9488ffet8fltRCiDXYnU6（general，新会话，非执行者），2026-07-01。
- Verdict: **pass**（无 P0/P1 阻塞）。
- 核实项：
  - 三阶段 Status 均为 `completed`，全部工作项与退出标准均为 `[x]`。
  - 代码落位与计划一致：`IErpPurReceiveBiz` 6 方法签名（`@SingleSession @Transactional`）、`ErpPurReceiveBizModel` 状态机+`triggerIncomingMove`→`generateMove`+`applyPostingResult`(posted 接线)+`rollupOrderReceiveStatus`+`ensureReversed`(Design A 内部冲销 + REVERSAL 幂等防双冲销)、`ReceiveStockMoveBuilder`(INCOMING/`ERP_PUR_RECEIVE` 幂等键 + VARCHAR 单价解析 + 按 orgId 解析 acctSchemaId)、`ErpPurConstants`/`ErpPurErrors` 错误码齐备。
  - 保护区域：未改任何 `model/*.orm.xml`/`*.api.xml`（git diff 核实）；purchase 未注册过账 Provider（`PURCHASE_INPUT` 由库存域 `InvAcctDocProvider` 独占，grep 核实无 `PurAcctDocProvider`）。
  - 测试：3 个测试文件（审批状态机 4 + 入库触发 4 + 端到端 1），复现 `mvn test -pl module-purchase/erp-pur-service -am` = Tests run: 14, Failures: 0, Errors: 0，BUILD SUCCESS。
  - 文档：`docs/logs/2026/07-01.md` 新增 1132-1 条目；`docs/backlog/core-business-roadmap.md` 工作项 1.1 由 `—` 更新为 `🔶 partial`。
  - 残留风险（库存域 `reverse()` 不传播 `acctSchemaId` 致冲销红字凭证不生成）在 4 处明确标注为库存域 Follow-up（计划 Phase 2 Decision / 日志 / 测试注释 / Follow-up 列表），非隐藏缺口；purchase 侧契约只保证冲销移动单生成 + 余额冲回（测试已断言）。
- P2（非阻塞）：执行者最初保留 8 个 Closure Gates 为 `[ ]` 系对「执行者不得自我审计」规则的保守解读，由本独立审计判定全部 satisfied。

Follow-up:

- 采购订单审核状态机（见上方 Deferred）
- nop-wf 审批流编排（见上方 Deferred）
- 采购发票/付款 Provider（AP_INVOICE/PAYMENT）（见上方 Deferred）
- 订单行级超收容差校验（触发条件：实施三单匹配 1.4 时）
- 库存域 `reverse()` 传播 `acctSchemaId`（触发条件：冲销红字凭证净额=0 需可观测时；属库存域 `2026-07-01-0811-2` 后继）
