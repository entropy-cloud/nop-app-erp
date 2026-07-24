# 2026-07-24-1351-2 Intercompany Cross-Company PO/SO Lifecycle

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/backlog/deepening-roadmap.md` §8.9 A3 落地证据 Deferred successor「跨公司交易完整生命周期状态机（当前仅 confirm 触发，触发：跨公司采购/销售单据直接交易需求）」（line 377）；`docs/plans/2026-07-22-1000-1-finance-multi-company-operational-depth.md`
> Related: `docs/plans/2026-07-22-1000-1-finance-multi-company-operational-depth.md`（A3 — intercompany 基础设施 + inventory transfer confirm 试点）；`docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（A1 — intercompany 4 accountKey 经 resolver 解析）；`docs/architecture/multi-company.md`（A3 EXPAND owner doc，生命周期段 L64-91 当前仅 inventory confirm）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-24，对 finance intercompany 链 + purchase/sales Processor + transfer-price resolver + multi-company.md 扫描）：

### A3 已落地的基础设施（本计划复用，不改 ORM）

- **SPI**：`IErpFinIntercompanyTransferBiz`（`erp-fin-dao/.../biz/IErpFinIntercompanyTransferBiz.java`）单方法 `onTransferConfirmed(transferOrderId, fromWarehouseId, toWarehouseId, businessDate, context)` 返回配对凭证 ID 列表。SYNC 同事务。
- **实现**：`ErpFinIntercompanyTransferBizModel`（`erp-fin-service/.../intercompany/ErpFinIntercompanyTransferBizModel.java`）— config-gate → 解析 warehouse orgId → `resolveLegalEntityRoot(orgId)` 沿 `ErpMdOrganization.parentId` 链向上找首个 `orgType=COMPANY`（含环检测）→ 同法人 skip → 解析转移定价 → 调 generator。
- **配对凭证生成器**：`IntercompanyVoucherGenerator.generatePairedVouchers`（`erp-fin-service/.../intercompany/IntercompanyVoucherGenerator.java:69-112`）— 一笔跨法人交易产 2 凭证（AR 侧 Dr INTERCOMPANY_AR/Cr INTERCOMPANY_REVENUE + AP 侧 Dr INTERCOMPANY_COST/Cr INTERCOMPANY_AP）。经 `GlMappingDimensions.fromOrgId/toOrgId` 调 glMappingResolver 解析科目（fallback 硬编码 1131/5001/1401/2202）。billType = INTERCOMPANY_SALE/INTERCOMPANY_PURCHASE。
- **转移定价解析器**：`IErpFinTransferPriceResolver`（`erp-fin-dao/.../api/`）+ `ErpFinTransferPriceResolver`（`erp-fin-service/.../posting/`）— 3 策略 COST_PLUS/MARKET/NEGOTIATED + 精确→materialCategoryId→通配 default 优先级链 + 缓存 + validity 窗口。
- **GL Mapping 维度**：`GlMappingDimensions`（`erp-fin-dao/.../dto/GlMappingDimensions.java:24/26`）含 `fromOrgId`/`toOrgId`；4 INTERCOMPANY_* accountKey 在 `erp-fin/account-key` 字典（L97/101/105/109）。
- **config-gate**：`erp-fin.intercompany-posting-enabled`（`ErpFinConstants.java:450`）默认 false，SPI 自门控（`ErpFinIntercompanyTransferBizModel:100-102`）。

### 既有触发点（单一）

- `ErpInvTransferOrderBizModel.confirm`（`erp-inv-service/.../entity/ErpInvTransferOrderBizModel.java:45-58`）— 非阻塞 try-catch 调 `intercompanyTransferBiz.onTransferConfirmed(...)`，凭证失败不阻塞库存确认。

### 扩展目标（零 intercompany 引用 — grep 实测）

| Processor | approve() | reverseApprove() | cancel() | 既有 finance SPI |
|-----------|-----------|------------------|----------|------------------|
| `ErpPurOrderProcessor` | L75 | L98 | — | commitment hooks（A2）+ budget check |
| `ErpPurReceiveProcessor` | L83 | L108 | — | stockMoveBiz（入库移动）|
| `ErpSalOrderProcessor` | L69 | L90 | — | creditLimitChecker（AR 信用，非 intercompany）|
| `ErpSalDeliveryProcessor` | L92 | L113 | — | stockMoveBiz（出库移动）+ credit hold |

