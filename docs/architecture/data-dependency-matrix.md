# 数据依赖矩阵：多模块之间的数据如何依赖

> **状态**：已生效架构文档（数据层依赖真相源）。落地实施（codegen 生成 `I*Biz` 接口）仍受 `<domain>/model/*.orm.xml` ask-first 保护区域约束。
>
> **本文定位**：`module-boundaries.md` 的**数据层细化**。`module-boundaries.md` 管"模块级依赖方向（DAG）"，本文管"表级数据依赖关系"——具体回答三个问题：
> 1. 哪些模块**只读依赖**哪些表？
> 2. 哪些模块**同事务同步修改**哪些表？
> 3. 跨模块数据引用通过**什么字段**建立？

## TL;DR

nop-app-erp 共 **18 个业务域、279 个实体表**（见 `domain-module-split-analysis.md §2.0` 与 `docs/context/codebase-map.md`）。跨域数据依赖遵循三条铁律：

1. **只读依赖**：所有业务域对 `master-data` 表族（物料/往来单位/仓库/科目/币种等）是**只读引用**，通过纯字符串/ID 外键列承载，不做 ORM 强引用。
2. **同步修改仅限业财一体闭环**：业务单据状态变迁时，**同事务内**写"本域单据表 + inventory 流水/余额表 + finance 凭证/回链表"。这是唯一的跨域写场景。
3. **跨业务域引用一律走弱指针**：`relatedBillType` / `sourceBillType` + `relatedBillCode` / `sourceBillCode` + `sourceLineCode` 三元组（字符串），**零跨业务域 ORM 引用**。

## 1. 三种依赖类型定义

跨模块数据依赖分三类，每类的实现机制、事务语义、演进自由度完全不同：

| 类型 | 实现机制 | 事务语义 | 演进自由度 | 典型场景 |
|---|---|---|---|---|
| **R（只读引用）** | 纯外键列（ID 或字符串），无 `<to-one>` 关系声明；BizModel 通过 `@Inject I*Biz` 只读查询 | 无（读已提交即可） | 高（被引用方表结构变更不影响引用方的列） | 所有业务域引用 master-data 主数据 |
| **S（同步修改）** | 同一 `@BizMutation` 方法内、同一事务上下文写多个表 | 强一致（同事务原子提交） | 中（跨域写需被引用方提供 `I*Biz` 写方法） | 业财一体：业务单据过账写凭证 + 库存 |
| **P（弱指针反查）** | 字符串三元组 `(billType, billCode, lineCode)`，应用层反查 | 无（指针只存值，不约束完整性） | 极高（双方表结构独立演进） | 库存移动反查采购单、凭证反查业务源单 |

> **关键区分**：R 与 P 都是"跨域无 FK 约束"，但 R 指向**主数据**（稳定、近静态），P 指向**业务单据**（动态、有生命周期）。主数据用 ID 列承载，业务单据用字符串三元组承载——因为业务单据的"类型"本身需要被表达（PO/INPUT/DELIVERY 等枚举）。

## 2. 域级依赖矩阵（L1 已稳定层）

> 本节是**已定稿**的域级依赖关系，源自 `module-boundaries.md` 的 DAG。表中的"读/写"列指向本节后续的表级清单。

### 2.0 裁决原则（依赖方向冲突时的优先级）

当多份文档对同一依赖方向的描述出现冲突时，按以下 4 条优先级裁决（高层架构依据：`architecture-principles.md §二` 确认 DAG 按依赖类型分层校验）：

1. **ORM 层引用**：以本文 §5.6.2 实测清单（`notGenCode` 外部实体 to-one）为**最高权威**——已 DAG 验证零循环，代码即真相。
2. **S 写（同步修改）层**：以本文 §4.2 的 S 写表集矩阵为权威。
3. **模块级摘要**：`module-boundaries.md` 中文表是模块级依赖摘要；当与本文数据层清单冲突时，回改 `module-boundaries.md` 以匹配本文（数据层更细）。
4. **分层独立校验**：ORM 引用与 S 写/事件触发属不同依赖类型，允许跨层"看似双向"——只要每层内部单向无环即合法（如 ORM finance→projects + S 写 projects→finance 在不同层，不构成循环）。

### 2.1 依赖方向总览（DAG）

> **完整 18 域的物理目录 × 逻辑工程名 × appName 映射见 `domain-module-split-analysis.md §2.0`**。本图按逻辑工程名标注依赖方向。下方 DAG 仅刻画**已稳定的核心依赖**（master-data 根 + 业财一体闭环）；第二批 8 个扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b）在 ORM 层均只引用 master-data，业务层关联待各域设计深化后补充。

```
L0 根域（被所有业务域 R 只读引用）：
  app-erp-master-data ── 被全部 17 个业务域 R 引用（~120+ 处）

L1 直接下游（R: master-data；无业务域间 ORM 引用）：
  app-erp-inventory    app-erp-projects    app-erp-quality
  app-erp-hr           app-erp-cs          app-erp-crm
  app-erp-contract     app-erp-drp         app-erp-aps
  app-erp-logistics    app-erp-b2b

L2 业务域（R: master-data + inventory；S 写 finance 凭证 + inventory 库存）：
  app-erp-purchase     app-erp-sales
        │                     │
        └──── 业财一体：过账同事务 S 写 finance ────┐
                                                    ↓
L3 顶域（业财一体核心，被多业务域 S 写，不反向写业务）：
  app-erp-finance  ← R+S（被 purchase/sales/assets/projects/manufacturing/maintenance S 写）

外围第一批扩展域（R: master-data + inventory/其他；S 写 finance；事件触发）：
  app-erp-assets        （R: master-data + inventory；S 写 finance：折旧/处置过账）
  app-erp-manufacturing （R: master-data + inventory；S 写 finance；事件触发 quality 检验）
  app-erp-maintenance   （R: master-data + inventory + assets；S 写 finance）
```

> **关于 8 个第二批扩展域的业务层关联**（crm/cs/hr/aps/contract/drp/logistics/b2b）：当前 ORM 层实测仅引用 master-data（详见 §5.6.2）。它们是否参与业财过账（S 写 finance）、是否与其他业务域有弱指针（P）关联，待各域业务设计深化后在 §2.2/§4.2/§5.1 补充。

### 2.2 域级依赖矩阵

