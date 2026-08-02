# MA4 pur+sal+inv view.xml vs 后端契约 drift 审计（A4.7）

> Audit Status: closed
> 里程碑: MA4（代码与前端质量层）
> 工作项: A4.7（pur+sal+inv view.xml vs 后端契约 drift，A 级 view.xml drift 第二批）
> 范围文档: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行 + §残留风险 5「未覆盖：AMIS view.xml drift」
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（7 维度适配「view.xml vs 后端契约 drift」主题，经 A4.6 验证有效）
> 来源计划: `docs/plans/2026-07-29-0749-1-audit-remediation-ma4-pur-sal-inv-view-xml-drift.md`
> 后端契约真相源基线: A4.5（pur+sal+inv+qa+crm）+ MA2（三域状态机）已 done——后端 BizModel/xbiz 契约稳定
> 同型前批: A4.6（finance+mfg S 级第一批，134 view.xml，已 done）

## 1. 审计对象与基线

- **审计对象**：purchase 40 view.xml + sales 32 view.xml + inventory 42 view.xml = **合计 114 view.xml**（`module-{purchase,sales,inventory}/erp-*-web/src/main/resources/_vfs/erp/{pur,sal,inv}/pages/`）。其中 delta（非 `_gen`）定制层 purchase 20 + sales 16 + inventory 21 = **57 套**，`_gen` 生成层各对应一套。delta 层是手写 drift 的发源地；`_gen` 层由 XMeta 驱动生成，理论自洽，本审计以 delta 层为主、`_gen` 层为辅交叉对照（与 A4.6 同型方法）。
- **后端真相源**：`module-{purchase,sales,inventory}/erp-*-service/`（BizModel Java + `*.xbiz`/`_*.xbiz` + Processor + `Erp{Pur,Sal,Inv}Constants.java`）+ `*-meta/`（XMeta + dict yaml）+ `module-{purchase,sales,inventory}/model/app-erp-*.orm.xml`（字段 + `ext:dict` 绑定）+ 共享 `module-common-service/_vfs/dict/erp/doc-status.dict.yaml`（DRAFT/**ACTIVE**/CANCELLED）+ 平台 `wf/approve-status.dict.yaml`（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。
- **drift 维度**（`multi-dimensional-audit-prompt.md` 7 维度适配本主题，经 A4.6 验证有效）：(1) 字段名一致性 / (2) BizMutation 动作名一致性 / (3) 枚举值状态值一致性 / (4) 参数类型一致性 / (5) dict 绑定一致性 / (6) gen-control 内联脚本契约 / (7) 跨实体字段引用。

## 2. 7 维度逐项审查结果

### 维度 1 — 字段名一致性

**裁决：本维度无 drift（三域 delta 层 col/cell id 全量核验命中 ORM 实体字段或 to-one/to-many 关联；`bounded-merge` 静默丢弃机制兜底）。**

- 三域 delta grid `<cols>` / form `<cells>` 普遍声明 `x:override="bounded-merge"`。Nop `bounded-merge` 语义：仅合并基础层（XMeta）已声明的 prop，未知 `id` 在运行时被静默丢弃而非报错——字段名拼写漂移被平台机制自愈（dropped，最坏退化为列缺失可见性，不致页面报错/空白）。
- **purchase**：逐实体核验 delta col/cell id 全部命中 `app-erp-purchase.orm.xml`——ErpPurOrder（`settlementMethodId/paidAmount/discountRate/receiveStatus/paidStatus/quotationId`）、ErpPurOrderLine（`amountWithTax/receivedQuantity/invoicedQuantity/taxRateId/skuId/projectId`）、ErpPurReceive（`receiveType/orderId`）、ErpPurReceiveLine（`rejectedQuantity/batchNo/orderLineId`）、ErpPurInvoice（`invoiceNo/invoiceType/paidAmount/paidStatus`）、ErpPurPayment（`paymentMethod/bankAccountId/partnerBankAccountId/writtenOffStatus/settlementMethodId`）、ErpPurQuotation（`isAccepted/validFrom/validTo/rfqId`）、ErpPurRfq（`validUntil`）、ErpPurSupplierPriceList（`priority/minOrderQuantity/isActive`）等零悬挂字段。
- **sales**：逐实体核验 delta col/cell id 全部命中 `app-erp-sales.orm.xml`——ErpSalOrder（`contractId/quotationId/settlementMethodId/deliveryDate/deliveryStatus/receivedStatus/posted` 等）、ErpSalOrderLine（`deliveredQuantity/invoicedQuantity/pricingSource/taxRateId/skuId/amountWithTax`）、ErpSalDeliveryLine（`batchNo/orderLineId`）、ErpSalReceipt（`bankAccountId/partnerBankAccountId/receiptMethod/writtenOffStatus`）、ErpSalContract（`contractName/signedBy`）、ErpSalQuotation（`isAccepted`）。PricingRule/PriceList/Contract delta view 为空壳（`<crud name="main"/>`），无自定义 col/cell，无可 drift 字段。
- **inventory**：逐实体核验 delta col/cell id 全部命中 `app-erp-inventory.orm.xml`——StockMove（`id/code/moveType/orgId/sourceWarehouseId/destWarehouseId/businessDate/docStatus/approveStatus`）、StockMoveLine（`lineNo/materialId/quantity/unitCost/totalCost/batchNo/serialNo`）、StockTake、PickingOrder（`pickerId/relatedBillType/relatedBillCode`）、Batch/SerialNumber/StockBalance/StockLedger/CostAdjust(Lane)/LandedCost(Lane)/OwnershipTransfer(Lane)/TransferOrder(Lane) 全部命中。ORM 跨实体 `uoMId`/`uomId` 大小写不一致属 ORM 维护性问题，**各 view 与各自 ORM 实体大小写一致**，无 view 层 drift。