**关键发现**：4 Processor 均无 intercompany 引用。跨公司采购/销售单据在 approve 时不会生成 intercompany 凭证 — 这是 A3 lifecycle 的最大缺口。`multi-company.md` 生命周期段（L64-91）仅描述 inventory confirm 触发路径。

### 保护区域提示

本工作触及会计/财务保护区域（跨公司凭证）。按 `AGENTS.md` AI 阻塞条件，owner doc（`multi-company.md` EXPAND）须先描述预期行为，且 SPI 泛化 + PO/SO 钩子接入须经 Phase 1 Decision 明确。

## Goals

- 将 intercompany 凭证生成从单一 inventory transfer confirm 触发扩展至跨公司 **采购订单**（ErpPurOrder）+ **销售订单**（ErpSalOrder）approve/reverseApprove 生命周期，使跨法人买卖单据自动生成配对的内部销售/采购凭证。
- 泛化 `IErpFinIntercompanyTransferBiz` SPI 使其支持 transfer（库存视角）+ trade-document（PO/SO 视角）两种入参形态，或新增并行 trade-document 方法（Phase 1 Decision）。
- EXPAND `docs/architecture/multi-company.md` 生命周期段，补充跨公司 PO/SO 触发路径 + 状态机语义。
- 配套 transfer pricing 在 PO/SO approve 时解析（复用既有 resolver），同法人保持现状（仅库存/业务移动）。

## Non-Goals

- **不改 ORM 实体**（A3 已落地 ErpFinIntercompanyTransferPrice/Match/ConsolidationElimination 三实体，本计划仅消费）。
- **不做实时合并报表渲染**（A3 Deferred — 复用 nop-report successor）。
- **不做跨币种合并折算**（A3 Deferred — treasury owner doc successor）。
- **不做跨公司预算结转/合并预算**（A2 Deferred successor）。
- **不接入 ErpPurInvoice/ErpSalInvoice 的 intercompany**（发票级 intercompany 凭证归 successor；本计划聚焦订单级预留/确认）。
- **不改变转移定价 3 策略算法**（COST_PLUS/MARKET/NEGOTIATED 已落地）。

## Task Route

- Type: `architecture change`（SPI 接口扩展 + 跨域 Processor 钩子）+ `implementation-only change`
- Owner Docs: `docs/architecture/multi-company.md`（A3 EXPAND，生命周期段 L64-91 扩展）、`docs/design/finance/posting.md`（跨法人凭证段 EXPAND）
- Skill Selection Basis: `nop-backend-dev`（Processor 钩子 + 跨实体 SPI + 事务边界 + config-gate + 保护区域决策门）；需阅读 `nop-entropy/docs-for-ai/02-core-guides/` 跨实体访问 + transaction-boundary 文档

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- config-gate `erp-fin.intercompany-posting-enabled` 保持默认 false；测试中开启。Phase 1 Decision 将裁定是否新增 `erp-fin.intercompany-trade-posting-enabled` 子门控（独立于 inventory transfer 门控，使 PO/SO intercompany 可独立开关）。

## Execution Plan

### Phase 1 - SPI 泛化裁决 + Owner Doc EXPAND 设计

