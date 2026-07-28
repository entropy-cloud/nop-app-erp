# 2026-07-29-0430-2-audit-remediation-ma4-pur-sal-inv-qa-crm-code-quality MA4 pur+sal+inv+qa+crm 代码质量抽样审计（A4.5）

> Plan Status: active
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.5，pur+sal+inv+qa+crm 代码质量抽样——A 级合并）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」行；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/code-quality-audit-prompt.md`（审计方法）；`docs/design/purchase/`（state-machine.md + three-way-match.md + returns.md）+ `docs/design/sales/`（state-machine.md + use-cases.md + returns.md）+ `docs/design/finance/costing-methods.md`（库存成本方法 owner doc，跨 inv 引用）+ `docs/design/quality/state-machine.md` + crm README（各域 README）；`docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（A2.8）+ `2026-07-28-0400-1-...-sales-...`（A2.9）+ `2026-07-28-0400-3-...-inventory-...`（A2.11）+ `2026-07-28-1020-1-...-quality-...`（A2.12）+ `2026-07-28-1020-3-...-ext-domains-...`（A2.14 crm 子集）；`docs/plans/2026-07-29-0024-3-audit-remediation-ma4-assets-depreciation-processor-code-quality.md`（A4.3 MA4 已落地范式参照）
> Audit: required

## Current Baseline

purchase + sales + inventory + quality + crm 五域代码质量抽样审计（代码与前端质量层 MA4 第七项，A 级合并）。roadmap 工作项 A4.5 声明审查"pur+sal+inv+qa+crm 代码质量抽样（A 级合并）"，owner doc 标注各域 README，skill `docs/skills/code-quality-audit-prompt.md`。

**关键基线事实（实时仓库核实）**：

- **五域代码规模**（实时仓库核实）：`find module-<domain> -path "*service*" -name "*.java" -not -path "*/target/*" -not -path "*_gen/*"`：purchase **111 文件** / sales **109 文件** / inventory **89 文件** / quality **69 文件** / crm **75 文件** = **合计 453 文件**。五域均为 A 级核心/扩展域，业务逻辑密度高。核心组件（按域组织）：
  - **purchase**：`ErpPurOrderProcessor` + `ErpPurReceiveProcessor` + `ErpPurInvoiceProcessor` + `ErpPurPaymentProcessor` + `ErpPurReturnProcessor` + `ErpPurRequisitionProcessor`（六实体大 Processor 审批轴 + 过账）/ `PaymentSettler`（付款核销）/ `PurAcctDocProvider` 系列（采购过账科目文档）/ `ThreeWayMatchService`（三单匹配）
  - **sales**：`ErpSalOrderProcessor` + `ErpSalDeliveryProcessor` + `ErpSalInvoiceProcessor` + `ErpSalReceiptProcessor` + `ErpSalReturnProcessor` + `ErpSalQuotationProcessor`（六实体大 Processor）/ `SalAcctDocProvider` 系列（销售过账科目文档）/ `DeliveryStockMoveBuilder`
  - **inventory**：`StockMoveBookkeeper`（移动单记账 upsertBalance——P0-MA2-020 UK 已修复）/ `ErpInvOwnershipTransferProcessor`（所有权转移）/ `ErpInvLandedCostProcessor`（到岸成本分摊）/ `StandardCostResolver` + `CostMethodResolver` + `CostAdjustmentService`（成本方法解析与调整，方法语义见 `docs/design/finance/costing-methods.md`）/ `ErpInvStockTakeProcessor` + `ErpInvPickListProcessor`
  - **quality**：`ErpQaInspectionBizModel`（passInspection/failInspection——P0-MA2-017 状态守卫已修复）/ `NcrPostingDispatcher` + `NcrReturnOrchestrator`（NCR 过账 + 退货编排）/ `InspectionTrigger`（质检门控）/ `SpcCalculator`（SPC 统计过程控制）
  - **crm**：`ErpCrmLeadBizModel`（Lead 评分 + 状态漏斗）/ `ErpCrmForecastBizModel`（销售预测）/ `ErpCrmPriceRuleBizModel`（价格规则）/ `ErpCrmFunnelStageMetricsBizModel`（漏斗阶段指标）
