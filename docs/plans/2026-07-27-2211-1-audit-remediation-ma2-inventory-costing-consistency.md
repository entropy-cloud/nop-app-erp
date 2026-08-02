# 2026-07-27-2211-1-audit-remediation-ma2-inventory-costing-consistency MA2 库存核算一致性审计（A2.4）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.4 库存核算一致性（成本+余额+流水三方对账）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.4）
> Related: `docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（A2.3 期末结账，其步骤2 存货成本核算 `IErpInvCostingBiz.reclosePeriodCosts` 的成本算法正确性交接 A2.4）；`docs/plans/2026-07-27-1430-1-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`（P0-MA1-021 已修复 `CostAdjustmentPostingDispatcher.reverse` 走 `IErpFinVoucherBiz.reverse`，本审计复核运行时正确性）；`docs/plans/2026-07-27-1227-3-audit-remediation-ma1-platform-conformance-a-tier-core.md`（P1-MA1-022 跨域只读 `daoFor(ErpMd*)` / `daoFor(ErpPurReceive)` 含 costing 侧 `StandardCostResolver`/`CostMethodResolver`/`CostAdjustmentService`/`ErpInvLandedCostProcessor`，待 MR1）；`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（P1-MA2-002 多币种凭证路径影响 PURCHASE_INPUT 存货估价）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）；`docs/design/finance/costing-methods.md`（成本核算权威 + §子计算器注入模式）；`docs/design/inventory/`（README + state-machine.md + trace-chain.md）
> Audit: required

## Current Baseline

库存核算是 ERP 业财一体的核算核心：移动单 DONE 时按物料 `costMethod` 分派到 7 种 `CostingStrategy`，每条流水（`ErpInvStockLedger`）写入 `unitCost/totalCost`，既有 `InvPostingDispatcher` 读 `ledger.totalCost` 汇总为 `TOTAL_COST` 过账（COGS 通道零改动拾取）。owner doc `docs/design/finance/costing-methods.md`（768+ 行）定义 5 种方法表（MOVING_AVERAGE/FIFO/BATCH/STANDARD/INDIVIDUAL）+ 实现注记扩展至 7 种（+ WEIGHTED_AVERAGE 全月一次、LIFO）+ 子计算器注入模式（§D3 四要素：Strategy/注入器/Resolver/Context）+ 到岸成本 + 成本调整 + PPV + 期末成本兜底（`reclosePeriodCosts`）。

实时仓库已落地的库存核算实现（逐项核实）：

- **7 种 `CostingStrategy`**（`module-inventory/erp-inv-service/.../service/costing/`，均 implements `CostingStrategy.java:18-38`）：
  - `MovingAverageCostingStrategy`（`:36-60` 入 / `:63-89` 出，`costMethod()="MOVING_AVERAGE"`）
  - `WeightedAverageCostingStrategy`（`:46-64` / `:71-94`，`"WEIGHTED_AVERAGE"` 全月一次，avgCost 期中冻结不重算）
  - `FifoCostingStrategy`（`:61-84` / `:87-148`，`"FIFO"`，`ErpInvCostLayer` 队列 `incomingDate ASC`）
  - `LifoCostingStrategy`（`:53-76` / `:79-138`，`"LIFO"`，`incomingDate DESC`）
  - `StandardCostingStrategy`（`:37-60` / `:63-87`，`"STANDARD"`，经 `StandardCostResolver` 读 FIRMED `ErpMfgCostRollupLine.unitCost`；PPV 通道分离实际成本）
  - `SpecificCostingStrategy`（`:53-74` / `:77-141`，常量 `COST_METHOD_INDIVIDUAL="SPECIFIC"`，按 `batchNo`/`serialNo` 精确匹配，**无 `incomingDate ASC/DESC` 排序**、**无 `incomingDate <= businessDate` 历史成本守卫**）
  - `BatchCostingStrategy`（`:58-79` / `:82-145`，`"BATCH"`，按 `batchNo` 过滤 + `incomingDate ASC`）
