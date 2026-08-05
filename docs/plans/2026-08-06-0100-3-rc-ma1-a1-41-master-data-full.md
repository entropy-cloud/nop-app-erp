# 2026-08-06-0100-3 rc-ma1-a1-41-master-data-full 主数据域全功能需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A1.41（MA1 需求追踪矩阵审计 — master-data 全功能：扫码开单 findSkuByBarcode / 多单位换算 convertQty / 价格优先级 resolvePrice / 最低价校验 validatePrice / 默认 SKU resolveSku / SKU 状态约束 / 主数据看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.41
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.41 的 0.2 依赖）、`2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`+`2026-08-06-0100-2-rc-ma1-a1-40-cs-f4-survey-entitlement-catalog-fulfillment.md`（同批 N=1/N=2 cs 域；本切片 N=3 为 master-data 域**首个** RC 切片；本批次续编自 N=2 之后的最新 RC 编号）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.41 给出 UC 清单 = `UC-MD-01~07`（7 UC），覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 **master-data 域首个 RC 审计切片**（arm-index 无任何 UC-MD-* 需求符合性 finding）。

- **L1 需求契约（权威真相源）**：`docs/design/master-data/use-cases.md`（机制细节引用 `sku-multi-unit.md`，L2 设计参考）：
  - **UC-MD-01 扫码开单**（`:8`）：扫描条码 → findSkuByBarcode(barcode) → SKU + 物料；自动填充物料/SKU/单位(=SKU.单位)/数量/价格(按价格优先级解析)；**条码全局唯一（违反 idx_barcode unique → 拒绝）**。
  - **UC-MD-02 多单位换算落账**（`:23`）：录入单位=箱/数量=10；换算 baseQty = 数量 × conversionFactor(箱→瓶)；**落账：业务单据行.baseQty == 10 × 系数（用于库存/成本计算）**；单位组(UoMGroup)内换算系数一致。
  - **UC-MD-03 价格优先级解析**（`:39`）：优先级 **手工价 > 价格表 > SKU 默认档(purchasePrice/wholesalePrice/retailPrice)**；手工填单据行.单价 → 用手工价；否则查价格表（客户专属/促销）→ 命中则用；否则用 SKU 默认档（按单据类型选 purchase/wholesale/retail）。
  - **UC-MD-04 最低价校验拦截**（`:55`）：若最终售价 < SKU.minPrice：**MaterialCategory.priceValidationLevel == HARD → 拒绝 / == WARN → 警告但放行 / 配置 == OFF → 不校验**。
  - **UC-MD-05 默认 SKU 兜底**（`:71`）：单据行未指定 SKU → resolveSku(物料) → 取 defaultFlag=true 的 SKU；**每物料必有且仅有一个默认 SKU（约束）**；若无默认 SKU 且配置 sku-default-required → 报错。
  - **UC-MD-06 SKU 状态约束**（`:86`）：**停用唯一默认 SKU → 拒绝（必须先设其他默认）**；**SKU 被业务单据引用 → 拒绝删除（只能停用）**；物料停用 → 联动所有 SKU 不可被新单引用；存量单据保留对已停用 SKU 的引用（历史完整）。
  - **UC-MD-07 主数据看板**（`:106`）：KPI 卡片值 == 实时聚合（按期间/orgId/权限过滤）：物料/往来单位总数，无 SKU 物料/无价格物料预警；预警项 == 满足阈值条件的记录（**阈值来自系统配置，非硬编码**）；**看板数据受行级权限约束（只看自己组织/部门/成本中心）**。

