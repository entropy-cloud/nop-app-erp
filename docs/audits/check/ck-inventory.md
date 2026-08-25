# ck-inventory — inventory 实现代码检查报告

> 工作项：C2.3。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-inventory/erp-inv-service/src/main/java` 全部 90 个手写生产文件（service 根 3、costing/ 16、processor/ 21、posting/ 11、entity/ 21、statemachine/ 6、dashboard/ 1、report/ 1、spi/ 1、stock/ 1、trace/ 1、metrics/ 1）+ `app-service.beans.xml` + `model/app-erp-inventory.orm.xml`（versionProp/dict/列核对）+ xmeta/xbiz 抽查（StockLedger 写禁用核对）。api/web 骨架与 dao 层生成物不深查。
> 方法：Skill: `code-quality-audit-prompt`（发现骨架 + P0-P3 分级）+ `behavioral-failure-mode-scan-prompt`（B1/B2/B3 grep 程式）。机械扫描（D1 全项 grep）+ 核心链逐文件深读（StockMoveBookkeeper / 7 个 CostingStrategy 全读 / StockMove 5 Processor / StockTake CompleteTake / Reservation BizModel / OwnershipTransfer / LandedCost 全链 / CostAdjust 全链 / RecloseProcessor / TraceChainQuery / Dashboard / posting 包 11 类全读）+ 跨域核实（finance `ErpFinDeferredPostingRetryHelper` 事件重建、purchase `ReceiveStockMoveBuilder`/`ReturnStockMoveBuilder` location/currency 传参、sales `DeliveryStockMoveBuilder` 传参）+ 平台 API 语义验证（nop-entropy `IOrmEntityDao.tryUpdateWithVersionCheck`→`updateDirectly`→`flushUpdate(单实体)`、`FilterBeans.eq(name,null)`→`SQL.SqlBuilder.eqEx`→`IS NULL`）。
> D4：module-inventory 无 `*.batch.xml`/job 配置（job-scheduling.md §3.3 声明 stock-check 为 Deferred）→ 调度链维度除死配置键（P3-CK-inv-021 一部分）外 **N/A**。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-inv-001（D6/D8）4 个出库策略 locationId 回退误用 `move.getSourceWarehouseId()`——仓库 ID 写入库位列，余额维度污染

- **控制点**：`erp-inv-service/src/main/java/app/erp/inv/service/costing/WeightedAverageCostingStrategy.java#onOutgoing`（L74-75）、`BatchCostingStrategy.java#onOutgoing`（L85-86）、`LifoCostingStrategy.java#onOutgoing`（L82-83）、`SpecificCostingStrategy.java#onOutgoing`（L81-82）
- **证据**：四处同型 copy-paste：
  ```java
  String locationId = line.getSourceLocationId() != null ? line.getSourceLocationId()
          : move.getSourceWarehouseId();   // ← 回退值是 warehouseId，非 move.getSourceLocationId()
  ```
  对照正确写法：`MovingAverageCostingStrategy#onOutgoing`（L66-67）与 `FifoCostingStrategy#onOutgoing`（L90-91）回退 `move.getSourceLocationId()`。该 locationId 随后进入 `ctx.upsertBalance(...)`（余额自然键 locationId 维）与 `ctx.writeLedger(...)`（流水 locationId 列）。
- **触发条件**：行级与头级 sourceLocationId 均为 null 的出库移动单——**sales 出库主路径必然触发**：`module-sales/.../entity/DeliveryStockMoveBuilder.java#build`（L33-34）只设 `setSourceWarehouseId(delivery.getWarehouseId())`，不设 sourceLocationId；行构造（L57-69）亦不设。
- **问题**：WEIGHTED_AVERAGE/BATCH/LIFO/SPECIFIC 计价物料的销售出库（及一切无库位出库）把 warehouseId 值持久化进 `erp_inv_stock_balance.location_id`/`erp_inv_stock_ledger.location_id`（ORM to-one 逻辑关联 `location`→ErpMdLocation 无 DB FK，静默落库）。后果：(1) 余额行维度被污染（locationId 指向仓库记录）；(2) 同物料同仓出现「入库行 locationId=null + 出库行 locationId=warehouseId」两行——出入库落不同余额行，库存数量分裂，可用量校验失真；(3) 流水 locationId 不可信，追溯/报表按库位聚合错乱。
- **建议修复方向**：四处回退统一改为 `move.getSourceLocationId()`（与 MovingAverage/Fifo 对齐）；修复阶段补一条「无库位出库 → 余额行 locationId=null」的回归测试。
- **arm-index 裁决**：新增（grep `locationId`/`warehouseId 回退`/`维度污染` 零命中；A1.25/A1.27 inventory 切片均未涉及）。

### P1-CK-inv-002（D6/D8）`upsertBalance/findBalance` 查询键与 UK 自然键不一致——skuId 永不过滤、null 维度不做 IS NULL 匹配，余额行错配写入

