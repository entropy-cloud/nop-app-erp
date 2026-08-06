# RC MA4 A4.1.16 — FIFO 物料到岸成本红冲 delta 层物理删除余额守恒评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证层 / A4.1 展开器实体行）
> 工作项：A4.1.16（MA4 运行时行为验证 — A1.5 §7-2：UC-FIN-10 FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除余额守恒，复用 P2-MA2-029 子场景）
> 审计 plan：`docs/plans/2026-08-06-1517-1-rc-ma4-a4-1-16-fifo-landed-cost-reverse-delta-layer-deletion-balance-conservation.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §去重协议 MA4↔A5.6 边界 / §7 arm-index 衔接 / §8 过程纪律自检）
> L1 真相源：`docs/design/finance/use-cases.md:197` UC-FIN-10 LC-L3（逐字「后续出库按更新后的队列单价计算」）+ 红冲闭环隐含不变量（成本调整红冲后余额/层状态回退至调整前一致状态）
> 存疑点来源：`docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 2
> 关联 finding：**P2-MA2-029**（arm-index `:717`，CostAdjustment FIFO 红冲 delta 层物理删除三方一致性未测试）；**P2-RC-004**（arm-index `:134`，A4.1.15 升级 P1，forward 路径 delta 层消耗缺陷）
> 审计性质：**只读运行时余额守恒评估**（读红冲余额回退 + delta 层物理删除哨兵 + delta 层追加 + FIFO 升序消耗代码路径 + 既有测试覆盖普查 + 余额守恒静态推理；不改代码/ORM/api.xml/真相源；成本过账/删除核心路径 `CostAdjustmentService.reverseLine`/`removeFifoAdjustLayer`/`appendFifoAdjustLayer`/`FifoCostingStrategy` 经只读探针评估不修改）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`112a4b493`

## 0. 审计结论（TL;DR）

| 项 | 结论 |
|---|---|
| **存疑点裁决** | A1.5 §7 存疑点 2「FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除余额守恒」经静态代码路径推理 + A4.1.15 结论复核 **裁决：余额守恒在「delta 层全量未消耗」常态下成立；「部分消耗后删除」的层不变量漂移面存在但被 P2-RC-004（P1，forward）完全涵盖**——`reverseLine:182-186` 余额回退 `totalCost -= adjustAmount` + `avgCost = oldUnitCost` 与 `applyFifo:140` 正向 `totalCost += adjustAmount` **严格对称**；`removeFifoAdjustLayer:194-202` 按 `-lineId` 哨兵精确删除 delta 层（与 `appendFifoAdjustLayer:168` 写入哨兵镜像）。常态（delta 层结构性未被消耗，A4.1.15 §3.3 证明）下删除全量未消耗层 + 对称余额回退 → FIFO 不变量（Σ layer.remaining×unitCost == balance.totalCost）成立。 |
| **「部分消耗」可达性复核** | **条件性可达**：A4.1.15 §3.3 已证「常态」（单原入库层 + 出库 ≤ onHand）下 delta 层结构性永不被消耗（remainingQuantity 恒 = onHand）；但 A4.1.15 §3.4 已证「多原入库层 + delta 层 incomingDate 落中间」+「原层耗尽 → 新增更晚日期入库层 → 后续出库」跨期场景下 delta 层**可被部分消耗**。该部分消耗本身是 P2-RC-004（P1，forward delta 层消耗缺陷）的表现，非 reverse 路径独立缺陷。 |
| **P2-MA2-029 分级裁决** | **维持 P2**（测试覆盖补强，不升级）。①常态 delta 层未消耗 → 物理删除全量未消耗层 + 对称余额回退 → 余额守恒成立（行为正确）；②部分消耗漂移面被 P2-RC-004（P1，forward，同根因 = delta 层 remainingQuantity=onHand 幻影量）完全涵盖，按方法论 §去重协议**不重复升级同一根因**；reverse 路径无独立行为缺陷（`reverseLine` 严格按 `line.adjustAmount` 对称回退，`removeFifoAdjustLayer` 哨兵精确删除，无独立错误）。P2-MA2-029 独立缺口 = FIFO delta 层红冲三方一致性（成本层 + 余额 + 流水）测试覆盖缺失。 |
| **新 finding** | 无新建（维持既有 P2-MA2-029，追加 A4.1.16 运行时余额守恒评估注记；与 P2-RC-004[A4.1.15 forward 路径，不同方向不同控制点]交叉引用不合并） |
| **MR0 即时通道** | **不触发**。非 §2 P0④ 会计过账正确性破坏的「默认活跃路径」形态——MOVING_AVERAGE（默认计价法）路径 + FIFO delta 层「全量未消耗」常态余额守恒成立；漂移仅限「FIFO 物料 + delta 层被 forward 消耗后再红冲」特定组合（被 P2-RC-004 P1 涵盖），非默认活跃路径。 |
| **修复归口** | P2-MA2-029 独立修复（补 FIFO delta 层红冲三方一致性测试）归 MR1（纯测试代码，roadmap 预授权类目）；部分消耗漂移面随 P2-RC-004（P1）MR1 修复一并消除（forward + reverse 同 delta 层结构紧耦合，修 forward 必改 reverse）。 |

