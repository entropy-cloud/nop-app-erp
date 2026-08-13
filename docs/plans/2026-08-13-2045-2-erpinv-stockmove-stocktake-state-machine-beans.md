# 2026-08-13-2045-2-erpinv-stockmove-stocktake-state-machine-beans 库存移动单 + 盘点单 ErpInvStockMove/ErpInvStockTake.docStatus 实体级状态机 Bean（M4.29 + M4.30）

> Plan Status: completed
> Review Hold: §11.2 M4 (i) 人工/owner-doc 门控**已于 2026-08-13 经人工确认解除**——本计划触及受保护存货过账行为（StockMove DONE 触发 `InvPostingExecutor`→`IErpFinVoucherBiz.post` 存货过账事件，已由起草者经 live code 实证：`ErpInvStockMoveCompleteProcessor` doComplete 翻 DONE + 过账派发；库存强一致保护区）。M4 plan-first + 库存强一致门控成立；该人工裁定非起草者可自主解除（project-context.md 会计/库存保护域硬停止）。计划格式/完备性/范围/结束证据就绪 + 人工门控已确认，已转 `active` 进入实施。
> Last Reviewed: 2026-08-13
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` 工作项 M4.29（ErpInvStockMove.docStatus，plan-first）+ M4.30（ErpInvStockTake.docStatus，plan-first）；M0.2 清单行 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §1.2 inventory INV-1/INV-8 + §3.5 inventory`
> Related: M4 plan-first 先例 `2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（§11.2 M4 硬约束 (i)–(v) + 人工门控 honest framing）；M0.1 契约 `2026-08-12-0617-1-entity-state-machine-m0-1-contract.md`（done）+ M1.3 模板 `2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation.md`（done）；姊妹 M4 计划 `2026-08-13-2045-1-erpfin-period-state-machine-bean.md`、`2026-08-13-2045-3-erpfin-voucher-state-machine-bean.md`
> Mission: entity-state-machine
> Work Item: M4.29 + M4.30
> Audit: required
>
> **治理声明（§11.2 M4）**：本计划按 M4 plan-first 约束执行。StockMove DONE 触发存货过账事件（→ `IErpFinVoucherBiz.post`），StockTake DONE 经差异移动单（当前 Deferred 手工 `generateMove`）间接触发；均属财务影响 + 库存强一致保护区。声明 §11.2 M4 硬约束：(i) plan-first + 受保护行为人工/owner-doc 门控；(ii) 过账时序/编排/失败回退（posted 回写）/红冲闭环不改，继续由 `InvPostingDispatcher/Executor` + StockMoveBookkeeper 编排与 `posted` 契约管理；(iii) `posted` 不入轴；(iv) 跨域副作用（`IErpFinVoucherBiz.post`、余额/预留量更新、批次效期拦截、差异移动单生成）保留原 Processor/`I*Biz` 路径；(v) 既有红冲/反向单闭环以 `posted` 为契约不改。本计划是 plan-first 产物（满足 (i) 的 plan 要件），人工/owner-doc 确认门控已于 2026-08-13 解除，转 `active` 进入实施。
>
> **规则 14 bundling 声明**：M4.29（StockMove）+ M4.30（StockTake）属同一组件（同一 owner doc `docs/design/inventory/state-machine.md`、同一 `erp-inv/move-status` dict、同一「DONE 触发存货过账」行为契约、同一结果表面 = 库存单据 docStatus 生命周期），按指南规则 14 合并为单计划两阶段，而非一轴一计划。

## Current Baseline

> 按 M0.2 清单 `docs/analysis/2026-08-12-entity-state-axis-inventory.md §1.2 inventory INV-1/INV-8 + §3.5` + 实仓核实。

- **共享 dict**：`erp-inv/move-status`（`app-erp-inventory.orm.xml:37`）4 值 `DRAFT/CONFIRMED/DONE/CANCELLED`（实仓核实，**无 COUNTING 值**）。常量 `ErpInvConstants.DOC_STATUS_*`。
- **实体一：ErpInvStockMove**（`app-erp-inventory.orm.xml:148`），`docStatus` `ext:dict="erp-inv/move-status"`（`:161`）。另有 `posted` boolean（业财过账契约，不迁移）。`originMoveId/originReturnedMoveId` 红冲反向单回链（`:184-185`）。
  - **docStatus 现状 writer（5 Processor + facade，实仓核实）**：
    - `confirm`（`ErpInvStockMoveConfirmProcessor` doConfirm）：`DRAFT → CONFIRMED`（守卫 DRAFT + validateAvailable 可用量校验 + validateBatchExpiry 批次效期拦截 + applyReservation 预留量占用）。
    - `complete`（`ErpInvStockMoveCompleteProcessor` doComplete）：`CONFIRMED → DONE`（守卫 CONFIRMED + releaseReservation 释放预留 + StockMoveBookkeeper 记账 + 翻 DONE + InvPostingDispatcher 过账派发 → `IErpFinVoucherBiz.post` 存货过账事件）。
    - `cancel`（`ErpInvStockMoveCancelProcessor:26-37`）：守卫 `DRAFT 或 CONFIRMED`（`:26-33`，非法抛 `ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`erp.err.inv.illegal-status-transition`，`ErpInvErrors.java:49`）携带 `ARG_MOVE_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS="DRAFT或CONFIRMED"`）→ CONFIRMED 时条件 `releaseReservation`（`:34`）→ `setDocStatus(CANCELLED)`（`:37`）。
    - `reverse`（`ErpInvStockMoveReverseProcessor`）：**reversal = 生成反向移动单（新 DRAFT，数量取负），不改原单 docStatus**（owner doc §3 + §2 明确：DONE 的「冲销」是生成新单非状态回退）。→ 在 docStatus Bean 中**无迁移边**（纯生成路径，§9.2 选项 c 初始态 DRAFT）。
    - `generateMove`（`ErpInvStockMoveGenerateMoveProcessor`）：**生成路径**（创建新移动单 seed DRAFT，初始态写入，§9.2 选项 c，不调 assertCan*）。
    - facade `ErpInvStockMoveProcessor:114`：守卫 CONFIRMED（complete 路径辅助）。
  - 终态 = {DONE, CANCELLED}；初始 = {DRAFT}（generateMove/业务单据联动 seed）。