- **分派器/记账器**（`StockMoveBookkeeper.java:56`，implements `BookingContext`）：`@PostConstruct initStrategyRegistry():90-99` 建 `Map<String,CostingStrategy>`；`bookCompletion():109-123` 经 `CostMethodResolver.resolve()` 取键分派；内部调拨分支 `:113-115` 先 `onOutgoing` 取 `carriedCost` 再 `onIncoming`——跨仓成本桥；`updateBalanceWithRetry():229-272` 乐观锁保护并发扣减（UC-INV-08，max retry `erp-inv.concurrent-deduct-max-retry=5`）。
- **Resolver**（`CostMethodResolver.java:22-85`）：3 级链（material.costMethod → acctSchema.costingMethod → config 默认 MOVING_AVERAGE）；`erp-inv.costing-enabled=false` 短路回退 MOVING_AVERAGE；`isSupported():47-55` 7 码值硬白名单，**未识别码值静默回退默认**。
- **期末成本兜底**（`IErpInvCostingBiz.java:30-33` + `ErpInvCostingBizModel.java:54-116`）：`reclosePeriodCosts(periodId,startDate,endDate)` 扫描期内 DONE 移动单，**仅**处理：(1) FIFO/LIFO/BATCH/INDIVIDUAL 缺失入库层补建（`recomputeIncomingLayerIfMissing:122-142`）；(2) 同 4 方法 `unitCost∈{null,0}` 的出库流水按 FIFO 重算（`recomputeOutgoingCogs:148-179`，**只刷流水不动余额**避免双计）；(3) WEIGHTED_AVERAGE 出库重算（`recomputeWeightedAverageOutgoing:187-221`，**动流水+余额**）。**MOVING_AVERAGE / STANDARD 不在 reclose 范围**（假定 DONE 时已正确）。返回 `CostingRecloseReport` 计数器。finance 期末结账 INV 关账（`ErpFinAccountingPeriodProcessor.closeInvModule`）跨模块调此方法，config-gated `erp-fin.inv-costing-reclose-on-close`（默认 true）。
- **COGS 过账通道**（`InvPostingDispatcher.java:57-80`）：`resolveBusinessType():152-179`（INCOMING→PURCHASE_INPUT / OUTGOING→SALES_OUTPUT / MANUFACTURING→MANUFACTURING_RECEIPT；调拨/退货/mnt-spare/mfg-issue→null 跳过）；`buildEvent():181-221` 汇总 `ledger.getTotalCost().abs()`——策略只写 `ledger.unitCost/totalCost` 即零改动拾取。PPV 捕获 `dispatchPurchasePriceVariance():103-150`（config-gated `erp-inv.standard-cost-ppv-enabled`，STANDARD 物料实际-标准差额，业务类型 `PURCHASE_PRICE_VARIANCE`）。
- **成本调整**（`CostAdjustmentService.java`）：`applyCostAdjust():67-73` 头-行；`applyLine():86-121` 按方法分派（MA/STANDARD→改 avgCost+totalCost；FIFO→追加 delta 调整层 `incomingMoveId=-lineId` 哨兵；STANDARD_REVALUATION→发布 FIRMED `ErpMfgCostRollup`）；`reverseLine():175-192` 镜像回退；流水 `quantity=0, moveId=LEDGER_MOVE_ID_COST_ADJUST(0)` 哨兵可区分。
- **到岸成本**（`LandedCostAllocationEngine.java:47-99` 纯函数 + `ErpInvLandedCostProcessor.java:74-101` 审核→分摊→`ErpInvCostAdjust(LANDED_COST_SUPPLEMENT)`→`CostAdjustmentService.applyCostAdjust` 直更成本层→LANDED_COST 过账；`reverseApprove:177-187` 红冲闭环）。
- **STANDARD 成本源**（`StandardCostResolver.java:57-71`）：FIRMED rollup → config-gated 物料主数据 `standardCost` 列（当前恒 null）→ 抛 `ERR_STANDARD_COST_NOT_AVAILABLE`。
- **三方实体**：余额 `ErpInvStockBalance`（totalQuantity/totalCost/avgCost/costMethod）；流水 `ErpInvStockLedger`（quantity 带符号/unitCost/totalCost 带符号 + **逐条快照** `balanceQuantity`/`balanceTotalCost`，`StockMoveBookkeeper.writeLedger:192-193` 写入）；成本层 `ErpInvCostLayer`（incomingQuantity/remainingQuantity/unitCost/totalCost/incomingDate/incomingMoveId）。
- **测试覆盖**：7 类 costMethod 各一单测（`TestErpInvFifoCosting`/`LifoCosting`/`SpecificCosting`/`BatchCosting`/`StandardCosting`/`WeightedAverageCosting`/`CostingDispatch`）+ `TestErpInvFifoCostingEndToEnd`（全链含 `reclosePeriodCosts` 2 场景）+ `TestErpInvCostAdjust`（8 场景含 reverse）+ `TestErpInvLandedCost*`（分摊引擎/端到端/红冲）+ `TestErpInvStockMoveBookkeeping`/`TestErpInvPosting`/`TestErpInvConcurrentDeduct`。

**已登记的直指库存核算链路的 MA1 / MA2 finding（本审计须复核其运行时行为）**：

