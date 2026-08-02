# MA2 库存核算一致性审计报告（A2.4 — 成本层+余额+流水三方对账）

> Audit Status: closed
> 里程碑：MA2（业财端到端 / 业务正确性层）
> 域/功能模块：inventory / 存货成本核算（7 costMethod + 三方对账 + reclose + 成本调整 + 到岸成本 + PPV）
> 审计 plan：`docs/plans/2026-07-27-2211-1-audit-remediation-ma2-inventory-costing-consistency.md`
> 来源 finding（运行时影响复核）：P1-MA1-022 / P0-MA1-021（done）/ P1-MA2-017 / P1-MA2-002
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 审计日期：2026-07-27
> 审计者：主代理（独立子代理已完成草案审查 + 证据采集协作）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（三方对账破缺 / reclose 掩盖缺陷 / SPECIFIC 未来成本消耗致余额错误 / 红冲成本不变量运行时破缺） | **0** | 无即时通道修复 |
| **P1**（新登记） | **2** | P1-MA2-023（SPECIFIC 历史成本守卫缺失）/ P1-MA2-024（STANDARD 红冲成本不变量跨重估破缺） → 待 MR1 |
| **P2**（watch-only） | **5** | P2-MA2-026 ~ P2-MA2-030（测试覆盖缺口 + 已文档化简化边界） |
| MA1/MA2 finding 运行时复核 | 4 项 | P1-MA1-022/P0-MA1-021/P1-MA2-017/P1-MA2-002 — **无升级**（详见 §6） |

**整体裁决**：库存核算三方对账在正常路径下**成立且经证据确认**（7 costMethod 逐方法 + 成本调整哨兵流水 + 到岸成本 + PPV 分离）。`reclosePeriodCosts` 兜底覆盖范围与不动 MOVING_AVERAGE/STANDARD 的假设**经核验为合理设计**（非掩盖缺陷）。CostAdjustmentService AP-01 反模式（owner doc §6 已登记）维持治理层 finding 不升级。`passes multi-dimensional audit`（带 2 项 P1 + 5 项 P2 残留风险）。

---

## 1. 审计范围与方法覆盖矩阵

### 1.1 审计对象（实仓逐项核实）

| 组件 | 文件 | 行号 | 审计状态 |
|---|---|---|---|
| 7 种 CostingStrategy | `module-inventory/erp-inv-service/.../costing/*CostingStrategy.java` | 全文逐行 | ✅ |
| 策略接口 | `CostingStrategy.java` | 18-38 | ✅ |
| Context 接口 + 实现 | `BookingContext.java` / `StockMoveBookkeeper.java`（implements） | 全文 | ✅ |
| 分派器 + 跨仓成本桥 + 乐观锁 | `StockMoveBookkeeper.java` | 56-322 | ✅ |
| Resolver（解析链 + 兜底 + 白名单） | `CostMethodResolver.java` | 22-85 | ✅ |
| STANDARD 成本源 | `StandardCostResolver.java` | 42-124 | ✅ |
| 成本调整引擎 | `CostAdjustmentService.java` | 51-319 | ✅ |
| 到岸成本分摊引擎 | `LandedCostAllocationEngine.java` | 35-174 | ✅ |
| 期末成本兜底 | `ErpInvCostingBizModel.java` | 54-327 | ✅ |
| 存货过账分派器 + PPV | `InvPostingDispatcher.java` | 37-292 | ✅ |
| 成本调整过账分派器 | `CostAdjustmentPostingDispatcher.java` | 37-117 | ✅ |
| 红冲流程 | `ErpInvStockMoveProcessor.reverse` / `ErpInvStockMoveBizModel.reverse` | 115-154 / 60-64 | ✅ |
| 三方实体 schema | `ErpInvStockBalance` / `ErpInvStockLedger` / `ErpInvCostLayer`（_gen） | 全文 | ✅ |
| 配置 + 常量 | `ErpInvConfigs.java` / `ErpInvConstants.java` | 全文 | ✅ |

### 1.2 测试覆盖矩阵（7 类 costMethod + 辅助）

| 测试类 | 三方对账聚合断言 | 红冲不变量断言 | 备注 |
|---|---|---|---|
| `TestErpInvFifoCosting` | ✅（唯一：`sumRemainingCost` @ 329-337 比对 balance.totalCost，容差 0.01） | ✅ `testReverseRestoresCostInvariant` 163-204 | 全策略中覆盖最完整 |
| `TestErpInvLifoCosting` | ❌ | ❌ | 仅正路径 + 边界 |
| `TestErpInvSpecificCosting` | ❌ | ❌ | 仅正路径 + 边界（4 测试） |
| `TestErpInvBatchCosting` | ❌ | ❌ | 仅正路径 + 边界 |
| `TestErpInvStandardCosting` | ❌ | ❌ | STANDARD 出库 + PPV 3 场景 + 缺失拒绝；**无红冲** |
| `TestErpInvWeightedAverageCosting` | ❌（无层语义，不适用层对账） | ⚠️ 部分 `testReversePreservesFrozenAvgCost` 120-142（仅 avgCost 冻结） | 期中冻结正确 |
| `TestErpInvCostingDispatch` | ❌ | ❌ | 分派链 + 总开关兜底 + 内部调拨（**仅 MA**） |
| `TestErpInvFifoCostingEndToEnd` | ❌ | ❌ | reclose 2 场景（normal no-op + rebuild missing layer） + 全链过账 |
| `TestErpInvCostAdjust` | ❌ | ⚠️ `testReverseRollsBackBalanceAndVoucher` 262-296（**仅 MA + 余额+凭证**；无 FIFO 三方） | 8 场景 |
| `TestErpInvLandedCost*`（3 类） | ❌ | ⚠️ `TestErpInvLandedCostReversal` 87-143（**仅 MA**；FIFO 边界 successor） | 分摊引擎 + E2E + 红冲 |