- **L3 代码实现现状（实测，`module-master-data/erp-md-service` + `erp-md-dao`）**——**全域核心完整+强测，多处 SPI 无生产实现/DB 约束缺失/派生字段偏离 L1 命名（candidate P1/P2，须 §4 三判据区分"经批准的 Deferred" vs "静默降级"）**：
  - **UC-MD-01 扫码（✅ 查找完整，⚠️ 唯一性仅应用层无 DB 唯一索引[代码注记 Deferred G1]）**：`findSkuByBarcode` @BizQuery `ErpMdMaterialSkuBizModel.java:76-85`（filter eq barcode + findFirst）；唯一性经 `defaultPrepareSave/Update` → `enforceBarcodeUnique:255-271`（gated `CONFIG_SKU_BARCODE_UNIQUE` `ErpMdConstants:20` 默认 true，抛 `ERR_SKU_BARCODE_DUPLICATE`）。**ORM `app-erp-master-data.orm.xml:408-418` 仅非唯一 `IDX_MD_MATERIAL_SKU_*`，无 DB 唯一索引**（代码注记 :50/:253 "Deferred DB unique index G1"，TOCTOU 窗口）。⚠️ 自动填充（单位/数量/价格）编排属消费域（purchase/sales 行），master-data 仅暴露查找原语。
  - **UC-MD-02 多单位换算（✅ 引擎完整，⚠️ 无 UoMGroup 实体[设计分歧但行为等价]，落账属消费域）**：`convertQty` @BizQuery `ErpMdUoMConversionBizModel.java:46-75`（优先级：物料级 ErpMdUoMConversion[materialId 非空, fromUoMId→toUoMId exact] → 通用[materialId null] → strict=false 兜底 SKU.conversionRate；strict=true 抛 `ERR_UOM_CONVERSION_NOT_FOUND`；BigDecimal scale=4 HALF_UP；同单位返回原值）。config `erp-md.uom-conversion-strict`（`ErpMdConstants:24` 默认 true）。**无 UoMGroup 实体**——一致性经显式 per-pair 行 + SKU 兜底保证（非组约束，行为等价）。⚠️ baseQty 落账属消费域（purchase/sales/inventory 行实体），master-data 仅提供 convertQty 原语。
  - **UC-MD-03 价格优先级（✅ 三级链完整，⚠️ 供应商价格表 SPI 无生产实现→采购单据恒落默认档）**：`resolvePrice`/`resolvePriceWithSource` @BizQuery `ErpMdMaterialSkuBizModel.java:128-176`（manualPrice 即返 → customerPriceResolver SPI → supplierPriceResolver SPI → SKU 默认档 `pickDefaultTierPrice:311-325`：PURCHASE→purchasePrice/WHOLESALE→wholesalePrice/RETAIL→retailPrice/else salePrice）。**`IErpMdCustomerPriceResolver` 有生产实现** `module-sales/.../ErpSalCustomerPriceResolver.java`；**`IErpMdSupplierPriceResolver` 无生产实现**（仅测试桩 TestStubSupplierPriceResolver，grep `implements IErpMdSupplierPriceResolver` 生产零命中）→ 采购单据价格表层 no-op 恒落默认档（SPI doc :13-16 "默认无实现时返回 null…下游接线归 Deferred"）。⚠️ 交叉引用：`P1-RC-021`（A1.18 sales）sales 侧 applyPricingRules 未调 master-data min-price 守卫——sales 侧缺口，与 UC-MD-04 互补。
  - **UC-MD-04 最低价校验（✅ OFF/WARN/HARD 派遣完整，⚠️ 无 minPrice 列[派生][G3] + priceValidationLevel 默认"20"孤儿值[G5]）**：`validatePrice`/`resolvePriceValidationLevel:352-369`/`deriveMinPrice:331-346` @BizQuery `ErpMdMaterialSkuBizModel.java:178-203`（读 MaterialCategory.priceValidationLevel；OFF→pass；else 算 minPrice；finalPrice<minPrice：HARD→抛 `ERR_PRICE_BELOW_MIN`，WARN→warning=true）。**ORM 无 minPrice 列**（grep 零）——派生 minPrice = min{purchase,wholesale,retail,sale 正值}（代码注记 :328-330 "Explore 裁定选项 b：派生底线"）；**L1 命名 `SKU.minPrice` 为独立底线字段**——派生行为等价但**与 L1 命名字段分歧**（4 档全设价的 SKU 无法有低于最廉档的 minPrice）。⚠️ `MaterialCategory.priceValidationLevel` ORM `defaultValue="20"`（orm.xml:344）是**孤儿非字典值**——dict `price-validation.dict.yaml` 仅 OFF/WARN/HARD；代码把未知值（含"20"）当 WARN（:367-368），新种子分类默认"20"静默得 WARN 语义（G5 代码注记 :349-350）。
  - **UC-MD-05 默认 SKU（✅ resolveSku/findDefaultSku 完整，⚠️ 无 (materialId,isDefault) DB 唯一约束 + auto-create-default config 声明未实现）**：`findDefaultSku`/`resolveSku` @BizQuery `ErpMdMaterialSkuBizModel.java:89-124`（物料 active 校验 `isMaterialActive:375-385` 读 ErpMdMaterial.status → unitId 匹配 → 兜底 isDefault=true SKU → 无且 `sku-default-required` 抛 `ERR_SKU_DEFAULT_REQUIRED`）。config `erp-md.sku-default-required`（`ErpMdConstants:18` 默认 true）。**ORM 无 (materialId,isDefault=true) DB 唯一约束**——"每物料恰一默认 SKU"L1 约束**仅应用约定，非 DB 强制**（多默认 SKU 物理可能）；`sku-auto-create-default` config 键声明（`ErpMdConstants:22`）但**自动创建逻辑未实现**（注记 :21 "本计划仅声明配置键"）。
  - **UC-MD-06 SKU 状态约束（⚠️ 默认 SKU 守卫+物料停用读侧联动完整，❌ SKU 无独立 status 列[G2] + 引用检查 SPI 无生产实现→被引用 SKU 生产可删）**：`validateSkuDeactivation` @BizQuery + `defaultPrepareDelete:245-249` `ErpMdMaterialSkuBizModel.java:207-249`（守卫 1：isDefault=true 且无其他 active SKU → 抛 `ERR_CANNOT_DEACTIVATE_DEFAULT_SKU`；守卫 2：skuReferenceChecker!=null && isReferencedByBill → 抛 `ERR_SKU_REFERENCED_BY_BILL`）；物料停用级联 `ErpMdMaterialBizModel.java:63-81`（状态→INACTIVE **非阻塞**，级联经 resolveSku/findDefaultSku 读侧 filter `isMaterialActive`）。**SKU 无独立 status 列**（ORM 仅 isDefault 布尔，无 status）→ L1/设计 `sku-multi-unit.md:266-268`"SKU 独立停用"**不可达**（`hasOtherActiveSku:292-305` 把 active 当同物料不同 id，无 status 过滤）。**`IErpMdSkuReferenceChecker` 无生产实现**（grep 仅测试桩 TestStubSkuReferenceChecker）→ **生产环境被活跃采购/销售/库存单据引用的 SKU 仍可删除**（仅默认 SKU 守卫触发）——UC-MD-06 最显著功能缺口。
  - **UC-MD-07 看板（⚠️ KPI 实时聚合+预警完整，❌ 预警阈值硬编码 ALERT_MAX_ROWS=5000 非配置 + 行级权限未在 BizModel 应用[疑似复用 P1-MA2-093]）**：`ErpMdDashboardBizModel`——`getDashboardKpi:47-67`（实时聚合 materialDao.count + 按 partnerType 计数 + inactive 计数，经 IDaoProvider + IOrmTemplate 内存聚合）；`findMaterialWithoutSkuAlert:70-98`/`findSkuWithoutPriceAlert:101-125`（全表扫描，**硬编码 `ALERT_MAX_ROWS=5000`** `:40` 上限）。**L1"阈值来自系统配置非硬编码"未达**（alerts 返回至硬上限的全部匹配，无 `erp-md.dashboard-*-threshold` config 键，grep 确认；与 `P2-RC-009` mfg 看板同型）。**L1"看板受行级权限约束"未达**——BizModel 经 IDaoProvider/IOrmTemplate **直接访问无 orgId/权限过滤**（IServiceContext context 收到但未用于 scope，`new QueryBean()` 无 org filter）；与 `P1-MA2-093`（看板直接访问绕鉴权管线，A2.18 :99-101；R1.29 全局 `ErpOrgIsolationQueryTransformer` 部分域解决）同型——**疑似复用 P1-MA2-093**（待核全局 transformer 是否覆盖 ErpMdDashboard 查询）。
  - **跨域 daoFor**：master-data 是**最被引用的基础域**——`ErpMd*` 实体被 purchase/sales/inventory/finance/assets/mfg/mnt/prj/qa/drp/aps 经 daoFor 广泛只读访问（~100+ 文件），属 **P1-MA1-022**（resolved plan 2026-07-29-2225-1：读侧统一裁决在 `data-dependency-matrix.md §9`，md 目标域子集 = 可迁移到 I*Biz，successor 已命名）。`ErpMdMaterialSkuBizModel` 自身同域 daoFor(ErpMdMaterial/ErpMdMaterialCategory) 合法。**本 RC 切片不重审跨域 daoFor**（不同维度：需求符合性 vs 平台一致性），但 SPI 解耦模式（IErpMdSkuReferenceChecker/IErpMdSupplierPriceResolver/IErpMdCustomerPriceResolver）是**处方方向**——且 3 SPI 中 2 个缺生产接线（见 UC-MD-03/06 缺口）。

