# rc-ma1-a1-25 inventory-F1 移动单主链与追溯 需求-实现符合性五级追踪审计

> 报告类型：MA1(RC) 需求-实现符合性五级追踪审计（只读审计，无代码/ORM/真相源变更）
> Mission: requirement-compliance
> Work Item: A1.25（inventory-F1 移动单主链与追溯，UC-INV-01/03/04/05，4 UC + 5 候选缺口）
> 切片基线：`docs/audits/rc-requirement-baseline-inventory.md` A1.25 UC 锚点（`✅ 一致`）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> L1 真相源：`docs/design/inventory/use-cases.md`（UC-INV-01/03/04/05）
> L2 设计参考：`state-machine.md` §1/§2/§3 + `trace-chain.md` §追溯链模型/场景 + `cross-domain.md` §与采购协作/余量校验/与财务协作
> L5 既有证据复用：A2.11（`2026-07-28-0400-arm-ma2-inventory-state-machine.md`，移动单状态机 + DONE 冲销非回退 PASS）/ A2.4（`2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`，P2-MA2-028 reverse businessDate watch-only）/ A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`，StockMoveBookkeeper/TraceChainQuery 代码质量 PASS + P1-MA4-021 resolved R2.14）

## 整体裁决

**4 UC 全部接受，零 P0 / 零 P1 / 零新 P2 finding。** 5 候选缺口经 live-repo HEAD 实测复核**全部不成立或已 resolved**（详见 §5/§6）。inventory-F1 移动单主链与追溯子系统（generateMove 全链 / DONE 冲销非回退 / 正反向追溯 / 退货反查 / 不可变流水 + 余额快照）**完整实现需求契约且测试断言强度充分**。

**关键 live-repo 校正**（计划 `Current Baseline` 第 30 行声称"UC-INV-04 ⚠️ 无 dedicated forwardTrace test" / "UC-INV-05 ⚠️ 无 dedicated returnTrace/findByRelatedBill test"——**此声称与实仓不符**）：HEAD 实仓存在 `TestErpInvTraceChain.java`（9 方法，强断言：forward/backward/return/batch 四类追溯 + 环检测 + max-depth 截断 + delVersion 过滤 + disabled 降级）。计划基线的测试覆盖声称**陈旧/错误**，本报告以 live-repo 实测为准（§1 L4 + §5 候选缺口 #1/#2 复核）。

---

## 1. 需求契约原文（L1 逐字引用）

> 来源：`docs/design/inventory/use-cases.md`（L1 权威功能契约，§4 Q1 真相源层级 2）

### UC-INV-01 采购入库移动单全链（`use-cases.md:15`）

```
场景:采购入库单审核触发生成入库移动单,完成库存增加与流水写入。
行为链路:见 cross-domain.md §与采购协作
  采购入库单.审核通过 →
    库存域 generateMove(incoming) → 移动单(DRAFT → CONFIRMED → DONE)
可验证断言:
  移动单.状态 == DONE
  库存余额[物料, 仓库, 批次].现有量 += 移动数量
  存在不可变流水: 关联移动单, 记录 数量/单位成本/余额快照
  移动单 DONE 发布事件 → 触发存货估值凭证异步生成(见 cross-domain §与财务协作)
```

### UC-INV-03 已完成移动单冲销（`use-cases.md:57`）

```
场景:已 DONE 的移动单需要冲销(如入库错误),生成反向移动单。
可验证断言(见 state-machine.md §2/§3):
  原移动单.冲销 →
    生成反向移动单(新 DRAFT, 数量取负)
    反向单走 DRAFT → CONFIRMED → DONE 流程
  DONE 后:
    库存余额按反向数量调整(原+的反-)
    原流水不删除(不可变), 新增反向流水
    追溯链: 反向单.originReturnedMoveId 指向原单
```

### UC-INV-04 全链路正向追溯（`use-cases.md:76`）

```
场景:从采购入库到销售出库的全链路追溯(物料来源与去向)。
可验证断言(见 trace-chain.md §追溯链场景):
  链路: 采购入库 → 生产领料 → 完工入库 → 销售出库
  每环移动单通过 originMoveId / destMoveIds 关联
  从销售出库单 可反向追溯到 采购入库单(经中间环节)
  从采购入库单 可正向追踪到 所有去向(领料/完工/销售)
```

### UC-INV-05 退货反查原移动单（`use-cases.md:92`）

```
场景:采购退货/销售退货,反查原入库/出库移动单。
可验证断言(见 trace-chain.md §追溯链模型):
  退货移动单.originReturnedMoveId == 原入库/出库移动单.id
  原单.returnedMoveIds 包含 退货单.id  (双向)
