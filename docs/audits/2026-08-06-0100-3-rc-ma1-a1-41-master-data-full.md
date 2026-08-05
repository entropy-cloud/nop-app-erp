# 2026-08-06-0100-3 rc-ma1-a1-41 master-data 全功能需求符合性审计报告

> 报告状态：done
> Mission: requirement-compliance（MA1 切片 A1.41）
> Work Item: A1.41（MA1 需求追踪矩阵审计 — master-data 全功能：扫码开单 findSkuByBarcode / 多单位换算 convertQty / 价格优先级 resolvePrice / 最低价校验 validatePrice / 默认 SKU resolveSku / SKU 状态约束 / 主数据看板）
> Source Plan: `docs/plans/2026-08-06-0100-3-rc-ma1-a1-41-master-data-full.md`
> 方法论契约: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点: `docs/audits/rc-requirement-baseline-inventory.md` A1.41 = UC-MD-01~07（7 UC，覆盖率 ✅ 一致，无基线分歧 D-xx）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）
> 本审计为**只读审计**：不修改代码/ORM/api.xml/view.xml/真相源；结果表面 = 本报告 + arm-index 登记。finding 的修复按 §10 经 MR0/MR1 实施。

---

## §0 与既有 MA2 报告差异增量声明（methodology §6 段落 9 / §去重协议）

- **无 master-data 专属 MA2 状态机报告**：`docs/audits/2026-07-2*-arm-ma2-*` 三份 MA2 e2e 报告（O2C / P2P / 期末结账）+ 15 份 MA2 状态机报告（finance/mfg/hr/pur/sal/ast/inv/qa/prj/crm+cs+ct+b2b+mnt / aps+log）**均不含 master-data SKU/价格/条码/UoM/状态约束/看板行为证据**——master-data 是基础数据底座非业务流转域，A2.x 状态机/e2e 范围均以业务单据/凭证为主轴，无 master-data 行为审计。**唯一 master-data 既有 MA2 引用** = `P2-MA2-013`（O2C 报告:172 注 `SettlementAllocation` 类*位置*在 master-data 模块，结算维度，与 UC-MD-01..07 无关）。**故无可复用 MA2 行为证据——本切片为 master-data SKU/价格/条码/UoM/状态约束/看板行为的首份证据**。
- **跨域 daoFor 复用 P1-MA1-022 现状证据**：master-data 是 DAG 根域，是**最被引用的目标域**——`ErpMd*` 实体被 purchase/sales/inventory/finance/assets/mfg/mnt/prj/qa/drp/aps 经 `daoFor` 广泛只读访问（~100+ 文件）。**本切片不重审跨域 daoFor**（不同维度：需求符合性 vs 平台一致性），其读侧统一裁决在 `docs/architecture/data-dependency-matrix.md §9`（plan `2026-07-29-2225-1` resolved）。本切片只引用其作为现状证据，不重审。
- **既有 master-data 非 UC-MD finding 复用注记**：`P1-MA3-003`（master-data 文档=plan 执行记录/字段表转录，resolved R2.1）/ `P2-MA1-030`（ErpMdCurrencyBizModel:60 `LocalDate.now()` watch-only）/ `P2-MA5-007`（master-data.write.amis Non-Goal 长期悬挂 watch-only）**均不属 UC-MD 维度**（文档转录/`LocalDate.now()` 副作用/AMIS Non-Goal），按 §去重协议本切片**不复审**。
- **本切片须声明与 MA2 报告的差异增量**（报告段落 9）：无 master-data 专属 MA2 报告，无可复用行为证据；P1-MA1-022 引作跨域 daoFor 现状证据（不重审平台一致性维度），只补需求视角差异（SKU status+引用检查 / supplier SPI / 看板阈值+权限 / minPrice 派生 / DB 约束缺失）。
- **master-data 域首个 RC 切片**：grep arm-index master-data/md/sku/barcode/uom/price-list/priceValidation/minPrice/defaultFlag/resolveSku/material-category → **无 UC-MD-* finding**（既有 master-data finding 均非 UC-MD 维度，见上）。本切片为 master-data 域首份 RC 审计切片，新 finding 续全仓 RC 序列 N=2（A1.40 cs-F4）之后最新编号。

---

## §1 需求契约原文（L1 use-case 需求契约，逐字引用）

> 真相源：`docs/design/master-data/use-cases.md`（L1 权威功能契约，methodology §4 层级 2）。验收标准逐字引用，禁止转述（§1 L1 格式）。

### UC-MD-01 扫码开单 — `use-cases.md:8`

```
扫描条码 → findSkuByBarcode(barcode) → SKU + 物料
自动填充: 物料, SKU, 单位(=SKU.单位), 数量, 价格(按价格优先级解析)
条码全局唯一(违反 idx_barcode unique → 拒绝)
```

### UC-MD-02 多单位换算落账 — `use-cases.md:23`

```
录入: 单位=箱, 数量=10
换算: baseQty = 数量 × conversionFactor(箱→瓶)
落账: 业务单据行.baseQty == 10 × 系数  (用于库存/成本计算)
单位组(UoMGroup)内换算系数一致
```

### UC-MD-03 价格优先级解析 — `use-cases.md:39`

```
价格优先级: 手工价 > 价格表 > SKU 默认档(purchasePrice/wholesalePrice/retailPrice)
单据行.单价 若手工填 → 用手工价
否则 查价格表(客户专属/促销) → 命中则用
否则 用 SKU 默认档(按单据类型选 purchase/wholesale/retail)
```

### UC-MD-04 最低价校验拦截 — `use-cases.md:55`

```
若 最终售价 < SKU.minPrice:
  MaterialCategory.priceValidationLevel == HARD → 拒绝
  == WARN → 警告但放行
  配置 == OFF → 不校验
```

### UC-MD-05 默认 SKU 兜底 — `use-cases.md:71`

```
单据行未指定 SKU → resolveSku(物料) → 取 defaultFlag=true 的 SKU
每物料必有且仅有一个默认 SKU(约束)
若无默认 SKU 且配置 sku-default-required → 报错
```

### UC-MD-06 SKU 状态约束 — `use-cases.md:86`

```
停用唯一默认 SKU → 拒绝(必须先设其他默认)
SKU 被业务单据引用 → 拒绝删除(只能停用)
物料停用 → 联动所有 SKU 不可被新单引用
存量单据保留对已停用 SKU 的引用(历史完整)
```

### UC-MD-07 主数据看板 — `use-cases.md:106`

```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  物料/往来单位总数, 无SKU物料/无价格物料预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

---

## §2 实现证据（L3 代码路径，方法锚点 + 关键行为断言）

> 引用格式：`module-master-data/erp-md-service/.../<X>.java#<method>`（方法锚点 + 行为断言；行号为写时实测导航，漂移不构成引用失效）。master-data 是 DAG 根域，自身不跨域调用下游业务域；跨域访问（价格表/引用检查）经 SPI（`IErpMdSupplierPriceResolver` / `IErpMdCustomerPriceResolver` / `IErpMdSkuReferenceChecker`）解耦，避免基础域反向依赖构成依赖环。

### UC-MD-01 扫码开单

- **查找**：`ErpMdMaterialSkuBizModel#findSkuByBarcode:76-85` @BizQuery —— barcode 空→null 守卫（:79-81）+ `QueryBean` `addFilter(eq("barcode", barcode))`（:82-83）+ `findFirst`（:84）。
- **唯一性（应用层）**：`ErpMdMaterialSkuBizModel#defaultPrepareSave:230-233` + `defaultPrepareUpdate:236-239`（CRUD 钩子）→ `enforceBarcodeUnique:255-271` —— 配置 `CONFIG_SKU_BARCODE_UNIQUE`（`ErpMdConstants:20` 默认 true）开时，barcode 非空 + 已被其他 SKU 占用 → 抛 `ERR_SKU_BARCODE_DUPLICATE`（:267-269，按 entity.id 排除自身 :266）。
- **DB 唯一索引（关键缺口）**：ORM `app-erp-master-data.orm.xml:408-418` ErpMdMaterialSku 仅 3 个非唯一索引（`IDX_MD_MATERIAL_SKU_MATERIAL_ID/UOM_ID/TAX_RATE_ID`），**无 barcode UK**——代码注记 `:50`/`:253` "Deferred DB unique index G1"（TOCTOU 窗口：并发 save 时两请求均通过 `findFirst` 查无 → 双双落库 → 唯一性失效）。
- ⚠️ **自动填充（物料/SKU/单位/数量/价格）编排属消费域**（purchase/sales/inventory 单据行）：master-data 仅暴露 `findSkuByBarcode` 原语，消费域单据行 onEvent 调用并自动填充——**本切片仅审 master-data 暴露的原语是否符合 L1**（自动填充归 purchase A1.15-A1.17 / sales A1.18-A1.21 / inventory A1.25-A1.27 范围，Non-Goal）。

### UC-MD-02 多单位换算落账

- **引擎**：`ErpMdUoMConversionBizModel#convertQty:46-75` @BizQuery —— qty null→ZERO 守卫（:53）+ 同单位直接返回原值 setScale(4, HALF_UP)（:57-59）+ `resolveConversionRate:82-90`（物料级[materialId 非空] → 通用[materialId null] 两层 findRate）+ 严格模式 `CONFIG_UOM_CONVERSION_STRICT`（`ErpMdConstants:24` 默认 true）strict=true 无系数抛 `ERR_UOM_CONVERSION_NOT_FOUND`（:65-69）/ strict=false 兜底 `fallbackSkuConversionRate:121-136`（查 SKU.conversionRate）+ 计算 `quantity.multiply(rate).setScale(4, HALF_UP)`（:74）。
- **findRate:97-114**：按 (materialId|null, fromUoMId, toUoMId) 精确匹配 ErpMdUoMConversion 取 conversionRate（:108-112）。
- **UoMGroup 实体（设计分歧但行为等价）**：L1 字面"单位组(UoMGroup)内换算系数一致"——实仓**无 UoMGroup 实体**（设计选择：以显式 per-pair 行 `ErpMdUoMConversion` + SKU.conversionRate 兜底替代组约束）。**行为等价**（同一物料 SKU 单位换算经物料级 + 通用 + SKU 兜底三层保证一致性）。
- ⚠️ **baseQty 落账属消费域**（purchase/sales/inventory 单据行 baseQty 字段）：master-data 仅提供 `convertQty` 原语，**本切片仅审 master-data 暴露的原语**。