**整体裁决**：A1.5 §7 存疑点 2 经运行时余额守恒静态推理 **裁决维持 P2-MA2-029 = P2**。核心证据链：红冲余额回退（`reverseLine:182-186` `totalCost -= adjustAmount` + `avgCost = oldUnitCost`）与正向追加（`applyFifo:140` `totalCost += adjustAmount`）**严格对称** → delta 层物理删除（`removeFifoAdjustLayer:197` `eq("incomingMoveId", -line.getId())` 哨兵精确查 + `:199-201` 逐层 deleteEntity）与 delta 层追加（`appendFifoAdjustLayer:168` `setIncomingMoveId(-line.getId())` 哨兵写入）**镜像**。据 A4.1.15 §3.3 结论复核「部分消耗」可达性：常态（delta 层 `remainingQuantity=onHand` + `incomingDate≥原层` + 升序消耗原层独占满足）下 delta 层结构性永不被消耗 → `removeFifoAdjustLayer` 删除的恒为**全量未消耗 delta 层** → 余额守恒成立（删除未消耗层 totalCost=adjustAmount 与余额回退量一致 + 不破坏 FIFO 出库已消耗的原入库层）。A4.1.15 §3.4 跨期/多层场景下 delta 层可被部分消耗致 reverse 时层不变量漂移，但**该漂移被 P2-RC-004（P1，forward，同根因）完全涵盖**——reverse 路径无独立错误（严格对称回退 + 哨兵精确删除），forward+reverse 同 delta 层结构紧耦合故修 forward 必改 reverse，按 §去重协议不重复升级。本验证**不实施修复**（成本过账/删除核心路径保护区域 + plan Non-Goals），只确认分级 + 登记修复归口。

---

## 1. 存疑点原文 + L1 需求契约

