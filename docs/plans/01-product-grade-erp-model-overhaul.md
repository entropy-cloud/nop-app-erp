# 01-product-grade-erp-model-overhaul 产品化 ERP 数据库设计完善 + 架构对标文档

> Plan Status: draft
> Last Reviewed: 2026-06-22
> Source: 用户请求（完善文档、彻底实现通用产品化 ERP 设计、完成数据库设计、产出超越调研竞品的对标文档）
> Related: 无前序计划（bootstrap 阶段）
> Audit: required

## Current Baseline

**已有资产：**

- 10 份 `<domain>/model/app-erp-<domain>.orm.xml`，共 **85 实体**（master-data 11 / purchase 10 / sales 10 / inventory 10 / finance 8 / assets 7 / manufacturing 9 / projects 6 / maintenance 8 / quality 9，按文件逐个计数）。
- 完整目录式设计文档树（`docs/design/<domain>/README + state-machine + 跨域协作`）+ 全局文档（app-overview / flow-overview / domain-design-guidelines / domain-glossary / roles-and-permissions / feature-inventory / erp-design-audit-checklist）。
- 架构文档：project-vision / system-baseline / module-boundaries / domain-module-split-analysis / customization-capabilities / api-response-conventions / integration-and-transaction-patterns。

**已审计的关键差距（带文件:行证据）：**

1. **业务单据无 `posted` 标志**（文档声称有）。`posted` 仅存在于 finance 的 `ErpFinVoucher`（finance.orm.xml:99-100），purchase/sales/inventory/assets/projects/maintenance/quality 的 orm 中**一个 `posted` 列都没有**。但 `erp-design-audit-checklist.md:18,191`、`flow-overview.md:52,70,94`、`domain-design-guidelines.md:187`、`finance/posting.md:27,145` 都声称业务单据有 `posted` + 兜底扫描。**文档与模型漂移。**
2. **无 `orgId` 多组织维度**。所有业务单据都没有 org/organization 字段（仅 finance 凭证行有 `departmentId`）。但 audit-checklist:95,228 与 guidelines:254 声称有 `orgId`。
3. **库存单据无业务日期**。`ErpInvStockMove`、`ErpInvTransferOrder`、`ErpInvStockTake` 只有 `createdAt/updatedAt`，无 `moveDate/transferDate/takeDate`。
4. **财务缺多套账与多币种完整支持**：`ErpFinVoucherLine` 无 `acctSchemaId`（多套账不可行）、无 `exchangeRate`、无 `amountFunctional`（finance.orm.xml:118-135）。posting.md:196-201 声称"多套科目表并行"，模型不支持。
5. **无 AR/AP 明细账/核销账**（finance 无 `ErpFinArApItem` / `ErpFinReconciliation`）；往来余额靠 `ErpMdPartner.receivableBalance/payableBalance` 字段"魔法更新"。
6. **制造域缺核心对象**：无 MRP、无 `WorkOrderLine`（WorkOrder 只有单 productId）、无 `ProductionVersion`、无领料单、无 `JobCardTimeLog`（state-machine.md:174 引用）。
7. **库存缺预留单实体**：`ErpInvStockBalance.reservedQuantity` 只是计数器，无记录级预留。
8. **主数据缺**：结算方式、UoM 换算、税率主数据、银行账户主数据、员工/职员主数据（Partner 被重载为客户+供应商+员工+经理）。
9. **无记录级预留/锁**，**无 picking/packing**，**无 cost method 字段**（avgCost 硬编码）。
10. **资产/项目/维护单据缺 `posted`、缺 `code`（ValueAdjustment/Disposal 无 code）、缺币种**。
11. **质保/校准/抽样方案（AQL）**等质量深度对象缺失。
12. **损坏链接**：`docs/design/README.md:75` 指向 `../nop-entropy/...`，实际兄弟目录是 `nop-entropy-wt`。

**竞品对标研究结论（用于"超越"论证）：**

- Odoo（LGPL CE）：代码优先 + ORM，定制改源码耦合，**无 CE 官方升级工具**，无多套账，无 LIFO/具体辨认。最大痛点=升级脆弱。
- ERPNext（GPL）：DocType 元数据部分 DB 存储，Property Setter 可升级，但深度 class override 仍会丢；真多租户；无多套账。
- iDempiere/metasfresh/Openbravo（GPL，Compiere AD 家族）：**真模型驱动 + Application Dictionary + 2Pack/OSGi delta**，**真多套账**，多 Client/Org。弱点：老旧 Java/UX、小社区、Openbravo 已闭源。
- Tryton（GPL）：代码优先但模块化纪律严、**升级稳定性最强**；多公司 MultiValue 一流；多套账弱。
- MixERP：ASP.NET WebForms，**基本停摆**。
- 关键"超越"杠杆 = (A) 真模型驱动 + 真 Delta 升级层，(B) 原生多套账，(C) 多租户 + 多公司并存，(D) 业财一体三件套，(E) 完整成本方法集，(F) 制造全链。

