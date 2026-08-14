# 2026-08-15-0320-2-rc-mr1-r1-29-md-supplier-price-resolver-spi RC-R1.29 — master-data supplier 价格解析 SPI 实现（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.29（P1-RC-063 master-data UC-MD-03 ④ IErpMdSupplierPriceResolver 无生产实现致采购价格表层 no-op 恒落默认档）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.29 行 + `docs/audits/arm-index.md` P1-RC-063 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 SPI 实现 + beans.xml 注册，Q9b 维持强制）
> Related: `docs/design/master-data/use-cases.md`（L1 UC-MD-03 ④）；`docs/design/master-data/sku-multi-unit.md`（§价格优先级 :180-198）；`docs/audits/2026-08-08-0015-rc-ma4-a4-2-143-146-master-data-runtime.md`（A4.2.143 运行时证据——SPI 零实现致 resolvePrice 永假跳过）；`docs/design/purchase/`（A1.15-A1.17 协同——采购单据价格表层消费）；`module-sales/erp-sal-service/.../ErpSalCustomerPriceResolver.java` + `app-service.beans.xml:173-176`（**同型范式参照**——customer SPI 生产实现 + 注册）
> Audit: required

## Current Baseline

- **finding P1-RC-063（arm-index 行，UC-MD-03 ④）**：L1（`use-cases.md:43-49`）逐字「价格优先级: 手工价 > 价格表 > SKU 默认档（purchasePrice/wholesalePrice/retailPrice）」——L1 显式三级链，价格表层含**客户专属 + 促销 + 供应商专属**（purchase 方向）。L3 实仓：`ErpMdMaterialSkuBizModel.resolvePrice:128-154` 三级链框架完整[manualPrice 即返 :135-137 → customerPriceResolver SPI :140-146 → supplierPriceResolver SPI :147-152 → pickDefaultTierPrice :153]；`IErpMdCustomerPriceResolver` 有生产实现（`module-sales/erp-sal-service/.../ErpSalCustomerPriceResolver.java implements IErpMdCustomerPriceResolver`，beans.xml 注册 `app-service.beans.xml:173-176`）✅；**`IErpMdSupplierPriceResolver` 无生产实现**——grep `implements IErpMdSupplierPriceResolver` 全生产代码 0 命中（仅测试桩）→ `ErpMdMaterialSkuBizModel.supplierPriceResolver` @Inject @Nullable 注入 null → `:147` 条件永假跳过 → 采购方向价格表层 no-op 恒落默认档。§4 三判据复核（arm-index 已裁决 + 2026-08-08 §7 Q9b 维持强制）：均不成立 → Q4=(a) 强制实现 P1。
- **实仓（HEAD 核查）**：
  - SPI 契约（`module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdSupplierPriceResolver.java:27`）：`BigDecimal resolveSupplierPrice(ErpMdMaterialSku sku, Long partnerId)`——按供应商/客户 + SKU 解析价格表层命中价；无命中返回 null。**Javadoc 自述「下游接线归 Deferred」系 AI 自标（arm-index 已裁决不构成 documented simplification），本行即该下游接线**。
  - 供应商价格表实体已存在：`ErpPurSupplierPriceList`（`module-purchase/erp-pur-dao/.../entity/_gen/_ErpPurSupplierPriceList.java`；orm.xml:384-428）——字段 supplierId（propId 2）/materialId（3）/uoMId（4，**orm.xml:391 必填 + 索引**）/currencyId（5，**orm.xml:392 必填 + 索引**）/unitPrice（6）/taxRate（7）/minOrderQuantity（8）/leadTimeDays（9）/validFrom（10）/validTo（11）/priority（12，orm.xml:399 `displayName="优先级(数字小优先)"` defaultValue=100）/isActive（13）；`ErpPurSupplierPriceListBizModel`（erp-pur-service，CRUD 壳 + unitPrice 脱敏 @BizLoader :25-28）。
  - 注入点：`ErpMdMaterialSkuBizModel:62-64` `@Inject @Nullable protected IErpMdSupplierPriceResolver supplierPriceResolver`——**按类型注入**（`jakarta.inject.Inject` + `BeanFinder.findByType` 经 `beanType.isAssignableFrom(type)` 匹配），purchase 域注册实现 bean 后 master-data 自动注入（**无需 `ioc:type` 显式声明**——`ErpSalCustomerPriceResolver` 注册于 sales `app-service.beans.xml:173-181` 即无 `ioc:type` 且被同一 master-data BizModel 类型注入成功，同型先例已证实）。
  - 依赖链：`module-purchase/erp-pur-service/pom.xml:52` 依赖 `app-erp-master-data-dao`（SPI 接口 + ErpMdMaterialSku 所在模块）compile；`app-erp-master-data-service`（:80）为 test-scope（Phase 3 跨域集成测试用）——**依赖方向合法**（purchase → master-data，SPI 解耦避免 master-data → purchase 反向环）。
  - 注册范式参照：`ErpSalCustomerPriceResolver`（sales）在 `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/beans/app-service.beans.xml:173-181` 注册（`class` + `<property name="daoProvider"><ioc:inject type="io.nop.dao.api.IDaoProvider"/></property>` 风格）——purchase 侧同型注册到 `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/beans/app-service.beans.xml`。
  - 测试基线：`TestErpMdSkuPriceValidation`（master-data resolvePrice 强测）——**注意：supplier 层已有 stub 命中场景** `testResolvePriceFromSupplierList:72-82`（`TestStubSupplierPriceResolver` 经 testBeansFile 注入，断言 SPI 价 7.77 优于默认档）——**缺的是生产实现而非 master-data 侧覆盖**；`TestStubSupplierPriceResolver`（master-data 测试桩）。