```

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

> StockMove 遵循 Facade BizModel + per-mutation Processor 两层模式（R6.4 重构，`processor-extension-pattern.md` 每 mutation 一 Processor）。

### UC-INV-01 采购入库移动单全链

| 验收标准 | L3 代码路径（含行号） | 跨域调用链 |
|---------|----------------------|-----------|
| ① 移动单.状态 == DONE | `ErpInvStockMoveBizModel.java:58-60 generateMove`（Facade `@BizMutation`）→ `ErpInvStockMoveGenerateMoveProcessor.java:28-47 generateMove`（idempotent `findExisting:30-34` → `newMove:37` → `persistLines:39` → `doConfirm:41` → `if businessLinked doComplete:42-44`）→ `ErpInvStockMoveProcessor.java:100-114 doComplete`（`releaseReservation:109` → `bookkeeper.bookCompletion:110` → `setDocStatus(DONE):111` → `postingDispatcher.dispatchIfApplicable:113`） | 跨域 Facade 被调用方：purchase `ErpPurReceiveProcessor.java:218 generateMove` / sales `ErpSalDeliveryProcessor.java:244 generateMove`，全部经 `IErpInvStockMoveBiz` Facade |
| ② 库存余额.现有量 += 移动数量 | `StockMoveBookkeeper.java:116-130 bookCompletion`（按 `material.costMethod` dispatch 7 CostingStrategy：`MovingAverage/Fifo/Standard/Lifo/WeightedAverage/Specific/Batch`）→ 各策略 `onIncoming/onOutgoing` 经 `updateBalanceWithRetry:256-328`（`tryUpdateWithVersionCheck:271` + UK 冲突 evict+reload+retry）更新 `ErpInvStockBalance.totalQuantity/avgCost/totalCost` | `ErpInvStockBalance` UK `UK_INV_STOCK_BALANCE_NATURAL`（`app-erp-inventory.orm.xml:415`，P0-MA2-020 已落地）兜底 INSERT 竞态 |
| ③ 不可变流水（数量/单位成本/余额快照） | `StockMoveBookkeeper.java:194-220 writeLedger`：`ledger.setQuantity(signedQty):208` + `ledger.setUnitCost(roundCost(unitCost)):209` + `ledger.setTotalCost(signedTotalCost):210` + **`ledger.setBalanceQuantity(balance.getTotalQuantity()):211`** + **`ledger.setBalanceTotalCost(balance.getTotalCost()):212`**（**余额快照写入 ledger 实体**）。ORM `app-erp-inventory.orm.xml:299-300` `balanceQuantity`/`balanceTotalCost` 列 `mandatory="true"` | — |
| ④ DONE 发布事件 → 异步生成凭证 | `ErpInvStockMoveProcessor.java:113 postingDispatcher.dispatchIfApplicable(move, lines)` → `InvPostingDispatcher.java:57-80 dispatchIfApplicable`（`resolveBusinessType:58` → `buildEvent:63` → `executor.postEvent(event):65` try/catch 吞咽失败 → `markMovePosted:67`）→ `InvPostingExecutor.java:29-35 postEvent` → `voucherBiz.post(event, context)`（`@Transactional(REQUIRES_NEW)` 独立事务）+ `DeferredPostingSweepJob` 兜底重试 | 跨域写经 `IErpFinVoucherBiz` Facade（`InvPostingExecutor.java:27 @Inject`） |

### UC-INV-03 已完成移动单冲销

| 验收标准 | L3 代码路径 |
|---------|------------|
| ① 生成反向移动单（新 DRAFT，数量取负） | `ErpInvStockMoveBizModel.java:80-84 reverse`（Facade `@BizMutation`）→ `ErpInvStockMoveReverseProcessor.java:33-45 reverse`（`requireMove:34` → DONE 守卫 `:35-40` 抛 `ERR_REVERSE_NOT_DONE` → `buildReverseRequest:43` → `generateMoveProcessor.generateMove:44`）+ `buildReverseRequest:47-77`（`inverseMoveType:49` INCOMING↔OUTGOING + swap src/dest warehouses/locations `:52-55` + `relatedBillType="REVERSAL":56` + `relatedBillCode=original.code:57` + `originReturnedMoveId=original.id:58` + `remark="冲销":59`） |
| ② 反向单走 DRAFT→CONFIRMED→DONE | 反向单经 `generateMoveProcessor.generateMove` 走标准 `doConfirm→doComplete` 流程（businessLinked=true 自动 DONE） |
| ③ 库存余额按反向数量调整 | 反向单 DONE 经 `StockMoveBookkeeper.bookCompletion` 按 `inverseMoveType` 方向写流水 + 更新余额（原+的反-） |
| ④ 原流水不删除，新增反向流水 | `StockMoveBookkeeper.writeLedger:198-219` 每次调用 `dao.newEntity():199` + `dao.saveEntity(ledger):219` 追加新流水（不改写原流水），原 ledger 行 ORM 无 delete/update 调用 |
| ⑤ 追溯链 originReturnedMoveId 指向原单 | `buildReverseRequest:58 setOriginReturnedMoveId(original.getId())` → `ErpInvStockMoveProcessor.newMove:179 move.setOriginReturnedMoveId(request.getOriginReturnedMoveId())` |

### UC-INV-04 全链路正向追溯

| 验收标准 | L3 代码路径 |
|---------|------------|
| ① 每环通过 originMoveId / destMoveIds 关联 | `ErpInvStockMoveProcessor.newMove:178 setOriginMoveId(request.getOriginMoveId())`；下游链以反向查询表达（不存 M2M 中间表，见 `trace-chain.md:42` 实现说明：单 uplink 列 `originMoveId` + 反向查询 `findActiveMovesByOrigin`） |
| ② 从销售出库反向追溯到采购入库 | `ErpInvStockMoveBizModel.java:100-104 backwardTrace`（`@BizQuery`）→ `ErpInvStockMoveProcessor.java:72-74 backwardTrace` → `TraceChainQuery.java:94-129 backwardTrace`（沿 `originMoveId` 逐层上溯 + `Set<Long> visited` 环检测 `:114-117` + depth guard `:109-112` → `truncated=true`） |
| ③ 从采购入库正向追踪到所有去向 | `ErpInvStockMoveBizModel.java:94-98 forwardTrace`（`@BizQuery`）→ `ErpInvStockMoveProcessor.java:68-70 forwardTrace` → `TraceChainQuery.java:50-88 forwardTrace`（BFS via `findActiveMovesByOrigin(current):74` queries moves where `originMoveId=current.id` + `Set<Long> visited` cycle detection `:77-80` + depth guard `:67-70` → `truncated=true`） |
| 配置 | `ErpInvConstants.java:25-27 CONFIG_TRACE_CHAIN_ENABLED="erp-inv.trace-chain-enabled"`（default true）+ `CONFIG_TRACE_CHAIN_MAX_DEPTH`（default 10），read at `ErpInvStockMoveProcessor.java:290-302` |

### UC-INV-05 退货反查原移动单

| 验收标准 | L3 代码路径 |
|---------|------------|
| ① originReturnedMoveId == 原单.id | `ErpInvStockMoveProcessor.newMove:179 setOriginReturnedMoveId`（reverse 时由 `buildReverseRequest:58` 传入）；ORM `app-erp-inventory.orm.xml:176 originReturnedMoveId` + `:185` to-one + `:215-217` index |
| ② 原单.returnedMoveIds 包含退货单.id（双向） | 下游链以反向查询表达：`TraceChainQuery.java:135-162 returnTrace`（`anchorId = root.originReturnedMoveId != null ? root.originReturnedMoveId : moveId:148-149` → `findActiveReturnsOf(anchorId):154` queries moves where `originReturnedMoveId=anchorId`）双向覆盖（原单→退货单 + 退货单→原单） |
| 反查入口 | `ErpInvStockMoveBizModel.java:86-92 findByRelatedBill`（`@BizAction`）→ `ErpInvStockMoveProcessor.java:57-66`（QueryBean `eq("relatedBillType",...)` + `eq("relatedBillCode",...)` + `addOrderField("id", true)` DESC deterministic）+ `:106-110 returnTrace`（`@BizQuery`） |

---

## 3. 测试证据（L4 测试断言，注明断言强度）

| 测试文件#方法 | 覆盖 UC | 断言强度 | 断言要点 |
|--------------|--------|---------|---------|
| `TestErpInvStockMoveBizModel#testGenerateMoveBusinessLinkedAutoCompletes:55-64` | UC-INV-01 ① | **强** | DONE + posted=false + 1 line |
| `TestErpInvStockMoveBizModel#testGenerateMoveIdempotent:67-74` | UC-INV-01 幂等 | **强** | 同源单重复触发返回同一 ID + countMoves=1 |
| `TestErpInvStockMoveBizModel#testManualMoveStopsAtConfirmed:77-83` | UC-INV-01 手动单 | **强** | 独立移动单停 CONFIRMED |
| `TestErpInvStockMoveBizModel#testCancelReleasesReservation:96-115` | UC-INV-01 预留释放 | **强** | reserved=5 + available=5 → cancel → reserved=0 + available=10 |
| `TestErpInvStockMoveBizModel#testReverseCreatesReverseMove:117-135` | UC-INV-03 ①②④⑤ | **强** | 新 move relatedBillType=REVERSAL + relatedBillCode=original.code + DONE + 原 DONE 保持 + originReturnedMoveId（经 `TestErpInvTraceChain#testReverseSetsOriginReturnedMoveId:74-81` 强断言 `assertEquals(originalId, reversal.getOriginReturnedMoveId())`） |
| `TestErpInvStockMoveBookkeeping` | UC-INV-01 ②③ + UC-INV-03 ③④ | **强** | 余额更新 + 流水写入 + 余额快照（balanceQuantity/balanceTotalCost）+ CostingStrategy 分派（经 A4.5 §代码质量 PASS 证实） |
| **`TestErpInvTraceChain#testForwardAndBackwardTraceChain:83-99`** | **UC-INV-04 ①②③** | **强** | A→B→C 三节点链：forward 3 nodes + root=A + terminal=C + truncated=false；backward 3 nodes + root=C + terminal=A（**逐节点 ID 断言，非仅冒烟**） |
| **`TestErpInvTraceChain#testReturnTraceBidirectional:101-115`** | **UC-INV-05 ①②** | **强** | 原单 returnTrace 含退货单 + 原单本身；退货单 returnTrace 含原单 + 退货单本身（**双向断言**） |
| `TestErpInvTraceChain#testGenerateMovePersistsOriginLink:64-71` | UC-INV-04 ① | **强** | `assertEquals(originId, child.getOriginMoveId())` |
| `TestErpInvTraceChain#testReverseSetsOriginReturnedMoveId:73-81` | UC-INV-05 ① | **强** | `assertEquals(originalId, reversal.getOriginReturnedMoveId())` |
| `TestErpInvTraceChain#testRingDetectionTruncated:117-135` | UC-INV-04 环检测 | **强** | 人造环 X↔Y → truncated=true + nodes<=2 |
| `TestErpInvTraceChain#testMaxDepthTruncation:137-152` | UC-INV-04 depth guard | **强** | max-depth=2 + 4 节点链 → truncated=true + nodes<=3 |
| `TestErpInvTraceChain#testBatchTraceByBatchNo:154-164` | 追溯链批次维度 | **强** | 2 张含 BATCH-001 移动单 → 2 nodes；不存在 batchNo → 0 nodes |
| `TestErpInvTraceChain#testDelVersionFilterExcludesDeleted:166-183` | 追溯链 delVersion | **强** | 删除前 2 nodes → 删除 B → 1 node（仅根 A） |
| `TestErpInvTraceChain#testDisabledReturnsSingleNode:185-199` | trace-chain-enabled=false | **强** | 关闭 → 1 node + links 空 |