### 维度 2 — BizMutation/BizQuery 动作名一致性

**裁决：本维度无 drift（三域 delta 层全部自定义动作引用的**动作名**解析到 BizModel @BizMutation/@BizQuery 或 `*.xbiz` `<mutation>`/`<source>` 委托 Processor；唯一缺陷是 purchase Rfq cancel 的**参数名**，归维度 4）。**

逐一核验三域 delta 层全部自定义动作引用（CRUD 标准 `save/update/delete/batchDelete/findPage/get` 由 `_*.xbiz` `DefaultBizGenExtends` 自动派生，不逐项列出）：

**purchase 自定义动作**（动作名全部解析通过）：
- 5-action 审批集（submitForApproval/approve/reject/reverseApprove/withdrawApproval）：Order/Receive/Invoice/Payment/Return/Requisition 经 Processor 接线（`ErpPurOrder.xbiz:5-67` 委托 `*Processor`，余域同型 Processor 文件全在）；Quotation/Rfq 经 inline xbiz（`ErpPurRfq.xbiz:5-125`、`ErpPurQuotation.xbiz:5-125`）。
- cancel（8 域）：均解析到 BizModel Java `cancel(@Name(...))`（`rg` 确认零 xbiz 声明 cancel → 全部走 BizModel）。
- batchApprove：`ErpPurOrder__batchApprove?ids=$ids` → `ErpPurOrderBizModel.java:63` `batchApprove(@Name("ids") Collection<String>)`。

**sales 自定义动作**（动作名全部解析通过）：
- 6 交易实体 ×（审批五动作 + cancel）+ Order 专属 `batchApprove` = 37 个 `@mutation:` 目标全部解析。7 实体 delta xbiz **均声明** 5 个 `<mutation>`（Order/Delivery/Invoice/Receipt/Quotation/Return 经 `<source>` 委托 Processor；Contract 走 INLINE 脚本）。
- cancel：view `?<entityId>=$id` 与 BizModel `@Name` 匹配（orderId/deliveryId/invoiceId/receiptId/quotationId/returnId）。
- **观察（非 drift）**：Contract delta + `_gen` view **均不暴露审批按钮**，而 xbiz 声明完整 5 动作——属「后端声明、view 未暴露」的反向不对称（与 MA2 §2.1「Contract 是 CrudBizModel 桩」一致），非悬挂动作引用。

**inventory 自定义动作**（动作名全部解析通过，重点复核 MA2-062/063）：
- `ErpInvStockMove__confirm/complete/cancel?moveId=$id` → `ErpInvStockMoveBizModel:44/50/56` ✓
- `ErpInvStockTake__startTake/completeTake/cancelTake?takeId=$id` → `ErpInvStockTakeBizModel:26/40/54` ✓ ——**MA2-062 复核**：completeTake 动作名解析通过，"未自动生成盘盈/盘亏移动单"属后端逻辑缺口（A4.5/MA2 范畴），非 view 层 drift。
- `ErpInvTransferOrder__confirm?transferOrderId=$id` → `ErpInvTransferOrderBizModel:35` ✓
- `ErpInvCostAdjust__` 审批五动作 → `ErpInvCostAdjust.xbiz` `<source>` 注入 5 Processor ✓
- `ErpInvLandedCost__approve` + `@query:allocate` → `ErpInvLandedCostBizModel:40/46` ✓
- **MA2-063 复核（PickingOrder）**：`ErpInvPickingOrderBizModel` 为纯 CRUD stub（无自定义方法）。PickingOrder delta view **零自定义动作按钮**（无 pick/complete/startPicking 调用），`_gen` 层亦无。**故不存在指向不存在动作的悬挂引用——无 view 层 action drift**。

- **A4.5/MA3 已登记 P1 复核**：本维度与 P1-MA3-048（孤儿 Processor bean 携带 String 影子契约 dim3）交叉——本审计确认三域全部 Processor bean 经 xbiz `<source>` 正式接线或 BizModel 委托，无孤儿悬挂动作引用。

### 维度 3 — 枚举值/状态值一致性

**裁决：本维度无 drift（三域 delta 层全部 `visibleOn`/`disabledOn` 状态字面量命中有效 dict 值；MA2 三域死状态 finding 在 view 层无误导性投影）。**

逐实体核验三域 delta view 的 `visibleOn`/`disabledOn`/gen-control badge 状态字面量 vs dict yaml + Constants（完整映射表见 §状态值映射表）：

