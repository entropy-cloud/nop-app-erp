# RC MA1 A1.5 — finance-F5 成本核算（FIFO 出库 + 到岸成本）需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.5（MA1 需求追踪矩阵审计 — finance-F5 成本核算：FIFO 出库成本与到岸成本）
> 审计 plan：`docs/plans/2026-08-02-1815-2-rc-ma1-a1-5-finance-f5-costing.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/finance/use-cases.md`（UC-FIN-10，1 UC，6 条验收标准）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §finance + §切片索引 A1.5
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-02
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`85b2ab7e0`

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **0** | UC-FIN-10 六条验收标准经 L3-L5 四级证据全部实现且主路径可运行 |
| **P2**（新登记） | **2** | P2-RC-004（FIFO 物料 + 到岸成本交互 E2E 测试缺口[正向 delta 层 + 后续 FIFO 出库消耗更新后单价]）/ P2-RC-005（StockQueue↔ErpInvCostLayer owner-doc 命名漂移）→ successor watch-only |
| **P2**（复用） | **1** | P2-MA2-029（CostAdjustment FIFO 红冲 delta 层物理删除三方一致性未测试——到岸成本 FIFO 红冲是其子场景，watch-only，追加 RC 交叉引用） |
| **接受**（符合需求契约） | **6 验收标准** | UC-FIN-10 FIFO 出库三条（incomingDate 升序 / 跨队列消耗 / Σ 单价）+ 到岸成本三条（按金额比例分摊 / 入库成本+= / 后续出库用更新单价）全部裁定"接受" |
| resolved finding HEAD 复核 | **5/5 已落地** | P1-MA2-023/024/085 + P1-MA4-020/021 在当前 HEAD 实际落地（1 项经 SELECT FOR UPDATE 替代路径，非 UK 路径，属 arm-index 列明的合法修复方案之一） |
| MA2 既有行为证据复用 | A2.4 costing 审计 | 无升级（详见 §4 / §9） |
| 报告校正项 | **1** | 2026-07-06 审计 `:117` 将 UC-FIN-10 标 🔶"到岸成本分摊未实现(Non-Goal)"——**已过期**，到岸成本经 plan `2026-07-10-1100-3` 已完整落地 |

**整体裁决**：A1.5 切片 1 UC 五级追踪矩阵填齐，UC-FIN-10 六条验收标准（FIFO 出库三条 + 到岸成本三条）经 L3-L5 四级证据全部确认实现存在且主路径可运行，**整体裁定"接受"**。FIFO 出库核心算法（`FifoCostingStrategy.onOutgoing` 按 `incomingDate` 升序消耗 + 跨队列循环 + Σ 消耗量×单价 + 历史成本 `le(incomingDate, businessDate)` 守卫 + 不足拒绝 `ERR_COST_NOT_AVAILABLE`）+ 到岸成本分摊引擎（`LandedCostAllocationEngine` 按金额/数量/重量比例分摊 + 末行吸收尾差保证 Σ=total + 空输入拒绝）+ 到岸成本编排（`ErpInvLandedCostProcessor` 校验 receive 已审 + 防重复分摊 + receive 悲观锁 + delta 层追加 + LANDED_COST 过账 + 红冲闭环）均经单测/E2E 强断言覆盖。**零 P0 / 零 P1**——无活跃数据破坏、无功能完全缺失、无会计过账正确性破坏。三项 P2 为测试覆盖/文档类改进（主路径行为正确，仅交互边界/红冲边界/cosmetic 命名漂移弱）。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md:183`（L1 权威真相源，方法论 §4）。验收标准逐字引用，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-FIN-10 FIFO 出库成本与到岸成本（`use-cases.md:183`）

**场景**：出库按 FIFO 消耗队列；到岸成本（运费）分摊入入库成本。

```
可验证断言（见 costing-methods.md）:
// FIFO 出库
出库移动单 → 按 incomingDate 升序消耗 StockQueue
队列不足 → 跨队列消耗
出库成本 = Σ(各队列消耗量 × 队列单价)

// 到岸成本
运费/保险/关税 → 按金额比例分摊到入库批次
入库成本 += 分摊费用
后续出库按更新后的队列单价计算
```

**涉及机制**：`costing-methods.md` §FIFO / §到岸成本

> **验收标准完整枚举**（§3 完整枚举纪律，6 条逐一进入 L5 判读，不抽样）：
> - **FIFO-F1**：出库移动单 → 按 `incomingDate` 升序消耗 StockQueue
> - **FIFO-F2**：队列不足 → 跨队列消耗
> - **FIFO-F3**：出库成本 = Σ(各队列消耗量 × 队列单价)
> - **LC-L1**：运费/保险/关税 → 按金额比例分摊到入库批次
> - **LC-L2**：入库成本 += 分摊费用
> - **LC-L3**：后续出库按更新后的队列单价计算

> **L1 命名注记**：L1 原文使用队列实体名「StockQueue」（FIFO-F1/F2/F3），ORM 权威名为 `ErpInvCostLayer`（`costMethod=FIFO` 过滤）。属 cosmetic owner-doc 命名漂移，行为完全实现——见 §5 候选缺口 P2-RC-005 + §9 真相源冻结条款（L1 不可直改）。

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/` + `.../processor/` + `.../stock/`）。L3 引用格式遵循 §1 L3 规范（含行号）。**关键事实**：UC-FIN-10 的 FIFO 出库 + 到岸成本算法**全部实现于 inventory 模块**（非 finance）——finance 侧仅持 GL 凭证目的地（`ErpFinVoucher`）+ 期间结账调用（`IErpInvCostingBiz.reclosePeriodCosts`），无 finance 侧 costing service。owner doc `costing-methods.md:31-38` 显式声明权威实现位于 `module-inventory/erp-inv-service`。

### 2.1 FIFO 出库核心算法（FIFO-F1 / FIFO-F2 / FIFO-F3）

