# 2026-08-19-0445-1-rc-mr1-r1-72-md-sku-status-reference-check RC-R1.72 — master-data SKU 独立停用 + 跨域引用检查生产实现（A 类 ORM：ErpMdMaterialSku 加 status 列 + IErpMdSkuReferenceChecker 四域生产实现 + List 收集器聚合）

> Plan Status: completed
> Last Reviewed: 2026-08-19
> Mission: requirement-compliance
> Work Item: RC-R1.72（P1-RC-062，UC-MD-06 ③④「SKU 独立停用 + 被引用拒删」）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.72 行 + `docs/audits/arm-index.md` P1-RC-062（A1.41 切片登记 + A4.2.146 运行时确认维持 P1）+ 2026-08-12 批量裁决 A 类（roadmap 头 :41「master-data: RC-R1.72（ErpMdMaterialSku 加 status 列）」ORM 修改授权已批量批准，对齐 Q3 纯加性类自动执行，越界回落双独立子 agent 批准；行标签仍携旧「越界项」措辞，done 回写时按 R1.61-67 先例同步改写）
> Related: `docs/design/master-data/use-cases.md` UC-MD-06（:86-96）+ `docs/design/master-data/sku-multi-unit.md` §SKU 状态管理/§SKU 启停/§SKU 状态校验（:259-282）+ `docs/design/master-data/README.md`（:121-126）；`docs/plans/2026-08-07-1932-*`（R1.1-R1.3 先例）；fin 侧 `ErpMdEmployeeReferenceCheckerImpl`（跨域 checker 生产实现唯一先例，plan 2026-07-23-1145-2）
> Audit: required

## Current Baseline

- **finding P1-RC-062（arm-index A1.41 注记 + A4.2.146 运行时维持 P1）**：L1 `use-cases.md:88-94` 逐字断言：①停用唯一默认 SKU → 拒绝（必须先设其他默认）②SKU 被业务单据引用 → 拒绝删除（只能停用）③物料停用 → 联动所有 SKU 不可被新单引用 ④存量单据保留对已停用 SKU 的引用（历史完整）。sku-multi-unit.md §SKU 启停 :263-278 规则树另要求「SKU 独立停用 → 该 SKU 不可被新单据引用（其他 SKU 仍可用）」+ §SKU 状态校验 :280-282「停用 SKU 前校验：(1) 默认 SKU 必须存在其他可用 SKU 接替 (2) 被未完成业务单据引用则拒绝停用」。
- **L3 实仓（HEAD 核查，双路探索复核）**：
  - **SKU 无独立停用载体**：`ErpMdMaterialSku`（`module-master-data/model/app-erp-master-data.orm.xml:371-419`）18 列 propId 1-18，**无 status 列**（对照：ErpMdMaterial.status propId 7 :201、ErpMdPartner.status propId 5 :433 均 dict `erp-md/active-status`）；dict `erp-md/active-status`（:59-62，ACTIVE/INACTIVE）已存在可复用。`hasOtherActiveSku`（`ErpMdMaterialSkuBizModel.java:292-309`）javadoc 自述「当前实体无 status 列（G2），故『可用』=同物料+id 不同」——无 status 过滤。
  - **引用检查空转**：`IErpMdSkuReferenceChecker`（`module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdSkuReferenceChecker.java:15-23`，`boolean isReferencedByBill(ErpMdMaterialSku sku)`）**零生产实现**（仅测试桩 `TestStubSkuReferenceChecker`）；`ErpMdMaterialSkuBizModel.skuReferenceChecker` @Inject @Nullable（:70-72）→ `validateSkuDeactivation`（:211-229）守卫 2（:223-227）永假跳过。A4.2.146 运行时证实：被活跃单据引用的非默认 SKU 经 AMIS delete（`_gen/_ErpMdMaterialSku.view.xml:131-140`）实际删除成功（软删，`useLogicalDelete="true"` orm.xml:371-376）。
  - **守卫骨架已就绪**：`defaultPrepareDelete`（:249-253）已调 `validateSkuDeactivation`；守卫 1（默认 SKU 唯一性 :216-222，`ERR_CANNOT_DEACTIVATE_DEFAULT_SKU`）+ 错误码 `ERR_SKU_REFERENCED_BY_BILL`（`ErpMdErrors.java:88-91`）均已存在——缺的是 status 列语义 + 真实 checker。
  - **物料级联删除旁路（草案审查发现的矛盾路径）**：`ErpMdMaterial.skus` to-many tagSet 含 `cascade-delete`（orm.xml:244-246）且 `ErpMdMaterialBizModel` **无 defaultPrepareDelete 覆写**——删除物料在 ORM 层级联删除其全部 SKU，**不经过** SKU 侧 `defaultPrepareDelete` 守卫；且 `IErpMdMaterialReferenceChecker` 同为零生产实现（物料删除本身无守卫）。不裁决此路径则新守卫可被「删物料」整体绕过（UC-MD-06② 违背），见 D5。
  - **聚合机制缺口已知**：同族 SPI 先例 `IErpMdEmployeeReferenceChecker` 生产实现落 finance（`module-finance/erp-fin-service/.../spi/ErpMdEmployeeReferenceCheckerImpl.java:34`，注册于 erp-fin-service `_vfs/erp/fin/beans/app-service.beans.xml:248-249`），其 javadoc :30-32 显式声明「**单实例注入，多域聚合需引入 List 收集器，归 Deferred**」。本行需四域（purchase/sales/inventory/manufacturing）实现 → 必须引入 List 收集器。**List setter 注入范式在仓内已验证**：`ErpB2bEdiRegistry.setProviders(List<IErpB2bEdiProvider>)`（module-b2b）+ `ErpCtSignatureProviderRegistry`（module-contract）；跨模块 bean 收集经 app-erp-all 运行时容器成立（fin checker 被 md BizModel @Nullable @Inject 收集为证）。
  - **跨域 skuId 引用面普查（4 域，quality/aps/logistics/contract/b2b/drp/assets/hr/crm/cs/finance/projects/maintenance/notify 零 skuId 列）**：purchase（OrderLine :627 / ReceiveLine :763 / ReturnLine :1105，均经 header docStatus）+ sales（OrderLine :397 / DeliveryLine :560 / ReturnLine :962 经 header docStatus；PriceListLine :1076 经 priceList.isActive+validFrom/validTo）+ inventory（StockBalance.skuId :374 + totalQuantity :378；StockLedger :293；StockMoveLine :230 经 move-status；ReservationLine :493 经 reservation-status + reservedQuantity :497；CostLayer :546 remainingQuantity :551；TransferOrderLine :663 / StockTakeLine :753 经 move-status；PickingOrderLine :854 经 picking-status；Batch :903 + status :910 + availableQuantity :906；SerialNumber :951 + status :954；OwnershipTransferLine :1074）+ manufacturing（BomLine :241 / BomByproduct :338 经 bom.isActive :202；WorkOrderLine :682 经 work-order-status；MaterialIssueLine :1190 经 issue-status）。
  - **物料级联既有**：物料 INACTIVE → `isMaterialActive`（:380-390）门控 findDefaultSku/resolveSku 返回 null（断言③已实现）；`ErpMdMaterialBizModel.onMaterialDeactivated`（:78-81）空扩展点。
