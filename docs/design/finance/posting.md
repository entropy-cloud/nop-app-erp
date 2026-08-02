# 业财打通机制（过账）

## 目的

说明业务单据如何自动生成会计凭证，包括凭证模板、过账引擎、科目映射、异步与冲销机制。

本文件是 `flow-overview.md` L3 节"业财打通"的详细展开。

## 总体架构（三层模型）

业财打通采用**三层分层模型**，自下而上依次为"不可变的强一致底座 → 可配的凭证时序 → 强制不变的兜底约束"：

```
┌─────────────────────────────────────────────────────────────────────┐
│ 第①层 底座：业务单据 + 库存（强制 SYNC，不可配置）                       │
│   同一 @BizMutation 事务内原子提交：                                    │
│     ├─ 业务单据状态变更（docStatus / approveStatus）                    │
│     ├─ 库存写入（库存移动/流水/余额）                                    │
│     └─ posted=false（待过账标志，与业务+库存同事务落盘）                 │
│   约束：库存写入不参与"可配置时序"，永远是 SYNC。这是物理库存正确性的    │
│         硬约束（iDempiere Doc.post / Metasfresh IPostingService 均如此）│
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 第②层 凭证生成时序（按 (billType, acctSchemaId) 可配：SYNC / ASYNC）    │
│   方式 A（SYNC，默认）：与第①层同事务，立即生成凭证，业务+库存+凭证三强一致│
│   方式 B（ASYNC）：经 txn().afterCommit() 解耦，post-commit 异步过账：   │
│     ├─ 发布 PostingEvent（businessType, billHeadCode, ...）            │
│     ├─ 注册中心按 businessType 路由 Provider                            │
│     ├─ Provider 生成分录                                                │
│     └─ 写入凭证 + 业财回链 + 更新 posted=true                           │
│   切换依据：性能瓶颈出现时再对个别 billType 切 ASYNC（见 §异步过账）      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 第③层 兜底（跨 SYNC/ASYNC 强制不变，不可关闭）                           │
│   posted 标志幂等 + 兜底扫描 + 业财回链 + 物理锁定 + 红字冲销 + 可审计    │
│   详见 §稳定约束 vs 可配置策略                                          │
└─────────────────────────────────────────────────────────────────────┘
```

> **默认配置**：本项目默认全部 billType 走 **SYNC**（方式 A），保证业务+库存+凭证三者强一致。仅当性能压测证明个别高吞吐单据（如大批量销售出库）成为瓶颈时，才对该 billType 切 ASYNC（方式 B）。可配性仅作用于第②层的时序，第①层（库存强一致）与第③层（兜底约束）恒定不变。

### PostingEvent 契约

> `PostingEvent` 是登记的**共享内核类型**（所有者 `module-finance/erp-fin-dao`，跨域消费基线见 `module-boundaries.md §共享内核` + 裁决 `docs/analysis/shared-kernel-extraction-decision.md`）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `businessType` | `ErpFinBusinessType`（enum，登记的共享内核类型） | 业务类型枚举（如 PURCHASE_INPUT、AR_INVOICE）；枚举 `name()` 与字典 `erp-fin/business-type` 的 `value` 逐一一致（持久化值 = enum.name() = dict value，UI 筛选与审计查询据此命中） |
| `billHeadCode` | String | 业务单据编码（幂等键） |
| `tenantId` | String | 租户 ID |
| `acctSchemaId` | Long | 账套 ID |
| `orgId` | Long | 组织 ID |
| `currencyId` | Long | 币种 ID |
| `exchangeRate` | BigDecimal | 汇率 |
| `voucherDate` | LocalDate | 凭证日期 |
| `billData` | Map<String,Object> | 单据数据（头+行，含金额、科目映射维度） |

> `businessType` 经 `IErpFinVoucherBiz.post(PostingEvent, ...)` 强类型传入；派发消费方（各域 AcctDocProvider）用 `== ErpFinBusinessType.X` 常量比较路由，故该 enum 不可降级为 SPI 接口（详见裁决文档）。

### 幂等保证

过账操作前置检查 `posted=true` 时直接跳过，防止重复过账。兜底扫描与事件回调可能同时命中同一单据，posted 检查确保只处理一次。

## businessType vs billType 分工

`businessType` 与 `billType` 是**两个正交标识**，职责不同、非一对一，二者在业财回链表（`ErpFinVoucherBillR`）中同时落库：

| 标识 | 职责 | 取值来源 | 典型值 | 承载位置 |
|------|------|----------|--------|----------|
| `billType` | **源单识别 / 回链反查**（对应具体 ORM 实体/表） | `data-dependency-matrix.md §5.2` 枚举 | `PUR_RECEIVE`、`SAL_DELIVERY` | 弱指针三元组 `(billType, billHeadCode, lineCode)` |
| `businessType` | **过账语义 / 凭证模板路由**（会计事件分类） | 本节 §业务类型映射（唯一权威源） | `PURCHASE_INPUT`、`AR_INVOICE` | `PostingEvent.businessType`、凭证模板路由键 |

> **非 1:1 关系**：一个 `billType` 可映射多个 `businessType`。例如 `billType=PUR_RECEIVE`（采购入库单）在不同环节触发不同会计事件——入库时 `businessType=PURCHASE_INPUT`（暂估应付），收到发票时 `businessType=AP_INVOICE`（进项税+应付）。回链表同时存两者，便于"按源单反查"（用 billType）与"按会计语义聚合"（用 businessType）。

> **参考**：iDempiere 的 `C_DocTypeTarget_ID`/`DocBaseType`（单据类型/会计基类）与 `Fact_Acct.AD_Table_ID+Record_ID`（源单反查）正是这一分工的原型；Metasfresh 的 `AcctDocRegistry` 用 `docTableName`（识别实体）与 `Doc_Invoice.createFacts`（会计语义）同样分离。

## 业务类型映射（唯一权威源）

> **重要**：本表是全部 `businessType` 的唯一权威来源（负责过账语义/凭证模板路由）。源单识别/回链用 `billType`（见 `data-dependency-matrix.md §5.2` 枚举），两者非 1:1。所有模块的业财过账必须使用本表定义的 businessType，新增业务类型时必须更新本表。

每种业务单据对应一个 `businessType`，决定使用哪个凭证模板：