- **owner docs**：purchase `state-machine.md` + `three-way-match.md` + `returns.md`；sales `state-machine.md` + `use-cases.md` + `returns.md`；inventory `state-machine.md`（库存成本方法见 `docs/design/finance/costing-methods.md`——跨域引用）；quality `state-machine.md`；crm README。
- **MA2 已审计的已知 finding（代码质量审计输入，非重复审计）**：A2.8 purchase 状态机（P1-MA2-049 Quotation/Rfq reverseApprove→SUBMITTED 契约漂移 / P1-MA2-050 INLINE reject/withdrawApproval 缺 isCancelled 守卫致副轴漂移）；A2.9 sales 状态机（P1-MA2-056 Contract reverseApprove→SUBMITTED / P1-MA2-057 INLINE withdrawApproval + Contract 全 INLINE 缺守卫）；A2.11 inventory 状态机（stockMove 双轴 + 批次/序列号/预留）；A2.12 quality 状态机（P0-MA2-017 inspection 状态守卫已修复）；A2.14 crm 子集（Lead/Ticket 状态机）；A2.1 P2P 端到端（P1-MA2-001 暂估冲回缺失 / P1-MA2-002 多币种 P2P 本位币凭证路径未验证 / P1-MA2-003 付款核销缺三单匹配完成态复核）；A2.2 O2C 端到端（P1-MA2-009 多币种 O2C + 收款核销汇兑损益未实现）；A2.4 库存核算一致性（P1-MA2-023 SPECIFIC 历史成本守卫缺失 / P1-MA2-024 STANDARD 红冲成本不变量跨重估破缺）；A2.17 并发审计（P0-MA2-020 inv stock balance UK 已修复 + 全域 @Version 覆盖）；A2.18 多公司审计（orgId 隔离/账套隔离）。
- **MA1 已审计的已知 finding**：A1.4 pur+sal ORM（P1-MA1-009 crm DECIMAL↔Double）；A1.5 assets+inv ORM；A1.6 crm+quality+projects ORM；P1-MA1-022（五域跨域只读 daoFor——pur/sal/ast/inv A1.12 + crm A1.13 扩展投影）；P0-MA1-021（inv CostAdjustmentPostingDispatcher 跨域写 ErpFinVoucher 已修复）。
- **MA3 已审计的已知 finding**：A3.5 pur+sal+inv owner doc vs 代码 drift（0 P1 + 2 P2 minor——三域核心机制文档与代码高度一致，StockTake COUNTING vs CONFIRMED + 冲销反向移动取负 vs code 翻转 moveType）；A3.6 API 契约一致性（五域投影）。

**审计张力**：MA2 审计了五域链路的**业务正确性**（状态机/端到端/并发），并已发现 P0（P0-MA1-021 / P0-MA2-017 / P0-MA2-020 已修复）+ 多项 P1，但**代码实现质量**（架构边界 / 核心实现正确性 / 类型与契约 / 错误处理 / 测试有效性 / 可维护性 / 自动化防护）是 MA4 的独立维度。本审计聚焦 MA2 未覆盖的代码质量维度：PaymentSettler 核销算术与三单匹配门控（复核 P1-MA2-003）/ 三单匹配 ThreeWayMatchService 匹配算法正确性 / 过账 Provider（PurAcctDocProvider/SalAcctDocProvider）多币种凭证路径（复核 P1-MA2-002/009 VoucherFact 单一 amount 字段）/ StockMoveBookkeeper upsertBalance 并发安全（复核 P0-MA2-020 UK 修复后的 retry 路径）/ 成本方法解析与调整算术（复核 P1-MA2-023/024）/ NCR 过账 + 退货编排跨域 Facade 错误传播 / SpcCalculator 统计算术正确性 / crm DECIMAL↔Double 类型安全（复核 P1-MA1-009）/ 跨域 daoFor 投影（复核 P1-MA1-022）/ 测试异常路径覆盖。

剩余差距：需要一次 pur+sal+inv+qa+crm 五域代码实现质量抽样审计（A 级合并——抽样重点链路非逐行审查）。发现的缺陷分类为：(a) **架构边界违规**（major）；(b) **核心实现正确性**（major/blocker——核销/成本/匹配/统计算术错误 / 事务边界 / 异常悬挂）；(c) **错误处理与操作安全**（major）；(d) **测试有效性**（major）；(e) **可维护性风险**（P2）。blocker/major 登记为 P1（代码类目标 MR2；业务正确性类目标 MR1）。若发现活跃数据破坏路径，升级标注走 P0 即时通道。

## Goals