---

## 2. 三方对账不变量逐方法裁决（核心）

> 三方 = 成本层 `ErpInvCostLayer` + 余额 `ErpInvStockBalance` + 流水 `ErpInvStockLedger`
> 不变量 1：Σ `ErpInvStockLedger.quantity`（按 物料×仓库×库位×批次×账套 分组，含 `moveId=0` 成本调整哨兵流水）== `ErpInvStockBalance.totalQuantity`
> 不变量 2：Σ `ErpInvStockLedger.totalCost` == `ErpInvStockBalance.totalCost`
> 不变量 3（层基方法）：Σ `ErpInvCostLayer.remainingQuantity × unitCost` == `ErpInvStockBalance.totalCost`

| costMethod | 不变量 1（quantity） | 不变量 2（totalCost） | 不变量 3（层基） | 证据 / 裁决 |
|---|---|---|---|---|
| **MOVING_AVERAGE** | ✅ 成立 | ✅ 成立 | N/A（avgCost 语义，无层） | `MovingAverageCostingStrategy:36-89`：入库 `totalQuantity += qty` / `totalCost += lineTotalCost` / `avgCost` 重算；出库对称扣减；流水 `quantity` 带符号。成本调整哨兵流水 `quantity=0`（`CostAdjustmentService:260`）不影响 quantity 和。 |
| **WEIGHTED_AVERAGE** | ✅ 成立 | ✅ 成立（reclose 后） | N/A（无层） | `WeightedAverageCostingStrategy:46-94` + `ErpInvCostingBizModel.recomputeWeightedAverageOutgoing:187-221`：期中出库按期初 avgCost 暂估，月末 reclose 重算全月实际加权平均并**同步刷流水+余额**（动 totalCost + avgCost）。期中 totalCost 不变量成立（暂估一致）；月末 reclose 后不变量成立（重算一致）。 |
| **FIFO** | ✅ 成立 | ✅ 成立 | ✅ 成立（容差 0.01） | `FifoCostingStrategy:61-148`：入库追加层 + `balance.totalCost += lineTotalCost`；出库多层消耗 Σ `take × unitCost` = `totalCost`，扣 `balance.totalCost`。`TestErpInvFifoCosting.testReverseRestoresCostInvariant:163-204` 经 `sumRemainingCost:329-337` 断言 Σ 层成本 == balance.totalCost（容差 0.01 承认 scale 6→4 漂移）。**唯一经测试断言的层基不变量**。 |
| **LIFO** | ✅ 成立 | ✅ 成立 | ✅ 成立（无测试断言） | `LifoCostingStrategy:52-138`：结构同 FIFO，唯一差异 `incomingDate DESC` 排序。算法对称 → 不变量同 FIFO 成立。**但无聚合不变量测试**（P2-MA2-026）。 |
| **BATCH** | ✅ 成立 | ✅ 成立 | ✅ 成立（无测试断言） | `BatchCostingStrategy:57-145`：按 batchNo 过滤 + 批次内 `incomingDate ASC`。算法对称 → 不变量同 FIFO 成立。**无聚合不变量测试**。 |
| **SPECIFIC** | ✅ 成立 | ✅ 成立 | ⚠️ 成立但有**未来成本消耗风险** | `SpecificCostingStrategy:52-141`：按 batchNo/serialNo 精确匹配。算法对称 → 正常路径不变量成立。**但 `findSpecificLayers:168-188` 无 `le(incomingDate, businessDate)` 过滤**（FIFO:199/LIFO:185/BATCH:190 均有）→ 同 batchNo 的 future-dated 入库层可能被今日出库消耗（违反历史成本原则）。→ **P1-MA2-023** |
| **STANDARD** | ✅ 成立 | ✅ 成立（正常路径） | N/A（avgCost=standardCost 语义） | `StandardCostingStrategy:36-87`：入库 `avgCost=standardCost` / `totalCost += standard × qty`；出库对称扣减。**正常路径不变量成立**。**但红冲路径破缺**：`onIncoming:43` 忽略传入的 `unitCost` 参数，经 `standardCostResolver.resolve()` 重解析当前 FIRMED rollup — 若红冲期间 FIRMED rollup 经 STANDARD_REVALUATION 变更，反向入库用新标准成本，与原出库扣减的旧标准成本不一致 → 红冲后 balance.totalCost 漂移 `(newStd - oldStd) × qty`。→ **P1-MA2-024** |

### 2.1 成本调整哨兵流水纳入聚合的正确性（控制点 6 子项）

成本调整写 `ErpInvStockLedger` 行 `quantity=0, moveId=LEDGER_MOVE_ID_COST_ADJUST(0), totalCost=adjustAmount`（`CostAdjustmentService.writeLedger:248-270`）。三方对账聚合时：

- 不变量 1（quantity）：✅ 哨兵 `quantity=0` 不影响 quantity 和。
- 不变量 2（totalCost）：✅ 哨兵 `totalCost=adjustAmount` 与 `balance.totalCost += adjustAmount`（`applyAverageLike:127` / `applyFifo:140`）对称，纳入聚合后一致。
- FIFO delta 调整层（`incomingMoveId=-lineId` 哨兵，`appendFifoAdjustLayer:149-171`）：✅ 作为正常层纳入 Σ `remaining × unitCost`，与 `balance.totalCost += adjustAmount` 一致。

