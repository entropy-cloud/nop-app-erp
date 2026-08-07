# 2026-08-08-0015 rc-ma4-a4-2-143-146 master-data 运行时行为确认验证报告（MA4 RC）

> 报告状态：done
> Mission: requirement-compliance（MA4 切片，Work Items A4.2.143–A4.2.146，master-data 域 MA4 运行时验证）
> Source Plan: `docs/plans/2026-08-08-0015-2-rc-ma4-a4-2-143-146-master-data-runtime.md`
> Source Audits: `docs/audits/2026-08-06-0100-3-rc-ma1-a1-41-master-data-full.md`（A1.41 §7 SP-2..SP-5 + §6 P2-RC-058 / P2-RC-057 / P1-RC-063 / P1-RC-062 + reuse P1-MA2-093）
> Audit Status: closed
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 全部工作项指定）
> 审计类型：**只读审计**（无生产代码/ORM/api.xml/view.xml/真相源变更；零 `.java`/`.xml`/`.yaml` 生产文件修改）
> 产出时间：2026-08-08

---

## §0 范围与方法（前置）

本报告为 MA4 阶段对 master-data 域切片（A1.41 全功能）MA1 报告 §7 登记的 4 项静态存疑点（A4.2.143–A4.2.146 = SP-2..SP-5）的**运行时行为确认**。方法 = grep 接线 census（全生产代码 + beans.xml + delta 层 + nop-dyn）+ TOCTOU 窗口静态确认（check-then-act 代码路径追踪）+ 种子数据复核（csv/sql/json + ORM defaultValue）+ 删除路径追踪（delete 钩子链 + GraphQL 可达性 + useLogicalDelete 语义）。

**master-data 域不产 GL 凭证声明**：master-data 是基础数据底座非业务流转域，不直接产生 GL 凭证；A4.2.146（被引用 SKU 删除）触及数据完整性探针——**只读确认，不改 ORM/删除路径**（plan Non-Goals + roadmap §横切关注点 #5 ask-first 保护区域）。

**A1.41 SP-1 排除声明**：SP-1（UC-MD-07 ④ 看板行级权限 ErpOrgIsolationQueryTransformer 覆盖）**已由 A4.1.25 + A4.2.10 合并闭合**（8 域 dashboard orgId 行级权限 P1-MA2-093 reuse R1.29 全覆盖，done ✅），本计划不重复覆盖。

**反窄化自检**：本审计跨需求正确性 / owner-doc 对齐 / 架构边界 / 验证充分性 / 回归风险 / 路由技能 / 待办漂移 七维，逐维给出裁决（见 §5 每项 + §6 汇总）。未退化为单维代码 grep。

---

## §1 存疑点→裁决总表（4 项）

> 裁决分支：①主路径闭合 ②维持分级 P1（Q4 强制实现，修复归 MR1，触 ORM/删除路径须 ask-first） ③维持分级 P2（登记不强制） ④reuse 维持注记（不新建） ⑤登记 watch-only ⑥触发 MR0。本次无项升级 MR0。