- **预授权判据**（第一批纯预授权）：纯 SPI 实现 + beans.xml 注册 + 测试，**不触 ORM 结构（ErpPurSupplierPriceList 表已存在）/会计过账/删除**；roadmap RC-R1.29 行 `todo`，Deps（R1.0 done）已满足；arm-index 行「先须人工确认 product-scope 是否裁剪 supplier 价格表」已被 2026-08-08 生效的 Q9b 人工裁决覆盖（`docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §5「维持 P1 强制实现（三判据不成立；纯 SPI 实现预授权，无 ORM 变更）」，roadmap 预授权声明段 Q5-Q9 行登记）——维持强制，不裁剪。
- **涉及文件**：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/ErpPurSupplierPriceResolver.java`（新增，对齐 ErpSalCustomerPriceResolver 包路径风格）；`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/beans/app-service.beans.xml`（注册）；`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/`（新增测试）；`docs/design/master-data/sku-multi-unit.md`（§价格优先级 supplier 层接线注记）；`docs/design/purchase/`（价格表层消费注记，如需）；`docs/audits/arm-index.md` + `docs/backlog/requirement-compliance-roadmap.md` + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **supplier 价格表层运行时成立（P1-RC-063 核心）**：purchase 域新增 `ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver`——经 `IDaoProvider` 查 `ErpPurSupplierPriceList`：`supplierId == partnerId`（L1「供应商专属」= partner 即供应商）+ `materialId == sku.materialId` + `uoMId == sku.uoMId`（**Decision 项 U1**：单位匹配语义——多单位 SKU 的价格表行须按单位精确匹配，uoMId null 视为通配或回退，参照 `ErpSalCustomerPriceResolver.java:34`「materialId + uoMId（可选）」语义）+ `isActive == true` + 效期窗口（`validFrom`/`validTo` 覆盖当前日期，null 边界视为开放）+ 命中多条时按 `priority` 取最优（数字小优先，orm.xml:399 权威声明；Decision P1 落为约束记录）+ 货币维度（**Decision 项 U2**：SPI 签名无 currencyId 参数——currencyId 不参与匹配或按当前组织默认币种过滤，与 customer 路径经 `resolvePrice` 传 null 同型，记录为 SPI 边界残余）→ 返回 `unitPrice`；无命中返回 null（回退默认档）。
- **IoC 注入闭环**：beans.xml 注册 `ErpPurSupplierPriceResolver`（无需 `ioc:type`——`BeanFinder.findByType` 按 `isAssignableFrom` 类型匹配，`ErpSalCustomerPriceResolver` 同型先例已证实）→ `ErpMdMaterialSkuBizModel.supplierPriceResolver` 自动注入非 null → `resolvePrice:151` 分支运行时可达。
- **跨域协作确认**：与 purchase A1.15-A1.17 协同——核实采购订单/单据取价是否经 `IErpMdMaterialSkuBiz.resolvePrice`（**Explore 项**：grep purchase 侧 resolvePrice 消费点；若采购侧当前不经 resolvePrice 取价，本行 SPI 落地后价格表层对采购单据的价值 = resolvePrice 被消费时才显现——登记决策记录而非扩大范围）。
- **测试**：新增测试组覆盖——① SPI 单元命中（同 supplier+material+active+效期内 → 返回价格）；② 无命中返回 null（supplier 不符/material 不符/inactive/效期外）；③ 多条命中 priority 裁决（数字小优先）；④ 效期边界（validFrom/validTo null 开放边界 + 当日命中）；⑤ 单位匹配（多单位 SKU 按 U1 裁决语义）；⑥ master-data 侧集成验证——`resolvePrice` 经注入的 supplierPriceResolver 非 null 且 supplier 命中返回价格表层价（非默认档）。
- **owner doc 收敛**：`sku-multi-unit.md §价格优先级` 补 supplier 层实现注记（SPI 生产实现 + 注册 + 查询语义含单位匹配裁决）；不修改需求契约段（use-cases L1 不动）。
- **零回归**：既有 master-data + purchase 测试全绿 + 全仓构建 + compliance checker actual ≤ baseline 或按已知失败模式 #1 登记（SPI 实现类经 `daoProvider.daoFor(ErpPurSupplierPriceList.class)` 是既有范式但**新增 1 处生产 daoFor 用法 → R2c 计数 +1 → 预期基线漂移，须在 Closure Gates 按 `docs/audits/compliance-baseline.md` per-site 证据登记**，对齐 R1.2/R1.23/R1.27 先例）。
- **回填**：arm-index P1-RC-063 → `done (RC-R1.29)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-062 SKU 独立停用**（独立 finding，触 ORM ask-first，非本行范围）。
- **不实现 P2-RC-056/057/058/059**（master-data 其他 P2 watch-only，非本行范围）。
- **不触 ORM 结构**（ErpPurSupplierPriceList 表已存在，零列/零索引变更）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不重写 resolvePrice 三级链框架**（框架完整，仅补 supplier 层实现）。
- **不实现采购侧取价流程改造**（若 Explore 发现 purchase 单据不经 resolvePrice 取价——登记决策记录供 successor，不扩大本行范围）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权 SPI 实现修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/master-data/use-cases.md`（L1 UC-MD-03 ④）+ `docs/design/master-data/sku-multi-unit.md`（§价格优先级 :180-198）+ `docs/audits/2026-08-08-0015-rc-ma4-a4-2-143-146-master-data-runtime.md`（A4.2.143 运行时证据）+ `docs/design/purchase/`（A1.15-A1.17 协同）
- Skill Selection Basis: 实现面 = SPI 实现类 + IDaoProvider 查询 + beans.xml 注册（`nop-backend-dev`：SPI 模式、跨域注入范式、beans.xml 注册、daoProvider 查询）；测试（`nop-testing`：JunitAutoTestCase + 跨域集成测试）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务（SPI 实现直接读 ErpPurSupplierPriceList 表 + 当前日期）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-purchase/erp-pur-service` + `mvn test -pl module-master-data/erp-md-service`（resolvePrice 集成面）。

## Execution Plan

### Phase 1 - Explore supplier 价格表层消费面与 priority 语义（Decision）

Status: completed
Targets: `IErpMdSupplierPriceResolver.java`；`ErpPurSupplierPriceList`（_gen 实体）；`ErpPurSupplierPriceListBizModel`；`ErpSalCustomerPriceResolver.java`；purchase 侧 resolvePrice 消费点
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **priority 命中裁决语义（P1）**：orm.xml:399 `displayName="优先级(数字小优先)"` defaultValue=100 为**权威约束记录**（非开放决策）——数字小优先，对齐 `ErpSalCustomerPriceResolver.java:64-65` 升序排序语义；同 priority 多条时以 unitPrice 最优（低者）或首条为准（**决策：倾向 unitPrice 低者优先——采购取价保守语义**，记录理由）。
      - Skill: `nop-backend-dev`
      - **裁决落地**：P1 采「priority 数字小优先 + 同 priority unitPrice 低者优先」。理由：orm.xml:399 权威声明「优先级(数字小优先)」+ `ErpSalCustomerPriceResolver.java:64-65` 升序排序语义为框架先例；同 priority 时 unitPrice 低者优先 = 采购取价保守语义（供应商竞价比价取更优价，宁低勿高），实施时若命中行 unitPrice 为 null 视为劣于非 null 行（`isBetter` 比较守卫）。
- [x] `Decision` **单位匹配语义（U1）**：`uoMId` 为必填列（orm.xml:391）——**选项 A（倾向）** = 查询匹配 `uoMId == sku.uoMId`（多单位 SKU 精确按单位取价，`sku.getUoMId()` 可达）；**选项 B（否决）** = 仅 materialId 匹配（多单位 SKU 下可能返回错误单位的价格——静默错价风险，`sku-multi-unit.md` 核心主题）。**理由（选项 A）**：对齐 `ErpSalCustomerPriceResolver.java:34`「materialId + uoMId（可选）」语义 + 多单位 SKU 的价格表行按单位维护是价格表设计意图；null sku.uoMId 时按仅 materialId 匹配（宽放）并记录。
      - Skill: `nop-backend-dev`
      - **裁决落地**：U1 采**选项 A**——查询 filter 增 `uoMId == sku.uoMId`（`sku.getUoMId()` 实仓可达，`_ErpMdMaterialSku.java:767` getter 实证）；`sku.getUoMId()` 为 null 时宽放到仅 materialId 匹配（价格表行 uoMId 恒必填非空，故宽放仅影响 SKU 侧 null 单位——记录为边界语义）。
- [x] `Decision` **货币维度边界（U2）**：SPI 签名无 currencyId 参数（`resolveSupplierPrice(sku, partnerId)`）——**选项 A（倾向）** = currencyId 不参与匹配（返回命中行 unitPrice，货币一致性由价格表维护方保证，与 customer 路径经 `resolvePrice` 传 null 同型）；**选项 B（否决）** = 按当前组织默认币种过滤（需查组织上下文——SPI 边界外扩展，签名变更非本行范围）。**记录为 SPI 边界残余**（Deferred But Adjudicated 登记）。
      - Skill: `nop-backend-dev`
      - **裁决落地**：U2 采**选项 A**——currencyId 不参与匹配。SPI 契约 `IErpMdSupplierPriceResolver.java:27` 签名无 currencyId 参数（本行不改契约），与 customer 路径 `resolvePrice:144-146` 传 null 同型；多币种精确匹配须 SPI 签名扩展（跨域契约变更非本行范围）→ 按 Deferred But Adjudicated 登记（§Deferred 货币维度条目已含）。
- [x] `Decision` **跨域取价消费面裁决（P2）**：grep purchase 侧 `resolvePrice`/`IErpMdMaterialSkuBiz` 消费点（`module-purchase` main）——**选项 A** = 采购单据已经/应经 `IErpMdMaterialSkuBiz.resolvePrice` 取价（SPI 落地即生效，本行闭环）；**选项 B** = 采购侧当前零消费（SPI 落地后价格表层对现有采购路径不生效，登记 successor 供采购取价接线行）。**Explore 证据**：grep census 结果决定——**初查（本计划起草时）`module-purchase` main 对 `resolvePrice`/`IErpMdMaterialSkuBiz` 零命中 → 倾向选项 B**，不影响本行核心义务（SPI 生产实现 + 注册是 L1 UC-MD-03 ④ 本身要求，即使采购侧尚未接线，master-data 暴露的价格表层能力必须完整）。
      - Skill: `nop-backend-dev`
      - **裁决落地**：P2 采**选项 B**——执行期 grep census（`grep -rn 'resolvePrice|IErpMdMaterialSkuBiz' module-purchase --include=*.java`）main + test 全零命中（0 消费点）→ 采购侧当前不经 `IErpMdMaterialSkuBiz.resolvePrice` 取价，SPI 落地后价格表层对现有采购单据路径不自动生效；登记 successor（§Deferred 条目已含：触发条件 = 采购取价接线行启动时）。本行核心义务不受影响（L1 UC-MD-03 ④ 本身要求 SPI 生产实现 + 注册，master-data 暴露的价格表层能力完整）。
- [x] `Proof` **运行时验证前置**：`ErpPurSupplierPriceList` 表字段/查询可达性确认（daoProvider.daoFor(ErpPurSupplierPriceList.class) + findAllByQuery 范式）；`ErpSalCustomerPriceResolver` 注册范式摘录（beans.xml :173-176 + 类实现结构）；既有 master-data resolvePrice 测试基线（TestErpMdSkuPriceValidation 全绿）。
      - Skill: `nop-testing`
      - **验证前置证据**：①`_ErpPurSupplierPriceList.java` 字段实证（supplierId propId 2 / materialId 3 / uoMId 4 / currencyId 5 / unitPrice 6 / validFrom 10 / validTo 11 / priority 12 / isActive 13，getter/setter 齐全）+ `daoProvider.daoFor(ErpPurSupplierPriceList.class).findAllByQuery(QueryBean)` 为 `ErpSalCustomerPriceResolver` 同型查询范式（后者 `daoProvider.daoFor(ErpSalPriceList.class)` + QueryBean eq filter + findAllByQuery :85-89 实证）；②注册范式摘录：sales `app-service.beans.xml:172-181`（bean id = FQCN + `<property name="daoProvider"><ioc:inject type="io.nop.dao.api.IDaoProvider"/></property>` + 无 `ioc:type` → 经 `BeanFinder.findByType` 类型注入 master-data BizModel 同型先例实证）；③既有 master-data resolvePrice 测试基线 = `TestErpMdSkuPriceValidation`（6 @Test：手工价/SPI 桩/默认档/WARN/HARD/OFF），其中 `testResolvePriceFromSupplierList:72-82` 证明 supplier 层分支在注入非 null 时行为正确——缺口纯在生产实现；④orm.xml:391/392 实证 uoMId/currencyId 必填 + 索引（IDX_..._SUPPLIER_ID/MATERIAL_ID/UO_M_ID/CURRENCY_ID :414-426）→ findAllByQuery filter 查询可达。

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] priority 裁决（P1 约束记录）+ 单位匹配（U1）+ 货币边界（U2）+ 跨域消费面裁决（P2）记录落盘计划，Explore 证据（orm.xml:399 权威声明 + grep census）确认
- [x] 注册范式 + 查询范式确认（beans.xml 结构 + daoFor 用法 + 类型注入无需 ioc:type）

### Phase 2 - SPI 实现 + 注册（P1-RC-063 核心）

Status: completed
Targets: `ErpPurSupplierPriceResolver.java`（新增）；`app-service.beans.xml`（module-purchase 注册）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 新增 `ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver`（`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/support/`，对齐 `ErpSalCustomerPriceResolver` 包路径风格）——`resolveSupplierPrice(ErpMdMaterialSku sku, Long partnerId)`：`partnerId == null || sku == null || sku.getMaterialId() == null` → null；`IDaoProvider.daoFor(ErpPurSupplierPriceList.class).findAllByQuery`（filter：`supplierId == partnerId && materialId == sku.materialId && isActive == true` + **U1 裁决的 uoMId 匹配** + 效期窗口 `validFrom <= today <= validTo`，null validFrom/validTo 视为开放边界）；命中多条按 Phase 1 P1 裁决（priority 小优先 + unitPrice 低者）取最优 → 返回 unitPrice；无命中 → null。
      - Skill: `nop-backend-dev`
      - **落地**：`ErpPurSupplierPriceResolver.java` 新增（39 行类声明 + `resolveSupplierPrice` 主链 + `findCandidates`[QueryBean eq filter：supplierId/materialId/isActive + U1 uoMId 条件过滤] + `matchesPeriod`[空端开放] + `isBetter`[P1 裁决：priority 小优先 + 同 priority unitPrice 低者，null price 劣后]）；`CoreMetrics.currentDate()` 取今日（平台工具类）；daoFor 用途 javadoc 注明（非 BizModel SPI 解析器，master-data 反向注入，无 purchase I*Biz 可注入——E3 自检）。
- [x] `Add` beans.xml 注册（`module-purchase/.../beans/app-service.beans.xml`）：bean id `app.erp.pur.service.support.ErpPurSupplierPriceResolver` + class + `daoProvider` 注入（`<ioc:inject type="io.nop.dao.api.IDaoProvider"/>`，镜像 `ErpSalCustomerPriceResolver` :173-176 注册风格）——注册后 `ErpMdMaterialSkuBizModel.supplierPriceResolver` 经类型注入自动非 null（IoC 级联，master-data 侧零改动）。
      - Skill: `nop-backend-dev`
      - **落地**：purchase `app-service.beans.xml` 注册（`:12-20`，bean id = FQCN + `<property name="daoProvider"><ioc:inject type="io.nop.dao.api.IDaoProvider"/></property>`）+ 根元素补 `xmlns:ioc="ioc"` 命名空间声明（sales beans.xml 同型）。无 `ioc:type`——`BeanFinder.findByType` 按 `isAssignableFrom` 匹配，master-data 侧零改动。
- [x] `Proof` 注入闭环静态确认：grep master-data `ErpMdMaterialSkuBizModel` 注入点 + purchase beans 注册——`supplierPriceResolver != null` 分支运行时可达（Phase 3 集成测试证实）。
      - Skill: `nop-backend-dev`
      - **证据**：`ErpMdMaterialSkuBizModel:62-64` `@Inject @Nullable protected IErpMdSupplierPriceResolver supplierPriceResolver`（jakarta.inject + 包级可见，合规 R5）→ grep `implements IErpMdSupplierPriceResolver` 生产代码 1 命中（本实现类）→ purchase beans.xml 注册行实证 → 类型注入闭环静态成立；`mvn install -DskipTests -pl module-purchase/erp-pur-service` BUILD SUCCESS（编译 + 装配资源通过）；运行时非 null 由 Phase 3 集成测试断言。

Exit Criteria:

- [x] SPI 实现类 + beans.xml 注册落地（grep 显示 `implements IErpMdSupplierPriceResolver` 生产命中 1 处 + 注册行）
- [x] 查询语义确定（supplier/material/active/效期/priority 过滤链）且与 Phase 1 裁决一致

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/`（新增 `TestErpPurSupplierPriceResolver.java`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` SPI 单测（`TestErpPurSupplierPriceResolver`）：① 命中（supplier+material+active+效期内）→ 返回 unitPrice；② 无命中返回 null（supplier 不符 / material 不符 / inactive / 效期外 4 子场景）；③ 多条命中 priority 裁决（数字小优先 + unitPrice 低者）；④ 效期边界（validFrom/validTo null 开放 + 当日命中 + 末日命中）；⑤ partnerId null / sku null 防御 → null；⑥ 单位匹配（U1 语义——同 material 不同 uoMId 价格表行，sku.uoMId 精确命中）。
      - Skill: `nop-testing`
      - **落地**：`TestErpPurSupplierPriceResolver` 新增 12 组单测全绿——`testResolveHitActiveWithinPeriod` / `testResolveNoHitSupplierMismatch` / `testResolveNoHitMaterialMismatch` / `testResolveNoHitInactive` / `testResolveNoHitOutsidePeriod` / `testResolvePrioritySmallWins` / `testResolveSamePriorityLowerUnitPriceWins` / `testResolvePeriodBoundaries` / `testResolveNullPeriodOpenHit` / `testResolveLastDayHit` / `testResolveDefensiveNulls` / `testResolveUomExactMatch`（含 sku.uoMId null 宽放裁决）。经真实 purchase beans.xml 注册注入 resolver bean（非手工 new）+ H2 落库 seed `ErpPurSupplierPriceList`（runInSession + daoProvider.daoFor 范式）。
- [x] `Proof` master-data 侧集成验证（`TestErpMdSkuPriceValidation` 扩展或新增跨域测试——**经 purchase 模块测试环境**：`erp-pur-service/pom.xml:80` 已有 master-data-service test-scope 依赖，在 purchase 模块测试中 seed `ErpPurSupplierPriceList` + 调 `IErpMdMaterialSkuBiz.resolvePrice` 经真实 purchase beans.xml 注册 → supplier 命中返回价格表层价）：`resolvePrice(skuId, partnerId=供应商, billType=purchase, manualPrice=null)` → 命中 supplier 价格表层返回价格表层价（非默认档 purchasePrice）——证实 `supplierPriceResolver` 注入非 null + 三级链 supplier 分支运行时成立。
      - Skill: `nop-testing`
      - **落地**：同一测试类 2 组 GraphQL 集成用例全绿——`testResolvePriceIntegrationSupplierTierWins`（seed material+sku[purchasePrice=10.00]+价格表行[7.77] → `ErpMdMaterialSku__resolvePrice` 经真实 beans.xml 注册的 resolver → 返回 7.77 非 10.0000，**运行时断言 supplierPriceResolver 注入非 null + 三级链 supplier 分支成立**）；`testResolvePriceIntegrationNoSupplierTierFallsBack`（无价格表行 → 回退默认档 10.0000）。
- [x] `Proof` 既有测试零回归：`mvn test -pl module-purchase/erp-pur-service` + `mvn test -pl module-master-data/erp-md-service`（记录测试计数）。
      - Skill: `nop-testing`
      - **记录**：`mvn test -pl module-purchase/erp-pur-service` **308/308 全绿**（294 基线 + 14 新增）+ `mvn test -pl module-master-data/erp-md-service` **143/143 全绿**（零回归，master-data 侧无任何变更）——两模块 BUILD SUCCESS。

Exit Criteria:

- [x] 新增 SPI 测试组 + 集成验证全绿 + 既有 purchase/master-data 测试零回归（两模块 BUILD SUCCESS）
- [x] supplier 价格表层有运行时断言证据（非仅静态接线——resolvePrice 实际返回价格表层价）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/master-data/sku-multi-unit.md`；`docs/design/purchase/`（消费面注记）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`sku-multi-unit.md §价格优先级` 补 supplier 层实现注记（SPI 生产实现 `ErpPurSupplierPriceResolver` + beans.xml 注册 + 查询语义[supplier/material/uoMId U1 裁决/active/效期/priority] + 测试证据 + Phase 1 P2 跨域消费面裁决记录 + U2 货币边界声明）；`docs/design/purchase/` 消费面注记（按 P2 裁决——若选项 B 零消费，在 purchase 相关 doc 登记「supplier 价格表层已就绪，采购取价接线为 successor」一行）；不修改需求契约段（use-cases L1 不动）。
      - Skill: none
      - **落地**：①`sku-multi-unit.md §价格优先级` 价格校验块后新增 supplier 价格表层实现注记（实现类 + 注册 + 查询语义全链 + U1/U2 裁决 + 测试证据 + P2 消费面裁决）；②`docs/design/purchase/README.md` 跨域协作表新增「供应商价格表层取价 | master-data」行（价格表层能力已就绪 + **采购单据当前不经 resolvePrice 取价（P2 选项 B）——采购取价接线为 successor**，触发条件 = 采购取价接线行启动，与 A1.15-A1.17 协同行对接）；③L1 契约段（use-cases.md）零改动确认。