- **控制点**：`erp-inv-service/src/main/java/app/erp/inv/service/stock/StockMoveBookkeeper.java#findBalance`（L452-471）、`ErpInvReservationBizModel.java#findBalance`（L410-428，javadoc 自认「镜像 findBalance 语义：skuId 不参与过滤」）、`CostAdjustmentService.java#findBalance`（L274-285，另不过滤 locationId）
- **证据**：`StockMoveBookkeeper#findBalance` 接收 `skuId` 参数但**查询构造从不使用**（L454-468 仅 orgId/materialId/warehouseId/locationId(非空才加)/batchNo(非空才加)/owner(开关)）；`locationId==null`/`batchNo==null` 时**直接跳过过滤**（非 `isNull`），返回该 (org,mat,wh) 下任意首行。对照同文件 `findBalanceByNaturalKey`（L350-379）——重试路径精确匹配 skuId + nullable 列 `isNull()`，与 UK `UK_INV_STOCK_BALANCE_NATURAL(orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId)` 完全对齐。两个查找路径键语义互相矛盾即为实证。
- **问题**：主记账路径（`upsertBalance` L148-159 → `findBalance`）可能返回与请求自然键**不同**的余额行：lookup (loc=null,batch=null) 会命中带 loc/batch 的既有行（`findAllByQuery().get(0)` 任意序取首行），随后可用量校验、预留量增减、数量/成本 delta 全部落到错误维度行；ledger 结存快照（`balanceQuantity/balanceTotalCost`）记录错行结存。触发面真实存在：purchase `ReceiveStockMoveBuilder`（L25-45）不设 destLocationId → 采购入库全部走 (loc=null) lookup；一旦同物料同仓存在带库位行（手工移动单/盘点差异单/内部调拨生成），即发生错配。P0-MA2-020 的 UK 只防重复 INSERT，不防**查错行**。
- **建议修复方向**：`findBalance` 与 `findBalanceByNaturalKey` 统一为同一实现（skuId 过滤 + nullable 列 `isNull()` 精确匹配）；三处镜像调用方同步切换。修复阶段先写「入库无库位 + 既有带库位行」复现测试。
- **arm-index 裁决**：新增（P0-MA2-020 是 UK 缺失→并发重复 INSERT，不同控制点——本条是查询键语义错，UK 在位也不防护）。

### P1-CK-inv-003（D3/D5）「库存流水不可变 / 余额由流水驱动」两条核心规则在 CRUD 层零强制——Ledger/Balance 可经通用 mutation 直接改删

- **控制点**：`erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockLedgerBizModel.java`（15 行裸 `CrudBizModel`）、`ErpInvStockBalanceBizModel.java`（同）；`erp-inv-meta/.../ErpInvStockLedger/ErpInvStockLedger.xmeta`（4 行空 extends，无写禁用）+ `erp-inv-service/.../ErpInvStockLedger.xbiz`（空 `<actions/>`）
- **证据**：owner doc `docs/design/inventory/README.md §关键业务规则` 1「库存流水不可变：一旦写入不可修改，冲销走反向流水」、2「余额由流水驱动：余额表不是独立维护」。实现层 Ledger/Balance BizModel 为零覆写 CRUD 桩，xmeta/xbiz 无任何 `updatability`/mutation 收敛 → 平台自动生成 `ErpInvStockLedger__update/__delete/__save`、`ErpInvStockBalance__update/__save/__delete` 等通用写操作，任何有权限用户可直接 UPDATE/DELETE 不可变流水、或绕过流水直改余额（含 `totalQuantity/reservedQuantity/availableQuantity`，无任何校验或流水留痕）。
- **问题**：域内两条最高优先不变量（审计链不可变 + 余额可追溯）完全依赖前端不暴露入口（UI 掩盖不豁免后端）。冲销闭环（`ErpInvStockMoveReverseProcessor`）和记账器（`StockMoveBookkeeper`）建得再严密，也可被一条 GraphQL mutation 绕过。
- **建议修复方向**：Ledger BizModel 覆写 `defaultPrepareUpdate/defaultPrepareDelete`（及 save 路径）抛 `NopException`（或 xmeta 层删除 update/delete mutation，只留 find/page）；Balance 同理仅保留查询 + 受控写方法（预留/记账路径已是专用方法）。属代码层修复（BizModel 覆写），不动 ORM。
- **arm-index 裁决**：新增。**同型强化**：P1-CK-pur-003/P1-CK-sal-004 是「已审/已过账单据可改」；本条是**规则级不可变对象**可改删，控制点更强（全部行，不限状态）。

### P1-CK-inv-004（D3/D5）owner doc §4「批次/序列号缺失拒绝确认」未实现——批次管控物料无批号出库被跳过放行；序列号「未售校验」全缺

- **控制点**：`erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveProcessor.java#validateBatchExpiry`（L178-180）
- **证据**：`if (StringHelper.isBlank(line.getBatchNo())) { continue; }`——批次管控物料（`material.isBatchManaged=true`）行**无批号直接跳过**（非拒绝）。owner doc `docs/design/inventory/state-machine.md §4 异常路径表` 逐字：「批次/序列号缺失：启用批次/序列号的物料，移动单必须指定批次/序列号；**缺失拒绝确认**」；`README.md §关键业务规则` 6 同（含「出库校验批次在库、序列号未售」）。序列号侧：全 `erp-inv-service` grep 出库链 `serialNo` 校验零命中（`validateBatchExpiry/validateAvailable` 均不触及 serialNo；`ErpInvSerialNumberBizModel` 为 15 行 CRUD 桩，无 IN_STOCK→OUT 状态翻转 writer，`ErpInvSkuReferenceChecker` 只读）。「批次在库」校验同样缺失——`findBatch` 返回 null（批号不存在）也 `continue` 放行。
- **问题**：批次管控物料出库不填批号 → 确认成功 → 流水/余额 batchNo=null → 批次追溯链（UC-INV-06 前置）静默断裂；已售序列号可再次出库；不存在批号可出库。RC-R1.20 只落地了「过期批次拒绝」这一子句。
- **建议修复方向**：`validateBatchExpiry` 之前加 per-line 守卫：`isBatchManaged=true && isBlank(batchNo)` → 抛 NopException（新增错误码）；`batchNo` 非空但 `findBatch==null` → 拒绝；序列号管控物料（需 md 侧 isSerialManaged 字段核对）无 serialNo 或序列号状态非在库 → 拒绝。
- **arm-index 裁决**：新增（P1-RC-031/RC-R1.20 是效期拦截缺失→已修；「缺失拒绝」与「在库/未售校验」是同表不同子句、不同控制点，arm-index 零覆盖）。