### UC-MD-03 价格优先级解析

- **三级链**：`ErpMdMaterialSkuBizModel#resolvePrice:128-154` @BizQuery —— **manualPrice 即返**（:135-137）→ **customerPriceResolver SPI**（IErpMdCustomerPriceResolver，partnerId 非空时调 `resolveCustomerPrice` 返回 ResolvedPrice，命中 unitPrice 即返 :140-146）→ **supplierPriceResolver SPI**（IErpMdSupplierPriceResolver，partnerId 非空时调 `resolveSupplierPrice` 返回 BigDecimal，命中即返 :147-152）→ **SKU 默认档兜底** `pickDefaultTierPrice:311-325`（:153）。
- **billType 选档**：`pickDefaultTierPrice:311-325` —— PURCHASE→purchasePrice / WHOLESALE→wholesalePrice / RETAIL→retailPrice / 其他→salePrice（nullSafe 兜底 ZERO）。
- **resolvePriceWithSource:158-176**：增强版返回 ResolvedPrice（含 pricingSource 标记 + customerPriceResolver 命中分支 + SKU 默认档兜底）。
- **关键缺口（grep 实测，跨 `module-master-data` + `module-purchase` + `module-sales` 生产代码）**：
  - **IErpMdCustomerPriceResolver 有生产实现**：`module-sales/erp-sal-service/.../ErpSalCustomerPriceResolver.java:40 implements IErpMdCustomerPriceResolver`（sales 域 customer 价格清单）✅。
  - **IErpMdSupplierPriceResolver 无生产实现**：grep `implements IErpMdSupplierPriceResolver` 全生产代码 = **0 命中**（仅 `module-master-data/erp-md-service/src/test/.../TestStubSupplierPriceResolver.java:19` 测试桩）→ `ErpMdMaterialSkuBizModel.supplierPriceResolver` @Nullable + 无 Bean → 注入 null → :147 条件跳过 → **采购单据价格表层 no-op 恒落默认档**（SPI doc 自标"默认无实现时返回 null…下游接线归 Deferred"，但 Deferred 未经 §4 三判据人工批准痕迹，详见 §5/§6）。

### UC-MD-04 最低价校验拦截

- **OFF/WARN/HARD 派遣**：`ErpMdMaterialSkuBizModel#validatePrice:178-203` @BizQuery —— `resolvePriceValidationLevel:352-369`（materialCategoryId null 或 category 不存在 → WARN；读 category.priceValidationLevel，OFF/WARN/HARD 合法值直接返回；**非字典合法值（含 G5 孤儿"20"）→ WARN 宽松** :367-368）→ OFF 直接 `PriceValidationResult(true, false, null, level)`（:185-187）→ 否则 `deriveMinPrice:331-346` + `finalVal<minPrice` 判定（:191）→ HARD 抛 `ERR_PRICE_BELOW_MIN`（:195-200）/ WARN 放行带 warning=true（:201-202）/ 不低于底线返回 passed=true warning=false（:192-194）。
- **minPrice 来源（关键分歧）**：L1 字面"SKU.minPrice"（独立底线字段）——实仓 ORM `app-erp-master-data.orm.xml:377-396` ErpMdMaterialSku 字段列表（propId 1-18）**无 `minPrice` 列**（grep 零命中）。`deriveMinPrice:331-346` 实为**派生底线** = min{purchase, wholesale, retail, sale 正值}（代码注记 `:328-330` "Explore 裁定选项 b：派生底线"）。**4 档全设价的 SKU 无法有低于最廉档的 minPrice**——派生行为等价但**与 L1 命名字段分歧**。
- **priceValidationLevel 默认"20"孤儿值（关键分歧）**：ORM `app-erp-master-data.orm.xml:344` ErpMdMaterialCategory.priceValidationLevel `defaultValue="20"`——但 dict `erp-md/price-validation`（orm.xml:72-76）**仅 OFF/WARN/HARD 三值**，**"20" 是孤儿非字典值**。代码 :367-368 把未知值（含"20"）统一当 WARN（非硬编码：新种子分类默认"20"静默得 WARN 语义，与字典语义 WARN="警告放行"对齐，但**孤儿值违反字典契约**）。

### UC-MD-05 默认 SKU 兜底

- **resolveSku**：`ErpMdMaterialSkuBizModel#resolveSku:101-124` @BizQuery —— 物料 active 校验 `isMaterialActive:375-385`（materialId null 或物料 status != ACTIVE → 返回 null）+ unitId 非空按 (materialId, uoMId) 匹配 SKU（:109-117）+ 兜底 `findDefaultSku:89-99`（:118）+ 无默认且 `sku-default-required` 配置开（`ErpMdConstants:18` 默认 true）→ 抛 `ERR_SKU_DEFAULT_REQUIRED`（:119-122）。
- **findDefaultSku:89-99**：物料 active 校验 + 按 (materialId, isDefault=true) 查第一条。
- **恰一默认 SKU 约束（关键缺口）**：L1 "每物料必有且仅有一个默认 SKU（约束）"——实仓 ORM 无 `(materialId, isDefault=true)` DB UK（grep 零），约束**仅应用约定非 DB 强制**（物理上多默认 SKU 可同时存在）。代码注记未显式声明 Deferred（无 G 编号），但既无 DB UK 又无应用层守卫（save 时不查既有默认 SKU 数量），**多默认 SKU 物理可能**。
- **auto-create-default（关键缺口）**：`ErpMdConstants:22` 声明 `CONFIG_SKU_AUTO_CREATE_DEFAULT`（默认 true）但代码注记 `:21` "本计划仅声明配置键，自动创建逻辑归后续"——**自动创建逻辑未实现**（grep 全 `module-master-data` `autoCreateDefaultSku|createDefaultSkuIfAbsent` 零业务命中）。L1 未显式要求"自动创建"，但 `sku-multi-unit.md:244-246`（L2 设计参考）显式要求"创建新物料时自动创建默认 SKU unitId=baseUnitId"——L2 与 L1 冲突时按 §4 Q1 以 L1 为准，**L1 未要求故不算 P1 缺口**（仅 L2 设计参考缺失）。

### UC-MD-06 SKU 状态约束

- **默认 SKU 守卫**：`ErpMdMaterialSkuBizModel#validateSkuDeactivation:207-225` @BizQuery —— 守卫 1：sku.isDefault=true 且 `hasOtherActiveSku:292-305`（同 materialId + id 不同，**无 status 过滤**因 SKU 无 status 列）= false → 抛 `ERR_CANNOT_DEACTIVATE_DEFAULT_SKU`（:212-217）。守卫 2：`skuReferenceChecker != null && isReferencedByBill(sku)` → 抛 `ERR_SKU_REFERENCED_BY_BILL`（:220-223）。
- **删除钩子**：`ErpMdMaterialSkuBizModel#defaultPrepareDelete:245-249` —— delete 经 CRUD 钩子触发 `validateSkuDeactivation(entity.id, context)`（:248）。
- **物料停用联动（读侧 filter）**：`ErpMdMaterialBizModel#defaultPrepareUpdate:63-73` —— 物料 status→INACTIVE 时调 `onMaterialDeactivated:78-81`（默认空扩展点，不阻断）+ SKU 侧经 `isMaterialActive:375-385`（读物料 status）在 resolveSku/findDefaultSku 时**读侧 filter**（INACTIVE 物料的 SKU 返回 null）。
- **历史完整**：`useLogicalDelete=true`（orm.xml:374）+ ORM to-one 关联（默认不过滤 delVersion）→ 历史单据的 skuId 引用仍可解析到（已停用/已删除）SKU 实体 ✅。
- **关键缺口（grep 实测，跨 `module-master-data` + `module-purchase` + `module-sales` + `module-inventory` 生产代码）**：
  - **SKU 无独立 status 列**：ORM `app-erp-master-data.orm.xml:377-396` ErpMdMaterialSku 字段列表（propId 1-18）**无 status 列**（仅 isDefault 布尔）→ L1/设计 `sku-multi-unit.md:266-268` "SKU 独立停用 → 该 SKU 不可被新单据引用 / 其他 SKU 仍可用"**不可达**（`hasOtherActiveSku:292-305` 把 active 当同物料不同 id，**无 status 过滤**）。代码注记 `:289` G2 + `:374` "Phase 3 Decision 选 (b) SKU 独立 status 经物料级 status 承载"——但物料级 status 仅承载物料停用语义，**SKU 级独立停用语义不可达**。
  - **IErpMdSkuReferenceChecker 无生产实现**：grep `implements IErpMdSkuReferenceChecker` 全生产代码 = **0 命中**（仅 `module-master-data/erp-md-service/src/test/.../TestStubSkuReferenceChecker.java:18` 测试桩）→ `ErpMdMaterialSkuBizModel.skuReferenceChecker` @Nullable + 无 Bean → 注入 null → :220 条件 `skuReferenceChecker != null` 永假 → **生产环境被活跃采购/销售/库存单据引用的 SKU 仍可删除**（仅默认 SKU 守卫触发）——UC-MD-06 最显著功能缺口。
  - **DB 层 FK 兜底（P0 候选评估）**：purchase/sales/inventory →line ORM 虽 `to-one refEntityName="app.erp.md.dao.entity.ErpMdMaterialSku"`（如 sales.orm.xml:424/576/972/1085/1150 + inv.orm.xml:251/320/397/511/567/678/773/871 + pur 类似），但**Nop 平台 to-one 是逻辑/ORM 层虚拟 join（非 DB 物理 FK），无 CASCADE 兜底** → 生产环境删除 SKU 后历史单据 skuId 变悬空引用（虽 useLogicalDelete=true 软删保留物理行，但操作可见性丢失）。

### UC-MD-07 主数据看板

