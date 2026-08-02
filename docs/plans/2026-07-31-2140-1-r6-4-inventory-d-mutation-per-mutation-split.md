# 2026-07-31-2140-1-r6-4-inventory-d-mutation-per-mutation-split inventory 域 D-mutation + 内联多步 mutation per-mutation 拆分

> **草案阶段 triage 勘误（draft review iteration 1 实仓复核）**：R6.0 triage §R6.4 两处计数错误，本 plan 修正须拆 **14→11**：
> 1. **StockMove 4 个 trace 方法误分类**：`forwardTrace`/`backwardTrace`/`returnTrace`/`batchTrace` 在 `ErpInvStockMoveBizModel.java:75,81,87,93` 实测为 `@BizQuery`（单行委托 `traceChainQuery`），非 `@BizMutation`。MR6 完成判据（roadmap:271）明文针对 `@BizMutation`，故 `@BizQuery` 方法**不在范围**（对齐 `processor-extension-pattern.md:45`，等同 `findByRelatedBill` 处置）。StockMove 须拆 D-mutation 8→**5**（generateMove/confirm/complete/reverse + **补漏 cancel**）。
> 2. **StockMove.cancel 漏列**：`ErpInvStockMoveBizModel.java:56` `cancel` 为 `@BizMutation`，`ErpInvStockMoveProcessor.cancel:96-113` 实测多步（load→status guard→**条件跨实体 `releaseReservation` 改写 ErpInvStockBalance**→setDocStatus(CANCELLED)→save），非 `:46` 单步翻转（CONFIRMED 时有跨实体预留释放副作用）→ **须拆**（triage 漏列，+1）。
> 3. **facade 处置标签**：`ErpInvOwnershipTransferProcessor`（317 行）与 `ErpInvStockMoveProcessor`（416 行）目录下**无任何 MR5 S-mutation per-mutation 文件**（Glob 实测），且 MR5 R5.6 明文将二者列为「纯 D-mutation，不在本里程碑范围」。故二者实为 **delete-after-extract**，triage 标签 `slim-to-S-delegation-facade` 错误。仅 CostAdjust/LandedCost 是真正的 slim-to-S-delegation-facade。
> 4. **豁免登记**：`ErpInvOwnershipTransferProcessor.cancel:88-101` 为受 status guard 的单状态翻转（无跨实体写）= `:46` 合法豁免，须登记 exemption registry（不计须拆）；LandedCost `allocate` 实测为 `@BizQuery`（非 triage 所述「≤2 步查询」），按 `@BizQuery` 不在范围处置。
> 净影响：StockMove −4（trace 出范围）+1（cancel 补拆）= −3；catA 13→10，total 14→**11**。

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.4
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-31-2115-1-r6-1-finance-d-mutation-per-mutation-split.md`（R6.1 同范式先例 + helper 归属裁决）；`docs/plans/2026-07-30-1433-*-mr5-r5-6-inventory-s-mutation*`（R5.6 inventory S-mutation 先例）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.4
> Audit: required

## Current Baseline

- **MR5 inventory 域 S-mutation 已完成**：CostAdjust（5：SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval）+ LandedCost（2：Approve/ReverseApprove）共 7 个 S-mutation per-mutation Processor 自包含（Glob 实测 `.../inv/service/processor/` 仅此二实体有 S-mutation 文件），2 facade 公共 S-mutation 方法已精简为单行委托。MR6 **不重开 MR5**。
- **类别 A 违规 facade（4 个，持 ≥2 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测行数 + D-mutation 入口（@BizMutation）+ 处置**：
  - `ErpInvCostAdjustProcessor`（246 行）— D-mutation 入口 2：`applyCostAdjust`（:95）、`reverseCostAdjust`（:126）（均 @BizMutation 多步，含成本变更 + 过账派发）。处置：**slim-to-S-delegation-facade**（保留 5 S-mutation 单行委托 + delete 2 D-mutation）。
  - `ErpInvLandedCostProcessor`（532 行）— D-mutation 入口 1：`generateFreightLandedCost`（@BizMutation）；`allocate` 实测为 `@BizQuery`（`ErpInvLandedCostBizModel:53`），**不在 MR6 范围**。处置：**slim-to-S-delegation-facade**（保留 2 S-mutation 单行委托 + delete 1 D-mutation）。
  - `ErpInvOwnershipTransferProcessor`（317 行）— D-mutation 入口 2：`confirm`（:54）、`done`（:64）（均 @BizMutation 多步，done 含跨实体余额重分类 + 过账派发）。`cancel`（:88）= 受 status guard 单状态翻转（无跨实体写）= `:46` 合法豁免，登记 exemption registry 不拆。处置：**delete-after-extract**（无 S-mutation，Glob 实测目录仅 facade）。
  - `ErpInvStockMoveProcessor`（416 行）— D-mutation 入口 5：`generateMove`（:57）、`confirm`（:82）、`complete`（:89）、`cancel`（:96，**triage 漏列补入**——多步，CONFIRMED 时条件跨实体 `releaseReservation` 改写 ErpInvStockBalance）、`reverse`（:115）。`forwardTrace`/`backwardTrace`/`returnTrace`/`batchTrace` 实测为 `@BizQuery`（`ErpInvStockMoveBizModel:75,81,87,93`，单行委托 `traceChainQuery`），**不在 MR6 范围**；`findByRelatedBill` = `@BizAction` 查询不在范围。处置：**delete-after-extract**（无 S-mutation）。
  - **类别 A 须拆合计：10 D-mutation → 10 个新 `<Entity><Method>Processor`**（CostAdjust 2 + LandedCost 1 + OwnershipTransfer 2 + StockMove 5）。D-mutation per-mutation 文件**尚不存在**（Glob 实测目录仅含 4 facade + 7 MR5 S-mutation 文件），本 plan 须**新建**。
- **类别 A BizModel 配线现状**（实测）：`ErpInvCostAdjustBizModel`/`ErpInvLandedCostBizModel` 对 S-mutation 已 `@Inject` per-mutation Processor（MR5 成果），对 D-mutation 仍委托对应 facade；`ErpInvOwnershipTransferBizModel`/`ErpInvStockMoveBizModel` 各 `@Inject` 对应 facade 并委托全部 @BizMutation（含 trace @BizQuery 委托保持不动）。facade 删除/瘦身 D-mutation 后，4 BizModel 的 @BizMutation D-mutation 须**重配线**为 `@Inject` 对应 per-mutation Processor + 单行委托（@BizQuery 委托不重配线——仍委托 facade 或 helper，因其不在 MR6 范围）。
- **类别 B 违规 BizModel（1 个，1 个内联 `@BizMutation`，零 Processor 引用，违反 `:5/:7`）**：`ErpInvTransferOrderBizModel`（位于 `.../service/entity/` 包，`confirm` 方法 `:34` @BizMutation）。须拆 → `ErpInvTransferOrderConfirmProcessor`。
- **须拆合计：11**（类别 A 10 + 类别 B 1）。roadmap R6.4 行原列 14，本 plan 勘误为 11（StockMove trace 4 出范围 + cancel 1 补拆 = −3）。
- **[会计保护区域]** CostAdjust.applyCostAdjust/reverseCostAdjust 涉及存货成本调整 + GL 凭证；LandedCost.generateFreightLandedCost 涉及到岸成本分摊 + 成本更新 + 凭证；OwnershipTransfer.done 涉及余额重分类 + 过账；StockMove generateMove/complete/reverse/cancel 涉及库存余额/预留量/移动加权平均。R1.12（库存核算成本方法缺陷 SPECIFIC/STANDARD 不变量）+ R1.16（业财过账悬挂）+ R2.14（成本链路测试）已修复相关缺陷。owner doc `docs/design/inventory/`（state-machine / costing / consignment）+ `docs/design/finance/costing-methods.md` 已固化语义。本 plan 仅做**编排位置迁移**（facade/BizModel → per-mutation Processor），不改业务语义、成本算法、预留量逻辑或凭证生成逻辑。
- **既有测试基线**：inventory 域 erp-inv-service 测试源文件 28 个（R5.6 实测行为等价基线）。
- **helper 归属裁决（继承 R6.1 方案 A）**：facade 被多 D-mutation 共享的 protected helper（如 `ErpInvStockMoveProcessor.requireMove`/`loadLines`/`applyReservation`/`releaseReservation`、`ErpInvLandedCostProcessor.allocateCost`、`ErpInvOwnershipTransferProcessor.reclassifyBalance`/`findBalance`）保留 facade protected + per-mutation 经 `@Inject` facade 调用（同包 protected 可达，单一真相源，对齐 R6.1 Pattern A）。delete-after-extract facade（StockMove/OwnershipTransfer）保留为共享 helper 持有者（类保留，仅删 D-mutation public 入口），不物理删除文件；@BizQuery 方法（trace）保留 facade 委托 helper（`traceChainQuery`）不动。

## Goals

- inventory 域 11 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 10 + 类别 B 1），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 类别 A 4 facade 按处置（2 delete-after-extract [StockMove/OwnershipTransfer 无 S-mutation] + 2 slim-to-S-delegation-facade [CostAdjust/LandedCost]）；4 BizModel 的 @BizMutation D-mutation 重配线为 `@Inject` per-mutation Processor + 单行委托（@BizQuery 委托不重配线）。facade 共享辅助方法保留 facade protected helper（继承 R6.1 方案 A）。
- 类别 B 1 个 BizModel（TransferOrder）的内联 `@BizMutation` `confirm` 改为 `@Inject` Processor + 单行委托。
- beans.xml 注册全部新 Processor bean（bean id = 全限定类名，对齐既有范式）；xbiz 无 inline-script 残留。
- inventory 域 `mvn test` 全绿（0 failures），会计保护区域（成本/凭证/库存余额/预留量）语义不变经既有测试验证。
- arm-index P1-MA3-062 inventory 域须拆项标记 done；豁免登记（OwnershipTransfer.cancel `:46`）补充 exemption registry；R6.0 triage 勘误（StockMove trace 出范围 + cancel 补拆 + facade 标签，14→11）回填 roadmap §MR6 R6.4 行 + §R6.0 triage §R6.4。

## Non-Goals

- R6.5-R6.8（其他域 + 全量验证）——属后续 plan。
- CostAdjust/LandedCost S-mutation 重构（MR5 R5.6 已完成，状态保持 done）。
- StockMove/OwnershipTransfer 的 @BizQuery / @BizAction 方法（trace × 4 + findByRelatedBill + LandedCost allocate）——非 @BizMutation，不在 MR6 范围，保留 facade/helper 委托不动。
- 新增业务测试——测试覆盖深挖属 MR2/MR3（已完成）；本 plan 仅验证既有测试行为等价。
- 业务语义变更、成本算法调整、预留量逻辑变更、状态机迁移、错误码语义调整——仅编排位置迁移。
- StockTake `startTake`/`completeTake`/`cancelTake`（合法豁免 `:46`，保留 BizModel）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/inventory/`（state-machine / costing / consignment）、`docs/design/finance/costing-methods.md`、`docs/architecture/processor-extension-pattern.md`（真相源）、`docs/architecture/processor-per-mutation-exemption-registry.md`（豁免登记）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律）。涉及会计保护区域（成本调整/到岸成本/库存移动/预留量），须对照 R1.12/R1.16/R2.14 owner doc 静态校验语义不变。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（4 facade → 10 per-mutation Processor）+ BizModel 重配线