Status: completed
Targets: `IErpFinIntercompanyTransferBiz`、`docs/architecture/multi-company.md`、`docs/design/finance/posting.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: A3 基础设施已落地（plan 2026-07-22-1000-1 completed）

- [x] Explore: 核实 ErpPurOrder/ErpSalOrder 的 orgId 字段是否表达「执行组织」、以及如何判定跨法人（订单 orgId vs 交易对手 partnerId 关联的组织，或订单行 warehouseId 对应组织）。产出跨法人判定方案候选。
   - Skill: `nop-backend-dev`
- [x] Decision: SPI 泛化策略 — 候选 (a) 新增 `onTradeDocumentApproved(docType, docId, fromOrgId, toOrgId, materialLines, businessDate, context)` 方法到既有 SPI（保持 `onTransferConfirmed` 库存视角不变）；候选 (b) 抽象统一入参 DTO。裁决维度：向后兼容（a 零触及既有 inventory 路径）vs 接口一致性。记录选择 + 替代方案 + 残留风险。
   - Skill: `nop-backend-dev`
- [x] Decision: 跨法人判定来源 — PO/SO 跨法人判定的 orgId 来源（订单头 orgId / 行 warehouseId orgId / partner 关联组织）+ MARKET 策略真实市场价 successor 边界。记录到 owner doc。
   - Skill: `nop-backend-dev`
- [x] Add: EXPAND `docs/architecture/multi-company.md` 生命周期段（L64-91）补充「跨公司 PO/SO 触发路径」子段（状态机图 + approve→配对凭证 + reverseApprove→红冲 + 同法人 skip + config-gate）。EXPAND `docs/design/finance/posting.md` 跨法人凭证段补充 PO/SO 接入点表。
   - Skill: `nop-backend-dev`

Exit Criteria:

> 仅写此阶段交付的可观察结果。完整仓库 build 在 Closure Gates。

- [x] SPI 泛化 Decision 落盘（选择 + 替代方案 + 残留风险）；multi-company.md 生命周期段含跨公司 PO/SO 路径设计

> Explore 结论：`ErpPurOrder.orgId` / `ErpSalOrder.orgId` 确认为持久化 BIGINT 列（propId 3「业务组织」= 执行组织）。`ErpMdPartner` 无 orgId 列（实测；ORM 变更属 Non-Goal），故对手方法人根经转移定价规则表反向查找解析（见 multi-company.md §Phase 1 决策记录 Decision B）。
>
> Decision A 落盘：选择候选 (a) — 新增 `onTradeDocumentApproved` + `onTradeDocumentReversed` 两方法，`onTransferConfirmed` 不变（向后兼容，零触及 inventory 路径）。替代方案 (b) 统一 DTO 被否决（2 种调用形态不需统一 DTO 抽象）。残留风险：SPI 方法数 1→3，但语义正交可接受。
>
> Decision B 落盘：执行方 = `resolveLegalEntityRoot(order.orgId)`；对手方 = 定价规则表反向查找（PO 查 toOrgId、SO 查 fromOrgId）。残留风险/successor：多对手消歧需 partner→org 精确映射（ErpMdPartner.orgId 或映射实体）。

### Phase 2 - SPI 扩展 + 采购订单 intercompany 钩子

Status: completed
Targets: `IErpFinIntercompanyTransferBiz`、`ErpFinIntercompanyTransferBizModel`、`IntercompanyVoucherGenerator`、`ErpPurOrderProcessor`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Decision

- [x] Add: 按 Phase 1 Decision 扩展 SPI（新增 trade-document 方法或 DTO）+ BizModel 实现（跨法人判定 + 转移定价解析 + 配对凭证生成，复用 IntercompanyVoucherGenerator）。如 Phase 1 Decision 选择候选 (b) DTO 含 materialLines 入参，则扩展 generator 聚合订单行级金额（Add）；如选择候选 (a) 单方法，则复用既有 generatePairedVouchers（generator 仅在 transfer 视角下调用，订单金额由 BizModel 预汇总后传入）。
- [x] Add: `ErpPurOrderProcessor.approve`（L75）后置 intercompany 钩子（非阻塞 try-catch，对齐 inventory confirm 范式 L45-58）+ `reverseApprove`（L98）红冲钩子（reverseApprove 覆盖冲销，cancel 不单独设钩子因采购订单 cancel 经 reverseApprove 或独立路径，由 Phase 1 Explore 确认）。
- [x] Proof: 单元测试 — 跨法人 PO approve 产配对凭证（INTERCOMPANY_SALE/PURCHASE billType）+ GL Mapping 4 accountKey 经 resolver 解析 + reverseApprove 红冲同向取负 + 同法人零凭证。复用 `TestErpFinIntercompanyTransfer` 范式扩展。
   - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 跨法人 PO approve→配对凭证生成可观测（voucher billType + 凭证行 Dr/Cr）；purchase service 局部 `mvn test` 通过

> 实现选择候选 (a)：SPI 新增 `onTradeDocumentApproved` + `onTradeDocumentReversed`，BizModel 复用既有 `generatePairedVouchers`（订单 totalAmountWithTax 直接传入，不经行级聚合）。`IntercompanyVoucherGenerator` 新增 `reverseIntercompany` + `hasUnreversedIntercompany`（镜像 `CommitmentVoucherGenerator.reverseCommitment`）。PO Processor `approve`/`reverseApprove`/`cancel` 三处接钩（cancel 同 commitment 范式补钩，避免孤儿凭证）。测试：`testCrossLegalEntityPurchaseOrderGeneratesPairedVouchers` + `testTradeDocumentReverseApproveRedLetters` + `testSameLegalEntityTradeDocumentNoVoucher` 全绿；finance-service 279 测试 + purchase-service 116 测试全绿。

### Phase 3 - 销售订单 intercompany 钩子 + 接收/发货联级

Status: completed
Targets: `ErpSalOrderProcessor`、`ErpPurReceiveProcessor`/`ErpSalDeliveryProcessor`（联级触发由 Phase 3 Decision 裁定：纳入范围或转 Deferred successor）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2

- [x] Add: `ErpSalOrderProcessor.approve`（L69）后置 intercompany 钩子（镜像 PO 范式，AR 侧/ AP 侧方向取决于 fromOrg/toOrg 语义）+ `reverseApprove`（L90）红冲钩子。config-gated。
- [x] Decision: Receive/Delivery 联级 — ErpPurReceive/ErpSalDelivery approve 是否触发 intercompany（货物实际移动跨法人）vs 订单级已足够。裁决：纳入范围（补 receive/delivery 钩子）或转 Deferred successor（订单级已表达跨法人交易，联级为增强）。记录选择 + 替代方案 + 残留风险。
   - Skill: `nop-backend-dev`
- [x] Proof: 单元测试 — 跨法人 SO approve 产配对凭证 + reverseApprove 红冲 + 同法人零凭证 + 与 PO 路径对称性验证。若 Phase 3 Decision 纳入 receive/delivery，补联级测试。sales service 局部 `mvn test` 通过。
   - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 跨法人 SO approve→配对凭证生成可观测；sales service 局部 `mvn test` 通过

> SO Processor `approve`/`reverseApprove`/`cancel` 三处接钩（镜像 PO）。测试：`testCrossLegalEntitySalesOrderGeneratesPairedVouchers` 验证 AR(seller)/AP(buyer) 方向对称性，全绿；sales-service 122 测试全绿。
>
> Decision（Receive/Delivery 联级）：转 Deferred successor。订单级 approve 已完整表达跨法人交易（买卖双方 + 金额 + 定价）；receive/delivery 联级生成独立 intercompany 凭证将与订单级重复计量。替代方案（纳入范围）被否决（避免双重计量）。残留风险：货物实际跨法人移动需独立凭证的业务需求触发 successor（见 §Deferred But Adjudicated）。

### Phase 4 - Owner doc 回链 + roadmap 同步 + 全仓库验证

Status: completed
Targets: `docs/architecture/multi-company.md`、`docs/design/finance/posting.md`、`docs/backlog/deepening-roadmap.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2-3

