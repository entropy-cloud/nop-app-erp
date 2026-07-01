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
│     ├─ 库存写入（stock_move / stock_ledger / stock_balance）           │
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
│     ├─ ErpFinAcctDocRegistry 按 businessType 路由 Provider              │
│     ├─ IErpFinAcctDocProvider.createFacts() 生成分录                    │
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

| 字段 | 类型 | 说明 |
|------|------|------|
| `businessType` | String | 业务类型枚举（如 PURCHASE_INPUT、AR_INVOICE） |
| `billHeadCode` | String | 业务单据编码（幂等键） |
| `tenantId` | String | 租户 ID |
| `acctSchemaId` | String | 账套 ID |
| `billData` | Object | 单据数据（头+行，含金额、科目映射维度） |

### 幂等保证

过账操作前置检查 `posted=true` 时直接跳过，防止重复过账。兜底扫描与事件回调可能同时命中同一单据，posted 检查确保只处理一次。

## businessType vs billType 分工

`businessType` 与 `billType` 是**两个正交标识**，职责不同、非一对一，二者在业财回链表 `voucher_bill_r` 中同时落库：

| 标识 | 职责 | 取值来源 | 典型值 | 承载位置 |
|------|------|----------|--------|----------|
| `billType` | **源单识别 / 回链反查**（对应具体 ORM 实体/表） | `data-dependency-matrix.md §5.2` 枚举 | `PUR_RECEIVE`、`SAL_DELIVERY` | 弱指针三元组 `(billType, billHeadCode, lineCode)` |
| `businessType` | **过账语义 / 凭证模板路由**（会计事件分类） | 本节 §业务类型映射（唯一权威源） | `PURCHASE_INPUT`、`AR_INVOICE` | `PostingEvent.businessType`、凭证模板路由键 |

> **非 1:1 关系**：一个 `billType` 可映射多个 `businessType`。例如 `billType=PUR_RECEIVE`（采购入库单）在不同环节触发不同会计事件——入库时 `businessType=PURCHASE_INPUT`（暂估应付），收到发票时 `businessType=AP_INVOICE`（进项税+应付）。回链表 `voucher_bill_r` 同时存两者，便于"按源单反查"（用 billType）与"按会计语义聚合"（用 businessType）。

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

### 接口设计

财务域定义凭证生成接口与注册中心：

```
IErpFinAcctDocProvider（凭证生成 Provider）
  ├─ getSupportedBusinessTypes() → Set<BusinessType>
  └─ createFacts(billData, acctSchema) → List<VoucherLine>

ErpFinAcctDocRegistry（注册中心）
  ├─ providerMap: Map<BusinessType, IErpFinAcctDocProvider>  // 编译期类型安全
  ├─ @Inject List<IErpFinAcctDocProvider>  // 启动时收集所有 Provider Bean
  └─ getProvider(businessType) → IErpFinAcctDocProvider  // O(1) Map 查找
```

> **类型安全注册**：参考 Metasfresh 的 `ImmutableMap<String, AcctDocFactory>` 模式。注册中心启动时遍历所有 Provider Bean，按 `getSupportedBusinessTypes()` 建立 `BusinessType → Provider` 映射。运行时按 `businessType` 直接 Map 查找（O(1)），而非遍历 List（O(n)）。重命名 Provider 类不会导致注册失败（无反射字符串依赖）。

### 跨域自动聚合

各业务域（purchase/sales/inventory）可各自实现 `IErpFinAcctDocProvider` 并注册为 Bean：

- purchase 工程实现 `PurAcctDocProvider`（处理 AP_INVOICE/PAYMENT/PURCHASE_INPUT）。
- sales 工程实现 `SalAcctDocProvider`（处理 AR_INVOICE/RECEIPT/SALES_OUTPUT）。
- inventory 工程实现 `InvAcctDocProvider`（处理存货估值）。

财务域的 `ErpFinAcctDocRegistry` 通过 `@Inject List<IErpFinAcctDocProvider>` 自动聚合所有 Provider，按 `businessType` 路由。

**新增业务类型 = 新增 Provider Bean，零改动财务核心**——这是模块化业财一体的关键。

### 注册方式

使用类型安全的 Map 注册（避免反射命名约定）：

```
ErpFinAcctDocRegistry 维护 ImmutableMap<BusinessType, IErpFinAcctDocProvider>
  - 启动时收集所有 Provider Bean
  - 按 getSupportedBusinessTypes() 建立 businessType → Provider 映射
  - 运行时按 businessType 查 Provider
```

### 凭证写库前校验扩展点（IErpFinFactsValidator）

> 参考 Metasfresh 的 `IFactsValidator` 扩展机制。允许第三方在凭证写库前对借贷分录行做业务校验或改写（如按租户定制借贷规则、按维度分摊、特殊行业调整）。