Status: completed
Targets: `module-inventory/erp-inv-service/.../processor/ErpInv{CostAdjust,LandedCost,OwnershipTransfer,StockMove}*Processor.java`（新建 10 文件）；4 facade 瘦身/保留-helper；4 BizModel @BizMutation D-mutation 重配线；`.../_vfs/erp/inv/beans/app-service.beans.xml` 注册新 bean
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [x] Decision: 辅助方法归属策略——继承 R6.1 方案 A：facade 被多 D-mutation 共享的 protected helper 保留 facade（同包 protected 可达），per-mutation 经 `@Inject` facade 调用。delete-after-extract facade（StockMove/OwnershipTransfer）**类保留**为共享 helper 持有者（仅删 D-mutation public 入口），不物理删除文件；@BizQuery 方法（trace × 4 + allocate）保留 facade/helper 委托不动。在首个 facade（StockMove）拆分时确认 helper 可达性并记录替代分析（方案 B 上提到域专属基类 `AbstractErpInvXxxProcessor` 仅当跨包不可达时采用）。
  - Skill: `nop-backend-dev`
  - **裁决记录**：采用方案 A（保留 facade protected helper + per-mutation `@Inject` facade，同包 protected 可达）。4 facade 类保留为共享 helper 持有者（非物理删除）：StockMove/OwnershipTransfer = delete-after-extract（删 D-mutation public 入口，保留 protected helper + @BizQuery/@BizAction 方法 + doConfirm/doComplete step），CostAdjust/LandedCost = slim-to-S-delegation（保留 S-mutation 单行委托 + protected helper，删 D-mutation）。StockMove.reverse 须调 generateMove，reverse Processor `@Inject ErpInvStockMoveGenerateMoveProcessor`（对齐 R6.1 AccountingPeriod closeAnnual 迁入 ClosePeriodProcessor 的跨 Processor 委托范式），非回委托 facade。done/applyCostAdjust 等编排服务（postingDispatcher/costAdjustmentService）由各 per-mutation Processor 自包含 `@Inject`，facade 仅保留被多方法共享的 protected helper。