> 存疑点来源：`2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 2（逐字引用）。

| # | 存疑点 | 触发条件 | 交 MA4 |
|---|---|---|---|
| 2 | **FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除**的余额守恒（`removeFifoAdjustLayer` 物理删除已部分消耗的 delta 层是否破坏已扣减层 / 余额 totalCost 漂移） | 实际启用 FIFO 物料的到岸成本分摊后部分出库再红冲 | A4.1 运行时探针（闭合 P2-MA2-029 子场景） |

**L1 需求契约**（`docs/design/finance/use-cases.md:197`，UC-FIN-10 LC-L3，逐字）：

> 后续出库按更新后的队列单价计算

**红冲闭环隐含不变量**（余额守恒）：成本调整红冲后，余额（`ErpInvStockBalance.totalCost`/`avgCost`）与成本层状态应回退至成本调整前一致状态（modulo 调整 apply↔reverse 之间的独立出库操作）。FIFO 计价下，此不变量体现为 `balance.totalCost == Σ layer.remainingQuantity × layer.unitCost`（FIFO 层不变量）经 apply↔reverse 对称保持。

**L2 owner doc 契约**（`docs/design/finance/costing-methods.md`）：
- §成本调整 §实现注记 `:472`：「FIFO 物料追加「delta 调整层」（`ErpInvCostLayer`，unitCost=新旧单位成本差，incomingMoveId=-行ID 负值哨兵区别于正常移动单正 ID），保持 FIFO 队列先进先出不变量；**reverse 据此精确删除**。」
- §到岸成本红冲实现注记 `:64`：「(3) ... 调 `CostAdjustmentService.reverseCostAdjust` 反向应用成本层（... FIFO：按 `-line.id` 哨兵删调整层 ...）」。
- §残留风险（Deferred）`:66`：「FIFO 调整层已部分被后续出库消耗时 `removeFifoAdjustLayer` 直接物理删除可能破坏已扣减层——已由 Phase 4 单测覆盖 MOVING_AVERAGE 主路径（FIFO 边界场景归 successor，触发条件：实际启用 FIFO 物料的到岸成本红冲遇此场景时）。」

> **本验证核心问题**：L2 `:66` 登记的「部分消耗后物理删除」残留风险——本验证据 A4.1.15 结论复核「部分消耗」是否可达，并评估可达 / 不可达两种情形下的余额守恒。

---

## 2. 红冲余额回退逻辑核验（L3，file:line）

> 实仓逐行核实 `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java`（319 行，HEAD `112a4b493`）。

### 2.1 红冲入口分派

`CostAdjustmentService.reverseCostAdjust:78-82`：按行调 `reverseLine(adjust, line)`。

### 2.2 reverseLine 余额回退 + delta 层删除（核心证据）

`CostAdjustmentService.reverseLine:175-192`：
- `:176` `ormTemplate.flushSession()`——红冲前刷会话，确保 apply 期间写入的 delta 层 / 余额可见。
- `:177-178` `findBalance(...)`——按 (orgId, materialId, warehouseId, batchNo) 重查余额。
- `:179-180` `adjustAmount = nz(line.getAdjustAmount())` + `oldUnitCost = nz(line.getOldUnitCost())`——从调整行读取 apply 时固化的 adjustAmount / oldUnitCost（apply `applyLine:114-116` 写入）。
- `:182-186` **余额回退**（若 balance != null）：
  - `:183` `balance.setTotalCost(nz(balance.getTotalCost()).subtract(adjustAmount))`——**totalCost -= adjustAmount**（与正向 `applyFifo:140` `totalCost += adjustAmount` 严格对称）。
  - `:184` `balance.setAvgCost(oldUnitCost)`——avgCost 回退至调整前 oldUnitCost。
  - `:185` `saveOrUpdateEntity(balance)`。
- `:188` `removeFifoAdjustLayer(line)`——**delta 层物理删除**（§3 详核）。
- `:189-191` `if (STANDARD_REVALUATION) removeFirmedRollup(...)`——标准重估额外删 FIRMED rollup（非本存疑点范围）。

**核验结论**：红冲**先回退余额（`:182-186`）再删除 delta 层（`:188`）**，顺序正确。余额回退 `totalCost -= adjustAmount` 与正向 `totalCost += adjustAmount` **严格对称**——无论 apply↔reverse 之间是否有独立出库，`balance.totalCost` 都被精确回退了 apply 时增加的 adjustAmount。`avgCost = oldUnitCost` 回退至调整前值。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（需求正确性 / 验证充分性 / 回归风险）**：余额回退算术对称性经 file:line 证实；余额守恒的「充分可验证性」缺口在 FIFO 层不变量侧（§5），非 balance.totalCost 回退本身——balance.totalCost 回退恒成立（对称算术）。

---

## 3. delta 层物理删除哨兵机制核验（L3，file:line）

### 3.1 物理删除逻辑

`CostAdjustmentService.removeFifoAdjustLayer:194-202`：
- `:195` `dao = daoProvider.daoFor(ErpInvCostLayer.class)`。
- `:196-197` `QueryBean q = new QueryBean(); q.addFilter(eq("incomingMoveId", -line.getId()))`——**`-line.getId()` 哨兵精确查 delta 层**。
- `:198` `List<ErpInvCostLayer> layers = dao.findAllByQuery(q)`。
- `:199-201` `for (layer : layers) dao.deleteEntity(layer)`——**逐层物理删除**。

### 3.2 哨兵机制镜像对照（追加写 vs 红冲删）

| 方向 | 方法 | 行 | 哨兵操作 |
|---|---|---|---|
| 正向追加 | `appendFifoAdjustLayer` | `:168` `layer.setIncomingMoveId(-line.getId())` | 写 `-lineId` 哨兵 |
| 红冲删除 | `removeFifoAdjustLayer` | `:197` `eq("incomingMoveId", -line.getId())` | 按 `-lineId` 哨兵查删 |

**哨兵精确匹配核验**：
- 正常入库层（`FifoCostingStrategy.appendCostLayer:168`）`incomingMoveId = move.getId()`（**正值**，移动单主键）。
- delta 调整层（`appendFifoAdjustLayer:168`）`incomingMoveId = -line.getId()`（**负值**，调整行主键取负）。
- 红冲 `eq("incomingMoveId", -line.getId())` 查负值 → **仅命中 delta 层，绝不误删正常入库层**（正常层 incomingMoveId 为正，哨兵为负不冲突）。

**核验结论**：哨兵机制正确——正向写 `-lineId`，红冲按 `-lineId` 精确删，正负值域隔离保证不误伤正常入库层。与 L2 `:472`「reverse 据此精确删除」+ `:64`「按 `-line.id` 哨兵删调整层」一致。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（架构/边界 / 回归风险）**：哨兵机制无跨模块依赖变更，正负值域隔离是确定性设计（非偶然），无「换一个输入值就崩」的脆弱路径。

---

## 4. 「部分消耗」可达性复核（与 A4.1.15 结论交互——核心）

> A4.1.15（done，P2-RC-004 升级 P1）已对 delta 层 forward 消耗路径作静态数值推理。本节复用其结论复核「delta 层部分消耗后物理删除」是否可达。

### 4.1 delta 层结构（A4.1.15 §2.3 已证，复用）

`appendFifoAdjustLayer:149-171`：
- `:160-161` `setIncomingQuantity(qty)` + `setRemainingQuantity(qty)`，**qty = onHand**（`applyFifo:146` 传入调整时全量现有量）。
- `:162` `setUnitCost(deltaUnitCost)`（Δ = newUnitCost − oldUnitCost）。
- `:165-166` `setIncomingDate(adjust.businessDate)`（到岸成本审核日 / 成本调整业务日）。
- `:168` `setIncomingMoveId(-line.getId())` 哨兵。

**delta 层结构定论**：`{remainingQuantity=onHand, unitCost=Δ, incomingDate=adjust.businessDate, incomingMoveId=-lineId}`。

### 4.2 常态「不可达」证明（A4.1.15 §3.3 复用）

`FifoCostingStrategy.findFifoLayers:178-205`：
- `:188` 过滤 `remainingQuantity > 0`。
- `:199` 过滤 `incomingDate <= businessDate`（历史成本守卫）。
- `:202-203` 按 `incomingDate` **升序**排序。

`onOutgoing:103-120` 升序消耗循环：`:106-107` `remaining <= 0` 即 break。

**常态推理**（单原入库层 L0，delta 层 D）：
1. `rD = onHand = r0`（调整时 onHand 即原层 remaining）。
2. `dD = adjust.businessDate ≥ d0`（到岸成本/调整发生在入库之后）。
3. 升序序列首部为 L0（d0 ≤ dD）。
4. 出库 `q ≤ onHand = r0`：循环从 L0 起耗，`take = min(q, r0) = q` → `remaining = 0` → break。
5. **delta 层 D 永不被到达**，`remainingQuantity` 恒 = onHand（全量未消耗）。

→ **常态下 `removeFifoAdjustLayer` 删除的恒为全量未消耗 delta 层**（remainingQuantity=onHand，totalCost=adjustAmount）。

### 4.3 边角「可达」场景（A4.1.15 §3.4 + 跨期场景）

A4.1.15 §3.4 已证两种 delta 层**可被部分消耗**的场景：

**场景 A（多原入库层 + delta 层日期落中间）**：原层 L0(d0)、L2(d2)，delta 层 D(dD) 满足 `d0 < dD < d2`（到岸成本审核日落在两次入库之间）。升序序列 `L0、D、L2`，出库耗尽 L0 后**部分消耗 D**（D 的 unitCost=Δ 是单价差非真实单价，Σ 错位——此为 P2-RC-004 forward 缺陷的表现）。

**场景 B（跨期：原层耗尽 → 新增更晚日期入库层 → 后续出库）**：
- T1 delta 层 D 创建（remaining=onHand，dD）。
- T2 出库耗尽所有 d ≤ dD 的原层 → balance.totalQuantity 可降至 0，但 D（remaining=onHand）未被到达（原层独占满足）。
- T3 新增入库层 L3（d3 > dD）→ balance 回补。
- T4 出库：升序 D(dD) 先于 L3(d3) → **D 被部分消耗**（D 的幻影 remaining=onHand > 真实可代表量）。

两种场景下 D 被部分消耗（remainingQuantity < onHand，totalCost < adjustAmount），均为 P2-RC-004（P1，forward delta 层 remaining=onHand 幻影量 + Δ 单价结构缺陷）的表现。

### 4.4 可达性裁决

| 情形 | 「部分消耗」可达性 | reverse 删除对象 |
|---|---|---|
| **常态**（单原层 / 原层 remaining 充足覆盖出库） | **不可达**（A4.1.15 §3.3：delta 层结构性永不被消耗） | 全量未消耗 delta 层（remaining=onHand, totalCost=adjustAmount） |
| **边角**（多层日期落中间 / 跨期原层耗尽再补） | **可达**（A4.1.15 §3.4 + 本节场景 B）但属 P2-RC-004 forward 缺陷表现 | 部分消耗 delta 层（remaining<onHand, totalCost<adjustAmount） |

**明确裁决**：「部分消耗」在**常态不可达**（delta 层恒全量未消耗）；**边角可达但被 P2-RC-004（P1）完全涵盖**——边角场景本身是 forward delta 层消耗缺陷（幻影量 + Δ 单价），非 reverse 路径独立引入的可达性。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（需求正确性 / 路由正确性）**：本验证据 A4.1.15 结论复核而非重跑 forward 数值推理（§去重协议，A4.1.15 已 done），路由正确（MA4 运行时行为验证）。

---

## 5. 余额守恒一致性评估（据可达性裁决推理）

### 5.1 常态（delta 层全量未消耗）—— 余额守恒成立

设 apply 前 balance.totalCost = C0，onHand = Q0。
- **apply**（`applyFifo:140-146`）：`balance.totalCost = C0 + adjustAmount`（adjustAmount = Δ × Q0）；delta 层创建 totalCost = adjustAmount。
  - FIFO 不变量：原层(C0) + delta(Q0×Δ=adjustAmount) = C0 + adjustAmount ✓ == balance.totalCost。
- **intervening 出库**（消耗原层，不触 delta 层，§4.2）：消耗原层成本 c → balance.totalCost = C0 + adjustAmount − c；delta 层 totalCost 不变 = adjustAmount。
  - FIFO 不变量：原层(C0−c) + delta(adjustAmount) = C0 + adjustAmount − c ✓ == balance.totalCost。
- **reverse**（`reverseLine:182-186` + `removeFifoAdjustLayer`）：`balance.totalCost -= adjustAmount` → C0 − c；删除 delta 层（totalCost=adjustAmount，**全量未消耗**）。
  - FIFO 不变量：原层(C0−c) + delta(已删) = C0 − c ✓ == balance.totalCost。

**常态裁决**：余额守恒**成立**——删除全量未消耗 delta 层不破坏 FIFO 出库已消耗的原入库层（原层 remainingQuantity 已被出库扣减，删除 delta 层不影响原层）+ 余额回退量（adjustAmount）与删除层 totalCost（adjustAmount）一致 + FIFO 不变量保持。

### 5.2 边角（delta 层部分消耗）—— 层不变量漂移，但被 P2-RC-004 涵盖

以场景 B（跨期）数值推演（Q0=100, Δ=+2, adjustAmount=200）：
- T1 apply：原层 L0=100@10(1000)，delta D=100@2(200)。balance.totalCost=1200。
- T2 出库 100：耗 L0(1000)。balance.totalCost=200，totalQuantity=0。D 未被到达（remaining=100）。
- T3 入库 50：L3=50@10(500)。balance.totalCost=700，totalQuantity=50。
- T4 出库 50：升序 D(dD) 先于 L3(d3)。耗 D 50：D.remaining=50，D.totalCost=100。balance.totalCost=600。
  - **D 部分消耗**（remaining 100→50，totalCost 200→100）。
- T5 reverse：`balance.totalCost -= adjustAmount(200)` → **400**；删 D（当前 totalCost=100，**部分消耗态**）。
  - FIFO 不变量：L3(500) = 500 **≠** balance.totalCost(400) → **层不变量漂移 100**（= D 已消耗部分 50×2）。

**漂移根因**：`reverseLine:183` 减去**原始 adjustAmount(200)**（apply 时固化于 `line.adjustAmount`），但 D 当前 totalCost 已因 forward 部分消耗降至 100 → 删除层代表的成本(100) ≠ 回退量(200) → balance 与层不对账。

**漂移归属**：
1. 漂移**仅**在 delta 层被 forward 部分消耗后出现（§4.3 边角场景，P2-RC-004 forward 缺陷）。
2. reverse 路径**无独立错误**——`reverseLine:183` 按 `line.adjustAmount` 对称回退（与 `applyFifo:140` 严格对称），`removeFifoAdjustLayer` 哨兵精确删除（§3），均无独立逻辑缺陷。漂移源于 forward 缺陷在 apply↔reverse 间**变异**了 delta 层状态。
3. forward + reverse **同 delta 层结构紧耦合**：任何 P2-RC-004 修复（如 A4.1.15 §9 方向 A 重述原层单价，不再追加 delta 层）必然**重写 reverse 路径**（removeFifoAdjustLayer 失效，改 un-restate 原层）→ reverse 漂移面随 forward 修复一并消除，无独立修复路径。

**边角裁决**：层不变量漂移**存在但被 P2-RC-004（P1，forward，同根因 = delta 层 remainingQuantity=onHand 幻影量）完全涵盖**。按方法论 §去重协议**不重复升级同一根因**（P2-RC-004 已 P1 捕获根因；升 P2-MA2-029 = P1 将双重计数同根因于两个 finding）。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（验证充分性 / 回归风险 / 待办策略漂移）**：余额守恒在常态可独立验证（对称算术 + §5.1 数值推演）；边角漂移的「充分可验证性」归 P2-RC-004 forward 路径（A4.1.15 已 done）；本验证未将边角漂移无声升 P1（避免同根因重复升级），亦未将 P2-MA2-029 降级 deferred（维持 P2 watch-only successor，符合 §去重协议）。

---

## 6. 既有测试覆盖边界普查（L4）

> grep `TestErpInvFifoCosting` + `TestErpInvLandedCostReversal` + `TestErpInvCostAdjust` 全集（HEAD `112a4b493`）。

| 测试文件 | 覆盖范围 | FIFO delta 层红冲删除覆盖 | 断言强度 |
|---|---|---|---|
| `TestErpInvFifoCosting.java`（6 @Test） | FIFO **正常入库层**消耗 + 红冲 | `#testReverseRestoresCostInvariant:163-204` 红冲的是**出库移动单**（`ErpInvStockMove__reverse`），经 `FifoCostingStrategy.onIncoming` 反向入库追加新层恢复 Σ remaining×unitCost 不变量——**覆盖正常层红冲，非 delta 调整层**（delta 层由 `CostAdjustmentService` 追加，不经 StockMove reverse 路径） | 深（正常层 Σ 不变量，容差 0.01） |
| `TestErpInvLandedCostReversal.java`（2 @Test） | 到岸成本 reverseApprove 红冲 | `:90`/`:150` 物料均 `COST_METHOD_MOVING_AVERAGE`——**FIFO 物料到岸成本红冲零覆盖**；红冲经 `reverseCostAdjust` 走 MA 路径（`balance.avgCost = oldUnitCost` 回退），不触 `removeFifoAdjustLayer` | 深（MA 路径） |
| `TestErpInvCostAdjust.java`（8 @Test） | 成本调整 8 类 | `#testReverseRollsBackBalanceAndVoucher:262-296` 红冲测试用 **MA 物料**（`:265` `COST_METHOD_MOVING_AVERAGE`）——断言 avgCost/totalCost 回退 + 原 NORMAL 凭证 isReversed；**不覆盖 FIFO delta 层 reverse（removeFifoAdjustLayer 删除 + 余额守恒）**。`#testFifoAppendsAdjustLayerAndOutgoingConsumes:144-175` 触及 FIFO delta 层**正向追加 + 出库**（A4.1.15 已证弱断言 unitCost>0 掩盖 forward 缺陷），但**不测 reverse** | **浅**（FIFO delta 层 reverse 路径零覆盖） |