- 按 `code-quality-audit-prompt.md` 7 重点领域对 pur+sal+inv+qa+crm 五域代码做抽样实现质量审计（A 级合并——按链路抽样重点组件非逐域逐行），产出审计报告。
- 审计覆盖重点链路：purchase（六 Processor 审批轴 + PaymentSettler 核销 + ThreeWayMatchService + PurAcctDocProvider）/ sales（六 Processor + SalAcctDocProvider + DeliveryStockMoveBuilder）/ inventory（StockMoveBookkeeper + 成本方法解析/调整链 + ErpInvLandedCostProcessor + 所有权转移）/ quality（InspectionBizModel 状态守卫 + NcrPostingDispatcher/ReturnOrchestrator + InspectionTrigger + SpcCalculator）/ crm（Lead 评分 + Forecast + PriceRule + FunnelStageMetrics 类型安全）。
- 复核 MA1/MA2/MA3 已知 finding（A2.8/9/11/12/14 + A2.1/2/4 + A2.17/18 + A1.4/5/6 + P1-MA1-009/022 + P0-MA1-021/MA2-017/020 已修复 + A3.5/3.6）的运行时状态，标记是否有 MA2 未发现的代码层缺陷。重点复核多币种凭证路径（P1-MA2-002/009）与成本算术（P1-MA2-023/024）。
- scope matrix §2.4「代码质量（MA4）」行 pur/sal/inv/qa/crm 维度推进至完成。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（起始编号 = A4.4 已分配最大 P1-MA4-N + 1，避免命名空间碰撞）。roadmap A4.5 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做五域状态机/业务正确性审计 — 归 A2.8/9/11/12/14（已 done）。本审计聚焦**代码实现质量**，MA2 已知 finding 作为输入复核而非重复审计。
- **不**做五域 view.xml vs 后端契约 drift — 归 A4.7（pur+sal+inv view drift）。
- **不**做 owner doc vs 代码 drift — 归 A3.5（已 done）。本审计的 owner doc drift 复核以 A3.5 已登记 finding 为输入。
- **不**做 finance 侧过账引擎实现质量（PurAcctDocProvider/SalAcctDocProvider 经 IErpFinVoucherBiz Facade，其 Facade 实现质量归 A4.1a/b）——本审计复核 purchase/sales 侧过账调用点的错误传播与多币种凭证装配。
- **不**做测试覆盖深度统计 — 归 MA5（测试层）。
- **不**做权限注解完整性 — 归 A6.1/A6.2（MA6 安全层）。
- **不**做 hr 域代码质量 — 归 A4.4（独立 S 级计划）。
- **不**在本计划内批量修复代码缺陷 — P1 经 R2.0/R1.0 展开机制进入 MR2/MR1。本审计只识别缺陷 + 分类。
- **不**手改生成物或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/purchase/state-machine.md` + `three-way-match.md` + `returns.md`；`docs/design/sales/state-machine.md` + `use-cases.md` + `returns.md`；`docs/design/inventory/state-machine.md`（库存成本方法见 `docs/design/finance/costing-methods.md`——跨域引用）；`docs/design/quality/state-machine.md`；crm README；`module-{purchase,sales,inventory,quality,crm}/erp-*-service/`（五域业务逻辑代码实现——审计对象）
- Skill Selection Basis: `code-quality-audit-prompt.md`（roadmap A4.5 指定此 skill——7 重点领域 + 严重性指南 P0-P3）。与 A2.8/9/11/12/14 不同维度（代码实现质量 vs 业务正确性状态机），互补不重叠。A 级合并——scope matrix §2.4「代码质量（MA4）」行声明"A 级合并"（机械维度以外的行为维度 A 级允许 2+ 域合并抽样）。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。代码缺陷修复在 MR2/MR1 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为代码静态审查 + 测试有效性抽样，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。代码静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 五域代码实现质量抽样审计（7 重点领域，按链路抽样）

Status: planned
Targets: purchase（ErpPurOrder/Receive/Invoice/Payment/Return/Requisition Processor + PaymentSettler + ThreeWayMatchService + PurAcctDocProvider）/ sales（ErpSalOrder/Delivery/Invoice/Receipt/Return/Quotation Processor + SalAcctDocProvider + DeliveryStockMoveBuilder）/ inventory（StockMoveBookkeeper + StandardCostResolver/CostMethodResolver/CostAdjustmentService + ErpInvLandedCostProcessor + ErpInvOwnershipTransferProcessor + ErpInvStockTakeProcessor + ErpInvPickListProcessor）/ quality（ErpQaInspectionBizModel + NcrPostingDispatcher/NcrReturnOrchestrator + InspectionTrigger + SpcCalculator）/ crm（ErpCrmLeadBizModel + ErpCrmForecastBizModel + ErpCrmPriceRuleBizModel + ErpCrmFunnelStageMetricsBizModel）；owner docs 各域 state-machine.md + three-way-match.md + returns.md（库存成本方法见 `docs/design/finance/costing-methods.md`）
Skill: `code-quality-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 + MA2 + MA3 done（已知 finding 作为输入）；A2.8/9/11/12/14 done（状态机基线）；A2.1/2/4 done（端到端基线）；A2.17/18 done（并发/多公司基线）；A4.1a/b done（MA4 过账 Facade 范式参照）。**注**：A4.4（hr 代码质量）与本计划为独立审计工作项（读不同域代码），无执行依赖；P1-MA4 命名空间延续在 EXECUTE 时按"已分配最大 P1-MA4-N + 1"动态确定，不硬阻塞于 A4.4 完成。

