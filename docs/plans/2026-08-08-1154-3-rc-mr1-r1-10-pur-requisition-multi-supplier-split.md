# 2026-08-08-1154-3-rc-mr1-r1-10-pur-requisition-multi-supplier-split RC-R1.10 — purchase 请购转订单多供应商拆分（P1-RC-017，MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.10（MR1 第一批纯预授权：purchase 请购转订单多供应商拆分——convertToOrder 按行 supplier 分组生成多个 ErpPurOrder，P1-RC-017）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.10 行 + `docs/audits/arm-index.md` P1-RC-017 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3（RC-R1.10 = 纯 BizModel/Processor 重构）
> Related: `docs/design/purchase/use-cases.md`（L1 UC-PUR-08 ⑫）；`docs/design/purchase/requisition.md`（L2 请购→转订单）；`docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`（A4.2.30 运行时确认）；`docs/plans/2026-08-08-1154-1-rc-mr1-r1-8-hr-timesheet-family.md`（同批计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-017（arm-index 行）**：UC-PUR-08 ⑫——「一个请购可拆多个订单(不同供应商/到货期) 生成订单数 >= 1」被 `validateConsistentSupplier` 守卫阻断。L1 `use-cases.md:214-218` 逐字「// 一个请购可拆多个订单(不同供应商/到货期) 生成订单数 >= 1」+「// 幂等 请购单.已转订单 == true 标记后不可重复转化 再次转化 → 报错或返回已转化」。
- **A4.2.30 运行时确认（`2026-08-07-2300` 报告）**：`validateConsistentSupplier:171-186` 强制单一供应商[任一 null 或 size!=1 抛 ERR_REQ_MIXED_OR_MISSING_SUPPLIER] + L4 `testConvertMixedSupplierRejected:113-125` + `testConvertMissingSupplierRejected:128-139` 双 @Test 强断言阻断。**维持 P1 不撤销**（Q4 强制实现，修复归 MR1 纯 BizModel/Processor 预授权）。
- **实仓（HEAD 核查）**：
  - `ErpPurRequisitionProcessor.convertToOrder:89-97`：`validateApprovedForConversion` → `loadLines` → `validateLinesNonEmptyForConversion` → `validateConsistentSupplier`（单一供应商强制）→ `validateNotAlreadyConverted`（`existsActiveByRequisition` 经 `orderBiz`，查非 CANCELLED 订单）→ `doConvertToOrder`（单订单）。
  - `IErpPurRequisitionBiz.convertToOrder:24-27` 返回**单个** `ErpPurOrder`（@BizMutation，GraphQL `ErpPurRequisition__convertToOrder`）；`ErpPurRequisitionBizModel.convertToOrder:45-47` 委托 Processor。
  - `IErpPurOrderBiz.createFromRequisition:41` / `ErpPurOrderBizModel.createFromRequisition:91-102`：单订单保存 + 行批量保存。
  - `RequisitionToOrderConverter`（module-purchase/erp-pur-service/.../entity/）：`build(req, lines, supplierId, request)` 单供应商头组装（code=PO-FROM-REQ-{id}-{uuid}、requisitionId 回链、supplierId、warehouseId/currencyId 取 request 全局字段、businessDate=req.businessDate、approveStatus=UNSUBMITTED、docStatus=DRAFT）；`buildLines` 按 `lineUnitPrices[lineNo]` 解析单价。
  - `ConvertToOrderRequest`（erp-pur-dao/.../biz/）：全局 `warehouseId/currencyId` + `lineUnitPrices/lineTaxRates`（按 lineNo 映射）；**无 per-supplier 字段**。
  - **消费方普查**：前端 `erp-pur-web` 零 convertToOrder 引用（grep `convertToOrder` 全 `_vfs` 零命中）；`tests/e2e/` 零引用；唯一消费方 = 两个测试类 `TestErpPurRequisitionConvertToOrder` / `TestErpPurRequisitionToOrderEnd`（GraphQL 路径）。→ 返回契约从单 `ErpPurOrder` 改为 `List<ErpPurOrder>` 的调用面风险仅限两测试类。
  - **ORM 载体**：`ErpPurOrder` 有 `requisitionId`（propId 4，回链键）+ `supplierId`（propId 6，mandatory）+ `warehouseId`（propId 7）+ `deliveryDate`（propId 9，可空）+ `currencyId`（propId 10，mandatory）——per-supplier 头字段载体齐备，**零 ORM 变更**。行 `ErpPurOrderLine` 经 orderId 归属订单。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor 代码逻辑重构（分组逻辑 + 请求 DTO 扩展 + 契约返回调整），**不触 ORM 结构/会计核心/删除**；**无 ask-first checkbox**。roadmap RC-R1.10 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurRequisitionBiz.java`；`module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/IErpPurOrderBiz.java`；`module-purchase/erp-pur-dao/src/main/java/app/erp/pur/biz/ConvertToOrderRequest.java`；`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurRequisitionProcessor.java`；`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurRequisitionBizModel.java`；`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ErpPurOrderBizModel.java`；`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/RequisitionToOrderConverter.java`；`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurRequisitionConvertToOrder.java`；`.../TestErpPurRequisitionToOrderEnd.java`。

## Goals

- **convertToOrder 多供应商拆分**：`ErpPurRequisitionProcessor.convertToOrder` 重构——行按 `suggestedSupplierId` 分组（保留行顺序），每组生成一个 `ErpPurOrder`（含该组行）；单供应商时保持单订单（现状不变）；缺失 supplier 的行仍拒绝（`ERR_REQ_MIXED_OR_MISSING_SUPPLIER`，见 Decision）。
- **per-supplier 头字段**：`ConvertToOrderRequest` 扩展 per-supplier 映射（supplierId → warehouseId/currencyId/deliveryDate），兼容既有全局字段（单供应商/未提供映射时回退既有行为）；`RequisitionToOrderConverter.build` 支持 per-supplier 头字段 + 组行子集。
- **返回契约**：`IErpPurRequisitionBiz.convertToOrder` / `IErpPurOrderBiz.createFromRequisition` 返回 `List<ErpPurOrder>`；GraphQL `ErpPurRequisition__convertToOrder` 响应形状随之变更；两个既有测试类适配。
- **幂等保持**：`validateNotAlreadyConverted` 语义不变（一次调用全量转化；已转化再转 → `ERR_REQ_ALREADY_CONVERTED`，与 L1 幂等断言一致）。
- owner doc `requisition.md` 补实现注记（拆单语义）；回填 arm-index P1-RC-017 → `done (RC-R1.10)` + roadmap RC-R1.10 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零 UK 变更——requisitionId 回链 + per-supplier 头字段载体已就绪）。
- **不改请购审批状态机**（submit/approve/reject/reverseApprove/withdraw/cancel 既有语义不动）。
- **不做前端 AMIS 拆单交互**（页面层无 convertToOrder 消费，拆单行为纯后端契约；前端调用面 successor 注记）。
- **不做到货期拆分到行级**（L1「不同供应商/到货期」——供应商分组即拆单键，到货期按组头 `deliveryDate` 承载，行级到货期拆分无 ORM 载体，归 successor）。
- **不改真相源**（use-cases/requisition 需求契约段；仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/purchase/use-cases.md`（L1 UC-PUR-08）+ `docs/design/purchase/requisition.md`（L2 拆单注记锚点）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ `docs/audits/2026-08-07-2300-rc-ma4-a4-2-27-32-purchase-f1-mainflow-runtime.md`（A4.2.30 运行时证据）
- Skill Selection Basis: 实现面 = Processor 重构 + BizModel/IBiz 契约 + 请求 DTO 扩展（`nop-backend-dev`：@BizMutation/@Name 签名、Processor protected step 模式[派生覆盖点]、跨实体访问规则、ErrorCode 复用）；测试（`nop-testing`：JunitAutoTestCase/IGraphQLEngine 断言 + 既有测试类适配 + 快照录制）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-purchase/erp-pur-service`。

## Execution Plan

### Phase 1 - convertToOrder 按 supplier 分组重构 + per-supplier 头字段

Status: planned
Targets: `ErpPurRequisitionProcessor.java`；`ConvertToOrderRequest.java`；`RequisitionToOrderConverter.java`；`IErpPurOrderBiz.java`；`ErpPurOrderBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [ ] `Decision` **缺失 supplier 行的处理**：选项 A（推荐，对齐既有语义）= 任一行的 `suggestedSupplierId` 为 null 时整次转化拒绝（保留 `ERR_REQ_MIXED_OR_MISSING_SUPPLIER`——与 `testConvertMissingSupplierRejected` 断言一致，L1「生成订单数 >= 1」不改变缺失行的拒绝语义）；选项 B = 跳过缺失行只转有供应商的行（静默裁剪——违背「整单转化 + 幂等标记」语义，弃）。备选与理由记录于本 Decision；**残留风险**：无（选项 A 与既有测试/语义一致）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ConvertToOrderRequest` 扩展：新增 per-supplier 头字段映射（如 `Map<Long, SupplierConversionOption{warehouseId, currencyId, deliveryDate}>`，key = supplierId）；保留既有全局 `warehouseId/currencyId` 作单供应商回退（兼容既有调用方）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpPurRequisitionProcessor.convertToOrder` 重构：`validateConsistentSupplier` 改造为分组收集器（或新增 `groupLinesBySupplier` protected step 替代之）——`LinkedHashMap<Long, List<Line>>` 按行序分组 + 缺失 supplier 拒绝（Decision）；循环分组调用 `doConvertToOrder`（per-group）；`validateNotAlreadyConverted` 保持在循环前（一次调用全量转化，幂等语义不变）；`doConvertToOrder` 返回 `List<ErpPurOrder>`（保持 protected step 派生覆盖模式，派生类可覆盖单组/全组逻辑）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `RequisitionToOrderConverter`：`build(req, groupLines, supplierId, request, perSupplierOption)` 头组装支持 per-supplier warehouseId/currencyId/deliveryDate（未提供映射时回退 request 全局字段，保持单供应商现状）；`buildLines` 接收组行子集（lineNo 按组内重排或沿用请购行 lineNo，执行期定并记录）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` 返回契约调整：`IErpPurRequisitionBiz.convertToOrder` / `IErpPurOrderBiz.createFromRequisition` 返回类型 `ErpPurOrder` → `List<ErpPurOrder>`；`ErpPurRequisitionBizModel` / `ErpPurOrderBizModel` 同步（`createFromRequisition` 循环组内保存 + 行保存，或保持单组实现由 Processor 循环调用——执行期按派生覆盖友好性定并记录）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 多供应商请购一次转化生成 N 个订单（N = 供应商数），行归属正确（各组行挂对应订单）；单供应商请购仍生成 1 个订单（现状不变）
- [ ] 缺失 supplier 行仍拒绝（`ERR_REQ_MIXED_OR_MISSING_SUPPLIER`）；重复转化仍拒绝（`ERR_REQ_ALREADY_CONVERTED`）；per-supplier warehouseId/currencyId/deliveryDate 正确落各订单头
- [ ] 无 ORM 结构变更（纯 Java 契约调整 + DTO 扩展）

### Phase 2 - 既有测试适配 + 新增拆单测试

Status: planned
Targets: `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurRequisitionConvertToOrder.java`；`.../TestErpPurRequisitionToOrderEnd.java`
Skill: `nop-testing`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Fix` 适配既有测试：`TestErpPurRequisitionConvertToOrder` 内 `testConvertMixedSupplierRejected` 语义反转（原强断言拒绝 → 改断言拆单成功生成 2 订单，见新测试）；`idOf(conv)` 读取从「单实体」改为「List 首元素」；`testConvertApprovedReqToOrder` / `testConvertMissingSupplierRejected` / `testConvertNotApprovedRejected` / `testConvertIdempotentRejected` / `testConvertedOrderThenApprove` 逐一适配返回契约（`_cases/` 快照同步更新）；`TestErpPurRequisitionToOrderEnd.testRequisitionToOrderToEnd` 同型适配（两处 `idOf(conv)` 取 List 首元素）。
      - Skill: `nop-testing`