| 编号 | 存疑点 | 运行时证据（file:line） | 裁决 | 衔接既有 finding |
|------|--------|------------------------|------|------------------|
| A4.2.143 | IErpMdSupplierPriceResolver 在 purchase 域是否有未被 grep 发现的接线 | grep `implements IErpMdSupplierPriceResolver` 全生产代码 **0 命中**（仅 `TestStubSupplierPriceResolver.java:19` 测试桩）；beans.xml census——`module-purchase/.../app-service.beans.xml`（全文件）+ `module-master-data/.../app-service.beans.xml` **零 SupplierPriceResolver bean**；`app-erp-all/src/main/resources/_vfs/_delta` 仅 2 个 NopAuth 视图覆盖（无 SPI 实现）；生产代码无 Spring `@Component/@Configuration/@Service`（Nop IoC 经 beans.xml 注册）+ 无 nop-dyn 动态 bean；`ErpMdMaterialSkuBizModel.supplierPriceResolver:58-60` `@Inject @Nullable` → 无 Bean → **注入 null** → `resolvePrice:147` 条件 `supplierPriceResolver != null` 永假 → 采购价格表层 no-op 恒落默认档；**对照**：`ErpSalCustomerPriceResolver.java:40`（customer SPI 生产实现）经 `module-sales/.../app-service.beans.xml:89-90` 注册——证明 beans.xml 注册机制即为接线路径且 supplier 侧缺失 | **维持 P1** | P1-RC-063（接线完全缺失，运行时注入 null；修复归 MR1 purchase 域新增 `ErpPurSupplierPriceResolver` 纯 SPI 实现预授权不触 ask-first，须与 A1.15-A1.17 协同） |
| A4.2.144 | enforceBarcodeUnique 并发 save 的 TOCTOU 实际窗口 | `enforceBarcodeUnique:255-271` check-then-act——`findList(eq("barcode", barcode))`（:265）查无 → saveEntity（钩子 `defaultPrepareSave:230-233`/`defaultPrepareUpdate:236-239`），两操作间无锁；`isBarcodeUniqueEnabled():391-393` config-gated（CONFIG_SKU_BARCODE_UNIQUE 默认 true）；**DB 无 UK 兜底**——ORM `ErpMdMaterialSku`（orm.xml:370-418）barcode 列（:381）仅 3 个非唯一索引（IDX_MD_MATERIAL_SKU_MATERIAL_ID/UOM_ID/TAX_RATE_ID :408-417），**无 `<unique-keys>` 块**；单测 `testBarcodeDuplicateRejected:182-214` 单线程顺序执行（SKU A 先置 barcode → SKU B save 被拒）不触发并发窗口 | **维持 P2** | P2-RC-058（TOCTOU 窗口存在但单线程主路径应用层守卫强测 OK；修复归 MR1 触 ORM UK[加 UK_MD_MATERIAL_SKU_BARCODE]须 ask-first，登记不强制） |
| A4.2.145 | priceValidationLevel="20" 种子分类实际 WARN 语义影响面 | ORM `ErpMdMaterialCategory.priceValidationLevel`（orm.xml:344）`defaultValue="20"`——**孤儿非字典值**（dict `erp-md/price-validation` orm.xml:72-76 仅 OFF/WARN/HARD；常量 `ErpMdConstants.java:31-35` 同）；**种子数据普查**：全仓 csv/sql/json 零 priceValidationLevel 填充（测试 CSV 仅表头无值）→ "20" 仅经 ORM defaultValue 在插入未显式赋值时物化；`resolvePriceValidationLevel:352-369` 非字典合法值（含 "20"）→ WARN（:367-368）+ category null/不存在 → WARN；`validatePrice:178-203` WARN 分支 → `PriceValidationResult(true, true, minPrice, level)` passed=true + warning=true **警告但放行** | **维持 P2** | P2-RC-057（新创建分类默认 "20" 静默得 WARN 语义与 dict WARN="警告放行"行为对齐但违反字典契约；修复归 MR1 ORM defaultValue "20"→"WARN" 触 ORM 须 ask-first 或方案 B 文档预授权） |
| A4.2.146 | IErpMdSkuReferenceChecker 生产缺失下被引用 SKU 删除的实际数据完整性事件 | grep `implements IErpMdSkuReferenceChecker` 全生产代码 **0 命中**（仅 `TestStubSkuReferenceChecker.java:18` 测试桩）；`skuReferenceChecker:66-68` `@Inject @Nullable` → 注入 null → `validateSkuDeactivation:220` 守卫 2 `skuReferenceChecker != null` 永假 → **仅默认 SKU 守卫（:212-217）触发**；删除链：AMIS `_gen/_ErpMdMaterialSku.view.xml:131-140` row-delete/batch-delete → `@mutation:ErpMdMaterialSku__delete` → `defaultPrepareDelete:245-249` → `validateSkuDeactivation`；`useLogicalDelete="true"`（orm.xml:371-374）软删保留物理行（delVersion 置位，历史单据 skuId to-one 仍可解析）但**软删后 operational 查询消失**（delVersion=0 过滤）；硬删路径：grep `skipLogicalDelete/physicalDelete` 全 master-data 生产代码 **0 命中** | **维持 P1** | P1-RC-062（被活跃 AP/AR 发票引用的非默认 SKU 经 GraphQL delete 实际成功——生产零守卫；Q4 强制实现，修复归 MR1 触 ORM/删除路径[status 列 + 跨域 checker 接线]须 ask-first；软删主路径 delVersion 过滤是设计简化非活跃物理破坏，**不触发 MR0**） |

---

## §2 运行时证据采集细节（按四类分组）

### 2.1 接线 census（A4.2.143）