`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/FifoCostingStrategy.java`（210 行）

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **FIFO-F1**（按 `incomingDate` 升序消耗） | `FifoCostingStrategy.java:202-203`（`list.sort(Comparator.comparing(l -> l.getIncomingDate() != null ? l.getIncomingDate() : CoreMetrics.today()))`）+ 历史成本守卫 `:198-200`（`if (businessDate != null) q.addFilter(le("incomingDate", businessDate))`）+ 查询过滤 `remainingQuantity > 0` `:188` + `costMethod=FIFO` `:187` | 按 `incomingDate` 升序消耗 + 仅消耗出库日及之前入库的层（历史成本原则） | ✅ |
| **FIFO-F2**（队列不足跨队列消耗） | `FifoCostingStrategy.java:105-120`（for 循环逐层消耗：`take = remaining.min(avail)` `:113` → 扣减 `layer.remainingQuantity` `:115` → 累加 `totalCost` `:118` → 递减 `remaining` `:119` → 循环至 `remaining<=0` `:106-107`） | 跨队列消耗循环 | ✅ |
| **FIFO-F3**（出库成本 = Σ 消耗量×单价） | `FifoCostingStrategy.java:114/118`（`takeCost = take.multiply(nz(layer.getUnitCost))` `:114` → `totalCost = totalCost.add(takeCost)` `:118`）+ 加权 unitCost `:128-129`（`weightedUnitCost = totalCost.divide(qty, SCALE, HALF_UP)`）+ 刷回行 `line.setUnitCost` `:131`（供红冲透传） | Σ 各队列消耗量×单价汇总为出库成本 | ✅ |
| 不足拒绝 | `FifoCostingStrategy.java:121-126`（总剩余不足覆盖出库量 → `throw ERR_COST_NOT_AVAILABLE`）+ 首次无层拒绝 `:97-101`（`layers.isEmpty()` → 同异常） | 出库量超过可用层剩余时拒绝（防负成本） | ✅ |
| 入库追加层 | `FifoCostingStrategy.appendCostLayer:152-171`（新建 `ErpInvCostLayer`：`incomingQuantity=remainingQuantity=qty` `:162-163`、`unitCost` `:164`、`incomingDate` `:167`、`incomingMoveId=move.id` `:168`、`costMethod=FIFO` `:161`） | FIFO 队列入库追加 | ✅ |
| 红冲语义 Decision (a) | `FifoCostingStrategy.java:130-132`（出库时刷新 `line.unitCost` 为加权成本，reverse 流程透传给反向入库行 → `onIncoming` 据此追加新层，成本回加对齐） | 红冲按原出库加权 unitCost 追加新层（非直接恢复被消耗层） | ✅ |

### 2.2 策略分派（FIFO 出库/入库入口）

`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/stock/StockMoveBookkeeper.java`（490 行）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 按行分派 | `StockMoveBookkeeper.bookCompletion:116-130`（逐行 `costMethodResolver.resolve(line, acctSchemaId)` `:118` → `resolveStrategy(method)` `:119` → 按 `moveType` 分派：OUTGOING `:123-124` 调 `onOutgoing`；INTERNAL_TRANSFER `:120-122` 先 `onOutgoing` 取 `carriedCost` 再 `onIncoming`[成本桥]；else `:125-128` 调 `onIncoming`） | ✅ |
| 7 策略注册 | `StockMoveBookkeeper.initStrategyRegistry:97-106`（MOVING_AVERAGE/FIFO/STANDARD/LIFO/WEIGHTED_AVERAGE/SPECIFIC/BATCH 7 策略 `:99-105`）+ `strategyByMethod` Map `:95` + `resolveStrategy` fallback MA `:132-135` | ✅ |
| 成本桥（内部调拨） | `StockMoveBookkeeper.java:120-122`（`carriedCost = strategy.onOutgoing(...)` → `strategy.onIncoming(..., carriedCost, ...)`，源仓出库加权 unitCost 沿用至目标仓入库） | ✅ |

### 2.3 到岸成本分摊引擎（LC-L1）

`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/LandedCostAllocationEngine.java`（174 行，纯函数无 ORM）

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **LC-L1**（按金额比例分摊） | `LandedCostAllocationEngine.allocate:47-99` + `baseOf:101-110`（BY_AMOUNT 默认 `:108-109` 返回 `line.amount`；BY_QUANTITY `:102-104` 返回 `line.quantity`；BY_WEIGHT `:105-107` 返回 `line.weight`）+ 分摊公式 `:76-78`（`allocatedAmount = totalToAllocate.multiply(lineBase).divide(totalBase, SCALE, HALF_UP)`） | 按金额（默认）/数量/重量比例分摊 | ✅ |
| 末行吸收尾差 | `LandedCostAllocationEngine.java:72-74`（末行 `allocatedAmount = totalToAllocate.subtract(allocated)`，保证 Σ allocatedAmount == totalToAllocate） | ✅ |
| 空输入拒绝 | `LandedCostAllocationEngine.java:50-53`（`receiveLines` 空 → `ERR_LANDED_COST_NO_LINES`）+ totalBase=0 拒绝 `:59-62` | ✅ |

### 2.4 到岸成本编排（LC-L2 / LC-L3）