### 核心业务类型（进销存+财务）

| 业务单据 | businessType | 借贷方向（典型） | 触发域 | 设计文档 |
|----------|--------------|------------------|--------|---------|
| 采购入库 | PURCHASE_INPUT | 借：存货 / 贷：暂估应付 | purchase | `purchase/README.md` |
| 销售出库 | SALES_OUTPUT | 借：结转成本 / 贷：存货 | sales | `sales/README.md` |
| 采购发票 | AP_INVOICE | 借：费用/采购 / 借：进项税 / 贷：应付 | purchase | `purchase/README.md` |

> **GRNI 暂估冲回 documented simplification**：「先入库后开票」黄金路径下，`PURCHASE_INPUT`（receive）与 `AP_INVOICE`（invoice）各自独立过账——`PURCHASE_INPUT` 凭证 `billHeadCode = 库存移动单号`，`AP_INVOICE` 凭证 `billHeadCode = 发票号`，两者经不同 billHeadCode 落业财回链，**无自动冲回**（GL 2202 暂估应付双计 + 1403/1401 存货双计）。辅助账层（`ErpFinArApItem`）不受影响（辅助账生成器不处理 `PURCHASE_INPUT`）。**裁决为 documented simplification（非降级 deferred）**：完整自动冲回需双向钩子（approve 红冲 + reverseApprove 反冲回，后者需 inventory 域 `repostPurchaseInput` SPI）+ 部分开票覆盖判定（`reverse()` 仅全额红冲）+ 跨期语义，对参考应用不成比例；期末试算平衡可发现并手工清理。详见 `docs/design/purchase/returns.md §暂估应付冲减「正向 receive→invoice 暂估冲回」`。
| 销售发票 | AR_INVOICE | 借：应收 / 贷：收入 / 贷：销项税 | sales | `sales/README.md` |
| 付款 | PAYMENT | 借：应付 / 贷：银行存款 | purchase | `purchase/README.md` |
| 收款 | RECEIPT | 借：银行存款 / 贷：应收 | sales | `sales/README.md` |

### 资产业务类型

| 业务单据 | businessType | 借贷方向（典型） | 触发域 | 设计文档 |
|----------|--------------|------------------|--------|---------|
| 资产折旧 | DEPRECIATION | 借：折旧费用 / 贷：累计折旧 | assets | `flow-overview.md` / `assets/state-machine.md` |
| 资产资本化 | CAPITALIZATION | 借：固定资产 / 贷：在建工程或存货 | assets | `flow-overview.md` |
| 资产处置 | DISPOSAL | 借：累计折旧 / 借：清理损益 / 贷：固定资产 | assets | `flow-overview.md` |

### 费用报销与资金业务类型

| 业务单据 | businessType | 借贷方向（典型） | 触发域 | 设计文档 |
|----------|--------------|------------------|--------|---------|
| 费用报销 | EXPENSE_CLAIM | 借：费用科目 / 借：进项税 / 贷：应付-员工或银行存款 | finance | `expense-claim.md` |
| 员工借款 | EMPLOYEE_ADVANCE | 借：其他应收款-员工预支 / 贷：银行存款 | finance | `expense-claim.md` |
| 借款核销 | EMPLOYEE_ADVANCE_SETTLE | 借：应付-员工（报销抵扣）或银行存款（现金还款） / 贷：其他应收款-员工预支 | finance | `expense-claim.md` |
| 应收票据收到 | NOTES_RECEIVABLE_RECEIVED | 借：应收票据 / 贷：应收账款 | finance | `treasury.md` |
| 票据贴现 | NOTES_RECEIVABLE_DISCOUNTED | 借：银行存款(实得) / 借：财务费用-贴现息 / [借/贷] 汇兑损益 / 贷：应收票据 | finance | `treasury.md` |
| 背书转让 | NOTES_RECEIVABLE_ENDORSED | 借：应付账款(抵供应商) / 贷：应收票据 | finance | `treasury.md` |
| 到期托收 | NOTES_RECEIVABLE_COLLECTION | 借：银行存款 / 贷：应收票据 | finance | `treasury.md` |
| 应付票据开出 | NOTES_PAYABLE_ISSUED | 借：应付账款 / 贷：应付票据 | finance | `treasury.md` |
| 票据兑付 | NOTES_PAYABLE_HONORED | 借：应付票据 / 贷：银行存款 | finance | `treasury.md` |
| 授信利息 | CREDIT_FACILITY_INTEREST | 借：财务费用-利息支出 / 贷：银行存款 | finance | `treasury.md` |

### 制造与物流业务类型

| 业务单据 | businessType | 借贷方向（典型） | 触发域 | 设计文档 |
|----------|--------------|------------------|--------|---------|
| 制造完工入库 | MANUFACTURING_FINISHED_INPUT | 借：产成品存货 / 贷：生产成本-结转 | manufacturing | `manufacturing/state-machine.md` |
| 制造成本结转 | MANUFACTURING_COST_CLOSE | 借：主营业务成本 / 贷：产成品存货 | manufacturing | `manufacturing/bom-and-routing.md` |
| 委外发料 | SUBCONTRACT_ISSUE | 借：委外加工物资 / 贷：原材料 | manufacturing | `manufacturing/subcontracting.md` |
| 委外收货 | SUBCONTRACT_RECEIPT | 借：半成品/产成品 / 贷：委外加工物资 + 应付加工费 | manufacturing | `manufacturing/subcontracting.md` |
| 销售运费 | FREIGHT | 借：销售费用-运费 / 贷：应付或银行存款 | logistics | `logistics/state-machine.md` |

### 质量与异常业务类型

| 业务单据 | businessType | 借贷方向（典型） | 触发域 | 设计文档 |
|----------|--------------|------------------|--------|---------|
| NCR 报废损失 | NCR_SCRAP | 借：营业外支出 / 贷：存货 | quality | `quality/state-machine.md` |
| 所有权转移 | OWNERSHIP_TRANSFER | 借：存货(自有) / 贷：应付-供应商 | inventory | `consignment.md` |
| 内部调拨 | INTER_TRANSFER | 借：存货-调入方 / 贷：存货-调出方（内部交易） | inventory | `inventory/README.md` |

### 人力资源业务类型