- **接口实现 census**：`rg "implements IErpMdSupplierPriceResolver"` 全仓库（排除 docs/target）命中仅 `module-master-data/erp-md-service/src/test/java/app/erp/md/service/TestStubSupplierPriceResolver.java:19`（测试桩）。SPI doc（`module-master-data/erp-md-dao/.../IErpMdSupplierPriceResolver.java:13-16`）自标「默认无实现时返回 null（价格表层空转，resolvePrice 回退到 SKU 默认档）。下游接线归 Deferred」。
- **beans.xml census**：`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/beans/app-service.beans.xml` 全文 60+ bean（Processor/Dispatcher/Builder/Checker/Linker）**零 SupplierPriceResolver**；`module-master-data/.../app-service.beans.xml`（10 bean：Dashboard/Report/PartyBizModel/OrganizationReferenceChecker/SubjectMappingResolver/ExchangeRateClientFactory/2 Processor）**零 SupplierPriceResolver**；`_dao.beans.xml` 空。测试注册仅存在于 `erp-md-service/src/test/resources/_vfs/erp/md/beans/test-supplier-price-resolver.beans.xml`。
- **delta/nop-dyn census**：`app-erp-all/src/main/resources/_vfs/_delta` 仅 `default/nop/auth/pages/NopAuthOpLog/NopAuthOpLog.view.xml` + `NopAuthSession/NopAuthSession.view.xml` 两视图覆盖——无 SPI 实现、无 beans.xml 覆盖；生产代码 grep `@Component/@Configuration/@Service`（md/pur service main）零命中（Nop IoC 注册 = beans.xml，非 Spring 注解扫描）；grep `nop-dyn`（md-service resources + app-erp-all application.yaml）零命中。
- **注入值判定**：`ErpMdMaterialSkuBizModel.java:58-60` `@Inject @Nullable protected IErpMdSupplierPriceResolver supplierPriceResolver;`——Nop IoC 按类型注入唯一实现 bean，无实现且 @Nullable → **运行时注入 null** → `resolvePrice:147-152` 条件跳过 → `pickDefaultTierPrice:311-325` 恒落默认档。
- **对照证据（机制成立性）**：`ErpSalCustomerPriceResolver.java:40 implements IErpMdCustomerPriceResolver` + `module-sales/.../app-service.beans.xml:89-90` `<bean id="app.erp.sal.service.support.ErpSalCustomerPriceResolver" class="..."/>` 注册——证明「下游域 beans.xml 注册 SPI 实现 → master-data @Inject @Nullable 自动注入」机制在 customer 侧成立，supplier 侧**接线完全缺失**。

### 2.2 TOCTOU 窗口静态确认（A4.2.144）

- **应用层 check-then-act**：`enforceBarcodeUnique:255-271`——config-gated（:256 `isBarcodeUniqueEnabled()`，`CONFIG_SKU_BARCODE_UNIQUE` `ErpMdConstants:20` 默认 true）+ barcode 空守卫（:260-262）+ `findList(eq("barcode", barcode))`（:263-265）逐行排除自身（:266）→ 命中抛 `ERR_SKU_BARCODE_DUPLICATE`（:267-269）；未命中 → 放行进入 saveEntity。**读后写窗口无锁**。
- **DB 无 UK 兜底**：ORM `ErpMdMaterialSku`（orm.xml:370-418）——barcode 列 `:381`（VARCHAR 50 非 mandatory）；`<indexes>`（:408-417）仅 `IDX_MD_MATERIAL_SKU_MATERIAL_ID/UOM_ID/TAX_RATE_ID` 三非唯一索引；**实体级 `<unique-keys>` 块缺失**（对照：ErpMdMaterial 有 `UK_MD_MATERIAL_CODE` :249 / ErpMdMaterialCategory 有 `UK_MD_MATERIAL_CATEGORY_CODE` :361——证明该实体本可声明 UK 而 SKU 未声明）。
- **并发窗口结论**：两并发 save 同 barcode → 双双通过 `findList`（互不可见对方未提交行）→ 双双落库 → **唯一性失效**。窗口在无 UK 兜底下可实际发生（非理论——check-then-act 无原子化）。
- **单线程主路径**：`testBarcodeDuplicateRejected:182-214` 顺序执行——SKU A 先置 barcode（MANAGED 实体 flush）+ SKU B save 触发 `defaultPrepareSave` → 查重命中抛 `ERR_SKU_BARCODE_DUPLICATE` 断言通过（强）。**无并发测试覆盖**（L4 缺口维持）。

