# 2026-08-16-0904-2-rc-mr1-r1-51-sal-return-exchange RC-R1.51 — sales 退货换货（MR1 越界项：ORM 结构变更 + product-scope 已裁决 Q5 不裁剪）

> Plan Status: active
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

Status: planned
Targets: `app-erp-sales.orm.xml`（ErpSalReturn/ErpSalDelivery/可能新实体）；`ErpSalReturnBizModel.java`；`returns.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [ ] `Decision` **D1 换货触发点**：**选项 A（倾向选定）** = 独立 @BizMutation `generateExchangeDelivery(returnId, lines[], ...)`（returnType=EXCHANGE 退货审核后操作员显式触发换货出库单生成——换货商品/数量需操作员决策，自动生成违背业务自由度）；**选项 B（否决）** = return approve 后置自动生成（换货内容无从获得，需复制退货行 = 等值换货字面但无法换不同货物——L1「换发等值或不同货物」要求操作员选择）。**理由**：L1 断言②「换货生成新销售出库单」未指定触发点；选项 A 允许不同货物 + 操作员决策 + 幂等（同一退货单多次调用防重——Phase 4 守卫）；选项 B 无法承载「不同货物」。**残留风险**：审核后生成窗口（审核通过但未生成换货单期间的运营跟踪）——换货单生成状态可经 ErpSalReturn.exchangeDeliveryId 空/非空判定，owner doc 注记引导。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **D2 双向关联载体**：**选项 A（倾向选定）** = 两 FK 列——`ErpSalReturn.exchangeDeliveryId`（propId 30，BIGINT 可空）+ `ErpSalDelivery.exchangeReturnId`（propId 29，BIGINT 可空，与 returnType propId 29 需核对——returnType 占 29 则 exchangeReturnId 占 29 同实体不同实体无冲突，ErpSalReturn 29=returnType、ErpSalDelivery 29=exchangeReturnId、ErpSalReturn 30=exchangeDeliveryId）——域内直接 FK 模式（deliveryId/orderId 先例）+ to-one 关系 + 无 UK 无索引（或最小 IDX 对齐既有范式）；**选项 B（否决）** = 新增 `ErpSalReturnExchangeLink` 实体（独立关联实体，需新实体 + 新 dao 面 + 查询接线，超 A 类授权面且双向查询需 join 链）。**理由**：A 对称两列最小面 + 双向各一跳查询 + 生成链同步（R1.43 实证）；「sourceBill 双向关联」断言④ 由 exchangeDeliveryId + exchangeReturnId 双向互指满足（域内无 sourceBillType/sourceBillCode 字符串模式，FK 即关联契约——A4.2.59 基线事实）。**残留风险**：单向写漏（Phase 4 同事务双写保证）；历史数据零迁移（新列可空）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **D3 价差语义与金额口径**：**选项 A（倾向选定）** = 头级口径——价差 Δ = 换货出库单 totalAmountWithTax − 退货单 totalAmountWithTax；Δ>0 补差价开票（经既有发票创建机制——Phase 1 Proof 核实 IErpSalInvoiceBiz/Invoice 创建入口可达性，复用 credit-over-limit/发票过账既有链），Δ<0 退款（复用 ReturnRefundOrchestrator 既有 reverse-settlement 或 SALES_RETURN 负 AR 机制——核实并复用）；**选项 B（否决）** = 行级逐行价差（行级匹配复杂 + 换货行物料可不同无法逐行对应——不同货物行不可比）。**理由**：头级口径与退货/出库金额聚合模式一致（totalAmountWithTax 头字段既有）；行级对换不同货物不成立。**残留风险**：换货出库金额如何产生——出库行 unitPrice 操作员录入（换货价由操作员定价，走既有出库金额计算；Remark 引导）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **D4 换货出库幂等键与重复生成语义**：**选项 A（倾向选定）** = 幂等拒绝——`exchangeDeliveryId` 非空（已生成换货单）时再次调用 `generateExchangeDelivery` 抛 `ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED`（显式错误码，防重复生成）；**选项 B（否决）** = 幂等返回——已生成时静默返回既有换货单（隐藏重复调用错误，操作员无感知）。**理由**：显式拒绝暴露重复操作 + 与既有守卫族风格一致（ERR_RETURN_* 显式错误码族）；换货出库移动单键语义 = 新出库单经既有 DeliveryStockMoveBuilder（relatedBillType=ERP_SAL_DELIVERY + 新出库单 code），与退货 INCOMING 移动单键（ERP_SAL_RETURN + return.code）不同 moveType 不同键无冲突（A4.2.59 基线事实）。**残留风险**：重复调用在 D1 选项 A 下需操作员自行处理（重新生成需先作废/删除既有换货单——owner doc 注记引导）。
      - Skill: `nop-backend-dev`
- [ ] `Proof` **复用面确认**：① IErpSalInvoiceBiz 创建入口（发票创建/保存/过账链可达性——为 D3 Δ>0 分支设计输入）；② ReturnRefundOrchestrator 退款能力（Δ<0 分支复用边界——已核销/未核销双路径 :69-99/:26-27）；③ ErpSalReturnProcessor.doApprove 链（:211-226）与 R1.19 守卫族（:188-197）接线点——换货前置守卫挂载位置；④ ErpSalConstants RELATED_BILL_TYPE_* 与换货出库移动单幂等键设计（D4 定稿：新出库单经既有 DeliveryStockMoveBuilder 键语义，与退货 INCOMING 移动单键不冲突）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] D1-D4 裁决记录落盘（选择 + 备选 + 理由 + 残留风险），复用面清单（发票创建/退款/守卫/移动单键）产出
- [ ] ORM propId 分配定稿（ErpSalReturn 29=returnType/30=exchangeDeliveryId；ErpSalDelivery 29=exchangeReturnId 空闲实证——ErpSalDelivery 业务 1-27 + remark 28，空闲 29+）与 Q3 纯加性范围核对

### Phase 2 - 双独立子 agent 批准（ORM 变更前置硬门）

Status: planned
Targets: `app-erp-sales.orm.xml`（returnType + 双向关联列设计定稿）
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 D1-D4 裁决完成

- [ ] `Proof` **双独立子 agent 批准（ORM 结构变更[returnType + 双向关联列，含超 A 类字面授权[仅 returnType 列]的 exchangeDeliveryId/exchangeReturnId 2 列——越界回落双独立子 agent 批准] + 换货流程[出库单生成/价差过账面]变更门控，硬门，批准落盘前不得进入 Phase 3 ORM 实施）**：两个独立子代理（fresh session，无执行者上下文）分别检查批准（批准记录落盘本计划 Draft Review Record/Closure 段，对齐 2026-08-12 A 类裁决「越界回落双独立子 agent 批准」+ roadmap:13/29 + ai-autonomy-policy:79「两个批准均通过后才可实施」）。批准前置条件：3 列纯加性（Q3——returnType 显式 defaultValue 属 Q3 语义，其余无 NOT NULL/无默认值/无索引/无 UK）、RETURN 默认零行为变化、价差分支复用既有会计机制不新增过账 Provider（若需新增 VoucherFact/Provider 则独立 plan-audit 复核）、Q4 收敛方向。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 双独立子 agent 批准记录落盘（批准人 2 个独立子代理 + 结论），批准覆盖 3 列 ORM + 换货流程范围

### Phase 3 - ORM returnType + 双向关联列 + 增量重生成（A 类批量授权变更）

Status: planned
Targets: `module-sales/model/app-erp-sales.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 D1-D4 裁决 + **Phase 2 双独立子 agent 批准落盘**