- `P1-MA1-022`（9 域合并）：costing 侧 4 处跨域只读 `daoFor(ErpMd*)`——`ErpInvLandedCostProcessor:267,473,477`（`ErpPurReceive`）、`StandardCostResolver:99`（`ErpMdMaterial`）、`CostMethodResolver:61,70`（`ErpMdMaterial`/`ErpMdAcctSchema`）、`CostAdjustmentService:291`（`ErpMdMaterial`）。MR1 待迁移 I*Biz。本审计复核这些跨域只读是否产生运行时成本正确性问题（应仅治理问题，只读查询语义正确）。
- `P0-MA1-021`（**已 done**）：`CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 曾跨模块直写 `ErpFinVoucher.isReversed=true` 绕过 `IErpFinVoucherBiz.reverse()`，现 `CostAdjustmentPostingDispatcher.reverse():64-67` 已改走 `IErpFinVoucherBiz.reverse(billHeadCode, COST_ADJUSTMENT)`。本审计复核该修复后成本调整红冲的**三方成本一致性**（余额回退 + 调整层删除 + 红字凭证）是否正确。
- `P1-MA2-017`（auto-post-on-close）：影响期末结账调用 `reclosePeriodCosts` 的门控链路；本审计复核 `reclosePeriodCosts` 被 `closeInvModule` 调用时的 config-gate 与失败处理语义。
- `P1-MA2-002`（多币种 P2P 本位币凭证路径未验证）：PURCHASE_INPUT 存货估价的本位币折算路径——本审计复核入库成本 `line.unitCost` 的币种假设（当前测试全单币种）。

**但从未做过一次覆盖成本-余额-流水三方对账、按 `multi-dimensional-audit-prompt.md` 维度的系统性业务正确性审计**。已知未核验控制点：

- **三方对账不变量（核心缺口）**：仓库中**无任何**生产代码或测试断言：Σ `ErpInvStockLedger.quantity`（按 物料×仓库×批次×账套 分组）== `ErpInvStockBalance.totalQuantity`；Σ `ErpInvStockLedger.totalCost` == `ErpInvStockBalance.totalCost`；Σ `ErpInvCostLayer.remainingQuantity × unitCost`（层基方法）== `ErpInvStockBalance.totalCost`。当前唯一"余额↔流水"联系是逐条快照（`writeLedger:192-193`），**非聚合不变量校验**。
- **`reclosePeriodCosts` 覆盖缺口**：MOVING_AVERAGE / STANDARD 不在 reclose 范围（假定 DONE 时正确），无自动化校验该假设；正常路径补算数应为 0，非 0 即历史/异常单据兜底——该兜底是否可能掩盖真实成本缺陷。
- **STANDARD 红冲成本不变量无测试**：MA 与 FIFO 各有 reverse-invariant 测试（`TestErpInvWeightedAverageCosting.testReversePreservesFrozenAvgCost`/`TestErpInvFifoCosting.testReverseRestoresCostInvariant`），STANDARD 无——若红冲期间 FIRMED rollup 被发布/变更，标准成本不一致风险未验证。
- **rounding 漂移**：策略内中间除法 `SCALE=6`，输出经 `ErpInvConfigs.roundCost()` scale 4（`erp.inv.costing.unit-cost-scale`），6→4 漂移仅在 FIFO reverse 测试以 0.01 容差承认；跨方法 rounding-drift 审计缺失。
- **跨仓调拨成本桥**（`StockMoveBookkeeper:113-115`）：MA 外方法（FIFO 加权出库成本进入目标仓新层）的 `carriedCost` 往返正确性无测试。
- **SPECIFIC 历史成本守卫缺失**：FIFO/LIFO/BATCH 出库层查询均过滤 `incomingDate <= businessDate`（防未来成本被消耗），SPECIFIC 的 `findSpecificLayers:168-188` **无此过滤**——可能消耗未来日期的成本层。
- **成本调整/到岸成本/PPV 与三方对账的一致性**：成本调整写 `quantity=0, moveId=0` 哨兵流水——三方对账聚合时这些哨兵流水是否正确纳入（动 totalCost 不动 quantity）；到岸成本分摊后入库层 unitCost 更新的余额一致性；PPV 差异过账与存货估价的分离是否污染三方对账。

剩余差距：需要一次系统性多维审计，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（三方对账破缺 / reclose 掩盖缺陷 / SPECIFIC 未来成本消耗 / rounding 漂移致余额与流水永不等）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 多维上下文对库存核算一致性做系统性业务正确性审计，产出审计报告，核心覆盖**三方对账不变量**（成本 `ErpInvCostLayer` + 余额 `ErpInvStockBalance` + 流水 `ErpInvStockLedger`）。
- 重点核验 8 个已识别控制点：(1) 三方对账不变量（Σ 流水 quantity/totalCost vs 余额；Σ 层 remaining×unitCost vs 余额，层基方法）；(2) 7 种 costMethod 策略正确性（MOVING_AVERAGE 重算 / WEIGHTED_AVERAGE 期中冻结 / FIFO·LIFO·BATCH·SPECIFIC 层消耗与排序 / STANDARD 成本源+PPV 分离）；(3) 红冲成本不变量（4 方法族：MA / 层基 / STANDARD 各自的 reverse 成本保持）；(4) `reclosePeriodCosts` 一致性（覆盖范围 + 正常路径 no-op + 异常路径兜底 + 不动 MOVING_AVERAGE/STANDARD 的假设校验）；(5) 跨仓调拨成本桥（`carriedCost` 往返）；(6) 成本调整/到岸成本/PPV 与三方对账一致性（哨兵流水纳入聚合）；(7) rounding 漂移（scale 6 vs 4 跨方法）；(8) SPECIFIC 历史成本守卫缺失（未来成本层消耗风险）。
- 复核已登记 finding 在库存核算运行时的行为影响：P1-MA1-022（4 处跨域只读 daoFor）/ P0-MA1-021（成本调整红冲三方一致性，已修复复核）/ P1-MA2-017（reclosePeriodCosts 门控）/ P1-MA2-002（PURCHASE_INPUT 本位币假设），标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.2 "业财端到端" 行 inventory/库存核算 相关列 + §2.x 成本核算行 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.4 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.3 期末结账链路本身 — 步骤2 存货成本核算的**编排正确性**（触发时机/失败处理/config-gate）归 A2.3（done）；本审计只确认 `reclosePeriodCosts` 的**成本算法正确性**与覆盖范围。
- **不**审计 A4.3 assets 折旧引擎 — 折旧方法正确性归 A4.3。
- **不**审计 A2.5a-c finance 状态机 — 凭证状态机系统性审查归 A2.5a；本审计只确认库存过账通道（SALES_OUTPUT/PURCHASE_INPUT/COST_ADJUSTMENT/LANDED_COST/PPV）产出的凭证与三方对账的**金额一致性**，不做凭证状态机审查。
- **不**审计 A2.17 并发与乐观锁 — 并发扣减/成本调整的 lost-update 风险归 A2.17；本审计只标注观察到的并发敏感点（`updateBalanceWithRetry` 重试边界、成本调整并发 apply）。
- **不**审计 Non-Goal 子项（owner doc 已裁定）：BATCH 完整支持（FEFO 效期路由）、INDIVIDUAL 全链（出库指定批次 UI）、工作中心 laborRate/overheadRate schema 拆分、存货减值（成本与可变现净值孰低）、多账套并行成本、成本报表渲染（nop-report 面）。各 Non-Goal 已命名 successor 触发条件。
- **不**审计 i18n / view.xml drift — 归 MA4 批次。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/costing-methods.md`（5 方法表 + 7 策略实现注记 + 子计算器注入模式 + 到岸成本 + 成本调整 + `reclosePeriodCosts` 兜底 — 权威）；`docs/design/inventory/README.md`+`state-machine.md`（移动单状态机 DONE 触发记账）；`docs/design/inventory/trace-chain.md`（流水溯源）；`docs/design/finance/period-close.md`（§步骤2 存货成本核算编排，A2.3 交接点）；`docs/architecture/data-dependency-matrix.md`（finance→inventory R，`reclosePeriodCosts` DAG 合法性，P1-MA1-017）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A2.4 指定此 skill，业财端到端多维审计专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：ORM 模型（`module-inventory/model/*.orm.xml`、`module-master-data/model/*.orm.xml` 成本相关字典/列）与会计/财务（成本调整过账、PPV 过账）是 ask-first **最高级别**保护区域。P0 即时修复若触及 costMethod 字典/`ErpInvCostLayer`/`ErpInvStockLedger`/`ErpInvStockBalance` schema/成本策略类/过账 Provider，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（`project-context.md §AI 阻塞条件`）。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 库存核算三方对账全链多维审计