- **L4 测试证据现状**（`module-master-data/*/src/test` + `tests/e2e/`）：
  - UC-MD-01：`TestErpMdSkuServices.java#testFindSkuByBarcode:58-78`（**强**：命中/未命中/id）/ `testBarcodeDuplicateRejected:182-214`（**强**：ERR_SKU_BARCODE_DUPLICATE 经 GraphQL save 钩子）。
  - UC-MD-02：`TestErpMdSkuServices.java:129-178`——`testConvertQtyMaterialLevel`（10×24=240）/`testConvertQtyGenericFallback`（2×576=1152）/`testConvertQtyStrictNotFound`（错误码）/`testConvertQtySameUnit`（7→7）（均**强**）。
  - UC-MD-03：`TestErpMdSkuPriceValidation.java:60-99`——`testResolvePriceManualWins`/`testResolvePriceFromSupplierList`（SPI 7.77 击败默认）/`testResolvePriceDefaultTier`（4 单据类型 × 精确档位 scale=4）（均**强**，SPI 经 TestStubSupplierPriceResolver）。
  - UC-MD-04：`TestErpMdSkuPriceValidation.java:103-166`——`testValidatePriceHardReject`/`testValidatePriceWarnAllows`/`testValidatePriceOff`/`testValidatePriceAboveMinNoWarning`（均**强**）。
  - UC-MD-05：`TestErpMdSkuServices.java:82-125`——`testFindDefaultSku`/`testResolveSkuByUnit`/`testResolveSkuNoDefaultRequired`（均**强**）。
  - UC-MD-06：`TestErpMdSkuStatusConstraints.java:60-141`——`testCannotDeactivateOnlyDefaultSku`/`testCanDeactivateNonDefaultSku`/`testMaterialDeactivateCascadeGuard`/`testDeleteReferencedSkuRejected`（SPI 桩）/`testDeleteUnreferencedSkuOk`（均**强**）。
  - UC-MD-07：`TestErpMdDashboard.java:41-100`——`testKpiEmptyDatasetReturnsZeros`/`testKpiCountsByPartnerTypeAndStatus`/`testMaterialWithoutSkuAlertTriggersAndNot`/`testSkuWithoutPriceAlertTriggersAndNot`（**强**：精确计数）；E2E `tests/e2e/dashboards/master-data.value.spec.ts`（KPI 值断言 materialCount:4/customerCount:2/vendorCount:2 + 预警空集，**强**）。
  - **缺口**：UC-MD-03 供应商价格表生产实现零测试（仅测试桩）/ UC-MD-06 引用检查生产实现零测试 / UC-MD-07 阈值配置化+行级权限零测试。

