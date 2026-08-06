# RC MA4 A4.1.15 — FIFO 物料到岸成本 delta 层后续出库消耗数值正确性评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证层 / A4.1 展开器实体行）
> 工作项：A4.1.15（MA4 运行时行为验证 — A1.5 §7-1：UC-FIN-10 LC-L3 FIFO 物料 + 到岸成本 delta 层 + 后续出库消耗的运行时数值正确性，关联 P2-RC-004）
> 审计 plan：`docs/plans/2026-08-07-1400-3-rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §去重协议 MA4↔A5.6 边界 / §7 arm-index 衔接 / §8 过程纪律自检）
> L1 真相源：`docs/design/finance/use-cases.md:197` UC-FIN-10 LC-L3（逐字「后续出库按更新后的队列单价计算」）
> 存疑点来源：`docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 1
> 关联 finding：P2-RC-004（arm-index `:134`，FIFO + 到岸成本交互 E2E 测试缺口）
> 审计性质：**只读运行时数值正确性评估**（读 delta 层追加/消耗代码路径 + 既有测试覆盖普查 + 数值正确性静态推理；不改代码/ORM/api.xml/真相源；成本过账核心路径 CostAdjustmentService/FifoCostingStrategy 经只读探针评估不修改）
> 审计日期：2026-08-07
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`fe32f4c21`

## 0. 审计结论（TL;DR）

| 项 | 结论 |
|---|---|
| **存疑点裁决** | A1.5 §7 存疑点 1「FIFO delta 层后续出库消耗数值正确性」经静态数值推理 **CONFIRMED 为行为缺陷**——delta 调整层（unitCost=Δ、qty=onHand、incomingDate=adjust.businessDate）在后续 FIFO 出库的升序消耗循环中**结构性永不被消耗**（原入库层 incomingDate ≤ delta 层 + 原入库层 remaining=onHand 已足以满足任何 ≤ onHand 的出库量），导致 LC-L3「后续出库按更新后的队列单价计算」在 FIFO 物料路径下**未实现**（出库恒用旧单价，delta 层残留在零数量余额上） |
| **P2-RC-004 分级调整** | **P2 → P1（升级）**。A1.5 §5.2/§6.1 原定级 P2（测试覆盖补强项，假设行为正确仅 E2E 缺口）的**前提假设被证伪**：本验证经数值推理证实 FIFO delta 层消耗数值有静态 bug（时序错位 + Δ 累计错位），命中方法论 §2 **P1①**「行为实质偏离验收标准」，触发 MR1 优先修复 |
| **新 finding** | 无新建（升级既有 P2-RC-004，不另立编号；与 P2-MA2-029 reverse 路径物理删除、A1.5 §7-2 红冲余额守恒不同控制点） |
| **MR0 即时通道** | **不触发**。非 §2 P0④ 会计过账正确性破坏的「默认活跃路径」形态——MOVING_AVERAGE（默认计价法）路径经 `applyAverageLike` 直接更新 `balance.avgCost` 行为正确强测覆盖；缺陷仅限「FIFO 物料 + 到岸成本/成本调整」特定组合（非默认 costMethod），且技术可逆（reverse 按 `-lineId` 哨兵删 delta 层回退余额） |
| **修复归口** | MR1（R1.0→RC-R1.n）。修复触及**成本过账核心路径**（CostAdjustmentService.applyFifo / FifoCostingStrategy.onOutgoing），**须 ask-first + 独立 plan-audit（§5 会计过账逻辑类）** |