## Goals

1. **10 份 orm.xml 升级到产品级基线**：补齐所有审计差距，使模型真正支撑设计文档声称的能力（posted/orgId/多套账/AR-AP/MRP/预留/补主数据等），目标实体总数从 85 增至约 120+。
2. **文档与模型一致性修复**：消除所有"文档声称、模型没有"的漂移；修正损坏链接；audit-checklist 的完成度声明对齐真实状态。
3. **新增架构对标文档** `docs/architecture/competitive-comparison.md`，完整矩阵 + 逐杠杆深度分析，论证 nop-app-erp 在哪些维度超越 7 个调研竞品，证据来自真实模型字段/实体。

## Non-Goals

- 不做 `nop-cli gen` 代码生成、不构建 Java 工程、不跑测试套件（项目仍处于 pre-codegen 阶段，验证命令不可执行，按 AGENTS.md 约定）。
- 不写 BizModel/xbiz/view（本阶段只交付**模型 + 设计/架构文档**）。
- 不引入租户字段到 orm.xml（按 project-vision.md 与 system-baseline.md：租户隔离走平台标准，不在源模型预置 `tenantId`）。
- 不做中国本地化 l10n 模块（audit-checklist 已列为低优先级延迟项）。
- 不改动 nop-entropy 平台本身。

## Task Route

- Type: `architecture change` + `app-layer design change`（数据模型是持久化契约真相源，属架构变更）。
- Owner Docs: `docs/architecture/project-vision.md`、`docs/architecture/customization-capabilities.md`、`docs/architecture/domain-module-split-analysis.md`、`docs/design/domain-design-guidelines.md`、`docs/design/flow-overview.md`、`docs/design/<domain>/README.md`、各 `<domain>/model/*.orm.xml`。
- Skill Selection Basis: 此工作是纯 ORM 模型设计 + 文档，不涉及 BizModel/view/codegen 执行；`docs/skills/` 现有技能（design-doc-audit / design-completeness-scan）用于结束审计阶段。模型字段设计遵循 `../nop-entropy-wt/nop-entropy-master/docs-for-ai/02-core-guides/orm-model-design.md`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本阶段无代码、无构建、无外部服务。

## Execution Plan

### Phase 1 - 跨切面模型基线（公共字段与字典）

Status: planned
Targets: `master-data/model/app-erp-master-data.orm.xml`（补充公共主数据），并定义将在所有域复用的公共字段约定
Skill: none

- Item Types: `Add | Decision`
- Prereqs: 无

- [ ] `Add`：master-data 补齐缺失主数据实体——`ErpMdSettlementMethod`（结算方式）、`ErpMdUoMConversion`（单位换算）、`ErpMdTaxRate`（税率主数据）、`ErpMdBankAccount`（银行账户）、`ErpMdEmployee`（职员）、`ErpMdPartnerAddress`/`ErpMdPartnerContact`（Partner 多地址多联系人子表）。
  - Skill: none
- [ ] `Add`：master-data 补 `ErpMdAcctSchema`（会计核算表/账套主数据）+ `ErpMdAcctSchemaCoa`（账套 × 科目表关联），支撑多套账。
  - Skill: none
- [ ] `Decision`：公共维度字段统一约定（写入 domain-design-guidelines.md 的"单据标准字段"小节）：所有业务单据头加 `orgId`（业务组织）、`businessDate`（业务日期）、`posted`（业财过账标志）、`postedAt/postedBy`，保留 `version` 乐观锁。备选（被否）：用扩展字段承载——orgId/posted/businessDate 是所有单据的标准维度，物理列查询性能与索引远优于 EAV。
  - Skill: none
- [ ] `Decision`：多币种标准列对——`currencyId` + `exchangeRate` + `amountSource`（源币种金额）+ `amountFunctional`（本位币金额）在所有金额类单据头/行统一。
  - Skill: none

Exit Criteria:

- [ ] master-data orm.xml 新增 ≥8 个实体且 XML well-formed（xmllint --noout 通过）
- [ ] 公共字段约定写入 domain-design-guidelines.md

### Phase 2 - 库存域升级（三层模型 + 预留 + 成本方法）