| 引用方域 | 只读依赖（R）的域 | 同步修改（S）的域 | 被引用（P 反查源） |
|---|---|---|---|
| **master-data** | （无，根域） | （无） | 所有业务域（R）+ finance（P 反查业务源单） |
| **inventory** | master-data | （库存写自身表族，不跨域 S 写） | purchase/sales（S 写库存）/ manufacturing / maintenance / assets |
| **purchase** | master-data / inventory | finance（过账 S 写凭证） | inventory（P：receive→stock_move）/ finance（P：invoice→ar_ap_item） |
| **sales** | master-data / inventory | finance（过账 S 写凭证） | inventory（P：delivery→stock_move）/ finance（P：invoice→ar_ap_item） |
| **finance** | master-data / purchase / sales / inventory / assets / projects（全部 R，经 I*Biz 只读查源单） | （处于 DAG 顶，不被 S 写） | （凭证被业务域 P 反查） |
| **assets** | master-data / inventory | finance（折旧/处置过账 S 写凭证） | finance（P：折旧凭证反查资产） |
| **projects** | master-data | finance（成本归集过账 S 写凭证） | finance（P：成本归集反查源单）；purchase/sales（R：项目采购/销售单按 projectId 建 to-one 只读引用，归集项目成本） |
| **manufacturing** | master-data / inventory | finance（工单完工过账 S 写凭证）/ quality | inventory（P：领料→stock_move） |
| **quality** | master-data | （无 S 写，只被业务域引用） | purchase/sales/manufacturing（P：检验反查业务源） |
| **maintenance** | master-data / inventory / assets | manufacturing（停机影响排产）/ finance（维修领料过账） | inventory（P：备件领用→stock_move） |
| **crm** | master-data | （业务层 S/P 待深化） | （待业务设计补充） |
| **cs** | master-data | （业务层 S/P 待深化） | （待业务设计补充） |
| **hr** | master-data | （业务层 S/P 待深化，如薪资过账） | （待业务设计补充） |
| **aps** | master-data | （业务层 S/P 待深化，与 manufacturing 协作） | （待业务设计补充） |
| **contract** | master-data | （业务层 S/P 待深化） | （待业务设计补充） |
| **drp** | master-data | （业务层 S/P 待深化） | （待业务设计补充） |
| **logistics** | master-data | （业务层 S/P 待深化） | （待业务设计补充） |
| **b2b** | master-data | （业务层 S/P 待深化） | （待业务设计补充） |

### 2.3 三类依赖计数

| 域 | 入向 R（被引用为主数据） | 出向 R（只读引用主数据） | 出向 S（同步写他域） | 入向 S（被他域同步写） | P 指针字段数 |
|---|---|---|---|---|---|
| master-data | **~120**（根域，被所有业务行引用） | 0 | 0 | 0 | 0 |
| inventory | ~6（被 purchase/sales/mfg/mnt/ast 写） | 4 类主数据 | 0（仅自写） | **purchase/sales/mfg/mnt/ast** | 5 |
| purchase | 2（invoice/receive 被 finance 查） | 5 类主数据 + inventory | **finance** | 0 | 0 |
| sales | 2（invoice/delivery 被 finance 查） | 5 类主数据 + inventory | **finance** | 0 | 0 |
| finance | 0（顶域，不被 R 引用） | 6 域全 R | 0 | **purchase/sales/ast/prj/mfg/mnt** | 2 |
| assets | 1（被 finance 查） | 3 类主数据 + inventory | **finance** | 0 | 0 |
| projects | 1（被 finance 查） | 3 类主数据 | **finance** | 0 | 2 |
| manufacturing | 1（被 finance 查） | 3 类主数据 + inventory | **finance**/quality | 0 | 2 |
| quality | 1（被业务域查） | 2 类主数据 | 0 | （被业务域触发检验，非 S 写） | 4 |
| maintenance | 1（被 mfg 查） | 3 类主数据 + inventory + assets | **finance**/manufacturing | 0 | 0 |
| crm | 待业务深化 | ~7 类主数据（实测 31 to-one） | 待深化 | 待深化 | 待深化 |
| cs | 待业务深化 | ~3 类主数据（实测 5 to-one） | 待深化 | 待深化 | 待深化 |
| hr | 待业务深化 | ~6 类主数据（实测 19 to-one） | 待深化 | 待深化 | 待深化 |
| aps | 待业务深化 | ~3 类主数据（实测 6 to-one） | 待深化 | 待深化 | 待深化 |
| contract | 待业务深化 | ~4 类主数据（实测 10 to-one） | 待深化 | 待深化 | 待深化 |
| drp | 待业务深化 | ~3 类主数据（实测 7 to-one） | 待深化 | 待深化 | 待深化 |
| logistics | 待业务深化 | ~4 类主数据（实测 9 to-one） | 待深化 | 待深化 | 待深化 |
| b2b | 待业务深化 | ~5 类主数据（实测 15 to-one） | 待深化 | 待深化 | 待深化 |

> **关键观察**：`master-data` 是唯一的高入向 R 域（~120 处引用），它是整个系统的根。`finance` 是唯一的入向 S 顶域（被 6 个业务域同步写过账）。这两个域的稳定性直接决定全局稳定性。

## 3. 只读依赖（R）明细：哪些模块只读引用哪些表

> 本节回答"哪些模块只读依赖哪些表"。所有 R 依赖都通过**纯外键列**承载，BizModel 通过 `@Inject I*Biz` 只读查询，**零 ORM `<to-one>` 跨域声明**。

### 3.1 master-data 表族（被引用方，根域）

`master-data` 提供 20 张主数据表，按引用频次分三级：

| 主数据表 | 引用字段名（标准化） | 高频引用方（R） | 说明 |
|---|---|---|---|
| `erp_md_material` | `materialId` | inventory / purchase / sales / manufacturing / quality / maintenance / assets / finance | 物料，被所有业务行的物料引用 |
| `erp_md_material_sku` | `skuId` / `materialSkuId` | inventory / purchase / sales / manufacturing | SKU，批次/序列号管理的最小单位 |
| `erp_md_partner` | `partnerId` / `customerId` / `supplierId` | purchase / sales / finance / projects / manufacturing（subcontract）/ quality | 往来单位，customer/supplier 是同一表的角色 |
| `erp_md_warehouse` | `warehouseId` / `fromWarehouseId` / `toWarehouseId` | inventory / purchase / sales / manufacturing / quality / maintenance / assets / finance | 仓库 |
| `erp_md_location` | `locationId` / `fromLocationId` / `toLocationId` | inventory / assets / maintenance | 库位 |
| `erp_md_uom` | `uoMId` / `fromUoMId` / `toUoMId` | 所有持有数量的行表 | 计量单位 |
| `erp_md_currency` | `currencyId` | 所有持有金额的头/行表 | 币种 |
| `erp_md_organization` | `orgId` / `businessOrgId` | **所有业务表（无例外）** | 组织/公司，多公司核算维度 |
| `erp_md_tax_rate` | `taxRateId` / `inputTaxRateId` / `outputTaxRateId` | purchase / sales / inventory | 税率 |
| `erp_md_subject` | `subjectId` / `subjectCode` | finance / assets / projects | 会计科目 |
| `erp_md_acct_schema` | `acctSchemaId` | finance / inventory（成本层） | 账套（多套账并行） |
| `erp_md_employee` | `employeeId` / `managerId` / `staffId` | assets / projects / maintenance | 职员 |
| `erp_md_material_category` | `categoryId` | assets / maintenance | 分类（资产/设备借用） |
| `erp_md_settlement_method` | `settlementMethodId` | purchase / sales | 结算方式 |
| `erp_md_bank_account` | `bankAccountId` / `partnerBankAccountId` | purchase / sales | 银行账户 |

### 3.2 finance 域对业务域的只读依赖

`finance` 处于 DAG 顶，**不持有任何业务域的外键列**，而是通过 `I*Biz` 接口只读查询业务源单（凭证生成时取数）：