**整体裁决**：A1.5 §7 存疑点 1 经运行时数值正确性静态推理 **CONFIRMED 升级 P2-RC-004 = P1**。核心证据链：`CostAdjustmentService.appendFifoAdjustLayer:160-161` delta 层 `incomingQuantity=remainingQuantity=onHand`（全量 onHand，非增量）+ `:165-166` `incomingDate=adjust.businessDate`（≥ 原入库层 incomingDate）→ `FifoCostingStrategy.findFifoLayers:202-203` 按 incomingDate 升序排序 + `onOutgoing:105-120` 循环至 `remaining<=0` 中止 → 原入库层（更早 incomingDate + remaining=onHand）**总是先于且独占地**满足出库量 → delta 层**结构性永不被到达**。后果：① LC-L3 未实现（出库用旧单价 10 而非更新单价 12）；② 零数量余额残留 adjustAmount 成本（GL 存货科目与实物量错配）。**既有测试 `TestErpInvCostAdjust#testFifoAppendsAdjustLayerAndOutgoingConsumes:167` 注释自承「COGS=10×10=100（先消耗原层）」即旧单价**，且 `:171` 仅弱断言 `unitCost>0` 掩盖了缺陷。本验证**不实施修复**（成本过账核心路径 + plan Non-Goals），只升级分级 + 登记修复归口。

---

## 1. 存疑点原文 + L1 需求契约