### P1-CK-inv-005（D5/D3）通用 CRUD update 无状态守卫——同型 P1-CK-pur-003/P1-CK-sal-004（不复用展开）

- **控制点**：`ErpInvStockMoveBizModel`（仅覆写域动作，`defaultPrepareUpdate` 无守卫）、`ErpInvStockMoveLineBizModel`/`ErpInvStockTakeLineBizModel`/`ErpInvTransferOrderLineBizModel`/`ErpInvOwnershipTransferLineBizModel`/`ErpInvReservationLineBizModel` 等（全部裸 CrudBizModel，见 §剩余风险清单）
- **证据**：`ErpInvStockMove__update`/`ErpInvStockMoveLine__update` 等 mutation 无 docStatus/posted 拦截——DONE（已写不可变流水、已过账）移动单的头字段（relatedBillCode/remark）与**行数量/单价**可直接改写，导致已写流水的 `quantity`/`unitCost` 与行数据漂移（流水不可变、行可变，事后对账失真）。
- **问题/修复**：同 purchase 裁决（P1-CK-pur-003）——头/行 `defaultPrepareUpdate` 加 `docStatus ∈ {DRAFT}（CONFIRMED 视域动作语义）` 守卫；与 P1-CK-inv-003 一并设计统一「不可变/终态禁改」拦截。
- **arm-index 裁决**：**同型复用 P1-CK-pur-003/P1-CK-sal-004**（全域同型模式，独立站点独立修复，状态不继承）。

### P2-CK-inv-006（D6/D10）OwnershipTransfer `reclassifyBalance` 除法无 scale——非整除即 ArithmeticException，VMI DONE 崩溃

- **控制点**：`processor/ErpInvOwnershipTransferProcessor.java#reclassifyBalance`（L134-136）
- **证据**：`b.setAvgCost(nz(b.getTotalCost()).divide(nz(b.getTotalQuantity()), BigDecimal.ROUND_HALF_UP));`——`divide(divisor, int roundingMode)` 重载在商为无限小数（如 totalCost=100, totalQuantity=3）时抛 `ArithmeticException: Non-terminating decimal expansion`，目标余额子行创建路径整体失败（DONE 回滚）。同时使用废弃 `BigDecimal.ROUND_HALF_UP` 常量（D1 风格）。
- **建议修复方向**：改 `divide(qty, 6, RoundingMode.HALF_UP)`（对齐策略族 SCALE=6）。
- **arm-index 裁决**：新增（drp SafetyStockEngine 空列表除零为已知同型家族——「除法必须带 scale+RoundingMode」，不同站点）。

### P2-CK-inv-007（D6/D8）VMI 所有权转移对层式计价物料零成本转移——`source.getAvgCost()` 对 FIFO/LIFO/BATCH/SPECIFIC 恒为 null

- **控制点**：`processor/ErpInvOwnershipTransferProcessor.java#reclassifyBalance`（L117-123）
- **证据**：`BigDecimal unitCost = nz(source.getAvgCost()); BigDecimal movedCost = qty.multiply(unitCost);`——但 FifoCostingStrategy 等层式策略在每次记账时显式 `b.setAvgCost(null)`（FifoCostingStrategy L77/L141 等）。层式计价物料的 VMI_CONSUME/CONSIGNMENT_RETURN 转移：源行 `totalQuantity -= qty` 而 `totalCost -= 0`（成本滞留源行，源行 qty=0 时 totalCost 残留>0），目的行 qty 入账成本 0 → 分行存货估值失真 + `OwnershipTransferPostingDispatcher.buildEvent` 的 AP 凭证金额（Σ line.totalCost，手工填列时另计）与余额实际转移额脱钩。
- **建议修复方向**：层式计价按 `totalCost/totalQuantity` 现算单位成本（或按成本层消耗转移）；至少与 006 一并修复除法。
- **arm-index 裁决**：新增（feature 由 `erp-inv.ownership-tracking-enabled` 门控默认关，故 P2 非 P1）。

### P2-CK-inv-008（D6）SpecificCostingStrategy 声称按 serialNo 匹配但查询从未使用 serialNo——serial-only 出库消耗任意成本层

- **控制点**：`costing/SpecificCostingStrategy.java#findSpecificLayers`（L172-195）+ `#onOutgoing`（L86-91）
- **证据**：L86-91 允许「batchNo 空但 serialNo 非空」的出库行（只校验两者不能同时为空）；javadoc L167 声称「若 batchNo 为空则按 serialNo 匹配」。但 `findSpecificLayers` 接收 `serialNo` 参数后**查询过滤从未引用**（L178-194 仅 org/mat/wh/costMethod/batchNo(非空才加)/acctSchema/businessDate）——serial-only 行命中该仓该物料**任意** remaining>0 的 SPECIFIC 层，个别计价语义失效（成本错配）；且 `appendCostLayer`（L144-163）从不写 serialNo（成本层无 serial 维度）。
- **建议修复方向**：短期在 onOutgoing 对 serial-only 行抛 `ERR_COST_NOT_AVAILABLE`（对齐 javadoc 不可满足语义）；长期成本层加 serial 维度（ORM 变更走 dual-agent）。
- **arm-index 裁决**：新增。