- **Q4 判据**：§2 P1② 异常路径未实现 + 功能载体缺失；三判据复核均不成立（代码注记 :293 G2 + :378「Phase 3 Decision 选 (b)」+ SPI javadoc「下游接线归 Deferred」均 AI 自标；sku-multi-unit.md §SKU 启停字面正向需求；product-scope 未裁剪——**Q8 人工裁决已确认不裁剪**）→ Q4=(a) 强制实现。**2026-08-12 A 类批量裁决**：ErpMdMaterialSku 加 status 列 ORM 授权已批量批准（纯加性：可空无默认无索引无 UK）。
- **测试基线**：erp-md-service **26 测试类 / 146 @Test 全绿**；SKU 状态约束套件 `TestErpMdSkuStatusConstraints` 5 @Test（含 `testDeleteReferencedSkuRejected` 走测试桩非真实跨域检查）+ `TestErpMdSkuServices` 9 @Test。
- **compliance 基线**（§BASELINE 机器可读块）：R2b=235 / R2c=1439 / R2d=35 / R10=12 / R12a=70。四域 checker 均只查本域实体（daoFor 同域，fin employee checker 同型零 R2c 面）；预期零漂移。

## Goals

- **UC-MD-06 ①②④ + 独立停用运行时成立**：`ErpMdMaterialSku.status`（propId 19，dict `erp-md/active-status` 复用，可空无默认——null 派生 ACTIVE 兼容既有行）落地「SKU 独立停用」载体。
- **写侧守卫补全（全路径）**：`defaultPrepareUpdate` 检测 status→INACTIVE 迁移时调 `validateSkuDeactivation`（对齐 §SKU 状态校验 :280-282 停用前双校验）；`hasOtherActiveSku` 增 status 过滤（「可用」= 非 INACTIVE）；`defaultPrepareDelete` 既有接线核对不变；**物料级联删除旁路堵闭**（`ErpMdMaterialBizModel.defaultPrepareDelete` 对子 SKU 逐一校验——orm cascade-delete 不经 SKU 侧守卫的矛盾路径）。
- **IErpMdSkuReferenceChecker 四域生产实现 + List 收集器聚合**：purchase/sales/inventory/manufacturing 各自实现本域 checker bean（只查本域实体）；md 侧 `ErpMdMaterialSkuBizModel` 改为经 List 收集器聚合（任一 true → 拒绝），单域测试无 bean 时空集合零回归。
- **读侧新引用拦截**：`findSkuByBarcode`/`findDefaultSku`/`resolveSku` 跳过 INACTIVE SKU（对齐「SKU 独立停用 → 不可被新单据引用」，物料级既有 `isMaterialActive` 门控同层扩展）。
- **UI 可见性**：`ErpMdMaterialSku.view.xml`（delta bounded-merge）grid 增 status 列 + 启用/停用操作可达（标准 CRUD update 即可，不新增专用 mutation）。
- **测试补强**：md 域 status/守卫/读侧过滤测试 + 四域各自 checker 单测（真实本域实体引用构造）+ 146 基线零回归 + 全量构建 + checker 零漂移。
- **owner doc 收敛**：sku-multi-unit.md §SKU 状态管理实现注记 + README.md + arm-index P1-RC-062 → done + roadmap 行 done + 行标签改写 + logs 条目。