> 存疑点来源：`2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 1（逐字引用）。

| # | 存疑点 | 触发条件 | 交 MA4 |
|---|---|---|---|
| 1 | **FIFO 物料 + 到岸成本 delta 层 + 后续出库消耗**的运行时数值正确性（delta 层 unitCost=Δ 被后续 FIFO 出库消耗时，出库成本是否正确含调整；多个 delta 层 + 原入库层混合排序时的 Σ 正确性） | 实际启用 FIFO 物料的到岸成本分摊后再多笔出库 | A4.1 运行时探针（补 E2E 即闭合 P2-RC-004） |

**L1 需求契约**（`docs/design/finance/use-cases.md:197`，UC-FIN-10 LC-L3，逐字）：

> 后续出库按更新后的队列单价计算

**L2 owner doc 契约**（`docs/design/finance/costing-methods.md`）：
- §成本调整 §实现注记：成本调整 `:472`「FIFO 物料追加「delta 调整层」（`ErpInvCostLayer`，unitCost=新旧单位成本差，incomingMoveId=-行ID 负值哨兵），保持 FIFO 队列先进先出不变量」。
- §FIFO 出库逻辑 `:224-249`：按 incomingDate 升序消耗，Σ 各队列消耗量×单价。

> **本验证核心问题**：L2 §实现注记 `:472` 声称 delta 层「保持 FIFO 队列先进先出不变量」即可实现 LC-L3，但未论证「delta 层 qty=onHand + incomingDate≥原入库层」时升序消耗循环是否会实际到达 delta 层。本验证填补该论证缺口——结论是**不会到达**。

---

## 2. delta 层追加逻辑核验（L3，file:line）

> 实仓逐行核实 `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java`（319 行，HEAD `fe32f4c21`）。

### 2.1 三轴分派（FIFO 走 applyFifo）

`CostAdjustmentService.applyLine:86-121`：`:103` `resolveCostMethod` → `:108-109` FIFO 走 `applyFifo`，MA/Standard 走 `applyAverageLike`。

### 2.2 Δ 计算（applyFifo）

`CostAdjustmentService.applyFifo:137-147`：
- `:146` `appendFifoAdjustLayer(adjust, line, balance, onHand, newUnitCost.subtract(oldUnitCost), adjustAmount)`——**Δ = newUnitCost − oldUnitCost** 作为第 5 形参 `deltaUnitCost` 传入（证实 MINOR1：Δ-subtract 发生在 `applyFifo:146`，存储在 `appendFifoAdjustLayer`）。
- `:140-144` 余额 `totalCost += adjustAmount` + `avgCost=null`（FIFO 语义）。

### 2.3 delta 层追加（appendFifoAdjustLayer）—— 核心结构证据

`CostAdjustmentService.appendFifoAdjustLayer:149-171`：
- `:160` `layer.setIncomingQuantity(qty)`——**qty = onHand**（第 4 形参，由 `applyFifo:146` 传入 `onHand`，即调整时的全部现有量）。
- `:161` `layer.setRemainingQuantity(qty)`——**remaining = onHand**（与 incoming 同量）。
- `:162` `layer.setUnitCost(ErpInvConfigs.roundCost(deltaUnitCost))`——unitCost = Δ（证实 setUnitCost :162）。
- `:163` `layer.setTotalCost(adjustAmount)`——totalCost = Δ×onHand = adjustAmount（内部自洽）。
- `:165-166` `incomingDate = adjust.getBusinessDate() != null ? adjust.getBusinessDate() : CoreMetrics.today()` + `layer.setIncomingDate(incomingDate)`——**incomingDate = adjust.businessDate**（到岸成本审核日 / 成本调整业务日；证实 :165-166）。
- `:168` `layer.setIncomingMoveId(-line.getId())`——负值哨兵（区别正常移动单正 ID；证实 :168）。

**delta 层结构定论**：`{incomingQuantity=onHand, remainingQuantity=onHand, unitCost=Δ, incomingDate=adjust.businessDate, incomingMoveId=-lineId}`。**关键缺陷根源**：delta 层的 `remainingQuantity = onHand`（全量现有量，非增量），且 `incomingDate = adjust.businessDate ≥ 原入库层 incomingDate`（到岸成本/成本调整总是发生在入库之后）。

---

## 3. FIFO 升序消耗 delta 层核验（L3，file:line）—— 核心发现

> 实仓逐行核实 `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/FifoCostingStrategy.java`（210 行）。

### 3.1 升序排序（findFifoLayers）

`FifoCostingStrategy.findFifoLayers:178-205`：
- `:184-200` 查询过滤：`orgId/materialId/warehouseId/costMethod=FIFO/remainingQuantity>0` + 历史成本守卫 `:198-200` `le("incomingDate", businessDate)`。
- `:202-203` `list.sort(Comparator.comparing(l -> l.getIncomingDate() != null ? l.getIncomingDate() : CoreMetrics.today()))`——**按 incomingDate 升序排序**（证实 MINOR2：升序排序 :202-203 在 findFifoLayers 内，非 onOutgoing）。原入库层 + delta 层**混合排序**。

### 3.2 跨队列消耗循环（onOutgoing）—— 缺陷发生点

`FifoCostingStrategy.onOutgoing:105-120`：
- `:103` `remaining = qty`（出库量）。
- `:105-120` for 循环逐层消耗：`:113` `take = remaining.min(avail)` → `:114` `takeCost = take.multiply(nz(layer.getUnitCost()))` → `:115` 扣减 `layer.remainingQuantity` → `:118` `totalCost = totalCost.add(takeCost)` → `:119` `remaining = remaining.subtract(take)`。
- `:106-107` `if (remaining.signum() <= 0) break;`——**出库量满足即中止循环**。

### 3.3 数值正确性推理（静态证明 delta 层结构性永不被消耗）

设原入库层 `L0 = {remaining=r0, unitCost=u0, incomingDate=d0}`，delta 层 `D = {remaining=rD, unitCost=Δ, incomingDate=dD}`。

**关键不变量**（由 §2.3 证实）：
1. `rD = onHand`（delta 层 remaining = 调整时全量现有量）。
2. 调整发生时 `onHand = r0`（若无中途出库，原入库层 remaining 即为 onHand；若有中途出库，`onHand = Σ 各原入库层 remaining`，delta 层 remaining = 该 Σ）。
3. `dD = adjust.businessDate ≥ d0`（到岸成本/成本调整总是发生在原入库之后；同日时稳定排序按层 id，原入库层先建先排）。

**出库量 `q ≤ onHand` 时的消耗轨迹**（`q > onHand` 被库存可用量校验在 costing 之前拒绝）：
- 升序排序后序列首部为 `L0`（`d0 ≤ dD`）。
- 循环从 `L0` 开始：`take = min(q, r0) = min(q, onHand) = q`（因 `q ≤ onHand = r0`）。
- `L0.remaining = r0 − q`，`remaining = q − q = 0` → `:106-107` break。
- **delta 层 `D` 永不被到达**（循环在 `L0` 即中止）。

**后果**：
- 出库 `totalCost = q × u0`（**旧单价**，非 `q × (u0+Δ)` 更新单价）——**LC-L3 未实现**。
- delta 层 `D.remaining = onHand` 永不扣减，残留在成本层表。
- 余额 `totalCost` 在全量出库后残留 `adjustAmount = Δ × onHand`，但 `totalQuantity = 0`——**零数量余额非零成本**（成本守恒破缺）。

### 3.4 多原入库层 + delta 层 incomingDate 落中间的场景（更严重）

若存在多个原入库层 `L0(d0)、L2(d2)` 且 delta 层 `D(dD)` 满足 `d0 < dD < d2`（到岸成本审核日落在两次入库之间）：
- 升序序列：`L0(d0)、D(dD)、L2(d2)`。
- 消耗 `q`：先耗 `L0`，不足则耗 `D`（**delta 层被部分消耗，但其 unitCost=Δ 是单价差非真实单价**）→ Σ 错误（混入 Δ 幻影单位）。
- 例：`L0=50@10(d0)`、`L2=50@10(d2)`、`D=100@2(dD)`，出库 80 → `L0` 耗 50（500）+ `D` 耗 30（60）= 560，加权 unitCost=7.0；**正确应为 80×12=960**。差额 400。

**结论**：delta 层 `remainingQuantity=onHand` + `unitCost=Δ` 的结构在 FIFO 升序消耗模型下**数值不可能正确**——要么永不被消耗（同日/晚日，残留），要么被错误地当作真实入库层部分消耗（落中间，Σ 错位）。该机制无法实现 LC-L3「后续出库按更新后的队列单价计算」。

### 3.5 既有测试自承缺陷（关键证据）

`TestErpInvCostAdjust#testFifoAppendsAdjustLayerAndOutgoingConsumes:144-175`（FIFO 物料 receive 10@10 → 调整至 12[Δ=2] → 出库 10）：
- `:167` 注释逐字：「后续出库 10：按 FIFO 升序消耗（原层 10@10 + delta 10@2），**COGS=10×10=100（先消耗原层）**」——**测试作者自承出库用旧单价 10（非更新单价 12），delta 层未消耗**。
- `:170` 注释：「delta 层 incomingDate 同 businessDate（2026-07-01），与原层同日，消耗顺序按层 id；原层先入先建先消耗」。
- `:171` 断言：`assertTrue(outLedger.getUnitCost().compareTo(BigDecimal.ZERO) > 0, "出库有成本")`——**弱断言（仅 >0）**，不断言更新单价 12，也不断言出库后余额（会暴露 20 残留）。