### P2-CK-inv-009（D6）期末 reclose 全月一次加权平均公式与自身 javadoc 不符且顺序依赖——多出库流水期间重算结果错误

- **控制点**：`costing/ErpInvCostingReclosePeriodCostsProcessor.java#recomputeWeightedAverageOutgoing`（L167-201）
- **证据**：javadoc L164-165「全月实际加权平均 = 余额 totalCost / 余额 totalQuantity」；实现 L183-184 却是 `(balance.totalCost + |本条 ledger.totalCost|) / (balance.totalQty + 本条 qty)`——只把**当前这条**出库流水的暂估成本加回。期间有多条出库流水时：(1) 分子/分母未还原其他出库条目，加权平均错；(2) 每条处理完 L197 即改写 `balance.totalCost`，下一条基于已污染余额计算——结果依赖迭代顺序。owner doc `finance/costing-methods.md §WEIGHTED_AVERAGE`（期末调整）期望全月统一口径。
- **建议修复方向**：先按期间聚合还原（期初+Σ入库），再统一对全部出库流水重定价；或按 owner doc 公式先重算期末余额 avgCost 再逐条按其重写。
- **arm-index 裁决**：新增（带复用注记：P2-MA2-030 是「reclose 不覆盖 MA/STANDARD 边缘」的覆盖面缺口，本条是 WEIGHTED_AVERAGE 分支公式正确性，不同控制点）。

### P2-CK-inv-010（D6/D8）多币种混算无汇率折算——余额/流水按首行币种混加，PostingEvent 汇率恒 1

- **控制点**：`stock/StockMoveBookkeeper.java#buildNewBalanceForMove`（L181 `balance.setCurrencyId(line.getCurrencyId())` 首行定币种，后续异币行 totalCost 直接累加）；`posting/InvPostingDispatcher.java#buildEvent`（L212 `event.setExchangeRate(BigDecimal.ONE)`）、`CostAdjustmentPostingDispatcher`（L90）、`OwnershipTransferPostingDispatcher`（L102）同型
- **证据**：`erp_inv_stock_ledger/stock_balance` 均无 exchangeRate/本位币金额列（orm 核对）；purchase `ReceiveStockMoveBuilder` L38 `request.setCurrencyId(receive.getCurrencyId())`——外币采购入库（receive.exchangeRate≠1）后：流水金额为外币原值，过账事件 `currencyId=外币 + exchangeRate=1` → GL 按外币数额当本位币入账；后续本位币入库与外币入库在同余额行混加 avgCost。
- **问题**：外币单据下 GL 金额与存货子账双重失真。单币种部署（种子数据 CNY）下无症状，属潜伏边界。
- **建议修复方向**：短期守卫——行币种≠账套本位币时抛错或按账套汇率折算后入账；长期 balance/ledger 增本位币金额（ORM 变更走 dual-agent）。
- **arm-index 裁决**：新增（P1-MA4-021「多币种/业财异常测试有效性」是测试维度，不同控制点）。

### P2-CK-inv-011（D6）Dashboard 周转率分子读 `line.getTotalCost()`——策略族只刷 unitCost 不刷 totalCost，出库成本≈请求价×量（常为 0）

- **控制点**：`dashboard/ErpInvDashboardBizModel.java#sumOutgoingCostInRange`（L337-340）
- **证据**：`sum = sum.add(DashboardUtil.nz(l.getTotalCost()))`。而 `newLines`（ErpInvStockMoveProcessor L272）在**生成时**以请求 unitCost×qty 置 line.totalCost；出库 DONE 时 7 个策略仅 `line.setUnitCost(...)` 刷新（Fifo L131、Batch L130、Lifo L121、Specific L127、Standard L92）**从不回写 totalCost**；sales 出库请求 unitCost 通常为 null→0。→ `turnoverRate = outgoingCost/avgInventory` 的分子是请求价口径（常 0），不是流水成本口径。正确来源是 `ErpInvStockLedger.totalCost`（负值，取 abs）。
- **建议修复方向**：分子改读期间出库流水 `|ledger.totalCost|`（与 `InvPostingDispatcher.buildEvent` 同源）；或策略族 DONE 时同步回写 line.totalCost（后者同时惠及报表）。
- **arm-index 裁决**：新增。

### P2-CK-inv-012（D7/D2）REQUIRES_NEW 凭证先于主事务提交——doComplete/approve 尾部失败留下孤儿凭证