- **KPI 实时聚合**：`ErpMdDashboardBizModel#getDashboardKpi:47-67` @BizQuery —— `ormTemplate.runInSession`（:49）+ `materialDao.countByQuery(new QueryBean())`（:53）+ 按 partnerType=CUSTOMER/SUPPLIER 分别 partnerDao.countByQuery（:54-55）+ 按 status=INACTIVE 计数 material/partner（:56-57）→ LinkedHashMap 返回 materialCount/customerCount/vendorCount/inactiveMaterialCount/inactivePartnerCount（:59-65）。**经 IDaoProvider + IOrmTemplate 实时聚合**（非硬编码）✅。
- **无 SKU 物料预警**：`ErpMdDashboardBizModel#findMaterialWithoutSkuAlert:70-98` @BizQuery —— 单字段取 SKU.materialId 集合（:74-80 带 `ALERT_MAX_ROWS=5000` 硬上限 :76）+ 物料逐行比对（:82-95 带 `ALERT_MAX_ROWS` 上限 :84）+ 返回无 SKU 物料明细行（materialId/code/name/status）。
- **无价格 SKU 预警**：`ErpMdDashboardBizModel#findSkuWithoutPriceAlert:101-125` @BizQuery —— SKU 逐行价格比对（:105-108 带 `ALERT_MAX_ROWS` 上限 :107）+ 四档价（purchase/sale/wholesale/retail）均为 0/null → 触发（:111-114）+ 返回无价格 SKU 明细行（skuId/skuCode/materialId）。
- **预警阈值硬编码（关键缺口）**：`ALERT_MAX_ROWS=5000`（:40 `private static final int`）是**服务端硬上限**而非 L1 "阈值来自系统配置非硬编码"。**无 `erp-md.dashboard-*-threshold` config key**（grep 全 `module-master-data` 确认零），运营无法调整预警扫描行数上限或阈值条件。**与 `P2-RC-009` mfg 看板同型**（mfg dashboard 亦用状态/日期驱动而非 config threshold）。
- **行级权限（关键缺口）**：`ErpMdDashboardBizModel` @BizModel（:32 非实体聚合）+ 注入 IDaoProvider/IOrmTemplate（:42-45）+ IServiceContext context 全文参数列表中存在但**未用于 scope**（:48/71/102 收到但 method body 不读 context.getUserId()/getOrgId()）+ `new QueryBean()` 无 orgId filter。**与 `P1-MA2-093`**（A2.18 :99-101 显式列 11 dashboard 直访绕鉴权管道，**resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`**）**同根因同控制点**——R1.29 已注入全局 `ErpOrgIsolationQueryTransformer`（实测 `module-common-service/erp-common-service/.../ErpOrgIsolationQueryTransformer.java:34 implements IQueryTransformer`，config-gated），但 R1.29 是否覆盖 `ErpMdDashboardBizModel` 的 IDaoProvider 直访路径**需运行时确认**（SP-1 交 MA4）。

---

## §3 测试证据（L4 测试断言，注明断言强度）

> 测试根目录 `module-master-data/erp-md-service/src/test/java/`。引用 `<TestFile>.java#<method>`，注明断言强度（强/弱/仅冒烟）。

### UC-MD-01 扫码 — `TestErpMdSkuServices.java`

- `testFindSkuByBarcode:58-78` —— **强**：命中（BC-HIT 查到 skuId）/ 未命中（NOT-EXIST 返回 null）。
- `testBarcodeDuplicateRejected:182-214` —— **强**：经 GraphQL `ErpMdMaterialSku__save` 触发 `defaultPrepareSave` 钩子 + 重复 barcode 抛 `ERR_SKU_BARCODE_DUPLICATE`。
- **缺口（零测试）**：DB 唯一索引（G1 TOCTOU 实测窗口需并发场景）/ 应用层 TOCTOU 并发 save 同 barcode 的实际数据完整性事件。

### UC-MD-02 多单位换算 — `TestErpMdSkuServices.java`

- `testConvertQtyMaterialLevel:129-140` —— **强**：物料级 1 箱=24 瓶，10 箱×24 = 240.0000。
- `testConvertQtyGenericFallback:142-153` —— **强**：通用层 1 托盘=576 瓶，2 托盘×576 = 1152.0000。
- `testConvertQtyStrictNotFound:155-168` —— **强**：strict=true 未命中系数抛 `ERR_UOM_CONVERSION_NOT_FOUND`。
- `testConvertQtySameUnit:170-178` —— **强**：同单位（瓶→瓶）返回 7.0000。

### UC-MD-03 价格优先级 — `TestErpMdSkuPriceValidation.java`

- `testResolvePriceManualWins:60-70` —— **强**：手工价 99.99 > SPI 价格表 8.88 命中。
- `testResolvePriceFromSupplierList:72-82` —— **强**：SPI 价格表 7.77 击败默认档采购价 10.00（经 TestStubSupplierPriceResolver 注入）。
- `testResolvePriceDefaultTier:84-99` —— **强**：4 单据类型 × 精确档位（PURCHASE 10.0000 / WHOLESALE 12.0000 / RETAIL 18.0000 / DEFAULT 15.0000，scale=4）。
- **缺口（零测试）**：`IErpMdSupplierPriceResolver` 生产实现零测试（仅测试桩）；`IErpMdCustomerPriceResolver` 生产实现零测试（虽 sales 域有 `ErpSalCustomerPriceResolver`，但 master-data 切片未跨域注入测）。

### UC-MD-04 最低价校验 — `TestErpMdSkuPriceValidation.java`

- `testValidatePriceHardReject:103-119` —— **强**：HARD + finalPrice 9 < 底线 10 → `ERR_PRICE_BELOW_MIN`。
- `testValidatePriceWarnAllows:121-135` —— **强**：WARN + finalPrice 9 < 底线 10 → passed=true + warning=true + level=WARN。
- `testValidatePriceOff:137-151` —— **强**：OFF + finalPrice 1 < 底线 → passed=true + warning=false + level=OFF（直接通过不校验）。
- `testValidatePriceAboveMinNoWarning:153-166` —— **强**：HARD + finalPrice 20 > 底线 10 → passed=true + warning=false（高于底线不警告）。
- **缺口（零测试）**：minPrice 派生 vs L1 命名分歧（4 档全设价的 SKU 派生=最廉档，无法独立底线测试）/ priceValidationLevel="20" 孤儿值实际 WARN 语义影响面（新种子分类无显式设置时静默得 WARN）。

### UC-MD-05 默认 SKU — `TestErpMdSkuServices.java`

- `testFindDefaultSku:82-94` —— **强**：有默认 SKU（isDefault=true 命中）/ 无默认 SKU（返回 null）。
- `testResolveSkuByUnit:96-114` —— **强**：按 unitId 匹配 + unitId=null 兜底默认 SKU。
- `testResolveSkuNoDefaultRequired:116-125` —— **强**：无默认 SKU + sku-default-required=true（默认）→ 抛 `ERR_SKU_DEFAULT_REQUIRED`。
- **缺口（零测试）**：(materialId, isDefault=true) 多默认 SKU 物理可能性（DB UK 缺失）/ sku-auto-create-default config 声明未实现的运行时确认。

### UC-MD-06 SKU 状态约束 — `TestErpMdSkuStatusConstraints.java`

- `testCannotDeactivateOnlyDefaultSku:60-70` —— **强**：物料仅一默认 SKU → 停用/删除抛 `ERR_CANNOT_DEACTIVATE_DEFAULT_SKU`。
- `testCanDeactivateNonDefaultSku:72-82` —— **强**：默认 + 非默认 SKU → 停用非默认 SKU 放行（返回 true）。
- `testMaterialDeactivateCascadeGuard:86-113` —— **强**：物料 ACTIVE 时 resolveSku 返回 SKU / 物料 INACTIVE 后 resolveSku + findDefaultSku 均返回 null（**读侧 filter 联动**）。
- `testDeleteReferencedSkuRejected:117-128` —— **强（仅 SPI 桩）**：refChecker.markReferenced → 抛 `ERR_SKU_REFERENCED_BY_BILL`。**生产环境无 SPI 实现 → 此守卫永不触发**。
- `testDeleteUnreferencedSkuOk:130-141` —— **强**：未标记引用 → 放行（返回 true）。
- **缺口（零测试）**：SKU 独立 status 列缺失（无独立停用 mutation 可测）/ `IErpMdSkuReferenceChecker` 生产实现零测试 / 被活跃单据引用的 SKU 在生产删除的实际数据完整性事件。

### UC-MD-07 主数据看板 — `TestErpMdDashboard.java` + E2E `tests/e2e/dashboards/master-data.value.spec.ts`

- `testKpiEmptyDatasetReturnsZeros:41-49` —— **强**：空数据集 5 KPI 全 0。
- `testKpiCountsByPartnerTypeAndStatus:51-71` —— **强**：3 物料（1 ACTIVE + 2 INACTIVE）+ 4 往来单位（2 客户[1+1] + 2 供应商[2+0]）→ materialCount=3/customerCount=2/vendorCount=2/inactiveMaterialCount=2/inactivePartnerCount=1 精确计数。
- `testMaterialWithoutSkuAlertTriggersAndNot:73-85` —— **强**：物料 A 有 SKU 不触发 / 物料 B 无 SKU 触发（精确命中 112L）。
- `testSkuWithoutPriceAlertTriggersAndNot:87-100` —— **强**：SKU A 有采购价不触发 / SKU B 无任何价触发（精确命中 222L）。
- E2E `master-data.value.spec.ts:5-18` —— **强**：经 GraphQL 断言 materialCount=4/customerCount=2/vendorCount=2/inactiveMaterialCount=0/inactivePartnerCount=0 + `findMaterialWithoutSkuAlert`/`findSkuWithoutPriceAlert` 在种子基线均空集。
- **缺口（零测试）**：阈值 config 化（无 config key 可测）/ 行级权限（无 ctx.getUserId()/getOrgId() 测）/ `ALERT_MAX_ROWS=5000` 边界（数据集超限截断行为）。

---

## §4 运行时行为证据（L5，与 MA2 去重）