**purchase**：全域 docStatus 绑定共享 `erp/doc-status`（DRAFT/**ACTIVE**/CANCELLED，含 ACTIVE）+ approveStatus 绑定 `wf/approve-status`（四态全中）。故 badge `== 'ACTIVE'` / `== 'CANCELLED'` 字面量在 pur **dict 合法**（ACTIVE 存在）——区别于 finance/mfg（域专属 status dict 无 ACTIVE 致 P2-MA4-014）。paidStatus（UNPAID/PARTIAL/PAID）+ receiveStatus（UNRECEIVED/PARTIAL/RECEIVED）+ scorecard-status 全命中 dict ✓。

**sales**：全域 docStatus 绑定共享 `erp/doc-status`（含 ACTIVE）+ approveStatus 绑定 `wf/approve-status`（四态全中）+ deliveryStatus/receivedStatus/price-list-status 全命中 dict。全部 visibleOn 状态字面量（UNSUBMITTED/REJECTED/SUBMITTED/APPROVED/CANCELLED）与 dict + Processor 迁移守卫**双向一致**。**无 WorkOrder-`STARTED` 类死枚举**（A4.6 P1-MA4-023 同型在 sales 不存在）。

**inventory（MA2 死状态最高风险域）**：StockMove `visibleOn` docStatus `DRAFT`/`CONFIRMED` + moveType `INCOMING`/`OUTGOING` 全命中 move-status/operation-type dict；StockTake `visibleOn` docStatus `DRAFT`/`CONFIRMED` 命中 move-status；CostAdjust `visibleOn` approveStatus 四态全中 wf/approve-status；TransferOrder `visibleOn` docStatus `DRAFT` 命中 move-status。**关键事实澄清**：plan 提及的 `stock-take-status` dict 不存在为文件——StockTake.docStatus 经 ORM:708 `ext:dict` 实际绑定 `erp-inv/move-status`（DRAFT/CONFIRMED/DONE/CANCELLED），`take-type` dict 仅服务 `takeType` 字段（FULL/SAMPLE/CYCLE）非状态轴。无缺失 dict。

**MA2 三域状态机 dict 死状态 view 层投影复核**（plan 要求）：
- **P1-MA2-049**（Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2）：Rfq view 暴露 reverseApprove 按钮（`ErpPurRfq.view.xml:83`，confirmText 通用「确认反审批此询价单？」未声称 REJECTED）；Quotation view **无** reverseApprove 按钮。错误目标态属后端 xbiz（`ErpPurRfq.xbiz:97`），view 无误导性文本。**无 view 层 drift**。
- **P1-MA2-050**（INLINE reject/withdrawApproval 缺 isCancelled guard）：view reject/withdrawApproval visibleOn 仅基于 approveStatus（无 docStatus guard），与后端 guard 缺失**一致**（两层均不守卫）→ 非 view-后端分歧，drift 属后端。**无 view 层 drift**。
- **P1-MA2-051**（rollbackReceive 不对称 → receive APPROVED+posted=false 悬挂）：Receive view 准确显示 APPROVED+posted=false（`ErpPurReceive.view.xml:84-87`），无按钮引用死状态，关联凭证 drawer（posted=false 时空）行为正确。**无 view 层投影**。
- **P1-MA2-056**（Contract reverseApprove→SUBMITTED INLINE 契约漂移）：Contract delta + `_gen` view **均不暴露审批/反审批按钮**，误导性 SUBMITTED 目标态不经 view 暴露。**无 view 层投影**。
- **P1-MA2-057**（INLINE withdrawApproval 缺 isCancelled 守卫）：sales 6 交易实体 withdrawApproval visibleOn `${approveStatus=='SUBMITTED'}` 与后端 INLINE 守卫**同等缺少** isCancelled 守卫，属 MA2-057 后端范畴。Contract 未在 view 暴露。**无 view 层 drift**。
- **P1-MA2-062**（StockTake completeTake 未生成移动单）：`completeTake` 动作名解析通过，`visibleOn docStatus=='CONFIRMED'` 与 BizModel 状态判断一致。后端逻辑缺口不经 view 暴露为 drift。**无 view 层投影**。
- **P1-MA2-063**（PickingOrder PICKING/PICKED dict 死状态）：`picking-status.dict.yaml` 确含 PICKING/PICKED（PENDING/PICKING/PICKED/CANCELLED），但 PickingOrder delta view（62 行）**零 `visibleOn`/`disabledOn`/gen-control badge/状态守卫引用 docStatus 状态值**（`docStatus` 仅作为查询 filter cell + form 显示字段）。即 view **未将死状态映射为可见按钮/误导性守卫**。查询过滤下拉会列出 dict 全部 4 值（含 PICKING/PICKED），但这是 view 忠实绑定 dict 的正确行为——死状态根因在后端 CRUD stub 无 writer（`ErpInvPickingOrderBizModel:11-14`），**非 view 层 drift**（与 A4.6 对 P1-MA2-035/036 的裁决一致：view 与 dict 一致，死状态属后端）。

### 维度 4 — 参数类型一致性

**裁决：本维度发现 1 项 P1 drift（purchase ErpPurRfq cancel 参数名 `id` vs BizModel `rfqId`）。**

