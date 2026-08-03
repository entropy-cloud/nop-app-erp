# rc-ma1-a1-26 inventory-F2 批次/可用量/负库存 需求-实现符合性五级追踪审计

> 报告类型：MA1(RC) 需求-实现符合性五级追踪审计（只读审计，无代码/ORM/真相源变更）
> Mission: requirement-compliance
> Work Item: A1.26（inventory-F2 批次与可用量，UC-INV-02/06/09，3 UC + 6 候选缺口）
> 切片基线：`docs/audits/rc-requirement-baseline-inventory.md` A1.26 UC 锚点（`✅ 一致`）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> L1 真相源：`docs/design/inventory/use-cases.md`（UC-INV-02/06/09）
> L2 设计参考：`state-machine.md` §4 异常路径 + `trace-chain.md` §追溯链与批次 + `cross-domain.md` §余量校验
> L5 既有证据复用：A2.11（`2026-07-28-0400-arm-ma2-inventory-state-machine.md`，移动单状态机 + 出库 approve 可用量校验 PASS + 负库存放行证伪 PASS）/ A2.9（`2026-07-28-0400-arm-ma2-sales-state-machine.md`，UC-INV-02 跨域传播至 sales PASS）/ A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`，ErpInvStockMoveProcessor/validateAvailable 代码质量 PASS）

## 整体裁决

**3 UC 结论：UC-INV-02 = 接受；UC-INV-06 = P1（效期拦截完全缺失）；UC-INV-09 = 接受。** **1 项新 P1 finding（P1-RC-031）登记，零 P0、零新 P2。**

UC-INV-02（销售出库可用量不足拒绝）+ UC-INV-09（负库存放行）**完整实现需求契约**（validateAvailable + @BizMutation 原子回滚 + config 默认 false + NopSysVariable 运行时覆盖），行为经 MA2 A2.11/A2.9 三重证实，本切片只补需求视角增量差异（approveStatus 保持 SUBMITTED 的跨域传播路径）。

UC-INV-06 批次追溯侧 ✅ 实现完整（`TraceChainQuery.batchTrace` + 余额按 batchNo 维度 + 领料继承 batchNo + 完工入库生成新批次）；**但效期拦截完全缺失**——L1 `use-cases.md:113` 逐字「若 批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库移动单确认失败（批次过期拦截）」**未实现**：`ErpInvStockMoveProcessor.validateAvailable:116-136` 无 expiry check；`ErpInvBatch.expiryDate`/`shelfLifeDays` 字段存在（`app-erp-inventory.orm.xml:908-909`）但 `ErpInvBatchBizModel` 是 15 行 CRUD 桩（无 ACTIVE→EXPIRED 状态迁移、无 expiry scheduler）；`ErpMdMaterial.isBatchManaged`（`app-erp-master-data.orm.xml:203`）字段存在但 `validateAvailable` 从不查询它（条件分支未被消费）；仅 `ErpInvDashboardBizModel.findBatchExpiryAlert:250-291` 提供 advisory 预警（`@BizQuery` 返回行集，非 hard interception）。过期批次出库不被拦截，存在过期物料发放风险（食品安全/药品合规风险）。倾向 **P1**（§2 P1① 功能完全缺失），须人工确认 product-scope 是否范围裁剪（L1 明确要求，故默认 P1 强制实现）。

---

## 1. 需求契约原文（L1 逐字引用）

> 来源：`docs/design/inventory/use-cases.md`（L1 权威功能契约，§4 Q1 真相源层级 2）

### UC-INV-02 销售出库可用量不足拒绝（`use-cases.md:37`）

```
场景:销售出库时库存可用量不足,移动单创建/确认失败,审核回滚。

可验证断言(见 state-machine.md §4、cross-domain.md §余量校验):
  校验维度 = 物料 × 仓库 × 库位 × 批次
  若 可用量 < 出库数量:
    generateMove(outgoing) 拒绝
    销售订单审核回滚(approveStatus 保持 SUBMITTED)
    库存余额不变

  // 可用量 = 现有量 - 预留量(见 cross-domain §余量校验)
  // 负库存配置 erp-inv.allow-negative-stock 决定是否放行
```

### UC-INV-06 批次追溯与效期拦截（`use-cases.md:106`）

```
场景:领料继承入库批次;批次过期则拦截出库。

可验证断言(见 trace-chain.md §追溯链与批次、state-machine.md §4):
  领料移动单.批次 == 入库移动单.批次  (批次继承)
  若 批次.效期 < 当前日期 且 物料.批次管控 == 强制:
    出库移动单确认失败(批次过期拦截)
  完工入库生成新批次(不继承)
```

### UC-INV-09 负库存放行（`use-cases.md:155`）

```
场景:先发货后入库的场景(配置允许负库存)。

可验证断言(见 state-machine.md §4、cross-domain.md §余量校验):
  配置 erp-inv.allow-negative-stock == true
  出库时 可用量 < 出库数量 → 放行(现有量变负)
  后续入库移动单补回
  若 配置 == false: 拒绝(回到 UC-INV-02)
```

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

> StockMove 遵循 Facade BizModel + per-mutation Processor 两层模式（R6.4 重构）。可用量校验在 `ErpInvStockMoveProcessor.doConfirm` 状态翻转前强制执行。

### UC-INV-02 销售出库可用量不足拒绝

| 验收标准 | L3 代码路径（含行号） | 跨域调用链 |
|---------|----------------------|-----------|
| ① 校验维度 = 物料×仓库×库位×批次 | `ErpInvStockMoveProcessor.java:116-136 validateAvailable`（per-line `bookkeeper.upsertBalance(move,line,...):124-125` 按物料/SKU/仓库/库位/batchNo 维度查/建 `ErpInvStockBalance`；ORM `app-erp-inventory.orm.xml:415 UK_INV_STOCK_BALANCE_NATURAL` on `(orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId)` 物理支撑维度） | 跨域入口：sales `ErpSalDeliveryProcessor.java:244 stockMoveBiz.generateMove`（`IErpInvStockMoveBiz` Facade） |
| ② 可用量 < 出库数量 → generateMove 拒绝 | `validateAvailable:128-134`：`available.compareTo(required) < 0` → `throw new NopException(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT).param(ARG_MATERIAL_ID/WAREHOUSE_ID/AVAILABLE/REQUIRED)`；`@BizMutation generateMove`（`IErpInvStockMoveBiz.java:31`）原子事务回滚 | exception bubble up → sales `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → 整个 `__approve` mutation 回滚 |
| ③ 可用量公式 = 现有量 − 预留量 | `ErpInvStockMoveProcessor.recomputeAvailable:278-283`：`availableQuantity = totalQuantity − reservedQuantity − lockedQuantity`（L1「可用量 = 现有量 − 预留量」语义一致；lockedQuantity 是更严格的三因子版本，满足 L1 不破坏语义） | — |
| ④ 销售订单审核回滚（approveStatus 保持 SUBMITTED） | `@BizMutation generateMove`（`IErpInvStockMoveBiz:31`）事务回滚传播——exception 抛出后整个 mutation 事务回滚，move/line/balance 均不落库；caller `ErpSalDeliveryProcessor:244` 的 `__approve` 亦为 `@BizMutation`，事务传播至 sales 域审核，approveStatus 经事务回滚保持原值（SUBMITTED） | 跨域事务传播（标准 Nop `@BizMutation` REQUIRES 默认）；sales 侧 approveStatus 状态机由 A2.9 覆盖（PASS） |
| ⑤ 库存余额不变 | 拒绝发生在 `doConfirm:86-98` **状态翻转前**（`validateAvailable:94` 早于 `setDocStatus(CONFIRMED):96`）；`bookkeeper.upsertBalance:124-125` 的预留量写入在 `applyReservation:95`（在 validateAvailable 之后），拒绝路径不进入 applyReservation，故 reservedQuantity/balance 不变 | — |
| ⑥ 负库存配置决定放行（与 UC-INV-09 共用控制点） | `validateAvailable:117-119 if(isNegativeStockAllowed()) return;` 短路（由 UC-INV-09 详审）；`isNegativeStockAllowed:285-288 AppConfig.var(CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE)` | — |