### 2.3 种子数据复核 + WARN 派遣（A4.2.145）

- **字典与常量**：dict `erp-md/price-validation`（orm.xml:72-76）= OFF/WARN/HARD 三值；`ErpMdConstants.java:31-35` `PRICE_VALIDATION_OFF/WARN/HARD`——"20" 无常量承载。
- **defaultValue 孤儿值**：ORM `ErpMdMaterialCategory.priceValidationLevel`（orm.xml:344）`defaultValue="20"` 与字典契约冲突（孤儿非字典值）。
- **种子数据普查**：全仓库 csv/sql/json（含 `erp-md-service/_cases/**/erp_md_material_category.csv`——仅表头行无数据值 + `erp-md-meta/_templates/_ErpMdMaterialCategory.json` 空值模板）**零 priceValidationLevel 显式填充** → 生产种子分类若创建不显式赋值则经 ORM defaultValue 落 "20"；显式赋值走字典合法值。
- **WARN 派遣路径**：`resolvePriceValidationLevel:352-369`——materialCategoryId null → WARN（:353-355）；category 不存在 → WARN（:358-360）；OFF/WARN/HARD 原样返回（:362-365）；**任何其他值（含 G5 孤儿 "20"）→ WARN 宽松**（:367-368）。`validatePrice:178-203`——WARN 级 + below → `new PriceValidationResult(true, true, minPrice, level)`（:201-202）passed=true + warning=true **警告但放行**（与 dict WARN="警告放行"语义一致）；HARD 级抛 `ERR_PRICE_BELOW_MIN`（:195-200）；OFF 直接通过（:185-187）。
- **影响面结论**：新创建分类未显式设置 priceValidationLevel → 静默得 WARN 语义（对齐字典 WARN 行为、无阻断副作用）；孤儿值仅违反字典契约（可维护性/语义漂移风险），运行时行为正确。**登记 watch-only 部署配置决策**（defaultValue 修订归 MR1 须人工裁决方案 A[ORM ask-first]/B[文档预授权]）。

### 2.4 删除路径追踪（A4.2.146）

- **引用检查 SPI 缺失**：`rg "implements IErpMdSkuReferenceChecker"` 全仓库（排除 docs/target）命中仅 `TestStubSkuReferenceChecker.java:18`（测试桩）+ 测试 beans（`erp-md-service/src/test/resources/_vfs/erp/md/beans/test-sku-reference-checker.beans.xml`）——**生产零实现**。
- **守卫链**：`ErpMdMaterialSkuBizModel.skuReferenceChecker:66-68` `@Inject @Nullable` → 生产注入 null → `validateSkuDeactivation:207-225` 守卫 1（:212-217 isDefault=true 且 `hasOtherActiveSku:292-305` 无其他 SKU → 抛 `ERR_CANNOT_DEACTIVATE_DEFAULT_SKU`）**常驻**；守卫 2（:220 `skuReferenceChecker != null && isReferencedByBill(sku)`）**生产永假**。
- **删除可达性**：AMIS `_gen/_ErpMdMaterialSku.view.xml:116-140`——batch-delete-button（:116）+ row-delete-button（:131-140 `@mutation:ErpMdMaterialSku__delete?id=${id}`）→ GraphQL delete → `defaultPrepareDelete:245-249` → `validateSkuDeactivation`。**被活跃 AP/AR 发票引用的非默认 SKU 经 GraphQL `ErpMdMaterialSku__delete` 实际成功**（仅默认 SKU 守卫可能拦截；非默认被引用 SKU 删除成功 → 数据完整性事件，L1 UC-MD-06 ②"SKU 被业务单据引用 → 拒绝删除(只能停用)"生产零守卫）。
- **软删语义**：`useLogicalDelete="true" deleteFlagProp="delVersion" deleteVersionProp="delVersion"`（orm.xml:371-374）→ delete 置 delVersion（逻辑删），**物理行保留** → 历史单据 skuId to-one（ORM 默认不过滤 delVersion）仍可解析（L1 ⑤"存量单据保留对已停用 SKU 的引用（历史完整）"语义保持）；**但软删后 SKU 在 operational 查询消失**（默认 delVersion=0 过滤）——与 A1.41 P0 评估一致。
- **硬删路径**：grep `skipLogicalDelete|physicalDelete|hardDelete` 全 master-data 生产代码 **0 命中**——硬删路径当前不存在（软删为主路径；管理员直接 SQL 属人工越权不在应用层范围）。
- **测试证据**：`testDeleteReferencedSkuRejected:117-128`（refChecker.markReferenced → `ERR_SKU_REFERENCED_BY_BILL`，**经 SPI 桩**——证明守卫在 checker 存在时有效）+ `testDeleteUnreferencedSkuOk:130-141`（放行）——生产无 checker → 前者的拦截语义生产不可达。

