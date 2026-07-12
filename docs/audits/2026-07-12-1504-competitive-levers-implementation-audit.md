# 竞争杠杆兑现审计：8 个"超越点"实现层核实（vs erp-survey 标杆）

**审计日期**：2026-07-12 15:04
**审计类型**：实现层兑现审计（承诺功能/优势 vs 已落地代码）
**审计方法**：3 路并行子代理（杠杆 B/E/D、F/C、A/G/H）→ 主代理自核实关键存疑项（Maven 依赖环、Delta 使用、实体计数、委外空壳）→ 定稿
**审计对象**：`docs/architecture/competitive-comparison.md` §四 的 8 个杠杆声明（A–H）
**兑现标准**：模型（orm.xml）+ 服务逻辑（Java/BizModel，非 CRUD 骨架）+ 测试（JUnit / Playwright E2E）三层齐备
**仓库快照**：`HEAD = 6d34e665`（feat(erp): 落地全部7种成本方法+多套账并行传播+跨账套科目映射）
**调研来源**：`docs/analysis/erp-survey/`（16+ 开源 ERP：Odoo/ERPNext/iDempiere/metasfresh/Tryton/Axelor/管伊佳/赤龙 等）

---

## 执行摘要

`competitive-comparison.md` §四提出 8 个相对主流开源 ERP 的"超越点"（杠杆 A–H）。本次审计逐一核实**实现层**兑现情况，结论：

- **4 个完整兑现**（B/D/E/H）：模型 + 服务逻辑 + 测试三层齐备，是真正超越 Odoo/ERPNext 的产品能力。
- **2 个大部分兑现、各有 1 处明确缺口**（A/F）：能力落地，但文档数据错误（A）或子能力空壳（F 委外）。
- **2 个机制兑现但措辞夸大 / 行为层缺失**（C/G）：结构地基完整，但"多公司行为"（C）与"域可独立部署"（G）言过其实。

发现问题：**2 个高（H-级）**、**4 个中（M-级）**、**若干低（L-级）**。核心风险集中在文档诚信（competitive-comparison.md 存在可核实的错误陈述），而非产品能力本身。

> **重要背景**：本审计对象是 `nop-app-erp`（应用层）。其中杠杆 A（模型驱动 + Delta）在很大程度上是**继承自 Nop 平台**的能力，而非本项目发明。文档若能区分"平台可支撑"与"本项目已演示"，将显著提升可信度。

---

## 一、逐杠杆兑现结论

| 杠杆 | 承诺 | erp-survey 标杆 | 模型 | 服务逻辑 | 测试 | 裁决 |
|---|---|---|:---:|:---:|:---:|---|
| **B 原生多套账** | acctSchema + 并行传播 + 跨账套科目映射 | iDempiere / metasfresh | ✅ | ✅ | ⚠️ | **完整兑现** |
| **E 7 种成本方法** | 7 法 + CostLayer 多层 | iDempiere / Odoo | ✅ | ✅ | ⚠️ | **完整兑现** |
| **D 业财一体 + posted 兜底扫描** | 三件套 + 模板引擎 + 定时补扫 | ERPNext on_submit / metasfresh EventBus | ✅ | ✅ | ✅ | **完整兑现** |
| **H AR/AP open-item 核销** | ArApItem + 核销引擎 + 汇兑损益 | —（本项目原创） | ✅ | ✅ | ✅ | **完整兑现**（1 处次要缺口） |
| **A 模型驱动 + Delta** | orm.xml 唯一真相 + Delta 层 | Compiere 家族 / Tryton | ✅ | ✅ | n/a | **兑现（文档数据错误）** |
| **F 制造全链** | MRP + 生产版本 + 领料 + 委外 + 工时 + 成本滚算 | Odoo / Axelor | ✅ | ⚠️ | ✅ | **大部分兑现（委外空壳）** |
| **G 跨域 DAG + I*Biz 解耦** | 无 ORM 循环 + 可独立部署 | 单体 ERP | ✅ | ✅ | n/a | **机制兑现，措辞夸大** |
| **C 多组织 / 多公司** | orgId 物理列 + 组织树 | iDempiere / Tryton | ✅ | ❌ | ❌ | **结构兑现，行为缺失** |

图例：✅ 兑现 / ⚠️ 部分（有缺口）/ ❌ 缺失 / n/a 不适用

---

## 二、完整兑现的杠杆（B / D / E / H）

### 2.1 杠杆 B — 原生多套账（并行账簿）✅