- **无 master-data 专属 MA2 报告**（§0 已声明）→ **L5 无可复用 MA2 行为证据**，本切片以 L3 代码 + L4 单测为行为依据。
- **跨域 daoFor 现状证据**：P1-MA1-022（resolved plan `2026-07-29-2225-1`）`data-dependency-matrix.md §9`——master-data 是 DAG 根域，被 11 域 ~100+ 文件经 `daoFor(ErpMd*)` 只读访问；读侧统一裁决为"md 子集=可迁移 / 业务域子集=永久只读豁免"，**本切片不重审此维度**（不同维度：需求符合性 vs 平台一致性）。
- **静态存疑点**（无法静态定论，需运行时确认）登记于 §7（SP-1~SP-5），交 MA4 展开。

---

## §5 符合性结论（五级追踪矩阵 + 每 UC 结论，methodology §2 判据）

> 每个 UC 一行；候选缺口逐条对照 L1 验收标准，§2 取最高分级；P1 项含 §4 三判据复核结论。

### 五级追踪矩阵

| UC 编号 | L2 owner doc 契约 | L3 代码 | L4 测试 | L5 运行时 | 符合性结论 |
|---------|------------------|---------|---------|-----------|-----------|
| UC-MD-01 | `sku-multi-unit.md §多 barcode`（设计参考，冲突以 L1 为准；§条码唯一约束 :139-141 未声明 Deferred，G1 DB UK 缺失属代码注记 AI 自标） | `ErpMdMaterialSkuBizModel#findSkuByBarcode:76-85` + `enforceBarcodeUnique:255-271` + `defaultPrepareSave/Update:230-239` | `TestErpMdSkuServices#testFindSkuByBarcode`（强）+ `testBarcodeDuplicateRejected`（强） | L3+L4 强证实查找 + 应用层唯一性；DB UK 缺失归 P2（SP-3 G1 TOCTOU 窗口交 MA4） | **接受 on 查找 + P2 on DB UK 缺失（P2-RC-058）** |
| UC-MD-02 | `sku-multi-unit.md §多单位换算`（设计参考；无 UoMGroup 实体属 L2 设计差异，行为等价） | `ErpMdUoMConversionBizModel#convertQty:46-75` + `resolveConversionRate:82-90` + `findRate:97-114` + `fallbackSkuConversionRate:121-136` | `TestErpMdSkuServices#testConvertQtyMaterialLevel/GenericFallback/StrictNotFound/SameUnit`（4 强） | L3+L4 强证实物料级/通用/SKU 兜底/同单位四路径；baseQty 落账属消费域（Non-Goal） | **接受** |
| UC-MD-03 | `sku-multi-unit.md §多档价格/§价格优先级`（设计参考；SPI doc :13-16 "默认无实现时返回 null…下游接线归 Deferred" AI 自标无人工批准痕迹） | `ErpMdMaterialSkuBizModel#resolvePrice:128-154` + `resolvePriceWithSource:158-176` + `pickDefaultTierPrice:311-325` | `TestErpMdSkuPriceValidation#testResolvePriceManualWins/FromSupplierList/DefaultTier`（3 强，SPI 经 TestStub） | L3+L4 强证实三级链 + customer 层生产实现；**supplier SPI 无生产实现→采购恒落默认档** | **接受 on 手工价/customer 层/默认档 + P1 on supplier SPI 无生产实现（P1-RC-063）** |
| UC-MD-04 | `sku-multi-unit.md §多档价格/§配置项`（设计参考；G3 minPrice 派生 + G5 默认"20"孤儿值 AI 自标） | `ErpMdMaterialSkuBizModel#validatePrice:178-203` + `resolvePriceValidationLevel:352-369` + `deriveMinPrice:331-346` | `TestErpMdSkuPriceValidation#testValidatePriceHardReject/WarnAllows/Off/AboveMinNoWarning`（4 强） | L3+L4 强证实 OFF/WARN/HARD 三态派遣；minPrice 派生 vs L1 命名分歧 + "20" 孤儿值属 P2 | **接受 on OFF/WARN/HARD + P2 on minPrice 派生+孤儿值（P2-RC-057）** |
| UC-MD-05 | `sku-multi-unit.md §默认 SKU`（设计参考；auto-create-default L2 :244-246 要求但 L1 未要求，以 L1 为准；DB UK 缺失 AI 自标） | `ErpMdMaterialSkuBizModel#findDefaultSku:89-99` + `resolveSku:101-124` + `isMaterialActive:375-385` | `TestErpMdSkuServices#testFindDefaultSku/ResolveSkuByUnit/ResolveSkuNoDefaultRequired`（3 强） | L3+L4 强证实 resolveSku/findDefaultSku/sku-default-required；(materialId,isDefault) DB UK 缺失归 P2；auto-create-default 归 L2 设计参考缺失非 L1 缺口 | **接受 on resolveSku 核心 + P2 on (materialId,isDefault) DB UK 缺失（P2-RC-059）** |
| UC-MD-06 | `sku-multi-unit.md §SKU 状态管理`（设计参考；G2 SKU 无 status 列 AI 自标"Phase 3 Decision 选 (b) 物料级 status 承载" + 引用检查 SPI doc AI 自标，均无人工批准痕迹） | `ErpMdMaterialSkuBizModel#validateSkuDeactivation:207-225` + `defaultPrepareDelete:245-249` + `hasOtherActiveSku:292-305` + `ErpMdMaterialBizModel#defaultPrepareUpdate:63-73` + `onMaterialDeactivated:78-81` | `TestErpMdSkuStatusConstraints#testCannotDeactivateOnlyDefaultSku/CanDeactivateNonDefaultSku/MaterialDeactivateCascadeGuard/DeleteReferencedSkuRejected[SPI 桩]/DeleteUnreferencedSkuOk`（5 强） | L3+L4 强证实默认 SKU 守卫+物料停用读侧联动；**SKU 无 status 列致独立停用不可达 + 引用检查 SPI 无生产实现→被引用 SKU 生产可删** | **P1 on SKU status+引用检查（P1-RC-062）+ 接受 on 默认 SKU 守卫+物料级联+历史完整** |
| UC-MD-07 | `../dashboards.md §主数据看板` + `sku-multi-unit.md §配置项`（设计参考；行级权限 + 阈值 config L1 显式要求） | `ErpMdDashboardBizModel#getDashboardKpi:47-67` + `findMaterialWithoutSkuAlert:70-98` + `findSkuWithoutPriceAlert:101-125` + `ALERT_MAX_ROWS:40` | `TestErpMdDashboard#testKpiEmptyDatasetReturnsZeros/KpiCountsByPartnerTypeAndStatus/MaterialWithoutSkuAlertTriggersAndNot/SkuWithoutPriceAlertTriggersAndNot`（4 强）+ E2E `master-data.value.spec.ts`（强） | L3+L4+E2E 强证实 KPI 实时聚合+预警；**阈值硬编码 ALERT_MAX_ROWS=5000 + 行级权限 BizModel 未应用** | **接受 on KPI 聚合 + P2 on 阈值硬编码（P2-RC-056）+ reuse P1-MA2-093 on 行级权限（R1.29 已 resolved，SP-1 交 MA4）** |

### 每 UC 符合性结论（逐条验收标准 + §2 判据 + §4 三判据复核）

#### UC-MD-01 扫码开单 → **接受 on 查找 + P2 on DB UK 缺失**

逐条：
- ① **扫描条码 → findSkuByBarcode(barcode) → SKU + 物料**：`findSkuByBarcode:76-85` 经 QueryBean eq barcode + findFirst ✅（命中/未命中双路径强测）→ **接受**。
- ② **自动填充（物料/SKU/单位/数量/价格）**：属消费域（purchase/sales/inventory 单据行 onEvent）——master-data 仅暴露 `findSkuByBarcode` 原语，**Non-Goal**（自动填充归 A1.15-A1.27 业务域切片范围）。
- ③ **条码全局唯一**：应用层 `enforceBarcodeUnique:255-271` 经 `CONFIG_SKU_BARCODE_UNIQUE` config-gated 默认 true + GraphQL save 钩子触发 ✅；**但 ORM 无 DB 唯一索引**（orm.xml:408-418 仅非唯一 IDX_MD_MATERIAL_SKU_*）→ TOCTOU 窗口（并发 save 同 barcode 两请求均通过 findFirst 查无 → 双双落库 → 唯一性失效）→ 命中 **§2 P2①（次要验收标准未完全满足——主路径[应用层守卫]OK，边界[DB UK 缺失 TOCTOU]弱）**。详见 **P2-RC-058**。

**§4 三判据复核（UC-MD-01 候选 P2：barcode DB UK 缺失）**：
- (i) plan 含独立 plan-audit 通过记录：✗ 产生本功能的计划（`2026-07-07-0024-1`）为 2026-07 实现期产物，无 RC 式独立 plan-audit 通过记录裁决 barcode DB UK 缺失裁剪。
- (ii) owner doc 显式 documented simplification 标注且经人工批准：代码注记 `:50`/`:253` "Deferred DB unique index G1" 是 **AI 自标**（methodology §4 line 168），`sku-multi-unit.md §条码唯一约束 :139-141` 字面声明"全局唯一（允许 null，仅非 null 时强制唯一）。约束定义以 orm.xml 为权威源"——**未声明 Deferred**（owner doc 与 L1 一致要求 UK），但 `git log -- docs/design/master-data/sku-multi-unit.md module-master-data/model/app-erp-master-data.orm.xml` 全部提交作者 = AI（canonical）无人工批准痕迹。
- (iii) product-scope 范围裁剪登记：✗ barcode 全局唯一为 product-scope master-data 域列明核心约束，未列入裁剪。
- **三判据均不成立 → 非 documented simplification → 按 Q4=(a) 评估**：非 P0（不破坏活跃数据——TOCTOU 窗口需并发触发 + 应用层守卫默认 active 已保护主路径 + 无 GL/库存破坏）+ 非 P1（主路径应用层守卫强测覆盖 + TOCTOU 是边界并发场景），定 **P2**。**修复触及 ORM 结构变更[ErpMdMaterialSku 加 UK_MD_MATERIAL_SKU_BARCODE barcode UK]须 ask-first + 独立 plan-audit §5 ORM 类**。

#### UC-MD-02 多单位换算落账 → **接受**