- **控制点**：`processor/ErpInvStockMoveProcessor.java#doComplete`（L134 `postingDispatcher.dispatchIfApplicable` 为末步，其后 BizMutation 事务才提交）；`ErpInvLandedCostProcessor#doPostApprove`（L351 tryPost → L374 updateEntity）；`ErpInvCostAdjustApplyCostAdjustProcessor#applyCostAdjust`（L42 tryPost → L51 finalize updateEntity）
- **证据**：`IErpFinVoucherBiz.post` 为 `@Transactional(REQUIRES_NEW)`（purchase 报告已实证），凭证独立提交后，主事务若在其后失败（`finalizeApplied` 的 updateEntity 乐观锁冲突、`generateMove` 末尾 `requireMove` 异常、提交时 flush 版本冲突）→ 移动单/调整单回滚，凭证已落 GL/AP。重试会生成新 code（`MV-UUID`）新凭证，孤儿凭证无 sweep 回收（`ErpFinDeferredPostingRetryHelper` 只重试 posted=false 的 DONE 单，不回收无主凭证）。
- **对照**：purchase P1-CK-pur-002 有 SoD 必触发点定 P1；inventory 侧无确定性触发（仅尾部窄窗口异常）→ P2。
- **建议修复方向**：过账挪到主事务提交后（post-commit 事件，owner doc §与财务域协作本就写「post-commit 异步」）；或主事务最后一步化（doComplete 内 dispatch 后零后续写）。
- **arm-index 裁决**：同型家族 P1-CK-pur-002（不同域独立站点；触发强度不同降 P2）。

### P2-CK-inv-013（D7）预留量写与成本层消耗不走乐观锁重试——违背 owner doc §4「扣减失败重试」与 UC-INV-08 失败语义

- **控制点**：`processor/ErpInvStockMoveProcessor.java#applyReservation`（L210-226，plain `balanceDao.saveOrUpdateEntity`）；`FifoCostingStrategy#onOutgoing`（L117 层消耗 `saveOrUpdateEntity(layer)`）、Lifo（L109）、Batch（L118）、Specific（L115）同型
- **证据**：`ErpInvConcurrencyMetrics` javadoc 自认「app-erp 全仓仅 `StockMoveBookkeeper.updateBalanceWithRetry` 一处 tryLock+retry 范式」。预留量（confirm/cancel/complete 三路径）与成本层消耗均为托管实体 plain update——并发冲突时在 flush/commit 抛裸 ORM 乐观锁异常（fail-safe 无丢失更新，因全实体 versionProp=version），但：(1) owner doc `state-machine.md §4`「并发扣减同一批次的可用量 | 乐观锁 + **扣减失败重试**；重试仍失败则拒绝确认」未实现于预留路径；(2) 用户得到的是不可读的 ORM 异常而非 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`；(3) 预留 delta 在 `updateBalanceWithRetry` 的 applyDelta 回调之外（doComplete 中 releaseReservation 先直写），若该直写未 flush 即发生数量 delta 冲突，evict+reload 重试仅重放数量 lambda——预留释放写丢失窗口（窄：upsertBalance 内 flushSession 通常已将其落盘）。
- **建议修复方向**：预留增减并入 `updateBalanceWithRetry` 的 applyDelta（reserved+available 同 lambda）；成本层消耗包一层同类 retry 或至少映射友好错误码。
- **arm-index 裁决**：新增（带复用注记：A2.17 PASS 覆盖的是**余额行** lost-update 防护；预留/层消耗是无共享 retry 的不同控制点。check-then-act over-commitment 窗口归 A2.17 既有追踪不重复登记）。

### P2-CK-inv-014（D6/D8）到岸成本 GL 金额与库存成本层调整额口径分叉——`applyLine` 用重估公式覆盖了分摊金额

- **控制点**：`costing/CostAdjustmentService.java#applyLine`（L104-106）vs `processor/ErpInvLandedCostProcessor#createAndApplyCostAdjust`（L319-336）与 `posting/LandedCostPostingDispatcher#buildEvent`（L101-113）
- **证据**：到岸成本审核：GL 按分摊结果入账（billData ALLOCATIONS，Dr Inventory=Σ allocatedAmount）；但成本层侧 `applyLine` L106 `adjustAmount = (newUnitCost − oldUnitCost) × onHand`，其中 `oldUnitCost = balance.avgCost`（MOVING_AVERAGE，L313）而 `newUnitCost = unitPrice + allocated/qty`（分摊引擎 L85-87）。当 avgCost ≠ receive.unitPrice（存在期初库存或先前入库时必然）→ `balance.totalCost` 变动 ≠ Σ allocatedAmount → 存货子账与 GL 凭证金额漂移，期末对账不平。`createAndApplyCostAdjust` 预置的 `line.setAdjustAmount(r.getAllocatedAmount())` 被 applyLine 覆写（L116）。
- **建议修复方向**：到岸成本路径的 applyLine 语义应为「成本**追加** allocatedAmount」（或明确以重估口径统一 GL），两口径二选一并在 owner doc 登记。
- **arm-index 裁决**：新增。

### P2-CK-inv-015（D8/D9）StandardCostResolver 全表加载 FIRMED 卷算 + 逐 header N+1 + 无 orgId 过滤

- **控制点**：`costing/StandardCostResolver.java#resolveFromRollup`（L84-107）
- **证据**：`headerDao.findAllByQuery(eq("status", FIRMED))` 无 orgId 过滤、无 limit——**跨 org 的 FIRMED 卷算会被当作本 org 标准成本**（多公司场景串用，P1-MA2-093 orgId 隔离家族的写侧变体）；内存排序后逐 header `lineDao.findAllByQuery(...)` 直到命中物料（N+1，卷算多而物料晚时全扫）。每次 STANDARD 计价出入库**逐行**调用。
- **建议修复方向**：加 `eq("orgId", ...)`；按 (orgId, materialId) 直接查 line join header + businessDate DESC limit 1。
- **arm-index 裁决**：orgId 缺失维度复用 P1-MA2-093 家族注记；性能维度新增（与 P3-CK-md-012/P3-CK-pur-011 同族不同站点）。

### P2-CK-inv-016（D9）reclosePeriodCosts 期末全量 N+1 扫描