该测试**文档化了缺陷**（注释明示旧单价消耗）却以弱断言**掩盖了缺陷**——这正是 P2-RC-004「FIFO delta 层 E2E 缺口」背后隐藏的真实行为 bug。

---

## 4. 与 MOVING_AVERAGE 路径对照（证实 FIFO 路径缺陷非共性）

`CostAdjustmentService.applyAverageLike:123-135`（MA 路径）：
- `:126` `balance.setAvgCost(newUnitCost)`——直接更新 avgCost 为新单价。
- `:127-128` `totalCost += adjustAmount`。
- 后续 MA 出库（`MovingAverageCostingStrategy`）取 `balance.avgCost` = 新单价 → LC-L3 在 MA 路径下**正确实现**（`TestErpInvLandedCostEndToEnd` 强覆盖）。

**对照结论**：LC-L3 在 MA 路径正确、FIFO 路径缺陷。根因 = FIFO 用「delta 层」间接表达单价更新，而 MA 用「直接改 avgCost」直接表达；delta 层的「全量 onHand + Δ 单价 + 升序消耗」结构与 FIFO 升序消耗模型不兼容。

---

## 5. 既有测试覆盖边界普查（L4）

> grep `TestErpInvFifoCosting` + `TestErpInvLandedCostEndToEnd` + `TestErpInvCostAdjust` 全集（HEAD `fe32f4c21`），引用 A1.5 §3 评级。