逐条：
- ① **录入单位/数量 + 换算 baseQty = 数量 × conversionFactor**：`convertQty:46-75` 三层（物料级 → 通用 → SKU 兜底）+ strict 模式 + scale=4 HALF_UP + 同单位原值返回 ✅（4 @Test 强测覆盖 10×24=240 / 2×576=1152 / strict 抛错 / 7→7 四路径）→ **接受**。
- ② **落账 baseQty 用于库存/成本计算**：属消费域（业务单据行 baseQty 字段）——master-data 仅提供 convertQty 原语，**Non-Goal**。
- ③ **单位组(UoMGroup)内换算系数一致**：**无 UoMGroup 实体**——L2 `sku-multi-unit.md §单位组与换算系数 :80-98` 设计有 UoMGroup，实仓以显式 per-pair 行 + SKU 兜底替代（行为等价：同物料 SKU 单位换算经三层一致性保证）。**L1 字面"单位组(UoMGroup)内换算系数一致"满足**（一致性经显式 per-pair + SKU.conversionRate 实现；UoMGroup 是实现载体差异，行为等价）→ **接受**（非 P2，因行为等价 L1 满足）。

#### UC-MD-03 价格优先级解析 → **接受 on 手工价/customer 层/默认档 + P1 on supplier SPI 无生产实现**

逐条：
- ① **手工价 > 价格表 > SKU 默认档**：`resolvePrice:128-154` 三级链 manualPrice 即返 → customerPriceResolver SPI → supplierPriceResolver SPI → pickDefaultTierPrice ✅（框架完整 + 经 TestStub 强测覆盖三级命中）。
- ② **单据行.单价 若手工填 → 用手工价**：`:135-137` manualPrice 非空即返 ✅ → **接受**（强测 `testResolvePriceManualWins` 99.99 击败 SPI 8.88）。
- ③ **否则 查价格表(客户专属/促销) → 命中则用**：`IErpMdCustomerPriceResolver` 有生产实现 `module-sales/.../ErpSalCustomerPriceResolver.java:40`（customer 专属价格清单）✅ → **接受 on customer 层**。
- ④ **否则 查价格表（供应商专属）→ 命中则用**：**`IErpMdSupplierPriceResolver` 无生产实现**（grep 全生产代码 0 命中，仅 TestStubSupplierPriceResolver 测试桩）→ 生产环境 `ErpMdMaterialSkuBizModel.supplierPriceResolver` 注入 null → :147 条件 `supplierPriceResolver != null` 永假跳过 → **采购单据价格表层 no-op 恒落默认档** → 命中 **§2 P1①（功能实质偏离验收标准——L1 三级链之"价格表"层在采购方向恒不触发）**。详见 **P1-RC-063**。
- ⑤ **否则 用 SKU 默认档（按单据类型选 purchase/wholesale/retail）**：`pickDefaultTierPrice:311-325` PURCHASE/WHOLESALE/RETAIL/DEFAULT 四档精确选 ✅（强测 4 单据类型 × scale=4 精确断言）→ **接受**。

**§4 三判据复核（UC-MD-03 候选 P1：supplier SPI 无生产实现）**：
- (i) plan 含独立 plan-audit 通过记录：✗ 产生本功能的计划（`2026-07-07-0024-1`）无 RC 式独立 plan-audit 通过记录裁决 supplier SPI 接线裁剪。
- (ii) owner doc 显式 documented simplification 标注且经人工批准：`IErpMdSupplierPriceResolver` SPI doc（接口源文件）"默认无实现时返回 null…下游接线归 Deferred" 是 **AI 自标**（methodology §4 line 168）；`sku-multi-unit.md §价格优先级 :180-198` 字面声明三级链完整未标 Deferred；`git log -- module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdSupplierPriceResolver.java` 全部 AI 提交无人工批准痕迹。
- (iii) product-scope 范围裁剪登记：✗ 价格优先级三级链是 product-scope master-data 域列明核心能力，未将 supplier 价格表列入裁剪。
- **三判据均不成立 → 非 documented simplification → 按 Q4=(a) 强制实现 P1**。**须人工确认 product-scope 是否要求 supplier 价格表**：若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现。**修复属代码逻辑类预授权**（purchase 域新增 `ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver` 注入 ErpMdMaterialSkuBizModel + supplier 价格表查询逻辑[查 ErpPurPriceList/ErpPurPriceListLine by partnerId+materialId+effectiveDate]，**纯 BizModel/SPI 实现预授权不触 §5 ask-first**）；**UC-MD-03 supplier SPI 接线须与 purchase 域 A1.15-A1.17 协同**。

#### UC-MD-04 最低价校验拦截 → **接受 on OFF/WARN/HARD + P2 on minPrice 派生+孤儿值**

逐条：
- ① **若 最终售价 < SKU.minPrice**：L1 字面"SKU.minPrice"为独立底线字段——实仓 ORM 无 minPrice 列，`deriveMinPrice:331-346` 实为**派生底线** = min{purchase,wholesale,retail,sale 正值}。**4 档全设价的 SKU 无法有低于最廉档的 minPrice**（派生底线=最廉档）→ **行为等价但与 L1 命名字段分歧** → 命中 **§2 P2①（次要验收标准未完全满足——主路径[OFF/WARN/HARD 派遣]OK，边界[minPrice 派生 vs L1 命名]弱）**。
- ② **MaterialCategory.priceValidationLevel == HARD → 拒绝**：`validatePrice:195-200` HARD + below → 抛 `ERR_PRICE_BELOW_MIN` ✅（强测）→ **接受**。
- ③ **== WARN → 警告但放行**：`:201-202` WARN + below → passed=true + warning=true ✅（强测）→ **接受**。
- ④ **配置 == OFF → 不校验**：`:185-187` OFF 直接 passed=true + warning=false ✅（强测）→ **接受**。
- **priceValidationLevel 默认"20"孤儿值**：ORM `orm.xml:344` `defaultValue="20"` 是**孤儿非字典值**（dict erp-md/price-validation 仅 OFF/WARN/HARD）。`:367-368` 把未知值（含"20"）统一当 WARN → **新种子分类默认"20"静默得 WARN 语义**（与 dict WARN="警告放行"行为对齐，但**孤儿值违反字典契约**，可维护性/语义漂移风险）→ 与 #1 minPrice 派生**合并登记 P2**（同 owner doc AI 自标 + 同行为等价但命名/契约分歧根因）。详见 **P2-RC-057**。

**§4 三判据复核（UC-MD-04 候选 P2：minPrice 派生 + "20" 孤儿值）**：
- (i) ✗ 无独立 plan-audit；(ii) 代码注记 `:328-330` "Explore 裁定选项 b：派生底线" + `:349-350` "G5：列默认值 '20' 为孤儿不参与逻辑" 是 **AI 自标**；`sku-multi-unit.md §多档价格 :171-178` 字面列出 minPrice 为独立字段（与 L1 一致），未声明派生 Deferred；`git log` 全 AI 提交无人工批准痕迹；(iii) ✗ product-scope 未裁剪。**三判据均不成立 → 非 documented simplification → 按 Q4=(a) 评估**：非 P0（GL 平衡不破坏——派生底线=最廉档 >= 真实 minPrice 故不会错误放行低于底线的价格）/ 非 P1（OFF/WARN/HARD 派遣主路径完整强测 + 派生行为等价 + "20" 实际语义=WARN 与 dict 一致故无运行时副作用），定 **P2**。**修复 minPrice 独立列触及 ORM 结构变更[ErpMdMaterialSku 加 minPrice 列]须 ask-first + 独立 plan-audit §5 ORM 类**；"20" 孤儿值修复为 ORM defaultValue 改 "WARN"（**触及 ORM 结构变更[默认值改]须 ask-first**）或 owner doc 补注 documented simplification（纯文档预授权）。

#### UC-MD-05 默认 SKU 兜底 → **接受 on resolveSku 核心 + P2 on (materialId,isDefault) DB UK 缺失**

逐条：
- ① **单据行未指定 SKU → resolveSku(物料) → 取 defaultFlag=true 的 SKU**：`resolveSku:101-124` 物料 active 校验 + unitId 匹配 + 兜底 `findDefaultSku:89-99` (materialId, isDefault=true) ✅（强测覆盖按单位匹配 + 兜底默认两条路径）→ **接受**。
- ② **每物料必有且仅有一个默认 SKU（约束）**：**ORM 无 (materialId, isDefault=true) DB UK**（grep 零），约束**仅应用约定非 DB 强制**——多默认 SKU 物理可能（save 时不查既有默认 SKU 数量 + 无 DB UK 兜底）→ 命中 **§2 P2①（次要验收标准未完全满足——主路径[应用层 findDefaultSku 取第一条]OK，边界[DB UK 缺失致多默认 SKU 物理可能]弱）**。详见 **P2-RC-059**。
- ③ **若无默认 SKU 且配置 sku-default-required → 报错**：`:119-122` 无默认 + `CONFIG_SKU_DEFAULT_REQUIRED` 默认 true → 抛 `ERR_SKU_DEFAULT_REQUIRED` ✅（强测）→ **接受**。

**§4 三判据复核（UC-MD-05 候选 P2：(materialId,isDefault) DB UK 缺失）**：
- (i) ✗；(ii) 代码无显式 Deferred 注记（无 G 编号），`sku-multi-unit.md §默认 SKU :232-249` 字面声明"每个物料必须有一个 defaultFlag = true 的 SKU"未声明 DB UK 缺失 Deferred；(iii) ✗。**三判据均不成立 → 按 Q4=(a) 评估**：非 P0（不破坏活跃数据——多默认 SKU 时 findDefaultSku 取第一条仍可用 + 无 GL/库存破坏）/ 非 P1（主路径 resolveSku 行为正确强测 + 边界 DB UK 缺失不阻断功能），定 **P2**。**修复触及 ORM 结构变更[ErpMdMaterialSku 加部分 UK (materialId) WHERE isDefault=true 或应用层 defaultPrepareSave 增 (materialId, isDefault=true) 唯一性校验]须 ask-first + 独立 plan-audit §5 ORM 类**（DB 部分索引可能需 dialect 支持；或纯应用层守卫属代码逻辑预授权）。

#### UC-MD-06 SKU 状态约束 → **P1 on SKU status+引用检查 + 接受 on 默认 SKU 守卫+物料级联+历史完整**

