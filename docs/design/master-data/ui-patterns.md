# 主数据域页面设计要点

> 本文档定义主数据域关键页面的结构布局、交互模式与导航流程。
> 字段定义以 `model/app-erp-master-data.orm.xml` 为准，业务语义见 `README.md`、`sku-multi-unit.md`。
> 调研引用格式 `[源项目#要点]`，详见 `docs/analysis/erp-survey/`。

## 设计原则

1. **编码自动生成 + 人工覆盖**：主数据编码支持自动流水号生成，也允许人工录入覆盖。编码唯一性在前端输入时即校验（异步检查），不等到提交才报错。
2. **树形结构统一交互**：物料分类、科目表等树形结构使用统一的树形组件（支持拖拽排序、父节点切换、层级展开/折叠）。
3. **启用/停用二态简化为开关**：所有主数据实体的启用/停用使用统一 Switch 控件，停用时前端提示"停用后将不可被新单据引用"。
4. **引用预览**：停用/删除前自动查询并展示"引用此主数据的业务单据数量"，让用户了解影响范围后再确认。

## 页面清单

| 页面 | 类型 | 主要用户 | 复杂度 |
|------|------|----------|--------|
| 物料分类管理 | 树形管理 | 管理员 | ★★☆ |
| 物料/SKU 编辑 | 表单+子表 | 管理员 | ★★★ |
| 物料/SKU 列表 | 列表+快捷编辑 | 管理员 | ★★☆ |
| 往来单位编辑 | 表单 | 管理员 | ★★☆ |
| 仓库/库位管理 | 树形+列表 | 管理员 | ★★☆ |
| 科目表管理 | 树形管理 | 财务管理员 | ★★★ |
| 计量单位管理 | 列表 | 管理员 | ★☆☆ |
| 币种/汇率管理 | 表格 | 财务管理员 | ★★☆ |

## 各页面设计要点

### 物料/SKU 编辑

**页面入口**：主数据 → 物料管理 → 新建/编辑物料

**主表单（Material）**：
```
┌────────────────────────────────────────────────────────┐
│ 物料编码: MAT-001 (自动) [手动覆盖] 启用: [🔛]          │
│ ┌───── 基本信息 ─────────────────────────────────────┐ │
│ │ 物料名称: [___]  规格型号: [___]  品牌: [选择]      │ │
│ │ 物料分类: [树形选择]  基本单位: [下拉选择]            │ │
│ │ 保质期: [___] 天  批次管理: [☐]  序列号管理: [☐]   │ │
│ │ 助记码: (自动)  图片: [上传]                         │ │
│ └────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────┤
│ SKU 列表 (MaterialSku 子表)                            │
│ ┌────┬────────┬──────┬──────┬──────┬──────┬──────┬──┐ │
│ │ #  │ 条码   │ 包装单位│ 采购价│ 批发价│ 零售价│最低价│默认│ │
│ │ 1  │ 690123 │ 个    │ 1.5  │ 2.0  │ 3.0  │ 1.2  │ ☑  │ │
│ │ 2  │ 690124 │ 箱(100)│ 140  │ 160  │ 200  │ 130  │ ☐  │ │
│ │ [+] [扫描条码] [批量生成]                            │ │
│ └────────────────────────────────────────────────────┘ │
│ ────────────────────────────────────────────────────── │
│ (点击 SKU 行展开更多单位换算)                            │
│ 换算关系:                                              │
│ 1 箱 = 100 个 (基本单位)                                │
└────────────────────────────────────────────────────────┘
```