---

## §3 过程纪律自检

- [x] **checker actual=baseline 门控**：本计划为只读审计，**零生产代码变更**（未修改任何 `.java`/`.xml`/`.yaml` 生产文件；仅新增本报告 + 更新 plan/roadmap/arm-index/log 文档）。closure 复核复跑 `bash docs/audits/nop-compliance-checker.sh`——**19 规则汇总与 `compliance-baseline.md §BASELINE` 块逐行一致，0 漂移**（R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=229/R2c=1382/R2d=34/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=6/R11=0/R12a=69/R12b=66/R12c=40，EXIT=0）——本审计不引入任何新命中，actual=baseline（无代码变更故无 build/test 回归风险，按 plan Closure Gates 声明）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **数据完整性探针纪律声明（A4.2.146 READ-ONLY）**：被引用 SKU 删除为只读确认——**未修改 ORM/删除路径/skuReferenceChecker 接线**（plan Non-Goals + roadmap §横切关注点 #5 ask-first 保护区域）。运行时确认被引用 SKU 删除成功属已登记 P1-RC-062 缺陷事实复述，不触发 MR0（软删主路径保留物理行 + delVersion 过滤是设计简化非活跃物理破坏，与 A1.41 P0 评估一致）。
- [x] **真相源未修改声明**：本审计未修改任何真相源（`product-scope.md` / `use-cases.md` / owner doc 需求契约段落 / `model/*.orm.xml` / 代码）。分歧维持分级记入本报告 + 既有 finding。
- [x] **A1.41 SP-1 排除声明**：SP-1（UC-MD-07 ④ 看板行级权限 ErpOrgIsolationQueryTransformer 覆盖）已由 A4.1.25 + A4.2.10 合并闭合（8 域 dashboard orgId 行级权限 P1-MA2-093 reuse R1.29 全覆盖，done ✅），本审计不重复覆盖。

---

## §4 与既有 finding 衔接（全部维持，无新建/无撤销）

| finding | 本审计裁决 | 运行时证据 |
|---------|-----------|-----------|
| **P1-RC-063**（supplier SPI 无生产实现→采购恒落默认档） | **维持 P1** | A4.2.143 beans.xml/delta/nop-dyn 全通道 census 零实现 + `supplierPriceResolver:58-60` 注入 null + `resolvePrice:147` 永假跳过；修复归 MR1 purchase 域 `ErpPurSupplierPriceResolver` 纯 SPI 实现预授权不触 ask-first |
| **P1-RC-062**（SKU status+引用检查双缺→被引用 SKU 可删） | **维持 P1** | A4.2.146 checker 生产零实现 + `validateSkuDeactivation:220` 永假 + AMIS delete 可达（`_gen` view:131-140）+ 软删语义（orm.xml:371-374）+ 硬删零路径；修复归 MR1 触 ORM/删除路径须 ask-first，Q4 强制实现 |
| **P2-RC-058**（barcode DB UK 缺失→TOCTOU） | **维持 P2** | A4.2.144 check-then-act（:263-265）+ ORM 无 `<unique-keys>`（:408-417 仅非唯一索引）+ 单线程强测 + 零并发测试；修复归 MR1 触 ORM UK 须 ask-first，登记不强制 |
| **P2-RC-057**（minPrice 派生 + "20" 孤儿值） | **维持 P2** | A4.2.145 defaultValue="20"（orm.xml:344）+ 种子零填充 + `resolvePriceValidationLevel:367-368` "20"→WARN + `validatePrice:201-202` WARN 放行；登记 watch-only 部署配置决策，修复方案 A/B 归 MR1 人工裁决 |
| **reuse P1-MA2-093**（行级权限，R1.29） | **维持 resolved R1.29 注记** | SP-1 已由 A4.1.25/A4.2.10 闭合（本计划 Non-Goals 排除，不重复覆盖） |

