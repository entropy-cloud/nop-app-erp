# 主数据域用例规格(Master Data Use Cases)

> 从使用场景出发组织主数据域可验证用例。机制细节引用不重复(指向 sku-multi-unit)。
> 主数据核心是物料/SKU/多单位/多档价格/条码,以及主数据对业务单据的约束(停用/删除/引用)。

---

## UC-MD-01 扫码开单

**场景**:业务单据录入时扫描条码,反查 SKU 与物料,自动填充单位与价格。

**可验证断言**(见 sku-multi-unit.md §多 barcode、§业务单据引用):
```
扫描条码 → findSkuByBarcode(barcode) → SKU + 物料
自动填充: 物料, SKU, 单位(=SKU.单位), 数量, 价格(按价格优先级解析)
条码全局唯一(违反 idx_barcode unique → 拒绝)
```

**涉及机制**:sku-multi-unit.md §多 barcode/§业务单据引用

---

## UC-MD-02 多单位换算落账

**场景**:按箱录入(大单位),系统自动换算为基本单位数量落账。

**可验证断言**(见 sku-multi-unit.md §多单位换算):
```
录入: 单位=箱, 数量=10
换算: baseQty = 数量 × conversionFactor(箱→瓶)
落账: 业务单据行.baseQty == 10 × 系数  (用于库存/成本计算)
单位组(UoMGroup)内换算系数一致
```

**涉及机制**:sku-multi-unit.md §多单位换算

---

## UC-MD-03 价格优先级解析

**场景**:销售单据取价时,按优先级解析最终价格。

**可验证断言**(见 sku-multi-unit.md §多档价格):
```
价格优先级: 手工价 > 价格表 > SKU 默认档(purchasePrice/wholesalePrice/retailPrice)
单据行.单价 若手工填 → 用手工价
否则 查价格表(客户专属/促销) → 命中则用
否则 用 SKU 默认档(按单据类型选 purchase/wholesale/retail)
```

**涉及机制**:sku-multi-unit.md §多档价格

---

## UC-MD-04 最低价校验拦截

**场景**:售价低于 minPrice(价格底线),拒绝或警告。

**可验证断言**(见 sku-multi-unit.md §多档价格、§配置项):
```
若 最终售价 < SKU.minPrice:
  配置 erp-md.sku-price-validation == HARD → 拒绝
  配置 == WARN → 警告但放行
  配置 == OFF → 不校验
```

**涉及机制**:sku-multi-unit.md §多档价格/§配置项

---

## UC-MD-05 默认 SKU 兜底

**场景**:单据未指定 SKU/单位时,取物料的默认 SKU。

**可验证断言**(见 sku-multi-unit.md §默认 SKU):
```
单据行未指定 SKU → resolveSku(物料) → 取 defaultFlag=true 的 SKU
每物料必有且仅有一个默认 SKU(约束)
若无默认 SKU 且配置 sku-default-required → 报错
```

**涉及机制**:sku-multi-unit.md §默认 SKU

---

## UC-MD-06 SKU 状态约束

**场景**:SKU 的停用与删除约束(保护主数据完整性)。

**可验证断言**(见 sku-multi-unit.md §SKU 状态管理):
```
停用唯一默认 SKU → 拒绝(必须先设其他默认)
SKU 被业务单据引用 → 拒绝删除(只能停用)
物料停用 → 联动所有 SKU 不可被新单引用
存量单据保留对已停用 SKU 的引用(历史完整)
```

**涉及机制**:sku-multi-unit.md §SKU 状态管理

---

## 用例与测试的衔接

- 条码/单位/价格(U01/U02/U03)→ 主数据服务单测 + 业务单据集成测试
- 约束(U06)→ 主数据完整性测试(停用/删除拦截)

## 参考机制文档

- `sku-multi-unit.md` — 物料/SKU 分离/多单位/多档价格/条码/状态约束/配置项