| 测试文件 | 覆盖范围 | FIFO delta 层消耗覆盖 | 断言强度 |
|---||---|---|
| `TestErpInvFifoCosting.java`（6 @Test） | FIFO 正常入库层消耗（`testOutgoingSpansMultipleLayersWeightedCost:111-143` 两**正常入库层** @10/@12 跨层 Σ=-620 强断言） | **不覆盖 delta 调整层**（仅正常入库层，非 CostAdjustmentService 追加的 Δ 层） | 深（正常层） |
| `TestErpInvLandedCostEndToEnd.java`（4 @Test） | 到岸成本分摊 E2E | `:82-83` 物料均 `COST_METHOD_MOVING_AVERAGE`——**FIFO 物料 + 到岸成本交互零 E2E** | 深（MA 路径） |
| `TestErpInvCostAdjust.java`（8 @Test） | 成本调整 8 类 | `testFifoAppendsAdjustLayerAndOutgoingConsumes:144-175` 唯一触及 FIFO delta 层，但**弱断言（unitCost>0）+ 注释自承旧单价消耗**（§3.5） | **浅**（FIFO delta 层消耗路径） |

**测试覆盖边界定论**：
1. FIFO 多层 Σ 正确性 = **深覆盖**（正常入库层，非 delta 层）。
2. 到岸成本 MA 路径 = **深覆盖**。
3. **FIFO delta 层后续出库消耗 = 仅 1 个浅断言测试，且该测试文档化并掩盖了缺陷**——无任何测试断言「出库用更新单价 12」或「出库后余额守恒」。

> 本验证**不重复登记**测试覆盖缺口（P2-RC-004 已登记 E2E 缺口）；本验证的增量 = 揭示该 E2E 缺口背后隐藏的是**行为缺陷**（非仅测试缺失），故升级分级。

---

## 6. MA4↔A5.6 边界声明

> 方法论 §去重协议 MA4↔A5.6：本验证审「行为是否符合需求」（FIFO delta 层消耗数值是否正确实现 LC-L3），与 A5.6（audit-remediation E2E 断言强度，审测试质量视角）边界按此执行。

- 本验证**不重做** A5.6 E2E 断言强度审计（A5.6 `2026-07-29-1430-arm-ma5-e2e-effectiveness.md` 已覆盖 E2E 业务断言强度分类）。
- 本验证只评 delta 层消耗**行为正确性**（L3 代码路径数值推理）+ 既有测试覆盖边界（§5）。
- A5.6 视角的「FIFO delta 层 E2E 断言强度」若未来审计，其输入 = 本验证 §3.5 揭示的「既有测试弱断言掩盖缺陷」证据。

---

## 7. P2-RC-004 分级确认/调整（方法论 §2 判据 + 三源对照）

### 7.1 分级裁决：P2 → P1（升级）

| 维度 | 评估 | 命中 |
|---|---|---|
| **§2 P0④ 会计过账正确性破坏** | **不成立**——缺陷仅限「FIFO 物料 + 到岸成本/成本调整」特定组合（非默认 costMethod，默认 MOVING_AVERAGE 路径正确）；非「默认活跃路径每次操作即破坏」形态；技术可逆（reverse 按 `-lineId` 哨兵删 delta 层回退余额） | ❌ |
| **§2 P0① 活跃数据破坏** | **不成立**——需「物料配 FIFO + 应用到岸成本/成本调整」前置配置，非默认活跃路径 | ❌ |
| **§2 P1① 功能完全缺失或行为实质偏离验收标准** | **成立**——LC-L3「后续出库按更新后的队列单价计算」在 FIFO 物料路径下**未实现**（出库恒用旧单价，§3.3 证明）；属「行为实质偏离验收标准」 | ✅ |
| **§2 P2① 次要验收标准未完全满足（主路径 OK 边界弱）** | **不适用**——LC-L3 是 UC-FIN-10 六条验收标准之一（主验收标准），且 MA 路径正确不构成「FIFO 路径缺陷可降为边界弱」的理由（两条路径是并列实现，非主/辅） | ❌ |