- **控制点**：`costing/ErpInvCostingReclosePeriodCostsProcessor.java#reclosePeriodCosts`（L65-93）
- **证据**：`findDoneMovesInPeriod` 全量加载期间 DONE 移动单（无 limit）→ 每 move `loadLines`（查询）→ 每 line `findLedgers`（查询）→ 每 ledger `costMethodResolver.resolve`（物料实体查询）+ 条件分支再查层/余额。期末结账关键路径（finance→`ErpInvCosting` 跨模块调用），行数 = 移动单×行×流水次查询。
- **建议修复方向**：moves 一次 in-query 加载全部行/流水（按 moveId in 分组）；物料 costMethod 批量预取。
- **arm-index 裁决**：新增。

### P3-CK-inv-017（D3）dict 死状态群：batch-status 4 值 / serial-status 3 值 / reservation EXPIRED 零 writer（部分被引用检查消费）

- **控制点**：orm dict `erp-inv/batch-status`（LOCKED/EXPIRED/CONSUMED/BLOCKED）、`erp-inv/serial-status`（OUT/RESERVED/BLOCKED）、`erp-inv/reservation-status`（EXPIRED）——grep 全 service 零 `setStatus` writer（batch/serial 全部 5/4 值仅 OPEN/IN_STOCK 可经 CRUD 手工录入）。拣货 PICKING/PICKED 死状态为 owner doc 显式 Deferred（state-machine.md §拣货单生命周期），**不登记**。
- **问题**：`ErpInvSkuReferenceChecker` 以 `status=OPEN`/`IN_STOCK` 为「活跃引用」判据——状态永不向后迁移（无出库翻转 OUT、无过期 job EXPIRED）时，判据退化为伴随条件（availableQuantity>0 等）；批次过期后仍 OPEN 不影响引用检查但与效期拦截（RC-R1.20 按 expiryDate 判）口径分叉。
- **建议修复方向**：与 arm-index MR1 dict 死状态家族统一处置（补 writer 或 owner doc 显式 Deferred 标注 + 引用检查口径声明）。
- **arm-index 裁决**：**复用** dict 死状态家族（MR1 跨 inv 域同型已裁决批次处置模式），不重复展开。

### P3-CK-inv-018（D2/D10）currentUserId 宽 catch 返回 null 无日志（2 处，跨域同型）

- **控制点**：`processor/ErpInvLandedCostProcessor.java#currentUserId`（L504-511）、`processor/ErpInvCostAdjustProcessor.java#currentUserId`（同型）
- **问题**：`catch (Exception e) { return null; }` 无日志——approvedBy/postedBy 审计字段可静默 null。同型 P3-CK-pur-010/P3-CK-md-008。
- **建议修复方向**：窄化 catch 或 log.warn。

### P3-CK-inv-019（D6）reclose recomputeOutgoingCogs 出库流水 totalCost 写正值（符号约定破坏）

- **控制点**：`costing/ErpInvCostingReclosePeriodCostsProcessor.java#recomputeOutgoingCogs`（L155）
- **证据**：`ledger.setTotalCost(totalCost)` 为正，而 ledger.quantity 为负、策略族出库流水 totalCost 恒负（`qty.negate(), totalCost.negate()`）；同文件 `recomputeWeightedAverageOutgoing` L193 正确 `negate()`。防御路径（unitCost 空/零时）触发时混入正 totalCost 出库流水。另 L104 `findExistingLayer` 不含 batchNo——同物料同仓双批次入库行时第二行误判「层已存在」跳过补建（同函数族附注）。
- **建议修复方向**：L155 negate；findExistingLayer 加 batchNo。
- **arm-index 裁决**：新增。

### P3-CK-inv-020（D9）Dashboard/追溯链无界加载与 N+1

- **控制点**：`dashboard/ErpInvDashboardBizModel`（`loadLedgersInRange` L362-368 无 limit 全量入内存；`sumMoveQtyInRange`/`sumOutgoingCostInRange` 对同期 DONE moves 两遍全量 + 行全量）；`trace/TraceChainQuery#batchTrace`（L184-190 逐 moveId `findActiveMove` N+1）。`findShortageAlert/findSlowMovingAlert` 的 ALERT_MAX_ROWS=5000 硬上限属类 C 显式裁决不登记（但无 ORDER BY，取哪 5000 行不确定，附注）。
- **建议修复方向**：趋势改 SQL 聚合（groupBy month）；batchTrace 批量 in-query。
- **arm-index 裁决**：新增（与 P3-CK-md-012/P3-CK-pur-011 同族）；orgId 行级过滤缺失维度复用 P2-RC-086（全域 watch-only，不重复登记）。

### P3-CK-inv-021（D4）死配置键：`erp-inv.concurrent-deduct-retry-backoff-ms` 声明零消费

- **控制点**：`ErpInvConstants.java`（L26-28 `CONFIG_CONCURRENT_DEDUCT_RETRY_BACKOFF_MS` + 默认值常量）——grep 全模块（含 `StockMoveBookkeeper.updateBalanceWithRetry`）零消费，重试无退避语义，配置无效。
- **建议修复方向**：接线（retry 间 sleep）或删除常量并同步 javadoc（`erp-inv.stock-check-cron` 为 owner doc 显式 Deferred，不登记）。
- **arm-index 裁决**：新增（同 P3-CK-pur-012 模式）。

## 验证为正确（显式排除，防误报）