| finance 取数场景 | 调用的 I*Biz（codegen 后生成） | 用途 |
|---|---|---|
| 采购入库过账 | `IErpPurReceiveBiz` / `IErpPurInvoiceBiz` | 读源单取金额/税额/科目映射 |
| 销售出库过账 | `IErpSalDeliveryBiz` / `IErpSalInvoiceBiz` | 同上 |
| 库存估值过账 | `IErpInvStockMoveBiz` / `IErpInvStockLedgerBiz` | 读移动单/流水取成本 |
| 资产折旧过账 | `IErpAstDepreciationScheduleBiz` | 读折旧计划 |
| 项目成本归集 | `IErpPrjCostCollectionBiz` | 读成本归集单 |
| 制造工单完工 | `IErpMfgWorkOrderBiz` | 读工单完工量 |

> **关键规则**：finance 对业务域是**纯读**——从不写业务表。业财一体是"业务→财务"单向 S 写，财务不回写业务。

## 4. 同步修改（S）明细：业财一体的事务边界

> 本节回答"哪些模块同步修改哪些表"。所有 S 写都发生在**同一 `@BizMutation` 方法、同一事务**内，保证强一致。`@BizMutation` 自动包装事务（见 `ai-defaults.md`）。

### 4.1 业财一体闭环：以"采购入库过账"为例

> **凭证层时序可配**：下图的"凭证生成"段（第②层）按 `(billType, acctSchemaId)` 可切 **SYNC 同事务**（默认）或 **ASYNC post-commit**（性能瓶颈时）。库存段（第①层）恒定 SYNC 强一致。详见 `posting.md §总体架构` 与 `§稳定约束 vs 可配置策略`。

```
[app-erp-purchase] ErpPurReceiveBiz.confirmReceive()  @BizMutation
    │ 同一事务上下文（@BizMutation 自动 @Transactional）
    │
    │ ============ 第①层：业务单据 + 库存（强制 SYNC，不可配）============
    ├─ S 写 purchase：更新 erp_pur_receive.docStatus = CONFIRMED
    ├─ S 写 purchase：插入 erp_pur_receive_line（已存在，仅状态更新）
    ├─ posted = false（与单据+库存同事务落盘）
    │
    ├─ @Inject IErpInvStockMoveBiz.saveFromReceive()
    │   └─ S 写 inventory（强制 SYNC，物理库存正确性硬约束）：
    │       ├─ 插入 erp_inv_stock_move（移动单）
    │       ├─ 插入 erp_inv_stock_move_line（移动行）
    │       ├─ 插入 erp_inv_stock_ledger（不可变流水）
    │       └─ 更新 erp_inv_stock_balance（余额快照）
    │
    │ ============ 第②层：凭证生成时序（按 billType 可配 SYNC/ASYNC）============
    ├─ 方式 A（SYNC，默认）：同事务内立即生成凭证
    │   └─ @Inject IErpFinAcctDocProvider → ErpFinAcctDocRegistry → Provider
    │       ├─ S 写 finance：插入 erp_fin_voucher / voucher_line
    │       ├─ S 写 finance：插入 erp_fin_voucher_bill_r（业财回链）
    │       └─ 更新 posted = true（同事务）
    │   事务提交 → 业务单据 / 库存 / 凭证三者强一致
    │
    └─ 方式 B（ASYNC，可选）：txn().afterCommit() 解耦
        ├─ 主事务提交：单据 + 库存 + posted=false 落盘
        └─ post-commit 异步：发布 PostingEvent → Provider 生成凭证 → posted=true
            （由第③层 posted 兜底扫描保证最终一致）
```

### 4.2 同步修改表集矩阵（S 写清单）

每个业务闭环的 S 写涉及表集：

| 业务闭环 | 触发方域 | S 写表集 | 触发动作 |
|---|---|---|---|
| 采购入库 | purchase | `erp_pur_receive` + inventory 表族（4）+ finance 凭证表族（3） | `confirmReceive()` |
| 采购发票 | purchase | `erp_pur_invoice` + finance（凭证 + ar_ap_item + voucher_bill_r） | `confirmInvoice()` |
| 采购付款 | purchase | `erp_pur_payment` + finance（凭证 + ar_ap_item 核销） | `confirmPayment()` |
| 销售出库 | sales | `erp_sal_delivery` + inventory 表族（4）+ finance 凭证表族（3） | `confirmDelivery()` |
| 销售发票 | sales | `erp_sal_invoice` + finance（凭证 + ar_ap_item） | `confirmInvoice()` |
| 销售收款 | sales | `erp_sal_receipt` + finance（凭证 + ar_ap_item 核销） | `confirmReceipt()` |
| 库存调拨 | inventory | `erp_inv_transfer_order` + stock_move/ledger/balance（自写，不跨域） | `confirmTransfer()` |
| 生产领料 | manufacturing | `erp_mfg_material_issue` + inventory（stock_move 表族）+ finance 凭证 | `confirmIssue()` |
| 工单完工 | manufacturing | `erp_mfg_work_order` + inventory（入库 stock_move）+ finance 凭证 | `confirmWorkOrder()` |
| 资产折旧 | assets | `erp_ast_depreciation_schedule` + finance 凭证 | `runDepreciation()` |
| 资产处置 | assets | `erp_ast_disposal` + finance 凭证 | `confirmDisposal()` |
| 项目成本归集 | projects | `erp_prj_cost_collection` + finance 凭证 | `confirmCollection()` |
| 维修领料 | maintenance | `erp_mnt_spare_part_usage` + inventory（stock_move）+ finance 凭证 | `confirmUsage()` |

> **关键模式**：所有 S 写都是"业务域触发 → inventory（写库存）+ finance（写凭证）"。这是 13 个开源 ERP 验证过的业财一体共识（见 `docs/analysis/2026-06-22-0000-cross-domain-coupling-vs-microservice.md` §9.10）。

### 4.3 业财 SPI 注册机制（实现 S 写跨域的关键）

finance 域定义接口 + 注册中心，业务域提供 Provider Bean（Metasfresh 范式，见 `module-boundaries.md:57-59`）：

```
[app-erp-finance] 定义契约
    interface IErpFinAcctDocProvider {
        AcctDocContext buildContext(Object sourceBill);
        List<VoucherLine> generateVoucherLines(AcctDocContext ctx);
    }
    @Singleton class ErpFinAcctDocRegistry {
        @Inject List<IErpFinAcctDocProvider> providers;  // 平台自动聚合所有 Bean
    }

[app-erp-purchase] 提供实现
    @Component class PurReceiveAcctDocProvider implements IErpFinAcctDocProvider {
        // 仅当 sourceBillType = PUR_RECEIVE 时生效
    }
```

> **新增业务单据类型 = 新增一个 Provider Bean，零改动 finance 核心**。这是 Metasfresh `AcctDocRegistry` 的类型安全注册模式（见 `erp-survey/2026-06-22-0000-metasfresh.md:77-84`）。

### 4.4 同步修改的约束