**测试覆盖边界定论**：
1. FIFO **正常入库层**红冲不变量 = **深覆盖**（`testReverseRestoresCostInvariant`，Σ remaining×unitCost 容差断言）。
2. 到岸成本红冲 MA 路径 = **深覆盖**（`TestErpInvLandedCostReversal`）。
3. 成本调整 reverse MA 路径 = **深覆盖**（`testReverseRollsBackBalanceAndVoucher`）。
4. **FIFO delta 调整层 reverse（removeFifoAdjustLayer 哨兵删除 + 余额守恒 + 三方一致性[成本层 + 余额 + 流水]）= 零覆盖**——无任何测试断言「FIFO 物料成本调整/到岸成本红冲后 delta 层被哨兵精确删除 + balance.totalCost 守恒 + FIFO 层不变量保持」。

> 本验证**不重复登记**测试覆盖缺口（P2-MA2-029 已登记）；本验证的增量 = 经余额守恒评估确认 P2-MA2-029 维持 P2（删除逻辑正确，仅三方一致性测试缺失），非升级。

> **Skill `multi-dimensional-audit-prompt.md` 维度裁决（验证充分性）**：FIFO delta 层 reverse 路径零覆盖是客观缺口，但删除逻辑（哨兵精确）+ 余额回退（对称算术）经 §2/§3 静态证实正确，故缺口定性为「测试覆盖补强」（P2）非「行为缺陷」（P1）。