**裁决**：成本调整哨兵流水与三方对账的一致性**经代码核实正确**。但**无聚合不变量测试断言此路径**（`TestErpInvCostAdjust` 8 场景均为单状态 post-action 断言）→ P2-MA2-026。

### 2.2 PPV 与三方对账的分离（控制点 6 子项）

PPV 经 `InvPostingDispatcher.dispatchPurchasePriceVariance:103-150` 捕获，过账业务类型 `PURCHASE_PRICE_VARIANCE`（`PurchasePriceVarianceAcctDocProvider`），**仅过凭证，不触碰 balance/ledger/layer**。STANDARD 入库流水 `ledger.unitCost = standardCost`（非 actual），`balance.totalCost += standard × qty`。三方对账在 standard cost 维度自洽。PPV 差异独立过账到 1404 材料成本差异科目。

**裁决**：✅ PPV 与存货估价分离正确，不污染三方对账。`TestErpInvStandardCosting.testPpvUnfavorable/FavorableActualLessThanStandard:123-174` 验证 inLedger.unitCost=standard（非 actual）+ PPV 凭证独立生成。

### 2.3 到岸成本分摊后入库层 unitCost 更新的余额一致性（控制点 6 子项）

到岸成本经 `ErpInvLandedCostProcessor.approve` 编排：(1) 引擎分摊 → (2) 创建 `ErpInvCostAdjust(LANDED_COST_SUPPLEMENT)` + 行 → (3) 调 `CostAdjustmentService.applyCostAdjust` 直更成本层（MA: avgCost + totalCost；FIFO: delta 层）。三方对账经 §2.1 的成本调整哨兵流水路径保持一致。

**裁决**：✅ 到岸成本分摊的三方一致性经 `TestErpInvLandedCostEndToEnd` 4 场景 + `TestErpInvLandedCostReversal` 2 场景覆盖（MA 主路径）。FIFO 物料的到岸成本分摊 + 红冲边界场景为 costing-methods.md:66 已登记 successor（P2-MA2-029 维持）。

---

## 3. 八个已识别控制点逐项裁决

### 控制点 1：三方对账不变量 → ✅ 通过（带 P1 残留）

见 §2 逐方法裁决。**核心结论**：7 costMethod 在正常路径下三方对账成立。SPECIFIC 未来成本消耗风险（P1-MA2-023）与 STANDARD 红冲跨重估漂移（P1-MA2-024）为残留 P1，不影响正常路径不变量。

### 控制点 2：7 costMethod 策略正确性 → ✅ 通过

逐方法核验算法对齐 owner doc `costing-methods.md`：

| 方法 | 算法核验 | 证据 |
|---|---|---|
| MOVING_AVERAGE | ✅ 入库重算 `avgCost=(oldTotalCost+lineTotalCost)/(oldTotalQty+qty)` scale 6 HALF_UP；出库 `unitCost=avgCost` | `MovingAverageCostingStrategy:36-89` |
| WEIGHTED_AVERAGE | ✅ 期中入库累加但不重算 avgCost（冻结期初）；月末 reclose 重算 | `WeightedAverageCostingStrategy:46-94` + `ErpInvCostingBizModel:187-221`；`testIncomingAccumulatesButDoesNotRecomputeAvgCost:64-82` 断言 avgCost=ZERO 冻结 |
| FIFO | ✅ `incomingDate ASC` + 多层消耗加权 + scale 6 中间值 / scale 4 输出（`roundCost`） | `FifoCostingStrategy:178-205` `findFifoLayers` 排序 + `le(incomingDate,businessDate)` 守卫 |
| LIFO | ✅ `incomingDate DESC` + 同 FIFO 算法 | `LifoCostingStrategy:164-192` `.reversed()` |
| STANDARD | ✅ `StandardCostResolver` 解析 FIRMED rollup + PPV 分离 | `StandardCostingStrategy:36-87` + `StandardCostResolver:57-71` |
| SPECIFIC | ⚠️ batchNo/serialNo 精确匹配正确，**但缺历史成本守卫** | `SpecificCostingStrategy:168-188` → P1-MA2-023 |
| BATCH | ✅ batchNo 过滤 + 批次内 `incomingDate ASC`（批次内 FIFO） | `BatchCostingStrategy:171-196` |

### 控制点 3：红冲成本不变量 → ⚠️ 通过（带 P1 残留）

`ErpInvStockMoveProcessor.reverse:144` 经 `rl.setUnitCost(nz(ol.getUnitCost()))` 原值回传。逐方法族：

| 方法族 | 红冲不变量 | 证据 |
|---|---|---|
| MA | ✅ avgCost 回传重算（reverse 入库重算 avgCost） | `MovingAverageCostingStrategy.onIncoming` 对称；`TestErpInvCostAdjust.testReverseRollsBackBalanceAndVoucher:262-296` 验证余额回退 |
| 层基 FIFO | ✅ 出库加权 unitCost 回写 line → reverse 入库追加新层（Decision (a)） | `TestErpInvFifoCosting.testReverseRestoresCostInvariant:163-204` 断言 Σ 层成本恢复（容差 0.01） |
| 层基 LIFO/BATCH/SPECIFIC | ⚠️ 算法对称应成立，**但无红冲测试** | 无 reverse 测试 → P2-MA2-026 |
| STANDARD | ❌ **跨重估破缺** | `StandardCostingStrategy.onIncoming:43` 忽略传入 unitCost，重解析 rollup → P1-MA2-024 |
| WEIGHTED_AVERAGE | ✅ 期中 avgCost 冻结保持 | `testReversePreservesFrozenAvgCost:120-142` |

### 控制点 4：reclosePeriodCosts 一致性 → ✅ 通过