### UC-INV-06 批次追溯与效期拦截

| 验收标准 | L3 代码路径（含行号） | 跨域调用链 |
|---------|----------------------|-----------|
| ① 领料移动单.批次 == 入库移动单.批次（批次继承） | 制造领料：`MaterialIssueStockMoveBuilder.java:66 req.setBatchNo(line.getBatchNo())`（领料请求透传 issue line 的 batchNo）→ `ErpInvStockMoveProcessor.newLines:202 line.setBatchNo(req.getBatchNo())` 持久化；`StockMoveBookkeeper.upsertBalance:154 findBalance(...,line.getBatchNo(),...)` + `buildNewBalanceForMove:173 balance.setBatchNo(line.getBatchNo())` 以 batchNo 为余额维度键 | 跨域：mfg `ErpMfgWorkOrderProcessor` 经 issue StockMove Builder 调用库存 `generateMove` Facade |
| ② 批次追溯（机制侧） | `TraceChainQuery.batchTrace:168-192`：聚合 `findLinesByBatch:228-233`（`ErpInvStockMoveLine.batchNo`）+ `findLedgersByBatch:235-240`（`ErpInvStockLedger.batchNo`）→ `findActiveMove:196-204`（filter `delVersion=0L` via `findActiveMovesByQuery:220-226`）；`ErpInvStockMoveBizModel.batchTrace`（`@BizQuery`）入口；`StockMoveBookkeeper.writeLedger:217 ledger.setBatchNo(line.getBatchNo())` 写流水 batchNo | — |
| ③ 完工入库生成新批次（不继承） | 制造完工：`ErpMfgWorkOrderProcessor.java:321-326 batchGenealogyWriter.writeOnCompletion(wo, completedQty, context)` → `BatchGenealogyWriter.writeOnCompletion:64` → `:151 String batchNo = ErpMfgConstants.GENEALOGY_OUTPUT_BATCH_PREFIX + "-" + wo.getCode()`（按工单 code **派生新 batchNo**，非继承输入批次）→ `:159-168 newBatchEntity` 创建 `ErpInvBatch`（状态 OPEN）；best-effort 注释 `ErpMfgWorkOrderReportCompletionProcessor.java:67`（genealogy 写失败不阻断完工入库） | 跨域：mfg 调库存写 `ErpInvBatch` 实体（同事务） |
| **④ 批次过期拦截**（**功能完全缺失**） | **❌ 无实现**：`ErpInvStockMoveProcessor.validateAvailable:116-136` 全文无 `getExpiryDate()` 调用、无 `BATCH_STATUS_EXPIRED` 判定、无 `isBatchManaged` 查询；`doConfirm:86-98` + `doComplete:100-114` 同样无 expiry check；`ErpInvBatchBizModel.java` = **15 行 CRUD 桩**（`extends CrudBizModel<ErpInvBatch>` 无任何方法，无 ACTIVE→EXPIRED 状态迁移、无 expiry scheduler）；全 `module-inventory/erp-inv-service/src/main` grep `效期\|ERR_.*EXPIR\|ERR_.*BATCH\|isBatchManaged` **零业务命中**（仅 dashboard advisory `ErpInvDashboardBizModel.findBatchExpiryAlert:250-291` `@BizQuery` 返回行集供前端展示，非 hard interception）；`ErpInvErrors` 无 `ERR_BATCH_EXPIRED` 类错误码 | — |
| **⑤ 物料.批次管控 == 强制 条件分支**（**条件未被消费**） | `ErpMdMaterial.isBatchManaged`（`Boolean`，`app-erp-master-data.orm.xml:203 IS_BATCH_MANAGED`，`_ErpMdMaterial.java:57-58 PROP_NAME_isBatchManaged`，default false）字段**存在**；但 `ErpInvStockMoveProcessor.validateAvailable:116-136` + `applyReservation:138-154` + `doConfirm/doComplete` **从不查询** `IErpMdMaterialBiz`/`daoFor(ErpMdMaterial).getEntityById(materialId).isBatchManaged()`；L1「物料.批次管控 == 强制」条件分支完全未被消费 | — |
| ⑥ 字段基础设施存在（证明属"代码逻辑缺口"非"ORM 结构缺口"） | `ErpInvBatch.expiryDate`（`app-erp-inventory.orm.xml:908 EXPIRY_DATE DATE`，`PROP_ID_expiryDate=10`）+ `shelfLifeDays`（`:909 SHELF_LIFE_DAYS INTEGER`，propId=11）字段存在；`EXPIRED` dict 状态存在（`ErpInvDaoConstants.BATCH_STATUS_EXPIRED="EXPIRED"`）；`isBatchManaged` 字段存在 → **修复不需 ORM 结构变更**，属"代码逻辑修复"类（预授权） | — |

### UC-INV-09 负库存放行

| 验收标准 | L3 代码路径（含行号） | 跨域调用链 |
|---------|----------------------|-----------|
| ① 配置 erp-inv.allow-negative-stock 决定行为 | `ErpInvConstants.java:14 CONFIG_ALLOW_NEGATIVE_STOCK="erp-inv.allow-negative-stock"` 常量；`ErpInvStockMoveProcessor.isNegativeStockAllowed:285-288`：`AppConfig.var(CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE)`（**default false**），运行时经 `NopSysVariable` 标准机制覆盖（无需重启） | — |
| ② 配置==true → 放行（现有量变负） | `validateAvailable:117-119 if(isNegativeStockAllowed()) return;` **短路所有 per-line 可用量检查**；后续 `bookkeeper.bookCompletion:110` 按 `MOVE_TYPE_OUTGOING` 走标准出库路径，`totalQuantity` 经 `updateBalanceWithRetry:256-328` 减至负值（无下界守卫）；`StockMoveBookkeeper.writeLedger:208 setQuantity(signedQty)` 流水允许负数 | — |
| ③ 后续入库移动单补回 | 标准入库路径（`MOVE_TYPE_INCOMING`）经同一 `bookCompletion:110` 加回 `totalQuantity`，自然补回负余额；`recomputeAvailable:278-283` 重算 available；UC-INV-01 入库链路（A1.25 已审 PASS）承接 | — |
| ④ 配置==false → 拒绝（回到 UC-INV-02） | `isNegativeStockAllowed:286 default Boolean.FALSE` → `validateAvailable` 不短路 → 进入 per-line `available < required` 检查（UC-INV-02 ②）→ 抛 `ERR_AVAILABLE_INSUFFICIENT`；与 L1 字面「拒绝（回到 UC-INV-02）」语义一致 | — |

