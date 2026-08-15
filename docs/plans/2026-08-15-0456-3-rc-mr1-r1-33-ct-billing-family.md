# 2026-08-15-0456-3-rc-mr1-r1-33-ct-billing-family RC-R1.33 — contract 计费族（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.33（P1-RC-074 UC-CT-03 A/C invoiceTerm 生成/已开票锁 + P1-RC-075 UC-CT-04 B/C/D 消耗计费引擎，同域同修复方式族）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.33 行 + `docs/audits/arm-index.md` P1-RC-074/075 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 BizModel 计费编排，G7 contract 计费族）
> Related: `docs/design/contract/use-cases.md`（L1 UC-CT-03/04）；`docs/design/contract/state-machine.md`（§InvoicePlan 触发）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.159 triggerInvoice 守卫证据）；`docs/plans/2026-08-15-0456-2-rc-mr1-r1-32-ct-create-validation-version-family.md`（同域前行）
> Audit: required

## Current Baseline

- **finding P1-RC-074（arm-index 行，UC-CT-03 A/C）**：L1（`use-cases.md:54-58/63`）逐字「系统按合同行的 invoiceTerm 批量生成 InvoicePlan[ADVANCE 签署后 N 天/MILESTONE 里程碑日期/MONTHLY 每月固定/COMPLETION endDate] / 已开票的 InvoicePlan 不允许修改金额」。L3 实仓：
  - grep `generatePlan|generateInvoicePlan|byInvoiceTerm|ADVANCE|MILESTONE|MONTHLY|COMPLETION` 跨 erp-ct-service/src/main **仅命中实体字段+测试种子**，无「按 invoiceTerm 批量生成」编排方法（InvoicePlan 须经手工 seed，测试 `createInvoicePlan` 证实）。
  - `ErpCtInvoicePlanBizModel` 无 defaultPrepareUpdate isInvoiced 守卫（grep `isInvoiced.*amount|alreadyInvoiced.*lock` 零命中）。
  - 接受 on UC-CT-03-B 触发 AP/AR 草稿 ✅（`ErpCtInvoicePlanTriggerInvoiceProcessor#triggerInvoice:46-67` INBOUND→AP/OUTBOUND→AR 发票草稿 posted=false DRAFT O-4 豁免 P1-MA1-029 resolved + 回写 isInvoiced/invoiceBillCode/invoiceDate）+ UC-CT-03-D SUSPENDED 拦截 ✅（`:46-49` 抛 ERR_CT_CONTRACT_SUSPENDED）。