- **禁止反向 S 写**：finance 不回写业务表，inventory 不回写业务单据表。S 写严格单向（业务→财务）。
- **禁止跨域 private 字段**：`@Inject` 的 `I*Biz` 字段不能是 `private`（nop IoC 规则，见 `AGENTS.md`）。
- **事务边界清晰**：所有 S 写必须在同一 `@BizMutation` 方法内，不依赖 `@Transactional` 显式传播（`@BizMutation` 自动包装）。
- **异步过账是可选优化**：Metasfresh 把过账异步化到 `post-commit` EventBus。本工程**默认 SYNC**（业务+库存+凭证同事务强一致），按 `(billType, acctSchemaId)` 可对个别高吞吐单据切 ASYNC（经 `txn().afterCommit()` 解耦），但**第①层库存写入恒定 SYNC、`posted` 字段兜底恒定生效**。完整可配边界见 `posting.md §稳定约束 vs 可配置策略`。

## 5. 弱指针（P）字段目录：跨业务域引用

> 本节回答"跨业务域的数据结构引用通过什么字段建立"。所有跨业务域引用（非主数据）一律走**字符串三元组弱指针**，零 ORM 跨域声明。

### 5.1 弱指针字段全集（从 9 个 orm.xml 实测归纳）

| 所在域 | 所在表 | 字段 | 指向的（类型+单号） |
|---|---|---|---|
| inventory | `erp_inv_stock_move` | `relatedBillType` + `relatedBillCode` | 采购入库单 / 销售出库单 / 调拨单 / 生产领料单 等 |
| inventory | `erp_inv_reservation` | `sourceBillType` + `sourceBillCode` | 销售订单 / 生产订单 等（预留来源） |
| inventory | `erp_inv_reservation_line` | `sourceLineCode` | 上述单据的行 |
| inventory | `erp_inv_picking_order` | `relatedBillType` + `relatedBillCode` | 销售出库 / 调拨 等（拣货来源） |
| manufacturing | `erp_mfg_mrp_demand` | `sourceBillType` + `sourceBillCode` | 销售订单 / 生产订单 / 安全库存 等（MRP 需求源） |
| projects | `erp_prj_cost_collection_line` | `sourceBillType` + `sourceBillCode` | 工时单 / 采购单 / 领料单 等（成本归集源） |
| finance | `erp_fin_ar_ap_item` | `sourceBillType` + `sourceBillCode` | 采购发票 / 销售发票 / 付款单 / 收款单（应收应付明细来源） |
| finance | `erp_fin_voucher_bill_r` | `billType` + `billHeadCode` | 任意业务单据（业财回链，反查源单） |
| quality | `erp_qa_inspection` | `relatedBillType` + `relatedBillCode` | 采购入库 / 生产工单 等（检验对象） |
| quality | `erp_qa_review` | `relatedBillType` + `relatedBillCode` | 不合格品 / 供应商评审 等 |

### 5.2 弱指针的枚举约定（`billType` / `relatedBillType` 取值）

> **`billType` vs `businessType` 分工**：`billType` 只管源单识别/回链（对应具体 ORM 实体/表，承载于弱指针三元组）；过账模板路由用 `businessType`（见 `docs/design/finance/posting.md §业务类型映射`），两者**非 1:1**（一个 billType 可映射多个 businessType，如 `PUR_RECEIVE` 对应 `PURCHASE_INPUT` 与 `AP_INVOICE`）。回链表 `voucher_bill_r` 同时落两者。

`billType` 是字符串枚举，在 `docs/design/domain-design-guidelines.md` 统一约定（见该文档"单据类型枚举"）。当前已出现的取值：

| 枚举值 | 含义 | 典型使用方 |
|---|---|---|
| `PUR_REQUISITION` | 采购请购单 | — |
| `PUR_ORDER` | 采购订单 | projects（项目采购归集） |
| `PUR_RECEIVE` | 采购入库单 | inventory（stock_move 反查）、quality（来料检验） |
| `PUR_INVOICE` | 采购发票 | finance（ar_ap_item） |
| `PUR_PAYMENT` | 采购付款单 | finance（ar_ap_item 核销） |
| `PUR_RETURN` | 采购退货单 | inventory |
| `SAL_QUOTATION` | 销售报价单 | — |
| `SAL_ORDER` | 销售订单 | inventory（reservation）、manufacturing（mrp_demand） |
| `SAL_DELIVERY` | 销售出库单 | inventory（stock_move）、quality |
| `SAL_INVOICE` | 销售发票 | finance（ar_ap_item） |
| `SAL_RECEIPT` | 销售收款单 | finance（ar_ap_item 核销） |
| `SAL_RETURN` | 销售退货单 | inventory |
| `INV_STOCK_MOVE` | 库存移动单 | finance（库存估值过账源） |
| `INV_TRANSFER` | 库存调拨单 | — |
| `INV_STOCK_TAKE` | 库存盘点单 | — |
| `MFG_WORK_ORDER` | 生产工单 | quality（完工检验）、manufacturing（MRP） |
| `MFG_MATERIAL_ISSUE` | 生产领料单 | projects（成本归集） |
| `AST_DEPRECIATION` | 资产折旧 | finance |
| `AST_DISPOSAL` | 资产处置 | finance |
| `PRJ_TIMESHEET` | 工时单 | projects（成本归集） |
| `MNT_VISIT` | 维护工单 | maintenance |

> **新增单据类型时**：必须在此表登记新枚举值 + 在 `domain-design-guidelines.md` 同步，否则弱指针反查会失效。

### 5.3 弱指针反查的标准实现

BizModel 反查弱指针源单的标准模式（codegen 后在 `*-service` 层实现）：

```java
// inventory 的 ErpInvStockMoveBiz 反查采购入库单
public ErpPurReceive getSourceReceive(IErpInvStockMove move) {
    if (!"PUR_RECEIVE".equals(move.getRelatedBillType())) return null;
    // 注入采购域的 I*Biz 只读查询
    return purReceiveBiz.findByCode(move.getRelatedBillCode());
}
```

> **禁止用 `IDaoProvider` / `IOrmTemplate` 直接跨域查表**（见 `AGENTS.md` "跨实体访问"规则）。跨域查询必须经 `I*Biz` 接口，确保业务规则封装。

## 5.5 跨模块关联查询：怎么做

> 本节回应核心问题："跨模块的关联查询、条件过滤、显示名带出怎么做？"
>
> 完整机制（四种范式 + 平台实测证据）见 `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`。本节给出本工程的标准做法。
>
> **关键认知校正**：跨模块并非"必须断开 ORM 关联"。平台 `entity.xdef` 明确定义了 `notGenCode` 属性——**"引用其他模块的实体类时设置此属性，避免在本模块生成其他模块的实体类"**，平台代码生成器 `orm-gen.xlib` 自身就演示了用 `notGenCode="true"` 引用 `nop-sys` 的 `NopSysExtField` 表建立 `<to-many>` 关联的标准范式。因此跨模块关联查询有两条主线：
>
> - **路线 1（机制 B，`notGenCode` 外部实体引用）**：在本模块 orm.xml 声明外部实体 + `notGenCode="true"`，建立 `<to-one>`，EQL 可点导航、GraphQL 可展开。
> - **路线 2（机制 D，纯外键 + `I*Biz`）**：断开 ORM 关联，用冗余显示名 + `@BizLoader` + EQL 子查询。
>
> 本工程按引用场景选用，详见 §5.5.3 决策表。

### 5.5.1 主数据三级字段策略（L1/L2/L3，配合路线 2）