---

## 3. 测试证据（L4 测试断言，注明断言强度）

| 测试文件#方法 | 覆盖 UC | 断言强度 | 断言要点 |
|--------------|--------|---------|---------|
| `TestErpInvStockMoveBizModel#testIllegalTransitionRejected:86-93` | UC-INV-02（异常路径） | **强** | DONE→CONFIRMED 非法迁移抛 `ERR_ILLEGAL_STATUS_TRANSITION`（GraphQL error code propagation 验证） |
| `TestErpInvStockMoveBizModel#testCancelReleasesReservation:95-115` | UC-INV-02 ①⑤ + UC-INV-09 reserved/available 关系 | **强** | 余额 total=10 → CONFIRMED outgoing reserves 5 → reserved=5 + available=5（=10−5−0）；cancel → reserved=0 + available=10 恢复 |
| `TestErpInvStockMoveBookkeeping#testOutgoingInsufficientAvailableRejected`（`:120-121`） | UC-INV-02 ② | **强** | `assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), resp.getCode())` 断言拒绝错误码 |
| `TestErpInvStockMoveBookkeeping`（`:154` 负库存放行） | UC-INV-09 ② | **强** | allow-negative-stock=true，无初始库存出库 5 → total & available 同步变 −5（无下界守卫验证） |
| `TestErpInvStockMoveBookkeeping`（`:217` 负库存下 CONFIRMED 占预留） | UC-INV-09 + UC-INV-02 reserved 语义 | **强** | allow-negative-stock=true 下 CONFIRMED 出库（无 DONE）正确占用 reserved（验证配置不影响预留机制） |
| `TestErpInvStockMoveBookkeeping`（`:268-278` default false 拒绝） | UC-INV-09 ④ | **强** | 不显式设 allow-negative-stock（默认 false），无库存出库 → `ERR_AVAILABLE_INSUFFICIENT`（验证默认安全语义） |
| `TestErpInvConcurrentDeduct#testToggleNegativeStockFlag:425-466` | UC-INV-09 config 翻转 | **强** | 运行时 toggle flag 行为切换（NopSysVariable 机制验证） |
| 各 CostingStrategy test（FIFO:356 / LIFO:263 / Batch:248 / Specific:314 / Standard:380） | UC-INV-09 各 costMethod 下负库存行为 | **强** | 各策略均 flip config 验证 allow-negative-stock 分支正确（覆盖矩阵完整） |
| `TestErpInvTraceChain#testBatchTraceByBatchNo:154-164` | UC-INV-06 ② 批次追溯 | **强** | 2 张含 BATCH-001 移动单 → batchTrace 返回 2 nodes；不存在 batchNo → 0 nodes（精确节点数断言） |
| **UC-INV-06 ④⑤ 效期拦截测试** | **❌ 缺失** | — | 全 `module-inventory/erp-inv-service/src/test` grep `效期\|expiry\|isBatchManaged\|ERR_BATCH_EXPIRED` **零业务命中**（仅 dashboard 测试 `TestErpInvDashboard` seed expiryDate 但断言的是预警返回行集，非 outgoing move 拒绝路径）。过期批次出库拦截路径**无任何测试**（路径不存在，因实现缺失） |

**断言强度评级**：UC-INV-02/09 全部覆盖方法均为**强断言**（精确错误码 + 数值断言）；UC-INV-06 批次追溯侧强断言；UC-INV-06 效期拦截侧**测试完全缺失**（与实现同步缺失，§2 P1⑤ 命中）。

---

## 4. 运行时行为证据（L5，复用既有 MA2 报告）

> §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，不重复验证。

| 行为 | L5 证据来源 | 结论 |
|------|-----------|------|
| 出库 approve 可用量校验已落实（UC-INV-02 主路径） | A2.11（`2026-07-28-0400-arm-ma2-inventory-state-machine.md:459`）PASS | **行为已证实**（`doConfirm → validateAvailable` 强制 (available < required) → `ERR_AVAILABLE_INSUFFICIENT`；`@BizMutation` 原子回滚） |
| UC-INV-02 跨域传播至 sales（出库 approve 销售独有约束） | A2.9（`2026-07-28-0400-arm-ma2-sales-state-machine.md:451`）PASS | **行为已证实**（`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245 → IErpInvStockMoveBiz.generateMove` Facade；validateAvailable 失败 → exception bubble → `__approve` mutation 回滚 → approveStatus 保持原值） |
| 负库存放行配置默认 false + NopSysVariable 运行时覆盖 | A2.11 `:459`「负库存放行权限缺失——证伪」（config 默认 false + 运行时覆盖权限保护）PASS | **行为已证实**（默认安全，运行时可配置；`AppConfig.var(..., Boolean.FALSE)`） |
| 库存余额并发安全（UC-INV-02 拒绝路径不破坏并发不变量） | A2.17（`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`）§13 PASS + P0-MA2-020 已落地 | **行为已证实**（`UK_INV_STOCK_BALANCE_NATURAL` + `updateBalanceWithRetry` MANAGED/TRANSIENT/SAVING 三分支 + `TestErpInvConcurrentDeduct` 6 测试含 3 真实多线程） |
| `ErpInvStockMoveProcessor` / `validateAvailable` 代码质量 | A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）PASS（A4.5 §代码质量段） | **代码质量已证实**（编排健壮性 / 异常规范化 / BigDecimal 货币类型安全 / 拒绝路径在状态翻转前落实） |
| **过期批次出库拦截行为**（UC-INV-06 ④） | **无既有 MA2 证据**（A2.11 `:459` 仅在 dashboard context 列出 `ErpInvBatch.expiryDate` advisory，未审计 move-confirm layer 的 expiry interception） | **行为缺失**（与 §2/§3 静态证据一致——拦截路径不存在） |
| 批次追溯运行时行为（UC-INV-06 ②） | `TestErpInvTraceChain#testBatchTraceByBatchNo:154-164` 强断言 | **行为已证实**（按 batchNo 聚合 move line + ledger → move，filter delVersion=0） |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论）

### 五级追踪矩阵