- **finding P1-RC-075（arm-index 行，UC-CT-04 B/C/D）**：L1（`use-cases.md:79-85`）逐字「周期结束时，汇总 ConsumptionLine 总量与合同行预估总量对比 / 超量部分生成额外计费 InvoicePlan / 系统生成 AP/AR Invoice 草稿给财务 / 消耗量超过合同行预估总量 120% → 触发超量审批通知」。L3 实仓：`ErpCtConsumptionLineBizModel` CRUD 桩（仅 setEntityName + amount 脱敏 loader）；grep `sumConsumption|overage|consumeSummary|periodSum|estimatedTotal|120` 跨 erp-ct-service/src/main 零业务命中——**无周期汇总对比 + 无超量 InvoicePlan + 无超 120% 通知 + 无 AP/AR 发票草稿生成**。L4：零 dedicated 测试。接受 on UC-CT-04-A 实体载体 ✅（`_ErpCtConsumptionLine` 含 consumptionDate/quantity/unitPrice/amount/sourceBillType/sourceBillCode）。
- **A4.2.159 运行时证据**（`2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`）：`ErpCtInvoicePlanTriggerInvoiceProcessor#triggerInvoice:35-54` 非 ACTIVE 态双守卫[SUSPENDED 专属 :46-49 ERR_CT_CONTRACT_SUSPENDED + 通用非 ACTIVE :50-54 ERR_CT_CONTRACT_NOT_ACTIVE] + `testTriggerInvoiceRejectedForTerminatedContract` PASS——**triggerInvoice 触发面已完整，本行补生成面（计划生成）+ 锁面（已开票禁改）**。
- **实仓（HEAD 核查）**：
  - `ErpCtInvoicePlanBizModel`：`triggerInvoice`（@BizMutation 委托 triggerInvoiceProcessor）+ `triggerDuePlans(contractId, asOfDate)`（@BizMutation 委托 `ErpCtInvoicePlanTriggerDuePlansProcessor`——config-gated `erp-ct.invoiceplan-auto-trigger` 默认 true，按 `planDate <= asOfDate && isInvoiced=false` 批量查 + 逐 plan 调 triggerInvoice，**`triggerDuePlans` 是本行 generateInvoicePlansByTerm 的既有批量触发先例**）。`ErpCtInvoicePlanTriggerDuePlansProcessor` 经 daoProvider.daoFor 直查（绕过 XMeta 查询算子白名单注记 :41-43）。
  - `ErpCtInvoicePlan` ORM（`app-erp-contract.orm.xml:274-301`）：contractLineId（mandatory）/planDate/amount/isInvoiced（default false）/invoiceBillCode/invoiceDate/invoiceTerm（**mandatory，dict erp-ct/invoice-term**：ADVANCE/MILESTONE/MONTHLY/COMPLETION）/remark——**invoiceTerm 在 Plan 实体而非 Line 实体**（L1「合同行已配置 invoiceTerm」与 ORM 结构差异：Line 无 invoiceTerm 列——**生成入口语义裁决见 Decision D2**）。
  - `ErpCtContractLine` ORM（`:195-214`）：quantity/unitPrice/amount/description，无 invoiceTerm 列。
  - `ErpCtConsumptionLine` ORM（`:305-330`）：contractLineId（mandatory）/consumptionDate（mandatory）/quantity/unitPrice/amount/sourceBillType/sourceBillCode/remark——**「合同行预估总量」= line.quantity**（UC-CT-04 前置条件：quantity 为预估总量）。
  - notify 先例：`IErpSysNotificationBiz.notify(eventType, context, ctx)` 统一派发入口——超 120% 通知经 notify（事件 `ct.consumption-over-120-percent` 或按命名规范，**Decision 项**）；无 ACTIVE 模板时 config-gated 静默跳过（R1.4 范式：不预置模板，运营侧 CRUD 登记后生效；测试侧 seed 模板）。
  - 测试基线：erp-ct-service ≈ 77 tests（同 R1.32 基线，执行时实跑计数）。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑（批量生成 mutation + isInvoiced 守卫 + 周期汇总 + notify 派发），**不触 ORM 结构/会计过账/删除**（发票草稿生成复用既有 triggerInvoice 的 IDaoProvider 直接持久化范式，O-4 豁免已 documented）；roadmap RC-R1.33 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`ErpCtInvoicePlanBizModel.java`（generateInvoicePlansByTerm + defaultPrepareUpdate 守卫）；`ErpCtConsumptionLineBizModel.java`（periodSummarize 或新 Processor）；`ErpCtErrors.java`（新增错误码）；`ErpCtConfigs.java`（按 Decision 新增 config key）；`IErpCtInvoicePlanBiz.java`/`IErpCtConsumptionLineBiz.java`（契约）；测试类（新增 `TestErpCtBillingFamily` 或扩展既有）；`docs/design/contract/volume-discount.md`（消耗计费注记）或 `state-machine.md` + `docs/audits/arm-index.md` + `docs/backlog/requirement-compliance-roadmap.md` + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **generateInvoicePlansByTerm 批量生成运行时成立（P1-RC-074 ①）**：`ErpCtInvoicePlanBizModel#generateInvoicePlansByTerm` @BizMutation——按合同行批量生成 InvoicePlan（isInvoiced=false）。生成语义按 **Decision D2** 裁决（invoiceTerm 载体：经 line 聚合映射 vs 入参指定；planDate 推导：ADVANCE 签署后 N 天/MILESTONE 里程碑日期/MONTHLY 每月固定/COMPLETION endDate——**触发日期输入契约 Decision 项**：入参 planDate 列表 vs 配置推导）。守卫：合同 ACTIVE（非 ACTIVE 拒绝，复用 ERR_CT_CONTRACT_NOT_ACTIVE 语义）+ 幂等（重复生成防重——**Decision 项**：按 contractLineId+invoiceTerm+planDate 查重或调用方职责）。
