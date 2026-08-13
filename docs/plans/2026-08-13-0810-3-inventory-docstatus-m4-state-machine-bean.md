# 2026-08-13-0810-3-inventory-docstatus-m4-state-machine-bean 调拨/所有权转移/成本调整/到岸成本 ErpInvTransferOrder/OwnershipTransfer/CostAdjust/LandedCost.docStatus 实体级状态机 Bean（M4.31 + M4.32 + M4.33 + M4.34）

> Plan Status: draft
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控待确认——本计划触及受保护存货成本过账行为（OwnershipTransfer done / CostAdjust applyCostAdjust / LandedCost approve 触发 `*PostingDispatcher`→`IErpFinVoucherBiz.post` 存货成本过账事件，已由起草者经 live code 实证；库存强一致保护区）。TransferOrder 保护区较轻（仅可选跨法人内部往来 GL hook，config-gated + 失败吞掉，无存货成本过账/stock movement——如实登记）。M4 plan-first 门控成立；该人工裁定非起草者可自主解除（project-context.md 会计/库存保护域硬停止）。计划格式/完备性/范围/结束证据就绪后，保持 `draft` 直至门控确认。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.31（ErpInvTransferOrder.docStatus）+ M4.32（ErpInvOwnershipTransfer.docStatus）+ M4.33（ErpInvCostAdjust.docStatus）+ M4.34（ErpInvLandedCost.docStatus），均 plan-first；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.5 inventory`（444 行段）+ §1.2 INV 行
> Related: M4 plan-first 先例 + 同域姊妹计划 `2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`（StockMove/StockTake M4.29/M4.30，common→`ErpInvErrors` 映射 + InvPostingExecutor 过账路径 + 生成路径 §9.2 + 三层测试范式）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；`2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（M4 plan-first 先例）；姊妹 M4 计划 `2026-08-13-0810-1-purchase-docstatus-m4-state-machine-bean.md`、`2026-08-13-0810-2-sales-docstatus-m4-state-machine-bean.md`
> Mission: entity-state-machine
> Work Item: M4.31 + M4.32 + M4.33 + M4.34
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。OwnershipTransfer done / CostAdjust applyCostAdjust / LandedCost approve 触发存货成本过账事件（→ `IErpFinVoucherBiz.post`：OWNERSHIP_TRANSFER/COST_ADJUSTMENT/LANDED_COST），属财务影响 + 库存强一致保护区；TransferOrder 仅可选跨法人内部往来 GL hook（较轻）。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退（posted 回写）/红冲闭环不改，继续由 `OwnershipTransfer/CostAdjustment/LandedCostPostingDispatcher` + `InvPostingExecutor` + `StockMoveBookkeeper`/`CostAdjustmentService` + `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用（`IErpFinVoucherBiz`、余额重分类、成本层更新、子单据 CostAdjust 联动）保留原 Processor/`I*Biz` 路径；(v) 既有红冲/reverse 路径（CostAdjust DONE→CONFIRMED / LandedCost DONE→CANCELLED / OwnershipTransfer 无 reverse）不改。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控未满足前保持 `draft`。
>
> **规则 14 bundling 声明**：M4.31–M4.34 属同一组件（同一 owner doc `docs/design/inventory/state-machine.md`、同一 inventory 域 docStatus 结果表面），按指南规则 14 合并为单计划。4 实体矩阵不同（TransferOrder 单边 / OwnershipTransfer 3 边 / CostAdjust 2 边 approveStatus-gated / LandedCost 2 边双轴联动），故各为独立 Bean + 独立矩阵测试，但共享 owner doc/测试范式/过账保护区裁定，合为一计划四阶段切片。3 实体共享 `erp-inv/move-status` dict；OwnershipTransfer 用独立 dict `erp-inv/ownership-transfer-status`（值相同）——如实反映。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §1.2 + §3.5 inventory`（444 行段）+ 实仓核实。inv-service 当前**无 statemachine 目录**（greenfield，与 StockMove/StockTake 姊妹计划同期）。