每个跨模块引用字段按使用场景分三级，**用于纯外键引用（路线 2）**：

| 级别 | 字段策略 | 示例（采购单引用供应商） | 适用场景 |
|---|---|---|---|
| **L1 显示字段** | 本表冗余存显示名 | `supplierName` VARCHAR(200) | 列表高频显示，避免 join |
| **L2 外键字段** | 纯 ID/Code 列，**无 `<to-one>` 关系声明** | `supplierId` VARCHAR(50) | 关系引用载体 |
| **L3 详情展开** | `@BizLoader` + `requireBiz` 懒加载 | `getSupplier()` 返回 `ErpMdPartner` | 详情页按需带出完整对象 |

> **L1 冗余字段的维护**：主数据改名时，需同步刷新业务表的冗余显示名。两种做法：① 主数据 update 时发出事件，业务域订阅刷新；② 列表场景不依赖冗余字段，改用 `@BizLoader` 实时带出（牺牲性能换一致性）。本工程默认 ①，性能敏感且一致性要求低的场景用 ②。

### 5.5.2 关联查询的四种手法

| # | 手法 | 场景 | 代价 | 示例 |
|---|---|---|---|---|
| 1 | **冗余显示名字段** | 列表显示关联对象名，不需过滤 | 主数据改名需同步刷新 | `supplierName` 字段直接 SELECT |
| 2 | **BizModel + QueryBean + `in`** | 按关联条件过滤本表 | 两步查询，结果集大时性能差 | 先查 `ErpMdPartner` 取 ID 集，再 `in` 过滤 `ErpPurOrder` |
| 3 | **`@BizLoader` + `requireBiz`** | 详情页带出完整关联对象 | N+1（列表场景慎用） | `getSupplier()` 懒加载 |
| 4 | **EQL 子查询** | 报表/复杂统计查询 | 需写 sql-lib | `WHERE o.supplierId IN (SELECT p.id FROM ErpMdPartner p WHERE ...)` |

### 5.5.3 标准代码模式

**(a) 冗余显示名（L1，列表场景）**

```xml
<!-- orm.xml: 纯外键 + 冗余显示名 -->
<column name="supplierId" precision="50" stdSqlType="VARCHAR"/>
<column name="supplierName" precision="200" stdSqlType="VARCHAR"/>
```

```java
// BizModel: 写入时同步冗余字段
order.setSupplierId(supplier.getId());
order.setSupplierName(supplier.getName());
```

**(b) BizModel 过滤查询（手法 2）**

```java
@BizQuery
public PageBean<ErpPurOrder> findOrders(@Name("supplierName") String supplierName,
                                        IServiceContext ctx) {
    QueryBean supplierQuery = new QueryBean();
    supplierQuery.addFilter(FilterBeans.like("name", supplierName));
    List<ErpMdPartner> suppliers = mdPartnerBiz.findList(supplierQuery, null, ctx);
    Set<String> supplierIds = suppliers.stream()
        .map(ErpMdPartner::orm_idString).collect(toSet());
    if (supplierIds.isEmpty()) return PageBean.empty();

    QueryBean orderQuery = new QueryBean();
    orderQuery.addFilter(FilterBeans.in("supplierId", supplierIds));
    return dao().findPageByQuery(orderQuery, ctx);
}
```

**(c) `@BizLoader` 详情展开（L3）**

```java
@BizObjName("ErpPurOrder")
public class ErpPurOrder extends _ErpPurOrder {
    @BizLoader
    @LazyLoad
    public ErpMdPartner getSupplier() {
        if (StringHelper.isBlank(getSupplierId())) return null;
        return requireBiz(IErpMdPartnerBiz.class)
            .getEntity(getSupplierId(), IServiceContext.requireCtx());
    }
}
```

**(d) EQL 跨实体子查询（手法 4，报表场景）**

```xml
<!-- sql-lib.xml -->
<eql name="findOrdersBySupplierCategory" rowType="app.erp.pur.dao.entity.ErpPurOrder">
    SELECT o FROM ErpPurOrder o
    WHERE o.supplierId IN (
        SELECT p.id FROM ErpMdPartner p WHERE p.categoryId = ${categoryId}
    )
</eql>
```

> **关键约束**：EQL 的 `o.supplier.name` 点导航**只识别本模块 `<to-one>` 关系声明**。跨模块没有关系声明，所以跨模块查询必须用 `IN` 子查询（把关联条件推到子查询里），不能用点导航自动 join。

### 5.5.4 路线 1：用 `notGenCode` 外部实体引用做高频关联查询

对于需要 EQL 点导航 + GraphQL 展开的高频关联场景（如 finance 凭证行按 `subject`/`partner`/`project`/`warehouse`/`material` 多维筛选；inventory 流水按物料/仓库/批次多维分析），在本模块 orm.xml 用机制 B 引用主数据表：

```xml
<!-- 例：app-erp-finance/model/app-erp-finance.orm.xml -->
<entities>
    <!-- 本模块实体建立 to-one 关联 -->
    <entity className="app.erp.fin.dao.entity.ErpFinVoucherLine" ...>
        <relations>
            <to-one name="subject" refEntityName="app.erp.md.dao.entity.ErpMdSubject">
                <join><on leftProp="subjectId" rightProp="id"/></join>
            </to-one>
        </relations>
    </entity>

    <!-- 【机制 B】引用外部主数据表,不生成本模块 Entity 类 -->
    <entity displayName="会计科目"
            name="app.erp.md.dao.entity.ErpMdSubject"
            notGenCode="true"
            tableName="erp_md_subject">
        <columns>
            <column name="id" .../>
            <column name="code" .../>
            <column name="name" .../>
            <column name="subjectClass" .../>
            <!-- 只声明 finance 会用到的列 -->
        </columns>
    </entity>
</entities>
```

```xml
<!-- app-erp-finance/pom.xml 加 master-data-dao 依赖 -->
<dependency>
    <groupId>io.nop.app</groupId>
    <artifactId>app-erp-master-data-dao</artifactId>
</dependency>
```

```sql
-- EQL 可以直接跨模块点导航 + 条件过滤(自动 LEFT JOIN)
SELECT vl.subject.name, SUM(vl.drAmount)
FROM ErpFinVoucherLine vl
WHERE vl.subject.subjectClass = 10
GROUP BY vl.subject.name
```

> **机制 B 的语义**：本模块 codegen **跳过**该实体（不生成 `ErpMdSubject.java`，复用 master-data-dao 已生成的类）；DDL 由 master-data 负责；ORM 关联、EQL 点导航、GraphQL 展开均正常工作。详见 `cross-module-entity-reference.md §2`。

### 5.5.5 反模式（禁止）

| 反模式 | 后果 |
|---|---|
| 跨模块 `<to-one>` 但**未在本模块声明 `notGenCode` 外部实体**，也没 Maven 依赖外部 `-dao` | codegen 失败（找不到 refEntityName）或运行时 `ClassNotFoundException` |
| 用 `ext:baseClass` Delta 扩展去做"引用外部表"（概念混淆） | 扩展给外部实体加了字段，没解决关联需求 |
| 列表场景对每行 `requireBiz` 查关联对象 | N+1 查询（应用冗余字段或机制 B） |
| 业务表直接 `@Inject` 外部 BizModel 实现类 | 不可替换、不可扩展（必须 `@Inject IXxxBiz` 接口） |
| 把 `notGenCode` 当 Delta 用（既设 `className` 子类又设 `notGenCode`） | 概念冲突：要么扩展（机制 C）要么引用（机制 B），二选一 |