- [ ] 领域「架构和边界完整性」：核查五域代码的跨域访问合规性——过账 Provider 是否经 IErpFinVoucherBiz Facade 过账（非 daoFor 直写凭证——P0-MA1-021 已修复复核）/ 跨域只读 daoFor 投影（复核 P1-MA1-022 五域站点：pur/sal Processor ErpMdSubject/ErpFinAccountingPeriod + inv ErpPurReceive + qa NcrDispatcher ErpInvStockBalance + crm Dashboard facade）/ NcrReturnOrchestrator 跨域建 ErpPurReturn 是否经 I*Biz（复核 contract P1-MA1-029 同型）。标记边界违规站点。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「核心实现正确性」：核查 PaymentSettler 核销算术与三单匹配门控（复核 P1-MA2-003）/ ThreeWayMatchService 匹配算法正确性（PO-Receive-Invoice 三方数量/价格/容差）/ 过账 Provider 多币种凭证路径（复核 P1-MA2-002/009 VoucherFact 单一 amount 字段——source-currency 直接写入）/ StockMoveBookkeeper upsertBalance 并发安全（复核 P0-MA2-020 UK 修复后的 ConstraintViolation→reload+retry 路径）/ 成本方法解析与调整算术（复核 P1-MA2-023 SPECIFIC 历史成本守卫 + P1-MA2-024 STANDARD 红冲成本不变量）/ ErpInvLandedCostProcessor 分摊算术 / NcrPostingDispatcher 过账异常吞咽悬挂（复核 hr P1-MA2-048 / assets P1-MA2-060 同型根因在 quality 侧）/ SpcCalculator 统计过程控制算术正确性（控制限/工序能力指数）。标记算术错误/事务/幂等/异常悬挂缺陷。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「类型和契约质量」：核查 crm DECIMAL↔Double 类型安全（复核 P1-MA1-009 ForecastAccuracy/PriceRule/LeadFunnel/FunnelStageMetrics 7 列浮点精度损失——参与比率计算）/ 过账金额 BigDecimal 类型安全 / 六 Processor 审批轴参数返回契约一致性 / INLINE vs Processor 路径契约漂移（复核 P1-MA2-050/057）。标记类型不匹配/契约漂移。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「错误处理和操作安全」：核查五域代码异常是否全部扩展 NopException + ErrorCode（`erp.err.pur.*` / `erp.err.sal.*` / `erp.err.inv.*` / `erp.err.qa.*` / `erp.err.crm.*`）/ 过账失败/核销超额/三单匹配容差/库存负数/质检不合格的错误传播 / 批量操作部分失败告警闭环。标记裸异常/ErrorCode 缺失。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「测试有效性」：抽样五域测试，核查**异常路径覆盖**（多币种凭证路径 P1-MA2-002/009 零 E2E 证据 / 三单匹配容差边界 / SPECIFIC 历史成本守卫 / STANDARD 红冲不变量 / 到岸成本分摊精度 / NCR 过账悬挂 / SpcCalculator 控制限边界）+ 断言强度。标记测试空洞。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「可维护性和未来变更风险」：核查六 Processor 审批轴重复模式对称性（pur/sal 各 6 实体）/ 成本方法策略可扩展性 / crm DECIMAL↔Double 跨域影响范围。标记 P2 可维护性风险。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「自动化和防护覆盖」：核查五域代码是否有 compliance checker 规则守护（R8/R2）/ 是否有测试门控防止回归（核销算术/成本算术/匹配算法）。标记防护缺口。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 产出审计报告 `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（含：7 领域逐项审查结果 / MA1/MA2/MA3 已知 finding 运行时复核 / P0-P3 finding 清单按严重性排序 / 每项含文件路径+行引用 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 7 重点领域逐项审查结果产出（每领域至少一句裁决，含"本领域无缺陷"）
- [ ] MA1/MA2/MA3 已知 finding 运行时复核产出（每项标记"如 owner doc 声明"或"发现新代码层缺陷"）
- [ ] P0-P3 finding 清单产出按严重性排序，每个含文件路径+行引用+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2/MR1 + 索引/矩阵更新

Status: planned
Targets: 五域代码质量 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] finding 汇总：全部缺陷 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = A4.4 已分配最大 P1-MA4-N + 1，避免命名空间碰撞；报告、领域、缺陷描述、目标 MR2[代码类]/MR1[业务正确性类]、修复状态 todo）。与 MA1/MA2/MA3/A4.1a-A4.4 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [ ] 分类裁决：代码实现质量 finding 目标 MR2；业务正确性类 finding 目标 MR1；活跃数据破坏走 P0 即时通道（成本/核销算术错误直接影响财务报表，升级评估优先），在报告中明确标注。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「代码质量（MA4）」行反映 pur/sal/inv/qa/crm 维度进度。
      - Skill: none

Exit Criteria:

- [ ] 所有缺陷 blocker/major 已登记 arm-index §P1 汇总（代码类 MR2 / 业务正确性类 MR1），待展开
- [ ] 与 MA1/MA2/MA3/A4.1a-A4.4 已登记 P1 经交叉去重无重复登记
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0561bad97ffey3Rf61DbOwCgN3`，独立 general 子代理 fresh-context）——BLOCKER：Rule 1 违规，两个 owner doc 路径不存在：(1) `docs/design/sales/delivery.md` 不存在（sales/ 仅有 contract/quotation/README/returns/state-machine/ui-patterns/use-cases）；(2) `docs/design/inventory/costing-methods.md` 不存在（实际为 `docs/design/finance/costing-methods.md`）。SUGGESTION：A4.4 硬前置序列化两个独立审计，建议 P1-MA4 命名空间在 EXECUTE 时动态确定。修订：全文移除 delivery.md 引用 + costing-methods.md 重定位至 finance/ + 软化 A4.4 前置。
- Independent draft review iteration 2: **needs revision**（`ses_05616e390ffeXsQvsD4MUUyAfv`，独立 general 子代理 fresh-context）——header/owner-docs-bullet 已修复，但 Task Route Owner Docs（line 53）+ Phase 1 Targets（line 67）两处残留 delivery.md + inventory/costing-methods.md 引用未清除。
- Independent draft review iteration 3: **accept**（修订后 grep 确认：`sales/delivery`=0 hits + `inventory/costing-methods`=0 hits + 纯 `delivery.md`=0 hits；`finance/costing-methods.md` + sales state-machine/use-cases/returns 实仓存在；A4.4 前置软化为"独立审计无执行依赖，P1-MA4 命名空间 EXECUTE 时动态确定"）。全部 BLOCKER 已清除，Plan 结构完好。Plan Status 转 active。