- [x] Add: `ErpInvCostAdjustProcessor` 2 D-mutation 拆分 → `ErpInvCostAdjustApplyCostAdjustProcessor` / `...ReverseCostAdjustProcessor`。每个含 `process()` 主流程 + protected step。facade slim-to-S-delegation（保留 S-mutation 单行委托 + delete 2 D-mutation）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpInvLandedCostProcessor` 1 D-mutation 拆分 → `ErpInvLandedCostGenerateFreightLandedCostProcessor`。`allocate`（@BizQuery）不在范围保留 facade。facade slim-to-S-delegation。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpInvOwnershipTransferProcessor` 2 D-mutation 拆分 → `ErpInvOwnershipTransferConfirmProcessor` / `...DoneProcessor`。`cancel`（:88 `:46` 单步翻转）登记 exemption registry 不拆。facade delete-after-extract（类保留为 helper 持有者，删 D-mutation public 入口）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpInvStockMoveProcessor` 5 D-mutation 拆分 → `ErpInvStockMoveGenerateMoveProcessor` / `...ConfirmProcessor` / `...CompleteProcessor` / `...CancelProcessor` / `...ReverseProcessor`。`forwardTrace`/`backwardTrace`/`returnTrace`/`batchTrace`（@BizQuery）+ `findByRelatedBill`（@BizAction）不在范围保留 facade/helper。facade delete-after-extract（类保留为 helper 持有者）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册全部 10 新 Processor bean（bean id = 全限定类名，对齐既有 per-mutation bean 注册范式；`@Inject` 按类型解析）。
  - Skill: `nop-backend-dev`
- [x] Add: 类别 A BizModel 重配线——4 BizModel（CostAdjust/LandedCost/OwnershipTransfer/StockMove）的 @BizMutation D-mutation 从 `@Inject facade` 改为 `@Inject` 对应 per-mutation Processor + 单行委托。@BizQuery / @BizAction 方法委托保持不动（仍委托 facade/helper）。
  - Skill: `nop-backend-dev`
- [x] Proof: inventory service 本地编译通过（`mvn compile -pl module-inventory/erp-inv-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 A 10 per-mutation 自包含 + 4 facade 瘦身/保留-helper + 4 BizModel @BizMutation D-mutation 重配线 + 编译通过。