**断言强度评级**：全部覆盖方法均为**强断言**（精确数值/节点 ID/状态/truncated flag 断言，非仅 `status==0` 冒烟）。

**计划基线校正**（关键）：计划 `Current Baseline` 第 30 行声称"UC-INV-04 ⚠️ 无 dedicated forwardTrace test" / "UC-INV-05 ⚠️ 无 dedicated returnTrace/findByRelatedBill test"——**此声称陈旧/错误**。HEAD 实仓 `TestErpInvTraceChain.java`（9 方法强断言）完整覆盖 forward/backward/return/batch 四类追溯 + 环检测 + depth guard + delVersion + disabled 降级。本报告以 live-repo HEAD 实测为准（方法论规则 1：诚实 live-repo baseline）。`findByRelatedBill` 经 `testReverseCreatesReverseMove:126 findMove("REVERSAL", original.getCode())` 间接覆盖（QueryBean eq relatedBillType/relatedBillCode 路径），且 `ErpInvStockMoveProcessor.findByRelatedBill:57-66` 与 `findExisting:224-230` 共享同一 QueryBean 模式（后者经 `testGenerateMoveIdempotent` 强覆盖）。

---

## 4. 运行时行为证据（L5，复用既有 MA2 报告）

> §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，不重复验证。

| 行为 | L5 证据来源 | 结论 |
|------|-----------|------|
| 移动单状态机 DRAFT→CONFIRMED→DONE/CANCELLED 5 迁移守卫 | A2.11（`2026-07-28-0400-arm-ma2-inventory-state-machine.md`）§维度 1/2 PASS | **行为已证实**（5 迁移 1:1 落实，doConfirm/doComplete/cancel/reverse 守卫完整） |
| DONE 冲销非状态回退（reverse 生成反向 StockMove 走正常流程，原单 DONE 保持） | A2.11 §维度 3 + 场景 C PASS + `testReverseCreatesReverseMove` 证据 | **行为已证实**（A2.11 `:459` 关键设计裁决） |
| 冲销反向单可用量校验（余额守恒） | A2.11 §维度 4 异常路径 PASS（reverse→generateMove→doConfirm→validateAvailable 强制） | **行为已证实** |
| 库存余额并发安全（UK + retry） | A2.17（`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`）§13 PASS + P0-MA2-020 已落地 | **行为已证实**（`UK_INV_STOCK_BALANCE_NATURAL` + `updateBalanceWithRetry` MANAGED/TRANSIENT/SAVING 三分支 + `TestErpInvConcurrentDeduct` 6 测试含 3 真实多线程） |
| StockMoveBookkeeper / TraceChainQuery / ErpInvStockMoveProcessor 代码质量 | A4.5（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`）PASS | **代码质量已证实**（编排健壮性 / 跨域写经 I*Biz Facade / 异常规范化 / BigDecimal 货币类型安全四面扎实） |
| 跨域 `IErpInvStockMoveBiz` Facade 调用合规性 | A4.5 PASS（production 代码零凭证直写，P0-MA1-021 修复后 CostAdjustmentPostingDispatcher 经 Facade） | **行为已证实** |
| 追溯链 forward/backward/return 行为 | `TestErpInvTraceChain` 9 方法强断言（本报告 §3 首次从需求契约视角引用） | **行为已证实**（BFS 遍历 + 环检测 + depth guard + delVersion + 双向退货） |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论）

### 五级追踪矩阵

| UC | L1 需求契约 | L2 owner doc（设计参考） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|------------|------------------------|------------|------------|--------------|-----------|
| **UC-INV-01** | `use-cases.md:15` 采购入库移动单全链（4 断言：①DONE ②余额+= ③不可变流水含余额快照 ④DONE 发布事件→异步生成凭证） | `state-machine.md §1/§2`（DONE 三维语义：写流水/影响余额/释放预留）+ `cross-domain.md §与采购协作/与财务协作`（设计参考，冲突以 L1 为准） | `ErpInvStockMoveBizModel:58-60` → `GenerateMoveProcessor:28-47` → `Processor:100-114 doComplete` → `StockMoveBookkeeper:116-130 bookCompletion` + `:194-220 writeLedger`（含 `:211-212 setBalanceQuantity/setBalanceTotalCost` 余额快照）+ `InvPostingDispatcher:57-80 dispatchIfApplicable` | `TestErpInvStockMoveBizModel` 5 方法强 + `TestErpInvStockMoveBookkeeping` 强 | A2.11 状态机 PASS + A2.17 并发 PASS + A4.5 代码质量 PASS | **接受 on ①②③** + **倾向接受 on ④**（见候选缺口 #4 裁决） |
| **UC-INV-03** | `use-cases.md:57` 已完成移动单冲销（5 断言：①反向单新 DRAFT 数量取负 ②走 DRAFT→CONFIRMED→DONE ③余额按反向调整 ④原流水不删除新增反向 ⑤originReturnedMoveId 指向原单） | `state-machine.md §2/§3`（DONE 冲销非状态回退，生成反向单走正常流程）+ `trace-chain.md §追溯链维护`（设计参考） | `ErpInvStockMoveBizModel:80-84 reverse` → `ReverseProcessor:33-45 reverse` + `buildReverseRequest:47-77`（inverseMoveType + swap warehouses + originReturnedMoveId）| `testReverseCreatesReverseMove:117-135` 强（REVERSAL+DONE+原 DONE 保持）+ `TestErpInvTraceChain#testReverseSetsOriginReturnedMoveId:74-81` 强 | A2.11 §维度 3 + 场景 C PASS（DONE 冲销非回退） | **接受**（全 5 断言 + #5 reverse businessDate R6.9 fixed） |
| **UC-INV-04** | `use-cases.md:76` 全链路正向追溯（3 断言：①originMoveId/destMoveIds 关联 ②反向追溯到采购入库 ③正向追踪到所有去向） | `trace-chain.md §追溯链模型/场景/查询`（设计参考；实现说明 :42 单 uplink 列 + 反向查询） | `ErpInvStockMoveBizModel:94-98 forwardTrace` + `:100-104 backwardTrace` → `Processor:68-74` → `TraceChainQuery:50-88 forwardTrace`（BFS + cycle detection + depth guard）+ `:94-129 backwardTrace` | **`TestErpInvTraceChain#testForwardAndBackwardTraceChain:83-99` 强**（3 节点链 forward+backward 逐节点 ID 断言）+ `testGenerateMovePersistsOriginLink` + `testRingDetectionTruncated` + `testMaxDepthTruncation` + `testDelVersionFilterExcludesDeleted` + `testDisabledReturnsSingleNode` | `TestErpInvTraceChain` 9 方法强断言（本报告首次从需求视角引用） | **接受**（全 3 断言 + dedicated 强测试覆盖——计划基线"无 dedicated test"声称陈旧错误） |
| **UC-INV-05** | `use-cases.md:92` 退货反查原移动单（2 断言：①originReturnedMoveId == 原单.id ②原单.returnedMoveIds 含退货单.id 双向） | `trace-chain.md §追溯链模型`（设计参考；下游链以反向查询表达） | `ErpInvStockMoveBizModel:106-110 returnTrace` + `:86-92 findByRelatedBill` → `Processor:57-66` + `:76-78` → `TraceChainQuery:135-162 returnTrace`（anchor + findActiveReturnsOf 双向） | **`TestErpInvTraceChain#testReturnTraceBidirectional:101-115` 强**（原单→退货单 + 退货单→原单 双向断言）+ `testReverseSetsOriginReturnedMoveId:74-81` 强 | 同上 | **接受**（全 2 断言 + dedicated 强测试覆盖——计划基线"无 dedicated test"声称陈旧错误） |