- **平台 API 语义验证（本轮新增两例）**：
  1. `IOrmEntityDao.tryUpdateWithVersionCheck`（nop-entropy `IOrmEntityDao.java:46-50`）→ `updateEntityDirectly` → `OrmSessionImpl.updateDirectly`（L520-537）→ `flushUpdate(entity)` **只 flush 目标实体自身的全部脏字段**（含此前未 flush 的预留量写入），版本冲突返回 false——`updateBalanceWithRetry` 的 evict+reload+重试设计成立（预留直写已在 upsertBalance 的 flushSession 落盘的前提下）。
  2. `FilterBeans.eq(name, null)` → `FilterBeanToSQLTransformer`（L131-132）→ `SQL.SqlBuilder.eqEx`（L478-486 `isEmptyObject(value) → " is null"`）——**生成 `IS NULL`**。`ErpInvOwnershipTransferProcessor#findBalance` L161 的 `eq("ownerId", null)` 是合法平台用法（非缺陷）。同一验证反证 P1-CK-inv-002：`findBalance` 跳过过滤 ≠ IS NULL 语义，确为宽匹配。
  3. 既有 pilot 验证沿用：`QueryBean.addOrderField(name, desc)` 第二参为 desc（`addOrderField("lineNo", false)` = 升序，盘点行/预留行加载正确）。
- **B1 过账吞异常范式**：`InvPostingDispatcher.dispatchIfApplicable`/`LandedCostPostingDispatcher.tryPost`/`CostAdjustmentPostingDispatcher.tryPost`/`OwnershipTransferPostingDispatcher.dispatchIfApplicable` 的 catch-吞异常-保持 posted=false 是 owner doc 显式设计（`cross-domain.md §与财务域协作`、`state-machine.md §7`），兜底经 finance `ErpFinDeferredPostingRetryHelper`（实仓在位，`rebuildEvent` 从序列化 eventData 重建完整 billData，对 LANDED_COST/COST_ADJUSTMENT 同样可重放）+ `ErpFinPostingExceptionRecorder` 落异常工作台 + G2 重试耗尽 MANUAL+告警——与 ck-purchase 同一裁决，非 B1 缺陷。reverse 方向 `CostAdjustmentPostingDispatcher.reverse` 重抛（硬前置）、LandedCost reverse 吞+G4 notify（owner doc 显式）。
- **汇率消费交叉核对（P2-CK-md-004 补充裁决）**：inventory 域运行时**不消费** `ErpMdExchangeRate` 表——汇率仅来自调用方透传（`resolveExchangeRate`：freight 参数 → `receive.exchangeRate` → 兜底 ONE）或 PostingEvent 硬编码 ONE。master-data 汇率区间 finding 在 inventory 无新增消费面。
- **Dashboard 死状态交叉核对**：`ErpInvDashboardBizModel` 主查询消费 `docStatus=DONE`（doComplete 有 writer）、`moveType=OUTGOING`（生成路径有 writer）、批次效期按 `expiryDate` 日期比较而非 status——**无 pur/sal 型死状态消费**。唯一按 status 消费的 `ErpInvSkuReferenceChecker`（OPEN/IN_STOCK）已并入 P3-CK-inv-017。
- **orgId 透传（OA-03 交叉核对）**：生成链 orgId 逐级透传完整——`ReceiveStockMoveBuilder.build`（`request.setOrgId(receive.getOrgId())`）→ `newMove.setOrgId` → `buildNewBalanceForMove.setOrgId(move.getOrgId())` → `writeLedger.setOrgId`。`erp_inv_stock_move_line` 无 orgId 列（orm 核对），行级无透传义务。盘点差异单 `buildDiffMoveRequest` 同样 `setOrgId(take.getOrgId())`。
- **红冲数量语义**：`negateOrSame` 返回原值**不是 bug**——reverse 同时反转 moveType（`inverseMoveType`），正向 INCOMING(+q) 的红冲 = OUTGOING(+q)，余额净零、流水符号（策略内 negate）正确。owner doc `state-machine.md`「数量取负」措辞与实现（类型反转+正量）为等价表达的标签漂移；reverse 幂等由 `relatedBillType=REVERSAL` 的 `findExisting` 兜住（重复 reverse 返回既有反向单）。reverse businessDate 沿用原单的计价维度问题归 P2-MA2-028（已 fixed/裁决，不重开）。
- **并发首次 INSERT 兜底（P0-MA2-020 修复在位）**：UK `UK_INV_STOCK_BALANCE_NATURAL` + `updateBalanceWithRetry` SAVING 分支 flush 捕获 UK 冲突 → evict + 按自然键 reload + 转更新路径，与 owner doc README §9 描述一致（代码形态与登记修复一致）。
- **批次效期拦截**：RC-R1.20 修复形态在位（`validateBatchExpiry` 首行、负库存不豁免、config 门控、null 跳过语义）——过期拦截本身正确（缺失拒绝是另一子句，见 P1-CK-inv-004）。
- **盘点差异闭环**：`completeTake` 差异公式 `actual−book`（对齐 use-cases.md:129）、独立移动单停 CONFIRMED、过账跳过集、逐行失败隔离 + D4-b 告警（config 默认关为 owner doc 显式裁决）——与 state-machine.md §盘点单状态机登记的实现形态逐项一致。
- **`loadLines` 无分页/权限**：同聚合子表加载（父实体已过 requireEntity 管道）为项目既定 D2 边界模式，非缺陷。
- **到岸成本防重复分摊**：`lockReceiveForAllocation`（ormTemplate.lock 悲观锁）+ 两段式 `validateNotAlreadyAllocated`（map 投影 + PROXY PK 锁定读，MySQL-RR TOCTOU 修复 P1-RC-092 形态）在位。
- **D1 机械扫描全零**：`@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date`=0（全用 `CoreMetrics.*`）、`extends RuntimeException/Exception`=0（业务异常全 `NopException`+ErrorCode）、字符串 `==` 仅 null 判等、无手编生成产物。仅 `BigDecimal.ROUND_*` 废弃常量 2 处（已并入 P2-CK-inv-006/P2-CK-inv-011 附注）。
- **TransferOrder confirm 错误码 copy-paste（抛 StockTake 的码）**：owner doc `state-machine.md §调拨单状态机` 已裁决「confirmed live defect，行为保持不修正，successor 登记」+ 代码注释原样保留——已裁决项不重复登记（修复阶段若统一处理须先复核该 Non-Goal）。
- **PPV 链**：仅 STANDARD ledger 的 INCOMING 触发、退货类型跳过、红冲反向入库因 unitCost 已被刷为标准成本方差为 0 自然跳过、金额脱敏（O-22）在位——与 StandardCostingStrategy 注释的 P1-MA2-024 红冲不变量自洽。