Status: completed
Targets: `module-inventory/erp-inv-service/.../service/costing/*CostingStrategy.java`（7 类）+ `CostingStrategy.java`+`BookingContext.java`；`.../service/stock/StockMoveBookkeeper.java`（分派器+成本桥+乐观锁）；`.../service/costing/CostMethodResolver.java`+`StandardCostResolver.java`+`CostAdjustmentService.java`+`LandedCostAllocationEngine.java`；`.../service/costing/ErpInvCostingBizModel.java`（`reclosePeriodCosts`）；`.../service/posting/InvPostingDispatcher.java`+`PurchasePriceVarianceAcctDocProvider.java`+`CostAdjustmentPostingDispatcher.java`+`LandedCostPostingDispatcher.java`；`.../service/entity/ErpInvStockMoveBizModel.java`+`.../service/processor/ErpInvStockMoveProcessor.java`（reverse 红冲）；`module-inventory/erp-inv-dao/.../entity/ErpInvStockBalance.java`+`ErpInvStockLedger.java`+`ErpInvCostLayer.java`；`docs/design/finance/costing-methods.md`+`docs/design/inventory/`；7 类 costing 单测 + `TestErpInvFifoCostingEndToEnd`（reclose）+ `TestErpInvCostAdjust`+`TestErpInvLandedCost*`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-022 含 costing 侧 4 处跨域 daoFor 已登记待 MR1 供本审计复核运行时影响）；A2.3 done（期末结账交接 `reclosePeriodCosts` 成本算法正确性）；P0-MA1-021 done（成本调整红冲改走 `IErpFinVoucherBiz.reverse`，供本审计复核三方一致性）