- **L5 既有证据（MA2 复用输入）**：
  - **无 master-data 专属 MA2 状态机报告**。3 份 MA2 e2e 报告（`2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`/`procure-to-pay-e2e.md`/`period-close-e2e.md`）**含零 master-data SKU/价格/条码/UoM 行为证据**——唯一 master-data 引用 = `P2-MA2-013`（O2C 报告:172 注 `SettlementAllocation` 类*位置*在 master-data 模块，结算维度，与 UC-MD-01..07 无关）。**master-data 任何行为（SKU 解析/价格优先级/条码/UoM 换算/最低价/状态约束/看板）均无 MA2 证据**——本切片为这些行为的首份证据。
  - **master-data 相关既有 finding**：`P1-MA3-003`（master-data 文档=plan 执行记录/字段表转录，resolved R2.1）、`P2-MA1-030`（ErpMdCurrencyBizModel:60 `LocalDate.now()` watch-only）、`P2-MA5-007`（master-data.write.amis Non-Goal 长期悬挂 watch-only）、**P1-MA1-022 族**（P1-MA4-003/006/008/012/015/022：master-data 是跨域 `daoFor(ErpMd*)` 只读访问**最被引用的目标域**）。**无任何 UC-MD-* 需求符合性 finding**（本切片为 master-data 域首个 RC 切片）。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：无 master-data 专属 MA2 报告，无可复用行为证据；P1-MA1-022 引作跨域 daoFor 现状证据（不重审平台一致性维度），只补需求视角差异。