`ErpInvCostingBizModel.reclosePeriodCosts:73-116` 覆盖范围经核验：

1. **正常路径 no-op**：✅ `testReclosePeriodCostsNormalDataIsNoOp:100-111` 断言 `recomputedIncomingLayers=0` + `recomputedOutgoingLedgers=0`（costing 全程启用时 DONE 已维护层/流水）。
2. **异常路径兜底**：✅ `testReclosePeriodCostsRebuildsMissingLayer:113-140` 断言 costing-disabled 期入库后 reclose 补建缺失层（`recomputedIncomingLayers=1`，层字段精确匹配）。
3. **MOVING_AVERAGE / STANDARD 不在 reclose 范围**：✅ **假设合理**。MA 在每次入库 DONE 时即时重算 avgCost（无后续校正窗口）；STANDARD 在 DONE 时解析 FIRMED rollup，后续变更经 PPV/STANDARD_REVALUATION 独立通道（非 reclose 职责）。`LAYER_BASED_METHODS` Set（`:67-71`）+ WEIGHTED_AVERAGE 分支（`:104`）的排除设计**非掩盖缺陷**——若 MA/STANDARD 流水出现 null/zero unitCost，属上游 DONE 记账异常（应在上游修复），reclose 不兜底是正确的职责边界。→ P2-MA2-030（边界场景 watch-only，非缺陷）。
4. **出库重算「只刷流水不动余额」(FIFO) vs「动流水+余额」(WAM)**：✅ 差异正确。FIFO 路径 `recomputeOutgoingCogs:148-179` 仅刷 ledger（余额在原 DONE 已扣，避免双计）；WAM 路径 `recomputeWeightedAverageOutgoing:187-221` 同步刷 ledger + 余额（因 WAM 期中按期初暂估，月末需补差）。

### 控制点 5：跨仓调拨成本桥 → ⚠️ 通过（带 P2 残留）

`StockMoveBookkeeper.bookCompletion:113-115`：

```java
BigDecimal carriedCost = strategy.onOutgoing(move, line, acctSchemaId, this);
strategy.onIncoming(move, line, acctSchemaId, carriedCost, this);
```

- **MA 路径**：✅ 源仓扣 avgCost → 目标仓以同 avgCost 入库重算。`TestErpInvCostingDispatch.testInternalTransferCarriesCostAcrossWarehouses:128-149` 覆盖（MA seed）。
- **层基路径**（FIFO 出库加权 unitCost → 目标仓追加新层）：⚠️ 算法正确（`carriedCost` 为源仓 FIFO 加权出库 unitCost，目标仓 `onIncoming` 以此 unitCost 追加新层），**但无测试覆盖** → P2-MA2-027。
- **STANDARD 路径**：⚠️ 算法正确（`carriedCost` 为源仓 standardCost 出库 unitCost，但目标仓 `onIncoming` 忽略 carriedCost 重解析 rollup——同 P1-MA2-024 根因，跨仓场景下若目标仓物料与源仓物料相同则一致，不同则各取各的标准成本，业务正确）。

**裁决**：跨仓成本桥算法正确，三方对账在跨仓后仍成立（两仓余额合计 + 两仓流水合计 + 层合计）。测试覆盖缺口 P2。

### 控制点 6：成本调整/到岸成本/PPV 与三方对账一致性 → ✅ 通过

见 §2.1 / §2.2 / §2.3。三项均经代码核实正确纳入三方对账聚合。

### 控制点 7：rounding 漂移 → ✅ 通过（带 P2 残留）

策略内 `SCALE=6` 中间除法（`FifoCostingStrategy:47` 等），输出经 `ErpInvConfigs.roundCost()` scale 4（`ErpInvConfigs:18-20`，config `erp.inv.costing.unit-cost-scale` 默认 4）。

- **FIFO 多层消耗加权**：layer.unitCost 已 scale 4，`takeCost = take × unitCost`（scale 4 × scale N）；`totalCost` Σ 后 `divide(qty, 6, HALF_UP)` 再 `roundCost` scale 4。单次操作漂移 ≤ 0.0001。
- **反复入库-出库-红冲后 Σ 层成本 vs 余额 totalCost**：`TestErpInvFifoCosting.testReverseRestoresCostInvariant:186-198` 以 0.01 容差承认（合理——会计场景容差普遍接受）。
- **成本调整 delta 层 unitCost = 新旧差值**：`applyFifo:146` `newUnitCost.subtract(oldUnitCost)` 经 `roundCost` scale 4；reverse `removeFifoAdjustLayer:194-202` 按 `-lineId` 哨兵物理删除整层，无 rounding 回退问题。

**裁决**：rounding 漂移在会计容差内可控，不致余额与流水永不相等。跨方法累积漂移审计缺失 → P2-MA2-026（测试覆盖缺口）。

### 控制点 8：SPECIFIC 历史成本守卫缺失 → ❌ P1-MA2-023

`SpecificCostingStrategy.findSpecificLayers:168-188` 与其他 3 层基方法对比：

| 方法 | `le(incomingDate, businessDate)` 过滤 | 行号 |
|---|---|---|
| FIFO | ✅ 有 | `FifoCostingStrategy:198-200` |
| LIFO | ✅ 有 | `LifoCostingStrategy:184-186` |
| BATCH | ✅ 有 | `BatchCostingStrategy:189-191` |
| **SPECIFIC** | ❌ **无** | `SpecificCostingStrategy:168-188`（query 无 businessDate 参数 + 无 le 过滤） |