**无新 finding 新建**（全部维持分级）。**无项升级 MR0**（A4.2.146 软删主路径不物理破坏活跃数据 + 无 GL/库存数值破坏；A4.2.144 并发窗口需并发触发非活跃破坏）。

---

## §5 多维裁决（七维，每维一句）

- **需求正确性**：四项运行时证据均对照 L1 验收标准与 owner doc（use-cases.md / sku-multi-unit.md / price-list.md），无发现新增需求偏离；A4.2.146 被引用 SKU 生产可删与 L1 UC-MD-06 ②"被引用拒删"直接冲突 → 维持 P1-RC-062（Q4 强制实现义务不改变）。
- **owner-doc 对齐**：`sku-multi-unit.md §价格优先级/§条码唯一约束/§SKU 启停` + `README.md §关键业务规则 2`（"主数据被业务单据引用后不可物理删除"）均为活跃设计契约承诺，运行时证据再次确认 supplier 接线缺失 + 引用检查缺失实现未达 owner doc 意图 → owner doc 反向支撑 P1（非 documented simplification）。
- **架构边界**：master-data 是 DAG 根域，SPI 解耦模式（`IErpMdSupplierPriceResolver`/`IErpMdSkuReferenceChecker`）设计上避免基础域反向依赖下游域构成环；本审计确认 supplier/checker SPI 端口声明 + customer SPI 跨域实现先例（`ErpSalCustomerPriceResolver`）——模块边界无违规，仅接线缺失；本审计零代码变更。
- **验证充分性**：每项裁决均有 file:line 证据 + grep 依据；接线 census 覆盖 beans.xml/delta/nop-dyn/Spring 注解全通道；种子普查覆盖 csv/sql/json/模板；删除链追踪覆盖 AMIS→GraphQL→CRUD 钩子→守卫全路径。
- **回归风险**：零生产代码变更，checker actual=baseline 零漂移（R1a-R12c 19 规则与 baseline 一致），无 build/test 回归风险。
- **路由技能选择**：本审计路由 = verification/audit work，加载 `multi-dimensional-audit-prompt.md` 技能，符合 roadmap MA4 指定。
- **待办/自主权漂移**：范围紧致（四项只读确认 + 报告 + roadmap/log/arm-index 同步），未扩大/关闭未完成项/降级阻塞；P1/P2 finding 分级维持不撤销，修复义务完整移交 MR1（P1-RC-062/063 Q4 强制实现，P2-RC-057/058 登记不强制，均不触 ask-first 保护区域在本审计内）。

---

## §6 结论

四项存疑点（A1.41 §7 SP-2..SP-5）运行时证据链闭合：

- **维持 P1（2 finding）**：P1-RC-063（A4.2.143 supplier SPI 接线完全缺失→采购恒落默认档，运行时注入 null 确认）/ P1-RC-062（A4.2.146 被引用 SKU 删除无守卫，生产经 GraphQL delete 实际成功）—— Q4 强制实现，修复归 MR1：P1-RC-063 纯 SPI 实现预授权（须与 purchase 域 A1.15-A1.17 协同），P1-RC-062 触 ORM/删除路径须 ask-first + 独立 plan-audit §5 ORM 类。
- **维持 P2（2 finding）**：P2-RC-058（A4.2.144 barcode DB UK 缺失 TOCTOU 窗口存在但单线程主路径 OK，修复归 MR1 触 ORM UK 须 ask-first）/ P2-RC-057（A4.2.145 "20" 孤儿值静默 WARN 语义行为对齐但违反字典契约，方案 A/B 归 MR1 人工裁决）。
- **维持 reuse resolved 注记（1 项）**：P1-MA2-093 R1.29（SP-1 已由 A4.1.25/A4.2.10 闭合，本计划排除）。
- **无 MR0 触发**（master-data 不产 GL 凭证；A4.2.146 软删主路径 delVersion 过滤是设计简化非活跃物理破坏）。

本审计维持/细化既有裁决，不改变 Q4 强制实现义务。roadmap A4.2.143–A4.2.146 回写 done。

**真相源冻结条款遵守声明**：本审计未修改任何真相源。发现的 doc 分歧维持分级记入本报告 + 既有 finding，不直改真相源。