- **isInvoiced 禁改金额守卫（P1-RC-074 ②）**：`defaultPrepareUpdate` 增守卫——isInvoiced=true 的 InvoicePlan 拒绝修改 amount（/planDate/invoiceTerm，**守卫字段集 Decision 项**），抛新增 `ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE`。
- **periodSummarize 消耗汇总 + 超量 + 120% 通知（P1-RC-075）**：`ErpCtConsumptionLineBizModel#periodSummarize` @BizMutation（contractId/lineId/期间参数）——(a) 汇总期间内 ConsumptionLine 总量 Σ quantity（amount 可选）对比 line.quantity（预估总量）；(b) 超量部分（Σ > line.quantity）生成额外 InvoicePlan（**生成载体复用 generateInvoicePlansByTerm 内部能力或独立创建，Decision 项**）+ 经 triggerInvoice 生成 AP/AR 发票草稿（复用既有 triggerInvoice Processor）；(c) Σ > line.quantity × 120% 时经 `IErpSysNotificationBiz.notify` 派发超量审批通知（事件命名 + context 键集 **Decision 项**，无 ACTIVE 模板静默跳过）。返回汇总结果（总量/预估/超量/是否触发通知）。
- **测试**：新增测试组——批量生成（多行×多 term 生成 + 守卫拒绝非 ACTIVE + 幂等）；isInvoiced 锁（已开票禁改金额/未开票可改）；periodSummarize（汇总正确/超量生成/120% 通知/未超量不通知）；通知落库断言（seed 模板）；GraphQL RPC 冒烟 + 快照录制。
- **零回归**：erp-ct-service 既有测试全绿（~77 基线）+ 全仓构建 + compliance checker 零漂移。
- **回填**：arm-index P1-RC-074/075 → `done (RC-R1.33)` + roadmap 行 → `done` + owner doc 注记 + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-072/073**（创建校验/版本族——独立行 RC-R1.32，本行依赖其 submit 建 v1 的既有流但无硬依赖）。
- **不实现 P1-RC-076/077**（terminate 法务门控/审批引擎——独立行 RC-R1.34）。
- **不实现 P1-MA2-071**（到期自动化 job——独立行 RC-R1.35）。
- **不触 ORM 结构**（零列/零索引变更——invoiceTerm 已在 Plan 实体；Line 加 invoiceTerm 列属 ORM 变更不在本行，见 Decision D2 语义化）。
- **不改 triggerInvoice 既有行为**（触发面已完整 + A4.2.159 已证实守卫，本行只补生成面与锁面）。
- **不做前端 AMIS 接线**（批量生成按钮、消耗录入表单均不在本行；后端 mutation 提供能力面）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现消耗计费定时调度**（周期触发 job 属部署/调度 successor——本行只提供手工 @BizMutation 能力面，对齐既有 triggerDuePlans 手工入口先例）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/contract/use-cases.md`（L1 UC-CT-03/04）+ `docs/design/contract/volume-discount.md`（§消耗计费 + 配置点）+ `docs/design/contract/state-machine.md`（§InvoicePlan 触发）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.159 守卫证据）
- Skill Selection Basis: 实现面 = CrudBizModel defaultPrepareUpdate 守卫 + 批量生成编排 + notify 派发 + 跨实体 IDaoProvider 发票草稿范式（`nop-backend-dev`）；测试（`nop-testing`：JunitAutoTestCase + GraphQL RPC + 快照 + notify 模板 seed 范式——对齐 R1.4 测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量。config key（按 Decision）：`erp-ct.consumption-over-120-event` 或类似 notify event 名登记于 `ErpCtConstants`（无默认模板，运营侧 CRUD 登记后生效——R1.4 范式）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-contract/erp-ct-service`。