**影响场景**：同 batchNo 存在 future-dated 入库成本层时（如预先录入未来到货），SPECIFIC 出库可能消耗未来成本层，违反历史成本原则（出库应只消耗出库日及之前入库的成本）。其他 3 层基方法均过滤。

**严重性 P1（非 P0）理由**：
- SPECIFIC 方法用于贵重物品/唯一标识商品（owner doc §个别计价法适用场景），同 batchNo 多次入库 + future-dated 场景狭窄；
- 余额数量/金额仍守恒（仅 unitCost 取值时点错误，非余额破缺）；
- 不影响其他 costMethod。

---

## 4. 多维审计维度裁决

### 维度「需求正确性」

对照 `costing-methods.md` 5 方法表 + 7 策略实现注记 + 子计算器注入模式 + 到岸成本 + 成本调整 + PPV + reclose 兜底。**承诺且有证据**：FIFO 红冲后 Σ layer remaining×unitCost 恢复（`testReverseRestoresCostInvariant`）。**承诺但无证据**：(1) STANDARD 红冲成本不变量（无测试，且实测破缺 → P1-MA2-024）；(2) 跨方法 rounding 不变量（仅 FIFO 0.01 容差，无跨方法断言 → P2-MA2-026）；(3) 三方对账聚合不变量（仅 FIFO 单 reverse，无混合操作后聚合断言 → P2-MA2-026）。

### 维度「owner-doc 对齐」

逐条核对 `costing-methods.md`：✅ 5 方法表（MOVING_AVERAGE/FIFO/BATCH/STANDARD/INDIVIDUAL）+ 子计算器注入模式四要素 + 到岸成本分摊（3 allocationMethod）+ 成本调整（4 adjustType + FIFO delta 层 + STANDARD_REVALUATION FIRMED 发布）+ reclose 兜底语义，实现均符合。

**owner doc 偏离（已文档化，非缺陷）**：LIFO（dict 40）/ WEIGHTED_AVERAGE（dict 20）已实现但 5 方法表（`costing-methods.md:106-112`）未列——实现注记 §1538-1 Deferred 已声明这两个方法为"本期 Non-Goal"但代码已落地（`TestErpInvLifoCosting` / `TestErpInvWeightedAverageCosting` 覆盖）。owner doc 滞后于代码，建议 MR1 顺手补 5 方法表 → 7 方法表（P2 文档漂移，不单独登记，归 MA3 文档-实现一致性层）。

**reclose 排除 MOVING_AVERAGE/STANDARD 的文档化**：owner doc §1538-1 实现注记未明确声明此排除，但 §控制点 4 已裁决为合理设计（非自主权漂移）。建议 MR1 在 owner doc 补注「reclose 不覆盖 MA/STANDARD 的职责边界理由」（P2 文档漂移）。

### 维度「业务正确性 — 见 §2 / §3」

已逐方法 + 逐控制点裁决。

### 维度「架构或边界影响」

库存核算跨域访问 DAG 合法性：

| 边 | 类型 | DAG 合法性 | 运行时正确性 |
|---|---|---|---|
| finance → inventory（`reclosePeriodCosts` 经 `IBizObjectManager.getBizObject("ErpInvCosting")`） | R（只读 command 编排） | ✅ 合法（`data-dependency-matrix.md §3.2/§4.4`，P1-MA1-017 已文档化） | ✅ |
| inventory → manufacturing（`StandardCostResolver` 经 mfg-dao 编译期依赖读 `ErpMfgCostRollupLine`） | R（只读） | ✅ 合法（编译期依赖，无环） | ✅ |
| inventory → master-data（`CostMethodResolver`/`StandardCostResolver`/`CostAdjustmentService` 经 `daoFor(ErpMdMaterial)`） | R（只读） | ⚠️ 治理（P1-MA1-022） | ✅ 只读语义正确（本审计复核） |
| inventory → master-data（`CostMethodResolver` 经 `daoFor(ErpMdAcctSchema)`） | R（只读） | ⚠️ 治理（P1-MA1-022） | ✅ |
| inventory → purchase（`ErpInvLandedCostProcessor` 经 `daoFor(ErpPurReceive/Line)` 3 处 `:267,473,477`） | R（只读） | ⚠️ 治理（P1-MA1-022） | ✅ 只读语义正确（本审计复核） |
| P0-MA1-021 修复：`CostAdjustmentPostingDispatcher.reverse:64-67` 走 `IErpFinVoucherBiz.reverse` | 跨模块写经 I*Biz | ✅ 已修复（plan 2026-07-27-1430-1 done） | ✅ 三方一致性复核通过（§6） |

**裁决**：DAG 零循环零禁止方向；P1-MA1-022 跨域只读 4 处经本审计复核运行时只读语义正确（仅治理缺陷，不升级）；P0-MA1-021 修复后不再跨模块直写。

### 维度「验证充分性」

对每个验收断言问「如果它假了，我怎么知道？」：

- ✅ **FIFO 出库多层消耗加权**：`testOutgoingSpansMultipleLayersWeightedCost:111-143` 断言 outLedger.totalCost=-620（50×10+10×12）+ 层 remaining 精确值——假了会数值不符。
- ⚠️ **三方对账聚合不变量**：当前**无**混合操作后的聚合断言（Σ ledger vs balance / Σ layer vs balance）——假了（如余额漂移）不会被发现，除非恰好撞上 FIFO 单 reverse 的 0.01 容差。→ P2-MA2-026。
- ⚠️ **STANDARD 红冲不变量**：当前**无**——红冲跨重估漂移不会被发现。→ P1-MA2-024。
- ⚠️ **跨仓调拨成本桥（层基）**：当前**无**——FIFO 跨仓 carriedCost 往返错误不会被发现。→ P2-MA2-027。
- ⚠️ **rounding 累积漂移**：仅 FIFO 单 reverse 0.01 容差——跨方法累积漂移不会被发现。→ P2-MA2-026。