- **实体二：ErpInvStockTake**（`app-erp-inventory.orm.xml:698`），`docStatus` `ext:dict="erp-inv/move-status"`（`:708`，**复用 move-status dict**）。
  - **docStatus 现状 writer（BizModel，实仓核实）**：
    - `startTake`（`ErpInvStockTakeBizModel:28-33`）：守卫 DRAFT（非法 `:29` 抛 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`）→ `setDocStatus(CONFIRMED)`（`:33`）。
    - `completeTake`（`:40-47`）：守卫 CONFIRMED（非法 `:43` 抛 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`）→ `setDocStatus(DONE)`（`:47`）。**无 qtyActual vs totalQuantity 比对、无 `IErpInvStockMoveBiz.generateMove` 调用**——不自动生成盘盈/盘亏移动单（owner doc §盘点单 Deferred：差异调整当前经库管员手工 `generateMove`）。
    - `cancelTake`（`ErpInvStockTakeBizModel:54-63`）：守卫 `非 DONE 且 非 CANCELLED`（`:57-59` 抛 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`）→ `setDocStatus(CANCELLED)`（`:63`）。
  - 终态 = {DONE, CANCELLED}；初始 = {DRAFT}。
- **doc-drift（关键，Phase 3 裁定）**：owner doc `§盘点单状态机（独立）` 状态图标注「草稿→**盘点中 (COUNTING)**→已完成(DONE)/已取消」，但 `erp-inv/move-status` dict **无 COUNTING 值**——StockTake 实际复用 `CONFIRMED`（代码 `:33` setDocStatus(CONFIRMED)）。即 owner doc 标签「盘点中 (COUNTING)」与实际 dict/code 值 `CONFIRMED` 漂移（标签/命名漂移，行为一致）。Phase 3 作 Decision + owner doc 补注（不改 dict/code，保留 CONFIRMED 行为）。
- **过账/库存强一致（§11.2 M4 (ii)/(iii)/(iv)/(v)）**：StockMove DONE → InvPostingExecutor → `IErpFinVoucherBiz.post`（存货估值凭证）；StockMoveBookkeeper 余额/预留量更新（`UK_INV_STOCK_BALANCE_NATURAL` 唯一约束 + retry-on-conflict）；批次效期拦截（config-gated `erp-inv.batch-expiry-check-enabled` 默认 true）。**过账失败保持 DONE + `posted=false`（不置 `posted=true`）**——实仓 `ErpInvStockMoveProcessor:122-124` 先翻 DONE 后派发过账，`InvPostingDispatcher:69-76` try/catch 失败保留 DONE + `posted=false`（ai-autonomy 已知失败模式：过账失败不得误置 `posted=true` 致悬挂）。这些全部保留原 Processor/`I*Biz` 路径，Bean 不触碰。
- **错误码现状**：StockMove cancel/confirm/complete 守卫违例抛 `ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION`（`erp.err.inv.illegal-status-transition`，`ErpInvErrors.java:49`，携带 `ARG_MOVE_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS`）；StockTake 三动作抛 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`（`ErpInvErrors.java:181`）；批次 `ERR_BATCH_EXPIRED`（`:59`，动态守卫）。common 层码 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 已存在（Bean 抛 common 码，Processor 映射 inventory 领域码，common 作 cause）。
- **生产 Bean 注册范式**：`module-inventory/erp-inv-service/src/main/resources/_vfs/erp/inv/beans/app-service.beans.xml` 已以 FQN id 注册 Processor/服务 Bean（StockMoveBookkeeper/InvPostingDispatcher/Executor 等）。**inv-service 当前无 statemachine 目录**（greenfield）。StateMachine Bean 追加于 beans.xml 末尾。
- **既有层 3 回归基线（非 greenfield）**：`TestErpInvStockMoveBizModel`、`TestErpInvStockMoveBookkeeping`（余额/预留量 + retry-on-conflict）、`TestErpInvPosting`（DONE→存货过账事件 + **`testPostingFailureLeavesMoveDonePostedFalse`：过账失败保持 DONE + `posted=false` 断言**，`:107-113`）、`TestErpInvBatchExpiryInterception`（批次效期拦截守卫）、`TestErpInvStockMoveCrudSmoke`、`TestErpInvStockMoveGraphQL`（均 `module-inventory/erp-inv-service/src/test/.../`）。**注意**：`TestErpInvPostingDispatcherFailureHangs` 覆盖的是 LandedCost/CostAdjust/OwnershipTransfer dispatcher（非 StockMove InvPostingDispatcher——其 javadoc 明示 StockMove 悬挂由 `TestErpInvPosting` 覆盖），故 StockMove 过账失败回归归 `TestErpInvPosting.testPostingFailureLeavesMoveDonePostedFalse`。M0.2 §3.5 inventory「既有测试：无」与实仓具名层 3 测试存在轻微漂移——层 1 矩阵测试为 greenfield（新增），层 3 既有回归为上述具名测试（StockTake 专用回归测试由 Phase 2 核实/补全）。
- **合规基线**：`@Inject private` 须保持 R5=0（inv-service grep 证实当前满足）。本计划保持 R5=0、R11 不增。
- **owner doc 覆盖**：`docs/design/inventory/state-machine.md §适用对象`（StockMove 完整 10 维度）+ `§盘点单状态机（独立）`（StockTake）。**StockTake COUNTING 标签漂移**为唯一缺口（Phase 3 补注）。

## Goals

- 落地 2 个无状态 Bean：`ErpInvStockMoveStateMachine`（docStatus 单轴）+ `ErpInvStockTakeStateMachine`（docStatus 单轴，复用同一 move-status dict 语义），遵循 §1 命名 + §2 无状态约束，各可经 Delta 同名覆盖。
  - StockMove 矩阵：`confirm`：`{DRAFT}→CONFIRMED`；`complete`：`{CONFIRMED}→DONE`；`cancel`：`{DRAFT,CONFIRMED}→CANCELLED`。分类 initial=`{DRAFT}`，terminal=`{DONE,CANCELLED}`。`reverse`/`generateMove` 为生成路径（无迁移边，§9.2）。
  - StockTake 矩阵：`startTake`：`{DRAFT}→CONFIRMED`；`completeTake`：`{CONFIRMED}→DONE`；`cancel`：`{DRAFT,CONFIRMED}→CANCELLED`（守卫非终态）。分类 initial=`{DRAFT}`，terminal=`{DONE,CANCELLED}`。