- **共享 dict `erp-inv/move-status`**（`app-erp-inventory.orm.xml:37`）4 值 `DRAFT/CONFIRMED/DONE/CANCELLED`，TransferOrder/CostAdjust/LandedCost 复用。常量 `ErpInvConstants.DOC_STATUS_*`。OwnershipTransfer 用**独立 dict `erp-inv/ownership-transfer-status`**（`:98-103`，值相同 DRAFT/CONFIRMED/DONE/CANCELLED），常量 `OWNERSHIP_TRANSFER_STATUS_*`（`ErpInvConstants.java:94` 注释「独立于移动单 move-status，不复用」）。
- **迁移范式（同域姊妹计划）**：StockMove/StockTake 计划确立——Bean 抛 common 码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION`，Processor 映射 inventory 领域码（common 作 cause）；Bean 无状态、不 import DAO/IBiz/IServiceContext/事务；过账路径保留 Processor；生成路径（seed DRAFT）无迁移边（§9.2 选项 c）；FQN id 注册于 beans.xml 末尾；三层测试（层 1 表驱动 / 层 2 四方对照 / 层 3 既有回归）。本计划沿用。
- **实体一：ErpInvTransferOrder**（`app-erp-inventory.orm.xml:597`），`docStatus` `ext:dict="erp-inv/move-status"`（`:608`）+ `approveStatus`（`:609`，**stub-only 无审批 Processor**）+ `posted`（`:610`）。
  - docStatus writer（1 个）：`confirm`（`ErpInvTransferOrderConfirmProcessor.java:30` `setDocStatus(DOC_STATUS_CONFIRMED)`，DRAFT→CONFIRMED；守卫 `validateDraft:36-42` 期望 DRAFT）。
  - **无 cancel/complete/reverse writer**——TransferOrder 仅 DRAFT→CONFIRMED，后续物理移动是独立 `ErpInvStockMove` 流（非本单 docStatus 生命周期）。矩阵 = 单边 `confirm: {DRAFT}→CONFIRMED`；initial=DRAFT、terminal={CONFIRMED}。
  - 过账：confirm **不触发存货成本过账、不生成 stock movement**；仅 `dispatchIntercompanyPosting:44-55` 调跨域 `IErpFinIntercompanyTransferBiz.onTransferConfirmed`（A3 跨法人内部往来 GL hook，config-gated，**失败吞掉** try/catch log warn `:50-53`）。**无 TransferOrderPostingDispatcher**。→ 保护区较轻（如实登记）。
  - 错误码（**关键缺陷**）：`validateDraft:38-40` 抛**错误码** `ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`（StockTake 的码，`ErpInvErrors.java:181`，`erp.err.inv.stock-take.illegal-transition`）+ `ARG_TAKE_ID` 参数——**copy-paste bug**（应为 TransferOrder 自己的码）。**无 TransferOrder 专属 illegal-transition 码**。generic `ERR_ILLEGAL_STATUS_TRANSITION`（`:49`）未被 TransferOrder 使用。
  - 测试：**无**（无 `TestErpInvTransferOrder*`）。
- **实体二：ErpInvOwnershipTransfer**（`:998`），`docStatus` `ext:dict="erp-inv/ownership-transfer-status"`（`:1014`，**独立 dict**）+ `posted`（`:1015`）。**无 approveStatus**（无审批流）。
  - docStatus writer（3 Processor，均用 `OWNERSHIP_TRANSFER_STATUS_*` 常量）：
    - `confirm`（`ErpInvOwnershipTransferConfirmProcessor:24`）：DRAFT→CONFIRMED（守卫 `assertStatus` 期望 DRAFT/CONFIRMED）。
    - `done`（`ErpInvOwnershipTransferDoneProcessor:44`）：CONFIRMED→DONE（守卫 `assertStatus` 期望 CONFIRMED/DONE；若 `!ownership-tracking-enabled` 抛 `ERR_OWNERSHIP_TRACKING_DISABLED`；`reclassifyBalances` 余额重分类 + `postingDispatcher.dispatchIfApplicable` 过账派发）。
    - `cancel`（`ErpInvOwnershipTransferProcessor:59` 内联守卫 `:49-62`）：{DRAFT,CONFIRMED}→CANCELLED（非法抛 `ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS`）。
  - 矩阵 = 3 边；initial=DRAFT、terminal={DONE, CANCELLED}。共享 `assertStatus` 辅助（`ErpInvOwnershipTransferProcessor:254-261`）。
  - 过账：done 触发 `OwnershipTransferPostingDispatcher.dispatchIfApplicable`（`:49-71`，仅 `transferType==VMI_CONSUME` 且 config `erp-inv.vmi-auto-generate-ap=true` 时过账 `ErpFinBusinessType.OWNERSHIP_TRANSFER`→`IErpFinVoucherBiz.post` Dr Inventory/Cr AP；失败保持 DONE + posted=false try/catch `:62-70`）+ 余额重分类（`bookkeeper.updateBalanceWithRetry`，数量守恒非 stock movement）。confirm/cancel 无过账。
  - 错误码：`ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS`（`ErpInvErrors.java:99`，`erp.err.inv.ownership-transfer-illegal-status`，**专属码**，最接近 StockMove 范式）。
  - 测试：`TestErpInvOwnershipTransfer`（361 行，VMI_CONSUME 重分类 + 凭证 + posted=true / tracking-disabled / loc-mismatch / 非-VMI 无 AP）；`TestErpInvPostingDispatcherFailureHangs.testOwnershipTransferFailureLeavesPostedFalse:95-113`；`TestErpInvFinanceReversalWriteback:79`。
- **实体三：ErpInvCostAdjust**（`:1223`），`docStatus` `ext:dict="erp-inv/move-status"`（`:1233`）+ `approveStatus`（`:1234`，**独立审批轴，5 INLINE 动作**）+ `posted`（`:1235`）。
  - docStatus writer（2 Processor + 跨实体 LandedCost 写）：
    - `applyCostAdjust`（`ErpInvCostAdjustApplyCostAdjustProcessor:72` finalizeApplied）：→DONE（守卫 `requireAndValidate:47-61` = validateNotCancelled + 已 applied 检查 `ERR_COST_ADJUST_ALREADY_APPLIED` + 审批门 `ERR_COST_ADJUST_NOT_APPROVED`；**无 docStatus 源态守卫**——docStatus 隐式）。applyCostAdjust 触发 `postingDispatcher.tryPost`（COST_ADJUSTMENT 成本差异凭证）+ 成本层更新。
    - `reverseCostAdjust`（`ErpInvCostAdjustReverseCostAdjustProcessor:62` revertToConfirmed）：DONE→CONFIRMED（守卫 `requirePosted` posted!=true 抛 `ERR_COST_ADJUST_NOT_APPLIED`；触发红字凭证 `postingDispatcher.reverse` + 成本层逆转）。
    - **跨实体 writer（LandedCost facade）**：`ErpInvLandedCostProcessor:303/333/196` 写子 CostAdjust docStatus（DRAFT seed / DONE / CANCELLED）——**内部编排对另一实体的写，刻意绕过 `ErpInvCostAdjustProcessor.applyCostAdjust`**（避免双 COST_ADJUSTMENT+LANDED_COST 过账，注释 `:327-328`）。CostAdjust Bean 须容忍这些写（非 Bean 迁移边，§9.2 内部编排）。
  - 矩阵 = 2 边；initial=DRAFT、terminal={DONE}（CONFIRMED 可逆 post-红冲）。**approveStatus 轴不在本 Bean**（gating 留 Processor，option a 同 StockMove Non-Goal）。
  - 过账：applyCostAdjust → `CostAdjustmentPostingDispatcher.tryPost`（COST_ADJUSTMENT，方向由 totalAdjustAmount 符号定 INCREASE/DECREASE，净 0 跳过）→ 成本层更新（`CostAdjustmentService`）。reverse → 红字凭证 + 成本层逆转。**无 StockMoveBookkeeper、无 stock movement**（纯成本变更，`LEDGER_MOVE_ID_COST_ADJUST=0L` 标记）。
  - 错误码：docStatus **无专属 illegal-transition 码**——apply/reverse 由 `ERR_COST_ADJUST_ALREADY_APPLIED`/`NOT_APPROVED`/`NOT_APPLIED`（posted/approval 门）守卫；`validateNotCancelled` 复用 generic `ERR_ILLEGAL_STATUS_TRANSITION`（`:49`，仅 approveStatus 轴 `illegalTransition:174-179` 用）。
  - 测试：`TestErpInvCostAdjust`（656 行，8 类：MA 增/减、FIFO 追加、STANDARD 重估+差异凭证、审批门、重复 apply 保护、红冲逆转、无余额/负成本拒绝）；`TestErpInvPostingDispatcherFailureHangs.testCostAdjustmentTryPostFailureReturnsNull:82-92`；`TestErpInvStandardCosting`/`TestErpInvWeightedAverageCosting`。
- **实体四：ErpInvLandedCost**（`:1317`），`docStatus` `ext:dict="erp-inv/move-status"`（`:1330`）+ `approveStatus`（`:1331`）+ `posted`（`:1332`）。
  - docStatus writer（全在 facade `ErpInvLandedCostProcessor`）：
    - `approve`（`doPostApprove:351`）：DRAFT→DONE（**同时** approveStatus→APPROVED `:348`——双轴联动；守卫 `ERR_LANDED_COST_ALREADY_APPROVED` 幂等 + 悲观锁 `lockReceiveForAllocation:388` + `validateNotAlreadyAllocated:392-407`）。触发 `LandedCostPostingDispatcher.tryPost`（LANDED_COST Dr Inventory/Cr AP）+ 分配引擎 + 子 CostAdjust 成本层更新。
    - `reverseApprove`（`doReverseApprove:186`）：DONE→CANCELLED（**同时** approveStatus→REJECTED `:185`——双轴联动；守卫 `validateCanReverse:141-147` posted=true + APPROVED）。触发红字凭证 `postingDispatcher.reverse`（**失败吞掉 + 告警** `IErpSysNotificationBiz.notify(NOTIFY_EVENT_LANDED_COST_REVERSE_FAILURE)` `:171/483-499`，G4 分级）+ 子 CostAdjust 逆转。
    - `generateFreightLandedCost`（`createLandedCostHead:264` seed DRAFT）：**生成路径**（初始态 DRAFT，§9.2 选项 c，无 assertCan*）。
  - 矩阵 = 2 边；initial=DRAFT、terminal={DONE, CANCELLED}。**无 CONFIRMED 写**（DRAFT→DONE 直达）。approve/reverseApprove 双轴联动——Bean 仅 docStatus 边，approveStatus 写留 Processor。
  - 过账：approve → `LandedCostPostingDispatcher.tryPost`（LANDED_COST）+ 子 CostAdjust（`createAndApplyCostAdjust:294-337`，刻意直接调 `costAdjustmentService.applyCostAdjust` 非经 CostAdjust Processor，避免双过账）。reverse → 红字凭证（失败吞掉+告警）+ 子成本层逆转。**无 stock movement**（纯成本）。
  - 错误码：docStatus **无专属 illegal-transition 码**——approve/reverse 由 `ERR_LANDED_COST_ALREADY_APPROVED`/`NOT_POSTED`（幂等/posted 门）守卫。
  - 测试：`TestErpInvLandedCostEndToEnd`（517 行，BY_AMOUNT/BY_QUANTITY/多 AP/重复分配）；`TestErpInvLandedCostReversal`；`TestErpInvLandedCostReverseFailureAlert:48`；`TestErpInvLandedCostReceiveMutex:40`（并发）；`TestErpInvLandedCostAllocationEngine`；`TestErpInvPostingDispatcherFailureHangs.testLandedCostTryPostFailureReturnsNull:70-80`。
- **生产 Bean 注册**：`erp-inv-service/.../beans/app-service.beans.xml` 已注册各实体 Processor + 服务 Bean（FQN id）。**无 statemachine 目录**（greenfield）。新 4 Bean 追加于 beans.xml 末尾。
- **过账失败悬挂回归基线**：`TestErpInvPostingDispatcherFailureHangs` 覆盖 OwnershipTransfer/CostAdjust/LandedCost dispatcher 失败（**不含 TransferOrder——其无 dispatcher**）。
- **合规基线**：`@Inject private` 须保持 R5=0（inv-service 当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 缺口（关键）**：`docs/design/inventory/state-machine.md` **仅覆盖 StockMove/StockTake**，TransferOrder/OwnershipTransfer/CostAdjust/LandedCost **无章节**（owner-doc 缺口轴，与 StockMove/StockTake 姊妹计划 Deferred 记载一致）。Phase 3 须从代码建立权威语义并补 owner doc 章节（规则 14 拆分例外：owner-doc 缺口轴的 layer-2 义务包括补章节）。

## Goals

- 落地 4 个无状态 Bean：`ErpInvTransferOrderStateMachine` / `ErpInvOwnershipTransferStateMachine` / `ErpInvCostAdjustStateMachine` / `ErpInvLandedCostStateMachine`（各 docStatus 单轴），遵循 §1 命名 + §2 无状态约束，各可经 Delta 同名覆盖。
  - TransferOrder：`confirm: {DRAFT}→CONFIRMED`；initial={DRAFT}、terminal={CONFIRMED}、transitions(1)。
  - OwnershipTransfer：`confirm: {DRAFT}→CONFIRMED`；`done: {CONFIRMED}→DONE`；`cancel: {DRAFT,CONFIRMED}→CANCELLED`；initial={DRAFT}、terminal={DONE,CANCELLED}、transitions(3)。用 `OWNERSHIP_TRANSFER_STATUS_*` 常量（独立 dict 语义）。
  - CostAdjust：`applyCostAdjust: {DRAFT,CONFIRMED}→DONE`；`reverseCostAdjust: {DONE}→CONFIRMED`；initial={DRAFT}、terminal={DONE}（CONFIRMED 可逆）、transitions(2)。approveStatus 轴不在 Bean（gating 留 Processor）。
  - LandedCost：`approve: {DRAFT}→DONE`；`reverseApprove: {DONE}→CANCELLED`；initial={DRAFT}、terminal={DONE,CANCELLED}、transitions(2)。`generateFreightLandedCost` 生成路径无迁移边（§9.2）。approveStatus 写留 Processor（双轴联动，Bean 仅 docStatus 边）。
- 将 4 实体 Processor/BizModel 的固定来源态/目标态守卫改调 Bean 委托；**动态业务守卫与副作用保留原位**（余额重分类、成本层更新、过账 dispatcher tryPost/reverse、子 CostAdjust 联动、悲观锁、reverse-failure 告警、乐观锁、config 门）。
- 保持全部既有外部行为不变（错误码 + 参数、迁移边、过账时序/失败回退/红冲路径、双轴联动写时序、TransferOrder intercompany hook 失败吞掉）。
- 各新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿（含过账失败不悬挂断言）。
- 层 2 四方对照 + **补 owner doc 4 章节**（owner-doc 缺口轴义务）+ TransferOrder 错误码缺陷 Decision。

## Non-Goals

- 不迁移 `posted`、`approveStatus`（CostAdjust/LandedCost approveStatus 轴非路线图独立项；§11.2 M4 (iii)）、不改变 `*PostingDispatcher`/`InvPostingExecutor` 过账编排、`StockMoveBookkeeper`/`CostAdjustmentService` 余额/成本层语义、TransferOrder intercompany hook（§11.2 M4 (ii)/(iv)/(v)）。
- 不改变 CostAdjust/LandedCost 双轴联动写时序（approve/reverseApprove 同时写 approveStatus+docStatus 的原子性）——Bean 仅集中 docStatus 边，approveStatus 写保留 Processor 原位。
- 不改变 LandedCost reverseApprove 过账失败吞掉+告警行为（G4 分级）。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（OwnershipTransfer 独立 dict 保留；TransferOrder approveStatus stub 保留）。
- 不迁移 inventory 其余轴（StockMove/StockTake=M4.29/M4.30 姊妹计划；`ErpInvPickingOrder`=CRUD 桩死状态；approveStatus 轴非路线图项）。
- **不静默修正 TransferOrder 错误码缺陷**（路线图 Non-Goal「不借迁移改变既有错误码」）——行为保持映射既有（错误）码 + Decision 登记 + successor Fix；亦不静默忽略（路线图规则 5）。
- 不引入通用 CRUD 对 docStatus 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单 + StockMove/StockTake 同域姊妹计划范式；落地 4 轴 Bean + 接线 + 三层测试 + 四方对照 + 补 owner doc；不改契约/模型/公共 API；**M4 plan-first**——3 实体触发存货成本过账 + 库存强一致；TransferOrder 较轻）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §8 生成路径/退化/双轴）、`docs/design/inventory/state-machine.md`（**owner-doc 缺口轴——Phase 3 补 4 章节**）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 §1.2 + §3.5 inventory）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans.md`（同域姊妹范式）、`docs/plans/2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（M4 plan-first 先例）
- Skill Selection Basis: 路线图 M4.31–M4.34 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「4 种不同矩阵 Bean、双轴联动、过账/余额/成本层副作用保留、跨实体子单据写容忍、错误码 common→域映射、TransferOrder 缺陷裁定、`@Inject` 非 private」；`nop-testing` 匹配「4 矩阵表驱动测试 + 既有集成回归（含过账故障注入/失败悬挂）」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护存货成本过账 + 库存强一致行为（OwnershipTransfer done / CostAdjust applyCostAdjust / LandedCost approve → 存货成本过账事件；余额/成本层强一致）。TransferOrder 仅可选 intercompany GL hook（较轻，如实登记）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此 4 轴、过账/余额/成本层/双轴联动/红冲路径完整保留」可接受前，计划保持 `draft`，不得进入实施。门控记录须写入本计划 Draft Review Record。
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖（除既有 `erp-inv.ownership-tracking-enabled`/`erp-inv.vmi-auto-generate-ap`/`erp-fin.budget-commitment-enabled` 配置，保留不动）。无数据迁移。

## Execution Plan

### Phase 1 - 4 个 StateMachine Bean（4 种矩阵）+ 注册 + 层 1 矩阵完备性测试

Status: planned
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/statemachine/{ErpInvTransferOrderStateMachine,ErpInvOwnershipTransferStateMachine,ErpInvCostAdjustStateMachine,ErpInvLandedCostStateMachine}.java`（新建）、`.../beans/app-service.beans.xml`（注册 4 Bean）、`.../statemachine/TestErpInvTransferOwnershipCostAdjustLandedCostStateMachines.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/4 矩阵/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [ ] 新建 `ErpInvTransferOrderStateMachine`：`assertCanConfirm(DRAFT)`→CONFIRMED；initial={DRAFT}、terminal={CONFIRMED}、transitions(1)。javadoc 标注：仅 confirm 边，无 DONE/CANCELLED writer（后续物理移动是独立 StockMove 流）。非法来源态抛 common 码。grep 证实无 DAO/IBiz/IServiceContext/事务 import。
  - Skill: `nop-backend-dev`
- [ ] 新建 `ErpInvOwnershipTransferStateMachine`：`assertCanConfirm(DRAFT)`→CONFIRMED；`assertCanDone(CONFIRMED)`→DONE；`assertCanCancel({DRAFT,CONFIRMED})`→CANCELLED；initial={DRAFT}、terminal={DONE,CANCELLED}、transitions(3)。用 `OWNERSHIP_TRANSFER_STATUS_*` 常量（独立 dict `erp-inv/ownership-transfer-status` 语义，javadoc 标注独立于 move-status）。
  - Skill: `nop-backend-dev`
- [ ] 新建 `ErpInvCostAdjustStateMachine`：`assertCanApplyCostAdjust({DRAFT,CONFIRMED})`→DONE；`assertCanReverseCostAdjust(DONE)`→CONFIRMED；initial={DRAFT}、terminal={DONE}（CONFIRMED 可逆）、transitions(2)。javadoc 标注：approveStatus 轴不在 Bean（gating 留 Processor，option a）；CONFIRMED 仅由 reverse 到达、可 re-apply。
  - Skill: `nop-backend-dev`
- [ ] 新建 `ErpInvLandedCostStateMachine`：`assertCanApprove(DRAFT)`→DONE；`assertCanReverseApprove(DONE)`→CANCELLED；initial={DRAFT}、terminal={DONE,CANCELLED}、transitions(2)。**`generateFreightLandedCost` 无迁移边**（生成路径 seed DRAFT §9.2，javadoc 标注）。javadoc 标注：approve/reverseApprove 双轴联动（同时写 approveStatus），Bean 仅 docStatus 边，approveStatus 写留 Processor。
  - Skill: `nop-backend-dev`
- [ ] Decision（前置，双轴联动）：记录 CostAdjust/LandedCost docStatus-driving action（applyCostAdjust/approve/reverseApprove）同时写 approveStatus 的双轴联动裁定——Bean 仅集中 docStatus 边 + assertCan*（docStatus 源态），approveStatus 写 + approveStatus/posted gating 保留 Processor（option a，同 StockMove Non-Goal 先例）。供 Phase 2/3 引用。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] 在 `app-service.beans.xml` 以 FQN id 注册 4 Bean（追加末尾，§11.1 步骤 2）。
  - Skill: `nop-backend-dev`
- [ ] Proof（层 1 矩阵完备性，表驱动，§11.1 步骤 4）：4 Bean × 各合法+非法边 + terminal 无出边 + transitions 计数 + initial/terminal。TransferOrder 着重 confirm 仅 DRAFT 合法、CONFIRMED/DONE/CANCELLED 非法；OwnershipTransfer 3 边 + cancel 对 DONE 非法；CostAdjust apply 对 DONE/CANCELLED 非法、reverse 仅 DONE 合法；LandedCost approve 仅 DRAFT、reverseApprove 仅 DONE、generateFreightLandedCost 无边。**不经 BizModel 入口**（层 1 只测 Bean）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 Bean 无状态、4 种矩阵完整；双轴联动 Decision 记录在案
- [ ] 4 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（合规 R5）
- [ ] 层 1 矩阵测试通过；本地化编译 `mvn compile -pl module-inventory/erp-inv-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - Processor 接线（4 实体不同形态，行为保持，过账/余额/成本层/双轴副作用保留）+ 层 3 回归