## Non-Goals

- **不做下游行级 save-time SKU 状态校验**（23 surface × 4 域行级接线超出本行修复指令；md 解析链读侧过滤 + 删除/停用守卫满足 L1 断言主语义；行级直填拦截登记 successor，触发条件 = 下游行级守卫需求立项）。
- **不做物料级自身引用检查生产实现**（`IErpMdMaterialReferenceChecker` 为不同 SPI/不同 finding——物料被引用拒删属物料维度守卫，本行仅经 D5 堵 SKU 级联旁路；触发条件 = 物料维度引用检查需求立项，与既有 TestStubMaterialReferenceChecker 测试桩共存）。
- **不增设 barcode / (materialId,isDefault) DB UK**（P2-RC-058/P2-RC-059 watch-only 独立项，不在本行）。
- **不做 SKU 级联启停批量操作 / 生命周期历史实体**（L1 未要求）。
- **不改既有软删语义**（断言④ 历史完整经 useLogicalDelete 既有行为保持）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/master-data/sku-multi-unit.md`（§SKU 状态管理/§SKU 启停/§SKU 状态校验）+ `docs/design/master-data/use-cases.md` UC-MD-06（L1 正文不动）+ `docs/design/master-data/README.md`
- Skill Selection Basis: ORM 加列 + 增量重生成（平台规则：`mvn clean install -DskipTests`，勿重跑 nop-cli gen）；BizModel/SPI（`nop-backend-dev`）；测试（`nop-testing`）。

## Infrastructure And Config Prereqs

- 无新 config 键、无新 job、无新 seed 模板、无数据迁移（status null=ACTIVE 派生兼容零迁移）。
- ORM：`ErpMdMaterialSku.status` propId 19（VARCHAR 20，可空无默认无索引无 UK，ext:dict 复用 `erp-md/active-status` 既有 dict——零新 dict yaml）——`mvn clean install -DskipTests` 增量重生成。
- 四域 checker bean 注册：各域 `app-service.beans.xml`（fin employee checker 先例位）。

## Execution Plan

### Phase 1 - ORM status 列 + md 域内停用语义与守卫

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`（ErpMdMaterialSku.status）、`ErpMdMaterialSkuBizModel.java`、`ErpMdMaterialBizModel.java`（D5 级联删除守卫）、`module-master-data/erp-md-web/.../ErpMdMaterialSku.view.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无

- [x] **双独立子 agent 批准（保护区域 checkbox）**：ORM 纯加性 status 列（Q3/A 类批量授权范围内）+ 数据删除守卫路径变更，按 R1.49/51/56-58/60/66 先例取得两个独立子 agent（fresh session）分别检查批准，批准记录落盘本计划。
      - Skill: none
      - **批准记录（2026-08-19）**：Reviewer 1（task `ses_fe93c4124ffegZ1TP2Aof0ocHa`，fresh session）→ **APPROVE**（证据链：Draft Review 3 迭代可接受 / roadmap :41 批量授权在案 / propId 19 空闲 + dict 既有 + C2 可空加列先例 / 级联旁路真实存在 / 守卫仅 reject 不扩删 / 无 auth/API 面变化；残留风险均接受：null 语义需 D1 四路径一致、物料级 checker 归 Deferred、Phase 1 守卫 2 桩语义不变）。Reviewer 2（task `ses_fe93c1427ffezM91qPOPq3KP2g`，fresh session）→ **APPROVE**（独立复核：授权范围精确匹配 / 146 基线测试无一设置 SKU status 故迁移检测零误触 / 无依赖环（IErpMdMaterialSkuBiz 不引用 IErpMdMaterialBiz）/ 守卫 1 整体删除豁免裁决正确 + Proof ⑦ 负控覆盖 / done 回写时行标签改写义务已登记）。两批准均通过，实施解锁。
- [x] **D1 status 列语义**：null=ACTIVE（派生兼容，存量行零迁移）；显式 ACTIVE/INACTIVE。`hasOtherActiveSku` 过滤条件 = 同物料 + id 不同 + status≠INACTIVE；`findSkuByBarcode`/`findDefaultSku`/`resolveSku` 跳过 INACTIVE（与 `isMaterialActive` 同层短路）。
      - Skill: `nop-backend-dev`
- [x] **D2 停用守卫接线**：`defaultPrepareUpdate` 检测 status ACTIVE→INACTIVE 迁移（null 视同 ACTIVE）→ 调 `validateSkuDeactivation`（守卫 1 默认 SKU 接替 + 守卫 2 引用检查）；非停用迁移不触发（防止改码/改名误伤）。`defaultPrepareDelete` 既有调用不变（守卫 2 经 Phase 2 真实化）。
      - Skill: `nop-backend-dev`
      - 迁移检测经 `orm_propOldValueByName("status")`（未修改返回当前值、修改返回旧值，null 旧值视同 ACTIVE）。
- [x] **D5 物料级联删除守卫（堵旁路，仅引用守卫）**：`ErpMdMaterialBizModel` 增 `defaultPrepareDelete` 覆写——**经注入 `IErpMdMaterialSkuBiz` 委托其校验路径**（非自行注入 checker——Phase 2 List 聚合升级落单一改造点即自动覆盖物料删除路径，防聚合面分叉），删除物料前对其全部子 SKU 逐一执行**引用检查**（任一被引用 → `ERR_SKU_REFERENCED_BY_BILL`，错误信息携带阻断 SKU 标识）；**守卫 1（默认 SKU 唯一性）在物料整体删除语境下显式不适用**——「必须先设其他默认」约束保护的是存活物料的默认 SKU 可解析性，物料连同全部 SKU 一起删除时该约束无意义，逐 SKU 全量套用将使单默认 SKU 物料（最常见形态）永久不可删且错误信息误导（迭代 2 审查 BLOCKER）。替代方案：物料删除时子 SKU 全量经 validateSkuDeactivation（否决——守卫 1 误伤如上）；仅文档登记旁路（违背 UC-MD-06②「全路径删除约束」，否决）；物料级自身引用检查（`IErpMdMaterialReferenceChecker` 生产实现）不在本行范围（不同 SPI/不同 finding——本项仅堵 SKU 级联旁路，见 Non-Goals）。
      - Skill: `nop-backend-dev`
      - **执行期事实修正（live-repo evidence）**：基线断言「orm cascade-delete 不经 SKU 侧守卫」在 CrudBizModel 层不成立——`CrudBizModel.deleteReferences`（nop-biz `CrudBizModel.java:1217-1265`）对 cascade-delete 子对象**逐一经子 BizModel `__delete` 级联**（父物料先 `dao().deleteEntity` 标记删除再级联），子 SKU 删除会进入自身 `defaultPrepareDelete` → 守卫 1。为实现迭代 2 裁决（整体删除守卫 1 豁免），守卫 1 增父物料会话态豁免：`isMaterialBeingDeleted`（`sku.getMaterial().orm_state().isGone()`——DELETING/DELETED/MISSING）时跳过守卫 1（引用检查仍执行）；物料级引用拦截仍由 `ErpMdMaterialBizModel.defaultPrepareDelete` 前置承担（fail-fast + 物料级错误归因）。Proof ⑥⑦ 双向覆盖该行为。
- [x] **Proof**：TestErpMdSkuStatusConstraints 扩展：①status=INACTIVE 默认唯一 SKU 停用拒绝 ②非默认 SKU 停用成功 ③停用后 findDefaultSku/resolveSku/findSkuByBarcode 跳过 ④null status 派生 ACTIVE 兼容 ⑤status→INACTIVE 迁移触发守卫、ACTIVE↔ACTIVE 不触发 ⑥删除含被引用 SKU 的物料被拒绝（级联旁路闭合，错误信息含阻断 SKU）⑦**负控：无引用的单默认 SKU 物料删除成功**（守卫 1 不误伤整体删除）+ `_cases/` 快照处理；146 基线零回归。验证命令：`mvn test -pl module-master-data/erp-md-service`。
      - Skill: `nop-testing`
      - 实测：12/12（5 旧 + 7 新）全绿；全量 erp-md-service **153 @Test**（146 基线 + 7 新增）零失败零回归；`_cases` 快照已录制（新 7 方法 + 旧 5 方法 CSV 头补 STATUS 列，值全空——null 派生 ACTIVE 兼容）。

Exit Criteria:

- [x] status 列 + 停用/读侧语义落地，分域测试绿（桩 checker 下守卫 2 行为不变）

### Phase 2 - 四域 checker 生产实现 + md List 收集器聚合

Status: completed
Targets: `module-purchase/erp-pur-service/.../spi/ErpPurSkuReferenceChecker.java`（新）、`module-sales/erp-sal-service/.../spi/ErpSalSkuReferenceChecker.java`（新）、`module-inventory/erp-inv-service/.../spi/ErpInvSkuReferenceChecker.java`（新）、`module-manufacturing/erp-mfg-service/.../spi/ErpMfgSkuReferenceChecker.java`（新）、各域 `app-service.beans.xml`、`ErpMdMaterialSkuBizModel.java`（聚合改造）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1

- [x] **D3 活跃引用口径表（Decision，落计划 + sku-multi-unit.md 引用注记）**：「被业务单据引用」= 开放单据行 + 在手量 + 活跃配置三类，逐 surface 判定：
  - purchase：OrderLine/ReceiveLine/ReturnLine 经 header docStatus ≠ CANCELLED；
  - sales：OrderLine/DeliveryLine/ReturnLine 经 header docStatus ≠ CANCELLED + PriceListLine 经 priceList.isActive=true 且 validTo ≥ 当日（过期价目表不阻断）；
  - inventory：StockBalance.totalQuantity ≠ 0 + ReservationLine 经 header status ∈ {OPEN, PARTIALLY_CONSUMED} 且 reservedQuantity>0 + CostLayer.remainingQuantity>0 + Batch.status=OPEN（:910）且 availableQuantity>0（:906）+ SerialNumber.status ∈ {IN_STOCK, RESERVED}（:954）+ StockMoveLine/TransferOrderLine/StockTakeLine/OwnershipTransferLine 经 header docStatus ∈ {DRAFT, CONFIRMED} + PickingOrderLine 经 status ∈ {PENDING, PICKING}（StockLedger 不可变历史不阻断——断言④ 历史完整）；
  - manufacturing：BomLine/BomByproduct 经 bom.isActive=true + WorkOrderLine 经 docStatus ∉ 终态 {CLOSED, CANCELLED} + MaterialIssueLine 经 docStatus ∈ {DRAFT, CONFIRMED}。
  替代方案：仅计「未完成单据」（窄口径，漏库存在手量→删除后余额行孤儿）；仅计在手量（漏开放订单→新单引用已删 SKU）。残留风险：口径偏保守（活跃价目表/开放 BOM 阻断删除）——停用可用，符合「只能停用」语义。
      - Skill: `nop-backend-dev`
      - 实现落位：四域 checker `isReferencedByBill` 逐 surface 方法；口径注记回填 sku-multi-unit.md 归 Phase 3 owner-doc 项。PriceListLine validTo 为空（不限期）按活跃处理；inv SerialNumber OUT/BLOCKED 不阻断、Batch 仅 OPEN 且 availableQuantity>0 阻断；mfg WorkOrderLine 终态双 ne（CLOSED/CANCELLED）。
- [x] **D4 聚合机制 = List 收集器**：md 侧新增聚合收集（`ErpMdMaterialSkuBizModel` 注入 `List<IErpMdSkuReferenceChecker>` 或 Registry bean，`ErpB2bEdiRegistry.setProviders` setter 注入范式）；`validateSkuDeactivation` 守卫 2 改为「任一实现 isReferencedByBill=true → ERR_SKU_REFERENCED_BY_BILL」；单域测试容器无下游 bean → 空集合跳过（对齐 fin checker 单域测试行为，TestStub 仍可用）。替代方案：单一聚合实现落某一下游域（需跨域 daoFor，违反 R2c/边界矩阵，否决）；bean 容器按类型运行时枚举（IoC API 面不确定，List 注入已有仓内先例，不采）。
      - Skill: `nop-backend-dev`
      - 实现落位：`ErpMdSkuReferenceCheckerRegistry`（md-service spi，setter 收集 + OR 聚合）注册于 md `app-service.beans.xml`（`ioc:collect-beans by-type` + `only-concrete-classes` + `ioc:ignore-depends`，镜像 b2b ErpB2bEdiRegistry）；BizModel `skuReferenceChecker` 单实例注入改 `skuReferenceCheckerRegistry`；守卫 2 落 `checkSkuReferenced` 单点（validateSkuDeactivation / validateSkuReference / 物料级联路径共享）。执行教训：beans.xml 使用 `ioc:` 前缀须在根元素声明 `xmlns:ioc="ioc"`（md app-service.beans.xml 原未声明，首次插入致整文件解析失败 → TestErpMdReportRendering bean 缺失，补声明后修复）。
- [x] **四域 checker 实现**：各域只查本域实体（daoFor 同域，fin employee checker 同型）；exists 判定用 limit 1 查询非全量加载；header 状态过滤经关联属性或两步 id 集查询（执行期按 XMeta 过滤算子白名单择型，实际选择记录于收口证据）。
      - Skill: `nop-backend-dev`
      - 择型记录：header 状态过滤选**关联属性路径**（`order.docStatus` 等，DAO 层 `findAllByQuery` 查询翻译，仓内先例 `BankLedgerQuery.eq("statement.fundAccountId")`——checker 走 DAO 直查不经 CrudBizModel 管道，无 objMeta 过滤算子白名单约束；to-one 路径无行膨胀）；未采两步 id 集查询（多一轮全量 header 扫描）。exists = `q.setLimit(1)` + `!findAllByQuery(q).isEmpty()`（仓内百+ 处 `setLimit(1)` 同型）。
- [x] **Proof**：每域新增 checker 单测（真实本域实体构造开放/取消/终态对照：开放引用→true / CANCELLED·终态·过期→false 各至少 1 断言）+ md 侧聚合测试（TestStub 多实例 OR 语义 + 空集合放行）。验证命令：`mvn test -pl module-purchase/erp-pur-service -pl module-sales/erp-sal-service -pl module-inventory/erp-inv-service -pl module-manufacturing/erp-mfg-service -pl module-master-data/erp-md-service`。
      - Skill: `nop-testing`
      - 实测：pur 4/4（开放 true / CANCELLED false / 收退双面 / 未引用 false）、sal 4/4（含价目表活跃 true·过期 false·停用 false）、inv 4/4（在手 5→true 零→false / move DRAFT true·DONE·CANCELLED false / 序列 IN_STOCK true·OUT false）、mfg 4/4（BOM 活跃 true·停用 false / 工单 IN_PROCESS true·CLOSED·CANCELLED false / 领料 DRAFT true·DONE false）、md 聚合 2/2（双桩 OR 对称命中 + 空收集器放行）；五域 reactor 全绿（master-data SUCCESS / inventory SUCCESS / purchase SUCCESS / sales SUCCESS / manufacturing SUCCESS，md 155 @Test 含 Phase 1 全部）。

Exit Criteria:

- [x] 四域 checker + 聚合落地，五域分域测试全绿零回归

### Phase 3 - 验证收口 + 文档回填

Status: completed
Targets: `docs/design/master-data/sku-multi-unit.md`、`docs/design/master-data/README.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-19.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1-2 全绿