### 5.5.6 例外：用 `ext:baseClass` Delta 扩展给主数据加字段

业务模块需要给主数据实体**加业务字段**时（类比 nop-app-mall 扩展 `NopAuthUser` 加 `picUrl`/`userLevel`），通过 `app-erp-delta` 工程做 Delta 扩展：

```xml
<!-- app-erp-delta/model/app-erp-md-delta.orm.xml -->
<entity className="app.erp.delta.dao.entity.ErpMdMaterialEx"
        name="app.erp.md.dao.entity.ErpMdMaterial"
        tableName="erp_md_material"
        ext:baseClass="app.erp.md.dao.entity.ErpMdMaterial">
    <columns>
        <!-- 只声明新增业务字段，平台字段由 baseClass 提供 -->
        <column name="assetPrefix" precision="20" stdSqlType="VARCHAR"/>
    </columns>
</entity>
```

> 扩展后 `ErpMdMaterialEx` 属于本 delta 模块视图，可正常与本模块实体建立 `<to-one>`。这是"业务模块需要给主数据加字段"的唯一正确范式，不在业务域建新表或写跨模块 `<to-one>`。

## 5.6 外部实体引用清单（机制 B 全工程应用）

> 本节是 §5.5.4 路线 1（机制 B）在本工程的具体落地清单。**共享单库 + 单向 DAG 前提下，所有业务域直接 Maven 依赖 `app-erp-master-data-dao`，在各自 orm.xml 用 `<entity notGenCode="true">` 引用 master-data 表，建立 `<to-one>` 关联。**

### 5.6.1 设计原则

1. **单向 DAG**：业务域 → master-data 单向合法。跨业务域允许的 ORM 只读引用（机制 B）：`finance → projects/assets`（finance 是 DAG 顶）+ `purchase/sales → projects`（项目采购/销售的成本归集，purchase/sales ORM 对 `ErpPrjProject` 建 to-one 只读引用）。**禁止**业务域之间的反向或循环引用，以及禁止 `projects → purchase/sales/finance` 反向。
2. **`notGenCode` 外部实体引用**：每个业务域 orm.xml 在 `<entities>` 末尾声明引用的 master-data 表，标 `notGenCode="true"`（不生成本模块 Entity 类，复用 master-data-dao 已生成的类）。
3. **只声明本模块用到的列**：外部实体声明只列本模块会用到的少数关键列（id/code/name 等），不全量复制；运行时由 master-data-dao 的完整 Entity 类提供所有列。
4. **to-one 命名约定**：沿用 master-data 现有内部 to-one 风格——`<to-one name="subject" tagSet="pub"><join><on leftProp="subjectId" rightProp="id"/></join></to-one>`，name 用单数语义名（subject/partner/warehouse/material/org/currency/uom 等）。
5. **与冗余字段并存**：`to-one` 提供对象导航/详情展开/条件过滤；冗余显示名字段（如 `supplierName`）提供列表零 join 显示——两者并存，不互斥。

### 5.6.2 依赖方向矩阵

每个业务域引用的 master-data 表（建立 to-one 关联）：

| 业务域 | 引用的 master-data 表 | 跨业务域引用 |
|---|---|---|
| **inventory** | material / materialSku / warehouse / location / uom / currency / organization / acctSchema | — |
| **purchase** | material / materialSku / partner / warehouse / uom / currency / organization / taxRate / settlementMethod / bankAccount | — |
| **sales** | material / materialSku / partner / warehouse / uom / currency / organization / taxRate / settlementMethod / bankAccount | — |
| **finance** | subject / acctSchema / currency / partner / organization / warehouse / material | **projects.project**（凭证行辅助核算 projectId） |
| **assets** | organization / currency / employee / location / materialCategory / subject | — |
| **projects** | organization / currency / employee / partner / subject | — |
| **manufacturing** | material / materialSku / uom / warehouse / location / organization / currency / partner | — |
| **quality** | organization / material / partner / warehouse / employee | — |
| **maintenance** | organization / location / materialCategory / employee / material / uom / warehouse | — |
| **crm** | organization / partner / employee / material 等（实测 31 to-one） | — |
| **cs** | organization / partner 等（实测 5 to-one） | — |
| **hr** | organization / employee / partner 等（实测 19 to-one） | — |
| **aps** | organization / material / warehouse 等（实测 6 to-one） | — |
| **contract** | organization / partner / currency 等（实测 10 to-one） | — |
| **drp** | organization / material / warehouse 等（实测 7 to-one） | — |
| **logistics** | organization / partner / material 等（实测 9 to-one） | — |
| **b2b** | organization / partner / material 等（实测 15 to-one） | — |

> **终态统计**（全量核对）：17 个业务域（除 master-data 根域）orm.xml 共建立约 **369 个跨模块 to-one**（其中 inventory/purchase/sales/finance/assets/projects/manufacturing/quality/maintenance 共 267 个 + crm/cs/hr/aps/contract/drp/logistics/b2b 共约 102 个），引用约 68+ 个外部实体声明。所有业务域 ORM 层均**只引用 master-data**（跨业务域仅 finance→projects 单向合法），零循环依赖。全量 to-one 总数与外部实体声明数待 codegen 后跑脚本精确统一。DAG 验证：所有引用边单向合法，零循环依赖。
>
> **关于 finance → assets**：assets 关联走 `voucher_bill_r` 弱指针（`billType=AST_DEPRECIATION` + `billHeadCode` 反查资产），不是固定 `assetId` 外键——因此 finance 不建到 assets 的 to-one（业务单据反查源单应用弱指针，见 §5.1）。
>
> master-data 共 15 张表被引用：material / materialSku / materialCategory / partner / warehouse / location / uom / currency / taxRate / settlementMethod / bankAccount / employee / organization / subject / acctSchema。其中 `material` 被所有持有明细的业务域引用，`organization` 被所有业务表引用（多公司核算维度）。

### 5.6.3 DAG 合规性规则

修改 orm.xml 时**必须**满足以下 DAG 方向：

```
app-erp-master-data ←（被引用，notGenCode）
        ↑
        ├── inventory / purchase / sales / assets / projects / manufacturing / quality / maintenance
        │   （单向依赖 master-data，合法）
        │
        └── finance
            （依赖 master-data + projects + assets，单向合法，finance 是顶）

禁止（循环或反向）：
  inventory → purchase/sales/finance      （业务域之间走弱指针 + I*Biz）
  purchase ↔ sales                        （走弱指针 + I*Biz）
  projects/assets → finance               （finance 引用它们，不反向）
  quality → 任何业务域                     （quality 被业务域引用，不反向）
```

### 5.6.4 标准代码模式

**(a) 在业务实体的 `<relations>` 追加 to-one**：