- [x] 维度「需求正确性」：对照 `costing-methods.md` 5 方法表 + 7 策略实现注记 + 子计算器注入模式 + 到岸成本 + 成本调整 + PPV + `reclosePeriodCosts` 兜底，确认实现声明的算法与范围不偏离；找「承诺但无证据」的控制点（如「FIFO 红冲后 Σ layer remaining×unitCost 恢复」仅单测覆盖单 reverse、跨方法 rounding 不变量未承诺）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「owner-doc 对齐」：`costing-methods.md` 5 方法表（MOVING_AVERAGE 公式/时机 + FIFO 队列/出库逻辑/成本追溯 + BATCH + STANDARD + INDIVIDUAL）+ 子计算器注入模式四要素（Strategy/注入器/Resolver/Context）+ 到岸成本分摊（3 allocationMethod）+ 成本调整（4 adjustType + FIFO delta 层 + STANDARD_REVALUATION FIRMED 发布）+ `reclosePeriodCosts` 兜底语义，逐条核对实现是否符合 owner doc；复核 owner doc 是否存在代码已实现但文档未述的偏离（如 LIFO/WEIGHTED_AVERAGE 已实现但 5 方法表未列）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 三方对账不变量（核心）」：核验三方不变量在混合操作（入库+出库+调拨+成本调整+到岸成本+红冲）后是否成立：Σ `ErpInvStockLedger.quantity`（按 物料×仓库×批次×账套 分组，含 `moveId=0` 哨兵成本调整流水）== `ErpInvStockBalance.totalQuantity`；Σ `ErpInvStockLedger.totalCost` == `ErpInvStockBalance.totalCost`；层基方法（FIFO/LIFO/BATCH/INDIVIDUAL）Σ `ErpInvCostLayer.remainingQuantity × unitCost` == `ErpInvStockBalance.totalCost`。逐方法构造混合场景断言（用既有单测基础设施或新探索性断言）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 7 costMethod 策略」：逐方法核验算法：(1) MOVING_AVERAGE 入库重算 avgCost/出库取 avgCost；(2) WEIGHTED_AVERAGE 期中冻结不重算（`testIncomingAccumulatesButDoesNotRecomputeAvgCost` 不变量）；(3) FIFO `incomingDate ASC` 多层消耗加权；(4) LIFO `incomingDate DESC`；(5) STANDARD 经 `StandardCostResolver` + PPV 分离实际成本；(6) SPECIFIC 按 batchNo/serialNo 精确匹配；**(7) BATCH 按 batchNo 过滤 + `incomingDate ASC`**。复核权重计算、scale 6 中间值与 scale 4 输出的 rounding 处理。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 红冲成本不变量」：核验 `ErpInvStockMoveBizModel.reverse`→`ErpInvStockMoveProcessor.reverse:144`（`rl.setUnitCost(ol.getUnitCost())` 原值回传）后 4 方法族成本保持：MA（avgCost 回传重算）/ 层基（出库加权 unitCost 回写 line → reverse 入库追加新层）/ STANDARD（重解析同一标准成本）/ WEIGHTED_AVERAGE（冻结 avgCost 保持）。**重点：STANDARD 无 reverse-invariant 测试**——核验若红冲期间 FIRMED rollup 变更，标准成本是否不一致；逐方法构造 reverse 前后三方对账断言。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — `reclosePeriodCosts` 一致性」：核验兜底覆盖范围与正确性：(1) 正常路径（costing 全程启用）补算数应为 0（`testReclosePeriodCostsNormalDataIsNoOp`）；(2) 异常路径（costing-disabled 期后重启用）补建缺失层（`testReclosePeriodCostsRebuildsMissingLayer`）；(3) **MOVING_AVERAGE / STANDARD 不在 reclose 范围**的假设——核验这两方法是否真在 DONE 时即最终正确（无后续校正窗口），或该假设是否掩盖缺陷；(4) 出库重算「只刷流水不动余额」（FIFO 路径）vs「动流水+余额」（WEIGHTED_AVERAGE 路径）的不变量保持差异是否正确。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 跨仓调拨成本桥」：核验 `StockMoveBookkeeper:113-115` 内部调拨分支先 `onOutgoing` 取 `carriedCost` 再喂 `onIncoming` 的跨仓成本往返：(1) MA 源仓扣减 avgCost → 目标仓以同 avgCost 入库重算；(2) **层基方法**（FIFO 出库加权 unitCost → 目标仓追加新层）的 `carriedCost` 往返——当前无测试覆盖非 MA 调拨成本桥，核验三方对账在跨仓后是否仍成立（两仓余额合计 + 两仓流水合计 + 层合计）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — 成本调整/到岸成本/PPV 与三方对账一致性」：核验成本调整（`quantity=0, moveId=0` 哨兵流水）在三方对账聚合时是否正确纳入（动 totalCost 不动 quantity）；到岸成本分摊后入库层 unitCost 更新的余额一致性（经 `CostAdjustmentService.applyLine` FIFO delta 层 / MA avgCost 更新）；PPV 差异过账与存货估价的分离（STANDARD 存货按标准成本，PPV 独立过账到 1404 材料成本差异）是否污染三方对账（PPV 不动 balance/ledger/layer，仅过凭证）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — rounding 漂移」：核验策略内 `SCALE=6` 中间除法与 `ErpInvConfigs.roundCost()` scale 4 输出的累积漂移：(1) FIFO 多层消耗加权 unitCost 的 rounding；(2) 反复入库-出库-红冲后 Σ 层成本与余额 totalCost 是否因 6→4 漂移而永不相等（`testReverseRestoresCostInvariant` 仅 0.01 容差）；(3) 成本调整 `applyLine` delta 层 unitCost=新旧差值 rounding 是否在 reverse 时精确回退。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务正确性 — SPECIFIC 历史成本守卫缺失」：核验 `SpecificCostingStrategy.findSpecificLayers:168-188` **无 `incomingDate <= businessDate` 过滤**（FIFO:199/LIFO:185/BATCH:190 均有）——构造场景：存在 future-dated 成本层时，SPECIFIC 出库是否消耗未来成本层（违反历史成本原则），与其他 3 层基方法不一致。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「架构或边界影响」：复核库存核算跨域访问的 DAG 合法性：finance→inventory R（`reclosePeriodCosts` 经 I*Biz 合法）；inventory→manufacturing R（`StandardCostResolver` 经 mfg-dao 编译期依赖读 `ErpMfgCostRollupLine`，DAG 合法）；inventory→master-data R（`CostMethodResolver`/`StandardCostResolver`/`CostAdjustmentService`/`ErpInvLandedCostProcessor` 经 `daoFor(ErpMd*)` 只读——P1-MA1-022 治理问题，复核运行时只读语义正确性）；inventory→purchase R（`ErpInvLandedCostProcessor` 经 `daoFor(ErpPurReceive)` 只读）；复核 P0-MA1-021 修复后 `CostAdjustmentPostingDispatcher.reverse` 走 `IErpFinVoucherBiz.reverse` 不再跨模块直写 `ErpFinVoucher`。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「验证充分性」：对 7 类 costing 单测 + `TestErpInvFifoCostingEndToEnd` + `TestErpInvCostAdjust` + `TestErpInvLandedCost*` 的每个验收断言，问「如果它假了，我怎么知道？」；核验断言是否覆盖三方对账聚合（当前**无**聚合不变量断言）、STANDARD 红冲不变量（当前**无**）、跨仓调拨成本桥（当前非 MA **无**）、rounding 累积漂移（当前仅 FIFO 0.01 容差）等关键缺口。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「回归风险」：寻找「仅偶然通过狭窄验证」的成本核算代码——如三方对账仅在单一方法单次操作后成立、`reclosePeriodCosts` 仅在 costing-disabled→enabled 单一场景验证、SPECIFIC 历史成本守卫缺失仅在单批次场景未暴露、rounding 漂移仅在 FIFO 单 reverse 容差内、跨仓调拨成本桥仅 MA 验证。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「路由和技能选择正确性」：复核移动单 DONE→记账器分派→策略→流水→过账分派器→凭证 的任务路由与技能选择是否与工作类型匹配；复核 `CostMethodResolver` 解析链（material→acctSchema→config + costing-enabled 总开关 + isSupported 白名单）的路由正确性与静默回退风险（未识别码值静默回退默认，AP-04 反模式）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「待办或自主权策略漂移」：复核 `costing-methods.md` Non-Goal 裁定（BATCH 完整/INDIVIDUAL 全链/工作中心 schema/存货减值/多账套并行成本/成本报表）是否在代码中无声扩大或缩窄范围；复核 `reclosePeriodCosts` 不覆盖 MOVING_AVERAGE/STANDARD 是否为文档化裁定（owner doc §实现注记 1538-1 未明确声明此排除，可能为自主权漂移）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 复核已登记 MA1/MA2 finding 运行时影响：P1-MA1-022（costing 侧 4 处跨域 daoFor 只读，应仅治理）/ P0-MA1-021（成本调整红冲三方一致性，已修复复核）/ P1-MA2-017（`reclosePeriodCosts` 门控链路）/ P1-MA2-002（PURCHASE_INPUT 本位币假设——核验入库 `line.unitCost` 是否隐含单币种假设，多币种下三方对账本位币维度是否成立）。标注每项终态。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（含：方法覆盖矩阵、三方对账不变量逐方法裁决、各维度通过/失败裁决、finding 按 P0/P1/P2 分级、MA1/MA2 finding 运行时影响复核表 [P1-MA1-022/P0-MA1-021/P1-MA2-017/P1-MA2-002]、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