- [x] Add: multi-company.md 生命周期段回链实际落地路径（PO/SO 接入点表 + config-gate + 凭证生成路径）；posting.md 跨法人凭证段回链。deepening-roadmap §8.9 Deferred successor 标注 PO/SO lifecycle 已落地。
- [x] Add: 更新每日开发日志 `docs/logs/2026/07-24.md`。
   - Skill: `nop-backend-dev`

Exit Criteria:

- [x] owner doc 回链完成；roadmap Deferred successor 状态更新

> multi-company.md §跨公司 PO/SO 触发路径含状态机图+接入点表+5 决策记录；posting.md §跨法人内部交易凭证含 PO/SO 接入点表+SPI 扩展表；deepening-roadmap §8.9 Deferred「跨公司交易完整生命周期状态机」标已落地 + 新增 §8.12 落地证据；docs/logs/2026/07-24.md 增本计划条目。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_06d4f7407ffe1e71i0w5BMDAOZ) because 3 处 anti-slack 用词违反 Rule 10（Phase 3「可选联级」optional / Phase 2「必要时」as-needed / Infra Prereqs「可能新增」maybe）+ Phase 2/3 Item Types 漏标 Proof/Decision。已修正：3 处 hedging 改为 Decision 依赖的明确表述 + Phase 2 `Add|Proof` + Phase 3 `Add|Decision|Proof` + cancel 列说明。基线 7 项事实全部核实通过。
- Independent draft review iteration 2: acceptable as-is (ses_06d4ab226ffezW1NoBnMlJSbco) — 4 项 iteration-1 阻塞全修复 + anti-slack 扫描零残留 + 模板完整 + Decision 记录 rationale/alternatives/residual risk。无阻塞问题。

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + finance/purchase/sales service `mvn test` 一次。