- **模型**：`ErpMdAcctSchema`（`nature` 字典 FINANCIAL/MANAGEMENT/TAX/CONSOLIDATION/BUDGET，`isPropagate` 布尔默认 false，`costingMethod`）+ `ErpMdAcctSchemaCoa`；`ErpFinVoucher/Line`、`ErpFinGlBalance`、`ErpInvCostLayer`、`ErpInvStockLedger` 全携带 `acctSchemaId`。
- **服务逻辑**（HEAD 提交落地）：
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/SchemaPropagator.java`（134 行）：`resolveTargetSchemas(orgId, primarySchemaId)` 读 `isPropagate`，启用时收集同 org 全部启用账套，按 nature 优先级排序，受 `erp-fin.multi-schema-enabled` 配置门控。
  - `ErpFinPostingProcessor.java`（887 行）`doProcess()`：迭代全部目标账套，非主账套走 `translateFactsForSchema()` 生成独立凭证。
  - `module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/SubjectMappingResolver.java`：按 `ErpMdSubjectMapping` 解析 `sourceSubjectId → targetSubjectId` 跨账套映射，无映射时回退源科目。
- **对标**：超越 Odoo/ERPNext（**无多套账**），与 iDempiere/metasfresh Accounting Schema 持平且栈更现代。
- **缺口（次要，测试）**：无"1 业务事件 → N 账套 N 凭证"的专项集成测试；`multi-schema-enabled` 默认 false（有意设计）。

### 2.2 杠杆 E — 完整成本方法集（7 种）✅

- **模型**：`erp-md/cost-method` 字典 7 项；`ErpInvCostLayer`（costMethod + acctSchemaId + batchNo + remainingQuantity + unitCost + incomingDate）。
- **服务逻辑**：7 个独立 Strategy 类均含**真实 BigDecimal 计算**（非桩），位于 `module-inventory/erp-inv-service/.../costing/`：`MovingAverageCostingStrategy`(92)、`WeightedAverageCostingStrategy`(104，全月一次)、`FifoCostingStrategy`(207)、`LifoCostingStrategy`(194，层排序 `.reversed()`)、`StandardCostingStrategy`(90，PPV 分离)、`SpecificCostingStrategy`(190，按 batch/serial 匹配)、`BatchCostingStrategy`(198)。`StockMoveBookkeeper` 于 `@PostConstruct` 注册全部 7 策略，`CostMethodResolver.resolve()` 显式分发。
- **对标**：超越 Odoo（缺 LIFO/具体辨认），与 iDempiere 持平且支持按账套差异化（`ErpMdAcctSchema.costingMethod` 默认 + 物料级覆盖）。
- **缺口（次要，测试）**：仅 FIFO / Standard / Dispatch 有专项测试文件（`TestErpInvFifoCosting` 354 行、`TestErpInvStandardCosting` 408 行、`TestErpInvCostingDispatch` 240 行）；LIFO/Batch/Specific/WAM 缺独立测试（FIFO/LIFO 共架构，覆盖度尚可）。

### 2.3 杠杆 D — 业财一体三件套 + posted 兜底扫描 ✅

- **模型**：`ErpFinVoucher/Line/BillR` 三件套 + `ErpFinVoucherTemplate/Line`（businessType + acctSchemaId + validFrom/To + isActive）+ 全域业务单据头 `posted/postedAt/postedBy`。
- **服务逻辑**：
  - 过账管线 `ErpFinPostingProcessor`（7 步：幂等 → Provider 解析 → Fact 生成校验 → 期间门控 → 平衡断言 → Voucher+Line+BillR 持久化 → AR/AP 生成）。
  - Provider 注册表 `ErpFinAcctDocRegistry`；域触发器 `InvPostingDispatcher`/`ExpenseClaimPostingDispatcher`/`NotesPostingDispatcher` 等，均遵循"捕获异常保持 posted=false 等兜底重扫"。
  - 模板引擎 `ErpFinTemplateAcctDocProvider`（196 行）：**非硬编码**，读模板 + `${key}` 占位符解析 + amountKey/amountExpression + 时效门控；注册为 `isFallback()=true`（域专用 Provider 优先）。
  - **posted 兜底扫描（关键，非设计稿）**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/job/DeferredPostingSweepJob.java`（218 行）扫 `ErpFinPostingException` status=PENDING、retryCount<3、24h 窗口，重建 PostingEvent 重放；**已接线到 `app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`：`erp-fin-deferred-posting-sweep` cron `0 0/5 * * * ?`（每 5 分钟）**。
