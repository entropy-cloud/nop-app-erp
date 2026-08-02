# 2026-07-30-0143-1-r1-12-inventory-cost-specific-standard-invariants 库存核算 SPECIFIC/STANDARD 成本不变量修复

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.12（P1-MA2-023 + P1-MA2-024，源自 A2.4 库存核算一致性审计 `docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`）
> Related: `docs/plans/2026-07-02-1538-1-inventory-costing-engine.md`（成本引擎奠基）、`docs/plans/2026-07-05-2352-3-inventory-cost-adjustment.md`（STANDARD_REVALUATION 落地）
> Audit: required

## Current Baseline

七种 costMethod 策略位于 `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/`，实现接口 `CostingStrategy`（`onIncoming(move,line,acctSchemaId,unitCost,ctx)` / `onOutgoing(move,line,acctSchemaId,ctx)`，`findXxxLayers` 为各策略私有方法，不在接口上）。

**P1-MA2-023（SPECIFIC 历史成本守卫缺失）— 确认：**
- `SpecificCostingStrategy.java:168-188` `findSpecificLayers(orgId,materialId,skuId,warehouseId,batchNo,serialNo,acctSchemaId)` **无 businessDate 参数**、无 `le(incomingDate, businessDate)` 过滤、无 incomingDate 排序；`le` 未 import（:24-25 仅 eq/gt）。
- 对照：FIFO `findFifoLayers:178-205`（:198-200 有 `le(incomingDate, businessDate)` + :202-204 升序）、LIFO `:185`、BATCH `:190` 均有此过滤，且调用处（FIFO:95-96 / LIFO:88 / BATCH:97）传 `move.getBusinessDate()`。
- SPECIFIC 调用处 `SpecificCostingStrategy.java:92-93` **`move.getBusinessDate()` 已可用**（同文件 `appendCostLayer:158` 已使用），仅未传入。
- 后果：同 batchNo 的 future-dated 入库成本层可能被今日出库消耗，违反历史成本原则。

**P1-MA2-024（STANDARD 红冲成本不变量跨重估破缺）— 确认，复合两处：**
- `StandardCostingStrategy.java:37-60` `onIncoming` 签名含 `unitCost` 参（:38）但**完全忽略**——:43 `standardCostResolver.resolve(line.getMaterialId())` 重解析当前 FIRMED rollup。
- `StandardCostingStrategy.java:62-87` `onOutgoing` **不刷新 `line.setUnitCost(...)`**（对照 FIFO:131 / LIFO:121 / BATCH:130 / SPECIFIC:126 均刷新）。
- `ErpInvStockMoveProcessor.java:144` `rl.setUnitCost(nz(ol.getUnitCost()))` 复制原出库行 unitCost 给反向入库行——对 STANDARD 该值是过期/未刷新的。
- `StockMoveBookkeeper.java:113-127` `bookCompletion`：INCOMING 分支传 `line.getUnitCost()` 给 `onIncoming`；INTERNAL_TRANSFER 传 `carriedCost=onOutgoing(...)`。
- `StandardCostResolver.resolve:57-71` 永远读**最新 businessDate 的 FIRMED** `ErpMfgCostRollup`（无 as-of-date 参）；`CostAdjustmentService.publishFirmedRollup:206-230` 经 STANDARD_REVALUATION 发布新 FIRMED rollup。
- 后果：红冲跨 STANDARD_REVALUATION 时反向入库用新标准成本，与原出库扣减的旧标准成本不一致，`balance.totalCost` 漂移 `(newStd-oldStd)×qty`。

**剩余差距 / 测试缺口：**
- 参考 fixture：`TestErpInvFifoCosting.testReverseRestoresCostInvariant:164-204`（红冲后 Σ layer×unitCost + balance.totalCost 恢复，容差 0.01）。
- `TestErpInvSpecificCosting`（252 行）无 future-dated 同 batchNo 测试；`TestErpInvStandardCosting`（408 行）无任何红冲/不变量测试。`TestErpInvCostAdjust` 覆盖 STANDARD_REVALUATION 发布 + reverse，但未覆盖「StockMove reverse 发生在 revaluation 改变 FIRMED rollup 之后」的交叉路径。