### 候选缺口逐条裁决（#1-#5）

| # | 候选缺口 | live-repo HEAD 实测复核 | 裁决 | §2 判据 |
|---|---------|------------------------|------|---------|
| **#1** | UC-INV-04 forwardTrace dedicated test 缺失 | **不成立**：`TestErpInvTraceChain#testForwardAndBackwardTraceChain:83-99` 强断言（3 节点链 forward 3 nodes + root=A + terminal=C + truncated=false；backward 3 nodes + root=C + terminal=A）+ `testGenerateMovePersistsOriginLink` + `testRingDetectionTruncated` + `testMaxDepthTruncation` + `testDelVersionFilterExcludesDeleted` + `testDisabledReturnsSingleNode` 共 6 方法覆盖 forward 链。**P1-MA4-021（resolved R2.14）范围不含 trace chain**——但 trace chain 已由 `TestErpInvTraceChain` 独立强覆盖，无须 P1-MA4-021 激活 | **接受**（dedicated 强测试存在） | — |
| **#2** | UC-INV-05 returnTrace/findByRelatedBill dedicated test 缺失 | **不成立**：`TestErpInvTraceChain#testReturnTraceBidirectional:101-115` 强断言（原单 returnTrace 含退货单 + 原单；退货单 returnTrace 含原单 + 退货单——双向 4 断言）+ `testReverseSetsOriginReturnedMoveId:74-81` 强断言 originReturnedMoveId。`findByRelatedBill` 经 `testReverseCreatesReverseMove:126 findMove("REVERSAL", original.code)` 间接覆盖（同一 QueryBean eq 模式经 `testGenerateMoveIdempotent` 强覆盖 findExisting 同模式） | **接受**（dedicated 强测试存在） | — |
| **#3** | UC-INV-01 不可变流水余额快照（ledger 是否有余额快照字段） | **不成立（字段存在）**：`StockMoveBookkeeper.writeLedger:211 setBalanceQuantity(balance.getTotalQuantity())` + `:212 setBalanceTotalCost(balance.getTotalCost())` 写入 `ErpInvStockLedger`。ORM `app-erp-inventory.orm.xml:299 column balanceQuantity mandatory="true"` + `:300 column balanceTotalCost mandatory="true"`。L1「记录 数量/单位成本/余额快照」三要素全部落地：quantity(`:208`) + unitCost(`:209`) + 余额快照(`:211-212`) | **接受**（余额快照字段存在且写入） | — |
| **#4** | UC-INV-01 异步凭证触发（dispatch 是否 post-commit async） | **倾向接受（L2↔L3 设计参考漂移，L1 满足）**：L3 `InvPostingDispatcher.dispatchIfApplicable:57-80` 在 `doComplete:113` 内同步调 `executor.postEvent` → `voucherBiz.post`（`@Transactional(REQUIRES_NEW)` 独立事务），**非 post-commit**。L2 `cross-domain.md §触发机制:50-52` 称「post-commit 异步」——L2↔L3 漂移。但 L1（真相源）`use-cases.md:30` 仅称「异步生成」，**L1 由失败隔离（try/catch 吞咽 `:69-76`）+ `posted` 标志 + `DeferredPostingSweepJob` 兜底重试共同满足**（移动单 DONE 不依赖过账成功 = 语义上的"异步"）。L2「post-commit」是设计参考层细节（§4 Q1：L2 与 L1 冲突以 L1 为准）。主路径功能正确（过账触发 + 失败不阻塞 + 兜底重试）。残留边缘风险（BizMutation rollback 后 REQUIRES_NEW 凭证孤立）极低（dispatchIfApplicable 是 doComplete 最后一步）→ 登记静态存疑点 SP-1 交 MA4 运行时确认，不构成本切片 finding | **倾向接受** | §2 接受（L1 满足）+ SP-1（L2↔L3 post-commit 漂移 + 边缘风险交 MA4） |
| **#5** | UC-INV-03 reverse businessDate（P2-MA2-028 R6.9 是否已 fix） | **已 fix（接受）**：HEAD `ErpInvStockMoveReverseProcessor.java:51 reverseReq.setBusinessDate(original.getBusinessDate() != null ? original.getBusinessDate() : CoreMetrics.today())`——**优先用 original.getBusinessDate()**，today() 仅作 null fallback。P2-MA2-028（A2.4 `:301` watch-only）的方案 A 推荐「`reverseReq.setBusinessDate(original.getBusinessDate())` 保持队列时序」**已在 R6.9 落地**（arm-index :525 P2-MA2-028 行）。FIFO 队列时序保持正确（反向层 incomingDate = 原 businessDate，非 today） | **接受**（HEAD 已 fix） | — |