逐条：
- ① **停用唯一默认 SKU → 拒绝**：`validateSkuDeactivation:212-217` sku.isDefault=true 且 hasOtherActiveSku=false → 抛 `ERR_CANNOT_DEACTIVATE_DEFAULT_SKU` ✅（强测）→ **接受**。
- ② **SKU 被业务单据引用 → 拒绝删除（只能停用）**：**`IErpMdSkuReferenceChecker` 无生产实现**（grep 全生产代码 0 命中，仅 TestStubSkuReferenceChecker 测试桩）→ `skuReferenceChecker` @Nullable 注入 null → :220 条件永假跳过 → **生产环境被活跃采购/销售/库存单据引用的 SKU 仍可删除**（仅默认 SKU 守卫触发）→ 命中 **§2 P1②（异常路径未实现——L1 "被引用拒删"控制点生产零保护）**。
- ③ **SKU 独立停用**：**SKU 无独立 status 列**（ORM ErpMdMaterialSku 字段 propId 1-18 无 status）→ L1/设计 `sku-multi-unit.md:266-268` "SKU 独立停用 → 该 SKU 不可被新单据引用 / 其他 SKU 仍可用"**不可达**（`hasOtherActiveSku:292-305` 无 status 过滤）→ 命中 **§2 P1①（功能完全缺失——SKU 独立停用语义结构性不可实现）**。
- ④ **物料停用 → 联动所有 SKU 不可被新单引用**：`ErpMdMaterialBizModel#defaultPrepareUpdate:63-73` 物料 status→INACTIVE（非阻塞）+ SKU 侧 `isMaterialActive:375-385` 读侧 filter（resolveSku/findDefaultSku 返回 null）✅（强测 `testMaterialDeactivateCascadeGuard`）→ **接受**。
- ⑤ **存量单据保留对已停用 SKU 的引用（历史完整）**：`useLogicalDelete=true` + ORM to-one 不过滤 delVersion → 历史 skuId 引用仍可解析 ✅ → **接受**。

**#2 + #3 合并登记 P1-RC-062**（同 UC-MD-06 SKU 状态约束同根因——SKU 数据完整性保护双缺：无 status 列致语义不可达 + 引用检查 SPI 无生产实现致被引用 SKU 可删；同 §2 P1①+②；同 §4 三判据复核）。

**§4 三判据复核（UC-MD-06 候选 P1：SKU status+引用检查）**：
- (i) plan 含独立 plan-audit 通过记录：✗ 产生本功能的计划（`2026-07-07-0024-1`）无 RC 式独立 plan-audit 通过记录裁决 SKU status 列裁剪或引用检查 SPI 接线裁剪。
- (ii) owner doc 显式 documented simplification 标注且经人工批准：代码注记 `:289` G2 "SKU 当前实体无 status 列"+ `:374` "Phase 3 Decision 选 (b) 物料级 status 承载" 是 **AI 自标**；`sku-multi-unit.md §SKU 启停 :257-274` 字面声明"SKU 独立停用"为正向需求非 Non-Goal；`README.md §启用/停用 :126` "SKU 可独立于物料停用" + §关键业务规则 2 "主数据被业务单据引用后不可物理删除"是活跃设计契约承诺；`git log -- docs/design/master-data/ module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdMaterialSkuBizModel.java` 全部提交作者 = `canonical`（AI）无人工批准痕迹。按 methodology §4 line 168「AI 自标 ≠ 人工批准」，判据 (ii) **不成立**。
- (iii) product-scope 范围裁剪登记：✗ SKU 状态约束 + 引用完整性为 product-scope master-data 域列明核心能力（README.md §关键业务规则 1-2），未将"SKU 独立停用"或"引用检查"列入范围裁剪。
- **三判据均不成立 → 非 documented simplification → 按 Q4=(a) 强制实现 P1**。**引用检查 SPI 无生产实现是数据完整性风险**（被活跃采购/销售/库存单据引用的 SKU 可删除，会计/数据安全类强制实现无例外）→ 倾向 **P1-RC-062**。**须人工确认 product-scope 是否要求 SKU 独立停用**：若裁剪→§4(iii) 改真相源非降级；若未裁剪→P1 强制实现。**修复 SKU 独立 status 列触及 ORM 结构变更[ErpMdMaterialSku 加 status 列 + dict erp-md/active-status 复用]须 ask-first + 独立 plan-audit §5 ORM 类**；**引用检查 SPI 接线须与 purchase/sales/inventory 跨域引用协调（P1-MA1-022 successor 方向：跨域 daoFor 迁移到 I*Biz 时同步注入 IErpMdSkuReferenceChecker 实现）**。

#### UC-MD-07 主数据看板 → **接受 on KPI 聚合 + P2 on 阈值硬编码 + reuse P1-MA2-093 on 行级权限**

逐条：
- ① **KPI 卡片值 == 对应实体的实时聚合（按期间/orgId/权限过滤）**：`getDashboardKpi:47-67` 经 IDaoProvider + IOrmTemplate 实时 count 聚合（materialCount/customerCount/vendorCount/inactiveMaterialCount/inactivePartnerCount）✅（4 @Test 强 + E2E 强断言）→ **接受 on KPI 聚合部分**。
- ② **物料/往来单位总数 + 无 SKU 物料/无价格物料预警**：`findMaterialWithoutSkuAlert:70-98` + `findSkuWithoutPriceAlert:101-125` 逐行比对 + 精确命中 ✅（强测）→ **接受**。
- ③ **预警项 == 满足阈值条件的记录（阈值来自系统配置，非硬编码）**：`ALERT_MAX_ROWS=5000`（:40 `private static final int`）**硬编码非 config**——L1 operative 约束"非硬编码"未达（无 `erp-md.dashboard-*-threshold` config key，grep 全 `module-master-data` 确认零）→ 命中 **§2 P2①（次要验收标准未完全满足——主路径[预警返回真实数据]OK，边界[阈值硬上限 ALERT_MAX_ROWS 非 config 键]弱）**。详见 **P2-RC-056**（与 `P2-RC-009` mfg 看板同型，**不复用 P2-RC-009** 因不同域不同控制点，新建）。
- ④ **看板数据受行级权限约束（只看自己组织/部门/成本中心）**：`ErpMdDashboardBizModel` 经 IDaoProvider/IOrmTemplate 直访 + IServiceContext context 收到但未用于 scope + `new QueryBean()` 无 orgId filter → **BizModel 未应用行级权限**。**复用 P1-MA2-093**（A2.18 :99-101 显式列 11 dashboard 直访之一，**resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`**——实测 `module-common-service/.../ErpOrgIsolationQueryTransformer.java:34 implements IQueryTransformer` config-gated 已注入）。**按 §去重协议 reuse 不新建**——R1.29 已 resolved，运行时覆盖有效性 SP-1 交 MA4（与 A1.7 UC-FIN-17⑫ / A1.11 UC-MFG-11③ / A1.21 UC-SAL-12 / A1.24 UC-AST-12③ / A1.27 UC-INV-11⑫ / A1.33 UC-QA-12 行级权限复用先例一致）。

### 本切片 finding 汇总（2 新 P1 + 4 新 P2 + 1 reuse resolved P1）

| Finding | UC | 缺口摘要 | 分级 | §5 裁决依据 |
|---------|----|---------|------|------------|
| **P1-RC-062**（新） | UC-MD-06 ②③ | SKU 无 status 列致独立停用不可达 + IErpMdSkuReferenceChecker 无生产实现→被引用 SKU 生产可删 | P1 | §2 P1①+②；§4 三判据均不成立（AI 自标 G2 + SPI doc Deferred 无人工批准）；引用检查 SPI 缺失是数据完整性风险；**触及 ORM 结构变更[加 status 列]须 ask-first + 引用检查 SPI 接线须与 P1-MA1-022 successor 跨域协调** |
| **P1-RC-063**（新） | UC-MD-03 ④ | IErpMdSupplierPriceResolver 无生产实现→采购单据价格表层 no-op 恒落默认档 | P1 | §2 P1①；§4 三判据均不成立（SPI doc AI 自标"下游接线归 Deferred"无人工批准）；须人工确认 product-scope；纯 SPI 实现预授权不触 ask-first；**须与 purchase 域 A1.15-A1.17 协同** |
| **P2-RC-056**（新） | UC-MD-07 ③ | 主数据看板预警阈值硬编码（ALERT_MAX_ROWS=5000 非 config key） | P2 | §2 P2①；与 P2-RC-009 mfg 看板同型不同域不同控制点；纯 BizModel+config key 补充预授权不触 ask-first |
| **P2-RC-057**（新） | UC-MD-04 ①+孤儿值 | minPrice 派生 vs L1 命名字段分歧 + priceValidationLevel 默认"20"孤儿值违反字典契约 | P2 | §2 P2①；§4 三判据均不成立（AI 自标 G3/G5 无人工批准）；行为等价 + 主路径 OFF/WARN/HARD 强测；修复 minPrice 列触及 ORM ask-first，"20"→WARN 触及 ORM ask-first 或文档预授权 |
| **P2-RC-058**（新） | UC-MD-01 ③ | barcode DB 唯一索引缺失（应用层 enforceBarcodeUnique TOCTOU 窗口） | P2 | §2 P2①；§4 三判据均不成立（代码注记 G1 AI 自标无人工批准）；主路径应用层守卫强测；**触及 ORM 结构变更[加 UK_MD_MATERIAL_SKU_BARCODE]须 ask-first** |
| **P2-RC-059**（新） | UC-MD-05 ② | (materialId, isDefault=true) DB UK 缺失（多默认 SKU 物理可能） | P2 | §2 P2①；§4 三判据均不成立（无 G 编号 AI 自标无人工批准）；主路径 findDefaultSku 取第一条仍可用；**修复触及 ORM[部分 UK WHERE isDefault=true]须 ask-first 或纯应用层守卫预授权** |
| reuse **P1-MA2-093**（resolved R1.29） | UC-MD-07 ④ | 主数据看板 BizModel 经 IDaoProvider 直访绕鉴权管道（行级权限未在 BizModel 应用） | P1（已 resolved R1.29） | A2.18 :99-101 显式列 ErpMdDashboardBizModel 11 dashboard 之一，resolved R1.29 全局 ErpOrgIsolationQueryTransformer 注入；按 §去重协议 reuse 不新建；运行时覆盖 SP-1 交 MA4 |

---

## §6 与 arm-index 衔接（methodology §7"复用 or 新增"裁决）

> 每条 finding 产出前已 grep `arm-index.md` 同域（master-data/md）同控制点（sku/barcode/uom/price/minprice/default/status/reference/dashboard/supplier/sku-code）后裁决。

| Finding | 裁决 | 与既有 finding 的差异依据（grep 依据） |
|---------|------|--------------------------------------|
| **P1-RC-062**（UC-MD-06） | **新建** | grep arm-index master-data/md sku status/reference/SKU 状态/引用检查/被引用拒删：既有 `P1-MA1-022`（跨域只读 daoFor，resolved plan 2026-07-29-2225-1，不同维度=平台一致性 vs 需求符合性 SKU 状态约束）/ `P1-MA3-003`（master-data 文档转录 resolved R2.1，不同维度=文档质量）/ `P2-MA1-030`（ErpMdCurrencyBizModel LocalDate.now watch-only，不同控制点）/ `P2-MA5-007`（AMIS Non-Goal watch-only，不同控制点）。**RC 系列对 master-data md SKU status/reference 零命中**（master-data 域首个 RC 切片）→ **新建**。**SKU status 列缺失**与 **IErpMdSkuReferenceChecker 无生产实现**同 UC-MD-06 同根因（SKU 数据完整性保护双缺）合并登记。 |
| **P1-RC-063**（UC-MD-03） | **新建** | grep arm-index master-data/md supplier/价格表/supplierPriceResolver：零命中。**P1-RC-021**（A1.18 UC-SAL-11 ⑥ sales 促销应用层最低价缺失）是 sales 域不同控制点（sales 促销侧 applyPricingRules 未调 master-data min-price 守卫 vs 本 finding = master-data 暴露的 supplier SPI 无生产实现）→ 互补不重复（A1.18 报告 §5 显式声明"master-data 侧守卫归 UC-MD-04（A1.41 master-data 全功能切片），本 finding 覆盖 sales 促销应用层缺失"）。→ **新建**。 |
| **P2-RC-056**（UC-MD-07 ③） | **新建** | grep arm-index master-data/md dashboard/阈值/ALERT_MAX_ROWS/hardcoded threshold：零命中。**P2-RC-009**（A1.11 UC-MFG-11 ② mfg 看板预警阈值非 config）**同型不同域不同控制点**（mfg dashboard findDelayedWorkOrderAlert 状态/日期驱动 vs md dashboard ALERT_MAX_ROWS 服务端硬上限），按 §7 不复用→ **新建**（与 A1.7 UC-FIN-17⑪ finance 看板阈值 config 化 PASS / A1.21 UC-SAL-12 sales 看板阈值 config 化 PASS / A1.27 UC-INV-11 ⑩⑪⑫ inventory 看板阈值 config 化部分 PASS 形成跨域对比矩阵）。 |
| **P2-RC-057**（UC-MD-04） | **新建** | grep arm-index master-data/md minPrice/derive/priceValidation/孤儿值：零命中。**P1-RC-021**（A1.18 sales UC-SAL-11 最低价缺失）是 sales 侧不同控制点（sales applyPricingRules 不调 master-data 守卫 vs 本 finding = master-data 自身 minPrice 派生 + priceValidationLevel "20" 孤儿值）→ 互补不重复。→ **新建**。 |
| **P2-RC-058**（UC-MD-01） | **新建** | grep arm-index master-data/md barcode UK/unique index/TOCTOU/条码唯一索引：零命中。→ **新建**。 |
| **P2-RC-059**（UC-MD-05） | **新建** | grep arm-index master-data/md materialId+isDefault/默认 SKU UK/恰一默认：零命中。→ **新建**。 |
| reuse **P1-MA2-093**（UC-MD-07 ④） | **复用** | `P1-MA2-093`（A2.18 :99-101 orgId 查询隔离全仓未落地）显式列 ErpMdDashboardBizModel 为 11 dashboard 直访之一，**resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`**（实测 `module-common-service/.../ErpOrgIsolationQueryTransformer.java:34` 已注入 config-gated）。**既有 P1-MA2-093 行追加"RC A1.41/UC-MD-07 ④ 交叉引用"，不新建**（§去重协议）。与 A1.7 UC-FIN-17⑫ / A1.11 UC-MFG-11③ / A1.21 UC-SAL-12 / A1.24 UC-AST-12③ / A1.27 UC-INV-11⑫ / A1.33 UC-QA-12 行级权限复用先例一致（reuse R1.29 resolved）。 |