| UC | L1 需求契约 | L2 owner doc（设计参考） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|------------|------------------------|------------|------------|--------------|-----------|
| **UC-INV-02** | `use-cases.md:37` 销售出库可用量不足拒绝（断言：校验维度=物料×仓库×库位×批次 / 可用量<出库数量→generateMove 拒绝 / approveStatus 保持 SUBMITTED / 库存余额不变 / 可用量=现有量−预留量 / 负库存配置决定放行） | `state-machine.md §4 异常路径`（出库可用量不足 DRAFT→CONFIRMED 时拒绝，整个审核回滚）+ `cross-domain.md §余量校验`（校验维度 + 不足拒绝 + 负库存配置，设计参考，冲突以 L1 为准） | `ErpInvStockMoveProcessor.validateAvailable:116-136`（per-line available<required → `ERR_AVAILABLE_INSUFFICIENT`）+ `recomputeAvailable:278-283`（total−reserved−locked）+ `@BizMutation generateMove`（`IErpInvStockMoveBiz:31`）原子事务回滚 + 跨域 sales `ErpSalDeliveryProcessor:244` Facade 调用 | `testIllegalTransitionRejected:86-93` 强 + `testCancelReleasesReservation:95-115` 强 + `TestErpInvStockMoveBookkeeping:120-121` 强（ERR_AVAILABLE_INSUFFICIENT 错误码断言） | A2.11 出库 approve 可用量校验 PASS + A2.9 UC-INV-02 跨域传播至 sales PASS + A4.5 validateAvailable 代码质量 PASS | **接受**（全验收标准 L3-L5 证据一致；候选缺口 #6 approveStatus 保持 SUBMITTED 由 `@BizMutation` 事务回滚传播满足） |
| **UC-INV-06** | `use-cases.md:106` 批次追溯与效期拦截（断言：领料.批次==入库.批次 / 批次追溯 / 完工入库新批次 / **若批次.效期<当前日期 且 物料.批次管控==强制：出库确认失败（批次过期拦截）**） | `trace-chain.md §追溯链与批次`（批次追溯链路 + 批次效期追溯查询，设计参考）+ `state-machine.md §4 异常路径`（批次过期：出库时校验批次是否在有效期；过期批次拒绝出库[可配置放行]） | 批次继承 `MaterialIssueStockMoveBuilder:66 setBatchNo` + 余额键 `StockMoveBookkeeper:154/173 setBatchNo` + 批次追溯 `TraceChainQuery.batchTrace:168-192` + 完工新批次 `BatchGenealogyWriter.writeOnCompletion:64/151-168`（`ErpMfgWorkOrderProcessor:326` 调用）；**效期拦截 ❌ 缺失**——`validateAvailable:116-136` 无 expiry check + `ErpInvBatchBizModel` 15 行 CRUD 桩 + dashboard `findBatchExpiryAlert:250-291` advisory only | 批次追溯 `testBatchTraceByBatchNo:154-164` 强；**效期拦截 ❌ 零测试**（路径不存在） | 批次追溯行为经 TestErpInvTraceChain 强证实；**效期拦截行为缺失**（无既有 MA2 证据 + 静态证据证实路径不存在） | **P1**（#1 效期拦截完全缺失 + #2 isBatchManaged 条件分支未被消费，同根因；§2 P1① + §2 P1⑤） |
| **UC-INV-09** | `use-cases.md:155` 负库存放行（断言：配置决定行为 / true→放行现有量变负 / 后续入库补回 / false→拒绝回到 UC-INV-02） | `state-machine.md §4 异常路径` + `cross-domain.md §余量校验`（负库存配置默认 false，开启时跳过可用量校验允许余额为负，设计参考） | `ErpInvConstants:14 CONFIG_ALLOW_NEGATIVE_STOCK` + `isNegativeStockAllowed:285-288 AppConfig.var(...,Boolean.FALSE)` + `validateAvailable:117-119` 短路 + 标准入库补回路径（UC-INV-01） | `TestErpInvStockMoveBookkeeping:154/217/268-278` 强 + `TestErpInvConcurrentDeduct:425-466` toggle flag 强 + 5 CostingStrategy test（FIFO/LIFO/Batch/Specific/Standard）均 flip config 强 | A2.11「负库存放行权限缺失——证伪」（config 默认 false + NopSysVariable 运行时覆盖权限保护）PASS | **接受**（全验收标准 L3-L5 证据一致；候选缺口 #5 默认 false 安全性满足 L1「若配置==false：拒绝」语义） |

### 候选缺口逐条裁决（#1-#6）