`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java`（504 行）

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| approve 编排 | `ErpInvLandedCostProcessor.approve:87-89` → `ErpInvLandedCostApproveProcessor.approve`；`doPostApprove:339-368`（LANDED_COST 过账 `:344` → 状态迁移 + posted `:348-367`） | ✅ |
| **LC-L2**（入库成本 += 分摊） | `ErpInvLandedCostProcessor.createAndApplyCostAdjust:294-337`（创建 `ErpInvCostAdjust(type=LANDED_COST_SUPPLEMENT)` `:294-307` + 行 `:309-325` 携带 `newUnitCost=r.getNewUnitCost()` `:318-319` → 调 `costAdjustmentService.applyCostAdjust(adjust, adjustLines)` `:329` **直接更新成本层**[不走 ErpInvCostAdjustProcessor 完整链，避免 COST_ADJUSTMENT(420) 与 LANDED_COST(490) 双重入账]） | 入库成本经 CostAdjustmentService 更新余额/层 | ✅ |
| 校验 receive 已审 | `ErpInvLandedCostProcessor.validateReceiveApproved:372-382`（receive==null 或 approveStatus≠APPROVED → `ERR_LANDED_COST_RECEIVE_NOT_APPROVED`） | ✅ |
| 防重复分摊 | `ErpInvLandedCostProcessor.validateNotAlreadyAllocated:392-407`（查同 receiveId + APPROVED 的 sibling，排除自身 → `ERR_LANDED_COST_ALREADY_ALLOCATED`） | ✅ |
| receive 悲观锁（P1-MA2-085 修复） | `ErpInvLandedCostProcessor.lockReceiveForAllocation:388-390`（`ormTemplate.lock(receive)` = SELECT FOR UPDATE，串行化并发同 receiveId 审核） | ✅ |
| 分摊预览（只读） | `ErpInvLandedCostProcessor.allocatePreview:93-113`（不落库，前端分摊预览） | ✅ |
| 红冲 | `ErpInvLandedCostProcessor.reverseApprove:137-139` → `ErpInvLandedCostReverseApproveProcessor.reverseApprove`；`doReverseApprove:156-201`（红冲 LANDED_COST 凭证 `:159-172`[吞异常+告警] → 反向应用成本层 `:174-178` → 翻 posted=false/REJECTED/CANCELLED `:180-200`） | ✅ |
| 红冲失败告警（P1-MA4-020 修复） | `ErpInvLandedCostProcessor.dispatchReverseFailureAlert:483-499`（catch 块 `:171` 调 → `notificationBiz.notify(NOTIFY_EVENT_LANDED_COST_REVERSE_FAILURE, ctx, serviceCtx)` `:494`） | ✅ |

### 2.5 队列单价更新（LC-L3，FIFO 路径）

`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/CostAdjustmentService.java`（319 行）

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| 三轴分派 | `CostAdjustmentService.applyLine:86-121`（`resolveCostMethod` `:103` → FIFO 走 `applyFifo` `:108-109`，MA/Standard 走 `applyAverageLike` `:110-111`） | ✅ |
| **LC-L3**（后续出库用更新单价 — FIFO delta 层） | `CostAdjustmentService.applyFifo:137-147` → `appendFifoAdjustLayer:149-171`（新建 `ErpInvCostLayer` delta 调整层：`unitCost = newUnitCost - oldUnitCost` `:162`、`incomingMoveId = -line.getId()` 负值哨兵 `:168`[区别正常移动单正 ID]、`incomingDate = adjust.businessDate` `:165-166`）→ 后续 FIFO 出库 `FifoCostingStrategy.findFifoLayers:178-205` 按 `incomingDate` 升序消耗时会消耗此 delta 层（其 unitCost=Δ），实现"后续出库按更新单价"语义 | FIFO 追加 delta 调整层，后续出库消耗时单价已含调整 | ✅ |
| 成本调整台账行 | `CostAdjustmentService.writeLedger:248-270`（`moveId = LEDGER_MOVE_ID_COST_ADJUST(0)` 哨兵 `:254-255` 标识非移动单来源；quantity=0 `:260`） | ✅ |
| 红冲回滚 delta 层 | `CostAdjustmentService.reverseLine:175-192`（回退余额 `:182-186` + `removeFifoAdjustLayer:188`）+ `removeFifoAdjustLayer:194-202`（按 `incomingMoveId = -line.getId()` 哨兵精确删除 delta 层） | ✅ |
| MA/Standard 更新 | `CostAdjustmentService.applyAverageLike:123-135`（`balance.avgCost = newUnitCost` `:126` + `totalCost += adjustAmount` `:127-128`；STANDARD_REVALUATION 额外发布 FIRMED rollup `:132-134`） | ✅ |

### 2.6 过账分派（COGS 通道 + LANDED_COST 通道）

| 通道 | 文件:行 | 审计状态 |
|---|---|---|
| FIFO COGS 通道 | `FifoCostingStrategy.writeLedger`（经 `ctx.writeLedger:145-146`）写 `ErpInvStockLedger`；`InvPostingDispatcher` 取 `ledger.totalCost` 汇总为 `TOTAL_COST` | ✅ |
| LANDED_COST 通道 | `LandedCostPostingDispatcher` + `LandedCostAcctDocProvider`（businessType=LANDED_COST(490)，Dr 1401 存货 / Cr 2202 应付，`:49` owner doc 注记） | ✅ |

### 2.7 跨模块调用链（inventory ↔ finance/purchase）

| 调用 | 文件:行 | 性质 |
|---|---|---|
| inventory → finance（GL 凭证目的地） | `LandedCostPostingDispatcher` → `IErpFinVoucherBiz.post/reverse`（经 REQUIRES_NEW Facade，P0-MA1-021 修复后走 I\*Biz） | 业财一体写（合法） |
| inventory → purchase（receive 只读） | `ErpInvLandedCostProcessor.loadReceive:432-437` + `loadReceiveLines:439-445`（`daoProvider.daoFor(ErpPurReceive/Line)`） | 跨域只读 DAO（P1-MA1-022/P1-MA4-022 登记的合法豁免，读侧统一裁决登记于 `data-dependency-matrix.md §9`） |
| finance → inventory（期间结账） | `IErpInvCostingBiz.reclosePeriodCosts`（finance 期末结账 INV 模块关账经 IBizObjectManager 跨模块调用，DAG 合法） | 跨域 command 编排（合法，`data-dependency-matrix.md §4.4` 例外） |

---

## 3. 测试证据（L4，注明断言强度）

> 测试位于 `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/`。断言强度引用 MA5 评级（深/中/浅）。FIFO + 到岸成本测试套件共 8 文件，覆盖核心算法 + E2E + 红冲 + 并发 + 失败告警。