Status: planned
Targets: `ErpInvTransferOrderConfirmProcessor`、`ErpInvOwnershipTransfer{Confirm,Done}Processor`+`ErpInvOwnershipTransferProcessor`(cancel)、`ErpInvCostAdjust{ApplyCostAdjust,ReverseCostAdjust}Processor`、`ErpInvLandedCost{Approve,ReverseApprove}Processor`
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 四 Bean 落地

- [ ] TransferOrder：`ErpInvTransferOrderConfirmProcessor` 注入 `ErpInvTransferOrderStateMachine`，`validateDraft` 改 `stateMachine.assertCanConfirm(from)` + 目标态回写。common→**既有（错误）码 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`** 映射（**行为保持——不修正错误码，缺陷 Decision + successor Fix**，路线图 Non-Goal）+ `ARG_TAKE_ID`/`ARG_CURRENT_STATUS` 参数对外不变。**完整保留** intercompany hook dispatch（config-gated + 失败吞掉）。
  - Skill: `nop-backend-dev`
- [ ] OwnershipTransfer：Confirm/Done Processor + facade cancel 注入 `ErpInvOwnershipTransferStateMachine`，`assertStatus`/cancel 守卫改 Bean 委托 + 目标态回写。common→`ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS` 映射。**完整保留**：done 的 `reclassifyBalances`（余额重分类，数量守恒）+ `OwnershipTransferPostingDispatcher.dispatchIfApplicable`（VMI_CONSUME + config 门过账，**失败保持 DONE + posted=false**）+ `ERR_OWNERSHIP_TRACKING_DISABLED` 动态守卫。
  - Skill: `nop-backend-dev`
- [ ] CostAdjust：ApplyCostAdjust/ReverseCostAdjust Processor 注入 `ErpInvCostAdjustStateMachine`。apply：`assertCanApplyCostAdjust(from)`→DONE 目标态回写（**保留** validateNotCancelled + 已-applied 检查 + 审批门 + 成本层更新 + `CostAdjustmentPostingDispatcher.tryPost`，**失败返回 null 保持 posted=false**）；reverse：`assertCanReverseCostAdjust(DONE)`→CONFIRMED（**保留** requirePosted + 红字凭证 reverse + 成本层逆转）。common→既有码映射（apply/reverse 由 posted/approval 门守卫，docStatus 无专属码——映射到 `validateNotCancelled` 既有 generic `ERR_ILLEGAL_STATUS_TRANSITION` 或记录 Decision）。**跨实体 LandedCost 写子 CostAdjust docStatus 不经 Bean**（内部编排 §9.2，Bean 容忍）。
  - Skill: `nop-backend-dev`
- [ ] LandedCost：Approve/ReverseApprove Processor（facade doPostApprove/doReverseApprove）注入 `ErpInvLandedCostStateMachine`。approve：`assertCanApprove(DRAFT)`→DONE（**保留** 幂等守卫 + 悲观锁 + 分配引擎 + 子 CostAdjust 联动 + `LandedCostPostingDispatcher.tryPost` + **approveStatus→APPROVED 双轴写保留 Processor**）；reverseApprove：`assertCanReverseApprove(DONE)`→CANCELLED（**保留** validateCanReverse(posted+APPROVED) + 红字凭证 reverse **失败吞掉+告警** + 子 CostAdjust 逆转 + **approveStatus→REJECTED 双轴写保留 Processor**）。`generateFreightLandedCost` 不接线 Bean（生成路径）。common→既有码映射（`ERR_LANDED_COST_ALREADY_APPROVED`/`NOT_POSTED` 幂等/posted 门守卫）。
  - Skill: `nop-backend-dev`
- [ ] Proof（层 3 回归）：`mvn test -pl module-inventory/erp-inv-service -am` 全绿——重点 `TestErpInvOwnershipTransfer`（VMI 重分类+凭证+posted / tracking-disabled / 非-VMI 无 AP）、`TestErpInvCostAdjust`（8 类含 STANDARD 重估+差异凭证 + 重复 apply 保护 + 红冲逆转）、`TestErpInvLandedCostEndToEnd`/`TestErpInvLandedCostReversal`/`TestErpInvLandedCostReverseFailureAlert`（reverse 失败告警不变）/`TestErpInvLandedCostReceiveMutex`（并发）、`TestErpInvPostingDispatcherFailureHangs`（OwnershipTransfer/CostAdjust/LandedCost dispatcher 失败悬挂——**不含 TransferOrder**）、`TestErpInvStandardCosting`/`TestErpInvWeightedAverageCosting`。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 4 实体接线后既有测试全绿（行为、过账编排/失败回退/红冲、余额重分类、成本层更新、双轴联动写时序、reverse-failure 告警、悲观锁、错误码、乐观锁无回归；过账失败不悬挂已断言）
- [ ] grep 证实相关方法体内不再有内联固定状态矩阵判断（动态副作用如过账/余额/成本层/子单据/告警除外；生成路径初始态不调 assertCan*）

### Phase 3 - 层 2 四方对照 + 补 owner doc 4 章节 + 漂移/缺陷 Decision

Status: planned
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/inventory/state-machine.md`（**补 TransferOrder/OwnershipTransfer/CostAdjust/LandedCost 4 章节**——owner-doc 缺口轴义务）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [ ] Proof（层 2 四方对照，§11.1 步骤 5，10 维度 × 4 轴）：dict ↔ owner doc（补章节）↔ Bean ↔ writer。重点：(a) TransferOrder 单边 + intercompany hook 较轻保护区；(b) OwnershipTransfer 独立 dict + 3 边 + VMI 过账门；(c) CostAdjust 双轴联动 + 跨实体子单据写 + CONFIRMED 可逆 + **net-0 DONE+posted=false 边缘核实**（既有代码 applyCostAdjust 无 docStatus 源态守卫，net-0 调整可达 DONE+posted=false 且理论可重复 apply；Bean `assertCanApplyCostAdjust({DRAFT,CONFIRMED})` 对 DONE 源态拒绝属合理收紧——Phase 3 须核实无既有测试覆盖此边缘并裁定收紧不违反 Non-Goal「保持既有外部行为不变」，若实测存在从 DONE 重 apply 路径则改 Decision 记录）；(d) LandedCost 双轴联动 + 生成路径无边 + reverse 失败吞掉告警；(e) TransferOrder 错误码缺陷裁定。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Add owner doc：在 `docs/design/inventory/state-machine.md` 补 4 章节（TransferOrder/OwnershipTransfer/CostAdjust/LandedCost docStatus 矩阵 + 状态定义 + 过账/副作用边界 + 双轴联动说明）——**owner-doc 缺口轴义务**（与 StockMove/StockTake 姊妹计划 Deferred 记载对接：本计划填补该 Deferred 的 4 轴部分）。
  - Skill: `state-machine-business-review-prompt.md`