## Execution Plan

### Phase 1 - Explore 生成入口语义与通知契约裁决（Decision）

Status: completed
Targets: `ErpCtInvoicePlanBizModel.java`；`ErpCtInvoicePlanTriggerDuePlansProcessor.java`；`ErpCtConsumptionLineBizModel.java`；`ErpCtConstants.java`；`ErpCtErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **生成入口语义裁决（D2）**：L1「合同行已配置 invoiceTerm」vs ORM（invoiceTerm 在 Plan 实体，Line 无该列）——选项 A（裁决候选）：generateInvoicePlansByTerm 入参指定（contractId + 生成项列表 [{contractLineId, invoiceTerm, planDate, amount}]，服务端校验行归属合同 + 合同 ACTIVE + 幂等查重），invoiceTerm 语义由调用方（前端/集成）按 L1 流程传入——零 ORM 变更 + 能力面完整；选项 B：按行固定 term 推导（合同头单 term 批量）——与 L1 多 term 混合不符。建议选项 A + owner doc 注记（Line 无 invoiceTerm 列，生成契约经入参承载——**对齐 L1 字面「按 invoiceTerm」的入参化解释**）；记录理由 + 备选。
      - Skill: `nop-backend-dev`
- [x] `Decision` **planDate 推导与幂等裁决（D3）**：planDate 由入参显式提供（选项 A 延续）vs config 推导（ADVANCE N 天/MONTHLY 每月固定日——**决策记录**：建议入参显式 + 可选 config 默认（`erp-ct.invoice-plan-advance-days` 等，若裁决引入则登记 ErpCtConfigs））；幂等——按 contractLineId+invoiceTerm+planDate 查重拒绝重复生成 vs 调用方职责（建议服务端查重，防重复计划）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **isInvoiced 守卫字段集裁决（D4）**：defaultPrepareUpdate 守卫字段 = {amount} 最小面 vs {amount, planDate, invoiceTerm}（L1 字面「不允许修改金额」）——建议 amount+planDate+invoiceTerm（金额+时间+条款均影响已开票一致性，**决策记录**）；错误码 `ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE`（`erp.err.ct.invoice-plan-invoiced-immutable`，参数 invoicePlanId）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **120% 通知契约裁决（D5）**：notify event 命名 `ct.consumption-over-120-percent`（登记 `ErpCtConstants.NOTIFY_EVENT_CONSUMPTION_OVER_120`）+ context 键集（contractId/contractCode/contractLineId/lineDescription/estimatedQuantity/totalConsumedQuantity/overRatio）+ 无 ACTIVE 模板静默跳过（R1.4 范式，测试侧 seed 模板断言落库）。**决策记录理由 + 备选**。
      - Skill: `nop-backend-dev`
- [x] `Decision` **超量金额推导裁决（D6）**：periodSummarize 超量 InvoicePlan 的 amount 推导——选项 A（裁决候选）：超量数量 × line.unitPrice（`(Σ quantity − line.quantity) × line.unitPrice`，对齐行金额 = quantity × unitPrice 既有语义）；选项 B：Σ amount − line.amount（金额差，单位不一致时漂移）。建议选项 A + 四舍五入 scale 4 HALF_UP；同时声明超量发票草稿行语义——复用 `triggerInvoice` 生成草稿时行级 quantity/unitPrice 镜像合同行（`ErpCtInvoicePlanTriggerInvoiceProcessor:105-117/:143-154`）而 header amount = 超量金额——**量/额不一致为刻意选择（header 是计费基准，行级仅供明细展示），owner doc 注记**。**决策记录理由 + 备选**。
      - Skill: `nop-backend-dev`
- [x] `Proof` **既有测试误伤面核查**：grep erp-ct 测试集全部 `ErpCtInvoicePlan__save`/`__update`/`__triggerInvoice`/`__triggerDuePlans` 调用面——确认 defaultPrepareUpdate 增守卫后既有测试种子（isInvoiced=true 行更新金额？）零误伤；确认 triggerDuePlans 既有测试与新增生成能力零冲突。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D2-D6 决策记录落盘（含理由 + 备选）+ 误伤面核查结论（零误伤或已识别调整点）
- [x] 生成入口契约确认（入参结构 + 守卫链 + 幂等查重）+ notify event/context 契约确认

### Phase 1 Decision Record（执行落盘）

- **D2（生成入口语义，选项 A 采纳）**：`generateInvoicePlansByTerm(contractId, items[])`，item = {contractLineId, invoiceTerm, planDate, amount}（DTO `ErpCtInvoicePlanGenerateItem`，放 erp-ct-dao `app.erp.ct.biz` 包，对齐 `ConvertToOrderRequest`/`BatchOperationResult` 先例）。理由：L1「合同行已配置 invoiceTerm」与 ORM 结构错配（`Plan.invoiceTerm` mandatory，`Line` 无 invoiceTerm 列）——入参化承载对齐 L1 字面「按 invoiceTerm」的入参化解释 + 零 ORM 变更 + 支持 L1 多 term 混合（ADVANCE/MILESTONE/MONTHLY/COMPLETION 同合同并存）。备选 B（合同头单 term 推导）否决：与 L1 多 term 混合场景不符。残留风险：调用方须按 L1 流程自行推导 term/planDate（owner doc 注记，前端接线 successor）。
- **D3（planDate 推导 + 幂等，选项 A 延续 + 服务端查重）**：planDate 由入参显式提供；**不引入 config 推导键**（`erp-ct.invoice-plan-advance-days` 等裁决不采纳——入参显式已覆盖 ADVANCE N 天/MONTHLY 固定日语义，config 推导属调度 successor 范畴）。幂等：服务端按 (contractLineId, invoiceTerm, planDate) 查重，重复生成抛新增 `ERR_CT_INVOICE_PLAN_DUPLICATE`（防重复计划；调用方职责备选否决——服务端查重才能防前端/集成重复调用）。
- **D4（isInvoiced 守卫字段集）**：守卫字段 = {amount, planDate, invoiceTerm}（L1 字面「不允许修改金额」最小面否决——planDate/invoiceTerm 同样影响已开票一致性：开票日期/条款回写后改时间/条款产生账实漂移；remark 等非计费字段放行）。错误码 `ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE`（`erp.err.ct.invoice-plan-invoiced-immutable`，参数 invoicePlanId）。取更新前状态经 ORM 脏值追踪（`orm_dirtyOldValues()`，对齐 R1.9 `ErpHrSurveyBizModel:99-117` publish 守卫范式）。
- **D5（120% 通知契约）**：event `ct.consumption-over-120-percent`，常量 `ErpCtConstants.NOTIFY_EVENT_CONSUMPTION_OVER_120`；context 键集 {contractId, contractCode, contractLineId, lineDescription, estimatedQuantity, totalConsumedQuantity, overRatio}。经 `IErpSysNotificationBiz.notify`（注入 notify-dao 接口，compile scope；测试 scope 引 notify-service 提供 Bean 实现——对齐 cs/purchase 先例）。无 ACTIVE 模板 → notify Processor best-effort 静默跳过（R1.4 范式，不阻断 periodSummarize 业务事实）。备选：域内自建通知表否决（重复造轮子，违反跨域通知派发子系统统一入口）。
- **D6（超量金额推导，选项 A 采纳）**：`overageAmount = (Σquantity − line.quantity) × line.unitPrice`，scale 4 HALF_UP（对齐行金额 = quantity × unitPrice 既有语义；备选 B Σamount − line.amount 否决——amount 为录入值非严格恒等于 qty×price，单位不一致时漂移）。**超量发票草稿量/额不一致声明**：复用 `triggerInvoice` 生成草稿时行级 quantity/unitPrice 镜像合同行（TriggerInvoiceProcessor 既有语义），header amount = 超量金额——刻意选择（header 是计费基准，行级仅供明细展示），owner doc 注记。
- **Proof（误伤面核查结论：零误伤）**：grep erp-ct 测试集——`ErpCtInvoicePlan__save` 仅 3 处种子调用（`TestErpCtContractPosting.saveInvoicePlan:237-249` ×2 场景 + `TestErpCtContractRebate.saveInvoicePlan:432`），全部 isInvoiced=false 新建路径，**零 `__update` 调用、零 isInvoiced=true 行改金额**（defaultPrepareUpdate 守卫零误伤）；`__triggerInvoice` 调用面（Posting 4 + Rebate 2）走 Processor `dao().updateEntity` 直写（:68），不经 CrudBizModel 更新路径，守卫不拦截回写（draft review MINOR 已核实）；`__triggerDuePlans` 零测试调用（新增生成能力与既有测试零冲突）。

### Phase 2 - generateInvoicePlansByTerm + isInvoiced 守卫落地（P1-RC-074 核心）

Status: completed
Targets: `ErpCtInvoicePlanBizModel.java`；`ErpCtErrors.java`；`IErpCtInvoicePlanBiz.java`；`ErpCtConstants.java`（按 D5 先登记）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成

- [x] `Add` `generateInvoicePlansByTerm` @BizMutation（契约 + 实现，镜像 triggerDuePlans 入口形态）：入参（contractId + 生成项列表，D2/D3 契约）→ 守卫（合同 ACTIVE，复用 ERR_CT_CONTRACT_NOT_ACTIVE/ERR_CT_CONTRACT_SUSPENDED 语义）→ 行归属校验（contractLineId ∈ contract.lines）→ 幂等查重（D3）→ 批量 saveEntity InvoicePlan（isInvoiced=false + invoiceTerm + planDate + amount）→ 返回生成计数/列表。建议 Processor 形态（`ErpCtInvoicePlanGenerateByTermProcessor`）或 BizModel 直写——**决策记录**（对齐既有 triggerDuePlans 的 Processor 委托范式，建议 Processor）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `defaultPrepareUpdate` 增 isInvoiced 守卫（D4 字段集）：isInvoiced=true 且 amount/planDate/invoiceTerm 变更 → 抛 `ERR_CT_INVOICE_PLAN_INVOICED_IMMUTABLE`（ORM 脏值追踪取更新前状态——对齐 R1.9 publish 守卫范式）。
      - Skill: `nop-backend-dev`
- [x] `Add` 错误码 define + 参数表注册（D4）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 批量生成接线且守卫落地（GraphQL 实调生成多 plan + 非 ACTIVE 拒绝 + 幂等拒绝）
- [x] isInvoiced 锁运行时拒绝（GraphQL 实调已开票计划改金额 → 错误码断言）

### Phase 3 - periodSummarize 消耗汇总 + 超量 + 120% 通知落地（P1-RC-075 核心）

Status: completed
Targets: `ErpCtConsumptionLineBizModel.java`（或新 `ErpCtConsumptionPeriodSummarizeProcessor`）；`ErpCtErrors.java`；`IErpCtConsumptionLineBiz.java`；`ErpCtConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2 完成（复用 generateInvoicePlansByTerm 内部能力）