- **P1-MA4-024**：`ErpPurRfq.view.xml:92` `@mutation:ErpPurRfq__cancel?id=$id` 传参数名 `id`，而 `ErpPurRfqBizModel.java:22` 签名为 `cancel(@Name("rfqId") Long rfqId, IServiceContext context)`。零 xbiz 声明 cancel 做 `id`→`rfqId` adapt（`rg` 确认）→ `rfqId` 收到 null → `requireEntity(String.valueOf(null))` → 实体未找到报错。**作废按钮功能性失效。** 其余 7 域 cancel 参数名与 `@Name` 全匹配（orderId/receiveId/invoiceId/paymentId/returnId/requisitionId/quotationId），唯 Rfq 用裸 `id` 漂移。此为 P1-MA3-047（API 命名/参数跨域不一致 dim7）的 view 层具象。
- 审批五动作（三域）：xbiz 声明 `<arg name="id" type="String" mandatory="true"/>`，view 传 `?id=$id`，Processor `requireEntity({id})` 接受 String→Long 转换。Nop 标准模式，跨域一致。与 P2-MA3-037（cancel Long↔String adapt 跨域不一致属后端 xbiz 层）无 view 层投影 ✓。
- sales cancel `?<entityId>=$id`（view 侧 String）→ BizModel `@Name("<entityId>") Long`，Nop 标准 adapt，跨 6 实体一致 ✓。
- inventory `?moveId/$takeId/$transferOrderId/$id` → BizModel `@Name` Long 全匹配 ✓。
- **日期参数**：frontend-ui-roadmap 已修复 12 日期参数报表下载 + input-date valueFormat。本审计复核 pur/sal/inv 业务页面 `businessDate filterOp="date-between"` 全域统一，AMIS 标准序列化，无残留裸字符串日期漂移 ✓。

### 维度 5 — dict 绑定一致性

**裁决：本维度无 drift（三域 ORM `ext:dict` 引用的 dict yaml 全部存在）。**

- **purchase**：全量核验 ORM `ext:dict` 绑定 `erp/doc-status` + `wf/approve-status` + `erp-pur/{paid-status,receive-status,receive-type,invoice-type,supplier-standing,scorecard-status,biz-type,doc-direction}` → 对应 `_vfs/dict/{erp-pur,erp,wf}/*.dict.yaml` **全部存在** ✓。delta view 未硬编码 `dict=` 路径（状态列用 gen-control badge + ORM ext:dict 经 XMeta 传播）。
- **sales**：全量核验 sales ext:dict 绑定（ORM 22 处）→ `erp/doc-status` + `wf/approve-status` + `erp-sal/{received-status,delivery-status,invoice-type,pricing-rule-type,pricing-target}` **全部存在** ✓。`erp-sal/price-list-status`（ACTIVE/INACTIVE）+ `erp-sal/biz-type` 存在但未绑定到任何 column（PriceList 用 BOOLEAN `isActive` 非 status 字符串）——属后端建模选择（dict 孤立），view 不引用，**非 view drift**。
- **inventory**：全量核验 inv ext:dict 绑定 `erp-inv/{move-status,operation-type,picking-status,batch-status,serial-status,reservation-status,ownership-transfer-status,ownership-transfer-type,ownership-type,adjust-type,cost-element,landed-cost-alloc-method,take-type}` + `erp-md/cost-method` + `wf/approve-status` + `erp/doc-status` → 对应 dict yaml **全部存在** ✓。**StockTake 状态 dict 澄清**：StockTake.docStatus 经 ORM:708 `ext:dict="erp-inv/move-status"` 绑定（**非缺失**）；`take-type` 服务 `takeType` 字段非状态轴。
- **P1-MA2-046**（hr 排班分配 status 无 dict 绑定 raw VARCHAR）同型复核：pur/sal/inv 全部 status/docStatus 字段在 ORM 均声明 `ext:dict`，**pur/sal/inv 无同型 dict 绑定缺失**。

### 维度 6 — gen-control 内联脚本契约

**裁决：本维度发现 3 项 P2（purchase 跨域调色板漂移 + inventory `ACTIVE` 死状态 badge + inventory 内联脚本属性拼写错误）；docStatus `ACTIVE` badge 在 pur/sal **正确**（dict 含 ACTIVE）。**