- **对标**：超越 Odoo（Anglo-Saxon 仅企业版）/ERPNext（永续盘存），posted 标志 + 兜底扫描保证幂等可重放的最终一致。
- **测试**：16+ 域过账测试类（Finance/Inventory/Purchase/Sales/Manufacturing/Quality/Contract/Logistics/Assets/B2B）+ Playwright `finance-voucher-post.action.spec.ts`。缺口：`DeferredPostingSweepJob.execute()` 无专项单测（次要）。

### 2.4 杠杆 H — AR/AP open-item + 记录级核销 ✅（最成熟）

- **模型**：`ErpFinArApItem`（direction RECEIVABLE/PAYABLE、openAmountSource/Functional、settledAmount*、status OPEN/PARTIAL/SETTLED、sourceBillType/Code 字符串软引用）+ `ErpFinReconciliation/Line`（paymentItemId ↔ invoiceItemId 多对多 + fxGainLoss）。
- **服务逻辑**（全为本项目原创，非平台继承）：
  - `ErpFinArApItemGenerator`（333 行）：过账时生成 ArApItem，覆盖 10 种 businessType 方向映射，幂等 + 红冲。
  - `ReconciliationSettler`（122 行）：post/reverse 更新已核销/未核销金额与 OPEN→PARTIAL→SETTLED 状态机。
  - `AutoReconciliationEngine`（352 行）：FIFO / 按金额 / 按比例三策略 + 精度控制 + 尾差调整。
  - `PartnerBalanceUpdater`（63 行）：从 ArApItem 聚合重算 `ErpMdPartner` 往来余额。
- **对标 & 反模式**：`docs/design/domain-design-guidelines.md`（§10.6 附近）明确禁止"用 ErpMdPartner 余额字段魔法更新代替 open-item 明细账"。
- **测试**：Playwright `fin-reconciliation.action.spec.ts`（408 行，正路径 + 5 负路径守卫 + 双面对账）+ JUnit（`TestErpFinReconciliation`/`TestErpFinAutoReconciliation`/`TestErpFinPartnerBalance`/`TestErpFinBankReconciliation*`）。
- **缺口（次要）**：核销时 `fxGainLoss` 在 `ErpFinReconciliationBizModel` 硬编码为 `ZERO`；期末汇兑重估由独立 `ExchangeRevaluationService` 承载，核销时点动态汇兑损益未计算。

---

## 三、大部分兑现、有明确缺口的杠杆（A / F）

### 3.1 杠杆 A — 真模型驱动 + 真 Delta 升级层 ✅（文档数据错误）

- **模型驱动（兑现）**：19 个 `module-*/model/app-erp-*.orm.xml` 为唯一真相源，codegen 产物（`_gen/`、`_` 前缀）未见手改违规。
- **Delta 层（兑现，纠正子代理误判）**：项目**实际在用** Nop Delta 分层——**338 个手写 `.view.xml` 通过 `x:extends="_gen/_*.view.xml"` 叠加于生成基线**（`x:override="bounded-merge"` 定制列/按钮）。子代理"零 Delta 使用"的结论仅检查了 orm.xml 层的 `x:extends`，遗漏了 view 层，**结论错误，本审计已纠正**。
- **[H-A] 文档数据错误（高）**：competitive-comparison.md 存在自相矛盾且与实测不符的实体/文件计数：
  | 位置 | 文档声称 | 实测 |
  |---|---|---|
  | §四杠杆 A（L84） | "**10 份** orm.xml，共 **145 实体**" | **19 份** orm.xml |
  | §五 MixERP 行（L165） | "全部 18 域 **447 实体**" | — |
  | 实测（本审计） | — | **自有实体 352 个**（生成）+ 110 个跨域 `notGenCode` 引用桩 = 462 个 `<entity>` 元素，跨 **19** 域 |
  > 三处数字互相冲突（145 / 447 / 实测 352），且"10 份"严重少报（实际 19）。需统一勘误。

### 3.2 杠杆 F — 制造全链 ⚠️（委外空壳）

7 项声明子能力中 **6 项完整兑现，1 项空壳**：