---

## 7. MA4↔A5.6 边界声明

> 方法论 §去重协议 MA4↔A5.6：本验证审「行为是否符合需求」（FIFO delta 层红冲删除余额是否守恒），与 A5.6（audit-remediation E2E 断言强度，审测试质量视角）边界按此执行。

- 本验证**不重做** A5.6 E2E 断言强度审计（A5.6 `2026-07-29-1430-arm-ma5-e2e-effectiveness.md` 已覆盖 E2E 业务断言强度分类）。
- 本验证只评 delta 层红冲删除**行为正确性**（L3 代码路径余额守恒推理）+ 既有测试覆盖边界（§6）。
- A5.6 视角的「FIFO delta 层红冲 E2E 断言强度」若未来审计，其输入 = 本验证 §6 揭示的「FIFO delta 层 reverse 路径零覆盖」证据。

---

## 8. P2-MA2-029 分级确认/调整（方法论 §2 判据 + 三源对照 + §去重协议）

### 8.1 分级裁决：维持 P2

| 维度 | 评估 | 命中 |
|---|---|---|
| **§2 P0④ 会计过账正确性破坏** | **不成立**——余额守恒在常态（delta 层全量未消耗）成立；漂移仅限「FIFO 物料 + delta 层被 forward 消耗后再红冲」边角组合（被 P2-RC-004 P1 涵盖），非默认活跃路径（默认 MOVING_AVERAGE 路径 + FIFO delta 层常态余额守恒均成立）；技术可逆（reverse 按 `-lineId` 哨兵删 delta 层 + 对称回退余额） | ❌ |
| **§2 P0① 活跃数据破坏** | **不成立**——需「物料配 FIFO + 成本调整/到岸成本 apply + delta 层被 forward 部分消耗 + reverse」前置链，非默认活跃路径 | ❌ |
| **§2 P1① 行为实质偏离验收标准** | **不成立（reverse 路径无独立缺陷）**——reverse 余额回退（`reverseLine:183` 对称 `-= adjustAmount`）+ delta 层删除（哨兵精确）在常态行为正确（§5.1 余额守恒成立）；边角漂移（§5.2）被 P2-RC-004（P1，forward 同根因）完全涵盖，按 §去重协议不重复升级。reverse 路径无独立于 P2-RC-004 的行为偏离。 | ❌ |
| **§2 P2① 次要验收标准未完全满足（主路径 OK 边界弱）** | **成立**——FIFO delta 层红冲三方一致性（成本层 + 余额 + 流水）测试覆盖缺失（§6 定论 4）；主路径（MA reverse 强测 + FIFO delta 层常态余额守恒 §5.1 静态成立）OK，边界（FIFO delta 层 reverse 路径）测试弱/零覆盖 | ✅ |

