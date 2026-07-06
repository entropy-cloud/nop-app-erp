# 2026-07-07-0024-1 主数据业务服务（SKU 扫码/换算/取价/校验/兜底/状态约束）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.12（主数据业务服务，`todo`）；`docs/audits/2026-07-06-use-case-implementation-audit.md` 建议 #1（主数据域优先实现，业务单据录入前验证的前置依赖）
> Related: `docs/design/master-data/sku-multi-unit.md`（机制权威）、`docs/design/master-data/use-cases.md`（UC-MD-01~06 可验证断言权威）、`docs/plans/2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（消费 SKU/单位解析的下游域范式）
> Audit: required

## Current Baseline

**项目阶段**（实时核实）：18 域 CRUD + 多数业务逻辑已落地。但 `master-data` 域是审计定位的**最弱域**（`2026-07-06-use-case-implementation-audit.md` §1：7 UC 中仅 1 ✅，6 🔶；完成率 14%）。设计文档 `sku-multi-unit.md` 完整，但自定义方法几乎未实现。

**BizModel/I*Biz 现状**（实时核实 `module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/`）：6 个相关 BizModel 均为 `CrudBizModel<T>` **空壳**，无任何自定义方法：
- `ErpMdMaterialSkuBizModel extends CrudBizModel<ErpMdMaterialSku>` — 无 `findSkuByBarcode`/`resolveSku`/`findDefaultSku`/`validateSkuDeactivation`。
- `ErpMdMaterialBizModel extends CrudBizModel<ErpMdMaterial>` — 无 `resolveSku`（物料级默认 SKU 兜底）。
- `ErpMdUoMConversionBizModel extends CrudBizModel<ErpMdUoMConversion>` — 无 `convertQty`/换算引擎。
- `ErpMdMaterialCategoryBizModel extends CrudBizModel<ErpMdMaterialCategory>` — 无价格校验级别读取封装。

即 UC-MD-01~06 的 6 个核心方法（`findSkuByBarcode`/`convertQty`/`resolvePrice`/`validatePrice`/`resolveSku`/`validateSkuDeactivation`）**全部缺失**（审计 §1 逐条确认）。

**实体模型已就绪**（实时核实 `module-master-data/model/app-erp-master-data.orm.xml`）：
- `ErpMdMaterialSku`（:280）：`materialId`/`skuCode`/`barcode`(VARCHAR 50, :290)/`uoMId`/`conversionRate`(DECIMAL 12,4, :292)/`purchasePrice`/`salePrice`/`wholesalePrice`/`retailPrice`(:293-296)/`taxRateId`/`isDefault`(BOOLEAN, :298)。
- `ErpMdUoMConversion`（:559）：`materialId`(可空=通用, :567)/`fromUoMId`/`toUoMId`/`conversionRate`(DECIMAL 20,8, :570)。
- `ErpMdMaterial`（:160 段）：`status`(dict `erp-md/active-status`, :184)/`baseUnitId`/`shelfLife`/`batchManaged`/`categoryId`。
- `ErpMdMaterialCategory`（:241）：`priceValidationLevel`(VARCHAR 20, defaultValue="20", dict `erp-md/price-validation`, :253)。
- `ErpPurSupplierPriceList`（purchase 域 :395）存在——采购侧价格清单。

**已确认的模型缺口**（实时核实，影响 UC-MD-04/06 完整性）：
- (G1) `ErpMdMaterialSku.barcode` **无唯一索引**（:317-327 索引仅 materialId/uoMId/taxRateId，均 non-unique）——UC-MD-01「条码全局唯一」DB 级未保证。
- (G2) `ErpMdMaterialSku` **无 `status` 字段**——UC-MD-06「SKU 独立停用」无状态列承载（仅 `ErpMdMaterial.status` 存在物料级状态 :184）。
- (G3) `ErpMdMaterialSku` **无 `minPrice` 字段**——UC-MD-04「最低价底线」无独立列承载（仅有 `priceValidationLevel` 在 MaterialCategory :253）。
- (G4) **无销售侧 PriceList 实体**——UC-MD-03「价格表匹配」客户专属/促销价格表仅采购侧 `ErpPurSupplierPriceList` 存在。
- (G5) **price-validation 字典 vs 列默认值不一致**（实时核实 `erp-md-meta/.../dict/erp-md/price-validation.dict.yaml`）：字典 `valueType: string`，合法值 `OFF`/`WARN`/`HARD`；但 `ErpMdMaterialCategory.priceValidationLevel` 列 `defaultValue="20"`(:253) 与字典**三项均不匹配**（孤儿默认值，疑似早期数值编码残留）。UC-MD-04 实现以字典字符串值为权威编码，列默认值修正归 Follow-up（保护区域）。

**配置项**（设计 `sku-multi-unit.md` §配置项，尚未在 `IErpMdConstants` 声明）：`erp-md.sku-default-required`(默认 true)、`erp-md.sku-barcode-unique`(默认 true)、`erp-md.sku-auto-create-default`(默认 true)、`erp-md.uom-conversion-strict`(默认 true)。

**剩余差距**：6 个 UC 服务方法全缺；模型缺口 G1~G4 须以 Decision/Explore 裁定（本计划默认不改 ORM 保护区域，缺口以应用层兜底 + Deferred 承接，见 Non-Goals）。

## Goals

- **UC-MD-01 扫码开单**：`findSkuByBarcode(barcode)` 落地——反查 SKU+物料；barcode 唯一性经应用层校验（`sku-barcode-unique` 配置开时，save 前查重拒绝重复），DB 唯一索引归 Deferred。
- **UC-MD-02 多单位换算**：`convertQty(materialId, qty, fromUoMId, toUoMId)` 引擎落地——按 `ErpMdUoMConversion`（物料级优先、通用 fallback）解析系数，`BigDecimal` 运算 `HALF_UP` 4 位；`uom-conversion-strict` 配置控制行级覆盖。
- **UC-MD-03 价格优先级解析**：`resolvePrice(skuId, partnerId, billType, manualPrice)` 落地——三级优先级：手工价 > 价格表（采购侧 `ErpPurSupplierPriceList`，销售侧价格表缺实体归 Non-Goal/Deferred）> SKU 默认档（按 billType 选 purchase/wholesale/retail/sale）。
- **UC-MD-04 最低价校验**：`validatePrice(skuId, finalPrice, materialCategoryId)` 落地——按 `MaterialCategory.priceValidationLevel`(OFF/WARN/HARD) 分派；底线来源经 Explore 裁定（G3 minPrice 列缺失）。
- **UC-MD-05 默认 SKU 兜底**：`resolveSku(materialId, unitId)` / `findDefaultSku(materialId)` 落地——unitId 非空按物料+单位匹配，否则取 `isDefault=true`；`sku-default-required` 配置开时无默认 SKU 抛 `NopException`。
- **UC-MD-06 SKU 状态约束**：`validateSkuDeactivation(skuId)` / 删除前引用校验落地——含「不能停用/删除唯一默认 SKU」「物料停用联动 SKU 不可被新单引用」「被未完成单据引用拒绝删除」；SKU 独立 status 列缺失（G2）的裁定经 Explore。
- **服务层测试证明**：每个 UC 的可验证断言（`use-cases.md`）经集成测试覆盖，`mvn test -pl module-master-data/erp-md-service -am` 全绿。

## Non-Goals

- **不改任何 `model/*.orm.xml`/`.api.xml`**（保护区域）——G1 barcode 唯一索引、G2 SKU.status 列、G3 minPrice 列均不改；以应用层校验 + Deferred 承接（与 `2026-07-01-0811-2` 保护区域纪律一致）。如 Explore 裁定某缺口必须加列，该列变更须拆为独立计划并经人工批准，不并入本计划。
- **不接线下游域（purchase/sales/inventory）的单据行取价/换算调用**——本计划只交付 master-data 域服务方法 + `@BizQuery`/`@BizMutation` 暴露；下游单据行调用方改造归各自域后继计划。
- **不做销售侧客户专属/促销 PriceList 实体**（G4）——`ErpMdPriceList`/`ErpSalPriceList` 不存在，新建实体属保护区域 + 独立设计；UC-MD-03 价格表层仅覆盖采购侧既有 `ErpPurSupplierPriceList`。
- **不做价格表 CRUD/有效期/批量导入**——仅消费既有价格表实体做匹配。
- **不做批次规则校验/保质期预警/物料替代建议/自动编码/供应商默认货源**——这些是 roadmap 1.12 文案中列举但**不在 `use-cases.md` UC-MD-01~06 范围内**的项目（见 Task Route 裁定），归独立后继。
- **不做条码生成规则（EAN 校验位/内部码）**——UC-MD-01 只消费已存在 barcode 反查 + 唯一性校验。

## Task Route

- Type: `implementation-only change`（greenfield BizModel 方法，复用已生成实体，不改公共 API 契约或 ORM）。
- Owner Docs: `docs/design/master-data/sku-multi-unit.md`、`docs/design/master-data/use-cases.md`、`docs/design/master-data/README.md`、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（CrudBizModel 扩展、I*Biz）、`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`。
- Skill Selection Basis: 实施阶段 `Skill: nop-backend-dev`（BizModel/IBiz/xbiz 方法编写——技能描述匹配「后端开发、BizModel、IBiz、写方法、加接口」）；验证阶段 `Skill: nop-testing`（JunitAutoTestCase 集成测试）。独立草案/结束审计用审计提示模板（非技能）。
- **范围裁定（roadmap 文案 vs use-cases.md 冲突）**：roadmap 1.12 括注「SKU 多单位自动转换 / 物料替代建议 / 自动编码生成 / 批次规则校验 / 保质期预警 / 供应商默认货源」与 `use-cases.md` UC-MD-01~06 实际内容（扫码/换算/取价/最低价/默认SKU/状态约束）**不一致**。按 `AGENTS.md` 真相源优先级，用例定义以 `design/master-data/use-cases.md` 为权威，设计机制以 `sku-multi-unit.md` 为权威；roadmap 括注为陈旧标签。本计划实施 UC-MD-01~06（`use-cases.md` 权威），roadmap 文案修正归 Follow-up。

## Infrastructure And Config Prereqs

- 无新增基础设施。H2 内存库（`erp-md-app` 已含 `quarkus-jdbc-h2`；服务层测试 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`）。
- `erp-md-service` test 依赖：跨域引用 `ErpPurSupplierPriceList`（purchase 域）需确认是否已有 test-scope 依赖；若无，UC-MD-03 价格表层测试以 master-data 自建 SKU 价格档为主、purchase 价格表匹配以 mock 或加 test 依赖验证（Decision 在 Phase 2 裁定）。
- 新增配置键（在 `ErpMdConstants` 声明，经 `AppConfig.var` 读取）：`erp-md.sku-default-required`/`erp-md.sku-barcode-unique`/`erp-md.sku-auto-create-default`/`erp-md.uom-conversion-strict`。
- 无数据迁移/回滚脚本需求（greenfield 方法，复用既有实体）。

## Execution Plan

### Phase 1 - UC-MD-01 扫码 + UC-MD-05 默认 SKU 兜底 + UC-MD-02 单位换算引擎

Status: completed
Targets: `module-master-data/erp-md-dao/.../IErpMdMaterialSkuBiz.java`（增方法签名）、`.../IErpMdUoMConversionBiz.java`、`module-master-data/erp-md-service/.../entity/ErpMdMaterialSkuBizModel.java`、`.../ErpMdUoMConversionBizModel.java`、`.../ErpMdMaterialBizModel.java`、`module-master-data/erp-md-service/.../ErpMdConstants.java`、`module-master-data/erp-md-service/src/test/.../`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（读/解析方法不依赖价格表/校验级别）

- [x] `Decision`：barcode 唯一性实施层（G1）。**裁决**：应用层校验——`ErpMdMaterialSkuBizModel` 经 `defaultPrepareSave`/`defaultPrepareUpdate` 钩子，按 `sku-barcode-unique` 配置查重（`barcode is not null` 且同 barcode 已存在另一 SKU → 抛 `NopException(ERR_SKU_BARCODE_DUPLICATE)`）。DB 唯一索引归 Deferred（保护区域）。**备选（被否）**：本计划加 ORM 唯一索引——保护区域变更须独立计划 + 人工批准，不并入。**残留风险**：应用层查重存在 TOCTOU 窗口（并发新建同 barcode），master-data SKU 创建低并发可接受，DB 索引 Deferred 补强。
  - Skill: nop-backend-dev
- [x] `Decision`：单位换算系数解析优先级。**裁决**：`convertQty` 先查物料级 `ErpMdUoMConversion`(materialId 非空, fromUoMId→toUoMId)，未命中查通用(materialId null)；仍未命中且 `uom-conversion-strict=true` 抛 `ERR_UOM_CONVERSION_NOT_FOUND`，`false` 时回退 `SKU.conversionRate`。`BigDecimal` `HALF_UP` scale=4。同单位换算（fromUoMId==toUoMId）返回原值。**备选（被否）**：仅用 SKU.conversionRate——忽略多单位组（箱↔托盘）非相邻换算。**残留风险**：通用换算系数跨物料共用可能不精确（strict 默认 true 已防护）。
  - Skill: nop-backend-dev
- [x] `Add`：`IErpMdMaterialSkuBiz` 增 `findSkuByBarcode(String barcode)`（@BizQuery）、`findDefaultSku(Long materialId)`、`resolveSku(Long materialId, Long unitId)`、`validateSkuDeactivation(Long skuId)`（UC-MD-06 实现在 Phase 3，此处先签名）。`ErpMdMaterialSkuBizModel` 实现：`findSkuByBarcode` 按 barcode 查询返回 SKU（调用方经关系取 material）；`findDefaultSku` 查 `materialId + isDefault=true`；`resolveSku` unitId 非空按物料+单位匹配否则 `findDefaultSku`，无默认且 `sku-default-required=true` 抛 `NopException(ERR_SKU_DEFAULT_REQUIRED)`。
  - Skill: nop-backend-dev
- [x] `Add`：`IErpMdUoMConversionBiz` 增 `convertQty(Long materialId, BigDecimal qty, Long fromUoMId, Long toUoMId)`（@BizQuery）。`ErpMdUoMConversionBizModel` 实现换算引擎（按上方 Decision）。`ErpMdConstants` 声明 4 个配置键 + `ERR_SKU_*`/`ERR_UOM_*` ErrorCode（中文描述，扩展 `NopException`）。
  - Skill: nop-backend-dev
- [x] `Proof`：集成测试（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`，`createPrereqs()` 自建物料/SKU/单位/换算）——`testFindSkuByBarcode`（命中+未命中）、`testFindDefaultSku`（有/无默认）、`testResolveSkuByUnit`（按单位匹配/兜底默认）、`testResolveSkuNoDefaultRequired`（配置开抛错）、`testConvertQtyMaterialLevel`（物料级系数）、`testConvertQtyGenericFallback`（通用 fallback）、`testConvertQtyStrictNotFound`（strict=true 抛错）、`testBarcodeDuplicateRejected`（应用层查重拒绝重复 barcode）。`mvn test -pl module-master-data/erp-md-service -am` 全绿。
  - Skill: nop-testing

Exit Criteria:

> 本阶段交付扫码/默认SKU/换算三大读解析能力。完整仓库 `mvn test` 归 Closure Gates。

- [x] 8 个行为测试存在且 `mvn test -pl module-master-data/erp-md-service -am` 全绿
- [x] barcode 唯一性应用层校验 + 换算物料级/通用 fallback + 默认 SKU 兜底均经测试证明

### Phase 2 - UC-MD-03 价格优先级解析 + UC-MD-04 最低价校验

Status: completed
Targets: `.../IErpMdMaterialSkuBiz.java`（增 resolvePrice/validatePrice）、`.../ErpMdMaterialSkuBizModel.java`、`.../ErpMdMaterialCategoryBizModel.java`、`module-master-data/erp-md-service/src/test/.../`
Skill: nop-backend-dev

- Item Types: `Explore | Decision | Add | Proof`
- Prereqs: Phase 1

- [x] `Explore`（须先于 UC-MD-04 Decision 完成）：G3 minPrice 列缺失的底线来源调研。**结论**：核实既有单据行（purchase/sales/inventory）无独立 minPrice 语义依赖——ErpMdMaterialSku 无 minPrice 列，ErpPurSupplierPriceList 无 minPrice 列，设计文档 sku-multi-unit.md §多档价格 列出 minPrice 但 ORM 未落地（G3）。选项 (a) 加列属保护区域须独立计划；(b) 派生底线（四档价最小正值）零迁移可立即落地；(c) 配置化全局兜底粒度过粗。**选定 (b)**：以 SKU 的 purchasePrice/wholesalePrice/retailPrice/salePrice 四档中 signum>0 的最小值为派生底线，全空返回 null（不限制）。
  - Skill: nop-backend-dev
- [x] `Decision`：UC-MD-04 底线来源（依 Explore 结论）。**裁决**：选 (b) 派生底线——`deriveMinPrice(sku)` 取四档价最小正值。零 ORM 迁移，不触保护区域。**备选（被否）**：(a) 加 minPrice 列须独立 successor（保护区域 G3）；(c) 配置化全局兜底粒度过粗无法按 SKU 差异化。**残留风险**：派生底线=最低档价，语义等同「不能低于最低档」，若业务需独立维护 minPrice 区别于档价，须 successor 加列（G3 Deferred 已跟踪）。
  - Skill: nop-backend-dev
- [x] `Decision`：UC-MD-03 价格表层范围（G4）+ 跨域访问模式。**裁决**：价格表层仅覆盖采购侧既有 `ErpPurSupplierPriceList`，**但 master-data 不得反向依赖 purchase（依赖环约束）**——经 SPI `IErpMdSupplierPriceResolver` 解耦：master-data 声明端口，下游域实现注册。优先级：手工价 > SPI 价格表 > SKU 默认档（billType=PURCHASE→purchasePrice/WHOLESALE→wholesalePrice/RETAIL→retailPrice/默认→salePrice）。**备选（被否）**：(a) master-data 经 IDaoProvider 直接查 ErpPurSupplierPriceList——构成 master→purchase 编译依赖环，违反域边界（codebase-map 确认 purchase→master 单向）；(b) 新建销售 PriceList 实体——保护区域 + 独立设计（G4 Deferred）。**残留风险**：SPI 未被下游实现前价格表层空转（仅 SKU 默认档生效），销售取价 UX 受限（Deferred 跟踪下游接线）。
  - Skill: nop-backend-dev
- [x] `Add`：`IErpMdMaterialSkuBiz` 增 `resolvePrice(Long skuId, Long partnerId, String billType, BigDecimal manualPrice)`（@BizQuery）+ `validatePrice(Long skuId, BigDecimal finalPrice, Long materialCategoryId)`（@BizQuery，返回 PriceValidationResult bean）。`ErpMdMaterialSkuBizModel.resolvePrice` 实现三级优先级（manualPrice 非空直接返回；否则 SPI `IErpMdSupplierPriceResolver` 命中返回；否则 SKU 默认档）。`validatePrice` 读 `ErpMdMaterialCategory.priceValidationLevel`——**以字典字符串值 `OFF`/`WARN`/`HARD` 为权威编码**（G5：列默认值 `"20"` 孤儿不参与逻辑，非字典值统一按 WARN 宽松），分派：`HARD` 低于底线抛 `NopException(ERR_PRICE_BELOW_MIN)`、`WARN` 返回 warning=true、`OFF` 直接通过。
  - Skill: nop-backend-dev
- [x] `Proof`：`testResolvePriceManualWins`（手工价优先）、`testResolvePriceFromSupplierList`（SPI 价格表命中，经测试侧 SPI impl 模拟）、`testResolvePriceDefaultTier`（按 billType 选默认档）、`testValidatePriceHardReject`（HARD 低于底线抛错）、`testValidatePriceWarnAllows`（WARN 放行带警告）、`testValidatePriceOff`（OFF 不校验）。`mvn test -pl module-master-data/erp-md-service -am` 全绿。
  - Skill: nop-testing

Exit Criteria:

> 本阶段交付取价三级优先级 + 最低价校验分派。完整仓库 `mvn test` 归 Closure Gates。

- [x] Explore 结论已书面记录且 UC-MD-04 Decision 依其裁定
- [x] 6 个取价/校验行为测试存在且 `mvn test -pl module-master-data/erp-md-service -am` 全绿

### Phase 3 - UC-MD-06 SKU 状态约束 + 收尾

Status: completed
Targets: `.../ErpMdMaterialSkuBizModel.java`、`.../ErpMdMaterialBizModel.java`、`module-master-data/erp-md-service/src/test/.../`、`docs/logs/2026/07-07.md`
Skill: nop-backend-dev

- Item Types: `Explore | Decision | Add | Proof`
- Prereqs: Phase 2

- [x] `Explore`（须先于 UC-MD-06 Decision 完成）：G2 SKU.status 列缺失调研。**结论**：核实下游域（purchase/sales/inventory）单据行均引用 SKU ID 但未按 SKU.status 做引用过滤（无 SKU.status 列，下游依赖物料级 status）。选项 (a) 加列须独立 successor（保护区域）；(b) 仅落地不依赖 SKU.status 的子约束可行且覆盖 UC-MD-06 主要断言。**选定 (b)**。
  - Skill: nop-backend-dev
- [x] `Decision`：UC-MD-06 范围（依 Explore 结论）。**裁决**：选 (b)——落地「停用/删除唯一默认 SKU 拒绝（{@code validateSkuDeactivation} 默认 SKU 守卫）」「物料 status 停用时其 SKU 不可被新单引用（{@code ErpMdMaterialSkuBizModel.isMaterialActive} 在 resolveSku/findDefaultSku 时过滤）」「SKU 被未完成单据引用拒绝删除（{@code defaultPrepareDelete} 钩子 + SPI {@code IErpMdSkuReferenceChecker}）」；SKU 独立 status 列 + 独立停用归 Deferred successor（G2）。**残留风险**：无法单独停用某 SKU 而保留同物料其他 SKU 可用——须加列 successor（G2 Deferred 已跟踪）。
  - Skill: nop-backend-dev
- [x] `Decision`：删除前引用校验的跨域访问模式。**裁决**：master-data 不直接查下游域单据表（依赖环约束）；采用 SPI `IErpMdSkuReferenceChecker` 端口模式——master-data 声明端口，下游域各自实现注册（{@code @Nullable @Inject}）。默认无 checker 时跨域引用校验空转（仅域内默认 SKU 守卫生效）。**备选（被否）**：(a) master-data 经 IDaoProvider 直接查下游单据表——构成 master→下游编译依赖环；(b) 不做删除引用校验——UC-MD-06 断言无法满足。**残留风险**：SPI 未被下游域实现前跨域引用校验空转，须 Deferred 跟踪下游接线。
  - Skill: nop-backend-dev
- [x] `Add`：`ErpMdMaterialSkuBizModel.validateSkuDeactivation(skuId)`——默认 SKU 守卫（`isDefault=true` 且无其他可用 SKU → `ERR_CANNOT_DEACTIVATE_DEFAULT_SKU`）；`defaultPrepareDelete` 钩子触发引用校验；`isMaterialActive(materialId)` 在 resolveSku/findDefaultSku 时物料级 status 过滤（联动停用物料 → SKU 不可被新单引用）。`ErpMdMaterialBizModel.defaultPrepareUpdate` 提供 `onMaterialDeactivated` 扩展点（protected，默认空，供下游覆盖通知/日志）。
  - Skill: nop-backend-dev
- [x] `Proof`：`testCannotDeactivateOnlyDefaultSku`、`testCanDeactivateNonDefaultSku`、`testMaterialDeactivateCascadeGuard`、`testDeleteReferencedSkuRejected`、`testDeleteUnreferencedSkuOk`。`mvn test -pl module-master-data/erp-md-service -am` 全绿。
  - Skill: nop-testing

Exit Criteria:

> 本阶段交付 SKU 状态约束子集 + 收尾。完整仓库 `mvn test` 归 Closure Gates。日志更新为计划级结束步骤（见 Closure Gates），非每阶段项目。

- [x] Explore 结论已记录且 UC-MD-06 Decision 依其裁定
- [x] 跨域访问模式 Decision 已裁定且未引入依赖环
- [x] 4 个状态约束行为测试存在且 `mvn test -pl module-master-data/erp-md-service -am` 全绿

## Draft Review Record

- Independent draft review iteration 1: **accept / consensus**（ses_0c7bbd2acffeMMVPKWJmr147rD，独立 general 子代理，新会话）— 实时核实全部基线主张属实（4 BizModel 空壳、SKU 缺 status/minPrice、barcode 无唯一索引、UoMConversion.materialId 可空、MaterialCategory.priceValidationLevel 存在、ErpPurSupplierPriceList 存在且无销售侧 PriceList、roadmap 1.12 括注与 use-cases.md 实际冲突）；G1~G4 模型缺口与 roadmap 漂移诚实处理（Non-Goals + Explore/Decision + Deferred 含 successor 触发）；无范围内项目静默丢弃；anti-slack 净；item 类型/技能/结束门控完整；退出标准未用全仓库验证填充。计划为可接受执行契约。采纳 4 项非阻塞建议并据以修订：S1（price-validation 字典为 string OFF/WARN/HARD、列默认值 "20" 孤儿——新增 G5 入基线 + 移除 Phase 2 数值映射硬编码，编码交 Explore/Decision 裁定）；S2（Phase 3 跨域删除引用校验新增独立 Decision：master-data 不得反向依赖下游域，采用端口 SPI 模式）；S3（Phase 3 日志 Add 项移除，日志归计划级 Closure Gates，对齐执行时 rule 10）；S4（`validatePrice` 公共表面确定为 @BizQuery 返回校验结果 bean，去除「或内部 helper」歧义）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：UC-MD-01~06 服务方法 + 应用层唯一性/约束校验全部落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 工作项 1.12 标注进展；`use-cases.md` 与实现一致；当日日志已记
- [x] 已运行验证：`mvn test -pl module-master-data/erp-md-service -am` 全绿（45 tests）；根 `mvn clean install -DskipTests` = BUILD SUCCESS（154 模块）；`mvn test -fae` = BUILD SUCCESS（0 failures/0 errors，无回归）
- [x] 无范围内项目降级为 deferred/follow-up（G1~G4 模型缺口均为计划内 Non-Goal/Explore 裁定，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### ErpMdMaterialSku.barcode DB 唯一索引（G1）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 应用层查重已防常规场景；DB 唯一索引须改 ORM（保护区域）。master-data SKU 创建低并发。
- Successor Required: yes（触发条件：启用高并发批量 SKU 导入时，或批量加唯一索引的保护区域计划获批时）

### ErpMdMaterialSku.status 列 + SKU 独立停用（G2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划落地不依赖 status 的子约束（默认 SKU 守卫 + 物料级联动 + 删除引用校验）；独立停用某 SKU 须加列（保护区域）。
- Successor Required: yes（触发条件：需独立停用单 SKU 而保留同物料其他 SKU 时）

### ErpMdMaterialSku.minPrice 列 + 独立底线语义（G3）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: UC-MD-04底线来源经 Explore 裁定（派生档价/配置兜底）；独立 minPrice 列须改 ORM（保护区域）。
- Successor Required: yes（触发条件：业务须独立维护最低价底线区别于档价时，须独立计划加列 + 人工批准）

### 销售侧客户专属/促销 PriceList 实体（G4）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: UC-MD-03 价格表层仅覆盖采购侧既有实体；销售 PriceList 须新建实体（保护区域 + 独立设计）。
- Successor Required: yes（触发条件：销售单据须按客户/促销价格表取价时）

## Closure

Status Note: 执行者完成全部 3 Phase 实施与验证（UC-MD-01~06 服务方法 + 应用层校验 + 跨域 SPI 端口；21 行为测试全绿；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；全 workspace `mvn test -fae` BUILD SUCCESS 0 failures/0 errors）。Plan Status 标记 completed；roadmap 1.12 ❌→✅；当日日志 `docs/logs/2026/07-07.md` 已记。G1~G5 模型缺口经计划内 Explore/Decision 裁定以应用层校验 + Deferred successor 承接（非范围内降级）。独立结束审计已由独立子代理执行并通过（见下方证据）。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-audit 子代理（新会话，不重用执行者上下文）
- Audit Method: 冷重读完整计划 + 对照实时仓库逐项核实（grep/glob/read 全量核实 IBiz/BizModel/SPI/DTO/测试/日志）
- Phase/Item Consistency: 全部 3 Phase `Status: completed`，所有执行项目 `[x]`，阶段体无残留 `[ ]`；退出标准全 `[x]`
- Live Code Verification（对照实时仓库）:
  - `IErpMdMaterialSkuBiz`（erp-md-dao/biz）声明 6 个 @BizQuery 方法（findSkuByBarcode/findDefaultSku/resolveSku/resolvePrice/validatePrice/validateSkuDeactivation），签名与计划一致（context 末参 + @Optional 可空参）
  - `IErpMdUoMConversionBiz` 声明 `convertQty` @BizQuery
  - `ErpMdMaterialSkuBizModel`（366 行）实现全部 6 方法——真实分支逻辑、真实 QueryBean 查询、`defaultPrepareSave/Update/Delete` 钩子接 barcode 查重与引用校验；无空体/无 `return null` 占位
  - `ErpMdUoMConversionBizModel`（141 行）实现物料级→通用 fallback→strict 抛错→SKU.conversionRate 回退四级引擎，HALF_UP scale=4
  - `ErpMdMaterialBizModel` 实现 `defaultPrepareUpdate` + `onMaterialDeactivated` protected 扩展点（默认空，扩展点非占位）
  - `ErpMdConstants`/`ErpMdErrors` 声明 4 配置键 + 价格校验级别字典值 + 单据类型编码 + 6 ErrorCode（中文描述）
  - SPI 端口 `IErpMdSupplierPriceResolver`/`IErpMdSkuReferenceChecker`（erp-md-dao/spi）经 `@Nullable @Inject` 注入——端口模式非空转占位，测试经 `TestStubSupplierPriceResolver`/`TestStubSkuReferenceChecker` 桩 + testBeansFile 验证运行时可达
- Anti-Hollow Check: 无空方法体、无 `return null` 占位、无吞异常；SPI `@Nullable` 注入是计划内端口模式（避免 master→下游依赖环），文档化且经桩测试验证可达
- Test Verification: 3 测试类共 21 个 @Test（`TestErpMdSkuServices`=9 / `TestErpMdSkuPriceValidation`=7 / `TestErpMdSkuStatusConstraints`=5），覆盖命中/未命中、物料级/通用/strict、手工价/SPI/默认档、HARD/WARN/OFF、默认 SKU 守卫/物料联动/引用校验——与计划退出标准一致（实际测试数 ≥ 计划承诺数 8/6/4）
- Docs Sync: `docs/logs/2026/07-07.md` 含详细计划级条目（3 Phase 工作总结 + 关键决策 + 下一步），符合 AGENTS.md 文档维护规则
- Deferred Honesty: G1~G5 均在 `Deferred But Adjudicated` 含 Classification + Successor 触发条件；无范围内缺陷隐藏为 Follow-up
- Five-Point Consistency: Plan Status=completed / 全部 Phase Status=completed / 全部 Exit Criteria `[x]` / Closure Gates 全 `[x]` / 日志条目存在——一致
- Check Command: `node ../attractor-guided-engineering-template/tools/mission-driver/src/plan-check.mjs docs/plans/2026-07-07-0024-1-master-data-sku-services.md --strict` → `passed: true`（0 unchecked）

Follow-up:

- roadmap 1.12 文案修正（与 `use-cases.md` UC-MD-01~06 对齐，触发条件：本计划 active 后）
- `ErpMdMaterialCategory.priceValidationLevel` 列默认值 `"20"` 孤儿修正为字典合法值（G5，触发条件：保护区域 ORM 默认值变更计划获批时）
- 下游域（purchase/sales/inventory）单据行接线 resolvePrice/convertQty/resolveSku + 实现 `IErpMdSkuReferenceChecker` SPI（触发条件：各域单据录入改造计划起草时）
- G1~G4 各 Deferred successor（见上方）