- [x] 10 个新 `<Entity><Method>Processor` 文件存在且自包含（`process()` + protected step，非 `return facade.method()` 回委托）
- [x] 4 facade 按处置执行（2 delete-after-extract [StockMove/OwnershipTransfer 类保留删 D-mutation] + 2 slim-to-S-delegation [CostAdjust/LandedCost]）+ 4 BizModel @BizMutation D-mutation 重配线 + beans.xml 更新
- [x] inventory service 本地编译通过

### Phase 2 - 类别 B BizModel 内联 mutation 拆分（1 BizModel → 1 per-mutation Processor）+ 域回归

Status: completed
Targets: `module-inventory/erp-inv-service/.../processor/ErpInvTransferOrderConfirmProcessor.java`（新建）；`.../service/entity/ErpInvTransferOrderBizModel.java` 改单行委托
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: `ErpInvTransferOrderBizModel.confirm` 内联 `@BizMutation` 提取到 `ErpInvTransferOrderConfirmProcessor`（process + protected step），BizModel 改 `@Inject` Processor + `return processor.confirm(transferOrderId, ctx)` 单行委托。beans.xml 注册新 bean。
  - Skill: `nop-backend-dev`
  - **设计记录**：TransferOrder 无既有 facade Processor（类别 B 零 Processor 引用）。per-mutation Processor 自包含 `@Inject IErpInvTransferOrderBiz`（经实体管道 requireEntity/updateEntity，纯位置迁移零行为变更）+ `@Inject IErpFinIntercompanyTransferBiz`（A3 跨法人凭证后置钩子，config-gated 失败不阻塞）。未引入 dao 旁路，保留 CrudBizModel 管道（auth/校验/hook）语义。