### 维度「回归风险」

寻找「仅偶然通过狭窄验证」的代码：

- ✅ **三方对账在正常路径稳定**（7 costMethod 算法对称）。
- ⚠️ **reclosePeriodCosts 仅 costing-disabled→enabled 单一场景验证**（`testReclosePeriodCostsRebuildsMissingLayer`）——其他异常路径（如部分层缺失 / 层存在但 unitCost=0）未覆盖。但代码 `recomputeIncomingLayerIfMissing:122-142` + `recomputeOutgoingCogs:148-179` 的分支逻辑经核实正确。
- ⚠️ **SPECIFIC 历史成本守卫缺失仅在单批次场景未暴露**（`testOutgoingMatchesSpecificBatch:75-95` 单批次单入库）——多批次同 batchNo + future-dated 场景未覆盖 → P1-MA2-023。
- ⚠️ **rounding 漂移仅在 FIFO 单 reverse 容差内**——长周期反复操作后漂移累积未验证 → P2-MA2-026。
- ⚠️ **跨仓调拨成本桥仅 MA 验证**——层基方法 carriedCost 往返未验证 → P2-MA2-027。

### 维度「路由和技能选择正确性」

- ✅ 移动单 DONE→记账器分派→策略→流水→过账分派器→凭证 的任务路由与工作类型匹配（`StockMoveBookkeeper.bookCompletion:109-123` 经 `CostMethodResolver.resolve` 取键 → `resolveStrategy` 查 registry → 按 moveType 调 onIncoming/onOutgoing）。
- ✅ `CostMethodResolver` 解析链（material→acctSchema→config + costing-enabled 总开关 + isSupported 7 码值白名单）路由正确。
- ⚠️ **未识别码值静默回退默认**（`CostMethodResolver:40-43` method==null||!isSupported → `defaultCostMethod()`）：AP-04 反模式（owner doc §6 已登记）。当前 7 码值全覆盖，无静默失败风险；但新增策略时漏改 `isSupported` 会静默走默认。owner doc 已文档化此陷阱（§AP-04），维持治理层提醒，不升级。

### 维度「待办或自主权策略漂移」

- ✅ `costing-methods.md` Non-Goal 裁定（BATCH 完整/INDIVIDUAL 全链/工作中心 schema/存货减值/多账套并行成本/成本报表）在代码中**未无声扩大或缩窄**——已落地 7 策略仅实现核心算法，Non-Goal 子项各命名 successor 触发条件。
- ⚠️ `reclosePeriodCosts` 不覆盖 MOVING_AVERAGE/STANDARD：**非自主权漂移**（§控制点 4 已裁决为合理设计，owner doc 补注建议归 P2 文档漂移）。
- ✅ CostAdjustmentService AP-01 反模式（内联 `if/Objects.equals` 分派）：owner doc §6 已登记，触发条件=业务方新增第 3 种 adjustType。维持治理层 finding 不升级。

---

## 5. 并发敏感点（交接 A2.17）

本审计标注观察到的并发敏感点，不做系统性并发正确性裁决（归 A2.17）：