- [ ] `Add` ORM 按裁决落地：`ErpSalReturn` 加 `returnType`（propId 29，VARCHAR 20，ext:dict=`erp-sal/return-type`，defaultValue="RETURN"——既有数据零迁移，defaultValue 属 Q3 显式默认值语义[R1.40 defaultValue 收敛先例]）+ `exchangeDeliveryId`（propId 30，BIGINT 可空）+ to-one `exchangeDelivery`（→ErpSalDelivery）；`ErpSalDelivery` 加 `exchangeReturnId`（propId 29，BIGINT 可空）+ to-one `exchangeReturn`（→ErpSalReturn）；新 dict `erp-sal/return-type`（RETURN 退货 / EXCHANGE 换货，i18n 双语）——除 returnType 显式 defaultValue="RETURN" 外均无 NOT NULL 无默认值无索引无 UK；索引决策 Phase 1 定稿（倾向不加，对齐最小面）。
      - Skill: `nop-backend-dev`
- [ ] `Proof` 增量重生成验证：`mvn clean install -DskipTests` → 生成产物核对（两实体 getter/setter + xmeta + DDL 三方言 + dict 文件同步）+ propId 分配核对 + 编译通过。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 2 实体 3 列 + dict 落地（orm.xml/Entity/xmeta/DDL/dict 五同步 grep 核对），`mvn clean install -DskipTests` 生成链通过，分域编译通过