- [ ] Decision（漂移/缺陷裁定，路线图规则 5）：(a) **TransferOrder 错误码缺陷** = `confirmed live defect`（confirm 守卫抛 StockTake 的 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`+ARG_TAKE_ID，copy-paste bug）——本计划行为保持映射既有（错误）码（路线图 Non-Goal），successor Fix 引入 `ERR_INV_TRANSFER_ORDER_ILLEGAL_TRANSITION`（非降级，命名 successor 触发条件）；(b) CostAdjust/LandedCost docStatus 无专属 illegal-transition 码（由 posted/approval 门守卫）=`intentional legacy`，Bean common→既有码映射；(c) LandedCost reverseApprove 过账失败吞掉+告警（G4 分级）=`intentional legacy`保留；(d) 跨实体子 CostAdjust 写（LandedCost facade）=内部编排，Bean 不发明边。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [ ] 四方对照无未裁决漂移（TransferOrder 错误码缺陷 + 双轴联动 + 跨实体写 + reverse 告警均裁定并落入 owner doc/计划）
- [ ] owner doc 4 章节补齐与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is (draft pending M4 gate)` (`ses_006e74d79ffev2zTEyOjVoXKWj`，新会话零信任实仓复核) — BLOCKER=none、MAJOR=none、MINOR=1（已采纳修正）：CostAdjust `assertCanApplyCostAdjust({DRAFT,CONFIRMED})→DONE` 较既有代码（applyCostAdjust `requireAndValidate:47-61` 无 docStatus 源态守卫，仅守 posted/validateNotCancelled/审批门）略严——net-0 CostAdjust 可达 DONE+posted=false（finalizeApplied:72 置 DONE，voucherId==null 跳过 posted=true），理论可从 DONE 重 apply；属合理收紧（从 DONE 重 apply 语义错误，正常流程重 apply 必经 reverse→CONFIRMED），无既有测试覆盖。→ 已在 Phase 3 layer-2 四方对照 (c) 增「net-0 DONE+posted=false 边缘核实」步骤（若实测存在从 DONE 重 apply 路径则改 Decision 记录，确保不违反 Non-Goal）。CONFIRMED（独立实证）：dict 绑定（3 共享 move-status + OwnershipTransfer 独立 ownership-transfer-status 同值 + OWNERSHIP_TRANSFER_STATUS_* 常量非 DOC_STATUS_*）、4 实体矩阵与 writer 行号 EXACT（TransferOrder 单边 confirm:30；OwnershipTransfer 3 边 confirm:24/done:44/cancel:59；CostAdjust 2 边 finalizeApplied:72/revertToConfirmed:62 + 跨实体 LandedCost 写子 CostAdjust :303/:333/:196；LandedCost 2 边 doPostApprove:351/doReverseApprove:186 + generateFreightLandedCost seed:264）、**TransferOrder copy-paste 缺陷 CONFIRMED**（validateDraft:38-40 抛 StockTake 的 ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION+ARG_TAKE_ID，无 TransferOrder 专属码；处理=行为保持+Decision+successor Fix 符合规则 5/13/Non-Goal，不静默忽略不静默修正）、过账路径（OwnershipTransfer done→OWNERSHIP_TRANSFER VMI+config 失败保 DONE+posted=false + 余额重分类数量守恒非 stock movement；CostAdjust→COST_ADJUSTMENT 方向由符号定净 0 跳过无 stock movement；LandedCost→LANDED_COST+子 CostAdjust+allocation；TransferOrder→仅 intercompany GL hook config-gated 失败吞掉）、双轴联动（LandedCost approve/reverseApprove 原子写 docStatus+approveStatus；CostAdjust 独立 approveStatus 轴 5 INLINE 动作；Bean 仅 docStatus 边 option a 合理）、posted 不入轴、测试存在性（含 PostingDispatcherFailureHangs 覆盖 3 实体不含 TransferOrder）+ 无矩阵测试 + 无 TransferOrder 测试、owner-doc 缺口（state-machine.md 仅 StockMove/StockTake，4 实体无章节→Phase 3 补 4 章节义务正确，对接姊妹计划 Deferred）、§11.2 M4 (i)-(v) 声明完整 + TransferOrder 较轻保护区 framing honest + rule 14 bundling 合理 + R5=0 + Plan Status=draft 不自激活。
- **M4 plan-first 人工/owner-doc 门控状态：pending**（§11.2 M4 (i) + 会计/库存强一致保护区）。草案审查虽已收敛（acceptable-as-draft），但在人工/owner-doc 确认「以行为保持方式迁移此 4 轴、过账/余额/成本层/双轴联动/红冲路径完整保留」前保持 `Plan Status: draft`（对齐 M3.10/M4.29-30/M4.1/M4.2 plan-first 先例）。确认后在此追加记录，方可转 `active`。
- Mission-driver review（2026-08-13，format/completeness/scope/closure checklist）：格式合规（front matter + 10 必需段 + 三阶段 Phase 结构完整、Item Types 全部落入合法集合 Add/Decision/Proof/Fix）；完备性达标（各阶段 Exit Criteria 可测且覆盖全部 Closure Gates 条目）；范围清晰（规则 14 bundling 同 owner doc/同结果表面已显式裁定、Non-Goals 明确、无 scope creep）；结束证据就绪（Closure 段 + Deferred But Adjudicated 全部带 successor 触发条件）。无可就地修复的 Blocker/Major。唯一阻塞 = §11.2 M4 (i) 人工/owner-doc 门控（本计划触及存货成本过账保护区 OwnershipTransfer done / CostAdjust applyCostAdjust / LandedCost approve → posting 事件，属 project-context.md 会计/库存硬停止域），为审查者不可自主解除的上游人工裁定。HOLD 裁定与 4 个姊妹 M4 plan-first 计划（StockMove/StockTake、Projects timesheet、Purchase、Sales）一致——均保持 `draft` 待门控。本审查 corroboration 不改变 hold 状态。
- Mission-driver review iteration 2（2026-08-13，fresh 复核）：独立复验全部四项——(1) 格式合规：front matter 含 Plan Status/Review Hold/Last Reviewed/Source/Related/Mission/Work Item/Audit；10 必需段齐全；3 阶段 Phase 各含 Status/Targets/Skill/Item Types/Prereqs/checkbox+Skill/Exit Criteria；Item Types 全合法（P1=Add|Decision|Proof、P2=Fix|Proof、P3=Proof|Decision|Add）。(2) 完备性：Exit Criteria 可测（P1 无状态+注册+层1+compile、P2 既有回归全绿+grep、P3 四方对照无未裁决漂移+owner doc 4 章节）；执行项覆盖全部 Goals。(3) 范围：规则 14 bundling 同 owner doc/同结果表面已显式裁定，Non-Goals 明确，无 scope creep。(4) 结束证据：Closure Gates 含具体验证命令（mvn test/-DskipTests/compliance-checker）、Deferred 全带 successor 触发条件。无就地可修 Blocker/Major。唯一阻塞 = §11.2 M4 (i) 人工/owner-doc 门控（OwnershipTransfer done/CostAdjust applyCostAdjust/LandedCost approve 触发存货成本过账事件 = project-context.md 会计/库存硬停止域），属上游人工裁定，审查者不可自主解除。Review Hold 已就位（line 4），Plan Status 保持 `draft`。本审查不改 hold 状态，仅 corroboration。
- Mission-driver review iteration 3（2026-08-13，fresh 复核 format/completeness/scope/closure checklist）：独立复验全部四项——(1) **格式合规**：front matter 完整（Plan Status=draft + Review Hold §11.2 M4(i) + Last Reviewed + Source + Related + Mission + Work Item + Audit=required）；10 必需段齐全（Current Baseline / Goals / Non-Goals / Task Route / Infrastructure And Config Prereqs / Execution Plan / Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure）；3 阶段 Phase 各含 Status=planned / Targets / Skill / Item Types / Prereqs / checkbox+Skill / Exit Criteria，结构合法；Item Types 全部落入合法集合 {Fix,Add,Decision,Proof,Follow-up}（P1=Add|Decision|Proof、P2=Fix|Proof、P3=Proof|Decision|Add），无非法类型。(2) **完备性达标**：各阶段 Exit Criteria 可测且互不重叠（P1=4 Bean 无状态+注册 FQN id+R5 合规+层1 矩阵+mvn compile；P2=既有集成回归全绿含过账失败悬挂断言+grep 证内联矩阵已清除；P3=四方对照无未裁决漂移+owner doc 4 章节与 dict/Bean/代码一致）；执行项完整覆盖全部 5 条 Goals（4 Bean→P1 / 接线→P2 / 行为保持→P2+P3 / 层1+层3 测试→P1+P2 / 层2 四方对照+补 owner doc+缺陷 Decision→P3），无 Goal 缺失执行项。(3) **范围清晰**：规则 14 bundling 已显式裁定（同一 owner doc `docs/design/inventory/state-machine.md` + 同一 inventory docStatus 结果表面，4 实体矩阵不同故各为独立 Bean+独立矩阵测试，合为四阶段切片）；Non-Goals 明确且穷尽（posted/approveStatus 不迁移、过账编排/余额/成本层不改、双轴联动写时序不改、reverse 失败告警保留、ORM/字典/API 不改、不静默修正 TransferOrder 缺陷、M4 门控不自主跳过、Delta 证明归 M5.3）；无 "and also..." scope creep，无 optional/maybe/nice-to-have 松弛词。(4) **结束证据就绪**：Closure Gates 9 项含具体验证命令（mvn test -pl module-inventory/erp-inv-service + mvn clean install -DskipTests + compliance-checker.sh）、M4 门控确认门控、独立结束审计门控；Deferred But Adjudicated 5 项全部带 Classification + Why Not Blocking + Successor Required 触发条件；TransferOrder 错误码缺陷正确分类为 `confirmed live defect moved to explicit successor ownership`（满足规则 13——已确认+successor，非静默降级为 follow-up，且位于 Deferred 而非 Follow-up 段）。**结论**：format/completeness/scope/closure 四维无就地可修的 Blocker/Major。唯一阻塞 = §11.2 M4 (i) 人工/owner-doc 门控（OwnershipTransfer done / CostAdjust applyCostAdjust / LandedCost approve 触发存货成本过账事件，属 project-context.md §AI 阻塞条件「触及会计/财务保护区域且 owner doc 缺失对应章节」硬停止域；owner doc `docs/design/inventory/state-machine.md` 当前仅覆盖 StockMove/StockTake，4 实体无章节）。该阻塞为审查者不可自主解除的上游人工裁定（escape-hatch「missing upstream decision」情形）。Review Hold 已就位且理由准确（line 4），Plan Status 保持 `draft` 不激活。本审查仅 corroboration，不改变 hold 状态——与 iteration 1/2 及 4 个姊妹 M4 plan-first 计划一致。