**关键未知（须先 Explore）：** 对一笔正常 STANDARD 采购入库（非红冲/转移），`StockMoveLine.unitCost` 持有的是「实际采购价」「标准成本」还是「0/null」。这决定 `onIncoming` 能否安全地「传入 unitCost>0 时优先用」（若正常入库持有实际价，则该规则会把标准成本计价误用为实际价，破坏标准成本法语义）。

**保护区域：** 本计划触及库存成本计算正确性（会计保护区域）。owner doc 存在（`docs/design/finance/costing-methods.md` + `docs/design/inventory/README.md`）。需 plan-audit + closure-audit + 行为测试证明。

## Goals

- SPECIFIC 出库永不消耗 businessDate 之后的入库成本层（历史成本原则成立）。
- STANDARD 红冲在跨 STANDARD_REVALUATION 场景下恢复 `balance.totalCost` 不变量（与 FIFO 红冲不变量对齐）。
- owner doc 明确 SPECIFIC 历史成本过滤契约与 STANDARD 红冲 `onOutgoing` 刷新 `line.unitCost` 的依赖。

## Non-Goals

- 不为 `StandardCostResolver` 增加 as-of-date 解析（更广架构变更，登记为 watch-only 残留）。
- 不重构 `CostAdjustmentService` 的 AP-01 `if/Objects.equals` 分派反模式（独立治理 finding）。
- 不迁移其他 costMethod；不改 `reverse` 的 `businessDate=CoreMetrics.today()` 语义。
- 不新增 SPECIFIC 的 `referencedMoveId` 精确匹配（owner doc 提到的 batchNo/serialNo 匹配维持现状，仅补日期过滤）。

## Task Route

- Type: `implementation-only change`（审计 finding 修复，契约已由 owner doc + 审计报告界定）
- Owner Docs: `docs/design/finance/costing-methods.md`（SPECIFIC §311-344 / STANDARD §68-74 / FIFO 红冲设计 §37）、`docs/design/inventory/README.md`
- Skill Selection Basis: 成本引擎为 service 层 Java（QueryBean 过滤 + 错误处理 + 单元测试），`nop-backend-dev` 提供 QueryBean/错误处理/反模式自检；测试阶段以 `TestErpInvFifoCosting` 红冲 fixture 为模板（`nop-testing`）。会计保护区域 → 强制 plan-audit + closure-audit。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。无新 config 键（两处修复均为无条件正确性补齐，不 gate）。

## Execution Plan

### Phase 1 - SPECIFIC 历史成本守卫（P1-MA2-023）

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/SpecificCostingStrategy.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: none

- [x] `findSpecificLayers` 增加 `java.time.LocalDate businessDate` 末尾参数；当 `businessDate != null` 时追加 `q.addFilter(le("incomingDate", businessDate))`（对齐 FIFO:198-200）；import `le`。
      - Skill: `nop-backend-dev`
- [x] 调用处 `SpecificCostingStrategy.java:92-93` 传入 `move.getBusinessDate()`。
      - Skill: `nop-backend-dev`
- [x] Proof（单元）：在 `TestErpInvSpecificCosting` 增加测试——同 batchNo 两笔入库（历史日 + future-dated），出库仅消耗历史层、future-dated 层 remainingQuantity 不变；断言出库 unitCost = 历史层成本。
      - Skill: `nop-testing`

Exit Criteria:

- [x] SPECIFIC 出库在存在 future-dated 同 batchNo 入库层时仅消耗 ≤ businessDate 的层（测试断言 future 层 remainingQuantity 不变 + 出库成本取历史层）。