**要点**：
- 物料分类使用树形弹窗选择，支持搜索
- SKU 子表行内编辑，条码支持扫描枪录入
- 点击 SKU 行展开单位的换算关系配置（如 1 箱 = 100 个）
- 多档价格列可选择显示/隐藏（按权限控制）
- 停用时弹窗提示引用预览（有多少未完成单据引用此物料） [管伊佳#Material+MaterialExtend]

### 物料分类管理

**树形结构**：
```
┌────────────────────────────────────────────────┐
│ 全部物料分类                                    │
│ ├─ 原材料                                       │
│ │  ├─ 金属材料                                   │
│ │  │  ├─ 钢材                                   │
│ │  │  ├─ 铝材                                   │
│ │  │  └─ 铜材                                   │
│ │  └─ 塑料材料                                   │
│ ├─ 半成品                                       │
│ ├─ 成品                                         │
│ └─ 辅助材料                                     │
│                                                │
│ [新增同级] [新增子级] [重命名] [删除] [拖拽移动]    │
└────────────────────────────────────────────────┘
```

**要点**：
- 树形节点支持：展开/折叠、拖拽排序、右键菜单（新增/编辑/删除/停用）
- 点击节点右侧显示该分类下的物料列表（嵌入式）
- 删除节点前校验：该分类下是否有物料、是否有子分类

### 往来单位编辑

**页面入口**：主数据 → 往来单位 → 新建/编辑

```
┌────────────────────────────────────────────────────────┐
│ 单位编码: PART-001 (自动)  启用: [🔛]                    │
│ ┌───── 基本信息 ─────────────────────────────────────┐ │
│ │ 单位名称: [___]  简称: [___]  类型: [客户/供应商/两者]│ │
│ │ 联系人: [___]  联系电话: [___]  邮箱: [___]          │ │
│ │ 所在地区: [省/市/区]  详细地址: [___]                │ │
│ └────────────────────────────────────────────────────┘ │
│ ┌───── 财务信息 ─────────────────────────────────────┐ │
│ │ 税号: [___]  开户行: [___]  账号: [___]             │ │
│ │ 税率(%): [___]  信用额度: [___]  账期(天): [___]    │ │
│ │ 付款条件: [下拉选择]                                 │ │
│ └────────────────────────────────────────────────────┘ │
│ ┌───── 联系信息 ─────────────────────────────────────┐ │
│ │ 收货地址: [___] [+] (多地址维护)                    │ │
│ │ 发票地址: [___] [+]                                 │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

**要点**：
- 类型选择"两者"时，同时作为客户和供应商（在采购/销售单据中都可选） [管伊佳#Supplier.type]
- 信用额度字段仅在客户类型时显示，供应商类型时隐藏
- 停用时预览：此单位关联的未完成订单/未核销发票数量

### 科目表管理

**页面入口**：财务管理 → 科目表

```
┌────────────────────────────────────────────────┐
│ 账套: [默认账套]                                 │
├────────────────────────────────────────────────┤
│ 科目树                                            │
│ ├─ 1001 库存现金                                  │
│ ├─ 1002 银行存款                                  │
│ │  ├─ 1002-01 工行                                │
│ │  └─ 1002-02 建行                                │
│ ├─ 1122 应收账款                                  │
│ ├─ 1403 原材料                                    │
│ ├─ 2001 短期借款                                  │
│ ├─ 2202 应付账款                                  │
│ │  ├─ 2202-01 暂估应付                            │
│ │  └─ 2202-02 一般应付                            │
│ └─ 5001 主营业务成本                              │
│                                                │
│ 科目编码: 1002-01 (自动带出父编码前缀)               │
│ 科目名称: [___]  余额方向: [借方/贷方]              │
│ 是否末级: [☑]  辅助核算: [项目/部门/客户/供应商]     │
│ [保存] [新增同级] [新增子级] [停用]                  │
└────────────────────────────────────────────────┘
```

**要点**：
- 科目编码由父级编码前缀 + 子级段码自动拼接
- 辅助核算多选：项目/部门/客户/供应商/仓库
- 末级科目才可被凭证分录引用（非末级只做汇总）
- 停用父科目弹窗提示：子科目不受影响，但不可新增子科目

### 币种与汇率

**汇率表格**（支持批量录入）：
```
┌────────────────────────────────────────────────────────┐
│ 币种管理                                                │
│ ┌────┬──────┬──────┬────────┬────────┐                 │
│ │ 编码│ 名称  │ 符号  │ 汇率方式│ 状态   │                 │
│ │ USD│ 美元  │ $    │ 浮动    │ 🟢 启用│                 │
│ │ EUR│ 欧元  │ €    │ 浮动    │ 🟢 启用│                 │
│ └────┴──────┴──────┴────────┴────────┘                 │
│ ────────────────────────────────────────────────────── │
│ 汇率录入 (USD → RMB)                                    │
│ 日期: 2026-06-20  汇率: 7.2500                         │
│ ┌──────┬────────┬────────┐                             │
│ │ 日期  │ 汇率    │ 操作   │                             │
│ │ 06-20│ 7.2500 │ [编辑] │                             │
│ │ 06-19│ 7.2450 │ [编辑] │                             │
│ └──────┴────────┴────────┘                             │
│ [新增行] [从外部导入]                                    │
└────────────────────────────────────────────────────────┘
```

## 跨页面导航流

```
物料分类 → [选择分类] → 物料列表 → [编辑物料] → 管理 SKU
    ↓
往来单位 → [引用预览] → 确认停用
    ↓
科目表 → [选择科目] → 凭证录入引用
    ↓
币种/汇率 → [录入汇率] → 多币种单据引用
```

## 树形 CRUD 范式（MaterialCategory + Subject）

主数据域含 2 个自引用树形实体：`ErpMdMaterialCategory`（物料分类树）和 `ErpMdSubject`（会计科目树）。两者均遵循 `docs/design/tree-entity-patterns.md` 跨域范式。

### 列集表

| 实体 | tree-list grid 列集 | 域专用业务约束 |
|------|---------------------|----------------|
| `ErpMdMaterialCategory` | `code` `name` `parentId` `sortNum` `priceValidationLevel` | 物料分类树，叶子节点用于物料归类 |
| `ErpMdSubject` | `code` `name` `parentId` `subjectClass` `direction` `isLeaf` `status` | 凭证录入科目 picker 仅返回叶子节点（`filter_isLeaf=1`） |

### 配置要点

1. **tree-list grid**：克隆 `<grid id="list">` + 追加 `<selection>children @TreeChildren(max:5)</selection>`
2. **crud main grid="tree-list"**：`<table loadDataOnce="true" sortable="false" pager="none">` + URL `@query:ErpMdXxx__findList/{@listSelection}?filter_parentId=__null`
3. **add-child simple page**：`<simple name="add-child" form="add"><data><parentId>$id</parentId></data></simple>` + rowActions 追加 `row-add-child-button`
4. **parentId tree-select 控件**：edit/add 表单 `<cell id="parentId">` 升级为 `<tree-select>`，URL 加 `filter_id__ne=$id` 排除自身防循环引用
5. **picker 升级**：picker.page.yaml table 改 tree 配置（`loadDataOnce + pager=none + filter_parentId=__null`）；ErpMdSubject picker 保留 `filter_isLeaf=1` 过滤

### ErpMdSubject 域专用约束（凭证录入科目 picker）

凭证录入只能选择叶子科目（`isLeaf=1`）。tree picker URL 同时含 `filter_parentId=__null` + `filter_isLeaf=1` —— 根节点 + 叶子双过滤确保只返回位于根层级且是叶子的科目。@TreeChildren 嵌套展开为只读层级上下文。

详细配置模板与反模式自检见 `docs/design/tree-entity-patterns.md`。

## 主数据专用交互模式（F7 §3）

> 落地计划：`docs/plans/2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions.md`
> 跨域范式参考：`docs/design/visible-on-patterns.md`

主数据实体（物料/往来单位/科目）有 3 类区别于业务单据的专用交互模式。本节固化本域 3 高频实体（`ErpMdMaterial` / `ErpMdPartner` / `ErpMdSubject`）的统一实现约定。

### 1. 编码唯一性前置校验（async validator on blur）

| 实体 | 校验字段 | 后端 @BizQuery | excludeId 自身排除 |
|------|---------|---------------|-------------------|
| `ErpMdMaterial` | `code` | `isCodeUnique(String code, Long excludeId)` | edit 模式传 id |
| `ErpMdPartner` | `code` | `isCodeUnique(String code, Long excludeId)` | edit 模式传 id |
| `ErpMdSubject` | `code` | `isCodeUnique(String code, Long excludeId)` | edit 模式传 id |

**触发时机**：`onEvent.blur`（用户离开输入框时异步校验，避免每键击一次请求）。
**反馈方式**：AMIS toast（msg：「编码已存在」红色 / 通过无提示）。
**前置范式**：见 `visible-on-patterns.md §5`。

### 2. 删除引用预览（countReferences + dialog 阻断）

| 实体 | 引用域覆盖 | 后端 @BizQuery |
|------|-----------|---------------|
| `ErpMdMaterial` | purchase（Order/Receive/Invoice Line）+ sales（Order/Delivery/Invoice Line）+ inventory（StockMove） 共 7 表 | `countReferences(Long id) → Map<String,Long>` |
| `ErpMdPartner` | purchase（Order/Receive/Invoice 头 supplierId）+ sales（Order/Delivery/Invoice 头 customerId）+ inventory（StockMove）共 7 表 | `countReferences(Long id) → Map<String,Long>` |
| `ErpMdSubject` | ❌ 不实现 | 会计语义上科目可停用不可删除，F1 已移除删除按钮，无消费者 |

**跨域解耦**：master-data 不可反向依赖 purchase/sales/inventory（依赖环约束）。`countReferences` 经 SPI 端口（`IErpMdMaterialReferenceChecker` / `IErpMdPartnerReferenceChecker`，在 `erp-md-dao` 声明）+ `@Nullable @Inject` 在 BizModel 收集下游实现。默认无实现时返回空 Map（删除直接走原 __delete 路径）。

**交互流程**：点击 `row-delete-button` → 调 countReferences → 引用 > 0 弹 dialog（列出 N 张单据 + 阻断） / 引用 = 0 走原 confirm + __delete 路径。
**前置范式**：见 `visible-on-patterns.md §6`。

### 3. 启用/停用 Switch 控件（替代 button-group-select）

| 实体 | status 字段 | onEvent.change | 覆盖状态 |
|------|-----------|---------------|---------|
| `ErpMdMaterial` | ACTIVE/INACTIVE | 停用方向弹确认 dialog | ✅ F7 落地 |
| `ErpMdPartner` | ACTIVE/INACTIVE | 停用方向弹确认 dialog | ✅ F7 落地 |
| `ErpMdSubject` | ACTIVE/INACTIVE | ❌ 不在本计划范围 | 🟡 successor（按域推进主数据 Switch 控件全覆盖） |

**控件配置**：`type: 'switch'` + `trueValue: 'ACTIVE'` + `falseValue: 'INACTIVE'`（保留 DB schema 兼容）。
**前置范式**：见 `visible-on-patterns.md §7`。

## 调研参考

| 设计点 | 参考来源 | 应用方式 |
|--------|----------|----------|
| 物料 + SKU 分离 + 多档价格 | 管伊佳#Material + MaterialExtend | 物料主表单 + SKU 可编辑子表 |
| 供应商/客户一体表 | 管伊佳#Supplier.type | 往来单位类型下拉（客户/供应商/两者） |
| 科目树形 + 段值编码 | 赤龙#MdFinanceSubject | 科目树自动拼接编码 |
| 多单位换算 | 管伊佳#MaterialExtend.unit | SKU 行展开单位换算配置 |
| 引用预览（停用前检查） | 通用范式 | 停用/删除前展示引用影响范围 |
| 编码唯一性前置校验（async validator on blur） | 通用范式 | F7 §3 落地：3 高频主数据实体 `code` 字段 |
| 删除引用预览（countReferences + dialog 阻断） | 通用范式 | F7 §3 落地：2 高频实体经 SPI 跨域聚合 |
| 启用/停用 Switch 控件 | 通用范式 | F7 §3 落地：2 高频实体（替代 button-group-select） |

## 主数据实体 form 布局分组（P3 B 类）

> 适用范围：master-data 域 10 个 B 类（有分组价值）实体（不含已 1500-1 覆盖的 `ErpMdMaterial`/`ErpMdPartner`/`ErpMdSubject`/`ErpMdOrganization`；不含 A 类纯字典 10 个）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 决策来源：`docs/plans/2026-07-20-2059-2-f3-p2p3-ext-masterdata-form-layout.md` Phase 0.I。
> A 类纯字典（Currency/UoM/UoMConversion/TaxRate/SettlementMethod/Location/CostCenter/SubjectMapping/AcctSchemaCoa/ErpSysConfig 共 10 个）维持 codegen 默认，本计划 Non-Goal。

### 模板分化决策（B 类 10 实体）

| 实体 | 分组结构 |
|------|----------|
| ErpMdWarehouse | baseInfo + storage + audit |
| ErpMdEmployee | baseInfo + contact + link + audit |
| ErpMdBankAccount | baseInfo + audit |
| ErpMdPartnerContact | baseInfo + audit |
| ErpMdPartnerAddress | baseInfo + contact + audit |
| ErpMdMaterialSku | baseInfo + price + audit |
| ErpMdMaterialCategory | baseInfo（tree，仅 form 分组，不改 tree grid） |
| ErpMdAcctSchema | baseInfo + accounting + audit |
| ErpMdExchangeRate | pair + validity + audit |
| ErpMdSupplierApproval | baseInfo + validity + approval + audit |

### ErpMdWarehouse 模板（仓库）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[仓库编码] name[仓库名称]
 warehouseType[仓库类型] orgId[业务组织]
 status[状态]
=========>storage[仓储配置]======
 address[地址] managerId[仓管员]
 batchSelectionStrategy[批次选择策略]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpMdMaterialSku 模板（物料 SKU，含多档价格）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 materialId[物料] skuCode[SKU 编码]
 barcode[条码] uoMId[计量单位]
 conversionRate[换算率] isDefault[是否默认]
=========>price[价格信息]======
 purchasePrice[采购价] salePrice[销售价]
 wholesalePrice[批发价] retailPrice[零售价]
 taxRateId[税率]
========^audit[审计信息]=========
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpMdExchangeRate 模板（汇率）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>pair[货币对]======
 fromCurrencyId[源币种] toCurrencyId[目标币种]
 rateType[汇率类型] rate[汇率]
=========>validity[生效期]======
 validFrom[生效日期] validTo[失效日期]
========^audit[审计信息]=========
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpMdMaterialCategory 模板（物料分类树，仅 form 分组）

```xml
<form id="view">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[编码] name[名称]
 parentId[上级分类] sortNum[排序号]
 priceValidationLevel[价格校验级别]
    </layout>
</form>
```

> 注：`ErpMdMaterialCategory` 的 tree grid 由 F10 plan 覆盖；本计划仅补其独立 form 分组，不改 tree-list grid 与 tree-select 控件。

### query 表单基线

所有 master-data B 类实体的 `<form id="query">` 至少含 5 个查询字段。`code`/`name`/`skuCode`/`barcode`/`bankAccount`/`contactPerson`/`address` 配 `filterOp=like`；`orgId`/`partnerId`/`materialId`/`warehouseType`/`nature`/`status`/`isDefault`/`isActive`/`accountType`/`addressType`/`parentId` 配 `filterOp=eq`；含日期字段（`validFrom`/`validTo`）配 `filterOp=date-between`。