Status: planned
Targets: `inventory/model/app-erp-inventory.orm.xml`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] `Fix`：`ErpInvStockMove`、`ErpInvTransferOrder`、`ErpInvStockTake` 加 `businessDate`、`orgId`、`posted`、`postedAt/postedBy`。
- [ ] `Add`：`ErpInvReservation`（库存预留单头/行），让 `StockBalance.reservedQuantity` 有明细载体。
- [ ] `Add`：`ErpInvCostLayer`（成本层，支撑 FIFO/批次核算）+ 在 Material/StockBalance 加 `costMethod` 字段。
- [ ] `Add`：`ErpInvPickingOrder`（拣货单头/行）。
- [ ] `Fix`：`ErpInvBatch.status`、`ErpInvSerialNumber.status` 改为字典 `erp-inv/batch-status` / `erp-inv/serial-status`。
- [ ] `Add`：`ErpInvStockLedger` 加 `orgId`、`costMethod`、`acctSchemaId`。

Exit Criteria:

- [ ] inventory orm.xml 新增 ≥3 实体，三类单据均有 businessDate/orgId/posted，XML well-formed
- [ ] `docs/design/inventory/README.md` 与 `cross-domain.md` 与模型一致

### Phase 3 - 财务域升级（多套账 + 多币种 + AR/AP + 核销）

Status: planned
Targets: `finance/model/app-erp-finance.orm.xml`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] `Fix`：`ErpFinVoucherLine` 加 `acctSchemaId`、`exchangeRate`、`amountFunctional`、`orgId`。
- [ ] `Fix`：`ErpFinVoucher` 加 `orgId`、`acctSchemaId`。
- [ ] `Add`：`ErpFinArApItem`（应收应付明细账 open-item）。
- [ ] `Add`：`ErpFinReconciliation` + `ErpFinReconciliationLine`（核销单头/行）。
- [ ] `Add`：`ErpFinGlBalance`（总账余额快照）+ `ErpFinTrialBalance`（试算平衡表快照）。
- [ ] `Fix`：`ErpFinAccountingPeriodStatus` 加按模块结账状态字段（AR/AP/INV/GL）。
- [ ] `Fix`：`ErpFinFundAccount` 与付款单的 `bankAccountId` 建立引用。

Exit Criteria:

- [ ] finance orm.xml 新增 ≥4 实体，VoucherLine 含 acctSchemaId/exchangeRate/amountFunctional
- [ ] `docs/design/finance/README.md`、`posting.md` 与模型一致

### Phase 4 - 采购/销售域升级（上游单据 + posted + orgId）

Status: planned
Targets: `purchase/model/app-erp-purchase.orm.xml`、`sales/model/app-erp-sales.orm.xml`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] `Fix`：所有采购/销售单据头加 `orgId`、`posted`、`postedAt/postedBy`、`bankAccountId`→引用 ErpMdBankAccount。
- [ ] `Add`：purchase 新增 `ErpPurRequisition`（请购单）头/行、`ErpPurQuotation`/`ErpPurRfq`（询价/报价）头/行、`ErpPurSupplierPriceList`（供应商价格清单）。
- [ ] `Add`：sales 新增 `ErpSalQuotation`（销售报价）头/行、`ErpSalContract`（销售合同）。
- [ ] `Fix`：`ErpPurPayment` 加 `paidStatus`/`writtenOffStatus`；`ErpPurReceive` 头加 `receiveStatus`；sales Order 加 `deliveryStatus`。

Exit Criteria:

- [ ] purchase/sales orm.xml 各新增 ≥2 实体，所有单据含 orgId/posted，XML well-formed
- [ ] `docs/design/purchase/README.md`、`sales/README.md` 与模型一致

### Phase 5 - 制造域升级（MRP + 工单行 + 生产版本 + 领料 + 工时）

Status: planned
Targets: `manufacturing/model/app-erp-manufacturing.orm.xml`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1

- [ ] `Add`：`ErpMfgWorkOrderLine`（工单多产出/多投入行）。
- [ ] `Add`：`ErpMfgProductionVersion`（生产版本）。
- [ ] `Add`：`ErpMfgMrpPlan` + `ErpMfgMrpPlanLine` + `ErpMfgMrpDemand`（MRP）。
- [ ] `Add`：`ErpMfgMaterialIssue`（领料单头/行）。
- [ ] `Add`：`ErpMfgJobCardTimeLog`（作业员工时记录）。
- [ ] `Add`：`ErpMfgSubcontractOrder`（委外加工单头/行）。
- [ ] `Add`：`ErpMfgCostRollup`（标准成本滚算头/行）。
- [ ] `Fix`：WorkOrder 加 `orgId`、`routingId`、`posted`、成本字段。

Exit Criteria:

- [ ] manufacturing orm.xml 新增 ≥5 实体，state-machine.md / bom-and-routing.md 引用的概念均有对应实体
- [ ] `docs/design/manufacturing/` 与模型一致

### Phase 6 - 资产/项目/维护/质量域升级