| 子能力 | 声明 | 实测 | 状态 |
|---|---|---|---|
| MRP 多级 BOM 展开 | 多级展开 | `MrpEngine`(277) DFS + 环检测 + `DemandAggregator`(299) 四来源 + `MrpReleaseService`(208) | ✅ |
| 工单 10 态状态机 | 含 STOCK_RESERVED 齐套 | `ErpMfgWorkOrderProcessor`(510) 全生命周期 + `KitAvailabilityChecker`(148) | ✅ |
| 领料 → 库存出库 | 触发出库 | `ErpMfgMaterialIssueBizModel.confirm()`(204) → generateMove + materialCost 回写 | ✅ |
| 工时 → 人工成本 | 逐条工时 | `ErpMfgJobCardProcessor.recordWork()`(188) → TimeLog + laborCost 回写 | ✅ |
| 标准成本滚算 | 材料+人工+制造费用+委外 | `CostRollupService`(271) 自底向上递归 | ⚠️ overhead=0、subcontract=0（文档化 Follow-up） |
| 生产差异 | 5 类差异 | `ProductionVarianceCalculator`(443) 5 类 | ✅ |
| **委外加工** | `ErpMfgSubcontractOrder/Line` | **`ErpMfgSubcontractOrderBizModel.java` 仅 15 行 CRUD 空壳，零业务逻辑**；`MrpReleaseService:49` 明示"委外流程独立面，本期不支持" | ❌ **空壳** |

- **对标**：相对 Odoo/Axelor 制造完整性，委外是实打实缺口。APS/工序级排产（Axelor OperationOrder）**按 D1 裁决明确排除**（"不引入 APS，报表级负荷"），CRP（`CrpLoadCalculator` 602 行）确为报表/告警不自排——**符合声明，不算缺口**；`module-aps/ErpApsSchedulingEngine`(343) 存在但在独立模块、未纳入制造链声明。
- **测试**：23 JUnit + 12 Playwright E2E（mfg-chain / mfg-variance / mfg-genealogy / mfg-inspection-gate 等）。

---

## 四、机制兑现但措辞夸大 / 行为缺失的杠杆（G / C）

### 4.1 杠杆 G — 跨域 DAG + I*Biz 解耦 ⚠️（"可独立部署"夸大）

- **无 ORM 强引用（兑现）**：跨域引用走字符串软引用（`sourceBillType/sourceBillCode`，如 `ErpFinArApItem`/`ErpFinVoucherBillR`），不做跨域 FK。
- **I*Biz 解耦（兑现）**：343 个 `IErp*Biz` 接口（每实体一个，声明于 `-dao` 层）；跨域调用注入接口而非实体。
- **无循环依赖（兑现，纠正子代理误判）**：子代理称"finance↔inventory 循环"**错误**。实测：`finance-service → inventory-dao`（仅接口），而 `inventory-dao` 仅依赖 `master-data-dao`；`inventory-service → finance-service` 是单向。跨域调用统一走 `-dao` 层接口，`-service` 反向依赖不构成环。Maven reactor 会拒绝真实环，而 `app-erp-all` 构建成功（jar 于今日产出）即证明**无环**。本审计已纠正。
- **[H-G] "每个域可独立 Maven 工程"夸大（高）**：`-service` 层存在大量编译期跨域依赖（如 `inventory-service` → `finance-service`/`manufacturing-dao`/`purchase-dao`/`assets-dao`）。域**不可独立构建部署**。声明应弱化为"域间 ORM 解耦 + 接口化调用，为未来微服务化奠定结构基础"，而非"每个域可独立 Maven 工程、独立部署"。

### 4.2 杠杆 C — 多组织 / 多公司 ⚠️（行为层完全缺失）

- **结构（兑现）**：`orgId` 覆盖全 19 域 723 处业务单据头；`ErpMdOrganization` 含 `orgType` 6 值字典（GROUP/COMPANY/BRANCH/DEPARTMENT/WORKSHOP/STORE）+ `functionalCurrencyId` + 自引用树 + `children`；`code,orgId` 复合唯一键。
- **[H-C] 行为层缺失（高，最需警惕）**：**无任何自动 org 数据过滤、权限范围、组织树遍历逻辑**。`orgId.*filter|filterByOrg|scopeByOrg|orgScope` 搜索**零命中**；`orgId` 仅在少数手写查询（MRP/DemandAggregator/AcctSchema 查询）作显式参数。无公司间交易/转移定价/合并抵销逻辑。**零多组织行为测试**。
- **对标**：相对 iDempiere 25 年验证的 Client/Org 隔离，当前仅是"多组织地基"（列 + 字典 + 树），**不构成"多公司 ERP 系统"**。声明"orgId 作为物理列……兼顾 Odoo/ERPNext 的易用与 Compiere 家族的严格"应修正为如实描述当前为结构基础、行为层待建。