- **P2-MA4-016 purchase 跨域通用状态调色板漂移**：`ErpPurOrder`（paidStatus:49/receiveStatus:64 badge）、`ErpPurReceive`（receiveStatus:16 badge）、`ErpPurInvoice`（paidStatus:17 badge）的 successVals/primaryVals 硬编码跨域值数组（`RECEIVED/PAID/DELIVERED/COMPLETED/SETTLED` + `PARTIAL/IN_PROGRESS`）。pur 有效值 PAID/RECEIVED→success、PARTIAL→primary 命中正确；但 DELIVERED/COMPLETED/SETTLED/IN_PROGRESS 非 pur dict 值（销售/物流域残留）→ 死分支永不命中。功能正确（UNPAID/UNRECEIVED 正确落 default），纯可维护性隐患（dict 演进时调色板失同步）。同 A4.6 P2-MA4-015 类。
- **P2-MA4-017 inventory 系统性 `ACTIVE` 死状态 badge 映射**：`ErpInvStockMove.view.xml:23` + `ErpInvLandedCost.view.xml:20`（docStatus gen-control badge）采用模板 `${valueProp == 'ACTIVE' ? 'primary' : 'default'}`，但 **StockMove/LandedCost 的 docStatus 绑定 `erp-inv/move-status`（DRAFT/CONFIRMED/DONE/CANCELLED），无 `ACTIVE` 值**（`move-status.dict.yaml` 仅 4 值）→ `== 'ACTIVE'` 永不命中 → 状态颜色恒渲染 `default`（灰）而非 `primary`（蓝）。CANCELLED line-through 仍有效（CANCELLED ∈ move-status）。影响：纯视觉（label 经 dict `graphql:labelProp` 正确显示，仅颜色类错误）。此为 A4.6 P2-MA4-014 同型系统性复制粘贴缺陷在 inventory 域的投影（从 `erp/doc-status` 模板复制粘贴未适配 move-status 词汇表）。注：两 view 的 approveStatus badge（`ErpInvStockMove.view.xml:39-41` / `ErpInvLandedCost.view.xml:36-38`）引用 APPROVED/REJECTED/SUBMITTED **正确命中** wf/approve-status，无 drift。
- **P2-MA4-018 inventory 内联脚本无效属性拼写**：`ErpInvStockMoveLine.view.xml:101`（totalCost sub-grid-edit gen-control）`return { type: 'number', kilometer: true, precision: 2 }`——`kilometer` 非 AMIS 合法属性（疑为它处复制残留），运行期被静默忽略。影响：零功能影响（字段仍按 `type:'number'`+`precision:2` 渲染）；纯可维护性瑕疵。
- **docStatus `ACTIVE` badge 在 pur/sal 正确（非 drift）**：与 finance/mfg（P2-MA4-014）不同，pur/sal 的 docStatus 绑定共享 `erp/doc-status`（**含 ACTIVE**），故 11+ 处 docStatus badge 模板 `${x == 'ACTIVE' ? 'primary' : 'default'}` 在本域**正确渲染**（ACTIVE→蓝）→ **pur/sal 无 P2-MA4-014 同型问题**。说明 badge 模板是跨域通用模板，其正确性取决于各域 docStatus dict 是否含 ACTIVE。
- **前端 UI-roadmap Phase 3 残留复核**：三域 delta view.xml **零** `ErpMdPartner__` 非法 GraphQL 引用（picker 均用合法 `/erp/md/pages/ErpMdMaterial/picker.page.yaml`）；全部 `<data>` 出现均为合法 AMIS drawer-data XML（`<billCode>${code}</billCode>` 等，非裸 JS `data` 变量）；inventory `payload.data` 安全解构仅出现在 `dashboard/*.page.yaml`（非本审计 view.xml 对象，且为 `d.data ? d.data : d` 安全 adaptor）；inventory.write input-table tabs（StockMove/CostAdjust/LandedCost/TransferOrder 的 `layoutControl="tabs"` + `<cell id="lines">` sub-grid）均已实现完整。**三域 view.xml 无 Phase 3 残留**。

### 维度 7 — 跨实体字段引用

**裁决：本维度无 drift（三域跨实体字段路径全部命中关联实体字段、经 picker 快照注入合法、子表 view/drawer page 路径全部存在）。**

- **purchase**：delta view 引用的关联字段均命中 ORM `refEntityName` 关联（OrderLine material/sku/warehouse/project/taxRateMd/order、ReceiveLine orderLine、InvoiceLine receiveLine、ReturnLine receiveLine、PaymentLine invoice、Quotation rfq、Rfq requisition、Receive order、Return receive、RequisitionLine suggestedSupplier→ErpMdPartner/project）。「从订单导入」/「从入库导入」picker `map:` 模板映射字段全部为源行实体字段，picker `valueField:id`/`labelField:name` 命中 `ErpMdMaterial` ORM ✓。
- **sales**：跨域只读 picker 源 `/erp/md/pages/ErpMdMaterial/picker.page.yaml`（OrderLine/DeliveryLine/InvoiceLine/ReturnLine ×4，**文件存在**）+ 跨域只读 drawer `/erp/fin/pages/ErpFinVoucherBillR/voucher-by-bill.page.yaml`（Order/Delivery/Invoice ×3，**存在**）+ `/erp/sal/pages/ErpSalDelivery/ref-order.page.yaml`（**存在**）+ 域内 picker（ErpSalOrderLine/ErpSalDeliveryLine picker.page.yaml **均存在**）✓。to-one FK 字段全为实体现存列，无 relation-path 悬挂 ✓。
- **inventory**：delta view 引用的子表 view 路径（`/erp/inv/pages/ErpInvStockMoveLine/...`、`ErpInvLandedCostLine/...`、`ErpInvTransferOrderLine/...`）**全部存在** ✓。picker 源（`ErpMd{Material,Currency,Location,Partner,Warehouse}/picker.page.yaml`）均为合法 master-data 只读 picker ✓。跨域 drawer page 路径（`ErpInvStockLedger/ref-move.page.yaml` + `ErpFinVoucherBillR/voucher-by-bill.page.yaml`）**存在** ✓。
- **P1-MA1-022**（跨域只读 daoFor 投影 / picker 快照注入字段）复核：三域跨域引用经 `notGenCode` 外部实体建立 EQL 点导航，picker 快照注入字段（id/name）均存在；inventory delta view 无 finance VoucherLine 式 `isAuxiliaryX` visibleOn 悬挂。本批次无 ORM refEntityName 重命名历史（MA1 未报告三域此类），无悬挂 ✓。**三域无 view 层 drift**。