### Phase 4 - 换货流程编排

Status: planned
Targets: `ErpSalReturnBizModel.java`；`ErpSalReturnProcessor.java`/新 per-mutation Processor；`ErpSalErrors.java`；`ErpSalConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3 完成（Phase 2 批准已落盘）

- [ ] `Add` `generateExchangeDelivery` @BizMutation（D1 选项 A）+ per-mutation Processor：守卫链（returnType==EXCHANGE 且已 APPROVED + 源出库已审核 + 期间 OPEN + 发票核销状态——复用 R1.19 守卫族 helper）+ 复制退货头（customer/warehouse/currency/businessDate）→ 新建 ErpSalDelivery（DRAFT + 行 [material/sku/uoM/quantity/unitPrice 操作员入参，默认复制退货行]）→ 同事务双写 exchangeDeliveryId/exchangeReturnId（D2）→ 幂等（D4 选项 A：exchangeDeliveryId 非空拒绝 ERR_EXCHANGE_DELIVERY_ALREADY_GENERATED）+ `ErpSalErrors` 新 ErrorCode（中文描述 + define 参数表）。
      - Skill: `nop-backend-dev`
- [ ] `Add` 价差分支（D3 选项 A）：换货出库单生成后价差计算 + Δ>0 补差价开票（经 IErpSalInvoiceBiz 既有入口——Phase 1 Proof ① 定稿复用面）/ Δ<0 退款（经 ReturnRefundOrchestrator 既有能力——Phase 1 Proof ② 定稿复用面）/ Δ=0 无动作；价差金额 + 方向记录 Remark（审计可追溯）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` 换货出库扣库存链验证：新出库单经既有出库状态机 → DeliveryStockMoveBuilder OUTGOING 移动单（relatedBillType=ERP_SAL_DELIVERY + 新单 code——既有键语义，D4 幂等键定稿）→ validateAvailable → bookCompletion 扣库存（断言②「扣库存」运行时成立）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] generateExchangeDelivery + 价差分支 + 双向关联双写落地（grep 证据）
- [ ] RETURN 类型既有路径零变更（git diff 仅 returnType 判别分支 + 新 mutation，既有 approve/posting/refund 链零改动）

### Phase 5 - 测试 + E2E + 文档回填 + 零回归验证