| 业务单据 | businessType | 借贷方向（典型） | 触发域 | 设计文档 |
|----------|--------------|------------------|--------|---------|
| 薪酬计提 | SALARY | 借：管理费用-工资 / 贷：应付职工薪酬 | hr | `human-resource/README.md` |
| 薪酬发放 | SALARY_PAYMENT | 借：应付职工薪酬 / 贷：银行存款 | hr | `human-resource/README.md` |
| 社保缴纳 | SOCIAL_INSURANCE | 借：管理费用-社保 / 贷：银行存款（+个人部分挂其他应收款） | hr | `human-resource/README.md` |

> 具体借贷科目取决于科目映射配置（见下文"科目映射"），上表是典型场景。新增业务类型时，必须在本表追加一行并更新对应设计文档。

## 凭证模板机制

### 模板结构

凭证模板（VoucherTemplate）预定义每类业务的借贷分录骨架：

```
模板头（VoucherTemplate）
  ├─ businessType（业务类型，如 AP_INVOICE）
  ├─ 凭证字（收/付/转）
  └─ 模板行（VoucherTemplateLine）[]
       ├─ 序号（行顺序）
       ├─ 摘要（memo）
       ├─ 科目映射键（accountKey，如 "INVENTORY"/"AP"/"INPUT_TAX"）
       ├─ 借贷方向（借/贷）
       └─ 金额占位符（amountKey，如 "AMOUNT"/"TAX_AMOUNT"/"TOTAL"）
```

### 占位符填充

模板行的金额是占位符（如 `"AMOUNT"`），业务单据触发时按 `businessType` 传入金额数组：

```
autoCreateVoucher(billHeadCode, Double[]{amountSum, taxAmountSum, voucherAmount}, businessType)
```

模板按行顺序依次用金额数组的对应下标填充占位符。例如 AP_INVOICE 模板可能有三行：
- 行1：借 费用科目，金额占位 `"AMOUNT"`（货款）
- 行2：借 进项税科目，金额占位 `"TAX_AMOUNT"`（税额）
- 行3：贷 应付科目，金额占位 `"TOTAL"`（价税合计）

### 模板配置化

- 凭证模板在数据库中维护（不是硬编码），支持按租户/账套定制。
- 新增业务类型只需新增模板 + 实现 Provider，无需改财务核心。
- 模板支持版本管理（不同会计期间可用不同模板）。

## 过账引擎（可插拔 Provider 机制）

> 过账流程的步骤顺序属"稳定约束"（见 §稳定约束 vs 可配置策略），Java 实现遵循 `docs/architecture/processor-extension-pattern.md` 的 Facade + Processor 两层结构与派生覆盖约定，不使用 task.xml 编排。

### 接口设计

财务域定义凭证生成 Provider SPI（`IErpFinAcctDocProvider`）：每个业务域实现该接口、声明所支持的 `businessType` 集合并生成分录。注册中心在启动期按 `businessType` 建立 O(1) 类型安全映射（运行时按 `businessType` 直接 Map 查找，而非遍历 List），运行时按 `businessType` 路由到对应 Provider。接口签名、注册机制与类型安全约束（参考 Metasfresh `ImmutableMap<String, AcctDocFactory>` 范式）见 `docs/architecture/processor-extension-pattern.md`。

### 跨域自动聚合

各业务域（purchase/sales/inventory）各自实现 `IErpFinAcctDocProvider` 并注册为 Bean：

- purchase 域 Provider 处理 AP_INVOICE/PAYMENT/PURCHASE_INPUT。
- sales 域 Provider 处理 AR_INVOICE/RECEIPT/SALES_OUTPUT。
- inventory 域 Provider 处理存货估值。

财务域注册中心自动聚合所有 Provider，按 `businessType` 路由。

**新增业务类型 = 新增 Provider Bean，零改动财务核心**——这是模块化业财一体的关键。

### 注册方式

使用类型安全的 Map 注册（避免反射命名约定）：启动期收集所有 Provider Bean 并按 `businessType` 建立不可变映射，运行时 O(1) 查找。注册中心实现细节见 `docs/architecture/processor-extension-pattern.md`。

### 凭证写库前校验扩展点（IErpFinFactsValidator）

> 参考 Metasfresh 的 `IFactsValidator` 扩展机制。允许第三方在凭证写库前对借贷分录行做业务校验或改写（如按租户定制借贷规则、按维度分摊、特殊行业调整）。

该扩展点 SPI（`IErpFinFactsValidator`）在 Provider 生成分录后、写库前按顺序执行：可校验（借贷平衡/科目有效性/维度完整性）、可改写（拆行/追加）、可拒绝（`NopException` 阻止过账）。与 Provider 同模式经 `@Inject List` 聚合，支持多个并按 `getOrder()` 排序。接口签名与注册机制见 `docs/architecture/processor-extension-pattern.md`。

**典型应用场景**：

| 场景 | Validator 行为 |
|------|----------------|
| 按部门/项目分摊金额 | 改写：单行拆成多行（GL Distribution 范式） |
| 行业附加税计提 | 追加：新增税额分录行 |
| 租户定制借贷规则 | 改写：按租户配置调整科目映射 |
| 合规校验（如现金流分类） | 拒绝：不符合现金流量表分类的凭证 throw NopException |
| 跨账套同步 | 追加：在多 AcctSchema 下各生成一组分录 |

**与 GL Distribution（科目分摊）的关系**：GL Distribution 是 FactsValidator 的一个具体实现——按部门/项目/产品线将一条分录拆成多条。本工程不强制实现 GL Distribution，但通过 FactsValidator 扩展点保留了实现能力（参考 iDempiere/Metasfresh 的 `MGLDistribution` + `FactsValidator` 组合）。

> **新增校验规则 = 新增 Validator Bean，零改动财务核心**。与 Provider 机制配套，形成"生成 → 校验/改写 → 落库"的完整可插拔流水线。

## 科目映射

### 多维科目解析

同一业务类型的科目可能因业务对象不同而不同（如同是采购入库，但不同物料类别的存货科目不同）。科目映射做成多维决策：