### Phase 2 - STANDARD 红冲不变量（P1-MA2-024）

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/StandardCostingStrategy.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Fix | Proof`
- Prereqs: Phase 1 无依赖，但同一结果面（成本不变量），按序执行。

- [x] **Explore**：确认正常 STANDARD 采购入库路径下 `StockMoveLine.unitCost` 的实际取值（实际价 / 标准成本 / 0）。手段：读 `ErpInvStockMoveProcessor` 入库 complete 路径 + 现有 `TestErpInvStandardCosting` seed 数据 + 必要时加临时断点式测试。结果写入本计划 Decision 依据。
      - Skill: `none`
      - **Explore 结论**：正常 STANDARD 采购入库 `StockMoveLine.unitCost` 持**实际采购价**（非 0/null、非标准成本）。证据：`ErpInvStockMoveProcessor.newLines:297-298` 从 `StockMoveLineRequest.unitCost`（实际价，如 12）写入 line；`StockMoveBookkeeper.bookCompletion:123` 将此实际价作为 `unitCost` 传入 `onIncoming`；`InvPostingDispatcher.dispatchPurchasePriceVariance:125` 读 `line.getUnitCost()` 作为 actualUnitCost 与 `ledger.getUnitCost()`（标准成本）比对算 PPV。`TestErpInvStandardCosting.testPpvUnfavorableActualGreaterThanStandard` 佐证：入库 ledger.unitCost=10（标准），而请求传 unitCost=12（实际）。
- [x] **Decision**：STANDARD `onIncoming` 是否「传入 unitCost 非 null 且 > 0 时优先用、否则重解析」。
      - **裁决 = 选择 B**（Explore 证明正常入库 unitCost 持实际采购价，选择 A 会将实际价误用为标准成本，破坏标准成本计价 + PPV）。实现：`onIncoming` 当 `move.getOriginReturnedMoveId() != null`（冲销反向入库标记，`reverse:135` 设置）且传入 `unitCost > 0` 时采用传入值（原出库行经 `onOutgoing` 刷新 + `reverse:144` 透传的旧标准成本）；否则重解析当前标准成本。正常采购入库 / 内部调拨目的侧 `originReturnedMoveId=null` → 重解析标准（行为不变，PPV 不受影响）。残留风险（选择 B 边界）：仅当某冲销入库行意外携带非零 unitCost 但非原出库刷新值时误用——当前 reverse 路径 line.unitCost 唯一来源是 `reverse:144` 从原行复制，故风险为空。
      - Skill: `none`
- [x] `StandardCostingStrategy.onOutgoing:62-87` 增加 `line.setUnitCost(ErpInvConfigs.roundCost(standardUnitCost))` + `saveOrUpdateEntity(line)`（对齐 FIFO:131-132），供 reverse:144 透传原标准成本。
      - Skill: `nop-backend-dev`
- [x] 按 Phase 2 Decision 实现 `onIncoming` 的 unitCost 采用规则。
      - Skill: `nop-backend-dev`
- [x] Proof（单元）：在 `TestErpInvStandardCosting` 增加 `testReverseRestoresCostInvariantAcrossRevaluation`——出库（扣旧标准成本）→ 经 STANDARD_REVALUATION 发布新 FIRMED rollup（新标准成本）→ reverse；断言红冲后 `balance.totalCost` 恢复至出库前值（容差 0.01）+ Σ layer remaining×unitCost 恢复。镜像 `TestErpInvFifoCosting.testReverseRestoresCostInvariant:164-204`。
      - Skill: `nop-testing`
      - **Proof 落地**：`testReverseRestoresCostInvariantAcrossRevaluation`（materialId=2406）——入库 20@实际12（标准10）→ 出库 8@旧标准10（onOutgoing 刷新 line.unitCost=10）→ 发布新 FIRMED rollup=15（businessDate 晚于旧 rollup，模拟制造 re-rollup / STANDARD_REVALUATION）→ reverse。断言：反向入库 ledger.unitCost=旧标准 10（非新 15，直接证明 onIncoming 沿用透传旧标准）+ balance.totalCost 恢复至出库前 200（容差 0.01）+ qty 恢复 20。全 6 个 STANDARD 测试 + 全 124 个 inventory-service 测试通过。

Exit Criteria:

- [x] STANDARD 红冲跨 STANDARD_REVALUATION 后 `balance.totalCost` 恢复不变量（测试通过）；`onOutgoing` 已刷新 `line.unitCost`（代码核实）。

### Phase 3 - owner doc 成本不变量契约

Status: completed
Targets: `docs/design/finance/costing-methods.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1/2 决定的最终行为已落地。

- [x] costing-methods.md SPECIFIC 段补「历史成本过滤契约：出库仅消耗 `incomingDate <= businessDate` 的同 batchNo/serialNo 成本层（对齐 FIFO/LIFO/BATCH）」。
- [x] costing-methods.md STANDARD 段补「红冲不变量依赖 `onOutgoing` 刷新 `line.unitCost` 为标准成本、`onIncoming` 在传入 unitCost 有效时采用之（reverse 透传原标准成本），跨 STANDARD_REVALUATION 时红冲后 `balance.totalCost` 恢复」。