- Mission-driver review iteration 4（2026-08-13，fresh 复核 format/completeness/scope/closure checklist）：独立复验全部四项——(1) **格式合规**：front matter 完整（Plan Status=draft + Review Hold §11.2 M4(i) + Last Reviewed=2026-08-13 + Source + Related + Mission + Work Item + Audit=required）；10 必需段齐全；3 阶段 Phase 各含 Status=planned / Targets / Skill / Item Types / Prereqs / checkbox+Skill / Exit Criteria；Item Types 全合法（P1=Add|Decision|Proof、P2=Fix|Proof、P3=Proof|Decision|Add）。(2) **完备性达标**：Exit Criteria 可测且互不重叠；执行项完整覆盖全部 5 条 Goals，无 Goal 缺失执行项。(3) **范围清晰**：规则 14 bundling 已显式裁定（同一 owner doc + 同一 inventory docStatus 结果表面，4 实体矩阵不同故各为独立 Bean+独立矩阵测试）；Non-Goals 明确穷尽，无 scope creep/松弛词。(4) **结束证据就绪**：Closure Gates 9 项含具体验证命令 + M4 门控确认门控 + 独立结束审计门控；Deferred 5 项全带 Classification + Successor 触发条件；TransferOrder 错误码缺陷正确分类为 `confirmed live defect moved to explicit successor ownership`（规则 13 合规）。**结论**：format/completeness/scope/closure 四维无就地可修的 Blocker/Major。唯一阻塞 = §11.2 M4 (i) 人工/owner-doc 门控（OwnershipTransfer done / CostAdjust applyCostAdjust / LandedCost approve 触发存货成本过账事件，属 project-context.md §AI 阻塞条件硬停止域），为审查者不可自主解除的上游人工裁定。Review Hold 已就位（line 4），Plan Status 保持 `draft`。本审查仅 corroboration，不改 hold 状态。

