# 业财打通机制（过账）

## 目的

说明业务单据如何自动生成会计凭证，包括凭证模板、过账引擎、科目映射、异步与冲销机制。

本文件是 `flow-overview.md` L3 节"业财打通"的详细展开。

## 总体架构

```
[业务单据审核通过]
      │
      ▼
[发布过账事件]（含 businessType + 单据数据）
      │
      ▼ post-commit 异步
[ErpFinAcctDocRegistry 查找 Provider]
      │
      ▼ 按 businessType 路由
[IErpFinAcctDocProvider.createFacts()]
      │
      ▼ 读取凭证模板 + 科目映射
[生成凭证分录行]
      │
      ▼ 校验借贷平衡
[写入凭证 + 业财回链 + 更新 posted=true]
```

## 业务类型映射

每种业务单据对应一个 `businessType`，决定使用哪个凭证模板：

| 业务单据 | businessType | 借贷方向（典型） | 触发域 |
|----------|--------------|------------------|--------|
| 采购入库 | PURCHASE_INPUT | 借：存货 / 贷：暂估应付 | purchase |
| 销售出库 | SALES_OUTPUT | 借：结转成本 / 贷：存货 | sales |
| 采购发票 | AP_INVOICE | 借：费用/采购 / 借：进项税 / 贷：应付 | purchase |
| 销售发票 | AR_INVOICE | 借：应收 / 贷：收入 / 贷：销项税 | sales |
| 付款 | PAYMENT | 借：应付 / 贷：银行存款 | purchase |
| 收款 | RECEIPT | 借：银行存款 / 贷：应收 | sales |

> 具体借贷科目取决于科目映射配置（见下文"科目映射"），上表是典型场景。

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
  ├─ @Inject List<IErpFinAcctDocProvider>
  └─ getProvider(businessType) → IErpFinAcctDocProvider
```

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

### 异步机制

- 业务单据审核通过 → 主事务落单据 + `posted=false` + 发布过账事件。
- 过账在 post-commit 异步执行（不阻塞业务单据审核响应）。
- 过账失败可重试，不影响主单据状态。

### posted 标志兜底

- 业务单据带 `posted` 字段（boolean）。
- 定期兜底扫描（定时任务）：扫描 `posted=false` 且已审核超过 N 分钟的单据，重新触发过账。
- 处理异步事件丢失、服务重启等异常场景。

### 失败处理策略

| 失败类型 | 处理 |
|----------|------|
| 模板缺失 | 报错并标记，等待人工配置模板后重试 |
| 科目映射缺失 | 报错并标记，等待人工配置科目映射 |
| 借贷不平衡 | 报错（通常是模板配置错误），人工介入 |
| 期间已结账 | 报错，需反结账或计入当前开启期间 |
| 系统异常 | 自动重试（指数退避），超过阈值告警 |

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

- 业务单据引用币种，金额按业务日期汇率转换本位币。
- 凭证分录行同时记录：
  - 源币种金额（`amountSource`）
  - 本位币金额（`amountFunctional`）
  - 币种编码（`currencyCode`）
  - 汇率（`exchangeRate`）
- 汇率由主数据域提供；缺失汇率时报错而非静默使用默认值。

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