---

## 五、汇总问题清单

### 高（3 项，均为文档诚信 / 能力缺口）

| ID | 问题 | 位置 | 建议 |
|----|------|------|------|
| **H-A** | 实体/文件计数三处冲突且错误（145 / 447 / 实测 352；"10 份"实际 19） | competitive-comparison.md §四 L84、§五 L165 | 统一勘误为"19 域，自有实体 352（+110 跨域引用桩）" |
| **H-G** | "每个域可独立 Maven 工程、独立部署"夸大（service 层强耦合） | competitive-comparison.md §四杠杆 G L142 | 弱化为"ORM 解耦 + 接口化调用，微服务化结构基础" |
| **H-C** | "多公司"仅结构地基，行为层（数据隔离/权限范围/公司间）完全缺失 | competitive-comparison.md §四杠杆 C；§二总览"多公司=是" | §六诚实声明补列"多组织行为层未实现"；或降级总览措辞 |

### 中（4 项）

| ID | 问题 | 位置 |
|----|------|------|
| **M-1** | 制造委外（`ErpMfgSubcontractOrder`）为 15 行 CRUD 空壳，杠杆 F 声明"委外"未兑现 | module-manufacturing；competitive-comparison.md L134 |
| **M-2** | 成本滚算 overhead/subcontract 恒 0（材料+人工已实，制造费用+委外未拆） | `CostRollupService.java` |
| **M-3** | 核销时点 `fxGainLoss` 硬编码 ZERO（动态汇兑损益未计算） | `ErpFinReconciliationBizModel.java` |
| **M-4** | competitive-comparison.md 未区分"平台继承能力"（杠杆 A Delta）与"本项目落地" | competitive-comparison.md §四杠杆 A |

### 低（3 项，测试覆盖）

| ID | 问题 |
|----|------|
| **L-1** | 多套账"1 事件→N 凭证"无专项集成测试（杠杆 B） |
| **L-2** | LIFO/Batch/Specific/WAM 无独立成本测试文件（杠杆 E，FIFO/Standard/Dispatch 已覆盖） |
| **L-3** | `DeferredPostingSweepJob.execute()` 无专项单测（杠杆 D） |

---

## 六、总体兑现度

- **实现成熟度高**：post-codegen 阶段，291 JUnit + 136 Playwright E2E spec，`app-erp-all` 构建通过（HEAD jar 今日产出），backlog 除 P8（HR 引擎 `ready`）外全 `done`。
- **核心业财与成本能力（B/D/E/H）是真金白银的超越**：模型 + 逻辑 + 测试三层齐备，确实超越市场领导者 Odoo/ERPNext。
- **最大的"承诺 > 实现"缺口**：多组织行为（C，H-C）与制造委外（F 子项，M-1）。
- **文档诚信是本次审计的主要产出**：competitive-comparison.md 存在可核实的错误陈述（H-A 计数、H-G 独立部署、H-C 多公司），需在同批修复计划中勘误，使其区分"平台可支撑 / 本项目已演示 / 尚未落地"。

后续修复见 `docs/plans/2026-07-12-1504-1-competitive-comparison-correction.md`。

---

## 七、方法说明

本报告由主代理调度 3 路子代理并行审计后综合，**并对存疑结论逐一自核实**：

1. **杠杆 B/E/D 子代理**（ses…FeS3vc）→ 全部 FULLY IMPLEMENTED，`DeferredPostingSweepJob` scheduler.yaml 接线经主代理复核确认。
2. **杠杆 F/C 子代理**（ses…v5gTtO）→ F 委外空壳、C 行为缺失，经主代理复核 `ErpMfgSubcontractOrderBizModel`（15 行）+ `orgId` 过滤零命中确认。
3. **杠杆 A/G/H 子代理**（ses…Qx3h8x）→ **两处结论被主代理纠正**：(a) "零 Delta 使用"错误——实测 338 个 view.xml 用 `x:extends="_gen/`；(b) "finance↔inventory 循环依赖"错误——实测 `-service→-dao` 单向、reactor 无环、构建成功。
4. **主代理自核实**：实体计数（Python 解析 462 元素 = 352 自有 + 110 引用桩）、Maven 依赖方向（`-dao` 仅依赖 master-data）、委外行数、Delta view 层用量。

> 教训：子代理对"缺失"的断言（zero Delta / circular dep）必须由主代理用构建事实（reactor 是否拒绝环、jar 是否产出）与精确 grep 复核，不可直接采信。