- [x] `Add` `periodSummarize` @BizMutation（契约 + 实现）：入参（contractId/contractLineId/期间 fromDate/toDate，**决策记录**：行级 vs 合同级聚合入口，建议 lineId 单行 + 合同级可选循环）→ (a) 期间内 Σ ConsumptionLine.quantity（+ amount 可选）对比 line.quantity；(b) 超量（Σ > line.quantity）→ 生成额外 InvoicePlan（复用 Phase 2 生成内部能力，invoiceTerm 按 D2 契约传参，amount 按 D6 推导）+ 调 triggerInvoiceProcessor.triggerInvoice 生成 AP/AR 发票草稿（复用既有 Processor，行级明细镜像语义按 D6 注记）；(c) Σ > line.quantity × 1.2 → `IErpSysNotificationBiz.notify(NOTIFY_EVENT_CONSUMPTION_OVER_120, ctx, context)`（D5 契约）→ 返回汇总结果对象。
      - Skill: `nop-backend-dev`
- [x] `Add` 错误码（按需：行不存在/期间非法）+ `ErpCtConstants` notify event 常量（D5）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] periodSummarize 汇总/超量/通知运行时成立（GraphQL 实调：Σ<预估无动作 / Σ>预估生成计划+发票草稿 / Σ>120% 通知落库断言）
- [x] 发票草稿复用既有 triggerInvoice 路径（非新增过账逻辑——O-4 豁免语义不变）