| 维度 | 示例 |
|------|------|
| 业务类型 | AP_INVOICE → 应付科目族 |
| 物料类别 | 原材料 → 原材料科目；产成品 → 产成品科目 |
| 往来单位组 | 国内供应商 → 应付-国内；国外供应商 → 应付-国外 |
| 仓库 | 普通仓 → 存货科目；在途仓 → 在途物资科目 |
| 部门/项目 | 辅助核算维度 |

### 解析规则

- 按"specific → generic"优先级匹配（先按物料类别，再按物料，最后用默认）。
- 科目映射在数据库配置（规则表或元数据驱动），不是硬编码 if-else。
- 支持多套会计科目表并行（管理账/税务账），同一业务在多套下各解析一组科目。

### 规则表实现（A1）

A1 以 `ErpFinGlMappingRule` 实体 + `IErpFinGlMappingResolver` 解析引擎，作为 Provider 之上的**可选多维覆盖层**：

- **接入点**：引擎在科目解析开头按 `VoucherFact.accountKey` 查规则表覆盖 `subjectCode`；其后既有 `code → ErpMdSubject` 查找流程不变（实现细节见 `docs/architecture/processor-extension-pattern.md`）。
- **优先级链算法**：`(priority DESC, 维度具体度 DESC)` 排序匹配；`priority=0` 为 default 兜底（全 NULL 维度），`priority≥100` 为精确规则惯例。详见 [`docs/design/finance/gl-mapping-rules.md` §3 优先级链算法](gl-mapping-rules.md#3-优先级链算法)。
- **试点进度**：purchase 域 AP_INVOICE × 3 键（PURCHASE/INPUT_VAT/ACCOUNTS_PAYABLE）已接入；其余域 Provider（sales/inventory/assets/hr/maintenance）opt-in 接入归 Deferred successor。
- **接入步骤模板 + 试点清单**：详见 [`docs/design/finance/gl-mapping-rules.md` §5 Provider opt-in 集成契约](gl-mapping-rules.md#5-provider-opt-in-集成契约)。
- **与 `ErpMdSubjectMapping` 边界**：`ErpMdSubjectMapping` 是 post-resolution 跨账套转换（subjectId → subjectId）；`ErpFinGlMappingRule` 是 pre-resolution 多维业务规则（businessType+accountKey+dimensions → subjectCode）。三层职责不重叠。

## 异步过账与失败处理

> 本节描述总体架构 §第②层的 ASYNC 模式（方式 B）及其失败恢复。**默认走 SYNC（方式 A）**，ASYNC 仅为可选优化。无论 SYNC/ASYNC，库存写入（第①层）恒定强一致，兜底约束（第③层）恒定生效。

### 异步机制（方式 B，可选）

- 业务单据审核通过 → 主事务落"单据 + 库存 + `posted=false`"（第①层，SYNC 强一致）。
- 凭证生成经 `txn().afterCommit()` 解耦到 post-commit 异步执行（不阻塞业务单据审核响应）。
- 凭证过账失败可重试，不影响已提交的单据+库存状态（业务与凭证在 ASYNC 模式下短暂解耦，由第③层兜底保证最终一致）。

### posted 标志兜底

- 业务单据带 `posted` 字段（boolean），与单据+库存同事务落盘。
- 定期兜底扫描（定时任务）：扫描 `posted=false` 且已审核超过 N 分钟的单据，重新触发过账。
- 处理异步事件丢失、服务重启等异常场景——兜底扫描对 SYNC/ASYNC 两种模式统一生效。
- **定时作业登记**：兜底扫描登记于 `docs/architecture/job-scheduling.md` §3.1 `erp-fin-posting-scan`（每分钟，`erp-fin.posting-scan-cron`，nop-batch candidate 大数据量迁移）；cron 接线归 follow-up。

### 同步测试缝（postNow）

`IErpFinPostingBiz` 设计阶段即预留**同步直调入口** `postNow(billType, billHeadCode)`：

- 测试场景下绕过 ASYNC 时序，直接在同事务内完成凭证生成，便于 `JunitAutoTestCase` 快照断言（见 `testing-strategy.md` 异步过账测试时序模型）。
- 生产场景下若某 billType 配置为 SYNC，`postNow` 即是其实现路径；ASYNC 模式下 `postNow` 可作为兜底直调（不依赖事件时序）。
- 该入口是"测试同步化 + 兜底直调"的统一缝，避免为测试单独开后门。

### 失败处理策略

| 失败类型 | 处理 |
|----------|------|
| 模板缺失 | 报错并标记，等待人工配置模板后重试 |
| 科目映射缺失 | 报错并标记，等待人工配置科目映射 |
| 借贷不平衡 | 报错（通常是模板配置错误），人工介入 |
| 期间已结账 | 报错，需反结账或计入当前开启期间 |
| 系统异常 | 自动重试（指数退避），超过阈值告警 |

## 稳定约束 vs 可配置策略

> 本节是三层模型（§总体架构）的**配置边界裁决表**：哪些恒定不变、哪些可调。修改过账机制时**禁止**触碰"稳定约束"列。

| 维度 | 稳定约束（恒定不变，不可配置） | 可配置策略（按需调整） |
|------|-------------------------------|------------------------|
| **库存一致性** | 第①层：业务单据 + 库存写入（库存移动/流水/余额）永远在同一 `@BizMutation` 事务强一致 | ❌ 不可配（物理库存正确性硬约束） |
| **凭证时序** | 第②层最终一致（posted 标志 + 兜底保证） | ✅ 按 `(billType, acctSchemaId)` 切 SYNC 同事务 / ASYNC post-commit |
| **幂等** | posted 标志前置检查，重复过账直接跳过 | ❌ 不可配 |
| **业财回链** | `ErpFinVoucherBillR` 同时存 billType + businessType，双向可查 | ❌ 不可配 |
| **物理锁定** | 过账中对单据加锁，防止并发过账 | ❌ 不可配 |
| **可补偿** | 红字冲销（见 §冲销机制） | ❌ 不可配 |
| **可审计** | 凭证 + 回链 + posted 翻转全程留痕 | ❌ 不可配 |
| **默认模式** | — | ✅ 默认 SYNC；性能瓶颈时个别 billType 切 ASYNC |

> **判定原则**：可配的**仅**"凭证生成时序"一项。任何声称"库存可异步""幂等可关闭""回链可省略"的设计都违反稳定约束。

## 冲销机制

> 冲销是**双向闭环**：业务侧作废单据会触发凭证红冲；财务侧直接红冲凭证也必须回退业务单据状态。两个方向缺一不可，否则业财数据将失配。本节定义两个方向的契约。

### 方向一：业务单据作废 → 凭证冲销（业务侧驱动）

- 业务单据作废/反审核时，业务域调用 `IErpFinPostingBiz.reverse(billHeadCode, businessType)`，按业财回链表反查关联的已过账凭证。
- 引擎生成红字冲销凭证（金额取负），关联原凭证（`reversalOfVoucherId`）与作废的业务单据（新写一条业财回链）。
- 红字凭证走正常"草稿→已过账"流程（平衡校验、期间门控、科目反查）。
- **原凭证标记**：引擎在红字凭证落库后将原 NORMAL + POSTED + 未冲销凭证的 `isReversed` 置 `true`——这是引擎侧统一承担的标记责任，业务域**不应**绕过 `IErpFinVoucherBiz.reverse()` I*Biz 边界直接跨模块写 `ErpFinVoucher.isReversed`（实现细节见 `docs/architecture/processor-extension-pattern.md`，业财一体写契约见 `integration-and-transaction-patterns.md`）。
- 业务单据状态本就已由业务域在作废动作中回退，引擎无需再反写。

### 方向二：凭证红冲 → 业务单据回退（冲销反写闭环）

> 此方向是业财闭环的必备能力。典型场景：财务员发现某采购发票记错科目，直接红冲凭证 → 采购单状态须从"已入账"回退为"待确认"，业务流程才能继续推进（重新开票或调整）。

- 财务员（或系统）调用 `reverse()` 红冲已过账凭证时，引擎在红字凭证过账成功后发布 **`VoucherReversedEvent`**。
- **业务域监听该事件，自行回退自身业务单据状态**（引擎不持有源实体，见 §反写契约）。
- 回退动作由域自治：purchase 域监听后回退采购单/发票状态，sales 域回退销售单状态，inventory 域回退库存移动单状态等。
- 回退失败时业务单据状态维持原状，已过账的红字凭证不回滚（凭证一旦过账具有法律效力），失败进告警队列人工处理（见 `posting-log.md` §过账异常处置）。

#### `VoucherReversedEvent` 契约

| 字段 | 类型 | 说明 |
|------|------|------|
| `voucherId` | String | 红字凭证 ID |
| `reversalOfVoucherId` | String | 被冲销的原凭证 ID |
| `billHeadCode` | String | 关联的业务单据号（经业财回链反查） |
| `businessType` | String | 业务类型（路由回退逻辑用） |
| `billType` | String | 源单类型（对应 ORM 实体，见 `data-dependency-matrix.md §5.2`） |
| `traceId` | String | 端到端追踪 ID（见 `posting-log.md`） |

> 事件派发时机：红字凭证 `post()` 事务提交后（post-commit），与 `posting.md` §总体架构 第②层 ASYNC 模式一致。SYNC 模式下可在同事务内同步通知域监听器。

#### 实现策略

##### 派发机制——finance 定义 SPI + 默认 SYNC 同事务通知

- **裁决**：finance 域定义 `IErpFinVoucherReversedListener` SPI；监听者注册中心（镜像 Provider 聚合范式，见 `docs/architecture/processor-extension-pattern.md`）启动期聚合所有监听者 Bean；引擎在红字凭证 + 业财回链 + `cancelOnReverse` 落库之后构造 `VoucherReversedEvent` 并按配置派发：
  - **默认 SYNC**（与 §总体架构 默认 SYNC 强一致对齐）：在同事务内同步遍历监听者——监听者与红字凭证原子提交，回退失败即整体回滚（保持业财强一致）。
  - **可选 ASYNC**（与第②层 ASYNC 模式对齐）：经 post-commit 回调，红字凭证事务提交成功后再异步派发。
- **平台能力核实**：
  - 进程内无 `IEventBus`/`@EventListener`；post-commit 回调能力存在（仅在事务提交成功时触发，回滚路径不执行）。
  - `IErpFinVoucherBiz.reverse()` I*Biz Facade **对齐 `post()` 叠加 `@Transactional(REQUIRES_NEW)`**（O-7，见 `ErpFinVoucherBizModel`）——红冲凭证写操作以独立事务承接，红冲失败回滚独立事务，**不污染调用方主事务**（与 `post()` 一致的事务边界语义）。SYNC 同事务通知在该 REQUIRES_NEW 独立事务边界内；post-commit 派发在独立事务提交后触发。
  - **跨域调用方事务边界注意事项**：跨域调用方（11 域 PostingExecutor/Dispatcher，如 purchase/sales/inventory/assets 域审核动作内的 `reverse()` 调用）在自身 `@BizMutation` 事务内调 `reverse()` 时，因 `reverse()` 叠加 REQUIRES_NEW，红冲凭证落库于独立事务——调用方主事务回滚**不会**回滚已过账红字凭证（凭证法律效力保护）；反之红冲失败抛 `NopException` 由调用方 try/catch 决定是否阻断自身业务流（域自治，见 `posting-log.md §错误传播分级策略` G3/G4）。
- **替代方案（被拒）**：
  - 外部 MQ：破坏 SYNC 强一致默认 + 引入 infra 依赖，与"默认 SYNC"哲学冲突。
  - Spring `ApplicationEvent`：平台无该设施。
- **失败隔离裁决**：SYNC 同事务通知下，监听者抛 `NopException` 会回滚整张红字凭证事务——**违反"凭证一旦过账具有法律效力不回滚"原则**。故裁决：**派发循环对每个监听者 try/catch 包裹**，单个监听者抛错不中断其他监听者、不回滚已过账红字凭证；失败记录（源单类型+billHeadCode+ErrorCode+处置状态）落入 §过账异常处置 异常工作台（`ErpFinPostingException`，postingType=`REVERSAL`，failedStage=`notify-reversal-listener`）的 PENDING 队列供人工处置。该裁决使"红字凭证法律效力"与"监听者失败可见可处置"两者并存。
- **代价**：finance-service 新增 1 SPI + 1 DTO + 1 注册中心 bean；业务域各加 1 监听者 bean（按需）。零 ORM 实体新增、零列新增。

##### 各域回退目标态——逐域裁定，复用既有 reverseApprove 语义

- **裁决**：监听者经 `ErpFinVoucherBillR` 反查源单（`billType`+`billCode`），按各域既有"业务侧反审核（reverseApprove）"已验证的状态回退逻辑回退自身状态——**不重复造回退逻辑**，直接复用各域 reverseApprove 中已验证的状态迁移。

| 域 | 源单类型（billType） | 回退目标态（posted=true → ?） | 依据 |
|----|---------------------|------------------------------|------|
| purchase | `AP_INVOICE`（ErpPurInvoice）/`PAYMENT`（ErpPurPayment）/`PUR_RETURN`（ErpPurReturn） | `approveStatus`: APPROVED → **REJECTED**；`posted=false`/`postedAt=null`/`postedBy=null` | 各域 reverseApprove 后置 REJECTED（实现见 `docs/architecture/processor-extension-pattern.md`） |
| purchase | `PURCHASE_INPUT`（ErpPurReceive） | `approveStatus`: APPROVED → **REJECTED**；`posted=false`/`postedAt=null`/`postedBy=null`（与 AP_INVOICE/PAYMENT/PUR_RETURN 对齐；库存物理冲销独立由业务侧 reverseApprove 链触发） | 对齐 reverseApprove 后置 REJECTED（原仅 posted=false 保留 APPROVED 的不对称已修复） |
| sales | `AR_INVOICE`（ErpSalInvoice）/`RECEIPT`（ErpSalReceipt）/`SAL_RETURN`（ErpSalReturn） | `approveStatus`: APPROVED → **REJECTED**；`posted=false`/`postedAt=null`/`postedBy=null` | sales 域 reverseApprove 镜像 purchase |
| sales | `SALES_OUTPUT`（ErpSalDelivery） | 经库存 `ErpInvStockMove` 反冲已出库（delivery 自身仅 posted=false） | 同 purchase receive 语义 |
| inventory | `OWNERSHIP_TRANSFER`（ErpInvOwnershipTransfer）/`INTER_TRANSFER`（ErpInvTransferOrder）/StockMove/StockTake | `posted=false`/`postedAt=null`/`postedBy=null`（inventory 单据无 approveStatus 状态机轴，仅 posted 翻转） | inventory 域既有 reversal 模式 |
| manufacturing | `SUBCONTRACT_ISSUE`/`SUBCONTRACT_RECEIPT`/`SUBCONTRACT_FEE`（ErpMfgSubcontractOrder，三段共用同一委外单，billHeadCode = `orderCode + "-SI"/"-SR"/"-SF"`，监听者去后缀反查 code） | `docStatus`: COMPLETED → **CANCELLED**；`posted=false`/`postedAt=null`/`postedBy=null`（委外单为 docStatus 驱动，无 approveStatus 回退——COMPLETED 时 approveStatus 已 APPROVED 且 CANCELLED 为终态） | 镜像 purchase receive 范式，回退字段差异：docStatus vs approveStatus |

> **判定原则**：回退目标态由各域自治（设计 `posting.md §反写契约` "域自治、引擎不持有源实体"）。引擎只持 `VoucherReversedEvent` 快照（含 billType+billCode+businessType+traceId），不反向 import 业务域模块（保持 DAG 顶层约束）。

### 业财回链表

```
VoucherBillR（业财回链）
  ├─ voucherHeadCode（凭证号）
  ├─ billType（业务类型）
  └─ billHeadCode（业务单据号）
```

- 每张业务生成的凭证通过回链表关联源单据。
- 回链是**双向**的：从凭证可查源单据，从单据可查凭证。
- 回链保证生命周期一致：作废单据 → 冲销凭证（方向一）；红冲凭证 → 回退单据（方向二）。

### 冲销的并发与审计

- **并发控制**：红冲期间对原凭证加锁防止并发修改。本项目用乐观锁（凭证 `docStatus` 状态约束 + 版本号）；不引入独立 LOCKED 状态（iDempiere 亦无，用行级锁）。
- **审批**：红冲属影响总账的高风险操作，需财务员权限；是否需双人审批按客户合规诉求配置（开源共识不内建冲销审批）。
- **审计轨迹**：原凭证与红字凭证经 `reversalOfVoucherId` 双向回链，全程留痕（见 `posting-log.md` §规则命中日志）。

## 反写契约

> 本节裁定本项目的反写哲学，澄清与"中台主动反写"模式的区别。详细三方对比与拒绝理由见 `docs/analysis/2026-07-04-finance-posting-engine-gap-vs-opensource.md`。

### 反写主体：域自治，引擎不持有源实体

- 过账引擎只持有 `PostingEvent` 快照（`billHeadCode` / `billData`），**不持有源业务实体的 ORM 引用**。
- 源业务单据的 `posted` 标志由**域调用方**在 `post()` / `reverse()` 成功返回后自行置位/回退。
- 这是 ADempiere 单体内 `Doc.post` 直接置源表 `Posted` 在多模块 DAG 架构下的正确演进——财务域处于 DAG 顶层，不应反向持有业务域实体引用。

### 反写数据载体：`posted` 字段 + 业财回链

- 反写语义由两个载体承载，**不引入独立反写记录表**：
  - 源业务单据的 `posted` 字段：标识"是否已过账"。
  - `ErpFinVoucherBillR` 业财回链：双向反查凭证与源单（凭证号 ↔ 单据号）。
- 此设计与 iDempiere/Metasfresh/Odoo/ERPNext 一致——主流开源 ERP 均无独立反写记录表。

### 反写时序：默认 SYNC 强一致

- 默认 SYNC：业务+库存+凭证同事务，`posted` 与凭证落库原子提交，无需独立反写步骤。
- ASYNC（可选）：post-commit 派发 `PostingEvent`，域调用方在异步过账成功回调中置位 `posted`；`posted=false` + 兜底扫描保证最终一致。
- 不强制异步（与"必须异步事件通知"的通用文章主张不同；ERPNext/赤龙亦用 SYNC）。

### 部分核销回写：由辅助账项承载

- AR/AP 的"部分核销反写"由 `ErpFinArApItem` 辅助账项机制承载，**不是引擎反写**——核销回写辅助账项的 `settledAmount/openAmount/status` 是领域模型事实（见 `ar-ap-reconciliation.md`）。
- 引擎不参与核销回写，仅生成核销引发的凭证（如汇兑损益凭证）。

### 反写方向汇总

| 场景 | 驱动方 | 反写动作 | 载体 |
|------|--------|----------|------|
| 正常过账成功 | 域调用方调 `post()` | 域置源单 `posted=true` | `posted` 字段 + 业财回链 |
| 业务单据作废 | 业务域 | 域先回退自身状态，再调 `reverse()` 红冲凭证 | 域状态机 + 业财回链 |
| 凭证红冲（财务侧） | 财务员调 `reverse()` | 引擎发 `VoucherReversedEvent`，域监听回退自身状态 | 事件 + 业财回链 |
| AR/AP 部分核销 | finance 核销动作 | 核销回写辅助账项 `settledAmount/openAmount/status` | `ErpFinArApItem` 辅助账项 |

## 多币种处理

- 业务单据引用币种，金额按**业务日期汇率**转换本位币（符合 ASC 830 / IAS 21 "交易发生日确认"原则）。
- 凭证分录行同时记录：
  - 源币种金额（`amountSource`）
  - 本位币金额（`amountFunctional`）
  - 币种编码（`currencyCode`）
  - 汇率（`exchangeRate`）—— 业务日期当天的汇率
- 汇率由主数据域提供；缺失汇率时报错而非静默使用默认值。
- **汇率锁定时机**：本位币金额在业务单据创建时按业务日期汇率锁定，过账时不重新计算。汇率差异在期末汇兑损益调整中统一处理（见 `domain-design-guidelines.md` §十二）。

### 实现契约

- `VoucherFact` 双金额字段：Provider 显式填充 `amountSource`（源币种）+ `amountFunctional`（= source × `ctx.exchangeRate`）；`amount` 字段保留作功能金额（`balanceTotals`/`assertBalanced` 以本位币为准）。未设置新字段时 fallback 到 `amount`（单币种向后兼容）。
- 引擎忠实写库 `line.amountSource`/`line.amountFunctional`/`line.debitAmount`/`line.creditAmount`（debit/credit 按本位币），实现细节见 `docs/architecture/processor-extension-pattern.md`。
- **多币种折算路径注记（P1-MA3-039，R1.9 已核实）**：`ErpFinPostingProcessor.persistVoucher` 按 Provider 显式传递的 `fact.amountSource`/`fact.amountFunctional` 写库；Provider 未设置时 fallback 到 `fact.amount`（**单币种场景 source==functional==amount，三者相等无币种折算**）。P2P/O2C Provider 已显式传双字段（多币种折算生效）；其余域单币种 fallback。完整多币种源币金额的全域迁移 successor 见下条。
- 辅助账项按 `event.exchangeRate` 折算 `amountFunctional`（= source × rate），`amountSource` = 源币种金额。
- P2P（purchase 域 Provider）+ O2C（sales 域 Provider）已迁移双字段；其余域 Provider 单币种 fallback（全域迁移 successor，`Deferred But Adjudicated`）。
- 收款核销汇兑损益 plug 见 `ar-ap-reconciliation.md §汇兑损益核销规则`。

## 多套科目表并行

- 支持多套会计科目表（`AcctSchema`）：管理账、税务账、集团合并账等。
- 同一业务单据在多套科目表下各生成一组凭证。
- 每套科目表有独立的本位币、科目体系、成本核算方法。
- 凭证分录行记录所属 `acctSchemaId`。

## 与其他域的协作总结

| 对端域 | 协作内容 |
|--------|----------|
| purchase | purchase 实现 `IErpFinAcctDocProvider`，处理采购相关凭证生成 |
| sales | sales 实现 `IErpFinAcctDocProvider`，处理销售相关凭证生成 |
| inventory | inventory 实现 `IErpFinAcctDocProvider`，处理存货估值凭证 |
| master-data | 引用科目表/科目/币种主数据 |

财务域处于 DAG 顶层，不依赖具体业务域的实现细节，只通过 `IErpFinAcctDocProvider` 接口聚合各域的凭证生成规则。

## 承付（COMMITMENT）实际过账（A2）

> A2 实现 [`budget.md §业务规则3`](budget.md) 既定义的承付过账逻辑：采购订单 APPROVED 时生成 postingType=COMMITMENT 凭证；订单 CANCELLED 或被发票接收时红冲。详见 [`budget.md §承付会计`](budget.md#承付会计a2)。

### 3 接入点（严格对齐 budget.md:78 业务规则）

| # | hook 点 | 时机 | 动作 | 事务边界 |
|---|---------|------|------|---------|
| 1 | **commit** | `ErpPurOrder.approve` 后置 | 生成 COMMITMENT 凭证（Dr 承付占用科目） | **SYNC 同事务**（与既有 `IErpFinBudgetControlBiz.check()` 强一致） |
| 2 | **release-on-cancel** | `ErpPurOrder.reverseApprove` / `cancel` | 红冲原 COMMITMENT 凭证 | SYNC 同事务（与既有 reverseApprove 同事务） |
| 3 | **release-on-invoice-approve** | `ErpPurInvoice.approve`（**AP 发票过账 = 实际占用产生 = 释放承付**） | 红冲原 COMMITMENT 凭证 | SYNC 同事务 |

### reject release-receive-complete（ErpPurReceive 入库路径）

`ErpPurReceive.approve`（采购入库）是**库存移动**（inventory 物理入库），**不产生 AP ACTUAL 占用**。承付不应在入库时释放——业务规则 budget.md:78 "订单 CANCELLED 或被发票接收时红冲" 中的 "被发票接收" = `ErpPurInvoice.approve`（AP 发票过账产生 ACTUAL 应付），**不是** `ErpPurReceive.approve`。在入库时释放承付会导致 actual + commitment 双重占用预算（红冲 commitment 但 actual 尚未产生）。

### 与既有 `IErpFinBudgetControlBiz.check()` 协同

| SPI | 调用点 | 数据载体 | 强弱一致 |
|-----|--------|---------|---------|
| `IErpFinBudgetControlBiz.check()` | purchase/sales 域审核事务内 | `ErpFinBudgetControlLog`（审计日志，不落库占用） | SYNC 强一致 |
| `IErpFinBudgetCommitmentBiz.commit()` | `ErpPurOrder.approve` 后置 | `ErpFinVoucher` + `VoucherLine`（承付凭证，落库占用） | SYNC 强一致 |
| `IErpFinBudgetCommitmentBiz.release()` | `ErpPurOrder.reverseApprove/cancel` + `ErpPurInvoice.approve` | `ErpFinVoucher` + `VoucherLine`（红冲凭证，释放占用） | SYNC 强一致 |

**协同关系**：check 是余量校验（不落库占用），commit 是实际占用（落库 COMMITMENT 凭证），release 是占用释放（红冲凭证）。三者事务边界均为 SYNC 同事务（release 不走事件总线 ASYNC，避免事务跨域复杂度）。

### config-gated 启用

承付过账经 `erp-fin.budget-commitment-enabled`（默认 false）控制：
- 默认关闭：保护既有 113 purchase 测试不触发承付凭证（config-gated 回归安全）。
- 启用时必配 `erp-fin.budget-commitment-subject-code`（采购承付占用科目编码）；缺失时抛 `ERR_BUDGET_COMMITMENT_SUBJECT_NOT_CONFIGURED`。
- sales 承付经同一总开关启用，科目经 `erp-fin.budget-commitment-sales-subject-code` 独立配置（收入面科目）；billType 按 sourceBillType 派发（PURCHASE_ORDER → PURCHASE_ORDER_COMMITMENT，SALES_ORDER → SALES_ORDER_COMMITMENT），详见 [`budget.md §sales 承付扩展`](budget.md#sales-承付扩展)。

### 承付凭证 Provider

承付凭证 Provider 实现 `IErpFinAcctDocProvider`：
- 与 BUDGET 同型：**不走 Provider 路由**（声明支持的业务类型集合为空），承付凭证直接由专用生成器写入。
- 存在仅为：文档化承付科目解析约定 + 满足接口约定 + 为 successor（多维承付科目解析）保留接入点。

### 错误码

| 错误码 | 含义 |
|--------|------|
| `ERP_FIN_BUDGET_COMMITMENT_ALREADY_RELEASED` | 重复 release 守卫（原 COMMITMENT 凭证已红冲或不存在） |
| `ERP_FIN_BUDGET_COMMITMENT_SUBJECT_NOT_CONFIGURED` | 启用承付但未配置承付科目编码 |

### 浏览器层验证

承付过账三接入点（commit / release-on-cancel / release-on-invoice-approve）经浏览器层全栈覆盖，验证范式与 BUDGET 同型（helper 按 `reversalOfVoucherId` 区分正向/红冲凭证）。详见 [`budget.md §承付会计 §浏览器层验证`](budget.md#浏览器层验证)。

## 跨法人内部交易凭证（A3）

> A3 实现 [`multi-company.md §跨公司交易生命周期状态机`](../architecture/multi-company.md) 既定义的跨法人调拨凭证生成逻辑。跨法人调拨 `ErpInvTransferOrder.confirm` 后置经 finance SPI 生成配对内部销售/采购凭证；同法人保持现状（仅库存移动）。

### 凭证生成路径（与 COMMITMENT 同型，不走 Provider 路由）

| 机制 | 实现 | 说明 |
|------|------|------|
| 跨法人判定 | finance SPI | warehouse.orgId 沿 parentId 链向上找首个 orgType=COMPANY |
| 转移定价解析 | `IErpFinTransferPriceResolver` SPI | 3 策略 cost-plus/market/negotiated + 优先级链 + 缓存 |
| 配对凭证生成 | 专用生成器 | AR 侧（INTERCOMPANY_SALE）+ AP 侧（INTERCOMPANY_PURCHASE），各 2 行 Dr/Cr |
| 科目解析 | A1 `IErpFinGlMappingResolver` | 按 INTERCOMPANY_AR/AP/REVENUE/COST accountKey + intercompany 维度解析 |
| intercompany 凭证 Provider | 文档化约定 | 声明支持的业务类型集合为空，与 BUDGET/COMMITMENT 同型 |

### config-gated 启用

跨法人内部交易凭证经 `erp-fin.intercompany-posting-enabled`（默认 false）控制：
- 默认关闭：保护既有 inventory 调拨测试不触发自动凭证（config-gated 回归安全）。
- 调拨确认失败不阻塞库存移动（凭证生成异常 try-catch 兜底，保持库存与凭证解耦）。

### PO/SO 触发路径扩展

> 将 intercompany 凭证生成从单一 inventory transfer confirm 扩展至跨公司 PO/SO approve/reverseApprove。完整生命周期设计与决策记录见 [`multi-company.md §跨公司 PO/SO 触发路径`](../architecture/multi-company.md#跨公司-poso-触发路径-expand)。

**PO/SO 接入点表**：

| 单据 | Processor | approve 钩子（后置） | reverseApprove 钩子（前置红冲） | 金额来源 | config-gate |
|------|-----------|---------------------|-------------------------------|---------|-------------|
| ErpPurOrder | approve 后置 | intercompany approve hook | intercompany reverse hook | `totalAmountWithTax`（本位币） | `erp-fin.intercompany-posting-enabled`（复用，默认 false） |
| ErpSalOrder | approve 后置 | intercompany approve hook | intercompany reverse hook | `totalAmountWithTax`（本位币） | 同上 |

**SPI 扩展**（`IErpFinIntercompanyTransferBiz`，向后兼容，`onTransferConfirmed` 不变）：

| 方法 | 触发点 | 入参 | 出参 |
|------|--------|------|------|
| `onTradeDocumentApproved` | PO/SO approve 后置 | docType + docId + docCode + executingOrgId + amount + businessDate | 配对凭证 ID 列表（AR + AP） |
| `onTradeDocumentReversed` | PO/SO reverseApprove 前置 | docType + docId + docCode | 红冲凭证 ID 列表 |

**跨法人判定**（全在 finance 域，AP-7 合规）：执行方法人根 = finance SPI 解析 `order.orgId`；对手方法人根 = 转移定价规则表反向查找（PO 查 toOrgId=执行方、SO 查 fromOrgId=执行方）。同法人 skip；钩子非阻塞 try-catch（对齐 inventory confirm 范式）。

**receive/delivery 联级**：归 Deferred successor（订单级已表达跨法人交易，联级为增强，避免重复计量）。