| # | 候选缺口 | live-repo HEAD 实测复核 | 裁决 | §2 判据 |
|---|---------|------------------------|------|---------|
| **#1** | UC-INV-06 效期拦截缺失（L1 `:113`） | **成立（功能完全缺失）**：`ErpInvStockMoveProcessor.validateAvailable:116-136` 全文无 `getExpiryDate()`/`BATCH_STATUS_EXPIRED`/expiry 调用；`doConfirm:86-98` + `doComplete:100-114` 同样无；`ErpInvBatchBizModel.java` = 15 行 CRUD 桩（无状态迁移、无 scheduler）；dashboard `findBatchExpiryAlert:250-291` 是 `@BizQuery` advisory（返回行集），非 hard interception；`ErpInvErrors` 无 `ERR_BATCH_EXPIRED`；`module-inventory/erp-inv-service/src/main` grep `效期\|ERR_.*EXPIR\|ERR_.*BATCH` **零业务命中**；字段基础设施齐全（`ErpInvBatch.expiryDate` ORM `:908` + `EXPIRED` dict 状态 + `ErpInvDaoConstants.BATCH_STATUS_EXPIRED`）但无消费——属"代码逻辑缺口"非"ORM 结构缺口"。**涉食品安全/药品合规风险**（过期物料发放）。**须人工确认 product-scope 是否范围裁剪**（L1 `use-cases.md:113` 逐字明确要求"出库移动单确认失败"，product-scope 未显式裁剪效期拦截 → 默认 P1 强制实现） | **P1** | §2 P1①（功能完全缺失——L1 `:113` 字面要求未实现）+ §2 P1⑤（验收标准零断言——拦截路径无测试） |
| **#2** | UC-INV-06 物料.批次管控 flag（L1 `:113` "物料.批次管控 == 强制"） | **成立（与 #1 同根因同控制点，按 §7 复用合并到 P1-RC-031）**：`ErpMdMaterial.isBatchManaged`（`Boolean`，`app-erp-master-data.orm.xml:203 IS_BATCH_MANAGED`，`_ErpMdMaterial.java:57-58`，default false）字段**存在**；但 `validateAvailable:116-136` + `applyReservation:138-154` + `doConfirm/doComplete` **从不查询** `IErpMdMaterialBiz`/`daoFor(ErpMdMaterial).isBatchManaged()`；L1「物料.批次管控 == 强制」条件分支完全未被消费。**与 #1 同根因**（expiry interception 代码路径完全缺失，isBatchManaged 是该路径的前置条件分支），按 §7 复用不新建 finding——合并入 P1-RC-031 描述 | **P1**（合并入 P1-RC-031） | §2 P1①（条件分支未被消费——同 #1 根因） |
| **#3** | UC-INV-06 完工入库新批次（L1 `:115`） | **不成立（已实现）**：mfg `ErpMfgWorkOrderProcessor:321-326 batchGenealogyWriter.writeOnCompletion(wo, completedQty, context)` → `BatchGenealogyWriter.writeOnCompletion:64` → `:151 batchNo = ErpMfgConstants.GENEALOGY_OUTPUT_BATCH_PREFIX + "-" + wo.getCode()`（按工单 code **派生新 batchNo**）→ `:159-168 newBatchEntity` 创建 `ErpInvBatch`（OPEN 状态）；best-effort try/catch（`ErpMfgWorkOrderReportCompletionProcessor:67` 注释：genealogy 写失败不阻断完工入库）。完工入库批次**不继承输入批次**（按 wo.code 派生），满足 L1「完工入库生成新批次（不继承）」 | **接受** | — |
| **#4** | UC-INV-06 批次继承（L1 `:111` "领料移动单.批次 == 入库移动单.批次"） | **不成立（已实现）**：mfg `MaterialIssueStockMoveBuilder:66 req.setBatchNo(line.getBatchNo())`（领料请求透传 issue line 的 batchNo——issue line 的 batchNo 来自工单领料时由用户/上溯入库移动单指定）→ `ErpInvStockMoveProcessor.newLines:202 line.setBatchNo(req.getBatchNo())` 持久化 → `StockMoveBookkeeper.upsertBalance:154 findBalance(...,line.getBatchNo(),...)` + `buildNewBalanceForMove:173 setBatchNo(line.getBatchNo())` 以 batchNo 为余额维度键 + `writeLedger:217 ledger.setBatchNo(line.getBatchNo())` 写流水。**领料移动单的 batchNo 由调用方（mfg/sales/purchase）经 StockMoveRequest.lines[].batchNo 传入，inventory 域忠实持久化**——批次继承是"调用方契约"，inventory 域在数据层忠实记录（满足 L1 字面） | **接受** | — |
| **#5** | UC-INV-09 默认 false 安全性（L1 `:161-164`） | **不成立（已实现）**：`ErpInvStockMoveProcessor.isNegativeStockAllowed:285-288 AppConfig.var(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE)`——**字面 default = `Boolean.FALSE`**；`validateAvailable:117-119 if(isNegativeStockAllowed()) return;` 在 default false 下不短路 → 进入 per-line 检查 → 抛 `ERR_AVAILABLE_INSUFFICIENT`（回到 UC-INV-02 路径）。与 L1「若 配置 == false：拒绝（回到 UC-INV-02）」**字面语义一致**。运行时经 NopSysVariable 翻转（`TestErpInvConcurrentDeduct:425-466` 强覆盖 toggle 行为） | **接受** | — |
| **#6** | UC-INV-02 回滚原子性（approveStatus 保持 SUBMITTED） | **不成立（已实现）**：`IErpInvStockMoveBiz.generateMove`（`IErpInvStockMoveBiz.java:31` `@BizMutation`）原子事务——exception 抛出后整个 mutation 事务回滚，move/line/balance 均不落库；caller sales `ErpSalDeliveryProcessor:244 stockMoveBiz.generateMove` 的 `__approve` 亦为 `@BizMutation`，事务传播至 sales 域审核（标准 Nop `@BizMutation` REQUIRES 默认），approveStatus 经事务回滚保持原值（SUBMITTED）。**注**：L1「销售订单审核回滚」字面的"订单审核"控制点 vs 实现"出库审核"控制点的偏离由 A1.18/A1.19 P1-RC-020 覆盖（不同 UC、不同控制点，按 §7 reuse），本切片只裁决 UC-INV-02 inventory 侧的回滚原子性——满足 | **接受** | — |

### 每 UC 符合性结论（取最高）

| UC | 结论 | 命中 §2 判据 |
|----|------|-------------|
| UC-INV-02 | **接受**（全验收标准 L3-L5 证据一致；#6 回滚原子性由 `@BizMutation` 满足） | §2 接受（availableQuantity 校验 + 公式 + 跨域 Facade + 原子回滚 + 默认安全全证实） |
| UC-INV-06 | **P1**（#1 效期拦截完全缺失 + #2 isBatchManaged 条件分支未被消费，同根因——合并为 P1-RC-031） | §2 P1①（功能完全缺失——L1 `:113` 字面"出库确认失败"未实现）+ §2 P1⑤（验收标准零断言——拦截路径无测试，因实现缺失） |
| UC-INV-09 | **接受**（全验收标准 L3-L5 证据一致；#5 默认 false 安全性满足 L1 字面语义） | §2 接受（config + 短路 + 入库补回 + 默认安全 + 运行时覆盖全证实） |

---

## 6. 与 arm-index 衔接（复用 or 新增裁决）

### 本切片 finding 裁决

**1 项新 P1 finding 登记**（P1-RC-031），按 §7 grep arm-index 同域同控制点后裁决为"新建"（无既有同控制点 finding）。