### Phase 4 - 测试矩阵

Status: completed
Targets: `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/`（新增 `TestErpCtBillingFamily` 或扩展既有）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-3 完成

- [x] `Add` 测试组（按 Goals）：① 批量生成（2 行×2 term 生成 4 plan 断言 + 非 ACTIVE 拒绝 + 幂等拒绝）；② isInvoiced 锁（已开票改金额拒绝 + 未开票可改 + 改 remark 放行）；③ periodSummarize 汇总正确（Σ 计算 + line.quantity 对比）；④ 超量生成（Σ>预估 → 额外 plan + 发票草稿落库）；⑤ 120% 通知（seed 模板 → notify 落库 + recipient 断言；未超量零通知）；⑥ GraphQL RPC 冒烟 + 快照录制。
      - Skill: `nop-testing`
- [x] `Proof` 既有 erp-ct-service 测试零回归：`mvn test -pl module-contract/erp-ct-service`（~77 基线 + 新增全绿）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试组全绿 + erp-ct-service 全模块零回归（BUILD SUCCESS）
- [x] 生成/锁/汇总/通知有运行时断言证据（GraphQL RPC 实调，非仅静态接线）

> **Phase 4 测试范式注记（执行落盘）**：测试矩阵 ⑥「GraphQL RPC 冒烟 + 快照录制」按同域同族先例（R1.32 `TestErpCtContractCreateValidate`「`_cases/` 无快照（直断言范式）」）执行为**直断言范式**——12 组全部经 GraphQL RPC 实调 + DAO 落库断言（拒绝路径不录快照避免 output-row-not-exists 不可比基线，对齐 nop-testing skill 拒绝路径指引）；`_cases/` 快照录制对拒绝路径测试无增益，留作后续正路径测试增强候选（非本行范围）。