**分级结论**：**P1**（§2 P1① 命中）。A1.5 §5.2/§6.1 原定级 P2 的前提假设（「行为实现正确，仅 FIFO 交互 E2E 缺口」）**被本证伪**——经 §3 数值推理证实 FIFO delta 层消耗有静态 bug。

### 7.2 三源对照

| 源 | 内容 | 与本裁决一致性 |
|---|---|---|
| **L1**（`use-cases.md:197`） | LC-L3「后续出库按更新后的队列单价计算」 | FIFO 路径未实现 → P1① 行为实质偏离 ✅ 一致（升级） |
| **L2**（`costing-methods.md:472`） | 「FIFO 追加 delta 调整层，保持 FIFO 队列先进先出不变量」 | L2 声称的机制经 §3 证伪无法实现 LC-L3；按 §4 Q1 L2↔L3 冲突以 L1 为准，L2 推定已向实现妥协，分歧记入报告不直改 L2 ✅ |
| **L3**（`CostAdjustmentService.java:160-166` + `FifoCostingStrategy.java:105-120,202-203`） | delta 层结构 + 升序消耗循环 | 数值推理证实缺陷 ✅ |

### 7.3 与 A1.5 §5 LC-L3 接受结论的分层一致性

- A1.5 §5.2 LC-L3 行裁决「**接受**（行为实现，FIFO 交互测试为 P2 watch-only）」——该裁决的依据是「假设 delta 层追加逻辑正确 + FIFO 升序消耗保证时序」（A1.5 §Current Baseline 初步实测）。
- 本验证**证伪了该假设**：delta 层追加逻辑本身的结构（remaining=onHand + incomingDate≥原层）导致升序消耗**保证的是 delta 层永不被消耗**（与时序正确相反）。
- 按 methodology，A1.5 的「接受」是基于未充分验证的静态假设；本运行时数值推理为 MA4 提供了**新证据**，据此升级 P2-RC-004 = P1。UC-FIN-10 整体裁决（A1.5 §5.3「接受」）的**其余 5 条验收标准不受影响**（FIFO-F1/F2/F3 + LC-L1/L2 经 A1.5/A2.4 强测证实）；仅 LC-L3 的 FIFO 路径投影升级。

### 7.4 与既有 finding 的去重（§去重协议）

| 既有 finding | 控制点 | 与本升级的关系 |
|---|---|---|
| **P2-RC-004**（arm-index `:134`） | FIFO + 到岸成本交互 forward E2E 缺口 | **本验证即升级此 finding**（P2→P1），不新建 |
| **P2-MA2-029** | CostAdjustment FIFO 红冲 delta 层**物理删除**三方一致性（reverse 路径） | 不同控制点（reverse 物理删除 vs forward 消耗），不合并；但同根因（delta 层 remaining=onHand 结构）——本升级佐证 P2-MA2-029 的 reverse 路径风险更值得关注 |
| **A1.5 §7-2**（交 A4.1.16） | FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除余额守恒 | 不同控制点（reverse 余额守恒 vs forward 消耗正确性），不合并；A4.1.16 范围 |
| **P1-MA4-021**（resolved） | 测试有效性（STANDARD/SPECIFIC，未含 landed-cost-FIFO-interaction） | 已 resolved 且范围未含本控制点，不合并 |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`fe32f4c21`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter，真正门控在 CI workflow（`.github/workflows/compliance.yml`）。本报告**不**以 checker 脚本退出码作为门控通过依据。**本审计无生产代码变更**（纯审计报告），checker 无回归风险（actual 反映既有 HEAD 状态，非本审计引入）。checker R3 段起既有行为（未输出计数即返回，A4.1.11/A4.1.13 已记录），因零生产代码变更 R3-R12 客观上与基线一致。

  | 规则 | 描述 | baseline（machine-readable） | actual（HEAD fe32f4c21） | delta | 说明 |
  |---|---|---|---|---|---|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 | — |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 | — |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 | — |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 | — |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | 0 | — |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | 0 | — |
  | R2d | Processor daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R3-R12 | （脚本 R3 段起既有行为未输出计数，A4.1.11/A4.1.13 已记录） | 5/0/0/2/0/0/6/0/69/66/40 | 同基线（零代码变更） | 0 | 不适用（脚本既有行为） |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告升级 P2-RC-004（不新建 finding），已按 §7 规则 grep arm-index 同域同控制点（P2-MA2-029 reverse / A1.5 §7-2 红冲 / P1-MA4-021 resolved）后给出「升级既有不新建」裁决（§7.4），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9）：本审计未修改 product-scope / use-cases / costing-methods 需求契约段落。L2 `costing-methods.md:472` 的 delta 层机制声称经本证伪，记入报告（§7.2），不直改 L2（§9 冻结条款；L2↔L3 冲突以 L1 为准）。