**分级结论**：**维持 P2**（§2 P2① 命中）。P2-MA2-029 独立缺口 = FIFO delta 层红冲三方一致性测试覆盖缺失（删除逻辑正确 + 余额回退对称，仅测试缺失）。边角漂移不构成独立 P1（被 P2-RC-004 涵盖）。

### 8.2 三源对照

| 源 | 内容 | 与本裁决一致性 |
|---|---|---|
| **L1**（`use-cases.md:197`） | LC-L3 + 红冲闭环隐含不变量（余额/层状态回退至调整前） | 常态余额守恒成立（§5.1）→ 契约满足；边角漂移被 P2-RC-004（forward LC-L3 未实现）涵盖 ✅ 一致（维持 P2） |
| **L2**（`costing-methods.md:472,64,66`） | delta 层「reverse 据此精确删除」+「FIFO 边界场景归 successor」 | 哨兵精确删除经 §3 证实与 L2 一致；`:66` successor 触发条件「实际启用 FIFO 物料的到岸成本红冲遇此场景时」经本验证细化：常态不触发（余额守恒成立），边角触发但归 P2-RC-004 ✅ 一致 |
| **L3**（`CostAdjustmentService.reverseLine:182-188` / `removeFifoAdjustLayer:194-202` / `appendFifoAdjustLayer:160-168` / `FifoCostingStrategy.findFifoLayers:178-205`） | 红冲对称回退 + 哨兵删除 + delta 层结构 + 升序消耗 | 余额守恒常态成立 / 边角漂移归属 P2-RC-004 ✅ 一致 |