Status: planned
Targets: 新增 `TestErpSalReturnExchange`；E2E `sal-return.action.spec.ts` 扩展或新 spec；`returns.md`；arm-index/roadmap/`docs/logs/`
Skill: `nop-testing` + `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 4 完成

- [ ] `Add` 新增 `TestErpSalReturnExchange`（7 组）：① returnType 落库 + 默认 RETURN 零回归；② EXCHANGE 审核库存恢复（断言①）；③ generateExchangeDelivery 生成换货出库单 + 双向关联（断言②④）+ 出库审核扣库存；④ 价差 Δ>0 补差价开票；⑤ 价差 Δ<0 退款；⑥ 守卫（未审核/期间 CLOSED/重复生成）+ 幂等；⑦ 换货出库单审核后 OUTGOING 移动单扣库存链（断言②运行时闭合）。
      - Skill: `nop-testing`
- [ ] `Add` E2E：`sal-return.action.spec.ts` 扩展换货用例（flux 渲染模式，`E2E_ENGINE` 缺省即 flux——见 `docs/testing/e2e-runbook.md`「渲染模式」节）或新增 `sal-return-exchange.action.spec.ts`（returnType=EXCHANGE 建单 → 审核 → generateExchangeDelivery → 出库审核 → 双向关联断言）。
      - Skill: `nop-testing`
- [ ] `Add` owner doc 注记：`returns.md §退货类型` 补换货实现注记（returnType/dict/D1-D4 裁决/价差语义/双向关联/操作引导）；`use-cases.md` 不动。
      - Skill: `nop-backend-dev`
- [ ] `Proof` 零回归验证：`mvn test -pl module-sales/erp-sal-service` 全绿（156 基线 + 新增零回归）+ E2E spec 运行（flux 模式）+ 全仓 `mvn test` + `mvn clean install -DskipTests` 全量构建 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（新增 daoFor 面时基线上调登记 per-site 证据）+ 回填（arm-index P1-RC-025 → done (RC-R1.51) + roadmap 行 done + `docs/logs/2026/08-16.md` 日志条目）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新测试全绿（①-⑦）+ E2E 换货用例全绿 + erp-sal-service 既有测试零回归 + 全量构建通过 + compliance checker 零漂移（或基线上调登记）
- [ ] owner doc 注记 + 三处回填（arm-index/roadmap/log）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_ff7e194deffe6aRi8UrOWnTP4T`）——1 MAJOR + 5 MINOR。MAJOR-1 已修正：**双独立子 agent 批准门控在受保护 ORM 变更之后**（原 Phase 2 先实施 3 列 ORM、Phase 3 才批准，且 exchangeDeliveryId/exchangeReturnId 2 列超 A 类字面授权「仅 returnType 列」须越界回落批准）——重构为独立 Phase 2「双独立子 agent 批准（ORM 变更前置硬门）」置于 ORM 实施（现 Phase 3）之前，批准范围显式覆盖 3 列 + 换货流程，Phase 3 Prereqs 引用批准落盘。5 MINOR 已修正：(1) baseline grep 声明修正——`sourceBill` 字面有 2 处无关命中（dashboard AR 账龄视图），改述为「0 换货相关命中」；(2) dict 措辞修正——module-sales 既有 `*.dict.yaml` 文件（invoice-type 等），新 return-type dict 对齐既有命名约定；(3) 行号/计数修正——ErpSalReturn remark=28 在 :891、状态机矩阵测试 11 组（原 10）；(4) returnType defaultValue 措辞统一——「均无默认值」vs「defaultValue="RETURN"」自相矛盾，统一为「除 returnType 显式 defaultValue 外均无」+ R1.40 先例；(5) D4 悬空引用补全——幂等/移动单键语义升级为显式 D4 Decision 项（拒绝 vs 静默返回 + 键语义定稿），Phase 4 幂等项目引用 D4 选项 A。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_ff7d8f21bffeXL6IuFOO4Ck7bt`）——0 MAJOR + 3 新 MINOR（编辑引入）。已修正：(1) Goals ⑤「既有 11 测试类全绿」与基线 10 测试类矛盾（"11" 系矩阵 11 组混淆）→「10 测试类全绿」；(2) D1 理由陈旧阶段引用「Phase 3 守卫」→「Phase 4 守卫」（幂等守卫实现在 Phase 4）；(3) Goals :28 尾部编辑性元注释「Phase 2 措辞统一」删除（Phase 2 现为批准门非措辞统一动作）。
- Independent draft review iteration 3: `needs revision`（独立子代理 `ses_ff7d2d088ffea3ADS3quar7Kfc`）——0 MAJOR + 1 MINOR。已修正：D2 残留风险陈旧阶段引用「单向写漏（Phase 3 同事务双写保证）」→「Phase 4 同事务双写保证」（同事务双写实现在 Phase 4，Phase 3 纯 ORM 落地无事务逻辑）。其余复核全绿（三修正零残留/Phase 1→5 prereqs 链/批准硬门前置/D1-D4 决策完整/测试计数一致/无重复覆盖计划）。
- Independent draft review iteration 4: `accept`（独立子代理 `ses_ff7d1e923ffeL71onsq0suYkf6`）——「Phase 3 同事务」零残留 + 「Phase 4 同事务」1 处（D2 与 Phase 4 实现项一致）+ 其余「Phase 3」引用全部核对为正确语义（Phase 2 批准前置语/Phase 3 heading/Phase 4 prereqs/iteration-1 历史叙述）→ **共识达成，计划可转 active**。

## Closure Gates

- [ ] 范围内行为完成（P1-RC-025：returnType + 双向关联 + 换货出库 + 价差 + 测试）
- [ ] 相关文档对齐（returns.md 注记 + arm-index P1-RC-025 → done (RC-R1.51) + roadmap 行 done）
- [ ] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-sales/erp-sal-service` + 全仓 `mvn test` + E2E 换货 spec flux 模式 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: 草案待独立审查（Plan Status 保持 draft 直至审查收敛）。

Closure Audit Evidence:

- Auditor / Agent: 未执行（待实施后独立结束审计）
- Evidence: —

Follow-up:

- （无）