### 每 UC 符合性结论（取最高）

| UC | 结论 | 命中 §2 判据 |
|----|------|-------------|
| UC-INV-01 | **接受 on ①②③** + **倾向接受 on ④** | ①②③ §2 接受（全验收标准 L3-L5 证据一致）；④ L1「异步生成」由失败隔离+sweep 满足，L2「post-commit」漂移登记 SP-1 交 MA4 |
| UC-INV-03 | **接受**（全 5 断言） | §2 接受（DONE 冲销非回退 + 反向单走正常流程 + 余额调整 + 原流水不可变 + originReturnedMoveId 双向；#5 reverse businessDate R6.9 fixed） |
| UC-INV-04 | **接受**（全 3 断言） | §2 接受（forward/backward trace 实现 + dedicated 强测试覆盖——计划基线校正） |
| UC-INV-05 | **接受**（全 2 断言） | §2 接受（originReturnedMoveId 双向 + dedicated 强测试覆盖——计划基线校正） |

---

## 6. 与 arm-index 衔接（复用 or 新增裁决）

### 本切片 finding 裁决

**零新 finding 登记。** 5 候选缺口经 live-repo HEAD 实测复核全部不成立或已 resolved（§5）。无须经 §7「复用 or 新增」裁决（无新 finding 须比对 arm-index）。