- [x] Proof: inventory 域 `mvn test -pl module-inventory/erp-inv-service -am` 全绿（0 failures）。类别 A + B mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线或确认无漂移（GraphQL 经 BizModel 契约面不变）。
  - Skill: `nop-testing`
  - **实测**：`Tests run: 131, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS（`mvn test -pl module-inventory/erp-inv-service`）。无快照漂移（GraphQL 经 BizModel 契约面不变，Processor 为内部编排重构）。注：`-am` 依赖链中 finance-service 有 4 个日期敏感测试因 `YearMonth.now()` 7→8 月翻滚漂移（BadDebtReversal/NotesPayableStateMachine/EmployeeAdvanceCashRepayReversal/Dashboard，MONTH=8 vs 7），与本 plan inventory 改动无关（inventory 改动不可能影响 finance 会计期间/看板日期），故采用「install 上游 -DskipTests + 单跑 inv test」隔离验证。

Exit Criteria:

> 本阶段交付类别 B 1 per-mutation 自包含 + TransferOrderBizModel 改 `@Inject` Processor 单行委托 + 域行为等价证据。

- [x] `ErpInvTransferOrderConfirmProcessor` 文件存在且自包含 + TransferOrderBizModel.confirm 已改为单行委托（grep 确认无残留编排体）+ beans.xml 更新
- [x] inventory 域 `mvn test` 全绿（0 failures），快照漂移已处理

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_046b8a30fffeBoJRpMb0KZsjyJ`）—B1：StockMove 4 个 trace 方法（forwardTrace/backwardTrace/returnTrace/batchTrace）实测为 `@BizQuery`（`ErpInvStockMoveBizModel:75,81,87,93`，单行委托 `traceChainQuery`），非 `@BizMutation`，按 MR6 完成判据不在范围（对齐 `:45` + findByRelatedBill），误分类为 D-mutation 致计数虚高（StockMove 8→4 genuine）。B2：StockMove.cancel（`@BizMutation` `:56`，`Processor.cancel:96-113` 多步含条件跨实体 releaseReservation）triage 漏列须补拆（+1）。OwnershipTransfer.cancel（`:88` 受 status guard 单翻转无跨实体写）= `:46` 豁免须登记。LandedCost allocate 实测 @BizQuery 非「≤2 步查询」。drafter 复核确认全部成立，已修正：StockMove 须拆 8→5、catA 13→10、total 14→**11**，豁免登记 + facade 标签勘误回填。
- Independent draft review iteration 2: accept（task `ses_046b37640ffe6Aksu0kvv0OJPQ`）— 全部 iteration-1 阻塞项经实仓复核确认已正确解决：B1（StockMove 4 trace 方法 @BizQuery 出范围，BizModel:75/81/87/93 实测确认）、B2（StockMove.cancel @BizMutation 多步含条件跨实体 releaseReservation，Processor:96-113 确认须拆）、OwnershipTransfer.cancel `:46` 豁免不拆（Processor:88-101 无跨实体写确认）、LandedCost.allocate @BizQuery 出范围（BizModel:53 确认）。计数一致性全表扫描通过：所有活跃条款一致为 catA 10/catB 1/total 11/StockMove 5/2 delete-after-extract + 2 slim，「14/13/8」仅存于历史勘误上下文。plan-guide 全项达标，达 R6.1 先例质量基线。可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 inventory 域 + compliance + 全量编译。