| Finding ID | 报告 | 域 | UC | 描述（简） | 分级判据 | 目标 MR | 修复状态 |
|-----------|------|---|----|-----------|---------|--------|---------|
| `P1-RC-031` | rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock | inventory（+master-data isBatchManaged 投影） | UC-INV-06 ④⑤（效期拦截 + isBatchManaged 条件分支） | **UC-INV-06 出库效期拦截完全缺失（功能完全缺失，§2 P1①；涉食品安全/药品合规风险；须人工确认 product-scope 是否范围裁剪）**：L1（`use-cases.md:113`）逐字「若 批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库移动单确认失败（批次过期拦截）」。L2 owner doc（`state-machine.md §4 异常路径 :64`）一致要求「批次过期：出库时校验批次是否在有效期；过期批次拒绝出库（可配置放行）」+ `trace-chain.md §批次效期追溯 :218-234` 描述效期查询机制。L3 实仓：`ErpInvStockMoveProcessor.validateAvailable:116-136` **全文无 expiry check**（无 `getExpiryDate()`/`BATCH_STATUS_EXPIRED`/`isBatchManaged` 查询），`doConfirm:86-98` + `doComplete:100-114` 同样无；`ErpInvBatchBizModel.java` = **15 行 CRUD 桩**（`extends CrudBizModel<ErpInvBatch>` 无任何方法——无 ACTIVE→EXPIRED 状态迁移、无 expiry scheduler/cron/Job bean）；仅 `ErpInvDashboardBizModel.findBatchExpiryAlert:250-291` 提供 advisory 预警（`@BizQuery` 返回行集供前端展示，非 hard interception，且 default `erp-dash.inv-batch-expiry-days=0` 关闭）；`ErpInvErrors` 无 `ERR_BATCH_EXPIRED` 类错误码；全 `module-inventory/erp-inv-service/src/main` grep `效期\|ERR_.*EXPIR\|ERR_.*BATCH\|isBatchManaged` **零业务命中**。**字段基础设施齐全**（`ErpInvBatch.expiryDate` ORM `:908 EXPIRY_DATE` + `shelfLifeDays` `:909` + `EXPIRED` dict 状态 `ErpInvDaoConstants.BATCH_STATUS_EXPIRED` + `ErpMdMaterial.isBatchManaged` `app-erp-master-data.orm.xml:203 IS_BATCH_MANAGED`）但**无消费** → 属"代码逻辑缺口"非"ORM 结构缺口"（修复不需 ORM 结构变更，按 roadmap 预授权类目[代码逻辑修复]可自动执行）。**#2 isBatchManaged 条件分支未被消费**与本 finding **同根因同控制点**（expiry interception 代码路径完全缺失，isBatchManaged 是该路径的前置条件分支），按 §7 复用合并入本 finding 描述（不新建独立编号）。**§4 三判据复核均不满足**（无 plan-audit 专门裁决效期拦截裁剪 / owner doc `state-machine.md:64` + `trace-chain.md:218-234` 描述效期拦截为正向需求非 Non-Goal / product-scope 未将效期拦截列入范围裁剪）→ 非 documented simplification → 按 Q4=(a) 默认 P1 强制实现。**非 P0**：(1) 不破坏库存余额守恒（过期的 batch 仍按标准 stock move bookkeeper 扣减，UK + 乐观锁独立防护）；(2) 不破坏 GL 平衡（存货估值凭证借贷平衡不变）；(3) 不破坏 O2C/P2P 核心循环（出库/入库主链路完整）；(4) 不破坏活跃数据（错误是"该拦截而未拦截"的合规风险，非"数据写入破坏"）。**涉食品安全/药品合规风险**（过期物料发放——state-machine.md `:64` 注「可配置放行」隐含强拦截为默认行为）→ 不可方案 B 降级。**须人工确认 product-scope 是否范围裁剪**：若裁剪 → 按 §4 (iii) 改 product-scope 真相源非降级（需求变更）；若未裁剪 → P1 强制实现 Q4 无例外。**与既有 finding 不同控制点**：grep arm-index inventory 批次/效期同域同控制点——A2.11 `:459` 仅在 dashboard context 列出 `ErpInvBatch.expiryDate`（advisory），**从未审计 move-confirm layer 的 expiry interception**；A1.25 inventory-F1 零 finding（UC-INV-01/03/04/05 移动单主链与追溯，不涉效期拦截）；P0-MA2-020 是 StockBalance UK（并发兜底，不同控制点）；P2-MA2-028 是 reverse businessDate（costing 维度，不同控制点）；P1-MA4-021 是测试有效性（多币种/业财异常，不含效期拦截维度）。RC 系列对 inventory 效期拦截为零命中——本 finding 是首次定级。 | §2 P1①（功能完全缺失——L1 `:113` 字面要求未实现）+ §2 P1⑤（验收标准零断言——拦截路径无测试因实现缺失） | MR1（R1.0 展开为 RC-R1.n）/ §4 (iii) product-scope 修订（若人工确认裁剪） | todo（本审计仅登记，不实施修复；**先须人工确认 product-scope 是否裁剪效期拦截功能**：若裁剪 → 按 §4 (iii) 改 product-scope 真相源非降级；若未裁剪 → P1 强制实现。修复 = `ErpInvStockMoveProcessor.validateAvailable:116-136` 增 per-line expiry 守卫[经 `IErpMdMaterialBiz` 查 `material.isBatchManaged()` → 若强制且 line.batchNo 非空 → 经 `IErpInvBatchBiz` 查 `batch.expiryDate` < today 抛新增 `ERR_BATCH_EXPIRED` 错误码 + ARG_BATCH_NO/EXPIRY_DATE 参数] + 可选 `ErpInvBatchBizModel` 增 ACTIVE→EXPIRED 状态迁移方法[定时 job 或出库时即时判定] + `ErpInvErrors` 增 `ERR_BATCH_EXPIRED` + 补 dedicated 测试[过期批次出库被拦截 + 强制 isBatchManaged=false 不拦截 + 非 batch 物料跳过]；**纯 BizModel/Processor + ErrorCode + 测试补充，按 roadmap 预授权类目[代码逻辑修复]可自动执行，不触发 §5 ask-first**[不触及 ORM/会计过账核心路径，仅消费既有 isBatchManaged + expiryDate 字段]） |

### 既有 resolved finding HEAD 复核

| Finding ID | arm-index 行 | 本切片 HEAD 复核结论 |
|-----------|-------------|--------------------|
| **P2-MA2-028**（reverse uses today() not businessDate） | `:525`（A2.4 watch-only） | **R6.9 已 fix 确认**（与 A1.25 复核一致）：HEAD `ErpInvStockMoveReverseProcessor.java:51` 优先用 `original.getBusinessDate()`。**维持 watch-only**（不在本切片 UC 范围——UC-INV-02/06/09 不涉 reverse costing 维度） |
| **P0-MA2-020**（StockBalance 自然键 UK） | `:222`（done, plan 2026-07-28-1249） | **done 确认**（与 A1.25 复核一致）：HEAD `app-erp-inventory.orm.xml:415 UK_INV_STOCK_BALANCE_NATURAL` on `(orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId)` 物理存在——**`batchNo` 是 UK 维度之一**，UC-INV-06 批次维度余额隔离由本 UK 物理支撑；`updateBalanceWithRetry:256-328` 三分支 + `flushAndCheckConflict` UK violation catch + evict + reload + retry。**UC-INV-02 拒绝路径 + UC-INV-09 负库存路径 + UC-INV-06 批次维度并发安全由本 UK 兜底**。维持 done |
| **P1-MA4-021**（pur+sal+inv 测试有效性） | `:647`（resolved R2.14） | **resolved R2.14 确认**（与 A1.25 复核一致 + 范围澄清）：P1-MA4-021 范围 = 多币种零覆盖 + 业财异常悬挂零覆盖 + STANDARD 红冲成本不变量 + SPECIFIC 成本调整 + rollbackReceive 不对称 + SalReversalListener 3/4 回退 + settle 三单匹配二次门禁 + 到岸成本反向悬挂（8 子项）。**范围不含效期拦截维度**（与 trace chain 同——UC-INV-06 ④效期拦截测试缺失是因实现缺失，非"实现存在测试不足"，不属 P1-MA4-021 范围）；UC-INV-02/09 测试强度经本切片 §3 证实为强断言（错误码 + 精确数值 + 5 CostingStrategy 矩阵） |
| **P1-MA3-062**（Processor per-mutation split） | `:423`（resolved R6.4） | **R6.4 done 确认**（与 A1.25 复核一致）：HEAD StockMove 遵循 Facade BizModel + per-mutation Processor 两层模式（5 per-mutation Processor），`validateAvailable:116-136` 是 `doConfirm` 的 protected step 方法，符合 `processor-extension-pattern.md` 定制余地范式 |
| **P1-MA4-020**（到岸成本反向过账悬挂） | `:646`（resolved R1.16） | **resolved R1.16 确认**（与 A1.25 复核一致）：`ErpInvLandedCostProcessor.dispatchReverseFailureAlert` 告警闭环已落地。**不在本切片范围**（UC-INV-02/06/09 不含到岸成本），仅记录邻近 reverse path HEAD 复核无回退 |