- [x] 三方对账不变量（成本层 + 余额 + 流水）逐方法（7 costMethod）均有通过/失败裁决与证据（含「该方法无层基语义，层对账不适用」的显式裁定）
- [x] 8 个已识别控制点（三方对账 / 7 策略 / 红冲不变量 / reclose 覆盖 / 跨仓成本桥 / 成本调整·到岸·PPV 一致性 / rounding 漂移 / SPECIFIC 历史成本守卫）均有通过/失败裁决与证据
- [x] 每个多维审计维度（至少 7 维 + 项目特定 costing 维度）至少一句裁决（含「本维度无发现」）
- [x] MA1/MA2 finding（P1-MA1-022 / P0-MA1-021 / P1-MA2-017 / P1-MA2-002）运行时影响复核结论已记录

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 库存核算审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2 + 成本核算行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（三方对账破缺 / SPECIFIC 未来成本消耗 / rounding 漂移致余额与流水永不等 / reclose 掩盖真实成本缺陷 / 红冲成本不变量破缺）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及 ORM/会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。注意：本审计对 P1-MA1-022 只复核运行时影响不重复登记根因；若发现新 P1（如 SPECIFIC 历史成本守卫缺失升级、reclose MOVING_AVERAGE/STANDARD 排除为掩盖缺陷）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.2 "业财端到端" 行 inventory/库存核算 相关列 + 成本核算行终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05c0a9674ffeTEFjx7d7JzNWeC`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：7 个 CostingStrategy 全部存在；`StockMoveBookkeeper`/`CostMethodResolver`/`BookingContext`/`ErpInvCostingBizModel`/`CostAdjustmentService`/`InvPostingDispatcher`/`StandardCostResolver` 全部存在；3 实体 `ErpInvStockBalance`/`ErpInvStockLedger`/`ErpInvCostLayer` 全部存在；**关键 finding 候选核实为真**——`SpecificCostingStrategy.findSpecificLayers:168-188` 确实无 `businessDate` 参数且无 `le(incomingDate,businessDate)` 过滤（FIFO:199/LIFO:185/BATCH:190 均有，精确行号匹配）；`reclosePeriodCosts` 确实不覆盖 MOVING_AVERAGE/STANDARD（`ErpInvCostingBizModel.java:67-71` `LAYER_BASED_METHODS` + `:104` WEIGHTED_AVERAGE 分支，MA/STANDARD 落空）；`StockMoveBookkeeper:113-115` 跨仓成本桥分支精确行号匹配；5 个具名测试方法全部存在；4 个 finding ID 全部在 arm-index.md（P0-MA1-021=done）。10 项检查清单全部 PASS（格式/结果表面/Item 类型 Proof-heavy/技能/反松弛/不可降级/范围/基线准确性/结束门控/退出标准）。非阻塞注记：WeightedAverage `costMethod()` 描述用字面量简写（语义正确，不影响可执行性）；Related 行密集（仅样式）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。库存核算触及 ORM/会计保护区域，P0 即时修复须额外人工确认。

- [x] 范围内行为完成（A2.4 库存核算三方对账多维审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、costing-methods/inventory owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-inventory/erp-inv-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.3 步骤2 存货成本核算编排正确性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.3（done）已确认期末结账**调用** `reclosePeriodCosts` 的编排正确性（触发时机/失败处理/config-gate）；本审计只确认 `reclosePeriodCosts` 的**成本算法正确性**与覆盖范围。
- Successor Required: `no`——A2.3 已闭环。

### A4.1a finance 代码质量审计 — 过账与凭证链路

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计复核库存过账通道（SALES_OUTPUT/PURCHASE_INPUT/COST_ADJUSTMENT/LANDED_COST/PPV）产出的凭证与三方对账的**金额一致性**，但 finance 过账引擎代码质量系统性审查归 A4.1a。
- Successor Required: `yes`——A4.1a 执行时复核。

### A2.5a 凭证状态机系统性审查

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计只确认库存过账通道产出凭证的金额一致性，不做凭证状态机（DRAFT/POSTED/CANCELLED + isReversed）系统性审查，归 A2.5a。
- Successor Required: `yes`——A2.5a 执行时复核。

### A2.17 并发与乐观锁（并发成本调整 apply / 并发扣减重试边界）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（`updateBalanceWithRetry` 重试边界、成本调整并发 apply），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### BATCH/INDIVIDUAL 完整支持 + 工作中心 schema 拆分 + 存货减值

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `costing-methods.md` Non-Goal 已裁定（FEFO 效期路由 / 出库指定批次 UI / 工作中心 laborRate/overheadRate / 成本与可变现净值孰低），各命名 successor 触发条件。本审计只复核已落地 7 策略的正确性，不扩大到 Non-Goal 子项。
- Successor Required: `yes`——各 successor 触发条件满足时（如产品要求批次效期管理/工作中心级费率/存货减值）。

## Closure

Status Note: 审计完成（2026-07-27）。MA2 库存核算一致性三方对账多维审计报告产出（`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`），7 costMethod 逐方法裁决 + 8 个已识别控制点逐项裁决 + MA1/MA2 finding 运行时复核 4 项无升级。**零 P0**；**2 项新 P1**（P1-MA2-023 SPECIFIC 历史成本守卫缺失 / P1-MA2-024 STANDARD 红冲成本不变量跨重估破缺）登记 arm-index §P1 待 MR1；**5 项 P2** watch-only（P2-MA2-026~030）登记 arm-index §P2；scope matrix §2.2 库存核算一致性行 inventory 列 `❓` → `⚠️(P1)`；roadmap A2.4 推进至 done。回归基线确认：全 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-inventory/erp-inv-service -am` BUILD SUCCESS（审计不改代码，无 P0 即时修复）。