- [x] `Add` arm-index P1-RC-063 → `done (RC-R1.29)` + 修复落地摘要（SPI 实现 + 注册 + 注入闭环 + 测试证据）；roadmap RC-R1.29 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      - Skill: none
      - **落地**：①arm-index `P1-RC-063` 行末列 `todo（...）` → `done (RC-R1.29)（修复落地摘要：ErpPurSupplierPriceResolver 实现 + beans.xml 注册 + 注入闭环 + TestErpPurSupplierPriceResolver 14 组 + 308/143 tests 零回归 + checker R2c 1393→1394 登记 + owner doc/purchase 注记）**【R1.0 展开归属】RC-R1.29**`；②roadmap RC-R1.29 行 `todo` → `done ✅（2026-08-15 修复落地 + 摘要）`；③`docs/logs/2026/08-15.md` 顶部新增 RC-R1.29 日志条目（决策裁决/落地/测试/验证基线/回填/Deferred 五段）。

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_ffe4455d7ffe6ZmQ4nnStZkU0j）— 0 BLOCKER / 2 MAJOR / 6 MINOR。MAJOR1 = baseline 事实修正（TestErpMdSkuPriceValidation 已有 stub 命中场景 testResolvePriceFromSupplierList:72-82，缺口是生产实现非 master-data 覆盖）——已改 baseline + Phase 3 范围表述；MAJOR2 = uoMId/currencyId 语义未裁决（必填列 + 多单位 SKU 静默错价风险）——新增 U1（uoMId 匹配）/U2（currency SPI 边界）Decision 项；6 MINOR 全部修正（行号刷新至 HEAD / supplierId propId=2 / Q9b 引用精确化 / Goals checker 漂移表述 / Phase 3 ② 不可行 fallback 删除 / 条件项确定性）。
- Independent draft review iteration 2: accept（独立子代理 ses_ffe3af2d3ffeZteewottvNHLxq）— 0 BLOCKER / 0 MAJOR。全部 iteration-1 发现确认修复 + 实仓复核通过（SPI :27 / entity propIds 2-13 / orm.xml:391-399 必填+索引+priority 权威声明 / pom :52/:80 依赖 / beans 注册目标 / 生产 grep 零实现）；剩余非阻塞 nits（Q9b 引用指向讨论文档 §5、Draft Review Record 填记、Phase 2/4 Item Types 声明调整 `Add | Proof`）已随本条处理。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成（P1-RC-063 核心义务落地：`ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver` 生产实现 + beans.xml 注册 + 注入闭环 + 测试矩阵 14 组全绿 + owner doc 收敛；Phase 1-4 全 `[x]`）
- [x] 相关文档对齐（sku-multi-unit.md §价格优先级注记 + purchase README 消费面行 + arm-index/roadmap 回填 + compliance-baseline R2c 1393→1394 登记 + 日志条目）
- [x] 已运行验证（`mvn test -pl module-purchase/erp-pur-service` **308/308 全绿**[294 基线 + 14 新增] + `mvn test -pl module-master-data/erp-md-service` **143/143 全绿** 两模块 BUILD SUCCESS + `mvn clean install -DskipTests` 全量 **BUILD SUCCESS** + `bash docs/audits/nop-compliance-checker.sh` actual == baseline **零漂移**（R1d=14 / R2a=34 / R2b=230 / **R2c=1394** / R2d=34 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40）——**R2c 1393→1394 基线上调已按 project-context 已知失败模式 #1 登记**（per-site 证据落 `docs/audits/compliance-baseline.md`「R2c 基线上调注记（plan 2026-08-15-0320-2，RC-R1.29）」块 + 基线表 + 机器可读块，镜像 R1.2/R1.23/R1.27 先例）
- [x] 无范围内项目降级为 deferred/follow-up（三个 Deferred But Adjudicated 条目均为计划起草时已登记的分类残余——P2 消费面 successor yes / U2 货币边界 successor no / 阶梯价优化候选 successor no，非本行降级）
- [x] 独立草案审查已完成并记录（Draft Review Record 两轮：iteration 1 needs revision → iteration 2 accept，0 BLOCKER / 0 MAJOR）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（Plan Status/4 Phase Status 全部 completed + 各 Phase 执行项与退出标准全 `[x]` + Closure Gates 一致 + `docs/logs/2026/08-15.md` RC-R1.29 条目写入）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### purchase 侧 resolvePrice 取价消费面（Phase 1 P2 裁决结果）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本行核心义务 = master-data 暴露的 supplier 价格表层能力完整（SPI 生产实现 + 注册，L1 UC-MD-03 ④ 本身要求）；采购单据是否经 resolvePrice 取价属消费侧接线，计划起草时初查 `module-purchase` main 对 `resolvePrice`/`IErpMdMaterialSkuBiz` 零命中 → 倾向选项 B（零消费），Phase 1 Explore 确认后登记 successor（与 A1.15-A1.17 协同行对接），不扩大本行范围。
- Successor Required: `yes`（触发条件 = 采购取价接线行启动时——purchase 单据取价须调 `IErpMdMaterialSkuBiz.resolvePrice` 使 supplier 价格表层对采购路径生效）