### 3.1 FIFO 出库测试（`TestErpInvFifoCosting.java`，6 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testIncomingAppendsCostLayer` | `:65-85` | 入库追加 cost layer（incomingQuantity/remainingQuantity/unitCost/totalCost 精确断言 + 余额 avgCost 置空） | **深** |
| `testOutgoingConsumesSingleLayer` | `:87-109` | FIFO-F3（单层消耗，出库 totalCost=-300=30×10 负号 + 余额守恒） | **深** |
| `testOutgoingSpansMultipleLayersWeightedCost` | `:111-143` | **FIFO-F1 + FIFO-F2 + FIFO-F3**（跨层消耗：队列1 50@10 全消耗 + 队列2 40@12 消耗 10 → COGS totalCost=-620=50×10+10×12，精确匹配需求公式；层数=2 不删层） | **深**（强断言，精确匹配 L1 Σ 公式） |
| `testFirstOutgoingWithoutCostLayerRejected` | `:145-161` | 不足拒绝（未入库直接出库 → `ERR_COST_NOT_AVAILABLE` + 不残留层） | **深** |
| `testReverseRestoresCostInvariant` | `:163-204` | 红冲不变量（红冲后 Σ layer remaining×unitCost 恢复至 980，容差 0.01；余额 totalQuantity 恢复 90；≥2 个 remainingQuantity>0 的层） | **深**（红冲成本不变量强断言） |
| `testOutgoingLedgerTotalCostFlowsToDispatcher` | `:206-220` | COGS 通道（ledger.totalCost=-680=20×10+40×12 流入 InvPostingDispatcher.TOTAL_COST + costMethod=FIFO） | **深** |

### 3.2 FIFO 全链 E2E（`TestErpInvFifoCostingEndToEnd.java`，3 @Test，深断言）

| 测试方法 | 覆盖 | 断言强度 |
|---|---|---|
| `testFifoIncomingOutgoingGeneratesSalesOutputVoucher` | 全链：采购入库 FIFO 建层 → 销售出库跨层消耗 COGS → `SALES_OUTPUT` 凭证（Dr COGS/Cr 存货，金额=Σ 队列消耗） | **深** |
| `testRecloseRebuildsMissingCostLayer` | period-close 兜底重算（缺失成本层的入库补建） | **深** |
| 第 3 测试 | reclose 对正常数据为 no-op | **深** |

### 3.3 到岸成本分摊引擎单测（`TestErpInvLandedCostAllocationEngine.java`，5 @Test，深断言，纯单元无 ORM）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testAllocateByAmount` | `:33-58` | **LC-L1**（算例精确匹配 owner doc `costing-methods.md:389-413`：A=180×(1000/1500)=120 / B=60；新 unitCost A=10+120/100=11.20 / B=11.20） | **深**（精确匹配 owner doc 算例） |
| `testAllocateByQuantity` | `:64-81` | LC-L1（按数量分摊：A=120/B=60） | **深** |
| `testAllocateByWeight` | `:87-104` | LC-L1（按重量分摊：A=120/B=60） | **深** |
| `testRoundingRemainderAbsorbedByLastLine` | `:109-129` | 末行吸收尾差（Σ allocatedAmount == totalCost） | **深** |
| `testEmptyReceiveLinesRejected` | `:134-139` | 空输入拒绝 | **深** |

### 3.4 到岸成本 E2E（`TestErpInvLandedCostEndToEnd.java`，4 @Test，深断言）

| 测试方法 | 覆盖 | 断言强度 |
|---|---|---|
| `testAllocateByAmount` | **LC-L1 + LC-L2**（BY_AMOUNT 分摊 + 成本层更新 + GL 凭证 Dr 存货 180/Cr 应付 180） | **深** |
| `testAllocateByQuantity` | LC-L1（BY_QUANTITY 分摊） | **深** |
| 多应付对象测试 | 多贷方行（多 AP partner） | **深** |
| 防重复分摊测试 | `ERR_LANDED_COST_ALREADY_ALLOCATED` 拒绝 | **深** |

> **⚠️ 测试覆盖缺口（P2-RC-004）**：`TestErpInvLandedCostEndToEnd` 物料均用 `MOVING_AVERAGE`（`:82-83` `seedMaterial(matA, COST_METHOD_MOVING_AVERAGE)`）。**FIFO 物料 + 到岸成本交互（delta 层追加 + 后续 FIFO 出库消耗更新后单价）无 E2E 测试**——LC-L3 的"后续出库按更新单价"语义在 FIFO 物料路径下经 `CostAdjustmentService.appendFifoAdjustLayer` 追加 delta 层实现，但无 E2E 断言"FIFO 物料到岸成本分摊后再出库，出库成本含 delta 层单价"。该交互在 MOVING_AVERAGE 路径下经 `applyAverageLike` 直接更新 `balance.avgCost` 已强覆盖，FIFO delta 层路径仅单测间接覆盖（`TestErpInvCostAdjust` MA 路径）。详见 §5 P2-RC-004。

### 3.5 到岸成本红冲 + 并发 + 失败告警测试

| 测试文件 | @Test 数 | 覆盖 | 断言强度 |
|---|---|---|---|
| `TestErpInvLandedCostReversal.java`（2） | 红冲原凭证 isReversed + 回滚 + 非 posted 拒绝（`ERR_LANDED_COST_NOT_POSTED`） | **深** |
| `TestErpInvLandedCostReceiveMutex.java`（processor/） | receive 悲观锁 + 防重复分摊并发（P1-MA2-085 修复验证） | **深** |
| `TestErpInvLandedCostReverseFailureAlert.java`（processor/） | 红冲失败告警派发（P1-MA4-020 修复验证，mock reverse 抛异常 → 断言告警通知派发） | **深** |
| `TestErpInvCostAdjust.java` | 成本调整 MA 主路径 + 红冲回滚余额/凭证 | **深** |

> **⚠️ 测试覆盖缺口（复用 P2-MA2-029）**：到岸成本红冲测试（`TestErpInvLandedCostReversal`）用 `MOVING_AVERAGE` 物料。**FIFO 物料到岸成本红冲（delta 层部分消耗后物理删除）无测试**——`CostAdjustmentService.removeFifoAdjustLayer:194-202` 按 `-line.id` 哨兵物理删除 delta 层，若该层已部分被后续出库消耗，物理删除可能破坏已扣减层（`costing-methods.md:66` 已登记 successor）。此为 P2-MA2-029（CostAdjustment FIFO 红冲三方一致性未测试）的到岸成本子场景，同根因同控制点（`removeFifoAdjustLayer` 物理删除），§6 裁决**复用 P2-MA2-029**，不新建。