### 8.3 与 A1.5 §5.3 P2-MA2-029 复用结论的分层一致性

- A1.5 §5.3 裁决「FIFO 物料到岸成本红冲测试缺口（delta 层部分消耗后物理删除无测试）= **P2**，复用 P2-MA2-029」——其依据是「假设删除逻辑正确，仅测试缺失」。
- 本验证**证实了该假设**：删除逻辑（哨兵精确 + 对称回退）正确（§2/§3），常态余额守恒成立（§5.1）；「部分消耗后删除」边角漂移被 P2-RC-004 涵盖（§5.2）。
- 故本验证**维持** A1.5 §5.3 的 P2 定级（证实而非证伪其前提假设），与 A1.5 §5.3 复用 P2-MA2-029 结论一致。

### 8.4 与 A4.1.15 P2-RC-004=P1（forward 路径）的分层一致性

| finding | 方向 | 控制点 | 根因 | 分级 |
|---|---|---|---|---|
| **P2-RC-004**（A4.1.15） | forward（出库消耗 delta 层） | delta 层升序消耗数值正确性（LC-L3 FIFO 路径未实现） | delta 层 remaining=onHand 幻影量 + Δ 单价结构 | **P1** |
| **P2-MA2-029**（本验证） | reverse（红冲删除 delta 层） | delta 层哨兵物理删除 + 余额守恒 | 同根因（delta 层 remaining=onHand 幻影量致边角可被 forward 部分消耗） | **P2**（维持） |