### 货币维度（U2 SPI 边界残余）

- Classification: `watch-only residual`
- Why Not Blocking Closure: SPI 签名无 currencyId 参数，currencyId 不参与匹配（返回命中行 unitPrice，货币一致性由价格表维护方保证）——与 customer 路径同型（`resolvePrice` 传 null）；若业务需要多币种价格表精确匹配，须 SPI 签名扩展（跨域契约变更，非本行范围）。
- Successor Required: `no`

### supplier 价格表阶梯价/批量价扩展

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前价格表字段已含 validFrom/validTo/priority/minOrderQuantity 且本行按 Phase 1 裁决消费；更复杂的定价规则（阶梯价/批量价联动 minOrderQuantity）属产品增强非 L1 UC-MD-03 四级链字面要求。
- Successor Required: `no`

## Closure

Status Note: 执行完成并独立结束审计通过（2026-08-15）。四 Phase 全绿：Phase 1 决策裁决落地（P1 priority 数字小优先+同档 unitPrice 低者 / U1 uoMId 精确匹配[null 宽放] / U2 货币不参与匹配 / P2 跨域消费面选项 B 零消费）+ Phase 2 SPI 生产实现与注册（`ErpPurSupplierPriceResolver implements IErpMdSupplierPriceResolver` + purchase beans.xml 注册[类型注入无需 ioc:type]→ `ErpMdMaterialSkuBizModel.supplierPriceResolver` 注入闭环，master-data 侧零改动）+ Phase 3 测试矩阵 14 组全绿（12 SPI 单测 + 2 GraphQL 集成——resolvePrice 运行时返回价格表层价 7.77 非默认档 10.0000）+ Phase 4 文档回填（sku-multi-unit.md/purchase README/arm-index/roadmap/日志/compliance-baseline R2c 1393→1394 基线上调登记）。验证：erp-pur-service 308/308 + erp-md-service 143/143 tests 全绿 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS + checker actual == baseline 零漂移（R2c=1394）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，只读，零文件修改）——ses_ffe1233ebffe6X64esH9JvVPKy
- Evidence: **Verdict PASS**（0 P0 / 0 P1；3 P2 非阻塞建议：①Closure Gate 文字在状态翻转前 Plan Status 为 active[规范正确，guide:72-73 仅禁止反向不一致]；②同批未跟踪 R1.30 计划文件属其他切片勿混入提交；③全量构建建议提交前补跑[执行者已在审计前实跑 BUILD SUCCESS]）——①计划状态一致性（4/4 Phase completed + 全 `[x]` + Exit Criteria 全 `[x]` + Draft Review Record 两轮 + Deferred 3 条目分类残余）；②Phase 2 实仓核验（ErpPurSupplierPriceResolver.java:39 implements + :46-48 防御 null + :63-76 findCandidates[U1 uoMId 条件过滤] + :81-88 matchesPeriod 空端开放 + :95-106 isBetter[P1 裁决] + :49 CoreMetrics.currentDate + @Inject 非 private；beans.xml :15-19 注册 + :3 xmlns:ioc；ErpMdMaterialSkuBizModel:62-64 注入点原样未动；grep 生产 `implements IErpMdSupplierPriceResolver` 恰 1 命中）；③Phase 3 测试实跑（审计者自跑 `mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurSupplierPriceResolver` 14/14 绿 + 全模块 308/308 + erp-md-service 143/143 绿）；④Phase 4 文档实仓核验（arm-index:241 done (RC-R1.29) / roadmap:421 done ✅ / sku-multi-unit.md:201 注记 / purchase README:67 消费面行 / logs 08-15.md:3-11 条目 / compliance-baseline :23 基线表 + :344-352 注记块 + :366 机器可读块 R2c: 1394）；⑤范围守卫（git status 仅预期文件，零 ORM/会计/删除/master-data 变更；审计者自跑 checker actual ≤ baseline 零漂移[R2c=1394]）。

Follow-up:

- 无范围外 follow-up；MR1 第一批后续 RC-R1.30+（maintenance 排程冲突人员维度等）由 mission driver 继续。watch-only 维持：purchase 侧 resolvePrice 取价消费面（P2 选项 B 零消费，触发条件 = 采购取价接线行启动，与 A1.15-A1.17 协同行对接）+ 货币维度精确匹配（U2，SPI 签名扩展跨域契约变更）+ 价格表阶梯价/批量价联动（optimization candidate）——均登记于 Deferred But Adjudicated。