- 将 StockMove 5 Processor/facade + StockTake BizModel 的固定来源态/目标态守卫改调 Bean 委托；**动态业务守卫与副作用保留原位**（可用量校验、批次效期拦截、预留量占用/释放、StockMoveBookkeeper 记账、InvPostingDispatcher 过账派发、余额更新 retry-on-conflict、completeTake 不自动生成差异移动单 Deferred、乐观锁）。
- 保持全部既有外部行为不变（错误码 + 参数、迁移边、过账时序/失败回退/红冲反向单、批次效期、COUNTING↔CONFIRMED 实际值）。
- 各新增层 1 矩阵完备性表驱动测试；层 3 既有集成测试回归全绿（含过账失败不悬挂断言）。
- 层 2 四方对照：确认 4 值全可达 + reversal=生成新单非迁移 + StockTake COUNTING 标签漂移裁定 + completeTake Deferred。

## Non-Goals

- 不迁移 `posted`、不改变 `InvPostingDispatcher/Executor` 过账编排（触发时机/顺序/失败回退/红冲闭环）、不改变 `StockMoveBookkeeper` 余额/预留量语义、不改变批次效期拦截（§11.2 M4 (ii)/(iv)/(v)）。
- 不改变 StockTake `completeTake` 不自动生成差异移动单的 Deferred 行为（owner doc §盘点单；差异当前手工 `generateMove`）。
- 不改变 `reverse`（生成反向新单，不改原单 docStatus）语义；不在 Bean 为 reversal 发明迁移边。
- 不修改 `model/*.orm.xml`、字典值或 API 契约（StockTake COUNTING 标签漂移保留 CONFIRMED 行为，不改 dict/绑；owner doc 补注标签映射）。
- 不迁移 inventory 其余 4 轴（TransferOrder M4.31 / OwnershipTransfer M4.32 / CostAdjust M4.33 / LandedCost M4.34——owner doc 缺口轴，另属 successor 计划）、`ErpInvPickingOrder`（CRUD 桩死状态，owner doc §拣货单 Deferred）、`ErpInvStockMove.approveStatus`（M0.2 INV-2 排除-技术，docStatus 为主轴）。
- 不引入通用 CRUD 对 docStatus 写入的运行时禁止（M0.1 successor）。
- 不自主跳过 M4 plan-first 人工/owner-doc 门控（§11.2 M4 (i)）：门控未确认前计划保持 `draft`。
- 不证 Delta 覆盖（M4 保护域单项，归 M5.3）。

## Task Route

- Type: `implementation-only change`（消费 M0.1 契约 + M1.3 模板 + M0.2 清单；落地两轴 Bean + 接线 + 三层测试 + 四方对照；不改契约/模型/公共 API；**M4 plan-first**——DONE 触发存货过账 + 库存强一致保护区）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`（M0.1 契约 + §11 迁移模板 + §11.2 M4 变体 + §3 posted 不入轴 + §8 生成路径/退化）、`docs/design/inventory/state-machine.md`（§适用对象 StockMove + §盘点单 StockTake + §异常路径 批次效期/并发扣减）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md`（M0.2 清单 inventory INV-1/INV-8 行）、`docs/architecture/processor-extension-pattern.md`、`docs/plans/2026-08-13-0805-3-erpprj-timesheet-settlement-state-machine-beans.md`（M4 plan-first 先例）
- Skill Selection Basis: 路线图 M4.29/M4.30 指定 `nop-backend-dev` + `nop-testing`。`nop-backend-dev` 匹配「Processor/BizModel 接线、生成路径识别、过账/记账副作用保留、批次效期动态守卫、错误码、`@Inject` 非 private、过账吞异常自检」；`nop-testing` 匹配「矩阵表驱动测试 + 既有集成测试回归（含过账故障注入）」。层 2 引用 `state-machine-business-review-prompt.md`。必需输入均已就绪。

## Infrastructure And Config Prereqs

- **M4 plan-first 人工/owner-doc 门控（阻塞前置，§11.2 M4 (i)）**：本计划触及受保护存货过账 + 库存强一致行为（StockMove DONE → 存货过账事件；余额/预留量强一致）。在人工/owner-doc 确认「以行为保持的矩阵集中化方式迁移此两轴、过账/记账/红冲路径完整保留」可接受前为阻塞前置。**[此门控已于 2026-08-13 经人工确认解除，见 Draft Review Record 门控确认记录]**
- 无端口/环境变量/CORS/密钥/.env/外部服务依赖（除既有 `erp-inv.allow-negative-stock` / `erp-inv.batch-expiry-check-enabled` / `erp-inv.concurrent-deduct-max-retry` 配置，保留不动）。无数据迁移。

## Execution Plan