```xml
<entity className="app.erp.fin.dao.entity.ErpFinVoucherLine" ...>
    <relations>
        <!-- 本模块内部关联保持不变 -->
        <to-one name="voucher" refEntityName="app.erp.fin.dao.entity.ErpFinVoucher" tagSet="pub">
            <join><on leftProp="voucherId" rightProp="id"/></join>
        </to-one>
        <!-- 跨模块 to-one（引用 master-data 外部实体） -->
        <to-one name="subject" refEntityName="app.erp.md.dao.entity.ErpMdSubject" tagSet="pub">
            <join><on leftProp="subjectId" rightProp="id"/></join>
        </to-one>
        <to-one name="partner" refEntityName="app.erp.md.dao.entity.ErpMdPartner" tagSet="pub">
            <join><on leftProp="partnerId" rightProp="id"/></join>
        </to-one>
        <!-- 跨业务域 to-one（finance → projects） -->
        <to-one name="project" refEntityName="app.erp.prj.dao.entity.ErpPrjProject" tagSet="pub">
            <join><on leftProp="projectId" rightProp="id"/></join>
        </to-one>
    </relations>
</entity>
```

**(b) 在 `<entities>` 末尾追加外部实体声明**（`notGenCode="true"`）：

```xml
<!-- ============ 外部实体引用（notGenCode，不生成本模块 Entity 类） ============ -->
<entity displayName="会计科目" name="app.erp.md.dao.entity.ErpMdSubject"
         notGenCode="true" tableName="erp_md_subject">
    <columns>
        <column name="id" code="ID" stdSqlType="BIGINT" primary="true" stdDataType="long"/>
        <column name="code" code="CODE" stdSqlType="VARCHAR" precision="50"/>
        <column name="name" code="NAME" stdSqlType="VARCHAR" precision="200"/>
        <column name="subjectClass" code="SUBJECT_CLASS" stdSqlType="INTEGER" stdDataType="int"/>
    </columns>
</entity>
```

**(c) Maven 依赖**（codegen 后在业务域 `erp-xxx-dao/pom.xml` 配置）：

```xml
<dependency>
    <groupId>io.nop.app</groupId>
    <artifactId>app-erp-master-data-dao</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

finance 额外依赖 projects-dao 和 assets-dao：

```xml
<dependency>
    <groupId>io.nop.app</groupId>
    <artifactId>app-erp-projects-dao</artifactId>
</dependency>
<dependency>
    <groupId>io.nop.app</groupId>
    <artifactId>app-erp-assets-dao</artifactId>
</dependency>
```

### 5.6.5 使用场景对照

| 场景 | 用 to-one（机制 B） | 用冗余字段（L1） | 用 `@BizLoader`（L3） |
|---|---|---|---|
| 列表显示名称 | △（可但非最优） | ✅ **零 join 最优** | ❌（N+1） |
| 列表按主数据条件过滤 | ✅ **EQL 点导航最优** | ❌ | △（两步查询） |
| 详情页带出完整对象 | ✅ **天然支持** | △（只有名字） | ✅ |
| 报表按多维主数据筛选 | ✅ **EQL 自动 join** | ❌ | △ |
| GraphQL 嵌套查询 | ✅ **天然支持** | ❌ | △ |

> **核心原则**：高频列表显示用冗余字段，**条件过滤/详情展开/报表多维筛选**用 to-one。

### 5.6.6 EQL 跨模块查询示例

机制 B 建立后，EQL 可直接跨模块点导航：

```sql
-- 凭证行按科目类别汇总(自动 LEFT JOIN erp_md_subject)
SELECT vl.subject.subjectClass, vl.subject.name, SUM(vl.debitAmount)
FROM ErpFinVoucherLine vl
WHERE vl.acctSchema.id = ${acctSchemaId}
GROUP BY vl.subject.subjectClass, vl.subject.name

-- 采购单按供应商信用额度筛选(自动 LEFT JOIN erp_md_partner)
SELECT o FROM ErpPurOrder o
WHERE o.partner.creditLimit > ${limit}

-- 凭证行按项目维度分析(跨业务域,自动 LEFT JOIN erp_prj_project)
SELECT vl.project.projectCode, SUM(vl.debitAmount)
FROM ErpFinVoucherLine vl
WHERE vl.project is not null
GROUP BY vl.project.projectCode
```

## 6. 各域数据依赖详图

### 6.1 master-data（根域）

```
erp_md_material ──┬─ material_category (self-ref tree)
                  ├─ material_sku (1:N)
                  ├─ uom (M:1)
                  ├─ warehouse (M:1, default)
                  └─ tax_rate (M:1, default)

erp_md_partner ──┬─ partner_address (1:N)
                 ├─ partner_contact (1:N)
                 └─ bank_account (1:N)

erp_md_acct_schema ── acct_schema_coa (1:N, 关联 subject)

被以下域 R 引用：所有 17 个业务域（合计 ~222 处 to-one）
对外 R 引用：无（根域）
对外 S 写：无
```

### 6.2 inventory（库存域，被多域 S 写）

```
核心三件套（不可变流水 + 余额快照）：
erp_inv_stock_move ─── stock_move_line (1:N)
         │              ├─ material/sku (R: md)
         │              └─ uom (R: md)
         ├─ relatedBillType/Code (P: 反查业务源单)
         └─→ erp_inv_stock_ledger (1:N, 不可变流水)
                       └─ material/sku/warehouse/location (R: md)
                  └─→ erp_inv_stock_balance (汇总余额，按 material×warehouse×location)
                              └─ material/sku/warehouse/location (R: md)

辅助表族：
erp_inv_reservation / reservation_line（预留，sourceBillType P 反查销售/生产单）
erp_inv_cost_layer（成本层，acctSchemaId R: md）
erp_inv_transfer_order / line（调拨，自写库存）
erp_inv_stock_take / line（盘点）
erp_inv_picking_order / line（拣货，relatedBillType P 反查）
erp_inv_batch / serial_number（批次/序列号台账）

被 S 写入方：purchase / sales / manufacturing / maintenance / assets（业务发生时写库存）
对外 R：master-data（material/warehouse/location/uom/currency）
对外 S 写：无（仅自写，不回写业务）
P 指针：stock_move / reservation / picking_order 共 5 个字段
```

### 6.3 finance（财务域，DAG 顶，被 6 域 S 写）

```
业财一体三件套（凭证 + 分录 + 回链）：
erp_fin_voucher ─── voucher_line (1:N, 借贷分录)
         │           ├─ subject/subjectCode (R: md)
         │           ├─ currency (R: md)
         │           ├─ partner (R: md, 可选)
         │           ├─ project (R: prj, 可选)
         │           ├─ warehouse/material (R: md, 可选, 库存估值过账用)
         │           └─ acctSchema/org (R: md)
         └─→ erp_fin_voucher_bill_r (1:N, 业财回链)
                       ├─ billType + billHeadCode (P: 反查任意业务源单)
                       └─ voucherHeadCode (本表 FK)

凭证模板：
erp_fin_voucher_template / template_line（模板，AMOUNT 占位符）

应收应付 open-item：
erp_fin_ar_ap_item ─── sourceBillType + sourceBillCode (P: 反查发票/收付款)
         ├─ partner/acctSchema/currency (R: md)
         └─→ erp_fin_reconciliation / line（核销单，N:M 核销 ar_ap_item）