## 3. P0-P3 finding 清单（按严重性排序）

> 起始编号 = A4.6 已分配最大（P1-MA4-023 / P2-MA4-015）+ 1 = **P1-MA4-024 / P2-MA4-016**。本审计零 P0（无活跃数据破坏路径——view.xml drift 最坏为按钮失效/颜色错误，无 GL/库存写入破坏）。

| Finding ID | 严重性 | 域 | view.xml 文件:行 | 后端对照 | 缺陷描述 | 影响 | 目标 MR |
|-----------|--------|----|-----------------|---------|---------|------|---------|
| **P1-MA4-024** | **P1 (major)** | pur | `module-purchase/erp-pur-web/.../ErpPurRfq/ErpPurRfq.view.xml:92`（`row-cancel-button` `<api url>`） | `ErpPurRfqBizModel.java:22` `cancel(@Name("rfqId") Long rfqId, ...)`；零 xbiz 声明 cancel（`rg` 确认 → 走 BizModel Java） | 「作废」按钮 `@mutation:ErpPurRfq__cancel?id=$id` 传 `id`，BizModel 期望 `rfqId`。其余 7 域 cancel 均用匹配命名参数（orderId/receiveId/invoiceId/paymentId/returnId/requisitionId/quotationId），唯 Rfq 用裸 `id` | `rfqId`=null → `requireEntity("null")` 实体未找到 → **作废按钮报错/失效**。是 P1-MA3-047（API 命名跨域不一致）的 view 层具象 | **MR2**（view.xml 代码类） |
| P2-MA4-016 | P2 (minor) | pur | `ErpPurOrder.view.xml:49,64` / `ErpPurReceive.view.xml:16` / `ErpPurInvoice.view.xml:17`（paidStatus/receiveStatus gen-control badge） | `erp-pur/paid-status`（UNPAID/PARTIAL/PAID）/ `erp-pur/receive-status`（UNRECEIVED/PARTIAL/RECEIVED） | badge 调色板硬编码跨域值（DELIVERED/COMPLETED/SETTLED/IN_PROGRESS）非 pur dict 值（死分支）；pur 有效值命中正确 | 纯视觉/可维护性（功能正确；dict 演进时调色板失同步）。watch-only | MR2（view.xml 代码类，watch-only） |
| P2-MA4-017 | P2 (minor) | inv | `ErpInvStockMove.view.xml:23` + `ErpInvLandedCost.view.xml:20`（docStatus gen-control badge） | dict `erp-inv/move-status`（DRAFT/CONFIRMED/DONE/CANCELLED，**无 ACTIVE**） | docStatus badge 模板 `${valueProp == 'ACTIVE' ? 'primary' : 'default'}` 的 `ACTIVE` 不匹配 move-status 任何值 → 状态色恒灰（default），永不蓝（primary）。A4.6 P2-MA4-014 同型系统性复制粘贴缺陷 | 纯视觉（label 正确，CANCELLED 删除线有效）；状态活跃度颜色区分失效。watch-only | MR2（view.xml 代码类，watch-only） |
| P2-MA4-018 | P2 (minor) | inv | `ErpInvStockMoveLine.view.xml:101`（totalCost sub-grid-edit gen-control `<c:script>`） | AMIS number 属性集（无 `kilometer`） | 内联脚本 `{ type:'number', kilometer:true, precision:2 }` 含非法属性 `kilometer`（复制残留），运行期静默忽略 | 零功能影响；可维护性瑕疵。watch-only | MR2（view.xml 代码类，watch-only） |

## 4. 已知 finding view 层投影复核汇总

| 来源 finding | view 层投影裁决 |
|-------------|----------------|
| P1-MA2-049（Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2） | 无 view 层 drift（Rfq view reverseApprove confirmText 通用未声称 REJECTED；Quotation view 无 reverseApprove 按钮。错误目标态属后端 xbiz） |
| P1-MA2-050（INLINE reject/withdrawApproval 缺 isCancelled guard → 副轴 drift） | 无 view 层 drift（view visibleOn 与后端 guard 缺失一致，非 view-后端分歧。drift 属后端） |
| P1-MA2-051（rollbackReceive 不对称 → receive APPROVED+posted=false 悬挂） | 无 view 层投影（Receive view 准确显示 APPROVED+posted=false，关联凭证 drawer 行为正确） |
| P1-MA2-056（Contract reverseApprove→SUBMITTED INLINE 契约漂移） | 无 view 层投影（Contract delta + _gen view 均不暴露审批按钮，误导目标态不经 view 暴露） |
| P1-MA2-057（INLINE withdrawApproval 缺 isCancelled 守卫） | 无 view 层 drift（sales 6 交易实体 view 与后端 INLINE 同等缺少 isCancelled 守卫，属后端范畴；Contract 未在 view 暴露） |
| P1-MA2-062（StockTake completeTake 未自动生成盘盈/盘亏移动单） | 无 view 层 drift（completeTake 动作名解析通过，visibleOn 与 BizModel 状态判断一致。后端逻辑缺口不经 view 暴露） |
| P1-MA2-063（PickingOrder PICKING/PICKED dict 死状态） | 无 view 层 drift（PickingOrder view 零状态守卫/visibleOn/badge 引用；查询 filter 下拉忠实绑定 dict，死状态根因在后端 CRUD stub 无 writer） |
| P1-MA3-047（API 命名/参数跨域不一致 dim7） | **发现 view 层 drift：P1-MA4-024**（ErpPurRfq.view.xml:92 cancel `?id=$id` vs BizModel `rfqId`）。此为 MA3-047 后端命名不一致的 view 层具象 |
| P1-MA3-048（孤儿 Processor bean String 影子契约 dim3） | 无 view 层 drift（三域全部 Processor 经 xbiz `<source>` 正式接线或 BizModel 委托，无孤儿悬挂动作引用） |
| P1-MA1-022（跨域只读 daoFor 投影 / picker 快照注入字段） | 无 view 层 drift（三域 picker 快照注入字段命中 notGenCode 外部实体字段，跨实体字段路径全命中 ORM refEntityName 关联，无悬挂） |
| 前端 UI-roadmap Phase 3（notify-inbox 裸 data / ErpMdPartner 非法 GraphQL / inventory.write input-table tabs） | 无三域 view.xml 残留（delta view.xml 零 `ErpMdPartner__` 引用；全部 `<data>` 为合法 AMIS drawer-data XML；inventory.write input-table tabs 已实现完整） |