### 修复行预留（MR1 RC-R1.n 触发条件 + 触及区域标注）

- **P1-RC-062**：MR1 RC-R1.n 修复行须实现 ①SKU 独立 status 列（**触及 ORM 结构变更[ErpMdMaterialSku 加 status 列 + 复用 dict erp-md/active-status]须 ask-first + 独立 plan-audit §5 ORM 类**）+ ②validateSkuDeactivation 增 SKU 级 status 守卫[isDefault=true 且 sku.status=INACTIVE 且 hasOtherActiveSku=false → 拒绝]+ ③跨域 IErpMdSkuReferenceChecker 生产实现接入（**协调 purchase/sales/inventory 域：purchase ErpPurOrderLine/ErpPurReceiveLine/ErpPurInvoiceLine + sales ErpSalOrderLine/ErpSalDeliveryLine/ErpSalInvoiceLine + inventory ErpInvStockMoveLine/ErpInvStockBalance 等查询 skuId 引用**，与 P1-MA1-022 successor "daoFor → I*Biz 迁移"方向协同）+ ④defaultPrepareDelete/defaultPrepareUpdate[status→INACTIVE] 调 validateSkuDeactivation 守卫全路径。
- **P1-RC-063**：MR1 RC-R1.n 修复行须新增 `ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver`（**协调 purchase 域 A1.15-A1.17 主流程**：查 `ErpPurPriceList/ErpPurPriceListLine` by partnerId+materialId+effectiveDate + 返回命中最优价）+ module-purchase beans.xml 注册 + 期望经 `ErpMdMaterialSkuBizModel.supplierPriceResolver` @Inject @Nullable 自动注入。**纯 SPI 实现预授权不触 §5 ask-first**。
- **P2-RC-056**：MR1 RC-R1.n 修复行须 `ErpMdConstants` 增 `CONFIG_DASH_MD_ALERT_MAX_ROWS`（默认 5000）+ `ErpMdDashboardBizModel` 改读 `AppConfig.var`；**纯 BizModel 代码逻辑 + config key 补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**。
- **P2-RC-057**：MR1 RC-R1.n 修复行须方案 A[`ErpMdMaterialSku` ORM 加 `minPrice` 列 + `deriveMinPrice` 优先读 sku.minPrice 列再回退派生 + `orm.xml:344` `defaultValue="20"` 改为 `defaultValue="WARN"`，**触及 ORM 结构变更须 ask-first + 独立 plan-audit §5 ORM 类**]；方案 B[owner doc `sku-multi-unit.md §多档价格` + `§配置项` 补注「minPrice 当前为派生底线=min{四档正值}；priceValidationLevel 列默认值 '20' 历史遗留，实际语义=WARN（与字典 WARN 一致），建议种子数据迁移改 'WARN'」+ ORM defaultValue 修订，**纯文档/默认值修订预授权不触 ask-first**]。须人工裁决方案 A/B。
- **P2-RC-058**：MR1 RC-R1.n 修复行须 `ErpMdMaterialSku` ORM 加 `UK_MD_MATERIAL_SKU_BARCODE`（barcode 列；部分索引 WHERE barcode IS NOT NULL 视 dialect 支持，或全列 UK + 应用层允许 null）+ `enforceBarcodeUnique` 守卫保留为兜底；**触及 ORM 结构变更须 ask-first + 独立 plan-audit §5 ORM 类**。
- **P2-RC-059**：MR1 RC-R1.n 修复行须方案 A[`ErpMdMaterialSku` ORM 加部分 UK `(materialId) WHERE isDefault=true`（dialect 支持时）/ 或 DB 触发器 / 或全局 UK 借助虚拟列]；方案 B[纯应用层守卫 `defaultPrepareSave/Update` 加 (materialId, isDefault=true) 唯一性查询校验 + 多默认时抛 ERR]。方案 B **纯 BizModel 代码逻辑预授权不触 §5 ask-first**；方案 A **触及 ORM 结构变更须 ask-first**。倾向方案 B（应用层守卫成本更低）。
- **reuse P1-MA2-093**：修复随 R1.29 全局 `ErpOrgIsolationQueryTransformer` 已落地（resolved），运行时覆盖有效性交 MA4 A4.1 运行时展开。

### 协调声明

- **UC-MD-03 supplier SPI 接线须与 purchase 域 A1.15-A1.17 协同**：purchase 切片审消费域对 master-data 原语的接线（非本切片范围），本切片登记 master-data 暴露的 SPI 无生产实现，修复时须 purchase 域新增 ErpPurSupplierPriceResolver。
- **UC-MD-06 引用检查 SPI 接线须与 purchase/sales/inventory 跨域引用协调**（P1-MA1-022 successor 方向）：跨域 daoFor 迁移到 I*Biz 时同步注入 IErpMdSkuReferenceChecker 生产实现，覆盖业务单据行→skuId 的引用查询。
- **UC-MD-07 行级权限修复随 P1-MA2-093/R1.29 全局 transformer 方向**：R1.29 已 resolved，本切片 reuse 不重开；运行时覆盖 SP-1 交 MA4。
- **ORM 结构类修复（barcode DB 唯一索引 / minPrice 列 / (materialId,isDefault) DB 约束 / SKU status 列）须 ask-first + 独立 plan-audit §5 ORM 类**（roadmap 预授权声明明确排除 ORM 结构变更）。

---

## §7 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。无运行时探针（本计划纯静态）。