## Closure Gates

> 本计划主体是代码静态审查 + 测试有效性抽样（不改代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。代码缺陷修复在 MR2/MR1 批量进行；活跃数据破坏走 P0 即时通道。本审计只识别缺陷 + 分类。

- [ ] 范围内行为完成（A4.5 五域代码质量抽样审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [ ] 已运行验证：代码静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 五域 view.xml drift（A4.7）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端 view.xml 调用的 API/字段 vs 后端契约 drift 归 A4.7（pur+sal+inv view drift 批次）。本审计审后端代码实现质量。
- Successor Required: `yes`——A4.7 执行时复核 pur/sal/inv view；crm view 归 A4.8。

### 测试覆盖深度统计（MA5）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计"测试有效性"维度审异常路径覆盖 + 断言强度；覆盖深度统计归 MA5（测试层）。
- Successor Required: `yes`——MA5 执行时复核五域测试深度。

### 业务正确性/状态机（A2.8/9/11/12/14）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审代码**实现质量**（算术/事务/异常/类型/测试）；五域状态机业务正确性归 A2.8/9/11/12/14（已 done）。MA2 已知 finding 作为本审计输入复核。
- Successor Required: `no`——A2.8/9/11/12/14 已 done。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计后填写>
- Evidence: <待独立结束审计后填写>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