**接口设计**：

```
IErpFinFactsValidator（凭证分录校验/改写扩展点）
  ├─ validate(facts: List<VoucherLine>, context: AcctDocContext) → List<VoucherLine>
  │   ├─ 可校验：借贷平衡、科目有效性、维度完整性
  │   ├─ 可改写：调整分录行（如按部门分摊金额）、追加分录行（如计提附加税）
  │   └─ 可拒绝：throw NopException 阻止过账（如不满足行业合规要求）
  └─ getOrder() → int  // 多个 Validator 的执行顺序
```

**注册机制**（与 Provider 同模式）：

```
ErpFinAcctDocRegistry
  ├─ @Inject List<IErpFinAcctDocProvider> providers   // 生成借贷分录
  └─ @Inject List<IErpFinFactsValidator> validators   // 校验/改写分录（可选，可多个）

过账流程：
  1. Provider.createFacts() → 原始分录行
  2. 按顺序调用所有 FactsValidator.validate() → 校验/改写后的分录行
  3. 最终借贷平衡校验 → 写库
```

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
| **库存一致性** | 第①层：业务单据 + 库存写入（stock_move/ledger/balance）永远在同一 `@BizMutation` 事务强一致 | ❌ 不可配（物理库存正确性硬约束） |
| **凭证时序** | 第②层最终一致（posted 标志 + 兜底保证） | ✅ 按 `(billType, acctSchemaId)` 切 SYNC 同事务 / ASYNC post-commit |
| **幂等** | posted 标志前置检查，重复过账直接跳过 | ❌ 不可配 |
| **业财回链** | `voucher_bill_r` 同时存 billType + businessType，双向可查 | ❌ 不可配 |
| **物理锁定** | 过账中对单据加锁，防止并发过账 | ❌ 不可配 |
| **可补偿** | 红字冲销（见 §冲销机制） | ❌ 不可配 |
| **可审计** | 凭证 + 回链 + posted 翻转全程留痕 | ❌ 不可配 |
| **默认模式** | — | ✅ 默认 SYNC；性能瓶颈时个别 billType 切 ASYNC |

> **判定原则**：可配的**仅**"凭证生成时序"一项。任何声称"库存可异步""幂等可关闭""回链可省略"的设计都违反稳定约束。

## 冲销机制

### 业务单据作废 → 凭证冲销

- 业务单据作废/反审核时，按业财回链表反查关联的已过账凭证。
- 生成红字冲销凭证（金额取负），关联原凭证与作废的业务单据。
- 红字凭证走正常"草稿→已过账"流程。

### 业财回链表

```
VoucherBillR（业财回链）
  ├─ voucherHeadCode（凭证号）
  ├─ billType（业务类型）
  └─ billHeadCode（业务单据号）
```

- 每张业务生成的凭证通过回链表关联源单据。
- 回链是**双向**的：从凭证可查源单据，从单据可查凭证。
- 回链保证生命周期一致：作废单据 → 冲销凭证；作废凭证 → 标记单据异常。

## 多币种处理

- 业务单据引用币种，金额按**业务日期汇率**转换本位币（符合 ASC 830 / IAS 21 "交易发生日确认"原则）。
- 凭证分录行同时记录：
  - 源币种金额（`amountSource`）
  - 本位币金额（`amountFunctional`）
  - 币种编码（`currencyCode`）
  - 汇率（`exchangeRate`）—— 业务日期当天的汇率
- 汇率由主数据域提供；缺失汇率时报错而非静默使用默认值。
- **汇率锁定时机**：本位币金额在业务单据创建时按业务日期汇率锁定，过账时不重新计算。汇率差异在期末汇兑损益调整中统一处理（见 `domain-design-guidelines.md` §十二）。

## 多套科目表并行

- 支持多套会计科目表（`AcctSchema`）：管理账、税务账、集团合并账等。
- 同一业务单据在多套科目表下各生成一组凭证。
- 每套科目表有独立的本位币、科目体系、成本核算方法。
- 凭证分录行记录所属 `acctSchemaId`。

## 与其他域的协作总结

| 对端域 | 协作内容 |
|--------|----------|
| purchase | purchase 实现 `PurAcctDocProvider`，处理采购相关凭证生成 |
| sales | sales 实现 `SalAcctDocProvider`，处理销售相关凭证生成 |
| inventory | inventory 实现 `InvAcctDocProvider`，处理存货估值凭证 |
| master-data | 引用科目表/科目/币种主数据 |

财务域处于 DAG 顶层，不依赖具体业务域的实现细节，只通过 `IErpFinAcctDocProvider` 接口聚合各域的凭证生成规则。