- **SP-1（UC-MD-07 ④）**：全局 `ErpOrgIsolationQueryTransformer`（R1.29 resolved P1-MA2-093）是否覆盖 `ErpMdDashboardBizModel` 的 IDaoProvider/IOrmTemplate 直访路径——`ErpMdDashboardBizModel.getDashboardKpi:47-67` 经 `materialDao.countByQuery(new QueryBean())` + `partnerDao.countByQuery(eqQuery("partnerType", ...))`，**`new QueryBean()` 无 orgId filter**；R1.29 transformer 注入是否在 BizModel 调用栈自动追加 `eq("orgId", currentOrgId)` 需运行时确认（`ErpOrgIsolationQueryTransformer.java:34` config-gated 默认状态需 MA4 实测）。与 A1.7 SP-4 / A1.11 SP-3 / A1.21 SP / A1.24 SP-4 / A1.27 SP / A1.33 SP 同根因（dashboard 直访路径覆盖）。
- **SP-2（UC-MD-03 ④）**：`IErpMdSupplierPriceResolver` 在 purchase 域是否有未被 grep 发现的接线——grep `implements IErpMdSupplierPriceResolver` 全生产代码 0 命中，但 purchase 域可能经其他机制（如 nop-dyn 动态 bean / Spring `@Component` + 配置类生成 / delta 层 beans.xml）注入。需 MA4 运行时确认 `ErpMdMaterialSkuBizModel.supplierPriceResolver` 实际注入值（null or instance）。
- **SP-3（UC-MD-01 ③）**：`enforceBarcodeUnique:255-271` 在并发 save 的 TOCTOU 实际窗口（G1）——单测 `testBarcodeDuplicateRejected:182-214` 单线程顺序执行不触发；并发场景下两请求均通过 `findFirst` 查无 → 双双 `saveEntity` → DB 无 UK 兜底 → 唯一性失效。需 MA4 实测并发 save 同 barcode 的实际数据库状态。
- **SP-4（UC-MD-04 ①）**：`priceValidationLevel="20"` 种子分类实际 WARN 语义影响面（G5）——种子数据 `ErpMdMaterialCategory` 是否实际含 `priceValidationLevel="20"` 字段值（defaultValue 触发条件下），以及生产环境新创建分类的默认值。需 MA4 复核种子数据 + 运行时新建分类的实际 WARN 派遣路径。
- **SP-5（UC-MD-06 ②）**：`IErpMdSkuReferenceChecker` 生产缺失下被引用 SKU 删除的实际数据完整性事件——尽管 `useLogicalDelete=true` 软删保留物理行（历史单据 skuId 引用 ORM to-one 仍可解析），但**软删后 SKU 在 operational 查询中消失**（默认 delVersion=0 过滤）+ **硬删路径若启用则物理破坏引用**。需 MA4 实测：被活跃 AP/AR 发票引用的 SKU 经 GraphQL `ErpMdMaterialSku__delete` 是否实际成功（生产环境 skuReferenceChecker=null → validateSkuDeactivation 仅默认 SKU 守卫触发 → 被引用 SKU 删除成功 → 数据完整性事件）。

### P0 即时通道评估

UC-MD-06 引用检查缺失致被引用 SKU 可删——倾向**数据完整性 P0 候选评估**：

- **DB 层 FK/CASCADE 兜底核验**：purchase/sales/inventory →line ORM 虽 `to-one refEntityName="app.erp.md.dao.entity.ErpMdMaterialSku"`（sales.orm.xml:424/576/972/1085/1150 + inv.orm.xml:251/320/397/511/567/678/773/871 + pur 类似），**但 Nop 平台 to-one 是逻辑/ORM 层虚拟 join（非 DB 物理 FK），无 CASCADE 兜底**——`skuId` 列无 mandatory + 无 DB FK 约束 + 无触发器 → 物理层无防护。
- **应用层兜底核验**：`useLogicalDelete=true`（orm.xml:374）默认走软删（设 delVersion），物理行保留 → 历史单据 skuId ORM to-one 解析仍可达（默认不过滤 delVersion）→ **历史数据物理完整**。但**软删后 SKU 在 operational 查询消失** + **硬删路径若启用（如管理员工具直接 SQL）则物理破坏引用**。
- **运行时实际阻断面**：`ErpMdMaterialSkuBizModel.defaultPrepareDelete:245-249` 调 `validateSkuDeactivation` → 默认 SKU 守卫触发（若 sku.isDefault=true 且无其他 SKU → 拒绝）；**引用检查 SPI 无生产实现 → 生产环境 skuReferenceChecker=null → :220 条件永假 → 被引用非默认 SKU 删除无任何应用层守卫**。
- **结论**：**降 P1 不升 P0**——(a) `useLogicalDelete=true` 默认软删路径保留物理行，历史单据 ORM to-one 仍可解析（与 L1 "存量单据保留对已停用 SKU 的引用（历史完整）"语义一致）；(b) 不破坏活跃数据物理完整性（无 GL/库存/余额数值破坏）+ 非会计过账破坏 + 非核心循环断裂；(c) 运营层面 SKU 软删后操作可见性丢失属"应做未做"运营风险非"活跃数据破坏"（§2 P0①）；(d) **倾向 P1**（§2 P1②——异常路径未实现：被活跃单据引用的 SKU 应拒绝删除[非默认 SKU 路径]但生产零守卫）。**MR0 不触发**，登记 P1-RC-062 入 MR1 批量修复通道，但**优先级建议高**（数据完整性风险，会计/数据安全类强制实现无例外）。

---

## §8 过程纪律自检（methodology §8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（本次实测退出码 = 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计，无生产代码变更，checker 无回归风险**（actual 偏移为审计前既有状态，非本审计引入）。

  | 规则 | 描述 | baseline 命中 | actual 实测（本审计时） | 评估 |
  |------|------|--------------|------------------------|------|
  | R1a/b/c | dao() 直接调用 BizModel（save/update/getEntityById） | 0/0/0 | 0/0/0 | = baseline |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | = baseline |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | = baseline |
  | R2b | BizModel daoFor(Erp*) 跨域 | 240（基线表）/ 后续注记已下降 | 229 | ≤（审计前既有下降趋势） |
  | R2c | 全生产代码 daoFor() 总量 | 1380 | 1382 | +2（审计前既有，非本审计引入；本审计零代码变更） |
  | R2d | Processor daoFor(ErpMd*) | 32 | 34 | +2（审计前既有，非本审计引入） |
  | R3 | new Erp*() 构造实体 | 5 | （规则运行未超基线） | ≤ |
  | R5 | @Inject private | 0 | 0 | = baseline |

  > 声明：上表 actual 为本审计执行时实测快照；R2b/R2c/R2d 的 +N 偏移是审计前既有仓库状态（compliance-baseline.md 多版注记显示 R2 系列经多轮重构持续下降，基线表与 inline 注记存在口径差异），**本审计零生产代码变更，不引入任何新命中**，故无回归风险。CI 门控由 `.github/workflows/compliance.yml` 强制；如 CI 因既有偏移 fail，须由独立基线裁决计划处理，与本审计无关。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-062/063 + P2-RC-056/057/058/059 + reuse P1-MA2-093）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（见 §6 表），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**：本审计未修改 `product-scope.md` / `use-cases.md` / owner doc 需求契约段落 / `model/*.orm.xml` / 代码（§9 冻结条款遵守）。分歧记入本报告 §5/§6。

---

## §9 与 MA2 报告差异增量声明（methodology §6 段落 9 / §去重协议）

（与 §0 一致，此处列明只补的需求视角差异）

- **无 master-data 专属 MA2 报告**：A2.1-A2.18 + A2.5-A2.15 共 18 份 MA2 报告均不含 master-data SKU/价格/条码/UoM/状态约束/看板行为证据（master-data 是基础数据底座非业务流转域）→ **无可复用行为证据**。
- **跨域 daoFor 复用 P1-MA1-022 现状证据**：master-data 是 DAG 根域最被引用（~100+ 文件经 daoFor(ErpMd*) 只读访问），读侧统一裁决在 `data-dependency-matrix.md §9`（resolved plan `2026-07-29-2225-1`），本切片**不重审此维度**（不同维度：需求符合性 vs 平台一致性），只引作现状证据。
- **既有 master-data 非 UC-MD finding 复用注记**：P1-MA3-003 / P2-MA1-030 / P2-MA5-007 均非 UC-MD 维度，按 §去重协议本切片**不复审**。
- **本切片从需求契约视角只补的差异增量**（与既有 MA2/A2 行为证据互补不复重）：
  - **UC-MD-01 ③ barcode DB UK 缺失**（TOCTOU 窗口）→ P2-RC-058。
  - **UC-MD-03 ④ supplier SPI 无生产实现**（采购恒落默认档）→ P1-RC-063。
  - **UC-MD-04 ① minPrice 派生 + "20" 孤儿值**（行为等价但字段分歧）→ P2-RC-057。
  - **UC-MD-05 ② (materialId,isDefault) DB UK 缺失**（多默认 SKU 物理可能）→ P2-RC-059。
  - **UC-MD-06 ②③ SKU status+引用检查双缺**（独立停用不可达 + 被引用 SKU 可删）→ P1-RC-062。
  - **UC-MD-07 ③ 阈值硬编码**（ALERT_MAX_ROWS=5000 非 config）→ P2-RC-056；**④ 行级权限 reuse P1-MA2-093**（resolved R1.29 + SP-1 交 MA4）。

---

## §自检（报告 9 段完整性，methodology §6 段落完整性自检）

- [x] §0 与 MA2 报告差异增量声明（声明段，对应段落 9 前置）
- [x] §1 需求契约原文（L1 逐字引用 UC-MD-01~07）
- [x] §2 实现证据（L3 代码路径 + 行为断言 + SPI 解耦模式）
- [x] §3 测试证据（L4 注明断言强度）
- [x] §4 运行时行为证据（L5 + 无专属 MA2 + P1-MA1-022 跨域 daoFor 复用）
- [x] §5 符合性结论（五级矩阵 + 每 UC 结论 + §2 判据 + §4 三判据复核）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 修复行预留 + 协调声明）
- [x] §7 静态存疑点清单（SP-1~SP-5 + P0 评估）
- [x] §8 过程纪律自检（checker actual vs baseline 表 + 独立性 + 去重 + 真相源未修改）
- [x] §9 与 MA2 报告差异增量声明

**9 段齐全**（§0 为 §9 的前置声明段，§1-§9 完整）。本报告可交 closure audit。