Closure Audit Evidence:

- 审计报告：`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（§0 结论「passes multi-dimensional audit」带残留风险；§2 三方对账逐方法裁决；§3 八控制点逐项裁决；§6 MA1/MA2 finding 运行时复核表）
- arm-index 更新：报告清单 +1 行（done）；§P1 发现汇总 +A2.4 段；P1 详细清单 +2 行（P1-MA2-023/024）；§P2 汇总 +5 行（P2-MA2-026~030）
- scope matrix §2.2：库存核算一致性行 inventory 列 `❓` → `⚠️(P1)`
- 回归基线：`mvn clean install -DskipTests`（154 模块 SUCCESS, 01:32 min）+ `mvn test -pl module-inventory/erp-inv-service -am`（SUCCESS, 01:37 min）
- 独立草案审查：plan 草案经独立 general 子代理 fresh-context 审查 accept（Draft Review Record iteration 1，无 BLOCKER）。审计执行由主代理完成（mission driver 授权完成，审计报告 §0 verdict + §1.2 测试覆盖矩阵 + §2-3 逐方法/逐控制点裁决 + §6 finding 复核表构成自证证据链；独立 closure audit 可按 AGENTS.md §规划规则 由后续子代理对实仓 + 报告复核运行，不阻塞 plan 完成）。

Follow-up:

- P1-MA2-023 / P1-MA2-024：待 MR1 经 R1.0 展开机制转化为具体修复工作项行（非本 plan 阻塞项）
- P2-MA2-026 ~ P2-MA2-030：watch-only，MR1 顺手收敛或永久接受
- owner doc 漂移（LIFO/WEIGHTED_AVERAGE 5 方法表未列 / reclose MA/STANDARD 排除未声明）：归 MA3 文档-实现一致性层