### 既有 resolved finding HEAD 复核

| Finding ID | arm-index 行 | 本切片 HEAD 复核结论 |
|-----------|-------------|--------------------|
| **P2-MA2-028**（reverse uses today() not businessDate） | `:525`（A2.4 watch-only） | **R6.9 已 fix 确认**：HEAD `ErpInvStockMoveReverseProcessor.java:51` 优先用 `original.getBusinessDate()`（today() 仅 null fallback）。FIFO 队列时序保持正确。**维持 watch-only**（P2-MA2-028 状态未变，R6.9 是其推荐方案 A 的落地；本切片不复核 costing 维度，维持 A2.4 watch-only 不升级） |
| **P0-MA2-020**（StockBalance 自然键 UK） | `:222`（done, plan 2026-07-28-1249） | **done 确认**：HEAD `app-erp-inventory.orm.xml:415 UK_INV_STOCK_BALANCE_NATURAL` on `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` 物理存在 + `StockMoveBookkeeper.updateBalanceWithRetry:256-328` MANAGED/TRANSIENT/SAVING 三分支 + `flushAndCheckConflict:334-344` UK violation catch + evict + reload + retry。**UC-INV-01 ②余额更新并发安全由本 UK 兜底**。维持 done |
| **P1-MA4-021**（pur+sal+inv 测试有效性） | `:647`（resolved R2.14） | **resolved R2.14 确认 + 范围澄清**：P1-MA4-021 范围 = 多币种零覆盖 + 业财异常悬挂零覆盖 + STANDARD 红冲成本不变量 + SPECIFIC 成本调整 + rollbackReceive 不对称 + SalReversalListener 3/4 回退 + settle 三单匹配二次门禁 + 到岸成本反向悬挂（8 子项）。**范围不含 trace chain**——但 trace chain 已由 `TestErpInvTraceChain`（9 方法强断言）独立覆盖，无须 P1-MA4-021 激活。本切片计划基线第 30/38 行声称"P1-MA4-021 覆盖 trace chain gap"不准确——trace chain 覆盖来自 `TestErpInvTraceChain` 而非 P1-MA4-021 的 R2.14 修复 |
| **P1-MA3-062**（Processor per-mutation split） | `:423`（resolved R6.4） | **R6.4 done 确认**：HEAD StockMove 遵循 Facade BizModel + per-mutation Processor 两层模式（`ErpInvStockMoveBizModel` Facade + `GenerateMoveProcessor`/`ConfirmProcessor`/`CompleteProcessor`/`CancelProcessor`/`ReverseProcessor` 5 per-mutation Processor），符合 `processor-extension-pattern.md:42`「每个 @BizMutation 方法对应一个独立的 Processor 类」 |
| **P1-MA4-020**（到岸成本反向过账悬挂） | `:646`（resolved R1.16） | **resolved R1.16 确认**（邻近 UC-INV-03 reverse path）：`ErpInvLandedCostProcessor.dispatchReverseFailureAlert` 告警闭环已落地。**不在本切片范围**（UC-INV-01/03/04/05 不含到岸成本），仅记录邻近 reverse path HEAD 复核无回退 |