## Closure Gates

> 本计划含生产代码变更（4 Bean + 4 实体接线 + 测试 + owner doc 补 4 章节），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（move-status/ownership-transfer-status 值保留 + TransferOrder approveStatus stub 保留），Compliance 基线预期无漂移（R5=0/R11=0）。

- [ ] 范围内行为完成（4 Bean + 4 实体接线 + 三层证据；过账/余额/成本层/双轴联动/红冲/reverse 告警时序完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [ ] 相关文档对齐（owner doc 补 4 章节 + 漂移/缺陷 Decision 登记；路线图 M4.31 + M4.32 + M4.33 + M4.34 done）
- [ ] 已运行验证：`mvn test -pl module-inventory/erp-inv-service -am` + Closure 时 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`
- [ ] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### TransferOrder 错误码缺陷修正（引入专属 illegal-transition 码）

- Classification: `confirmed live defect moved to explicit successor ownership`
- Why Not Blocking Closure: `ErpInvTransferOrderConfirmProcessor.validateDraft:38-40` 抛 StockTake 的 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`（+ARG_TAKE_ID），copy-paste bug。路线图 Non-Goal「不借迁移改变既有错误码」→ 本计划行为保持映射既有（错误）码。修正须新增 `ERR_INV_TRANSFER_ORDER_ILLEGAL_TRANSITION` + 改外部错误码值/参数 shape（takeId→transferOrderCode）+ 前端文案/测试影响。
- Successor Required: yes（触发条件 = 独立「TransferOrder 错误码缺陷修正」Fix plan，须评估外部错误码变更影响 + 取得 owner-doc 确认）