### Phase 1 - 2 个 StateMachine Bean + 注册 + 层 1 矩阵完备性测试

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/statemachine/{ErpInvStockMoveStateMachine,ErpInvStockTakeStateMachine}.java`（新建）、`.../beans/app-service.beans.xml`（注册 2 Bean）、`.../statemachine/TestErpInvStockMoveAndStockTakeStateMachines.java`（新建）
Skill: `nop-backend-dev`（Bean 形状/注册）+ `nop-testing`（层 1 表驱动测试）

- Item Types: `Add | Decision | Proof`
- Prereqs: M1.3 done

- [x] 新建无状态 `ErpInvStockMoveStateMachine`：矩阵 `assertCanConfirm(DRAFT)`→CONFIRMED；`assertCanComplete(CONFIRMED)`→DONE；`assertCanCancel({DRAFT,CONFIRMED})`→CANCELLED。分类 initial=`{DRAFT}`、terminal=`{DONE,CANCELLED}`、`transitions()`=4 边（confirm 1 + complete 1 + cancel 2 来源）。**reversal/generateMove 无迁移边**（生成路径 §9.2，javadoc 标注）。非法来源态抛 common 码携带 action/fromStatus。grep 证实不 import DAO/IBiz/IServiceContext/事务。
  - Skill: `nop-backend-dev`
- [x] 新建无状态 `ErpInvStockTakeStateMachine`：矩阵 `assertCanStartTake(DRAFT)`→CONFIRMED；`assertCanCompleteTake(CONFIRMED)`→DONE；`assertCanCancel({DRAFT,CONFIRMED})`→CANCELLED（守卫非终态 {DONE,CANCELLED}）。分类 initial=`{DRAFT}`、terminal=`{DONE,CANCELLED}`、`transitions()`=4 边。**目标态 CONFIRMED 对应 owner doc 标签「盘点中」（dict 无 COUNTING）**——javadoc 标注 COUNTING↔CONFIRMED 标签映射（Phase 3 裁定）。
  - Skill: `nop-backend-dev`
- [x] Decision（前置）：记录 StockTake COUNTING 标签漂移分类——owner doc §盘点单 标「盘点中 (COUNTING)」但 dict `erp-inv/move-status` 无 COUNTING 值，实际 code 写 CONFIRMED；分类 = `doc label drift`（标签漂移，行为一致），保留 CONFIRMED 行为不改 dict/绑，owner doc 补注标签映射。已落入两 Bean javadoc + 层 1 测试 `takeCountingLabelDriftResolvedToConfirmed`，Phase 3 正式化。
  - Skill: `state-machine-business-review-prompt.md`
- [x] 在 `app-service.beans.xml` 以 FQN id 注册 2 Bean（§11.1 步骤 2）。
  - Skill: `nop-backend-dev`
- [x] Proof（层 1 矩阵完备性，表驱动，§11.1 步骤 4）：`TestErpInvStockMoveAndStockTakeStateMachines`（20 tests 全绿）——StockMove × {confirm/complete/cancel} 合法+非法边 + terminal {DONE,CANCELLED} 真终态无出边 + transitions(4) + initial/terminal + 4 值全可达；StockTake × {startTake/completeTake/cancel} 合法+非法边（cancel 对 DONE/CANCELLED 非法）+ transitions(4) + COUNTING 标签漂移核对。**不经 BizModel 入口**（层 1 只测 Bean）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 2 Bean 无状态、矩阵完整；reversal/generateMove 无迁移边；COUNTING↔CONFIRMED Decision 记录在案
- [x] 2 Bean 已在 `app-service.beans.xml` 注册（FQN id）；`@Inject` 字段非 private（Bean 无注入字段，严格无状态，合规 R5）
- [x] 层 1 矩阵测试通过；本地化编译 `mvn compile -pl module-inventory/erp-inv-service -am` 通过（解除 Phase 2 接线依赖）

### Phase 2 - Processor/BizModel 接线（行为保持，过账/记账/效期副作用保留）+ 层 3 回归

Status: completed
Targets: `ErpInvStockMoveConfirmProcessor`、`ErpInvStockMoveCompleteProcessor`、`ErpInvStockMoveCancelProcessor`、`ErpInvStockMoveProcessor`（facade）、`ErpInvStockTakeBizModel`（3 动作）
Skill: `nop-backend-dev`（接线 + 错误码映射）+ `nop-testing`（回归断言）

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 两 Bean 落地

- [x] StockMove：Confirm/Complete（facade `ErpInvStockMoveProcessor.doConfirm/doComplete`）+ Cancel（`ErpInvStockMoveCancelProcessor`）注入 `ErpInvStockMoveStateMachine`，固定来源态守卫改 `stateMachine.assertCan<Action>(from)` + 目标态回写（`<action>TargetStatus()`）。common→既有 `ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION` 映射（common 作 cause）+ `ARG_MOVE_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS` 参数对外不变（**不新增错误码**）。**完整保留**：confirm 的 validateAvailable + validateBatchExpiry（config-gated）+ applyReservation；complete 的 releaseReservation + StockMoveBookkeeper 记账 + InvPostingDispatcher 过账派发（**过账失败保持 DONE + `posted=false`**——`testPostingFailureLeavesMoveDonePostedFalse` 回归绿）；cancel 的 CONFIRMED 条件 releaseReservation + 跨实体 ErpInvStockBalance 写。**reverse/generateMove 不接线 Bean**（生成路径，初始态 DRAFT §9.2，grep 证实零 `stateMachine`/`assertCan` 引用）。
  - Skill: `nop-backend-dev`
- [x] StockTake：BizModel 3 动作（`startTake`/`completeTake`/`cancelTake`，`ErpInvStockTakeBizModel.java`）注入 `ErpInvStockTakeStateMachine`，固定来源态守卫改 Bean 委托 + 目标态回写。common→`ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION` 映射。**完整保留 `completeTake` 不自动生成差异移动单的 Deferred 行为**（无 qtyActual vs totalQuantity 比对、无 generateMove 调用——owner doc §盘点 Deferred）。
  - Skill: `nop-backend-dev`
- [x] Proof（层 3 回归）：`mvn test -pl module-inventory/erp-inv-service` 全绿（**174 tests, 0 failures, 0 errors**）——重点 `TestErpInvStockMoveBizModel`（confirm/complete/cancel happy + 非法态）、`TestErpInvStockMoveBookkeeping`（余额/预留量 + retry-on-conflict）、`TestErpInvPosting`（DONE→存货过账事件 + **`testPostingFailureLeavesMoveDonePostedFalse`：过账失败保持 DONE + `posted=false` 不悬挂**）、`TestErpInvBatchExpiryInterception`（批次效期拦截守卫不变）、`TestErpInvStockMoveCrudSmoke`、`TestErpInvStockMoveGraphQL`。（`TestErpInvPostingDispatcherFailureHangs` 覆盖 LandedCost/CostAdjust/OwnershipTransfer dispatcher，非 StockMove，不作 StockMove 回归依据。）
  - Skill: `nop-testing`

Exit Criteria:

- [x] StockMove + StockTake 接线后既有测试全绿（行为、过账编排/失败回退/红冲反向单、批次效期、余额/预留量、completeTake Deferred、错误码、乐观锁无回归；过账失败不悬挂已断言）
- [x] grep 证实相关方法体内不再有内联固定状态矩阵判断（动态副作用如可用量/效期/记账/过账/预留量除外；cancel 的 `Objects.equals(status,CONFIRMED)` 为条件 releaseReservation 路由非守卫；reverse/generateMove 初始态不调 assertCan*，零引用证实）

### Phase 3 - 层 2 四方对照 + 漂移 Decision + owner doc 补注

Status: completed
Targets: 四方对照审计记录（写入本计划 Closure 段）；`docs/design/inventory/state-machine.md`（§盘点单 COUNTING↔CONFIRMED 标签补注）；本计划 Closure
Skill: `state-machine-business-review-prompt.md`（四方对照 + 10 维度）

- Item Types: `Proof | Decision | Add`
- Prereqs: Phase 2 接线完成

- [x] Proof（层 2 四方对照，§11.1 步骤 5，10 维度 × 双轴）：dict（move-status 4 值）↔ owner doc（§适用对象 + §盘点单）↔ Bean ↔ writer。StockMove 重点：(a) reversal = 生成反向新单非状态迁移（无迁移边，§9.2，grep 证实零引用）；(b) 4 值全可达（层 1 `moveAllDictValuesReachable` 绿）；(c) 过账/记账/效期副作用边界（完整保留 Processor）。StockTake 重点：(d) COUNTING↔CONFIRMED 标签漂移裁定；(e) completeTake Deferred（不自动生成差异移动单）；(f) dict 复用（StockTake 共享 move-status）。详见 Closure 段四方位对照表。
  - Skill: `state-machine-business-review-prompt.md`
- [x] Add owner doc：在 `docs/design/inventory/state-machine.md §盘点单状态机（独立）` 补 COUNTING↔CONFIRMED 标签映射注记（owner doc 标签「盘点中 (COUNTING)」对应实际 dict/code 值 `CONFIRMED`，erp-inv/move-status 无 COUNTING；行为一致，保留 CONFIRMED）。
  - Skill: `state-machine-business-review-prompt.md`
- [x] Decision（漂移裁定，路线图规则 5）：(a) StockTake COUNTING 标签漂移 = `doc label drift`（owner doc 标签 vs dict/code 值，行为一致），保留 CONFIRMED + owner doc 补注；(b) reversal/completeTake Deferred = 如实反映（非 implementation drift），Bean 不发明边；(c) M0.2 §3.5 inventory「既有测试：无」与实仓具名层 3 测试存在轻微漂移——登记建议 reconcile。详见 Closure 段漂移 Decision。
  - Skill: `state-machine-business-review-prompt.md`

Exit Criteria:

- [x] 四方对照无未裁决漂移（COUNTING 标签 + reversal + Deferred + 测试名均裁定并落入 owner doc/计划）
- [x] owner doc §盘点单 COUNTING↔CONFIRMED 补注与 dict/Bean/代码一致

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_007195d10ffe3z01A42IM8oOzv`，新会话零信任实仓复核) — 1 BLOCKER + 1 MAJOR + 3 MINOR：BLOCKER = `ERR_INV_STOCK_MOVE_ILLEGAL_TRANSITION` 实仓不存在（StockMove cancel/confirm/complete 实际抛 `ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION` `erp.err.inv.illegal-status-transition` 携 ARG_MOVE_CODE/ARG_CURRENT_STATUS/ARG_EXPECTED_STATUS），计划误作基线 + Phase 2 映射目标（会致新增错误码违约 Non-Goals）；MAJOR = `TestErpInvPostingDispatcherFailureHangs` 不测 StockMove 过账失败（实为 LandedCost/CostAdjust/OwnershipTransfer），StockMove 过账失败回归应为 `TestErpInvPosting.testPostingFailureLeavesMoveDonePostedFalse`；MINOR = cancel→`cancelTake`、CancelProcessor 守卫行 `:26-33`、「过账失败保持 DONE + posted=false」措辞。v2 已：全量替换为正确错误码 + Phase 2「不新增错误码」、修正测试归属并正确界定 DispatcherFailureHangs 范围、`cancelTake`、行号、措辞。
- Independent draft review iteration 2: `acceptable as-is`（draft pending M4 gate）（`ses_007107497ffe86UPB2iUrEL3bx`，新会话零信任复核）— 全部 iter1 BLOCKER/MAJOR/MINOR CONFIRMED-FIXED（实仓复核 `ERR_INV_STOCK_MOVE_ILLEGAL_TRANSITION` 零残留 + `ERR_ILLEGAL_STATUS_TRANSITION:49` / `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION:181` 正确 + `testPostingFailureLeavesMoveDonePostedFalse:106-115` DONE+posted=false 断言 + DispatcherFailureHangs javadoc 范围）；无新增 blocker/major；§11.2 M4 (i)–(v)、rule 14 bundling、scope（4 缺口轴/approveStatus/PickingOrder 排除）、anti-slack、4 值可达性、COUNTING 标签漂移裁定全 PASS。草案审查收敛。
- Independent draft review iteration 3: `acceptable as-is (draft pending M4 gate)` — format/completeness/scope/closure 复核 + 关键基线实仓抽查。格式完备性（必需节/字段/三阶段结构 PASS）、范围（M4.29+M4.30 rule 14 bundling + Non-Goals 显式排除 4 缺口轴/approveStatus/PickingOrder）、结束证据（具名验证命令 + 行为/文档/审计门控）均 PASS。实仓抽查复核 iter1 BLOCKER/MAJOR 修复零残留：move-status dict orm.xml:37 确为 4 值无 COUNTING；`ERR_ILLEGAL_STATUS_TRANSITION:49` / `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION:181` 正确且 StockMove/StockTake 抛码归属正确；`testPostingFailureLeavesMoveDonePostedFalse:107-113` 断言 DONE+posted=false；`DispatcherFailureHangs` javadoc:35 自述不覆盖 StockMove。无新增 format/completeness/scope/closure blocker/major。M4 plan-first 人工/owner-doc 门控为合法 upstream-decision hold（§11.2 M4 (i) + 库存强一致保护区 + project-context AI阻塞条件），非审查可解除——保持 `draft` + Review Hold。
- Plan review（mission-driver 2026-08-13-080540-mission-driver）：`approved (review ran); held as draft` — 格式合规性/完备性/范围/结束证据四维度经核定全部就绪，无除门控外的 blocker/major。实仓抽查 iter1 BLOCKER/MAJOR 修复零残留：move-status dict orm.xml:37-42 确为 4 值（DRAFT/CONFIRMED/DONE/CANCELLED）无 COUNTING；`ERR_ILLEGAL_STATUS_TRANSITION:49` / `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION:181` 正确且 StockMove/StockTake 抛码归属正确；`testPostingFailureLeavesMoveDonePostedFalse:107` 断言 DONE+posted=false；`TestErpInvPostingDispatcherFailureHangs:35` javadoc 自述不覆盖 StockMove。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为外部依赖（roadmap 规则「M4 触及受保护行为不因 Bean 抽象免除门控」+ project-context.md 库存/会计保护域硬停止），审查者不可自主解除。保持 `Plan Status: draft`（对齐 holding 机制），不晋升 active。门控解除后于此追加记录（日期 + 批准范围）并转 `active`。
- Plan review（mission-driver 2026-08-13-193118-mission-driver）：`approved (review ran); held as draft` — 四维度复核全部就绪（格式合规性/完备性/范围/结束证据无 blocker/major）。实仓抽查确认基线零漂移：move-status dict orm.xml:37-42 确为 4 值（DRAFT/CONFIRMED/DONE/CANCELLED）无 COUNTING；`ERR_ILLEGAL_STATUS_TRANSITION`（`erp.err.inv.illegal-status-transition`，ErpInvErrors.java）+ `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION` 存在且 StockMove/StockTake 抛码归属正确；`testPostingFailureLeavesMoveDonePostedFalse`（TestErpInvPosting.java:107）断言 posted=false（:113）。范围 M4.29+M4.30 rule 14 bundling 合规（同 owner doc/state-machine.md、同 move-status dict、同「DONE 触发存货过账」契约），Non-Goals 显式排除 4 缺口轴/approveStatus/PickingOrder/posted/CRUD 禁止/Delta 证明。唯一 Blocker = §11.2 M4 (i) 人工/owner-doc 门控，为外部依赖（roadmap「M4 触及受保护行为不因 Bean 抽象免除门控」+ project-context.md 会计/库存保护域硬停止），审查者不可自主解除。保持 `Plan Status: draft` + Review Hold（对齐 holding 机制）；门控解除后转 `active`。
- **M4 plan-first 人工/owner-doc 门控状态：confirmed（2026-08-13）**（§11.2 M4 (i) + 库存强一致保护区）。草案审查已收敛。
- **M4 plan-first 门控确认记录（人工，2026-08-13）**：人工确认「以行为保持方式迁移此两轴、过账/记账/红冲路径完整保留」可接受。门控解除，`Plan Status: draft → active`。