- [x] 全量验证：`mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（预期零漂移：四域 checker 同域 daoFor + md List 注入零新增跨域面；若计为新站点则 baseline-raise per-site 证据）。
      - Skill: none
      - 实测：全仓 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；checker 19 规则——18 规则零漂移（R1d=14/R2a=34/R2b=235/R2d=35/R3=5/R10=12/R12a=70/R12b=66/R12c=40 持平），**R2c 1439→1460（+21）按计划预告路径 baseline-raise**（21 per-site 证据 = 四域 checker 同域 daoFor 站点，BASELINE 块 + 注记落 `docs/audits/compliance-baseline.md`）；追加全仓 `mvn test` BUILD SUCCESS（156 模块全绿，含 app-erp-all 页面校验组——R1.68/69 教训）。**过程记录**：一次全仓 test 中 TestErpMdSkuServices 出现 9×`unknown-operation`（同 JVM 前后邻居类全绿），后续 4 种范围复跑（模块单独 ×3 / 五域 reactor / `-am` / 全仓重跑）均不可复现且全仓重跑全绿——判定为 clean 后 VFS 组件缓存构建竞态的一次性环境毛刺，非代码回归（同 JVM 邻居类同 RPC 全绿排除注册缺失）。
- [x] owner doc 回填：sku-multi-unit.md §SKU 状态管理实现注记（status 列/D3 口径表/List 收集器）+ README.md :126 注记实现位 + arm-index P1-RC-062 → done (RC-R1.72) + roadmap 行 done + 行标签 A 类改写 + logs 条目（全绿验证状态）。
      - Skill: none

Exit Criteria:

- [x] 五处回填一致（代码 / sku-multi-unit.md / arm-index / roadmap / logs）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（task `ses_fe9522b83ffe6NEHQL167sThTC`，2026-08-19）——BLOCKER-1：ErpMdMaterial.skus cascade-delete（orm.xml:244-246）+ ErpMdMaterialBizModel 无 defaultPrepareDelete 覆写 → 删物料绕过全部 SKU 守卫（UC-MD-06② 全路径违背）；MINORS：普查子字段行号漂移（Batch/SerialNumber）/ arm-index 快照行号 / fin checker javadoc 行号 / 择型记录义务。其余维度（基线事实/授权/scope/List 聚合先例/L1 对齐/测试/反松弛）全 PASS。
- Independent draft review iteration 2: needs-revision（task `ses_fe9483ba8ffeHgKF5mpdG8wwf5`，2026-08-19）——BLOCKER-1：D5 原稿对物料删除逐 SKU 全量套用 validateSkuDeactivation，守卫 1（默认 SKU 唯一性）在物料整体删除语境不适用 → 单默认 SKU 物料（最常见形态）永久不可删且错误误导；MINORS：Phase 1 Targets 未列 ErpMdMaterialBizModel.java / Baseline 普查行号未同步。迭代 1 修订项（级联旁路基线盘点 + D5 + Proof ⑥ + Goals/Non-Goals + 各 MINOR）全部落地确认；D5 可行性（无既有 defaultPrepareDelete / CrudBizModel 覆写先例 / IErpMdMaterialSkuBiz 注入无环 / countReferences 仅 RPC 预览无冲突 / Proof 可加于既有测试类）核实通过。
- Independent draft review iteration 3: acceptable（task `ses_fe944f2d9ffeDngPxI0Ve8nSle`，2026-08-19）——零 BLOCKER；迭代 2 修订全部落地确认（D5 仅引用守卫 + 守卫 1 不适用裁决 + Proof ⑦ 负控 / Targets 补列 / 行号同步与 D3 互证一致 / 桩 checker 下 Phase 1 可测性核实）；MINOR 1 项已采纳：D5 补「经注入 IErpMdMaterialSkuBiz 委托其校验路径」条款（Phase 2 聚合升级单一改造点自动覆盖物料删除路径）。最终扫描（item typing/Skill 注解/退出标准/Closure Gates/Deferred 触发条件/头元数据）清洁。**共识达成，计划转 active。**

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn clean install -DskipTests` + 分域 `mvn test` + compliance checker）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 下游行级 save-time SKU 状态校验

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 断言主语义（删除/停用约束 + 停用后不可经 md 解析链被新单引用）经本计划守卫 + 读侧过滤满足；行级直填 skuId 拦截面（23 surface × 4 域）超出 arm-index 修复指令（「status 列 + validateSkuDeactivation + checker 生产实现 + defaultPrepareDelete/Update 接线」）。
- Successor Required: yes（触发条件：下游行级守卫需求立项 / 行级直填误用事件出现）