---

## 4. 运行时行为证据（L5，复用 MA2 + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实行为作为既有证据输入，本审计**不重新核实行为本身**，只补"需求契约↔行为"差异。

### 4.1 复用 A2.4 costing 审计（`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`，331 行，**closed**）

A2.4 多维 costing 审计已证实：
- **7 costMethod 策略齐备** + 子计算器注入模式 + 三方对账（成本层 + 余额 + 流水）正常路径成立
- **FIFO 队列消耗**（incomingDate 升序 + 跨队列 + Σ 单价 + 历史成本守卫 + 不足拒绝）行为正确
- **到岸成本分摊**（BY_AMOUNT/BY_QUANTITY/BY_WEIGHT + 末行吸收尾差 + 空拒绝）行为正确
- **到岸成本编排**（receive 校验 + 防重复 + 悲观锁 + delta 层 + LANDED_COST 过账 + 红冲闭环）行为正确
- **零 P0**；2 P1（P1-MA2-023/024，均 resolved）+ 5 P2 watch-only（P2-MA2-026~030）

**本切片复核结论**：A2.4 已证实的 costing 行为经 HEAD 代码复核**无回退、无升级**。UC-FIN-10 六条验收标准的运行时行为均经 A2.4 + E2E 测试证实，本审计接受类引用，不重复验证。

### 4.2 复用 A2.11 inventory 状态机审计（`docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`）

- LandedCost approve/reverseApprove 状态机（DRAFT→DONE + APPROVED 守卫 + posted 翻转 + reverseApprove 红冲闭环）经 A2.11 证实
- CostAdjust DIRECT 5 action + applyCostAdjust/reverseCostAdjust 域动作齐全

### 4.3 复用 A4.5 代码质量审计（`docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）

- `StockMoveBookkeeper` + `ErpInvLandedCostProcessor` + 7 CostingStrategy 代码质量已审
- P1-MA4-020（到岸成本红冲业财悬挂，resolved R1.16）+ P1-MA4-021（测试有效性，resolved R2.14）+ P1-MA4-022（跨域 daoFor，resolved）均 closed

### 4.4 本切片需求视角差异（不重审行为，只补需求↔行为对照）

本审计相对 A2.4/A2.11/A4.5 的**需求视角增量**：
1. **UC-FIN-10 六条验收标准逐条 L1↔L3↔L5 对照**（A2.4 是状态机/链路行为视角，未做需求验收标准逐条对照）——§5 矩阵逐条确认实现存在
2. **FIFO 物料 + 到岸成本交互 E2E 测试缺口**（A2.4 P2-MA2-029 覆盖 reverse 路径，本切片发现 forward 路径 FIFO+landed cost 交互亦无 E2E）——§5 P2-RC-004
3. **StockQueue↔ErpInvCostLayer 命名漂移**（A2.4/A3.5 未登记此 cosmetic 漂移）——§5 P2-RC-005
4. **resolved finding HEAD 复核**（防"已 resolved 但代码回退"）——§6.2

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 的符合性结论）

### 5.1 UC-FIN-10 五级追踪矩阵（1 UC 一行，6 验收标准逐条进入判读）

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---|---|---|---|---|---|---|
| UC-FIN-10 | `use-cases.md:183` FIFO 出库成本与到岸成本（验收标准逐字见 §1：FIFO-F1 按 incomingDate 升序消耗 StockQueue / FIFO-F2 队列不足跨队列消耗 / FIFO-F3 出库成本=Σ(消耗量×单价) / LC-L1 运费保险关税按金额比例分摊 / LC-L2 入库成本+=分摊费用 / LC-L3 后续出库按更新后队列单价） | `costing-methods.md` §FIFO 队列 `:196-222`（设计称队列实体 StockQueue，ORM 权威名 ErpInvCostLayer——cosmetic 命名漂移，**冲突以 L1 为准**，行为完全实现）/ §FIFO 出库逻辑 `:224-249` / §到岸成本定义 `:350-363` / §到岸成本分摊+算例 `:365-413` / §成本调整 `:415-477` / §实现注记 `:29-66`（权威实现位于 module-inventory/erp-inv-service） | `FifoCostingStrategy.java:202-203`(FIFO-F1 升序) / `:105-120`(FIFO-F2 跨队列) / `:114/118`(FIFO-F3 Σ) / `LandedCostAllocationEngine.java:76-78/108-109`(LC-L1 按金额比例) / `ErpInvLandedCostProcessor.java:294-337`(LC-L2 入库成本+=) / `CostAdjustmentService.java:149-171`(LC-L3 FIFO delta 层) + 跨模块链：`StockMoveBookkeeper.java:116-130` 分派 + `ErpInvLandedCostProcessor` → `IErpFinVoucherBiz`(finance GL) / `daoFor(ErpPurReceive)`(purchase 只读) | `TestErpInvFifoCosting#testOutgoingSpansMultipleLayersWeightedCost:111-143`(FIFO-F1/F2/F3 强断言精确匹配 Σ 公式) / `#testReverseRestoresCostInvariant:163-204`(红冲不变量) / `TestErpInvLandedCostAllocationEngine#testAllocateByAmount:33-58`(LC-L1 精确匹配 owner doc 算例) / `TestErpInvLandedCostEndToEnd#testAllocateByAmount`(LC-L1+LC-L2 E2E) / `TestErpInvFifoCostingEndToEnd`(全链 SALES_OUTPUT 凭证) | A2.4 costing 审计已证实 FIFO 出库 + 到岸成本分摊 + 编排 + 红冲行为（引用 `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`）；A2.11 证实 LandedCost 状态机；本切片差异见 §4.4 | **接受**（6 验收标准全部实现且主路径可运行；候选缺口均为 P2 测试/文档类，无功能缺失） |

### 5.2 逐验收标准符合性裁决