## arm-index 复用 or 新增裁决（汇总）

- **同型复用（不重复展开）**：通用 CRUD update 无守卫 → **同型 P1-CK-pur-003/P1-CK-sal-004**（P1-CK-inv-005）；dashboard orgId 行级过滤缺失 → **P2-RC-086**（全域 watch-only）；dict 死状态 → MR1 dict 死状态家族（P3-CK-inv-017）；StandardCostResolver orgId 缺失 → P1-MA2-093 家族注记（性能维度独立登记）。
- **带复用注记的新增**：P2-CK-inv-009（P2-MA2-030 是覆盖面缺口，本条是公式正确性）、P2-CK-inv-013（A2.17 PASS 仅覆盖余额行路径；over-commitment check-then-act 窗口归 A2.17 既有追踪）、P2-CK-inv-012（同型 P1-CK-pur-002 降级变体）。
- **验证为已裁决不登记**：TransferOrder 错误码缺陷（owner doc Non-Goal 行为保持）、拣货 PICKING/PICKED 死状态（Deferred）、`erp-inv.stock-check-cron`（Deferred）、reverse businessDate 计价维度（P2-MA2-028 已处置）、批次效期拦截缺失（P1-RC-031/RC-R1.20 已修）。
- 其余关键符号（`findBalance skuId`/`warehouseId 回退`/`ROUND_HALF_UP divide`/`serialNo 匹配`/`reclose 公式`/`line.totalCost 口径`/`RETRY_BACKOFF`/`多币种混算`）在 `arm-index.md` 零命中 → 新增。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 5 | P1-CK-inv-001..005 |
| P2 | 11 | P2-CK-inv-006..016 |
| P3 | 5 | P3-CK-inv-017..021 |

按维度（主维度计）：D6×8（001/006/007/008/009/011/014/019）、D7×2（012/013）、D5×2（003/005）、D3×2（004/017）、D8×2（002/010，另 001/007/014/015 为副维度）、D9×2（016/020）、D10×1（006 副）、D4×1（021）、D2×1（018）。D4 除死配置键外 N/A（无 batch.xml）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-inv-service 90 个手写文件中约 55 个逐行深读（stock/costing/posting/processor 四包全量、StockMove/StockTake/TransferOrder/Reservation/LandedCost BizModel、Dashboard、Trace、SPI、metrics、beans.xml 全量注册核对）；其余约 35 个为薄委托状态机 Bean（迁移矩阵已由 M2-M4 计划审计覆盖，抽样 1 个核対形态）与 15 行 CRUD 桩 BizModel（已按 P1-005 同型登记，逐个核对无覆写）；orm.xml dict/versionProp/关键列核对；跨域核实 finance 兜底重放、purchase/sales 移动单构造器传参、平台 `tryUpdateWithVersionCheck`/`eq(null)` 语义（nop-entropy 源码）。
- **未深查**：`erp-inv-web` AMIS view 与后端契约 drift（归 A4.7/C8.2）；`erp-inv-api`/meta 生成物；xbiz 层 auth 注解完备性（抽查 StockLedger 空 xbiz 定性 P1-003，其余实体 xbiz 未逐一核——权限注解维度归 MA6 已审计范畴）；`ErpInvCostAdjust` 5 审批动作 Processor（Submit/Approve/Reject/ReverseApprove/Withdraw——结构性同构，抽样核对了 facade 守卫矩阵）；并发场景以代码推理 + 既有 `TestErpInvConcurrentDeduct` 存在性为据，未实仓复跑；P2-CK-inv-009/014 的金额口径推断基于代码链路推演（无实数据复算），修复阶段应先写证伪/复现测试。
- **最不确定、建议主 agent 复核**：**P2-CK-inv-014**（到岸成本 GL 与成本层口径分叉——若 finance 侧 `LandedCostAcctDocProvider` 消费 ALLOCATIONS 时另有重算则不成立，本代理读到的 Provider 直接使用 allocatedAmount，但 GL 科目解析引擎侧未逐行追）；**P2-CK-inv-010**（多币种——若产品基线显式单币种（product-scope 裁剪）则降 not-a-problem/Deferred）；**P1-CK-inv-002 触发面**（错配需「无库位 lookup + 同维度存在带库位行」共存，实际种子/操作流程下共现频率需修复阶段验证，但键语义不一致本身无争议）。