- [ ] `Add` 新增测试矩阵：① `testConvertMultiSupplierSplitsOrders`（2 供应商 × 各 1+ 行 → 2 订单 + 各组行归属 + 各单头 supplierId 正确）；② per-supplier 头字段（两组 warehouseId/currencyId/deliveryDate 各自落单）；③ 单供应商回退（不传 per-supplier 映射 → 全局字段生效，现状回归）；④ 缺失行拒绝（保留）；⑤ 幂等（拆单后重复转化 → ERR_REQ_ALREADY_CONVERTED；cancel 全部订单后可再转）；⑥ 行单价/税额按 lineNo 解析在拆单场景下正确（金额族 per-order 汇总正确）。
      - Skill: `nop-testing`
- [ ] `Proof` GraphQL 冒烟断言（`graphQLEngine.executeRpc` 调 `ErpPurRequisition__convertToOrder` 多供应商场景返回 List 结构 + 每单 code 非空 + 回链 requisitionId 正确）+ `_cases/` 快照录制（镜像既有 executeRpc 范式）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 适配后既有测试全绿 + 新增拆单矩阵全绿：`mvn test -pl module-purchase/erp-pur-service`（既有 tests 零回归）
- [ ] 拆单行为/头字段/幂等/金额族断言落地（无「契约变更但零覆盖」缺口）；GraphQL 返回 List 契约可达性有证据

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/purchase/requisition.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [ ] `Add` owner doc 补注：`requisition.md` 请购→转订单段补「多供应商拆单」实现注记（分组语义 + per-supplier 头字段 + 返回 List 契约 + 行级到货期 successor 注记）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [ ] `Add` arm-index P1-RC-017 行「修复状态」→ `done (RC-R1.10)` + 修复落地摘要；roadmap RC-R1.10 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（2026-08-08 draft review）— 格式合规（模板全部必需段齐全）、基线证据已对照实仓核验（Processor/IBiz/BizModel/Converter/DTO 签名与行号、ORM propId 载体、arm-index P1-RC-017 + roadmap RC-R1.10 + expander 映射、消费方普查、`_cases/` 快照存在性全部吻合）、预授权判据成立（纯 BizModel/Processor 重构，不触 ORM/会计核心/删除）、Decision 项含备选/理由/残留风险、Deferred 项含触发条件；Minor 已修：涉及文件补 `ErpPurRequisitionBizModel.java`；Phase 2 适配枚举补 `testConvertApprovedReqToOrder` + `TestErpPurRequisitionToOrderEnd.testRequisitionToOrderToEnd`（两处 `idOf(conv)`）；Closure Status Note 随激活同步更新

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（`mvn test -pl module-purchase/erp-pur-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——新增 DTO 字段/方法不产生 checker 新违规）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 行级到货期拆分（orderLine.deliveryDate）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1「不同供应商/到货期」的拆单键 = 供应商分组；到货期差异当前按组头 `ErpPurOrder.deliveryDate` 承载（一行级到货期无 ORM 载体）；行级到货期拆分属细化能力，非 P1-RC-017 修复面。
- Successor Required: no（触发条件 = 业务出现「同供应商组内行级到货期不同」的活跃需求）

### 前端 AMIS 拆单交互（页面层）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端返回 List 契约已满足 L1 验收（生成订单数 >= 1）；前端页面零 convertToOrder 消费，拆单展示接线属前端增强。
- Successor Required: no

## Closure

Status Note: 待执行（active）

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计

Follow-up:

- 待执行后填写（范围内项目全落地后关闭）