Status: planned
Targets: `assets|projects|maintenance|quality/model/*.orm.xml`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] `Fix`：四域所有单据加 `orgId`、`posted`、`postedAt/postedBy`；资产/项目加 `currencyId/exchangeRate`。
- [ ] `Fix`：assets `ValueAdjustment`/`Disposal` 加 `code`；`ErpAstMovement` 加 docStatus/approveStatus。
- [ ] `Add`：assets `ErpAstCip`（在建工程）、`ErpAstSplit`/`ErpAstMerge`。
- [ ] `Add`：projects `ErpPrjBudget`、`ErpPrjCostCollection`、`ErpPrjMilestone`、`ErpPrjBilling`。
- [ ] `Fix`：maintenance `ErpMntEquipment.assetId` 补 declared 关系；新增 `ErpMntSparePartUsage`、`ErpMntMaintenanceTeamMember`、`ErpMntCalibration`。
- [ ] `Fix`：quality `ErpQaInspection` 补 to-one 关系；`QualityGoal.targetValue/currentValue` 改 numeric；新增 `ErpQaSamplingPlan`、`ErpQaCalibration`。

Exit Criteria:

- [ ] 四域 orm.xml 所有单据含 orgId/posted；新增 ≥6 实体；XML well-formed
- [ ] 各域 README + 跨域集成文档与模型一致

### Phase 7 - 文档一致性修复 + audit-checklist 重对齐

Status: planned
Targets: `docs/design/erp-design-audit-checklist.md`、`docs/design/flow-overview.md`、`docs/design/domain-design-guidelines.md`、`docs/design/finance/posting.md`、`docs/design/inventory/cross-domain.md`、`docs/design/README.md`、`docs/architecture/customization-capabilities.md`、`docs/requirements/product-scope.md`、各域 README
Skill: `design-doc-audit-prompt`

- Item Types: `Fix | Add`
- Prereqs: Phase 1-6

- [ ] `Fix`：修正 `docs/design/README.md` 损坏链接（`../nop-entropy/` → 不依赖兄弟目录路径）。
- [ ] `Fix`：重写 `erp-design-audit-checklist.md` 的"已完成项/完成度表"，使 ✅ 与真实模型状态一致。
- [ ] `Fix`：`product-scope.md` 实体计数从"88 实体"更新为最终真实数。
- [ ] `Fix`：所有 README 的"核心实体"列表与 orm.xml 实体清单一致。
- [ ] `Add`：`domain-design-guidelines.md` 新增"单据标准字段约定"小节。

Exit Criteria:

- [ ] audit-checklist 的每一条 ✅ 都能在 orm.xml 中找到对应字段/实体
- [ ] 无损坏内部链接（grep `../nop-entropy/` 无残留误引）

### Phase 8 - 架构对标文档 `competitive-comparison.md`

Status: planned
Targets: `docs/architecture/competitive-comparison.md`（新建）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-6（需要真实模型作为证据）

- [ ] `Add`：撰写完整架构对标文档（调研对象 + 总览矩阵 + 8 个杠杆深度分析 + "在哪些地方超越什么"汇总表 + 诚实声明）。
- [ ] `Add`：在 `docs/architecture/README.md` 与 `docs/index.md` 注册新文档链接。

Exit Criteria:

- [ ] competitive-comparison.md 每条"超越"声明都有可点击的模型证据（文件:实体/字段）
- [ ] 文档含诚实声明节，不夸大

## Draft Review Record

- Independent draft review iteration 1: 待执行（计划写入后由独立子代理审查）

## Closure Gates

> 本计划无代码变更，验证命令门控按"仅文档计划"调整。

- [ ] 范围内行为完成：10 orm.xml well-formed + 文档一致 + 对标文档完成
- [ ] 相关文档对齐（audit-checklist ✅ 与模型逐条一致）
- [ ] 已运行验证：所有变更后的 orm.xml 通过 `xmllint --noout` well-formed 校验；grep 验证损坏链接清零
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status / 阶段 Status / Exit Criteria / Closure Gates / 日志 一致
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 中国本地化 l10n-cn 模块

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: audit-checklist 已列为低优先级；本计划聚焦通用产品基线
- Successor Required: yes（当首个国内客户交付时）

### nop-cli 代码生成与端到端业务循环验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 项目处于 pre-codegen 阶段（AGENTS.md 明示验证命令暂不可执行）；本计划交付模型 + 文档
- Successor Required: yes（下一个计划：首域 codegen + 采购→入库→应付→凭证 端到端）

### SaaS 多租户启用

- Classification: `watch-only residual`
- Why Not Blocking Closure: 需业务决策（project-vision.md 列为人工决策点）；租户隔离走平台标准不在 orm 预置 tenantId
- Successor Required: yes（当确认启用 SaaS 时）

## Closure

Status Note: 待执行结束后填写。

Closure Audit Evidence:

- Auditor / Agent: 待填
- Evidence: 待填

Follow-up:

- 首域 codegen + 端到端业务循环验证（见 Deferred）
- 中国本地化 l10n-cn 模块（见 Deferred）