## 5. Verdict

**FAIL（有 drift）—— 零 P0**（无活跃数据破坏路径；view.xml drift 最坏为按钮失效/颜色错误，无 GL/库存写入破坏）。**1 项 P1**（P1-MA4-024 purchase Rfq cancel 参数名 `id` vs `rfqId`，作废按钮功能性失效——P1-MA3-047 的 view 层具象）+ **3 项 P2** watch-only（P2-MA4-016 purchase paid/receive badge 跨域调色板 / P2-MA4-017 inventory `ACTIVE` 死状态 badge × 2 view / P2-MA4-018 inventory `kilometer` 内联脚本拼写）。MA2/MA3/MA1/前端-roadmap 已知 finding view 层投影复核全部「无 view 层 drift」或「无投影」，唯 MA3-047 投影为 P1-MA4-024。

**drift 密度评估**：purchase 40 + sales 32 + inventory 42 = 114 view.xml。**sales 域零 drift（7 维度全 PASS）**——根因 sales docStatus 绑定共享 `erp/doc-status`（含 ACTIVE）从源头消除 P2-MA4-014 死状态类 + 全部 visibleOn 状态字面量与 dict + Processor 迁移守卫双向一致 + 6 交易实体审批五动作经 xbiz→Processor 一致委托零悬挂。purchase/inventory drift 集中在参数绑定（维度 4，1 项 P1，Rfq cancel 单点）与 gen-control 内联脚本（维度 6，3 项 P2）。**drift 密度 1 P1 / 114 view.xml ≈ 0.88%**，与 A4.6（0.75%）同量级——delta 层 `bounded-merge` 自愈 + xbiz/Processor 正式接线 + ORM `ext:dict` 绑定三道防线有效抑制字段/动作/dict 三类高频 drift。**inventory 域虽为 MA2 死状态最高风险域，但 view 层未放大死状态风险**——PickingOrder view 零状态守卫（不误导用户）、StockTake completeTake 动作名正确解析。

## 6. 剩余风险

- **A4.8（crm+hr）未覆盖**：本审计仅 pur+sal+inv（A 级第二批）。crm（A 级，view.xml 数 68）+ hr（S 级，72）view.xml drift 归 A4.8。hr view.xml 数 72（与 finance 持平）且 P1-MA2-039~048 hr 死状态密集，A4.8 需重点复核 hr 排班/工时单/银行文件 dict 死状态 view 层投影。
- **`_gen` 层未逐文件深审**：本审计以 delta 层为主。`_gen` 层由 XMeta 驱动生成，理论自洽，但若 XMeta 与 ORM 不同步（MA1 未报告三域此类），`_gen` 层可能携带字段 drift。MR2 修复 P1-MA4-024 时建议同步核验 `_gen/_ErpPurRfq.view.xml`（cancel 为 delta 自定义动作，_gen 不含，无同型风险）。
- **gen-control 内联脚本无编译期校验**：P2-MA4-016/017/018 根因是 gen-control `<c:script>` 为运行期 JS，无 schema/类型校验——`ACTIVE`/调色板/属性拼写类漂移只能经运行时视觉回归或本型静态审计发现。A4.8 应沿用本审计维度 6 方法。
- **P1-MA4-024 修复方向**：将 `ErpPurRfq.view.xml:92` 的 `?id=$id` 改为 `?rfqId=$id` 以对齐 `ErpPurRfqBizModel.java:22` `@Name("rfqId")`（与其余 7 域命名参数一致，view 侧对齐风险更低）；或后端统一 cancel 参数名为 `id`（MA3-047 根因方向）。
- **Rfq/Quotation reverseApprove 后端 SUBMITTED bug**（MA2-049）+ Contract reverseApprove（MA2-056）+ withdrawApproval guard（MA2-050/057）未消解，A4.7 不涉及后端修复；待 MA2/后端修复后 view 无需改动（confirmText 已通用 / Contract view 未暴露审批按钮）。

## 7. 范围内/范围外