期间与余额：
erp_fin_accounting_period / period_status（按 acctSchema 维度）
erp_fin_gl_balance（总账余额，按 subject×partner×project×warehouse 维度）
erp_fin_trial_balance（试算平衡）
erp_fin_fund_account（资金账户）

对外 R：master-data（subject/acctSchema/currency/partner/warehouse/material/org）+ projects（project）
        + 业务域 I*Biz（只读查源单，非字段引用）
对外 S 写：无（顶域）
被 S 写入方：purchase / sales / assets / projects / manufacturing / maintenance（业财过账）
P 指针：voucher_bill_r / ar_ap_item 共 2 处（但这是反查入口，是业财一体的核心）
```

### 6.4 purchase / sales（业务域，对称结构）

```
采购链（purchase）：
requisition → rfq → quotation → order → receive → invoice → payment
                                              │        │         │
                                              ↓ S      ↓ P       ↓ P
                                         inventory  finance   finance
                                         (stock)  (ar_ap_item 核销)

销售链（sales）：
quotation → order → delivery → invoice → receipt
                       │ S       │ P       │ P
                       ↓         ↓         ↓
                   inventory  finance   finance

所有业务行 R 引用：material / sku / uom / tax_rate / warehouse（md）
所有业务头 R 引用：partner / currency / settlement_method / org（md）
可选 R：project（prj，项目采购/销售）
对外 S 写：finance（过账）
被 P 反查：inventory（stock_move）/ finance（voucher_bill_r / ar_ap_item）/ quality（inspection）
```

### 6.5 扩展域

#### 第一批（assets / projects / manufacturing / maintenance / quality）

| 域 | 核心 R 引用 | 对外 S 写 | 被 P 反查入口 |
|---|---|---|---|
| assets | md（subject/category/currency/org/location/employee） | finance（折旧/处置/资本化过账） | voucher_bill_r（finance 反查资产） |
| projects | md（org/partner/currency/employee/subject） + prj（self-ref：task→project） | finance（成本归集过账） | cost_collection_line（sourceBillType 反查） + finance voucher_line.projectId |
| manufacturing | md（material/sku/uom/org/currency/partner/warehouse） | finance（工单完工过账）+ quality | mrp_demand（sourceBillType 反查销售/生产单） |
| maintenance | md（org/location/category/employee/warehouse/material）+ assets（equipment→asset，R） | finance（维修领料过账）+ manufacturing（停机影响排产） | 无（被 manufacturing 反查 equipment） |
| quality | md（org/material/partner/warehouse） | 无（被业务域触发检验，非 S 写） | inspection / review（relatedBillType 反查采购/生产单） |

#### 第二批（crm / cs / hr / aps / contract / drp / logistics / b2b）

> 以下 8 域 ORM 层实测仅引用 master-data（见 §5.6.2）；业务层 S 写/P 关系待各域设计深化后补充。

| 域 | 核心 R 引用（ORM 实测） | 对外 S 写 | 被 P 反查入口 |
|---|---|---|---|
| crm | md（organization/partner/employee/material 等，31 to-one） | 待深化（与 sales 协作：线索转客户） | 待深化 |
| cs | md（organization/partner 等，5 to-one） | 待深化（与 sales 协作：售后工单） | 待深化 |
| hr | md（organization/employee/partner 等，19 to-one） | 待深化（薪资过账 finance） | 待深化 |
| aps | md（organization/material/warehouse 等，6 to-one） | 待深化（与 manufacturing 协作：排程→工单） | 待深化 |
| contract | md（organization/partner/currency 等，10 to-one） | 待深化（合同关联 purchase/sales 单据） | 待深化 |
| drp | md（organization/material/warehouse 等，7 to-one） | 待深化（分销网络与 inventory/sales） | 待深化 |
| logistics | md（organization/partner/material 等，9 to-one） | 待深化（运输与 inventory/sales 发货） | 待深化 |
| b2b | md（organization/partner/material 等，15 to-one） | 待深化（B2B 订单与 purchase/sales） | 待深化 |

## 7. 数据依赖的演进规则

### 7.1 何时更新本文档

| 触发事件 | 更新内容 |
|---|---|
| 新增 orm.xml 跨域字段 | §3 / §5 的字段目录 |
| 新增业务单据类型 | §5.2 的 `billType` 枚举表 + `domain-design-guidelines.md` |
| 新增业务闭环（新 S 写场景） | §4.2 的 S 写矩阵 |
| 新增 `I*Biz` 只读方法 | §3.2 的 finance 取数清单 |
| codegen 后 `I*Biz` 接口签名定型 | §3.2 / §4.3 的接口清单从"预期"改为"实测" |

### 7.2 反模式（禁止）

1. **跨业务域 ORM `<to-one>` 声明**——违反 `module-boundaries.md` 硬规则。
2. **finance 回写业务表**——破坏业财单向原则（业务→财务）。
3. **业务表写外键到凭证表**——破坏弱指针范式，凭证生成时业务表不应感知凭证存在。
4. **跨域直接 `IDaoProvider` / `IOrmTemplate` 查表**——必须经 `I*Biz` 接口封装业务规则。
5. **逗号字符串表达多值引用**（管伊佳 `accountIdList` 反范式，见分析报告 §9.5）——必须用子表。
6. **`billType` 枚举未登记就用**——会导致弱指针反查失效。

### 7.3 L2 待演进部分（codegen 后补充）

本文档的 §3.2 / §4.3 涉及 `I*Biz` 接口清单，当前是**预期签名**。codegen 生成实际接口后，需在此补充：

- [ ] 各 `IErpXxxBiz` 的具体只读方法签名（§3.2）
- [ ] `IErpFinAcctDocProvider` 的 `AcctDocContext` 结构定型（§4.3）
- [ ] 各业务域 Provider Bean 的注册清单（§4.3）
- [ ] 弱指针反查的 `I*Biz` 方法统一命名规范

## 8. 相关文档

- `docs/architecture/module-boundaries.md` — 模块级依赖方向（DAG），本文是其数据层细化
- `docs/architecture/domain-module-split-analysis.md` — 模块拆分决策与命名方案
- `docs/architecture/integration-and-transaction-patterns.md` — 本地优先 + 幂等 + 外部结果所有权
- `docs/architecture/system-baseline.md` — 系统基线（引用本文作为数据层依据）
- `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md` — **跨模块实体引用与关联查询（§5.5 的权威依据）**
- `../nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md` §2-3 — `requireBiz()` 跨实体只读查询 + 实体缓存
- `../nop-entropy/docs-for-ai/02-core-guides/eql-and-database-compatibility.md` — EQL 语法（手法 4 的依据）
- `docs/design/domain-design-guidelines.md` — 单据类型枚举（`billType` 取值的权威来源）
- `docs/design/finance/posting.md` — 业财一体过账机制（§4 的业务设计依据）
- `docs/analysis/2026-06-22-0000-cross-domain-coupling-vs-microservice.md` §9 — 13 项目跨模块引用机制实测（本文 §5 的横向依据）
- `<domain>/model/app-erp-<domain>.orm.xml` — 各域权威数据模型（本文 §3-6 的字段来源）