### 双向可追溯

- **1 项新 P1 finding**（`P1-RC-031`）入 arm-index §RC 发现追踪分区
- 本切片报告 ID `2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock` 追加至 arm-index §报告清单 + §RC 交叉引用注记
- 既有 resolved finding（P2-MA2-028 / P0-MA2-020 / P1-MA4-021 / P1-MA3-062 / P1-MA4-020）HEAD 复核结论记录于本报告 §6，arm-index 对应行维持原状态（无须回填——本切片是需求契约视角复核，不改变 audit-remediation 侧 finding 状态）

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。

| SP# | 存疑点 | 触发条件 | 验证方法 |
|-----|--------|---------|---------|
| **SP-1** | **allow-negative-stock=true 下并发出库的实际余额下限行为**：UC-INV-09 默认 false 安全，但 config 翻转 true 后并发出库无下界守卫（`validateAvailable:117-119` 短路所有检查）。`StockMoveBookkeeper.updateBalanceWithRetry:256-328` + 乐观锁保证不超扣（A2.17 §13 PASS），但极端场景（多个出库同时扣减已为负的余额）下 totalQuantity 可能深度为负。L1 `:162` 仅称「现有量变负」未限定下限 | config=true + 余额已为负 + 并发出库 | A4.1 运行时探针：seed 余额 = -5 + allow-negative-stock=true + 2 并发出库（各扣 10）→ 断言最终余额 = -25（无下界）+ 乐观锁重试行为 + ledger 流水一致性 |
| **SP-2** | **batchTrace 在跨域 move 链下的聚合正确性**：UC-INV-06 ② `TraceChainQuery.batchTrace:168-192` 聚合 `findLinesByBatch` + `findLedgersByBatch` → moveIds → `findActiveMove`（filter delVersion=0）。跨域 move 链（采购入库 → mfg 领料/完工 → sales 出库）下，若不同域的 move line 共享同一 batchNo（批次继承语义），聚合是否完整覆盖全链 | 跨域 move 链共享 batchNo（如 BATCH-001 经采购入库→mfg 领料→mfg 完工[新批次]→sales 出库） | A4.1 运行时探针：构造 BATCH-001 → 采购入库（INCOMING）→ mfg 领料（OUTGOING，batchNo=BATCH-001）→ mfg 完工（INCOMING，新 batchNo=BATCH-002）→ sales 出库（OUTGOING，batchNo=BATCH-002）→ 断言 batchTrace("BATCH-001") 返回采购入库 + mfg 领料 2 nodes（不含 BATCH-002 节点）+ batchTrace("BATCH-002") 返回 mfg 完工 + sales 出库 2 nodes |
| **SP-3** | **expiryDate 字段在 ORM 存在但无 writer 时的默认值行为**：UC-INV-06 ④ `ErpInvBatch.expiryDate`（ORM `:908 EXPIRY_DATE DATE`）字段存在；但 `ErpInvBatchBizModel` 是 15 行 CRUD 桩——无ACTIVE→EXPIRED 状态迁移、无 expiry scheduler。expiryDate 由 `BatchGenealogyWriter.newBatchEntity:159-168`（mfg 完工时）写入，但其他入库路径（采购入库/销售退货入库）**是否写 expiryDate**？不写则 expiryDate=null，未来若实现效期拦截须定义 null 语义（跳过 / 视为永不过期 / 视为立即过期） | 非 mfg 完工入库路径（如采购入库 batch 创建） | A4.1 运行时探针：seed 采购入库移动单带 batchNo=BATCH-PUR-001 + 检查 `ErpInvBatch` 是否自动创建 + expiryDate/shelfLifeDays 字段默认值 → 断言 null 或具体值；为 MR1 P1-RC-031 修复提供 null 语义设计输入 |
| **SP-4** | **MR1 修复落地后 reserved/available 一致性（与 A1.8 SP-3 同根因）**：UC-INV-02 `validateAvailable` + `applyReservation` 在 `doConfirm` 内顺序执行（校验 → 占预留）。当前实现下，UC-INV-02 拒绝路径不进入 applyReservation（满足"余额不变"）。未来若 MR1 P1-RC-031 修复在 `validateAvailable` 内增 expiry check（早于 applyReservation），expiry 拒绝路径同样不进入 applyReservation——一致性保持。但若修复扩展到 `doComplete`（DONE 时再校验 expiry），须确认 reserved 已被 release（避免假阴性） | MR1 P1-RC-031 修复实现的拦截点选择（doConfirm vs doComplete） | A4.1/MR1 修复 plan 自身审计：mock `validateAvailable` 抛 `ERR_BATCH_EXPIRED` → 断言 `applyReservation` 未执行 + reservedQuantity 不变 + balance 不变 |

**无其他静态存疑点。** 候选缺口 #1/#2 静态即可定论（P1-RC-031）；#3/#4/#5/#6 静态即可定论（接受）；UC-INV-02/09 主路径行为经 MA2 三重证实无须运行时复验。SP-1/SP-2/SP-3/SP-4 交 MA4 A4.1 运行时展开。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（UC-INV-02/09 接受，UC-INV-06 效期拦截缺失属 §2 P1① 功能完全缺失类——非活跃数据破坏[库存守恒]/非核心循环断裂[O2C/P2P 完整]/非会计过账破坏[GL 平衡]）。**不触发 MR0**，无 R0.n 实体行追加。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（1 项新 P1）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6——P1-RC-031 裁决为"新建"，列明与 A2.11 dashboard advisory / A1.25 / P0-MA2-020 / P2-MA2-028 / P1-MA4-021 / P1-MA4-020 的差异依据）。既有 resolved finding HEAD 复核结论记录于 §6，无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-03-1200 HEAD）

| 规则 | 描述 | actual | baseline | 状态 |
|------|------|--------|---------|------|
| R1a | dao().saveEntity (BizModel) | 0 | 0 | ✅ |
| R1b | dao().updateEntity (BizModel) | 0 | 0 | ✅ |
| R1c | dao().getEntityById (BizModel) | 0 | 0 | ✅ |
| R1d | dao().findAllByQuery (BizModel) | 14 | 17 | ✅ |
| R2a | BizModel daoFor(ErpMd*) | 34 | 34 | ✅ |
| R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | ✅ |
| R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | ✅ |
| R2d | Processor daoFor(ErpMd*) | 34 | 34 | ✅ |
| R3 | new Erp*() 构造实体 | 5 | 5 | ✅ |
| R4 | extends RuntimeException | 0 | 0 | ✅ |
| R5 | @Inject private | 0 | 0 | ✅ |
| R6 | @Transactional in BizModel | 2 | 2 | ✅ |
| R7 | System.currentTimeMillis() | 0 | 0 | ✅ |
| R8 | Processor 无 xbiz 接线 | 0 | 0 | ✅ |
| R10 | REQUIRES_NEW 事务 | 6 | 6 | ✅ |
| R11 | Processor 重复状态判断方法 | 0 | 0 | ✅ |
| R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | ✅ |
| R12b | 共享内核 import PostingEvent | 66 | 66 | ✅ |
| R12c | 共享内核 import AcctSchemaResolver | 40 | 40 | ✅ |