- [x] 范围内行为完成（跨法人 PO/SO approve→配对凭证 + reverseApprove 红冲 + 同法人零凭证 + config-gate 默认 false）
- [x] 相关文档对齐（multi-company.md 生命周期段 EXPAND PO/SO 路径 + posting.md 跨法人凭证段）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + finance/purchase/sales `mvn test` 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 发票级 intercompany 凭证（ErpPurInvoice/ErpSalInvoice）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划聚焦订单级 intercompany；发票级（实际开票跨法人）语义更复杂（含税/结算），归 successor
- Successor Required: `yes`（触发条件：跨法人开票业务需求 + finance owner doc 授权）

### Receive/Delivery 联级 intercompany

- Classification: `optimization candidate`
- Why Not Blocking Closure: Phase 3 Decision 裁定；若订单级已足够表达跨法人交易，联级为增强
- Successor Required: `yes`（触发条件：货物实际跨法人移动需独立凭证的业务需求）

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（glm-5.2，新会话，2026-07-24）
- Verification method: 逐文件读取 LIVE 仓库源码 + 运行测试，未采信执行者声明
- Implementation verified:
  - `IErpFinIntercompanyTransferBiz`（erp-fin-dao）— 3 SPI 方法齐全：`onTransferConfirmed`（不变）+ `onTradeDocumentApproved` + `onTradeDocumentReversed`，末参均为 `IServiceContext context`，均 `@BizMutation`
  - `ErpFinIntercompanyTransferBizModel`（erp-fin-service）— 2 新方法实现完整：config-gate（`isIntercompanyPostingEnabled` 默认 false）→ `resolveLegalEntityRoot`（带环检测）→ `resolveCounterpartyLegalEntity`（转移定价规则表反向查找：PO 查 toOrgId、SO 查 fromOrgId）→ 同法人 skip；AR/AP 方向 Decision C（PO 执行方=买方、SO 执行方=卖方）
  - `IntercompanyVoucherGenerator` — `reverseIntercompany`（按 billCode 反查 INTERCOMPANY_SALE/PURCHASE 凭证→借贷互换+isReversed+reversalOfVoucherId 回链，镜像 CommitmentVoucherGenerator.reverseCommitment）+ `hasUnreversedIntercompany`
  - `ErpPurOrderProcessor` — `@Inject IErpFinIntercompanyTransferBiz`（package-private）+ `runIntercompanyApproveHook`/`runIntercompanyReverseHook` 非阻塞 try-catch，approve/reverseApprove/cancel 三处接钩
  - `ErpSalOrderProcessor` — 镜像 PO 范式，approve/reverseApprove/cancel 三处接钩
- Anti-pattern scan（rg）: 0 处 `@Inject private`、0 处 `new RuntimeException`（全 NopException）；hooks 全部非阻塞 try-catch
- Tests: `mvn -pl module-finance/erp-fin-service test -Dtest=TestErpFinIntercompanyTransfer` → **Tests run: 6, Failures: 0, Errors: 0, Skipped: 0**（2 既有 inventory + 4 新增 trade-document：PO 配对凭证 + SO 配对凭证 + reverseApprove 红冲 + 同法人零凭证）
- Docs verified:
  - `docs/architecture/multi-company.md` §跨公司 PO/SO 触发路径 EXPAND（L93）+ §Phase 1 决策记录（L131）含 Decision A-E 共 5 条
  - `docs/design/finance/posting.md` §跨法人内部交易凭证（L550）+ §PO/SO 触发路径扩展（L570）
  - `docs/logs/2026/07-24.md` 增本计划条目（L3）
  - `docs/backlog/deepening-roadmap.md` §8.9 Deferred「跨公司交易完整生命周期状态机」标已落地（L377）+ §8.12 落地证据（L439）
- Text consistency: Plan Status=completed；4 Phase Status 全 completed；Phase 1-4 全部 item + Exit Criteria 均 `[x]`；Closure Gates 1-8 全 `[x]`
- Verdict: **PASS** — 全部门控通过，实现完整、测试全绿、文档对齐、无反模式

Follow-up:

- 实时合并报表渲染（触发：业务客户合并报表需求 + report successor）
- 跨币种合并折算（触发：跨国集团多币种合并 + treasury owner doc）