- **arm-index 既有 finding 衔接**：grep arm-index master-data/md/sku/barcode/uom/price-list/priceValidation/minPrice/defaultFlag/resolveSku/material-category → **无 UC-MD-* finding**。非 UC 的 master-data tag 既有 finding 见上（P1-MA3-003/P2-MA1-030/P2-MA5-007/P1-MA1-022 族）。本切片新 finding 续全仓 RC 序列（执行时 grep arm-index 取 N=2 之后的最新续编号）。本切片须 grep arm-index master-data sku/barcode/uom/price/minprice/default/status/reference/dashboard 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10。本切片候选偏差多为**代码逻辑**类（预授权——SPI 生产接线/看板阈值配置化/行级权限）；**多处触及 ORM 结构**（UC-MD-01 barcode DB 唯一索引 / UC-MD-04 minPrice 列 / UC-MD-05 (materialId,isDefault) DB 唯一约束 / UC-MD-06 SKU status 列）→ **ORM 结构变更须 ask-first + 独立 plan-audit**（roadmap 预授权声明明确排除 ORM 结构变更）；须在报告逐项标注触及保护区域。

- **剩余差距**：A1.41 切片五级追踪审计报告缺失 = MA4 及 MR1 该切片证据缺口来源。本计划产出 A1.41 报告并登记 finding，解除 master-data 域全功能证据缺口。

## Goals