## Closure Gates

> 本计划含生产代码变更（2 Bean + StockMove/StockTake 接线 + 测试 + owner doc 补注），Closure Gates 运行完整仓库验证。无 ORM/API/字典变更（move-status 4 值保留 + StockTake CONFIRMED 行为不改绑），Compliance 基线预期无漂移（R5=0/R11=0）。

- [x] 范围内行为完成（2 Bean + StockMove/StockTake 接线 + 三层证据；过账/记账/效期/红冲反向单时序完整保留，§11.2 M4 (ii)/(iv)/(v)）
- [x] 相关文档对齐（owner doc §盘点单 COUNTING↔CONFIRMED 补注 + 漂移 Decision 登记；路线图 M4.29 + M4.30 done）
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service`（174 tests 全绿，含 `testPostingFailureLeavesMoveDonePostedFalse`）+ Closure 时 `mvn clean install -DskipTests`（全模块 BUILD SUCCESS）+ `bash docs/audits/nop-compliance-checker.sh`（EXIT=0，R5=0/R11=0 基线维持）
- [x] **M4 plan-first 人工/owner-doc 门控已确认并记录于 Draft Review Record**（§11.2 M4 (i)；2026-08-13 人工确认，见 Draft Review Record 门控确认记录）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Draft Review Record iter1-3 + 2 次 mission-driver plan review 收敛）
- [x] 文本一致性已验证：Plan Status=completed、Phase 1/2/3 Status=completed、Exit Criteria/Phase items 全 [x]、Closure Gates 一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位（独立结束审计子代理 `2026-08-13-193118-mission-driver`，新会话零执行者上下文，见 Closure 段审计证据）
- [x] 结束证据存在于文件中（Closure 段 Status Note + 层 2 四方对照表 + 漂移 Decision；执行证据：2 Bean 源文件 + beans.xml 注册 + 层 1 测试 + 接线 diff + owner doc 补注）

## Deferred But Adjudicated

### inventory 其余 4 缺口轴（TransferOrder/OwnershipTransfer/CostAdjust/LandedCost）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.31–M4.34 属 inventory 域 docStatus 轴，但 owner doc `inventory/state-machine.md` 未覆盖（仅 StockMove/StockTake 有章节）——owner-doc 缺口轴，须 layer-2 从代码建立权威语义，owner-doc 义务不同（规则 14 拆分例外）。本计划仅迁移文档化的 M4.29/M4.30。
- Successor Required: yes（触发条件 = inventory 缺口轴迁移计划启动时，补 owner doc 章节 + 4 轴 Bean）

### StockTake completeTake 自动生成差异移动单

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §盘点 Deferred：completeTake 当前仅置 DONE，差异调整经库管员手工 generateMove。本计划保持既有行为。
- Successor Required: yes（触发条件 = 盘点闭环自动化需求落地时，见 owner doc §盘点 Successor）

### StockMove reversal 目标态（DONE→CANCELLED 显式作废）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 既有 reversal 语义 = 生成反向新单（不改原单 docStatus），owner doc §3 明确。本计划保持既有行为，Bean 不发明 reversal 边。
- Successor Required: no（除非 PM 要求原单显式作废态）

### 过账编排 / posted 契约（行为保持边界）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划按 §11.2 M4 (ii)/(iv)/(v) 原序保留 InvPostingDispatcher/Executor + StockMoveBookkeeper 过账/记账编排 + `posted` 不入轴——硬约束而非另属 M4 项。过账 dispatcher 内部实现（凭证科目映射、retry-on-conflict 细节）不在矩阵集中化范围。
- Successor Required: no（行为保持已由本计划 M4 硬约束覆盖）

### 通用 CRUD 写入禁止 / Delta 覆盖证明

- Classification: `watch-only residual` / `optimization candidate`
- Why Not Blocking Closure: CRUD 写入边界 = M0.1 successor；M4 保护域单项不自带 Delta 证明，归 M5.3。
- Successor Required: no（归 M0.1/M5.3）

## Closure

Status Note: 三阶段执行完成。Phase 1 = 2 Bean（`ErpInvStockMoveStateMachine` + `ErpInvStockTakeStateMachine`，严格无状态）+ beans.xml 注册 + 层 1 矩阵测试（20 tests 全绿）；Phase 2 = StockMove Confirm/Complete/Cancel + StockTake 3 动作接线（固定守卫委托 Bean + common 作 cause 映射领域码 + 目标态回写），动态副作用（可用量/效期/记账/过账/预留量/completeTake Deferred）原序保留，reverse/generateMove 生成路径不接线；Phase 3 = 层 2 四方对照（下）+ 漂移 Decision + owner doc §盘点单 COUNTING↔CONFIRMED 补注。验证：`mvn test -pl module-inventory/erp-inv-service` = **174 tests, 0 failures, 0 errors**（含 `testPostingFailureLeavesMoveDonePostedFalse`）；本地化 `mvn compile -pl module-inventory/erp-inv-service -am` 通过。

### 层 2 四方对照（dict ↔ owner-doc ↔ Bean ↔ writer，10 维度 × 双轴）

> 审查方法：`docs/skills/state-machine-business-review-prompt.md` 10 维度。writer 盘点含通用 CRUD 路径（契约 §9.4）。

**轴一：ErpInvStockMove.docStatus**

| 维度 | dict (`erp-inv/move-status`, orm.xml) | owner-doc (§适用对象) | Bean (`ErpInvStockMoveStateMachine`) | writer (Processor/BizModel/CRUD) | 一致性 |
|------|--------|---------|------|--------|--------|
| 1 状态全集 | DRAFT/CONFIRMED/DONE/CANCELLED 4 值 | §1 同 4 值 | `transitions()` 覆盖全 4 值（initial DRAFT + 3 toStatus） | writer 覆盖全 4 值 | ✅ |
| 2 迁移边 | — | §2：DRAFT→CONFIRMED、CONFIRMED→DONE、{DRAFT,CONFIRMED}→CANCELLED | confirm/complete/cancel×2 = 4 边 | doConfirm/doComplete/CancelProcessor 同 | ✅ |
| 3 4 值全可达（无死状态） | 4 值均有 writer | §5 无不可达 | 层 1 `moveAllDictValuesReachable` 绿（DRAFT=initial seed，CONFIRMED/DONE/CANCELLED=迁移 toStatus） | DRAFT=generateMove/联动 seed；CONFIRMED=confirm；DONE=complete；CANCELLED=cancel | ✅ (a) |
| 4 终态无出边 | — | §3：DONE/CANCELLED 终态 | DONE/CANCELLED 真终态（`moveTerminalsAreTrueTerminals` 绿） | 无 writer 从 DONE/CANCELLED 迁出 | ✅ |
| 5 reversal = 生成新单非迁移 | — | §3：DONE「冲销」=生成反向新单非状态回退 | Bean 无 reversal 边（§9.2） | `ErpInvStockMoveReverseProcessor` 不改原单 docStatus（grep 零 `stateMachine` 引用） | ✅ (b) |
| 6 过账/记账/效期副作用边界 | — | §4 异常路径 | Bean 不持有（契约 §2） | 完整保留 Processor：validateAvailable/validateBatchExpiry/applyReservation/releaseReservation/StockMoveBookkeeper/InvPostingDispatcher（失败保持 DONE+posted=false） | ✅ (c) |
| 7 错误码 | — | — | common 码 + action/fromStatus | common→`ERR_ILLEGAL_STATUS_TRANSITION` 映射（cause）+ ARG_MOVE_CODE/CURRENT/EXPECTED 不变 | ✅ |
| 8 生成路径 | — | §9 场景 A/C | generateMove 无迁移边（§9.2 选项 c） | `ErpInvStockMoveGenerateMoveProcessor` seed DRAFT 不调 assertCan*（grep 零引用） | ✅ |
| 9 dict ↔ owner-doc 标签 | 4 值 | §1 标签 = 代码值 | Bean 用 ErpInvDocStatus 常量 | 代码用常量 | ✅ |
| 10 posted 不入轴 | posted boolean（独立字段） | §1 DONE「等待后续过账」+ 业财契约 | Bean 无 posted 边（契约 §3） | posted 由 InvPostingDispatcher/Executor + 红冲闭环管理 | ✅ |

**轴二：ErpInvStockTake.docStatus（复用 move-status dict）**

| 维度 | dict | owner-doc (§盘点单状态机) | Bean (`ErpInvStockTakeStateMachine`) | writer (BizModel/CRUD) | 一致性 |
|------|------|---------|------|--------|--------|
| 1 状态全集 | 复用 move-status 4 值 | 标 COUNTING（漂移，见下） | 4 边覆盖全 4 值 | writer 覆盖全 4 值 | ✅ |
| 2 迁移边 | — | startTake/completeTake/cancel | startTake/completeTake/cancel×2 = 4 边 | BizModel 3 动作同 | ✅ |
| 3 4 值全可达 | 4 值 | §5 同 | 层 1 `takeReachabilityFromInitial` 绿 | DRAFT=seed；CONFIRMED=startTake；DONE=completeTake；CANCELLED=cancel | ✅ |
| 4 终态无出边 | — | DONE/CANCELLED 终态 | `takeTerminalsAreTrueTerminals` 绿 | 无 writer 从 DONE/CANCELLED 迁出 | ✅ |
| 5 COUNTING 标签漂移 | **无 COUNTING 值** | 标「盘点中 (COUNTING)」 | startTake 目标态=CONFIRMED（`takeCountingLabelDriftResolvedToConfirmed` 绿） | 代码 `setDocStatus(CONFIRMED)` | ✅ (d) 已裁定+owner doc 补注 |
| 6 dict 复用 | StockTake 共享 move-status | §盘点单「独立状态机」 | Bean 独立（语义同 dict） | StockTake 复用 CONFIRMED | ✅ (f) |
| 7 completeTake Deferred | — | §盘点 Deferred：手工 generateMove | Bean 不发明边 | completeTake 仅置 DONE，无 qtyActual 比对/无 generateMove 调用 | ✅ (e) 如实反映 |
| 8 错误码 | — | — | common 码 | common→`ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION` 映射（cause）+ ARG_TAKE_ID/CURRENT 不变 | ✅ |

### 漂移 Decision（路线图规则 5）

- **(d) StockTake COUNTING 标签漂移 = `doc label drift`（标签漂移，行为一致）**：owner doc §盘点单状态图标「盘点中 (COUNTING)」，但 dict `erp-inv/move-status` 无 COUNTING 值，实际 code 写 CONFIRMED。裁定：保留 CONFIRMED 行为不改 dict/绑（非 ask-first 可自主范围），owner doc §盘点单已补 COUNTING↔CONFIRMED 标签映射注记。Bean 按既有 writer 建模。**非 implementation drift**（行为一致，仅命名）。
- **(b)(e) reversal 生成新单 + completeTake Deferred = `如实反映`（非 implementation drift）**：reversal = 生成反向新单非状态迁移（owner doc §3 明确），Bean 不发明边（§9.2）；completeTake 不自动生成差异移动单 = owner doc §盘点 Deferred（差异当前手工 generateMove）。两者如实反映既有语义，Bean 正确不发明边。
- **(c) M0.2 §3.5 inventory「既有测试：无」与实仓具名层 3 测试轻微漂移 = 登记建议 reconcile**：M0.2 清单标 inventory「既有测试：无」，但实仓存在具名层 3 测试（`TestErpInvStockMoveBizModel`/`Bookkeeping`/`Posting`/`BatchExpiryInterception` 等，构成层 3 既有基线）。本计划已正确识别并复用为层 3 回归（非 greenfield 重建）。登记建议：M0.2 §3.5 inventory 行 reconcile「既有测试」列。非阻塞。

**四方对照结论**：双轴无未裁决漂移。COUNTING 标签漂移已裁定（doc label drift + owner doc 补注）；reversal/completeTake Deferred 如实反映；4 值全可达无死状态；过账/记账/效期/红冲反向单时序完整保留（§11.2 M4 (ii)/(iv)/(v)）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `2026-08-13-193118-mission-driver`（新会话，零执行者上下文，非执行者自我审计）
- Evidence: 实仓零信任复核（Read/grep/glob），全部退出标准与 Closure Gates 对照 live code 验证通过：
  - **Phase 1 落地**：`ErpInvStockMoveStateMachine.java`（168 行，严格无状态，4 边矩阵 confirm/complete/cancel×2，initial={DRAFT}、terminal={DONE,CANCELLED}，抛 common 层 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` + action/currentStatus/expectedStatus 参数，无 DAO/IBiz/IServiceContext/事务 import）；`ErpInvStockTakeStateMachine.java`（164 行，4 边 startTake/completeTake/cancel×2，javadoc 标注 COUNTING↔CONFIRMED 标签漂移，`startTakeTargetStatus()=CONFIRMED`）；两 Bean 已于 `app-service.beans.xml:154-157` 以 FQN id 注册。
  - **层 1 测试**：`TestErpInvStockMoveAndStockTakeStateMachines.java`（363 行，真实断言 assertEquals/assertTrue/assertThrows，覆盖无重复/冲突边、4 值全可达、终态无出边、合法/非法来源态全集、COUNTING 标签漂移核对）。
  - **Phase 2 接线**：`ErpInvStockMoveProcessor`（doConfirm/doComplete 注入 + 委托 + 目标态回写）、`ErpInvStockMoveCancelProcessor`（cancel 委托）、`ErpInvStockTakeBizModel`（3 动作委托）——全部 `@Inject` 字段包级可见（R5=0 合规）。**reverse/generateMove Processor 零 stateMachine/assertCan 引用**（grep EXIT=1 证实生成路径不接线）。
  - **Phase 3 owner doc**：`docs/design/inventory/state-machine.md` §盘点单状态机 含 COUNTING↔CONFIRMED 标签映射注记（:168 doc label drift 裁定）+ completeTake Deferred 注记（:170）。
  - **Anti-hollow**：无空函数体/`return null` 占位/吞异常；Bean 经 Processor/BizModel 运行时调用并写回目标态；动态副作用（可用量/效期/记账/过账/预留量）保留原位。
  - **Deferred honesty**：§Deferred But Adjudicated 5 项均为 out-of-scope/watch-only（4 缺口轴 successor 计划 / completeTake 自动差异移动单 owner doc Deferred / reversal 目标态 watch-only / 过账编排 watch-only / CRUD 禁止+Delta 归 M0.1/M5.3）——无范围内缺陷伪装 follow-up。
  - **执行证据**：Phase 1 层 1 测试（`TestErpInvStockMoveAndStockTakeStateMachines`）；Phase 2 `mvn test -pl module-inventory/erp-inv-service` 174 tests 全绿（含 `testPostingFailureLeavesMoveDonePostedFalse`）；Phase 3 四方对照 + 漂移 Decision + owner doc §盘点单 COUNTING↔CONFIRMED 补注（`docs/design/inventory/state-machine.md`）。

Follow-up:

- <非阻塞跟进见 §Deferred But Adjudicated（inventory 4 缺口轴 / completeTake 自动差异移动单 / reversal 目标态 / 过账编排 watch-only / CRUD 禁止+Delta 归 M0.1/M5.3）；已确认缺陷不得出现在此处>