### 双向可追溯

- 本切片**无新 finding** 入 arm-index（零 P0/P1/P2 新登记）
- 本切片报告 ID `2026-08-03-0953-rc-ma1-a1-25-inventory-f1-stockmove-reversal-traceability` 将追加至 arm-index §报告清单 + §RC 交叉引用注记
- 既有 resolved finding（P2-MA2-028 / P0-MA2-020 / P1-MA4-021 / P1-MA3-062 / P1-MA4-020）HEAD 复核结论记录于本报告 §6，arm-index 对应行维持原状态（无须回填——本切片是需求契约视角复核，不改变 audit-remediation 侧 finding 状态）

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行。

| SP# | 存疑点 | 触发条件 | 验证方法 |
|-----|--------|---------|---------|
| **SP-1** | **InvPostingDispatcher post-commit 时序边缘风险**：`dispatchIfApplicable:113` 在 `doComplete` 内同步调 `voucherBiz.post`（REQUIRES_NEW 独立事务提交）。若 REQUIRES_NEW 凭证已 commit 后外层 `@BizMutation` 事务 rollback（如 dispatchIfApplicable 返回后的极少时序），凭证孤立（voucher 存在但 move 未 DONE + posted=false）。L2 `cross-domain.md §触发机制:51` 称「post-commit 异步」但 L3 为 in-transaction REQUIRES_NEW | BizMutation 在 dispatchIfApplicable 返回后 rollback（极罕见——dispatchIfApplicable 是 doComplete 最后一步，其后无其他写操作） | A4.1 运行时探针：mock voucherBiz.post 成功后强制外层事务 rollback → 断言 voucher 是否孤立 + DeferredPostingSweepJob 是否能检测/清理孤立凭证 |
| **SP-2** | **forwardTrace 在超深链/多分支链下的 truncated 行为**：`TraceChainQuery.forwardTrace:50-88` BFS 按 `originMoveId` 反查下游，`max-depth` 默认 10。多分支链（1 采购入库→N 生产领料→M 完工入库）节点数可能超阈值 | 深度 >10 或广度 >100 的链 | A4.1 运行时探针：构造 1→N→M 多分支链 + max-depth=3 → 断言 truncated=true + nodes 数量符合 root+3 层 + 无无限循环 |