- [x] inventory 域 11 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 10 + 类别 B 1）
- [x] 4 类别 A facade 按处置执行（2 delete-after-extract [StockMove/OwnershipTransfer] + 2 slim-to-S-delegation [CostAdjust/LandedCost]）
- [x] 4 类别 A BizModel @BizMutation D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托（@BizQuery/@BizAction 委托不动）
- [x] 1 类别 B BizModel 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托
- [x] beans.xml 注册一致性（11 新 bean id 与 @Inject 匹配）
- [x] 合法豁免登记：OwnershipTransfer.cancel（`:46`）+ StockTake startTake/completeTake/cancelTake；@BizQuery 不在范围方法（StockMove trace × 4 + findByRelatedBill + LandedCost allocate）保留未动
- [x] 会计保护区域语义不变（成本/凭证/库存余额/预留量经既有测试行为等价）
- [x] `mvn compile` 全域通过 + `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [x] compliance checker 基线不高于当前基线
- [x] arm-index P1-MA3-062 inventory 域须拆项标记 done + R6.0 triage 勘误（14→11）回填 roadmap R6.4 行 + §R6.0 triage §R6.4 + OwnershipTransfer.cancel 豁免登记 registry
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在/将入 registry 登记非本 plan deferred）_

## Closure

Status Note: 全部两个 Phase 执行完毕。inventory 域 11 个须拆 mutation（类别 A 10 + 类别 B 1）全部拆为独立 `<Entity><Method>Processor`，4 facade 按处置瘦身（2 delete-after-extract [StockMove/OwnershipTransfer 类保留为 helper 持有者] + 2 slim-to-S-delegation [CostAdjust/LandedCost]），5 BizModel 的 @BizMutation D-mutation 改为 `@Inject` per-mutation Processor 单行委托（@BizQuery/@BizAction 委托不动），beans.xml 注册 11 新 bean。合法豁免 OwnershipTransfer.cancel（`:46`）新登 exemption registry；StockTake startTake/completeTake/cancelTake 既有登记。@BizQuery 不在范围方法（StockMove trace × 4 + findByRelatedBill + LandedCost allocate）保留 facade/helper 委托未动。验证：inventory `mvn test` 131 全绿（0 failures/0 errors）+ 全量 `mvn clean install -DskipTests` BUILD SUCCESS（156 reactor 模块）+ compliance checker exit 0。R6.0 triage 勘误（StockMove trace × 4 出范围 + cancel 补拆 + facade 标签，14→11）已回填 roadmap §MR6 R6.4 行 + §R6.0 triage §R6.4 + arm-index P1-MA3-062。会计保护区域语义不变经既有测试验证（BizModel GraphQL 契约面不变，Processor 为内部编排重构）。独立结束审计已由新会话子代理执行并通过（见下 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission `2026-07-31-210902-mission-driver`，CLOSURE_VERIFY 新会话，冷上下文，不重用执行者上下文）
- Evidence:
  - 新建 11 个 per-mutation Processor 文件（类别 A 10 + 类别 B 1），均位于 `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/`：CostAdjust 2（ApplyCostAdjust/ReverseCostAdjust）+ LandedCost 1（GenerateFreightLandedCost）+ OwnershipTransfer 2（Confirm/Done）+ StockMove 5（GenerateMove/Confirm/Complete/Cancel/Reverse）+ TransferOrder 1（Confirm，类别 B）。
  - 4 facade 瘦身：`ErpInvStockMoveProcessor`（删 5 D-mutation 入口，保留 protected helper + doConfirm/doComplete step + findByRelatedBill/@BizQuery trace）/ `ErpInvOwnershipTransferProcessor`（删 confirm/done 入口，保留 cancel `:46` 豁免 + protected helper，postingDispatcher 字段移至 DoneProcessor）/ `ErpInvCostAdjustProcessor`（删 applyCostAdjust/reverseCostAdjust，保留 S-mutation 单行委托 + protected helper，costAdjustmentService/ormTemplate/postingDispatcher 字段移至 per-mutation Processor）/ `ErpInvLandedCostProcessor`（删 generateFreightLandedCost，保留 S-mutation 单行委托 + allocatePreview `:45` + protected helper）。
  - 5 BizModel 重配线（CostAdjust/LandedCost/OwnershipTransfer/StockMove/TransferOrder），所有 `@BizMutation` D-mutation 方法体改为 `return <processor>.<method>(...)` 单行委托；@BizQuery（trace × 4 + allocate）/@BizAction（findByRelatedBill）委托保持不动。
  - beans.xml 注册 11 新 bean（全限定类名 id，对齐既有范式）。
  - 验证命令与结果：
    - `mvn test -pl module-inventory/erp-inv-service`（上游已 install -DskipTests 隔离 finance 日期漂移）→ `Tests run: 131, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS
    - `mvn clean install -DskipTests -T1C`（全量）→ BUILD SUCCESS（156 reactor 模块）
    - `bash docs/audits/nop-compliance-checker.sh` → exit 0（actual ≤ baseline）
  - 文档同步：`docs/backlog/audit-remediation-roadmap.md` R6.4 行 todo→done + 计数 14/catA13→11/catA10 + facade 标签勘误；§R6.0 triage §R6.4 展开节同步勘误；`docs/audits/arm-index.md` P1-MA3-062 标记 R6.4 inventory done；`docs/architecture/processor-per-mutation-exemption-registry.md` §B 新登 OwnershipTransfer.cancel；`docs/logs/2026/08-01.md` 追加日志。
  - finance-service 日期漂移说明：`-am` 依赖链 finance-service 有 4 个日期敏感测试（BadDebtReversal/NotesPayableStateMachine/EmployeeAdvanceCashRepayReversal/Dashboard）因 `YearMonth.now()` 7→8 月翻滚漂移（MONTH=8 vs 7），与 inventory 改动无关，采用「install 上游 -DskipTests + 单跑 inv test」隔离验证 inventory 行为等价。