**全 19 规则 actual ≤ baseline（零漂移），与 A1.25 同期实测一致。** 本审计为只读审计（无代码/ORM/api.xml/view.xml/真相源变更），checker 无回归风险。

---

## 9. 与 MA2 报告差异增量声明

> §去重协议：本切片复用既有 MA2 报告已证实行为，只补需求契约↔实际行为差异。

### 复用的 MA2/A4 既有证据

| 报告 | 复用维度 | 本切片复用结论 |
|------|---------|---------------|
| **A2.11**（`2026-07-28-0400-arm-ma2-inventory-state-machine.md`） | 移动单状态机 + 出库 approve 可用量校验 + 业务单据双轴 + 所有权转移 + 批次/序列号/预留 | PASS（5 迁移守卫 + doConfirm→validateAvailable 强制[available < required] 抛 ERR_AVAILABLE_INSUFFICIENT + 反向单可用量校验 + 幂等键 + 存货过账事件解耦 + 跨域写经 Facade 全证实）；`:459` 出库 approve 可用量校验已落实——**经 `IErpInvStockMoveBiz.generateMove→ErpInvStockMoveProcessor.doConfirm→validateAvailable` 强制**；`:459` 负库存放行权限缺失——**证伪**（config 默认 false + NopSysVariable 运行时覆盖权限保护） |
| **A2.9**（`2026-07-28-0400-arm-ma2-sales-state-machine.md`） | UC-INV-02 跨域传播至 sales 域（出库 approve 销售独有约束） | PASS（`:451` 出库 approve 可用量校验销售独有约束已落实——`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245 → IErpInvStockMoveBiz.generateMove` Facade，validateAvailable 失败 → exception bubble → `__approve` mutation 回滚 → approveStatus 保持原值） |
| **A4.5**（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`） | `ErpInvStockMoveProcessor` / `validateAvailable` 代码质量 + 跨域 Facade 合规性 | PASS（A4.5 §代码质量段：编排健壮性 / 异常规范化 / BigDecimal 货币类型安全 / 拒绝路径在状态翻转前落实 四面扎实） |
| **A2.17**（`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`） | UC-INV-02 拒绝路径 + UC-INV-09 负库存路径并发安全（@Version + UK + retry） | PASS（`UK_INV_STOCK_BALANCE_NATURAL` P0-MA2-020 已落地——`batchNo` 是 UK 维度之一，支撑 UC-INV-06 批次维度并发隔离 + `TestErpInvConcurrentDeduct` 6 测试强覆盖含 3 真实多线程） |

### 本切片只补的"需求契约↔行为"差异增量

1. **UC-INV-06 ④⑤ 效期拦截缺失裁决**（A2.11 未从 L1 字面"出库确认失败"视角审视）：A2.11 `:459` 仅在 dashboard context 列出 `ErpInvBatch.expiryDate`（advisory），**从未审计 move-confirm layer 的 expiry interception**。本切片首次从需求契约视角定级——L1 `use-cases.md:113` 字面要求"出库确认失败（批次过期拦截）"完全未实现，新建 P1-RC-031。
2. **UC-INV-06 ④ isBatchManaged 条件分支未被消费裁决**（A2.11/A4.5 未从 L1 字面"物料.批次管控 == 强制"视角审视）：`ErpMdMaterial.isBatchManaged` 字段存在但 `validateAvailable` 从不查询——L1 条件分支完全未被消费，与效期拦截同根因，合并入 P1-RC-031 描述（按 §7 复用不新建独立编号）。
3. **UC-INV-06 完工入库新批次行为确认**（A2.11 未从 L1 字面"完工入库生成新批次（不继承）"视角审视）：mfg `BatchGenealogyWriter.writeOnCompletion:64/151-168` 经 `ErpMfgWorkOrderProcessor:326` 调用——按 wo.code 派生新 batchNo（非继承），创建 OPEN 状态 ErpInvBatch（best-effort 不阻断完工入库）。
4. **UC-INV-06 批次继承行为确认**（A2.11 未从 L1 字面"领料移动单.批次 == 入库移动单.批次"视角审视）：mfg `MaterialIssueStockMoveBuilder:66 req.setBatchNo` + `ErpInvStockMoveProcessor.newLines:202 setBatchNo` + `StockMoveBookkeeper:154/173/217` 余额/流水键——inventory 域忠实持久化调用方传入的 batchNo（批次继承是调用方契约，inventory 域数据层满足 L1 字面）。
5. **UC-INV-09 默认 false 安全性裁决**（A2.11 未从 L1 字面"若配置 == false：拒绝"视角审视）：`isNegativeStockAllowed:285-288 AppConfig.var(..., Boolean.FALSE)` 字面 default false，与 L1「若 配置 == false：拒绝（回到 UC-INV-02）」语义一致。

### 未重做的 MA2 维度（§去重协议）

- 移动单状态机迁移守卫（A2.11 §维度 1/2 PASS）——不重审
- 出库 approve 可用量校验行为（A2.11 `:459` + A2.9 `:451` PASS）——不重审
- 负库存放行行为（A2.11 `:459` 证伪 PASS）——不重审
- 库存余额并发安全（A2.17 §13 PASS + P0-MA2-020 done）——不重审
- `ErpInvStockMoveProcessor`/`validateAvailable` 代码质量（A4.5 PASS）——不重审
- 跨域 `IErpInvStockMoveBiz` Facade 调用合规性（A4.5 PASS）——不重审

---

## 9 段完整性自检

- [x] §1 需求契约原文（3 UC 逐字引用）
- [x] §2 实现证据（L3 代码路径含行号 + 跨域调用链）
- [x] §3 测试证据（L4 断言强度注明 + UC-INV-06 效期拦截测试缺失记录）
- [x] §4 运行时行为证据（L5 复用 MA2/A4 + UC-INV-06 效期拦截行为缺失记录）
- [x] §5 符合性结论（五级追踪矩阵 + 3 UC 逐结论 + 6 候选缺口逐裁决）
- [x] §6 与 arm-index 衔接（1 新 P1 finding P1-RC-031 + 5 resolved finding HEAD 复核）
- [x] §7 静态存疑点清单（SP-1 负库存下限 + SP-2 batchTrace 跨域 + SP-3 expiryDate null 语义 + SP-4 MR1 一致性）
- [x] §8 过程纪律自检（checker actual vs baseline 19 规则全 ✅ + 独立性 + 交叉去重声明）
- [x] §9 与 MA2 报告差异增量声明（复用 4 报告 + 5 差异增量 + 6 未重做维度）