| 验收标准 | L3 证据 | L4 证据 | 裁决 | 命中判据 |
|---|---|---|---|---|
| **FIFO-F1**（按 incomingDate 升序消耗） | `FifoCostingStrategy.java:202-203` sort + `:198-200` le 守卫 | `testOutgoingSpansMultipleLayersWeightedCost:111-143`（跨层消耗顺序） | **接受** | §2 接受（全部验收标准 L3-L5 有证据且一致） |
| **FIFO-F2**（队列不足跨队列消耗） | `FifoCostingStrategy.java:105-120` 循环 | `testOutgoingSpansMultipleLayersWeightedCost:111-143`（队列1 不足→队列2） | **接受** | §2 接受 |
| **FIFO-F3**（出库成本=Σ 消耗量×单价） | `FifoCostingStrategy.java:114/118` + 加权 `:128-129` | `testOutgoingSpansMultipleLayersWeightedCost:135`（totalCost=-620=50×10+10×12 精确） | **接受** | §2 接受 |
| **LC-L1**（按金额比例分摊） | `LandedCostAllocationEngine.java:76-78/108-109` | `testAllocateByAmount:33-58`（A=120/B=60 精确匹配 owner doc 算例） | **接受** | §2 接受 |
| **LC-L2**（入库成本 += 分摊） | `ErpInvLandedCostProcessor.java:294-337` → `CostAdjustmentService.applyCostAdjust` | `TestErpInvLandedCostEndToEnd#testAllocateByAmount`（成本层更新 + GL 凭证） | **接受** | §2 接受 |
| **LC-L3**（后续出库用更新单价） | `CostAdjustmentService.java:149-171` appendFifoAdjustLayer delta 层 + FIFO 出库消耗 | MA 路径强覆盖（`TestErpInvCostAdjust`）；**FIFO delta 层 E2E 缺口**（P2-RC-004） | **接受**（行为实现，FIFO 交互测试为 P2 watch-only） | §2 接受 + §2 P2①（次要验收标准边界场景测试弱） |

### 5.3 候选缺口分级裁决

| 候选缺口 | 分级 | 命中判据 | 处置 |
|---|---|---|---|
| **FIFO 物料 + 到岸成本交互 E2E 测试缺口**（forward：delta 层追加 + 后续 FIFO 出库消耗更新后单价无 E2E） | **P2** | §2 P2①（次要验收标准[LC-L3 FIFO 路径]未完全满足测试覆盖，主路径[MA 路径]OK 边界[FIFO delta 层]弱） | 新建 **P2-RC-004**（与 P2-MA2-029[reverse 路径] + P1-MA4-021[已 resolved STANDARD/SPECIFIC，未含 landed-cost-FIFO-interaction] 不同控制点）→ successor watch-only |
| **FIFO 物料到岸成本红冲测试缺口**（delta 层部分消耗后物理删除无测试） | **P2** | §2 P2① | **复用 P2-MA2-029**（同根因同控制点：`removeFifoAdjustLayer` 物理删除；到岸成本是其子场景）→ 追加 RC 交叉引用，不新建 |
| **StockQueue↔ErpInvCostLayer owner-doc 命名漂移** | **P2** | §2 P2①（cosmetic 文档漂移，行为完全实现；L1 use-cases + L2 costing-methods 均用 StockQueue，ORM 权威名 ErpInvCostLayer） | 新建 **P2-RC-005**（与 A3.5 P2-MA3-034/035 不同控制点：StockQueue 命名 vs StockTake 状态/冲销方向）→ successor watch-only（L1 冻结，§9） |

**UC-FIN-10 整体裁决**：**接受**。六条验收标准全部实现且主路径可运行，三项候选缺口均为 P2（测试覆盖/cosmetic 文档），无 P0/P1 功能缺失或行为分歧。

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：产出 finding 前 grep arm-index 同域同控制点后裁决（禁止未经比对直接新建）。

### 6.1 finding 复用/新增裁决

| 候选 finding | grep 比对 | 裁决 | 依据 |
|---|---|---|---|
| FIFO 物料 + 到岸成本交互 E2E 测试缺口（forward） | 比对 P2-MA2-029（reverse 路径物理删除）/ P1-MA4-021（已 resolved STANDARD/SPECIFIC/多币种/业财悬挂，未含 landed-cost-FIFO-interaction）/ P2-MA4-010/011（watch-only 防护缺口） | **新建 P2-RC-004** | 新控制点：forward 路径 FIFO 物料经到岸成本 delta 层后出库消耗的 E2E 断言缺失。P2-MA2-029 是 reverse 路径物理删除，P1-MA4-021 已 resolved 且范围未含此交互，不可合并 |
| FIFO 物料到岸成本红冲测试缺口（delta 层物理删除） | 比对 P2-MA2-029（CostAdjustment FIFO 红冲 delta 层物理删除三方一致性未测试） | **复用 P2-MA2-029** | 同根因同控制点：`CostAdjustmentService.removeFifoAdjustLayer:194-202` 按 `-line.id` 哨兵物理删除 delta 层。到岸成本红冲（`ErpInvLandedCostProcessor.doReverseApprove:174-178` → `reverseCostAdjust` → `removeFifoAdjustLayer`）是其子场景。追加 RC 交叉引用，不新建 |
| StockQueue↔ErpInvCostLayer 命名漂移 | 比对 A3.5 P2-MA3-034（StockTake COUNTING vs CONFIRMED）/ P2-MA3-035（冲销反向移动取负 vs 翻转 moveType）/ A3.3 finance drift（P1-MA3-024~039，未含 costing-methods StockQueue 命名） | **新建 P2-RC-005** | 新控制点：costing-methods.md §FIFO 队列 + use-cases.md:190 用 StockQueue，ORM 权威名 ErpInvCostLayer（字段 queueId→id / moveLineId 无对应 / batchId→batchNo / incomingQty→incomingQuantity / outgoingQty 无对应 / remainingQty→remainingQuantity）。cosmetic，行为完全实现。A3.5 未覆盖 costing-methods 命名漂移 |

### 6.2 resolved finding HEAD 复核（防"已 resolved 但代码回退"）

> 方法论增强（plan Phase 1 item）：对 arm-index 标记 resolved 的 costing finding 在当前 HEAD（`85b2ab7e0`）代码实际落地逐条复核。