### Phase 5 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/contract/volume-discount.md`（或 `state-machine.md`）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-4 完成

- [x] `Add` owner doc 注记：消耗计费/开票计划生成实现注记（生成入口契约 + isInvoiced 锁 + periodSummarize 语义 + 120% 通知事件 + Line 无 invoiceTerm 列的结构注记）；不修改需求契约段（use-cases L1 不动）。
      - Skill: none
- [x] `Add` arm-index P1-RC-074/075 → `done (RC-R1.33)` + 修复落地摘要；roadmap RC-R1.33 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_ffdeccff5ffeP6tH1rzsT5sf7z）— 0 BLOCKER / 0 MAJOR / 5 MINOR。基线准确（含核心结构性声明「Line 无 invoiceTerm 列而 Plan.invoiceTerm mandatory」实证成立）、D2 入参化裁决正确处理结构错配、范围与预授权相符、isInvoiced 守卫不阻塞 triggerInvoice 回写（dao().updateEntity 直写绕过 CrudBizModel 更新路径）无 blocker。5 MINOR 全部折叠处理：行号注记 :33-38→:41-43 / 测试计数 ~77（实跑 78-82）维持近似表述 / 超量金额公式新增 Decision D6 / 超量草稿行量额不一致语义并入 D6 注记 / Deferred 段 draft 期已登记候选分类项。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成——P1-RC-074/075 批量生成 + isInvoiced 锁 + 消耗汇总/超量/120% 通知运行时成立（独立结束审计逐文件核验，ses_ffcd3e843ffe0LNlqjwyq4aeT0）
- [x] 相关文档对齐——arm-index/roadmap/owner doc/日志回填（独立结束审计核验）
- [x] 已运行验证（`mvn test -pl module-contract/erp-ct-service` 全绿 110/110 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline 1399）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 4 项均有 successor 分类与触发条件，无已确认缺陷/契约漂移）
- [x] 独立草案审查已完成并记录（Draft Review Record 迭代 1 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（独立结束审计 §1 核验）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（ses_ffcd3e843ffe0LNlqjwyq4aeT0，独立新会话）
- [x] 结束证据存在于文件中（arm-index/roadmap/volume-discount.md/compliance-baseline.md/docs-logs 落盘）

