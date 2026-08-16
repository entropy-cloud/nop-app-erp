# 2026-08-16-0904-2-rc-mr1-r1-51-sal-return-exchange RC-R1.51 — sales 退货换货（MR1 越界项：ORM 结构变更 + product-scope 已裁决 Q5 不裁剪）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.51（P1-RC-025 sales 退货换货[UC-SAL-06 四断言]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.51 行 + `docs/audits/arm-index.md` P1-RC-025 行 + 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 A 类：sales RC-R1.51 ORM 变更批准——ErpSalReturn 加 returnType 列；product-scope 已裁决 Q5 不裁剪[2026-08-07 人工裁决，2026-08-08 生效]**）
> Related: `docs/design/sales/use-cases.md`（L1 UC-SAL-06 :149-162）；`docs/design/sales/returns.md`（§退货类型 :26）；`docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（A4.2.59 运行时确认）；`docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md`（Q5 裁决 :170/:190）
> Audit: required

## Current Baseline

- **finding P1-RC-025（arm-index:175，UC-SAL-06）**：L1（`use-cases.md:149-162`）逐字断言：①「退货单(returnType=换货) 审核 → 库存恢复」②「换货生成新销售出库单(关联退货单) → 扣库存」③「若价差: 补差价开票 或 退款」④「退货单与换货单通过 sourceBill 双向关联」。L3 实仓（HEAD 核查）：
  - `ErpSalReturn`（`app-erp-sales.orm.xml:857-934`）：28 列（propId 1-28，末列 remark=28 :891），**无 `returnType` 列**；**空闲 propId 29+**；无 sourceBillType/sourceBillId/sourceBillCode/relatedBillType 列（域内关联模式 = 直接 FK 列：return.deliveryId :867/894-896 → delivery.orderId :470 两跳）；
  - `ErpSalReturnLine`（:937-997）：20 列（propId 1-20），**空闲 propId 21+**；
  - grep `returnType|换货|exchange.*return`（module-sales 全仓含 orm.xml）= **0 换货相关命中**（A4.2.59 :139-140 证实；`sourceBill` 字面在 `ErpSalDashboardBizModel.java:202`/dashboard yaml 有 2 处无关命中——finance AR 账龄视图字段，非换货关联）；无 `erp-sal/return-type` dict（module-sales 既有 dict 文件为 `_vfs/dict/erp-sal/*.dict.yaml`——invoice-type/biz-type 等，无退货类型 dict）；
  - 无换货分支/新出库单生成/价差开票/退款/sourceBill 双向关联（A4.2.59 :141）。
- **真相源裁决（Q5 已人工批准，2026-08-08 生效）**：`docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md:190` §5 审批登记「Q5 | 维持 P1 强制实现（选项 A） | P1-RC-025 换货不裁剪，product-scope 不变」；product-scope.md:18 销售行为「销售订单、销售出库、销售发票、收款、销售退货」——换货未显式列入也未裁剪（:133）；L1 UC-SAL-06 显式含换货 4 断言 → **P1 强制实现，Q4=(a) 禁止方案 B**。真相源冻结（§9）：不直改 product-scope/use-cases，实现收敛向 L1 契约。
- **现有退货主路径（换货复用基础）**：`ErpSalReturnProcessor.doApprove:211-226`（triggerIncomingMove → flushSession → triggerPosting[SalReturnPostingDispatcher.tryPost:66-85] → refundOrchestrator.orchestrateRefund:49-57 → APPROVED → applyPosted → updateUndeliveredQuantity）；`ReturnStockMoveBuilder.build:40-53`（moveType=INCOMING + destWarehouseId + relatedBillType=ERP_SAL_RETURN + relatedBillCode=return.code 幂等键 :25）；`ReturnRefundOrchestrator`（reverseSettlementsForInvoice:79-99 已核销发票退款 + 未核销经 SALES_RETURN 负 AR credit memo :26-27）；`SalAcctDocProvider.createFacts` SALES_RETURN 分支（Dr 1401/Cr 6401 成本侧反向 :83-90）；`ErpSalConstants`（RELATED_BILL_TYPE_SAL_DELIVERY/SAL_RETURN :34-35）；pre-approve 守卫族（customer active/源出库 APPROVED/期间 OPEN/发票未核销/reason 必填/数量校验，R1.19 落地）。
- **换货缺失面（A4.2.59 :141 逐项）**：无 returnType 判别、无换货出库单生成、无价差处理、无 sourceBill 双向关联；无换货测试（:219「无换货测试（路径不存在）」）；无 exchange 相关 ErrorCode（ErpSalErrors:185-220 九码无换货/价差码）。
- **预授权判据**：2026-08-12 批量裁决 A 类（roadmap:36/443）——sales RC-R1.51 ORM 变更**批量批准**（ErpSalReturn 加 returnType 列）；越界回落双独立子 agent 批准（roadmap:13/29 + ai-autonomy-policy:79/83 2026-08-15 升级：ORM 保护区域 `auto + dual-agent-approval`）。**须 双独立子 agent 批准 + 独立 plan-audit**。roadmap 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-sales/model/app-erp-sales.orm.xml`（ErpSalReturn 加 returnType propId 29 + ErpSalDelivery 加 exchangeReturnId propId 29 或新 ErpSalReturnExchangeLink 实体——Phase 1 D2 裁决；ErpSalReturn 加 exchangeDeliveryId propId 30）；`ErpSalReturnBizModel.java`（换货 mutation Facade）；`ErpSalReturnProcessor.java`/新 per-mutation Processor（换货流程编排）；`ErpSalErrors.java`（新 ErrorCode）；dict `erp-sal/return-type`；`ReturnRefundOrchestrator`/`SalReturnPostingDispatcher`（价差退款/开票复用，Phase 1 D3 裁决）；新测试类 + E2E spec；owner doc `returns.md` + arm-index/roadmap/logs 回填。
- **测试基线**：`TestErpSalReturnApproval`(7)/`TestErpSalReturnCompliance`(9)/`TestErpSalReturnCostAndGuards`(12)/`TestErpSalReturnInventory`(3)/`TestErpSalReturnPosting`(2)/`TestErpSalReturnQty`(4)/`TestErpSalReturnRefund`(2)/`TestErpSalReturnRefundEndToEnd`(2)/`TestErpSalReturnTrace`(2)/`statemachine/TestErpSalReturnApprovalStateMachineMatrix`(11)——全绿基线，换货零覆盖；E2E `sal-return.action.spec.ts`（approval axis + reject/cancel :53/:120）。

## Goals

- **UC-SAL-06 四断言运行时成立（P1-RC-025 核心）**：① returnType=EXCHANGE 退货单审核 → 既有库存恢复（INCOMING move 主路径复用，零改动）；② 换货生成新销售出库单（关联退货单）→ 经既有出库→OUTGOING 移动单→扣库存链；③ 价差处理（换货金额 > 退货金额 → 补差价开票；< → 退款，经既有 AR/refund 机制）；④ 退货单与换货出库单双向关联（sourceBill 双向——域内 FK 列模式对齐）。
- **ORM 纯加性变更**（A 类批量授权范围）：`ErpSalReturn` 加 `returnType`（propId 29，VARCHAR 20，dict `erp-sal/return-type` 两值 RETURN/EXCHANGE，defaultValue="RETURN"——既有退货零行为变化，defaultValue 属 Q3 范围[R1.40 defaultValue 收敛先例]）+ 双向关联列（Phase 1 D2 裁决：ErpSalReturn.exchangeDeliveryId + ErpSalDelivery.exchangeReturnId 两 FK 列 或 新增 ErpSalReturnExchangeLink 实体）——除 returnType 显式 defaultValue 外均无 NOT NULL 无默认值无索引无 UK。
- **换货流程编排**：新 @BizMutation（Phase 1 D1 裁决触发点：return approve 后置自动 vs 独立 mutation `generateExchangeDelivery`）——复制退货头/行 → 新建 ErpSalDelivery（orderId 可空 + customer/warehouse/currency 继承退货）→ 新出库行（material/sku/uoM/quantity 可换货改物）→ 双向关联回写 → 价差分支（D3）。
- **价差语义**（D3 裁决）：换货出库金额 vs 退货金额——差为正补差价开票（复用既有发票创建/过账机制，Phase 1 核实 IErpSalInvoiceBiz 创建入口）；差为负退款（复用 ReturnRefundOrchestrator 或负 AR credit memo 机制）；差额 0 无动作。金额口径 = 头级 totalAmountWithTax 或行级 Σ（D3 裁决，对齐 L1「补差价开票 或 退款」字面）。
- **测试**：① returnType 落库 + dict 契约；② EXCHANGE 审核库存恢复（断言①）；③ 换货出库单生成 + 双向关联（断言②④）+ 出库扣库存；④ 价差开票（差正）与退款（差负）双分支；⑤ RETURN 默认零回归（既有 10 测试类全绿）；⑥ 守卫（换货前置条件：源出库已审核/期间 OPEN/发票核销状态——复用 R1.19 守卫族）；⑦ E2E sal-return spec 扩展或新增换货用例。
- **零回归**：erp-sal-service 全量测试（156 基线）全绿 + 全仓 `mvn test` + 全量构建 + compliance checker 零漂移（或基线上调带 per-site 证据——新增 daoFor 面时按先例）。
- **owner doc 收敛**：`returns.md §退货类型` 补换货实现注记（returnType/dict/流程/价差语义/双向关联）；arm-index P1-RC-025 → done (RC-R1.51) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现换货的多轮/多次换货链**（一次退货单 → 一次换货出库单；多轮换货 successor）。
- **不实现换货出库单的独立审批流**（复用既有出库单状态机——新出库单走标准 DRAFT→SUBMITTED→APPROVED，不自动 approve）。
- **不实现原路退回/其他账户/预收款抵扣等退款路由**（treasury 域 Non-Goal，ReturnRefundOrchestrator 既有边界）。
- **不实现退换货的库存批次级换货匹配**（换货出库行物料可改，批次由出库流程既有机制处理）。
- **不改变既有 RETURN 类型退货行为**（returnType default RETURN + 既有路径零改动）。
- **不改真相源契约段落**（product-scope/use-cases/returns.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 纯加性变更[A 类批量授权] + 换货流程[双独立子 agent 批准]；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/sales/use-cases.md`（L1 UC-SAL-06）+ `docs/design/sales/returns.md`
- Skill Selection Basis: ORM 模型变更 + 增量重生成（`nop-backend-dev`）；BizModel/Processor 编排 + 跨实体调用（`nop-backend-dev`：per-mutation Processor 模式 + IBiz 注入）；测试（`nop-testing`：JunitBaseTestCase + `_cases/` 快照 + E2E spec）。

## Infrastructure And Config Prereqs

- 无新 config key（换货行为由 returnType 单据字段驱动，非部署配置）。
- ORM 变更触发增量重生成：`mvn clean install -DskipTests`（不重跑 nop-cli gen）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-sales/erp-sal-service`。

## Execution Plan

### Phase 1 - 换货编排/关联载体/价差语义裁决（Decision）

Status: completed
Targets: `app-erp-sales.orm.xml`（ErpSalReturn/ErpSalDelivery/可能新实体）；`ErpSalReturnBizModel.java`；`returns.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **D1 换货触发点**：**选项 A（选定）** = 独立 @BizMutation `generateExchangeDelivery(returnId, lines[], ...)`（returnType=EXCHANGE 退货审核后操作员显式触发换货出库单生成——换货商品/数量需操作员决策，自动生成违背业务自由度）；**选项 B（否决）** = return approve 后置自动生成（换货内容无从获得，需复制退货行 = 等值换货字面但无法换不同货物——L1「换发等值或不同货物」要求操作员选择）。**理由**：L1 断言②「换货生成新销售出库单」未指定触发点；选项 A 允许不同货物 + 操作员决策 + 幂等（同一退货单多次调用防重——Phase 4 守卫）；选项 B 无法承载「不同货物」。**残留风险**：审核后生成窗口（审核通过但未生成换货单期间的运营跟踪）——换货单生成状态可经 ErpSalReturn.exchangeDeliveryId 空/非空判定，owner doc 注记引导。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D2 双向关联载体**：**选项 A（选定）** = 两 FK 列——`ErpSalReturn.exchangeDeliveryId`（propId 30，BIGINT 可空）+ `ErpSalDelivery.exchangeReturnId`（propId 29，BIGINT 可空）——域内直接 FK 模式（deliveryId/orderId 先例）+ to-one 关系 + 无 UK 无索引；**选项 B（否决）** = 新增 `ErpSalReturnExchangeLink` 实体（独立关联实体，需新实体 + 新 dao 面 + 查询接线，超 A 类授权面且双向查询需 join 链）。**理由**：A 对称两列最小面 + 双向各一跳查询 + 生成链同步；「sourceBill 双向关联」断言④ 由 exchangeDeliveryId + exchangeReturnId 双向互指满足（域内无 sourceBillType/sourceBillCode 字符串模式，FK 即关联契约）。**残留风险**：单向写漏（Phase 4 同事务双写保证）；历史数据零迁移（新列可空）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D3 价差语义与金额口径**：**选项 A（选定）** = 头级口径——价差 Δ = 换货出库单 totalAmountWithTax − 退货单 totalAmountWithTax；Δ>0 补差价开票（经既有发票创建机制——IErpSalInvoiceBiz.save 入口，CrudBizModel 管道，头+行嵌套数据），Δ<0 退款（复用 ReturnRefundOrchestrator 既有 reverse-settlement 能力）；**选项 B（否决）** = 行级逐行价差（行级匹配复杂 + 换货行物料可不同无法逐行对应——不同货物行不可比）。**理由**：头级口径与退货/出库金额聚合模式一致（totalAmountWithTax 头字段既有）；行级对换不同货物不成立。**残留风险**：换货出库金额如何产生——出库行 unitPrice 操作员录入（换货价由操作员定价，走既有出库金额计算；Remark 引导）。**实施细化（Phase 1 Proof 后定稿）**：Δ 计算与分支在 `generateExchangeDelivery` 内完成——Δ>0 经 `IErpSalInvoiceBiz.save` 建 DRAFT 发票（code=`EX-<returnCode>-DIFF` + 单行 [materialId/uoMId/quantity=1/unitPrice=Δ] + 头 totalAmountWithTax=Δ，remark 记录价差方向）；Δ<0 调 `refundOrchestrator.orchestrateRefund(returnOrder)`（既有 reverse-settlement 客户级退款）；Δ=0 无动作。价差金额 + 方向记录换货出库单 remark（审计可追溯）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D4 换货出库幂等键与重复生成语义**：**选项 A（选定）** = 幂等拒绝——`exchangeDeliveryId` 非空（已生成换货单）时再次调用 `generateExchangeDelivery` 抛 `ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED`（显式错误码，防重复生成）；**选项 B（否决）** = 幂等返回——已生成时静默返回既有换货单（隐藏重复调用错误，操作员无感知）。**理由**：显式拒绝暴露重复操作 + 与既有守卫族风格一致（ERR_RETURN_* 显式错误码族）；换货出库移动单键语义 = 新出库单经既有 DeliveryStockMoveBuilder（relatedBillType=ERP_SAL_DELIVERY + 新出库单 code），与退货 INCOMING 移动单键（ERP_SAL_RETURN + return.code）不同 moveType 不同键无冲突。**残留风险**：重复调用在 D1 选项 A 下需操作员自行处理（重新生成需先作废/删除既有换货单——owner doc 注记引导）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **复用面确认**：① IErpSalInvoiceBiz 创建入口——`ErpSalInvoiceBizModel extends CrudBizModel<ErpSalInvoice>` 标准 `save(data, context)`（ICrudBiz 三参签名），`_ErpSalInvoice.xmeta` lines 嵌套 insertable/updatable（生成 xmeta :175-176），头字段（code/customerId/businessDate/currencyId/totalAmount/totalTaxAmount/totalAmountWithTax/docStatus/approveStatus/receivedStatus/posted）全可写——D3 Δ>0 分支经 IErpSalInvoiceBiz.save 数据 map（头+嵌套 lines）可行（E2E orchestration `saveHeadWithLine` 同型实证）；② ReturnRefundOrchestrator 退款能力——`orchestrateRefund:49-57` 客户级已收款发票核销反转（findReceivedInvoicesOfCustomer :69-77 → reverseSettlementsForInvoice :79-99 → ReceiptSettler.reverseSettlement 负金额 ReceiptLine），未核销路径 = SALES_RETURN 负 AR credit memo 既有（SalReturnPostingDispatcher billData TOTAL_AMOUNT_WITH_TAX）——D3 Δ<0 分支复用 orchestrateRefund；**边界注记**：退货审核时 doApprove 已先行调用 orchestrateRefund（标准场景下换货生成时点客户发票已 UNRECEIVED → Δ<0 分支通常为 no-op，作为退款兜底 + remark 审计记录，测试 ⑤ 以「退货审核后客户再核销」时序构造可观察断言）；③ ErpSalReturnProcessor.doApprove 链（:211-226 = triggerIncomingMove → flushSession → triggerPosting → orchestrateRefund → APPROVED → applyPosted → updateUndeliveredQuantity）+ R1.19 守卫族（validateBusinessRulesForApprove :188-197 = requireCustomerActive/requireSourceDeliveryApproved/requirePeriodOpen/validateInvoiceNotSettled/requireReasonIfConfigured/returnQtyValidator）——换货前置守卫挂载点：新 per-mutation Processor（同包）经注入 facade Processor 调同包 protected step helper（ErpSalReturnApproveProcessor:45 同型实证：`processor.validateBusinessRulesForApprove` 跨类调用合法）；④ ErpSalConstants RELATED_BILL_TYPE_SAL_DELIVERY/SAL_RETURN :34-35——换货出库移动单经既有 `DeliveryStockMoveBuilder.build:28-41`（OUTGOING + ERP_SAL_DELIVERY + 新出库单 code），与退货 INCOMING 移动单键（ERP_SAL_RETURN + return.code）不同 moveType 不同键无冲突；`ErpSalDeliveryApproveProcessor.approve:31-51` = triggerOutgoingMove → applyPostingResult → setApproveStatus 链——换货出库审核扣库存断言②运行时成立（D4 定稿）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D4 裁决记录落盘（选择 + 备选 + 理由 + 残留风险），复用面清单（发票创建/退款/守卫/移动单键）产出
- [x] ORM propId 分配定稿（ErpSalReturn 29=returnType/30=exchangeDeliveryId；ErpSalDelivery 29=exchangeReturnId 空闲实证——ErpSalDelivery 业务 1-27 + remark 28，空闲 29+）与 Q3 纯加性范围核对

### Phase 2 - 双独立子 agent 批准（ORM 变更前置硬门）

Status: completed
Targets: `app-erp-sales.orm.xml`（returnType + 双向关联列设计定稿）
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 D1-D4 裁决完成

- [x] `Proof` **双独立子 agent 批准（ORM 结构变更[returnType + 双向关联列，含超 A 类字面授权[仅 returnType 列]的 exchangeDeliveryId/exchangeReturnId 2 列——越界回落双独立子 agent 批准] + 换货流程[出库单生成/价差过账面]变更门控，硬门，批准落盘前不得进入 Phase 3 ORM 实施）**：两个独立子代理（fresh session，无执行者上下文）分别检查批准（批准记录落盘本计划 Draft Review Record/Closure 段，对齐 2026-08-12 A 类裁决「越界回落双独立子 agent 批准」+ roadmap:13/29 + ai-autonomy-policy:79「两个批准均通过后才可实施」）。批准前置条件：3 列纯加性（Q3——returnType 显式 defaultValue 属 Q3 语义，其余无 NOT NULL/无默认值/无索引/无 UK）、RETURN 默认零行为变化、价差分支复用既有会计机制不新增过账 Provider（若需新增 VoucherFact/Provider 则独立 plan-audit 复核）、Q4 收敛方向。
      - Skill: `nop-backend-dev`

**批准记录（双独立子 agent，2026-08-16）**：

- **批准人 1**：`ses_ff71efedaffeo38tPDhnJNNAEq`（fresh session）——**APPROVE**。证据：ErpSalReturn 末列 remark propId 28（orm.xml:891）/ ErpSalDelivery 末列 remark propId 28（:494）→ 29/30 空闲实证；三列零命中现存模型；2 关联列无 NOT NULL/默认/索引/UK（计划 :28/:108）；唯一 defaultValue=returnType"RETURN" 纯加性（R1.40 先例）；既有 doApprove 链（:211-226）与 R1.19 守卫族（:188-197）零修改（Phase 4 仅新增 mutation + returnType 判别分支）；Δ>0 复用 IErpSalInvoiceBiz.save（标准 CrudBizModel）、Δ<0 复用 ReturnRefundOrchestrator.orchestrateRefund（:49）——零新增 VoucherFact/Provider（计划 :92 自设独立 plan-audit 条件门）；Q5 已裁决不裁剪（discussions:190）+ L1 UC-SAL-06 四断言（use-cases.md:149-162）收敛且真相源契约段不动（Non-Goals :42）；换货出库复用既有出库状态机 + 幂等 ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED 零冲突；政策符合（ai-autonomy-policy:69/:79/:83——A 类授权仅覆盖 returnType 列，2 关联列正确回落本门）。
- **批准人 2**：`ses_ff71d6568ffeOIzmiurzEmL1um`（fresh session，独立复核）——**APPROVE**。证据：propId 空闲独立实证（:891/:494 + 全仓 grep 零命中）；additive 语义 + dict 模式核对（orm.xml `<dicts>` 内联 ↔ erp-sal-meta 生成 dict.yaml，invoice-type 先例 :77）；RETURN 路径零行为变化（doApprove/守卫族逐行核对 + 退出标准 :136 git diff 证据要求）；零新增 Provider/VoucherFact（IErpSalInvoiceBiz + _ErpSalInvoice.xmeta lines insertable/updatable :76-77/:170-172 + ReturnRefundOrchestrator.orchestrateRefund:49）；Q4=(a) 收敛（A 类授权仅 returnType + 2 关联列回落本门 + Non-Goals :42 真相源不动 + 四断言全覆盖映射）；ERR_EXCHANGE 零冲突（ErpSalErrors.java 全读）；Phase 2 硬门先于 Phase 3 顺序正确（Phase 3 Prereqs 显式引用批准落盘）。2 项非阻断观察：(a) Δ<0 为客户级 orchestrateRefund 非 Δ 封顶（计划 Phase 1 Proof ② 已显式登记边界 + 测试时序构造）；(b) 2 FK 列无索引偏离域内 FK 索引范式（最小面决策，已记录接受）。

**结论**：双批准均 APPROVE，批准覆盖 3 列 ORM + 换货流程[出库单生成/价差过账面]，Phase 3 ORM 实施门解除。

Exit Criteria:

- [x] 双独立子 agent 批准记录落盘（批准人 2 个独立子代理 + 结论），批准覆盖 3 列 ORM + 换货流程范围

### Phase 3 - ORM returnType + 双向关联列 + 增量重生成（A 类批量授权变更）

Status: completed
Targets: `module-sales/model/app-erp-sales.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 D1-D4 裁决 + **Phase 2 双独立子 agent 批准落盘**

- [x] `Add` ORM 按裁决落地：`ErpSalReturn` 加 `returnType`（propId 29，VARCHAR 20，ext:dict=`erp-sal/return-type`，defaultValue="RETURN"——既有数据零迁移，defaultValue 属 Q3 显式默认值语义[R1.40 defaultValue 收敛先例]）+ `exchangeDeliveryId`（propId 30，BIGINT 可空）+ to-one `exchangeDelivery`（→ErpSalDelivery）；`ErpSalDelivery` 加 `exchangeReturnId`（propId 29，BIGINT 可空）+ to-one `exchangeReturn`（→ErpSalReturn）；新 dict `erp-sal/return-type`（RETURN 退货 / EXCHANGE 换货，i18n 双语）——除 returnType 显式 defaultValue="RETURN" 外均无 NOT NULL 无默认值无索引无 UK；索引决策 Phase 1 定稿（倾向不加，对齐最小面）。**codegen 循环依赖修正**：双向 to-one 触发 `nop.err.orm.model.ref-depends-contains-loop`（拓扑序构建器 `OrmModelTopEntryBuilder.buildDependsMap:91` 对每个 to-one 加 ref→entity 依赖边，ErpSalReturn↔ErpSalDelivery 双向边成环）——`ErpSalDelivery.exchangeReturn` 关联加 `ignoreDepends="true"`（entity.xdef :148 官方注释「出现循环依赖时需要进行标注」），保留双向运行时关联、仅从拓扑依赖边中剔除反向边（ErpSalReturn→ErpSalDelivery 主方向仍为依赖边），重新生成通过。
      - Skill: `nop-backend-dev`
- [x] `Proof` 增量重生成验证：`mvn clean install -DskipTests`（BUILD SUCCESS 156 reactor）→ 生成产物核对：`_ErpSalReturn.java`（getReturnType/setReturnType :1595-1603 + getExchangeDeliveryId/setExchangeDeliveryId :1614 + getExchangeDelivery :1656）、`_ErpSalDelivery.java`（getExchangeReturnId/setExchangeReturnId :1565 + getExchangeReturn :1607）、`_ErpSalReturn.xmeta`（returnType propId 29 :136 + exchangeDeliveryId propId 30 :140 + exchangeDelivery 关联 :150）、DDL 三方言（mysql :444/:502-503、oracle :444/:502-503、postgresql :444/:502-503 三列全量 + default 'RETURN' 物化属预期[对齐 R1.40 先例]）、dict 文件 `erp-sal-meta/src/main/resources/_vfs/dict/erp-sal/return-type.dict.yaml`（RETURN 退货/EXCHANGE 换货，`__XGEN_FORCE_OVERRIDE__` 头）——五同步 grep 核对完成 + propId 分配核对（29/30 无冲突）+ 编译通过。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 2 实体 3 列 + dict 落地（orm.xml/Entity/xmeta/DDL/dict 五同步 grep 核对），`mvn clean install -DskipTests` 生成链通过，分域编译通过

### Phase 4 - 换货流程编排

Status: completed
Targets: `ErpSalReturnBizModel.java`；`ErpSalReturnProcessor.java`/新 per-mutation Processor；`ErpSalErrors.java`；`ErpSalConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3 完成（Phase 2 批准已落盘）

- [x] `Add` `generateExchangeDelivery` @BizMutation（D1 选项 A）+ per-mutation Processor：守卫链（returnType==EXCHANGE 且已 APPROVED + 源出库已审核 + 期间 OPEN + 发票核销状态——复用 R1.19 守卫族 helper）+ 复制退货头（customer/warehouse/currency/businessDate）→ 新建 ErpSalDelivery（DRAFT + 行 [material/sku/uoM/quantity/unitPrice 操作员入参，默认复制退货行]）→ 同事务双写 exchangeDeliveryId/exchangeReturnId（D2）→ 幂等（D4 选项 A：exchangeDeliveryId 非空拒绝 ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED）+ `ErpSalErrors` 新 ErrorCode（中文描述 + define 参数表）。
      - Skill: `nop-backend-dev`
- [x] `Add` 价差分支（D3 选项 A）：换货出库单生成后价差计算 + Δ>0 补差价开票（经 IErpSalInvoiceBiz 既有入口——Phase 1 Proof ① 定稿复用面）/ Δ<0 退款（经 ReturnRefundOrchestrator 既有能力——Phase 1 Proof ② 定稿复用面）/ Δ=0 无动作；价差金额 + 方向记录 Remark（审计可追溯）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 换货出库扣库存链验证：新出库单经既有出库状态机 → DeliveryStockMoveBuilder OUTGOING 移动单（relatedBillType=ERP_SAL_DELIVERY + 新单 code——既有键语义，D4 幂等键定稿）→ validateAvailable → bookCompletion 扣库存（断言②「扣库存」运行时成立）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] generateExchangeDelivery + 价差分支 + 双向关联双写落地（grep 证据）
- [x] RETURN 类型既有路径零变更（git diff 仅 returnType 判别分支 + 新 mutation，既有 approve/posting/refund 链零改动）

### Phase 5 - 测试 + E2E + 文档回填 + 零回归验证

Status: completed
Targets: 新增 `TestErpSalReturnExchange`；E2E `sal-return.action.spec.ts` 扩展或新 spec；`returns.md`；arm-index/roadmap/`docs/logs/`
Skill: `nop-testing` + `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 4 完成

- [x] `Add` 新增 `TestErpSalReturnExchange`（7 组）：① returnType 落库 + 默认 RETURN 零回归；② EXCHANGE 审核库存恢复（断言①）；③ generateExchangeDelivery 生成换货出库单 + 双向关联（断言②④）+ 出库审核扣库存；④ 价差 Δ>0 补差价开票；⑤ 价差 Δ<0 退款；⑥ 守卫（未审核/期间 CLOSED/重复生成）+ 幂等；⑦ 换货出库单审核后 OUTGOING 移动单扣库存链（断言②运行时闭合）。
      - Skill: `nop-testing`
- [x] `Add` E2E：新增 `sal-return-exchange.action.spec.ts`（flux 渲染模式，`E2E_ENGINE` 缺省即 flux——见 `docs/testing/e2e-runbook.md`「渲染模式」节）：O2C 链 → returnType=EXCHANGE 建单 → 审核（INCOMING 移动单断言）→ generateExchangeDelivery（EX- 前缀 + exchangeReturnId 双向关联断言）→ 出库 submit/approve（OUTGOING 移动单断言）→ 幂等拒绝断言 → 全链清理。
      - Skill: `nop-testing`
- [x] `Add` owner doc 注记：`returns.md §退货类型` 补换货实现注记（returnType/dict/D1-D4 裁决/价差语义/双向关联/操作引导/多轮换货边界）；`use-cases.md` 不动。
      - Skill: `nop-backend-dev`
- [x] `Proof` 零回归验证：`mvn test -pl module-sales/erp-sal-service` **296 tests 全绿**（289 基线 + 7 新增零回归）+ E2E spec 运行（flux 模式：新 spec 1/1 + 既有 sal-return 2/2 全绿）+ 全仓 `mvn test` **3463 tests 0 failures 0 errors**（1 skipped 既有）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（**R2c 1413→1415 基线上调登记**——per-mutation Processor `returnDao()`+`loadReturnLines` 2 处同域 daoFor，per-site 证据落 compliance-baseline.md 注记 + BASELINE 块更新；R2a/R2b/R2d 零漂移）+ 回填（arm-index P1-RC-025 → done (RC-R1.51) + roadmap 行 done ✅ + `docs/logs/2026/08-16.md` 日志条目）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试全绿（①-⑦）+ E2E 换货用例全绿 + erp-sal-service 既有测试零回归 + 全量构建通过 + compliance checker 零漂移（或基线上调登记）
- [x] owner doc 注记 + 三处回填（arm-index/roadmap/log）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_ff7e194deffe6aRi8UrOWnTP4T`）——1 MAJOR + 5 MINOR。MAJOR-1 已修正：**双独立子 agent 批准门控在受保护 ORM 变更之后**（原 Phase 2 先实施 3 列 ORM、Phase 3 才批准，且 exchangeDeliveryId/exchangeReturnId 2 列超 A 类字面授权「仅 returnType 列」须越界回落批准）——重构为独立 Phase 2「双独立子 agent 批准（ORM 变更前置硬门）」置于 ORM 实施（现 Phase 3）之前，批准范围显式覆盖 3 列 + 换货流程，Phase 3 Prereqs 引用批准落盘。5 MINOR 已修正：(1) baseline grep 声明修正——`sourceBill` 字面有 2 处无关命中（dashboard AR 账龄视图），改述为「0 换货相关命中」；(2) dict 措辞修正——module-sales 既有 `*.dict.yaml` 文件（invoice-type 等），新 return-type dict 对齐既有命名约定；(3) 行号/计数修正——ErpSalReturn remark=28 在 :891、状态机矩阵测试 11 组（原 10）；(4) returnType defaultValue 措辞统一——「均无默认值」vs「defaultValue="RETURN"」自相矛盾，统一为「除 returnType 显式 defaultValue 外均无」+ R1.40 先例；(5) D4 悬空引用补全——幂等/移动单键语义升级为显式 D4 Decision 项（拒绝 vs 静默返回 + 键语义定稿），Phase 4 幂等项目引用 D4 选项 A。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_ff7d8f21bffeXL6IuFOO4Ck7bt`）——0 MAJOR + 3 新 MINOR（编辑引入）。已修正：(1) Goals ⑤「既有 11 测试类全绿」与基线 10 测试类矛盾（"11" 系矩阵 11 组混淆）→「10 测试类全绿」；(2) D1 理由陈旧阶段引用「Phase 3 守卫」→「Phase 4 守卫」（幂等守卫实现在 Phase 4）；(3) Goals :28 尾部编辑性元注释「Phase 2 措辞统一」删除（Phase 2 现为批准门非措辞统一动作）。
- Independent draft review iteration 3: `needs revision`（独立子代理 `ses_ff7d2d088ffea3ADS3quar7Kfc`）——0 MAJOR + 1 MINOR。已修正：D2 残留风险陈旧阶段引用「单向写漏（Phase 3 同事务双写保证）」→「Phase 4 同事务双写保证」（同事务双写实现在 Phase 4，Phase 3 纯 ORM 落地无事务逻辑）。其余复核全绿（三修正零残留/Phase 1→5 prereqs 链/批准硬门前置/D1-D4 决策完整/测试计数一致/无重复覆盖计划）。
- Independent draft review iteration 4: `accept`（独立子代理 `ses_ff7d1e923ffeL71onsq0suYkf6`）——「Phase 3 同事务」零残留 + 「Phase 4 同事务」1 处（D2 与 Phase 4 实现项一致）+ 其余「Phase 3」引用全部核对为正确语义（Phase 2 批准前置语/Phase 3 heading/Phase 4 prereqs/iteration-1 历史叙述）→ **共识达成，计划可转 active**。

## Closure Gates

- [x] 范围内行为完成（P1-RC-025：returnType + 双向关联 + 换货出库 + 价差 + 测试）
- [x] 相关文档对齐（returns.md 注记 + arm-index P1-RC-025 → done (RC-R1.51) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-sales/erp-sal-service` + 全仓 `mvn test` + E2E 换货 spec flux 模式 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（2026-08-16 独立结束审计执行，证据见 Closure 段）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 多轮/多次换货链

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 断言单次换货（一退货单 → 一换货出库单）；多轮换货需链式关联语义扩展（超断言面）
- Successor Required: `yes`（触发条件：运营要求退货→换货→再换货链式跟踪时，按标准立项）

### 换货出库单独立审批流/自动审核

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 换货出库复用既有出库状态机（DRAFT→SUBMITTED→APPROVED），操作员手工审核；L1 断言②未要求自动审核
- Successor Required: `no`

## Closure

Status Note: 五 Phase 全部完成并勾选，验证全绿（erp-sal-service 296 tests + 全仓 3463 tests + 全量构建 + E2E 换货 spec flux 全绿 + checker actual ≤ baseline[R2c 1413→1415 基线上调登记带 per-site 证据]）。独立结束审计（2026-08-16，独立子代理新会话）执行完毕，全部证据复核通过，Closure Gates 8/8 勾选。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver closure audit，fresh session，未共享执行者上下文）
- Evidence: ①结构检查 `plan-check.mjs --strict` 全项通过（30 checked / 0 unchecked，Plan Status: completed）；②语义复核——ORM 三列 + dict 落库实证（`app-erp-sales.orm.xml` :499/:505-506/:900-901/:907-908 returnType propId 29 + exchangeDeliveryId propId 30 + exchangeReturnId propId 29 + ignoreDepends + `return-type.dict.yaml` RETURN/EXCHANGE）；③实现复核——`ErpSalReturnGenerateExchangeDeliveryProcessor`（守卫族复用 R1.19 helper + 幂等 ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED + 双向关联同事务双写 + Δ>0 `IErpSalInvoiceBiz.save` / Δ<0 `ReturnRefundOrchestrator.orchestrateRefund` / Δ=0 无动作）beans.xml 注册 + BizModel Facade 路由 + `ErpSalErrors` 2 新码 + `ErpSalConstants` RETURN_TYPE 两常量；④测试实证——`mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalReturnExchange` **7/7 全绿** + 全量 erp-sal-service **296 tests 0 failures 0 errors**（与计划声称一致）；⑤反空洞——换货出库经 `deliveryBiz.save` 真实落库 + 出库审核 → OUTGOING 移动单 → 库存 23→20 断言闭合（test⑦）；⑥文档回填实证——`returns.md §退货类型` 实现注记 + arm-index P1-RC-025 → done (RC-R1.51) + roadmap RC-R1.51 → done ✅ + `docs/logs/2026/08-16.md` 条目 + compliance-baseline R2c 1413→1415 注记与 BASELINE 块同步（R2c: 1415）；⑦Deferred 诚实性——多轮换货链（out-of-scope improvement，successor yes + 触发条件）与换货出库独立审批流（successor no）均非范围裁剪，Δ<0 客户级退款边界在 Phase 1 Proof ② 显式登记。

Follow-up:

- （无）