- **范围内**：purchase 40 + sales 32 + inventory 42 view.xml vs 后端契约 7 维度 drift（done）。
- **范围外**（Deferred）：crm+hr view.xml drift（A4.8）/ i18n 完整性（A4.9）/ 后端代码实现质量（A4.5 已 done）/ 报表 page.yaml 渲染层（前端 UI-roadmap 已修复）/ 像素级视觉回归（前端 UI-roadmap Deferred）。

## 8. 状态值映射表

### purchase

| 实体 | view 引用状态值 | dict 真值 | 裁决 |
|------|----------------|----------|------|
| 全域 docStatus | badge: ACTIVE/CANCELLED；visibleOn: !=CANCELLED | `erp/doc-status`: DRAFT/**ACTIVE**/CANCELLED | ✓（ACTIVE 在 dict **存在**，badge 正确——与 fin/mfg P2-MA4-014 不同） |
| 全域 approveStatus | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | `wf/approve-status`: 四态全中 | ✓ |
| Order/Invoice paidStatus | UNPAID/PARTIAL/PAID | `erp-pur/paid-status`: 三态 | ✓ |
| Order/Receive receiveStatus | UNRECEIVED/PARTIAL/RECEIVED | `erp-pur/receive-status`: 三态 | ✓ |
| SupplierScorecard status | delta 空 shell（_gen 驱动） | `erp-pur/scorecard-status`: DRAFT/FINALIZED | ✓ |

### sales

| 实体 | view 引用状态值 | dict 真值 | 裁决 |
|------|----------------|----------|------|
| ErpSalOrder.docStatus | badge: ACTIVE/CANCELLED；visibleOn: !=CANCELLED | `erp/doc-status`: DRAFT/**ACTIVE**/CANCELLED | ✓ |
| ErpSalOrder.approveStatus | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | `wf/approve-status`: 四态全中 | ✓ |
| ErpSalOrder.deliveryStatus | col 展示（无字面量） | `erp-sal/delivery-status`: UNDELIVERED/PARTIAL/DELIVERED | ✓ |
| ErpSalOrder.receivedStatus | col 展示（无字面量） | `erp-sal/received-status`: UNRECEIVED/PARTIAL/RECEIVED | ✓ |
| Delivery/Invoice/Receipt/Quotation/Return.docStatus/approveStatus | 同 Order（ACTIVE/CANCELLED + 四态） | `erp/doc-status` + `wf/approve-status` | ✓ |
| ErpSalContract.docStatus/approveStatus | form col（**无 badge、无审批按钮**） | `erp/doc-status` + `wf/approve-status` | ✓（无字面量引用） |

### inventory

| 实体 | view 引用状态值 | dict 真值 | 裁决 |
|------|----------------|----------|------|
| ErpInvStockMove.docStatus | visibleOn: DRAFT/CONFIRMED；badge: ACTIVE/CANCELLED | `erp-inv/move-status`: DRAFT/CONFIRMED/DONE/CANCELLED | visibleOn ✓；**badge `ACTIVE` 死值 ✗（P2-MA4-017）**；CANCELLED ✓ |
| ErpInvStockMove.approveStatus | badge: APPROVED/REJECTED/SUBMITTED | `wf/approve-status`: 四态全中 | ✓ |
| ErpInvStockMove.moveType | visibleOn: INCOMING/OUTGOING | `erp-inv/operation-type`: INCOMING/OUTGOING/INTERNAL/MANUFACTURE | ✓ |
| ErpInvStockTake.docStatus | visibleOn: DRAFT/CONFIRMED | `erp-inv/move-status`（**非** stock-take-status，该 dict 不存在；StockTake 复用 move-status） | ✓ |
| ErpInvCostAdjust.approveStatus | visibleOn: UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | `wf/approve-status`: 四态全中 | ✓ |
| ErpInvTransferOrder.docStatus | visibleOn: DRAFT | `erp-inv/move-status` | ✓ |
| ErpInvLandedCost.docStatus | badge: ACTIVE/CANCELLED | `erp-inv/move-status` | **badge `ACTIVE` 死值 ✗（P2-MA4-017）**；CANCELLED ✓ |
| ErpInvLandedCost.approveStatus | badge: APPROVED/REJECTED/SUBMITTED | `wf/approve-status` | ✓ |
| ErpInvPickingOrder.docStatus | **delta view 无状态引用**（仅 query filter + form 显示） | `erp-inv/picking-status`: **PENDING/PICKING/PICKED/CANCELLED** | view 忠实绑定 dict，无 visibleOn/badge 守卫；PICKING/PICKED 死状态无 view 投影（后端 CRUD stub 无 writer）→ ✓ 无 view drift |
| ErpInvBatch.status | query filter（无 visibleOn） | `erp-inv/batch-status`: OPEN/LOCKED/EXPIRED/CONSUMED/BLOCKED | ✓ |
| ErpInvSerialNumber.status | query filter（无 visibleOn） | `erp-inv/serial-status`: IN_STOCK/OUT/RESERVED/BLOCKED | ✓ |
| ErpInvOwnershipTransfer.docStatus | 仅 form 显示（无 visibleOn/badge） | `erp-inv/ownership-transfer-status`: DRAFT/CONFIRMED/DONE/CANCELLED | ✓ |