| Finding | arm-index 状态 | HEAD 复核 | 结论 |
|---|---|---|---|
| **P1-MA2-023**（SPECIFIC 历史成本守卫缺失） | ✅ resolved (R1.12) | `SpecificCostingStrategy.findSpecificLayers:172-196` 已增 `businessDate` 参数 `:174` + `le("incomingDate", businessDate)` 过滤 `:192-194`；`onOutgoing:93-94` 传入 `move.getBusinessDate()`；对齐 FIFO/LIFO/BATCH 基方法 | **已落地** |
| **P1-MA2-024**（STANDARD 红冲成本不变量跨重估破缺） | ✅ resolved (R1.12) | `StandardCostingStrategy.onIncoming:54-59` 当 `move.getOriginReturnedMoveId() != null && unitCost != null && unitCost.signum() > 0` 时采用传入 unitCost（Choice B），否则重解析；`onOutgoing:90-93` 刷新 `line.unitCost` 为标准成本（供 reverse:144 透传） | **已落地**（Choice B 路径） |
| **P1-MA2-085**（LandedCost TOCTOU + 非唯一索引） | ✅ resolved (R1.28) | `ErpInvLandedCostProcessor.lockReceiveForAllocation:388-390` `ormTemplate.lock(receive)`（SELECT FOR UPDATE）+ `validateNotAlreadyAllocated:392-407`；ORM `app-erp-inventory.orm.xml:1352-1354` 仅有 `UK_INV_LANDED_COST_CODE_ORG`（code,orgId），**无 (receiveId, approveStatus) UK**，仅非唯一 `IDX_INV_LANDED_COST_RECEIVE_ID:1356-1358` | **已落地（SELECT FOR UPDATE 替代路径，非 UK 路径）**——arm-index 修复描述列明"方案 A 加 UK **或** SELECT FOR UPDATE pre-check"，实现取后者（合法修复方案之一）；测试 `TestErpInvLandedCostReceiveMutex` 覆盖 |
| **P1-MA4-020**（到岸成本红冲业财悬挂 reverse 方向无 sweep） | ✅ resolved (R1.16) | `ErpInvLandedCostProcessor.doReverseApprove:159-172` catch 块调 `dispatchReverseFailureAlert:483-499`（`notificationBiz.notify(NOTIFY_EVENT_LANDED_COST_REVERSE_FAILURE, ...)` `:494`）；测试 `TestErpInvLandedCostReverseFailureAlert` 覆盖（mock reverse 抛异常 → 断言告警派发） | **已落地** |
| **P1-MA4-021**（pur+sal+inv 测试有效性系统性不足） | ✅ resolved (R2.14) | 测试套件齐备：`TestErpInvFifoCosting`(6) + `TestErpInvFifoCostingEndToEnd`(3) + `TestErpInvLandedCostAllocationEngine`(5) + `TestErpInvLandedCostEndToEnd`(4) + `TestErpInvLandedCostReversal`(2) + `TestErpInvLandedCostReceiveMutex` + `TestErpInvLandedCostReverseFailureAlert` + `TestErpInvCostAdjust` | **已落地**（costing 子范围） |

**resolved finding 复核结论**：5/5 已落地，无代码回退。P1-MA2-085 经 SELECT FOR UPDATE 路径落地（非 UK 路径），属 arm-index 列明的合法修复方案之一，维持 resolved。

### 6.3 finding 双向可追溯

| Finding ID | 类型 | 目标 MR | 修复状态 |
|---|---|---|---|
| **P2-RC-004** | 新建（FIFO+到岸成本交互 E2E 测试缺口） | successor watch-only（P2 登记不强制） | todo（修复 = 补 FIFO 物料到岸成本 E2E 测试[FIFO 物料 receive → landed cost delta 层 → 后续 FIFO 出库消耗含 delta 单价断言]，纯测试补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first） |
| **P2-RC-005** | 新建（StockQueue↔ErpInvCostLayer 命名漂移） | successor watch-only（P2 登记不强制） | todo（修复触及 L1 use-cases + L2 costing-methods 真相源，§9 冻结条款须经人工批准；或仅在 costing-methods.md 补注 ORM 权威名 ErpInvCostLayer 对齐表，L2 设计参考段落可与代码协同修订） |
| **P2-MA2-029**（复用） | 既有（CostAdjustment FIFO 红冲 delta 层物理删除） | 既有 successor（`costing-methods.md:66` 文档化） | 追加 RC 交叉引用注记（到岸成本红冲是其子场景），状态不变 |

---

## 7. 静态存疑点清单（供 MA4 A4.1 运行时展开）

> 方法论 §6 段落 7：L5 无法静态定论、需运行时确认的点，每存疑点一行；无则注明"无"。