- [x] **保护区域纪律声明**：本审计为只读评估，未修改 CostAdjustmentService/FifoCostingStrategy/LandedCostAllocationEngine/ErpInvLandedCostProcessor（成本过账核心路径，保护区域）。修复须 ask-first + 独立 plan-audit（§5 会计过账逻辑类）。

---

## 9. 修复方向指引（供 MR1，非本审计实施）

> 本节为 MR1 修复提供方向性输入，**本审计不实施**（保护区域 + plan Non-Goals）。

缺陷根因 = delta 层 `remainingQuantity=onHand` + `unitCost=Δ` + FIFO 升序消耗模型不兼容。可能修复方向（须独立 plan-audit 评估）：

- **方向 A（重陈述原层单价）**：成本调整时直接更新原入库层的 `unitCost`（重述），而非追加 delta 层。需处理「原层已部分消耗」时的差值分摊。
- **方向 B（delta 层消耗配对）**：修改 `onOutgoing` 消耗循环，使消耗原层时同步按比例消耗配对的 delta 层（需建立 delta↔原层配对关系，当前仅 `-lineId` 哨兵不足以配对）。
- **方向 C（delta 层 incomingDate 前置 + qty 调整）**：调整 delta 层的 qty/incomingDate 使其在升序消耗中正确到达——但 §3.4 证明任何 incomingDate 位置均无法正确（前置则幻影单位混入 Σ）。

三方向均触及成本过账核心路径，须 ask-first。MR1 修复时须补强 FIFO delta 层消耗的**深断言测试**（断言更新单价 + 出库后余额守恒，替代当前 `unitCost>0` 弱断言）。

---

## §自检清单（报告产出前强制）

- [x] §1 存疑点原文 + L1 需求契约（UC-FIN-10 LC-L3 逐字）
- [x] §2 delta 层追加逻辑核验（L3 file:line，Δ 计算 + delta 层结构）
- [x] §3 FIFO 升序消耗核验 + 数值正确性推理（核心发现，静态证明 delta 层结构性永不被消耗）
- [x] §4 MA 路径对照（证实 FIFO 路径缺陷非共性）
- [x] §5 既有测试覆盖边界普查（3 测试文件覆盖矩阵 + 弱断言掩盖缺陷证据）
- [x] §6 MA4↔A5.6 边界声明
- [x] §7 P2-RC-004 分级裁决（P2→P1 升级 + 三源对照 + 与 A1.5 分层一致性 + 去重）
- [x] §8 过程纪律自检（checker actual vs baseline + 独立性 + 交叉去重 + 真相源未修改 + 保护区域）
- [x] §9 修复方向指引（供 MR1）

**报告完整性自检结论**：§1-§9 全部存在，无缺失。