**分层一致性**：不同方向（reverse 删除 vs forward 消耗）不同控制点，**不合并**（§去重协议）；同根因（delta 层 remaining=onHand）——A4.1.15 §7.4 已声明「P2-MA2-029 reverse 路径风险更值得关注」，本验证经 §5 量化后裁决：reverse 漂移面**完全被 P2-RC-004 涵盖**（forward+reverse 同结构紧耦合，修 forward 必改 reverse）。**修复 P2-RC-004 的 MR1 将一并消除 P2-MA2-029 的 reverse 漂移面**；P2-MA2-029 独立剩余 = FIFO delta 层红冲三方一致性测试补强（P2，随修复补深断言测试）。

### 8.5 与既有 finding 的去重（§去重协议）

| 既有 finding | 控制点 | 与本裁决的关系 |
|---|---|---|
| **P2-MA2-029**（arm-index `:717`） | CostAdjustment FIFO 红冲 delta 层物理删除三方一致性（reverse 路径） | **本验证即评估此 finding**，维持 P2，追加 A4.1.16 运行时余额守恒评估注记 |
| **P2-RC-004**（arm-index `:134`，A4.1.15 升 P1） | FIFO + 到岸成本 delta 层 forward 消耗 | 不同方向不同控制点（forward vs reverse），不合并；同根因，边角漂移被其涵盖（§5.2/§8.4） |
| **A1.5 §7-1**（A4.1.15 done） | forward delta 层消耗数值正确性 | 不同存疑点（§7-1 vs §7-2），不合并 |
| **P1-MA4-021**（resolved） | 测试有效性（STANDARD/SPECIFIC，未含 FIFO delta 层红冲） | 已 resolved 且范围未含本控制点，不合并 |

---

## 9. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`112a4b493`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter，真正门控在 CI workflow（`.github/workflows/compliance.yml`）。本报告**不**以 checker 脚本退出码作为门控通过依据。**本审计无生产代码变更**（纯审计报告），checker 无回归风险（actual 反映既有 HEAD 状态，非本审计引入）。actual 与 A4.1.15 baseline（HEAD `fe32f4c21`）一致（零代码变更期间 R1-R2 计数稳定）。

  | 规则 | 描述 | baseline（A4.1.15 `fe32f4c21`） | actual（HEAD `112a4b493`） | delta | 说明 |
  |---|---|---|---|---|---|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 | — |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 | — |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 | — |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 | — |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | 0 | — |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | 0 | — |
  | R2d | Processor daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R3-R12 | （脚本 R3 段起既有行为未输出计数，A4.1.11/A4.1.13/A4.1.15 已记录） | 同基线 | 同基线（零代码变更） | 0 | 不适用（脚本既有行为） |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告维持 P2-MA2-029（不新建 finding），已按 §7 规则 grep arm-index 同域同控制点（P2-RC-004 forward / A1.5 §7-1 / P1-MA4-021 resolved）后给出「维持既有不新建」裁决（§8.5），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9 冻结）：本审计未修改 product-scope / use-cases / costing-methods 需求契约段落。L2 `costing-methods.md:66` 的「部分消耗后物理删除」残留风险经本验证细化（常态不触发 / 边角归 P2-RC-004），记入报告（§4/§5/§8.2），不直改 L2（§真相源冻结条款）。
- [x] **保护区域纪律声明**：本审计为只读评估，未修改 `CostAdjustmentService.reverseLine`/`removeFifoAdjustLayer`/`appendFifoAdjustLayer`/`FifoCostingStrategy`（成本过账/删除核心路径，保护区域）。修复须 ask-first + 独立 plan-audit（§5 会计过账/数据删除逻辑类）。

---

## §自检清单（报告产出前强制）

- [x] §1 存疑点原文 + L1 需求契约（UC-FIN-10 LC-L3 + 红冲闭环不变量逐字）
- [x] §2 红冲余额回退逻辑核验（L3 file:line，reverseLine:175-192 对称回退 + 顺序）
- [x] §3 delta 层物理删除哨兵机制核验（L3 file:line，removeFifoAdjustLayer:194-202 + appendFifoAdjustLayer:168 镜像 + 正负值域隔离）
- [x] §4 「部分消耗」可达性复核（与 A4.1.15 §3.3/§3.4 交互，常态不可达 / 边角可达但归 P2-RC-004）
- [x] §5 余额守恒一致性评估（常态成立 §5.1 / 边角漂移被 P2-RC-004 涵盖 §5.2）
- [x] §6 既有测试覆盖边界普查（3 测试文件覆盖矩阵 + FIFO delta 层 reverse 零覆盖定论）
- [x] §7 MA4↔A5.6 边界声明
- [x] §8 P2-MA2-029 分级裁决（维持 P2 + 三源对照 + 与 A1.5 §5.3 + A4.1.15 P2-RC-004 分层一致 + 去重）
- [x] §9 过程纪律自检（checker actual vs baseline + 独立性 + 交叉去重 + 真相源未修改 + 保护区域）

**报告完整性自检结论**：§1-§9 全部存在，无缺失。