### TransferOrder DONE/CANCELLED 生命周期 + approveStatus 接入

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: TransferOrder 仅 confirm（DRAFT→CONFIRMED），无 cancel/complete；approveStatus 列为 stub-only（无审批 Processor）。补全属业务行为变更。
- Successor Required: yes（触发条件 = PM 要求调拨单 cancel/完成/审批业务流落地时）

### inventory approveStatus 轴（CostAdjust/LandedCost）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: CostAdjust/LandedCost 的 approveStatus 审批轴非路线图独立 M4 项（M4.29–M4.34 仅 docStatus）。本计划仅迁移 docStatus，approveStatus 写/gating 保留 Processor。
- Successor Required: yes（触发条件 = PM/owner 纳入 approveStatus 轴迁移时，另起 plan）

### 过账编排 / posted 契约（行为保持边界）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划按 §11.2 M4 (ii)/(iv)/(v) 原序保留过账 dispatcher/executor + 余额/成本层编排 + `posted` 不入轴。LandedCost reverseApprove 过账失败吞掉+告警（G4 分级）为 intentional legacy 保留。
- Successor Required: no（行为保持已由本计划 M4 硬约束覆盖）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD 写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，归 M5.3。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: <待执行与独立结束审计后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <task id / walkthrough record>

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated；已确认缺陷不得出现在此处>