- 产出 A1.41 切片审计报告 `docs/audits/2026-08-06-0100-3-rc-ma1-a1-41-master-data-full.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-MD-01~07 逐条核验**每条验收标准**（完整枚举，§3）：UC-MD-01 条码查找/唯一拒绝；UC-MD-02 换算引擎/落账；UC-MD-03 三级价格优先级（手工>价格表>默认）；UC-MD-04 OFF/WARN/HARD/最低价拦截；UC-MD-05 resolveSku/默认 SKU 兜底/必报错；UC-MD-06 默认 SKU 停用拒/被引用拒删/物料停用级联/历史完整；UC-MD-07 KPI 实时聚合/预警阈值配置/行级权限 全链逐条。
- 对候选缺口给出分级结论：①UC-MD-06 **SKU 无 status 列致独立停用不可达 + 引用检查 SPI 无生产实现→被引用 SKU 生产可删**倾向 **P1**（**§4 三判据关键裁决**——L1/设计 `sku-multi-unit.md:266-268` 明确要求"SKU 独立停用"+"被引用拒删"，须核 owner doc 是否显式 Deferred 且经人工批准；**引用检查 SPI 无生产实现**是数据完整性风险，会计/数据安全类强制实现无例外）；②UC-MD-03 **供应商价格表 SPI 无生产实现→采购单据恒落默认档**倾向 **P1/P2**（L1 三级链明确要求"价格表"层；SPI doc 自标"下游接线归 Deferred"须 §4 三判据核人工批准痕迹）；③UC-MD-07 **预警阈值硬编码 + 行级权限未应用**倾向 **P1/P2**（L1 明确"阈值配置非硬编码"+"行级权限"；行级权限疑似复用 P1-MA2-093，阈值硬编码与 P2-RC-009 同型）；④UC-MD-04 **minPrice 派生 vs L1 命名字段分歧 + priceValidationLevel 默认"20"孤儿值**倾向 **P2**（行为等价但字段分歧 + 种子默认值语义漂移）；⑤UC-MD-01 barcode DB 唯一索引 / UC-MD-05 (materialId,isDefault) DB 唯一约束 倾向 **P2**（代码注记 Deferred G1/G2，须 §4 三判据核经批准）；⑥UC-MD-02 全 + UC-MD-01 查找 + UC-MD-03 customer 价格表层 + UC-MD-04 OFF/WARN/HARD + UC-MD-05 resolveSku 核心 + UC-MD-06 默认 SKU 守卫 + UC-MD-07 KPI 聚合 → 倾向**接受**（强测）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编，执行时取最新）并按 §10 触发 MR1（本计划仅登记，不实施修复；**ORM 结构类须 ask-first**）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/sku-multi-unit.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不重审 P1-MA1-022 跨域 daoFor**（§去重协议：平台一致性维度以 audit-remediation 收口；本切片只补需求符合性维度，引用其读侧统一裁决现状）。
- **不重审 P1-MA3-003 / P2-MA1-030 / P2-MA5-007**（非 UC-MD 维度，复用不复审）。
- **不审计消费域对 master-data 原语的接线**（UC-MD-01 自动填充/UC-MD-02 baseQty 落账属 purchase/sales/inventory 切片 A1.15-A1.27 范围；本切片仅审 master-data 暴露的原语是否符合 L1）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.41 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.41 UC 锚点）+ `docs/design/master-data/use-cases.md`（L1 真相源）+ `docs/design/master-data/sku-multi-unit.md`+`README.md`（L2 设计参考，非真相源——Deferred/Non-Goal 标注须 §4 三判据复核）+ `docs/audits/arm-index.md`（finding 衔接）+ `docs/architecture/data-dependency-matrix.md §9`（P1-MA1-022 读侧裁决现状）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-master-data/erp-md-service -Dtest=TestErpMdSkuServices,TestErpMdSkuPriceValidation,TestErpMdSkuStatusConstraints,TestErpMdDashboard`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/2026-08-06-0100-3-rc-ma1-a1-41-master-data-full.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-MD-01~07 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:8/23/39/55/71/86/106` 验收标准原文；L2 引用 `sku-multi-unit.md`（§多 barcode/§多单位换算/§多档价格/§默认 SKU/§SKU 状态管理）+ `README.md`（标注"设计参考，冲突以 L1 为准"——Deferred/G1/G2/G3/G5 注记须 §4 三判据复核）；L3 引用 `ErpMdMaterialSkuBizModel`#findSkuByBarcode/resolvePrice/validatePrice/resolveSku/validateSkuDeactivation + `ErpMdUoMConversionBizModel`#convertQty + `ErpMdMaterialBizModel`#deactivate + `ErpMdDashboardBizModel` + `ErpMdConstants`/`ErpMdConfig` + `ErpMdMaterial`/`ErpMdMaterialSku`/`ErpMdUoMConversion`/`ErpMdMaterialCategory` ORM（含行号）；L4 引用 `TestErpMdSkuServices`/`TestErpMdSkuPriceValidation`/`TestErpMdSkuStatusConstraints`/`TestErpMdDashboard`#method + E2E `master-data.value.spec.ts`（注明断言强度）；L5 标注无 master-data 专属 MA2 报告（首份证据）+ P1-MA1-022 跨域 daoFor 现状复用。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条对照）：UC-MD-01 findSkuByBarcode（✅）+ **唯一性**（应用层 enforceBarcodeUnique ✅ / **DB 唯一索引 ❌[G1 Deferred]**）；UC-MD-02 convertQty 引擎（✅）+ **UoMGroup 一致性**（无实体，per-pair 行 ⚠️ 行为等价）+ baseQty 落账（属消费域，原语 ✅）；UC-MD-03 manual>priceList>default（✅）+ **customer 价格表层**（ErpSalCustomerPriceResolver 生产实现 ✅）+ **supplier 价格表层**（IErpMdSupplierPriceResolver **无生产实现**❌→采购恒落默认档）；UC-MD-04 OFF/WARN/HARD（✅）+ **minPrice**（无列，派生 ⚠️[G3]）+ **priceValidationLevel 默认"20"**（孤儿值 ⚠️[G5]）；UC-MD-05 resolveSku/findDefaultSku（✅）+ **恰一默认 SKU 约束**（无 DB 唯一约束 ⚠️）+ auto-create-default（config 声明未实现 ⚠️）；UC-MD-06 默认 SKU 停用拒（✅）+ **被引用拒删**（IErpMdSkuReferenceChecker **无生产实现**❌→生产可删）+ **SKU 独立停用**（无 status 列❌[G2]）+ 物料停用级联（读侧 filter ✅）+ 历史完整（✅）；UC-MD-07 KPI 实时聚合（✅）+ 无 SKU/无价格预警（✅）+ **阈值配置**（硬编码 ALERT_MAX_ROWS=5000 ❌）+ **行级权限**（BizModel 未应用 ❌ 疑似复用 P1-MA2-093）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对 UC-MD-01~07 给出符合性结论（取最高）：UC-MD-06 → SKU 无 status 列致独立停用不可达 + 引用检查 SPI 无生产实现→被引用 SKU 生产可删倾向 **P1**（**§4 三判据关键裁决**：L1 `use-cases.md:86-96`+设计 `sku-multi-unit.md:266-268` 明确要求"独立停用"+"被引用拒删"；**引用检查 SPI 无生产实现是数据完整性风险，会计/数据安全类强制实现无例外**[Q4=(a)]；核 sku-multi-unit.md G2 注记是否经人工批准：判据[i]plan-audit / [ii]owner doc 显式 Deferred 经**人工批准**痕迹（grep git log，AI 自标 ≠ 人工批准 methodology §4 line 168）/ [iii]product-scope 裁剪；**ORM 加 SKU status 列触及保护区域须 ask-first**）；UC-MD-03 供应商价格表 SPI 无生产实现倾向 **P1/P2**（L1 三级链明确；SPI doc"下游接线归 Deferred"自标须 §4 三判据核）；UC-MD-07 阈值硬编码+行级权限倾向 **P1/P2**（L1 明确；行级权限复用 P1-MA2-093 须核全局 transformer 覆盖；阈值硬编码与 P2-RC-009 同型）；UC-MD-04 minPrice 派生 vs 命名 + 默认"20"孤儿值倾向 **P2**；UC-MD-01 DB 唯一索引 / UC-MD-05 DB 唯一约束倾向 **P2**（G1/注记 Deferred 须 §4 三判据核经批准，**ORM 变更须 ask-first**）；UC-MD-02 全 + UC-MD-01 查找 + UC-MD-03 customer 层 + UC-MD-04 OFF/WARN/HARD + UC-MD-05 resolveSku 核心 + UC-MD-06 默认守卫+物料级联 + UC-MD-07 KPI 聚合 → **接受**。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（**P1 项核 owner doc Deferred/Non-Goal 标注的人工批准痕迹**）+ 触及保护区域标注。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-MD-01~07 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L2 引用 sku-multi-unit.md（G1/G2/G3/G5 注记 §4 复核）、L3 含行号、L4 注明断言强度、L5 标注无专属 MA2 + P1-MA1-022 复用
- [ ] UC-MD-01~07 有符合性结论且列明 §2 判据编号；候选缺口有明确分级；UC-MD-06 P1 裁决须含 owner doc Deferred 标注的人工批准痕迹核查结论；触及 ORM 保护区域项显式标注 ask-first

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/2026-08-06-0100-3-rc-ma1-a1-41-master-data-full.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` master-data sku/barcode/uom/price/minprice/default/status/reference/dashboard/supplier 同域同控制点后裁决——UC-MD-06 SKU status+引用检查为**新根因**（无 UC-MD finding）→ 新建 P1-RC（UC-MD-06）；UC-MD-03 供应商价格表 SPI 为**新根因** → 新建 P1/P2-RC（UC-MD-03）；UC-MD-07 阈值硬编码若不复用 P2-RC-009 则新建 P2-RC，行级权限若不复用 P1-MA2-093 则新建——**须先裁决复用**（P2-RC-009 mfg 看板同型阈值硬编码 / P1-MA2-093 看板直接访问同型，跨域但同控制点可复用注记）；UC-MD-04 minPrice 派生+孤儿值 / UC-MD-01 DB 索引 / UC-MD-05 DB 约束 各新建 P2-RC（视 §4 三判据复核后定 P2/接受）。执行时 grep arm-index 取 N=2 之后最新续编号避免冲突。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）+ **ORM 结构类修复（barcode DB 唯一索引 / minPrice 列 / (materialId,isDefault) DB 约束 / SKU status 列）须 ask-first + 独立 plan-audit** + **UC-MD-03 supplier SPI 接线须与 purchase 域 A1.15-A1.17 协同** + **UC-MD-06 引用检查 SPI 接线须与 purchase/sales/inventory 跨域引用协调（P1-MA1-022 successor 方向）**。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 全局 ErpOrgIsolationQueryTransformer 是否覆盖 ErpMdDashboardBizModel 查询[P1-MA2-093 复用判定] / SP-2 IErpMdSupplierPriceResolver 在采购域是否有未被 grep 发现的接线 / SP-3 enforceBarcodeUnique 在并发 save 的 TOCTOU 实际窗口[G1] / SP-4 priceValidationLevel="20" 种子分类实际 WARN 语义影响面[G5] / SP-5 IErpMdSkuReferenceChecker 生产缺失下被引用 SKU 删除的实际数据完整性事件；每存疑点一行）。**P0 即时通道评估**（UC-MD-06 引用检查缺失致被引用 SKU 可删——倾向**数据完整性 P0 候选**：须核实际是否有业务单据外键约束兜底[DB 层 FK/CASCADE]，若有 DB 层兜底则降 P1，若无则升 P0 触发 MR0；评估在报告 §7 给结论）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：无 master-data 专属 MA2 报告（无可复用行为证据，本切片为首份）；P1-MA1-022 引作跨域 daoFor 现状证据（不重审平台一致性维度）；列明只补的需求视角差异（SKU status+引用检查 / supplier SPI / 看板阈值+权限 / minPrice 派生 / DB 约束缺失）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 RC finding 入 RC 发现追踪分区；audit reports 表新增 A1.41 行（master-data 域首行）。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding 已写入 `arm-index.md`；P1-MA2-093/P2-RC-009 复用裁决有结论；静态存疑点清单已登记（SP-1~SP-5 供 A4.1/A4.2 展开）；UC-MD-06 P0 候选评估有结论
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02ec3c999ffemgsaaJDm0tRuJa，fresh session，未起草本计划）。范围/依赖/方法论/反 slack/模板/保护区域全 PASS；load-bearing 引用经实仓复核 CONFIRMED TRUE：①module-master-data/erp-md-service 存在 + ErpMdMaterialSkuBizModel 5 方法行号匹配（findSkuByBarcode:76-85/resolvePrice:128-176/validatePrice:178-203/resolveSku:101-124/validateSkuDeactivation:207-225/defaultPrepareDelete:245-249/enforceBarcodeUnique:255-271）✅；②UC-MD-06 SKU ORM 无 status 列（orm.xml:377-396，仅 isDefault 布尔，代码注记 :289 G2）+ IErpMdSkuReferenceChecker 仅 TestStubSkuReferenceChecker 无生产实现 ✅；③UC-MD-03 IErpMdSupplierPriceResolver 仅 TestStubSupplierPriceResolver 无生产实现 + IErpMdCustomerPriceResolver 有生产实现 ErpSalCustomerPriceResolver ✅；④UC-MD-04 ORM 无 minPrice 列（grep 零）+ deriveMinPrice:331-346 派生 + priceValidationLevel defaultValue="20"（orm.xml:344）孤儿值 ✅；⑤UC-MD-07 ErpMdDashboardBizModel:40 ALERT_MAX_ROWS=5000 硬编码 + IDaoProvider/IOrmTemplate 直接访问无 org/权限 filter ✅；⑥ORM 无 barcode DB 唯一索引、无 (materialId,isDefault) UK（orm.xml:408-418）✅；⑦arm-index 无 UC-MD-* finding（master-data 域首个 RC 切片）+ 无 master-data 专属 MA2 报告 ✅；⑧P1-MA1-022 resolved/P1-MA3-003/P2-MA1-030/P2-MA5-007/P1-MA2-093/P2-RC-009 均存在如述 ✅。ORM 结构类修复全标 ask-first；UC-MD-06 P0 候选（被引用 SKU 可删）正确设 MR0 评估路径（DB-FK 兜底核验）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.41 报告 9 段齐全 + UC-MD-01~07 矩阵行（逐验收标准）+ finding 登记入 arm-index（master-data 域首行）
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.41 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差多为**代码逻辑**类（预授权——SPI 生产接线/看板阈值配置化/行级权限）；**ORM 结构类（barcode DB 唯一索引 / minPrice 列 / (materialId,isDefault) DB 约束 / SKU status 列）须 ask-first + 独立 plan-audit**（roadmap 预授权声明明确排除 ORM 结构变更）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-MD-03 supplier SPI 接线须与 purchase 域 A1.15-A1.17 协同；UC-MD-06 引用检查 SPI 接线须与 P1-MA1-022 successor 跨域引用协调；UC-MD-07 行级权限修复随 P1-MA2-093/R1.29 全局 transformer 方向）