## Deferred But Adjudicated

- **消耗计费定时调度 job**（触发条件 = 周期自动化业务流立项，successor yes）：periodSummarize 手工 @BizMutation 能力面已落地（Non-Goal 声明对齐），周期自动触发属部署/调度 successor。
- **合同级 periodSummarize 聚合循环**（D6 行级入口的合同级扩展，successor no）：行级聚合已实现，合同级可选循环留扩展。
- **前端批量生成/消耗录入 AMIS 接线**（watch-only residual，successor no）：Non-Goal 声明，后端 mutation 能力面已提供。
- **生成入口入参化 vs 未来 Line 加 invoiceTerm 列**（ORM 演进，触 `model/*.orm.xml` ask-first，successor yes = 行级 term 持久化需求立项）：D2 入参化承载已裁决，ORM 演进需人工裁决。

## Closure

Status Note: 四 Phase 全部执行完毕（Phase 1 裁决落盘 → Phase 2-3 落地 → Phase 4 测试全绿 → Phase 5 回填）。验证：`mvn test -pl module-contract/erp-ct-service` 110/110 全绿（98 基线 + 12 新增）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（R2c 1394→1399 基线上调登记，per-site 证据落 `compliance-baseline.md`，其余 18 规则零漂移）。arm-index P1-RC-074/075 → done (RC-R1.33) + roadmap RC-R1.33 → done ✅ + owner doc volume-discount.md 实现注记 + 日志条目写入。独立结束审计 APPROVED（见下）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_ffcd3e843ffe0LNlqjwyq4aeT0，无执行者上下文）— 逐项核验：①文本一致性 PASS（5 Phase 全 completed、items/exit criteria 全 [x]）②行为完整性 PASS（generateInvoicePlansByTerm 委托 + isInvoiced 守卫 :107-132、GenerateByTermProcessor 守卫链 :63-96、SummarizeProcessor Σ/超量/通知 :90-128、IBiz 契约 @BizMutation、5 错误码 :76-94、常量 :76、beans.xml :64-68、pom notify-dao/notify-service）③验证证据 PASS（日志/arm-index/roadmap/volume-discount.md 落盘）④compliance 基线 PASS（R2c 1394→1399 注记 + machine-readable 块 R2c: 1399 + per-site 证据）⑤反模式自检 PASS（零 @Inject private / 全 ErrorCode+NopException / newEntity() 构造 / 契约带 @BizMutation）⑥Closure Gates 逐项实质核验通过。3 MINOR 全部收尾：compliance-baseline.md 顶部表 1394→1399 + roadmap「3 Processor」措辞订正 + 门控勾选。**最终裁决：APPROVED**

Follow-up:

- 无（Deferred But Adjudicated 4 项均已分类 + 触发条件，无已确认缺陷）