1. **`StockMoveBookkeeper.updateBalanceWithRetry:229-272`**：乐观锁保护下的余额扣减，冲突时 evict + reload + 重试，上限 `erp-inv.concurrent-deduct-max-retry=5`。UC-INV-08 加固。并发扣减同一余额时重试边界正确，但耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT` 后调用方需处理（当前移动单 DONE 流程未显式 catch，异常上浮 → 移动单 DONE 失败回滚）。归 A2.17 复核重试耗尽的事务语义。
2. **`CostAdjustmentService.applyLine:86-121`**：并发 apply 同一余额时无乐观锁（直接 `saveOrUpdateEntity(balance)`）。`TestErpInvCostAdjust` 未覆盖并发 apply。归 A2.17。
3. **`ErpInvCostingBizModel.recomputeWeightedAverageOutgoing:187-221`**：月末 reclose 修改 balance 时无乐观锁。若 reclose 与正常 DONE 记账并发，可能 lost-update。归 A2.17（但 reclose 通常在结账窗口期，无并发 DONE）。

---

## 6. MA1/MA2 finding 运行时影响复核

| Finding ID | 原描述 | 本审计运行时复核结论 | 终态 |
|---|---|---|---|
| **P1-MA1-022**（costing 侧 4 处跨域 daoFor） | `ErpInvLandedCostProcessor:267,473,477`（ErpPurReceive/Line）+ `StandardCostResolver:99`（ErpMdMaterial）+ `CostMethodResolver:61,70`（ErpMdMaterial/ErpMdAcctSchema）+ `CostAdjustmentService:291`（ErpMdMaterial）经 `daoFor` 跨域只读 | ✅ **仅治理缺陷，不升级**。逐处复核：(1) `ErpInvLandedCostProcessor.loadReceiveByCode/loadReceive/loadReceiveLines` 纯只读查询采购入库单/行（分摊基数来源），无回写；(2) `StandardCostResolver.resolveFromRollup/resolveFromMaterialMaster` 纯只读查 FIRMED rollup + 物料 standardCost（当前恒 null）；(3) `CostMethodResolver.readMaterialCostMethod/readAcctSchemaCostingMethod` 纯只读查 costMethod 字段；(4) `CostAdjustmentService.resolveCostMethod` 纯只读 fallback。**只读查询语义正确，不产生运行时成本正确性问题**。维持 P1 治理待 MR1（方案 A: master-data/purchase I*Biz 补便捷只读方法后迁移）。 | 不升级（维持 P1-MA1-022 todo） |
| **P0-MA1-021**（成本调整红冲跨模块直写 ErpFinVoucher） | `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 曾跨模块直写 `ErpFinVoucher.isReversed=true` 绕过 `IErpFinVoucherBiz.reverse()` | ✅ **修复正确，三方一致性复核通过**。`CostAdjustmentPostingDispatcher.reverse:64-67` 现调 `voucherBiz.reverse(adjust.getCode(), ErpFinBusinessType.COST_ADJUSTMENT, context)` 经 finance 完整红冲。`TestErpInvCostAdjust.testReverseRollsBackBalanceAndVoucher:262-296` 验证：原 COST_ADJUSTMENT 凭证 `isReversed=true` + 余额 avgCost/totalCost 回退 + posted=false。**MA 路径三方一致性确认**；FIFO 路径红冲三方一致性为 costing-methods.md:66 已登记 successor（P2-MA2-029 维持）。 | done（plan 2026-07-27-1430-1） |
| **P1-MA2-017**（auto-post-on-close 门控） | 影响 `closeInvModule` 调用 `reclosePeriodCosts` 的门控链路 | ✅ **不影响 reclose 成本算法正确性**。`erp-fin.inv-costing-reclose-on-close`（默认 true）仅控制 closeInvModule 是否**调用** reclose；reclose 的成本算法正确性独立（本审计 §控制点 4 已确认）。auto-post-on-close 的 doc/code 默认值偏离 + 语义双重偏离 + AR-AP/allowance 阻断分级不一致均为 finance 期末结账侧问题（归 A2.3 P1-MA2-017 todo），不影响 inventory costing 运行时。 | 不升级（维持 P1-MA2-017 todo） |
| **P1-MA2-002**（多币种 P2P 本位币凭证路径未验证） | PURCHASE_INPUT 存货估价的本位币折算路径 | ✅ **三方对账在本币种维度成立，多币种未验证但不升级**。入库 `line.unitCost` 隐含单币种假设（`balance.currencyId = line.currencyId`，`StockMoveBookkeeper.upsertBalance:162`）。同币种 moves 的三方对账在本位币/源币均成立（量纲一致）。多币种混合同一 balance 行时 `totalCost` 量纲不一致（schema 未强制一 balance 一币种）——但此为 P1-MA2-002 同根因（VoucherFact 单一 amount 字段 + 多币种 P2P E2E 未验证），本审计不重复登记，归 P1-MA2-002 一并 MR1 裁决。 | 不升级（维持 P1-MA2-002 todo） |

---

## 7. Finding 汇总

### 7.1 P0（即时通道）

**零 P0**。三方对账在正常路径成立，SPECIFIC 未来成本消耗与 STANDARD 红冲跨重估漂移均为边缘场景（P1），不构成即时修复门槛。

### 7.2 P1（新登记，待 MR1）

| Finding ID | 描述 | 证据 | 修复方向 | 目标 MR |
|---|---|---|---|---|
| **P1-MA2-023** | **SPECIFIC 历史成本守卫缺失**：`SpecificCostingStrategy.findSpecificLayers:168-188` 无 `le(incomingDate, businessDate)` 过滤（FIFO:199/LIFO:185/BATCH:190 均有）。同 batchNo 的 future-dated 入库成本层可能被今日出库消耗，违反历史成本原则。 | `SpecificCostingStrategy.java:168-188`（query 无 businessDate 参数）；对比 `FifoCostingStrategy.java:178-205` / `LifoCostingStrategy.java:164-192` / `BatchCostingStrategy.java:171-196` | MR1 在 `findSpecificLayers` 增 `businessDate` 参数 + `le(incomingDate, businessDate)` 过滤（对齐其他 3 层基方法）；`onOutgoing:92-93` 调用处传入 `move.getBusinessDate()`；补 future-dated 同 batchNo 场景测试 | MR1 |
| **P1-MA2-024** | **STANDARD 红冲成本不变量跨重估破缺**：`StandardCostingStrategy.onIncoming:43` 忽略传入的 `unitCost` 参数，经 `standardCostResolver.resolve()` 重解析当前 FIRMED rollup。若红冲期间 FIRMED rollup 经 STANDARD_REVALUATION 变更（`CostAdjustmentService.publishFirmedRollup:206-230`），反向入库用新标准成本，与原出库扣减的旧标准成本不一致，balance.totalCost 漂移 `(newStd-oldStd)×qty`。`StandardCostingStrategy.onOutgoing:63-87` 也不刷新 `line.unitCost`（其他层基策略均刷新），故 `ErpInvStockMoveProcessor.reverse:144 rl.setUnitCost(ol.getUnitCost())` 对 STANDARD 无效。 | `StandardCostingStrategy.java:36-87`（onIncoming 忽略 unitCost / onOutgoing 不刷 line.unitCost）；`ErpInvStockMoveProcessor.java:144`；无 STANDARD 红冲测试 | MR1 裁决——方案 A（推荐）：`onIncoming` 优先用传入的 `unitCost`（非 null 且 > 0 时），fallback 重解析；`onOutgoing` 刷新 `line.unitCost=standardUnitCost` 供 reverse 透传。方案 B：owner doc 标注「STANDARD 红冲跨重估为已知简化，建议红冲前先 reverse 相关 STANDARD_REVALUATION」。补 STANDARD 红冲不变量测试 | MR1 |

### 7.3 P2（watch-only，待 MR 顺手收敛或永久接受）