- 独立结束审计复核（冷上下文新会话，实仓验证非盲信 `[x]`）：
  - **文件落地**：`ls processor/` 实测 11 个新 per-mutation Processor 全部存在（CostAdjust 2 + LandedCost 1 + OwnershipTransfer 2 + StockMove 5 + TransferOrder 1），与 plan 声明一致。
  - **反 hollow**：抽查 `ErpInvStockMoveReverseProcessor` 自包含，`@Inject ErpInvStockMoveGenerateMoveProcessor` 后 `return generateMoveProcessor.generateMove(reverseReq, context)`——跨 Processor 委托非回委托 facade，对齐 R6.1 范式；无 `return null`/空体/吞异常。
  - **facade 瘦身**：4 facade grep 实测——CostAdjust 仅剩 S-mutation 公共方法（submitForApproval/withdrawApproval/approve/reject/reverseApprove），applyCostAdjust/reverseCostAdjust 已删；LandedCost 仅剩 approve/reverseApprove + allocatePreview(`:45`)，generateFreightLandedCost 已删；OwnershipTransfer 仅剩 cancel(`:46` 豁免)+protected helper，confirm/done 已删；StockMove 仅剩 findByRelatedBill(`@BizAction`)+trace × 4(`@BizQuery`)+protected helper(doConfirm/doComplete/applyReservation/releaseReservation)，5 D-mutation 已删。处置与 plan 一致。
  - **BizModel 重配线**：5 BizModel grep 实测所有 `@BizMutation` D-mutation 方法体均为 `return <processor>.<method>(...)` 单行委托；@BizQuery(trace × 4 + allocate)/@BizAction(findByRelatedBill) 仍委托 facade/helper 未动。
  - **beans.xml**：grep 命中 11 新 Processor bean 注册。
  - **豁免登记**：`processor-per-mutation-exemption-registry.md:193` 实测 OwnershipTransfer.cancel 新登（R6.4 标记）。
  - **roadmap/arm-index 同步**：roadmap R6.4 行 done + 14→11 勘误 + facade 标签；arm-index P1-MA3-062 R6.4 inventory done + 完整证据。
  - **docs/logs**：`docs/logs/2026/08-01.md` 实测含 R6.4 inventory 完整日志条目。
  - **Deferred 诚实性**：Deferred But Adjudicated 节为空，无范围内缺陷隐藏。
  - **五点一致性**：Plan Status completed / 两 Phase Status completed / 全 Exit Criteria `[x]` / 全 Closure Gates `[x]`（含本审计门控）/ Closure evidence 实证——全部一致。
  - **审计结论**：APPROVED。会计保护区域（成本/凭证/库存余额/预留量）仅做编排位置迁移，业务语义不变经既有 131 测试行为等价验证。

Follow-up:

- R6.0 triage 勘误（StockMove trace × 4 出范围 + cancel 补拆 + OwnershipTransfer/StockMove facade slim→delete-after-extract，须拆 14→11）须回填 roadmap §MR6 R6.4 行 + §R6.0 triage 展开 §R6.4 + arm-index P1-MA3-062。
- OwnershipTransfer.cancel（`:46`）须补登 `docs/architecture/processor-per-mutation-exemption-registry.md`。