## Closure

Status Note: 三阶段全部完成且独立结束审计通过——范围内行为（status 列 + 全路径停用/删除守卫 + 读侧过滤 + 四域 checker + List 收集器聚合 + 级联旁路堵闭）经 live-repo 逐点核验落地；五域测试 + 全仓构建 + compliance checker 全绿（R2c baseline-raise per-site 证据在案）；五处文档回填一致。计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，fresh session，mission-driver 2026-08-17-212541 流水线指派，非执行者会话）
- Evidence: live-repo 逐点核验通过——①ORM `app-erp-master-data.orm.xml:398` ErpMdMaterialSku.status propId 19（可空无默认，dict 复用 erp-md/active-status）；②`ErpMdMaterialSkuBizModel`：`skuReferenceCheckerRegistry` 注入（:75-77）+ `defaultPrepareUpdate` ACTIVE→INACTIVE 迁移检测调 `validateSkuDeactivation`（:282-296）+ `hasOtherActiveSku`/`isSkuActive` status≠INACTIVE 过滤（:349-371）+ `findSkuByBarcode`/`findDefaultSku`/`resolveSku` INACTIVE 跳过（:87-147）+ `checkSkuReferenced` 单点（:266-271）+ 守卫 1 整体删除豁免 `isMaterialBeingDeleted`（:377-380）；③`ErpMdMaterialBizModel.defaultPrepareDelete:137-143` 经 `IErpMdMaterialSkuBiz.validateSkuReference` 逐子 SKU 仅引用检查（D5）；④四域 checker 生产实现存在且各自注册于本域 `app-service.beans.xml`（pur/sal/inv/mfg），md `ErpMdSkuReferenceCheckerRegistry` 经 `ioc:collect-beans by-type` 注册（md beans.xml:21-24）——无空实现/空转（任一 isReferencedByBill=true → ERR_SKU_REFERENCED_BY_BILL）；⑤测试落地：`TestErpPurSkuReferenceChecker`/`TestErpSalSkuReferenceChecker`/`TestErpInvSkuReferenceChecker`/`TestErpMfgSkuReferenceChecker`（各 4 @Test 开放/终态对照）+ md `TestErpMdSkuReferenceAggregationMulti`/`Empty`（OR 语义 + 空集合放行）+ `TestErpMdSkuStatusConstraints` 12 @Test；⑥文档五处回填一致：`sku-multi-unit.md:284` §实现注记（RC-R1.72）+ `README.md:126` + `arm-index.md:240` P1-RC-062 → done (RC-R1.72) + `requirement-compliance-roadmap.md:464` RC-R1.72 done + A 类行标签改写 + `docs/logs/2026/08-19.md` 全绿验证条目（R2c 1439→1460 baseline-raise per-site 证据 = compliance-baseline.md）；⑦双独立子 agent 批准记录在案（Phase 1 item :69）。文本一致性：Plan Status=completed / 三阶段 Status=completed / 全部 Exit Criteria [x] / Closure Gates 全 [x] / 日志条目一致。
- Verification Commands: `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ 五域 `mvn test`（md 155 / pur·sal·inv·mfg checker 16 + 聚合 2 全绿）+ `bash docs/audits/nop-compliance-checker.sh`（18 规则零漂移 + R2c baseline-raise）

Follow-up:

- 无已确认缺陷；下游行级校验归 Deferred But Adjudicated successor。

## Post-Closure Verification Addendum（2026-08-19 结束审计反馈再执行）

结束审计反馈 4 项核验均已在案（22 checked / 0 unchecked / Plan Status=completed / 五处文档回填 live-repo 复核一致），未新增未勾选项。再验证（同日）发现并修复**测试基础设施秩序脆弱性**（非 RC-R1.72 语义回归，范围内行为与证据不变）：

- **现象**：md 全量 `mvn test`（clean 后仍确定性复现）`TestErpMdSupplierApprovalStateMachineDeltaOverride` 2 失败 + `TestErpMdCurrencyRefreshRatesDate` 1 错误；五域 reactor 中 mfg `TestErpMfgForecastStateMachineDeltaOverride` 同型 2 失败。RC-R1.72 新增测试类改变 surefire filesystem 类秩序为触发条件。
- **根因**：(a) 平台 `VfsConfigLoader._default`（@GlobalInstance 静态缓存）+ ConfigStarter 同 JVM 前置容器类 ≥5 时跳过 VFS 重建（二分实证：任意 5 类前缀失败 / 4 类通过 / 与具体类无关）→ delta 层失效拿基线 bean；(b) `ErpMdExchangeRateApiClientFactory.limiters` 令牌桶跨类残留（rate 绑定创建时配置、resetStats 不清桶）→ 冻结时钟零超时 tryAcquire 被残留时间戳误伤。
- **修复**（镜像 cs RC-R1.67 Phase 3 先例，测试作用域零产品行为）：①`resetTestState()` 增 `limiters.clear()`；②aps/b2b/ct/crm/drp/hr/mfg/md/prj 9 模块 pom 增 surefire 覆写 `forkCount=4 + reuseForks=false + parallel=classes`（每测试类独立 JVM；cs 已有同款）。
- **判定修正**：Phase 3 所记 12:42 全仓跑 9×unknown-operation「一次性环境毛刺」与本发现同根因族（VFS/组件静态缓存跨类残留），fork 隔离后确定性消除。
- **再验证全绿**：md 155/0/0 ×2（确定性）+ mfg 286/0/0 + 全仓 `mvn test` 156 模块 BUILD SUCCESS（12:31）+ 全仓 `mvn clean install -DskipTests` BUILD SUCCESS + checker 零漂移（R2c=1460 = 已登记 baseline-raise，其余持平）。详见 `docs/logs/2026/08-19.md` 当日条目。