Exit Criteria:

- [x] owner doc 反映 SPECIFIC 历史成本过滤 + STANDARD 红冲不变量两处实际契约。

## Draft Review Record

- Independent draft review iteration 1: accept (this review session). Format compliance, completeness, scope, and closure evidence all pass. All Current Baseline technical claims verified against the live codebase: SpecificCostingStrategy.java (no businessDate param at :168-188, `le` not imported, call site :92-93 omits `move.getBusinessDate()`); StandardCostingStrategy.java (onIncoming :37-60 ignores unitCost, onOutgoing :62-87 does not refresh line.unitCost); ErpInvStockMoveProcessor.java:144 copies stale unitCost to reverse line; FifoCostingStrategy.java:131-132 is the correct reference pattern for line refresh; StandardCostResolver.resolve:57-71 reads latest FIRMED with no as-of-date. Phase 2 Explore+Decision before implementation is the correct handling of the genuine unitCost-semantics unknown (rule 9). Two findings share the same component/result surface (rule 14). No Blocker/Major issues found. Promoted to active.

## Closure Gates

- [x] 范围内行为完成（SPECIFIC 历史成本守卫 + STANDARD 红冲不变量）
- [x] 相关文档对齐（costing-methods.md）
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service`（聚焦成本测试）+ Closure 时 `mvn clean install -DskipTests` 全绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### StandardCostResolver as-of-date 解析

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划经 `onOutgoing` 刷新 + `onIncoming` 采用传入值在 reverse 路径修复不变量，无需改 resolver；resolver 永远读最新 FIRMED 是正向入库的正确行为。
- Successor Required: `yes`（若未来需要按业务日期回溯标准成本快照——如历史报表重述——再为 resolver 加 as-of-date 参）

## Closure

Status Note: 已通过独立子代理结束审计（PASS WITH NOTES，无 blocker）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 ses_050ba25eaffeBkVja4GPI4jMN2，未参与实现）
- Verdict: PASS WITH NOTES（全部退出标准达成，无 blocker / 无回归）
- Evidence:
  - Phase 1：`SpecificCostingStrategy.java` `findSpecificLayers` 增 `businessDate` 参 + `le(incomingDate, businessDate)` 过滤 + `le` import + 调用处传 `move.getBusinessDate()`（:93-94 / :172-174 / :192-194 / :26）。结束审计指出原 qty=8 测试为弱回归守卫（无过滤时按 PK 序历史层先消耗仍可通过），执行者据此强化为确定性拒绝变体（qty=12 > 历史层 10）：有过滤→ERR_COST_NOT_AVAILABLE；无过滤→消耗两层成功。`TestErpInvSpecificCosting.testOutgoingIgnoresFutureDatedSameBatchLayer` 现 5 测试全绿。
  - Phase 2：Explore 结论经核实成立（`ErpInvStockMoveProcessor.newLines:297-298` 写实际价、`InvPostingDispatcher.dispatchPurchasePriceVariance:124-125` 读 line.unitCost 为 actualUnitCost 算 PPV）→ Choice B 正确。`StandardCostingStrategy.onOutgoing` 刷 line.unitCost（:92-93，对齐 FIFO:131-132）+ `onIncoming` Choice B（:55-59，`originReturnedMoveId != null && unitCost > 0` 采用之）。`testReverseRestoresCostInvariantAcrossRevaluation` 为确定性回归守卫（缺任一修复即失败）。
  - Phase 3：`costing-methods.md` SPECIFIC 段（:318-320）+ STANDARD 段（:74）两处契约已落。
  - 交叉：`@Inject IDaoProvider` 经 `app-service.beans.xml:25-26` 解析；正常采购入库 / INTERNAL_TRANSFER 路径行为不变（`originReturnedMoveId=null` → 重解析）；无生成代码被手改。
  - 验证：全工作区 `mvn clean install -DskipTests` BUILD SUCCESS（154 reactor 模块）；`erp-inv-service` 124 测试全绿。

Follow-up:

- `StandardCostResolver as-of-date` 解析（见 Deferred，非阻塞 successor）。