| Finding ID | 描述 | 处置 |
|---|---|---|
| **P2-MA2-026** | **三方对账聚合不变量测试缺失**：当前仅 `TestErpInvFifoCosting.testReverseRestoresCostInvariant` 单次断言 Σ layer remaining×unitCost == balance.totalCost（容差 0.01）。Σ ledger quantity/totalCost vs balance 无任何测试断言。LIFO/BATCH/SPECIFIC/STANDARD 无聚合不变量测试。 | watch-only，MR1 顺手补：混合操作（入库+出库+调拨+成本调整+红冲）后的三方对账聚合断言测试（逐方法） |
| **P2-MA2-027** | **跨仓调拨成本桥层基方法未测试**：`StockMoveBookkeeper:113-115` 内部调拨分支先 `onOutgoing` 取 `carriedCost` 再 `onIncoming`，仅 MA 经 `testInternalTransferCarriesCostAcrossWarehouses:128-149` 覆盖。FIFO/LIFO/BATCH/SPECIFIC/STANDARD 的跨仓 carriedCost 往返无测试。 | watch-only，MR1 顺手补：FIFO 跨仓调拨 E2E（源仓出库加权 unitCost → 目标仓追加新层，两仓余额合计 + 层合计断言） |
| **P2-MA2-028** | **红冲使用 today() 而非原 businessDate**：`ErpInvStockMoveProcessor.reverse:128 reverseReq.setBusinessDate(CoreMetrics.today())`。FIFO 反向入库新层 incomingDate=today，若原出库在 T-10，红冲后新层排在 FIFO 队列末尾（today）。若后续出库日期介于 T-10 与 today 之间（如 T-5），`le(incomingDate, businessDate=T-5)` 过滤排除 today 的反向层 → 可能 ERR_COST_NOT_AVAILABLE。owner doc §FIFO 红冲 Decision (a) 已文档化「追加新层」语义，但 incomingDate=today 的 FIFO 排序副作用未述。 | watch-only，MR1 裁决：方案 A（推荐）`reverseReq.setBusinessDate(original.getBusinessDate())` 保持队列时序；方案 B owner doc 标注「红冲新层 incomingDate=today，FIFO 队列末尾」为已知简化 |
| **P2-MA2-029** | **CostAdjustment FIFO 红冲三方一致性未测试**：`TestErpInvCostAdjust.testReverseRollsBackBalanceAndVoucher:262-296` 仅 MA 路径（无层）。FIFO 物料成本调整的 delta 调整层（`incomingMoveId=-lineId` 哨兵）经 `removeFifoAdjustLayer:194-202` 物理删除——若该调整层已部分被后续出库消耗，物理删除可能破坏已扣减层。costing-methods.md:66 已登记 successor。 | watch-only，归 costing-methods.md:66 已文档化 successor（触发条件：实际启用 FIFO 物料的成本调整红冲遇此场景时） |
| **P2-MA2-030** | **reclosePeriodCosts 不覆盖 MOVING_AVERAGE/STANDARD 的 null/zero unitCost 边缘**：若 MA/STANDARD 流水因上游异常出现 `unitCost=null/0`，reclose 不兜底（`LAYER_BASED_METHODS` + WEIGHTED_AVERAGE 分支排除 MA/STANDARD）。属设计边界（DONE 时应已正确），非缺陷。 | watch-only，MR1 顺手在 owner doc 补注「reclose 职责边界：MA/STANDARD 在 DONE 时即最终，上游异常 unitCost 不兜底」 |

---

## 8. 残留风险

1. **三方对账聚合不变量无自动化校验**（P2-MA2-026）：当前唯一校验点是 FIFO 单 reverse 的 0.01 容差。长周期累积漂移 / 混合操作后聚合破缺无监控。建议 MR1 补混合场景聚合断言测试。
2. **STANDARD 红冲跨重估场景未覆盖**（P1-MA2-024）：实际启用 STANDARD 物料 + STANDARD_REVALUATION + 红冲的场景下，balance 漂移不会被发现。
3. **SPECIFIC 未来成本消耗**（P1-MA2-023）：实际启用 SPECIFIC + 同 batchNo future-dated 入库的场景下，unitCost 取值时点错误。
4. **CostAdjustmentService AP-01 反模式**（owner doc §6 已登记）：新增第 3 种 adjustType 时 `if` 链扩展成本超过重构成本，触发 Strategy+registry 重构。
5. **并发敏感点**（§5）：3 处并发缺口交接 A2.17 系统性审计。

---

## 9. 验证基线

- **审计不改代码**，本报告无 P0 即时修复。
- **回归基线确认**（2026-07-27 实测）：
  - `mvn clean install -DskipTests`（全 154 模块）：**BUILD SUCCESS**（01:32 min）
  - `mvn test -pl module-inventory/erp-inv-service -am`：**BUILD SUCCESS**（01:37 min，含 finance-service 上游依赖）
- 锚点：`docs/audits/compliance-baseline.md §M0 锚点注记`（HEAD=0e963531d）

---

## 10. 结论

库存核算三方对账（成本层 + 余额 + 流水）经多维系统性审计**通过**：7 costMethod 逐方法裁决 + 8 个已识别控制点逐项裁决 + MA1/MA2 finding 运行时复核 4 项无升级。**零 P0**；**2 项新 P1**（P1-MA2-023 SPECIFIC 历史成本守卫 / P1-MA2-024 STANDARD 红冲跨重估）登记待 MR1；**5 项 P2** watch-only。CostAdjustmentService AP-01 反模式 + owner doc LIFO/WEIGHTED_AVERAGE 表漂移维持治理层 finding。`passes multi-dimensional audit`（带残留风险）。

**`passes multi-dimensional audit`**