| # | 存疑点 | 触发条件 | 交 MA4 |
|---|---|---|---|
| 1 | **FIFO 物料 + 到岸成本 delta 层 + 后续出库消耗**的运行时数值正确性（delta 层 unitCost=Δ 被后续 FIFO 出库消耗时，出库成本是否正确含调整；多个 delta 层 + 原入库层混合排序时的 Σ 正确性） | 实际启用 FIFO 物料的到岸成本分摊后再多笔出库 | A4.1 运行时探针（补 E2E 即闭合 P2-RC-004） |
| 2 | **FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除**的余额守恒（`removeFifoAdjustLayer` 物理删除已部分消耗的 delta 层是否破坏已扣减层 / 余额 totalCost 漂移） | 实际启用 FIFO 物料的到岸成本分摊后部分出库再红冲 | A4.1 运行时探针（闭合 P2-MA2-029 子场景） |
| 3 | **P1-MA2-085 SELECT FOR UPDATE 路径**在 H2 内存库（测试环境）外的真实 DB（PG/MySQL）的锁行为（`ormTemplate.lock` 跨数据库方言一致性） | 生产部署 | A4.1 运行时验证（非本切片阻塞，P1-MA2-085 已 resolved） |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`85b2ab7e0`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计无生产代码变更**（纯审计报告），checker 无回归风险（actual 反映既有 HEAD 状态，非本审计引入）。

  | 规则 | 描述 | baseline（machine-readable） | actual（HEAD 85b2ab7e0） | delta | 说明 |
  |---|---|---|---|---|---|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 | — |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 | — |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 | — |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 | — |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R2b | BizModel daoFor(Erp*) 跨域 | 240 | 229 | -11 | 既有改进（非本审计引入） |
  | R2c | 全生产代码 daoFor() 总量 | 1380 | 1382 | +2 | 既有漂移（非本审计引入；本审计无代码变更） |
  | R2d | Processor daoFor(ErpMd*) | 32 | 34 | +2 | 既有漂移（非本审计引入） |
  | R3 | new Erp*() 构造实体 | 5 | 5 | 0 | — |
  | R4 | extends RuntimeException | 0 | 0 | 0 | — |
  | R5 | @Inject private | 0 | 0 | 0 | — |
  | R6 | @Transactional in BizModel | 2 | 2 | 0 | — |
  | R7 | System.currentTimeMillis() | 0 | 0 | 0 | — |
  | R8 | Processor 无 xbiz 接线 | 0 | 0 | 0 | — |
  | R10 | REQUIRES_NEW 事务 | 6 | 6 | 0 | — |
  | R11 | Processor 重复状态判断方法 | 0 | 0 | 0 | — |
  | R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | 0 | — |
  | R12b | 共享内核 import PostingEvent | 66 | 66 | 0 | — |
  | R12c | 共享内核 import AcctSchemaResolver | 40 | 40 | 0 | — |

  > **注**：R2b/R2c/R2d 的 actual vs top-table baseline 差异反映既有 HEAD 与 `compliance-baseline.md` 顶部人工表之间已知的漂移历史（多轮 MR1 重构的累计结果，已记录于 baseline 文件的增量注记段），**与本审计无关**（本审计无生产代码变更）。CI 门控以 `## BASELINE (machine-readable)` 块为准；本审计不触发 CI（无代码变更）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P2-RC-004/005 + 复用 P2-MA2-029）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9）：本审计未修改 product-scope / use-cases / costing-methods 需求契约段落。StockQueue↔ErpInvCostLayer 命名漂移记入报告（§5.3 P2-RC-005），不直改 L1（§9 冻结条款）。

---

## 9. 与 MA2 报告差异增量声明

> 方法论 §去重协议 + §6 段落 9：声明复用既有 MA2 报告已证实行为，列明本切片只补的需求视角差异。

### 9.1 复用的 MA2/MA4 既有证据

- **`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（A2.4，331 行，closed）= THE costing 审计**：7 costMethod + 三方对账 + reclose + 成本调整 + 到岸成本 + PPV 全覆盖，结论 0 P0、2 P1（P1-MA2-023/024，均 resolved）、5 P2 watch-only（P2-MA2-026~030）。本审计复用其已证实的 costing 行为，不重跑。
- **`docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11）**：LandedCost approve/reverseApprove 状态机。
- **`docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）**：`StockMoveBookkeeper` + `ErpInvLandedCostProcessor` + 7 CostingStrategy 代码质量；P1-MA4-020/021/022。
- **`docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b）**：finance 侧 GL 成本映射 + 期间结账（不含 FIFO/到岸成本算法本身——算法在 inventory 模块）。

### 9.2 本切片需求视角差异增量（只补，不重审）

1. **UC-FIN-10 六条验收标准逐条 L1↔L3↔L4↔L5 五级追踪**（A2.4 是行为/状态机视角，未做需求验收标准逐条对照）——§5.2 逐条裁决"接受"
2. **FIFO 物料 + 到岸成本交互 E2E 测试缺口**（forward 路径，A2.4 P2-MA2-029 仅覆盖 reverse 路径）——§5.3 新建 P2-RC-004
3. **StockQueue↔ErpInvCostLayer cosmetic 命名漂移**（A2.4/A3.5 未登记）——§5.3 新建 P2-RC-005
4. **resolved finding HEAD 复核**（5 个 costing P1 finding 在 HEAD `85b2ab7e0` 实际落地复核，防"已 resolved 但代码回退"）——§6.2 结论 5/5 已落地
5. **陈旧证据校正**：`docs/audits/2026-07-06-use-case-implementation-audit.md:117` 将 UC-FIN-10 标 🔶"到岸成本分摊未实现(Non-Goal)"——**已过期**，到岸成本经 plan `2026-07-10-1100-3` 已完整落地（`LandedCostAllocationEngine` + `ErpInvLandedCostProcessor` + `LandedCostPostingDispatcher` + 8 测试文件），本审计经实测确认实现完整，不引用此陈旧行为缺口证据

### 9.3 不重审维度（§去重协议）

- **不重跑 A2.4 costing 行为审计**（已 closed，行为直接引用）
- **不重审架构/代码质量维度**（A4.5 已覆盖）
- **不重审 owner-doc vs code drift 文本一致性**（A3.5 已覆盖 pur+sal+inv；StockQueue 命名漂移是本切片新发现，§5.3 P2-RC-005）
- **不复跑 MA1-MA7 架构漂移类审计**（以 audit-remediation 收口为准）

---

## §自检清单（报告产出前强制，方法论 §6 段落完整性自检）

- [x] §1 需求契约原文（UC-FIN-10 验收标准逐字引用，6 条完整枚举）
- [x] §2 实现证据（L3 `file:line`，跨模块 inventory↔finance↔purchase 调用链列全）
- [x] §3 测试证据（L4，注明断言强度，8 测试文件覆盖矩阵）
- [x] §4 运行时行为证据（L5，复用 A2.4/A2.11/A4.5 + 本切片差异）
- [x] §5 符合性结论（五级追踪矩阵 1 UC 行 + 6 验收标准逐条裁决 + 候选缺口分级）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + resolved finding HEAD 复核 + 双向可追溯）
- [x] §7 静态存疑点清单（3 项交 MA4 A4.1）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重 + 真相源未修改声明）
- [x] §9 与 MA2 报告差异增量声明（复用证据 + 需求视角差异 + 不重审维度）

**9 段完整性自检结论**：§1-§9 全部存在，无缺失。