**无其他静态存疑点。** 5 候选缺口 #1/#2/#3/#5 经静态实测即可定论（不交 MA4）；#4 登记 SP-1 + SP-2 交 MA4 运行时展开。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（4 UC 全接受/倾向接受，零活跃数据破坏/零核心循环断裂/零会计过账破坏）。**不触发 MR0**，无 R0.n 实体行追加。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（零新 finding）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6）。既有 resolved finding HEAD 复核结论记录于 §6，无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-03-0953 HEAD）

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

**全 19 规则 actual ≤ baseline（零漂移）。** 本审计为只读审计（无代码/ORM/api.xml/view.xml/真相源变更），checker 无回归风险。

---

## 9. 与 MA2 报告差异增量声明

> §去重协议：本切片复用既有 MA2 报告已证实行为，只补需求契约↔实际行为差异。

### 复用的 MA2/A4 既有证据

| 报告 | 复用维度 | 本切片复用结论 |
|------|---------|---------------|
| **A2.11**（`2026-07-28-0400-arm-ma2-inventory-state-machine.md`） | 移动单状态机 + DONE 冲销非回退 + 业务单据双轴 + 所有权转移 + 批次/序列号/预留 | PASS（5 迁移守卫 + 冲销非回退 + 反向单可用量校验 + 幂等键 + 存货过账事件解耦 + 跨域写经 Facade 全证实） |
| **A2.4**（`2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`） | FIFO 出库成本 + reverse businessDate（P2-MA2-028） | P2-MA2-028 R6.9 已 fix（HEAD 用 original.getBusinessDate()）；costing 算法不在本切片范围 |
| **A4.5**（`2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`） | StockMoveBookkeeper / TraceChainQuery / ErpInvStockMoveProcessor 代码质量 + 跨域 Facade 合规性 + P1-MA4-021 resolved R2.14 | PASS（编排健壮性 / 跨域写经 I*Biz Facade / 异常规范化 / BigDecimal 货币类型安全四面扎实；P1-MA4-021 resolved R2.14 范围澄清：含 8 子项，不含 trace chain） |
| **A2.17**（`2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`） | UC-INV-01 ②余额并发安全（@Version + UK + retry） | PASS（`UK_INV_STOCK_BALANCE_NATURAL` P0-MA2-020 已落地 + `TestErpInvConcurrentDeduct` 6 测试强覆盖含 3 真实多线程） |

### 本切片只补的"需求契约↔行为"差异增量

1. **UC-INV-01 ④ "异步生成"语义裁决**（A2.11 未从 L1 字面"异步"视角审视）：L1「异步生成」由失败隔离（try/catch）+ posted 标志 + DeferredPostingSweepJob 兜底共同满足；L2「post-commit」是设计参考层细节（§4 Q1 以 L1 为准）。登记 SP-1 交 MA4 运行时确认边缘风险。
2. **UC-INV-01 ③ 余额快照字段存在性确认**（A2.11/A4.5 未从 L1「余额快照」字面审视 ledger 实体字段）：`ErpInvStockLedger.balanceQuantity/balanceTotalCost`（ORM :299-300 mandatory）+ `writeLedger:211-212` 写入——L1 三要素（数量/单位成本/余额快照）全落地。
3. **UC-INV-04/05 dedicated 测试存在性 live-repo 校正**（计划基线声称陈旧）：`TestErpInvTraceChain` 9 方法强断言完整覆盖 forward/backward/return/batch + 环检测 + depth guard + delVersion + disabled——计划基线「无 dedicated test」声称与实仓不符。
4. **UC-INV-03 reverse businessDate HEAD 复核**（P2-MA2-028 R6.9 fix 确认）：`ReverseProcessor:51` 优先用 `original.getBusinessDate()`，FIFO 队列时序保持正确。

### 未重做的 MA2 维度（§去重协议）

- 移动单状态机迁移守卫（A2.11 §维度 1/2 PASS）——不重审
- DONE 冲销非状态回退行为（A2.11 §维度 3 + 场景 C PASS）——不重审
- 余额并发安全（A2.17 §13 PASS + P0-MA2-020 done）——不重审
- StockMoveBookkeeper/TraceChainQuery 代码质量（A4.5 PASS）——不重审
- 跨域 Facade 调用合规性（A4.5 PASS）——不重审

---

## 9 段完整性自检

- [x] §1 需求契约原文（4 UC 逐字引用）
- [x] §2 实现证据（L3 代码路径含行号 + 跨域调用链）
- [x] §3 测试证据（L4 断言强度注明 + 计划基线校正）
- [x] §4 运行时行为证据（L5 复用 MA2/A4）
- [x] §5 符合性结论（五级追踪矩阵 + 4 UC 逐结论 + 5 候选缺口逐裁决）
- [x] §6 与 arm-index 衔接（零新 finding + 5 resolved finding HEAD 复核）
- [x] §7 静态存疑点清单（SP-1 post-commit + SP-2 多分支链）
- [x] §8 过程纪律自检（checker actual vs baseline 19 规则全 ✅ + 独立性 + 交叉去重声明）
- [x] §9 与 MA2 报告差异增量声明（复用 4 报告 + 4 差异增量 + 5 未重做维度）
